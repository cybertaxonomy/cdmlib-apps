/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.util.Calendar;
import java.util.UUID;

import org.apache.log4j.Logger;

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
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class BupleurumExportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BupleurumExportActivator.class);

    static final ICdmDataSource greeceSource = CdmDestinations.cdm_production_greece_checklist();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();

    static final String sourceRefTitle = "Flora of Greece";
    static final UUID sourceRefUuid = UUID.fromString("f88e33e5-1f6a-463e-b6fd-220d5e93d810");

    static final DbSchemaValidation schemaValidation = DbSchemaValidation.CREATE;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final int partitionSize = 5000;

    static final boolean doTaxa = true;
    static final boolean doDescriptions = false;

    static final boolean doConcurrent = false;
    //auditing
    static final boolean registerAuditing = true;

// ***************** ALL ************************************************//

    UUID uuidBupleurumTaxonNodeFilter = UUID.fromString("51e768cf-321b-4108-8bee-46143996b033");

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
        config.getTaxonNodeFilter().orSubtree(uuidBupleurumTaxonNodeFilter);
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
        BupleurumExportActivator myImport = new BupleurumExportActivator();
        myImport.doImport(greeceSource, cdmDB, schemaValidation);
        System.exit(0);
    }
}
