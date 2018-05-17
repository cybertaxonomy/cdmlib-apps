/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.mexico;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.mexico.MexicoBorhidiImportConfigurator;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Activator for import of Borhidi Rubiaceae (Mexico)
 * @author a.mueller
 * @since 16.06.2016
 *
 */
public class MexicoBorhidiActivator {
    private static final Logger logger = Logger.getLogger(MexicoBorhidiActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_mexico_rubiaceae_production();

    static boolean invers = true;

//    boolean doRubiaceae = include;

//    static boolean include = !invers;


    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("d1f4ed29-9aae-4f6e-aa1e-4a3bf780e11d");

    //classification
    static final UUID classificationUuid = UUID.fromString("8ebb2076-d849-47e0-ad32-4fe08ca61cac");
    private static final String classificationName = "Rubiaceae Borhidi";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);
//    static final boolean doTaxa = false;
//    static final boolean doDeduplicate = true;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = borhidi();

        //make Source
        MexicoBorhidiImportConfigurator config= MexicoBorhidiImportConfigurator.NewInstance(source, cdmDestination);
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

        CdmDefaultImport<MexicoBorhidiImportConfigurator> myImport = new CdmDefaultImport<>();

        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //Borhidi
    public static URI borhidi() {
        return URI.create("file:////BGBM-PESIHPC/Mexico/Borhidi_2012.xlsx");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Borhidi 2012 accepted spp checked in TROPICOS.XLSX");
        Person borhidi = Person.NewTitledInstance("Borhidi");
        borhidi.setGivenName("Attila");
        result.setAuthorship(borhidi);
        return result;
    }

    private Reference getSecReference() {
        Reference result = ReferenceFactory.newBook();
        result.setTitle("Rubiáceas de México");
        result.setPlacePublished("Budapest");
        result.setPublisher("Akadémiai Kiadó");
        result.setPages("608 pp.");
        result.setDatePublished(TimePeriodParser.parseString("2012"));
        Person borhidi = Person.NewTitledInstance("Borhidi");
        borhidi.setGivenName("Attila");
        result.setAuthorship(borhidi);
        result.setUuid(MexicoConabioTransformer.uuidReferenceBorhidi);
        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        MexicoBorhidiActivator me = new MexicoBorhidiActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
