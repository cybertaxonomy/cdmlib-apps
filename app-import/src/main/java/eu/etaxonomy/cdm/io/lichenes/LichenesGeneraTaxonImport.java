/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.lichenes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Lichenes genera taxon import.
 *
 * @author a.mueller
 * @since 10.03.2020
 */
@Component
public class LichenesGeneraTaxonImport<CONFIG extends LichenesGeneraImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = -6291948918967763381L;
    private static final Logger logger = Logger.getLogger(LichenesGeneraTaxonImport.class);

    private static UUID rootUuid = UUID.fromString("cdf58e08-b152-4f26-b2e4-24d62899e500");
    private static UUID plantaeUuid = UUID.fromString("339fb74a-27a0-4c61-95e4-ef89c73177cd");

   private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "Unique ID","uuid","Group","Family","Genus","Species","Species Author",
            "Subspecies","Subspecies Author",
            "A","C","G","H","M","P","R","W", "Taxon"
    });

    private Reference sourceReference;
    private Reference secReference;

    private String lastGenus;
    private String lastSpecies;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "valid taxa names";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

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

        state.putTaxon(noStr, taxon);
    }

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
        IBotanicalName name = (IBotanicalName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
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

    private String makeSpeciesKey(String genusStr, String speciesStr, String speciesAuthorStr) {
        return CdmUtils.concat(" ", new String[]{genusStr, speciesStr, speciesAuthorStr});
    }

    private TaxonNode makeGenusNode(SimpleExcelTaxonImportState<CONFIG> state,
            Map<String, String> record, String genusStr) {
        IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
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
            IBotanicalName name = makeFamilyName(state, familyStr);
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
            IBotanicalName name = makeFamilyName(state, groupStr);
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

            IBotanicalName plantaeName = TaxonNameFactory.NewBotanicalInstance(Rank.KINGDOM());
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

    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference == null){
            this.secReference = getPersistentReference(state.getConfig().getSecReference());
        }
        return this.secReference;
    }

    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
        }
        return this.sourceReference;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        if (result == null){
            result = reference;
        }
        return result;
    }

    protected <NAME extends INonViralName> NAME replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, NAME name) {
        NAME result = deduplicationHelper.getExistingName(state, name);
        deduplicationHelper.replaceAuthorNamesAndNomRef(state, result);
        return result;
    }
}
