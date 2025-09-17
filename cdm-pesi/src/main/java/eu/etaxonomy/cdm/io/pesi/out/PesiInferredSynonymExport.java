/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * Moved from {@link PesiTaxonExport}
 *
 * @author muellera
 * @since 16.06.2025
 */
@Component
public class PesiInferredSynonymExport extends PesiTaxonExportBase {

    private static final long serialVersionUID = 1614180572694170041L;
    private static Logger logger = LogManager.getLogger();

    private static int currentTaxonId;
    private static int modCount = 1000;

    private static final String parentPluralString = "Taxa";
    private static final String dbTableNameSynRel = "RelTaxon";

    private Integer kingdomFk;

    @Override
    protected void doInvoke(PesiExportState state) {
        try {
            logger.info("*** Started Making " + pluralString + " ...");

            // Stores whether this invoke was successful or not.
            boolean success = true;

            initPreparedStatements(state);

            // Get specific mappings: (CDM) Taxon -> (PESI) Taxon
            PesiExportMapping mapping = getMapping();
            PesiExportMapping synonymRelMapping = getSynRelMapping();

            // Initialize the db mapper
            mapping.initialize(state);
            synonymRelMapping.initialize(state);

            //"PHASE 5: Creating Inferred Synonyms...
            success &= doPhase(state, mapping, synonymRelMapping);

            logger.info("*** Finished making " + pluralString + " ..." + getSuccessString(success));

            if (!success){
                state.getResult().addError("An error occurred in PesiInferredSynonymExport.doInvoke. Success = false");
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            state.getResult().addException(e);
        }
    }

    private void initPreparedStatements(PesiExportState state) throws SQLException {
        initRankUpdateStatement(state);
    }

    private PesiExportMapping getSynRelMapping() {

        PesiExportMapping mapping = new PesiExportMapping(dbTableNameSynRel);

        //RelTaxonId (is an identity column and does not need mapping)
        mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getSynonymId", Synonym.class));
//        mapping.addMapper(MethodMapper.NewInstance("TaxonFk1", this.getClass(), "getSynonym", Synonym.class, PesiExportState.class));
        mapping.addMapper(DbObjectMapper.NewInstance("acceptedTaxon", "TaxonFk2"));
        mapping.addMapper(MethodMapper.NewInstance("RelTaxonQualifierFk", this, Synonym.class));
        mapping.addMapper(MethodMapper.NewInstance("RelQualifierCache", this.getClass(), "getSynonymTypeCache", Synonym.class, PesiExportState.class));
        // TODO
//      mapping.addMapper(MethodMapper.NewInstance("Notes", this,  RelationshipBase.class));

        return mapping;
    }

    /**
     * Returns the <code>RelTaxonQualifierFk</code> attribute.
     * @param relationship The {@link RelationshipBase Relationship}.
     * @return The <code>RelTaxonQualifierFk</code> attribute.
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
    private static Integer getRelTaxonQualifierFk(Synonym synonym) {
        return PesiTransformer.synonym2RelTaxonQualifierFk(synonym);
    }

    @SuppressWarnings("unused")
    private static Integer getSynonymId(Synonym synonym) {
        return synonym.getId();
    }

    /**
     * Returns the <code>RelQualifierCache</code> for synonyms
     * @param synonym the synonym to map
     * @param state the export state
     * @see MethodMapper
     */
    @SuppressWarnings("unused")
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

    //  "PHASE: Creating Inferred Synonyms..." (was PHASE 05 in TaxonExport)
    private boolean doPhase(PesiExportState state, PesiExportMapping taxonMapping, PesiExportMapping synRelMapping) {
        int count;

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

        int pageSize = limit/10;
        int pageNumber = 1;
        String inferredSynonymPluralString = "Inferred Synonyms";

        // Start transaction
        TransactionStatus txStatus = startTransaction(true);
        if (logger.isDebugEnabled()) {
            logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");
        }

        //for species
        List<Taxon> taxonList = null;
        EnumSet<NomenclaturalCode> zooNameFilter = EnumSet.of(NomenclaturalCode.ICZN);
        while ((taxonList = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*",
                Rank.SPECIES(), zooNameFilter, pageSize, pageNumber, null)).size() > 0) {

            Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

            if (logger.isDebugEnabled()) {
                logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            }

            Map<Integer, TaxonName> inferredSynonyms = createInferredSynonymsForTaxonList(state, taxonMapping,
                    synRelMapping, taxonList);
            inferredSynonymsDataToBeSaved.putAll(inferredSynonyms);

            doCount(count += taxonList.size(), modCount, inferredSynonymPluralString);
            // Commit transaction
            commitTransaction(txStatus);
            if (logger.isDebugEnabled()){logger.debug("Committed transaction.");}
            logger.info("Exported " + (taxonList.size()) + " " + inferredSynonymPluralString + ". Total: " + count);

            // Save Rank Data and KingdomFk for inferred synonyms
            for (Integer taxonFk : inferredSynonymsDataToBeSaved.keySet()) {
                TaxonName taxonName = inferredSynonymsDataToBeSaved.get(taxonFk);
                invokeRankDataAndKingdomFk(taxonName, taxonFk, kingdomFk, state);
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

        //for subspecies
        while ((taxonList = getTaxonService().listTaxaByName(Taxon.class, "*", "*", "*", "*", "*",
                Rank.SUBSPECIES(), zooNameFilter, pageSize, pageNumber, null)).size() > 0) {

            Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

            logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            Map<Integer, TaxonName> inferredSynonyms = createInferredSynonymsForTaxonList(state, taxonMapping,
                    synRelMapping, taxonList);
            inferredSynonymsDataToBeSaved.putAll(inferredSynonyms);

            doCount(count += taxonList.size(), modCount, inferredSynonymPluralString);
            // Commit transaction
            commitTransaction(txStatus);
            logger.debug("Committed transaction.");
            logger.info("Exported " + taxonList.size()+ " " + inferredSynonymPluralString + ". Total: " + count);

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
//      logger.warn("Taking snapshot at the end of phase 5 of taxonExport");
//      ProfilerController.memorySnapshot();

        // Commit transaction
        commitTransaction(txStatus);
        System.gc();
        logger.debug("Taking snapshot at the end of phase 5 after gc() of taxonExport");
        //ProfilerController.memorySnapshot();
        logger.debug("Committed transaction.");
        return success;
    }

    /**
     * Creates a map of not-persisted inferred synonyms (including all inferred types)
     * with the id being the key of the map.
     * Exports the inferred synonyms to the datawarehouse.
     */
    private Map<Integer, TaxonName> createInferredSynonymsForTaxonList(PesiExportState state,
            PesiExportMapping mapping, PesiExportMapping synRelMapping, List<Taxon> taxonList) {

        Classification classification = null;
        boolean localSuccess = true;

        Map<Integer, TaxonName> inferredSynonymsDataToBeSaved = new HashMap<>();

        for (Taxon acceptedTaxon : taxonList) {

            if (acceptedTaxon.getName().isZoological()) {  //not really needed anymore as taxonList should only include zoological names now

                kingdomFk = findKingdomIdFromTreeIndex(acceptedTaxon, state);

                classification = getClassification(classification, acceptedTaxon);

                if (classification != null) {
                    try{
//                        TaxonName name = acceptedTaxon.getName();
                        //if (name.isSpecies() || name.isInfraSpecific()){
                        List<Synonym> inferredSynonyms = getTaxonService().createAllInferredSynonyms(acceptedTaxon, classification, true);
                        //}
//                              inferredSynonyms = getTaxonService().createInferredSynonyms(classification, acceptedTaxon, SynonymType.INFERRED_GENUS_OF());

                        for (Synonym inferredSynonym : inferredSynonyms) {

                            //add hasNoGuid-marker
                            MarkerType markerType =getUuidMarkerType(PesiTransformer.uuidMarkerGuidIsMissing, state);
                            inferredSynonym.addMarker(Marker.NewInstance(markerType, true));

                            // Both Synonym and its TaxonName have no valid Id yet
                            inferredSynonym.setId(currentTaxonId++);

                            //map
                            localSuccess &= mapping.invoke(inferredSynonym);

                            //get SynonymRelationship and export
                            if (inferredSynonym.getAcceptedTaxon() == null ){
                                IdentifiableSource source = inferredSynonym.getSources().iterator().next();
                                if (source.getIdNamespace().contains("Potential combination")){
                                    acceptedTaxon.addSynonym(inferredSynonym, SynonymType.POTENTIAL_COMBINATION_OF);
                                    logger.error(inferredSynonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to potential combination");
                                } else if (source.getIdNamespace().contains("Inferred Genus")){
                                    acceptedTaxon.addSynonym(inferredSynonym, SynonymType.INFERRED_GENUS_OF);
                                    logger.error(inferredSynonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred genus");
                                } else if (source.getIdNamespace().contains("Inferred Epithet")){
                                    acceptedTaxon.addSynonym(inferredSynonym, SynonymType.INFERRED_EPITHET_OF);
                                    logger.error(inferredSynonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred epithet");
                                } else{
                                    acceptedTaxon.addSynonym(inferredSynonym, SynonymType.INFERRED_SYNONYM_OF);
                                    logger.error(inferredSynonym.getTitleCache() + " is not attached to " + acceptedTaxon.getTitleCache() + " type is set to inferred synonym");
                                }

                                localSuccess &= synRelMapping.invoke(inferredSynonym);
                                if (!localSuccess) {
                                    logger.error("Synonym relationship export failed " + inferredSynonym.getTitleCache() + " accepted taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
                                }
                            } else {
                                localSuccess &= synRelMapping.invoke(inferredSynonym);
                                if (!localSuccess) {
                                    logger.error("Synonym relationship export failed " + inferredSynonym.getTitleCache() + " accepted taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
                                } else {
                                    logger.info("Synonym relationship successfully exported: " + inferredSynonym.getTitleCache() + "  " +acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache()+")");
                                }
                            }

                            inferredSynonymsDataToBeSaved.put(inferredSynonym.getId(), inferredSynonym.getName());
                        }
                    }catch(Exception e){
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    logger.error("Classification is NULL. Inferred Synonyms could not be created for this Taxon: " + acceptedTaxon.getUuid() + " (" + acceptedTaxon.getTitleCache() + ")");
                }
            } else {
//              logger.error("TaxonName is not a ZoologicalName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
            }
        }

        return inferredSynonymsDataToBeSaved;
    }

    private Classification getClassification(Classification classification, Taxon acceptedTaxon) {
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
        return classification;
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


    @Override
    protected boolean doCheck(PesiExportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoInferredSynonyms();
    }
}