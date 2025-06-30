/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.pesi.merging.PesiCommandLineMerge;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.common.mapping.out.ExportTransformerBase;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.io.pesi.out.PesiExportBase.PesiSource;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.common.RelationshipTermBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.HybridRelationshipType;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea.FaunaEuropaeaTransformer;

/**
 * @author e.-m.lee
 * @author a.mueller (update)
 * @since 16.02.2010
 */
public final class PesiTransformer extends ExportTransformerBase{

    private static final long serialVersionUID = 1L;

    private static Logger logger = LogManager.getLogger();

	public static final String AUCT_STRING = "auct.";

	//sourceRefUUIDs
	public static final UUID uuidSourceRefEuroMed = UUID.fromString("51b3900c-91e0-4cc5-94f3-577a352ca9c4");
	public static final UUID uuidSourceRefFaunaEuropaea = UUID.fromString("f27a5e67-d065-4b79-8d41-eabd3ae0edd0");
	public static final UUID uuidSourceRefErms = UUID.fromString("7744bc26-f914-42c4-b54a-dd2a030a8bb7");
	public static final UUID uuidSourceRefIndexFungorum = UUID.fromString("8de25d27-7d40-47f4-af3b-59d64935a843");
	public static final UUID uuidSourceRefAuct = UUID.fromString("5f79f96c-c100-4cd8-b78e-2b2dacf09a23");
	public static final UUID uuidSourceRefFaunaEuropaea_fromSql = UUID.fromString("6786d863-75d4-4796-b916-c1c3dff4cb70");

	//other UUIDs
    public static final UUID uuidTaxonValuelessEuroMed = UUID.fromString("f0a9322b-a57e-447b-9a75-a909f2f2a994");
    public static final UUID uuidEuroMedMossesareas = UUID.fromString("ad177547-1f23-48db-8657-ca120668fca6");

	public static final String SOURCE_STR_EM = "E+M";
	public static final String SOURCE_STR_FE = "FaEu";
	public static final String SOURCE_STR_IF = "IF";
	public static final String SOURCE_STR_ERMS = "ERMS";

	// status keys
	private static int QUALITY_STATUS_CHECKED_EDITOR_ERMS_1_1 = 0;
	private static int QUALITY_STATUS_ADD_BY_DBMT= 2;
	private static int QUALITY_STATUS_CHECKED_EDITOR = 3;
	private static int QUALITY_STATUS_EDITED_BY_DBMT = 4;
	private static int QUALITY_STATUS_THEMATIC_EDITOR = 5;

	// marker type
	public static final UUID uuidMarkerGuidIsMissing = UUID.fromString("24e70843-05e2-44db-954b-84df0d23ea20");
	public static final UUID uuidMarkerTypeHasNoLastAction = UUID.fromString("99652d5a-bc92-4251-b57d-0fec4d258ab7");
//	public static final UUID uuidMarkerFossil = UUID.fromString("761ce108-031a-4e07-b444-f8d757070312");


	//extension type uuids
	public static final UUID uuidExtCacheCitation = UUID.fromString("29656168-32d6-4301-9067-d57c63be5c67");
	//public static final UUID expertUserIdUuid = UUID.fromString("e25813d3-c67c-4585-9aa0-970fafde50b4");
	//public static final UUID speciesExpertUserIdUuid = UUID.fromString("6d42abd8-8894-4980-ae07-e918affd4172");
	public static final UUID uuidExtExpertName = BerlinModelTransformer.uuidExpertName;
	public static final UUID uuidExtSpeciesExpertName = BerlinModelTransformer.uuidSpeciesExpertName;
	public static final UUID uuidExtLastActionDate = UUID.fromString("8d0a7d81-bb83-4576-84c3-8c906ef039b2");
	public static final UUID uuidExtLastAction = UUID.fromString("bc20d5bc-6161-4279-9499-89ea26ce5f6a");
	public static final UUID uuidExtTaxComment = UUID.fromString("8041a752-0479-4626-ab1b-b266b751f816");
	public static final UUID uuidExtFauComment = UUID.fromString("054f773a-41c8-4ad5-83e3-981320c1c126");
	public static final UUID uuidExtFauExtraCodes = UUID.fromString("b8c7e77d-9869-4787-bed6-b4b302dbc5f5");

	// References
	private static int REF_ARTICLE_IN_PERIODICAL = 1;
	private static int REF_PART_OF_OTHER = 2;
	public static int REF_BOOK = 3;
	public static int REF_DATABASE = 4;
	private static int REF_INFORMAL = 5;
	private static int REF_NOT_APPLICABLE = 6;
	private static int REF_WEBSITE = 7;
	public static int REF_PUBLISHED_CD = 8;
	public static int REF_JOURNAL = 9;
	public static int REF_UNRESOLVED = 10;
	public static int REF_PUBLICATION = 11;
	public static String REF_STR_UNRESOLVED = "unresolved";

	private static int LANG_UNKNOWN = -99;
	private static int LANG_DUTCH = 11;
    private static int LANG_VALENCIAN = 65;
	private static int LANG_HIGH_ARAGONES = 66;
	private static int LANG_MAJORCAN = 67;


	// NameStatus
    public static UUID uuidNomStatusTemporaryName = UUID.fromString("aa6ada5a-ca21-4fef-b76f-9ae237e9c4ae");

	private static int NAME_ST_NOM_INVAL = 1;
	private static int NAME_ST_NOM_ILLEG = 2;
	private static int NAME_ST_NOM_NUD = 3;
	private static int NAME_ST_NOM_REJ = 4;
	private static int NAME_ST_NOM_REJ_PROP = 5;
	private static int NAME_ST_NOM_UTIQUE_REJ = 6;
	private static int NAME_ST_NOM_UTIQUE_REJ_PROP = 7;
	private static int NAME_ST_NOM_CONS = 8;
	private static int NAME_ST_NOM_CONS_PROP = 9;
	private static int NAME_ST_ORTH_CONS = 10;
	private static int NAME_ST_ORTH_CONS_PROP = 11;
	private static int NAME_ST_NOM_SUPERFL = 12;
	private static int NAME_ST_NOM_AMBIG = 13;
	private static int NAME_ST_NOM_PROVIS = 14;
	private static int NAME_ST_NOM_DUB = 15;
	private static int NAME_ST_NOM_NOV = 16;
	private static int NAME_ST_NOM_CONFUS = 17;
	private static int NAME_ST_NOM_ALTERN = 18;
	private static int NAME_ST_COMB_INVAL = 19;
	private static int NAME_ST_LEGITIMATE = 20; // PESI specific from here
	private static int NAME_ST_COMB_INED = 21;
	private static int NAME_ST_COMB_AND_STAT_INED = 22;
	private static int NAME_ST_NOM_AND_ORTH_CONS = 23;
	private static int NAME_ST_NOM_NOV_INED = 24;
	private static int NAME_ST_SP_NOV_INED = 25;
	private static int NAME_ST_ALTERNATE_REPRESENTATION = 26;
	private static int NAME_ST_TEMPORARY_NAME = 27;
	private static int NAME_ST_SPECIES_INQUIRENDA = 28;
	private static int NAME_ST_COMB_ILLEG = 29;


	// TaxonStatus
	public static int T_STATUS_ACCEPTED = 1;
	public static int T_STATUS_SYNONYM = 2;
	public static int T_STATUS_PARTIAL_SYN = 3;
	public static int T_STATUS_PRO_PARTE_SYN = 4;
	public static int T_STATUS_UNRESOLVED = 5;
	private static int T_STATUS_ORPHANED = 6;
	public static int T_STATUS_UNACCEPTED = 7;
	private static int T_STATUS_NOT_ACCEPTED_VALUELESS = 8;

	// TypeDesginationStatus //	 -> not a table anymore
	private static int TYPE_BY_ORIGINAL_DESIGNATION = 1;
	private static int TYPE_BY_SUBSEQUENT_DESIGNATION = 2;
	private static int TYPE_BY_MONOTYPY = 3;
	private static String TYPE_STR_BY_ORIGINAL_DESIGNATION = "Type by original designation";
	private static String TYPE_STR_BY_SUBSEQUENT_DESIGNATION = "Type by subsequent designation";
	private static String TYPE_STR_BY_MONOTYPY = "Type by monotypy";

	// RelTaxonQualifier
	private static int IS_BASIONYM_FOR = 1;
	private static int IS_LATER_HOMONYM_OF = 2;
	private static int IS_REPLACED_SYNONYM_FOR = 3;
	private static int IS_VALIDATION_OF = 4;
	private static int IS_LATER_VALIDATION_OF = 5;
	private static int IS_TYPE_OF = 6;
	private static int IS_CONSERVED_TYPE_OF = 7;
	private static int IS_REJECTED_TYPE_OF = 8;
	private static int IS_FIRST_PARENT_OF = 9;
	private static int IS_SECOND_PARENT_OF = 10;
	private static int IS_FEMALE_PARENT_OF = 11;
	private static int IS_MALE_PARENT_OF = 12;
	private static int IS_CONSERVED_AGAINST = 13;
	private static int IS_REJECTED_IN_FAVOUR_OF = 14;
	private static int IS_TREATED_AS_LATER_HOMONYM_OF = 15;
	private static int IS_ORTHOGRAPHIC_VARIANT_OF = 16;
	private static int IS_ALTERNATIVE_NAME_FOR = 17;
	private static int HAS_SAME_TYPE_AS = 18;
	public static int IS_ORIGINAL_SPELLING_FOR = 19;
	private static int IS_BLOCKING_NAME_FOR = 20;

	private static int IS_MISSPELLING_FOR = 21;
	private static int HAS_EMENDATION = 22;

    private static int IS_LATER_ISONYM_OF = 31;

	private static int IS_LECTOTYPE_OF = 61;
	private static int TYPE_NOT_DESIGNATED = 62;
	public static int IS_TAXONOMICALLY_INCLUDED_IN = 101;
	public static int IS_SYNONYM_OF = 102;
	private static int IS_MISAPPLIED_NAME_FOR = 103;
	private static int IS_PRO_PARTE_SYNONYM_OF = 104;
	private static int IS_PARTIAL_SYNONYM_OF = 105;
	private static int IS_HETEROTYPIC_SYNONYM_OF = 106;
	private static int IS_HOMOTYPIC_SYNONYM_OF = 107;
	private static int IS_PRO_PARTE_MISAPPLIED_NAME_FOR = 108;
	private static int IS_PRO_PARTE_AND_HOMOTYPIC_SYNONYM_OF = 201;
	private static int IS_PRO_PARTE_AND_HETEROTYPIC_SYNONYM_OF = 202;
	private static int IS_PARTIAL_AND_HOMOTYPIC_SYNONYM_OF = 203;
	private static int IS_PARTIAL_AND_HETEROTYPIC_SYNONYM_OF = 204;
	private static int IS_INFERRED_EPITHET_FOR = 301;
	private static int IS_INFERRED_GENUS_FOR = 302;
	private static int IS_POTENTIAL_COMBINATION_FOR = 303;

	//namespaces
	public static String STR_NAMESPACE_NOMINAL_TAXON = "Nominal taxon from TAX_ID:";
	public static String STR_NAMESPACE_INFERRED_EPITHET = "Inferred epithet from TAX_ID:";
	public static String STR_NAMESPACE_INFERRED_GENUS = "Inferred genus from TAX_ID:";
	public static String STR_NAMESPACE_POTENTIAL_COMBINATION = "Potential combination from TAX_ID:";

	// Kingdoms
	private static final int KINGDOM_NULL = 0;
	private static final int KINGDOM_ANIMALIA = 2;
	private static final int KINGDOM_PLANTAE = 3;
	private static final int KINGDOM_FUNGI = 4;
	private static final int KINGDOM_PROTOZOA = 5;
	private static final int KINGDOM_BACTERIA = 6;
	private static final int KINGDOM_CHROMISTA = 7;

	// Kingdoms
	private static Map<String, Integer> pesiKingdomMap = new HashMap<>();

	//Kingdom title
    private static String KINGDOM_PLANTAE_STRING = "Plantae";
    private static String KINGDOM_FUNGI_STRING = "Fungi";
    private static String KINGDOM_PROTOZOA_STRING = "Protozoa";
    private static String kINGDOM_BACTERIA_STRING = "Bacteria";
    private static String KINGDOM_CHROMISTA_STRING = "Chromista";

	//ranks of all kingdoms
    private static int Kingdom = 10;
    private static int Subkingdom = 20;
    private static int Phylum = 30;  //Phylum and Division is same (#8541) according to ICNAFP
    private static int Division = 30;
    private static int Subphylum = 40;  //See above comment
    private static int Subdivision = 40;
    private static int Class = 60;
    private static int Subclass = 70;
    private static int Order = 100;
    private static int Suborder = 110;
    private static int Family = 140;
    private static int Subfamily = 150;
    private static int Tribe = 160;
    private static int Subtribe = 170;
    private static int Genus = 180;
    private static int Subgenus = 190;
    private static int Species =220;
    private static int Subspecies = 230;
    private static int Variety = 240;
    private static int Forma = 260;

    //ranks of some kingdoms
    private static int Infrakingdom = 25; //2,3,5,7
    private static int Infraphylum = 45;  //2,5,7
    private static int Superclass = 50;   //2,5,6,7
    private static int Infraclass = 80;   //2,5,6,7
    private static int Superorder = 90;   //2,3,5,6,7
    private static int Infraorder = 120;  //2,5,6,7
    private static int Superfamily = 130; //2,5,6,7
    private static int Bot_Section = 200;    //3,4,7
    private static int Bot_Subsection = 210; //3,4,7
    private static int Subvariety = 250;     //2,3,4,7
    private static int Subform = 270;        //2,3,4
    private static int Forma_spec = 275;     //3,4,5,7

    // Animalia Ranks
	private static int Superphylum = 28;
	private static int Parvphylum = 46;
    private static int Megaclass = 49;
    private static int Gigaclass = 48;
    private static int Subterclass = 85;
	private static int Parvorder = 122;
	private static int Animalia_Section = 125;
	private static int Animalia_Subsection = 127;
	private static int Animalia_Epifamily = 135;
	private static int Natio = 235;
	private static int Mutatio = 280;


	// Plantae Ranks
	private static int Series = 212;
	private static int Subseries	= 214;
	private static int Aggregate	= 216;
	private static int Coll_Species = 218;
	private static int Grex = 225;
	private static int Proles = 232;
	private static int Race = 234;
	private static int Convarietas = 236;
	private static int Taxa_infragen = 280;
	private static int Taxa_infraspec = 285;
	private static int Taxa_supragen = 175;

	//NoteCategory
	private static int NoteCategory_description = 1;
	public static int NoteCategory_ecology = 4;
	private static int NoteCategory_phenology	= 5;
	private static int NoteCategory_general_distribution_euromed = 10;
	private static int NoteCategory_general_distribution_world = 11;
	private static int NoteCategory_Common_names = 12;
	private static int NoteCategory_Occurrence = 13;
	private static int NoteCategory_Maps =14;
	private static int NoteCategory_Link_to_maps = 20;
	private static int NoteCategory_Link_to_images = 21;
	public static int NoteCategory_Link_to_taxonomy = 22;
	public static int NoteCategory_Link_to_general_information = 23;
	public static int NoteCategory_undefined_link = 24;
	private static int NoteCategory_Editor_Braces = 249;
	private static int NoteCategory_Editor_Brackets = 250;
	private static int NoteCategory_Editor_Parenthesis = 251;
	private static int NoteCategory_Inedited = 252;
	private static int NoteCategory_Comments_on_editing_process = 253;
	private static int NoteCategory_Publication_date = 254;
	private static int NoteCategory_Morphology = 255;
	private static int NoteCategory_Acknowledgments = 257;
	private static int NoteCategory_Original_publication = 258;
	private static int NoteCategory_Type_locality	= 259;
	private static int NoteCategory_Environment = 260;
	private static int NoteCategory_Spelling = 261;
	private static int NoteCategory_Systematics = 262;
	private static int NoteCategory_Remark = 263;
	private static int NoteCategory_Additional_information = 266;
	private static int NoteCategory_Status = 267;
	private static int NoteCategory_Nomenclature = 268;
	private static int NoteCategory_Homonymy = 269;
	private static int NoteCategory_Taxonomy = 270;
	private static int NoteCategory_Taxonomic_status = 272;
	private static int NoteCategory_Authority	= 273;
	private static int NoteCategory_Identification = 274;
	private static int NoteCategory_Validity = 275;
	private static int NoteCategory_Classification = 276;
	private static int NoteCategory_Distribution = 278;
	private static int NoteCategory_Synonymy = 279;
	private static int NoteCategory_Habitat = 280;
	private static int NoteCategory_Biology = 281;
	private static int NoteCategory_Diagnosis	= 282;
	private static int NoteCategory_Host = 283;
	private static int NoteCategory_Note = 284;
	private static int NoteCategory_Rank = 285;
	private static int NoteCategory_Taxonomic_Remark = 286;
	private static int NoteCategory_Taxonomic_Remarks = 287;
	private static int NoteCategory_Etymology = 288;
    private static int NoteCategory_Type_species = 289;
	private static int NoteCategory_Depth_Range = 290;
	private static int NoteCategory_Grammatical_Gender = 291;
	private static int NoteCategory_Introduced_Species_Remark = 292;
	private static int NoteCategory_Alien_Species = 293;
	private static int NoteCategory_Dimensions = 294;
    private static int NoteCategory_New_Combination = 295;
    private static int NoteCategory_Original_Combination = 296;

	private static int NoteCategory_Conservation_Status= 301;
	private static int NoteCategory_Use = 302;
	private static int NoteCategory_Comments = 303;
    private static int NoteCategory_Diet = 304;
    private static int NoteCategory_Fossil_Range = 305;
    private static int NoteCategory_Original_Description = 306;
    private static int NoteCategory_Reproduction = 307;
    private static int NoteCategory_Specimen = 308;
    private static int NoteCategory_Type_Specimen = 309;
    private static int NoteCategory_Type_Material = 310;
    private static int NoteCategory_Editors_Comment = 311;
    private static int NoteCategory_Syntype = 312;
    private static int NoteCategory_IUCN_Status = 313;

