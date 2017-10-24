/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.berlinModelImport.SourceBase;
import eu.etaxonomy.cdm.app.tcs.TcsSources;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.mueller
 * @date 21.04.2010
 *
 */
public class CdmImportSources extends SourceBase{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(CdmImportSources.class);

	public static Source ROTE_LISTE_GEFAESSPFLANZEN_DB(){
		String dbms = Source.MYSQL;
		String strServer = "localhost";
		String strDB = "RL_Gefaesspflanzen_source";
		int port = 3306;
		String userName = "root";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source ROTE_LISTE_DB(){
		String dbms = Source.ORACLE;
		String strServer = "xxx";
		String strDB = "dbName";
		int port = 1433;
		String userName = "adam";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}


	public static Source GLOBIS(){
		String dbms = Source.SQL_SERVER_2005;
		String strServer = "LENOVO-T61";
		String strDB = "globis";
		int port = 0001;
		String userName = "user";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source GLOBIS_ODBC(){
		String dbms = Source.ODDBC;
		String strServer = "LENOVO-T61";
		String strDB = "globis";
		int port = 1433;
		String userName = "sa";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source GLOBIS_MDB(){
		String dbms = Source.ACCESS;
		String strServer = null;
		String strDB = "C:\\localCopy\\Data\\globis\\globis.mdb";
		int port = -1;
		String userName = "";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source GLOBIS_MDB_20120928(){
		String dbms = Source.ACCESS;
		String strServer = null;
		String strDB = "C:\\localCopy\\Data\\globis\\globis.20120928.mdb";
		int port = -1;
		String userName = "";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source GLOBIS_MDB_20140113(){
		String dbms = Source.ACCESS;
		String strServer = null;
		String strDB = "C:\\localCopy\\Data\\globis\\globis_20140113.mdb";
		int port = -1;
		String userName = "";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	//Problem MS Access ODBC Driver does not work on PESIIMPORT 3
	@Deprecated
	public static Source GLOBIS_MDB_20140113_PESIIMPORT(){
		String dbms = Source.ACCESS;
		String strServer = null;
		String strDB = "\\\\PESIIMPORT3\\globis\\globis_20140113.mdb";
		int port = -1;
		String userName = "";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static Source GLOBIS_MDB_20140113_PESIIMPORT_SQLSERVER(){
		String dbms = Source.SQL_SERVER_2008;
		String strServer = "PESIIMPORT3";
		String strDB = "globis_orig_20140113";
		int port = 1433;
		String userName = "globisImport";
		return  makeSource(dbms, strServer, strDB, port, userName, null);
	}

	public static URI SYNTHESYS_SPECIMEN(){
		//		tcsXmlTest.xml
		URL url = new TcsSources().getClass().getResource("/specimen/SynthesysSpecimenExample.xls");
		String sourceUrl = url.toString();
		URI uri = URI.create(sourceUrl);
		return uri;
	}

    public static Source EDAPHOBASE(){
        String dbms = Source.POSTGRESQL9;
        String strServer = "130.133.70.26";  //BGBM-PESISQL
        String strDB = "edaphobase";
        int port = 5432;
        String userName = "postgres";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

    public static Source EDAPHOBASE2(){
        String dbms = Source.POSTGRESQL9;
        String strServer = "130.133.70.26";  //BGBM-PESISQL
        String strDB = "edaphobase2";
        int port = 5432;
        String userName = "postgres";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

    public static Source EDAPHOBASE5(){
        String dbms = Source.POSTGRESQL9;
        String strServer = "130.133.70.26";  //BGBM-PESISQL
        String strDB = "edaphobase5";
        int port = 5432;
        String userName = "postgres";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }




}
