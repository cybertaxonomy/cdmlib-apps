/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.berlinModelImport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.FirstDataInserter;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.api.service.IGroupService;
import eu.etaxonomy.cdm.api.service.description.AggregationMode;
import eu.etaxonomy.cdm.api.service.description.DistributionAggregation;
import eu.etaxonomy.cdm.api.service.description.DistributionAggregationConfiguration;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.ext.geo.IEditGeoService;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.metadata.CdmPreference;
import eu.etaxonomy.cdm.model.metadata.PreferencePredicate;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.permission.GrantedAuthorityImpl;
import eu.etaxonomy.cdm.model.permission.Group;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.persistence.permission.Role;
import eu.etaxonomy.cdm.persistence.query.MatchMode;
import eu.etaxonomy.cdm.persistence.query.OrderHint;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to
 * dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 * @author a.mueller
 */
public class EuroMedActivator {
	private static final Logger logger = Logger.getLogger(EuroMedActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
//  static final Source berlinModelSource = BerlinModelSources.euroMed_Pub2();
	static final Source berlinModelSource = BerlinModelSources.euroMed_BGBM42();
//	static final Source berlinModelSource = BerlinModelSources.euroMed_PESI3();
//
    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_euromed();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_euromed2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_euroMed();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final boolean doUser = true;
//  //authors
    static final boolean doAuthors = true;
    //references
    static final DO_REFERENCES doReferences = DO_REFERENCES.ALL;
    //names
    static final boolean doTaxonNames = true;
    static final boolean doRelNames = true;
    static final boolean doNameStatus = true;
    static final boolean doNameFacts = true;

    //taxa
    static final boolean doTaxa = true;
    static final boolean doFacts = true;
    static final boolean doRelTaxa = true;
    static final boolean doOccurrences = true;
    static final boolean doOccurrenceSources = true;
    static final boolean doCommonNames = true;

    static final boolean doNamedAreas = true;

  //serious types do not exist in E+M except for name types which are handled in name relations
    static final boolean doTypes = false;  //serious types do not exist in E+M except for name types which are handled in name relations


    static final boolean doRunTransmissionEngine = false; // (hbm2dll == DbSchemaValidation.VALIDATE);

    //etc.
    static final boolean doMarker = false;  //no relevant markers exist

    boolean invers = !(hbm2dll == DbSchemaValidation.CREATE);

    boolean doPreliminaryRefDetailsWithNames = true;

    boolean logNotMatchingOldNames = false;
    boolean logMatchingNotExportedOldNames = false;  //true
    boolean checkOldNameIsSynonym = false;
    boolean includeMANsForOldNameCheck = true;

	static final boolean includePesiExport = false;

	static final int sourceSecId = 7000000; //500000
	static final UUID classificationUuid = UUID.fromString("314a68f9-8449-495a-91c2-92fde8bcf344");
	static final boolean useSingleClassification = true;
	static final String classificationName = "Euro+Med 2018";
	static final UUID featureTreeUuid = UUID.fromString("6a5e1c2b-ec0d-46c8-9c7d-a2059267ffb7");
	static final Object[] featureKeyList = new Integer[]{1, 31, 4, 98, 41};

	// set to zero for unlimited nameFacts
	static final int maximumNumberOfNameFacts = 0;

	static final int partitionSize = 2500;


	//editor - import
	static final EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;

	//ignore null
	static final boolean ignoreNull = false;

	static final boolean switchSpeciesGroup = true;

	static boolean useClassification = true;

	static boolean isSplitTdwgCodes = false;
	static boolean useEmAreaVocabulary = true;

	private final boolean removeHttpMapsAnchor = true;


	static final String infrGenericRankAbbrev = "[unranked]";
	static final String infrSpecificRankAbbrev = "[unranked]";

	static boolean useLastScrutinyAsSec = true;
	static boolean warnForDifferingSynonymReference = false;


	static String taxonTable = "v_cdm_exp_taxaAll";
	static String classificationQuery = " SELECT DISTINCT t.PTRefFk, r.RefCache FROM PTaxon t INNER JOIN Reference r ON t.PTRefFk = r.RefId WHERE t.PTRefFk = " + sourceSecId;
	static String relPTaxonIdQuery = " SELECT r.RelPTaxonId "
					+ " FROM RelPTaxon AS r "
					+ "   INNER JOIN v_cdm_exp_taxaDirect AS a ON r.PTNameFk2 = a.PTNameFk AND r.PTRefFk2 = a.PTRefFk "
					+ "   INNER JOIN PTaxon As pt1 ON pt1.PTNameFk = r.PTNameFk1 AND pt1.PTRefFk = r.PTRefFk1 "
					+ " WHERE r.RelPTaxonID NOT IN (1874890,1874959,1874932,1874793,1874956,1874971,1874902,1874696) " //Relations to unpublished Kew genus taxa of Bethulaceae which are not imported anymore, but Bethalaceae is still imported
					+ "     AND NOT (pt1.PTRefFk = 8000000 AND pt1.publishFlag = 0) ";
	static String nameIdTable = " v_cdm_exp_namesAll ";
	static String referenceIdTable = " v_cdm_exp_refAll ";
	static String refDetailFilter =  " RefDetailID IN (SELECT RefDetailID FROM v_cdm_exp_RefDetail) ";
	static String factFilter = " factId IN ( SELECT factId FROM v_cdm_exp_factsAll WHERE FactCategoryFk NOT IN (12, 13, 14, 249, 251))";
	static String occurrenceFilter = " occurrenceId IN ( SELECT occurrenceId FROM v_cdm_exp_occurrenceAll )";
	static String occurrenceSourceFilter = " occurrenceFk IN ( SELECT occurrenceId FROM v_cdm_exp_occurrenceAll )";
	static String commonNameFilter = " commonNameId IN ( SELECT commonNameId FROM v_cdm_exp_commonNamesAll )";
	static String webMarkerFilter = " TableNameFk <> 500 OR ( RIdentifierFk IN (SELECT RIdentifier FROM v_cdm_exp_taxaAll)) ";
	static String authorTeamFilter =  " authorTeamId IN (SELECT drvTab.authorTeamId FROM (SELECT authorTeamId FROM v_cdm_exp_authorTeamsAll) as drvTab) ";
	static String authorFilter = " authorId IN (SELECT drvTab.authorId FROM (SELECT authorId FROM v_cdm_exp_authorsAll) as drvTab) ";



// **************** ALL *********************


	public void importEm2CDM (Source source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") to " + cdmDestination.getDatabase() + " ...");
		//make BerlinModel Source

		BerlinModelImportConfigurator config = BerlinModelImportConfigurator.NewInstance(source,  destination);

		config.setClassificationName(classificationName);

		config.setClassificationUuid(classificationUuid);
		config.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);
		config.setIgnoreNull(ignoreNull);