	// FossilStatus
	private static int FOSSILSTATUS_RECENT_ONLY = 1;
	private static int FOSSILSTATUS_FOSSIL_ONLY = 2;
	private static int FOSSILSTATUS_RECENT_FOSSIL = 3;
	public static String STR_FOSSIL_ONLY = "fossil only";  //still used for Index Fungorum

	// SourceUse
	private static int ORIGINAL_DESCRIPTION = 1;
	private static int BASIS_OF_RECORD = 2;
	private static int ADDITIONAL_SOURCE = 3;
	private static int SOURCE_OF_SYNONYMY = 4;
	private static int REDESCRIPTION = 5;
	private static int NEW_COMBINATION_REFERENCE = 6;
	private static int STATUS_SOURCE = 7;
	public static int NOMENCLATURAL_REFERENCE = 8;
	public static String STR_NOMENCLATURAL_REFERENCE = "nomenclatural reference";

	// Area
	private static int AREA_EAST_AEGEAN_ISLANDS = 1;
	private static int AREA_GREEK_EAST_AEGEAN_ISLANDS = 2;
	private static int AREA_TURKISH_EAST_AEGEAN_ISLANDS = 3;
	private static int AREA_ALBANIA = 4;
	private static int AREA_AUSTRIA_WITH_LIECHTENSTEIN = 5;
	private static int AREA_AUSTRIA = 6;
	private static int AREA_LIECHTENSTEIN = 7;
	private static int AREA_AZORES = 8;
	private static int AREA_CORVO = 9;
	private static int AREA_FAIAL = 10;
	private static int AREA_GRACIOSA = 11;
	private static int AREA_SAO_JORGE = 12;
	private static int AREA_FLORES = 13;
	private static int AREA_SAO_MIGUEL = 14;
	private static int AREA_PICO = 15;
	private static int AREA_SANTA_MARIA = 16;
	private static int AREA_TERCEIRA = 17;
	private static int AREA_BELGIUM_WITH_LUXEMBOURG = 18;
	private static int AREA_BELGIUM = 19;
	private static int AREA_LUXEMBOURG = 20;
	private static int AREA_BOSNIA_HERZEGOVINA = 21;
	private static int AREA_BALEARES = 22;
	private static int AREA_IBIZA_WITH_FORMENTERA = 23;
	private static int AREA_MALLORCA = 24;
	private static int AREA_MENORCA = 25;
	private static int AREA_GREAT_BRITAIN = 26;
	private static int AREA_BALTIC_STATES_ESTONIA_LATVIA_LITHUANIA_AND_KALININGRAD_REGION = 27;
	private static int AREA_BULGARIA = 28;
	private static int AREA_BELARUS = 29;
	private static int AREA_CANARY_ISLANDS = 30;
	private static int AREA_GRAN_CANARIA = 31;
	private static int AREA_FUERTEVENTURA_WITH_LOBOS = 32;
	private static int AREA_GOMERA = 33;
	private static int AREA_HIERRO = 34;
	private static int AREA_LANZAROTE_WITH_GRACIOSA = 35;
	private static int AREA_LA_PALMA = 36;
	private static int AREA_TENERIFE = 37;
	private static int AREA_MONTENEGRO = 38;
	private static int AREA_CORSE = 39;
	private static int AREA_CRETE_WITH_KARPATHOS_KASOS_AND_GAVDHOS = 40;
	private static int AREA_CZECH_REPUBLIC = 41;
	private static int AREA_CROATIA = 42;
	private static int AREA_CYPRUS = 43;
	private static int AREA_FORMER_CZECHOSLOVAKIA = 44;
	private static int AREA_DENMARK_WITH_BORNHOLM = 45;
	private static int AREA_ESTONIA = 46;
	private static int AREA_FAROE_ISLANDS = 47;
	private static int AREA_FINLAND_WITH_AHVENANMAA = 48;
	private static int AREA_FRANCE = 49;
	private static int AREA_CHANNEL_ISLANDS = 50;
	private static int AREA_FRENCH_MAINLAND = 51;
	private static int AREA_MONACO = 52;
	private static int AREA_GERMANY = 53;
	private static int AREA_GREECE_WITH_CYCLADES_AND_MORE_ISLANDS = 54;
	private static int AREA_IRELAND = 55;
	private static int AREA_REPUBLIC_OF_IRELAND = 56;
	private static int AREA_NORTHERN_IRELAND = 57;
	private static int AREA_SWITZERLAND = 58;
	private static int AREA_NETHERLANDS = 59;
	private static int AREA_SPAIN = 60;
	private static int AREA_ANDORRA = 61;
	private static int AREA_GIBRALTAR = 62;
	private static int AREA_KINGDOM_OF_SPAIN = 63;
	private static int AREA_HUNGARY = 64;
	private static int AREA_ICELAND = 65;
	private static int AREA_ITALY = 66;
	private static int AREA_ITALIAN_MAINLAND = 67;
	private static int AREA_SAN_MARINO = 68;
	private static int AREA_FORMER_JUGOSLAVIA = 69;
	private static int AREA_LATVIA = 70;
	private static int AREA_LITHUANIA = 71;
	private static int AREA_PORTUGUESE_MAINLAND = 72;
	private static int AREA_MADEIRA_ARCHIPELAGO = 73;
	private static int AREA_DESERTAS = 74;
	private static int AREA_MADEIRA = 75;
	private static int AREA_PORTO_SANTO = 76;
	private static int AREA_THE_FORMER_JUGOSLAV_REPUBLIC_OF_MAKEDONIJA = 77;
	private static int AREA_MOLDOVA = 78;
	private static int AREA_NORWEGIAN_MAINLAND = 79;
	private static int AREA_POLAND = 80;
	private static int AREA_THE_RUSSIAN_FEDERATION = 81;
	private static int AREA_NOVAYA_ZEMLYA_AND_FRANZ_JOSEPH_LAND = 82;
	private static int AREA_CENTRAL_EUROPEAN_RUSSIA = 83;
	private static int AREA_EASTERN_EUROPEAN_RUSSIA = 84;
	private static int AREA_KALININGRAD = 85;
	private static int AREA_NORTHERN_EUROPEAN_RUSSIA = 86;
	private static int AREA_NORTHWEST_EUROPEAN_RUSSIA = 87;
	private static int AREA_SOUTH_EUROPEAN_RUSSIA = 88;
	private static int AREA_ROMANIA = 89;
	private static int AREA_FORMER_USSR = 90;
	private static int AREA_RUSSIA_BALTIC = 91;
	private static int AREA_RUSSIA_CENTRAL = 92;
	private static int AREA_RUSSIA_SOUTHEAST = 93;
	private static int AREA_RUSSIA_NORTHERN = 94;
	private static int AREA_RUSSIA_SOUTHWEST = 95;
	private static int AREA_SARDEGNA = 96;
	private static int AREA_SVALBARD_WITH_BJORNOYA_AND_JAN_MAYEN = 97;
	private static int AREA_SELVAGENS_ISLANDS = 98;
	private static int AREA_SICILY_WITH_MALTA = 99;
	private static int AREA_MALTA = 100;
	private static int AREA_SICILY = 101;
	private static int AREA_SLOVAKIA = 102;
	private static int AREA_SLOVENIA = 103;
	private static int AREA_SERBIA_WITH_MONTENEGRO = 104;
	private static int AREA_SERBIA_INCLUDING_VOJVODINA_AND_WITH_KOSOVO = 105;
	private static int AREA_SWEDEN = 106;
	private static int AREA_EUROPEAN_TUERKIYE = 107;
	private static int AREA_UKRAINE_INCLUDING_CRIMEA = 108;
	private static int AREA_CRIMEA = 109;
	private static int AREA_UKRAINE = 110;
	private static int AREA_GREEK_MAINLAND = 111;
	private static int AREA_CRETE = 112;
	private static int AREA_DODECANESE_ISLANDS = 113;
	private static int AREA_CYCLADES_ISLANDS = 114;
	private static int AREA_NORTH_AEGEAN_ISLANDS = 115;
	private static int AREA_VATICAN_CITY = 116;
	private static int AREA_FRANZ_JOSEF_LAND = 117;
	private static int AREA_NOVAYA_ZEMLYA = 118;
	private static int AREA_AZERBAIJAN_INCLUDING_NAKHICHEVAN = 119;
	private static int AREA_AZERBAIJAN = 120;
	private static int AREA_NAKHICHEVAN = 121;
	private static int AREA_ALGERIA = 122;
	private static int AREA_ARMENIA = 123;
	private static int AREA_CAUCASUS_REGION = 124;
	private static int AREA_EGYPT = 125;
	private static int AREA_GEORGIA = 126;
	private static int AREA_ISRAEL_JORDAN = 127;
	private static int AREA_ISRAEL = 128;
	private static int AREA_JORDAN = 129;
	private static int AREA_LEBANON = 130;
	private static int AREA_LIBYA = 131;
	private static int AREA_LEBANON_SYRIA = 132;
	private static int AREA_MOROCCO = 133;
	private static int AREA_NORTH_CAUCASUS = 134;
	private static int AREA_SINAI = 135;
	private static int AREA_SYRIA = 136;
	private static int AREA_TUNISIA = 137;
	private static int AREA_ASIATIC_TUERKIYE = 138;
	private static int AREA_TUERKIYE = 139;
	private static int AREA_NORTHERN_AFRICA = 140;
	private static int AREA_AFRO_TROPICAL_REGION = 141;
	private static int AREA_AUSTRALIAN_REGION = 142;
	private static int AREA_EAST_PALAEARCTIC = 143;
	private static int AREA_NEARCTIC_REGION = 144;
	private static int AREA_NEOTROPICAL_REGION = 145;
	private static int AREA_NEAR_EAST = 146;
	private static int AREA_ORIENTAL_REGION = 147;
	private static int AREA_EUROPEAN_MARINE_WATERS = 148;
	private static int AREA_MEDITERRANEAN_SEA = 149;
	private static int AREA_WHITE_SEA = 150;
	private static int AREA_NORTH_SEA = 151;
	private static int AREA_BALTIC_SEA = 152;
	private static int AREA_BLACK_SEA = 153;
	private static int AREA_BARENTSZ_SEA = 154;
	private static int AREA_CASPIAN_SEA = 155;
	private static int AREA_PORTUGUESE_EXCLUSIVE_ECONOMIC_ZONE = 156;
	private static int AREA_BELGIAN_EXCLUSIVE_ECONOMIC_ZONE = 157;
	private static int AREA_FRENCH_EXCLUSIVE_ECONOMIC_ZONE = 158;
	private static int AREA_ENGLISH_CHANNEL = 159;
	private static int AREA_ADRIATIC_SEA = 160;
	private static int AREA_BISCAY_BAY = 161;
	private static int AREA_DUTCH_EXCLUSIVE_ECONOMIC_ZONE = 162;
//	private static int AREA_UNITED_KINGDOM_EXCLUSIVE_ECONOMIC_ZONE = 163;
	private static int AREA_SPANISH_EXCLUSIVE_ECONOMIC_ZONE = 164;
	private static int AREA_EGYPTIAN_EXCLUSIVE_ECONOMIC_ZONE = 165;
	private static int AREA_GREEK_EXCLUSIVE_ECONOMIC_ZONE = 166;
//	private static int AREA_TIRRENO_SEA = 167;
	private static int AREA_ICELANDIC_EXCLUSIVE_ECONOMIC_ZONE = 168;
	private static int AREA_IRISH_EXCLUSIVE_ECONOMIC_ZONE = 169;
	private static int AREA_IRISH_SEA = 170;
	private static int AREA_ITALIAN_EXCLUSIVE_ECONOMIC_ZONE = 171;
	private static int AREA_NORWEGIAN_SEA = 172;
	private static int AREA_MOROCCAN_EXCLUSIVE_ECONOMIC_ZONE = 173;
	private static int AREA_NORWEGIAN_EXCLUSIVE_ECONOMIC_ZONE = 174;
	private static int AREA_SKAGERRAK = 175;
	private static int AREA_TUNISIAN_EXCLUSIVE_ECONOMIC_ZONE = 176;
	private static int AREA_WADDEN_SEA = 177;
	private static int AREA_BELT_SEA = 178;
//	private static int AREA_MARMARA_SEA = 179;
	private static int AREA_SEA_OF_AZOV = 180;
	private static int AREA_AEGEAN_SEA = 181;
	private static int AREA_BULGARIAN_EXCLUSIVE_ECONOMIC_ZONE = 182;
	private static int AREA_SOUTH_BALTIC_PROPER = 183;
	private static int AREA_BALTIC_PROPER = 184;
	private static int AREA_NORTH_BALTIC_PROPER = 185;
	private static int AREA_ARCHIPELAGO_SEA = 186;
	private static int AREA_BOTHNIAN_SEA = 187;
	private static int AREA_GERMAN_EXCLUSIVE_ECONOMIC_ZONE = 188;
	private static int AREA_SWEDISH_EXCLUSIVE_ECONOMIC_ZONE = 189;
	private static int AREA_UKRAINIAN_EXCLUSIVE_ECONOMIC_ZONE = 190;
//	private static int AREA_MADEIRAN_EXCLUSIVE_ECONOMIC_ZONE = 191;
	private static int AREA_LEBANESE_EXCLUSIVE_ECONOMIC_ZONE = 192;
	private static int AREA_SPANISH_EXCLUSIVE_ECONOMIC_ZONE_MEDITERRANEAN_PART = 193;
	private static int AREA_ESTONIAN_EXCLUSIVE_ECONOMIC_ZONE = 194;
	private static int AREA_CROATIAN_EXCLUSIVE_ECONOMIC_ZONE = 195;
//	private static int AREA_BALEAR_SEA = 196;
	private static int AREA_TURKISH_EXCLUSIVE_ECONOMIC_ZONE = 197;
	private static int AREA_DANISH_EXCLUSIVE_ECONOMIC_ZONE = 198;
	private static int AREA_TRANSCAUCASUS = 199;

	private static int AREA_GEORGIA_G = 200;
	private static int AREA_ABKHAZIA = 201;
	private static int AREA_ADZARIA = 202;

	private static int AREA_UNITED_KINGDOM = 203;
	private static int AREA_DENMARK_COUNTRY = 204;
	private static int AREA_TUERKIYE_COUNTRY = 205;
	private static int AREA_SPAIN_COUNTRY = 206;
	private static int AREA_GREECE_COUNTRY = 207;
	private static int AREA_PORTUGAL_COUNTRY = 208;
	//continued ERMS areas without variables
	//...
	private static int AREA_WALES = 293;

	//new E+M areas
	private static int AREA_SERBIA = 294;
	private static int AREA_KOSOVO = 295;
	private static int AREA_SWEDEN_AND_FINLAND = 300;

	private static int AREA_Arctic_European_Russia = 301;
	private static int AREA_Central_European_Russia_Mosses = 302;
    private static int AREA_Egypt_and_Sinai = 303;
    private static int AREA_Greece_East_Aegean_Islands = 304;
    private static int AREA_NE_European_Russia = 305;
    private static int AREA_NW_European_Russia = 306;
    private static int AREA_SE_European_Russia = 307;
    private static int AREA_South_Urals = 308;
    private static int AREA_PALESTINE = 309; // Country
    private static int AREA_NORTH_AMERICA = 310;

