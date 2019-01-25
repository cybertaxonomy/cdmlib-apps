package eu.etaxonomy.cdm.io.plantglossary.kew;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;

/**
 *
 * @author pplitzner
 * @since Jan 25, 2019
 *
 */
public class KewPlantGlossaryActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(KewPlantGlossaryActivator.class);

	private void doImport(ICdmDataSource cdmDestination) throws IOException{

	    /*
	     * Source file:
	     * extracted data from Kew Plant Glossary
	     *
	     */
	    URI uri = URI.create("file:/home/pplitzner/projects/Additivity/KEW_Plant_Glossary.csv");
		KewPlantGlossaryCsvImportConfigurator config = KewPlantGlossaryCsvImportConfigurator.NewInstance(uri, cdmDestination);
		config.setCheck(CHECK.IMPORT_WITHOUT_CHECK);
		config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

		CdmDefaultImport<KewPlantGlossaryCsvImportConfigurator> myImport = new CdmDefaultImport<>();

		System.out.println("Start import from ("+ cdmDestination.toString() + ") ...");
		myImport.invoke(config);
		System.out.println("End import from ("+ cdmDestination.toString() + ")...");
	}

	public static void main(String[] args) {
		KewPlantGlossaryActivator activator = new KewPlantGlossaryActivator();
		try {
	        ICdmDataSource dataSource = CdmDestinations.makeDestination(DatabaseTypeEnum.MySQL, "127.0.0.1", "empty", 3306, "root", null);
            activator.doImport(dataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
