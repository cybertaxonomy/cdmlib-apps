/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import eu.etaxonomy.cdm.common.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.io.csv.in.CsvImportState;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
public class PlantGlossaryCsvImportState extends CsvImportState<PlantGlossaryCsvImportConfigurator> {

    private TermVocabulary<DefinedTerm> propertyVoc;
    private List<TermVocabulary> existingVocabularies = new ArrayList<>();
    private List<State> existingTerms = new ArrayList<>();
    private Set<TermVocabulary> vocabularies = new HashSet<>();
    private final Reference citation;


    protected PlantGlossaryCsvImportState(PlantGlossaryCsvImportConfigurator config) {
        super(config);
        citation = ReferenceFactory.newGeneric();
        citation.setTitle("FloraTerms");
        Team team = Team.NewInstance();
        team.addTeamMember(Person.NewInstance(null, "Cui", null, "Hong"));
        team.addTeamMember(Person.NewInstance(null, "Cole", null, "Heather"));
        team.addTeamMember(Person.NewInstance(null, "Endara", null, "Lorena"));
        team.addTeamMember(Person.NewInstance(null, "Macklin", null, "James"));
        team.addTeamMember(Person.NewInstance(null, "Sachs", null, "Joel"));
        citation.setAuthorship(team);
        VerbatimTimePeriod datePublished = VerbatimTimePeriod.NewVerbatimInstance();
        datePublished.setStartYear(2014);
        datePublished.setStartMonth(6);
        datePublished.setStartDay(13);
        citation.setDatePublished(datePublished);
        Institution institution = Institution.NewNamedInstance("OTO System");
        institution.addUrl(URI.create("http://biosemantics.arizona.edu/OTO/"));
        citation.setInstitution(institution);
        URI uri;
        try {
            uri = new URI("https://terms.tdwg.org/wiki/FloraTerms");
            citation.setUri(uri);
        } catch (URISyntaxException e) {
        }

        propertyVoc = TermVocabulary.NewInstance(TermType.Property, DefinedTerm.class);
        propertyVoc.setLabel("Plant Glossary Properties");
        propertyVoc.addSource(IdentifiableSource.NewInstance(OriginalSourceType.Import, citation.getTitle(), null, citation, null));
    }

    public TermVocabulary<DefinedTerm> getPropertyVoc() {
        return propertyVoc;
    }

    void addVocabulary(TermVocabulary vocabulary) {
        vocabularies.add(vocabulary);
    }

    TermVocabulary checkVocabularies(String vocName, IVocabularyService vocabularyService){
        if(existingVocabularies.isEmpty()){
            existingVocabularies = vocabularyService.list(TermVocabulary.class, null, null, null, null);
        }
        for (TermVocabulary termVocabulary : vocabularies) {
            if(termVocabulary.getLabel().equals(vocName)){
                return termVocabulary;
            }
        }
        return null;
    }

    public boolean isTermPresent(String termName, ITermService termService) {
        if(existingTerms.isEmpty()){
            existingTerms = termService.list(State.class, null, null, null, null);
        }
        return existingTerms.stream().map(term->term.getLabel()).anyMatch(label->label.equals(termName));
    }

    Reference getCitation() {
        return citation;
    }

}
