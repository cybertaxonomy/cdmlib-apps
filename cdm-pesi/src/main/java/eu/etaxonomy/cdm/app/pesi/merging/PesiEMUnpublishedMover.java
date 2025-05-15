/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.merging;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.UpdateResult;
import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.metadata.SecReferenceHandlingEnum;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * Moves published taxa in the unpublished branch to the published branch.
 *
 * Input: a list of unpublished taxon node uuids, and parent names.
 * Re
 *
 * @author a.mueller
 * @since 15.05.2025
 */
public class PesiEMUnpublishedMover extends PesiMergeBase{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();
    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2025_final();

    static final UUID targetClassificationUuid = UUID.fromString("6fa988a9-10b7-48b0-a370-2586fbc066eb");

    private void invoke(ICdmDataSource source){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);

        List<List<String>> fileData = getFileData();
        while(!fileData.isEmpty()){
            handleSingleLine(app, fileData);
        }System.out.println("End");
    }

    private void handleSingleLine(CdmApplicationController app, List<List<String>> fileData) {
        MoveInfo moveInfo = readLineFromFile(fileData);
        UUID taxonNodeUuid = moveInfo.taxonNodeUuid;
        UUID newParentTaxonNodeUuid = getNewParent(app, moveInfo);
        if (newParentTaxonNodeUuid == null) {
            return;
        }
        int movingType = 0;
        SecReferenceHandlingEnum secHandling = SecReferenceHandlingEnum.KeepOrWarn;
        UpdateResult result = app.getTaxonNodeService().moveTaxonNode(taxonNodeUuid, newParentTaxonNodeUuid, movingType, secHandling, null);
        System.out.println(result);
    }

    private UUID getNewParent(CdmApplicationController app, MoveInfo moveInfo) {
        TransactionStatus tx = app.startTransaction();
        String parentName = moveInfo.parentName;
        MatchingTaxonConfigurator config = MatchingTaxonConfigurator.NewInstance();
        config.setTaxonNameTitle(parentName);
        config.setClassificationUuid(targetClassificationUuid);
        config.setOnlyMatchingClassificationUuid(true);
        List<TaxonBase> r = app.getTaxonService().findTaxaByName(config);
        List<TaxonNode>nodes = r.stream()
            .filter(tb->tb.isInstanceOf(Taxon.class))
            .map(tb->tb.acceptedTaxon())
            .flatMap(t->t.getTaxonNodes().stream())
            .filter(tn->tn.getClassification().getUuid().equals(targetClassificationUuid))
            .collect(Collectors.toList());

        app.commitTransaction(tx);
        if (nodes.size() == 1) {
            return nodes.get(0).getUuid();
        }else {
            System.out.println("Parent could not be defined: " + parentName + ". n = " +  nodes.size());
            return null;
        }
    }

    private class MoveInfo{
        UUID taxonNodeUuid;
        String parentName;
    }

    private MoveInfo readLineFromFile(List<List<String>> fileData) {
        List<String> line = fileData.get(0);
        MoveInfo moveInfo = new MoveInfo();
        try {
            moveInfo.taxonNodeUuid = UUID.fromString(line.get(0));
            moveInfo.parentName = line.get(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileData.remove(0);
        return moveInfo;
    }

    public static void main(String[] args) {
        PesiEMUnpublishedMover mover = new PesiEMUnpublishedMover();
        mover.invoke(pesiSource);
        System.out.println("End");
        System.exit(0);
    }
}