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
	private static String testServer = "160.45.63.175";
	
	private static String[] integrationDatabases = new String[]{/**/"cdm_integration_cichorieae",
		"cdm_integration_cyprus", "cdm_integration_diptera",  "cdm_integration_flora_malesiana",
		"cdm_integration_palmae"};
	
	private static String[] testDatabases = new String[]{/**/"caryo", "cdm_campanulaceae", 
		"cdm_campanulaceae_082014","cdm_col","cdm_corvidae","cdm_cyprus","cdm_demo1","cdm_demo2",
		"cdm_demo3", "cdm_edit_algaterra","cdm_edit_cichorieae","cdm_edit_flora_central_africa",
		"cdm_edit_flora_malesiana","cdm_edit_globis","cdm_edit_palmae", 
		"cdm_ipni_Caryophyllaceae","cdm_mt_standardliste",
		"cdm_pesi_euromed",  "cdm_pesi_fauna_europaea",
		"cdm_rl_mammalia","cdm_test_eckhard","cdm_test_euromed","cdm_test_gabi",
		"cdm_test_norbert","cdm_test_sabine","cdm_vibrant_index"};
	
	private static String[] testDatabasesInnoDb = new String[]{/**/"cdm_bgbm_edit_usergroup",
		"cdm_edit_ildis","cdm_flora_guianas","cdm_flore_gabon","cdm_mt_moose","cdm_pesi_erms",
		"cdm_proibiosphere_chenopodium_pilot",};
	private static String[] testDatabasesOthers = new String[]{"cdm_caryo_amaranthaceae",
		"cdm_caryo_caryophyllales","cdm_flora_malesiana_prospective","cdm_pesi_all","cdm_salvador",
		};
	
	private static String[] productionDatabases = new String[]{"cdm_integration_cichorieae",};

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
    			//result = updater.updateToCurrentVersion(dataSource, DefaultProgressMonitor.NewInstance());
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
