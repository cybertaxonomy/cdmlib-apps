/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.common.UriUtils;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.globis.validation.GlobisImageImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.occurrence.Specimen;
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
				"  NULL as Updated_who, NULL as Updated_When, NULL as Notes, st.SpecCurrspecID " + 
			" FROM " + getTableName() + " i " +
				" LEFT JOIN specTax st ON i.spectaxID = st.SpecTaxID " +
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
		
		Map<String, Specimen> typeMap = (Map<String, Specimen>) partitioner.getObjectMap(GlobisSpecTaxImport.TYPE_NAMESPACE);
		
		Map<String, Taxon> taxonMap = (Map<String, Taxon>) partitioner.getObjectMap(TAXON_NAMESPACE);
//		Map<String, DerivedUnit> ecoFactDerivedUnitMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();
		
		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer bildID = rs.getInt("BildID");
        		
        		Integer spectaxID = nullSafeInt(rs, "spectaxID");
        		
        		Integer taxonID = nullSafeInt(rs, "SpecCurrspecID");
        		
        		String copyright = rs.getString("copyright");
        		
        		//ignore: 
        		//	[file lab2], same as Dateiname04 but less data
        		//	Dateipfad
        		Set<Media> recordMedia = new HashSet<Media>();
        		
        		try {
					
        			//make image path
					String pathShort = rs.getString("Dateipfad_kurz");
					String fileOS = rs.getString("file OS");
					pathShort= pathShort.replace(fileOS, "");
					String newPath = state.getConfig().getImageBaseUrl();
					String path = pathShort.replace("image:Webversionen/", newPath);
					
					Media singleMedia = makeMedia(state, rs, "file OS", "Legende 1", path, objectsToSave );
					recordMedia.add(singleMedia);
					singleMedia = makeMedia(state, rs, "Dateinamen02", "Legende 2", path, objectsToSave );
					recordMedia.add(singleMedia);
					singleMedia = makeMedia(state, rs, "Dateinamen03", "Legende 3", path, objectsToSave );
					recordMedia.add(singleMedia);
					singleMedia = makeMedia(state, rs, "Dateinamen04", "Legende 4", path, objectsToSave );
					recordMedia.add(singleMedia);
					
					if (spectaxID != null){
						String collectionCode = transformCopyright2CollectionCode(copyright);
						String id = GlobisSpecTaxImport.getTypeId(spectaxID, collectionCode);
						Specimen typeSpecimen = typeMap.get(id);
						if (typeSpecimen != null){
							DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(typeSpecimen);
							for (Media media: recordMedia){
								facade.addDerivedUnitMedia(media);
							}
						}else{
							//TODO
						}
					}else{
						//TODO
					}
					
					
					
					
				} catch (Exception e) {
					logger.warn("Exception in Einzelbilder: bildID " + bildID + ". " + e.getMessage());
					e.printStackTrace();
				} 
                
            }
           
			logger.info(pluralString + " to save: " + objectsToSave.size());
			getMediaService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private Media makeMedia(GlobisImportState state, ResultSet rs, String fileNameAttr, String legendAttr, String path, Set<Media> objectsToSave) throws SQLException {
		Media media = null;
		String fileName = rs.getString(fileNameAttr);
		String legend = rs.getString(legendAttr);
		Integer bildID = rs.getInt("BildID");
		
		URI uri = URI.create(path+fileName); 
		
//		Media media = ImageInfo.NewInstanceWithMetaData(uri, null);
		
		try {
			boolean readMediaData = state.getConfig().isDoReadMediaData();
			if (isBlank(legend) && readMediaData){
				if (UriUtils.isOk(UriUtils.getResponse(uri, null))){
					logger.warn("Image exists but legend is null " + uri + ", bildID" + bildID );
				}else{
					return null;
				}
			}
			
			media = this.getImageMedia(uri.toString(), readMediaData, false);
			media.putTitle(Language.ENGLISH(), legend);
			this.doIdCreatedUpdatedNotes(state, media, rs, bildID, IMAGE_NAMESPACE);
			
			objectsToSave.add(media);
			
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return media;
	}
	
	private String transformCopyright2CollectionCode(String copyright){
		
		if (isBlank(copyright)){
			return "";
		}else if(copyright.matches("Museum f.?r Naturkunde der Humboldt-Universit.?t, Berlin")){
			return "MFNB";
		}else if(copyright.matches("Staatliches Museum f.?r Tierkunde Dresden")){
			return "SMTD";
		}else if(copyright.equals("Natural History Museum, London")){
			return "BMNH";
		}else if(copyright.matches("Zoologische Staatssammlung M.?nchen")){
			return "ZSSM";
		}else if(copyright.matches("Staatliches Museum f.?r Naturkunde Karlsruhe")){
			return "SMNK";
		}else if(copyright.matches("Deutsches Entomologisches Institut M.?ncheberg")){
			return "DEIE";
		}else if(copyright.equals("Forschungsinstitut und Naturmuseum Senckenberg")){
			return "SMFM";
		}else if(copyright.matches("Mus.?um National d.?Histoire Naturelle, Paris")){
			return "MNHN";
		}else if(copyright.equals("Naturhistorisches Museum Wien")){
			return "NHMW";
		}else if(copyright.equals("Naturhistoriska Riksmuseet Stockholm")){
			return "NRMS";
		}else if(copyright.matches("Staatliches Museum f.?r Naturkunde Stuttgart")){
			return "SMNS";
		}else if(copyright.equals("United States National Museum of Natural History, Washington")){
			return "USNM";
		}else if(copyright.matches("Zentrum f.?r Biodokumentation des Saarlandes")){
			return "ZFBS";
		}else if(copyright.equals("Zoological Museum, University of Copenhagen")){
			return "ZMUC";
		}else if(copyright.equals("Zoologisches Forschungsinstitut und Museum \"Alexander Koenig\", Bonn")){
			return "ZFMK";
		}else if(copyright.equals("Zoologisches Forschungsmuseum \"Alexander Koenig\", Bonn")){
			return "ZFMK";
		}else if(copyright.matches("Zoologisches Institut der Martin-Luther-Universit.?t Halle-Wittenberg")){
			return "ZIUH";
		}else if(copyright.matches("Zoologisches Institut Universit.?t T.?bingen")){
			return "ZIUT";
		}else{
			logger.warn("Unknown copyright entry: " + copyright);
			return "";
		}

	
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
			Set<String> currSpecIdSet = new HashSet<String>();
			Set<String> typeIdSet = new HashSet<String>();
			
			while (rs.next()){
				handleForeignKey(rs, currSpecIdSet, "SpecCurrspecID");
				handleTypeKey(rs, typeIdSet, "spectaxID", "copyright");
			}
			
			//taxon map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = Taxon.class;
			idSet = currSpecIdSet;
			Map<String, Taxon> taxonMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//type map
			nameSpace = GlobisSpecTaxImport.TYPE_NAMESPACE;
			cdmClass = Specimen.class;
			idSet = typeIdSet;
			Map<String, Specimen> typeMap = (Map<String, Specimen>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, typeMap);
			
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	private void handleTypeKey(ResultSet rs, Set<String> idSet, String specTaxIdAttr, String copyrightAttr)
			throws SQLException {
		Integer specTaxId = nullSafeInt(rs, specTaxIdAttr);
		if (specTaxId != null){
			String copyright = rs.getString(copyrightAttr);
			if (isNotBlank(copyright)){
				String id  = GlobisSpecTaxImport.getTypeId(specTaxId, transformCopyright2CollectionCode(copyright));
				idSet.add(id);
			}
		}
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
