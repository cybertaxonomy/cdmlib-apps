// $Id$
/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.edaphobase;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.CdmImportSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.edaphobase.EdaphobaseImportConfigurator;

/**
 * @author a.mueller
 * @date 04.12.2015
 *
 */
public class EdaphobaseActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final Source edaphoSource = CdmImportSources.EDAPHOBASE();

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("a543d66a-e310-4b3e-a9fa-b729afefad16");
    private static final String featureTreeTitle = "Edaphobase Presentation Feature Tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("91231ebf-1c7a-47b9-a56c-b45b33137244");
    static final String classificationTitle = "Edaphobase";

    private static final boolean doTaxa = true;



    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(Source source, ICdmDataSource cdmDestination){

        EdaphobaseImportConfigurator config= EdaphobaseImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationTitle);
        config.setDoTaxa(doTaxa);

        CdmDefaultImport<EdaphobaseImportConfigurator> myImport = new CdmDefaultImport<EdaphobaseImportConfigurator>();
        myImport.invoke(config);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        EdaphobaseActivator me = new EdaphobaseActivator();
        me.doImport(edaphoSource, cdmDestination);
        System.exit(0);
    }
}
