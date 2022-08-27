/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.casearia;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.casearia.CaseariaImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;

/**
 * @author a.mueller
 * @since 08.05.2020
 */
public class CaryoCaseariaActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

//    private static final ICdmDataSource destinationDb = CdmDestinations.cdm_local_casearia();
    private static final ICdmDataSource destinationDb = CdmDestinations.cdm_production_casearia();

    private static String filename = "Kew-2019-12-19-Casearia.xlsx";

    private boolean doDeduplicate = true;

    private static final UUID secUuid = UUID.fromString("e8cf6605-c577-4ea9-8258-05681d49a21b");

    private static final UUID sourceUuid = UUID.fromString("7062ad70-976b-4565-9ffd-5b2874d282ed");

    private static final String classificationName = "Casearia";

    private void invoke(ICdmDataSource destination){

        URI source =excelFile();
        CaseariaImportConfigurator config = CaseariaImportConfigurator.NewInstance(source, destination);
        config.setSecUuid(secUuid);
        config.setDoDeduplicate(doDeduplicate);
        config.setSourceReferenceTitle(filename + " (export from WCVP)");
        config.setSourceRefUuid(sourceUuid);
        config.setDbSchemaValidation(hbm2dll);
        config.setClassificationName(classificationName);

        CdmDefaultImport<CaseariaImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    public static URI excelFile() {
        return URI.create("file:////BGBM-PESIHPC/Casearia/" + filename);
//        return URI.create("file:////BGBM-PESIHPC/Caryophyllales/Kew/Aizoaceae_500.xlsx");
    }

    public static void main(String[] args) {
        CaryoCaseariaActivator me = new CaryoCaseariaActivator();
        me.invoke(destinationDb);
        System.exit(0);
    }
}
