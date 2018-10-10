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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonNameImportValidator;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
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
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.ICultivarPlantName;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

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

	public static final UUID SOURCE_ACC_UUID = UUID.fromString("c3959b4f-d876-4b7a-a739-9260f4cafd1c");

	private static int modCount = 5000;
	private static final String pluralString = "TaxonNames";
	private static final String dbTableName = "Name";


	public BerlinModelTaxonNameImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		if (state.getConfig().getNameIdTable()==null ){
			return super.getIdQuery(state);
		}else{
			return "SELECT nameId FROM " + state.getConfig().getNameIdTable() + "";
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
                    " FROM Name LEFT OUTER JOIN RefDetail ON Name.NomRefDetailFk = RefDetail.RefDetailId AND  " +
                    	" Name.NomRefFk = RefDetail.RefFk " +
                    	" LEFT OUTER JOIN Rank ON Name.RankFk = Rank.rankID " +
                " WHERE name.nameId IN ("+ID_LIST_TOKEN+") ";
					//strQuery += " AND RefDetail.PreliminaryFlag = 1 ";
					//strQuery += " AND Name.Created_When > '03.03.2004' ";
		return strRecordQuery +  "";
	}



	@Override
	protected void doInvoke(BerlinModelImportState state) {
		//update rank labels if necessary
		String strAbbrev = state.getConfig().getInfrGenericRankAbbrev();
		Rank rank = Rank.INFRAGENERICTAXON();
		testRankAbbrev(strAbbrev, rank);

		strAbbrev = state.getConfig().getInfrSpecificRankAbbrev();
		rank = Rank.INFRASPECIFICTAXON();
		testRankAbbrev(strAbbrev, rank);

		super.doInvoke(state);
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

					//nomRef
					success &= makeNomenclaturalReference(config, taxonName, nameId, rs, partitioner);

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

					//NonViralName
					if (taxonName.isNonViral()){
						INonViralName nonViralName = taxonName;

						//authorTeams
						if (teamMap != null ){
							nonViralName.setCombinationAuthorship(getAuthorTeam(teamMap, authorFk, nameId, config));
							nonViralName.setExCombinationAuthorship(getAuthorTeam(teamMap, exAuthorFk, nameId, config));
							nonViralName.setBasionymAuthorship(getAuthorTeam(teamMap, basAuthorFk, nameId, config));
							nonViralName.setExBasionymAuthorship(getAuthorTeam(teamMap, exBasAuthorFk, nameId, config));
						}else{
							logger.warn("TeamMap is null");
							success = false;
						}
					}//nonviralName



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
		return success;
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
			while (rs.next()){
				handleForeignKey(rs, teamIdSet, "AuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "ExAuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "BasAuthorTeamFk");
				handleForeignKey(rs, teamIdSet, "ExBasAuthorTeamFk");
				handleForeignKey(rs, referenceIdSet, "nomRefFk");
				handleForeignKey(rs, refDetailIdSet, "nomRefDetailFk");
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


	private boolean makeNomenclaturalReference(BerlinModelImportConfigurator config, TaxonName taxonName,
					int nameId, ResultSet rs, @SuppressWarnings("rawtypes") ResultSetPartitioner partitioner) throws SQLException{

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
				    makePrelimRefDetailRef(config, rs, taxonName, nameId);
				}

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
		return success;
	}


	private INonViralNameParser<?> parser = NonViralNameParserImpl.NewInstance();

	/**
     * @param config
     * @param rs
     * @param taxonName
     * @param nameId
	 * @throws SQLException
     */
    private void makePrelimRefDetailRef(IImportConfigurator config, ResultSet rs, TaxonName taxonName, int nameId) throws SQLException {
        String fullNomRefCache = rs.getString("FullNomRefCache");
        if (fullNomRefCache == null){
            logger.warn("fullNomRefCache is null for preliminary refDetail. NameId: " + nameId);
            return;
        }else if (fullNomRefCache.trim().startsWith(": ")){
            logger.warn("fullNomRefCache starts with for preliminary refDetail. NameId: " + nameId);
            return;
        }else if (fullNomRefCache.trim().startsWith("in ")){
            String fullStr = taxonName.getTitleCache()+ " " + fullNomRefCache;
            INonViralName newName = parser.parseReferencedName(fullStr, config.getNomenclaturalCode(), taxonName.getRank());
            if (newName.isProtectedFullTitleCache()){
                Reference nomRef = ReferenceFactory.newGeneric();
                nomRef.setAbbrevTitleCache(fullNomRefCache, true);
                taxonName.setNomenclaturalReference(nomRef);
                //check detail
            }else{
                Reference nomRef = newName.getNomenclaturalReference();
                taxonName.setNomenclaturalReference(nomRef);
                String detail = newName.getNomenclaturalMicroReference();
                String oldDetail = taxonName.getNomenclaturalMicroReference();
                if (isBlank(detail)){
                    if (isNotBlank(oldDetail)){
                        logger.warn("Detail could not be parsed but seems to exist. NameId: " + nameId);
                    }
                }else{
                    if (isNotBlank(oldDetail) && !detail.equals(oldDetail)){
                        logger.warn("Details differ: " +  detail + " <-> " + oldDetail + ". NameId: " + nameId);
                    }
                    taxonName.setNomenclaturalMicroReference(detail);
                }
            }
        }else{
            String fullStrComma = taxonName.getTitleCache()+ ", " + fullNomRefCache;
            String fullStrIn = taxonName.getTitleCache()+ " in " + fullNomRefCache;
            INonViralName newNameComma = parser.parseReferencedName(fullStrComma, config.getNomenclaturalCode(), taxonName.getRank());
            INonViralName newNameIn = parser.parseReferencedName(fullStrIn, config.getNomenclaturalCode(), taxonName.getRank());

            INonViralName newName;
            boolean commaProtected = newNameComma.isProtectedFullTitleCache() || (newNameComma.getNomenclaturalReference() != null
                    && newNameComma.getNomenclaturalReference().isProtectedTitleCache());
            boolean inProtected = newNameIn.isProtectedFullTitleCache() || (newNameIn.getNomenclaturalReference() != null
                    && newNameIn.getNomenclaturalReference().isProtectedTitleCache());
            if (commaProtected && !inProtected){
                newName = newNameIn;
            }else if (!commaProtected && inProtected){
                newName = newNameComma;
            }else if (commaProtected && inProtected){
                logger.warn("Can't parse preliminary refDetail: " +  fullNomRefCache + " for name " + taxonName.getTitleCache() + "; nameId: " + nameId );
                newName = newNameComma;
            }else{
                logger.warn("Can't decide ref type for preliminary refDetail: " +  fullNomRefCache + " for name " + taxonName.getTitleCache() + "; nameId: " + nameId );
                newName = newNameComma;
            }


            if (newName.isProtectedFullTitleCache()){
                Reference nomRef = ReferenceFactory.newGeneric();
                nomRef.setAbbrevTitleCache(fullNomRefCache, true);
                taxonName.setNomenclaturalReference(nomRef);
                //check detail
            }else{
                Reference nomRef = newName.getNomenclaturalReference();
                taxonName.setNomenclaturalReference(nomRef);
                String detail = newName.getNomenclaturalMicroReference();
                String oldDetail = taxonName.getNomenclaturalMicroReference();
                if (isBlank(detail)){
                    if (isNotBlank(oldDetail)){
                        logger.warn("Detail could not be parsed but seems to exist. NameId: " + nameId);
                    }
                }else{
                    if (isNotBlank(oldDetail) && !detail.equals(oldDetail)){
                        logger.warn("Details differ: " +  detail + " <-> " + oldDetail + ". NameId: " + nameId);
                    }
                    taxonName.setNomenclaturalMicroReference(detail);
                }
            }
        }
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
