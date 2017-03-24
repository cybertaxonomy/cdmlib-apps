/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.greece.FloraHellenicaImportConfigurator;
import eu.etaxonomy.cdm.io.greece.FloraHellenicaTransformer;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;

/**
 *
 * Import for Checklist of Greece.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/6286
 *
 * @author a.mueller
 * @date 13.12.2016
 */
public class FloraHellenicaActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FloraHellenicaActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_greece_checklist_production();

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("9e1e0e81-7475-4b28-8619-b7f42cd760b6");
    private static final String featureTreeTitle = "Flora Hellenica dataportal feature tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("e537d69a-c2d9-4ac6-8f79-5b5e3dd5c154");
    private static final String classificationName = "Greek Checklist";


    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);

    private void doImport(ICdmDataSource cdmDestination){

        URI source = greekChecklist();  //just any

        //make Source
        FloraHellenicaImportConfigurator config= FloraHellenicaImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
//      config.setDoDistribution(doDistribution);
//        config.setDoTaxa(doTaxa);
        config.setDbSchemaValidation(hbm2dll);
//        config.setSourceReferenceTitle(sourceReferenceTitle);
//        config.setDoVocabularies(doVocabularies);

        CdmDefaultImport<FloraHellenicaImportConfigurator> myImport = new CdmDefaultImport<FloraHellenicaImportConfigurator>();
        myImport.invoke(config);

        FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
        myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
    }

    private FeatureTree makeFeatureNodes(ITermService service){

        FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
        result.setTitleCache(featureTreeTitle, true);
        FeatureNode root = result.getRoot();
        FeatureNode newNode;

        Feature newFeature = (Feature)service.find(Feature.DISTRIBUTION().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(FloraHellenicaTransformer.uuidFloraHellenicaChorologyFeature);
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.LIFEFORM().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.HABITAT().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);


        newFeature = (Feature)service.find(Feature.NOTES().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        return result;
    }

    private URI greekChecklist_OldVersion(){
        return URI.create("file:////BGBM-PESIHPC/Greece/VPG_FINAL_June_2016.xlsx");
    }

    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/VPG_FINAL_WITH_SYNONYMS_21.01.2017.xlsx");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        FloraHellenicaActivator me = new FloraHellenicaActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
