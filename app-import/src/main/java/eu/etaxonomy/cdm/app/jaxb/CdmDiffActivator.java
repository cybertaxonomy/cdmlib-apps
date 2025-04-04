/**
* Copyright (C) 2008 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.jaxb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;

/**
 * @author a.babadshanjan
 * @since 19.09.2008
 */
public class CdmDiffActivator {

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

	static final ICdmDataSource cdmSourceOne = CdmDestinations.localH2();
	static final ICdmDataSource cdmSourceTwo = CdmDestinations.localH2();

}
