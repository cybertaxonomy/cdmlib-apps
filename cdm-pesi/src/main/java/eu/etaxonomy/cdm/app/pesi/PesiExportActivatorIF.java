/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.PesiDestinations;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.DbExportConfiguratorBase.IdType;
import eu.etaxonomy.cdm.io.common.IExportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IExportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiExportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;

/**
 * @author a.mueller
 * @author e.-m.lee
 * @since 16.02.2010
 */
public class PesiExportActivatorIF {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_indexFungorum2();
	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_IF2PESI();
//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_IF2PESI_2();


	static final IdType idType = IdType.CDM_ID;
	//check - export
	static final CHECK check = CHECK.EXPORT_WITHOUT_CHECK;
	static final int partitionSize = 2000;

// ****************** ALL *****************************************

    boolean deleteAll = true;
    DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
    boolean doTaxa = true;
    boolean doRelTaxa = true;
    boolean doDescriptions = true;

    boolean doTreeIndex = doTaxa;
    boolean doParentAndBiota = doTaxa;

    boolean doPureNames = false;  //!!
    boolean doAdditionalTaxonSource = true;  //existing??
    boolean doInferredSynonyms = false;   //no inferred synonyms in IF
    boolean doEcologyAndLink = false;   //do not exist in IF


// ************************ NONE **************************************** //

//    boolean deleteAll = false;
//    DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//    boolean doTaxa = false;
//    boolean doRelTaxa = false;
//    boolean doDescriptions = false;
//
//    boolean doTreeIndex = doTaxa;
//    boolean doParentAndBiota = doTaxa;
//
//    boolean doPureNames = false;  //!!
//    boolean doAdditionalTaxonSource = false;  //existing??
//    boolean doInferredSynonyms = false;   //no inferred synonyms in IF
//    boolean doEcologyAndLink = false;   //do not exist in IF


	public boolean 	doExport(ICdmDataSource source){
		System.out.println("Start export to PESI ("+ pesiDestination.getDatabase() + ") ...");

		//make PESI Source
		Source destination = pesiDestination;
		PesiTransformer transformer = new PesiTransformer(destination);

		PesiExportConfigurator config = PesiExportConfigurator.NewInstance(destination, source, transformer);

		config.setDoReferences(doReferences);
		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoParentAndBiota(doParentAndBiota);
		config.setDoTreeIndex(doTreeIndex);
		config.setDoInferredSynonyms(doInferredSynonyms);
		config.setDoPureNames(doPureNames);
		config.setDoDescription(doDescriptions);
		config.setDoAdditionalTaxonSource(doAdditionalTaxonSource);
		config.setDoEcologyAndLink(doEcologyAndLink);

		config.setCheck(check);
		config.setLimitSave(partitionSize);
		config.setIdType(idType);
		if (deleteAll){
			destination.update("EXEC sp_deleteAllData");
		}

		// invoke export
		CdmDefaultExport<PesiExportConfigurator> pesiExport = new CdmDefaultExport<>();

		boolean result = pesiExport.invoke(config).isSuccess();

		System.out.println("End export to PESI ("+ destination.getDatabase() + ")..." + (result? "(successful)":"(with errors)"));
		return result;
	}

	public static void main(String[] args) {
		PesiExportActivatorIF ex = new PesiExportActivatorIF();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;
		ex.doExport(source);
		System.exit(0);
	}
}
