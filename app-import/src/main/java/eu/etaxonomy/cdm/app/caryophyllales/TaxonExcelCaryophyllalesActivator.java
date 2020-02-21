package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.csv.caryophyllales.out.CsvNameExportConfigurator;


public class TaxonExcelCaryophyllalesActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TaxonExcelCaryophyllalesActivator.class);

	private static final ICdmDataSource destinationDb = CdmDestinations.cdm_production_caryophyllales_genus();

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


