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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.IJournal;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraRefArticlesImport extends MexicoEfloraReferenceImportBase {

    private static final long serialVersionUID = -1186364983750790695L;

    private static final Logger logger = Logger.getLogger(MexicoEfloraRefArticlesImport.class);

	public static final String NAMESPACE = "Articles";

	private static final String pluralString = "Articles";
	private static final String dbTableName = "RefArticles";

	public MexicoEfloraRefArticlesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;
	    MexicoEfloraImportConfigurator config = state.getConfig();
		Set<Reference> refsToSave = new HashSet<>();

		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				int refId = rs.getInt("CONABIO-BIB-ID");
				String type = rs.getString("PubType");
				String authorStr = rs.getString("Author");
				String yearStr = rs.getString("Year");
				String articleTitleStr = rs.getString("ArticleTitle");
                String journalTitleStr = rs.getString("JournalTitle");
                String concat = rs.getString("Concatenation");
                String vStr = rs.getString("V");
                String pStr = rs.getString("P");
                String urlStr = rs.getString("URL");
                String doiStr = rs.getString("DOI");
                String issnStr = rs.getString("ISSN");

                try {
                    Reference ref = ReferenceFactory.newArticle();
                    //type
                    if (!"A".equals(type)) {
                        logger.warn(refId + ": Type not 'A'");
                    }

                    //author
                    handleAuthorStr(state, authorStr, ref, refId);

                    //year
                    handleYearStr(state, yearStr, ref, refId);

                    //articleTitle
                    handleTitleStr(state, journalTitleStr, ref, refId);

                    //journalTitle
                    if (isNotBlank(journalTitleStr)) {
                        //TODO dedup
                        IJournal journal = ReferenceFactory.newJournal();
                        journal.setTitle(journalTitleStr);
                        ref.setInJournal(journal);
                    }else {
                        logger.warn(refId + ": No journal title");
                    }

                    //concat
                    if (isNotBlank(concat)) {
                        //TODO split
                        String[] split = concat.split(":");
                        String volume = split[0];
                        if (split.length > 1) {
                            //TODO
                            String detail = split[1];
                        }
                        ref.setVolume(volume);
                    }else {
                        logger.info(refId + ": No volume");
                    }

                    //url
                    handleUrlStr(state, urlStr, ref, refId);

                    //doi
                    handleDoiStr(state, doiStr, ref, refId);

                    //issn
                    if (isNotBlank(issnStr) && !"NA".equals(issnStr)) {
                        if (issnStr.startsWith("ISSN:")) {
                            issnStr = issnStr.replace("ISSN:", "").trim();
                        }else if (issnStr.startsWith("ISSN ")) {
                            issnStr = issnStr.replace("ISSN ", "").trim();
                        }
                        ref.getInJournal().setIssn(issnStr);
                    }

					partitioner.startDoSave();
					refsToSave.add(ref);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when creating reference with id " + refId + ". Reference could not be saved.");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getReferenceService().save(refsToSave);
		return success;
	}

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
//				handleForeignKey(rs, nameIdSet, "PTNameFk");
//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

}