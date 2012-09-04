/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraSpecimenImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.03.2008
 * @version 1.0
 */
@Component
public class AlgaTerraSpecimenImport  extends BerlinModelImportBase {
	private static final Logger logger = Logger.getLogger(AlgaTerraSpecimenImport.class);

	public static final String NAMESPACE = "Occurrence";
	
	
	private static int modCount = 5000;
	private static final String pluralString = "specimen and observation";
	private static final String dbTableName = "Fact";  //??  


	public AlgaTerraSpecimenImport(){
		super();
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT factId " + 
				" FROM " + getTableName() + " INNER JOIN PTaxon ON Fact.PTNameFk = PTaxon.PTNameFk AND Fact.PTRefFk = PTaxon.PTRefFk "
				+ " WHERE FactCategoryFk = 202 "
				+ " ORDER BY PTaxon.RIdentifier, Fact.FactId ";
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =   //DISTINCT because otherwise emOccurrenceSource creates multiple records for a single distribution 
            " SELECT PTaxon.RIdentifier as taxonId, Fact.FactId, Fact.RecordBasis, EcoFact.* " + 
            " FROM Fact " + 
                 " INNER JOIN EcoFact ON Fact.ExtensionFk = EcoFact.EcoFactId " +
                 " INNER JOIN PTaxon ON dbo.Fact.PTNameFk = dbo.PTaxon.PTNameFk AND dbo.Fact.PTRefFk = dbo.PTaxon.PTRefFk " +
            " WHERE Fact.FactCategoryFk = 202 AND (Fact.FactId IN (" + ID_LIST_TOKEN + ")  )"  
            + " ORDER BY PTaxon.RIdentifier, Fact.FactId "
            ;
		return strQuery;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#doPartition(eu.etaxonomy.cdm.io.berlinModel.in.ResultSetPartitioner, eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState)
	 */
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true;
		Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();
		
		Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>) partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
			
		ResultSet rs = partitioner.getResultSet();

		try {
			int oldTaxonId = -1;
			TaxonDescription oldDescription = null;
			int i = 0;
			int countDescriptions = 0;
			int countSpecimen = 0;
			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("Specimen facts handled: " + (i-1));}
				
