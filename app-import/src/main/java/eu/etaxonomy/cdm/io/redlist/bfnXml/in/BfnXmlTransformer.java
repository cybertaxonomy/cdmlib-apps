/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

public final class BfnXmlTransformer extends InputTransformerBase{

    private static final long serialVersionUID = -4795356792130338005L;

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BfnXmlTransformer.class);

    public static final String RAUTE = "\u2666";

    public static final String ABWESEND_FEHLEINGABE = "abwesend - frühere Fehleingabe";
    public static final String ABWESEND = "abwesend";
    public static final String ABWESEND_AUSGESTORBEN = "abwesend - ausgestorben";
    public static final String ABWESEND_SEIT1980 = "abwesend - letzter Nachweis 1901-1980";
    public static final String ABWESEND_ABGELEHNT = "abgelehnt";
    public static final String ABWESEND_KEIN_NACHWEIS = "kein Nachweis";
    public static final String VORHANDEN_UNSICHER = "vorkommend - unsicher";
    public static final String VORHANDEN_VORKOMMEN_UNSICHER = "vorkommend - Vorkommen unsicher";
    public static final String VORHANDEN_UNBESTAENDIG = "vorkommend - unbeständig";
    public static final String VORHANDEN_KULTIVIERT_DOMESTIZIERT = "vorkommend - kultiviert, domestiziert";
    public static final String VORHANDEN_ETABLIERT = "vorkommend - etabliert";
    public static final String VORHANDEN_EINBUERGERUNG = "vorkommend - in Einbürgerung befindlich";
    public static final String VORHANDEN = "vorkommend";

    public static final UUID uuidAreaDeutschland = UUID.fromString("a7f3855e-d4fa-4313-8fcf-da792ef848e7");
    public static final UUID uuidAreaHamburg = UUID.fromString("f087a7d7-974f-4627-a414-df27c04f99dd");
    public static final UUID uuidAreaHessen = UUID.fromString("59de29e6-bf32-4677-89c7-a6834fcb5085");
    public static final UUID uuidAreaMV = UUID.fromString("06dccbd5-8d5a-4e4f-b56e-d1d74ab25c19");
    public static final UUID uuidAreaNiedersachsen = UUID.fromString("97f77fe8-07ab-4e14-8f8b-40e8caf7e653");
    public static final UUID uuidAreaSachsen = UUID.fromString("ca3ef152-ee3a-45f2-8343-983cf0fdddbd");
    public static final UUID uuidAreaTH = UUID.fromString("72e18526-6bf7-4300-8329-53cab5da2b51");
    public static final UUID uuidAreaSH = UUID.fromString("863323a7-22fb-4070-ad94-ce317098a28a");
    public static final UUID uuidAreaSachsenAnhalt = UUID.fromString("bb95b9a4-87ee-49bd-a542-4c30289e8d1f");
    public static final UUID uuidAreaSaarland = UUID.fromString("26d3e85f-ce90-43ae-8ac0-42a60302b7b7");
    public static final UUID uuidAreaRP = UUID.fromString("dd3ddb29-b1ec-4937-99a9-4a94d383becf");
    public static final UUID uuidAreaNRW = UUID.fromString("46bf702e-1438-470c-9c77-04202c34ebf2");
    public static final UUID uuidAreaBB = UUID.fromString("dda9d8b8-8090-4667-953e-d8b1f7243926");
    public static final UUID uuidAreaBremen = UUID.fromString("a6d2f97d-5dba-4b79-a073-25fb491b6320");
    public static final UUID uuidAreaBerlin = UUID.fromString("d9339e12-7efa-45df-a008-3c934b9386bc");
    public static final UUID uuidAreaBY = UUID.fromString("ba075265-368f-4ff0-8942-88546239c70a");
    public static final UUID uuidAreaBW = UUID.fromString("00e64948-9ce9-4ebf-961b-133c56517b1c");

    public static final UUID uuidAreaBB_BE = UUID.fromString("14603e8c-d8c8-45ab-99f3-c5291c00899b");
    public static final UUID uuidAreaSH_HH = UUID.fromString("7484a4a2-fcaa-48ee-bb4a-ae266e8178ba");

    public static final UUID uuidAreaHGL = UUID.fromString("519d33a9-529b-40e9-bc14-5ee4eae2b225");
    public static final UUID uuidAreaNIW = UUID.fromString("cdcb0f07-29cc-443a-8119-bcd9ed12a7de");
    public static final UUID uuidAreaSHW = UUID.fromString("67d8cac8-277a-414d-bb59-6c1988d63a22");
    public static final UUID uuidAreaSHO = UUID.fromString("efccc336-4fe7-49ad-85c2-2dcb5cbacb3e");
    public static final UUID uuidAreaMVO = UUID.fromString("62bca830-f544-4828-9d28-30743f2d3e23");

    public static final UUID uuidAreaAEWN = UUID.fromString("d8e6a996-6cdb-49bc-bf58-1248d60355f0");
    public static final UUID uuidAreaSuN = UUID.fromString("dc5bd8ab-c7da-4d49-8e51-ffc8e2b167dc");
    public static final UUID uuidAreaHel = UUID.fromString("e367be33-0988-426d-8165-2d538d3d482e");
    public static final UUID uuidAreaDog = UUID.fromString("b903337e-2fa2-4974-9020-109fdfef1ddb");
    public static final UUID uuidAreaOst = UUID.fromString("08719b27-0f2e-4487-aa2e-f4f95031e4cd");


    //Vorkommens status
    public static final UUID uuidStatusAusgestorben = UUID.fromString("7a620705-7c0d-4c72-863f-f41d548a2cc5");
    public static final UUID uuidStatusFehleingabe = UUID.fromString("1009264c-197d-43d4-ba16-7a7f0a6fde0c");
    public static final UUID uuidStatusEinbuergerung = UUID.fromString("ec2f4099-82f7-44de-8892-09651c76d255");
    public static final UUID uuidStatusEtabliert = UUID.fromString("c1954b3c-58b5-43f3-b122-c872b2708bba");
    public static final UUID uuidStatusKultiviert = UUID.fromString("99ebdb24-fda0-4203-9455-30441cdee17b");
    public static final UUID uuidStatusUnbestaendig = UUID.fromString("12566e82-cdc2-48e4-951d-2fb88f30c5fd");
    public static final UUID uuidStatusVorkommenUnsicher = UUID.fromString("a84d2ddb-fe7b-483b-96ba-fc0884d77c81");
    public static final UUID uuidStatusUnsicher = UUID.fromString("0b144b76-dab6-40da-8511-898f8226a24a");
    public static final UUID uuidStatusAbwesend = UUID.fromString("517c4c68-952e-4580-8379-66a4aa12c04b");
    public static final UUID uuidStatusVorkommend = UUID.fromString("b294e7db-919f-4da0-9ba4-c374e7876aff");
    public static final UUID uuidStatusKeinNachweis = UUID.fromString("1512c771-8daa-410d-8329-5df57229bfa6");
    public static final UUID uuidStatusAbgelehnt = UUID.fromString("e693a468-5814-4fce-bd17-18e9225c38a7");
    public static final UUID uuidStatusKeinNachweisNach1980 = UUID.fromString("55e7be75-91c7-46e2-aa01-8bf94eaaccc4");


    private static final UUID uuidEstablishKulturflucht = UUID.fromString("411f9190-56b7-41dd-a31a-3f200619c5e0");
    private static final UUID uuidEstablishNeophyt = UUID.fromString("fdf6f1b7-c6ad-4b49-bc6b-b06398f8b1b5");
    private static final UUID uuidEstablishKulturpflanzeDomestiziert = UUID.fromString("94aa6408-f950-4e2e-bded-e01a1be859f6");
    private static final UUID uuidEstablishIndigen = UUID.fromString("20a99907-406a-45f1-aa3e-4768697488e4");
    private static final UUID uuidEstablishArchaeophyt = UUID.fromString("2cd2bc48-9fcb-4ccd-b03d-bafc0d3dde8c");


    //redList feature vocabulary
    public static final UUID vocRLFeatures =  UUID.fromString("74091f30-faa0-487b-bd7e-c82eed05d3c9");

    public static final UUID UUID_TAX_NR_IDENTIFIER_TYPE =  UUID.fromString("7d12de50-0db7-47b3-bb8e-703ad1d54fbc");
    public static final UUID UUID_REIHENFOLGE_IDENTIFIER_TYPE =  UUID.fromString("97961851-b1c1-41fb-adfd-2961b48f7efe");

    //redList Vocabularies
    public static final UUID uuidVocGermanFederalStates =  UUID.fromString("a80dc3d4-0def-4c9b-97a1-12e8eb7ec87f");
    public static final UUID uuidVocCombinedStates =  UUID.fromString("5d4a7452-7571-4f9b-a720-c2e44b64b975");
    public static final UUID uuidVocMarineAlgaeAreas =  UUID.fromString("306d9afd-6be4-4bf7-844d-51ebf2cc6e08");
    public static final UUID uuidVocMarineInvertebratesAreas =  UUID.fromString("5c9f09a9-e3cf-4b6a-94d4-0942aaea2d97");

    public static final UUID uuidVocStateRLKat =  UUID.fromString("66bbb391-af8a-423b-9506-a235b61af922");
    public static final UUID uuidVocStateRlKatDiff =  UUID.fromString("abe3702e-ddd4-420c-a985-08a0f8138215");
    public static final UUID uuidVocStateRlAkt =  UUID.fromString("a0bb10de-35c1-47f5-b700-02ceb0a6b50c");
    public static final UUID uuidVocStateRLLang =  UUID.fromString("c48d99db-50b6-469f-879d-8bb781842382");
    public static final UUID uuidVocStateRLKurz =  UUID.fromString("46549c3b-d9d0-4d34-9135-4789d5ed6fff");
    public static final UUID uuidVocStateRLRisk =  UUID.fromString("ec38db54-07dd-4e48-8976-bfa4813ffa44");
    public static final UUID uuidVocStateRLResp =  UUID.fromString("c4763d33-75ea-4387-991f-b767650b4899");
    public static final UUID uuidVocStateRLKatOld =  UUID.fromString("e9be0626-e14e-4556-a8af-9d49e6279669");
    public static final UUID uuidVocStateRLNeo =  UUID.fromString("6c55ae1d-046d-4b67-89aa-b24c4888df6a");
    public static final UUID uuidVocStateRLKatId =  UUID.fromString("c54481b3-bf07-43ce-b1cb-09759e4d2a70");
    public static final UUID uuidVocStateRLSpecialCases =  UUID.fromString("ce2f4f8f-4222-429f-938b-77b794ecf704");
    public static final UUID uuidVocGermanPresenceTerms =  UUID.fromString("57d6bfa6-ac49-4c88-a9f0-b9c763d5b521");
    public static final UUID uuidVocGermanEstablishmentTerms =  UUID.fromString("b5919067-ec28-404a-a22e-be914c810f22");

    //redlist feature
    public static final UUID uuidFeatureRLKat =  UUID.fromString("744f8833-619a-4d83-b330-1997c3b2c2f9");
    public static final UUID uuidFeatureRlKatDiff =  UUID.fromString("bf93361d-0c8c-4961-9f60-20bcb1d3dbaf");
    public static final UUID uuidFeatureRlAkt =  UUID.fromString("39b6962b-05ba-4cd6-a1a9-337d5d156e2f");
    public static final UUID uuidFeatureRLLang =  UUID.fromString("f6027318-b17d-49e6-b8eb-7464304044c8");
    public static final UUID uuidFeatureRLKurz =  UUID.fromString("9ecc65b5-7760-4ce7-add0-950bdcc2c792");
    public static final UUID uuidFeatureRLRisk =  UUID.fromString("2c8f8ffa-c604-4385-b428-4485f5650735");
    public static final UUID uuidFeatureRLResp =  UUID.fromString("02d8010f-7d1b-46a3-8c01-b5e6760bfd14");
    public static final UUID uuidFeatureRLKatOld =  UUID.fromString("bbdff68d-4fa0-438d-afb5-cff89791c93f");
    public static final UUID uuidFeatureRLNeo =  UUID.fromString("153c7173-6d3d-4bee-b8f2-cf8e63e0bc25");
    public static final UUID uuidFeatureRLKatId =  UUID.fromString("dc9f5dd2-302c-4a32-bd70-278bbd9abd16");
    public static final UUID uuidFeatureRLTaxComment =  UUID.fromString("b7c05d78-16a4-4b6e-a03b-fa6bb2ed74ae");
    public static final UUID uuidFeatureRLHazardComment =  UUID.fromString("5beb1ebf-8643-4d5f-9849-8087c35455bb");
    public static final UUID uuidFeatureRLSpecialCases =  UUID.fromString("fb92068d-667a-448e-8019-ca4551891b3b");
    public static final UUID uuidFeatureRLLastOccurrence =  UUID.fromString("218a32be-fb87-41c9-8d64-b21b43b47caa");
    public static final UUID uuidFeatureRLAdditionalComment =  UUID.fromString("c610c98e-f242-4f3b-9edd-7b84a9435867");

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

	private static final BiMap<String, Rank> rankMap = HashBiMap.create();
	static {
	    rankMap.put(BfnXmlConstants.RNK_INFRAGEN, Rank.INFRAGENUS());
	    rankMap.put(BfnXmlConstants.RNK_SUBGEN, Rank.SUBGENUS());
	    rankMap.put(BfnXmlConstants.RNK_GEN, Rank.GENUS());
	    //genus subdivision
	    rankMap.put(BfnXmlConstants.RNK_AGGR, Rank.SPECIESAGGREGATE());
	    rankMap.put(BfnXmlConstants.RNK_TAXINFRAGEN, Rank.INFRAGENERICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_SUBSER, Rank.SUBSERIES());
	    rankMap.put(BfnXmlConstants.RNK_SER, Rank.SERIES());
	    rankMap.put(BfnXmlConstants.RNK_SUBSECT, Rank.SUBSECTION_BOTANY());
	    rankMap.put(BfnXmlConstants.RNK_SECT, Rank.SECTION_BOTANY());
	    //species group
	    rankMap.put(BfnXmlConstants.RNK_SUBSP_AGGR, Rank.SUBSPECIFICAGGREGATE());
	    rankMap.put(BfnXmlConstants.RNK_SSP, Rank.SUBSPECIES());
	    rankMap.put(BfnXmlConstants.RNK_SPEZIES, Rank.SPECIES());
	    //below subspecies
	    rankMap.put(BfnXmlConstants.RNK_CAND, Rank.CANDIDATE());
	    rankMap.put(BfnXmlConstants.RNK_TAXINFRASP, Rank.INFRASPECIFICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_FSP, Rank.SPECIALFORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBSUBFM, Rank.SUBSUBFORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBFM, Rank.SUBFORM());
	    rankMap.put(BfnXmlConstants.RNK_FM, Rank.FORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBSUBVAR, Rank.SUBSUBVARIETY());
	    rankMap.put(BfnXmlConstants.RNK_SUBVAR, Rank.SUBVARIETY());
	    rankMap.put(BfnXmlConstants.RNK_VAR, Rank.VARIETY());
	    rankMap.put(BfnXmlConstants.RNK_INFRASP, Rank.INFRASPECIES());
	    //above superfamily
	    rankMap.put(BfnXmlConstants.RNK_INFRAORD, Rank.INFRAORDER());
	    rankMap.put(BfnXmlConstants.RNK_ORD, Rank.ORDER());
	    rankMap.put(BfnXmlConstants.RNK_SUPERORD, Rank.SUPERORDER());
	    rankMap.put(BfnXmlConstants.RNK_INFRACL, Rank.INFRACLASS());
	    rankMap.put(BfnXmlConstants.RNK_SUBCL, Rank.SUBCLASS());
	    rankMap.put(BfnXmlConstants.RNK_CL, Rank.CLASS());
	    rankMap.put(BfnXmlConstants.RNK_SUPERCL, Rank.SUPERCLASS());
	    rankMap.put(BfnXmlConstants.RNK_INFRAPHYL_DIV, Rank.INFRAPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_SUBPHYL_DIV, Rank.SUBPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_PHYL_DIV, Rank.PHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_SUPERPHYL_DIV, Rank.SUPERPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_INFRAREG, Rank.INFRAKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_SUBREG, Rank.SUBKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_REG, Rank.KINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_SUPERREG, Rank.SUPERKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_DOM, Rank.DOMAIN());
	    rankMap.put(BfnXmlConstants.RNK_TAXSUPRAGEN, Rank.SUPRAGENERICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_AUSWERTUNGSGRUPPE, Rank.EMPIRE());
	    //family group
	    rankMap.put(BfnXmlConstants.RNK_INFRAFAM, Rank.FAMILY());
	}

    public static String getRankCodeForRank(Rank rank) {
        return rankMap.inverse().get(rank);
    }

    /** Creates an cdm-Rank by the tcs rank
     */
    public static Rank getRankForRankCode (String rankCode){
        if (rankCode == null){
            return null;
        }
        //handle ambiguous key
        else if (rankCode.equals(BfnXmlConstants.RNK_AGG)){return Rank.SPECIESAGGREGATE();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBSP)){return Rank.SUBSPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBSP_DOT)){return Rank.SUBSPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_INFRAGEN)){return Rank.INFRAGENUS();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SP)){return Rank.SPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_F)){return Rank.FORM();
        }else if (rankCode.equals(BfnXmlConstants.RNK_VAR_DOT)){return Rank.VARIETY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBFAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_FAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUPERFAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_INTRATRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBTRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_TRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUPERTRIB)){return Rank.FAMILY();
        }else {
            return rankMap.get(rankCode);
        }
    }

    private static final BiMap<String, TaxonRelationshipType> relationshipTypeMap = HashBiMap.create();
    static {
        relationshipTypeMap.put("!=", TaxonRelationshipType.CONGRUENT_TO());
        relationshipTypeMap.put("!=,>", TaxonRelationshipType.CONGRUENT_OR_INCLUDES());
        relationshipTypeMap.put("!=,<", TaxonRelationshipType.CONGRUENT_OR_INCLUDED_OR_INCLUDES());
        relationshipTypeMap.put(">", TaxonRelationshipType.INCLUDES());
        relationshipTypeMap.put(">,><", TaxonRelationshipType.INCLUDES_OR_OVERLAPS());
        relationshipTypeMap.put("><", TaxonRelationshipType.OVERLAPS());
        relationshipTypeMap.put("~", TaxonRelationshipType.CONGRUENT_OR_INCLUDES_OR_OVERLAPS());//TODO Included in not here
        relationshipTypeMap.put("?", TaxonRelationshipType.ALL_RELATIONSHIPS());
        relationshipTypeMap.put("/=", TaxonRelationshipType.EXCLUDES());
    }

    public static String getConceptCodeForTaxonRelation(TaxonRelationshipType type) {
        return relationshipTypeMap.inverse().get(type);
    }

	public static TaxonRelationshipType getTaxonRelationForConceptCode(String conceptCode){
		if(conceptCode == null) {
			return null;
		}
        //handle ambiguous key
		else if(conceptCode.equalsIgnoreCase("=!")){
			return TaxonRelationshipType.CONGRUENT_TO();
		}else if(conceptCode.equalsIgnoreCase("\\")){
			return TaxonRelationshipType.EXCLUDES();
		}
		else{
			return relationshipTypeMap.get(conceptCode);
		}
	}

	public static String redListString2RedListCode (String redListCode) throws UnknownCdmTypeException{
		if (redListCode == null){ return null;
		}else if (redListCode.equals("#dtpl_RLKat_NULL#")){return "0";
		}else if (redListCode.equals("#dtpl_RLKat_EINS#")){return "1";
		}else if (redListCode.equals("#dtpl_RLKat_ZWEI#")){return "2";
		}else if (redListCode.equals("#dtpl_RLKat_DREI#")){return "3";
		}else if (redListCode.equals("#dtpl_RLKat_VIER#")){return "4";
		}else if (redListCode.equals("#dtpl_RLKat_G#")){return "G";
		}else if (redListCode.equals("#dtpl_RLKat_R#")){return "R";
		}else if (redListCode.equals("#dtpl_RLKat_V#")){return "V";
		}else if (redListCode.equals("#dtpl_RLKat_STERN#")){return "*";
		}else if (redListCode.equals("#dtpl_RLKat_STERN##dtpl_RLKat_STERN#")){return "**";
		}else if (redListCode.equals("#dtpl_RLKat_D#")){return "D";
		}else if (redListCode.equals("#dtpl_RLKat_RAUTE#")){return RAUTE;
		}else if (redListCode.equals("kN")){return "kN";
		}else if (redListCode.equals("+")){return "+";
		}else if (redListCode.equals("-")){return "-";
		}else if (redListCode.equals("=")){return "=";
		}else if (redListCode.equals("N")){return "N";
		}else if (redListCode.equals("S")){return "S";
		}else if (redListCode.equals("E")){return "E";
		}else if (redListCode.equals("D")){return "D";
		}else if (redListCode.equals("#dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER#")){
			char c = 0x2193;
			return String.valueOf(c)+String.valueOf(c)+String.valueOf(c);
		}else if (redListCode.equals("#dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER#")){
			char c = 0x2193;
			return String.valueOf(c)+String.valueOf(c);
		}else if (redListCode.equals("(#dtpl_KurzfBest_RUNTER#)")){
			char c = 0x2193;
			return "("+String.valueOf(c)+")";
		}else if (redListCode.equals("#dtpl_KurzfBest_HOCH#")){
			char c = 0x2191;
			return String.valueOf(c);
		}else if (redListCode.equals("#dtpl_Risiko_MINUS#")){return "-";
		}else if (redListCode.equals("#dtpl_VERANTW_NB#")){return "nb";
		}else if (redListCode.equals("#dtpl_TaxBez_GLEICH#")){return "=";
		}else if (redListCode.equals("#dtpl_TaxBez_KLEINER#")){return "<";
		}else if (redListCode.equals("#dtpl_TaxBez_GROESSER#")){return ">";
		}else if (redListCode.equals("#dtpl_TaxBez_UNGLEICH#")){
			char c = 0x2260;
			return String.valueOf(c);
		}else if (redListCode.equals("#dtpl_AlteRLKat_STERN#")){return "*";
		}else if (redListCode.equals("#dtpl_AlteRLKat_ZWEISTERN#")){return "**";
		}else if (redListCode.equals("#dtpl_AlteRLKat_NB#")){return "nb";
		}else if (redListCode.equals("#dtpl_AlteRLKat_KN#")){return "kN";
		}else if (redListCode.equals("#dtpl_TaxBez_UNGLEICH#")){return "-";
		}else if (StringUtils.isBlank(redListCode)){return "keine Angabe";
		}
		else if (redListCode.matches("(ex|es|ss?|mh|h|sh|\\?|nb|kn)")){
		    return redListCode;
		}else if (redListCode.equals("")){
		    return "";
		}
		else if (redListCode.matches("(<<?<?|\\(<\\)|>|!!?|\\(!\\))")){
            return redListCode;
        }else if (redListCode.matches("(0|1|2|3|G|R|V)")){
            return redListCode;
        }
		else {
			throw new UnknownCdmTypeException("Unknown Redlist Code " + redListCode);
		}
	}


	public static UUID getRedlistVocabularyUUID(String redListVocabulary) throws UnknownCdmTypeException {

		if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return uuidVocStateRLKat;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return uuidVocStateRlKatDiff;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return uuidVocStateRlAkt;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return uuidVocStateRLLang;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return uuidVocStateRLKurz;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return uuidVocStateRLRisk;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return uuidVocStateRLResp;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return uuidVocStateRLKatOld;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return uuidVocStateRLNeo;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return uuidVocStateRLKatId;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return uuidVocStateRLSpecialCases;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VORKOMMENSSTATUS)) {
		    return uuidVocGermanPresenceTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ETABLIERUNGSSTATUS)) {
		    return uuidVocGermanEstablishmentTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_BUNDESLAENDER)) {
            return uuidVocGermanFederalStates;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KOMBINIERTE_LAENDER)) {
            return uuidVocCombinedStates;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_MARINE_INVERTEBRATEN_GEBIETE)) {
            return uuidVocMarineInvertebratesAreas;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_MARINE_ALGEN_GEBIETE)) {
            return uuidVocMarineAlgaeAreas;
        } else{
			throw new UnknownCdmTypeException("Unknown Vocabulary feature, could not match: " + redListVocabulary);
		}

	}


	public static UUID getRedlistFeatureUUID(String redListFeature) throws UnknownCdmTypeException {

		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return uuidFeatureRLKat;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return uuidFeatureRlKatDiff;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return uuidFeatureRlAkt;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return uuidFeatureRLLang;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return uuidFeatureRLKurz;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return uuidFeatureRLRisk;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return uuidFeatureRLResp;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return uuidFeatureRLKatOld;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return uuidFeatureRLNeo;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return uuidFeatureRLKatId;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_TAXONOMIE)) {
            return uuidFeatureRLTaxComment;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_GEFAEHRDUNG)) {
            return uuidFeatureRLHazardComment;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return uuidFeatureRLSpecialCases;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_LETZTER_NACHWEIS)) {
            return uuidFeatureRLLastOccurrence;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_WEITERE_KOMMENTARE)) {
            return uuidFeatureRLAdditionalComment;
        } else{
			throw new UnknownCdmTypeException("Unknown feature, could not match: " + redListFeature);
		}

	}

	public static UUID getRedlistStateTermUUID(String redListStateTerm, String redListFeature) throws UnknownCdmTypeException {
		//RL Kat
		char a = 0x2666;
		if(redListStateTerm.equalsIgnoreCase("0") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKat0;
        }
		if(redListStateTerm.equalsIgnoreCase("1") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKat1;
        }
		if(redListStateTerm.equalsIgnoreCase("2") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKat2;
        }
		if(redListStateTerm.equalsIgnoreCase("3") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKat3;
        }
		if(redListStateTerm.equalsIgnoreCase("G") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatG;
        }
		if(redListStateTerm.equalsIgnoreCase("R") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatR;
        }
		if(redListStateTerm.equalsIgnoreCase("V") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatV;
        }
		if(redListStateTerm.equalsIgnoreCase("*") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatStar;
        }
		if(redListStateTerm.equalsIgnoreCase("**") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatStar2;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatD;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(a)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatDiamond;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return stateTermRlKatKN;
        }

		//RL Diff
		if(redListStateTerm.equalsIgnoreCase("+") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return stateTermRLKatDiffPlus;
        }
		if(redListStateTerm.equalsIgnoreCase("-") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return stateTermRLKatDiffMinus;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return stateTermRLKatDiffEqual;
        }

		//Rl Akt
		if(redListStateTerm.equalsIgnoreCase("ex") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktEx;
        }
		if(redListStateTerm.equalsIgnoreCase("es") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktEs;
        }
		if(redListStateTerm.equalsIgnoreCase("ss") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktSs;
        }
		if(redListStateTerm.equalsIgnoreCase("s") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktS;
        }
		if(redListStateTerm.equalsIgnoreCase("mh") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktMh;
        }
		if(redListStateTerm.equalsIgnoreCase("h") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktH;
        }
		if(redListStateTerm.equalsIgnoreCase("sh") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktSh;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktQuest;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktNb;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return stateTermRLKatAktKn;
        }

		//RL Lang
		if(redListStateTerm.equalsIgnoreCase("<<<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangLT3;
        }
		if(redListStateTerm.equalsIgnoreCase("<<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangLT2;
        }
		if(redListStateTerm.equalsIgnoreCase("<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangLT1;
        }
		if(redListStateTerm.equalsIgnoreCase("(<)") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangLT;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangEqual;
        }
		if(redListStateTerm.equalsIgnoreCase(">") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangGT;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatLangQuest;
        }

		//RL Kurz
		char c = 0x2193;
		char b = 0x2191;
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(c)+String.valueOf(c)+String.valueOf(c)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzDown3;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(c)+String.valueOf(c)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzDown2;
        }
		if(redListStateTerm.equalsIgnoreCase("("+String.valueOf(c)+")") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzDown1;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzEqual;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(b)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzUp;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return stateTermRLKatKurzQuest;
        }

		//RL Risk
		if(redListStateTerm.equalsIgnoreCase("-") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return stateTermRLKatRiskMinus;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return stateTermRLKatRiskEqual;
        }

		//RL Resp
		if(redListStateTerm.equalsIgnoreCase("!!") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return stateTermRLKatRespBang2;
        }
		if(redListStateTerm.equalsIgnoreCase("!") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return stateTermRLKatRespBang1;
        }
		if(redListStateTerm.equalsIgnoreCase("(!)") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return stateTermRLKatRespBang;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return stateTermRLKatRespQuest;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return stateTermRLKatRespNb;
        }

		//RL Kat Old
		if(redListStateTerm.equalsIgnoreCase("0") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOld0;
        }
		if(redListStateTerm.equalsIgnoreCase("1") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOld1;
        }
		if(redListStateTerm.equalsIgnoreCase("2") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOld2;
        }
		if(redListStateTerm.equalsIgnoreCase("3") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOld3;
        }
		if(redListStateTerm.equalsIgnoreCase("G") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldG;
        }
		if(redListStateTerm.equalsIgnoreCase("R") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldR;
        }
		if(redListStateTerm.equalsIgnoreCase("V") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldV;
        }
		if(redListStateTerm.equalsIgnoreCase("*") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldStar;
        }
		if(redListStateTerm.equalsIgnoreCase("**") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldStar2;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldD;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldNb;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return stateTermRLKatOldKn;
        }

		//RL Neo
		if(redListStateTerm.equalsIgnoreCase("N") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return stateTermRLKatNeo;
        }

		//RL Special
		if(redListStateTerm.equalsIgnoreCase("S") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return stateTermRLSpecialS;
        }
		if(redListStateTerm.equalsIgnoreCase("E") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return stateTermRLSpecialE;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return stateTermRLSpecialD;
        }



		//RL Empty
		if(StringUtils.isBlank(redListStateTerm) || redListStateTerm.equalsIgnoreCase("keine Angabe")) {
            return stateTermEmpty;
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + redListStateTerm +"\n In Category: " + redListFeature);
        }

	}


	/**
	 * retrieve german federal States
	 *
	 * @param strGermanState
	 * @return
	 * @throws UnknownCdmTypeException
	 */
	public static UUID getAreaUUID(String strGermanState) throws UnknownCdmTypeException {

		if(strGermanState.equalsIgnoreCase("Deutschland")) {
            return uuidAreaDeutschland;
        }else if(strGermanState.equalsIgnoreCase("Baden-Württemberg") || strGermanState.equalsIgnoreCase("BW")) {
            return uuidAreaBW;
		}else if(strGermanState.equalsIgnoreCase("Bayern") || strGermanState.equalsIgnoreCase("BY")) {
            return uuidAreaBY;
		}else if(strGermanState.equalsIgnoreCase("Berlin") || strGermanState.equalsIgnoreCase("BE")) {
            return uuidAreaBerlin;
		}else if(strGermanState.equalsIgnoreCase("Bremen") || strGermanState.equalsIgnoreCase("HB")) {
            return uuidAreaBremen;
		}else if(strGermanState.equalsIgnoreCase("Brandenburg") || strGermanState.equalsIgnoreCase("BB")) {
            return uuidAreaBB;
		}else if(strGermanState.equalsIgnoreCase("Hamburg") || strGermanState.equalsIgnoreCase("HH")) {
            return uuidAreaHamburg;
		}else if(strGermanState.equalsIgnoreCase("Hessen") || strGermanState.equalsIgnoreCase("HE")) {
            return uuidAreaHessen;
		}else if(strGermanState.equalsIgnoreCase("Mecklenburg-Vorpommern") || strGermanState.equalsIgnoreCase("MV")) {
            return uuidAreaMV;
		}else if(strGermanState.equalsIgnoreCase("Niedersachsen") || strGermanState.equalsIgnoreCase("NI")) {
            return uuidAreaNiedersachsen;
        }else if(strGermanState.equalsIgnoreCase("Nordrhein-Westfalen")||strGermanState.equalsIgnoreCase("NW")) {
            return uuidAreaNRW;
        }else if(strGermanState.equalsIgnoreCase("Rheinland-Pfalz")||strGermanState.equalsIgnoreCase("RP")) {
            return uuidAreaRP;
        }else if(strGermanState.equalsIgnoreCase("Saarland")||strGermanState.equalsIgnoreCase("SL")) {
            return uuidAreaSaarland;
        }else if(strGermanState.equalsIgnoreCase("Sachsen")||strGermanState.equalsIgnoreCase("SN")) {
            return uuidAreaSachsen;
        }else if(strGermanState.equalsIgnoreCase("Sachsen-Anhalt")|| strGermanState.equalsIgnoreCase("ST")) {
            return uuidAreaSachsenAnhalt;
        }else if(strGermanState.equalsIgnoreCase("Schleswig-Holstein")||strGermanState.equalsIgnoreCase("SH")) {
            return uuidAreaSH;
        }else if(strGermanState.equalsIgnoreCase("Thüringen")||strGermanState.equalsIgnoreCase("TH")) {
            return uuidAreaTH;
        //Combined States
        }else if(strGermanState.equalsIgnoreCase("Brandenburg und Berlin")||strGermanState.equalsIgnoreCase("BB+BE")) {
            return uuidAreaBB_BE;
        }else if(strGermanState.equalsIgnoreCase("Schleswig-Holstein und Hamburg")||strGermanState.equalsIgnoreCase("SH+HH")) {
            return uuidAreaSH_HH;
        //Marine Algen
        }else if(strGermanState.equalsIgnoreCase("Helgoland2")||strGermanState.equalsIgnoreCase("HGL")) {
            return uuidAreaHGL;
        }else if(strGermanState.equalsIgnoreCase("Niedersächsisches Wattenmeer")||strGermanState.equalsIgnoreCase("NIW")) {
            return uuidAreaNIW;
        }else if(strGermanState.equalsIgnoreCase("Schleswig-Holsteinisches Wattenmeer")||strGermanState.equalsIgnoreCase("SHW")) {
            return uuidAreaSHW;
        }else if(strGermanState.equalsIgnoreCase("Schleswig-Holsteinische Ostsee")||strGermanState.equalsIgnoreCase("SHO")) {
            return uuidAreaSHO;
        }else if(strGermanState.equalsIgnoreCase("Mecklenburg-Vorpommerische Ostsee")||strGermanState.equalsIgnoreCase("MVO")) {
            return uuidAreaMVO;
        //Marine Invertebraten
        }else if(strGermanState.equalsIgnoreCase("Ästuarien und Watt Nordsee")||strGermanState.equalsIgnoreCase("ÄWN")) {
            return uuidAreaAEWN;
        }else if(strGermanState.equalsIgnoreCase("Sublitoral Nordsee")||strGermanState.equalsIgnoreCase("SuN")) {
            return uuidAreaSuN;
        }else if(strGermanState.equalsIgnoreCase("Helgoland")||strGermanState.equalsIgnoreCase("Hel")) {
            return uuidAreaHel;
        }else if(strGermanState.equalsIgnoreCase("Doggerbank")||strGermanState.equalsIgnoreCase("Dog")) {
            return uuidAreaDog;
        }else if(strGermanState.equalsIgnoreCase("Ostsee")||strGermanState.equalsIgnoreCase("Ost")) {
            return uuidAreaOst;

        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strGermanState);
        }
	}

	public static UUID getGermanAbsenceTermUUID(String term) throws UnknownCdmTypeException {
	    if(term.equalsIgnoreCase(ABWESEND)) {return uuidStatusAbwesend;}
	    else if(term.equalsIgnoreCase(ABWESEND_AUSGESTORBEN)) {return uuidStatusAusgestorben;}
	    else if(term.equalsIgnoreCase(ABWESEND_FEHLEINGABE)) {return uuidStatusFehleingabe;}
	    else if(term.equalsIgnoreCase(ABWESEND_SEIT1980)) {return uuidStatusKeinNachweisNach1980;}
	    else if(term.equalsIgnoreCase(VORHANDEN)) {return uuidStatusVorkommend;}
	    else if(term.equalsIgnoreCase(VORHANDEN_EINBUERGERUNG)) {return uuidStatusEinbuergerung;}
	    else if(term.equalsIgnoreCase(VORHANDEN_ETABLIERT)) {return uuidStatusEtabliert;}
	    else if(term.equalsIgnoreCase(VORHANDEN_KULTIVIERT_DOMESTIZIERT)) {return uuidStatusKultiviert;}
	    else if(term.equalsIgnoreCase(VORHANDEN_UNBESTAENDIG)) {return uuidStatusUnbestaendig;}
	    else if(term.equalsIgnoreCase(VORHANDEN_VORKOMMEN_UNSICHER)) {return uuidStatusVorkommenUnsicher;}
	    else if(term.equalsIgnoreCase(VORHANDEN_UNSICHER)) {return uuidStatusUnsicher;}
	    else if(term.equalsIgnoreCase(ABWESEND_KEIN_NACHWEIS)) {return uuidStatusKeinNachweis;}
	    else if(term.equalsIgnoreCase(ABWESEND_ABGELEHNT)) {return uuidStatusAbgelehnt;
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + term);
        }
	}
    public static UUID getGermanEstablishmentTermUUID(String strGermanTerm) throws UnknownCdmTypeException {
        if(strGermanTerm.equalsIgnoreCase("Archaeophyt")) {return uuidEstablishArchaeophyt;}
        else if(strGermanTerm.equalsIgnoreCase("Indigen")) {return uuidEstablishIndigen;}
        else if(strGermanTerm.equalsIgnoreCase("Kulturpflanze / domestiziertes Tier")) {return uuidEstablishKulturpflanzeDomestiziert;}
        else if(strGermanTerm.equalsIgnoreCase("Neophyt")) {return uuidEstablishNeophyt;}
        else if(strGermanTerm.equalsIgnoreCase("Kultuflüchtling")) {return uuidEstablishKulturflucht;
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strGermanTerm);
        }
    }


    /**
     * Matches a redlist status string to a presence status uuid
     * @param strDistStat
     * @return the presence status uuid
     * @throws UnknownCdmTypeException
     */
    public static UUID matchDistributionValue(String strDistStat) throws UnknownCdmTypeException {

        if(strDistStat.equalsIgnoreCase("*")){return uuidStatusVorkommend;}       //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("0")){return uuidStatusVorkommend;}  //uuidStatusAusgestorben
        else if(strDistStat.equalsIgnoreCase("1")){return uuidStatusVorkommend;}     //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("2")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("3")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("G")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("D")){return null;}  //uuidStatusVorkommenUnsicher
        else if(strDistStat.equalsIgnoreCase("R")){return uuidStatusVorkommend;}   //uuidStatusVorkommenUnsicher
        else if(strDistStat.equalsIgnoreCase("N")){return uuidStatusVorkommend;}
        else if(strDistStat.equalsIgnoreCase("V")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("nb")){return uuidStatusVorkommend;}   //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("*")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("")){return uuidStatusVorkommend;}     //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase(" ")){return uuidStatusVorkommend;}    //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("-")){return uuidStatusKeinNachweis;}  //uuidStatusVorkommenUnsicher
        else if(strDistStat.equalsIgnoreCase("+")){return uuidStatusVorkommenUnsicher;}
        else if(strDistStat.equalsIgnoreCase("°")){return uuidStatusVorkommenUnsicher;}
        else if(strDistStat.equalsIgnoreCase("G/D")){return uuidStatusVorkommend;}
        else if(strDistStat.equalsIgnoreCase("R/1")){return uuidStatusVorkommend;}
        else if(strDistStat.equalsIgnoreCase("?")){return uuidStatusUnsicher;}   //uuidStatusVorkommenUnsicher
        else if(strDistStat.equalsIgnoreCase(RAUTE)){return null;}   //?????????????  (Nicht bewertet)

        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_STERN_DP#")){return uuidStatusVorkommend;}  //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_STERN#")){return uuidStatusVorkommend;}  //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_STERN##dtpl_SynopseBL_STERN#")){return   uuidStatusVorkommend;}   //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_NB#")){return uuidStatusVorkommend;}   //uuidStatusEtabliert
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_X_KLAMMER#")){return uuidStatusUnsicher;}   //uuidStatusVorkommenUnsicher
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_X#")){return uuidStatusVorkommend;}
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_STRICH#")){return uuidStatusKeinNachweis;}
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_LEER#")){return uuidStatusKeinNachweis;}
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_KREUZ#")){return uuidStatusVorkommend;}    //uuidStatusAusgestorben
        else if(strDistStat.equalsIgnoreCase("#dtpl_SynopseBL_KREIS#")){return uuidStatusVorkommend;}   //uuidStatusKeinNachweisNach1980

        else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strDistStat);
        }
    }
}
