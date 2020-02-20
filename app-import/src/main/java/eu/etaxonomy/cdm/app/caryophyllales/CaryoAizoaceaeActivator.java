/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.caryo.CaryoAizoaceaeExcelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;

/**
 * @author a.mueller
 * @since 17.02.2020
 */
public class CaryoAizoaceaeActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(CaryoAizoaceaeActivator.class);

    private static final ICdmDataSource destinationDb = CdmDestinations.cdm_local_caryo_aizoaceae();
//    private static final ICdmDataSource destinationDb = CdmDestinations.cdm_production_caryophyllales();

    private static final UUID secUuid = UUID.fromString("07cbb595-8cad-494c-80ba-9969925a6f78");

    private void invoke(ICdmDataSource destination){

        URI source =excelFile();
        CaryoAizoaceaeExcelImportConfigurator config = CaryoAizoaceaeExcelImportConfigurator.NewInstance(source, destination);
        config.setSecUuid(secUuid);
        config.setSourceReferenceTitle("Aizoaceae_UTF8.xlsx (export from Kew World Plant List)");

        CdmDefaultImport<CaryoAizoaceaeExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    public static URI excelFile() {
        return URI.create("file:////BGBM-PESIHPC/Caryophyllales/Kew/Aizoaceae_UTF8.xlsx");
//        return URI.create("file:////BGBM-PESIHPC/Caryophyllales/Kew/Aizoaceae_500.xlsx");
    }

    public static void main(String[] args) {

        CaryoAizoaceaeActivator me = new CaryoAizoaceaeActivator();
        me.invoke(destinationDb);
        System.exit(0);
    }
}
