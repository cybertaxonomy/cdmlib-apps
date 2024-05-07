/**
* Copyright (C) 2024 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*/
package eu.etaxonomy.cdm.app.common.tasks;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author muellera
 * @since 05.04.2024
 */
public class AnyScript {

    private static final Logger logger = LogManager.getLogger();

    private static final ICdmDataSource datasource = CdmDestinations.cdm_local_euromed();

    public static CdmApplicationController startDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation) {

		logger.info("Start script for database '" + db.getName() + "'");
		CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation);
		return appCtrInit;
    }

    private static void runScript(CdmApplicationController app) {
	    TransactionStatus txStatus = app.startTransaction();

	    List<Taxon> taxa = app.getTaxonService().list(Taxon.class, null, null, null, null);
        taxa.forEach(t->{
            Set<TextData> facts = t.getDescriptionItems(Feature.ETYMOLOGY(), TextData.class);
            if (facts.size() > 1) {
                logger.warn("More then 1 etymology fact exists for " + t.getTitleCache() + "/" + t.getUuid());
            }
            facts.forEach(f->{
                f.getInDescription().removeElement(f);
                TextData clone = f.clone();
                if (t.getName().getDescriptions().isEmpty()) {
                   TaxonNameDescription d = TaxonNameDescription.NewInstance(t.getName());
                   d.setTitleCache("Etymology fact moved from taxon to name", true);
                }
                t.getName().getDescriptions().iterator().next().addElement(clone);
            });
        });

	    app.commitTransaction(txStatus);
	}

	public static void main(String[] args) {
	    DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
	    CdmApplicationController app = startDb(datasource, schemaValidation);
	    runScript(app);
	    app.close();
	    System.exit(0);
	}
}