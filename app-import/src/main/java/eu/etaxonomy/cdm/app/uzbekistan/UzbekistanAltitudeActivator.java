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
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.fact.altitude.in.AltitudeExcelImportConfigurator;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 *
 * Import for Flora of Uzbekistan altitudes.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/9041
 *
 * @author a.mueller
 * @since 28.05.2020
 */
public class UzbekistanAltitudeActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UzbekistanAltitudeActivator.class);

    //database validation status (create, update, validate ...)

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_uzbekistan();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_uzbekistan();

    static DbSchemaValidation hbm2dll = cdmDestination == CdmDestinations.localH2() ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    static final String fileName = "FoU template habitats final.xlsx";

    static final UUID uuidAltitude = Feature.uuidAltitude;

    static final UUID uuidSourceRef = UUID.fromString("6278112a-67c3-4ddf-9a81-2f8c1915d52a");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = fileName();
//        URI source = uzbekistanChecklist_local();

        AltitudeExcelImportConfigurator config = AltitudeExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setFeatureUuid(uuidAltitude);
        config.setSourceReference(getSourceReference());

        CdmDefaultImport<AltitudeExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
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
        UzbekistanAltitudeActivator me = new UzbekistanAltitudeActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
