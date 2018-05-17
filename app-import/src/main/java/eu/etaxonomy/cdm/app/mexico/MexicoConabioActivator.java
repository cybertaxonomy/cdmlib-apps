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

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioImportConfigurator;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 16.06.2016
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

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());
        config.setSecReference(getSecReference());

        CdmDefaultImport<MexicoConabioImportConfigurator> myImport = new CdmDefaultImport<>();

        myImport.invoke(config);

        if (true){
            FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController());
            myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
        }

        System.out.println("End import from ("+ source.toString() + ")...");
    }


    //Conabio Rubiaceae
    public static URI conabio_rubiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Mexico/CONABIO-Rubiaceae.xlsx");
    }

    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitleCache("CONABIO database", true);
        VerbatimTimePeriod tp = TimePeriodParser.parseStringVerbatim("2016");
        tp.setStartMonth(5);
        result.setDatePublished(tp);
        Institution inst = Institution.NewNamedInstance("CONABIO");
        result.setInstitution(inst);
        return result;
    }

    private Reference getSecReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Rubiáceas de México");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2016"));
        Person author = Person.NewInstance();
        author.setGivenName("Helga");
        author.setFamilyName("Ochoterena Booth");
        result.setAuthorship(author);
        result.setUuid(MexicoConabioTransformer.uuidReferenceConabio);
        return result;
    }

    private FeatureTree makeFeatureNodes(ICdmRepository app){

        FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
        result.setTitleCache("Mexico Rubiaceae Feature Tree", true);
        FeatureNode root = result.getRoot();
        FeatureNode newNode;

        Feature distribution = Feature.DISTRIBUTION();
        Representation rep = Representation.NewInstance("Distribución", "Distribución", null, Language.SPANISH_CASTILIAN());
        distribution.addRepresentation(rep);
        app.getTermService().saveOrUpdate(distribution);
        newNode = FeatureNode.NewInstance(distribution);
        root.addChild(newNode);

        Feature commonName = Feature.COMMON_NAME();
        rep = Representation.NewInstance("Nombres comunes", "Nombres comunes", null, Language.SPANISH_CASTILIAN());
        commonName.addRepresentation(rep);
        app.getTermService().saveOrUpdate(commonName);
        newNode = FeatureNode.NewInstance(commonName);
        root.addChild(newNode);

        Feature notes = Feature.NOTES();
        rep = Representation.NewInstance("Notas", "Notas", null, Language.SPANISH_CASTILIAN());
        notes.addRepresentation(rep);
        app.getTermService().saveOrUpdate(notes);
        newNode = FeatureNode.NewInstance(notes);
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
