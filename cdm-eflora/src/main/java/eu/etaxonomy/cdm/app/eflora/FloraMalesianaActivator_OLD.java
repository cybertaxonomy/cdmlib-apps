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
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.eflora.EfloraImportConfigurator;
import eu.etaxonomy.cdm.io.eflora.floraMalesiana.FloraMalesianaTransformer;
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
public class FloraMalesianaActivator_OLD {

    private static Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI fmSource1 = EfloraSources.fm_sapindaceae_local();
	static final URI fmSource2 = EfloraSources.fm_sapindaceae2_local();
	static final URI fmSource13_1 = EfloraSources.fm_13_1_local();
	static final URI fmSource13_2 = EfloraSources.fm_13_2_local();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_andreasM3();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_malesiana_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_malesiana_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("168df0c6-6429-484c-b26f-ded1f7e44bd9");

	//classification
	static final UUID classificationUuid = UUID.fromString("ca4e4bcb-a1d1-4124-a358-a3d3c41dd450");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;

	//taxa
	static final boolean doTaxa = true;

	private boolean includeSapindaceae1 = true;
	private boolean includeSapindaceae2 = true;
	private boolean includeVol13_1 = false;
	private boolean includeVol13_2 = false;


	private void doImport(ICdmDataSource cdmDestination){

		//make Source
		URI source = fmSource1;
		EfloraImportConfigurator floraMalesianaConfig= EfloraImportConfigurator.NewInstance(source, cdmDestination);
		floraMalesianaConfig.setClassificationUuid(classificationUuid);
		floraMalesianaConfig.setDoTaxa(doTaxa);
		floraMalesianaConfig.setCheck(check);
		floraMalesianaConfig.setDoPrintKeys(doPrintKeys);
		floraMalesianaConfig.setDbSchemaValidation(hbm2dll);

		CdmDefaultImport<EfloraImportConfigurator> myImport = new CdmDefaultImport<EfloraImportConfigurator>();


		//Sapindaceae1
		if (includeSapindaceae1){
			System.out.println("Start import from ("+ fmSource1.toString() + ") ...");
			floraMalesianaConfig.setSourceReference(getSourceReference("Flora Malesiana - Sapindaceae I"));
			myImport.invoke(floraMalesianaConfig);
			System.out.println("End import from ("+ fmSource1.toString() + ")...");
		}

		//Sapindaceae2
		if (includeSapindaceae2){
			System.out.println("\nStart import from ("+ fmSource2.toString() + ") ...");
			source = fmSource2;
			floraMalesianaConfig.setSource(source);
			floraMalesianaConfig.setSourceReference(getSourceReference("Flora Malesiana - Sapindaceae II"));
			myImport.invoke(floraMalesianaConfig);
			System.out.println("End import from ("+ fmSource2.toString() + ")...");
		}

		floraMalesianaConfig.setSourceReference(getSourceReference("Flora Malesiana - Vol. 13"));
		//Vol13_1
		if (includeVol13_1){
			System.out.println("\nStart import from ("+ fmSource13_1.toString() + ") ...");
			source = fmSource13_1;
			floraMalesianaConfig.setSource(source);
			myImport.invoke(floraMalesianaConfig);
			System.out.println("End import from ("+ fmSource13_1.toString() + ")...");
		}

		//Vol13_2
		if (includeVol13_2){
			System.out.println("\nStart import from ("+ fmSource13_2.toString() + ") ...");
			source = fmSource13_2;
			floraMalesianaConfig.setSource(source);
			myImport.invoke(floraMalesianaConfig);
			System.out.println("End import from ("+ fmSource13_2.toString() + ")...");
		}

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
		FloraMalesianaTransformer transformer = new FloraMalesianaTransformer();

		TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
		result.setTitleCache("Flora Malesiana Presentation Feature Tree", true);
		TermNode<Feature> root = result.getRoot();

		TermNode<Feature> newNode = root.addChild(Feature.DESCRIPTION());

		addFeatureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

		addFeatureNodesByStringList(generellDescriptionsList, root, transformer, service);


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


	private static String [] generellDescriptionsList = new String[]{
		"Fossils",
		"Morphology and anatomy",
		"Morphology",
		"Vegetative morphology and anatomy",
		"Flower morphology",
		"Palynology",
		"Pollination",
		"Pollen morphology",
		"Life cycle",
		"Fruits and embryology",
		"Dispersal",
		"Wood anatomy",
		"Leaf anatomy",
		"Chromosome numbers",
		"Phytochemistry and Chemotaxonomy",
		"Phytochemistry",
		"Taxonomy",
	};

	private static String [] descriptionFeatureList = new String[]{
		"lifeform",
		"Bark",
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

	public void addFeatureNodesByStringList(String[] featureStringList, TermNode<Feature> root, IInputTransformer transformer, ITermService termService){
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
	}

	public static void main(String[] args) {
		FloraMalesianaActivator_OLD me = new FloraMalesianaActivator_OLD();
		me.doImport(cdmDestination);
	}
}