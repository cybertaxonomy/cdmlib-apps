/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * Import for the Flora Hellenica taxon comments.
 *
 * @author a.mueller
 * @since 14.12.2016
 */
@Component
public class FloraHellenicaCommentsImport<CONFIG extends FloraHellenicaImportConfigurator>
        extends FloraHellenicaImportBase<CONFIG>{

    private static final long serialVersionUID = -3565782012921316901L;
    private static final Logger logger = LogManager.getLogger();

    private static final String TAXON = "Taxon";
    private static final String UNIQUE_ID_ACCEPTED = "Unique ID of taxon name (Includes valid and excluded taxa IDs)";
    private static final String COMMENT = "Comment";


   private  static List<String> expectedKeys= Arrays.asList(new String[]{
            UNIQUE_ID_ACCEPTED,
            TAXON,
            COMMENT
    });

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "comments";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        String noStr = getValue(record, "Unique ID");
        makeComment(state, line, record, noStr);
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private void makeComment(SimpleExcelTaxonImportState<CONFIG> state, String line,
            Map<String, String> record,
            String noStr) {

        Taxon acceptedTaxon = getAcceptedTaxon(record, state, UNIQUE_ID_ACCEPTED);
        if (acceptedTaxon == null){
            logger.warn(line + "Accepted not found: " + record.get(UNIQUE_ID_ACCEPTED));
            return;
        }

        String commentStr = getValue(record, COMMENT);

        TaxonDescription desc = getTaxonDescription(acceptedTaxon);
        TextData comment = TextData.NewInstance(Feature.NOTES(), commentStr, Language.ENGLISH(), null);
        desc.addElement(comment);
        comment.addImportSource(noStr, getWorksheetName(state.getConfig()), getSourceCitation(state), null);
        getTaxonService().saveOrUpdate(acceptedTaxon);

        TaxonNode taxonNode = acceptedTaxon.getTaxonNodes().iterator().next();
        if(taxonNode.isExcluded()){
            taxonNode.putStatusNote(Language.ENGLISH(), commentStr);
        }
        return ;
    }

}
