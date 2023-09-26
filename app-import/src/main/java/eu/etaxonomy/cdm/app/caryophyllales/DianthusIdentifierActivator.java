/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.identifier.IdentifierImportConfigurator;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.term.IdentifierType;

/**
 * @author a.mueller
 * @since 2021-11-03
 */
public class DianthusIdentifierActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo_spp();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();

    static final UUID identifierUuid = IdentifierType.uuidWfoNameIdentifier;
    String filename = "Dianthus_WFO_IDs.csv";
//    String filename = "Cactaceae_SpeciesAndBelowWithoutWFO-ID.txt";
    boolean warnAndDoNotOverrideIfExists = true;

//    static final UUID identifierUuid = DefinedTerm.uuidWfoNameIdentifier;
//    String filename = "WFO2CDM_Cactaceae_WFO-ID.txt";

//    static final UUID identifierUuid = DefinedTerm.uuidIpniNameIdentifier;
//    String filename = "WFO2CDM_Cactaceae_IPNI-ID.txt";

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        InputStreamReader source = getNepenthesIdentifier();
        IdentifierImportConfigurator config= IdentifierImportConfigurator.NewInstance(source, cdmDestination);
        config.setWarnAndDoNotOverrideIfExists(warnAndDoNotOverrideIfExists);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setIdentifierTypeUuid(identifierUuid);
        config.setCdmClass(TaxonName.class);
        config.setCheck(check);

        CdmDefaultImport<IdentifierImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private InputStreamReader getNepenthesIdentifier() {

//        URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/fmvol14_final2.xml")
        String path = "C://opt//data//Caryophyllales";
        File file = new File(path + File.separator + filename);
        if (!file.exists()){
            System.exit(-1);
            return null;
        }
        try {
            InputStreamReader input = new FileReader(file);
            return input;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static void main(String[] args) {
        DianthusIdentifierActivator me = new DianthusIdentifierActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}