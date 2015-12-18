// $Id$
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

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseImportBase(String tableName, String pluralString) {
        super(tableName, pluralString);
    }

}
