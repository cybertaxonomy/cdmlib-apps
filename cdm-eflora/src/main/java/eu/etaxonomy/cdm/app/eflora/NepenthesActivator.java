/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.eflora;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.api.service.ITermService;
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
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.PolytomousKey;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 20.06.2008
 */
public class NepenthesActivator extends EfloraActivatorBase {
	private static final Logger logger = Logger.getLogger(NepenthesActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_nepenthes_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();

	//1775   /   16415
	//no syso, 5 uncovered, 2 of them epiphytic
//	private final boolean includeVol15 = includeBase;

    static final URI fmSource15 = EfloraSources.fm_15();

	private final boolean h2ForCheck = true;

	static final boolean reuseState = true;  //when running multiple imports


	//check - import
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;
	static boolean doDeduplicate = false;

	//taxa
	static final boolean doTaxa = true;


    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("ef2e2978-1ea4-44d2-a819-4e79b372b9b7");
    private static final String featureTreeTitle = "Nepenthaceae Feature Tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("a245793a-a70f-4fcf-a626-dd4aa6d2aa1c");
    static final String classificationTitle = "Nepenthaceae";

    static final UUID specimenNotSeenMarkerTypeUuid = UUID.fromString("fb8ad1dd-ac70-4453-bc33-a9eeafd5e439");
    static final String specimenNotSeenMarkerTypeLabel = "Not seen for Flora Malesiana";

	private final boolean replaceStandardKeyTitles = false;
	private boolean ignoreLocalityClass = true;
	private boolean handleWriterManually = true;
	private boolean doExtensionForTaxonTitle = false;


	private final IIoObserver observer = new LoggingIoObserver();
	private final Set<IIoObserver> observerList = new HashSet<>();


	private void doImport(ICdmDataSource cdmDestination){
		observerList.add(observer);
		if (h2ForCheck && cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
			check = CHECK.CHECK_ONLY;
		}

		//make Source
        URI source = null;

		MarkupImportConfigurator config= MarkupImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setClassificationName(classificationTitle);
		config.setDoTaxa(doTaxa);
		config.setCheck(check);
		config.setDoPrintKeys(doPrintKeys);
		config.setDbSchemaValidation(hbm2dll);
		config.setObservers(observerList);
		config.setReplaceStandardKeyTitles(replaceStandardKeyTitles);
		config.setReuseExistingState(reuseState);
		config.setIgnoreLocalityClass(ignoreLocalityClass);
		config.setHandleWriterManually(handleWriterManually);
		config.setKnownCollections(getKnownCollections());
		config.setSpecimenNotSeenMarkerTypeUuid(specimenNotSeenMarkerTypeUuid);
		config.setSpecimenNotSeenMarkerTypeLabel(specimenNotSeenMarkerTypeLabel);
		config.setDoExtensionForTaxonTitle(doExtensionForTaxonTitle);

		config.setSourceReference(getSourceReference("Flora Malesiana - Vol. 15"));

		CdmDefaultImport<MarkupImportConfigurator> myImport = new CdmDefaultImport<>();


		//Vol15
		doSource(true, fmSource15, "Flora Malesiana - vol. 15", config, myImport);


		makeAutomatedFeatureTree(myImport.getCdmAppController(), config.getState(),
				featureTreeUuid, featureTreeTitle);

//		makeGeoService();

		FeatureTree tree = makeFeatureNode(myImport.getCdmAppController().getTermService());
		myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);

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

	      //deduplicate
        if (doDeduplicate){
            ICdmRepository app = myImport.getCdmAppController();
            if (app == null){
                app = CdmApplicationController.NewInstance(cdmDestination, hbm2dll, false);
            }
//            app.getAgentService().updateTitleCache(Team.class, null, null, null);
//            return;
          int count = app.getAgentService().deduplicate(Person.class, null, null);
          logger.warn("Deduplicated " + count + " persons.");
          count = app.getAgentService().deduplicate(Team.class, null, null);
//            logger.warn("Deduplicated " + count + " teams.");
          count = app.getReferenceService().deduplicate(Reference.class, null, null);
          logger.warn("Deduplicated " + count + " references.");
        }

	}

	/**
     * @return
     */
    private List<String> getKnownCollections() {
        List<String> result = Arrays.asList(new String[]
                {"Nippon Dental College","Nagoya","Sabah National Parks Herbarium","K-Wall"}) ;
        return result;
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

	private FeatureTree makeFeatureNode(ITermService service){
		FloraMalesianaTransformer transformer = new FloraMalesianaTransformer();

		FeatureTree result = FeatureTree.NewInstance(UUID.randomUUID());
		result.setTitleCache("Flora Malesiana Nepenthaceae Simple Feature Tree", true);
		FeatureNode root = result.getRoot();
		FeatureNode newNode;

		newNode = FeatureNode.NewInstance(Feature.CITATION());
        root.addChild(newNode);


		newNode = FeatureNode.NewInstance(Feature.DESCRIPTION());
		root.addChild(newNode);

	    newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
	    root.addChild(newNode);

        newNode = FeatureNode.NewInstance(Feature.ECOLOGY());
        root.addChild(newNode);

        newNode = FeatureNode.NewInstance(Feature.SPECIMEN());
        root.addChild(newNode);

        newNode = FeatureNode.NewInstance(Feature.NOTES());
        root.addChild(newNode);

//		addFeataureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

//		addFeataureNodesByStringList(generellDescriptionsUpToAnatomyList, root, transformer, service);
//		newNode = FeatureNode.NewInstance(Feature.ANATOMY());
//		addFeataureNodesByStringList(anatomySubfeatureList, newNode, transformer, service);
//
//		newNode = addFeataureNodesByStringList(generellDescriptionsFromAnatomyToPhytoChemoList, root, transformer, service);
//		addFeataureNodesByStringList(phytoChemoSubFeaturesList, newNode, transformer, service);
//
//		newNode = addFeataureNodesByStringList(generellDescriptionsFromPhytoChemoList, root, transformer, service);
//
//

//
//		newNode = FeatureNode.NewInstance(Feature.ECOLOGY());
//		root.addChild(newNode);
//		addFeataureNodesByStringList(habitatEcologyList, root, transformer, service);
//
//		newNode = FeatureNode.NewInstance(Feature.USES());
//		root.addChild(newNode);
//
//		addFeataureNodesByStringList(chomosomesList, root, transformer, service);


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

	public FeatureNode addFeataureNodesByStringList(String[] featureStringList, FeatureNode root, IInputTransformer transformer, ITermService termService){
		FeatureNode lastChild = null;
		try {
			for (String featureString : featureStringList){
				UUID featureUuid;
				featureUuid = transformer.getFeatureUuid(featureString);
				Feature feature = (Feature)termService.find(featureUuid);
				if (feature != null){
					FeatureNode child = FeatureNode.NewInstance(feature);
					root.addChild(child);
				}
			}

		} catch (UndefinedTransformerMethodException e) {
			logger.error("getFeatureUuid is not implemented in transformer. Features could not be added");
		}
		return lastChild;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NepenthesActivator me = new NepenthesActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}

}
