/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.OrderedTermBase;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
/**
 *
 * @author a.oppermann
 * @date 04.07.2013
 *
 */
@Component
public class BfnXmlImportAddtionalTerms extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {

    private static final Logger logger = Logger.getLogger(BfnXmlImportAddtionalTerms.class);

	public BfnXmlImportAddtionalTerms(){
		super();
	}

	@Override
	public boolean doCheck(BfnXmlImportState state){
		boolean result = true;
		//TODO needs to be implemented
		return result;
	}

    public enum Vocabulary{
        GERMAN_FEDERAL_STATES("Bundesländer"),
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

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void doInvoke(BfnXmlImportState state){
		logger.info("create german terms ...");
		IVocabularyService vocabularyService = getVocabularyService();
		ITermService termService = getTermService();
		TransactionStatus tx = startTransaction();
//		createGermanNamedAreas(state, vocabularyService, termService);
		createGermanTerms(vocabularyService, termService, GERMAN_FEDERAL_STATES, Vocabulary.GERMAN_FEDERAL_STATES);
		createGermanTerms(vocabularyService, termService, GERMAN_PRESENCE_ABSENCE_TERMS, Vocabulary.GERMAN_PRESENCE_TERMS);
		createGermanTerms(vocabularyService, termService, GERMAN_ESTABLISHMENT_STATUS_TERMS, Vocabulary.GERMAN_ESTABLISHMENT_TERMS);
		commitTransaction(tx);
		logger.info("end create german terms.");
		return;

	}
//
//
//	private void createGermanNamedAreas(BfnXmlImportState state ,IVocabularyService vocabularyService, ITermService termService) {
//		int id = 0;
//		NamedArea parentGermany = null;
//		for(String strGermanState:GERMAN_FEDERAL_STATES){
//			UUID germanStateUUID;
//			try {
//				germanStateUUID = BfnXmlTransformer.getGermanStateUUID(strGermanState);
//			} catch (UnknownCdmTypeException e) {
//				logger.warn("Could not match german state to uuid: "+e.toString());
//				germanStateUUID = UUID.randomUUID();
//			}
//			NamedArea germanState = (NamedArea)termService.load(germanStateUUID);
//			if(germanState != null){
//				//already in the db, so no need to step through the whole process again.
//				return;
//			}else{
//				germanState = NamedArea.NewInstance(strGermanState, strGermanState, strGermanState);
//				germanState.setUuid(germanStateUUID);
//				germanState.setType(NamedAreaType.ADMINISTRATION_AREA());
//				germanState.setIdInVocabulary(Integer.toString(id));;
//				if(strGermanState.equalsIgnoreCase("Deutschland")){
//					germanState.setLevel(NamedAreaLevel.COUNTRY());
//					parentGermany = germanState;
//				}else{
//					germanState.setLevel(NamedAreaLevel.STATE());
//					germanState.setPartOf(parentGermany);
//				}
//			}
//			createOrUpdateTermVocabulary(TermType.NamedArea, vocabularyService, germanState, "German Federal States");
//			id++;
//		}
//	}

