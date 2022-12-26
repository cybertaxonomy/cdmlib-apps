/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.casearia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Taxon import for Casearia from Kew world checklist of plants.
 *
 * @author a.mueller
 * @since 12.05.2020
 */
@Component
public class CaseariaTaxonImport extends SimpleExcelTaxonImport<CaseariaImportConfigurator>{

    private static final long serialVersionUID = 7686154384296707819L;
    private static final Logger logger = LogManager.getLogger();

    protected static final String TAXON_MAPPING = "TaxonMapping";
    private static final String NAME_CIT = "NameCit";
    private static final String IPNI_ID = "ipni_id";
    private static final String PLANT_NAME_ID = "plant_name_id";
    private static final String TAXON_RANK = "taxon_rank";
    private static final String TAXON_STATUS = "taxon_status";
    private static final String NOMENCLATURAL_REMARKS = "nomenclatural_remarks";
    private static final String ACCEPTED_PLANT_NAME_ID = "accepted_plant_name_id";
    private static final String TAXON_NAME = "taxon_name";
    private static final String TAXON_AUTHORS = "taxon_authors";
    private static final String FAMILY = "family";
    private static final String FIRST_PUBLISHED = "first_published";
    private static final String PUB_TYPE = "PubType";
    private static final String VOLUME_AND_PAGE = "volume_and_page";
    private static final String PLACE_OF_PUBLICATION = "place_of_publication";
    private static final String PRIMARY_AUTHOR = "primary_author";
    private static final String PARENTHETICAL_AUTHOR = "parenthetical_author";
    private static final String INFRASPECIFIC_RANK = "infraspecific_rank";
    private static final String INFRASPECIES = "infraspecies";
    private static final String SPECIES = "species";
    private static final String GENUS = "genus";

    private static final int RECORDS_PER_TRANSACTION = 500;
    private static boolean logMissingIpniId = false;

    private Map<String, UUID> taxonMapping = new HashMap<>();
    private Reference secRef = null;
    private Set<UUID> createdNames = new HashSet<>();

