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
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Language;
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
public class FloraHellenicaTaxonImport extends SimpleExcelTaxonImport<FloraHellenicaImportConfigurator>{
    private static final long serialVersionUID = -6291948918967763381L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaTaxonImport.class);


    private static UUID rootUuid = UUID.fromString("aa667b0b-b417-470e-a9b0-ef9409a3431e");
    private static UUID plantaeUuid = UUID.fromString("4f151932-ab97-4d81-b88e-46fe82cd3e88");

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "No","Family","Genus","Species","Species Author","Subspecies","Subspecies Author","IoI","NPi","SPi","Pe","StE","EC","NC","NE","NAe","WAe","Kik","KK","EAe","Stat","Ch","Lf","Hab A","Hab C","Hab G","Hab H","Hab M","Hab P","Hab R","Hab W","comment TR"
    });

    private String lastFamily;
    private String lastGenus;
    private String lastSpecies;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName() {
        return "6616 taxa";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        //Nicht unbedingt notwendig
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
            logger.warn("Name could not be parsed: " + nameStr);
        }
        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));
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
    private TaxonNode makeGenusNode(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
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
     * @return
     */
    private Reference getSecReference(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param state
     * @param parentStr
     * @return
     */
    private TaxonNode getParent(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state, String parentStr) {
        Taxon taxon = state.getHigherTaxon(parentStr);

        return taxon == null ? null : taxon.getTaxonNodes().iterator().next();
    }

    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(HashMap<String, String> record, SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
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
            ITaxonTreeNode rootNode = getClassification(state);
            familyNode = rootNode.addChildTaxon(family, sec, null);
            state.putHigherTaxon(familyStr, family);
            getTaxonNodeService().save(familyNode);
        }

        return familyNode;
    }

    /**
     * @param state
     * @param famStr
     * @return
     */
    private BotanicalName makeFamilyName(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state, String famStr) {
        BotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
        name.setGenusOrUninomial(famStr);
        name.addSource(makeOriginalSource(state));
        return name;
    }



    private TaxonNode rootNode;
    private TaxonNode getClassification(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
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


}
