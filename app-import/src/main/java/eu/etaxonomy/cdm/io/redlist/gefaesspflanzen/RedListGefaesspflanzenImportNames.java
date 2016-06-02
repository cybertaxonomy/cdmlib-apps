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
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

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
        Set<TaxonNameBase<?,?>> namesToSave = new HashSet<TaxonNameBase<?,?>>();
        Set<TaxonBase<?>> taxaToSave = new HashSet<TaxonBase<?>>();
        try {
            while (rs.next()){
                makeSingleNameAndTaxon(state, rs, namesToSave, taxaToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getNameService().saveOrUpdate((TaxonNameBase) namesToSave);
        getTaxonService().saveOrUpdate((TaxonBase) taxaToSave);
        return true;
    }

    private void makeSingleNameAndTaxon(RedListGefaesspflanzenImportState state, ResultSet rs, Set<TaxonNameBase<?,?>> namesToSave, Set<TaxonBase<?>> taxaToSave)
            throws SQLException {
        long id = rs.getLong(RedListUtil.NAMNR);
        String clTaxonString = rs.getString(RedListUtil.CL_TAXON);
        String relationE = rs.getString(RedListUtil.E);
        String relationW = rs.getString(RedListUtil.W);
        String relationK = rs.getString(RedListUtil.K);
        String relationAW = rs.getString(RedListUtil.AW);
        String relationAO = rs.getString(RedListUtil.AO);
        String relationR = rs.getString(RedListUtil.R);
        String relationO = rs.getString(RedListUtil.O);
        String relationS = rs.getString(RedListUtil.S);

        //---NAME---
        NonViralName<?> name = importName(state, rs, namesToSave);


        //--- AUTHORS ---
        importAuthors(state, rs, name);

        //---TAXON---
        TaxonBase<?> taxonBase = importTaxon(rs, name);
        if(taxonBase==null){
            RedListUtil.logMessage(id, "Taxon for name "+name+" could not be created.", logger);
            return;
        }
        taxonBase.setSec(state.getConfig().getSourceReference());

        //---CONCEPT RELATIONSHIPS---
        /*check if taxon/synonym also exists in other classification
         * 1. create new taxon with the same name (in that classification)
         * 2. create concept relationship between both
         */
        //checklist
        if(CdmUtils.isNotBlank(clTaxonString) && !clTaxonString.trim().equals("-")){
            cloneTaxon(taxonBase, name, TaxonRelationshipType.CONGRUENT_TO(), taxaToSave, id, RedListUtil.TAXON_CHECKLISTE_NAMESPACE, false, true, state);
        }
        //E, W, K, AW, AO, R, O, S
        addConceptRelation(relationE, RedListUtil.CLASSIFICATION_NAMESPACE_E, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationW, RedListUtil.CLASSIFICATION_NAMESPACE_W, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationK, RedListUtil.CLASSIFICATION_NAMESPACE_K, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationAW, RedListUtil.CLASSIFICATION_NAMESPACE_AW, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationAO, RedListUtil.CLASSIFICATION_NAMESPACE_AO, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationR, RedListUtil.CLASSIFICATION_NAMESPACE_R, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationO, RedListUtil.CLASSIFICATION_NAMESPACE_O, taxonBase, name, taxaToSave, id, state);
        addConceptRelation(relationS, RedListUtil.CLASSIFICATION_NAMESPACE_S, taxonBase, name, taxaToSave, id, state);

        //NOTE: the source has to be added after cloning or otherwise the clone would also get the source
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE);
        taxaToSave.add(taxonBase);
    }

    private void addConceptRelation(String relationString, String classificationNamespace, TaxonBase<?> taxonBase, TaxonNameBase<?,?> name, Set<TaxonBase<?>> taxaToSave, long id, RedListGefaesspflanzenImportState state){
        if(CdmUtils.isNotBlank(relationString) && !relationString.equals(".")){
            String substring = relationString.substring(relationString.length()-1, relationString.length());
            TaxonRelationshipType taxonRelationshipTypeByKey = new RedListGefaesspflanzenTransformer().getTaxonRelationshipTypeByKey(substring);
            if(taxonRelationshipTypeByKey==null){
                RedListUtil.logMessage(id, "Could not interpret relationship "+relationString+" for taxon "+taxonBase.generateTitle(), logger);
            }
            //there is no type "included in" so we have to reverse the direction
            if(substring.equals("<")){
                cloneTaxon(taxonBase, name, taxonRelationshipTypeByKey, taxaToSave, id, classificationNamespace, true, false, state);
            }
            else{
                cloneTaxon(taxonBase, name, taxonRelationshipTypeByKey, taxaToSave, id, classificationNamespace, false, false, state);
            }
        }
    }

    /**
     * <b>NOTE:</b> the {@link TaxonRelationshipType} passed as parameter is
     * directed <b>from the clone</b> to the taxon.<br>
     * This can be changed with parameter <i>reverseRelation</i>
     */
    private void cloneTaxon(TaxonBase<?> taxonBase, TaxonNameBase<?, ?> name, TaxonRelationshipType relationFromCloneToTaxon, Set<TaxonBase<?>> taxaToSave, long id, String sourceNameSpace, boolean reverseRelation, boolean doubtful, RedListGefaesspflanzenImportState state){
        TaxonBase<?> clone = (TaxonBase<?>) taxonBase.clone();
        clone.setName(name);
        if(taxonBase.isInstanceOf(Taxon.class)){
            TaxonRelationship taxonRelation;
            if(reverseRelation){
                taxonRelation = ((Taxon) taxonBase).addTaxonRelation((Taxon) clone, relationFromCloneToTaxon, null, null);
            }
            else {
                taxonRelation = ((Taxon) clone).addTaxonRelation((Taxon) taxonBase, relationFromCloneToTaxon, null, null);
            }
            taxonRelation.setDoubtful(doubtful);
        }
        ImportHelper.setOriginalSource(clone, state.getTransactionalSourceReference(), id, sourceNameSpace);
        taxaToSave.add(clone);
    }

    private TaxonBase<?> importTaxon(ResultSet rs, NonViralName<?> name) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String gueltString = rs.getString(RedListUtil.GUELT);
        String trivialString = rs.getString(RedListUtil.TRIVIAL);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
        String hybString = rs.getString(RedListUtil.HYB);

        TaxonBase<?> taxonBase = null;
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
        else{
            return null;
        }

        //common name
        if(taxonBase.isInstanceOf(Taxon.class) && trivialString!=null){
            Taxon taxon = HibernateProxyHelper.deproxy(taxonBase, Taxon.class);
            TaxonDescription description = TaxonDescription.NewInstance(taxon);
            description.addElement(CommonTaxonName.NewInstance(trivialString, Language.getDefaultLanguage()));
        }

        //check taxon name consistency
        checkTaxonNameConsistency(id, taxNameString, hybString, taxonBase);
        return taxonBase;
    }

    private void importAuthors(RedListGefaesspflanzenImportState state, ResultSet rs, NonViralName<?> name) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);
        String zusatzString = rs.getString(RedListUtil.ZUSATZ);
        String authorKombString = rs.getString(RedListUtil.AUTOR_KOMB);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);

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
                    TeamOrPersonBase<?> authorKomb = (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setExCombinationAuthorship(authorKomb);
                }
                else{
                    TeamOrPersonBase<?> authorKomb = (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, kombSplit[i]);
                    name.setCombinationAuthorship(authorKomb);
                }
            }
        }
        else if(authorKombString.trim().contains(RedListUtil.AUCT)){
            RedListUtil.logMessage(id, "AUCT information in "+RedListUtil.AUTOR_KOMB+" column", logger);
        }
        else if(CdmUtils.isNotBlank(authorKombString)){
            TeamOrPersonBase<?> authorKomb = (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, authorKombString);
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
                    TeamOrPersonBase<?> authorBasi= (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, basiSplit[i]);
                    if(CdmUtils.isBlank(authorKombString)){
                        name.setExCombinationAuthorship(authorBasi);
                    }
                    else{
                        name.setExBasionymAuthorship(authorBasi);
                    }
                }
                else{
                    TeamOrPersonBase<?> authorBasi= (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, basiSplit[i]);
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
            TeamOrPersonBase<?> authorBasi= (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, authorBasiString);
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
        checkAuthorShipConsistency(id, nomZusatzString, taxZusatzString, zusatzString, authorString, authorshipCache);
    }

    private NonViralName<?> importName(RedListGefaesspflanzenImportState state, ResultSet rs, Set<TaxonNameBase<?,?>> namesToSave) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String rangString = rs.getString(RedListUtil.RANG);
        String ep1String = rs.getString(RedListUtil.EPI1);
        String ep2String = rs.getString(RedListUtil.EPI2);
        String ep3String = rs.getString(RedListUtil.EPI3);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String hybString = rs.getString(RedListUtil.HYB);

        if(CdmUtils.isBlank(taxNameString) && CdmUtils.isBlank(ep1String)){
            RedListUtil.logMessage(id, "No name found!", logger);
        }

        Rank rank = makeRank(id, state, rangString, ep3String!=null);
        NonViralName<?> name = BotanicalName.NewInstance(rank);

        //ep1 should always be present
        if(CdmUtils.isBlank(ep1String)){
            RedListUtil.logMessage(id, RedListUtil.EPI1+" is empty!", logger);
        }
        name.setGenusOrUninomial(ep1String);
        if(CdmUtils.isNotBlank(ep2String)){
            name.setSpecificEpithet(ep2String);
        }
        if(CdmUtils.isNotBlank(ep3String)){
            name.setInfraSpecificEpithet(ep3String);
        }
        //nomenclatural status
        if(CdmUtils.isNotBlank(nomZusatzString)){
            NomenclaturalStatusType status = makeNomenclaturalStatus(id, state, nomZusatzString);
            if(status!=null){
                name.addStatus(NomenclaturalStatus.NewInstance(status));
            }
        }
        //hybrid
        if(CdmUtils.isNotBlank(hybString)){
            if(hybString.equals(RedListUtil.HYB_X)){
                name.setBinomHybrid(true);
            }
            else if(hybString.equals(RedListUtil.HYB_G)){
                name.setMonomHybrid(true);
            }
            else if(hybString.equals(RedListUtil.HYB_XF)){
                name.setHybridFormula(true);
                if(ep1String.contains(RedListUtil.HYB_SIGN)){
                    RedListUtil.logMessage(id, "EPI1 has hybrid signs but with flag: "+RedListUtil.HYB_XF, logger);
                }
                else if(ep2String.contains(RedListUtil.HYB_SIGN)){
                    String[] split = ep2String.split(RedListUtil.HYB_SIGN);
                    if(split.length!=2){
                        RedListUtil.logMessage(id, "Multiple hybrid signs found in "+ep2String, logger);
                    }
                    String hybridFormula1 = ep1String+" "+split[0].trim();
                    String hybridFormula2 = ep1String+" "+split[1].trim();
                    if(CdmUtils.isNotBlank(ep3String)){
                        hybridFormula1 += " "+ep3String;
                        hybridFormula2 += " "+ep3String;
                    }
                    String fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
                    name = NonViralNameParserImpl.NewInstance().parseFullName(fullFormula);
                }
                else if(ep3String.contains(RedListUtil.HYB_SIGN)){
                    String[] split = ep3String.split(RedListUtil.HYB_SIGN);
                    if(split.length!=2){
                        RedListUtil.logMessage(id, "Multiple hybrid signs found in "+ep3String, logger);
                    }
                    String hybridFormula1 = ep1String+" "+ep2String+" "+split[0];
                    String hybridFormula2 = ep1String+" "+ep2String+" "+split[1];
                    String fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
                    name = NonViralNameParserImpl.NewInstance().parseFullName(fullFormula);
                }
            }
            else if(hybString.equals(RedListUtil.HYB_N)){
                name = NonViralNameParserImpl.NewInstance().parseFullName(ep1String+" "+ep2String+" nothosubsp. "+ep3String);
            }
            else if(hybString.equals(RedListUtil.HYB_GF)){
                if(ep1String.contains(RedListUtil.HYB_SIGN)){
                    name = NonViralNameParserImpl.NewInstance().parseFullName(ep1String);
                }
                else{
                    RedListUtil.logMessage(id, "HYB is "+hybString+" but "+RedListUtil.HYB+" does not contain "+RedListUtil.HYB_SIGN, logger);
                }
            }
            else{
                logger.error("HYB value "+hybString+" not yet handled");
            }
        }
        //add source
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, RedListUtil.NAME_NAMESPACE);

        namesToSave.add(name);
        return name;
    }

    private void checkAuthorShipConsistency(long id, String nomZusatzString, String taxZusatzString,
            String zusatzString, String authorString, String authorshipCache) {
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
    }

    private void checkTaxonNameConsistency(long id, String taxNameString, String hybString, TaxonBase<?> taxonBase) {
        if(hybString.equals(RedListUtil.HYB_XF)){
            if(HibernateProxyHelper.deproxy(taxonBase.getName(),NonViralName.class).getHybridChildRelations().isEmpty()){
                RedListUtil.logMessage(id, "Hybrid name but no hybrid child relations", logger);
                return;
            }
            return;
        }


        String nameCache = HibernateProxyHelper.deproxy(taxonBase.getName(), NonViralName.class).getNameCache().trim();

        if(taxNameString.endsWith("agg.")){
            taxNameString = taxNameString.replace("agg.", "aggr.");
        }
        if(hybString.equals(RedListUtil.HYB_X)){
            taxNameString = taxNameString.replace(RedListUtil.HYB_SIGN+" ", RedListUtil.HYB_SIGN);//hybrid sign has no space after it in titleCache for binomial hybrids
        }
        if(taxNameString.endsWith("- Gruppe")){
            taxNameString.replaceAll("- Gruppe", "species group");
        }
        if(taxNameString.endsWith("- group")){
            taxNameString.replaceAll("- group", "species group");
        }
        if(!taxNameString.trim().equals(nameCache)){
            RedListUtil.logMessage(id, "Taxon name inconsistent! taxon.titleCache <-> Column "+RedListUtil.TAXNAME+": "+nameCache+" <-> "+taxNameString, logger);
        }
    }

    private Rank makeRank(long id, RedListGefaesspflanzenImportState state, String rankStr, boolean hasSpecificEpithet) {
        Rank rank = null;
        try {
            if(rankStr.equals("ORA")){
                //special handling for ORA because of two possibilities
                if(hasSpecificEpithet){
                    return Rank.UNRANKED_INFRASPECIFIC();
                }
                else{
                    return Rank.UNRANKED_INFRAGENERIC();
                }
            }
            else{
                rank = state.getTransformer().getRankByKey(rankStr);
            }
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
