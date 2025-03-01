/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport.PublishMarkerChooser;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelGeneralImportValidator;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class BerlinModelImportConfigurator
        extends DbImportConfiguratorBase<BerlinModelImportState>{

    private static final long serialVersionUID = 70300913255425256L;

    private static final Logger logger = LogManager.getLogger();

	public static BerlinModelImportConfigurator NewInstance(Source berlinModelSource, ICdmDataSource destination){
			return new BerlinModelImportConfigurator(berlinModelSource, destination);
	}

	//im-/export uses Classification for is_taxonomically_included_in relationships
    private boolean useClassification = true;

	private PublishMarkerChooser taxonPublishMarker = PublishMarkerChooser.ALL;

	//TODO
	private static IInputTransformer defaultTransformer = null;

	private boolean doNameStatus = true;
	private boolean doRelNames = true;
	private boolean doCommonNames = true;
	private boolean doOccurrence = true;
	private boolean doOccurrenceSources = true;
	private boolean doMarker = true;
	private boolean doUser = true;
	private boolean doFacts = true;
	private boolean doNameFacts = true;
	private boolean doAuthors = true;
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	private boolean doTaxonNames = true;
	private boolean doTypes = true;
	private boolean doNamedAreas = true;

	private boolean isSalvador = false;
	private boolean isEuroMed = false;
	private boolean isMoose = false;
	private boolean isMcl = false;

	//taxa
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;

	private boolean useSingleClassification = false;
	private boolean includeFlatClassifications = false;  //concepts with no taxon relationship (even no misapplied name or synonym rel)
	private boolean includeAllNonMisappliedRelatedClassifications = true;  //all concepts with any relationship except for misapplied name relationships
	private boolean useLastScrutinyAsSec = false;

	private boolean warnForDifferingSynonymReference = true;   //do not warn for E+M as it uses last scrutiny

	//references
	private boolean doSourceNumber = false;

    //nullValues
    private boolean ignoreNull = false;

	//occurrences
	private boolean isSplitTdwgCodes = true;

	private boolean useEmAreaVocabulary = false;

	private boolean includesEmCode = true;  // in Campanula we do not have an EMCOde
	private boolean allowInfraSpecTaxonRank = true;

	private Method namerelationshipTypeMethod;
	private Method uuidForDefTermMethod;
	private Method nameTypeDesignationStatusMethod;

	private boolean logNotMatchingOldNames = false;
	private boolean logMatchingNotExportedOldNames = true;
	private boolean checkOldNameIsSynonym = false;
	private boolean includeMANsForOldNameCheck = false;

	// NameFact stuff
	private URL mediaUrl;
	private File mediaPath;
	private int maximumNumberOfNameFacts;
	private boolean isIgnore0AuthorTeam = false;

	private boolean switchSpeciesGroup = false;

	//Term labels
	private String infrGenericRankAbbrev = null;
	private String infrSpecificRankAbbrev = null;

	private boolean removeHttpMapsAnchor = false;

	//Data Filter
	private String taxonTable = "PTaxon";
	private String classificationQuery = null;
	private String relTaxaIdQuery = null;
	private String nameIdTable = null;
	private String referenceIdTable = null;
	private String authorTeamFilter = null;
	private String authorFilter = null;
	private String factFilter = null;
	private String refDetailFilter = null;
	private String commonNameFilter = null;
	private String occurrenceFilter = null;
	private String occurrenceSourceFilter = null;
	private String webMarkerFilter = null;

	//specific functions
	private Method 	makeUrlForTaxon = null;

	private UUID featureTreeUuid;
	private String featureTreeTitle;

    private boolean isTaxonNoteAsFeature = false;

    private boolean doPreliminaryRefDetailsWithNames = false;


    public boolean isTaxonNoteAsFeature() {return isTaxonNoteAsFeature;}
    public void setTaxonNoteAsFeature(boolean isTaxonNoteAsFeature) {this.isTaxonNoteAsFeature = isTaxonNoteAsFeature;}

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
			BerlinModelGeneralImportValidator.class
			, BerlinModelUserImport.class
			, BerlinModelAuthorImport.class
			, BerlinModelAuthorTeamImport.class
			, BerlinModelRefDetailImport.class
			, BerlinModelReferenceImport.class
			, BerlinModelTaxonNameImport.class
			, BerlinModelTaxonNameRelationImport.class
			, BerlinModelNameStatusImport.class
			, BerlinModelNameFactsImport.class
			, BerlinModelTypesImport.class
			, BerlinModelTaxonImport.class
			, BerlinModelAreaImport.class
			, BerlinModelFactsImport.class
			, BerlinModelTaxonRelationImport.class
			, BerlinModelOccurrenceImport.class
			, BerlinModelOccurrenceSourceImport.class
			, BerlinModelWebMarkerCategoryImport.class
			, BerlinModelWebMarkerImport.class
            , BerlinModelCommonNamesImport.class
		};
	}

	@Override
    public BerlinModelImportState getNewState() {
		return new BerlinModelImportState(this);
	}

	protected BerlinModelImportConfigurator(Source berlinModelSource, ICdmDataSource destination) {
	   super(berlinModelSource, destination, NomenclaturalCode.ICNAFP, defaultTransformer); //default for Berlin Model
	}

	/**
	 * Import name relationships yes/no?.
	 */
	public boolean isDoRelNames() {
		return doRelNames;
	}
	public void setDoRelNames(boolean doRelNames) {
		this.doRelNames = doRelNames;
	}

	public URL getMediaUrl() {
		return mediaUrl;
	}
	public void setMediaUrl(URL mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public File getMediaPath() {
		return mediaPath;
	}
	public void setMediaPath(File mediaPath) {
		this.mediaPath = mediaPath;
	}

	public void setMediaPath(String mediaPathString){
		this.mediaPath = new File(mediaPathString);
	}

	public void setMediaUrl(String mediaUrlString) {
		try {
			this.mediaUrl = new URL(mediaUrlString);
		} catch (MalformedURLException e) {
			logger.error("Could not set mediaUrl because it was malformed: " + mediaUrlString);
		}
	}

	/**
	 * @return the maximumNumberOfNameFacts
	 */
	public int getMaximumNumberOfNameFacts() {
		return maximumNumberOfNameFacts;
	}

	/**
	 * set to 0 for unlimited
	 *
	 * @param maximumNumberOfNameFacts the maximumNumberOfNameFacts to set
	 */
	public void setMaximumNumberOfNameFacts(int maximumNumberOfNameFacts) {
		this.maximumNumberOfNameFacts = maximumNumberOfNameFacts;
	}

    /**
     * If true, no errors occur if objects are not found that should exist. This may
     * be needed e.g. when only subsets of the data are imported.
     * Default value is <cod>false</code>.
     * @return the ignoreNull
     */
    public boolean isIgnoreNull() {
        return ignoreNull;
    }
    public void setIgnoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

	/**
	 * If true, an authorTeam with authorTeamId = 0 is not imported (casus Salvador)
	 * @return the isIgnore0AuthorTeam
	 */
	public boolean isIgnore0AuthorTeam() {
		return isIgnore0AuthorTeam;
	}

	/**
	 * @param isIgnore0AuthorTeam the isIgnore0AuthorTeam to set
	 */
	public void setIgnore0AuthorTeam(boolean isIgnore0AuthorTeam) {
		this.isIgnore0AuthorTeam = isIgnore0AuthorTeam;
	}

	/**
	 * @return the namerelationshipTypeMethod
	 */
	public Method getNamerelationshipTypeMethod() {
		return namerelationshipTypeMethod;
	}

	/**
	 * @param namerelationshipTypeMethod the namerelationshipTypeMethod to set
	 */
	public void setNamerelationshipTypeMethod(Method namerelationshipTypeMethod) {
		this.namerelationshipTypeMethod = namerelationshipTypeMethod;
	}

	/**
	 * @return the taxonPublishMarker
	 */
	public BerlinModelTaxonImport.PublishMarkerChooser getTaxonPublishMarker() {
		return taxonPublishMarker;
	}

	/**
	 * @param taxonPublishMarker the taxonPublishMarker to set
	 */
	public void setTaxonPublishMarker(
			BerlinModelTaxonImport.PublishMarkerChooser taxonPublishMarker) {
		this.taxonPublishMarker = taxonPublishMarker;
	}



	/**
	 * @return the uuidForDefTermMethod
	 */
	public Method getUuidForDefTermMethod() {
		return uuidForDefTermMethod;
	}

	/**
	 * @param uuidForDefTermMethod the uuidForDefTermMethod to set
	 */
	public void setUuidForDefTermMethod(Method uuidForDefTermMethod) {
		this.uuidForDefTermMethod = uuidForDefTermMethod;
	}




	/**
	 * @return the nameTypeDesignationStatusMethod
	 */
	public Method getNameTypeDesignationStatusMethod() {
		return nameTypeDesignationStatusMethod;
	}


	/**
	 * @param nameTypeDesignationStatusMethod the nameTypeDesignationStatusMethod to set
	 */
	public void setNameTypeDesignationStatusMethod(
			Method nameTypeDesignationStatusMethod) {
		this.nameTypeDesignationStatusMethod = nameTypeDesignationStatusMethod;
	}

	public boolean isDoNameStatus() {
		return doNameStatus;
	}
	public void setDoNameStatus(boolean doNameStatus) {
		this.doNameStatus = doNameStatus;
	}


	public boolean isDoCommonNames() {
		return doCommonNames;
	}


	/**
	 * @param doCommonNames
	 */
	public void setDoCommonNames(boolean doCommonNames) {
		this.doCommonNames = doCommonNames;

	}

	public boolean isDoFacts() {
		return doFacts;
	}
	public void setDoFacts(boolean doFacts) {
		this.doFacts = doFacts;
	}


	public boolean isDoOccurrence() {
		return doOccurrence;
	}
	public void setDoOccurrence(boolean doOccurrence) {
		this.doOccurrence = doOccurrence;
	}

    public boolean isDoOccurrenceSources() {
        return doOccurrenceSources;
    }
    public void setDoOccurrenceSources(boolean doOccurrenceSources) {
        this.doOccurrenceSources = doOccurrenceSources;
	}


	public boolean isDoMarker() {
		return doMarker;
	}

	public void setDoMarker(boolean doMarker) {
		this.doMarker = doMarker;
	}

	public boolean isDoUser() {
		return doUser;
	}
	public void setDoUser(boolean doUser) {
		this.doUser = doUser;
	}

	public boolean isDoNameFacts() {
		return doNameFacts;
	}
	public void setDoNameFacts(boolean doNameFacts) {
		this.doNameFacts = doNameFacts;
	}

	public boolean isDoAuthors() {
		return doAuthors;
	}
	public void setDoAuthors(boolean doAuthors) {
		this.doAuthors = doAuthors;
	}

	public DO_REFERENCES getDoReferences() {
		return doReferences;
	}
	public void setDoReferences(DO_REFERENCES doReferences) {
		this.doReferences = doReferences;
	}

	public boolean isDoTaxonNames() {
		return doTaxonNames;
	}
	public void setDoTaxonNames(boolean doTaxonNames) {
		this.doTaxonNames = doTaxonNames;
	}

	public boolean isDoTypes() {
		return doTypes;
	}
	public void setDoTypes(boolean doTypes) {
		this.doTypes = doTypes;
	}

	public boolean isDoTaxa() {
		return doTaxa;
	}
	public void setDoTaxa(boolean doTaxa) {
		this.doTaxa = doTaxa;
	}

	public boolean isDoRelTaxa() {
		return doRelTaxa;
	}
	public void setDoRelTaxa(boolean doRelTaxa) {
		this.doRelTaxa = doRelTaxa;
	}


	public String getTaxonTable() {
		return this.taxonTable ;
	}
	public void setTaxonTable(String taxonTable) {
		this.taxonTable = taxonTable;
	}

	public String getClassificationQuery() {
		return this.classificationQuery ;
	}
	public void setClassificationQuery(String classificationQuery) {
		this.classificationQuery = classificationQuery;
	}

	public void setRelTaxaIdQuery(String relTaxaIdQuery) {
		this.relTaxaIdQuery = relTaxaIdQuery;
	}
	public String getRelTaxaIdQuery() {
		return this.relTaxaIdQuery ;
	}

	public String getNameIdTable() {
		return nameIdTable;
	}
	public void setNameIdTable(String nameIdTable) {
		this.nameIdTable = nameIdTable;
	}

	public String getReferenceIdTable() {
	    return referenceIdTable;
	}
	public void setReferenceIdTable(String referenceIdTable) {
		this.referenceIdTable = referenceIdTable;
	}

	public String getFactFilter() {
	    return factFilter;
	}
	public void setFactFilter(String factFilter) {
		this.factFilter = factFilter;
	}

	public String getRefDetailFilter() {
	    return refDetailFilter;
	}
	public void setRefDetailFilter(String refDetailFilter) {
		this.refDetailFilter = refDetailFilter;
	}



	public String getOccurrenceFilter() {
		return occurrenceFilter;
	}
	public void setOccurrenceFilter(String occurrenceFilter) {
		this.occurrenceFilter = occurrenceFilter;
	}



	public String getCommonNameFilter() {
		return commonNameFilter;
	}
	public void setCommonNameFilter(String commonNameFilter) {
		this.commonNameFilter = commonNameFilter;
	}



	public String getOccurrenceSourceFilter() {
		return occurrenceSourceFilter;
	}
	public void setOccurrenceSourceFilter(String occurrenceSourceFilter) {
		this.occurrenceSourceFilter = occurrenceSourceFilter;
	}



	public String getWebMarkerFilter() {
		return webMarkerFilter;
	}
	public void setWebMarkerFilter(String webMarkerFilter) {
		this.webMarkerFilter = webMarkerFilter;
	}


	public boolean isUseSingleClassification() {
		return useSingleClassification;
	}
	public void setUseSingleClassification(boolean useSingleClassification) {
		this.useSingleClassification = useSingleClassification;
	}

	public String getAuthorTeamFilter() {
	    return authorTeamFilter;
	}
	public void setAuthorTeamFilter(String authorTeamFilter) {
		this.authorTeamFilter = authorTeamFilter;
	}

	public String getAuthorFilter() {
		return authorFilter;
	}
	public void setAuthorFilter(String authorFilter) {
		this.authorFilter = authorFilter;
	}

	public boolean isSwitchSpeciesGroup() {
		return switchSpeciesGroup;
	}
	/**
	 * If true, the rankId for speicesGroup is changed from 59 to 57 and
	 * 59 is used for coll. species instead
	 * @param switchSpeciesGroup
	 */
	public void setSwitchSpeciesGroup(boolean switchSpeciesGroup) {
		this.switchSpeciesGroup = switchSpeciesGroup;
	}


	public boolean isSplitTdwgCodes() {
		return isSplitTdwgCodes;
	}
	public void setSplitTdwgCodes(boolean isSplitTdwgCodes) {
		this.isSplitTdwgCodes = isSplitTdwgCodes;
	}


	public Method getMakeUrlForTaxon() {
		return makeUrlForTaxon;
	}
	public void setMakeUrlForTaxon(Method makeUrlForTaxon) {
		this.makeUrlForTaxon = makeUrlForTaxon;
	}


	public String getInfrGenericRankAbbrev() {
		return infrGenericRankAbbrev;
	}
	public void setInfrGenericRankAbbrev(String infrGenericRankAbbrev) {
		this.infrGenericRankAbbrev = infrGenericRankAbbrev;
	}


	public String getInfrSpecificRankAbbrev() {
		return infrSpecificRankAbbrev;
	}
	public void setInfrSpecificRankAbbrev(String infrSpecificRankAbbrev) {
		this.infrSpecificRankAbbrev = infrSpecificRankAbbrev;
	}


	public boolean isRemoveHttpMapsAnchor() {
		return removeHttpMapsAnchor;
	}
	public void setRemoveHttpMapsAnchor(boolean removeHttpMapsAnchor) {
		this.removeHttpMapsAnchor = removeHttpMapsAnchor;
	}


	public boolean isIncludeFlatClassifications() {
		return includeFlatClassifications;
	}
	public void setIncludeFlatClassifications(boolean includeFlatClassifications) {
		this.includeFlatClassifications = includeFlatClassifications;
	}


	public boolean isIncludesAreaEmCode() {
		return includesEmCode;
	}
	public void setIncludesEmCode(boolean includesEmCode) {
		this.includesEmCode = includesEmCode;

	}


	public boolean isAllowInfraSpecTaxonRank() {
		return allowInfraSpecTaxonRank ;
	}
	public void setAllowInfraSpecTaxonRank(boolean allowInfraSpecTaxonRank) {
		this.allowInfraSpecTaxonRank = allowInfraSpecTaxonRank;
	}


	public boolean isIncludeAllNonMisappliedRelatedClassifications() {
		return includeAllNonMisappliedRelatedClassifications;
	}
	public void setIncludeAllNonMisappliedRelatedClassifications(boolean includeAllNonMisappliedRelatedClassifications) {
		this.includeAllNonMisappliedRelatedClassifications = includeAllNonMisappliedRelatedClassifications;
	}


	public boolean isUseEmAreaVocabulary() {
		return useEmAreaVocabulary;
	}
	public void setUseEmAreaVocabulary(boolean useEmAreaVocabulary) {
		this.useEmAreaVocabulary = useEmAreaVocabulary;
	}


    public boolean isSalvador() {return isSalvador;}
    public void setSalvador(boolean isSalvador) {this.isSalvador = isSalvador;}

    public boolean isEuroMed() {return isEuroMed;}
    public void setEuroMed(boolean isEuroMed) {this.isEuroMed = isEuroMed;}

    public boolean isMcl() {return isMcl;}
    public void setMcl(boolean isMcl) {this.isMcl = isMcl;}

    public boolean isMoose() {return isMoose;}
    public void setMoose(boolean isMoose) {this.isMoose = isMoose;}

    public UUID getFeatureTreeUuid() {
        return featureTreeUuid;
    }
    public void setFeatureTreeUuid(UUID featureTreeUuid) {
        this.featureTreeUuid = featureTreeUuid;
    }

    @Override
    public String getFeatureTreeTitle() {
        return featureTreeTitle;
    }
    @Override
    public void setFeatureTreeTitle(String featureTreeTitle) {
        this.featureTreeTitle = featureTreeTitle;
    }

    public boolean isUseLastScrutinyAsSec() {
        return useLastScrutinyAsSec;
    }
    public void setUseLastScrutinyAsSec(boolean useLastScrutinyAsSec) {
        this.useLastScrutinyAsSec = useLastScrutinyAsSec;
    }


    public boolean isDoPreliminaryRefDetailsWithNames() {
        return doPreliminaryRefDetailsWithNames;
    }
    public void setDoPreliminaryRefDetailsWithNames(boolean doPreliminaryRefDetailsWithNames) {
        this.doPreliminaryRefDetailsWithNames = doPreliminaryRefDetailsWithNames;
    }

    public boolean isWarnForDifferingSynonymReference() {
        return warnForDifferingSynonymReference;
    }
    public void setWarnForDifferingSynonymReference(boolean warnForDifferingSynonymReference) {
        this.warnForDifferingSynonymReference = warnForDifferingSynonymReference;
    }

    public boolean isLogNotMatchingOldNames() {
        return logNotMatchingOldNames;
    }
    public void setLogNotMatchingOldNames(boolean logNotMatchingOldNames) {
        this.logNotMatchingOldNames = logNotMatchingOldNames;
    }

    public boolean isCheckOldNameIsSynonym() {
        return checkOldNameIsSynonym;
    }
    public void setCheckOldNameIsSynonym(boolean checkOldNameIsSynonym) {
        this.checkOldNameIsSynonym = checkOldNameIsSynonym;
    }

    public boolean isLogMatchingNotExportedOldNames() {
        return logMatchingNotExportedOldNames;
    }
    public void setLogMatchingNotExportedOldNames(boolean logMatchingNotExportedOldNames) {
        this.logMatchingNotExportedOldNames = logMatchingNotExportedOldNames;
    }

    public boolean isDoSourceNumber() {
        return doSourceNumber;
    }
    public void setDoSourceNumber(boolean doSourceNumber) {
        this.doSourceNumber = doSourceNumber;
    }
    public boolean isDoNamedAreas() {
        return doNamedAreas;
    }
    public void setDoNamedAreas(boolean doNamedAreas) {
        this.doNamedAreas = doNamedAreas;
    }

    public boolean isIncludeMANsForOldNameCheck() {
        return includeMANsForOldNameCheck;
    }
    public void setIncludeMANsForOldNameCheck(boolean includeMANsForOldNameCheck) {
        this.includeMANsForOldNameCheck = includeMANsForOldNameCheck;
    }

    public boolean isUseClassification() {
        return useClassification;
    }
    public void setUseClassification(boolean useClassification) {
        this.useClassification = useClassification;
    }

}
