/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import eu.etaxonomy.cdm.common.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.greece.FloraHellenicaImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 *
 * Import for Checklist of Greece.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/6286
 *
 * @author a.mueller
 * @since 13.12.2016
 */
public class GreeceActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

    //feature tree uuid
    public static final UUID featureTreeUuid = UUID.fromString("9e1e0e81-7475-4b28-8619-b7f42cd760b6");
    private static final String featureTreeTitle = "Flora of Greece dataportal feature tree";

    //classification
    static final UUID classificationUuid = UUID.fromString("e537d69a-c2d9-4ac6-8f79-5b5e3dd5c154");
    private static final String classificationName = "Vascular plants of Greece";


    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    boolean doImages = true;

    boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);

    private void doImport(ICdmDataSource cdmDestination){

        URI source = greekChecklist();  //just any

        //make Source
        FloraHellenicaImportConfigurator config = FloraHellenicaImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setUuidFeatureTree(featureTreeUuid);
        config.setFeatureTreeTitle(featureTreeTitle);
        config.setDoImages(doImages);
        config.setSecReference(getSecReference());
        config.setSourceReference(getSourceReference());
        config.setSecReference2(getSecReference2());

        CdmDefaultImport<FloraHellenicaImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

    }


    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/VPG_FINAL_WITH_SYNONYMS_21.01.2017.xlsx");
    }

    private Reference secRef;
    private Reference getSecReference(){
        if (secRef != null){
            return secRef;
        }
        Reference result = ReferenceFactory.newBook();
        result.setTitle("Vascular plants of Greece: An annotated checklist.");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2013"));

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

        secRef.setUuid(UUID.fromString("1f78fd94-3a99-4a7b-881f-71cb099ea13a"));
        return result;
    }

    private Reference getSecReference2(){
        Reference result = ReferenceFactory.newArticle();
        result.setTitle("Vascular plants of Greece: An annotated checklist. Supplement");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("26.10.2016"));

        TeamOrPersonBase<?> team = getSecReference().getAuthorship();
                result.setAuthorship(team);

        result.setPublisher("Berlin: Botanic Garden and Botanical Museum Berlin-Dahlem; Athens: Hellenic Botanical Society.");

        result.setVolume("46(3)");
        result.setPages("301–347");
        Reference journal = ReferenceFactory.newJournal();
        journal.setTitle("Willdenowia");
        result.setInReference(journal);
        result.setDoi(DOI.fromString("http://dx.doi.org/10.3372/wi.46.46303"));
        result.setReferenceAbstract("Supplementary information on taxonomy, nomenclature, distribution within Greece, total range, life form and ecological traits of vascular plants known to occur in Greece is presented and the revised data are quantitatively analysed. Floristic discrepancies between Vascular plants of Greece: An annotated checklist (Dimopoulos & al. 2013) and relevant influential datasets (Flora europaea, Med-Checklist, Euro+Med PlantBase, etc.) are explained and clarified. An additional quantity of synonyms and misapplied names used in previous Greek floristic literature is presented. Taxonomic and floristic novelties published after 31 October 2013 are not considered.");
        result.setUri(URI.create("http://www.bioone.org/doi/full/10.3372/wi.46.46303"));
        result.setUuid(UUID.fromString("94fad58e-34de-48b6-a130-ffa3e7cf9a3c"));

        return result;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Excelfile (VPG_FINAL_WITH_SYNONYMS_21.01.2017.xlsx) derived from ");
        result.setInReference(getSecReference());
        result.setUuid(UUID.fromString("7e14e5d2-b3aa-486b-ba31-41dbb91d7fe3"));

        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GreeceActivator me = new GreeceActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
