/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.common.DbExportBase;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.HybridRelationship;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.query.OrderHint;
import eu.etaxonomy.cdm.strategy.cache.name.TaxonNameDefaultCacheStrategy;
import eu.etaxonomy.cdm.strategy.cache.name.ZooNameNoMarkerCacheStrategy;

/**
 * @author e.-m.lee
 * @since 12.02.2010
 */
public abstract class PesiExportBase
              extends DbExportBase<PesiExportConfigurator, PesiExportState, PesiTransformer> {

    private static final long serialVersionUID = 6226747017958138156L;
    private static final Logger logger = Logger.getLogger(PesiExportBase.class);

	protected static final boolean IS_CACHE = true;

	private static Set<NameRelationshipType> excludedRelTypes = new HashSet<>();

	private static TaxonNameDefaultCacheStrategy zooNameStrategy = ZooNameNoMarkerCacheStrategy.NewInstance();
	private static TaxonNameDefaultCacheStrategy botanicalNameStrategy = TaxonNameDefaultCacheStrategy.NewInstance();

	public PesiExportBase() {
		super();
	}

	protected <CLASS extends TaxonBase> List<CLASS> getNextTaxonPartition(Class<CLASS> clazz, int limit,
	        int partitionCount, List<String> propertyPath) {

	    List<OrderHint> orderHints = new ArrayList<>();
		orderHints.add(new OrderHint("id", OrderHint.SortOrder.ASCENDING ));

		List<CLASS> list = getTaxonService().list(clazz, limit, partitionCount * limit, orderHints, propertyPath);

		if (list.isEmpty()){
			return null;
		}

		Iterator<CLASS> it = list.iterator();
		while (it.hasNext()){
			TaxonBase<?> taxonBase = it.next();
			if (! isPesiTaxon(taxonBase)){
				it.remove();
			}
			taxonBase = null;
		}
		it = null;
		return list;
	}

	protected List<TaxonNameDescription> getNextNameDescriptionPartition(int limit, int partitionCount, List<String> propertyPath) {
		List<OrderHint> orderHints = new ArrayList<>();
		orderHints.add(new OrderHint("id", OrderHint.SortOrder.ASCENDING ));
		Pager<TaxonNameDescription> l = getDescriptionService().getTaxonNameDescriptions(null, limit, partitionCount, propertyPath);
		List<TaxonNameDescription> list = l.getRecords();
		if (list.isEmpty()){
			return null;
		}

		Iterator<TaxonNameDescription> it = list.iterator();
		while (it.hasNext()){
			TaxonNameDescription nameDescription = it.next();
			if (! isPesiNameDescriptionTaxon(nameDescription)){
				it.remove();
			}
		}
		return list;
	}

	private boolean isPesiNameDescriptionTaxon(TaxonNameDescription nameDescription) {
		TaxonName name = nameDescription.getTaxonName();
		if (isPurePesiName(name)){
			return true;
		}else{
			Set<TaxonBase> taxa = name.getTaxonBases();
			for (TaxonBase<?> taxonBase : taxa){
				if (isPesiTaxon(taxonBase)){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the next list of pure names. If finished result will be null. If list is empty there may be result in further partitions.
	 */
	protected List<TaxonName> getNextPureNamePartition(Class<TaxonName> clazz,int limit, int partitionCount) {
		List<OrderHint> orderHints = new ArrayList<>();
		orderHints.add(new OrderHint("id", OrderHint.SortOrder.ASCENDING ));
//		List<String> propPath = Arrays.asList(new String[]{"taxonBases"});

		List<TaxonName> list = getNameService().list(clazz, limit, partitionCount * limit, orderHints, null);
		if (list.isEmpty()){
			return null;
		}
		Iterator<TaxonName> it = list.iterator();
		while (it.hasNext()){
		    TaxonName taxonName = HibernateProxyHelper.deproxy(it.next());
			if (! isPurePesiName(taxonName)){
				it.remove();
			}
		}
		return list;
	}

	protected <CLASS extends RelationshipBase> List<CLASS> getNextNameRelationshipPartition(
	                Class<CLASS> clazz, int pageSize, int partitionCount, List<String> propertyPaths) {

	    List<CLASS> result = new ArrayList<>();
		List<OrderHint> orderHints = null;
		List<CLASS> list;
		if (NameRelationship.class.isAssignableFrom(clazz)){
            list = (List<CLASS>)getNameService().listNameRelationships(null, pageSize, partitionCount, orderHints, propertyPaths);
        }else if (HybridRelationship.class.isAssignableFrom(clazz)){
            list = (List<CLASS>)getNameService().listHybridRelationships(null, pageSize, partitionCount, orderHints, propertyPaths);
        }else{
            throw new RuntimeException("Only NameRelationship or HybridRelationship allowed here");
        }
		if (list.isEmpty()){
			return null;
		}
		for (CLASS rel : list){
			if (isPesiNameRelationship(rel)){
				result.add(rel);
			}
		}
		return result;
	}

	protected <CLASS extends RelationshipBase> List<CLASS> getNextTaxonRelationshipPartition( int limit, int partitionCount, List<String> propertyPaths) {

	    List<CLASS> result = new ArrayList<>();

	    List<OrderHint> orderHints = null;
		@SuppressWarnings("unchecked")
        List<CLASS> list = (List<CLASS>)this.getTaxonService()
		        .listTaxonRelationships(null, limit, partitionCount, orderHints, propertyPaths);

		if (list.isEmpty()){
			return null;
		}

		for (CLASS rel : list){
			if (isPesiTaxonOrSynonymRelationship(rel)){
				result.add(rel);
			}
		}
		return result;
	}

    protected List<TaxonNode> getNextTaxonNodePartition( int limit, int partitionCount, List<String> propertyPaths) {

        List<TaxonNode> result = new ArrayList<>();

        List<OrderHint> orderHints = null;
        List<TaxonNode> list = this.getTaxonNodeService()
            .list(TaxonNode.class, limit, limit * partitionCount, orderHints, propertyPaths);

        if (list.isEmpty()){
            return null;
        }

        for (TaxonNode tn : list){
            if (isPesiTaxonNode(tn)){
                result.add(tn);
            }
        }
        return result;
    }

    protected boolean isPesiTaxonNode(TaxonNode tn){
        TaxonBase<?> fromTaxon;
        Taxon toTaxon;

        fromTaxon = tn.getTaxon();
        toTaxon = tn.getParent()== null? null: tn.getParent().getTaxon();

        return (isPesiTaxon(fromTaxon, true) && isPesiTaxon(toTaxon, true));
    }

	protected boolean isPesiNameRelationship(RelationshipBase<?,?,?> rel){
		TaxonName name1;
		TaxonName name2;
		if (rel.isInstanceOf(HybridRelationship.class)){
			HybridRelationship hybridRel = CdmBase.deproxy(rel, HybridRelationship.class);
			name1 = hybridRel.getParentName();
			name2 = hybridRel.getHybridName();
		}else if (rel.isInstanceOf(NameRelationship.class)){
			NameRelationship nameRel = CdmBase.deproxy(rel, NameRelationship.class);
			name1 = nameRel.getFromName();
			name2 = nameRel.getToName();
		}else{
			logger.warn ("Only hybrid- and name-relationships alowed here");
			return false;
		}
		return (isPesiName(name1) && isPesiName(name2));
	}

	private boolean isPesiName(TaxonName name) {
		return hasPesiTaxon(name) || isPurePesiName(name);
	}

	protected boolean isPesiTaxonOrSynonymRelationship(RelationshipBase rel){
		TaxonBase<?> fromTaxon;
		Taxon toTaxon;
		// TODO:fix!!!
//		if (rel.isInstanceOf(SynonymRelationship.class)){
//			SynonymRelationship synRel = CdmBase.deproxy(rel, SynonymRelationship.class);
//			fromTaxon = synRel.getSynonym();
//			toTaxon = synRel.getAcceptedTaxon();
//			synRel = null;
//		}else
		if (rel.isInstanceOf(TaxonRelationship.class)){
			TaxonRelationship taxRel = CdmBase.deproxy(rel, TaxonRelationship.class);
			fromTaxon = taxRel.getFromTaxon();
			toTaxon = taxRel.getToTaxon();
			taxRel = null;
		}else{
			logger.warn ("Only synonym - and taxon-relationships allowed here");
			return false;
		}
		return (isPesiTaxon(fromTaxon, false) && isPesiTaxon(toTaxon, true));
	}

	/**
	 * Decides if a name is not used as the name part of a PESI taxon (and therefore is
	 * exported to PESI as taxon already) but is related to a name used as a PESI taxon
	 * (e.g. as basionym, orthographic variant, etc.) and therefore should be exported
	 * to PESI as part of the name relationship.
	 * @param taxonName
	 * @return
	 */
	protected boolean isPurePesiName(TaxonName taxonName){
		if (hasPesiTaxon(taxonName)){
			return false;
		}

		//from names
		for (NameRelationship rel :taxonName.getRelationsFromThisName()){
			TaxonName relatedName = rel.getToName();
			if (hasPesiTaxon(relatedName)){
				return true;
			}
		}

		//excluded relationships on to-side
		initExcludedRelTypes();

		//to names
		for (NameRelationship rel :taxonName.getRelationsToThisName()){
			//exclude certain types
			if (excludedRelTypes.contains(rel.getType())){
				continue;
			}
			TaxonName relatedName = rel.getFromName();
			if (hasPesiTaxon(relatedName)){
				return true;
			}
		}

		//include hybrid parents, but no childs

		for (HybridRelationship rel : taxonName.getHybridParentRelations()){
			INonViralName child = rel.getHybridName();
			if (hasPesiTaxon(child)){
				return true;
			}
		}

		return false;
	}


	private void initExcludedRelTypes() {
		if (excludedRelTypes.isEmpty()){
			excludedRelTypes.add(NameRelationshipType.BASIONYM());
			excludedRelTypes.add(NameRelationshipType.REPLACED_SYNONYM());
			excludedRelTypes.add(NameRelationshipType.ORTHOGRAPHIC_VARIANT());
		}
	}


	/**
	 * Decides if a given name has "PESI taxa" attached.
	 *
	 * @see #getPesiTaxa(TaxonNameBase)
	 * @see #isPesiTaxon(TaxonBase)
	 * @param taxonName
	 * @return
	 */
	protected boolean hasPesiTaxon(INonViralName taxonName) {
		for (TaxonBase<?> taxon : taxonName.getTaxonBases()){
			if (isPesiTaxon(taxon)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns those concepts (taxon bases) for the given name that
	 * are pesi taxa.
	 *
	 *  @see #isPesiTaxon(TaxonBase)
	 * @param name
	 * @return
	 */
	protected Set<TaxonBase<?>> getPesiTaxa(TaxonName name){
		Set<TaxonBase<?>> result = new HashSet<>();
		for (TaxonBase<?> taxonBase : name.getTaxonBases()){
			if (isPesiTaxon(taxonBase)){
				result.add(taxonBase);
			}
		}
		return result;
	}

	/**
	 * @see #isPesiTaxon(TaxonBase, boolean)
	 * @return
	 */
	protected static boolean isPesiTaxon(TaxonBase taxonBase) {
		return isPesiTaxon(taxonBase, false);
	}

	/**
	 * Checks if this taxon base is a taxon that is to be exported to PESI. This is generally the case
	 * but not for taxa that are marked as "unpublish". Synonyms and misapplied names are exported if they are
	 * related at least to one accepted taxon that is also exported, except for those misapplied names
	 * marked as misapplied names created by Euro+Med common names ({@linkplain http://dev.e-taxonomy.eu/trac/ticket/2786} ).
	 * The list of conditions may change in future.
	 * @param taxonBase
	 * @return
	 */
	protected static boolean isPesiTaxon(TaxonBase<?> taxonBase, boolean excludeMisappliedNames) {
		if (taxonBase == null){
		    return false;
		}
	    //handle accepted taxa
		if (taxonBase.isInstanceOf(Taxon.class)){
			Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
			return isPesiAcceptedTaxon(excludeMisappliedNames, taxon);
		//handle synonyms
		}else if (taxonBase.isInstanceOf(Synonym.class)){
			Synonym synonym = CdmBase.deproxy(taxonBase, Synonym.class);
			return isPesiSynonym(synonym);
		}else {
			throw new RuntimeException("Unknown taxon base type: " + taxonBase.getClass());
		}
	}

    private static boolean isPesiSynonym(Synonym synonym) {
        boolean hasAcceptedPesiTaxon = false;

        hasAcceptedPesiTaxon = isPesiTaxon(synonym.getAcceptedTaxon());
        if (!hasAcceptedPesiTaxon) {if (logger.isDebugEnabled()){logger.debug("Synonym has no accepted PESI taxon: " +  synonym.getUuid() + ", (" +  synonym.getTitleCache() + ")");}}

        return hasAcceptedPesiTaxon && synonym.isPublish();
    }

    private static boolean isPesiAcceptedTaxon(boolean excludeMisappliedNames, Taxon taxon) {
        if (! taxon.isPublish()){
        	return false;
        }

        //handle PESI accepted taxa
        if (! taxon.isMisapplication()){
        	for (Marker marker : taxon.getMarkers()){
        		if (marker.getValue() == false && marker.getMarkerType().equals(MarkerType.PUBLISH())){
        			return false;
        		}
        	}
        	return true;
        //handle misapplied names
        }else{
        	if (excludeMisappliedNames){
        		return false;
        	}
        	for (TaxonRelationship taxRel : taxon.getRelationsFromThisTaxon()){
        		if (taxRel.getType().isAnyMisappliedName()){
//						logger.warn(taxRel.getUuid() + "; " + taxRel.getToTaxon().getUuid() + " + " + taxRel.getToTaxon().getTitleCache());
        			if (isPesiTaxon(taxRel.getToTaxon(), true)){
        				return true;
        			}
        		}
        	}
        	if (logger.isDebugEnabled()){ logger.debug("Misapplied name has no accepted PESI taxon: " +  taxon.getUuid() + ", (" +  taxon.getTitleCache() + ")");}
        	return false;
        }
    }

	@Override
    protected Object getDbIdCdmWithExceptions(CdmBase cdmBase, PesiExportState state) {
		if (cdmBase.isInstanceOf(TaxonName.class)){
		    TaxonName name = CdmBase.deproxy(cdmBase, TaxonName.class);
		    if (name.getTaxonBases().size()>1){
		        logger.warn("Name has multiple taxa. Can't define correct ID. Use first one." + name.getUuid());
		    }
		    if (!name.getTaxonBases().isEmpty()){
		        TaxonBase<?> tb = name.getTaxonBases().iterator().next();
		        return this.getDbId(tb, state);
            }else{
                return ( cdmBase.getId() + state.getConfig().getNameIdStart() );
            }
		}else if (isAdditionalSource(cdmBase) ){
			return ( cdmBase.getId() + 2 * state.getConfig().getNameIdStart() );  //make it a separate variable if conflicts occur.
		}else{
			return super.getDbIdCdmWithExceptions(cdmBase, state);
		}
	}

	private boolean isAdditionalSource(CdmBase cdmBase) {
		if (cdmBase.isInstanceOf(TextData.class)){
			TextData textData = CdmBase.deproxy(cdmBase, TextData.class);
			if (textData.getFeature().equals(Feature.ADDITIONAL_PUBLICATION()) ||
					textData.getFeature().equals(Feature.CITATION())){
				return true;
			}
		}
		return false;
	}

	protected MarkerType getUuidMarkerType(UUID uuid, PesiExportState state){
		if (uuid == null){
			uuid = UUID.randomUUID();
		}

		MarkerType markerType = state.getMarkerType(uuid);
		if (markerType == null){
			if (uuid.equals(PesiTransformer.uuidMarkerGuidIsMissing)){
				markerType = MarkerType.NewInstance("Uuid is Missing", "Uuid is missing", null);
				markerType.setUuid(uuid);
			} else if (uuid.equals(PesiTransformer.uuidMarkerTypeHasNoLastAction)){
				markerType = MarkerType.NewInstance("Has no last Action", "Has no last action", null);
				markerType.setUuid(uuid);
			}
		}

		state.putMarkerType(markerType);
		return markerType;
	}

	protected static TaxonNameDefaultCacheStrategy getCacheStrategy(TaxonName taxonName) {
	    TaxonNameDefaultCacheStrategy cacheStrategy;
		if (taxonName.isZoological()){
			cacheStrategy = zooNameStrategy;
		}else if (taxonName.isBotanical()) {
			cacheStrategy = botanicalNameStrategy;
		}else{
			logger.error("Unhandled taxon name type. Can't define strategy class");
			cacheStrategy = botanicalNameStrategy;
		}
		return cacheStrategy;
	}

	/**
	 * Checks whether a given taxon is a misapplied name.
	 * @param taxon The {@link TaxonBase Taxon}.
	 * @return Whether the given TaxonName is a misapplied name or not.
	 */
	protected static boolean isMisappliedName(TaxonBase<?> taxon) {
		return getAcceptedTaxonForMisappliedName(taxon) != null;
	}

	/**
     * Checks whether a given taxon is a pro parte or partial synonym.
     * @param taxon The {@link TaxonBase Taxon}.
     * @return <code>true</code> if the the given taxon is a pp or partial synonym
     */
    protected static boolean isProParteOrPartialSynonym(TaxonBase<?> taxon) {
        return getAcceptedTaxonForProParteSynonym(taxon) != null;
    }

	/**
	 * Returns the first accepted taxon for this misapplied name.
	 * If this misapplied name is not a misapplied name, <code>null</code> is returned.
	 * @param taxon The {@link TaxonBase Taxon}.
	 */
	protected static Taxon getAcceptedTaxonForMisappliedName(TaxonBase<?> taxon) {
		if (! taxon.isInstanceOf(Taxon.class)){
			return null;
		}
		Set<TaxonRelationship> taxonRelations = CdmBase.deproxy(taxon, Taxon.class).getRelationsFromThisTaxon();
		for (TaxonRelationship taxonRelationship : taxonRelations) {
			TaxonRelationshipType taxonRelationshipType = taxonRelationship.getType();
			if (taxonRelationshipType.isAnyMisappliedName()) {
				return taxonRelationship.getToTaxon();
			}
		}
		return null;
	}

    protected static Taxon getAcceptedTaxonForProParteSynonym(TaxonBase<?> taxon) {
        if (! taxon.isInstanceOf(Taxon.class)){
            return null;
        }
        Set<TaxonRelationship> taxonRelations = CdmBase.deproxy(taxon, Taxon.class).getRelationsFromThisTaxon();
        for (TaxonRelationship taxonRelationship : taxonRelations) {
            TaxonRelationshipType taxonRelationshipType = taxonRelationship.getType();
            if (taxonRelationshipType.isAnySynonym()) {
                return taxonRelationship.getToTaxon();
            }
        }
        return null;
    }

    protected List<AnnotationType> getLastActionAnnotationTypes() {
        Set<UUID> uuidSet = new HashSet<>();
        uuidSet.add(DbLastActionMapper.uuidAnnotationTypeLastActionDate);
        uuidSet.add(DbLastActionMapper.uuidAnnotationTypeLastAction);
        uuidSet.add(ErmsTransformer.uuidAnnSpeciesExpertName);
        @SuppressWarnings({"unchecked","rawtypes"})
        List<AnnotationType> result = (List)getTermService().find(uuidSet);
        return result;
    }

    protected enum PesiSource{
        EM,
        FE,
        ERMS,
        IF;

        private PesiSource(){

        }
    }

    /**
     * Returns the source (E+M, Fauna Europaea, Index Fungorum, ERMS) of a given
     * Identifiable Entity as an {@link EnumSet enum set}
     */
    protected static EnumSet<PesiSource> getSources(IdentifiableEntity<?> entity){
        EnumSet<PesiSource> result = EnumSet.noneOf(PesiSource.class);

        Set<IdentifiableSource> sources = getPesiSources(entity);
        for (IdentifiableSource source : sources) {
            Reference ref = source.getCitation();
            UUID refUuid = ref.getUuid();
            if (refUuid.equals(PesiTransformer.uuidSourceRefEuroMed)){
                result.add(PesiSource.EM);
            }else if (refUuid.equals(PesiTransformer.uuidSourceRefFaunaEuropaea)){
                result.add(PesiSource.FE);
            }else if (refUuid.equals(PesiTransformer.uuidSourceRefErms)){
                result.add(PesiSource.ERMS);
            }else if (refUuid.equals(PesiTransformer.uuidSourceRefIndexFungorum)){
                result.add(PesiSource.IF);
            }else{
                if (logger.isDebugEnabled()){logger.debug("Not a PESI source");}
            }
        }
        return result;
    }

    /**
     * Returns the Sources for a given TaxonName only.
     * @param identifiableEntity
     * @return The Sources.
     */
    protected static Set<IdentifiableSource> getPesiSources(IdentifiableEntity<?> identifiableEntity) {
        Set<IdentifiableSource> sources = new HashSet<>();

        //Taxon Names
        if (identifiableEntity.isInstanceOf(TaxonName.class)){
            // Sources from TaxonName
            TaxonName taxonName = CdmBase.deproxy(identifiableEntity, TaxonName.class);
            Set<IdentifiableSource> testSources = identifiableEntity.getSources();
            sources = filterPesiSources(testSources);

            if (sources.size() == 0 && testSources.size()>0){
                IdentifiableSource source = testSources.iterator().next();
                logger.warn("There are sources, but they are no pesi sources!!!" + source.getIdInSource() + " - " + source.getIdNamespace() + " - " + (source.getCitation()== null? "no reference" : source.getCitation().getTitleCache()));
            }
            if (sources.size() > 1) {
                logger.warn("This TaxonName has more than one Source: " + identifiableEntity.getUuid() + " (" + identifiableEntity.getTitleCache() + ")");
            }

            // name has no PESI source, take sources from TaxonBase
            if (sources == null || sources.isEmpty()) {
                Set<TaxonBase> taxa = taxonName.getTaxonBases();
                for (TaxonBase<?> taxonBase: taxa){
                    sources.addAll(filterPesiSources(taxonBase.getSources()));
                }
            }

        //for TaxonBases
        }else if (identifiableEntity.isInstanceOf(TaxonBase.class)){
            sources = filterPesiSources(identifiableEntity.getSources());
        } else {
            sources = filterPesiSources(identifiableEntity.getSources());
        }

        return sources;
    }

    // return all sources with a PESI reference
    protected static Set<IdentifiableSource> filterPesiSources(Set<? extends IdentifiableSource> sources) {
        Set<IdentifiableSource> result = new HashSet<>();
        for (IdentifiableSource source : sources){
            Reference ref = source.getCitation();
            if (ref != null){
                UUID refUuid = ref.getUuid();
                if (refUuid.equals(PesiTransformer.uuidSourceRefEuroMed) ||
                        refUuid.equals(PesiTransformer.uuidSourceRefFaunaEuropaea)||
                        refUuid.equals(PesiTransformer.uuidSourceRefErms)||
                        refUuid.equals(PesiTransformer.uuidSourceRefIndexFungorum) ||
                        refUuid.equals(PesiTransformer.uuidSourceRefAuct)){
                    result.add(source);
                }
            }
        }
        return result;
    }

}
