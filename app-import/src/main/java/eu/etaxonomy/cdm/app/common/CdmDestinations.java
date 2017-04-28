/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.common;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.ICdmDataSource;

public class CdmDestinations {
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(CdmDestinations.class);

	public static ICdmDataSource cdm_redlist_animalia_localhost(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "cdm_bfn_imports_animalia";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_redlist_animalia_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rl_animals";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_redlist_plant_localhost(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "cdm_bfn_imports_plants";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_redlist_lumbricidae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_rl_lumbricidae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_testDB_localhost(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "testDB";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_useSummary(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "palmae_2011_07_17";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_mysql(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "rl2020_gefaesspflanzen";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_test_local_mysql_moose(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "moose";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_mysql_standardliste(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "standardliste";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_test_local_mysql_dwca(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "dwca";
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
		String cdmUserName = "edit";     //root on pesiimport2
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_euromed(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "euroMed";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_local_euromed2(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "euroMed2";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}
	public static ICdmDataSource cdm_test_local_euromed3(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "euroMed3";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_test_col(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_col";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_col2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_col2";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_col_local(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "col_test";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_col2_local(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "col_test2";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }


	public static ICdmDataSource cdm_local_EDITImport(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "EDITImport";
		String cdmUserName = "edit";     //root on pesiimport2
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_euroMed(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_test_euromed";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_corvidae_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_corvidae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_ildis_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_ildis";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

//
//	public static ICdmDataSource cdm_ildis_production(){
//		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
//		String cdmServer = "160.45.63.171";
//		String cdmDB = "cdm_edit_ildis";
//		String cdmUserName = "edit";
//		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
//	}


	public static ICdmDataSource cdm_redlist_moose_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_mt_moose";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_redlist_moose_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_rl_moose";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_redlist_standardlist_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_mt_standardliste";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_redlist_germanSL_preview(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_rl_german_sl";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_germanSL_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rl_german_sl";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }


	public static ICdmDataSource cdm_cyprus_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_cyprus_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_cuba_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_cuba";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_greece_checklist_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_hellenica";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_mexico_rubiaceae_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rubiaceae_mexico";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_bogota_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_bogota";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_cyprus_production_tunnel(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		int port = 13306;
		String cdmDB = "cdm_production_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_cyprus_dev_tunnel(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		int port = 13306;
		String cdmDB = "cdm_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_campanulaceae_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_campanulaceae";
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

	public static ICdmDataSource cdm_local_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}



	public static ICdmDataSource cdm_local_palmae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_caryo";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_caryo(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_local_caryophyllales";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_globis_dev(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_globis";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_local_edaphobase(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "edaphobase";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_globis_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_globis";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_globis(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_globis";
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

	public static ICdmDataSource localH2Edapho(){
        return CdmDataSource.NewH2EmbeddedInstance("cdmEdapho", "sa", "");
    }

	public static ICdmDataSource localH2Salvador(){
		return CdmDataSource.NewH2EmbeddedInstance("salvador", "sa", "");
	}

	public static ICdmDataSource localH2Armeria(){
		return CdmDataSource.NewH2EmbeddedInstance("armeria", "sa", "");
	}

	public static ICdmDataSource localH2Standardliste(){
		return CdmDataSource.NewH2EmbeddedInstance("standardliste", "sa", "");
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


	public static ICdmDataSource cdm_algaterra_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_algaterra";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_algaterra_production(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_algaterra";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_salvador_preview(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_salvador";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_salvador_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_salvador";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_edit_cichorieae_local_PG(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae_a";
		String cdmUserName = "edit";
		int port = 15432;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_cichorieae_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
//		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_production_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_palmae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_palmae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_flora_deutschland(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_rl_standardliste";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_caryophyllales(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_caryophyllales";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_caryophyllales_nepenthaceae(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_caryo_nepenthaceae";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_caryophyllales_nepenthaceae(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_caryo_nepenthaceae";
        String cdmUserName = "root";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_production_redlist_gefaesspflanzen(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_rl_plantae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_production_edaphobase(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_edaphobase";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }


	public static ICdmDataSource cdm_production_diptera(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_diptera";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_cichorieae_preview(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		int port = 13306;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_edit_cichorieae_preview_direct(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		int port = 3306;
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_piB(String database){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_" + database + "_pilot";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_piB(String database){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "piB_" + database;
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

   public static ICdmDataSource proibiosphere_chenopodium_local() {
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "localhost";
        String cdmDB = "cdm_production_proibiosphere_chenopodium_pilot";
        String cdmUserName = "root";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

   public static ICdmDataSource proibiosphere_ants_local() {
       DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
       String cdmServer = "localhost";
       String cdmDB = "cdm_production_proibiosphere_ants_pilot";
       String cdmUserName = "root";
       return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
   }

   public static ICdmDataSource proibiosphere_eupolybothrus_local() {
       DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
       String cdmServer = "localhost";
       String cdmDB = "cdm_production_proibiosphere_eupolybothrus_pilot";
       String cdmUserName = "root";
       return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
   }

   public static ICdmDataSource proibiosphere_spiders_local() {
       DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
       String cdmServer = "localhost";
       String cdmDB = "cdm_production_proibiosphere_spiders_pilot";
       String cdmUserName = "root";
       return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
   }

   public static ICdmDataSource cdm_pesi_fauna_europaea(){
       DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
       String cdmServer = "160.45.63.175";
       String cdmDB = "cdm_pesi_fauna_europaea";
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
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}

}

