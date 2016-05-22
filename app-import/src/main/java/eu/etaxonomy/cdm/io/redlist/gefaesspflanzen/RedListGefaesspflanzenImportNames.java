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
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportNames extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

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
        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String gueltString = rs.getString(RedListUtil.GUELT);
        String rangString = rs.getString(RedListUtil.RANG);
        String ep1String = rs.getString(RedListUtil.EPI1);
        String ep2String = rs.getString(RedListUtil.EPI2);
        String ep3String = rs.getString(RedListUtil.EPI3);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);
        String zusatzString = rs.getString(RedListUtil.ZUSATZ);
        String authorKombString = rs.getString(RedListUtil.AUTOR_KOMB);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
        String hybString = rs.getString(RedListUtil.HYB);

        //---NAME---
        if(CdmUtils.isBlank(taxNameString) && CdmUtils.isBlank(ep1String)){
            RedListUtil.logMessage(id, "No name found!", logger);
        }

        Rank rank = makeRank(id, state, rangString);
        BotanicalName name = BotanicalName.NewInstance(rank);

        //ep1 should always be present
        if(CdmUtils.isBlank(ep1String)){
            RedListUtil.logMessage(id, RedListUtil.EPI1+" is empty!", logger);
        }
        name.setGenusOrUninomial(ep1String);
        if(CdmUtils.isNotBlank(ep2String)){
            name.setSpecificEpithet(ep2String);
        }
        if(CdmUtils.isNotBlank(ep3String)){
            if(rank==Rank.SUBSPECIES() ||
                    rank==Rank.VARIETY()){
                name.setInfraSpecificEpithet(ep3String);
            }
        }
        //nomenclatural status
        if(CdmUtils.isNotBlank(nomZusatzString)){
            NomenclaturalStatusType status = makeNomenclaturalStatus(id, state, nomZusatzString);
            if(status!=null){
                name.addStatus(NomenclaturalStatus.NewInstance(status));
            }
        }
        //hybrid
        if(hybString.equals(RedListUtil.HYB_X)){
            name.setBinomHybrid(true);
        }
        else if(hybString.equals(RedListUtil.HYB_XF)){

        }


        //--- AUTHORS ---
        //combination author
        if(authorKombString.contains(RedListUtil.EX)){
            //TODO: what happens with multiple ex authors??
            String[] kombSplit = authorKombString.split(RedListUtil.EX);
            if(kombSplit.length!=2){
                RedListUtil.logMessage(id, "Multiple ex combination authors found", logger);
            }
            for (int i = 0; i < kombSplit.length; i++) {
                if(i==0){
                    //first author is ex author
                    TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setExCombinationAuthorship(authorKomb);
                }
                else{
                    TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setCombinationAuthorship(authorKomb);
                }
            }
        }
        else if(authorKombString.trim().contains(RedListUtil.AUCT)){
            RedListUtil.logMessage(id, "AUCT information in "+RedListUtil.AUTOR_KOMB+" column", logger);
        }
        else if(CdmUtils.isNotBlank(authorKombString)){
            TeamOrPersonBase authorKomb = (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, authorKombString);
            name.setCombinationAuthorship(authorKomb);
        }
        //basionym author
        if(authorBasiString.contains(RedListUtil.EX)){
            String[] basiSplit = authorBasiString.split(RedListUtil.EX);
            for (int i = 0; i < basiSplit.length; i++) {
                if(basiSplit.length!=2){
                    RedListUtil.logMessage(id, "Multiple ex basionymn authors found", logger);
                }
                if(i==0){
                    TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, basiSplit[i]);
                    if(CdmUtils.isBlank(authorKombString)){
                        name.setExCombinationAuthorship(authorBasi);
                    }
                    else{
                        name.setExBasionymAuthorship(authorBasi);
                    }
                }
                else{
                    TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, basiSplit[i]);
                    if(CdmUtils.isBlank(authorKombString)){
                        name.setCombinationAuthorship(authorBasi);
                    }
                    else{
                        name.setBasionymAuthorship(authorBasi);
                    }
                }
            }
        }
        else if(CdmUtils.isNotBlank(authorBasiString)){
            //this seems to be a convention in the source database: When there is only a single author then only the "AUTOR_BASI" column is used
            TeamOrPersonBase authorBasi= (TeamOrPersonBase) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, authorBasiString);
            if(CdmUtils.isBlank(authorKombString)){
                name.setCombinationAuthorship(authorBasi);
            }
            else{
                name.setBasionymAuthorship(authorBasi);
            }
        }

        //check authorship consistency
        String authorString = rs.getString(RedListUtil.AUTOR);
        String authorshipCache = name.getAuthorshipCache();

        if(CdmUtils.isNotBlank(zusatzString)){
            authorString = authorString.replace(", "+zusatzString, "");
        }
        if(CdmUtils.isNotBlank(nomZusatzString)){
            authorString = authorString.replace(", "+nomZusatzString, "");
        }
        if(CdmUtils.isNotBlank(taxZusatzString)){
            authorString = authorString.replace(", "+taxZusatzString, "");
        }
        if(authorString.equals(RedListUtil.AUCT)){
            authorString = "";
        }
        if(!authorString.equals(authorshipCache)){
            RedListUtil.logMessage(id, "Authorship inconsistent! name.authorhshipCache <-> Column "+RedListUtil.AUTOR+": "+authorshipCache+" <-> "+authorString, logger);
        }

        //id
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, RedListUtil.NAME_NAMESPACE);
        state.getNameMap().put(id, name.getUuid());

        namesToSave.add(name);

        //---TAXON---
        TaxonBase taxonBase = null;
        if(authorBasiString.trim().contains(RedListUtil.AUCT)){
            taxonBase = Taxon.NewInstance(name, null);
            taxonBase.setAppendedPhrase(RedListUtil.AUCT);
        }
        else if(gueltString.equals(RedListUtil.GUELT_ACCEPTED_TAXON)){
            taxonBase = Taxon.NewInstance(name, null);
        }
        else if(gueltString.equals(RedListUtil.GUELT_SYNONYM) || gueltString.equals(RedListUtil.GUELT_BASIONYM)){
            taxonBase = Synonym.NewInstance(name, null);
        }
        if(taxonBase==null){
            RedListUtil.logMessage(id, "Taxon for name "+name+" could not be created.", logger);
            return;
        }

        //check taxon name consistency
        if(taxNameString.endsWith("agg.")){
            taxNameString = taxNameString.replace("agg.", "aggr.");
        }
        taxNameString = taxNameString.replace("× ", "×");//hybrid sign has no space in titleCache
        String nameCache = ((BotanicalName)taxonBase.getName()).getNameCache().trim();
        if(!taxNameString.trim().equals(nameCache)){
            RedListUtil.logMessage(id, "Taxon name inconsistent! taxon.titleCache <-> Column "+RedListUtil.TAXNAME+": "+nameCache+" <-> "+taxNameString, logger);
        }


        taxaToSave.add(taxonBase);

        //id
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE);
        state.getTaxonMap().put(id, taxonBase.getUuid());

        /*check if taxon/synonym is also in checklist
         * 1. create new taxon with the same name (in the checklist classification)
         * 2. create congruent concept relationship between both
         */
        String clTaxonString = rs.getString(RedListUtil.CL_TAXON);
        if(CdmUtils.isNotBlank(clTaxonString) && !clTaxonString.trim().equals("-")){
            TaxonBase clone = (TaxonBase) taxonBase.clone();
            clone.setName(name);
            if(taxonBase.isInstanceOf(Taxon.class)){
                TaxonRelationship taxonRelation = ((Taxon) taxonBase).addTaxonRelation((Taxon) clone, TaxonRelationshipType.CONGRUENT_TO(), null, null);
                taxonRelation.setDoubtful(true);//TODO Ist das mit " mit Fragezeichen" gemeint?
            }
            ImportHelper.setOriginalSource(clone, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_CHECKLISTE_NAMESPACE);
            state.getTaxonMap().put(id, clone.getUuid());
            taxaToSave.add(clone);
        }

    }

    private Rank makeRank(long id, RedListGefaesspflanzenImportState state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        if(rank==null){
            RedListUtil.logMessage(id, rankStr+" could not be associated to a known rank.", logger);
        }
        return rank;
    }

    private NomenclaturalStatusType makeNomenclaturalStatus(long id, RedListGefaesspflanzenImportState state, String nomZusatzString) {
        NomenclaturalStatusType status = null;
        try {
            status = state.getTransformer().getNomenclaturalStatusByKey(nomZusatzString);
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        if(status==null){
            RedListUtil.logMessage(id, nomZusatzString+" could not be associated to a known nomenclatural status.", logger);
        }
        return status;
    }



    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        Map<String, AgentBase<?>> authorMap = new HashMap<String, AgentBase<?>>();

        try {
            while (rs.next()){
                String authorKombString = rs.getString(RedListUtil.AUTOR_KOMB);

                if(authorKombString.contains(RedListUtil.EX)){
                    String[] kombSplit = authorKombString.split(RedListUtil.EX);
                    for (int i = 0; i < kombSplit.length; i++) {
                        if(!authorMap.containsKey(kombSplit[i])){
                            authorMap.put(kombSplit[i], getAgentService().load(state.getAuthorMap().get(kombSplit[i])));
                        }
                    }
                }
                else if(CdmUtils.isNotBlank(authorKombString) && !authorMap.containsKey(authorKombString)){
                    authorMap.put(authorKombString, getAgentService().load(state.getAuthorMap().get(authorKombString)));
                }

                String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
                //basionym author
                if(authorBasiString.contains(RedListUtil.EX)){
                    String[] basiSplit = authorBasiString.split(RedListUtil.EX);
                    for (int i = 0; i < basiSplit.length; i++) {
                        if(!authorMap.containsKey(basiSplit[i])){
                            authorMap.put(basiSplit[i], getAgentService().load(state.getAuthorMap().get(basiSplit[i])));
                        }
                    }
                }
                else if(CdmUtils.isNotBlank(authorBasiString) && !authorMap.containsKey(authorBasiString)){
                    authorMap.put(authorBasiString, getAgentService().load(state.getAuthorMap().get(authorBasiString)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        result.put(RedListUtil.AUTHOR_NAMESPACE, authorMap);

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
