/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.validate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.PesiDestinations;
import eu.etaxonomy.cdm.app.pesi.EuroMedSourceActivator;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * Tests the E+M -> PESI pipeline by comparing the source DB with destination PESI DB.
 *
 * @author a.mueller
 * @since 08.10.2019
 */
public class PesiEuroMedValidator extends PesiValidatorBase {

    private static Logger logger = LogManager.getLogger();

    private static final ICdmDataSource defaultSource = CdmDestinations.cdm_test_local_mysql_euromed();
//    private static final ICdmDataSource defaultSource = CdmDestinations.cdm_pesi2019_final();
    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI();
//    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI_2();

    boolean doReferences = false;
    boolean doTaxa = true;
    boolean doTaxRels = false;
    boolean doDistributions = false;
    boolean doCommonNames = false;
    boolean doNotes = false;
    boolean doAdditionalTaxonSources = false;

    private Source source = new Source(defaultSource);
    private Source destination = defaultDestination;

    private String origEuroMed = "OriginalDB = 'E+M' ";

    public void invoke(Source source, Source destination){
        logger.warn("Validate destination " +  destination.getDatabase());
        boolean success = true;
        try {
            this.source = source;
            this.destination = destination;
            success &= testReferences();
            success &= testTaxa();
            success &= testTaxonRelations();
            success &= testDistributions();
            success &= testCommonNames();
            success &= testNotes();
            success &= testAdditionalTaxonSources();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        //TBC
        System.out.println("end validation " + (success? "":"NOT ") + "successful.");
    }

    private boolean testAdditionalTaxonSources() throws SQLException {
        if (!doAdditionalTaxonSources){
            System.out.println("Ignore validate additional taxon sources");
            return true;
        }
        System.out.println("Start validate additional taxon sources");
        boolean success = testAdditionalTaxonSourcesCount();
        if (success){
              success &= testSingleAdditionalTaxonSources(source.getUniqueInteger(countAddtionalTaxonSource));
        }
        return success;
    }

    private boolean testNotes() throws SQLException {
        if (!doNotes){
            System.out.println("Ignore validate notes");
            return true;
        }
        System.out.println("Start validate notes");
        boolean success = testNotesCount();
        if (success){
              success &= testSingleNotes(source.getUniqueInteger("SELECT count(*) FROM notes "));
        }
        return success;
    }

    private boolean testDistributions() throws SQLException {
        if (!doDistributions){
            System.out.println("Ignore validate distributions");
            return true;
        }
        System.out.println("Start validate distributions");
        boolean success = testDistributionCount();
        if (!success){
              success &= testSingleDistributions(source.getUniqueInteger(distributionCountSQL));
        }
        return success;
    }

    private boolean testCommonNames() throws SQLException {
        if (!doCommonNames){
            System.out.println("Ignore validate common names");
            return true;
        }
        System.out.println("Start validate common names");
        boolean success = testCommonNameCount();
        if (success){
            success &= testSingleCommonNames(source.getUniqueInteger("SELECT count(*) FROM vernaculars "));
        }
        return success;
    }

    int countSynonyms;
    int countIncludedIns;
    private boolean testTaxonRelations() throws SQLException {
        if (!doTaxRels){
            System.out.println("Ignore validate taxon relations");
            return true;
        }
        System.out.println("Start validate taxon relations");
        boolean success = testSynonymRelations();
        success &= testIncludedInRelations();
        success &= testTotalRelations();
        success &= testNameRelations();
        return success;
    }

    private boolean testTotalRelations() {
        if (!(countSynonyms < 0 || countIncludedIns < 0)){
            int countTotalSrc = countSynonyms + countIncludedIns;
            int countSrc = source.getUniqueInteger("SELECT count(*) FROM tu ");
            boolean success = equals("Taxrel count + 1 must be same as source taxon count ", countTotalSrc+1, countSrc, String.valueOf(-1));
            int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon t WHERE t."+ origEuroMed);
            success &= equals("Taxrel count + 1 must be same as destination taxon count ", countTotalSrc+1, countDest, String.valueOf(-1));
            return success;
        }else{
            return false;
        }
    }

    private final String countSynonymRelation = "SELECT count(*) FROM TaxonBase syn LEFT JOIN TaxonBase acc ON syn.acceptedTaxon_id = acc.id WHERE syn.publish = 1 AND acc.publish = 1 ";
    private boolean testSynonymRelations() throws SQLException {

        int countSrc = source.getUniqueInteger(countSynonymRelation);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk > 101");
        boolean success = equals("Synonym count ", countSrc, countDest, String.valueOf(-1));
        if (success){
            //TODO test single synonym relations
            success &= testSingleSynonymRelations(source.getUniqueInteger(countSynonymRelation));
        }
        countSynonyms = (countSrc == countDest)? countSrc : -1;
        return success;
    }

    private boolean testSingleSynonymRelations(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.id tid, pt.id pid "
                + " FROM TaxonNode tn "
                + "   INNER JOIN TaxonBase t ON tn.taxon_id = t.id "
                + "   LEFT JOIN TaxonNode ptn ON ptn.id = tn.parent_id "
                + "   LEFT JOIN TaxonBase  pt ON ptn.taxon_id = pt.id "
                + " WHERE t.publish = 1 && pt.publish = 1 "
                + " ORDER BY CAST(tb.id as char(20)) ");

        ResultSet destRS = destination.getResultSet("SELECT rel.*, t1.IdInSource t1Id, t2.IdInSource t2Id "
                + " FROM RelTaxon rel "
                + "    LEFT JOIN Taxon t1 ON t1.TaxonId = rel.TaxonFk1 "
                + "    LEFT JOIN Taxon t2 ON t2.TaxonId = rel.TaxonFk2 "
                + " WHERE t1."+origEuroMed+" AND t2." + origEuroMed + " AND RelTaxonQualifierFk > 101 "
                + " ORDER BY t1.IdInSource");
        int i = 0;
        while (srcRS.next() && destRS.next()){
            success &= testSingleSynonymRelation(srcRS, destRS);
            i++;
        }
        success &= equals("Synonym relation count for single compare", n, i, String.valueOf(-1));
        return success;
    }

    private boolean testSingleSynonymRelation(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Taxon relation taxon1", "NameId: " + srcRS.getInt("id"), destRS.getString("t1Id"), id);
        success &= equals("Taxon relation taxon2", "NameId: " + srcRS.getInt("tu_accfinal"), destRS.getString("t2Id"), id);
        success &= equals("Taxon relation qualifier fk", PesiTransformer.IS_SYNONYM_OF, destRS.getInt("RelTaxonQualifierFk"), id);
        success &= equals("Taxon relation qualifier cache", "is synonym of", destRS.getString("RelQualifierCache"), id);
        //TODO enable after next import
//        success &= isNull("notes", destRS);
        //complete if no further relations need to added
        return success;
    }

