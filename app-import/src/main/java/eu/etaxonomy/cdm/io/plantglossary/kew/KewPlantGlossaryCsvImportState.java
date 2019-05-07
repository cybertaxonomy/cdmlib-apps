/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary.kew;

import java.util.ArrayList;
import java.util.List;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.io.csv.in.CsvImportState;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.persistence.dto.UuidAndTitleCache;

/**
 *
 * @author pplitzner
 * @since Jan 25, 2019
 *
 */
public class KewPlantGlossaryCsvImportState extends CsvImportState<KewPlantGlossaryCsvImportConfigurator> {

    private List<TermVocabulary> existingVocabularies = new ArrayList<>();
    private List<UuidAndTitleCache<DefinedTermBase>> existingTerms = new ArrayList<>();
    private final Reference citation;
    private final TermVocabulary<DefinedTerm> structureVoc;


    protected KewPlantGlossaryCsvImportState(KewPlantGlossaryCsvImportConfigurator config) {
        super(config);
        structureVoc = TermVocabulary.NewInstance(TermType.Structure, DefinedTerm.class);
        structureVoc.setLabel("Structures");

        citation = ReferenceFactory.newBook();
        citation.setTitle("The Kew Plant Glossary, an illustrated dictionary of plant terms");
        citation.setAuthorship(Person.NewInstance(null, "J. Beentje", null, "Henk"));
        VerbatimTimePeriod datePublished = VerbatimTimePeriod.NewVerbatimInstance();
        datePublished.setStartYear(2010);
        citation.setDatePublished(datePublished);
        Institution institution = Institution.NewNamedInstance("Royal Botanic Gardens, Kew");
        citation.setInstitution(institution);
        citation.setIsbn("978-1-84246-422-9");
    }

    public boolean isTermPresent(String termName, ITermService termService) {
        if(existingTerms.isEmpty()){
            existingTerms = termService.getUuidAndTitleCache(null, "*");
        }
        return existingTerms.stream().anyMatch(term->term.getTitleCache().equals(termName));
    }

    TermVocabulary<DefinedTerm> getStructureVoc() {
        return structureVoc;
    }

    Reference getCitation() {
        return citation;
    }

}
