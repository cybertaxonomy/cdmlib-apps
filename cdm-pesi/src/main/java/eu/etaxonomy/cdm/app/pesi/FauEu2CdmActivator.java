/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter.ORDER;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ITaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitionerConcurrent;
import eu.etaxonomy.cdm.io.pesi.fauEu2Cdm.FauEu2CdmImportConfigurator;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FauEu2CdmActivator.class);

    static final ICdmDataSource fauEuSource = CdmDestinations.cdm_pesi_fauna_europaea();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_pesi_leer2();

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final int partitionSize = 5000;

    static final boolean doConcurrent = false;

// ***************** ALL ************************************************//

//    >50 records
    UUID uuidTaxonNodeFilter = UUID.fromString("0e8bc793-f434-47c4-ba82-650c3bbd83bf");
    //>17000 records
//    UUID uuidTaxonNodeFilter = UUID.fromString("7ee4983b-78a3-44c5-9af2-beb0494b5fc8");


    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start"+importFrom);

        FauEu2CdmImportConfigurator config = FauEu2CdmImportConfigurator.NewInstance(source,  destination);
        config.setConcurrent(doConcurrent);

        IProgressMonitor monitor = config.getProgressMonitor();

//        config.setDoTaxa(doTaxa);
        config.setDbSchemaValidation(hbm2dll);
        config.getTaxonNodeFilter().orSubtree(uuidTaxonNodeFilter);
        config.getTaxonNodeFilter().setOrder(ORDER.TREEINDEX);
        if (doConcurrent){
            ITaxonNodeOutStreamPartitioner partitioner = TaxonNodeOutStreamPartitionerConcurrent
                    .NewInstance(config.getSource(), config.getTaxonNodeFilter(),
                            8, monitor, 1, TaxonNodeOutStreamPartitioner.fullPropertyPaths);
            config.setPartitioner(partitioner);
        }

        config.setCheck(check);
//        config.setRecordsPerTransaction(partitionSize);

        // invoke import
        CdmDefaultImport<FauEu2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        FauEu2CdmActivator myImport = new FauEu2CdmActivator();
        myImport.doImport(fauEuSource, cdmDB, DbSchemaValidation.VALIDATE);
        System.exit(0);
    }
}
