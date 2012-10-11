/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.net.MalformedURLException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.globis.validation.GlobisImageImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class GlobisImageImport  extends GlobisImportBase<Taxon> {
	private static final Logger logger = Logger.getLogger(GlobisImageImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "images";
	private static final String dbTableName = "Einzelbilder";
	private static final Class cdmTargetClass = Media
	.class;  //not needed
	
	private static final String IMAGE_NAMESPACE = "Einzelbilder";
	
	public GlobisImageImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strRecordQuery = 
			" SELECT BildId " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery = 
			" SELECT i.*, NULL as Created_When, NULL as Created_Who," +
				"  NULL as Updated_who, NULL as Updated_When, NULL as Notes " + 
			" FROM " + getTableName() + " i " +
			" WHERE ( i.BildId IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#doPartition(eu.etaxonomy.cdm.io.common.ResultSetPartitioner, eu.etaxonomy.cdm.io.globis.GlobisImportState)
	 */
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;
		
		Set<Media> objectsToSave = new HashSet<Media>();
		
//		Map<String, Taxon> taxonMap = (Map<String, Taxon>) partitioner.getObjectMap(TAXON_NAMESPACE);
//		Map<String, DerivedUnit> ecoFactDerivedUnitMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();
		
		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer bildID = rs.getInt("BildID");
        		
        		Integer spectaxID = nullSafeInt(rs, "spectaxID");
        		
        		//ignore: [file lab2], same as Dateiname04 but less data
        		
        		
        		
        		try {
					
        			//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
					
					//make image path
					String pathShort = rs.getString("Dateipfad_kurz");
					String fileOS = rs.getString("file OS");
					pathShort.replace(fileOS, "");
					//TODO move to config
					String newPath = "http://globis-images.insects-online.de/images/";
					String path = pathShort.replace("image:Webversionen", newPath);
					
					
					
					Media media1 = makeMedia(state, rs, "file OS", "Legende 1", path );
        			Media media2 = makeMedia(state, rs, "Dateiname02", "Legende 2", path );
        			Media media3 = makeMedia(state, rs, "Dateiname03", "Legende 3", path );
        			Media media4 = makeMedia(state, rs, "Dateiname04", "Legende 4", path );
        			
					
        			//TODO
					this.doIdCreatedUpdatedNotes(state, media1, rs, bildID, IMAGE_NAMESPACE);
					
					save(objectsToSave, media1);
					
										

				} catch (Exception e) {
					logger.warn("Exception in Einzelbilder: bildID " + bildID + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getMediaService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void save(Set<Media> objectsToSave, Media media) {
		if (media != null){
			objectsToSave.add(media); 
		}
	}




	private Media makeMedia(GlobisImportState state, ResultSet rs, String fileNameAttr, String legendAttr, String path) throws SQLException {
		Media media = null;
		String fileName = rs.getString(fileNameAttr);
		String legend = rs.getString(legendAttr);
		
		URI uri = URI.create(path+fileName); 
		
//		Media media = ImageInfo.NewInstanceWithMetaData(uri, null);
		
		try {
			media = this.getImageMedia(uri.toString(), true, false);
			media.putTitle(Language.ENGLISH(), legend);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return media;
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
		IOValidator<GlobisImportState> validator = new GlobisImageImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoImages();
	}





}
