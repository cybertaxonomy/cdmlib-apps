/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.redlist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.ExportDataWrapper;
import eu.etaxonomy.cdm.io.common.ExportResult;
import eu.etaxonomy.cdm.io.common.ExportResultType;
import eu.etaxonomy.cdm.io.redlist.moose.KoperskiExportConfigurator;

/**
 * Exports the Koperski et al. mosses to the BfN format.
 *
 * @author a.mueller
 * @since 05.08.2023
 */
public class KoperskiExportActivator {

    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_test_mysql();

    // Export:
    private static String exportFileName = "KoperskiExport";

    private void invokeExport(ICdmDataSource sourceParam, URI uri) {

    	//config
    	KoperskiExportConfigurator config = KoperskiExportConfigurator.NewInstance();
        TaxonNodeFilter filter = TaxonNodeFilter.NewClassificationInstance(KoperskiImportActivator.classificationUuid);
        config.setTaxonNodeFilter(filter);
        
        //export
    	CdmDefaultExport<KoperskiExportConfigurator> export = new CdmDefaultExport<>();
        config.setCreateZipFile(true);
        config.setSource(cdmSource);

        ExportResult result = export.invoke(config);
        handleResult(result, uri);
        return;
    }
    
	private void handleResult(ExportResult result, URI uri) {
		try {
			ExportResultType type = result.getExportData().getType();
			if (type.equals(ExportResultType.MAP_BYTE_ARRAY)){
			    ExportDataWrapper<?> exportData = result.getExportData();
			    Map<String, byte[]> map = (Map<String, byte[]>)exportData.getExportData();
			    for (String key:map.keySet()){
			        byte[] data =map.get(key);
			        String fileEnding =".csv";
			        
//			        File myFile = new File(destination() + File.separator + key + fileEnding);
			        File file = new File("C:\\Users\\muellera\\tmp\\export\\"+exportFileName+ File.separator + key + fileEnding);
			        boolean ex = file.exists();
			        if (!ex) {
			        	file.createNewFile();       	
			        }
//			        FileOutputStream fos = new FileOutputStream(file);
			        try {
			        	String s = new String(data, StandardCharsets.UTF_8);
			        	System.out.println(s);
			        	FileUtils.writeStringToFile(file, s, StandardCharsets.UTF_8);
//			        	BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8);
//			        	Files.writ
			        	//			        	OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
//			        	writer.close();
			        } catch (IOException e) {
			            // TODO Auto-generated catch block
			            e.printStackTrace();
			        }
			    }
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    private static URI destination(){
    	File file = new File("C:\\Users\\muellera\\tmp\\export\\" + exportFileName );
        return URI.fromFile(file);
    }

    public static void main(String[] args) {
    	KoperskiExportActivator sc = new KoperskiExportActivator();
    	sc.invokeExport(cdmSource, destination());
    	System.exit(0);
    }
}