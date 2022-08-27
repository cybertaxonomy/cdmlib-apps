/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraSpecimenImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.occurrence.DerivationEvent;
import eu.etaxonomy.cdm.model.occurrence.DerivationEventType;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.DeterminationEvent;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.cache.occurrence.DerivedUnitDefaultCacheStrategy;


/**
 * Alga Terra Import für den Fact mit FactId =202 (Ecology)
 * @author a.mueller
 * @since 01.09.2012
 */
@Component
public class AlgaTerraFactEcologyImport  extends AlgaTerraSpecimenImportBase {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "determinations";
	private static final String dbTableName = "Fact";


	public AlgaTerraFactEcologyImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT f.factId " +
				" FROM Fact f LEFT JOIN PTaxon pt ON f.PTNameFk = pt.PTNameFk AND f.PTRefFk = pt.PTRefFk "
				+ " WHERE f.FactCategoryFk = 202 "
				+ " ORDER BY pt.RIdentifier, f.FactId ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
            " SELECT pt.RIdentifier as taxonId, f.* " +
            " FROM Fact f " +
                 " LEFT JOIN PTaxon pt ON f.PTNameFk =pt.PTNameFk AND f.PTRefFk = pt.PTRefFk " +
             " WHERE f.FactCategoryFk = 202 AND (f.FactId IN (" + ID_LIST_TOKEN + ")  )"
            + " ORDER BY pt.RIdentifier, f.FactId "
            ;
		return strQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();

		Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		Map<String, DerivedUnit> ecoFactDerivedUnitMap = partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				Integer taxonId = nullSafeInt(rs, "taxonId");
				int factId = rs.getInt("FactId");
				Integer ecoFactId = nullSafeInt(rs, "ExtensionFk");
				String recordBasis = rs.getString("RecordBasis");


				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					DerivedUnit ecoFact = ecoFactDerivedUnitMap.get(String.valueOf(ecoFactId));
					if (ecoFact == null){
						logger.warn("EcoFact is null for EcoFact: " + CdmUtils.Nz(ecoFactId) + ", taxonId: " + CdmUtils.Nz(taxonId));
					}

