/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;

/**
 * @author a.mueller
 * @date 18.12.2015
 *
 */
public abstract class EdaphobaseImportBase
        extends DbImportBase<EdaphobaseImportState, EdaphobaseImportConfigurator>  implements ICdmIO<EdaphobaseImportState>, IPartitionedIO<EdaphobaseImportState> {
    private static final long serialVersionUID = 3496726181873011520L;

    protected static final String TAXON_NAMESPACE = "tax_taxon";
    protected static final String CLASSIFICATION_NAMESPACE = "Classification";
    protected static final String REFERENCE_NAMESPACE = "lit_document";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseImportBase(String tableName, String pluralString) {
        super(tableName, pluralString);
    }

}
