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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.tasks.TestDatabase;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.jaxb.JaxbExportConfigurator;

/**
 * @author a.babadshanjan
 * @since 25.09.2008
 */
public class JaxbExportActivator {

	/* SerializeFrom DB **/
//	private static final ICdmDataSource cdmSource = CdmDestinations.localH2();
	private static final ICdmDataSource cdmSource = CdmDestinations.cdm_production_flora_deutschland();

	// Export:
	private static String exportFileName = "file:/F:/data/redlist/standardliste/standardliste_jaxb.xml";

	/** NUMBER_ROWS_TO_RETRIEVE = 0 is the default case to retrieve all rows.
	 * For testing purposes: If NUMBER_ROWS_TO_RETRIEVE >0 then retrieve
	 *  as many rows as specified for agents, references, etc.
	 *  Only root taxa and no synonyms and relationships are retrieved. */
	private static final int NUMBER_ROWS_TO_RETRIEVE = 0;

    private static final Logger logger = LogManager.getLogger();

	private void invokeExport(ICdmDataSource sourceParam, File file) {
//		String server = "localhost";
//		String database = "EDITimport";
//		String username = "edit";
//		sourceParam = CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));

		JaxbExportConfigurator jaxbExportConfigurator;
		if (file !=null && sourceParam != null){
			jaxbExportConfigurator = JaxbExportConfigurator.NewInstance(sourceParam, file);
		}else if (sourceParam != null){
			jaxbExportConfigurator = JaxbExportConfigurator.NewInstance(sourceParam, new File(exportFileName));
		} else if (file !=null ){
			jaxbExportConfigurator = JaxbExportConfigurator.NewInstance(cdmSource, file);
		} else{
			jaxbExportConfigurator = JaxbExportConfigurator.NewInstance(cdmSource, new File(exportFileName));
		}

		CdmDefaultExport<JaxbExportConfigurator> jaxbExport =
			new CdmDefaultExport<JaxbExportConfigurator>();

		// invoke export
		logger.debug("Invoking Jaxb export");
		jaxbExport.invoke(jaxbExportConfigurator);

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

	private CdmApplicationController initDb(ICdmDataSource db) {
		// Init source DB
		CdmApplicationController appCtrInit = TestDatabase.initDb(db, DbSchemaValidation.VALIDATE, false);
		return appCtrInit;
	}

	// Load test data to DB
	private void loadTestData(CdmApplicationController appCtrInit) {
		TestDatabase.loadTestData("", appCtrInit);
	}

	public static void main(String[] args) {

		JaxbExportActivator sc = new JaxbExportActivator();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;
		String file = chooseFile(args);
		if (file == null){
			file = exportFileName;
		}
		URI uri = URI.create(file);
		try {
			File myFile = new File(uri.getJavaUri());
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(myFile), "UTF8"), true);
			sc.initDb(source);  //does this make sense here (it starts the appControler even if it is not needed later

			sc.invokeExport(source, new File(file));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}