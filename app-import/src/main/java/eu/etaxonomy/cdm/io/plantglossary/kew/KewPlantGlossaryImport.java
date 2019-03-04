/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary.kew;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.csv.in.CsvImportBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 *
 * @author pplitzner
 * @since Jan 25, 2019
 *
 */
@Component
public class KewPlantGlossaryImport extends CsvImportBase<KewPlantGlossaryCsvImportConfigurator, KewPlantGlossaryCsvImportState, File>{
    private static final long serialVersionUID = -5600766240192189822L;
    private static Logger logger = Logger.getLogger(KewPlantGlossaryImport.class);

    final String HEADER_LABEL = "term";
    final String HEADER_DEFINITION = "definition";
    final String HEADER_TYPE = "type";

    @Override
    protected void handleSingleLine(KewPlantGlossaryCsvImportState importState) {

        Map<String, String> currentRecord = importState.getCurrentRecord();

        if(CdmUtils.isBlank(currentRecord.get(HEADER_TYPE)) || !currentRecord.get(HEADER_TYPE).equals("1")){
            // only structures (type=1) are imported
            return;
        }

        String termLabel = currentRecord.get(HEADER_LABEL);
        //check if already present
        if(importState.isTermPresent(termLabel, getTermService())){
            return;
        }

        Feature structure = Feature.NewInstance(currentRecord.get(HEADER_DEFINITION), termLabel, null);
        structure.setIdInVocabulary(termLabel);

        TermVocabulary vocabulary = importState.getStructureVoc();
        vocabulary.addTerm(structure);

        IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, importState.getCitation().getTitle(), null, importState.getCitation(), null);
        source.setIdInSource(termLabel);
        structure.addSource(source);

        getVocabularyService().saveOrUpdate(vocabulary);
        getTermService().saveOrUpdate(structure);
    }

}