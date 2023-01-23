/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
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
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Kew excel taxon import for Caryophyllaceae.
 *
 * @author a.mueller
 * @since 05.01.2022
 */
@Component
public class KewExcelTaxonImport<CONFIG extends KewExcelTaxonImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 1081966876789613803L;
    private static final Logger logger = LogManager.getLogger();

    private static final String NO_SIMPLE_DIFF = "xxxxx";

//    private static final String KEW_UNPLACED_NODE = "82a9e3a1-2519-402a-b3c9-ec4c1fddf4d0";
//
//    private static final String KEW_ORPHANED_PLACEHOLDER_TAXON = "dccac79b-a967-49ed-b153-5faa83194060";

    private static final String CDM_Name_UUID = "CDM-Name_UUID";
    private static final String Kew_Name_ID = "Kew-Name-ID";
    private static final String Kew_Name_Citation = "Kew-Name-Citation";
    private static final String Kew_Taxonomic_Status = "Kew-Taxonomic-Status";
    private static final String Kew_Nomencl_Status = "Kew-Nomencl-Status";
    private static final String Kew_Rel_Acc_Name_ID = "Kew-Rel-Acc-Name-ID";
    private static final String Kew_Rel_Basionym_Name_ID = "Kew-Rel-Basionym-Name-ID";
    private static final String GENUS_HYBRID = "genus_hybrid";
    private static final String GENUS = "genus";
    private static final String SPECIES_HYBRID = "species_hybrid";
    private static final String SPECIES = "species";

    private static final String infraspecific_rank = "infraspecific_rank";
    private static final String infraspecies = "infraspecies";

    private static final String parenthetical_author = "parenthetical_author";
    private static final String primary_author = "primary_author";
    private static final String publication_author = "publication_author";
    private static final String place_of_publication = "place_of_publication";
    private static final String volume_and_page = "volume_and_page";
    private static final String KewYear4CDM = "KewYear4CDM";
    private static final String PubTypeABSG = "PubTypeABSG";
    private static final String Sec_Ref_CDM_UUID = "Sec-Ref-CDM-UUID";

    private static final Map<String, UUID> nameMap = new HashMap<>();
    private static final Map<String, UUID> taxonMap = new HashMap<>();

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            CDM_Name_UUID, Kew_Name_ID, Kew_Name_Citation, Kew_Taxonomic_Status,
            Kew_Nomencl_Status, Kew_Rel_Acc_Name_ID, Kew_Rel_Basionym_Name_ID, GENUS_HYBRID, GENUS,
            SPECIES_HYBRID, SPECIES, infraspecific_rank, infraspecies,
            parenthetical_author, primary_author, publication_author, place_of_publication,
            volume_and_page, KewYear4CDM, PubTypeABSG, Sec_Ref_CDM_UUID
    });

    private Reference sourceReference;
    private Reference secReference;

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

