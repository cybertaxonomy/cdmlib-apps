/**
* Copyright (C) 2018 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.greece.GreeceWillingImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 21.08.2018
 *
 */
public class GreeceWillingActivator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GreeceWillingActivator.class);


    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_greece_checklist_production();

    private static final UUID sourceUuid = UUID.fromString("b96b8a10-e2a5-4a01-b2f2-435a59a7a269");

    private static final String fileName = "WillingImport.xslx";

    private void doImport(ICdmDataSource cdmDestination){

        DbSchemaValidation schemaVal = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;
        URI source = greekChecklist();  //just any
        //make Source
        GreeceWillingImportConfigurator config = GreeceWillingImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(CHECK.IMPORT_WITHOUT_CHECK);
        config.setDbSchemaValidation(schemaVal);
        config.setSourceReference(getSourceReference());
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);

        CdmDefaultImport<GreeceWillingImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());

    }


    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/" + fileName);
    }


    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
//        xx;
        result.setTitle(fileName);
        result.setUuid(sourceUuid);

        return result;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GreeceWillingActivator me = new GreeceWillingActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }

}
