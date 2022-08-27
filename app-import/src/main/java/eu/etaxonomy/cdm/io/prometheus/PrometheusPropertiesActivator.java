package eu.etaxonomy.cdm.io.prometheus;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;

/**
 * @author pplitzner
 * @date 18.02.2019
 */
public class PrometheusPropertiesActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	private void doImport(ICdmDataSource cdmDestination) throws IOException{

	    /*
	     * Source file:
	     * extracted data from xml-exported prometheus ontology
	     */
	    URI uri = URI.create("file:/home/pplitzner/projects/Additivity/prometheus_extract/new_qualitative_properties.csv");
		PrometheusPropertiesCsvImportConfigurator config = PrometheusPropertiesCsvImportConfigurator.NewInstance(uri, cdmDestination);
		config.setCheck(CHECK.IMPORT_WITHOUT_CHECK);
		config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

		CdmDefaultImport<PrometheusPropertiesCsvImportConfigurator> myImport = new CdmDefaultImport<>();

		System.out.println("Start import from ("+ cdmDestination.toString() + ") ...");
		myImport.invoke(config);
		System.out.println("End import from ("+ cdmDestination.toString() + ")...");
	}

	public static void main(String[] args) {
		PrometheusPropertiesActivator activator = new PrometheusPropertiesActivator();
		try {
	        ICdmDataSource dataSource = CdmDestinations.makeDestination(DatabaseTypeEnum.MySQL, "127.0.0.1", "empty", 3306, "root", null);
//		    ICdmDataSource dataSource = CdmDestinations.cdm_additivity_ontology();
            activator.doImport(dataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
