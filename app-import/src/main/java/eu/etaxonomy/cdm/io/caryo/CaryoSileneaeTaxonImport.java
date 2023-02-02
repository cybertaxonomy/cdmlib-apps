/**
* Copyright (C) 2020 EDIT
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
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeTaxonImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 5594951908819469636L;
    private static final Logger logger = LogManager.getLogger();

    private static final String ACCEPTED_TAXON_ID = "AcceptedTaxon_ID";
    private static final String NOMEN_LINK = "nomen_link";
    private static final String PARENT = "parent";

    private Map<String, UUID> taxonMapping = new HashMap<>();
    private Set<String> neglectedRecords = new HashSet<>();

    private SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        int line = state.getCurrentLine();
        if ((line % 500) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        Integer accTaxonId = Integer.valueOf(getValue(record, ACCEPTED_TAXON_ID));
        Integer nameLinkID = Integer.valueOf(getValue(record, NOMEN_LINK));

        String row = String.valueOf(line) + "("+accTaxonId+"): ";

        TaxonName name = getName(state, nameLinkID);

        if (name == null) {
            logger.warn(row + "Name does not exist");
            return;
        }

        Taxon taxon = Taxon.NewInstance(name, getSecRef(state));

        getTaxonService().saveOrUpdate(taxon);
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        int line = state.getCurrentLine();
        if ((line % 500) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        Integer accTaxonId = Integer.valueOf(getValue(record, ACCEPTED_TAXON_ID));
        Integer parentId = Integer.valueOf(getValue(record, PARENT));

        String row = String.valueOf(line) + "("+accTaxonId+"): ";

        Taxon taxon = getTaxon(state, accTaxonId);
        if (taxon == null) {
            logger.warn(row + "Taxon does not exist");
            return;
        }

        Taxon parent = getTaxon(state, parentId);
        if (parent == null) {
            logger.warn(row + "Taxon does not exist");
            return;
        }

        Classification classification = getClassification(state);

        //TODO ref needed?
        TaxonNode node = classification.addParentChild(parent, taxon, null, null);

        getTaxonNodeService().saveOrUpdate(node);
    }

    private Classification getClassification(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state2) {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean hasSameAcceptedTaxon(TaxonBase<?> taxonBase, TaxonBase<?> basionymTaxon) {
        if (taxonBase.isInstanceOf(Synonym.class)){
            taxonBase = CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
        }
        if (basionymTaxon.isInstanceOf(Synonym.class)){
            basionymTaxon = CdmBase.deproxy(basionymTaxon, Synonym.class).getAcceptedTaxon();
        }
        return taxonBase != null && basionymTaxon != null && taxonBase.equals(basionymTaxon);
    }

    private TaxonNode getFamily(){
        UUID uuid = UUID.fromString("0334809a-aa20-447d-add9-138194f80f56");
        TaxonNode aizoaceae = getTaxonNodeService().find(uuid);
        return aizoaceae;
    }

    private TaxonNode hybridParent(){
        UUID uuid = UUID.fromString("2fae0fa1-758a-4fcb-bb6c-a2bd11f40641");
        TaxonNode hybridParent = getTaxonNodeService().find(uuid);
        return hybridParent;
    }
    private TaxonNode unresolvedParent(){
        UUID uuid = UUID.fromString("accb1ff6-5748-4b18-b529-9368c331a38d");
        TaxonNode unresolvedParent = getTaxonNodeService().find(uuid);
        return unresolvedParent;
    }
}
