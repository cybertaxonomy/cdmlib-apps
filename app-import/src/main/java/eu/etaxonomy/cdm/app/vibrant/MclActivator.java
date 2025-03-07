/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.vibrant;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.berlinModelImport.BerlinModelSources;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 * @author a.mueller
 */
public class MclActivator {

    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.mcl();

	static final ICdmDataSource cdmDestination = VibrantActivator.cdm_test_local_vibrant();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();

	static final boolean useSingleClassification = true;
	static final int sourceSecId = 1272;
	static final UUID classificationUuid = UUID.fromString("ba6efd26-5b45-4ce6-915d-4f9576e0bf0a");

	static final UUID sourceRefUuid = UUID.fromString("ca8b25d6-e251-4d2b-8b45-142e1e6448f7");

	// set to zero for unlimited nameFacts
	static final int maximumNumberOfNameFacts = 0;

	static final int partitionSize = 5000;

	//check - import
	static final CHECK check = CHECK.CHECK_AND_IMPORT;

	//editor - import
	static final EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;

	//ignore null
	static final boolean ignoreNull = true;

	static boolean useClassification = true;


// **************** ALL *********************


	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doNameStatus = true;

	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = true;



// **************** SELECTED *********************

//	//authors
//	static final boolean doAuthors = false;
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//	//names
//	static final boolean doTaxonNames = false;
//	static final boolean doRelNames = false;
//	static final boolean doNameStatus = false;
//
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = true;
//	static final boolean doFacts = false;



// **********Always IGNORE:***********************************************

	//etc.
	static final boolean doUser = false;
	static final boolean doTypes = false;   //not available in MCL
	static final boolean doNameFacts = false;  //not available in MCL
	static final boolean doOccurences = false;     //not available in MCL
	static final boolean doCommonNames = false;   //not available in MCL
	static final boolean doMarker = false;   //not available in MCL


	public void importMcl (Source source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") to " + cdmDestination.getDatabase() + " ...");
		//make BerlinModel Source

		BerlinModelImportConfigurator config = BerlinModelImportConfigurator.NewInstance(source,  destination);

		try {
			Method makeUrlMethod = this.getClass().getDeclaredMethod("makeUrlForTaxon", TaxonBase.class, ResultSet.class);
			config.setMakeUrlForTaxon(makeUrlMethod);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}


		config.setClassificationUuid(classificationUuid);
		config.setSourceSecId(sourceSecId);

		config.setNomenclaturalCode(nomenclaturalCode);

		config.setIgnoreNull(ignoreNull);
		config.setDoAuthors(doAuthors);
		config.setDoReferences(doReferences);
		config.setDoTaxonNames(doTaxonNames);
		config.setDoRelNames(doRelNames);
		config.setDoNameStatus(doNameStatus);
		config.setDoTypes(doTypes);
		config.setDoNameFacts(doNameFacts);
		config.setUseClassification(useClassification);
		config.setSourceRefUuid(sourceRefUuid);

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoFacts(doFacts);
		config.setDoOccurrence(doOccurences);
		config.setDoCommonNames(doCommonNames);

		config.setDoMarker(doMarker);
		config.setDoUser(doUser);
		config.setEditor(editor);
		config.setDbSchemaValidation(hbm2dll);

		// maximum number of name facts to import
		config.setMaximumNumberOfNameFacts(maximumNumberOfNameFacts);

		config.setUseSingleClassification(useSingleClassification);


		config.setCheck(check);
		config.setEditor(editor);
		config.setRecordsPerTransaction(partitionSize);


		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<BerlinModelImportConfigurator>();
		bmImport.invoke(config);

		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MclActivator importActivator = new MclActivator();
		Source source = berlinModelSource;
		ICdmDataSource cdmRepository = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		importActivator.importMcl(source, cdmRepository, hbm2dll);

	}

	public static ICdmDataSource cdm_test_local_mcl(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "mcl";
		String cdmUserName = "root";
		return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	private static final String URLbase = "http://ww2.bgbm.org/mcl/PTaxonDetail.asp?";
	public static Method makeUrlForTaxon(TaxonBase<?> taxon, ResultSet rs){
		Method result = null;
		ExtensionType urlExtensionType = ExtensionType.URL();
		int nameFk;
		try {
			nameFk = rs.getInt("PTNameFk");
		int refFkInt = rs.getInt("PTRefFk");
			if (nameFk != 0 && refFkInt != 0){
				String url = String.format(URLbase + "NameId=%s&PTRefFk=%s",nameFk, refFkInt);
				taxon.addExtension(url, urlExtensionType);
			}else{
				logger.warn("NameFk or refFkInt is 0. Can't create url");
			}
		} catch (SQLException e) {
			logger.warn("Exception when trying to access result set for url creation.");
		}

		return result;
	}

}
