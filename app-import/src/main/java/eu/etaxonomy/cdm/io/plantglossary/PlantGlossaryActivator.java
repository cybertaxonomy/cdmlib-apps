package eu.etaxonomy.cdm.io.plantglossary;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;

public class PlantGlossaryActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PlantGlossaryActivator.class);

	private void doImport(ICdmDataSource cdmDestination) throws IOException{

	    /*
	     * Source file:
	     * extracted data from https://terms.tdwg.org
	     *
	     * Cleaning data with OpenRefine:
	     *  - generated URI column
	     *  - parsed term description by crawling term html pages (description are not retrieved via web interface)
	     */
	    URI uri = URI.create("file:/home/pplitzner/projects/Additivity/plant_glossary_states.csv");
		PlantGlossaryCsvImportConfigurator config = PlantGlossaryCsvImportConfigurator.NewInstance(uri, cdmDestination);
		config.setCheck(CHECK.IMPORT_WITHOUT_CHECK);
		config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

		CdmDefaultImport<PlantGlossaryCsvImportConfigurator> myImport = new CdmDefaultImport<>();

		System.out.println("Start import from ("+ cdmDestination.toString() + ") ...");
		myImport.invoke(config);
		System.out.println("End import from ("+ cdmDestination.toString() + ")...");
	}

	public static void main(String[] args) {
		PlantGlossaryActivator activator = new PlantGlossaryActivator();
		try {
	        ICdmDataSource dataSource = CdmDestinations.makeDestination(DatabaseTypeEnum.MySQL, "127.0.0.1", "empty", 3306, "root", null);
            activator.doImport(dataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
