/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.DoubleResult;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter.ORDER;
import eu.etaxonomy.cdm.filter.VocabularyFilter;
import eu.etaxonomy.cdm.io.cdm2cdm.Cdm2CdmImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.mueller
 * @since 2022-11-08
 */
public class EuroMedCaucasusChecklistExportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource source = CdmDestinations.cdm_local_euromed();
    static final ICdmDataSource sourceConspectus = CdmDestinations.cdm_local_euromed_caucasus();

    static boolean isProduction = false;
    static final DB db = DB.GEORGIA;

    static final boolean doTaxa = false;
    static final boolean doVocabularies = false;
    static final boolean doDescriptions = false;
    static final boolean doConspectusAreas = true;

    static ICdmDataSource cdmDestination = !isProduction ?
            (db == DB.GEORGIA ? CdmDestinations.cdm_local_georgia() :
             db == DB.ARMENIA ? CdmDestinations.cdm_local_armenia() :
                                CdmDestinations.cdm_local_azerbaijan()
          ):(db == DB.GEORGIA ? CdmDestinations.cdm_production_georgia() :
             db == DB.ARMENIA ? CdmDestinations.cdm_production_armenia() :
                                CdmDestinations.cdm_production_azerbaijan());

    static final String sourceRefTitle = "Euro+Med Plantbase";
    static final UUID sourceRefUuid = UUID.fromString("65e22d20-a206-426e-9851-e874cf79ed7d");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private enum DB{GEORGIA, ARMENIA,AZERBAIJAN}

    static final int partitionSize = 5000;

    static final boolean distributionFilterFromAreaFilter = true;
    static final boolean includeUnpublished = true;
    static final boolean addAncestorTaxa = true;

    static final boolean includeAbsentDistributions = true;

    //invisible root
    //TODO filter out Kew/ILDIS
    private UUID uuidEuroMedTaxonNodeFilter = UUID.fromString("f13529f2-2644-43e0-9bf5-58f767bcfd77");
    //"Acer" subtree
//    private UUID uuidEuroMedTaxonNodeFilter = UUID.fromString("c7626b08-0ab8-4c18-9314-eecb9653cd24");
    //"Inula" subtree
//    private UUID uuidEuroMedTaxonNodeFilter = UUID.fromString ("f2be0026-b484-43e2-a447-ca027c8cb072");
    //Compositae subtree
//    private UUID uuidEuroMedTaxonNodeFilter = UUID.fromString("a45f6945-9b66-499e-b4f2-33b06caf6634");

    static final Set<UUID> commonNameLanguageFilter = new HashSet<>(Arrays.asList(new UUID[] {
//            UUID.fromString("7a0fde13-26e9-4382-a5c9-5640fc2b3334"),  //Armenian
//            UUID.fromString("2fc29072-908d-4bd3-b12f-cd2587ab8d75"),  //Azerbaijani
//            UUID.fromString("fb64b07c-c079-4fda-a803-212a0beca61b"),  //Georgian
//            UUID.fromString("e9f8cdb7-6819-44e8-95d3-e2d0690c3523"),  //English
            UUID.fromString("64ea9354-cbf8-40de-9f6e-387d24896f50"),  //Russian
//            UUID.fromString("ecf81f98-177d-49a6-ad0f-f6d89944c76b"),  //Turkish
    }));

    static final boolean removeImportSources = true;
    static final boolean addSource = true;

    //auditing
    static final boolean registerAuditing = true;

