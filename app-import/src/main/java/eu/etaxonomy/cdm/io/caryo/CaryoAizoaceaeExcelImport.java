/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.config.SynonymDeletionConfigurator;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 17.02.2020
 */
@Component
public class CaryoAizoaceaeExcelImport extends SimpleExcelTaxonImport<CaryoAizoaceaeExcelImportConfigurator>{

    private static final long serialVersionUID = -729761811965260921L;
    private static final Logger logger = LogManager.getLogger();

    private static final String ACCEPTED_PLANT_NAME_ID = "accepted_plant_name_id";
    private static final String NOMENCLATURAL_REMARKS = "nomenclatural_remarks";
    private static final String TAXON_RANK = "taxon_rank";
    private static final String NAME_CIT = "NameCit";
    private static final String KEW_NAME4CDM_LINK = "KewName4CDMLink";
    private static final String KEW_F_NAME4CDM_LINK = "KewFName4CDMLink";
    private static final String TAXON_STATUS = "taxon_status";
    private static final String PLANT_NAME_ID = "plant_name_id";
    private static final String IPNI_ID = "ipni_id";

    private Map<String, UUID> taxonMapping = new HashMap<>();
    private Reference secRef = null;
    private Set<String> neglectedRecords = new HashSet<>();
    private Set<UUID> createdNames = new HashSet<>();

