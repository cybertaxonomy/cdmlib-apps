/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IRelationshipType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 01.03.2010
 */
public final class ErmsTransformer extends InputTransformerBase {

    private static final long serialVersionUID = 1777919792691129468L;
    private static Logger logger = LogManager.getLogger();

	public static final int SOURCE_USE_ORIGINAL_DESCRIPTION = 1;
	public static final int SOURCE_USE_BASIS_OF_RECORD = 2;
	public static final int SOURCE_USE_ADDITIONAL_SOURCE = 3;
	public static final int SOURCE_USE_SOURCE_OF_SYNONYMY = 4;
	public static final int SOURCE_USE_REDESCRIPTION = 5;
	public static final int SOURCE_USE_NEW_COMBINATION_REFERENCE = 6;
	public static final int SOURCE_USE_STATUS_SOURCE = 7;
	public static final int SOURCE_USE_EMENDATION = 8;

	public static final String SOURCE_USE_STR_BASIS_OF_RECORD = "basis of record";

	//nomenclatural status tpye uuids
	public static final UUID uuidNomStatusSpeciesInquirenda = UUID.fromString("7790a4e2-d071-499c-8f25-a5ae34fd2dfb");
	public static final UUID uuidNomStatusAlternateRepresentation = UUID.fromString("bb746f99-5c46-4b6b-963a-8aff1b04f22b");

	//rank uuids
	public static final UUID uuidRankSuperdomain = UUID.fromString("66d4d773-4946-4e02-b758-8903563eaa26");
    public static final UUID uuidRankSubterclass = UUID.fromString("39257363-913b-4b3a-a536-b415360dfc8c");
    public static final UUID uuidRankParvorder = UUID.fromString("d26b8cae-96f9-4aee-81f2-98e36f1db7c3");
    public static final UUID uuidRankParvphylum = UUID.fromString("1cdfac29-3abf-4435-bb42-a80de50c3444");
    public static final UUID uuidRankGigaclass = UUID.fromString("18ca03ef-d219-423b-b946-6cd61507e871");
    public static final UUID uuidRankMegaclass = UUID.fromString("1201fd28-c6df-45ee-99c1-ae3729f6093b");
    public static final UUID uuidRankEpifamily = UUID.fromString("609af382-9fb7-4a57-927a-696a3149177d");
    public static final UUID uuidRankMutatio = UUID.fromString("a63de3c4-8f5c-40a6-8f11-f4e62602bce9");

