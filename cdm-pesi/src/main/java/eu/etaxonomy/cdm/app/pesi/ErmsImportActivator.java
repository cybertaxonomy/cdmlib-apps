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

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.ISourceable;

/**
 * @author a.mueller
 */
public class ErmsImportActivator {

    private static final Logger logger = Logger.getLogger(ErmsImportActivator.class);

//	static final Source ermsSource = PesiSources.PESI3_ERMS();
	static final Source ermsSource = PesiSources.PESI2019_ERMS();

//	static final ICdmDataSource cdmDestination = CdmDestinations.test_cdm_pesi_erms();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_erms();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_erms2();

	static final Source pesiDestination =
	        cdmDestination == CdmDestinations.cdm_test_local_mysql_erms()?
	                PesiDestinations.pesi_test_local_CDM_ERMS2PESI():
	                PesiDestinations.pesi_test_local_CDM_ERMS2PESI_2()    ;

	static final boolean includeExport2PESI = false;

	static final UUID classificationUuid = UUID.fromString("6fa988a9-10b7-48b0-a370-2586fbc066eb");
	static final String classificationName = "ERMS 2019";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static final int partitionSize = 5000;
	//ignore null
	static final boolean ignoreNull = true;

// ***************** ALL ************************************************//

//	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
//	static final boolean doTaxa = true;
//	static final boolean doRelTaxa = doTaxa; //should always run with doTaxa because dependent on state from doTaxa
//    static final boolean doCommonNames = true;
//    static final boolean doNotes = true;
//	static final boolean doDistributions = true;
//	static final boolean doLinks = true;
//	static final boolean doImages = true;

//******************** NONE ***************************************//
//
	static final DO_REFERENCES doReferences = DO_REFERENCES.NONE;
	static final boolean doTaxa = false;
	static final boolean doRelTaxa = doTaxa; //should always run with doTaxa because depends on state from doTaxa
	static final boolean doCommonNames = true;
	static final boolean doNotes = true;
	static final boolean doDistributions = true;
	static final boolean doLinks =true;
	static final boolean doImages = true;

	private static DbSchemaValidation hbm2dll = (doReferences ==  DO_REFERENCES.ALL)? DbSchemaValidation.CREATE:DbSchemaValidation.VALIDATE;

	private void doImport(Source source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
		System.out.println("Start import from ("+ ermsSource.getDatabase() + ") to "+cdmDestination.getDatabase()+" ..." );

		//make ERMS Source

		ErmsImportConfigurator config = ErmsImportConfigurator.NewInstance(source,  destination);

		config.setClassificationUuid(classificationUuid);

		config.setIgnoreNull(ignoreNull);
		config.setDoReferences(doReferences);

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoLinks(doLinks);
		config.setDoDistributions(doDistributions);
		config.setDoVernaculars(doCommonNames);
		config.setDoNotes(doNotes);
		config.setDoImages(doImages);

		config.setDbSchemaValidation(hbm2dll);

		config.setCheck(check);
		config.setRecordsPerTransaction(partitionSize);
		config.setSourceRefUuid(PesiTransformer.uuidSourceRefErms);
		config.setClassificationName(classificationName);

		// invoke import
		CdmDefaultImport<ErmsImportConfigurator> ermsImport = new CdmDefaultImport<>();
		ermsImport.invoke(config);

		if (config.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || config.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK)    ){
			ICdmRepository app = ermsImport.getCdmAppController();
			ISourceable<?> obj = app.getCommonService().getSourcedObjectByIdInSource(TaxonName.class, "1000027", null);
			logger.info(obj);

//			//make feature tree
//			FeatureTree tree = TreeCreator.flatTree(featureTreeUuid, ermsImportConfigurator.getFeatureMap(), featureKeyList);
//			app = ermsImport.getCdmAppController();
//			app.getFeatureTreeService().saveOrUpdate(tree);
		}
		System.out.println("End import from ("+ source.getDatabase() + ") to "+cdmDestination.getDatabase() + "...");
	}

	public static void main(String[] args) {
		ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		ErmsImportActivator ermsImport = new ErmsImportActivator();
		ermsImport.doImport(ermsSource, cdmDB, hbm2dll);

		if (includeExport2PESI){
			PesiExportActivatorERMS ermsExport = new PesiExportActivatorERMS();
			ermsExport.doTaxa = doTaxa;
			ermsExport.doTreeIndex = doTaxa;
			ermsExport.doRelTaxa = doRelTaxa;
//            ermsExport.doInferredSynonyms = doRelTaxa;
            ermsExport.doDescriptions = doImages;

			ermsExport.doExport(cdmDB, pesiDestination);
		}
		System.exit(0);
	}
}
