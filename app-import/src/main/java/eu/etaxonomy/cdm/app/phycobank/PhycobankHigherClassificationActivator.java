/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.phycobank;

import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.phycobank.PhycobankHigherClassificationImportConfigurator;
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
    private static final Logger logger = LogManager.getLogger();

    // ====================================================================================

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;  //

//    static ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
//    static ICdmDataSource cdmDestination = CdmDestinations.cdm_test_phycobank();
    static ICdmDataSource cdmDestination = CdmDestinations.cdm_phycobank_production();

    static String worksheetName = "Syllabus2_1";

    static UUID uuidRefPhycobank = UUID.fromString("8058a5ec-60ee-4a04-8c17-5623e3a4795c");

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;


    private void doImport(ICdmDataSource cdmDestination){
        hbm2dll = (cdmDestination == CdmDestinations.cdm_phycobank_production()|| cdmDestination == CdmDestinations.cdm_test_phycobank())?
                DbSchemaValidation.VALIDATE : hbm2dll;

        URI source = fileURI();
        Reference sourceRef = getSourceReference();

        //make Source
        PhycobankHigherClassificationImportConfigurator config= PhycobankHigherClassificationImportConfigurator.NewInstance(source, cdmDestination);
        config.setWorksheetName(worksheetName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSourceReference(sourceRef);
        config.setPhycobankReference(getPhycobankReference());
        config.setProgressMonitor(DefaultProgressMonitor.NewInstance());

        CdmDefaultImport<PhycobankHigherClassificationImportConfigurator> myImport = new CdmDefaultImport<>();
        myImport.invoke(config);

        System.exit(0);
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
        return "Algen_Syllabus_Produktion_2019-01-08_Import_2.xlsx";
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
