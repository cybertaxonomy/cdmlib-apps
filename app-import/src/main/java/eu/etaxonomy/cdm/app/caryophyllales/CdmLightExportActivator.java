/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import eu.etaxonomy.cdm.common.URI;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.util.TestDatabase;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.cdmLight.CdmLightExportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.ExportResult;
import eu.etaxonomy.cdm.io.common.ExportResultType;

/**
 * @author k.luther
 * @since 24.03.2017
 */
public class CdmLightExportActivator {
private static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_caryophyllales_nepenthaceae();

    // Export:
    private static String exportFileName = "file://C://Users//k.luther//Documents//Caryophyllales//OutputModel";


    private static final Logger logger = Logger.getLogger(CdmLightExportActivator.class);

    private ExportResult invokeExport(ICdmDataSource sourceParam, URI uri) {
//      String server = "localhost";
//      String database = "EDITimport";
//      String username = "edit";
//      sourceParam = CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));

        CdmLightExportConfigurator cdmlightExportConfigurator = new CdmLightExportConfigurator(null);

        CdmDefaultExport<CdmLightExportConfigurator> cdmLightExport = new CdmDefaultExport<>();


        // invoke export
        logger.debug("Invoking OutputModel export");
        ExportResult result = cdmLightExport.invoke(cdmlightExportConfigurator);
        return result;

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


    /**
     * @param args
     */
    public static void main(String[] args) {

        CdmLightExportActivator sc = new CdmLightExportActivator();
        ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;
      //  String file = chooseFile(args);

        try {

            sc.initDb(source);  //does this make sense here (it starts the appControler even if it is not needed later

            ExportResult result = sc.invokeExport(source, null);
            if (result.getExportData().getType().equals(ExportResultType.MAP_BYTE_ARRAY)){
                Map<String, byte[]> map = (Map<String, byte[]>)result.getExportData();

                for (String key:map.keySet()){
                    byte[] data =map.get(key);
                    String fileEnding =".csv";
                    File myFile = new File(exportFileName+File.separator + key + fileEnding);
                    FileOutputStream stream = new FileOutputStream(myFile);
                    try {
                        stream.write(data);
                        stream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
