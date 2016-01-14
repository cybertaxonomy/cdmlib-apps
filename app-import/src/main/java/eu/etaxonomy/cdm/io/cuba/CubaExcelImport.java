/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.cuba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.excel.common.ExcelImporterBase;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationship;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
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

    private static final String HOMONYM_MARKER = ".*\\s+homon.?$";
    private static final String DOUBTFUL_MARKER = "^\\?\\s?";


    private static UUID rootUuid = UUID.fromString("206d42e4-ac32-4f20-a093-14826014e667");
    private static UUID plantaeUuid = UUID.fromString("139e7314-dd19-4286-a01d-8cc94ef77a09");

    private static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();
    private static NomenclaturalCode nc = NomenclaturalCode.ICNAFP;

    private  static List<String> expectedKeys= Arrays.asList(new String[]{"Fam.","(Fam.)","Taxón","(Notas)","Syn.","End","Ind","Ind? D","Nat","Dud P","Adv","Cult C","CuW","PR PR*","Art","Hab(*)","May","Mat","IJ","CuC","VC","Ci","SS","CA","Cam","LT","CuE","Gr","Ho","SC","Gu","Esp","Ja","PR","Men","Bah","Cay","AmN","AmC","AmS","VM"});

	@Override
    protected void analyzeRecord(HashMap<String, String> record, CubaImportState state) {
	    //we do everything in firstPass here
    	return;
    }


    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeCubanDistribution(HashMap<String, String> record, CubaImportState state) {
        try {
            NamedArea cuba = getNamedArea(state, state.getTransformer().getNamedAreaUuid("C"), null, null, null, null, null);
            TaxonDescription desc = getTaxonDescription(state.getCurrentTaxon(), false, true);
            List<PresenceAbsenceTerm> statuss =  makeCubanStatus(record, state);
            for (PresenceAbsenceTerm status : statuss){
                Distribution distribution = Distribution.NewInstance(cuba, status);
                desc.addElement(distribution);
            }
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
    private List<PresenceAbsenceTerm> makeCubanStatus(HashMap<String, String> record, CubaImportState state) throws UndefinedTransformerMethodException {
        boolean isAbsent = false;  //TODO

        String line = state.getCurrentLine() + ": ";
        List<PresenceAbsenceTerm> result = new ArrayList<>();

        String endemicStr = getValue(record, "End");
        String indigenousStr = getValue(record, "Ind");
        String indigenousDoubtStr = getValue(record, "Ind? D");
        String naturalisedStr = getValue(record, "Nat");
        String dudStr = getValue(record, "Dud P");
        String advStr = getValue(record, "Adv");
        String cultStr = getValue(record, "Cult C");

        if (endemicStr != null){
            if(endemicStr.equals("+")){
                PresenceAbsenceTerm endemicState = state.getTransformer().getPresenceTermByKey("E");
                result.add(endemicState);
            }else if(isMinus(endemicStr)){
                UUID endemicUuid = state.getTransformer().getPresenceTermUuid("-E");
                PresenceAbsenceTerm endemicState = getPresenceTerm(state, endemicUuid, null, null, null, false);
                result.add(endemicState);
            }else{
                logger.warn(line + "Endemic not recognized: " + endemicStr);
            }
        }
        if (indigenousStr != null){
            if(indigenousStr.equals("+")){
                UUID indigenousUuid = state.getTransformer().getPresenceTermUuid("Ind.");
                PresenceAbsenceTerm indigenousState = getPresenceTerm(state, indigenousUuid, null, null, null, false);
                result.add(indigenousState);
            }else if(isMinus(indigenousStr)){
                PresenceAbsenceTerm haturalizedState = state.getTransformer().getPresenceTermByKey("-Ind.");
                result.add(haturalizedState);
            }else if(indigenousStr.equals("?")){
                UUID indigenousDoubtUuid = state.getTransformer().getPresenceTermUuid("?Ind.");
                PresenceAbsenceTerm indigenousDoubtState = getPresenceTerm(state, indigenousDoubtUuid, null, null, null, false);
                result.add(indigenousDoubtState);
            }else{
                logger.warn(line + "Indigenous not recognized: " + indigenousStr);
            }
        }
        if(indigenousDoubtStr != null){
            if(indigenousDoubtStr.equals("D")){
                UUID indigenousDoubtUuid = state.getTransformer().getPresenceTermUuid("Ind.?");
                PresenceAbsenceTerm indigenousDoubtState = getPresenceTerm(state, indigenousDoubtUuid, null, null, null, false);
                result.add(indigenousDoubtState);
            }else{
                logger.warn(line + "Indigenous doubtful not recognized: " + indigenousDoubtStr);
            }
        }
        if(naturalisedStr != null){
            if(naturalisedStr.equals("N")){
                  PresenceAbsenceTerm haturalizedState = state.getTransformer().getPresenceTermByKey("Nat.");
                  result.add(haturalizedState);
            }else if(isMinus(naturalisedStr)){
                UUID naturalisedErrorUuid = state.getTransformer().getPresenceTermUuid("-Nat.");
                PresenceAbsenceTerm naturalisedErrorState = getPresenceTerm(state, naturalisedErrorUuid, null, null, null, false);
                result.add(naturalisedErrorState);
            }else if(naturalisedStr.equals("?")){
                UUID naturalisedDoubtUuid = state.getTransformer().getPresenceTermUuid("?Nat.");
                PresenceAbsenceTerm naturalisedDoubtState = getPresenceTerm(state, naturalisedDoubtUuid, null, null, null, false);
                result.add(naturalisedDoubtState);
            }else{
                logger.warn(line + "Naturalized not recognized: " + naturalisedStr);
            }
        }
        if(dudStr != null){
            if(dudStr.equals("P")){
                UUID dudUuid = state.getTransformer().getPresenceTermUuid("Dud.");
                PresenceAbsenceTerm dudState = getPresenceTerm(state, dudUuid, null, null, null, false);
                result.add(dudState);
            }else if(isMinus(dudStr)){
                UUID nonNativeErrorUuid = state.getTransformer().getPresenceTermUuid("-Dud.");
                PresenceAbsenceTerm nonNativeErrorState = getPresenceTerm(state, nonNativeErrorUuid, null, null, null, false);
                result.add(nonNativeErrorState);
            }else if(dudStr.equals("?")){
                UUID naturalisedDoubtUuid = state.getTransformer().getPresenceTermUuid("?Dud.");
                PresenceAbsenceTerm naturalisedDoubtState = getPresenceTerm(state, naturalisedDoubtUuid, null, null, null, false);
                result.add(naturalisedDoubtState);
            }else{
                logger.warn(line + "non-native and doubtfully naturalised not recognized: " + dudStr);
            }
        }
        if(advStr != null){
            if(advStr.equals("A")){
                UUID advUuid = state.getTransformer().getPresenceTermUuid("Adv.");
                PresenceAbsenceTerm advState = getPresenceTerm(state, advUuid, null, null, null, false);
                result.add(advState);
            }else if(isMinus(advStr)){
                UUID advUuid = state.getTransformer().getPresenceTermUuid("-Adv.");
                PresenceAbsenceTerm advState = getPresenceTerm(state, advUuid, null, null, null, false);
                result.add(advState);
            }else{
                logger.warn(line + "'adventive (casual) alien' not recognized: " + advStr);
            }
        }else if(cultStr != null){
            if (! (cultStr.matches("(C|\\(C\\)|\\?|–)"))){
                logger.warn("'cultivated' not recognized: " + cultStr);
            }else if(cultStr.equals("C")){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("Cult.");
                result.add(cultivatedState);
            }else if(cultStr.equals("?")){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("?Cult.");
                result.add(cultivatedState);
            }else if(cultStr.equals("(C)")){
                UUID ocassualCultUuid = state.getTransformer().getPresenceTermUuid("(C)");
                PresenceAbsenceTerm cultivatedState = getPresenceTerm(state, ocassualCultUuid, null, null, null, false);
                result.add(cultivatedState);
            }else if(isMinus(cultStr)){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("-Cult.");
                result.add(cultivatedState);
            }else{
                logger.warn(line + "'cultivated' not recognized: " + cultStr);
            }
        }

        return result;
    }


    /**
     * @param indigenousStr
     * @return
     */
    private boolean isMinus(String str) {
        return str.equals("-") || str.equals("–");
    }


    /**
     * @param indigenousStr
     * @return
     */
    private boolean checkPlusMinusDoubt(String str) {
        return str.equals("+") || isMinus(str)|| str.equals("?");
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


    private static final String acceptedRegExStr = "\\(([^\\[\\]“”]{6,})\\)";
//    String heterotypicRegExStr2 = "([^\\(]{5,}" +"(\\(.+\\))?" + "[^\\)\\(]{2,})" +
//                    + "(\\((.{6,})\\))?";
    private static final String heterotypicRegExStr = "([^\\(\\[\\]“”]{5,})"
                                                     +"(\\((.{6,})\\))?";
    private static final String heterotypicRegExStr_TEST = "([^\\(]{5,}" +"(\\(.+\\))?" + "[^\\)\\(]{2,})"
            +"(\\((.{6,})\\))?";
    private static final String auctRegExStr = "auct\\."
            +"((\\sFC(\\-S)?(\\s&\\sA&S)?)|(\\sA&S))?(\\s+p\\.\\s*p\\.)?";
    private static final String missapliedRegExStr = "“(.*{5,})”\\s+(" + auctRegExStr + "|sensu\\s+.{2,})";
    private static final String nomInvalRegExStr = "“(.*{5,})”\\s+nom\\.\\s+inval\\.";
    private static final String homonymRegExStr = "\\s*(\\[.*\\])*\\s*";

    private static final Pattern acceptedRegEx = Pattern.compile(acceptedRegExStr + homonymRegExStr);
    private static final Pattern heterotypicRegEx = Pattern.compile(heterotypicRegExStr + homonymRegExStr);
    private static final Pattern missapliedRegEx = Pattern.compile(missapliedRegExStr);
    private static final Pattern nomInvalRegEx = Pattern.compile(nomInvalRegExStr);

    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeSynonyms(HashMap<String, String> record, CubaImportState state) {
//        boolean forAccepted = true;
        String synonymStr = record.get("Syn.");
        String line = state.getCurrentLine() + ": ";

        if (synonymStr == null){
            //TODO test that this is not a synonym only line
            return;
        }
        synonymStr = synonymStr.trim();

//        String heterotypicRegExStr = "([^\\(]{5,}(\\(.+\\))?[^\\)\\(]{2,})(\\((.{6,})\\))?";
//        String heterotypicRegExStr = "([^\\(]{5,})(\\((.{6,})\\))?";

//        Pattern heterotypicRegEx = Pattern.compile(heterotypicRegExStr + homonymRegExStr);

        Matcher missapliedMatcher = missapliedRegEx.matcher(synonymStr);
        Matcher nomInvalMatcher = nomInvalRegEx.matcher(synonymStr);
        Matcher acceptedMatcher = acceptedRegEx.matcher(synonymStr);
        Matcher heterotypicMatcher = heterotypicRegEx.matcher(synonymStr);

        List<BotanicalName> homonyms = new ArrayList<>();
        if (missapliedMatcher.matches()){
            String firstPart = missapliedMatcher.group(1);
            BotanicalName name = (BotanicalName)nameParser.parseSimpleName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());

            String secondPart = missapliedMatcher.group(2);
            Taxon misappliedNameTaxon = Taxon.NewInstance(name, null);
            if (secondPart.startsWith("sensu")){
                secondPart = secondPart.substring(5).trim();
                if (secondPart.contains(" ")){
                    logger.warn(line + "Second part contains more than 1 word. Check if this is correct: " + secondPart);
                }
                Reference<?> sensu = ReferenceFactory.newGeneric();
                Team team = Team.NewTitledInstance(secondPart, null);
                sensu.setAuthorship(team);
                misappliedNameTaxon.setSec(sensu);
            }else if (secondPart.matches(auctRegExStr)){
                secondPart = secondPart.replace("p. p.", "p.p.");
                misappliedNameTaxon.setAppendedPhrase(secondPart);
            }else{
                logger.warn(line + "Misapplied second part not recognized: " + secondPart);
            }
            //TODO
            Reference<?> relRef = null;
            state.getCurrentTaxon().addMisappliedName(misappliedNameTaxon, relRef, null);
        }else if (nomInvalMatcher.matches()){
            String firstPart = nomInvalMatcher.group(1);
            BotanicalName name = (BotanicalName)nameParser.parseSimpleName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            NomenclaturalStatus status = NomenclaturalStatus.NewInstance( NomenclaturalStatusType.INVALID());
            name.addStatus(status);
            state.getCurrentTaxon().addSynonymName(name, SynonymRelationshipType.SYNONYM_OF());
        }else if (acceptedMatcher.matches()){
            String firstPart = acceptedMatcher.group(1);
            String homonymPart = acceptedMatcher.groupCount() < 2 ? null : acceptedMatcher.group(2);
            handleHomotypicGroup(firstPart, state, (BotanicalName)state.getCurrentTaxon().getName(), false, homonyms, homonymPart, false);
        }else if(heterotypicMatcher.matches()){
            String firstPart = heterotypicMatcher.group(1).trim();
            String secondPart = heterotypicMatcher.groupCount() < 3 ? null : heterotypicMatcher.group(3);
            String homonymPart = heterotypicMatcher.groupCount() < 4 ? null : heterotypicMatcher.group(4);
            boolean isDoubtful = firstPart.matches("^\\?\\s*.*");
            boolean isHomonym = firstPart.trim().matches(HOMONYM_MARKER);
            firstPart = normalizeStatus(firstPart);
            BotanicalName synName = (BotanicalName)nameParser.parseReferencedName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            if (synName.isProtectedTitleCache()){
                logger.warn(line + "heterotypic base synonym could not be parsed correctly:" + firstPart);
            }
            if (isHomonym){
                homonyms.add(synName);
            }
            SynonymRelationship sr = state.getCurrentTaxon().addHeterotypicSynonymName(synName);
            sr.getSynonym().setDoubtful(isDoubtful);
            handleHomotypicGroup(secondPart, state, synName, true, homonyms, homonymPart, isDoubtful);
        }else{
            logger.warn(line + "Synonym entry does not match: " + synonymStr);
        }
    }



    /**
     * @param synonymStr
     * @param state
     * @param homonyms
     * @param homonymPart
     * @param isDoubtful
     * @param taxon
     * @param homotypicalGroup
     */
    private void handleHomotypicGroup(String homotypicStr,
            CubaImportState state,
            BotanicalName homotypicName,
            boolean isHeterotypic,
            List<BotanicalName> homonyms,
            String homonymPart,
            boolean isDoubtful) {

        if (homotypicStr == null){
            return;
        }else if (homotypicStr.startsWith("(") && homotypicStr.endsWith("")){
            homotypicStr = homotypicStr.substring(1, homotypicStr.length() - 1);
        }

        BotanicalName currentBasionym = homotypicName;
        String[] splits = homotypicStr.split("\\s*,\\s*");
        for (String split : splits){
            boolean isHomonym = split.trim().matches(HOMONYM_MARKER);
            String singleName = normalizeStatus(split);
            BotanicalName newName = (BotanicalName)nameParser.parseReferencedName(singleName, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            if (newName.isProtectedTitleCache()){
                logger.warn(state.getCurrentLine() + ": homotypic name part could not be parsed: " + split);
            }
            if (isHomonym){
                homonyms.add(newName);
            }
            if (isHeterotypic){
                SynonymRelationship sr = state.getCurrentTaxon().addHeterotypicSynonymName(newName, homotypicName.getHomotypicalGroup(), null, null);
                sr.getSynonym().setDoubtful(isDoubtful);
//                newName.addBasionym(homotypicName);
                currentBasionym = handleBasionym(currentBasionym, newName);
            }else{
                state.getCurrentTaxon().addHomotypicSynonymName(newName, null, null);
                handleBasionym(currentBasionym, newName);
            }
        }
        makeHomonyms(homonyms, homonymPart, state);
    }


    /**
     * @param homonyms
     * @param homonymPart
     * @param state
     */
    private void makeHomonyms(List<BotanicalName> homonyms, String homonymPart, CubaImportState state) {
        String line = state.getCurrentLine() + ": ";
        homonymPart = homonymPart == null ? "" : homonymPart.trim();
        if (homonyms.isEmpty() && homonymPart.equals("")){
            return;
        }else if (homonymPart.equals("")){
            logger.warn(line + "SynonymPart has homonyms but homonymPart is empty");
            return;
        }
        homonymPart = homonymPart.substring(1, homonymPart.length() - 1);
        String[] splits = homonymPart.split("\\]\\s*\\[");
        if (splits.length != homonyms.size()){
            logger.warn(line + "Number of homonyms (" + homonyms.size() + ") and homonymParts ("+splits.length+") does not match");
            return;
        }
        int i = 0;
        for (String split : splits){
            split = split.replaceAll("^non\\s+", "");
            BotanicalName newName = (BotanicalName)nameParser.parseReferencedName(split, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            if (newName.isProtectedTitleCache()){
                logger.warn(state.getCurrentLine() + ": homonym name could not be parsed: " + split);
            }
            newName.addRelationshipToName(homonyms.get(i), NameRelationshipType.LATER_HOMONYM(), null);
            i++;
        }
    }


    /**
     * @param newName
     * @param homotypicName
     * @return
     */
    private BotanicalName handleBasionym(BotanicalName currentBasionym, BotanicalName name2) {
        BotanicalName basionymName = currentBasionym;
        BotanicalName newCombination = name2;
        //switch if necessary
        if (basionymName.getBasionymAuthorship() != null && newCombination.getBasionymAuthorship() == null){
            basionymName = name2;
            newCombination = currentBasionym;
        }
        if (matchAuthor(basionymName.getCombinationAuthorship(), newCombination.getBasionymAuthorship())){
            newCombination.getHomotypicalGroup().setGroupBasionym(basionymName);
        }
        return basionymName;
    }


    /**
     * @param combinationAuthorship
     * @param basi
     * @return
     */
    private boolean matchAuthor(TeamOrPersonBase<?> author1, TeamOrPersonBase<?> author2) {
        if (author1 == null || author2 == null){
            return false;
        }else {
            return author1.getNomenclaturalTitle().equals(author2.getNomenclaturalTitle());
        }
    }


    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeNotes(HashMap<String, String> record, CubaImportState state) {
        String notesStr = getValue(record, "(Notas)");
        if (notesStr == null){
            return;
        }else{
            Annotation annotation = Annotation.NewDefaultLanguageInstance(notesStr);
            //TODO
            annotation.setAnnotationType(AnnotationType.EDITORIAL());
            state.getCurrentTaxon().addAnnotation(annotation);
        }
    }


    /**
     * @param record
     * @param state
     * @param familyTaxon
     * @return
     */
    private Taxon makeTaxon(HashMap<String, String> record, CubaImportState state, TaxonNode familyNode, boolean isSynonym) {
        String taxonStr = getValue(record, "Taxón");
        if (taxonStr == null){
            return isSynonym ? state.getCurrentTaxon() : null;
        }
        boolean isAbsent = false;
        if (taxonStr.startsWith("[") && taxonStr.endsWith("]")){
            taxonStr = taxonStr.substring(1, taxonStr.length() - 1);
            isAbsent = true;
        }
        taxonStr = normalizeStatus(taxonStr);

        BotanicalName botanicalName = (BotanicalName)nameParser.parseReferencedName(taxonStr, nc, Rank.SPECIES());
        Reference<?> sec = getSecReference(state);
        Taxon taxon = Taxon.NewInstance(botanicalName, sec);
        TaxonNode higherNode;
        if (botanicalName.isProtectedTitleCache()){
            logger.warn(state.getCurrentLine() + ": Taxon could not be parsed: " + taxonStr);
            higherNode = familyNode;
        }else{
            String genusStr = botanicalName.getGenusOrUninomial();
            Taxon genus = state.getHigherTaxon(genusStr);
            if (genus != null){
                higherNode = genus.getTaxonNodes().iterator().next();
            }else{
                BotanicalName name = BotanicalName.NewInstance(Rank.GENUS());
                name.setGenusOrUninomial(genusStr);
                genus = Taxon.NewInstance(name, sec);
                higherNode = familyNode.addChildTaxon(genus, null, null);
                state.putHigherTaxon(genusStr, genus);
            }
        }

        higherNode.addChildTaxon(taxon, null, null);

        return taxon;
    }

    /**
     * @param state
     * @return
     */
    private Reference<?> getSecReference(CubaImportState state) {
        Reference<?> result = state.getSecReference();
        if (result == null){
            result = ReferenceFactory.newDatabase();
            result.setTitle("Flora of Cuba");
            state.setSecReference(result);
        }
        return result;
    }


    private static final String[] nomStatusStrings = new String[]{"nom. cons.", "ined.", "nom. illeg.",
            "nom. rej.","nom. cons. prop.","nom. altern."};
    /**
     * @param taxonStr
     * @return
     */
    private String normalizeStatus(String taxonStr) {
        if (taxonStr == null){
            return null;
        }
        for (String nomStatusStr : nomStatusStrings){
            nomStatusStr = " " + nomStatusStr;
            if (taxonStr.endsWith(nomStatusStr)){
                taxonStr = taxonStr.replace(nomStatusStr, "," + nomStatusStr);
            }
        }
        taxonStr = taxonStr.replaceAll(HOMONYM_MARKER, "").trim();
        taxonStr = taxonStr.replaceAll(DOUBTFUL_MARKER, "").trim();
        return taxonStr;


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
            Reference<?> sec = getSecReference(state);
            Taxon taxon = Taxon.NewInstance(name, sec);
            ITaxonTreeNode rootNode = getClassification(state);
            familyNode = rootNode.addChildTaxon(taxon, sec, null);
            state.putHigherTaxon(familyStr, taxon);
        }

        return familyNode;
    }


    /**
     * @param state
     * @return
     */
    private TaxonNode getClassification(CubaImportState state) {
        Classification classification = state.getClassification();
        if (classification == null){
            classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        }
        TaxonNode rootNode = state.getRootNode();
        if (rootNode == null){
            rootNode = getTaxonNodeService().find(plantaeUuid);
        }
        if (rootNode == null){
            Reference<?> sec = getSecReference(state);
            if (classification == null){
                String classificationName = state.getConfig().getClassificationName();
                //TODO
                Language language = Language.DEFAULT();
                classification = Classification.NewInstance(classificationName, sec, language);
                state.setClassification(classification);
                classification.setUuid(state.getConfig().getClassificationUuid());
                classification.getRootNode().setUuid(rootUuid);
            }

            BotanicalName plantaeName = BotanicalName.NewInstance(Rank.KINGDOM());
            plantaeName.setGenusOrUninomial("Plantae");
            Taxon plantae = Taxon.NewInstance(plantaeName, sec);
            TaxonNode plantaeNode = classification.addChildTaxon(plantae, null, null);
            plantaeNode.setUuid(plantaeUuid);
            state.setRootNode(plantaeNode);
            getClassificationService().save(classification);

            rootNode = plantaeNode;
        }
        return rootNode;
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



	/**
	 *  Stores taxa records in DB
	 */
	@Override
    protected void firstPass(CubaImportState state) {
	    boolean isSynonym = false;

        int line = state.getCurrentLine();
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn("Unexpected Key: " + key);
            }
        }

        if (record.get("Fam.") == null && keys.size() == 2 && record.get("Syn.") == null && record.get("Nat") != null && record.get("Adv") != null){
            //second header line, don't handle
            return;
        }

        //Fam.
        TaxonNode familyTaxon = getFamilyTaxon(record, state);
        if (familyTaxon == null){
            if (record.get("Taxón") != null){
                logger.warn(line + ": Family not recognized but taxon exists: " + record.get("Taxón"));
                return;
            }else if (record.get("Syn.") == null){
                logger.warn(line + ": Family not recognized but also no synonym exists");
                return;
            }else{
                isSynonym = true;
            }
        }

        //(Fam.)
        //TODO

        //Taxón
        Taxon taxon = makeTaxon(record, state, familyTaxon, isSynonym);
        if (taxon == null && ! isSynonym){
            logger.warn(line + ": taxon could not be created and is null");
            return;
        }
        state.setCurrentTaxon(taxon);

        //(Notas)
        makeNotes(record, state);

        //Syn.
        makeSynonyms(record, state);

        //End, Ind, Ind? D, Nat N, Dud P, Adv A, Cult C
        makeCubanDistribution(record, state);


        // "CuW","PR PR*","Art","Hab(*)","May","Mat","IJ",
//        "CuC","VC","Ci","SS","CA","Cam","LT",
//        "CuE","Gr","Ho","SC","Gu",
//      "Esp","Ja","PR","Men","Bah","Cay",
//      "AmN","AmC","AmS","VM"});
        makeProvincesDistribution(record, state);

		return;
    }



	/**
     * @param record
     * @param state
     * @param taxon
     */
    // "CuW","PR PR*","Art","Hab(*)","May","Mat","IJ",
//  "CuC","VC","Ci","SS","CA","Cam","LT",
//  "CuE","Gr","Ho","SC","Gu",
    private void makeProvincesDistribution(HashMap<String, String> record, CubaImportState state) {
        List<String> areaKeys = Arrays.asList(new String[]{
                "CuW","PR PR*","Art","Hab(*)","May","Mat","IJ",
                "CuC","VC","Ci","SS","CA","Cam","LT",
                "CuE","Gr","Ho","SC","Gu",
                "Esp","Ja","PR","Men","Bah","Cay",
                "AmN","AmC","AmS","VM"});
        for (String areaKey : areaKeys){
            makeSingleProvinceDistribution(areaKey, record, state);
        }

    }


    /**
     * @param areaKey
     * @param record
     * @param state
     * @param taxon
     */
    private void makeSingleProvinceDistribution(String areaKey,
            HashMap<String, String> record,
            CubaImportState state) {
        try {
            UUID areaUuid = state.getTransformer().getNamedAreaUuid(areaKey);
            if (areaUuid == null){
                logger.warn("Area not recognized: " + areaKey);
                return;
            }
            if (record.get(areaKey)==null){
                return; //no status defined
            }

            NamedArea area = getNamedArea(state, areaUuid, null, null, null, null, null);
            if (area == null){
                logger.warn(state.getCurrentLine() + ": Area not recognized: " + area);
            }
            TaxonDescription desc = getTaxonDescription(state.getCurrentTaxon(), false, true);
            PresenceAbsenceTerm status =  makeProvinceStatus(areaKey, record, state);
            if (status == null){
                logger.warn(state.getCurrentLine() + ": Distribution Status could not be defined: " + record.get(areaKey));
            }
            Distribution distribution = Distribution.NewInstance(area, status);
            desc.addElement(distribution);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }

    }


    /**
     * @param areaKey
     * @param record
     * @param state
     * @return
     * @throws UndefinedTransformerMethodException
     */
    private PresenceAbsenceTerm makeProvinceStatus(String areaKey, HashMap<String, String> record, CubaImportState state) throws UndefinedTransformerMethodException {
        String statusStr = record.get(areaKey);
        if (statusStr == null){
            return null;
        }
        PresenceAbsenceTerm status = state.getTransformer().getPresenceTermByKey(statusStr);
        if (status == null){
            UUID statusUuid = state.getTransformer().getPresenceTermUuid(statusStr);
            status = getPresenceTerm(state, statusUuid, null, null, null, false);
        }
        return status;
    }


    /**
	 *  Stores parent-child, synonym and common name relationships
	 */
	@Override
    protected void secondPass(CubaImportState state) {
//		CyprusRow cyprusRow = state.getCyprusRow();
		return;
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
