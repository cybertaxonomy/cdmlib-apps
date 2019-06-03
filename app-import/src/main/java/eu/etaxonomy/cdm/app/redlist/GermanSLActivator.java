/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.redlist;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.redlist.germanSL.GermanSLImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 25.11.2016
 *
 */
public class GermanSLActivator {
    private static final Logger logger = Logger.getLogger(GermanSLActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_germanSL_preview();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_germanSL_production();

    static String versionString = "version_1_3_4";

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("490de21f-db97-49f8-ae2c-48145f592ceb");

    //classification
    static final UUID classificationUuid = UUID.fromString("25754287-18d8-4352-9cd5-7f2cead4e53c");
    private static final String classificationName = "German SL";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

//    static final boolean doTaxa = false;
//    static final boolean doDeduplicate = true;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = germanSL();

        //make Source
        GermanSLImportConfigurator config= GermanSLImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSecReference(getSecReference());
        config.setSourceReference(getSourceReference());
        config.setSource(source);
        config.setVersionString(versionString);

        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);


        CdmDefaultImport<GermanSLImportConfigurator> myImport = new CdmDefaultImport<>();

        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //GermanSL Excel file
    public static URI germanSL() {
        return URI.create("file:////BGBM-PESIHPC/GermanSL/GermanSL.xlsx");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("GermanSL.xslx");
        Person florian = Person.NewTitledInstance("Jansen");
        florian.setGivenName("Florian");
        result.setAuthorship(florian);
        return result;
    }

    private Reference getSecReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("German Standard List");
//        result.setPlacePublished("Budapest");
//        result.setPublisher("Akadémiai Kiadó");
//        result.setPages("608 pp.");
//        result.setDatePublished(TimePeriodParser.parseString("2012"));
//        Person borhidi = Person.NewTitledInstance("Borhidi");
//        borhidi.setGivenName("Attila");
//        result.setAuthorship(borhidi);
//        result.setUuid(MexicoConabioTransformer.uuidReferenceBorhidi);
        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GermanSLActivator me = new GermanSLActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}

