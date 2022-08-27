/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.util.UUID;

import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.common.UTF8;

/**
 * @author pplitzner
 * @since Mar 7, 2016
 *
 */
public class RedListUtil {

    public static final UUID checkListClassificationUuid = UUID.fromString("928a4695-c055-465e-99da-07322384b0b7");

    public static final UUID checkListReferenceUuid = UUID.fromString("e3abe234-ff23-4d85-9c1f-6769df965f33");
    public static final UUID gesamtListeReferenceUuid = UUID.fromString("ec0308de-2245-4e71-a362-4ee72290bf23");

    public static final UUID uuidClassificationReferenceE = UUID.fromString("0d62803a-430e-465d-aa45-9ba924c52df7");
    public static final UUID uuidClassificationReferenceW = UUID.fromString("ba62f7cc-b489-4ef4-92d3-5cfa6eedf947");
    public static final UUID uuidClassificationReferenceK = UUID.fromString("9d5a5f0b-44f3-415b-b704-37f98c577127");
    public static final UUID uuidClassificationReferenceAW = UUID.fromString("509a95d6-0c9f-461c-94d9-762a7467a918");
    public static final UUID uuidClassificationReferenceAO = UUID.fromString("422d81dc-d7ea-4d78-8836-c50ef4365eb4");
    public static final UUID uuidClassificationReferenceR = UUID.fromString("f802727b-e3fe-4ec3-84cf-153f03239f4d");
    public static final UUID uuidClassificationReferenceO = UUID.fromString("832155f7-caeb-4cf4-a14b-64e0e9710f7a");
    public static final UUID uuidClassificationReferenceS = UUID.fromString("a3085ffa-4f59-4bf8-add9-d4cc84b66047");

    public static final UUID uuidClassificationE = UUID.fromString("87d3d656-57bc-4b54-b338-80f9f2a37435");
    public static final UUID uuidClassificationW = UUID.fromString("1bd21015-f542-4bf9-9e1f-ac0147e3a9f8");
    public static final UUID uuidClassificationK = UUID.fromString("c6120524-f21b-4426-92db-52add2286831");
    public static final UUID uuidClassificationAW = UUID.fromString("b48ca7a0-a641-4fd1-ad51-238a9b57a470");
    public static final UUID uuidClassificationAO = UUID.fromString("a8743caa-56d5-4636-9ce6-4ef272d769d2");
    public static final UUID uuidClassificationR = UUID.fromString("870d2e18-cd1b-4aa1-a360-ba5e0a5905aa");
    public static final UUID uuidClassificationO = UUID.fromString("e19bb2e6-d898-4793-8cd4-d866eeb1f872");
    public static final UUID uuidClassificationS = UUID.fromString("53e81162-5c2d-425b-bbe6-6e8d12e85790");

    public static final UUID uuidRankCollectionSpecies = UUID.fromString("6056e143-4efe-4632-b532-27699ed62884");
    public static final UUID uuidRankModification = UUID.fromString("1c6e16f5-f7a5-41a5-9cc4-53c1438478c9");
    public static final UUID uuidRankSubspeciesPrincipes = UUID.fromString("b6b9351b-6beb-431d-8c7a-d30fe0cf3a90");
    public static final UUID uuidRankCombination = UUID.fromString("ec2c580e-416a-4ecf-85df-d03641f3bd64");
    public static final UUID uuidRankForme = UUID.fromString("05b4f66f-5559-4e44-bbb8-70744a5ea64a");

