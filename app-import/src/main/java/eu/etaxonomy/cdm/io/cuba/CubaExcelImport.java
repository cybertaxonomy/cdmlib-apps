/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.cuba;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.cyprus.CyprusRow;
import eu.etaxonomy.cdm.io.excel.common.ExcelImporterBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component
public class CubaExcelImport extends ExcelImporterBase<CubaImportState> {
    private static final long serialVersionUID = -747486709409732371L;

    private static final Logger logger = Logger.getLogger(CubaExcelImport.class);

    private static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();
    private static NomenclaturalCode nc = NomenclaturalCode.ICNAFP;

    private  static List<String> expectedKeys= Arrays.asList(new String[]{"Fam.","(Fam.)","Taxón","(Notas)","Syn.","End","Ind","Ind? D","Nat","Dud P","Adv","Cult C","CuW","PR PR*","Art","Hab(*)","May","Mat","IJ","CuC","VC","Ci","SS","CA","Cam","LT","CuE","Gr","Ho","SC","Gu","Esp","Ja","PR","Men","Bah","Cay","AmN","AmC","AmS","VM"});

    private  static List<String> dummy= Arrays.asList(new String[]{
            "(Fam.)","Syn.","Ind? D","Nat","Dud P","Adv","Cult C",
            "CuW","PR PR*","Art","Hab(*)","May","Mat","IJ",
            "CuC","VC","Ci","SS","CA","Cam","LT",
            "CuE","Gr","Ho","SC","Gu",
            "Esp","Ja","PR","Men","Bah","Cay",
            "AmN","AmC","AmS","VM"});


