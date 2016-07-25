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
import java.util.Arrays;
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
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationship;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

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
        String result = "select e.*, f.FAMILIE "
                + "from V_TAXATLAS_D20_EXPORT e, GATTUNG_FAMILIE f "
                + "where e.EPI1 = f.GATTUNG and e.NAMNR IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        Classification gesamtListe = makeClassification("Gesamtliste", state.getConfig().getClassificationUuid(), "Gesamtliste", null, RedListUtil.gesamtListeReferenceUuid, state);
        Classification checkliste = makeClassification("Checkliste", RedListUtil.checkListClassificationUuid, "Checkliste", null, RedListUtil.checkListReferenceUuid, state);
        makeClassification("E", RedListUtil.uuidClassificationE, "Ehrendorfer", null, RedListUtil.uuidClassificationReferenceE, state);
        makeClassification("W", RedListUtil.uuidClassificationW, "Wisskirchen (Standardliste)", 1998, RedListUtil.uuidClassificationReferenceW, state);
        makeClassification("K", RedListUtil.uuidClassificationK, "Korneck (Rote Liste)", 1996, RedListUtil.uuidClassificationReferenceK, state);
        makeClassification("AW", RedListUtil.uuidClassificationAW, "Atlas (Westdeutschland)", 1988, RedListUtil.uuidClassificationReferenceAW, state);
        makeClassification("AO", RedListUtil.uuidClassificationAO, "Atlas (Ostdeutschland)", 1996, RedListUtil.uuidClassificationReferenceAO, state);
        makeClassification("R", RedListUtil.uuidClassificationR, "Rothmaler", 2011, RedListUtil.uuidClassificationReferenceR, state);
        makeClassification("O", RedListUtil.uuidClassificationO, "Oberdorfer", 2001, RedListUtil.uuidClassificationReferenceO, state);
        makeClassification("S", RedListUtil.uuidClassificationS, "Schmeil-Fitschen", 2011, RedListUtil.uuidClassificationReferenceS, state);
        importFamilies(gesamtListe, checkliste, state);
        super.doInvoke(state);
    }


    private void importFamilies(Classification gesamtListe, Classification checkliste, RedListGefaesspflanzenImportState state) {
        for(UUID uuid:state.getFamilyMap().values()){
            Taxon familyGL = HibernateProxyHelper.deproxy(getTaxonService().load(uuid, Arrays.asList(new String[]{"*"})), Taxon.class);
            Taxon familyCL = (Taxon) familyGL.clone();
            getTaxonService().saveOrUpdate(familyCL);

            gesamtListe.addChildTaxon(familyGL, null, null);
            familyGL.setSec(gesamtListe.getReference());
            familyGL.setTitleCache(null);

            checkliste.addChildTaxon(familyCL, null, null);
            familyCL.setSec(checkliste.getReference());
            familyCL.setTitleCache(null);

            getClassificationService().saveOrUpdate(gesamtListe);
            getClassificationService().saveOrUpdate(checkliste);
        }
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
        String familieString = rs.getString(RedListUtil.FAMILIE);

        String relationE = rs.getString(RedListUtil.E);
        String relationW = rs.getString(RedListUtil.W);
        String relationK = rs.getString(RedListUtil.K);
        String relationAW = rs.getString(RedListUtil.AW);
        String relationAO = rs.getString(RedListUtil.AO);
        String relationR = rs.getString(RedListUtil.R);
        String relationO = rs.getString(RedListUtil.O);
        String relationS = rs.getString(RedListUtil.S);

        //Gesamtliste
        TaxonBase<?> taxonBaseGL = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, String.valueOf(id), TaxonBase.class);
        TaxonBase<?> parentBaseGL = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(parentBaseGL!=null && !parentBaseGL.isInstanceOf(Taxon.class)){
            RedListUtil.logMessage(id, parentBaseGL+" is no taxon but is a parent of "+taxonBaseGL+" (Gesamtliste)", logger);
        }
        Taxon parentGL = (Taxon) state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, parentId, TaxonBase.class);
        //add to family if no parent found
        if(parentBaseGL==null){
            if(!taxonBaseGL.isInstanceOf(Taxon.class)){
                RedListUtil.logMessage(id, taxonBaseGL+" has no parent but is not a taxon.", logger);
            }
            else{
                Taxon family = HibernateProxyHelper.deproxy(getTaxonService().load(state.getFamilyMap().get(familieString)), Taxon.class);
                gesamtListeClassification.addParentChild(family, HibernateProxyHelper.deproxy(taxonBaseGL, Taxon.class), null, null);
            }
        }
        //add to higher taxon
        else{
            createParentChildNodes(gesamtListeClassification, id, gueltString, taxZusatzString, taxonBaseGL, parentGL);
        }

        //Checkliste
        TaxonBase<?> taxonBaseCL = state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, String.valueOf(id), TaxonBase.class);
        TaxonBase<?> parentBaseCL = state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(parentBaseCL!=null && !parentBaseCL.isInstanceOf(Taxon.class)){
            RedListUtil.logMessage(id, parentBaseCL+" is no taxon but is a parent of "+taxonBaseCL+" (Checkliste)", logger);
        }
        Taxon parentCL = (Taxon) state.getRelatedObject(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, parentId, TaxonBase.class);
        if(taxonBaseCL!=null){//null check necessary because not all taxa exist in the checklist
            //add to family if no parent found
            if(parentCL==null){
                if(!taxonBaseCL.isInstanceOf(Taxon.class)){
                    RedListUtil.logMessage(id, taxonBaseCL+" has no parent but is not a taxon.", logger);
                }
                else{
                    Taxon family = HibernateProxyHelper.deproxy(getTaxonService().load(state.getFamilyMap().get(familieString)), Taxon.class);
                    checklistClassification.addParentChild(family, HibernateProxyHelper.deproxy(taxonBaseCL, Taxon.class), null, null);
                }
            }
            //add to higher taxon
            else{
                createParentChildNodes(checklistClassification, id, gueltString, taxZusatzString, taxonBaseCL, parentCL);
            }
        }

        //check uuids
        if(taxonBaseGL!= null && taxonBaseCL!=null
                && taxonBaseGL.getUuid().equals(taxonBaseCL.getUuid())){
            RedListUtil.logMessage(id, "Same UUID for "+taxonBaseGL+ " (Gesamtliste) and "+taxonBaseCL+" (Checkliste)", logger);
        }
        if(parentGL!=null && parentCL!=null && parentGL.getUuid().equals(parentCL.getUuid())){
            RedListUtil.logMessage(id, "Same UUID for "+parentGL+ " (Gesamtliste) and "+parentCL+" (Checkliste)", logger);
        }

        //add taxa for concept relationships to E, W, K, AW, AO, R, O, S
        addTaxonToClassification(classificationE, RedListUtil.CLASSIFICATION_NAMESPACE_E, relationE, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationW, RedListUtil.CLASSIFICATION_NAMESPACE_W, relationW, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationK, RedListUtil.CLASSIFICATION_NAMESPACE_K, relationK, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationAW, RedListUtil.CLASSIFICATION_NAMESPACE_AW, relationAW, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationAO, RedListUtil.CLASSIFICATION_NAMESPACE_AO, relationAO, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationR, RedListUtil.CLASSIFICATION_NAMESPACE_R, relationR, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationO, RedListUtil.CLASSIFICATION_NAMESPACE_O, relationO, taxonBaseGL, taxonBaseCL, id, state);
        addTaxonToClassification(classificationS, RedListUtil.CLASSIFICATION_NAMESPACE_S, relationS, taxonBaseGL, taxonBaseCL, id, state);
    }

    private void addTaxonToClassification(Classification classification, String classificationNamespace, String relationString, final TaxonBase<?> gesamtListeTaxon, final TaxonBase<?> checklisteTaxon, long id, RedListGefaesspflanzenImportState state){
        Taxon taxon = HibernateProxyHelper.deproxy(state.getRelatedObject(classificationNamespace, String.valueOf(id), TaxonBase.class), Taxon.class);
        //add concept relation to gesamtliste and checkliste
        if(taxon!=null && CdmUtils.isNotBlank(relationString) && !relationString.equals(".")){
            //if the related concept in gesamtliste/checkliste is a synonym then we
            //create a relation to the accepted taxon
            Taxon acceptedGesamtListeTaxon = getAcceptedTaxon(gesamtListeTaxon);
            Taxon acceptedChecklistTaxon = getAcceptedTaxon(checklisteTaxon);
            String relationSubstring = relationString.substring(relationString.length()-1, relationString.length());
            TaxonRelationshipType taxonRelationshipTypeByKey = new RedListGefaesspflanzenTransformer().getTaxonRelationshipTypeByKey(relationSubstring);
            if(taxonRelationshipTypeByKey==null){
                RedListUtil.logMessage(id, "Could not interpret relationship "+relationString+" for taxon "+gesamtListeTaxon.generateTitle(), logger);
            }
            //there is no type "included in" so we have to reverse the direction
            if(relationSubstring.equals("<")){
                if(acceptedGesamtListeTaxon!=null){
                    acceptedGesamtListeTaxon.addTaxonRelation(taxon, taxonRelationshipTypeByKey, null, null);
                }
                if(acceptedChecklistTaxon!=null) {
                    acceptedChecklistTaxon.addTaxonRelation(taxon, taxonRelationshipTypeByKey, null, null);
                }
            }
            else{
                if(acceptedGesamtListeTaxon!=null){
                    taxon.addTaxonRelation(acceptedGesamtListeTaxon, taxonRelationshipTypeByKey, null, null);
                }
                if(acceptedChecklistTaxon!=null) {
                    taxon.addTaxonRelation(acceptedChecklistTaxon, taxonRelationshipTypeByKey, null, null);
                }
            }

            taxon.setSec(classification.getReference());
            classification.addChildTaxon(taxon, null, null);
            //TODO is saving and setting the title cache to null neccessary?
            getTaxonService().saveOrUpdate(taxon);
            getTaxonService().saveOrUpdate(gesamtListeTaxon);
            if(checklisteTaxon!=null){
                getTaxonService().saveOrUpdate(checklisteTaxon);
            }
            taxon.setTitleCache(null);//Reset title cache to see sec ref in title
        }
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
        result.put(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE));
        result.put(RedListUtil.TAXON_CHECKLISTE_NAMESPACE, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.TAXON_CHECKLISTE_NAMESPACE));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_E, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_E));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_W, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_W));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_K, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_K));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_AW, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_AW));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_AO, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_AO));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_R, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_R));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_O, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_O));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_S, (Map<String, TaxonBase>) getCommonService().getSourcedObjectsByIdInSource(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_S));
        return result;
    }

    private Classification makeClassification(String classificationName, UUID classificationUuid, String referenceName, Integer yearPublished, UUID referenceUuid, RedListGefaesspflanzenImportState state) {
        Classification classification = Classification.NewInstance(classificationName, Language.DEFAULT());
        classification.setUuid(classificationUuid);
        Reference reference = ReferenceFactory.newGeneric();
        reference.setTitle(referenceName);
        reference.setUuid(referenceUuid);
        classification.setReference(reference);
        if(yearPublished!=null){
            reference.setDatePublished(TimePeriod.NewInstance(yearPublished));
        }
        return getClassificationService().save(classification);
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
