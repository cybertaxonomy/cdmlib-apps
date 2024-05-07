/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.csv.caryophyllales.out.CsvNameExportConfigurator;

/**
 * @author k.luther
 * @since 03.07.2015
 */
public class TaxonExcelCaryophyllalesActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

//	private static final ICdmDataSource destinationDb = CdmDestinations.cdm_production_caryophyllales_genus();
    private static final ICdmDataSource destinationDb = CdmDestinations.cdm_local_caryo();

    public static void main(String[] args) {

    	File file = new File("test.csv");
    	CsvNameExportConfigurator csvNameExportConfigurator =
    			CsvNameExportConfigurator.NewInstance(destinationDb, file);

    	csvNameExportConfigurator.setClassificationUUID(UUID.fromString("9edc58b5-de3b-43aa-9f31-1ede7c009c2b"));

    	CdmDefaultExport<CsvNameExportConfigurator> csvExport = new CdmDefaultExport<>();

		// invoke import
		//logger.debug("Invoking CSV name export");
    	csvExport.invoke(csvNameExportConfigurator);

    }
}