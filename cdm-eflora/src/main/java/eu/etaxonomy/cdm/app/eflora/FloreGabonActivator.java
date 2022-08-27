/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.eflora;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.markup.MarkupTransformer;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PolytomousKey;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 * @since 20.06.2008
 */
public class FloreGabonActivator extends EfloraActivatorBase {

	private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
//	static final URI source = EfloraSources.fdg_sample();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flore_gabon_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flore_gabon_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("ee688973-2595-4d4d-b11e-6df71e96a5c2");
	private static final String featureTreeTitle = "Flore Gabon Presentation Feature Tree";

	//classification
	static final UUID classificationUuid = UUID.fromString("2f892452-ff49-48cf-834f-52ca29600719");
	static final String classificationTitle = "Flore du Gabon";

	//check - import
	private boolean h2ForCheck = false;
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;

	private boolean replaceStandardKeyTitles = true;

	//taxa
	static final boolean doTaxa = true;

	static final boolean reuseState = true;

	//if true, use inverse include information
	private boolean inverseInclude = true;

	private boolean includeFdg12_17 = false;  //2236  1h17  803t (Rubiaceae1+2)
	private boolean includeFdg36_37 = true;  //2022  1h8   1627t(824)
    private boolean includeFdg16 = true ;  //1355   0h52  2078t (451)
    private boolean includeFdg29 = true;   //1442   2h09  2505t (427)
    private boolean includeFdg26 = true;   //1290   42m   3044 (539)
    private boolean includeFdg15 = true;   //1228     1h55  3547 (503)
    private boolean includeFdg44 = true;   //1176     32m   3974 (427)
    private boolean includeFdg20 = true;   //1045    51m    4301 (327)

    private boolean includeFdg13 = true;   //960     33m    4660 (359)
    private boolean includeFdg5 = true;    //959  27m   5375 (715)
    private boolean includeFdg5bis = true; //666   //addenda not yet implemented
    private boolean includeFdg31 = true;   //938   39m     5727 (352)
    private boolean includeFdg23 = true;   //929   47m    6054 (327)
    private boolean includeFdg24 = true;   //917  29m   6313 (257)
    private boolean includeFdg32 = true;   //917  40m  6506 (193)
    private boolean includeFdg8 = true;    //896 27m  7037(?) (531)
    private boolean includeFdg19 = true;   //735  23m  7196   (159)

    private boolean includeFdg18 = true;   //676  21m    7366 (170)
    private boolean includeFdg33 = true;   //611 20m   7532 (166)
    private boolean includeFdg25 = true;   //592 28m   7694 (162)
    private boolean includeFdg9 = true;    //572 27m   7992 (298), 3564
    private boolean includeFdg2 = true;    //486 19m  8157(165) , 3656(92)
    private boolean includeFdg43 = true;   //483  20m 8282(125), 3739(83)
    private boolean includeFdg42 = true;   //480  17m 8464(182) , 3824(85)
    private boolean includeFdg34 = true;   //467  10m 8622(168) , 3877(53)
    private boolean includeFdg35 = true;   //460  18m 8729(107), 3944(67)
    private boolean includeFdg45 = true;   //442  17m 8875(146), 4008(64)
    private boolean includeFdg1 = true;    //425  17m 9067(192), 4105(97)
    private boolean includeFdg14 = true;   //414  13m 9140(73), 4136(31)
    private boolean includeFdg30 = true;   //399  16m 9251(111), 4185(49)
    private boolean includeFdg10 = true;   //394 15m  9461(210), 4326(131)
    private boolean includeFdg6 = true;    //369  15m 9624(163), 4394(68)
    private boolean includeFdg11 = true;   //353  18m 9784(160), 4478(84)
    private boolean includeFdg39 = true;   //352  16m 9873(89), 4545(67)

    private boolean includeFdg22 = true;   //330  14m 10058(85), 4593(48)
    private boolean includeFdg21 = true;   //314 17m 10189(131), 4668(75)
    private boolean includeFdg41 = true;   //275 14m 10294(105), 4720(52)
    private boolean includeFdg40 = true;   //273 15m 10386(92), 4773(53)
    private boolean includeFdg3 = true;    //270 12m 10487(101) 4818(45)
    private boolean includeFdg7 = true;   //246 15m 10584(97) 4876(58)
    private boolean includeFdg27 = true;  //244 12m 10707(123), 4916(40)
    private boolean includeFdg38 = true;   //242 13m 10780(73), 4966(50)
    private boolean includeFdg28 = true;   //235 10m 10907(127)  5000(34)
    private boolean includeFdg4 = true;   //227 16m 10973(66), 5036(36)

