/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

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

    protected Taxon getTaxon(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state, Integer taxonLinkId) {
        // TODO Auto-generated method stub
        return null;
    }

    protected TaxonName getName(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state, Integer nameLinkID) {
        // TODO Auto-generated method stub
        return null;
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
