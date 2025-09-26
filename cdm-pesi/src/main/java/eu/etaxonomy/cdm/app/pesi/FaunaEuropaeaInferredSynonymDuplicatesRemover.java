/**
* Copyright (C) 2024 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*/
package eu.etaxonomy.cdm.app.pesi;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.config.SynonymDeletionConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * Removes inferred synonym duplicates. See also https://dev.e-taxonomy.eu/redmine/issues/10816
 *
 * @author muellera
 * @since 05.04.2024
 */
public class FaunaEuropaeaInferredSynonymDuplicatesRemover {

    private static final Logger logger = LogManager.getLogger();

//    private static final ICdmDataSource datasource = CdmDestinations.cdm_production_fauna_europaea();
    private static final ICdmDataSource datasource = CdmDestinations.cdm_pesi2025_final();

    public static CdmApplicationController startDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation) {

		logger.info("Start script for database '" + db.getName() + "'");
		CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation);
		return appCtrInit;
    }

    private static void runScript(CdmApplicationController app) {

	    List<Taxon> taxa = null;
	    for (int i = 0; taxa == null || taxa.size() > 0; i++) {
	        TransactionStatus txStatus = app.startTransaction();
	        Set<Synonym> synonymsToBeRemoved = new HashSet<>();

	        int pagesize = 100;
	        taxa = app.getTaxonService().list(Taxon.class, pagesize, i * pagesize, null, null);
	        System.out.println(i);
	        taxa.stream()
	            .flatMap(t->t.getSynonyms().stream())
	            .filter(s->s.getType().isInferredSynonym())
	            .forEach(s->{
	                if (otherSynonymExists(s, synonymsToBeRemoved)) {
	                    System.out.println("Superfluous: " +  s.getTitleCache() + "; NameId: " + s.getName().getId() + "; SynId: " + s.getId());
	                    synonymsToBeRemoved.add(s);
	                }
	            });
	        if (!synonymsToBeRemoved.isEmpty()) {
	            synonymsToBeRemoved.forEach(s->{
	                s.getAcceptedTaxon().removeSynonym(s);
	                SynonymDeletionConfigurator config = new SynonymDeletionConfigurator();
	                app.getTaxonService().deleteSynonym(s, config);
	            });
	        }

	        app.commitTransaction(txStatus);
	    }
	}

    private static boolean otherSynonymExists(Synonym inferredSyn, Set<Synonym> alreadyRemovedSyns) {
        Optional<Synonym> existingSyns = inferredSyn.getAcceptedTaxon().getSynonyms().stream()
            .filter(s-> inferredSyn.getName().getNameCache().equals(s.getName().getNameCache())
                    && s.getName().getTitleCache().equals(inferredSyn.getName().getTitleCache())
                    && s.getId() != inferredSyn.getId()
                    && s.getName().getId() != inferredSyn.getName().getId()
                    && !alreadyRemovedSyns.contains(inferredSyn)
                    && !alreadyRemovedSyns.contains(s)
                    ).findAny();
        return existingSyns.isPresent();
    }

    public static void main(String[] args) {
	    DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
	    CdmApplicationController app = startDb(datasource, schemaValidation);
	    runScript(app);
	    app.close();
	    System.exit(0);
	}
}