/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.edaphobase;

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
 * @since 04.12.2015
 */
public class EdaphobaseRankActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_edaphobase();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_edaphobase();

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        InputStreamReader source = getGermanRanks();
        RepresentationCsvImportConfigurator config= RepresentationCsvImportConfigurator.NewInstance(source, cdmDestination);
        config.setDbSchemaValidation(dbSchemaValidation);
        config.setLanguageUuid(Language.uuidGerman);

        CdmDefaultImport<RepresentationCsvImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        config.setLanguageUuid(Language.uuidLatin);
        config.setSource(getLatinRanks());
        myImport.invoke(config);

    }

    private InputStreamReader getGermanRanks() {
        String filename = "Rank_de.csv";
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

    private InputStreamReader getLatinRanks() {
        String filename = "Rank_la.csv";
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


    /**
     * @param args
     */
    public static void main(String[] args) {
        EdaphobaseRankActivator me = new EdaphobaseRankActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
