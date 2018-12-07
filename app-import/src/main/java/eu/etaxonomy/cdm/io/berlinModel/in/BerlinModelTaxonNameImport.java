/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonNameImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.ICultivarPlantName;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.IJournal;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.match.FieldMatcher;
import eu.etaxonomy.cdm.strategy.match.IMatchStrategy;
import eu.etaxonomy.cdm.strategy.match.IMatchStrategyEqual;
import eu.etaxonomy.cdm.strategy.match.MatchException;
import eu.etaxonomy.cdm.strategy.match.MatchMode;
import eu.etaxonomy.cdm.strategy.match.MatchResult;
import eu.etaxonomy.cdm.strategy.match.MatchStrategyFactory;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonNameImport extends BerlinModelImportBase {
    private static final long serialVersionUID = -8860800694286602016L;

    private static final boolean BLANK_TO_NULL = true;

	private static final Logger logger = Logger.getLogger(BerlinModelTaxonNameImport.class);

	public static final String NAMESPACE = "TaxonName";

	   public static final String NAMESPACE_PRELIM = "RefDetail_Preliminary";

	public static final UUID SOURCE_ACC_UUID = UUID.fromString("c3959b4f-d876-4b7a-a739-9260f4cafd1c");

	private static int modCount = 5000;
	private static final String pluralString = "TaxonNames";
	private static final String dbTableName = "Name";


	public BerlinModelTaxonNameImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		if (state.getConfig().getNameIdTable() == null ){
			return super.getIdQuery(state);
		}else{
			return "SELECT nameId FROM " + state.getConfig().getNameIdTable()
	//         + " WHERE nameId = 146109 "
			        ;
		}
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		Source source = config.getSource();

			String facultativCols = "";
			String strFacTable = "RefDetail";
			String strFacColumn = "IdInSource";
			String strColAlias = null;
			if (checkSqlServerColumnExists(source, strFacTable, strFacColumn)){
				facultativCols +=  ", " + strFacTable + "." + strFacColumn ;
				if (! CdmUtils.Nz(strColAlias).equals("") ){
					facultativCols += " AS " + strColAlias;
				}
			}

		String strRecordQuery =
					"SELECT Name.* , RefDetail.RefDetailId, RefDetail.RefFk, " +
                      		" RefDetail.FullRefCache, RefDetail.FullNomRefCache, RefDetail.PreliminaryFlag AS RefDetailPrelim, RefDetail.Details, " +
                      		" RefDetail.SecondarySources, Rank.RankAbbrev, Rank.Rank " +
                      		facultativCols +
                    " FROM Name LEFT OUTER JOIN RefDetail ON Name.NomRefDetailFk = RefDetail.RefDetailId " +
                    	                                   " AND Name.NomRefFk = RefDetail.RefFk " +
                    	" LEFT OUTER JOIN Rank ON Name.RankFk = Rank.rankID " +
                    " WHERE name.nameId IN ("+ID_LIST_TOKEN+") ";
	//	    strRecordQuery += " AND RefDetail.PreliminaryFlag = 1 ";
					//strQuery += " AND Name.Created_When > '03.03.2004' ";
		return strRecordQuery +  "";
	}


	private class ReferenceMapping{
	    public Map<String, ReferenceWrapper> titleMapping = new HashMap<>();
	    public Map<String, ReferenceWrapper> abbrevMapping = new HashMap<>();

	    private class ReferenceWrapper {
	        Set<ReferenceCandidate> candidates = new HashSet<>();

            public Set<ReferenceCandidate> getCandidates() {
                return candidates;
            }
            public void add(Reference ref, String detail) {
                candidates.add(new ReferenceCandidate(ref, detail));
            }
        }
	    private void unload(){
	        titleMapping.clear();
	        abbrevMapping.clear();
	    }

	    public void addCandidate(Reference ref, String detail) {
	        String hash = refHash(ref);
            ReferenceWrapper wrap = abbrevMapping.get(hash);
            if (wrap == null){
                wrap = new ReferenceWrapper();
                abbrevMapping.put(hash, wrap);
            }
            wrap.add(ref, detail);
	    }


        /**
         * @param nomRef
         * @return
         */
        public Set<ReferenceCandidate> getCandidates(Reference exemplar) {
            String hash = refHash(exemplar);
            ReferenceMapping.ReferenceWrapper wrap = abbrevMapping.get(hash);
            if (wrap == null){
                return new HashSet<>();
            }else{
                return wrap.getCandidates();
            }
        }

        @Override
        public String toString(){
            return "ReferenceMapping";
        }
	}

	private ReferenceMapping refMapping = new ReferenceMapping();

	private void loadReferenceMap(BerlinModelImportState state){
	    List<Reference> list = getReferenceService().list(null, null, null, null, null);
	    for (Reference ref : list){
	        refMapping.addCandidate(ref, null);
	    }

//	    try {
//
//            String query = "SELECT * FROM Reference ";
//
//            ResultSet rs = state.getConfig().getDestination().executeQuery(query);
//            while (rs.next()){
//                String title = rs.getString("title");
//                String abbrevTitle = rs.getString("abbrevTitle");
//                int id = rs.getInt("id");
//                UUID uuid = UUID.fromString(rs.getString("uuid"));
//                String titleCache = rs.getString("titleCache");
//                String abbrevTitleCache = rs.getString("abbrevTitleCache");
//                String typeStr = rs.getString("refType");
//                ReferenceType type = ReferenceType.valueOf(typeStr);
//
//                ReferenceMapping.ReferenceWrapper wrapping = refMapping.new ReferenceWrapper(title, id, uuid, titleCache, type) ;
//                refMapping.titleMapping.put(title, wrapping);
//                wrapping = refMapping.new ReferenceWrapper(abbrevTitle, id, uuid, abbrevTitleCache, type) ;
//
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
	}

    private void unloadReferenceMap(){
	    refMapping.unload();
	    refMapping = null;
	}


	@Override
	protected void doInvoke(BerlinModelImportState state) {
	    loadReferenceMap(state);

	    //update rank labels if necessary
		String strAbbrev = state.getConfig().getInfrGenericRankAbbrev();
		Rank rank = Rank.INFRAGENERICTAXON();
		testRankAbbrev(strAbbrev, rank);

		strAbbrev = state.getConfig().getInfrSpecificRankAbbrev();
		rank = Rank.INFRASPECIFICTAXON();
		testRankAbbrev(strAbbrev, rank);

		super.doInvoke(state);
		unloadReferenceMap();
		printMatchResults();
	}

	private void testRankAbbrev(String strAbbrev, Rank rank) {
		if (strAbbrev != null){
			Representation rep = rank.getRepresentation(Language.ENGLISH());
			rep.setAbbreviatedLabel(strAbbrev);
			getTermService().saveOrUpdate(rank);
		}
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		String dbAttrName;
		String cdmAttrName;
		boolean success = true ;
		BerlinModelImportConfigurator config = state.getConfig();
		Set<TaxonName> namesToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, Team> teamMap = partitioner.getObjectMap(BerlinModelAuthorTeamImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {
			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0 && i != 1 ){ logger.info("Names handled: " + (i-1));}

				//create TaxonName element
				int nameId = rs.getInt("nameId");
				Integer authorFk = nullSafeInt(rs, "AuthorTeamFk");
				Integer exAuthorFk = nullSafeInt(rs, "ExAuthorTeamFk");
				Integer basAuthorFk = nullSafeInt(rs, "BasAuthorTeamFk");
				Integer exBasAuthorFk = nullSafeInt(rs, "ExBasAuthorTeamFk");
				String strCultivarGroupName = rs.getString("CultivarGroupName");
				String strCultivarName = rs.getString("CultivarName");
				String nameCache = rs.getString("NameCache");
				String fullNameCache = rs.getString("FullNameCache");
				String uuid = null;
				if (resultSetHasColumn(rs,"UUID")){
					uuid = rs.getString("UUID");
				}

				try {

					//define rank
					boolean useUnknownRank = true;
					Rank rank = BerlinModelTransformer.rankId2Rank(rs, useUnknownRank, config.isSwitchSpeciesGroup());

					boolean allowInfraSpecTaxonRank = state.getConfig().isAllowInfraSpecTaxonRank() ;
					if (rank == null || rank.equals(Rank.UNKNOWN_RANK()) || (rank.equals(Rank.INFRASPECIFICTAXON()) && ! allowInfraSpecTaxonRank)){
						rank = handleProlesAndRaceSublusus(state, rs, rank);
					}

					if (rank.getId() == 0){
						getTermService().save(rank);
						logger.warn("Rank did not yet exist: " +  rank.getTitleCache());
					}

					//create TaxonName
					TaxonName taxonName;
					if (config.getNomenclaturalCode() != null){
						taxonName = config.getNomenclaturalCode().getNewTaxonNameInstance(rank);
						//check cultivar
						if (taxonName.isBotanical()){
							if (isNotBlank(strCultivarGroupName) && isNotBlank(strCultivarName)){
								taxonName = TaxonNameFactory.NewCultivarInstance(rank);
							}
						}
					}else{
						taxonName = TaxonNameFactory.NewNonViralInstance(rank);
					}
					if (uuid != null){
						taxonName.setUuid(UUID.fromString(uuid));
					}

					if (rank == null){
						//TODO rank should never be null or a more sophisticated algorithm has to be implemented for genus/supraGenericName
						logger.warn("Rank is null. Genus epithet was imported. May be wrong");
						success = false;
					}

					//epithets
					if (rank.isSupraGeneric()){
						dbAttrName = "supraGenericName";
					}else{
						dbAttrName = "genus";
					}
					cdmAttrName = "genusOrUninomial";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);

					dbAttrName = "genusSubdivisionEpi";
					cdmAttrName = "infraGenericEpithet";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);

					dbAttrName = "speciesEpi";
					cdmAttrName = "specificEpithet";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);


					dbAttrName = "infraSpeciesEpi";
					cdmAttrName = "infraSpecificEpithet";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);

					dbAttrName = "unnamedNamePhrase";
					cdmAttrName = "appendedPhrase";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);

					//Details
					dbAttrName = "details";
					cdmAttrName = "nomenclaturalMicroReference";
					success &= ImportHelper.addStringValue(rs, taxonName, dbAttrName, cdmAttrName, BLANK_TO_NULL);

                    //authorTeams
                    if (teamMap != null ){
                        taxonName.setCombinationAuthorship(getAuthorTeam(teamMap, authorFk, nameId, config));
                        taxonName.setExCombinationAuthorship(getAuthorTeam(teamMap, exAuthorFk, nameId, config));
                        taxonName.setBasionymAuthorship(getAuthorTeam(teamMap, basAuthorFk, nameId, config));
                        taxonName.setExBasionymAuthorship(getAuthorTeam(teamMap, exBasAuthorFk, nameId, config));
                    }else{
                        logger.warn("TeamMap is null");
                        success = false;
                    }

					//nomRef
					success &= makeNomenclaturalReference(state, taxonName, nameId, rs, partitioner);

					//Source_Acc
					boolean colExists = true;
					try {
						colExists = state.getConfig().getSource().checkColumnExists("Name", "Source_Acc");
					} catch (DatabaseTypeNotSupportedException e) {
						logger.debug("Source does not support 'checkColumnExists'");
					}
					if (colExists){
						String sourceAcc = rs.getString("Source_Acc");
						if (isNotBlank(sourceAcc)){
							ExtensionType sourceAccExtensionType = getExtensionType(state, SOURCE_ACC_UUID, "Source_Acc","Source_Acc","Source_Acc");
							Extension.NewInstance(taxonName, sourceAcc, sourceAccExtensionType);
						}
					}

					//created, notes
					boolean excludeUpdated = true;
					boolean excludeNotes = true;
					success &= doIdCreatedUpdatedNotes(state, taxonName, rs, nameId, NAMESPACE, excludeUpdated, excludeNotes);
					handleNameNotes(state, taxonName, rs, nameId);

					//zoologicalName
					if (taxonName.isZoological()){
						IZoologicalName zooName = taxonName;
						makeZoologialName(rs, zooName, nameId);
					}
					//botanicalName
					else if (taxonName.isBotanical()){
						IBotanicalName botName = taxonName;
						success &= makeBotanicalNamePart(rs, botName) ;

					}

	//				dbAttrName = "preliminaryFlag";
					Boolean preliminaryFlag = rs.getBoolean("PreliminaryFlag");
					Boolean hybridFormulaFlag = rs.getBoolean("HybridFormulaFlag");  //hybrid flag does not lead to cache update in Berlin Model
					if (preliminaryFlag == true || hybridFormulaFlag == true){
						//Computes all caches and sets
						taxonName.setTitleCache(fullNameCache, true);
						taxonName.setFullTitleCache(taxonName.getFullTitleCache(), true);
						taxonName.setNameCache(nameCache, true);
						taxonName.setAuthorshipCache(taxonName.getAuthorshipCache(), true);
					}
					namesToSave.add(taxonName);

				}
				catch (UnknownCdmTypeException e) {
					logger.warn("Name with id " + nameId + " has unknown rankId " + " and could not be saved.");
					success = false;
				}

			} //while rs.hasNext()
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}


