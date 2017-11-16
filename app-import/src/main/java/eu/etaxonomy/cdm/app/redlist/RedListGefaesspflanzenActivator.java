package eu.etaxonomy.cdm.app.redlist;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.CdmImportSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.redlist.gefaesspflanzen.RedListGefaesspflanzenImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

public class RedListGefaesspflanzenActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source mySource = CdmImportSources.ROTE_LISTE_GEFAESSPFLANZEN_DB();

//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_redlist_gefaesspflanzen();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("8a78ac1f-b2de-4e9e-bb14-319da0b4a790");

	public static final String sourceReference = "Rote Listen - Gefäßpflanzen";


	//classification
	public static final UUID classificationUuid = UUID.fromString("6f734299-fd8e-4cf2-864e-78ce1d53880b");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//taxa
	static final boolean doTaxa = true;

	private void doImport(ICdmDataSource cdmDestination){

		//make Source
		Source source = mySource;

		RedListGefaesspflanzenImportConfigurator config= RedListGefaesspflanzenImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
//		config.setDoTaxa(doTaxa);
		config.setCheck(check);
		config.setDbSchemaValidation(hbm2dll);

		CdmDefaultImport<RedListGefaesspflanzenImportConfigurator> myImport = new CdmDefaultImport<RedListGefaesspflanzenImportConfigurator>();

		System.out.println("Start import from ("+ source.toString() + ") ...");
		config.setSourceReference(getSourceReference(sourceReference));
		myImport.invoke(config);
		System.out.println("End import from ("+ source.toString() + ")...");
	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}


	public static void main(String[] args) {
		RedListGefaesspflanzenActivator me = new RedListGefaesspflanzenActivator();
		me.doImport(cdmDestination);
	}
}
