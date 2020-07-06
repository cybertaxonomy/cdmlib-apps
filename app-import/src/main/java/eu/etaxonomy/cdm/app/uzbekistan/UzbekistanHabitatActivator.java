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
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.fact.categorical.in.CategoricalDataExcelImportConfigurator;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 *
 * Import for Flora of Uzbekistan habitats.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/9049
 *
 * @author a.mueller
 * @since 28.05.2020
 */
public class UzbekistanHabitatActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UzbekistanHabitatActivator.class);

    //database validation status (create, update, validate ...)

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_uzbekistan();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_uzbekistan();

    static DbSchemaValidation hbm2dll = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    static final String fileName = "FoU_template_habitats_final.xlsx";

    static final UUID uuidHabitat = Feature.uuidHabitat;
    static final UUID uuidStateVocabulary = UUID.fromString("8c06bc6c-8f06-421d-990b-4faf6bf5840e");
    static final String stateVocabularyLabel = "Flora of Uzbekistan habitat states";

    static final UUID uuidSourceRef = UUID.fromString("6278112a-67c3-4ddf-9a81-2f8c1915d52a");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = fileName();
//        URI source = uzbekistanChecklist_local();

        CategoricalDataExcelImportConfigurator config = CategoricalDataExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setFeatureUuid(uuidHabitat);
        config.setStateVocabularyUuid(uuidStateVocabulary);
        config.setStateVocabularyLabel(stateVocabularyLabel);
        config.setSourceReference(getSourceReference());

        CdmDefaultImport<CategoricalDataExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }


    private URI fileName(){
        return URI.create("file:////BGBM-PESIHPC/Uzbekistan/" + fileName);
    }
    @SuppressWarnings("unused")
    private URI fileName_local(){
        File file = new File("C:\\Users\\a.mueller\\BGBM\\Data\\Uzbekistan\\" + fileName);
    	return file.toURI();
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle(fileName);
        result.setUuid(uuidSourceRef);
        return result;
    }

    public static void main(String[] args) {
        UzbekistanHabitatActivator me = new UzbekistanHabitatActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
