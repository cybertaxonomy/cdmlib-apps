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
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
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
        makeClassification("Gesamtliste", state.getConfig().getClassificationUuid(), "Gesamtliste_ref", RedListUtil.gesamtListeReferenceUuid, state);
        makeClassification("Checkliste", RedListUtil.checkListClassificationUuid, "Checkliste_ref", RedListUtil.checkListReferenceUuid, state);
        makeClassification("E", RedListUtil.uuidClassificationE, "E_ref", RedListUtil.uuidClassificationReferenceE, state);
        makeClassification("W", RedListUtil.uuidClassificationW, "W_ref", RedListUtil.uuidClassificationReferenceW, state);
        makeClassification("K", RedListUtil.uuidClassificationK, "K_ref", RedListUtil.uuidClassificationReferenceK, state);
        makeClassification("AW", RedListUtil.uuidClassificationAW, "AW_ref", RedListUtil.uuidClassificationReferenceAW, state);
        makeClassification("AO", RedListUtil.uuidClassificationAO, "AO_ref", RedListUtil.uuidClassificationReferenceAO, state);
        makeClassification("R", RedListUtil.uuidClassificationR, "R_ref", RedListUtil.uuidClassificationReferenceR, state);
        makeClassification("O", RedListUtil.uuidClassificationO, "O_ref", RedListUtil.uuidClassificationReferenceO, state);
        makeClassification("S", RedListUtil.uuidClassificationS, "S_ref", RedListUtil.uuidClassificationReferenceS, state);
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Classification gesamtListeClassification = getClassificationService().load(state.getConfig().getClassificationUuid());
        Classification checklistClassification = getClassificationService().load(RedListUtil.checkListClassificationUuid);
        Classification classificationE = getClassificationService().load(RedListUtil.uuidClassificationE);
        Classification classificationW = getClassificationService().load(RedListUtil.uuidClassificationW);
        Classification classificationK = getClassificationService().load(RedListUtil.uuidClassificationK);
        Classification classificationAW = getClassificationService().load(RedListUtil.uuidClassificationAW);
        Classification classificationAO = getClassificationService().load(RedListUtil.uuidClassificationAO);
        Classification classificationR = getClassificationService().load(RedListUtil.uuidClassificationR);
        Classification classificationO = getClassificationService().load(RedListUtil.uuidClassificationO);
        Classification classificationS = getClassificationService().load(RedListUtil.uuidClassificationS);
        try {
            while (rs.next()){
                makeSingleTaxonNode(state, rs, gesamtListeClassification, checklistClassification,
                        classificationE, classificationW, classificationK, classificationAW
                        , classificationAO, classificationR, classificationO, classificationS);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        logger.info("Update classification (1000 nodes)");
        getClassificationService().saveOrUpdate(gesamtListeClassification);
        getClassificationService().saveOrUpdate(checklistClassification);
        return true;
    }

    private void makeSingleTaxonNode(RedListGefaesspflanzenImportState state, ResultSet rs,
            Classification gesamtListeClassification, Classification checklistClassification,
            Classification classificationE, Classification classificationW, Classification classificationK,
            Classification classificationAW, Classification classificationAO, Classification classificationR,
            Classification classificationO, Classification classificationS)
            throws SQLException {
        long id = rs.getLong(RedListUtil.NAMNR);
        String parentId = String.valueOf(rs.getLong(RedListUtil.LOWER));
        String gueltString = rs.getString(RedListUtil.GUELT);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);

        //Gesamtliste
        TaxonBase<?> taxonBaseGL = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, String.valueOf(id), TaxonBase.class);
        TaxonBase<?> parentBaseGL = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(parentBaseGL!=null && !parentBaseGL.isInstanceOf(Taxon.class)){
            RedListUtil.logMessage(id, parentBaseGL+" is no taxon but is a parent of "+taxonBaseGL+" (Gesamtliste)", logger);
        }
        Taxon parentGL = (Taxon) state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, parentId, TaxonBase.class);
        createParentChildNodes(gesamtListeClassification, id, gueltString, taxZusatzString, taxonBaseGL, parentGL);

        //Checkliste
        TaxonBase<?> taxonBaseCL = state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, String.valueOf(id), TaxonBase.class);
        TaxonBase<?> parentBaseCL = state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(parentBaseCL!=null && !parentBaseCL.isInstanceOf(Taxon.class)){
            RedListUtil.logMessage(id, parentBaseCL+" is no taxon but is a parent of "+taxonBaseCL+" (Checkliste)", logger);
        }
        Taxon parentCL = (Taxon) state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(taxonBaseCL!=null){//null check necessary because not all taxa exist in the checklist
            createParentChildNodes(checklistClassification, id, gueltString, taxZusatzString, taxonBaseCL, parentCL);
        }

        if(taxonBaseGL!= null && taxonBaseCL!=null
                && taxonBaseGL.getUuid().equals(taxonBaseCL.getUuid())){
            RedListUtil.logMessage(id, "Same UUID for "+taxonBaseGL+ " (Gesamtliste) and "+taxonBaseCL+" (Checkliste)", logger);
        }
        if(parentGL!=null && parentCL!=null && parentGL.getUuid().equals(parentCL.getUuid())){
            RedListUtil.logMessage(id, "Same UUID for "+parentGL+ " (Gesamtliste) and "+parentCL+" (Checkliste)", logger);
        }

        //add taxa for concept relationships to E, W, K, AW, AO, R, O, S
        addTaxonToClassification(classificationE, RedListUtil.CLASSIFICATION_NAMESPACE_E, id, state);
        addTaxonToClassification(classificationW, RedListUtil.CLASSIFICATION_NAMESPACE_W, id, state);
        addTaxonToClassification(classificationK, RedListUtil.CLASSIFICATION_NAMESPACE_K, id, state);
        addTaxonToClassification(classificationAW, RedListUtil.CLASSIFICATION_NAMESPACE_AW, id, state);
        addTaxonToClassification(classificationAO, RedListUtil.CLASSIFICATION_NAMESPACE_AO, id, state);
        addTaxonToClassification(classificationR, RedListUtil.CLASSIFICATION_NAMESPACE_R, id, state);
        addTaxonToClassification(classificationO, RedListUtil.CLASSIFICATION_NAMESPACE_O, id, state);
        addTaxonToClassification(classificationS, RedListUtil.CLASSIFICATION_NAMESPACE_S, id, state);
    }

    private void addTaxonToClassification(Classification classification, String classificationNamespace, long id, RedListGefaesspflanzenImportState state){
        Taxon taxon = HibernateProxyHelper.deproxy(state.getRelatedObject(classificationNamespace, String.valueOf(id), TaxonBase.class), Taxon.class);
        classification.addChildTaxon(taxon, null, null);
    }

    private void createParentChildNodes(Classification classification, long id, String gueltString,
            String taxZusatzString, TaxonBase<?> taxonBase, Taxon parent) {
        if(taxonBase==null){
            RedListUtil.logMessage(id, "child taxon/synonym of "+parent+"  is null. ("+classification.generateTitle()+")" , logger);
            return;
        }
        //taxon
        if(taxonBase.isInstanceOf(Taxon.class)){
            //misapplied name
            String appendedPhrase = taxonBase.getAppendedPhrase();
            if(appendedPhrase!=null && appendedPhrase.equals(RedListUtil.AUCT)){
                if(parent==null){
                    RedListUtil.logMessage(id, "parent taxon of misapplied name "+taxonBase+"  is null. ("+classification.getTitleCache()+")" , logger);
                    return;
                }
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
        //synonym
        else if(taxonBase.isInstanceOf(Synonym.class)){
            if(parent==null){
                RedListUtil.logMessage(id, "parent taxon of synonym "+taxonBase+"  is null. ("+classification.getTitleCache()+")" , logger);
                return;
            }
            //basionym
            if(gueltString.equals(RedListUtil.GUELT_BASIONYM)){
                parent.addHomotypicSynonym((Synonym) taxonBase, null, null);
                parent.getName().addBasionym(taxonBase.getName());
            }
            //regular synonym
            else{
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
                        RedListUtil.logMessage(id, "unknown value "+taxZusatzString+" for column "+RedListUtil.TAX_ZUSATZ, logger);
                    }
                }
            }
        }
        //set sec reference
        taxonBase.setSec(classification.getReference());
        taxonBase.setTitleCache(null, false);//refresh title cache
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
        Map<String, TaxonBase> taxonMapGesamtListe = (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE);
        result.put(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, taxonMapGesamtListe);
        Map<String, TaxonBase> taxonMapCheckliste = (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.TAXON_CHECKLISTE_NAMESPACE);
        result.put(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, taxonMapCheckliste);
        return result;
    }

    private void makeClassification(String classificationName, UUID classificationUuid, String referenceName, UUID referenceUuid, RedListGefaesspflanzenImportState state) {
        Classification classification = Classification.NewInstance(classificationName, Language.DEFAULT());
        classification.setUuid(classificationUuid);
        Reference gesamtListeReference = ReferenceFactory.newGeneric();
        gesamtListeReference.setTitle(referenceName);
        gesamtListeReference.setUuid(referenceUuid);
        classification.setReference(gesamtListeReference);
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