    public static final String NAME_NAMESPACE = "name";
    public static final String AUTHOR_NAMESPACE = "author";
    public static final String TAXON_GESAMTLISTE_NAMESPACE = "taxon_gesamt_liste";
    public static final String CLASSIFICATION_NAMESPACE_E = "classification_namespace_e";
    public static final String CLASSIFICATION_NAMESPACE_W = "classification_namespace_w";
    public static final String CLASSIFICATION_NAMESPACE_K = "classification_namespace_k";
    public static final String CLASSIFICATION_NAMESPACE_AW = "classification_namespace_aw";
    public static final String CLASSIFICATION_NAMESPACE_AO = "classification_namespace_ao";
    public static final String CLASSIFICATION_NAMESPACE_R = "classification_namespace_r";
    public static final String CLASSIFICATION_NAMESPACE_O = "classification_namespace_o";
    public static final String CLASSIFICATION_NAMESPACE_S = "classification_namespace_s";
    public static final String FAMILY_NAMESPACE_GESAMTLISTE = "family_namespace_gesamtliste";


    //cell content
    public static final String AUCT = "auct.";
    public static final String EX = " ex ";
    public static final String GUELT_BASIONYM = "b";
    public static final String GUELT_SYNONYM = "x";
    public static final String GUELT_ACCEPTED_TAXON = "1";
    public static final String CL_TAXON_B = "b";
    public static final String CL_TAXON_K = "k";
    public static final String HYB_X = "x";
    public static final String HYB_XF = "xf";
    public static final String HYB_XS = "xs";
    public static final String HYB_N = "n";
    public static final String HYB_G = "g";
    public static final String HYB_XU = "xu";
    public static final String HYB_GF = "gf";

    public static final String HYB_SIGN = UTF8.HYBRID.toString();


    //column names
    public static final String NAMNR = "NAMNR";
    public static final String SEQNUM = "SEQNUM";
    public static final String GUELT = "GUELT";
    public static final String LOWER = "LOWER_G";
    public static final String AUTOR_BASI = "AUTOR_BASI";
    public static final String AUTOR_KOMB = "AUTOR_KOMB";
    public static final String ZUSATZ = "ZUSATZ";
    public static final String NOM_ZUSATZ = "NOM_ZUSATZ";
    public static final String TAX_ZUSATZ = "TAX_ZUSATZ";
    public static final String NON = "NON";
    public static final String SENSU = "SENSU";
    public static final String TRIVIAL = "TRIVIAL";
    public static final String EPI3 = "EPI3";
    public static final String EPI2 = "EPI2";
    public static final String EPI1 = "EPI1";
    public static final String RANG = "RANG";
    public static final String TAXNAME = "TAXNAME";
    public static final String AUTOR = "AUTOR";
    public static final String CL_TAXON = "CL_TAXON";
    public static final String HYB = "HYB";
    public static final String FORMEL = "FORMEL";
    public static final String FLOR = "FLOR";
    public static final String ATLAS_IDX = "ATLAS_IDX";
    public static final String KART = "KART";
    public static final String RL2015 = "RL2015";
    public static final String EHRD = "EHRD";
    public static final String WISSK = "WISSK";

    public static final String FAMILIE = "FAMILIE";

    public static final String E = "E";
    public static final String W = "W";
    public static final String K = "K";
    public static final String AW = "AW";
    public static final String AO = "AO";
    public static final String R = "R";
    public static final String O = "O";
    public static final String S = "S1";
    public static final String S2 = "S2";

    public static void logMessage(long id, String message, Logger logger){
        logger.error(NAMNR+": "+id+" "+message);
    }

    public static void logInfoMessage(long id, String message, Logger logger){
        logger.info(NAMNR+": "+id+" "+message);
    }

    public static String getAuthorOfExAuthorshipString(String authorshipString){
        return getAuthorOfExAuthorship(authorshipString, false);
    }

    public static String getExAuthorOfExAuthorshipString(String authorshipString){
        return getAuthorOfExAuthorship(authorshipString, true);
    }

    private static String getAuthorOfExAuthorship(String authorshipString, boolean isExAuthor){
        String[] split = authorshipString.split(EX);
        if(split.length>0){
            return isExAuthor?split[0].trim():split[split.length-1].trim();
        }
        return null;
    }

}