		config.setDoAuthors(doAuthors ^ invers);
		config.setDoReferences(invers ? doReferences.invers() : doReferences);
		config.setDoTaxonNames(doTaxonNames ^ invers);
		config.setDoRelNames(doRelNames ^ invers);
		config.setDoNameStatus(doNameStatus ^ invers);
		config.setDoTypes(doTypes);  //always false
		config.setDoNameFacts(doNameFacts ^ invers);
		config.setDoTaxa(doTaxa ^ invers);
		config.setDoRelTaxa(doRelTaxa ^ invers);
		config.setDoFacts(doFacts ^ invers);
		config.setDoOccurrence(doOccurrences ^ invers);
		config.setDoOccurrenceSources(doOccurrenceSources ^ invers);
        config.setDoCommonNames(doCommonNames ^ invers);
        config.setDoNamedAreas(doNamedAreas ^ invers);

		config.setDoMarker(doMarker);
		config.setDoUser(doUser ^ invers);

		config.setEuroMed(true);
		config.setDoSourceNumber(true);

		config.setLogNotMatchingOldNames(logNotMatchingOldNames);
		config.setLogMatchingNotExportedOldNames(logMatchingNotExportedOldNames);
		config.setCheckOldNameIsSynonym(checkOldNameIsSynonym);
		config.setIncludeMANsForOldNameCheck(includeMANsForOldNameCheck);

