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

import com.microsoft.sqlserver.jdbc.StringUtils;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author muellera
 * @since 06.05.2024
 */
public class EuroMedMossesSynonymImport_EuroMossesNames {

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

        String sql = "SELECT * FROM synonyms ORDER BY id";
        Source source = new Source(Source.MYSQL, "160.45.63.171",
                "cdm_production_euromed_mosses_syn2acc", 3306);
        //XXXXXXXXXXXXXXXXXXX
        source.setUserAndPwd("XXX", "XXX");

        TransactionStatus tx = app.startTransaction();
        ResultSet rs = source.getResultSet(sql);
        try {
            int line = 1;
            while (rs.next()) {
                String lineStr = line + ": ";
                UUID taxonUuid = UUID.fromString(rs.getString("accUuid"));
                String scientificname = rs.getString("scientificname");
                String authorship = rs.getString("authorship");
                String fullName = rs.getString("Fullname");

                Taxon taxon = (Taxon)app.getTaxonService().findTaxonByUuid(taxonUuid, null);
                TaxonName synonymName = parser.parseFullName(fullName, NomenclaturalCode.ICNAFP, null);

                Synonym newSynonym = taxon.addSynonymName(synonymName, SynonymType.SYNONYM_OF);
                if (synonymName.getBasionymAuthorship() != null) {
                    String basionymAuthorStr = synonymName.getBasionymAuthorship().getNomenclaturalTitleCache();
                    checkIsBasionym(lineStr, basionymAuthorStr, taxon.getName().getCombinationAuthorship(), synonymName.getTitleCache());
                    for (TaxonName syn : taxon.getSynonymNames()) {
                        checkIsBasionym(lineStr, basionymAuthorStr, syn.getCombinationAuthorship(), synonymName.getTitleCache());
                    }
                }


                app.getTaxonService().save(newSynonym);

                line++;
            }
            app.commitTransaction(tx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkIsBasionym(String line, String basionymAuthorStr, TeamOrPersonBase<?> combinationAuthorship, String taxonNameStr) {
        String candidateAuthor = combinationAuthorship.getNomenclaturalTitleCache();
        if (basionymAuthorStr.replace(" ", "").equalsIgnoreCase(CdmUtils.Nz(candidateAuthor).replace(" ", ""))) {
            logger.warn(line + "Basionym may exist already: " +  taxonNameStr);
        }
    }

    private static Rank getRank(String rankStr) {
        if (StringUtils.isEmpty(rankStr)) {
            return null;
        }else if ("subgenus".equals(rankStr)) {
            return Rank.SUBGENUS();
        }else if ("species".equals(rankStr)) {
            return Rank.SPECIES();
        }else if ("variety".equals(rankStr)) {
            return Rank.VARIETY();
        }else if ("infrageneric name".equals(rankStr)) {
            return Rank.INFRAGENERICTAXON();
        }else if ("section botany".equals(rankStr)) {
            return Rank.SECTION_BOTANY();
        }else if ("subsection botany".equals(rankStr)) {
            return Rank.SUBSECTION_BOTANY();
        }else if ("subspecies".equals(rankStr)) {
            return Rank.SUBSPECIES();
        }else if ("form".equals(rankStr)) {
            return Rank.FORM();
        }else if ("infraspecific name".equals(rankStr)) {
            return Rank.INFRASPECIFICTAXON();
        }else {
            logger.warn("Rank not yet handled "+ rankStr);
            return null;
        }
    }

    public static void main(String[] args) {
        DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
        CdmApplicationController app = startDb(datasource, schemaValidation);
        runScript(app);
        app.close();
        System.exit(0);
    }
}