//		logger.info( i + " names handled");
		getNameService().save(namesToSave);
//		printMatchResults();
		return success;
	}


	/**
     *
     */
    private void printMatchResults() {
        for (MatchType type : MatchType.values()){
            List<String> list = matchResults.get(type);
            list = list == null? new ArrayList<>(): list;
            Collections.sort(list);
            System.out.println("\n" + type.toString() + " " + list.size());
            System.out.println("=============================");
            for (String result : list){
                System.out.println(result);
            }
        }

    }


    /**
     * @param state
     * @param taxonName
     * @param rs
     * @param nameId
	 * @throws SQLException
     */
    private void handleNameNotes(BerlinModelImportState state, TaxonName taxonName, ResultSet rs, int nameId) throws SQLException {
        String notesOrig = rs.getString("notes");
        String notes = filterNotes(notesOrig, nameId);
        boolean isParentalSpecies = state.getConfig().isEuroMed() && isPostulatedParentalSpeciesNote(notes);
        if (isNotBlank(notes) && taxonName != null && !isParentalSpecies ){
            String notesString = String.valueOf(notes);
            if (notesString.length() > 65530 ){
                notesString = notesString.substring(0, 65530) + "...";
                logger.warn("Notes string is longer than 65530 and was truncated: " + taxonName);
            }
            Annotation notesAnnotation = Annotation.NewInstance(notesString, Language.DEFAULT());
            //notesAnnotation.setAnnotationType(AnnotationType.EDITORIAL());
            //notes.setCommentator(bmiConfig.getCommentator());
            taxonName.addAnnotation(notesAnnotation);
        }

    }

    private static final String MCL = "MCL\\s?[0-9]{1,3}(\\-[0-9]{1,4}(\\-[0-9]{1,4}(\\-[0-9]{1,4}(\\-[0-9]{1,3})?)?)?)?";
    /**
     * @param notes
     */
    protected static String filterNotes(String notes, int nameId) {
        String result;
        if (isBlank(notes)){
            result = null;
        }else if (notes.matches("Acc:.*")){
            if (notes.matches("Acc: .*\\$$") || (notes.matches("Acc: .*"+MCL))){
                result = null;
            }else if (notes.matches("Acc: .*(\\$|"+MCL+")\\s*\\{.*\\}")){
                notes = notes.substring(notes.indexOf("{")+1, notes.length()-1);
                result = notes;
            }else if (notes.matches("Acc: .*(\\$|"+MCL+")\\s*\\[.*\\]")){
                notes = notes.substring(notes.indexOf("[")+1, notes.length()-1);
                result = notes;
            }else{
                logger.warn("Name id: " + nameId + ". Namenote: " + notes);
                result = notes;
            }
        }else if (notes.matches("Syn:.*")){
            if (notes.matches("Syn: .*\\$$") || (notes.matches("Syn: .*"+MCL))){
                result = null;
            }else if (notes.matches("Syn: .*(\\$|"+MCL+")\\s*\\{.*\\}")){
                notes = notes.substring(notes.indexOf("{")+1, notes.length()-1);
                result = notes;
            }else if (notes.matches("Syn: .*(\\$|"+MCL+")\\s*\\[.*\\]")){
                notes = notes.substring(notes.indexOf("[")+1, notes.length()-1);
                result = notes;
            }else{
                logger.warn("Name id: " + nameId + ". Namenote: " + notes);
                result = notes;
            }
        }else{
            result = notes;
        }
        return result;
    }


    /**
     * @param nameNotes
     * @return
     */
    protected static boolean isPostulatedParentalSpeciesNote(String nameNotes) {
        if (nameNotes == null){
            return false;
        }else{
            return nameNotes.matches(".*<>.*");
        }
    }


    private Rank handleProlesAndRaceSublusus(BerlinModelImportState state, ResultSet rs, Rank rank) throws SQLException {
		Rank result;
		String rankAbbrev = rs.getString("RankAbbrev");
//		String rankStr = rs.getString("Rank");
		if (CdmUtils.nullSafeEqual(rankAbbrev, "prol.") ){
			result = Rank.PROLES();
		}else if(CdmUtils.nullSafeEqual(rankAbbrev, "race")){
			result = Rank.RACE();
		}else if(CdmUtils.nullSafeEqual(rankAbbrev, "sublusus")){
			result = Rank.SUBLUSUS();
		}else{
			result = rank;
			logger.warn("Unhandled rank: " + rankAbbrev);
		}
		return result;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;

		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> teamIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> refDetailIdSet = new HashSet<>();
			Set<Integer> prelimRefDetailCandidateIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, teamIdSet, "AuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "ExAuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "BasAuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "ExBasAuthorTeamFk");
				handleForeignKey(rs, referenceIdSet, "nomRefFk");
				handleForeignKey(rs, refDetailIdSet, "nomRefDetailFk");
				prelimRefDetailCandidateIdSet.addAll(getPreliminaryIdCandidates(state, rs));
			}

			//team map
			nameSpace = BerlinModelAuthorTeamImport.NAMESPACE;
			cdmClass = TeamOrPersonBase.class;
			idSet = teamIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TeamOrPersonBase<?>> teamMap = (Map<String, TeamOrPersonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, teamMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

			//refDetail map
			nameSpace = BerlinModelRefDetailImport.REFDETAIL_NAMESPACE;
			cdmClass = Reference.class;
			idSet = refDetailIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> refDetailMap= (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, refDetailMap);

	         //prelim map
            nameSpace = NAMESPACE_PRELIM;
            cdmClass = Reference.class;
            List<Reference> list = getReferenceService().findById(prelimRefDetailCandidateIdSet);
            Map<String, Reference> prelimMap = new HashMap<>();
            for (Reference ref : list){
                prelimMap.put(String.valueOf(ref.getId()), ref);
            }
            result.put(nameSpace, prelimMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private boolean makeZoologialName(ResultSet rs, IZoologicalName zooName, int nameId)
					throws SQLException{
		boolean success = true;
		//publicationYear
		String authorTeamYear = rs.getString("authorTeamYear");
		try {
			if (! "".equals(CdmUtils.Nz(authorTeamYear).trim())){
				Integer publicationYear  = Integer.valueOf(authorTeamYear.trim());
				zooName.setPublicationYear(publicationYear);
			}
		} catch (NumberFormatException e) {
			logger.warn("authorTeamYear could not be parsed for taxonName: "+ nameId);
		}
		//original publication year
		String basAuthorTeamYear = rs.getString("basAuthorTeamYear");
		try {
			if (! "".equals(CdmUtils.Nz(basAuthorTeamYear).trim())){
				Integer OriginalPublicationYear  = Integer.valueOf(basAuthorTeamYear.trim());
				zooName.setOriginalPublicationYear(OriginalPublicationYear);
			}
		} catch (NumberFormatException e) {
			logger.warn("basAuthorTeamYear could not be parsed for taxonName: "+ nameId);
		}
		return success;
	}

	private boolean makeBotanicalNamePart(ResultSet rs, IBotanicalName botanicalName)throws SQLException{
		boolean success = true;
		String dbAttrName;
		String cdmAttrName;

		dbAttrName = "HybridFormulaFlag";
		cdmAttrName = "isHybridFormula";
		success &= ImportHelper.addBooleanValue(rs, botanicalName, dbAttrName, cdmAttrName);

		dbAttrName = "MonomHybFlag";
		cdmAttrName = "isMonomHybrid";
		success &= ImportHelper.addBooleanValue(rs, botanicalName, dbAttrName, cdmAttrName);

		dbAttrName = "BinomHybFlag";
		cdmAttrName = "isBinomHybrid";
		success &= ImportHelper.addBooleanValue(rs, botanicalName, dbAttrName, cdmAttrName);

		dbAttrName = "TrinomHybFlag";
		cdmAttrName = "isTrinomHybrid";
		success &= ImportHelper.addBooleanValue(rs, botanicalName, dbAttrName, cdmAttrName);

		try {
			String strCultivarGroupName = rs.getString("CultivarGroupName");
			String strCultivarName = rs.getString("CultivarName");
			if (botanicalName.isCultivar()){
				ICultivarPlantName cultivarName = (ICultivarPlantName)botanicalName;
				String concatCultivarName = CdmUtils.concat("-", strCultivarName, strCultivarGroupName);
				if (isNotBlank(strCultivarGroupName) && isNotBlank(strCultivarName)){
					logger.warn("CDM does not support cultivarGroupName and CultivarName together: " + concatCultivarName);
				}
				cultivarName.setCultivarName(strCultivarGroupName);
			}
		} catch (SQLException e) {
			throw e;
		}
		return success;
	}


	private boolean makeNomenclaturalReference(BerlinModelImportState state, TaxonName taxonName,
					int nameId, ResultSet rs, @SuppressWarnings("rawtypes") ResultSetPartitioner partitioner) throws SQLException{
	    BerlinModelImportConfigurator config = state.getConfig();

	    @SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refDetailMap = partitioner.getObjectMap(BerlinModelRefDetailImport.REFDETAIL_NAMESPACE);

		Integer nomRefFkInt = nullSafeInt(rs, "NomRefFk");
		Integer nomRefDetailFkInt = nullSafeInt(rs, "NomRefDetailFk");
		boolean refDetailPrelim = rs.getBoolean("RefDetailPrelim");

		boolean success = true;
		//nomenclatural Reference
		if (refMap != null){
			if (nomRefFkInt != null){
				String nomRefFk = String.valueOf(nomRefFkInt);
				String nomRefDetailFk = String.valueOf(nomRefDetailFkInt);
				//get nomRef
				Reference nomReference =
					getReferenceFromMaps(refDetailMap, refMap, nomRefDetailFk, nomRefFk);

				if(config.isDoPreliminaryRefDetailsWithNames() && refDetailPrelim){
				    makePrelimRefDetailRef(state, rs, partitioner, taxonName, nameId);
				}else{

    				//setNomRef
    				if (nomReference == null ){
    					//TODO
    					if (! config.isIgnoreNull()){
    						logger.warn("Nomenclatural reference (nomRefFk = " + nomRefFk + ") for TaxonName (nameId = " + nameId + ")"+
    							" was not found in reference store. Nomenclatural reference not set!!");
    					}
    				}else{
    					if (! INomenclaturalReference.class.isAssignableFrom(nomReference.getClass())){
    						logger.warn("Nomenclatural reference (nomRefFk = " + nomRefFk + ") for TaxonName (nameId = " + nameId + ")"+
    								" is not assignable from INomenclaturalReference. (Class = " + nomReference.getClass()+ ")");
    					}
    					nomReference.setNomenclaturallyRelevant(true);
    					taxonName.setNomenclaturalReference(nomReference);
    				}
				}
			}
		}
		return success;
	}


	private INonViralNameParser<?> parser = NonViralNameParserImpl.NewInstance();


    private class ReferenceCandidate{
        Reference ref;
        String detail;
        private ReferenceCandidate(Reference ref, String detail) {
            this.ref = ref;
            this.detail = detail;
        }
        public Integer getId() {
            return ref.getId();
        }
        @Override
        public String toString(){
            return ref.toString() + ": " + detail;
        }
    }

    private class FinalCandidate{
        private FinalCandidate(ReferenceCandidate candidate, ReferenceCandidate exemplar, MatchResult matchResult) {
            this.candidate = candidate;
            this.exemplar = exemplar;
            this.matchResult = matchResult;
        }
        ReferenceCandidate candidate;
        ReferenceCandidate exemplar;
        MatchResult matchResult;

        @Override
        public String toString(){
            return candidate.toString() + " <-> " + exemplar.toString() + "\n   " + matchResult.toString()+"\n";
        }
    }

	/**
     * @param config
     * @param rs
	 * @param partitioner
     * @param taxonName
     * @param nameId
	 * @throws SQLException
     */
    private void makePrelimRefDetailRef(BerlinModelImportState state, ResultSet rs, @SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
            TaxonName taxonName, int nameId) throws SQLException {

        int refDetailId = rs.getInt("RefDetailId");
        @SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(NAMESPACE_PRELIM);

        String nameTitleCache = taxonName.getTitleCache();

        String fullNomRefCache = rs.getString("FullNomRefCache");
        String detail = rs.getString("Details");

        if (fullNomRefCache == null){
            logger.warn("fullNomRefCache is null for preliminary refDetail. NameId: " + nameId);
            return;
        }

        fullNomRefCache = fullNomRefCache.trim();
        if (fullNomRefCache.startsWith(": ")){
            logger.warn("fullNomRefCache starts with ':' for preliminary refDetail. NameId: " + nameId);
            return;
        }else if (fullNomRefCache.matches("[12][7890][0-9][0-9](-(1774|1832))?") && isBlank(detail)){
            handlePrelimYearOnly(state, rs, taxonName, nameId, refMap, fullNomRefCache, detail, refDetailId);
        }else{
            Reference genericCandidate = ReferenceFactory.newGeneric();
            genericCandidate.setAbbrevTitleCache(fullNomRefCache, true);
            Set<FinalCandidate> finalCandidates = new HashSet<>();
            Set<FinalCandidate> finalInRefCandidates = new HashSet<>();
            Set<Reference> parsedReferences = new HashSet<>();

            makeFinalCandidates(state, rs, taxonName, refMap,
                    nameTitleCache, finalCandidates,
                    finalInRefCandidates, parsedReferences);

            evaluateFinalCandidates(state, rs, taxonName, detail, genericCandidate, parsedReferences,
                    finalCandidates, fullNomRefCache);
        }
    }


    /**
     * @param state
     * @param rs
     * @param taxonName
     * @param nameId
     * @param refMap
     * @param fullNomRefCache
     * @param detail
     * @throws SQLException
     */
    private void handlePrelimYearOnly(BerlinModelImportState state, ResultSet rs, TaxonName taxonName, int nameId,
            Map<String, Reference> refMap, String fullNomRefCache, String detail, int refDetailId) throws SQLException {
        TeamOrPersonBase<?> combAuthor = taxonName.getCombinationAuthorship();
        Set<Integer> candidateIds = getPreliminaryIdCandidates(state, rs);

        boolean candidateMatches = false;
        for (Integer candidateId : candidateIds){
            Reference dedupCandidate = CdmBase.deproxy(refMap.get(String.valueOf(candidateId)));
            System.out.println("dedupCandidate: " + dedupCandidate.getAbbrevTitleCache());
            TeamOrPersonBase<?> dedupAuthor = dedupCandidate.getAuthorship();
            if (dedupAuthor != null && combAuthor != null){
                if (Objects.equals(dedupAuthor, combAuthor)){
                    taxonName.setNomenclaturalReference(dedupCandidate);
                    candidateMatches = true;
                }else if (Objects.equals(dedupAuthor.getNomenclaturalTitle(), combAuthor.getNomenclaturalTitle())){
                    logger.warn("Year nomAuthor equal in nomTitle but not same: " + dedupAuthor.getNomenclaturalTitle() + "; " + fullNomRefCache + "; nameId " + nameId);
                    taxonName.setNomenclaturalReference(dedupCandidate);
                    candidateMatches = true;
                }
            }else if (dedupCandidate.getAuthorship() == null && combAuthor != null){
                logger.warn("Year dedupCand and name have no author: " + fullNomRefCache + "; nameId " + nameId);
                taxonName.setNomenclaturalReference(dedupCandidate);
                candidateMatches = true;
            }
        }
        if (!candidateMatches){
            Reference yearRef = ReferenceFactory.newGeneric();
            VerbatimTimePeriod timePeriod = TimePeriodParser.parseStringVerbatim(fullNomRefCache);
            yearRef.setDatePublished(timePeriod);
            yearRef.setAuthorship(combAuthor);
            taxonName.setNomenclaturalReference(yearRef);
            yearRef.addImportSource(String.valueOf(refDetailId), NAMESPACE_PRELIM, state.getTransactionalSourceReference(), null);
            refMapping.addCandidate(yearRef, detail);
            //TODO
//                refMap.put(key, yearRef);
        }
    }


    private enum MatchType{
        UNPARSED,
        NO_MATCH_SINGLE_PARSE_ARTICLE_WITH_COMMA,
        NO_MATCH_SINGLE_PARSE_ARTICLE_NO_COMMA,
        NO_MATCH_SINGLE_PARSE_BOOKSECTION,
        NO_MATCH_SINGLE_PARSE_BOOK,
        NO_MATCH_SINGLE_PARSE_GENERIC,
        NO_MATCH_SINGLE_PARSE_OTHER,
        NO_MATCH_MULTI_PARSE,
        NO_MATCH_WITH_CANDIDATE,
        SINGLE_FULL_MATCH,
        SINGLE_INREF_MATCH,
        MULTI_SINGLE_PERSISTENT,
        MULTI_MULTI_PERSISTENT_NO_EXACT,
        MULTI_MULTI_PERSISTENT_MULTI_EXACT,
        MULTI_MULTI_PERSISTENT_SINGLE_EXACT,
        MULTI_NO_PERSISTENT_MULTI_EXACT,
        MULTI_NO_PERSISTENT_SINGLE_EXACT,
        MULTI_NO_PERSISTENT_NO_EXACT,
    }

    private Map<MatchType, List<String>> matchResults = new HashMap<>();

    /**
     * @param taxonName
     * @param detail
     * @param genericCandidate
     * @param finalCandidates
     * @param fullNomRefCache
     * @param exemplars
     * @throws SQLException
     */
    private void evaluateFinalCandidates(BerlinModelImportState state, ResultSet rs,
            TaxonName taxonName, String detail, Reference genericCandidate, Set<Reference> parsedCandidates,
            Set<FinalCandidate> finalCandidates, String fullNomRefCache) throws SQLException {

        int refDetailId = rs.getInt("RefDetailId");
        Set<FinalCandidate> matchingCandidates = getSuccess(finalCandidates);
        if (matchingCandidates.isEmpty()){
            taxonName.setNomenclaturalReference(genericCandidate);
            genericCandidate.addImportSource(String.valueOf(refDetailId), BerlinModelRefDetailImport.REFDETAIL_NAMESPACE,
                    state.getTransactionalSourceReference(), null);
            //TODO should we set this?
            taxonName.setNomenclaturalMicroReference(detail);
            if (finalCandidates.isEmpty()){
                if (taxonName.getCombinationAuthorship()==null){
                    System.out.println("nom. ref. not parsed because author is null: " + taxonName.getTitleCache());
                }else{
                    System.out.println("Final Candidates empty but author exists - should not happen: " + taxonName.getTitleCache());
                }
                handleNoMatch(state, taxonName, detail, genericCandidate, finalCandidates, fullNomRefCache, parsedCandidates);
//                printResult(MatchType.NO_MATCH, unparsedAndName(fullNomRefCache, taxonName));
            }else if (hasOnlyUnparsedExemplars(finalCandidates)){
                printResult(MatchType.UNPARSED, unparsedAndName(fullNomRefCache, taxonName));
            }else if (hasNoCandidateExemplars(finalCandidates)){
                //but we can define the ref type here
                handleNoMatch(state, taxonName, detail, genericCandidate, finalCandidates, fullNomRefCache, parsedCandidates);
//                printResult(MatchType.NO_MATCH, unparsedAndName(fullNomRefCache, taxonName));
            }else{
                String message = resultMessage(fullNomRefCache, finalCandidates, taxonName);
                printResult(MatchType.NO_MATCH_WITH_CANDIDATE, message);
            }
        }else if (matchingCandidates.size() == 1){
            ReferenceCandidate single = matchingCandidates.iterator().next().candidate;
            addAuthorAndDetail(taxonName, single);
            if (single.ref.isPersited()){
                printResult(MatchType.SINGLE_FULL_MATCH, unparsedAndName(fullNomRefCache, taxonName));
            }else{
                single.ref.addImportSource(String.valueOf(refDetailId), BerlinModelRefDetailImport.REFDETAIL_NAMESPACE,
                        state.getTransactionalSourceReference(), null);
                printResult(MatchType.SINGLE_INREF_MATCH,  unparsedAndName(fullNomRefCache, taxonName));
            }
        }else{
            FinalCandidate finCand = findBestMatchingFinalCandidate(taxonName, matchingCandidates, fullNomRefCache);
            addAuthorAndDetail(taxonName, finCand.candidate);
        }
    }


    /**
     * @param state
     * @param taxonName
     * @param detail
     * @param genericCandidate
     * @param finalCandidates
     * @param fullNomRefCache
     * @param parsedCandidates
     */
    private void handleNoMatch(BerlinModelImportState state, TaxonName taxonName, String detail,
            Reference genericCandidate, Set<FinalCandidate> finalCandidates, String fullNomRefCache,
            Set<Reference> parsedCandidatesAsRef) {
        Set<FinalCandidate> parsedCandidates = getParsedExemplars(finalCandidates);
//        parsedCandidatesAsRef = removeGenericFromParsedReferencesAsRef();
//        if (parsedCandidates.size() != parsedCandidatesAsRef.size()){
//            System.out.println("Parsed Candidates differ in size. Should not happen");
//        }
        if (parsedCandidates.isEmpty()){
            System.out.println("Parsed Candidates empty. Should not happen");
        }else if (parsedCandidates.size() == 1){

            ReferenceCandidate refCand = parsedCandidates.iterator().next().exemplar;
            addAuthorAndDetail(taxonName, refCand);
            if (refCand.ref.getType() == ReferenceType.Article){
                if(refCand.ref.getInReference().getAbbrevTitle().contains(",")){
                    printResult(MatchType.NO_MATCH_SINGLE_PARSE_ARTICLE_WITH_COMMA, unparsedAndName(fullNomRefCache, taxonName));
                }else{
                    printResult(MatchType.NO_MATCH_SINGLE_PARSE_ARTICLE_NO_COMMA, unparsedAndName(fullNomRefCache, taxonName));
                }
            }else if (refCand.ref.getType() == ReferenceType.BookSection){
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_BOOKSECTION, unparsedAndName(fullNomRefCache, taxonName));
            }else if (refCand.ref.getType() == ReferenceType.Book){
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_BOOK, unparsedAndName(fullNomRefCache, taxonName));
            }else {
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_OTHER, unparsedAndName(fullNomRefCache, taxonName));
            }
        }else{
            ReferenceCandidate generCandidate = createGenericReference(parsedCandidates, detail);
            addAuthorAndDetail(taxonName, generCandidate);
            if (generCandidate.ref.getType() == ReferenceType.Article){
                if(generCandidate.ref.getInReference().getAbbrevTitle().contains(",")){
                    printResult(MatchType.NO_MATCH_SINGLE_PARSE_ARTICLE_WITH_COMMA, unparsedAndName(fullNomRefCache, taxonName));
                }else{
                    printResult(MatchType.NO_MATCH_SINGLE_PARSE_ARTICLE_NO_COMMA, unparsedAndName(fullNomRefCache, taxonName));
                }
            }else if (generCandidate.ref.getType() == ReferenceType.BookSection){
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_BOOKSECTION, unparsedAndName(fullNomRefCache, taxonName));
            }else if (generCandidate.ref.getType() == ReferenceType.Book){
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_BOOK, unparsedAndName(fullNomRefCache, taxonName));
            }else if (generCandidate.ref.getType() == ReferenceType.Generic){
                printResult(MatchType.NO_MATCH_SINGLE_PARSE_GENERIC, unparsedAndName(fullNomRefCache, taxonName));
            }else {
                printResult(MatchType.NO_MATCH_MULTI_PARSE, unparsedAndName(fullNomRefCache, taxonName));
            }
        }

