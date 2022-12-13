/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
/**
 * This class creates all area vocabularies and all distribution status vocabularies
 * for the German redlists.
 *
 * @author a.oppermann
 * @author a.mueller
 * @since 04.07.2013
 */
@Component
public class BfnXmlImportAddtionalTerms extends BfnXmlImportBase {

    private static final long serialVersionUID = 8997499338798245806L;
    private static final Logger logger = LogManager.getLogger();

    public enum Vocabulary{
        GERMAN_FEDERAL_STATES("Bundesländer"),
        GERMAN_COMBINED_STATES("Kombinierte Bundesländer"),
        GERMAN_MARINE_ALGAE_AREAS("Marine Algen Gebiete"),
        GERMAN_MARINE_INVERTEBRATE_AREAS("Marine Invertebraten Gebiete"),

        GERMAN_PRESENCE_TERMS("Vorkommensstatus"),
        GERMAN_ESTABLISHMENT_TERMS("Etablierungsstatus");

        private final String vocabulary;

        private Vocabulary(final String vocabulary) {
            this.vocabulary = vocabulary;
        }

        @Override
        public String toString() {
            return vocabulary;
        }
    }


    private static final List<String> GERMAN_PRESENCE_ABSENCE_TERMS = Arrays.asList(new String[] {
            "x:" + BfnXmlTransformer.VORHANDEN ,
            "?:" + BfnXmlTransformer.VORHANDEN_UNSICHER,
            "#:" + BfnXmlTransformer.ABWESEND_ABGELEHNT,
            "-:" + BfnXmlTransformer.ABWESEND_KEIN_NACHWEIS,

            "a:" + BfnXmlTransformer.ABWESEND,
            "aa:"  + BfnXmlTransformer.ABWESEND_AUSGESTORBEN,
            "an:"  + BfnXmlTransformer.ABWESEND_SEIT1980,
            "af:" + BfnXmlTransformer.ABWESEND_FEHLEINGABE,

            "v+:" + BfnXmlTransformer.VORHANDEN_EINBUERGERUNG,
            "ve:" + BfnXmlTransformer.VORHANDEN_ETABLIERT,
            "vk:" + BfnXmlTransformer.VORHANDEN_KULTIVIERT_DOMESTIZIERT,
            "vu:" + BfnXmlTransformer.VORHANDEN_UNBESTAENDIG,
            "v?:" + BfnXmlTransformer.VORHANDEN_VORKOMMEN_UNSICHER,

    });

    private static final List<String> GERMAN_ESTABLISHMENT_STATUS_TERMS = Arrays.asList(new String[] {
            "A:Archaeophyt",
            "I:Indigen",
            "K:Kulturpflanze / domestiziertes Tier",
            "N:Neophyt",
            "KF:Kultuflüchtling"
    });

    private static final List<String> GERMAN_FEDERAL_STATES = Arrays.asList(new String[] {
            "DE:Deutschland",
            "BW:Baden-Württemberg",
            "BY:Bayern",
            "BE:Berlin",
            "BB:Brandenburg",
            "HB:Bremen",
            "HH:Hamburg",
            "HE:Hessen",
            "MV:Mecklenburg-Vorpommern",
            "NI:Niedersachsen",
            "NW:Nordrhein-Westfalen",
            "RP:Rheinland-Pfalz",
            "SL:Saarland",
            "SN:Sachsen",
            "ST:Sachsen-Anhalt",
            "SH:Schleswig-Holstein",
            "TH:Thüringen"
    });

    private static final List<String> GERMAN_COMBINED_STATES = Arrays.asList(new String[] {
            "BB+BE:Brandenburg und Berlin",
            "SH+HH:Schleswig-Holstein und Hamburg"
    });

    private static final List<String> GERMAN_MARINE_ALGAE_AREAS = Arrays.asList(new String[] {
            "HGL:Helgoland",
            "NIW:Niedersächsisches Wattenmeer",
            "SHW:Schleswig-Holsteinisches Wattenmeer",
            "SHO:Schleswig-Holsteinische Ostsee",
            "MVO:Mecklenburg-Vorpommerische Ostsee"
    });

    private static final List<String> GERMAN_MARINE_INVERTEBRATE_AREAS = Arrays.asList(new String[] {
            "ÄWN:Ästuarien und Watt Nordsee",
            "SuN:Sublitoral Nordsee",
            "Hel:Helgoland2",  //TODO: the 2 is a workaround to distinguish from Algae Helgoloand, still needs to be discussed with BfN if these are 2 different areas
            "Dog:Doggerbank",
            "Ost:Ostsee"
    });


    /** Hibernate classification vocabulary initialization strategy */
    private static final List<String> VOC_CLASSIFICATION_INIT_STRATEGY = Arrays.asList(new String[] {
            "classification.$",
            "classification.rootNodes",
            "childNodes",
            "childNodes.taxon",
            "childNodes.taxon.name",
            "taxonNodes",
            "taxonNodes.taxon",
            "synonyms",
            "taxon.*",
            "taxon.secSource.citation",
            "taxon.name.*",
            "taxon.synonyms",
            "termVocabulary.*",
            "terms",
            "namedArea"
    });


    public BfnXmlImportAddtionalTerms(){
        super();
    }


