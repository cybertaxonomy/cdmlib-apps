/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.casearia;

import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 12.05.2020
 */
@Component
public class CaseariaDistributionImport extends SimpleExcelTaxonImport<CaseariaImportConfigurator>{

    private static final long serialVersionUID = -7841292708411771648L;

    private static final Logger logger = Logger.getLogger(CaseariaDistributionImport.class);

    private static final String NAME_CIT = "NameCit";
    private static final String IPNI_ID = "ipni_id";
    private static final String PLANT_NAME_ID = "plant_name_id";
    private static final String TAXON_RANK = "taxon_rank";

    private static final String CONTINENT_L1 = "continent_code_l1";
    private static final String CONTINENT_LABEL = "continent";
    private static final String REGION_L2 = "region_code_l2";
    private static final String REGION_LABEL = "region";
    private static final String AREA_L3 = "area_code_l3";
    private static final String AREA_LABEL = "area";
    private static final String INTRODUCED = "introduced";
    private static final String EXTINCT = "extinct";
    private static final String LOCATION_DOUBTFUL = "location_doubtful";

    private static final int RECORDS_PER_TRANSACTION = 500;

    private SimpleExcelTaxonImportState<CaseariaImportConfigurator> state;

    @Override
    protected String getWorksheetName(CaseariaImportConfigurator config) {
        return "OutputWDistribution";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state) {
        this.state = state;
        int line = state.getCurrentLine();
        if ((line % RECORDS_PER_TRANSACTION) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        Map<String, String> record = state.getOriginalRecord();

        String fullCitation = getValue(record, NAME_CIT);
        String ipniId = getValue(record, IPNI_ID);
        String sourceId = getValue(record, PLANT_NAME_ID);
//        String rankStr = getValue(record, TAXON_RANK);
        String continent_l1 = getValue(record, CONTINENT_L1);
        String continent_label = getValue(record, CONTINENT_LABEL);
        String region_l2 = getValue(record, REGION_L2);
        String region_label = getValue(record, REGION_LABEL);
        String area_l3 = getValue(record, AREA_L3);
        String area_label = getValue(record, AREA_LABEL);
        String introduced = getValue(record, INTRODUCED);
        String extinct = getValue(record, EXTINCT);
        String location_doubtful = getValue(record, LOCATION_DOUBTFUL);


        String fullNameStr = CdmUtils.concat(" ", "","");
        String row = String.valueOf(line) + "("+fullNameStr+"): ";

        UUID uuid = getTaxonMapping().get(sourceId);
        if (uuid == null){
            logger.warn(row + "Taxon uuid not found in taxon mapping for " + sourceId);
        }
        TaxonBase<?> taxonBase = getTaxonService().find(uuid);
        if (taxonBase == null){
            logger.warn(row + "Taxon " + sourceId + " for distribution "+area_label +" does not exist: " + fullCitation);
            return;
        }
        String taxonName = taxonBase.getName().getTitleCache();
        Taxon taxon;
        if (taxonBase.isInstanceOf(Synonym.class)){
            taxon = CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
            if (taxon == null){
                logger.warn(row + "Taxon "+taxonName+" for distribution "+area_label +" is synonym and synonym has no accepted taxon. Distribution"+area_label+"was ignored.");
                return;
            }else{
                logger.warn(row + "Taxon "+taxonName+" for distribution " + area_label + " is synonym. Distribution was moved to accepted taxon " + taxon.getName().getTitleCache());
            }
        }else {
            taxon = CdmBase.deproxy(taxonBase, Taxon.class);
        }
        @SuppressWarnings("unchecked")
        TermVocabulary<NamedArea> voc = getVocabularyService().find(NamedArea.uuidTdwgAreaVocabulary);
        NamedArea area = voc.getTermByIdInvocabulary(area_l3);
        if (area == null){
            logger.warn(row + "TDWG area not found: " + area_l3 + "; " + area_label);
        }else{
            if (taxon.getDescriptions().isEmpty()){
                TaxonDescription.NewInstance(taxon);
            }
            TaxonDescription desc = taxon.getDescriptions().iterator().next();
            PresenceAbsenceTerm status = PresenceAbsenceTerm.PRESENT();
            if (introduced.equals("1")){
                status = PresenceAbsenceTerm.INTRODUCED();
            }else if (extinct.equals("1")){
                //TODO extinct
                logger.warn(row + "Improve status to extinct");
                status = PresenceAbsenceTerm.ABSENT();
            }else if (location_doubtful.equals("1")){
                status = PresenceAbsenceTerm.PRESENT_DOUBTFULLY();
            }
            Distribution distribution = Distribution.NewInstance(area, status);
            desc.addElement(distribution);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, UUID> getTaxonMapping() {
        return (Map<String, UUID>)state.getStatusItem(CaseariaTaxonImport.TAXON_MAPPING);
    }

    private void newTransaction(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state) {
        commitTransaction(state.getTransactionStatus());
//        secRef = null;
//        dedupHelper = null;
        state.setSourceReference(null);
        System.gc();
        state.setTransactionStatus(startTransaction());
    }
}
