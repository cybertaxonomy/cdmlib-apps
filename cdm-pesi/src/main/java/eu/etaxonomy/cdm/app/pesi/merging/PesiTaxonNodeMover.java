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
    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2025_final();
//    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final_test();

    private void invoke(ICdmDataSource source){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);
        System.out.println("Start");

        String uuidStrIfFungi = "09d7e222-4dff-4623-9628-064b88471b04";
        String uuidStrIfPlantae = "2eecff54-45fb-4578-ae2b-303eb63beae2";
        String uuidStrIfProtozoa = "c66ef73f-fcee-4b1b-b6bf-2a7ec1e30aa7";

        String uuidStrEMTracheophyta = "47125361-6ac4-4173-b6f5-6900f496f76a";
        String uuidStrFauEuAnimalia = "feaa3025-a4a9-499a-b62f-15b3b96e5c55";
        String uuidStrBiotaErms = "4f854b11-b69b-4fea-bd86-2014e896546b";
        String uuidStrErmsStreptophyta = "a31f7152-ab64-436f-ba8f-476c1c957f63";
        String uuidStrMergeRoot = "3e9e97ac-b317-42f2-ab8c-f491a539792f";

        String uuidBryophytes = "dca25102-3967-4cb2-bc65-6aa3a3cf8c4b";
        String uuidStreptophyta = "b9a26be4-3118-4f11-9d3f-348814185230";

        UUID taxonNodeToMoveUuid = UUID.fromString(uuidBryophytes);
        UUID newParentTaxonNodeUuid = UUID.fromString(uuidStreptophyta);
        int movingType = 0;
        SecReferenceHandlingEnum secHandling = SecReferenceHandlingEnum.KeepOrWarn;
        UpdateResult result = app.getTaxonNodeService().moveTaxonNode(taxonNodeToMoveUuid, newParentTaxonNodeUuid, movingType, secHandling, null);
        System.out.println(result);
    }

    public static void main(String[] args) {
        PesiTaxonNodeMover mover = new PesiTaxonNodeMover();
        mover.invoke(pesiSource);
        System.out.println("End");
        System.exit(0);
    }
}