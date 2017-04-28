/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.faueu;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.distribution.excelupdate.ExcelDistributionUpdateConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @date 27.04.2017
 *
 */
public class FaunaEuropaeaDistributionUpdateActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FaunaEuropaeaDistributionUpdateActivator.class);

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_faunaEu_mysql();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi_fauna_europaea();
    static final URI source = fauEuDistrUpdateSource();
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final UUID areaVocabularyUuid = UUID.fromString("16325043-e7da-4742-b012-9ce03362a124");

    /**
     * @param source
     * @param args
     */
    private void invoke(URI source, ICdmDataSource cdmDestination) {


        ICdmDataSource destination = cdmDestination;
        System.out.println("Starting update for Fauna Europaea distribution data from (" + source + ")...");

                // invoke Fauna Europaea to CDM import

        ExcelDistributionUpdateConfigurator config =
                ExcelDistributionUpdateConfigurator.NewInstance(source,  destination, areaVocabularyUuid);

        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);
        config.setCheck(check);
        config.setNomenclaturalCode(NomenclaturalCode.ICZN);

        try {
            ImportResult result = new CdmDefaultImport<ExcelDistributionUpdateConfigurator>().invoke(config);
            List<byte[]> reports = result.getReports();
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("End updating Fauna Europaea data");
    }


    private static URI fauEuDistrUpdateSource(){
        return URI.create("file:////BGBM-PESIHPC/fauEu/Thysanoptera-orig_update_20-i-2017-2.xlsx");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        FaunaEuropaeaDistributionUpdateActivator me = new FaunaEuropaeaDistributionUpdateActivator();
        me.invoke(source, cdmDestination);
        System.exit(0);
    }


}
