/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.merging;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.UpdateResult;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.metadata.SecReferenceHandlingEnum;

/**
 * @author a.mueller
 * @since 15.01.2020
 */
public class PesiTaxonNodeMover {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();
    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final();
//    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final_test();

    private void invoke(ICdmDataSource source){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);

        String uuidStrEMTracheophyta = "47125361-6ac4-4173-b6f5-6900f496f76a";
        String uuidStrFauEuAnimalia = "feaa3025-a4a9-499a-b62f-15b3b96e5c55";
        String uuidStrBiotaErms = "76407f1f-cdae-4a64-830f-2849f2a4f018";
        String uuidStrIfFungi = "4fd1864c-d358-4c09-b6e2-8849b663f8d0";
        String uuidStrErmsStreptophyta = "a31f7152-ab64-436f-ba8f-476c1c957f63";
        UUID taxonNodeUuid = UUID.fromString(uuidStrIfFungi);
        String uuidStrMergeRoot = "3e9e97ac-b317-42f2-ab8c-f491a539792f";
        UUID newParentTaxonNodeUuid = UUID.fromString(uuidStrBiotaErms);
        int movingType = 0;
        SecReferenceHandlingEnum secHandling = SecReferenceHandlingEnum.KeepOrWarn;
        UpdateResult result = app.getTaxonNodeService().moveTaxonNode(taxonNodeUuid, newParentTaxonNodeUuid, movingType, secHandling, null);
    }

    public static void main(String[] args) {
        PesiTaxonNodeMover mover = new PesiTaxonNodeMover();
        mover.invoke(pesiSource);
        System.out.println("End");
        System.exit(0);
    }
}
