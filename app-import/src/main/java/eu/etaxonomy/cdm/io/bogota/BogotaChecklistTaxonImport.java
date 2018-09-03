// $Id$
/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.homotypicgroup.BasionymRelationCreator;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 21.04.2017
 *
 */
@Component
public class BogotaChecklistTaxonImport<CONFIG extends BogotaChecklistImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -884838817884874228L;
    private static final Logger logger = Logger.getLogger(BogotaChecklistTaxonImport.class);

    private static final String ID_COL = "#";
    private static final String AUTHOR = "Autor";
    private static final String NAME = "Nombre";
    private static final String GENUS = "Género";
    private static final String FAMILIA = "Familia";
    private static final String INFRASPECIFIC = "Taxones infraespecíficos";
    private static final String SINONIMOS = "Sinonimos";

    private static UUID rootUuid = UUID.fromString("d66eda18-4c11-4472-bfe8-f6cd5ed95c9f");
    private static UUID plantaeUuid = UUID.fromString("032fc183-eb4f-4f19-a290-28597a849096");

    @SuppressWarnings("unchecked")
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>> deduplicationHelper
           = (ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>>)ImportDeduplicationHelper.NewStandaloneInstance();

    private String lastGenus;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
    private BasionymRelationCreator basionymCreator = new BasionymRelationCreator();


    @Override
    protected String getWorksheetName() {
        return "Resultados Busqueda Avanzada";
    }

    private boolean isFirst = true;
    private TransactionStatus tx = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (isFirst){
            tx = this.startTransaction();
            isFirst = false;
        }

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        String noStr = getValue(record, ID_COL);

        //species
        TaxonNode taxonNode = makeTaxon(state, line, record, noStr);

        if (taxonNode != null){
            //synonyms
            makeSynonyms(state, record, line, taxonNode.getTaxon(), noStr);

            //infraspecific
            makeInfraSpecific(state, record, line, taxonNode, noStr);
        }else{
            logger.warn(line + "No taxon node given");
        }
    }


    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }

    /**
     * @param state
     * @param record
     * @param line
     * @param taxon
     */
    private void makeSynonyms(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record, String line,
            Taxon taxon, String noStr) {

        String synonymsStr = getValue(record, SINONIMOS);
        if (synonymsStr != null){
            String[] splits = synonymsStr.split(",");
            for(String split : splits){
                split = split.trim();
                boolean isMisapplied = split.contains("auct.") || split.contains(" sensu ");
                if (split.endsWith(" None")){
                    split = split.replace(" None", "").trim();
                }
                if (isMisapplied){
                    handleSingleMisapplied(state, split, line, taxon, noStr);
                }else{
                    handleSingleSynonym(state, split, line, taxon, noStr);
                }
            }
        }
        basionymCreator.invoke(taxon);
    }

    /**
     * @param state
     * @param trim
     * @param line
     * @param taxon
     * @param noStr
     */
    private void handleSingleMisapplied(SimpleExcelTaxonImportState<CONFIG> state, String nameStr, String line,
            Taxon taxon, String noStr) {
        Rank rank = Rank.SPECIES();
        String AUCT_NON = "auct. non ";
        String auctStr = nameStr.contains(AUCT_NON)? AUCT_NON: nameStr.endsWith("auct.")? "auct.": null;
        boolean auctRequired = false;
        if (auctStr == null){
            auctRequired = true;
            if (nameStr.endsWith("auct.colomb.")){
                nameStr = nameStr.replace(" auct.colomb.", "");
                auctStr = "auct.colomb.";
            }else if (nameStr.endsWith(" [auct.mult.non Sw.]")){
                nameStr = nameStr.replace(" [auct.mult.non Sw.]", "");
                auctStr = "[auct.mult.non Sw.]";
            }else if (nameStr.endsWith(" auct.pr.p.")){
                nameStr = nameStr.replace(" auct.pr.p.", "");
                auctStr = "auct.pr.p.";
            }else if (nameStr.contains(" sensu ")){
                logger.warn(line + "sensu not yet handled correctly:" +  nameStr);
                auctRequired = false;
            }else{
                auctRequired = false;
                logger.warn(line + "auct. not recognized: " + nameStr);
            }

        }else{
            nameStr = nameStr.replace(auctStr, "").trim();
        }
        IBotanicalName name = (IBotanicalName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        name.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
        name = deduplicationHelper.getExistingName(state, name);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Misapplied name could not be parsed: " + nameStr);
        }
        deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

        Taxon misApp = Taxon.NewInstance(name, null);
        if (auctRequired){
            misApp.setAppendedPhrase(auctStr);
        }
        misApp.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
        taxon.addMisappliedName(misApp, state.getConfig().getSecReference(), null);
    }


    /**
     * @param col
     * @return
     */
    private String getNamespace() {
        return getWorksheetName()+"."+ ID_COL;
    }


    /**
     * @param state
     * @param record
     * @param line
     * @param taxon
     * @param noStr
     */
    private void handleSingleSynonym(SimpleExcelTaxonImportState<CONFIG> state, String nameStr,
            String line, Taxon taxon, String noStr) {
        Rank rank = Rank.SPECIES();
        IBotanicalName name = (IBotanicalName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        name.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
        name = deduplicationHelper.getExistingName(state, name);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Synonym could not be parsed: " + nameStr);
        }
        deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

        Synonym synonym = Synonym.NewInstance(name, getSecReference(state));
        synonym.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
        taxon.addSynonym(synonym, SynonymType.SYNONYM_OF());
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param taxon
     * @param noStr
     */
    private void makeInfraSpecific(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record, String line,
            TaxonNode speciesNode, String noStr) {
        String subSpeciesStr = getValue(record, INFRASPECIFIC);
        if (subSpeciesStr != null){
            String[] splits = subSpeciesStr.split(",");
            for(String split : splits){
                if (split.endsWith(" None")){
                    split = split.replace(" None", "").trim();
                }
                Rank rank = Rank.SUBSPECIES();
                IBotanicalName name = (IBotanicalName)parser.parseFullName(split.trim(), state.getConfig().getNomenclaturalCode(), rank);
                name.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
                name = deduplicationHelper.getExistingName(state, name);
                if (name.isProtectedTitleCache()){
                    logger.warn(line + "Infraspecific taxon could not be parsed: " + split.trim());
                }
                deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

                Taxon subSpecies = Taxon.NewInstance(name, getSecReference(state));
                subSpecies.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
                TaxonNode subSpeciesNode = speciesNode.addChildTaxon(subSpecies, getSecReference(state), null);
                getTaxonNodeService().save(subSpeciesNode);
            }
        }
    }

    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private TaxonNode makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record,
            String noStr) {

        TaxonNode familyTaxon = getFamilyTaxon(record, state);
        if (familyTaxon == null){
            logger.warn(line + "Family not created: " + record.get(FAMILIA));
        }

        String genusStr = getValue(record, GENUS);
        String nameStr = getValue(record, NAME);
        String speciesAuthorStr = getValue(record, AUTHOR);

        nameStr = CdmUtils.concat(" ", nameStr, speciesAuthorStr);
        Rank rank = Rank.SPECIES();
        IBotanicalName name = (IBotanicalName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        name.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);
        name = deduplicationHelper.getExistingName(state, name);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + nameStr);
        }
        deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));

        taxon.addImportSource(noStr, getNamespace(), getSourceCitation(state), null);

        String parentStr = genusStr;
        boolean genusAsBefore = genusStr.equals(lastGenus);
        TaxonNode parent = getParent(state, parentStr);
        TaxonNode newNode;
        if (parent != null){
            if (genusAsBefore ){
                //everything as expected
                newNode = parent.addChildTaxon(taxon, getSecReference(state), null);
                getTaxonNodeService().save(newNode);
            }else{
                logger.warn(line + "Unexpected non-missing parent");
                newNode = null;
            }
        }else{
            if (genusAsBefore){
                logger.warn(line + "Unexpected missing genus parent");
                newNode = null;
            }else{
                parent = makeGenusNode(state, record, genusStr);
                newNode = parent.addChildTaxon(taxon, getSecReference(state), null);
                getTaxonNodeService().save(newNode);
            }
        }

        this.lastGenus = genusStr;
        return newNode;
    }

    /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        String familyStr = getValue(record, FAMILIA);
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
            Reference sec = getSecReference(state);
            family = Taxon.NewInstance(name, sec);

            ITaxonTreeNode classificationNode = getClassification(state);
            familyNode = classificationNode.addChildTaxon(family, sec, null);
            state.putHigherTaxon(familyStr, family);
            getTaxonNodeService().save(familyNode);
        }

        return familyNode;
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
            Taxon plantae = Taxon.NewInstance(plantaeName, sec);
            TaxonNode plantaeNode = classification.addChildTaxon(plantae, null, null);
            plantaeNode.setUuid(plantaeUuid);
            getClassificationService().save(classification);

            rootNode = plantaeNode;
        }
        return rootNode;
    }


    protected IBotanicalName makeFamilyName(SimpleExcelTaxonImportState<CONFIG> state, String famStr) {
        IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
        famStr = decapitalize(famStr);
        name.setGenusOrUninomial(famStr);
        name.addSource(makeOriginalSource(state));
        return name;
    }

    /**
     * @param state
     * @return
     */
    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance(getValue(state.getOriginalRecord(),ID_COL), getNamespace(), state.getConfig().getSourceReference());
    }

    /**
     * @param famStr
     * @return
     */
    private String decapitalize(String famStr) {
        String result = famStr.substring(0,1) + famStr.substring(1).toLowerCase();
        return result;
    }


    protected Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        return state.getConfig().getSecReference();
    }

    /**
     * @param state
     * @return
     */
    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        return state.getConfig().getSourceReference();
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
     * @param state
     * @param record
     * @param genusStr
     * @return
     */
    private TaxonNode makeGenusNode(SimpleExcelTaxonImportState<CONFIG> state,
            Map<String, String> record, String genusStr) {
        IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
        name.setGenusOrUninomial(genusStr);
        Taxon genus = Taxon.NewInstance(name, getSecReference(state));
        TaxonNode family = getFamilyTaxon(record, state);
        TaxonNode genusNode = family.addChildTaxon(genus, getSecReference(state), null);
        state.putHigherTaxon(genusStr, genus);
        genus.addSource(makeOriginalSource(state));
        getTaxonNodeService().save(genusNode);
        return genusNode;
    }

}
