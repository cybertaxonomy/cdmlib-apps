/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.greece;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.media.CdmImageInfo;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;

/**
 * @author a.mueller
 * @since 05.2017
 */
public class GreeceLargeImagesAdderActivator {
	@SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceLargeImagesAdderActivator.class);


//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

	static boolean testOnly = false;


    private void addLargeImage(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();

        List<Media> list = app.getMediaService().list(Media.class, null, null, null, null);
        for (Media media : list){
            if (! isFirstImportMedia(media)){
                continue;
            }
            Set<MediaRepresentation> reps = media.getRepresentations();
            if (reps.size() != 2){
                System.out.println("Media has not exactly 2 representations: " +  media.getId() + "; " +  media.getTitleCache());
                continue;
            }else{
                MediaRepresentation first = reps.iterator().next();
                if (first.getParts().size() != 1){
                    System.out.println("Media has representation with not exactly 1 parts: " +  media.getId() + "; " +  media.getTitleCache());
                    continue;
                }
                MediaRepresentationPart part = first.getParts().iterator().next();
                String uri = part.getUri().toString();
                if (uri.startsWith("https://media.e-taxonomy.eu/flora-greece/medium/Plate")){
                    uri = uri.replace("flora-greece/medium/Plate", "flora-greece/large/Plate");
                }else if(uri.startsWith("https://media.e-taxonomy.eu/flora-greece/thumbs/Plate")) {
                    uri = uri.replace("flora-greece/thumbs/Plate", "flora-greece/large/Plate");
                }else{
                    System.out.println("URI has unexpected format: " +  uri);
                    continue;
                }
                handleUri(media, uri);
            }
        }
        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);
    }

	private void updateImageSizes(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();

        List<Media> list = app.getMediaService().list(Media.class, null, null, null, null);
        for (Media media : list){
            if (! isFirstImportMedia(media)){
                continue;
            }
            Set<MediaRepresentation> reps = media.getRepresentations();
            for (MediaRepresentation rep : reps){
                for (MediaRepresentationPart part : rep.getParts()){
                    if (part.isInstanceOf(ImageFile.class)){
                        handlePart(CdmBase.deproxy(part, ImageFile.class));
                    }else{
                        System.out.println("Representation part is not of type ImageFile: "+  part.getId());
                    }
                }
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
    private boolean isFirstImportMedia(Media media) {
        Boolean result = null;
        Set<String> urls = getUrlStringForMedia(media);
        for (String url : urls){
            if (url.startsWith("http://150.140.202.8/files/Goula/") || url.startsWith("http://n4412.gr/images/Globula")){
                if (result == Boolean.TRUE){
                    System.out.println("Ambigous: "  + media.getId());
                    return false;
                }
                result = false;
            }else if (url.startsWith("https://media.e-taxonomy.eu/flora-greece")){
                if (result == Boolean.FALSE){
                    System.out.println("Ambigous: "  + media.getId());
                    return false;
                }
                result = true;
            }
        }
        if (result == null){
            System.out.println("No data: "  + media.getId());
            return false;
        }
        return result;
    }

    /**
     * @param media
     * @return
     */
    private Set<String> getUrlStringForMedia(Media media) {
        Set<String> result = new HashSet<>();
        for (MediaRepresentation rep : media.getRepresentations()){
            for (MediaRepresentationPart part : rep.getParts()){
                URI uri = part.getUri();
                if (uri != null){
                    result.add(uri.toString());
                }else{
                    System.out.println("URI is null:" + media.getId());
                }
            }
        }
        return result;
    }

    private void handlePart(ImageFile part) {
        CdmImageInfo imageInfo = null;
        URI uri = part.getUri();
        try {
            imageInfo = CdmImageInfo.NewInstance(uri, 0);
        } catch (Exception e) {
            String message = "An error occurred when trying to read image meta data for %s.";
            message = String.format(message, uri.toString());
            System.out.println(message);
            return;
        }
        part.setHeight(imageInfo.getHeight());
        part.setWidth(imageInfo.getWidth());

        MediaRepresentation representation = part.getMediaRepresentation();
        representation.setMimeType(imageInfo.getMimeType());
        representation.setSuffix(imageInfo.getSuffix());

    }

    /**
     * @param reps
     * @param uri
     */
    private void handleUri(Media media, String uriStr) {
        URI uri = URI.create(uriStr);
        CdmImageInfo imageInfo = null;
        try {
            imageInfo = CdmImageInfo.NewInstance(uri, 0);
        } catch (Exception e) {
            String message = "An error occurred when trying to read image meta data for %s.";
            message = String.format(message, uri.toString());
            System.out.println(message);
        }
        ImageFile imageFile = ImageFile.NewInstance(uri, null, imageInfo);

        MediaRepresentation representation = MediaRepresentation.NewInstance();

        if(imageInfo != null){
            representation.setMimeType(imageInfo.getMimeType());
            representation.setSuffix(imageInfo.getSuffix());
        }
        representation.addRepresentationPart(imageFile);
        media.addRepresentation(representation);

    }




    /**
	 * @param args
	 */
	public static void main(String[] args) {
		GreeceLargeImagesAdderActivator me = new GreeceLargeImagesAdderActivator();
//		me.addLargeImage(cdmDestination);
		me.updateImageSizes(cdmDestination);
//		me.test();
		System.exit(0);
	}

}