	//FauEu area UUIDs
    private static UUID uuidAreaAD = UUID.fromString("38dd31d2-8275-4b05-8b85-eb71a390d67f");
    private static UUID uuidAreaAFR = UUID.fromString("c3123386-51a4-42a4-9ff4-b3905b18a83c");//x
    private static UUID uuidAreaAL = UUID.fromString("84c54f15-fc90-44bb-a45d-26b7daa26ceb");
    private static UUID uuidAreaAT = UUID.fromString("d285e4a5-d027-4610-a4ff-f5e6dd16cdc3");
    private static UUID uuidAreaAUS = UUID.fromString("930e6455-de5e-4002-bfc3-45eecd07e4f3");//x
    private static UUID uuidAreaBA = UUID.fromString("946a06ea-200f-409e-bb2f-05e64e55ed41");
    private static UUID uuidAreaBE = UUID.fromString("f914f7fd-d5a3-4165-8409-004f197ae4e9");
    private static UUID uuidAreaBG = UUID.fromString("9586daca-a739-4d28-836e-6d15861375e1");
    private static UUID uuidAreaBY = UUID.fromString("b6f94a56-990d-41da-8e92-abf757afeca1");
    private static UUID uuidAreaCH = UUID.fromString("cccc1da5-6a0e-47c2-900e-e274fd593ff0");
    private static UUID uuidAreaCY = UUID.fromString("78950366-3f15-48bb-80c9-8b24afedcd3c");
    private static UUID uuidAreaCZ = UUID.fromString("d1221dbc-a7c5-4805-b7c8-15ae56557825");
    private static UUID uuidAreaDE = UUID.fromString("6f31223e-9a10-4c97-b482-85d96f195a23");
    private static UUID uuidAreaDK_DEN = UUID.fromString("4ed51fa9-f89a-47c8-a9ce-43d462f74c53");
    private static UUID uuidAreaDK_FOR = UUID.fromString("272874cf-b6f7-4805-8a0c-1a0efbbf0a21");
    private static UUID uuidAreaEE = UUID.fromString("1472705d-c94e-41e8-9482-adb46f201f71");
    private static UUID uuidAreaEPA = UUID.fromString("2f162235-6c5a-4ad4-8ba0-8dca28826dab");//x
    private static UUID uuidAreaES_BAL = UUID.fromString("d5466ee2-38fa-47e8-8109-008b71a2f47b");
    private static UUID uuidAreaES_CNY = UUID.fromString("1f19cdaa-af1e-406c-bb58-25f17ee00228");
    private static UUID uuidAreaES_SPA = UUID.fromString("d3cc7b05-b506-44b7-a49a-77d36974bd5f");
    private static UUID uuidAreaFI = UUID.fromString("93d6e2ba-308b-4d57-851c-94583e139806");
    private static UUID uuidAreaFR_COR = UUID.fromString("f7088b27-805f-46df-916f-eeed47a2e336");
    private static UUID uuidAreaFR_FRA = UUID.fromString("83d2df98-c0b4-4ad8-9a02-7cdfcfa74371");
    private static UUID uuidAreaGB_CI = UUID.fromString("a97807d0-e7e2-4ee0-8567-07f3a72d91bb");
    private static UUID uuidAreaGB_GI = UUID.fromString("4441b42a-7c9f-4a39-89d6-5c9c9dab9c05");
    private static UUID uuidAreaGB_GRB = UUID.fromString("94561875-19a6-4bf4-ade7-340c245ffd67");
    private static UUID uuidAreaGB_NI = UUID.fromString("791be300-6e86-4253-b1ba-aef46ea6bfb9");
    private static UUID uuidAreaGR_AEG = UUID.fromString("efb2f01a-f01e-4da2-ba02-464b0e1687e5");//x
    private static UUID uuidAreaGR_CYC = UUID.fromString("f9ad21b1-c9ed-4062-9086-cba6e6ef353b");//x
    private static UUID uuidAreaGR_DOD = UUID.fromString("7a84feae-9d93-4408-8122-e82b9485fc0f");//x
    private static UUID uuidAreaGR_GRC = UUID.fromString("a392dbac-8671-40e6-95c7-a0d2db8f7d9c");//x
    private static UUID uuidAreaGR_KRI = UUID.fromString("ed8e7bcd-2267-4953-b422-bc2ff45238f7");//x
    private static UUID uuidAreaHR = UUID.fromString("83323e5b-8a62-4784-956a-5f21ddc853b9");
    private static UUID uuidAreaHU = UUID.fromString("ab6985e7-e718-4bcb-bc84-55e563870998");
    private static UUID uuidAreaIE = UUID.fromString("6a623016-2cd7-4391-8bc9-ae8af50060b4");
    private static UUID uuidAreaIS = UUID.fromString("b8469350-7c87-4202-9cb1-5f0ad11ec89c");
    private static UUID uuidAreaIT_ITA = UUID.fromString("43df04d6-90b4-4cf4-8dfc-f60ae921f92c");
    private static UUID uuidAreaIT_SAR = UUID.fromString("f68d9695-238d-4684-901a-f4bbc24813ee");
    private static UUID uuidAreaIT_SI = UUID.fromString("1889a571-d12a-48f1-a49f-112e60c699ef");
    private static UUID uuidAreaLI = UUID.fromString("ebad24b5-559e-46c4-8b0d-87107e05169e");
    private static UUID uuidAreaLT = UUID.fromString("d00df2a8-8d5d-4c36-b3a2-4afc9006be70");
    private static UUID uuidAreaLU = UUID.fromString("65bbbb63-3351-4fda-a8f8-50557bc34e0d");
    private static UUID uuidAreaLV = UUID.fromString("997d97ea-4678-4e01-9a81-518c09da7d7c");
    private static UUID uuidAreaMC = UUID.fromString("0debabad-e7e2-4aad-b959-876d3d85a4b1");
    private static UUID uuidAreaMD = UUID.fromString("7a4ffe59-f8d6-4a58-bb4e-449fbe8420b3");
    private static UUID uuidAreaMK = UUID.fromString("40140714-6214-4338-bc45-f85455f7eaed");
    private static UUID uuidAreaMT = UUID.fromString("b36c2f47-47d5-497f-9235-2faf0d9a6e6c");
    private static UUID uuidAreaNAF = UUID.fromString("e3b2193d-e23a-47a7-9bf5-7f7eebe61afb");//x
    private static UUID uuidAreaNEA = UUID.fromString("d6082e94-7289-45a3-a298-5cb730cfbd7f");//x
    private static UUID uuidAreaNEO = UUID.fromString("7a530ae7-d070-49aa-8496-7b06c4ed735b");//x
    private static UUID uuidAreaNL = UUID.fromString("7b74033f-ada3-4956-87b7-ec9bf0cb4c76");
    private static UUID uuidAreaNO_NOR = UUID.fromString("11b26fc9-3da4-4323-8b03-e960e8b1c914");
    private static UUID uuidAreaNO_SVA = UUID.fromString("9a3fe056-2011-4b02-a416-59de00eaf533");
    private static UUID uuidAreaNRE = UUID.fromString("5e005e59-b313-4c5b-8702-c2f88317db25");//x
    private static UUID uuidAreaORR = UUID.fromString("2047d0dd-12dc-4454-8344-07ad52fa0af0");//x
    private static UUID uuidAreaPL = UUID.fromString("68edc557-dbed-494e-9549-6746f11fb904");
    private static UUID uuidAreaPT_AZO = UUID.fromString("6f5e4eb5-83c8-4baf-99f2-cb6e25d8ac9d");
    private static UUID uuidAreaPT_MDR = UUID.fromString("97bed937-f4ad-4b42-ba0c-8832816fa234");
    private static UUID uuidAreaPT_POR = UUID.fromString("09107b85-2bdb-4534-a51c-8d02cdd71590");
    private static UUID uuidAreaPT_SEL = UUID.fromString("e067287c-50c6-4e54-9859-91aa15aa289a");
    private static UUID uuidAreaRO = UUID.fromString("de3e2ca8-d2ea-4c1f-8a19-a7a0b5daf39d");
    private static UUID uuidAreaRU_FJL = UUID.fromString("2b79434b-df7d-4dc5-996f-1ca4227f6e0e");//x
    private static UUID uuidAreaRU_KGD = UUID.fromString("227a661b-91de-489c-88ed-6b72f78481f0");
    private static UUID uuidAreaRU_NOZ = UUID.fromString("2b26f68a-fdac-41ab-bce0-105b24d7eb26");//x
    private static UUID uuidAreaRU_RUC = UUID.fromString("235748eb-9eef-48a8-9b9a-c4abd00b6244");
    private static UUID uuidAreaRU_RUE = UUID.fromString("5ad5b07e-518f-44ee-ac96-c7608d93e757");
    private static UUID uuidAreaRU_RUN = UUID.fromString("05d35653-9f67-442c-8d99-4a7ae68cf5d1");
    private static UUID uuidAreaRU_RUS = UUID.fromString("98b00cf8-7bd6-4f44-885c-0927bbc1d456");
    private static UUID uuidAreaRU_RUW = UUID.fromString("ae84bc08-957a-4c62-a828-1fb3fc2f0c3e");
    private static UUID uuidAreaSE = UUID.fromString("8ff37027-4832-4ce1-9f25-e21eaf26299c");
    private static UUID uuidAreaSI = UUID.fromString("0b76b448-5564-415c-bc18-727c37e62bf1");
    private static UUID uuidAreaSK = UUID.fromString("e966cd7c-3e68-4153-887b-54920eb6eb47");
    private static UUID uuidAreaSM = UUID.fromString("c40b8a71-c50c-4636-96de-be0bb48ed025");
    private static UUID uuidAreaTR_TUE = UUID.fromString("807317b3-746b-4916-9441-d8086d0d1cb1");
    private static UUID uuidAreaUA = UUID.fromString("2f80440d-cb03-499c-a829-c4ac4c6608d3");//x
    private static UUID uuidAreaVA = UUID.fromString("cd256143-1b74-4a7d-998f-ba0a9c01bb3e");
    private static UUID uuidAreaYU = UUID.fromString("cd80a852-1993-465a-b0e3-8c73218d4d90");


	// OccurrenceStatus
	private static int STATUS_PRESENT = 1;
	public static int STATUS_ABSENT = 2;
	public static int STATUS_NATIVE = 3;
	public static int STATUS_INTRODUCED = 4;
	public static int STATUS_NATURALISED = 5;
	private static int STATUS_INVASIVE = 6;
	public static int STATUS_MANAGED = 7;
	public static int STATUS_DOUBTFUL = 8;

	private final Map<String, Integer> tdwgKeyMap = new HashMap<>();
	private final Map<Integer, String> areaCacheMap = new HashMap<>();
	private final Map<Integer, String> languageCacheMap  = new HashMap<>();
	private static final Map<String,Integer> languageCodeToKeyMap = new HashMap<>();

	private final Map<Integer, String> featureCacheMap  = new HashMap<>();
	private final Map<Integer, String> nameStatusCacheMap  = new HashMap<>();
	private final Map<Integer, String> qualityStatusCacheMap  = new HashMap<>();
	private final Map<Integer, String> taxonStatusCacheMap  = new HashMap<>();
	private final Map<Integer, String> taxRelQualifierCacheMap  = new HashMap<>();
	private final Map<Integer, String> taxRelZooQualifierCacheMap  = new HashMap<>();
	private final Map<Integer, String> sourceUseCacheMap  = new HashMap<>();
	private final Map<Integer, String> fossilStatusCacheMap  = new HashMap<>();
	private final Map<Integer, String> typeDesigStatusCacheMap  = new HashMap<>();
	private final Map<Integer, String> sourceCategoryCacheMap  = new HashMap<>();
	private final Map<Integer, String> occurrenceStatusCacheMap  = new HashMap<>();
	private final Map<Integer, Map<Integer, String>> rankCacheMap  = new  HashMap<>();
	private final Map<Integer, Map<Integer, String>> rankAbbrevCacheMap  = new  HashMap<>();

    private final Source destination;

    public PesiTransformer(Source destination) {
        this.destination = destination;
        fillMaps();
    }
	private void fillMaps() {
		try {
		    //TDWG
			String sql = " SELECT AreaId, AreaName, AreaTdwgCode, AreaEmCode, AreaFaEuCode FROM Area";
			ResultSet rs = destination.getResultSet(sql);
			while (rs.next()){
				String tdwg = rs.getString("AreaTdwgCode");
				Integer id = rs.getInt("AreaId");
				String label = rs.getString("AreaName");

				if (StringUtils.isNotBlank(tdwg)){
					this.tdwgKeyMap.put(tdwg, id);
				}
				this.areaCacheMap.put(id, label);
			}

			//rankCache
			sql = " SELECT KingdomId, RankId, Rank, RankAbbrev, Kingdom  FROM Rank";
			rs = destination.getResultSet(sql);
			while (rs.next()){
				String rank = rs.getString("Rank");
				String abbrev = rs.getString("RankAbbrev");
				Integer rankId = rs.getInt("RankId");
				Integer kingdomId = rs.getInt("KingdomId");
				String kingdom = rs.getString("Kingdom");

				//rank str
				Map<Integer, String> kingdomMap = rankCacheMap.get(kingdomId);
				if (kingdomMap == null){
					kingdomMap = new HashMap<>();
					rankCacheMap.put(kingdomId, kingdomMap);
				}
				kingdomMap.put(rankId, rank);

				if (rank.equals("Kingdom")){
				    pesiKingdomMap.put(kingdom, kingdomId);
				}

				//rank abbrev
				Map<Integer, String> kingdomAbbrevMap = rankAbbrevCacheMap.get(kingdomId);
				if (kingdomAbbrevMap == null){
					kingdomAbbrevMap = new HashMap<>();
					rankAbbrevCacheMap.put(kingdomId, kingdomAbbrevMap);
				}
				if (StringUtils.isNotBlank(abbrev)){
					kingdomAbbrevMap.put(rankId, abbrev);
				}
			}

			//languageCache
			fillSingleMap(languageCacheMap, "Language");

			//feature / note category
			fillSingleMap(featureCacheMap, "NoteCategory");

			//nameStatusCache
			fillSingleMap(nameStatusCacheMap, "NameStatus", "NomStatus");

			//qualityStatusCache
			fillSingleMap(qualityStatusCacheMap, "QualityStatus");

			//taxonStatusCache
			fillSingleMap(taxonStatusCacheMap, "TaxonStatus", "Status");

			//sourceUse
			fillSingleMap(sourceUseCacheMap, "SourceUse");

			//fossil status
			fillSingleMap(fossilStatusCacheMap, "FossilStatus");

			//fossil status
			fillSingleMap(typeDesigStatusCacheMap, "FossilStatus");

			//fossil status
			fillSingleMap(occurrenceStatusCacheMap, "OccurrenceStatus");

			//source category
			fillSingleMap(sourceCategoryCacheMap, "SourceCategory", "Category", "SourceCategoryId");

			//RelTaxonQualifier
			sql = " SELECT QualifierId, Qualifier, ZoologQualifier FROM RelTaxonQualifier ";
			rs = destination.getResultSet(sql);
			while (rs.next()){
				Integer key = rs.getInt("QualifierId");
				String cache = rs.getString("Qualifier");
				if (StringUtils.isNotBlank(cache)){
					this.taxRelQualifierCacheMap.put(key, cache);
				}
				String zoologCache = rs.getString("ZoologQualifier");
				if (StringUtils.isNotBlank(zoologCache)){
					this.taxRelZooQualifierCacheMap.put(key, zoologCache);
				}
			}

			//language code map
			sql = " SELECT LanguageId, Language, ISO639_1, ISO639_2, ISO639_3 FROM Language";
			rs = destination.getResultSet(sql);
			while (rs.next()){
                Integer id = rs.getInt("LanguageId");
                Integer oldId;
                String iso639_1 = rs.getString("ISO639_1");
                if (StringUtils.isNotBlank(iso639_1)){
                    oldId = languageCodeToKeyMap.put(iso639_1, id);
                    checkOldId(id, oldId, iso639_1);
                }
                String iso639_2 = rs.getString("ISO639_2");
                if (StringUtils.isNotBlank(iso639_2)){
                    oldId = languageCodeToKeyMap.put(iso639_2, id);
                    checkOldId(id, oldId, iso639_1);
                }
                String iso639_3 = rs.getString("ISO639_3");
                if (StringUtils.isNotBlank(iso639_3)){
                    oldId = languageCodeToKeyMap.put(iso639_3, id);
                    checkOldId(id, oldId, iso639_1);
                }
            }
			rs = null;
		} catch (Exception e) {
			logger.error("Exception when trying to read area map", e);
			e.printStackTrace();
		}
	}

    private void checkOldId(Integer id, Integer oldId, String isoCode) {
        if (oldId != null && !oldId.equals(id)){
            logger.warn("Language code " + isoCode + " exists for >1 language IDs. This should not happen.");
        }
    }

    private void fillSingleMap(Map<Integer, String> map, String tableName) throws SQLException {
		fillSingleMap(map, tableName, tableName,  tableName + "Id");
	}

	private void fillSingleMap(Map<Integer, String> map, String tableName, String attr) throws SQLException {
			fillSingleMap(map, tableName, attr,  attr + "Id");
	}

	private void fillSingleMap(Map<Integer, String> map, String tableName, String attr, String idAttr) throws SQLException {
		String sql;
		ResultSet rs;
		sql = " SELECT %s, %s FROM %s ";
		sql = String.format(sql, idAttr, attr, tableName);
		rs = destination.getResultSet(sql);
		while (rs.next()){
			Integer key = rs.getInt(idAttr);
			String cache = rs.getString(attr);
			if (StringUtils.isNotBlank(cache)){
				map.put(key, cache);
			}
		}
	}

	/**
	 * Converts the databaseString to its abbreviation if its known.
	 * Otherwise the databaseString is returned.
	 */
	public static String databaseString2Abbreviation(String databaseString) {
		String result = databaseString;
		if (databaseString.equals("Fauna Europaea database")) {
			result = "FaEu";
		}
		return result;
	}

