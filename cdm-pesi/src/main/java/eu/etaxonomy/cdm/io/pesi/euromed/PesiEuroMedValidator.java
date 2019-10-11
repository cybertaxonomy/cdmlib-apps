/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.euromed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.pesi.PesiDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;

/**
 * Tests the ERMS -> PESI pipeline by comparing the source DB with destination PESI DB.
 *
 * @author a.mueller
 * @since 01.09.2019
 */
public class PesiEuroMedValidator {

    private static final Logger logger = Logger.getLogger(PesiEuroMedValidator.class);

    private static final ICdmDataSource defaultSource = CdmDestinations.cdm_test_local_mysql_euromed();
//    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI();
    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI_2();

    private Source source = new Source(defaultSource);
    private Source destination = defaultDestination;

    private String origEuroMed = "OriginalDB = 'E+M' ";

    public void invoke(Source source, Source destination){
        logger.warn("Validate destination " +  destination.getDatabase());
        boolean success = true;
        try {
            this.source = source;
            this.destination = destination;
//            success &= testReferences();
              success &= testTaxa();
//            success &= testTaxonRelations();
//              success &= testDistributions();
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
        if (!success){
              success &= testSingleDistributions(source.getUniqueInteger(distributionCountSQL));
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
            int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon t WHERE t."+ origEuroMed);
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
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM tu WHERE ("
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

    private final String countAddtionalTaxonSource = "SELECT count(*) FROM tu_sources ts ";
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
                + " WHERE (tu_marine IS NOT NULL OR tu_brackish IS NOT NULL OR tu_fresh IS NOT NULL OR tu_terrestrial IS NOT NULL) " );
        countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE (NoteCategoryFk = 4 AND LastAction IS NULL) ");
        result &= equals("Notes ecology count ", countSrc, countDest, String.valueOf(-1));

        countSrc = source.getUniqueInteger("SELECT count(*) FROM links ");
        countDest = destination.getUniqueInteger("SELECT count(*) FROM Note "
                + " WHERE NoteCategoryFk IN (22,23,24) ");
        result &= equals("Notes link count ", countSrc, countDest, String.valueOf(-1));

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

    private final String countSynonymRelation = "SELECT count(*) FROM tu syn LEFT JOIN tu acc ON syn.tu_acctaxon = acc.id WHERE (syn.id <> acc.id AND syn.tu_acctaxon IS NOT NULL AND syn.id <> acc.tu_parent) ";
    private final String countParentRelation  = "SELECT count(*)-1 FROM tu syn LEFT JOIN tu acc ON syn.tu_acctaxon = acc.id WHERE (syn.id =  acc.id OR  syn.tu_acctaxon IS     NULL OR  syn.id =  acc.tu_parent) ";

    private final String countTaxon = "SELECT count(*) FROM TaxonBase tb WHERE tb.publish = 1 ";
    private boolean testTaxaCount() {
         int countSrc = source.getUniqueInteger(countTaxon);
         int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon t WHERE t.SourceFk IS NOT NULL OR t.AuthorString = 'auct.' ");
         boolean result = equals("Taxon count ", countSrc, countDest, String.valueOf(-1));

//         //NomStatus
//         countSrc = source.getUniqueInteger("SELECT count(*) FROM tu WHERE ("
//               + " tu_unacceptreason like '%inval%' OR  tu_unacceptreason like '%not val%' "
//               + " OR tu_unacceptreason like '%illeg%' OR tu_unacceptreason like '%nud%' "
//               + " OR tu_unacceptreason like '%rej.%' OR tu_unacceptreason like '%superfl%' "
//               + " OR tu_unacceptreason like '%Comb. nov%' OR tu_unacceptreason like '%New name%' "
//               + " OR tu_unacceptreason = 'new combination'  "
//               + " OR tu_status IN (3,5,6,7,8) )");
//         countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon WHERE NameStatusFk IS NOT NULL ");
//         result = equals("Taxon name status count ", countSrc, countDest, String.valueOf(-1));

         return result;
     }

    private boolean testSingleTaxa(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT CAST(tn.id as char(20)) tid, tb.uuid as GUID, tn.rank_id, rank.titleCache rank_name, "
                + "      sec.titleCache secTitle,"
                + "      tn.genusOrUninomial, tn.infraGenericEpithet, tn.specificEpithet, tn.infraSpecificEpithet, "
                + "      tn.nameCache, tn.authorshipCache, tn.titleCache nameTitleCache, "
                + "      tb.DTYPE taxStatus, nsType.id nsId, nsType.idInVocabulary nsTitle, "
                + "      CASE WHEN tb.updated IS NOT NULL THEN tb.updated ELSE tb.created END as lastActionDate, "
                + "      CASE WHEN tb.updated IS NOT NULL THEN 'changed' ELSE 'created' END as lastAction "
                + " FROM TaxonBase tb "
                + "     LEFT JOIN TaxonName tn on tb.name_id = tn.id "
                + "     LEFT JOIN DefinedTermBase rank ON rank.id = tn.rank_id "
                + "     LEFT JOIN Reference sec ON sec.id = tb.sec_id "
                + "     LEFT JOIN TaxonName_NomenclaturalStatus nsMN ON tn.id = nsMN.TaxonName_id "
                + "     LEFT JOIN NomenclaturalStatus ns ON ns.id = nsMN.status_id "
                + "     LEFT JOIN DefinedTermBase nsType ON nsType.id = ns.type_id "
                + " WHERE tb.publish = 1 "
                + " GROUP BY tid, GUID, tn.rank_id, rank.titleCache, secTitle,"
                + "      tn.genusOrUninomial, tn.infraGenericEpithet, tn.specificEpithet, tn.infraSpecificEpithet, "
                + "      tn.nameCache, tn.authorshipCache, tn.titleCache, "
                + "      tb.DTYPE, tb.updated, tb.created "    //for duplicates caused by >1 name status
                + " ORDER BY tid, GUID, lastActionDate ");
        ResultSet destRS = destination.getResultSet("SELECT t.*, "
                + "     pt.GenusOrUninomial p_GenusOrUninomial, pt.InfraGenericEpithet p_InfraGenericEpithet, pt.SpecificEpithet p_SpecificEpithet, "
                + "     s.Name as sourceName, type.IdInSource typeSourceId, r.Rank "
                + " FROM Taxon t "
                + "    LEFT JOIN Taxon pt ON pt.TaxonId = t.ParentTaxonFk "
                + "    LEFT JOIN Taxon type ON type.TaxonId = t.TypeNameFk "
                + "    LEFT JOIN Rank r ON r.RankId = t.RankFk AND r.KingdomId = t.KingdomFk "
                + "    LEFT JOIN Source s ON s.SourceId = t.SourceFk "
                + " WHERE t."+ origEuroMed + " AND (t.SourceFk IS NOT NULL  OR t.AuthorString = 'auct.') "   //FIXME remove SourceFk filter is only preliminary for first check
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
        success &= equals("Taxon source", srcRS.getString("secTitle"), destRS.getString("sourceName"), id);

        success &= equals("Taxon kingdomFk", "3", destRS.getString("KingdomFk"), id);
//difficult to test        success &= equals("Taxon rank fk", srcRS.getString("rank_id"), destRS.getString("RankFk"), id);
        success &= equals("Taxon rank cache", normalizeRank(srcRS.getString("rank_name"), srcRS, id), destRS.getString("Rank"), id);
        success &= equals("Taxon genusOrUninomial", srcRS.getString("genusOrUninomial"), destRS.getString("GenusOrUninomial"), id) ;
        success &= equals("Taxon infraGenericEpithet", srcRS.getString("infraGenericEpithet"), destRS.getString("InfraGenericEpithet"), id) ;
        success &= equals("Taxon specificEpithet", srcRS.getString("specificEpithet"), destRS.getString("SpecificEpithet"), id) ;
        success &= equals("Taxon infraSpecificEpithet", srcRS.getString("infraSpecificEpithet"), destRS.getString("InfraSpecificEpithet"), id) ;

        success &= equals("Taxon websearchname", srcRS.getString("nameCache"), destRS.getString("WebSearchName"), id);
//TODO        success &= equals("Taxon WebShowName", srcRS.getString("tu_displayname"), destRS.getString("WebShowName"), id);
//FIXME sensu+auct. autoren       success &= equals("Taxon authority", srcRS.getString("authorshipCache"), destRS.getString("AuthorString"), id);
//FIXME sensu+auct. autoren        success &= equals("Taxon FullName", srcRS.getString("nameTitleCache"), destRS.getString("FullName"), id);
//TODO        success &= isNull("NomRefString", destRS);
//TODO        success &= equals("Taxon DisplayName", srcDisplayName(srcRS), destRS.getString("DisplayName"), id);  //in ERMS according to SQL script same as FullName, no nom.ref. information attached

//TODO        success &= equals("Taxon NameStatusFk", nullSafeInt(srcRS, "nsId"),nullSafeInt( destRS,"NameStatusFk"), id);
        success &= equals("Taxon NameStatusCache", srcRS.getString("nsTitle"), destRS.getString("NameStatusCache"), id);

//        success &= equals("Taxon TaxonStatusFk", mapTaxStatusFk(srcRS.getString("taxStatus")), nullSafeInt( destRS,"TaxonStatusFk"), id);
//        success &= equals("Taxon TaxonStatusCache", mapTaxStatus(srcRS.getString("taxStatus")), destRS.getString("TaxonStatusCache"), id);

//        //TODO ParentTaxonFk
//        Integer orgigTypeNameFk = nullSafeInt(srcRS, "tu_typetaxon");
//        success &= equals("Taxon TypeNameFk", orgigTypeNameFk == null? null : "tu_id: " + orgigTypeNameFk, destRS.getString("typeSourceId"), id);
////TODO  success &= equals("Taxon TypeFullNameCache", CdmUtils.concat(" ", srcRS.getString("typename"), srcRS.getString("typeauthor")), destRS.getString("TypeFullNameCache"), id);
          //quality status, according to SQL always constant, could be changed in future
        success &= equals("Taxon QualityStatusFK", 2, nullSafeInt( destRS,"QualityStatusFk"), String.valueOf(id));
        success &= equals("Taxon QualityStatusCache", "Added by Database Management Team", destRS.getString("QualityStatusCache"), id);
//        //TODO TreeIndex
          success &= isNull("FossilStatusFk", destRS);
          success &= isNull("FossilStatusCache", destRS);
        success &= equals("Taxon GUID", srcRS.getString("GUID"), destRS.getString("GUID"), id);
        success &= equals("Taxon DerivedFromGuid", srcRS.getString("GUID"), destRS.getString("DerivedFromGuid"), id); //according to SQL script GUID and DerivedFromGuid are always the same, according to 2014DB this is even true for all databases
        success &= isNull("ExpertGUID", destRS);  //according to SQL + PESI2014
//        success &= isNull("ExpertName", destRS);
//        success &= isNull("SpeciesExpertGUID", destRS);
//      success &= isNull("SpeciesExpertName", destRS);  //only relevant after merge
//FIXME !!        success &= equals("Taxon cache citation", srcRS.getString("secTitle"), destRS.getString("CacheCitation"), id);
        success &= equals("Taxon Last Action", srcRS.getString("lastAction"),  destRS.getString("LastAction"), id);
        success &= equals("Taxon Last Action Date", srcRS.getTimestamp("lastActionDate"),  destRS.getTimestamp("LastActionDate"), id);

        success &= isNull("GUID2", destRS);  //only relevant after merge
        success &= isNull("DerivedFromGuid2", destRS);  //only relevant after merge
        return success;
    }

    private String mapTaxStatus(String string) {
        if (string == null){
            return null;
        }else if ("Synonym".equals(string)){
            return "synonym";
        }else if ("Taxon".equals(string)){
            return "accepted";
        }
        return null;
    }

    private Integer mapTaxStatusFk(String string) {
        if (string == null){
            return null;
        }else if ("Synonym".equals(string)){
            return PesiTransformer.T_STATUS_SYNONYM;
        }else if ("Taxon".equals(string)){
            return PesiTransformer.T_STATUS_ACCEPTED;
        }
        return null;
    }

    private String normalizeRank(String rankStr, ResultSet srcRS, String id) throws SQLException {
        if (rankStr == null){return null;
        }else if (rankStr.equals("Convar")){return "Convariety";
        }else if (rankStr.equals("Unranked (infrageneric)")){return "Tax. infragen.";
        }else if (rankStr.equals("Unranked (infraspecific)")){return "Tax. infraspec.";
        }else if (rankStr.equals("Coll. species")){return "Coll. Species";
        }else if (rankStr.equals("Species Aggregate")){return "Aggregate";
        }else if (rankStr.equals("Subsection bot.")){return "Subsection";
        }return rankStr;
    }

    //see also ErmsTaxonImport.getExpectedTitleCache()
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

    private boolean testSingleTaxonRelations(int n) throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet(""
                + " SELECT t.* "
                + " FROM tu t "
                + " WHERE tu_acctaxon <> id "
                + " ORDER BY CAST(t.id as char(20)) ");
        ResultSet destRS = destination.getResultSet("SELECT rel.*, t1.IdInSource t1Id, t2.IdInSource t2Id "
                + " FROM RelTaxon rel "
                + "    LEFT JOIN Taxon t1 ON t1.TaxonId = rel.TaxonFk1 "
                + "    LEFT JOIN Taxon t2 ON t2.TaxonId = rel.TaxonFk2 "
                + " WHERE t1."+origEuroMed+" AND t2." + origEuroMed
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
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT no.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM notes no "
                + "   INNER JOIN tu ON tu.id = no.tu_id "
                + "   LEFT JOIN languages l ON l.LanID = no.lan_id"
                + "   LEFT JOIN notes_sessions MN ON no.id = MN.note_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(tu.id as char(20)), no.type, no.noteSortable, s.sessiondate DESC, a.id DESC ");

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
        success &= isNull("SourceFk", destRs);  //sources should be moved to extra table only, according to script there were values, but in PESI 2014 values existed only in OccurrenceSource table (for all only E+M records)
        success &= isNull("SourceCache", destRs);  //sources should be moved to extra table, see above
//TODO        success &= equals("Distribution notes ", srcRs.getString("note"), destRs.getString("Notes"), id);
        success &= isNull("SpeciesExpertGUID", destRs);  //SpeciesExpertGUID does not exist in EM and according to script
        success &= isNull("SpeciesExpertName", destRs);  //SpeciesExpertName does not exist in EM and according to script
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
        }else if (uuid.equals(PresenceAbsenceTerm.uuidIntroducesAdventitious)  //casual, introduced adventitious
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
        }else if ("Turkey-in-Europe".equals(area)){return "European Turkey";
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
        ResultSet srcRsLastAction = source.getResultSet(""
                + " SELECT v.id, s.sessiondate, a.action_name, s.ExpertName "
                + " FROM vernaculars v "
                + "   INNER JOIN tu ON tu.id = v.tu_id "
                + "   LEFT JOIN languages l ON l.LanID = v.lan_id"
                + "   LEFT JOIN vernaculars_sessions MN ON v.id = MN.vernacular_id "
                + "   LEFT JOIN actions a ON a.id = MN.action_id "
                + "   LEFT JOIN sessions s ON s.id = MN.session_id  "
                + " ORDER BY CAST(tu.id as char(20)), ISNULL([639_3],[639_2]), v.vername, v.id, s.sessiondate DESC, a.id DESC ");
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
        ResultSet srcRS = source.getResultSet("SELECT r.* FROM Reference r ORDER BY r.id ");
        ResultSet destRS = destination.getResultSet("SELECT s.* FROM Source s "
                + " WHERE s." + origEuroMed
                + " ORDER BY s.RefIdInSource ");  // +1 for the source reference "erms" but this has no OriginalDB
        while (srcRS.next() && destRS.next()){
            success &= testSingleReference(srcRS, destRS);
        }
        return success;
    }

    private boolean testSingleReference(ResultSet srcRS, ResultSet destRS) throws SQLException {
        String id = String.valueOf(srcRS.getInt("id"));
        boolean success = equals("Reference ID ", srcRS.getInt("id"), destRS.getInt("RefIdInSource"), id);
        success &= isNull("IMIS_Id", destRS);  //for E+M no IMIS id exists
        success &= equals("Reference SourceCategoryFk ", convertSourceTypeFk(srcRS.getString("refType")), destRS.getInt("SourceCategoryFk"), id);
        success &= equals("Reference SourceCategoryCache ", convertSourceTypeCache(srcRS.getString("refType")), destRS.getString("SourceCategoryCache"), id);
        success &= equals("Reference name ", srcRS.getString("titleCache"), destRS.getString("Name"), id);
        success &= equals("Reference abstract ", srcRS.getString("referenceAbstract"), destRS.getString("Abstract"), id);
        success &= equals("Reference title ", srcRS.getString("title"), destRS.getString("Title"), id);
//        success &= equals("Reference author string ", srcRS.getString("source_author"), destRS.getString("AuthorString"), id);
//        success &= equals("Reference year ", normalizeYear(srcRS.getString("source_year")), destRS.getString("RefYear"), id);
        success &= equals("Reference NomRefCache ", srcRS.getString("abbrevTitleCache"), destRS.getString("NomRefCache"), id);
        //TODO DOI
//        success &= equals("Reference link ", srcRS.getString("source_link"), destRS.getString("Link"), id);
//        success &= equals("Reference note ", srcRS.getString("source_note"), destRS.getString("Notes"), id);
        //TODO see above
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
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM reference ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s."+ origEuroMed);  // +1 for the source reference "erms" but this has no OriginalDB
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
        PesiEuroMedValidator validator = new PesiEuroMedValidator();
        validator.invoke(new Source(defaultSource), defaultDestination);
        System.exit(0);
    }
}
