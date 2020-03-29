/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.lichenes;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
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
    private static final Logger logger = Logger.getLogger(LichenesGeneraActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_lichenes();

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("35cf8268-b860-4a0a-81ae-077223116c56");
//    private static final String featureTreeTitle = "Flora of Greece dataportal feature tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("43183724-1919-4036-84ee-3e0e84938f8d");
    private static final String classificationName = "Lichenes";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        //make Source
        URI source = lichenesChecklist();

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
        return URI.create("file:////BGBM-PESIHPC/Lichenes/xxx.xlsx");
    }

    private Reference secRef;
    private Reference getSecReference(){
        if (secRef != null){
            return secRef;
        }
        Reference result = ReferenceFactory.newBook();
        result.setTitle("Lichenes publication");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2016"));

        Team team = Team.NewInstance();

        Person person = Person.NewInstance();
        person.setGivenName("P.");
        person.setFamilyName("Dimopoulos");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("Th.");
        person.setFamilyName("Raus");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("E.");
        person.setFamilyName("Bergmeier");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("Th.");
        person.setFamilyName("Constantinidis");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("G.");
        person.setFamilyName("Iatrou");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("S.");
        person.setFamilyName("Kokkini");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("A.");
        person.setFamilyName("Strid");
        team.addTeamMember(person);

        person = Person.NewInstance();
        person.setGivenName("D.");
        person.setFamilyName("Tzanoudakis");
        team.addTeamMember(person);

        result.setAuthorship(team);

        result.setPublisher("Berlin: Botanic Garden and Botanical Museum Berlin-Dahlem; Athens: Hellenic Botanical Society.");

        result.setVolume("31");
        Reference englera = ReferenceFactory.newPrintSeries();
        englera.setTitle("Englera");
        result.setInReference(englera);
        secRef = result;

        secRef.setUuid(UUID.fromString("761bd693-2789-4305-9a79-9e510adf2388"));
        return result;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Excelfile (VPG_FINAL_WITH_SYNONYMS_21.01.2017.xlsx) derived from ");
        result.setInReference(getSecReference());
        result.setUuid(UUID.fromString("b704f8a2-bc1c-45c7-8860-68663a7a44c0"));

        return result;
    }

    public static void main(String[] args) {
        LichenesGeneraActivator me = new LichenesGeneraActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
