/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 08.03.2023
 */
@Component
public class EuroMedEuroMossesGenusImport<CONFIG extends EuroMedEuroMossesImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -7521286086780236020L;
    private static final Logger logger = LogManager.getLogger();

    private static final String GENUS = "Genus";
    private static final String SERIAL = "Serial";
    private static final String NUMBER = "Number";
    private static final String AUTHORITY = "Authority";
    private static final String FAMILY = "Family";
    private static final String SUBFAMILY = "Subfamily";
    @SuppressWarnings("unused")
    private static final String NOTES = "Notes";


    @Override
    protected String getWorksheetName(CONFIG config) {
        return "Genera";
    }

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
    private ImportDeduplicationHelper dedupHelper;

    private boolean isFirst = true;
    private TransactionStatus tx = null;


    private Map<String,TaxonNode> familyNodeMap = new HashMap<>();

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (isFirst){
            tx = this.startTransaction();
            isFirst = false;
        }

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        TaxonName genusName = makeName(state, line, record);
        Taxon taxon = Taxon.NewInstance(genusName, getSecRef(state));
        taxon.setPublish(false);

        TaxonNode parent = findParentNode(state, record, familyNodeMap, line);
        if (parent != null) {
            TaxonNode childNode = parent.addChildTaxon(taxon, null, null); //E+M taxon nodes usually do not have a citation
            getTaxonNodeService().saveOrUpdate(childNode);
        }

        addImportSource(state, taxon);
    }

    private Reference refEuroMed;
    private Reference getSecRef(SimpleExcelTaxonImportState<CONFIG> state) {

        if (refEuroMed == null) {
            UUID secUuid = state.getConfig().getSecUuid();
            refEuroMed = getReferenceService().find(secUuid);
            if(refEuroMed == null){
                logger.warn("refEuroMed not found!");
            }
        }
        return refEuroMed;
    }

    private void addImportSource(SimpleExcelTaxonImportState<CONFIG> state, ISourceable<?> sourceable) {
        sourceable.addImportSource(state.getOriginalRecord().get(NUMBER), "Genera.Number", getSourceReference(state), null);
    }


    private TaxonName makeName(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {

        String genusStr = getValue(record, GENUS);
        String authority = getValue(record, AUTHORITY);
        String serialStr = getValue(record, SERIAL);

        String fullStr = genusStr + " " + authority;

        TaxonName name = parser.parseReferencedName(fullStr, NomenclaturalCode.ICNAFP, Rank.GENUS());

        if (name.hasProblem()) {
            logger.warn(line + "Problem with parsing: " + fullStr);
        }
        if (name.getNomenclaturalReference() != null && name.getNomenclaturalReference().hasProblem()) {
            logger.warn(line + "Problem with parsing reference for: " + fullStr);
        }

        //deduplicate authors and references
        getDedupHelper(state).replaceAuthorNamesAndNomRef(name);

        addImportSource(state, name);
        IdentifierType idType = this.getIdentiferType(state, EuroMedEuroMossesImportConfigurator.uuidEuroMossSerial,
                "EuroMoss Serial", "EuroMoss Serial", "EM ser.", null);
        name.addIdentifier(serialStr, idType);
        return name;
    }

    private TaxonNode findParentNode(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record,
            Map<String, TaxonNode> familyNodeMap, String line) {

        String genusStr = getValue(record, GENUS);
        String familyStr = getValue(record, FAMILY);
        String subfamilyStr = getValue(record, SUBFAMILY);

        String parentStr = isBlank(subfamilyStr) ? familyStr : subfamilyStr;
        if (familyNodeMap.get(parentStr) != null) {
            return familyNodeMap.get(parentStr);
        }

        MatchingTaxonConfigurator matchConfig = MatchingTaxonConfigurator.NewInstance();
        matchConfig.setTaxonNameTitle(parentStr);
        matchConfig.setSecUuid(state.getConfig().getSecUuid());
        List<TaxonBase> parentCandidates = getTaxonService().findTaxaByName(matchConfig);
        if (parentCandidates.isEmpty()) {
            logger.warn(line + "Parent " + parentStr + " not found for " + genusStr);
        } else if (parentCandidates.size() > 1) {
            logger.warn(line + ">1 parent candidates " + parentStr + " found for " + genusStr);
        } else if (!parentCandidates.iterator().next().isInstanceOf(Taxon.class)) {
            logger.warn(line + "Parent candidate " + parentStr + " is not accepted for " + genusStr);
        }
        Taxon parent = CdmBase.deproxy(parentCandidates.iterator().next(), Taxon.class);
        if (parent == null) {
            return null;
        }
        if (parent.getTaxonNodes().size() != 1) {
            logger.warn(line + "Parent " + parentStr + " has not exactly 1 node but " + parent.getTaxonNodes().size());
            return null;
        } else {
            TaxonNode parentNode = parent.getTaxonNodes().iterator().next();
            familyNodeMap.put(parentStr, parentNode);
            return parentNode;
        }
    }

    private ImportDeduplicationHelper getDedupHelper(SimpleExcelTaxonImportState<CONFIG> state) {
        if (dedupHelper == null) {
            dedupHelper = ImportDeduplicationHelper.NewInstance(this, state);
        }
        return this.dedupHelper;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }
}