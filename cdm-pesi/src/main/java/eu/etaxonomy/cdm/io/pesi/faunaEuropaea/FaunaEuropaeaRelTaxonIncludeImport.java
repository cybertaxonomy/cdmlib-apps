/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import static eu.etaxonomy.cdm.io.pesi.faunaEuropaea.FaunaEuropaeaTransformer.A_AUCT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.MapWrapper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
//import eu.etaxonomy.cdm.profiler.ProfilerController;
import eu.etaxonomy.cdm.model.taxon.TaxonNodeAgentRelation;



/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
@Component
public class FaunaEuropaeaRelTaxonIncludeImport extends FaunaEuropaeaImportBase  {

	public static final String OS_NAMESPACE_TAXON = "Taxon";
	private static final String AUCT_STRING = "auct.";
	private static final Logger logger = Logger.getLogger(FaunaEuropaeaRelTaxonIncludeImport.class);
	//private static final String acceptedTaxonUUID = "A9C24E42-69F5-4681-9399-041E652CF338"; // any accepted taxon uuid, taken from original fauna europaea database
	//private static final String acceptedTaxonUUID = "E23E6295-836A-4332-BF72-7D29949C7C60"; //faunaEu_1_3
	//private static final String acceptedTaxonUUID = "bab7642e-f733-4a21-848d-a15250d2f4ed"; //for faunEu (2.4)
	private static final String acceptedTaxonUUID = "DADA6F44-B7B5-4C0A-9F32-980F54B02C36"; // for MfNFaunaEuropaea

	private Map<UUID, TeamOrPersonBase> agentMap = new HashMap<UUID, TeamOrPersonBase>();


	private Reference sourceRef;
    private final String parentPluralString = "Taxa";
	private static String ALL_SYNONYM_FROM_CLAUSE = " FROM Taxon INNER JOIN Taxon AS Parent " +
	" ON Taxon.TAX_TAX_IDPARENT = Parent.TAX_ID " +
	" WHERE (Taxon.TAX_VALID = 0) " +
	" AND (Taxon.TAX_AUT_ID <> " + A_AUCT + " OR Taxon.TAX_AUT_ID IS NULL)";

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		boolean result = true;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		logger.warn("Checking for Taxa not yet fully implemented");
		result &= checkTaxonStatus(fauEuConfig);

