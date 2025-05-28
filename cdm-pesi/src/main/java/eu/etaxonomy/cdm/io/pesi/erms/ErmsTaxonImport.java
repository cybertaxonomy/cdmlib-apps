/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.StringComparator;
import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportLsidMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMarkerMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsTaxonImportValidator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTaxonExport;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IRelationshipType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStanding;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.cache.name.TaxonNameDefaultCacheStrategy;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsTaxonImport
        extends ErmsImportBase<TaxonBase<?>>
        implements IMappingImport<TaxonBase<?>, ErmsImportState>{

    private static final long serialVersionUID = -7111568277264140051L;
    private static Logger logger = LogManager.getLogger();

	private static final String pluralString = "taxa";
	private static final String dbTableName = "tu";
	private static final Class<?> cdmTargetClass = TaxonBase.class;

	private static Map<String, Integer> unacceptReasons = new HashMap<>();

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	public ErmsTaxonImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT id FROM tu " ;  //WHERE id NOT IN (147415) for now we exclude Monera as it has no children and is unclear what classification it has. In ERMS it is alternative accepted name (in https://en.wikipedia.org/wiki/Monera it might be a super taxon to bacteria).
		return strQuery;
	}

	@Override
    protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "id", TAXON_NAMESPACE)); //id + tu_status
			mapping.addMapper(DbImportLsidMapper.NewInstance("GUID", "lsid"));

			UUID tsnUuid = ErmsTransformer.uuidExtTsn;
			ExtensionType tsnExtType = getExtensionType(tsnUuid, "TSN", "TSN", "TSN");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("tsn", tsnExtType));
