// $Id$
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
import eu.etaxonomy.cdm.io.mexico.MexicoConabioImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
public class MexicoConabioActivator {
    private static final Logger logger = Logger.getLogger(MexicoConabioActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_mexico_rubiaceae_production();

    static final boolean doTaxa = true;
    static final boolean doDistributions = true;
    static final boolean doCommonNames = true;

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("d1f4ed29-9aae-4f6e-aa1e-4a3bf780e11d");

    //classification
    static final UUID classificationUuid = UUID.fromString("61968b43-e881-4043-b5c2-ba192e8f72dc");
    private static final String classificationName = "Rubiaceae Conabio";

    static final String sourceReferenceTitle = "OchaXXX";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = conabio_rubiaceae();

        //make Source
        MexicoConabioImportConfigurator config= MexicoConabioImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDoTaxa(doTaxa);
        config.setDoDistributions(doDistributions);
        config.setDoCommonNames(doCommonNames);
        config.setDbSchemaValidation(hbm2dll);
        config.setSourceReferenceTitle(sourceReferenceTitle);

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference(sourceReferenceTitle));

        CdmDefaultImport<MexicoConabioImportConfigurator> myImport = new CdmDefaultImport<MexicoConabioImportConfigurator>();

        myImport.invoke(config);

        if (true){
            FeatureTree tree = makeFeatureNodes();
            myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
        }

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //Conabio Rubiaceae
    public static URI conabio_rubiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Mexico/CONABIO-Rubiaceae.xlsx");
    }

    private Reference getSourceReference(String string) {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitleCache(string, true);
//        result.setTitle("Rubiáceas de México");
//        result.setPlacePublished("Budapest");
//        result.setPublisher("Akadémiai Kiadó");
//        result.setPages("512 pp.");
        result.setDatePublished(TimePeriodParser.parseString("2016"));
        Person author = Person.NewTitledInstance(string);
        author.setFirstname("Helga");
        result.setAuthorship(author);
//        result.setUuid(MexicoConabioTransformer.uuidReferenceBorhidi);
        return result;
    }

    private FeatureTree makeFeatureNodes(){

        FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
        result.setTitleCache("Mexico Rubiaceae Feature Tree", true);
        FeatureNode root = result.getRoot();
        FeatureNode newNode;

        newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
        root.addChild(newNode);

        newNode = FeatureNode.NewInstance(Feature.COMMON_NAME());
        root.addChild(newNode);

        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        MexicoBorhidiActivator borhidi = new MexicoBorhidiActivator();
        borhidi.doImport(cdmDestination);

        MexicoConabioActivator conabio = new MexicoConabioActivator();
        conabio.doImport(cdmDestination);
        System.exit(0);
    }
}
