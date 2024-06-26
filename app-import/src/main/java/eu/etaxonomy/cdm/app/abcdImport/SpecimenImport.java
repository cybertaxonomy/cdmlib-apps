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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.specimen.abcd206.in.Abcd206ImportConfigurator;

public class SpecimenImport {
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger(SpecimenImport.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
    //  final static String xmlSource = "/home/patricia/Desktop/multiABCD.xml";
    //final static String xmlSource = "D:\\_Tagungen\\2010-09 TDWG 2010\\Workshop\\data\\specimen\\Picris pauciflora\\B-W14632-000_B-W14632-010_B100097145_B100097146_B100326668_B180004364_B180017717_.xml";

    final static String xmlSource = "/home/pkelbert/Documents/Proibiosphere/Presentations/Demo/ABCD/calvum.xml";

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.makeDestination(DatabaseTypeEnum.H2, "localhost", "abcd_import_test", 8082, "root", "root");
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    public static void main(String[] args) {

        URI source;
        try {
            URI uri = URI.fromFile(new File(xmlSource));
            source = new URI(uri.toString());
            System.out.println(source.toString());
            System.out.println("Start import from  ABCD Specimen data("+ source.toString() + ") ...");

            ICdmDataSource destination = cdmDestination;
            Abcd206ImportConfigurator specimenImportConfigurator = Abcd206ImportConfigurator.NewInstance(source, destination);

////            specimenImportConfigurator.setClassificationName(specimenImportConfigurator.getSourceReferenceTitle());
            specimenImportConfigurator.setSourceSecId("specimen");
            specimenImportConfigurator.setCheck(check);
            specimenImportConfigurator.setDbSchemaValidation(hbm2dll);
            specimenImportConfigurator.setReuseExistingMetaData(true);
            specimenImportConfigurator.setReuseExistingDescriptiveGroups(true);
//
            specimenImportConfigurator.setReuseExistingTaxaWhenPossible(true);
//
            specimenImportConfigurator.setAddIndividualsAssociations(true);
            specimenImportConfigurator.setUseClassification(true);
            specimenImportConfigurator.setInteractWithUser(true);
//
            specimenImportConfigurator.setSourceReference(null);
            specimenImportConfigurator.setTaxonReference(null);

            // invoke import
            CdmDefaultImport<Abcd206ImportConfigurator> specimenImport = new CdmDefaultImport<Abcd206ImportConfigurator>();
            //new Test().invoke(tcsImportConfigurator);
            specimenImport.invoke(specimenImportConfigurator);
            System.out.println("End import from SpecimenData ("+ source.toString() + ")...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}