    private SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state) {
        int line = state.getCurrentLine();
        if ((line % 500) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        String fullCitation = getValue(record, NAME_CIT);
        String nameCache = getValue(record, KEW_NAME4CDM_LINK);
        String fullName = getValue(record, KEW_F_NAME4CDM_LINK);
        String status = getValue(record, TAXON_STATUS);
        String sourceId = getValue(record, PLANT_NAME_ID);
        String ipniId = getValue(record, IPNI_ID);
        String rankStr = getValue(record, TAXON_RANK);
        String nomenclaturalRemarks = getValue(record, NOMENCLATURAL_REMARKS);
        String accId = getValue(record, ACCEPTED_PLANT_NAME_ID);

        String row = String.valueOf(line) + "("+fullName+"): ";

        if("Misapplied".equals(status)){
            neglectedRecords.add(sourceId);
            return;
        }

        boolean isNewName = false;

        try {

            List<NomenclaturalStatusType> statusTypes = new ArrayList<>();
            Class<? extends CdmBase> clazz = makeStatus(status, sourceId, accId, row, statusTypes);

            TaxonName name;
            Rank rank = state.getTransformer().getRankByKey(rankStr);
            List<TaxonName> existingNames = getNameService().getNamesByNameCache(nameCache);
            Iterator<TaxonName> it = existingNames.iterator();
            while (it.hasNext()){
                TaxonName next = it.next();
                if (createdNames.contains(next.getUuid())){
                    it.remove();
                }
            }

            List<TaxonName> fullNameMatches = new ArrayList<>();

            @SuppressWarnings("rawtypes")
            List<TaxonBase> allFullNameTaxa = new ArrayList<>();
            @SuppressWarnings("rawtypes")
            List<TaxonBase> allNameCacheTaxa = new ArrayList<>();

            for (TaxonName existingName : existingNames){
                if (existingName.getTitleCache().equals(fullName)){
                    fullNameMatches.add(existingName);
                    allFullNameTaxa.addAll(existingName.getTaxonBases());
                }
                allNameCacheTaxa.addAll(existingName.getTaxonBases());
            }

            logMultipleCandidates(row, existingNames, fullNameMatches);

            TaxonBase<?> existingTaxon;
            if(allFullNameTaxa.size()>1){
                existingTaxon = findBestMatchingTaxon(allFullNameTaxa, clazz, row);
                name = existingTaxon.getName();
            }else if (allFullNameTaxa.size()==1){
                existingTaxon = allFullNameTaxa.iterator().next();
                name = existingTaxon.getName();
            }else{
                existingTaxon = null;
                if (!fullNameMatches.isEmpty()){
                    logger.warn(row + "None of the existing names exists as taxon/synonym. Existing name taken as base for new taxon/synonym created.");
                    if (fullNameMatches.size()>1){
                        logger.warn(row + "More than 1 matching full names exist as candidats for new taxon/synonym. Arbitrary one taken.");
                    }
                    name = fullNameMatches.iterator().next();
                }else if (!existingNames.isEmpty()){
                    if (!allNameCacheTaxa.isEmpty()){
                        logger.warn(row + "Taxa exist with matching nameCache but not matching fullname cache. New name and new taxon/synonym created. Other authors are " + getOtherAuthors(existingNames));
                        name = null;
                    }else{
                        logger.warn(row + "No matching fullnames exist but namecache matches. None of the matches is used in a taxon/synonym. Other authors are " + getOtherAuthors(existingNames));
                        name = null;
                    }
                }else{
                    name = null;
                }
            }

            if (existingTaxon == null){
                if (rank == null){
                    logger.warn(row + "Name has no rank " + nameCache);
                }else if (rank.equals(Rank.GENUS())){
                    logger.warn(row + "No name exists for genus " + nameCache + ". This is unexpected.");
                }
            }else{
                if (existingTaxon.isInstanceOf(Taxon.class)){
                    if (!CdmBase.deproxy(existingTaxon, Taxon.class).getTaxonNodes().isEmpty()){
                        neglectedRecords.add(sourceId);
                    }
                }else{
                    Taxon taxon = CdmBase.deproxy(existingTaxon, Synonym.class).getAcceptedTaxon();
                    if (taxon != null && !taxon.getTaxonNodes().isEmpty()){
                        neglectedRecords.add(sourceId);
                    }
                }
            }
            if (name == null){
                NonViralNameParserImpl parser = new NonViralNameParserImpl();
                name = parser.parseReferencedName(fullCitation, NomenclaturalCode.ICNAFP, rank);
                if (name.isProtectedFullTitleCache() || name.isProtectedTitleCache() || name.isProtectedNameCache()
                        || name.isProtectedAuthorshipCache()){
                    logger.warn(row + "Name not parsable: " + fullCitation);
                    name.setTitleCache(fullName, true);
                    name.setNameCache(nameCache, true);
                }else{
                    testParsedName(state, name, row, null);
                }
                name.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
                name = dedupliateNameParts(name);
                getNameService().saveOrUpdate(name);
                isNewName = true;
                createdNames.add(name.getUuid());
            }else{
                testParsedName(state, name, row, fullCitation);
            }

            handleNomenclRemarkAndNameStatus(nomenclaturalRemarks, row, isNewName, name, statusTypes);

            TaxonBase<?> taxonBase = existingTaxon;

            if (taxonBase == null){
                if (clazz == Taxon.class){
                    taxonBase = Taxon.NewInstance(name, getSecRef());
                }else{
                    taxonBase = Synonym.NewInstance(name, getSecRef());
                }
                taxonBase.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
                getTaxonService().saveOrUpdate(taxonBase);
            }

            if (!isBlank(ipniId)){
                DefinedTerm ipniIdIdentifierType = DefinedTerm.IDENTIFIER_NAME_IPNI();
                name.addIdentifier(ipniId, ipniIdIdentifierType);
            }else{
                logger.warn(row + "IPNI id is missing.");
            }

            taxonMapping.put(sourceId, taxonBase.getUuid());
//            if("Accepted".equals(status)){
            if(taxonBase.isInstanceOf(Taxon.class)){
                    UUID existingUuid = taxonMapping.put(name.getNameCache(), taxonBase.getUuid());
                if (existingUuid != null){
                    logger.warn(row + name.getNameCache() + " has multiple instances in file");
                }
            }
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
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

    private TaxonBase<?> findBestMatchingTaxon(@SuppressWarnings("rawtypes") List<TaxonBase> allFullNameTaxa,
            Class<? extends CdmBase> clazz, String row) {

        TaxonBase<?> result = null;
        TaxonBase<?> otherStatus = null;
        for (TaxonBase<?> taxonBase : allFullNameTaxa) {
            if (taxonBase.isInstanceOf(clazz)){
                if (result != null){
                    logger.warn(row + "More than 1 taxon with matching full name AND matching status exists. This is not further handled. Arbitrary one taken.");
                }
                result = taxonBase;
            }else{
                otherStatus = taxonBase;
            }
        }
        if (result == null && allFullNameTaxa.size()>1){
            logger.warn(row + "More than 1 taxon with matching fullname but NOT matching status exists. This is not further handled. Arbitrary one taken.");
        }
        return result == null? otherStatus :result ;
    }

    private void logMultipleCandidates(String row, List<TaxonName> existingNames, List<TaxonName> fullNameMatches) {
        if(fullNameMatches.size()>1){
            String message = row + "More than one name with matching full name exists in DB. Try to take best matching.";
            if (existingNames.size()>fullNameMatches.size()){
                message += " Additionally names with matching name cache exist.";
            }
            logger.warn(message);
        }else if (existingNames.size()>1){
            String message = row + "More than one name with matching nameCache exists in DB. ";
            if(fullNameMatches.isEmpty()){
                message += "But none matches full name.";
            }else{
                message += "But exactly 1 matches full name.";
            }
            logger.warn(message);
        }
    }

    private Class<? extends CdmBase> makeStatus(String status, String sourceId,
            String accId, String row, List<NomenclaturalStatusType> statusTypes) {

        Class<? extends CdmBase> clazz;
        if ("Accepted".equals(status) || "Unplaced".equals(status) || "Artificial Hybrid".equals(status) ){
            clazz = Taxon.class;
        }else if ("Synonym".equals(status) || "Orthographic".equals(status)){
            clazz = (accId == null)? Taxon.class : Synonym.class;
            if("Orthographic".equals(status)){
                statusTypes.add(NomenclaturalStatusType.SUPERFLUOUS());
//                addStatus(NomenclaturalStatusType.SUPERFLUOUS(), row, isNewName, statusAdded, statusTypes, null);
            }
        }else if("Illegitimate".equals(status)){
            clazz = getIllegInvalidStatus(sourceId, accId);
            statusTypes.add(NomenclaturalStatusType.ILLEGITIMATE());
//            addStatus(NomenclaturalStatusType.ILLEGITIMATE(), row, isNewName, statusAdded, statusTypes, getSecRef());
        }else if ("Invalid".equals(status)){
            clazz = getIllegInvalidStatus(sourceId, accId);
            statusTypes.add(NomenclaturalStatusType.INVALID());
//            addStatus(NomenclaturalStatusType.INVALID(), row, isNewName, statusAdded, statusTypes, getSecRef());
        }else{
            logger.warn(row + "Unhandled status: " + status);
            clazz = Taxon.class;  //to do something
        }
        return clazz;
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
                name.addStatus(kewType, getSecRef(), null);
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

    private void newTransaction(SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state) {
        commitTransaction(state.getTransactionStatus());
        secRef = null;
        state.getDeduplicationHelper().reset();
        state.setSourceReference(null);
        System.gc();
        state.setTransactionStatus(startTransaction());
    }

    private Reference getSecRef() {
        if (secRef == null){
            secRef = getReferenceService().find(state.getConfig().getSecUuid());
        }
        return secRef;
    }

    private Class<? extends CdmBase> getIllegInvalidStatus(String sourceId, String accId) {
        if (sourceId.equals(accId)){
            return Taxon.class;
        }else if(accId != null){
            return Synonym.class;
        }
        return null;
    }


    private void testParsedName(SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state, TaxonName name,
            String row, String fullCitation) throws UndefinedTransformerMethodException {
        Map<String, String> record = state.getOriginalRecord();

        String nameCache = getValue(record, KEW_NAME4CDM_LINK);
        String fullName = getValue(record, KEW_F_NAME4CDM_LINK);
        String rankStr = getValue(record, TAXON_RANK);
        String genusHybrid = getValue(record, "genus_hybrid");
        String genus = getValue(record, "genus");
        String speciesHybrid = getValue(record, "species_hybrid");
        String species = getValue(record, "species");
        String infraSpecRank = getValue(record, "infraspecific_rank");
        String infraspecies = getValue(record, "infraspecies");
        String basionymAuthor = getValue(record, "parenthetical_author");
        String combinationAuthor = getValue(record, "primary_author");
        String authors = getValue(record, "taxon_authors");
        String year = getValue(record, "KewYear4CDM");
        String pubType = getValue(record, "PubType");
        String place_of_publication = getValue(record, "place_of_publication");
        String volume_and_page = getValue(record, "volume_and_page");

        if (!CdmUtils.nullSafeEqual(name.getNameCache(), nameCache)){
            logger.warn(row + "Unexpected nameCache: " + nameCache);
        }
        if (!CdmUtils.nullSafeEqual(name.getTitleCache(), fullName)){
            logger.warn(row + "Unexpected titleCache: <->" + name.getTitleCache());
        }
        if (isBlank(genusHybrid) == name.isMonomHybrid()){
            logger.warn(row + "Unexpected genus hybrid: " + genusHybrid);
        }
        if (!CdmUtils.nullSafeEqual(name.getGenusOrUninomial(),genus)){
            logger.warn(row + "Unexpected genus: " + genus);
        }if (isBlank(speciesHybrid) == name.isBinomHybrid()){
            logger.warn(row + "Unexpected species hybrid: " + speciesHybrid);
        }
        if (!CdmUtils.nullSafeEqual(name.getSpecificEpithet(),species)){
            logger.warn(row + "Unexpected species epithet: " + name.getSpecificEpithet() +"<->"+ species);
        }
        if (!CdmUtils.nullSafeEqual(name.getInfraSpecificEpithet(), infraspecies)){
            logger.warn(row + "Unexpected infraspecific epithet: " + name.getInfraSpecificEpithet() +"<->"+ infraspecies);
        }
        if (!CdmUtils.nullSafeEqual(name.getAuthorshipCache(),authors)){
            logger.warn(row + "Unexpected authors: " + name.getAuthorshipCache() +"<->"+ authors);
        }
        String combinationAndExAuthor = authorTitle(name.getCombinationAuthorship(), name.getExCombinationAuthorship());
        if (!CdmUtils.nullSafeEqual(combinationAndExAuthor, combinationAuthor)){
            logger.warn(row + "Unexpected combination author: " + combinationAndExAuthor +"<->"+ combinationAuthor);
        }
        String basionymAndExAuthor = authorTitle(name.getBasionymAuthorship(), name.getExBasionymAuthorship());
        if (!CdmUtils.nullSafeEqual(basionymAndExAuthor, basionymAuthor)){
            logger.warn(row + "Unexpected basionym author: " + basionymAndExAuthor +"<->"+ basionymAuthor);
        }
        Rank rank = state.getTransformer().getRankByKey(rankStr);
        if (!rank.equals(name.getRank())){
            logger.warn(row + "Unexpected rank: " + rankStr);
        }

        Reference nomRef = name.getNomenclaturalReference();
        if (nomRef == null){
            if (fullCitation != null){
                NonViralNameParserImpl parser = new NonViralNameParserImpl();
                TaxonName parsedName = parser.parseReferencedName(fullCitation, NomenclaturalCode.ICNAFP, rank);
                if (parsedName.getNomenclaturalReference() != null){
                    name.setNomenclaturalReference(parsedName.getNomenclaturalReference());
                    logger.warn(row + "Nom.ref. was missing. Taken from Kew");
                }else{
                    logger.warn(row + "Nom. ref. is missing or can not be parsed");
                }
            }else{
                logger.warn(row + "NomRef is missing.");
            }
        }else{
            if ("A".equals(pubType) && nomRef.getType() != ReferenceType.Article){
                logger.warn(row + "Unexpected nomref type: " + pubType + "<->" + nomRef.getType().toString());
            }
            if ("B".equals(pubType) && nomRef.getType() != ReferenceType.Book){
                logger.warn(row + "Unexpected nomref type: " + pubType + "<->" + nomRef.getType().toString());
            }
            year = normalizeYear(year);
            if (!CdmUtils.nullSafeEqual(year, nomRef.getDatePublishedString())){
                logger.warn(row + "Unexpected year: " + year + "<->" + nomRef.getDatePublishedString());
            }
            if (volume_and_page != null && !name.getFullTitleCache().contains(volume_and_page)){
                logger.warn(row + "volume_and_page not found in fullTitleCache: " + name.getFullTitleCache() +"<->"+ volume_and_page);
            }
            if (place_of_publication != null && !name.getFullTitleCache().contains(place_of_publication)){
                logger.warn(row + "place_of_publication not found in fullTitleCache: " + name.getFullTitleCache() +"<->"+ place_of_publication);
            }
        }
        if ("subsp.".equals(infraSpecRank) && !rank.equals(Rank.SUBSPECIES())){
            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
        }else if ("var.".equals(infraSpecRank) && !rank.equals(Rank.VARIETY())){
            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
        }else if ("f.".equals(infraSpecRank) && !rank.equals(Rank.FORM())){
            logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
        }
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
    protected void secondPass(SimpleExcelTaxonImportState<CaryoAizoaceaeExcelImportConfigurator> state) {
        Map<String, String> record = state.getOriginalRecord();
        int line = state.getCurrentLine();
        String fullName = getValue(record, KEW_F_NAME4CDM_LINK);
        String status = getValue(record, TAXON_STATUS);
        String sourceId = getValue(record, PLANT_NAME_ID);
        String accId = getValue(record, ACCEPTED_PLANT_NAME_ID);
        String accName = getValue(record, "AcceptedName");
        String basionymId = getValue(record, "basionym_plant_name_id");
        String homotypicSynonym = getValue(record, "homotypic_synonym");

        String row = String.valueOf(line) + "("+fullName+"): ";
        try {
            if ((line % 500) == 0){
                newTransaction(state);
                System.out.println(line);
            }

            if("Misapplied".equals(status)){
                return;
            }else if (neglectedRecords.contains(sourceId)){
                logger.info(row + "Record ignored.");
                return;
            }

            UUID uuid = taxonMapping.get(sourceId);
            TaxonBase<?> taxonBase = getTaxonService().find(uuid);
            if (taxonBase == null){
                logger.warn(row + "taxonBase not found: " + sourceId);
                return;
            }

            UUID accUuid = taxonMapping.get(accId);
            boolean hasAccepted = !sourceId.equals(accId);

            Taxon accTaxon = null;
            TaxonNode parent = null;
            Taxon child = null;
            Synonym syn = null;
            boolean isSynonymAccepted = false;

            if(accId == null){
                logger.info(row + "accID is null");
                child = CdmBase.deproxy(taxonBase, Taxon.class);
            }else if(hasAccepted){
                TaxonBase<?> accTaxonBase = getTaxonService().find(accUuid);
                if (accTaxonBase == null){
                    logger.warn(row + "acctaxon not found: " + accId + "; " + accName);
                }else if(!accTaxonBase.isInstanceOf(Taxon.class)){
                    logger.warn(row + "acctaxon is synonym: " + accId + "; " + accName);
                    isSynonymAccepted = true;
                }else{
                    accTaxon = CdmBase.deproxy(accTaxonBase, Taxon.class);
                    if (!accTaxon.getName().getTitleCache().equals(accName)){
                        logger.warn(row + "Accepted name differs: " + accName +" <-> "+ accTaxon.getName().getTitleCache());
                    }
                }
            }else if (sourceId.equals(accId)){
                if (!taxonBase.isInstanceOf(Taxon.class)){
                    logger.warn(row + "child not of class Taxon: " + sourceId);
                }else{
                    Rank rank = taxonBase.getName().getRank();
                    child = CdmBase.deproxy(taxonBase, Taxon.class);
                    if(rank.equals(Rank.GENUS())){
                        parent = getFamily();
                    }else if (rank.equals(Rank.SPECIES())){
                        String genus = child.getName().getGenusOrUninomial();
                        UUID parentUuid = taxonMapping.get(genus);
                        parent = getParent(parentUuid, row);
                    }else if (rank.isLowerThan(RankClass.Species)){
                        String speciesName = child.getName().getGenusOrUninomial() + " " + child.getName().getSpecificEpithet();
                        UUID parentUuid = taxonMapping.get(speciesName);
                        parent = getParent(parentUuid, row);
                    }
                }
            }

            if (taxonBase.isInstanceOf(Synonym.class)){
                syn = CdmBase.deproxy(taxonBase, Synonym.class);
            }

            if ("Accepted".equals(status)){
                if (parent == null){
                    logger.warn(row + "Parent is missing. Taxon is moved to 'unresolved' instead'");
                    parent = unresolvedParent();
                }
                if (child == null){
                    logger.warn(row + "Child is missing. Taxon not imported.");
                }else{
                    if (!child.getTaxonNodes().isEmpty()){
                        if(!child.getName().getRank().equals(Rank.GENUS())){
                            logger.warn(row + "Taxon already has a parent. Taxon not attached to any further parent taxon.");
                        }
                    }else{
                        addChild(parent, child, row);
                    }
                }
            }else if ("Synonym".equals(status)){
                if(accTaxon == null){
                    if(isSynonymAccepted){
                        logger.warn(row +  "Synonym added to 'unresolved' as accepted taxon is synonym itself.");
                    }else if (accId != null){
                        logger.warn(row +  "Accepted taxon for synonym unexpectedly does not exist (it seems not to be a synonym itself). Synonym moved to 'unresolved'");
                    }else{
                        logger.warn(row +  "No accepted taxon given for synonym. Therefore taxon moved to 'unresolved'");
                    }
                    if(accId != null){
                        child = Taxon.NewInstance(syn.getName(), syn.getSec());
                        child.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
                    }
                    addChild(unresolvedParent(), child, row);
                    getTaxonService().deleteSynonym(syn, new SynonymDeletionConfigurator());
                }else{
                    accTaxon.addSynonym(syn, SynonymType.SYNONYM_OF);
                }
            }else if ("Unplaced".equals(status)){
                parent = unresolvedParent();
                addChild(parent, child, row);
            }else if ("Artificial Hybrid".equals(status)){
                parent = hybridParent();
                addChild(parent, child, row);
            }else if ("Orthographic".equals(status)){
                if(accTaxon == null){
                    logger.warn(row + "'Orthographic' taxon has no acc taxon");
                }else{
                    accTaxon.addSynonym(syn, SynonymType.SYNONYM_OF);
                }
            }else if("Illegitimate".equals(status) || "Invalid".equals(status)){
                if (hasAccepted){
                    if(accTaxon == null){
                        logger.warn(row + "accepted taxon for illegitimate or invalid taxon not found");
                    }else{
                        accTaxon.addSynonym(syn, SynonymType.SYNONYM_OF);
                    }
                }else{
                    addChild(unresolvedParent(), child, row);
                }
            }else{
                logger.warn(row + "Unhandled status: " +  status);
            }

            if (basionymId != null && false){
                UUID basionymUuid = taxonMapping.get(basionymId);
                TaxonBase<?> basionymTaxon = getTaxonService().find(basionymUuid);
                if (basionymTaxon != null){
                    if (hasSameAcceptedTaxon(taxonBase, basionymTaxon)){
                        if (taxonBase.getName().getBasionym() == null){
                            taxonBase.getName().addBasionym(basionymTaxon.getName());
                        }
                    }else{
                        logger.warn(row + "Basionym has not same accepted taxon and therefore was ignored.");
                    }
                }else{
                    logger.warn(row + "Basionym "+basionymId+" not found.");
                }
            }
        } catch (Exception e) {
            logger.error(row + "Error.");
            e.printStackTrace();
        }
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

    private TaxonNode getParent(UUID parentUuid, String row) {
        if(parentUuid == null){
            logger.warn(row + "Parent uuid is null. No parent found.");
            return null;
        }
        TaxonBase<?> pTaxon = getTaxonService().find(parentUuid);
        if (pTaxon == null){
            logger.warn(row + "No parent found for parent UUID. This should not happen.");
            return null;
        }
        if (pTaxon.isInstanceOf(Synonym.class)){
            logger.warn(row + "Parent is synonym");
            return null;
        }else{
            Taxon ptax = CdmBase.deproxy(pTaxon, Taxon.class);
            if(ptax.getTaxonNodes().isEmpty()){
                logger.warn(row + "Parent has no node yet");
                return null;
            }else {
                if(ptax.getTaxonNodes().size()>1){
                    logger.warn("Parent has >1 nodes. Take arbitrary one");
                }
                return ptax.getTaxonNodes().iterator().next();
            }
        }
    }

    private void addChild(TaxonNode parent, Taxon child, String row) {
        if (parent == null){
            logger.warn(row + "Parent is null");
        }else if (child == null){
            logger.warn(row + "Child is null");
        }else{
            if (!child.getTaxonNodes().isEmpty()){
                TaxonNode childNode = child.getTaxonNodes().iterator().next();
                if (childNode.getParent() != null && childNode.getParent().equals(parent)){
                    logger.info(row + "Parent-child relation exists already.");
                }else{
                    logger.warn(row + "Child already has different parent. Parent-child relation not added.");
                }
            }else{
                TaxonNode node = parent.addChildTaxon(child, null, null);
                getTaxonNodeService().saveOrUpdate(node);
            }
        }
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
