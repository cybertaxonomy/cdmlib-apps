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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

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
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.PolytomousKey;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 20.06.2008
 * @version 1.0
 */
public class FloreGabonActivator {
	private static final Logger logger = Logger.getLogger(FloreGabonActivator.class);
	
	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI fg_sample = EfloraSources.fg_sample();
	
	
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flore_gabon_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flore_gabon_production();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
	

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("ee688973-2595-4d4d-b11e-6df71e96a5c2");
	
	//classification
	static final UUID classificationUuid = UUID.fromString("2f892452-ff49-48cf-834f-52ca29600719");
	
	//check - import
	private boolean h2ForCheck = true;
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	
	static boolean doPrintKeys = false;
	
	//taxa
	static final boolean doTaxa = true;
	
	private boolean includeFgSample = true;
	

		
	private boolean replaceStandardKeyTitles = false;

	private IIoObserver observer = new LoggingIoObserver();
	private Set<IIoObserver> observerList = new HashSet<IIoObserver>();
	
	
	private void doImport(ICdmDataSource cdmDestination){
		observerList.add(observer);
		if (h2ForCheck && cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
			check = CHECK.CHECK_ONLY;
		}
		
		//make Source
		URI source = fg_sample;
		MarkupImportConfigurator markupConfig= MarkupImportConfigurator.NewInstance(source, cdmDestination);
		markupConfig.setClassificationUuid(classificationUuid);
		markupConfig.setDoTaxa(doTaxa);
		markupConfig.setCheck(check);
		markupConfig.setDoPrintKeys(doPrintKeys);
		markupConfig.setDbSchemaValidation(hbm2dll);
		markupConfig.setObservers(observerList);
		markupConfig.setReplaceStandardKeyTitles(replaceStandardKeyTitles);
		
		
		markupConfig.setSourceReference(getSourceReference("Flore Gabon -  Sample"));
		
		CdmDefaultImport<MarkupImportConfigurator> myImport = new CdmDefaultImport<MarkupImportConfigurator>(); 
		
		//Vol13_2
		if (includeFgSample){
			System.out.println("\nStart import from ("+ fg_sample.toString() + ") ...");
			source = fg_sample;
			markupConfig.setSource(source);
			myImport.invoke(markupConfig);
			System.out.println("End import from ("+ fg_sample.toString() + ")...");
		}
		

		
		
//			makeGeoService();
		
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
		
	}
	
	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string);
		return result;
	}

	private FeatureTree makeFeatureNode(ITermService service){
		FloraMalesianaTransformer transformer = new FloraMalesianaTransformer();
		
		FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
		result.setTitleCache("Flore Gabon Presentation Feature Tree");
		FeatureNode root = result.getRoot();
		FeatureNode newNode;
		
		newNode = FeatureNode.NewInstance(Feature.DESCRIPTION());
		root.addChild(newNode);
		
		addFeataureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

		addFeataureNodesByStringList(generellDescriptionsUpToAnatomyList, root, transformer, service);
		newNode = FeatureNode.NewInstance(Feature.ANATOMY());
		addFeataureNodesByStringList(anatomySubfeatureList, newNode, transformer, service);
		
		newNode = addFeataureNodesByStringList(generellDescriptionsFromAnatomyToPhytoChemoList, root, transformer, service);
		addFeataureNodesByStringList(phytoChemoSubFeaturesList, newNode, transformer, service);

		newNode = addFeataureNodesByStringList(generellDescriptionsFromPhytoChemoList, root, transformer, service);
		
		
		newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
		root.addChild(newNode);

		newNode = FeatureNode.NewInstance(Feature.ECOLOGY());
		root.addChild(newNode);
		addFeataureNodesByStringList(habitatEcologyList, root, transformer, service);
		
		newNode = FeatureNode.NewInstance(Feature.USES());
		root.addChild(newNode);
		
		addFeataureNodesByStringList(chomosomesList, root, transformer, service);

		newNode = FeatureNode.NewInstance(Feature.CITATION());
		root.addChild(newNode);
		
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
		FloreGabonActivator me = new FloreGabonActivator();
		me.doImport(cdmDestination);
	}
}