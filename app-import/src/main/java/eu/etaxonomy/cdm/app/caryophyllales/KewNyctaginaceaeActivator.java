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

import eu.etaxonomy.cdm.app.berlinModelImport.SourceBase;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.caryo.KewExcelTaxonImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Import Kew WCVP Nyctaginaceae data to CDM Caryophyllales.
 *
 * @author a.mueller
 * @since 12.01.2023
 */
public class KewNyctaginaceaeActivator extends SourceBase{

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	private static final DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	private static final URI source = nyctaginaceae();

	private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();
//    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();


	//classification
	static final UUID classificationUuid = UUID.fromString("9edc58b5-de3b-43aa-9f31-1ede7c009c2b");

	static final UUID rootTaxonUuid = UUID.fromString("7040cc71-4f0d-4fd9-9384-eb4d003274aa"); //taxon.uuid not taxonNode.uuid , here Nyctaginaceae
	//for taxa/names declared as unplaced
	static final UUID unplacedTaxonUuid = UUID.fromString("9835a6e5-3b8a-49b5-a692-5da531ef8634");
	//for orphaned synonyms
	static final UUID orphanedPlaceholderTaxonUuid = UUID.fromString("cc5cec9b-b0c7-432a-8d10-31cbb3685d35");


	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//taxa
	static final boolean doTaxa = true;

	private void doImport(ICdmDataSource cdmDestination){

		//make Source
	    KewExcelTaxonImportConfigurator config= KewExcelTaxonImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setRootTaxonUuid(rootTaxonUuid);
		config.setUnplacedTaxonUuid(unplacedTaxonUuid);
		config.setOrphanedPlaceholderTaxonUuid(orphanedPlaceholderTaxonUuid);
		config.setCheck(check);
//		config.setDoTaxa(doTaxa);
		config.setDbSchemaValidation(hbm2dll);
		config.setSourceReferenceTitle("Nyctaginaceae_Export_Kew4CDM-Import.xlsx");

		CdmDefaultImport<KewExcelTaxonImportConfigurator> myImport = new CdmDefaultImport<>();

		//...
		if (true){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}
	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}


	public static URI nyctaginaceae(){
      String fileName = "Nyctaginaceae_Export_Kew4CDM-Import.xlsx";
//      URI uri = URI.create("file:////BGBM-PESIHPC/Caryophyllales/Kew/" +  fileName);
      File file = new File("E://data/Caryophyllales/Kew/" +  fileName);
      if (!file.exists()) {
          System.exit(0);
      }
      URI uri = URI.fromFile(file);
//      URI uri = URI.create("file://E:/Caryophyllales/Kew/" +  fileName);
      return uri;
	}

	public static void main(String[] args) {
		KewNyctaginaceaeActivator me = new KewNyctaginaceaeActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}
}