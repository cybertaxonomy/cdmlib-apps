/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.pesi.euromed.EuroMedSourcesImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;

/**
 * @author a.mueller
 * @since 08.10.2019
 */
public class EuroMedSourceActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EuroMedSourceActivator.class);

    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_euromed();

    UUID sourceRefUuid = PesiTransformer.uuidSourceRefEuroMed;
    String sourceReferenceTitle = "Euro+Med CDM database";

    private void doImport(ICdmDataSource cdmDB) {
        System.out.println("Start adding EuroMed sources to " + cdmDB.getDatabase() + " ..." );

        EuroMedSourcesImportConfigurator config = EuroMedSourcesImportConfigurator.NewInstance(cdmDB);
        config.setSourceRefUuid(sourceRefUuid);
        config.setSourceReferenceTitle(sourceReferenceTitle);
        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

        CdmDefaultImport<EuroMedSourcesImportConfigurator> cdmImport = new CdmDefaultImport<>();
        cdmImport.invoke(config);
    }

    public static void main(String[] args) {
        logger.debug("This is to force log4j to use the PESI log4j config file");
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        EuroMedSourceActivator euromedSourceImport = new EuroMedSourceActivator();
        euromedSourceImport.doImport(cdmDB);
        System.exit(0);
    }

}
