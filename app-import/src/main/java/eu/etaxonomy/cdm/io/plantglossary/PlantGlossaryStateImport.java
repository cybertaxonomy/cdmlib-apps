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
import java.net.URI;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.csv.in.CsvImportBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
@Component
public class PlantGlossaryStateImport extends CsvImportBase<PlantGlossaryCsvImportConfigurator, PlantGlossaryCsvImportState, File>{
    private static final long serialVersionUID = -5600766240192189822L;
    private static Logger logger = Logger.getLogger(PlantGlossaryStateImport.class);

    final String HEADER_LABEL = "dcterms:identifier";
    final String HEADER_DEFINITION = "definition";
    final String HEADER_CATEGORY = "vann:termGroup";
    final String HEADER_NOTES = "skos:example";
    final String SOURCE_HEADER = "sourceDataset";
    final String HEADER_URI = "term_URI";

    @Override
    protected void handleSingleLine(PlantGlossaryCsvImportState importState) {

        Map<String, String> currentRecord = importState.getCurrentRecord();

        String termLabel = currentRecord.get(HEADER_LABEL);
        //check if already present
        if(importState.isTermPresent(termLabel, getTermService())){
            return;
        }

        State stateTerm = State.NewInstance(currentRecord.get(HEADER_DEFINITION), termLabel, null);
        stateTerm.setIdInVocabulary(termLabel);
        stateTerm.setUri(URI.create(currentRecord.get(HEADER_URI)));
        if(CdmUtils.isNotBlank(currentRecord.get(HEADER_NOTES))){
            stateTerm.addAnnotation(Annotation.NewInstance(currentRecord.get(HEADER_NOTES), AnnotationType.EDITORIAL(), Language.ENGLISH()));
        }

        String vocName = currentRecord.get(HEADER_CATEGORY);
        // TODO how should we handle multiple possible categories?
        // for now we just take the first one
        if(vocName.contains(",")){
            vocName = vocName.split(",")[0];
        }
        TermVocabulary vocabulary = importState.checkVocabularies(vocName, getVocabularyService());
        if(vocabulary==null){
            logger.error("No vocabulary found for term: "+stateTerm+" with vocName: "+vocName);
            return;
        }
        vocabulary.addTerm(stateTerm);

        IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, importState.getCitation().getTitle(), null, importState.getCitation(), null);
        source.setIdInSource(termLabel);
        stateTerm.addSource(source);

        getVocabularyService().saveOrUpdate(vocabulary);
        getTermService().saveOrUpdate(stateTerm);
    }

}