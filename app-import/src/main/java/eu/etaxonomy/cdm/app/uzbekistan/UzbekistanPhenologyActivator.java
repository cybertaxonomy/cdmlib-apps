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
import eu.etaxonomy.cdm.io.fact.temporal.in.PhenologyExcelImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Import for Flora of Uzbekistan phenology.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/9126
 *
 * @author a.mueller
 * @since 15.07.2020
 */
public class UzbekistanPhenologyActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_uzbekistan();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_uzbekistan();

    static DbSchemaValidation hbm2dll = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    static final String fileName = "FoU_template_flowering.xlsx";

    static final UUID uuidSourceRef = UUID.fromString("fea9175d-d1be-4f92-b4c2-bec7df449b6e");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = fileName();
//        URI source = uzbekistanChecklist_local();

        PhenologyExcelImportConfigurator config = PhenologyExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setFloweringStartColumnLabel("начало цветения");
        config.setFloweringEndColumnLabel("конец цветения");
        config.setFruitingStartColumnLabel("начало плодоношения");
        config.setFruitingEndColumnLabel("конец плодоношения");

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<PhenologyExcelImportConfigurator> myImport = new CdmDefaultImport<>();
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
        UzbekistanPhenologyActivator me = new UzbekistanPhenologyActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}