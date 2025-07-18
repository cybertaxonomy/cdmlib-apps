/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
public class PesiSources {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static Source faunEu_pesi3(){
		//	Fauna Europaea auf pesiimport3
		String dbms = Source.SQL_SERVER;
        String strServer = "pesiimport3";
        String strDB = "FaunEu";
		int port = 1433;
		String userName = "pesiExportFaunaEu";
		return  ImportUtils.makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source mfn_faunEu_pesi3(){
		//	Fauna Europaea auf pesiimport3
		String dbms = Source.SQL_SERVER;
        String strServer = "pesiimport3";
        String strDB = "MfN_FaunaEuropaea";
		int port = 1433;
		String userName = "pesiExportFaunaEu";
		return  ImportUtils.makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source faunEu(){
		//	Fauna Europaea
		String dbms = Source.SQL_SERVER;
       	String strServer = "BGBM42";               // "192.168.1.36";
        String strDB = "FaunEu";
		int port = 1433;
		String userName = "WebUser";
		return  ImportUtils.makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source PESI_ERMS(){
		// Pesi-ERMS
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "ERMS";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

    public static Source SQL_EX_PESI2019_ERMS_2019(){
        // Pesi-ERMS
        String dbms = Source.SQL_SERVER;
        String strServer = "BGBM-PESISQL\\SQLEXPRESS";
        String strDB = "erms2019_12_16";
        int port = 1434;
        String userName = "pesiimport";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

    public static Source SQL_EX_PESI2025_ERMS_2025(){
        // Pesi-ERMS
        String dbms = Source.SQL_SERVER_TRUSTED;
        String strServer = "BGBM-PESISQL\\SQLEXPRESS";
        String strDB = "erms_2025_04_03";
        int port = 1434;
        String userName = "pesiimport";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }




    public static Source SQL_EX_FAU_EU_COMMON_NAMES(){
        //  Index Fungorum
        String dbms = Source.SQL_SERVER_TRUSTED;
        String strServer = "BGBM-PESISQL\\SQLEXPRESS";
        String strDB = "FauEuCommonNames_2015_11_17";
        int port = 1434;
        String userName = "pesiimport";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }


	public static Source PESI3_IF(){
		//	Index Fungorum
		String dbms = Source.SQL_SERVER;
		String strServer = "Pesiimport3";
		String strDB = "IF_2014_06";
		int port = 1433;
		String userName = "pesi2019";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source PESI_IF(){
		//  Pesi-IF
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "IF";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	/**
	 * Initializes the source.
	 * @param dbms
	 * @param strServer
	 * @param strDB
	 * @param port
	 * @param userName
	 * @param pwd
	 * @return the source
	 */
	private static Source makeSource(String dbms, String strServer, String strDB, int port, String userName, String pwd ){
		//establish connection
		Source source = null;
		source = new Source(dbms, strServer, strDB);
		source.setPort(port);

		pwd = AccountStore.readOrStorePassword(dbms, strServer, userName, pwd);
		source.setUserAndPwd(userName, pwd);
		// write pwd to account store
		return source;
	}


}
