/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.DoubleResult;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.filter.VocabularyFilter;
import eu.etaxonomy.cdm.io.cdm2cdm.Cdm2CdmImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * Import for area vocabularies in the caucasus context.
 * For imports from a preliminary to a final (or more recent preliminary) version.
 * Usually running after {@link EuroMedCaucasusChecklistExportActivator}.
 *
 * @author a.mueller
 * @since 2023-12-13
 */
public class CaucasusAreaVocabularyMigrationActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource source = CdmDestinations.cdm_local_azerbaijan(); //_old
//    static final ICdmDataSource source = CdmDestinations.cdm_production_azerbaijan_old();

    static boolean isProduction = true;
    static final DB db = DB.AZERBAIJAN;

    static final UUID areaVocabulary = UUID.fromString("c839f88f-c04b-481e-badc-384773b649d0");

    static ICdmDataSource cdmDestination = !isProduction ?
            (db == DB.GEORGIA ? CdmDestinations.cdm_local_georgia() :
             db == DB.ARMENIA ? CdmDestinations.cdm_local_armenia() :
                                CdmDestinations.cdm_local_azerbaijan()
          ):(db == DB.GEORGIA ? CdmDestinations.cdm_production_georgia() :
             db == DB.ARMENIA ? CdmDestinations.cdm_production_armenia() :
                                CdmDestinations.cdm_production_azerbaijan());

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private enum DB{GEORGIA, ARMENIA,AZERBAIJAN}

    static final int partitionSize = 5000;

    static final boolean includeAbsentDistributions = true;

    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    static final boolean removeImportSources = true;
    static final boolean addSource = false;
    //TODO needed?
    static final String sourceRefTitle = "Azerbaijan area trees";
    static final UUID sourceRefUuid = UUID.fromString("30583931-5f5d-40cb-8198-10dc9e7fb9ef");

    //auditing
    static final boolean registerAuditing = true;

// ***************** ALL ************************************************//


    private void doImportVocs(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(false);
        config.setDoDescriptions(false);
        config.setDoVocabularies(true);
        config.setSourceReference(getSourceRef());

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        VocabularyFilter vocFilter = VocabularyFilter.NewInstance();
        vocFilter.orVocabulary(areaVocabulary);
        config.setVocabularyFilter(vocFilter);

//    }else {
//            //vocFilter.orVocabulary(UUID.fromString("625a4962-c211-4597-816e-5804083efe26"));  //E+M areas
//            List<UUID> trees = Arrays.asList(new UUID[] {
//                    UUID.fromString("23970e4d-9da3-4416-86f7-5ae4f4307065"), //Caucasus area tree
//                    UUID.fromString("6a5e1c2b-ec0d-46c8-9c7d-a2059267ffb7")});  //feature tree
//            config.setGraphFilter(trees);
//        }

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll) throws NoSuchMethodException, SecurityException{

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(false);
        config.setDoDescriptions(false);
        config.setDoVocabularies(false);
        config.setSourceReference(getSourceRef());

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        config.setDbSchemaValidation(hbm2dll);

        config.setCheck(check);
        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
    }

//    private Reference getEndemismReference() {
//        Reference ref = ReferenceFactory.newWebPage();
//        ref.setTitle("Euro+Med PlantBase");
//        ref.setAccessed(DateTime.now());
//        ref.setUuid(endemismReferenceUuid);
//        return ref;
//    }

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
//        ref.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(Calendar.getInstance()));
//        ref.setAccessed(DateTime.now());
        ref.setUuid(sourceRefUuid);
        return ref;
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        CaucasusAreaVocabularyMigrationActivator myImport = new CaucasusAreaVocabularyMigrationActivator();
        myImport.doImportVocs(source, cdmDB, schemaValidation);
        System.exit(0);
    }
}