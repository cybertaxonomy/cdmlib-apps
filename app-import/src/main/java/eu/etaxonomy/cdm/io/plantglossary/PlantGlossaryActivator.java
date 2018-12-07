package eu.etaxonomy.cdm.io.plantglossary;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.util.TestDatabase;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;

public class PlantGlossaryActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PlantGlossaryActivator.class);

	private void doImport(ICdmDataSource cdmDestination) throws FileNotFoundException{

        FileInputStream inStream = new FileInputStream("/home/pplitzner/plantglossary.csv");
		PlantGlossaryCsvImportConfigurator config = PlantGlossaryCsvImportConfigurator.NewInstance(new InputStreamReader(inStream), cdmDestination);
		config.setCheck(CHECK.IMPORT_WITHOUT_CHECK);

		CdmDefaultImport<PlantGlossaryCsvImportConfigurator> myImport = new CdmDefaultImport<PlantGlossaryCsvImportConfigurator>();

		System.out.println("Start import from ("+ cdmDestination.toString() + ") ...");
		myImport.invoke(config);
		System.out.println("End import from ("+ cdmDestination.toString() + ")...");
	}

	public static void main(String[] args) {
		PlantGlossaryActivator me = new PlantGlossaryActivator();
		try {
	        ICdmDataSource dataSource = CdmDestinations.makeDestination(DatabaseTypeEnum.MySQL, "127.0.0.1", "additivity", 3306, "root", "root");
	        TestDatabase.initDb(dataSource, DbSchemaValidation.VALIDATE, true);
            me.doImport(dataSource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