    private boolean testNameRelations() {
        //Name relations
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM NameRelationship WHERE ("
               + " 1=1 "
                 + ")");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk <100 ");
        boolean success = equals("Taxon name relation count ", countSrc, countDest, String.valueOf(-1));
        if (success){
            //TODO test single name relation
//            success &= testSingleNameRelations(source.getUniqueInteger(countSynonymRelation));
        }
        return success;
    }

    private final String countParentRelation  = "SELECT count(*) "
            + " FROM TaxonNode tn "
            + " INNER JOIN TaxonBase tb ON tn.taxon_id = tb.id "
            + "   LEFT JOIN TaxonNode ptn ON ptn.id = tn.parent_id "
            + "   LEFT JOIN TaxonBase pt ON ptn.taxon_id = pt.id "
            + " WHERE tb.publish = 1 && pt.publish = 1  ";

    private boolean testIncludedInRelations() throws SQLException {
        int countSrc = source.getUniqueInteger(countParentRelation);
        int  countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk = 101 ");
        boolean success = equals("Tax included in count ", countSrc, countDest, String.valueOf(-1));
        if (success){
            success &= testSingleTaxonRelations(source.getUniqueInteger(countParentRelation));
        }
        countIncludedIns = (countSrc == countDest)? countSrc : -1;
        return success;
    }

    private boolean testTaxa() throws SQLException {
        if (!doTaxa){
            System.out.println("Ignore validate taxa");
            return true;
        }
        System.out.println("Start validate taxa");
        boolean success = testTaxaCount();
        if (success){
            success &= testSingleTaxa(source.getUniqueInteger(countTaxon));
        }
        return success;
    }

    String countReferencesStr = "SELECT count(*) FROM reference ";
    private boolean testReferences() throws SQLException {
        if (!doReferences){
            System.out.println("Ignore validate references");
            return true;
        }
        System.out.println("Start validate references");
        boolean success = testReferenceCount();
        if (success){
            success &= testSingleReferences(source.getUniqueInteger(countReferencesStr));
        }
        return success;
    }

    private final String countAddtionalTaxonSource = "SELECT count(*) FROM tu_sources ts ";
    private boolean testAdditionalTaxonSourcesCount() {
        int countSrc = source.getUniqueInteger(countAddtionalTaxonSource);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM AdditionalTaxonSource ");
        return equals("AdditionalTaxonSource count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testNotesCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM notes ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE (1=1) ");
        boolean result = equals("Notes count ", countSrc, countDest, String.valueOf(-1));

        return result;
    }

    private String distributionCountWhere = " WHERE deb.DTYPE = 'Distribution' AND tb.publish = 1 AND a.uuid NOT IN ("
            + "'111bdf38-7a32-440a-9808-8af1c9e54b51',"   //E+M
            //Former UUSR
            + "'c4a898ce-0f32-44fe-a8a3-278e11a4ba53','a575d608-dd53-4c01-b2af-5067d0711f64','da4e9cc3-b1cc-403a-81ff-bcc5d9fadbd1',"
            + "'7e0f8fa3-5db9-48f0-9fa8-87fcab3eaa53','2188e3a5-0446-47c8-b11b-b4b2b9a71c75','44f262e3-5091-4d28-8081-440d3978fb0b',"
            + "'efabc8fd-0b3c-475b-b532-e1ca0ba0bdbb') ";
    private String distributionCountSQL = "SELECT count(*) as n "
            + " FROM DescriptionElementBase deb INNER JOIN DescriptionBase db ON deb.inDescription_id = db.id "
            + "    LEFT JOIN TaxonBase tb ON db.taxon_id = tb.id "
            + "    LEFT JOIN DefinedTermBase a ON a.id = deb.area_id "
            + distributionCountWhere;
    private boolean testDistributionCount() {
        int countSrc = source.getUniqueInteger(distributionCountSQL);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Occurrence ");
        return equals("Occurrence count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testCommonNameCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM vernaculars ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM CommonName ");
        return equals("CommonName count ", countSrc, countDest, String.valueOf(-1));
    }

    private final String countTaxon = "SELECT count(*) FROM TaxonBase tb WHERE tb.publish = 1 ";
    private final String destTaxonFilter = "(t.SourceFk IS NOT NULL OR t.AuthorString like 'auct.%' OR t.AuthorString like 'sensu %')";
    private boolean testTaxaCount() {
         int countSrc = source.getUniqueInteger(countTaxon);
         int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon t WHERE "+ destTaxonFilter);
         boolean result = equals("Taxon count ", countSrc, countDest, String.valueOf(-1));
         return result;
    }

