/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.UpdateResult;
import eu.etaxonomy.cdm.api.service.config.SubtreeCloneConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;

/**
 * Clones the taxon node for Centaurea in the Flora of Greece.
 *
 * @author a.mueller
 * @since 30.11.2020
 */
public class CentaureaCloneActivator {


    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BupleurumExportActivator.class);

    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_greece();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_bupleurum();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

    static final String newClassificationTitle = "Centaurea";
//    static final UUID sourceRefUuid = UUID.fromString("f88e33e5-1f6a-463e-b6fd-220d5e93d810");
    static final UUID centaureaTaxonNodeUuid = UUID.fromString("ea9ae65a-cd1e-4e8a-b300-9f1904de074b");


    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    private void doClone(ICdmDataSource cdmDestination) {
        CdmApplicationController app = CdmApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        SubtreeCloneConfigurator cloneConfig = SubtreeCloneConfigurator.NewBaseInstance(
                centaureaTaxonNodeUuid, newClassificationTitle);
        cloneConfig.setReuseNames(false);

        UpdateResult result = app.getTaxonNodeService().cloneSubtree(cloneConfig);
        result.getCdmEntity();
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        CentaureaCloneActivator myImport = new CentaureaCloneActivator();
        myImport.doClone(cdmDB);
        System.exit(0);
    }

}
