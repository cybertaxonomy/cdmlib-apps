/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.format.reference.NomenclaturalSourceFormatter;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Import for Koperski & al., Referenzliste der Moose Deutschlands.
 *
 * @author a.mueller
 * @since 03.08.2023
 */
@Component
public class KoperskiImport extends SimpleExcelTaxonImport<KoperskiImportConfigurator>{

    private static final long serialVersionUID = 7686154384296707819L;
    private static final Logger logger = LogManager.getLogger();

    private static final String TAXON_ID = "TAXON_ID";
    private static final String TAXON_CHILD_OF = "TAXON_CHILD_OF";
    private static final String SYNFLAG = "SYNFLAG";
    private static final String STATUS = "Status";
    private static final String NAME = "Name";
    private static final String ZITAT = "Zitat";
    private static final String NOM_STATUS = "nom.Status";
    private static final String ORIG_NAME = "orig.Name";
    private static final String FIDE = "fide";
    
    private static final String SEC_TAXONOMIE = "sec. Taxonomie";
    private static final String SEC_KOMMENTAR = "sec Kommentar";
    private static final String TAX_KOMMENTAR = "Tax-Kommentar";
    
    static UUID uuidAddedInfo = UUID.fromString("50dcd9c2-8cc7-4c23-a115-0f5ed57c1ccc");
    static UUID notInGermany = UUID.fromString("024d0480-ec3a-458f-921a-a2f44a042187");
    static UUID uuidProParteInclusion = UUID.fromString("182a8d40-51ac-4596-9f48-11726f7b14d0");
    static UUID uuidFideType = UUID.fromString("be21f2f7-df4f-49fa-b36f-849975aacdee");
    static UUID uuidTaxonIdIdentifierType = UUID.fromString("31f3154c-c4a4-4519-a8c9-1057e9c08015");
    
    private static final int RECORDS_PER_TRANSACTION = 50000;
    private static final int LINENR = 500;

    private Map<Integer, Taxon> taxonMapping = new HashMap<>();
    private Map<String, Map<String, Taxon>> conceptTaxa = new HashMap<>();
    private Reference secRef = null;

    private SimpleExcelTaxonImportState<KoperskiImportConfigurator> state;
    private NonViralNameParserImpl parser = new NonViralNameParserImpl();
    
