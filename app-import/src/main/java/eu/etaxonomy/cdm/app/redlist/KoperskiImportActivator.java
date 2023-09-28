/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.redlist;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.redlist.moose.KoperskiImportConfigurator;
import eu.etaxonomy.cdm.io.uzbekistan.UzbekistanTaxonImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Import activator for Koperski & al., Referenzliste der Moose Deutschlands.
 *
 * @author a.mueller
 * @since 03.08.2023
 */
public class KoperskiImportActivator {

    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_moose();

    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
//    static DbSchemaValidation hbm2dll =  cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    //classification
    static final UUID classificationUuid = UUID.fromString("268eb7fd-933a-430f-8f75-e5c6536d4a7d");
    private static final String classificationName = "Moose";

    static final UUID secRefUuid = UUID.fromString("7fbcbace-c9ef-445f-a142-b8dd045aad4c");
    static final UUID sourceRefUuid = UUID.fromString("73c0e018-db75-4b16-a0df-89ee0b3960e0");

    static final String fileName = "MooseAM.xlsx";
    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){
    	System.out.println("Start");
    	logger.warn("xxx");

        //make Source
        URI source = moose_local();

        KoperskiImportConfigurator config = KoperskiImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
//        config.setUuidFeatureTree(featureTreeUuid);
//        config.setFeatureTreeTitle(featureTreeTitle);
//        config.setSecReference(getSecReference());
        config.setSecUuid(secRefUuid);
        config.setSourceReference(getSourceReference());

        CdmDefaultImport<KoperskiImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    @SuppressWarnings("unused")
    private URI moose(){  //not checked yet
        return URI.create("file:////BGBM-PESIHPC/Moose/" + fileName);
    }
    private URI moose_local(){
        File file = new File("E:\\data\\RoteListen\\Moose\\" + fileName);
        return URI.fromFile(file);
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("MooseAM.xslx");
//        result.setInReference(getSecReference());
        result.setUuid(sourceRefUuid);

        return result;
    }

    public static void main(String[] args) {
        KoperskiImportActivator me = new KoperskiImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}