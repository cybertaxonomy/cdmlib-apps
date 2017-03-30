/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.ext.geo.GeoServiceArea;
import eu.etaxonomy.cdm.ext.geo.GeoServiceAreaAnnotatedMapping;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @date 14.12.2016
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

    private OrderedTermVocabulary<NamedArea> areasVoc;
    private NamedArea greece;
    private OrderedTermVocabulary<State> lifeformVoc;
    private OrderedTermVocabulary<State> habitatVoc;
    private Map<String, State> lifeformMap = new HashMap<>();

    private OrderedTermVocabulary<PresenceAbsenceTerm> statusVoc;
    private PresenceAbsenceTerm rangeRestricted;
    private PresenceAbsenceTerm doubtfullyRangeRestricted;


    private OrderedTermVocabulary<State> chorologicalVoc;
    private Map<String, State> chorologyMap = new HashMap<>();


   private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "Unique ID","Group","Family","Genus","Species","Species Author","Subspecies","Subspecies Author",
            "IoI","NPi","SPi","Pe","StE","EC","NC","NE","NAe","WAe","Kik","KK","EAe",
            STATUS,CHOROLOGICAL_CATEGOGY,LIFE_FORM,"A","C","G","H","M","P","R","W", "Taxon"
    });

    private String lastGenus;
    private String lastSpecies;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName() {
        return "valid taxa names";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        initAreaVocabulary(state);
        initLifeformVocabulary(state);
        initHabitatVocabulary(state);
        initChorologicalVocabulary(state);
        initStatusVocabulary(state);
        makeFeatureTree(state);

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

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

    boolean hasFeatureTree = false;
    /**
     * @param state
     */
    private void makeFeatureTree(SimpleExcelTaxonImportState<CONFIG> state) {
        if (hasFeatureTree  ){
            return;
        }
        if (getFeatureTreeService().find(state.getConfig().getUuidFeatureTree()) != null){
            hasFeatureTree = true;
            return;
        }
        FeatureTree result = FeatureTree.NewInstance(state.getConfig().getUuidFeatureTree());
        result.setTitleCache(state.getConfig().getFeatureTreeTitle(), true);
        FeatureNode root = result.getRoot();
        FeatureNode newNode;

        ITermService service = getTermService();
        Feature newFeature = (Feature)service.find(Feature.DISTRIBUTION().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(FloraHellenicaTransformer.uuidFloraHellenicaChorologyFeature);
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.LIFEFORM().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.HABITAT().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);


        newFeature = (Feature)service.find(Feature.NOTES().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        getFeatureTreeService().saveOrUpdate(result);
        hasFeatureTree = true;

    }

    /**
     * @param state
     * @param line
     * @param noStr
     * @param desc
     */
    private void makeChorologicalCategory(SimpleExcelTaxonImportState<CONFIG> state, String line, String noStr,
            TaxonDescription desc) {

        HashMap<String, String> record = state.getOriginalRecord();
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
        HashMap<String, String> record = state.getOriginalRecord();
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
        HashMap<String, String> record = state.getOriginalRecord();
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
    private Taxon makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, HashMap<String, String> record,
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
        Rank rank = isSubSpecies ? Rank.SUBSPECIES() : Rank.SPECIES();
        BotanicalName name = (BotanicalName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + nameStr);
        }
        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));
        taxon.addImportSource(noStr, getWorksheetName(), getSourceCitation(state), null);
