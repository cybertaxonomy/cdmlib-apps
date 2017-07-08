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
import java.util.HashSet;
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
import eu.etaxonomy.cdm.io.excel.common.ExcelImportBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TaxonInteraction;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.HomotypicalGroup;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.homotypicgroup.BasionymRelationCreator;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component
public class CubaExcelImport
        extends ExcelImportBase<CubaImportState, CubaImportConfigurator, ExcelRowBase> {
    private static final long serialVersionUID = -747486709409732371L;
    private static final Logger logger = Logger.getLogger(CubaExcelImport.class);

    private static final String HOMONYM_MARKER = "\\s+homon.?$";
    private static final String DOUBTFUL_MARKER = "^\\?\\s?";


    private static UUID rootUuid = UUID.fromString("206d42e4-ac32-4f20-a093-14826014e667");
    private static UUID plantaeUuid = UUID.fromString("139e7314-dd19-4286-a01d-8cc94ef77a09");

    private static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();
    private static NomenclaturalCode nc = NomenclaturalCode.ICNAFP;

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "Fam. default","Fam. FRC","Fam. A&S","Fam. FC",
            "Taxón","(Notas)","Syn.","End","Ind","Ind? D","Nat","Dud P","Adv","Cult C","CuW","PR PR*","Art","Hab(*)","May","Mat","IJ","CuC","VC","Ci","SS","CA","Cam","LT","CuE","Gr","Ho","SC","Gu","Esp","Ja","PR","Men","Bah","Cay","AmN","AmC","AmS","VM"});

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
            NamedArea cuba = getNamedArea(state, state.getTransformer().getNamedAreaUuid("Cu"), null, null, null, null, null);
            TaxonDescription desc = getTaxonDescription(state.getCurrentTaxon(), false, true);
            List<PresenceAbsenceTerm> statuss =  makeCubanStatuss(record, state);
            for (PresenceAbsenceTerm status : statuss){
                Distribution distribution = Distribution.NewInstance(cuba, status);
                desc.addElement(distribution);
                distribution.addSource(makeDescriptionSource(state));
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
    private List<PresenceAbsenceTerm> makeCubanStatuss(HashMap<String, String> record, CubaImportState state) throws UndefinedTransformerMethodException {
        PresenceAbsenceTerm highestStatus = null;

        String line = state.getCurrentLine() + ": ";
        List<PresenceAbsenceTerm> result = new ArrayList<>();

        String endemicStr = getValue(record, "End");
        String indigenousStr = getValue(record, "Ind");
        String indigenousDoubtStr = getValue(record, "Ind? D");
        String naturalisedStr = getValue(record, "Nat");
        String dudStr = getValue(record, "Dud P");
        String advStr = getValue(record, "Adv");
        String cultStr = getValue(record, "Cult C");

        state.setEndemic(false);

        if (endemicStr != null){
            if(endemicStr.equals("+")){
                PresenceAbsenceTerm endemicState = state.getTransformer().getPresenceTermByKey("E");
                result.add(endemicState);
                highestStatus = endemicState;
                state.setEndemic(true);
            }else if(isMinus(endemicStr)){
                UUID endemicUuid = state.getTransformer().getPresenceTermUuid("-E");
                PresenceAbsenceTerm endemicState = getPresenceTerm(state, endemicUuid, null, null, null, false);
                result.add(endemicState);
                checkAbsentHighestState(highestStatus, line, "endemic", false);
            }else if(endemicStr.equals("?")){
                UUID endemicDoubtfulUuid = state.getTransformer().getPresenceTermUuid("?E");
                PresenceAbsenceTerm endemicState = getPresenceTerm(state, endemicDoubtfulUuid, null, null, null, false);
                result.add(endemicState);
                checkAbsentHighestState(highestStatus, line, "endemic", false);
            }else{
                logger.warn(line + "Endemic not recognized: " + endemicStr);
            }
        }
        if (indigenousStr != null){
            if(indigenousStr.equals("+")){
                PresenceAbsenceTerm indigenousState = state.getTransformer().getPresenceTermByKey("Ind.");
//                PresenceAbsenceTerm indigenousState = getPresenceTerm(state, indigenousUuid, null, null, null, false);
                result.add(indigenousState);
                highestStatus = highestStatus != null ? highestStatus : indigenousState;
            }else if(isMinus(indigenousStr)){
                PresenceAbsenceTerm indigenousState = state.getTransformer().getPresenceTermByKey("-Ind.");
                result.add(indigenousState);
                checkAbsentHighestState(highestStatus, line, "indigenous", false);
            }else if(indigenousStr.equals("?")){
                PresenceAbsenceTerm indigenousDoubtState = state.getTransformer().getPresenceTermByKey("?Ind.");
//                PresenceAbsenceTerm indigenousDoubtState = getPresenceTerm(state, indigenousDoubtUuid, null, null, null, false);
                result.add(indigenousDoubtState);
                checkAbsentHighestState(highestStatus, line, "indigenous", true);
            }else{
                logger.warn(line + "Indigenous not recognized: " + indigenousStr);
            }
        }
        if(indigenousDoubtStr != null){
            if(indigenousDoubtStr.equals("D")){
                PresenceAbsenceTerm doubtIndigenousState = state.getTransformer().getPresenceTermByKey("Ind.?");
//                PresenceAbsenceTerm doubtIndigenousState = getPresenceTerm(state, doubtIndigenousUuid, null, null, null, false);
                result.add(doubtIndigenousState);
                highestStatus = highestStatus != null ? highestStatus : doubtIndigenousState;
            }else if(isMinus(indigenousDoubtStr)){
                UUID doubtIndigenousErrorUuid = state.getTransformer().getPresenceTermUuid("-Ind.?");
                PresenceAbsenceTerm doubtIndigenousErrorState = getPresenceTerm(state, doubtIndigenousErrorUuid, null, null, null, false);
                result.add(doubtIndigenousErrorState);
                checkAbsentHighestState(highestStatus, line, "doubtfully indigenous", true);
            }else{
                logger.warn(line + "doubtfully indigenous not recognized: " + indigenousDoubtStr);
            }
        }
        if(naturalisedStr != null){
            if(naturalisedStr.equals("N")){
                PresenceAbsenceTerm haturalizedState = state.getTransformer().getPresenceTermByKey("Nat.");
                result.add(haturalizedState);
                highestStatus = highestStatus != null ? highestStatus : haturalizedState;
            }else if(isMinus(naturalisedStr)){
                UUID naturalisedErrorUuid = state.getTransformer().getPresenceTermUuid("-Nat.");
                PresenceAbsenceTerm naturalisedErrorState = getPresenceTerm(state, naturalisedErrorUuid, null, null, null, false);
                result.add(naturalisedErrorState);
                checkAbsentHighestState(highestStatus, line, "naturalized", false);
            }else if(naturalisedStr.equals("?")){
                UUID naturalisedDoubtUuid = state.getTransformer().getPresenceTermUuid("?Nat.");
                PresenceAbsenceTerm naturalisedDoubtState = getPresenceTerm(state, naturalisedDoubtUuid, null, null, null, false);
                result.add(naturalisedDoubtState);
                checkAbsentHighestState(highestStatus, line, "naturalized", true);
            }else{
                logger.warn(line + "Naturalized not recognized: " + naturalisedStr);
            }
        }
        if(dudStr != null){
            if(dudStr.equals("P")){
                UUID dudUuid = state.getTransformer().getPresenceTermUuid("Dud.");
                PresenceAbsenceTerm dudState = getPresenceTerm(state, dudUuid, null, null, null, false);
                result.add(dudState);
                highestStatus = highestStatus != null ? highestStatus : dudState;
            }else if(isMinus(dudStr)){
                UUID nonNativeErrorUuid = state.getTransformer().getPresenceTermUuid("-Dud.");
                PresenceAbsenceTerm nonNativeErrorState = getPresenceTerm(state, nonNativeErrorUuid, null, null, null, false);
                result.add(nonNativeErrorState);
                checkAbsentHighestState(highestStatus, line, "non-native and doubtfully naturalised", false);
            }else if(dudStr.equals("?")){
                UUID naturalisedDoubtUuid = state.getTransformer().getPresenceTermUuid("?Dud.");
                PresenceAbsenceTerm naturalisedDoubtState = getPresenceTerm(state, naturalisedDoubtUuid, null, null, null, false);
                result.add(naturalisedDoubtState);
                checkAbsentHighestState(highestStatus, line, "non-native and doubtfully naturalised", true);
            }else{
                logger.warn(line + "non-native and doubtfully naturalised not recognized: " + dudStr);
            }
        }
        if(advStr != null){
            if(advStr.equals("A")){
                PresenceAbsenceTerm advState = state.getTransformer().getPresenceTermByKey("Adv.");
//                PresenceAbsenceTerm advState = getPresenceTerm(state, advUuid, null, null, null, false);
                result.add(advState);
                highestStatus = highestStatus != null ? highestStatus : advState;
            }else if(isMinus(advStr)){
                UUID advUuid = state.getTransformer().getPresenceTermUuid("-Adv.");
                PresenceAbsenceTerm advState = getPresenceTerm(state, advUuid, null, null, null, false);
                result.add(advState);
                checkAbsentHighestState(highestStatus, line, "adventive", false);
            }else if(advStr.equals("(A)")){
                UUID rareCasualUuid = state.getTransformer().getPresenceTermUuid("(A)");
                PresenceAbsenceTerm rareCasual = getPresenceTerm(state, rareCasualUuid, null, null, null, false);
                result.add(rareCasual);
            }else{
                logger.warn(line + "'adventive (casual) alien' not recognized: " + advStr);
            }
        }else if(cultStr != null){
            if (! (cultStr.matches("(C|\\(C\\)|\\?|–)"))){
                logger.warn("'cultivated' not recognized: " + cultStr);
            }else if(cultStr.equals("C")){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("Cult.");
                result.add(cultivatedState);
                highestStatus = highestStatus != null ? highestStatus : cultivatedState;
            }else if(cultStr.equals("?")){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("?Cult.");
                result.add(cultivatedState);
                checkAbsentHighestState(highestStatus, line, "cultivated", true);
            }else if(cultStr.equals("(C)")){
                UUID ocassualCultUuid = state.getTransformer().getPresenceTermUuid("(C)");
                PresenceAbsenceTerm cultivatedState = getPresenceTerm(state, ocassualCultUuid, null, null, null, false);
                result.add(cultivatedState);
            }else if(isMinus(cultStr)){
                PresenceAbsenceTerm cultivatedState = state.getTransformer().getPresenceTermByKey("-Cult.");
                result.add(cultivatedState);
                checkAbsentHighestState(highestStatus, line, "cultivated", false);
            }else{
                logger.warn(line + "'cultivated' not recognized: " + cultStr);
            }
        }
        state.setHighestStatusForTaxon(highestStatus);
        return result;
    }


    /**
     * @param highestStatus
     * @param line
     */
    private void checkAbsentHighestState(PresenceAbsenceTerm highestStatus, String line, String stateLabel, boolean doubtful) {
        //can be removed, highest status is not used anymore
        if (highestStatus == null){
            String absentStr = doubtful ? "doubtful" : "absent";
            logger.info(line + "Highest cuban state is " + absentStr + " " + stateLabel);
        }

    }


    /**
     * @param indigenousStr
     * @return
     */
    private boolean isMinus(String str) {
        return str.equals("-") || str.equals("–") || str.equals("‒");
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
            +"((\\sFC(\\-S)?(\\s&\\sA&S)?)|(\\sA&S)|\\sSagra|\\sCombs|\\sBritton|\\sGriseb\\.(\\sFC-S|\\sA&S)?|\\sWright"
            + "|\\sHammer|\\sEngl\\.||\\sMaza|\\sMiers|\\sRoig|\\sBorhidi|\\sFRC|\\sCoL"
            + "|\\sAckerman|\\sMújica|\\sDíaz|\\sUrb\\.)?(\\s+p\\.\\s*p\\.)?";


    private static final String missapliedRegExStr = "(\\?\\s)?“(.*{5,})”\\s+(" + auctRegExStr + "|sensu\\s+.{2,})";
    private static final String sphalmRegExStr = "“(.*{5,})”\\s+((FC-S|A&S)\\s)?sphalm\\.(\\s(FC(-S)?|A&S|inval\\.))?";
    private static final String nomInvalRegExStr = "“(.*{5,})”\\s+nom\\.\\s+inval\\.(\\s(West|Moldenke|FC|Jacq.))?";
    private static final String homonymRegExStr = "\\s*(\\[.*\\])*\\s*";

    private static final Pattern acceptedRegEx = Pattern.compile(acceptedRegExStr + homonymRegExStr);
    private static final Pattern heterotypicRegEx = Pattern.compile(heterotypicRegExStr + homonymRegExStr);
    private static final Pattern missapliedRegEx = Pattern.compile(missapliedRegExStr);
    private static final Pattern nomInvalRegEx = Pattern.compile(nomInvalRegExStr);
    private static final Pattern sphalmRegEx = Pattern.compile(sphalmRegExStr);

    /**
     * @param record
     * @param state
     * @param taxon
     */
    private void makeSynonyms(HashMap<String, String> record, CubaImportState state, boolean isFirstSynonym) {
//        boolean forAccepted = true;
        String synonymStr = record.get("Syn.");
        String line = state.getCurrentLine() + ": ";


        if (synonymStr == null){
            //TODO test that this is not a synonym only line
            return;
        }

        if (state.getCurrentTaxon() == null){
            logger.error(line + "Current taxon is null for synonym");
            return;
        }


        synonymStr = synonymStr.trim();
        synonymStr = synonymStr.replace("[taxon]", "[infraspec.]");

//        String heterotypicRegExStr = "([^\\(]{5,}(\\(.+\\))?[^\\)\\(]{2,})(\\((.{6,})\\))?";
//        String heterotypicRegExStr = "([^\\(]{5,})(\\((.{6,})\\))?";

//        Pattern heterotypicRegEx = Pattern.compile(heterotypicRegExStr + homonymRegExStr);


        Matcher missapliedMatcher = missapliedRegEx.matcher(synonymStr);
        Matcher nomInvalMatcher = nomInvalRegEx.matcher(synonymStr);
        Matcher acceptedMatcher = acceptedRegEx.matcher(synonymStr);
        Matcher heterotypicMatcher = heterotypicRegEx.matcher(synonymStr);
        Matcher sphalmMatcher = sphalmRegEx.matcher(synonymStr);

        List<IBotanicalName> homonyms = new ArrayList<>();
        if (missapliedMatcher.matches()){
            boolean doubtful = missapliedMatcher.group(1) != null;
            String firstPart = missapliedMatcher.group(2);
            IBotanicalName name = (IBotanicalName)nameParser.parseSimpleName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            name.addSource(makeOriginalSource(state));

            String secondPart = missapliedMatcher.group(3);
            Taxon misappliedNameTaxon = Taxon.NewInstance(name, null);
            misappliedNameTaxon.addSource(makeOriginalSource(state));
            misappliedNameTaxon.setDoubtful(doubtful);
            if (secondPart.startsWith("sensu")){
                secondPart = secondPart.substring(5).trim();
                if (secondPart.contains(" ")){
                    logger.warn(line + "CHECK: Second part contains more than 1 word. Check if this is correct: " + secondPart);
                }
                Reference sensu = ReferenceFactory.newGeneric();
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
            Reference relRef = null;
            state.getCurrentTaxon().addMisappliedName(misappliedNameTaxon, relRef, null);
        }else if (nomInvalMatcher.matches()){
            String firstPart = nomInvalMatcher.group(1);
            String afterInval = nomInvalMatcher.group(2);
            if (StringUtils.isNotBlank(afterInval)){
                logger.warn(state.getCurrentLine() + ": After inval to be implemented: " + afterInval);
            }
            TaxonName name = (TaxonName)nameParser.parseSimpleName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            name.addSource(makeOriginalSource(state));
            NomenclaturalStatus status = NomenclaturalStatus.NewInstance( NomenclaturalStatusType.INVALID());
            name.addStatus(status);
            Synonym syn = state.getCurrentTaxon().addSynonymName(name, SynonymType.SYNONYM_OF());
            syn.addSource(makeOriginalSource(state));
        }else if (sphalmMatcher.matches()){
            String firstPart = sphalmMatcher.group(1);
            String sphalmPart = synonymStr.replace(firstPart, "").replace("“","").replace("”","").trim();
            TaxonName name = (TaxonName)nameParser.parseSimpleName(firstPart, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
//            NomenclaturalStatus status = NomenclaturalStatus.NewInstance( NomenclaturalStatusType.INVALID());
//            name.addStatus(status);
            name.addSource(makeOriginalSource(state));
            Synonym syn = state.getCurrentTaxon().addSynonymName(name, SynonymType.SYNONYM_OF());
            syn.setAppendedPhrase(sphalmPart);
            syn.setSec(null);
            syn.addSource(makeOriginalSource(state));
        }else if (acceptedMatcher.matches()){
            String firstPart = acceptedMatcher.group(1);
            String homonymPart = acceptedMatcher.groupCount() < 2 ? null : acceptedMatcher.group(2);
            List<IBotanicalName> list = handleHomotypicGroup(firstPart, state, state.getCurrentTaxon().getName(), false, homonyms, homonymPart, false);
            checkFirstSynonym(state, list, isFirstSynonym, synonymStr, false);
        }else if(heterotypicMatcher.matches()){
            String firstPart = heterotypicMatcher.group(1).trim();
            String secondPart = heterotypicMatcher.groupCount() < 3 ? null : heterotypicMatcher.group(3);
            String homonymPart = heterotypicMatcher.groupCount() < 4 ? null : heterotypicMatcher.group(4);
            boolean isDoubtful = firstPart.matches("^\\?\\s*.*");
            firstPart = replaceHomonIlleg(firstPart);
            boolean isHomonym = firstPart.matches(".*" + HOMONYM_MARKER);
            TaxonName synName = (TaxonName)makeName(state, firstPart);
            if (synName.isProtectedTitleCache()){
                logger.warn(line + "Heterotypic base synonym could not be parsed correctly: " + firstPart);
            }
            if (isHomonym){
                homonyms.add(synName);
            }
            Synonym syn = state.getCurrentTaxon().addHeterotypicSynonymName(synName);
            syn.setDoubtful(isDoubtful);
            syn.addSource(makeOriginalSource(state));
            List<IBotanicalName> list = handleHomotypicGroup(secondPart, state, synName, true, homonyms, homonymPart, isDoubtful);
            checkFirstSynonym(state, list, isFirstSynonym, synonymStr, true);

        }else if (isSpecialHeterotypic(synonymStr)){
            TaxonName synName = (TaxonName)makeName(state, synonymStr);
            if (synName.isProtectedTitleCache()){
                logger.warn(line + "Special heterotypic synonym could not be parsed correctly:" + synonymStr);
            }
            Synonym syn = state.getCurrentTaxon().addHeterotypicSynonymName(synName);
            syn.addSource(makeOriginalSource(state));
        }else{
            logger.warn(line + "Synonym entry does not match: " + synonymStr);
        }
    }

    /**
     * @param state
     * @param list
     * @param isFirstSynonym
     * @param synonymStr
     * @param b
     */
    private void checkFirstSynonym(CubaImportState state, List<IBotanicalName> list, boolean isFirstSynonym, String synonymStr, boolean isHeterotypicMatcher) {
        if (!isFirstSynonym){
            return;
        }
        String line = state.getCurrentLine() + ": ";
        IBotanicalName currentName = isHeterotypicMatcher? (IBotanicalName)state.getCurrentTaxon().getName(): list.get(0);
        boolean currentHasBasionym = currentName.getBasionymAuthorship() != null;
        IBotanicalName firstSynonym = isHeterotypicMatcher ? list.get(0): list.get(1);
//        if (list.size() <= 1){
//            logger.error(line + "homotypic list size is 1 but shouldn't");
//            return;
//        }
        if (isHeterotypicMatcher && currentHasBasionym){
            logger.error(line + "Current taxon (" + currentName.getTitleCache() + ") has basionym author but has no homotypic basionym , but : " + synonymStr);
        }else if (isHeterotypicMatcher){
            //first synonym must not have a basionym author
            if (firstSynonym.getBasionymAuthorship() != null){
                logger.error(line + "Current taxon (" + currentName.getTitleCache() + ") has no basionym but first synonym requires basionym : " + synonymStr);
            }
        }else{  //isAcceptedMatcher
            if (currentHasBasionym){
                if (! matchAuthor(currentName.getBasionymAuthorship(), firstSynonym.getCombinationAuthorship())){
                    logger.info(line + "Current basionym author and first synonym combination author do not match: " + currentName.getTitleCache() + "<->" + firstSynonym.getTitleCache());
                }
            }else{
                if (! matchAuthor(currentName.getCombinationAuthorship(), firstSynonym.getBasionymAuthorship())){
                    logger.info(line + "Current combination author and first synonym basionym author do not match: " + currentName.getTitleCache() + "<->" + firstSynonym.getTitleCache());
                }
            }
        }

    }


    /**
     * @param synonymStr
     * @return
     */
    private boolean isSpecialHeterotypic(String synonymStr) {
        if (synonymStr == null){
            return false;
        }else if (synonymStr.equals("Rhynchospora prenleloupiana (‘prenteloupiana’) Boeckeler")){
            return true;
        }else if (synonymStr.equals("Psidium longipes var. orbiculare (O.Berg) McVaugh")){
            return true;
        }
        return false;
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
                logger.warn(state.getCurrentLine() + ": Province distribution status could not be defined: " + record.get(areaKey));
            }
            Distribution distribution = Distribution.NewInstance(area, status);
            desc.addElement(distribution);
            distribution.addSource(makeDescriptionSource(state));
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
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
    private List<IBotanicalName> handleHomotypicGroup(String homotypicStrOrig,
            CubaImportState state,
            IBotanicalName homotypicName,
            boolean isHeterotypic,
            List<IBotanicalName> homonyms,
            String homonymPart,
            boolean isDoubtful) {

        List<IBotanicalName> homotypicNameList = new ArrayList<>();
        homotypicNameList.add(homotypicName);

        String homotypicStr = homotypicStrOrig;
        if (homotypicStr == null){
            return homotypicNameList;
        }else if (homotypicStr.startsWith("(") && homotypicStr.endsWith("")){
            homotypicStr = homotypicStr.substring(1, homotypicStr.length() - 1);
        }

        HomotypicalGroup homotypicGroup = homotypicName.getHomotypicalGroup();
        String[] splits = homotypicStr.split("\\s*,\\s*");
        for (String split : splits){
            split = replaceHomonIlleg(split);
            boolean isHomonym = split.matches(".*" + HOMONYM_MARKER);
            TaxonName newName = (TaxonName)makeName(state, split);
            newName.setHomotypicalGroup(homotypicGroup);  //not really necessary as this is later set anyway
            if (newName.isProtectedTitleCache()){
                logger.warn(state.getCurrentLine() + ": homotypic name part could not be parsed: " + split);
            }
            if (isHomonym){
                homonyms.add(newName);
            }
            if (isHeterotypic){
                Synonym syn = state.getCurrentTaxon().addHeterotypicSynonymName(newName, null, null, homotypicGroup);
                syn.setDoubtful(isDoubtful);
                syn.addSource(makeOriginalSource(state));
//                newName.addBasionym(homotypicName);
            }else{
                state.getCurrentTaxon().addHomotypicSynonymName(newName);
            }
            handleBasionym(state, homotypicNameList, homonyms, newName);
            homotypicNameList.add(newName);
        }
        makeHomonyms(homonyms, homonymPart, state, homotypicGroup);
        return homotypicNameList;
    }


    /**
     * @param split
     * @return
     */
    private String replaceHomonIlleg(String split) {
        String result = split.trim().replace("homon. illeg.", "nom. illeg. homon.").trim();
        return result;
    }


    /**
     * @param homonyms
     * @param homonymPart
     * @param state
     * @param currentBasionym
     */
    private void makeHomonyms(List<IBotanicalName> homonyms, String homonymPartOrig, CubaImportState state,
            HomotypicalGroup homotypicGroup) {
        String line = state.getCurrentLine() + ": ";
        String homonymPart = homonymPartOrig == null ? "" : homonymPartOrig.trim();
        if (homonyms.isEmpty() && homonymPart.equals("")){
            return;
        }else if (homonymPart.equals("")){
            logger.warn(line + "SynonymPart has homonyms but homonymPart is empty");
            return;
        }
        homonymPart = homonymPart.substring(1, homonymPart.length() - 1);
        String[] splits = homonymPart.split("\\]\\s*\\[");
        if (splits.length != homonyms.size()){
            if(homonyms.size() == 0 && splits.length >= 1){
                handleSimpleBlockingNames(splits, state, homotypicGroup);
            }else{
                logger.warn(line + "Number of homonyms (" + homonyms.size() + ") and homonymParts ("+splits.length+") does not match");
            }
            return;
        }
        int i = 0;
        for (String split : splits){
            split = split.replaceAll("^non\\s+", "");
            TaxonName newName = (TaxonName)makeName(state, split);
//            BotanicalName newName = (BotanicalName)nameParser.parseReferencedName(split, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
            if (newName.isProtectedTitleCache()){
                logger.warn(state.getCurrentLine() + ": homonym name could not be parsed: " + split);
            }
            homonyms.get(i).addRelationshipToName(newName, NameRelationshipType.LATER_HOMONYM(), null);
            i++;
        }
    }

    /**
     * @param homonymPart
     * @param state
     * @param homotypicGroup
     */
    private void handleSimpleBlockingNames(String[] splitsi,
            CubaImportState state,
            HomotypicalGroup homotypicGroup) {
        List<IBotanicalName> replacementNameCandidates = new ArrayList<>();
        for (String spliti : splitsi){

            String split = spliti.replaceAll("^non\\s+", "");
            IBotanicalName newName = makeName(state, split);
            if (newName.isProtectedTitleCache()){
                logger.warn(state.getCurrentLine() + ": blocking name could not be parsed: " + split);
            }
            Set<IBotanicalName> typifiedNames = (Set)homotypicGroup.getTypifiedNames();
            Set<IBotanicalName> candidates = new HashSet<>();
            for (IBotanicalName name : typifiedNames){
                if (name.getGenusOrUninomial() != null && name.getGenusOrUninomial().equals(newName.getGenusOrUninomial())){
                    if (name.getStatus().isEmpty() || ! name.getStatus().iterator().next().getType().equals(NomenclaturalStatusType.ILLEGITIMATE())){
                        candidates.add(name);
                    }
                }
            }
            if (candidates.size() == 1){
                TaxonName blockedName = (TaxonName)candidates.iterator().next();
                newName.addRelationshipToName(blockedName, NameRelationshipType.BLOCKING_NAME_FOR(), null);
                replacementNameCandidates.add(blockedName);
            }else{
                logger.warn(state.getCurrentLine() + ": Blocking name could not be handled. " + candidates.size() + " candidates.");
            }
        }
        makeReplacedSynonymIfPossible(state, homotypicGroup, replacementNameCandidates);
    }

    /**
     * @param homotypicGroup
     * @param replacementNameCandidates
     */
    private void makeReplacedSynonymIfPossible(CubaImportState state,
            HomotypicalGroup homotypicGroup,
            List<IBotanicalName> replacementNameCandidates) {
        String line = state.getCurrentLine() +": ";
        List<IBotanicalName> replacedCandidates = new ArrayList<>();
        for (TaxonName typifiedName : homotypicGroup.getTypifiedNames()){
            IBotanicalName candidate = typifiedName;
            if (candidate.getBasionymAuthorship() == null){
                if (candidate.getStatus().isEmpty()){
                    if (! replacementNameCandidates.contains(candidate)){
                        replacedCandidates.add(candidate);
                    }
                }
            }
        }
        if (replacedCandidates.size() == 1){
            TaxonName replacedSynonym = (TaxonName)replacedCandidates.iterator().next();
            for (IBotanicalName replacementName : replacementNameCandidates){
                replacementName.addReplacedSynonym(replacedSynonym, null, null, null);
            }
        }else if (replacedCandidates.size() < 1){
            logger.warn(line + "No replaced synonym candidate found");
        }else{
            logger.warn(line + "More than 1 ("+replacedCandidates.size()+") replaced synonym candidates found");
        }
    }


    /**
     * @param homotypicGroup
     * @param newName
     */
    private void handleBasionym(CubaImportState state, List<IBotanicalName> homotypicNameList,
            List<IBotanicalName> homonyms, IBotanicalName newName) {
        for (IBotanicalName existingName : homotypicNameList){
            if (existingName != newName){  //should not happen anymore, as new name is added later
                boolean onlyIfNotYetExists = true;
                createBasionymRelationIfPossible(state, existingName, newName, homonyms.contains(newName), onlyIfNotYetExists);
            }
        }
    }

    /**
     * @param state
     * @param name1
     * @param name2
     * @return
     */
    private void createBasionymRelationIfPossible(CubaImportState state, IBotanicalName name1,
            IBotanicalName name2,
            boolean name2isHomonym, boolean onlyIfNotYetExists) {
        TaxonName basionymName = TaxonName.castAndDeproxy(name1);
        TaxonName newCombination = TaxonName.castAndDeproxy(name2);
        //exactly one name must have a basionym author
        if (name1.getBasionymAuthorship() == null && name2.getBasionymAuthorship() == null
                || name1.getBasionymAuthorship() != null && name2.getBasionymAuthorship() != null){
            return;
        }

        //switch order if necessary
        if (! name2isHomonym && basionymName.getBasionymAuthorship() != null && newCombination.getBasionymAuthorship() == null){
            basionymName = TaxonName.castAndDeproxy(name2);
            newCombination = TaxonName.castAndDeproxy(name1);
        }
        if (matchAuthor(basionymName.getCombinationAuthorship(), newCombination.getBasionymAuthorship())
                && BasionymRelationCreator.matchLastNamePart(basionymName, newCombination)){
            newCombination.addBasionym(basionymName);
        }else{
            if ( (newCombination.getBasionyms().isEmpty() || ! onlyIfNotYetExists)
                    && isLegitimate(basionymName)
                    && ! name2isHomonym){
                logger.info(state.getCurrentLine() + ": Names are potential basionyms but either author or name part do not match: " + basionymName.getTitleCache() + " <-> " + newCombination.getTitleCache());
            }
        }
    }

    /**
     * @param basionymName
     * @return
     */
    private boolean isLegitimate(IBotanicalName basionymName) {
        for (NomenclaturalStatus nomStatus : basionymName.getStatus()){
            if (nomStatus.getType()!= null && nomStatus.getType().isIllegitimateType()){
                    return false;
            }
        }
        for (NameRelationship nameRel : basionymName.getNameRelations()){
            if (nameRel.getType()!= null && nameRel.getType().isIllegitimateType()){
                    return false;
            }
        }
        return true;
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
            annotation.setAnnotationType(AnnotationType.TECHNICAL());
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
        String taxonStrOrig = getValue(record, "Taxón");
        if (taxonStrOrig == null){
            return isSynonym ? state.getCurrentTaxon() : null;
        }

        boolean isAbsent = false;
        String taxonStr = taxonStrOrig;
        if (taxonStrOrig.startsWith("[") && taxonStrOrig.endsWith("]")){
            taxonStr = taxonStr.substring(1, taxonStr.length() - 1);
            isAbsent = true;
        }

        boolean isAuct = false;
        if (taxonStr.endsWith("auct.")){
            isAuct = true;
            taxonStr.replace("auct.", "").trim();
        }
        state.setTaxonIsAbsent(isAbsent);
        IBotanicalName botanicalName = makeName(state, taxonStr);
        Reference sec = getSecReference(state);
        Taxon taxon = Taxon.NewInstance(botanicalName, sec);
        if (isAuct){
            taxon.setAppendedPhrase("auct.");
        }

        TaxonNode higherNode;
        if (botanicalName.isProtectedTitleCache()){
            logger.warn(state.getCurrentLine() + ": Taxon could not be parsed: " + taxonStrOrig);
            higherNode = familyNode;
        }else{
            String genusStr = botanicalName.getGenusOrUninomial();
            Taxon genus = state.getHigherTaxon(genusStr);
            if (genus != null){
                higherNode = genus.getTaxonNodes().iterator().next();
            }else{
                IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
                name.addSource(makeOriginalSource(state));
                name.setGenusOrUninomial(genusStr);
                genus = Taxon.NewInstance(name, sec);
                genus.addSource(makeOriginalSource(state));
                higherNode = familyNode.addChildTaxon(genus, null, null);
                state.putHigherTaxon(genusStr, genus);
            }
        }
        taxon.addSource(makeOriginalSource(state));

        TaxonNode newNode = higherNode.addChildTaxon(taxon, null, null);
        if(isAbsent){
            botanicalName.setTitleCache(taxonStrOrig, true);
            newNode.setExcluded(true);
        }

        return taxon;
    }

    private final String orthVarRegExStr = "[A-Z][a-z]+\\s[a-z]+\\s(\\(‘([a-z]){3,}’\\))\\s(\\([A-Z][a-z]+\\.?\\)\\s)?[A-Z][a-zó]+\\.?";
    private final Pattern orthVarRegEx = Pattern.compile(orthVarRegExStr);
    /**
     * @param taxonStr
     * @return
     */
    private IBotanicalName makeName(CubaImportState state, String nameStrOrig) {
        //normalize
        String nameStr = normalizeStatus(nameStrOrig);
        //orthVar
        Matcher orthVarMatcher = orthVarRegEx.matcher(nameStr);
        String orthVar = null;
        if (orthVarMatcher.matches()) {
            orthVar = orthVarMatcher.group(1);
            nameStr = nameStr.replace(" " + orthVar, "").trim().replaceAll("\\s{2,}", " ");
            orthVar = orthVar.substring(2, orthVar.length() - 2);
        }

        boolean isNomInval = false;
        if (nameStr.endsWith("nom. inval.")){
            isNomInval = true;
            nameStr = nameStr.replace("nom. inval.", "").trim();
        }

        TaxonName result = (TaxonName)nameParser.parseReferencedName(nameStr, nc, Rank.SPECIES());
        result.addSource(makeOriginalSource(state));
        if (isNomInval){
            result.addStatus(NomenclaturalStatus.NewInstance(NomenclaturalStatusType.INVALID()));
        }
        if (orthVar != null){
            TaxonName orthVarName = (TaxonName)result.clone();
            orthVarName.addSource(makeOriginalSource(state));
            //TODO
            Reference citation = null;
            orthVarName.addRelationshipToName(result, NameRelationshipType.ORTHOGRAPHIC_VARIANT(), citation, null, null);
            orthVarName.setSpecificEpithet(orthVar);
        }
        normalizeAuthors(result);
        return result;

    }

    /**
     * @param result
     */
    private void normalizeAuthors(IBotanicalName result) {
        result.setCombinationAuthorship(normalizeAuthor(result.getCombinationAuthorship()));
        result.setExCombinationAuthorship(normalizeAuthor(result.getExCombinationAuthorship()));
        result.setExBasionymAuthorship(normalizeAuthor(result.getExBasionymAuthorship()));
        result.setBasionymAuthorship(normalizeAuthor(result.getBasionymAuthorship()));

    }


    /**
     * @param combinationAuthorship
     * @return
     */
    private TeamOrPersonBase<?> normalizeAuthor(TeamOrPersonBase<?> author) {
        if (author == null){
            return null;
        }
        TeamOrPersonBase<?> result;
        if (author.isInstanceOf(Person.class)){
            result = normalizePerson(CdmBase.deproxy(author, Person.class));
        }else{
            Team team = CdmBase.deproxy(author, Team.class);
            List<Person> list = team.getTeamMembers();
            for(int i = 0; i < list.size(); i++){
                Person person = list.get(i);
                Person tmpMember = normalizePerson(person);
                list.set(i, tmpMember);
            }
            return team;
        }
        return result;
    }


    /**
     * @param deproxy
     * @return
     */
    private Person normalizePerson(Person person) {
        String title = person.getNomenclaturalTitle();
        title = title.replaceAll("(?<=[a-zA-Z])\\.(?=[a-zA-Z])", ". ");
        person.setNomenclaturalTitle(title);
        boolean isFilius = title.endsWith(" f.");
        if (isFilius){
            title.replace(" f.", "");
        }

        String[] splits = title.split("\\s+");
        int nNotFirstName = isFilius ? 2 : 1;
        person.setLastname(splits[splits.length - nNotFirstName] + (isFilius? " f." : ""));
        person.setFirstname(CdmUtils.concat(" ", Arrays.copyOfRange(splits, 0, splits.length-nNotFirstName)));
        return person;
    }


    /**
     * @param state
     * @return
     */
    private Reference getSecReference(CubaImportState state) {
        Reference result = state.getSecReference();
        if (result == null){
            result = ReferenceFactory.newDatabase();
            result.setTitle("Flora of Cuba");
            state.setSecReference(result);
        }
        return result;
    }


    private static final String[] nomStatusStrings = new String[]{"nom. cons.", "ined.", "nom. illeg.",
            "nom. rej.","nom. cons. prop.","nom. altern.","nom. confus.","nom. dub.", "nom. nud."};
    /**
     * @param taxonStr
     * @return
     */
    private String normalizeStatus(String nameStr) {
        if (nameStr == null){
            return null;
        }
        String result = nameStr.replaceAll(HOMONYM_MARKER, "").trim();
        for (String nomStatusStr : nomStatusStrings){
            nomStatusStr = " " + nomStatusStr;
            if (result.endsWith(nomStatusStr)){
                result = result.replace(nomStatusStr, "," + nomStatusStr);
            }
        }
        result = result.replaceAll(DOUBTFUL_MARKER, "").trim();
        result = result.replace("[taxon]", "[infraspec.]");
        return result;


    }


    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(HashMap<String, String> record, CubaImportState state) {
        String familyStr = getValue(record, "Fam. default");
        if (familyStr == null){
            return null;
        }
        familyStr = familyStr.trim();
        String alternativeFamilyStr = null;
        if (familyStr.contains("/")){
            String[] splits = familyStr.split("/");
            if (splits.length > 2){
                logger.warn(state.getCurrentLine() +": " + "More than 1 alternative name:" + familyStr);
            }
            familyStr = splits[0].trim();
            alternativeFamilyStr = splits[1].trim();
        }

        Taxon family = state.getHigherTaxon(familyStr);
        TaxonNode familyNode;
        if (family != null){
            familyNode = family.getTaxonNodes().iterator().next();
        }else{
            TaxonName name = (TaxonName)makeFamilyName(state, familyStr);
            Reference sec = getSecReference(state);
            family = Taxon.NewInstance(name, sec);
            ITaxonTreeNode rootNode = getClassification(state);
            familyNode = rootNode.addChildTaxon(family, sec, null);
            state.putHigherTaxon(familyStr, family);

        }

        if (isNotBlank(alternativeFamilyStr)){
            NameRelationshipType type = NameRelationshipType.ALTERNATIVE_NAME();
            TaxonName alternativeName = (TaxonName)makeFamilyName(state, alternativeFamilyStr);
            IBotanicalName familyName = family.getName();
            boolean hasRelation = false;
            for (NameRelationship nameRel : familyName.getRelationsToThisName()){
                if (nameRel.getType().equals(type)){
                    if (nameRel.getFromName().equals(alternativeName)){
                        hasRelation = true;
                    }
                }
            }
            if (!hasRelation){
                familyName.addRelationshipFromName(alternativeName, type, null);
            }

        }

        return familyNode;
    }


    /**
     * @param state
     * @param taxon
     */
    private void validateTaxonIsAbsent(CubaImportState state, Taxon taxon) {
        if (!state.isTaxonIsAbsent()){
            return;
        }

        for (DescriptionElementBase el : taxon.getDescriptions().iterator().next().getElements()){
            if (el instanceof Distribution){
                Distribution dist = (Distribution)el;
                NamedArea area = dist.getArea();
                if (isCubanArea(area)){
                    PresenceAbsenceTerm status = dist.getStatus();
                    if (status != null && !status.isAbsenceTerm()){
                        if (!isDoubtfulTerm(status)){
                            String name = taxon.getName().getTitleCache();
                            logger.error(state.getCurrentLine() +": Taxon ("+name+")is absent'[]' but has presence distribution: " + status.getTitleCache());
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * @param state
     * @param taxon
     */
    private void validateEndemic(CubaImportState state, Taxon taxon) {

        boolean hasExternalPresence = false;
        for (DescriptionElementBase el : taxon.getDescriptions().iterator().next().getElements()){
            if (el instanceof Distribution){
                Distribution dist = (Distribution)el;
                NamedArea area = dist.getArea();
                if (!isCubanArea(area)){
                    PresenceAbsenceTerm status = dist.getStatus();
                    if (status != null && !status.isAbsenceTerm()){
                        if (!isDoubtfulTerm(status)){
                            hasExternalPresence = true;
                            if (state.isEndemic()){
                                String name = taxon.getName().getTitleCache();
                                logger.error(state.getCurrentLine() +": Taxon ("+name+")is endemic but has non-cuban distribution: " + area.getIdInVocabulary() + "-" + status.getIdInVocabulary());
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (!state.isEndemic() && ! hasExternalPresence){
            String name = taxon.getName().getTitleCache();
            logger.error(state.getCurrentLine() +": Taxon ("+name+")is not endemic but has no non-cuban distribution" );
        }
    }


    /**
     * @param state
     * @param taxon
     * @param famStr
     * @param famRef
     * @return
     */
    private Taxon makeAlternativeFamilyTaxon(CubaImportState state, String famStr, Reference famRef) {
        String key = famRef.getTitle() + ":"+ famStr;
        Taxon family = state.getHigherTaxon(key);
        if (family == null){
            IBotanicalName name = makeFamilyName(state, famStr);
            family = Taxon.NewInstance(name, famRef);
            state.putHigherTaxon(key, family);
        }

        return family;
    }


    /**
     * @param state
     * @param famStr
     * @return
     */
    private IBotanicalName makeFamilyName(CubaImportState state, String famStr) {
        IBotanicalName name = state.getFamilyName(famStr);
        if (name == null){
            name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
            name.setGenusOrUninomial(famStr);
            state.putFamilyName(famStr, name);
            name.addSource(makeOriginalSource(state));
        }
        return name;
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
            Reference sec = getSecReference(state);
            if (classification == null){
                String classificationName = state.getConfig().getClassificationName();
                //TODO
                Language language = Language.DEFAULT();
                classification = Classification.NewInstance(classificationName, sec, language);
                state.setClassification(classification);
                classification.setUuid(state.getConfig().getClassificationUuid());
                classification.getRootNode().setUuid(rootUuid);
            }

            IBotanicalName plantaeName = TaxonNameFactory.NewBotanicalInstance(Rank.KINGDOM());
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
	    boolean isSynonymOnly = false;

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        if (record.get("Fam. default") == null && keys.size() == 2 && record.get("Syn.") == null && record.get("Nat") != null && record.get("Adv") != null){
            //second header line, don't handle
            return;
        }

        //Fam.
        TaxonNode familyTaxon = getFamilyTaxon(record, state);
        if (familyTaxon == null){
            if (record.get("Taxón") != null){
                logger.warn(line + "Family not recognized but taxon exists: " + record.get("Taxón"));
                return;
            }else if (record.get("Syn.") == null){
                logger.warn(line + "Family not recognized but also no synonym exists");
                return;
            }else{
                isSynonymOnly = true;
            }
        }

       //Taxón
        Taxon taxon = makeTaxon(record, state, familyTaxon, isSynonymOnly);
        if (taxon == null && ! isSynonymOnly){
            logger.warn(line + "taxon could not be created and is null");
            return;
        }
        state.setCurrentTaxon(taxon);

        //Fam. ALT
        if (!isSynonymOnly){
            makeAlternativeFamilies(record, state, familyTaxon, taxon);
        }

        //(Notas)
        makeNotes(record, state);

        //Syn.
        makeSynonyms(record, state, !isSynonymOnly);

        //End, Ind, Ind? D, Nat N, Dud P, Adv A, Cult C
        makeCubanDistribution(record, state);


//        "CuW","PR PR*","Art","Hab(*)","May","Mat","IJ",
//        "CuC","VC","Ci","SS","CA","Cam","LT",
//        "CuE","Gr","Ho","SC","Gu",
        makeProvincesDistribution(record, state);

//      "Esp","Ja","PR","Men","Bah","Cay",
//      "AmN","AmC","AmS","VM"});
        makeOtherAreasDistribution(record, state);

        validateTaxonIsAbsent(state, taxon);
        if (!isSynonymOnly){
            validateEndemic(state, taxon);
        }

        state.setHighestStatusForTaxon(null);

		return;
    }


    /**
     * @param state
     * @return
     */
    private IdentifiableSource makeOriginalSource(CubaImportState state) {
        return IdentifiableSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, state.getConfig().getSourceReference());
    }
    /**
     * @param state
     * @return
     */
    private DescriptionElementSource makeDescriptionSource(CubaImportState state) {
        return DescriptionElementSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, state.getConfig().getSourceReference());
    }

    private static Set<UUID> doubtfulStatus = new HashSet<>();

    /**
     * @param status
     * @return
     */
    private boolean isDoubtfulTerm(PresenceAbsenceTerm status) {
        if (doubtfulStatus.isEmpty()){
            doubtfulStatus.add(CubaTransformer.nonNativeDoubtfullyNaturalisedUuid);
            doubtfulStatus.add(CubaTransformer.doubtfulIndigenousDoubtfulUuid);
            doubtfulStatus.add(CubaTransformer.endemicDoubtfullyPresentUuid);
            doubtfulStatus.add(CubaTransformer.naturalisedDoubtfullyPresentUuid);
            doubtfulStatus.add(CubaTransformer.nonNativeDoubtfullyPresentUuid);
            doubtfulStatus.add(CubaTransformer.occasionallyCultivatedUuid);
            doubtfulStatus.add(CubaTransformer.rareCasualUuid);
            doubtfulStatus.add(PresenceAbsenceTerm.NATIVE_PRESENCE_QUESTIONABLE().getUuid());
            doubtfulStatus.add(PresenceAbsenceTerm.CULTIVATED_PRESENCE_QUESTIONABLE().getUuid());
        }
        boolean isDoubtful = doubtfulStatus.contains(status.getUuid());
        return isDoubtful;
    }


    /**
     * @param area
     * @return
     */
    private boolean isCubanArea(NamedArea area) {
        if (area.getUuid().equals(CubaTransformer.uuidCuba)){
            return true;
        }else if (area.getPartOf()!= null){
            return isCubanArea(area.getPartOf());
        }else{
            return false;
        }
    }


    /**
     * @param record
     * @param state
     * @param familyTaxon
     * @param taxon
     */
    private void makeAlternativeFamilies(HashMap<String, String> record,
            CubaImportState state,
            TaxonNode familyTaxon,
            Taxon taxon) {

        String famFRC = record.get("Fam. FRC");
        String famAS = record.get("Fam. A&S");
        String famFC = record.get("Fam. FC");

        Reference refFRC = makeReference(state, CubaTransformer.uuidRefFRC);
        Reference refAS = makeReference(state, CubaTransformer.uuidRefAS);
        Reference refFC = makeReference(state, CubaTransformer.uuidRefFC);

        makeSingleAlternativeFamily(state, taxon, famFRC, refFRC);
        makeSingleAlternativeFamily(state, taxon, famAS, refAS);
        makeSingleAlternativeFamily(state, taxon, famFC, refFC);
    }


    /**
     * @param state
     * @param uuidreffrc
     * @return
     */
    private Reference makeReference(CubaImportState state, UUID uuidRef) {
        Reference ref = state.getReference(uuidRef);
        if (ref == null){
            ref = getReferenceService().find(uuidRef);
            state.putReference(uuidRef, ref);
        }
        return ref;
    }


    /**
     * @param state
     * @param taxon
     * @param famString
     * @param famRef
     */
    private void makeSingleAlternativeFamily(CubaImportState state, Taxon taxon, String famStr, Reference famRef) {
        if (isBlank(famStr)){
            famStr = "-";
//            return;
        }

        TaxonDescription desc = getTaxonDescription(taxon, false, true);

        UUID altFamUuid1;
        UUID altFamUuid2;
        try {
            altFamUuid1 = state.getTransformer().getFeatureUuid("Alt.Fam.");
            altFamUuid2 = state.getTransformer().getFeatureUuid("Alt.Fam.2");
        } catch (UndefinedTransformerMethodException e) {
            throw new RuntimeException(e);
        }


        Taxon famTaxon = makeAlternativeFamilyTaxon(state, famStr, famRef);


        //TextData
        Feature feature1 = getFeature(state, altFamUuid1, "Families in other Floras (Text)", "Families in other Floras (Text)", "Other floras", null);
        feature1.addRepresentation(Representation.NewInstance("Familias en otras Floras", "Familias en otras Floras", null, Language.SPANISH_CASTILIAN()));
//        TextData textData = TextData.NewInstance(feature1, famStr, Language.DEFAULT(), null);
        TextData textData = TextData.NewInstance(feature1, null, Language.DEFAULT(), null);
        textData.addSource(OriginalSourceType.PrimaryTaxonomicSource, null,null, famRef, null, famTaxon.getName(),null);
        desc.addElement(textData);



        //TaxonInteraction
        Feature feature2 = getFeature(state, altFamUuid2, "Families in other Floras", "Families in other Floras", "Other floras(2)", null);
        feature2.setSupportsTaxonInteraction(true);
        feature2.addRepresentation(Representation.NewInstance("Familias en otras Floras", "Familias en otras Floras", null, Language.SPANISH_CASTILIAN()));
        TaxonInteraction taxInteract = TaxonInteraction.NewInstance(feature2);
        textData.putText(Language.SPANISH_CASTILIAN(), "Familias en otras Floras");
        taxInteract.setTaxon2(famTaxon);
        taxInteract.addSource(OriginalSourceType.PrimaryTaxonomicSource, null,null, famRef, null);
        desc.addElement(taxInteract);

        //Concept Relation
        famTaxon.addTaxonRelation(taxon, TaxonRelationshipType.INCLUDES(), taxon.getSec(), null);

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
                });
        for (String areaKey : areaKeys){
            state.setCubanProvince(true);
            makeSingleProvinceDistribution(areaKey, record, state);
        }
    }

    private void makeOtherAreasDistribution(HashMap<String, String> record, CubaImportState state) {
        List<String> areaKeys = Arrays.asList(new String[]{
                "Esp","Ja","PR","Men","Bah","Cay",
                "AmN","AmC","AmS","VM"});
        for (String areaKey : areaKeys){
            state.setCubanProvince(false);
            makeSingleProvinceDistribution(areaKey, record, state);
        }
    }




    /**
     * @param areaKey
     * @param record
     * @param state
     * @param highestStatus
     * @return
     * @throws UndefinedTransformerMethodException
     */
    private PresenceAbsenceTerm makeProvinceStatus(String areaKey,
            HashMap<String, String> record,
            CubaImportState state) throws UndefinedTransformerMethodException {

        String statusStr = record.get(areaKey);
        if (statusStr == null){
            return null;
        }else{
            statusStr = statusStr.trim();
        }
        PresenceAbsenceTerm status = state.getTransformer().getPresenceTermByKey(statusStr);
        if (status == null){
//            PresenceAbsenceTerm highestStatus = state.getHighestStatusForTaxon();
            if (state.isCubanProvince() && isMinus(statusStr)){
//                getAbsenceTermForStatus(state, highestStatus);
                //we now handle cuban provinces same as external regions
                status = state.getTransformer().getPresenceTermByKey("--");
            }else if (! state.isCubanProvince() && isMinus(statusStr)){
                status = state.getTransformer().getPresenceTermByKey("--");
            }else{
//                logger.warn("Unhandled status str for provinces / external regions: " + statusStr);
                UUID statusUuid = state.getTransformer().getPresenceTermUuid(statusStr);
                if (statusUuid == null){
                    logger.error(state.getCurrentLine() + ": Undefined status str for provinces / external regions. No UUID given: '" + statusStr + "'");
                }else{
                    status = getPresenceTerm(state, statusUuid, statusStr, statusStr, statusStr, false);
                }
            }
        }

        return status;
    }


    /**
     * @param highestStatus
     * @throws UndefinedTransformerMethodException
     */
    private PresenceAbsenceTerm getAbsenceTermForStatus(CubaImportState state, PresenceAbsenceTerm highestStatus) throws UndefinedTransformerMethodException {
        if (highestStatus == null){
            logger.warn(state.getCurrentLine() + ": Highest status not defined");
            return null;
        }
        PresenceAbsenceTerm result = null;
        if (highestStatus.equals(getStatus(state, "E"))){
            result = getStatus(state, "-E");
        }else if (highestStatus.getUuid().equals(state.getTransformer().getPresenceTermUuid("Ind.")) || highestStatus.equals(PresenceAbsenceTerm.NATIVE())){
            result = getStatus(state, "-Ind.");
        }else if (highestStatus.equals(getStatus(state, "Ind.?"))){
            result = getStatus(state, "-Ind.?");  //TODO
        }else if (highestStatus.equals(getStatus(state, "N"))){
            result = getStatus(state, "-N");
        }else if (highestStatus.equals(getStatus(state, "P"))){
            result = getStatus(state, "-P");
        }else if (highestStatus.equals(getStatus(state, "A"))){
            result = getStatus(state, "-A");
        }else if (highestStatus.equals(getStatus(state, "C"))){
            result = getStatus(state, "-C");
        }
        logger.warn(state.getCurrentLine() + ": Absent province status could not be defined for highest status " + highestStatus.getTitleCache());
        return result;
    }


    /**
     * @param string
     * @return
     * @throws UndefinedTransformerMethodException
     */
    private PresenceAbsenceTerm getStatus(CubaImportState state, String key) throws UndefinedTransformerMethodException {
        PresenceAbsenceTerm status = state.getTransformer().getPresenceTermByKey(key);
        if (status == null){
            UUID statusUuid = state.getTransformer().getPresenceTermUuid(key);
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
