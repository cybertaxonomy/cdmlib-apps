/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.pesi;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.PesiSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.indexFungorum.IndexFungorumImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 */
public class IndexFungorumImportActivator {

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(IndexFungorumImportActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

	static final Source indexFungorumSource = PesiSources.PESI3_IF();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_indexFungorum();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_indexFungorum2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2019_final();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2019_final_test();

	static final UUID classificationUuid = UUID.fromString("4bea48c3-eb10-41d1-b708-b5ee625ed243");

	static final boolean doPesiExport = false;

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static final int partitionSize = 2000;
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;


	public void doImport(Source source, ICdmDataSource destination){
		System.out.println("Start import from ("+ indexFungorumSource.getDatabase() + ") ...");

		IndexFungorumImportConfigurator ifImportConfigurator = IndexFungorumImportConfigurator.NewInstance(source,  destination);

		ifImportConfigurator.setClassificationUuid(classificationUuid);
		ifImportConfigurator.setNomenclaturalCode(nomenclaturalCode);

		ifImportConfigurator.setDoTaxa(true);
		ifImportConfigurator.setDbSchemaValidation(hbm2dll);

		ifImportConfigurator.setCheck(check);
		ifImportConfigurator.setRecordsPerTransaction(partitionSize);
		ifImportConfigurator.setSourceRefUuid(PesiTransformer.uuidSourceRefIndexFungorum);

		// invoke import
		CdmDefaultImport<IndexFungorumImportConfigurator> ifImport = new CdmDefaultImport<IndexFungorumImportConfigurator>();
		ifImport.invoke(ifImportConfigurator);

		if (ifImportConfigurator.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || ifImportConfigurator.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK)    ){
//			ICdmApplicationConfiguration app = ifImport.getCdmAppController();
//			ISourceable obj = app.getCommonService().getSourcedObjectByIdInSource(ZoologicalName.class, "1000027", null);
//			logger.info(obj);

//			//make feature tree
//			FeatureTree tree = TreeCreator.flatTree(featureTreeUuid, ermsImportConfigurator.getFeatureMap(), featureKeyList);
//			app = ermsImport.getCdmAppController();
//			app.getFeatureTreeService().saveOrUpdate(tree);
		}
		System.out.println("End import from ("+ source.getDatabase() + ")...");
	}

	public static void main(String[] args) {

		//make IF Source
		Source source = indexFungorumSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		IndexFungorumImportActivator ifActivator = new IndexFungorumImportActivator();
		ifActivator.doImport(source, destination);

		if (doPesiExport){
			PesiExportActivatorIF ifExportActivator = new PesiExportActivatorIF();
			ifExportActivator.doExport(destination);
		}
		System.exit(0);
	}
}