	/**
	 * Returns the OccurrenceStatusId for a given PresenceAbsenceTerm.
	 */
	public static Integer presenceAbsenceTerm2OccurrenceStatusId(PresenceAbsenceTerm term) {
		Integer result = null;
		if (term == null){
			return null;
		//present
		}else if (term.isInstanceOf(PresenceAbsenceTerm.class)) {
			PresenceAbsenceTerm presenceTerm = CdmBase.deproxy(term, PresenceAbsenceTerm.class);
			if (presenceTerm.equals(PresenceAbsenceTerm.PRESENT()) ||
					presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED_DOUBTFULLY_INTRODUCED()) ||
					presenceTerm.equals(PresenceAbsenceTerm.NATIVE_DOUBTFULLY_NATIVE())) {
				result = STATUS_PRESENT;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.NATIVE())) {
				result = STATUS_NATIVE;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED()) ||
					presenceTerm.equals(PresenceAbsenceTerm.CASUAL()) ||
					presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED_UNCERTAIN_DEGREE_OF_NATURALISATION())) {
				result = STATUS_INTRODUCED;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.NATURALISED())
					|| presenceTerm.equals(PresenceAbsenceTerm.NATURALISED())) {
				result = STATUS_NATURALISED;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.INVASIVE())) {
				result = STATUS_INVASIVE;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.CULTIVATED())) {
				result = STATUS_MANAGED;
			} else if (presenceTerm.equals(PresenceAbsenceTerm.PRESENT_DOUBTFULLY())||
					presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED_PRESENCE_QUESTIONABLE()) ||
					presenceTerm.equals(PresenceAbsenceTerm.NATIVE_PRESENCE_QUESTIONABLE() )) {
				result = STATUS_DOUBTFUL;
			//absent
			}else if (presenceTerm.equals(PresenceAbsenceTerm.ABSENT()) || presenceTerm.equals(PresenceAbsenceTerm.NATIVE_FORMERLY_NATIVE()) ||
					presenceTerm.equals(PresenceAbsenceTerm.CULTIVATED_REPORTED_IN_ERROR()) || presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED_REPORTED_IN_ERROR()) ||
					presenceTerm.equals(PresenceAbsenceTerm.INTRODUCED_FORMERLY_INTRODUCED()) || presenceTerm.equals(PresenceAbsenceTerm.NATIVE_REPORTED_IN_ERROR() ) ) {
				result = STATUS_ABSENT;
			} else {
				logger.error("PresenceAbsenceTerm could not be translated to datawarehouse occurrence status id: " + presenceTerm.getLabel());
			}
		}
		return result;
	}

	@Override
	public String getCacheByPresenceAbsenceTerm(PresenceAbsenceTerm status) throws UndefinedTransformerMethodException {
		if (status == null){
			return null;
		}else{
			return this.occurrenceStatusCacheMap.get(getKeyByPresenceAbsenceTerm(status));
		}
	}

	@Override
	public Object getKeyByPresenceAbsenceTerm(PresenceAbsenceTerm status) throws UndefinedTransformerMethodException {
		return presenceAbsenceTerm2OccurrenceStatusId(status);
	}

	@Override
	public String getCacheByNamedArea(NamedArea namedArea) throws UndefinedTransformerMethodException {
		NamedArea area = CdmBase.deproxy(namedArea);
		if (area == null){
			return null;
		}else{
			return this.areaCacheMap.get(getKeyByNamedArea(area));
		}
	}

	@Override
	public Object getKeyByNamedArea(NamedArea area) throws UndefinedTransformerMethodException {
		NamedArea namedArea = CdmBase.deproxy(area);

		if (area == null) {
			return null;
		//TDWG areas
		} else if (area.getVocabulary().getUuid().equals(NamedArea.uuidTdwgAreaVocabulary)) {
			String abbrevLabel = namedArea.getRepresentation(Language.DEFAULT()).getAbbreviatedLabel();
			Integer result = this.tdwgKeyMap.get(abbrevLabel);
			if (result == null){
				logger.warn("Unknown TDWGArea: " + area.getTitleCache());
			}
			return result;
		//countries
		}else if (namedArea.isInstanceOf(Country.class)){
			if (namedArea.equals(Country.UKRAINE())) { return AREA_UKRAINE_INCLUDING_CRIMEA; }
			else if (namedArea.equals(Country.AZERBAIJANREPUBLICOF())) { return AREA_AZERBAIJAN_INCLUDING_NAKHICHEVAN; }
			else if (namedArea.equals(Country.GEORGIA())) { return AREA_GEORGIA; }
			else if (namedArea.equals(Country.RUSSIANFEDERATION())) { return AREA_THE_RUSSIAN_FEDERATION; }
			else if (namedArea.equals(Country.UNITEDKINGDOMOFGREATBRITAINANDNORTHERNIRELAND())) { return AREA_UNITED_KINGDOM; }
			else if (namedArea.equals(Country.DENMARKKINGDOMOF())) { return AREA_DENMARK_COUNTRY; }
			else if (namedArea.equals(Country.TUERKIYEREPUBLICOF())) { return AREA_TUERKIYE_COUNTRY; }
			else if (namedArea.equals(Country.FRANCE())) { return AREA_FRANCE; }
			else if (namedArea.equals(Country.PALESTINIANTERRITORYOCCUPIED())) { return AREA_PALESTINE; }

            else {
				logger.warn("Unknown Country: " + area.getTitleCache());
			}
		}else if (area.getVocabulary().getUuid().equals(NamedArea.uuidContinentVocabulary)){
		    if (namedArea.equals(NamedArea.NORTH_AMERICA())) { return AREA_NORTH_AMERICA; }

		}else if (area.getVocabulary().getUuid().equals(BerlinModelTransformer.uuidVocEuroMedAreas)){
			if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEM)) {
//				logger.warn("E+M area not available in PESI");
				return null;
			}
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEUR)) { logger.warn("EUR area not available in PESI"); return null; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAb)) { return AREA_AZERBAIJAN_INCLUDING_NAKHICHEVAN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAb_A)) { return AREA_AZERBAIJAN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAb_N)) { return AREA_NAKHICHEVAN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAE)) { return AREA_EAST_AEGEAN_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAE_G)) { return AREA_GREEK_EAST_AEGEAN_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAE_T)) { return AREA_TURKISH_EAST_AEGEAN_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAg)) { return AREA_ALGERIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAl)) { return AREA_ALBANIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAr)) { return AREA_ARMENIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAu)) { return AREA_AUSTRIA_WITH_LIECHTENSTEIN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAu_A)) { return AREA_AUSTRIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAu_L)) { return AREA_LIECHTENSTEIN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz)) { return AREA_AZORES; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_C)) { return AREA_CORVO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_F)) { return AREA_FAIAL; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_G)) { return AREA_GRACIOSA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_J)) { return AREA_SAO_JORGE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_L)) { return AREA_FLORES; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_M)) { return AREA_SAO_MIGUEL; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_P)) { return AREA_PICO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_S)) { return AREA_SANTA_MARIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidAz_T)) { return AREA_TERCEIRA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBe)) { return AREA_BELGIUM_WITH_LUXEMBOURG; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBe_B)) { return AREA_BELGIUM; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBe_L)) { return AREA_LUXEMBOURG; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBH)) { return AREA_BOSNIA_HERZEGOVINA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBl)) { return AREA_BALEARES; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBl_I)) { return AREA_IBIZA_WITH_FORMENTERA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBl_M)) { return AREA_MALLORCA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBl_N)) { return AREA_MENORCA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBr)) { return AREA_GREAT_BRITAIN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBt)) { return AREA_BALTIC_STATES_ESTONIA_LATVIA_LITHUANIA_AND_KALININGRAD_REGION; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBu)) { return AREA_BULGARIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidBy)) { return AREA_BELARUS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa)) { return AREA_CANARY_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_C)) { return AREA_GRAN_CANARIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_F)) { return AREA_FUERTEVENTURA_WITH_LOBOS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_G)) { return AREA_GOMERA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_H)) { return AREA_HIERRO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_L)) { return AREA_LANZAROTE_WITH_GRACIOSA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_P)) { return AREA_LA_PALMA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCa_T)) { return AREA_TENERIFE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCc)) { return AREA_CAUCASUS_REGION; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCg)) { return AREA_MONTENEGRO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCo)) { return AREA_CORSE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCr)) { return AREA_CRETE_WITH_KARPATHOS_KASOS_AND_GAVDHOS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCs)) { return AREA_CZECH_REPUBLIC; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCt)) { return AREA_CROATIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCy)) { return AREA_CYPRUS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidCz)) { return AREA_FORMER_CZECHOSLOVAKIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidDa)) { return AREA_DENMARK_WITH_BORNHOLM; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEg)) { return AREA_EGYPT; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEs)) { return AREA_ESTONIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidFa)) { return AREA_FAROE_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidFe)) { return AREA_FINLAND_WITH_AHVENANMAA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGa)) { return AREA_FRANCE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGa_C)) { return AREA_CHANNEL_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGa_F)) { return AREA_FRENCH_MAINLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGa_M)) { return AREA_MONACO; }

			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGe)) { return AREA_GERMANY; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGg)) { return AREA_GEORGIA; }

			//old
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGg_G)) { return AREA_GEORGIA_G; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGg_A)) { return AREA_ABKHAZIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGg_D)) { return AREA_ADZARIA; }
			//end old

			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGr)) { return AREA_GREECE_WITH_CYCLADES_AND_MORE_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHb)) { return AREA_IRELAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHb_E)) { return AREA_REPUBLIC_OF_IRELAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHb_N)) { return AREA_NORTHERN_IRELAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHe)) { return AREA_SWITZERLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHo)) { return AREA_NETHERLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHs)) { return AREA_SPAIN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHs_A)) { return AREA_ANDORRA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHs_G)) { return AREA_GIBRALTAR; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHs_S)) { return AREA_KINGDOM_OF_SPAIN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidHu)) { return AREA_HUNGARY; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIJ)) { return AREA_ISRAEL_JORDAN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIr)) { return AREA_ISRAEL; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIs)) { return AREA_ICELAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt)) { return AREA_ITALY; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt_I)) { return AREA_ITALIAN_MAINLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt_S)) { return AREA_SAN_MARINO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidJo)) { return AREA_JORDAN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidJu)) { return AREA_FORMER_JUGOSLAVIA; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidKo)) { return AREA_KOSOVO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLa)) { return AREA_LATVIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLe)) { return AREA_LEBANON; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLi)) { return AREA_LIBYA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLS)) { return AREA_LEBANON_SYRIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLt)) { return AREA_LITHUANIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidLu)) { return AREA_PORTUGUESE_MAINLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMa)) { return AREA_MOROCCO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMd)) { return AREA_MADEIRA_ARCHIPELAGO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMd_D)) { return AREA_DESERTAS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMd_M)) { return AREA_MADEIRA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMd_P)) { return AREA_PORTO_SANTO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMk)) { return AREA_THE_FORMER_JUGOSLAV_REPUBLIC_OF_MAKEDONIJA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidMo)) { return AREA_MOLDOVA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidNo)) { return AREA_NORWEGIAN_MAINLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidPo)) { return AREA_POLAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf)) { return AREA_THE_RUSSIAN_FEDERATION; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_A)) { return AREA_NOVAYA_ZEMLYA_AND_FRANZ_JOSEPH_LAND; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_C)) { return AREA_CENTRAL_EUROPEAN_RUSSIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_CS)) { return AREA_NORTH_CAUCASUS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_E)) { return AREA_EASTERN_EUROPEAN_RUSSIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_K)) { return AREA_KALININGRAD; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_N)) { return AREA_NORTHERN_EUROPEAN_RUSSIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_NW)) { return AREA_NORTHWEST_EUROPEAN_RUSSIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_S)) { return AREA_SOUTH_EUROPEAN_RUSSIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRm)) { return AREA_ROMANIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs)) { return AREA_FORMER_USSR; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_B)) { return AREA_RUSSIA_BALTIC; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_C)) { return AREA_RUSSIA_CENTRAL; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_E)) { return AREA_RUSSIA_SOUTHEAST; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_K)) { return AREA_CRIMEA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_N)) { return AREA_RUSSIA_NORTHERN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRs_W)) { return AREA_RUSSIA_SOUTHWEST; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSa)) { return AREA_SARDEGNA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSb)) { return AREA_SVALBARD_WITH_BJORNOYA_AND_JAN_MAYEN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSe)) { return AREA_SERBIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSg)) { return AREA_SELVAGENS_ISLANDS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSi)) { return AREA_SICILY_WITH_MALTA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSi_M)) { return AREA_MALTA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSi_S)) { return AREA_SICILY; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSk)) { return AREA_SLOVAKIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSl)) { return AREA_SLOVENIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSM)) { return AREA_SERBIA_WITH_MONTENEGRO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSn)) { return AREA_SINAI; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSr)) { return AREA_SERBIA_INCLUDING_VOJVODINA_AND_WITH_KOSOVO; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSu)) { return AREA_SWEDEN; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidSy)) { return AREA_SYRIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidTcs)) { return AREA_TRANSCAUCASUS; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidTn)) { return AREA_TUNISIA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidTu)) { return AREA_TUERKIYE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidTu_A)) { return AREA_ASIATIC_TUERKIYE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidTu_E)) { return AREA_EUROPEAN_TUERKIYE; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidUk)) { return AREA_UKRAINE_INCLUDING_CRIMEA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidUk_K)) { return AREA_CRIMEA; }
			else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidUk_U)) { return AREA_UKRAINE; }
			else {
                logger.warn("Unknown EuroMed distribution area: " + area.getTitleCache());
            }
	    }else if (area.getVocabulary().getUuid().equals(PesiTransformer.uuidEuroMedMossesareas)){
            if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEUR)) {logger.warn("EUR area not available in PESI"); return null; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_A2)) { return AREA_Arctic_European_Russia; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_C2)) { return AREA_Central_European_Russia_Mosses; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidEgSn)) { return AREA_Egypt_and_Sinai; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidGr_AE_G)) { return AREA_Greece_East_Aegean_Islands; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt_I)) { return AREA_ITALIAN_MAINLAND; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_NE)) { return AREA_NE_European_Russia; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_NW2)) { return AREA_NW_European_Russia; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt_S)) { return AREA_SAN_MARINO; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_SE)) { return AREA_SE_European_Russia; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidRf_SU)) { return AREA_South_Urals; }
            else if (namedArea.getUuid().equals(BerlinModelTransformer.uuidIt_V)) { return AREA_VATICAN_CITY; }

        }else if (area.getVocabulary().getUuid().equals(BerlinModelTransformer.uuidVocEuroMedCommonNameAreas)){
            UUID uuidArea = namedArea.getUuid();
            if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameAlbania)) { return AREA_ALBANIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameAndorra)) { return AREA_ANDORRA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameArmenia)) { return AREA_ARMENIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameAustria)) { return AREA_AUSTRIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameAzerbaijan)) { return AREA_AZERBAIJAN; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameAzores)) { return AREA_AZORES; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameBaleares)) { return AREA_BALEARES; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameBelarus)) { return AREA_BELARUS; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameBelgium)) { return AREA_BELGIUM; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameBulgaria)) { return AREA_BULGARIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCanaryIs)) { return AREA_CANARY_ISLANDS; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCorse)) { return AREA_CORSE; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCrete)) { return AREA_CRETE; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCrimea)) { return AREA_CRIMEA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCroatia)) { return AREA_CROATIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameCzechRepublic)) { return AREA_CZECH_REPUBLIC; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameDenmark)) { return AREA_DENMARK_COUNTRY; } //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameEastAegeanIslands)) { return AREA_EAST_AEGEAN_ISLANDS; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameEstonia)) { return AREA_ESTONIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameEstonia)) { return AREA_ESTONIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameFaroer)) { return AREA_FAROE_ISLANDS; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameFinland)) { return AREA_FINLAND_WITH_AHVENANMAA; }  //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameFinlandWithAhvenanmaa)) { return AREA_FINLAND_WITH_AHVENANMAA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameFrance)) { return AREA_FRANCE; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameGeorgia)) { return AREA_GEORGIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameGermany)) { return AREA_GERMANY; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameGreatBritain)) { return AREA_GREAT_BRITAIN; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameGreece)) { return AREA_GREECE_COUNTRY; } //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameHungary)) { return AREA_HUNGARY; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameIbizaWithFormentera)) { return AREA_IBIZA_WITH_FORMENTERA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameIceland)) { return AREA_ICELAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameIreland)) { return AREA_IRELAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameIsrael)) { return AREA_ISRAEL; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameItaly)) { return AREA_ITALY; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameLaPalma)) { return AREA_LA_PALMA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameLativa)) { return AREA_LATVIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameLebanon)) { return AREA_LEBANON; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameLibya)) { return AREA_LIBYA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameLithuania)) { return AREA_LITHUANIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMadeira)) { return AREA_MADEIRA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMallorca)) { return AREA_MALLORCA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMalta)) { return AREA_MALTA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMenorca)) { return AREA_MENORCA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMoldova)) { return AREA_MOLDOVA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameMorocco)) { return AREA_MOROCCO; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameNetherlands)) { return AREA_NETHERLANDS; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameNorway)) { return AREA_NORWEGIAN_MAINLAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNamePoland)) { return AREA_POLAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNamePortugal)) { return AREA_PORTUGAL_COUNTRY; }  //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameRomania)) { return AREA_ROMANIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameRussiaCentral)) { return AREA_RUSSIA_CENTRAL; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameRussianFederation)) { return AREA_THE_RUSSIAN_FEDERATION; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSardegna)) { return AREA_SARDEGNA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSerbiaMontenegro)) { return AREA_SERBIA_WITH_MONTENEGRO; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSlovakia)) { return AREA_SLOVAKIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSlovenia)) { return AREA_SLOVENIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSouthEuropeanRussia)) { return AREA_SOUTH_EUROPEAN_RUSSIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSpain)) { return AREA_SPAIN; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSweden)) { return AREA_SWEDEN; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSwedenAndFinland)) { return AREA_SWEDEN_AND_FINLAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSwitzerland)) { return AREA_SWITZERLAND; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameSyria)) { return AREA_SYRIA; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameTenerife)) { return AREA_TENERIFE; }
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameTuerkiye)) { return AREA_TUERKIYE; }  //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameUkraine)) { return AREA_UKRAINE; }  //??
            else if (uuidArea.equals(BerlinModelTransformer.uuidEMAreaCommonNameWales)) { return AREA_WALES; }
            else {
                logger.warn("Unknown EuroMed common name area: " + area.getTitleCache());
            }

        }else if (area.getVocabulary().getUuid().equals(FaunaEuropaeaTransformer.uuidFauEuArea)){
            UUID uuidArea = namedArea.getUuid();
            if (uuidArea.equals(uuidAreaAD)) { return AREA_ANDORRA; }
            else if (uuidArea.equals(uuidAreaAFR)) { return AREA_AFRO_TROPICAL_REGION; }
            else if (uuidArea.equals(uuidAreaAL)) { return AREA_ALBANIA; }
            else if (uuidArea.equals(uuidAreaAT)) { return AREA_AUSTRIA; }
			else if (uuidArea.equals(uuidAreaAUS)) { return AREA_AUSTRALIAN_REGION; }
			else if (uuidArea.equals(uuidAreaBA)) { return AREA_BOSNIA_HERZEGOVINA; }
			else if (uuidArea.equals(uuidAreaBE)) { return AREA_BELGIUM; }
			else if (uuidArea.equals(uuidAreaBG)) { return AREA_BULGARIA; }
			else if (uuidArea.equals(uuidAreaBY)) { return AREA_BELARUS; }
			else if (uuidArea.equals(uuidAreaCH)) { return AREA_SWITZERLAND; }
			else if (uuidArea.equals(uuidAreaCY)) { return AREA_CYPRUS; }
			else if (uuidArea.equals(uuidAreaCZ)) { return AREA_CZECH_REPUBLIC; }
			else if (uuidArea.equals(uuidAreaDE)) { return AREA_GERMANY; }
			else if (uuidArea.equals(uuidAreaDK_DEN)) { return AREA_DENMARK_WITH_BORNHOLM; }
			else if (uuidArea.equals(uuidAreaDK_FOR)) { return AREA_FAROE_ISLANDS; }
			else if (uuidArea.equals(uuidAreaEE)) { return AREA_ESTONIA; }
			else if (uuidArea.equals(uuidAreaEPA)) { return AREA_EAST_PALAEARCTIC; }
			else if (uuidArea.equals(uuidAreaES_BAL)) { return AREA_BALEARES; }
			else if (uuidArea.equals(uuidAreaES_CNY)) { return AREA_CANARY_ISLANDS; }
			else if (uuidArea.equals(uuidAreaES_SPA)) { return AREA_KINGDOM_OF_SPAIN; }
			else if (uuidArea.equals(uuidAreaFI)) { return AREA_FINLAND_WITH_AHVENANMAA; }
			else if (uuidArea.equals(uuidAreaFR_COR)) { return AREA_CORSE; }
			else if (uuidArea.equals(uuidAreaFR_FRA)) { return AREA_FRENCH_MAINLAND; }
			else if (uuidArea.equals(uuidAreaGB_CI)) { return AREA_CHANNEL_ISLANDS; }
			else if (uuidArea.equals(uuidAreaGB_GI)) { return AREA_GIBRALTAR; }
			else if (uuidArea.equals(uuidAreaGB_GRB)) { return AREA_GREAT_BRITAIN; }
			else if (uuidArea.equals(uuidAreaGB_NI)) { return AREA_NORTHERN_IRELAND; }
			else if (uuidArea.equals(uuidAreaGR_AEG)) { return AREA_NORTH_AEGEAN_ISLANDS; }
			else if (uuidArea.equals(uuidAreaGR_CYC)) { return AREA_CYCLADES_ISLANDS; }
			else if (uuidArea.equals(uuidAreaGR_DOD)) { return AREA_DODECANESE_ISLANDS; }
			else if (uuidArea.equals(uuidAreaGR_KRI)) { return AREA_CRETE; }
			else if (uuidArea.equals(uuidAreaGR_GRC)) { return AREA_GREEK_MAINLAND; }
			else if (uuidArea.equals(uuidAreaHR)) { return AREA_CROATIA; }
			else if (uuidArea.equals(uuidAreaHU)) { return AREA_HUNGARY; }
			else if (uuidArea.equals(uuidAreaIE)) { return AREA_REPUBLIC_OF_IRELAND; }
			else if (uuidArea.equals(uuidAreaIS)) { return AREA_ICELAND; }
			else if (uuidArea.equals(uuidAreaIT_ITA)) { return AREA_ITALIAN_MAINLAND; }
			else if (uuidArea.equals(uuidAreaIT_SAR)) { return AREA_SARDEGNA; }
			else if (uuidArea.equals(uuidAreaIT_SI)) { return AREA_SICILY; }
			else if (uuidArea.equals(uuidAreaLI)) { return AREA_LIECHTENSTEIN; }
			else if (uuidArea.equals(uuidAreaLT)) { return AREA_LITHUANIA; }
			else if (uuidArea.equals(uuidAreaLU)) { return AREA_LUXEMBOURG; }
			else if (uuidArea.equals(uuidAreaLV)) { return AREA_LATVIA; }
			else if (uuidArea.equals(uuidAreaMC)) { return AREA_MONACO; }
			else if (uuidArea.equals(uuidAreaMD)) { return AREA_MOLDOVA; }
			else if (uuidArea.equals(uuidAreaMK)) { return AREA_THE_FORMER_JUGOSLAV_REPUBLIC_OF_MAKEDONIJA; }
			else if (uuidArea.equals(uuidAreaMT)) { return AREA_MALTA; }
			else if (uuidArea.equals(uuidAreaNAF)) { return AREA_NORTHERN_AFRICA; }
			else if (uuidArea.equals(uuidAreaNEA)) { return AREA_NEARCTIC_REGION; }
			else if (uuidArea.equals(uuidAreaNEO)) { return AREA_NEOTROPICAL_REGION; }
			else if (uuidArea.equals(uuidAreaNL)) { return AREA_NETHERLANDS; }
			else if (uuidArea.equals(uuidAreaNO_NOR)) { return AREA_NORWEGIAN_MAINLAND; }
			else if (uuidArea.equals(uuidAreaNO_SVA)) { return AREA_SVALBARD_WITH_BJORNOYA_AND_JAN_MAYEN; }
			else if (uuidArea.equals(uuidAreaNRE)) { return AREA_NEAR_EAST; }
			else if (uuidArea.equals(uuidAreaORR)) { return AREA_ORIENTAL_REGION; }
			else if (uuidArea.equals(uuidAreaPL)) { return AREA_POLAND; }
			else if (uuidArea.equals(uuidAreaPT_AZO)) { return AREA_AZORES; }
			else if (uuidArea.equals(uuidAreaPT_MDR)) { return AREA_MADEIRA; }
			else if (uuidArea.equals(uuidAreaPT_POR)) { return AREA_PORTUGUESE_MAINLAND; }
			else if (uuidArea.equals(uuidAreaPT_SEL)) { return AREA_SELVAGENS_ISLANDS; }
			else if (uuidArea.equals(uuidAreaRO)) { return AREA_ROMANIA; }
			else if (uuidArea.equals(uuidAreaRU_FJL)) { return AREA_FRANZ_JOSEF_LAND; }
			else if (uuidArea.equals(uuidAreaRU_KGD)) { return AREA_KALININGRAD; }
			else if (uuidArea.equals(uuidAreaRU_NOZ)) { return AREA_NOVAYA_ZEMLYA; }
			else if (uuidArea.equals(uuidAreaRU_RUC)) { return AREA_CENTRAL_EUROPEAN_RUSSIA; }
			else if (uuidArea.equals(uuidAreaRU_RUE)) { return AREA_EASTERN_EUROPEAN_RUSSIA; }
			else if (uuidArea.equals(uuidAreaRU_RUN)) { return AREA_NORTHERN_EUROPEAN_RUSSIA; }
			else if (uuidArea.equals(uuidAreaRU_RUS)) { return AREA_SOUTH_EUROPEAN_RUSSIA; }
			else if (uuidArea.equals(uuidAreaRU_RUW)) { return AREA_NORTHWEST_EUROPEAN_RUSSIA; }
			else if (uuidArea.equals(uuidAreaSE)) { return AREA_SWEDEN; }
			else if (uuidArea.equals(uuidAreaSI)) { return AREA_SLOVENIA; }
			else if (uuidArea.equals(uuidAreaSK)) { return AREA_SLOVAKIA; }
			else if (uuidArea.equals(uuidAreaSM)) { return AREA_SAN_MARINO; }
			else if (uuidArea.equals(uuidAreaTR_TUE)) { return AREA_EUROPEAN_TUERKIYE; }
			else if (uuidArea.equals(uuidAreaUA)) { return AREA_UKRAINE_INCLUDING_CRIMEA; }
			else if (uuidArea.equals(uuidAreaVA)) { return AREA_VATICAN_CITY; }
			else if (uuidArea.equals(uuidAreaYU)) { return AREA_FORMER_JUGOSLAVIA; }

        }else if (area.getVocabulary().getUuid().equals(ErmsTransformer.uuidVocErmsAreas)){
			//ERMS
            UUID uuidArea = namedArea.getUuid();
            if (uuidArea.equals(ErmsTransformer.uuidEuropeanMarineWaters)) { return AREA_EUROPEAN_MARINE_WATERS; }
			else if (//(namedArea.getRepresentation(Language.DEFAULT()).getAbbreviatedLabel()).equals("MES") ||   /carefull: NPE!
					(uuidArea.equals(ErmsTransformer.uuidMediterraneanSea))) { return AREA_MEDITERRANEAN_SEA; } // abbreviated label missing
			else if (uuidArea.equals(ErmsTransformer.uuidWhiteSea)) { return AREA_WHITE_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorthSea)) { return AREA_NORTH_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidBalticSea)) { return AREA_BALTIC_SEA; }
			else if (//(namedArea.getRepresentation(Language.DEFAULT()).getAbbreviatedLabel()).equals("BLS") ||   /carefull: NPE!
					(uuidArea.equals(ErmsTransformer.uuidBlackSea))) { return AREA_BLACK_SEA; } // abbreviated label missing
			else if (uuidArea.equals(ErmsTransformer.uuidBarentszSea)) { return AREA_BARENTSZ_SEA; }
			else if (//(namedArea.getRepresentation(Language.DEFAULT()).getAbbreviatedLabel()).equals("CAS") ||   /carefull: NPE!
					(uuidArea.equals(ErmsTransformer.uuidCaspianSea))) { return AREA_CASPIAN_SEA; } // abbreviated label missing
			else if (uuidArea.equals(ErmsTransformer.uuidPortugueseExclusiveEconomicZone)) { return AREA_PORTUGUESE_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidBelgianExclusiveEconomicZone)) { return AREA_BELGIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidFrenchExclusiveEconomicZone)) { return AREA_FRENCH_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidEnglishChannel)) { return AREA_ENGLISH_CHANNEL; }
			else if (uuidArea.equals(ErmsTransformer.uuidAdriaticSea)) { return AREA_ADRIATIC_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidBayOfBiscay)) { return AREA_BISCAY_BAY; }
			else if (uuidArea.equals(ErmsTransformer.uuidDutchExclusiveEconomicZone)) { return AREA_DUTCH_EXCLUSIVE_ECONOMIC_ZONE; }
