/**
 *
 */
package eu.etaxonomy.cdm.database.update;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.taxon.Classification;

/**
 * @author a.mueller
 */
public class BgbmInstancesUpdater {

    private static final Logger logger = LogManager.getLogger();

	private static String integrationServer = "160.45.63.201";
	private static String productionServer = "160.45.63.171";
//	private static String testServer = "160.45.63.175";
	private static String testServer = "edit-test.bgbm.fu-berlin.de";  //preliminary
    private static String mexicoServer = "132.248.13.83";

	private static String[] integrationDatabases = new String[]{
	    "cdm_integration_cichorieae",
		"cdm_integration_cyprus",
		"cdm_integration_diptera",
	    "cdm_integration_flora_malesiana",
		"cdm_integration_palmae",
		"cdm_integration_reference"
	};

	private static String[] testDatabases = new String[]{
	    "cdm_bgbm_edit_usergroup",
	    "cdm_caryo_nepenthes",
	    "cdm_caryo_spp","cdm_causcasus_workshop",
	    "cdm_corvidae", "cdm_cyprus","cdm_edaphobase_test",
		"cdm_edit_algaterra",
		"cdm_edit_cichorieae",
		"cdm_edit_flora_central_africa", "cdm_edit_flora_malesiana",
		"cdm_edit_globis", "cdm_edit_palmae",
		"cdm_flora_cuba",
		"cdm_flora_guianas",
		"cdm_flore_gabon",
		"cdm_greece_bupleurum","cdm_greece_bupleurum_01","cdm_greece_bupleurum_04",
		"cdm_mt_moose",
		"cdm_mt_standardliste",
//	    "cdm_pesi_all",
		"cdm_pesi_erms", "cdm_pesi_euromed", "cdm_pesi_fauna_europaea",
		"cdm_phycobank",
	    "cdm_production_additivity_ontology",
		"cdm_rem_conf_am",
		"cdm_rl_animalia","cdm_rl_german_sl","cdm_rl_mammalia", "cdm_rl_plantae",
		"cdm_salvador","cdm_test1","cdm_test2",
        "cdm_test_euromed",
        "cdm_vibrant_index"
	};

	private static String[] testDatabasesOthers = new String[]{"cdm_caryo_amaranthaceae",
		"cdm_caryo_caryophyllales","cdm_flora_malesiana_prospective","cdm_pesi_all",
	};

	private static String[] phycobankDatabase = new String[]{
	        "cdm_production_phycobank"
	};

	private static String[] mexicoDatabase = new String[]{
            "cdm_flora_mexico"
    };

	private static String[] productionDatabases = new String[]{
	    "cdm_production_col",
	    "cdm_production_algaterra",
	    "cdm_production_algaterranew",
	    "cdm_production_asteraceae",
	    "cdm_production_bromeliaceae",
	    "cdm_production_campanulaceae",
	    "cdm_production_caryo_amaranthaceae",
	    "cdm_production_caryo_nepenthaceae",
	    "cdm_production_caryophyllales",
	    "cdm_production_caryophyllales_spp",
	    "cdm_production_casearia",
	    "cdm_production_caucasus",
	    "cdm_production_cichorieae",
	    "cdm_production_corvidae",
	    "cdm_production_cyprus",
	    "cdm_production_diptera",
	    "cdm_production_edaphobase",
	    "cdm_production_euromed",
	    "cdm_production_euromed_caucasus",
	    "cdm_production_flora_central_africa",
	    "cdm_production_flora_cuba",
	    "cdm_production_flora_greece",
	    "cdm_production_flora_guianas",
	    "cdm_production_flora_malesiana",
	    "cdm_production_flora_malesiana_clean",
	    "cdm_production_flora_malesiana_prospective",
	    "cdm_production_flora_uzbekistan",
	    "cdm_production_flore_gabon",
	    "cdm_production_globis",
	    "cdm_production_greece_bupleurum",
	    "cdm_production_lichenes",
	    "cdm_production_myristicaceae",
	    "cdm_production_oxalis",
	    "cdm_production_palmae",
	    "cdm_production_piB_ants_pilot",
	    "cdm_production_piB_campylopus_pilot",
	    "cdm_production_piB_chenopodium_pilot",
	    "cdm_production_piB_eupolybothrus_pilot",
	    "cdm_production_piB_lactarius_pilot",
	    "cdm_production_piB_nephrolepis_pilot",
	    "cdm_production_piB_spiders_pilot",
	    "cdm_production_rl_animalia",
	    "cdm_production_rl_animalia_final",
	    "cdm_production_rl_armeria_demo",
	    "cdm_production_rl_german_sl",
	    "cdm_production_rl_lumbricidae",
	    "cdm_production_rl_mammalia",
	    "cdm_production_rl_moose",
	    "cdm_production_rl_plantae",
	    "cdm_production_rl_standardliste",
	    "cdm_production_rubiaceae_mexico",
	    "cdm_production_salvador",
	    "cdm_production_vibrant_index"
	};

