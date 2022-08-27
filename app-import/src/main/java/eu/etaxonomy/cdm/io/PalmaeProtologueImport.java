/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.app.images.AbstractImageImporter;
import eu.etaxonomy.cdm.app.images.ImageImportState;
import eu.etaxonomy.cdm.common.ExcelUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.model.media.ExternalLinkType;
import eu.etaxonomy.cdm.model.name.TaxonName;

/**
 * @author n.hoffmann
 * @since 19.11.2008
 */
@Component
public class PalmaeProtologueImport extends AbstractImageImporter {

    private static final long serialVersionUID = -7178567387220714414L;
    private static final Logger logger = LogManager.getLogger();

	public static final String SPECIES = "Species";
	public static final String TAXONID = "Taxon ID";
	public static final String LINK_PROTO = "Link proto";

	@Override
	protected void invokeImageImport(ImageImportState state) {

		List<Map<String, String>> contents;
		try {
			contents = ExcelUtils.parseXLS(state.getConfig().getSource());
		} catch (/*FileNotFound*/Exception e) {
			logger.error("FileNotFound: " + state.getConfig().getSource().toString());
			state.setUnsuccessfull();
			return;
		}

		Set<TaxonName> taxonNameStore = new HashSet<>();

		int count = 0;

		for (Map<String, String> row : contents){
			count++;

			TaxonName taxonName = null;
			String species = null;
			String taxonId = null;
			String linkProto = null;
			try{
				species = row.get(PalmaeProtologueImport.SPECIES).trim();
				taxonId = row.get(PalmaeProtologueImport.TAXONID);
				linkProto= row.get(PalmaeProtologueImport.LINK_PROTO).trim();
				taxonName = getCommonService().getSourcedObjectByIdInSource(TaxonName.class, "palm_tn_" + taxonId.replace(".0", ""), "TaxonName");
			}catch (Exception e){
				logger.error("The row has errors: rowNumber: " +count + ", content: "  + row, e);
			}

			if(taxonName == null){
				logger.warn("no taxon with this name found: " + species + ", idInSource: " + taxonId);
			}else{
				try {
					URI uri = new URI(linkProto);
					taxonName.addProtologue(uri, null, ExternalLinkType.WebSite);
				} catch (URISyntaxException e) {
					String message= "URISyntaxException when trying to convert: " + linkProto;
					logger.error(message);
					e.printStackTrace();
				}

				taxonNameStore.add(taxonName);
				if(count % 50 == 0){
					logger.info(count + " protologues processed.");
				}
			}
		}

		getNameService().save(taxonNameStore);
		logger.info(count + " protologues imported to CDM store.");

		return;
	}
}