	//area uuids
    public static final UUID uuidVocErmsAreas = UUID.fromString("7e6daf5e-f536-4c0c-8967-1400a45f7b4a");
    public static final UUID uuidAdriaticSea = UUID.fromString("da376165-f970-4f0c-99db-773686d66591");
    public static final UUID uuidAegeanSea = UUID.fromString("65d6c443-225f-4ac0-9c86-da51502b46df");
    public static final UUID uuidArchipelagoSea = UUID.fromString("d9ea9d63-ec4d-4b01-967d-13f28b09a715");
    public static final UUID uuidBalticProper = UUID.fromString("12ddfcad-bf8f-43d8-a772-15ae69d37b20");
//    public static final UUID uuidBalearSea = UUID.fromString("478f30f0-01b1-4772-9d01-3a0a571f41c3");
    public static final UUID uuidBalticSea = UUID.fromString("0031cda2-4b27-49de-afa3-fdec75ee5060");
    public static final UUID uuidBarentszSea = UUID.fromString("b6fd9a4d-0ad0-4481-a0b4-5dd71c8fda8b");
    public static final UUID uuidBelgianExclusiveEconomicZone = UUID.fromString("02138b0f-cee1-4c56-ae12-72a5b36839af");
    public static final UUID uuidBaeltSea = UUID.fromString("780f4144-f157-45e8-ae42-cacb3ec369ba");
    public static final UUID uuidBayOfBiscay = UUID.fromString("36ffc01e-85a2-4f71-91fd-012d0b1eeff5");
    public static final UUID uuidBlackSea = UUID.fromString("1f110909-7462-4ee8-a7ff-9f976701dd1d");
    public static final UUID uuidBothnianSea = UUID.fromString("926f7fa3-b0a4-4763-85eb-4c3804a72333");
    public static final UUID uuidBulgarianExclusiveEconomicZone = UUID.fromString("13e5aa21-3971-4d06-bc34-ed75a31c2f66");
    public static final UUID uuidCaspianSea = UUID.fromString("0d3c3850-0cec-48d0-ac0d-9cbcc2c60552");
    public static final UUID uuidCroatianExclusiveEconomicZone = UUID.fromString("028b045a-b1bd-4a72-a4c2-a3d0473b8257");
    public static final UUID uuidDanishExclusiveEconomicZone = UUID.fromString("53d5a8bd-804b-4cbb-b5ad-f47ff6433db0");
    public static final UUID uuidDutchExclusiveEconomicZone = UUID.fromString("a1bd019e-e2af-41c8-a5e4-c7245b575759");
    public static final UUID uuidEgyptianExclusiveEconomicZone = UUID.fromString("e542bcfd-0ff1-49ac-a6ae-c0b3db39e560");
    public static final UUID uuidEnglishChannel = UUID.fromString("3ed96112-bb4a-47df-b489-2c198d6f0fd2");
    public static final UUID uuidEstonianExclusiveEconomicZone = UUID.fromString("ed17f07b-357f-4b4a-9653-3a564fdd32e5");
	public static final UUID uuidEuropeanMarineWaters = UUID.fromString("47389e42-3b3c-4873-bded-ac030db86462");
	public static final UUID uuidFrenchExclusiveEconomicZone = UUID.fromString("9f6a61c0-c329-4a61-a47a-f5f383737c36");
	public static final UUID uuidGermanExclusiveEconomicZone = UUID.fromString("a6dbea03-090f-4f5f-bf5e-27a00ab4cc1d");
	public static final UUID uuidIcelandicExclusiveEconomicZone = UUID.fromString("a121a8fb-6287-4661-9228-0816affdf3f5");
	public static final UUID uuidIrishExclusiveeconomicZone = UUID.fromString("c8fe2626-53d2-4eaa-962b-99662470b96e");
	public static final UUID uuidIrishSea = UUID.fromString("9e972ad5-b153-419e-ab7e-935b93ff881b");
	public static final UUID uuidItalianExclusiveEconomicZone = UUID.fromString("10557c6f-a33f-443a-ad8b-cd31c105bddd");
	public static final UUID uuidLebaneseExclusiveEconomicZone = UUID.fromString("d9f7dc8b-9041-4206-bf5f-5226c42a5978");
//	public static final UUID uuidMadeiranExclusiveEconomicZone = UUID.fromString("c00f442a-4c08-4452-b979-825fa3ff97b2");
//	public static final UUID uuidMarmaraSea = UUID.fromString("3db5d470-3265-4187-ba5a-01ecfb94ce6e");
	public static final UUID uuidMediterraneanSea = UUID.fromString("bde8a624-23c4-4ac3-b381-11287f5d656a");
	public static final UUID uuidMoroccanExclusiveEconomicZone = UUID.fromString("e62e5cc2-922f-4807-abd6-1b4bffbced49");
	public static final UUID uuidNorthBalticproper = UUID.fromString("183ec305-1e9e-4cb1-93cc-703bd64de28f");
	public static final UUID uuidNorthSea = UUID.fromString("d5ea2d46-ed97-4996-8702-0619231626b6");
	public static final UUID uuidNorwegianSea = UUID.fromString("c6c44372-a963-41b2-8c12-a0b46425c523");
	public static final UUID uuidNorwegianExclusiveEconomicZone = UUID.fromString("bd317f3e-9719-4943-ae3e-19ff0c9761be");
	public static final UUID uuidPortugueseExclusiveEconomicZone = UUID.fromString("642336f3-41cb-4546-9a1c-ffeccbad2ef5");
	public static final UUID uuidSeaofAzov = UUID.fromString("5b02cb7e-8a83-446c-af47-936a2ea31a8a");
	public static final UUID uuidSkagerrak = UUID.fromString("5f63ece2-d112-4b39-80a0-bffb6c78654c");
	public static final UUID uuidSouthBalticproper = UUID.fromString("1c2a672d-4948-455d-9877-42a8da1ff1d0");
	public static final UUID uuidSpanishExclusiveEconomicZone = UUID.fromString("68c2823a-2173-4c31-89e8-bc1439abf448");
	public static final UUID uuidSpanishExclusiveEconomicZoneMediterraneanpart = UUID.fromString("94ccf304-9687-41b6-a14b-019509adb723");
	public static final UUID uuidSwedishExclusiveEconomicZone = UUID.fromString("94b0e605-d241-44e1-a301-d8911c34fdef");
//	public static final UUID uuidTirrenoSea = UUID.fromString("6e4f8a9d-ca6e-4b23-9211-446fac35a052");
	public static final UUID uuidTyrrhenianSea = UUID.fromString("26c39604-b7fd-425a-93a5-958774261d04");
	public static final UUID uuidTunisianExclusiveEconomicZone = UUID.fromString("b5972b59-6a36-45ea-88f7-0c520c99b99d");
	public static final UUID uuidTurkishExclusiveEconomicZone = UUID.fromString("3d552e73-2bf5-4f36-8a91-94fbead970e5");
	public static final UUID uuidUkrainianExclusiveEconomicZone = UUID.fromString("b7335968-e34f-412c-91a5-5dc0b73310e7");
//	public static final UUID uuidUnitedKingdomExclusiveEconomicZone = UUID.fromString("18ab29c0-3104-4102-ada8-6711fcdbdbb8");
	public static final UUID uuidWaddenSea = UUID.fromString("ae0c4555-8e19-479d-8a4f-e1b62939c09b");
	public static final UUID uuidWhiteSea = UUID.fromString("bf14bfb6-8925-4696-911c-56d3e90d4491");


//	public static final UUID uuidAfghanistan = UUID.fromString("9ecbc81b-9150-45b5-953d-1051cd519dd0");
	public static final UUID uuidAlboranSea = UUID.fromString("a4f1ef1e-0bda-4be0-95f9-59ed4f536fb6");
	public static final UUID uuidAlgeria = UUID.fromString("d254b5bb-e24c-4f79-88dd-80a75cd935db");
//	public static final UUID uuidAngola = UUID.fromString("fdb74fd7-e6a1-48ff-8721-f1db6e0d93ee");
	public static final UUID uuidArcticOcean = UUID.fromString("4e68ddce-4472-4ae0-b6c3-406f39bf86ec");
//	public static final UUID uuidAustralianExclusiveEconomicZone = UUID.fromString("de8f70b9-3cac-48d5-b456-5cd489df5c26");
	public static final UUID uuidAzoresExclusiveEconomicZone = UUID.fromString("e0297e2c-d28a-46aa-8b30-694148b0640d");
//	public static final UUID uuidBahamas = UUID.fromString("3e7eb2e5-975b-433c-9dc0-106ba16fd6e4");
	public static final UUID uuidBalearicSea = UUID.fromString("f2eb56e0-0eac-4def-8143-c87f8bd04613");
	public static final UUID uuidBelgium = UUID.fromString("4480742d-d760-4ec8-8ee6-24b889143ec1");
//	public static final UUID uuidBelize = UUID.fromString("d70dcf11-143c-48ce-bb8d-436600e3eb64");
//	public static final UUID uuidBrazil = UUID.fromString("597c66d5-528f-4eaa-aa71-c4ffc7d3d226");
	public static final UUID uuidBulgaria = UUID.fromString("27dc2a12-943a-4e9f-88ac-70896f857bc5");
//	public static final UUID uuidCanada = UUID.fromString("65dd3c29-3cd0-4233-bd0e-b112bb27e519");
//	public static final UUID uuidCaribbeanSea = UUID.fromString("a1f1d511-2e62-4a73-a399-90bfffd8af56");
    //TODO probably to delete
	public static final UUID uuidCapeVerde = UUID.fromString("4704c7ff-cd3d-4460-81d6-a925b81657e1");
	public static final UUID uuidCapeVerdeanExclusiveEconomicZone = UUID.fromString("1c951957-630a-4467-87be-ac9daba184a4");
//	public static final UUID uuidChile = UUID.fromString("a191ee04-2a58-47fd-aa12-566c9f16ee02");
//	public static final UUID uuidColombia = UUID.fromString("b1719057-9f4a-4042-8f85-7097afca2ba8");
//	public static final UUID uuidCostaRica = UUID.fromString("acbe3731-8568-400c-b666-dfdbc8f5cc9f");
	public static final UUID uuidCroatia = UUID.fromString("f9d6a5ac-590e-4618-8fe1-a047fd9e1b32");
//	public static final UUID uuidCuba = UUID.fromString("17ae1220-7cc2-4dc5-bdd5-b852889b7dda");
	public static final UUID uuidDenmark = UUID.fromString("3338dd30-98cd-4469-96b1-d8b79ae42ed5");
	public static final UUID uuidEgypt = UUID.fromString("830d2178-722e-4668-922b-380501b031dd");
	public static final UUID uuidEstonia = UUID.fromString("7b2953cc-7ee7-45cf-b0e6-db229b0d88a3");
	public static final UUID uuidFaeroeExclusiveEconomicZone = UUID.fromString("b9a80017-0177-4d1e-83e2-d93d2e764b92");
	public static final UUID uuidFrance = UUID.fromString("b7876595-230d-4ac8-bbcf-0a271221aeb0");
//	public static final UUID uuidGhana = UUID.fromString("b550ba30-41b8-485a-a1de-ba79b4b6e152");
	public static final UUID uuidGreece = UUID.fromString("c004a689-048b-4541-9371-063bca1ab20e");
	public static final UUID uuidGermany = UUID.fromString("55d23f99-c517-49ef-936b-3815c10b6303");
    public static final UUID uuidGreekExclusiveEconomicZone = UUID.fromString("6e587a2b-d681-46d3-bede-30411b0707a9");
	public static final UUID uuidGulfOfBothnia = UUID.fromString("e0126fd6-a163-4483-92e4-abe1a96ed025");
	public static final UUID uuidGulfOfFinland = UUID.fromString("d312ff30-18d0-4f8a-9c80-3a06772cefbb");
//	public static final UUID uuidGulfOfGuinea = UUID.fromString("ea6f3983-64ad-46c4-832a-51bed6507d95");
//	public static final UUID uuidGulfOfMexico = UUID.fromString("9693deb4-82ed-40fa-822c-b5016d7fccf0");
	public static final UUID uuidGulfOfRiga = UUID.fromString("c16dd324-850a-4dfc-8f3b-6718c8614cae");
	public static final UUID uuidIceland = UUID.fromString("c3075a02-2aad-4745-9d40-45c498599b72");
	public static final UUID uuidIonianSea = UUID.fromString("ff4fa153-5174-48fb-9eaa-0b391ba96153");
	public static final UUID uuidIreland = UUID.fromString("f8e878b1-517d-420d-9445-cf92e7c8f986");
	public static final UUID uuidIsraeliExclusiveEconomicZone = UUID.fromString("2eb96b7c-4634-44be-a68b-1ea36593ba4a");
	public static final UUID uuidItaly = UUID.fromString("a8b1ade2-a70c-4902-b375-670d779a5078");
//	public static final UUID uuidJamaica = UUID.fromString("3a0b6181-3c91-44b6-8844-a61242adddd2");
	public static final UUID uuidKattegat = UUID.fromString("c96bcfa2-20fc-4a23-9b1b-0da899b389c2");
	public static final UUID uuidLevantineSea = UUID.fromString("04fd2746-5a6e-4392-9881-0e75a2f060e9");
	public static final UUID uuidLigurianSea = UUID.fromString("9dcbf18e-3483-4ba8-84a4-c3c7ca2be297");
	public static final UUID uuidMalteseExclusiveEconomicZone = UUID.fromString("450a1f9b-6d63-4c9f-9488-b800547c5c21");
	public static final UUID uuidMauritanianExclusiveEconomicZone = UUID.fromString("7e9a12c3-31ee-4e09-bec0-dce173d1096e");
	public static final UUID uuidMediterraneanSea_EasternBasin = UUID.fromString("a67dfa4d-4a96-403b-a64c-1e294826c2ed");
	public static final UUID uuidMediterraneanSea_WesternBasin = UUID.fromString("b3e81fa7-ee7a-45d2-965c-88020f9ee1ea");
//	public static final UUID uuidMexico = UUID.fromString("a9be5a5c-99ec-4627-9892-104a0c175118");
//	public static final UUID uuidMongolia = UUID.fromString("0385e6b1-4796-4e9f-8656-b6092c97c58f");
	public static final UUID uuidMorocco = UUID.fromString("4948e1ef-f61e-462f-9920-ab44c71ca2ae");
	public static final UUID uuidNetherlands = UUID.fromString("8d4152fb-bf29-4a24-a772-62ccb4a8c02f");
//	public static final UUID uuidNewZealand = UUID.fromString("f3ff8497-00e0-4d45-9d14-eb80fea0fee0");
//	public static final UUID uuidNewZealandExclusiveEconomicZone = UUID.fromString("b10d2637-4b44-44d5-8742-fe8576859926");
	public static final UUID uuidNorthAtlanticOcean = UUID.fromString("628354d1-1ded-4f3d-887e-4f90d43e0f14");
	public static final UUID uuidNorway = UUID.fromString("d76b5b8b-2e1e-4241-b4f1-eb3acdfc12fd");
//	public static final UUID uuidPanama = UUID.fromString("796ed240-94bd-4558-bb99-7b1facdbc435");
//	public static final UUID uuidPanamanianExclusiveEconomicZone = UUID.fromString("49e40b33-30d0-4905-adea-b3ce05a40450");
	public static final UUID uuidPolishExclusiveEconomicZone = UUID.fromString("dc7f8339-528b-49ec-a8d9-c2be0441e933");
	public static final UUID uuidPortugal = UUID.fromString("4a04d8fd-3f2f-43ec-a5c4-684c8028e6c6");
	public static final UUID uuidPortugueseExclusiveEconomicZone_Azores = UUID.fromString("33672f3d-dc4e-43ca-8b46-1e7d292c2fae");
	public static final UUID uuidPortugueseExclusiveEconomicZone_Madeira = UUID.fromString("8308b767-09be-459a-9281-67b15fb59380");
	public static final UUID uuidRedSea = UUID.fromString("6470eda4-738a-411d-9af7-0fd791dc9195");
	public static final UUID uuidRussianExclusiveEconomicZone = UUID.fromString("cb7b047e-562d-4a1e-be08-872f0679dd1f");
	public static final UUID uuidSeaOfMarmara = UUID.fromString("4b9dce18-0e4a-46e1-b8a0-634284f3fa18");
//	public static final UUID uuidSenegaleseExclusiveEconomicZone = UUID.fromString("05a4a971-ed53-4133-8999-8c96ddd20e22");
//	public static final UUID uuidSingapore = UUID.fromString("f68a178f-093f-427b-9431-ed2564516e90");
	public static final UUID uuidSlovenianExclusiveEconomicZone = UUID.fromString("b5b06645-da20-4fc3-b964-09e4bbb63337");
//	public static final UUID uuidSouthAfrica = UUID.fromString("0004d255-7dbe-47e2-9acc-6086a5ac6719");
//	public static final UUID uuidSouthAfricanExclusiveEconomicZone = UUID.fromString("cba99c12-040b-4e17-9b20-2350ec4201c2");
//	public static final UUID uuidSouthChinaSea = UUID.fromString("b91b1cae-0c9d-4d73-b3c7-98312664f4b4");
	public static final UUID uuidSpain = UUID.fromString("36b5a55c-6fd8-4fab-bd01-d776fb1f357e");
	public static final UUID uuidSpanishExclusiveEconomicZone_CanaryIslands = UUID.fromString("30404044-c1e5-4757-92fd-0b1851c7d801");
//	public static final UUID uuidSriLankanExclusiveEconomicZone = UUID.fromString("be08163d-9e4c-44ad-b1f6-0592497724e1");
	public static final UUID uuidStraitOfGibraltar = UUID.fromString("0cdd58d4-cf46-4ea2-b841-bcfcb1ee2195");
	public static final UUID uuidSweden = UUID.fromString("dade7b65-d408-4017-a16c-f5ea7aeb3783");
	public static final UUID uuidTunisia = UUID.fromString("e7caa4b3-cf79-4ea0-8468-2438c2a201c6");
	public static final UUID uuidTuerkiye = UUID.fromString("0fbbf26a-7743-44d3-a7e4-2783016a37ed");
	public static final UUID uuidUnitedKingdom = UUID.fromString("a066c48c-6821-4acb-a454-3e1564e17cfe");
//	public static final UUID uuidUnitedStates = UUID.fromString("44d0c16c-b9d0-4db2-8776-34d230222caa");
//    public static final UUID uuidUnitedStatesExclusiveEconomicZone_Alaska = UUID.fromString("2d7d93fe-68ac-43d1-9d3a-92ccb8000ae6");
//	public static final UUID uuidVenezuela = UUID.fromString("c19956af-02e6-4868-97ef-135db405cc75");

