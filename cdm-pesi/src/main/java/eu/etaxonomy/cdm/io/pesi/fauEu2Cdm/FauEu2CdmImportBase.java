/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.fauEu2Cdm;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.io.common.CdmImportBase;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public abstract class FauEu2CdmImportBase
        extends CdmImportBase<FauEu2CdmImportConfigurator, FauEu2CdmImportState> {

    private static final long serialVersionUID = 8917991155285743172L;

    protected ICdmRepository source(FauEu2CdmImportState state){
        ICdmRepository repo = state.getSourceRepository();
        if (repo == null){
            System.out.println("start source repo");
            boolean omitTermLoading = true;
            repo = CdmApplicationController.NewInstance(state.getConfig().getSource(),
                    DbSchemaValidation.VALIDATE, omitTermLoading);
            state.setSourceRepository(repo);
            System.out.println("end source repo");
        }
        return repo;
    }

}
