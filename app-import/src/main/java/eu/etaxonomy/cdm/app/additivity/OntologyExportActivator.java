/**
* Copyright (C) 2022 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.additivity;

import java.util.Collection;
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
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.VocabularyEnum;

/**
 * @author a.mueller
 * @since 31.08.2022
 */
public class OntologyExportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource source = CdmDestinations.cdm_local_terms();
//    static final ICdmDataSource source = CdmDestinations.cdm_production_cdmterms();

    static final ICdmDataSource destination = CdmDestinations.cdm_local_greece();
//    static final ICdmDataSource destination = CdmDestinations.cdm_local_cichorieae();

//    static final ICdmDataSource destination = CdmDestinations.cdm_production_greece_checklist();
//    static final ICdmDataSource destination = CdmDestinations.cdm_production_cichorieae();

    static final DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;

    //allow updating
    static final boolean updateExistingTermCollections = true;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    static final boolean doVocabularies = true;
    static final boolean addSources = false;
    static final boolean removeImportSources = true;
    static final boolean externallyManaged = true;

    //defaults
    static final boolean doTaxa = false;
    static final boolean doDescriptions = false;
    static final boolean doConcurrent = false;
    static final boolean registerAuditing = true;


// ***************** ALL ************************************************//

//    UUID uuidBupleurumTaxonNodeFilter = UUID.fromString("51e768cf-321b-4108-8bee-46143996b033");

    private void doImport(ICdmDataSource source, ICdmDataSource destination, DbSchemaValidation hbm2dll){

        String importFrom = " import from "+ source.getDatabase() + " to "+ destination.getDatabase() + " ...";
        System.out.println("Start"+importFrom);

        Cdm2CdmImportConfigurator config = Cdm2CdmImportConfigurator.NewInstace(source, destination);
        VocabularyFilter vocFilter = VocabularyFilter.NewInstance();
        //modifier filter
        addVocFilters(vocFilter, VocabularyEnum.ontologyModifierVocabularyUuids());
        config.setVocabularyFilter(vocFilter);
        //structures, properties and states
        Collection<UUID> graphFilter = getGraphFilter();
        config.setGraphFilter(graphFilter);

        config.setDoTaxa(doTaxa);
        config.setDoDescriptions(doDescriptions);
        config.setDoVocabularies(doVocabularies);

        config.setAddSources(addSources);
        config.setSourceReference(getSourceRefNull());
        config.setRemoveImportSources(removeImportSources);
        config.setAddMissingTerms(updateExistingTermCollections);

        config.setExternallyManaged(externallyManaged);

        config.setDbSchemaValidation(hbm2dll);
//        config.getTaxonNodeFilter().orSubtree(uuidBupleurumTaxonNodeFilter);
//        config.getTaxonNodeFilter().setOrder(ORDER.TREEINDEX);

        config.setCheck(check);

        config.setRegisterAuditing(registerAuditing);

        // invoke import
        CdmDefaultImport<Cdm2CdmImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.out.println("End" + importFrom);
    }

    private List<UUID> getGraphFilter() {
        return VocabularyEnum.ontologyTreeUuids();
    }

    private void addVocFilters(VocabularyFilter vocFilter, List<UUID> vocUuids) {
        for (UUID vocUuid : vocUuids) {
            vocFilter.orVocabulary(vocUuid);
        }
    }

    private Reference getSourceRefNull() {
        return null;
    }
//    private Reference getSourceRef() {
//        Reference ref = ReferenceFactory.newDatabase();
//        ref.setTitle(sourceRefTitle);
//        ref.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(Calendar.getInstance()));
//        ref.setUuid(sourceRefUuid);
//        return ref;
//    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : destination;
        OntologyExportActivator myImport = new OntologyExportActivator();
        myImport.doImport(source, cdmDB, schemaValidation);
        System.exit(0);
    }
}