//			else if (uuidArea.equals(ErmsTransformer.uuidUnitedKingdomExclusiveEconomicZone)) { return AREA_UNITED_KINGDOM_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidSpanishExclusiveEconomicZone)) { return AREA_SPANISH_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidEgyptianExclusiveEconomicZone)) { return AREA_EGYPTIAN_EXCLUSIVE_ECONOMIC_ZONE; }
//			else if (uuidArea.equals(ErmsTransformer.uuidTirrenoSea)) { return AREA_TIRRENO_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidIcelandicExclusiveEconomicZone)) { return AREA_ICELANDIC_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidIrishExclusiveeconomicZone)) { return AREA_IRISH_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidIrishSea)) { return AREA_IRISH_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidIsraeliExclusiveEconomicZone)){ return 218;}
			else if (uuidArea.equals(ErmsTransformer.uuidItalianExclusiveEconomicZone)) { return AREA_ITALIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorwegianSea)) { return AREA_NORWEGIAN_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidMoroccanExclusiveEconomicZone)) { return AREA_MOROCCAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorwegianExclusiveEconomicZone)) { return AREA_NORWEGIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidSkagerrak)) { return AREA_SKAGERRAK; }
			else if (uuidArea.equals(ErmsTransformer.uuidTunisianExclusiveEconomicZone)) { return AREA_TUNISIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidWaddenSea)) { return AREA_WADDEN_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidBaeltSea)) { return AREA_BELT_SEA; }
//			else if (uuidArea.equals(ErmsTransformer.uuidMarmaraSea)) { return AREA_MARMARA_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidSeaofAzov)) { return AREA_SEA_OF_AZOV; }
			else if (uuidArea.equals(ErmsTransformer.uuidAegeanSea)) { return AREA_AEGEAN_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidBulgarianExclusiveEconomicZone)) { return AREA_BULGARIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidSouthBalticproper)) { return AREA_SOUTH_BALTIC_PROPER; }
			else if (uuidArea.equals(ErmsTransformer.uuidBalticProper)) { return AREA_BALTIC_PROPER; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorthBalticproper)) { return AREA_NORTH_BALTIC_PROPER; }
			else if (uuidArea.equals(ErmsTransformer.uuidArchipelagoSea)) { return AREA_ARCHIPELAGO_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidBothnianSea)) { return AREA_BOTHNIAN_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidGermanExclusiveEconomicZone)) { return AREA_GERMAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidSwedishExclusiveEconomicZone)) { return AREA_SWEDISH_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidUkrainianExclusiveEconomicZone)) { return AREA_UKRAINIAN_EXCLUSIVE_ECONOMIC_ZONE; }
//			else if (uuidArea.equals(ErmsTransformer.uuidMadeiranExclusiveEconomicZone)) { return AREA_MADEIRAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidLebaneseExclusiveEconomicZone)) { return AREA_LEBANESE_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidSpanishExclusiveEconomicZoneMediterraneanpart)) { return AREA_SPANISH_EXCLUSIVE_ECONOMIC_ZONE_MEDITERRANEAN_PART; }
			else if (uuidArea.equals(ErmsTransformer.uuidEstonianExclusiveEconomicZone)) { return AREA_ESTONIAN_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidCroatianExclusiveEconomicZone)) { return AREA_CROATIAN_EXCLUSIVE_ECONOMIC_ZONE; }
//			else if (uuidArea.equals(ErmsTransformer.uuidBalearSea)) { return AREA_BALEAR_SEA; }
			else if (uuidArea.equals(ErmsTransformer.uuidTurkishExclusiveEconomicZone)) { return AREA_TURKISH_EXCLUSIVE_ECONOMIC_ZONE; }
			else if (uuidArea.equals(ErmsTransformer.uuidDanishExclusiveEconomicZone)) { return AREA_DANISH_EXCLUSIVE_ECONOMIC_ZONE; }

//			else if (uuidArea.equals(ErmsTransformer.uuidAfghanistan)) { return 297; }
            else if (uuidArea.equals(ErmsTransformer.uuidAlboranSea)) { return 219; }
			else if (uuidArea.equals(ErmsTransformer.uuidAlgeria)) { return 220; }
//			else if (uuidArea.equals(ErmsTransformer.uuidAngola)) { return 221; }
			else if (uuidArea.equals(ErmsTransformer.uuidArcticOcean)) { return 296; }
//            else if (uuidArea.equals(ErmsTransformer.uuidAustralianExclusiveEconomicZone)) { return 222; }
//			else if (uuidArea.equals(ErmsTransformer.uuidBahamas)) { return 223; }
			else if (uuidArea.equals(ErmsTransformer.uuidBalearicSea)) { return 224; }
			else if (uuidArea.equals(ErmsTransformer.uuidBelgium)) { return 225; }
//			else if (uuidArea.equals(ErmsTransformer.uuidBelize)) { return 226; }
//			else if (uuidArea.equals(ErmsTransformer.uuidBrazil)) { return 227; }
			else if (uuidArea.equals(ErmsTransformer.uuidBulgaria)) { return 228; }
//			else if (uuidArea.equals(ErmsTransformer.uuidCanada)) { return 229; }
			else if (uuidArea.equals(ErmsTransformer.uuidCapeVerde)) { return 230; }
			else if (uuidArea.equals(ErmsTransformer.uuidCapeVerdeanExclusiveEconomicZone)) { return 231; }
//			else if (uuidArea.equals(ErmsTransformer.uuidCaribbeanSea)) { return 210; }
//            else if (uuidArea.equals(ErmsTransformer.uuidChile)) { return 232; }
//			else if (uuidArea.equals(ErmsTransformer.uuidColombia)) { return 233; }
//			else if (uuidArea.equals(ErmsTransformer.uuidCostaRica)) { return 234; }
			else if (uuidArea.equals(ErmsTransformer.uuidCroatia)) { return 235; }
//			else if (uuidArea.equals(ErmsTransformer.uuidCuba)) { return 236; }
			else if (uuidArea.equals(ErmsTransformer.uuidDenmark)) { return 292; }
            else if (uuidArea.equals(ErmsTransformer.uuidEgypt)) { return 237; }
			else if (uuidArea.equals(ErmsTransformer.uuidEstonia)) { return 238; }
			else if (uuidArea.equals(ErmsTransformer.uuidFaeroeExclusiveEconomicZone)) { return 239; }
			else if (uuidArea.equals(ErmsTransformer.uuidFrance)) { return 240; }
//			else if (uuidArea.equals(ErmsTransformer.uuidGhana)) { return 241; }
			else if (uuidArea.equals(ErmsTransformer.uuidGreece)) { return 242; }
			else if (uuidArea.equals(ErmsTransformer.uuidGermany)) { return 298; }
            else if (uuidArea.equals(ErmsTransformer.uuidGreekExclusiveEconomicZone)) { return AREA_GREEK_EXCLUSIVE_ECONOMIC_ZONE; }
            else if (uuidArea.equals(ErmsTransformer.uuidGulfOfBothnia)) { return 243; }
			else if (uuidArea.equals(ErmsTransformer.uuidGulfOfFinland)) { return 244; }
//			else if (uuidArea.equals(ErmsTransformer.uuidGulfOfGuinea)) { return 245; }
//			else if (uuidArea.equals(ErmsTransformer.uuidGulfOfMexico)) { return 246; }
			else if (uuidArea.equals(ErmsTransformer.uuidGulfOfRiga)) { return 247; }
			else if (uuidArea.equals(ErmsTransformer.uuidIceland)) { return 248; }
			else if (uuidArea.equals(ErmsTransformer.uuidIonianSea)) { return 249; }
			else if (uuidArea.equals(ErmsTransformer.uuidIreland)) { return 250; }
            else if (uuidArea.equals(ErmsTransformer.uuidItaly)) { return 251; }
//			else if (uuidArea.equals(ErmsTransformer.uuidJamaica)) { return 252; }
			else if (uuidArea.equals(ErmsTransformer.uuidKattegat)) { return 253; }
			else if (uuidArea.equals(ErmsTransformer.uuidLevantineSea)) { return 254; }
			else if (uuidArea.equals(ErmsTransformer.uuidLigurianSea)) { return 255; }
			else if (uuidArea.equals(ErmsTransformer.uuidMalteseExclusiveEconomicZone)) { return 256; }
			else if (uuidArea.equals(ErmsTransformer.uuidMauritanianExclusiveEconomicZone)) { return 257; }
			else if (uuidArea.equals(ErmsTransformer.uuidMediterraneanSea_EasternBasin)) { return 258; }
			else if (uuidArea.equals(ErmsTransformer.uuidMediterraneanSea_WesternBasin)) { return 259; }
//			else if (uuidArea.equals(ErmsTransformer.uuidMexico)) { return 260; }
//			else if (uuidArea.equals(ErmsTransformer.uuidMongolia)) { return 299; }
            else if (uuidArea.equals(ErmsTransformer.uuidMorocco)) { return 261; }
			else if (uuidArea.equals(ErmsTransformer.uuidNetherlands)) { return 262; }