	public static final UUID uuidCelticSea = UUID.fromString("6741c141-3d98-4fb8-84e5-9f79624d336b");
	public static final UUID uuidTurkishAegeanSea = UUID.fromString("60b58206-1b60-4535-8baf-12df52caf97e");
	public static final UUID uuidRomanianExclusiveEconomicZone = UUID.fromString("5e82d626-d97e-4a42-acbe-0bb6db82a426");
	public static final UUID uuidBritishExclusiveEconomicZone = UUID.fromString("8a6089d3-0f72-4cc1-b0c5-07aedecd622f");
	public static final UUID uuidAlgerianExclusiveEconomicZone = UUID.fromString("f4816949-4d21-4b1e-8eb7-788f88945c29");
//	public static final UUID uuidSierraLeoneanExclusiveEconomicZone = UUID.fromString("cfd9e51b-d65d-4161-8f81-49e3a6182b35");
//	public static final UUID uuidSaoTomeanExclusiveEconomicZone = UUID.fromString("9a8f000a-f6ef-4992-8970-239df692ab95");
//	public static final UUID uuidBritishExclusiveEconomicZoneBermuda = UUID.fromString("ee43054e-e799-4b17-9d5f-d0037592b5a6");
//	public static final UUID uuidBahamianExclusiveEconomicZone = UUID.fromString("dd8a86c0-05a2-4791-a92c-5f495d2a41f8");
//	public static final UUID uuidCubanExclusiveEconomicZone = UUID.fromString("ac780dd5-7799-4127-a14c-fa1e294d07f0");
//	public static final UUID uuidBritishExclusiveEconomicZoneCaymanIslands = UUID.fromString("21b8ec19-77df-4c13-b0c0-6823bcce850d");
//	public static final UUID uuidUnitedStatesExclusiveEconomicZonePuertoRico = UUID.fromString("7ed1c2dd-aa6e-45c0-b0e1-705e440c70f6");
//	public static final UUID uuidBritishExclusiveEconomicZoneBritishVirginIslands = UUID.fromString("52dbed7b-a18c-4918-a078-2be4e7e2ef7a");
//	public static final UUID uuidVenezuelanExclusiveEconomicZone = UUID.fromString("539ebe05-a133-49f0-9c95-b26bbfe6b7ad");
//	public static final UUID uuidFrenchExclusiveEconomicZoneGuadeloupe = UUID.fromString("316ab71d-2ed9-4f3d-9a72-b6252e146c97");
//	public static final UUID uuidBelizeanExclusiveEconomicZone = UUID.fromString("f0bea01c-e04d-44d3-9234-621bf8dd9cf9");
//	public static final UUID uuidJamaicanExclusiveEconomicZone = UUID.fromString("e5b9290e-e920-41b0-931c-d9ae180caf10");
//	public static final UUID uuidAngolanExclusiveEconomicZone = UUID.fromString("cc831842-e3fe-41c7-be39-343b9079667c");
//	public static final UUID uuidSingaporeanExclusiveEconomicZone = UUID.fromString("a1771934-2819-4fd7-94ba-da24368002fb");
//	public static final UUID uuidColombianExclusiveEconomicZone = UUID.fromString(  "95ba028f-2057-4fc8-a2d0-6fe2538c7433");
//    public static final UUID uuidGhanaianExclusiveEconomicZone = UUID.fromString("225a2dfd-4da3-499b-af8b-9727ddd370b9");
//    public static final UUID uuidCostaRicanExclusiveEconomicZone = UUID.fromString("e98bee75-fba0-4e76-8c1b-088159497982");
    public static final UUID uuidDanishExclusiveEconomicZoneFaeroe = UUID.fromString("c32295f0-fad5-49dc-928e-f665da3bccdb");
    public static final UUID uuidNorwegianExclusiveEconomicZoneJanMayen = UUID.fromString("3173ecf0-3147-48d8-844a-e70a81147da7");
//    public static final UUID uuidDutchExclusiveEconomicZoneCuraçao = UUID.fromString("96c66a5e-b5c9-44d6-863a-eed5af37e857");
//    public static final UUID uuidDutchExclusiveEconomicZoneAruba = UUID.fromString("92803bd0-b6ea-4f17-b6e9-d893f9e0010a");
//    public static final UUID uuidDutchExclusiveEconomicZoneBonaire = UUID.fromString("e74b2f09-aa90-4d2b-9b8b-2f4c603d34c0");
    public static final UUID uuidCyprioteExclusiveEconomicZone = UUID.fromString("019edf16-3baf-4952-bc3e-3ea6f40305aa");
    public static final UUID uuidNorwegianExclusiveEconomicZoneSvalbard = UUID.fromString("dc91cbef-7020-411b-b5e6-4301d1fc45e5");
    public static final UUID uuidFrenchExclusiveEconomicZoneMartinique = UUID.fromString("f8b341dc-64f0-4881-8656-e1138580b75b");

    //2025
    public static final UUID  uuidLibyanExclusiveEconomicZone = UUID.fromString("4c5c0c6a-0d10-45de-a224-966df62e49c3");
    public static final UUID  uuidSyrianExclusiveEconomicZone = UUID.fromString("13182437-d5ab-471d-98e9-6a75f112b51e");
    public static final UUID  uuidMontenegrinExclusiveEconomicZone = UUID.fromString("475edbb5-6661-4a8e-9ea7-9a4d2c81c4f5");
    public static final UUID  uuidJordanianExclusiveEconomicZone = UUID.fromString("0fb9a740-076e-47fc-9fbd-9d1a711130cd");

    //    public static final UUID uuidIndianOcean = UUID.fromString("ac7f5e7a-786a-4a7a-9c51-f2e61b249961");
//    public static final UUID  uuidGulfOfSaintLawrence = UUID.fromString("1ef1a806-f0a6-45da-8af7-1e42bd06c7ee");
//    public static final UUID  uuidAustralia = UUID.fromString("504210f8-a494-42bc-a1f7-5d8430febce3");
//    public static final UUID  uuidFrenchExclusiveEconomicZoneNewCaledonia = UUID.fromString("de59b568-784d-45d1-a659-2c9c62415513");
//    public static final UUID  uuidPhilippineExclusiveEconomicZone = UUID.fromString("7a6c3324-3b7c-4d23-95c1-feb85047277d");
//    public static final UUID  uuidMozambicanExclusiveEconomicZone = UUID.fromString("5bf91193-949a-4718-9e97-6c75e4421845");
//    public static final UUID  uuidBritishExclusiveEconomicZoneSaintHelena = UUID.fromString("d71b1f97-f420-4550-af8b-1b3915b7e6ac");
//    public static final UUID  uuidDominicanRepublicExclusiveEconomicZone = UUID.fromString("e397f22f-bdc9-4c9d-aa6a-8191cb58c2a9");
//    public static final UUID  uuidUnitedStatesExclusiveEconomicZoneHawaii = UUID.fromString("9e23731f-b46b-4142-950d-48301f654c9b");
//    public static final UUID  uuidBrazilianExclusiveEconomicZone = UUID.fromString("85866236-32b8-49d5-87b2-7061473e2057");
//    public static final UUID  uuidGuineanExclusiveEconomicZone = UUID.fromString("51ae8b2b-2e98-4bf1-9ea4-ec2b0d0ae687");
//    public static final UUID  uuidUnitedStatesExclusiveEconomicZone = UUID.fromString("559c5912-5261-4b9e-a9de-d250ced94409");
//    public static final UUID  uuidGabon = UUID.fromString("594ecdf9-96ba-4613-8793-64c1bff93377");
//    public static final UUID  uuidTrinidadianAndTobagonianExclusiveEconomicZone = UUID.fromString("0d265568-5980-43a5-bd45-751be713fc40");
//    public static final UUID  uuidPakistaniExclusiveEconomicZone = UUID.fromString("85f7189d-efcb-4d77-9149-c6429079ce93");
//    public static final UUID  uuidPersianGulf = UUID.fromString("bb0015a5-341c-4502-900e-7b3de87f1ead");
//    public static final UUID  uuidIvorianExclusiveEconomicZone = UUID.fromString("7469f459-dd57-4774-bf35-6c1b5456f1fa");
//    public static final UUID  uuidGaboneseExclusiveEconomicZone = UUID.fromString("c61bda0b-3523-475e-8983-dbb48e860280");
//    public static final UUID  uuidCongoleseDemocraticRepublicExclusiveEconomicZone = UUID.fromString("efe2d081-6dca-4521-b7f5-58e3347c355c");
//    public static final UUID  uuidChineseExclusiveEconomicZone = UUID.fromString("55b6b012-f198-416d-9952-de30780177d5");
//    public static final UUID  uuidDjiboutianExclusiveEconomicZone = UUID.fromString("f4b9bb79-fe66-4c67-8549-1ea7d1a4ca31");
//    public static final UUID  uuidIranianExclusiveEconomicZone = UUID.fromString("0fb2be11-a6cd-4c83-8c9e-94bff047a55f");
//    public static final UUID  uuidMexicanExclusiveEconomicZone = UUID.fromString("a9229985-baa1-4067-a395-5e2b601d11ca");
//    public static final UUID  uuidOmaniExclusiveEconomicZone = UUID.fromString("68bb7f6c-2791-43ca-baf7-5f14a3fd36d7");
//    public static final UUID uuidUruguayanExclusiveEconomicZone = UUID.fromString("4351a037-6b5f-4507-80b9-01d1befb6956");
//    public static final UUID uuidYemeniExclusiveEconomicZone = UUID.fromString("41bc016e-f2eb-445d-96bb-6822406742a9");

