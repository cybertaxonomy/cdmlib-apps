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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.DbAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbFixedIntegerMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbFixedStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.name.HybridRelationship;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalSource;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;

/**
 * The export class for relations between {@link eu.etaxonomy.cdm.model.taxon.TaxonBase TaxonBases}.<p>
 * Inserts into DataWarehouse database table <code>RelTaxon</code>.
 * @author e.-m.lee
 * @since 23.02.2010
 */
@Component
public class PesiRelTaxonExport extends PesiExportBase {

    private static final long serialVersionUID = 67808745337549629L;
    private static Logger logger = LogManager.getLogger();

    private static final Class<? extends CdmBase> standardMethodParameter = RelationshipBase.class;

	private static int modCount = 1000;
	private static final String dbTableName = "RelTaxon";
	private static final String pluralString = "Relationships";
	private PesiExportMapping mapping;
	private PesiExportMapping synonymMapping;
	private PesiExportMapping taxonNodeMapping;
	private PesiExportMapping originalSpellingMapping;

	public PesiRelTaxonExport() {
		super();
	}

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}

	@Override
	protected void doInvoke(PesiExportState state) {
		try {
			logger.info("*** Started Making " + pluralString + " ...");

			doDelete(state);

			// Stores whether this invoke was successful or not.
			boolean success = true;

			// PESI: Clear the database table RelTaxon.
			//doDelete(state); -> done by stored procedure

			// Get specific mappings: (CDM) Relationship -> (PESI) RelTaxon
			mapping = getMapping();
			mapping.initialize(state);

			// Get specific mappings: (CDM) Synonym -> (PESI) RelTaxon
            synonymMapping = getSynonymMapping();
            synonymMapping.initialize(state);

            // Get specific mappings: (CDM) Synonym -> (PESI) RelTaxon
            taxonNodeMapping = getTaxonNodeMapping();
            taxonNodeMapping.initialize(state);

			//Export taxon relations
			success &= doPhase01(state, mapping);

			//Export taxon nodes
            success &= doPhase01b(state, taxonNodeMapping);

			// Export name relations
			success &= doPhase02(state, mapping);

	         // Export synonym relations (directly attached to taxa)
            success &= doPhase03(state, synonymMapping);

			if (! success){
				state.getResult().addError("An unknown error occurred in PesiRelTaxonExport");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.getResult().addException(e);
			return;
		}
	}

    private boolean doPhase03(PesiExportState state, PesiExportMapping synonymMapping) {
        logger.info("PHASE 3: Direct Synonym Relationships ...");

        boolean success = true;
        int limit = state.getConfig().getLimitSave();
        // Start transaction
        TransactionStatus txStatus = startTransaction(true);
        logger.debug("Started new transaction. Fetching some synonyms (max: " + limit + ") ...");

        List<Synonym> list;

        //taxon relations
        int partitionCount = 0;
        int totalCount = 0;
        while ((list = getNextTaxonPartition(Synonym.class, limit, partitionCount++, null))!= null) {
            totalCount = totalCount + list.size();
            logger.info("Read " + list.size() + " PESI synonyms. Limit: " + limit + ". Total: " + totalCount );
//          if (list.size() > 0){
//              logger.warn("First relation type is " + list.get(0).getType().getTitleCache());
//          }
            for (Synonym synonym : list){
                try {
                    synonymMapping.invoke(synonym);
                } catch (Exception e) {
                    logger.error(e.getMessage() + ". Synonym: " +  synonym.getUuid());
                    e.printStackTrace();
                }
            }

            commitTransaction(txStatus);
            txStatus = startTransaction();
        }
        commitTransaction(txStatus);
        return success;
    }

    private boolean doPhase01(PesiExportState state, PesiExportMapping mapping) {
		logger.info("PHASE 1: Taxon Relationships ...");
		boolean success = true;

		int limit = state.getConfig().getLimitSave();
		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		logger.debug("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");

		List<RelationshipBase<?,?,?>> list;

		//taxon relations
		int partitionCount = 0;
		int totalCount = 0;
		while ((list = getNextTaxonRelationshipPartition(limit, partitionCount++, null)) != null) {
			totalCount = totalCount + list.size();
			logger.info("Read " + list.size() + " PESI relations. Limit: " + limit + ". Total: " + totalCount );
//			if (list.size() > 0){
//				logger.warn("First relation type is " + list.get(0).getType().getTitleCache());
//			}
			for (RelationshipBase<?,?,?> rel : list){
				try {
					mapping.invoke(rel);
				} catch (Exception e) {
					logger.error(e.getMessage() + ". Relationship: " +  rel.getUuid());
					e.printStackTrace();
				}
			}

			commitTransaction(txStatus);
			txStatus = startTransaction();
		}
		commitTransaction(txStatus);
		return success;
	}

    private boolean doPhase01b(PesiExportState state, PesiExportMapping taxonNodeMapping) {
        logger.info("PHASE 1b: Taxonnodes ...");
        boolean success = true;

        int limit = state.getConfig().getLimitSave();
        // Start transaction
        TransactionStatus txStatus = startTransaction(true);
        logger.debug("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");

        List<TaxonNode> list;

        //taxon nodes
        int partitionCount = 0;
        int totalCount = 0;
        while ((list = getNextTaxonNodePartition(limit, partitionCount++, null)) != null) {
            totalCount = totalCount + list.size();
            logger.info("Read " + list.size() + " PESI taxon nodes. Limit: " + limit + ". Total: " + totalCount );
            for (TaxonNode tn : list){
                try {
                    taxonNodeMapping.invoke(tn);
                } catch (Exception e) {
                    logger.error(e.getMessage() + ". TaxonNode: " +  tn.getUuid());
                    e.printStackTrace();
                }
            }

            commitTransaction(txStatus);
            txStatus = startTransaction();
        }
        commitTransaction(txStatus);
        return success;
    }

	private boolean doPhase02(PesiExportState state, PesiExportMapping mapping2) {

	    logger.info("PHASE 2: Name Relationships ...");
		boolean success = true;

		int limit = state.getConfig().getLimitSave();
		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		logger.debug("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");

		//name relations
		List<NameRelationship> list;
		int partitionCount = 0;
		while ((list = getNextNameRelationshipPartition(NameRelationship.class, limit, partitionCount++, null)) != null   ) {
			txStatus = handleNameRelationList(state, txStatus, list);
		}

        //hybrid relations
		logger.info("PHASE 2b: ... Hybrid Relationships ...");
        List<HybridRelationship> hybridList;
		partitionCount = 0;
        while ((hybridList = getNextNameRelationshipPartition(HybridRelationship.class, limit, partitionCount++, null)) != null   ) {
            txStatus = handleNameRelationList(state, txStatus, hybridList);
        }

        //original spellings
        logger.info("PHASE 2c: ... Original Spellings ...");
        List<NomenclaturalSource> originalSpellingList;
        partitionCount = 0;
        while ((originalSpellingList = getNextOriginalSpellingPartition(limit, partitionCount++, null)) != null   ) {
            txStatus = handleOriginalSpellingList(state, txStatus, originalSpellingList);
        }

        commitTransaction(txStatus);
		logger.info("End PHASE 2: Name Relationships ...");
		state.setCurrentFromObject(null);
		state.setCurrentToObject(null);
		return success;
	}

    private TransactionStatus handleNameRelationList(PesiExportState state, TransactionStatus txStatus,
            List<? extends RelationshipBase<?,?,?>> list) {
        for (RelationshipBase<?,?,?> rel : list){
        	try {
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
        			logger.warn ("Only hybrid- and name-relationships allowed here");
        			continue;
        		}
        		List<IdentifiableEntity> fromList = new ArrayList<>();
        		List<IdentifiableEntity> toList = new ArrayList<>();
        		makeList(name1, fromList);
        		makeList(name2, toList);

        		for (IdentifiableEntity<?> fromEntity : fromList){
        			for (IdentifiableEntity<?> toEntity : toList){
        				//TODO set entities to state
        				state.setCurrentFromObject(fromEntity);
        				state.setCurrentToObject(toEntity);
        				mapping.invoke(rel);
        			}
        		}
        	} catch (Exception e) {
        		logger.error(e.getMessage() + ". Relationship: " +  rel.getUuid());
        		e.printStackTrace();
        	}
        }
        commitTransaction(txStatus);
        txStatus = startTransaction();
        return txStatus;
    }

    private TransactionStatus handleOriginalSpellingList(PesiExportState state, TransactionStatus txStatus,
            List<NomenclaturalSource> list) {
        for (NomenclaturalSource nomSource : list){
            try {
                TaxonName name1 = nomSource.getNameUsedInSource();
                TaxonName name2 = nomSource.getSourcedName();
                List<IdentifiableEntity> fromList = new ArrayList<>();
                List<IdentifiableEntity> toList = new ArrayList<>();
                makeList(name1, fromList);
                makeList(name2, toList);

                for (IdentifiableEntity<?> fromEntity : fromList){
                    for (IdentifiableEntity<?> toEntity : toList){
                        //TODO set entities to state
                        state.setCurrentFromObject(fromEntity);
                        state.setCurrentToObject(toEntity);
                        originalSpellingMapping.invoke(nomSource);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage() + ". NomenclaturalSource: " +  nomSource.getUuid());
                e.printStackTrace();
            }
        }
        commitTransaction(txStatus);
        txStatus = startTransaction();
        return txStatus;
    }


	private void makeList(TaxonName name, List<IdentifiableEntity> list) {
		if (! hasPesiTaxon(name)){
			list.add(name);
		}else{
			for (TaxonBase<?> taxon: getPesiTaxa(name)){
				list.add(taxon);
			}
		}
	}

	/**
	 * Deletes all entries of database tables related to <code>RelTaxon</code>.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return Whether the delete operation was successful or not.
	 */
	protected boolean doDelete(PesiExportState state) {
		PesiExportConfigurator pesiConfig = state.getConfig();

		Source destination =  pesiConfig.getDestination();

		// Clear RelTaxon
		String sql = "DELETE FROM " + dbTableName;
		destination.update(sql);
		return true;
	}

	/**
	 * Returns the <code>TaxonFk1</code> attribute. It corresponds to a CDM <code>TaxonRelationship</code>.
	 * @param relationship The {@link RelationshipBase Relationship}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>TaxonFk1</code> attribute.
	 * @see MethodMapper
	 */
    @SuppressWarnings("unused")
	private static Integer getTaxonFk1(RelationshipBase<?, ?, ?> relationship, PesiExportState state) {
		return getObjectFk(relationship, state, true);
	}

	/**
     * Returns the <code>TaxonFk1</code> attribute. It corresponds to a CDM <code>TaxonRelationship</code>.
     * @param relationship The {@link RelationshipBase Relationship}.
     * @param state The {@link PesiExportState PesiExportState}.
     * @return The <code>TaxonFk1</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getTaxonFk1(Synonym synonym, PesiExportState state) {
        return synonym.getAcceptedTaxon().getId();
    }
    /**
     * Returns the <code>TaxonFk1</code> attribute. It corresponds to a CDM <code>Synonym</code>.
     * @param synonym The {@link Synonym synonym}.
     * @param state The {@link PesiExportState PesiExportState}.
     * @return The <code>TaxonFk1</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getSynonym(Synonym synonym, PesiExportState state) {
        return state.getDbId(synonym);
    }

    /**
     * Returns the <code>TaxonFk1</code> attribute. It corresponds to a CDM <code>Synonym</code>.
     * @param synonym The {@link Synonym synonym}.
     * @param state The {@link PesiExportState PesiExportState}.
     * @return The <code>TaxonFk1</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getParent(TaxonNode taxonNode, PesiExportState state) {
        TaxonNode parent = taxonNode == null? null : taxonNode.getParent();
        Taxon parentTaxon = parent == null? null: parent.getTaxon();
        return state.getDbId(parentTaxon);
    }

    /**
     * Returns the id of the previously defined fromObject.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getFromObject(PesiExportState state) {
        return state.getDbId(state.getCurrentFromObject());
    }
    /**
     * Returns the id of the previously defined fromObject.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getToObject(PesiExportState state) {
        return state.getDbId(state.getCurrentToObject());
    }

	/**
	 * Returns the <code>TaxonFk2</code> attribute. It corresponds to a CDM <code>SynonymRelationship</code>.
	 * @param relationship The {@link RelationshipBase Relationship}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>TaxonFk2</code> attribute.
	 * @see MethodMapper
	 */
    @SuppressWarnings("unused")
	private static Integer getTaxonFk2(RelationshipBase<?, ?, ?> relationship, PesiExportState state) {
		return getObjectFk(relationship, state, false);
	}

	/**
	 * Returns the <code>RelTaxonQualifierFk</code> attribute.
	 * @param relationship The {@link RelationshipBase Relationship}.
	 * @return The <code>RelTaxonQualifierFk</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static Integer getRelTaxonQualifierFk(RelationshipBase<?, ?, ?> relationship) {
		return PesiTransformer.taxonRelation2RelTaxonQualifierFk(relationship);
	}

    @SuppressWarnings("unused")
    private static Integer getRelTaxonQualifierFk(Synonym synonym) {
        return PesiTransformer.synonym2RelTaxonQualifierFk(synonym);
    }

	/**
	 * Returns the <code>RelQualifierCache</code> attribute.
	 * @param relationship The {@link RelationshipBase Relationship}.
	 * @return The <code>RelQualifierCache</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getRelQualifierCache(RelationshipBase<?, ?, ?> relationship, PesiExportState state) {
		String result = null;
		NomenclaturalCode code = null;
		Taxon taxon = null;
		TaxonName name= null;
		if (relationship.isInstanceOf(TaxonRelationship.class)){
			TaxonRelationship rel = CdmBase.deproxy(relationship, TaxonRelationship.class);
			taxon = rel.getToTaxon();
			name = taxon.getName();
			code = name.getNameType();
		}else if (relationship.isInstanceOf(NameRelationship.class)){
			NameRelationship rel = CdmBase.deproxy(relationship,  NameRelationship.class);
			name = rel.getFromName();
			code =name.getNameType();
		}else if (relationship.isInstanceOf(HybridRelationship.class)){
			HybridRelationship rel =  CdmBase.deproxy(relationship,  HybridRelationship.class);
			name = rel.getParentName();
			code = name.getNameType();
		}
		if (code != null) {
			result = state.getConfig().getTransformer().getCacheByRelationshipType(relationship, code);
		} else {
			logger.error("NomenclaturalCode is NULL while creating the following relationship: " + relationship.getUuid());
		}
		return result;
	}


	/**
	 * Returns the database key of an object in the given relationship.
	 * @param relationship {@link RelationshipBase RelationshipBase}.
	 * @param state {@link PesiExportState PesiExportState}.
	 * @param isFrom A boolean value indicating whether the database key of the parent or child in this relationship is searched. <code>true</code> means the child is searched. <code>false</code> means the parent is searched.
	 * @return The database key of an object in the given relationship.
	 */
	private static Integer getObjectFk(RelationshipBase<?, ?, ?> relationship, PesiExportState state, boolean isFrom) {
		TaxonBase<?> taxonBase = null;
		if (relationship.isInstanceOf(TaxonRelationship.class)) {
			TaxonRelationship tr = (TaxonRelationship)relationship;
			taxonBase = (isFrom) ? tr.getFromTaxon():  tr.getToTaxon();
		} else if (relationship.isInstanceOf(NameRelationship.class)
		        || relationship.isInstanceOf(HybridRelationship.class)) {
			if (isFrom){
				return state.getDbId(state.getCurrentFromObject());
			}else{
				return state.getDbId(state.getCurrentToObject());
			}
		}
		if (taxonBase != null) {
			if (! isPesiTaxon(taxonBase)){
				logger.warn("Related taxonBase is not a PESI taxon. Taxon: " + taxonBase.getId() + "/" + taxonBase.getUuid() + "; TaxonRel: " +  relationship.getId() + "(" + relationship.getType().getTitleCache() + ")");
				return null;
			}else{
				return state.getDbId(taxonBase);
			}

		}
		logger.warn("No taxon found in state for relationship: " + relationship.toString());
		return null;
	}

   private static Integer getObjectFk(NomenclaturalSource nomSource, PesiExportState state, boolean isFrom) {
        if (isFrom){
            return state.getDbId(state.getCurrentFromObject());
        }else{
            return state.getDbId(state.getCurrentToObject());
        }
    }

    @SuppressWarnings("unused")  //for synonym mapping
    private static String getSynonymTypeCache(Synonym synonym, PesiExportState state) {
        String result = null;
        NomenclaturalCode code = null;
        code = synonym.getAcceptedTaxon().getName().getNameType();

        if (code != null) {
            result = state.getConfig().getTransformer().getCacheBySynonymType(synonym, code);
        } else {
            logger.error("NomenclaturalCode is NULL while creating the following synonym: " + synonym.getUuid());
        }
        return result;
    }

	/**
	 * Returns the CDM to PESI specific export mappings.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	PesiExportMapping getMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbTableName);

		mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getTaxonFk1", standardMethodParameter, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("TaxonFk2", this.getClass(), "getTaxonFk2", standardMethodParameter, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("RelTaxonQualifierFk", this));
		mapping.addMapper(MethodMapper.NewInstance("RelQualifierCache", this, RelationshipBase.class, PesiExportState.class));
		mapping.addMapper(DbAnnotationMapper.NewExludedInstance(getLastActionAnnotationTypes(), "Notes"));

		return mapping;
	}

	/**
     * Returns the CDM to PESI specific export mappings.
     * @return The {@link PesiExportMapping PesiExportMapping}.
     */
    PesiExportMapping getSynonymMapping() {

        PesiExportMapping mapping = new PesiExportMapping(dbTableName);

        mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getSynonym", Synonym.class, PesiExportState.class));
        mapping.addMapper(DbObjectMapper.NewInstance("acceptedTaxon", "TaxonFk2"));
        mapping.addMapper(MethodMapper.NewInstance("RelTaxonQualifierFk", this, Synonym.class));
        mapping.addMapper(MethodMapper.NewInstance("RelQualifierCache", this.getClass(), "getSynonymTypeCache", Synonym.class, PesiExportState.class));
        mapping.addMapper(DbAnnotationMapper.NewExludedInstance(getLastActionAnnotationTypes(), "Notes"));

        return mapping;
    }

    PesiExportMapping getTaxonNodeMapping() {

        PesiExportMapping mapping = new PesiExportMapping(dbTableName);

        mapping.addMapper(MethodMapper.NewInstance("TaxonFk2", this.getClass(), "getParent", TaxonNode.class, PesiExportState.class));
        mapping.addMapper(DbObjectMapper.NewInstance("taxon", "TaxonFk1"));
        mapping.addMapper(DbFixedIntegerMapper.NewInstance(PesiTransformer.IS_TAXONOMICALLY_INCLUDED_IN, "RelTaxonQualifierFk"));
        mapping.addMapper(DbFixedStringMapper.NewInstance("is taxonomically included in", "RelQualifierCache"));
//        mapping.addMapper(DbAnnotationMapper.NewExludedInstance(getLastActionAnnotationTypes(), "Notes"));

        return mapping;
    }

    PesiExportMapping getOriginalSpellingMapping() {

        PesiExportMapping mapping = new PesiExportMapping(dbTableName);

        mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getFromObject", PesiExportState.class));
        mapping.addMapper(MethodMapper.NewInstance("TaxonFk2", this.getClass(), "getToObject", PesiExportState.class));
        mapping.addMapper(DbFixedIntegerMapper.NewInstance(PesiTransformer.IS_ORIGINAL_SPELLING_FOR, "RelTaxonQualifierFk"));
        mapping.addMapper(DbFixedStringMapper.NewInstance("is original spelling for", "RelQualifierCache"));
//        mapping.addMapper(DbAnnotationMapper.NewExludedInstance(getLastActionAnnotationTypes(), "Notes"));

        return mapping;
    }

    @Override
    protected boolean doCheck(PesiExportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoRelTaxa();
    }
}