    static final URI fdg1 = EfloraSources.fdg_1();
    static final URI fdg2 = EfloraSources.fdg_2();
    static final URI fdg3 = EfloraSources.fdg_3();
    static final URI fdg4 = EfloraSources.fdg_4();
    static final URI fdg5 = EfloraSources.fdg_5();
    static final URI fdg5bis = EfloraSources.fdg_5bis();
    static final URI fdg6 = EfloraSources.fdg_6();
    static final URI fdg7 = EfloraSources.fdg_7();
    static final URI fdg8 = EfloraSources.fdg_8();
    static final URI fdg9 = EfloraSources.fdg_9();
    static final URI fdg10 = EfloraSources.fdg_10();
    static final URI fdg11 = EfloraSources.fdg_11();
    static final URI fdg12_17 = EfloraSources.fdg_12_17();
    static final URI fdg13 = EfloraSources.fdg_13();
    static final URI fdg14 = EfloraSources.fdg_14();
    static final URI fdg15 = EfloraSources.fdg_15();
    static final URI fdg16 = EfloraSources.fdg_16();
    static final URI fdg18 = EfloraSources.fdg_18();
    static final URI fdg19 = EfloraSources.fdg_19();
    static final URI fdg20 = EfloraSources.fdg_20();
    static final URI fdg21 = EfloraSources.fdg_21();
    static final URI fdg22 = EfloraSources.fdg_22();
    static final URI fdg23 = EfloraSources.fdg_23();
    static final URI fdg24 = EfloraSources.fdg_24();
    static final URI fdg25 = EfloraSources.fdg_25();
    static final URI fdg26 = EfloraSources.fdg_26();
    static final URI fdg27 = EfloraSources.fdg_27();
    static final URI fdg28 = EfloraSources.fdg_28();
    static final URI fdg29 = EfloraSources.fdg_29();
    static final URI fdg30 = EfloraSources.fdg_30();
    static final URI fdg31 = EfloraSources.fdg_31();
    static final URI fdg32 = EfloraSources.fdg_32();
    static final URI fdg33 = EfloraSources.fdg_33();
    static final URI fdg34 = EfloraSources.fdg_34();
    static final URI fdg35 = EfloraSources.fdg_35();
    static final URI fdg36_37 = EfloraSources.fdg_36_37();
    static final URI fdg38 = EfloraSources.fdg_38();
    static final URI fdg39 = EfloraSources.fdg_39();
    static final URI fdg40 = EfloraSources.fdg_40();
    static final URI fdg41 = EfloraSources.fdg_41();
    static final URI fdg42 = EfloraSources.fdg_42();
    static final URI fdg43 = EfloraSources.fdg_43();
    static final URI fdg44 = EfloraSources.fdg_44();
    static final URI fdg45 = EfloraSources.fdg_45();

// **************** NO CHANGE **********************************************/

