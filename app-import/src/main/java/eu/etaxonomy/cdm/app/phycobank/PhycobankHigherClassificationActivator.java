/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.phycobank;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.phycobank.PhycobankHigherClassificationImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;


/**
 * Activator to import phycobank higher classifications.
 * @author a.mueller
 * @since 2018-08-09
 */
public class PhycobankHigherClassificationActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PhycobankHigherClassificationActivator.class);


    // ====================================================================================

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;  //

//    static ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
    static ICdmDataSource cdmDestination = CdmDestinations.cdm_test_phycobank();
//TODO: Die 3 Syllabus noch richtig als 3 Referenzen anlegen (muss Henning machen), hier dann die jeweilige UUID
// verwenden und unten den worksheetNamen entsprechend anpassen
    static Reference secRef = getSecReference_Frey();
//    static Reference secRef = getSecReference_WoRMS();
//    static String worksheetName = secRef.equals(getSecReference_WoRMS())? "WoRMS" :
//        secRef.equals(getSecReference_Frey())? "HigherRanksEntwurfNeu" : null;
    static String worksheetName = "Syllabus2_1";

    static UUID uuidRefPhycobank = UUID.fromString("8058a5ec-60ee-4a04-8c17-5623e3a4795c");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = fileURI();
        Reference sourceRef = getSourceReference();

        //make Source
        PhycobankHigherClassificationImportConfigurator config= PhycobankHigherClassificationImportConfigurator.NewInstance(source, cdmDestination);
        config.setWorksheetName(worksheetName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSourceReference(sourceRef);
        config.setSecReference(secRef);
        config.setPhycobankReference(getPhycobankReference());
        config.setProgressMonitor(DefaultProgressMonitor.NewInstance());

        CdmDefaultImport<PhycobankHigherClassificationImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.exit(0);

    }

    private static Reference getSecReference_Frey() {
        Reference result = ReferenceFactory.newBook();
        result.setTitle("Syllabus of the plant families");
        result.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(2015));
        result.setPublisher("Borntraeger");
        result.setPlacePublished("Stuttgart");
        Person author = Person.NewInstance();
        author.setFamilyName("Frey");
        author.setInitials("W.");
        result.setAuthorship(author);
        result.setUuid(UUID.fromString("2b4a3a67-e432-4d6b-b716-081045179df9"));
        return result;
    }

    private static Reference getSecReference_WoRMS() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("WoRMS World Register of Marine Species");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2018-04-20"));
        result.setUri(URI.create("http://www.marinespecies.org/index.php"));
        result.setUuid(UUID.fromString("b33daeb0-8770-4ee2-92d0-80aaa87bfba2"));
        return result;
    }

    private static Reference getPhycobankReference() {
        Reference result = ReferenceFactory.newWebPage();
        result.setTitle("Phycobank");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2018+"));
        result.setUri(URI.create("https://www.phycobank.org/"));
        result.setUuid(uuidRefPhycobank);
        return result;
    }

    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Higher classification Excel import: " + fileName());
        result.setUri(fileURI());
        result.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(new Date(), null));
        return result;
    }

    public static String fileName(){
//        return "Algen_Syllabus_NormalImplied_Test.xlsx";
        return "Algen_Syllabus_Produktion_corr.xlsx";
    }
    public static String filePath(){
        return "file:////BGBM-PESIHPC/Phycobank/";
    }

    public static URI fileURI() {
        return URI.create(filePath() + fileName());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        PhycobankHigherClassificationActivator me = new PhycobankHigherClassificationActivator();
        me.doImport(cdmDestination);
    }

}
