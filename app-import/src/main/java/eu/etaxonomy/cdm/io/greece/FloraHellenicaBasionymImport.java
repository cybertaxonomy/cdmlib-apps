/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.homotypicgroup.BasionymRelationCreator;

/**
 * Creates basionym relationships for all taxa.
 *
 * @author a.mueller
 * @since 03.04.2017
 */
@Component
public class FloraHellenicaBasionymImport<CONFIG extends FloraHellenicaImportConfigurator>
        extends CdmImportBase<CONFIG,SimpleExcelTaxonImportState<CONFIG>>{

    private static final long serialVersionUID = -7066421762179411758L;
    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    @Override
    protected void doInvoke(SimpleExcelTaxonImportState<CONFIG> state) {

        TransactionStatus tx = this.startTransaction();

        List<Taxon> list = getTaxonService().list(Taxon.class, null, null, null, null);
        for (Taxon taxon : list){
            BasionymRelationCreator creator = new BasionymRelationCreator();
            creator.invoke(taxon);
        }
        this.commitTransaction(tx);
    }

    @Override
    protected boolean doCheck(SimpleExcelTaxonImportState<CONFIG> state) {
        return false;
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return false;
    }
}