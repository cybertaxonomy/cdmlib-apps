/**
* Copyright (C) 2024 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.VocabularyFilter;
import eu.etaxonomy.cdm.io.cdm2cdm.Cdm2CdmImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;

/**
 * @author muellera
 * @since 21.10.2024
 */
public class MexicoVocabularyImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource source = CdmDestinations.cdm_local_mexico();

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_caryophyllales();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales();

//    static boolean isProduction = true;
    static final UUID eFloraMexEditingFeatureTree = UUID.fromString("b3fc4562-950f-45a9-ad80-9a7b036cdbe5");
//  static final UUID eFloraMexFeatureVocabulary  = UUID.fromString("3039637e-c0f5-49b6-9139-9310fd804080");
    static final UUID eFloraMexAreaTree = UUID.fromString("3e89c171-58fe-4e5a-8883-97b4e6dab941");
    static final UUID eFloraMexDistributionStatusTree = UUID.fromString("0ca0c1be-ef96-4141-966b-56e6d74190ac");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    static final boolean removeImportSources = true;
    static final boolean addSource = false;
    static final boolean externallyManaged = true;
    static final String externallyManagedBaseUri = "https://efloramex.ib.unam.mx/cdmserver/";

// ***************** ALL ************************************************//


    private void doImportVocs(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(false);
        config.setDoDescriptions(false);
        config.setDoVocabularies(true);
        config.setExternallyManaged(externallyManaged);
        config.setExternallyManagedBaseURI(externallyManagedBaseUri);

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        VocabularyFilter vocFilter = VocabularyFilter.NewInstance();
        //languages
        vocFilter.orVocabulary(UUID.fromString("d37d043e-94af-4cb0-b702-e6f45318b039")); //Mexican Languages
        //states
        vocFilter.orVocabulary(UUID.fromString("315d5205-1834-49d2-8cab-57e46597091b")); //taxon status
        vocFilter.orVocabulary(UUID.fromString("d212f150-73f8-4ea4-8355-8131fbea5280")); //forma de Crecimiento
        vocFilter.orVocabulary(UUID.fromString("6a32ed74-8393-412b-a24e-f3a771372cde")); //forma de vida
        vocFilter.orVocabulary(UUID.fromString("ec59ea8a-3ada-4e8e-ad9f-c1624a8da815")); //Nutricion
        vocFilter.orVocabulary(UUID.fromString("76bde6c9-b5c1-4c2a-8a56-45eb225bd918")); //Vegetacion
        vocFilter.orVocabulary(UUID.fromString("57fe20fd-50fd-4aa5-bcf3-3a10cef8cf42")); //IUCN
        vocFilter.orVocabulary(UUID.fromString("da1dd407-3276-49b8-b5a3-c59b2990a330")); //NOM-059

        config.setVocabularyFilter(vocFilter);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

    }

    private void doImportTrees(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){
        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start" + importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        config.setDoTaxa(false);
        config.setDoDescriptions(false);
        config.setDoVocabularies(true);
        config.setExternallyManaged(externallyManaged);
        config.setExternallyManagedBaseURI(externallyManagedBaseUri);

        config.setRemoveImportSources(removeImportSources);
        config.setAddSources(addSource);

        List<UUID> trees = Arrays.asList(new UUID[] {
            eFloraMexEditingFeatureTree, //eFloraMex editing feature tree
            eFloraMexAreaTree,
            eFloraMexDistributionStatusTree
        });
        config.setGraphFilter(trees);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        MexicoVocabularyImportActivator myImport = new MexicoVocabularyImportActivator();
        myImport.doImportVocs(source, cdmDB, schemaValidation);
        myImport.doImportTrees(source, cdmDB, schemaValidation);
        System.exit(0);
    }
}