    static BgbmServer bgbmServer = BgbmServer.TEST;

	static String username = "edit";

    private static void updateToCurrentVersion() {
        DbSchemaValidation schema = DbSchemaValidation.VALIDATE;
    	String server = bgbmServer.server;
    	for (String database : bgbmServer.databases){
    	    logger.warn("Update: " + database + " ... ");
    		ICdmDataSource dataSource = CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));
    		SchemaUpdateResult result = new SchemaUpdateResult();
            try {
    			CdmUpdater updater = new CdmUpdater();
    			System.out.println(database);
                result = updater.updateToCurrentVersion(dataSource, DefaultProgressMonitor.NewInstance());
    			System.out.println(result.createReport());
    			CdmApplicationController appCtr = CdmIoApplicationController.NewInstance(dataSource,schema);
    			System.out.println(appCtr.getClassificationService().count(Classification.class));
    		} catch (Exception e) {
    			result.addException(e, e.getMessage());
    			e.printStackTrace();
    		}
    		if(!result.isSuccess() ){
    			logger.warn("Problem");
    			System.exit(1);
    		}

    		logger.warn("Update: " + database + " ... DONE ");
    	}
    }

    private static void singleUpdateStep(boolean startApp) {
        DbSchemaValidation schema = DbSchemaValidation.VALIDATE;
        String server = bgbmServer.server;
        for (String database : bgbmServer.databases){
            logger.warn("Update: " + database + " ... ");
            ICdmDataSource dataSource = CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));
            SchemaUpdateResult result = new SchemaUpdateResult();
            try {
                System.out.println(database);
                //define step here !!!
                SchemaUpdaterStepBase step = IndexRenamer.NewStringInstance(null, "TaxonName",
                        "taxonNameBaseNameCacheIndex", "taxonNameNameCacheIndex", "nameCache", 255);
                SchemaUpdaterStepBase step2 = IndexRenamer.NewStringInstance(null, "TaxonName",
                        "taxonNameBaseTitleCacheIndex", "taxonNameTitleCacheIndex", "titleCache", 333);
                dataSource.startTransaction();
                step.invoke(dataSource, DefaultProgressMonitor.NewInstance(), CaseType.caseTypeOfDatasource(dataSource), result);
                step2.invoke(dataSource, DefaultProgressMonitor.NewInstance(), CaseType.caseTypeOfDatasource(dataSource), result);
                dataSource.commitTransaction();
                System.out.println(result.createReport());
                if (startApp){
                    CdmApplicationController appCtr = CdmIoApplicationController.NewInstance(dataSource,schema);
                    System.out.println(appCtr.getClassificationService().count(Classification.class));
                }
            } catch (Exception e) {
                result.addException(e, e.getMessage());
                e.printStackTrace();
            }
            if(!result.isSuccess() ){
                logger.warn("Problem");
                System.exit(1);
            }

            logger.warn("Update: " + database + " ... DONE ");
        }

    }

    public static void  main(String[] args) {
        updateToCurrentVersion();
//        singleUpdateStep(false);
        System.exit(0);
    }

    private enum BgbmServer{
		INTEGRATION (integrationServer, integrationDatabases),
		TEST(testServer, testDatabases),
		PRODUCTION(productionServer, productionDatabases),
        PHYCOBANK(productionServer, phycobankDatabase),
        MEXICO(mexicoServer, mexicoDatabase);
        private final String server;
		private final String[] databases;
		private BgbmServer(String server, String[] databases){
			this.server = server;
			this.databases = databases;
		}
	}
}