//        String parentStr = isSubSpecies ? makeSpeciesKey(genusStr, speciesStr, speciesAuthorStr) : genusStr;
        String parentStr = genusStr;
        boolean genusAsBefore = genusStr.equals(lastGenus);
        boolean speciesAsBefore = speciesStr.equals(lastSpecies);
        TaxonNode parent = getParent(state, parentStr);
        if (parent != null){
//            if (!isSubSpecies && genusAsBefore || isSubSpecies && speciesAsBefore){
            if (genusAsBefore ){
                        //everything as expected
                TaxonNode newNode = parent.addChildTaxon(taxon, getSecReference(state), null);
                getTaxonNodeService().save(newNode);
            }else{
                logger.warn(line + "Unexpected non-missing parent");
            }
        }else{
//            if (isSubSpecies){
//                logger.warn(line + "Subspecies should always have an existing parent");
//            }else
            if (genusAsBefore){
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

    /**
     * @param state
     * @param record
     * @param genusStr
     * @return
     */
    private TaxonNode makeGenusNode(SimpleExcelTaxonImportState<CONFIG> state,
            HashMap<String, String> record, String genusStr) {
        BotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
        name.setGenusOrUninomial(genusStr);
        Taxon genus = Taxon.NewInstance(name, getSecReference(state));
        TaxonNode family = getFamilyTaxon(record, state);
        TaxonNode genusNode = family.addChildTaxon(genus, getSecReference(state), null);
        state.putHigherTaxon(genusStr, genus);
        genus.addSource(makeOriginalSource(state));
        getTaxonNodeService().save(genusNode);
        return genusNode;
    }

    /**
     * @param state
     * @param parentStr
     * @return
     */
    private TaxonNode getParent(SimpleExcelTaxonImportState<CONFIG> state, String parentStr) {
        Taxon taxon = state.getHigherTaxon(parentStr);

        return taxon == null ? null : taxon.getTaxonNodes().iterator().next();
    }

    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
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
            BotanicalName name = makeFamilyName(state, familyStr);
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
    private TaxonNode getGroupTaxon(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
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
            BotanicalName name = makeFamilyName(state, groupStr);
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

            BotanicalName plantaeName = TaxonNameFactory.NewBotanicalInstance(Rank.KINGDOM());
            plantaeName.setGenusOrUninomial("Plantae");
            Taxon plantae = Taxon.NewInstance(plantaeName, sec);
            TaxonNode plantaeNode = classification.addChildTaxon(plantae, null, null);
            plantaeNode.setUuid(plantaeUuid);
            getClassificationService().save(classification);

            rootNode = plantaeNode;
        }
        return rootNode;
    }

    /**
     * @param desc
     * @param string
     * @param uuidUserDefinedAnnotationTypeVocabulary
     */
    private void handleDistribution(SimpleExcelTaxonImportState<CONFIG> state,
                TaxonDescription desc, String key, UUID uuid, String line, String id) {
        HashMap<String, String> record = state.getOriginalRecord();
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
            dist.addImportSource(id, getWorksheetName(), getSourceCitation(state), line);
        }else {
            logger.warn(line + "Unrecognized distribution status '" + value + "' for " + key);
        }
    }

    private void handleStatus(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonDescription desc, String key, UUID uuid, String line, String id) {
        HashMap<String, String> record = state.getOriginalRecord();
        String value = getValue(record, key);
        NamedArea area = getNamedArea(state, uuid, null, null, null, null, null);
        Distribution dist;
        if (value == null || ".".equals(value) ){
            dist = Distribution.NewInstance(area, PresenceAbsenceTerm.NATIVE());
            if (".".equals(value)){
                logger.warn(line + "'.' Should not exist anmore as a distribution status: '" + value + "' for " + key);
            }
        }else if ("Range-restricted".equals(value)){
            dist = Distribution.NewInstance(area, rangeRestricted);
        }else if ("?Range-restricted".equals(value)){
            dist = Distribution.NewInstance(area, doubtfullyRangeRestricted);
        }else if ("Xenophyte".equals(value)){
            dist = Distribution.NewInstance(area, PresenceAbsenceTerm.INTRODUCED());
        }else if ("?Xenophyte".equals(value)){
            dist = Distribution.NewInstance(area, PresenceAbsenceTerm.INTRODUCED_DOUBTFULLY_INTRODUCED());
        }else {
            logger.warn(line + "Not matching status. This should not happpen '" + value + "' for " + key);
            return;
        }
        desc.addElement(dist);
        dist.addImportSource(id, getWorksheetName(), getSourceCitation(state), line);
    }

    @SuppressWarnings("unchecked")
    private void initAreaVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (areasVoc == null){
            areasVoc = (OrderedTermVocabulary<NamedArea>)this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaAreasVoc);
            if (areasVoc == null){
                createAreasVoc(state);
            }
        }
    }


    /**
     * @param state
     */
    private void initChorologicalVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (chorologicalVoc == null){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaChorologicalVoc;
            chorologicalVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
            if (chorologicalVoc == null){
                createChorologicalVoc(state, uuid);
            }
        }

    }


    @SuppressWarnings("unchecked")
    private void initLifeformVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (lifeformVoc == null){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaLifeformVoc;
            lifeformVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
            if (lifeformVoc == null){
                createLifeformVoc(state, uuid);
            }
        }
    }

    /**
     * @param state
     * @param vocUuid
     */
    private void createLifeformVoc(SimpleExcelTaxonImportState<CONFIG> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Lifeforms";
        String description = "Lifeforms as used in the Checklist of Greece";
        lifeformVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        lifeformVoc.setUuid(vocUuid);

        addLifeform(state, "A", "Aquatics", FloraHellenicaTransformer.uuidLifeformA);
        addLifeform(state, "C", "Chamaephytes", FloraHellenicaTransformer.uuidLifeformC);
        addLifeform(state, "G", "Geophytes (Cryptophytes)", FloraHellenicaTransformer.uuidLifeformG);
        addLifeform(state, "H", "Hemicryptophytes", FloraHellenicaTransformer.uuidLifeformH);
        addLifeform(state, "P", "Phanerophytes", FloraHellenicaTransformer.uuidLifeformP);
        addLifeform(state, "T", "Therophytes", FloraHellenicaTransformer.uuidLifeformT);
        this.getVocabularyService().save(lifeformVoc);
        return;
    }


    /**
     * @param state
     * @param uuid
     */
    private void createChorologicalVoc(SimpleExcelTaxonImportState<CONFIG> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Chorological Categories";
        String description = "Chorological Categories as used in the Checklist of Greece";
        chorologicalVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        chorologicalVoc.setUuid(vocUuid);

        addChorological(state, "*", "Greek endemic", "Greek endemics (incl. single-island and single-mountain endemics)", FloraHellenicaTransformer.uuidChorologicalStar);
        addChorological(state, "Bk", "Balkan", "Taxa restricted to Balkan countries, occasionally extending to adjacent parts of SE Europe", FloraHellenicaTransformer.uuidChorologicalBk);
        addChorological(state, "BI", "Balkan-Italy", "Taxa restricted to Balkan countries and Italy (amphi-Adreatic)", FloraHellenicaTransformer.uuidChorologicalBI);
        addChorological(state, "BA", "Balkan-Anatolia", "Taxa restricted to Balkan countries and to Asia minor (Anatolia), occasionally extending to S Ukraine (Crimea), adjacent Caucasian countries (Georgia, Armenia) or N Iraq", FloraHellenicaTransformer.uuidChorologicalBA);
        addChorological(state, "BC", "Balkan-Central Europe", "Taxa distributed in the Balkans, Carpathians, Alps and adjacent areas (mainly in the mountains)", FloraHellenicaTransformer.uuidChorologicalBC);
        addChorological(state, "EM", "East Mediterranean", "Taxa restricted to the E Mediterranean, occasionally extending to S Italy or adjacent Caucasian countries", FloraHellenicaTransformer.uuidChorologicalEM);
        addChorological(state, "Me", "Mediterranean", "Taxa with a circum-Mediterranean distribution including Portugal, occasionally extending to the Caucasus area and N Iran", FloraHellenicaTransformer.uuidChorologicalMe);
        addChorological(state, "MA", "Mediterranean-Atlantic", "Taxa restricted to maritime W Europe and the Mediterranean", FloraHellenicaTransformer.uuidChorologicalMA);
        addChorological(state, "ME", "Mediterranean-European", "Taxa restricted to the Mediterranean and temperate Europe, occasionally extending to NW Africa and the Caucasus area", FloraHellenicaTransformer.uuidChorologicalME);
        addChorological(state, "MS", "Mediterranean-SW Asian", "Taxa distributed in one or more Mediterranean countries and extending to SW and C Asia", FloraHellenicaTransformer.uuidChorologicalMS);
        addChorological(state, "EA", "European-SW Asian", "Eruopean taxa (occasionally reachin N Africa) with a distribution extending to SW Asia, occasionally reaching C Asia", FloraHellenicaTransformer.uuidChorologicalEA);
        addChorological(state, "ES", "Euro-Siberian", "Taxa with main distribution in temperate Eurasia (occasionally reaching the Caucasus area)", FloraHellenicaTransformer.uuidChorologicalES);
        addChorological(state, "Eu", "European", "Taxa with a distribution all over Europe. In S European countries this category in fact represents the C European element", FloraHellenicaTransformer.uuidChorologicalEu);
        addChorological(state, "Pt", "Paleotemperate", "Taxa of extratropical Eurasia including the Himalaya and E Asia, not (or at most marginally) extending to North America", FloraHellenicaTransformer.uuidChorologicalPt);
        addChorological(state, "Ct", "Circumtemperate", "Taxa of both extratropical Eurasia and North America", FloraHellenicaTransformer.uuidChorologicalCt);
        addChorological(state, "IT", "Irano-Turanian", "Taxa with main distribution in arid SW and C Asia, extrazonally extending to the Mediterranean", FloraHellenicaTransformer.uuidChorologicalIT);
        addChorological(state, "SS", "Saharo-Sindian", "Taxa with main distribution in arid N Africa and SQ Asia, extrazonally extending to the Mediterranean", FloraHellenicaTransformer.uuidChorologicalSS);
        addChorological(state, "ST", "Subtropical-tropical", "Taxa widespread in the warmer regions of both hemispheres", FloraHellenicaTransformer.uuidChorologicalST);
        addChorological(state, "Bo", "(Circum-)Boreal", "Taxa with main distribution in N and high-montane Eurasia (occasionally extending to North America)", FloraHellenicaTransformer.uuidChorologicalBo);
        addChorological(state, "AA", "Arctic-Alpine", "Taxa with main distribution beyound the N and aove the high-montane timerlines o fEurasia (occasionally extending to North America)", FloraHellenicaTransformer.uuidChorologicalAA);
        addChorological(state, "Co", "Cosmopolitan", "Taxa distributed in all continents, i.e. beyond the N hemisphere. This category may be given in brackets after the known or supposed native distribution in cases of taxa that have been spread worldwide by humans", FloraHellenicaTransformer.uuidChorologicalCo);

        addChorological(state, "[trop.]", "[tropical]", "", FloraHellenicaTransformer.uuidChorologicaltrop);
        addChorological(state, "[subtrop.]", "[subtropical]", "", FloraHellenicaTransformer.uuidChorologicalsubtrop);
        addChorological(state, "[paleotrop.]", "[paleotropical]", "", FloraHellenicaTransformer.uuidChorologicalpaleotrop);
        addChorological(state, "[neotrop.]", "[neotropical]", "", FloraHellenicaTransformer.uuidChorologicalneotrop);
        addChorological(state, "[pantrop.]", "[pantropical]", "", FloraHellenicaTransformer.uuidChorologicalpantrop);
        addChorological(state, "[N-Am.]", "[North American]", "", FloraHellenicaTransformer.uuidChorologicalN_Am);
        addChorological(state, "[S-Am.]", "[South American]", "", FloraHellenicaTransformer.uuidChorologicalS_Am);
        addChorological(state, "[E-As.]", "[East Asian]", "", FloraHellenicaTransformer.uuidChorologicalE_As);
        addChorological(state, "[SE-As.", "[South East Asian]", "", FloraHellenicaTransformer.uuidChorologicalSE_As);
        addChorological(state, "[S-Afr.]", "[South African]", "", FloraHellenicaTransformer.uuidChorologicalS_Afr);
        addChorological(state, "[Arab.]", "[Arabian]", "", FloraHellenicaTransformer.uuidChorologicalArab);
        addChorological(state, "[Arab. NE-Afr.]", "[Arabian and North East African]", "", FloraHellenicaTransformer.uuidChorologicalArab_NE_Afr);
        addChorological(state, "[Caucas.]", "[Caucasian]", "", FloraHellenicaTransformer.uuidChorologicalCaucas);
        addChorological(state, "[Pontic]", "[Pontic]", "", FloraHellenicaTransformer.uuidChorologicalPontic);
        addChorological(state, "[Europ.]", "[European]", "", FloraHellenicaTransformer.uuidChorologicalEurop);
        addChorological(state, "[Austr.]", "[Australian]", "", FloraHellenicaTransformer.uuidChorologicalAustral);

        addChorological(state, "[W-Med.]", "[West Mediterranean]", "", FloraHellenicaTransformer.uuidChorologicalW_Med);
        addChorological(state, "[C-Med.]", "[Central Mediterranean]", "", FloraHellenicaTransformer.uuidChorologicalC_Med);
        addChorological(state, "[W-Eur.]", "[West European]", "", FloraHellenicaTransformer.uuidChorologicalW_Eur);
        addChorological(state, "[S-Eur.]", "[South European]", "", FloraHellenicaTransformer.uuidChorologicalS_Eur);
        addChorological(state, "[C-Am.]", "[Central American]", "", FloraHellenicaTransformer.uuidChorologicalC_Am);
        addChorological(state, "[C-As.]", "[Central Asian]", "", FloraHellenicaTransformer.uuidChorologicalC_As);
        addChorological(state, "[SW-As.]", "[South West Asian]", "", FloraHellenicaTransformer.uuidChorologicalSW_As);
        addChorological(state, "[unknown]", "[unknown]", "", FloraHellenicaTransformer.uuidChorologicalUnknown);
        addChorological(state, "[N-Afr.]", "[North African]", "", FloraHellenicaTransformer.uuidChorologicalN_Afr);
        addChorological(state, "[Am.]", "[American]", "", FloraHellenicaTransformer.uuidChorologicalAm);
        addChorological(state, "[paleosubtrop.]", "[paleosubtropical]", "", FloraHellenicaTransformer.uuidChorologicalPaleosubtrop);
        addChorological(state, "[SW-Eur.]", "[South West European]", "", FloraHellenicaTransformer.uuidChorologicalSW_Eur);

        addChorological(state, "[S-As.]", "[South Asian]", "", FloraHellenicaTransformer.uuidChorologicalS_As);
        addChorological(state, "[NE-Afr.]", "[North East African]", "", FloraHellenicaTransformer.uuidChorologicalNE_Afr);
        addChorological(state, "[NW-Afr.]", "[North West African]", "", FloraHellenicaTransformer.uuidChorologicalNW_Afr);
        addChorological(state, "[trop. Afr.]", "[tropical African]", "", FloraHellenicaTransformer.uuidChorologicalTrop_Afr);
        addChorological(state, "[Afr.]", "[Arican]", "", FloraHellenicaTransformer.uuidChorologicalAfr);
        addChorological(state, "[As.]", "[Asian]", "", FloraHellenicaTransformer.uuidChorologicalAs);
        addChorological(state, "[W-As.]", "[West Asian]", "", FloraHellenicaTransformer.uuidChorologicalW_As);
        addChorological(state, "[C-Eur.]", "[Central European]", "", FloraHellenicaTransformer.uuidChorologicalC_Eur);
        addChorological(state, "[E-Afr.]", "[East African]", "", FloraHellenicaTransformer.uuidChorologicalE_Afr);
        addChorological(state, "[W-Austr.]", "[West Australian]", "", FloraHellenicaTransformer.uuidChorologicalW_Austr);
        addChorological(state, "[trop. As.]", "[tropical Asian]", "", FloraHellenicaTransformer.uuidChorologicaltrop_As);

        addChorological(state, "[Co]", "[Cosmopolitan]", "Taxa distributed in all continents, i.e. beyond the N hemisphere. This category may be given in brackets after the known or supposed native distribution in cases of taxa that have been spread worldwide by humans", FloraHellenicaTransformer.uuidChorological__Co_);

        this.getVocabularyService().save(chorologicalVoc);
        return;

    }


    /**
     * @param state
     * @param string
     * @param string2
     * @param string3
     * @param uuidchorologicalstar
     */
    private void addChorological(SimpleExcelTaxonImportState<CONFIG> state, String abbrevLabel, String label,
            String desc, UUID uuidChorological) {
        desc = isBlank(desc)? label : desc;
        State chorologyTerm = addState(state, abbrevLabel, label, desc, uuidChorological, chorologicalVoc);
        chorologyMap.put(abbrevLabel, chorologyTerm);
    }

    /**
     * @param state
     * @param string
     * @param uuidlifeformt
     */
    private void addLifeform(SimpleExcelTaxonImportState<CONFIG> state, String abbrevLabel, String label, UUID uuidlifeform) {
        State lifeForm = addState(state, abbrevLabel, label, label, uuidlifeform, lifeformVoc);
        lifeformMap.put(abbrevLabel, lifeForm);
    }

    private State addState(SimpleExcelTaxonImportState<CONFIG> state,
            String abbrev, String stateLabel, String description, UUID uuid, OrderedTermVocabulary<State> voc) {
        State newState = State.NewInstance(
                description, stateLabel, abbrev);
        newState.setUuid(uuid);
        newState.setIdInVocabulary(abbrev);
        voc.addTerm(newState);
        return newState;
    }

    private PresenceAbsenceTerm addStatus(SimpleExcelTaxonImportState<CONFIG> state,
            String abbrev, String stateLabel, String description, UUID uuid, OrderedTermVocabulary<PresenceAbsenceTerm> voc) {
        PresenceAbsenceTerm newStatus = PresenceAbsenceTerm.NewPresenceInstance(
                description, stateLabel, abbrev);
        newStatus.setUuid(uuid);
        newStatus.setIdInVocabulary(abbrev);
        newStatus.setSymbol(abbrev);
        voc.addTerm(newStatus);
        return newStatus;
    }

    private void initHabitatVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (habitatVoc == null){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaHabitatVoc;
            habitatVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
            if (habitatVoc == null){
                createHabitatVoc(state, uuid);
            }
        }
    }

    private void initStatusVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (statusVoc == null){
            UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaStatusVoc;
            statusVoc = (OrderedTermVocabulary<PresenceAbsenceTerm>)this.getVocabularyService().find(uuid);
            if (statusVoc == null){
                createStatusVoc(state, uuid);
            }
        }
    }

    /**
     * @param state
     */
    private void createStatusVoc(SimpleExcelTaxonImportState<CONFIG> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Status";
        String description = "Status as used in the Checklist of Greece";
        statusVoc = OrderedTermVocabulary.NewInstance(TermType.PresenceAbsenceTerm,
                description, label, null, termSourceUri);
        statusVoc.setUuid(vocUuid);

        rangeRestricted = addStatus(state, "RR", "Range-restricted", "", FloraHellenicaTransformer.uuidStatusRangeRestricted, statusVoc);
        doubtfullyRangeRestricted = addStatus(state, "?RR", "?Range-restricted", "", FloraHellenicaTransformer.uuidStatusRangeRestrictedDoubtfully, statusVoc);

        this.getVocabularyService().save(statusVoc);
        return;
    }


    /**
     * @param state
     */
    private void createHabitatVoc(SimpleExcelTaxonImportState<CONFIG> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Habitats";
        String description = "Habitats as used in the Checklist of Greece";
        habitatVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        habitatVoc.setUuid(vocUuid);

        addHabitat(state, "A", "Freshwater habitats", "Freshwater habitats (Aquatic habitats, springs and fens, reedbeds and damp tall herb vegetation, seasonally flooded depressions, damp and seepage meadows, streambanks, river and lake shores)", FloraHellenicaTransformer.uuidHabitatA);
        addHabitat(state, "C", "Cliffs, rocks, walls, ravines, boulders", "Cliffs, rocks, walls, ravines, boulders", FloraHellenicaTransformer.uuidHabitatC);
        addHabitat(state, "G", "Temperate and submediterranean Grasslands", "Temperate and submediterranean Grasslands (lowland to montane dry and mesic meadows and pastures, rock outcrops and stony ground, grassy non-ruderal verges and forest edges)", FloraHellenicaTransformer.uuidHabitatG);
        addHabitat(state, "H", "High mountain vegetation", "High mountain vegetation (subalpine and alpine grasslands, screes and rocks, scrub above the treeline)", FloraHellenicaTransformer.uuidHabitatH);
        addHabitat(state, "M", "Coastal habitats", "Coastal habitats (Marine waters and mudflats, salt marshes, sand dunes, littoral rocks, halo-nitrophilous scrub)", FloraHellenicaTransformer.uuidHabitatM);
        addHabitat(state, "P", "Xeric Mediterranean Phrygana and grasslands", "Xeric Mediterranean Phrygana and grasslands (Mediterranean dwarf shrub formations, annual-rich pastures and lowland screes)", FloraHellenicaTransformer.uuidHabitatP);
        addHabitat(state, "R", "Agricultural and Ruderal habitats", "Agricultural and Ruderal habitats (fields, gardens and plantations, roadsides and trampled sites, frequently disturbed and pioneer habitats)", FloraHellenicaTransformer.uuidHabitatR);
        addHabitat(state, "W", "Woodlands and scrub", "Woodlands and scrub (broadleaved and coniferous forest, riparian and mountain forest and scrub, hedges, shady woodland margins)", FloraHellenicaTransformer.uuidHabitatW);

        this.getVocabularyService().save(habitatVoc);
        return;
    }

    /**
     * @param state
     * @param string
     * @param uuidlifeformt
     */
    private void addHabitat(SimpleExcelTaxonImportState<CONFIG> state, String abbrev, String label, String desc, UUID uuidHabitat) {
        addState(state, abbrev, label, desc, uuidHabitat, habitatVoc);
    }

    /**
     * @param state
     * @return
     */
    @SuppressWarnings("unchecked")
    private void createAreasVoc(SimpleExcelTaxonImportState<CONFIG> state) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Areas";
        String description = "Areas as used in the Checklist of Greece";
        areasVoc = OrderedTermVocabulary.NewInstance(TermType.NamedArea,
                description, label, null, termSourceUri);
        areasVoc.setUuid(FloraHellenicaTransformer.uuidFloraHellenicaAreasVoc);
