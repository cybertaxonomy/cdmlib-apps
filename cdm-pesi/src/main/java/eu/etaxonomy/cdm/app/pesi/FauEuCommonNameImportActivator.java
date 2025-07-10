/**
* Copyright (C) 2025 EDIT
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
import eu.etaxonomy.cdm.app.common.PesiSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.faueu.FaunaEuropaeaCommonNameImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author muellera
 * @since 02.07.2025
 */
public class FauEuCommonNameImportActivator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

    static final Source ifSource = PesiSources.SQL_EX_FAU_EU_COMMON_NAMES();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2025_final();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi2025_final2();

    //not needed if addSources = false
    static final String sourceRefTitle = "Fauna Europaea Common Name Import.";
    public static final UUID sourceRefUuid = PesiTransformer.uuidSourceRefFaunaEuropaeaCommonNames;

    private static boolean registerAuditing = false;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(Source source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + ") ...";
        System.out.println("Start"+importFrom + " ("  + LocalDateTime.now() +")");

        FaunaEuropaeaCommonNameImportConfigurator config = FaunaEuropaeaCommonNameImportConfigurator.NewInstance(source, destination);

        config.setSourceReference(getSourceRef());
        config.setSourceRefUuid(sourceRefUuid);

        config.setDbSchemaValidation(hbm2dll);

        config.setCheck(check);
//        config.setRecordsPerTransaction(partitionSize);

        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<FaunaEuropaeaCommonNameImportConfigurator> myImport = new CdmDefaultImport<>();
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
        FauEuCommonNameImportActivator myImport = new FauEuCommonNameImportActivator();
        myImport.doImport(ifSource, cdmDB, DbSchemaValidation.VALIDATE);
        System.exit(0);
    }
}
