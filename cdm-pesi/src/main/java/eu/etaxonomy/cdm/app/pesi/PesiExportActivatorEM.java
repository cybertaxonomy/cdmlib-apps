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
public class PesiExportActivatorEM {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PesiExportActivatorEM.class);

//	static final ICdmDataSource cdmSource = CdmDestinations.test_cdm_pesi_euroMed();
	static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_mysql_euromed();

	//database validation status (create, update, validate ...)
//	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI();
	static final Source pesiDestination = PesiDestinations.pesi_test_local_CDM_EM2PESI_2();

// ****************** ALL *****************************************

	boolean deleteAll = true;
	DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	boolean doTaxa = true;
	boolean doRelTaxa = true;
	boolean doDescriptions = true;

	boolean doPureNames = doTaxa;
	boolean doTreeIndex = doTaxa;
	boolean doParentAndBiota = doTaxa;
    boolean doAdditionalTaxonSource = false;  //do not exist in E+M
    boolean doInferredSynonyms = false;   //no inferred synonyms in E+M
	boolean doEcologyAndLink = false;   //do not exist in E+M

// ************************ NONE **************************************** //

//    boolean deleteAll = false;
//    DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//    boolean doTaxa = false;
//    boolean doPureNames = false;
//    boolean doTreeIndex = false;
//    boolean doParentAndBiota = false;
//    boolean doInferredSynonyms = false;   //no inferred synonyms in E+M
//    boolean doRelTaxa = false;
//    boolean doAdditionalTaxonSource = false;
//    boolean doDescriptions = false;
//    boolean doEcologyAndLink = false;


    //check - export
    static final CHECK check = CHECK.EXPORT_WITHOUT_CHECK;

    //Taxon names can't be mapped to their CDM ids as PESI Taxon table mainly holds taxa and there IDs. We ad nameIdStart to the TaxonName id to get a unique id
    static final int nameIdStart = 10000000;
    static final IdType idType = IdType.CDM_ID_WITH_EXCEPTIONS;

    static final int partitionSize = 1000;

	public boolean 	doExport(ICdmDataSource source){
	    System.out.println("Start export from " + source.getDatabase() + " to PESI ("+ pesiDestination.getDatabase() + ") ...");

		//make PESI Source
		Source destination = pesiDestination;
		PesiTransformer transformer = new PesiTransformer(destination);

		PesiExportConfigurator config = PesiExportConfigurator.NewInstance(destination, source, transformer);

		config.setDoTaxa(doTaxa);
		config.setDoPureNames(doPureNames);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoReferences(doReferences);
		config.setDoTreeIndex(doTreeIndex);
		config.setDoParentAndBiota(doParentAndBiota);
		config.setDoInferredSynonyms(doInferredSynonyms);
		config.setDoDescription(doDescriptions);
		config.setDoAdditionalTaxonSource(doAdditionalTaxonSource);
		config.setDoEcologyAndLink(doEcologyAndLink);

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
		PesiExportActivatorEM ex = new PesiExportActivatorEM();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;
//		Connection con = pesiDestination.getConnection();
//		System.out.println(con);
		ex.doExport(source);
		System.exit(0);
	}

}
