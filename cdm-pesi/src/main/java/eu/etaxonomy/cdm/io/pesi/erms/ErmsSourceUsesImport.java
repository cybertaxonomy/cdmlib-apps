/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_ADDITIONAL_SOURCE;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_BASIS_OF_RECORD;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_EMENDATION;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_NEW_COMBINATION_REFERENCE;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_ORIGINAL_DESCRIPTION;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_REDESCRIPTION;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_SOURCE_OF_SYNONYMY;
import static eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer.SOURCE_USE_STATUS_SOURCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsSourceUsesImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsSourceUsesImport  extends ErmsImportBase<CommonTaxonName> {
    private static final long serialVersionUID = -5139234838768878653L;

    private static final Logger logger = Logger.getLogger(ErmsSourceUsesImport.class);

//	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping; //not needed

	private static final String pluralString = "source uses";
	private static final String dbTableName = "tu_sources";
	private static final Class<?> cdmTargetClass = null;

	public ErmsSourceUsesImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT sourceuse_id, source_id, tu_id " + " " +
						" FROM tu_sources " +
						" ORDER BY sourceuse_id, source_id, tu_id  ";
		return strQuery;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM tu_sources INNER JOIN sourceuses ON tu_sources.sourceuse_id = sourceuses.sourceuse_id" +
			" WHERE ( tu_sources.sourceuse_id IN (" + ID_LIST_TOKEN + ") AND " +
			" 		tu_sources.source_id IN (" + ID_LIST_TOKEN + ") AND " +
			"		tu_sources.tu_id IN (" + ID_LIST_TOKEN + ")  )";
		return strRecordQuery;
	}


	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, ErmsImportState state) {
		boolean success = true ;
		Set objectsToSave = new HashSet<>();

// 		DbImportMapping<?, ?> mapping = getMapping();
//		mapping.initialize(state, cdmTargetClass);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
				//success &= mapping.invoke(rs,referencesToSave);

				//read and normalize values
				int sourceUseId = rs.getInt("sourceuse_id");
				int sourceId = rs.getInt("source_id");
				String strSourceId = String.valueOf(sourceId);
				int taxonId = rs.getInt("tu_id");
				String strTaxonId = String.valueOf(taxonId);
				String strPageNr = rs.getString("pagenr");
				if (StringUtils.isBlank(strPageNr)){
					strPageNr = null;
				}
				Reference ref = (Reference)state.getRelatedObject(ErmsImportBase.REFERENCE_NAMESPACE, strSourceId);

				try {
				IdentifiableEntity<?> objectToSave = null;
				//invoke methods for each sourceUse type
				if (sourceUseId == SOURCE_USE_ORIGINAL_DESCRIPTION){
					objectToSave = makeOriginalDescription(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_BASIS_OF_RECORD){
					objectToSave = makeBasisOfRecord(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_ADDITIONAL_SOURCE){
					objectToSave = makeAdditionalSource(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_SOURCE_OF_SYNONYMY){
					objectToSave = makeSourceOfSynonymy(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_REDESCRIPTION){
					objectToSave = makeRedescription(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_NEW_COMBINATION_REFERENCE){
					objectToSave = makeCombinationReference(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_STATUS_SOURCE){
					objectToSave = makeStatusSource(partitioner, state, ref, strTaxonId, strPageNr);
				}else if (sourceUseId == SOURCE_USE_EMENDATION){
					objectToSave = makeEmendation(partitioner, state, ref, strTaxonId, strPageNr);
				}
				if(objectToSave != null){
					objectsToSave.add(objectToSave);
				}
				} catch (Exception e) {
					e.printStackTrace();
					success = false;
			}
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		partitioner.startDoSave();
		getCommonService().save(objectsToSave);
		return success;
	}


	/**
	 * @param strTaxonId
	 * @param ref
	 * @param state
	 * @param partitioner
	 * @param strPageNr
	 *
	 */
	private TaxonName makeOriginalDescription(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
	    TaxonName taxonName = (TaxonName)state.getRelatedObject(ErmsImportBase.NAME_NAMESPACE, strTaxonId);
		taxonName.setNomenclaturalReference(ref);
		taxonName.setNomenclaturalMicroReference(strPageNr);
		return taxonName;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private boolean isFirstBasisOfRecord = true;
	@SuppressWarnings("unused")
    private IdentifiableEntity<?> makeBasisOfRecord(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		if (isFirstBasisOfRecord){
			logger.warn("Basis of record not yet implemented");
			isFirstBasisOfRecord = false;
		}
		return null;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private IdentifiableEntity<?> makeAdditionalSource(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		Feature citationFeature = Feature.CITATION();
		DescriptionElementBase element = TextData.NewInstance(citationFeature);
		DescriptionElementSource source = element.addSource(OriginalSourceType.PrimaryTaxonomicSource, null, null, ref, strPageNr);
		if (source == null){
			logger.warn("Source is null");
			return null;
		}
		TaxonBase<?> taxonBase = (TaxonBase<?>)state.getRelatedObject(ErmsImportBase.TAXON_NAMESPACE, strTaxonId);
		Taxon taxon;

		//if taxon base is a synonym, add the description to the accepted taxon
		if (taxonBase.isInstanceOf(Synonym.class)){
			Synonym synonym = CdmBase.deproxy(taxonBase, Synonym.class);
			taxon = synonym.getAcceptedTaxon();
			if (taxon == null){
				String warning = "Synonym "+ strTaxonId + " has no accepted taxon";
				logger.warn(warning);
				return null;
				//throw new IllegalStateException(warning);
			}
			//add synonym name as name used in source
			source.setNameUsedInSource(synonym.getName());
		}else{
			taxon = (Taxon)taxonBase;
		}

		//get or create description and add the element
		TaxonDescription description;
		if (taxon.getDescriptions().size() > 0){
			description = taxon.getDescriptions().iterator().next();
		}else{
			description = TaxonDescription.NewInstance(taxon);
		}
		description.addElement(element);
		return taxon;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private IdentifiableEntity<?> makeSourceOfSynonymy(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		TaxonBase<?> taxonBase = (TaxonBase<?>)state.getRelatedObject(ErmsImportBase.TAXON_NAMESPACE, strTaxonId);
		if (taxonBase == null){
			String warning = "taxonBase (id = " + strTaxonId + ") could not be found ";
			logger.warn(warning);
			return null;
		}else if (! taxonBase.isInstanceOf(Synonym.class)){
			String message = "TaxonBase is not of class Synonym but " + taxonBase.getClass().getSimpleName();
			logger.info(message);
			Taxon taxon =CdmBase.deproxy(taxonBase, Taxon.class);
			Set<TaxonRelationship> synRels = taxon.getTaxonRelations();
			if (synRels.size() != 1){
				logger.warn("TaxonSynonym (" + strTaxonId + ") has not 1 but " + synRels.size() + " relations!");
			}else{
				TaxonRelationship synRel = synRels.iterator().next();
				synRel.setCitation(ref);
				synRel.setCitationMicroReference(strPageNr);
			}
		}else{
			Synonym synonym =CdmBase.deproxy(taxonBase, Synonym.class);
			synonym.setSec(ref);
			synonym.setSecMicroReference(strPageNr);
		}

		return taxonBase;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private boolean isFirstRediscription = true;
	@SuppressWarnings("unused")
    private IdentifiableEntity<?> makeRedescription(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		if (isFirstRediscription){
			logger.warn("Rediscription not yet implemented");
			isFirstRediscription = false;
		}
		return null;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private IdentifiableEntity<?> makeCombinationReference(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		// Kopie von Orig. Comb.
		//TODO ist das wirklich der richtige Name, oder muss ein verknüpfter Name verwendet werden
	    TaxonName taxonName = (TaxonName)state.getRelatedObject(ErmsImportBase.NAME_NAMESPACE, strTaxonId);
		taxonName.setNomenclaturalReference(ref);
		taxonName.setNomenclaturalMicroReference(strPageNr);
		return taxonName;
	}


	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private boolean isFirstStatusSource = true;
	@SuppressWarnings("unused")
    private IdentifiableEntity<?> makeStatusSource(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		if (isFirstStatusSource){
			logger.warn("StatusSource not yet implemented");
			isFirstStatusSource = false;
		}
		return null;
	}

	/**
	 * @param partitioner
	 * @param state
	 * @param ref
	 * @param strTaxonId
	 * @param strPageNr
	 */
	private boolean isFirstEmendation = true;
	@SuppressWarnings("unused")
    private IdentifiableEntity<?> makeEmendation(ResultSetPartitioner<?> partitioner, ErmsImportState state, Reference ref, String strTaxonId, String strPageNr) {
		if (isFirstEmendation){
			logger.warn("Emmendation not yet implemented");
			isFirstEmendation = false;
		}
		return null;
	}

	@SuppressWarnings("unused")
    public CommonTaxonName createObject(ResultSet rs, ErmsImportState state)
			throws SQLException {
		return null;  //not needed
	}


//************************************ RELATED OBJECTS *************************************************/

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
				handleForeignKey(rs, nameIdSet, "tu_id");
				handleForeignKey(rs, referenceIdSet, "source_id");
			}

			//name map
			nameSpace = ErmsImportBase.NAME_NAMESPACE;
			cdmClass = TaxonName.class;
			idSet = nameIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonName> nameMap = (Map<String, TaxonName>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//reference map
			nameSpace = ErmsImportBase.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;

	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsSourceUsesImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(ErmsImportState state){
		boolean result = state.getConfig().getDoReferences() != IImportConfigurator.DO_REFERENCES.ALL;
		result |= ! state.getConfig().isDoTaxa();
		return result;
	}

	@Override
	protected DbImportMapping<?, ?> getMapping() {
		logger.info("getMapping not implemented for EmrsSourceUsesImport");
		return null;  // not needed because Mapping is not implemented in this class yet
	}
}
