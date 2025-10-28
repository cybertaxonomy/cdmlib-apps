/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.greece.CentaureaSpecimenImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author muellera
 * @since 21.10.2025
 */
public class CentaureaSpecimenImportActivator {

    private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_greece();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

    static final String fileName = "Lopholoma_ExcelSpecimenImportTemplate_edited2.xlsx";

    static int minRow = 1;
    static boolean doDetermination = false;
    static boolean dedupAllDatabaseAuthors = false;

    static UUID centauereaClassificationUuid = UUID.fromString("5e86b24a-fa01-4dc4-b0b6-7c20e1351b4e");

    private void doInvoke(ICdmDataSource destination) {

        URI source = specimenExcelUri();

        //make Source
        CentaureaSpecimenImportConfigurator config = CentaureaSpecimenImportConfigurator.NewInstance(source, destination);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
//        config.setCheck(check);
        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

        config.setClassificationUuid(centauereaClassificationUuid);
        config.setMinLineNumber(minRow);
        config.setDoDetermination(doDetermination);
//        config.setMaxLineNumber(maxRow);
//        config.setDeduplicateReferences(dedupRefs);
//        config.setSecReference(getSecReference());
        config.setDeduplicateAuthors(dedupAllDatabaseAuthors);
//        config.setOnlyNonCdmTaxa(onlyNonCdmTaxa);

        config.setSource(source);
//        String fileName = source.toString();
//        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<CentaureaSpecimenImportConfigurator> myImport = new CdmDefaultImport<>();

        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());

        System.out.println("End import from (" + source.toString() + ")...");
    }

    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle(fileName);
        result.setUuid(UUID.fromString("9526cc75-0012-471b-ad2e-86be105ba9ad"));
        return result;
    }

    private URI specimenExcelUri(){
        return URI.create("file:////BGBM-PESIHPC/Greece/" + fileName);
    }

    public static void main(String[] args) {
        ICdmDataSource cdmDB = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
        CentaureaSpecimenImportActivator myImport = new CentaureaSpecimenImportActivator();
        myImport.doInvoke(cdmDB);
        System.exit(0);
    }
}