    private SimpleExcelTaxonImportState<CaseariaImportConfigurator> state;
    private NonViralNameParserImpl parser = new NonViralNameParserImpl();


    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state) {
        int line = state.getCurrentLine();
        if ((line % RECORDS_PER_TRANSACTION) == 0){
            newTransaction(state);
            System.out.println(line);
        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        String fullCitation = getValue(record, NAME_CIT);
        String ipniId = getValue(record, IPNI_ID);
        String sourceId = getValue(record, PLANT_NAME_ID);
        String rankStr = getValue(record, TAXON_RANK);
        String status = getValue(record, TAXON_STATUS);
        String nomenclaturalRemarks = getValue(record, NOMENCLATURAL_REMARKS);
        String accId = getValue(record, ACCEPTED_PLANT_NAME_ID);
        String taxonNameStr = getValue(record, TAXON_NAME);
        String taxonAuthors = getValue(record, TAXON_AUTHORS);

        String fullNameStr = CdmUtils.concat(" ", taxonNameStr,taxonAuthors);
        String row = String.valueOf(line) + "("+fullNameStr+"): ";

        boolean isNewName = true;

        try {

            List<NomenclaturalStatusType> statusTypes = new ArrayList<>();
            Class<? extends CdmBase> taxonClazz = makeStatus(status, sourceId, accId, row, statusTypes);


            Rank rank = state.getTransformer().getRankByKey(rankStr);

            TaxonName name = parser.parseReferencedName(fullCitation, state.getConfig().getNomenclaturalCode(), rank);
            if (name.isProtectedFullTitleCache() || name.isProtectedTitleCache() || name.isProtectedNameCache()
                    || name.isProtectedAuthorshipCache()){
                logger.warn(row + "Name not parsable: " + fullCitation);
                name.setTitleCache(fullNameStr, true);
                name.setNameCache(taxonNameStr, true);
            }else{
                testParsedName(state, name, row, null);
            }
            name.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
            name = dedupliateNameParts(name);
            getNameService().saveOrUpdate(name);
            createdNames.add(name.getUuid());

            handleNomenclRemarkAndNameStatus(nomenclaturalRemarks, row, isNewName, name, statusTypes);

            TaxonBase<?> taxonBase;
            if (taxonClazz == Taxon.class){
                taxonBase = Taxon.NewInstance(name, getSecRef());
            }else{
                taxonBase = Synonym.NewInstance(name, getSecRef());
            }
            taxonBase.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
            getTaxonService().saveOrUpdate(taxonBase);

            if (!isBlank(ipniId)){
                DefinedTerm ipniIdIdentifierType = DefinedTerm.IDENTIFIER_NAME_IPNI();
                name.addIdentifier(ipniId, ipniIdIdentifierType);
            }else{
                if(logMissingIpniId){
                    logger.warn(row + "IPNI id is missing.");
                }
            }

            UUID uuid = taxonMapping.put(sourceId, taxonBase.getUuid());{
                if (uuid != null){
                    logger.warn(row + "sourceId already existed in taxonMapping: " + sourceId);
                }
            }
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

    private Class<? extends CdmBase> makeStatus(String status, String sourceId,
            String accId, String row, List<NomenclaturalStatusType> statusTypes) {

        Class<? extends CdmBase> clazz;
        if ("Accepted".equals(status) || "Unplaced".equals(status) || "Misapplied".equals(status)){
            clazz = Taxon.class;
        }else if ("Synonym".equals(status)){
            clazz = (accId == null)? Taxon.class : Synonym.class;
        }else if("Illegitimate".equals(status)){
            clazz = getIllegInvalidStatus(sourceId, accId);
            statusTypes.add(NomenclaturalStatusType.ILLEGITIMATE());
        }else if ("Invalid".equals(status)){
            clazz = getIllegInvalidStatus(sourceId, accId);
            statusTypes.add(NomenclaturalStatusType.INVALID());
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

    private void newTransaction(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state) {
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
            if (secRef == null){
                secRef = ReferenceFactory.newDatabase();
                secRef.setTitle("Casearia Database");
            }
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


    private void testParsedName(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state, TaxonName name,
            String row, String fullCitation) throws UndefinedTransformerMethodException {

        Map<String, String> record = state.getOriginalRecord();

//      publication_author

        String rankStr = getValue(record, TAXON_RANK);
        String nameCache = getValue(record, TAXON_NAME);
        String authorshipCache = getValue(record, TAXON_AUTHORS);
        String genus = getValue(record, GENUS);
        String species = getValue(record, SPECIES);
        String infraspecies = getValue(record, INFRASPECIES);
        String infraSpecRank = getValue(record, INFRASPECIFIC_RANK);
        String basionymAuthor = getValue(record, PARENTHETICAL_AUTHOR);
        String combinationAuthor = getValue(record, PRIMARY_AUTHOR);
        String place_of_publication = getValue(record, PLACE_OF_PUBLICATION);
        String volume_and_page = getValue(record, VOLUME_AND_PAGE);
        String pubType = getValue(record, PUB_TYPE);
        String yearPublished = getValue(record, FIRST_PUBLISHED);

        String fullName = CdmUtils.concat(" ", nameCache, authorshipCache);

        if (!CdmUtils.nullSafeEqual(name.getNameCache(), nameCache)){
            logger.warn(row + "Unexpected nameCache: " + nameCache);
        }
        if (!CdmUtils.nullSafeEqual(name.getTitleCache(), fullName)){
            logger.warn(row + "Unexpected titleCache: <->" + name.getTitleCache());
        }
        if (!CdmUtils.nullSafeEqual(name.getGenusOrUninomial(),genus)){
            logger.warn(row + "Unexpected genus: " + genus);
        }
        if (!CdmUtils.nullSafeEqual(name.getSpecificEpithet(), species)){
            logger.warn(row + "Unexpected species epithet: " + name.getSpecificEpithet() +"<->"+ species);
        }
        if (!CdmUtils.nullSafeEqual(name.getInfraSpecificEpithet(), infraspecies)){
            logger.warn(row + "Unexpected infraspecific epithet: " + name.getInfraSpecificEpithet() +"<->"+ infraspecies);
        }
        if (!CdmUtils.nullSafeEqual(name.getAuthorshipCache(), authorshipCache)){
            logger.warn(row + "Unexpected authors: " + name.getAuthorshipCache() +"<->"+ authorshipCache);
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
            String year = normalizeYear(yearPublished);
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
        if (isBlank(infraSpecRank)){
            if (rank.isLowerThan(RankClass.Species)){
                logger.warn(row +  "No infraspce marker given but rank is lower than species");
            }
        }else if ("subsp.".equals(infraSpecRank)){
            if(!rank.equals(Rank.SUBSPECIES())){
                logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
            }
        }else if ("var.".equals(infraSpecRank)){
            if (!rank.equals(Rank.VARIETY())){
                logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
            }
        }else if ("subvar.".equals(infraSpecRank)){
            if (!rank.equals(Rank.SUBVARIETY())){
                logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
            }
        }else if ("f.".equals(infraSpecRank)){
            if (!rank.equals(Rank.FORM())){
                logger.warn(row + "Unexpected infraspec marker: " + infraSpecRank);
            }
        }else{
            logger.warn(row + "Unhandled infraspec marker: " + infraSpecRank);
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
        }else{
            year = year.substring(1, year.length() - 1);
        }
        if (year.contains("\" [")){
            String[] split = year.split("\" \\[");
            year = split[1].replace("]","") + " [" + split[0]+"\"]";
        }else if ("?".equals(year)){
            return null;
        }
        return year;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaseariaImportConfigurator> state) {
        state.putStatusItem(TAXON_MAPPING, taxonMapping);


        Map<String, String> record = state.getOriginalRecord();
        int line = state.getCurrentLine();
//        String fullName = getValue(record, KEW_F_NAME4CDM_LINK);
        String status = getValue(record, TAXON_STATUS);
        String sourceId = getValue(record, PLANT_NAME_ID);
        String accId = getValue(record, ACCEPTED_PLANT_NAME_ID);
        String family = getValue(record, FAMILY);

        String accName = getValue(record, "AcceptedName");
        String basionymId = getValue(record, "basionym_plant_name_id");
        String homotypicSynonym = getValue(record, "homotypic_synonym");

//      AcceptedName, Basionym, taxon_name_hybcorr, genus_hybrid, species_hybrid, homotypic_synonym,
//      basionym_plant_name_id

        String taxonNameStr = getValue(record, TAXON_NAME);
        String taxonAuthors = getValue(record, TAXON_AUTHORS);
        String fullNameStr = CdmUtils.concat(" ", taxonNameStr,taxonAuthors);
        String row = String.valueOf(line) + "("+fullNameStr+"): ";

        try {
            if ((line % RECORDS_PER_TRANSACTION) == 0){
                newTransaction(state);
                System.out.println(line);
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
            //synonyms
            }else if(hasAccepted){
                TaxonBase<?> accTaxonBase = getTaxonService().find(accUuid);
                if (accTaxonBase == null){
//                    logger.warn(row + "acctaxon not found: " + accId + "; " + accName);
                }else if(!accTaxonBase.isInstanceOf(Taxon.class)){
                    logger.warn(row + "acctaxon is synonym: " + accId + "; " + accName);
                    isSynonymAccepted = true;
                }else{
                    accTaxon = CdmBase.deproxy(accTaxonBase, Taxon.class);
                    if (!accTaxon.getName().getTitleCache().equals(accName)){
                        logger.warn(row + "Accepted name differs: " + accName +" <-> "+ accTaxon.getName().getTitleCache());
                    }
                }
            //accepted taxa
            }else if (sourceId.equals(accId)){
                if (!taxonBase.isInstanceOf(Taxon.class)){
                    logger.warn(row + "child not of class Taxon: " + sourceId);
                }else{
                    Rank rank = taxonBase.getName().getRank();
                    child = CdmBase.deproxy(taxonBase, Taxon.class);
                    if(rank.equals(Rank.GENUS())){
                        parent = getFamily(row, family);
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
                        logger.warn(row +  "Accepted taxon "+accName+" for synonym unexpectedly does not exist. Synonym was moved to 'unresolved'");
                    }else{
                        logger.warn(row +  "No accepted taxon given for synonym. Therefore taxon was moved to 'unresolved'");
                    }
                    if(accId != null){
                        child = Taxon.NewInstance(syn.getName(), syn.getSec());
                        taxonMapping.put(sourceId, child.getUuid());
                        child.addImportSource(sourceId, PLANT_NAME_ID, getSourceReference(state), "line " + state.getCurrentLine());
                    }
                    addChild(unresolvedParent(), child, row);
                    getTaxonService().deleteSynonym(syn, new SynonymDeletionConfigurator());
                }else{
                    accTaxon.addSynonym(syn, SynonymType.SYNONYM_OF);
                }
            }else if ("Misapplied".equals(status)){
                Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
                if(accTaxon == null){
                    if(isSynonymAccepted){
                        logger.warn(row +  "Misapplication added to 'unresolved' as accepted taxon is synonym itself.");
                    }else if (accId != null){
                        logger.warn(row +  "Accepted taxon "+accName+" for misapplication unexpectedly does not exist. Misapplication was moved to 'unresolved'");
                    }else{
                        logger.warn(row +  "No accepted taxon given for misapplication. Therefore taxon was moved to 'unresolved'");
                    }
                    addChild(unresolvedParent(), taxon, row);
                }else{
                    accTaxon.addMisappliedName(taxon, null, null);
                }
            }else if ("Unplaced".equals(status)){
                parent = unresolvedParent();
                addChild(parent, child, row);
            }else if("Illegitimate".equals(status) || "Invalid".equals(status)){
                if (hasAccepted){
                    if(accTaxon == null){
                        logger.warn(row + "accepted taxon for illegitimate or invalid taxon not found. Illeg/inval taxon was moved to 'unresolved'");
                        child = Taxon.NewInstance(syn.getName(), syn.getSec());
                        addChild(unresolvedParent(), child, row);
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
                logger.info(row + "Parent has no node yet");
                TaxonNode newParent = getClassification().addChildTaxon(ptax, null, null);
                getTaxonNodeService().saveOrUpdate(newParent);
                return newParent;
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

    private TaxonNode getFamily(String line, String family){
        UUID uuid;
        if ("Salicaceae".equals(family)){
            uuid = UUID.fromString("5432a4eb-2fbe-4494-925d-d01743ed435f");
//        }else if ("Meliaceae".equals(family)){
//            //Note: not needed, genus with family Meliaceae is synonym
//            uuid = UUID.fromString("c8694910-bfec-45a1-8901-2a0a2a6f12b1");
        }else{
            logger.warn(line + "Family not yet handled: " + family);
            return null;
        }
        TaxonNode familyNode = getTaxonNodeService().find(uuid);
        if (familyNode == null){
            familyNode = createFamily(family, uuid);
        }
        return familyNode;
    }

    private TaxonNode createFamily(String family, UUID uuid) {
        Classification classification = getClassification();
        TaxonName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
        name.setGenusOrUninomial(family);
        Taxon taxon = Taxon.NewInstance(name, getSecRef());
        TaxonNode result = classification.addChildTaxon(taxon, null, null);
        result.setUuid(uuid);
        getTaxonNodeService().saveOrUpdate(result);
        return result;
    }

    private Classification getClassification() {
        Classification classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        if (classification == null){
            classification = Classification.NewInstance(
                    state.getConfig().getClassificationName(), getSecRef(), Language.LATIN());
            classification.setUuid(state.getConfig().getClassificationUuid());
            getClassificationService().save(classification);
        }
        return classification;
    }

    private TaxonNode unresolvedParent(){
        UUID uuid = UUID.fromString("1c48b8d3-077d-4aef-9e41-d4d3e0abd4c7");
        TaxonNode unresolvedParent = getTaxonNodeService().find(uuid);
        if (unresolvedParent == null){
            unresolvedParent = createFamily("Unresolved", uuid);
        }
        return unresolvedParent;
    }
}