					//description element
					if (taxonId != null){
						Taxon taxon = getTaxon(state, taxonId, taxonMap, factId);

						if(taxon != null){
							DerivedUnit identifiedSpecimen = makeIdentifiedSpecimen(ecoFact, recordBasis, taxonId, ecoFactId);

							makeDetermination(state, rs, taxon, identifiedSpecimen, factId, partitioner);

							makeIndividualsAssociation(state, taxon, sourceRef, identifiedSpecimen);

							this.doIdCreatedUpdatedNotes(state, identifiedSpecimen, rs, factId, getDerivedUnitNameSpace());

							identifiedSpecimen.setCacheStrategy(DerivedUnitDefaultCacheStrategy.NewInstance());
							taxaToSave.add(taxon);
						}
					}else{
						logger.warn("No taxon defined for ecology fact: " +  factId);
					}


				} catch (Exception e) {
					logger.warn("Exception in FactEcology: FactId " + factId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn("Taxa to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void makeIndividualsAssociation(AlgaTerraImportState state, Taxon taxon, Reference sourceRef, DerivedUnit identifiedSpecimen){
		TaxonDescription taxonDescription = getTaxonDescription(state, taxon, sourceRef);
		IndividualsAssociation indAssociation = IndividualsAssociation.NewInstance();
		Feature feature = makeFeature(identifiedSpecimen.getRecordBasis(), state);
		indAssociation.setAssociatedSpecimenOrObservation(identifiedSpecimen);
		indAssociation.setFeature(feature);
		taxonDescription.addElement(indAssociation);
	}

	private void makeDetermination(AlgaTerraImportState state, ResultSet rs, Taxon taxon, DerivedUnit identifiedSpecimen, int factId, ResultSetPartitioner partitioner) throws SQLException {
		Date identifiedWhen = rs.getDate("IdentifiedWhen");
		Date identifiedWhenEnd = rs.getDate("IdentiedWhenEnd");
		boolean restrictedFlag = rs.getBoolean("RestrictedFlag");
		//Team FK ist immer null
		String identifiedBy = rs.getString("IdentifiedBy");
		String identificationReference = rs.getString("IdentificationReference");
		Integer refFk = nullSafeInt(rs, "IdentifidationRefFk");


		DeterminationEvent determination = DeterminationEvent.NewInstance(taxon, identifiedSpecimen);
		TimePeriod determinationPeriod = TimePeriod.NewInstance(identifiedWhen, identifiedWhenEnd);
		determination.setTimeperiod(determinationPeriod);
		determination.setPreferredFlag(! restrictedFlag);
		//TODO

		TeamOrPersonBase<?> author = getAuthor(identifiedBy);
		determination.setDeterminer(author);
		if (refFk != null){
			Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

			Reference ref = refMap.get(String.valueOf(refFk));
			if (ref != null){
				determination.addReference(ref);
			}else{
				logger.warn("Ref not found for Determination Event");
			}
		}else{
			//IdentificationReference is not to be handled according to Henning
			if (StringUtils.isNotBlank(identificationReference)){
				logger.warn("IdentificationReference exists without IdentificationRefFk. FactId: "+  factId);
			}
		}



		//TODO
//		kind of identification, IdentificationUncertainty, IdentificationMethod,


	}



	private DerivedUnit makeIdentifiedSpecimen(DerivedUnit ecoFact, String recordBasis, Integer taxonId, Integer ecoFactId) {
		//TODO event type
		DerivationEvent event = DerivationEvent.NewInstance(DerivationEventType.ACCESSIONING());
		SpecimenOrObservationType derivedUnitType = makeDerivedUnitType(recordBasis);
		if (derivedUnitType == null){
			String message = "derivedUnitType is NULL for recordBasis (%s). Use dummy type instead. (TaxonId = %s)";
			logger.warn(String.format(message, recordBasis, taxonId));
			derivedUnitType = SpecimenOrObservationType.PreservedSpecimen;
		}

		DerivedUnit result = DerivedUnit.NewInstance(derivedUnitType);
		result.setDerivedFrom(event);
		if (ecoFact == null){
			String message = "EcoFact (%s) is null for taxonId (%s)";
			logger.warn(String.format(message, ecoFactId, taxonId));
		}else{
			ecoFact.addDerivationEvent(event);
		}
		return result;
	}



	@Override
    protected String getDerivedUnitNameSpace(){
		return FACT_ECOLOGY_NAMESPACE;
	}

	@Override
    protected String getFieldObservationNameSpace(){
		return null;
	}




	/**
	 * @param state
	 * @param ecoFactId
	 * @param derivedUnitMap
	 * @param type
	 * @return
	 */
	private DerivedUnitFacade getDerivedUnit(AlgaTerraImportState state, int ecoFactId, Map<String, DerivedUnit> derivedUnitMap, SpecimenOrObservationType type) {
		String key = String.valueOf(ecoFactId);
		DerivedUnit derivedUnit = derivedUnitMap.get(key);
		DerivedUnitFacade facade;
		if (derivedUnit == null){
			facade = DerivedUnitFacade.NewInstance(type);
			derivedUnitMap.put(key, derivedUnit);
		}else{
			try {
				facade = DerivedUnitFacade.NewInstance(derivedUnit);
			} catch (DerivedUnitFacadeNotSupportedException e) {
				logger.error(e.getMessage());
				facade = DerivedUnitFacade.NewInstance(type);
			}
		}

		return facade;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> extensionFkSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
				handleForeignKey(rs, extensionFkSet, "extensionFk");
				handleForeignKey(rs, referenceIdSet, "IdentifidationRefFk");
			}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//derived unit map
			nameSpace = AlgaTerraSpecimenImportBase.ECO_FACT_DERIVED_UNIT_NAMESPACE;
			idSet = extensionFkSet;
			Map<String, DerivedUnit> derivedUnitMap = getCommonService().getSourcedObjectsByIdInSourceC(DerivedUnit.class, idSet, nameSpace);
			result.put(nameSpace, derivedUnitMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraSpecimenImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoFactEcology();
	}

}
