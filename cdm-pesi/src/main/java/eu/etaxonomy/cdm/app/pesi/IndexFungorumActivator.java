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
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.indexFungorum.IndexFungorumImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data base under another operation systen 
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 * 
 * 
 * @author a.mueller
 *
 */
public class IndexFungorumActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(IndexFungorumActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source indexFungorumSource = PesiSources.PESI_IF();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi_erms();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_indexFungorum();
	static final UUID treeUuid = UUID.fromString("4bea48c3-eb10-41d1-b708-b5ee625ed243");
	
	//check - import
	static final CHECK check = CHECK.CHECK_AND_IMPORT;

	static final int partitionSize = 1000;


	//NomeclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICBN;

	//ignore null
	static final boolean ignoreNull = true;
	
// ***************** ALL ************************************************//
	
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	
	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doOccurences = true;
	
	
//******************** NONE ***************************************//
	

//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//	
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = false;
//	static final boolean doOccurences = false;
//	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start import from ("+ indexFungorumSource.getDatabase() + ") ...");
		
		//make IF Source
		Source source = indexFungorumSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		
		IndexFungorumImportConfigurator ifImportConfigurator = IndexFungorumImportConfigurator.NewInstance(source,  destination);
		
		ifImportConfigurator.setClassificationUuid(treeUuid);
		ifImportConfigurator.setNomenclaturalCode(nomenclaturalCode);

		ifImportConfigurator.setIgnoreNull(ignoreNull);
		ifImportConfigurator.setDoReferences(doReferences);
		
		ifImportConfigurator.setDoTaxa(doTaxa);
		ifImportConfigurator.setDoRelTaxa(doRelTaxa);
		ifImportConfigurator.setDoOccurrence(doOccurences);
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

}
