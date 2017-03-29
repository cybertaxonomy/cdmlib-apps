/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.HashMap;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @date 14.12.2016
 */

public abstract class FloraHellenicaImportBase<CONFIG extends FloraHellenicaImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 2593130403213346396L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaImportBase.class);


    /**
     * @param taxon
     * @return
     */
    protected TaxonDescription getTaxonDescription(Taxon taxon) {
        if (!taxon.getDescriptions().isEmpty()){
            return taxon.getDescriptions().iterator().next();
        }else{
            TaxonDescription desc = TaxonDescription.NewInstance(taxon);
            desc.setDefault(true);
            return desc;
        }
    }


    /**
     * @param state
     * @return
     */
    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        return state.getConfig().getSourceReference();
    }


    protected Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * @param record
     * @param state
     * @return
     */
    protected Taxon getAcceptedTaxon(HashMap<String, String> record,
            SimpleExcelTaxonImportState<CONFIG> state, String key) {
        String accStr = getValue(record, key);
        if (accStr == null){
            return null;
        }
        accStr = accStr.trim();

        Taxon accTaxon = state.getTaxon(accStr);
        if (accTaxon == null){
            String message = state.getCurrentLine()+  ": Accepted taxon could not be found: " + accStr;
            logger.warn(message);
            return null;
        }
        return accTaxon;
    }



    protected BotanicalName makeFamilyName(SimpleExcelTaxonImportState<CONFIG> state, String famStr) {
        BotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
        name.setGenusOrUninomial(famStr);
        name.addSource(makeOriginalSource(state));
        return name;
    }

}
