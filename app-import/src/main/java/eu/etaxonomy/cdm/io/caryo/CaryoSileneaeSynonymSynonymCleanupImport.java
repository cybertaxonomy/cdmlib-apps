/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.dto.IdentifiedEntityDTO;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeSynonymSynonymCleanupImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = -3721982716275962061L;
    private static final Logger logger = LogManager.getLogger();

    private static final UUID uuidSileneaeInfoNameIdType = UUID.fromString("95ecbf6d-521d-447f-bae5-d82585ff3617");

    private static final String NOMTAX_ID = "NomTax_ID";
    private static final String NOMEN_LINK = "Nomen_link";
    private static final String TAXON_LINK = "Taxon_link";

    @Override
    protected String getWorksheetName(CaryoSileneaeImportConfigurator config) {
        return "Synonyms";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        int line = state.getCurrentLine();
        if ((line % 500) == 0){
//            newTransaction(state);
            System.out.println(line);
        }

        Map<String, String> record = state.getOriginalRecord();

        Integer nomTaxId = Integer.valueOf(getValue(record, NOMTAX_ID));
        Integer nameLinkID = Integer.valueOf(getValue(record, NOMEN_LINK));
        Integer taxonLinkId = Integer.valueOf(getValue(record, TAXON_LINK));


        String row = String.valueOf(line) + "("+nomTaxId+"): ";

        //find synonym name
        TaxonName synonymName = getNameFromDb(state, nameLinkID, row);
        if (synonymName == null) {
            logger.warn(row + "Name does not exist");
            return;
        }

        Integer accNameId = accIdMap.get(taxonLinkId);
        if (accNameId == null) {
            logger.warn(row + "Taxon not found in taxon map: " + taxonLinkId);
            return;
        }
        TaxonName accName = getNameFromDb(state, accNameId, row);
        if (accName == null) {
            logger.warn(row + "Accepeted taxon name for synonym " + synonymName.getTitleCache() + " not found. Keep as unresolved.");
            return;
        }

        Taxon newAccTaxon;
        //add as synonym
        if (accName.getTaxa().size() != 1) {
            if (accName.getTaxa().isEmpty()){
                logger.warn(row + "Accepted taxon name "+accName.getTitleCache()+" has no taxon attached. Keep synonynm " + synonymName.getTitleCache() + "as unresolved");
                return;
            }else {
                String names = accName.getTaxa().stream().map(t->t.getTitleCache()).collect(Collectors.toList()).toString();
                logger.warn(row + "Acc taxon name has "+accName.getTaxa().size()+" taxa attached: n=" + accName.getTaxa().size() + " (" + names +")" );
                newAccTaxon = accName.getTaxa().iterator().next();
                logger.warn(row + " used: " + newAccTaxon.getTitleCache());
            }
        }else {
            newAccTaxon = accName.getTaxa().iterator().next();
        }

        Synonym synonym = Synonym.NewInstance(synonymName, getSecRef(state));
        newAccTaxon.addSynonym(synonym, SynonymType.SYNONYM_OF);

        getTaxonService().saveOrUpdate(newAccTaxon);


        //remove from parent
        if (synonymName.getTaxa().size() != 1) {
            logger.warn(row + "syn taxon name "+ synonymName.getTitleCache() + " has "+synonymName.getTaxa().size()+"  taxa attached");
            return;
        }
        Taxon accTaxon = synonymName.getTaxa().iterator().next();
        if (accTaxon.getTaxonNodes().size() != 1) {
            logger.warn(row + "syn taxon has "+accTaxon.getTaxonNodes().size()+" taxon nodes attached");
            return;
        }
        TaxonNode node = accTaxon.getTaxonNodes().iterator().next();

        //boolean success = node.getParent().deleteChildNode(node);
        node.setTaxon(null);
//        if (!success) {
//            logger.warn(row + "deleteChildNode not successful");
//        }

        accTaxon.setName(null);
        getTaxonService().delete(accTaxon);
//        getTaxonNodeService().delete(node);
    }


    private TaxonName getNameFromDb(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state, Integer nameId, String row) {
        try {
            IdentifierType sileneaeInfoNameIdType = getIdentiferType(state,
                    uuidSileneaeInfoNameIdType, null, null, null, null);
            Pager<IdentifiedEntityDTO<TaxonName>> list = getNameService().findByIdentifier(TaxonName.class, nameId.toString(),
                    sileneaeInfoNameIdType, MatchMode.EXACT, true, null, null, null);
            return list.getRecords().get(0).getCdmEntity().getEntity();
        } catch (Exception e) {
            logger.warn(row + "Error when getting name from DB. nameID = " + nameId);
            e.printStackTrace();
            return null;
        }
    }

    boolean first = true;
    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
//        if (first) {
//            for (TaxonName taxonName : orphanedNameMap.values()) {
//                Taxon taxon = Taxon.NewInstance(taxonName, getSecRef(state));
//                TaxonNode node = getUnresolvedNode(state).addChildTaxon(taxon, null);
//                getTaxonNodeService().saveOrUpdate(node);
//            }
//            first = false;
//        }
    }
//
//    private TaxonNode unresolvedNode;
//    private TaxonNode getUnresolvedNode(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
//        if (unresolvedNode == null) {
//            unresolvedNode = getTaxonNodeService().find(state.getConfig().getUnresolvedNodeUuid());
//        }
//        return unresolvedNode;
//    }

}