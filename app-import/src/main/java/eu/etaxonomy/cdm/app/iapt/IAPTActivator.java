/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.iapt;

import java.io.File;
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.iapt.IAPTImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;


/**
 * @author a.kohlbecker
 * @date Jul 26, 2016
 *
 */
public class IAPTActivator {
    private static final Logger logger = Logger.getLogger(IAPTActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_cuba_production();

    static boolean invers = true;

    static boolean include = !invers;

    //classification
    static final UUID classificationUuid = UUID.fromString("8c51efb4-3d67-4bea-8f87-4bc1cba1310d");
    private static final String classificationName = "IAPT";
    static final String sourceReferenceTitle = "IAPT Import";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final boolean doTaxa = false;


    private void doImport(ICdmDataSource cdmDestination){

        URI source = iapt();  //just any

        Reference secRef = ReferenceFactory.newDatabase();
        secRef.setTitle("IAPT");

        //make Source
        IAPTImportConfigurator config= IAPTImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSourceReferenceTitle(sourceReferenceTitle);
        config.setSecReference(secRef);

        CdmDefaultImport<IAPTImportConfigurator> myImport = new CdmDefaultImport<>();

        doSingleSource(iapt(), config, myImport);

        System.exit(0);

    }

    /**
     * @param source
     * @param config
     * @param myImport
     */
    private void doSingleSource(URI source, IAPTImportConfigurator config, CdmDefaultImport<IAPTImportConfigurator> myImport) {
        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);
        config.setSourceReference(getSourceReference(fileName));
        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");
    }

    private final Reference inRef = ReferenceFactory.newGeneric();
    private Reference getSourceReference(String string) {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitleCache(string, true);
        result.setInReference(inRef);
        inRef.setTitleCache(sourceReferenceTitle, true);
        return result;
    }



    public static URI iapt() {
        File f = new File("~/data/Projekte/Algea Name Registry/registry/sources/IAPT/Registration_DB_from_BGBM17-cleaned.xls");
        return f.toURI();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        IAPTActivator me = new IAPTActivator();
        me.doImport(cdmDestination);
    }

}
