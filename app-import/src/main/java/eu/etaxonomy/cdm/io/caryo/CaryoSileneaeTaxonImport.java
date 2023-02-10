/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeTaxonImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 5594951908819469636L;
    private static final Logger logger = LogManager.getLogger();

    private static final String ACCEPTED_TAXON_ID = "AcceptedTaxon_ID";
    private static final String NOMEN_LINK = "nomen_link";
    private static final String PARENT = "parent";

    @Override
    protected String getWorksheetName(CaryoSileneaeImportConfigurator config) {
        return "Taxa";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        try {
            int line = state.getCurrentLine();
//            if ((line % 500) == 0){
//                newTransaction(state);
//                System.out.println(line);
//            }

            Map<String, String> record = state.getOriginalRecord();

            Integer accTaxonId = Integer.valueOf(getValue(record, ACCEPTED_TAXON_ID));
            Integer nameLinkID = Integer.valueOf(getValue(record, NOMEN_LINK));

            String row = String.valueOf(line) + "("+accTaxonId+"): ";

            TaxonName name = getName(nameLinkID);
            if (name == null) {
                logger.warn(row + "Name does not exist");
                return;
            }

            orphanedNameMap.remove(nameLinkID);
            Taxon taxon = Taxon.NewInstance(name, getSecRef(state));

            putToTaxonMap(accTaxonId, taxon);
            getTaxonService().saveOrUpdate(taxon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        try {
            int line = state.getCurrentLine();
//            if ((line % 500) == 0){
//                newTransaction(state);
//                System.out.println(line);
//            }

            Map<String, String> record = state.getOriginalRecord();

            Integer accTaxonId = Integer.valueOf(getValue(record, ACCEPTED_TAXON_ID));
            Integer parentId = getInt(getValue(record, PARENT));

            String row = String.valueOf(line) + "("+accTaxonId+"): ";

            Taxon taxon = getTaxon(accTaxonId);
            if (taxon == null) {
                logger.warn(row + "Taxon does not exist");
                return;
            }

            Taxon parent;
            if (parentId == null) {
                TaxonNode rootNode = getRootNode(state);
                parent = rootNode.getTaxon();
            }else {
                parent = getTaxon(parentId);

            }
            if (parent == null) {
                logger.warn(row + "Taxon does not exist");
                return;
            }
            Classification classification = getClassification(state);

            TaxonNode node = classification.addParentChild(parent, taxon, null, null);

            getTaxonNodeService().saveOrUpdate(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TaxonNode rootNode;
    private TaxonNode getRootNode(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        if (rootNode == null) {
            rootNode = getTaxonNodeService().find(state.getConfig().getAcceptedNodeUuid());
        }
        return rootNode;
    }

    private Classification classification;
    private Classification getClassification(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        if (classification == null) {
            classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        }
        return classification;
    }
}
