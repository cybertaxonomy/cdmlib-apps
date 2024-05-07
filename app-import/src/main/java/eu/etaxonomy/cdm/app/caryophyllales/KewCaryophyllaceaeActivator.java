/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

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
 * @author a.mueller
 * @since 05.01.2022
 */
public class KewCaryophyllaceaeActivator extends SourceBase{

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	private static final DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	private static final URI source = caryophyllaceae();

	private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();
//    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();


	//classification
	static final UUID classificationUuid = UUID.fromString("9edc58b5-de3b-43aa-9f31-1ede7c009c2b");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//taxa
	static final boolean doTaxa = true;

	private void doImport(ICdmDataSource cdmDestination){

		//make Source
	    KewExcelTaxonImportConfigurator config= KewExcelTaxonImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
		config.setCheck(check);
//		config.setDoTaxa(doTaxa);
		config.setDbSchemaValidation(hbm2dll);
		config.setSourceReferenceTitle("WCVP2CDM-Caryophyllaceae.xlsx");

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


	public static URI caryophyllaceae(){
      String fileName = "WCVP2CDM-Caryophyllaceae.xlsx";
      URI uri = URI.create("file:////BGBM-PESIHPC/Caryophyllales/" +  fileName);
      return uri;
	}

	public static void main(String[] args) {
		KewCaryophyllaceaeActivator me = new KewCaryophyllaceaeActivator();
		me.doImport(cdmDestination);
		System.exit(0);
	}
}