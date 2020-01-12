/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.TaxonServiceImpl;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.common.mapping.out.DbConstantMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.ObjectChangeMapper;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.name.NameTypeDesignation;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.cache.HTMLTagRules;
import eu.etaxonomy.cdm.strategy.cache.TagEnum;
import eu.etaxonomy.cdm.strategy.cache.name.INonViralNameCacheStrategy;
import eu.etaxonomy.cdm.strategy.cache.name.TaxonNameDefaultCacheStrategy;
import eu.etaxonomy.cdm.strategy.cache.name.ZooNameNoMarkerCacheStrategy;

/**
 * The export class for {@link eu.etaxonomy.cdm.model.name.TaxonNameBase TaxonNames}.<p>
 * Inserts into DataWarehouse database table <code>Taxon</code>.
 * It is divided into four phases:<p><ul>
 * <li>Phase 1:	Export of all taxon and taxon name data
 *                     except for some data exported in the following phases.
 * <li>Phase 2:	Export of additional data: ParentTaxonFk and TreeIndex.
 * <li>Phase 3:	Export of additional data: Rank data, KingdomFk, TypeNameFk, expertFk and speciesExpertFk.
 * <li>Phase 4:	Export of Inferred Synonyms.</ul>
 *
 * @author e.-m.lee
 * @since 23.02.2010
 */
@Component
public class PesiTaxonExport extends PesiExportBase {

    private static final long serialVersionUID = -3412722058790200078L;
    private static final Logger logger = Logger.getLogger(PesiTaxonExport.class);

	private static final Class<? extends CdmBase> standardMethodParameter = TaxonBase.class;

	private static int modCount = 1000;
	private static final String dbTableName = "Taxon";
	private static final String dbTableNameSynRel = "RelTaxon";
	private static final String dbTableAdditionalSourceRel = "AdditionalTaxonSource";

	private static final String pluralString = "Taxa";
	private static final String parentPluralString = "Taxa";
	private static final String pluralStringNames = "Names";

//	private PreparedStatement parentTaxonFk_TreeIndex_KingdomFkStmts;
	private PreparedStatement parentTaxonFkStmt;
	private PreparedStatement rankTypeExpertsUpdateStmt;
	private PreparedStatement rankUpdateStmt;
	private Integer kingdomFk;

	private static ExtensionType lastActionExtensionType;
	private static ExtensionType lastActionDateExtensionType;
	private static ExtensionType expertNameExtensionType;
	private static ExtensionType speciesExpertNameExtensionType;
	private static ExtensionType cacheCitationExtensionType;

	public static TaxonNameDefaultCacheStrategy zooNameStrategy = ZooNameNoMarkerCacheStrategy.NewInstance();
	public static TaxonNameDefaultCacheStrategy nonViralNameStrategy = TaxonNameDefaultCacheStrategy.NewInstance();
	private static int currentTaxonId;

	enum NamePosition {
		beginning,
		end,
		between,
		alone,
		nowhere
	}

	public PesiTaxonExport() {
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

			initPreparedStatements(state);

			// Stores whether this invoke was successful or not.
			boolean success = true;

			// PESI: Clear the database table Taxon.
			doDelete(state);

			// Get specific mappings: (CDM) Taxon -> (PESI) Taxon
			PesiExportMapping mapping = getMapping();
			PesiExportMapping synonymRelMapping = getSynRelMapping();
			PesiExportMapping additionalSourceMapping = getAdditionalSourceMapping(state);

			// Initialize the db mapper
			mapping.initialize(state);
			synonymRelMapping.initialize(state);
			additionalSourceMapping.initialize(state);

			// Find extensionTypes
			lastActionExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtLastAction);
			lastActionDateExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtLastActionDate);
			expertNameExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtExpertName);
			speciesExpertNameExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtSpeciesExpertName);
			cacheCitationExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtCacheCitation);

			//Export Taxa..
			success &= doPhase01(state, mapping, additionalSourceMapping);

			//"PHASE 1b: Handle names without taxa ...
			success &= doPhase01b_Names(state, additionalSourceMapping);

			// 2nd Round: Add ParentTaxonFk to each taxon
			success &= doPhase02(state);

			//PHASE 3: Add Rank data, KingdomFk, TypeNameFk ...
			success &= doPhase03(state);

			// 4nd Round: Add TreeIndex to each taxon
			success &= doPhase04(state);

			//"PHASE 5: Creating Inferred Synonyms...
			success &= doPhase05(state, mapping, synonymRelMapping);

			logger.info("*** Finished Making " + pluralString + " ..." + getSuccessString(success));

			if (!success){
				state.getResult().addError("An error occurred in PesiTaxonExport.doInvoke. Success = false");
			}
			return;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.getResult().addException(e);
		}
	}


	private void initPreparedStatements(PesiExportState state) throws SQLException {
//		initTreeIndexStatement(state);
		initRankExpertsUpdateStmt(state);
		initRankUpdateStatement(state);

		initParentFkStatement(state);
	}

