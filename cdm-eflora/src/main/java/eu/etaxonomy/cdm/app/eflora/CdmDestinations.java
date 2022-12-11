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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.ICdmDataSource;

public class CdmDestinations {

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();
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

	public static ICdmDataSource NULL(){
		return null;
	}


	public static ICdmDataSource localH2(){
		return CdmDataSource.NewH2EmbeddedInstance("cdm", "sa", "");
	}

	public static ICdmDataSource localH2(String database, String username, String filePath){
		return CdmDataSource.NewH2EmbeddedInstance(database, username, "", filePath);
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