	@Override
	public void doInvoke(BfnXmlImportState state){
		logger.info("create german terms ...");

		TransactionStatus tx = startTransaction();
		//areas
		createTerms(GERMAN_FEDERAL_STATES, Vocabulary.GERMAN_FEDERAL_STATES, TermType.NamedArea);
		createTerms(GERMAN_MARINE_ALGAE_AREAS, Vocabulary.GERMAN_MARINE_ALGAE_AREAS, TermType.NamedArea);
		createTerms(GERMAN_MARINE_INVERTEBRATE_AREAS, Vocabulary.GERMAN_MARINE_INVERTEBRATE_AREAS, TermType.NamedArea);
		createTerms(GERMAN_COMBINED_STATES, Vocabulary.GERMAN_COMBINED_STATES, TermType.NamedArea);
		//distribution status
        createTerms(GERMAN_PRESENCE_ABSENCE_TERMS, Vocabulary.GERMAN_PRESENCE_TERMS, TermType.PresenceAbsenceTerm);
		createTerms(GERMAN_ESTABLISHMENT_STATUS_TERMS, Vocabulary.GERMAN_ESTABLISHMENT_TERMS, TermType.PresenceAbsenceTerm);

		commitTransaction(tx);
		logger.info("end create german terms.");
		return;

	}

	private void createTerms(List<String> termList, Vocabulary vocabulary, TermType termType){
	   NamedArea parentGermany = null;
	   PresenceAbsenceTerm lastParent = null;
	   int id = 0;
       for(String strTerm : termList){
           //Split string into label and abbrevated label
           String[] splittedStrings = StringUtils.splitByWholeSeparator(strTerm, ":");
           String abbrevatedLabel = splittedStrings[0];
           String label = splittedStrings[1];
           //get UUID and load existing term
           UUID termUuuid = null;
           try {
               if(vocabulary == Vocabulary.GERMAN_PRESENCE_TERMS){
                   termUuuid = BfnXmlTransformer.getGermanAbsenceTermUUID(label);
               }else if(vocabulary == Vocabulary.GERMAN_ESTABLISHMENT_TERMS){
                   termUuuid = BfnXmlTransformer.getGermanEstablishmentTermUUID(label);
               }else if(termType == TermType.NamedArea){
                   termUuuid = BfnXmlTransformer.getAreaUUID(label);
               }
           } catch (UnknownCdmTypeException e) {
               logger.warn("Could not match term to uuid: "+e.toString());
               termUuuid = UUID.randomUUID();
           }

           @SuppressWarnings("rawtypes")
           DefinedTermBase term = getTermService().load(termUuuid);
           if(term != null){
               //already in the db, so no need to step through the whole process again.
               return;
           }else{
               if(termType == TermType.NamedArea){
                   //Namedareas
                   NamedArea namedArea = NamedArea.NewInstance(label, label, abbrevatedLabel);
                   term = namedArea;
                   term.setIdInVocabulary(Integer.toString(id));
                   if (vocabulary == Vocabulary.GERMAN_FEDERAL_STATES){
                       namedArea.setType(NamedAreaType.ADMINISTRATION_AREA());
                       if(label.equalsIgnoreCase("Deutschland")){
                           namedArea.setLevel(NamedAreaLevel.COUNTRY());
                           parentGermany = (NamedArea) term;
                       }else{
                           namedArea.setLevel(NamedAreaLevel.STATE());
                           term.setPartOf(parentGermany);
                       }
                   }else{
                       if (vocabulary == Vocabulary.GERMAN_COMBINED_STATES){
                           namedArea.setType(NamedAreaType.ADMINISTRATION_AREA());
                       }else{
                           namedArea.setType(NamedAreaType.NATURAL_AREA());
                       }
                   }
               }else{
                   term = PresenceAbsenceTerm.NewPresenceInstance(label, label, abbrevatedLabel);
                   term.setIdInVocabulary(abbrevatedLabel);
                   if(vocabulary.equals(Vocabulary.GERMAN_PRESENCE_TERMS)){
                       //create hierarchy of terms
                       if(abbrevatedLabel.length() >1 ){
                           ((PresenceAbsenceTerm)term).setPartOf(lastParent);
                       }else {
                           lastParent = (PresenceAbsenceTerm) term;
                       }
                   }
               }
               term.setUuid(termUuuid);
           }
           createOrUpdateTermVocabulary(termType, getVocabularyService(), term, vocabulary.toString());
           id++;
       }
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private OrderedTermVocabulary createOrUpdateTermVocabulary(TermType termType, IVocabularyService vocabularyService, DefinedTermBase term, String strTermVocabulary) {
		OrderedTermVocabulary termVocabulary = null;
		UUID vocUUID = null;
		try {
			vocUUID=BfnXmlTransformer.getRedlistVocabularyUUID(strTermVocabulary);
		} catch (UnknownCdmTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(vocUUID != null){
			termVocabulary = (OrderedTermVocabulary) vocabularyService.load(vocUUID);
		}
		//lookup via String in case uuid lookup does not work
		if(termVocabulary == null && vocUUID == null){
			List<TermVocabulary> vocList = vocabularyService.list(TermVocabulary.class, null, null, null, VOC_CLASSIFICATION_INIT_STRATEGY);
			for(TermVocabulary tv : vocList){
				if(tv.getTitleCache().equalsIgnoreCase(strTermVocabulary)){
					termVocabulary = (OrderedTermVocabulary) tv;
				}
			}
		}
		//create termvocabulary
		if(termVocabulary == null){
			termVocabulary = OrderedTermVocabulary.NewOrderedInstance(termType, null, strTermVocabulary, strTermVocabulary, strTermVocabulary, null);
			if(vocUUID != null){
				termVocabulary.setUuid(vocUUID);
			}
		}
		termVocabulary.addTerm(term);
		vocabularyService.saveOrUpdate(termVocabulary);

		return termVocabulary;
	}

	@Override
    protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoAdditionalTerms();
	}




    @Override
    public boolean doCheck(BfnXmlImportState state){
        boolean result = true;
        //TODO needs to be implemented
        return result;
    }
}
