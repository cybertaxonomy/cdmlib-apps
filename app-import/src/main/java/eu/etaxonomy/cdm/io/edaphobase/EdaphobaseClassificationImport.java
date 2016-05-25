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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @date 21.12.2015
 *
 */
@Component
public class EdaphobaseClassificationImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = -9138378836474086070L;

    private static final Logger logger = Logger.getLogger(EdaphobaseClassificationImport.class);

    private static final String tableName = "tax_taxon";

    private static final String pluralString = "taxon relationships";


    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseClassificationImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return "SELECT taxon_id FROM "
                + " (SELECT DISTINCT taxon_id, length(path_to_root) FROM tax_taxon t "
                + " ORDER BY length(path_to_root), taxon_id) as drvTbl ";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = "SELECT DISTINCT t.* "
                + " FROM tax_taxon t"
                + " WHERE taxon_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(EdaphobaseImportState state) {
        makeClassification(state);
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Map<String, Classification> map = partitioner.getObjectMap(CLASSIFICATION_NAMESPACE);
        Classification classification = map.get(state.getConfig().getClassificationUuid().toString());
        Reference sourceReference = state.getTransactionalSourceReference();

        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                int id = rs.getInt("taxon_id");
                 //parentTaxonFk
                boolean isValid = rs.getBoolean("valid");
//                boolean idDeleted = rs.getBoolean("deleted");
//                String treeIndex = rs.getString("path_to_root");
//                Integer rankFk = rs.getInt("tax_rank_fk");
//                String officialRemark = rs.getString("official_remark");
//                boolean isGroup = rs.getBoolean("taxonomic_group");
                Integer parentTaxonFk = nullSafeInt(rs, "parent_taxon_fk");

                if (parentTaxonFk != null){
                    TaxonBase<?> parent = state.getRelatedObject(TAXON_NAMESPACE, parentTaxonFk.toString(), TaxonBase.class);
                    if (parent == null){
                        logger.warn("Parent taxon " + parentTaxonFk + " not found for taxon " + id );
                    }else{

                        TaxonNameBase<?,?> parentName = parent.getName();

                        TaxonBase<?> child = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(id), TaxonBase.class);
//                        TaxonNameBase<?,?> childName = child.getName();

//                        handleMissingNameParts(CdmBase.deproxy(childName, NonViralName.class), CdmBase.deproxy(parentName, NonViralName.class));

                        if (isValid){
                            if (parent.isInstanceOf(Synonym.class)){
                                logger.warn("Parent taxon (" + parentTaxonFk + " is not valid for valid child " + id);
                            }else{
                                Taxon accParent = CdmBase.deproxy(parent, Taxon.class);
                                classification.addParentChild(accParent, (Taxon)child, sourceReference, null);
                                taxaToSave.add(accParent);
                            }
                        }else{
//                            Synonym synonym = CdmBase.deproxy(child, Synonym.class);
//                            if (synonym == null){
//                                logger.warn("Synonym " + id + " not found for taxon ");
//                            }
//                            if(parent.isInstanceOf(Synonym.class)){
//                                String message = "Taxon ("+parentTaxonFk+") is not accepted but synonym. Can't add synonym ("+id+")";
//                                logger.warn(message);
//                            }else{
//                                Taxon accepted = CdmBase.deproxy(parent, Taxon.class);
////                                accepted.addSynonym(synonym, SynonymRelationshipType.SYNONYM_OF());
//                                taxaToSave.add(accepted);
//                            }
                        }
                    }
                }

//              //id
//              String nameSpace = "tax_taxon";
//              ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, nameSpace);
//              ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, nameSpace);


            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getTaxonService().saveOrUpdate(taxaToSave);
        return true;
    }

//    /**
//     * @param childName
//     * @param parentName
//     */
//    private void handleMissingNameParts(NonViralName<?> childName, NonViralName<?> parentName) {
//        if (childName.getGenusOrUninomial())
//    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        String nameSpace;
        Class<?> cdmClass;
        Set<String> idSet;
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        try{
            Set<String> taxonIdSet = new HashSet<String>();
            while (rs.next()){
                handleForeignKey(rs, taxonIdSet, "taxon_id");
                handleForeignKey(rs, taxonIdSet, "parent_taxon_fk");
            }

            //name map
            nameSpace = TAXON_NAMESPACE;
            cdmClass = TaxonBase.class;
            idSet = taxonIdSet;
            Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
            result.put(nameSpace, taxonMap);

            //Classification
            Map<String, Classification> classificationMap = new HashMap<>();
            UUID classificationUuid = state.getConfig().getClassificationUuid();
            Classification classification = getClassificationService().find(state.getConfig().getClassificationUuid());
            classificationMap.put(classificationUuid.toString(), classification);
            result.put(CLASSIFICATION_NAMESPACE, classificationMap);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    /**
     * @param state
     */
    private void makeClassification(EdaphobaseImportState state) {
        Classification classification = Classification.NewInstance(state.getConfig().getClassificationName());
        classification.setUuid(state.getConfig().getClassificationUuid());
        getClassificationService().save(classification);
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
