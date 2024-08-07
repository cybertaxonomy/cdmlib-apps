/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.edaphobase;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.CdmImportSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.edaphobase.EdaphobaseImportConfigurator;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 * @since 04.12.2015
 */
public class EdaphobaseActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.CREATE;

//    static final Source edaphoSource = CdmImportSources.EDAPHOBASE();
    static final Source edaphoSource = CdmImportSources.EDAPHOBASE8();

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_edaphobase();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_edaphobase();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_postgres_edaphobase();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_postgres_edaphobase();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_postgres_edaphobase();



    //feature tree uuid
    private static final UUID featureTreeUuid = UUID.fromString("a543d66a-e310-4b3e-a9fa-b729afefad16");
    private static final String featureTreeTitle = "Edaphobase Presentation Feature Tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("91231ebf-1c7a-47b9-a56c-b45b33137244");
    static final String classificationTitle = "Edaphobase";
    static final UUID secUuid = UUID.fromString("95dcb1b6-5197-4ce6-b2fa-c6482119d2ea");
    static final String secundumTitle = "Edaphobase";

    private static final boolean doTaxa = true;
    private static final boolean doSynonyms = true;
    private static final boolean doReferences = true;
    private static final boolean doDescriptions = false;

    //logging
    private static final boolean ignoreSubgenus = true;
    private static final boolean ignore4nomial = true;


    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(Source source, ICdmDataSource cdmDestination){

        EdaphobaseImportConfigurator config= EdaphobaseImportConfigurator.NewInstance(source, cdmDestination);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationTitle);
        config.setEdaphobaseSecundumTitle(secundumTitle);
        config.setSecUuid(secUuid);
        config.setDoTaxa(doTaxa);
        config.setDoReferences(doReferences);
        config.setDoSynonyms(doSynonyms);
        config.setDoDescriptions(doDescriptions);
        config.setCheck(check);

        config.setIgnoreSubgenus(ignoreSubgenus);
        config.setIgnore4nomial(ignore4nomial);

        CdmDefaultImport<EdaphobaseImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        TermTree<Feature> tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
        myImport.getCdmAppController().getTermTreeService().saveOrUpdate(tree);
    }

    private TermTree<Feature> makeFeatureNodes(ITermService service){

        TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
        result.setTitleCache(featureTreeTitle, true);
        TermNode<Feature> root = result.getRoot();

        Feature distributionFeature = (Feature)service.find(Feature.DISTRIBUTION().getUuid());
        root.addChild(distributionFeature);

        return result;
    }

    public static void main(String[] args) {
        EdaphobaseActivator me = new EdaphobaseActivator();
        me.doImport(edaphoSource, cdmDestination);
        System.exit(0);
    }
}