	//feature uuids
	public static final UUID uuidRemark = UUID.fromString("648eab77-8469-4139-bbf4-3fb26ec15864");
	public static final UUID uuidAdditionalinformation = UUID.fromString("ef00c304-ce33-45ef-9543-0b9336a2b6eb");
	public static final UUID uuidSpelling = UUID.fromString("536594a1-21a5-4d99-aa46-132bc7b31316");
	public static final UUID uuidPublicationdate = UUID.fromString("b996b34f-1313-4575-bf46-732676674290");
	public static final UUID uuidSystematics = UUID.fromString("caac0f7f-f43e-4b7c-b296-ec2d930c4d05");
	public static final UUID uuidClassification = UUID.fromString("aa9bffd3-1fa8-4bd7-9e25-e2d162177b3d");
	public static final UUID uuidEnvironment = UUID.fromString("4f8ea10d-2242-443f-9d7d-4ecccdee4953");
	public static final UUID uuidHabitat = UUID.fromString("b7387877-51e3-4192-b9e4-025a359f4b59");
	public static final UUID uuidAuthority = UUID.fromString("9c7f8908-2530-4900-8da9-d328f7ac9031");
	public static final UUID uuidMorphology = UUID.fromString("5be1f948-d85f-497f-a0d5-4e5f3b227274");
	public static final UUID uuidTaxonomicRemarks = UUID.fromString("cc863aee-8da9-448b-82cd-47e3af942998");
	public static final UUID uuidNote = UUID.fromString("2c66d35f-c76e-40e0-951b-f2c340e5973f");
	public static final UUID uuidTaxonomy = UUID.fromString("d5734631-c86b-4212-9b8d-cb62f813e0a0");
	public static final UUID uuidTaxonomicstatus = UUID.fromString("ffbadab5-a8bc-4fb6-a6b3-d1f2593187ff");
	public static final UUID uuidStatus = UUID.fromString("fcc50853-bcff-4d0f-bc9a-123d7f175490");
	public static final UUID uuidRank = UUID.fromString("cabada57-a098-47fc-929f-31c8c910f6cf");
	public static final UUID uuidHomonymy = UUID.fromString("2791a14f-49b2-417f-a248-84c3d022d75f");
	public static final UUID uuidNomenclature = UUID.fromString("15fe184f-4aab-4076-8bbb-3415d6f1f27f");
	public static final UUID uuidTypespecies = UUID.fromString("cf674b0d-76e2-4628-952c-2cd06e209c6e");
	public static final UUID uuidTaxonomicRemark = UUID.fromString("044e7c4e-aab8-4f44-bfa5-0339e7576c74");
	public static final UUID uuidAcknowledgments = UUID.fromString("3b2fd495-3f9a-480e-986a-7643741177da");
	public static final UUID uuidOriginalpublication = UUID.fromString("ea9b7e53-0487-499f-a281-3d82d10e76dd");
	public static final UUID uuidTypelocality = UUID.fromString("7c1c5779-2b4b-467b-b2ca-5ca2e029e116");
	public static final UUID uuidValidity = UUID.fromString("bd066f25-935b-4b4e-a2eb-3fbfcd5e608f");
	public static final UUID uuidIdentification = UUID.fromString("dec3cd5b-0690-4035-825d-bda9aee96bc1");
	public static final UUID uuidSynonymy = UUID.fromString("f5c8be5f-8d33-47df-838e-55fc7999fc81");
	public static final UUID uuidSourceOfSynonymy = UUID.fromString("cf217b3f-360a-42e4-b447-ec87db1d3806");
	public static final UUID uuidDepthRange = UUID.fromString("e5c799a9-87ac-4171-8dfb-8c5c39f9f635");
	public static final UUID uuidFossilRange = UUID.fromString("5c68f42a-5c66-4e2a-8754-8922be104f6e");
    public static final UUID uuidGrammaticalGender = UUID.fromString("ec1ca718-1ef0-41de-87e7-d6b464bf0c24");
    public static final UUID uuidIntroducedSpeciesRemark = UUID.fromString("c60327b7-3333-436f-9aea-c02acaffdf94");
    public static final UUID uuidAlienSpecies = UUID.fromString("03cd2316-e428-45bd-b336-c67137bbcd6a");
    public static final UUID uuidDimensions = UUID.fromString("e2ffd374-b147-4014-a3aa-05394448e59f");
    public static final UUID uuidDiet = UUID.fromString("17e22a59-6fea-4eb2-8e0c-4578f633cd0a");
    public static final UUID uuidReproduction = UUID.fromString("fffdc170-db89-4ef6-9d21-f9dff19b4fb4");

    public static final UUID uuidNewCombination = UUID.fromString("f51f71c7-4a07-491d-b04a-ac5360173ab9");
    public static final UUID uuidTypeMaterial = UUID.fromString("5df1ad63-5f8c-43f9-812d-4448f30ddd0a");
    public static final UUID uuidOriginalCombination = UUID.fromString("1c8e5637-1024-452f-9ebe-28058a5c6473");
    public static final UUID uuidTypeSpecimen = UUID.fromString("0e74bee7-8a83-4f07-a6e9-05e5b1358411");
    public static final UUID uuidOriginalDescription = UUID.fromString("bf7b3d4b-90e1-4eb2-b3ae-4f89e27b126f");
    public static final UUID uuidSpecimen = UUID.fromString("a08e8158-d39d-4897-a6a0-ded4186d4ed0");
    public static final UUID uuidEditorsComment = UUID.fromString("b46ad912-8c61-4c76-ad89-d38451d650e9");
    public static final UUID uuidSyntype = UUID.fromString("6d0989a1-7467-43ec-a087-4838fa8246de");
    public static final UUID uuidBiology = UUID.fromString("af5c6832-74f3-4b87-bac9-6fdfc68ffada");
    public static final UUID uuidEtymology = UUID.fromString("4b17f86f-f264-4550-bc08-510939340156");

	//extension type uuids
    public static final UUID ermsExtensionTypeVocabularyUuid = UUID.fromString("27c68dd3-95e1-4690-998d-451d016fc6f7");
	public static final UUID uuidPesiTaxonStatus = UUID.fromString("859eee7f-5240-48a0-8edc-7af63557fa6e");
	public static final UUID uuidExtGazetteer = UUID.fromString("dcfa124a-1028-49cd-aea5-fdf9bd396c1a");
	public static final UUID uuidExtImis = UUID.fromString("ee2ac2ca-b60c-4e6f-9cad-720fcdb0a6ae");
	public static final UUID uuidExtFossilStatus = UUID.fromString("ec3dffbe-a0c8-4d76-845f-5fc166a33d5b");
	public static final UUID uuidExtTsn = UUID.fromString("6b0df02b-7278-4ce0-8fc9-0e6523832eb5");
	public static final UUID uuidExtDisplayName = UUID.fromString("cd72225d-32c7-4b2d-a973-a95184392690");
	public static final UUID uuidExtFuzzyName = UUID.fromString("8870dc69-d3a4-425f-a5a8-093a79f527a8");
	public static final UUID uuidExtCredibility = UUID.fromString("909a3886-8744-49dc-b9cc-277378b81b42");
	public static final UUID uuidExtCompleteness = UUID.fromString("141f4816-78c0-4da1-8a79-5c9031e6b149");
	public static final UUID uuidExtUnacceptReason = UUID.fromString("3883fb79-374d-4120-964b-9666307e3567");
	public static final UUID uuidExtQualityStatus = UUID.fromString("4de84c6e-41bd-4a0e-894d-77e9ec3103d2");
	public static final UUID uuidExtAuthor = UUID.fromString("85387300-281f-47bc-8499-7008075dc8e0");
	public static final UUID uuidExtErmsLink = UUID.fromString("b2d6ee54-1363-4641-9658-75a1843b84ff");

	//AnnotationType
    public static final UUID ermsAnnotationTypeVocabularyUuid = UUID.fromString("6d59385b-3065-4a15-b3ed-ffcd0087419d");
	public static final UUID uuidAnnSpeciesExpertName = UUID.fromString("4d8abf02-3d92-4c65-b30b-0393a1f4818b");

	//MarkerTypes
	public static final UUID ermsMarkerTypeVocabularyUuid = UUID.fromString("c716ad33-839a-4303-820d-6f178c19391a");
	public static final UUID uuidMarkerMarine = UUID.fromString("5da78a28-5668-4ed5-b788-10c69343f91e");
	public static final UUID uuidMarkerBrackish = UUID.fromString("2da39f5d-67d6-4779-b40d-923dca85fe14");
	public static final UUID uuidMarkerFreshwater = UUID.fromString("1190b182-e1d3-4986-8cc3-a6de3c115cf7");
	public static final UUID uuidMarkerTerrestrial = UUID.fromString("5ed92edb-e2c6-48da-8367-6e82071c888f");

	public static final UUID uuidMarkerRefPublication = UUID.fromString("cdc1e38a-7b80-450a-8eb9-78035a51f33c");
	public static final UUID uuidMarkerRefInformal = UUID.fromString("296a10f1-596d-4799-9624-34f9c5f54dc6");
    public static final UUID uuidMarkerRefTypeI = UUID.fromString("1632d9e4-d921-4835-8915-d0d6ad298c7e");


	public static NomenclaturalCode kingdomId2NomCode(Integer kingdomId){
		switch (kingdomId){
			case 1: return null;
			case 2: return NomenclaturalCode.ICZN;  //Animalia
			case 3: return NomenclaturalCode.ICNAFP;  //Plantae
			case 4: return NomenclaturalCode.ICNAFP;  //Fungi
			case 5: return NomenclaturalCode.ICNAFP ;  //Protozoa, not sure if correct, they have "subsp."
			case 6: return NomenclaturalCode.ICNP ;  //Bacteria
			case 7: return NomenclaturalCode.ICNAFP;  //Chromista??, not sure if correct, they have "subsp."
			case 147415: return NomenclaturalCode.ICNP;  //Monera, it is only an alternative name for Bacteria and should not be handled as separate kingdom
			//-> formatting of infrageneric taxa and available ranks (rank table) let me assume that ICZN is most suitable
			//at the same time formatting of subsp. (with marker!) behaves like ICNAFP so this is unclear
			default: return null;
		}
	}

