/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.app.abcdImport;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.specimen.excel.in.SpecimenSynthesysExcelImportConfigurator;

/**
 * @author PK
 * @since 19.09.2008
 */
public class SynthesysCacheActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)

//    final static String xmlSource = "/home/pkelbert/workspace/proibiosphere/cdmlib-io/target/test-classes/eu/etaxonomy/cdm/io/specimen/excel/in/ExcelImportConfiguratorTest-input.xls";
    final static String xmlSource = "/home/pkelbert/Documents/Proibiosphere/ChenopodiumQuentin/GBIF data etc.xls";

//  static final ICdmDataSource cdmDestination = CdmDestinations.mon_cdm();
//    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_proibiosphere_chenopodium();
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    public static void main(String[] args) {
        URI source;
        try {
            URI uri = URI.fromFile(new File(xmlSource));
            source = new URI(uri.toString());
            System.out.println(source.toString());
            System.out.println("Start import from  Excel Specimen data("+ source.toString() + ") ...");

            ICdmDataSource destination = cdmDestination;
            SpecimenSynthesysExcelImportConfigurator specimenImportConfigurator = SpecimenSynthesysExcelImportConfigurator.NewInstance(source,  destination);

            specimenImportConfigurator.setSourceSecId("specimen");
            specimenImportConfigurator.setCheck(check);
            specimenImportConfigurator.setDbSchemaValidation(hbm2dll);
            specimenImportConfigurator.setDoAutomaticParsing(true);
            specimenImportConfigurator.setReUseExistingMetadata(true);
            specimenImportConfigurator.setAskForDate(true);
            specimenImportConfigurator.setDefaultAuthor("L.");
            specimenImportConfigurator.setClassificationName("Chenopodium");

            specimenImportConfigurator.setReUseTaxon(true);

            specimenImportConfigurator.setSourceReference(null);
            specimenImportConfigurator.setTaxonReference(null);

            // invoke import
            CdmDefaultImport<SpecimenSynthesysExcelImportConfigurator> specimenImport = new CdmDefaultImport<SpecimenSynthesysExcelImportConfigurator>();
            //new Test().invoke(tcsImportConfigurator);
            specimenImport.invoke(specimenImportConfigurator);
            System.out.println("End import from SpecimenData ("+ source.toString() + ")...");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}