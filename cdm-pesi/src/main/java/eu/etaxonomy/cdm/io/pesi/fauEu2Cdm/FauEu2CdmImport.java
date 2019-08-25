/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.fauEu2Cdm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Contact;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.InstitutionalMembership;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Credit;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IIntextReferencable;
import eu.etaxonomy.cdm.model.common.IIntextReferenceTarget;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Identifier;
import eu.etaxonomy.cdm.model.common.IntextReference;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.LanguageString;
import eu.etaxonomy.cdm.model.common.LanguageStringBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.ReferencedEntityBase;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.common.SourcedEntityBase;
import eu.etaxonomy.cdm.model.common.VersionableEntity;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.DescriptiveDataSet;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.FeatureState;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.media.ExternalLink;
import eu.etaxonomy.cdm.model.media.IdentifiableMediaEntity;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.name.HomotypicalGroup;
import eu.etaxonomy.cdm.model.name.HybridRelationship;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.Registration;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TypeDesignationBase;
import eu.etaxonomy.cdm.model.occurrence.DerivationEvent;
import eu.etaxonomy.cdm.model.occurrence.DeterminationEvent;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.reference.OriginalSourceBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonNodeAgentRelation;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.OrderedTermBase;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermBase;
import eu.etaxonomy.cdm.model.term.TermCollection;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermRelationBase;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
@Component
public class FauEu2CdmImport
        extends FauEu2CdmImportBase {

    private static final long serialVersionUID = -2111102574346601573L;
    private static final Logger logger = Logger.getLogger(FauEu2CdmImport.class);

    //TODO move to state
    private Map<UUID, CdmBase> sessionCache = new HashMap<>();
    private Map<UUID, CdmBase> permanentCache = new HashMap<>();
    private Set<UUID> movedObjects = new HashSet<>();

    private Set<CdmBase> toSave = new HashSet<>();


    @Override
    protected void doInvoke(FauEu2CdmImportState state) {
        IProgressMonitor monitor = state.getConfig().getProgressMonitor();
        System.out.println("start source repo");
        source(state);
        System.out.println("end source repo");
        FauEu2CdmImportConfigurator config = state.getConfig();
        if (config.getTaxonNodeFilter().hasClassificationFilter()) {
            Classification classification = getClassificationService()
                    .load(config.getTaxonNodeFilter().getClassificationFilter().get(0).getUuid());
            state.setRootId(classification.getRootNode().getUuid());
        } else if (config.getTaxonNodeFilter().hasSubtreeFilter()) {
            state.setRootId(config.getTaxonNodeFilter().getSubtreeFilter().get(0).getUuid());
        }
        @SuppressWarnings("unchecked")
        TaxonNodeOutStreamPartitioner<FauEu2CdmImportState> partitioner = TaxonNodeOutStreamPartitioner
                .NewInstance(source(state), state, state.getConfig().getTaxonNodeFilter(), 100, monitor, null);
        monitor.subTask("Start partitioning");
        partitioner.setLastCommitManually(true);
        doData(state, partitioner);
    }

    /**
     * @param state
     * @param partitioner
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void doData(FauEu2CdmImportState state, TaxonNodeOutStreamPartitioner<FauEu2CdmImportState> partitioner){
        TaxonNode node = partitioner.next();
        int partitionSize = 100;
        int count = 0;
        TransactionStatus tx = startTransaction();
        while (node != null) {
            node = doSingleNode(state, node);
            count++;
            if (count>=partitionSize){
                clearCache();
                try {
                    commitTransaction(tx);
                } catch (Exception e) {
                    logger.warn("Exception during commit node " + node.treeIndex());
                    e.printStackTrace();
                }
                tx = startTransaction();
                count=0;
            }
            node = partitioner.next();
        }
        commitTransaction(tx);
        partitioner.close();
    }

    /**
     * @param state
     * @param node
     * @return
     */
    private TaxonNode doSingleNode(FauEu2CdmImportState state, TaxonNode node) {
        TaxonNode result = null;
        logger.info(node.treeIndex());
        try {
            result = detache(node);
        } catch (Exception e) {
            logger.warn("Exception during detache node " + node.treeIndex());
            e.printStackTrace();
        }
        try {
            if (result != null){
                getTaxonNodeService().saveOrUpdate(node);
                getCommonService().saveOrUpdate(toSave);
                toSave.clear();
            }
        } catch (Exception e) {
            logger.warn("Exception during save node " + node.treeIndex());
             e.printStackTrace();
        }

        return result;
    }

    private void clearCache() {
        sessionCache.clear();
    }

    private TaxonNode handlePersistedTaxonNode(TaxonNode node) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {

        TaxonNode result = handlePersisted((AnnotatableEntity)node);
        if (result ==null){
            return result;
        }
        handleCollection(result, TaxonNode.class, "agentRelations", TaxonNodeAgentRelation.class);
        result.setTaxon(detache(result.getTaxon()));
        result.setReference(detache(node.getReference()));
        handleMap(result, TaxonNode.class, "excludedNote", Language.class, LanguageString.class);
        //classification, parent, children
        this.setInvisible(node, "classification", detache(node.getClassification()));
        handleParentTaxonNode(result);
        setNewCollection(node, TaxonNode.class, "childNodes", TaxonNode.class);
        return result;
    }

    private void handleParentTaxonNode(TaxonNode childNode) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TaxonNode parent = detache(childNode.getParent());
        if (parent == null){
            return;
        }
        //TODO
        String microReference = null;
        Reference reference = null;
        parent.addChildNode(childNode, reference, microReference);
    }

    private void setInvisible(Object holder, String fieldName, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        setInvisible(holder, holder.getClass(), fieldName, value);
    }
    private void setInvisible(Object holder, Class<?> holderClazz, String fieldName, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = holderClazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(holder, value);
    }

    private Classification handlePersistedClassification(Classification classification) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Classification result = handlePersisted((IdentifiableEntity)classification);
        if (result ==null){
            return result;
        }
        result.setName(detache(classification.getName()));
        result.setReference(detache(classification.getReference()));
        result.setRootNode(detache(classification.getRootNode()));
        handleCollection(result, Classification.class, "geoScopes", NamedArea.class);
        handleMap(result, Classification.class, "description", Language.class, LanguageString.class);

        return result;
    }

    private Reference handlePersistedReference(Reference reference) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Reference result = handlePersisted((IdentifiableMediaEntity)reference);
        result.setAuthorship(detache(result.getAuthorship()));
        result.setInstitution(detache(result.getInstitution()));
        result.setSchool(detache(result.getSchool()));
        result.setInReference(detache(result.getInReference()));
        return result;
    }

    private SpecimenOrObservationBase<?> handlePersistedSpecimenOrObservationBase(SpecimenOrObservationBase specimen) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        SpecimenOrObservationBase<?> result = handlePersisted((IdentifiableEntity)specimen);
        result.setSex(detache(result.getSex()));
        result.setLifeStage(detache(result.getLifeStage()));
        result.setKindOfUnit(detache(result.getKindOfUnit()));
        //TODO implement for classes
        handleCollection(result, SpecimenOrObservationBase.class, "determinations", DeterminationEvent.class);
        handleCollection(result, SpecimenOrObservationBase.class, "descriptions", SpecimenDescription.class);
        handleCollection(result, SpecimenOrObservationBase.class, "derivationEvents", DerivationEvent.class);
        handleMap(result, SpecimenOrObservationBase.class, "definition", Language.class, LanguageString.class);
        return result;
    }

    private IdentifiableSource handlePersistedIdentifiableSource(IdentifiableSource source) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        IdentifiableSource result = handlePersisted((OriginalSourceBase)source);
        if (result ==null){
            return null;
        }
        return result;
    }

    private DescriptionElementSource handlePersistedDescriptionElementSource(DescriptionElementSource source) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        DescriptionElementSource result = handlePersisted((OriginalSourceBase)source);
        result.setNameUsedInSource(detache(result.getNameUsedInSource()));
        return result;
    }

    private <T extends OriginalSourceBase> T  handlePersisted(OriginalSourceBase source) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((ReferencedEntityBase)source);
        handleCollection(result, OriginalSourceBase.class, "links", ExternalLink.class);

        return result;
    }

    private LanguageString handlePersistedLanguageString(LanguageString languageString) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        LanguageString result = handlePersisted((LanguageStringBase)languageString);
        handleCollection(result, LanguageString.class, "intextReferences", IntextReference.class);
        return result;
    }

    private IntextReference handlePersistedIntextReference(IntextReference intextReference) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        IntextReference result = handlePersisted((VersionableEntity)intextReference);
        result.setReferencedEntity(detache(result.getReferencedEntity(), false));
        Method targetMethod = IntextReference.class.getDeclaredMethod("setTarget", IIntextReferenceTarget.class);
        targetMethod.setAccessible(true);
        targetMethod.invoke(result, detache(result.getTarget(), false));
        return result;
    }

    private <T extends LanguageStringBase> T  handlePersisted(LanguageStringBase lsBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((AnnotatableEntity)lsBase);
        if (result ==null){
            return null;
        }
        result.setLanguage(detache(lsBase.getLanguage()));
        return result;
    }

    /**
     * @param classification
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     */
    private <T extends IdentifiableEntity> T  handlePersisted(IdentifiableEntity identifiableEntity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((SourcedEntityBase)identifiableEntity);
        if (result ==null){
            return null;
        }
        handleCollection(result, IdentifiableEntity.class, "credits", Credit.class);
        handleCollection(result, IdentifiableEntity.class, "extensions", Extension.class);
        handleCollection(result, IdentifiableEntity.class, "identifiers", Identifier.class);
        handleCollection(result, IdentifiableEntity.class, "rights", Rights.class);
        return result;
    }

    private <T extends TeamOrPersonBase> T  handlePersisted(TeamOrPersonBase teamOrPerson) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((AgentBase)teamOrPerson);
        if (result ==null){
            return null;
        }
        return result;
    }

    private <T extends AgentBase> T  handlePersisted(AgentBase agent) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((IdentifiableMediaEntity)agent);
        result.setContact(detache(result.getContact()));
        return result;
    }

    private <T extends TaxonBase> T  handlePersisted(TaxonBase taxonBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((IdentifiableEntity)taxonBase);
        if (result ==null){
            return null;
        }
        result.setName(detache(taxonBase.getName()));
        result.setSec(detache(taxonBase.getSec()));
//        handleCollection(result, IdentifiableEntity.class, "credits", Credit.class);
//        handleCollection(result, IdentifiableEntity.class, "extensions", Extension.class);
//        handleCollection(result, IdentifiableEntity.class, "identifiers", Identifier.class);
//        handleCollection(result, IdentifiableEntity.class, "rights", Rights.class);
        return result;
    }

    private <T extends SourcedEntityBase> T  handlePersisted(SourcedEntityBase sourcedEntity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((AnnotatableEntity)sourcedEntity);
        if (result ==null){
            return null;
        }
        handleCollection(result, SourcedEntityBase.class, "sources", OriginalSourceBase.class);
        return result;
    }

    private <T extends IdentifiableMediaEntity> T  handlePersisted(IdentifiableMediaEntity mediaEntity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((IdentifiableEntity)mediaEntity);
        handleCollection(result, IdentifiableMediaEntity.class, "media", Media.class);
        return result;
    }

    private <T extends ReferencedEntityBase> T  handlePersisted(ReferencedEntityBase referencedEntity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((AnnotatableEntity)referencedEntity);
        if (result ==null){
            return null;
        }
        result.setCitation(detache(result.getCitation()));
        return result;
    }

    private <T extends DescriptionBase> T  handlePersisted(DescriptionBase descriptionBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((IdentifiableEntity)descriptionBase);
        handleCollection(result, DescriptionBase.class, "descriptionElements", DescriptionElementBase.class);
        handleCollection(result, DescriptionBase.class, "descriptiveDataSets", DescriptiveDataSet.class);
        handleCollection(result, DescriptionBase.class, "descriptionSources", Reference.class);
        result.setDescribedSpecimenOrObservation(detache(descriptionBase.getDescribedSpecimenOrObservation()));
        return result;
    }

    private <T extends DescriptionElementBase> T  handlePersisted(DescriptionElementBase element) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((AnnotatableEntity)element);
        result.setFeature(detache(result.getFeature()));
        setInvisible(result, DescriptionElementBase.class, "inDescription", detache(result.getInDescription()));
        handleCollection(result, DescriptionElementBase.class, "sources", DescriptionElementSource.class);
        handleCollection(result, DescriptionElementBase.class, "media", Media.class);
        handleCollection(result, DescriptionElementBase.class, "modifiers", DefinedTerm.class);
        handleMap(result, DescriptionElementBase.class, "modifyingText", Language.class, LanguageString.class);

        return result;
    }

    private <T extends CommonTaxonName> T  handlePersistedCommonTaxonName(CommonTaxonName element) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((DescriptionElementBase)element);
        result.setLanguage(detache(result.getLanguage()));
        result.setArea(detache(result.getArea()));
        return result;
    }

    private <T extends TextData> T  handlePersistedTextData(TextData element) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((DescriptionElementBase)element);
        result.setFormat(detache(result.getFormat()));
        handleMap(result, TextData.class, "multilanguageText", Language.class, LanguageString.class);
        return result;
    }

    private <T extends Distribution> T  handlePersistedDistribution(Distribution element) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((DescriptionElementBase)element);
        result.setArea(detache(result.getArea()));
        result.setStatus(detache(result.getStatus()));
        return result;
    }

    private <T extends RelationshipBase> T  handlePersisted(RelationshipBase relBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((ReferencedEntityBase)relBase);
        return result;
    }

    private <T extends TaxonDescription> T  handlePersistedTaxonDescription(TaxonDescription taxDescription) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((DescriptionBase)taxDescription);
        setInvisible(taxDescription, "taxon", detache(taxDescription.getTaxon()));
        handleCollection(taxDescription, TaxonDescription.class, "geoScopes", NamedArea.class);
        handleCollection(taxDescription, TaxonDescription.class, "scopes", DefinedTerm.class);
        return result;
    }

    private <T extends TaxonDescription> T  handlePersistedTaxonNameDescription(TaxonNameDescription nameDescription) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((DescriptionBase)nameDescription);
        setInvisible(nameDescription, "taxonName", detache(nameDescription.getTaxonName()));
        return result;
    }

    private <T extends AnnotatableEntity> T handlePersisted(AnnotatableEntity entity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((VersionableEntity)entity);
        if (result ==null){
            return null;
        }
        handleCollection(result, AnnotatableEntity.class, "annotations", Annotation.class);
        handleCollection(result, AnnotatableEntity.class, "markers", Marker.class);
        return result;
    }

    private <HOLDER extends CdmBase, ITEM extends CdmBase> void handleCollection(
            HOLDER holder, Class<? super HOLDER> declaringClass, String parameter, Class<ITEM> itemClass)
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Collection<ITEM> oldCollection = setNewCollection(holder, declaringClass, parameter, itemClass);
        Collection<ITEM> newCollection = getTargetCollection(itemClass, oldCollection);
        Field field = declaringClass.getDeclaredField(parameter);
        field.setAccessible(true);
        field.set(holder, newCollection);
