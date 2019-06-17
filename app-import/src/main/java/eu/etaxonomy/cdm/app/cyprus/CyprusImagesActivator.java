/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.cyprus;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.common.media.ImageInfo;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.media.RightsType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 05.2017
 */
public class CyprusImagesActivator {
	private static final Logger logger = Logger.getLogger(CyprusImagesActivator.class);


	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_cyprus();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cyprus();

	static boolean testOnly = false;
	static boolean update_notCreate = false;
	//if true, data will always be updated, if false, only missing data will be updated
	static boolean forceUpdate = false;

    private static final String path = "//media/digitalimages/EditWP6/Zypern/photos/";
    private static final String urlPath = "http://media.bgbm.org/erez/erez?src=EditWP6/zypern/photos/";

    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>> deduplicationHelper;


	private void doImport(ICdmDataSource cdmDestination){

		CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
		TransactionStatus tx = app.startTransaction();

		deduplicationHelper = (ImportDeduplicationHelper)ImportDeduplicationHelper.NewInstance(app);

        File file = new File(path);
        String[] fileList = file.list();
        Set<String> notFound = new HashSet<>();

        String regEx = "([A-Z][a-z]+_[a-z\\-]{3,}(?:_s_[a-z\\-]{3,})?)_[A-F]\\d{1,2}\\.(?:jpg|JPG)";
        Pattern pattern = Pattern.compile(regEx);

        for (String fileName : fileList){

            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()){
//                System.out.println(fileName);
                String taxonName = matcher.group(1);
                taxonName = taxonName.replace("_s_", " subsp. ").replace("_", " ");
                Taxon taxon = getAcceptedTaxon(app, taxonName);
                if (taxon == null){
                    if (!notFound.contains(taxonName)){
                        notFound.add(taxonName);
                        logger.warn("Taxon not found: " + taxonName);
                    }
                }else{
                    handleTaxon(app, taxon, fileName);
                }
            }else{
                if (!fileName.matches("(?:\\.erez|Thumbs\\.db.*|zypern_.*|__Keywords_template\\.txt)")){
                    logger.warn("Incorrect filename:" + fileName);
                }
            }
        }

//		app.getTaxonService().saveOrUpdate(taxaToSave);

