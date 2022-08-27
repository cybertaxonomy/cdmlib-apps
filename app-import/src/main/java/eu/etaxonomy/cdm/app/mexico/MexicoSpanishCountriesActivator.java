/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.mexico;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.terms.RepresentationCsvImportConfigurator;
import eu.etaxonomy.cdm.model.common.Language;

/**
 * @author a.mueller
 * @since 2021-09-30
 */
public class MexicoSpanishCountriesActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_mexico();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_mexico_flora();

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        InputStreamReader source = getSpanishCountries();
        RepresentationCsvImportConfigurator config= RepresentationCsvImportConfigurator.NewInstance(source, cdmDestination);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setLanguageUuid(Language.uuidSpanish_Castilian);
        config.setCheck(check);

        CdmDefaultImport<RepresentationCsvImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private InputStreamReader getSpanishCountries() {
        String filename = "Country_es.csv";
        String path = "terms" + File.separator + filename;
        try {
            InputStreamReader input = CdmUtils.getUtf8ResourceReader(path);
            return input;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public static void main(String[] args) {
        MexicoSpanishCountriesActivator me = new MexicoSpanishCountriesActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}