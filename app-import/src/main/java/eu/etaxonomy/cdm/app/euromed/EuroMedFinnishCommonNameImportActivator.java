/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.fact.commonname.in.CommonNameExcelImportConfigurator;
import eu.etaxonomy.cdm.model.common.TreeIndex;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;

/**
 * @author a.mueller
 * @since 24.01.2023
 */
public class EuroMedFinnishCommonNameImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_euroMed();
//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

    static final UUID sourceRefUuid = UUID.fromString("c239bd59-63b8-442a-b097-4a7a7869b933");
    static final OriginalSourceType sourceType = OriginalSourceType.PrimaryTaxonomicSource;

    static final UUID finnishUuid = UUID.fromString("4606abdf-6dbe-4bfe-b3da-a1fa08f2f1e2");
    static final UUID finland = BerlinModelTransformer.uuidEMAreaCommonNameFinland;
    static final UUID swedishUuid = UUID.fromString("d4541a74-becc-4ba6-874c-510ee0d0a29f");
    static final UUID swedenAndFinland = BerlinModelTransformer.uuidEMAreaCommonNameSwedenAndFinland;
    static final TreeIndex treeIndexFilter = TreeIndex.NewInstance("#t10#10#56284#"); //Plantae
    static final boolean allowNameMatch = true;

    static final boolean isSwedish = true;

    static final UUID areaUuid = isSwedish ? swedenAndFinland : finland;
    static final UUID languageUuid = isSwedish ? swedishUuid : finnishUuid;
    static final String commonNameCol = isSwedish ? "SWEDISH_NAME" : "FINNISH_NAME";


    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = getEM_FinnishCN_Uri();
        CommonNameExcelImportConfigurator config;
//        try {
            config = CommonNameExcelImportConfigurator.NewInstance(source, cdmDestination);

            config.setDbSchemaValidation(dbSchemaValidation);
            config.setCheck(check);
            config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
            config.setSourceRefUuid(sourceRefUuid);
            config.setSourceType(sourceType);
//            config.setSourceReference(getSourceReference());
            config.setDefaultLanguageUuid(languageUuid);
            config.setDefaultAreaUuid(areaUuid);
            config.setCommonNameColumnLabel(commonNameCol);
            config.setWorksheetName("ACCEPTED");
            config.setTreeIndexFilter(treeIndexFilter);
            config.setAllowNameMatching(allowNameMatch);

            CdmDefaultImport<CommonNameExcelImportConfigurator> myImport = new CdmDefaultImport<>();
            ImportResult result = myImport.invoke(config);
            System.out.println(result.createReport());
//        } catch (IOException e) {
//            System.out.println("URI not 'found': " + source);
//        }
    }

    private URI getEM_FinnishCN_Uri(){
        String fileName = "EM_finnish_commonNames_fi_2019.xlsx";

        URI uri = URI.create("file:////BGBM-PESIHPC/EuroMed/" +  fileName);
        if (!uri.toFile().exists()) {
            System.exit(-1);
        }
        return uri;
    }

//    private Reference getSourceReference(){
//        Reference result = ReferenceFactory.newDatabase();
//        result.setTitle("Euro+Med IPNI import");
//        result.setUuid(UUID.fromString("937f3a38-dbb4-4888-9ea3-2f52d47f7953"));
//        result.setDatePublished(VerbatimTimePeriod.NewVerbatimNowInstance());
//        return result;
//    }

    public static void main(String[] args) {
        EuroMedFinnishCommonNameImportActivator me = new EuroMedFinnishCommonNameImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
