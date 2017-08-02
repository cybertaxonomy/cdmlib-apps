/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.salvador;

import java.io.IOException;
import java.net.URI;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.database.update.CdmUpdater;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.salvador.SalvadorSpecimenImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @date 08.07.2017
 *
 */
public class SalvadorSpecimenActivator {
    private static final Logger logger = Logger.getLogger(SalvadorSpecimenActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_salvador_preview();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_salvador_production();

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = salvadorSpecimen();

        //make Source
        SalvadorSpecimenImportConfigurator config;
        try {
            config = SalvadorSpecimenImportConfigurator.NewInstance(source, cdmDestination);
        } catch (IOException e) {
            String message = "IO Exception when configuring import: " + e.getMessage();
            logger.warn(message);
            return;
        }
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setTransactionLineCount(100);

        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<SalvadorSpecimenImportConfigurator> myImport
                = new CdmDefaultImport<>();

        ImportResult result = myImport.invoke(config);
        String report = result.createReport().toString();
        System.out.println(report);
        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //SalvadorSpecimen
    public static URI salvadorSpecimen() {
        return URI.create("file:////BGBM-PESIHPC/Salvador/Specimen3.csv");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Specimen3.csv");
        return result;
    }

    public void doUpdate(ICdmDataSource cdmDestination){
        CdmUpdater updater = CdmUpdater.NewInstance();
        updater.updateToCurrentVersion(cdmDestination, null);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SalvadorSpecimenActivator me = new SalvadorSpecimenActivator();
        me.doImport(cdmDestination);
//        me.doUpdate(cdmDestination);
        System.exit(0);
    }
}
