/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.pesi.fauEu2Cdm.FauEu2CdmImportConfigurator;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FauEu2CdmActivator.class);

    static final ICdmDataSource fauEuSource = CdmDestinations.cdm_pesi_fauna_europaea();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_erms();

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final int partitionSize = 5000;

// ***************** ALL ************************************************//

    //references
    static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;

    //taxa
    static final boolean doTaxa = true;


    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start"+importFrom);

        FauEu2CdmImportConfigurator config = FauEu2CdmImportConfigurator.NewInstance(source,  destination);

//        config.setDoTaxa(doTaxa);
        config.setDbSchemaValidation(hbm2dll);

        config.setCheck(check);
//        config.setRecordsPerTransaction(partitionSize);
//        config.setSourceRefUuid(PesiTransformer.uuidSourceRefErms);

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

    }
}