	@Override
    protected void analyzeRecord(HashMap<String, String> record, CubaImportState state) {

		Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn("Unexpected Key: " + key);
            }
        }

        //Fam.
        TaxonNode familyTaxon = getFamilyTaxon(record, state);
        if (familyTaxon == null){
            logger.warn("Family not recognized. Do not handle row");
            return;
        }

        //(Fam.)
        //TODO

        //Taxón
        Taxon taxon = makeTaxon(record, state, familyTaxon);

        //(Notas)
        makeNotes(record, state, taxon);

        //Syn.
        makeSynonyms(record, state, taxon);

        //End, Ind, Ind? D, Nat N, Dud P, Adv A, Cult C
        makeCubanDistribution(record, state, taxon);

    	return;
    }


    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeCubanDistribution(HashMap<String, String> record, CubaImportState state, Taxon taxon) {
        try {
            NamedArea cuba = getNamedArea(state, state.getTransformer().getNamedAreaUuid("C"), null, null, null, null, null);
            TaxonDescription desc = getTaxonDescription(taxon, false, true);
            PresenceAbsenceTerm status =  makeCubanStatus(record, state);
            Distribution distribution = Distribution.NewInstance(cuba, status);
            desc.addElement(distribution);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param record
     * @param state
     * @return
     * @throws UndefinedTransformerMethodException
     */
    private PresenceAbsenceTerm makeCubanStatus(HashMap<String, String> record, CubaImportState state) throws UndefinedTransformerMethodException {
        boolean isAbsent = false;  //TODO

        String endemicStr = getValue(record, "End");
        String indigenousStr = getValue(record, "Ind");
        String indigenousDoubtStr = getValue(record, "Ind? D");
        String naturalisedStr = getValue(record, "Nat");
        String dudStr = getValue(record, "Dud P");
        String advStr = getValue(record, "Adv");
        String cultStr = getValue(record, "Cult C");

        if (endemicStr != null){
            boolean allNull = checkAllNull(indigenousStr, indigenousDoubtStr, naturalisedStr, dudStr, advStr, cultStr);
            if (!endemicStr.equals("+")){
                logger.warn("Endemic not recognized: " + endemicStr);
                return null;
            }else if (! allNull){
                logger.warn("Cuban state is endemic but others exist");
                return null;
            }else{
                return PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();
            }
        }else if (indigenousStr != null){
            boolean allNull = checkAllNull(indigenousDoubtStr, naturalisedStr, dudStr, advStr, cultStr);
            if (!checkPlusMinus(indigenousStr)){
                logger.warn("Indigenous not recognized: " + indigenousStr);
                return null;
            }else if (! allNull){
                //TODO may this exist?
                logger.warn("Cuban state is indigenous but others exist");
                return null;
            }else if(indigenousStr.equals("+")){
                UUID indigenousUuid = state.getTransformer().getPresenceTermUuid("Ind.");
                PresenceAbsenceTerm indigenousState = getPresenceTerm(state, indigenousUuid, null, null, null);
                return indigenousState;
            }else if(indigenousStr.equals("-") || indigenousStr.equals("–")){
                logger.warn("Indigenous status '-' not yet handled)");
                return PresenceAbsenceTerm.ABSENT();
            }else{
                logger.warn("Indigenous not recognized: " + indigenousStr);
                return null;
            }
        }else if(indigenousDoubtStr != null){

        }

        return null;
    }


    /**
     * @param indigenousStr
     * @return
     */
    private boolean checkPlusMinus(String str) {
        return str.equals("+") || str.equals("-") || str.equals("–");
    }


    /**
     * @param indigenousStr
     * @param indigenousDoubtStr
     * @param naturalisedStr
     * @param dudStr
     * @param advStr
     * @param cultStr
     */
    private boolean checkAllNull(String ... others) {
        for (String other : others){
            if (other != null){
                return false;
            }
        }
        return true;
    }


    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeSynonyms(HashMap<String, String> record, CubaImportState state, Taxon taxon) {
        // TODO Auto-generated method stub

    }


    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeNotes(HashMap<String, String> record, CubaImportState state, Taxon taxon) {
        String notesStr = getValue(record, "(Notas)");
        if (notesStr == null){
            return;
        }else{
            Annotation annotation = Annotation.NewDefaultLanguageInstance(notesStr);
            //TODO
            annotation.setAnnotationType(AnnotationType.EDITORIAL());
            taxon.addAnnotation(annotation);
        }
    }


    /**
     * @param record
     * @param state
     * @param familyTaxon
     * @return
     */
    private Taxon makeTaxon(HashMap<String, String> record, CubaImportState state, TaxonNode familyNode) {
        String taxonStr = getValue(record, "Taxón");
        if (taxonStr == null){
            return null;
        }
        boolean isAbsent = false;
        if (taxonStr.startsWith("[") && taxonStr.endsWith("]")){
            taxonStr = taxonStr.substring(1, taxonStr.length() - 1);
            isAbsent = true;
        }

        TaxonNameBase<?,?> botanicalName = nameParser.parseFullName(taxonStr, nc, Rank.SPECIES());
        if (botanicalName.isProtectedTitleCache()){
            logger.warn("Taxon could not be parsed: " + taxonStr);
        }
        Reference<?> sec = null;
        Taxon taxon = Taxon.NewInstance(botanicalName, sec);
        familyNode.addChildTaxon(taxon, null, null);

        return taxon;
    }


    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(HashMap<String, String> record, CubaImportState state) {
        String familyStr = getValue(record, "Fam.");
        if (familyStr == null){
            return null;
        }
        Taxon family = state.getHigherTaxon(familyStr);
        TaxonNode familyNode;
        if (family != null){
            familyNode = family.getTaxonNodes().iterator().next();
        }else{
            BotanicalName name = BotanicalName.NewInstance(Rank.FAMILY());
            name.setGenusOrUninomial(familyStr);
            Reference<?> sec = null;
            Taxon taxon = Taxon.NewInstance(name, sec);
            Classification classification = getClassification(state);
            familyNode = classification.addChildTaxon(taxon, sec, null);
        }

        return familyNode;
    }


    /**
     * @param state
     * @return
     */
    private Classification getClassification(CubaImportState state) {
        Classification classification = state.getClassification();
        if (classification == null){
            String name = state.getConfig().getClassificationName();
            //TODO
            Reference<?> sec = null;
            Language language = Language.DEFAULT();
            classification = Classification.NewInstance(name, sec, language);
            state.setClassification(classification);
            getClassificationService().save(classification);
        }
        return classification;
    }


    /**
     * @param record
     * @param originalKey
     * @return
     */
    private String getValue(HashMap<String, String> record, String originalKey) {
        String value = record.get(originalKey);
        if (! StringUtils.isBlank(value)) {
        	if (logger.isDebugEnabled()) { logger.debug(originalKey + ": " + value); }
        	value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
        	return value;
        }else{
        	return null;
        }
    }


	private Feature redBookCategory;
	private Feature endemism;



	/**
	 *  Stores taxa records in DB
	 */
	@Override
    protected void firstPass(CubaImportState state) {

		CyprusRow taxonLight = null; //state.getCyprusRow();
		Reference<?> citation = null;
		String microCitation = null;

//		//species name
//		String speciesStr = taxonLight.getSpecies();
//		String subSpeciesStr = taxonLight.getSubspecies();
//		String homotypicSynonymsString = taxonLight.getHomotypicSynonyms();
//		List<String> homotypicSynonymList = Arrays.asList(homotypicSynonymsString.split(";"));
//		String heterotypicSynonymsString = taxonLight.getHeterotypicSynonyms();
//		List<String> heterotypicSynonymList = Arrays.asList(heterotypicSynonymsString.split(";"));
//
//		String systematicsString = taxonLight.getSystematics();
//		String endemismString = taxonLight.getEndemism();
//		String statusString = taxonLight.getStatus();
//		String redBookCategory = taxonLight.getRedDataBookCategory();
//
//		if (StringUtils.isNotBlank(speciesStr)) {
//			boolean speciesIsExisting = false;
//			Taxon mainTaxon = null;
//			//species
//			Taxon speciesTaxon = (Taxon)createTaxon(state, Rank.SPECIES(), speciesStr, Taxon.class, nc);
//			mainTaxon = speciesTaxon;
//
//			//subspecies
//			if (StringUtils.isNotBlank(subSpeciesStr)){
//				Taxon existingSpecies = state.getHigherTaxon(speciesStr);
//				if (existingSpecies != null){
//					speciesIsExisting = true;
//					speciesTaxon = existingSpecies;
//				}
//
//				Taxon subSpeciesTaxon = (Taxon)createTaxon(state, Rank.SUBSPECIES(), subSpeciesStr, Taxon.class, nc);
//
//				if (subSpeciesTaxon != null){
//					makeParent(state, speciesTaxon, subSpeciesTaxon, citation, microCitation);
//				}
//				mainTaxon = subSpeciesTaxon;
//				state.putHigherTaxon(speciesStr, speciesTaxon);
//			}
//
//			if (! speciesIsExisting){
//				makeHigherTaxa(state, taxonLight, speciesTaxon, citation, microCitation);
//			}
//			makeHomotypicSynonyms(state, citation, microCitation, homotypicSynonymList, mainTaxon);
//			makeHeterotypicSynonyms(state, citation, microCitation, heterotypicSynonymList, mainTaxon);
//			makeSystematics(systematicsString, mainTaxon);
//			makeEndemism(endemismString, mainTaxon);
//			makeStatus(statusString, mainTaxon);
//			makeRedBookCategory(redBookCategory, mainTaxon);
//
////			state.putHigherTaxon(higherName, uuid);//(speciesStr, mainTaxon);
//			getTaxonService().save(mainTaxon);
//		}
		return;
    }


	private void makeHigherTaxa(CubaImportState state, CyprusRow taxonLight, Taxon speciesTaxon, Reference citation, String microCitation) {
		String divisionStr = taxonLight.getDivision();
		String genusStr = taxonLight.getGenus();
		String familyStr = taxonLight.getFamily();

		Taxon division = getTaxon(state, divisionStr, Rank.DIVISION(), null, citation, microCitation);
		Taxon family = getTaxon(state, familyStr, Rank.FAMILY(), division, citation, microCitation);
		Taxon genus = getTaxon(state, genusStr, Rank.GENUS(), family, citation, microCitation);
		makeParent(state, genus, speciesTaxon, citation, microCitation)	;
	}


	private Taxon getTaxon(CubaImportState state, String taxonNameStr, Rank rank, Taxon parent, Reference citation, String microCitation) {
		Taxon result;
		if (state.containsHigherTaxon(taxonNameStr)){
			result = state.getHigherTaxon(taxonNameStr);
		}else{
			result = (Taxon)createTaxon(state, rank, taxonNameStr, Taxon.class, nc);
			state.putHigherTaxon(taxonNameStr, result);
			if (parent == null){
				makeParent(state, null,result, citation, microCitation);
			}else{
				makeParent(state, parent, result, citation, microCitation);
			}

		}
		return result;
	}


	private void makeHomotypicSynonyms(CubaImportState state,
			Reference citation, String microCitation, List<String> homotypicSynonymList, Taxon mainTaxon) {
		for (String homotypicSynonym: homotypicSynonymList){
			if (StringUtils.isNotBlank(homotypicSynonym)){
				Synonym synonym = (Synonym)createTaxon(state, null, homotypicSynonym, Synonym.class, nc);
				mainTaxon.addHomotypicSynonym(synonym, citation, microCitation);
			}
		}
	}


	private void makeHeterotypicSynonyms(CubaImportState state, Reference citation, String microCitation, List<String> heterotypicSynonymList, Taxon mainTaxon) {
		for (String heterotypicSynonym: heterotypicSynonymList){
			if (StringUtils.isNotBlank(heterotypicSynonym)){
				Synonym synonym = (Synonym)createTaxon(state, null, heterotypicSynonym, Synonym.class, nc);
				mainTaxon.addSynonym(synonym, SynonymRelationshipType.HETEROTYPIC_SYNONYM_OF(), citation, microCitation);
			}
		}
	}


	private void makeSystematics(String systematicsString, Taxon mainTaxon) {
		//Systematics
		if (StringUtils.isNotBlank(systematicsString)){
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			TextData textData = TextData.NewInstance(Feature.SYSTEMATICS());
			textData.putText(Language.UNDETERMINED(), systematicsString);
			td.addElement(textData);
		}
	}


	private void makeEndemism(String endemismString, Taxon mainTaxon) {
		//endemism
		if (StringUtils.isNotBlank(endemismString)){
			//OLD - not wanted as marker
//			boolean flag;
//			if (endemismString.trim().equalsIgnoreCase("not endemic") || endemismString.trim().equalsIgnoreCase("ne?")){
//				flag = false;
//			}else if (endemismString.trim().equalsIgnoreCase("endemic")){
//				flag = true;
//			}else{
//				throw new RuntimeException(endemismString + " is not a valid value for endemism");
//			}
//			Marker marker = Marker.NewInstance(MarkerType.ENDEMIC(), flag);
//			mainTaxon.addMarker(marker);
			//text data
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			TextData textData = TextData.NewInstance(endemism);
			textData.putText(Language.ENGLISH(), endemismString);
			td.addElement(textData);
		}
	}


	private void makeRedBookCategory(String redBookCategory, Taxon mainTaxon) {
		//red data book category
		if (StringUtils.isNotBlank(redBookCategory)){
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			TextData textData = TextData.NewInstance(this.redBookCategory);
			textData.putText(Language.ENGLISH(), redBookCategory);
			td.addElement(textData);
		}
	}




	/**
	 *  Stores parent-child, synonym and common name relationships
	 */
	@Override
    protected void secondPass(CubaImportState state) {
//		CyprusRow cyprusRow = state.getCyprusRow();
		return;
	}



	/**
	 * @param state
	 * @param rank
	 * @param taxonNameStr
	 * @param authorStr
	 * @param nameStatus
	 * @param nc
	 * @return
	 */
	private TaxonBase createTaxon(CubaImportState state, Rank rank, String taxonNameStr,
			Class statusClass, NomenclaturalCode nc) {
		TaxonBase taxonBase;
		NonViralName taxonNameBase = null;
		if (nc == NomenclaturalCode.ICVCN){
			logger.warn("ICVCN not yet supported");

		}else{
			taxonNameBase =(NonViralName) nc.getNewTaxonNameInstance(rank);
			//NonViralName nonViralName = (NonViralName)taxonNameBase;
			INonViralNameParser parser = nameParser;//NonViralNameParserImpl.NewInstance();
			taxonNameBase = (NonViralName<BotanicalName>)parser.parseFullName(taxonNameStr, nc, rank);

			//taxonNameBase.setNameCache(taxonNameStr);

		}

		//Create the taxon
		Reference sec = state.getConfig().getSourceReference();
		// Create the status
		if (statusClass.equals(Taxon.class)){
			taxonBase = Taxon.NewInstance(taxonNameBase, sec);
		}else if (statusClass.equals(Synonym.class)){
			taxonBase = Synonym.NewInstance(taxonNameBase, sec);
		}else {
			Taxon taxon = Taxon.NewInstance(taxonNameBase, sec);
			taxon.setTaxonStatusUnknown(true);
			taxonBase = taxon;
		}
		return taxonBase;
	}

	private boolean makeParent(CubaImportState state, Taxon parentTaxon, Taxon childTaxon, Reference citation, String microCitation){
		boolean success = true;
		Reference sec = state.getConfig().getSourceReference();

//		Reference sec = parentTaxon.getSec();
		Classification tree = state.getTree(sec);
		if (tree == null){
			tree = makeTree(state, sec);
			tree.setTitleCache(state.getConfig().getSourceReferenceTitle(), true);
		}
		if (sec.equals(childTaxon.getSec())){
			success &=  (null !=  tree.addParentChild(parentTaxon, childTaxon, citation, microCitation));
		}else{
			logger.warn("No relationship added for child " + childTaxon.getTitleCache());
		}
		return success;
	}


    @Override
    protected boolean isIgnore(CubaImportState state) {
        return ! state.getConfig().isDoTaxa();
    }

    @Override
    protected boolean doCheck(CubaImportState state) {
        logger.warn("DoCheck not yet implemented for CubaExcelImport");
        return true;
    }

}