//        System.out.println(fullNomRefCache);
    }

    private static final Reference NO_REMAINING_SINGLE = ReferenceFactory.newGeneric();
    /**
     * @param parsedCandidates
     * @return
     */
    private ReferenceCandidate createGenericReference(Set<FinalCandidate> parsedCandidates, String detail) {
        Reference refGen = ReferenceFactory.newGeneric();
        String title = null;
        VerbatimTimePeriod datePublished = null;
        String volume = null;
        String series = null;
        String edition = null;
        TeamOrPersonBase<?> author = null;

        Reference journalCandidate = null;
        Reference remainingSingle = null;
        for (FinalCandidate parsedCand : parsedCandidates){

            Reference ref = parsedCand.exemplar.ref;
            if (ref.getType().isArticle()){
                journalCandidate = ref;
            }
            if (!ref.getType().isPublication()){
                if (ref.getInReference().getAbbrevTitle().matches("((ser|ed)\\..*|(Beih|App|Suppl|Praef|Bot|S\u00E9r\\. Bot|Prodr|Alt|Ap|Nachtr)\\.|Apend|Texte|Atlas)")){
                    continue;
                }
            }
            if (ref.getType().isArticle()){
                if (ref.getVolume() == null || ref.getInReference().getAbbrevTitle().endsWith(", ed.")){
                    continue;
                }
            }

            //title
            if (ref.getType().isPublication()){
                title = verify(title, ref.getAbbrevTitle());
            }else{
                title = verify(title, ref.getInReference().getAbbrevTitle());
            }
            //volume
            if (ref.getType().isVolumeReference()){
                volume = verify(volume, ref.getVolume());
            }else{
                volume = verify(volume, ref.getInReference().getVolume());
            }
            //edition
            if (ref.getType().isVolumeReference()){
                edition = verify(edition, ref.getEdition());
            }else{
                edition = verify(edition, ref.getInReference().getEdition());
            }
            //series
            if (ref.getType().isVolumeReference()){
                series = verify(series, ref.getSeriesPart());
            }else{
                series = verify(series, ref.getInReference().getSeriesPart());
            }
            //datePublished
            datePublished = verify(datePublished, ref.getDatePublished());
            //datePublished
            author = verify(author, ref.getAuthorship());

            remainingSingle = remainingSingle == null? ref : NO_REMAINING_SINGLE;
        }

        if (remainingSingle == null){
            System.out.println("No remaing ref. This should not happen.");
        }else if (remainingSingle != NO_REMAINING_SINGLE){
            refGen = remainingSingle;
        }else if (IJournal.guessIsJournalName(title) && journalCandidate != null){
            refGen = journalCandidate;
        }else{
            refGen.setAbbrevTitle(title);
            refGen.setVolume(volume);
            refGen.setEdition(edition);
            refGen.setSeriesPart(series);
            refGen.setDatePublished(datePublished);
            refGen.setAuthorship(author);
        }

        ReferenceCandidate cand = new ReferenceCandidate(refGen, detail);
        return cand;
    }


    /**
     * @param existing
     * @param newText
     * @return
     */
    private <T extends Object> T verify(T existing, T newText) {
        if (existing == null){
            return newText;
        }else if (existing.equals(newText)){
            return existing;
        }else if (newText == null){
            logger.warn("Text not verified, missing, before: " + existing);
            return existing;
        }else{
            logger.warn("Text not verified, differs: " +  existing + "<->" +newText);
            return existing;
        }
    }


    /**
     * @param finalCandidates
     * @return
     */
    private boolean hasNoCandidateExemplars(Set<FinalCandidate> finalCandidates) {
        for (FinalCandidate finalCandidate : finalCandidates){
            if (finalCandidate.matchResult != UNPARSED_EXEMPLAR && finalCandidate.matchResult != PARSED_NO_CANDIDATE ){
                return false;
            }
        }
        return true;
    }

    private Set<FinalCandidate> getParsedExemplars(Set<FinalCandidate> finalCandidates) {
        Set<FinalCandidate> parsedCandidates = new HashSet<>();
        for (FinalCandidate finalCandidate : finalCandidates){
            if (finalCandidate.matchResult != UNPARSED_EXEMPLAR){
                parsedCandidates.add(finalCandidate);
            }
        }
        return parsedCandidates;
    }

    /**
     * @param finalCandidates
     * @return
     */
    private boolean hasOnlyUnparsedExemplars(Set<FinalCandidate> finalCandidates) {
        for (FinalCandidate finalCandidate : finalCandidates){
            if (finalCandidate.matchResult != UNPARSED_EXEMPLAR){
                return false;
            }
        }
        return true;
    }


    /**
     * @param taxonName
     * @param single
     */
    private void addAuthorAndDetail(TaxonName taxonName, ReferenceCandidate refCand) {
        if (!CdmUtils.nullSafeEqual(refCand.ref.getAuthorship(), taxonName.getCombinationAuthorship())){
            TeamOrPersonBase<?> refAut = refCand.ref.getAuthorship();
            TeamOrPersonBase<?> nameAut = taxonName.getCombinationAuthorship();
            try {
                MatchResult match = MatchStrategyFactory.NewParsedTeamOrPersonInstance().invoke(refAut, nameAut, true);
                if (match.isFailed()){
                    System.out.println("not same author \n"+ match);
                }else{
                    taxonName.setCombinationAuthorship(refAut);
                }
            } catch (MatchException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //FIXME deduplicate
        taxonName.setNomenclaturalReference(refCand.ref);
        taxonName.setNomenclaturalMicroReference(refCand.detail);
    }



//
//    /**
//     * @param finalCandidates
//     * @return
//     */
//    private int getSuccessCount(Set<FinalCandidate> finalCandidates) {
//        int i = 0;
//        for (FinalCandidate candidate : finalCandidates){
//            if (candidate.matchResult.isSuccessful()){
//                i++;
//            }
//        }
//        return i;
//    }

    /**
     * @param fullNomRefCache
     * @param taxonName
     * @return
     */
    private String unparsedAndName(String fullNomRefCache, TaxonName taxonName) {
        return fullNomRefCache +" | " + taxonName.getFullTitleCache();
    }


    /**
     * @param noMatch
     * @param fullTitleCache
     */
    private void printResult(MatchType type, String text) {
        List<String> list = matchResults.get(type);
        if (list == null){
            list = new ArrayList<>();
            matchResults.put(type, list);
        }
        list.add(text);

    }


    /**
     * @param fullNomRefCache
     * @param finalCandidates
     * @param taxonName
     * @return
     */
    private String resultMessage(String fullNomRefCache, Set<FinalCandidate> finalCandidates, TaxonName taxonName) {
        String result = unparsedAndName(fullNomRefCache, taxonName)+"\n";
        result += finalCandidates.size() +": " + matchResultMessage(finalCandidates);
        return result;
    }


    /**
     * @param finalCandidates
     * @param result
     * @return
     */
    private String matchResultMessage(Set<FinalCandidate> finalCandidates) {
        String result = "\n     ";
        for (FinalCandidate finalCand : finalCandidates){
            result += finalCand.matchResult.toString()+"\n     ";
        }
        return result;
    }


    private Set<FinalCandidate> getSuccess(Set<FinalCandidate> finalCandidates) {
        Set<FinalCandidate> result = new HashSet<>();
        for (FinalCandidate candidate : finalCandidates){
            if (candidate.matchResult.isSuccessful()){
               result.add(candidate);
            }
        }
        return result;
    }


    /**
     * @param taxonName
     * @param finalCandidates
     * @param exemplars
     * @return
     */
    private FinalCandidate findBestMatchingFinalCandidate(TaxonName taxonName,
            Set<FinalCandidate> finalCandidates, String fullNomRefCache) {
        try {
            Set<FinalCandidate> persistentMatches = findPersistentMatch(taxonName, finalCandidates);
            if (persistentMatches.size() >= 1){
                if (persistentMatches.size()>1){
                    Set<FinalCandidate> exactMatches = findExactMatch(taxonName, finalCandidates);
                    Set<FinalCandidate> successCandidatesExacts = getSuccess(exactMatches);
                    if (successCandidatesExacts.size() >= 1){
                        FinalCandidate result = successCandidatesExacts.iterator().next();
                        addAuthorAndDetail(taxonName, result.candidate);
//                        String message = resultMessage(fullNomRefCache, exactMatches, taxonName);
                        if (successCandidatesExacts.size()>1){
                            printResult(MatchType.MULTI_MULTI_PERSISTENT_MULTI_EXACT, unparsedAndName(fullNomRefCache, taxonName));
                        }else{
                            printResult(MatchType.MULTI_MULTI_PERSISTENT_SINGLE_EXACT, unparsedAndName(fullNomRefCache, taxonName));
                        }
                        return result;
                    }else{
                        String message = resultMessage(fullNomRefCache, successCandidatesExacts, taxonName);
                        printResult(MatchType.MULTI_MULTI_PERSISTENT_NO_EXACT, message);
                        FinalCandidate result = persistentMatches.iterator().next();
                        addAuthorAndDetail(taxonName, result.candidate);
                        return result;
                    }
                }else{
                    FinalCandidate result = persistentMatches.iterator().next();
                    addAuthorAndDetail(taxonName, result.candidate);
                    printResult(MatchType.MULTI_SINGLE_PERSISTENT, taxonName.getFullTitleCache());
                    return result;
                }
            }
            Set<FinalCandidate> exactMatches = findExactMatch(taxonName, finalCandidates);
            Set<FinalCandidate> successCandidatesExacts = getSuccess(exactMatches);
            if (successCandidatesExacts.size() >= 1){
                FinalCandidate result = successCandidatesExacts.iterator().next();
                addAuthorAndDetail(taxonName, result.candidate);
                String message = resultMessage(fullNomRefCache, exactMatches, taxonName);
                if (successCandidatesExacts.size()>1){
                    printResult(MatchType.MULTI_NO_PERSISTENT_MULTI_EXACT, message);
//                    System.out.println("More then 1 exact match: " + taxonName.getFullTitleCache() + ": " + exactMatches.iterator().next().exemplar.ref.getAbbrevTitleCache());
                }else{
                    printResult(MatchType.MULTI_NO_PERSISTENT_SINGLE_EXACT, message);
                }
                return result;
            }else{
                FinalCandidate result = finalCandidates.iterator().next();
                addAuthorAndDetail(taxonName, result.candidate);
                String message = resultMessage(fullNomRefCache, exactMatches, taxonName);
                printResult(MatchType.MULTI_NO_PERSISTENT_NO_EXACT, message);
                return result;
            }
        } catch (MatchException e) {
            e.printStackTrace();
            return finalCandidates.iterator().next();
        }
    }



    /**
     * @param taxonName
     * @param finalCandidates
     * @return
     */
    private String getMultiMultiPersistentMessage(TaxonName taxonName, Set<FinalCandidate> finalCandidates) {
        String result = finalCandidates.size() + ":" + taxonName.getFullTitleCache();
        result += matchResultMessage(finalCandidates);
        return result;
    }
    private String getMultiNoPersistentMultiExactMessage(TaxonName taxonName, Set<FinalCandidate> finalCandidates) {
        String result = finalCandidates.size() + ":" + taxonName.getFullTitleCache();
        result += matchResultMessage(finalCandidates);
        return result;
    }

    /**
     * @param taxonName
     * @param finalCandidates
     * @param exemplars
     * @return
     * @throws MatchException
     */
    private Set<FinalCandidate> findExactMatch(TaxonName taxonName, Set<FinalCandidate> finalCandidates) throws MatchException {
        IMatchStrategyEqual exactMatcher = getExactMatcher();
        Set<FinalCandidate> result = new HashSet<>();
        for (FinalCandidate cand : finalCandidates){
            Reference exemplarRef = cand.exemplar.ref;
            if (cand.candidate.ref.getType().equals(exemplarRef.getType())){
                MatchResult match = exactMatcher.invoke(cand.candidate.ref, exemplarRef, true);
                result.add(new FinalCandidate(cand.candidate, cand.exemplar, match));
                if (match.isFailed()){
                    String oldTitle = exemplarRef.getTitle();
                    exemplarRef.setTitle(exemplarRef.getAbbrevTitle());
                    match = exactMatcher.invoke(cand.candidate.ref, exemplarRef, true);
                    if (match.isSuccessful()){
                        result.add(new FinalCandidate(cand.candidate, cand.exemplar, match));
                    }
                    exemplarRef.setTitle(oldTitle);
                }
            }else{
                MatchResult match = MatchResult.NewNoTypeInstance(cand.candidate.ref.getType(), exemplarRef.getType());
                FinalCandidate finCand = new FinalCandidate(cand.candidate, cand.exemplar, match);
                result.add(finCand);
            }
        }
        return result;
    }

    /**
     * @return
     */
    private IMatchStrategyEqual getExactMatcher() {
        IMatchStrategyEqual result = MatchStrategyFactory.NewDefaultInstance(Reference.class);
        FieldMatcher inRefMatcher = result.getMatching().getFieldMatcher("inReference");
        try {
            inRefMatcher.getMatchStrategy().setMatchMode("title", MatchMode.EQUAL);
            return result;
        } catch (MatchException e) {
            throw new RuntimeException("Problems creating exact matcher.", e);
        }//must not be EXACT_REQUIRED
    }


    private Set<FinalCandidate> findPersistentMatch(TaxonName taxonName, Set<FinalCandidate> finalCandidates) throws MatchException {
        Set<FinalCandidate> result = new HashSet<>();
        for (FinalCandidate cand : finalCandidates){
            if (cand.candidate.ref.isPersited()){
                result.add(cand);
            }
        }

        return result;
    }


    /**
     * @param state
     * @param rs
     * @param taxonName
     * @param refMap
     * @param nameTitleCache
     * @param fullNomRefCache
     * @param finalCandidates
     * @param genericCandidate
     * @param exemplars2
     * @param finalCandidates
     * @throws SQLException
     */
    public static final MatchResult UNPARSED_EXEMPLAR = new MatchResult();
    public static final MatchResult PARSED_NO_CANDIDATE = new MatchResult();
    {
        UNPARSED_EXEMPLAR.addNullMatching(null, null);
        PARSED_NO_CANDIDATE.addNullMatching(null, null);
    }
    private void makeFinalCandidates(BerlinModelImportState state, ResultSet rs, TaxonName taxonName,
            Map<String, Reference> refMap, String nameTitleCache,
            Set<FinalCandidate> finalCandidates,
            Set<FinalCandidate> finalInRefCandidates, Set<Reference> parsedReferences
            ) throws SQLException {

        Set<Integer> candidateIds = getPreliminaryIdCandidates(state, rs);
        Set<TaxonName> nameCandidates = parseExemplars(state, rs, taxonName);

        Set<ReferenceCandidate> exemplars = new HashSet<>();
        for(TaxonName nameCandidate: nameCandidates){
            if(nameCandidate.getNomenclaturalReference()!= null){
                exemplars.add(new ReferenceCandidate(nameCandidate.getNomenclaturalReference(), nameCandidate.getNomenclaturalMicroReference()));
                parsedReferences.add(nameCandidate.getNomenclaturalReference());
            }
        }

        for(ReferenceCandidate exemplar: exemplars){
            if (exemplar.ref.isProtectedAbbrevTitleCache() || exemplar.ref.isProtectedTitleCache()){
                FinalCandidate parsedNoCandidateExemplarCandidate = new FinalCandidate(null, exemplar, UNPARSED_EXEMPLAR);
                finalCandidates.add(parsedNoCandidateExemplarCandidate);
            }else if (candidateIds.isEmpty()){
                FinalCandidate unparsedExemplarCandidate = new FinalCandidate(null, exemplar, PARSED_NO_CANDIDATE);
                finalCandidates.add(unparsedExemplarCandidate);
            }else{
                for (Integer candidateId : candidateIds){
                    if (candidateId == null){
                        logger.warn("CandidateId not found: " + candidateId);
                        continue;
                    }
                    Reference dedupCandidate = CdmBase.deproxy(refMap.get(String.valueOf(candidateId)));

                    //ref
                    FinalCandidate cand = matchSingle(finalCandidates, dedupCandidate, exemplar, nameTitleCache);
                    //inRef
                    if (cand.matchResult.isFailed() && exemplar.ref.getInReference() != null ){
                        FinalCandidate candInRef = matchSingle(finalInRefCandidates, dedupCandidate, new ReferenceCandidate(exemplar.ref.getInReference(), null), nameTitleCache);
                        if(candInRef.matchResult.isSuccessful()){
                            Reference clone = (Reference)exemplar.ref.clone();
                            clone.setInReference(dedupCandidate);
                            FinalCandidate inRefCand = new FinalCandidate(new ReferenceCandidate(clone, exemplar.detail),
                                    exemplar, candInRef.matchResult);
                            finalCandidates.add(inRefCand);
                        }
                    }
                }
            }
        }

        return;
    }


    /**
     * @param finalCandidates
     * @param refCandidate
     * @param exemplar
     * @param fullNomRefCache
     * @param nameTitleCache
     */
    protected FinalCandidate matchSingle(Set<FinalCandidate> finalCandidates, Reference dedupCandidate,
            ReferenceCandidate exemplar, String nameTitleCache) {

        try {
            MatchResult match = null;
            FinalCandidate finalCand;
            IMatchStrategy matchStrategy = getReferenceMatchStrategy();
            Reference refExemplar = exemplar.ref;
            if(refExemplar.getType().equals(dedupCandidate.getType())){
                TeamOrPersonBase<?> exemplarAuthor = refExemplar.getAuthorship();
                TeamOrPersonBase<?> candidateAuthor = CdmBase.deproxy(dedupCandidate.getAuthorship());
                String cache = refExemplar.getTitleCache();
                String ccache = dedupCandidate.getTitleCache();
                String abbrevCache = refExemplar.getAbbrevTitleCache();
                String cabbrevCache = dedupCandidate.getAbbrevTitleCache();
                if (exemplarAuthor != null && candidateAuthor != null){
                    exemplarAuthor.getTitleCache();
                    String exemplarAuthorStr = exemplarAuthor.getNomenclaturalTitle();
//                    System.out.println(exemplarAuthor.getTitleCache());
                    String candidateAuthorStr = candidateAuthor.getNomenclaturalTitle();
//                    System.out.println(candidateAuthor.getTitleCache());
                    if (!exemplarAuthorStr.equals(candidateAuthorStr)){
                        match = MatchResult.NewInstance(":authorship", MatchMode.EQUAL, exemplarAuthorStr, candidateAuthorStr);
                    }
                }

                if (match == null){
                    match = matchStrategy.invoke(dedupCandidate, refExemplar, true);
                }

                //TODO detail match
                //TODO formatter match
                if (true){
//                    return true;
                }else if (refExemplar.getInReference() != null && dedupCandidate.getInReference() != null){
//                    boolean matchInRef = matchStrategy.invoke(dedupCandidate.getInReference(), refExemplar.getInReference());
//                    if(matchInRef){
//                        Reference clone = (Reference)refExemplar.clone();
//                        clone.setInReference(dedupCandidate.getInReference());
//                        finalCandidates.add(new ReferenceCandidate(clone, exemplar.detail));
//                    }
                }
            }else{
                match = MatchResult.NewNoTypeInstance(refExemplar.getType(), dedupCandidate.getType());
            }
            finalCand = new FinalCandidate(new ReferenceCandidate(dedupCandidate, exemplar.detail), exemplar, match);
            finalCandidates.add(finalCand);

            return finalCand;
        } catch (MatchException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


    private IMatchStrategy referenceMatchStrategy;

    /**
     * @return
     * @throws MatchException
     */
    protected IMatchStrategy getReferenceMatchStrategy() throws MatchException {

        if (referenceMatchStrategy == null){
            referenceMatchStrategy = MatchStrategyFactory.NewParsedReferenceInstance();
        }
//        if (referenceMatchStrategy == null){
//            referenceMatchStrategy = DefaultMatchStrategy.NewInstance(Reference.class);
//
//            referenceMatchStrategy.setMatchMode("title", MatchMode.EQUAL_OR_SECOND_NULL);
//            referenceMatchStrategy.setMatchMode("placePublished", MatchMode.EQUAL_OR_SECOND_NULL);
//            @SuppressWarnings("rawtypes")
//            SubClassMatchStrategy<TeamOrPersonBase> refAuthorMatchStrategy = SubClassMatchStrategy
//                    .NewInstance(TeamOrPersonBase.class, Person.class, Team.class);
//            refAuthorMatchStrategy.setMatchMode(Person.class, "familyName", MatchMode.EQUAL_OR_SECOND_NULL);
//            refAuthorMatchStrategy.setMatchMode(Person.class, "givenName", MatchMode.EQUAL_OR_SECOND_NULL);
//            refAuthorMatchStrategy.setMatchMode(Person.class, "initials", MatchMode.EQUAL_OR_SECOND_NULL);
//            referenceMatchStrategy.setMatchMode("authorship", MatchMode.MATCH, refAuthorMatchStrategy);
//
//            //for testing only
////            referenceMatchStrategy = null;
////            FieldMatcher autMatcher = referenceMatchStrategy.getMatching().getFieldMatcher("authorship");
//        }
        return referenceMatchStrategy;
    }

    private Set<Integer> getPreliminaryIdCandidates(BerlinModelImportState state, ResultSet rs) throws SQLException{

        Set<Integer> result = new HashSet<>();
        boolean refDetailPrelim = rs.getBoolean("RefDetailPrelim");
        if(state.getConfig().isDoPreliminaryRefDetailsWithNames() && refDetailPrelim){

            Set<TaxonName> names = parseExemplars(state, rs, null);
            for (TaxonName name : names){
                Reference exemplar = name.getNomenclaturalReference();
                if (exemplar != null){
                    Set<ReferenceCandidate> persistendCandidates = refMapping.getCandidates(exemplar);
                    if (exemplar.getInReference()!= null){
                        persistendCandidates.addAll(refMapping.getCandidates(exemplar.getInReference()));
                    }
                    for (ReferenceCandidate persistendCandidate : persistendCandidates){
                        result.add(persistendCandidate.getId());
                    }
                }
            }
        }
        return result;
    }

    private Set<TaxonName> parseExemplars(BerlinModelImportState state, ResultSet rs, TaxonName taxonName) throws SQLException{
        BerlinModelImportConfigurator config = state.getConfig();

        Set<TaxonName> result = new HashSet<>();

        String fullNomRefCache = rs.getString("FullNomRefCache");
        String detail = rs.getString("Details");


        if (fullNomRefCache == null){
//            logger.warn("fullNomRefCache is null for preliminary refDetail. NameId: " + nameId);
            return result;
        }else if (fullNomRefCache.trim().startsWith(": ")){
//            logger.warn("fullNomRefCache starts with for preliminary refDetail. NameId: " + nameId);
            return result;
        }else{
            TaxonName testName = taxonName == null ? getTestName() : (TaxonName)taxonName.clone();

            Set<String> fullStrCandidates;
            if (fullNomRefCache.trim().startsWith("in ")){
                //RefDetails with "in" references
                fullStrCandidates = makePrelimRefDetailInRef(state, testName, fullNomRefCache, detail);
            }else if (fullNomRefCache.trim().startsWith(", ")){
                //RefDetails with ", " reference
                fullStrCandidates = makePrelimRefDetailBook(state, testName, fullNomRefCache, detail);
            }else{
                //ordinary protected ref details
                fullStrCandidates = makePrelimRefDetailNotInRef(state, testName, fullNomRefCache, detail);
            }

            for (String parseStr : fullStrCandidates){
                TaxonName newName = (TaxonName)parser.parseReferencedName(parseStr, config.getNomenclaturalCode(), testName.getRank());
                Reference newNomRef = newName.getNomenclaturalReference();
                if (taxonName != null && newNomRef != null && !newNomRef.isProtectedAbbrevTitleCache()&& !newNomRef.isProtectedTitleCache()){
                    newNomRef.setAuthorship(taxonName.getCombinationAuthorship());
                }
                result.add(newName);
//                Reference exemplar = newName.getNomenclaturalReference();
            }
            return result;
        }
    }


    /**
     * @return
     */
    protected TaxonName getTestName() {
        TaxonName testName = TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES(), null);
        testName.setGenusOrUninomial("Abies");
        testName.setSpecificEpithet("alba");
        testName.setAuthorshipCache("Mill.");
        return testName;
    }



    /**
     * @param config
     * @param refMap
     * @param taxonName
     * @param nameId
     * @param fullNomRefCache
     * @return
     */
    protected Set<String> makePrelimRefDetailNotInRef(BerlinModelImportState state, TaxonName taxonName,
            String fullNomRefCache, String detail) {
        Set<String> result = new HashSet<>();
        String fullStrComma = taxonName.getTitleCache()+ ", " + fullNomRefCache;
        result.add(fullStrComma);
        String fullStrIn = taxonName.getTitleCache()+ " in " + fullNomRefCache;
        result.add(fullStrIn);
        return result;

//
//        INonViralName newNameComma = parser.parseReferencedName(fullStrComma, config.getNomenclaturalCode(), taxonName.getRank());
//        INonViralName newNameIn = parser.parseReferencedName(fullStrIn, config.getNomenclaturalCode(), taxonName.getRank());
//
//        INonViralName newName;
//        boolean commaProtected = newNameComma.isProtectedFullTitleCache() || (newNameComma.getNomenclaturalReference() != null
//                && newNameComma.getNomenclaturalReference().isProtectedTitleCache());
//        boolean inProtected = newNameIn.isProtectedFullTitleCache() || (newNameIn.getNomenclaturalReference() != null
//                && newNameIn.getNomenclaturalReference().isProtectedTitleCache());
//        if (commaProtected && !inProtected){
//            newName = newNameIn;
//        }else if (!commaProtected && inProtected){
//            newName = newNameComma;
//        }else if (commaProtected && inProtected){
//            logger.warn("Can't parse preliminary refDetail: " +  fullNomRefCache + " for name " + taxonName.getTitleCache() + "; nameId: " + nameId );
//            newName = newNameComma;
//        }else{
//            logger.warn("Can't decide ref type for preliminary refDetail: " +  fullNomRefCache + " for name " + taxonName.getTitleCache() + "; nameId: " + nameId );
//            newName = newNameComma;
//        }
//
//
//        if (newName.isProtectedFullTitleCache()){
//            Reference nomRef = ReferenceFactory.newGeneric();
//            nomRef.setAbbrevTitleCache(fullNomRefCache, true);
//            taxonName.setNomenclaturalReference(nomRef);
//            //check detail
//        }else{
//            Reference nomRef = newName.getNomenclaturalReference();
//            taxonName.setNomenclaturalReference(nomRef);
//            String detail = newName.getNomenclaturalMicroReference();
//            String oldDetail = taxonName.getNomenclaturalMicroReference();
//            if (isBlank(detail)){
//                if (isNotBlank(oldDetail)){
//                    logger.warn("Detail could not be parsed but seems to exist. NameId: " + nameId);
//                }
//            }else{
//                if (isNotBlank(oldDetail) && !detail.equals(oldDetail)){
//                    logger.warn("Details differ: " +  detail + " <-> " + oldDetail + ". NameId: " + nameId);
//                }
//                taxonName.setNomenclaturalMicroReference(detail);
//            }
//        }
    }


    /**
     * @param config
     * @param refMap
     * @param taxonName
     * @param nameId
     * @param fullNomRefCache
     * @param detail
     * @return
     */
    protected Set<String> makePrelimRefDetailInRef(BerlinModelImportState state,
            TaxonName taxonName,
            String fullNomRefCache, String detail) {

        Set<String> result = new HashSet<>();
        String parseStr = taxonName.getTitleCache()+ " " + fullNomRefCache;
        result.add(parseStr);
        return result;


//            String detail = newName.getNomenclaturalMicroReference();
//            String oldDetail = taxonName.getNomenclaturalMicroReference();
//            if (isBlank(detail)){
//                if (isNotBlank(oldDetail)){
//                    logger.warn("Detail could not be parsed but seems to exist. NameId: " + nameId);
//                }
//            }else{
//                if (isNotBlank(oldDetail) && !detail.equals(oldDetail)){
//                    logger.warn("Details differ: " +  detail + " <-> " + oldDetail + ". NameId: " + nameId);
//                }
//                taxonName.setNomenclaturalMicroReference(detail);
//            }
    }

    protected Set<String> makePrelimRefDetailBook(BerlinModelImportState state,
            TaxonName taxonName,
            String fullNomRefCache, String detail) {

        Set<String> result = new HashSet<>();
        String parseStr = taxonName.getTitleCache()+ fullNomRefCache;
        result.add(parseStr);
        return result;
    }


    /**
     * Creates the hash string for finding preliminary RefDetail duplicates
     * @param nomRef
     */
    private String refHash(Reference nomRef) {
//        TeamOrPersonBase<?> author = nomRef.getAuthorship();
//        String authorStr = author == null? "" : author.getNomenclaturalTitle();

        String title = nomRef.getAbbrevTitle();
        if (title == null){
            title = nomRef.getTitle();
            if (title == null && nomRef.getInReference() != null){
                title = nomRef.getInReference().getAbbrevTitle();
                if (title == null){
                    title = nomRef.getInReference().getTitle();
                }
            }
            if (title == null){
                title = nomRef.getAbbrevTitleCache();
            }
            if (title == null){
                title = nomRef.getTitleCache();
            }
        }
        String vol = nomRef.getVolume();
        if (vol == null && nomRef.getInReference() != null){
            vol = nomRef.getInReference().getVolume();
        }
        String date = nomRef.getDatePublishedString();
        if (date == null && nomRef.getInReference() != null){
            date = nomRef.getInReference().getDatePublishedString();
        }
        ReferenceType type = nomRef.getType();

        String result = CdmUtils.concat("@", title, vol, date, type.getKey());
        return result;
    }


    private static TeamOrPersonBase<?> getAuthorTeam(Map<String, Team> teamMap, Integer teamIdInt, int nameId, BerlinModelImportConfigurator config){
		if (teamIdInt == null){
			return null;
		}else {
			String teamIdStr = String.valueOf(teamIdInt);
			TeamOrPersonBase<?> author = teamMap.get(teamIdStr);
			if (author == null){
				//TODO
				if (!config.isIgnoreNull() && ! (teamIdStr.equals(0) && config.isIgnore0AuthorTeam()) ){
					logger.warn("AuthorTeam (teamId = " + teamIdStr + ") for TaxonName (nameId = " + nameId + ")"+
				        " was not found in authorTeam store. Relation was not set!");
				}
				return null;
			}else{
				return author;
			}
		}
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonNameImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoTaxonNames();
	}






//FOR FUTURE USE , DONT DELETE
//	new CdmStringMapper("nameId", "nameId"),
//	new CdmStringMapper("rankFk", "rankFk"),
//	new CdmStringMapper("nameCache", "nameCache"),
//	new CdmStringMapper("unnamedNamePhrase", "unnamedNamePhrase"),
//	new CdmStringMapper("fullNameCache", "fullNameCache"),
//	new CdmStringMapper("preliminaryFlag", "preliminaryFlag"),
//	new CdmStringMapper("supragenericName", "supragenericName"),
//	new CdmStringMapper("genus", "genus"),
//	new CdmStringMapper("genusSubdivisionEpi", "genusSubdivisionEpi"),
//	new CdmStringMapper("speciesEpi", "speciesEpi"),
//	new CdmStringMapper("infraSpeciesEpi", "infraSpeciesEpi"),
//	new CdmStringMapper("authorTeamFk", "authorTeamFk"),
//	new CdmStringMapper("exAuthorTeamFk", "exAuthorTeamFk"),
//	new CdmStringMapper("basAuthorTeamFk", "basAuthorTeamFk"),
//	new CdmStringMapper("exBasAuthorTeamFk", "exBasAuthorTeamFk"),
//	new CdmStringMapper("hybridFormulaFlag", "hybridFormulaFlag"),
//	new CdmStringMapper("monomHybFlag", "monomHybFlag"),
//	new CdmStringMapper("binomHybFlag", "binomHybFlag"),
//	new CdmStringMapper("trinomHybFlag", "trinomHybFlag"),
//	new CdmStringMapper("cultivarGroupName", "cultivarGroupName"),
//	new CdmStringMapper("cultivarName", "cultivarName"),
//	new CdmStringMapper("nomRefFk", "nomRefFk"),
//	new CdmStringMapper("nomRefDetailFk", "nomRefDetailFk"),
//	new CdmStringMapper("nameSourceRefFk", "nameSourceRefFk"),
//	new CdmStringMapper("source_Acc", "source_Acc"),
//	new CdmStringMapper("created_When", "created_When"),
//	new CdmStringMapper("created_Who", "created_Who"),
//	new CdmStringMapper("notes", "notes"),
//	new CdmStringMapper("parsingComments", "parsingComments"),
//	new CdmStringMapper("oldNomRefFk", "oldNomRefFk"),
//	new CdmStringMapper("oldNomRefDetailFk", "oldNomRefDetailFk"),
//	new CdmStringMapper("updated_Who", "updated_Who"),
//	new CdmStringMapper("orthoProjection", "orthoProjection"),


}
