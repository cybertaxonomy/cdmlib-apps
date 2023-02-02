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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeNomRefImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 7227226331297614469L;
    private static final Logger logger = LogManager.getLogger();

    private static final String NOMEN_ID = "nomen_id";
    private static final String NAME = "name";
    private static final String PUBLICATION = "Publication";
    private static final String PUB_TYPE_ED = "PubTypeEd";
    private static final String PUB_TYPE_KEW = "PubTypeKew";
    private static final String PUB_KEW = "PubKew";
    private static final String NIMM_KEW = "NimmKew";
    private static final String ORIG_SPELLING = "Original spelling";
    private static final String NOM_STATUS = "Nom. Status";

    private static final String SECOND_PUBLICATION = "SecondPublication";
    private static final String IMPORT = "import";
    private static final String DUPL = "dupl";


    private Map<Integer, Reference> nameMapping = new HashMap<>();
    private Set<String> neglectedRecords = new HashSet<>();
    private Set<UUID> createdNames = new HashSet<>();

    private static final NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

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
        String nameStr = getValue(record, NAME);
        String origPublication = getValue(record, PUBLICATION);
        String pubTypeEd = getValue(record, PUB_TYPE_ED);
        String pubTypeKew = getValue(record, PUB_TYPE_KEW);
        String pubKew = getValue(record, PUB_KEW);

        String nimmKew = getValue(record, NIMM_KEW);
        String origSpelling = getValue(record, ORIG_SPELLING);
        //TODO erstmal nicht importieren laut NaK
        @SuppressWarnings("unused")
        String nomStatus = getValue(record, NOM_STATUS);

        String row = String.valueOf(line) + "("+nameStr+"): ";

        TaxonName name = getName(state, nomenId);
        if (name == null) {
            return;   //record did not exist
            //TODO check if it is really a duplicate
        }

        boolean isKew = isNotBlank(nimmKew) && "x".equals(nimmKew);

        String publication = isKew ? pubKew : origPublication;
        String pubType = isKew ? pubTypeEd : pubTypeKew;

        ReferenceType refType = getRefType(pubType);
        if (refType == null) {
            logger.warn(row + "reference type not found");
        }else if (refType == ReferenceType.Article) {
            publication = "in " + publication;
        }else if (refType == ReferenceType.Book) {
            //
        }else {
            logger.warn(row + "reference type not handled: " + refType);
        }
        String referenceName = name.getTitleCache()+ " " + publication;
        TaxonName parsedName = parser.parseReferencedName(referenceName);
        if (parsedName.isProtectedFullTitleCache() || parsedName.isProtectedTitleCache() ) {
            logger.warn(row + "name could not be parsed");
        }else {
            Reference ref = parsedName.getNomenclaturalReference();
            name.setNomenclaturalReference(ref);
            String microRef = parsedName.getNomenclaturalMicroReference();
            name.setNomenclaturalMicroReference(microRef);
        }

        //validateName (name);
        validateName(name, nameStr, row);
        //deduplicate
        dedupliateNameParts(name);

        //orig spelling
        if (isNotBlank(origSpelling)) {
            TaxonName origName = (TaxonName)parser.parseFullName(origSpelling);
            if (origName.isProtectedTitleCache()) {
                logger.warn(row + "orig name could not be parsed");
            }
            name.getNomenclaturalSource().setNameUsedInSource(origName);
        }
    }

    private void validateName(TaxonName name, String nameStr, String row) {
        if (name.getTitleCache().equals(nameStr)) {
            logger.warn(row+ "name titleCache does not match");
        }
    }

    private ReferenceType getRefType(String pubType) {
        if ("A".equals(pubType)){
            return ReferenceType.Article;
        }else if ("B".equals(pubType)) {
            return ReferenceType.Book;
        }
        return null;
    }

    private TaxonName dedupliateNameParts(TaxonName name) {
        if (state.getConfig().isDoDeduplicate()){
            state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
        }
        return name;
    }

    private void makeStatus(String status, String sourceId,
            String accId, String row, List<NomenclaturalStatusType> statusTypes) {

        Class<? extends CdmBase> clazz;
        if ("Accepted".equals(status) || "Unplaced".equals(status) || "Artificial Hybrid".equals(status) ){
            clazz = Taxon.class;
        }else if ("Synonym".equals(status) || "Orthographic".equals(status)){
            clazz = (accId == null)? Taxon.class : Synonym.class;
            if("Orthographic".equals(status)){
                statusTypes.add(NomenclaturalStatusType.SUPERFLUOUS());
            }
        }else if("Illegitimate".equals(status)){
            statusTypes.add(NomenclaturalStatusType.ILLEGITIMATE());
        }else if ("Invalid".equals(status)){
            statusTypes.add(NomenclaturalStatusType.INVALID());
        }else{
            logger.warn(row + "Unhandled status: " + status);
        }
        return;
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

    private void testParsedName(SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state, TaxonName name,
            String row, String fullCitation) throws UndefinedTransformerMethodException {
        Map<String, String> record = state.getOriginalRecord();

        String nameCache = getValue(record, NAME);
        String pubType = getValue(record, "PubType");
        String place_of_publication = getValue(record, "place_of_publication");
        String volume_and_page = getValue(record, "volume_and_page");

//        if (!CdmUtils.nullSafeEqual(name.getNameCache(), nameCache)){
//            logger.warn(row + "Unexpected nameCache: " + nameCache);
//        }
//        if (!CdmUtils.nullSafeEqual(name.getTitleCache(), fullName)){
//            logger.warn(row + "Unexpected titleCache: <->" + name.getTitleCache());
//        }
//        if (isBlank(genusHybrid) == name.isMonomHybrid()){
//            logger.warn(row + "Unexpected genus hybrid: " + genusHybrid);
//        }
//        if (!CdmUtils.nullSafeEqual(name.getGenusOrUninomial(),genus)){
//            logger.warn(row + "Unexpected genus: " + genus);
//        }if (isBlank(speciesHybrid) == name.isBinomHybrid()){
//            logger.warn(row + "Unexpected species hybrid: " + speciesHybrid);
//        }
//        if (!CdmUtils.nullSafeEqual(name.getSpecificEpithet(),species)){
//            logger.warn(row + "Unexpected species epithet: " + name.getSpecificEpithet() +"<->"+ species);
//        }
//        if (!CdmUtils.nullSafeEqual(name.getInfraSpecificEpithet(), infraspecies)){
//            logger.warn(row + "Unexpected infraspecific epithet: " + name.getInfraSpecificEpithet() +"<->"+ infraspecies);
//        }
//        if (!CdmUtils.nullSafeEqual(name.getAuthorshipCache(),authors)){
//            logger.warn(row + "Unexpected authors: " + name.getAuthorshipCache() +"<->"+ authors);
//        }
//        String combinationAndExAuthor = authorTitle(name.getCombinationAuthorship(), name.getExCombinationAuthorship());
//        if (!CdmUtils.nullSafeEqual(combinationAndExAuthor, combinationAuthor)){
//            logger.warn(row + "Unexpected combination author: " + combinationAndExAuthor +"<->"+ combinationAuthor);
//        }
//        String basionymAndExAuthor = authorTitle(name.getBasionymAuthorship(), name.getExBasionymAuthorship());
//        if (!CdmUtils.nullSafeEqual(basionymAndExAuthor, basionymAuthor)){
//            logger.warn(row + "Unexpected basionym author: " + basionymAndExAuthor +"<->"+ basionymAuthor);
//        }
//        Rank rank = state.getTransformer().getRankByKey(rankStr);
//        if (!rank.equals(name.getRank())){
//            logger.warn(row + "Unexpected rank: " + rankStr);
//        }
//
//        Reference nomRef = name.getNomenclaturalReference();
//        if (nomRef == null){
//            if (fullCitation != null){
//                NonViralNameParserImpl parser = new NonViralNameParserImpl();
//                TaxonName parsedName = parser.parseReferencedName(fullCitation, NomenclaturalCode.ICNAFP, rank);
//                if (parsedName.getNomenclaturalReference() != null){
//                    name.setNomenclaturalReference(parsedName.getNomenclaturalReference());
//                    logger.warn(row + "Nom.ref. was missing. Taken from Kew");
//                }else{
//                    logger.warn(row + "Nom. ref. is missing or can not be parsed");
//                }
//            }else{
//                logger.warn(row + "NomRef is missing.");
//            }
//        }else{
//            if ("A".equals(pubType) && nomRef.getType() != ReferenceType.Article){
//                logger.warn(row + "Unexpected nomref type: " + pubType + "<->" + nomRef.getType().toString());
//            }
//            if ("B".equals(pubType) && nomRef.getType() != ReferenceType.Book){
//                logger.warn(row + "Unexpected nomref type: " + pubType + "<->" + nomRef.getType().toString());
//            }
//            year = normalizeYear(year);
//            if (!CdmUtils.nullSafeEqual(year, nomRef.getDatePublishedString())){
//                logger.warn(row + "Unexpected year: " + year + "<->" + nomRef.getDatePublishedString());
//            }
//            if (volume_and_page != null && !name.getFullTitleCache().contains(volume_and_page)){
//                logger.warn(row + "volume_and_page not found in fullTitleCache: " + name.getFullTitleCache() +"<->"+ volume_and_page);
//            }
//            if (place_of_publication != null && !name.getFullTitleCache().contains(place_of_publication)){
//                logger.warn(row + "place_of_publication not found in fullTitleCache: " + name.getFullTitleCache() +"<->"+ place_of_publication);
//            }
//        }
//        if ("subsp.".equals(infraSpecRank) && !rank.equals(Rank.SUBSPECIES())){
//            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
//        }else if ("var.".equals(infraSpecRank) && !rank.equals(Rank.VARIETY())){
//            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
//        }else if ("f.".equals(infraSpecRank) && !rank.equals(Rank.FORM())){
//            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
//        }
    }

    private String authorTitle(TeamOrPersonBase<?> author, TeamOrPersonBase<?> exAuthor) {
        String authorStr = author == null? null: author.getNomenclaturalTitleCache();
        String exAuthorStr = exAuthor == null? null: exAuthor.getNomenclaturalTitleCache();
        return CdmUtils.concat(" ex ", exAuthorStr, authorStr);
    }

    private String normalizeYear(String year) {
        if (year == null){
            return null;
        }else if (year.contains("\" [")){
            String[] split = year.split("\" \\[");
            year = split[1].replace("]","") + " [" + split[0]+"\"]";
        }else if ("?".equals(year)){
            return null;
        }
        return year;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
//        Map<String, String> record = state.getOriginalRecord();
//        int line = state.getCurrentLine();
//        String accName = getValue(record, "AcceptedName");
//        String basionymId = getValue(record, "basionym_plant_name_id");
//        String homotypicSynonym = getValue(record, "homotypic_synonym");
//
//        String row = String.valueOf(line) + "("+fullName+"): ";
//
//        if ((line % 500) == 0){
//            newTransaction(state);
//            System.out.println(line);
//        }
    }
}