	@Override
	public NameTypeDesignationStatus getNameTypeDesignationStatusByKey(String key) throws UndefinedTransformerMethodException {
		if (key == null){
			return null;
		}
		Integer intDesignationId = Integer.valueOf(key);
		switch (intDesignationId){
			case 1: return NameTypeDesignationStatus.ORIGINAL_DESIGNATION();
			case 2: return NameTypeDesignationStatus.SUBSEQUENT_DESIGNATION();
			case 3: return NameTypeDesignationStatus.MONOTYPY();
			default:
				String warning = "Unknown name type designation status id " + key;
				logger.warn(warning);
				return null;
		}
	}

	@Override
	public UUID getNameTypeDesignationStatusUuid(String key) throws UndefinedTransformerMethodException {
		//not needed
		return super.getNameTypeDesignationStatusUuid(key);
	}

	private Map<String, Language> iso639_3_languages;

	@Override
    public Language getLanguageByKey(String ermsAbbrev) throws IllegalArgumentException {
		if(iso639_3_languages == null){
		    fillIso639_3_languages();
		}
//	    Set<String> unhandledLanguages = new HashSet<>();
		if (StringUtils.isBlank(ermsAbbrev)){
		    return null;
		}else if (iso639_3_languages.get(ermsAbbrev)!= null){
		    return iso639_3_languages.get(ermsAbbrev);
		}else if ("fra".equals(ermsAbbrev)){
		    return Language.FRENCH();
		}else if ("deu".equals(ermsAbbrev)){
            return Language.GERMAN();
		}else if ("eng".equals(ermsAbbrev)){
            return Language.ENGLISH();
        }else{
		    //unhandledLanguage.add(xxx);
//			if (unhandledLanguages.contains(ermsAbbrev)){
//				logger.warn("Unhandled language '" + ermsAbbrev + "' replaced by 'UNDETERMINED'" );
//				return Language.UNDETERMINED();
//			}
			String warning = "New language abbreviation " + ermsAbbrev;
			logger.warn(warning);
			try {
                throw new IllegalArgumentException(warning);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
		}
	}

    private void fillIso639_3_languages() {
        iso639_3_languages = new HashMap<>();
        TermVocabulary<Language> voc639_3 = TermVocabulary.NewInstance(TermType.Language,
                Language.class, "ISO 639-3 language subset", "ISO 639-3 languages", "ISO 639-3", null);
        voc639_3.setUuid(Language.uuidLanguageIso639_3Vocabulary);

        addIso639_3_language(voc639_3, "swh", Language.uuidLangKiswahiliSwahili, "Kiswahili, Swahili");
        addIso639_3_language(voc639_3, "zlm", Language.uuidLangMalay, "Malay");
        addIso639_3_language(voc639_3, "bcc", Language.uuidLangSouthernBalochi, "Southern Balochi");
        addIso639_3_language(voc639_3, "lij", Language.uuidLangLigurian, "Ligurian");
        addIso639_3_language(voc639_3, "mey", Language.uuidLangHassaniyya, "Hassaniyya");
        addIso639_3_language(voc639_3, "kri", Language.uuidLangKrio, "Krio");
        addIso639_3_language(voc639_3, "evn", Language.uuidLangEvenki, "Evenki");
        addIso639_3_language(voc639_3, "kpy", Language.uuidLangKoryak, "Koryak");
        addIso639_3_language(voc639_3, "eve", Language.uuidLangEven, "Even");
        addIso639_3_language(voc639_3, "yrk", Language.uuidLangNenets, "Nenets");
        addIso639_3_language(voc639_3, "ckt", Language.uuidLangChukot, "Chukot");
        addIso639_3_language(voc639_3, "aeb", Language.uuidLangTunisianArabic, "Tunisian Arabic");
        addIso639_3_language(voc639_3, "auq", Language.uuidLangAnusKorur, "Anus, Korur");
        addIso639_3_language(voc639_3, "kca", Language.uuidLangKhanty, "Khanty");
        addIso639_3_language(voc639_3, "vls", Language.uuidLangVlaams, "Vlaams");
    }

    private void addIso639_3_language(TermVocabulary<Language> voc, String abbrev, UUID uuid, String label) {
        Language lang = Language.NewInstance(uuid, label, abbrev);
        voc.addTerm(lang);
        lang.setIdInVocabulary(abbrev);
        iso639_3_languages.put(abbrev, lang);
    }

	@Override
	public ExtensionType getExtensionTypeByKey(String key) throws UndefinedTransformerMethodException {
		if (key == null){return null;
		}
		return null;
	}

	@Override
	public UUID getExtensionTypeUuid(String key)
			throws UndefinedTransformerMethodException {
		if (key == null){return null;
//		}else if (key.equalsIgnoreCase("recent only")){return uuidRecentOnly;
//		}else if (key.equalsIgnoreCase("recent + fossil")){return uuidRecentAndFossil;
//		}else if (key.equalsIgnoreCase("fossil only")){return uuidFossilOnly;
		}
		return null;
	}

	public static UUID uuidFromGuName(String guName){
		if (StringUtils.isBlank(guName)){return null;
		}else if (guName.equalsIgnoreCase("Adriatic Sea")){ return uuidAdriaticSea;
		}else if (guName.equalsIgnoreCase("Aegean Sea")){ return uuidAegeanSea;
		}else if (guName.equalsIgnoreCase("Archipelago Sea")){ return uuidArchipelagoSea;
		}else if (guName.equalsIgnoreCase("Azores Exclusive Economic Zone")){ return uuidAzoresExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Balear Sea")){ return uuidBalearSea;
		}else if (guName.equalsIgnoreCase("Baltic Proper")){ return uuidBalticProper;
		}else if (guName.equalsIgnoreCase("Baltic Sea")){ return uuidBalticSea;
		}else if (guName.equalsIgnoreCase("Barentsz Sea")){ return uuidBarentszSea;
		}else if (guName.equalsIgnoreCase("Belgian Exclusive Economic Zone")){ return uuidBelgianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Baelt Sea")){ return uuidBaeltSea;
		}else if (guName.equalsIgnoreCase("Bay of Biscay")){ return uuidBayOfBiscay;
		}else if (guName.equalsIgnoreCase("Black Sea")){ return uuidBlackSea;
		}else if (guName.equalsIgnoreCase("Bothnian Sea")){ return uuidBothnianSea;
		}else if (guName.equalsIgnoreCase("Bulgarian Exclusive Economic Zone")){ return uuidBulgarianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Caspian Sea")){ return uuidCaspianSea;
		}else if (guName.equalsIgnoreCase("Croatian Exclusive Economic Zone")){ return uuidCroatianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Danish Exclusive Economic Zone")){ return uuidDanishExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Dutch Exclusive Economic Zone")){ return uuidDutchExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Egyptian Exclusive Economic Zone")){ return uuidEgyptianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("English Channel")){ return uuidEnglishChannel;
		}else if (guName.equalsIgnoreCase("Estonian Exclusive Economic Zone")){ return uuidEstonianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("European Marine Waters")){ return uuidEuropeanMarineWaters;
		}else if (guName.equalsIgnoreCase("French Exclusive Economic Zone")){ return uuidFrenchExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("German Exclusive Economic Zone")){ return uuidGermanExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Greek Exclusive Economic Zone")){ return uuidGreekExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Icelandic Exclusive Economic Zone")){ return uuidIcelandicExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Irish Exclusive economic Zone")){ return uuidIrishExclusiveeconomicZone;
		}else if (guName.equalsIgnoreCase("Irish Sea and St. George's Channel")){ return uuidIrishSea;
		}else if (guName.equalsIgnoreCase("Italian Exclusive Economic Zone")){ return uuidItalianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Lebanese Exclusive Economic Zone")){ return uuidLebaneseExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Madeiran Exclusive Economic Zone")){ return uuidMadeiranExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Marmara Sea")){ return uuidMarmaraSea;
		}else if (guName.equalsIgnoreCase("Mediterranean Sea")){ return uuidMediterraneanSea;
		}else if (guName.equalsIgnoreCase("Moroccan Exclusive Economic Zone")){ return uuidMoroccanExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("North Baltic proper")){ return uuidNorthBalticproper;
		}else if (guName.equalsIgnoreCase("North Sea")){ return uuidNorthSea;
		}else if (guName.equalsIgnoreCase("Norwegian Exclusive Economic Zone")){ return uuidNorwegianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Norwegian Sea")){ return uuidNorwegianSea;
		}else if (guName.equalsIgnoreCase("Polish Exclusive Economic Zone")){ return uuidPolishExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Portuguese Exclusive Economic Zone")){ return uuidPortugueseExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Sea of Azov")){ return uuidSeaofAzov;
		}else if (guName.equalsIgnoreCase("Skagerrak")){ return uuidSkagerrak;
		}else if (guName.equalsIgnoreCase("South Baltic proper")){ return uuidSouthBalticproper;
		}else if (guName.equalsIgnoreCase("Spanish Exclusive Economic Zone")){ return uuidSpanishExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Spanish Exclusive Economic Zone [Mediterranean part]")){ return uuidSpanishExclusiveEconomicZoneMediterraneanpart;
		}else if (guName.equalsIgnoreCase("Swedish Exclusive Economic Zone")){ return uuidSwedishExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Tirreno Sea")){ return uuidTirrenoSea;
		}else if (guName.equalsIgnoreCase("Tyrrhenian Sea")){ return uuidTyrrhenianSea;
		}else if (guName.equalsIgnoreCase("Tunisian Exclusive Economic Zone")){ return uuidTunisianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Turkish Exclusive Economic Zone")){ return uuidTurkishExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Ukrainian Exclusive Economic Zone")||
		        guName.equalsIgnoreCase("Overlapping claim Ukrainian Exclusive Economic Zone")){ return uuidUkrainianExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("United Kingdom Exclusive Economic Zone")){ return uuidUnitedKingdomExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Wadden Sea")){ return uuidWaddenSea;
		}else if (guName.equalsIgnoreCase("White Sea")){ return uuidWhiteSea;


//        }else if (guName.equalsIgnoreCase("Afghanistan")){ return uuidAfghanistan;
		}else if (guName.equalsIgnoreCase("Alboran Sea")){ return uuidAlboranSea;
		}else if (guName.equalsIgnoreCase("Algeria")){ return uuidAlgeria;
//		}else if (guName.equalsIgnoreCase("Angola")){ return uuidAngola;
		}else if (guName.equalsIgnoreCase("Arctic Ocean")){ return uuidArcticOcean;
//        }else if (guName.equalsIgnoreCase("Australian Exclusive Economic Zone")){ return uuidAustralianExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Bahamas")){ return uuidBahamas;
		}else if (guName.equalsIgnoreCase("Balearic Sea")){ return uuidBalearicSea;
		}else if (guName.equalsIgnoreCase("Belgium")){ return uuidBelgium;
//		}else if (guName.equalsIgnoreCase("Belize")){ return uuidBelize;
//		}else if (guName.equalsIgnoreCase("Brazil")){ return uuidBrazil;
		}else if (guName.equalsIgnoreCase("Bulgaria")){ return uuidBulgaria;
//		}else if (guName.equalsIgnoreCase("Canada")){ return uuidCanada;
//		}else if (guName.equalsIgnoreCase("Caribbean Sea")){ return uuidCaribbeanSea;
		}else if (guName.equalsIgnoreCase("Cape Verde")){ return uuidCapeVerde;
		}else if (guName.equalsIgnoreCase("Cape Verdean Exclusive Economic Zone")){ return uuidCapeVerdeanExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Chile")){ return uuidChile;
//		}else if (guName.equalsIgnoreCase("Colombia")){ return uuidColombia;
//		}else if (guName.equalsIgnoreCase("Costa Rica")){ return uuidCostaRica;
		}else if (guName.equalsIgnoreCase("Croatia")){ return uuidCroatia;
//		}else if (guName.equalsIgnoreCase("Cuba")){ return uuidCuba;
		}else if (guName.equalsIgnoreCase("Denmark")){ return uuidDenmark;
		}else if (guName.equalsIgnoreCase("Egypt")){ return uuidEgypt;
		}else if (guName.equalsIgnoreCase("Estonia")){ return uuidEstonia;
		}else if (guName.equalsIgnoreCase("Faeroe Exclusive Economic Zone")){ return uuidFaeroeExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("France")){ return uuidFrance;
//		}else if (guName.equalsIgnoreCase("Ghana")){ return uuidGhana;
		}else if (guName.equalsIgnoreCase("Greece")){ return uuidGreece;
		}else if (guName.equalsIgnoreCase("Germany")){ return uuidGermany;
        }else if (guName.equalsIgnoreCase("Gulf of Bothnia")){ return uuidGulfOfBothnia;
		}else if (guName.equalsIgnoreCase("Gulf of Finland")){ return uuidGulfOfFinland;
//		}else if (guName.equalsIgnoreCase("Gulf of Guinea")){ return uuidGulfOfGuinea;
//		}else if (guName.equalsIgnoreCase("Gulf of Mexico")){ return uuidGulfOfMexico;
		}else if (guName.equalsIgnoreCase("Gulf of Riga")){ return uuidGulfOfRiga;
		}else if (guName.equalsIgnoreCase("Iceland")){ return uuidIceland;
		}else if (guName.equalsIgnoreCase("Ionian Sea")){ return uuidIonianSea;
		}else if (guName.equalsIgnoreCase("Ireland")){ return uuidIreland;
		}else if (guName.equalsIgnoreCase("Israeli Exclusive Economic Zone")){return uuidIsraeliExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Italy")){ return uuidItaly;
//		}else if (guName.equalsIgnoreCase("Jamaica")){ return uuidJamaica;
		}else if (guName.equalsIgnoreCase("Kattegat")){ return uuidKattegat;
		}else if (guName.equalsIgnoreCase("Levantine Sea")){ return uuidLevantineSea;
		}else if (guName.equalsIgnoreCase("Ligurian Sea")){ return uuidLigurianSea;
		}else if (guName.equalsIgnoreCase("Maltese Exclusive Economic Zone")){ return uuidMalteseExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Mauritanian Exclusive Economic Zone")){ return uuidMauritanianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Mediterranean Sea - Eastern Basin")){ return uuidMediterraneanSea_EasternBasin;
		}else if (guName.equalsIgnoreCase("Mediterranean Sea - Western Basin")){ return uuidMediterraneanSea_WesternBasin;
//		}else if (guName.equalsIgnoreCase("Mexico")){ return uuidMexico;
//		}else if (guName.equalsIgnoreCase("Mongolia")){ return uuidMongolia;
        }else if (guName.equalsIgnoreCase("Morocco")){ return uuidMorocco;
		}else if (guName.equalsIgnoreCase("Netherlands")){ return uuidNetherlands;
//		}else if (guName.equalsIgnoreCase("New Zealand")){ return uuidNewZealand;
//		}else if (guName.equalsIgnoreCase("New Zealand Exclusive Economic Zone")){ return uuidNewZealandExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("North Atlantic Ocean")){ return uuidNorthAtlanticOcean;
		}else if (guName.equalsIgnoreCase("Norway")){ return uuidNorway;
//		}else if (guName.equalsIgnoreCase("Panama")){ return uuidPanama;
//		}else if (guName.equalsIgnoreCase("Panamanian Exclusive Economic Zone")){ return uuidPanamanianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Portugal")){ return uuidPortugal;
		}else if (guName.equalsIgnoreCase("Portuguese Exclusive Economic Zone (Azores)")){ return uuidPortugueseExclusiveEconomicZone_Azores;
		}else if (guName.equalsIgnoreCase("Portuguese Exclusive Economic Zone (Madeira)")){ return uuidPortugueseExclusiveEconomicZone_Madeira;
		}else if (guName.equalsIgnoreCase("Red Sea")){ return uuidRedSea;
		}else if (guName.equalsIgnoreCase("Russian Exclusive economic Zone")){ return uuidRussianExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Sea of Marmara")){ return uuidSeaOfMarmara;
//		}else if (guName.equalsIgnoreCase("Senegalese Exclusive Economic Zone")){ return uuidSenegaleseExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("Singapore")){ return uuidSingapore;
		}else if (guName.equalsIgnoreCase("Slovenian Exclusive Economic Zone")){ return uuidSlovenianExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("South Africa")){ return uuidSouthAfrica;
//		}else if (guName.equalsIgnoreCase("South African Exclusive Economic Zone")){ return uuidSouthAfricanExclusiveEconomicZone;
//		}else if (guName.equalsIgnoreCase("South China Sea")){ return uuidSouthChinaSea;
		}else if (guName.equalsIgnoreCase("Spain")){ return uuidSpain;
		}else if (guName.equalsIgnoreCase("Spanish Exclusive Economic Zone (Canary Islands)")){ return uuidSpanishExclusiveEconomicZone_CanaryIslands;
//		}else if (guName.equalsIgnoreCase("Sri Lankan Exclusive Economic Zone")){ return uuidSriLankanExclusiveEconomicZone;
		}else if (guName.equalsIgnoreCase("Strait of Gibraltar")){ return uuidStraitOfGibraltar;
		}else if (guName.equalsIgnoreCase("Sweden")){ return uuidSweden;
		}else if (guName.equalsIgnoreCase("Tunisia")){ return uuidTunisia;
		}else if (guName.equalsIgnoreCase("Türkiye")){ return uuidTuerkiye;
		}else if (guName.equalsIgnoreCase("United Kingdom")){ return uuidUnitedKingdom;
//        }else if (guName.equalsIgnoreCase("United States")){ return uuidUnitedStates;
//		}else if (guName.equalsIgnoreCase("United States Exclusive Economic Zone (Alaska)")){ return uuidUnitedStatesExclusiveEconomicZone_Alaska;
//		}else if (guName.equalsIgnoreCase("Venezuela")){ return uuidVenezuela;
        }else if (guName.equalsIgnoreCase("Celtic Sea")){ return uuidCelticSea;
        }else if (guName.equalsIgnoreCase("Turkish part of the Aegean Sea")){ return uuidTurkishAegeanSea;
        }else if (guName.equalsIgnoreCase("Romanian Exclusive Economic Zone")){ return uuidRomanianExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("British Exclusive Economic Zone")){ return uuidBritishExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Algerian Exclusive Economic Zone")){ return uuidAlgerianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Sierra Leonean Exclusive Economic Zone")){ return uuidSierraLeoneanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("São Toméan Exclusive Economic Zone")){ return uuidSaoTomeanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("British Exclusive Economic Zone (Bermuda)")){ return uuidBritishExclusiveEconomicZoneBermuda;
//        }else if (guName.equalsIgnoreCase("Bahamian Exclusive Economic Zone")){ return uuidBahamianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Cuban Exclusive Economic Zone")){ return uuidCubanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("British Exclusive Economic Zone (Cayman Islands)")){ return uuidBritishExclusiveEconomicZoneCaymanIslands;
//        }else if (guName.equalsIgnoreCase("United States Exclusive Economic Zone (Puerto Rico)")){ return uuidUnitedStatesExclusiveEconomicZonePuertoRico;
//        }else if (guName.equalsIgnoreCase("British Exclusive Economic Zone (British Virgin Islands)")){ return uuidBritishExclusiveEconomicZoneBritishVirginIslands;
//        }else if (guName.equalsIgnoreCase("Venezuelan Exclusive Economic Zone")){ return uuidVenezuelanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("French Exclusive Economic Zone (Guadeloupe)")){ return uuidFrenchExclusiveEconomicZoneGuadeloupe;
//        }else if (guName.equalsIgnoreCase("Belizean Exclusive Economic Zone")){ return uuidBelizeanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Jamaican Exclusive Economic Zone")){ return uuidJamaicanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Angolan Exclusive Economic Zone")){ return uuidAngolanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Singaporean Exclusive Economic Zone")){ return uuidSingaporeanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Colombian Exclusive Economic Zone")){ return uuidColombianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Ghanaian Exclusive Economic Zone")){ return uuidGhanaianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Costa Rican Exclusive Economic Zone")){ return uuidCostaRicanExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Danish Exclusive Economic Zone (Faeroe)")){ return uuidDanishExclusiveEconomicZoneFaeroe;
        }else if (guName.equalsIgnoreCase("Norwegian Exclusive Economic Zone (Jan Mayen)")){ return uuidNorwegianExclusiveEconomicZoneJanMayen;
//        }else if (guName.equalsIgnoreCase("Dutch Exclusive Economic Zone (Curaçao)")){ return uuidDutchExclusiveEconomicZoneCuraçao;
//        }else if (guName.equalsIgnoreCase("Dutch Exclusive Economic Zone (Aruba)")){ return uuidDutchExclusiveEconomicZoneAruba;
//        }else if (guName.equalsIgnoreCase("Dutch Exclusive Economic Zone (Bonaire)")){ return uuidDutchExclusiveEconomicZoneBonaire;
        }else if (guName.equalsIgnoreCase("Cypriote Exclusive Economic Zone")){ return uuidCyprioteExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Norwegian Exclusive Economic Zone (Svalbard)")){ return uuidNorwegianExclusiveEconomicZoneSvalbard;
        }else if (guName.equalsIgnoreCase("French Exclusive Economic Zone (Martinique)")){ return uuidFrenchExclusiveEconomicZoneMartinique;

        //2025
        }else if (guName.equalsIgnoreCase("Libyan Exclusive Economic Zone")){ return uuidLibyanExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Syrian Exclusive Economic Zone")){ return uuidSyrianExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Jordanian Exclusive Economic Zone")){ return uuidJordanianExclusiveEconomicZone;
        }else if (guName.equalsIgnoreCase("Montenegrin Exclusive Economic Zone")){ return uuidMontenegrinExclusiveEconomicZone;

        //        }else if (guName.equalsIgnoreCase("Indian Ocean")){ return uuidIndianOcean;
//        }else if (guName.equalsIgnoreCase("Gulf of Saint Lawrence")){ return uuidGulfOfSaintLawrence;
//        }else if (guName.equalsIgnoreCase("Australia")){ return uuidAustralia;
//        }else if (guName.equalsIgnoreCase("French Exclusive Economic Zone (New Caledonia)")){ return uuidFrenchExclusiveEconomicZoneNewCaledonia;
//        }else if (guName.equalsIgnoreCase("Philippine Exclusive Economic Zone")){ return uuidPhilippineExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Mozambican Exclusive Economic Zone")){ return uuidMozambicanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("British Exclusive Economic Zone (Saint Helena)")){ return uuidBritishExclusiveEconomicZoneSaintHelena;
//        }else if (guName.equalsIgnoreCase("Dominican (Republic) Exclusive Economic Zone")){ return uuidDominicanRepublicExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("United States Exclusive Economic Zone (Hawaii)")){ return uuidUnitedStatesExclusiveEconomicZoneHawaii;
//        }else if (guName.equalsIgnoreCase("Brazilian Exclusive Economic Zone")){ return uuidBrazilianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Guinean Exclusive Economic Zone")){ return uuidGuineanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("United States Exclusive Economic Zone")){ return uuidUnitedStatesExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Gabon")){ return uuidGabon;
//        }else if (guName.equalsIgnoreCase("Trinidadian and Tobagonian Exclusive Economic Zone")){ return uuidTrinidadianAndTobagonianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Pakistani Exclusive Economic Zone")){ return uuidPakistaniExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Persian Gulf")){ return uuidPersianGulf;
//        }else if (guName.equalsIgnoreCase("Ivorian Exclusive Economic Zone")){ return uuidIvorianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Gabonese Exclusive Economic Zone")){ return uuidGaboneseExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Congolese (Democratic Republic of) Exclusive Economic Zone")){ return uuidCongoleseDemocraticRepublicExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Chinese Exclusive Economic Zone")){ return uuidChineseExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Djiboutian Exclusive Economic Zone")){ return uuidDjiboutianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Iranian Exclusive Economic Zone")){ return uuidIranianExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Mexican Exclusive Economic Zone")){ return uuidMexicanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Omani Exclusive Economic Zone")){ return uuidOmaniExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Uruguayan Exclusive Economic Zone")){ return uuidUruguayanExclusiveEconomicZone;
//        }else if (guName.equalsIgnoreCase("Yemeni Exclusive Economic Zone")){ return uuidYemeniExclusiveEconomicZone;

		}else{
			throw new IllegalArgumentException("Unknown area " + guName);
		}
	}

	public static Feature noteType2Feature(String type){
		if (StringUtils.isBlank(type)){return null;
		}else if (type.equals("Remark")){return Feature.UNKNOWN();
		}else if (type.equals("Additional information")){return Feature.UNKNOWN();
		}else if (type.equals("Spelling")){return Feature.UNKNOWN();
		}else if (type.equals("Publication date")){return Feature.UNKNOWN();
		}else if (type.equals("Systematics")){return Feature.UNKNOWN();
		}else if (type.equals("Classification")){return Feature.UNKNOWN();
		}else if (type.equals("Environment")){return Feature.UNKNOWN();
		}else if (type.equals("Habitat")){return Feature.UNKNOWN();
		}else if (type.equals("Authority")){return Feature.UNKNOWN();
		}else if (type.equals("Ecology")){return Feature.UNKNOWN();
		}else if (type.equals("Morphology")){return Feature.UNKNOWN();
		}else if (type.equals("Taxonomic Remarks")){return Feature.UNKNOWN();
		}else if (type.equals("NULL")){return Feature.UNKNOWN();
		}else if (type.equals("Distribution")){return Feature.UNKNOWN();
		}else if (type.equals("Note")){return Feature.UNKNOWN();
		}else if (type.equals("Taxonomy")){return Feature.UNKNOWN();
		}else if (type.equals("Taxonomic status")){return Feature.UNKNOWN();
		}else if (type.equals("Status")){return Feature.UNKNOWN();
		}else if (type.equals("Rank")){return Feature.UNKNOWN();
		}else if (type.equals("Homonymy")){return Feature.UNKNOWN();
		}else if (type.equals("Nomenclature")){return Feature.UNKNOWN();
		}else if (type.equals("Type species")){return Feature.UNKNOWN();
		}else if (type.equals("Taxonomic Remark")){return Feature.UNKNOWN();
		}else if (type.equals("Diagnosis")){return Feature.UNKNOWN();
		}else if (type.equals("Date of Publication")){return Feature.UNKNOWN();
		}else if (type.equals("Acknowledgments")){return Feature.UNKNOWN();
		}else if (type.equals("Biology")){return Feature.UNKNOWN();
		}else if (type.equals("Original publication")){return Feature.UNKNOWN();
		}else if (type.equals("Type locality")){return Feature.UNKNOWN();
		}else if (type.equals("Host")){return Feature.UNKNOWN();
		}else if (type.equals("Validity")){return Feature.UNKNOWN();
		}else if (type.equals("Identification")){return Feature.UNKNOWN();
		}else if (type.equals("Synonymy")){return Feature.UNKNOWN();
		}else{
			throw new IllegalArgumentException("Unknown note type " + type);
		}
	}

	@Override
	public Feature getFeatureByKey(String key) throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){return null;
		}else if (key.equalsIgnoreCase("Distribution")){return Feature.DISTRIBUTION();
		}else if (key.equalsIgnoreCase("Ecology")){return Feature.ECOLOGY();
		}else if (key.equalsIgnoreCase("Diagnosis")){return Feature.DIAGNOSIS();
		}else if (key.equalsIgnoreCase("Host")){return Feature.HOSTPLANT();
		}else if (key.equalsIgnoreCase("Etymology")){return Feature.ETYMOLOGY();
        }else{
			return null;
		}
	}

	@Override
	public UUID getFeatureUuid(String key)
			throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){return null;
		}else if (key.equalsIgnoreCase("Acknowledgments")){return Feature.uuidAcknowledgments;
		}else if (key.equalsIgnoreCase("Additional information")){return uuidAdditionalinformation;
		}else if (key.equalsIgnoreCase("Alien species")){return uuidAlienSpecies;
		}else if (key.equalsIgnoreCase("Authority")){return uuidAuthority;
		}else if (key.equalsIgnoreCase("Biology")){return uuidBiology;
		}else if (key.equalsIgnoreCase("Classification")){return uuidClassification;
		}else if (key.equalsIgnoreCase("Date of Publication")){return uuidPublicationdate;
		}else if (key.equalsIgnoreCase("Depth range")){return uuidDepthRange;
		}else if (key.equalsIgnoreCase("Diet")){return uuidDiet;
		}else if (key.equalsIgnoreCase("Dimensions")){return uuidDimensions;
		}else if (key.equalsIgnoreCase("Editor's comment")){return uuidEditorsComment;
		}else if (key.equalsIgnoreCase("Environment")){return uuidEnvironment;
		}else if (key.equalsIgnoreCase("Etymology")){return Feature.uuidEtymology;
		}else if (key.equalsIgnoreCase("Fossil range")){return uuidFossilRange;
		}else if (key.equalsIgnoreCase("Grammatical gender")){return uuidGrammaticalGender;
		}else if (key.equalsIgnoreCase("Habitat")){return uuidHabitat;
		}else if (key.equalsIgnoreCase("Homonymy")){return uuidHomonymy;
		}else if (key.equalsIgnoreCase("Identification")){return uuidIdentification;
		}else if (key.equalsIgnoreCase("Introduced species remark")){return uuidIntroducedSpeciesRemark;
		}else if (key.equalsIgnoreCase("Morphology")){return uuidMorphology;
		}else if (key.equalsIgnoreCase("New combination")){return uuidNewCombination;
		}else if (key.equalsIgnoreCase("Note")){return uuidNote;
		}else if (key.equalsIgnoreCase("Nomenclature")){return uuidNomenclature;
		}else if (key.equalsIgnoreCase("Original Combination")){return uuidOriginalCombination;
		}else if (key.equalsIgnoreCase("Original publication")){return uuidOriginalpublication;
		}else if (key.equalsIgnoreCase("Original description")){return uuidOriginalDescription;
		}else if (key.equalsIgnoreCase("Publication date")){return uuidPublicationdate;
		}else if (key.equalsIgnoreCase("Rank")){return uuidRank;
		}else if (key.equalsIgnoreCase("Remark")){return uuidRemark;
		}else if (key.equalsIgnoreCase("Reproduction")){return uuidReproduction;
		}else if (key.equalsIgnoreCase("Specimen")){return uuidSpecimen;
		}else if (key.equalsIgnoreCase("Spelling")){return uuidSpelling;
		}else if (key.equalsIgnoreCase("Status")){return uuidStatus;
		}else if (key.equalsIgnoreCase("Synonymy")){return uuidSynonymy;
		}else if (key.equalsIgnoreCase("Syntype")){return uuidSyntype;
		}else if (key.equalsIgnoreCase("Systematics")){return uuidSystematics;
		}else if (key.equalsIgnoreCase("Taxonomic Remarks")){return uuidTaxonomicRemarks;
		}else if (key.equalsIgnoreCase("Taxonomic Remark")){return uuidTaxonomicRemark;
		}else if (key.equalsIgnoreCase("Taxonomic status")){return uuidTaxonomicstatus;
		}else if (key.equalsIgnoreCase("Taxonomy")){return uuidTaxonomy;
		}else if (key.equalsIgnoreCase("Type locality")){return uuidTypelocality;
		}else if (key.equalsIgnoreCase("Type material")){return uuidTypeMaterial;
		}else if (key.equalsIgnoreCase("Type species")){return uuidTypespecies;
		}else if (key.equalsIgnoreCase("Type specimen")){return uuidTypeSpecimen;
		}else if (key.equalsIgnoreCase("Validity")){return uuidValidity;

		}else{
			logger.warn("Feature key " + key + " not yet supported by ERMS transformer");
			return null;
		}
	}

	//handles the nomstatus type of unaccept reason
	@Override
    public NomenclaturalStatusType getNomenclaturalStatusByKey(String key) throws UndefinedTransformerMethodException{
	   if (isBlank(key)){
	       return null;
	   }else if (key.matches("(?i).*inval.*")){
	       return NomenclaturalStatusType.INVALID();  //1,  test if correct for zoological names
       }else if (key.matches("(?i).*not val*")){
           return NomenclaturalStatusType.INVALID();  //1,  test if correct for zoological names
       }else if (key.matches("(?i).*illeg.*")){
           return NomenclaturalStatusType.ILLEGITIMATE();  //2
       }else if (key.matches("(?i).*nud.*")){
           return NomenclaturalStatusType.NUDUM();   //3
       }else if (key.matches("(?i).*rej\\..*")){
           return NomenclaturalStatusType.REJECTED();  //4
       }else if (key.matches("(?i).*superfl.*")){
           return NomenclaturalStatusType.SUPERFLUOUS(); //12
       }else if (key.matches("(?i).*Comb\\. nov.*")){
           //??
           return NomenclaturalStatusType.NOVUM();  // 16
       }else if (key.matches("(?i).*New name.*")){
           //??
           return NomenclaturalStatusType.NOVUM();   // 16
       }else if (key.matches("(?i)new combination")){
           //TODO better use BerlinModelTransformer.uuidNomStatusCombIned, see also comment on NomenclaturalStatusType.COMB_NOV()
           return NomenclaturalStatusType.COMB_NOV();  //comb. ined./21
	   }else{
	       return null;
	   }
	}

    @Override
    public IRelationshipType[] getSynonymRelationTypesByKey(String unacceptreason, DbImportStateBase<?,?> state) {

        IRelationshipType[] result = new IRelationshipType[4];
        SynonymType synType = SynonymType.SYNONYM_OF;
        TaxonRelationshipType taxonRelType = getSynTaxonRelType(state);
        NameRelationshipType nameType = null;
//        HybridRelationshipType hybridType = null;
        ((ErmsImportState)state).setUnhandledUnacceptReason(null);  //see below for unhandledAcceptReason handling

        //according to SQL script erms300_Match_Relation&Status.sql

        unacceptreason = unacceptreason == null? null: unacceptreason.trim();
        boolean handled = false;
        if (unacceptreason == null || isBlank(unacceptreason)){
            handled = true;  //no change
        }else if (unacceptreason.matches("(?i)(synonym|superseded recombination|transferred to .*)")){
            handled = true;  //no change
        }else{
            if (unacceptreason.matches("(?i)(.*bas[iy][no].*ny.*|.*homot.*syn.*|objective syny?onym)")){
                synType = SynonymType.HOMOTYPIC_SYNONYM_OF;
                taxonRelType = getHomoSynTaxonRelType(state);
            }else if (unacceptreason.matches("(?i)(heterotypic|subjective) synonym")){
                synType = SynonymType.HETEROTYPIC_SYNONYM_OF;
//                taxonRelType = getHeteroSynTaxRelType(state);  //do nothing
            }else if (unacceptreason.matches("(?i)part.*\\s+synonym.*")){
                synType = null;
                taxonRelType = TaxonRelationshipType.PRO_PARTE_SYNONYM_FOR();
            }else if (unacceptreason.matches("(?i)(misidentifications?|misapplied .*name|.*misapplication.*|incorrect identification)")){
                //see ErmsTaxonImport.getAcceptedTaxaKeys(); this differs from SQL script
                synType = null;
                taxonRelType = TaxonRelationshipType.MISAPPLIED_NAME_FOR();
            }else if (unacceptreason.matches("(?i)(.*jun.*syn.*|\\(synonym\\)|reverted genus transfer)")){
                handled = true;  //no change
            }else if (unacceptreason.matches("(?i)(currently (placed|held)|sy).*")){
                handled = true;  //no change
            }

            //name relations
            if (unacceptreason.matches("(?i)(.*spell.*|lapsus .*)")){
                nameType = NameRelationshipType.ORTHOGRAPHIC_VARIANT();
            }else if (unacceptreason.matches("(?i).*homon.*")){
                nameType = NameRelationshipType.LATER_HOMONYM();
            }else if (unacceptreason.matches("(?i)(.*genus transfer.*|genus change)")){
                //TODO check with above (synonym relations) reverted genus transfer
                nameType = NameRelationshipType.BASIONYM();
            }else if (unacceptreason.matches("(?i)(original combination|Subsequent combination)")){
                nameType = NameRelationshipType.BASIONYM();
            }else if (unacceptreason.matches("(?i).*bas[iy][no].*ny.*")){
                nameType = NameRelationshipType.BASIONYM();
            }

            if(handled == false && SynonymType.SYNONYM_OF == synType &&
                    getSynTaxonRelType(state).equals(taxonRelType) && nameType == null){
                ((ErmsImportState)state).setUnhandledUnacceptReason(unacceptreason);
                logger.debug("Unaccept reason not yet handled: " + unacceptreason);
            }
        }
//      update Match_RelStat set RelTaxon      =  1 where tu_unacceptreason like '%bas[iy][no]%ny%'
//              update Match_RelStat set RelTaxon   =  1 where tu_unacceptreason = 'original combination' or tu_unacceptreason = 'Subsequent combination'
//              update Match_RelStat set RelTaxon   =  1 where tu_unacceptreason like '%genus transfer%' or tu_unacceptreason = 'genus change'
//              update Match_RelStat set RelTaxon   =  2 where tu_unacceptreason like '%homon%'
//              update Match_RelStat set RelTaxon   = 16 where tu_unacceptreason like '%spell%'
//              update Match_RelStat set RelTaxon   = 16 where tu_unacceptreason like 'lapsus calami%'  --AM: lapsus% should be enough

//    update Match_RelStat set RelTaxon  =  102 where tu_unacceptreason like 'currently placed%'
//            update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason like 'currently held%'
//            update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason like 'sy%' or tu_unacceptreason like '%jun%syn%'
//            update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason = '(synonym)'
//            update Match_RelStat set RelTaxon   =  102 where tu_unacceptreason = 'reverted genus transfer'
//            update Match_RelStat set RelTaxon   =  103 where tu_unacceptreason like 'misapplied%'
//            update Match_RelStat set RelTaxon   =  104 where tu_unacceptreason like 'part% synonym%'
//            update Match_RelStat set RelTaxon   =  106 where tu_unacceptreason = 'heterotypic synonym' or tu_unacceptreason = 'subjective synonym'
//            update Match_RelStat set RelTaxon   =  107 where tu_unacceptreason like '%homot%syn%' or tu_unacceptreason = 'objective synonym'
//            update Match_RelStat set RelTaxon   =  107 where tu_unacceptreason like '%bas[iy][no]%ny%'
//

        if (synType != null){
            result[0]= synType;
        }
        if (taxonRelType != null){
            result[1]= taxonRelType;
        }
        if (nameType != null){
            result[2]= nameType;
        }
//        if (hybridType != null){
//            result[3]= hybridType;
//        }

        return result;
    }

    private TaxonRelationshipType getSynTaxonRelType(DbImportStateBase<?, ?> state) {
        @SuppressWarnings("unchecked")
        TaxonRelationshipType result = state.getCurrentIO().getTaxonRelationshipType(state,
                TaxonRelationshipType.uuidSynonymOfTaxonRelationship,
                "is taxon synonym of",
                "is synonym of relation used by synonym that are of class Taxon as they can not be handled differently",
                "tso",
                "has taxon synonym",
                "is accepted taxon of synonym relation used by synonym that are of class Taxon as they can not be handled differently",
                "has tso",
                null);
        return result;
    }

    private TaxonRelationshipType getHomoSynTaxonRelType(DbImportStateBase<?, ?> state) {
        String labelHomoRel = "is homotypic taxon synonym relationship";
        String inverseLabelHomoRel = "has homotypic taxon synonym relationship";
        @SuppressWarnings("unchecked")
        TaxonRelationshipType result = state.getCurrentIO().getTaxonRelationshipType(
                state, TaxonRelationshipType.uuidHomotypicSynonymTaxonRelationship,
                labelHomoRel, labelHomoRel, "htso",
                inverseLabelHomoRel, inverseLabelHomoRel, "has htso",
                null);
        return result;
    }

    private TaxonRelationshipType getHeteroSynTaxRelType(DbImportStateBase<?, ?> state) {
        String labelHeteroRel = "is heterotypic taxon synonym relationship";
        String inverseLabelHeteroRel = "has heterotypic taxon synonym relationship";
        @SuppressWarnings("unchecked")
        TaxonRelationshipType result = state.getCurrentIO().getTaxonRelationshipType(
                state, TaxonRelationshipType.uuidHeterotypicSynonymTaxonRelationship,
                labelHeteroRel, labelHeteroRel, null,
                inverseLabelHeteroRel, inverseLabelHeteroRel, null,
                null);
        return result;
    }


}
