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
import java.util.Map.Entry;
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
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * @author pplitzner
 * @since Mar 1, 2016
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
        return "SELECT SEQNUM "
                + "FROM V_TAXATLAS_D20_EXPORT t "
                + " ORDER BY SEQNUM";
    }

    @Override
    protected String getRecordQuery(RedListGefaesspflanzenImportConfigurator config) {
        String result = "select distinct e.*, f.FAMILIE "
                + "from V_TAXATLAS_D20_EXPORT e, GATTUNG_FAMILIE f "
                + "where e.EPI1 = f.GATTUNG and e.SEQNUM IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        Classification gesamtListe = makeClassification("Gesamtliste", state.getConfig().getClassificationUuid(), "Gesamtliste", null, RedListUtil.gesamtListeReferenceUuid, state);
        Classification checkliste = makeClassification("Checkliste", RedListUtil.checkListClassificationUuid, "Checkliste", null, RedListUtil.checkListReferenceUuid, state);
        makeClassification("Ehrendorfer", RedListUtil.uuidClassificationE, "Ehrendorfer", null, RedListUtil.uuidClassificationReferenceE, state);
        makeClassification("Wisskirchen (Standardliste)", RedListUtil.uuidClassificationW, "Wisskirchen (Standardliste)", 1998, RedListUtil.uuidClassificationReferenceW, state);
        makeClassification("Korneck (Rote Liste)", RedListUtil.uuidClassificationK, "Korneck (Rote Liste)", 1996, RedListUtil.uuidClassificationReferenceK, state);
        makeClassification("Atlas (Westdeutschland)", RedListUtil.uuidClassificationAW, "Atlas (Westdeutschland)", 1988, RedListUtil.uuidClassificationReferenceAW, state);
        makeClassification("Atlas (Ostdeutschland)", RedListUtil.uuidClassificationAO, "Atlas (Ostdeutschland)", 1996, RedListUtil.uuidClassificationReferenceAO, state);
        makeClassification("Rothmaler", RedListUtil.uuidClassificationR, "Rothmaler", 2011, RedListUtil.uuidClassificationReferenceR, state);
        makeClassification("Oberdorfer", RedListUtil.uuidClassificationO, "Oberdorfer", 2001, RedListUtil.uuidClassificationReferenceO, state);
        makeClassification("Schmeil-Fitschen", RedListUtil.uuidClassificationS, "Schmeil-Fitschen", 2011, RedListUtil.uuidClassificationReferenceS, state);
        importFamilies(gesamtListe, checkliste, state);
        super.doInvoke(state);
    }


    private void importFamilies(Classification gesamtListe, Classification checkliste, RedListGefaesspflanzenImportState state) {
        for(UUID uuid:state.getFamilyMap().values()){
            Taxon family = HibernateProxyHelper.deproxy(getTaxonService().load(uuid, true, Arrays.asList(new String[]{"*"})), Taxon.class);

            gesamtListe.addParentChild(null, family, null, null);
            checkliste.addParentChild(null, family, null, null);
            family.setSec(gesamtListe.getReference());
            family.setTitleCache(null);

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
        getClassificationService().saveOrUpdate(classificationE);
        getClassificationService().saveOrUpdate(classificationW);
        getClassificationService().saveOrUpdate(classificationK);
        getClassificationService().saveOrUpdate(classificationAW);
        getClassificationService().saveOrUpdate(classificationAO);
        getClassificationService().saveOrUpdate(classificationR);
        getClassificationService().saveOrUpdate(classificationO);
        getClassificationService().saveOrUpdate(classificationS);
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
        String clTaxonString = rs.getString(RedListUtil.CL_TAXON);

        String relationE = rs.getString(RedListUtil.E);
        String relationW = rs.getString(RedListUtil.W);
        String relationK = rs.getString(RedListUtil.K);
        String relationAW = rs.getString(RedListUtil.AW);
        String relationAO = rs.getString(RedListUtil.AO);
        String relationR = rs.getString(RedListUtil.R);
        String relationO = rs.getString(RedListUtil.O);
        String relationS = rs.getString(RedListUtil.S);

        //Gesamtliste
        TaxonBase<?> taxonBase = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, String.valueOf(id), TaxonBase.class);
        taxonBase.setSec(gesamtListeClassification.getReference());
        Taxon parent = state.getRelatedObject(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, parentId, Taxon.class);
        if(parent!=null && !parent.isInstanceOf(Taxon.class)){
            RedListUtil.logMessage(id, parent+" is no taxon but is a parent of "+taxonBase+" (Gesamtliste)", logger);
        }
        //add to family if no parent found
        if(parent==null){
            if(!taxonBase.isInstanceOf(Taxon.class)){
                RedListUtil.logMessage(id, taxonBase+" has no parent but is not a taxon.", logger);
            }
            else{
                Taxon family = (Taxon) state.getRelatedObject(RedListUtil.FAMILY_NAMESPACE_GESAMTLISTE, familieString);
                gesamtListeClassification.addParentChild(family, HibernateProxyHelper.deproxy(taxonBase, Taxon.class), null, null);
                //Buttler/Checklist taxon
                if(CdmUtils.isNotBlank(clTaxonString) && clTaxonString.equals(RedListUtil.CL_TAXON_B)){
                    checklistClassification.addParentChild(family, HibernateProxyHelper.deproxy(taxonBase, Taxon.class), null, null);
                    taxonBase.setSec(checklistClassification.getReference());
                }
                if(family.getTaxonNodes().isEmpty()){
                    gesamtListeClassification.addChildTaxon(family, null, null);
                    //do not add empty families to checklist classification
                    if(!getClassificationService().listChildNodesOfTaxon(family.getUuid(), RedListUtil.checkListClassificationUuid,
                            true, null, null, null).isEmpty()){
                        checklistClassification.addChildTaxon(family, null, null);
                    }
                }
            }
        }
        //add to higher taxon
        else{
            createParentChildNodes(gesamtListeClassification, id, gueltString, taxZusatzString, taxonBase, parent);
            //Buttler/Checklist taxon
            if(CdmUtils.isNotBlank(clTaxonString) && clTaxonString.equals(RedListUtil.CL_TAXON_B)){
                if(!checklistClassification.isTaxonInTree(parent)){
                    RedListUtil.logInfoMessage(id, parent+" is parent taxon but is not in checklist. Skipping child "+taxonBase, logger);
                }
                else{
                    if(taxonBase.isInstanceOf(Taxon.class)){
                        createParentChildNodes(checklistClassification, id, gueltString, taxZusatzString, taxonBase, parent);
                    }
                    else if(taxonBase.isInstanceOf(Synonym.class)){
                        //if it is a synonym it is already added to the accepted taxon
                        //so we just change the sec reference
                        taxonBase.setSec(checklistClassification.getReference());
                        taxonBase.setTitleCache(null);
                    }
                }

            }
        }
        taxonBase.setTitleCache(null, false);//refresh title cache

        //add taxa for concept relationships to E, W, K, AW, AO, R, O, S
        addTaxonToClassification(classificationE, RedListUtil.CLASSIFICATION_NAMESPACE_E, relationE, taxonBase, id, state);
        addTaxonToClassification(classificationW, RedListUtil.CLASSIFICATION_NAMESPACE_W, relationW, taxonBase, id, state);
        addTaxonToClassification(classificationK, RedListUtil.CLASSIFICATION_NAMESPACE_K, relationK, taxonBase, id, state);
        addTaxonToClassification(classificationAW, RedListUtil.CLASSIFICATION_NAMESPACE_AW, relationAW, taxonBase, id, state);
        addTaxonToClassification(classificationAO, RedListUtil.CLASSIFICATION_NAMESPACE_AO, relationAO, taxonBase, id, state);
        addTaxonToClassification(classificationR, RedListUtil.CLASSIFICATION_NAMESPACE_R, relationR, taxonBase, id, state);
        addTaxonToClassification(classificationO, RedListUtil.CLASSIFICATION_NAMESPACE_O, relationO, taxonBase, id, state);
        addTaxonToClassification(classificationS, RedListUtil.CLASSIFICATION_NAMESPACE_S, relationS, taxonBase, id, state);
    }



    private void addTaxonToClassification(Classification classification, String classificationNamespace, String relationString, final TaxonBase<?> gesamtListeTaxon, long id, RedListGefaesspflanzenImportState state){
        Taxon taxon = HibernateProxyHelper.deproxy(state.getRelatedObject(classificationNamespace, String.valueOf(id), TaxonBase.class), Taxon.class);
        //add concept relation to gesamtliste/checkliste
        if(taxon!=null && isNotBlank(relationString) && !relationString.equals(".")){
            //if the related concept in gesamtliste/checkliste is a synonym then we
            //create a relation to the accepted taxon
            Taxon acceptedGesamtListeTaxon = getAcceptedTaxon(gesamtListeTaxon);
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
            }
            else{
                if(acceptedGesamtListeTaxon!=null){
                    taxon.addTaxonRelation(acceptedGesamtListeTaxon, taxonRelationshipTypeByKey, null, null);
                }
            }

            taxon.setSec(classification.getReference());
            classification.addChildTaxon(taxon, null, null);
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
                //return because misapplied names do not have sec references
                return;
            }
            else{
                classification.addParentChild(parent, (Taxon)taxonBase, null, null);
            }

            if(isNotBlank(taxZusatzString)){
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
                parent.addHomotypicSynonym((Synonym) taxonBase);
                parent.getName().addBasionym(taxonBase.getName());
            }
            //regular synonym
            else{
                Synonym synonym = (Synonym) taxonBase;
                parent.addSynonym(synonym, SynonymType.HETEROTYPIC_SYNONYM_OF());

                //TAX_ZUSATZ
                if(isNotBlank(taxZusatzString)){
                    if(taxZusatzString.trim().equals("p. p.")){
                        logger.warn(id + ": p. p. not implemented anymore");
                    }
                    else if(taxZusatzString.trim().equals("s. l. p. p.")){
                        logger.warn(id + ": p. p. not implemented anymore");
                        taxonBase.setAppendedPhrase("s. l.");
                    }
                    else if(taxZusatzString.trim().equals("s. str. p. p.")){
                        logger.warn(id + ": p. p. not implemented anymore");
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
        //add taxa and their parent taxa
        result.put(RedListUtil.TAXON_GESAMTLISTE_NAMESPACE, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_E, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_E));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_W, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_W));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_K, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_K));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_AW, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_AW));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_AO, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_AO));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_R, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_R));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_O, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_O));
        result.put(RedListUtil.CLASSIFICATION_NAMESPACE_S, getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, RedListUtil.CLASSIFICATION_NAMESPACE_S));

        //add families
        //gesamtliste
        Map<String, Taxon> familyMapGL = new HashMap<String, Taxon>();
        for (Entry<String, UUID> entry: state.getFamilyMap().entrySet()) {
            familyMapGL.put(entry.getKey(), HibernateProxyHelper.deproxy(getTaxonService().load(entry.getValue()), Taxon.class));
        }
        result.put(RedListUtil.FAMILY_NAMESPACE_GESAMTLISTE, familyMapGL);
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
            reference.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(yearPublished));
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
