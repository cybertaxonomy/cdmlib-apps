/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.globis.validation.GlobisCurrentSpeciesImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.ZoologicalName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class GlobisCommonNameImport  extends GlobisImportBase<Taxon> {
	private static final Logger logger = Logger.getLogger(GlobisCommonNameImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "common names";
	private static final String dbTableName = "species_language";
	private static final Class cdmTargetClass = Taxon.class;  //not needed
	
	public GlobisCommonNameImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strRecordQuery = 
			" SELECT ID " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery = 
			" SELECT * " + 
			" FROM " + getTableName() + " sl " +
			" WHERE ( sl.ID IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#doPartition(eu.etaxonomy.cdm.io.common.ResultSetPartitioner, eu.etaxonomy.cdm.io.globis.GlobisImportState)
	 */
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;
		
		Set<TaxonBase> objectsToSave = new HashSet<TaxonBase>();
		
		Map<String, Taxon> taxonMap = (Map<String, Taxon>) partitioner.getObjectMap(TAXON_NAMESPACE);
//		Map<String, DerivedUnit> ecoFactDerivedUnitMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		try {
			
			int i = 0;

			//for each common name
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer taxonId = rs.getInt("IdCrrentSpec");
        		
        		
        		//String dtSpcJahr -> ignore !
        		//empty: fiSpcLiteratur
        		
        		//TODO
        		//fiSpcspcgrptax
        		
        	
        		
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
			
					//species
					Taxon species = createObject(rs, state);
					
					
					handleCountries(state, rs, species);
					
					this.doIdCreatedUpdatedNotes(state, species, rs, taxonId, TAXON_NAMESPACE);
					
					objectsToSave.add(species); 
					

				} catch (Exception e) {
					logger.warn("Exception in current_species: IDcurrentspec " + taxonId + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void handleCountries(GlobisImportState state, ResultSet rs, Taxon species) throws SQLException {
		String countriesStr = rs.getString("dtSpcCountries");
		if (isBlank(countriesStr)){
			return;
		}
		String[] countriesSplit = countriesStr.split(";");
		for (String countryStr : countriesSplit){
			if (isBlank(countryStr)){
				continue;
			}
			countryStr = countryStr.trim();
			
			//TODO use isComplete
			boolean isComplete = countryStr.endsWith(".");
			if (isComplete){
				countryStr = countryStr.substring(0,countryStr.length() - 1).trim();
			}
			boolean isDoubtful = countryStr.endsWith("[?]");
			if (isDoubtful){
				countryStr = countryStr.substring(0,countryStr.length() - 3).trim();
			}
			if (countryStr.startsWith("?")){
				isDoubtful = true;
				countryStr = countryStr.substring(1).trim();
			}
			
			
			
			countryStr = normalizeCountry(countryStr);
			
			WaterbodyOrCountry country = getCountry(state, countryStr);
			
			PresenceTerm status;
			if (isDoubtful){
				status = PresenceTerm.PRESENT_DOUBTFULLY();
			}else{
				status = PresenceTerm.PRESENT();
			}
			
			if (country != null){
				TaxonDescription desc = getTaxonDescription(species, state.getTransactionalSourceReference(), false, true);
				Distribution distribution = Distribution.NewInstance(country, status);
				desc.addElement(distribution);
			}else{
				logger.warn("Country string not recognized: " + countryStr);
			}
		}
	}



	/**
	 * @param countryStr
	 * @return
	 */
	private String normalizeCountry(String countryStr) {
		String result = countryStr.trim();
		if (result.endsWith(".")){
			result = result.substring(0,result.length() - 1);
		}
		return result; 
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet, eu.etaxonomy.cdm.io.common.ImportStateBase)
	 */
	public Taxon createObject(ResultSet rs, GlobisImportState state)
			throws SQLException {
		String speciesEpi = rs.getString("dtSpcSpcakt");
		String subGenusEpi = rs.getString("dtSpcSubgenakt");
		String genusEpi = rs.getString("dtSpcGenusakt");
		String author = rs.getString("dtSpcAutor");
		
		
		ZoologicalName zooName = ZoologicalName.NewInstance(Rank.SPECIES());
		zooName.setSpecificEpithet(speciesEpi);
		if (StringUtils.isNotBlank(subGenusEpi)){
			zooName.setInfraGenericEpithet(subGenusEpi);
		}
		zooName.setGenusOrUninomial(genusEpi);
		handleAuthorAndYear(author, zooName);
		
		Taxon taxon = Taxon.NewInstance(zooName, state.getTransactionalSourceReference());
		
		return taxon;
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
//				handleForeignKey(rs, taxonIdSet, "taxonId");
			}
			
			//taxon map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = Taxon.class;
			idSet = taxonIdSet;
			Map<String, Taxon> objectMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(GlobisImportState state){
		IOValidator<GlobisImportState> validator = new GlobisCurrentSpeciesImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoCurrentTaxa();
	}





}
