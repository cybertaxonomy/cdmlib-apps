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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.ICdmDataSource;

public class CdmDestinations {

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    public static ICdmDataSource cdm_pesi2019_final(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_2019final";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_redlist_animalia(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "cdm_bfn_imports_animalia";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_redlist_animalia_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rl_animalia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_additivity_ontology(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_additivity_ontology";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_redlist_animalia_production_final(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rl_animalia_final";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_redlist_plant(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "cdm_bfn_imports_plants";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_testDB(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "localhost";
		String cdmDB = "testDB";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_local_test_mysql(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "test";
        String cdmUserName = "root";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_cdmtest_mysql(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_test";
        String cdmUserName = "root";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_terms(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_terms";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_greece_bupleurum(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_greece_bupleurum";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_uzbekistan(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_uzbekistan";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_redlist_gefaesspflanzen(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "rl2020_gefaesspflanzen";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_local_test_mysql_moose(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "moose";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_test_mysql_standardliste(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "standardliste";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}


	public static ICdmDataSource cdm_local_test_mysql_dwca(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "dwca";
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

    public static ICdmDataSource cdm_local_greece(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_greece";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_casearia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_casearia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_mysql_pesi_euromed(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_pesi_euromed";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_euromed(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_euromed";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_euromed_caucasus(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_euromed_caucasus";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_mysql_tmpTest(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "tmpTest";
        String cdmUserName = "edit";
        int port = 3306;
        return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_euromed2(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "euroMed2";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_georgia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_georgia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_armenia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_armenia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_azerbaijan(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_azerbaijan";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_test_euromed3(){
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

    public static ICdmDataSource cdm_local_col(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "col_test";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_col2(){
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

    public static ICdmDataSource cdm_production_medchecklist(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_medchecklist";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_euromed(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_euromed";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_euromed_caucasus(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_euromed_caucasus";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_georgia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_georgia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_armenia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_armenia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_azerbaijan(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_azerbaijan";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_corvidae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_corvidae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

   public static ICdmDataSource cdm_test_redlist_mammalia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_rl_mammalia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_redlist_moose(){
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

	public static ICdmDataSource cdm_test_redlist_standardlist(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_mt_standardliste";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_redlist_germanSL(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_rl_german_sl";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_germanSL(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rl_german_sl";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource local_cyprus(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "localhost";
        String cdmDB = "cdm_cyprus";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_cyprus(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_test_bupleurum(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_test_bupleurum";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_production_cyprus(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_production_casearia(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_casearia";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_uzbekistan(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_uzbekistan";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_phycobank_production(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_phycobank";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_flora_cuba(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_cuba";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_greece_checklist(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_greece";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_greece_bupleurum(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_greece_bupleurum";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_lichenes(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_lichenes";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_mexico_rubiaceae(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_rubiaceae_mexico";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_bogota(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_bogota";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_production_tunnel_cyprus(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		int port = 13306;
		String cdmDB = "cdm_production_cyprus";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_campanulaceae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_campanulaceae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_production_cdmterms(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_cdmterms";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_portal_test(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_portal_test";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_portal_test2(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_portal_test2";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_local_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_local_cyprus(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_cyprus";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_mexico(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_mexico";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }
    public static ICdmDataSource cdm_local_mexico2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_mexico2";
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

    public static ICdmDataSource cdm_local_cuba(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_flora_cuba";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_local_caryo(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_caryo_spp";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_local_algaterranew(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "cdm_production_algaterranew";
		String cdmUserName = "root";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_test_globis(){
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

    public static ICdmDataSource cdm_postgres_edaphobase(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
        String cdmServer = "130.133.70.26";
        String cdmDB = "cdm_edaphobase";
        String cdmUserName = "edaphobase";
        int port = 5433;
        return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_postgres_edaphobase(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_edaphobase";
        String cdmUserName = "edaphobase";
        int port = 5432;
        return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_postgres__ssh_edaphobase(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.PostgreSQL;
        String cdmServer = "localhost";
        String cdmDB = "cdm_edaphobase";
        String cdmUserName = "edaphobase";
        int port = 13306;
        return makeDestination(dbType, cdmServer, cdmDB, port, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_caryo_spp(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_caryophyllales_spp";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_local_caryo_spp1(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_local_caryophyllales_spp1";
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
    public static ICdmDataSource cdm_mexico_flora(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "132.248.13.83";
        String cdmDB = "cdm_flora_mexico";
        String cdmUserName = "bgbm-developer";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
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


    public static ICdmDataSource cdm_test_test1(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_test1";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}
    public static ICdmDataSource cdm_test_test2(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_test2";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }
    public static ICdmDataSource cdm_test_test3(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_test3";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_test_phycobank(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_phycobank";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_algaterra(){
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

    public static ICdmDataSource cdm_test_salvador(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_salvador";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_salvador(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_salvador";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.175";
		String cdmDB = "cdm_edit_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_int_flora_malesiana(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.201";
        String cdmDB = "cdm_integration_flora_malesiana";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_test_caryo_spp(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.175";
        String cdmDB = "cdm_caryo_spp";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

	public static ICdmDataSource cdm_production_cichorieae(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_cichorieae";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

    public static ICdmDataSource cdm_production_cuba(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_flora_cuba";
        String cdmUserName = "edit";
        return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
    }

    public static ICdmDataSource cdm_production_buxales(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_buxales";
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

	public static ICdmDataSource cdm_production_caryophyllales_genus(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "160.45.63.171";
		String cdmDB = "cdm_production_caryophyllales";
		String cdmUserName = "edit";
		return makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

	public static ICdmDataSource cdm_production_caryophyllales_spp(){
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "160.45.63.171";
        String cdmDB = "cdm_production_caryophyllales_spp";
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

    public static ICdmDataSource cdm_test_pesi_fauna_europaea(){
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

