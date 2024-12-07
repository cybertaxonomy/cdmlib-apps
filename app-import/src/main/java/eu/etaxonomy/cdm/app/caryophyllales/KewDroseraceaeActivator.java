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
public class KewDroseraceaeActivator extends SourceBase{

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	private static final DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	private static final URI source = nyctaginaceae();

//	private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo2();
    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales();

    //NOTE: BE CAREFUL WITH SYNONYMY OF TAXA THAT EXIST IN PUBLISHED TREE. THE TAXA SEEM
    //      TO BE REUSED AND THEREFOR THE SYNONYMS ARE ALSO VISIBLE THERE. BETTER TRY
    //      NOT TO REUSE TAXA.

	//classification
	static final UUID classificationUuid = UUID.fromString("9edc58b5-de3b-43aa-9f31-1ede7c009c2b");

	//taxon.uuid not taxonNode.uuid , here Droseraceae
	static final UUID rootTaxonUuid = UUID.fromString("5019dc80-6703-46a3-88ac-5c9d73da7d1c");
	//for taxa/names declared as unplaced
	static final UUID unplacedTaxonUuid = UUID.fromString("35ad50e8-8c74-4100-9a54-86ce7db2714f");
	//for orphaned synonyms
	static final UUID orphanedPlaceholderTaxonUuid = UUID.fromString("d826a05a-5b15-440a-95e4-c36afb6a303e");
	static final String sourceReferenceTitle = "Droseraceae_WCVP3.xlsx";


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
		config.setSourceReferenceTitle(sourceReferenceTitle);

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
      String fileName = "Droseraceae_WCVP3.xlsx";
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
		KewDroseraceaeActivator me = new KewDroseraceaeActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}
}