//        Representation rep = Representation.NewInstance("Estados Méxicanos", "Estados Méxicanos", null, Language.SPANISH_CASTILIAN());
//        areasVoc.addRepresentation(rep);

        //greece country
        String countryLabel = "Greece";
        greece = NamedArea.NewInstance(countryLabel, countryLabel, "GR");
        greece.setUuid(FloraHellenicaTransformer.uuidAreaGreece);
        greece.setIdInVocabulary("GR");
        greece.setSymbol("GR");
        areasVoc.addTerm(greece);
        //FIXME
//        addMapping(greece, xx "mex_adm0", "iso", "MEX");

        addArea(state, "IoI", "Ionian Islands", FloraHellenicaTransformer.uuidAreaIoI);
        addArea(state, "NPi", "North Pindos", FloraHellenicaTransformer.uuidAreaNPi);
        addArea(state, "SPi", "South Pindos", FloraHellenicaTransformer.uuidAreaSPi);
        addArea(state, "Pe", "Peloponnisos", FloraHellenicaTransformer.uuidAreaPe);
        addArea(state, "StE", "Sterea Ellas", FloraHellenicaTransformer.uuidAreaStE);
        addArea(state, "EC", "East Central Greece", FloraHellenicaTransformer.uuidAreaEC);
        addArea(state, "NC", "North Central Greece", FloraHellenicaTransformer.uuidAreaNC);
        addArea(state, "NE", "North-East Greece", FloraHellenicaTransformer.uuidAreaNE);
        addArea(state, "NAe", "North Aegean islands", FloraHellenicaTransformer.uuidAreaNAe);
        addArea(state, "WAe", "West Aegean islands", FloraHellenicaTransformer.uuidAreaWAe);
        addArea(state, "Kik", "Kiklades", FloraHellenicaTransformer.uuidAreaKik);
        addArea(state, "KK", "Kriti and Karpathos", FloraHellenicaTransformer.uuidAreaKK);
        addArea(state, "EAe", "East Aegean islands", FloraHellenicaTransformer.uuidAreaEAe);

        this.getVocabularyService().save(areasVoc);
        return;
    }

    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String abbrevLabel, String areaLabel, UUID uuid) {
        addArea(state, abbrevLabel, areaLabel, uuid, areaLabel);  //short cut if label and mapping label are equal
    }

    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String abbrevLabel, String areaLabel,
            UUID uuid, String mappingLabel) {
        addArea(state, abbrevLabel, areaLabel, uuid, mappingLabel, null);  //short cut if label and mapping label are equal
    }


    /**
     * @param state
     * @param string
     * @param uuidaguascalientes
     */
    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String abbrevLabel, String areaLabel, UUID uuid, String mappingLabel, Integer id1) {
        NamedArea newArea = NamedArea.NewInstance(
                areaLabel, areaLabel, abbrevLabel);
        newArea.setIdInVocabulary(abbrevLabel);
        newArea.setUuid(uuid);
        newArea.setPartOf(greece);
        newArea.setLevel(null);
        newArea.setType(NamedAreaType.NATURAL_AREA());
        areasVoc.addTerm(newArea);
        //FIXME
        if (id1 != null){
            addMapping(newArea, "mex_adm1", "id_1", String.valueOf(id1));
        }else if (mappingLabel != null){
            addMapping(newArea, "mex_adm1", "name_1", mappingLabel);
        }
    }

    private void addMapping(NamedArea area, String mapping_layer, String mapping_field, String abbrev) {
        GeoServiceAreaAnnotatedMapping mapping = (GeoServiceAreaAnnotatedMapping)this.getBean("geoServiceAreaAnnotatedMapping");
        GeoServiceArea geoServiceArea = new GeoServiceArea();
        geoServiceArea.add(mapping_layer, mapping_field, abbrev);
        mapping.set(area, geoServiceArea);
    }

}
