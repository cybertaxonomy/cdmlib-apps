/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.erms.validation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.pesi.PesiDestinations;
import eu.etaxonomy.cdm.app.pesi.PesiSources;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;

/**
 * Tests the ERMS -> PESI pipeline by comparing the source DB with destination PESI DB.
 *
 * @author a.mueller
 * @since 01.09.2019
 */
public class PesiErmsValidator {

    private static final Logger logger = Logger.getLogger(PesiErmsValidator.class);

    private static final Source defaultSource = PesiSources.PESI2019_ERMS();
    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI_2();

    private Source source = defaultSource;
    private Source destination = defaultDestination;

    public void invoke(Source source, Source destination){
        logger.warn("Validate destination " +  destination.getDatabase());
        boolean success = true;
        try {
            this.source = source;
            this.destination = destination;
//            success &= testReferences();
            success &= testTaxa();
            success &= testTaxonRelations();
//            success &= testCommonNames();
//            success &= testDistributions();
//            success &= testNotes();
//            success &= testAdditionalTaxonSources();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        //TBC
        System.out.println("end validation " + (success? "":"NOT ") + "successful.");
    }

    private boolean testAdditionalTaxonSources() throws SQLException {
        System.out.println("Start validate additional taxon sources");
        boolean success = testAdditionalTaxonSourcesCount();
        if (success){
              success &= testSingleAdditionalTaxonSources(source.getUniqueInteger(countAddtionalTaxonSource));
        }
        return success;
    }

    private boolean testNotes() throws SQLException {
        System.out.println("Start validate notes");
        boolean success = testNotesCount();
        if (success){
              success &= testSingleNotes(source.getUniqueInteger("SELECT count(*) FROM notes "));
        }
        return success;
    }

    private boolean testDistributions() throws SQLException {
        System.out.println("Start validate distributions");
        boolean success = testDistributionCount();
        if (success){
              success &= testSingleDistributions(source.getUniqueInteger("SELECT count(*) FROM dr "));
        }
        return success;
    }

    private boolean testCommonNames() throws SQLException {
        System.out.println("Start validate common names");
        boolean success = testCommonNameCount();
        if (success){
            success &= testSingleCommonNames(source.getUniqueInteger("SELECT count(*) FROM vernaculars "));
        }
        return success;
    }

    private boolean testTaxonRelations() throws SQLException {
        System.out.println("Start validate taxon relations");
        boolean success = testTaxonRelationCount();
        if (success){
            success &= testSingleTaxonRelations(source.getUniqueInteger(countTaxonRelation));
        }
        return success;
    }

    private boolean testTaxa() throws SQLException {
        System.out.println("Start validate taxa");
        boolean success = testTaxaCount();
        if (success){
            success &= testSingleTaxa(source.getUniqueInteger(countTaxon));
        }
        return success;
    }

    private boolean testReferences() throws SQLException {
        System.out.println("Start validate references");
        boolean success = testReferenceCount();
        if (success){
            success &= testSingleReferences();
        }
        return success;
    }

    private final String countAddtionalTaxonSource = "SELECT count(*) FROM tu_sources ts WHERE ts.tu_id <>  147415 ";
    private boolean testAdditionalTaxonSourcesCount() {
        int countSrc = source.getUniqueInteger(countAddtionalTaxonSource);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM AdditionalTaxonSource ");
        return equals("AdditionalTaxonSource count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testNotesCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM notes ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Note ");
        return equals("Notes count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testDistributionCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM dr ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Occurrence ");
        return equals("Occurrence count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testCommonNameCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM vernaculars ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM CommonName ");
        return equals("CommonName count ", countSrc, countDest, String.valueOf(-1));
    }

    private final String countTaxonRelation = "SELECT count(*) FROM tu WHERE  tu_acctaxon <> id AND id NOT IN (147415) ";
    private boolean testTaxonRelationCount() {
         int countSrc = source.getUniqueInteger(countTaxonRelation);
         int countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon ");
         return equals("Taxon count ", countSrc, countDest, String.valueOf(-1));
     }


    private final String countTaxon = "SELECT count(*) FROM tu WHERE id NOT IN (147415)";
    private boolean testTaxaCount() {
         int countSrc = source.getUniqueInteger(countTaxon);
         int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon ");
         return equals("Taxon count ", countSrc, countDest, String.valueOf(-1));
     }

    private boolean testSingleTaxa(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.*, r.rank_name, acc.tu_sp as acc_sp, st.status_name, "
                + "        type.tu_displayname typename, type.tu_authority typeauthor, "
                + "        fo.fossil_name, qs.qualitystatus_name "
                + " FROM tu t "
                + " LEFT JOIN (SELECT DISTINCT rank_id, rank_name FROM ranks WHERE NOT(rank_id = 40 AND rank_name = 'Subphylum' OR rank_id = 122 AND rank_name='Parvorder')) as r ON t.tu_rank = r.rank_id "
                + " LEFT JOIN tu acc ON acc.id = t.tu_acctaxon "
                + " LEFT JOIN status st ON st.status_id = t.tu_status "
                + " LEFT JOIN tu type ON type.id = t.tu_typetaxon "
                + " LEFT JOIN fossil fo ON t.tu_fossil = fo.fossil_id "
                + " LEFT JOIN qualitystatus qs ON t.tu_qualitystatus = qs.id "
                + " WHERE t.id NOT IN (147415) "
                + " ORDER BY CAST(t.id as nvarchar(20)) ");
        ResultSet destRS = destination.getResultSet("SELECT t.*, s.Name as sourceName, type.IdInSource typeSourceId, r.Rank "
                + " FROM Taxon t "
                + "    LEFT JOIN Taxon type ON type.TaxonId = t.TypeNameFk "
                + "    LEFT JOIN Rank r ON r.RankId = t.RankFk AND r.KingdomId = t.KingdomFk "
                + "    LEFT JOIN Source s ON s.SourceId = t.SourceFk "
                + " WHERE t.OriginalDB = 'erms' "
                + " ORDER BY t.IdInSource");
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT t.id, s.sessiondate, a.action_name "
                + " FROM tu t "
                + "   LEFT OUTER JOIN tu_sessions MN ON t.id = MN.tu_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(t.id as nvarchar(20)), s.sessiondate DESC, a.id DESC ");
        int i = 0;
        while (srcRS.next() && destRS.next()){
            success &= testSingleTaxon(srcRS, destRS);
            success &= testTaxonLastAction(srcRsLastAction, destRS, String.valueOf(srcRS.getInt("id")));
            i++;
        }
        success &= equals("Taxon count for single compare", n, i, String.valueOf(-1));
        return success;
    }


    private boolean testSingleTaxon(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Taxon ID", "tu_id: " + srcRS.getInt("id"), destRS.getString("IdInSource"), id);
        success &= equals("Taxon source", "ERMS export for PESI", destRS.getString("sourceName"), id);
//      success &= compareKingdom("Taxon kingdom", srcRS, destRS, id);
        success &= equals("Taxon rank fk", srcRS.getString("tu_rank"), destRS.getString("RankFk"), id);
//      success &= equals("Taxon rank cache", normalizeRank(srcRS.getString("rank_name")), destRS.getString("Rank"), id);
        //TODO GenusOrUninomial, InfraGenericEpithet, SpecificEpithet, InfraSpecificEpithet
//      success &= equals("Taxon websearchname", srcRS.getString("tu_displayname"), destRS.getString("WebSearchName"), id);
//        success &= equals("Taxon WebShowName", srcRS.getString("tu_displayname"), destRS.getString("WebShowName"), id);
        success &= equals("Taxon authority", srcRS.getString("tu_authority"), destRS.getString("AuthorString"), id);
//        success &= equals("Taxon FullName", srcFullName(srcRS), destRS.getString("FullName"), id);
        success &= isNull("NomRefString", destRS);
//        success &= equals("Taxon DisplayName", srcDisplayName(srcRS), destRS.getString("DisplayName"), id);  //according to SQL script same as FullName, no nom.ref. information attached

//TODO        success &= equals("Taxon NameStatusFk", toNameStatus(nullSafeInt(srcRS, "tu_status")),nullSafeInt( destRS,"NameStatusFk"), id);
//TODO        success &= equals("Taxon NameStatusCache", srcRS.getString("status_name"), destRS.getString("NameStatusCache"), id);

//TODO        success &= equals("Taxon TaxonStatusFk", nullSafeInt(srcRS, "tu_status"),nullSafeInt( destRS,"TaxonStatusFk"), id);
//TODO        success &= equals("Taxon TaxonStatusCache", srcRS.getString("status_name"), destRS.getString("TaxonStatusCache"), id);

        //TODO ParentTaxonFk
        Integer orgigTypeNameFk = nullSafeInt(srcRS, "tu_typetaxon");
        success &= equals("Taxon TypeNameFk", orgigTypeNameFk == null? null : "tu_id: " + orgigTypeNameFk, destRS.getString("typeSourceId"), id);
//TODO  success &= equals("Taxon TypeFullNameCache", CdmUtils.concat(" ", srcRS.getString("typename"), srcRS.getString("typeauthor")), destRS.getString("TypeFullNameCache"), id);
        success &= equals("Taxon QualityStatusFK", nullSafeInt(srcRS, "tu_qualitystatus"),nullSafeInt( destRS,"QualityStatusFk"), String.valueOf(id));
        success &= equals("Taxon QualityStatusCache", srcRS.getString("qualitystatus_name"), destRS.getString("QualityStatusCache"), id);
        //TODO TreeIndex
        success &= equals("Taxon FossilStatusFk", nullSafeInt(srcRS, "tu_fossil"),nullSafeInt( destRS,"FossilStatusFk"), String.valueOf(id));
        success &= equals("Taxon FossilStatusCache", srcRS.getString("fossil_name"), destRS.getString("FossilStatusCache"), id);
        success &= equals("Taxon GUID", srcRS.getString("GUID"), destRS.getString("GUID"), id);
        //in 2014 GUID and DerivedFromGuid was always same for ERMS
        success &= equals("Taxon DerivedFromGuid", srcRS.getString("GUID"), destRS.getString("DerivedFromGuid"), id);
        //TODO ExpertGUID
        //TODO ExpertName
        //TODO SpeciesExpertGUID
        //TODO SpeciesExpertName
        //TODO CacheCitation
        //LastAction(Date) handled in separate method
        success &= isNull("GUID2", destRS);  //only relevant after merge
        success &= isNull("DerivedFromGuid2", destRS);  //only relevant after merge
        return success;
    }

    /**
     * @param string
     * @return
     */
    private String normalizeRank(String string) {
        String result = string.replace("Subforma", "Subform")
                .replace("Forma", "Form")
// --               .replace("Subdivision", "Subphylum")
         ;
        return result;
    }

    private String srcFullName(ResultSet srcRs) throws SQLException {
        String result = null;
        String epi = srcRs.getString("tu_name");
        epi = " a" + epi;
        String display = srcRs.getString("tu_displayname");
        String sp = srcRs.getString("tu_sp");
        if (display.indexOf(epi) != display.lastIndexOf(epi) && !sp.startsWith("#2#")){ //homonym, animal
            result = srcRs.getString("tu_displayname").replaceFirst(epi+" ", CdmUtils.concat(" ", " "+epi, srcRs.getString("tu_authority")))+" ";
        }else{
            result = CdmUtils.concat(" ", srcRs.getString("tu_displayname"), srcRs.getString("tu_authority"));
        }
        return result;
    }

    private String srcDisplayName(ResultSet srcRs) throws SQLException {
        String result = null;
        String epi = srcRs.getString("tu_name");
        epi = " a" + epi;
        String display = "<i>"+srcRs.getString("tu_displayname")+"</i>";
        display = display.replace(" var. ", "</i> var. <i>").replace(" f. ", "</i> f. <i>");
        String sp = srcRs.getString("tu_sp");
        if (display.indexOf(epi) != display.lastIndexOf(epi) && !sp.startsWith("#2#")){ //homonym, animal
            result = display.replaceFirst(epi+" ", CdmUtils.concat(" ", " "+epi, srcRs.getString("tu_authority")))+" ";
        }else{
            result = CdmUtils.concat(" ", display, srcRs.getString("tu_authority"));
        }
        return result;
    }

    private boolean testTaxonLastAction(ResultSet srcRs, ResultSet destRs, String id) throws SQLException {
        boolean success = true;
        while (srcRs.next()){
            int srcId = srcRs.getInt("id");
            if (id.equals(String.valueOf(srcId))){
                break;
            }
        }
        String destStr = destRs.getString("LastAction");
        success &= equals("Taxon Last Action", srcRs.getString("action_name"), destStr == null? null : destStr, id);
        success &= equals("Taxon Last Action Date", srcRs.getTimestamp("sessiondate"), destRs.getTimestamp("LastActionDate"), id);
        return success;
    }

    private boolean compareKingdom(String messageStart, ResultSet srcRS, ResultSet destRS, String id) throws SQLException {
        String strSrc = srcRS.getString("acc_sp");
        if (strSrc == null){
            strSrc = srcRS.getString("tu_sp");
        }
        Integer intDest = nullSafeInt(destRS, "KingdomFk");
        if (intDest == null){
            logger.warn(id +": " + messageStart + " must never be null for destination. Biota needs to be 0, all the rest needs to have >0 int value.");
            return false;
        }else if (strSrc == null){
            //TODO
            logger.warn("Computation of source kingdom not yet implemented for top level taxa. ID= " + id);
            return true;
        }else{
            strSrc = strSrc.substring(1);
            String strSrcKingdom = strSrc.substring(0, strSrc.indexOf("#"));
            return equals(messageStart, strSrcKingdom, String.valueOf(intDest), id);
        }
    }

    private boolean testSingleTaxonRelations(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.* "
                + " FROM tu t "
                + " WHERE t.id NOT IN (147415) AND tu_acctaxon <> id "
                + " ORDER BY CAST(t.id as nvarchar(20)) ");
        ResultSet destRS = destination.getResultSet("SELECT rel.*, t1.IdInSource t1Id, t2.IdInSource t2Id "
                + " FROM RelTaxon rel "
                + "    LEFT JOIN Taxon t1 ON t1.TaxonId = rel.TaxonFk1 "
                + "    LEFT JOIN Taxon t2 ON t2.TaxonId = rel.TaxonFk2 "
                + " WHERE t1.OriginalDB = 'erms' AND t2.OriginalDB = 'erms' "
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
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Taxon relation taxon1", "tu_id: " + srcRS.getInt("id"), destRS.getString("t1Id"), id);
        success &= equals("Taxon relation taxon2", "tu_id: " + srcRS.getInt("tu_acctaxon"), destRS.getString("t2Id"), id);
        success &= equals("Taxon relation qualifier fk", PesiTransformer.IS_SYNONYM_OF, destRS.getInt("RelTaxonQualifierFk"), id);
        success &= equals("Taxon relation qualifier cache", "is synonym of", destRS.getString("RelQualifierCache"), id);
        //TODO enable after next import
//        success &= isNull("notes", destRS);
        //complete if no further relations need to added
        return success;
    }


    private boolean testSingleAdditionalTaxonSources(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT CAST(tu.id as nvarchar(20)) tuId, MN.*, s.*, su.sourceuse_name "
                + " FROM tu_sources MN INNER JOIN tu ON MN.tu_id = tu.id "
                + "    LEFT JOIN sources s ON s.id = MN.source_id "
                + "    LEFT JOIN sourceuses su ON MN.sourceuse_id = su.sourceuse_id "
                + " WHERE MN.tu_id NOT IN (147415)  "
                + " ORDER BY CAST(tu.id as nvarchar(20)), MN.sourceuse_id, s.id ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, ats.*, s.*, su.* "
                + " FROM AdditionalTaxonSource ats INNER JOIN Taxon t ON t.TaxonId = ats.TaxonFk "
                + "    INNER JOIN Source s ON s.SourceId = ats.SourceFk "
                + "    LEFT JOIN SourceUse su ON su.SourceUseId = ats.SourceUseFk "
                + " WHERE t.OriginalDB = 'erms' "
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
        //success &= equals("Additional taxon source name cache ", srcRs.getString("source_name"), destRs.getString("SourceNameCache"), id);
        success &= equals("Additional taxon source detail ", srcRs.getString("pagenr"), destRs.getString("SourceDetail"), id);
        //Complete
        return success;
    }

    private boolean testSingleNotes(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT CAST(tu.id as nvarchar(20)) tuId, no.*, l.LanName "
                + " FROM notes no INNER JOIN tu ON no.tu_id = tu.id "
                + "    LEFT JOIN languages l ON l.LanID = no.lan_id "
                + " ORDER BY CAST(tu.id as nvarchar(20)), no.type ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, no.*, cat.NoteCategory, l.Language "
                + " FROM Note no INNER JOIN Taxon t ON t.TaxonId = no.TaxonFk "
                + "    LEFT JOIN NoteCategory cat ON cat.NoteCategoryId = no.NoteCategoryFk "
                + "    LEFT JOIN Language l ON l.LanguageId = no.LanguageFk "
                + " WHERE t.OriginalDB = 'erms' "
                + " ORDER BY t.IdInSource, no.NoteCategoryCache ");
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
        success &= isNull("Note_2", destRs);
        success &= equals("Note category ", srcRs.getString("type"), destRs.getString("NoteCategoryCache"), id);
        success &= equals("Note language ", srcRs.getString("LanName"), destRs.getString("Language"), id);

        //TODO
        //SpeciesExpertGUID, SpeciesExpertName, LastAction, LastActionDate
        return success;
    }

    private boolean testSingleDistributions(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT CAST(ISNULL(tu.tu_accfinal, tu.id) as nvarchar(20)) tuId, gu.gazetteer_id, dr.*, gu.id guId, gu.gu_name "
                + " FROM dr INNER JOIN tu ON dr.tu_id = tu.id "
                + "    LEFT JOIN gu ON gu.id = dr.gu_id "
                + " ORDER BY CAST(ISNULL(tu.tu_accfinal, tu.id) as nvarchar(20)), gu.gazetteer_id, gu.gu_name ");  //, dr.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, a.AreaERMSGazetteerId, oc.*, a.AreaName "
                + " FROM Occurrence oc INNER JOIN Taxon t ON t.TaxonId = oc.TaxonFk "
                + "    LEFT JOIN Area a ON a.AreaId = oc.AreaFk "
                + " WHERE t.OriginalDB = 'erms' "
                + " ORDER BY t.IdInSource, a.AreaERMSGazetteerId, a.AreaName, oc.Notes ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleDistribution(srcRs, destRs);
            count++;
        }
        success &= equals("Distribution count differs", n, count, "-1");
        return success;
    }

    private boolean testSingleDistribution(ResultSet srcRs, ResultSet destRs) throws SQLException {
        String id = String.valueOf(srcRs.getInt("tuId") + "-" + srcRs.getString("gu_name"));
        boolean success = equals("Distribution taxonID ", "tu_id: " + String.valueOf(srcRs.getInt("tuId")), destRs.getString("IdInSource"), id);
        success &= equals("Distribution gazetteer_id ", srcRs.getString("gazetteer_id"), destRs.getString("AreaERMSGazetteerId"), id);
        success &= equals("Distribution area name ", srcRs.getString("gu_name"), destRs.getString("AreaName"), id);
        success &= equals("Distribution area name ", srcRs.getString("note"), destRs.getString("Notes"), id);

        //TODO
        return success;
    }

    private boolean testSingleCommonNames(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT v.*, ISNULL([639_3],[639_2]) iso, l.LanName, tu.id tuId "
                + " FROM vernaculars v LEFT JOIN tu ON v.tu_id = tu.id LEFT JOIN languages l ON l.LanID = v.lan_id "
                + " ORDER BY CAST(tu.id as nvarchar(20)), ISNULL([639_3],[639_2]), v.vername  ");
        ResultSet destRs = destination.getResultSet("SELECT cn.*, t.IdInSource, l.ISO639_2, l.ISO639_3 "
                + " FROM CommonName cn INNER JOIN Taxon t ON t.TaxonId = cn.TaxonFk LEFT JOIN Language l ON l.LanguageId = cn.LanguageFk "
                + " WHERE t.OriginalDB = 'erms' "
                + " ORDER BY t.IdInSource, ISNULL("+preferredISO639+", "+alternativeISO639+"), cn.CommonName");
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
        success &= equals("Common name languageID ", srcRs.getString("iso"), getLanguageIso(destRs), id);
        success &= equals("CommonName name ", srcRs.getString("vername"), destRs.getString("CommonName"), id);
        //TODO success = equals("CommonName language code ", srcRs.getString("lan_id"), destRs.getString("LanguageFk"), id);
        success = equals("CommonName LanguageCache ", normalizeLang(srcRs.getString("LanName")), destRs.getString("LanguageCache"), id);
        success &= isNull("Region", destRs);  //region does not seem to exist in ERMS

        //TODO
        return success;
    }

    private String normalizeLang(String string) {
        if ("Norwegian Nynorsk".equals(string)){
            return "Nynorsk (Norwegian)";
        }else if ("Norwegian Bokmål".equals(string)){
            return "Bokmål  (Norwegian)";
        }else if ("Spanish".equals(string)){
            return "Spanish, Castillian";
        }else if ("Modern Greek (1453-)".equals(string)){
            return "Greek";
        }else if ("Hebrew".equals(string)){
            return "Israel (Hebrew)";
        }else if ("Malay (individual language)".equals(string)){
            return "Malay";
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

    private boolean testSingleReferences() throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT s.* FROM sources s ORDER BY s.id ");
        ResultSet destRS = destination.getResultSet("SELECT s.* FROM Source s "
                + " WHERE s.OriginalDB = 'erms' ORDER BY s.RefIdInSource");  // +1 for the source reference "erms" but this has no OriginalDB
        while (srcRS.next() && destRS.next()){
            success &= testSingleReference(srcRS, destRS);
        }
        return success;
    }

    private boolean testSingleReference(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Reference ID ", srcRS.getInt("id"), destRS.getInt("RefIdInSource"), id);
        success &= equals("Reference IMIS_id ", srcRS.getString("imis_id"), destRS.getString("IMIS_Id"), id);
//TODO        success &= equals("Reference SourceCategoryFk ", srcRS.getString("source_type"), destRS.getInt("SourceCategoryFk"), id);
//TODO       success &= equals("Reference SourceCategoryCache ", srcRS.getString("source_type"), destRS.getString("SourceCategoryCache"), id);
        success &= equals("Reference name ", srcRS.getString("source_name"), destRS.getString("Name"), id);
        success &= equals("Reference abstract ", srcRS.getString("source_abstract"), destRS.getString("Abstract"), id);
        success &= equals("Reference title ", srcRS.getString("source_title"), destRS.getString("Title"), id);
        success &= equals("Reference author string ", srcRS.getString("source_author"), destRS.getString("AuthorString"), id);
        success &= equals("Reference year ", normalizeYear(srcRS.getString("source_year")), destRS.getString("RefYear"), id);
        success &= isNull("NomRefCache", destRS);  //for ERMS no other value was found in 2014 value
        success &= equals("Reference link ", srcRS.getString("source_link"), destRS.getString("Link"), id);
        success &= equals("Reference note ", srcRS.getString("source_note"), destRS.getString("Notes"), id);
        //complete
        return success;
    }


    private boolean testReferenceCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM sources ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s.OriginalDB = 'erms'");  // +1 for the source reference "erms" but this has no OriginalDB
        boolean success = equals("Reference count ", countSrc, countDest, "-1");
        return success;
    }

    private String normalizeYear(String yearStr) {
        if (StringUtils.isBlank(yearStr)){
            return yearStr;
        }
        yearStr = yearStr.trim();
        if (yearStr.matches("\\d{4}-\\d{2}")){
            yearStr = yearStr.substring(0, 5)+yearStr.substring(0, 2)+yearStr.substring(5);
        }
        return yearStr;
    }

    private boolean isNull(String attrName, ResultSet destRS) throws SQLException {
        Object value = destRS.getObject(attrName);
        if (value != null){
            String message = attrName + " was expected to be null but was: " + value.toString();
            logger.warn(message);
            return false;
        }else{
            logger.info(attrName + " was null as expected");
            return true;
        }
    }

    private boolean equals(String messageStart, Timestamp srcDate, Timestamp destDate, String id) {
        if (!CdmUtils.nullSafeEqual(srcDate, destDate)){
            String message = id + ": " + messageStart + " must be equal, but was not.\n Source: "+  srcDate + "; Destination: " + destDate;
            logger.warn(message);
            return false;
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

    private boolean equals(String messageStart, String strSrc, String strDest, String id) {
        if (StringUtils.isBlank(strSrc)){
            strSrc = null;
        }else{
            strSrc = strSrc.trim();
        }
        //we do not trim strDest here because this should be done during import already. If not it should be shown here
        if (!CdmUtils.nullSafeEqual(strSrc, strDest)){
            int index = diffIndex(strSrc, strDest);
            String message = id+ ": " + messageStart + " must be equal, but was not at "+index+".\n  Source:      "+  strSrc + "\n  Destination: " + strDest;
            logger.warn(message);
            return false;
        }else{
            logger.info(id+ ": " + messageStart + " were equal: " + strSrc);
            return true;
        }
    }

    private int diffIndex(String strSrc, String strDest) {
        if (strSrc == null || strDest == null){
            return 0;
        }
        int i;
        for (i = 0; i<strSrc.length() && i<strDest.length() ;i++) {
            if (strSrc.charAt(i)!= strDest.charAt(i)){
                return i;
            }
        }
        if(strSrc.length()!=strDest.length()){
            return Math.max(strSrc.length(), strDest.length());
        }
        return i;
    }

    protected Integer nullSafeInt(ResultSet rs, String columnName) throws SQLException {
        Object intObject = rs.getObject(columnName);
        if (intObject == null){
            return null;
        }else{
            return Integer.valueOf(intObject.toString());
        }
    }

//** ************* MAIN ********************************************/



    public static void main(String[] args){
        PesiErmsValidator validator = new PesiErmsValidator();
        validator.invoke(defaultSource, defaultDestination);
        System.exit(0);
    }
}
