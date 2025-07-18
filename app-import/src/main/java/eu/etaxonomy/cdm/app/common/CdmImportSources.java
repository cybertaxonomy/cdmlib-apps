/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.berlinModelImport.SourceBase;
import eu.etaxonomy.cdm.app.tcs.TcsSources;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.mueller
 * @since 21.04.2010
 */
public class CdmImportSources extends SourceBase{

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

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

	public static Source GLOBIS_MDB_20140113_PESIIMPORT_SQLSERVER(){
		String dbms = Source.SQL_SERVER;
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

    public static Source EDAPHOBASE6(){
        String dbms = Source.POSTGRESQL9;
        String strServer = "130.133.70.26";  //BGBM-PESISQL
        String strDB = "edaphobase6";
        int port = 5432;
        String userName = "postgres";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

    public static Source EDAPHOBASE8(){
        String dbms = Source.POSTGRESQL9;  //TODO 10
        String strServer = "130.133.70.26";  //BGBM-PESISQL
        String strDB = "edaphobase8";
        int port = 5433;
        String userName = "edaphobase";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

    public static Source MEXICO_EFLORA(){
        String dbms = Source.MYSQL;
        String strServer = "localhost";
        String strDB = "mexico_eflora";
        int port = 3306;
        String userName = "edit";
        return  makeSource(dbms, strServer, strDB, port, userName, null);
    }

}