	private void doImport(ICdmDataSource cdmDestination){
		super.doImport(fdg1, cdmDestination, check, h2ForCheck);

		//make config
		config.setClassificationUuid(classificationUuid);
		config.setDoTaxa(doTaxa);
		config.setDoPrintKeys(doPrintKeys);
		config.setDbSchemaValidation(hbm2dll);
		config.setReplaceStandardKeyTitles(replaceStandardKeyTitles);
		config.setSourceReference(getSourceReference("Flore du Gabon"));
		config.setClassificationName(classificationTitle);
		config.setReuseExistingState(reuseState);

		//Vol1
		executeVolume( fdg1, includeFdg1 ^ inverseInclude);

		//Vol2
		executeVolume(fdg2, includeFdg2 ^ inverseInclude);

		//Vol3
		executeVolume(fdg3, includeFdg3 ^ inverseInclude);

		//Vol4
		executeVolume(fdg4, includeFdg4 ^ inverseInclude);

		//Vol5
		executeVolume(fdg5, includeFdg5 ^ inverseInclude);

		//Vol5bis
		executeVolume(fdg5bis, includeFdg5bis ^ inverseInclude);

		//Vol6
		executeVolume(fdg6, includeFdg6 ^ inverseInclude);

		//Vol7
		executeVolume(fdg7, includeFdg7 ^ inverseInclude);

		//Vol8
		executeVolume(fdg8, includeFdg8 ^ inverseInclude);

		//Vol9
		executeVolume(fdg9, includeFdg9 ^ inverseInclude);

		//Vol10
		executeVolume(fdg10, includeFdg10 ^ inverseInclude);

		//Vol11
		executeVolume(fdg11, includeFdg11 ^ inverseInclude);

		//Vol12
		executeVolume(fdg12_17, includeFdg12_17 ^ inverseInclude);

		//Vol13
		executeVolume(fdg13, includeFdg13 ^ inverseInclude);

		//Vol14
		executeVolume(fdg14, includeFdg14 ^ inverseInclude);

		//Vol15
		executeVolume(fdg15, includeFdg15 ^ inverseInclude);

		//Vol16
		executeVolume(fdg16, includeFdg16 ^ inverseInclude);

		//Vol18
		executeVolume(fdg18, includeFdg18 ^ inverseInclude);

		//Vol19
		executeVolume(fdg19, includeFdg19 ^ inverseInclude);

		//Vol20
		executeVolume(fdg20, includeFdg20 ^ inverseInclude);

		//Vol21
		executeVolume(fdg21, includeFdg21 ^ inverseInclude);
		//Vol22
		executeVolume(fdg22, includeFdg22 ^ inverseInclude);
		//Vol23
		executeVolume(fdg23, includeFdg23 ^ inverseInclude);
		//Vol24
		executeVolume(fdg24, includeFdg24 ^ inverseInclude);
		//Vol25
		executeVolume(fdg25, includeFdg25 ^ inverseInclude);
		//Vol26
		executeVolume(fdg26, includeFdg26 ^ inverseInclude);
		//Vol27
		executeVolume(fdg27, includeFdg27 ^ inverseInclude);
		//Vol28
		executeVolume(fdg28, includeFdg28 ^ inverseInclude);
		//Vol29
		executeVolume(fdg29, includeFdg29 ^ inverseInclude);
		//Vol30
		executeVolume(fdg30, includeFdg30 ^ inverseInclude);
		//Vol31
		executeVolume(fdg31, includeFdg31 ^ inverseInclude);
		//Vol32
		executeVolume(fdg32, includeFdg32 ^ inverseInclude);
		//Vol33
		executeVolume(fdg33, includeFdg33 ^ inverseInclude);
		//Vol34
		executeVolume(fdg34, includeFdg34 ^ inverseInclude);
		//Vol35
		executeVolume(fdg35, includeFdg35 ^ inverseInclude);

		//Vol 36_37
		executeVolume(fdg36_37, includeFdg36_37 ^ inverseInclude);

		//Vol38
		executeVolume(fdg38, includeFdg38 ^ inverseInclude);

		//Vol39
		executeVolume(fdg39, includeFdg39 ^ inverseInclude);

		//Vol40
		executeVolume(fdg40, includeFdg40 ^ inverseInclude);

		//Vol41
		executeVolume(fdg41, includeFdg41 ^ inverseInclude);

		//Vol42
		executeVolume(fdg42, includeFdg42 ^ inverseInclude);

		//Vol43
		executeVolume(fdg43, includeFdg43 ^ inverseInclude);

		//Vol44
		executeVolume(fdg44, includeFdg44 ^ inverseInclude);

		//Vol45
		executeVolume(fdg45, includeFdg45 ^ inverseInclude);

//		TermTree<Feature> tree = makeFeatureNode(myImport.getCdmAppController().getTermService());
//		myImport.getCdmAppController().getTermTreeService().saveOrUpdate(tree);
//
//		makeAutomatedFeatureTree(myImport.getCdmAppController(), config.getState(),
//				featureTreeUuid, featureTreeTitle);

		//check keys
		if (doPrintKeys){
			TransactionStatus tx = myImport.getCdmAppController().startTransaction();
			List<PolytomousKey> keys = myImport.getCdmAppController().getPolytomousKeyService().list(PolytomousKey.class, null, null, null, null);
			for(PolytomousKey key : keys){
				key.print(System.out);
				System.out.println();
			}
			myImport.getCdmAppController().commitTransaction(tx);
		}

	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private TermTree<Feature> makeFeatureNode(ITermService service){
		MarkupTransformer transformer = new MarkupTransformer();

		TermTree<Feature> result = TermTree.NewFeatureInstance();
		result.setTitleCache("Old feature tree", true);
		TermNode<Feature> root = result.getRoot();

		TermNode<Feature> newNode = root.addChild(Feature.DESCRIPTION());

		addFeatureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

		addFeatureNodesByStringList(generellDescriptionsUpToAnatomyList, root, transformer, service);
		newNode = root.addChild(Feature.ANATOMY());  //not sure if this is correct, but it looked like the node was orphaned before
        addFeatureNodesByStringList(anatomySubfeatureList, newNode, transformer, service);

		newNode = addFeatureNodesByStringList(generellDescriptionsFromAnatomyToPhytoChemoList, root, transformer, service);
		addFeatureNodesByStringList(phytoChemoSubFeaturesList, newNode, transformer, service);

		newNode = addFeatureNodesByStringList(generellDescriptionsFromPhytoChemoList, root, transformer, service);

		newNode = root.addChild(Feature.COMMON_NAME());

		newNode = root.addChild(Feature.DISTRIBUTION());

		newNode = root.addChild(Feature.ECOLOGY());
		addFeatureNodesByStringList(habitatEcologyList, root, transformer, service);

		newNode = root.addChild(Feature.USES());

		addFeatureNodesByStringList(chomosomesList, root, transformer, service);

		newNode = root.addChild(Feature.CITATION());

		return result;
	}

	private static String [] chomosomesList = new String[]{
		"Chromosomes",
	};


	private static String [] habitatEcologyList = new String[]{
		"Habitat",
		"Habitat & Ecology"
	};


	private static String [] generellDescriptionsUpToAnatomyList = new String[]{
		"Fossils",
		"Morphology and anatomy",
		"Morphology",
		"Vegetative morphology and anatomy",
	};


	private static String [] anatomySubfeatureList = new String[]{
		"Leaf anatomy",
		"Wood anatomy"
	};

	private static String [] generellDescriptionsFromAnatomyToPhytoChemoList = new String[]{
		"Flower morphology",
		"Palynology",
		"Pollination",
		"Pollen morphology",
		"embryology",
		"cytology",
		"Life cycle",
		"Fruits and embryology",
		"Dispersal",
		"Chromosome numbers",
		"Phytochemistry and Chemotaxonomy",
	};


	private static String [] phytoChemoSubFeaturesList = new String[]{
		"Alkaloids",
		"Iridoid glucosides",
		"Leaf phenolics",
		"Storage products of seeds",
		"Aluminium",
		"Chemotaxonomy",
	};


	private static String [] generellDescriptionsFromPhytoChemoList = new String[]{
		"Phytochemistry",
		"Taxonomy",
		"history",
		"cultivation",
		"Notes"
	};


	private static String [] descriptionFeatureList = new String[]{
		"lifeform",
		"Juvenile parts",
		"Bark",
		//new
		"wood",
		"Indumentum",
		"endophytic body",
		"apical buds",
		"flowering buds",
		"Branchlets",
		"Branches",
		"Branch",
		"Flowering branchlets",
		"Trees",
		"Twigs",
		"stem",
		"Stems",
		"stem leaves",
		"Leaves",
		"extraxylary sclerenchyma",
		"flower-bearing stems",
		"Petiole",
		"Petiolules",
		"Leaflets",
		"Lamina",
		"Veins",
		"Lateral veins",
		"secondary veins",
		"Intersecondary veins",
		"veinlets",
		"Thyrsus",
		"Thyrses",
		"Inflorescences",
		"Inflorescence",
		"Young inflorescences",
		"Male inflorescences",
		"Female inflorescences",
		"rachises",
		"Bracts",
		"Pedicels",
		"flowering buds",
		"scales",
		"Buds",
		"Flowers",
		"Flower",
		"Flowering",
		"Stigma",
		"perianth",
		"Sepals",
		"Sepal",
		"Outer Sepals",
		"Axillary",
		"cymes",
		"Calyx",
		"Androgynophore",
		"Petal",
		"Petals",
		"perigone",
		"perigone lobes",
		"perigone tube",
		"Disc",
		"corolla",
		"Stamens",
		"Staminodes",
		"Ovary",
		"Anthers",
		"anther",
		"Pistil",
		"Pistillode",
		"Ovules",
		"androecium",
		"gynoecium",
		"Filaments",
		"Style",
		"annulus",
		"female flowers",
		"Male flowers",
		"Androphore",
		"Female",
		"Infructescences",    //order not consistent (sometimes before "Flowers")
		"Fruit",
		"Fruits",
		"fruiting axes",
		"drupes",
		"Arillode",
		"seed",
		"Seeds",
		"Seedling",
		"flower tube",
		"nutlets",
		"pollen",
		"secondary xylem",
		"chromosome number",

		"figure",
		"fig",
		"figs",
	};

	public TermNode<Feature> addFeatureNodesByStringList(String[] featureStringList,
	             TermNode<Feature> root, IInputTransformer transformer, ITermService termService){

	    TermNode<Feature> lastChild = null;
		try {
			for (String featureString : featureStringList){
				UUID featureUuid;
				featureUuid = transformer.getFeatureUuid(featureString);
				Feature feature = (Feature)termService.find(featureUuid);
				if (feature != null){
					root.addChild(feature);
				}
			}

		} catch (UndefinedTransformerMethodException e) {
			logger.error("getFeatureUuid is not implemented in transformer. Features could not be added");
		}
		return lastChild;
	}

	public static void main(String[] args) {
	    logger.warn("start");
		FloreGabonActivator me = new FloreGabonActivator();
		try {
            me.doImport(cdmDestination);
        } catch (Exception e) {
            logger.warn("Uncatched exception: " + e.getMessage());
            e.printStackTrace();
        }
		logger.warn("Finish");
		System.exit(0);
	}
}