		if (testOnly){
		    tx.setRollbackOnly();
		}
		app.commitTransaction(tx);
	}

    /**
     * @param app
     * @param taxon
     * @param fileName
     */
    private void handleTaxon(CdmApplicationController app, Taxon taxon, String fileName) {
        Set<String> urlStr = getAllExistingUrls(taxon);
        String fullName = urlPath + fileName;
        if (urlStr.contains(fullName)){
            return;
        }else{
            addMedia(app, taxon, fileName);
        }
    }

    /**
     * @param app
     * @param taxon
     * @param fileName
     */
    private void addMedia(CdmApplicationController app, Taxon taxon, String fileName) {
        try {
            String fullName = urlPath + fileName;
            Media media = getImageMedia(fullName, null, true);
            makeMetaData(media, fileName, false);
            makeTitle(media, fileName, false);
            if (!testOnly){
                makeTextData(fileName, media, taxon);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * @param media
     * @param fileName
     * @param b
     */
    private void makeTitle(Media media, String fileName, boolean updateOnly) {
        String title = fileName.replace("_s_"," subsp. ")
                .replace("_"," ").replace(".jpg","").replace(".JPG","");
        if ( (!updateOnly) || media.getAllTitles().isEmpty()){
            media.putTitle(Language.LATIN(), title);
        }
    }

    /**
     * @param media
     */
    private void makeMetaData(Media media, String fileName, boolean updateOnly) {

        File file = new File(path + fileName);
        if (!file.exists()){
            logger.warn("File for filename " +  fileName + " does not exist.");
            return;
        }

        Map<String, String> keywords = new HashMap<>();
        String copyright = null;
        String artistStr = null;
        String created = null;
        try{
//            IImageMetadata metadata = Sanselan.getMetadata(file);
            ImageMetadata metadata = Imaging.getMetadata(file);
            List<? extends ImageMetadataItem> items = metadata.getItems();
            for (Object object : items){
                ImageMetadataItem metadataItem = (ImageMetadataItem) object;
//                System.out.println(item.getKeyword() +  ":    " + item.getText());
                if (metadataItem instanceof GenericImageMetadataItem){
                    GenericImageMetadataItem item = (GenericImageMetadataItem) metadataItem;

                    String keyword = item.getKeyword().toLowerCase();
                    String value =removeQuots(item.getText());

                    if("keywords".equals(keyword)){
                        String[] splits = value.split(":");
                        if (splits.length == 2){
                            keywords.put(splits[0].trim().toLowerCase(), splits[1].trim());
                        }else{
                            logger.warn("Keyword has not correct format and can not be parsed: " + value +  "  for file " + fileName);
                        }
                    }else if ("Copyright Notice".equalsIgnoreCase(keyword)){
                        copyright = value;
                    }else if ("artist".equals(keyword)){
                        artistStr = value;
                    }else if ("date time original".equalsIgnoreCase(item.getKeyword())){
                        created = value;
                    }
                }
            }
        } catch (ImageReadException | IOException e1) {
            logger.warn("       Problem (" + e1.getMessage() + ") when reading metadata from file: " + fileName);
            e1.printStackTrace();
        }


        AgentBase<?> artistAgent = null;
        Rights right = null;
        DateTime createdDate = null;
        String locality = null;

        //artist
        if (keywords.get("photographer") != null){
            String artist = keywords.get("photographer");
            artistAgent = makePerson(artist, fileName);
        }
        if (artistStr != null){
            if (keywords.get("photographer") == null){
                artistAgent = makePerson(artistStr, fileName);
            }else if (!keywords.get("photographer").toLowerCase().replace(" ", "")
                    .contains(artistStr.toLowerCase().replace(" ", ""))){
                logger.warn("Artist '" + artistStr + "' could not be handled for " + fileName);
            }
        }

        //locality
        if (keywords.get("locality") != null){
            locality = keywords.get("locality");
        }

        //copyright
        if (copyright != null){
            AgentBase<?> agent;
            if (copyright.equals("Botanic Garden and Botanical Museum Berlin-Dahlem (BGBM)")){
                agent = Institution.NewNamedInstance(copyright);
            }else{
                agent = makePerson(copyright, fileName);
            }
            right = Rights.NewInstance(null, null, RightsType.COPYRIGHT());
            right.setAgent(agent);
            right = deduplicationHelper.getExistingCopyright(null, right);
        }

        //created
        if (created != null){
            DateTimeFormatter f = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");
            try {
                createdDate = f/*.withZone(DateTimeZone.forID("Europe/Athens"))*/.parseDateTime(created);
            } catch (Exception e) {
                logger.warn("Exception (" + e.getMessage() + ") when parsing create date " + created + " for file " + fileName);
            }
        }

        boolean force = !updateOnly || forceUpdate;
        //add to media
        if (artistAgent != null && (force || media.getArtist() == null)){
            media.setArtist(artistAgent);
        }
        if (right != null && (force || media.getRights().isEmpty())){
            media.removeRights(right);
            media.addRights(right);
        }
        if (createdDate != null && (force || media.getMediaCreated() == null)){
            media.setMediaCreated(TimePeriod.NewInstance(createdDate));
        }
        if (locality != null && (force || media.getDescription(Language.ENGLISH()) == null)){
            media.putDescription(Language.ENGLISH(), locality);
        }
    }

    /**
     * @param artist
     * @return
     */
    private Person makePerson(String artist, String fileName) {
        artist = artist.trim();
        String regEx = "((?:[A-Z]\\. ?)+)([A-Z][a-z\\-\u00E4\u00F6\u00FC]+)";
        Matcher matcher = Pattern.compile(regEx).matcher(artist);
        Person person = Person.NewInstance();
        if (matcher.matches()){
            person.setGivenName(matcher.group(1).trim());
            person.setFamilyName(matcher.group(2).trim());
        }else{
            person.setTitleCache(artist, true);
            logger.warn("Person could not be parsed: " + artist + " for file " + fileName);
        }

        person = deduplicationHelper.getExistingAuthor(null, person);
        return person;
    }

    private String removeQuots(String text) {
        if (text.startsWith("'") && text.endsWith("'")){
            return text.substring(1, text.length() -1);
        }else{
            return text;
        }
    }

    private void makeTextData(String fileStr, Media media, Taxon taxon) {
        TaxonDescription imageGallery = taxon.getImageGallery(true);
        TextData textData = null;
        if (!imageGallery.getElements().isEmpty()){
            DescriptionElementBase el = imageGallery.getElements().iterator().next();
            if (el.isInstanceOf(TextData.class)){
                textData = CdmBase.deproxy(el, TextData.class);
            }else{
                logger.warn("Image gallery had non-textdata description element: " +  fileStr);
            }
        }
        if (textData == null){
            textData = TextData.NewInstance();
            textData.setFeature(Feature.IMAGE());
        }
        imageGallery.addElement(textData);
        textData.addMedia(media);
    }

    /**
     * Creates
     * @param uriString
     * @param readDataFromUrl
     * @see #READ_MEDIA_DATA
     * @return
     * @throws MalformedURLException
     */
    protected Media getImageMedia(String uriString, String uriStrThumb, boolean readMediaData) throws MalformedURLException {
        if( uriString == null){
            return null;
        } else {
            uriString = uriString.replace(" ", "%20");  //replace whitespace
            try {
                ImageInfo imageInfo = null;
                URI uri = new URI(uriString);

                try {
                    if (readMediaData){
                        logger.info("Read media data from: " + uri);
                        imageInfo = ImageInfo.NewInstance(uri, 0);
                    }
                } catch (Exception e) {
                    String message = "An error occurred when trying to read image meta data for " + uri.toString() + ": " +  e.getMessage();
                    logger.warn(message);
                }
                ImageFile imageFile = ImageFile.NewInstance(uri, null, imageInfo);

                MediaRepresentation representation = MediaRepresentation.NewInstance();

                if(imageInfo != null){
                    representation.setMimeType(imageInfo.getMimeType());
                    representation.setSuffix(imageInfo.getSuffix());
                }
                representation.addRepresentationPart(imageFile);
                Media media = Media.NewInstance();
                media.addRepresentation(representation);

                if (uriStrThumb != null){
                    ImageInfo imageInfoThumb = null;
                    uriStrThumb = uriStrThumb.replace(" ", "%20");  //replace whitespace
                    URI uriThumb = new URI(uriStrThumb);
                    try {
                        if (readMediaData){
                            logger.info("Read media data from: " + uriThumb);
                            imageInfoThumb = ImageInfo.NewInstance(uriThumb, 0);
                        }
                    } catch (Exception e) {
                        String message = "An error occurred when trying to read image meta data for " + uriThumb.toString() + ": " +  e.getMessage();
                        logger.warn(message);
                    }

                    ImageFile imageFileFhumb = ImageFile.NewInstance(uriThumb, null, imageInfoThumb);
                    MediaRepresentation reprThumb = MediaRepresentation.NewInstance();
                    if(imageInfoThumb != null){
                        reprThumb.setMimeType(imageInfoThumb.getMimeType());
                        reprThumb.setSuffix(imageInfoThumb.getSuffix());
                    }
                    reprThumb.addRepresentationPart(imageFileFhumb);
                    media.addRepresentation(reprThumb);
                }

                return media;
            } catch (URISyntaxException e1) {
                String message = "An URISyntaxException occurred when trying to create uri from multimedia objcet string: " +  uriString;
                logger.warn(message);
                return null;
            }
        }
    }

    /**
     * @param taxon
     * @return
     */
    private Set<String> getAllExistingUrls(Taxon taxon) {
        Set<String> result = new HashSet<>();
        Set<TaxonDescription> descriptions = taxon.getDescriptions();
        for (TaxonDescription td : descriptions){
            if (td.isImageGallery()){
                for (DescriptionElementBase deb : td.getElements()){
                    if (deb.isInstanceOf(TextData.class)){
                        TextData textData = CdmBase.deproxy(deb, TextData.class);
                        for (Media media :textData.getMedia()){
                            for (MediaRepresentation rep : media.getRepresentations()){
                                for (MediaRepresentationPart part : rep.getParts()){
                                    URI uri = part.getUri();
                                    if (uri != null){
                                        String uriStr = uri.toString();
                                        result.add(uriStr);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        return result;
    }

    private Taxon getAcceptedTaxon(CdmApplicationController app, String taxonNameStr) {

        MatchingTaxonConfigurator config = new MatchingTaxonConfigurator();
        taxonNameStr = adaptName(taxonNameStr);
        config.setTaxonNameTitle(taxonNameStr);
        config.setIncludeSynonyms(false);
        List<TaxonBase> list = app.getTaxonService().findTaxaByName(config);
        if (list.isEmpty()){
//            logger.warn("Taxon not found for media: " + taxonNameStr);
            taxonNameStr = taxonNameStr.replaceFirst(" ", " " + UTF8.HYBRID.toString());
            config.setTaxonNameTitle(taxonNameStr);
            list = app.getTaxonService().findTaxaByName(config);
            if (list.isEmpty()){
                return null;
            }else if (list.size() > 1){
                logger.warn("After searching for hybrids more than 1 taxon was foung: " + taxonNameStr);
            }
        }
        if (list.size()>1){
            Iterator<TaxonBase> it = list.iterator();
            while (it.hasNext()){
                Taxon next = (Taxon)it.next();
                if (next.getTaxonNodes().isEmpty() && !next.getTaxaForMisappliedName(true).isEmpty()){
                    it.remove();
                }
            }
            if (list.size()>1){
                logger.warn("More than 1 taxon found for media: " + taxonNameStr + " . Will now try to use only taxon with taxon node.");
                it = list.iterator();
                while (it.hasNext()){
                    Taxon next = (Taxon)it.next();
                    if (next.getTaxonNodes().isEmpty()){
                        it.remove();
                    }
                }
                if (list.size()>1){
                    logger.warn("Still more than 1 taxon found for media: " + taxonNameStr);
                }else if (list.size() < 1){
                    logger.warn("After removing nodeless taxa no taxon was left: " +  taxonNameStr);
                    return null;
                }
            }else if (list.size() < 1){
                logger.warn("After removing misapplications no taxon was left: " +  taxonNameStr);
                return null;
            }
        }
        TaxonBase<?> taxonBase = list.get(0);
        Taxon result;
        if (taxonBase.isInstanceOf(Synonym.class)){
            result = CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
        }else{
            result = CdmBase.deproxy(taxonBase, Taxon.class);
        }
        return result;
    }

    /**
     * @param taxonNameStr
     * @return
     */
    private String adaptName(String taxonNameStr) {
//        if (taxonNameStr.equals("Hypericum cerastoides")){
//            taxonNameStr = "Hypericum cerastioides";
//        }
        return taxonNameStr;
    }

	private void test(){
	    File f = new File(path);
	    String[] list = f.list();
	    List<String> fullFileNames = new ArrayList<>();
	    for (String fileName : list){
	        fullFileNames.add(path + fileName);
	        if (! fileName.matches("([A-Z][a-z]+_[a-z\\-]{3,}(?:_s_[a-z\\-]{3,})?)_[A-F]\\d{1,2}\\.(jpg|JPG)")){
	            System.out.println(fileName);
	        }
	    }
	}

	private void updateMetadata(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();

        deduplicationHelper = (ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>>)ImportDeduplicationHelper.NewInstance(app);

        List<Media> list = app.getMediaService().list(Media.class, null, null, null, null);
        for (Media media : list){
            String fileName = getUrlStringForMedia(media);
            if (fileName.startsWith(urlPath)){
                fileName = fileName.replace(urlPath, "");
                if (fileName.equals("Acinos_exiguus_C1.jpg")){  //for debugging only
//                  System.out.println(fileName);
                    makeMetaData(media, fileName, true);
                    makeTitle(media, fileName, true);
                }
            }else{
                logger.warn("Filename does not start with standard url path: " + fileName);
            }
        }

        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);

	}

	/**
     * @param media
	 * @return
     */
    private String getUrlStringForMedia(Media media) {
        String result = null;
        for (MediaRepresentation rep : media.getRepresentations()){
            for (MediaRepresentationPart part : rep.getParts()){
                URI uri = part.getUri();
                if (uri != null){
                    if (result != null){
                        logger.warn("More than 1 uri exists for media "+ media.getId());
                    }else{
                        result = uri.toString();
                    }
                }
            }
        }
        return result;
    }

    /**
	 * @param args
	 */
	public static void main(String[] args) {
		CyprusImagesActivator me = new CyprusImagesActivator();
		if (update_notCreate){
		    me.updateMetadata(cdmDestination);
		}else{
		    me.doImport(cdmDestination);
		}
//		me.test();
		System.exit(0);
	}

}
