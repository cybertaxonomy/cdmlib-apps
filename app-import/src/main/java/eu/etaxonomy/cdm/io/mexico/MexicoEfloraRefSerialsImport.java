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

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraRefSerialsImport extends MexicoEfloraReferenceImportBase {

    private static final long serialVersionUID = -1186364983750790695L;
    private static final Logger logger = LogManager.getLogger();

	public static final String NAMESPACE = "Serials";

	private static final String pluralString = "Serials";
	private static final String dbTableName = "RefSerials";

	public MexicoEfloraRefSerialsImport(){
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
				String volumeStr = rs.getString("Volume");
                String pagesStr = rs.getString("Pages");
                //exported as Excel table
//                String observacionesStr = rs.getString("Observaciones");
                String urlStr = rs.getString("URL");
                String doiStr = rs.getString("DOI");
                String isbnStr = rs.getString("ISBN");

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

                    //pages
                    if (isNotBlank(pagesStr)) {
                        ref.setPages(pagesStr);
                    }

                    //concat
                    if (isNotBlank(volumeStr)) {
                        ref.setVolume(volumeStr);
                    }else {
                        logger.info(refId + ": No volume");
                    }

                    //url
                    handleUrlStr(state, urlStr, ref, refId);

                    //doi
                    handleDoiStr(state, doiStr, ref, refId);

                    //issn
                    if (isNotBlank(isbnStr) && !"NA".equals(isbnStr)) {
                        if (isbnStr.startsWith("ISBN:")) {
                            isbnStr = isbnStr.replace("ISBN:", "").trim();
                        }else if (isbnStr.startsWith("ISSN ")) {
                            isbnStr = isbnStr.replace("ISSN ", "").trim();
                        }
                        ref.setIsbn(isbnStr);
                    }

                    //register id and make import source
                    handleId(state, refId, ref);

					partitioner.startDoSave();
					refsToSave.add(ref);
				} catch (Exception e) {
					e.printStackTrace();
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