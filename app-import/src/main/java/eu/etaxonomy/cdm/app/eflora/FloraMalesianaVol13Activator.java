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
import eu.etaxonomy.cdm.app.common.CdmDestinations;
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
public class FloraMalesianaVol13Activator {
	private static final Logger logger = Logger.getLogger(FloraMalesianaVol13Activator.class);
	
	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI fmSource13_small = EfloraSources.fm_13_small_families();
	static final URI fmSource13_large = EfloraSources.fm_13_large_families();
	
	
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
	static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	
	static boolean doPrintKeys = false;
	
	//taxa
	static final boolean doTaxa = true;

	private boolean includeVol13_small = true;
	private boolean includeVol13_large = false;

	private IIoObserver observer = new LoggingIoObserver();
	private Set<IIoObserver> observerList = new HashSet<IIoObserver>();
	
	
	private void doImport(ICdmDataSource cdmDestination){
		observerList.add(observer);
		if (cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
			check = CHECK.CHECK_ONLY;
		}
		
		//make Source
		URI source = fmSource13_small;
		MarkupImportConfigurator markupConfig= MarkupImportConfigurator.NewInstance(source, cdmDestination);
		markupConfig.setClassificationUuid(classificationUuid);
		markupConfig.setDoTaxa(doTaxa);
		markupConfig.setCheck(check);
		markupConfig.setDoPrintKeys(doPrintKeys);
		markupConfig.setDbSchemaValidation(hbm2dll);
		markupConfig.setObservers(observerList);
		
		CdmDefaultImport<MarkupImportConfigurator> myImport = new CdmDefaultImport<MarkupImportConfigurator>();


		markupConfig.setSourceReference(getSourceReference("Flora Malesiana - Vol. 13"));
		//Vol13_1
		if (includeVol13_small){
			System.out.println("\nStart import from ("+ fmSource13_small.toString() + ") ...");
			source = fmSource13_small;
			markupConfig.setSource(source);
			myImport.invoke(markupConfig);
			System.out.println("End import from ("+ fmSource13_small.toString() + ")...");
		}

		//Vol13_2
		if (includeVol13_large){
			System.out.println("\nStart import from ("+ fmSource13_large.toString() + ") ...");
			source = fmSource13_large;
			markupConfig.setSource(source);
			myImport.invoke(markupConfig);
			System.out.println("End import from ("+ fmSource13_large.toString() + ")...");
		}
		
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
		result.setTitleCache("Flora Malesiana Presentation Feature Tree");
		FeatureNode root = result.getRoot();
		FeatureNode newNode;
		
		newNode = FeatureNode.NewInstance(Feature.DESCRIPTION());
		root.addChild(newNode);
		
		addFeataureNodesByStringList(descriptionFeatureList, newNode, transformer, service);

		addFeataureNodesByStringList(generellDescriptionsList, root, transformer, service);

		
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
	
	
	private static String [] generellDescriptionsList = new String[]{
		"Fossils",
		"Morphology and anatomy",
		"Morphology", 
		"Vegetative morphology and anatomy",
		"Flower morphology",
		"Palynology",  
		"Pollination",  
		"Pollen morphology",
		"embryology",
		"cytology",
		"Life cycle",
		"Fruits and embryology",
		"Dispersal",
		"Wood anatomy",  
		"Leaf anatomy",  
		"Chromosome numbers", 
		"Phytochemistry and Chemotaxonomy",
		"Phytochemistry",
		"Taxonomy",
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
		"male inflorescences", 
		"female inflorescences", 
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
	
	public void addFeataureNodesByStringList(String[] featureStringList, FeatureNode root, IInputTransformer transformer, ITermService termService){
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
	}
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FloraMalesianaVol13Activator me = new FloraMalesianaVol13Activator();
		me.doImport(cdmDestination);
	}
	
}
