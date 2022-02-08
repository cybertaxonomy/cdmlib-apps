// $Id$
/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen.excel;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.homotypicgroup.BasionymRelationCreator;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Import for German red list checklist for plantae.
 * @author a.mueller
 * @since 13.06.2019
 */
@Component
public class RedListGefaesspflanzenTaxonExcelImport<CONFIG extends RedListGefaesspflanzenExcelImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -884838817884874228L;
    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenTaxonExcelImport.class);

    private static final String ID_COL = "SORT_ID";
    private static final String UUID_COL = "TAXON_UUID";
    private static final String SYN_FLAG_COL = "SYN_FLAG";
    private static final String VOLLNAME_COL = "VOLLNAME";
    private static final String WISS_NAME_COL = "WISS_NAME";
    private static final String AUTHOR_COL = "AUTOR";
    private static final String RANK_COL = "RANG";
    private static final String ZUSATZ_COL = "ZUSATZ";


    private static UUID rootUuid = UUID.fromString("235ae474-227f-438a-b132-4508053fcb1c");
    private static UUID plantaeUuid = UUID.fromString("31bd1b7c-245a-416d-b076-aa090c7469ce");

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
    private BasionymRelationCreator basionymCreator = new BasionymRelationCreator();


    @Override
    protected String getWorksheetName(CONFIG config) {
        return "Florenliste";
    }

    private boolean isFirst = true;
    private TransactionStatus tx = null;

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
        TaxonBase<?> taxon = makeTaxon(state, line, record, noStr);

        getTaxonService().save(taxon);
        saveNameRelations(taxon.getName());
    }


    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }




    /**
     * @param col
     * @return
     */
    private String getNamespace(CONFIG config) {
        return getWorksheetName(config)+"."+ ID_COL;
    }



    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private TaxonBase<?> makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record,
            String noStr) {

//        TaxonNode familyTaxon = getFamilyTaxon(record, state);
//        if (familyTaxon == null){
//            logger.warn(line + "Family not created: " + record.get(FAMILIA));
//        }

        String nameStr = getValue(record, WISS_NAME_COL);
        String authorStr = getValue(record, AUTHOR_COL);
        String synFlag = getValue(record, SYN_FLAG_COL);
        String uuidTaxon = getValue(record, UUID_COL);
        String vollName = getValue(record, VOLLNAME_COL);
        String zusatz = getValue(record, ZUSATZ_COL);

        String sensuStr;
        if (StringUtils.isNotEmpty(zusatz) && zusatz.startsWith("s. ")){
            sensuStr = zusatz.split(",")[0].trim();
        }else {
           sensuStr = null;
        }
        String nomStatusStr;
        if (StringUtils.isNotEmpty(zusatz) && !zusatz.trim().equals(sensuStr)){
            nomStatusStr = sensuStr == null? zusatz.trim():zusatz.split(",")[1].trim();
        }else{
            nomStatusStr = null;
        }


        nameStr = CdmUtils.concat(" ", nameStr, authorStr);
        boolean isAuct = nameStr.endsWith("auct.");
        nameStr = normalizeNameStr(nameStr);

        Rank rank = Rank.SPECIES();
        TaxonName name = (TaxonName)parser.parseFullName(nameStr, state.getConfig().getNomenclaturalCode(), rank);
        name.addImportSource(noStr, getNamespace(state.getConfig()), getSourceCitation(state), null);
        name = state.getDeduplicationHelper().getExistingName(name, true);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + nameStr);
        }

        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);

        TaxonBase<?> taxon;
        if ("1".equals(synFlag) || isAuct){
            taxon = Taxon.NewInstance(name, getSecReference(state));
        }else if ("b".equals(synFlag)||"x".equals(synFlag)){
            taxon = Synonym.NewInstance(name, getSecReference(state));
        }else{
            logger.warn("Unknown synFlag: " + synFlag);
            return null;
        }
        taxon.setUuid(UUID.fromString(uuidTaxon));
        if (isAuct){
            taxon.setAppendedPhrase("auct.");  //TODO
        }
        if (sensuStr != null){
            taxon.setAppendedPhrase(sensuStr);
        }

        taxon.addImportSource(noStr, getNamespace(state.getConfig()), getSourceCitation(state), null);

        checkVollname(state, taxon, vollName, sensuStr, isAuct);
        return taxon;
    }



    /**
     * @param state
     * @param taxon
     * @param sensuStr
     * @param isAuct
     * @param vollName
     */
    private void checkVollname(SimpleExcelTaxonImportState<CONFIG> state, TaxonBase<?> taxon, String vollName, String sensuStr, boolean isAuct) {
        TaxonName name = taxon.getName();
        String titleCache = (sensuStr == null && !isAuct) ? name.getTitleCache() : taxon.getTitleCache();
        vollName = vollName.replace(" agg.", " aggr.").replace(" (E)", "");
        if (!titleCache.equals(vollName)){
            logger.warn("Vollname weicht ab: " + vollName +" <-> " +  titleCache);
        }
    }


    /**
     * @param nameStr
     */
    private String normalizeNameStr(String nameStr) {
        String result = nameStr.replace(" agg.", " aggr.").replaceAll(" auct.$", "")
                .replaceAll(" grex ", " subsp. ").replaceAll(" sublusus ", " subsp. ");
        return result;
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


//    protected IBotanicalName makeFamilyName(SimpleExcelTaxonImportState<CONFIG> state, String famStr) {
//        IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
//        famStr = decapitalize(famStr);
//        name.setGenusOrUninomial(famStr);
//        name.addSource(makeOriginalSource(state));
//        return name;
//    }

    /**
     * @param state
     * @return
     */
    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance(getValue(state.getOriginalRecord(),ID_COL),
                getNamespace(state.getConfig()), state.getConfig().getSourceReference());
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

}
