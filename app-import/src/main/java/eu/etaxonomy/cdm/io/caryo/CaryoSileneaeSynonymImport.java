/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeSynonymImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 7967768097472488888L;
    private static final Logger logger = LogManager.getLogger();

    private static final String NOMTAX_ID = "NomTax_ID";
    private static final String NOMEN_LINK = "Nomen_link";
    private static final String TAXON_LINK = "Taxon_link";

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

        Integer nomTaxId = Integer.valueOf(getValue(record, NOMTAX_ID));
        Integer nameLinkID = Integer.valueOf(getValue(record, NOMEN_LINK));
        Integer taxonLinkId = Integer.valueOf(getValue(record, TAXON_LINK));


        String row = String.valueOf(line) + "("+nomTaxId+"): ";

        TaxonName name = getName(state, nameLinkID);

        if (name == null) {
            logger.warn(row + "Name does not exist");
            return;
        }

        Synonym synonym = Synonym.NewInstance(name, getSecRef(state));

        Taxon taxon = getTaxon(state, taxonLinkId);
        if (taxon == null) {
            logger.warn(row + "Taxon does not exist");
            return;
        }
        //TODO type (compute homotypics)
        taxon.addSynonym(synonym, SynonymType.SYNONYM_OF);
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
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
}