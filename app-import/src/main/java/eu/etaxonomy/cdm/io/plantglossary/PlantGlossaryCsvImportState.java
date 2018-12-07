/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import java.util.HashSet;
import java.util.Set;

import eu.etaxonomy.cdm.io.csv.in.CsvImportState;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
public class PlantGlossaryCsvImportState extends CsvImportState<PlantGlossaryCsvImportConfigurator> {

    private Set<TermVocabulary> vocabularies = new HashSet<>();
    private Reference citation;


    protected PlantGlossaryCsvImportState(PlantGlossaryCsvImportConfigurator config) {
        super(config);
    }

    @Override
    public void resetSession(){
        vocabularies.clear();
        citation = null;
        super.resetSession();
    }

    TermVocabulary initVocabulary(String vocName) {
        TermVocabulary vocabulary = checkVocabularies(vocName);
        if(vocabulary==null){
            vocabulary = TermVocabulary.NewInstance(TermType.State, null, vocName, null, null);
            vocabularies.add(vocabulary);
        }
        return vocabulary;
    }

    private TermVocabulary checkVocabularies(String vocName){
        for (TermVocabulary termVocabulary : vocabularies) {
            if(termVocabulary.getLabel().equals(vocName)){
                return termVocabulary;
            }
        }
        return null;
    }

    IdentifiableSource initSource(String sourceName) {
        if(citation==null){
            citation = ReferenceFactory.newGeneric();
            citation.setTitle("fna_gloss_final_20130517");
        }
        return IdentifiableSource.NewInstance(OriginalSourceType.Import, sourceName, null, citation, null);
    }

}
