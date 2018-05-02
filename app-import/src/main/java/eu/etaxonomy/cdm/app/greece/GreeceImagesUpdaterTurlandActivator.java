/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.greece;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.Rights;

/**
 * @author a.mueller
 * @since 05.2017
 */
public class GreeceImagesUpdaterTurlandActivator {
	@SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceImagesUpdaterTurlandActivator.class);

//  static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_greece_checklist_production();

	static boolean testOnly = false;

	private void updateRights(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();

        UUID uuidLicence = UUID.fromString("b1d1b1d2-dc79-4e13-b304-0bce50a648cc");
        List<Media> list = app.getMediaService().list(Media.class, null, null, null, null);
        Rights licenceRight = app.getRightsService().find(uuidLicence);
        AgentBase<?> turland = app.getAgentService().find(3972);
        int i = 0;
        for (Media media : list){
            Set<Rights> rights = media.getRights();
            if (rights.size() == 1){
                Rights right = rights.iterator().next();
                if (right.getAgent().equals(turland)){
                    rights.add(licenceRight);
                    i++;
                }
            }
        }

        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);
        System.out.println("Updated " +  i +  " licences");
	}


    /**
	 * @param args
	 */
	public static void main(String[] args) {
		GreeceImagesUpdaterTurlandActivator me = new GreeceImagesUpdaterTurlandActivator();
		me.updateRights(cdmDestination);
//		me.test();
		System.exit(0);
	}

}
