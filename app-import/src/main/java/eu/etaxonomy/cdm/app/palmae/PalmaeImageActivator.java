/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.palmae;

import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.images.ImageImportConfigurator;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.PalmaeImageImport;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;

/**
 * TODO not working at the moment
 *
 * @author n.hoffmann
 * @since 18.11.2008
 */
public class PalmaeImageActivator  {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	public static final String sourceFolderString = "\\\\Media\\EditWP6\\palmae\\photos\\new";
	private static final ICdmDataSource cdmDestination = CdmDestinations.localH2Palmae();


	// set the webserver path to the images
	private static final String urlString = "http://media.e-taxonomy.eu/palmae/photos/";

	static final UUID secUuid = UUID.fromString("5f32b8af-0c97-48ac-8d33-6099ed68c625");

	public static void main (String[] cowabunga){
		URI uri;
		try {
			uri = new URI(sourceFolderString);
			ImageImportConfigurator imageConfigurator = ImageImportConfigurator.NewInstance(uri, cdmDestination, urlString, PalmaeImageImport.class);
			imageConfigurator.setSecUuid(secUuid);

			CdmDefaultImport<IImportConfigurator> importer = new CdmDefaultImport<>();
			//AbstractImageImporter imageImporter = new PalmaeImageActivator();
			importer.invoke(imageConfigurator);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

}
