/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.cichorieae;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
public class CichorieaeWfoIdentifierActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cichorieae();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cichorieae();

    static final UUID identifierUuid = IdentifierType.uuidWfoNameIdentifier;
//    static final String fileName = "Cichorieae_Syn_WFOID_Matches.csv";
    static final String fileName = "Cichorieae_Syn_WFOID_10_NewToWfo.csv";

    boolean warnAndDoNotOverrideIfExists = true;

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        InputStreamReader source = getCichorieaeIdentifier();
        IdentifierImportConfigurator config= IdentifierImportConfigurator.NewInstance(source, cdmDestination);
        config.setWarnAndDoNotOverrideIfExists(warnAndDoNotOverrideIfExists);
        config.setIdentifierTypeUuid(identifierUuid);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setCdmClass(TaxonName.class);
        config.setCheck(check);
        config.setIgnoreEmptyIdentifier(true);

        CdmDefaultImport<IdentifierImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private InputStreamReader getCichorieaeIdentifier() {

        String path = "E://data//Cichorieae";
        File file = new File(path + File.separator + fileName);
        if (!file.exists()){
            System.exit(-1);
            return null;
        }
        try {
            URL url = file.toURI().toURL();
            InputStream stream = url.openStream();
            InputStreamReader input = new InputStreamReader(stream, "UTF8");
//            InputStreamReader input = new FileReader(file);
            return input;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static void main(String[] args) {
        CichorieaeWfoIdentifierActivator me = new CichorieaeWfoIdentifierActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}