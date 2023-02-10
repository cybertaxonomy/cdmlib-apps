/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.etaxonomy.cdm.common.DoubleResult;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @date 02.02.2023
 */
public abstract class CaryoSileneaeImportBase extends SimpleExcelTaxonImport<CaryoSileneaeImportConfigurator> {

    private static final long serialVersionUID = -299606747509423614L;

    private Reference secRef = null;

    protected static Map<Integer,TaxonName> nameMap = new HashMap<>();

    private static Map<Integer,Taxon> taxonMap = new HashMap<>();

    protected static Map<Integer,TaxonName> origNameMap = new HashMap<>();
    protected static Map<Integer,TaxonName> orphanedNameMap = new HashMap<>();
    protected static Set<TaxonName> origSpellingNames = new HashSet<>();

    protected static Map<Integer, Integer> accIdMap = new HashMap<>();

    protected static Map<Integer,DoubleResult<String,String>> origPublicationMap = new HashMap<>();



    protected Taxon getTaxon(Integer taxonLinkId) {
        return taxonMap.get(taxonLinkId);
    }

    protected TaxonName getName(Integer nameLinkId) {
        return nameMap.get(nameLinkId);
    }

    protected void putToNameMap(Integer id, TaxonName name) {
        nameMap.put(id, name);
        origNameMap.put(id, name);
        orphanedNameMap.put(id, name);
    }
    protected void putToTaxonMap(Integer id, Taxon taxon) {
        taxonMap.put(id, taxon);
    }

    protected Reference getSecRef(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        if (secRef == null){
            secRef = getReferenceService().find(state.getConfig().getSecUuid());
        }
        return secRef;
    }

    protected void newTransaction(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        commitTransaction(state.getTransactionStatus());
        secRef = null;
        state.getDeduplicationHelper().reset();
        state.setSourceReference(null);
        System.gc();
        state.setTransactionStatus(startTransaction());
    }

    protected Integer getInt(String value) {
        return isBlank(value) ? null : Integer.valueOf(value);
    }
}
