/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_ARTICLE;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_BOOK;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_CONFERENCE_PROCEEDINGS;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_DATABASE;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_INFORMAL;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_JOURNAL;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_JOURNAL_VOLUME;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_PART_OF_OTHER_TITLE;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_PRINT_SERIES;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_UNKNOWN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.REF_WEBSITE;
import static eu.etaxonomy.cdm.io.common.ImportHelper.NO_OVERWRITE;
import static eu.etaxonomy.cdm.io.common.ImportHelper.OBLIGATORY;
import static eu.etaxonomy.cdm.io.common.ImportHelper.OVERWRITE;

import java.net.URISyntaxException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelReferenceImportValidator;
import eu.etaxonomy.cdm.io.common.ICdmImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.CdmAttributeMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.CdmIoMapping;
import eu.etaxonomy.cdm.io.common.mapping.CdmSingleAttributeMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMarkerMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbSingleAttributeImportMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.berlinModel.CdmOneToManyMapper;
import eu.etaxonomy.cdm.io.common.mapping.berlinModel.CdmStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.berlinModel.CdmUriMapper;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Identifier;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.reference.IArticle;
import eu.etaxonomy.cdm.model.reference.IBookSection;
import eu.etaxonomy.cdm.model.reference.IPrintSeries;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.cache.agent.PersonDefaultCacheStrategy;
import eu.etaxonomy.cdm.strategy.cache.agent.TeamDefaultCacheStrategy;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelReferenceImport extends BerlinModelImportBase {
    private static final long serialVersionUID = -3667566958769967591L;

    private static final Logger logger = Logger.getLogger(BerlinModelReferenceImport.class);

	public static final String REFERENCE_NAMESPACE = "Reference";
	private static final String REF_AUTHOR_NAMESPACE = "Reference.refAuthorString";

	public static final UUID REF_DEPOSITED_AT_UUID = UUID.fromString("23ca88c7-ce73-41b2-8ca3-2cb22f013beb");
	public static final UUID REF_SOURCE_UUID = UUID.fromString("d6432582-2216-4b08-b0db-76f6c1013141");
	public static final UUID DATE_STRING_UUID = UUID.fromString("e4130eae-606e-4b0c-be4f-e93dc161be7d");
	public static final UUID IS_PAPER_UUID = UUID.fromString("8a326129-d0d0-4f9d-bbdf-8d86b037c65e");

	private final int modCount = 1000;
	private static final String pluralString = "references";
	private static final String dbTableName = "reference";

	public BerlinModelReferenceImport(){
		super(dbTableName, pluralString);
	}

	protected void initializeMappers(BerlinModelImportState state){
		for (CdmAttributeMapperBase mapper: classMappers){
			if (mapper instanceof DbSingleAttributeImportMapperBase){
				@SuppressWarnings("unchecked")
                DbSingleAttributeImportMapperBase<BerlinModelImportState,Reference> singleMapper =
				        (DbSingleAttributeImportMapperBase<BerlinModelImportState,Reference>)mapper;
				singleMapper.initialize(state, Reference.class);
			}
		}
		return;
	}

	private Set<Integer> commonNameRefSet = null;
	private void initializeCommonNameRefMap(BerlinModelImportState state) throws SQLException{
	    if (state.getConfig().isEuroMed()){
	        commonNameRefSet = new HashSet<>();
	        String queryStr = "SELECT DISTINCT RefFk "
	                + " FROM emCommonName ";
	        ResultSet rs = state.getConfig().getSource().getResultSet(queryStr);
	        while (rs.next()){
	            commonNameRefSet.add(rs.getInt("RefFk"));
	        }
	    }
	}

	protected static CdmAttributeMapperBase[] classMappers = new CdmAttributeMapperBase[]{
		new CdmStringMapper("edition", "edition"),
		new CdmStringMapper("volume", "volume"),
		new CdmStringMapper("publisher", "publisher"),
		new CdmStringMapper("publicationTown", "placePublished"),
		new CdmStringMapper("isbn", "isbn"),
		new CdmStringMapper("isbn", "isbn"),
		new CdmStringMapper("pageString", "pages"),
		new CdmStringMapper("series", "seriesPart"),
		new CdmStringMapper("issn", "issn"),
		new CdmUriMapper("url", "uri"),
		DbImportExtensionMapper.NewInstance("NomStandard", ExtensionType.NOMENCLATURAL_STANDARD()),
		DbImportExtensionMapper.NewInstance("DateString", DATE_STRING_UUID, "Date String", "Date String", "dates"),
		DbImportExtensionMapper.NewInstance("RefDepositedAt", REF_DEPOSITED_AT_UUID, "Ref. deposited at", "Reference is deposited at", "at"),
		DbImportExtensionMapper.NewInstance("RefSource", REF_SOURCE_UUID, "RefSource", "Reference Source", "source"),
		DbImportMarkerMapper.NewInstance("isPaper", IS_PAPER_UUID, "is paper", "is paper", "paper", false)
		//not yet supported by model
        ,new CdmStringMapper("refAuthorString", "refAuthorString"),
	};

	protected static String[] operationalAttributes = new String[]{
		"refId", "refCache", "nomRefCache", "preliminaryFlag", "inRefFk", "title", "nomTitleAbbrev",
		"refAuthorString", "nomAuthorTeamFk",
		"refCategoryFk", "thesisFlag", "informalRefCategory", "idInSource"
	};

	protected static String[] createdAndNotesAttributes = new String[]{
			"created_When", "updated_When", "created_Who", "updated_Who", "notes"
	};

	protected static String[] unclearMappers = new String[]{
			/*"isPaper",*/ "exportDate",
	};

	//TODO isPaper
	//

	//type to count the references nomReferences that have been created and saved
	private class RefCounter{
		RefCounter() {refCount = 0;}
		int refCount;
		int dedupCount;

		@Override
        public String toString(){return String.valueOf(refCount) + "/" + String.valueOf(dedupCount) ;}
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		return null;  //not needed
	}

	@Override
	protected void doInvoke(BerlinModelImportState state){
		logger.info("start make " + getPluralString() + " ...");

		boolean success = true;
		initializeMappers(state);
		try {
            initializeCommonNameRefMap(state);
        } catch (SQLException e1) {
            e1.printStackTrace();
            logger.error("Error in initializeCommonNameRefMap in BerlinModelReferenceimport");
        }
		BerlinModelImportConfigurator config = state.getConfig();
		Source source = config.getSource();

		String strSelectId = " SELECT Reference.RefId as refId ";
		String strSelectFull =
			" SELECT Reference.* ,InReference.RefCategoryFk as InRefCategoryFk, RefSource.RefSource " ;
		String strFrom =
		        " FROM %s  " +
		    	    " LEFT OUTER JOIN Reference as InReference ON InReference.refId = Reference.inRefFk " +
		    	    " LEFT OUTER JOIN RefSource ON Reference.RefSourceFk = RefSource.RefSourceId " +
		    	" WHERE (1=1) ";
		String strOrderBy = " ORDER BY InReference.inRefFk, Reference.inRefFk "; //to make in-references available in first run
		String strWherePartitioned = " AND (Reference.refId IN ("+ ID_LIST_TOKEN + ") ) ";

		String referenceTable = CdmUtils.Nz(state.getConfig().getReferenceIdTable());
		referenceTable = referenceTable.isEmpty() ? " Reference"  : referenceTable + " as Reference ";
		String strIdFrom = String.format(strFrom, referenceTable );

		String referenceFilter = CdmUtils.Nz(state.getConfig().getReferenceIdTable());
		if (! referenceFilter.isEmpty()){
			referenceFilter = " AND " + referenceFilter + " ";
		}
		referenceFilter = "";  //don't use it for now, in E+M the tabelle is directly used

		String strIdQueryFirstPath = strSelectId + strIdFrom + strOrderBy ;
		String strIdQuerySecondPath = strSelectId + strIdFrom + " AND (Reference.InRefFk is NOT NULL) ";

//		if (config.getDoReferences() == CONCEPT_REFERENCES){
//			strIdQueryNoInRef += " AND ( Reference.refId IN ( SELECT ptRefFk FROM PTaxon) ) " + referenceFilter;
//		}

		String strRecordQuery = strSelectFull + String.format(strFrom, " Reference ") + strWherePartitioned + strOrderBy;

		int recordsPerTransaction = config.getRecordsPerTransaction();
		try{
			//firstPath
			ResultSetPartitioner<BerlinModelImportState> partitioner =
			        ResultSetPartitioner.NewInstance(source, strIdQueryFirstPath, strRecordQuery, recordsPerTransaction);
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
			logger.info("end make references without in-references ... " + getSuccessString(success));
			state.setReferenceSecondPath(true);

			//secondPath
//			partitioner = ResultSetPartitioner.NewInstance(source, strIdQuerySecondPath, strRecordQuery, recordsPerTransaction);
//			while (partitioner.nextPartition()){
//			    //currently not used as inRef assignment fully works through sorting of idQuery now, at least in E+M
//				partitioner.doPartition(this, state);
//			}
//			logger.info("end make references with no 1 in-reference ... " + getSuccessString(success));
			state.setReferenceSecondPath(false);
			logger.warn("Parsed book volumes: " + parsedBookVolumes);
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
	        return;
		}
		logger.info("end make " + getPluralString() + " ... " + getSuccessString(success));
		if (! success){
			state.setUnsuccessfull();
		}
		return;
	}

    @Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
        state.getDeduplicationHelper().restartSession();

	    if (state.isReferenceSecondPath()){
			return doPartitionSecondPath(partitioner, state);
		}
		boolean success = true;

		Map<Integer, Reference> refToSave = new HashMap<>();

		BerlinModelImportConfigurator config = state.getConfig();

		try {

			int i = 0;
			RefCounter refCounter  = new RefCounter();
			ResultSet rs = partitioner.getResultSet();

			//for each resultset
			while (rs.next()){
				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("References handled: " + (i-1) + " in round -" );}

				success &= makeSingleReferenceRecord(rs, state, partitioner, refToSave, refCounter);
			} // end resultSet

			//for the concept reference a fixed uuid may be needed -> change uuid
			Integer sourceSecId = (Integer)config.getSourceSecId();
			Reference sec = refToSave.get(sourceSecId);

			if (sec != null){
				sec.setUuid(config.getSecUuid());
				logger.info("SecUuid changed to: " + config.getSecUuid());
			}

			//save and store in map
			logger.warn("Save references (" + refCounter.toString() + ")");  //set preliminary to warn for printing dedup count

			getReferenceService().saveOrUpdate(refToSave.values());

//			logger.info("end makeReferences ..." + getSuccessString(success));;
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}



	/**
	 * Adds the inReference to the according references.
	 * @param partitioner
	 * @param state
	 * @return
	 */
	private boolean doPartitionSecondPath(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true;

		Map<Integer, Reference> refToSave = new HashMap<>();

		@SuppressWarnings("unchecked")
        Map<String, Reference> relatedReferencesMap = partitioner.getObjectMap(REFERENCE_NAMESPACE);

		try {
				int i = 0;
				RefCounter refCounter  = new RefCounter();

				ResultSet rs = partitioner.getResultSet();
				//for each resultset
				while (rs.next()){
					if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("References handled: " + (i-1) + " in round -" );}

					Integer refId = rs.getInt("refId");
					Integer inRefFk = nullSafeInt(rs, "inRefFk");

					if (inRefFk != null){

						Reference thisRef = relatedReferencesMap.get(String.valueOf(refId));

						Reference inRef = relatedReferencesMap.get(String.valueOf(inRefFk));

						if (thisRef != null){
							if (inRef == null){
								logger.warn("No InRef found for nomRef: " + thisRef.getTitleCache() + "; RefId: " +  refId + "; inRefFK: " +  inRefFk);
							}
							thisRef.setInReference(inRef);
							refToSave.put(refId, thisRef);
							if(!thisRef.isProtectedTitleCache()){
							    thisRef.setTitleCache(null);
							    thisRef.getTitleCache();
							}
						}else{
						    logger.warn("Reference which has an inReference not found in DB. RefId: " + refId);
						}
						if(inRefFk.equals(0)){
						    logger.warn("InRefFk is 0 for refId "+ refId);
						}
					}

				} // end resultSet

				//save and store in map
				logger.info("Save in references (" + refCounter.toString() + ")");
				getReferenceService().saveOrUpdate(refToSave.values());

//			}//end resultSetList

//			logger.info("end makeReferences ..." + getSuccessString(success));;
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;

		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> teamIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> teamStringSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, teamIdSet, "NomAuthorTeamFk");
				handleForeignKey(rs, referenceIdSet, "InRefFk");
				handleForeignKey(rs, teamStringSet, "refAuthorString");
				//TODO only needed in second path but state not available here to check if state is second path
				handleForeignKey(rs, referenceIdSet, "refId");
			}

			Set<String> teamStringSet2 = new HashSet<>();
			for (String teamString : teamStringSet){
			    teamStringSet2.add(teamString.replace("'", "´"));
			}

			//team map
			nameSpace = BerlinModelAuthorTeamImport.NAMESPACE;
			idSet = teamIdSet;
            @SuppressWarnings("rawtypes")
            Map<String, TeamOrPersonBase> teamMap = getCommonService().getSourcedObjectsByIdInSourceC(TeamOrPersonBase.class, idSet, nameSpace);
			result.put(nameSpace, teamMap);

            //refAuthor map
            nameSpace = REF_AUTHOR_NAMESPACE;
            idSet = teamStringSet2;
            @SuppressWarnings("unchecked")
            Map<String, TeamOrPersonBase> refAuthorMap = getCommonService().getSourcedObjectsByIdInSourceC(TeamOrPersonBase.class, idSet, nameSpace);
            result.put(nameSpace, refAuthorMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	/**
	 * Handles a single reference record
	 * @param rs
	 * @param state
	 * @param biblioRefToSave
	 * @param nomRefToSave
	 * @param relatedBiblioReferences
	 * @param relatedNomReferences
	 * @param refCounter
	 * @return
	 */
	private boolean makeSingleReferenceRecord(
				ResultSet rs,
				BerlinModelImportState state,
				ResultSetPartitioner<BerlinModelImportState> partitioner,
				Map<Integer, Reference> refToSave,
				RefCounter refCounter){

	    boolean success = true;

		Integer refId = null;
		try {
			Map<String, Object> valueMap = getValueMap(rs);

			Integer categoryFk = (Integer)valueMap.get("refCategoryFk".toLowerCase());
			refId = (Integer)valueMap.get("refId".toLowerCase());
			Boolean thesisFlag = (Boolean)valueMap.get("thesisFlag".toLowerCase());


			Reference reference;
			logger.debug("RefCategoryFk: " + categoryFk);

			if (thesisFlag){
				reference = makeThesis(valueMap);
			}else if (categoryFk == REF_JOURNAL){
				reference = makeJournal(valueMap);
			}else if(categoryFk == REF_BOOK){
				reference = makeBook(valueMap);
			}else if(categoryFk == REF_DATABASE){
				reference = makeDatabase(valueMap);
			}else if(categoryFk == REF_INFORMAL){
				reference = makeInformal(valueMap);
			}else if(categoryFk == REF_WEBSITE){
				reference = makeWebSite(valueMap);
			}else if(categoryFk == REF_UNKNOWN){
				reference = makeUnknown(valueMap);
			}else if(categoryFk == REF_PRINT_SERIES){
				reference = makePrintSeries(valueMap);
			}else if(categoryFk == REF_CONFERENCE_PROCEEDINGS){
				reference = makeProceedings(valueMap);
			}else if(categoryFk == REF_ARTICLE){
				reference = makeArticle(valueMap);
			}else if(categoryFk == REF_JOURNAL_VOLUME){
				reference = makeJournalVolume(valueMap);
			}else if(categoryFk == REF_PART_OF_OTHER_TITLE){
				reference = makePartOfOtherTitle(valueMap);
			}else{
				logger.warn("Unknown categoryFk (" + categoryFk + "). Create 'Generic instead'");
				reference = ReferenceFactory.newGeneric();
				success = false;
			}

			//refYear
			String refYear = (String)valueMap.get("refYear".toLowerCase());
			reference.setDatePublished(ImportHelper.getDatePublished(refYear));

			handleEdition(reference);

			//created, updated, notes
			doCreatedUpdatedNotes(state, reference, rs);

			//idInSource (import from older source to berlin model)
			//TODO do we want this being imported? Maybe as alternatvie identifier?
			String idInSource = (String)valueMap.get("IdInSource".toLowerCase());
			if (isNotBlank(idInSource)){
				if(!state.getConfig().isDoSourceNumber()){
				    IdentifiableSource source = IdentifiableSource.NewDataImportInstance(idInSource);
				    source.setIdNamespace("import to Berlin Model");
				    reference.addSource(source);
				}else{
				    makeSourceNumbers(state, idInSource, reference, refId);
				}
			}
            String uuid = null;
            if (resultSetHasColumn(rs,"UUID")){
                uuid = rs.getString("UUID");
                if (uuid != null){
                    reference.setUuid(UUID.fromString(uuid));
                }
            }

			//nom&BiblioReference  - must be last because a clone is created
			success &= makeNomAndBiblioReference(rs, state, partitioner, refId, reference, refCounter, refToSave);


		} catch (Exception e) {
			logger.warn("Reference with BM refId '" + CdmUtils.Nz(refId) +  "' threw Exception and could not be saved");
			e.printStackTrace();
			success = false;
		}
		return success;
	}


	/**
     * @param state
     * @param idInSource
     * @param reference
     * @param refId
     */
    private void makeSourceNumbers(BerlinModelImportState state, String idInSource, Reference reference,
            Integer refId) {
        String[] splits = idInSource.split("\\|");
        for (String split : splits){
            split = split.trim();
            UUID uuid = BerlinModelTransformer.uuidEMReferenceSourceNumber;
            TermVocabulary<DefinedTerm> voc = null;  //user defined voc
            DefinedTerm type = getIdentiferType(state, uuid, "E+M Reference Source Number", "Euro+Med Reference Source Number", "E+M Source Number", voc);
            Identifier.NewInstance(reference, split, type);
        }
    }

    /**
     * @param reference
     */
    private void handleEdition(Reference reference) {
        if (reference.getEdition()!= null && reference.getEdition().startsWith("ed. ")){
            reference.setEdition(reference.getEdition().substring(4));
        }

    }

    /**
	 * Creates and saves a nom. reference and a biblio. reference after checking necessity
	 * @param rs
	 * @param refId
	 * @param ref
	 * @param refCounter
	 * @param biblioRefToSave
	 * @param nomRefToSave
	 * @param teamMap
	 * @param stores
	 * @return
	 * @throws SQLException
	 */
	private boolean makeNomAndBiblioReference(
				ResultSet rs,
				BerlinModelImportState state,
				@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
				int refId,
				Reference ref,
				RefCounter refCounter,
				Map<Integer, Reference> refToSave
			) throws SQLException{

		@SuppressWarnings("unchecked")
        Map<String, Team> teamMap = partitioner.getObjectMap(BerlinModelAuthorTeamImport.NAMESPACE);

		String refCache = trim(rs.getString("refCache"));
		String nomRefCache = trim(rs.getString("nomRefCache"));
		String title = trim(rs.getString("title"));
		String nomTitleAbbrev = trim(rs.getString("nomTitleAbbrev"));
		boolean isPreliminary = rs.getBoolean("PreliminaryFlag");
		String refAuthorString = trim(rs.getString("refAuthorString"));
		Integer nomAuthorTeamFk = nullSafeInt(rs, "NomAuthorTeamFk");
		Integer inRefFk = nullSafeInt(rs, "inRefFk");


		TeamOrPersonBase<?> nomAuthor = null;
		if (nomAuthorTeamFk != null){
		    String strNomAuthorTeamFk = String.valueOf(nomAuthorTeamFk);
		    nomAuthor = teamMap.get(strNomAuthorTeamFk);
		    if (nomAuthor == null){
		        logger.warn("NomAuthor ("+strNomAuthorTeamFk+") not found in teamMap (but it should exist) for " + refId);
		    }
		}

		Reference sourceReference = state.getTransactionalSourceReference();

		//preliminary
		if (isPreliminary){
			ref.setAbbrevTitleCache(nomRefCache, true);
			ref.setTitleCache(refCache, true);
		}

		//title/abbrevTitle
		if (isNotBlank(nomTitleAbbrev)){
			ref.setAbbrevTitle(nomTitleAbbrev);
		}
		if (isNotBlank(title)){
			ref.setTitle(title);
		}

		//author
		TeamOrPersonBase<?> author = getAuthorship(state, refAuthorString, nomAuthor, refId);
		ref.setAuthorship(author);

		if (ref.getType().equals(ReferenceType.Book)){
		    extraktBookVolume(ref);
		}

		//inRef
		Reference inRef = null;
		if (inRefFk != null){
		    @SuppressWarnings({"unchecked" })
		    Map<String, Reference>  relatedReferences = partitioner.getObjectMap(REFERENCE_NAMESPACE);
		    inRef = relatedReferences.get(String.valueOf(inRefFk));
		    if (inRef == null){
		        inRef = refToSave.get(inRefFk);
		    }
		    if (inRef == null){
		        logger.warn("InRef not (yet) found. RefId: " + refId + "; InRef: "+ inRefFk);
		    }else{
		        ref.setInReference(inRef);
		    }
		}

		Reference result = deduplicateReference(state, ref);
		if(ref != result){
		    //dedup not possible at this point because inRef exists but is not yet defined
		    if (inRefFk != null && inRef == null){
		        result = ref;
		        logger.warn("Ref has deduplication candidate but inRef is still missing. " + inRef);
		    }else{
		        logger.debug("Reference was deduplicated. RefId: " + refId);
		        //FIXME also check annotations etc. for deduplication
		        refCounter.dedupCount++;
		    }
		}else{
		    refCounter.refCount++;
		}

		//save
		if (! refToSave.containsKey(refId)){
			refToSave.put(refId, result);
		}else{
		    //should not happen
			logger.warn("Duplicate refId in Berlin Model database. Second reference was not imported !!");
		}


		//refId
		ImportHelper.setOriginalSource(result, sourceReference, refId, REFERENCE_NAMESPACE);

		if (commonNameRefSet != null && commonNameRefSet.contains(refId)){
		    result.addMarker(Marker.NewInstance(MarkerType.COMMON_NAME_REFERENCE(), true));
        }

		return true;
	}

	/**
     * @param string
     * @return
     */
    private String trim(String string) {
        if (string == null){
            return null;
        }else{
            return string.trim();
        }
    }

    /**
	 * Copies the created and updated information from the nomReference to the cloned bibliographic reference
	 * @param referenceBase
	 * @param nomReference
	 */
	private void copyCreatedUpdated(Reference biblioReference, Reference nomReference) {
		biblioReference.setCreatedBy(nomReference.getCreatedBy());
		biblioReference.setCreated(nomReference.getCreated());
		biblioReference.setUpdatedBy(nomReference.getUpdatedBy());
		biblioReference.setUpdated(nomReference.getUpdated());

	}

	private Reference makeArticle (Map<String, Object> valueMap){

		IArticle article = ReferenceFactory.newArticle();
		Object inRefFk = valueMap.get("inRefFk".toLowerCase());
		Integer inRefCategoryFk = (Integer)valueMap.get("inRefCategoryFk".toLowerCase());
		Integer refId = (Integer)valueMap.get("refId".toLowerCase());

		if (inRefFk != null){
			if (inRefCategoryFk != REF_JOURNAL){
				logger.warn("Wrong inrefCategory for Article (refID = " + refId +"). Type must be 'Journal' but was not (RefCategoryFk=" + inRefCategoryFk + "))." +
					" InReference was added anyway! ");
			}
		}else{
			logger.warn ("Article has no inreference: " + refId);
		}
		makeStandardMapper(valueMap, (Reference)article); //url, pages, series, volume
		String url = (String)valueMap.get("url");
		if (url != null && url.contains("dx.doi.org")){
		    article.setDoi(DOI.fromString(url));
		    article.setUri(null);
		}
		return (Reference)article;
	}

	private Reference makePartOfOtherTitle (Map<String, Object> valueMap){

		Reference result;
		Object inRefFk = valueMap.get("inRefFk".toLowerCase());
		Integer inRefCategoryFk = (Integer)valueMap.get("inRefCategoryFk".toLowerCase());
		Integer refId = (Integer)valueMap.get("refId".toLowerCase());

		if (inRefCategoryFk == null){
			//null -> error
			logger.warn("Part-Of-Other-Title has no inRefCategoryFk! RefId = " + refId + ". ReferenceType set to Generic.");
			result = makeUnknown(valueMap);
		}else if (inRefFk == null){
			//TODO is this correct ??
			logger.warn("Part-Of-Other-Title has no in reference: " + refId);
			result = makeUnknown(valueMap);
		}else if (inRefCategoryFk == REF_BOOK){
			//BookSection
			IBookSection bookSection = ReferenceFactory.newBookSection();
			result = (Reference)bookSection;
		}else if (inRefCategoryFk == REF_ARTICLE){
			//Article
			logger.info("Reference (refId = " + refId + ") of type 'part_of_other_title' is part of 'article'." +
					" We use the section reference type for such in references now.") ;
			result = ReferenceFactory.newSection();
		}else if (inRefCategoryFk == REF_JOURNAL){
			//TODO
			logger.warn("Reference (refId = " + refId + ") of type 'part_of_other_title' has inReference of type 'journal'." +
					" This is not allowed! Generic reference created instead") ;
			result = ReferenceFactory.newGeneric();
			result.addMarker(Marker.NewInstance(MarkerType.TO_BE_CHECKED(), true));
		}else if (inRefCategoryFk == REF_PART_OF_OTHER_TITLE){
			logger.info("Reference (refId = " + refId + ") of type 'part_of_other_title' has inReference 'part of other title'." +
					" This is allowed, but may be true only for specific cases (e.g. parts of book chapters). You may want to check if this is correct") ;
			result = ReferenceFactory.newSection();
		}else{
			logger.warn("InReference type (catFk = " + inRefCategoryFk + ") of part-of-reference not recognized for refId " + refId + "." +
				" Create 'Generic' reference instead");
			result = ReferenceFactory.newGeneric();
		}
		makeStandardMapper(valueMap, result); //url, pages
		return result;
	}


	/**
	 * @param inRefFkInt
	 * @param biblioRefToSave
	 * @param nomRefToSave
	 * @param relatedBiblioReferences
	 * @param relatedNomReferences
	 * @return
	 */
	private boolean existsInMapOrToSave(Integer inRefFkInt, Map<Integer, Reference> refToSave, Map<String, Reference> relatedReferences) {
		boolean result = false;
		if (inRefFkInt == null){
			return false;
		}
		result |= refToSave.containsKey(inRefFkInt);
		result |= relatedReferences.containsKey(String.valueOf(inRefFkInt));
		return result;
	}

	private Reference makeWebSite(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Website'");}
		Reference webPage = ReferenceFactory.newWebPage();
		makeStandardMapper(valueMap, webPage); //placePublished, publisher
		return webPage;
	}

	private Reference makeUnknown(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Unknown'");}
		Reference generic = ReferenceFactory.newGeneric();
//		generic.setSeries(series);
		makeStandardMapper(valueMap, generic); //pages, placePublished, publisher, series, volume
		return generic;
	}

	private Reference makeInformal(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Informal'");}
		Reference generic = ReferenceFactory.newGeneric();
//		informal.setSeries(series);
		makeStandardMapper(valueMap, generic);//editor, pages, placePublished, publisher, series, volume
		String informal = (String)valueMap.get("InformalRefCategory".toLowerCase());
		if (isNotBlank(informal) ){
			generic.addExtension(informal, ExtensionType.INFORMAL_CATEGORY());
		}
		return generic;
	}

	private Reference makeDatabase(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Database'");}
		Reference database =  ReferenceFactory.newDatabase();
		makeStandardMapper(valueMap, database); //?
		return database;
	}

	private Reference makeJournal(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Journal'");}
		Reference journal = ReferenceFactory.newJournal();

		Set<String> omitAttributes = new HashSet<>();
		String series = "series";
//		omitAttributes.add(series);

		makeStandardMapper(valueMap, journal, omitAttributes); //issn,placePublished,publisher
//		if (valueMap.get(series) != null){
//			logger.warn("Series not yet implemented for journal!");
//		}
		return journal;
	}

	private Reference makeBook(
				Map<String, Object> valueMap){

		if (logger.isDebugEnabled()){logger.debug("RefType 'Book'");}
		Reference book = ReferenceFactory.newBook();
//		Integer refId = (Integer)valueMap.get("refId".toLowerCase());

		//Set bookAttributes = new String[]{"edition", "isbn", "pages","publicationTown","publisher","volume"};

		Set<String> omitAttributes = new HashSet<>();
		String attrSeries = "series";
//		omitAttributes.add(attrSeries);

		makeStandardMapper(valueMap, book, omitAttributes);

		//Series (as String)
		IPrintSeries printSeries = null;
		if (valueMap.get(attrSeries) != null){
			String series = (String)valueMap.get("title".toLowerCase());
			if (series == null){
				String nomTitle = (String)valueMap.get("nomTitleAbbrev".toLowerCase());
				series = nomTitle;
			}
			printSeries = ReferenceFactory.newPrintSeries(series);
			logger.info("Implementation of printSeries is preliminary");
		}
		//Series (as Reference)
		if (book.getInSeries() != null && printSeries != null){
			logger.warn("Book has series string and inSeries reference. Can not take both. Series string neglected");
		}else{
			book.setInSeries(printSeries);
		}
		book.setEditor(null);

		return book;

	}


	int parsedBookVolumes = 0;
    private void extraktBookVolume(Reference book) {
        if (isExtractBookVolumeCandidate(book)){
            String patternStr = "(.{2,})\\s(\\d{1,2})";
            int groupIndex = 2;
            Pattern pattern = Pattern.compile(patternStr);

            String abbrevCache = book.getAbbrevTitleCache();
            String titleCache = book.getTitleCache();
            String vol = null;
            String volFull = null;
            String abbrev = book.getAbbrevTitle();
            if (isNotBlank(abbrev)){
                Matcher matcher = pattern.matcher(abbrev);
                if (matcher.matches()){
                    vol = matcher.group(groupIndex);
                    abbrev = matcher.group(1);
                }
            }

            String full = book.getTitle();
            if (isNotBlank(full)){
                Matcher matcher = pattern.matcher(full);
                if (matcher.matches()){
                    volFull = matcher.group(groupIndex);
                    full = matcher.group(1);
                }
            }
            if (vol != null && volFull != null){
                if (!vol.equals(volFull)){
                    return;
                }
            }else if (vol == null && volFull == null){
                return;
            }else if (vol == null){
                if (isNotBlank(abbrev)){
                    return;
                }else{
                    vol = volFull;
                }
            }else if (volFull == null){
                if (isNotBlank(full)){
                    return;
                }
            }else{
                logger.warn("Should not happen");
            }
            book.setVolume(vol);
            book.setAbbrevTitle(abbrev);
            book.setTitle(full);
            if (!book.getAbbrevTitleCache().equals(abbrevCache)){
                logger.warn("Abbrev title cache for parsed book volume does not match: " + book.getAbbrevTitleCache() + " <-> "+abbrevCache);
            }else if (!book.getTitleCache().equals(titleCache)){
                logger.warn("Title cache for parsed book volume does not match: " + book.getTitleCache() + " <-> "+titleCache);
            }else{
//                System.out.println(titleCache);
//                System.out.println(abbrevCache);
                parsedBookVolumes++;
            }
        }else{
            return;
        }
    }

    /**
     * @param book
     * @return
     */
    private boolean isExtractBookVolumeCandidate(Reference book) {
        if (isNotBlank(book.getVolume()) || isNotBlank(book.getEdition()) || isNotBlank(book.getSeriesPart())){
            return false;
        }
        if (!checkExtractBookVolumeTitle(book.getAbbrevTitle())){
            return false;
        }
        if (!checkExtractBookVolumeTitle(book.getTitle())){
            return false;
        }
        return true;
    }

    /**
     * @param abbrevTitle
     * @return
     */
    private boolean checkExtractBookVolumeTitle(String title) {
        if (title == null){
            return true;
        }
        if (title.contains(",") || title.contains("ed.") || title.contains("Ed.")|| title.contains("Suppl")
                || title.contains("Ser.")|| title.contains("ser.")) {
            return false;
        }
        return true;
    }

    /**
	 * Returns the requested object if it exists in one of both maps. Prefers the refToSaveMap in ambigious cases.
	 * @param inRefFkInt
	 * @param nomRefToSave
	 * @param relatedNomReferences
	 * @return
	 */
	private Reference getReferenceFromMaps(
			int inRefFkInt,
			Map<Integer, Reference> refToSaveMap,
			Map<String, Reference> relatedRefMap) {
		Reference result = null;
		result = refToSaveMap.get(inRefFkInt);
		if (result == null){
			result = relatedRefMap.get(String.valueOf(inRefFkInt));
		}
		return result;
	}

	private Reference makePrintSeries(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'PrintSeries'");}
		Reference printSeries = ReferenceFactory.newPrintSeries();
		makeStandardMapper(valueMap, printSeries, null);
		return printSeries;
	}

	private Reference makeProceedings(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Proceedings'");}
		Reference proceedings = ReferenceFactory.newProceedings();
		makeStandardMapper(valueMap, proceedings, null);
		return proceedings;
	}

	private Reference makeThesis(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'Thesis'");}
		Reference thesis = ReferenceFactory.newThesis();
		makeStandardMapper(valueMap, thesis, null);
		return thesis;
	}


	private Reference makeJournalVolume(Map<String, Object> valueMap){
		if (logger.isDebugEnabled()){logger.debug("RefType 'JournalVolume'");}
		//Proceedings proceedings = Proceedings.NewInstance();
		Reference journalVolume = ReferenceFactory.newGeneric();
		makeStandardMapper(valueMap, journalVolume, null);
		logger.warn("Journal volumes not yet implemented. Generic created instead but with errors");
		return journalVolume;
	}

	private boolean makeStandardMapper(Map<String, Object> valueMap, Reference ref){
		return makeStandardMapper(valueMap, ref, null);
	}


	private boolean makeStandardMapper(Map<String, Object> valueMap, CdmBase cdmBase, Set<String> omitAttributes){
		boolean result = true;
		for (CdmAttributeMapperBase mapper : classMappers){
			if (mapper instanceof CdmSingleAttributeMapperBase){
				result &= makeStandardSingleMapper(valueMap, cdmBase, (CdmSingleAttributeMapperBase)mapper, omitAttributes);
			}else if (mapper instanceof CdmOneToManyMapper){
				result &= makeMultipleValueAddMapper(valueMap, cdmBase, (CdmOneToManyMapper)mapper, omitAttributes);
			}else{
				logger.error("Unknown mapper type");
				result = false;
			}
		}
		return result;
	}

	private boolean makeStandardSingleMapper(Map<String, Object> valueMap, CdmBase cdmBase, CdmSingleAttributeMapperBase mapper, Set<String> omitAttributes){
		boolean result = true;
		if (omitAttributes == null){
			omitAttributes = new HashSet<>();
		}
		if (mapper instanceof DbImportExtensionMapper){
			result &= ((DbImportExtensionMapper)mapper).invoke(valueMap, cdmBase);
		}else if (mapper instanceof DbImportMarkerMapper){
			result &= ((DbImportMarkerMapper)mapper).invoke(valueMap, cdmBase);
		}else{
			String sourceAttribute = mapper.getSourceAttributeList().get(0).toLowerCase();
			Object value = valueMap.get(sourceAttribute);
			if (mapper instanceof CdmUriMapper && value != null){
				try {
					value = new URI (value.toString());
				} catch (URISyntaxException e) {
					logger.error("URI syntax exception: " + value.toString());
					value = null;
				}
			}
			if (value != null){
				String destinationAttribute = mapper.getDestinationAttribute();
				if (! omitAttributes.contains(destinationAttribute)){
					result &= ImportHelper.addValue(value, cdmBase, destinationAttribute, mapper.getTypeClass(), OVERWRITE, OBLIGATORY);
				}
			}
		}
		return result;
	}


	private boolean makeMultipleValueAddMapper(Map<String, Object> valueMap, CdmBase cdmBase, CdmOneToManyMapper<CdmBase, CdmBase, CdmSingleAttributeMapperBase> mapper, Set<String> omitAttributes){
		if (omitAttributes == null){
			omitAttributes = new HashSet<>();
		}
		boolean result = true;
		String destinationAttribute = mapper.getSingleAttributeName();
		List<Object> sourceValues = new ArrayList<>();
		List<Class> classes = new ArrayList<>();
		for (CdmSingleAttributeMapperBase singleMapper : mapper.getSingleMappers()){
			String sourceAttribute = singleMapper.getSourceAttribute();
			Object value = valueMap.get(sourceAttribute);
			sourceValues.add(value);
			Class<?> clazz = singleMapper.getTypeClass();
			classes.add(clazz);
		}

		result &= ImportHelper.addMultipleValues(sourceValues, cdmBase, destinationAttribute, classes, NO_OVERWRITE, OBLIGATORY);
		return result;
	}


	private TeamOrPersonBase<?> getAuthorship(BerlinModelImportState state, String refAuthorString,
	        TeamOrPersonBase<?> nomAuthor, Integer refId){

	    TeamOrPersonBase<?> result;
		if (nomAuthor != null){
			result = nomAuthor;
			if (isNotBlank(refAuthorString) && !nomAuthor.getTitleCache().equals(refAuthorString)){
			    boolean isSimilar = handleSimilarAuthors(state, refAuthorString, nomAuthor, refId);
			    if (! isSimilar){
			        String message = "refAuthorString differs from nomAuthor.titleCache: " + refAuthorString
                            + " <-> " + nomAuthor.getTitleCache() + "; RefId: " + refId;
			        logger.warn(message);
			    }
			}
		} else if (isNotBlank(refAuthorString)){//only RefAuthorString exists
		    refAuthorString = refAuthorString.trim();
			//TODO match with existing Persons/Teams
		    TeamOrPersonBase<?> author = state.getRelatedObject(REF_AUTHOR_NAMESPACE, refAuthorString, TeamOrPersonBase.class);
			if (author == null){
			    if (!BerlinModelAuthorTeamImport.hasTeamSeparator(refAuthorString)){
			        author = makePerson(refAuthorString, false, refId);
			    }else{
			        author = makeTeam(state, refAuthorString, refId);
			    }
			    state.addRelatedObject(REF_AUTHOR_NAMESPACE, refAuthorString, author);
			    result = deduplicatePersonOrTeam(state, author);

			    if (result != author){
                    logger.debug("RefAuthorString author deduplicated " + author);
                }else{
                    if (!importSourceExists(author, refAuthorString, REF_AUTHOR_NAMESPACE, state.getTransactionalSourceReference() )){
                        author.addImportSource(refAuthorString, REF_AUTHOR_NAMESPACE, state.getTransactionalSourceReference(), null);
                    }
                }
			}else{
			    logger.debug("RefAuthor loaded from map");
			}
			result = author;
		}else{
			result = null;
		}

		return result;
	}


    /**
     * @param state
     * @param refAuthorString
     * @param refId
     * @return
     */
    private TeamOrPersonBase<?> makeTeam(BerlinModelImportState state, String refAuthorString, Integer refId) {
        Team team = Team.NewInstance();
        boolean hasDedupMember = false;
        if (containsEdOrColon(refAuthorString)){
            team.setTitleCache(refAuthorString, true);
        }else{
            String[] refAuthorTeams = BerlinModelAuthorTeamImport.splitTeam(refAuthorString);
            boolean lastWasInitials = false;
            for (int i = 0; i< refAuthorTeams.length ;i++){
                if (lastWasInitials){
                    lastWasInitials = false;
                    continue;
                }
                String fullTeam = refAuthorTeams[i].trim();
                String initials = null;
                if (refAuthorTeams.length > i+1){
                    String nextSplit = refAuthorTeams[i+1].trim();
                    if (isInitial(nextSplit)){
                        lastWasInitials = true;
                        initials = nextSplit;
                    }
                }
                Person member = makePerson(fullTeam, isNotBlank(initials), refId);

                if (initials != null){
                    if (member.getInitials() != null){
                        logger.warn("Initials already set: " + refId);
                    }else if (!member.isProtectedTitleCache()){
                        member.setInitials(initials);
                    }else {
                        member.setTitleCache(member.getTitleCache() + ", " + initials, true);
                    }
                }

                if (i == refAuthorTeams.length -1 && BerlinModelAuthorTeamImport.isEtAl(member)){
                    team.setHasMoreMembers(true);
                }else{
                    Person dedupMember = deduplicatePersonOrTeam(state, member);
                    if (dedupMember != member){
                        hasDedupMember = true;
                    }else{
                        if (!importSourceExists(member, refAuthorString, REF_AUTHOR_NAMESPACE, state.getTransactionalSourceReference())){
                            member.addImportSource(refAuthorString, REF_AUTHOR_NAMESPACE, state.getTransactionalSourceReference(), null);
                        }
                    }

                    team.addTeamMember(dedupMember);
                }
            }
        }

        TeamOrPersonBase<?> result = team;
        if (team.getTeamMembers().size() == 1 && !team.isHasMoreMembers()){
            Person person = team.getTeamMembers().get(0);
            checkPerson(person, refAuthorString, hasDedupMember, refId);
            result = person;
        }else{
            checkTeam(team, refAuthorString, refId);
            result = team;
        }

        return result;
    }

    private static void checkTeam(Team team, String refAuthorString, Integer refId) {
        TeamDefaultCacheStrategy formatter = (TeamDefaultCacheStrategy) team.getCacheStrategy();

        if (formatter.getTitleCache(team).equals(refAuthorString)){
            team.setProtectedTitleCache(false);
        }else if(formatter.getTitleCache(team).replace(" & ", ", ").equals(refAuthorString.replace(" & ", ", ").replace(" ,", ","))){
            //also accept teams with ', ' as final member separator as not protected
            team.setProtectedTitleCache(false);
        }else if(formatter.getFullTitle(team).replace(" & ", ", ").equals(refAuthorString.replace(" & ", ", "))){
            //.. or teams with initials first
            team.setProtectedTitleCache(false);
        }else if (containsEdOrColon(refAuthorString)){
            //nothing to do, it is expected to be protected

        }else{
            team.setTitleCache(refAuthorString, true);
            logger.warn("Creation of titleCache for team with members did not (fully) work: " + refAuthorString + " <-> " + formatter.getTitleCache(team)+ " : " + refId);
        }
    }

    private static void checkPerson(Person person, String refAuthorString, boolean hasDedupMember, Integer refId) {
        PersonDefaultCacheStrategy formatter = (PersonDefaultCacheStrategy) person.getCacheStrategy();

        String oldTitleCache = person.getTitleCache();
        boolean oldTitleCacheProtected = person.isProtectedTitleCache();

        if (! oldTitleCache.equals(refAuthorString)){
            logger.error("Old titleCache does not equal refAuthorString this should not happen. "+ oldTitleCache + " <-> " + refAuthorString + "; refId = " + refId);
        }

        boolean protect = true;
        person.setProtectedTitleCache(false);
        if (refAuthorString.equals(formatter.getTitleCache(person))){
            protect = false;
        }else if(formatter.getFullTitle(person).equals(refAuthorString)){
            //.. or teams with initials first
            protect = false;
        }else{
            //keep protected, see below
        }

        if (hasDedupMember){
            //restore
            //TODO maybe even do not use dedup for testing
            person.setTitleCache(oldTitleCache, oldTitleCacheProtected);
            if (protect != oldTitleCacheProtected){
                logger.warn("Deduplicated person protection requirement unclear for "+refAuthorString+". New:"+protect+"/Old:"+oldTitleCacheProtected+"; RefId: " + refId);
            }
        }else{
            if (protect){
                logger.warn("Creation of titleCache for person (converted from team) with members did not (fully) work: " + refAuthorString + " <-> " + formatter.getTitleCache(person)+ " : " + refId);
                person.setTitleCache(refAuthorString, protect);
            }else{
                //keep unprotected
            }
        }
    }

    private static boolean containsEdOrColon(String str) {
        if (str.contains(" ed.") || str.contains(" Ed.") || str.contains("(ed.")
                || str.contains("[ed.") || str.contains("(Eds)") || str.contains("(Eds.)") ||
                str.contains("(eds.)") || str.contains(":")|| str.contains(";") || str.contains("Publ. & Inform. Directorate")
                || str.contains("Anonymous [Department of Botany, Faculty of Science, FER-ZPR, University of Zagreb]")
                || str.contains("Davis, P. H. (Güner, A. & al.)")){
            return true;
        }else{
            return false;
        }
    }

    /**
     * @param nextSplit
     * @return
     */
    private static boolean isInitial(String str) {
        if (str == null){
            return false;
        }
        boolean matches = str.trim().matches("(\\p{javaUpperCase}|Yu|Ya|Th|Ch|Lj|Sz|Dz|Sh|Ju|R. M. da S)\\.?"
                + "(\\s*[-\\s]\\s*(\\p{javaUpperCase}|Yu|Ja|Kh|Tz|Ya|Th|Ju)\\.?)*(\\s+(van|von|de|de la|del|da|van der))?");
        return matches;
    }

    private <T extends TeamOrPersonBase<?>> T deduplicatePersonOrTeam(BerlinModelImportState state,T author) {
        T result = state.getDeduplicationHelper().getExistingAuthor(author);
        return result;
    }

    private Reference deduplicateReference(BerlinModelImportState state,Reference ref) {
        Reference result = state.getDeduplicationHelper().getExistingReference(ref);
        return result;
    }

    private static Person makePerson(String full, boolean followedByInitial, Integer refId) {
        Person person = Person.NewInstance();
        person.setTitleCache(full, true);
        if (!full.matches(".*[\\s\\.].*")){
            person.setFamilyName(full);
            person.setProtectedTitleCache(false);
        }else{
            parsePerson(person, full, true, followedByInitial);
        }

        if ((full.length() <= 2 && !full.matches("(Li|Bo|Em|Ay|Ma)")) || (full.length() == 3 && full.endsWith(".") && !full.equals("al.")) ){
            logger.warn("Unexpected short nom author name part: " + full + "; " + refId);
        }

        return person;
    }

    private static void parsePerson(Person person, String str, boolean preliminary, boolean followedByInitial) {
        String capWord = "\\p{javaUpperCase}\\p{javaLowerCase}{2,}";
        String famStart = "(Le |D'|'t |Mc|Mac|Des |d'|Du |De |Al-)";
        String regEx = "((\\p{javaUpperCase}|Ya|Th|Ju|Kh|An)\\.([\\s-]\\p{javaUpperCase}\\.)*(\\s(de|del|da|von|van|van der|v.|af|zu|von M. Und L.))?\\s)("
                + famStart + "?" + capWord + "((-| y | i | é | de | de la )" + capWord + ")?)";
        Matcher matcher = Pattern.compile(regEx).matcher(str);
        if (matcher.matches()){
            person.setProtectedTitleCache(false);
            String familyName = matcher.group(6).trim();
            person.setFamilyName(familyName);
            person.setInitials(matcher.group(1).trim());
        }else{
            String regEx2 = "("+ capWord + "\\s" + capWord + "|Le Sueur|Beck von Mannagetta|Di Martino|Galán de Mera|Van Der Maesen|Farga i Arquimbau|Perez de Paz|Borzatti de Loewenstern|Lo Giudice|Perez de Paz)";
            Matcher matcher2 = Pattern.compile(regEx2).matcher(str);
            if (followedByInitial && matcher2.matches()){
                person.setFamilyName(str);
                person.setProtectedTitleCache(false);
            }else{
                person.setTitleCache(str, preliminary);
            }
        }
    }

    private static boolean handleSimilarAuthors(BerlinModelImportState state, String refAuthorString,
            TeamOrPersonBase<?> nomAuthor, int refId) {
        String nomTitle = nomAuthor.getTitleCache();

        if (refAuthorString.equals(nomAuthor.getNomenclaturalTitleCache())){
            //nomTitle equal
            return true;
        }else{
            if (refAuthorString.replace(" & ", ", ").equals(nomTitle.replace(" & ", ", "))){
                //nomTitle equal except for "&"
                return true;
            }
            String nomFullTitle = nomAuthor.getFullTitle();
            if (refAuthorString.replace(" & ", ", ").equals(nomFullTitle.replace(" & ", ", "))){
                return true;
            }

            if (nomAuthor.isInstanceOf(Person.class)){
                Person person = CdmBase.deproxy(nomAuthor, Person.class);

                //refAuthor has initials behind, nom Author in front // the other way round is handled in firstIsFullNameOfInitialName
                if (refAuthorString.contains(",") && !nomTitle.contains(",") ){
                    String[] splits = refAuthorString.split(",");
                    if (splits.length == 2){
                        String newMatch = splits[1].trim() + " " + splits[0].trim();
                        if (newMatch.equals(nomTitle)){
                            if (isBlank(person.getFamilyName())){
                                person.setFamilyName(splits[0].trim());
                            }
                            if (isBlank(person.getInitials())){
                                person.setInitials(splits[1].trim());
                            }
                            return true;
                        }
                    }
                }

                if (refAuthorIsFamilyAuthorOfNomAuthor(state, refAuthorString, person)){
                    return true;
                }

                if (firstIsFullNameOfInitialName(state, refAuthorString, person, refId)){
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * @param state
     * @param refAuthorString
     * @param person
     * @return
     */
    private static boolean refAuthorIsFamilyAuthorOfNomAuthor(BerlinModelImportState state, String refAuthorString,
            Person person) {
        if (refAuthorString.equals(person.getFamilyName())){
            return true;
        }else{
            return false;
        }
    }

    /**
     * @param state
     * @param refAuthorString
     * @param nomAuthor
     * @return
     */
    private static boolean firstIsFullNameOfInitialName(BerlinModelImportState state, String fullName,
            Person initialAuthor, int refId) {
        String initialName = initialAuthor.getTitleCache();

        String[] fullSplits = fullName.split(",");
        String[] initialSplits = initialName.split(",");

        if (fullSplits.length == 2 && initialSplits.length == 2){
            String[] fullGivenName = fullSplits[1].trim().split(" ");
            String[] initialsGivenName = initialSplits[1].trim().split(" ");
            boolean result = compareFamilyAndInitials(fullSplits[0], initialSplits[0], fullGivenName, initialsGivenName);
            if (result){
                setGivenName(state, fullSplits[1], initialAuthor, refId);
            }
            return result;
        }else if (fullSplits.length == 1 && initialSplits.length == 2){
            String[] fullSingleSplits = fullName.split(" ");
            String fullFamily = fullSingleSplits[fullSingleSplits.length-1];
            String[] fullGivenName = Arrays.copyOfRange(fullSingleSplits, 0, fullSingleSplits.length-1);
            String[] initialsGivenName = initialSplits[1].trim().split(" ");
            boolean result =  compareFamilyAndInitials(fullFamily, initialSplits[0], fullGivenName, initialsGivenName);
            if (result){
                if(hasAtLeastOneFullName(fullGivenName)){
                    setGivenName(state, CdmUtils.concat(" ", fullGivenName), initialAuthor, refId);
                }
            }
            return result;
        }else if (fullSplits.length == 1 && initialAuthor.getInitials() == null){
            //don't if this will be implemented, initialAuthors with only nomencl.Author set
        }

        return false;
    }

    /**
     * @param fullGivenName
     * @return
     */
    private static boolean hasAtLeastOneFullName(String[] fullGivenName) {
        for (String singleName : fullGivenName){
            if (!singleName.endsWith(".") && singleName.length() > 2 && !singleName.matches("(von|van)") ){
                return true;
            }
        }
        return false;
    }

    private static void setGivenName(BerlinModelImportState state, String givenName, Person person, int refId) {
        givenName = givenName.trim();
        if(person.getGivenName() == null || person.getGivenName().equals(givenName)){
            person.setGivenName(givenName);
        }else{
            logger.warn("RefAuthor given name and existing given name differ: " + givenName + " <-> " + person.getGivenName() + "; RefId + " + refId);
        }
    }

    protected static boolean compareFamilyAndInitials(String fullFamilyName, String initialsFamilyName,
            String[] fullGivenName, String[] initialsGivenName) {
        if (!fullFamilyName.equals(initialsFamilyName)){
            return false;
        }
        if (fullGivenName.length == initialsGivenName.length){
            for (int i =0; i< fullGivenName.length ; i++){
                if (fullGivenName[i].length() == 0  //comma ending not allowed
                        || initialsGivenName[i].length() != 2 //only K. or similar allowed
                        || fullGivenName[i].length() < initialsGivenName[i].length()  //fullFirstName must be longer than abbrev Name
                        || !initialsGivenName[i].endsWith(".") //initials must end with "."
                        || !fullGivenName[i].startsWith(initialsGivenName[i].replace(".", ""))){ //start with same letter
                    if (fullGivenName[i].matches("(von|van|de|zu)") && fullGivenName[i].equals(initialsGivenName[i])){
                        continue;
                    }else{
                        return false;
                    }
                }
            }
            return true;
        }else{
            return false;
        }
    }

	public Set<String> getObligatoryAttributes(boolean lowerCase, BerlinModelImportConfigurator config){
		Set<String> result = new HashSet<>();
		Class<ICdmImport>[] ioClassList = config.getIoClassList();
		result.addAll(Arrays.asList(unclearMappers));
		result.addAll(Arrays.asList(createdAndNotesAttributes));
		result.addAll(Arrays.asList(operationalAttributes));
		CdmIoMapping mapping = new CdmIoMapping();
		for (CdmAttributeMapperBase mapper : classMappers){
			mapping.addMapper(mapper);
		}
		result.addAll(mapping.getSourceAttributes());
		if (lowerCase){
			Set<String> lowerCaseResult = new HashSet<>();
			for (String str : result){
				if (str != null){lowerCaseResult.add(str.toLowerCase());}
			}
			result = lowerCaseResult;
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		BerlinModelReferenceImportValidator validator = new BerlinModelReferenceImportValidator();
		return validator.validate(state, this);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return (state.getConfig().getDoReferences() == IImportConfigurator.DO_REFERENCES.NONE);
	}

}
