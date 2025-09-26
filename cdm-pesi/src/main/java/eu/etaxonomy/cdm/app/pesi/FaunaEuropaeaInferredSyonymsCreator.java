/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*/
package eu.etaxonomy.cdm.app.pesi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.IInferredSynonymsService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * Create inferred synonyms in Fauna Europaea.
 *
 * See #10816 for how to remove duplicates if they exist.
 *
 * @author muellera
 * @since 18.09.2025
 */
public class FaunaEuropaeaInferredSyonymsCreator {

    private static final Logger logger = LogManager.getLogger();

//    private static final ICdmDataSource datasource = CdmDestinations.cdm_production_fauna_europaea();
    private static final ICdmDataSource datasource = CdmDestinations.cdm_pesi2025_final();

    public static CdmApplicationController startDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation) {

		logger.info("Start script for database '" + db.getName() + "'");
		CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation);
		return appCtrInit;
    }

    private static void runScript(CdmApplicationController app) {

        UUID fauEuClassificationUuid = UUID.fromString("44d8605e-a7ce-41e1-bee9-99edfec01e7c");
        UUID pesi2025ClassificationUuid = UUID.fromString("6fa988a9-10b7-48b0-a370-2586fbc066eb");
//        UUID classificationUuid = fauEuClassificationUuid;
        UUID classificationUuid = pesi2025ClassificationUuid;

	    List<Taxon> taxa = null;
	    IInferredSynonymsService inferredSynService = (IInferredSynonymsService)app.getBean("inferredSynonymsServiceImpl");
	    Set<String> nameCaches = new HashSet<>(inferredSynService.getDistinctNameCaches());

	    boolean doPersist = true;
	    for (int pageNo = 0; taxa == null || taxa.size() > 0; pageNo++) {
	        TransactionStatus txStatus = app.startTransaction();

	        int pagesize = 100;
	        taxa = app.getTaxonService().list(Taxon.class, pagesize, pageNo * pagesize, null, null);
	        System.out.println(pageNo);
	        for (Taxon taxon : taxa) {
	            if (!isFauEuTaxon(taxon)) {
	                continue;
	            }

	            List<Synonym> inferredSynList = inferredSynService.createAllInferredSynonyms(
	                    taxon.getUuid(), classificationUuid, true, true, nameCaches, doPersist);
	            if (inferredSynList.isEmpty()) {
	                System.out.print(".");
	            }else {
	                System.out.println(inferredSynList);
	            }
	        }
	        System.out.println(pageNo + ".");

	        app.commitTransaction(txStatus);
	    }
	}

    /**
     * Returns <code>true</code> if the taxon as the "Fauna Europaea PESI 2025"
     * import source.
     */
    private static boolean isFauEuTaxon(Taxon taxon) {
        UUID FauEu2025uuid = PesiTransformer.uuidSourceRefFaunaEuropaea;
        boolean result = taxon.getSources().stream()
            .filter(s->s.getType() == OriginalSourceType.Import)
            .filter(s->s.getCitation() != null)
            .anyMatch(s->s.getCitation().getUuid().equals(FauEu2025uuid));
        return result;
    }

    public static void main(String[] args) {
	    DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
	    CdmApplicationController app = startDb(datasource, schemaValidation);
	    runScript(app);
	    app.close();
	    System.exit(0);
	}
}