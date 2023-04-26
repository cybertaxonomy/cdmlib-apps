/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.euromed.EuroMedEuroMossesImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
public class EuroMedMossesImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_euroMed();
//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

    private static final UUID hodgettsUuid = UUID.fromString("06c0cf1e-9bcd-4f2b-8c77-1ef1485c2328");

    //check - import
    private static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private static boolean doGenera = false;
    private static boolean isLiverworts = true;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = getMossesUri();

        EuroMedEuroMossesImportConfigurator config = EuroMedEuroMossesImportConfigurator.NewInstance(source, cdmDestination);

        config.setDbSchemaValidation(dbSchemaValidation);
        config.setCheck(check);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        config.setSourceReference(getSourceReference());
        config.setSecUuid(hodgettsUuid);
        config.setDoGenera(doGenera);
        config.setLiverwort(isLiverworts);

        CdmDefaultImport<EuroMedEuroMossesImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    private URI getMossesUri(){
        String fileName = doGenera ? "EuroMoss_names.xls":
            isLiverworts ? "European liverworts+Tropicos.xlsx":
                    "European checklist+TropicosRefStatus+ID.xlsx";
//
//        URI uri = URI.create("file:////BGBM-PESIHPC/EuroMed/" +  fileName);
//        return uri;
        File file = new File("E://data/EuroMed/" +  fileName);
        if (!file.exists()) {
            System.exit(0);
        }
        URI uri = URI.fromFile(file);
        return uri;
    }

    private Reference getSourceReference(){
        if (doGenera) {
            Reference result = ReferenceFactory.newDatabase();
            result.setTitle("EuroMoss_names.xls");
            result.setUuid(UUID.fromString("45112c45-dc80-4512-81a5-c8466771b1b1"));
            result.setDatePublished(TimePeriodParser.parseStringVerbatim("2006"));
            return result;
        }else if (!isLiverworts) {
            Reference result = ReferenceFactory.newDatabase();
            result.setTitle("European checklist+TropicosRefStatus+ID.xlsx");
            result.setUuid(UUID.fromString("bc1c7880-ba6f-4549-95d0-e6b8fd089c35"));
            result.setDatePublished(TimePeriodParser.parseStringVerbatim("2020"));
            return result;
        }else {
            Reference result = ReferenceFactory.newDatabase();
            result.setTitle("European liverworts+Tropicos.xlsx");
            result.setUuid(UUID.fromString("cbd7819b-3c4d-4a91-9d23-10b6e1cdeba9"));
            result.setDatePublished(TimePeriodParser.parseStringVerbatim("2020"));
            return result;
        }
    }

    public static void main(String[] args) {
        EuroMedMossesImportActivator me = new EuroMedMossesImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}