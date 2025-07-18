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
public class PesiExportActivator {

    private static Logger logger = LogManager.getLogger();


	static final ICdmDataSource cdmSource = CdmDestinations.cdm_pesi2025_final();
//	static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_pesi_faunaEu();
//	static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_mysql_test();

//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI();
//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_FE2PESI();
	static final Source pesiDestination = PesiDestinations.pesisql_DW_2025_1();
//	static final Source pesiDestination = PesiDestinations.pesisql_DW_2025_2();

	//Taxon names can't be mapped to their CDM ids as PESI Taxon table mainly holds taxa and their IDs.
	//We add nameIdStart to the TaxonName id to get a unique id
	static final int nameIdStart = 10000000;

	static final int partitionSize = 5000;

	static final boolean deleteAll = true;

	// !!!!!!!!!!!!!!
//	static final int startDescriptionPartition = 0;
//	static final int maxDescriptionPartitions = 10000;

	//!!!!!!!!!!!!!!!

	//check - export
	static final CHECK check = CHECK.EXPORT_WITHOUT_CHECK;


	static final IdType idType = IdType.CDM_ID_WITH_EXCEPTIONS;

// ****************** ALL *****************************************

	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;

	//taxa
	private static final boolean doTaxa = true;
	private static final boolean doPureNames = true;
	private static final boolean doInferredSynonyms = false;
	private static final boolean doTreeIndex = true;
	private static final boolean doRelTaxa = true;
	private static final boolean doAdditionalTaxonSource = true;
	private static final boolean doDescription = true;
	private static final boolean doEcologyAndLink = true;

// ************************ NONE **************************************** //

//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//
//	//taxa
//    private static final boolean doTaxa = false;
//    private static final boolean doPureNames = false;
//    private static final boolean doTreeIndex = false;
//    private static final boolean doInferredSynonyms = false;
//    private static final boolean doRelTaxa = false;
//    private static final boolean doAdditionalTaxonSource = false;
//    private static final boolean doDescription = true;
//    private static final boolean doEcologyAndLink = false;

	public boolean 	doExport(ICdmDataSource source){

	    logger.warn("Start logging");
	    System.out.println("Start export to PESI ("+ pesiDestination.getDatabase() + ") ...");

		//make PESI Source
		Source destination = pesiDestination;
		PesiTransformer transformer = new PesiTransformer(destination);

		PesiExportConfigurator config = PesiExportConfigurator.NewInstance(destination, source, transformer);

		config.setDoReferences(doReferences);
		config.setDoTaxa(doTaxa);
		config.setDoTreeIndex(doTreeIndex);
		config.setDoInferredSynonyms(doInferredSynonyms);
		config.setDoPureNames(doPureNames);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoAdditionalTaxonSource(doAdditionalTaxonSource);
		config.setDoDescription(doDescription);
		config.setDoEcologyAndLink(doEcologyAndLink);

		config.setCheck(check);
		config.setLimitSave(partitionSize);
//		config.setStartDescriptionPartition(startDescriptionPartition);
//		config.setMaxDescriptionPartitions(maxDescriptionPartitions);
		config.setIdType(idType);
		config.setNameIdStart(nameIdStart);
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
		PesiExportActivator ex = new PesiExportActivator();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;

		ex.doExport(source);
		System.exit(0);
	}
}