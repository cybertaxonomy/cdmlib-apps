/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.lichenes;

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
import eu.etaxonomy.cdm.io.lichenes.LichenesGeneraImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 *
 * Import for lichenes of genera checklist.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/8886
 *
 * @author a.mueller
 * @since 10.03.2020
 */
public class LichenesGeneraActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_cdmtest_mysql();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_lichenes();

    //feature tree uuid
//    public static final UUID featureTreeUuid = UUID.fromString("35cf8268-b860-4a0a-81ae-077223116c56");
//    private static final String featureTreeTitle = "Flora of Greece dataportal feature tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("43183724-1919-4036-84ee-3e0e84938f8d");
    private static final String classificationName = "Lichenes";

    static final UUID secRefUuid = UUID.fromString("2c4d58eb-2432-4217-8179-e3739a3d255f");
    static final UUID sourceRefUuid = UUID.fromString("db54eefe-d1cb-44c5-ada2-cfb233cc708b");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = lichenesChecklist();
//        URI source = lichenesChecklist_local();

        LichenesGeneraImportConfigurator config = LichenesGeneraImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
//        config.setUuidFeatureTree(featureTreeUuid);
//        config.setFeatureTreeTitle(featureTreeTitle);
        config.setSecReference(getSecReference());
        config.setSourceReference(getSourceReference());

        CdmDefaultImport<LichenesGeneraImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);
    }


    private URI lichenesChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Lichenes/LichenesGeneraImport.xlsx");
    }
    private URI lichenesChecklist_local(){
//        return URI.create("file://C:\\Users\\a.mueller\\BGBM\\Data\\Lichenes\\LichenesImport.xlsx");
        File file = new File("C:\\Users\\a.mueller\\BGBM\\Data\\Lichenes\\LichenesGeneraImport.xlsx");
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

        person = Person.NewInstance();
        person.setInitials("B.P.");
        person.setFamilyName("Hodkinson");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setInitials("S.D.");
        person.setFamilyName("Leavitt");
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
        result.setTitle("Excelfile (LichenesGeneraImport.xlsx) derived from ");
        result.setInReference(getSecReference());
        result.setUuid(sourceRefUuid);

        return result;
    }

    public static void main(String[] args) {
        LichenesGeneraActivator me = new LichenesGeneraActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
