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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.MapWrapper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
//import eu.etaxonomy.cdm.profiler.ProfilerController;



/**
 * @author a.babadshanjan
 * @created 12.05.2009
 */
@Component
public class FaunaEuropaeaRelTaxonIncludeImport extends FaunaEuropaeaImportBase  {
	
	public static final String OS_NAMESPACE_TAXON = "Taxon";
	private static final Logger logger = Logger.getLogger(FaunaEuropaeaRelTaxonIncludeImport.class);
	//private static final String acceptedTaxonUUID = "A9C24E42-69F5-4681-9399-041E652CF338"; // any accepted taxon uuid, taken from original fauna europaea database
	//private static final String acceptedTaxonUUID = "E23E6295-836A-4332-BF72-7D29949C7C60"; //faunaEu_1_3
	private static final String acceptedTaxonUUID = "BB9CDF6C-BBA3-4AC7-A3FD-648A14F518A0"; //for faunEu (2.4)
	
	private Reference<?> sourceRef;
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
		state.getConfig().isDoMisappliedNames() || state.getConfig().isDoHeterotypicSynonyms());
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
		Map<String, MapWrapper<? extends CdmBase>> stores = state.getStores();

		MapWrapper<TeamOrPersonBase> authorStore = (MapWrapper<TeamOrPersonBase>)stores.get(ICdmIO.TEAM_STORE);
		authorStore.makeEmpty();

		if(logger.isInfoEnabled()) { logger.info("Start making relationships..."); }

		TransactionStatus txStatus = startTransaction();
		
		// the uuid of an accepted taxon is needed here. any accepted taxon will do.
		TaxonBase taxon = getTaxonService().find(UUID.fromString(acceptedTaxonUUID));
		sourceRef = taxon.getSec();

		Classification tree = getClassificationFor(state, sourceRef);
		commitTransaction(txStatus);
		/*
		logger.warn("Before processParentsChildren");
		
		ProfilerController.memorySnapshot();
		*/
		if (state.getConfig().isDoTaxonomicallyIncluded()) {
			processParentsChildren(state);
		}
		/*
		logger.warn("Before processMissappliedNames");
		
		ProfilerController.memorySnapshot();
		*/
		if (state.getConfig().isDoMisappliedNames()) {
			processMisappliedNames(state);
		}
		/*
		logger.warn("Before heterotypic synonyms");
		ProfilerController.memorySnapshot();
		*/
		if (state.getConfig().isDoHeterotypicSynonyms()) {
			if(logger.isInfoEnabled()) { 
				logger.info("Start making heterotypic synonym relationships..."); 
			}
			processHeterotypicSynonyms(state, ALL_SYNONYM_FROM_CLAUSE);
		}
		/*
		logger.warn("End RelTaxon doInvoke");
		ProfilerController.memorySnapshot();
		*/
		logger.info("End making taxa...");

		return;
	}

	/** Retrieve child-parent uuid map from CDM DB */
	private void processParentsChildren(FaunaEuropaeaImportState state) {

		int limit = state.getConfig().getLimitSave();

		TransactionStatus txStatus = null;

		Map<UUID, UUID> childParentMap = null;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();
		int i = 0;
		
		String selectCount = 
			" SELECT count(*) ";

		String selectColumns = " SELECT Taxon.UUID AS ChildUuid, Parent.UUID AS ParentUuid ";
		
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
					
					if(logger.isInfoEnabled()) {
						logger.info("Taxonomically included retrieved: " + (i-1)); 
					}
				}

				String childUuidStr = rs.getString("ChildUuid");
				String parentUuidStr = rs.getString("ParentUuid");
				UUID childUuid = UUID.fromString(childUuidStr);
				UUID parentUuid = UUID.fromString(parentUuidStr);
				
				if (!childParentMap.containsKey(childUuid)) {

						childParentMap.put(childUuid, parentUuid);

				} else {
					if(logger.isDebugEnabled()) {
						logger.debug("Duplicated child UUID (" + childUuid + ")");
					}
				}
				if (((i % limit) == 0 && i != 1 ) || i == count ) { 

					createAndCommitParentChildRelationships(
							state, txStatus, childParentMap);
					childParentMap = null;

					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Transaction committed"); 
					}
				}
			}
	        
	        if (childParentMap != null){
	        	logger.info("processParentsChildren... last commit");
	        	createAndCommitParentChildRelationships(
						state, txStatus, childParentMap);
	        	childParentMap = null;
	        }

		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
		return;		
	}

	private Map<UUID, UUID> createAndCommitParentChildRelationships(
			FaunaEuropaeaImportState state, TransactionStatus txStatus,
			Map<UUID, UUID> childParentMap) {
		createParentChildRelationships(state, childParentMap);

		
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
	private void createParentChildRelationships(FaunaEuropaeaImportState state, Map<UUID, UUID> childParentMap) {
		//gets the taxon "Hydroscaphidae"(family)
		TaxonBase taxon = getTaxonService().find(UUID.fromString(acceptedTaxonUUID));
		sourceRef = taxon.getSec();
		int limit = state.getConfig().getLimitSave();
		
		Classification tree = getClassificationFor(state, sourceRef);
		
		Set<TaxonBase> childSet = new HashSet<TaxonBase>(limit);
		
		Set<UUID> childKeysSet = childParentMap.keySet();
		Set<UUID> parentValuesSet = new HashSet<UUID>(childParentMap.values());
		
		if (logger.isTraceEnabled()) {
			logger.trace("Start reading children and parents");
		}
		List<TaxonBase> children = getTaxonService().find(childKeysSet);
		List<TaxonBase> parents = getTaxonService().find(parentValuesSet);
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
						if (logger.isDebugEnabled()) {
							logger.debug("Parent (" + mappedParentUuid + ") found for child (" + childUuid + ")");
						}
//							break;
//						}
//					}
				
				Taxon parentTaxon = parent.deproxy(parent, Taxon.class);
				
				if (childTaxon != null && parentTaxon != null) {
					
					tree.addParentChild(parentTaxon, childTaxon, sourceRef, null);
					
					if (logger.isDebugEnabled()) {
						logger.debug("Parent-child (" + mappedParentUuid + "-" + childUuid + 
						") relationship created");
					}
					if (childTaxon != null && !childSet.contains(childTaxon)) {
						
						childSet.add(childTaxon);
						
						if (logger.isTraceEnabled()) {
							logger.trace("Child taxon (" + childUuid + ") added to Set");
						}
						
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Duplicated child taxon (" + childUuid + ")");
						}
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Parent(" + mappedParentUuid + ") or child (" + childUuid + " is null");
					}
				}
				
			} catch (Exception e) {
				logger.error("Error creating taxonomically included relationship parent-child (" + 
					mappedParentUuid + " <----> " + childUuid + ")", e);
			}

		}
		if (logger.isTraceEnabled()) {
			logger.trace("Start saving childSet");
		}
		getTaxonService().save(childSet);
		if (logger.isTraceEnabled()) {
			logger.trace("End saving childSet");
		}

		parentValuesSet = null;
		childSet = null;
		children = null;
		parents = null;
		tree = null;
		
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

	
	/* Creates heterotypic synonym relationships.
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
				synonym = CdmBase.deproxy(synonymTaxonBase, Synonym.class);
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
					acceptedTaxon.addSynonym(synonym, SynonymRelationshipType.HETEROTYPIC_SYNONYM_OF());

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
				logger.error("Error creating synonym relationship: accepted taxon-synonym (" + 
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

}
