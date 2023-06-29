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
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter.ORDER;
import eu.etaxonomy.cdm.io.cdm2cdm.Cdm2CdmImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ITaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitionerConcurrent;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 2022-11-08
 */
public class EuroMedCaucasusChecklistExportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource source = CdmDestinations.cdm_local_euromed();

    static final DB db = DB.GEORGIA;

    static ICdmDataSource cdmDestination = db==DB.GEORGIA ? CdmDestinations.cdm_local_georgia() :
        db == DB.ARMENIA ? CdmDestinations.cdm_local_armenia() : CdmDestinations.cdm_local_azerbaijan();

//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_georgia();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_armenia();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_azerbaijan();

    static final String sourceRefTitle = "Euro+Med Plantbase";
    static final UUID sourceRefUuid = UUID.fromString("65e22d20-a206-426e-9851-e874cf79ed7d");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private enum DB{GEORGIA, ARMENIA,AZERBAIJAN}

    static final int partitionSize = 5000;

    static final boolean doTaxa = false;
    static final boolean doDescriptions = true;
    static final boolean doVocabularies = false;
    static final boolean distributionFilterFromAreaFilter = true;
    static final boolean includeUnpublished = true;
    static final boolean addAncestorTaxa = true;

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
            UUID.fromString("7a0fde13-26e9-4382-a5c9-5640fc2b3334"),  //Armenian
            UUID.fromString("2fc29072-908d-4bd3-b12f-cd2587ab8d75"),  //Azerbaijani
            UUID.fromString("fb64b07c-c079-4fda-a803-212a0beca61b"),  //Georgian
            UUID.fromString("e9f8cdb7-6819-44e8-95d3-e2d0690c3523"),  //English
            UUID.fromString("64ea9354-cbf8-40de-9f6e-387d24896f50"),  //Russian
            UUID.fromString("ecf81f98-177d-49a6-ad0f-f6d89944c76b"),  //Turkish
    }));

    static final boolean removeImportSources = true;
    static final boolean addSource = true;

    static final boolean doConcurrent = false;
    //auditing
    static final boolean registerAuditing = true;

// ***************** ALL ************************************************//


    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setConcurrent(doConcurrent);
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
        taxonNodeFilter.notSubtree(UUID.fromString("95a81577-7b2b-46d6-b9f9-c8e21cbad033"));  //Asperagaceae
        taxonNodeFilter.notSubtree(UUID.fromString("9efc66b6-70fd-4b98-9aa2-1511e0991b7c"));  //Orchidaceae
        taxonNodeFilter.notSubtree(UUID.fromString("c054e965-c79e-4a6d-80c8-ca3c04dd4d8b"));  //Amaryllidaceae
        taxonNodeFilter.notSubtree(UUID.fromString("c2479ff6-6076-4710-862e-0bdabd742254"));  //Araliaceae
        taxonNodeFilter.notSubtree(UUID.fromString("d9da9a5e-70d0-43ff-96c3-a362886c4df5"));  //Lamiaceae
        taxonNodeFilter.notSubtree(UUID.fromString("b2c0331f-1c41-40ff-8c18-0464fab17f51"));  //Fabaceae
        taxonNodeFilter.notSubtree(UUID.fromString("4a26def8-93ed-4196-960d-997ecb5f4fba"));  //Fagaceae
        taxonNodeFilter.notSubtree(UUID.fromString("674b601d-01df-4bd9-837a-cbed43fd55ba"));  //Betualaceae
        taxonNodeFilter.notSubtree(UUID.fromString("930d3b65-6a9f-4c10-bf0d-bf097a5121cf"));  //Euphorbiaceae

        if (db==DB.GEORGIA) {
            //Georgia
            taxonNodeFilter.orArea(UUID.fromString("da1ccda8-5867-4098-a709-100a66e2150a"));  //Gg
            taxonNodeFilter.orArea(UUID.fromString("5fad859b-7929-4d5f-b92c-95e3e0469bb2"));  //Gg(A)
            taxonNodeFilter.orArea(UUID.fromString("6091c975-b946-4ef3-a18f-2e148eae6a06"));  //Gg(D)
            taxonNodeFilter.orArea(UUID.fromString("048799b0-d7b9-44c6-b2d1-5ca2a49fa175"));  //Gg(G)
        } else if (db==DB.ARMENIA) {
            //Armenia
            taxonNodeFilter.orArea(UUID.fromString("535fed1e-3ec9-4563-af55-e753aefcfbfe"));   //Ar
        }else if (db == DB.AZERBAIJAN) {
            //Azerbaijan
            taxonNodeFilter.orArea(UUID.fromString("d3744c2d-2777-4e85-98bf-04d2fd589ebf"));   //Ab
            taxonNodeFilter.orArea(UUID.fromString("0f4c98bf-af7b-4cda-b62c-ad6a1909bfa0"));    //Ab(A)
            taxonNodeFilter.orArea(UUID.fromString("aa75b0ca-49c9-4f8e-8cc2-2a343eb2fff4"));    //Ab(N)
        }else {
            throw new RuntimeException("Unhandled DB");
        }
        taxonNodeFilter.setOrder(ORDER.TREEINDEX);

        config.setCommonNameLanguageFilter(commonNameLanguageFilter);

        if (doConcurrent){
            ITaxonNodeOutStreamPartitioner partitioner = TaxonNodeOutStreamPartitionerConcurrent
                    .NewInstance(config.getSource(), taxonNodeFilter,
                            8, monitor, 1, TaxonNodeOutStreamPartitioner.fullPropertyPaths);
            config.setPartitioner(partitioner);
        }

        config.setCheck(check);
//        config.setRecordsPerTransaction(partitionSize);

        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
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
        myImport.doImport(source, cdmDB, schemaValidation);
        System.exit(0);
    }
}