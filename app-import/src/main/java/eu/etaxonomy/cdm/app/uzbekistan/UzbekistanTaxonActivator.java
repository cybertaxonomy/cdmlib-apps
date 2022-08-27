/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.uzbekistan;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.uzbekistan.UzbekistanTaxonImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Import for Flora of Uzbekistan taxa.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/8996
 *
 * @author a.mueller
 * @since 05.05.2020
 */
public class UzbekistanTaxonActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_uzbekistan();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_uzbekistan();

    static DbSchemaValidation hbm2dll = cdmDestination == CdmDestinations.localH2() ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;

    //classification
    static final UUID classificationUuid = UUID.fromString("230ca922-bf63-4297-b631-aed87f553107");
    private static final String classificationName = "Flora of Uzbekistan";

    static final UUID secRefUuid = UUID.fromString("09b9b07a-a501-49d0-877c-14dd738537bb");
    static final UUID sourceRefUuid = UUID.fromString("295e0b9c-c637-45db-8e71-ca664f128221");

    static final String fileName = "FoU_taxonomy_2020_05_12.xlsx";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = uzbekistanChecklist();
//        URI source = uzbekistanChecklist_local();

        UzbekistanTaxonImportConfigurator config = UzbekistanTaxonImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
//        config.setUuidFeatureTree(featureTreeUuid);
//        config.setFeatureTreeTitle(featureTreeTitle);
        config.setSecReference(getSecReference());
        config.setSourceReference(getSourceReference());

        CdmDefaultImport<UzbekistanTaxonImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    private URI uzbekistanChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Uzbekistan/" + fileName);
    }
    @SuppressWarnings("unused")
    private URI uzbekistanChecklist_local(){
        File file = new File("C:\\Users\\a.mueller\\BGBM\\Data\\Uzbekistan\\" + fileName);
        return URI.fromFile(file);
    }

    private Reference secRef;
    private Reference getSecReference(){
        if (secRef != null){
            return secRef;
        }
        Reference result = ReferenceFactory.newArticle();
        result.setTitle("Corrections and amendments to the 2016 classification of lichenized fungi in the Ascomycota and Basidiomycota");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2016"));

        Team team = Team.NewInstance();

        Person person = Person.NewInstance();
        person.setInitials("R.");
        person.setFamilyName("Lücking");
        team.addTeamMember(person);

        result.setAuthorship(team);

//        result.setPublisher("Berlin: Botanic Garden and Botanical Museum Berlin-Dahlem; Athens: Hellenic Botanical Society.");

        result.setVolume("120(1)");
        Reference journal = ReferenceFactory.newJournal();
        journal.setTitle("The Bryologist");
        result.setInReference(journal);
        secRef = result;

        secRef.setUuid(secRefUuid);
        return result;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Excelfile (FoU_taxonomy_template.xlsx) derived from ");
//        result.setInReference(getSecReference());
        result.setUuid(sourceRefUuid);

        return result;
    }

    public static void main(String[] args) {
        UzbekistanTaxonActivator me = new UzbekistanTaxonActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}