/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.lichenes;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.lichenes.CoraImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 *
 * Import for species of lichenes genus Cora.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/10432
 *
 * @author a.mueller
 * @since 2023-11-23
 */
public class CoraImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_lichenes_cora();

    //classification
    static final UUID classificationUuid = UUID.fromString("43183724-1919-4036-84ee-3e0e84938f8d");
    private static final String classificationName = "Lichenes";

    static final UUID secRefUuid = UUID.fromString("5c2e5198-6cbd-4d30-97d5-ee2abb1f422c");
    static final UUID sourceRefUuid = UUID.fromString("ac67197e-8668-45cf-82bf-3682d21b3e69");

    static final String fileName = "cora_names_EDIT.xlsx";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = coraChecklist();
//        URI source = lichenesChecklist_local();

        CoraImportConfigurator config = CoraImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
//        config.setUuidFeatureTree(featureTreeUuid);
//        config.setFeatureTreeTitle(featureTreeTitle);
        config.setSecReference(getSecReference());
        config.setSourceReference(getSourceReference());
        config.setNomenclaturalCode(NomenclaturalCode.Fungi);

        CdmDefaultImport<CoraImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }


    private URI coraChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Lichenes/" + fileName);
    }

    private Reference secRef;
    private Reference getSecReference(){
        if (secRef != null){
            return secRef;
        }
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Default sec-reference for Cora");
        secRef = result;

        secRef.setUuid(secRefUuid);
        return result;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Excelfile (LichenesGeneraImport.xlsx) derived from ");
        result.setInReference(getSecReference());
        result.setUuid(sourceRefUuid);

        return result;
    }

    public static void main(String[] args) {
        CoraImportActivator me = new CoraImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}