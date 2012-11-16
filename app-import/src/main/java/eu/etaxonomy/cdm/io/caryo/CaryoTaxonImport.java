/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.caryo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class CaryoTaxonImport  extends DbImportBase<CaryoImportState, CaryoImportConfigurator> {
	private static final Logger logger = Logger.getLogger(CaryoTaxonImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "taxa";
	private static final String dbTableName = "CARYOPHYLLALES";
	private static final String FAMILY = "Family";
	private static final String PERSON = "Person";
	private static final String TEAM = "Team";
	private static final String JOURNAL = "Journal";
	private static final String BOOK = "Book";

	
	private Map<String, Taxon> familyMap = new HashMap<String, Taxon>();
	private Map<String, TeamOrPersonBase> personMap = new HashMap<String, TeamOrPersonBase>();
	private Map<String, TeamOrPersonBase> teamMap = new HashMap<String, TeamOrPersonBase>();
	private Map<String, Reference> journalMap = new HashMap<String, Reference>();
	private Map<String, Reference> bookMap = new HashMap<String, Reference>();
	
	
	private Classification classification;

	
	public CaryoTaxonImport(){
		super(pluralString, dbTableName);
	}

	
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.DbImportBase#getIdQuery(eu.etaxonomy.cdm.io.common.DbImportStateBase)
	 */
	@Override
	protected String getIdQuery(CaryoImportState state) {
		String strRecordQuery = 
			" SELECT ID " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.DbImportBase#getRecordQuery(eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase)
	 */
	@Override
	protected String getRecordQuery(CaryoImportConfigurator config) {
		String strRecordQuery = 
			" SELECT cs.* " + 
			" FROM " + getTableName() + " t " +
			" WHERE ( t.ID IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#doPartition(eu.etaxonomy.cdm.io.common.ResultSetPartitioner, eu.etaxonomy.cdm.io.globis.GlobisImportState)
	 */
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, CaryoImportState state) {
		boolean success = true;
		
		Set<TaxonBase> objectsToSave = new HashSet<TaxonBase>();
		
//		Map<String, Taxon> taxonMap = (Map<String, Taxon>) partitioner.getObjectMap(TAXON_NAMESPACE);

		
		classification = getClassification(state);
		
		try {
			doFamilies(state);
			doAuthors(state);
			doJournals(state);
			doBooks(state);
			
			ResultSet rs = partitioner.getResultSet();
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer taxonId = rs.getInt("IDcurrentspec");
        		
        		
        		//String dtSpcJahr -> ignore !
        		//empty: fiSpcLiteratur
        		
        		//TODO
        		//fiSpcspcgrptax
        		
        	
        		
				try {
					
					
//					classification.addParentChild(nextHigherTaxon, species, sourceRef, null);
//					
//					
//					this.doIdCreatedUpdatedNotes(state, species, rs, taxonId, TAXON_NAMESPACE);
//					
//					objectsToSave.add(species); 
					

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







	private void doAuthors(CaryoImportState state) {
		// TODO Auto-generated method stub
		
	}




	private void doBooks(CaryoImportState state) {
		// TODO Auto-generated method stub
		
	}




	private void doJournals(CaryoImportState state) {
		// TODO Auto-generated method stub
		
	}




	private void doFamilies(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sqlFamily = "SELECT DISTINCT family FROM table WHERE family IS NOT NULL";
		ResultSet rs = source.getResultSet(sqlFamily);
		while (rs.next()){
			String family = rs.getString("family");
			BotanicalName name = BotanicalName.NewInstance(Rank.FAMILY());
			name.setGenusOrUninomial(family);
			Taxon taxon = Taxon.NewInstance(name, state.getTransactionalSourceReference());
			classification.addChildTaxon(taxon, null, null, null);
//			taxon.addSource(id, idNamespace, citation, null);
			
			
			familyMap.put(family, taxon);
		}
		
	}

	private Classification getClassification(CaryoImportState state) {
		if (this.classification == null){
			String name = state.getConfig().getClassificationName();
			Reference<?> reference = state.getTransactionalSourceReference();
			this.classification = Classification.NewInstance(name, reference, Language.DEFAULT());
			if (state.getConfig().getClassificationUuid() != null){
				classification.setUuid(state.getConfig().getClassificationUuid());
			}
			getClassificationService().save(classification);
		}
		return this.classification;
		
	}





	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
//		String nameSpace;
//		Class cdmClass;
//		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
//		try{
//			Set<String> taxonIdSet = new HashSet<String>();
//			
//			while (rs.next()){
////				handleForeignKey(rs, taxonIdSet, "taxonId");
//			}
//			
//			//taxon map
//			nameSpace = TAXON_NAMESPACE;
//			cdmClass = Taxon.class;
//			idSet = taxonIdSet;
//			Map<String, Taxon> objectMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
//			result.put(nameSpace, objectMap);
//
//			
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(CaryoImportState state){
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(CaryoImportState state){
		return ! state.getConfig().isDoTaxa();
	}







}
