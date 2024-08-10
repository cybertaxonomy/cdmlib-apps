/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.cdmLight.CdmLightExportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.ExportDataWrapper;
import eu.etaxonomy.cdm.io.common.ExportResult;
import eu.etaxonomy.cdm.io.common.ExportResultType;

/**
 * @author k.luther
 * @since 24.03.2017
 */
public class EuroMedCdmLightExportActivator {

    private static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_euromed();
//    private static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_euromed();

    private static final Logger logger = LogManager.getLogger();

    //Folder
    private static String exportFolder = "C://Users//muellera//tmp//export//cdmlight_em";
    //Filter
    //... few data for testing
//    UUID subtreeUuid = UUID.fromString("77484764-4cde-4582-97a8-4ae0794092b2");
    //allData
    UUID subtreeUuid = UUID.fromString("f13529f2-2644-43e0-9bf5-58f767bcfd77");


    private ExportResult invokeExport(ICdmDataSource source, URI uri) {
        CdmLightExportConfigurator config = CdmLightExportConfigurator.NewInstance();
        config.setSource(source);
        TaxonNodeFilter subtreeFilter = TaxonNodeFilter.NewSubtreeInstance(subtreeUuid);
        config.setTaxonNodeFilter(subtreeFilter);
        config.setCreateZipFile(true);  //TODO has no effect in this script
        config.setDoFactualData(false);

        CdmDefaultExport<CdmLightExportConfigurator> cdmLightExport = new CdmDefaultExport<>();

        // invoke export
        logger.debug("Invoking CDM light export");
        ExportResult result = cdmLightExport.invoke(config);
        return result;

    }

    public static void main(String[] args) {

//        String testName = "C://Users/muellera/tmp/export\\CommonNameFact.csv";
//        File testFile = new File(testName);
//        try {
//            testFile.createNewFile();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        try {
            EuroMedCdmLightExportActivator sc = new EuroMedCdmLightExportActivator();
            ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;

            ExportResult result = sc.invokeExport(source, null);
            ExportDataWrapper<?> expData = result.getExportData();
            if (expData.getType().equals(ExportResultType.MAP_BYTE_ARRAY)){
                Map<String, byte[]> map = (Map<String, byte[]>)expData.getExportData();

                for (String key:map.keySet()){
                    byte[] data =map.get(key);
                    String fileEnding =".csv";
                    String fileName = exportFolder+File.separator + key + fileEnding;
                    System.out.println(fileName);
                    File myFile = new File(fileName);
                    try {
                        myFile.createNewFile();
                        FileOutputStream stream = new FileOutputStream(myFile);
                        stream.write(data);
                        stream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);

    }
}