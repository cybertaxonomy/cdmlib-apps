/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.berlinModelImport;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.io.common.Source;

public class BerlinModelSources {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	public static Source euroMed_PESI3(){
		//	BerlinModel - Euro+Med
		String dbms = Source.SQL_SERVER;
		String strServer = "PESIIMPORT3";
		String strDB = "EM_2014_06";
		int port = 1433;
		String userName = "pesiexport";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source iopi(){
		//	BerlinModel - Euro+Med
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM17";
		String strDB = "IOPIBM";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source euroMed_Pub2(){
        //  BerlinModel - Euro+Med
        String dbms = Source.SQL_SERVER;
        String strServer = "bgbm-sql03";
        String strDB = "EuroPlusMed_Pub2";
        int port = 1433;
        String userName = "WebUser";  //was webUser  or pesiexport
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

	public static Source euroMed_BGBM42(){
		//	BerlinModel - Euro+Med
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "EuroPlusMed_00_Edit";
		int port = 1433;  //was 1247
		String userName = "Webuser";  //was webUser  or pesiexport
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source mcl(){
		//	BerlinModel - Euro+Med
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "MCL";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source Campanulaceae(){
		//	BerlinModel - Campanulaceae
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "Campanulaceae";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source ILDIS(){
		//	BerlinModel - EditWP6
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "ILDIS_EM_BM";
		int port = 1433;
		String userName = "webUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source EDIT_CICHORIEAE(){
		//	BerlinModel - EditWP6
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "EditWP6";
		int port = 1433;
		String userName = "webUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source EDIT_Taraxacum(){
		//	BerlinModel - EditWP6
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "Edit_Taraxacum";
		int port = 1247;
		String userName = "webUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source EDIT_Diptera(){
		//	BerlinModel - EDIT_Diptera
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM42";
		String strDB = "EDIT_Diptera";
		int port = 1247;
		String userName = "webUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source El_Salvador(){
		//	BerlinModel - El_Salvador
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM-SQL01";
		String strDB = "Salvador";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source AlgaTerra(){
		//	BerlinModel - AlgaTerra
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM-SQL01";
		String strDB = "Algaterra";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source MT_MOOSE(){
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM-SQL02";
		String strDB = "MTMoose";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source MT_Standardliste(){
		String dbms = Source.SQL_SERVER;
		String strServer = "BGBM-SQL02";
		String strDB = "MTStandardliste";
		int port = 1433;
		String userName = "WebUser";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	/**
	 * Initializes the source.
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