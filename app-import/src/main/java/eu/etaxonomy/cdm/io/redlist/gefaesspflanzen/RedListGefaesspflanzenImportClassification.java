/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationship;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportClassification extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportClassification.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "classifications";

    public RedListGefaesspflanzenImportClassification() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(RedListGefaesspflanzenImportState state) {
        return "SELECT NAMNR "
                + "FROM V_TAXATLAS_D20_EXPORT t "
                + " ORDER BY NAMNR";
    }

    @Override
    protected String getRecordQuery(RedListGefaesspflanzenImportConfigurator config) {
        String result = " SELECT * "
                + " FROM V_TAXATLAS_D20_EXPORT t "
                + " WHERE t.NAMNR IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        makeClassification(state);
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Classification classification = getClassificationService().load(state.getConfig().getClassificationUuid());
        try {
            while (rs.next()){
                makeSingleTaxonNode(state, rs, classification);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        logger.info("Update classification (1000 nodes)");
        getClassificationService().saveOrUpdate(classification);
        return true;
    }

    private void makeSingleTaxonNode(RedListGefaesspflanzenImportState state, ResultSet rs, Classification classification)
            throws SQLException {
        long id = rs.getLong(RedListUtil.NAMNR);
        String parentId = String.valueOf(rs.getLong(RedListUtil.LOWER));
        String gueltString = rs.getString(RedListUtil.GUELT);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);

        TaxonBase taxonBase = state.getRelatedObject(RedListUtil.TAXON_NAMESPACE, String.valueOf(id), TaxonBase.class);
        Taxon parent = (Taxon) state.getRelatedObject(RedListUtil.TAXON_NAMESPACE, parentId, TaxonBase.class);

        //taxon
        if(taxonBase.isInstanceOf(Taxon.class)){
            //misapplied name
            String appendedPhrase = taxonBase.getName().getAppendedPhrase();
            if(appendedPhrase!=null && appendedPhrase.contains(RedListUtil.AUCT)){
                parent.addMisappliedName((Taxon) taxonBase, null, null);
            }
            else{
                classification.addParentChild(parent, (Taxon)taxonBase, null, null);
            }

            if(CdmUtils.isNotBlank(taxZusatzString)){
                if(taxZusatzString.trim().equals("p. p.")){
                    RedListUtil.logMessage(id, "pro parte for accepted taxon "+taxonBase, logger);
                }
            }
        }
        else if(taxonBase.isInstanceOf(Synonym.class)){
            //basionym
            if(gueltString.equals(RedListUtil.GUELT_BASIONYM)){
                parent.addHomotypicSynonym((Synonym) taxonBase, null, null);
                parent.getName().addBasionym(taxonBase.getName());
            }
            //regular synonym
            else{
                //TODO: how to correctly add a synonym?
                SynonymRelationship synonymRelationship = parent.addSynonym((Synonym) taxonBase, SynonymRelationshipType.HETEROTYPIC_SYNONYM_OF(), null, null);

                //TAX_ZUSATZ
                if(CdmUtils.isNotBlank(taxZusatzString)){
                    if(taxZusatzString.trim().equals("p. p.")){
                        synonymRelationship.setProParte(true);
                    }
                    else if(taxZusatzString.trim().equals("s. l. p. p.")){
                        synonymRelationship.setProParte(true);
                        taxonBase.setAppendedPhrase("s. l.");
                    }
                    else if(taxZusatzString.trim().equals("s. str. p. p.")){
                        synonymRelationship.setProParte(true);
                        taxonBase.setAppendedPhrase("s. str.");
                    }
                    else if(taxZusatzString.trim().equals("s. l.")
                            || taxZusatzString.trim().equals("s. str.")){
                        taxonBase.setAppendedPhrase(taxZusatzString);
                    }
                    else{
                        RedListUtil.logMessage(id, "unknown value for column "+RedListUtil.TAX_ZUSATZ, logger);
                    }
                }
            }
        }
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        Set<String> idSet = new HashSet<String>();
        try {
            while (rs.next()){
                idSet.add(String.valueOf(rs.getLong(RedListUtil.NAMNR)));
                idSet.add(String.valueOf(rs.getLong(RedListUtil.LOWER)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.TAXON_NAMESPACE);
        result.put(RedListUtil.TAXON_NAMESPACE, taxonMap);
        return result;
    }

    private void makeClassification(RedListGefaesspflanzenImportState state) {
        Classification classification = Classification.NewInstance(state.getConfig().getClassificationName());
        classification.setUuid(state.getConfig().getClassificationUuid());
        getClassificationService().save(classification);
    }

    @Override
    protected boolean doCheck(RedListGefaesspflanzenImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(RedListGefaesspflanzenImportState state) {
        return false;
    }

}
