/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.greece;

import java.io.IOException;
import eu.etaxonomy.cdm.common.URI;
import java.util.List;

import org.apache.http.HttpException;
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
public class GreeceImagesUpdaterActivator {
	private static final Logger logger = Logger.getLogger(GreeceImagesUpdaterActivator.class);

//  static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

	static boolean testOnly = false;
	int partition = 3;
	int partitionsize = 500;

	private void updateMetadata(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();
        int offset = partition*partitionsize;
        List<Media> list = app.getMediaService().list(Media.class, partitionsize, offset, null, null);
        for (Media media : list){
            for (MediaRepresentation rep : media.getRepresentations()){
                for (MediaRepresentationPart part : rep.getParts()){
                    if (part.isInstanceOf(MediaRepresentationPart.class)){
                        ImageFile image = CdmBase.deproxy(part, ImageFile.class);
                        URI uri = part.getUri();
                        if (uri != null){
                            if (image.getSize()!= null){
                                continue;
                            }
                            CdmImageInfo imageInfo;
                            try {
                                imageInfo = CdmImageInfo.NewInstance(uri, 0);
                                image.setWidth(imageInfo.getWidth());
                                image.setHeight(imageInfo.getHeight());
                                image.setSize((int)imageInfo.getLength());
                            } catch (IOException | HttpException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                logger.warn("IO or Http exception for uri " + uri.toString());
                            }
                        }
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
	 * @param args
	 */
	public static void main(String[] args) {
		GreeceImagesUpdaterActivator me = new GreeceImagesUpdaterActivator();
		me.updateMetadata(cdmDestination);
//		me.test();
		System.exit(0);
	}

}
