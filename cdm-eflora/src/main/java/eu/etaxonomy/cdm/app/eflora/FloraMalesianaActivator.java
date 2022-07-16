/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.eflora;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.events.IIoObserver;
import eu.etaxonomy.cdm.io.common.events.LoggingIoObserver;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.eflora.floraMalesiana.FloraMalesianaTransformer;
import eu.etaxonomy.cdm.io.markup.MarkupImportConfigurator;
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
public class FloraMalesianaActivator extends EfloraActivatorBase {

    private static Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_malesiana_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_malesiana_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();

	private final boolean includeBase = false;

	//5880 / 121834
	//2 footnote issues
	//habitat: 6 syso, + ???
	private final boolean includeVol04 = includeBase;
	//6258  / 110625
	//11 sysos, + ??
	private final boolean includeVol05 = includeBase;
	//8097   / 177845
	//2 sysos (<sub>
	//102 keys
    private final boolean includeVol06 = includeBase;

	//2283 /49700
	//no syso, 120 lifecycle, few of them references! and figureRef?
	private final boolean includeVol07_1 = includeBase;
	//1640
	//keys: 1 nothofagus; 4 quercus
//12 syso, no uncovered, >>100 lifecycle, >10 references, figureRefs?
    private final boolean includeVol07_2 = includeBase;
    //4225
 //430 syso (10 subheadings),no uncovered, 43 figureRef, references and lifecycle
    private final boolean includeVol07_3_09_1 = includeBase;
    //561   /   12440
    //no syso, 14 lifecycle, 1 reference
    private final boolean includeVol07_4 = includeBase;

    //223   /   5986
    //no syso, 5 lifecycle
    private final boolean includeVol08_1 = includeBase;
    //2830   /   53189
    //keys: 23 S. Moore -> Symplocos Moore (handle manually before import)
 //4syso, <= uncovered, <150 lifecycle, figureRef and references
    private final boolean includeVol08_2 = includeBase;
    //2912    /   54235
    //no syso
	private final boolean includeVol08_3 = includeBase;
	//4641    /   94566
 //184 syso!! (30 subheadings), <=3 uncovered, <50 lifecycle and figureRef
	private final boolean includeVol09 = includeBase;
	//845   / 19098
	//no syso, <= 5 uncovered, <=50 lifecycle
	private final boolean includeVol10_1 = includeBase;
	//1211  / 32334
//6 syso, <=3 uncovered, <50 lifecycle
    private final boolean includeVol10_2 = includeBase;
    //1993   /  49034
//2 syso habitats, 2 fast, many lifcycle
	private final boolean includeVol10_3 = includeBase;
	//533    /   13553
	//no syso, 1 uncovered, 1 figureRef, 10 lifecycle
	private final boolean includeVol10_4 = includeBase;
	//1529     /  36379
	//keys: 1 manually (a sect. botrycephalae)
	//no syso, <= 10 uncovered, < 100 lifecycle
	private final boolean includeVol11_1 = includeBase;
	//1271   /  24775
	//keys: 2 cultivar groups (a cultivar group aggregatum; b cultivar group common onion)
	//no syso, 1 uncovered, 1 lifecycle, 2 references
	private final boolean includeVol11_2 = includeBase;
	//4582    /  52747
	//1 empty "[]"
	//no syso habitat, <10 uncovered, > 100 lifecycles
	private final boolean includeVol11_3 = includeBase;
	//6135   /   116737
	//keys: 10 apostrophs
	//habitat: no syso, 200 - 400 uncovered, different types
	private final boolean includeVol12 = includeBase;
	//2865    /   43764
	//keys: 1 manual (Loranthaceae)
	//no syso, 1 uncovered habitat
	private final boolean includeVol13 = includeBase;
	//4843   /   55501
	//no syso habitat, 100-150 uncovered, most of them flowering and fruiting lifecycle
	private final boolean includeVol14 = includeBase;
	//1775   /   16415
	//no syso, 5 uncovered, 2 of them epiphytic
	private final boolean includeVol15 = includeBase;
	//1471   /   22049
	//no syso, <= 60 uncovered, very different types
	private final boolean includeVol16 = includeBase;

