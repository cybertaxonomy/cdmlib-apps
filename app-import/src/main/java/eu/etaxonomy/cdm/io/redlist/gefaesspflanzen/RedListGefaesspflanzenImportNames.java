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
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportNames extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final String EX = " ex ";

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportNames.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "names";

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
        Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();
        try {
            while (rs.next()){
                makeSingleNameAndTaxon(state, rs, namesToSave, taxaToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getNameService().saveOrUpdate(namesToSave);
        getTaxonService().saveOrUpdate(taxaToSave);
        return true;
    }

    private void makeSingleNameAndTaxon(RedListGefaesspflanzenImportState state, ResultSet rs, Set<TaxonNameBase> namesToSave, Set<TaxonBase> taxaToSave)
            throws SQLException {
        long id = rs.getLong("NAMNR");
        String taxNameString = rs.getString("TAXNAME");
        String gueltString = rs.getString("GUELT");
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
            if(kombSplit.length!=2){
                logger.error("NAMNR: "+id+" Multiple ex combination authors found");
            }
            for (int i = 0; i < kombSplit.length; i++) {
                if(i==0){
                    //first author is ex author
                    TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setExCombinationAuthorship(authorKomb);
                }
                else{
                    TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setCombinationAuthorship(authorKomb);
                }
            }
        }
        else if(authorKombString.trim().equals("auct.")){
            logger.warn("NAMNR: "+id+" AUCT information in AUTOR_KOMB column");
        }
        else if(!CdmUtils.isBlank(authorKombString)){
            TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, authorKombString);
            name.setCombinationAuthorship(authorKomb);
        }
        //basionym author
        if(authorBasiString.contains(EX)){
            String[] basiSplit = authorBasiString.split(EX);
            for (int i = 0; i < basiSplit.length; i++) {
                if(basiSplit.length!=2){
                    logger.error("NAMNR: "+id+" Multiple ex basionymn authors found");
                }
                if(i==0){
                    TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, basiSplit[i]);
                    if(CdmUtils.isBlank(authorKombString)){
                        name.setExCombinationAuthorship(authorBasi);
                    }
                    else{
                        name.setExBasionymAuthorship(authorBasi);
                    }
                }
                else{
                    TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, basiSplit[i]);
                    if(CdmUtils.isBlank(authorKombString)){
                        name.setCombinationAuthorship(authorBasi);
                    }
                    else{
                        name.setBasionymAuthorship(authorBasi);
                    }
                }
            }
        }
        else if(authorBasiString.trim().equals("auct.")){
            name.setAppendedPhrase(authorBasiString);
        }
        else if(!CdmUtils.isBlank(authorBasiString)){
            //this seems to be a convention in the source database: When there is only a single author then only the "AUTOR_BASI" column is used
            TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(Namespace.AUTHOR_NAMESPACE, authorBasiString);
            if(CdmUtils.isBlank(authorKombString)){
                name.setCombinationAuthorship(authorBasi);
            }
            else{
                name.setBasionymAuthorship(authorBasi);
            }
        }

        //check authorship consistency
        String authorString = rs.getString("AUTOR");
        String authorshipCache = name.getAuthorshipCache();

        if(!CdmUtils.isBlank(zusatzString)){
            authorString = authorString.replace(", "+zusatzString, "");
        }
//        if(CdmUtils.isBlank(authorKombString) && !CdmUtils.isBlank(authorBasiString)){
//            authorString = "("+authorString+")";
//        }
        if(authorString.equals("auct.")){
            authorString = "";
        }
        if(!authorString.equals(authorshipCache)){
            logger.warn("NAMNR: "+id+" Authorship inconsistent! name.authorhshipCache <-> Column AUTOR: "+authorshipCache+" <-> "+authorString);
        }

        //id
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, Namespace.NAME_NAMESPACE);
        state.getNameMap().put(id, name.getUuid());

        namesToSave.add(name);

        //---TAXON---
        TaxonBase taxonBase = null;
        if(gueltString.equals("1") || (name.getAppendedPhrase()!=null && name.getAppendedPhrase().equals("auct."))){
            taxonBase = Taxon.NewInstance(name, null);
        }
        else if(gueltString.equals("x") || gueltString.equals("b")){
            taxonBase = Synonym.NewInstance(name, null);
        }
        if(taxonBase==null){
            logger.error("NAMNR: "+id+" Taxon for name "+name+" could not be created.");
            return;
        }

        taxaToSave.add(taxonBase);

        //id
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, Namespace.TAXON_NAMESPACE);
        state.getTaxonMap().put(id, taxonBase.getUuid());
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
        Map<String, AgentBase<?>> authorMap = new HashMap<String, AgentBase<?>>();

        try {
            while (rs.next()){
                String authorKombString = rs.getString("AUTOR_KOMB");

                if(authorKombString.contains(EX)){
                    String[] kombSplit = authorKombString.split(EX);
                    for (int i = 0; i < kombSplit.length; i++) {
                        if(!authorMap.containsKey(kombSplit[i])){
                            authorMap.put(kombSplit[i], getAgentService().load(state.getAuthorMap().get(kombSplit[i])));
                        }
                    }
                }
                else if(!CdmUtils.isBlank(authorKombString) && !authorMap.containsKey(authorKombString)){
                    authorMap.put(authorKombString, getAgentService().load(state.getAuthorMap().get(authorKombString)));
                }

                String authorBasiString = rs.getString("AUTOR_BASI");
                //basionym author
                if(authorBasiString.contains(EX)){
                    String[] basiSplit = authorBasiString.split(EX);
                    for (int i = 0; i < basiSplit.length; i++) {
                        if(!authorMap.containsKey(basiSplit[i])){
                            authorMap.put(basiSplit[i], getAgentService().load(state.getAuthorMap().get(basiSplit[i])));
                        }
                    }
                }
                else if(!CdmUtils.isBlank(authorBasiString) && !authorMap.containsKey(authorBasiString)){
                    authorMap.put(authorBasiString, getAgentService().load(state.getAuthorMap().get(authorBasiString)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        result.put(Namespace.AUTHOR_NAMESPACE, authorMap);

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
