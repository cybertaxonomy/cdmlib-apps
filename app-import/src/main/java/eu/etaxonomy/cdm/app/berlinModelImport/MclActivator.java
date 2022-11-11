/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.berlinModelImport;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.metadata.CdmPreference;
import eu.etaxonomy.cdm.model.metadata.PreferencePredicate;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to
 * dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 * @author a.mueller
 */
public class MclActivator {

    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.mcl();

//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_medchecklist();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_medchecklist();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_medchecklist();

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final boolean doUser = false;
//  //authors
    static final boolean doAuthors = true;
    //references
    static final DO_REFERENCES doReferences = DO_REFERENCES.ALL;
    //names
    static final boolean doTaxonNames = true;
    static final boolean doRelNames = true;
    static final boolean doNameStatus = true;
    static final boolean doNameFacts = false;  //name facts do not exist in MCL

    //taxa
    static final boolean doTaxa = true;
    static final boolean doFacts = true;
    static final boolean doRelTaxa = true;

    static final boolean doNamedAreas = true;

    //serious types do not exist in MCL
    static final boolean doTypes = false;

    static final boolean doRunTransmissionEngine = false; // (hbm2dll == DbSchemaValidation.VALIDATE);

    //etc.
    static final boolean doMarker = false;  //no relevant markers exist

    boolean invers = !(hbm2dll == DbSchemaValidation.CREATE);

    boolean doPreliminaryRefDetailsWithNames = true;

    boolean logNotMatchingOldNames = false;
    boolean logMatchingNotExportedOldNames = false;  //true
    boolean checkOldNameIsSynonym = false;
    boolean includeMANsForOldNameCheck = true;

	static final int sourceSecId = 7000000; //500000
	static final UUID classificationUuid = UUID.fromString("f65a592d-a430-4db9-b994-39c181c34abe");
	static final boolean useSingleClassification = true;
	static final String classificationName = "Med-Checklist";
	static final UUID featureTreeUuid = UUID.fromString("be5851fb-ddb7-4e30-a375-57d98cf30c40");
	static final Object[] featureKeyList = new Integer[]{20, 21};

	// set to zero for unlimited nameFacts
	static final int maximumNumberOfNameFacts = 0;

	static final int partitionSize = 2500;


	//editor - import
	static final EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;

	static final boolean switchSpeciesGroup = true;

	static boolean useClassification = true;

	private final boolean removeHttpMapsAnchor = true;


	static final String infrGenericRankAbbrev = "[unranked]";
	static final String infrSpecificRankAbbrev = "[unranked]";

	static boolean warnForDifferingSynonymReference = false;


// **************** ALL *********************

	public void importEm2CDM (Source source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") to " + cdmDestination.getDatabase() + " ...");
		//make BerlinModel Source

		BerlinModelImportConfigurator config = BerlinModelImportConfigurator.NewInstance(source,  destination);

		config.setClassificationName(classificationName);

		config.setClassificationUuid(classificationUuid);
		config.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);
		config.setIgnoreNull(false);

		config.setDoAuthors(doAuthors ^ invers);
		config.setDoReferences(invers ? doReferences.invers() : doReferences);
		config.setDoTaxonNames(doTaxonNames ^ invers);
		config.setDoRelNames(doRelNames ^ invers);
		config.setDoNameStatus(doNameStatus ^ invers);
		config.setDoTypes(doTypes);  //always false
		config.setDoNameFacts(doNameFacts);//always false
		config.setDoTaxa(doTaxa ^ invers);
		config.setDoRelTaxa(doRelTaxa ^ invers);
		config.setDoFacts(doFacts ^ invers);
		config.setDoOccurrence(false); //always false
		config.setDoOccurrenceSources(false); //always false
        config.setDoCommonNames(false);//always false
        config.setDoNamedAreas(doNamedAreas ^ invers);

		config.setDoMarker(doMarker);
		config.setDoUser(doUser);

		config.setMcl(true);
		config.setDoSourceNumber(true);

		config.setLogNotMatchingOldNames(logNotMatchingOldNames);
		config.setLogMatchingNotExportedOldNames(logMatchingNotExportedOldNames);
		config.setCheckOldNameIsSynonym(checkOldNameIsSynonym);
		config.setIncludeMANsForOldNameCheck(includeMANsForOldNameCheck);

		config.setUseClassification(useClassification);
		config.setSourceRefUuid(BerlinModelTransformer.uuidSourceRefEuroMed);
		config.setEditor(editor);
		config.setDbSchemaValidation(hbm2dll);
		config.setWarnForDifferingSynonymReference(warnForDifferingSynonymReference);

		// maximum number of name facts to import
		config.setMaximumNumberOfNameFacts(maximumNumberOfNameFacts);

		config.setInfrGenericRankAbbrev(infrGenericRankAbbrev);
		config.setInfrSpecificRankAbbrev(infrSpecificRankAbbrev);
		config.setRemoveHttpMapsAnchor(removeHttpMapsAnchor);

		config.setDoPreliminaryRefDetailsWithNames(doPreliminaryRefDetailsWithNames);

