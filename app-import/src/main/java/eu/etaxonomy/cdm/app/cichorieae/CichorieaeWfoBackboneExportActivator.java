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
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.wfo.out.WfoBackboneExportConfigurator;
import eu.etaxonomy.cdm.model.term.IdentifierType;

/**
 * Activator to export Cichorieae to WFO via classification DwC-A.
 *
 * @author a.mueller
 * @since 2023-12-09
 */
public class CichorieaeWfoBackboneExportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_cichorieae();
//    static final ICdmDataSource cdmSource = CdmDestinations.cdm_production_cichorieae();

    static final UUID identifierUuid = IdentifierType.uuidWfoNameIdentifier;
    String fileName = "CichorieaeWfoBackboneExport.csv";  //TODO add date

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmSource){

        File fileDestination = getCichorieaeExport();
        WfoBackboneExportConfigurator config= WfoBackboneExportConfigurator.NewInstance(cdmSource, fileDestination);
        config.setDbSchemaValidation(dbSchemaValidation);

        CdmDefaultExport<WfoBackboneExportConfigurator> myImport = new CdmDefaultExport<>();
        myImport.invoke(config);
    }

    private File getCichorieaeExport() {

        String path = "E://data//Cichorieae//export";
        File file = new File(path + File.separator + fileName);
        return file;
    }

    public static void main(String[] args) {
        CichorieaeWfoBackboneExportActivator me = new CichorieaeWfoBackboneExportActivator();
        me.doImport(cdmSource);
        System.exit(0);
    }
}