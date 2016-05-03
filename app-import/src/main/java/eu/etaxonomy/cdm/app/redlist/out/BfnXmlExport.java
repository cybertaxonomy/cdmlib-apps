/**
 * Copyright (C) 2008 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 */

package eu.etaxonomy.cdm.app.redlist.out;

import java.io.File;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.sdd.ViolaExportActivator;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.redlist.bfnXml.out.BfnXmlExportConfigurator;
import eu.etaxonomy.cdm.io.redlist.bfnXml.out.BfnXmlExportTransformer;

/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public class BfnXmlExport {

	private static final Logger logger = Logger.getLogger(ViolaExportActivator.class);

	private void invokeExport(String[] args) {

		ICdmDataSource sourceDb = CdmDestinations.cdm_test_local_mysql();
		BfnXmlExportTransformer transformer = new BfnXmlExportTransformer();
		File destination = new File("/home/pplitzner/Rote Listen 2020/doctronic/export/export.xml");
		BfnXmlExportConfigurator config = BfnXmlExportConfigurator.NewInstance(destination, sourceDb, transformer);

		CdmDefaultExport<BfnXmlExportConfigurator> export =
			new CdmDefaultExport<BfnXmlExportConfigurator>();

		// invoke export
		logger.debug("Invoking BfnXml export");
		export.invoke(config);

	}

	public static void main(String[] args) {
		BfnXmlExport export = new BfnXmlExport();
		export.invokeExport(args);
	}

}