//		filter
//		config.setTaxonTable(taxonTable);
//		config.setClassificationQuery(classificationQuery);
//		config.setRelTaxaIdQuery(relPTaxonIdQuery);
//		config.setNameIdTable(nameIdTable);
//		config.setReferenceIdTable(referenceIdTable);
//		config.setAuthorTeamFilter(authorTeamFilter);
//		config.setAuthorFilter(authorFilter);
//		config.setFactFilter(factFilter);
//		config.setRefDetailFilter(refDetailFilter);
//		config.setCommonNameFilter(commonNameFilter);
//		config.setOccurrenceFilter(occurrenceFilter);
//		config.setOccurrenceSourceFilter(occurrenceSourceFilter);
//		config.setWebMarkerFilter(webMarkerFilter);
		config.setUseSingleClassification(useSingleClassification);

		config.setCheck(check);
		config.setEditor(editor);
		config.setRecordsPerTransaction(partitionSize);

		config.setSwitchSpeciesGroup(switchSpeciesGroup);

		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<>();
		bmImport.invoke(config);

//		renameRanks(config, bmImport);

        renamePresenceAbsenceTerms(config, bmImport);

		createFeatureTree(config, bmImport);

//		createUsersAndRoles(config, bmImport);

//        runTransmissionEngine(config, bmImport);

//        importShapefile(config, bmImport);

//      createPreferences(config, bmImport); => manual

		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
	}


	private void renamePresenceAbsenceTerms(BerlinModelImportConfigurator config,
        CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {

        ICdmRepository app = bmImport.getCdmAppController();
        TransactionStatus tx = app.startTransaction();
        try {
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidNative, "present as native", "+", "");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidPresentDoubfully, "doubtfully present", "?", "d");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidReportedInError, "absent but reported in error", "-", "-");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidIntroducedDoubtfullyIntroduced, "doubtfully naturalized", "P", "p");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidNativeDoubtfullyNative, "doubtfully native", "D", "d");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidNaturalised, "naturalized", "N", "n");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidIntroducedAdventitious, "casual alien", "A", "a");
            renamePresenceAbsenceTerm(app, PresenceAbsenceTerm.uuidNativeFormerlyNative, "(presumably) extinct", "E", "†");

            app.commitTransaction(tx);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in renameRanks: " + e.getMessage());
        }


	}

    private void renamePresenceAbsenceTerm(ICdmRepository app, UUID uuidTerm, String label, String abbrev, String symbol) {
        PresenceAbsenceTerm term = (PresenceAbsenceTerm)app.getTermService().find(uuidTerm);
        Representation repr = term.getRepresentation(Language.ENGLISH());
        repr.setLabel(label);
        repr.setAbbreviatedLabel(abbrev);
        term.setSymbol2(symbol);
        term.setTitleCache(null, false);  //to definitely update the titleCache also
        app.getTermService().saveOrUpdate(term);
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
                app.getTermTreeService().saveOrUpdate(tree);

                app.commitTransaction(tx);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Exception in createFeatureTree: " + e.getMessage());
            }
		}
    }

	public static void main(String[] args) {
		MclActivator importActivator = new MclActivator();
		Source source = berlinModelSource;
		ICdmDataSource cdmRepository = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		importActivator.importEm2CDM(source, cdmRepository, hbm2dll);
		System.exit(0);
	}
}