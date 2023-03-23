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
 * @since 18.10.2017
 */
public class PlumbaginaceaeIdentifierActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();

//    private static final UUID idTypeUuid = DefinedTerm.uuidWfoNameIdentifier;
    private static final UUID idTypeUuid = IdentifierType.uuidIpniNameIdentifier;

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        InputStreamReader source = getPlumbaginaceaeIdentifier_IPNI();
        IdentifierImportConfigurator config= IdentifierImportConfigurator.NewInstance(source, cdmDestination);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setIdentifierTypeUuid(idTypeUuid);
        config.setCdmClass(TaxonName.class);
        config.setCheck(check);
        config.setIgnoreEmptyIdentifier(true);

        CdmDefaultImport<IdentifierImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private InputStreamReader getPlumbaginaceaeIdentifier() {
        String filename = "WFONamesInCDM_Plumbaginaceae.csv";

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

    private InputStreamReader getPlumbaginaceaeIdentifier_IPNI() {
        String filename = "WFONamesInCDM_Plumbaginaceae_IPNI.csv";

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
        PlumbaginaceaeIdentifierActivator me = new PlumbaginaceaeIdentifierActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}