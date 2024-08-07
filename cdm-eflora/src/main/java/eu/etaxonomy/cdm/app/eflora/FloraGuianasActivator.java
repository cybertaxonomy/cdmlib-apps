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
 * Activator for Flora of the Guianas imports.
 * This class is meant for advanced use and is therefore not documented.
 * @author a.mueller
 */
public class FloraGuianasActivator extends EfloraActivatorBase {

    private static Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI fgu1 = EfloraSources.fgu_1();

	static final URI fotg03 = EfloraSources.fotg_03();

	static final URI fotg11 = EfloraSources.fotg_11();

	static final URI fotg14 = EfloraSources.fotg_14();
	static final URI fotg15 = EfloraSources.fotg_15();
	static final URI fotg16 = EfloraSources.fotg_16();

	static final URI fotg20 = EfloraSources.fotg_20();

	static final URI fotg22 = EfloraSources.fotg_22();
	static final URI fotg23 = EfloraSources.fotg_23();
	static final URI fotg24 = EfloraSources.fotg_24();
	static final URI fotg24_plus = EfloraSources.fotg_24plus();
	static final URI fotg25 = EfloraSources.fotg_25();
	static final URI fotg25_plus = EfloraSources.fotg_25plus();
	static final URI fotg26 = EfloraSources.fotg_26();
	static final URI fotg27 = EfloraSources.fotg_27();

	static final URI fotg29 = EfloraSources.fotg_29();
	static final URI fotg30 = EfloraSources.fotg_30();

	private boolean inverseInclude = false;

	private boolean includeFotg1 = false;

	private boolean includeFotg03 = true;
	private boolean includeFotg11 = false;
	private boolean includeFotg14 = false;
	private boolean includeFotg15 = false;
	private boolean includeFotg16 = false;
	private boolean includeFotg20 = false;
	private boolean includeFotg22 = false;
	private boolean includeFotg23 = false;
	private boolean includeFotg24 = false;
	private boolean includeFotg24_plus = false;
	private boolean includeFotg25 = false;
	private boolean includeFotg25_plus = false;
	private boolean includeFotg26 = false;
	private boolean includeFotg27 = false;

	private boolean includeFotg29 = false;
	private boolean includeFotg30 = false;


//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_guianas_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_guianas_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();


	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("2be99595-92fc-4f80-b9c4-b48d38505f5d");

	//classification
	static final UUID classificationUuid = UUID.fromString("5e3a1b07-2609-4597-bbda-7b02dfe8c2b3");

	private static final String SOURCE_REFERENCE_TITLE = "Flora of the Guianas";
	static final String classificationTitle = "Flora of the Guianas";

	private static final String FEATURE_TREE_TITLE = "Flora of the Guianas Feature Tree";

	//check - import
	private boolean h2ForCheck = true;
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;

	//taxa
	static final boolean doTaxa = true;

	static final boolean reuseState = true;

	private boolean replaceStandardKeyTitles = true;

	private boolean useFotGCollectionTypeOnly = true;

// ****************** NO CHANGE *******************************************/

	private void doImport(ICdmDataSource cdmDestination){
		super.doImport(fotg22, cdmDestination,check, h2ForCheck);


		//make config
		config.setClassificationUuid(classificationUuid);
		config.setDoTaxa(doTaxa);
		config.setDoPrintKeys(doPrintKeys);
		config.setDbSchemaValidation(hbm2dll);
		config.setReplaceStandardKeyTitles(replaceStandardKeyTitles);
		config.setSourceReference(getSourceReference(SOURCE_REFERENCE_TITLE));
		config.setClassificationName(classificationTitle);
		config.setReuseExistingState(reuseState);
		config.setUseFotGSpecimenTypeCollectionAndTypeOnly(useFotGCollectionTypeOnly);

//		URI uri = config.getSource();
//		try {
////			InputStream is = uri.toURL().openStream();
//			File file = new File(uri);
//			System.out.println(file.exists());
//			InputStream is = new FileInputStream(file);
//			System.out.println(is);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		//Vol1-79
		executeVolume( fgu1, includeFotg1 ^ inverseInclude);
		//Vol03
		executeVolume(fotg03, includeFotg03 ^ inverseInclude);
		//Vol11
		executeVolume(fotg11, includeFotg11 ^ inverseInclude);
		//Vol14
		executeVolume(fotg14, includeFotg14 ^ inverseInclude);
		//Vol15
		executeVolume(fotg15, includeFotg15 ^ inverseInclude);
		//Vol16
		executeVolume(fotg16, includeFotg16 ^ inverseInclude);
		//Vol20
		executeVolume(fotg20, includeFotg20 ^ inverseInclude);
		//Vol22
		executeVolume(fotg22, includeFotg22 ^ inverseInclude);
		//Vol23
		executeVolume(fotg23, includeFotg23 ^ inverseInclude);
		//Vol24
		executeVolume(fotg24, includeFotg24 ^ inverseInclude);
		//Vol24+
		executeVolume(fotg24_plus, includeFotg24_plus ^ inverseInclude);
		//Vol25
		executeVolume(fotg25, includeFotg25 ^ inverseInclude);
		//Vol26
		executeVolume(fotg25_plus, includeFotg25_plus ^ inverseInclude);
		//Vol26
		executeVolume(fotg26, includeFotg26 ^ inverseInclude);
		//Vol27
		executeVolume(fotg27, includeFotg27 ^ inverseInclude);

		//Vol29
		executeVolume(fotg29, includeFotg29 ^ inverseInclude);
		//Vol30
		executeVolume(fotg30, includeFotg30 ^ inverseInclude);

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

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private TermTree<Feature> makeFeatureNode(ITermService service){
		MarkupTransformer transformer = new MarkupTransformer();

		TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
		result.setTitleCache(FEATURE_TREE_TITLE, true);
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

		newNode = root.addChild(Feature.COMMON_NAME());

		newNode = root.addChild(Feature.PHENOLOGY());

		newNode = root.addChild(Feature.ECOLOGY());
		addFeatureNodesByStringList(habitatEcologyList, root, transformer, service);

		newNode = root.addChild(Feature.USES());

		addFeatureNodesByStringList(chomosomesList, root, transformer, service);

		newNode = root.addChild(Feature.CITATION());

		String sql = "\nSELECT feature.titleCache " +
				" FROM DescriptionElementBase deb INNER JOIN DefinedTermBase feature ON deb.feature_id = feature.id " +
				" GROUP BY feature.id " +
				" HAVING feature.id NOT IN (SELECT DISTINCT fn.feature_id " +
				" FROM FeatureNode fn " +
				" WHERE fn.feature_id IS NOT NULL) ";
		logger.warn("Check for missing features in feature tree: " + sql);

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
		"Thyrsus",
		"Thyrses",
		"Inflorescences",
		"Inflorescence",
		"Young inflorescences",
		"Male inflorescences",
		"Female inflorescences",
		"rachises",
		"Pedicels",
		"Bracts",
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

	public TermNode<Feature> addFeatureNodesByStringList(String[] featureStringList, TermNode<Feature> root, IInputTransformer transformer, ITermService termService){
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
		FloraGuianasActivator me = new FloraGuianasActivator();
		me.doImport(cdmDestination);
	}
}