		config.setUseClassification(useClassification);
		config.setSourceRefUuid(BerlinModelTransformer.uuidSourceRefEuroMed);
		config.setEditor(editor);
		config.setDbSchemaValidation(hbm2dll);
		config.setUseLastScrutinyAsSec(useLastScrutinyAsSec);
		config.setWarnForDifferingSynonymReference(warnForDifferingSynonymReference);

		// maximum number of name facts to import
		config.setMaximumNumberOfNameFacts(maximumNumberOfNameFacts);

		config.setInfrGenericRankAbbrev(infrGenericRankAbbrev);
		config.setInfrSpecificRankAbbrev(infrSpecificRankAbbrev);
		config.setRemoveHttpMapsAnchor(removeHttpMapsAnchor);

		config.setDoPreliminaryRefDetailsWithNames(doPreliminaryRefDetailsWithNames);

//		filter
		config.setTaxonTable(taxonTable);
		config.setClassificationQuery(classificationQuery);
		config.setRelTaxaIdQuery(relPTaxonIdQuery);
		config.setNameIdTable(nameIdTable);
		config.setReferenceIdTable(referenceIdTable);
		config.setAuthorTeamFilter(authorTeamFilter);
		config.setAuthorFilter(authorFilter);
		config.setFactFilter(factFilter);
		config.setRefDetailFilter(refDetailFilter);
		config.setCommonNameFilter(commonNameFilter);
		config.setOccurrenceFilter(occurrenceFilter);
		config.setOccurrenceSourceFilter(occurrenceSourceFilter);
		config.setWebMarkerFilter(webMarkerFilter);
		config.setUseSingleClassification(useSingleClassification);

		//TDWG codes
		config.setSplitTdwgCodes(isSplitTdwgCodes);
		config.setUseEmAreaVocabulary(useEmAreaVocabulary);

		config.setCheck(check);
		config.setEditor(editor);
		config.setRecordsPerTransaction(partitionSize);

		config.setSwitchSpeciesGroup(switchSpeciesGroup);

		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<>();
		bmImport.invoke(config);

//		renameRanks(config, bmImport);

		createFeatureTree(config, bmImport);

		changeCommonNameLabel(config, bmImport);

		createUsersAndRoles(config, bmImport);

        runTransmissionEngine(config, bmImport);

