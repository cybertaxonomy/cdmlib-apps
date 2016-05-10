// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.model.common.Language;

/**
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public class BfnXmlConstants {
    public static final Logger logger = Logger.getLogger(BfnXmlConstants.class);

    public static final Language defaultLanguage = Language.DEFAULT();

    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String EL_DEB_EXPORT = "DEBExport";
    public static final String EL_ROTELISTEDATEN = "ROTELISTEDATEN";
    public static final String ATT_INHALT = "inhalt";

    public static final String EL_EIGENSCHAFTEN = "EIGENSCHAFTEN";
    public static final String EL_EIGENSCHAFT = "EIGENSCHAFT";
    public static final String ATT_STANDARDNAME = "standardname";

    public static final String EL_METAINFOS = "METAINFOS";
    public static final String EL_MWERT = "MWERT";
    public static final String ATT_NAME = "name";

    public static final String EL_LISTENWERTE = "LISTENWERTE";
    public static final String EL_LWERT = "LWERT";
    public static final String ATT_REIHENFOLGE = "reihenfolge";

    public static final String EL_RAUMINFOS = "RAUMINFOS";
    public static final String EL_RAUM = "RAUM";
    public static final String EL_RAUM_WERT = "RWERT";

    public static final String EL_TAXONYME = "TAXONYME";
    public static final String EL_TAXONYM = "TAXONYM";
    public static final String ATT_TAXNR = "taxNr";

    public static final String EL_WISSNAME = "WISSNAME";
    public static final String EL_NANTEIL = "NANTEIL";
    public static final String ATT_BEREICH = "bereich";

    public static final String EL_DEUTSCHENAMEN = "DEUTSCHENAMEN";
    public static final String EL_DNAME = "DNAME";
    public static final String ATT_SEQUENZ = "sequenz";
    public static final String EL_TRIVIALNAME = "TRIVIALNAME";
    public static final String EL_GRUPPE = "GRUPPE";
    public static final String EL_SPEZIFISCH = "SPEZIFISCH";

    public static final String EL_SYNONYME = "SYNONYME";
    public static final String EL_SYNONYM = "SYNONYM";
    public static final String EL_STATI = "STATI";
    public static final String EL_STATUS = "STATUS";
    public static final String ATT_TYP = "typ";

    public static final String EL_INFORMATIONEN = "INFORMATIONEN";
    public static final String EL_BEZUGSRAUM = "BEZUGSRAUM";
    public static final String EL_IWERT = "IWERT";
    public static final String EL_WERT = "WERT";

    public static final String EL_KONZEPTBEZIEHUNGEN = "KONZEPTBEZIEHUNGEN";
    public static final String EL_KONZEPTBEZIEHUNG = "KONZEPTBEZIEHUNG";


    public static final String BEREICH_WISSNAME = "wissName";
    public static final String BEREICH_EINDEUTIGER_CODE = "Eindeutiger Code";
    public static final String BEREICH_EPITHETON1 = "Epitheton 1";
    public static final String BEREICH_EPITHETON2 = "Epitheton 2";
    public static final String BEREICH_EPITHETON3 = "Epitheton 3";
    public static final String BEREICH_EPITHETON4 = "Epitheton 4";
    public static final String BEREICH_EPITHETON5 = "Epitheton 5";
    public static final String BEREICH_RANG = "Rang";
    public static final String BEREICH_ORDNUNGSZAHL = "Ordnungszahl";
    public static final String BEREICH_AUTONYM = "Autonym";
    public static final String BEREICH_REICH = "Reich";
    public static final String BEREICH_BASTARD = "Bastard";
    public static final String BEREICH_AUTOREN = "Autoren";
    public static final String BEREICH_ZUSAETZE = "Zusätze";

    public static final String RNK_SUPERTRIB = "supertrib";
    public static final String RNK_TRIB = "trib";
    public static final String RNK_SUBTRIB = "subtrib";
    public static final String RNK_INTRATRIB = "intratrib";
    public static final String RNK_SUPERFAM = "superfam";
    public static final String RNK_FAM = "fam";
    public static final String RNK_SUBCL = "subcl";
    public static final String RNK_SPEZIES = "spezies";
    public static final String RNK_SSP = "ssp";
    public static final String RNK_SUBFAM = "subfam";
    public static final String RNK_INFRAFAM = "infrafam";
    public static final String RNK_AUSWERTUNGSGRUPPE = "Auswertungsgruppe";
    public static final String RNK_TAXSUPRAGEN = "taxsupragen";
    public static final String RNK_DOM = "dom";
    public static final String RNK_SUPERREG = "superreg";
    public static final String RNK_REG = "reg";
    public static final String RNK_SUBREG = "subreg";
    public static final String RNK_INFRAREG = "infrareg";
    public static final String RNK_SUPERPHYL_DIV = "superphyl_div";
    public static final String RNK_PHYL_DIV = "phyl_div";
    public static final String RNK_SUBPHYL_DIV = "subphyl_div";
    public static final String RNK_INFRAPHYL_DIV = "infraphyl_div";
    public static final String RNK_SUPERCL = "supercl";
    public static final String RNK_CL = "cl";
    public static final String RNK_INFRACL = "infracl";
    public static final String RNK_SUPERORD = "superord";
    public static final String RNK_ORD = "ord";
    public static final String RNK_INFRAORD = "infraord";
    public static final String RNK_INFRASP = "infrasp";
    public static final String RNK_VAR_DOT = "var.";
    public static final String RNK_VAR = "var";
    public static final String RNK_SUBVAR = "subvar";
    public static final String RNK_SUBSUBVAR = "subsubvar";
    public static final String RNK_F = "f.";
    public static final String RNK_FM = "fm";
    public static final String RNK_SUBFM = "subfm";
    public static final String RNK_SUBSUBFM = "subsubfm";
    public static final String RNK_FSP = "fsp";
    public static final String RNK_TAXINFRASP = "taxinfrasp";
    public static final String RNK_CAND = "cand";
    public static final String RNK_SP = "sp";
    public static final String RNK_SUBSP = "subsp";
    public static final String RNK_SUBSP_DOT = "subsp.";
    public static final String RNK_SUBSP_AGGR = "subsp_aggr";
    public static final String RNK_SECT = "sect";
    public static final String RNK_SUBSECT = "subsect";
    public static final String RNK_SER = "ser";
    public static final String RNK_SUBSER = "subser";
    public static final String RNK_TAXINFRAGEN = "taxinfragen";
    public static final String RNK_AGG = "agg.";
    public static final String RNK_AGGR = "aggr";
    public static final String RNK_GEN = "gen";
    public static final String RNK_SUBGEN = "subgen";
    public static final String RNK_INFRAGEN = "infragen";

    public static final String VOC_REDLIST_FEATURES = "RedList Feature";
    public static final String VOC_BUNDESLAENDER = "Bundesländer";
    public static final String VOC_ETABLIERUNGSSTATUS = "Etablierungsstatus";
    public static final String VOC_VORKOMMENSSTATUS = "Vorkommensstatus";
    public static final String VOC_SONDERFAELLE = "Sonderfälle";
    public static final String VOC_EINDEUTIGER_CODE = "Eindeutiger Code";
    public static final String VOC_NEOBIOTA = "Neobiota";
    public static final String VOC_ALTE_RL_KAT = "alte RL- Kat.";
    public static final String VOC_VERANTWORTLICHKEIT = "Verantwortlichkeit";
    public static final String VOC_RISIKOFAKTOREN = "Risikofaktoren";
    public static final String VOC_KURZFRISTIGER_BESTANDSTREND = "kurzfristiger Bestandstrend";
    public static final String VOC_LANGFRISTIGER_BESTANDSTREND = "langfristiger Bestandstrend";
    public static final String VOC_AKTUELLE_BESTANDSSTITUATION = "aktuelle Bestandsstituation";
    public static final String VOC_KAT = "Kat. +/-";
    public static final String VOC_RL_KAT = "RL Kat.";

    public static final String BEZUGRAUM_BUND = "Bund";

    //redList feature vocabulary
    public static final UUID vocRLFeatures =  UUID.fromString("74091f30-faa0-487b-bd7e-c82eed05d3c9");
   
    public static final UUID TAX_NR_IDENTIFIER =  UUID.fromString("7d12de50-0db7-47b3-bb8e-703ad1d54fbc");

    //redList state Vocabularies
    public static final UUID vocStateRLKat =  UUID.fromString("66bbb391-af8a-423b-9506-a235b61af922");
    public static final UUID vocStateRlKatDiff =  UUID.fromString("abe3702e-ddd4-420c-a985-08a0f8138215");
    public static final UUID vocStateRlAkt =  UUID.fromString("a0bb10de-35c1-47f5-b700-02ceb0a6b50c");
    public static final UUID vocStateRLLang =  UUID.fromString("c48d99db-50b6-469f-879d-8bb781842382");
    public static final UUID vocStateRLKurz =  UUID.fromString("46549c3b-d9d0-4d34-9135-4789d5ed6fff");
    public static final UUID vocStateRLRisk =  UUID.fromString("ec38db54-07dd-4e48-8976-bfa4813ffa44");
    public static final UUID vocStateRLResp =  UUID.fromString("c4763d33-75ea-4387-991f-b767650b4899");
    public static final UUID vocStateRLKatOld =  UUID.fromString("e9be0626-e14e-4556-a8af-9d49e6279669");
    public static final UUID vocStateRLNeo =  UUID.fromString("6c55ae1d-046d-4b67-89aa-b24c4888df6a");
    public static final UUID vocStateRLKatId =  UUID.fromString("c54481b3-bf07-43ce-b1cb-09759e4d2a70");
    public static final UUID vocStateRLSpecialCases =  UUID.fromString("ce2f4f8f-4222-429f-938b-77b794ecf704");
    public static final UUID vocGermanFederalStates =  UUID.fromString("a80dc3d4-0def-4c9b-97a1-12e8eb7ec87f");
    public static final UUID vocGermanPresenceTerms =  UUID.fromString("57d6bfa6-ac49-4c88-a9f0-b9c763d5b521");
    public static final UUID vocGermanEstablishmentTerms =  UUID.fromString("b5919067-ec28-404a-a22e-be914c810f22");

    //redlist feature
    public static final UUID featureRLKat =  UUID.fromString("744f8833-619a-4d83-b330-1997c3b2c2f9");
    public static final UUID featureRlKatDiff =  UUID.fromString("bf93361d-0c8c-4961-9f60-20bcb1d3dbaf");
    public static final UUID featureRlAkt =  UUID.fromString("39b6962b-05ba-4cd6-a1a9-337d5d156e2f");
    public static final UUID featureRLLang =  UUID.fromString("f6027318-b17d-49e6-b8eb-7464304044c8");
    public static final UUID featureRLKurz =  UUID.fromString("9ecc65b5-7760-4ce7-add0-950bdcc2c792");
    public static final UUID featureRLRisk =  UUID.fromString("2c8f8ffa-c604-4385-b428-4485f5650735");
    public static final UUID featureRLResp =  UUID.fromString("02d8010f-7d1b-46a3-8c01-b5e6760bfd14");
    public static final UUID featureRLKatOld =  UUID.fromString("bbdff68d-4fa0-438d-afb5-cff89791c93f");
    public static final UUID featureRLNeo =  UUID.fromString("153c7173-6d3d-4bee-b8f2-cf8e63e0bc25");
    public static final UUID featureRLKatId =  UUID.fromString("dc9f5dd2-302c-4a32-bd70-278bbd9abd16");
    public static final UUID featureRLTaxComment =  UUID.fromString("b7c05d78-16a4-4b6e-a03b-fa6bb2ed74ae");
    public static final UUID featureRLHazardComment =  UUID.fromString("5beb1ebf-8643-4d5f-9849-8087c35455bb");
    public static final UUID featureRLSpecialCases =  UUID.fromString("fb92068d-667a-448e-8019-ca4551891b3b");
    public static final UUID featureRLLastOccurrence =  UUID.fromString("218a32be-fb87-41c9-8d64-b21b43b47caa");
    public static final UUID featureRLAdditionalComment =  UUID.fromString("c610c98e-f242-4f3b-9edd-7b84a9435867");
    public static List<UUID> featureUuids = new ArrayList<UUID>();
        static {
            featureUuids.add(featureRLKat);
            featureUuids.add(featureRlKatDiff);
            featureUuids.add(featureRlAkt);
            featureUuids.add(featureRLLang);
            featureUuids.add(featureRLKurz);
            featureUuids.add(featureRLRisk);
            featureUuids.add(featureRLResp);
            featureUuids.add(featureRLKatOld);
            featureUuids.add(featureRLNeo);
            featureUuids.add(featureRLKatId);
            featureUuids.add(featureRLTaxComment);
            featureUuids.add(featureRLHazardComment);
            featureUuids.add(featureRLSpecialCases);
            featureUuids.add(featureRLLastOccurrence);
            featureUuids.add(featureRLAdditionalComment);
    }

    //rl kat state list
    public static final UUID stateTermRlKat0 = UUID.fromString("05ff7c0f-2fb2-4c10-9527-a2e0c68d68af");
    public static final UUID stateTermRlKat1 = UUID.fromString("76a6defc-41d0-43bf-a15a-997caeefbbce");
    public static final UUID stateTermRlKat2 = UUID.fromString("ee6b79b6-8306-42d1-a80a-2963ded7c952");
    public static final UUID stateTermRlKat3 = UUID.fromString("309bf199-c0a3-4f01-829a-b10aafda4547");
    public static final UUID stateTermRlKatG = UUID.fromString("fdf9c84e-1b76-4aa8-b676-a614591ad320");
    public static final UUID stateTermRlKatR = UUID.fromString("a694e7bd-87a4-4d3c-8333-aed5092bcb0e");
    public static final UUID stateTermRlKatV = UUID.fromString("b1a6695d-65f9-4c53-9765-fd7b54e1674c");
    public static final UUID stateTermRlKatStar = UUID.fromString("1cda0ef4-cace-42e9-8061-4ada41d03974");
    public static final UUID stateTermRlKatStar2 = UUID.fromString("539cffb0-29b9-48fd-af6e-abf9c466199c");
    public static final UUID stateTermRlKatD = UUID.fromString("4d61cadd-b27e-41da-9c91-f29e96adaf89");
    public static final UUID stateTermRlKatDiamond = UUID.fromString("aedc4006-4097-41cd-bab9-f8607ff84519");
    public static final UUID stateTermRlKatKN = UUID.fromString("49dc7656-4cef-4b0e-81dd-8422a3d0d06b");

    //rl kat diff state list
    public static final UUID stateTermRLKatDiffPlus = UUID.fromString("6bc7ddc2-6f25-4076-a392-2626cb7a4b35");
    public static final UUID stateTermRLKatDiffMinus = UUID.fromString("8f6a8c16-195c-4084-a201-8d702f9636e7");
    public static final UUID stateTermRLKatDiffEqual = UUID.fromString("5215fd8a-7e70-43a6-abde-4e14966a0e0e");

    //rl kat neo state list
    public static final UUID stateTermRLKatNeo = UUID.fromString("d9ae3dc2-99c9-40aa-b724-9810ed52ca15");

    //rl kat akt state list
    public static final UUID stateTermRLKatAktEx = UUID.fromString("a36d4251-0ca1-4818-bbf7-4089a9362a7e");
    public static final UUID stateTermRLKatAktEs = UUID.fromString("9d2426a2-d845-47df-9607-01addc4e3253");
    public static final UUID stateTermRLKatAktSs = UUID.fromString("bde09fdd-459a-4f8e-a83c-ee562e220f52");
    public static final UUID stateTermRLKatAktS = UUID.fromString("3f44fbd3-6d02-4cef-a2c7-c29684b4eb20");
    public static final UUID stateTermRLKatAktMh = UUID.fromString("88e3cfff-623a-43b0-a708-e4d7125a504c");
    public static final UUID stateTermRLKatAktH = UUID.fromString("ec96d3f0-0f32-4121-9636-41c44079c9ea");
    public static final UUID stateTermRLKatAktSh = UUID.fromString("1126e1ad-5c06-43b2-bfd5-8327257a41eb");
    public static final UUID stateTermRLKatAktQuest = UUID.fromString("2ee9820e-c98d-4d5a-8621-5d7b73be66c2");
    public static final UUID stateTermRLKatAktNb = UUID.fromString("5da81f91-0089-4360-b07c-b3b833f8fc8e");
    public static final UUID stateTermRLKatAktKn = UUID.fromString("f10865f9-aa13-4cf0-9e6c-cc657103bd13");

    //rl kat lang state list
    public static final UUID stateTermRLKatLangLT3 = UUID.fromString("6d23b5f9-ac18-4ecb-9be8-2c6e5e7db736");
    public static final UUID stateTermRLKatLangLT2 = UUID.fromString("1bd75728-79ed-427d-b96e-858ddca6103d");
    public static final UUID stateTermRLKatLangLT1 = UUID.fromString("b7c592a4-72cd-4914-87f0-05a6b324af43");
    public static final UUID stateTermRLKatLangLT = UUID.fromString("5f202b93-6f20-4bae-ba3c-e2792b5451b4");
    public static final UUID stateTermRLKatLangEqual = UUID.fromString("d66be068-4a0c-4f95-aa6e-9e5804ceb1f1");
    public static final UUID stateTermRLKatLangGT = UUID.fromString("528185c6-3c12-41bd-a1e5-6ee3d729776c");
    public static final UUID stateTermRLKatLangQuest = UUID.fromString("53076429-d4ac-427f-a9dc-2c8a15901999");

    //rl kat kurz state list
    public static final UUID stateTermRLKatKurzDown3 = UUID.fromString("dd97697c-004a-4860-a553-67695d32a992");
    public static final UUID stateTermRLKatKurzDown2 = UUID.fromString("311a531b-8263-4c72-af79-662ffbc26fbe");
    public static final UUID stateTermRLKatKurzDown1 = UUID.fromString("d2a2a51f-5c8f-4cef-809a-58162beae5c2");
    public static final UUID stateTermRLKatKurzEqual = UUID.fromString("0a8cf4c0-8b7c-49d5-9195-0999a0f202ad");
    public static final UUID stateTermRLKatKurzUp = UUID.fromString("13e7c95c-3ca3-435d-b7b6-4889e594bf2a");
    public static final UUID stateTermRLKatKurzQuest = UUID.fromString("4eb11517-a874-484d-8390-dbb8c6bda47c");

    //rl kat risk state list
    public static final UUID stateTermRLKatRiskMinus = UUID.fromString("4e96c671-e1f7-4273-83e7-6650207b57e0");

    public static final UUID stateTermRLKatRiskEqual = UUID.fromString("d3f00d31-26a4-40c0-99d4-55ea3672ff5d");

    //rl kat resp state list
    public static final UUID stateTermRLKatRespBang2 = UUID.fromString("d1e6b6cd-bb19-40a3-9d02-33099295e7f7");
    public static final UUID stateTermRLKatRespBang1 = UUID.fromString("01856904-aced-4889-b955-d16872bcd0e8");
    public static final UUID stateTermRLKatRespBang = UUID.fromString("c432e39e-ec09-41b7-be9b-28e0d76a4cf9");
    public static final UUID stateTermRLKatRespQuest = UUID.fromString("4116fbda-a392-417c-be1c-08f5e72f762b");
    public static final UUID stateTermRLKatRespNb = UUID.fromString("800328ce-f618-4de1-9237-243f16fbe9f9");

    //rl Kat old state list
    public static final UUID stateTermRLKatOld0 = UUID.fromString("1977b29c-8b63-407a-a11b-ada8726ac653");
    public static final UUID stateTermRLKatOld1 = UUID.fromString("4f3d3255-7e06-4a4e-83d1-5841beee85d4");
    public static final UUID stateTermRLKatOld2 = UUID.fromString("65163104-76db-4c20-a00f-5c7531f42b3b");
    public static final UUID stateTermRLKatOld3 = UUID.fromString("3c0fbb9e-b3fa-4bab-ada2-6efb9b6b9155");
    public static final UUID stateTermRLKatOldG = UUID.fromString("a3d2daf2-a570-40d7-b08d-d105a30bb5e2");
    public static final UUID stateTermRLKatOldR = UUID.fromString("d6e511cb-40aa-48ab-8a0d-2365c984d698");
    public static final UUID stateTermRLKatOldV = UUID.fromString("92a6f1ec-6d61-4879-826a-255c34492507");
    public static final UUID stateTermRLKatOldStar = UUID.fromString("196310bc-3b8b-43c9-b317-e2b02bff5c8a");
    public static final UUID stateTermRLKatOldStar2 = UUID.fromString("7fbc42ed-43b6-4198-a9dc-bb97bacb3b9b");
    public static final UUID stateTermRLKatOldD = UUID.fromString("075a456c-2291-436c-9b9c-b06d95bf6fc6");
    public static final UUID stateTermRLKatOldNb = UUID.fromString("72faec78-6db9-4471-9a65-c6d2337bd324");
    public static final UUID stateTermRLKatOldKn = UUID.fromString("92276f3e-3c09-4761-ba5b-b49697c6d5ce");
    public static final UUID stateTermEmpty = UUID.fromString("1d357340-5329-4f43-a454-7f99625a1d71");
    public static final UUID stateTermRLSpecialS = UUID.fromString("71fda1f6-a7eb-44a0-aeb8-e7f676096916");
    public static final UUID stateTermRLSpecialE = UUID.fromString("ef335a01-f4f1-4a02-95a2-2254aa457774");
    public static final UUID stateTermRLSpecialD = UUID.fromString("6b267cc5-49b6-4ebd-87ec-aa574e9cbcc5");

}
