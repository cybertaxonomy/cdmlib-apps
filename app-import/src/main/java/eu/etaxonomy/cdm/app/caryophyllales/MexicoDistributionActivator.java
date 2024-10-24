/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

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
import eu.etaxonomy.cdm.io.fact.distribution.in.DistributionExcelImportConfigurator;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Import for Mexico distribution for eFloraMex.
 *
 * @author a.mueller
 * @since 07.10.2024
 */
public class MexicoDistributionActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_caryophyllales();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales();

    static DbSchemaValidation hbm2dll = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    static final String fileName = "NyctaginaceaeTaxaInMexico.xlsx";

    static final UUID uuidSourceRef = UUID.fromString("f9868eaf-2434-4095-b3ef-16d20e660f8e");
    static final UUID uuidAreaMexicoCountry = UUID.fromString("4ba4809b-3fa8-496d-a74d-80843a4740c8");
    static final UUID uuidStatusPresent = UUID.fromString("cef81d25-501c-48d8-bbea-542ec50de2c2");
    static final UUID uuidEfloraMexMarker = UUID.fromString("ba2c1a71-7886-4968-851f-0f898e4db172");
    static final UUID uuidEfloraMexDistributionFeature = UUID.fromString("033bfc67-15b4-496c-9a91-dd0df14590ba");

    static final boolean useAcceptedNameAsNameUsedInSource = true;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final String colNameTitleCache = "FullNameWithAuthors";

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = fileName_local();
//        URI source = uzbekistanChecklist_local();

        DistributionExcelImportConfigurator config = DistributionExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setDefaultAreaUuid(uuidAreaMexicoCountry);
        config.setDefaultStatusUuid(uuidStatusPresent);
        config.setColNameTitleCache(colNameTitleCache);
        config.setDescriptionMarkerTypeUuid(uuidEfloraMexMarker);
        config.setDistributionFeatureUuid(uuidEfloraMexDistributionFeature);
        config.setSourceType(OriginalSourceType.PrimaryTaxonomicSource);
        config.setUseAcceptedNameAsNameUsedInSource(useAcceptedNameAsNameUsedInSource);

        config.setSourceRefUuid(uuidSourceRef);
//        config.setSourceReference(getSourceReference());

        CdmDefaultImport<DistributionExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    @SuppressWarnings("unused")
    private URI fileName(){
        return URI.create("file:////BGBM-PESIHPC/Caryophyllales/" + fileName);
    }
    public static URI fileName_local(){

        File file = new File("E://data/Caryophyllales/" +  fileName);
        if (!file.exists()) {
            System.exit(0);
        }
        URI uri = URI.fromFile(file);
        return uri;
      }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle(fileName);
        result.setUuid(uuidSourceRef);
        return result;
    }

    public static void main(String[] args) {
        MexicoDistributionActivator me = new MexicoDistributionActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}