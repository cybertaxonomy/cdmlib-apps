/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import java.time.LocalDateTime;
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
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 07.04.2025
 */
public class IF2CdmActivator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

    static final ICdmDataSource ifSource = CdmDestinations.cdm_pesi_indexFungorum();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi_leer();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2025_final();

    static final boolean doTaxa = true;
    static final boolean doDescriptions = true;

    static final boolean addSources = false;
    //not needed if addSources = false
    static final String sourceRefTitle = "Index Fungorum PESI import.";
    public static final UUID sourceRefUuid = PesiTransformer.uuidSourceRefIndexFungorum;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final int partitionSize = 5000;

    //auditing
    static final boolean registerAuditing = false;

// ***************** ALL ************************************************//

//    >50 records
//    UUID uuidTaxonNodeFilter = UUID.fromString("");
    //complete
    UUID uuidTaxonNodeFilter = UUID.fromString("4181cd98-9c54-4d2a-9a2f-a2c28d027643");


    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + ") ...";
        System.out.println("Start"+importFrom + " ("  + LocalDateTime.now() +")");

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(doTaxa);
        config.setDoDescriptions(doDescriptions);
        config.setAddSources(addSources);
        config.setSourceReference(getSourceRef());

        IProgressMonitor monitor = config.getProgressMonitor();

        config.setDbSchemaValidation(hbm2dll);
        config.getTaxonNodeFilter().orSubtree(uuidTaxonNodeFilter);
        config.getTaxonNodeFilter().setOrder(ORDER.TREEINDEX);

        config.setCheck(check);
//        config.setRecordsPerTransaction(partitionSize);

        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom + " (" + LocalDateTime.now() +")" );
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
        IF2CdmActivator myImport = new IF2CdmActivator();
        myImport.doImport(ifSource, cdmDB, DbSchemaValidation.VALIDATE);
        System.exit(0);
    }
}