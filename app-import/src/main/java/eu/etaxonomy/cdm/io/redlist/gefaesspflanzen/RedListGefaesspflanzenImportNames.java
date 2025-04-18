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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.ICultivarPlantName;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.ITaxonNameBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author pplitzner
 * @since Mar 1, 2016
 */
@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportNames extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final Logger logger = LogManager.getLogger();

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "names";

    private static final boolean STRICT_TITLE_CHECK = false;

    private ExtensionType extensionTypeFlor;

    private ExtensionType extensionTypeAtlasIdx;

    private ExtensionType extensionTypeKart;

    private ExtensionType extensionTypeRl2015;

    private ExtensionType extensionTypeEhrd;

    private ExtensionType extensionTypeWissk;

    public RedListGefaesspflanzenImportNames() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(RedListGefaesspflanzenImportState state) {
        return "SELECT SEQNUM "
                + "FROM V_TAXATLAS_D20_EXPORT t "
                + " ORDER BY SEQNUM";
    }

    @Override
    protected String getRecordQuery(RedListGefaesspflanzenImportConfigurator config) {
        String result = " SELECT * "
                + " FROM V_TAXATLAS_D20_EXPORT t "
                + " WHERE t.SEQNUM IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        makeExtensionTypes();
        super.doInvoke(state);
    }


    private void makeExtensionTypes() {
        extensionTypeFlor = ExtensionType.NewInstance(RedListUtil.FLOR, RedListUtil.FLOR, "");
        extensionTypeAtlasIdx = ExtensionType.NewInstance(RedListUtil.ATLAS_IDX, RedListUtil.ATLAS_IDX, "");
        extensionTypeKart = ExtensionType.NewInstance(RedListUtil.KART, RedListUtil.KART, "");
        extensionTypeRl2015 = ExtensionType.NewInstance(RedListUtil.RL2015, RedListUtil.RL2015, "");
        extensionTypeEhrd = ExtensionType.NewInstance(RedListUtil.EHRD, RedListUtil.EHRD, "");
        extensionTypeWissk = ExtensionType.NewInstance(RedListUtil.WISSK, RedListUtil.WISSK, "");
        getTermService().saveOrUpdate(extensionTypeFlor);
        getTermService().saveOrUpdate(extensionTypeAtlasIdx);
        getTermService().saveOrUpdate(extensionTypeKart);
        getTermService().saveOrUpdate(extensionTypeRl2015);
        getTermService().saveOrUpdate(extensionTypeEhrd);
        getTermService().saveOrUpdate(extensionTypeWissk);
    }

    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Set<ITaxonNameBase> namesToSave = new HashSet<>();
        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                makeSingleNameAndTaxon(state, rs, namesToSave, taxaToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getNameService().saveOrUpdate(TaxonName.castAndDeproxy(namesToSave));
        getTaxonService().saveOrUpdate(taxaToSave);
        return true;
    }

    private void makeSingleNameAndTaxon(RedListGefaesspflanzenImportState state, ResultSet rs, Set<ITaxonNameBase> namesToSave, Set<TaxonBase> taxaToSave)
            throws SQLException {
        long id = rs.getLong(RedListUtil.NAMNR);
        String relationE = rs.getString(RedListUtil.E);
        String relationW = rs.getString(RedListUtil.W);
        String relationK = rs.getString(RedListUtil.K);
        String relationAW = rs.getString(RedListUtil.AW);
        String relationAO = rs.getString(RedListUtil.AO);
        String relationR = rs.getString(RedListUtil.R);
        String relationO = rs.getString(RedListUtil.O);
        String relationS = rs.getString(RedListUtil.S);

        //---NAME---
        INonViralName name = importName(state, rs, namesToSave);


        //--- AUTHORS ---
        importAuthors(state, rs, name);

        //---TAXON---
        TaxonBase<?> taxonBase = importTaxon(rs, name, state);
        if(taxonBase==null){
            RedListUtil.logMessage(id, "!SERIOUS ERROR! Taxon for name "+name+" could not be created!", logger);
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

        //NOTE: the source has to be added after cloning or otherwise the clone would also get the source
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, RedListUtil.TAXON_GESAMTLISTE_NAMESPACE);
        taxaToSave.add(taxonBase);
    }

    private void cloneTaxon(final TaxonBase<?> gesamtListeTaxon, String relationString, String sourceNameSpace, Set<TaxonBase> taxaToSave, long id, RedListGefaesspflanzenImportState state){
        if(isNotBlank(relationString) && !relationString.equals(".")){
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

    private TaxonBase<?> importTaxon(ResultSet rs, INonViralName name, RedListGefaesspflanzenImportState state) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String taxNameString = rs.getString(RedListUtil.TAXNAME);
        String epi1String = rs.getString(RedListUtil.EPI1);
        String epi2String = rs.getString(RedListUtil.EPI2);
        String epi3String = rs.getString(RedListUtil.EPI3);
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
            RedListUtil.logMessage(id, "Taxon was not created!! Unknown value for "+RedListUtil.GUELT+"!", logger);
            return null;
        }

        //common name
        if(taxonBase.isInstanceOf(Taxon.class) && trivialString!=null){
            Taxon taxon = HibernateProxyHelper.deproxy(taxonBase, Taxon.class);
            TaxonDescription description = TaxonDescription.NewInstance(taxon);
            description.addElement(CommonTaxonName.NewInstance(trivialString, Language.GERMAN()));
        }

        //add annotations
        taxonBase.addExtension(florString, extensionTypeFlor);
        taxonBase.addExtension(atlasIdxString, extensionTypeAtlasIdx);
        taxonBase.addExtension(kartString, extensionTypeKart);
        taxonBase.addExtension(rl2015String, extensionTypeRl2015);
        taxonBase.addExtension(ehrdString, extensionTypeEhrd);
        taxonBase.addExtension(wisskString, extensionTypeWissk);

        //check taxon name consistency
        checkTaxonConsistency(id, taxNameString, hybString, epi1String, epi2String, epi3String, taxonBase, state);
        return taxonBase;
    }

    private void importAuthors(RedListGefaesspflanzenImportState state, ResultSet rs, INonViralName name) throws SQLException {

        long id = rs.getLong(RedListUtil.NAMNR);
        String nomZusatzString = rs.getString(RedListUtil.NOM_ZUSATZ);
        String taxZusatzString = rs.getString(RedListUtil.TAX_ZUSATZ);
        String zusatzString = rs.getString(RedListUtil.ZUSATZ);
        String authorKombString = rs.getString(RedListUtil.AUTOR_KOMB);
        String authorBasiString = rs.getString(RedListUtil.AUTOR_BASI);
        String hybString = rs.getString(RedListUtil.HYB);

        //combination author
        if(authorKombString.contains(RedListUtil.EX)){
            // multiple ex authors will be reduced to only the last one
            // e.g. Almq. ex Sternström ex Dahlst. -> Almq. ex Dahlst.
            //first author is ex combination author
            String exAuthorString = RedListUtil.getExAuthorOfExAuthorshipString(authorKombString);
            TeamOrPersonBase<?> exAuthor = (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, exAuthorString);
            name.setExCombinationAuthorship(exAuthor);
            //the last author is the combination author
            String authorString = RedListUtil.getAuthorOfExAuthorshipString(authorKombString);
            TeamOrPersonBase<?> combAuthor = (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, authorString);
            name.setCombinationAuthorship(combAuthor);
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
            TeamOrPersonBase<?> authorExBasi= (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, RedListUtil.getExAuthorOfExAuthorshipString(authorBasiString));
            if(CdmUtils.isBlank(authorKombString)){
                name.setExCombinationAuthorship(authorExBasi);
            }
            else{
                name.setExBasionymAuthorship(authorExBasi);
            }
            TeamOrPersonBase<?> authorBasi= (TeamOrPersonBase<?>) state.getRelatedObject(RedListUtil.AUTHOR_NAMESPACE, RedListUtil.getAuthorOfExAuthorshipString(authorBasiString));
            if(CdmUtils.isBlank(authorKombString)){
                name.setCombinationAuthorship(authorBasi);
            }
            else{
                name.setBasionymAuthorship(authorBasi);
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

    private INonViralName importName(RedListGefaesspflanzenImportState state, ResultSet rs, Set<ITaxonNameBase> namesToSave) throws SQLException {

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

        INonViralName name = null;
        Rank rank = makeRank(id, state, rangString, CdmUtils.isNotBlank(ep3String));
        //cultivar
        if(rank!= null && rank.equals(Rank.CULTIVAR())){
            ICultivarPlantName cultivar = TaxonNameFactory.NewCultivarInstance(rank);
            cultivar.setGenusOrUninomial(ep1String);
            cultivar.setSpecificEpithet(ep2String);
            cultivar.setCultivarEpithet(ep3String);
            name = cultivar;
        }
        //botanical names
        else{
            name = TaxonNameFactory.NewBotanicalInstance(rank);

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
                    if(statusType.equals(NomenclaturalStatusType.INVALID()) || statusType.equals(NomenclaturalStatusType.REJECTED()) ){
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
                //more than two hybrids not yet handled by name parser
                //TODO: use parser when implemented to fully support hybrids
                if(taxNameString.split(RedListUtil.HYB_SIGN).length>2){
                    name = TaxonNameFactory.NewBotanicalInstance(rank);
                    name.setTitleCache(taxNameString, true);
                }
                else if(hybString.equals(RedListUtil.HYB_X)){
                    name.setBinomHybrid(true);
                }
                else if(hybString.equals(RedListUtil.HYB_G)){
                    name.setMonomHybrid(true);
                }
                else if(hybString.equals(RedListUtil.HYB_XF) || hybString.equals(RedListUtil.HYB_XU)){
                    name.setHybridFormula(true);
                    String fullFormula = buildHybridFormula(ep1String, ep2String, ep3String, rank);
                    name = NonViralNameParserImpl.NewInstance().parseFullName(fullFormula, NomenclaturalCode.ICNAFP, rank);
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
                //save hybrid formula
                if(CdmUtils.isNotBlank(formelString)){
                    Annotation annotation = Annotation.NewDefaultLanguageInstance(formelString);
                    annotation.setAnnotationType(AnnotationType.INTERNAL());
                    name.addAnnotation(annotation);
                }
            }
        }

        //add source
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, RedListUtil.NAME_NAMESPACE);

        namesToSave.add(name);
        return name;
    }

    private String buildHybridFormula(String ep1String, String ep2String, String ep3String, Rank rank) {
        String fullFormula = null;
        if(ep1String.contains(RedListUtil.HYB_SIGN)){
            fullFormula = ep1String;
        }
        else if(ep2String.contains(RedListUtil.HYB_SIGN)){
            String[] split = ep2String.split(RedListUtil.HYB_SIGN);
            String hybridFormula1 = ep1String+" "+split[0].trim();
            String hybridFormula2 = ep1String+" "+split[1].trim();
            //check if the genus is mentioned in EP2 or not
            String[] secondHybrid = split[1].trim().split(" ");
            //check if the genus is abbreviated like e.g. Centaurea jacea × C. decipiens
            if(secondHybrid.length>1 && secondHybrid[0].matches("[A-Z]\\.")){
                hybridFormula2 = ep1String+" "+split[1].trim().substring(3);
            }
            else if(secondHybrid.length>1 && secondHybrid[0].matches("[A-Z].*")){
                hybridFormula2 = split[1].trim();
            }
            if(CdmUtils.isNotBlank(ep3String)){
                hybridFormula1 += " "+rank.getAbbreviation()+" "+ep3String;
                hybridFormula2 += " "+rank.getAbbreviation()+" "+ep3String;
            }
            fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
        }
        else if(ep3String.contains(RedListUtil.HYB_SIGN)){
            String[] split = ep3String.split(RedListUtil.HYB_SIGN);
            String hybridFormula1 = ep1String+" "+ep2String+" "+rank.getAbbreviation()+" "+split[0].trim();
            String hybridFormula2 = ep1String+" "+ep2String+" "+rank.getAbbreviation()+" "+split[1].trim();
            //check if the genus is mentioned in EP3 or not
            String[] secondHybrid = split[1].trim().split(" ");
            //check if the genus is abbreviated like e.g. Centaurea jacea jacea × C. jacea subsp. decipiens
            if(secondHybrid.length>1 && secondHybrid[0].matches("[A-Z]\\.")){
                hybridFormula2 = ep1String+" "+split[1].trim().substring(3);
            }
            else if(secondHybrid.length>1 && secondHybrid[0].matches("[A-Z].*")){
                hybridFormula2 = split[1].trim();
            }
            fullFormula = hybridFormula1+" "+RedListUtil.HYB_SIGN+" "+hybridFormula2;
        }
        return fullFormula;
    }

    private void checkNameConsistency(long id, String nomZusatzString, String taxZusatzString,
            String zusatzString, String authorString, String hybString, INonViralName name) {
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
        if(!STRICT_TITLE_CHECK && authorString.matches(".*ex.*ex.*")){
            return;
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

    private void checkTaxonConsistency(long id, String taxNameString, String hybString, String epi1String, String epi2String, String epi3String, TaxonBase<?> taxonBase, RedListGefaesspflanzenImportState state) {
        if(taxNameString.split(RedListUtil.HYB_SIGN).length>2){
            RedListUtil.logInfoMessage(id, "multiple hybrid signs. No name check for "+taxNameString, logger);
            return;
        }

        String nameCache = taxonBase.getName().getNameCache().trim();
        taxNameString = taxNameString.trim();
        taxNameString = taxNameString.replaceAll(" +", " ");


        if((hybString.equals(RedListUtil.HYB_X) || hybString.equals(RedListUtil.HYB_N))
                && nameCache.matches(".*\\s"+RedListUtil.HYB_SIGN+"\\w.*")){
            taxNameString = taxNameString.replace(" "+RedListUtil.HYB_SIGN+" ", " "+RedListUtil.HYB_SIGN);//hybrid sign has no space after it in titleCache for binomial hybrids
            taxNameString = taxNameString.replace(" x ", " "+RedListUtil.HYB_SIGN);//in some cases a standard 'x' is used
        }
        else if(hybString.equals(RedListUtil.HYB_G)){
            taxNameString = taxNameString.replace("X ", RedListUtil.HYB_SIGN);
        }
        else if(hybString.equals(RedListUtil.HYB_GF)){
            taxNameString = taxNameString.replace(" "+RedListUtil.HYB_SIGN+" ", " "+RedListUtil.HYB_SIGN);
        }
        else if(hybString.equals(RedListUtil.HYB_XF)){
            nameCache = taxonBase.getName().getTitleCache();
            if(nameCache.contains("sec")){
                nameCache = nameCache.substring(0, nameCache.indexOf("sec"));
            }
            if(!STRICT_TITLE_CHECK){
                taxNameString = buildHybridFormula(epi1String, epi2String, epi3String, taxonBase.getName().getRank());
            }
            if(taxNameString.split(RedListUtil.HYB_SIGN).length==1){
                taxNameString = taxNameString.replace(RedListUtil.HYB_SIGN+" ", RedListUtil.HYB_SIGN);
            }
        }

        if(taxNameString.endsWith("- Gruppe")){
            taxNameString = taxNameString.replaceAll("- Gruppe", "species group");
        }
        if(taxNameString.endsWith("- group")){
            taxNameString = taxNameString.replaceAll("- group", "species group");
        }

        taxNameString = taxNameString.replace("agg.", "aggr.");
        taxNameString = taxNameString.replace("[ranglos]", "[unranked]");

        if(taxonBase.getName().getRank()!=null){
            if(taxonBase.getName().getRank().equals(Rank.PROLES())){
                taxNameString = taxNameString.replace("proles", "prol.");
            }
            else if(taxonBase.getName().getRank().equals(state.getRank(RedListUtil.uuidRankCollectionSpecies))){
                taxNameString = taxNameString.replace("\"Sammelart\"", "\"Coll. Species\"");
            }
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
                    //re-load term because the representation was changed before
                    return (Rank) getTermService().load(Rank.uuidInfraspecificTaxon);
                }
                else{
                    return Rank.UNRANKED_INFRAGENERIC();
                }
            }
            else if(rankStr.equals("SAM")){
                return getRank(state, RedListUtil.uuidRankCollectionSpecies, "Collective Species", "Collective Species", "\"Coll. Species\"", (OrderedTermVocabulary<Rank>) Rank.GENUS().getVocabulary(), null, RankClass.SpeciesGroup);
            }
            else if(rankStr.equals("MOD")){
                return getRank(state, RedListUtil.uuidRankModification, "Modification", "Modification", "modificatio", (OrderedTermVocabulary<Rank>) Rank.GENUS().getVocabulary(), null, RankClass.Infraspecific);
            }
            else if(rankStr.equals("SPI")){
                return getRank(state, RedListUtil.uuidRankSubspeciesPrincipes, "Subspecies principes", "Subspecies principes", "subsp. princ.", (OrderedTermVocabulary<Rank>) Rank.GENUS().getVocabulary(), null, RankClass.Infraspecific);
            }
            else if(rankStr.equals("KMB")){
                return getRank(state, RedListUtil.uuidRankCombination, "Combination", "Combination", "", (OrderedTermVocabulary<Rank>) Rank.GENUS().getVocabulary(), null, RankClass.Infraspecific);
            }
            else if(rankStr.equals("'FO")){
                return getRank(state, RedListUtil.uuidRankForme, "Forme'", "Forme'", "", (OrderedTermVocabulary<Rank>) Rank.GENUS().getVocabulary(), null, RankClass.Infraspecific);
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
