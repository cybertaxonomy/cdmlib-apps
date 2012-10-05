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

import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.globis.validation.GlobisReferenceImportValidator;
import eu.etaxonomy.cdm.io.globis.validation.GlobisSpecTaxaImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class GlobisSpecTaxImport  extends GlobisImportBase<Reference> implements IMappingImport<Reference, GlobisImportState>{
	private static final Logger logger = Logger.getLogger(GlobisSpecTaxImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "taxa";
	private static final String dbTableName = "specTax";
	private static final Class cdmTargetClass = Reference.class;

	public GlobisSpecTaxImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strRecordQuery = 
			" SELECT specTaxId " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery = 
			" SELECT t.*, t.DateCreated as Created_When, t.CreatedBy as Created_Who," +
			"        t.ModifiedBy as Updated_who, t.DateModified as Updated_When, t.SpecRemarks as Notes " + 
			" FROM " + getTableName() + " t " +
			" WHERE ( t.refId IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#doPartition(eu.etaxonomy.cdm.io.common.ResultSetPartitioner, eu.etaxonomy.cdm.io.globis.GlobisImportState)
	 */
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;
		
		Set<Reference> objectsToSave = new HashSet<Reference>();
		
//		Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>) partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
//		Map<String, DerivedUnit> ecoFactDerivedUnitMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer refId = rs.getInt("RefId");
        		String title = rs.getString("RefTitle");
        		String refJournal = rs.getString("RefJournal");
				
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
				
					Reference<?> ref = createObject(rs, state);
					ref.setTitle(title);
					
					if (StringUtils.isNotBlank(refJournal)){
						if (ref.getType().equals(ReferenceType.Article) ){
							Reference<?> journal = getJournal(state, rs, refJournal);
							ref.setInJournal(journal);
						}else{
							logger.warn("Reference type not supported for journal: " + ref.getType().toString());
						}
					}
					
					this.doIdCreatedUpdatedNotes(state, ref, rs, refId, REFERENCE_NAMESPACE);
					
					
					
					//DONE
//					RefType, RefTitle, RefJournal
					
					//TODO
					//Refid,CreatedBy,DateCreated,DateModified,ModifiedBy
					//RefAuthor,RefBookTitle,RefDatePublished
					//RefEdition, RefEditor, RefGeneralKeywords
					//RefGeoKeywords, RefIll only,RefISSN, RefJournal
					//RefLibrary, RefMarker,RefPages,RefPages only,
					//RefPlace, RefPublisher, RefRemarks,
					//RefSerial, RefSource, RefSpecificKeywords, RefTaxKeywords,
					//RefURL, RefVolPageFig, RefVolume, RefYear
					//SpecificKeywordDummy
					
					//no data
						//CountryDummy
					
					objectsToSave.add(ref); 
					

				} catch (Exception e) {
					logger.warn("Exception in literature: RefId " + refId + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getReferenceService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}



	
	private Reference<?> getJournal(GlobisImportState state, ResultSet rs, String refJournal) throws SQLException {
		
		
		Reference<?> journal = ReferenceFactory.newJournal();
		String issn = rs.getString("RefISSN");
		if (StringUtils.isNotBlank(issn)){
			issn.replaceAll("ISSN", "").trim();
			journal.setIssn(issn);			
		}

		
		
		//TODO deduplicate
		journal.setTitle(refJournal);
		return journal;
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet, eu.etaxonomy.cdm.io.common.ImportStateBase)
	 */
	public Reference<?> createObject(ResultSet rs, GlobisImportState state)
			throws SQLException {
		Reference<?> ref;
		String refType = rs.getString("RefType");
		if (refType == null){
			ref = ReferenceFactory.newGeneric();
		}else if (refType == "book"){
			ref = ReferenceFactory.newBook();
		}else if (refType == "paper in journal"){
			ref = ReferenceFactory.newArticle();
		}else if (refType.startsWith("unpublished") ){
			ref = ReferenceFactory.newGeneric();
		}else if (refType.endsWith("paper in journal")){
			ref = ReferenceFactory.newArticle();
		}else if (refType == "paper in book"){
			ref = ReferenceFactory.newBookSection();
		}else if (refType == "paper in journalwebsite"){
			ref = ReferenceFactory.newArticle();
		}else{
			logger.warn("Unknown reference type: " + refType);
			ref = ReferenceFactory.newGeneric();
		}
		return ref;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		return result;  //not needed
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(GlobisImportState state){
		IOValidator<GlobisImportState> validator = new GlobisSpecTaxaImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoSpecTaxa();
	}





}
