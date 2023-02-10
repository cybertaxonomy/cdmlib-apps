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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.DoubleResult;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
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
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeNameImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 8931253645038029899L;
    private static final Logger logger = LogManager.getLogger();

    private static final UUID uuidSileneaeInfoNameIdType = UUID.fromString("95ecbf6d-521d-447f-bae5-d82585ff3617");

    private static final String NOMEN_ID = "nomen_ID";
    private static final String RANK = "Rank";
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

    @Override
    protected String getWorksheetName(CaryoSileneaeImportConfigurator config) {
        return "Names";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        int line = state.getCurrentLine();
//        if ((line % 500) == 0){
//            newTransaction(state);
//            System.out.println(line);
//        }

        Map<String, String> record = state.getOriginalRecord();

        Integer nomenId = Integer.valueOf(getValue(record, NOMEN_ID));
        String rankStr = getValue(record, RANK);
        String genusStr = getValue(record, GENUS);
        String infragenStr = getValue(record, INFRAGEN_NAME);
        String speciesStr = getValue(record, SPECIES);
        String infraspStr = getValue(record, INFRASP_EPITHET);
        String publicationStr = getValue(record, PUBLICATION);
        String publDateStr = getValue(record, PUBL_DATE);
        String authorsStr = getValue(record, AUTHORS);
        String ipniId = getValue(record, IPNI_ID);
        String notes = getValue(record, NOTES);
        String nomInvalFlag = getValue(record, NOMINVAL_FLAG);
        String typeSpecimenStr = getValue(record, TYPE_SPECIMEN);

        String row = String.valueOf(line) + "("+nomenId+"): ";

        try {
            //create name
            Rank rank = state.getTransformer().getRankByKey(rankStr);
            if (rank == null) {
                logger.warn(row + "rank not recognized: " + rankStr);
            }
            TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);

            //fill simple
            name.setGenusOrUninomial(Ne(genusStr));
            name.setInfraGenericEpithet(Ne(infragenStr));
            if (speciesStr != null && speciesStr.startsWith("×")) {
                name.setBinomHybrid(true);
                speciesStr = speciesStr.replace("×", "").trim();
            }
            name.setSpecificEpithet(Ne(speciesStr));
            name.setInfraSpecificEpithet(Ne(infraspStr));
            try {
                NonViralNameParserImpl.NewInstance().parseAuthors(name, authorsStr);
            } catch (Exception e) {
                name.setAuthorshipCache(authorsStr, true);
                logger.warn(row + "authorship not parsable: " + authorsStr);
            }

            //publication
            publicationStr = normalizePublication(publicationStr);
            if (isNotBlank(publicationStr) || isNotBlank(publDateStr)) {
                DoubleResult<String, String> publ = new DoubleResult<>(publicationStr, publDateStr);
                origPublicationMap.put(nomenId, publ);
            }

            //ipni ID
            if (isNotBlank(ipniId)) {
                name.addIdentifier(ipniId, DefinedTerm.IDENTIFIER_NAME_IPNI());
            }

            //add ID
            DefinedTerm sileneaeInfoNameIdType = getIdentiferType(state,
                    uuidSileneaeInfoNameIdType, null, null, null, null);
            name.addIdentifier(nomenId.toString(), sileneaeInfoNameIdType);

            //notes
            if (isNotBlank(notes)) {
                handleNotes(name, notes, row);
            }

            //nom.inval flag
            if (nomInvalFlag != null && nomInvalFlag.trim().equalsIgnoreCase("yes")) {
                Reference ref = null;
                name.addStatus(NomenclaturalStatusType.INVALID(), ref, null);
            }

            //type specimen
            if (isNotBlank(typeSpecimenStr)) {
                DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(SpecimenOrObservationType.PreservedSpecimen);
                facade.setCollector(null);  //just to create field unit and gathering event
                facade.innerFieldUnit().setTitleCache("Field Unit for: " + typeSpecimenStr, true);
                facade.innerDerivedUnit().setTitleCache(typeSpecimenStr, true);
                SpecimenTypeDesignationStatus status = SpecimenTypeDesignationStatus.UNSPECIFIC();
                Reference ref = null;
                String originalInfo = null;
                name.addSpecimenTypeDesignation(facade.innerDerivedUnit(), status, ref, null, originalInfo, false, false);
                // save ??
            }

            Reference sourceRef = getSourceReference(state);
            name.addImportSource(nomenId.toString(), "Names.nomen_ID", sourceRef, "row " + String.valueOf(line));

            putToNameMap(nomenId, name);

        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
    }

    private String normalizePublication(String publicationStr) {
        if (isBlank(publicationStr)) {
            return null;
        }
        if ("-".equalsIgnoreCase(publicationStr)) {
            return null;
        }
        if ("?".equalsIgnoreCase(publicationStr)) {
            return null;
        }
        if ("??".equalsIgnoreCase(publicationStr)) {
            return null;
        }
        if ("none".equalsIgnoreCase(publicationStr)) {
            return null;
        }
        return publicationStr;
    }

    private String Ne(String genusStr) {
        return CdmUtils.Ne(genusStr);
    }

    private String handleNotes(TaxonName name, String notes, String row) {

        NomenclaturalStatusType remarkType = null;
        if (notes == null){
           //nothing to do
        }else if ("ined".equals(notes)){
            remarkType = NomenclaturalStatusType.INED();
            notes = null;
        }else if ("nom. utique rej.".equals(notes)){
            remarkType = NomenclaturalStatusType.UTIQUE_REJECTED();
            notes = null;
        }else if ("nomen nudum".equalsIgnoreCase(notes)){
            remarkType = NomenclaturalStatusType.NUDUM();
            notes = null;
        }else if (notes.startsWith("nomen nudum") || notes.startsWith("Nomen nudum") ){
            remarkType = NomenclaturalStatusType.NUDUM();
        }else if (notes.startsWith("nom. illeg.")){
            remarkType = NomenclaturalStatusType.ILLEGITIMATE();
        }else if (notes.startsWith("nom. inval")){
            remarkType = NomenclaturalStatusType.INVALID();
        }else if ("Nom. rej.".equals(notes)){
            remarkType = NomenclaturalStatusType.REJECTED();
            notes = null;
        }

        //annotation
        if (isNotBlank(notes)) {
            Annotation annotation = Annotation.NewDefaultLanguageInstance(notes);
            annotation.setAnnotationType(AnnotationType.TECHNICAL());
            name.addAnnotation(annotation);
        }

        //nom. status.
        if (remarkType != null) {
            if (!name.hasStatus(remarkType)) {

                Reference ref = null;
                name.addStatus(remarkType, ref, null);
                if (name.getStatus().size() > 1) {
                    logger.warn(row + "name has >1 status: " + name.getFullTitleCache());
                }
            }
        }

        return notes;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        Map<String, String> record = state.getOriginalRecord();
        int line = state.getCurrentLine();

        Integer nomenId = Integer.valueOf(getValue(record, NOMEN_ID));
        Integer basionymId = getInt(getValue(record, BASIONYM_LINK));
        Integer typeSpeciesId = getInt(getValue(record, TYPE_SPECIES_LINK));

        String row = String.valueOf(line) + "("+nomenId+"): ";
//        if ((line % 500) == 0){
//            newTransaction(state);
//            System.out.println(line);
//        }

        TaxonName name = getName(nomenId);
        if (name == null) {
            logger.warn(row + "Name does not exist");
            return;
        }

        //basionym
        if (basionymId != null && !basionymId.equals(nomenId)) {
            TaxonName basionym = getName(basionymId);
            if (basionym == null) {
                logger.warn(row + "basionym does not exist");
            }else {
                name.addBasionym(basionym);
            }
        }

        //type name
        if (typeSpeciesId != null) {
            TaxonName typeSpecies = getName(typeSpeciesId);
            if (typeSpecies == null) {
                logger.warn(row + "typeSpecies does not exist");
            }else {
                Reference ref = null;
                NameTypeDesignationStatus status = null; // NameTypeDesignationStatus.NOT_APPLICABLE(); //TODO minor NameTypeDesignationStatus
                name.addNameTypeDesignation(typeSpecies, ref, null, null, status, false);
            }
        }
    }
}