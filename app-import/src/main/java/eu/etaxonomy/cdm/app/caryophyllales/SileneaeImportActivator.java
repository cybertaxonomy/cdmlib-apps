/**
* Copyright (C) 2023 EDIT
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
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.caryo.CaryoSileneaeImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @date 01.02.2023
 */
public class SileneaeImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    private static final DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
    private static final URI source = sileneae();

    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();
//    private static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();

    private static final UUID acceptedUuid = UUID.fromString("8ac4a182-1281-4473-b6a1-c4f681f7dea3");
    private static final UUID unresolvedUuid = UUID.fromString("99ab0e67-4214-4bc7-884d-6af8c2e60c38");
    private static final UUID classificationUuid = UUID.fromString("9edc58b5-de3b-43aa-9f31-1ede7c009c2b");
    private static final UUID secUuid = UUID.fromString("770bf23b-68ab-4074-8241-3ed6899694af");

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        CaryoSileneaeImportConfigurator config = CaryoSileneaeImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setAcceptedNodeUuid(acceptedUuid);
        config.setUnresolvedNodeUuid(unresolvedUuid);
        config.setCheck(check);
//      config.setDoTaxa(doTaxa);
        config.setDbSchemaValidation(hbm2dll);
        config.setSecUuid(secUuid);
        config.setSourceReferenceTitle("Sileneae.xlsx");

        CdmDefaultImport<CaryoSileneaeImportConfigurator> myImport = new CdmDefaultImport<>();

        //...
        if (true){
            System.out.println("Start import from ("+ source.toString() + ") ...");
            config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
            myImport.invoke(config);
            System.out.println("End import from ("+ source.toString() + ")...");
        }
    }

    private Reference getSourceReference(String string) {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitleCache(string, true);
        return result;
    }

    public static URI sileneae(){
        String fileName = "Sileneae.xlsx";
        File file = new File("E://data/Caryophyllales/" +  fileName);
        if (!file.exists()) {
            System.exit(0);
        }
        URI uri = URI.fromFile(file);
        return uri;
    }

    public static void main(String[] args) {
        SileneaeImportActivator me = new SileneaeImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
