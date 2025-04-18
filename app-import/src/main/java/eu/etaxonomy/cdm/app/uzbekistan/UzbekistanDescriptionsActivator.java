/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.uzbekistan;

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
import eu.etaxonomy.cdm.io.fact.textdata.in.TextDataExcelImportConfigurator;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Import for Flora of Uzbekistan descriptions.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/9125
 *
 * @author a.mueller
 * @since 06.07.2020
 */
public class UzbekistanDescriptionsActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_uzbekistan();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_uzbekistan();

    static DbSchemaValidation hbm2dll = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    static final String fileName = "FoU_template_descriptions.xls";

    static final UUID uuidDescription = Feature.uuidDescription;
    static final UUID uuidTextLanguage = Language.uuidRussian;

    static final UUID uuidSourceRef = UUID.fromString("5d7ee0da-ccce-4666-b6f9-434d71dbddfd");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = fileName();
//        URI source = uzbekistanChecklist_local();

        TextDataExcelImportConfigurator config = TextDataExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setFeatureUuid(uuidDescription);
        config.setSourceReference(getSourceReference());
        config.setTextLanguageUuid(uuidTextLanguage);
        config.setTextColumnLabel("morphological description");

        CdmDefaultImport<TextDataExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    private URI fileName(){
        return URI.create("file:////BGBM-PESIHPC/Uzbekistan/" + fileName);
    }
    @SuppressWarnings("unused")
    private URI fileName_local(){
        File file = new File("C:\\Users\\a.mueller\\BGBM\\Data\\Uzbekistan\\" + fileName);
        return URI.fromFile(file);
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle(fileName);
        result.setUuid(uuidSourceRef);
        return result;
    }

    public static void main(String[] args) {
        UzbekistanDescriptionsActivator me = new UzbekistanDescriptionsActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
