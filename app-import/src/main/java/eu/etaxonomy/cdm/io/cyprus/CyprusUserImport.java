/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.cyprus;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.CdmImportBase;

/**
 * @author a.babadshanjan
 * @since 08.01.2009
 */

@Component
public class CyprusUserImport extends CdmImportBase<CyprusImportConfigurator, CyprusImportState> {
    private static final long serialVersionUID = 3941622961913569851L;
    private static final Logger logger = Logger.getLogger(CyprusUserImport.class);

	@Override
	protected boolean isIgnore(CyprusImportState state) {
		return ! state.getConfig().isDoTaxa();
	}

	@Override
	protected boolean doCheck(CyprusImportState state) {
		logger.warn("DoCheck not yet implemented for CyprusExcelImport");
		return true;
	}


	@Override
	protected void doInvoke(CyprusImportState state) {
		this.getAuthenticationManager();

		return;
	}

}
