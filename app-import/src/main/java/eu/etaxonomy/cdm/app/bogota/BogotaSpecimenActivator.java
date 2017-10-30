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
import eu.etaxonomy.cdm.io.bogota.BogotaSpecimenImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Activator for import of Bogota Checklist
 *
 * @author a.mueller
 * @date 21.04.2017
 *
 */
public class BogotaSpecimenActivator {
    private static final Logger logger = Logger.getLogger(BogotaSpecimenActivator.class);

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_bogota();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_bogota_production();

//    int minRow = 6;
//    int maxRow = 15; //minRow + 11999;

    int minRow = 180;
    int maxRow = 191; //minRow + 11999;

    boolean dedupRefs = false;
    boolean dedupAuthors = false;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = bogotaSpecimens();

        //make Source
        BogotaSpecimenImportConfigurator config= BogotaSpecimenImportConfigurator.NewInstance(source, cdmDestination);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        config.setCheck(check);
        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

        config.setMinLineNumber(minRow);
        config.setMaxLineNumber(maxRow);
        config.setDeduplicateReferences(dedupRefs);
        config.setDeduplicateAuthors(dedupAuthors);

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<BogotaSpecimenImportConfigurator> myImport = new CdmDefaultImport<>();

        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());

        System.out.println("End import from (" + source.toString() + ")...");

    }


    //bogotaChecklist
    public static URI bogotaSpecimens() {
        return URI.create("file:////BGBM-PESIHPC/Bogota/Flora_de_Bogota_Dataset_20170901_GB20171011_14607-entries-to-import_GB_20171016.xlsx");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Flora_de_Bogota_Dataset_20170901_GB20171011_14607-entries-to-import_GB_20171016.xlsx");
        result.setUuid(UUID.fromString("05e8c346-4809-4323-a484-822c92ad033d"));
        return result;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        BogotaSpecimenActivator me = new BogotaSpecimenActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
