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

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @date 25.11.2016
 *
 */
@Component
public class GermanSLTaxonRelationImport<CONFIG extends GermanSLImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = 3381597141845204995L;

    private static final Logger logger = Logger.getLogger(GermanSLTaxonRelationImport.class);

    public static final String TAXON_NAMESPACE = "1.3.4";

    @Override
    protected String getWorksheetName() {
        return "1.3.4";
    }

    private Classification classification;
    private Set<TaxonBase> taxaToSave = new HashSet<>();
    private int count = 0;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        count++;
        Map<String, String> record = state.getOriginalRecord();
        String line = state.getCurrentLine() + ": ";

        String parentStr = getValue(record, GermanSLTaxonImport.AGG);
        String acceptedStr = getValue(record, GermanSLTaxonImport.VALID_NR);
        String idStr = getValue(record, GermanSLTaxonImport.SPECIES_NR);
        String statusStr = getValue(record, GermanSLTaxonImport.SYNONYM);

        Classification classification = getClassification(state);
        TaxonBase<?> taxonBase = GermanSLTaxonImport.taxonIdMap.get(idStr);
        Taxon parent;
        if (isAccepted(statusStr)){
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
                    classification.addChildTaxon(taxon, relRef, null);
                }else{
                    classification.addParentChild(parent, taxon, relRef, null);
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
        taxaToSave.add(taxonBase);
        if ((count % 1000) == 0){
            count = 0;
            getTaxonService().saveOrUpdate(taxaToSave);
            taxaToSave = new HashSet<>();
        }
    }


    private boolean isAccepted(String statusStr){
        if ("FALSE()".equals(statusStr) || "0".equals(statusStr) || "false".equalsIgnoreCase(statusStr)){
            return true;
        } else if ("TRUE()".equals(statusStr) || "1".equals(statusStr)|| "true".equalsIgnoreCase(statusStr)){
            return false;
        }else{
            logger.warn("Unhandled taxon status: " + statusStr);
            return false;
        }
    }


    /**
     * @param next
     * @return
     */
    private Taxon getAccepted(TaxonBase<?> taxonBase) {
        if (taxonBase.isInstanceOf(Taxon.class)){
            return CdmBase.deproxy(taxonBase, Taxon.class);
        }else{
            Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
            return syn.getAcceptedTaxon();
        }
    }


    boolean needsFinalSave = true;
    /**
     * {@inheritDoc}
     */
    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (needsFinalSave){
            getTaxonService().saveOrUpdate(taxaToSave);
            needsFinalSave = false;
        }
    }


    /**
     * @return
     */
    private Classification getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
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
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoTaxa();
    }
}