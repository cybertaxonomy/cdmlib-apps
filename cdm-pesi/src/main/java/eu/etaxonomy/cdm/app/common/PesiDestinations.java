/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author e.-m.lee
 * @since 16.02.2010
 */
public class PesiDestinations {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PesiDestinations.class);

	public static Source pesi_test_local_CDM_EM2PESI(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "pesiimport3";
		String strDB = "CDM_EM2PESI";
		int port = 1433;
		String userName =  "pesi2019";// "pesiexport3";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source pesi_test_local_CDM_EM2PESI_2(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "pesiimport3";
		String strDB = "CDM_EM2PESI_2";
		int port = 1433;
		String userName = "pesi2019";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source pesi_test_local_CDM_FE2PESI(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "pesiimport3"; //130.133.70.48
		String strDB = "CDM_FE2PESI";
		int port = 1433;
		String userName = "pesi2019"; //"pesiExportFaunaEu";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

    public static Source pesi_test_local_CDM_FE2PESI_2(){
        String dbms = Source.SQL_SERVER_2008;
        String strServer = "pesiimport3"; //130.133.70.48
        String strDB = "CDM_FE2PESI";
        int port = 1433;
        String userName = "pesi2019";
        return makeSource(dbms, strServer, strDB, port, userName, null);
    }

	public static Source pesi_test_local_CDM_IF2PESI(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "pesiimport3";
		String strDB = "CDM_IF2PESI";
		int port = 1433;
		String userName = "pesi2019";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source pesi_test_local_CDM_IF2PESI_2(){
        String dbms = Source.SQL_SERVER_2008;
        String strServer = "pesiimport3";
        String strDB = "CDM_IF2PESI_2";
        int port = 1433;
        String userName = "pesi2019";
        return makeSource(dbms, strServer, strDB, port, userName, null);
    }

	public static Source pesi_test_local_CDM_ERMS2PESI(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "pesiimport3";
		String strDB = "CDM_ERMS2PESI";
		int port = 1433;
		String userName = "pesi2019";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

    public static Source pesi_test_local_CDM_ERMS2PESI_2(){
        String dbms = Source.SQL_SERVER_2008;
        String strServer = "pesiimport3";
        String strDB = "CDM_ERMS2PESI_2";
        int port = 1433;
        String userName = "pesi2019";
        return makeSource(dbms, strServer, strDB, port, userName, null);
    }

	public static Source pesi_test_bgbm42_CDM_DWH_FaEu(){
		//	CDM - PESI
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "BGBM42";
		String strDB = "CDM_DWH_FaEu";
		int port = 1433;
		String userName = "WebUser";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source pesi_test_bgbm42_PESI_V11(){
		//	CDM - PESI
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "BGBM42";
		String strDB = "PESI_v11";
		int port = 1433;
		String userName = "WebUser";
		return makeSource(dbms, strServer, strDB, port, userName, null);
	}


	/**
	 * Initializes the source.
	 * @param dbms
	 * @param strServer
	 * @param strDB
	 * @param port
	 * @param userName
	 * @param pwd
	 * @return
	 */
	private static Source makeSource(String dbms, String strServer, String strDB,
	        int port, String userName, String pwd ){
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
