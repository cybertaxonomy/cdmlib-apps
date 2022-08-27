/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.mexico;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioImportConfigurator;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
public class MexicoConabioActivator {

    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_mexico();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_mexico_rubiaceae();

    static final boolean doTaxa = true;
    static final boolean doDistributions = true;
    static final boolean doCommonNames = true;

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("d1f4ed29-9aae-4f6e-aa1e-4a3bf780e11d");

    //classification
    static final UUID classificationUuid = UUID.fromString("4ae5cc80-d06b-4102-a154-a5bc525e61d6");
    private static final String classificationName = "IBUNAM";

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

        if (hbm2dll == DbSchemaValidation.CREATE){
            TermTree<Feature> tree = makeFeatureNodes(myImport.getCdmAppController());
            myImport.getCdmAppController().getTermTreeService().saveOrUpdate(tree);
        }

        System.out.println("End import from ("+ source.toString() + ")...");
    }


    //Conabio Rubiaceae
    public static URI conabio_rubiaceae() {
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Orchidaceae.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Hydrocharitaceae.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Ebenaceae.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Annonaceae.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Amaranthaceae.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Mexico/Rubiaceae.xlsx");
        return URI.create("file:////BGBM-PESIHPC/Mexico/TaxonomyInclRefType.xlsx");
    }

    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("CONABIO database");
        VerbatimTimePeriod tp = TimePeriodParser.parseStringVerbatim("2021");
        tp.setStartMonth(7);
        result.setDatePublished(tp);
        Institution inst = Institution.NewNamedInstance("CONABIO");
        result.setInstitution(inst);
        return result;
    }

    private Reference getSecReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Flora de México");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2021+"));
//        Person author = Person.NewInstance();
//        author.setGivenName("Helga");
//        author.setFamilyName("Ochoterena Booth");
//        result.setAuthorship(author);
        result.setUuid(MexicoConabioTransformer.uuidReferenceConabio2);
        return result;
    }

    private TermTree<Feature> makeFeatureNodes(ICdmRepository app){

        TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
        result.setTitleCache("Mexico Rubiaceae Feature Tree", true);
        TermNode<Feature> root = result.getRoot();

        Feature distribution = Feature.DISTRIBUTION();
        Representation rep = Representation.NewInstance("Distribución", "Distribución", null, Language.SPANISH_CASTILIAN());
        distribution.addRepresentation(rep);
        app.getTermService().saveOrUpdate(distribution);
        root.addChild(distribution);

        Feature commonName = Feature.COMMON_NAME();
        rep = Representation.NewInstance("Nombres comunes", "Nombres comunes", null, Language.SPANISH_CASTILIAN());
        commonName.addRepresentation(rep);
        app.getTermService().saveOrUpdate(commonName);
        root.addChild(commonName);

        Feature notes = Feature.NOTES();
        rep = Representation.NewInstance("Notas", "Notas", null, Language.SPANISH_CASTILIAN());
        notes.addRepresentation(rep);
        app.getTermService().saveOrUpdate(notes);
        root.addChild(notes);

        return result;
    }

    public static void main(String[] args) {
//        MexicoBorhidiActivator borhidi = new MexicoBorhidiActivator();
//        borhidi.doImport(cdmDestination);

        MexicoConabioActivator conabio = new MexicoConabioActivator();
        conabio.doImport(cdmDestination);
        System.exit(0);
    }
}