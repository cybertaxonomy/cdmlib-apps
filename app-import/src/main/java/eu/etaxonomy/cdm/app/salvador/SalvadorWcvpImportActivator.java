/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.salvador;

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
 * Import Kew WCVP data to CDM Salvador dendroflora.
 *
 * @author a.mueller
 * @since 12.01.2023
 */
public class SalvadorWcvpImportActivator extends SourceBase{

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	private static final DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	private static final URI source = wcvpSalvador();

	private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_salvador();
//    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_salvador_flora();

	//classification
	static final UUID classificationUuid = UUID.fromString("1cd4dfa6-8702-4006-b10c-80694526664a");

	//taxon.uuid not taxonNode.uuid , here DUMMY
	static final UUID rootTaxonUuid = UUID.fromString("c4de8069-5805-49f4-9617-7341c235bd61");
	//for taxa/names declared as unplaced
	static final UUID unplacedTaxonUuid = UUID.fromString("ab5d37a2-6101-428d-8720-c298cb79b4d3");
	//for orphaned synonyms
	//for Salvador we use the same as unplaced
	static final UUID orphanedPlaceholderTaxonUuid = UUID.fromString("ab5d37a2-6101-428d-8720-c298cb79b4d3");
	static final String fileName = "Import_4_Flora-El-Salvador-v2.xlsx";
//    static final String fileName = "Import_4_Flora-El-Salvador-v2_test.xlsx";
	static final String sourceReferenceTitle = fileName;
	static final boolean useNewNomRefIfNotExists = true;

	//sec uuid = "403474fd-c31c-4c4e-a960-fbcf2d625e20"


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
		config.setUseNewNomRefIfNotExists(useNewNomRefIfNotExists);

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


	public static URI wcvpSalvador(){
      File file = new File("E://data/Salvador/" +  fileName);
      if (!file.exists()) {
          System.exit(0);
      }
      URI uri = URI.fromFile(file);
      return uri;
	}

	public static void main(String[] args) {
		SalvadorWcvpImportActivator me = new SalvadorWcvpImportActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}
}