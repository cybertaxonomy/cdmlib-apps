/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 14.12.2016
 *
 */

@Component
public class FloraHellenicaTaxonImport<CONFIG extends FloraHellenicaImportConfigurator>
            extends FloraHellenicaImportBase<CONFIG>{

    private static final long serialVersionUID = -6291948918967763381L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaTaxonImport.class);

    private static final String LIFE_FORM = "Life-form";
    private static final String STATUS = "Status";
    private static final String CHOROLOGICAL_CATEGOGY = "Chorological categogy";

    private static UUID rootUuid = UUID.fromString("aa667b0b-b417-470e-a9b0-ef9409a3431e");
    private static UUID plantaeUuid = UUID.fromString("4f151932-ab97-4d81-b88e-46fe82cd3e88");

    private Map<String, State> lifeformMap = new HashMap<>();
    private Map<String, State> chorologyMap = new HashMap<>();
    private PresenceAbsenceTerm rangeRestricted;
    private PresenceAbsenceTerm doubtfullyRangeRestricted;
    private OrderedTermVocabulary<State> habitatVoc;
    private OrderedTermVocabulary<State> statusVoc;



   private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "Unique ID","uuid","Group","Family","Genus","Species","Species Author","Subspecies","Subspecies Author",
            "IoI","NPi","SPi","Pe","StE","EC","NC","NE","NAe","WAe","Kik","KK","EAe",
            STATUS,CHOROLOGICAL_CATEGOGY,LIFE_FORM,"A","C","G","H","M","P","R","W", "Taxon"
    });

    private String lastGenus;
    private String lastSpecies;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "valid taxa names";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        initLifeFormMap();
        initChorologyMap();
        initOtherTerms(state);

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        String noStr = getValue(record, "Unique ID");
        Taxon taxon = makeTaxon(state, line, record, noStr);

        //Distribution
        TaxonDescription desc = getTaxonDescription(taxon);
        makeDistribution(state, line, noStr, desc);

        makeChorologicalCategory(state, line, noStr, desc);

        //lifeform
        makeLifeform(state, line, noStr, desc);

        //habitat
        makeHabitat(state, line, noStr, desc);

        state.putTaxon(noStr, taxon);

    }


    /**
     * @param state
     *
     */
    private void initOtherTerms(SimpleExcelTaxonImportState<CONFIG> state) {
        if (state.getConfig().isStatusAsDistribution()){
            if (rangeRestricted == null){
                rangeRestricted = (PresenceAbsenceTerm)getTermService().find(FloraHellenicaTransformer.uuidStatusRangeRestricted);
            }
            if (this.doubtfullyRangeRestricted == null){
                doubtfullyRangeRestricted = (PresenceAbsenceTerm)getTermService().find(FloraHellenicaTransformer.uuidStatusRangeRestrictedDoubtfully);
            }
        }else{
            if (this.statusVoc == null){
                @SuppressWarnings("unchecked")
                OrderedTermVocabulary<State> voc = (OrderedTermVocabulary<State>) getVocabularyService().find(
                        FloraHellenicaTransformer.uuidFloraHellenicaStatusVoc);
                statusVoc = voc;
            }
        }
        if (this.habitatVoc == null){
            @SuppressWarnings("unchecked")
            OrderedTermVocabulary<State> voc = (OrderedTermVocabulary<State>) getVocabularyService().find(
                    FloraHellenicaTransformer.uuidFloraHellenicaHabitatVoc);
            habitatVoc = voc;
        }
    }


    private void initLifeFormMap() {
        if (lifeformMap.isEmpty()){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaLifeformVoc;
            @SuppressWarnings("unchecked")
            OrderedTermVocabulary<State> lifeformVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
            for (State state : lifeformVoc.getTerms()){
                lifeformMap.put(state.getIdInVocabulary(), state);
            }
        }
    }

    private void initChorologyMap() {
        if (chorologyMap.isEmpty()){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaChorologicalVoc;
            @SuppressWarnings("unchecked")
            OrderedTermVocabulary<State> voc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
            for (State state : voc.getTerms()){
                chorologyMap.put(state.getIdInVocabulary(), state);
            }
        }
    }



    /**
     * @param state
     * @param line
     * @param noStr
     * @param desc
     */
    private void makeChorologicalCategory(SimpleExcelTaxonImportState<CONFIG> state, String line, String noStr,
            TaxonDescription desc) {

        Map<String, String> record = state.getOriginalRecord();
        String valueStr = getValue(record, CHOROLOGICAL_CATEGOGY);

        String value = valueStr;
        if (value == null){
            return;
        }
        Feature choroFeature = getFeature(state, FloraHellenicaTransformer.uuidFloraHellenicaChorologyFeature,
                "Chorology", "The Chorological Category", "Choro", null);
        CategoricalData catData = CategoricalData.NewInstance(choroFeature);
        catData.setOrderRelevant(true);

        String[] splits = value.split(" & ");
        replaceDirection(splits, line);
        for (String split: splits){
            String[] splitsA = split.split("/");
            for (String splitA : splitsA){
                String[] splitsB = splitA.split(", ");
                for (String splitB : splitsB){
                    splitB = normalizeChorology(splitB);
                    State choroTerm = chorologyMap.get(splitB);
                    if (choroTerm == null){
                        logger.warn(line + "Some chorology could not be recognized in: " + value + "; Term was: " +splitB);
                    }else{
                        catData.addStateData(choroTerm);
                    }
                }
            }
        }
        if (catData.getStateData().size() > 1){
            catData.setOrderRelevant(true);
        }
        desc.addElement(catData);
    }

    /**
     * @param splitB
     * @return
     */
    private String normalizeChorology(String choroStr) {
        choroStr = choroStr.trim()
                .replace("BK", "Bk")
                .replace("Austral.", "Austr.")
                .replace("trop.As.", "trop. As.");
        if (choroStr.startsWith("[") && !choroStr.endsWith("]")){
            choroStr += "]";
        }else if (!choroStr.startsWith("[") && choroStr.endsWith("]")){
            choroStr = "[" + choroStr;
        }
        return choroStr;
    }

    /**
     * @param splits
     * @param line
     */
    private void replaceDirection(String[] splits, String line) {
        if (splits.length > 1){
            String[] divs = splits[1].split("-");
            if (divs.length == 2){
                splits[0] = splits[0] + "-" + divs[1];
            }else{
                logger.warn(line + "Splits[1] has not expected format: " + splits[1]);
            }
        }
    }

    /**
     * @param state
     * @param line
     * @param noStr
     * @param desc
     */
    private void makeLifeform(SimpleExcelTaxonImportState<CONFIG> state, String line, String noStr,
            TaxonDescription desc) {
        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, LIFE_FORM);
        String[] splits = value.split("\\s+");
        if (splits.length > 2){
            logger.warn("Unexpected length of lifeform: " + value + " line: "  + line );
        }
        CategoricalData catData = CategoricalData.NewInstance(Feature.LIFEFORM());
        for (String split : splits){
            State lifeform = lifeformMap.get(split);
            if (lifeform == null){
                logger.warn(line + "Unexpected lifeform: " + value);
            }else{
                catData.addStateData(lifeform);
            }
        }
        desc.addElement(catData);

    }

    /**
     * @param state
     * @param line
     * @param noStr
     * @param desc
     */
    private void makeHabitat(SimpleExcelTaxonImportState<CONFIG> state, String line, String noStr,
            TaxonDescription desc) {
        CategoricalData catData = CategoricalData.NewInstance(Feature.HABITAT());
        handleHabitat(state, catData, "A", FloraHellenicaTransformer.uuidHabitatA, line, noStr);
        handleHabitat(state, catData, "C", FloraHellenicaTransformer.uuidHabitatC, line, noStr);
        handleHabitat(state, catData, "G", FloraHellenicaTransformer.uuidHabitatG, line, noStr);
        handleHabitat(state, catData, "H", FloraHellenicaTransformer.uuidHabitatH, line, noStr);
        handleHabitat(state, catData, "M", FloraHellenicaTransformer.uuidHabitatM, line, noStr);
        handleHabitat(state, catData, "P", FloraHellenicaTransformer.uuidHabitatP, line, noStr);
        handleHabitat(state, catData, "R", FloraHellenicaTransformer.uuidHabitatR, line, noStr);
        handleHabitat(state, catData, "W", FloraHellenicaTransformer.uuidHabitatW, line, noStr);
        desc.addElement(catData);
    }

    /**
     * @param state
     * @param catData
     * @param string
     * @param uuidhabitata
     * @param line
     * @param noStr
     */
    private void handleHabitat(SimpleExcelTaxonImportState<CONFIG> state, CategoricalData catData, String label,
            UUID uuidHabitat, String line, String noStr) {
        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, "" + label);
        if (value == null){
            //do nothing
        }else if (value.matches("[ACGHMPRW]")){
            State habitatState = this.getStateTerm(state, uuidHabitat, null, null, null, habitatVoc);
            catData.addStateData(habitatState);
        }else{
            logger.warn(line + "Unrecognized habitat state '" + value + "' for " + label);
        }
    }

    /**
     * @param state
     * @param line
     * @param noStr
     * @param desc
     */
    private void makeDistribution(SimpleExcelTaxonImportState<CONFIG> state, String line, String noStr,
            TaxonDescription desc) {
        //TODO status Greece
        handleStatus(state, desc, STATUS, FloraHellenicaTransformer.uuidAreaGreece, line, noStr);

        handleDistribution(state, desc, "IoI", FloraHellenicaTransformer.uuidAreaIoI, line, noStr);
        handleDistribution(state, desc, "NPi", FloraHellenicaTransformer.uuidAreaNPi, line, noStr);
        handleDistribution(state, desc, "SPi", FloraHellenicaTransformer.uuidAreaSPi, line, noStr);
        handleDistribution(state, desc, "Pe", FloraHellenicaTransformer.uuidAreaPe, line, noStr);
        handleDistribution(state, desc, "StE", FloraHellenicaTransformer.uuidAreaStE, line, noStr);
        handleDistribution(state, desc, "EC", FloraHellenicaTransformer.uuidAreaEC, line, noStr);
        handleDistribution(state, desc, "NC", FloraHellenicaTransformer.uuidAreaNC, line, noStr);
        handleDistribution(state, desc, "NE", FloraHellenicaTransformer.uuidAreaNE, line, noStr);
        handleDistribution(state, desc, "NAe", FloraHellenicaTransformer.uuidAreaNAe, line, noStr);
        handleDistribution(state, desc, "WAe", FloraHellenicaTransformer.uuidAreaWAe, line, noStr);
        handleDistribution(state, desc, "Kik", FloraHellenicaTransformer.uuidAreaKik, line, noStr);
        handleDistribution(state, desc, "KK", FloraHellenicaTransformer.uuidAreaKK, line, noStr);
        handleDistribution(state, desc, "EAe", FloraHellenicaTransformer.uuidAreaEAe, line, noStr);
    }

    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private Taxon makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record,
            String noStr) {

        TaxonNode familyTaxon = getFamilyTaxon(record, state);
        if (familyTaxon == null){
            logger.warn(line + "Family not created: " + record.get("Family"));
        }

        String genusStr = getValue(record, "Genus");
        String speciesStr = getValue(record, "Species");
        String speciesAuthorStr = getValue(record, "Species Author");
        String subSpeciesStr = getValue(record, "Subspecies");
        String subSpeciesAuthorStr = getValue(record, "Subspecies Author");
        String uuidStr = getValue(record, "uuid");
        UUID uuid = UUID.fromString(uuidStr);
        boolean isSubSpecies = isNotBlank(subSpeciesStr);
        boolean isAutonym = isSubSpecies && speciesStr.equals(subSpeciesStr);
        if (isSubSpecies && ! isAutonym && isBlank(subSpeciesAuthorStr)){
            logger.warn(line + "Non-Autonym subspecies has no auhtor");
        }else if (isSubSpecies && isAutonym && isNotBlank(subSpeciesAuthorStr)){
            logger.warn(line + "Autonym subspecies has subspecies auhtor");
        }

        String[] nameParts;
        if (!isSubSpecies){
            nameParts = new String[]{genusStr, speciesStr, speciesAuthorStr};
        }else if (!isAutonym){
            nameParts = new String[]{genusStr, speciesStr, "subsp. " + subSpeciesStr, subSpeciesAuthorStr};
        }else{
            nameParts = new String[]{genusStr, speciesStr, speciesAuthorStr, "subsp. " + subSpeciesStr};
        }

        String nameStr = CdmUtils.concat(" ", nameParts);
        boolean isSensuStrictu = false;
        if (nameStr.endsWith("s.str.")){
            isSensuStrictu = true;
            nameStr = nameStr.substring(0, nameStr.length() - "s.str.".length() ).trim();
        }
        Rank rank = isSubSpecies ? Rank.SUBSPECIES() : Rank.SPECIES();
        TaxonName name = (TaxonName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + nameStr);
        }
        name = replaceNameAuthorsAndReferences(state, name);

        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));
        taxon.addImportSource(noStr, getWorksheetName(state.getConfig()), getSourceCitation(state), null);
        if (isSensuStrictu){
            taxon.setAppendedPhrase("s.str.");
        }
        String parentStr = isSubSpecies ?
                makeSpeciesKey(genusStr, speciesStr, speciesAuthorStr) : genusStr;
        taxon.setUuid(uuid);
        boolean genusAsBefore = genusStr.equals(lastGenus);
        boolean speciesAsBefore = speciesStr.equals(lastSpecies);
        TaxonNode parent = getParent(state, parentStr);
        if (parent != null){
            if (!isSubSpecies && genusAsBefore || isSubSpecies && speciesAsBefore){
//            if (genusAsBefore ){
                    //everything as expected
                TaxonNode newNode = parent.addChildTaxon(taxon, getSecReference(state), null);
                getTaxonNodeService().saveOrUpdate(newNode);
            }else{
                logger.warn(line + "Unexpected non-missing parent");
            }
        }else{
            if (isSubSpecies){
                logger.warn(line + "Subspecies should always have an existing parent");
            }else if (genusAsBefore){
                logger.warn(line + "Unexpected missing genus parent");
            }else{
                parent = makeGenusNode(state, record, genusStr);
                TaxonNode newNode = parent.addChildTaxon(taxon, getSecReference(state), null);
                getTaxonNodeService().save(newNode);
            }
        }
        if (!isSubSpecies){
            state.putHigherTaxon(makeSpeciesKey(genusStr, speciesStr, speciesAuthorStr), taxon);
        }

