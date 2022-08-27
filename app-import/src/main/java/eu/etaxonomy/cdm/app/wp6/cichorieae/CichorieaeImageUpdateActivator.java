/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.wp6.cichorieae;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.media.MediaInfoFactory;
import eu.etaxonomy.cdm.api.service.media.MediaInfoFileReader;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.media.CdmImageInfo;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.media.RightsType;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * Note: copied from similar class for cyprus but additional "create" method was removed as for
 *       for now it is not necessary here. Once necessary copy and adapt from according cyprus
 *       class.
 *
 *       Currently only size metadata are updated.
 *
 * @author a.mueller
 * @since 12.2021
 */
public class CichorieaeImageUpdateActivator {

    private static final Logger logger = LogManager.getLogger();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cichorieae();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_cyprus();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cichorieae();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cyprus();

	static boolean testOnly = false;

	//if true, data will always be updated, if false, only missing data will be updated
	static boolean forceUpdate = true;
	static boolean sizeOnly = true;

	static UUID languageUuid = Language.uuidEnglish;

    private static final String newUrlPath = "https://pictures.bgbm.org/digilib/Scaler?fn=Cyprus/";

    //currently not used
    private ImportDeduplicationHelper deduplicationHelper;
    private static Language language;


    private void makeTitle(Media media, String fileName, boolean updateOnly) {
        String title = fileName.replace("_s_"," subsp. ")
                .replace("_"," ").replace(".jpg","").replace(".JPG","");
        if ( (!updateOnly) || media.getAllTitles().isEmpty()){
            media.putTitle(language, title);
        }
    }

