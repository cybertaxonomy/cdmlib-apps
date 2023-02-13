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

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeTaxonSynonymCleanupImport extends CaryoSileneaeImportBase {

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

            int line = state.getCurrentLine();

            Map<String, String> record = state.getOriginalRecord();

            Integer accTaxonId = Integer.valueOf(getValue(record, ACCEPTED_TAXON_ID));
            Integer nameLinkID = Integer.valueOf(getValue(record, NOMEN_LINK));
            String row = String.valueOf(line) + "("+accTaxonId+"): ";

            accIdMap.put(accTaxonId, nameLinkID);
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        //nothing todo
    }
}