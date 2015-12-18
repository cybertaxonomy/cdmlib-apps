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

import java.sql.ResultSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @date 18.12.2015
 *
 */
@Component
public class EdaphobaseTaxonImport extends EdaphobaseImportBase {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseTaxonImport.class);

    private static final String tableName = "tax_taxon";

    private static final String pluralString = "taxa";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseTaxonImport() {
        super(tableName, pluralString);
    }

    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see eu.etaxonomy.cdm.io.common.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet, eu.etaxonomy.cdm.io.common.IPartitionedState)
     */
    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoTaxa();
    }

}