//        this.lastFamily = familyStr
        this.lastGenus = genusStr;
        this.lastSpecies = speciesStr;
        return taxon;
    }

    /**
     * @param genusStr
     * @param speciesStr
     * @param speciesAuthorStr
     * @return
     */
    private String makeSpeciesKey(String genusStr, String speciesStr, String speciesAuthorStr) {
        return CdmUtils.concat(" ", new String[]{genusStr, speciesStr, speciesAuthorStr});
    }

    private TaxonNode makeGenusNode(SimpleExcelTaxonImportState<CONFIG> state,
            Map<String, String> record, String genusStr) {
        TaxonName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
        name.setGenusOrUninomial(genusStr);
        name = replaceNameAuthorsAndReferences(state, name);
        Taxon genus = Taxon.NewInstance(name, getSecReference(state));
        TaxonNode family = getFamilyTaxon(record, state);
        TaxonNode genusNode = family.addChildTaxon(genus, getSecReference(state), null);
        state.putHigherTaxon(genusStr, genus);
        genus.addSource(makeOriginalSource(state));
        getTaxonNodeService().save(genusNode);
        return genusNode;
    }

    private TaxonNode getParent(SimpleExcelTaxonImportState<CONFIG> state, String parentStr) {
        Taxon taxon = state.getHigherTaxon(parentStr);

        return taxon == null ? null : taxon.getTaxonNodes().iterator().next();
    }

    private TaxonNode getFamilyTaxon(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        String familyStr = getValue(record, "Family");
        if (familyStr == null){
            return null;
        }
        familyStr = familyStr.trim();

        Taxon family = state.getHigherTaxon(familyStr);
        TaxonNode familyNode;
        if (family != null){
            familyNode = family.getTaxonNodes().iterator().next();
        }else{
            TaxonName name = makeFamilyName(state, familyStr);
            name = replaceNameAuthorsAndReferences(state, name);

            Reference sec = getSecReference(state);
            family = Taxon.NewInstance(name, sec);

            ITaxonTreeNode groupNode = getGroupTaxon(record, state);
            familyNode = groupNode.addChildTaxon(family, sec, null);
            state.putHigherTaxon(familyStr, family);
            getTaxonNodeService().save(familyNode);
        }

        return familyNode;
    }

    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getGroupTaxon(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        String groupStr = getValue(record, "Group");
        if (groupStr == null){
            return null;
        }
        groupStr = groupStr.trim();

        Taxon group = state.getHigherTaxon(groupStr);
        TaxonNode groupNode;
        if (group != null){
            groupNode = group.getTaxonNodes().iterator().next();
        }else{
            TaxonName name = makeFamilyName(state, groupStr);
            name = replaceNameAuthorsAndReferences(state, name);

            Reference sec = getSecReference(state);
            group = Taxon.NewInstance(name, sec);
            ITaxonTreeNode rootNode = getClassification(state);
            groupNode = rootNode.addChildTaxon(group, sec, null);
            state.putHigherTaxon(groupStr, group);
            getTaxonNodeService().save(groupNode);
        }

        return groupNode;
    }

    private TaxonNode rootNode;
    private TaxonNode getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (rootNode == null){
            Reference sec = getSecReference(state);
            String classificationName = state.getConfig().getClassificationName();
            Language language = Language.DEFAULT();
            Classification classification = Classification.NewInstance(classificationName, sec, language);
            classification.setUuid(state.getConfig().getClassificationUuid());
            classification.getRootNode().setUuid(rootUuid);

            TaxonName plantaeName = TaxonNameFactory.NewBotanicalInstance(Rank.KINGDOM());
            plantaeName.setGenusOrUninomial("Plantae");
            plantaeName = replaceNameAuthorsAndReferences(state, plantaeName);

            Taxon plantae = Taxon.NewInstance(plantaeName, sec);
            TaxonNode plantaeNode = classification.addChildTaxon(plantae, null, null);
            plantaeNode.setUuid(plantaeUuid);
            getClassificationService().save(classification);

            rootNode = plantaeNode;
        }
        return rootNode;
    }

    private void handleDistribution(SimpleExcelTaxonImportState<CONFIG> state,
                TaxonDescription desc, String key, UUID uuid, String line, String id) {
        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, key);
        if (value == null || value.matches("[x\\.\\?]")){
            NamedArea area = getNamedArea(state, uuid, null, null, null, null, null);
            Distribution dist;
            if (".".equals(value)){
                logger.warn(line + "'.' Should not exist anmore as a distribution status: '" + value + "' for " + key);
                dist = Distribution.NewInstance(area, PresenceAbsenceTerm.ABSENT());
            }else if (value == null){
                //TODO is absent wanted
                dist = Distribution.NewInstance(area, PresenceAbsenceTerm.ABSENT());
            }else if ("x".equals(value)){
                dist = Distribution.NewInstance(area, PresenceAbsenceTerm.PRESENT());
            }else if ("?".equals(value)){
                dist = Distribution.NewInstance(area, PresenceAbsenceTerm.PRESENT_DOUBTFULLY());
            }else {
                logger.warn(line + "Not matching status. THis should not happpen '" + value + "' for " + key);
                return;
            }
            desc.addElement(dist);
            dist.addImportSource(id, getWorksheetName(state.getConfig()), getSourceCitation(state), line);
        }else {
            logger.warn(line + "Unrecognized distribution status '" + value + "' for " + key);
        }
    }

    private void handleStatus(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonDescription desc, String key, UUID uuid, String line, String id) {
        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, key);
        DescriptionElementBase descEl;
        if (state.getConfig().isStatusAsDistribution()){
            NamedArea area = getNamedArea(state, uuid, null, null, null, null, null);
            if (value == null || ".".equals(value) ){
                descEl = Distribution.NewInstance(area, PresenceAbsenceTerm.NATIVE());
                if (".".equals(value)){
                    logger.warn(line + "'.' Should not exist anymore as a distribution status: '" + value + "' for " + key);
                }
            }else if ("Range-restricted".equals(value)){
                descEl = Distribution.NewInstance(area, rangeRestricted);
            }else if ("?Range-restricted".equals(value)){
                descEl = Distribution.NewInstance(area, doubtfullyRangeRestricted);
            }else if ("Xenophyte".equals(value)){
                descEl = Distribution.NewInstance(area, PresenceAbsenceTerm.INTRODUCED());
            }else if ("?Xenophyte".equals(value)){
                descEl = Distribution.NewInstance(area, PresenceAbsenceTerm.INTRODUCED_DOUBTFULLY_INTRODUCED());
            }else {
                logger.warn(line + "Not matching status. This should not happpen '" + value + "' for " + key);
                return;
            }
        }else{
            CategoricalData catData = CategoricalData.NewInstance(Feature.STATUS());
            descEl = catData;
            if (value == null || ".".equals(value) ){
                handleSingleStatus(state, catData, FloraHellenicaTransformer.uuidStatusNative, line);
                if (".".equals(value)){
                    logger.warn(line + "'.' Should not exist anymore as a status: '" + value + "' for " + key);
                }
            }else if ("Range-restricted".equals(value)){
                handleSingleStatus(state, catData, FloraHellenicaTransformer.uuidStatusRangeRestricted, line);
            }else if ("?Range-restricted".equals(value)){
                handleSingleStatus(state, catData, FloraHellenicaTransformer.uuidStatusRangeRestrictedDoubtfully, line);
            }else if ("Xenophyte".equals(value)){
                handleSingleStatus(state, catData, FloraHellenicaTransformer.uuidStatusXenophyte, line);
            }else if ("?Xenophyte".equals(value)){
                handleSingleStatus(state, catData, FloraHellenicaTransformer.uuidStatusXenophyteDoubtfully, line);
            }else {
                logger.warn(line + "Not matching status. This should not happpen '" + value + "' for " + key);
                return;
            }
        }

        desc.addElement(descEl);
        descEl.addImportSource(id, getWorksheetName(state.getConfig()), getSourceCitation(state), line);
    }

    private void handleSingleStatus(SimpleExcelTaxonImportState<CONFIG> state, CategoricalData catData,
            UUID uuidStatus, String line) {

        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, "Status");
        if (value == null || value.matches("(\\??Range-restricted|\\??Xenophyte)")){
            State statusState = this.getStateTerm(state, uuidStatus, null, null, null, statusVoc);
            catData.addStateData(statusState);
        }else{
            logger.warn(line + "Unrecognized status '" + value + "' for column 'Status'");
        }
    }


}
