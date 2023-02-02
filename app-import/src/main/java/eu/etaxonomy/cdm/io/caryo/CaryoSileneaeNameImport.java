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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.exceptions.StringNotParsableException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeNameImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 8931253645038029899L;
    private static final Logger logger = LogManager.getLogger();

    private static final String NOMEN_ID = "nomen_id";
    private static final String RANK = "rank";
    private static final String GENUS = "Genus";
    private static final String INFRAGEN_NAME = "Infragen_name";
    private static final String SPECIES = "Species";
    private static final String INFRASP_EPITHET = "Infrasp_epithet";
    private static final String AUTHORS = "Authors";

    private static final String BASIONYM_LINK = "Basionym_link";
    private static final String IPNI_ID = "IPNI_ID";
    private static final String NOTES = "Notes";
    private static final String NOMINVAL_FLAG = "NomInval_Flag";
    private static final String TYPE_SPECIMEN = "Type_specimen";
    private static final String TYPE_SPECIES_LINK = "TypeSpecies_link";

    //maybe not used
    private static final String PUBLICATION = "Publication";
    private static final String PUBL_DATE = "PublDate";

    //not used
    @SuppressWarnings("unused")
    private static final String HOMONYM_FLAG = "Homonym_Flag";
    @SuppressWarnings("unused")
    private static final String MISAPPLIED_FLAG = "Misapplied_Flag";
    @SuppressWarnings("unused")
    private static final String SynonymyReference_link = "SynonymyReference_link";

    private Map<Integer, UUID> nameMapping = new HashMap<>();

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

        Integer nomenId = Integer.valueOf(getValue(record, NOMEN_ID));
        String rankStr = getValue(record, RANK);
        String genusStr = getValue(record, GENUS);
        String infragenStr = getValue(record, INFRAGEN_NAME);
        String speciesStr = getValue(record, SPECIES);
        String infraspStr = getValue(record, INFRASP_EPITHET);
        String authorsStr = getValue(record, AUTHORS);
        String ipniId = getValue(record, IPNI_ID);
        String notes = getValue(record, NOTES);
        String nomInvalFlag = getValue(record, NOMINVAL_FLAG);
        String typeSpecimenStr = getValue(record, TYPE_SPECIMEN);

        String row = String.valueOf(line) + "("+nomenId+"): ";

        try {
            //create name
            Rank rank = state.getTransformer().getRankByKey(rankStr);
            TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);

            //fill simple
            name.setGenusOrUninomial(Ne(genusStr));
            name.setInfraGenericEpithet(Ne(infragenStr));
            name.setSpecificEpithet(Ne(speciesStr));
            name.setInfraSpecificEpithet(Ne(infraspStr));
            NonViralNameParserImpl.NewInstance().parseAuthors(name, authorsStr);

            //TODO ??publication + PublDate

            //ipni ID
            if (isNotBlank(ipniId)) {
                name.addIdentifier(ipniId, DefinedTerm.IDENTIFIER_NAME_IPNI());
            }

            //notes
            if (isNotBlank(notes)) {
                Annotation annotation = Annotation.NewDefaultLanguageInstance(notes);
                annotation.setAnnotationType(AnnotationType.TECHNICAL());
                name.addAnnotation(annotation);
            }

            //nominval flag
            if (nomInvalFlag != null && nomInvalFlag.trim().equalsIgnoreCase("yes")) {
                Reference ref = null;
                name.addStatus(NomenclaturalStatusType.INVALID(), ref, null);
            }

            //type specimen
            if (isNotBlank(typeSpecimenStr)) {
                DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(SpecimenOrObservationType.PreservedSpecimen);
                facade.setCollector(null);  //just to create field unit and gathering event
                facade.innerDerivedUnit().setTitleCache(typeSpecimenStr, true);
                SpecimenTypeDesignationStatus status = null; //TODO
                Reference ref = null;
                String originalInfo = null;
                name.addSpecimenTypeDesignation(facade.innerDerivedUnit(), status, ref, null, originalInfo, false, false);
                // save ??
            }


        } catch (UndefinedTransformerMethodException | StringNotParsableException e) {
            e.printStackTrace();
        }
    }

    private String Ne(String genusStr) {
        return CdmUtils.Ne(genusStr);
    }

    private TaxonName dedupliateNameParts(TaxonName name) {
        if (state.getConfig().isDoDeduplicate()){
            state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
        }
        return name;
    }

    private String getOtherAuthors(List<TaxonName> otherNames) {
        String result = "";
        for (TaxonName name : otherNames){
            result = CdmUtils.concat(";", result, name.getAuthorshipCache());
        }
        return result;
    }


    private void handleNomenclRemarkAndNameStatus(String nomenclaturalRemarks, String row, boolean isNewName, TaxonName name,
            List<NomenclaturalStatusType> statusTypes) {

        NomenclaturalStatusType remarkType = null;
        NomenclaturalStatusType statusType = statusTypes.isEmpty()? null: statusTypes.iterator().next();
        if (nomenclaturalRemarks == null){
           //nothing to do
        }else if (", nom. illeg.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.ILLEGITIMATE();
        }else if (", nom. cons.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.CONSERVED();
        }else if (", nom. nud.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.NUDUM();
        }else if (", nom. provis.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.PROVISIONAL();
        }else if (", nom. rej.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.REJECTED();
        }else if (", nom. subnud.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.SUBNUDUM();
        }else if (", nom. superfl.".equals(nomenclaturalRemarks)){
            remarkType = NomenclaturalStatusType.SUPERFLUOUS();
        }else if (", not validly publ.".equals(nomenclaturalRemarks)){
            statusTypes.add(NomenclaturalStatusType.INVALID());
        }else if (", opus utique oppr.".equals(nomenclaturalRemarks)){
            statusTypes.add(NomenclaturalStatusType.OPUS_UTIQUE_OPPR());
        }else {
            logger.warn(row + "Unhandled nomenclatural remark: " + nomenclaturalRemarks);
        }

        NomenclaturalStatusType kewType = remarkType != null? remarkType : statusType;
        if (isNewName){
            if(remarkType != null && statusType != null && !remarkType.equals(statusType)){
                logger.warn(row + "Kew suggests 2 different nom. status. types for new name. The status from nomenclatural_remarks was taken.");
            }
            if (kewType != null){
                name.addStatus(kewType, getSecRef(state), null);
            }
        }else{
            NomenclaturalStatusType existingType = null;
            if (!name.getStatus().isEmpty()){
                existingType = name.getStatus().iterator().next().getType();
            }
            if (existingType != null && kewType != null){
                if (!existingType.equals(kewType)){
                    logger.warn(row + "Existing name status "+existingType.getTitleCache()+" differs from Kew status " + kewType.getTitleCache() + ". Key status ignored");
                }
            }else if (existingType != null && kewType == null){
                logger.warn(row + "Info: Existing name has a name status "+existingType.getTitleCache()+" but Kew name has no status. Existing status kept.");
            }else if (existingType == null && kewType != null){
                if(remarkType != null && statusType != null && !remarkType.equals(statusType)){
                    logger.warn(row + "Existing name has no status while Kew name suggests a status (but 2 different status form status and nomenclatural_remarks field).");
                }else{
                    logger.warn(row + "Existing name has no status while Kew name suggests a status ("+kewType.getTitleCache()+"). Kew status ignored.");
                }
            }
        }
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        Map<String, String> record = state.getOriginalRecord();
        int line = state.getCurrentLine();

        Integer nomenId = Integer.valueOf(getValue(record, NOMEN_ID));
        Integer basionymId = getInt(getValue(record, BASIONYM_LINK));
        Integer typeSpeciesId = getInt(getValue(record, TYPE_SPECIES_LINK));

        String row = String.valueOf(line) + "("+nomenId+"): ";
        if ((line % 500) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        TaxonName name = getName(state, nomenId);
        if (name == null) {
            logger.warn(row + "Name does not exist");
            return;
        }

        //basionym
        if (basionymId != null) {
            TaxonName basionym = getName(state, basionymId);
            if (basionym == null) {
                logger.warn(row + "basionym does not exist");
            }else {
                name.addBasionym(basionym);
            }
        }

        //type name
        if (typeSpeciesId != null) {
            TaxonName typeSpecies = getName(state, typeSpeciesId);
            if (typeSpecies == null) {
                logger.warn(row + "typeSpecies does not exist");
            }else {
                Reference ref = null;
                NameTypeDesignationStatus status = null; //TODO
                name.addNameTypeDesignation(typeSpecies, ref, null, null, status, false);
            }
        }
    }
}