/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.bogota;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.bogota.BogotaChecklistImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.IWebPage;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Activator for import of Bogota Checklist
 *
 * @author a.mueller
 * @since 21.04.2017
 *
 */
public class BogotaChecklistActivator {
    private static final Logger logger = Logger.getLogger(BogotaChecklistActivator.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_bogota_production();

    //classification
    static final UUID classificationUuid = UUID.fromString("c7779e17-8d45-4429-a9b0-e9c0fce93ec5");
    private static final String classificationName = "Bogota Taxonomic Backbone";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = bogotaChecklist();

        //make Source
        BogotaChecklistImportConfigurator config= BogotaChecklistImportConfigurator.NewInstance(source, cdmDestination);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSecReference(getSecReference());

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<BogotaChecklistImportConfigurator> myImport = new CdmDefaultImport<>();

        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");

    }


    //bogotaChecklist
    public static URI bogotaChecklist() {
        return URI.create("file:////BGBM-PESIHPC/Bogota/Resultados_Busqueda_avanzada_2017-04-19_0810_import.xlsx");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Resultados_Busqueda_avanzada_2017-04-19_0810_import.xlsx");
        return result;
    }

    private Reference getSecReference() {
        IWebPage result = ReferenceFactory.newWebPage();
        result.setTitle("Catálogo de plantas y líquenes de Colombia");
        result.setPlacePublished("Bogotá");
        result.setUri(URI.create("http://catalogoplantasdecolombia.unal.edu.co/"));
        result.setPublisher("Instituto de Ciencias Naturales, Universidad Nacional de Colombia");
        result.setDatePublished(TimePeriodParser.parseString("2016"));
        result.getDatePublished().setFreeText("2016 (visited 2017-04-19)");

        Team team = Team.NewInstance();
        Person bernal = Person.NewTitledInstance("Bernal, R.");
        bernal.setGivenName("R.");
        bernal.setFamilyName("Bernal");
        Person gradstein = Person.NewTitledInstance("Gradstein, S.R.");
        gradstein.setGivenName("S.R.");
        gradstein.setFamilyName("Gradstein");
        Person celis = Person.NewTitledInstance("Celis, M.");
        celis.setGivenName("M.");
        celis.setFamilyName("Celis");
        team.addTeamMember(bernal);
        team.addTeamMember(gradstein);
        team.addTeamMember(celis);
        result.setAuthorship(team);

        return (Reference)result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        BogotaChecklistActivator me = new BogotaChecklistActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
