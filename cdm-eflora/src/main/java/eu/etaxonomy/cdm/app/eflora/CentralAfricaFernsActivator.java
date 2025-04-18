/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.eflora;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.CentralAfricaFernsImportConfigurator;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 * @since 20.06.2008
 */
public class CentralAfricaFernsActivator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source mySource = CdmImportSources.AFRICA_FERNS_ACCESS();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_andreasM2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_central_africa_preview();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_central_africa_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_postgres_CdmTest();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("62d930cb-aabb-461c-ad16-0fdbd2bae592");

	public static final String sourceReference = "Flora of Central Africa";

	public static final String classificationName = "Flora of Central Africa - Ferns";

	//classification
	static final UUID classificationUuid = UUID.fromString("a90fa160-8f33-4a19-9c5a-ab05a1553017");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

//	static boolean doPrintKeys = false;

	//taxa
	static final boolean doTaxa = true;

//	private boolean includeSapindaceae1 = true;



	private void doImport(ICdmDataSource cdmDestination){


		//make Source
		Source source = mySource;

//		mySource.getResultSet("SELECT * FROM tmp");
		CentralAfricaFernsImportConfigurator config= CentralAfricaFernsImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setClassificationName(classificationName);
		config.setDoTaxa(doTaxa);
		config.setCheck(check);
//		configsetDoPrintKeys(doPrintKeys);
		config.setDbSchemaValidation(hbm2dll);

		CdmDefaultImport<CentralAfricaFernsImportConfigurator> myImport = new CdmDefaultImport<CentralAfricaFernsImportConfigurator>();


//		if (includeSapindaceae1){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(sourceReference));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
//		}

		TermTree<Feature> tree = makeFeatureNode(myImport.getCdmAppController().getTermService());
		myImport.getCdmAppController().getTermTreeService().saveOrUpdate(tree);

		//check keys
//		if (doPrintKeys){
//			TransactionStatus tx = myImport.getCdmAppController().startTransaction();
//			List<FeatureTree> keys = myImport.getCdmAppController().getTermTreeService().list(PolytomousKey.class, null, null, null, null);
//			for(FeatureTree key : keys){
//				((PolytomousKey)key).print(System.out);
//				System.out.println();
//			}
//			myImport.getCdmAppController().commitTransaction(tx);
//		}

	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private TermTree<Feature> makeFeatureNode(ITermService service){

		TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
		result.setTitleCache("Flora Malesiana Presentation Feature Tree", true);
		TermNode<Feature> root = result.getRoot();
		TermNode<Feature> newNode = root.addChild(Feature.CITATION());

		root.addChild(Feature.DESCRIPTION());

		return result;
	}

	public static void main(String[] args) {
		CentralAfricaFernsActivator me = new CentralAfricaFernsActivator();
		me.doImport(cdmDestination);
	}

}
