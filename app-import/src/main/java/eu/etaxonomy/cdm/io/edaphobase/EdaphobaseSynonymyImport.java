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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * Import class for synonym relationships in Edaphobase.
 *
 * @author a.mueller
 * @since 21.12.2015
 */
@Component
public class EdaphobaseSynonymyImport extends EdaphobaseImportBase {

    private static final long serialVersionUID = 6641343927320994726L;
    private static final Logger logger = LogManager.getLogger();

    private static final String tableName = "tax_synonym";
    private static final String pluralString = "related synonyms";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseSynonymyImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return "    SELECT sr.tax_synonym_id  "
                + " FROM tax_synonym sr "
                + "  INNER JOIN tax_taxon s ON s.taxon_id = sr.a_taxon_fk_taxon_id "
                + "  INNER JOIN tax_taxon t ON t.taxon_id = sr.b_taxon_fk_taxon_id "
                + " WHERE  s.valid = false AND t.valid = true AND sr.synonym_role = 11614 "
                + " ORDER BY sr.a_taxon_fk_taxon_id ";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = "SELECT sr.* "
                + " FROM tax_synonym sr"
                + " WHERE tax_synonym_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, EdaphobaseImportState state) {

        ResultSet rs = partitioner.getResultSet();
        Reference sourceReference = state.getTransactionalSourceReference();

        @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                int id = rs.getInt("tax_synonym_id");
                 //parentTaxonFk
//                boolean isValid = rs.getBoolean("valid");
                Integer synId = nullSafeInt(rs, "a_taxon_fk_taxon_id");
                Integer accId = nullSafeInt(rs, "b_taxon_fk_taxon_id");


                if (synId == null || accId == null){
                    logger.warn("Either a_taxon or b_taxon is NULL for tax_synonym " + id);
                }else{
                    TaxonBase<?> synonymCandidate = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(synId), TaxonBase.class);
                    if (synonymCandidate == null){
                        logger.warn("Synonym " + synId + " not found for synonymRelations (tax_synonym): " + id);
                    }else if (synonymCandidate.isInstanceOf(Taxon.class)){
                        String message = "Synonym ("+synId+") is not synonym but accepted (valid). Can't add synonym for tax_synonym: "+id;
                        logger.warn(message);
                    }else{
                        Synonym synonym = CdmBase.deproxy(synonymCandidate, Synonym.class);
                        TaxonBase<?> accepted = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(accId), TaxonBase.class);
                        if (accepted == null){
                            logger.warn("Accepted(parent) taxon " + accId + " not found for tax_synonym " + id );
                        }else if(accepted.isInstanceOf(Synonym.class)){
                            String message = "Taxon ("+accId+") is not accepted but synonym. Can't add synonym for tax_synonym: "+id;
                            logger.warn(message);
                        }else{
                            Taxon taxon = CdmBase.deproxy(accepted, Taxon.class);
                            if (synonym.getAcceptedTaxon()!= null){
                                String message = "Synonym ("+synId+") already has an accepted taxon. Have to clone synonym. RelId: " + id;
                                logger.warn(message);
                                synonym = synonym.clone();
                            }
                            taxon.addSynonym(synonym, SynonymType.SYNONYM_OF());
                            state.addSynonymWithAcceptedTaxon(synId);
                            taxaToSave.add(synonym);
                            taxaToSave.add(taxon);
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

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {

        String nameSpace;
        Set<String> idSet;
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        try{
            Set<String> taxonIdSet = new HashSet<>();
            while (rs.next()){
                handleForeignKey(rs, taxonIdSet, "a_taxon_fk_taxon_id");
                handleForeignKey(rs, taxonIdSet, "b_taxon_fk_taxon_id");
            }

            //name map
            nameSpace = TAXON_NAMESPACE;
            idSet = taxonIdSet;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
            result.put(nameSpace, taxonMap);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoSynonyms();
    }

}
