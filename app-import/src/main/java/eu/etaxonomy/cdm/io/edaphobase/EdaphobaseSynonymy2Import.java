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
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * Import class for synonym relationships in edaphobase.
 *
 * @author a.mueller
 * @date 21.12.2015
 *
 */
@Component
public class EdaphobaseSynonymy2Import extends EdaphobaseImportBase {
    private static final long serialVersionUID = 8968205268798472136L;
    private static final Logger logger = Logger.getLogger(EdaphobaseSynonymy2Import.class);

    private static final String tableName = "tax_synonym";
    private static final String pluralString = "related unreal synonyms";


    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseSynonymy2Import() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return "    SELECT sr.tax_synonym_id  "
                + " FROM tax_synonym sr "
                + "  INNER JOIN tax_taxon s ON s.taxon_id = sr.a_taxon_fk_taxon_id "
                + "  INNER JOIN tax_taxon t ON t.taxon_id = sr.b_taxon_fk_taxon_id "
                + " WHERE  NOT (s.valid = false AND t.valid = true) OR sr.synonym_role <> 11614 "
                + " ORDER BY sr.a_taxon_fk_taxon_id ";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = "SELECT sr.* , t2.valid t2valid, t3.valid t3valid, t2.taxon_id t2taxon_id, t3.taxon_id t3taxon_id "

                + " FROM tax_taxon s "
                + "  INNER JOIN tax_synonym sr ON s.taxon_id = sr.a_taxon_fk_taxon_id "
                + "  INNER JOIN tax_taxon t ON t.taxon_id =    sr.b_taxon_fk_taxon_id "
                + "  LEFT JOIN tax_synonym sr2 ON t.taxon_id = sr2.a_taxon_fk_taxon_id "
                + "  LEFT JOIN tax_taxon t2 ON t2.taxon_id =    sr2.b_taxon_fk_taxon_id "
                + "  LEFT JOIN tax_synonym sr3 ON t2.taxon_id = sr3.a_taxon_fk_taxon_id"
                + "  LEFT JOIN tax_taxon t3 ON t3.taxon_id =    sr3.b_taxon_fk_taxon_id "
                + " WHERE sr.tax_synonym_id IN (@IDSET)"
                + " ORDER BY sr.tax_synonym_id ";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Map<String, TaxonBase> map = partitioner.getObjectMap(TAXON_NAMESPACE);
        Reference sourceReference = state.getTransactionalSourceReference();

        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                int id = rs.getInt("tax_synonym_id");
                 //parentTaxonFk
//                boolean isValid = rs.getBoolean("valid");
                Integer synId = nullSafeInt(rs, "a_taxon_fk_taxon_id");
                Integer accId = nullSafeInt(rs, "b_taxon_fk_taxon_id");

                Integer role = nullSafeInt(rs, "synonym_role");
                boolean isSynonym = role.equals(11614);

