// $Id$
/**
* Copyright (C) 2022 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.mexico;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.CdmImportSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.io.mexico.MexicoEfloraImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * See #9932.
 *
 * @author a.mueller
 * @date 29.01.2022
 */
public class MexicoEfloraActivator {

    private static final Logger logger = Logger.getLogger(MexicoEfloraActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    //static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("7cfa2122-043d-4e96-922c-356fc04d68a3");

    //classification
    static final UUID classificationUuid = UUID.fromString("28abf7da-e0ad-48ca-b26c-9f276dd90267");
    private static final String classificationName = "Mexico Eflora";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);
//    static final boolean doTaxa = false;
//    static final boolean doDeduplicate = true;

    protected void doImport(ICdmDataSource cdmDestination){

        Source source = CdmImportSources.MEXICO_EFLORA();

        //make Source
        MexicoEfloraImportConfigurator config= MexicoEfloraImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSecReference(getSecReference());

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<MexicoEfloraImportConfigurator> myImport = new CdmDefaultImport<>();

        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("CONABIO Eflora");
//        Person borhidi = Person.NewTitledInstance("Borhidi");
//        borhidi.setGivenName("Attila");
//        result.setAuthorship(borhidi);
        return result;
    }

    private Reference getSecReference() {
        Reference result = ReferenceFactory.newBook();
        result.setTitle("Mexico Eflora");
        //TODO
        result.setUuid(MexicoConabioTransformer.uuidReferenceEflora);
        return result;
    }

    public static void main(String[] args) {
        MexicoEfloraActivator me = new MexicoEfloraActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
