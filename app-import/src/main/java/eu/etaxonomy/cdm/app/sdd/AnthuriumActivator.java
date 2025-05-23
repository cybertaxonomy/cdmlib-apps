/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.app.sdd;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.sdd.in.SDDImportConfigurator;

/**
 * @author h.fradin
 * @since 24.10.2008
 */
public class AnthuriumActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI sddSource = SDDSources.tdwg_expert();
	//static final String sddSource = SDDSources.viola_local_andreas();
	//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_andreasM2();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();

	//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_portal_test_localhost();

	static final UUID secUuid = UUID.fromString("fc98e890-e487-4664-ac9b-8a60fda6244c");
	static final String sourceSecId = "viola_pub_ed_999999";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start import from SDD("+ sddSource.toString() + ") ...");

		//make Source
		URI source;
		try {
			source = sddSource;
			ICdmDataSource destination = cdmDestination;

			SDDImportConfigurator sddImportConfigurator = SDDImportConfigurator.NewInstance(source,  destination);

			sddImportConfigurator.setSecUuid(secUuid);
			sddImportConfigurator.setSourceSecId(sourceSecId);

			sddImportConfigurator.setCheck(check);
			sddImportConfigurator.setDbSchemaValidation(hbm2dll);

			// invoke import
			CdmDefaultImport<SDDImportConfigurator> sddImport = new CdmDefaultImport<SDDImportConfigurator>();
			sddImport.invoke(sddImportConfigurator);

			System.out.println("End import from SDD ("+ source.toString() + ")...");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


}
