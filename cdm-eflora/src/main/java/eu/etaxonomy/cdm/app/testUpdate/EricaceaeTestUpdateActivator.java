/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.testUpdate;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.ICdmApplication;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.eflora.CdmDestinations;
import eu.etaxonomy.cdm.app.eflora.EfloraSources;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.database.update.CdmUpdater;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.eflora.EfloraImportConfigurator;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ericaceae.CentralAfricaEricaceaeImportConfigurator;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.model.term.TermType;

/**
 * @author a.mueller
 * @since 20.06.2008
 */
public class EricaceaeTestUpdateActivator {

	@SuppressWarnings("unused")
	private static Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	static final URI source = EfloraSources.ericacea_local();


//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_andreasM2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_central_africa_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_central_africa_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_postgres_CdmTest();


	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("051d35ee-22f1-42d8-be07-9e9bfec5bcf7");

	public static UUID defaultLanguageUuid = Language.uuidEnglish;

	//classification
	static final UUID classificationUuid = UUID.fromString("10e5efcc-6e13-4abc-ad42-e0b46e50cbe7");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static boolean doPrintKeys = false;

	//taxa
	static final boolean doTaxa = false;

	private boolean includeEricaceae = true;



	private void doImport(ICdmDataSource cdmDestination){

		CdmUpdater updater = new CdmUpdater();
		updater.updateToCurrentVersion(cdmDestination, DefaultProgressMonitor.NewInstance());

		//make Source
		CentralAfricaEricaceaeImportConfigurator config= CentralAfricaEricaceaeImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setDoTaxa(doTaxa);
		config.setCheck(check);
		config.setDefaultLanguageUuid(defaultLanguageUuid);
		config.setDoPrintKeys(doPrintKeys);
		config.setDbSchemaValidation(hbm2dll);

		CdmDefaultImport<EfloraImportConfigurator> myImport = new CdmDefaultImport<EfloraImportConfigurator>();

		ICdmApplication app = myImport.getCdmAppController();

		//
		if (includeEricaceae){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}

		app = myImport.getCdmAppController();

		TransactionStatus tx = app.startTransaction();
		List<TermTree> featureTrees = app.getTermTreeService().list(TermType.Feature, null, null, null, null);
		for (TermTree<Feature> tree :featureTrees){
			if (tree.getClass().getSimpleName().equalsIgnoreCase("FeatureTree")){
				moveChild(app, tree);
			}
		}
		app.commitTransaction(tx);
	}

	private void moveChild(ICdmRepository app, TermTree<Feature> tree) {
		TermNode<Feature> root = tree.getRoot();
		int count = root.getChildCount();
		TermNode<Feature> lastChild = root.getChildAt(count - 1);
		root.removeChild(lastChild);
		root.addChild(lastChild, 1);
		app.getTermTreeService().saveOrUpdate(tree);
	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	public static void main(String[] args) {
		EricaceaeTestUpdateActivator me = new EricaceaeTestUpdateActivator();
		me.doImport(cdmDestination);
	}
}