/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.out;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportBase;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @since 20.03.2008
 * <IExportConfigurator>
 */
public abstract class BerlinModelExportBase<T extends CdmBase>
        extends DbExportBase<BerlinModelExportConfigurator, BerlinModelExportState, IExportTransformer> {

    private static final long serialVersionUID = 5512623147969534945L;
    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	public BerlinModelExportBase() {
		super();
	}
}