        importShapefile(config, bmImport);

//      createPreferences(config, bmImport); => manual

//        markAreasAsHidden(config, bmImport);  //has been moved to BM occurrence import

		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
	}

    private void createPreferences(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        if (config.isDoUser() && (config.getCheck().isImport() )){
            ICdmRepository app = bmImport.getCdmAppController();

            //area vocs
            CdmPreference preference = CdmPreference.NewTaxEditorInstance(PreferencePredicate.AvailableDistributionAreaVocabularies, BerlinModelTransformer.uuidVocCaucasusAreas.toString()+";" + BerlinModelTransformer.uuidVocEuroMedAreas.toString());
            preference.setAllowOverride(false);
            app.getPreferenceService().set(preference);

            //occ status list
            String status ="42946bd6-9c22-45ad-a910-7427e8f60bfd;9eb99fe6-59e2-4445-8e6a-478365bd0fa9;c3ee7048-15b7-4be1-b687-9ce9c1a669d6;643cf9d1-a5f1-4622-9837-82ef961e880b;0c54761e-4887-4788-9dfa-7190c88746e3;83eb0aa0-1a45-495a-a3ca-bf6958b74366;aeec2947-2700-4623-8e32-9e3a430569d1;ddeac4f2-d8fa-43b8-ad7e-ca13abdd32c7;310373bf-7df4-4d02-8cb3-bcc7448805fc;5c397f7b-59ef-4c11-a33c-45691ceda91b;925662c1-bb10-459a-8c53-da5a738ac770;61cee840-801e-41d8-bead-015ad866c2f1;e191e89a-a751-4b0c-b883-7f1de70915c9";
            CdmPreference statusListPref = CdmPreference.NewTaxEditorInstance(PreferencePredicate.AvailableDistributionAreaVocabularies, status);
            statusListPref.setAllowOverride(false);
            app.getPreferenceService().set(statusListPref);

            //distr. editor activated
            CdmPreference distrEditorActive = CdmPreference.NewTaxEditorInstance(PreferencePredicate.DistributionEditorActivated, "true");
            statusListPref.setAllowOverride(true);
            app.getPreferenceService().set(distrEditorActive);

//            //idInVoc for areas
//            CdmPreference distrEditorShowIdInVocForAreas = CdmPreference.NewTaxEditorInstance(PreferencePredicate.Sho.ShowIdInVocabulary, "true");
//            distrEditorShowIdInVocForAreas.setAllowOverride(true);
//            app.getPreferenceService().set(distrEditorShowIdInVocForAreas);
//
//            //areas sort order
//            //?? correct?
//            CdmPreference distrEditorSorted = CdmPreference.NewTaxEditorInstance(PreferencePredicate.AreasSortedByIdInVocabulary, "true");
//            distrEditorSorted.setAllowOverride(true);
//            app.getPreferenceService().set(distrEditorSorted);
//
//            //distr. status uses symbol
//            //?? correct?
//            CdmPreference distrEditorStatusUseSymbols = CdmPreference.NewTaxEditorInstance(PreferencePredicate.ShowSymbolForStatus, "false");
//            distrEditorStatusUseSymbols.setAllowOverride(true);
//            app.getPreferenceService().set(distrEditorStatusUseSymbols);

            //media view
            CdmPreference showMediaView = CdmPreference.NewTaxEditorInstance(PreferencePredicate.ShowMediaView, "false");
            showMediaView.setAllowOverride(false);
            app.getPreferenceService().set(showMediaView);

            //multi classification
            CdmPreference multiClassification = CdmPreference.NewTaxEditorInstance(PreferencePredicate.DisableMultiClassification, "true");
            multiClassification.setAllowOverride(false);
            app.getPreferenceService().set(multiClassification);

            //taxon node wizard
            CdmPreference showTaxonNodeWizard = CdmPreference.NewTaxEditorInstance(PreferencePredicate.ShowTaxonNodeWizard, "false");
            showTaxonNodeWizard.setAllowOverride(false);
            app.getPreferenceService().set(showTaxonNodeWizard);

            //import+export
            CdmPreference showImportExportMenu = CdmPreference.NewTaxEditorInstance(PreferencePredicate.ShowImportExportMenu, "false");
            showImportExportMenu.setAllowOverride(true);
            app.getPreferenceService().set(showImportExportMenu);

            //show specimen
            CdmPreference showSpecimen = CdmPreference.NewTaxEditorInstance(PreferencePredicate.ShowSpecimen, "false");
            showSpecimen.setAllowOverride(false);
            app.getPreferenceService().set(showSpecimen);

        }

    }

    //Rename Ranks (still needed?)
    private void renameRanks(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        if (config.isDoTaxonNames() && (config.getCheck().isImport() )  ){
			ICdmRepository app = bmImport.getCdmAppController();
			TransactionStatus tx = app.startTransaction();
			try {
				Rank sectBot = (Rank)app.getTermService().find(Rank.SECTION_BOTANY().getUuid());
				Representation repr = sectBot.getRepresentation(Language.ENGLISH());
				repr.setAbbreviatedLabel(repr.getAbbreviatedLabel().replace("(bot.)", "").trim());
				repr.setLabel(repr.getLabel().replace("(Botany)", "").trim());
				sectBot.setTitleCache(null, false);  //to definitely update the titleCache also
				app.getTermService().saveOrUpdate(sectBot);

				Rank subSectBot = (Rank)app.getTermService().find(Rank.SECTION_BOTANY().getUuid());
				repr = subSectBot.getRepresentation(Language.ENGLISH());
				repr.setAbbreviatedLabel(repr.getAbbreviatedLabel().replace("(bot.)", "").trim());
				repr.setLabel(repr.getLabel().replace("(Botany)", "").trim());
				subSectBot.setTitleCache(null, false);  //to definitely update the titleCache also
				app.getTermService().saveOrUpdate(subSectBot);
				app.commitTransaction(tx);
			} catch (Exception e) {
			    e.printStackTrace();
                logger.error("Exception in renameRanks: " + e.getMessage());
			}
		}
    }

    //create feature tree
    private void createFeatureTree(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport){
	    if (config.isDoFacts() && (config.getCheck().isImport()  )  ){
			try {
                ICdmRepository app = bmImport.getCdmAppController();
                TransactionStatus tx = app.startTransaction();

                //make feature tree
                TermTree<Feature> tree = TreeCreator.flatTree(featureTreeUuid, config.getFeatureMap(), featureKeyList);
                tree.setTitleCache("Euro+Med Feature Tree", true);
                tree.getRoot().addChild(Feature.IMAGE());
                tree.getRoot().addChild(Feature.DISTRIBUTION(), 1);
                tree.getRoot().addChild(Feature.COMMON_NAME(), 2);
                app.getFeatureTreeService().saveOrUpdate(tree);

                app.commitTransaction(tx);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in createFeatureTree: " + e.getMessage());
            }
		}
    }

    //Change common name label
    private void changeCommonNameLabel(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {
	    if (config.isDoFacts() && (config.getCheck().isImport()  )  ){
	        try {
                ICdmRepository app = bmImport.getCdmAppController();
                TransactionStatus tx = app.startTransaction();

                DefinedTermBase<?> commonNameFeature = app.getTermService().find(Feature.COMMON_NAME().getUuid());
                commonNameFeature.setLabel("Common Names", Language.ENGLISH());
                commonNameFeature.setTitleCache(null, false);  //to definitely update the titleCache also
                app.getTermService().saveOrUpdate(commonNameFeature);

                app.commitTransaction(tx);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in changeCommonNameLabel: " + e.getMessage());
            }
	    }
    }

    //1. run transmission engine #3979
    private void runTransmissionEngine(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        if (doRunTransmissionEngine && (config.getCheck().isImport()  )  ){
            try {
                ICdmRepository app = bmImport.getCdmAppController();

                final List<String> term_init_strategy = Arrays.asList(new String []{
                        "representations"
                });

                UUID uuidSuperAreaLevel = BerlinModelTransformer.uuidEuroMedAreaLevelFirst;
                NamedAreaLevel euroMedLevel1 = (NamedAreaLevel)app.getTermService().find(uuidSuperAreaLevel);

                Pager<NamedArea> areaPager = app.getTermService().list(
                        euroMedLevel1,
                        (NamedAreaType) null,
                        null,
                        null,
                        (List<OrderHint>) null,
                        term_init_strategy);
                DistributionAggregation distributionAggregation = new DistributionAggregation();

                Set<UUID> classificationUuids = new HashSet<>();
                TaxonNodeFilter filter = TaxonNodeFilter.NewInstance(classificationUuids, null, null, null, null,
                        null, Rank.uuidGenus);

                DistributionAggregationConfiguration aggregationConfig = DistributionAggregationConfiguration
                        .NewInstance(
                        AggregationMode.byAreasAndRanks(),
                        areaPager.getRecords().stream().map(area->area.getUuid()).collect(Collectors.toList()),
                        filter,
                        DefaultProgressMonitor.NewInstance());
                distributionAggregation.invoke(aggregationConfig, app);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in markAreasAsHidden: " + e.getMessage());
            }
        }
    }

//    //5.Mark areas to be hidden #3979 .5
//    private void markAreasAsHidden(BerlinModelImportConfigurator config,
//            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {
//
//        if (config.isDoOccurrence() && (config.getCheck().isImport())){
//	        try {
//                ICdmRepository app = bmImport.getCdmAppController();
//                TransactionStatus tx = app.startTransaction();
//
//                MarkerType hiddenAreaMarkerType = MarkerType.NewInstance("Used to hide distributions for the named areas in publications", "Hidden Area", null);
//                hiddenAreaMarkerType.setUuid(BerlinModelTransformer.uuidHiddenArea);
//                @SuppressWarnings("unchecked")
//                TermVocabulary<MarkerType> vocUserDefinedMarkerTypes = app.getVocabularyService().find(CdmImportBase.uuidUserDefinedMarkerTypeVocabulary);
//                if (vocUserDefinedMarkerTypes == null){
//                    String message = "Marker type vocabulary could not be found. Hidden areas not added.";
//                    logger.error(message);
//                    System.out.println(message);
//                }else{
//                    vocUserDefinedMarkerTypes.addTerm(hiddenAreaMarkerType);
//                    app.getVocabularyService().saveOrUpdate(vocUserDefinedMarkerTypes);
//
//                    //Add hidden area marker to Rs(C) and Rs(N)
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_B);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_C);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_E);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_N);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_K);
//                    hideArea(app, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_W);
//                 }
//                app.commitTransaction(tx);
//            } catch (Exception e) {
//                e.printStackTrace();
//                logger.error("Exception in markAreasAsHidden: " + e.getMessage());
//            }
//	    }
//    }
//
//    private void hideArea(ICdmRepository app, MarkerType hiddenAreaMarkerType, UUID areaUuid) {
//        NamedArea area = (NamedArea)app.getTermService().find(areaUuid);
//        area.addMarker(Marker.NewInstance(hiddenAreaMarkerType, true));
//        app.getTermService().saveOrUpdate(area);
//    }

    //2. import shapefile attributes #3979 .2
    private void importShapefile(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        if (config.isDoOccurrence() && (config.getCheck().isImport())){

	       try {
	           UUID areaVocabularyUuid = BerlinModelTransformer.uuidVocEuroMedAreas;
               List<String> idSearchFields = Arrays.asList(new String[]{"EMAREA","PARENT"});
               String wmsLayerName = "euromed_2013";
               Set<UUID> areaUuidSet = null;

               ICdmRepository app = bmImport.getCdmAppController();
               IEditGeoService geoService = (IEditGeoService)app.getBean("editGeoService");

               Map<NamedArea, String> resultMap;
               try {
                   InputStream in = EuroMedActivator.class.getResourceAsStream("/euromed/euromed_2013.csv");
                   Reader reader = new InputStreamReader(in, "UTF-8");

                   resultMap = geoService.mapShapeFileToNamedAreas(
                               reader, idSearchFields , wmsLayerName , areaVocabularyUuid, areaUuidSet);
                   Map<String, String> flatResultMap = new HashMap<>(resultMap.size());
                   for(NamedArea area : resultMap.keySet()){
                       flatResultMap.put(area.getTitleCache() + " [" + area.getUuid() + "]", resultMap.get(area));
                   }
               } catch (IOException e) {
                    String message = "IOException when reading from mapping file or creating result map.";
                    logger.error(message);
                    System.out.println(message);
               }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in importShapefile: " + e.getMessage());
            }
	    }
    }

    //4. Create users and assign roles  #3979
    private void createUsersAndRoles(BerlinModelImportConfigurator config,
            CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        try {
            if (config.isDoRelTaxa() && (config.getCheck().isImport())){
                ICdmRepository app = bmImport.getCdmAppController();
                TransactionStatus tx = app.startTransaction();

                //eraabstraube
                String eraabstraube = "e.raabstraube";
                List<User> users = app.getUserService().listByUsername(eraabstraube, MatchMode.EXACT, null, null, null, null, null);
                User userEraabStraube;
                if (users.isEmpty()){
                    userEraabStraube = User.NewInstance(eraabstraube, eraabstraube);
                }else{
                    userEraabStraube = users.get(0);
                }
                if (userEraabStraube.getPerson() == null){
                    Person eckhard = Person.NewInstance();
                    eckhard.setFamilyName("von Raab-Straube");
                    eckhard.setGivenName("Eckhard");
                    eckhard.setPrefix("Dr.");
                    userEraabStraube.setPerson(eckhard);
                }
                app.getUserService().saveOrUpdate(userEraabStraube);

                //groups
                Group groupEditor = app.getGroupService().load(Group.GROUP_EDITOR_UUID);
                groupEditor.addMember(userEraabStraube);
                app.getGroupService().saveOrUpdate(groupEditor);

                Group groupProjectManager = app.getGroupService().load(Group.GROUP_PROJECT_MANAGER_UUID);
                groupProjectManager.addMember(userEraabStraube);
                app.getGroupService().saveOrUpdate(groupProjectManager);

                String[] publishRoles = new String[]{Role.ROLE_PUBLISH.toString()};
                Group groupPublisher = checkGroup(app.getGroupService(), Group.GROUP_PUBLISH_UUID, "Publisher", publishRoles);
                groupPublisher.addMember(userEraabStraube);
                app.getGroupService().saveOrUpdate(groupPublisher);

                UUID uuidEuroMedPlantBaseGroup = UUID.fromString("91be42ea-ad04-4458-9836-389277e773db");
                String[] emPlantBaseRoles = new String[]{"TAXONNODE.[CREATE,READ,UPDATE,DELETE]"};
                Group euroMedPlantbase = checkGroup(app.getGroupService(), uuidEuroMedPlantBaseGroup, "Euro+Med Plantbase", emPlantBaseRoles);
                euroMedPlantbase.addMember(userEraabStraube);
                app.getGroupService().saveOrUpdate(euroMedPlantbase);

                //cichorieae-editor
                String cichorieaeEditor = "cichorieae-editor";
                app.getUserService().listByUsername(cichorieaeEditor, MatchMode.EXACT, null, null, null, null, null);
                User userCichEditor;
                if (users.isEmpty()){
                    userCichEditor = User.NewInstance(cichorieaeEditor, cichorieaeEditor);
                }else{
                    userCichEditor = users.get(0);
                }
                app.getUserService().saveOrUpdate(userCichEditor);

                //groups
                groupEditor.addMember(userCichEditor);
                app.getGroupService().saveOrUpdate(groupEditor);

                UUID uuidCichorieaeSubtree = null;
                UUID uuidCichorieae = UUID.fromString("63c7dbeb-b9a2-48b8-a75f-e3fe5e161f7c");
                Taxon cich = (Taxon)app.getTaxonService().find(uuidCichorieae);
                if (cich != null){
                    if (!cich.getTaxonNodes().isEmpty()){
                        TaxonNode cichNode = cich.getTaxonNodes().iterator().next();
                        uuidCichorieaeSubtree = cichNode.getUuid();
                    }
                }

                String[] cichorieaeRoles = new String[]{};
                if (uuidCichorieaeSubtree != null){
                    cichorieaeRoles = new String[]{"TAXONNODE.[CREATE,READ,UPDATE,DELETE]{"+uuidCichorieaeSubtree.toString()+"}"};
                }else{
                    String message = "Cichorieae node could not be found for cichorieae-editor role";
                    logger.warn(message);
                    System.out.println(message);
                }
                UUID uuidCichorieaeGroup = UUID.fromString("a630938d-dd4f-48c2-9406-91def487b11e");
                String cichorieaeGroupName = "Cichorieae";
                Group cichorieaeGroup = checkGroup(app.getGroupService(), uuidCichorieaeGroup, cichorieaeGroupName, cichorieaeRoles);
                cichorieaeGroup.addMember(userCichEditor);
                app.getGroupService().saveOrUpdate(cichorieaeGroup);

                app.commitTransaction(tx);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in createUsersAndRoles: " + e.getMessage());
        }
    }

	/**
	  * copied from {@link FirstDataInserter#checkGroup}
     */
    private Group checkGroup(IGroupService groupService, UUID groupUuid, String groupName, String[] requiredAuthorities) {
        Group group = groupService.load(groupUuid);
        if(group == null){
            group = Group.NewInstance();
            group.setUuid(groupUuid);
            logger.info("New Group '" + groupName + "' created");
        }
        group.setName(groupName); // force name

        Set<GrantedAuthority> grantedAuthorities = group.getGrantedAuthorities();

        for(String a : requiredAuthorities){
            boolean isMissing = true;
            for(GrantedAuthority ga : grantedAuthorities){
                if(a.equals(ga.getAuthority())){
                    isMissing = false;
                    break;
                }
            }
            if(isMissing){
                GrantedAuthorityImpl newGa = GrantedAuthorityImpl.NewInstance(a);
                group.addGrantedAuthority(newGa);
                logger.info("New GrantedAuthority '" + a + "' added  to '" + groupName + "'");
            }
        }
        groupService.saveOrUpdate(group);
        logger.info("Check of group  '" + groupName + "' done");
        return group;
    }


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EuroMedActivator importActivator = new EuroMedActivator();
		Source source = berlinModelSource;
		ICdmDataSource cdmRepository = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		importActivator.importEm2CDM(source, cdmRepository, hbm2dll);
		System.exit(0);
	}

}
