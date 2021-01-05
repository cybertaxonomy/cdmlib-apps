/**
 * Copyright (C) 2008 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.app.jaxb;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.util.TestDatabase;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.jaxb.JaxbImportConfigurator;

/**
 * @author a.babadshanjan
 * @since 25.09.2008
 *
 * NOTE: the result may go into
 * cdmlib-persistence\target\test-classes\eu\etaxonomy\cdm\h2\LocalH2
 */
public class JaxbImportActivator {

	/* SerializeFrom DB **/
//	private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
	//if you run from IDE it may run into {cdmlib-folder}\cdmlib-persistence\target\test-classes\eu\etaxonomy\cdm\h2\LocalH2\
	private static final ICdmDataSource cdmDestination = CdmDestinations.localH2Armeria();

	// Import:
	private static String importFileNameString =
		//"C:\\workspace\\cdmlib_2.2\\cdmlib-io\\src\\test\\resources\\eu\\etaxonomy\\cdm\\io\\jaxb\\export_test_app_import.xml";
//		"file:/C:/export_test_app_import.xml";
//	"file:/C:/opt/data/rl/201406041541-jaxb_export-Regenwuermer.xml";
//	"file:/C:/opt/data/rl/201406241132-jaxb_export-Armeria.xml";
//	"file:/F:/data/redlist/standardliste/standardliste_jaxb.xml";
//	"//PESIIMPORT3/redlist/standardliste/standardliste_jaxb.xml";
	"//PESIIMPORT3/redlist/201411261506-jaxb_export-armeria_demo_local.xml";


	/** NUMBER_ROWS_TO_RETRIEVE = 0 is the default case to retrieve all rows.
	 * For testing purposes: If NUMBER_ROWS_TO_RETRIEVE >0 then retrieve
	 *  as many rows as specified for agents, references, etc.
	 *  Only root taxa and no synonyms and relationships are retrieved. */
	private static final int NUMBER_ROWS_TO_RETRIEVE = 0;

	private static final Logger logger = Logger.getLogger(JaxbImportActivator.class);

	private void invokeImport(String importFileParamString, ICdmDataSource destination) {
		try {
			JaxbImportConfigurator jaxbImportConfigurator;
			if (importFileParamString !=null && destination != null){
				URI importFileParam = new URI(importFileParamString);
				jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importFileParam, destination);
			}else if (destination != null){
				URI importFileName = new URI(importFileNameString);
				jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importFileName, destination);
			} else if (importFileParamString !=null ){
				URI importFileParam = new URI(importFileParamString);
				jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importFileParam, cdmDestination);
			} else{
				URI importFileName = new URI(importFileNameString);
				jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importFileName, cdmDestination);
			}

			CdmDefaultImport<JaxbImportConfigurator> jaxbImport =
				new CdmDefaultImport<JaxbImportConfigurator>();


			// invoke import
			logger.debug("Invoking Jaxb import");

			jaxbImport.invoke(jaxbImportConfigurator, destination, true);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private void invokeImport(URI importUri, ICdmDataSource destination) {
		JaxbImportConfigurator jaxbImportConfigurator;
		if (destination != null){
			jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importUri, destination);
		}else {
			jaxbImportConfigurator = JaxbImportConfigurator.NewInstance(importUri, cdmDestination);
		}

		CdmDefaultImport<JaxbImportConfigurator> jaxbImport =
			new CdmDefaultImport<JaxbImportConfigurator>();

		// invoke import
		logger.debug("Invoking Jaxb import");

		jaxbImport.invoke(jaxbImportConfigurator, destination, true);
	}

	private CdmApplicationController initDb(ICdmDataSource db) {
		// Init source DB
		CdmApplicationController appCtrInit = null;
		appCtrInit = TestDatabase.initDb(db, DbSchemaValidation.CREATE, false);
		return appCtrInit;
	}

	// Load test data to DB
	private void loadTestData(CdmApplicationController appCtrInit) {

		TestDatabase.loadTestData("", appCtrInit);
	}

	public static String chooseFile(String[] args) {
		if(args == null) {
            return null;
        }
		for (String dest: args){
			if (dest.endsWith(".xml")){
				return args[0];
			}
		}
		return null;
	}

	public static void main(String[] args) {

		JaxbImportActivator jia = new JaxbImportActivator();
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		String fileStr = chooseFile(args)!= null ? chooseFile(args) : importFileNameString;
		File file = new File(fileStr);

		URI uri = URI.fromFile(file);
		System.out.println(new File(uri.getJavaUri()).exists());
		if (! new File(uri.getJavaUri()).exists()){
			System.out.println("File does not exist! Exit");
			return;
		}
		jia.invokeImport(uri, destination);
	}
}