    private void makeMetaData(Media media, String fileName, ImageFile part, boolean updateOnly, boolean sizeOnly) {

        URI uri = part.getUri();
        Map<String, String> keywords = new HashMap<>();
        String copyright = null;
        String artistStr = null;
        String created = null;
        try{
            MediaInfoFactory mediaFactory = new MediaInfoFactory();
            CdmImageInfo imageInfo;
            try {
                imageInfo = mediaFactory.cdmImageInfo(uri, !sizeOnly);
            } catch (Exception e) {
                URI lowerCaseUri = URI.create(uri.toString().replace(".JPG", ".jpg"));
                try {
                    imageInfo = mediaFactory.cdmImageInfo(lowerCaseUri, !sizeOnly);
                    part.setUri(lowerCaseUri);  //if no error arises we expect this to be the better URI
                } catch (Exception e1) {
                    logger.error("Metadata not readable: " + uri.toString());
                    return;
                }
            }

            //size
            makeSize(part, imageInfo);
            if (sizeOnly){
                return;
            }

            //additional metadata
            for (String metaDataKey : imageInfo.getMetaData().keySet()){
                String value = imageInfo.getMetaData().get(metaDataKey);
//                System.out.println(metaDataKey +  ":    " + value);
                value = removeQuots(value); //not sure if still necessary
                if ("Copyright Notice".equalsIgnoreCase(metaDataKey)){
                    copyright = value;
                }else if ("artist".equals(metaDataKey)){
                    artistStr = value;
                }else if ("DateTimeOriginal".equalsIgnoreCase(metaDataKey)){  //TODO seems not to exist anymore
                    created = value;
                }else{
                    keywords.put(metaDataKey.trim().toLowerCase(), value);
                }
            }
        } catch (Exception e1) {
            logger.warn("       Problem (" + e1.getMessage() + ") when reading metadata from uri: " + part);
            e1.printStackTrace();
            return;
        }

        AgentBase<?> artistAgent = null;
        Rights right = null;
        DateTime createdDate = null;
        String locality = null;

        //artist
        if (keywords.get("photographer") != null){
            String artist = keywords.get("photographer");
            artistAgent = getOrCreatePerson(artist, fileName);
        }
        if (artistStr != null){
            if (keywords.get("photographer") == null){
                artistAgent = getOrCreatePerson(artistStr, fileName);
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
                agent = getOrCreatePerson(copyright, fileName);
            }
            right = Rights.NewInstance(null, null, RightsType.COPYRIGHT());
            right.setAgent(agent);
            right = deduplicationHelper.getExistingCopyright(right);
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

    private void makeSize(ImageFile part, CdmImageInfo imageInfo) {
        //h
        Integer height = part.getHeight();
        if (height == null || height != imageInfo.getHeight()){
            part.setHeight(imageInfo.getHeight());
        }
        //w
        Integer width = part.getWidth();
        if (width == null || width != imageInfo.getWidth()){
            part.setWidth(imageInfo.getWidth());
        }
        //s
        Integer size = part.getSize();
        if(size == null || size != imageInfo.getLength()){
            part.setSize((int)imageInfo.getLength());
        }
    }

    private Person getOrCreatePerson(String artist, String fileName) {
        artist = artist.trim();
        String regEx = "((?:[A-Z](?:\\.|[a-z\\-\u00E4\u00F6\u00FC]+) ?)+)([A-Z][a-z\\-\u00E4\u00F6\u00FC]+)";
        Matcher matcher = Pattern.compile(regEx).matcher(artist);
        Person person = Person.NewInstance();
        if (matcher.matches()){
            person.setGivenName(matcher.group(1).trim());
            person.setFamilyName(matcher.group(2).trim());
        }else{
            person.setTitleCache(artist, true);
            logger.warn("Person could not be parsed: " + artist + " for file " + fileName);
        }

        person = deduplicationHelper.getExistingAuthor(person, false);
        return person;
    }

    private String removeQuots(String text) {
        if (text.startsWith("'") && text.endsWith("'")){
            return text.substring(1, text.length() -1);
        }else{
            return text;
        }
    }

    /**
     * Creates
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
                MediaRepresentation representation = makeMediaRepresentation(uriString, readMediaData);
                Media media = Media.NewInstance();
                media.addRepresentation(representation);

                if (uriStrThumb != null){
                    CdmImageInfo imageInfoThumb = null;
                    uriStrThumb = uriStrThumb.replace(" ", "%20");  //replace whitespace
                    URI uriThumb = new URI(uriStrThumb);
                    try {
                        if (readMediaData){
                            logger.info("Read media data from: " + uriThumb);
//                          //imageInfoThumb = CdmImageInfo.NewInstance(uriThumb, 0);
                            imageInfoThumb = MediaInfoFileReader.legacyFactoryMethod(uriThumb)
                                    .readBaseInfo()
                                    .getCdmImageInfo();
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

    private MediaRepresentation makeMediaRepresentation(String uriString, boolean readMediaData) {

        uriString = uriString.replace(" ", "%20");  //replace whitespace
        CdmImageInfo imageInfo = null;
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e1) {
            logger.error("Malformed URI. Could not create media representation: " + uriString);
            return null;
        }
        try {
            if (readMediaData){
                logger.info("Read media data from: " + uri);
                //imageInfo = CdmImageInfo.NewInstance(uri, 0);
                imageInfo = MediaInfoFileReader.legacyFactoryMethod(uri)
                        .readBaseInfo()
                        .getCdmImageInfo();
            }
        } catch (Exception e) {
            try {
                //try again
                //imageInfo = CdmImageInfo.NewInstance(uri, 0);
                imageInfo = MediaInfoFileReader.legacyFactoryMethod(uri)
                        .readBaseInfo()
                        .getCdmImageInfo();
            } catch (Exception e1) {
                String message = "An error occurred when trying to read image meta data for " + uri.toString() + ": " +  e1.getMessage();
                e1.printStackTrace();
                logger.warn(message);
            }
        }
        ImageFile imageFile = ImageFile.NewInstance(uri, null, imageInfo);

        MediaRepresentation representation = MediaRepresentation.NewInstance();

        if(imageInfo != null){
            representation.setMimeType(imageInfo.getMimeType());
            representation.setSuffix(imageInfo.getSuffix());
        }
        representation.addRepresentationPart(imageFile);
        return representation;
    }

	private void updateMetadata(ICdmDataSource cdmDestination){


	    CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);

	    language = DefinedTermBase.getTermByClassAndUUID(Language.class, languageUuid);
	    TransactionStatus tx = app.startTransaction();

        deduplicationHelper = ImportDeduplicationHelper.NewInstance(app, null);

        List<Media> list = app.getMediaService().list(Media.class, null, null, null, null);
        for (Media media : list){
            handleSingleMediaUpdate(media);
        }

        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);
	}

    private void handleSingleMediaUpdate(Media media){
        ImageFile part = getUrlStringForMedia(media);
        if (part == null || part.getUri() == null){
            logger.warn("No uri found for media (id = " + media.getId() + ")");
            return;
        }
        String url = part.getUri().toString();
        if (url.startsWith(newUrlPath)){
            String fileName = url.replace(newUrlPath, "").replace("&mo=file", "");
            makeMetaData(media, fileName, part, true, sizeOnly);
            makeTitle(media, fileName, true);
            System.out.println(fileName);
        }else{
            logger.warn("URL does not start with standard url path: " + url);
        }
    }

    private ImageFile getUrlStringForMedia(Media media) {
        ImageFile result = null;
        for (MediaRepresentation rep : media.getRepresentations()){
            for (MediaRepresentationPart part : rep.getParts()){
                URI uri = part.getUri();
                if (uri != null){
                    if (result != null){
                        //TODO this still needs to be adapted to the 3 representations of media
                        logger.warn("More than 1 uri exists for media "+ media.getId());
                    }else if (!part.isInstanceOf(ImageFile.class)){
                        logger.warn("MediaRepresentationPart is not an ImageFile: " + uri);
                    }else{
                        result = CdmBase.deproxy(part, ImageFile.class);
                    }
                }
            }
        }
        return result;
    }

	public static void main(String[] args) {
		CichorieaeImageUpdateActivator me = new CichorieaeImageUpdateActivator();
		me.updateMetadata(cdmDestination);
		System.exit(0);
	}
}
