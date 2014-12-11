/**
 * 
 */
package eu.etaxonomy.cdm.database.update;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.common.AccountStore;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.taxon.Classification;

/**
 * @author a.mueller
 *
 */
public class BgbmInstancesUpdater {
	private static final Logger logger = Logger.getLogger(BgbmInstancesUpdater.class);
	
	private static String integrationServer = "160.45.63.201";
	private static String productionServer = "160.45.63.171";
//	private static String testServer = "160.45.63.175";
	private static String testServer = "edit-test.bgbm.fu-berlin.de";  //preliminary
	
	private static String[] integrationDatabases = new String[]{/**/"cdm_integration_cichorieae",
		"cdm_integration_cyprus", "cdm_integration_diptera",  "cdm_integration_flora_malesiana",
		"cdm_integration_palmae"};
	
	private static String[] testDatabases = new String[]{"cdm_bgbm_edit_usergroup","cdm_campanulaceae", 
		"cdm_campanulaceae_082014","cdm_caryo", "cdm_col","cdm_corvidae","cdm_cyprus","cdm_demo1","cdm_demo2",
		"cdm_demo3", "cdm_edit_algaterra","cdm_edit_cichorieae","cdm_edit_ildis","cdm_edit_flora_central_africa",
		"cdm_flora_guianas","cdm_flore_gabon","cdm_edit_flora_malesiana","cdm_edit_globis","cdm_edit_palmae", 
		"cdm_ipni_Caryophyllaceae","cdm_mt_moose","cdm_mt_standardliste",
		"cdm_pesi_euromed", "cdm_pesi_erms","cdm_pesi_fauna_europaea",
		"cdm_proibiosphere_chenopodium_pilot","cdm_rl_mammalia","cdm_test_eckhard","cdm_test_euromed","cdm_test_gabi",
		"cdm_test_norbert","cdm_test_sabine","cdm_vibrant_index"};
	

	private static String[] testDatabasesOthers = new String[]{"cdm_caryo_amaranthaceae",
		"cdm_caryo_caryophyllales","cdm_flora_malesiana_prospective","cdm_pesi_all","cdm_salvador",
		};
	
	private static String[] productionDatabases = new String[]{
		"cdm_col", "cdm_production_acantholimon","cdm_production_algaterra",
		"cdm_production_amaranthaceae","cdm_production_cactaceae",
		"cdm_production_campanulaceae","cdm_production_caryophyllales",
		"cdm_production_chenopodiaceae","cdm_production_cichorieae",
		"cdm_production_corvidae","cdm_production_cyprus",
		"cdm_production_dianthus","cdm_production_diptera",
		"cdm_production_flora_central_africa","cdm_production_flora_malesiana",
		"cdm_production_flora_malesiana_prospective","cdm_production_flore_gabon",
		"cdm_production_globis","cdm_production_nyctaginaceae",
		"cdm_production_palmae","cdm_production_piB_campylopus_pilot",
		"cdm_production_piB_eupolybothrus_pilot","cdm_production_piB_lactarius_pilot",
		"cdm_production_piB_loranthaceae","cdm_production_piB_nephrolepis_pilot",
		"cdm_production_piB_spiders_pilot","cdm_production_polygonaceae",
		"cdm_production_proibiosphere_ants_pilot","cdm_production_proibiosphere_chenopodium_pilot",
		"cdm_production_rl_armeria_demo","cdm_production_rl_lumbricidae",
		"cdm_production_rl_odonata_demo","cdm_production_rl_standardliste",
		"cdm_production_tamaricaceae","cdm_production_vibrant_index"};

	static BgbmServer bgbmServer = BgbmServer.TEST;
	
	
	
	static String username = "edit";
	
	
	
	/**
	 * @param args
	 */
	public static void  main(String[] args) {
		DbSchemaValidation schema = DbSchemaValidation.VALIDATE;
    	String server = bgbmServer.server;
    	for (String database : bgbmServer.databases){
    		boolean result = true;
    		logger.warn("Update: " + database + " ... ");
    		ICdmDataSource dataSource = CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));
    		try {
    			CdmUpdater updater = new CdmUpdater();
    			result = updater.updateToCurrentVersion(dataSource, DefaultProgressMonitor.NewInstance());
    			CdmApplicationController appCtr = CdmIoApplicationController.NewInstance(dataSource,schema);
    			System.out.println(appCtr.getClassificationService().count(Classification.class));
    		} catch (Exception e) {
    			result = false;
    			e.printStackTrace();
    		}	
    		if(!result ){
    			logger.warn("Problem");
    			break;
    		}
    		
    		logger.warn("Update: " + database + " ... DONE ");
    	}
    	
    	
	}
	
	private enum BgbmServer{
		INTEGRATION (integrationServer, integrationDatabases),
		TEST(testServer,testDatabases),
		PRODUCTION(productionServer,productionDatabases);
		private String server;
		private String[] databases;
		private BgbmServer(String server, String[] databases){
			this.server = server;
			this.databases = databases;
		}
		
		
		
	}
}