//    @Override
//    protected String getWorksheetName(CONFIG config) {
//        return "valid taxa names";
//    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = getLine(state, 50);
//        System.out.println(line);
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        makeTaxon(state, line, record);
    }

    private void makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {
//        state.getTransactionStatus().flush();
        Reference sec = getSecReference(state, record);

        //name
        boolean isNewName = true;
        TaxonName name = getExistingName(state, line);
        if (name != null){
            verifyName(state, name, record, line, false);
            isNewName = false;
        }else{
            name = createName(state, line);
        }

        //taxon
        TaxonBase<?> taxonBase = makeTaxonBase(state, line, record, name, sec, isNewName);

        if (taxonBase != null){
            getTaxonService().saveOrUpdate(taxonBase);
        }

        return;
    }

    private TaxonName createName(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        //parse
        String fullTitle = getValue(state, Kew_Name_Citation);
        String kewNameId = getValue(state, Kew_Name_ID);

//        fullTitle = replaceBookSectionAuthor(state, fullTitle);

        TaxonName newName = parser.parseReferencedName(fullTitle, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        handleBookSectionAuthor(newName, state, line);

        putName(kewNameId, newName.getUuid(), line);
        //name status
        makeNameStatus(line, state.getOriginalRecord(), newName);
        verifyName(state, newName, state.getOriginalRecord(), line, true);
        //deduplication
        replaceNameAuthorsAndReferences(state, newName);
        newName.addSource(makeOriginalSource(state));
        getNameService().saveOrUpdate(newName);
        //Kew-Nomencl-Status
        return newName;
    }

    private void handleBookSectionAuthor(TaxonName newName, SimpleExcelTaxonImportState<CONFIG> state, String line) {
        String type = getValue(state, PubTypeABSG);
        if ("BS".equals(type)){
            Reference book = newName.getNomenclaturalReference();
            String pubAuthor = getValue(state, publication_author);
            if (book != null && StringUtils.isNotEmpty(pubAuthor)){
                TeamOrPersonBase<?> bookAuthor = parseBookSectionAuthor(pubAuthor, line);
                Reference bookSection = ReferenceFactory.newBookSection();
                bookSection.setAuthorship(book.getAuthorship());
                book.setAuthorship(bookAuthor);
                bookSection.setInReference(book);
                bookSection.setDatePublished(book.getDatePublished());
                newName.setNomenclaturalReference(bookSection);
            }else{
                logger.warn(line + "unexpected booksection author handling");
            }
        }
    }

    private TeamOrPersonBase<?> parseBookSectionAuthor(String pubAuthor, String line) {
        TeamOrPersonBase<?> result;
        String ed = "";
        if (pubAuthor.endsWith(" (ed.)")){
            ed = " (ed.)";
        }else if (pubAuthor.endsWith(" (eds.)")){
            ed = " (eds.)";
        }
        pubAuthor = pubAuthor.substring(0, pubAuthor.length() - ed.length());
        String[] splits = pubAuthor.split("(, | & )");
        if (splits.length > 1){
            Team team = Team.NewInstance();
            result = team;
            for (String split : splits){
                if ("al.".equals(split.trim())){
                    team.setHasMoreMembers(true);
                }else{
                    team.addTeamMember(getPerson(split, line));
                }
            }
        }else{
            result = getPerson(splits[0], line);
        }
        if (ed.length() > 0){
            result.setTitleCache(result.getTitleCache() + ed, true);
        }
        return result;
    }

    private Person getPerson(String personStr, String line) {
        Person result = Person.NewInstance();
        String regEx = "([A-ZÉ]\\.\\-?)+((de|von)\\s)?(?<famname>[A-Z][a-zèéöü]+((\\-|\\s(i|de)?\\s*)[A-Z][a-zèéü]+)?)";
//        regEx = "([A-ZÉ]\\.\\-?)+((de|von)\\s)?Boissier";
        Matcher matcher = Pattern.compile(regEx).matcher(personStr);
        if (matcher.matches()){
            String famName = matcher.group("famname");
            result.setFamilyName(famName);
            String initials = personStr.replace(famName,"").trim();
            result.setInitials(initials);
        }else{
            result.setTitleCache(personStr, true);
            logger.warn(line + "BookSection author could not be parsed: " +  personStr);
        }
        return result;
    }

    private String replaceBookSectionAuthor(SimpleExcelTaxonImportState<CONFIG> state, String fullTitle) {
        String type = getValue(state, PubTypeABSG);
        if ("BS".equals(type)){
            String pubAuthor = getValue(state, publication_author);
            int inIndex = fullTitle.indexOf(" in ");
            int commaIndex = fullTitle.indexOf(", ");

        }
        return fullTitle;
    }

    private void verifyName(SimpleExcelTaxonImportState<CONFIG> state, TaxonName taxonName,
            Map<String, String> record, String line, boolean isNew) {
        if (isNew){
            boolean parsed = checkParsed(taxonName, getValue(state, Kew_Name_Citation), null, line);
            if (!parsed){
                return;
            }
        }
        String fullDiff = verifyField(replaceStatus(taxonName.getFullTitleCache()), record, Kew_Name_Citation, line, null, isNew);
        verifyField(taxonName.getGenusOrUninomial(), record, GENUS, line, null, isNew);
        verifyField(taxonName.getSpecificEpithet(), record, SPECIES, line, null, isNew);
        verifyField(taxonName.getInfraSpecificEpithet(), record, infraspecies, line, null, isNew);
        String existingBasionymAuthor = authorAndExAuthor(taxonName.getBasionymAuthorship(), taxonName.getExBasionymAuthorship());
        verifyField(existingBasionymAuthor, record, parenthetical_author, line, null, isNew);
        String existingCombinationAuthor = authorAndExAuthor(taxonName.getCombinationAuthorship(), taxonName.getExCombinationAuthorship());
        verifyField(existingCombinationAuthor, record, primary_author, line, null, isNew);

        //reference
        Reference nomRef = taxonName.getNomenclaturalReference();
        if (nomRef == null){
            logger.warn(line + "no nom.ref. exists in existing name");
        }else{

            //place of publication
            boolean hasInRef = nomRef.getInReference() != null;
            String existingAbbrevTitle = hasInRef && (nomRef.getType() == ReferenceType.BookSection || nomRef.getType() == ReferenceType.Article) ?
                    nomRef.getInReference().getAbbrevTitle() :
                    nomRef.getAbbrevTitle();
            String diffPlacePub = verifyField(existingAbbrevTitle, record, place_of_publication, line, fullDiff, isNew);
            //author
            String inRefAuthor = (!hasInRef || nomRef.getInReference().getAuthorship() == null) ? null : nomRef.getInReference().getAuthorship().getTitleCache();
            verifyField(inRefAuthor, record, publication_author, line, fullDiff, isNew);
            //vol and page
            String existingVolume = getVolume(nomRef);
            String existingVolAndPage = CdmUtils.Nz(existingVolume) + ": " + CdmUtils.Nz(taxonName.getNomenclaturalSource().getCitationMicroReference());
            verifyField(existingVolAndPage, record, volume_and_page, line, fullDiff, diffPlacePub, isNew);
            //year
            verifyField(nomRef.getYear(), record, KewYear4CDM, line, fullDiff, isNew);
            //pub type
            verifyField(abbrefRefType(nomRef.getType()), record, PubTypeABSG, line, null, isNew);
        }
    }

    private String getVolume(Reference nomRef) {
        Reference ref = nomRef.isBookSection()? nomRef.getInReference(): nomRef;
        String vol = ref.getVolume();
        String edition = ref.getEdition();
        if (StringUtils.isNotBlank(edition)){
            edition = ", " + (isNumber(edition)? "ed. ":"") + edition + ",";
        }
        String series = ref.getSeriesPart();
        if (StringUtils.isNotBlank(series)){
            series = ", " + (isNumber(series)? "ser. ":"") + series + ",";
        }

        return vol;
    }

    private boolean isNumber(String edition) {
        try {
            Integer.valueOf(edition);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private String authorAndExAuthor(TeamOrPersonBase<?> author,
            TeamOrPersonBase<?> exAuthor) {
        return author == null? null : (exAuthor != null ? (exAuthor.getNomenclaturalTitleCache() + " ex "): "")
                + author.getNomenclaturalTitleCache();
    }

    private String replaceStatus(String fullTitleCache) {
        return fullTitleCache.replaceAll(", nom\\. inval\\.$", "").replaceAll(", nom\\. illeg\\.$", "");
    }

    private String abbrefRefType(ReferenceType type) {
        return type == ReferenceType.Article ? "A" :
            type == ReferenceType.Book ? "B" :
            type == ReferenceType.BookSection ? "BS" :
            type == ReferenceType.Generic ? "GEN" :
            type.getLabel() ;
    }

    private String verifyField(String expectedValue, Map<String, String> record, String fieldName, String line, String noLogIf, boolean isNew) {
        return verifyField(expectedValue, record, fieldName, line, noLogIf, null, isNew);
    }

    private String verifyField(String expectedValue, Map<String, String> record, String fieldName, String line,
            String noLogIf, String noLogIf2, boolean isNew) {

        String value = getValue(record, fieldName);
        if (!CdmUtils.nullSafeEqual(expectedValue, value)){
            String diff = singleDiff(expectedValue, value);
            String label1 = isNew ? "Parsed  " : "Existing";
            String label2 = isNew ? "Atomized" : "Kew     ";
            if (!diff.equals(noLogIf) && !diff.equals(noLogIf2) || diff.equals(NO_SIMPLE_DIFF)){
                System.out.println("   " + line + fieldName + "\n        "+label1+": " + expectedValue + "\n        "+label2+": " + value);
            }
            return diff;
        }else{
            return "";
        }
    }

    private String singleDiff(String expectedValue, String value) {
        if (expectedValue == null){
            return CdmUtils.Nz(value);
        }else if (value == null){
            return CdmUtils.Nz(expectedValue);
        }
        expectedValue = expectedValue.trim();
        value = value.trim();
        String diff_ab = StringUtils.difference(expectedValue, value);
        String diff_ba = StringUtils.difference(value, expectedValue);
        if (diff_ab.endsWith(diff_ba)){
            return "+" + diff_ab.substring(0, diff_ab.length() - diff_ba.length());
        }else if (diff_ba.endsWith(diff_ab)){
            return "-" + diff_ba.substring(0, diff_ba.length() - diff_ab.length());
        }else{
            return NO_SIMPLE_DIFF;
        }
    }

    private TaxonName getExistingName(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        String cdmNameUuid = getValue(state, CDM_Name_UUID);
        String kewNameId = getValue(state, Kew_Name_ID);
        if (cdmNameUuid == null){
            return null;
        }
        TaxonName existingName = getNameService().load(UUID.fromString(cdmNameUuid));
        if (existingName != null){
            putName(kewNameId, existingName.getUuid(), line);
            return CdmBase.deproxy(existingName);
        }else{
            logger.warn(line + "Name with CDM-Name_UUID="+cdmNameUuid+" could not be found in database");
            return null;
        }
    }

    private void putName(String kewNameId, UUID uuid, String line) {
        UUID existingUuid = nameMap.put(kewNameId, uuid);
        if (existingUuid != null){
            logger.warn(line + "Kew-Name-id already exists: " + kewNameId);
        }
    }

    private void makeNameStatus(String line, Map<String, String> record,
            TaxonName taxonName) {
        String nameStatus = getValue(record, Kew_Nomencl_Status);
        NomenclaturalStatusType status;
        if (isBlank(nameStatus)){
            status = null;
        }else if ("Illegitimate".equals(nameStatus) || "nom. illeg.".equals(nameStatus)){
            status = NomenclaturalStatusType.ILLEGITIMATE();
        }else if ("Invalid".equals(nameStatus) || "nom. inval.".equals(nameStatus)){
            status = NomenclaturalStatusType.INVALID();
        }else if ("nom. cons.".equals(nameStatus)){
            status = NomenclaturalStatusType.CONSERVED();
        }else if ("nom. rej.".equals(nameStatus)){
            status = NomenclaturalStatusType.REJECTED();
        }else{
            logger.warn(line + "Nom. status not recognized: " + nameStatus);
            status = null;
        }
        if (status != null){
            taxonName.addStatus(NomenclaturalStatus.NewInstance(status));
        }
    }


    private TaxonBase<?> makeTaxonBase(SimpleExcelTaxonImportState<CONFIG> state, String line,
            Map<String, String> record, TaxonName taxonName, Reference sec, boolean isNewName) {

        TaxonBase<?> taxonBase;
        boolean isUnplaced = false;
        String taxStatusStr = getValue(record, Kew_Taxonomic_Status);


        TaxonBase<?> existingTaxon = null;
        if (!isNewName && !taxonName.getTaxa().isEmpty()) {
            if (taxonName.getTaxa().size() > 1) {
                System.out.println("  " + line + "Existing name is used in more than 1 taxon/synonym: " + taxonName.getTitleCache());
            }
            existingTaxon = CdmBase.deproxy(taxonName.getTaxa().iterator().next());
        }

        if ("Accepted".equals(taxStatusStr)){
            taxonBase = Taxon.NewInstance(taxonName, sec);
        }else if ("Synonym".equals(taxStatusStr)){
            taxonBase = Synonym.NewInstance(taxonName, sec);
        }else if ("Artificial Hybrid".equals(taxStatusStr)){
            taxonBase = Synonym.NewInstance(taxonName, sec);
        }else if ("Unplaced".equals(taxStatusStr)){
            taxonBase = Taxon.NewInstance(taxonName, sec);
        }else{
            logger.warn(line + "Status not handled: " + taxStatusStr);
            return null;
        }
        if (existingTaxon != null && existingTaxon.getClass().equals(taxonBase.getClass())) {
            taxonBase = existingTaxon;
        }


        taxonBase.addSource(makeOriginalSource(state));
        taxonMap.put(getValue(record, Kew_Name_ID), taxonBase.getUuid());
        if (taxonBase instanceof Taxon){
            UUID existing = taxonMap.get(taxonBase.getName().getNameCache());
            if (existing == null || !isUnplaced){
                taxonMap.put(taxonBase.getName().getNameCache(), taxonBase.getUuid());
            }else if (!isUnplaced){
                taxonMap.put(taxonBase.getName().getNameCache(), taxonBase.getUuid());
                System.out.println("  " + line + "There is more than 1 taxon with name: " + taxonBase.getName().getNameCache());
            }
        }
        return taxonBase;
    }

    int c2 = 0;
    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String kewId = getValue(state, Kew_Name_ID) + ": ";
        String line = " (line: " + state.getCurrentLine() + ")";
//        System.out.println(line);
        if (c2++ % 100 == 0){
            this.commitTransaction(state.getTransactionStatus());
            this.classification = null;
            this.secReference = null;
            this.sourceReference = null;
            this.orphanedSynonymTaxon = null;
            TransactionStatus tx = this.startTransaction();
            state.setTransactionStatus(tx);
            logger.info(line + "New transaction started.");
        }
        Map<String, String> record = state.getOriginalRecord();

        Classification classification = getClassification(state);
        TaxonBase<?> taxonBase = getTaxon(record);
        TaxonName taxonName = taxonBase.getName();

        if (taxonBase.isInstanceOf(Taxon.class)){
            Taxon parent = getParent(state, record, taxonName, line, kewId);
            if (parent != null){
                classification.addParentChild(parent, CdmBase.deproxy(taxonBase, Taxon.class), null, null);
            }
        }else if (taxonBase.isInstanceOf(Synonym.class)){
            Taxon taxon = getAcceptedTaxon(record, line, kewId);
            if (taxon == null){
                taxon = getOrphanedSynonymTaxon(state);
                logger.warn(kewId + "Accepted taxon not found. Added synonym to 'orphaned synonym taxon': " + getValue(record, Kew_Rel_Acc_Name_ID) + line);
            }
            taxon.addSynonym(CdmBase.deproxy(taxonBase, Synonym.class), SynonymType.SYNONYM_OF);
        }else{
            logger.warn("Unhandled");
        }

        String basionymId = getValue(record, Kew_Rel_Basionym_Name_ID);
        if (basionymId != null){
            UUID basionymUuid = nameMap.get(basionymId);
            TaxonName basionym = getNameService().find(basionymUuid);
            if(basionym == null){
                logger.warn(kewId + "Basionym does not exist: " + basionymId + line);
            }else{
                taxonName.addBasionym(basionym);
                taxonName.mergeHomotypicGroups(basionym);  //just in case this is not automatically done
                //TODO
                //          adjustSynonymType(taxonBase, basionymTaxon, line);
            }
        }

    }

    Taxon orphanedSynonymTaxon;
    private Taxon getOrphanedSynonymTaxon(SimpleExcelTaxonImportState<CONFIG> state) {
        if (orphanedSynonymTaxon != null) {
            return orphanedSynonymTaxon;
        }
        UUID orphanedTaxonUuid = state.getConfig().getOrphanedPlaceholderTaxonUuid();
        orphanedSynonymTaxon = CdmBase.deproxy(getTaxonService().find(orphanedTaxonUuid), Taxon.class);
        if (orphanedSynonymTaxon == null){
            TaxonName placeholderName = TaxonNameFactory.NewBotanicalInstance(Rank.SUBFAMILY());
            placeholderName.setTitleCache("Orphaned_Synonyms_KEW", true);
            orphanedSynonymTaxon = Taxon.NewInstance(placeholderName, getSecReference(state, state.getOriginalRecord()));
            orphanedSynonymTaxon.setUuid(orphanedTaxonUuid);
            Taxon unplacedTaxon = CdmBase.deproxy(getTaxonService().find(state.getConfig().getUnplacedTaxonUuid()), Taxon.class);
            TaxonNode orphandNode = getClassification(state).addParentChild(unplacedTaxon, orphanedSynonymTaxon, null, null);
            getTaxonNodeService().save(orphandNode);
        }
        return orphanedSynonymTaxon;
    }

    private Classification classification;
    private Classification getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (classification == null){
            classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        }
        return classification;
    }

    private Taxon getAcceptedTaxon(Map<String, String> record, String line, String kewId) {
        String statusStr = getValue(record, Kew_Taxonomic_Status);
        if ("Synonym".equals(statusStr) || "Artificial Hybrid".equals(statusStr) ){
            String accKewId = getValue(record, Kew_Rel_Acc_Name_ID);
            UUID accUuid = taxonMap.get(accKewId);
            TaxonBase<?> accBase = getTaxonService().find(accUuid);
            if (accBase == null){
                logger.warn(kewId + "Accepted Taxon does not exist: " + accKewId + line);
                return null;
            }else if (accBase.isInstanceOf(Synonym.class)){
                logger.warn(kewId + "Accepted Taxon is synonym: " + accKewId + line);
                return null;
            }else{
                return CdmBase.deproxy(accBase, Taxon.class);
            }
        }else{
            logger.warn(kewId + "Parent not retrieved" +  line);
            return null;
        }
    }

    private Taxon getParent(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record, TaxonName taxonName, String line, String kewId) {
        String statusStr = getValue(record, Kew_Taxonomic_Status);
        if ("Unplaced".equals(statusStr)){
            Taxon unplaced = CdmBase.deproxy(getTaxonService().find(state.getConfig().getUnplacedTaxonUuid()), Taxon.class);
            logger.warn(line + "Could not find 'unplaced' taxon with uuid '" + state.getConfig().getUnplacedTaxonUuid() + "'."  );
            return unplaced;
        }else if ("Artificial Hybrid".equals(statusStr)){
            return null ; //getTaxonNodeService().find(UUID.fromString(KEW_HYBRIDS_NODE)); hybrids are handled as synonyms now
        }else if ("Accepted".equals(statusStr)){
            String higherName = getHigherRankName(taxonName);
            UUID parentTaxonUuid = higherName == null ? null : taxonMap.get(higherName);
            if (parentTaxonUuid != null){
                TaxonBase<?> parentBase = getTaxonService().find(parentTaxonUuid);
                if (parentBase == null){
                    return null;
                } else if (parentBase.isInstanceOf(Taxon.class)){
                    Taxon parentTaxon = CdmBase.deproxy(parentBase, Taxon.class);
                    return parentTaxon;
                } else {
                    logger.warn(kewId + "Parent is synonym " + line);
                    return null;
                }
            }else{
                UUID rootTaxonUuid = state.getConfig().getRootTaxonUuid();
                return CdmBase.deproxy(getTaxonService().find(rootTaxonUuid), Taxon.class);
            }
        }else if ("Synonym".equals(statusStr)){
            //not relevant
            return null;
        }else{
            logger.warn(kewId + "Parent not retrieved" + line);
            return null;
        }
    }

    private String getHigherRankName(TaxonName taxonName) {
        if (Rank.SPECIES().equals(taxonName.getRank())){
            return taxonName.getGenusOrUninomial();
        }else if (taxonName.isInfraSpecific()){
            return taxonName.getGenusOrUninomial() + " " + taxonName.getSpecificEpithet();
        }
        return null;
    }

    private void adjustSynonymType(TaxonBase<?> taxonBase, TaxonBase<?> homotypicTaxon, String line) {
        adjustSynonymTypeOrdered(taxonBase, homotypicTaxon, line);
        adjustSynonymTypeOrdered(homotypicTaxon, taxonBase, line);
    }

    private void adjustSynonymTypeOrdered(TaxonBase<?> firstTaxon, TaxonBase<?> secondTaxon, String line) {
        if (firstTaxon == null){
            logger.warn(line + "first taxon is null for adjust synonym type");
        }else if (secondTaxon == null){
            logger.warn(line + "second taxon is null for adjust synonym type");
        }else if (secondTaxon.isInstanceOf(Synonym.class)){
            Synonym syn = CdmBase.deproxy(secondTaxon, Synonym.class);
            if (firstTaxon.equals(syn.getAcceptedTaxon())){
                syn.setType(SynonymType.HOMOTYPIC_SYNONYM_OF);
            }
        }
    }

    protected TaxonBase<?> getTaxon(Map<String, String> record) {
        String kew_name_id = getValue(record, Kew_Name_ID);
        UUID taxonUuid = taxonMap.get(kew_name_id);
        TaxonBase<?> taxon = getTaxonService().find(taxonUuid);
        return taxon;
    }

	private boolean checkParsed(TaxonName name, String fullName, String nameStr, String line) {
		boolean result = true;
	    if (name.isProtectedTitleCache() || name.isProtectedFullTitleCache() || name.isProtectedNameCache()) {
			logger.warn(line + "Name could not be parsed: " + fullName);
			result = false;
		}
		Reference nomRef = name.getNomenclaturalReference();
		if (nomRef != null && (nomRef.isProtectedTitleCache()
		        || nomRef.getInReference() != null && nomRef.getInReference().isProtectedTitleCache())){
		    logger.warn(line + "Nom ref could not be parsed: " + fullName);
            result = false;
		}
		if (nameStr != null && !name.getTitleCache().equals(nameStr)){
            logger.warn(line + "Name part not parsed correctly: " + name.getTitleCache() + "<-> expected: " + nameStr);
            result = false;
        }
		return result;
	}

    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record) {
        if (this.secReference == null){
            logger.warn("Load sec ref");
            String secUuid = record.get(Sec_Ref_CDM_UUID);
            secReference = getReferenceService().load(UUID.fromString(secUuid));
            if (this.secReference == null){
                logger.warn("Sec ref is null");
            }
        }
        return this.secReference;
    }

    private Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
        }
        return this.sourceReference;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        logger.warn("Loaded persistent reference: "+ reference.getUuid());
        if (result == null){
            logger.warn("Persistent reference is null: " + reference.getUuid());
            result = reference;
            getReferenceService().saveOrUpdate(result);
        }
        return result;
    }

    private void replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, INonViralName name) {
        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
    }


    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
    	String noStr = getValue(state.getOriginalRecord(), Kew_Name_ID);
        return IdentifiableSource.NewDataImportInstance(noStr, Kew_Name_ID, getSourceCitation(state));
    }
}
