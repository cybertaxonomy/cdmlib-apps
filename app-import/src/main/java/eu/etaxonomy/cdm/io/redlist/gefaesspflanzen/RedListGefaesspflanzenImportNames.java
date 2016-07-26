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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.CultivarPlantName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
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

    private static final boolean STRICT_TITLE_CHECK = false;

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

        //---CONCEPT RELATIONSHIPS---
        //E, W, K, AW, AO, R, O, S
        cloneTaxon(taxonBase, relationE, RedListUtil.CLASSIFICATION_NAMESPACE_E, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationW, RedListUtil.CLASSIFICATION_NAMESPACE_W, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationK, RedListUtil.CLASSIFICATION_NAMESPACE_K, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationAW, RedListUtil.CLASSIFICATION_NAMESPACE_AW, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationAO, RedListUtil.CLASSIFICATION_NAMESPACE_AO, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationR, RedListUtil.CLASSIFICATION_NAMESPACE_R, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationO, RedListUtil.CLASSIFICATION_NAMESPACE_O, taxaToSave, id, state);
        cloneTaxon(taxonBase, relationS, RedListUtil.CLASSIFICATION_NAMESPACE_S, taxaToSave, id, state);
        //checklist
        TaxonBase<?> checklistTaxon = null;
        if(CdmUtils.isNotBlank(clTaxonString) && !clTaxonString.trim().equals("-")){
            checklistTaxon = (TaxonBase<?>) taxonBase.clone();
            if(checklistTaxon.isInstanceOf(Taxon.class)){
                TaxonRelationship relation = HibernateProxyHelper.deproxy(checklistTaxon, Taxon.class).addTaxonRelation(HibernateProxyHelper.deproxy(taxonBase, Taxon.class), TaxonRelationshipType.CONGRUENT_TO(), null, null);
                relation.setDoubtful(true);
            }

            ImportHelper.setOriginalSource(checklistTaxon, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_CHECKLISTE_NAMESPACE);
            taxaToSave.add(checklistTaxon);
        }

        //NOTE: the source has to be added after cloning or otherwise the clone would also get the source
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE);
        taxaToSave.add(taxonBase);
    }

    private void cloneTaxon(final TaxonBase<?> gesamtListeTaxon, String relationString, String sourceNameSpace, Set<TaxonBase> taxaToSave, long id, RedListGefaesspflanzenImportState state){
        if(CdmUtils.isNotBlank(relationString) && !relationString.equals(".")){
            Taxon clonedTaxon = null;

            if(gesamtListeTaxon.isInstanceOf(Taxon.class)){
                clonedTaxon = HibernateProxyHelper.deproxy(gesamtListeTaxon.clone(), Taxon.class);
            }
            else if(gesamtListeTaxon.isInstanceOf(Synonym.class)){
                clonedTaxon = Taxon.NewInstance(gesamtListeTaxon.getName(), gesamtListeTaxon.getSec());
            }
            else{
                RedListUtil.logMessage(id, "Taxon base "+gesamtListeTaxon+" is neither taxon nor synonym! Taxon could not be cloned", logger);
                return;
            }
            ImportHelper.setOriginalSource(clonedTaxon, state.getTransactionalSourceReference(), id, sourceNameSpace);
            taxaToSave.add(clonedTaxon);
        }
    }

    private TaxonBase<?> importTaxon(ResultSet rs, NonViralName<?> name) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String gueltString = rs.getString(RedListUtil.GUELT);
        String trivialString = rs.getString(RedListUtil.TRIVIAL);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
        String hybString = rs.getString(RedListUtil.HYB);
        String florString = rs.getString(RedListUtil.FLOR);
        String atlasIdxString = rs.getString(RedListUtil.ATLAS_IDX);
        String kartString = rs.getString(RedListUtil.KART);
        String rl2015String = rs.getString(RedListUtil.RL2015);
        String ehrdString = rs.getString(RedListUtil.EHRD);
        String wisskString = rs.getString(RedListUtil.WISSK);

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
            description.addElement(CommonTaxonName.NewInstance(trivialString, Language.GERMAN()));
        }

        //add annotations
        addAnnotation(RedListUtil.FLOR+": "+florString, taxonBase);
        addAnnotation(RedListUtil.ATLAS_IDX+": "+atlasIdxString, taxonBase);
        addAnnotation(RedListUtil.KART+": "+kartString, taxonBase);
        addAnnotation(RedListUtil.RL2015+": "+rl2015String, taxonBase);
        addAnnotation(RedListUtil.EHRD+": "+ehrdString, taxonBase);
        addAnnotation(RedListUtil.WISSK+": "+wisskString, taxonBase);

        //check taxon name consistency
        checkTaxonConsistency(id, taxNameString, hybString, taxonBase);
        return taxonBase;
    }

    private void addAnnotation(String string, TaxonBase<?> taxonBase) {
        if(CdmUtils.isNotBlank(string)){
            taxonBase.addAnnotation(Annotation.NewInstance(string, AnnotationType.TECHNICAL(), Language.GERMAN()));
        }
    }

    private void importAuthors(RedListGefaesspflanzenImportState state, ResultSet rs, NonViralName<?> name) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);
        String zusatzString = rs.getString(RedListUtil.ZUSATZ);
        String authorKombString = rs.getString(RedListUtil.AUTOR_KOMB);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
        String hybString = rs.getString(RedListUtil.HYB);

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
        checkNameConsistency(id, nomZusatzString, taxZusatzString, zusatzString, authorString, hybString, name);
    }

    private NonViralName<?> importName(RedListGefaesspflanzenImportState state, ResultSet rs, Set<TaxonNameBase> namesToSave) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String rangString = rs.getString(RedListUtil.RANG);
        String ep1String = rs.getString(RedListUtil.EPI1);
        String ep2String = rs.getString(RedListUtil.EPI2);
        String ep3String = rs.getString(RedListUtil.EPI3);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String hybString = rs.getString(RedListUtil.HYB);
        String formelString = rs.getString(RedListUtil.FORMEL);

        if(CdmUtils.isBlank(taxNameString) && CdmUtils.isBlank(ep1String)){
            RedListUtil.logMessage(id, "No name found!", logger);
        }

        NonViralName<?> name = null;
        Rank rank = makeRank(id, state, rangString, ep3String!=null);
        //cultivar
        if(rank!= null && rank.equals(Rank.CULTIVAR())){
            CultivarPlantName cultivar = CultivarPlantName.NewInstance(rank);
            cultivar.setGenusOrUninomial(ep1String);
            cultivar.setSpecificEpithet(ep2String);
            cultivar.setCultivarName(ep3String);
            name = cultivar;
        }
        //botanical names
        else{
            name = BotanicalName.NewInstance(rank);

            //ep1 should always be present
            if(CdmUtils.isBlank(ep1String)){
                RedListUtil.logMessage(id, RedListUtil.EPI1+" is empty!", logger);
            }
            name.setGenusOrUninomial(ep1String);
            if(CdmUtils.isNotBlank(ep2String)){
                if(rank!=null && rank.isInfraGenericButNotSpeciesGroup()){
                    name.setInfraGenericEpithet(ep2String);
                }
                else{
                    name.setSpecificEpithet(ep2String);
                }
            }
            if(CdmUtils.isNotBlank(ep3String)){
                name.setInfraSpecificEpithet(ep3String);
            }


            //nomenclatural status
            if(CdmUtils.isNotBlank(nomZusatzString)){
                NomenclaturalStatusType statusType = makeNomenclaturalStatus(id, state, nomZusatzString);
                if(statusType!=null){
                    NomenclaturalStatus status = NomenclaturalStatus.NewInstance(statusType);
                    //special case for invalid names where the DB entry contains
                    //additional information in brackets e.g. "nom. inval. (sine basion.)"
                    if(statusType.equals(NomenclaturalStatusType.INVALID())){
                        Pattern pattern = Pattern.compile("\\((.*?)\\)");
                        Matcher matcher = pattern.matcher(nomZusatzString);
                        if (matcher.find()){
                            status.setRuleConsidered(matcher.group(1));
                        }
                    }
                    name.addStatus(status);
                }
            }
            //hybrid
            if(CdmUtils.isNotBlank(hybString)){
                //save hybrid formula
                if(CdmUtils.isNotBlank(formelString)){
                    Annotation annotation = Annotation.NewDefaultLanguageInstance(formelString);
                    annotation.setAnnotationType(AnnotationType.TECHNICAL());
                    name.addAnnotation(annotation);
                }
                //more than two hybrids not yet handled by name parser
                //TODO: use parser when implemented to fully support hybrids
                if(taxNameString.split(RedListUtil.HYB_SIGN).length>2){
                    name = BotanicalName.NewInstance(rank);
                    name.setTitleCache(taxNameString, true);
                }
                else if(hybString.equals(RedListUtil.HYB_X)){
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
                        String hybridFormula1 = ep1String+" "+split[0].trim();
                        String hybridFormula2 = ep1String+" "+split[1].trim();
                        //check if the specific epithets are from the same genus or not like e.g. EPI2 = pratensis × Lolium multiflorum
                        String[] secondHybrid = split[1].trim().split(" ");
                        if(secondHybrid.length>1 && secondHybrid[0].matches("[A-Z].*")){
                            hybridFormula2 = split[1];
                        }
                        if(CdmUtils.isNotBlank(ep3String)){
                            hybridFormula1 += " "+ep3String;
                            hybridFormula2 += " "+ep3String;
                        }
                        String fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
                        name = NonViralNameParserImpl.NewInstance().parseFullName(fullFormula, NomenclaturalCode.ICNAFP, rank);
                    }
                    else if(ep3String.contains(RedListUtil.HYB_SIGN)){
                        String[] split = ep3String.split(RedListUtil.HYB_SIGN);
                        String hybridFormula1 = ep1String+" "+ep2String+" "+split[0];
                        String hybridFormula2 = ep1String+" "+ep2String+" "+split[1];
                        String fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
                        name = NonViralNameParserImpl.NewInstance().parseFullName(fullFormula, NomenclaturalCode.ICNAFP, rank);
                    }
                }
                else if(hybString.equals(RedListUtil.HYB_N)){
                    name = NonViralNameParserImpl.NewInstance().parseFullName(taxNameString, NomenclaturalCode.ICNAFP, rank);
                }
                else if(hybString.equals(RedListUtil.HYB_GF)){
                    if(ep1String.contains(RedListUtil.HYB_SIGN)){
                        name = NonViralNameParserImpl.NewInstance().parseFullName(ep1String, NomenclaturalCode.ICNAFP, rank);
                    }
                    else{
                        RedListUtil.logMessage(id, "HYB is "+hybString+" but "+RedListUtil.HYB+" does not contain "+RedListUtil.HYB_SIGN, logger);
                    }
                }
                else if(hybString.equals(RedListUtil.HYB_XS)){
                    //nothing to do
                }
                else{
                    logger.error("HYB value "+hybString+" not yet handled");
                }
            }
        }
        //add source
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, RedListUtil.NAME_NAMESPACE);

        namesToSave.add(name);
        return name;
    }

    private void checkNameConsistency(long id, String nomZusatzString, String taxZusatzString,
            String zusatzString, String authorString, String hybString, NonViralName<?> name) {
        String authorshipCache = name.getAuthorshipCache();
        //FIXME: remove split length check when name parser can parse multiple hybrid parents
        if(hybString.equals(RedListUtil.HYB_XF) && name.getTitleCache().split(RedListUtil.HYB_SIGN).length==2){
            if(name.getHybridChildRelations().isEmpty()){
                RedListUtil.logMessage(id, "Hybrid formula but no hybrid child relations: "+name.getTitleCache(), logger);
                return;
            }
            return;
        }

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
        if(STRICT_TITLE_CHECK){
            if(!authorString.equals(authorshipCache)){
                RedListUtil.logMessage(id, "Authorship inconsistent! name.authorhshipCache <-> Column "+RedListUtil.AUTOR+": "+authorshipCache+" <-> "+authorString, logger);
            }
        }
        else{
            if(CdmUtils.isNotBlank(authorString) && !authorString.startsWith(authorshipCache)){
                RedListUtil.logMessage(id, "Authorship inconsistent! name.authorhshipCache <-> Column "+RedListUtil.AUTOR+": "+authorshipCache+" <-> "+authorString, logger);
            }
        }
    }

    private void checkTaxonConsistency(long id, String taxNameString, String hybString, TaxonBase<?> taxonBase) {
        String nameCache = HibernateProxyHelper.deproxy(taxonBase.getName(), NonViralName.class).getNameCache().trim();
        taxNameString = taxNameString.trim();
        taxNameString = taxNameString.replaceAll(" +", " ");

        if(taxNameString.endsWith("agg.")){
            taxNameString = taxNameString.replace("agg.", "aggr.");
        }

        if(hybString.equals(RedListUtil.HYB_X) || hybString.equals(RedListUtil.HYB_N)){
            taxNameString = taxNameString.replace(" "+RedListUtil.HYB_SIGN+" ", " "+RedListUtil.HYB_SIGN);//hybrid sign has no space after it in titleCache for binomial hybrids
            taxNameString = taxNameString.replace(" x ", " "+RedListUtil.HYB_SIGN);//in some cases a standard 'x' is used
        }
        else if(hybString.equals(RedListUtil.HYB_G)){
            taxNameString = taxNameString.replace("X ", RedListUtil.HYB_SIGN);
        }
        else if(hybString.equals(RedListUtil.HYB_GF)){
            taxNameString = taxNameString.replace(" "+RedListUtil.HYB_SIGN, " x");
        }

        if(taxNameString.endsWith("- Gruppe")){
            taxNameString = taxNameString.replaceAll("- Gruppe", "species group");
        }
        if(taxNameString.endsWith("- group")){
            taxNameString = taxNameString.replaceAll("- group", "species group");
        }

        taxNameString = taxNameString.replace("[ranglos]", "[unranked]");
        if(taxonBase.getName().getRank()!=null && taxonBase.getName().getRank().equals(Rank.PROLES())){
            taxNameString = taxNameString.replace("proles", "prol.");
        }
        if(STRICT_TITLE_CHECK){
            if(!taxNameString.trim().equals(nameCache)){
                RedListUtil.logMessage(id, "Taxon name inconsistent! taxon.nameCache <-> Column "+RedListUtil.TAXNAME+": "+nameCache+" <-> "+taxNameString, logger);
            }
        }
        else{
            if(!taxNameString.startsWith(nameCache)){
                RedListUtil.logMessage(id, "Taxon name inconsistent! taxon.nameCache <-> Column "+RedListUtil.TAXNAME+": "+nameCache+" <-> "+taxNameString, logger);
            }
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
