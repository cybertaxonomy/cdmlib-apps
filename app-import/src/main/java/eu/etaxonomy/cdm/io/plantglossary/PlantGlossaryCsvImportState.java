/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import eu.etaxonomy.cdm.io.csv.in.CsvImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
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
        Person authorship = Person.NewInstance(null, "Cui", null, "Hong");
        citation.setAuthorship(authorship);
        VerbatimTimePeriod datePublished = VerbatimTimePeriod.NewVerbatimInstance();
        datePublished.setStartYear(2014);
        datePublished.setStartMonth(6);
        datePublished.setStartDay(13);
        citation.setDatePublished(datePublished);
        URI uri;
        try {
            uri = new URI("https://github.com/biosemantics/glossaries/blob/925f2c1691ed00bf2b9a9cd7f83609cffae47145/Plant/0.11/Plant_glossary_term_category.csv");
            citation.setUri(uri);
        } catch (URISyntaxException e) {
        }
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