//			mapping.addMapper(DbImportStringMapper.NewInstance("tu_name", "(NonViralName)name.nameCache"));

			ExtensionType displayNameExtType = getExtensionType(ErmsTransformer.uuidExtDisplayName, "display name", "display name", "display name");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("tu_displayname", displayNameExtType));
            //Ignore fuzzyName
            //  ExtensionType fuzzyNameExtType = getExtensionType(ErmsTransformer.uuidExtFuzzyName, "fuzzy name", "fuzzy name", "fuzzy name");
            //  mapping.addMapper(DbImportExtensionMapper.NewInstance("tu_fuzzyname", fuzzyNameExtType));
			mapping.addMapper(DbImportStringMapper.NewInstance("tu_authority", "name.authorshipCache"));

			ExtensionType fossilStatusExtType = getExtensionType(ErmsTransformer.uuidExtFossilStatus, "fossil status", "fossil status", "fos. stat.");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("fossil_name", fossilStatusExtType));

			ExtensionType unacceptExtType = getExtensionType(ErmsTransformer.uuidExtUnacceptReason, "unaccept reason", "unaccept reason", "reason");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("tu_unacceptreason", unacceptExtType));

			ExtensionType qualityStatusExtType = getExtensionType(ErmsTransformer.uuidExtQualityStatus, "quality status", "quality status", "quality status");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("qualitystatus_name", qualityStatusExtType)); //checked by Tax Editor ERMS1.1, Added by db management team (2x), checked by Tax Editor

			ExtensionType cacheCitationExtType = getExtensionType(PesiTransformer.uuidExtCacheCitation, "cache_citation", "quality status", "cache_citation");
            mapping.addMapper(DbImportExtensionMapper.NewInstance("cache_citation", cacheCitationExtType));

            //flags
			mapping.addMapper(DbImportMarkerMapper.NewInstance("tu_marine", ErmsTransformer.uuidMarkerMarine, "marine", "marine", "marine", null));
			mapping.addMapper(DbImportMarkerMapper.NewInstance("tu_brackish", ErmsTransformer.uuidMarkerBrackish, "brackish", "brackish", "brackish", null));
			mapping.addMapper(DbImportMarkerMapper.NewInstance("tu_fresh", ErmsTransformer.uuidMarkerFreshwater, "freshwater", "fresh", "fresh", null));
			mapping.addMapper(DbImportMarkerMapper.NewInstance("tu_terrestrial", ErmsTransformer.uuidMarkerTerrestrial, "terrestrial", "terrestrial", "terrestrial", null));

			//last action, species expert
			ExtensionType speciesExpertNameExtType = getExtensionType(PesiTransformer.uuidExtSpeciesExpertName, "species expert name", "species expert name", "species expert name");
            mapping.addMapper(DbImportExtensionMapper.NewInstance("ExpertName", speciesExpertNameExtType)); //according to sql script ExpertName maps to SpeciesExpertName in ERMS
            AnnotationType lastActionDateType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastActionDate, "Last action date", "Last action date", null);
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastActionDate", lastActionDateType));
            AnnotationType lastActionType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastAction, "Last action", "Last action", null);
            MarkerType hasNoLastActionMarkerType = getMarkerType(DbLastActionMapper.uuidMarkerTypeHasNoLastAction, "has no last action", "No last action information available", "no last action");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastAction", lastActionType, hasNoLastActionMarkerType));

            //MAN authorshipCache => appendedPhrase
            mapping.addMapper(DbImportMethodMapper.NewDefaultInstance(this, "appendedPhraseForMisapplications", ErmsImportState.class));

            //titleCache compare
            mapping.addMapper(DbImportMethodMapper.NewDefaultInstance(this, "testTitleCache", ErmsImportState.class));

			//ignore
            mapping.addMapper(DbIgnoreMapper.NewInstance("tu_sp", "included in rank/object creation, only needed for defining kingdom"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("tu_fossil", "tu_fossil implemented as foreign key"));

		}
		return mapping;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strSelect = " SELECT tu.*, parent1.tu_name AS parent1name, parent2.tu_name AS parent2name, parent3.tu_name AS parent3name, parent4.tu_name AS parent4name, " +
		            " parent1.tu_rank AS parent1rank, parent2.tu_rank AS parent2rank, parent3.tu_rank AS parent3rank, " +
		            " status.status_id as status_id, status.status_name, fossil.fossil_name, qualitystatus.qualitystatus_name," +
		            " s.sessiondate lastActionDate, a.action_name lastAction, s.ExpertName ";
		String strFrom = " FROM tu  LEFT OUTER JOIN  tu AS parent1 ON parent1.id = tu.tu_parent " +
				" LEFT OUTER JOIN   tu AS parent2  ON parent2.id = parent1.tu_parent " +
				" LEFT OUTER JOIN tu AS parent3 ON parent2.tu_parent = parent3.id " +
				" LEFT OUTER JOIN tu AS parent4 ON parent3.tu_parent = parent4.id " +
                " LEFT OUTER JOIN status ON tu.tu_status = status.status_id " +
				" LEFT OUTER JOIN fossil ON tu.tu_fossil = fossil.fossil_id " +
				" LEFT OUTER JOIN qualitystatus ON tu.tu_qualitystatus = qualitystatus.id " +
				" LEFT OUTER JOIN tu_sessions ts ON ts.tu_id = tu.id " +
                " LEFT OUTER JOIN [sessions] s ON s.id = ts.session_id " +
                " LEFT OUTER JOIN actions a ON a.id = ts.action_id ";
		String strWhere = " WHERE ( tu.id IN (" + ID_LIST_TOKEN + ") )";
		String strOrderBy = " ORDER BY tu.id, s.sessiondate DESC, a.id DESC ";
		String strRecordQuery = strSelect + strFrom + strWhere + strOrderBy;
		return strRecordQuery;
	}

	@Override
	protected void doInvoke(ErmsImportState state) {

		state.setAcceptedTaxaKeys(getAcceptedTaxaKeys(state));

		//first path
		super.doInvoke(state);
		if(true){
		    logUnacceptReasons();
		}
		return;
	}

    Integer lastTaxonId = null;
    @Override
    protected boolean ignoreRecord(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        boolean result = id.equals(lastTaxonId);
        lastTaxonId = id;
        return result;
    }

	private Set<Integer> getAcceptedTaxaKeys(ErmsImportState state) {

	    Set<Integer> result = new HashSet<>();
		String idCol = " id ";
		String tuFk = "tu_id";
		String vernacularsTable = "vernaculars";
		String distributionTable = "dr";
		String notesTable = "notes";
		String sql =
                "          SELECT id FROM tu WHERE tu_acctaxon is NULL" //id of taxa not having accepted taxon
                + " UNION  SELECT DISTINCT tu_acctaxon FROM tu "  //fk to accepted taxon (either the accepted taxon or the taxon itself, if accepted)
                + " UNION  SELECT id FROM tu WHERE trim(tu.tu_unacceptreason) like 'misidentification' OR trim(tu.tu_unacceptreason) like 'misidentifications' OR "
                            + " tu.tu_unacceptreason like 'misapplied %%name' OR "
                            + " tu.tu_unacceptreason like '%%misapplication%%' OR "
                            + " tu.tu_unacceptreason like 'incorrect identification%%'" //Misapplications, see ErmsTransformer.getSynonymRelationTypesByKey
                + " UNION  SELECT syn.id FROM tu syn INNER JOIN tu acc ON syn.tu_acctaxon = acc.id WHERE syn.id = acc.tu_parent AND acc.id <> syn.id "  //see also ErmsTaxonRelationImport.isAccepted, there are some autonyms being the accepted taxon of there own parents
                + " UNION  SELECT DISTINCT %s FROM %s " //vernaculars
                + " UNION  SELECT DISTINCT %s FROM %s "  //distributions
                + " UNION  SELECT DISTINCT %s FROM %s ";  //notes
		sql = String.format(sql,
		        tuFk, vernacularsTable,
				tuFk, distributionTable,
				tuFk, notesTable);
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);
		try {
			while (rs.next()){
				Integer id;
				id = rs.getInt(idCol.trim());
				result.add(id);
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		//currently no referencing objects needed
	    Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;
	}

	@Override
	public TaxonBase<?> createObject(ResultSet rs, ErmsImportState state) throws SQLException {
		int statusId = rs.getInt("status_id");
		Integer meId = rs.getInt("id");
		Integer accFinal = nullSafeInt(rs, "tu_acctaxon");

        TaxonName taxonName = getTaxonName(rs, state);
		fillTaxonName(taxonName, rs, state, meId);

		//add original source for taxon name (taxon original source is added in mapper)
		Reference citation = state.getTransactionalSourceReference();
		addOriginalSource(rs, taxonName, "id", NAME_NAMESPACE, citation);

		TaxonBase<?> result;
		//handle accepted<-> synonym, we create more accepted taxa as we need them within the tree or to attache factual data
		if (state.getAcceptedTaxaKeys().contains(meId)){
			Taxon taxon = Taxon.NewInstance(taxonName, citation);
			if (statusId != 1){
				logger.info("Taxon created as taxon but has status <> 1 ("+statusId+"): " + meId);
				boolean idsDiffer = accFinal != null && !meId.equals(accFinal);
				handleNotAcceptedTaxonStatus(taxon, statusId, idsDiffer, state, rs);
			}
			result = taxon;
		}else{
			result = Synonym.NewInstance(taxonName, citation);
			//real synonyms (id <> tu_acctaxon) are always handled as "synonym" or "pro parte synonym"
//			handleNotAcceptedTaxonStatus(result, statusId, state, rs);
		}

		handleNameStatus(result.getName(), rs, state);
		return result;
	}

    private void handleNameStatus(TaxonName name, ResultSet rs, ErmsImportState state) throws SQLException {
        NomenclaturalStatusType nomStatus = null;
        int tuStatus = rs.getInt("tu_status");
        //the order is bottom up from SQL script as their values are overridden from top to bottom
        if (tuStatus == 8){
            //species inquirenda
            //TODO nom. standing unclear
            NomenclaturalStanding nomenclaturalStanding = NomenclaturalStanding.INVALID;
            nomStatus = getNomenclaturalStatusType(state, ErmsTransformer.uuidNomStatusSpeciesInquirenda, "species inquirenda", "species inquirenda", null, nomenclaturalStanding, Language.LATIN(), null);
        }else if (tuStatus == 7){
            //temporary name
            //TODO nom. standing unclear
            NomenclaturalStanding nomenclaturalStanding = NomenclaturalStanding.INVALID;
            nomStatus = getNomenclaturalStatusType(state, PesiTransformer.uuidNomStatusTemporaryName, "temporary name", "temporary name", null, nomenclaturalStanding, Language.ENGLISH(), null);
        }else if (tuStatus == 6){
            //nomen dubium
            nomStatus = NomenclaturalStatusType.DOUBTFUL();
        }else if (tuStatus == 5){
            //"alternate representation"
            //TODO nom. standing unclear
            NomenclaturalStanding nomenclaturalStanding = NomenclaturalStanding.VALID;
            nomStatus = getNomenclaturalStatusType(state, ErmsTransformer.uuidNomStatusAlternateRepresentation, "alternate representation", "alternate representation", null, nomenclaturalStanding, Language.ENGLISH(), null);
        }else if (tuStatus == 3){
            //nomen nudum
            nomStatus = NomenclaturalStatusType.NUDUM();
        }
        if (nomStatus == null){
            //IN SQL Script it is set first by unacceptreason and then overriden if above tu_status exists
            String unacceptReason = rs.getString("tu_unacceptreason");
            try {
                nomStatus = state.getTransformer().getNomenclaturalStatusByKey(unacceptReason);
            } catch (UndefinedTransformerMethodException e) {logger.warn("Unhandled method");
            }
        }
        if (nomStatus != null){
            name.addStatus(nomStatus, null, null);
        }
    }

    private TaxonName fillTaxonName(TaxonName taxonName, ResultSet rs, ErmsImportState state, Integer meId) throws SQLException {
        String tuName = rs.getString("tu_name");
		String displayName = rs.getString("tu_displayname").trim();

		String parent1Name = rs.getString("parent1name");
		Integer parent1Rank = rs.getInt("parent1rank");

		String parent2Name = rs.getString("parent2name");
		Integer parent2Rank = rs.getInt("parent2rank");

		String parent3Name = rs.getString("parent3name");
		Integer parent3Rank = rs.getInt("parent3rank");

	    String parent4Name = rs.getString("parent4name");

		//set epithets
		if (taxonName.isGenus() || taxonName.isSupraGeneric()){
			taxonName.setGenusOrUninomial(tuName);
		}else if (taxonName.isInfraGeneric()){
			taxonName.setInfraGenericEpithet(tuName);
			taxonName.setGenusOrUninomial(parent1Name);
		}else if (taxonName.isSpecies()){
			taxonName.setSpecificEpithet(tuName);
			getGenusAndInfraGenus(parent1Name, parent2Name, parent1Rank, taxonName);
		}else if (taxonName.isInfraSpecific()){
			if (parent1Rank < 220){
				handleException(parent1Rank, taxonName, displayName, meId);
			}
			taxonName.setInfraSpecificEpithet(tuName);
			if (parent1Rank > 220){  //parent is still infraspecific
			    taxonName.setSpecificEpithet(parent2Name);
			    getGenusAndInfraGenus(parent3Name, parent4Name, parent3Rank, taxonName);
			}else{
			    //default
			    taxonName.setSpecificEpithet(parent1Name);
			    getGenusAndInfraGenus(parent2Name, parent3Name, parent2Rank, taxonName);
			}
		}else if (taxonName.getRank()== null){
			if ("Biota".equalsIgnoreCase(tuName)){
				Rank rank = Rank.DOMAIN();  //should be Superdomain
				taxonName.setRank(rank);
				taxonName.setGenusOrUninomial(tuName);
			}else{
				String warning = "TaxonName has no rank. Use namecache.";
				logger.warn(warning);
				taxonName.setNameCache(tuName);
			}
		}

		//e.g. Leucon [Platyhelminthes] ornatus
		if (containsBrackets(displayName)){
			taxonName.setNameCache(displayName);
			logger.warn("Set name cache: " +  displayName + "; id =" + meId);
		}
        if (!taxonName.getNameCache().equals(displayName) && !isErroneousSubgenus(taxonName, displayName)){
            int pos = CdmUtils.diffIndex(taxonName.getNameCache(), displayName);
            logger.warn("Computed name cache differs at "+pos+".\n Computed   : " + taxonName.getNameCache()+"\n DisplayName: " +displayName);
            taxonName.setNameCache(displayName, true);
        }
		taxonName.getTitleCache();
        return taxonName;
    }

    private static boolean isErroneousSubgenus(TaxonName taxonName, String displayName) {
        //this is an error in ERMS formatting in v2019 for ICNafp names, that hopefully soon will be corrected
        return (taxonName.isSpecies() && displayName.contains(" subg. "));
    }

    @SuppressWarnings("unused")  //used by MethodMapper
    private static TaxonBase<?> appendedPhraseForMisapplications(ResultSet rs, ErmsImportState state) throws SQLException{
        TaxonBase<?> taxon = (TaxonBase<?>)state.getRelatedObject(DbImportStateBase.CURRENT_OBJECT_NAMESPACE, DbImportStateBase.CURRENT_OBJECT_ID);
        TaxonName taxonName = taxon.getName();
        String unacceptreason = rs.getString("tu_unacceptreason");
        IRelationshipType[] rels = state.getTransformer().getSynonymRelationTypesByKey(unacceptreason, state);
        if (rels[1]!= null && rels[1].equals(TaxonRelationshipType.MISAPPLIED_NAME_FOR())){
            taxon.setAppendedPhrase(taxonName.getAuthorshipCache());
            taxon.setSec(null);
            taxonName.setAuthorshipCache(null, taxonName.isProtectedAuthorshipCache());
            //TODO maybe some further authorship handling is needed if authors get parsed, but not very likely for MAN authorship
        }
        if(state.getUnhandledUnacceptReason() != null){
            //to handle it hear is a workaround, as the real place where it is handled is DbImportSynonymMapper which is called ErmsTaxonRelationImport but where it is diffcult to aggregate logging data
            addUnacceptReason(state.getUnhandledUnacceptReason());
        }
        return taxon;
    }

    private static void addUnacceptReason(String unhandledUnacceptReason) {
        unhandledUnacceptReason = unhandledUnacceptReason.toLowerCase();
        if (!unacceptReasons.keySet().contains(unhandledUnacceptReason)){
            unacceptReasons.put(unhandledUnacceptReason, 1);
        }else{
            unacceptReasons.put(unhandledUnacceptReason, unacceptReasons.get(unhandledUnacceptReason)+1);
        }
    }

    @SuppressWarnings("unused")  //used by MethodMapper
    private static TaxonBase<?> testTitleCache(ResultSet rs, ErmsImportState state) throws SQLException{
        TaxonBase<?> taxon = (TaxonBase<?>)state.getRelatedObject(DbImportStateBase.CURRENT_OBJECT_NAMESPACE, DbImportStateBase.CURRENT_OBJECT_ID);
        TaxonName taxonName = taxon.getName();
        String displayName = rs.getString("tu_displayname");
        displayName = displayName == null ? null : displayName.trim();
        String titleCache = taxonName.resetTitleCache(); //calling titleCache should always be kept to have a computed titleCache in the CDM DB.
        titleCache = CdmUtils.concat(" ", titleCache, taxon.getAppendedPhrase());
        String expectedTitleCache = getExpectedTitleCache(rs);
        //TODO check titleCache, but beware of autonyms
        if (!titleCache.equals(expectedTitleCache) && !isErroneousSubgenus(taxonName, displayName)){
            if (onlyAutonymDiffers(titleCache, expectedTitleCache, taxonName)){
                //TODO do
            }else {
                int pos = CdmUtils.diffIndex(titleCache, expectedTitleCache);
                logger.warn("Computed title cache differs at "+pos+".\n Computed             : " + titleCache + "\n DisplayName+Authority: " + expectedTitleCache);
                taxonName.setNameCache(displayName, true);
            }
        }
        return taxon;
    }

    /**
     * Checks if 2 name strings represent autonyms and differ only in the way where the author is put
     */
    private static boolean onlyAutonymDiffers(String titleCache, String expectedTitleCache, TaxonName taxonName) {
        if (! taxonName.isAutonym()) {
            return false;
        }else {
            List<String> computed = Arrays.asList(titleCache.split(" "));
            computed.sort(new StringComparator());
            List<String> expected = Arrays.asList(expectedTitleCache.split(" "));
            expected.sort(new StringComparator());
            return computed.equals(expected);

//          boolean sameWords = WordUtils.containsAllWords(expectedTitleCache, titleCache.split(" "));  //throws exceptions due to brackets
        }
    }

    //see also PesiErmsValidation.srcFullName()
    private static String getExpectedTitleCache(ResultSet srcRs) throws SQLException {
        String result;
        String epi = srcRs.getString("tu_name");
        epi = " a" + epi;
        String display = srcRs.getString("tu_displayname");
        String sp = srcRs.getString("tu_sp");
        if (display.indexOf(epi) != display.lastIndexOf(epi) && !sp.startsWith("#2#")){ //homonym, animal
            result = srcRs.getString("tu_displayname").replaceFirst(epi+" ", CdmUtils.concat(" ", " "+epi, srcRs.getString("tu_authority")))+" ";
        }else{
            result = CdmUtils.concat(" ", srcRs.getString("tu_displayname"), srcRs.getString("tu_authority"));
        }
        return result.trim();
    }

    private void handleNotAcceptedTaxonStatus(Taxon taxon, int statusId, boolean idsDiffer, ErmsImportState state, ResultSet rs) throws SQLException {
		ExtensionType pesiStatusType = getExtensionType(state, ErmsTransformer.uuidPesiTaxonStatus, "PESI taxon status", "PESI taxon status", "status", null);

		if(idsDiffer){
		    //if ids differ the taxon should always be an ordinary synonym, some synonyms need to be imported to CDM as Taxon because they have factual data attached, they use a concept relationship as synonym relationship
		    addPesiStatus(taxon, PesiTransformer.T_STATUS_SYNONYM, pesiStatusType);
		}else if(statusId == 1){
            //nothing to do, not expected to happen
		}else if (statusId > 1 && statusId < 6 || statusId == 7){ //unaccepted, nomen nudum, alternate representation, temporary name       they have sometimes no tu_acctaxon or are handled incorrect
		    //TODO discuss alternate representations, at the very end of the PESI export unaccepted taxa with relationship "is alternative name for" are set to status "accepted". Need to check if this is true for the PESI taxa too (do they have such a relationship?)
		    //Note: in SQL script, also the tu_unacceptreason was checked to be NOT LIKE '%syno%', this is not always correct and the few real synonyms should better data cleaned
		    addPesiStatus(taxon, PesiTransformer.T_STATUS_UNACCEPTED, pesiStatusType);
        }else if (statusId == 6 || statusId == 8 || statusId == 10){
            taxon.setDoubtful(true);  //nomen dubium, taxon inquirendum, uncertain
        }else if (statusId == 9){
            addPesiStatus(taxon, PesiTransformer.T_STATUS_UNACCEPTED, pesiStatusType);         //interim unpublished, we should better not yet publish, but will be probably accepted in future
        }else if (statusId == 11 || statusId == 12){
            addPesiStatus(taxon, PesiTransformer.T_STATUS_SYNONYM, pesiStatusType);         //superseded combination and junior synonym, see #10683 description
        }else if (statusId == 21){
            //added for PESI 2025
            addPesiStatus(taxon, PesiTransformer.T_STATUS_UNACCEPTED, pesiStatusType);   //unavailable name, Not compliant with the relevant ICZN or ICN code
        }else if (statusId == 22){
            //added for PESI 2025
            addPesiStatus(taxon, PesiTransformer.T_STATUS_UNACCEPTED, pesiStatusType);   //unassessed, Name coming from nomenclator (lists of names) or from, e.g., museum collection database, where the status might be known in the published literature, but for which the status has not yet been researched, assessed and documented
        }else if (statusId == 23){
            //added for PESI 2025, mapping still to be discussed
            addPesiStatus(taxon, PesiTransformer.T_STATUS_UNACCEPTED, pesiStatusType);  //unreplaced junior homonym, To indicate the unreplaced invalid status of the junior, or later established name, or in the case of simultaneous establishment, the name not given precedence by a first reviser or by an ICZN or ICN ruling
        }else{
            logger.error("Unhandled statusId "+ statusId);
        }
	}

    private void addPesiStatus(Taxon taxon, int status, ExtensionType pesiStatusType) {
        taxon.addExtension(String.valueOf(status), pesiStatusType);

    }

    private void handleException(Integer parentRank, TaxonName taxonName, String displayName, Integer meId) {
		logger.warn("Parent of infra specific taxon is of higher rank ("+parentRank+") than species. Used nameCache: " + displayName +  "; id=" + meId) ;
		taxonName.setNameCache(displayName);
	}

	private boolean containsBrackets(String displayName) {
		int index = displayName.indexOf("[");
		return (index > -1);
	}

	private void getGenusAndInfraGenus(String parentName, String grandParentName, Integer parent1Rank, TaxonName taxonName) {
		if (parent1Rank <220 && parent1Rank > 180){
			//parent is infrageneric
			taxonName.setInfraGenericEpithet(parentName);
			taxonName.setGenusOrUninomial(grandParentName);
		}else{
			taxonName.setGenusOrUninomial(parentName);
		}
	}

	/**
	 * Returns an empty Taxon Name instance according to the given rank and kingdom.
	 */
	private TaxonName getTaxonName(ResultSet rs, ErmsImportState state) throws SQLException {
	    TaxonName result;
		int kingdomId = parseKingdomId(rs);
		Integer intRank = rs.getInt("tu_rank");

		NomenclaturalCode nc = ErmsTransformer.kingdomId2NomCode(kingdomId);
		Rank rank = null;
		rank = state.getRank(intRank, kingdomId);

		if (rank == null){
			logger.warn("Rank is null. KingdomId: " + kingdomId + ", rankId: " +  intRank);
		}
		if (nc != null){
			result = nc.getNewTaxonNameInstance(rank);
		}else{
			result = TaxonNameFactory.NewNonViralInstance(rank);
		}
		//cache strategy
		if (result.isZoological()){
		    TaxonNameDefaultCacheStrategy cacheStrategy = PesiTaxonExport.zooNameStrategy;
			result.setCacheStrategy(cacheStrategy);
		}

		return result;
	}

	/**
	 * Returns the kingdom id by extracting it from the second character in the <code>tu_sp</code>
	 * attribute. If the attribute can not be parsed to a valid id <code>null</code>
	 * is returned. If the attribute is <code>null</code> the id of the record is returned.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private int parseKingdomId(ResultSet rs) throws SQLException {
		String treeString = rs.getString("tu_sp");
		if (treeString != null){
		    if (StringUtils.isNotBlank(treeString) && treeString.length() > 1){
				String strKingdom = treeString.substring(1,2);

				if (! treeString.substring(0, 1).equals("#") && ! treeString.substring(2, 3).equals("#") ){
					String message = "Tree string " + treeString + " has no recognized format";
                    logger.warn(message);
                    throw new RuntimeException(message);
				}else{
					try {
						return Integer.valueOf(strKingdom);
					} catch (NumberFormatException e) {
					    String message = "Kingdom string " + strKingdom + "could not be recognized as a valid number";
						logger.warn(message);
						throw new RuntimeException(message);
					}
				}
			}else{
                String message = "Tree string for kingdom recognition is to short: " + treeString;
                logger.warn(message);
                throw new RuntimeException(message);
			}
		}else{
			int tu_id = rs.getInt("id");
			return tu_id;
		}
	}

    private void logUnacceptReasons() {
        String logStr = "\n Unhandled unaccept reasons:\n===================";

        while (!unacceptReasons.isEmpty()) {
            int n = 0;
            List<String> mostUsedStrings = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : unacceptReasons.entrySet()) {
                if (entry.getValue() > n) {
                    mostUsedStrings = new ArrayList<>();
                    mostUsedStrings.add(entry.getKey());
                    n = entry.getValue();
                } else if (entry.getValue() == n) {
                    mostUsedStrings.add(entry.getKey());
                } else {
                    //neglect
                }
            }
            mostUsedStrings.sort(new StringComparator());
            logStr += "\n   " + String.valueOf(n);
            for (String str : mostUsedStrings) {
                logStr += "\n   "+ str;
                unacceptReasons.remove(str);
            }
        }
        logger.warn(logStr);

    }

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsTaxonImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}
