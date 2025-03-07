/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.berlinModelImport;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.algaterra.AlgaTerraImportConfigurator;
import eu.etaxonomy.cdm.io.algaterra.AlgaTerraImportTransformer;
import eu.etaxonomy.cdm.io.algaterra.AlgaTerraSpecimenImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 *
 * @author a.mueller
 *
 */
public class AlgaTerraActivator {

    private static Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.AlgaTerra();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_algaterra_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_algaterra_production();


	static final UUID treeUuid = UUID.fromString("1f617402-78dc-4bf1-ac77-d260600a8879");
	static final int sourceSecId = 7331;
	static final UUID sourceRefUuid = UUID.fromString("7e1a2500-93a5-40c2-ba34-0213d7822379");

	static final UUID featureTreeUuid = UUID.fromString("a970168a-36fd-4c7c-931e-87214a965c14");
	static final Object[] featureKeyList = new Integer[]{7,201,203,204,206,207};
	static final UUID specimenFeatureTreeUuid = UUID.fromString("ba86246e-d4d0-419f-832e-86d70b1e4bd7");

	static final boolean loginAsDefaultAdmin = true;
	//TODO set to false for final import
	static final boolean removeRestricted = true;

	static final boolean importOriginalSizeMedia = false;

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	private boolean ignoreNull = true;

	private boolean includeFlatClassifications = true;
	private boolean includeAllNonMisappliedRelatedClassifications = true;

	private EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;

	static String factFilter = " factCategoryFk NOT IN (7, 201, 202, 203, 204, 205, 206, 207, 208, 1000 ) ";


// ****************** ALL *****************************************

	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doNameStatus = true;
	static final boolean doTypes = true;

	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = true;

	//alga terra specific
	static final boolean ecoFacts = true;
	static final boolean doFactEcology = true;
	static final boolean doImages = false;
	static final boolean doDna = true;
	static final boolean doMorphology = true;

// ************************ NONE **************************************** //

//	//authors
//	static final boolean doAuthors = false;
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//	//names
//	static final boolean doTaxonNames = false;
//	static final boolean doRelNames = false;
//	static final boolean doNameStatus = false;
//	static final boolean doTypes = true;
//	static final boolean doNameFacts = false;
//
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = false;
//	static final boolean doFacts = false;
//
//  //alga terra specific
//	static final boolean ecoFacts = false;
//	static final boolean doFactEcology = false;
//	static final boolean doImages = false;
//	static final boolean doDna = false;
//	static final boolean doMorphology = false;


	public void invoke(String[] args){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") ...");
		logger.debug("Start");
		//make BerlinModel Source
		Source source = berlinModelSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		AlgaTerraImportConfigurator config = AlgaTerraImportConfigurator.NewInstance(source,  destination);