	//4113    /   116956
	//no syso, medium # uncovered habitat (some or)
	private final boolean includeVol17 = includeBase;
	//6422    /   71151
	//no syso habitat, <20 uncovered
	private final boolean includeVol18 = includeBase;
	//2612    /   27663
    //no syso habitat, <= 50 uncovered, mostly untagged lifecycle, some errors
    private final boolean includeVol19 = includeBase;
    //563    /    5866
    //no syso, < 20 uncovered, mostly untagged lifecycle
	private final boolean includeVol20 = includeBase;
	//1735     /   19469
	//no syso, no covered, medium # lifecycle and very few references
	private final boolean includeVol21 = ! includeBase;

//Ser II
	//5798  / 123.627
	//2 manually, 9 non standard(?), 2 hybrids(?)
	//no syso habitat, only figureRef and references as habitat issues
    private final boolean includeVol2_1 = includeBase;
    //1054   / 25100
    //2 hybrid formulas
    //no syso, no habitat issue
    private final boolean includeVol2_2 = includeBase;
    //2382  / 52100
    //no syso, 10-15 uncovered, 2 unexpected attributes: extra
	private final boolean includeVol2_3 = includeBase;
	//1045  / 12240
	//no syso, 1 <br>
	private final boolean includeVol2_4 = includeBase;


	static final URI fmSource04 = EfloraSources.fm_04();
	static final URI fmSource05 = EfloraSources.fm_05();
	static final URI fmSource06 = EfloraSources.fm_06();
    static final URI fmSource07_1 = EfloraSources.fm_07_1();
    static final URI fmSource07_2 = EfloraSources.fm_07_2();
    static final URI fmSource07_3_09_1 = EfloraSources.fm_07_3_09_1();
    static final URI fmSource07_4 = EfloraSources.fm_07_4();

    static final URI fmSource08_1 = EfloraSources.fm_08_1();
    static final URI fmSource08_2 = EfloraSources.fm_08_2();
    static final URI fmSource08_3 = EfloraSources.fm_08_3();

    static final URI fmSource09 = EfloraSources.fm_09();

    static final URI fmSource10_1 = EfloraSources.fm_10_1();
    static final URI fmSource10_2 = EfloraSources.fm_10_2();
    static final URI fmSource10_3 = EfloraSources.fm_10_3();
    static final URI fmSource10_4 = EfloraSources.fm_10_4();


    static final URI fmSource11_1 = EfloraSources.fm_11_1();
    static final URI fmSource11_2 = EfloraSources.fm_11_2();
    static final URI fmSource11_3 = EfloraSources.fm_11_3();
    static final URI fmSource12_1 = EfloraSources.fm_12();
    static final URI fmSource13 = EfloraSources.fm_13();
    static final URI fmSource14 = EfloraSources.fm_14();
    static final URI fmSource15 = EfloraSources.fm_15();
    static final URI fmSource16 = EfloraSources.fm_16();
    static final URI fmSource17 = EfloraSources.fm_17();
    static final URI fmSource17_1 = EfloraSources.fm_17_1();
    static final URI fmSource17_2 = EfloraSources.fm_17_2();
    static final URI fmSource18 = EfloraSources.fm_18();
    static final URI fmSource19 = EfloraSources.fm_19();
    static final URI fmSource20 = EfloraSources.fm_20();
    static final URI fmSource21 = EfloraSources.fm_21();

    static final URI fmSource_Ser2_01 = EfloraSources.fm_ser2_1();
    static final URI fmSource_Ser2_02 = EfloraSources.fm_ser2_2();
    static final URI fmSource_Ser2_03 = EfloraSources.fm_ser2_3();
    static final URI fmSource_Ser2_04 = EfloraSources.fm_ser2_4();

	private final boolean h2ForCheck = true;

	static final boolean reuseState = true;  //when running multiple imports

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("168df0c6-6429-484c-b26f-ded1f7e44bd9");
	private static final String featureTreeTitle = "Flora Malesiana Presentation Feature Tree";

	//classification
	static final UUID classificationUuid = UUID.fromString("ca4e4bcb-a1d1-4124-a358-a3d3c41dd450");
	static final String classificationTitle = "Flora Malesiana";

	//check - import
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;

	//taxa
	static final boolean doTaxa = true;


	private final boolean replaceStandardKeyTitles = false;

	private final IIoObserver observer = new LoggingIoObserver();
	private final Set<IIoObserver> observerList = new HashSet<IIoObserver>();


