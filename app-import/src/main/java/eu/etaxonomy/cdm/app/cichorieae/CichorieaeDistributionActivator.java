/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.app.cichorieae;

import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.tasks.TestDatabase;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.excel.distribution.DistributionImportConfigurator;

/**
 * @author a.babadshanjan
 * @since 31.10.2008
 */
public class CichorieaeDistributionActivator {

    private static final Logger logger = LogManager.getLogger();

    private static final String dbName = "cdm_test_anahit";
	private static String fileName =
		new String( System.getProperty("user.home") + "\\workspace\\cdmlibNew\\app-import\\src\\main\\resources\\distribution\\distribution_cich.xls");
//	private static String fileName = new String( System.getProperty("user.home") + File.separator + "Africa plus x.xls");

	private static final ICdmDataSource destinationDb = TestDatabase.CDM_DB(dbName);

    public static void main(String[] args) {

    	DistributionImportConfigurator distributionImportConfigurator;
		try {
			distributionImportConfigurator = DistributionImportConfigurator.NewInstance(new URI(fileName), destinationDb);

			CdmDefaultImport<DistributionImportConfigurator> distributionImport =
				new CdmDefaultImport<DistributionImportConfigurator>();

			// invoke import
			logger.debug("Invoking Cichorieae distribution import");
			distributionImport.invoke(distributionImportConfigurator);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

    }

}
