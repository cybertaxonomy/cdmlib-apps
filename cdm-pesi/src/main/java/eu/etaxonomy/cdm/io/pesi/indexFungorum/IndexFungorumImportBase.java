/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.lsid.MalformedLSIDException;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.LSID;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.strategy.exceptions.StringNotParsableException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 27.02.2012
 */
public abstract class IndexFungorumImportBase
        extends CdmImportBase<IndexFungorumImportConfigurator, IndexFungorumImportState>
        implements ICdmIO<IndexFungorumImportState>, IPartitionedIO<IndexFungorumImportState> {

    private static final long serialVersionUID = -729872543287390949L;
    private static Logger logger = LogManager.getLogger();

	//NAMESPACES
	protected static final String NAMESPACE_REFERENCE = "reference";
	protected static final String NAMESPACE_TAXON = "Taxon";
	protected static final String NAMESPACE_SUPRAGENERIC_NAMES = "SupragenericNames";
	protected static final String NAMESPACE_GENERA = "Genera";
	protected static final String NAMESPACE_SPECIES = "Species";

	protected static final String INCERTAE_SEDIS = "Incertae sedis";
	protected static final String FOSSIL_FUNGI = "Fossil Fungi";

	protected static final String SOURCE_REFERENCE = "SOURCE_REFERENCE";

	private final String pluralString;
	private final String dbTableName;


	public IndexFungorumImportBase(String pluralString, String dbTableName) {
		this.pluralString = pluralString;
		this.dbTableName = dbTableName;
	}

	@Override
    protected void doInvoke(IndexFungorumImportState state){
		System.out.println("start make " + getPluralString() + " ...");
		IndexFungorumImportConfigurator config = state.getConfig();
		Source source = config.getSource();

		String strIdQuery = getIdQuery();
		String strRecordQuery = getRecordQuery(config);

		int recordsPerTransaction = config.getRecordsPerTransaction();
		try{
			ResultSetPartitioner<IndexFungorumImportState> partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
			return;
		}

		logger.info("end make " + getPluralString() + " ... " + getSuccessString(true));
		return;
	}

    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, IndexFungorumImportState state) {
        //should be overridden by subclasses if used
		return true;
	}

	protected abstract String getRecordQuery(IndexFungorumImportConfigurator config);

	protected String getIdQuery(){
		String result = " SELECT id FROM " + getTableName();
		return result;
	}

	@Override
    public String getPluralString(){
		return pluralString;
	}

	protected String getTableName(){
		return this.dbTableName;
	}

	protected boolean resultSetHasColumn(ResultSet rs, String columnName){
		try {
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 0; i < metaData.getColumnCount(); i++){
				if (metaData.getColumnName(i + 1).equalsIgnoreCase(columnName)){
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
            logger.warn("Exception in resultSetHasColumn");
            return false;
		}
	}

	protected boolean checkSqlServerColumnExists(Source source, String tableName, String columnName){
		String strQuery = "SELECT  Count(t.id) as n " +
				" FROM sysobjects AS t " +
				" INNER JOIN syscolumns AS c ON t.id = c.id " +
				" WHERE (t.xtype = 'U') AND " +
				" (t.name = '" + tableName + "') AND " +
				" (c.name = '" + columnName + "')";
		ResultSet rs = source.getResultSet(strQuery) ;
		int n;
		try {
			rs.next();
			n = rs.getInt("n");
			return n>0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns a map that holds all values of a ResultSet. This is needed if a value needs to
	 * be accessed twice
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected Map<String, Object> getValueMap(ResultSet rs) throws SQLException{
		try{
			Map<String, Object> valueMap = new HashMap<>();
			int colCount = rs.getMetaData().getColumnCount();
			for (int c = 0; c < colCount ; c++){
				Object value = rs.getObject(c+1);
				String label = rs.getMetaData().getColumnLabel(c+1).toLowerCase();
				if (value != null && ! CdmUtils.Nz(value.toString()).trim().equals("")){
					valueMap.put(label, value);
				}
			}
			return valueMap;
		}catch(SQLException e){
			throw e;
		}
	}

	/**
	 * Reads a foreign key field from the result set and adds its value to the idSet.
	 * @param rs
	 * @param teamIdSet
	 * @throws SQLException
	 */
	protected void handleForeignKey(ResultSet rs, Set<String> idSet, String attributeName)
			throws SQLException {
		Object idObj = rs.getObject(attributeName);
		if (idObj != null){
			String id  = String.valueOf(idObj);
			idSet.add(id);
		}
	}

	/**
	 * Returns true if i is a multiple of recordsPerTransaction
	 */
	protected boolean loopNeedsHandling(int i, int recordsPerLoop) {
		startTransaction();
		return (i % recordsPerLoop) == 0;
	}

	protected void doLogPerLoop(int count, int recordsPerLog, String pluralString){
		if ((count % recordsPerLog ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
	}

	protected void makeAuthorAndPublication(IndexFungorumImportState state, ResultSet rs, INonViralName name) throws SQLException {
		//authors
		NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
		String authorStr = rs.getString("AUTHORS");
		if (isNotBlank(authorStr)){
		    if (authorStr.endsWith("ined.")){
		        name.addStatus(NomenclaturalStatusType.INED(), null, null);
		        authorStr = authorStr.substring(0, authorStr.length()-5).trim();
		    }
		}
	    if (isNotBlank(authorStr)){
	        try {
                parser.parseAuthors(name, authorStr);
            } catch (StringNotParsableException e){
                logger.warn("Authorstring not parsable: " + authorStr);
                name.setAuthorshipCache(authorStr);
            }
	    }

		//page
		String page = rs.getString("PAGE");
		if (StringUtils.isNotBlank(page)){
			name.setNomenclaturalMicroReference(page);
		}

		//Reference
		Reference ref = ReferenceFactory.newGeneric();
		boolean hasInReference = false;
		//publishing authors
		Team pubAuthor = null;
		String pubAuthorStr = rs.getString("PUBLISHING AUTHORS");
		if (StringUtils.isNotBlank(pubAuthorStr)){
			if (StringUtils.isNotBlank(authorStr)){
				if (! pubAuthorStr.equals(authorStr)){
					pubAuthor = Team.NewTitledInstance(pubAuthorStr, pubAuthorStr);
				}
			}else{
				logger.warn("'AUTHORS' is blank for not empty PUBLISHING_AUTHORS. This is not yet handled.");
			}
		}

		//inRef + inRefAuthor
		if (pubAuthor != null){
			Reference inRef = ReferenceFactory.newGeneric();
			inRef.setAuthorship(pubAuthor);
			ref.setInReference(inRef);
			hasInReference = true;
		}

		//refAuthor
		TeamOrPersonBase<?> refAuthor = CdmBase.deproxy(name.getCombinationAuthorship());
		if (refAuthor == null){
			refAuthor = Team.NewTitledInstance(authorStr, authorStr);
		}
		ref.setAuthorship(refAuthor);
		//location
		String location = rs.getString("pubIMIAbbrLoc");
		if (StringUtils.isNotBlank(location)){
			if (hasInReference){
				ref.getInReference().setPlacePublished(location);
			}else{
				ref.setPlacePublished(location);
			}
		}
		//title
		String titleMain = rs.getString("pubIMIAbbr");
		String supTitle = rs.getString("pubIMISupAbbr");
		String title = CdmUtils.concat(", ", titleMain, supTitle);
		//preliminary to comply with current Index Fungorum display
		if (StringUtils.isNotBlank(location)){
			title += " (" + location +")";
		}
		//end preliminary
		if (StringUtils.isNotBlank(title)){
			if (hasInReference){
				ref.getInReference().setTitle(title);
			}else{
				ref.setTitle(title);
			}
		}
		//Volume
		String volume = CdmUtils.Nz(rs.getString("VOLUME")).trim();
		String part = rs.getString("PART");
		if (StringUtils.isNotBlank(part)){
			volume = volume + "(" + part + ")";
			if (StringUtils.isBlank(volume)){
				logger.warn("'Part' is not blank for blank volume. This may be an inconsistency.");
			}
		}
		ref.setVolume(volume);

		//year
		String yearOfPubl = rs.getString("YEAR OF PUBLICATION");
		String yearOnPubl = rs.getString("YEAR ON PUBLICATION");  //verbatim year
		VerbatimTimePeriod datePublished = null;
		if (isNotBlank(yearOfPubl)){
			datePublished = TimePeriodParser.parseStringVerbatim(yearOfPubl.trim());
		}
		if (isNotBlank(yearOnPubl)){
		    if (datePublished == null){
		        logger.warn("Verbatim year without real year not yet supported");
		    }else{
		        datePublished.setVerbatimDate(yearOnPubl.trim());
		    }
		}
		if (datePublished != null){
			ref.setDatePublished(datePublished);
		}

		//TODO preliminary, set protected titlecache as generic cache generation with in-references currently doesn't fully work yet
		String titleCache = CdmUtils.concat(", ", pubAuthorStr, title);
		if  (isNotBlank(pubAuthorStr)){
			titleCache = "in " + titleCache;
		}
		titleCache = CdmUtils.concat(" ", titleCache, volume);
		titleCache = CdmUtils.concat(": ", titleCache, page);
		titleCache = CdmUtils.concat(". ", titleCache, datePublished == null ? null : datePublished.toString());
		ref.setTitleCache(titleCache, true);

		//set nom ref
		if (StringUtils.isNotBlank(titleCache)){
			name.setNomenclaturalReference(ref);
		}
	}

	protected MarkerType getNoLastActionMarkerType(IndexFungorumImportState state) {
		return getMarkerType(state, DbLastActionMapper.uuidMarkerTypeHasNoLastAction,
				"has no last action", "No last action information available", "no last action");
	}

	protected void makeSource(IndexFungorumImportState state, IdentifiableEntity<?> entity, Integer id, String namespace) {
		//source reference
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		//source
		String strId = (id == null ? null : String.valueOf(id));
		IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, strId, namespace, sourceReference, null);
		entity.addSource(source);

		//no last action
		MarkerType hasNoLastAction = getNoLastActionMarkerType(state);
		entity.addMarker(Marker.NewInstance(hasNoLastAction, true));
		//LSID
		makeLSID(entity, strId, state);
	}

	private void makeLSID(IdentifiableEntity<?> entity, String strId, IndexFungorumImportState state) {
		try {
			if (StringUtils.isNotBlank(strId) &&  strId != null){
				LSID lsid = new LSID(IndexFungorumTransformer.LSID_PREFIX + strId);
				entity.setLsid(lsid);
			}else{
				logger.warn("No ID available for taxon " + entity.getTitleCache() + ", " +  entity.getUuid());
				MarkerType missingGUID = getMissingGUIDMarkerType(state);
				entity.addMarker(Marker.NewInstance(missingGUID, true));
			}
		} catch (MalformedLSIDException e) {
			logger.error(e.getMessage());
		}
	}

	protected MarkerType getMissingGUIDMarkerType(IndexFungorumImportState state) {
		MarkerType missingGUID = getMarkerType(state, PesiTransformer.uuidMarkerGuidIsMissing, "GUID is missing", "GUID is missing", null);
		return missingGUID;
	}

	protected Classification getClassification(IndexFungorumImportState state) {
		Classification result;
		UUID classificationUuid = state.getTreeUuid(state.getConfig().getSourceReference());
		if (classificationUuid == null){
			Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
			result = makeTreeMemSave(state, sourceReference);
		} else {
			result = getClassificationService().find(classificationUuid);
		}
		return result;
	}
}
