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

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportNames extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {
    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportNames.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "names";

    private static final String TAXON_NAMESPACE = "name";

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
        Set<TaxonNameBase> taxaToSave = new HashSet<TaxonNameBase>();
        try {
            while (rs.next()){
                makeSingleName(state, rs, taxaToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getNameService().saveOrUpdate(taxaToSave);
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


        BotanicalName name = BotanicalName.NewInstance(makeRank(state, rangString));

        //id
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
    }

    private Rank makeRank(RedListGefaesspflanzenImportState state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        return rank;
    }



    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
//        Map<String, TeamOrPersonBase<?>> authorMap = new HashMap<>();
//        Set<String> authorKombSet = new HashSet<>();
//        Set<String> referenceIdSet = new HashSet<String>();
//
//        try {
//            while (rs.next()){
//                String authorStr = rs.getString("tax_author_name");
//                authorKombSet.add(authorStr);
//                handleForeignKey(rs, referenceIdSet, "tax_document");
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