		return result;
	}

	@Override
	protected boolean isIgnore(FaunaEuropaeaImportState state) {
		return ! (state.getConfig().isDoTaxonomicallyIncluded() ||
		state.getConfig().isDoMisappliedNames() || state.getConfig().isDoHeterotypicSynonyms() || state.getConfig().isDoInferredSynonyms());
	}

	private boolean checkTaxonStatus(FaunaEuropaeaImportConfigurator fauEuConfig) {
		boolean result = true;
//		try {
			Source source = fauEuConfig.getSource();
			String sqlStr = "";
			ResultSet rs = source.getResultSet(sqlStr);
			return result;
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return false;
//		}
	}

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {

		/*logger.warn("Start RelTaxon doInvoke");
		ProfilerController.memorySnapshot();
		*/
	    TransactionStatus txStatus = startTransaction();
	    if (state.getAgentMap().isEmpty()){
	        createAgentMap(state);
	    }
		Map<String, MapWrapper<? extends CdmBase>> stores = state.getStores();

		MapWrapper<TeamOrPersonBase> authorStore = (MapWrapper<TeamOrPersonBase>)stores.get(ICdmIO.TEAM_STORE);
		authorStore.makeEmpty();

		if(logger.isInfoEnabled()) { logger.info("Start making relationships..."); }



		// the uuid of an accepted taxon is needed here. any accepted taxon will do.
		TaxonBase taxon = getTaxonService().find(UUID.fromString(acceptedTaxonUUID));
		sourceRef = taxon.getSec();

		Classification tree = getClassificationFor(state, sourceRef);
		commitTransaction(txStatus);

		logger.warn("Before processParentsChildren " + state.getConfig().isDoTaxonomicallyIncluded());

		//ProfilerController.memorySnapshot();

		if (state.getConfig().isDoTaxonomicallyIncluded())  {
			processParentsChildren(state);

		}
		if (state.getConfig().isDoAssociatedSpecialists()){
			processAssociatedSpecialists(state);
		}

		logger.warn("Before processMissappliedNames " + state.getConfig().isDoMisappliedNames());

		//ProfilerController.memorySnapshot();

		if (state.getConfig().isDoMisappliedNames()) {
			processMisappliedNames(state);
		}
		/*
		logger.warn("Before heterotypic synonyms");
		ProfilerController.memorySnapshot();
		*/
		if (state.getConfig().isDoHeterotypicSynonyms()) {
			if(logger.isInfoEnabled()) {
				logger.info("Start making heterotypic synonyms ...");
			}
			processHeterotypicSynonyms(state, ALL_SYNONYM_FROM_CLAUSE);
		}

		if (state.getConfig().isDoInferredSynonyms()){
		    processInferredSynonyms(state);
		}
		/*
		logger.warn("End RelTaxon doInvoke");
		ProfilerController.memorySnapshot();
		*/
		logger.info("End making relationships......");

		return;
	}

	/**
     * @param state
     */
    private void createAgentMap(FaunaEuropaeaImportState state) {


        List<String> propertyPaths = new ArrayList<String>();
        propertyPaths.add("sources");

        Query query =  getAgentService().getSession().createQuery("select ab.uuid, ob.idInSource from AgentBase ab join ab.sources ob where ob.idNamespace like 'User' ");
        List<Object[]> result = query.list();
       // List<TeamOrPersonBase> agents = getAgentService().list(TeamOrPersonBase.class, 1000, 0, null, null);

        Integer idInSource;
        for (Object[] person: result){
            idInSource = Integer.valueOf((String)person[1]);
            UUID agentUuid = (UUID) person[0];


            state.getAgentMap().put(idInSource, agentUuid);
        }
    }

    /** Retrieve child-parent uuid map from CDM DB */
	private void processParentsChildren(FaunaEuropaeaImportState state) {

		int limit = state.getConfig().getLimitSave();

		TransactionStatus txStatus = null;

		Map<UUID, UUID> childParentMap = null;

		Map<UUID, UUID> taxonSpecialistMap = null;
		Map<UUID, UUID> taxonGroupCoordinatorMap = null;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();
		int i = 0;

		String selectCount =
			" SELECT count(*) ";

		String selectColumns = " SELECT Taxon.UUID AS ChildUuid, Taxon.tax_usr_idgc as group_coordinator, Taxon.tax_usr_idsp as specialist, Parent.UUID AS ParentUuid, Parent.tax_usr_idgc as parent_group_coordinator, Parent.tax_usr_idsp as parent_specialist";

		String fromClause = " FROM Taxon INNER JOIN Taxon AS Parent " +
		" ON Taxon.TAX_TAX_IDPARENT = Parent.TAX_ID " +
		" WHERE (Taxon.TAX_VALID <> 0) AND (Taxon.TAX_AUT_ID <> " + A_AUCT + " OR Taxon.TAX_AUT_ID IS NULL )";

		String orderClause = " ORDER BY Taxon.TAX_RNK_ID ASC";

		String countQuery =
			selectCount + fromClause;

		String selectQuery =
			selectColumns + fromClause + orderClause;

		if(logger.isInfoEnabled()) { logger.info("Start making taxonomically included relationships..."); }

		try {

			ResultSet rs = source.getResultSet(countQuery);
			rs.next();
			int count = rs.getInt(1);

			rs = source.getResultSet(selectQuery);

	        if (logger.isInfoEnabled()) {
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQuery);
				logger.info("Select Query: " + selectQuery);
			}

	        while (rs.next()) {

				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					childParentMap = new HashMap<UUID, UUID>(limit);
					taxonSpecialistMap = new HashMap<UUID,UUID>(limit);
					taxonGroupCoordinatorMap = new HashMap<UUID, UUID>(limit);
					if(logger.isInfoEnabled()) {
						logger.info("Taxonomically included retrieved: " + (i-1));
					}
				}

				String childUuidStr = rs.getString("ChildUuid");
				String parentUuidStr = rs.getString("ParentUuid");

				int group_coordinator_id = rs.getInt("group_coordinator");
				int specialist_id = rs.getInt("specialist");
				int parent_group_coordinator_id = rs.getInt("parent_group_coordinator");
                int parent_specialist_id = rs.getInt("parent_specialist");
				UUID childUuid = UUID.fromString(childUuidStr);
				UUID parentUuid = UUID.fromString(parentUuidStr);

				if (!childParentMap.containsKey(childUuid)) {

						childParentMap.put(childUuid, parentUuid);


				} else {
					if(logger.isInfoEnabled()) {
						logger.info("Duplicated child UUID (" + childUuid + ")");
					}
				}
				if (!taxonSpecialistMap.containsKey(childUuid)) {

				    taxonSpecialistMap.put(childUuid, state.getAgentMap().get(specialist_id));
				}
				if (!taxonSpecialistMap.containsKey(parentUuid)) {

                    taxonSpecialistMap.put(parentUuid, state.getAgentMap().get(parent_specialist_id));
                }
				if (!taxonGroupCoordinatorMap.containsKey(childUuid)) {

				    taxonGroupCoordinatorMap.put(childUuid, state.getAgentMap().get(group_coordinator_id));
                }
                if (!taxonGroupCoordinatorMap.containsKey(parentUuid)) {

                    taxonGroupCoordinatorMap.put(parentUuid, state.getAgentMap().get(parent_group_coordinator_id));
                }


				if (((i % limit) == 0 && i != 1 ) || i == count ) {

					createAndCommitParentChildRelationships(
							state, txStatus, childParentMap, taxonSpecialistMap, taxonGroupCoordinatorMap);
					childParentMap = null;

					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Transaction committed");
					}
				}
			}
	        rs = null;
	        if (childParentMap != null){
	        	logger.info("processParentsChildren... last commit");
	        	createAndCommitParentChildRelationships(
						state, txStatus, childParentMap,taxonSpecialistMap, taxonGroupCoordinatorMap);
	        	childParentMap = null;
	        }

		} catch (Exception e) {
			logger.error("Exception:" +  e);
			state.setUnsuccessfull();
		}
		agentMap = null;
		childParentMap = null;
		return;
	}

	private Map<UUID, UUID> createAndCommitParentChildRelationships(
			FaunaEuropaeaImportState state, TransactionStatus txStatus,
			Map<UUID, UUID> childParentMap, Map<UUID, UUID> taxonSpecialistMap, Map<UUID, UUID> taxonGroupCoordinatorMap) {
		createParentChildRelationships(state, childParentMap, taxonSpecialistMap, taxonGroupCoordinatorMap, txStatus);


		commitTransaction(txStatus);
		return childParentMap;
	}


	/** Retrieve misapplied name / accepted taxon uuid map from CDM DB */
	private void processMisappliedNames(FaunaEuropaeaImportState state) {

		int limit = state.getConfig().getLimitSave();

		TransactionStatus txStatus = null;

		Map<UUID, UUID> childParentMap = null;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();
		int i = 0;

		String selectCount =
			" SELECT count(*) ";

		String selectColumns = " SELECT Taxon.UUID AS MisappliedUuid, Parent.UUID AS AcceptedUuid ";

		String fromClause = " FROM Taxon INNER JOIN Taxon AS Parent " +
		" ON Taxon.TAX_TAX_IDPARENT = Parent.TAX_ID " +
		" WHERE (Taxon.TAX_VALID = 0) AND (Taxon.TAX_AUT_ID = " + A_AUCT + ")";
		String orderClause = " ORDER BY dbo.Taxon.TAX_RNK_ID ASC ";




		String countQuery =
			selectCount + fromClause;

		String selectQuery =
			selectColumns + fromClause + orderClause;

		if(logger.isInfoEnabled()) { logger.info("Start making misapplied name relationships..."); }

		try {

			ResultSet rs = source.getResultSet(countQuery);
			rs.next();
			int count = rs.getInt(1);

			rs = source.getResultSet(selectQuery);

	        if (logger.isInfoEnabled()) {
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQuery);
				logger.info("Select Query: " + selectQuery);
			}

			while (rs.next()) {

				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					childParentMap = new HashMap<UUID, UUID>(limit);

					if(logger.isInfoEnabled()) {
						logger.info("Misapplied names retrieved: " + (i-1) );
					}
				}

				String childUuidStr = rs.getString("MisappliedUuid");
				String parentUuidStr = rs.getString("AcceptedUuid");
				UUID childUuid = UUID.fromString(childUuidStr);
				UUID parentUuid = UUID.fromString(parentUuidStr);

				if (!childParentMap.containsKey(childUuid)) {

						childParentMap.put(childUuid, parentUuid);

				} else {
					if(logger.isDebugEnabled()) {
						logger.debug("Duplicated child UUID (" + childUuid + ")");
					}
				}

				if (((i % limit) == 0 && i != 1 ) || i == count) {

					createAndCommitMisappliedNameRelationships(state, txStatus,
							childParentMap);
					childParentMap = null;


					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Transaction committed");
					}
				}
			}
			if (childParentMap != null){
				logger.info("processMisappliedNames... last commit");
				createAndCommitMisappliedNameRelationships(state, txStatus,
						childParentMap);
				childParentMap = null;
			}

		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
		return;
	}

	private void createAndCommitMisappliedNameRelationships(
			FaunaEuropaeaImportState state, TransactionStatus txStatus,
			Map<UUID, UUID> childParentMap) {
		createMisappliedNameRelationships(state, childParentMap);
		commitTransaction(txStatus);
	}



	/** Retrieve synonyms from FauEuDB DB */
	private void processHeterotypicSynonyms(FaunaEuropaeaImportState state, String fromClause) {

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

		String selectCount =
			" SELECT count(*) ";

		String selectColumns = " SELECT Taxon.UUID AS SynonymUuid, Parent.UUID AS AcceptedUuid ";

		String orderClause = " ORDER BY dbo.Taxon.TAX_RNK_ID ASC ";

		String countQuery =
			selectCount + fromClause;

		String selectQuery =
			selectColumns + fromClause + orderClause;
		logger.debug(selectQuery);

		try {

			ResultSet rs = source.getResultSet(countQuery);
			rs.next();
			int count = rs.getInt(1);

			rs = source.getResultSet(selectQuery);

	        if (logger.isInfoEnabled()) {
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQuery);
				logger.info("Select Query: " + selectQuery);
			}

	        storeSynonymRelationships(rs, count, state);
	        rs = null;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
		return;
	}




	private void storeSynonymRelationships(ResultSet rs, int count, FaunaEuropaeaImportState state)
	throws SQLException {

		TransactionStatus txStatus = null;
		Map<UUID, UUID> synonymAcceptedMap = null;
		int i = 0;
		int limit = state.getConfig().getLimitSave();

		while (rs.next()) {

			if ((i++ % limit) == 0) {

				txStatus = startTransaction();
				synonymAcceptedMap = new HashMap<UUID, UUID>(limit);

				if(logger.isInfoEnabled()) {
					logger.info("Synonyms retrieved: " + (i-1));
				}
			}

			String synonymUuidStr = rs.getString("SynonymUuid");
			String acceptedUuidStr = rs.getString("AcceptedUuid");
			UUID synonymUuid = UUID.fromString(synonymUuidStr);
			UUID acceptedUuid = UUID.fromString(acceptedUuidStr);

			if (!synonymAcceptedMap.containsKey(synonymUuid)) {

				synonymAcceptedMap.put(synonymUuid, acceptedUuid);

			} else {
				if(logger.isDebugEnabled()) {
					logger.debug("Duplicated synonym UUID (" + synonymUuid + ")");
				}
			}

			if (((i % limit) == 0 && i != 1 ) || i == count ) {

				synonymAcceptedMap = createAndCommitHeterotypicSynonyms(state,
						txStatus, synonymAcceptedMap);

				if(logger.isInfoEnabled()) {
					logger.info("i = " + i + " - Transaction committed");
				}
			}
		}
		if (synonymAcceptedMap != null){
			logger.info("processHeterotypicSynonyms... last commit");
			synonymAcceptedMap = createAndCommitHeterotypicSynonyms(state,
					txStatus, synonymAcceptedMap);
		}
		return;
	}

	private Map<UUID, UUID> createAndCommitHeterotypicSynonyms(
			FaunaEuropaeaImportState state, TransactionStatus txStatus,
			Map<UUID, UUID> synonymAcceptedMap) {
		createHeterotypicSynonyms(state, synonymAcceptedMap);

		synonymAcceptedMap = null;
		commitTransaction(txStatus);
		return synonymAcceptedMap;
	}





	/* Creates parent-child relationships.
	 * Parent-child pairs are retrieved in blocks via findByUUID(Set<UUID>) from CDM DB.
	 */
	private void createParentChildRelationships(FaunaEuropaeaImportState state, Map<UUID, UUID> childParentMap, Map<UUID, UUID> taxonSpecialistMap, Map<UUID, UUID> taxonGroupCoordinatorMap, TransactionStatus tx) {
		//gets the taxon "Hydroscaphidae"(family)
		TaxonBase taxon = getTaxonService().find(UUID.fromString(acceptedTaxonUUID));
		sourceRef = taxon.getSec();
		int limit = state.getConfig().getLimitSave();

		Classification tree = getClassificationFor(state, sourceRef);

		Set<TaxonBase> childSet = new HashSet<TaxonBase>(limit);

		Set<UUID> childKeysSet = childParentMap.keySet();
		Set<UUID> parentValuesSet = new HashSet<UUID>(childParentMap.values());
		logger.debug("Start reading children and parents");
		if (logger.isInfoEnabled()) {
			logger.info("Start reading children and parents");
		}
		List<TaxonBase> children = getTaxonService().find(childKeysSet);
		List<TaxonBase> parents = getTaxonService().find(parentValuesSet);
		logger.info(children.size() + "children are available");
		logger.info(parents.size() + "parents are available");
		Map<UUID, TaxonBase> parentsMap = new HashMap<UUID, TaxonBase>(parents.size());

		for (TaxonBase taxonBase : parents){
			parentsMap.put(taxonBase.getUuid(), taxonBase);
		}

		if (logger.isTraceEnabled()) {
			logger.debug("End reading children and parents");
			for (UUID uuid : childKeysSet) {
				logger.trace("child uuid query: " + uuid);
			}
			for (UUID uuid : parentValuesSet) {
				logger.trace("parent uuid query: " + uuid);
			}
			for (TaxonBase tb : children) {
				logger.trace("child uuid result: " + tb.getUuid());
			}
			for (TaxonBase tb : parents) {
				logger.trace("parent uuid result: " + tb.getUuid());
			}
		}

		UUID mappedParentUuid = null;
		UUID childUuid = null;
		TaxonNode childNode;
		TeamOrPersonBase taxonomicSpecialist = null;
		TeamOrPersonBase groupCoordinator = null;
		UUID agentUuid = null;
		for (TaxonBase child : children) {

			try {
				Taxon childTaxon = child.deproxy(child, Taxon.class);
				childUuid = childTaxon.getUuid();
				mappedParentUuid = childParentMap.get(childUuid);
				TaxonBase parent = null;

				TaxonBase potentialParent = parentsMap.get(mappedParentUuid);
//					for (TaxonBase potentialParent : parents ) {
//						parentUuid = potentialParent.getUuid();
//						if(parentUuid.equals(mappedParentUuid)) {
						parent = potentialParent;
						if (logger.isInfoEnabled()) {
							logger.info("Parent (" + mappedParentUuid + ") found for child (" + childUuid + ")");
						}
//							break;
//						}
//					}

				Taxon parentTaxon = parent.deproxy(parent, Taxon.class);

				if (childTaxon != null && parentTaxon != null) {

					childNode = tree.addParentChild(parentTaxon, childTaxon, sourceRef, null);
					agentUuid = taxonSpecialistMap.get(childTaxon.getUuid());

					taxonomicSpecialist = agentMap.get(agentUuid);
					if (taxonomicSpecialist == null){
					    taxonomicSpecialist = (TeamOrPersonBase) getAgentService().find(agentUuid);
					    if (taxonomicSpecialist != null){
					        agentMap.put(agentUuid, taxonomicSpecialist);
					        logger.info("get new person: " + agentUuid + " name: " + taxonomicSpecialist.getTitleCache());
					    }
					}
					if (taxonomicSpecialist != null){
    				    childNode.addAgentRelation(FaunaEuropaeaTransformer.getTaxonomicSpecialistType(getTermService()),  taxonomicSpecialist);
    				} else {
    				    logger.info("Taxonomic Specialist for " + childTaxon.getTitleCache() + " - " + childTaxon.getUuid() + " does not exist");
    				}


					agentUuid = taxonGroupCoordinatorMap.get(childTaxon.getUuid());
					groupCoordinator = agentMap.get(agentUuid);

					if (groupCoordinator == null){
					    groupCoordinator = (TeamOrPersonBase) getAgentService().find(agentUuid);
					    if (groupCoordinator != null){
					        agentMap.put(agentUuid, groupCoordinator);
					        logger.info("get new person: " + agentUuid + " name: " + groupCoordinator.getTitleCache());
					    }
					}
					if (groupCoordinator != null){
                        childNode.addAgentRelation(FaunaEuropaeaTransformer.getGroupCoordinatorType(getTermService()),  groupCoordinator);
                    } else {
                        logger.debug("Group Coordinator for " + childTaxon.getTitleCache() + " - " + childTaxon.getUuid() + " does not exist");
                    }

					//TODO: add the specialist and the group coordinator to the node!!
					if (logger.isInfoEnabled()) {
						logger.info("Parent-child (" + mappedParentUuid + "-" + childUuid +
						") relationship created");
					}
					if (childTaxon != null && !childSet.contains(childTaxon)) {

						childSet.add(childTaxon);

						if (logger.isDebugEnabled()) {
							logger.info("Child taxon (" + childUuid + ") added to Set");
						}

					} else {
						if (logger.isDebugEnabled()) {
							logger.info("Duplicated child taxon (" + childUuid + ")");
						}
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.info("Parent(" + mappedParentUuid + ") or child (" + childUuid + " is null");
					}
				}

			} catch (Exception e) {
				logger.error("Error creating taxonomically included relationship parent-child (" +
					mappedParentUuid + " <----> " + childUuid + ")", e);
			}

		}
		if (logger.isInfoEnabled()) {
			logger.info("Start saving childSet");
		}

		getTaxonService().saveOrUpdate(childSet);

		if (logger.isInfoEnabled()) {
			logger.info("End saving childSet");
		}

		parentValuesSet = null;
		childSet = null;
		children = null;
		parents = null;
		tree = null;
		this.agentMap = new HashMap<UUID, TeamOrPersonBase>();
		return;
	}

	/* Creates misapplied name relationships.
	 * Misapplied name-accepted taxon pairs are retrieved in blocks via findByUUID(Set<UUID>) from CDM DB.
	 */
	private void createMisappliedNameRelationships(FaunaEuropaeaImportState state, Map<UUID, UUID> fromToMap) {

		//gets the taxon "Hydroscaphidae" (family)

		TaxonBase taxon = getTaxonService().find(UUID.fromString(acceptedTaxonUUID));
		sourceRef = taxon.getSec();
		int limit = state.getConfig().getLimitSave();

			Set<TaxonBase> misappliedNameSet = new HashSet<TaxonBase>(limit);

			Set<UUID> misappliedNamesSet = fromToMap.keySet();
			Set<UUID> acceptedTaxaSet = new HashSet<UUID>(fromToMap.values());

			if (logger.isTraceEnabled()) {
				logger.trace("Start reading misapplied names and accepted taxa");
			}
			List<TaxonBase> misappliedNames = getTaxonService().find(misappliedNamesSet);
			List<TaxonBase> acceptedTaxa = getTaxonService().find(acceptedTaxaSet);
			Map<UUID, TaxonBase> acceptedTaxaMap = new HashMap<UUID, TaxonBase>(acceptedTaxa.size());
			for (TaxonBase taxonBase : acceptedTaxa){
				acceptedTaxaMap.put(taxonBase.getUuid(), taxonBase);
			}

			if (logger.isTraceEnabled()) {
				logger.info("End reading misapplied names and accepted taxa");
				for (UUID uuid : misappliedNamesSet) {
					logger.trace("misapplied name uuid query: " + uuid);
				}
				for (UUID uuid : acceptedTaxaSet) {
					logger.trace("accepted taxon uuid query: " + uuid);
				}
				for (TaxonBase tb : misappliedNames) {
					logger.trace("misapplied name uuid result: " + tb.getUuid());
				}
				for (TaxonBase tb : acceptedTaxa) {
					logger.trace("accepted taxon uuid result: " + tb.getUuid());
				}
			}

			UUID mappedAcceptedTaxonUuid = null;
			UUID misappliedNameUuid = null;
			Taxon misappliedNameTaxon = null;
			TaxonBase acceptedTaxonBase = null;
			Taxon acceptedTaxon = null;

			for (TaxonBase misappliedName : misappliedNames) {

				try {

					misappliedNameTaxon = misappliedName.deproxy(misappliedName, Taxon.class);
					misappliedName.setAppendedPhrase(AUCT_STRING);

					misappliedNameUuid = misappliedNameTaxon.getUuid();
					mappedAcceptedTaxonUuid = fromToMap.get(misappliedNameUuid);
					acceptedTaxonBase = null;

					acceptedTaxonBase = acceptedTaxaMap.get(mappedAcceptedTaxonUuid);
							if (logger.isDebugEnabled()) {
								logger.debug("Parent (" + mappedAcceptedTaxonUuid + ") found for child (" + misappliedNameUuid + ")");
							}

							acceptedTaxon = acceptedTaxonBase.deproxy(acceptedTaxonBase, Taxon.class);

					if (misappliedNameTaxon != null && acceptedTaxon != null) {

						acceptedTaxon.addMisappliedName(misappliedNameTaxon, sourceRef, null);

						if (logger.isDebugEnabled()) {
							logger.debug("Accepted taxon / misapplied name (" + mappedAcceptedTaxonUuid + "-" + misappliedNameUuid +
							") relationship created");
						}
						if (!misappliedNameSet.contains(misappliedNameTaxon)) {

							misappliedNameSet.add(misappliedNameTaxon);

							if (logger.isTraceEnabled()) {
								logger.trace("Misapplied name taxon (" + misappliedNameUuid + ") added to Set");
							}

						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Duplicated misapplied name taxon (" + misappliedNameUuid + ")");
							}
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Accepted taxon (" + mappedAcceptedTaxonUuid + ") or misapplied name (" + misappliedNameUuid + " is null");
						}
					}

					if (misappliedNameTaxon != null && !misappliedNameSet.contains(misappliedNameTaxon)) {
						misappliedNameSet.add(misappliedNameTaxon);
						if (logger.isTraceEnabled()) {
							logger.trace("Misapplied name taxon (" + misappliedNameUuid + ") added to Set");
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Duplicated misapplied name taxon (" + misappliedNameUuid + ")");
						}
					}

				} catch (Exception e) {
					logger.error("Error creating misapplied name relationship accepted taxon-misapplied name (" +
						mappedAcceptedTaxonUuid + "-" + misappliedNameUuid + ")", e);
				}

			}
			if (logger.isTraceEnabled()) {
				logger.trace("Start saving misappliedNameSet");
			}
			getTaxonService().save(misappliedNameSet);
			if (logger.isTraceEnabled()) {
				logger.trace("End saving misappliedNameSet");
			}

			acceptedTaxaSet = null;
			misappliedNameSet = null;
			misappliedNames = null;
			acceptedTaxa = null;

		return;
	}


	/* Creates heterotypic synonyms.
	 * Synonym-accepted taxon pairs are retrieved in blocks via findByUUID(Set<UUID>) from CDM DB.
	 */
	private void createHeterotypicSynonyms(FaunaEuropaeaImportState state, Map<UUID, UUID> fromToMap) {

		int limit = state.getConfig().getLimitSave();

		Set<TaxonBase> synonymSet = new HashSet<TaxonBase>(limit);

		Set<UUID> synonymUuidSet = fromToMap.keySet();
		Set<UUID> acceptedTaxaUuidSet = new HashSet<UUID>(fromToMap.values());

		if (logger.isTraceEnabled()) {
			logger.trace("Reading synonym names and accepted taxa...");
		}
		List<TaxonBase> synonyms = getTaxonService().find(synonymUuidSet);
		List<TaxonBase> acceptedTaxa = getTaxonService().find(acceptedTaxaUuidSet);
		Map<UUID, TaxonBase> acceptedTaxaMap = new HashMap<UUID, TaxonBase>(acceptedTaxa.size());
		for (TaxonBase taxonBase : acceptedTaxa){
			acceptedTaxaMap.put(taxonBase.getUuid(), taxonBase);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("End reading synonyms names and accepted taxa");
			for (UUID uuid : synonymUuidSet) {
				logger.trace("synonym uuid query: " + uuid);
			}
			for (UUID uuid : acceptedTaxaUuidSet) {
				logger.trace("accepted taxon uuid query: " + uuid);
			}
			for (TaxonBase tb : synonyms) {
				logger.trace("synonym uuid result: " + tb.getUuid());
			}
			for (TaxonBase tb : acceptedTaxa) {
				logger.trace("accepted taxon uuid result: " + tb.getUuid());
			}
		}

		UUID mappedAcceptedTaxonUuid = null;
		UUID synonymUuid = null;
		Synonym synonym = null;
		TaxonBase acceptedTaxonBase = null;
		Taxon acceptedTaxon = null;

		for (TaxonBase synonymTaxonBase : synonyms) {

			try {
				//check for misapplied names with nec (in Fauna Europaea they have a synonym relationship to the accepted taxon)
				if (synonymTaxonBase instanceof Taxon ){
					if (((Taxon)synonymTaxonBase).isMisapplication()){
						if (logger.isDebugEnabled()) {
							logger.debug("misapplied name with exclusion" +  synonymTaxonBase.getTitleCache());
						}
					}
					continue;
				}
				synonym = HibernateProxyHelper.deproxy(synonymTaxonBase, Synonym.class);
				synonymUuid = synonym.getUuid();
				mappedAcceptedTaxonUuid = fromToMap.get(synonymUuid);
				acceptedTaxonBase = null;

				acceptedTaxonBase = acceptedTaxaMap.get(mappedAcceptedTaxonUuid);
				if (logger.isDebugEnabled()) {
					logger.debug("Parent (" + mappedAcceptedTaxonUuid + ") found for child (" + synonymUuid + ")");
				}
				acceptedTaxon = CdmBase.deproxy(acceptedTaxonBase, Taxon.class);

				if (synonym != null && acceptedTaxon != null) {

					//TODO: in case original genus exists must add synonym to original genus instead of to accepted taxon
					acceptedTaxon.addSynonym(synonym, SynonymType.HETEROTYPIC_SYNONYM_OF());

					if (logger.isDebugEnabled()) {
						logger.debug("Accepted taxon - synonym (" + mappedAcceptedTaxonUuid + " - " + synonymUuid +
						") relationship created");
					}
					if (synonym != null && !synonymSet.contains(synonym)) {

						synonymSet.add(synonym);

						if (logger.isTraceEnabled()) {
							logger.trace("Synonym (" + synonymUuid + ") added to Set");
						}

					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Duplicated synonym (" + synonymUuid + ")");
						}
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Accepted taxon (" + mappedAcceptedTaxonUuid + ") or misapplied name (" + synonymUuid + " is null");
					}
				}
			} catch (Exception e) {
				logger.error("Error attaching synonym to accepted taxon (" +
						mappedAcceptedTaxonUuid + "-" + synonymUuid + ": "+ synonymTaxonBase.getTitleCache() +")", e);
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Start saving synonymSet");
		}
		getTaxonService().save(synonymSet);
		if (logger.isTraceEnabled()) {
			logger.trace("End saving synonymSet");
		}

		acceptedTaxaUuidSet = null;
		synonymSet = null;
		synonyms = null;
		acceptedTaxa = null;

		return;
	}

	private void processAssociatedSpecialists(FaunaEuropaeaImportState state){
		int limit = state.getConfig().getLimitSave();

		TransactionStatus txStatus = null;
		Set<UUID> taxonUuids = new HashSet<UUID>();
		Map<UUID, Set<UUID>> agentUUIDTaxonMap = new HashMap<UUID, Set<UUID>>();
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();
		int i = 0;
		UUID agentUuid;
		Set<UUID> agentUuids = new HashSet<UUID>();
		Set<UUID> taxaOfAgent = new HashSet<UUID>();
		String selectCount =
			" SELECT count(*) ";



		String selectColumns = "SELECT  u.USR_GROUPNAME as groupName, u.USR_GROUPNOTE groupNote, u.USR_USR_ID as user_user_id, "
				+ "		u.usr_id user_id, user2.USR_ID as user2_id, taxon.UUID as tax_uuid ";

		String fromClause = " FROM USERROLE as userrole left join USERS u on userrole.URL_USR_ID = u.USR_ID join USERS user2 on user2.USR_ID = u.USR_USR_ID "
						+ " join TAXON taxon on (taxon.TAX_USR_IDSP= user2.USR_ID or taxon.TAX_USR_IDGC= user2.USR_ID) "
						+ " where USERROLE.URL_ROL_ID = 7 ";
		String orderClause = " ORDER BY taxon.TAX_ID";

		String countQuery =
			selectCount + fromClause;

		String selectQuery =
			selectColumns + fromClause + orderClause;

		if(logger.isInfoEnabled()) { logger.info("Start making associated specialists..."); }

		try {

			ResultSet rs = source.getResultSet(countQuery);
			rs.next();
			int count = rs.getInt(1);

			rs = source.getResultSet(selectQuery);

	        if (logger.isInfoEnabled()) {
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQuery);
				logger.info("Select Query: " + selectQuery);
			}
	        String oldTaxonUuidString = null;
	        while (rs.next()) {

                if ((i++ % limit) == 0) {
                    txStatus = startTransaction();
                    if(logger.isInfoEnabled()) {
                        logger.info("Associated specialists: " + (i-1));
                    }
                }
                String taxonUUID = rs.getString("tax_uuid");

				int userId = rs.getInt("user_id");
				int user2Id = rs.getInt("user2_id");
				String groupName = rs.getString("groupName");

				taxonUuids.add(UUID.fromString(taxonUUID));
				agentUuid = state.getAgentMap().get(userId);
				agentUuids.add(agentUuid);
				if (agentUUIDTaxonMap.containsKey(agentUuid)){
				    taxaOfAgent = agentUUIDTaxonMap.get(agentUuid);
				} else{
				    taxaOfAgent = new HashSet<UUID>();
				}
				taxaOfAgent.add(UUID.fromString(taxonUUID));
                agentUUIDTaxonMap.put(agentUuid, taxaOfAgent);


				 if (((i % limit) == 0 && i != 1 ) || i == count ) {

				        createTaxonNodeAgentRealtionships(state, taxonUuids, agentUUIDTaxonMap, agentUuids);
		                commitTransaction(txStatus);
		                agentUUIDTaxonMap.clear();
		                taxonUuids= new HashSet<UUID>();
		                agentUuids=new HashSet<UUID>();
		                if(logger.isInfoEnabled()) {
		                    logger.info("i = " + i + " - Transaction committed");
		                }
		            }

	        }
		}catch(SQLException e){
			logger.info("Problems during creating associated specialists.", e);
		}
	}

	private void createTaxonNodeAgentRealtionships(FaunaEuropaeaImportState state, Set<UUID> taxonUuids, Map<UUID, Set<UUID>> agentUUIDTaxonMap, Set<UUID> agentUUuids){
	    Taxon taxon = null;
	    TaxonBase taxonBase;
        Synonym syn;
        Set<TaxonNode> nodes = new HashSet<TaxonNode>();
        List<TaxonBase> taxonBases = getTaxonService().find(taxonUuids);
        List<AgentBase> agentBases = getAgentService().find(agentUUuids);
        TeamOrPersonBase associatedSpecialist = null;
        TaxonNodeAgentRelation agentRel = null;

        TaxonNode taxonNode;
//        Map<UUID, TeamOrPersonBase> agentsMap = new HashMap<UUID,TeamOrPersonBase>();
//        for (AgentBase agent: agentBases){
//            agentsMap.put(agent.getUuid(), (TeamOrPersonBase) agent);
//        }
        Map<UUID, TaxonBase> taxonMap = new HashMap<UUID,TaxonBase>();
        for (TaxonBase tax: taxonBases){
            taxonMap.put(tax.getUuid(), tax);
        }

        for (AgentBase agent: agentBases){
            Set<UUID> taxaUuids = agentUUIDTaxonMap.get(agent.getUuid());
            for (UUID taxonUuid: taxaUuids){
                taxonBase = taxonMap.get(taxonUuid);
                    if (taxonBase instanceof Synonym){
                        logger.debug("synonym has relationship to associated specialist. " + taxonBase.getTitleCache());
                        syn = HibernateProxyHelper.deproxy(taxonBase, Synonym.class);
                        if (syn.getAcceptedTaxon() != null){
                            taxon = syn.getAcceptedTaxon();
                            syn = null;
                        }
                    } else{
                        taxon = HibernateProxyHelper.deproxy(taxonBase, Taxon.class);
                    }

                    if (taxon != null && !taxon.getTaxonNodes().isEmpty()){
                        taxonNode = taxon.getTaxonNodes().iterator().next();
                    } else {
                        taxonNode = null;
                        logger.debug("There is an associated specialist for a taxon which has no taxonnode.");
                    }


                    associatedSpecialist = (TeamOrPersonBase)agent;
                    if (associatedSpecialist != null && taxonNode != null){
                        agentRel =taxonNode.addAgentRelation(FaunaEuropaeaTransformer.getAssociateSpecialistType(getTermService()), associatedSpecialist);
                        /*if (!StringUtils.isBlank(groupName)) {
                            agentRel.addAnnotation(Annotation.NewInstance(groupName, Language.DEFAULT()));
                        }*/
                    }



                nodes.add(taxonNode);
              }
            agent = null;
            associatedSpecialist = null;
            taxonNode = null;
        }
        if (!nodes.isEmpty()){
            while (nodes.contains(null)){
                nodes.remove(null);
            }
            getTaxonNodeService().saveOrUpdate(nodes);
            nodes = null;

        }
        taxonMap= null;
        agentMap= null;
        taxonBases = null;
        agentBases = null;
       }



	private void processInferredSynonyms(FaunaEuropaeaImportState state){

	        int count;
	        int pastCount;
	        boolean success = true;
	        // Get the limit for objects to save within a single transaction.
	        if (! state.getConfig().isDoInferredSynonyms()){
	            logger.info ("Ignore Creating Inferred Synonyms...");
	            return;
	        }

	        int limit = state.getConfig().getLimitSave();
	        // Create inferred synonyms for accepted taxa
	        logger.info("Creating Inferred Synonyms...");


	        count = 0;
	        pastCount = 0;
	        int pageSize = limit/10;
	        int pageNumber = 1;
	        String inferredSynonymPluralString = "Inferred Synonyms";

	        // Start transaction
	        TransactionStatus txStatus = startTransaction(true);
	        logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");
	        List<TaxonBase> taxonList = null;
	        Set<TaxonBase> synonymList = new HashSet<TaxonBase>();


	        while ((taxonList  = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*", Rank.SPECIES(), pageSize, pageNumber)).size() > 0) {
	            HashMap<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

	            logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Importing...");
	            synonymList = createInferredSynonymsForTaxonList(state,  taxonList);
	            getTaxonService().save(synonymList);

	          //  getTaxonService().saveOrUpdate(taxonList);
	            // Commit transaction
	            commitTransaction(txStatus);
	            logger.debug("Committed transaction.");
	            logger.info("Imported " + (taxonList.size()) + " " + inferredSynonymPluralString + ". Total: " + count);
	            //pastCount = count;



	            // Start transaction
	            txStatus = startTransaction(true);
	            logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");

	            // Increment pageNumber
	            pageNumber++;
	        }
	        taxonList = null;
	        while ((taxonList  = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*", Rank.SUBSPECIES(), pageSize, pageNumber)).size() > 0) {
	            HashMap<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

	            logger.info("Fetched " + taxonList.size() + " " + parentPluralString  + ". Exporting...");
	            synonymList = createInferredSynonymsForTaxonList(state, taxonList);

	            getTaxonService().save(synonymList);
	            // Commit transaction
	            commitTransaction(txStatus);
	            logger.debug("Committed transaction.");
	            logger.info("Exported " + taxonList.size()+ " " + inferredSynonymPluralString + ". Total: " + count);
	            //pastCount = count;



	            // Start transaction
	            txStatus = startTransaction(true);
	            logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");

	            // Increment pageNumber
	            pageNumber++;
	            inferredSynonymsDataToBeSaved = null;
	        }
	        if (taxonList.size() == 0) {
	            logger.info("No " + parentPluralString + " left to fetch.");
	        }

	        taxonList = null;

	        // Commit transaction
	        commitTransaction(txStatus);
	        System.gc();

	        //ProfilerController.memorySnapshot();
	        logger.debug("Committed transaction.");


	}

	/**
    * @param state
    * @param mapping
    * @param synRelMapping
    * @param currentTaxonId
    * @param taxonList
    * @param inferredSynonymsDataToBeSaved
    * @return
    */
   private Set<TaxonBase> createInferredSynonymsForTaxonList(FaunaEuropaeaImportState state,
            List<TaxonBase> taxonList) {

       Taxon acceptedTaxon;
       Classification classification = null;
       Set<TaxonBase> inferredSynonyms = new HashSet<TaxonBase>();
       List<Synonym> inferredSynonymsLocal= new ArrayList<Synonym>();
       boolean localSuccess = true;

       HashMap<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

       for (TaxonBase<?> taxonBase : taxonList) {

           if (taxonBase.isInstanceOf(Taxon.class)) { // this should always be the case since we should have fetched accepted taxon only, but you never know...
               acceptedTaxon = CdmBase.deproxy(taxonBase, Taxon.class);
               TaxonName taxonName = acceptedTaxon.getName();

               if (taxonName.isZoological()) {
                   Set<TaxonNode> taxonNodes = acceptedTaxon.getTaxonNodes();
                   TaxonNode singleNode = null;

                   if (taxonNodes.size() > 0) {
                       // Determine the classification of the current TaxonNode

                       singleNode = taxonNodes.iterator().next();
                       if (singleNode != null) {
                           classification = singleNode.getClassification();
                       } else {
                           logger.error("A TaxonNode belonging to this accepted Taxon is NULL: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache() +")");
                       }
                   } else {
                       // Classification could not be determined directly from this TaxonNode
                       // The stored classification from another TaxonNode is used. It's a simple, but not a failsafe fallback solution.
                       if (taxonNodes.size() == 0) {
                           //logger.error("Classification could not be determined directly from this Taxon: " + acceptedTaxon.getUuid() + " is misapplication? "+acceptedTaxon.isMisapplication()+ "). The classification of the last taxon is used");

                       }
                   }

                   if (classification != null) {
                       try{
                           TaxonName name = acceptedTaxon.getName();

                            //if (name.isSpecies() || name.isInfraSpecific()){
                               inferredSynonymsLocal = getTaxonService().createAllInferredSynonyms(acceptedTaxon, classification, true);
                              // logger.info("number of inferred synonyms: " + inferredSynonyms.size());
                           //}
//                             inferredSynonyms = getTaxonService().createInferredSynonyms(classification, acceptedTaxon, SynonymType.INFERRED_GENUS_OF());
                           if (inferredSynonymsLocal != null) {
                               for (TaxonBase synonym : inferredSynonymsLocal) {
//                                 TaxonNameBase<?,?> synonymName = synonym.getName();
                                   MarkerType markerType =getUuidMarkerType(PesiTransformer.uuidMarkerGuidIsMissing, state);

                                   synonym.addMarker(Marker.NewInstance(markerType, true));


                                   //get SynonymRelationship and export
                                   if (((Synonym)synonym).getAcceptedTaxon() == null ){
                                       IdentifiableSource source = ((Synonym)synonym).getSources().iterator().next();
                                       if (source.getIdNamespace().contains("Potential combination")){
                                           acceptedTaxon.addSynonym((Synonym)synonym, SynonymType.POTENTIAL_COMBINATION_OF());
                                           logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to potential combination");
                                       } else if (source.getIdNamespace().contains("Inferred Genus")){
                                           acceptedTaxon.addSynonym((Synonym)synonym, SynonymType.INFERRED_GENUS_OF());
                                           logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred genus");
                                       } else if (source.getIdNamespace().contains("Inferred Epithet")){
                                           acceptedTaxon.addSynonym((Synonym)synonym, SynonymType.INFERRED_EPITHET_OF());
                                           logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred epithet");
                                       } else{
                                           acceptedTaxon.addSynonym((Synonym)synonym, SynonymType.INFERRED_SYNONYM_OF());
                                           logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred synonym");
                                       }

                                   } else {
                                       if (!localSuccess) {
                                           logger.error("Synonym relationship export failed " + synonym.getTitleCache() + " accepted taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
                                       } else {
                                          // logger.info("Synonym relationship successfully exported: " + synonym.getTitleCache() + "  " +acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
                                       }
                                   }

                                   inferredSynonymsDataToBeSaved.put(synonym.getId(), synonym.getName());
                               }

                               inferredSynonyms.addAll(inferredSynonymsLocal);
                              //logger.info("inferredSet: " + inferredSet.size());
                               //getTaxonService().save(inferredSynonyms);
                               //commitTransaction(txStatus);

                               inferredSynonymsLocal = null;

                           }


                       }catch(Exception e){
                           logger.error(e.getMessage());
                           e.printStackTrace();
                       }
                   } else {
                       logger.error("Classification is NULL. Inferred Synonyms could not be created for this Taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache() + ")");
                   }
               } else {
//                         logger.error("TaxonName is not a ZoologicalName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
               }
           } else {
               logger.error("This TaxonBase is not a Taxon even though it should be: " + taxonBase.getUuid() + " (" + taxonBase.getTitleCache() + ")");
           }
       }
       //getTaxonService().saveOrUpdate(taxonList);
       taxonList = null;
       return inferredSynonyms;
   }


}