	private void doImport(ICdmDataSource cdmDestination){
		observerList.add(observer);
		if (h2ForCheck && cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
			check = CHECK.CHECK_ONLY;
		}

		//make Source
//		URI source = fmSource13_small;
		URI source = null;

		MarkupImportConfigurator markupConfig= MarkupImportConfigurator.NewInstance(source, cdmDestination);
		markupConfig.setClassificationUuid(classificationUuid);
		markupConfig.setClassificationName(classificationTitle);
		markupConfig.setDoTaxa(doTaxa);
		markupConfig.setCheck(check);
		markupConfig.setDoPrintKeys(doPrintKeys);
		markupConfig.setDbSchemaValidation(hbm2dll);
		markupConfig.setObservers(observerList);
		markupConfig.setReplaceStandardKeyTitles(replaceStandardKeyTitles);
		markupConfig.setReuseExistingState(reuseState);

		markupConfig.setSourceReference(getSourceReference("Flora Malesiana - Vol. 13"));

		CdmDefaultImport<MarkupImportConfigurator> myImport = new CdmDefaultImport<MarkupImportConfigurator>();

        //Vol04
        doSource(includeVol04, fmSource04, "Flora Malesiana - vol. 4", markupConfig, myImport);
        //Vol05
        doSource(includeVol05, fmSource05, "Flora Malesiana - vol. 5", markupConfig, myImport);
        //Vol06
        doSource(includeVol06, fmSource06, "Flora Malesiana - vol. 6", markupConfig, myImport);
        //Vol07_1
        doSource(includeVol07_1, fmSource07_1, "Flora Malesiana - vol. 7, pt.1", markupConfig, myImport);
        //Vol07_2
        doSource(includeVol07_2, fmSource07_2, "Flora Malesiana - vol. 7, pt.2", markupConfig, myImport);
        //Vol07_3
        doSource(includeVol07_3_09_1, fmSource07_3_09_1, "Flora Malesiana - vol. 7, pt.3 and vol. 9 pt.1", markupConfig, myImport);
        //Vol07_4
        doSource(includeVol07_4, fmSource07_4, "Flora Malesiana - vol. 7, pt.4", markupConfig, myImport);

		//Vol08_1
		doSource(includeVol08_1, fmSource08_1, "Flora Malesiana - vol. 8, pt.1", markupConfig, myImport);

		//Vol08_2
		doSource(includeVol08_2, fmSource08_2, "Flora Malesiana - vol. 8, pt.2", markupConfig, myImport);

		//Vol08_3
		doSource(includeVol08_3, fmSource08_3, "Flora Malesiana - vol. 8, pt.3", markupConfig, myImport);

		//Vol09
        doSource(includeVol09, fmSource09, "Flora Malesiana - vol. 9", markupConfig, myImport);

		//Vol10_1
		doSource(includeVol10_1, fmSource10_1, "Flora Malesiana - vol. 10, pt.1", markupConfig, myImport);

		//Vol10_2
		doSource(includeVol10_2, fmSource10_2, "Flora Malesiana - vol. 10, pt.2", markupConfig, myImport);

		//Vol10_3
		doSource(includeVol10_3, fmSource10_3, "Flora Malesiana - vol. 10, pt.3", markupConfig, myImport);

		//Vol10_4
		doSource(includeVol10_4, fmSource10_4, "Flora Malesiana - vol. 10, pt.4", markupConfig, myImport);

		//Vol11_1
		doSource(includeVol11_1, fmSource11_1, "Flora Malesiana - vol. 11, pt.1", markupConfig, myImport);

		//Vol11_2
		doSource(includeVol11_2, fmSource11_2, "Flora Malesiana - vol. 11, pt.2", markupConfig, myImport);

		//Vol11_3
		doSource(includeVol11_3, fmSource11_3, "Flora Malesiana - vol. 11, pt.3", markupConfig, myImport);

		//Vol12_1
		doSource(includeVol12, fmSource12_1, "Flora Malesiana - vol. 12", markupConfig, myImport);

		//Vol13_large
		doSource(includeVol13, fmSource13, "Flora Malesiana - vol. 13", markupConfig, myImport);

		//Vol14
		doSource(includeVol14, fmSource14, "Flora Malesiana - vol. 14", markupConfig, myImport);

		//Vol15
		doSource(includeVol15, fmSource15, "Flora Malesiana - vol. 15", markupConfig, myImport);

		//Vol16
		doSource(includeVol16, fmSource16, "Flora Malesiana - vol. 16", markupConfig, myImport);

		//Vol17, part1+2
        doSource(includeVol17, fmSource17, "Flora Malesiana - vol. 17, part I and II", markupConfig, myImport);

//		//Vol17, part1
//		doSource(includeVol17_1, fmSource17_1, "Flora Malesiana - vol. 17, part I", markupConfig, myImport);
//
//		//Vol17, part2
//		doSource(includeVol17_2, fmSource17_2, "Flora Malesiana - vol. 17, part II", markupConfig, myImport);

		//Vol18
		doSource(includeVol18, fmSource18, "Flora Malesiana - vol. 18", markupConfig, myImport);

		//Vol19
		doSource(includeVol19, fmSource19, "Flora Malesiana - vol. 19", markupConfig, myImport);

		//Vol20
		doSource(includeVol20, fmSource20, "Flora Malesiana - vol. 20", markupConfig, myImport);

		//Vol21
		doSource(includeVol21, fmSource21, "Flora Malesiana - vol. 21", markupConfig, myImport);

        //Vol_2_1
        doSource(includeVol2_1, fmSource_Ser2_01, "Flora Malesiana - Ser.2, vol. 1", markupConfig, myImport);

		//Vol_2_2
		doSource(includeVol2_2, fmSource_Ser2_02, "Flora Malesiana - Ser.2, vol. 2", markupConfig, myImport);

		//Vol_2_3
		doSource(includeVol2_3, fmSource_Ser2_03, "Flora Malesiana - Ser.2, vol. 3", markupConfig, myImport);

		//Vol_2_3
		doSource(includeVol2_4, fmSource_Ser2_04, "Flora Malesiana - Ser.2, vol. 4", markupConfig, myImport);


		makeAutomatedFeatureTree(myImport.getCdmAppController(), markupConfig.getState(),
				featureTreeUuid, featureTreeTitle);

//		makeGeoService();

		TermTree<Feature> tree = makeFeatureNode(myImport.getCdmAppController().getTermService());
		myImport.getCdmAppController().getTermTreeService().saveOrUpdate(tree);

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

	private void doSource(boolean doInclude, URI source, String sourceTitle, MarkupImportConfigurator markupConfig,
			CdmDefaultImport<MarkupImportConfigurator> myImport) {
		if (doInclude){
			System.out.println("\nStart import from ("+ source.toString() + ") ...");
			markupConfig.setSource(source);
			markupConfig.setSourceReference(getSourceReference(sourceTitle));
			myImport.invoke(markupConfig);
			System.out.println("End import from ("+ source.toString() + ")...");
		}
	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private TermTree<Feature> makeFeatureNode(ITermService service){
		FloraMalesianaTransformer transformer = new FloraMalesianaTransformer();

		TermTree<Feature> result = TermTree.NewFeatureInstance(UUID.randomUUID());
		result.setTitleCache("Flora Malesiana Presentation Feature Tree - Old", true);
		TermNode<Feature> root = result.getRoot();

		TermNode<Feature> newNode = root.addChild(Feature.DESCRIPTION());

		addFeatureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

		addFeatureNodesByStringList(generellDescriptionsUpToAnatomyList, root, transformer, service);
		newNode = root.addChild(Feature.ANATOMY());  //not sure if this is correct, but it looked like the node was orphaned before
		addFeatureNodesByStringList(anatomySubfeatureList, newNode, transformer, service);

		newNode = addFeatureNodesByStringList(generellDescriptionsFromAnatomyToPhytoChemoList, root, transformer, service);
		addFeatureNodesByStringList(phytoChemoSubFeaturesList, newNode, transformer, service);

		newNode = addFeatureNodesByStringList(generellDescriptionsFromPhytoChemoList, root, transformer, service);


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
		"Bark",
		//new
		"wood",
		"Indumentum",
		"endophytic body",
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
		"flower-bearing stems",
		"Petiole",
		"Petiolules",
		"Leaflets",
		"Thyrsus",
		"Thyrses",
		"Inflorescences",
		"Inflorescence",
		"Young inflorescences",
		"Male inflorescences",
		"Female inflorescences",
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
		FloraMalesianaActivator me = new FloraMalesianaActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}
}