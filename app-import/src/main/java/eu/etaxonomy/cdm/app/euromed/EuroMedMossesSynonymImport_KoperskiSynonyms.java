
/**
* Copyright (C) 2024 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.euromed;

import java.sql.ResultSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.homotypicgroup.BasionymRelationCreator;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author muellera
 * @since 22.05.2024
 */
public class EuroMedMossesSynonymImport_KoperskiSynonyms {

    private static final Logger logger = LogManager.getLogger();

    private static final ICdmDataSource datasource = CdmDestinations.cdm_local_euromed();
//    private static final ICdmDataSource datasource = CdmDestinations.cdm_production_euromed();

    public static CdmApplicationController startDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation) {

        logger.info("Start script for database '" + db.getName() + "'");
        CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation);
        return appCtrInit;
    }

    private static void runScript(CdmApplicationController app) {
        INonViralNameParser<TaxonName> parser = (INonViralNameParser)NonViralNameParserImpl.NewInstance();
        ImportDeduplicationHelper dedupHelper = ImportDeduplicationHelper.NewInstance(app, null);

        String sql = "SELECT * FROM Koperski_synonym ORDER BY accUuid, synname";
        Source source = new Source(Source.MYSQL, "160.45.63.171",
                "cdm_production_euromed_mosses_syn2acc", 3306);
        //XXXXXXXXXXXXXXXXXXX
        source.setUserAndPwd("XXX", "XXX");

        TransactionStatus tx = app.startTransaction();
        ResultSet rs = source.getResultSet(sql);
        Reference sourceRef = ReferenceFactory.newBook();
        sourceRef.setTitle("Referenzliste der Moose Deutschlands");
        sourceRef.setAuthorship(Team.NewTitledInstance("Monika Koperski, Michael Sauer, Walther Braun, und S. Rob Gradstein", null));
        sourceRef.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(2000));
        app.getReferenceService().save(sourceRef);
        try {
            int line = 1;
            while (rs.next()) {
                String lineStr = line + ": ";
                UUID taxonUuid = UUID.fromString(rs.getString("accUuid"));
                String synName = rs.getString("synname");

                Taxon taxon = (Taxon)app.getTaxonService().findTaxonByUuid(taxonUuid, null);
                TaxonName newSynonymName = parser.parseFullName(synName, NomenclaturalCode.ICNAFP, null);
                dedupHelper.replaceAuthorNamesAndNomRef(newSynonymName);
                newSynonymName.addImportSource(null, null, sourceRef, null);

                boolean hasBasionym = false;
                if (newSynonymName.getBasionymAuthorship() != null) {
                    hasBasionym = checkIsBasionym(lineStr, taxon.getName(), newSynonymName);
                    if (!hasBasionym) {
                        for (TaxonName syn : taxon.getSynonymNames()) {
                            hasBasionym = checkIsBasionym(lineStr, syn, newSynonymName);
                            if (hasBasionym) {
                                break;
                            }
                        }
                    }
                    if (!hasBasionym) {
                        logger.warn(lineStr + "New synonym has basionym author but no existing basionym found: " + newSynonymName.getTitleCache() + "; acc: " + taxon.getName().getTitleCache());
                    }
                }
                SynonymType synonymType = (newSynonymName.getHomotypicalGroup().equals(taxon.getName().getHomotypicalGroup())) ?
                        SynonymType.HOMOTYPIC_SYNONYM_OF : SynonymType.HETEROTYPIC_SYNONYM_OF;

                Synonym newSynonym = taxon.addSynonymName(newSynonymName, synonymType);
                newSynonym.addImportSource(null, null, sourceRef, null);
                app.getTaxonService().save(newSynonym);

                line++;
            }
            app.commitTransaction(tx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkIsBasionym(String line, TaxonName basionymCandidate, TaxonName combinationCandidate) {
        if (basionymCandidate.getCombinationAuthorship() == null) {
            return false;
        }
        String basionymCandidateAuthorStr = basionymCandidate.getCombinationAuthorship().getNomenclaturalTitleCache();
        String combinationAuthorshipStr = combinationCandidate.getBasionymAuthorship().getNomenclaturalTitleCache();
        if (CdmUtils.Nz(basionymCandidateAuthorStr.replace(" ", "")).equalsIgnoreCase(CdmUtils.Nz(combinationAuthorshipStr).replace(" ", ""))) {
            boolean lastEpiMatches = BasionymRelationCreator.matchLastNamePart(basionymCandidate, combinationCandidate);
            if (lastEpiMatches) {
                logger.warn(line + "Basionym seems to exist already. Added name to homotypic group: " +  combinationCandidate.getTitleCache() +  "->" + basionymCandidate.getTitleCache());
                combinationCandidate.setHomotypicalGroup(basionymCandidate.getHomotypicalGroup());
                combinationCandidate.addBasionym(basionymCandidate);
                return true;
            }else {
                logger.warn(line + "Authors could be basionym authors, but epithets differ: " +  combinationCandidate.getTitleCache() +  ", " + basionymCandidate.getTitleCache());
            }
        }
        return false;
    }

    public static void main(String[] args) {
        DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
        CdmApplicationController app = startDb(datasource, schemaValidation);
        runScript(app);
        app.close();
        System.exit(0);
    }
}