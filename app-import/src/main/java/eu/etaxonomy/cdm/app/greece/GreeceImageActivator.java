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

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.media.in.MediaExcelImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 *
 * Import for Checklist of Greece.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/6286
 *
 * @author a.mueller
 * @date 13.12.2016
 */
public class GreeceImageActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceImageActivator.class);


//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_greece_checklist_production();

//    private static final UUID sourceUuid = UUID.fromString("418b5885-08fb-4f1e-ac94-8f5c84b1683d");
    private static final UUID sourceUuid = UUID.fromString("c3d300f0-86ef-4c65-8727-c594035ed7a7");
//    private static final String fileName = "20171107_sent_1332_images.xlsx";
    private static final String fileName = "20171110_Turland_433_others_59.xlsx";


    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doImages = true;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = greekChecklist();  //just any
        //make Source
        MediaExcelImportConfigurator config = MediaExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);
        config.setSourceReference(getSourceReference());
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);

        CdmDefaultImport<MediaExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());

    }


    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/images/" + fileName);
    }


    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle(fileName);
        result.setUuid(sourceUuid);

        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GreeceImageActivator me = new GreeceImageActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
