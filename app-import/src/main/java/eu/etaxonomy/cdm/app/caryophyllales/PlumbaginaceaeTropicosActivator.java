/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.caryophyllales;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.tropicos.in.TropicosNameImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 18.10.2017
 */
public class PlumbaginaceaeTropicosActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_test1();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo_spp();

    static final boolean createTaxa = true;
    static int transactionLineCount = 100;
    static boolean allowTropicosDuplicates = true;
    static boolean allowIpniDuplicates = true;
    static boolean allowWfoDuplicates = false;
    static boolean addAuthorsToReferences = true;
    static boolean reportDuplicateIdentifier = true;

    static final String classificationName = "Tropicos Plumbaginaceae";
    static final UUID parentNodeUuid = UUID.fromString("46755269-5b06-4308-9871-c55850610769");

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = getPlumbaginaceaeTropicosUri();
        TropicosNameImportConfigurator config;
        try {
            config = TropicosNameImportConfigurator.NewInstance(source, cdmDestination);

            config.setDbSchemaValidation(dbSchemaValidation);
            config.setCheck(check);
            config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
            config.setCreateTaxa(createTaxa);
            config.setClassificationName(classificationName);
            config.setSourceReference(getSourceReference());
            config.setParentNodeUuid(parentNodeUuid);
            config.setTransactionLineCount(transactionLineCount);
            config.setAllowIpniDuplicates(allowIpniDuplicates);
            config.setAllowTropicosDuplicates(allowTropicosDuplicates);
            config.setAllowWfoDuplicates(allowWfoDuplicates);
            config.setAddAuthorsToReference(addAuthorsToReferences);
            config.setReportDuplicateIdentifier(reportDuplicateIdentifier);

            CdmDefaultImport<TropicosNameImportConfigurator> myImport = new CdmDefaultImport<>();
            ImportResult result = myImport.invoke(config);
            System.out.println(result.createReport());
        } catch (IOException e) {
            System.out.println("URI not 'found': " + source);
        }
    }

    private URI getPlumbaginaceaeTropicosUri(){
//        String fileName = "TropicosNameImportTest-input.txt";
        String fileName = "TropicosOutput2CDM-rev-output_Plumbaginaceae.txt";
//        String fileName = "TropicosOutput2CDM_Plumbaginaceae.txt";

        URI uri = URI.create("file:////BGBM-PESIHPC/Caryophyllales/" +  fileName);
        return uri;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Tropicos Plumbaginaceae import");
        result.setUuid(UUID.fromString("937f3a38-dbb4-4888-9ea3-2f52d47f7953"));
        return result;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        PlumbaginaceaeTropicosActivator me = new PlumbaginaceaeTropicosActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