//	// Prepare TreeIndex-And-KingdomFk-Statement
//	private void initTreeIndexStatement(PesiExportState state) throws SQLException {
//		Connection connection = state.getConfig().getDestination().getConnection();
//		String parentTaxonFk_TreeIndex_KingdomFkSql = "UPDATE Taxon SET ParentTaxonFk = ?, TreeIndex = ? WHERE TaxonId = ?";
//		parentTaxonFk_TreeIndex_KingdomFkStmt = connection.prepareStatement(parentTaxonFk_TreeIndex_KingdomFkSql);
//	}

	// Prepare TreeIndex-And-KingdomFk-Statement
	private void initParentFkStatement(PesiExportState state) throws SQLException {
		Connection connection = state.getConfig().getDestination().getConnection();
		String parentTaxonFkSql = "UPDATE Taxon SET ParentTaxonFk = ? WHERE TaxonId = ?";
		parentTaxonFkStmt = connection.prepareStatement(parentTaxonFkSql);
	}

	private void initRankUpdateStatement(PesiExportState state) throws SQLException {
		Connection connection = state.getConfig().getDestination().getConnection();
		String rankSql = "UPDATE Taxon SET RankFk = ?, RankCache = ?, KingdomFk = ? WHERE TaxonId = ?";
		rankUpdateStmt = connection.prepareStatement(rankSql);
	}

	private void initRankExpertsUpdateStmt(PesiExportState state) throws SQLException {
//		String sql_old = "UPDATE Taxon SET RankFk = ?, RankCache = ?, TypeNameFk = ?, KingdomFk = ?, " +
//				"ExpertFk = ?, SpeciesExpertFk = ? WHERE TaxonId = ?";
		//TODO handle experts GUIDs
		Connection connection = state.getConfig().getDestination().getConnection();

		String sql = "UPDATE Taxon SET RankFk = ?, RankCache = ?, TypeNameFk = ?, KingdomFk = ? " +
				" WHERE TaxonId = ?";
		rankTypeExpertsUpdateStmt = connection.prepareStatement(sql);
	}

	private boolean doPhase01(PesiExportState state, PesiExportMapping mapping, PesiExportMapping additionalSourceMapping){

	    int count = 0;
		int pastCount = 0;
		boolean success = true;
		// Get the limit for objects to save within a single transaction.
		int limit = state.getConfig().getLimitSave();

		logger.info("PHASE 1: Export Taxa...limit is " + limit);
		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		if (logger.isDebugEnabled()) {
            logger.info("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");
            logger.info("Taking snapshot at the beginning of phase 1 of taxonExport");
            //ProfilerController.memorySnapshot();
        }

		int partitionCount = 0;
		List<TaxonBase<?>> list;
		while ((list = getNextTaxonPartition(null, limit, partitionCount++, null)) != null   ) {

			logger.debug("Fetched " + list.size() + " " + pluralString + ". Exporting...");

			for (TaxonBase<?> taxon : list) {
				doCount(count++, modCount, pluralString);
				TaxonName taxonName = taxon.getName();

				TaxonName nvn = CdmBase.deproxy(taxonName);
				if (! nvn.isProtectedTitleCache()){
					nvn.setTitleCache(null, false);
				}
				if (! nvn.isProtectedNameCache()){
					nvn.setNameCache(null, false);
				}
				if (! nvn.isProtectedFullTitleCache()){
					nvn.setFullTitleCache(null, false);
				}
				if (! nvn.isProtectedAuthorshipCache()){
					nvn.setAuthorshipCache(null, false);
				}
				try{
    				if (nvn.getRank().equals(Rank.KINGDOM())){
    				    if(taxon.isInstanceOf(Taxon.class)){
    				        String treeIndex = ((Taxon)taxon).getTaxonNodes().iterator().next().treeIndex();
    				        Integer kingdomId = PesiTransformer.pesiKingdomId(nvn.getGenusOrUninomial());
    				        state.getTreeIndexKingdomMap().put(treeIndex, kingdomId);
    				    }else{
    				        logger.warn("Kingdom taxon is not of class Taxon but " + taxon.getClass().getSimpleName() + ": " + nvn.getGenusOrUninomial());
    				    }
    				}
				}catch(NullPointerException e){
				    logger.error(nvn.getTitleCache() + " has no Rank!");
				    System.err.println(nvn.getTitleCache() + " has no Rank!");
				}
				//core mapping
				success &= mapping.invoke(taxon);
				//additional source
				if (nvn.getNomenclaturalReference() != null || StringUtils.isNotBlank(nvn.getNomenclaturalMicroReference() )){
					additionalSourceMapping.invoke(taxon);
				}

				//TODO switch on again, leads to some warnings in ERMS for taxa of not correctly handled kingdoms
				validatePhaseOne(taxon, nvn);
			}

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " " + pluralString + ". Total: " + count + " (Phase 01)");
			pastCount = count;

			// Start new transaction
			txStatus = startTransaction(true);
			if (logger.isDebugEnabled()) {
                logger.info("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");
            }

		}
		logger.debug("No " + pluralString + " left to fetch.");

		// Commit transaction
		commitTransaction(txStatus);
		txStatus = null;

		return success;
	}

	private void validatePhaseOne(TaxonBase<?> taxon, TaxonName taxonName) {

	    // Check whether some rules are violated
		String genusOrUninomial = taxonName.getGenusOrUninomial();
		String specificEpithet = taxonName.getSpecificEpithet();
		String infraSpecificEpithet = taxonName.getInfraSpecificEpithet();
		String infraGenericEpithet = taxonName.getInfraGenericEpithet();
		Rank rank =  taxonName.getRank();

		//as kingdomFk can not be defined in Phase 01 the below code was switched to use the CDM rank.
		//This may be changed if we move validation to Phase03 or later
//		Integer rankFk = getRankFk(taxonName, taxonName.getNameType());
//		if (rankFk == null) {
//			logger.error("Rank was not determined: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
//		} else {

			// Check whether infraGenericEpithet is set correctly
			// 1. Childs of an accepted taxon of rank subgenus that are accepted taxa of rank species have to have an infraGenericEpithet
			// 2. Grandchilds of an accepted taxon of rank subgenus that are accepted taxa of rank subspecies have to have an infraGenericEpithet

			int ancestorLevel = 0;
			if (rank == null){
			    logger.warn("PhaseOne validation: Taxon name has no rank: " + taxonName.getTitleCache());
			}else if (rank.equals(Rank.SUBSPECIES())) {
				// The accepted taxon two rank levels above should be of rank subgenus
				ancestorLevel  = 2;
			}else if (rank.equals(Rank.SPECIES())) {
				// The accepted taxon one rank level above should be of rank subgenus
				ancestorLevel = 1;
			}
			if (ancestorLevel > 0) {
				if (validateAncestorOfSpecificRank(taxon, ancestorLevel, Rank.SUBGENUS())) {
					// The child (species or subspecies) of this parent (subgenus) has to have an infraGenericEpithet
					if (infraGenericEpithet == null) {
						logger.warn("InfraGenericEpithet for (sub)species of infrageneric taxon does not exist even though it should (also valid for Botanical Names?) for: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
						// maybe the taxon could be named here
					}
				}
			}

			if (rank != null){
			    if (infraGenericEpithet == null && rank.isInfraGenericButNotSpeciesGroup()) {
			        logger.warn("InfraGenericEpithet was not determined although it should exist for infra generic names: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
			    }
			    if (specificEpithet != null && (rank.isInfraGenericButNotSpeciesGroup()||rank.isGenus()||rank.isSupraGeneric())) {
			        logger.warn("SpecificEpithet was determined for rank " + rank.getTitleCache() + " although it should only exist for species aggregates, species or infraspecific taxa: TaxonName " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
			    }
			    if (infraSpecificEpithet != null && !rank.isInfraSpecific()) {
			        String message = "InfraSpecificEpithet '" +infraSpecificEpithet + "' was determined for rank " + rank.getTitleCache() + " although it should only exist for rank species and higher: "  + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")";
			        if (StringUtils.isNotBlank(infraSpecificEpithet)){
			            logger.warn(message);
			        }else{
			            logger.warn(message);
			        }
			    }
			}
//		}
		if (infraSpecificEpithet != null && specificEpithet == null) {
			logger.warn("An infraSpecificEpithet was determined, but a specificEpithet was not determined: "  + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
		}
		if (genusOrUninomial == null) {
			logger.warn("GenusOrUninomial was not determined: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
		}
	}

	/**
	 * 2nd Round: Add ParentTaxonFk to each taxon and add Biota if not exists
	 */
	private boolean doPhase02(PesiExportState state) {
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		if (! state.getConfig().isDoParentAndBiota()){
			logger.info ("Ignore PHASE 2: Make ParentFk and Biota...");
			return success;
		}

		// Get the limit for objects to save within a single transaction.
		int limit = state.getConfig().getLimitSave();

		insertBiota(state);

		logger.info("PHASE 2: Make ParentFk and Biota ... limit is " + limit);
		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		int partitionCount = 0;

//		ProfilerController.memorySnapshot();
		List<Taxon> list;
		while ((list = getNextTaxonPartition(Taxon.class, limit, partitionCount++, null)) != null   ) {

			if(logger.isDebugEnabled()) {
                logger.info("Fetched " + list.size() + " " + pluralString + ". Exporting...");
            }
			for (Taxon taxon : list) {
				for (TaxonNode node : taxon.getTaxonNodes()){
					doCount(count++, modCount, pluralString);
					TaxonNode parentNode = node.getParent();
					if (parentNode != null && isPesiTaxon(parentNode.getTaxon())){ //exclude root taxa and unpublished parents (relevant for "Valueless" parent for E+M Rubus taxa). Usually a parent should not be unpublished
						int childId = state.getDbId( taxon);
						int parentId = state.getDbId(parentNode.getTaxon());
						success &= invokeParentTaxonFk(parentId, childId);
					}
				}
			}

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " " + pluralString + ". Total: " + count + " (Phase 2)");
			pastCount = count;
			// Start transaction
			txStatus = startTransaction(true);
			if (logger.isDebugEnabled()){
			    logger.info("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") ...");
			}
		}
		logger.debug("No " + pluralString + " left to fetch.");

		// Commit transaction
		commitTransaction(txStatus);

		return success;
	}

	/**
	 * Inserts the Biota Taxon if not yet exists.
	 */
	private void insertBiota(PesiExportState state) {
		try {
			ResultSet rs = state.getConfig().getDestination().getResultSet("SELECT * FROM Taxon WHERE GenusOrUninomial = 'Biota' ");
			if (rs.next() == false){
				int biotaId = state.getConfig().getNameIdStart() -1 ;
				String sqlInsertBiota = "INSERT INTO Taxon (TaxonId, KingdomFk, RankFk, RankCache, GenusOrUninomial, WebSearchName, WebShowName, FullName, DisplayName, TaxonStatusFk, TaxonStatusCache) " +
									       " VALUES (" + biotaId + ",    0,    0,   'Superdomain',   'Biota',          'Biota',  '<i>Biota</i>',   'Biota', '<i>Biota</i>',  1 ,      'accepted')";
				state.getConfig().getDestination().update(sqlInsertBiota);
			}
			rs = null;
		} catch (SQLException e) {
			logger.warn ("Biota could not be requested or inserted");
		}
	}

	//PHASE 3: Add Rank data, KingdomFk, TypeNameFk, expertFk and speciesExpertFk...
	private boolean doPhase03(PesiExportState state) {
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		if (! state.getConfig().isDoTreeIndex()){
			logger.info ("Ignore PHASE 3: Add Rank data, KingdomFk, TypeNameFk, expertFk and speciesExpertFk...");
			return success;
		}

		addValuelessTaxonToKingdomMap(state);

		// Get the limit for objects to save within a single transaction.
		int limit = state.getConfig().getLimitSave();

		logger.info("PHASE 3: Add Rank data, KingdomFk, TypeNameFk, expertFk and speciesExpertFk...");

		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		if (logger.isDebugEnabled()) {
            logger.info("Started new transaction for rank, kingdom, typeName, expertFk and speciesExpertFK. Fetching some " + pluralString + " (max: " + limit + ") ...");
        }
		int partitionCount = 0;
		@SuppressWarnings("rawtypes")
        List<TaxonBase> list;
		while ((list = getNextTaxonPartition(TaxonBase.class, limit, partitionCount++, null)) != null) {

			if (logger.isDebugEnabled()) {
                logger.debug("Fetched " + list.size() + " " + pluralString + ". Exporting...");
            }
			for (TaxonBase<?> taxon : list) {
				TaxonName taxonName = CdmBase.deproxy(taxon.getName());
				// Determine expertFk
//				Integer expertFk = makeExpertFk(state, taxonName);
//
//				// Determine speciesExpertFk
//				Integer speciesExpertFk = makeSpeciesExpertFk(state, taxonName);

				doCount(count++, modCount, pluralString);
				Integer typeNameFk = getTypeNameFk(taxonName, state);
				Integer kingdomFk = findKingdomIdFromTreeIndex(taxon, state);
				Integer rankFk = getRankFk(taxonName, kingdomFk);

			    invokeRankDataAndTypeNameFkAndKingdomFk(taxonName, state.getDbId(taxon),
						typeNameFk, kingdomFk, rankFk, state);
			}

			// Commit transaction
			commitTransaction(txStatus);
			if (logger.isDebugEnabled()){logger.debug("Committed transaction.");}
			logger.info("Exported " + (count - pastCount) + " " + pluralString + ". Total: " + count + " (Phase 3)");
			pastCount = count;

			// Start transaction
			txStatus = startTransaction(true);
			if (logger.isDebugEnabled()) {
                logger.info("Started new transaction for rank, kingdom, typeName, expertFk and speciesExpertFK. Fetching some " + pluralString + " (max: " + limit + ") ...");
            }
		}
		logger.debug("No " + pluralString + " left to fetch.");

		// Commit transaction
		commitTransaction(txStatus);

		if (logger.isDebugEnabled()){
		    logger.debug("Committed transaction.");
		    logger.debug("Try to take snapshot at the end of phase 3 of taxonExport, number of partitions: " + partitionCount);
		    //ProfilerController.memorySnapshot();
		}
		return success;
	}

    private void addValuelessTaxonToKingdomMap(PesiExportState state) {
        TransactionStatus txStatus = startTransaction();
        Taxon valuelessTaxon = (Taxon)getTaxonService().find(PesiTransformer.uuidTaxonValuelessEuroMed);
        if (valuelessTaxon != null){
            String treeIndex = valuelessTaxon.getTaxonNodes().iterator().next().treeIndex();
            Integer kingdomId = PesiTransformer.pesiKingdomId("Plantae");
            state.getTreeIndexKingdomMap().put(treeIndex, kingdomId);
        }
        commitTransaction(txStatus);
    }

    // 4th round: Add TreeIndex to each taxon
    private boolean doPhase04(PesiExportState state) {
        boolean success = true;

        logger.info("PHASE 4: Make TreeIndex ... ");

        //TODO test if possible to move to phase 02
        String sql = " UPDATE Taxon SET ParentTaxonFk = (SELECT TaxonId FROM Taxon WHERE RankFk = 0) " +
                " WHERE (RankFk = 10) and TaxonStatusFk = 1 ";
        state.getConfig().getDestination().update(sql);

        state.getConfig().getDestination().update("EXEC dbo.recalculateallstoredpaths");

        logger.info("PHASE 4: Make TreeIndex DONE");

        return success;
    }

    private static Integer findKingdomIdFromTreeIndex(TaxonBase<?> taxonBase, PesiExportState state) {
        Taxon taxon;
        if (taxonBase instanceof Synonym){
            taxon = ((Synonym) taxonBase).getAcceptedTaxon();
        }else{
            taxon = checkPseudoOrRelatedTaxon((Taxon)taxonBase);
        }
        if (taxon == null){
            NomenclaturalCode nomenclaturalCode = taxonBase.getName().getNameType();
            logger.warn("Taxon is synonym with no accepted taxon attached: " + taxonBase.getTitleCache() + ". The kingdom is taken from the nomenclatural code: " + PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode) );
            return PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode);
        } else{
            Set<TaxonNode> nodes = taxon.getTaxonNodes();
            if (nodes.isEmpty()){
                NomenclaturalCode nomenclaturalCode = taxon.getName().getNameType();
                logger.warn("The taxon has no nodes: " + taxon.getTitleCache() + ". The kingdom is taken from the nomenclatural code: " + PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode));
                return PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode);
            } else {
                if (nodes.size()>1){
                    logger.warn("The taxon has more then 1 taxon node: " + taxon.getTitleCache() + ". Take arbitrary one.");
                }
                String treeIndex = nodes.iterator().next().treeIndex();

                Pattern pattern = Pattern.compile("#t[0-9]+#([0-9]+#){3}");
                Matcher matcher = pattern.matcher(treeIndex);
                Integer kingdomID = null;
                if(matcher.find()) {
                    String treeIndexKingdom = matcher.group(0);
                    kingdomID = state.getTreeIndexKingdomMap().get(treeIndexKingdom);
                }
                if (kingdomID == null){
                    pattern = Pattern.compile("#t[0-9]+#([0-9]+#){2}");
                    matcher = pattern.matcher(treeIndex);
                    if(matcher.find()) {
                        String treeIndexKingdom = matcher.group(0);
                        Map<String, Integer> map = state.getTreeIndexKingdomMap();
                        kingdomID = map.get(treeIndexKingdom);
                    }
                }
                if(Rank.DOMAIN().equals(taxon.getName().getRank())){
                    return 0;
                }
                if(kingdomID == null){
                    logger.warn("Kingdom could not be defined for treeindex " + treeIndex);
                }
                return kingdomID;
            }
        }
    }

    private static Taxon checkPseudoOrRelatedTaxon(Taxon taxon) {
        if (!taxon.getTaxonNodes().isEmpty()){
            return taxon;
        }else if(hasPseudoTaxonRelationship(taxon)){
            return acceptedPseudoTaxon(taxon);
        }else if(isMisappliedNameOrProParteSynonym(taxon)){
            return acceptedTaxonConcept(taxon);
        }else{
            return taxon;
        }
    }

    private static Taxon acceptedPseudoTaxon(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid())){
                return rel.getToTaxon();
            }
        }
        return taxon;
    }

    private static Taxon acceptedTaxonConcept(Taxon taxon) {
       for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.misappliedNameUuids().contains(rel.getType().getUuid())||
                    TaxonRelationshipType.proParteOrPartialSynonymUuids().contains(rel.getType().getUuid())){
                return rel.getToTaxon();
            }
        }
        return taxon;
    }

    private static boolean hasPseudoTaxonRelationship(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid())){
                return true;
            }
        }
        return false;
    }

    private static boolean isMisappliedNameOrProParteSynonym(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.misappliedNameUuids().contains(rel.getType().getUuid())||
                    TaxonRelationshipType.proParteOrPartialSynonymUuids().contains(rel.getType().getUuid())){
                return true;
            }
        }
        return false;
    }

    //	"PHASE 5: Creating Inferred Synonyms..."
	private boolean doPhase05(PesiExportState state, PesiExportMapping mapping, PesiExportMapping synRelMapping) {
		int count;
		int pastCount;
		boolean success = true;
		// Get the limit for objects to save within a single transaction.
		if (! state.getConfig().isDoInferredSynonyms()){
			logger.info ("Ignore PHASE 5: Creating Inferred Synonyms...");
			return success;
		}

		int limit = state.getConfig().getLimitSave();
		// Create inferred synonyms for accepted taxa
		logger.info("PHASE 5: Creating Inferred Synonyms...");

		// Determine the count of elements in data warehouse database table Taxon
		currentTaxonId = determineTaxonCount(state);
		currentTaxonId++;

		count = 0;
		pastCount = 0;
		int pageSize = limit/10;
		int pageNumber = 1;
		String inferredSynonymPluralString = "Inferred Synonyms";

		// Start transaction
		TransactionStatus txStatus = startTransaction(true);
		if (logger.isDebugEnabled()) {
            logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");
        }

		List<TaxonBase> taxonList = null;
		while ((taxonList  = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*", Rank.SPECIES(), pageSize, pageNumber, null)).size() > 0) {

		    Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

			if (logger.isDebugEnabled()) {
                logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            }
			inferredSynonymsDataToBeSaved.putAll(createInferredSynonymsForTaxonList(state, mapping,
					synRelMapping, taxonList));

			doCount(count += taxonList.size(), modCount, inferredSynonymPluralString);
			// Commit transaction
			commitTransaction(txStatus);
			if (logger.isDebugEnabled()){logger.debug("Committed transaction.");}
			logger.info("Exported " + (taxonList.size()) + " " + inferredSynonymPluralString + ". Total: " + count);
			//pastCount = count;

			// Save Rank Data and KingdomFk for inferred synonyms
			for (Integer taxonFk : inferredSynonymsDataToBeSaved.keySet()) {
			    TaxonName taxonName = inferredSynonymsDataToBeSaved.get(taxonFk);
                invokeRankDataAndKingdomFk(inferredSynonymsDataToBeSaved.get(taxonFk), taxonFk, kingdomFk, state);
			}

			// Start transaction
			txStatus = startTransaction(true);
			if (logger.isDebugEnabled()) {
                logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");
            }

			// Increment pageNumber
			pageNumber++;
		}
		taxonList = null;
		while ((taxonList  = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*", Rank.SUBSPECIES(), pageSize, pageNumber, null)).size() > 0) {
			Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

			logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
			inferredSynonymsDataToBeSaved.putAll(createInferredSynonymsForTaxonList(state, mapping,
					synRelMapping, taxonList));

			doCount(count += taxonList.size(), modCount, inferredSynonymPluralString);
			// Commit transaction
			commitTransaction(txStatus);
			logger.debug("Committed transaction.");
			logger.info("Exported " + taxonList.size()+ " " + inferredSynonymPluralString + ". Total: " + count);
			//pastCount = count;

			// Save Rank Data and KingdomFk for inferred synonyms
			for (Integer taxonFk : inferredSynonymsDataToBeSaved.keySet()) {
			    TaxonName taxonName = inferredSynonymsDataToBeSaved.get(taxonFk);
			    invokeRankDataAndKingdomFk(taxonName, taxonFk, kingdomFk, state);
			}

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
//		logger.warn("Taking snapshot at the end of phase 5 of taxonExport");
//		ProfilerController.memorySnapshot();

		// Commit transaction
		commitTransaction(txStatus);
		System.gc();
		logger.debug("Taking snapshot at the end of phase 5 after gc() of taxonExport");
		//ProfilerController.memorySnapshot();
		logger.debug("Committed transaction.");
		return success;
	}

	private Map<Integer, TaxonName> createInferredSynonymsForTaxonList(PesiExportState state,
			PesiExportMapping mapping, PesiExportMapping synRelMapping,	 List<TaxonBase> taxonList) {

		Taxon acceptedTaxon;
		Classification classification = null;
		List<Synonym> inferredSynonyms = null;
		boolean localSuccess = true;

		Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

		for (TaxonBase<?> taxonBase : taxonList) {

			if (taxonBase.isInstanceOf(Taxon.class)) { // this should always be the case since we should have fetched accepted taxon only, but you never know...
				acceptedTaxon = CdmBase.deproxy(taxonBase, Taxon.class);
				TaxonName taxonName = acceptedTaxon.getName();

				if (taxonName.isZoological()) {
					kingdomFk = findKingdomIdFromTreeIndex(taxonBase, state);

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
								inferredSynonyms  = getTaxonService().createAllInferredSynonyms(acceptedTaxon, classification, true);
							//}
//								inferredSynonyms = getTaxonService().createInferredSynonyms(classification, acceptedTaxon, SynonymType.INFERRED_GENUS_OF());
							if (inferredSynonyms != null) {
								for (Synonym synonym : inferredSynonyms) {
//									TaxonName synonymName = synonym.getName();
									MarkerType markerType =getUuidMarkerType(PesiTransformer.uuidMarkerGuidIsMissing, state);
									synonym.addMarker(Marker.NewInstance(markerType, true));
									// Both Synonym and its TaxonName have no valid Id yet
									synonym.setId(currentTaxonId++);


									localSuccess &= mapping.invoke(synonym);
									//get SynonymRelationship and export
									if (synonym.getAcceptedTaxon() == null ){
										IdentifiableSource source = synonym.getSources().iterator().next();
										if (source.getIdNamespace().contains("Potential combination")){
											acceptedTaxon.addSynonym(synonym, SynonymType.POTENTIAL_COMBINATION_OF());
											logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to potential combination");
										} else if (source.getIdNamespace().contains("Inferred Genus")){
											acceptedTaxon.addSynonym(synonym, SynonymType.INFERRED_GENUS_OF());
											logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred genus");
										} else if (source.getIdNamespace().contains("Inferred Epithet")){
											acceptedTaxon.addSynonym(synonym, SynonymType.INFERRED_EPITHET_OF());
											logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred epithet");
										} else{
											acceptedTaxon.addSynonym(synonym, SynonymType.INFERRED_SYNONYM_OF());
											logger.error(synonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred synonym");
										}

										localSuccess &= synRelMapping.invoke(synonym);
										if (!localSuccess) {
											logger.error("Synonym relationship export failed " + synonym.getTitleCache() + " accepted taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
										}
									} else {
										localSuccess &= synRelMapping.invoke(synonym);
										if (!localSuccess) {
											logger.error("Synonym relationship export failed " + synonym.getTitleCache() + " accepted taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
										} else {
											logger.info("Synonym relationship successfully exported: " + synonym.getTitleCache() + "  " +acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
										}
									}

									inferredSynonymsDataToBeSaved.put(synonym.getId(), synonym.getName());
								}
							}
						}catch(Exception e){
							logger.error(e.getMessage());
							e.printStackTrace();
						}
					} else {
						logger.error("Classification is NULL. Inferred Synonyms could not be created for this Taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache() + ")");
					}
				} else {
//							logger.error("TaxonName is not a ZoologicalName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
				}
			} else {
				logger.error("This TaxonBase is not a Taxon even though it should be: " + taxonBase.getUuid() + " (" + taxonBase.getTitleCache() + ")");
			}
		}
		return inferredSynonymsDataToBeSaved;
	}

	/**
	 * Handles names that do not appear in taxa.
	 */
	private boolean doPhase01b_Names(PesiExportState state, PesiExportMapping additionalSourceMapping) {

		boolean success = true;
		if (! state.getConfig().isDoPureNames()){
			logger.info ("Ignore PHASE 1b: PureNames");
			return success;
		}

		try {
			PesiExportMapping mapping = getPureNameMapping(state);
			mapping.initialize(state);
			int count = 0;
			int pastCount = 0;
			success = true;
			// Get the limit for objects to save within a single transaction.
			int limit = state.getConfig().getLimitSave();

			logger.info("PHASE 1b: Export Pure Names ...");
			// Start transaction
			TransactionStatus txStatus = startTransaction(true);
			logger.info("Started new transaction for Pure Names. Fetching some " + pluralString + " (max: " + limit + ") ...");

			int partitionCount = 0;
			List<TaxonName> list;
			while ((list = getNextPureNamePartition(null, limit, partitionCount++)) != null   ) {

				logger.debug("Fetched " + list.size() + pluralStringNames + " without taxa. Exporting...");
				for (TaxonName taxonName : list) {
					doCount(count++, modCount, pluralString);
					success &= mapping.invoke(taxonName);
					//additional source
					if (taxonName.getNomenclaturalReference() != null || StringUtils.isNotBlank(taxonName.getNomenclaturalMicroReference() )){
						additionalSourceMapping.invoke(taxonName);
					}
				}

				// Commit transaction
				commitTransaction(txStatus);
				logger.debug("Committed transaction.");
				logger.info("Exported " + (count - pastCount) + " " + pluralStringNames + ". Total: " + count + ". Partition: " + partitionCount);
				pastCount = count;

				// Start transaction
				txStatus = startTransaction(true);
				logger.debug("Started new transaction for PureNames. Fetching some " + pluralString + " (max: " + limit + ") ...");
			}
			logger.debug("No " + pluralString + " left to fetch.");

			// Commit transaction
			commitTransaction(txStatus);
			logger.debug("Committed transaction.");
		} catch (Exception e) {
			logger.error("Error occurred in pure name export");
			e.printStackTrace();
			success = false;
		}
		return success;
	}

	/**
	 * Determines the current number of entries in the DataWarehouse database table <code>Taxon</code>.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The count.
	 */
	private Integer determineTaxonCount(PesiExportState state) {
		Integer result = null;
		PesiExportConfigurator pesiConfig = state.getConfig();

		String sql;
		Source destination =  pesiConfig.getDestination();
		sql = "SELECT max(taxonId) FROM Taxon";
		destination.setQuery(sql);
		ResultSet resultSet = destination.getResultSet();
		try {
			resultSet.next();
			result = resultSet.getInt(1);
		} catch (SQLException e) {
			logger.error("TaxonCount could not be determined: " + e.getMessage());
			e.printStackTrace();
		}
		resultSet = null;
		return result;
	}

	/**
	 * Checks whether a parent at specific level has a specific Rank.
	 * @param taxonName A {@link TaxonNameBase TaxonName}.
	 * @param level The ancestor level.
	 * @param ancestorRank The ancestor rank.
	 * @return Whether a parent at a specific level has a specific Rank.
	 */
	private boolean validateAncestorOfSpecificRank(TaxonBase<?> taxonBase, int level, Rank ancestorRank) {
		boolean result = false;
		TaxonNode parentNode = null;
		if (taxonBase.isInstanceOf(Taxon.class)){
			Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
			// Get ancestor Taxon via TaxonNode
			Set<TaxonNode> taxonNodes = taxon.getTaxonNodes();
			if (taxonNodes.size() == 1) {
				TaxonNode taxonNode = taxonNodes.iterator().next();
				if (taxonNode != null) {
					for (int i = 0; i < level; i++) {
						if (taxonNode != null) {
							taxonNode  = taxonNode.getParent();
						}
					}
					parentNode = taxonNode;
				}
			} else if (taxonNodes.size() > 1) {
				logger.error("This taxon has " + taxonNodes.size() + " taxonNodes: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
			}
		}
		//compare
		if (parentNode != null) {
			TaxonNode node = CdmBase.deproxy(parentNode, TaxonNode.class);
			Taxon parentTaxon = node.getTaxon();
			if (parentTaxon != null) {
				TaxonName parentTaxonName = parentTaxon.getName();
				if (parentTaxonName != null && parentTaxonName.getRank().equals(ancestorRank)) {
					result = true;
				}
			} else if (parentNode.treeIndex().matches("#t\\d+#\\d+#")) {
				//do nothing (is root node)
			} else {
				logger.error("This TaxonNode has no Taxon: " + node.getUuid());
			}
		}
		return result;
	}

	/**
	 * Returns the AnnotationType for a given UUID.
	 * @param uuid The Annotation UUID.
	 * @param label The Annotation label.
	 * @param text The Annotation text.
	 * @param labelAbbrev The Annotation label abbreviation.
	 * @return The AnnotationType.
	 */
	protected AnnotationType getAnnotationType(UUID uuid, String label, String text, String labelAbbrev){
		AnnotationType annotationType = (AnnotationType)getTermService().find(uuid);
		if (annotationType == null) {
			annotationType = AnnotationType.NewInstance(label, text, labelAbbrev);
			annotationType.setUuid(uuid);
//			annotationType.setVocabulary(AnnotationType.EDITORIAL().getVocabulary());
			getTermService().save(annotationType);
		}
		return annotationType;
	}

	private boolean invokeParentTaxonFk(Integer parentId, Integer childId) {
		try {
			parentTaxonFkStmt.setInt(1, parentId);
			parentTaxonFkStmt.setInt(2, childId);
			parentTaxonFkStmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.warn("ParentTaxonFk (" + (parentId ==null? "-":parentId) + ") could not be inserted into database "
			        + "for taxon "+ (childId == null? "-" :childId) + ": " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Inserts Rank data and KingdomFk into the Taxon database table.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param nomenclaturalCode The {@link NomenclaturalCode NomenclaturalCode}.
	 * @param taxonFk The TaxonFk to store the values for.
	 * @param state
	 * @param kindomFk The KingdomFk.
	 * @return Whether save was successful or not.
	 */
	private boolean invokeRankDataAndKingdomFk(TaxonName taxonName,
	        Integer taxonFk, Integer kingdomFk, PesiExportState state) {

	    try {
			Integer rankFk = getRankFk(taxonName, kingdomFk);
			if (rankFk != null) {
				rankUpdateStmt.setInt(1, rankFk);
			} else {
				rankUpdateStmt.setObject(1, null);
			}

			String rankCache = getRankCache(taxonName, kingdomFk, state);
			if (rankCache != null) {
				rankUpdateStmt.setString(2, rankCache);
			} else {
				rankUpdateStmt.setObject(2, null);
			}

			if (kingdomFk != null) {

				rankUpdateStmt.setInt(3, kingdomFk);
			} else {
				rankUpdateStmt.setObject(3, null);
			}

			if (taxonFk != null) {
				rankUpdateStmt.setInt(4, taxonFk);
			} else {
				rankUpdateStmt.setObject(4, null);
			}

			rankUpdateStmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error("Data (RankFk, RankCache, KingdomFk) could not be inserted into database: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Inserts Rank data, TypeNameFk, KingdomFk, expertFk and speciesExpertFk into the Taxon database table.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param nomenclaturalCode The {@link NomenclaturalCode NomenclaturalCode}.
	 * @param taxonFk The TaxonFk to store the values for.
	 * @param typeNameFk The TypeNameFk.
	 * @param rankFk
	 * @param state
	 * @param kindomFk The KingdomFk.
	 * @param expertFk The ExpertFk.
	 * @param speciesExpertFk The SpeciesExpertFk.
	 * @return Whether save was successful or not.
	 */
	private boolean invokeRankDataAndTypeNameFkAndKingdomFk(TaxonName taxonName,
			Integer taxonFk, Integer typeNameFk, Integer kingdomFk, Integer rankFk, PesiExportState state) {

	    try {
			int index = 1;
			if (rankFk != null) {
				rankTypeExpertsUpdateStmt.setInt(index++, rankFk);
			} else {
				rankTypeExpertsUpdateStmt.setObject(index++, null);
			}

			String rankCache = getRankCache(taxonName, kingdomFk, state);
			if (rankCache != null) {
				rankTypeExpertsUpdateStmt.setString(index++, rankCache);
			} else {
				rankTypeExpertsUpdateStmt.setObject(index++, null);
			}

			if (typeNameFk != null) {
				rankTypeExpertsUpdateStmt.setInt(index++, typeNameFk);
			} else {
				rankTypeExpertsUpdateStmt.setObject(index++, null);
			}

			if (kingdomFk != null) {
				rankTypeExpertsUpdateStmt.setInt(index++, kingdomFk);
			} else {
				rankTypeExpertsUpdateStmt.setObject(index++, null);
			}

//			if (expertFk != null) {
//				rankTypeExpertsUpdateStmt.setInt(5, expertFk);
//			} else {
//				rankTypeExpertsUpdateStmt.setObject(5, null);
//			}
//
//			//TODO handle experts GUIDS
//			if (speciesExpertFk != null) {
//				rankTypeExpertsUpdateStmt.setInt(6, speciesExpertFk);
//			} else {
//				rankTypeExpertsUpdateStmt.setObject(6, null);
//			}
//
			if (taxonFk != null) {
				rankTypeExpertsUpdateStmt.setInt(index++, taxonFk);
			} else {
				rankTypeExpertsUpdateStmt.setObject(index++, null);
			}

			rankTypeExpertsUpdateStmt.executeUpdate();
			return true;
		} catch (SQLException e) {
		    String name = taxonName == null? null:taxonName.getTitleCache();
			logger.error("Data could not be inserted into database: " + e.getMessage() + "; rankFk = " + rankFk + "; kingdomFk = " + kingdomFk  + "; taxonFk = "+ taxonFk  + "; typeNameFk = "  + typeNameFk + "; name = " + name);
			e.printStackTrace();
			return false;
		} catch (Exception e) {
		    String name = taxonName == null? null:taxonName.getTitleCache();
            logger.error("Some exception occurred: " + e.getMessage() + "; rankFk = " + rankFk + "; kingdomFk = " + kingdomFk  + "; taxonFk = "+ taxonFk + "; typeNameFk = " + typeNameFk + "; name = " + name);
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Deletes all entries of database tables related to <code>Taxon</code>.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return Whether the delete operation was successful or not.
	 */
	protected boolean doDelete(PesiExportState state) {

		Source destination =  state.getConfig().getDestination();

		String[] tables = new String[]{"AdditionalTaxonSource","CommonNameSource","CommonName",
		        "Image","NoteSource","Note","OccurrenceSource","Occurrence","RelTaxon","Taxon"};

		for(String table : tables){
		    String sql = "DELETE FROM " + table;
		    destination.update(sql);
		}

		return true;
	}

	private static Integer getRankFk(TaxonName taxonName, NomenclaturalCode nomenclaturalCode) {
	    Integer kingdomId = PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode);
	    return getRankFk(taxonName, kingdomId);
	}

	/**
	 * Returns the <code>RankFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param nomenclaturalCode The {@link NomenclaturalCode NomenclaturalCode}.
	 * @return The <code>RankFk</code> attribute.
	 * @see MethodMapper
	 */
	private static Integer getRankFk(TaxonName taxonName, Integer kingdomId) {
		Integer result = null;
		try {
			if (taxonName != null) {
				if (taxonName.getRank() == null) {
					logger.warn("Rank is null: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
				} else {
					result = PesiTransformer.rank2RankId(taxonName.getRank(), kingdomId);
				}
				if (result == null) {
					logger.warn("Rank could not be determined for PESI-Kingdom-Id " + kingdomId + " and TaxonName " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("unused")  //used by mapper
    private static String getRankCache(TaxonName taxonName, PesiExportState state) {
	    List<TaxonNode> nodes = getTaxonNodes(taxonName);
	    Integer kingdomId;
	    if (nodes == null||nodes.isEmpty()){
	        kingdomId = getKingdomFk(taxonName);
	    }else{
	        //should not happen, method exists only pure names
	        kingdomId = findKingdomIdFromTreeIndex(nodes.iterator().next().getTaxon(), state);
	    }
        return getRankCache(taxonName, kingdomId, state);
	}

	private static String getRankCache(TaxonName taxonName, Integer kingdomFk, PesiExportState state) {
	    if (Rank.DOMAIN().equals(taxonName.getRank())){
            return state.getTransformer().getCacheByRankAndKingdom(Rank.DOMAIN(), null);
        }else if (kingdomFk != null) {
            return state.getTransformer().getCacheByRankAndKingdom(taxonName.getRank(), kingdomFk);
        }else if (taxonName.getNameType() != null){
            return state.getTransformer().getCacheByRankAndKingdom(taxonName.getRank(), PesiTransformer.nomenclaturalCode2Kingdom(taxonName.getNameType()));
        }else{
			logger.warn("No kingdom ID could be defined for name " + taxonName.getUuid());
			return null;
		}
	}

    private static List<TaxonNode> getTaxonNodes(TaxonName taxonName) {
        List<TaxonNode> result = new ArrayList<>();
        for (TaxonBase<?> tb:taxonName.getTaxonBases()){
            Taxon taxon;
            //TODO handle ERMS taxon relationships
            if (tb.isInstanceOf(Taxon.class)){
                taxon = CdmBase.deproxy(tb, Taxon.class);
            }else{
                taxon = CdmBase.deproxy(tb, Synonym.class).getAcceptedTaxon();
            }
            if (isPesiTaxon(taxon)){
                for (TaxonNode node : taxon.getTaxonNodes()){
                    result.add(node);
                }
            }
        }
        return result;
    }

//    @SuppressWarnings("unused")  //used by pure name mapper and by getRankFk
    private static Integer getKingdomFk(TaxonName taxonName){
        EnumSet<PesiSource> origin = getSources(taxonName);
        if (origin.size() == 1 && origin.contains(PesiSource.EM)){
            //maybe simply replace by
            //return PesiTransformer.KINGDOM_PLANTAE;
            return PesiTransformer.nomenclaturalCode2Kingdom(taxonName.getNameType());
        }else{
            logger.warn("getKingdomFk not yet implemented for non-EuroMed pure names");
            return null;
        }
    }

    /**
     * Returns the rankFk for the taxon name based on the names nomenclatural code.
     * You may not use this method for kingdoms other then Animalia, Plantae and Bacteria.
     */
    @SuppressWarnings("unused")  //used by pure name mapper
    private static Integer getRankFk(TaxonName taxonName) {
        EnumSet<PesiSource> origin = getSources(taxonName);
        if (origin.size() == 1 && origin.contains(PesiSource.EM)){
            return getRankFk(taxonName, getKingdomFk(taxonName));
        }else{
            logger.warn("getRankFk not yet implemented for non-EuroMed pure names");
            return null;
        }
    }

	/**
	 * Returns the <code>AuthorString</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>AuthorString</code> attribute.
	 * @see MethodMapper
	 */
	//used by mapper
	protected static String getAuthorString(TaxonBase<?> taxon) {
		try {
		    // For misapplied names there are special rules
            if (isMisappliedName(taxon)){
                return getMisappliedNameAuthorship(taxon);
            }else{
                boolean isNonViralName = false;
                String authorshipCache = null;
                TaxonName taxonName = taxon.getName();
                if (taxonName != null && taxonName.isNonViral()){
                    authorshipCache = taxonName.getAuthorshipCache();
                    isNonViralName = true;
                }
                String result = authorshipCache;

                if (taxonName == null){
                    logger.warn("TaxonName does not exist for taxon: " + taxon.getUuid() + " (" + taxon.getTitleCache() + ")");
                }else if (! isNonViralName){
                    logger.warn("TaxonName is not of instance NonViralName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
                }

                if (StringUtils.isBlank(result)) {
                    return null;
                } else {
                    return result;
                }
            }
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getMisappliedNameAuthorship(TaxonBase<?> taxon){
        String result;
	    String relAppendedPhrase = taxon.getAppendedPhrase();
        Reference sec = taxon.getSec();
        String secTitle = sec != null ? sec.getTitleCache(): null;
        if(relAppendedPhrase == null && sec == null) {
            result = "auct.";
        }else if (relAppendedPhrase != null && sec == null){
            result = relAppendedPhrase;
        }else if (relAppendedPhrase == null && sec != null){
            result = "sensu " + secTitle;
        }else{  //append!=null && sec!=null
            result = relAppendedPhrase + " " + secTitle;
        }
        String authorship = taxon.getName().getAuthorshipCache();
        if (isNotBlank(authorship)){
            result += ", non " + authorship;
        }
        return result;
	}

    /**
     * Returns the <code>DisplayName</code> attribute.
     * @param taxon The {@link TaxonBase Taxon}.
     * @return The <code>DisplayName</code> attribute.
     * @see MethodMapper
     */
    //used by Mapper
    private static String getDisplayName(TaxonBase<?> taxon) {
        boolean isMisapplied = isMisappliedName(taxon);
        TaxonName taxonName = taxon.getName();
        String result = getDisplayName(taxonName, isMisapplied);
        if (isMisapplied){
            result = result + " " + getMisappliedNameAuthorship(taxon);
        }
        return result;
    }

    /**
     * Returns the <code>DisplayName</code> attribute.
     * @param taxonName The {@link TaxonNameBase TaxonName}.
     * @return The <code>DisplayName</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")  //used by Mapper
    private static String getDisplayName(TaxonName taxonName) {
        return getDisplayName(taxonName, false);
    }

	private static String getDisplayName(TaxonName taxonName, boolean useNameCache) {
		if (taxonName == null) {
			return null;
		}else{
		    taxonName = CdmBase.deproxy(taxonName);
			INonViralNameCacheStrategy cacheStrategy = getCacheStrategy(taxonName);
			HTMLTagRules tagRules = new HTMLTagRules().
					addRule(TagEnum.name, "i").
					addRule(TagEnum.nomStatus, "@status@");

			String result;
			if (useNameCache){
                result = cacheStrategy.getNameCache(taxonName, tagRules);
			}else{
			    EnumSet<PesiSource> sources = getSources(taxonName);
			    if (sources.contains(PesiSource.ERMS)){
			        result = cacheStrategy.getTitleCache(taxonName, tagRules);  //according to SQL script (also in ERMS sources are not abbreviated)
			    }else if (sources.contains(PesiSource.FE) || sources.contains(PesiSource.IF)){
			        //TODO define for FE + IF and for multiple sources
			        result = cacheStrategy.getFullTitleCache(taxonName, tagRules);
			    }else if (sources.contains(PesiSource.EM)){
			        result = cacheStrategy.getFullTitleCache(taxonName, tagRules);
			    }else{
			        logger.warn("Source not yet handled");
			        result = cacheStrategy.getTitleCache(taxonName, tagRules);
			    }
			    result = replaceTagForInfraSpecificMarkerForProtectedTitleCache(taxonName, result);
			    result = result.replaceAll("(, ?)?\\<@status@\\>.*\\</@status@\\>", "").trim();
			}
            return result;
		}
	}

	/**
	 * Returns the <code>WebShowName</code> attribute for a taxon.
	 * See {@link #getWebShowName(TaxonName)} for further explanations.
	 * @param taxon The {@link TaxonBase taxon}.
	 * @return The <code>WebShowName</code> attribute.
	 * @see #getWebShowName(TaxonName)
	 * @see #getDisplayName(TaxonBase)
	 * @see #getFullName(TaxonBase)
	 * @see MethodMapper
	*/
	@SuppressWarnings("unused")
	private static String getWebShowName(TaxonBase<?> taxon) {
	    if (isMisappliedName(taxon)){
	        //for misapplications the webshowname is the same as the displayname as they do not show the nom.ref. in displayname
	        return getDisplayName(taxon);
	    }else{
	        TaxonName taxonName = taxon.getName();
	        return getWebShowName(taxonName);
	    }
	}

	/**
	 * Returns the <code>WebShowName</code> attribute for a name. The
	 * <code>WebShowName</code> is like fullName but with
	 * tagged (<i>) name part. It is also similar to
	 * <code>DisplayName</code> but for titleCache not fullTitleCache.
	 * For misapplications it slightly differs (see {@link #getWebShowName(TaxonBase)} )
	 *
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>WebShowName</code> attribute.
	 * @see #getDisplayName(TaxonName)
	 * @see #getFullName(TaxonName)
	 * @see #getWebShowName(TaxonBase)
	 * @see MethodMapper
	 */
	private static String getWebShowName(TaxonName taxonName) {
		if (taxonName == null) {
			return null;
		}else{
		    taxonName = CdmBase.deproxy(taxonName);
            INonViralNameCacheStrategy cacheStrategy = getCacheStrategy(taxonName);

			HTMLTagRules tagRules = new HTMLTagRules().addRule(TagEnum.name, "i");
			String result = cacheStrategy.getTitleCache(taxonName, tagRules);
			result = replaceTagForInfraSpecificMarkerForProtectedTitleCache(taxonName, result);
			return result;
		}
	}

    private static String replaceTagForInfraSpecificMarkerForProtectedTitleCache(TaxonName taxonName, String result) {
        if (taxonName.isProtectedTitleCache()||taxonName.isProtectedNameCache()){
            if (!taxonName.isAutonym()){
                result = result
                        .replace(" subsp. ", "</i> subsp. <i>")
                        .replace(" var. ", "</i> var. <i>")
                        .replace(" subvar. ", "</i> subvar. <i>")
                        .replace(" f. ", "</i> f. <i>")
                        .replace(" subf. ", "</i> subf. <i>")  //does this exist?
                        ;
            }
        }
        return result;
    }

    /**
	 * Returns the <code>WebSearchName</code> attribute.
	 * @param taxonName The {@link NonViralName NonViralName}.
	 * @return The <code>WebSearchName</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getWebSearchName(TaxonName taxonName) {
		//TODO extensions?
	    TaxonNameDefaultCacheStrategy strategy = getCacheStrategy(taxonName);
		String result = strategy.getNameCache(taxonName);
		return result;
	}

    @SuppressWarnings("unused")     //used by mapper
    private static String getFullName(TaxonBase<?> taxon) {
        if (isMisappliedName(taxon)){
            String result = getCacheStrategy(taxon.getName()).getNameCache(taxon.getName());
            result = result + " " + getMisappliedNameAuthorship(taxon);
            return result;
        }else{
            return getFullName(taxon.getName());
        }
    }

	/**
	 * Returns the <code>FullName</code> attribute.
	 * @param taxonName The {@link NonViralName NonViralName}.
	 * @return The <code>FullName</code> attribute.
	 * @see MethodMapper
	 */
    //used by mapper
	private static String getFullName(TaxonName taxonName) {
		//TODO extensions?
		String result = getCacheStrategy(taxonName).getTitleCache(taxonName);
		//misapplied names are now handled differently in getFullName(TaxonBase)
//		Iterator<Taxon> taxa = taxonName.getTaxa().iterator();
//		if (taxonName.getTaxa().size() >0){
//			if (taxonName.getTaxa().size() == 1){
//				Taxon taxon = taxa.next();
//				if (isMisappliedName(taxon)){
//					result = result + " " + getAuthorString(taxon);
//				}
//			}
//		}
		return result;
	}

    @SuppressWarnings("unused")
    private static String getGUID(TaxonName taxonName) {
        UUID uuid = taxonName.getUuid();
        String result = "NameUUID:" + uuid.toString();
        return result;
    }

    static boolean isFirstAbbrevTitle = true;
	/**
	 * Returns the SourceNameCache for the AdditionalSource table
	 */
	@SuppressWarnings("unused")
	private static String getSourceNameCache(TaxonName taxonName) {
		if (taxonName != null){
			Reference nomRef = taxonName.getNomenclaturalReference();
			if (nomRef != null ){
			    if (isFirstAbbrevTitle){
			        //#5388 is definetely not the correct ticket number
			        logger.warn("Semantics of getAbbrevTitleCache has changed. Please check if output is still correct. See #5388");
			        isFirstAbbrevTitle = false;
			    }
			    return nomRef.getAbbrevTitleCache();
			}
		}
		return null;
	}

	/**
	 * Returns the nomenclatural reference which is the reference
	 * including the detail (microreference).
	 * @param taxonName The {@link TaxonName taxon name}.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getNomRefString(TaxonName taxonName) {
		INomenclaturalReference ref = taxonName.getNomenclaturalReference();
		if (ref == null){
			return null;
		}
		String result = null;
		EnumSet<PesiSource> sources = getSources(taxonName);
		if(sources.contains(PesiSource.EM)){
		    if (! ref.isProtectedAbbrevTitleCache()){
		        ref.setAbbrevTitleCache(null, false);  //to remove a false cache
		    }
		    result = ref.getNomenclaturalCitation(taxonName.getNomenclaturalMicroReference());
		}else if(sources.contains(PesiSource.FE)||sources.contains(PesiSource.IF) ){
            //TODO still need to check if correct for FE + IF
		    if (! ref.isProtectedAbbrevTitleCache()){
                ref.setAbbrevTitleCache(null, false);  //to remove a false cache
            }
            result = ref.getNomenclaturalCitation(taxonName.getNomenclaturalMicroReference());
            return result;   // according to SQL script
		}else if(sources.contains(PesiSource.ERMS)) {
            //result = null; //according to SQL script
		}else{
		    logger.warn("Source not yet supported");
		}
		return result;
	}

	/**
	 * Returns the <code>NameStatusFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>NameStatusFk</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static Integer getNameStatusFk(TaxonName taxonName) {
		Integer result = null;

		NomenclaturalStatus status = getNameStatus(taxonName);
		if (status != null) {
			result = PesiTransformer.nomStatus2nomStatusFk(status.getType());
		}
		return result;
	}

	/**
	 * Returns the <code>NameStatusCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>NameStatusCache</code> attribute.
	 * @throws UndefinedTransformerMethodException
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getNameStatusCache(TaxonName taxonName, PesiExportState state) throws UndefinedTransformerMethodException {
		String result = null;
		NomenclaturalStatus status = getNameStatus(taxonName);
		if (status != null) {
			result = state.getTransformer().getCacheByNomStatus(status.getType());
		}
		return result;
	}

	private static NomenclaturalStatus getNameStatus(TaxonName taxonName) {
		try {
			if (taxonName != null) {
			    Set<NomenclaturalStatus> states = taxonName.getStatus();
				if (states.size() >= 1) {
				    if (states.size() > 1) {
				        String statusStr = null;
				        for (NomenclaturalStatus status: states){
				            statusStr = CdmUtils.concat(",", statusStr, status.getType()== null? null:status.getType().getTitleCache());
				        }
				        //a known case is ad43508a-8a10-480a-8519-2a76de2c0a0f (Labiatae Juss.) from E+M
	                    logger.warn("This TaxonName has more than one nomenclatural status. This may happen in very rare cases but is not handled by the PESI data warehouse. Taxon name: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")Status:" + statusStr);
	                }
				    NomenclaturalStatus status = states.iterator().next();
					return status;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the <code>TaxonStatusFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>TaxonStatusFk</code> attribute.
	 * @see MethodMapper
	 */
	private static Integer getTaxonStatusFk(TaxonBase<?> taxon, PesiExportState state) {
		try {
			return PesiTransformer.taxonBase2statusFk(taxon);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the <code>TaxonStatusCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>TaxonStatusCache</code> attribute.
	 * @throws UndefinedTransformerMethodException
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getTaxonStatusCache(TaxonBase<?> taxon, PesiExportState state) throws UndefinedTransformerMethodException {
		return state.getTransformer().getTaxonStatusCacheByKey(getTaxonStatusFk(taxon, state));
	}

    /**
     * Returns the <code>TaxonFk1</code> attribute. It corresponds to a CDM <code>TaxonRelationship</code>.
     * @param relationship The {@link RelationshipBase Relationship}.
     * @param state The {@link PesiExportState PesiExportState}.
     * @return The <code>TaxonFk1</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getSynonym(Synonym synonym, PesiExportState state) {
         return state.getDbId(synonym);
    }

	/**
	 * Returns the <code>TypeNameFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>TypeNameFk</code> attribute.
	 * @see MethodMapper
	 */
	private static Integer getTypeNameFk(TaxonName taxonName, PesiExportState state) {
		Integer result = null;
		if (taxonName != null) {
			Set<NameTypeDesignation> nameTypeDesignations = taxonName.getNameTypeDesignations();
			if (nameTypeDesignations.size() == 1) {
				NameTypeDesignation nameTypeDesignation = nameTypeDesignations.iterator().next();
				if (nameTypeDesignation != null) {
					TaxonName typeName = nameTypeDesignation.getTypeName();
					if (typeName != null) {
					    if (typeName.getTaxonBases().isEmpty()){
					        logger.warn("Type name does not belong to a taxon and therefore is not expected to be a European taxon. Type name not added. Type name: " + typeName.getTitleCache() + ", typified name: " + taxonName.getTitleCache());
					    }else{
					        result = state.getDbId(typeName);
					    }
					}
				}
			} else if (nameTypeDesignations.size() > 1) {
				logger.warn("This TaxonName has " + nameTypeDesignations.size() + " NameTypeDesignations: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
			}
		}
		return result;
	}

	/**
	 * Returns the <code>TypeFullnameCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>TypeFullnameCache</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getTypeFullnameCache(TaxonName taxonName) {
		String result = null;

		try {
    		if (taxonName != null) {
    			Set<NameTypeDesignation> nameTypeDesignations = taxonName.getNameTypeDesignations();
    			if (nameTypeDesignations.size() == 1) {
    				NameTypeDesignation nameTypeDesignation = nameTypeDesignations.iterator().next();
    				if (nameTypeDesignation != null) {
    					TaxonName typeName = nameTypeDesignation.getTypeName();
    					if (typeName != null) {
    						result = typeName.getTitleCache();
    					}
    				}
    			} else if (nameTypeDesignations.size() > 1) {
    				logger.warn("This TaxonName has " + nameTypeDesignations.size() + " NameTypeDesignations: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
    			}
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * Returns the <code>QualityStatusFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>QualityStatusFk</code> attribute.
	 * @see MethodMapper
	 */
	private static Integer getQualityStatusFk(TaxonName taxonName) {
	    EnumSet<PesiSource> sources = getSources(taxonName);
		return PesiTransformer.getQualityStatusKeyBySource(sources, taxonName);
	}

	/**
	 * Returns the <code>QualityStatusCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>QualityStatusCache</code> attribute.
	 * @throws UndefinedTransformerMethodException
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getQualityStatusCache(TaxonName taxonName, PesiExportState state) throws UndefinedTransformerMethodException {
		return state.getTransformer().getQualityStatusCacheByKey(getQualityStatusFk(taxonName));
	}


	/**
	 * Returns the <code>TypeDesignationStatusFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>TypeDesignationStatusFk</code> attribute.
	 * @see MethodMapper
	 */
	//TODO seems not to be used
	private static Integer getTypeDesignationStatusFk(TaxonName taxonName) {
		Integer result = null;

		try {
    		if (taxonName != null) {
    			Set<NameTypeDesignation> typeDesignations = taxonName.getNameTypeDesignations();
    			if (typeDesignations.size() == 1) {
    				Object obj = typeDesignations.iterator().next().getTypeStatus();
    				NameTypeDesignationStatus designationStatus = CdmBase.deproxy(obj, NameTypeDesignationStatus.class);
    				result = PesiTransformer.nameTypeDesignationStatus2TypeDesignationStatusId(designationStatus);
    			} else if (typeDesignations.size() > 1) {
    				logger.error("Found a TaxonName with more than one NameTypeDesignation: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
    			}
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the <code>TypeDesignationStatusCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>TypeDesignationStatusCache</code> attribute.
	 * @see MethodMapper
	 */
	//TODO seems not to be used
	private static String getTypeDesignationStatusCache(TaxonName taxonName) {
		String result = null;

		try {
    		if (taxonName != null) {
    			Set<NameTypeDesignation> typeDesignations = taxonName.getNameTypeDesignations();
    			if (typeDesignations.size() == 1) {
    				Object obj = typeDesignations.iterator().next().getTypeStatus();
    				NameTypeDesignationStatus designationStatus = CdmBase.deproxy(obj, NameTypeDesignationStatus.class);
    				result = PesiTransformer.nameTypeDesignationStatus2TypeDesignationStatusCache(designationStatus);
    			} else if (typeDesignations.size() > 1) {
    				logger.error("Found a TaxonName with more than one NameTypeDesignation: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
    			}
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the <code>FossilStatusFk</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>FossilStatusFk</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static Integer getFossilStatusFk(IdentifiableEntity<?> identEntity, PesiExportState state) {
		Integer result = null;

		Set<String> fossilStatuus = identEntity.getExtensions(ErmsTransformer.uuidExtFossilStatus);
		if (fossilStatuus.size() == 0){
			return null;
		}else if (fossilStatuus.size() > 1){
			logger.warn("More than 1 fossil status given for " +  identEntity.getTitleCache() + " " + identEntity.getUuid());
		}
		String fossilStatus = fossilStatuus.iterator().next();

		int statusFk = state.getTransformer().fossilStatusCache2FossilStatusFk(fossilStatus);
		return statusFk;
	}

	/**
	 * Returns the <code>FossilStatusCache</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>FossilStatusCache</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getFossilStatusCache(IdentifiableEntity<?> identEntity, PesiExportState state) {
		String result = null;
		Set<String> fossilStatuus = identEntity.getExtensions(ErmsTransformer.uuidExtFossilStatus);
		if (fossilStatuus.size() == 0){
			return null;
		}
		for (String strFossilStatus : fossilStatuus){
			result = CdmUtils.concat(";", result, strFossilStatus);
		}
		return result;
	}

	/**
     * Returns the <code>sourceFk</code> attribute which is
     * a link to a reference.
     * @see #8796
     * @return The <code>sourceFk</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")  //used by methodmapper
    private static Integer getSourceFk(TaxonBase<?> taxonBase, PesiExportState state) {
        if (taxonBase.getSec() != null){
            return state.getDbId(taxonBase.getSec());
        }else{
            Set<IdentifiableSource> sources = getPesiSources(taxonBase);
            for (IdentifiableSource source : sources){
                Reference ref = source.getCitation();
                if (ref != null){
                    return state.getDbId(ref);
                }
            }
        }
        logger.warn("No source found for " + taxonBase.getTitleCache());
        return null;
    }

    /**
     * Returns the <code>sourceFk</code> attribute which is
     * a link to a reference.
     *
     * @return The <code>sourceFk</code> attribute.
     * @see MethodMapper
     * @see #8796
     */
    @SuppressWarnings("unused")  //used by methodmapper
    private static Integer getSourceFk(TaxonName taxonName, PesiExportState state) {
        //for now pure names (only coming from E+M) have no source
        //according to SQL scripts (#8796)
        return null;
    }

	/**
	 * Returns the <code>IdInSource</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>IdInSource</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getIdInSource(IdentifiableEntity<?> taxonName) {
		String result = null;

		try {
			Set<IdentifiableSource> sources = getPesiSources(taxonName);
			if (sources.size() > 1){
				logger.warn("There is > 1 Pesi source. This is not yet handled.");
			}
			if (sources.size() == 0){
				logger.warn("There is no Pesi source!" +taxonName.getUuid() + " (" + taxonName.getTitleCache() +")");
			}
			for (IdentifiableSource source : sources) {
				Reference ref = source.getCitation();
				UUID refUuid = ref.getUuid();
				String idInSource = source.getIdInSource();
				if (refUuid.equals(PesiTransformer.uuidSourceRefEuroMed)){
					result = idInSource != null ? ("NameId: " + source.getIdInSource()) : null;
				}else if (refUuid.equals(PesiTransformer.uuidSourceRefFaunaEuropaea)){
					result = idInSource != null ? ("TAX_ID: " + source.getIdInSource()) : null;
				}else if (refUuid.equals(PesiTransformer.uuidSourceRefErms)){
					result = idInSource != null ? ("tu_id: " + source.getIdInSource()) : null;
				}else if (refUuid.equals(PesiTransformer.uuidSourceRefIndexFungorum)){  //Index Fungorum
					result = idInSource != null ? ("if_id: " + source.getIdInSource()) : null;
				}else{
					if (logger.isDebugEnabled()){logger.debug("Not a PESI source");}
				}

				String sourceIdNameSpace = source.getIdNamespace();
				if (sourceIdNameSpace != null) {
					if (sourceIdNameSpace.equals(PesiTransformer.STR_NAMESPACE_NOMINAL_TAXON)) {
						result =  idInSource != null ? ("Nominal Taxon from TAX_ID: " + source.getIdInSource()):null;
					} else if (sourceIdNameSpace.equals(TaxonServiceImpl.INFERRED_EPITHET_NAMESPACE)) {
						result =  idInSource != null ? ("Inferred epithet from TAX_ID: " + source.getIdInSource()) : null;
					} else if (sourceIdNameSpace.equals(TaxonServiceImpl.INFERRED_GENUS_NAMESPACE)) {
						result =  idInSource != null ? ("Inferred genus from TAX_ID: " + source.getIdInSource()):null;
					} else if (sourceIdNameSpace.equals(TaxonServiceImpl.POTENTIAL_COMBINATION_NAMESPACE)) {
						result =  idInSource != null ? ("Potential combination from TAX_ID: " + source.getIdInSource()):null;
					}
				}
				if (result == null) {
					logger.warn("IdInSource is NULL for this taxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() +", sourceIdNameSpace: " + source.getIdNamespace()+")");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("An error occurs while creating idInSource..." + taxonName.getUuid() + " (" + taxonName.getTitleCache()+ e.getMessage());
		}

		if (result == null) {
			logger.warn("IdInSource is NULL for this taxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() +")");
		}
		return result;
	}

	/**
	 * Returns the idInSource for a given TaxonName only.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The idInSource.
	 */
	private static String getIdInSourceOnly(IdentifiableEntity<?> identEntity) {
		String result = null;

		// Get the sources first
		Set<IdentifiableSource> sources = getPesiSources(identEntity);

		// Determine the idInSource
		if (sources.size() == 1) {
			IdentifiableSource source = sources.iterator().next();
			if (source != null) {
				result = source.getIdInSource();
			}
		} else if (sources.size() > 1) {
			int count = 1;
			result = "";
			for (IdentifiableSource source : sources) {
				result += source.getIdInSource();
				if (count < sources.size()) {
					result += "; ";
				}
				count++;
			}

		}

		return result;
	}

	/**
	 * Returns the <code>GUID</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>GUID</code> attribute.
	 * @see MethodMapper
	 */
	private static String getGUID(TaxonBase<?> taxon) {
		if (taxon.getLsid() != null ){
			return taxon.getLsid().getLsid();
		}else if (taxon.hasMarker(PesiTransformer.uuidMarkerGuidIsMissing, true)){
			return null;
		}else{
			return taxon.getUuid().toString();
		}
	}

	/**
	 * Returns the <code>DerivedFromGuid</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>DerivedFromGuid</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getDerivedFromGuid(TaxonBase<?> taxon) {
		String result = null;
		try {
		// The same as GUID for now
		result = getGUID(taxon);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns the <code>CacheCitation</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The CacheCitation.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getCacheCitation(TaxonBase<?> taxon) {
		// !!! See also doPhaseUpdates

		TaxonName taxonName = taxon.getName();
		String result = "";
		//TODO implement anew for taxa
		try {
			EnumSet<PesiSource> sources = getSources(taxon);
			//TODO what if 2 sources? In PESI 2014 they were pipe separated
			//TODO why does ERMS use accessed through eu-nomen, while E+M uses accessed through E+M
			if (sources.isEmpty()) {
//				logger.error("OriginalDB is NULL for this TaxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
			} else if (sources.contains(PesiSource.ERMS)) {
				//TODO check if correct, compare with PESI 2014
			    Set<Extension> extensions = taxon.getExtensions();
				for (Extension extension : extensions) {
					if (extension.getType().equals(cacheCitationExtensionType)) {
						result = extension.getValue();
					}
				}
			} else if (sources.contains(PesiSource.EM)) {
			    //TODO
			    boolean isMisapplied = isMisappliedName(taxon);
			    boolean isProParteSyn = isProParteOrPartialSynonym(taxon);
			    Reference sec = null;
			    if(!isMisapplied && !isProParteSyn){
			        sec = taxon.getSec();
			    }else if (isMisapplied){
			        sec = getAcceptedTaxonForMisappliedName(taxon).getSec();
			    }else if (isProParteSyn){
                    sec = getAcceptedTaxonForProParteSynonym(taxon).getSec();
                }
			    if (sec == null){
			        logger.warn("Sec could not be defined for taxon " + taxon.getTitleCache()+ "; " + taxon.getUuid());
			    }
			    String author = sec == null? "" : sec.getTitleCache();
			    String webShowName = isMisapplied? getDisplayName(taxon):getWebShowName(taxonName);  //for misapplied we need also the sensu and non author part, for ordinary names name + author is enough
			    String accessed = ". Accessed through: Euro+Med PlantBase at https://www.europlusmed.org/cdm_dataportal/taxon/";
			    result = CdmUtils.removeTrailingDot(author)
			            + ". " + CdmUtils.removeTrailingDot(webShowName)
			            + accessed + taxon.getUuid();
			} else {
				//TODO check for IF + FE

			    String expertName = getExpertName(taxon);
				String webShowName = getWebShowName(taxonName);

				// idInSource only
				String idInSource = getIdInSourceOnly(taxonName);

				// build the cacheCitation
				if (expertName != null) {
					result += expertName + ". ";
				} else {
					if (logger.isDebugEnabled()){logger.debug("ExpertName could not be determined for this TaxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");}
				}
				if (webShowName != null) {
					result += webShowName + ". ";
				} else {
					logger.warn("WebShowName could not be determined for this TaxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
				}

				if (getOriginalDB(taxonName).equals("FaEu")) {
					result += "Accessed through: Fauna Europaea at http://faunaeur.org/full_results.php?id=";
				} else if (getOriginalDB(taxonName).equals("EM")) {
					result += "Accessed through: Euro+Med PlantBase at http://ww2.bgbm.org/euroPlusMed/PTaxonDetail.asp?UUID=";
				}

				if (idInSource != null) {
					result += idInSource;
				} else {
					logger.warn("IdInSource could not be determined for this TaxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (StringUtils.isBlank(result)) {
			return null;
		} else {
			return result;
		}
	}

	/**
	 * Returns the <code>OriginalDB</code> attribute.
	 * @param identifiableEntity
	 * @return The <code>OriginalDB</code> attribute.
	 * @see MethodMapper
	 */
//	@SuppressWarnings("unused")
	private static String getOriginalDB(IdentifiableEntity<?> identifiableEntity) {
		EnumSet<PesiSource> sources  = getSources(identifiableEntity);
		return PesiTransformer.getOriginalDbBySources(sources);
	}

	/**
	 * Returns the <code>ExpertName</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>ExpertName</code> attribute.
	 * @see MethodMapper
	 */
	//@SuppressWarnings("unused")  //for some reason it is also called by getCacheCitation
	private static String getExpertName(TaxonBase<?> taxon) {
		try {
		    String result = null;
    		if(expertNameExtensionType!=null){  //some databases do not have this extension type
    		    Set<Extension> extensions = taxon.getExtensions();
    		    for (Extension extension : extensions) {
    		        if (extension.getType().equals(expertNameExtensionType)) {
    		            result = extension.getValue();
    		        }
    		    }
    		}
    		if (getPesiSources(taxon).contains(PesiSource.EM)){
                return taxon.getSec().getTitleCache();
            }
            return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//TODO change to ExpertGUID
	private static Integer getExpertFk(Reference reference, PesiExportState state) {
		Integer result = state.getDbId(reference);
		return result;
	}

	/**
	 * Returns the <code>SpeciesExpertName</code> attribute.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @return The <code>SpeciesExpertName</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getSpeciesExpertName(TaxonBase<?> taxon) {
		try {
    		Set<Extension> extensions = taxon.getExtensions();
    		if(speciesExpertNameExtensionType != null){  //some databases do not have this extension type
                for (Extension extension : extensions) {
        			if (extension.getType().equals(speciesExpertNameExtensionType)) {
        				return extension.getValue();
        			}
        		}
    		}
    		if (getPesiSources(taxon).contains(PesiSource.EM)){
    		    return taxon.getSec().getTitleCache();
            }
    		return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the <code>SpeciesExpertFk</code> attribute.
	 * @param reference The {@link Reference Reference}.
	 * @param state The {@link PesiExportState PesiExportState}.
	 * @return The <code>SpeciesExpertFk</code> attribute.
	 * @see MethodMapper
	 */
	//TODO should be changed to SpeciesExpertGUID
	private static Integer getSpeciesExpertFk(Reference reference, PesiExportState state) {
		Integer result = state.getDbId(reference);
		return result;
	}

	protected static TaxonNameDefaultCacheStrategy getCacheStrategy(TaxonName taxonName) {
		taxonName = CdmBase.deproxy(taxonName);
		TaxonNameDefaultCacheStrategy cacheStrategy;
		if (taxonName.isZoological()){
			cacheStrategy = zooNameStrategy;
		}else if (taxonName.isBotanical()) {
			cacheStrategy = nonViralNameStrategy;
		}else if (taxonName.isNonViral()) {
			cacheStrategy = nonViralNameStrategy;
		}else if (taxonName.isBacterial()) {
			cacheStrategy = nonViralNameStrategy;
		}else{
			logger.error("Unhandled taxon name type. Can't define strategy class");
			cacheStrategy = nonViralNameStrategy;
		}
		return cacheStrategy;
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

    //TODO still in use?
    private static String getSynonymTypeCache(Synonym synonym, PesiExportState state) {
        String result = null;
        NomenclaturalCode code = null;
        code = CdmBase.deproxy(synonym, Synonym.class).getAcceptedTaxon().getName().getNameType();

        if (code != null) {
            result = state.getConfig().getTransformer().getCacheBySynonymType(synonym, code);
        } else {
            logger.error("NomenclaturalCode is NULL while creating the following synonym: " + synonym.getUuid());
        }
        return result;
    }

// ********************************** MAPPINGS ********************************/

	/**
	 * Returns the CDM to PESI specific export mappings.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbTableName);

		mapping.addMapper(IdMapper.NewInstance("TaxonId"));
		mapping.addMapper(MethodMapper.NewInstance("SourceFk", this.getClass(), "getSourceFk", standardMethodParameter, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("TaxonStatusFk", this.getClass(), "getTaxonStatusFk", standardMethodParameter, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("TaxonStatusCache", this.getClass(), "getTaxonStatusCache", standardMethodParameter, PesiExportState.class));

		mapping.addMapper(MethodMapper.NewInstance("GUID", this));

		mapping.addMapper(MethodMapper.NewInstance("DerivedFromGuid", this));
		mapping.addMapper(MethodMapper.NewInstance("CacheCitation", this));
		mapping.addMapper(MethodMapper.NewInstance("AuthorString", this));  //For Taxon because misapplied names are handled differently
		mapping.addMapper(MethodMapper.NewInstance("FullName", this));    //For Taxon because misapplied names are handled differently
		mapping.addMapper(MethodMapper.NewInstance("WebShowName", this));

		// DisplayName
		mapping.addMapper(MethodMapper.NewInstance("DisplayName", this));

		// FossilStatus (Fk, Cache)
		mapping.addMapper(MethodMapper.NewInstance("FossilStatusCache", this, IdentifiableEntity.class, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("FossilStatusFk", this, IdentifiableEntity.class, PesiExportState.class)); // PesiTransformer.FossilStatusCache2FossilStatusFk?

		//handled by name mapping
		mapping.addMapper(DbLastActionMapper.NewInstance("LastActionDate", false));
		mapping.addMapper(DbLastActionMapper.NewInstance("LastAction", true));

		//experts
//		mapping.addMapper(DbExtensionMapper.NewInstance(extensionTypeSpeciesExpertName, "SpeciesExpertName"));
		mapping.addMapper(MethodMapper.NewInstance("SpeciesExpertName", this, TaxonBase.class));
//		ExtensionType extensionTypeExpertName = (ExtensionType)getTermService().find(PesiTransformer.uuidExtExpertName);
//		mapping.addMapper(DbExtensionMapper.NewInstance(extensionTypeExpertName, "ExpertName"));
		mapping.addMapper(MethodMapper.NewInstance("ExpertName", this, TaxonBase.class));

		//ParentTaxonFk handled in Phase02 now
		mapping.addMapper(ObjectChangeMapper.NewInstance(TaxonBase.class, TaxonName.class, "Name"));

		addNameMappers(mapping);

		return mapping;
	}

	/**
	 * Returns the CDM to PESI specific export mappings.
	 * @param state
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 * @throws UndefinedTransformerMethodException
	 */
	private PesiExportMapping getPureNameMapping(PesiExportState state) throws UndefinedTransformerMethodException {
		PesiExportMapping mapping = new PesiExportMapping(dbTableName);

		mapping.addMapper(IdMapper.NewInstance("TaxonId"));

		mapping.addMapper(MethodMapper.NewInstance("SourceFk", this, TaxonName.class, PesiExportState.class));  //for now is only null
        mapping.addMapper(MethodMapper.NewInstance("KingdomFk", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("RankFk", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("RankCache", this, TaxonName.class, PesiExportState.class));
		mapping.addMapper(DbConstantMapper.NewInstance("TaxonStatusFk", Types.INTEGER, PesiTransformer.T_STATUS_UNACCEPTED));
		mapping.addMapper(DbConstantMapper.NewInstance("TaxonStatusCache", Types.VARCHAR, state.getTransformer().getTaxonStatusCacheByKey( PesiTransformer.T_STATUS_UNACCEPTED)));
		mapping.addMapper(DbStringMapper.NewInstance("AuthorshipCache", "AuthorString").setBlankToNull(true));
		mapping.addMapper(MethodMapper.NewInstance("FullName", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("WebShowName", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("GUID", this, TaxonName.class));

		// DisplayName
		mapping.addMapper(MethodMapper.NewInstance("DisplayName", this, TaxonName.class));

		mapping.addMapper(DbLastActionMapper.NewInstance("LastActionDate", false));
		mapping.addMapper(DbLastActionMapper.NewInstance("LastAction", true));

		addNameMappers(mapping);
		return mapping;
	}

	private void addNameMappers(PesiExportMapping mapping) {

	    //epithets
		mapping.addMapper(DbStringMapper.NewInstance("GenusOrUninomial", "GenusOrUninomial"));
		mapping.addMapper(DbStringMapper.NewInstance("InfraGenericEpithet", "InfraGenericEpithet"));
		mapping.addMapper(DbStringMapper.NewInstance("SpecificEpithet", "SpecificEpithet"));
		mapping.addMapper(DbStringMapper.NewInstance("InfraSpecificEpithet", "InfraSpecificEpithet"));

		//full name
//		mapping.addMapper(DbStringMapper.NewInstance("NameCache", "WebSearchName"));  //does not work as we need other cache strategy
		mapping.addMapper(MethodMapper.NewInstance("WebSearchName", this, TaxonName.class));

		//nom ref
		mapping.addMapper(MethodMapper.NewInstance("NomRefString", this, TaxonName.class));

		//status
		mapping.addMapper(MethodMapper.NewInstance("NameStatusFk", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("NameStatusCache", this, TaxonName.class, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("QualityStatusFk", this, TaxonName.class));
		mapping.addMapper(MethodMapper.NewInstance("QualityStatusCache", this, TaxonName.class, PesiExportState.class));

		//types
		mapping.addMapper(MethodMapper.NewInstance("TypeFullnameCache", this, TaxonName.class));
		//TypeNameFk handled in Phase3

		//supplemental
		mapping.addMapper(MethodMapper.NewInstance("IdInSource", this, IdentifiableEntity.class));
		mapping.addMapper(MethodMapper.NewInstance("OriginalDB", this, IdentifiableEntity.class) );

		//mapping.addMapper(ExpertsAndLastActionMapper.NewInstance());

	}

	private PesiExportMapping getSynRelMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbTableNameSynRel);
		logger.warn("SynRelMapping currently not implemented. Needs to be checked");

		mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getSynonym", Synonym.class, PesiExportState.class));
		mapping.addMapper(DbObjectMapper.NewInstance("acceptedTaxon", "TaxonFk2"));
		mapping.addMapper(DbObjectMapper.NewInstance("type", "RelTaxonQualifierFk"));
		mapping.addMapper(MethodMapper.NewInstance("RelQualifierCache", this.getClass(), "getSynonymTypeCache", Synonym.class, PesiExportState.class));
		// TODO
//		mapping.addMapper(MethodMapper.NewInstance("Notes", this,  RelationshipBase.class));

		return mapping;
	}

	private PesiExportMapping getAdditionalSourceMapping(PesiExportState state) {
		PesiExportMapping mapping = new PesiExportMapping(dbTableAdditionalSourceRel);

		mapping.addMapper(IdMapper.NewInstance("TaxonFk"));
		mapping.addMapper(ObjectChangeMapper.NewInstance(TaxonBase.class, TaxonName.class, "Name"));

		mapping.addMapper(DbObjectMapper.NewInstance("NomenclaturalReference", "SourceFk"));
//		mapping.addMapper(DbObjectMapper.NewInstance("NomenclaturalReference", "SourceNameCache", IS_CACHE));
		mapping.addMapper(MethodMapper.NewInstance("SourceNameCache", this, TaxonName.class));

		//we have only nomenclatural references here
		mapping.addMapper(DbConstantMapper.NewInstance("SourceUseFk", Types.INTEGER , PesiTransformer.NOMENCLATURAL_REFERENCE));
		mapping.addMapper(DbConstantMapper.NewInstance("SourceUseCache", Types.VARCHAR, state.getTransformer().getSourceUseCacheByKey( PesiTransformer.NOMENCLATURAL_REFERENCE)));

		mapping.addMapper(DbStringMapper.NewInstance("NomenclaturalMicroReference", "SourceDetail"));

		return mapping;
	}


    @Override
    protected boolean doCheck(PesiExportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoTaxa();
    }
}