//			else if (uuidArea.equals(ErmsTransformer.uuidNewZealand)) { return 263; }
//			else if (uuidArea.equals(ErmsTransformer.uuidNewZealandExclusiveEconomicZone)) { return 264; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorthAtlanticOcean)) { return 265; }
			else if (uuidArea.equals(ErmsTransformer.uuidNorway)) { return 266; }
//			else if (uuidArea.equals(ErmsTransformer.uuidPanama)) { return 267; }
//			else if (uuidArea.equals(ErmsTransformer.uuidPanamanianExclusiveEconomicZone)) { return 268; }
			else if (uuidArea.equals(ErmsTransformer.uuidPolishExclusiveEconomicZone)) { return 216; }
            else if (uuidArea.equals(ErmsTransformer.uuidPortugal)) { return 269; }
			else if (uuidArea.equals(ErmsTransformer.uuidPortugueseExclusiveEconomicZone_Azores)) { return 270; }
			else if (uuidArea.equals(ErmsTransformer.uuidPortugueseExclusiveEconomicZone_Madeira)) { return 271; }
			else if (uuidArea.equals(ErmsTransformer.uuidRedSea)) { return 272; }
			else if (uuidArea.equals(ErmsTransformer.uuidRussianExclusiveEconomicZone)) { return 217; }
			else if (uuidArea.equals(ErmsTransformer.uuidSeaOfMarmara)) { return 273; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSenegaleseExclusiveEconomicZone)) { return 274; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSingapore)) { return 275; }
			else if (uuidArea.equals(ErmsTransformer.uuidSlovenianExclusiveEconomicZone)) { return 276; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSouthAfrica)) { return 277; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSouthAfricanExclusiveEconomicZone)) { return 278; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSouthChinaSea)) { return 279; }
			else if (uuidArea.equals(ErmsTransformer.uuidSpain)) { return 280; }
			else if (uuidArea.equals(ErmsTransformer.uuidSpanishExclusiveEconomicZone_CanaryIslands)) { return 281; }
//			else if (uuidArea.equals(ErmsTransformer.uuidSriLankanExclusiveEconomicZone)) { return 282; }
			else if (uuidArea.equals(ErmsTransformer.uuidStraitOfGibraltar)) { return 283; }
			else if (uuidArea.equals(ErmsTransformer.uuidSweden)) { return 284; }
			else if (uuidArea.equals(ErmsTransformer.uuidTunisia)) { return 285; }
			else if (uuidArea.equals(ErmsTransformer.uuidTuerkiye)) { return 286; }
			else if (uuidArea.equals(ErmsTransformer.uuidTyrrhenianSea)) { return 287; }
			else if (uuidArea.equals(ErmsTransformer.uuidUnitedKingdom)) { return 288; }
