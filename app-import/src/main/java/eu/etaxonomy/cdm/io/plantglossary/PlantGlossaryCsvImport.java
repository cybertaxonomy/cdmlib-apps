/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.csv.in.CsvImportBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.State;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
@Component
public class PlantGlossaryCsvImport extends CsvImportBase<PlantGlossaryCsvImportConfigurator, PlantGlossaryCsvImportState, File>{
    private static final long serialVersionUID = -5600766240192189822L;
    private static Logger logger = Logger.getLogger(PlantGlossaryCsvImport.class);


    @Override
    protected void handleSingleLine(PlantGlossaryCsvImportState importState) {
        final String TERM_HEADER = "term";
        final String CATEGORY_HEADER = "category";
        final String HAS_SYN_HEADER = "hasSyn";
        final String SOURCE_HEADER = "sourceDataset";
        final String TERM_ID_HEADER = "termID";
        final String REMARK_HEADER = "remarks";

        Map<String, String> currentRecord = importState.getCurrentRecord();
        if(!currentRecord.get(REMARK_HEADER).equals("active")){
            String message = String.format(
                    "Line %s has obsolete data and was skipped", importState.getLine());
            logger.info(message);
            return;
        }

        State stateTerm = State.NewInstance(null, currentRecord.get(TERM_HEADER), null);
        stateTerm.setUuid(UUID.fromString(currentRecord.get(TERM_ID_HEADER)));

        String vocName = currentRecord.get(CATEGORY_HEADER);
        TermVocabulary vocabulary = importState.checkVocabularies(vocName);
        if(vocabulary==null){
            vocabulary = TermVocabulary.NewInstance(TermType.State, null, vocName, null, null);
            importState.addVocabulary(vocabulary);
        }
        vocabulary.addTerm(stateTerm);


        stateTerm.addSource(IdentifiableSource.NewInstance(OriginalSourceType.Import, importState.getCitation().getTitle(), null, importState.getCitation(), null));

        getVocabularyService().saveOrUpdate(vocabulary);
        getTermService().saveOrUpdate(stateTerm);
    }

}