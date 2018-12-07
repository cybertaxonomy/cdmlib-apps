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
    private final Reference citation;


    protected PlantGlossaryCsvImportState(PlantGlossaryCsvImportConfigurator config) {
        super(config);
        citation = ReferenceFactory.newGeneric();
        citation.setTitle("fna_gloss_final_20130517");
    }

    @Override
    public void resetSession(){
        super.resetSession();
    }

    void addVocabulary(TermVocabulary vocabulary) {
        vocabularies.add(vocabulary);
    }

    TermVocabulary checkVocabularies(String vocName){
        for (TermVocabulary termVocabulary : vocabularies) {
            if(termVocabulary.getLabel().equals(vocName)){
                return termVocabulary;
            }
        }
        return null;
    }

    Reference getCitation() {
        return citation;
    }

}