    private boolean testSingleTaxa(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT CAST(tn.id as char(20)) tid, tb.uuid as GUID, pt.id parentId, "
                + "      tn.rank_id, rank.titleCache rank_name, "
                + "      sec.titleCache secTitle, secAut.titleCache secAutTitle, "
                + "      tn.genusOrUninomial, tn.infraGenericEpithet, tn.specificEpithet, tn.infraSpecificEpithet, "
                + "      tn.nameCache, tn.authorshipCache, tn.titleCache nameTitleCache, tn.fullTitleCache nameFullTitleCache, "
                + "      tb.DTYPE taxStatus, tb.titleCache, tb.appendedPhrase tbAppendedPhrase, tb.sec_id secId, "
                + "      taxRelType.uuid taxRelTypeUuid, tr.relatedTo_id relToTaxonId, "
                + "      nsType.id nsId, nsType.idInVocabulary nsTitle, "
                + "      typeName_id, typeName.titleCache typeFullNameCache, "
                + "      CASE WHEN tb.updated IS NOT NULL THEN tb.updated ELSE tb.created END as lastActionDate, "
                + "      CASE WHEN tb.updated IS NOT NULL THEN 'changed' ELSE 'created' END as lastAction "
                + " FROM TaxonBase tb "
                + "     LEFT JOIN TaxonName tn on tb.name_id = tn.id "
                + "     LEFT JOIN DefinedTermBase rank ON rank.id = tn.rank_id "
                + "     LEFT JOIN Reference sec ON sec.id = tb.sec_id "
                + "     LEFT JOIN AgentBase secAut ON secAut.id = sec.authorship_id "
                + "     LEFT JOIN NomenclaturalStatus ns ON ns.name_id = tn.id "
                + "     LEFT JOIN DefinedTermBase nsType ON nsType.id = ns.type_id "
                + "     LEFT JOIN TaxonName_TypeDesignationBase typeMN ON typeMN.TaxonName_id = tn.id "
                + "     LEFT JOIN TypeDesignationBase td ON td.id = typeMN.typedesignations_id "
                + "     LEFT JOIN TaxonName typeName ON typeName.id = td.typeName_id "
                + "     LEFT JOIN TaxonNode n ON n.taxon_id = tb.id "
                + "     LEFT JOIN TaxonNode ptn ON n.parent_id = ptn.id "
                + "     LEFT JOIN TaxonBase pt ON pt.id = ptn.taxon_id AND pt.publish = 1 "
                + "     LEFT JOIN TaxonRelationship tr ON tr.relatedFrom_id = tb.id "
                + "     LEFT JOIN TaxonBase tbRelTo ON tr.relatedTo_id = tbRelTo.id "
                + "     LEFT JOIN DefinedTermBase taxRelType ON taxRelType.id = tr.type_id"
                + " WHERE tb.publish = 1 "
                + " GROUP BY tid, GUID, tn.rank_id, rank.titleCache, secTitle,"
                + "      tn.genusOrUninomial, tn.infraGenericEpithet, tn.specificEpithet, tn.infraSpecificEpithet, "
                + "      tn.nameCache, tn.authorshipCache, tn.titleCache, "
                + "      tb.DTYPE, tb.updated, tb.created "    //for duplicates caused by >1 name status
                + " ORDER BY tid, GUID, lastActionDate ");
        ResultSet destRS = destination.getResultSet("SELECT t.*, "
                + "     pt.treeIndex pTreeIndex, pt.IdInSource parentSourceId, "  //not needed
                + "     s.Name as sourceName, type.IdInSource typeSourceId, r.Rank "
                + " FROM Taxon t "
                + "    LEFT JOIN Taxon pt ON pt.TaxonId = t.ParentTaxonFk "
                + "    LEFT JOIN Taxon type ON type.TaxonId = t.TypeNameFk "
                + "    LEFT JOIN Rank r ON r.RankId = t.RankFk AND r.KingdomId = t.KingdomFk "
                + "    LEFT JOIN Source s ON s.SourceId = t.SourceFk "
                + " WHERE t."+ origEuroMed + " AND " + destTaxonFilter   //FIXME remove SourceFk filter is only preliminary for first check
                + " ORDER BY t.IdInSource, t.GUID, t.LastActionDate, AuthorString ");
        int i = 0;
        logger.error("remove SourceFk filter is only preliminary for first check");
        while (srcRS.next() && destRS.next()){
            success &= testSingleTaxon(srcRS, destRS);
            i++;
        }
        success &= equals("Taxon count for single compare", n, i, String.valueOf(-1));
        return success;
    }

    private boolean testSingleTaxon(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("tid"));
        //TODO decide, according to SQL it also contains the taxon UUID, but in PESI2014 backup I can't find this
        boolean success = equals("Taxon ID", "NameId: " + srcRS.getInt("tid"), destRS.getString("IdInSource"), id);
        success &= equals("Taxon source", makeSource(srcRS), destRS.getString("sourceName"), id);

        success &= equals("Taxon kingdomFk", "3", destRS.getString("KingdomFk"), id);
//difficult to test        success &= equals("Taxon rank fk", srcRS.getString("rank_id"), destRS.getString("RankFk"), id);
        success &= equals("Taxon rank cache", normalizeRank(srcRS.getString("rank_name")), destRS.getString("Rank"), id);
        success &= equals("Taxon genusOrUninomial", srcRS.getString("genusOrUninomial"), destRS.getString("GenusOrUninomial"), id) ;
        success &= equals("Taxon infraGenericEpithet", srcRS.getString("infraGenericEpithet"), destRS.getString("InfraGenericEpithet"), id) ;
        success &= equals("Taxon specificEpithet", srcRS.getString("specificEpithet"), destRS.getString("SpecificEpithet"), id) ;
        success &= equals("Taxon infraSpecificEpithet", srcRS.getString("infraSpecificEpithet"), destRS.getString("InfraSpecificEpithet"), id) ;

        success &= equals("Taxon websearchname", srcRS.getString("nameCache"), destRS.getString("WebSearchName"), id);
//TODO     success &= equals("Taxon WebShowName", srcRS.getString("tu_displayname"), destRS.getString("WebShowName"), id);
        success &= equals("Taxon authority", makeAuthorship(srcRS), destRS.getString("AuthorString"), id);
        success &= equals("Taxon FullName", makeFullName(srcRS), destRS.getString("FullName"), id);
        success &= equals("Taxon NomRefString", makeNomRefString(srcRS), destRS.getString("NomRefString"), id);
//      success &= equals("Taxon DisplayName", makeDisplayName(srcRS), destRS.getString("DisplayName"), id);  //in ERMS according to SQL script same as FullName, no nom.ref. information attached
//difficult to test   success &= equals("Taxon NameStatusFk", nullSafeInt(srcRS, "nsId"),nullSafeInt( destRS,"NameStatusFk"), id);
        success &= equals("Taxon NameStatusCache", srcRS.getString("nsTitle"), destRS.getString("NameStatusCache"), id);

        //TODO mostly Taxonomically Valueless
//        success &= equals("Taxon TaxonStatusFk", mapTaxStatusFk(srcRS.getString("taxStatus"), srcRS.getString("taxRelTypeUuid")), nullSafeInt( destRS,"TaxonStatusFk"), id);
//        success &= equals("Taxon TaxonStatusCache", mapTaxStatus(srcRS.getString("taxStatus"), srcRS.getString("taxRelTypeUuid")), destRS.getString("TaxonStatusCache"), id);

        success &= equals("Taxon ParentTaxonFk", nullSafeInt(srcRS, "parentId"), nullSafeInt(destRS, "ParentTaxonFk"), id);

        Integer origTypeNameFk = nullSafeInt(srcRS, "typeName_id");
        success &= equals("Taxon TypeNameFk", origTypeNameFk == null? null : "NameId: " + origTypeNameFk, destRS.getString("typeSourceId"), id);
        success &= equals("Taxon TypeFullNameCache", srcRS.getString("typeFullNameCache"), destRS.getString("TypeFullNameCache"), id);
        //according to SQL always constant, could be changed in future
        success &= equals("Taxon QualityStatusFK", 2, nullSafeInt( destRS,"QualityStatusFk"), String.valueOf(id));
        success &= equals("Taxon QualityStatusCache", "Added by Database Management Team", destRS.getString("QualityStatusCache"), id);
        success &= testTreeIndex(destRS, "TreeIndex", "pTreeIndex", id);
        success &= isNull("FossilStatusFk", destRS, id);
        success &= isNull("FossilStatusCache", destRS, id);
        success &= equals("Taxon GUID", srcRS.getString("GUID"), destRS.getString("GUID"), id);
        success &= equals("Taxon DerivedFromGuid", srcRS.getString("GUID"), destRS.getString("DerivedFromGuid"), id); //according to SQL script GUID and DerivedFromGuid are always the same, according to 2014DB this is even true for all databases
        success &= isNull("ExpertGUID", destRS, id);  //according to SQL + PESI2014
        success &= isNull("SpeciesExpertGUID", destRS, id);
        //ExpertName = SpeciesExpertName in E+M according to SQL script, 4689x NULL
        success &= equals("Taxon ExpertName", makeExpertName(srcRS), destRS.getString("ExpertName"), id);
        success &= equals("Taxon SpeciesExpertName", makeExpertName(srcRS), destRS.getString("SpeciesExpertName"), id);
