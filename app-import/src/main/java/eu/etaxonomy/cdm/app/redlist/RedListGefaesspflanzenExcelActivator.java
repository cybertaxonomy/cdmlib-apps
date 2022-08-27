package eu.etaxonomy.cdm.app.redlist;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.redlist.gefaesspflanzen.excel.RedListGefaesspflanzenExcelImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 13.06.2019
 */
public class RedListGefaesspflanzenExcelActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI mySource = redListPlantaeChecklist();

//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_redlist_gefaesspflanzen();

	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("8a78ac1f-b2de-4e9e-bb14-319da0b4a790");


	//classification
	public static final UUID classificationUuid = UUID.fromString("6f734299-fd8e-4cf2-864e-78ce1d53880b");

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//taxa
	static final boolean doTaxa = true;

	private void doImport(ICdmDataSource cdmDestination){

		//make Source
	    URI source = mySource;

		RedListGefaesspflanzenExcelImportConfigurator config= RedListGefaesspflanzenExcelImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
//		config.setDoTaxa(doTaxa);
		config.setCheck(check);
		config.setDbSchemaValidation(hbm2dll);
		config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);

		CdmDefaultImport<RedListGefaesspflanzenExcelImportConfigurator> myImport = new CdmDefaultImport<>();

		System.out.println("Start import from ("+ source.toString() + ") ...");
		config.setSourceReference(getSourceReference());
		myImport.invoke(config);
		System.out.println("End import from ("+ source.toString() + ")...");

		System.exit(0);
	}

    //bogotaChecklist
    public static URI redListPlantaeChecklist() {
        return URI.create("file:////BGBM-PESIHPC/RoteListen/gefaesspflanzen/skript519_checkliste_AM.xlsx");
    }

	private Reference getSourceReference() {
		Reference result = ReferenceFactory.newBook();
		result.setTitle("Liste der Gefäßpflanzen Deutschlands. Florensynopse und Synonyme. - BfN-Skripten 519");
		result.setDoi(DOI.fromString("10.19217/skr519"));
		result.setPages("1-286");
		result.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(2018));
		Team author = Team.NewInstance();
		author.addTeamMember(Person.NewInstance(null, "Buttler", "K.P.", null));
        author.addTeamMember(Person.NewInstance(null, "May", "R.", null));
        author.addTeamMember(Person.NewInstance(null, "Metzing", "D.", null));
		result.setAuthorship(author);
		return result;
	}

	public static void main(String[] args) {
		RedListGefaesspflanzenExcelActivator me = new RedListGefaesspflanzenExcelActivator();
		me.doImport(cdmDestination);
	}
}
