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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.PesiDestinations;
import eu.etaxonomy.cdm.app.common.PesiSources;
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

    private static final Source defaultSource = PesiSources.PESI2019_ERMS_2019();
//    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI();
    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI_2();

    private Source source = defaultSource;
    private Source destination = defaultDestination;
    private String moneraFilter = " NOT IN (-1)"; // 147415;
//    private String moneraFilter = " NOT IN (147415)"; // 147415;

    private String origErms = "OriginalDB = 'ERMS' ";

    public void invoke(Source source, Source destination){
        logger.warn("Validate destination " +  destination.getDatabase());
        boolean success = true;
        try {
            this.source = source;
            this.destination = destination;
//            success &= testReferences();  //ready, few minor issues to be discussed with VLIZ
            success &= testTaxa();
            success &= testTaxonRelations();  //name relations count!,  Implement single compare tests
//            success &= testCommonNames();  //source(s) discuss VLIZ, exact duplicates (except for sources), Anus(Korur)
//            success &= testDistributions();  //>1000 duplicates in "dr", sources (OccurrenceSource table), area spellings(Baelt Sea), 1 long note
//            success &= testNotes();  //ecology & link notes test (only count tested), sources untested (NoteSource table)
//            success &= testAdditionalTaxonSources();  //ready
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

    int countSynonyms;
    int countIncludedIns;
    private boolean testTaxonRelations() {
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
            int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon t WHERE t."+ origErms);
            success &= equals("Taxrel count + 1 must be same as destination taxon count ", countTotalSrc+1, countDest, String.valueOf(-1));
            return success;
        }else{
            return false;
        }
    }

    private boolean testSynonymRelations() {

        int countSrc = source.getUniqueInteger(countSynonymRelation);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk > 101");
        boolean success = equals("Synonym count ", countSrc, countDest, String.valueOf(-1));
//         update Match_RelStat set RelTaxon  =  102 where tu_unacceptreason like 'currently placed%'
//                 update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason like 'currently held%'
//                 update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason like 'sy%' or tu_unacceptreason like '%jun%syn%'
//                 update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason = '(synonym)'
//                 update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason = 'reverted genus transfer'
//                 update Match_RelStat set RelTaxon   =  103 where tu_unacceptreason like 'misapplied%'
//                 update Match_RelStat set RelTaxon   =  104 where tu_unacceptreason like 'part% synonym%'
//                 update Match_RelStat set RelTaxon   =  106 where tu_unacceptreason = 'heterotypic synonym' or tu_unacceptreason = 'subjective synonym'
//                 update Match_RelStat set RelTaxon   =  107 where tu_unacceptreason like '%homot%syn%' or tu_unacceptreason = 'objective synonym' synyonym
//                 update Match_RelStat set RelTaxon   =  107 where tu_unacceptreason like '%bas[iy][no]%ny%'
        if (success){
            //TODO test single synonym relations
//            success &= testSingleTaxonRelations(source.getUniqueInteger(countSynonymRelation));
        }
        countSynonyms = (countSrc == countDest)? countSrc : -1;
        return success;
    }

    private boolean testNameRelations() {
        //Name relations
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM tu WHERE id " + moneraFilter + " AND ("
               + " tu_unacceptreason like '%bas[iy][no]%ny%' OR tu_unacceptreason = 'original combination' "
               + " OR tu_unacceptreason = 'Subsequent combination' OR tu_unacceptreason like '%genus transfer%'  "
               + " OR tu_unacceptreason = 'genus change' "  //1
               + " OR tu_unacceptreason like '%homon%' "   // 2
               + " OR tu_unacceptreason like '%spell%' OR tu_unacceptreason like 'lapsus %' " //16

                 + ")");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk <100 ");
        boolean success = equals("Taxon name relation count ", countSrc, countDest, String.valueOf(-1));
        if (success){
            //TODO test single name relation
//            success &= testSingleTaxonRelations(source.getUniqueInteger(countSynonymRelation));
        }
        return success;
    }

    private boolean testIncludedInRelations() {
        int countSrc = source.getUniqueInteger(countParentRelation);
        int  countDest = destination.getUniqueInteger("SELECT count(*) FROM RelTaxon WHERE RelTaxonQualifierFk = 101 ");
        boolean success = equals("Tax included in count ", countSrc, countDest, String.valueOf(-1));
        if (success){
            //TODO test single includedIn relations
//            success &= testSingleTaxonRelations(source.getUniqueInteger(countSynonymRelation));
        }
        countIncludedIns = (countSrc == countDest)? countSrc : -1;
        return success;
    }

    private boolean testTaxa() throws SQLException {
        System.out.println("Start validate taxa");
        boolean success = testTaxaCount();
        //FIXME
        if (!success){
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

    private final String countAddtionalTaxonSource = "SELECT count(*) FROM tu_sources ts WHERE ts.tu_id " + moneraFilter;
    private boolean testAdditionalTaxonSourcesCount() {
        int countSrc = source.getUniqueInteger(countAddtionalTaxonSource);
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM AdditionalTaxonSource ");
        return equals("AdditionalTaxonSource count ", countSrc, countDest, String.valueOf(-1));
    }

    private boolean testNotesCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM notes ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE NOT (NoteCategoryFk = 4 AND LastAction IS NULL) AND NOT NoteCategoryFk IN (22,23,24) ");
        boolean result = equals("Notes count ", countSrc, countDest, String.valueOf(-1));

        countSrc = source.getUniqueInteger("SELECT count(*) FROM tu "
                + " WHERE (tu_marine IS NOT NULL OR tu_brackish IS NOT NULL OR tu_fresh IS NOT NULL OR tu_terrestrial IS NOT NULL) "
                + "     AND tu.id " + moneraFilter );
        countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE (NoteCategoryFk = 4 AND LastAction IS NULL) ");
        result &= equals("Notes ecology count ", countSrc, countDest, String.valueOf(-1));

        countSrc = source.getUniqueInteger("SELECT count(*) FROM links ");
        countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE NoteCategoryFk IN (22,23,24) ");
        result &= equals("Notes link count ", countSrc, countDest, String.valueOf(-1));

        return result;
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

    private final String countSynonymRelation = "SELECT count(*) FROM tu syn LEFT JOIN tu acc ON syn.tu_acctaxon = acc.id WHERE (syn.id <> acc.id AND syn.tu_acctaxon IS NOT NULL AND syn.id <> acc.tu_parent) AND syn.id " + moneraFilter;
    private final String countParentRelation  = "SELECT count(*)-1 FROM tu syn LEFT JOIN tu acc ON syn.tu_acctaxon = acc.id WHERE (syn.id =  acc.id OR  syn.tu_acctaxon IS     NULL OR  syn.id =  acc.tu_parent) AND syn.id " + moneraFilter;

    private final String countTaxon = "SELECT count(*) FROM tu WHERE id " + moneraFilter;
    private boolean testTaxaCount() {
         int countSrc = source.getUniqueInteger(countTaxon);
         int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon ");
         boolean result = equals("Taxon count ", countSrc, countDest, String.valueOf(-1));

         //NomStatus
         countSrc = source.getUniqueInteger("SELECT count(*) FROM tu WHERE id " + moneraFilter + " AND ("
               + " tu_unacceptreason like '%inval%' OR  tu_unacceptreason like '%not val%' "
               + " OR tu_unacceptreason like '%illeg%' OR tu_unacceptreason like '%nud%' "
               + " OR tu_unacceptreason like '%rej.%' OR tu_unacceptreason like '%superfl%' "
               + " OR tu_unacceptreason like '%Comb. nov%' OR tu_unacceptreason like '%New name%' "
               + " OR tu_unacceptreason = 'new combination'  "
               + " OR tu_status IN (3,5,6,7,8) )");
         countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon WHERE NameStatusFk IS NOT NULL ");
         result = equals("Taxon name status count ", countSrc, countDest, String.valueOf(-1));

         return result;
     }

    private boolean testSingleTaxa(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.*, tu1.tu_name tu1_name, r.rank_name, acc.tu_sp as acc_sp, st.status_name, "
                + "        type.tu_displayname typename, type.tu_authority typeauthor, "
                + "        fo.fossil_name, qs.qualitystatus_name "
                + " FROM tu t "
                + " LEFT JOIN tu as tu1 on t.tu_parent = tu1.id "
                + " LEFT JOIN (SELECT DISTINCT rank_id, rank_name FROM ranks WHERE NOT(rank_id = 30 AND rank_name = 'Phylum (Division)' OR rank_id = 40 AND rank_name = 'Subphylum (Subdivision)' OR rank_id = 122 AND rank_name='Subsection')) as r ON t.tu_rank = r.rank_id "
                + " LEFT JOIN tu acc ON acc.id = t.tu_acctaxon "
                + " LEFT JOIN status st ON st.status_id = t.tu_status "
                + " LEFT JOIN tu type ON type.id = t.tu_typetaxon "
                + " LEFT JOIN fossil fo ON t.tu_fossil = fo.fossil_id "
                + " LEFT JOIN qualitystatus qs ON t.tu_qualitystatus = qs.id "
                + " WHERE t.id " + moneraFilter
                + " ORDER BY CAST(t.id as nvarchar(20)) ");
        ResultSet destRS = destination.getResultSet("SELECT t.*, "
                + "     pt.GenusOrUninomial p_GenusOrUninomial, pt.InfraGenericEpithet p_InfraGenericEpithet, pt.SpecificEpithet p_SpecificEpithet, "
                + "     s.Name as sourceName, type.IdInSource typeSourceId, r.Rank "
                + " FROM Taxon t "
                + "    LEFT JOIN Taxon pt ON pt.TaxonId = t.ParentTaxonFk "
                + "    LEFT JOIN Taxon type ON type.TaxonId = t.TypeNameFk "
                + "    LEFT JOIN Rank r ON r.RankId = t.RankFk AND r.KingdomId = t.KingdomFk "
                + "    LEFT JOIN Source s ON s.SourceId = t.SourceFk "
                + " WHERE t."+ origErms
                + " ORDER BY t.IdInSource");
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT t.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM tu t "
                + "   LEFT OUTER JOIN tu_sessions MN ON t.id = MN.tu_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(t.id as nvarchar(20)), s.sessiondate DESC, a.id DESC ");
        int i = 0;
        while (srcRS.next() && destRS.next()){
            success &= testSingleTaxon(srcRS, destRS);
            success &= testLastAction(srcRsLastAction, destRS, String.valueOf(srcRS.getInt("id")), "Taxon");
            i++;
        }
        success &= equals("Taxon count for single compare", n, i, String.valueOf(-1));
        return success;
    }


    private boolean testSingleTaxon(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Taxon ID", "tu_id: " + srcRS.getInt("id"), destRS.getString("IdInSource"), id);
        success &= equals("Taxon source", "ERMS export for PESI", destRS.getString("sourceName"), id);
//TODO some
        success &= compareKingdom("Taxon kingdom", srcRS, destRS, id);
        success &= equals("Taxon rank fk", srcRS.getString("tu_rank"), destRS.getString("RankFk"), id);
        success &= equals("Taxon rank cache", normalizeRank(srcRS.getString("rank_name"), srcRS, id), destRS.getString("Rank"), id);
        success &= compareNameParts(srcRS, destRS, id);

        success &= equals("Taxon websearchname", srcRS.getString("tu_displayname"), destRS.getString("WebSearchName"), id);
//TODO        success &= equals("Taxon WebShowName", srcRS.getString("tu_displayname"), destRS.getString("WebShowName"), id);
        success &= equals("Taxon authority", srcRS.getString("tu_authority"), destRS.getString("AuthorString"), id);
        success &= equals("Taxon FullName", srcFullName(srcRS), destRS.getString("FullName"), id);
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
        success &= equals("Taxon DerivedFromGuid", srcRS.getString("GUID"), destRS.getString("DerivedFromGuid"), id); //according to SQL script GUID and DerivedFromGuid are always the same, according to 2014DB this is even true for all databases
        success &= isNull("ExpertGUID", destRS);  //only relevant after merge
        success &= isNull("ExpertName", destRS);  //only relevant after merge
        success &= isNull("SpeciesExpertGUID", destRS);  //only relevant after merge
        success &= equals("Taxon cache citation", srcRS.getString("cache_citation"), destRS.getString("CacheCitation"), id);
        //LastAction(Date) handled in separate method
        success &= isNull("GUID2", destRS);  //only relevant after merge
        success &= isNull("DerivedFromGuid2", destRS);  //only relevant after merge
        return success;
    }

    boolean namePartsFirst = true;
    private boolean compareNameParts(ResultSet srcRS, ResultSet destRS, String id) throws SQLException {
        if (namePartsFirst){
            logger.warn("Validation of name parts not fully implemented (difficult). Currently validated via fullname");
            namePartsFirst = false;
        }
        int rankFk = srcRS.getInt("tu_rank");
        String genusOrUninomial = null;
        String infraGenericEpithet = null;
        String specificEpithet = null;
        String infraSpecificEpithet = null;
        if (rankFk <= 180){
            genusOrUninomial = srcRS.getString("tu_name");
        }else if (rankFk == 190){
            genusOrUninomial = srcRS.getString("tu1_name");
            infraGenericEpithet =  srcRS.getString("tu_name");
            //TODO does not work this way
//        }else if (rankFk == 220){
//            genusOrUninomial = destRS.getString("p_GenusOrUninomial");
//            infraGenericEpithet = destRS.getString("p_InfraGenericEpithet");
//            specificEpithet = srcRS.getString("tu_name");
        }else{
            //TODO exception
            return false;
        }
        boolean result = testEpis(destRS, genusOrUninomial, infraGenericEpithet,
                specificEpithet, infraSpecificEpithet, id);
        return result;
    }

    private boolean testEpis(ResultSet destRS, String genusOrUninomial, String infraGenericEpithet, String specificEpithet,
            String infraSpecificEpithet, String id) throws SQLException {
        boolean result = equals("Taxon genusOrUninomial", genusOrUninomial, destRS.getString("GenusOrUninomial"), id) ;
        result &= equals("Taxon infraGenericEpithet", infraGenericEpithet, destRS.getString("InfraGenericEpithet"), id) ;
        result &= equals("Taxon specificEpithet", specificEpithet, destRS.getString("SpecificEpithet"), id) ;
        result &= equals("Taxon infraSpecificEpithet", infraSpecificEpithet, destRS.getString("InfraSpecificEpithet"), id) ;
        return result;
    }

    private String normalizeRank(String string, ResultSet srcRS, String id) throws SQLException {
        String result = string
                .replace("Subforma", "Subform")
                .replace("Forma", "Form");
        int kingdomFk = Integer.valueOf(getSourceKingdomFk(srcRS, id));
        if (kingdomFk == 3 || kingdomFk == 4){
            result = result.replace("Subphylum", "Subdivision");
            result = result.replace("Phylum", "Division");
        }
        return result;
    }

    //see also ErmsTaxonImport.getExpectedTitleCache()
    private String srcFullName(ResultSet srcRs) throws SQLException {
        String result = null;
        String epi = srcRs.getString("tu_name");
        String display = srcRs.getString("tu_displayname");
        String sp = srcRs.getString("tu_sp");
        if (display.indexOf(epi) != display.lastIndexOf(epi) && !sp.startsWith("#2#")){ //autonym, !animal
            String authority = srcRs.getString("tu_authority");
            result = srcRs.getString("tu_displayname").replaceFirst(epi+" ", CdmUtils.concat(" ", epi, authority)+" ");
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

    String lastLastActionId = "-1";
    private boolean testLastAction(ResultSet srcRsLastAction, ResultSet destRs, String id, String table) throws SQLException {
        try {
            boolean success = true;
            String srcId = null;
            while (srcRsLastAction.next()){
                srcId = String.valueOf(srcRsLastAction.getInt("id"));
                if (!lastLastActionId.equals(srcId)){
                    lastLastActionId = srcId;
                    break;
                }
            }
            if(!id.equals(srcId)){
                logger.warn("Last Action SourceIDs are not equal: id: " +id + ", la-id: " + srcId);
            }
            String destStr = destRs.getString("LastAction");
            success &= equals(table + " SpeciesExpertName", srcRsLastAction.getString("ExpertName"), destRs.getString("SpeciesExpertName"), id);  //mapping ExpertName => SpeciesExpertName according to SQL script
            success &= equals(table + " Last Action", srcRsLastAction.getString("action_name"), destStr == null? null : destStr, id);
            success &= equals(table + " Last Action Date", srcRsLastAction.getTimestamp("sessiondate"), destRs.getTimestamp("LastActionDate"), id);

            return success;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private boolean compareKingdom(String messageStart, ResultSet srcRS, ResultSet destRS, String id) throws SQLException {
        String srcKingdom = getSourceKingdomFk(srcRS, id);
        Integer intDest = nullSafeInt(destRS, "KingdomFk");
        if (intDest == null){
            logger.warn(id +": " + messageStart + " must never be null for destination. Biota needs to be 0, all the rest needs to have >0 int value.");
            return false;
        }else{
            return equals(messageStart, srcKingdom, String.valueOf(intDest), id);
        }
    }

    private String getSourceKingdomFk(ResultSet srcRS, String id) throws SQLException {
        String strSrc = srcRS.getString("acc_sp");
        if (strSrc == null){
            strSrc = srcRS.getString("tu_sp");
        }
        if (strSrc == null){
            if ("1".equals(id)){
                strSrc = "0";  //Biota
            }else if ("147415".equals(id)){
                strSrc = "6";  //Monera is synonym of Bacteria
            }else{
                strSrc = id;
            }
        }else{
            strSrc = strSrc.substring(1);
            strSrc = strSrc.substring(0, strSrc.indexOf("#"));
        }
        return strSrc;
    }

    private boolean testSingleTaxonRelations(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.* "
                + " FROM tu t "
                + " WHERE t.id "+ moneraFilter + " AND tu_acctaxon <> id "
                + " ORDER BY CAST(t.id as nvarchar(20)) ");
        ResultSet destRS = destination.getResultSet("SELECT rel.*, t1.IdInSource t1Id, t2.IdInSource t2Id "
                + " FROM RelTaxon rel "
                + "    LEFT JOIN Taxon t1 ON t1.TaxonId = rel.TaxonFk1 "
                + "    LEFT JOIN Taxon t2 ON t2.TaxonId = rel.TaxonFk2 "
                + " WHERE t1."+origErms+" AND t2." + origErms
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
                + " WHERE MN.tu_id  " + moneraFilter
                + " ORDER BY CAST(tu.id as nvarchar(20)), MN.sourceuse_id, s.id ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, ats.*, s.*, su.* "
                + " FROM AdditionalTaxonSource ats INNER JOIN Taxon t ON t.TaxonId = ats.TaxonFk "
                + "    INNER JOIN Source s ON s.SourceId = ats.SourceFk "
                + "    LEFT JOIN SourceUse su ON su.SourceUseId = ats.SourceUseFk "
                + " WHERE t."+origErms
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
        ResultSet srcRs = source.getResultSet("SELECT CAST(tu.id as nvarchar(20)) tuId, no.*, l.LanName "
                + " FROM notes no INNER JOIN tu ON no.tu_id = tu.id "
                + "    LEFT JOIN languages l ON l.LanID = no.lan_id "
                + " ORDER BY CAST(tu.id as nvarchar(20)), no.type, no.noteSortable ");  //, no.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, no.*, cat.NoteCategory, l.Language "
                + " FROM Note no INNER JOIN Taxon t ON t.TaxonId = no.TaxonFk "
                + "    LEFT JOIN NoteCategory cat ON cat.NoteCategoryId = no.NoteCategoryFk "
                + "    LEFT JOIN Language l ON l.LanguageId = no.LanguageFk "
                + " WHERE t." + origErms
                + "      AND NOT (NoteCategoryFk = 4 AND no.LastAction IS NULL) AND NOT NoteCategoryFk IN (22,23,24) "
                + " ORDER BY t.IdInSource, no.NoteCategoryCache, Note_1  ");
        int count = 0;
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT no.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM notes no "
                + "   INNER JOIN tu ON tu.id = no.tu_id "
                + "   LEFT JOIN languages l ON l.LanID = no.lan_id"
                + "   LEFT JOIN notes_sessions MN ON no.id = MN.note_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(tu.id as nvarchar(20)), no.type, no.noteSortable, s.sessiondate DESC, a.id DESC ");

        while (srcRs.next() && destRs.next()){
            success &= testSingleNote(srcRs, destRs);
            success &= testLastAction(srcRsLastAction, destRs, String.valueOf(srcRs.getInt("id")), "Note");
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
        success &= equals("Note category cache", normalizeNoteCatCache(srcRs.getString("type")), destRs.getString("NoteCategoryCache"), id);
        success &= equals("Note language ", srcRs.getString("LanName"), destRs.getString("Language"), id);
        success &= isNull("Region", destRs);
        success &= isNull("SpeciesExpertGUID", destRs);
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
        ResultSet srcRs = source.getResultSet("SELECT CAST(ISNULL(tu.tu_accfinal, tu.id) as nvarchar(20)) tuId,"
                + " gu.gazetteer_id, dr.*, gu.id guId, gu.gu_name "
                + " FROM dr INNER JOIN tu ON dr.tu_id = tu.id "
                + "    LEFT JOIN gu ON gu.id = dr.gu_id "
                + " ORDER BY CAST(ISNULL(tu.tu_accfinal, tu.id) as nvarchar(20)), gu.gazetteer_id, gu.gu_name, dr.noteSortable ");  //, dr.note (not possible because ntext
        ResultSet destRs = destination.getResultSet("SELECT t.IdInSource, a.AreaERMSGazetteerId, oc.*, a.AreaName "
                + " FROM Occurrence oc INNER JOIN Taxon t ON t.TaxonId = oc.TaxonFk "
                + "    LEFT JOIN Area a ON a.AreaId = oc.AreaFk "
                + " WHERE t." + origErms
                + " ORDER BY t.IdInSource, a.AreaERMSGazetteerId, a.AreaName, oc.Notes ");
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT dr.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM dr "
                + "   INNER JOIN tu ON tu.id = dr.tu_id "
                + "   LEFT JOIN gu ON gu.id = dr.gu_id "
                + "   LEFT JOIN dr_sessions MN ON dr.id = MN.dr_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(tu.id as nvarchar(20)), gu.gazetteer_id, gu.gu_name, s.sessiondate DESC, a.id DESC ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleDistribution(srcRs, destRs);
            //there are >1000 duplicates in dr, therefore this creates lots of warnings
            success &= testLastAction(srcRsLastAction, destRs, String.valueOf(srcRs.getInt("id")), "Distribution");
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
        success &= equals("Distribution area name cache", srcRs.getString("gu_name"), destRs.getString("AreaNameCache"), id);
        success &= equals("Distribution OccurrenceStatusFk", 1, destRs.getInt("OccurrenceStatusFk"), id);
        success &= equals("Distribution OccurrenceStatusCache", "Present", destRs.getString("OccurrenceStatusCache"), id);
        //TODO see comments
        success &= isNull("SourceFk", destRs);  //sources should be moved to extra table only, check with script and PESI 2014 (=> has values for ERMS)
        success &= isNull("SourceCache", destRs);  //sources should be moved to extra table, check with script and PESI 2014 (=> has values for ERMS)
        success &= equals("Distribution notes ", srcRs.getString("note"), destRs.getString("Notes"), id);
        success &= isNull("SpeciesExpertGUID", destRs);  //SpeciesExpertGUID does not exist in ERMS
        //SpeciesExpertName,LastAction,LastActionDate handled in separate method
        return success;
    }

    private boolean testSingleCommonNames(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRs = source.getResultSet("SELECT v.*, ISNULL([639_3],[639_2]) iso, l.LanName, tu.id tuId "
                + " FROM vernaculars v LEFT JOIN tu ON v.tu_id = tu.id LEFT JOIN languages l ON l.LanID = v.lan_id "
                + " ORDER BY CAST(tu.id as nvarchar(20)), ISNULL([639_3],[639_2]), v.vername, v.id ");
        ResultSet destRs = destination.getResultSet("SELECT cn.*, t.IdInSource, l.ISO639_2, l.ISO639_3 "
                + " FROM CommonName cn INNER JOIN Taxon t ON t.TaxonId = cn.TaxonFk LEFT JOIN Language l ON l.LanguageId = cn.LanguageFk "
                + " WHERE t." + origErms
                + " ORDER BY t.IdInSource, ISNULL("+preferredISO639+", "+alternativeISO639+"), cn.CommonName, cn.LastActionDate ");  //sorting also lastActionDate results in a minimum of exact duplicate problems
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT v.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM vernaculars v "
                + "   INNER JOIN tu ON tu.id = v.tu_id "
                + "   LEFT JOIN languages l ON l.LanID = v.lan_id"
                + "   LEFT JOIN vernaculars_sessions MN ON v.id = MN.vernacular_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(tu.id as nvarchar(20)), ISNULL([639_3],[639_2]), v.vername, v.id, s.sessiondate DESC, a.id DESC ");
        int count = 0;
        while (srcRs.next() && destRs.next()){
            success &= testSingleCommonName(srcRs, destRs);
            success &= testLastAction(srcRsLastAction, destRs, String.valueOf(srcRs.getInt("id")), "CommonName");
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
        //TODO needed? success = equals("CommonName language code ", srcRs.getString("lan_id"), destRs.getString("LanguageFk"), id);
        success &= isNull("Region", destRs);  //region does not seem to exist in ERMS
        //TODO see comments
//        success &= isNull("SourceFk", destRs);  //sources should be moved to extra table, check with PESI 2014
//        success &= isNull("SourceNameCache", destRs);  //sources should be moved to extra table, check with PESI 2014
        success &= isNull("SpeciesExpertGUID", destRs);  //SpeciesExpertGUID does not exist in ERMS
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

    private boolean testSingleReferences() throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT s.* FROM sources s ORDER BY s.id ");
        ResultSet destRS = destination.getResultSet("SELECT s.* FROM Source s "
                + " WHERE s." + origErms
                + " ORDER BY s.RefIdInSource ");  // +1 for the source reference "erms" but this has no OriginalDB
        while (srcRS.next() && destRS.next()){
            success &= testSingleReference(srcRS, destRS);
        }
        return success;
    }

    private boolean testSingleReference(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Reference ID ", srcRS.getInt("id"), destRS.getInt("RefIdInSource"), id);
        success &= equals("Reference IMIS_id ", srcRS.getString("imis_id"), destRS.getString("IMIS_Id"), id);
        success &= equals("Reference SourceCategoryFk ", convertSourceTypeFk(srcRS.getString("source_type")), destRS.getInt("SourceCategoryFk"), id);
        success &= equals("Reference SourceCategoryCache ", convertSourceTypeCache(srcRS.getString("source_type")), destRS.getString("SourceCategoryCache"), id);
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

    private Integer convertSourceTypeFk(String sourceType) {
        if (sourceType == null){
            return null;
        }else if ("d".equals(sourceType)){
            return 4;
        }else if ("e".equals(sourceType)){
            return 5;
        }else if ("p".equals(sourceType)){
            return 11;
        }else if ("i".equals(sourceType)){
            return 12;
        }
        return null;
    }
    private String convertSourceTypeCache(String sourceType) {
        if (sourceType == null){
            return null;
        }else if ("d".equals(sourceType)){
            return "database";
        }else if ("e".equals(sourceType)){
            return "informal reference";
        }else if ("p".equals(sourceType)){
            return "publication";
        }else if ("i".equals(sourceType)){
            //TODO
            return "i";
        }
        return null;
    }

    private boolean testReferenceCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM sources ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s."+ origErms);  // +1 for the source reference "erms" but this has no OriginalDB
        boolean success = equals("Reference count ", countSrc, countDest, "-1");
        return success;
    }

    //NOTE: there could be better parsing of source_year during import, this may also need better normalizing in the database
    private String normalizeYear(String yearStr) {
        if (StringUtils.isBlank(yearStr)){
            return yearStr;
        }
        yearStr = yearStr.trim();
        if (yearStr.matches("\\d{4}-\\d{2}")){
            yearStr = yearStr.substring(0, 5)+yearStr.substring(0, 2)+yearStr.substring(5);
        }
        if (yearStr.equals("20 Mar 1891")){
            yearStr = "20.3.1891";
        }
        if (yearStr.equals("July 1900")){
            yearStr = "7.1900";
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
            int index = CdmUtils.diffIndex(strSrc, strDest);
            String message = id+ ": " + messageStart + " must be equal, but was not at "+index+".\n  Source:      "+  strSrc + "\n  Destination: " + strDest;
            logger.warn(message);
            return false;
        }else{
            logger.info(id+ ": " + messageStart + " were equal: " + strSrc);
            return true;
        }
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