		config.setClassificationUuid(treeUuid);
		config.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);

		config.setDoAuthors(doAuthors);
		config.setDoReferences(doReferences);
		config.setDoTaxonNames(doTaxonNames);
		config.setDoRelNames(doRelNames);
		config.setDoNameStatus(doNameStatus);
		config.setDoTypes(doTypes);

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoFacts(doFacts);
		config.setDoEcoFacts(ecoFacts);
		config.setDoImages(doImages);
		config.setDoFactEcology(doFactEcology);
		config.setDoDna(doDna);
		config.setDoMorphology(doMorphology);

		config.setSourceRefUuid(sourceRefUuid);
		config.setIgnoreNull(ignoreNull);
		config.setRemoveRestricted(removeRestricted);
		config.setImportOriginalSizeMedia(importOriginalSizeMedia);

		config.setIncludeFlatClassifications(includeFlatClassifications);
		config.setIncludeAllNonMisappliedRelatedClassifications(includeAllNonMisappliedRelatedClassifications);
		config.setFactFilter(factFilter);

		config.setDbSchemaValidation(hbm2dll);

		config.setCheck(check);
		config.setEditor(editor);

		if (loginAsDefaultAdmin){
			config.authenticateAsDefaultAdmin();
		}

		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<BerlinModelImportConfigurator>();
		bmImport.invoke(config);

		if (doFacts && (config.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || config.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK) )   ){
		    ICdmRepository app = bmImport.getCdmAppController();

			//make feature tree
			makeTaxonFeatureTree(config, app);

			//make specimen feature tree
			//TODO more specimen specific
			makeSpecimenFeatureTree(config, app);

		}


		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
	}


	/**
	 * @param config
	 * @param app
	 */
	private void makeTaxonFeatureTree(AlgaTerraImportConfigurator config, ICdmRepository app) {
	    TermTree<Feature> tree = TreeCreator.flatTree(featureTreeUuid, config.getFeatureMap(), featureKeyList);
		tree.setTitleCache("AlgaTerra Taxon Feature Tree", true);

		tree.getRoot().addChild(Feature.HABITAT());

//		tree.getRoot().addChild(Feature.OBSERVATION());
//
//		tree.getRoot().addChild(Feature.SPECIMEN());
//
//		tree.getRoot().addChild(Feature.INDIVIDUALS_ASSOCIATION());

		//needed ??
		tree.getRoot().addChild(Feature.DISTRIBUTION(), 2);

//		//needed ??
//		tree.getRoot().addChild(Feature.IMAGE());

		app.getTermTreeService().saveOrUpdate(tree);
	}


	/**
	 * @param config
	 * @param app
	 * @param tree
	 */
	private void makeSpecimenFeatureTree(AlgaTerraImportConfigurator config, ICdmRepository app) {
		ITermService termService = app.getTermService();
		TermTree<Feature> specimenTree = TermTree.NewFeatureInstance(specimenFeatureTreeUuid);
//		FeatureTree specimenTree = TreeCreator.flatTree(specimenFeatureTreeUuid, config.getFeatureMap(), featureKeyList);
		specimenTree.setTitleCache("AlgaTerra Specimen Feature Tree", true);
		TermNode<Feature> root = specimenTree.getRoot();


		root.addChild(Feature.IMAGE());

		addFeatureNodeByUuid(root, termService, AlgaTerraSpecimenImportBase.uuidFeatureAlgaTerraClimate);
		root.addChild(Feature.HABITAT());
		addFeatureNodeByUuid(root, termService, AlgaTerraSpecimenImportBase.uuidFeatureHabitatExplanation);
		addFeatureNodeByUuid(root, termService, AlgaTerraSpecimenImportBase.uuidFeatureAlgaTerraLifeForm);

		addFeatureNodeByUuid(root, termService, AlgaTerraSpecimenImportBase.uuidFeatureAdditionalData);
		addFeatureNodeByUuid(root, termService, AlgaTerraSpecimenImportBase.uuidFeatureSpecimenCommunity);

		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeaturePH);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureConductivity);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureWaterTemperature);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureSilica);
		TermNode<Feature> nitrogenNode = makeNitrogenNode(root, termService);
		addFeatureNodeByUuid(nitrogenNode, termService, AlgaTerraImportTransformer.uuidFeatureNitrate);
		addFeatureNodeByUuid(nitrogenNode, termService, AlgaTerraImportTransformer.uuidFeatureNitrite);
		addFeatureNodeByUuid(nitrogenNode, termService, AlgaTerraImportTransformer.uuidFeatureAmmonium);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeaturePhosphate);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureOrthoPhosphate);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureNPRation);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureDIN);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureSRP);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureOxygenSaturation);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureCl);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureSecchiDepth);
		addFeatureNodeByUuid(root, termService, AlgaTerraImportTransformer.uuidFeatureCommunity);
		app.getTermTreeService().saveOrUpdate(specimenTree);
	}

	private TermNode<Feature> makeNitrogenNode(TermNode<Feature> root, ITermService termService) {
		Feature nFeature = Feature.NewInstance("Supra feature for all Nitrogen related subfeatures", "Nitrogen", "N");
		termService.save(nFeature);
		TermNode<Feature> nNode = root.addChild(nFeature);
		return nNode;
	}


//	private FeatureNode addFeataureNodesByUuidList(UUID[] featureUuidList, FeatureNode root, ITermService termService){
//		FeatureNode lastChild = null;
//		for (UUID featureUuid : featureUuidList){
//			addFeatureNodeByUuid(root, termService, featureUuid);
//		}
//
//		return lastChild;
//	}


	/**
	 * @param root
	 * @param termService
	 * @param featureUuid
	 */
	private void addFeatureNodeByUuid(TermNode<Feature> root, ITermService termService, UUID featureUuid) {
		Feature feature = (Feature)termService.find(featureUuid);
		if (feature != null){
			root.addChild(feature);
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AlgaTerraActivator activator = new AlgaTerraActivator();
		activator.invoke(args);
	}

}
