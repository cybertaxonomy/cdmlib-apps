/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_FACT_ALSO_PUBLISHED_IN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_FACT_BIBLIOGRAPHY;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_FACT_PROTOLOGUE;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.media.CdmImageInfo;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelNameFactsImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.ExternalLinkType;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelNameFactsImport  extends BerlinModelImportBase  {
    private static final long serialVersionUID = 4174085686074314138L;

    private static final Logger logger = Logger.getLogger(BerlinModelNameFactsImport.class);

	public static final String NAMESPACE = "NameFact";

	/**
	 * write info message after modCount iterations
	 */
	private int modCount = 500;
	private static final String pluralString = "name facts";
	private static final String dbTableName = "NameFact";


	public BerlinModelNameFactsImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		if (isNotBlank(state.getConfig().getNameIdTable())){
			String result = super.getIdQuery(state);
			result += " WHERE ptNameFk IN (SELECT NameId FROM " + state.getConfig().getNameIdTable() + ")";
			if (state.getConfig().isEuroMed()){
			    result += " AND NOT (NameFactCategoryFk = 3 AND NameFactRefFk = 8500000) ";  //#7796#note-11
			}
			return result;
		}else{
			return super.getIdQuery(state);
		}
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strQuery =
			" SELECT NameFact.*, Name.NameID as nameId, NameFactCategory.NameFactCategory " +
			" FROM NameFact INNER JOIN " +
              	" Name ON NameFact.PTNameFk = Name.NameId  INNER JOIN "+
              	" NameFactCategory ON NameFactCategory.NameFactCategoryID = NameFact.NameFactCategoryFK " +
            " WHERE (NameFactId IN ("+ ID_LIST_TOKEN+") )";
		return strQuery;
	}

	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		BerlinModelImportConfigurator config = state.getConfig();
		Set<TaxonName> nameToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, TaxonName> nameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		Reference sourceRef = state.getTransactionalSourceReference();
		try {
			int i = 0;
			//for each reference
			while (rs.next() && (config.getMaximumNumberOfNameFacts() == 0 || i < config.getMaximumNumberOfNameFacts())){

				if ((i++ % modCount) == 0  && i!= 1 ){ logger.info("NameFacts handled: " + (i-1));}

				int nameFactId = rs.getInt("nameFactId");
				int nameId = rs.getInt("nameId");
				Object nameFactRefFkObj = rs.getObject("nameFactRefFk");
				String nameFactRefDetail = rs.getString("nameFactRefDetail");

				String category = CdmUtils.Nz(rs.getString("NameFactCategory"));
				String nameFact = CdmUtils.Nz(rs.getString("nameFact"));

				TaxonName taxonName = nameMap.get(String.valueOf(nameId));
				String nameFactRefFk = String.valueOf(nameFactRefFkObj);
				Reference citation = refMap.get(nameFactRefFk);

				if (taxonName != null){
					//PROTOLOGUE
					if (category.equalsIgnoreCase(NAME_FACT_PROTOLOGUE)){

					    String uriString = config.getMediaUrl() + "/" + nameFact;
						try{
						    //this depends on specific project implementation, maybe also config.getMediaPath() is important
						    URI uri = URI.create(uriString);
							taxonName.addProtologue(uri, null, ExternalLinkType.Unknown);
						}catch(IllegalArgumentException e){
							logger.warn("Incorrect protologue URI: " + uriString);
							success = false;
						}
						//end NAME_FACT_PROTOLOGUE
					}else if (category.equalsIgnoreCase(NAME_FACT_ALSO_PUBLISHED_IN)){
						if (StringUtils.isNotBlank(nameFact)){
							TaxonNameDescription description = TaxonNameDescription.NewInstance();
							TextData additionalPublication = TextData.NewInstance(Feature.ADDITIONAL_PUBLICATION());
							//TODO language
							Language language = Language.DEFAULT();
							additionalPublication.putText(language, nameFact);
							additionalPublication.addSource(OriginalSourceType.Import, String.valueOf(nameFactId), NAMESPACE, null,null, null, null);
							if (citation != null || isNotBlank(nameFactRefDetail)){
								additionalPublication.addSource(OriginalSourceType.PrimaryTaxonomicSource, null, null, citation, nameFactRefDetail, null, null);
							}
							description.addElement(additionalPublication);
							taxonName.addDescription(description);
						}
					}else if (category.equalsIgnoreCase(NAME_FACT_BIBLIOGRAPHY)){
						if (isNotBlank(nameFact)){
							TaxonNameDescription description = TaxonNameDescription.NewInstance();
							TextData bibliography = TextData.NewInstance(Feature.CITATION());
							//TODO language
							Language language = Language.DEFAULT();
							bibliography.putText(language, nameFact);
							bibliography.addSource(OriginalSourceType.Import, String.valueOf(nameFactId), NAMESPACE, null,null, null, null);
							if (citation != null || isNotBlank(nameFactRefDetail)){
								bibliography.addSource(OriginalSourceType.PrimaryTaxonomicSource, null, null, citation, nameFactRefDetail, null, null);
							}
							description.addElement(bibliography);
							taxonName.addDescription(description);
						}
					}else {
						//TODO
						logger.warn("NameFactCategory '" + category + "' not yet implemented");
						success = false;
					}

					//TODO
//					DoubtfulFlag    bit        Checked
//					PublishFlag      bit        Checked
//					Created_When  datetime           Checked
//					Updated_When datetime           Checked
//					Created_Who    nvarchar(255)    Checked
//					Updated_Who  nvarchar(255)    Checked
//					Notes      nvarchar(1000)           Checked

					nameToSave.add(taxonName);
				}else{
					//TODO
					logger.warn("TaxonName for NameFact " + nameFactId + " does not exist in store");
					success = false;
				}
				//put
			}
			if (config.getMaximumNumberOfNameFacts() != 0 && i >= config.getMaximumNumberOfNameFacts() - 1){
				logger.warn("ONLY " + config.getMaximumNumberOfNameFacts() + " NAMEFACTS imported !!!" )
			;}
			logger.info("Names to save: " + nameToSave.size());
			getNameService().save(nameToSave);
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "PTnameFk");
				handleForeignKey(rs, referenceIdSet, "nameFactRefFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

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

	//FIXME gibt es da keine allgemeine Methode in common?
	public Media getMedia(String nameFact, URL mediaUrl, File mediaPath){
		if (mediaUrl == null){
			logger.warn("Media Url should not be null");
			return null;
		}
		String mimeTypeTif = "image/tiff";
		String mimeTypeJpg = "image/jpeg";
		String mimeTypePng = "image/png";
		String mimeTypePdf = "application/pdf";
		String suffixTif = "tif";
		String suffixJpg = "jpg";
		String suffixPng = "png";
		String suffixPdf = "pdf";

		String sep = File.separator;
		Integer size = null;

		logger.debug("Getting media for NameFact: " + nameFact);

		Media media = Media.NewInstance();

		String mediaUrlString = mediaUrl.toString();

		//tiff
		String urlStringTif = mediaUrlString + "tif/" + nameFact + "." + suffixTif;
		File file = new File(mediaPath, "tif" + sep + nameFact + "." + suffixTif);
		MediaRepresentation representationTif = MediaRepresentation.NewInstance(mimeTypeTif, suffixTif);
		if (file.exists()){
			representationTif.addRepresentationPart(makeImage(urlStringTif, size, file));
		}
		if(representationTif.getParts().size() > 0){
			media.addRepresentation(representationTif);
		}
		// end tif
		// jpg
		boolean fileExists = true;
		int jpgCount = 0;
		MediaRepresentation representationJpg = MediaRepresentation.NewInstance(mimeTypeJpg, suffixJpg);
		while(fileExists){
			String urlStringJpeg = mediaUrlString + "cmd_jpg/" + nameFact + "_page_000" + jpgCount + "." + suffixJpg;
			file = new File(mediaPath, "cmd_jpg" + sep + nameFact + "_page_000" + jpgCount + "." + suffixJpg);
			jpgCount++;
			if (file.exists()){
				representationJpg.addRepresentationPart(makeImage(urlStringJpeg, size, file));
			}else{
				fileExists = false;
			}
		}
		if(representationJpg.getParts().size() > 0){
			media.addRepresentation(representationJpg);
		}
		// end jpg
		//png
		String urlStringPng = mediaUrlString + "png/" + nameFact + "." + suffixPng;
		file = new File(mediaPath, "png" + sep + nameFact + "." + suffixPng);
		MediaRepresentation representationPng = MediaRepresentation.NewInstance(mimeTypePng, suffixPng);
		if (file.exists()){
			representationPng.addRepresentationPart(makeImage(urlStringPng, size, file));
		}else{
			fileExists = true;
			int pngCount = 0;
			while (fileExists){
				pngCount++;
				urlStringPng = mediaUrlString + "png/" + nameFact + "00" + pngCount + "." + suffixPng;
				file = new File(mediaPath, "png" + sep + nameFact + "00" + pngCount + "." + suffixPng);

				if (file.exists()){
					representationPng.addRepresentationPart(makeImage(urlStringPng, size, file));
				}else{
					fileExists = false;
				}
			}
		}
		if(representationPng.getParts().size() > 0){
			media.addRepresentation(representationPng);
		}
		//end png
        //pdf
        String urlStringPdf = mediaUrlString + "pdf/" + nameFact + "." + suffixPdf;
        URI uriPdf;
		try {
			uriPdf = new URI(urlStringPdf);
			file = new File(mediaPath, "pdf" + sep + nameFact + "." + suffixPdf);
	        MediaRepresentation representationPdf = MediaRepresentation.NewInstance(mimeTypePdf, suffixPdf);
	        if (file.exists()){
	                representationPdf.addRepresentationPart(MediaRepresentationPart.NewInstance(uriPdf, size));
	        }else{
	                fileExists = true;
	                int pdfCount = 0;
	                while (fileExists){
	                        pdfCount++;
	                        urlStringPdf = mediaUrlString + "pdf/" + nameFact + "00" + pdfCount + "." + suffixPdf;
	                        file = new File(mediaPath, "pdf/" + sep + nameFact + "00" + pdfCount + "." + suffixPdf);

	                        if (file.exists()){
	                                representationPdf.addRepresentationPart(MediaRepresentationPart.NewInstance(uriPdf, size));
	                        }else{
	                                fileExists = false;
	                        }
	                }
	        }
			if(representationPdf.getParts().size() > 0){
	        	media.addRepresentation(representationPdf);
	        }
		} catch (URISyntaxException e) {
			e.printStackTrace();
			logger.error("URISyntaxException" + urlStringPdf);
		}
        //end pdf

		if(logger.isDebugEnabled()){
			for (MediaRepresentation rep : media.getRepresentations()){
				for (MediaRepresentationPart part : rep.getParts()){
					logger.debug("in representation: " + part.getUri());
				}
			}
		}

		return media;
	}

	private ImageFile makeImage(String imageUri, Integer size, File file){
		CdmImageInfo imageMetaData = null;
		URI uri;
		try {
			uri = new URI(imageUri);
			try {
				imageMetaData = CdmImageInfo.NewInstance(uri, 0);
			} catch (IOException e) {
				logger.error("IOError reading image metadata." , e);
			} catch (HttpException e) {
				logger.error("HttpException reading image metadata." , e);
			}
			ImageFile image = ImageFile.NewInstance(uri, size, imageMetaData);
			return image;
		} catch (URISyntaxException e1) {
			logger.warn("URISyntaxException: " + imageUri);
			return null;
		}

	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelNameFactsImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoNameFacts();
	}

	//for testing only
	public static void main(String[] args) {

		BerlinModelNameFactsImport nf = new BerlinModelNameFactsImport();

		URL url;
		try {
			url = new URL("http://wp5.e-taxonomy.eu/dataportal/cichorieae/media/protolog/");
			File path = new File("/Volumes/protolog/protolog/");
			if(path.exists()){
				String fact = "Acanthocephalus_amplexifolius";
				// make getMedia public for this to work
				Media media = nf.getMedia(fact, url, path);
				logger.info(media);
				for (MediaRepresentation rep : media.getRepresentations()){
					logger.info(rep.getMimeType());
					for (MediaRepresentationPart part : rep.getParts()){
						logger.info(part.getUri());
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
