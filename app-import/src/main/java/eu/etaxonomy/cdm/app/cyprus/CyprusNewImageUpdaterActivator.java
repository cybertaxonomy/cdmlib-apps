/**
* Copyright (C) 2022 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.cyprus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;

/**
 * Updates the media URL to a new location.
 *
 * @author a.mueller
 * @date 23.03.2022
 */
public class CyprusNewImageUpdaterActivator {

    private static final Logger logger = LogManager.getLogger();

//    static final ICdmDataSource cdmDestination = CdmDestinations.local_cyprus();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_cyprus();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cyprus();

    static boolean testOnly = false;

//    private static final String newPath = "https://mediahub.bo.berlin/api/File/Original/";
    private static final String oldPath = "https://pictures.bgbm.org/digilib/Scaler?fn=Cyprus/";
    private static final String oldSuffix = "&mo=file";


    private void update(ICdmDataSource cdmDestination){

        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction(true);

        List<String> propertyPathList = new ArrayList<>();
        propertyPathList.add("representation.media");

        List<MediaRepresentationPart> oldList = app.getCommonService().list(MediaRepresentationPart.class, null, null, null, propertyPathList);
        Map<URI,MediaRepresentationPart> oldMap = new HashMap<>();
        Map<String,MediaRepresentationPart> fileNameMap = new HashMap<>();

        for (MediaRepresentationPart mrp : oldList) {
            String uri = mrp.getUri().toString();
//            mrp.getMediaRepresentation().getMedia();//initialize
            String fileName = uri.replace(oldPath, "").replace(oldSuffix, "").replace(".JPG", ".jpg");
            fileNameMap.put(fileName, mrp);
        }
        oldList.forEach(mrp->oldMap.put(mrp.getUri(), mrp));
        app.commitTransaction(tx);


        tx = app.startTransaction();

        handleData(app, fileNameMap);

        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);
    }

    private void handleData(CdmApplicationController app, Map<String, MediaRepresentationPart> fileNameMap) {

        List<String> list = list();
        List<URI> uriList = list.stream().map(str->URI.create(str)).collect(Collectors.toList());

        for (URI uri: uriList) {
            String uriStr = uri.toString();
            String fileName = uriStr.substring(uriStr.lastIndexOf("/") + 1).replace(".JPG", ".jpg");
            MediaRepresentationPart mrp = fileNameMap.get(fileName);
            if (mrp == null) {
                logger.warn("Original URI not found for: " + fileName);
                //TODO
            }else {
                mrp.setUri(uri);

                app.getCommonService().saveOrUpdate(mrp);
            }
        }

    }


    public static void main(String[] args) {
        CyprusNewImageUpdaterActivator me = new CyprusNewImageUpdaterActivator();
        me.update(cdmDestination);
        System.exit(0);
    }

    private List<String> list(){
        String[] result = new String[] {
                "https://mediahub.bo.berlin/api/File/Original/c88f981d-cc3b-407a-8dd8-1903326524fc/Sisymbrium_aegyptiacum_A1.jpg",
                //...

        };

        return Arrays.asList(result);

    }
}
