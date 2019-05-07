/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.prometheus;

import java.util.ArrayList;
import java.util.List;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.io.csv.in.CsvImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
*
* @author pplitzner
* @date 18.02.2019
*
*/
public class PrometheusPropertiesCsvImportState extends CsvImportState<PrometheusPropertiesCsvImportConfigurator> {

    private List<State> existingTerms = new ArrayList<>();
    private final TermVocabulary<Feature> propertiesVoc;
    private final Reference citation;


    protected PrometheusPropertiesCsvImportState(PrometheusPropertiesCsvImportConfigurator config) {
        super(config);
        propertiesVoc = TermVocabulary.NewInstance(TermType.Feature);
        propertiesVoc.setLabel("Properties");

        citation = ReferenceFactory.newGeneric();
        citation.setTitle("The Prometheus Description Model: an examination of the taxonomic description-building process and its representation.");
        Person author = Person.NewInstance(null, "Pullan", null, "M.");
        citation.setAuthorship(author);
        VerbatimTimePeriod datePublished = VerbatimTimePeriod.NewVerbatimInstance();
        datePublished.setStartYear(2005);
        citation.setDatePublished(datePublished);
        citation.setVolume("54");
        citation.setPages("751-756");
    }

    public boolean isTermPresent(String termName, ITermService termService) {
        if(existingTerms.isEmpty()){
            existingTerms = termService.list(State.class, null, null, null, null);
        }
        return existingTerms.stream().map(term->term.getLabel()).anyMatch(label->label.equals(termName));
    }

    public TermVocabulary<Feature> getPropertiesVoc() {
        return propertiesVoc;
    }

    Reference getCitation() {
        return citation;
    }

}
