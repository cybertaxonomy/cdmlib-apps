/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import java.util.Calendar;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter.ORDER;
import eu.etaxonomy.cdm.io.cdm2cdm.Cdm2CdmImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ITaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitionerConcurrent;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmActivator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

    static final ICdmDataSource fauEuSource = CdmDestinations.test_cdm_pesi_fauna_europaea();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_pesi_leer();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2019_final();

    static final boolean doTaxa = true;
    static final boolean doDescriptions = false;


    static final String sourceRefTitle = "Fauna Europaea PESI import.";
    public static final UUID sourceRefUuid = PesiTransformer.uuidSourceRefFaunaEuropaea;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final int partitionSize = 5000;

    static final boolean doConcurrent = false;
    //auditing
    static final boolean registerAuditing = false;

// ***************** ALL ************************************************//

//    >50 records
//    UUID uuidTaxonNodeFilter = UUID.fromString("0e8bc793-f434-47c4-ba82-650c3bbd83bf");
    //>17000 records
//    UUID uuidTaxonNodeFilter = UUID.fromString("7ee4983b-78a3-44c5-9af2-beb0494b5fc8");
    //complete
    UUID uuidTaxonNodeFilter = UUID.fromString("feaa3025-a4a9-499a-b62f-15b3b96e5c55");


    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start"+importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setConcurrent(doConcurrent);
        config.setDoTaxa(doTaxa);
        config.setDoDescriptions(doDescriptions);
        config.setSourceReference(getSourceRef());

        IProgressMonitor monitor = config.getProgressMonitor();

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

        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
    }

    private Reference getSourceRef() {
        Reference ref = ReferenceFactory.newDatabase();
        ref.setTitle(sourceRefTitle);
        ref.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(Calendar.getInstance()));
        ref.setUuid(sourceRefUuid);
        return ref;
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        FauEu2CdmActivator myImport = new FauEu2CdmActivator();
        myImport.doImport(fauEuSource, cdmDB, DbSchemaValidation.VALIDATE);
        System.exit(0);
    }
}
