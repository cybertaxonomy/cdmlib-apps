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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 14.12.2016
 */
public abstract class FloraHellenicaImportBase<CONFIG extends FloraHellenicaImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 2593130403213346396L;
    private static final Logger logger = LogManager.getLogger();

    private Map<UUID, Taxon> acceptedTaxonMap = new HashMap<>();
    private Reference sourceReference;
    private Reference secReference;
    private Reference secReference2;

    protected TaxonDescription getTaxonDescription(Taxon taxon) {
        if (!taxon.getDescriptions().isEmpty()){
            return taxon.getDescriptions().iterator().next();
        }else{
            TaxonDescription desc = TaxonDescription.NewInstance(taxon);
            desc.setDefault(true);
            return desc;
        }
    }

    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
        }
        return this.sourceReference;
    }

    protected Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference == null){
            this.secReference = getPersistentReference(state.getConfig().getSecReference());
        }
        return this.secReference;
    }

    protected Reference getSecReference2(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference2 == null){
            this.secReference2 = getPersistentReference(state.getConfig().getSecReference2());
        }
        return this.secReference2;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        if (result == null){
            result = reference;
        }
        return result;
    }

    protected Taxon getAcceptedTaxon(Map<String, String> record,
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
        }else{
            initAcceptedTaxonMap();
//            accTaxon = (Taxon)getTaxonService().find(accTaxon.getUuid());
            accTaxon = acceptedTaxonMap.get(accTaxon.getUuid());
        }
        return accTaxon;
    }

    private void initAcceptedTaxonMap() {
        if (acceptedTaxonMap.isEmpty()){
            List<Taxon> list = getTaxonService().list(Taxon.class, null, null, null, null);
            for (Taxon taxon : list){
                acceptedTaxonMap.put(taxon.getUuid(), taxon);
            }
        }
    }

    /**
     * @param record
     * @param state
     * @return
     */
    protected Taxon getHigherTaxon(Map<String, String> record,
            SimpleExcelTaxonImportState<CONFIG> state, String key) {

        String accStr = getValue(record, key);
        if (accStr == null){
            return null;
        }
        accStr = accStr.trim();

        Taxon accTaxon = state.getHigherTaxon(accStr);
        if (accTaxon == null){
            String message = state.getCurrentLine()+  ": Higher taxon could not be found: " + accStr;
            logger.info(message); //not critical
            return null;
        }else{
            initAcceptedTaxonMap();
//            accTaxon = (Taxon)getTaxonService().find(accTaxon.getUuid());
            accTaxon = acceptedTaxonMap.get(accTaxon.getUuid());
        }
        return accTaxon;
    }


    protected TaxonName makeFamilyName(SimpleExcelTaxonImportState<CONFIG> state,
            String famStr) {
        TaxonName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
        famStr = famStr.substring(0,1).toUpperCase() + famStr.substring(1).toLowerCase();
        name.setGenusOrUninomial(famStr);
        name.addSource(makeOriginalSource(state));
        return name;
    }

    protected TaxonName replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, TaxonName name, boolean parsed) {
        TaxonName result = state.getDeduplicationHelper().getExistingName(name, parsed);
        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(result);
        return result;
    }

    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, getSourceCitation(state));
    }

}
