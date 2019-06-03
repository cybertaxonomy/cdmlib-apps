/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.germanSL;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 25.11.2016
 *
 */
@Component
public class GermanSLTaxonRelationImport extends GermanSLTaxonImport {

    private static final long serialVersionUID = 3381597141845204995L;

    private static final Logger logger = Logger.getLogger(GermanSLTaxonRelationImport.class);

    public static final String TAXON_NAMESPACE = "1.3.4";

    @Override
    protected String getWorksheetName(GermanSLImportConfigurator config) {
        return "1.3.4";
    }

    private Classification classification;
    private Set<TaxonNode> nodesToSave = new HashSet<>();
    private int count = 0;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        count++;
        Map<String, String> record = state.getOriginalRecord();
        String line = state.getCurrentLine() + ": ";

        String parentStr = getValue(record, GermanSLTaxonImport.AGG);
        String acceptedStr = getValue(record, GermanSLTaxonImport.VALID_NR);
        String idStr = getValue(record, GermanSLTaxonImport.SPECIES_NR);
        String statusStr = getValue(record, GermanSLTaxonImport.SYNONYM);
        NameResult nameResult = makeName (line, record, state);

        Classification classification = getClassification(state);
        TaxonBase<?> taxonBase = GermanSLTaxonImport.taxonIdMap.get(idStr);
        Taxon parent;
        TaxonNode taxonNode = null;
        if (isAccepted(statusStr, nameResult)){
            TaxonBase<?> parentTmp = GermanSLTaxonImport.taxonIdMap.get(parentStr);
            if (parentTmp == null){
                logger.warn(line + "Parent is missing: "+ parentStr);
            }else if (parentTmp.isInstanceOf(Synonym.class)){
                logger.warn(line + "Parent is not of type Taxon");
            }else{
                parent = (Taxon)parentTmp;
                Taxon taxon = (Taxon)taxonBase;
                Reference relRef = null;  //TODO
                if ("0".equals(idStr)){
                    taxonNode = classification.addChildTaxon(taxon, relRef, null);
                }else{
                    taxonNode = classification.addParentChild(parent, taxon, relRef, null);
                }

            }
        } else {
            TaxonBase<?> parentTmp = GermanSLTaxonImport.taxonIdMap.get(acceptedStr);
            if (parentTmp == null){
                logger.warn(line + "Accepted taxon is missing: " + acceptedStr);
            }else if (parentTmp.isInstanceOf(Synonym.class)){
                logger.warn(line + "Accepted taxon is not of type Taxon");
            }else{
                parent = (Taxon)parentTmp;
                Synonym synonym = (Synonym)taxonBase;
                parent.addSynonym(synonym, SynonymType.SYNONYM_OF());
            }
        }
        if (taxonNode != null){
            nodesToSave.add(taxonNode);
        }
        if ((count % 1) == 0){
            count = 0;
            getTaxonNodeService().saveOrUpdate(nodesToSave);
            nodesToSave = new HashSet<>();
        }
    }


    boolean needsFinalSave = true;
    /**
     * {@inheritDoc}
     */
    @Override
    protected void secondPass(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        if (needsFinalSave){
            getTaxonNodeService().saveOrUpdate(nodesToSave);
            needsFinalSave = false;
        }
    }


    /**
     * @return
     */
    private Classification getClassification(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        if (classification == null){
            GermanSLImportConfigurator config = state.getConfig();
            classification = Classification.NewInstance(config.getClassificationName());
            classification.setUuid(config.getClassificationUuid());
            classification.setReference(config.getSecReference());
            getClassificationService().save(classification);
        }
        return classification;
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        return ! state.getConfig().isDoTaxa();
    }
}
