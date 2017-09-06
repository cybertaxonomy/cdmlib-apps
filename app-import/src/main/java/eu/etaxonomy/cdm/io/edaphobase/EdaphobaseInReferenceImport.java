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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @date 18.12.2015
 *
 */
@Component
public class EdaphobaseInReferenceImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = 6895687693249076160L;

    private static final Logger logger = Logger.getLogger(EdaphobaseInReferenceImport.class);

    private static final String tableName = "lit_document";

    private static final String pluralString = "documents";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseInReferenceImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return    " SELECT DISTINCT document_id "
                + " FROM lit_document ld INNER JOIN tax_taxon t ON t.tax_document = ld.document_id "
                + " WHERE ld.parent_document_fk_document_id IS NOT NULL "
                + " ORDER BY document_id ";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = " SELECT document_id, parent_document_fk_document_id "
                + " FROM lit_document ld "
                + " WHERE ld.document_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Set<Reference> referencesToSave = new HashSet<>();
        try {
            while (rs.next()){
                handleSingleReference(state, rs, referencesToSave);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getReferenceService().saveOrUpdate(referencesToSave);
        return true;
    }

    /**
     * @param state
     * @param rs
     * @param referencesToSave
     * @throws SQLException
     */
    private void handleSingleReference(EdaphobaseImportState state, ResultSet rs, Set<Reference> referencesToSave) throws SQLException {
        Integer id = rs.getInt("document_id");
        Integer parentId = rs.getInt("parent_document_fk_document_id");
//        Integer documentType = nullSafeInt(rs, "document_type");

        Reference child = state.getRelatedObject(REFERENCE_NAMESPACE, String.valueOf(id), Reference.class);
        Reference parent = state.getRelatedObject(REFERENCE_NAMESPACE, String.valueOf(parentId), Reference.class);
        if (child == null){
            logger.warn("Child reference for document_id " + id + " is NULL" );
        }else if (parent == null){
            logger.warn("Parent reference for document_type_fk_document_type_id " + parentId + " is NULL" );
        }else{
            child.setInReference(parent);
            referencesToSave.add(child);
        }
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        Set<String> referenceIdSet = new HashSet<String>();

        try {
            while (rs.next()){
                handleForeignKey(rs, referenceIdSet, "document_id");
                handleForeignKey(rs, referenceIdSet, "parent_document_fk_document_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //reference map
        String nameSpace = REFERENCE_NAMESPACE;
        Class<?> cdmClass = Reference.class;
        Set<String> idSet = referenceIdSet;
        Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
        result.put(nameSpace, referenceMap);

        return result;
    }



    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoReferences();
    }

}
