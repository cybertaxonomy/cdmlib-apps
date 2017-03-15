/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.edaphobase;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.terms.RepresentationCsvImportConfigurator;

/**
 * @author a.mueller
 * @date 04.12.2015
 *
 */
public class EdaphobaseRankActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseRankActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_edaphobase();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_edaphobase();

    static final InputStreamReader rankSource = getGermanRanks();

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(InputStreamReader source, ICdmDataSource cdmDestination){


        RepresentationCsvImportConfigurator config= RepresentationCsvImportConfigurator.NewInstance(source, cdmDestination);
        config.setDbSchemaValidation(dbSchemaValidation);

        CdmDefaultImport<RepresentationCsvImportConfigurator> myImport = new CdmDefaultImport<RepresentationCsvImportConfigurator>();
        myImport.invoke(config);

    }


    /**
     * @return
     * @throws IOException
     */
    private static InputStreamReader getGermanRanks() {
        String filename = "Rank_de.csv";
        String path = "terms" + CdmUtils.getFolderSeperator() + filename;
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
        me.doImport(rankSource, cdmDestination);
        System.exit(0);
    }
}
