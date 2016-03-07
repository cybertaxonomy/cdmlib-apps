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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportNames extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {
    /**
     *
     */
    private static final String EX = " ex ";

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportNames.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "names";

    private static final String NAME_NAMESPACE = "name";

    public RedListGefaesspflanzenImportNames() {
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
        String result = " SELECT * "
                + " FROM V_TAXATLAS_D20_EXPORT t "
                + " WHERE t.NAMNR IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Set<TaxonNameBase> namesToSave = new HashSet<TaxonNameBase>();
        try {
            while (rs.next()){
                makeSingleName(state, rs, namesToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getNameService().saveOrUpdate(namesToSave);
        return true;
    }

    private void makeSingleName(RedListGefaesspflanzenImportState state, ResultSet rs, Set<TaxonNameBase> namesToSave)
            throws SQLException {
        long id = rs.getLong("NAMNR");
        String taxNameString = rs.getString("TAXNAME");
        String rangString = rs.getString("RANG");
        String ep1String = rs.getString("EPI1");
        String ep2String = rs.getString("EPI2");
        String ep3String = rs.getString("EPI3");
        String nomZusatzString = rs.getString("NOM_ZUSATZ");
        String zusatzString = rs.getString("ZUSATZ");
        String authorKombString = rs.getString("AUTOR_KOMB");
        String authorBasiString = rs.getString("AUTOR_BASI");

        if(CdmUtils.isBlank(taxNameString) && CdmUtils.isBlank(ep1String)){
            logger.error("NAMNR: "+id+" No name found!");
        }

        Rank rank = makeRank(state, rangString);
        if(rank==null){
            logger.error("NAMNR: "+id+" Rank could not be resolved.");
            return;
        }
        BotanicalName name = BotanicalName.NewInstance(rank);

        //ep1 should always be present
        if(CdmUtils.isBlank(ep1String)){
            logger.error("NAMNR: "+id+" EPI1 is empty!");
        }
        name.setGenusOrUninomial(ep1String);
        if(!CdmUtils.isBlank(ep2String)){
            name.setSpecificEpithet(ep2String);
        }
        if(!CdmUtils.isBlank(ep3String)){
            if(rank==Rank.SUBSPECIES() ||
                    rank==Rank.VARIETY()){
                name.setInfraSpecificEpithet(ep3String);
            }
        }

        //--- AUTHORS ---
        //combination author
        if(authorKombString.contains(EX)){
            //TODO: what happens with multiple ex authors??
            String[] kombSplit = authorKombString.split(EX);
            for (int i = 0; i < kombSplit.length; i++) {
                if(i==0){
                    //first author is ex author
                    TeamOrPersonBase authorKomb = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(kombSplit[i])), TeamOrPersonBase.class);
                    name.setExCombinationAuthorship(authorKomb);
                }
                else{
                    TeamOrPersonBase authorKomb = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(kombSplit[i])), TeamOrPersonBase.class);
                    name.setCombinationAuthorship(authorKomb);
                }
            }
        }
        else if(!CdmUtils.isBlank(authorKombString)){
            TeamOrPersonBase authorKomb = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(authorKombString)), TeamOrPersonBase.class);
            name.setCombinationAuthorship(authorKomb);
        }
        //basionym author
        if(authorBasiString.contains(EX)){
            String[] basiSplit = authorBasiString.split(EX);
            for (int i = 0; i < basiSplit.length; i++) {
                if(i==0){
                    TeamOrPersonBase authorBasi = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(basiSplit[i])), TeamOrPersonBase.class);
                    name.setExBasionymAuthorship(authorBasi);
                }
                else{
                    TeamOrPersonBase authorBasi = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(basiSplit[i])), TeamOrPersonBase.class);
                    name.setBasionymAuthorship(authorBasi);
                }
            }
        }
        else if(!CdmUtils.isBlank(authorBasiString)){
            TeamOrPersonBase authorBasi = HibernateProxyHelper.deproxy(getAgentService().load(state.getAuthorMap().get(authorBasiString)), TeamOrPersonBase.class);
            name.setBasionymAuthorship(authorBasi);
        }

        //check authorship consistency
        String authorString = rs.getString("AUTOR");
        String authorshipCache = name.getAuthorshipCache();

        if(!CdmUtils.isBlank(zusatzString)){
            authorString = authorString.replace(", "+zusatzString, "");
        }
        if(CdmUtils.isBlank(authorKombString) && !CdmUtils.isBlank(authorBasiString)){
            authorString = "("+authorString+")";
        }
        if(!authorString.equals(authorshipCache)){
            logger.warn("NAMNR: "+id+" Authorship inconsistent! name.authorhshipCache <-> Column AUTOR: "+authorshipCache+" <-> "+authorString);
        }

        //id
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, NAME_NAMESPACE);
        state.getNameMap().put(id, name.getUuid());

        namesToSave.add(name);
    }

    private Rank makeRank(RedListGefaesspflanzenImportState state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        if(rank==null){
            logger.error(rankStr+" could not be associated to a known rank.");
        }
        return rank;
    }



    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
//        Map<Long, AgentBase<?>> authorKombMap = new HashMap<>();
//        Map<Long, AgentBase<?>> authorBasiMap = new HashMap<>();
//
//        //load authors
//        for(Entry<Long, UUID> entry:state.getAuthorKombMap().entrySet()){
//            authorKombMap.put(entry.getKey(), getAgentService().load(entry.getValue()));
//        }
//        for(Entry<Long, UUID> entry:state.getAuthorBasiMap().entrySet()){
//            authorBasiMap.put(entry.getKey(), getAgentService().load(entry.getValue()));
//        }
//        try {
//            while (rs.next()){
//                long id = rs.getLong("NAMNR");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        //Authors
//        Set<UUID> uuidSet = new HashSet<>();
//        for (String authorStr : authorKombSet){
//            UUID uuid = state.getAuthorUuid(authorStr);
//            uuidSet.add(uuid);
//        }
//        List<TeamOrPersonBase<?>> authors = (List)getAgentService().find(uuidSet);
//        Map<UUID, TeamOrPersonBase<?>> authorUuidMap = new HashMap<>();
//        for (TeamOrPersonBase<?> author : authors){
//            authorUuidMap.put(author.getUuid(), author);
//        }
//
//        for (String authorStr : authorKombSet){
//            UUID uuid = state.getAuthorUuid(authorStr);
//            TeamOrPersonBase<?> author = authorUuidMap.get(uuid);
//            authorMap.put(authorStr, author);
//        }
//        result.put(AUTHOR_NAMESPACE, authorMap);
//
//        //reference map
//        String nameSpace = REFERENCE_NAMESPACE;
//        Class<?> cdmClass = Reference.class;
//        Set<String> idSet = referenceIdSet;
//        Map<String, Reference<?>> referenceMap = (Map<String, Reference<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
//        result.put(nameSpace, referenceMap);
//
//        //secundum
//        UUID secUuid = state.getConfig().getSecUuid();
//        Reference<?> secRef = getReferenceService().find(secUuid);
//        referenceMap.put(secUuid.toString(), secRef);

        return result;
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