//        EntityCollectionSetterAdapter<HOLDER,ITEM> adapter = new EntityCollectionSetterAdapter(declaringClass, itemClass, parameter);
//        if ("rights".equals(parameter)){
//            adapter.setAddMethodName("addRights");
//            adapter.setRemovMethodName("removeRights");
//        }
//        if ("media".equals(parameter)){
//            adapter.setAddMethodName("addMedia");
//            adapter.setRemovMethodName("removeMedia");
//        }
//        if ("media".equals(parameter)){
//            adapter.setAddMethodName("addMedia");
//            adapter.setRemovMethodName("removeMedia");
//        }
//        if ("media".equals(parameter)){
//            adapter.setAddMethodName("addMedia");
//            adapter.setRemovMethodName("removeMedia");
//        }
//        try {
//            adapter.setCollection(holder, newCollection);
//        } catch (SetterAdapterException e) {
//
//            throw new RuntimeException(e);
//        }
////        for (T a : getTargetCollection(itemClass, oldCollection)){
////
////            result.addAnnotation(a);
////        }
    }

    private <HOLDER extends CdmBase, KEY extends CdmBase, ITEM extends CdmBase>
            void handleMap(
            HOLDER holder, Class<? super HOLDER> declaringClass, String parameter,
            Class<KEY> keyClass, Class<ITEM> itemClass)
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        //TODO we do not need to set the new map 2x
        Map<KEY,ITEM> oldMap = setNewMap(holder, declaringClass, parameter, keyClass, itemClass);
        Map<KEY,ITEM> newMap = getTargetMap(oldMap);
        Field field = declaringClass.getDeclaredField(parameter);
        field.setAccessible(true);
        field.set(holder, newMap);
    }

    private <T extends CdmBase> Collection<T> setNewCollection(CdmBase obj, Class<?> holderClass,
            String parameter, Class<T> entityClass) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = holderClass.getDeclaredField(parameter);
        field.setAccessible(true);
        Collection<T> oldValue = (Collection<T>)field.get(obj);
        Collection<T> newValue = null;
        if (Set.class.isAssignableFrom(field.getType())){
            newValue = new HashSet<>();
        }else if (List.class.isAssignableFrom(field.getType())){
            newValue = new ArrayList<>();
        }else{
            throw new RuntimeException("Unsupported collection type: " + field.getType().getCanonicalName());
        }
        field.set(obj, newValue);
        return oldValue;
    }

    private <KEY extends CdmBase, ITEM extends CdmBase> Map<KEY,ITEM> setNewMap(CdmBase obj, Class<?> holderClass,
            String parameter, Class<KEY> keyClass, Class<ITEM> itemClass) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = holderClass.getDeclaredField(parameter);
        field.setAccessible(true);
        Map<KEY,ITEM> oldValue = (Map<KEY,ITEM>)field.get(obj);
        Map<KEY,ITEM> newValue = null;
        if (Map.class.isAssignableFrom(field.getType())){
            newValue = new HashMap<>();
        }else{
            throw new RuntimeException("Unsupported map type: " + field.getType().getCanonicalName());
        }
        field.set(obj, newValue);
        return oldValue;
    }

    private <T extends Collection<S>, S extends CdmBase> Collection<S> getTargetCollection(Class<S> clazz, T sourceCollection) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Collection<S> result =  new ArrayList<>();
        if (Set.class.isAssignableFrom(sourceCollection.getClass())){
            result = new HashSet<>();
        }
        for (S entity : sourceCollection){
            S target = detache(entity);
            result.add(target);
        }
        return result;
    }

    private <K extends CdmBase, V extends CdmBase> Map<K,V> getTargetMap(Map<K,V> sourceMap) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Map<K,V> result = new HashMap<>();
        for (K key : sourceMap.keySet()){
            K targetKey = detache(key);
            V targetValue = detache(sourceMap.get(key));
            result.put(targetKey, targetValue);
        }
        return result;
    }

    private <T extends VersionableEntity> T handlePersisted(VersionableEntity entity) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = (T)handlePersistedCdmBase((CdmBase)entity);
        if (result ==null){
            return null;
        }
        entity.setUpdatedBy(detache(entity.getUpdatedBy()));
        return result;
    }

    private <A extends CdmBase> CdmBase handlePersisted(A cdmBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        if(cdmBase instanceof TaxonNode){
            return handlePersistedTaxonNode((TaxonNode)cdmBase);
        }else if(cdmBase instanceof Taxon){
            return handlePersistedTaxon((Taxon)cdmBase);
        }else if(cdmBase instanceof Synonym){
            return handlePersistedSynonym((Synonym)cdmBase);
        }else if(cdmBase instanceof TaxonName){
            return handlePersistedTaxonName((TaxonName)cdmBase);
        }else if(cdmBase instanceof Team){
            return handlePersistedTeam((Team)cdmBase);
        }else if(cdmBase instanceof Person){
            return handlePersistedPerson((Person)cdmBase);
        }else if(cdmBase instanceof Classification){
            return handlePersistedClassification((Classification)cdmBase);
        }else if(cdmBase instanceof Reference){
            return handlePersistedReference((Reference)cdmBase);
        }else if(cdmBase instanceof SpecimenOrObservationBase){
            return handlePersistedSpecimenOrObservationBase((SpecimenOrObservationBase)cdmBase);
        }else if(cdmBase instanceof IdentifiableSource){
            return handlePersistedIdentifiableSource((IdentifiableSource)cdmBase);
        }else if(cdmBase instanceof DescriptionElementSource){
            return handlePersistedDescriptionElementSource((DescriptionElementSource)cdmBase);
        }else if(cdmBase instanceof CommonTaxonName){
            return handlePersistedCommonTaxonName((CommonTaxonName)cdmBase);
        }else if(cdmBase instanceof Distribution){
            return handlePersistedDistribution((Distribution)cdmBase);
        }else if(cdmBase instanceof TextData){
            return handlePersistedTextData((TextData)cdmBase);
        }else if(cdmBase instanceof HomotypicalGroup){
            return handlePersistedHomotypicalGroup((HomotypicalGroup)cdmBase);
        }else if(cdmBase instanceof TypeDesignationBase){
            return handlePersistedTypeDesignationBase((TypeDesignationBase)cdmBase);
        }else if(cdmBase instanceof TaxonDescription){
            return handlePersistedTaxonDescription((TaxonDescription)cdmBase);
        }else if(cdmBase instanceof NomenclaturalStatus){
            return handlePersistedNomenclaturalStatus((NomenclaturalStatus)cdmBase);
        }else if(cdmBase instanceof TaxonNameDescription){
            return handlePersistedTaxonNameDescription((TaxonNameDescription)cdmBase);
        }else if(cdmBase instanceof TaxonRelationship){
            return handlePersistedTaxonRelationship((TaxonRelationship)cdmBase);
        }else if(cdmBase instanceof HybridRelationship){
            return handlePersistedHybridRelationship((HybridRelationship)cdmBase);
        }else if(cdmBase instanceof NameRelationship){
            return handlePersistedNameRelationship((NameRelationship)cdmBase);
        }else if(cdmBase instanceof TaxonNodeAgentRelation){
            return handlePersistedTaxonNodeAgentRelation((TaxonNodeAgentRelation)cdmBase);
        }else if(cdmBase instanceof User){
            return handlePersistedUser((User)cdmBase);
        }else if(cdmBase instanceof Extension){
            return handlePersistedExtension((Extension)cdmBase);
        }else if(cdmBase instanceof Marker){
            return handlePersistedMarker((Marker)cdmBase);
        }else if(cdmBase instanceof Annotation){
            return handlePersistedAnnotation((Annotation)cdmBase);
        }else if(cdmBase instanceof LanguageString){
            return handlePersistedLanguageString((LanguageString)cdmBase);
        }else if(cdmBase instanceof TermVocabulary){
            return handlePersistedVocabulary((TermVocabulary<?>)cdmBase);
        }else if(cdmBase instanceof NamedArea){
            return handlePersistedNamedArea((NamedArea)cdmBase);
        }else if(cdmBase instanceof TermNode){
            return handlePersistedTermNode((TermNode)cdmBase);
        }else if(cdmBase instanceof Representation){
            return handlePersistedRepresentation((Representation)cdmBase);
        }else if(cdmBase instanceof InstitutionalMembership){
            return handlePersistedInstitutionalMembership((InstitutionalMembership)cdmBase);
        }else if(cdmBase instanceof Institution){
            return handlePersistedInstitution((Institution)cdmBase);
        }else if(cdmBase instanceof IntextReference){
            return handlePersistedIntextReference((IntextReference)cdmBase);
        }else if(cdmBase instanceof ExtensionType){
            return handlePersistedExtensionType((ExtensionType)cdmBase);
        }else if(cdmBase instanceof DefinedTerm){
            return handlePersistedDefinedTerm((DefinedTerm)cdmBase);
        }else if(cdmBase instanceof DefinedTermBase){
            return handlePersistedTerm((DefinedTermBase<?>)cdmBase);
        }else {
            throw new RuntimeException("Type not yet supported: " + cdmBase.getClass().getCanonicalName());
        }
    }

    /**
     * @param cdmBase
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Taxon handlePersistedTaxon(Taxon taxon) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Taxon result = handlePersisted((TaxonBase)taxon);
        handleCollection(result, Taxon.class, "synonyms", Synonym.class);
//        handleCollection(result, Taxon.class, "taxonNodes", TaxonNode.class);
        setNewCollection(result, Taxon.class, "taxonNodes", TaxonNode.class);
        handleCollection(result, Taxon.class, "relationsFromThisTaxon", TaxonRelationship.class);
        handleCollection(result, Taxon.class, "relationsToThisTaxon", TaxonRelationship.class);
        handleCollection(result, Taxon.class, "descriptions", TaxonDescription.class);
        return result;
    }

    private Synonym handlePersistedSynonym(Synonym synonym) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Synonym result = handlePersisted((TaxonBase)synonym);
        setInvisible(result, "acceptedTaxon", detache(result.getAcceptedTaxon()));
        result.setType(detache(result.getType()));
        return result;
    }

    private TaxonRelationship handlePersistedTaxonRelationship(TaxonRelationship taxRel) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TaxonRelationship result = handlePersisted((RelationshipBase)taxRel);
        result.setFromTaxon(detache(result.getFromTaxon()));
        result.setToTaxon(detache(result.getToTaxon()));
        result.setType(detache(result.getType()));
        return result;
    }

    private NameRelationship handlePersistedNameRelationship(NameRelationship rel) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        NameRelationship result = handlePersisted((RelationshipBase)rel);
        setInvisible(result, "relatedFrom", detache(result.getFromName()));
        setInvisible(result, "relatedTo", detache(result.getToName()));
//        result.setFromName(detache(result.getFromName()));
//        result.setToName(detache(result.getToName()));
        result.setType(detache(result.getType()));
        return result;
    }

    private HybridRelationship handlePersistedHybridRelationship(HybridRelationship rel) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        HybridRelationship result = handlePersisted((RelationshipBase)rel);
        setInvisible(result, "relatedFrom", detache(result.getParentName()));
        setInvisible(result, "relatedTo", detache(result.getHybridName()));
//        result.setFromName(detache(result.getFromName()));
//        result.setToName(detache(result.getToName()));
        result.setType(detache(result.getType()));
        return result;
    }
    private NomenclaturalStatus handlePersistedNomenclaturalStatus(NomenclaturalStatus status) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        NomenclaturalStatus result = handlePersisted((ReferencedEntityBase)status);
        result.setType(detache(result.getType()));
        return result;
    }

    private TypeDesignationBase handlePersistedTypeDesignationBase(TypeDesignationBase<?> designation) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TypeDesignationBase result = handlePersisted((SourcedEntityBase)designation);
        result.setCitation(detache(result.getCitation()));
        handleCollection(result, TypeDesignationBase.class, "registrations", Registration.class);
        handleCollection(result, TypeDesignationBase.class, "typifiedNames", TaxonName.class);
        result.setTypeStatus(detache(result.getTypeStatus()));
        return result;
    }

    private InstitutionalMembership handlePersistedInstitutionalMembership(InstitutionalMembership institutionalMembership) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        InstitutionalMembership result = handlePersisted((VersionableEntity)institutionalMembership);
//        result.setPerson(detache(result.getPerson()));
        setInvisible(result, "person", detache(result.getPerson()));
        result.setInstitute(detache(result.getInstitute()));
        return result;
    }

    private Institution handlePersistedInstitution(Institution institution) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Institution result = handlePersisted((AgentBase)institution);
        result.setIsPartOf(detache(result.getIsPartOf()));
        handleCollection(result, Institution.class, "types", DefinedTerm.class);
        return result;
    }

    private TaxonNodeAgentRelation handlePersistedTaxonNodeAgentRelation(TaxonNodeAgentRelation nodeAgentRel) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TaxonNodeAgentRelation result = handlePersisted((AnnotatableEntity)nodeAgentRel);
        if (result ==null){
            return result;
        }
        result.setAgent(detache(result.getAgent()));
        result.setType(detache(result.getType()));
        setInvisible(result, "taxonNode", detache(result.getTaxonNode()));
        return result;
    }


    private TaxonName handlePersistedTaxonName(TaxonName taxonName) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        @SuppressWarnings("rawtypes")
        TaxonName result = handlePersisted((IdentifiableEntity)taxonName);
        if (result ==null){
            return result;
        }
        result.setCombinationAuthorship(detache(result.getCombinationAuthorship()));
        result.setExCombinationAuthorship(detache(result.getExCombinationAuthorship()));
        result.setBasionymAuthorship(detache(result.getBasionymAuthorship()));
        result.setExBasionymAuthorship(detache(result.getExBasionymAuthorship()));
        result.setInBasionymAuthorship(detache(result.getInBasionymAuthorship()));
        result.setInCombinationAuthorship(detache(result.getInCombinationAuthorship()));

        result.setNomenclaturalReference(detache(result.getNomenclaturalReference()));
        result.setHomotypicalGroup(detache(result.getHomotypicalGroup()));
        handleCollection(result, TaxonName.class, "descriptions", TaxonNameDescription.class);
        handleCollection(result, TaxonName.class, "hybridChildRelations", HybridRelationship.class);
        handleCollection(result, TaxonName.class, "hybridParentRelations", HybridRelationship.class);
        handleCollection(result, TaxonName.class, "relationsFromThisName", NameRelationship.class);
        handleCollection(result, TaxonName.class, "relationsToThisName", NameRelationship.class);
        handleCollection(result, TaxonName.class, "status", NomenclaturalStatus.class);

        handleCollection(result, TaxonName.class, "registrations", Registration.class);
        handleCollection(result, TaxonName.class, "typeDesignations", TypeDesignationBase.class);

        handleCollection(result, TaxonName.class, "taxonBases", TaxonBase.class);

        return result;
    }

    private HomotypicalGroup handlePersistedHomotypicalGroup(HomotypicalGroup group) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        HomotypicalGroup result = handlePersisted((AnnotatableEntity)group);
        if (result ==null){
            return result;
        }
        handleCollection(result, HomotypicalGroup.class, "typifiedNames", TaxonName.class);
        return result;
    }

    private Annotation handlePersistedAnnotation(Annotation annotation) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Annotation result = handlePersisted((AnnotatableEntity)annotation);
        if (result ==null){
            return result;
        }
        result.setAnnotationType(detache(annotation.getAnnotationType()));
        handleCollection(result, Annotation.class, "intextReferences", IntextReference.class);
        return result;
    }

    private Extension handlePersistedExtension(Extension extension) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Extension result = handlePersisted((VersionableEntity)extension);
        if (result ==null){
            return result;
        }
        result.setType(detache(extension.getType()));
        return result;
    }

    private Marker handlePersistedMarker(Marker marker) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Marker result = handlePersisted((VersionableEntity)marker);
        if (result ==null){
            return result;
        }
        result.setMarkerType(detache(marker.getMarkerType()));
        return result;
    }

    private Team handlePersistedTeam(Team team) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Team result = handlePersisted((TeamOrPersonBase)team);
        if (result ==null){
            return result;
        }
        handleCollection(result, Team.class, "teamMembers", Person.class);
        return result;
    }

    private Contact handlePersistedContact(Contact contact) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Contact result = contact; // getTarget(contact);
        if (result ==null){
            return result;
        }
        if (!contact.getAddresses().isEmpty() || !contact.getEmailAddresses().isEmpty()
               || !contact.getFaxNumbers().isEmpty() ||!contact.getPhoneNumbers().isEmpty()
               ||!contact.getUrls().isEmpty()){
            logger.warn("Addresses not yet implemented");
        }
        setInvisible(result, "addresses", new HashSet<>());
//        handleCollection(result, Contact.class, "", Address.class);
        setInvisible(result, "faxNumbers", new ArrayList<>());
        setInvisible(result, "phoneNumbers", new ArrayList<>());
        setInvisible(result, "emailAddresses", new ArrayList<>());
        setInvisible(result, "urls", new ArrayList<>());
        return result;
    }

    private Person handlePersistedPerson(Person person) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Person result = handlePersisted((TeamOrPersonBase)person);
        handleCollection(result, Person.class, "institutionalMemberships", InstitutionalMembership.class);
        return result;
    }

    private NamedArea handlePersistedNamedArea(NamedArea area) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        NamedArea result = handlePersisted((OrderedTermBase)area);
        handleCollection(result, NamedArea.class, "countries", Country.class);
        result.setLevel(detache(result.getLevel()));
        result.setType(detache(result.getType()));
        result.setShape(detache(result.getShape()));
        return result;
    }

    private <T extends CdmBase> CdmBase handlePersistedCdmBase(CdmBase cdmBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = (T)getTarget(cdmBase);
        if (result == null){
            return null;
        }
        cdmBase.setCreatedBy(detache(cdmBase.getCreatedBy()));
        return result;
    }

    private <T extends DefinedTermBase> T  handlePersisted(DefinedTermBase definedTermBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((TermBase)definedTermBase);

        handleCollection(result, DefinedTermBase.class, "media", Media.class);
        handleCollection(result, DefinedTermBase.class, "generalizationOf", DefinedTermBase.class);
        handleCollection(result, DefinedTermBase.class, "includes", DefinedTermBase.class);
        setInvisible(result, DefinedTermBase.class, "vocabulary", detache(result.getVocabulary()));

//        getTermService().saveOrUpdate(result);

        return result;
    }

    private DefinedTerm handlePersistedDefinedTerm(DefinedTerm term) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        DefinedTerm result = handlePersisted((DefinedTermBase)term);
        return result;
    }

    private ExtensionType handlePersistedExtensionType(ExtensionType term) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        ExtensionType result = handlePersisted((DefinedTermBase)term);
        return result;
    }

    //placeholder for not implemented methods for subclasses
    private DefinedTermBase<?> handlePersistedTerm(DefinedTermBase term) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        DefinedTermBase<?> result = handlePersisted(term);
        return result;
    }


    private TermVocabulary<?> handlePersistedVocabulary(TermVocabulary voc) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TermVocabulary<?> result = (TermVocabulary<?>)handlePersisted((TermCollection)voc);
        handleCollection(result, TermVocabulary.class, "terms", DefinedTermBase.class);
        return result;
    }

    private TermNode<?> handlePersistedTermNode(TermNode node) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        TermNode<?> result = (TermNode<?>)handlePersisted((TermRelationBase)node);
        setInvisible(result, "parent", detache(result.getParent()));
        handleCollection(result, TermNode.class, "inapplicableIf", FeatureState.class);
        handleCollection(result, TermNode.class, "onlyApplicableIf", FeatureState.class);
        handleCollection(result, TermNode.class, "inapplicableIf_old", State.class);
        handleCollection(result, TermNode.class, "onlyApplicableIf_old", State.class);
        handleCollection(result, TermNode.class, "children", TermNode.class);

        return result;
    }

    private Representation handlePersistedRepresentation(Representation representation) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        Representation result = (Representation)handlePersisted((LanguageStringBase)representation);
        return result;
    }

    private <T extends TermBase> T  handlePersisted(TermBase termBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((IdentifiableEntity)termBase);
        if (result ==null){
            return null;
        }
        handleCollection(result, TermBase.class, "representations", Representation.class);
        return result;
    }

    private <T extends TermCollection> T  handlePersisted(TermCollection termCollection) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((TermBase)termCollection);
        if (result ==null){
            return null;
        }
        handleCollection(result, TermCollection.class, "termRelations", TermRelationBase.class);
        return result;
    }

    private <T extends TermRelationBase> T  handlePersisted(TermRelationBase termRelationBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        T result = handlePersisted((VersionableEntity)termRelationBase);
        result.setTerm(detache(result.getTerm()));
        setInvisible(result, TermRelationBase.class, "graph", detache(result.getGraph()));
        return result;
    }

    private User handlePersistedUser(User user) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        User result = (User)handlePersistedCdmBase(user);
        if (result.getUsername().equals("admin")){
            result = getUserService().listByUsername("admin", MatchMode.EXACT, null, null, null, null, null).iterator().next();
            permanentCache.put(user.getUuid(), result);
            cache(result); //necessary?
            toSave.add(result);
            toSave.remove(user);
        }
        if (!result.isPersited()){
            result.setAuthorities(new HashSet<>());
            result.setGrantedAuthorities(new HashSet<>());
            setInvisible(result, "groups", new HashSet<>());
        }
        return result;
    }

    /**
     * @param result
     */
    private void cache(CdmBase cdmBase) {
       if (cdmBase instanceof User || cdmBase instanceof DefinedTermBase){
           permanentCache.put(cdmBase.getUuid(), cdmBase);
       }else{
           sessionCache.put(cdmBase.getUuid(), cdmBase);
       }
       movedObjects.add(cdmBase.getUuid());

    }

    private  Contact detache(Contact contact) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        contact = CdmBase.deproxy(contact);
        if (contact == null){
            return contact;
        }else{
            return handlePersistedContact(contact);
        }
    }

    private  IIntextReferencable detache(IIntextReferencable cdmBase, boolean onlyForDefinedSignature) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        return (IIntextReferencable)detache((CdmBase)cdmBase);
    }
    private  IIntextReferenceTarget detache(IIntextReferenceTarget cdmBase, boolean onlyForDefinedSignature) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        return (IIntextReferenceTarget)detache((CdmBase)cdmBase);
    }


    private <T extends CdmBase> T detache(T cdmBase) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException, SecurityException, IllegalArgumentException, NoSuchMethodException {
        cdmBase = CdmBase.deproxy(cdmBase);
        if (cdmBase == null ){
            return cdmBase;
        }else if(isInCache(cdmBase)){
            return getCached(cdmBase);
        }else {
            //TODO better load all existing UUIDs just in case any of the objects is already in the DB
            if (cdmBase instanceof TermBase || movedObjects.contains(cdmBase.getUuid())){
                Class<T> clazz = (Class<T>)cdmBase.getClass();
                T exists = getCommonService().find(clazz, cdmBase.getUuid());
                if (exists != null){
                    return exists;
                }else if (movedObjects.contains(cdmBase.getUuid())){
                    logger.warn("Object should be moved already but does not exist in target. This should not happen: " + cdmBase.getUuid());
                }
            }
        }
        if ( !cdmBase.isPersited()){
            logger.warn("Non persisted object not in cache and not in target DB. This should not happen: " + cdmBase.getUuid());
            return cdmBase; //should not happen anymore; either in cache or in target or persisted in source
        }else{
            return (T)handlePersisted(cdmBase);
        }
    }


    /**
     * @param cdmBase
     * @return
     */
    private boolean isInCache(CdmBase cdmBase) {
        return getCached(cdmBase) != null;
    }

    /**
     * @param cdmBase
     * @return
     */
    private <T extends CdmBase> T getCached(T cdmBase) {
        T result = (T)sessionCache.get(cdmBase.getUuid());
        if (result == null){
            result = (T)permanentCache.get(cdmBase.getUuid());
        }
        return result;
    }

    //TODO this should be cached for partition
    private <T extends CdmBase> T getTarget(T source) {
        if (source == null){
            return null;
        }
        T result = getCached(source);
//        if (result == null){
//            Class<T> clazz = (Class<T>)source.getClass();
//            result = getCommonService().find(clazz, source.getUuid());
//        }
        if (result == null){
            //Alternative: clone?
            result = CdmBase.deproxy(source);
            result.setId(0);
            cache(result);
            toSave.add(result);
        }
        return result;
    }



    @Override
    protected boolean doCheck(FauEu2CdmImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(FauEu2CdmImportState state) {
        return false;
    }

}
