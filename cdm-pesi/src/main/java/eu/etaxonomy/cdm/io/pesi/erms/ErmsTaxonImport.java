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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportLsidMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMarkerMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsTaxonImportValidator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTaxonExport;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
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
    private static final Logger logger = Logger.getLogger(ErmsTaxonImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "taxa";
	private static final String dbTableName = "tu";
	private static final Class<?> cdmTargetClass = TaxonBase.class;

	public ErmsTaxonImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT id FROM tu WHERE id NOT IN (147415) " ;  //for now we exclude Monera as it has no children and is unclear what classification it has. In ERMS it is alternative accepted name, but according to https://en.wikipedia.org/wiki/Monera it is a super taxon to bacteria.
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

			//not yet implemented
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("tu_sp", "included in rank/object creation"));

			//ignore
			mapping.addMapper(DbIgnoreMapper.NewInstance("tu_fossil", "tu_fossil implemented as foreign key"));

		}
		return mapping;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strSelect = " SELECT tu.*, parent1.tu_name AS parent1name, parent2.tu_name AS parent2name, parent3.tu_name AS parent3name, " +
		            " parent1.tu_rank AS parent1rank, parent2.tu_rank AS parent2rank, parent3.tu_rank AS parent3rank, " +
		            " status.status_id as status_id, status.status_name, fossil.fossil_name, qualitystatus.qualitystatus_name," +
		            " s.sessiondate lastActionDate, a.action_name lastAction, s.ExpertName ";
		String strFrom = " FROM tu  LEFT OUTER JOIN  tu AS parent1 ON parent1.id = tu.tu_parent " +
				" LEFT OUTER JOIN   tu AS parent2  ON parent2.id = parent1.tu_parent " +
				" LEFT OUTER JOIN tu AS parent3 ON parent2.tu_parent = parent3.id " +
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
		String parentCol = "tu_parent";
		String accCol = " tu_acctaxon";
		String idCol = " id ";
		String tuFk = "tu_id";
		String taxonTable = "tu";
		String vernacularsTable = "vernaculars";
		String distributionTable = "dr";
		String notesTable = "notes";
		String sql =
		        " SELECT DISTINCT %s FROM %s  "  //fk to parent
		        + " UNION  SELECT %s FROM %s WHERE %s is NULL" //id of taxa not having accepted taxon
		        + " UNION  SELECT DISTINCT %s FROM %s "  //fk to accepted taxon
		        + " UNION  SELECT DISTINCT %s FROM %s " //vernaculars
		        + " UNION  SELECT DISTINCT %s FROM %s "  //distributions
		        + " UNION  SELECT DISTINCT %s FROM %s ";  //notes
		sql = String.format(sql,
				parentCol, taxonTable,
				idCol, taxonTable, accCol,
				accCol, taxonTable,
				tuFk, vernacularsTable,
				tuFk, distributionTable,
				tuFk, notesTable);
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);
		try {
			while (rs.next()){
				Integer id;
				id = rs.getInt(parentCol);
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
//		Object accTaxonId = rs.getObject("tu_acctaxon");
		Integer meId = rs.getInt("id");

		String tuName = rs.getString("tu_name");
		String displayName = rs.getString("tu_displayname");

		String parent1Name = rs.getString("parent1name");
		Integer parent1Rank = rs.getInt("parent1rank");

		String parent2Name = rs.getString("parent2name");
		Integer parent2Rank = rs.getInt("parent2rank");

		String parent3Name = rs.getString("parent3name");
//		Integer parent3Rank = rs.getInt("parent3rank");

		TaxonName taxonName = getTaxonName(rs, state);
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
			taxonName.setSpecificEpithet(parent1Name);
			getGenusAndInfraGenus(parent2Name, parent3Name, parent2Rank, taxonName);
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
		taxonName.getTitleCache();

		//add original source for taxon name (taxon original source is added in mapper
		Reference citation = state.getTransactionalSourceReference();
		addOriginalSource(rs, taxonName, "id", NAME_NAMESPACE, citation);

		//old: if (statusId == 1){
		if (state.getAcceptedTaxaKeys().contains(meId)){
			Taxon result = Taxon.NewInstance(taxonName, citation);
			if (statusId != 1){
				logger.info("Taxon created as taxon but has status <> 1 ("+statusId+"): " + meId);
				handleNotAcceptedTaxon(result, statusId, state, rs);
			}
			return result;
		}else{
			return Synonym.NewInstance(taxonName, citation);
		}
	}

	private void handleNotAcceptedTaxon(Taxon taxon, int statusId, ErmsImportState state, ResultSet rs) throws SQLException {
		ExtensionType notAccExtensionType = getExtensionType(state, ErmsTransformer.uuidErmsTaxonStatus, "ERMS taxon status", "ERMS taxon status", "status", null);
		String statusName = rs.getString("status_name");

		if (statusId > 1){
			taxon.addExtension(statusName, notAccExtensionType);
		}
	}

	private void handleException(Integer parent1Rank, TaxonName taxonName, String displayName, Integer meId) {
		logger.warn("Parent of infra specific taxon is higher than species. Used nameCache: " + displayName +  "; id=" + meId) ;
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