//FIXME !!        success &= equals("Taxon cache citation", srcRS.getString("secTitle"), destRS.getString("CacheCitation"), id);
        success &= equals("Taxon Last Action", srcRS.getString("lastAction"),  destRS.getString("LastAction"), id);
        success &= equals("Taxon Last Action Date", srcRS.getTimestamp("lastActionDate"),  destRS.getTimestamp("LastActionDate"), id);

        success &= isNull("GUID2", destRS, id);  //only relevant after merge
        success &= isNull("DerivedFromGuid2", destRS, id);  //only relevant after merge
        return success;
    }

    private String makeExpertName(ResultSet srcRs) throws SQLException {
        String autStr = srcRs.getString("secAutTitle");
        if (autStr != null){
            return autStr;
        }else{
            return srcRs.getString("secTitle");
        }
    }

    private String makeSource(ResultSet srcRs) throws SQLException {
        String secStr = srcRs.getString("secTitle");
        if (secStr == null){
            return EuroMedSourceActivator.sourceReferenceTitle;
        }else{
            return secStr;
        }
    }

    private String makeAuthorship(ResultSet srcRs) throws SQLException {
        boolean isMisapplied = isMisapplied(srcRs);
        if (isMisapplied){
            String result = getMisappliedAuthor(srcRs).trim();
            return result;
        }else{
            return srcRs.getString("authorshipCache");
        }
    }

    private String makeFullName(ResultSet srcRs) throws SQLException {
        boolean isMisapplied = isMisapplied(srcRs);
        if (isMisapplied){
            String result = srcRs.getString("nameCache");
            result += getMisappliedAuthor(srcRs);
            return result;
        }else{
            return srcRs.getString("nameTitleCache");
        }
    }

    private String makeNomRefString(ResultSet srcRS) throws SQLException {
        //there is no pure nomRefString field in CDM and also computing is only possible
        //with cache strategy which requires a running CDM instance. So this is a workaround
        //that maybe needs to be adapted
        String result = null;
        String fullTitle = srcRS.getString("nameFullTitleCache");
        String nameTitleCache = srcRS.getString("nameTitleCache");
        String nameStatus = CdmUtils.Nz(srcRS.getString("nsTitle"));
        if (fullTitle != null && nameTitleCache != null){
            result = fullTitle.substring(nameTitleCache.length())
                    .replaceAll("^, ", "")
                    .replaceAll("(, |^)"+nameStatus+"$", "")
                    .replaceAll("\\[as \".*\"\\]", "")
                    .replaceAll(", nom\\. cons\\., nom\\. altern\\.$", "")  //single case with 2 nom. status
                    .trim();
        }
        return result;
    }

    private String mapTaxStatus(String dtype, String taxRelTypeUuidStr) {
        Integer statusFk = mapTaxStatusFk(dtype, taxRelTypeUuidStr);
        if (statusFk == null){
            return null;
        }else if (statusFk == PesiTransformer.T_STATUS_ACCEPTED){
            return "accepted";
        }else if (statusFk == PesiTransformer.T_STATUS_SYNONYM){
            return "synonym";
        }else if (statusFk == PesiTransformer.T_STATUS_PRO_PARTE_SYN){
            return "pro parte synonym";
        }else if (statusFk == PesiTransformer.T_STATUS_PARTIAL_SYN){
            return "partial synonym";
        }
        return null;
    }

    private Integer mapTaxStatusFk(String dtype, String taxRelTypeUuidStr) {
        if (dtype == null){
            return null;
        }else if ("Synonym".equals(dtype)){
            return PesiTransformer.T_STATUS_SYNONYM;
        }else if ("Taxon".equals(dtype)){
            UUID relTypeUuid = taxRelTypeUuidStr == null? null: UUID.fromString(taxRelTypeUuidStr);
            if (TaxonRelationshipType.proParteUuids().contains(relTypeUuid)){
                return PesiTransformer.T_STATUS_PRO_PARTE_SYN;
            }else if (TaxonRelationshipType.partialUuids().contains(relTypeUuid)){
                return PesiTransformer.T_STATUS_PARTIAL_SYN;
            }else if (TaxonRelationshipType.misappliedNameUuids().contains(relTypeUuid)){
                return PesiTransformer.T_STATUS_SYNONYM;  //no explicit MAN status exists in PESI
            }else{
                return PesiTransformer.T_STATUS_ACCEPTED;
            }
        }
        return null;
    }

    private String normalizeRank(String rankStr) {
        if (rankStr == null){return null;
        }else if (rankStr.equals("Convar")){return "Convariety";
        }else if (rankStr.equals("Unranked (infrageneric)")){return "Tax. infragen.";
        }else if (rankStr.equals("Unranked (infraspecific)")){return "Tax. infraspec.";
        }else if (rankStr.equals("Coll. species")){return "Coll. Species";
        }else if (rankStr.equals("Species Aggregate")){return "Aggregate";
        }else if (rankStr.equals("Subsection bot.")){return "Subsection";
        }return rankStr;
    }

    private String makeDisplayName(ResultSet srcRs) throws SQLException {
        boolean isMisapplied = isMisapplied(srcRs);

        String result;
        String nameTitle = srcRs.getString("nameTitleCache");
        String nameCache = srcRs.getString("nameCache");
        if(!isMisapplied){
            result = srcRs.getString("nameFullTitleCache");
            String taggedName = getTaggedNameTitle(nameCache, nameTitle);
            result = result.replace(nameTitle, taggedName);
            result = result.replaceAll("^<i>"+ UTF8.HYBRID , UTF8.HYBRID+ "<i>").replaceAll(" "+ UTF8.HYBRID, "</i> "+UTF8.HYBRID+"<i>");
        }else{
            result = srcRs.getString("nameCache");
            String taggedName = getTaggedNameTitle(nameCache, nameCache);
            result = result.replace(nameCache, taggedName);
            //misapplied
            result += getMisappliedAuthor(srcRs);
        }
        String nameStatus = CdmUtils.Nz(srcRs.getString("nsTitle"));
        result = result.replaceAll("(, |^)"+nameStatus+"$", "");
        return result;
    }

    private boolean isMisapplied(ResultSet srcRs) throws SQLException {
        String relTypeUuid = srcRs.getString("taxRelTypeUuid");
        boolean isMisapplied = relTypeUuid!=null
                && (relTypeUuid.equals(TaxonRelationshipType.uuidMisappliedNameFor.toString())
                   || relTypeUuid.equals(TaxonRelationshipType.uuidProParteMisappliedNameFor.toString())
                   || relTypeUuid.equals(TaxonRelationshipType.uuidPartialMisappliedNameFor.toString()))
                //TODO formatting of ppMANs not yet implemented
                && nullSafeInt(srcRs, "relToTaxonId") != null;
        return isMisapplied;
    }

    private String getMisappliedAuthor(ResultSet srcRs) throws SQLException {
        String result;
        String relAppendedPhrase = srcRs.getString("tbAppendedPhrase");
        String secId = srcRs.getString("secId");
        String secTitle = srcRs.getString("secTitle");
        if(relAppendedPhrase == null && secId == null) {
            result = " auct.";
        }else if (relAppendedPhrase != null && secId == null){
            result = " " + relAppendedPhrase;
        }else if (relAppendedPhrase == null && secId != null){
            result = " sensu " + secTitle;
        }else{
            result = " " + relAppendedPhrase + " " + secTitle;
        }
        String authorship = srcRs.getString("authorshipCache");
        if (isNotBlank(authorship)){
            result += ", non " + authorship;
        }
        return result;
    }

    private String getTaggedNameTitle(String nameCache, String nameTitle) {
        if (nameCache == null){
            logger.warn("NameCache is null");
            return nameTitle;
        }
        String result = null;
        try {
            String[] nameCacheSplit = nameCache.split(" ");
            String[] nameTitleSplit = nameTitle.split(" ");
            result = "";
            boolean currentIsName = false;
            for (int i=0, j=0; j < nameTitleSplit.length; j++){
                if (i < nameCacheSplit.length && nameCacheSplit[i].equals(nameTitleSplit[j])
                        && !isMarker(nameCacheSplit[i])){
                    if(!currentIsName){
                        result += " <i>" + nameCacheSplit[i];
                        currentIsName = true;
                    }else{
                        result += " " + nameCacheSplit[i];
                    }
                    if((j+1)==nameTitleSplit.length){
                        result += "</i>";
                    }
                    i++;
                }else{
                    if(currentIsName){
                        result += "</i>";
                        currentIsName = false;
                    }
                    result += " " + nameTitleSplit[j];
                    if (i < nameCacheSplit.length && nameCacheSplit[i].equals(nameTitleSplit[j])
                            && isMarker(nameCacheSplit[i])){
                        i++;
                    }
                }
            }
            return result.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    private boolean isMarker(String nameCacheSplit) {
        return nameCacheSplit.endsWith(".") || nameCacheSplit.equals("[unranked]")
                || nameCacheSplit.equals("grex")|| nameCacheSplit.equals("proles")
                || nameCacheSplit.equals("race");
    }

    private boolean testSingleTaxonRelations(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.name_id tid, pt.name_id pid "
                + " FROM TaxonNode tn "
                + "   INNER JOIN TaxonBase t ON tn.taxon_id = t.id "
                + "   LEFT JOIN TaxonNode ptn ON ptn.id = tn.parent_id "
                + "   LEFT JOIN TaxonBase  pt ON ptn.taxon_id = pt.id "
                + " WHERE t.publish = 1 && pt.publish = 1 "
                + " ORDER BY CAST(t.name_id as char(20)) ");

        ResultSet destRS = destination.getResultSet("SELECT rel.*, t1.IdInSource t1Id, t2.IdInSource t2Id "
                + " FROM RelTaxon rel "
                + "    LEFT JOIN Taxon t1 ON t1.TaxonId = rel.TaxonFk1 "
                + "    LEFT JOIN Taxon t2 ON t2.TaxonId = rel.TaxonFk2 "
                + " WHERE t1."+origEuroMed+" AND t2." + origEuroMed + " AND RelTaxonQualifierFk = 101 "
                + " ORDER BY t1.IdInSource");
        int i = 0;
        while (srcRS.next() && destRS.next()){
            success &= testSingleTaxonRelation(srcRS, destRS);
            i++;
        }
        success &= equals("Taxon relation count for single compare", n, i, String.valueOf(-1));
        return success;
    }

    private boolean testSingleTaxonRelation(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("tid"));
        boolean success = equals("Taxon relation taxon1", "NameId: " + srcRS.getInt("tid"), destRS.getString("t1Id"), id);
        success &= equals("Taxon relation taxon2", "NameId: " + srcRS.getInt("pid"), destRS.getString("t2Id"), id);
        success &= equals("Taxon relation qualifier fk", PesiTransformer.IS_TAXONOMICALLY_INCLUDED_IN, destRS.getInt("RelTaxonQualifierFk"), id);
        success &= equals("Taxon relation qualifier cache", "is taxonomically included in", destRS.getString("RelQualifierCache"), id);
        //TODO enable after next import
        success &= isNull("notes", destRS, id);
        //complete if no further relations need to added
        return success;
    }

    private boolean testSingleAdditionalTaxonSources(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT CAST(tu.id as char(20)) tuId, MN.*, s.*, su.sourceuse_name "
                + " FROM tu_sources MN INNER JOIN tu ON MN.tu_id = tu.id "
                + "    LEFT JOIN sources s ON s.id = MN.source_id "
                + "    LEFT JOIN sourceuses su ON MN.sourceuse_id = su.sourceuse_id "
                + " ORDER BY CAST(tu.id as char(20)), MN.sourceuse_id, s.id ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, ats.*, s.*, su.* "
                + " FROM AdditionalTaxonSource ats INNER JOIN Taxon t ON t.TaxonId = ats.TaxonFk "
                + "    INNER JOIN Source s ON s.SourceId = ats.SourceFk "
                + "    LEFT JOIN SourceUse su ON su.SourceUseId = ats.SourceUseFk "
                + " WHERE t."+origEuroMed
                + " ORDER BY t.IdInSource, su.SourceUseId, s.RefIdInSource ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleAdditionalTaxonSource(srcRs, destRs);
            count++;
        }
        success &= equals("Notes count differs", n, count, "-1");
        return success;
    }

    private boolean testSingleAdditionalTaxonSource(ResultSet srcRs, ResultSet destRs) throws SQLException {
        String id = String.valueOf(srcRs.getInt("tuId") + "-" + srcRs.getString("sourceuse_name"));
        boolean success = equals("Additional taxon source taxonID ", "tu_id: " + String.valueOf(srcRs.getInt("tuId")), destRs.getString("IdInSource"), id);
        success &= equals("Additional taxon source fk ", srcRs.getString("source_id"), destRs.getString("RefIdInSource"), id);  //currently we use the same id in ERMS and PESI
        success &= equals("Additional taxon source use fk ", srcRs.getString("sourceuse_id"), destRs.getString("SourceUseFk"), id);
        success &= equals("Additional taxon source use cache ", srcRs.getString("sourceuse_name"), destRs.getString("SourceUseCache"), id);
        //TODO some records are still truncated ~ >820 characters
        success &= equals("Additional taxon source name cache ", srcRs.getString("source_name"), destRs.getString("SourceNameCache"), id);
        success &= equals("Additional taxon source detail ", srcRs.getString("pagenr"), destRs.getString("SourceDetail"), id);
        //Complete
        return success;
    }

    private boolean testSingleNotes(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT CAST(tu.id as char(20)) tuId, no.*, l.LanName "
                + " FROM notes no INNER JOIN tu ON no.tu_id = tu.id "
                + "    LEFT JOIN languages l ON l.LanID = no.lan_id "
                + " ORDER BY CAST(tu.id as char(20)), no.type, no.noteSortable ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, no.*, cat.NoteCategory, l.Language "
                + " FROM Note no INNER JOIN Taxon t ON t.TaxonId = no.TaxonFk "
                + "    LEFT JOIN NoteCategory cat ON cat.NoteCategoryId = no.NoteCategoryFk "
                + "    LEFT JOIN Language l ON l.LanguageId = no.LanguageFk "
                + " WHERE t." + origEuroMed
                + "      AND NOT (NoteCategoryFk = 4 AND no.LastAction IS NULL) AND NOT NoteCategoryFk IN (22,23,24) "
                + " ORDER BY t.IdInSource, no.NoteCategoryCache, Note_1  ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleNote(srcRs, destRs);
            count++;
        }
        success &= equals("Notes count differs", n, count, "-1");
        return success;
    }

    private boolean testSingleNote(ResultSet srcRs, ResultSet destRs) throws SQLException {
        String id = String.valueOf(srcRs.getInt("tuId") + "-" + srcRs.getString("type"));
        boolean success = equals("Note taxonID ", "tu_id: " + String.valueOf(srcRs.getInt("tuId")), destRs.getString("IdInSource"), id);
        success &= equals("Note Note_1 ", srcRs.getString("note"), destRs.getString("Note_1"), id);
        success &= isNull("Note_2", destRs, id);
        success &= equals("Note category cache", normalizeNoteCatCache(srcRs.getString("type")), destRs.getString("NoteCategoryCache"), id);
        success &= equals("Note language ", srcRs.getString("LanName"), destRs.getString("Language"), id);
        success &= isNull("Region", destRs, id);
        success &= isNull("SpeciesExpertGUID", destRs, id);
        //SpeciesExpertName, LastAction, LastActionDate handled in separate method
        //complete
        return success;
    }

    private String normalizeNoteCatCache(String string) {
        return StringUtils.capitalize(string)
                .replace("Original Combination", "Original combination")
                .replace("Taxonomic remark", "Taxonomic Remark");
    }

    private boolean testSingleDistributions(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet(
                  " SELECT CAST(tb.name_id as char(20)) AS tid, a.idInVocabulary, a.titleCache area, st.uuid statusUuid, "
                + "        CASE WHEN deb.updated IS NOT NULL THEN deb.updated ELSE deb.created END as lastActionDate, "
                + "        CASE WHEN deb.updated IS NOT NULL THEN 'changed' ELSE 'created' END as lastAction "
                + " FROM DescriptionElementBase deb INNER JOIN DescriptionBase db ON deb.inDescription_id = db.id "
                + "    LEFT JOIN TaxonBase tb ON db.taxon_id = tb.id "
                + "    LEFT JOIN DefinedTermBase a ON a.id = deb.area_id "
                + "    LEFT JOIN DefinedTermBase st ON st.id = deb.status_id "
                + distributionCountWhere
                + " ORDER BY CAST(tb.name_id as char(20)), a.idInVocabulary, a.titleCache ");
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, a.AreaEmCode, oc.*, a.AreaName "
                + " FROM Occurrence oc INNER JOIN Taxon t ON t.TaxonId = oc.TaxonFk "
                + "    LEFT JOIN Area a ON a.AreaId = oc.AreaFk "
                + " WHERE t." + origEuroMed
                + " ORDER BY t.IdInSource, a.AreaEmCode, a.AreaName, oc.Notes ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleDistribution(srcRs, destRs);
            count++;
        }
        success &= equals("Distribution count differs", n, count, "-1");
        return success;
    }

    private boolean testSingleDistribution(ResultSet srcRs, ResultSet destRs) throws SQLException {
        String id = String.valueOf(srcRs.getInt("tid") + "-" + srcRs.getString("area"));
        boolean success = equals("Distribution taxonID ", "NameId: " + String.valueOf(srcRs.getInt("tid")), destRs.getString("IdInSource"), id);
        success &= equals("Distribution AreaEmCode ", srcRs.getString("idInVocabulary"), destRs.getString("AreaEmCode"), id);
//        success &= equals("Distribution area name ", normalizeDistrArea(srcRs.getString("area")), destRs.getString("AreaName"), id);
        success &= equals("Distribution area name cache", normalizeDistrArea(srcRs.getString("area")), destRs.getString("AreaNameCache"), id);
        success &= equals("Distribution OccurrenceStatusFk", mapStatus(srcRs.getString("statusUuid")), destRs.getInt("OccurrenceStatusFk"), id);
//TODO        success &= equals("Distribution OccurrenceStatusCache", "Present", destRs.getString("OccurrenceStatusCache"), id);
        success &= isNull("SourceFk", destRs, id);  //sources should be moved to extra table only, according to script there were values, but in PESI 2014 values existed only in OccurrenceSource table (for all only E+M records)
        success &= isNull("SourceCache", destRs, id);  //sources should be moved to extra table, see above
//TODO        success &= equals("Distribution notes ", srcRs.getString("note"), destRs.getString("Notes"), id);
        success &= isNull("SpeciesExpertGUID", destRs, id);  //SpeciesExpertGUID does not exist in EM and according to script
        success &= isNull("SpeciesExpertName", destRs, id);  //SpeciesExpertName does not exist in EM and according to script
        success &= equals("Distribution Last Action", srcRs.getString("lastAction"),  destRs.getString("LastAction"), id);
        success &= equals("Distribution Last Action Date", srcRs.getTimestamp("lastActionDate"),  destRs.getTimestamp("LastActionDate"), id);
        return success;
    }

    /**
     * @param string
     * @return
     */
    private Integer mapStatus(String uuidStr) {
        UUID uuid = UUID.fromString(uuidStr);
        if (uuid.equals(PresenceAbsenceTerm.uuidNativeError) ){  //native, reported in error
            return PesiTransformer.STATUS_ABSENT;
        }else if (uuid.equals(PresenceAbsenceTerm.uuidIntroducedAdventitious)  //casual, introduced adventitious
                || uuid.equals(PresenceAbsenceTerm.uuidIntroducedUncertainDegreeNaturalisation)//introduced: uncertain degree of naturalisation
                || uuid.equals(PresenceAbsenceTerm.uuidIntroduced)){
            return PesiTransformer.STATUS_INTRODUCED;
        }else if (uuid.equals(PresenceAbsenceTerm.uuidNative) ){  //native
            return PesiTransformer.STATUS_NATIVE;
        }else if (uuid.equals(PresenceAbsenceTerm.uuidNaturalised) ){  //naturalised
            return PesiTransformer.STATUS_NATURALISED;
        }else if (uuid.equals(PresenceAbsenceTerm.uuidNativePresenceQuestionable) ){  //native, presence questionable
            return PesiTransformer.STATUS_DOUBTFUL;
        }else if (uuid.equals(PresenceAbsenceTerm.uuidCultivated) ){  //cultivated
            return PesiTransformer.STATUS_MANAGED;
        }else if (uuid.equals(BerlinModelTransformer.uuidStatusUndefined) ){  //native, reported in error
            return -1;
        }

        return null;
    }

    private String normalizeDistrArea(String area) {
        if (area == null){
            return null;
        }else if ("France".equals(area)){return "French mainland";
        }else if ("France, with Channel Islands and Monaco".equals(area)){return "France";
        }else if ("Greece".equals(area)){return "Greece with Cyclades and more islands";
        }else if ("Spain, with Gibraltar and Andorra (without Bl and Ca)".equals(area)){return "Spain";
        }else if ("Italy, with San Marino and Vatican City (without Sa and Si(S))".equals(area)){return "Italy";
        }else if ("Morocco, with Spanish territories".equals(area)){return "Morocco";
        }else if ("Serbia including Kosovo and Vojvodina".equals(area)){return "Serbia including Vojvodina and with Kosovo";
        }else if ("Caucasia (Ab + Ar + Gg + Rf(CS))".equals(area)){return "Caucasus region";
        }else if ("Georgia, with Abchasia and Adzharia".equals(area)){return "Georgia";
        }else if ("Canary Is.".equals(area)){return "Canary Islands";
        }else if ("Kriti with Karpathos, Kasos & Gavdhos".equals(area)){return "Crete with Karpathos, Kasos & Gavdhos";
        }else if ("Ireland, with N Ireland".equals(area)){return "Ireland";
        }else if ("mainland Spain".equals(area)){return "Kingdom of Spain";
        }else if ("Portugal".equals(area)){return "Portuguese mainland";
        }else if ("Svalbard".equals(area)){return "Svalbard with Björnöya and Jan Mayen";
        }else if ("Norway".equals(area)){return "Norwegian mainland";
        }else if ("Ukraine".equals(area)){return "Ukraine including Crimea";
        }else if ("Türkiye-in-Europe".equals(area)){return "European Turkey";
        }else if ("Azerbaijan".equals(area)){return "Azerbaijan including Nakhichevan";
        }else if ("Ireland".equals(area)){return "Republic of Ireland";
        }else if ("France".equals(area)){return "French mainland";
        }
        return area;
    }

    private boolean testSingleCommonNames(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT v.*, ISNULL([639_3],[639_2]) iso, l.LanName, tu.id tuId "
                + " FROM vernaculars v LEFT JOIN tu ON v.tu_id = tu.id LEFT JOIN languages l ON l.LanID = v.lan_id "
                + " ORDER BY CAST(tu.id as char(20)), ISNULL([639_3],[639_2]), v.vername, v.id ");
        ResultSet destRs = destination.getResultSet("SELECT cn.*, t.IdInSource, l.ISO639_2, l.ISO639_3 "
                + " FROM CommonName cn INNER JOIN Taxon t ON t.TaxonId = cn.TaxonFk LEFT JOIN Language l ON l.LanguageId = cn.LanguageFk "
                + " WHERE t." + origEuroMed
                + " ORDER BY t.IdInSource, ISNULL("+preferredISO639+", "+alternativeISO639+"), cn.CommonName, cn.LastActionDate ");  //sorting also lastActionDate results in a minimum of exact duplicate problems
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleCommonName(srcRs, destRs);
            count++;
        }
        success &= equals("Common name count differs", n, count, "-1");
        return success;
    }

    boolean prefer639_3 = true;
    String preferredISO639 = prefer639_3? "ISO639_3":"ISO639_2";
    String alternativeISO639 = prefer639_3? "ISO639_2":"ISO639_3";

    private boolean testSingleCommonName(ResultSet srcRs, ResultSet destRs) throws SQLException {
        String id = String.valueOf(srcRs.getInt("tuId") + "-" + srcRs.getString("lan_id"));
        boolean success = equals("Common name taxonID ", "tu_id: " + String.valueOf(srcRs.getInt("tuId")), destRs.getString("IdInSource"), id);
        success &= equals("CommonName name ", srcRs.getString("vername"), destRs.getString("CommonName"), id);
        success &= equals("Common name languageFk ", srcRs.getString("iso"), getLanguageIso(destRs), id);
        success = equals("CommonName LanguageCache ", normalizeLang(srcRs.getString("LanName")), destRs.getString("LanguageCache"), id);
        //TODO cn lan_id needed? success = equals("CommonName language code ", srcRs.getString("lan_id"), destRs.getString("LanguageFk"), id);
        success &= isNull("Region", destRs, id);  //region does not seem to exist in ERMS
        //TODO cn sources, see comments
//        success &= isNull("SourceFk", destRs);  //sources should be moved to extra table, check with PESI 2014
//        success &= isNull("SourceNameCache", destRs);  //sources should be moved to extra table, check with PESI 2014
        success &= isNull("SpeciesExpertGUID", destRs, id);  //SpeciesExpertGUID does not exist in ERMS
        //SpeciesExpertName,LastAction,LastActionDate handled in separate method
        //complete
        return success;
    }

    private String normalizeLang(String string) {
        if ("Spanish".equals(string)){
            return "Spanish, Castillian";
        }else if ("Modern Greek (1453-)".equals(string)){
            return "Greek";
        }else if ("Malay (individual language)".equals(string)){
            return "Malay";
        }else if ("Swahili (individual language)".equals(string)){
            return "Swahili";
        }

        return string;
    }

    private String getLanguageIso(ResultSet destRs) throws SQLException {
        String result = destRs.getString(preferredISO639);
        if (result == null){
            result = destRs.getString(alternativeISO639);
        }
        return result;
    }

    private boolean testSingleReferences(int count) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT r.*, a.titleCache author "
                + " FROM Reference r LEFT OUTER JOIN AgentBase a ON r.authorship_id = a.id "
                + " ORDER BY r.id ");
        ResultSet destRS = destination.getResultSet("SELECT s.* FROM Source s "
                + " WHERE s." + origEuroMed
                + " ORDER BY s.RefIdInSource ");  // +1 for the source reference "erms" but this has no OriginalDB
        int i = 0;
        while (srcRS.next() && destRS.next()){
            success &= testSingleReference(srcRS, destRS);
            i++;
        }
        success &= equals("References count differs", count, i, "-1");
        return success;
    }

    private boolean testSingleReference(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Reference ID ", srcRS.getInt("id"), destRS.getInt("RefIdInSource"), id);
        success &= isNull("IMIS_Id", destRS, id);  //for E+M no IMIS id exists
        success &= equals("Reference SourceCategoryFk ", convertSourceTypeFk(srcRS.getString("refType")), destRS.getInt("SourceCategoryFk"), id);
        success &= equals("Reference SourceCategoryCache ", convertSourceTypeCache(srcRS.getString("refType")), destRS.getString("SourceCategoryCache"), id);
        success &= equals("Reference name ", srcRS.getString("titleCache"), destRS.getString("Name"), id);
        success &= equals("Reference abstract ", srcRS.getString("referenceAbstract"), destRS.getString("Abstract"), id);
        success &= equals("Reference title ", srcRS.getString("title"), destRS.getString("Title"), id);
        success &= equals("Reference author string ", srcRS.getString("author"), destRS.getString("AuthorString"), id);
        //TODO reference year
        success &= equals("Reference year ", normalizeYear(srcRS), destRS.getString("RefYear"), id);
        //FIXME reference nomrefcache
//        success &= equals("Reference NomRefCache ", srcRS.getString("abbrevTitleCache"), destRS.getString("NomRefCache"), id);
        success &= equals("Reference DOI ", srcRS.getString("doi"), destRS.getString("Doi"), id);
        success &= equals("Reference link ", srcRS.getString("uri"), destRS.getString("Link"), id);
        //TODO reference Notes
//        success &= equals("Reference note ", srcRS.getString("source_note"), destRS.getString("Notes"), id);
        //complete
        return success;
    }

    private Integer convertSourceTypeFk(String sourceType) {
        if (sourceType == null){
            return null;
        }else if ("DB".equals(sourceType)){
            return PesiTransformer.REF_DATABASE;
        }else if ("JOU".equals(sourceType)){
            return PesiTransformer.REF_JOURNAL;
        }else if ("BK".equals(sourceType)){
            return PesiTransformer.REF_BOOK;
        }else if ("GEN".equals(sourceType)){
            return PesiTransformer.REF_UNRESOLVED;
        }else if ("SER".equals(sourceType)){
//            TODO correct?
            return PesiTransformer.REF_UNRESOLVED;
        }
        return null;
    }
    private String convertSourceTypeCache(String sourceType) {
        if (sourceType == null){
            return null;
        }else if ("DB".equals(sourceType)){
            return "database";
        }else if ("JOU".equals(sourceType)){
            return "journal";
        }else if ("BK".equals(sourceType)){
            return "book";
        }else if ("SER".equals(sourceType)){
            return "published";
        }else if ("BK".equals(sourceType)){
            return "book";
        }else if ("GEN".equals(sourceType)){
            return "unresolved";
        }
        return null;
    }

    private boolean testReferenceCount() {
        int countSrc = source.getUniqueInteger(countReferencesStr);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s."+ origEuroMed);  // +1 for the source reference "erms" but this has no OriginalDB
        boolean success = equals("Reference count ", countSrc, countDest, "-1");
        return success;
    }

    private String normalizeYear(ResultSet rs) throws SQLException {
        String freetext = rs.getString("datePublished_freetext");
        if(StringUtils.isNotBlank(freetext)){
            return freetext;
        }
        String start = rs.getString("datePublished_start");
        String end = rs.getString("datePublished_end");
        if (start != null){
            start = start.substring(0,4);
        }
        if (end != null){
            end = end.substring(0,4);
        }
        String result = start == null? null: start + (end==null? "": "-"+ end);
        return result;
    }

    private boolean equals(String messageStart, Timestamp srcDate, Timestamp destDate, String id) {
        if (!CdmUtils.nullSafeEqual(srcDate, destDate)){
            LocalDate date1 = srcDate.toLocalDateTime().toLocalDate();
            LocalDate date2 = destDate.toLocalDateTime().toLocalDate();
            if (date1.equals(date2) || date1.plusDays(1).equals(date2)){
                logger.info(messageStart + " were (almost) equal: " + srcDate);
                return true;
            }else{
                String message = id + ": " + messageStart + " must be equal, but was not.\n Source: "+  srcDate + "; Destination: " + destDate;
                logger.warn(message);
                return false;
            }
        }else{
            logger.info(messageStart + " were equal: " + srcDate);
            return true;
        }
    }

    private boolean equals(String messageStart, Integer nSrc, Integer nDest, String id) {
        String strId = id.equals("-1")? "": (id+ ": ");
        if (!CdmUtils.nullSafeEqual(nSrc,nDest)){
            String message = strId+ messageStart + " must be equal, but was not.\n Source: "+  nSrc + "; Destination: " + nDest;
            logger.warn(message);
            return false;
        }else{
            logger.info(strId + messageStart + " were equal: " + nSrc);
            return true;
        }
    }

//** ************* MAIN ********************************************/

    public static void main(String[] args){
        PesiEuroMedValidator validator = new PesiEuroMedValidator();
        validator.invoke(new Source(defaultSource), defaultDestination);
        System.exit(0);
    }
}
