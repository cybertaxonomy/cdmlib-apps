/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;

/**
 * This is to create a new CDM database instance
 * @author muellera
 * @since 04.09.2025
 */
public class CreateNewInstance {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    private void invoke() {
        System.out.println("Start");
        DbSchemaValidation schema = DbSchemaValidation.CREATE;
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String server  = "160.45.63.171";
        String database = "cdm_production_piperaceae";
        CdmDataSource dataSource = getDatasource(dbType, server, database);
        CdmApplicationController appCtr = CdmApplicationController.NewInstance(dataSource, schema);
        appCtr.close();
        System.out.println("Ready");
    }

    private CdmDataSource getDatasource(DatabaseTypeEnum dbType, String server, String database) {
        String username = "edit";
        String serverSql = "130.133.70.26";
//        server = "160.45.63.175";

        if (dbType == DatabaseTypeEnum.MySQL){
            return CdmDataSource.NewMySqlInstance(server, database, username, AccountStore.readOrStorePassword(server, database, username, null));
        }else if (dbType == DatabaseTypeEnum.H2){
            //H2
            String path = "C:\\Users\\muellera\\.cdmLibrary\\writableResources\\h2\\LocalH2_" + database;
//            String path = "C:\\Users\\muellera\\.cdmLibrary\\writableResources\\h2\\LocalH2_xyz";
            username = "sa";
            CdmDataSource dataSource = CdmDataSource.NewH2EmbeddedInstance("cdmTest", username, "", path);
            return dataSource;
        }else if (dbType == DatabaseTypeEnum.SqlServer2005){
            server = serverSql;
            username = "cdmupdater";
            CdmDataSource dataSource = CdmDataSource.NewSqlServer2005Instance(server, database, 1433, username, AccountStore.readOrStorePassword(server, database, username, null));
            return dataSource;
        }else if (dbType == DatabaseTypeEnum.PostgreSQL){
            server = serverSql;
            username = "postgres";
            CdmDataSource dataSource = CdmDataSource.NewPostgreSQLInstance(server, database, 5432, username,  AccountStore.readOrStorePassword(server, database, username, null));
            return dataSource;
        }else{
            throw new IllegalArgumentException("dbType not supported:" + dbType);
        }
    }

    public static void  main(String[] args) {
        CreateNewInstance cc = new CreateNewInstance();
        cc.invoke();
        System.exit(0);
    }
}
