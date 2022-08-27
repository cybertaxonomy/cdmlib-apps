/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.api.service.UpdateResult;
import eu.etaxonomy.cdm.api.service.description.AggregationMode;
import eu.etaxonomy.cdm.api.service.description.DistributionAggregation;
import eu.etaxonomy.cdm.api.service.description.DistributionAggregationConfiguration;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @since 18.10.2019
 */
public class EuroMedDistributionAggregationActivator {

    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_mysql_pesi_euromed();
//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_euroMed();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

   //Actinolema
//    static final UUID subtreeUuid = UUID.fromString("108745e9-c740-4c88-a2bb-2304a797c827");
    //Apiaceae
//    static final UUID subtreeUuid = UUID.fromString("236f19ad-0eba-4cdd-a839-41348c0a7b38");
    //Asteranae
//  static final UUID subtreeUuid = UUID.fromString("7255db07-bbe0-43f6-921b-5c68c65e343f");

    //Magnoliopsida
    static final UUID subtreeUuid = UUID.fromString("342959e1-eb51-4992-bc60-80dd650727c9");

    private void doImport(ICdmDataSource cdmDestination){

        logger.info("start");
        ICdmRepository app = CdmApplicationController.NewInstance(cdmDestination, dbSchemaValidation);

        DistributionAggregation distributionAggregation = new DistributionAggregation();

        Set<UUID> subtreeUuids = new HashSet<>();
        subtreeUuids.add(subtreeUuid);
        TaxonNodeFilter filter = TaxonNodeFilter.NewInstance(null, subtreeUuids, null, null, null,
                null, Rank.uuidGenus);
        DistributionAggregationConfiguration aggregationConfig =
                DistributionAggregationConfiguration.NewInstance(
                AggregationMode.byToParent(),
                null,
                filter,
                DefaultProgressMonitor.NewInstance());
        UpdateResult result = distributionAggregation.invoke(aggregationConfig, app);
        System.out.println("Status: " + result.getStatus());
        System.out.println("Exceptions: " + result.getExceptions());
        for (Exception ex: result.getExceptions()){
            ex.printStackTrace();
        }
        System.out.println("Size: " + result.getUpdatedCdmIds().size());
    }

    public static void main(String[] args) {
        EuroMedDistributionAggregationActivator me = new EuroMedDistributionAggregationActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}