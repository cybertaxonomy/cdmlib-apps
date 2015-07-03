package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.net.URI;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.csv.caryophyllales.out.CsvNameExportBase;
import eu.etaxonomy.cdm.io.csv.caryophyllales.out.CsvNameExportConfigurator;
import eu.etaxonomy.cdm.io.csv.redlist.out.CsvExportBaseRedlist;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;








public class TaxonExcelCaryophyllalesActivator {
	
	
	//	private static final Logger logger = Logger.getLogger(TaxonExcelCaryophyllalesActivator.class);


		static final String source = XlsSources.xls_nyctaginaceae();
//			

		private static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.CREATE;

		private static final ICdmDataSource destinationDb = CdmDestinations.cdm_production_caryophyllales();
		//private static final ICdmDataSource destinationDb = CdmDestinations.mon_cdm();

	    public static void main(String[] args) {

	    	NomenclaturalCode code = NomenclaturalCode.ICNAFP;
	    	File csv = new File("test.csv");
	    	CsvNameExportConfigurator csvNameExportConfigurator =
	    			CsvNameExportConfigurator.NewInstance(destinationDb, csv);
	    	
	    	CsvNameExportBase export = new CsvNameExportBase();
	    	
	    	//export.invoke(csvNameExportConfigurator.getNewState());
	    	CdmDefaultExport<CsvNameExportConfigurator> normalExplicitImport =
				new CdmDefaultExport<CsvNameExportConfigurator>();

			// invoke import
			//logger.debug("Invoking Normal Explicit Excel import");
			normalExplicitImport.invoke(csvNameExportConfigurator);

	    }
	}


