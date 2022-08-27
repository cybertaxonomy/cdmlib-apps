/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

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
import eu.etaxonomy.cdm.io.euromed.IpniSourcesImportConfigurator;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 18.10.2019
 */
public class EuroMedIpniDistributionSourcesImportActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    //database validation status (create, update, validate ...)
    static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_euromed();
//   static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

    //check - import
    static CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        URI source = getEM_IpniUri();
        IpniSourcesImportConfigurator config;
//        try {
            config = IpniSourcesImportConfigurator.NewInstance(source, cdmDestination);

            config.setDbSchemaValidation(dbSchemaValidation);
            config.setCheck(check);
            config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
            config.setSourceReference(getSourceReference());
//            config.setAddAuthorsToReference(addAuthorsToReferences);

            CdmDefaultImport<IpniSourcesImportConfigurator> myImport = new CdmDefaultImport<>();
            ImportResult result = myImport.invoke(config);
            System.out.println(result.createReport());
//        } catch (IOException e) {
//            System.out.println("URI not 'found': " + source);
//        }
    }

    private URI getEM_IpniUri(){
        String fileName = "IPNI-name-EM-import.xlsx";

        URI uri = URI.create("file:////BGBM-PESIHPC/EuroMed/" +  fileName);
        return uri;
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle("Euro+Med IPNI import");
        result.setUuid(UUID.fromString("937f3a38-dbb4-4888-9ea3-2f52d47f7953"));
        result.setDatePublished(VerbatimTimePeriod.NewVerbatimNowInstance());
        return result;
    }

    public static void main(String[] args) {
        EuroMedIpniDistributionSourcesImportActivator me = new EuroMedIpniDistributionSourcesImportActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
