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
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

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
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_mexico_production();

    static boolean invers = true;

//    boolean doRubiaceae = include;

//    static boolean include = !invers;


    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("d1f4ed29-9aae-4f6e-aa1e-4a3bf780e11d");

    //classification
    static final UUID classificationUuid = UUID.fromString("61968b43-e881-4043-b5c2-ba192e8f72dc");
    private static final String classificationName = "Rubiaceae Conabio";

    static final String sourceReferenceTitle = "Conabio XXX";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);
    static final boolean doTaxa = true;
    static final boolean doDistributions = false;
    static final boolean doCommonNames = false;
//    static final boolean doDeduplicate = true;

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
//        config.setDoVocabularies(doVocabularies);

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference(sourceReferenceTitle));
//        config.setDoVocabularies(doVocabularies);

        CdmDefaultImport<MexicoConabioImportConfigurator> myImport = new CdmDefaultImport<MexicoConabioImportConfigurator>();

        myImport.invoke(config);

//        if (makeFeatureTree){
//            FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
//            myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
//        }

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //Conabio Rubiaceae
    public static URI conabio_rubiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Mexico/CONABIO-Rubiaceae.xlsx");
    }

    private Reference getSourceReference(String string) {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitleCache(string, true);
//        result.setInReference(inRef);
//        inRef.setTitleCache(sourceReferenceTitle, true);
        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        MexicoConabioActivator me = new MexicoConabioActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
