/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
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
public class PesiExportActivatorERMS {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PesiExportActivatorERMS.class);

//	static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_mysql_erms();
    static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_mysql_erms2();

	//database validation status (create, update, validate ...)
	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI();
//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_FE2PESI();
//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI();

	//Taxon names can't be mapped to their CDM ids as PESI Taxon table mainly holds taxa and there IDs. We ad nameIdStart to the TaxonName id to get a unique id
	static final int nameIdStart = 10000000;
	static final IdType idType = IdType.CDM_ID_WITH_EXCEPTIONS;

	static final boolean deleteAll = true;

	static final int partitionSize = 1000;

	//check - export
	static final CHECK check = CHECK.EXPORT_WITHOUT_CHECK;


// ****************** ALL *****************************************

	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;

	//taxa
	static final boolean doTaxa = true;
	static final boolean doTreeIndex = true;
	static final boolean doInferredSynonyms = true;
	static final boolean doRelTaxa = true;
	static final boolean doDescriptions = true;


// ************************ NONE **************************************** //

	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//
//	//taxa
//	boolean doTaxa = true;
//	boolean doTreeIndex = true; //only with doTaxa
//	boolean doInferredSynonyms = true; //only with doTaxa
//	boolean doRelTaxa = true;
//	boolean doDescriptions = false;

//	static final boolean doNotes = false;
//	static final boolean doNoteSources = false;
//	static final boolean doAdditionalTaxonSource = false;
//	static final boolean doOccurrence = false;
//	static final boolean doOccurrenceSource = false;
//	static final boolean doImage = false;


	public boolean 	doExport(ICdmDataSource source){
		System.out.println("Start export to PESI ("+ pesiDestination.getDatabase() + ") ...");

		//make PESI Source
		Source destination = pesiDestination;
		PesiTransformer transformer = new PesiTransformer(destination);

		PesiExportConfigurator config = PesiExportConfigurator.NewInstance(destination, source, transformer);

		config.setDoTreeIndex(doTreeIndex); //only with doTaxa
		config.setDoInferredSynonyms(doInferredSynonyms); //only with doTaxa

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoReferences(doReferences);
		config.setDoDescription(doDescriptions);

//		config.setDoOccurrence(doOccurrence);
//		config.setDoOccurrenceSource(doOccurrenceSource);
//		config.setDoNotes(doNotes);
//		config.setDoNoteSources(doNoteSources);
//		config.setDoImages(doImage);
//		config.setDoAdditionalTaxonSource(doAdditionalTaxonSource);

		config.setCheck(check);
		config.setLimitSave(partitionSize);
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
		PesiExportActivatorERMS ex = new PesiExportActivatorERMS();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;

		ex.doExport(source);
		System.exit(0);
	}
}
