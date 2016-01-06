/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.cuba;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.cuba.CubaImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 04.01.2016
 */
public class CubaActivator {
	private static final Logger logger = Logger.getLogger(CubaActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

	private static final URI source = monocots();
//    private static final URI source = cyperaceae();


//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_cyprus_dev();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_cyprus_production();


	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("dad6b9b5-693f-4367-a7aa-076cc9c99476");

	//classification
	static final UUID classificationUuid = UUID.fromString("5de394de-9c76-4b97-b04d-71be31c7f44b");

	static final String sourceReferenceTitle = "Cuba import";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//taxa
	static final boolean doTaxa = true;
	static final boolean doDeduplicate = false;
	static final boolean doDistribution = false;


	private void doImport(ICdmDataSource cdmDestination){

		//make Source
		CubaImportConfigurator config= CubaImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setCheck(check);
//		config.setDoDistribution(doDistribution);
		config.setDoTaxa(doTaxa);
		config.setDbSchemaValidation(hbm2dll);
		config.setSourceReferenceTitle(sourceReferenceTitle);

		CdmDefaultImport<CubaImportConfigurator> myImport = new CdmDefaultImport();


		//...
		if (true){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(sourceReferenceTitle));
			myImport.invoke(config);
			if (doTaxa){
				FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
				myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
			}

			System.out.println("End import from ("+ source.toString() + ")...");
		}



		//deduplicate
		if (doDeduplicate){
			ICdmApplicationConfiguration app = myImport.getCdmAppController();
			int count = app.getAgentService().deduplicate(Person.class, null, null);
			logger.warn("Deduplicated " + count + " persons.");
//			count = app.getAgentService().deduplicate(Team.class, null, null);
//			logger.warn("Deduplicated " + count + " teams.");
			count = app.getReferenceService().deduplicate(Reference.class, null, null);
			logger.warn("Deduplicated " + count + " references.");
		}

	}

	private Reference<?> getSourceReference(String string) {
		Reference<?> result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private FeatureTree makeFeatureNodes(ITermService service){
//		CyprusTransformer transformer = new CyprusTransformer();

		FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
		result.setTitleCache("Cuba Feature Tree", true);
		FeatureNode root = result.getRoot();
		FeatureNode newNode;

//		newNode = FeatureNode.NewInstance(Feature.STATUS());
//		root.addChild(newNode);

		newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
		root.addChild(newNode);

//		newNode = FeatureNode.NewInstance(Feature.SYSTEMATICS());
//		root.addChild(newNode);

		//user defined features
//		String [] featureList = new String[]{"Red Book", "Endemism"};
//		addFeataureNodesByStringList(featureList, root, transformer, service);

		return result;
	}


	//Monocots
	public static URI monocots() {
	    return URI.create("file:////BGBM-PESIHPC/Cuba/Monocot.xlsx");
	}

	//Cyperaceae
	public static URI cyperaceae() {
	    return URI.create("file:////BGBM-PESIHPC/Cuba/Cyper_Poaceae.xlsx");
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    CubaActivator me = new CubaActivator();
		me.doImport(cdmDestination);
	}

}