                if (synId == null || accId == null){
                    logger.warn("Either a_taxon or b_taxon is NULL for tax_synonym " + id);
                }else{
                    TaxonBase<?> synonymCandidate = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(synId), TaxonBase.class);
                    if (synonymCandidate == null){
                        logger.warn("Synonym " + synId + " not found for synonymRelations (tax_synonym): " + id);
                    }else if (isSynonym && synonymCandidate.isInstanceOf(Taxon.class)){
                        String message = "Synonym ("+synId+") is not synonym but accepted (valid). Can't add synonym for tax_synonym: "+id;
                        logger.warn(message);
                    }else if (!isSynonym && !synonymCandidate.isInstanceOf(Taxon.class)){
                        String message = "From taxon  ("+synId+") in included in relation is not of type Taxon but Synonym (invalid). Can't add concept relation for tax_synonym: "+id;
                        logger.warn(message);
                    }else if(isSynonym){
                        handleSynonymToInvalid(state, rs, taxaToSave, synId, accId, synonymCandidate);
                    }else{
                       handleConceptRelationship(state, taxaToSave, id, accId, role, synonymCandidate);
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

    /**
     * @param state
     * @param taxaToSave
     * @param id
     * @param accId
     * @param role
     * @param synonymCandidate
     */
    private void handleConceptRelationship(EdaphobaseImportState state, Set<TaxonBase> taxaToSave, int id,
                Integer accId, Integer role, TaxonBase<?> fromTaxonBase) {
           if (!role.equals(11613)){
               String message = "Concept relation is not of type is included in ('11613'). This is not expected here. tax_synonym: " + id;
               logger.warn(message);
           }else{
               Taxon fromTaxon = CdmBase.deproxy(fromTaxonBase, Taxon.class);
               TaxonBase<?> toTaxonBase = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(accId), TaxonBase.class);
               if (toTaxonBase == null){
                   logger.warn("Accepted(parent) taxon " + accId + " not found for tax_synonym " + id );
               }else if(toTaxonBase.isInstanceOf(Synonym.class)){
                   String message = "Taxon ("+accId+") is not accepted but synonym. Can't add concept relation for tax_synonym: "+id;
                   logger.warn(message);
               }else{
                   Taxon toTaxon = CdmBase.deproxy(toTaxonBase, Taxon.class);
                   TaxonRelationshipType relType = TaxonRelationshipType.INCLUDES();
                   toTaxon.addTaxonRelation(fromTaxon, relType, null, null);
                   taxaToSave.add(fromTaxon);
                   taxaToSave.add(toTaxon);
               }
           }
    }

    /**
     * @param state
     * @param taxaToSave
     * @param synId
     * @param accId
     * @param synonymCandidate
     * @throws SQLException
     */
    private void handleSynonymToInvalid(EdaphobaseImportState state, ResultSet rs, Set<TaxonBase> taxaToSave, Integer synId,
            Integer accId, TaxonBase<?> synonymCandidate) throws SQLException {
        Synonym synonym = CdmBase.deproxy(synonymCandidate, Synonym.class);
        if (state.hasAcceptedTaxon(synId)){
            //TODO do some further homotypie checking
        }else{
            String message = "Synonym a("+synId+") of invalid taxon (b) has not accepted taxon yet. Automatically create relationship to valid taxon (c) of invalid taxon (b) ";
            logger.warn(message);
            Integer acc3 = nullSafeInt(rs, "t3taxon_id");
            Boolean valid3 = nullSafeBoolean(rs, "t3valid");
            Integer acc2 = nullSafeInt(rs, "t2taxon_id");
            Boolean valid2 = nullSafeBoolean(rs, "t2valid");

            Integer accC  = acc2;
            if (valid2 == null || valid2 == false){
                accC = acc3;
                if (valid3 == null || valid3 == false){
                    logger.warn("All unvalid not handled: " + synId);
                }
            }
            TaxonBase<?> acceptedC = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(accC), TaxonBase.class);
            if (acceptedC == null || acceptedC.isInstanceOf(Synonym.class)){
                logger.warn("Taxon c does not exist or is not valid taxon.");
            }else{
                Taxon taxon = CdmBase.deproxy(acceptedC, Taxon.class);
                //TODO
                SynonymType synType = SynonymType.SYNONYM_OF();
                taxon.addSynonym(synonym, synType);
                taxaToSave.add(synonym);
                taxaToSave.add(taxon);
            }
        }
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        String nameSpace;
        Class<?> cdmClass;
        Set<String> idSet;
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        try{
            Set<String> taxonIdSet = new HashSet<>();
            while (rs.next()){
                handleForeignKey(rs, taxonIdSet, "a_taxon_fk_taxon_id");
                handleForeignKey(rs, taxonIdSet, "b_taxon_fk_taxon_id");
                handleForeignKey(rs, taxonIdSet, "t2taxon_id");
                handleForeignKey(rs, taxonIdSet, "t3taxon_id");
            }

            //name map
            nameSpace = TAXON_NAMESPACE;
            cdmClass = TaxonBase.class;
            idSet = taxonIdSet;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
            result.put(nameSpace, taxonMap);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoSynonyms();
    }

}
