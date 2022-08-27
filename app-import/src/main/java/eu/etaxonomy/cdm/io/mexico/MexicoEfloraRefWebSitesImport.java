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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraRefWebSitesImport extends MexicoEfloraReferenceImportBase {

    private static final long serialVersionUID = -1186364983750790695L;
    private static final Logger logger = LogManager.getLogger();

	private static final String pluralString = "Websites";
	private static final String dbTableName = "RefWebSites";

	public MexicoEfloraRefWebSitesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;
		Set<Reference> refsToSave = new HashSet<>();

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
			    String type = rs.getString("PubType");
				int refId = rs.getInt("CONABIO-BIB-ID");
				String authorStr = rs.getString("Author");
				String yearStr = rs.getString("Year");
				String titleStr = rs.getString("Title");
                String urlStr = rs.getString("URL");
                String issnStr = rs.getString("ISBN");

                try {
                    Reference ref = ReferenceFactory.newBook();
                    //type
                    if (!"B".equals(type)) {
                        logger.warn(refId + ": Type not 'B'");
                    }

                    //author
                    handleAuthorStr(state, authorStr, ref, refId);

                    //year
                    handleYearStr(state, yearStr, ref, refId);

                    //title
                    handleTitleStr(state, titleStr, ref, refId);

                    //url
                    if (isNotBlank(urlStr)) {
                        URI uri;
                        try {
                            uri = URI.fromString(urlStr);
                            ref.setUri(uri);
                        } catch (Exception e) {
                            logger.warn(refId + ": parse exception for " + urlStr);
                        }
                    }else {
                        //do not report anymore, is in doc file already
                        logger.info(refId + ": No uri");
                    }

                    //issn
                    if (isNotBlank(issnStr) && !"NA".equals(issnStr)) {
                        if (issnStr.startsWith("ISSN ")) {
                            issnStr = issnStr.replace("ISSN ", "").trim();
                        }
                        ref.setIssn(issnStr);
                    }

                    //register id and make import source
                    handleId(state, refId, ref);

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

		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
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