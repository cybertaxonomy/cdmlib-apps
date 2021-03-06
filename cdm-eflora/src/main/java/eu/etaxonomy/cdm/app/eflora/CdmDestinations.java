/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.eflora;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.ICdmDataSource;

public class CdmDestinations {
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(CdmDestinations.class);

	/**
	 * Intended to be used for imports
	 */
	public static ICdmDataSource import_a(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "import_a";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_mysql(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_mysql_fdac(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "fdac";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_mysql_test(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_fdac(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "fdac";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_ildis_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.201";
		String cdmDB = "cdm_edit_ildis";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_campanulaceae_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_campanulaceae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flora_malesiana_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_flora_malesiana";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flora_malesiana_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_flora_malesiana";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_nepenthes_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_caryo_nepenthaceae";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_flora_malesiana_prospective_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_flora_malesiana_prospective";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_flora_guianas_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_flora_guianas";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flora_guianas_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_flora_guianas";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flore_gabon_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_flore_gabon";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flore_gabon_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_flore_gabon";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_flora_central_africa_local(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "fdac";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flora_central_africa_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_flora_central_africa";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_flora_central_africa_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_flora_central_africa";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_portal_test_localhost(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_portal_test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_portal_test_localhost2(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_portal_test2";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_postgres_CdmTest(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "CdmTest";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}



	public static ICdmDataSource NULL(){
		return null;
	}


	public static ICdmDataSource localH2(){
		return CdmDataSource.NewH2EmbeddedInstance("cdm", "sa", "");
	}

	public static ICdmDataSource localH2(String database, String username, String filePath){
		return CdmDataSource.NewH2EmbeddedInstance(database, username, "", filePath);
	}

	public static ICdmDataSource localH2Salvador(){
		return CdmDataSource.NewH2EmbeddedInstance("salvador", "sa", "");
	}

	public static ICdmDataSource localH2Diptera(){
		return CdmDataSource.NewH2EmbeddedInstance("diptera", "sa", "");
	}


	public static ICdmDataSource localH2Cichorieae(){
		return CdmDataSource.NewH2EmbeddedInstance("cichorieae", "sa", "");
	}

	public static ICdmDataSource localH2Palmae(){
		return CdmDataSource.NewH2EmbeddedInstance("palmae", "sa", "");
	}

	public static ICdmDataSource localH2EuroMed(){
		return CdmDataSource.NewH2EmbeddedInstance("euroMed", "sa", "");
	}

	public static ICdmDataSource localH2Erms(){
		return CdmDataSource.NewH2EmbeddedInstance("erms", "sa", "");
	}

	public static ICdmDataSource localH2_viola(){
		return CdmDataSource.NewH2EmbeddedInstance("testViola", "sa", "");
	}

	public static ICdmDataSource localH2_LIAS(){
		return CdmDataSource.NewH2EmbeddedInstance("testLIAS", "sa", "");
	}

	public static ICdmDataSource localH2_Erythroneura(){
		return CdmDataSource.NewH2EmbeddedInstance("testErythroneura", "sa", "");
	}

	public static ICdmDataSource localH2_Cicad(){
		return CdmDataSource.NewH2EmbeddedInstance("testCicad", "sa", "");
	}

	public static ICdmDataSource localH2_ValRosandraFRIDAKey(){
		return CdmDataSource.NewH2EmbeddedInstance("testValRosandraFRIDAKey", "sa", "");
	}

	public static ICdmDataSource localH2_FreshwaterAquaticInsects(){
		return CdmDataSource.NewH2EmbeddedInstance("testFreshwaterAquaticInsects", "sa", "");
	}

	public static ICdmDataSource cdm_portal_test_pollux(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "192.168.2.11";
		String cdmDB = "cdm_portal_test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_algaterra_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.201";
		String cdmDB = "cdm_edit_algaterra";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_cichorieae_PG(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae_a";
		String cdmUserName = "edit";
		int port = 15432;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_cichorieae_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.201";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.151";
//		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_production_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_palmae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.151";
		String cdmDB = "cdm_production_palmae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_production_diptera(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.151";
		String cdmDB = "cdm_production_diptera";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource local_cdm_edit_cichorieae_a(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae_a";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource local_cdm_edit_cichorieae_b(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae_b";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_palmae_a(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "192.168.2.10";
		String cdmDB = "cdm_edit_palmae_a";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_diptera_preview_B(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_diptera_b";
		String cdmUserName = "edit";
		int port = 13306;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_cichorieae_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		int port = 13306;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_palmae_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_palmae";
		String cdmUserName = "edit";
		int port = 13306;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_salvador(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "192.168.2.10";
		String cdmDB = "cdm_edit_salvador";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_import_salvador() {
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "192.168.2.10";
		String cdmDB = "cdm_import_salvador";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_salvador_production() {
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "192.168.2.10";
		String cdmDB = "salvador_cdm";
		String cdmUserName = "salvador";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


//	public static ICdmDataSource LAPTOP_HP(){
//		DatabaseTypeEnum dbType = DatabaseTypeEnum.SqlServer2005;
//		String cdmServer = "LAPTOPHP";
//		String cdmDB = "cdmTest";
//		String cdmUserName = "edit";
//		return makeDestination(cdmServer, cdmDB, -1, cdmUserName, null);
//	}




	/**
	 * initializes source
	 * TODO only supports MySQL and PostgreSQL
	 *
	 * @param dbType
	 * @param cdmServer
	 * @param cdmDB
	 * @param port
	 * @param cdmUserName
	 * @param pwd
	 * @return
	 */
	private static ICdmDataSource makeDestination(DatabaseTypeEnum dbType, String cdmServer, String cdmDB, int port, String cdmUserName, String pwd ){
		//establish connection
		pwd = AccountStore.readOrStorePassword(cdmServer, cdmDB, cdmUserName, pwd);
		ICdmDataSource destination;
		if(dbType.equals(DatabaseTypeEnum.MySQL)){
			destination = CdmDataSource.NewMySqlInstance(cdmServer, cdmDB, port, cdmUserName, pwd);
		} else if(dbType.equals(DatabaseTypeEnum.PostgreSQL)){
			destination = CdmDataSource.NewPostgreSQLInstance(cdmServer, cdmDB, port, cdmUserName, pwd);
		} else {
			//TODO others
			throw new RuntimeException("Unsupported DatabaseType");
		}
		return destination;

	}


	/**
	 * Accepts a string array and tries to find a method returning an ICdmDataSource with
	 * the name of the given first string in the array
	 *
	 * @param args
	 * @return
	 */
	public static ICdmDataSource chooseDestination(String[] args) {
		if(args == null) {
            return null;
        }

		if(args.length != 1) {
            return null;
        }

		String possibleDestination = args[0];

		Method[] methods = CdmDestinations.class.getMethods();

		for (Method method : methods){
			if(method.getName().equals(possibleDestination)){
				try {
					return (ICdmDataSource) method.invoke(null, null);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}