//	        else if (uuidArea.equals(ErmsTransformer.uuidUnitedStates)) { return 291; }
//			else if (uuidArea.equals(ErmsTransformer.uuidUnitedStatesExclusiveEconomicZone_Alaska)) { return 289; }
//			else if (uuidArea.equals(ErmsTransformer.uuidVenezuela)) { return 290; }

			else if (uuidArea.equals(ErmsTransformer.uuidBritishExclusiveEconomicZone)) { return 312; }
            else if (uuidArea.equals(ErmsTransformer.uuidNorwegianExclusiveEconomicZoneJanMayen)) { return 313; }
            else if (uuidArea.equals(ErmsTransformer.uuidNorwegianExclusiveEconomicZoneSvalbard)) { return 314; }
            else if (uuidArea.equals(ErmsTransformer.uuidSyrianExclusiveEconomicZone)) { return 315; }
            else if (uuidArea.equals(ErmsTransformer.uuidAlgerianExclusiveEconomicZone)) { return 316; }
            else if (uuidArea.equals(ErmsTransformer.uuidCyprioteExclusiveEconomicZone)) { return 317; }
            else if (uuidArea.equals(ErmsTransformer.uuidCelticSea)) { return 318; }
            else if (uuidArea.equals(ErmsTransformer.uuidMontenegrinExclusiveEconomicZone)) { return 319; }
            else if (uuidArea.equals(ErmsTransformer.uuidJordanianExclusiveEconomicZone)) { return 320; }
            else if (uuidArea.equals(ErmsTransformer.uuidDanishExclusiveEconomicZoneFaeroe)) { return 321; }
            else if (uuidArea.equals(ErmsTransformer.uuidLibyanExclusiveEconomicZone)) { return 322; }
            else if (uuidArea.equals(ErmsTransformer.uuidRomanianExclusiveEconomicZone)) { return 323; }

    		else {
				logger.warn("Unknown ERMS Area: " + area.getTitleCache());
			}

		} else {
            logger.warn("Unknown NamedArea Area not in a known vocabulary: " + area.getTitleCache());
        }
		return null;
	}


	/**
	 * Returns the PESI SourceUseId for a given CDM sourceUseId.
	 * @param sourceUseId
	 * @return
	 */
	public static Integer sourceUseIdSourceUseId(Integer sourceUseId) {
		// TODO: CDM sourceUseId and PESI sourceUseId are equal for now.
		Integer result = null;
		switch (sourceUseId) {
			case 3: return ADDITIONAL_SOURCE;
			case 4: return SOURCE_OF_SYNONYMY;
			case 8: return NOMENCLATURAL_REFERENCE;
		}
		return result;
	}

	@Override
	public Object getKeyByLanguage(Language language) throws UndefinedTransformerMethodException {
		return language2LanguageId(language);
	}

	@Override
	public String getCacheByLanguage(Language language) throws UndefinedTransformerMethodException {
		if (language == null){
			return null;
		}else{
			return this.languageCacheMap.get(getKeyByLanguage(language));
		}
	}

	/**
	 * Returns the identifier of the given Language.
	 */
	public static Integer language2LanguageId(Language language) {
		if (language == null ) {
			return null;
		}
		Integer result;
		if ((result = languageCodeToKeyMap.get(language.getIso639_1())) != null){
		    return result;
		}else if ((result = languageCodeToKeyMap.get(language.getIdInVocabulary())) != null){
            return result;
        //Languages without ISO identifier
		}else if (language.getUuid().equals(BerlinModelTransformer.uuidLangValencian)){return LANG_VALENCIAN;
        }else if (language.getUuid().equals(BerlinModelTransformer.uuidLangHighAragonese)){return LANG_HIGH_ARAGONES;
        }else if (language.getUuid().equals(BerlinModelTransformer.uuidLangMajorcan)){return LANG_MAJORCAN;
        //FE wrong mapping in FE, Dutch_Middle should probably be Dutch
        }else if (language.equals(Language.DUTCH_MIDDLE())){return LANG_DUTCH;
        //some common names from ILDIS have no defined language
        }else if (language.equals(Language.UNKNOWN_LANGUAGE())){return LANG_UNKNOWN;
        } else {
			logger.warn("Unknown Language: " + language.getTitleCache());
			return null;
		}
	}

	/**
	 * Returns the NodeCategoryCache for a given TextData.
	 */
	@Override
    public String getCacheByFeature(Feature feature) {
		if (feature == null){
			return null;
		}else{
			return this.featureCacheMap.get(feature2NoteCategoryFk(feature));
		}
	}

	/**
	 * Returns the NodeCategoryFk for a given TextData.
	 */
	public static Integer feature2NoteCategoryFk(Feature feature) {
		if (feature == null) {
			return null;
		}

		if (feature.equals(Feature.DESCRIPTION())) {
			return NoteCategory_description;
		} else if (feature.equals(Feature.ECOLOGY())) {
			return NoteCategory_ecology;
		} else if (feature.equals(Feature.PHENOLOGY())) {
			return NoteCategory_phenology;
		} else if (feature.equals(Feature.DIAGNOSIS())) {
            return NoteCategory_Diagnosis;
        } else if (feature.equals(Feature.COMMON_NAME())) {
			return NoteCategory_Common_names;
		} else if (feature.equals(Feature.OCCURRENCE())) {
			return NoteCategory_Occurrence;
		} else if (feature.equals(Feature.DISTRIBUTION())) {
			return NoteCategory_Distribution;
		} else if (feature.equals(Feature.ETYMOLOGY())) {
            return NoteCategory_Etymology;
		} else if (feature.equals(Feature.IUCN_STATUS())) {
            return NoteCategory_IUCN_Status;

        //************************ ERMS Features ***************************/

        } else if (feature.getUuid().equals(ErmsTransformer.uuidAcknowledgments)){
		    return NoteCategory_Acknowledgments;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidAdditionalinformation)) {
		    return NoteCategory_Additional_information;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidAlienSpecies)) {
		    return NoteCategory_Alien_Species;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidAuthority)) {
		    return NoteCategory_Authority;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidDepthRange)) {
		    return NoteCategory_Depth_Range;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidBiology)) {
		    return NoteCategory_Biology;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidClassification)) {
		    return NoteCategory_Classification;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidDiet)) {
		    return NoteCategory_Diet;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidDimensions)) {
		    return NoteCategory_Dimensions;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidEditorsComment)) {
		    return NoteCategory_Editors_Comment;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidEnvironment)) {
		    return NoteCategory_Environment;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidFossilRange)) {
		    return NoteCategory_Fossil_Range;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidGrammaticalGender)) {
		    return NoteCategory_Grammatical_Gender;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidHabitat)) {
		    return NoteCategory_Habitat;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidHomonymy)) {
		    return NoteCategory_Homonymy;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidIdentification)) {
		    return NoteCategory_Identification;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidIntroducedSpeciesRemark)) {
		    return NoteCategory_Introduced_Species_Remark;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidMorphology)) {
		    return NoteCategory_Morphology;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidNewCombination)) {
		    return NoteCategory_New_Combination;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidNomenclature)) {
		    return NoteCategory_Nomenclature;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidNote)){
		    return NoteCategory_Note;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidOriginalCombination)) {
		    return NoteCategory_Original_Combination;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidOriginalDescription)) {
		    return NoteCategory_Original_Description;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidOriginalpublication)) {
		    return NoteCategory_Original_publication;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidPublicationdate)) {
            return NoteCategory_Publication_date;
        } else if (feature.getUuid().equals(ErmsTransformer.uuidRank)) {
		    return NoteCategory_Rank;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidRemark)) {
		    return NoteCategory_Remark;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidReproduction)) {
		    return NoteCategory_Reproduction;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidSpelling)) {
		    return NoteCategory_Spelling;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidSpecimen)) {
            return NoteCategory_Specimen;
        } else if (feature.getUuid().equals(ErmsTransformer.uuidStatus)){
		    return NoteCategory_Status;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidSynonymy)) {
		    return NoteCategory_Synonymy;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidSyntype)) {
		    return NoteCategory_Syntype;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidSystematics)) {
		    return NoteCategory_Systematics;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTaxonomicRemarks)) {
			return NoteCategory_Taxonomic_Remarks;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTaxonomy)) {
			return NoteCategory_Taxonomy;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTaxonomicstatus)) {
			return NoteCategory_Taxonomic_status;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTypelocality)) {
		    return NoteCategory_Type_locality;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTypeMaterial)) {
		    return NoteCategory_Type_Material;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTypeSpecimen)) {
		    return NoteCategory_Type_Specimen;
        } else if (feature.getUuid().equals(ErmsTransformer.uuidTypespecies)) {
            return NoteCategory_Type_species;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidTaxonomicRemark)) {
			return NoteCategory_Taxonomic_Remark;
		} else if (feature.getUuid().equals(ErmsTransformer.uuidValidity)) {
			return NoteCategory_Validity;

//        } else if (feature.getUuid().equals(ErmsTransformer.uuidSourceOfSynonymy)) {
//		    logger.debug("Source of synonymy not yet handled");
//		    return null;
		} else if (feature.equals(Feature.CITATION())) {
			return null;  //citations are handled differently
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureMaps)){
			return NoteCategory_Link_to_maps;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureUse)){
			return NoteCategory_Use;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureComments)){
			return NoteCategory_Comments;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureConservationStatus)){
			return NoteCategory_Conservation_Status;

		//E+M
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureDistrEM)){
			return NoteCategory_general_distribution_euromed;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureDistrWorld)){
			return NoteCategory_general_distribution_world;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureEditorBrackets)){
			return NoteCategory_Editor_Brackets;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureEditorParenthesis)){
			return NoteCategory_Editor_Parenthesis;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureInedited)){
			return NoteCategory_Inedited;
		} else if (feature.getUuid().equals(BerlinModelTransformer.uuidFeatureCommentsEditing)){
			return NoteCategory_Comments_on_editing_process;

		}else{
			logger.warn("Unhandled Feature: " + feature.getTitleCache());
			return null;
		}
	}

	/**
	 * Returns the string representation for a given rank.
	 */
	public String getCacheByRankAndKingdom(Rank rank, Integer pesiKingdomId) {
		if (rank == null){
			return null;
		}else if (pesiKingdomId == null && rank.equals(Rank.DOMAIN())){  //might be Superdomain in future
		    return this.rankCacheMap.get(0).get(0);
		}else{
		    Map<Integer, String> rankMap = this.rankCacheMap.get(pesiKingdomId);
		    if (rankMap != null){
		        return rankMap.get(rank2RankId(rank, pesiKingdomId));
		    }else{
		        logger.warn("RankCacheMap is null for " + pesiKingdomId);
		        return null;
		    }
		}
	}

	/**
	 * Returns the identifier of a PESI specific kingdom for a given CDM nomenclatural code.
	 * @param nomenclaturalCode
	 * @return KINGDOM_ANIMALIA for NomenclaturalCode.ICZN, KINGDOM_PLANTAE for NomenclaturalCode.ICNAFP
	 */
	public static Integer nomenclaturalCode2Kingdom(NomenclaturalCode nomenclaturalCode) {
		Integer result = null;
		// TODO: This needs to be refined. For now we differentiate between animalia, plantae and bacteria only.
		if (nomenclaturalCode.equals(NomenclaturalCode.ICZN)) {
			result = KINGDOM_ANIMALIA;
		} else if (nomenclaturalCode.equals(NomenclaturalCode.ICNAFP)) {
			result = KINGDOM_PLANTAE;
		} else if (nomenclaturalCode.equals(NomenclaturalCode.ICNP)) {
			result = KINGDOM_BACTERIA;
//		} else if (nomenclaturalCode.equals(NomenclaturalCode.)) { // Biota
//			result =
		} else {
 			logger.error("NomenclaturalCode not yet considered: " + nomenclaturalCode.getUuid() + " (" +  nomenclaturalCode.getTitleCache() + ")");
		}
		return result;
	}

	/**
	 * Returns the RankId for a Rank.
	 * @param rank
	 * @return
	 */
	public static Integer rank2RankId (Rank rank, Integer pesiKingdomId) {
		Integer result = null;
		if (rank == null) {
			return null;
		}else if (rank.equals(Rank.DOMAIN())){
		    return KINGDOM_NULL;
		}else if (rank.equals(Rank.KINGDOM())) {
            result = Kingdom;
        }else if (rank.equals(Rank.SUBKINGDOM())) {
            result = Subkingdom;
        } else if (rank.equals(Rank.PHYLUM())) {
            result = Phylum;
        }else if (rank.equals(Rank.DIVISION())) {  //same as Phylum
            result = Division;
        }else if (rank.equals(Rank.SUBPHYLUM())) {
            result = Subphylum;
        }else if (rank.equals(Rank.SUBDIVISION())) { //same as Subphylum
            result = Subdivision;
        }else if (rank.equals(Rank.CLASS())) {
            result = Class;
        } else if (rank.equals(Rank.SUBCLASS())) {
            result = Subclass;
        } else if (rank.equals(Rank.ORDER())) {
            result = Order;
        } else if (rank.equals(Rank.SUBORDER())) {
            result = Suborder;
        } else if (rank.equals(Rank.FAMILY())) {
            result = Family;
        } else if (rank.equals(Rank.SUBFAMILY())) {
            result = Subfamily;
        } else if (rank.equals(Rank.TRIBE())) {
            result = Tribe;
        } else if (rank.equals(Rank.SUBTRIBE())) {
            result = Subtribe;
        } else if (rank.equals(Rank.GENUS())) {
            result = Genus;
        } else if (rank.equals(Rank.SUBGENUS())) {
            result = Subgenus;
        } else if (rank.equals(Rank.SPECIES())) {
            result = Species;
        } else if (rank.equals(Rank.SUBSPECIES())) {
            result = Subspecies;
        } else if (rank.equals(Rank.VARIETY())) {
            result = Variety;
        } else if (rank.equals(Rank.FORM())) {
            result = Forma;
        } else

		// We differentiate between Animalia and Plantae only for now.
		if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_ANIMALIA) {
			if (rank.equals(Rank.INFRAKINGDOM())) {result = Infrakingdom;
            } else if (rank.equals(Rank.SUPERPHYLUM())) {result = Superphylum;
			} else if (rank.equals(Rank.INFRAPHYLUM())) {result = Infraphylum;
			} else if (rank.getUuid().equals(ErmsTransformer.uuidRankParvphylum)) {result = Parvphylum;
            } else if (rank.getUuid().equals(ErmsTransformer.uuidRankMegaclass)) {result = Megaclass;
            } else if (rank.getUuid().equals(ErmsTransformer.uuidRankGigaclass)) {result = Gigaclass;
            } else if (rank.equals(Rank.SUPERCLASS())) {result = Superclass;
			} else if (rank.equals(Rank.INFRACLASS())) {result = Infraclass;
            } else if (rank.getUuid().equals(ErmsTransformer.uuidRankSubterclass)) {result = Subterclass;
			} else if (rank.equals(Rank.SUPERORDER())) {result = Superorder;
			} else if (rank.equals(Rank.INFRAORDER())) {result = Infraorder;
			} else if (rank.getUuid().equals(ErmsTransformer.uuidRankParvorder)) {result = Parvorder;
            } else if (rank.equals(Rank.SECTION_ZOOLOGY())) {result = Animalia_Section;
			} else if (rank.equals(Rank.SUBSECTION_ZOOLOGY())) {result = Animalia_Subsection;
			} else if (rank.equals(Rank.SUPERFAMILY())) {result = Superfamily;
			} else if (rank.getUuid().equals(ErmsTransformer.uuidRankEpifamily)) {result = Animalia_Epifamily;
            } else if (rank.equals(Rank.NATIO())) {result = Natio;
			} else if (rank.equals(Rank.SUBVARIETY())) {result = Subvariety;
			} else if (rank.equals(Rank.SUBFORM())) {result = Subform;
			} else if (rank.getUuid().equals(ErmsTransformer.uuidRankMutatio)) {result = Mutatio;

            } else {
				//TODO Exception
				logger.warn("Rank for Kingdom Animalia not yet supported in CDM: "+ rank.getLabel());
				return null;
			}
		} else if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_PLANTAE) {
			if (rank.equals(Rank.INFRAKINGDOM())) {result = Infrakingdom;
            } else if (rank.equals(Rank.SUPERORDER())) {result = Superorder;
			} else if (rank.equals(Rank.SECTION_BOTANY())) {result = Bot_Section;
			} else if (rank.equals(Rank.SUBSECTION_BOTANY())) {result = Bot_Subsection;
			} else if (rank.equals(Rank.SERIES())) {result = Series;
			} else if (rank.equals(Rank.SUBSERIES())) {result = Subseries;
			} else if (rank.equals(Rank.SPECIESAGGREGATE() )) {result = Aggregate;
			} else if (rank.equals(Rank.SPECIESGROUP())) {
				logger.warn("Rank Species Group not yet implemented");
				result = null;
			} else if (rank.getUuid().equals(Rank.uuidCollSpecies)) {result = Coll_Species;
			} else if (rank.equals(Rank.GREX_INFRASPEC())) {result = Grex;
			} else if (rank.getUuid().equals(Rank.uuidProles) ) {result = Proles;
			} else if (rank.getUuid().equals(Rank.uuidRace)) {result = Race;
			} else if (rank.equals(Rank.CONVAR())) {result = Convarietas;
			} else if (rank.equals(Rank.SUBVARIETY())) {result = Subvariety;
			} else if (rank.equals(Rank.SUBFORM())) {result = Subform;
			} else if (rank.equals(Rank.SPECIALFORM())) {result = Forma_spec;
			} else if (rank.equals(Rank.INFRAGENERICTAXON())) {result = Taxa_infragen;
			} else if (rank.equals(Rank.INFRASPECIFICTAXON())) {result = Taxa_infraspec;
			} else if (rank.equals(Rank.SUPRAGENERICTAXON())) {result = Taxa_supragen;
            } else {
				//TODO Exception
				logger.warn("Rank for Kingdom Plantae not yet supported in CDM: "+ rank.getLabel());
				return null;
			}
		} else if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_FUNGI) {
		    if (rank.equals(Rank.SECTION_BOTANY())) { result = Bot_Section;
		    } else if (rank.equals(Rank.SUBSECTION_BOTANY())) { result = Bot_Subsection;
		    } else if (rank.equals(Rank.SUBVARIETY())) { result = Subvariety;
		    } else if (rank.equals(Rank.SUBFORM())) { result = Subform;
		    } else if (rank.equals(Rank.SPECIALFORM())) {result = Forma_spec;
		    } else {
		        //TODO Exception
		        logger.warn("Rank for Kingdom Fungi not yet supported in CDM: "+ rank.getLabel());
		        return null;
		    }
        }else if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_PROTOZOA) {
            if (rank.equals(Rank.INFRAKINGDOM())) { result = Infrakingdom;
            } else if (rank.equals(Rank.SUPERCLASS())) { result = Superclass;
            } else if (rank.equals(Rank.INFRACLASS())) { result = Infraclass;
            } else if (rank.equals(Rank.SUPERORDER())) { result = Superorder;
            } else if (rank.equals(Rank.INFRAORDER())) { result = Infraorder;
            } else if (rank.equals(Rank.SUPERFAMILY())) { result = Superfamily;
            } else if (rank.equals(Rank.SPECIALFORM())) {result = Forma_spec;
            } else if (rank.equals(Rank.INFRAPHYLUM())) {result = Infraphylum;
            } else {
                //TODO Exception
                logger.warn("Rank for Kingdom Protozoa not yet supported in CDM: "+ rank.getLabel());
                return null;
            }
        } else if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_BACTERIA) {
            if (rank.equals(Rank.SUPERCLASS())) { result = Superclass;
            } else if (rank.equals(Rank.INFRACLASS())) { result = Infraclass;
            } else if (rank.equals(Rank.SUPERORDER())) { result = Superorder;
            } else if (rank.equals(Rank.INFRAORDER())) { result = Infraorder;
            } else if (rank.equals(Rank.SUPERFAMILY())) { result = Superfamily;
            } else {
                //TODO Exception
                logger.warn("Rank for Kingdom Bacteria not yet supported in CDM: "+ rank.getLabel());
                return null;
            }
        }else if (pesiKingdomId != null && pesiKingdomId.intValue() == KINGDOM_CHROMISTA) {
            if (rank.equals(Rank.INFRAKINGDOM())) { result = Infrakingdom;
            } else if (rank.equals(Rank.INFRAPHYLUM())) { result = Infraphylum;
            } else if (rank.equals(Rank.SUPERCLASS())) { result = Superclass;
            } else if (rank.equals(Rank.INFRACLASS())) { result = Infraclass;
            } else if (rank.equals(Rank.SUPERORDER())) { result = Superorder;
            } else if (rank.equals(Rank.INFRAORDER())) { result = Infraorder;
            } else if (rank.equals(Rank.SUPERFAMILY())) { result = Superfamily;
            } else if (rank.equals(Rank.SECTION_BOTANY())) { result = Bot_Section;
            } else if (rank.equals(Rank.SUBSECTION_BOTANY())) { result = Bot_Subsection;
            } else if (rank.equals(Rank.SUBVARIETY())) { result = Subvariety;
            } else if (rank.equals(Rank.SPECIALFORM())) {result = Forma_spec;
            } else {
                //TODO Exception
                logger.warn("Rank for Kingdom Chromista not yet supported in CDM: "+ rank.getLabel());
                return null;
            }
        }else{
			//TODO Exception
			logger.warn("Kingdom not yet supported in CDM: "+ pesiKingdomId);
			return null;
		}
		return result;
	}

	public static Integer nameTypeDesignationStatus2TypeDesignationStatusId(NameTypeDesignationStatus nameTypeDesignationStatus) {
		if (nameTypeDesignationStatus == null) {
			return null;
		}
		if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.ORIGINAL_DESIGNATION())) {
			return TYPE_BY_ORIGINAL_DESIGNATION;
		} else if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.SUBSEQUENT_DESIGNATION())) {
			return TYPE_BY_SUBSEQUENT_DESIGNATION;
		} else if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.MONOTYPY())) {
			return TYPE_BY_MONOTYPY;
		} else {
			//TODO Figure out a way to handle this gracefully.
			logger.warn("Name Type Designation Status not yet supported in PESI: "+ nameTypeDesignationStatus.getLabel());
			return null;
		}

	}

	public static String nameTypeDesignationStatus2TypeDesignationStatusCache(NameTypeDesignationStatus nameTypeDesignationStatus) {
		if (nameTypeDesignationStatus == null) {
			return null;
		}
		if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.ORIGINAL_DESIGNATION())) {
			return TYPE_STR_BY_ORIGINAL_DESIGNATION;
		} else if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.SUBSEQUENT_DESIGNATION())) {
			return TYPE_STR_BY_SUBSEQUENT_DESIGNATION;
		} else if (nameTypeDesignationStatus.equals(NameTypeDesignationStatus.MONOTYPY())) {
			return TYPE_STR_BY_MONOTYPY;
		} else {
			//TODO Figure out a way to handle this gracefully.
			logger.warn("Name Type Designation Status not yet supported in PESI: "+ nameTypeDesignationStatus.getLabel());
			return null;
		}
	}

	/**
	 * @see PesiTaxonExport#doPhaseUpdates(PesiExportState) for further transformation
	 * @param taxonBase
	 * @return
	 */
	public static Integer taxonBase2statusFk (TaxonBase<?> taxonBase){
		if (taxonBase == null){
			return null;
		}else if(!taxonBase.getExtensions(ErmsTransformer.uuidPesiTaxonStatus).isEmpty()){
		    Integer ermsStatus = taxonStatusByErmsStatus(taxonBase.getExtensions(ErmsTransformer.uuidPesiTaxonStatus));
		    boolean isSynStatus = ermsStatus == 2 || ermsStatus == 3 || ermsStatus == 4;  //syn, ppsyn, partialSyn
		    if (isSynStatus && taxonBase.isInstanceOf(Synonym.class)) {
		        return ermsStatus;
		    }else if (!isSynStatus && taxonBase.isInstanceOf(Synonym.class)) {
		        logger.warn("Check erms status for synonym: "+ taxonBase.getTitleCache());
		        return ermsStatus;
		    }else if (isSynStatus && !taxonBase.isInstanceOf(Synonym.class)) {
		        Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
		        if (! PesiCommandLineMerge.isTaxonSynonym(taxon)) {
		            if (!taxon.isMisapplicationOnly()) {
		                if (isAltRepForAutonym(taxon)) {
		                    return 1;
		                }else {
		                    logger.warn("ERMS taxon has syn status but is not taxon synonym: " + taxon.getTitleCache());
		                }

		            } else {
                        logger.debug("ERMS taxon has syn status and is MAN. This is probably correct but needs final chck: " + taxon.getTitleCache());
		            }
		        }
		        return ermsStatus;
		    }else {
		        //!synStatus && !Synonym.class
		        if (ermsStatus == 7) {
		            //OK, unaccepted, but not synonym
		            //TODO aber nur, wenn ERMS Priorität hat, z.B. zusammen mit FauEu.
		            //Das sollte grundsätzlich beim Merge behandelt werden!! Der status sollte nicht kopiert werden
		        }else {
		            logger.warn("Unexpected pesi taxon status: " + taxonBase.getTitleCache());
		        }
		    }
		    return taxonStatusByErmsStatus(taxonBase.getExtensions(ErmsTransformer.uuidPesiTaxonStatus));
		}else{
		    if (taxonBase.isInstanceOf(Synonym.class)){
    		    Synonym synonym = CdmBase.deproxy(taxonBase, Synonym.class);
    		    if (taxonBase2statusFk(synonym.getAcceptedTaxon())== T_STATUS_NOT_ACCEPTED_VALUELESS ){
    		        return T_STATUS_NOT_ACCEPTED_VALUELESS;
    		    }else{
    		        return T_STATUS_SYNONYM;
    		    }
		    }else if (taxonBase.isInstanceOf(Taxon.class)){
    			Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
    			Set<TaxonRelationship> rels = taxon.getRelationsFromThisTaxon();
    			Set<TaxonNode> nodes = taxon.getTaxonNodes();
    			if (!rels.isEmpty() && !nodes.isEmpty()){
    			    logger.warn("Taxon has relations and parent. This is not expected in E+M, but maybe possible in ERMS. Check if taxon status is correct.");
    			}else if (rels.isEmpty() && nodes.isEmpty()){
                    logger.warn("Taxon has neither relations nor parent. This is not expected. Check if taxon status is correct:" + taxon.getTitleCache());
                }
    			if (!rels.isEmpty()){
    			    //we expect all rels to have same type, maybe not true
    			    UUID relTypeUuid = rels.iterator().next().getType().getUuid();
    			    //E+M
    			    if (TaxonRelationshipType.proParteUuids().contains(relTypeUuid)){
    	                return T_STATUS_PRO_PARTE_SYN;
    	            }else if (TaxonRelationshipType.partialUuids().contains(relTypeUuid)){
    	                return T_STATUS_PARTIAL_SYN;
    	            }else if (TaxonRelationshipType.misappliedNameUuids().contains(relTypeUuid)){
    	                return T_STATUS_SYNONYM;  //no explicit MAN status exists in PESI
    	            }
    			    //ERMS
    	            else if (TaxonRelationshipType.pseudoTaxonUuids().contains(relTypeUuid)){
    	                return T_STATUS_SYNONYM;
    	            }
    			}
    			if (!nodes.isEmpty()){
    			    TaxonNode parentNode = nodes.iterator().next().getParent();
    			    if (parentNode.getTaxon() != null && !parentNode.getTaxon().isPublish()){
    			        if (parentNode.getTaxon().getUuid().equals(uuidTaxonValuelessEuroMed) ){
    			            return T_STATUS_NOT_ACCEPTED_VALUELESS;
    			        }
    			    }else{
    			        if (taxon.isDoubtful()){
    			            return T_STATUS_UNRESOLVED;
    			        }else{
    			            return T_STATUS_ACCEPTED;
    			        }
    			    }
    	        }
    			logger.error("Taxon status could not be defined. This should not happen: " + taxonBase.getTitleCache() );
    			return T_STATUS_UNRESOLVED;
    		}else{
    		    logger.warn("Unresolved taxon status, neither taxon nor synonym, this should not happen");
    			return T_STATUS_UNRESOLVED;
    		}
    		//TODO
    //		public static int T_STATUS_ORPHANED = 6;  //never used in SQL import
		}
	}

    /**
     * Checks, if this taxon is an alternative representation and has a child taxon
     * which is an autonym. This is the way how ERMS handles it.
     * In this case the taxon should be accepted and not a synonym.
     */
    private static boolean isAltRepForAutonym(Taxon taxon) {
        try {
            TaxonName name = taxon.getName();
            UUID uuidAltRep = ErmsTransformer.uuidNomStatusAlternateRepresentation;
            boolean isAltRep = name.getStatus().stream()
                    .anyMatch(ns->ns.getType().getUuid().equals(uuidAltRep));
            if (isAltRep) {
                boolean hasAutonym = taxon.getTaxonNodes().stream()
                    .flatMap(tn->tn.getChildNodes().stream())
                    .anyMatch(ctn->isAutonymOf(ctn.getTaxon().getName(), name));
                return hasAutonym;
            }
        } catch (Exception e) {
            logger.error("Error in isAltRepForAutonym");
        }
        return false;
    }

    private static boolean isAutonymOf(TaxonName autonym, TaxonName originalName) {
        if (autonym.isAutonym(true)
                && Objects.equals(autonym.getGenusOrUninomial(), originalName.getGenusOrUninomial())) {
            if (originalName.isGenus()) {
                return true;
            }else if (originalName.isSpecies()) {
                return Objects.equals(autonym.getSpecificEpithet(), originalName.getSpecificEpithet());
            }
        }
        return false;
    }
    private static Integer taxonStatusByErmsStatus(Set<String> extensions) {
        //Note: status extensions should only be used during ERMS import
        //      if the status can not be derived from ordinary CDM status handling (Taxon, Synonym, ProParte, doubtful, ...)
        // see ErmsTaxonImport.handleNotAcceptedTaxonStatus
        if (extensions.size()>1){
            logger.warn("More than 1 ERMS status available. This should not happen.");
        }
        String statusStr = extensions.iterator().next();
        Integer status = Integer.valueOf(statusStr);
        return status;
    }

    /**
	 * Returns the {@link SourceCategory SourceCategory} representation of the given {@link ReferenceType ReferenceType} in PESI.
	 * @param reference The {@link Reference Reference}.
	 * @return The {@link SourceCategory SourceCategory} representation in PESI.
	 */
	public static Integer reference2SourceCategoryFK(Reference reference) {
		if (reference == null){
			return null;
		} else if (reference.getType().equals(ReferenceType.Article)) {
			return REF_ARTICLE_IN_PERIODICAL;
		} else if (reference.getType().equals(ReferenceType.Book)) {
			return REF_BOOK;
		} else if (reference.getType().equals(ReferenceType.BookSection)) {
			return REF_PART_OF_OTHER;
		} else if (reference.getType().equals(ReferenceType.Section)) {
			return REF_PART_OF_OTHER;
		} else if (reference.getType().equals(ReferenceType.Database)) {
			return REF_DATABASE;
		} else if (reference.getType().equals(ReferenceType.WebPage)) {
			return REF_WEBSITE;
		} else if (reference.getType().equals(ReferenceType.CdDvd)) {
			return REF_NOT_APPLICABLE;
		} else if (reference.getType().equals(ReferenceType.Journal)) {
			return REF_JOURNAL;
		} else if (reference.getType().equals(ReferenceType.PrintSeries)) {
			return REF_PUBLICATION;  //?
		} else if (reference.getType().equals(ReferenceType.Proceedings)) {
			return REF_PUBLICATION;  //?
		} else if (reference.getType().equals(ReferenceType.InProceedings)) {
            return REF_PUBLICATION;  //?
		} else if (reference.getType().equals(ReferenceType.Patent)) {
			return REF_NOT_APPLICABLE;
		} else if (reference.getType().equals(ReferenceType.PersonalCommunication)) {
			return REF_INFORMAL;
		} else if (reference.getType().equals(ReferenceType.Report)) {
			return REF_NOT_APPLICABLE;
		} else if (reference.getType().equals(ReferenceType.Thesis)) {
			return REF_NOT_APPLICABLE;
		} else if (reference.getType().equals(ReferenceType.Generic)) {
            if(reference.hasMarker(ErmsTransformer.uuidMarkerRefPublication, true)){
                return REF_PUBLICATION;
            }else if(reference.hasMarker(ErmsTransformer.uuidMarkerRefInformal, true)){
                return REF_INFORMAL;
            }else if(reference.hasMarker(ErmsTransformer.uuidMarkerRefTypeI, true)){
                logger.warn("ERMS ref type 'i' is not yet correctly matched to PESI");
                return REF_INFORMAL;
            }else{
                return REF_UNRESOLVED;
            }
        } else {
			logger.warn("Reference type not yet supported in PESI: "+ reference.getType());
			return null;
		}
	}

	/**
	 * Returns the {@link SourceCategoryCache SourceCategoryCache}.
	 * @param reference The {@link Reference Reference}.
	 * @return The {@link SourceCategoryCache SourceCategoryCache}.
	 */
	public String getCacheByReference(Reference reference) {
		if (reference == null){
			return null;
		}else{
			return this.sourceCategoryCacheMap.get(reference2SourceCategoryFK(reference));
		}
	}

	@Override
    public String getCacheByNomStatus(NomenclaturalStatusType status) {
		if (status == null){
			return null;
		}else{
			return this.nameStatusCacheMap.get(nomStatus2nomStatusFk(status));
		}
	}

	public static Integer nomStatus2nomStatusFk (NomenclaturalStatusType status){
		if (status == null){
			return null;
		}
		if (status.equals(NomenclaturalStatusType.INVALID())) {return NAME_ST_NOM_INVAL;
		}else if (status.equals(NomenclaturalStatusType.ILLEGITIMATE())) {return NAME_ST_NOM_ILLEG;
		}else if (status.equals(NomenclaturalStatusType.NUDUM())) {return NAME_ST_NOM_NUD;
		}else if (status.equals(NomenclaturalStatusType.REJECTED())) {return NAME_ST_NOM_REJ;
		}else if (status.equals(NomenclaturalStatusType.REJECTED_PROP())) {return NAME_ST_NOM_REJ_PROP;
		}else if (status.equals(NomenclaturalStatusType.UTIQUE_REJECTED())) {return NAME_ST_NOM_UTIQUE_REJ;
		}else if (status.equals(NomenclaturalStatusType.UTIQUE_REJECTED_PROP())) {return NAME_ST_NOM_UTIQUE_REJ_PROP;
		}else if (status.equals(NomenclaturalStatusType.CONSERVED())) {return NAME_ST_NOM_CONS;

		}else if (status.equals(NomenclaturalStatusType.CONSERVED_PROP())) {return NAME_ST_NOM_CONS_PROP;
		}else if (status.equals(NomenclaturalStatusType.ORTHOGRAPHY_CONSERVED())) {return NAME_ST_ORTH_CONS;
		}else if (status.equals(NomenclaturalStatusType.ORTHOGRAPHY_CONSERVED_PROP())) {return NAME_ST_ORTH_CONS_PROP;
		}else if (status.equals(NomenclaturalStatusType.SUPERFLUOUS())) {return NAME_ST_NOM_SUPERFL;
		}else if (status.equals(NomenclaturalStatusType.AMBIGUOUS())) {return NAME_ST_NOM_AMBIG;
		}else if (status.equals(NomenclaturalStatusType.PROVISIONAL())) {return NAME_ST_NOM_PROVIS;
		}else if (status.equals(NomenclaturalStatusType.DOUBTFUL())) {return NAME_ST_NOM_DUB;
		}else if (status.equals(NomenclaturalStatusType.NOVUM())) {return NAME_ST_NOM_NOV;

		}else if (status.equals(NomenclaturalStatusType.CONFUSUM())) {return NAME_ST_NOM_CONFUS;
		}else if (status.equals(NomenclaturalStatusType.ALTERNATIVE())) {return NAME_ST_NOM_ALTERN;
		}else if (status.equals(NomenclaturalStatusType.COMBINATION_INVALID())) {return NAME_ST_COMB_INVAL;
		}else if (status.equals(NomenclaturalStatusType.LEGITIMATE())) {return NAME_ST_LEGITIMATE;
		}else if (status.equals(NomenclaturalStatusType.COMBINATION_ILLEGITIMATE())) {return NAME_ST_COMB_ILLEG;
        }else if (status.equals(NomenclaturalStatusType.COMB_INED())) {return NAME_ST_COMB_INED;
        }else if (status.equals(NomenclaturalStatusType.NAME_AND_ORTHOGRAPHY_CONSERVED())) {return NAME_ST_NOM_AND_ORTH_CONS;
        }else if (status.equals(NomenclaturalStatusType.COMB_NOV())) {return NAME_ST_COMB_INED;

		//does not seem to exist anymore
//		}else if (status.getUuid().equals(BerlinModelTransformer.uuidNomStatusSpNovIned)) {return NAME_ST_SP_NOV_INED;


		// The following are non-existent in CDM
//		}else if (status.equals(NomenclaturalStatusType.)) {return NAME_ST_COMB_AND_STAT_INED;
//		}else if (status.equals(NomenclaturalStatusType.)) {return NAME_ST_NOM_NOV_INED;
		}else if (status.getUuid().equals(ErmsTransformer.uuidNomStatusAlternateRepresentation)) {return NAME_ST_ALTERNATE_REPRESENTATION;
		}else if (status.getUuid().equals(uuidNomStatusTemporaryName)) {return NAME_ST_TEMPORARY_NAME;
		}else if (status.getUuid().equals(ErmsTransformer.uuidNomStatusSpeciesInquirenda)) {return NAME_ST_SPECIES_INQUIRENDA;

		//TODO
		}else {
			//TODO Exception
			logger.warn("NomStatus type not yet supported by PESI export: "+ status + "("+status.getUuid()+")");
			return null;
		}
	}

	/**
	 * Returns the RelTaxonQualifierCache for a given taxonRelation.
	 */
	public String getCacheByRelationshipType(RelationshipBase relation, NomenclaturalCode code){
		if (relation == null){
			return null;
		}else{
			String result;
			Integer key = taxonRelation2RelTaxonQualifierFk(relation);
			if (code.equals(NomenclaturalCode.ICZN)){
				result = this.taxRelZooQualifierCacheMap.get(key);
				if (result == null){
					result = this.taxRelQualifierCacheMap.get(key);
				}
			}else{
				result = this.taxRelQualifierCacheMap.get(key);
			}
			return result;
		}
	}

    public String getCacheBySynonymType(Synonym synonym, NomenclaturalCode code){
        if (synonym == null){
            return null;
        }else{
            String result;
            Integer key = synonym2RelTaxonQualifierFk(synonym);
            if (code.equals(NomenclaturalCode.ICZN)){
                result = this.taxRelZooQualifierCacheMap.get(key);
                if (result == null){
                    result = this.taxRelQualifierCacheMap.get(key);
                }
            }else{
                result = this.taxRelQualifierCacheMap.get(key);
            }
            return result;
        }
    }

    public static Integer synonym2RelTaxonQualifierFk(Synonym synonym) {
        if (synonym == null || synonym.getType() == null){
            return null;
        }
        SynonymType type = synonym.getType();
        if (type.equals(SynonymType.SYNONYM_OF)) {return IS_SYNONYM_OF;
        }else if (type.equals(SynonymType.HOMOTYPIC_SYNONYM_OF)) {return IS_HOMOTYPIC_SYNONYM_OF;
        }else if (type.equals(SynonymType.HETEROTYPIC_SYNONYM_OF)) {return IS_HETEROTYPIC_SYNONYM_OF;
        }else if (type.equals(SynonymType.INFERRED_EPITHET_OF)) {return IS_INFERRED_EPITHET_FOR;
        }else if (type.equals(SynonymType.INFERRED_GENUS_OF)) {return IS_INFERRED_GENUS_FOR;
        }else if (type.equals(SynonymType.POTENTIAL_COMBINATION_OF)) {return IS_POTENTIAL_COMBINATION_FOR;
        }else if (type.equals(SynonymType.INFERRED_SYNONYM_OF)) {
            logger.warn("Inferred synonynm type not yet implemented. Should it realy exist?");
            return null;
        }else{
            logger.warn("Unhandled synonym type: " + type.getLabel());
            return null;
        }
    }

	/**
	 * Returns the RelTaxonQualifierFk for a TaxonRelation.
	 */
	public static Integer taxonRelation2RelTaxonQualifierFk(RelationshipBase<?,?,?> relation) {
		if (relation == null || relation.getType() == null) {
			return null;
		}
		RelationshipTermBase<?> type = relation.getType();
		if (type.equals(TaxonRelationshipType.MISAPPLIED_NAME_FOR())) {
			return IS_MISAPPLIED_NAME_FOR;
		} else if (type.equals(TaxonRelationshipType.PRO_PARTE_MISAPPLIED_NAME_FOR())) {
            return IS_PRO_PARTE_MISAPPLIED_NAME_FOR;
        } else if (type.equals(TaxonRelationshipType.PRO_PARTE_SYNONYM_FOR())) {
		    return IS_PRO_PARTE_SYNONYM_OF;
		} else if (type.equals(TaxonRelationshipType.PARTIAL_SYNONYM_FOR())) {
            return IS_PARTIAL_SYNONYM_OF;
        } else if (type.equals(NameRelationshipType.BASIONYM())) {
			return IS_BASIONYM_FOR;
		} else if (type.equals(NameRelationshipType.LATER_HOMONYM())) {
			return IS_LATER_HOMONYM_OF;
		} else if (type.equals(NameRelationshipType.REPLACED_SYNONYM())) {
			return IS_REPLACED_SYNONYM_FOR;
		} else if (type.equals(NameRelationshipType.VALIDATED_BY_NAME())) {
			return IS_VALIDATION_OF;
		} else if (type.equals(NameRelationshipType.LATER_VALIDATED_BY_NAME())) {
			return IS_LATER_VALIDATION_OF;
		} else if (type.equals(NameRelationshipType.CONSERVED_AGAINST())) {
			return IS_CONSERVED_AGAINST;
		} else if (type.equals(NameRelationshipType.TREATED_AS_LATER_HOMONYM())) {
			return IS_TREATED_AS_LATER_HOMONYM_OF;
		} else if (type.equals(NameRelationshipType.ORTHOGRAPHIC_VARIANT())) {
			return IS_ORTHOGRAPHIC_VARIANT_OF;
//	    } else if (type.equals(NameRelationshipType.ORIGINAL_SPELLING())) {
//	        return IS_ORIGINAL_SPELLING_FOR;
        } else if (type.equals(NameRelationshipType.BLOCKING_NAME_FOR())) {
            return IS_BLOCKING_NAME_FOR;
		} else if (type.equals(NameRelationshipType.ALTERNATIVE_NAME())) {
			return IS_ALTERNATIVE_NAME_FOR;
        } else if (type.equals(NameRelationshipType.LATER_ISONYM())) {
            return IS_LATER_ISONYM_OF;
        } else if (type.equals(NameRelationshipType.MISSPELLING())) {
            return IS_MISSPELLING_FOR;
        } else if (type.equals(NameRelationshipType.EMENDATION())) {
            return HAS_EMENDATION;

		} else if (type.equals(HybridRelationshipType.FEMALE_PARENT())) {
			return IS_FEMALE_PARENT_OF;
		} else if (type.equals(HybridRelationshipType.MALE_PARENT())) {
			return IS_MALE_PARENT_OF;
		} else if (type.equals(HybridRelationshipType.FIRST_PARENT())) {
			return IS_FIRST_PARENT_OF;
		} else if (type.equals(HybridRelationshipType.SECOND_PARENT())) {
			return IS_SECOND_PARENT_OF;
		} else if (type.getUuid().equals(TaxonRelationshipType.uuidSynonymOfTaxonRelationship)) {
			return IS_SYNONYM_OF;
	    } else if (type.getUuid().equals(TaxonRelationshipType.uuidHeterotypicSynonymTaxonRelationship)) {
	        return IS_HETEROTYPIC_SYNONYM_OF;
	    } else if (type.getUuid().equals(TaxonRelationshipType.uuidHomotypicSynonymTaxonRelationship)) {
	        return IS_HOMOTYPIC_SYNONYM_OF;
		} else {
			logger.warn("No equivalent RelationshipType found in datawarehouse for: " + type.getTitleCache());
		}

		// The following have no equivalent attribute in CDM
//		IS_TYPE_OF
//		IS_CONSERVED_TYPE_OF
//		IS_REJECTED_TYPE_OF
//		IS_REJECTED_IN_FAVOUR_OF
//		HAS_SAME_TYPE_AS
//		IS_LECTOTYPE_OF
//		TYPE_NOT_DESIGNATED


		return null;
	}


	/**
	 * Returns the StatusFk for a given StatusCache.
	 * @param StatusCache
	 * @return
	 */
	public Integer statusCache2StatusFk(String StatusCache) {
		Integer result = null;
		if (StatusCache.equalsIgnoreCase("Checked by Taxonomic Editor: included in ERMS 1.1")) {
			return 0;
		} else if (StatusCache.equalsIgnoreCase("Added by Database Management Team")) {
			return 2;
		} else if (StatusCache.equalsIgnoreCase("Checked by Taxonomic Editor")) {
			return 3;
		} else if (StatusCache.equalsIgnoreCase("Edited by Database Management Team")) {
			return 4;
		} else {
			logger.error("StatusFk could not be determined. StatusCache unknown: " + StatusCache);
		}

		return result;
	}

	/**
	 * Returns the FossilStatusFk for a given FossilStatusCache.
	 * @param fossilStatusCache
	 * @return
	 */
	public Integer fossilStatusCache2FossilStatusFk(String fossilStatusCache) {
		Integer result = null;
		if (fossilStatusCache.equalsIgnoreCase("recent only")) {
			return 1;
		} else if (fossilStatusCache.equalsIgnoreCase("fossil only")) {
			return 2;
		} else if (fossilStatusCache.equalsIgnoreCase("recent + fossil")) {
			return 3;
		} else {
			logger.error("FossilStatusFk could not be determined. FossilStatusCache unknown: " + fossilStatusCache);
		}

		return result;
	}

	/**
	 * Returns the NoteCategoryFk for a given UUID representing an ExtensionType.
	 */
	public static Integer getNoteCategoryFk(UUID uuid) {
		Integer result = null;
		if (uuid.equals(uuidExtTaxComment)) {
			result = 270;
		} else if (uuid.equals(uuidExtFauComment)) {
			result = 281;
		} else if (uuid.equals(uuidExtFauExtraCodes)) {
			result = 278;
		}
		return result;
	}

	/**
	 * Returns the NoteCategoryCache for a given UUID representing an ExtensionType.
	 */
	public static String getNoteCategoryCache(UUID uuid) {
		String result = null;
		if (uuid.equals(uuidExtTaxComment)) {
			result = "Taxonomy";
		} else if (uuid.equals(uuidExtFauComment)) {
			result = "Biology";
		} else if (uuid.equals(uuidExtFauExtraCodes)) {
			result = "Distribution";
		}
		return result;
	}

	public static Integer getQualityStatusKeyBySource(EnumSet<PesiSource> sources, TaxonName taxonName) {
		if (sources.contains(PesiSource.EM)){
			return QUALITY_STATUS_ADD_BY_DBMT;
		}else if (sources.contains(PesiSource.ERMS)){
			Set<String> statusSet = getAllQualityStatus(taxonName);
			if (statusSet.size() > 1){
				logger.warn("ERMS TaxonName has more than 1 quality status: " + taxonName.getTitleCache() + "; lisd=" + taxonName.getLsid());
			}
			if (statusSet.contains("Checked by Taxonomic Editor: included in ERMS 1.1")){
				return QUALITY_STATUS_CHECKED_EDITOR_ERMS_1_1;
			}else if (statusSet.contains("Added by Database Management Team")){
				return QUALITY_STATUS_ADD_BY_DBMT;
			}else if (statusSet.contains("Checked by Taxonomic Editor")){
				return QUALITY_STATUS_CHECKED_EDITOR;
			}else if (statusSet.contains("Edited by Database Management Team")){
				return QUALITY_STATUS_EDITED_BY_DBMT;
			}else if (statusSet.contains("Added/edited by Thematic Editor")){
                return QUALITY_STATUS_THEMATIC_EDITOR;
			}else if (statusSet.isEmpty()) {
			    logger.warn("ERMS name has no quality status: " +taxonName.getTitleCache());
			    return null;
			}else{
				logger.warn("Unknown ERMS quality status: " + statusSet.iterator().next() + " for taxon name " + taxonName.getTitleCache());
				return null;
			}
		}else{
			return null;   // TODO needs to be implemented for others
		}
	}


	private static Set<String> getAllQualityStatus(TaxonName taxonName) {
		Set<String> result = new HashSet<>();
		for (TaxonBase<?> taxonBase : taxonName.getTaxonBases()){
			result.addAll(taxonBase.getExtensions(ErmsTransformer.uuidExtQualityStatus));
		}
		return result;
	}

	@Override
	public String getQualityStatusCacheByKey(Integer qualityStatusId) throws UndefinedTransformerMethodException {
		if (qualityStatusId == null){
			return null;
		}else{
			return this.qualityStatusCacheMap.get(qualityStatusId);
		}
	}

	public Object getSourceUseCacheByKey(Integer sourceUseFk) {
		if (sourceUseFk == null){
			return null;
		}else{
			return this.sourceUseCacheMap.get(sourceUseFk);
		}
	}

	//TODO create a reverse map
	public Integer getSourceUseKeyCacheByCache(String sourceUseCache) {
        if (sourceUseCache == null){
            return null;
        }else{
            for (Integer key: sourceUseCacheMap.keySet()){
                if (sourceUseCacheMap.get(key).equalsIgnoreCase(sourceUseCache)){
                    return key;
                }
            }
        }
        return null;
    }

	@Override
	public String getTaxonStatusCacheByKey(Integer taxonStatusId) throws UndefinedTransformerMethodException {
		if (taxonStatusId == null){
			return null;
		}else{
			return this.taxonStatusCacheMap.get(taxonStatusId);
		}
	}

	public static String getOriginalDbBySources(EnumSet<PesiSource> sources) {
		String result = "";
		if (sources.contains(PesiSource.EM)){
			result = CdmUtils.concat(",", result,  SOURCE_STR_EM);
		}
		if (sources.contains(PesiSource.FE)){
			result = CdmUtils.concat(",", result,  SOURCE_STR_FE);
		}
		if (sources.contains(PesiSource.IF)){
			result = CdmUtils.concat(",", result,  SOURCE_STR_IF);
		}
		if (sources.contains(PesiSource.ERMS)){
			result = CdmUtils.concat(",", result,  SOURCE_STR_ERMS);
		}
		return result;
	}

    public static Integer pesiKingdomId(String str) {
        return pesiKingdomMap.get(str);
    }
}
