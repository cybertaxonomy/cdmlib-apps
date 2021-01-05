/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import eu.etaxonomy.cdm.common.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.greece.GreeceStatusUpdaterConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 08.12.2017
 */
public class GreeceStatusUpdaterActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceStatusUpdaterActivator.class);

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_greece();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

    private static final UUID sourceUuid = UUID.fromString("7f898cf8-5eef-4321-ba17-64983cf7ea26");
    private static final String fileName = "FoG_new_fields_14.03.2020_sent_8.xls";


    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        DbSchemaValidation schemaVal = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;
        URI source = greekChecklist();  //just any
        //make Source
        GreeceStatusUpdaterConfigurator config = GreeceStatusUpdaterConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(schemaVal);
        config.setSourceReference(getSourceReference());
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);

        CdmDefaultImport<GreeceStatusUpdaterConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/newStatus/" + fileName);
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle(fileName);
        result.setUuid(sourceUuid);
        return result;
    }

    public static void main(String[] args) {
        GreeceStatusUpdaterActivator me = new GreeceStatusUpdaterActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}