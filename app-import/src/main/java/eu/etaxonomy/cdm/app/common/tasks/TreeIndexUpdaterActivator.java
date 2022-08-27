/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common.tasks;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.database.update.CaseType;
import eu.etaxonomy.cdm.database.update.SchemaUpdateResult;
import eu.etaxonomy.cdm.database.update.TreeIndexUpdater;

/**
 * @author a.mueller
 * @since 28.02.2020
 */
public class TreeIndexUpdaterActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
     static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

    //  static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2019_final();

    private void doInvoke(ICdmDataSource destination){

        boolean includeAudTable = false;
        TreeIndexUpdater updater = TreeIndexUpdater.NewInstance(
                new ArrayList<>(), "Update Treeindex", "TaxonNode", "classification_id", includeAudTable);

        IProgressMonitor monitor = DefaultProgressMonitor.NewInstance();
        CaseType caseType = CaseType.CamelCase;
        SchemaUpdateResult result = new SchemaUpdateResult();
        try {
            updater.invoke(destination, monitor, caseType, result);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

        System.out.println("Start updating caches for "+ destination.getDatabase() + "...");
        TreeIndexUpdaterActivator me = new TreeIndexUpdaterActivator();
        me.doInvoke(destination);

    }
}