	private void createGermanTerms(IVocabularyService vocabularyService,ITermService termService , List<String> termList, Vocabulary vocabulary){
	   NamedArea parentGermany = null;
	   PresenceAbsenceTerm parent = null;
	   int id = 0;
       for(String strGermanTerm:termList){
           //Split string into label and abbrevated label
           String[] splittedStrings = StringUtils.splitByWholeSeparator(strGermanTerm, ":");
           String abbrevatedLabel = splittedStrings[0];
           String label = splittedStrings[1];
           //get UUID and load existing term
           UUID termUuuid = null;
           try {
               if(vocabulary.equals(Vocabulary.GERMAN_PRESENCE_TERMS)){
                   termUuuid = BfnXmlTransformer.getGermanAbsenceTermUUID(label);
               }else if(vocabulary.equals(Vocabulary.GERMAN_ESTABLISHMENT_TERMS)){
                   termUuuid = BfnXmlTransformer.getGermanEstablishmentTermUUID(label);
               }else if(vocabulary.equals(Vocabulary.GERMAN_FEDERAL_STATES)){
                   termUuuid = BfnXmlTransformer.getGermanStateUUID(label);
               }
           } catch (UnknownCdmTypeException e) {
               logger.warn("Could not match term to uuid: "+e.toString());
               termUuuid = UUID.randomUUID();
           }
           DefinedTermBase term = termService.load(termUuuid);
           if(term != null){
               //already in the db, so no need to step through the whole process again.
               return;
           }else{
               if(vocabulary.equals(Vocabulary.GERMAN_FEDERAL_STATES)){
                   //Namedareas
                   term = NamedArea.NewInstance(label, label, abbrevatedLabel);
                   ((NamedArea) term).setType(NamedAreaType.ADMINISTRATION_AREA());
                   term.setIdInVocabulary(Integer.toString(id));;
                   if(label.equalsIgnoreCase("Deutschland")){
                       ((NamedArea) term).setLevel(NamedAreaLevel.COUNTRY());
                       parentGermany = (NamedArea) term;
                   }else{
                       ((NamedArea) term).setLevel(NamedAreaLevel.STATE());
                       term.setPartOf(parentGermany);
                   }
               }else{
                   term = PresenceAbsenceTerm.NewPresenceInstance(label, label, abbrevatedLabel);
                   term.setIdInVocabulary(abbrevatedLabel);
                   if(vocabulary.equals(Vocabulary.GERMAN_PRESENCE_TERMS)){
                       //create hierarchy of terms
                       if(label.equalsIgnoreCase("abwesend")){
                           parent = (PresenceAbsenceTerm) term;
                       }else if(label.equalsIgnoreCase("vorkommend")){
                           parent = (PresenceAbsenceTerm) term;
                       }else{
                           ((PresenceAbsenceTerm)term).setPartOf(parent);
                       }
                   }
               }
               term.setUuid(termUuuid);
           }
           if(vocabulary.equals(Vocabulary.GERMAN_FEDERAL_STATES)){
               createOrUpdateTermVocabulary(TermType.NamedArea, vocabularyService, term, vocabulary.toString());
               id++;
           }else{
               createOrUpdateTermVocabulary(TermType.PresenceAbsenceTerm, vocabularyService, term, vocabulary.toString());
           }
       }
	}


	/**
	 * @param vocabularyService
	 * @param term
	 * @param vocUUID
	 */
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
			termVocabulary = OrderedTermVocabulary.NewInstance(termType, strTermVocabulary, strTermVocabulary, strTermVocabulary, null);
			if(vocUUID != null){
				termVocabulary.setUuid(vocUUID);
			}
		}
		termVocabulary.addTerm((OrderedTermBase) term);
		vocabularyService.saveOrUpdate(termVocabulary);

		return termVocabulary;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
    protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoTaxonNames();
	}



	private static final List<String> GERMAN_PRESENCE_ABSENCE_TERMS = Arrays.asList(new String[] {
	        "a:abwesend",
	        "aa:abwesend - ausgestorben",
	        "af:abwesend - frühere Fehleingabe",
	        "v:vorkommend",
	        "v+:vorkommend - in Einbürgerung befindlich",
	        "ve:vorkommend - etabliert",
	        "vk:vorkommend - kultiviert, domestiziert",
	        "vu:vorkommend - unbeständig",
	        "vs:vorkommend - Vorkommen unsicher",
	        "s:vorkommend - unsicher"

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


    /** Hibernate classification vocabulary initialisation strategy */
    private static final List<String> VOC_CLASSIFICATION_INIT_STRATEGY = Arrays.asList(new String[] {
    		"classification.$",
    		"classification.rootNodes",
    		"childNodes",
    		"childNodes.taxon",
            "childNodes.taxon.name",
            "taxonNodes",
            "taxonNodes.taxon",
            "synonymRelations",
            "taxon.*",
            "taxon.sec",
            "taxon.name.*",
            "taxon.synonymRelations",
            "termVocabulary.*",
            "terms",
            "namedArea"

    });


}
