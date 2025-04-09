/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.common;

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
	private static Logger logger = LogManager.getLogger();

	public static ICdmDataSource cdm_test_local_mysql(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_test";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_pesi_faunaEu(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_pesi_fauna_europaea";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_pesi_leer(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_leer";
        String cdmUserName = "edit";
        int port = 3306;
        return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_local_pesi_leer(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_test_leer";
        String cdmUserName = "edit";
        int port = 3306;
        return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_local_pesi_leer2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_test_leer2";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_pesi_indexFungorum(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_pesi_indexfungorum";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}
    public static ICdmDataSource cdm_pesi_indexFungorum2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_indexfungorum2";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }
	public static ICdmDataSource cdm_local_erms(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_pesi_erms";
		String cdmUserName = "edit";
		return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_local_erms2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_erms2";
        String cdmUserName = "edit";
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_local_mysql_euromed(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_euromed";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_pesi2019_final(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_2019final";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }
    public static ICdmDataSource cdm_pesi2019_final_test(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_2019final_test";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_pesi2025_final(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_2025_final";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_local_mysql_test(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource NULL(){
		return null;
	}

	public static ICdmDataSource localH2(){
		return CdmDataSource.NewH2EmbeddedInstance("cdm", "sa", "");
	}

	public static ICdmDataSource localH2EuroMed(){
		return CdmDataSource.NewH2EmbeddedInstance("euroMed", "sa", "");
	}

	public static ICdmDataSource localH2Erms(){
		return CdmDataSource.NewH2EmbeddedInstance("erms", "sa", "");
	}

	public static ICdmDataSource test_cdm_pesi_euroMed(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_pesi_euromed";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource test_cdm_pesi_fauna_europaea(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_pesi_fauna_europaea";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource test_cdm_pesi_erms(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_pesi_erms";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


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
	public static ICdmDataSource makeDestination(DatabaseTypeEnum dbType, String cdmServer, String cdmDB, int port, String cdmUserName, String pwd ){
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
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}