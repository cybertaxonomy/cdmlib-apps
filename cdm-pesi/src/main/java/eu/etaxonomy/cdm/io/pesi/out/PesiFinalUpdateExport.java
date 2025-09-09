/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * Final updates/cleanups for PESI DW export.
 * @author muellera
 */
@Component
public class PesiFinalUpdateExport extends PesiExportBase {

    private static final long serialVersionUID = 6190569804410237104L;
    private static Logger logger = LogManager.getLogger();

    private static final Class<? extends CdmBase> standardMethodParameter = TaxonBase.class;

	private static final String pluralString = "taxa";

	public PesiFinalUpdateExport() {
		super();
	}

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}

	@Override
	protected boolean doCheck(PesiExportState state) {
		boolean result = true;
		return result;
	}

	@Override
	protected void doInvoke(PesiExportState state) {
		try {
			logger.info("*** Started Making " + pluralString + " ...");

			// Stores whether this invoke was successful or not.
			boolean success = true;

			//updates to TaxonStatus and others
			success &= doPhaseUpdates(state);

			logger.info("*** Finished Making " + pluralString + " ..." + getSuccessString(success));

			if (!success){
				state.getResult().addError("An unknown error occurred in PesiFinalUpdateExport.invoke");
			}
			return;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.getResult().addException(e, e.getMessage());
			return;
		}
	}

	private boolean doPhaseUpdates(PesiExportState state) {

		String oldStatusFilter = " 7 ";  //"= '" + PesiTransformer.T_STATUS_STR_UNACCEPTED + "' ";
		String emStr = PesiTransformer.SOURCE_STR_EM;
		String feStr = PesiTransformer.SOURCE_STR_FE;
		String ifStr = PesiTransformer.SOURCE_STR_IF;

		//NOT ACCEPTED names
		String updateNotAccepted = " UPDATE Taxon SET TaxonStatusFk = %d, TaxonStatusCache = '%s' " +
				" WHERE OriginalDB = '%s' AND taxonstatusfk = 1 AND ParentTaxonFk = %s AND RankFk > 180 ";
		updateNotAccepted = String.format(updateNotAccepted, 8, "NOT ACCEPTED: TAXONOMICALLY VALUELESS LOCAL OR SINGULAR BIOTYPE", emStr, oldStatusFilter);
		int updated = state.getConfig().getDestination().update(updateNotAccepted);
		logger.debug(updated + " taxonomically valueless updated");

		//alternative names
		String updateAlternativeName = "UPDATE Taxon SET TaxonStatusFk = 1, TaxonStatusCache = 'accepted' " +
				" FROM RelTaxon RIGHT OUTER JOIN Taxon ON RelTaxon.TaxonFk1 = Taxon.TaxonId " +
				" WHERE (RelTaxon.RelTaxonQualifierFk = 17) AND (Taxon.TaxonStatusFk = %s) ";
		updateAlternativeName = String.format(updateAlternativeName, oldStatusFilter);
//		System.out.println(updateAlternativeName);
		updated = state.getConfig().getDestination().update(updateAlternativeName);
		logger.debug(updated + " alternative names status updated");


		String updateSynonyms = " UPDATE Taxon SET TaxonStatusFk = 2, TaxonStatusCache = 'synonym' " +
					" FROM RelTaxon RIGHT OUTER JOIN Taxon ON RelTaxon.TaxonFk1 = Taxon.TaxonId " +
					" WHERE (RelTaxon.RelTaxonQualifierFk in (1, 3)) AND (Taxon.TaxonStatusFk = %s)";
		updateSynonyms = String.format(updateSynonyms, oldStatusFilter);
		//System.out.println(updateSynonyms);
		updated = state.getConfig().getDestination().update(updateSynonyms);
		logger.debug(updated + " synonym status updated");


		// cache citation  - check if this can't be done in getCacheCitation
		// cache citation - FE
//		String updateCacheCitationFE = " UPDATE Taxon " +
//				" SET CacheCitation = IsNull(SpeciesExpertName + '. ', '') + WebShowName + '. Accessed through: Fauna Europaea at http://www.faunaeur.org/full_results.php?id=' + cast(TempFE_Id as varchar) " +
//				" WHERE OriginalDb = '%s'";
//		updateCacheCitationFE = String.format(updateCacheCitationFE, feStr);
//		updated = state.getConfig().getDestination().update(updateCacheCitationFE);

		// cache citation - EM
		String updateCacheCitationEM = " UPDATE Taxon " +
				" SET CacheCitation = SpeciesExpertName + ' ' + WebShowName + '. Accessed through: Euro+Med PlantBase at "
				       + "" + state.getConfig().getEuromedBaseUrl()+ "' + GUID " +
				" WHERE OriginalDb = '%s'";
		updateCacheCitationEM = String.format(updateCacheCitationEM, emStr);
		updated = state.getConfig().getDestination().update(updateCacheCitationEM);
		logger.debug(updated + " E+M cache citations updated");

		// cache citation - IF
//		String updateCacheCitationIF = " UPDATE Taxon " +
//				" SET CacheCitation = IsNull(SpeciesExpertName + ' ', '') + WebShowName + '. Accessed through: Index Fungorum at http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=' + cast(TempIF_Id as varchar) " +
//				" WHERE OriginalDb = '%s'";
//		updateCacheCitationIF = String.format(updateCacheCitationIF, ifStr);
//		updated = state.getConfig().getDestination().update(updateCacheCitationIF);

		return true;
	}

	@Override
	protected boolean isIgnore(PesiExportState state) {
		return ! state.getConfig().isDoTaxa();
	}
}