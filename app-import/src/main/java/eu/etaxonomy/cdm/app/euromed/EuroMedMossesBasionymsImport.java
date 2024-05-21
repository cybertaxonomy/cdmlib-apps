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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.persistence.query.MatchMode;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author muellera
 * @since 06.05.2024
 */
public class EuroMedMossesBasionymsImport {

    private static final Logger logger = LogManager.getLogger();

//    private static final ICdmDataSource datasource = CdmDestinations.cdm_local_euromed();
    private static final ICdmDataSource datasource = CdmDestinations.cdm_production_euromed();

    public static CdmApplicationController startDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation) {

        logger.info("Start script for database '" + db.getName() + "'");
        CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation);
        return appCtrInit;
    }

    private static void runScript(CdmApplicationController app) {
        INonViralNameParser<TaxonName> parser = (INonViralNameParser)NonViralNameParserImpl.NewInstance();

        String sql = "SELECT * FROM basionyms ORDER BY id";
        Source source = new Source(Source.MYSQL, "160.45.63.171",
                "cdm_production_euromed_mosses_basionyms", 3306);
        //XXXXXXXXXXXXXXXXXXX
        source.setUserAndPwd("XXX", "XXX");

        TransactionStatus tx = app.startTransaction();
        ResultSet rs = source.getResultSet(sql);
        try {
            int line = 1;
            while (rs.next()) {
                String lineStr = line + ": ";
                UUID uuid = UUID.fromString(rs.getString("cdmUuid"));
                Integer cdmBasAuthorId = rs.getInt("cdmBasAuthorId");
                Integer cdmExBasAuthorId = rs.getInt("cdmExBasAuthorId");
                String scientificname = rs.getString("scientificname");
                String authorship = rs.getString("authorship");
                String rankStr = rs.getString("rank");
                String genericname = rs.getString("genericname");
                String specificepithet = rs.getString("specificepithet");
                String infraspecificepithet = rs.getString("infraspecificepithet");
                String wfoId = rs.getString("wfo_id");
                String nameAlternativeId = rs.getString("namealternativeid");


                TaxonName taxonName = app.getNameService().find(uuid);
                if (taxonName == null) {
                    logger.warn(lineStr + "Name not found " + uuid);
                    continue;
                }
                //TODO dedup, needed?
                TeamOrPersonBase<?> cdmBasAuthor = (TeamOrPersonBase<?>)app.getAgentService().find(cdmBasAuthorId);
                TeamOrPersonBase<?> cdmExBasAuthor = (TeamOrPersonBase<?>)app.getAgentService().find(cdmExBasAuthorId);

                TaxonName basionymName = null;
                List<TaxonName> existingNames = app.getNameService().findNamesByNameCache(scientificname, MatchMode.EXACT, null);
                if (!existingNames.isEmpty()) {
                    List<TaxonName> matchingNames = existingNames.stream().filter(n->n.getAuthorshipCache().replace(" ", "").equals(authorship.replace(" ", ""))).collect(Collectors.toList());
                    if (matchingNames.isEmpty()) {
                        logger.warn(lineStr + existingNames.size() + " basionym pure name "+scientificname+" exists already but authors differ: " + authorship + "<->" + existingNames.iterator().next().getAuthorshipCache());
                    }else {
                        if (matchingNames.size() > 1) {
                            logger.warn(lineStr + matchingNames.size() + " exact matching names exist. Choosing arbitrary one.");
                        }
                        basionymName = matchingNames.iterator().next();
                        if (basionymName.getIdentifier(IdentifierType.uuidWfoNameIdentifier) == null) {
                            logger.warn(lineStr + "No WFO Identifier exists for existing name: " + scientificname);
                        }
                        if (basionymName.getIdentifier(IdentifierType.uuidTropicosNameIdentifier) == null) {
                            logger.warn(lineStr + "No Tropicos Identifier exists for existing name" + scientificname);
                        }
                    }
                }
                if (basionymName == null) {
                    Rank rank = getRank(rankStr);
//                  TaxonName basionymName = TaxonNameFactory.NewBotanicalInstance(rank, taxonName.getHomotypicalGroup());
                    basionymName = parser.parseSimpleName(scientificname, NomenclaturalCode.ICNAFP, rank);
                    basionymName.addIdentifier(wfoId, IdentifierType.IDENTIFIER_NAME_WFO());
                    if (CdmUtils.isNotBlank(nameAlternativeId)) {
                        if (nameAlternativeId.startsWith("tropicos:")) {
                            basionymName.addIdentifier(nameAlternativeId.replace("tropicos:", ""), IdentifierType.IDENTIFIER_NAME_TROPICOS());
                        }else {
                            logger.warn("Unhandled alternative identifier: " + nameAlternativeId);
                        }
                    }
                    if (cdmBasAuthor != null) {
                        String exBasAuthor = cdmExBasAuthor == null ? null : cdmExBasAuthor.getNomenclaturalTitleCache();
                        String basAndExAuthor = CdmUtils.concat(" ex ", exBasAuthor, cdmBasAuthor.getNomenclaturalTitleCache());
                        if (!basAndExAuthor.replace(" ", "").equals(authorship.replace(" ", ""))) {
                            logger.warn(lineStr + "Authors differ: " + authorship + "<->" + basAndExAuthor);
                        }
                        basionymName.setCombinationAuthorship(cdmBasAuthor);
                        basionymName.setExCombinationAuthorship(cdmExBasAuthor);
                    }else {
                        logger.warn(lineStr + "No cdmBasAuthor");
                    }
                }

                if (!CdmUtils.nullSafeEqual(basionymName.getGenusOrUninomial(), genericname)) {
                    logger.warn(lineStr + "Genus differs");
                }
                if (!CdmUtils.nullSafeEqual(basionymName.getSpecificEpithet(), CdmUtils.Ne(specificepithet))) {
                    logger.warn(lineStr + "Species epi differs");
                }
                if (!CdmUtils.nullSafeEqual(basionymName.getInfraSpecificEpithet(), CdmUtils.Ne(infraspecificepithet))) {
                    logger.warn(lineStr + "Infra specific epi differs");
                }

                taxonName.addBasionym(basionymName);
                app.getNameService().save(basionymName);

                //taxon base
                Set<TaxonBase> taxonBases = taxonName.getTaxonBases();
                if (taxonBases.isEmpty()) {
                    logger.warn(lineStr + "No taxon base");
                } else {
                    if (taxonBases.size() > 1) {
                        logger.warn(lineStr + "More than 1 taxon base: " + taxonBases.size());
                    }
                    if (basionymName.getTaxonBases().size() > 0) {
                        logger.warn(lineStr + "Basionym is already a synonym: " + basionymName.getTitleCache());
                    }
                    TaxonBase<?> taxonBase = taxonBases.iterator().next();
                    Synonym newSynonym;
                    if (taxonBase.isInstanceOf(Taxon.class)) {
                        Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
                        newSynonym = taxon.addHomotypicSynonymName(basionymName);
                    }else if (taxonBase.isInstanceOf(Synonym.class)) {
                        Synonym synonym = CdmBase.deproxy(taxonBase, Synonym.class);
                        Taxon taxon = synonym.getAcceptedTaxon();
                        newSynonym = taxon.addSynonymName(basionymName, synonym.getType());
                    }else {
                        throw new RuntimeException("Unhandled taxon base type");
                    }
                    app.getTaxonService().save(newSynonym);
                }

                line++;
            }
            app.commitTransaction(tx);
        } catch (Exception e) {
            e.printStackTrace();
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