// ***************** ALL ************************************************//


    private void doImportVocs(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        if (doConspectusAreas) {
            source = sourceConspectus;
        }
        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(false);
        config.setDoDescriptions(false);
        config.setDoVocabularies(true);
        config.setSourceReference(getSourceRef());

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        if (doConspectusAreas) {
            VocabularyFilter vocFilter = VocabularyFilter.NewInstance();
            vocFilter.orVocabulary(UUID.fromString("3ef82b77-23cb-4b60-a4b3-edc7e4775ddb"));  //Conspectus areas
            config.setVocabularyFilter(vocFilter);
        }else {
            //vocFilter.orVocabulary(UUID.fromString("625a4962-c211-4597-816e-5804083efe26"));  //E+M areas
            List<UUID> trees = Arrays.asList(new UUID[] {
                    UUID.fromString("23970e4d-9da3-4416-86f7-5ae4f4307065"), //Caucasus area tree
                    UUID.fromString("6a5e1c2b-ec0d-46c8-9c7d-a2059267ffb7")});  //feature tree
            config.setGraphFilter(trees);
        }

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(doTaxa);
        config.setDoDescriptions(doDescriptions);
        config.setDoVocabularies(doVocabularies);
        config.setSourceReference(getSourceRef());
        config.setDistributionFilterFromAreaFilter(distributionFilterFromAreaFilter);
        config.setAddAncestors(addAncestorTaxa);

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        IProgressMonitor monitor = config.getProgressMonitor();

        config.setDbSchemaValidation(hbm2dll);
        TaxonNodeFilter taxonNodeFilter = config.getTaxonNodeFilter();
        taxonNodeFilter.setIncludeUnpublished(includeUnpublished);
        taxonNodeFilter.orSubtree(uuidEuroMedTaxonNodeFilter);
        taxonNodeFilter.notSubtree(UUID.fromString("1d742a17-81be-420f-9601-8b414c7f3c87"));  //Juncaceae
        taxonNodeFilter.notSubtree(UUID.fromString("54e14202-94e1-460e-8ce6-4ae2f3cef4c0"));  //Irdaceae
//        taxonNodeFilter.notSubtree(UUID.fromString("95a81577-7b2b-46d6-b9f9-c8e21cbad033"));  //Asperagaceae
        taxonNodeFilter.notSubtree(UUID.fromString("9efc66b6-70fd-4b98-9aa2-1511e0991b7c"));  //Orchidaceae
        taxonNodeFilter.notSubtree(UUID.fromString("c054e965-c79e-4a6d-80c8-ca3c04dd4d8b"));  //Amaryllidaceae
        taxonNodeFilter.notSubtree(UUID.fromString("c2479ff6-6076-4710-862e-0bdabd742254"));  //Araliaceae
        taxonNodeFilter.notSubtree(UUID.fromString("d9da9a5e-70d0-43ff-96c3-a362886c4df5"));  //Lamiaceae
        taxonNodeFilter.notSubtree(UUID.fromString("b2c0331f-1c41-40ff-8c18-0464fab17f51"));  //Fabaceae
        taxonNodeFilter.notSubtree(UUID.fromString("4a26def8-93ed-4196-960d-997ecb5f4fba"));  //Fagaceae
        taxonNodeFilter.notSubtree(UUID.fromString("674b601d-01df-4bd9-837a-cbed43fd55ba"));  //Betualaceae
        taxonNodeFilter.notSubtree(UUID.fromString("930d3b65-6a9f-4c10-bf0d-bf097a5121cf"));  //Euphorbiaceae

        taxonNodeFilter.setIncludeAbsentDistributions(includeAbsentDistributions);

        Set<UUID> commonNameLanguageFilter = new HashSet<>(Arrays.asList(new UUID[] {
                UUID.fromString("64ea9354-cbf8-40de-9f6e-387d24896f50"),  //Russian
                UUID.fromString("e9f8cdb7-6819-44e8-95d3-e2d0690c3523"),  //English
//              UUID.fromString("7a0fde13-26e9-4382-a5c9-5640fc2b3334"),  //Armenian
//              UUID.fromString("2fc29072-908d-4bd3-b12f-cd2587ab8d75"),  //Azerbaijani
//              UUID.fromString("fb64b07c-c079-4fda-a803-212a0beca61b"),  //Georgian
//              UUID.fromString("ecf81f98-177d-49a6-ad0f-f6d89944c76b"),  //Turkish
        }));

        UUID mainArea;
        if (db==DB.GEORGIA) {
            //Georgia
            mainArea = UUID.fromString("da1ccda8-5867-4098-a709-100a66e2150a");  //Gg
            taxonNodeFilter.orArea(mainArea);
            taxonNodeFilter.orArea(UUID.fromString("5fad859b-7929-4d5f-b92c-95e3e0469bb2"));  //Gg(A)
            taxonNodeFilter.orArea(UUID.fromString("6091c975-b946-4ef3-a18f-2e148eae6a06"));  //Gg(D)
            taxonNodeFilter.orArea(UUID.fromString("048799b0-d7b9-44c6-b2d1-5ca2a49fa175"));  //Gg(G)
            commonNameLanguageFilter.add(UUID.fromString("fb64b07c-c079-4fda-a803-212a0beca61b"));  //Georgian

        } else if (db==DB.ARMENIA) {
            //Armenia
            mainArea = UUID.fromString("535fed1e-3ec9-4563-af55-e753aefcfbfe");   //Ar
            taxonNodeFilter.orArea(mainArea);
            commonNameLanguageFilter.add(UUID.fromString("7a0fde13-26e9-4382-a5c9-5640fc2b3334"));  //Armenian
        }else if (db == DB.AZERBAIJAN) {
            //Azerbaijan
            mainArea = UUID.fromString("d3744c2d-2777-4e85-98bf-04d2fd589ebf");  //Ab
            taxonNodeFilter.orArea(mainArea);

            taxonNodeFilter.orArea(UUID.fromString("0f4c98bf-af7b-4cda-b62c-ad6a1909bfa0"));    //Ab(A)
            taxonNodeFilter.orArea(UUID.fromString("aa75b0ca-49c9-4f8e-8cc2-2a343eb2fff4"));    //Ab(N)
            commonNameLanguageFilter.add(UUID.fromString("2fc29072-908d-4bd3-b12f-cd2587ab8d75"));  //Azerbaijanian
        }else {
            throw new RuntimeException("Unhandled DB");
        }
        config.setUuidEndemicRelevantArea(mainArea);
        taxonNodeFilter.setOrder(ORDER.TREEINDEX);

        config.setEndemismHandler(getEndemismHandler(taxonNodeFilter));

        config.setCommonNameLanguageFilter(commonNameLanguageFilter);

        config.setCheck(check);
        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
    }

    private Function<DoubleResult<Taxon,DefinedTermBase<?>[]>,Distribution> getEndemismHandler(TaxonNodeFilter taxonNodeFilter) {
        Set<UUID> emAreas = new HashSet<>();
        emAreas.add(UUID.fromString("111bdf38-7a32-440a-9808-8af1c9e54b51"));  //EM
        emAreas.add(UUID.fromString("865e0fcc-bfb5-4af2-8822-08c90c7ba61e")); //Europe
        Set<UUID> largerCaucasusAreas = new HashSet<>();
        emAreas.add(UUID.fromString("904c3980-b98d-422e-a195-95f4f41fc734"));  //Tcs
        emAreas.add(UUID.fromString("05b0dd06-30f8-477d-bf4c-30d9def56320")); //Cc


        Set<UUID> relevantAreas = taxonNodeFilter.getAreaFilter().stream().map(f->f.getUuid()).collect(Collectors.toSet());

        Function<DoubleResult<Taxon,DefinedTermBase<?>[]>,Distribution> function = input->{
            Taxon t = input.getFirstResult();
            DefinedTermBase<?>[] params = input.getSecondResult();
            NamedArea relevantArea = (NamedArea)params[0];

            PresenceAbsenceTerm endemic = (PresenceAbsenceTerm)params[1];
            PresenceAbsenceTerm notEndemic = (PresenceAbsenceTerm)params[2];
            PresenceAbsenceTerm unknownEndemic = (PresenceAbsenceTerm)params[3];
            Feature distributionFeature = (Feature)params[4];

            boolean isNativeInRelevantArea = false;
            boolean isEndemicInEuroMed = false;

            Distribution result = Distribution.NewInstance(relevantArea, unknownEndemic);
            result.setFeature(distributionFeature);
            Set<Distribution> distributions = t.getDescriptionItems(Feature.DISTRIBUTION(), Distribution.class);
            if (distributions.isEmpty()) {
                return null;
            }

            for (Distribution distribution : distributions) {
                NamedArea area = distribution.getArea();
                PresenceAbsenceTerm status = distribution.getStatus();
                if (area == null || status == null) {
                    continue;
                } else if (emAreas.contains(area.getUuid())) {
                    if (status.equals(notEndemic)) {
                        result.setStatus(notEndemic);
                        return result;
                    }else if (status.equals(endemic)) {
                        isEndemicInEuroMed = true;
                    }
                } else if (relevantAreas.contains(area.getUuid())) {
                    if (isNative(status)) {
                        isNativeInRelevantArea = true;
                    }
                } else if (largerCaucasusAreas.contains(area.getUuid())) {
                    System.out.println("Larger Cc area for " + t.getName().getTitleCache() + " with status " + distribution.getStatus().getTitleCache());
                } else {
                    //other areas
                    if (isNative(status)) {
                        result.setStatus(notEndemic);
                        return result;
                    }
                }
            }
            if (isNativeInRelevantArea && isEndemicInEuroMed) {
                result.setStatus(endemic);
            }

            return result;
        };
        return function;
    }

    private boolean isNative(PresenceAbsenceTerm status) {
        return status.equals(PresenceAbsenceTerm.NATIVE())
                || status.equals(PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA()) ;
    }

    private Reference getSourceRef() {
        Reference ref = ReferenceFactory.newDatabase();
        ref.setTitle(sourceRefTitle);
        ref.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(Calendar.getInstance()));
        ref.setUuid(sourceRefUuid);
        return ref;
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        EuroMedCaucasusChecklistExportActivator myImport = new EuroMedCaucasusChecklistExportActivator();
        if (doVocabularies || doConspectusAreas) {
            myImport.doImportVocs(source, cdmDB, schemaValidation);
        }
        if (doTaxa || doDescriptions) {
            myImport.doImport(source, cdmDB, schemaValidation);
        }
        System.exit(0);
    }
}