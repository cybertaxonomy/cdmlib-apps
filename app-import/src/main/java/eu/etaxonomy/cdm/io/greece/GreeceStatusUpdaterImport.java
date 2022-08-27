/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 08.12.2017
 */
@Component
public class GreeceStatusUpdaterImport
    	extends SimpleExcelTaxonImport<GreeceStatusUpdaterConfigurator>{

    private static final long serialVersionUID = 1017757693469612631L;
    private static final Logger logger = LogManager.getLogger();

    private static final UUID UUID_STATE_ALIEN_ESTABLISHED = UUID.fromString("0df082b1-d38a-455e-9406-8d5ca58c1df9");
    private static final UUID UUID_STATE_ALIEN_NON_ESTABLISHED = UUID.fromString("0642289d-3f7a-408f-b84f-335fb720d952");
    private static final UUID UUID_STATE_DOUBT_ALIEN_ESTABLISHEMENT_UNKNOWN = UUID.fromString("d2ff3b0c-d8b2-4f0c-9ed9-12455e22fb17");
    private static final UUID UUID_STATE_NATIVE_RANGE_RESTRICTED = UUID.fromString("6a646be7-df29-4594-93bb-af3fa1c83c72");
    private static final UUID UUID_STATE_NATIVE_NON_RANGE_RESTRICTED = UUID.fromString("fbfa2d04-5d3b-4e22-8c6e-6d1cb9902141");
    private static final UUID UUID_STATE_NATIVE_RANGE_RESTRICTION_UNKNWON = UUID.fromString("711d62a8-0763-4cd1-902a-f340612f8e8b");
    private static final UUID UUID_STATE_DOUBT_NATIVE_NON_RANGE_RESTRICTED = UUID.fromString("7291a253-3020-4207-807c-4dacae486b4e");

    private static final UUID UUID_IUCN_FEATURE = UUID.fromString("fc2b07a7-febe-4f8a-8713-da198e19bd69");
    private static final UUID UUID_IUCN_EXTINCT = UUID.fromString("4e2244ca-5858-4efb-9a24-57d18a24f1ef");
    private static final UUID UUID_IUCN_CRITICALLY_ENDANGERED = UUID.fromString("cbbde993-1acc-4dfb-bc3c-4f3a11592d3d");
    private static final UUID UUID_IUCN_ENDANGERED = UUID.fromString("39b0d998-9e4b-4ee6-990a-0b8b334f1c4d");
    private static final UUID UUID_IUCN_VULNERABLE = UUID.fromString("bd116f8d-2b1f-489b-9ba6-690fed6088aa");
    private static final UUID UUID_IUCN_NEAR_THREATENED = UUID.fromString("acb4f244-9fe0-4bcb-91e2-e9237c4f8165");
    private static final UUID UUID_IUCN_LEAST_CONCERN = UUID.fromString("40656435-0749-4ea6-ad10-071d8eddc332");
    private static final UUID UUID_IUCN_DATA_DEFICIENT = UUID.fromString("43518936-4bae-4fbc-a69b-5ccde7849bb3");
    private static final UUID UUID_IUCN_NOT_EVALUATED = UUID.fromString("ac0f2fe8-a7c9-4724-9d2e-cbe3d213d5d4");


    private static final UUID UUID_PD_FEATURE = UUID.fromString("3b08be3b-e3cc-41f2-8784-87414679f87d");
    private static final UUID UUID_PD_NO = UUID.fromString("7ba99117-fadc-40fe-8961-3f805ec8035a");
    private static final UUID UUID_PD_YES = UUID.fromString("60a5b995-b497-4813-9e7f-c1a49333b84f");
    private static final String PRESIDENTIAL_DECREE_67_81 = "Presidential Decree 67/81";
    private static final String STATUS = "Status";
    private static final String ESTABLISHMENT = "Establishment";
    private static final String RANGE_RESTRICTION = "Range-restriction";
    private static final String REDLIST_CATEGORY = "IUCN RedlistCategory";
    private static final String SCIENTIFIC_NAME = "scientificName";
    private static final String TAXON_UUID = "taxon uid";

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state) {

    	Map<String, String> record = state.getOriginalRecord();
        String line = getLine(state, 50);

    	String row = getValue(record, "line");
    	TaxonBase<?> taxonBase = getTaxon(state, record, line);
    	if (taxonBase == null){
    	    return;
    	}
    	Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);

    	//delete absent distribution status
    	Set<Distribution> distributions = taxon.getDescriptionItems(Feature.DISTRIBUTION(), Distribution.class);
    	for (Distribution distribution : distributions) {
    	    if (PresenceAbsenceTerm.ABSENT().equals(distribution.getStatus())) {
    	        logger.info("Remove distribution "+ distribution);
    	        distribution.getInDescription().removeElement(distribution);
    	    }
    	}

    	//delete old status
    	Set<CategoricalData> statuss = taxon.getDescriptionItems(Feature.STATUS(), CategoricalData.class);
        for (CategoricalData status : statuss) {
            if (Feature.uuidStatus.equals(status.getFeature().getUuid())) {
                logger.info("Remove status " + status);
                status.getInDescription().removeElement(status);
            }
        }

        //new data
        TaxonDescription newDescription = TaxonDescription.NewInstance(taxon);
        addOriginalSource(newDescription, row, "row", getSourceReference(state));
        String redListCategoryStr = getValue(record, REDLIST_CATEGORY);
        String presidentialDecreeStr = getValue(record, PRESIDENTIAL_DECREE_67_81);
        String statusStr = getValue(record, STATUS);
        String establishmentStr = getValue(record, ESTABLISHMENT);
        String rangeRestrictionStr = getValue(record, RANGE_RESTRICTION);

        CategoricalData presidentialDecree = getPresidentialDecree(state, line, presidentialDecreeStr);
        addOriginalSource(presidentialDecree, row, "row", getSourceReference(state));
        CategoricalData redListCategory = getRedListCategory(state, line, redListCategoryStr);
        addOriginalSource(redListCategory, row, "row", getSourceReference(state));
        CategoricalData newStatus = getNewStatus(state, line, statusStr, establishmentStr, rangeRestrictionStr);
        addOriginalSource(newStatus, row, "row", getSourceReference(state));
        newDescription.addElement(presidentialDecree);
        newDescription.addElement(redListCategory);
        newDescription.addElement(newStatus);
    }

    private TaxonBase<?> getTaxon(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state, Map<String, String> record, String line) {
        UUID taxonUuid = UUID.fromString(getValue(record, TAXON_UUID));
    	TaxonBase<?> taxonBase = getTaxonService().find(taxonUuid);
    	if (taxonBase == null) {
    		logger.warn(line + "no taxon " + taxonUuid);
    		String nameStr = getValue(state.getOriginalRecord(), SCIENTIFIC_NAME);
            TaxonName taxonName = (TaxonName)NonViralNameParserImpl.NewInstance().parseFullName(nameStr, NomenclaturalCode.ICNAFP, null);
    		taxonBase = Taxon.NewInstance(taxonName, null);
//    		return null;
    	}else if (!taxonBase.isInstanceOf(Taxon.class)) {
    		logger.warn(line + "is not accepted " + taxonUuid);
    		return null;
    	}
        if(!checkTaxonName(state, taxonBase, line)) {
            return null;
        }
        return taxonBase;
    }

    private CategoricalData getNewStatus(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state,
            String line, String statusStr, String establishmentStr, String rangeRestrictionStr) {

        CategoricalData result;
        State state2;
        Feature stateFeature = Feature.STATUS();
        if ("Alien".equalsIgnoreCase(statusStr) || "? Alien".equalsIgnoreCase(statusStr)) {
            if("Established".equalsIgnoreCase(establishmentStr)) {
                state2 = getStateTerm(state, UUID_STATE_ALIEN_ESTABLISHED);
            }else if("Non-established".equalsIgnoreCase(establishmentStr)) {
                state2 = getStateTerm(state, UUID_STATE_ALIEN_NON_ESTABLISHED);
            }else if("Establishment unknown".equalsIgnoreCase(establishmentStr)) {
                state2 = getStateTerm(state, UUID_STATE_DOUBT_ALIEN_ESTABLISHEMENT_UNKNOWN);
            }else{
                logger.warn(line+"Establishement not recognized: " + establishmentStr);
                state2 = null;
            }
        }else if ("Native".equalsIgnoreCase(statusStr)) {
            if("Range-Restricted".equalsIgnoreCase(rangeRestrictionStr)) {
                state2 = getStateTerm(state, UUID_STATE_NATIVE_RANGE_RESTRICTED);
            }else if("Non Range-Restricted".equalsIgnoreCase(rangeRestrictionStr)) {
                state2 = getStateTerm(state, UUID_STATE_NATIVE_NON_RANGE_RESTRICTED);
            }else if("Range-Restriction unknown".equalsIgnoreCase(rangeRestrictionStr)) {
                state2 = getStateTerm(state, UUID_STATE_NATIVE_RANGE_RESTRICTION_UNKNWON);
            }else{
                logger.warn(line+"Range restricted not recognized: " + rangeRestrictionStr);
                state2 = null;
            }
        }else if ("? Native".equalsIgnoreCase(statusStr)) {
            if("Non Range-Restricted".equalsIgnoreCase(rangeRestrictionStr)) {
                state2 = getStateTerm(state, UUID_STATE_DOUBT_NATIVE_NON_RANGE_RESTRICTED);
            }else{
                logger.warn(line+"Range restricted for doubtful native not recognized: " + rangeRestrictionStr);
                state2 = null;
            }
        }else {
            logger.warn(line+"Red list category not recognized: " + statusStr);
            state2 = null;
        }
        result = CategoricalData.NewInstance(state2, stateFeature);
        return result;
    }

    private CategoricalData getRedListCategory(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state,
            String line, String redListCategoryStr) {
        CategoricalData result;
        State state2;
        Feature redListCategoryFeature = getFeature(state, UUID_IUCN_FEATURE);
        if ("Extinct".equals(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_EXTINCT);
        }else if ("Critically Endangered".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_CRITICALLY_ENDANGERED);
        }else if ("Endangered".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_ENDANGERED);
        }else if ("Vulnerable".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_VULNERABLE);
        }else if ("Near Threatened".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_NEAR_THREATENED);
        }else if ("Least Concern".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_LEAST_CONCERN);
        }else if ("Data Deficient".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_DATA_DEFICIENT);
        }else if ("Not Evaluated".equalsIgnoreCase(redListCategoryStr)) {
            state2 = getStateTerm(state, UUID_IUCN_NOT_EVALUATED);
        }else {
            logger.warn(line+"Red list category not recognized: " + redListCategoryStr);
            state2 = null;
        }
        result = CategoricalData.NewInstance(state2, redListCategoryFeature);
        return result;
    }

    private CategoricalData getPresidentialDecree(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state,
            String line, String presidentialDecreeStr) {
        CategoricalData result;
        State state2;
        Feature presidentialDecreeFature = getFeature(state, UUID_PD_FEATURE);
        if ("No".equals(presidentialDecreeStr)) {
            state2 = getStateTerm(state, UUID_PD_NO);
        }else if ("Yes".equalsIgnoreCase(presidentialDecreeStr)) {
            state2 = getStateTerm(state, UUID_PD_YES);
        }else {
            logger.warn(line+"PresidentialDegree not recognized: " + presidentialDecreeStr);
            state2 = null;
        }
        result = CategoricalData.NewInstance(state2, presidentialDecreeFature);
        return result;
    }

    private boolean checkTaxonName(SimpleExcelTaxonImportState<GreeceStatusUpdaterConfigurator> state,
			TaxonBase<?> taxonBase, String line) {
		String nameStr = getValue(state.getOriginalRecord(), SCIENTIFIC_NAME);
		boolean equals = (nameStr.equals(taxonBase.getName().getTitleCache()));
		if (!equals) {
		    logger.warn(line + "Name does not match: " + nameStr + "<->" + taxonBase.getName().getTitleCache());
		}
		return true;
	}
}