				int newTaxonId = rs.getInt("taxonId");
				int factId = rs.getInt("FactId");
				try {
							
					String recordBasis = rs.getString("RecordBasis");
					
					Reference<?> sourceRef = state.getTransactionalSourceReference();
					//create description(elements)
					TaxonDescription taxonDescription = getTaxonDescription(newTaxonId, oldTaxonId, oldDescription, taxonMap, factId, sourceRef);

					DerivedUnitType type = makeDerivedUnitType(recordBasis);
					DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(type);
					
					handleSingleSpecimen(rs, facade);
					
					IndividualsAssociation indAssociation = IndividualsAssociation.NewInstance();
					Feature feature = makeFeature(type);
					indAssociation.setAssociatedSpecimenOrObservation(facade.innerDerivedUnit());
					indAssociation.setFeature(feature);
					taxonDescription.addElement(indAssociation);
					
					if (taxonDescription != oldDescription){ 
						taxaToSave.add(taxonDescription.getTaxon()); 
						oldDescription = taxonDescription; 
						countDescriptions++; 
					}
				} catch (Exception e) {
					logger.warn("Exception in ecoFact: FactId " + factId + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn("Taxa to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}


	private void handleSingleSpecimen(ResultSet rs, DerivedUnitFacade facade) throws SQLException {
       	//TDWGGazetteerFK, CollectionFk, Collector, GeoCodeMethod, Prec, AltitudeMethod, Depth,
    	//ISOCountrySub, CollectionDate/End, WaterBody, 
    	//CreatedWhen/Who/Updated/who
    	
    	//P1-10Value/Unit/Parameter/Method

//		int factId = rs.getInt("factId");
        String locality = rs.getString("Locality");
        Double latitude = rs.getDouble("Latitude");
        Double longitude = rs.getDouble("Longitude");
        int errorRadius = rs.getInt("Prec");
    	Integer altitude = rs.getInt("Altitude");
    	String altitudeUnit = rs.getString("AltitudeUnit");
    	String collectorsNumber = rs.getString("CollectorsNumber");
    	
    	//location
    	facade.setLocality(locality);
    	    	
    	//exact location
    	ReferenceSystem referenceSystem = null;
    	Point exactLocation = Point.NewInstance(longitude, latitude, referenceSystem, errorRadius);
    	facade.setExactLocation(exactLocation);
    	
    	//altitude
    	if (StringUtils.isNotBlank(altitudeUnit) && ! altitudeUnit.trim().equalsIgnoreCase("m")){
    		logger.warn("Altitude unit is not [m] but: " +  altitudeUnit);
    	}
    	facade.setAbsoluteElevationRange(altitude, altitude);  //TODO
    	
    	//field
    	facade.setFieldNumber(collectorsNumber);
     	
	}
	
	private Feature makeFeature(DerivedUnitType type) {
		if (type.equals(DerivedUnitType.DerivedUnit)){
			return Feature.INDIVIDUALS_ASSOCIATION();
		}else if (type.equals(DerivedUnitType.FieldObservation) || type.equals(DerivedUnitType.Observation) ){
			return Feature.OBSERVATION();
		}else if (type.equals(DerivedUnitType.Fossil) || type.equals(DerivedUnitType.LivingBeing) || type.equals(DerivedUnitType.Specimen )){
			return Feature.SPECIMEN();
		}
		logger.warn("No feature defined for derived unit type: " + type);
		return null;
	}


	private DerivedUnitType makeDerivedUnitType(String recordBasis) {
		DerivedUnitType result = null;
		if (StringUtils.isBlank(recordBasis)){
			result = DerivedUnitType.DerivedUnit;
		} else if (recordBasis.equalsIgnoreCase("FossileSpecimen")){
			result = DerivedUnitType.Fossil;
		}else if (recordBasis.equalsIgnoreCase("HumanObservation")){
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("Literature")){
			logger.warn("Literature record basis not yet supported");
			result = DerivedUnitType.DerivedUnit;
		}else if (recordBasis.equalsIgnoreCase("LivingSpecimen")){
			result = DerivedUnitType.LivingBeing;
		}else if (recordBasis.equalsIgnoreCase("MachineObservation")){
			logger.warn("MachineObservation record basis not yet supported");
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("PreservedSpecimen")){
			result = DerivedUnitType.Specimen;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		String nameSpace;
		Class cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> taxonIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
			}
			
			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase> objectMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, objectMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	/**
	 * Use same TaxonDescription if two records belong to the same taxon 
	 * @param newTaxonId
	 * @param oldTaxonId
	 * @param oldDescription
	 * @param taxonMap
	 * @return
	 */
	private TaxonDescription getTaxonDescription(int newTaxonId, int oldTaxonId, TaxonDescription oldDescription, Map<String, TaxonBase> taxonMap, int factId, Reference<?> sourceSec){
		TaxonDescription result = null;
		if (oldDescription == null || newTaxonId != oldTaxonId){
			TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(newTaxonId));
			//TODO for testing
			//TaxonBase taxonBase = Taxon.NewInstance(BotanicalName.NewInstance(Rank.SPECIES()), null);
			Taxon taxon;
			if ( taxonBase instanceof Taxon ) {
				taxon = (Taxon) taxonBase;
			} else if (taxonBase != null) {
				logger.warn("TaxonBase for Fact(Specimen) with factId" + factId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
				return null;
			} else {
				logger.warn("TaxonBase for Fact(Specimen) " + factId + " is null.");
				return null;
			}		
			Set<TaxonDescription> descriptionSet= taxon.getDescriptions();
			if (descriptionSet.size() > 0) {
				result = descriptionSet.iterator().next(); 
			}else{
				result = TaxonDescription.NewInstance();
				result.setTitleCache(sourceSec.getTitleCache(), true);
				taxon.addDescription(result);
			}
		}else{
			result = oldDescription;
		}
		return result;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraSpecimenImportValidator();
		return validator.validate(state);
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getTableName()
	 */
	@Override
	protected String getTableName() {
		return dbTableName;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getPluralString()
	 */
	@Override
	public String getPluralString() {
		return pluralString;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoSpecimen();
	}
	
}
