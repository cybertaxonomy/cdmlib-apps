/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.parser.BibliographicAuthorParser;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public abstract class MexicoEfloraReferenceImportBase  extends MexicoEfloraImportBase {

    public static final String NAMESPACE = "References";

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
            boolean isEds = false;
            boolean isEd = false;
            boolean isCoords = false;
            boolean isCoord = false;
            boolean isComps = false;
            boolean isComp = false;

            if (authorStr.endsWith("(Eds.)") || authorStr.endsWith("(eds.)")
               || authorStr.endsWith("(editors)")) {
                isEds = true;
                authorStr = authorStr.replace("(Eds.)", "").replace("(eds.)", "").replace("(editors)", "").trim();
            }
            if (authorStr.endsWith("(Ed.)")) {
                authorStr = authorStr.replace("(Ed.)", "").trim();
                isEd = true;
            }
            if (authorStr.endsWith("(Coords.)") || authorStr.endsWith("(coords.)")) {
                authorStr = authorStr.replace("(Coords.)", "").replace("(coords.)", "").trim();
                isCoords = true;
            }
            if (authorStr.endsWith("(Coord.)") || authorStr.endsWith("(coord.)")) {
                authorStr = authorStr.replace("(Coord.)", "").replace("(coord.)", "").trim();
                isCoords = true;
            }
            if (authorStr.endsWith("(Comps.)") || authorStr.endsWith("(comps.)")) {
                authorStr = authorStr.replace("(comps.)", "").replace("(Comps.)", "").trim();
                isComps = true;
            }
            if (authorStr.endsWith("(Comp.)") || authorStr.endsWith("(comp.)")) {
                authorStr = authorStr.replace("(comp.)", "").replace("(Comp.)", "").trim();
                isComps = true;
            }

            TeamOrPersonBase<?> author = BibliographicAuthorParser
                    .Instance().parse(authorStr);
            if (isEds) {
                author.setTitleCache(author.getTitleCache()+ " (Eds.)", true);
            }else if (isCoords) {
                author.setTitleCache(author.getTitleCache()+ " (Coords.)", true);
            }else if (isCoord) {
                author.setTitleCache(author.getTitleCache()+ " (Coord.)", true);
            }else if (isEd) {
                author.setTitleCache(author.getTitleCache()+ " (Ed.)", true);
            }else if (isComps) {
                author.setTitleCache(author.getTitleCache()+ " (Comps.)", true);
            }else if (isComp) {
                author.setTitleCache(author.getTitleCache()+ " (Comp.)", true);
            }


            //not sure what is better, but seems to work with "false"
            boolean parsed = false;
            author = state.getDeduplicationHelper().getExistingAuthor(author, parsed);
            ref.setAuthorship(author);
        }else {
            logger.warn(refId + ": No author");
        }
	}

    protected void handleYearStr(@SuppressWarnings("unused") MexicoEfloraImportState state,
            String yearStr, Reference ref, int refId) {
        //year
        if (isNotBlank(yearStr)) {
            VerbatimTimePeriod tp = TimePeriodParser.parseStringVerbatim(yearStr);
            ref.setDatePublished(tp);
        }else {
            logger.warn(refId + ": No year");
            }
    }

    protected void handleTitleStr(@SuppressWarnings("unused") MexicoEfloraImportState state,
            String titleStr, Reference ref, @SuppressWarnings("unused") int refId) {

        //articleTitle
        if (isNotBlank(titleStr)) {
            ref.setTitle(titleStr);
        }else {
//            logger.warn(refId + ": No title");
        }
    }

    protected void handleUrlStr(@SuppressWarnings("unused") MexicoEfloraImportState state,
            String urlStr, Reference ref, int refId) {

        //url
        if (isNotBlank(urlStr)) {
            try {
                URI uri = new URI(urlStr);
                //for now we handle the uri in the URI field also for non-websites
                //will be moved to external link by model update script in future
                ref.setUri(uri);
            } catch (URISyntaxException e) {
                logger.warn(refId + ": URL could not be parsed: " + urlStr);
            }
        }
    }

    protected void handleDoiStr(@SuppressWarnings("unused") MexicoEfloraImportState state,
            String doiStr, Reference ref, int refId) {

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

    protected void handleId(MexicoEfloraImportState state, int refId, Reference ref) {
        state.getReferenceUuidMap().put(refId, ref.getUuid());

        //.. identifier
        DefinedTerm conabioIdentifier = getIdentiferType(state, MexicoConabioTransformer.uuidConabioReferenceIdIdentifierType,
                "CONABIO Reference Identifier", "CONABIO Reference Identifier", "CONABIO", null);
        ref.addIdentifier(String.valueOf(refId), conabioIdentifier);
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoReferences();
	}
}