    private Taxon lastTaxon;
    private boolean isFirst = true;
    private ExtensionType addedInfoType;
    private MarkerType notInGermanyType;
    private TaxonRelationshipType proParteInclusion;
    private ExtensionType fideType;
    private IdentifierType taxonIdIdentifierType;
    
    
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<KoperskiImportConfigurator> state) {
        int line = state.getCurrentLine();
        if (isFirst) {
        	isFirst = false;
        	addedInfoType = getExtensionType(state, uuidAddedInfo, "AddedInfo", "AddedInfo", null);
        	notInGermanyType = getMarkerType(state, notInGermany, "Not in Germany", "Not in Germany", null);
        	proParteInclusion = getTaxonRelationshipType(state, uuidProParteInclusion, "Pro-parte-Inclusion", "Pro-parte-Inclusion", "<", "Inverse Pro-parte-Inclusion", "Inverse Pro-parte-Inclusion", ">", null);
        	fideType = getExtensionType(state, uuidFideType, "fide type", "fideType", null);
        	taxonIdIdentifierType = getIdentiferType(state, uuidTaxonIdIdentifierType, "Koperski Taxon ID" , "Koperski Taxon ID", null, null);
        }       
        
        if ((line % RECORDS_PER_TRANSACTION) == 0){
            newTransaction(state);
        }
        if ((line % LINENR) == 0){
            System.out.println(line);
        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        Integer taxonID = getIntValue(record, TAXON_ID);
        Integer childOf = getIntValue(record, TAXON_CHILD_OF);
        String synFlag = getValue(record, SYNFLAG);
        String status = getValue(record, STATUS);
        String nameStr = getValue(record, NAME);
        String zitat = getValue(record, ZITAT);
        String nomStatus = getValue(record, NOM_STATUS);
        String origName = getValue(record, ORIG_NAME);
        String fide = getValue(record, FIDE);
        String secTaxonomie = getValue(record, SEC_TAXONOMIE);
        String secComment = getValue(record, SEC_KOMMENTAR);
        String taxComment = getValue(record, TAX_KOMMENTAR);

        String row = String.valueOf(line) + "("+nameStr+"): ";

        if (taxonID == null) {
        	logger.warn(line + "TaxonID is null");
        	return;
        }
        
        boolean isAccepted = "1".equals(synFlag);
        boolean isSynonym = "b".equals(synFlag) || "x".equals(synFlag);
        boolean isConcept = "k".equals(synFlag);
        
        TaxonBase<?> taxonBase;
        String fideRef = null;

        boolean isNotInGermany = false;
        if(nameStr.startsWith("*")) {
        	nameStr = nameStr.substring(1);
        	isNotInGermany = true;
        }
        
        if (nameStr.contains(" fo. ")) {
        	//TODO do we really want normalization? Should be reverted in Export
        	nameStr = nameStr.replace(" fo. ", " f. ");
        }
        boolean isDeduplicated = false;
        
        if (isAccepted) {
        	
        	Rank defaultRank = childOf == null ? Rank.DIVISION() : Rank.GENUS();
        	TaxonName name = (TaxonName)parser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, defaultRank);
        	if (CdmUtils.isNotBlank(zitat)) {
        		String fullNameStr = nameStr + ", " + zitat; 
        		parser.parseReferencedName(name, fullNameStr, defaultRank, false);
//        		Reference nomRef = name.getNomenclaturalReference();
//        		nomRef.setTitleCache(zitat, true);
//        		name.setNomenclaturalReference(nomRef);
//        		name.getNomenclaturalSource();
        		if (CdmUtils.isNotBlank(fide)) {
        			fideRef = getFideReference(row, fide);
        		}
        	}
        	//taxon + classification
        	Taxon taxon = Taxon.NewInstance(name, getSecRef());
        	taxonBase = taxon;
        	Taxon parent = taxonMapping.get(childOf);
        	taxonMapping.put(taxonID, taxon);
        	lastTaxon = taxon;
        	getClassification().addParentChild(parent, taxon, null, null);
        	
        	//comment
        	if (isNotBlank(taxComment)) {
        		String noteStr = taxComment;
        		Annotation annotation = Annotation.NewInstance(noteStr, Language.GERMAN());
        		taxon.addAnnotation(annotation);
        	}
        	
        	taxon.addIdentifier(String.valueOf(taxonID), taxonIdIdentifierType);
        	
        	//original name
//        	if (isNotBlank(origName)) {
//            	String nameOnly = origName.substring(origName.indexOf("("), origName.indexOf(")"));
//            	nameOnly = nameOnly.substring(2, nameOnly.length() -1 );
//            	TaxonName originalName = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS(), name.getHomotypicalGroup());
//            	originalName.setGenusOrUninomial(nameOnly);
//            	name.getNomenclaturalSource(true).setNameUsedInSource(originalName);
//        	}
        	
        }else if (isSynonym) {
        	if ("−".equals(status)) {
        		//Misapplications
        		String appendedPhrase = nameStr.substring(nameStr.indexOf("auct."));
        		nameStr = nameStr.replace(appendedPhrase, "").trim();
        		TaxonName name = (TaxonName)parser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, Rank.GENUS());
        		Taxon taxon = Taxon.NewInstance(name, null);
        		taxon.setAppendedPhrase(appendedPhrase);
        		taxonBase = taxon;
        		lastTaxon.addMisappliedName(taxon, null, null);
        	}else {
        		if (nameStr.contains(", nom.")) {
        			nomStatus = nameStr.substring(nameStr.indexOf(", nom."));
        			nameStr = nameStr.replace(nomStatus, "").trim();
        			nomStatus = nomStatus.substring(2);
        		}
        		
        		TaxonName name;
        		if (isBlank(zitat)) {
        			name = (TaxonName)parser.parseReferencedName(nameStr, NomenclaturalCode.ICNAFP, Rank.GENUS());
        		}else {
        			name = (TaxonName)parser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, Rank.GENUS());
        		}
        		Synonym synonym = Synonym.NewInstance(name, getSecRef());
        		SynonymType synType = "≡".equals(status) || "≡B".equals(status) ? SynonymType.HOMOTYPIC_SYNONYM_OF :
        			"syn.".equals(status) || "?".equals(status) ? SynonymType.SYNONYM_OF : SynonymType.HETEROTYPIC_SYNONYM_OF;
        		synonym.setDoubtful(true);
        		
        		lastTaxon.addSynonym(synonym, synType);
        		if ("≡B".equals(status)) {
        			lastTaxon.getName().addBasionym(name);
        		}
        		
        		taxonBase = synonym;
        	}

        }else if (isConcept) {
        	
        	//name + sec
        	TaxonName name = (TaxonName)parser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, Rank.GENUS());
            Reference sec = getConceptSec(secTaxonomie);
            
            //taxon
            Map<String, Taxon> map = conceptTaxa.get(secTaxonomie);
            if (map == null) {
            	map = new HashMap<>();
            	conceptTaxa.put(secTaxonomie, map);
            }
            Taxon taxon = map.get(nameStr);
            if (taxon == null) {
            	taxon = Taxon.NewInstance(name, sec);
            	map.put(nameStr, taxon);

            	//classification
            	getClassification(secTaxonomie, sec).addChildTaxon(taxon, null, null);
            }else {
            	isDeduplicated = true;
            }
            taxonBase = taxon;

        	//rel type
        	TaxonRelationshipType relType = getTaxRelType(status, row);
        	TaxonRelationship rel = taxon.addTaxonRelation(lastTaxon, relType, null, null);

        	//comment
        	if (isNotBlank(secComment)) {
        		String noteStr = secComment;
        		Annotation annotation = Annotation.NewInstance(noteStr, Language.GERMAN());
        		rel.addAnnotation(annotation);
        	}
        	if (status.endsWith("?")) {
        		rel.setDoubtful(true);
        	}
        }else {
        	logger.warn("No status recognized");
        	taxonBase = null;
        }
        
        if (!isDeduplicated) { //may be null for deduplicated taxa
        	if (isNotInGermany) {
        		taxonBase.addMarker(notInGermanyType, isNotInGermany);
        	}
        	
        	//nom.Status
        	nomStatus = isBlank(nomStatus)? null : ", " + nomStatus;
        	origName = isBlank(origName) ? null : origName.startsWith(",") ? origName : " " + origName; 
        	
        	String addedInfo = CdmUtils.concat("", origName, nomStatus);
        	if (isNotBlank(addedInfo)) {
        		taxonBase.getName().addExtension(addedInfo, addedInfoType);
        	}
        	
        	if (fideRef != null) {
        		taxonBase.getName().addExtension(fideRef, fideType); 
        	}
        	
        	checkName(taxonBase, line);
        }
        getTaxonService().save(taxonBase);
    }

    private void checkName(TaxonBase<?> taxonBase, int line) {
		TaxonName name = taxonBase.getName();
		boolean isProtected = name.isProtectedNameCache() || name.isProtectedTitleCache() || name.isProtectedFullTitleCache();
		if (name.getNomenclaturalReference() != null) {
			Reference nomRef = name.getNomenclaturalReference();
			isProtected |= nomRef.isProtectedTitleCache() || nomRef.isProtectedAbbrevTitleCache();
			if (nomRef.getInReference() != null) {
				Reference inRef = nomRef.getInReference();
				isProtected |= inRef.isProtectedTitleCache() || inRef.isProtectedAbbrevTitleCache();
			}
		}
		if (isProtected) {
			logger.warn(line + ": Name could not be fully parsed: " + name.getFullTitleCache());
		}
	}

    private String getFideReference(String row, String fide) {
    	String[] split = fide.split(",");
    	String result = "";
    	for (String f : split) {
    		result = CdmUtils.concat("; ", result, getSingleFideReference(row, f.trim()));
    	}
    	return result;
    }
    
	private String getSingleFideReference(String row, String fide) {
		// TODO missing fide refs
		if ("!".equals(fide)) {return "Koperski et al. (2000)";}
		else if ("1".equals(fide)) {return "Wijk & al. (1959-1969)";}
		else if ("2".equals(fide)) {return "Crosby & al. (1992) und Crosby & Magill (1994, 1997)";}
		else if ("3".equals(fide)) {return "Missouri Botanical Garden (2000) ";}
		else if ("4".equals(fide)) {return "Grolle (1983a)";}
		else if ("5a".equals(fide)) {return "Isoviita (1966)";}
		else if ("5b".equals(fide)) {return "Koponen (1980)";}
		else if ("5c".equals(fide)) {return "Karttunen (1984)";}
		else if ("5d".equals(fide)) {return "Söderström & al. (1992)";}
		else if ("5e".equals(fide)) {return "Muñoz (1998)";}
		else if ("6".equals(fide)) {return "Koponen & al. (1977)";}
		else if ("7a".equals(fide)) {return "Hedenäs (1990b)";}
		else if ("7b".equals(fide)) {return "Hedenäs (1997b)";}
		else if ("8a".equals(fide)) {return "Muñoz (1997)";}
		else if ("8b".equals(fide)) {return "Hedenäs (1997a)";}
		else if ("8c".equals(fide)) {return "Robinson & Ignatov (1997)";}
		else if ("9a".equals(fide)) {return "Kramer (1980)";}
		else if ("9b".equals(fide)) {return "Blom (1996)";}
		else if ("10".equals(fide)) {return "Zander (1993)";}
		else if ("11".equals(fide)) {return "Bonner (1962-1987) und Geissler & Bischler (1985, 1989, 1990)";}
		else if ("12".equals(fide)) {return "Nyholm (1987, 1993, 1998)";}
		else if ("13".equals(fide)) {return "Geissler & Maier (1995)";}
		else if ("14".equals(fide)) {return "Ochyra (1994)";}
		else if ("15".equals(fide)) {return "Düll & Meinunger (1989)";}
		else if ("16".equals(fide)) {return "Grolle (1976)";}
		else if ("17a".equals(fide)) {return "Frisvoll (1983)";}
		else if ("17b".equals(fide)) {return "Frisvoll (1988)";}
		else if ("18".equals(fide)) {return "Burley & Pritchard (1990)";}
		else if ("19".equals(fide)) {return "Koponen (1994)";}
		else if ("20a".equals(fide)) {return "Smith & Whitehouse (1978)";}
		else if ("20b".equals(fide)) {return "Isoviita (1984)";}
		else if ("20c".equals(fide)) {return "Bischler-Causse & Boisselier-Dubayle (1991)";}
		else if ("20d".equals(fide)) {return "Townsend (1995)";}
		else if ("21".equals(fide)) {return "Horton (1983)";}
		else if ("22".equals(fide)) {return "Arnell (1956)";}
		else if ("23".equals(fide)) {return "Crum & Anderson (1981)";}
		else if ("24a".equals(fide)) {return "Vitt (1976)";}
		else if ("24b".equals(fide)) {return "Mogensen (1980)";}
		else if ("24c".equals(fide)) {return "Damsholt & Long (1983)";}
		else if ("24d".equals(fide)) {return "Hedenäs (1989)";}
		else if ("24e".equals(fide)) {return "Hedenäs (1990c)";}
		else if ("24f".equals(fide)) {return "Nyholm (1995)";}
		else if ("25".equals(fide)) {return "Schimper (1876)";}
		else if ("26".equals(fide)) {return "Düll (1984)";}
		else if ("27".equals(fide)) {return "Smith (1980)";}
		else if ("28".equals(fide)) {return "Grolle & Long (2000)";}
		else if ("29".equals(fide)) {return "Greuter & al. (1993)";}
		else if ("30a".equals(fide)) {return "Engel (1978)";}
		else if ("30b".equals(fide)) {return "Engel (1981)";}
		else if ("30c".equals(fide)) {return "Karttunen (1988)";}
		else if ("30d".equals(fide)) {return "Karttunen (1990)";}
		else if ("31".equals(fide)) {return "Crundwell (1970)";}
		else if ("32".equals(fide)) {return "Corley & al. (1981)";}
		else if ("33".equals(fide)) {return "Corley & Crundwell (1991)";}
		else if ("34".equals(fide)) {return "Ludwig & al. (1996)";}
		else if ("35".equals(fide)) {return "Grolle (1983b)";}
		else if ("36".equals(fide)) {return "Smith (1990)";}
		else if ("37".equals(fide)) {return "Müller (1954-1957)";}
		else if ("38".equals(fide)) {return "Greuter & al. (2000) ";}
		else if ("39".equals(fide)) {return "Daniels & Eddy (1990)";}
		logger.warn("fide not recognized:" + fide);
		return null;
	}

	private TaxonRelationshipType getTaxRelType(String status, String line) {
		status = status.trim();
		status = status.replace(" ", "");  //strange whitespace after <
		if (status.endsWith("?")) {
			status = status.substring(0, status.length()-1);
		}
    	if ("≶".equals(status)) {
			return TaxonRelationshipType.OVERLAPS();
		} else if ("<".equals(status)) {
			return proParteInclusion;
		} else if (">".equals(status)) {
			return TaxonRelationshipType.INCLUDES();
		} else if ("≶".equals(status)) {
			return TaxonRelationshipType.OVERLAPS();
		} else if ("".equals(status)) {
			return TaxonRelationshipType.UNCLEAR();
		} else if ("≙".equals(status)) {
			return TaxonRelationshipType.CONGRUENT_TO();
		} else if ("≠".equals(status)) {
			return TaxonRelationshipType.EXCLUDES();
		} else {
			logger.warn(line + "ConceptRelType not handled: " + status);
			return null;
		}    	
	}

	private Map<String, Reference> secRefMap = new HashMap<>();
    private Reference getConceptSec(String secTaxonomie) {
    	secTaxonomie = secTaxonomie.replace("sec. ", "");
		Reference result = secRefMap.get(secTaxonomie);
		if (result == null) {
			result = ReferenceFactory.newGeneric();
			result.setTitle(secTaxonomie);
			secRefMap.put(secTaxonomie, result);
		}
		return result;
	}

	private Integer getIntValue(Map<String, String> record, String taxonId) {
		String str = record.get(taxonId);
		return str == null? null : Integer.valueOf(str);
	}

    private void newTransaction(SimpleExcelTaxonImportState<KoperskiImportConfigurator> state) {
        commitTransaction(state.getTransactionStatus());
        secRef = null;
        classification = null;
//        state.getDeduplicationHelper().reset();
        state.setSourceReference(null);
        System.gc();
        state.setTransactionStatus(startTransaction());
    }

    private Reference getSecRef() {
        if (secRef == null){
            secRef = getReferenceService().find(state.getConfig().getSecUuid());
            if (secRef == null){
                secRef = ReferenceFactory.newBook();
                secRef.setTitle("Referenzliste der Moose Deutschlands");
                Team team = Team.NewInstance();
                addPerson(team, "Monika","Koperski");
                addPerson(team, "Michael","Sauer");
                addPerson(team, "Walther","Braun");
                addPerson(team, "S. Rob","Gradstein");
                secRef.setAuthorship(team);
                secRef.setDatePublished(TimePeriodParser.parseStringVerbatim("2000"));
                secRef.setPublisher("Bundesamt für Naturschutz");
                secRef.setPlacePublished("Bonn-Bad Godesberg");
                secRef.setIsbn("3-7843-3504-7");
                secRef.setSeriesPart("Schriftenreihe für Vegetationskunde");
                secRef.setVolume("Heft 34");
            }
        }
        return secRef;
    }

    private void addPerson(Team team, String firstName, String familyName) {
		Person person = Person.NewInstance(null, familyName, null, firstName);
		team.addTeamMember(person);
	}

    private Classification classification;
    private Classification getClassification() {
        if (classification == null) {
        	classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        }
        if (classification == null){
            classification = Classification.NewInstance(
            		NomenclaturalSourceFormatter.INSTANCE().format(getSecRef(), null),
            		getSecRef(), Language.LATIN());
            classification.setUuid(state.getConfig().getClassificationUuid());
            getClassificationService().save(classification);
        }
        return classification;
    }
    
    private Map<String,Classification> classificationMap = new HashMap<>();
    private Map<String,UUID> classificationUuidMap = new HashMap<>();
    private Classification getClassification(String secRef, Reference sec) {
    	secRef = secRef.replace("sec. ", "");
    	Classification classification = classificationMap.get(secRef);
    	if (classification == null) {
    		UUID uuid = classificationUuidMap.get(secRef);
    		if (uuid != null) {
    			classification = getClassificationService().find(uuid);
    		}
    	}
        if (classification == null){
            classification = Classification.NewInstance(
            		secRef, sec, Language.DEFAULT());
            getClassificationService().save(classification);
            classificationMap.put(secRef, classification);
            classificationUuidMap.put(secRef, classification.getUuid());
        }
        return classification;
    }

}
