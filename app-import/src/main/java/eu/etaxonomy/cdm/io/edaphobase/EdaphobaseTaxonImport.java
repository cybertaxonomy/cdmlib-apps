// $Id$
/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.ZoologicalName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @date 18.12.2015
 *
 */
@Component
public class EdaphobaseTaxonImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = -9138378836474086070L;
    private static final Logger logger = Logger.getLogger(EdaphobaseTaxonImport.class);

    private static final String tableName = "tax_taxon";

    private static final String pluralString = "taxa";

    private static final Object AUTHOR_NAMESPACE = "tax_author_name";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseTaxonImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return "SELECT DISTINCT taxon_id FROM tax_taxon t "
                + " ORDER BY taxon_id";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = " SELECT DISTINCT t.*, r.value as rankStr, pr.value as parentRankStr, ppr.value as grandParentRankStr, "
                    + " pt.name as parentName, ppt.name as grandParentName "
                + " FROM tax_taxon t "
                    + " LEFT JOIN tax_taxon pt ON t.parent_taxon_fk = pt.taxon_id "
                    + " LEFT JOIN tax_taxon ppt ON pt.parent_taxon_fk = ppt.taxon_id"
                    + " LEFT OUTER JOIN tax_rank_en r ON r.element_id = t.tax_rank_fk "
                    + " LEFT OUTER JOIN tax_rank_en pr ON pr.element_id = pt.tax_rank_fk "
                    + " LEFT OUTER JOIN tax_rank_en ppr ON pr.element_id = ppt.tax_rank_fk "
                + " WHERE t.taxon_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(EdaphobaseImportState state) {
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                makeSingleTaxon(state, rs, taxaToSave);

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        getTaxonService().saveOrUpdate(taxaToSave);
        return true;
    }

    /**
     * @param state
     * @param rs
     * @param taxaToSave
     * @throws SQLException
     */
    private void makeSingleTaxon(EdaphobaseImportState state, ResultSet rs, Set<TaxonBase> taxaToSave)
            throws SQLException {
        Integer id = nullSafeInt(rs, "taxon_id");
        Integer year = nullSafeInt(rs, "tax_year");
        boolean isBrackets = rs.getBoolean("tax_brackets");
        String remark = rs.getString("remark");
        String nameStr = rs.getString("name");
        String authorName = rs.getString("tax_author_name");
        //parentTaxonFk
        //rankFk
        Integer nomRefId = nullSafeInt(rs, "tax_document");
        boolean isValid = rs.getBoolean("valid");
        boolean idDeleted = rs.getBoolean("deleted");
        String displayString = rs.getString("display_string");
        Integer version = nullSafeInt(rs, "versionfield");
        String pages = rs.getString("pages");
        String treeIndex = rs.getString("path_to_root");
//      Integer rankFk = nullSafeInt(rs, "tax_rank_fk");
        String nameAddition = rs.getString("name_addition");
        String officialRemark = rs.getString("official_remark");
        boolean isGroup = rs.getBoolean("taxonomic_group");
        String rankStr = rs.getString("rankStr");
        String parentRankStr = rs.getString("parentRankStr");
        String grandParentRankStr = rs.getString("grandParentRankStr");
        String parentNameStr = rs.getString("parentName");
        String grandParentNameStr = rs.getString("grandParentName");

        TaxonBase<?> taxonBase;

        //Name etc.
        Rank rank = makeRank(state, rankStr);
        ZoologicalName name = ZoologicalName.NewInstance(rank);
        setNamePart(nameStr, rank, name);
        Rank parentRank = makeRank(state, parentRankStr);
        setNamePart(parentNameStr, parentRank, name);
        Rank parentParentRank = makeRank(state, grandParentRankStr);
        setNamePart(grandParentNameStr, parentParentRank, name);

        //Authors
        if (StringUtils.isNotBlank(authorName)){
            TeamOrPersonBase<?> author = state.getRelatedObject(AUTHOR_NAMESPACE, authorName, TeamOrPersonBase.class);
            if (author == null){
                logger.warn("Author not found in state: "  + authorName);
            }else{
                if (isBrackets){
                    name.setBasionymAuthorship(author);
                    name.setOriginalPublicationYear(year);
                }else{
                    name.setCombinationAuthorship(author);
                    name.setPublicationYear(year);
                }
            }
        }

        //nomRef
        if (nomRefId != null){
            Reference nomRef = state.getRelatedObject(REFERENCE_NAMESPACE, String.valueOf(nomRefId), Reference.class);
            if (nomRef == null){
                logger.warn("Reference " + nomRefId + " could not be found");
            }
            name.setNomenclaturalReference(nomRef);
        }
        name.setNomenclaturalMicroReference(StringUtils.isBlank(pages)? null : pages);


        Reference secRef = state.getRelatedObject(REFERENCE_NAMESPACE, state.getConfig().getSecUuid().toString(), Reference.class);
        if (secRef == null){
            secRef = makeSecRef(state);
        }
        if (isValid){
            taxonBase = Taxon.NewInstance(name, secRef);
        }else{
            taxonBase = Synonym.NewInstance(name, secRef);
        }
        taxaToSave.add(taxonBase);

        //remarks
        doNotes(taxonBase, remark);

        //id
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
    }


    /**
     * @param state
     * @return
     */
    private Reference makeSecRef(EdaphobaseImportState state) {
        Reference ref = ReferenceFactory.newDatabase();
        ref.setTitle(state.getConfig().getEdaphobaseSecundumTitle());
        ref.setUuid(state.getConfig().getSecUuid());
        state.addRelatedObject(REFERENCE_NAMESPACE, ref.getUuid().toString(), ref);
        getReferenceService().save(ref);
        return ref;
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        Map<String, TeamOrPersonBase<?>> authorMap = new HashMap<>();
        Set<String> authorSet = new HashSet<>();
        Set<String> referenceIdSet = new HashSet<String>();

        try {
            while (rs.next()){
                String authorStr = rs.getString("tax_author_name");
                authorSet.add(authorStr);
                handleForeignKey(rs, referenceIdSet, "tax_document");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Authors
        Set<UUID> uuidSet = new HashSet<>();
        for (String authorStr : authorSet){
            UUID uuid = state.getAuthorUuid(authorStr);
            uuidSet.add(uuid);
        }
        List<TeamOrPersonBase<?>> authors = (List)getAgentService().find(uuidSet);
        Map<UUID, TeamOrPersonBase<?>> authorUuidMap = new HashMap<>();
        for (TeamOrPersonBase<?> author : authors){
            authorUuidMap.put(author.getUuid(), author);
        }

        for (String authorStr : authorSet){
            UUID uuid = state.getAuthorUuid(authorStr);
            TeamOrPersonBase<?> author = authorUuidMap.get(uuid);
            authorMap.put(authorStr, author);
        }
        result.put(AUTHOR_NAMESPACE, authorMap);

        //reference map
        String nameSpace = REFERENCE_NAMESPACE;
        Class<?> cdmClass = Reference.class;
        Set<String> idSet = referenceIdSet;
        Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
        result.put(nameSpace, referenceMap);

        //secundum
        UUID secUuid = state.getConfig().getSecUuid();
        Reference secRef = getReferenceService().find(secUuid);
        referenceMap.put(secUuid.toString(), secRef);

        return result;
    }

    private void setNamePart(String nameStr, Rank rank, ZoologicalName name) {
        if (rank != null){
            if (rank.isSupraGeneric() || rank.isGenus()){
                if (StringUtils.isBlank(name.getGenusOrUninomial())){
                    name.setGenusOrUninomial(nameStr);
                }
            }else if (rank.isInfraGeneric()){
                if (StringUtils.isBlank(name.getInfraGenericEpithet())){
                    name.setInfraGenericEpithet(nameStr);
                }
            }else if (rank.isSpeciesAggregate() || rank.isSpecies()){
                if (StringUtils.isBlank(name.getSpecificEpithet())){
                    name.setSpecificEpithet(nameStr);
                }
            }else if (rank.isInfraSpecific()){
                if (StringUtils.isBlank(name.getInfraSpecificEpithet())){
                    name.setInfraSpecificEpithet(nameStr);
                }
            }
        }
    }

    private Rank makeRank(EdaphobaseImportState state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        return rank;
    }

    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoTaxa();
    }

}