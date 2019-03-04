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
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
@Component
public class PlantGlossaryCategoryImport extends CsvImportBase<PlantGlossaryCsvImportConfigurator, PlantGlossaryCsvImportState, File>{
    private static final long serialVersionUID = -5600766240192189822L;
    private static Logger logger = Logger.getLogger(PlantGlossaryCategoryImport.class);

    @Override
    protected void handleSingleLine(PlantGlossaryCsvImportState importState) {
        final String HEADER_LABEL = "rdfs:label";
        final String HEADER_DESCRIPTION = "skos:definition";
        final String HEADER_URI = "category_URI";
        final String HEADER_NOTES = "skos:notes";

        Map<String, String> currentRecord = importState.getCurrentRecord();

        String vocName = currentRecord.get(HEADER_LABEL);
        if(CdmUtils.isBlank(vocName)){
            // this line does not contain any vocabulary information
            return;
        }

        TermVocabulary existingVocabulary = importState.checkVocabularies(vocName, getVocabularyService());
        if(existingVocabulary!=null){
            return;
        }

        TermVocabulary<?> stateVoc = TermVocabulary.NewInstance(
                TermType.State,
                currentRecord.get(HEADER_DESCRIPTION),
                vocName,
                null,
                importState.getCitation().getUri());
        stateVoc.setUri(URI.create(currentRecord.get(HEADER_URI)));
        stateVoc.addAnnotation(Annotation.NewInstance(currentRecord.get(HEADER_NOTES), AnnotationType.EDITORIAL(), Language.ENGLISH()));

        importState.addVocabulary(stateVoc);

        IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, importState.getCitation().getTitle(), null, importState.getCitation(), null);
        source.setIdInSource(vocName);
        stateVoc.addSource(source);

        getVocabularyService().saveOrUpdate(stateVoc);
    }

}