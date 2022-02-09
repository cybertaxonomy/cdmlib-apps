/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public abstract class MexicoEfloraReferenceImportBase  extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -5161951752826380728L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraReferenceImportBase.class);

    public MexicoEfloraReferenceImportBase(String dbTableName, String pluralString){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT 'CONABIO-BIB-ID' "
		        + " FROM " + getTableName()
		        + " ORDER BY 'CONABIO-BIB-ID' ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + getTableName();
		String sqlWhere = " WHERE ( 'CONABIO-BIB-ID' IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	protected boolean doCheck(MexicoEfloraImportState state){
	    return true;
	}

	protected void handleAuthorStr(MexicoEfloraImportState state, String authorStr, Reference ref, int refId) {

        //author
        if (isNotBlank(authorStr)) {
            TeamOrPersonBase<?> author = Team.NewInstance();
            //TODO parse
            author.setTitleCache(authorStr, true);
            //TODO is parsed really ok?
            state.getDeduplicationHelper().getExistingAgent(author, true);
            ref.setAuthorship(author);
        }else {
            logger.warn(refId + ": No author");
        }
	}

    protected void handleYearStr(MexicoEfloraImportState state, String yearStr, Reference ref, int refId) {
        //year
        if (isNotBlank(yearStr)) {
            VerbatimTimePeriod tp = TimePeriodParser.parseStringVerbatim(yearStr);
            ref.setDatePublished(tp);
        }else {
            logger.warn(refId + ": No year");
            }
    }

    protected void handleTitleStr(MexicoEfloraImportState state, String titleStr, Reference ref, int refId) {

        //articleTitle
        if (isNotBlank(titleStr)) {
            ref.setTitle(titleStr);
        }else {
            logger.warn(refId + ": No title");
        }
    }

    protected void handleUrlStr(MexicoEfloraImportState state, String urlStr, Reference ref, int refId) {

        //url
        if (isNotBlank(urlStr)) {
            //TODO
//                        URI
//                        IJournal journal = ReferenceFactory.newJournal();
//                        journal.setTitle(journalTitleStr);
//                        ref.setInJournal(journal);
        }else {
//                        logger.warn(refId + ": No url");
        }
    }

    protected void handleDoiStr(MexicoEfloraImportState state, String doiStr, Reference ref, int refId) {

        //doi
        if (isNotBlank(doiStr)) {
            DOI doi;
            try {
                doi = DOI.fromString(doiStr);
                ref.setDoi(doi);
            } catch (Exception e) {
                logger.warn(refId + ": Doi could not be parsed: " + doiStr);
            }
        }
    }

    @Override
    protected String getIdInSource(MexicoEfloraImportState state, ResultSet rs) throws SQLException {
        String id = rs.getString("idInSource");
        return id;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoReferences();
	}
}