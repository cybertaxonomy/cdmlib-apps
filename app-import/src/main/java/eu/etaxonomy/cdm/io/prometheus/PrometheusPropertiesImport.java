/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.prometheus;

import java.io.File;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.csv.in.CsvImportBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
* @author pplitzner
* @date 18.02.2019
*/
@Component
public class PrometheusPropertiesImport extends CsvImportBase<PrometheusPropertiesCsvImportConfigurator, PrometheusPropertiesCsvImportState, File>{

    private static final long serialVersionUID = -5600766240192189822L;
    private static final Logger logger = LogManager.getLogger();

    final String HEADER_LABEL = "NEW_QUALITATIVE_PROPERTIES - NEW_QUALITATIVE_PROPERTY - term";

    @Override
    protected void handleSingleLine(PrometheusPropertiesCsvImportState importState) {

        Map<String, String> currentRecord = importState.getCurrentRecord();

        String termLabel = currentRecord.get(HEADER_LABEL);
        //check if already present
        if(CdmUtils.isBlank(termLabel) || importState.isTermPresent(termLabel, getTermService())){
            return;
        }

        Feature structure = Feature.NewInstance(null, termLabel, null);
        structure.setIdInVocabulary(termLabel);

        TermVocabulary vocabulary = importState.getPropertiesVoc();
        vocabulary.addTerm(structure);

        IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, importState.getCitation().getTitle(), null, importState.getCitation(), null);
        source.setIdInSource(termLabel);
        structure.addSource(source);

        getVocabularyService().saveOrUpdate(vocabulary);
        getTermService().saveOrUpdate(structure);
    }

}