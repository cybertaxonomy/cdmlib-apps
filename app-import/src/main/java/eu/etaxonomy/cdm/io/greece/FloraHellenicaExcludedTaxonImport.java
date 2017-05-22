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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
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
public class FloraHellenicaExcludedTaxonImport<CONFIG extends FloraHellenicaImportConfigurator>
            extends FloraHellenicaImportBase<CONFIG>{


    private static final long serialVersionUID = 2629253144140992196L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaExcludedTaxonImport.class);

    private static final String TAXON = "Taxon";
    private static final String UNIQUE_ID = "Unique ID";
    private static final String FAMILY = "Family";

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            UNIQUE_ID,FAMILY,TAXON
    });

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
    private TaxonNode excludedFamilyNode;

    @Override
    protected String getWorksheetName() {
        return "excluded taxa";
    }

    boolean isFirst = true;
    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }
        if (isFirst){
            System.out.println("Start excluded taxa");
            isFirst = false;
        }

        String noStr = getValue(record, UNIQUE_ID);
        TaxonNode taxonNode = makeTaxon(state, line, record, noStr);
        if (taxonNode != null){
            state.putTaxon(noStr, taxonNode.getTaxon());
        }
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private TaxonNode makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line,
            HashMap<String, String> record,
            String noStr) {

        TaxonNode familyTaxonNode = getFamilyTaxon(record, state);
        familyTaxonNode = getTaxonNodeService().find(familyTaxonNode.getUuid());
        if (familyTaxonNode == null){
            logger.warn(line + "Family not created, can't add excluded taxon: " + record.get(FAMILY));
            return null;
        }

        String taxonStr = getValue(record, TAXON);
        INonViralName name = parser.parseFullName(taxonStr, NomenclaturalCode.ICNAFP, null);
        name = replaceNameAuthorsAndReferences(state, name);
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + taxonStr);
        }

        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));
        taxon.addImportSource(noStr, getWorksheetName(), getSourceCitation(state), null);
        TaxonNode excludedNode = familyTaxonNode.addChildTaxon(taxon, getSecReference(state), null);
        excludedNode.setExcluded(true);
        getTaxonNodeService().saveOrUpdate(excludedNode);
        return excludedNode;
    }



   /**
     * @param record
     * @param state
     * @return
     */
    private TaxonNode getFamilyTaxon(HashMap<String, String> record,
            SimpleExcelTaxonImportState<CONFIG> state) {

        String familyStr = getValue(record, FAMILY);
        if (familyStr == null){
            return null;
        }
        familyStr = familyStr.trim();

//        Taxon family = state.getHigherTaxon(familyStr);
        Taxon family = this.getHigherTaxon(record, state, FAMILY);
        TaxonNode familyNode;
        if (family != null){
            familyNode = family.getTaxonNodes().iterator().next();
        }else{
            IBotanicalName name = makeFamilyName(state, familyStr);
            name = replaceNameAuthorsAndReferences(state, name);

            Reference sec = getSecReference(state);
            family = Taxon.NewInstance(name, sec);

            ITaxonTreeNode groupNode = getExcludedFamilyTaxon(state);
            familyNode = groupNode.addChildTaxon(family, sec, null);
            state.putHigherTaxon(familyStr, family);
            getTaxonNodeService().saveOrUpdate(familyNode);
//            logger.warn(state.getCurrentLine() +": " + "Family not found for excluded taxon");
        }
        return familyNode;
    }


    private ITaxonTreeNode getExcludedFamilyTaxon(
            SimpleExcelTaxonImportState<CONFIG> state) {

        if (excludedFamilyNode != null){
            return this.excludedFamilyNode;
        }
        Classification classification = getClassificationService().load(state.getConfig().getClassificationUuid());
        TaxonNode plantae = classification.getChildNodes().iterator().next();

        TaxonName name = TaxonNameFactory.NewBotanicalInstance(Rank.SUPERFAMILY());
        name.setTitleCache("Excluded", true);
        Taxon taxon = Taxon.NewInstance(name, getSecReference(state));
        excludedFamilyNode = plantae.addChildTaxon(taxon, getSourceCitation(state), null);
        excludedFamilyNode.setExcluded(true);
        getTaxonNodeService().saveOrUpdate(excludedFamilyNode);
        return excludedFamilyNode;
    }

}
