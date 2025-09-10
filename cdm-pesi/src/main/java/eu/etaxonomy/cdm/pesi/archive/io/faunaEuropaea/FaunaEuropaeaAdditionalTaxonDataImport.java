/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.ITaxonNameBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.babadshanjan
 * @since 21.08.2010
 */
@Component
public class FaunaEuropaeaAdditionalTaxonDataImport extends FaunaEuropaeaImportBase  {

    private static final long serialVersionUID = -6734273038256432559L;
    private static Logger logger = LogManager.getLogger();

//	private static final String parentPluralString = "Synonyms";
	private static final String pluralString = "InfraGenericEpithets";
	//private static final String acceptedTaxonUUID = "A9C24E42-69F5-4681-9399-041E652CF338"; // any accepted taxon uuid, taken from original fauna europaea database

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		boolean result = true;
		logger.warn("Checking for Taxa not yet fully implemented");
		return result;
	}


	/**
	 * Import taxa from FauEU DB
	 */
	@Override
    protected void doInvoke(FaunaEuropaeaImportState state) {

		if(logger.isInfoEnabled()) {
			logger.info("Started creating " + pluralString + "...");
		}

		processAdditionalInfraGenericEpithets(state);

		logger.info("The End is Nigh... " + pluralString + "...");
		return;
	}

	private void processAdditionalInfraGenericEpithets(FaunaEuropaeaImportState state) {
		int pageSize = 1000;
		Set<UUID> uuidSet = new HashSet<UUID>();
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		ICdmDataSource destination = fauEuConfig.getDestination();
		TransactionStatus txStatus = null;
		List<TaxonName> taxonNames = null;
		txStatus = startTransaction(false);

		String selectQuery = "SELECT t.uuid from TaxonNameBase t INNER JOIN " +
				"TaxonNameBase t2 ON t.GenusOrUninomial = t2.GenusOrUninomial AND t.SpecificEpithet = t2.SpecificEpithet " +
				"WHERE t.InfraGenericEpithet IS NULL AND t.rank_id = 764 AND t2.rank_id = 766 AND t2.InfraGenericEpithet IS NOT NULL";

		logger.info("Retrieving TaxonNames...");

		ResultSet resultSet;
		try {
			resultSet = destination.executeQuery(selectQuery);

			// Collect UUIDs
			while (resultSet.next()) {
				uuidSet.add(UUID.fromString(resultSet.getString("UUID")));
			}
		} catch (SQLException e) {
			logger.error("An error occured: ", e);
		}

		// Fetch TaxonName objects for UUIDs
		if (!uuidSet.isEmpty()){
			taxonNames = getNameService().find(uuidSet);

			for (TaxonName taxonName : taxonNames) {

				// Check whether its taxonName has an infraGenericEpithet
				if (taxonName != null) {
					INonViralName targetNonViralName = CdmBase.deproxy(taxonName);
					String infraGenericEpithet = targetNonViralName.getInfraGenericEpithet();
					if (infraGenericEpithet == null) {
						String genusOrUninomial = targetNonViralName.getGenusOrUninomial();
						String specificEpithet = targetNonViralName.getSpecificEpithet();
						List<Taxon> foundTaxa = getTaxonService().listTaxaByName(Taxon.class,
						        genusOrUninomial, "*", specificEpithet,
								"*", "*", null, null, pageSize, 1, null);
						if (foundTaxa.size() == 1) {
							// one matching Taxon found
							TaxonBase<?> taxon = foundTaxa.iterator().next();
							if (taxon != null) {
								ITaxonNameBase name = taxon.getName();
								if (name != null) {
									INonViralName nonViralName = CdmBase.deproxy(name, TaxonName.class);
									infraGenericEpithet = nonViralName.getInfraGenericEpithet();

									// set infraGenericEpithet
	//									targetNonViralName.setInfraGenericEpithet(infraGenericEpithet);
									logger.debug("Added an InfraGenericEpithet to this TaxonName: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
								}
							}
						} else if (foundTaxa.size() > 1) {
							logger.warn("Multiple taxa match search criteria: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
							for (TaxonBase<?> foundTaxon : foundTaxa) {
								logger.warn(foundTaxon.getUuid() + ", " + foundTaxon.getTitleCache());
							}
						} else if (foundTaxa.size() == 0) {
	//							logger.error("No matches for search criteria: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
						}
					}

				}
			}
		}else {
			logger.debug("There are no additional infrageneric epithets!");
		}

		// Commit transaction
		commitTransaction(txStatus);
		logger.info("Committed transaction.");

		return;
	}

	@Override
    protected boolean isIgnore(FaunaEuropaeaImportState state) {
		return ! state.getConfig().isDoTaxa();
	}
}