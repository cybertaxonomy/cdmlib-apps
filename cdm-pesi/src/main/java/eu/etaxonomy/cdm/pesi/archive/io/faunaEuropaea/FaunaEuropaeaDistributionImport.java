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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
@Component
public class FaunaEuropaeaDistributionImport extends FaunaEuropaeaImportBase {

    private static final long serialVersionUID = 746146902707885655L;
    private static Logger logger = LogManager.getLogger();

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		boolean result = true;
		logger.warn("Checking for Distributions not yet implemented");
		return result;
	}

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {
		/*
		logger.warn("Start distribution doInvoke");
		ProfilerController.memorySnapshot();
		*/

		if (!state.getConfig().isDoOccurrence()){
			return;
		}

		int limit = state.getConfig().getLimitSave();
		UUID noDataUuid;
		/* Taxon store for retrieving taxa from and saving taxa to CDM */
		List<TaxonBase> taxonList = null;
		/* UUID store as input for retrieving taxa from CDM */
		Set<UUID> taxonUuids = null;
		/* Store to hold helper objects */
		Map<UUID, FaunaEuropaeaDistributionTaxon> fauEuTaxonMap = null;


		TransactionStatus txStatus = null;

		//txStatus = startTransaction();
		/*PresenceAbsenceTerm noDataStatusTerm = PresenceAbsenceTerm.NewPresenceInstance("no data", "no data", "nod");
		noDataUuid = noDataStatusTerm.getUuid();
		TermVocabulary<PresenceAbsenceTerm> voc = getVocabularyService().find(30);
		HibernateProxyHelper.deproxy(voc, OrderedTermVocabulary.class);
		//voc.addTerm(noDataStatusTerm);
	//	getVocabularyService().saveOrUpdate(voc);
		getTermService().save(noDataStatusTerm);*/
		//commitTransaction(txStatus);

	//	FaunaEuropaeaTransformer.setUUIDs(noDataUuid);
		logger.info("create termvoc");
		createTermVocabulary(txStatus, state);

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

        int i = 0;

		String selectCount =
			" SELECT count(*) ";

		String selectColumns =
			" SELECT distribution.*, Area.*, Taxon.UUID ";

		String fromClause =
			" FROM distribution INNER JOIN " +
            " Area ON distribution.dis_ara_id = Area.ara_id INNER JOIN " +
            " Taxon ON distribution.dis_tax_id = Taxon.TAX_ID ";
		String orderBy = " ORDER BY distribution.dis_tax_id";

		String countQuery =
			selectCount + fromClause;

		String selectQuery =
			selectColumns + fromClause + orderBy;

		if(logger.isInfoEnabled()) { logger.info("Start making distributions..."); }

		try {
			ResultSet rs = source.getResultSet(countQuery);
			rs.next();
			int count = rs.getInt(1);

			rs = source.getResultSet(selectQuery);

			if (logger.isInfoEnabled()) {
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQuery);
				logger.info("Select Query: " + selectQuery);
			}

			//int taxonId;

			while (rs.next()) {
				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					taxonUuids = new HashSet<>(limit);
					fauEuTaxonMap = new HashMap<>(limit);

					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Distribution import transaction started");
					}
				}

				//taxonId = rs.getInt("dis_tax_id");
				int disId = rs.getInt("dis_id");
				int occStatusId = rs.getInt("dis_present");
				int areaId = rs.getInt("ara_id");
				String areaName = rs.getString("ara_name");
				String areaCode = rs.getString("ara_code");
				int extraLimital = rs.getInt("ara_extralimital");
				UUID currentTaxonUuid = null;
				if (resultSetHasColumn(rs,"UUID")){
					currentTaxonUuid = UUID.fromString(rs.getString("UUID"));
				} else {
					currentTaxonUuid = UUID.randomUUID();
				}

				FaunaEuropaeaDistribution fauEuDistribution = new FaunaEuropaeaDistribution();
				fauEuDistribution.setDistributionId(disId);
				fauEuDistribution.setOccurrenceStatusId(occStatusId);
				fauEuDistribution.setAreaId(areaId);
				fauEuDistribution.setAreaName(areaName);
				fauEuDistribution.setAreaCode(areaCode);
				fauEuDistribution.setExtraLimital(extraLimital);

				if (!taxonUuids.contains(currentTaxonUuid)) {
					taxonUuids.add(currentTaxonUuid);
					FaunaEuropaeaDistributionTaxon fauEuDistributionTaxon =
						new FaunaEuropaeaDistributionTaxon(currentTaxonUuid);
					fauEuTaxonMap.put(currentTaxonUuid, fauEuDistributionTaxon);
					fauEuDistributionTaxon = null;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("Taxon (" + currentTaxonUuid + ") already stored.");
						continue;
					}
				}

				fauEuTaxonMap.get(currentTaxonUuid).addDistribution(fauEuDistribution);

				if (((i % limit) == 0 && i != 1 ) || i == count ) {

					try {
						commitTaxaAndDistribution(state, taxonUuids, fauEuTaxonMap, txStatus);
						taxonUuids = null;
						taxonList = null;
						fauEuTaxonMap = null;

					} catch (Exception e) {
						logger.error("Commit of taxa and distributions failed" + e.getMessage());
						e.printStackTrace();
					}

					if(logger.isInfoEnabled()) { logger.info("i = " + i + " - Transaction committed");}
				}


			}
			if (taxonUuids != null){
				try {
					commitTaxaAndDistribution(state, taxonUuids, fauEuTaxonMap, txStatus);
					taxonUuids = null;
					fauEuTaxonMap = null;
				} catch (Exception e) {
					logger.error("Commit of taxa and distributions failed");
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}
			rs = null;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}

		if(logger.isInfoEnabled()) { logger.info("End making distributions..."); }

		return;
	}

    private void createTermVocabulary(TransactionStatus txStatus, FaunaEuropaeaImportState state) {
       TermVocabulary<NamedArea> faunaEuAreaVocabulary = TermVocabulary.NewInstance(TermType.NamedArea,
               NamedArea.class, "Areas for Fauna Europaea distribution data", "FE areas", "FE", null);
       faunaEuAreaVocabulary.setUuid(FaunaEuropaeaTransformer.uuidFauEuArea);

       NamedArea area =NamedArea.NewInstance(null, "Andorra", "AD");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("AD");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Albania", "AL");
       area.setIdInVocabulary("AL");
       area.setUuid(UUID.randomUUID());
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Austria", "AT");
       area.setIdInVocabulary("AT");
       area.setUuid(UUID.randomUUID());
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Bosnia and Herzegovina", "BA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("BA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Belgium", "BE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("BE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Bulgaria", "BG");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("BG");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Belarus", "BY");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("BY");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Switzerland", "CH");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("CH");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Cyprus", "CY");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("CY");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Czech Republic", "CZ");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("CZ");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Germany", "DE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("DE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Bornholm I.", "Danish mainland", "DK-DEN");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("DK-DEN");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Faroe Is.", "DK-FOR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("DK-FOR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Estonia", "EE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("EE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Mallorca I., Menorca I., and Pityuses Is.(=Ibiza I. + Formentera I.)", "Balearic Is.", "ES-BAL");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("ES-BAL");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Canary Is.", "ES-CNY");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("ES-CNY");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Alboran I.", "Spanish mainland", "ES-SPA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("ES-SPA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Finland", "FI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("FI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Corsica", "FR-COR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("FR-COR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "French mainland", "FR-FRA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("FR-FRA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Jersey, Guernsey, Alderney", "Channel Is.", "GB-CI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GB-CI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Gibraltar", "GB-GI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GB-GI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Shetlands, Orkneys, Hebrides and Man Is.", "Britain I.", "GB-GRB");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GB-GRB");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Northern Ireland", "GB-NI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GB-NI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Andípsara, Áyios Evstrátios, Foúrnoi,Ikaría, Khíos, "
               + "Lésvos, Límnos, Oinoúsa, Psará, Sámos, Skópelos Kaloyeroi and other smaller"
               + " islands","Vóreion Aiyáion (North Aegean Is.)", "GR-AEG");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GR-AEG");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Amorgós, Anáfi, Ánidros, Ándros, Andíparos, Denoúsa, Folégandros, "
               + "Íos, Iráklia, Káros, Kímolos, Kéa, Kýthnos, Mílos, Mýkonos, Náxos, Páros, Políaigos, Sérifos, "
               + "Sífnos, Síkinos, Sýros, Thíra, Tínos, Yiarós and other smaller islands", "Kikládes (Cyclades Is.)", "GR-CYC");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GR-CYC");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Alimniá, Árkoi, Astipálaia, Avgonísi, Ankathonísi, Farmakonísi, Ioinianísia,"
               + " Kálimnos, Kalolímnos, Kandelioúsa, Kárpathos, Kásos, Khálki, Khamilí, Kínaros, Kos, Léros, Levítha, "
               + "Lipsói, Meyísti, Nísiros, Ofidoúsa, Pátmos, Ródhos, Saría, Sími, Sírina, Tílos, Tría Nisiá, Yialí "
               + "and other smaller islands", "Dodekánisos (Dodecanese Is.) ", "GR-DOD");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GR-DOD");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(" Incl. Andikíthira I., Evvia I., Ionian Is., Samothráki I., Northern Sporades Is., Thásos I.",
               "Greek mainland", "GR-GRC");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GR-GRC");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. small adjacent islands like Gávdhos. Note that Andikíthira I. although being "
               + "closer to Kriti than to mainland, belongs to a mainland province", "Kriti (Crete)", "GR-KRI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("GR-KRI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Croatia", "HR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("HR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Hungary", "HU");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("HU");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Ireland", "IE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("IE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Iceland", "IS");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("IS");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Italian mainland", "IT-ITA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("IT-ITA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Sardinia", "IT-SAR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("IT-SAR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(" Incl. adjacent Italian islands (Lipari Is., Ustica I., Egadi Is., "
               + "Pantelleria I., Pelagie Is.) ", "Sicily", "IT-SI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("IT-SI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Liechtenstein", "LI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("LI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Lithuania", "LT");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("LT");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Luxembourg", "LU");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("LU");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Latvia", "LV");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("LV");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Monaco", "MC");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("MC");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Moldova, Republic of", "MD");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("MD");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Macedonia, the former Yugoslav Republic of", "MK");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("MK");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Malta", "MT");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("MT");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "The Netherlands", "NL");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NL");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Norwegian mainland", "NO-NOR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NO-NOR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Bear I.", "Svalbard & Jan Mayen", "NO-SVA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NO-SVA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Poland", "PL");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("PL");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Azores Is.", "PT-AZO");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("PT-AZO");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Madeira Is.", "PT-MDR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("PT-MDR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Portuguese mainland", "PT-POR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("PT-POR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Selvagens Is.", "PT-SEL");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("PT-SEL");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Romania", "RO");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RO");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Excl. Ushakova I. and Vize I.", "Franz Josef Land", "RU-FJL");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-FJL");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Kaliningrad Region", "RU-KGD");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-KGD");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Novaya Zemlya", "RU-NOZ");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-NOZ");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Central European Russia", "RU-RUC");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-RUC");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "East European Russia", "RU-RUE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-RUE");
       state.putNamedArea(area);

       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "North European Russia", "RU-RUN");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-RUN");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "South European Russia", "RU-RUS");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-RUS");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Northwest European Russia", "RU-RUW");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("RU-RUW");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Gotland I.", "Sweden", "SE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("SE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Slovenia", "SI");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("SI");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Slovakia", "SK");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("SK");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "San Marino", "SM");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("SM");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Imroz I. - Gökçeada, but not those in the Sea of Marmara",
               "European Türkiye", "TR-TUE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("TR-TUE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Ukraine", "UA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("UA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Vatican City", "VA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("VA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Incl. Serbia, Kosovo, Voivodina, Montenegro", "Yugoslavia", "YU");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("YU");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Afro-tropical region", "AFR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("AFR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Australian region", "AUS");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("AUS");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("East of the border line here defined", "East Palaearctic", "EPA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("EPA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Not including Sinai Peninsula", "North Africa", "NAF");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NAF");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Nearctic region", "NEA");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NEA");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Neotropical region", "NEO");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NEO");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance("Asian Türkiye, Caucasian Russian republics, Georgia, Armenia, Azerbaijan, "
               + "Lebanon, Syria, Israel, Jordan, Sinai Peninsula (Egypt), Arabian peninsula, Iran, Iraq",
               "Near East", "NRE");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("NRE");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       area = NamedArea.NewInstance(null, "Oriental region", "ORR");
       area.setUuid(UUID.randomUUID());
       area.setIdInVocabulary("ORR");
       state.putNamedArea(area);
       faunaEuAreaVocabulary.addTerm(area);
       if (txStatus == null){
           txStatus = startTransaction();
       }
       //txStatus = startTransaction();
       faunaEuAreaVocabulary = getVocabularyService().save(faunaEuAreaVocabulary);
       logger.info("save voc");
       commitTransaction(txStatus);
       state.setAreaVoc(faunaEuAreaVocabulary);
    }

    private void commitTaxaAndDistribution(
			FaunaEuropaeaImportState state,
			Set<UUID> taxonUuids,
			Map<UUID, FaunaEuropaeaDistributionTaxon> fauEuTaxonMap,
			TransactionStatus txStatus) throws Exception {

        List<TaxonBase> taxonList = prepareTaxaAndDistribution(getTaxonService().find(taxonUuids), fauEuTaxonMap, state);

		getTaxonService().save(taxonList);
		taxonList = null;
		taxonUuids = null;
		fauEuTaxonMap = null;
		commitTransaction(txStatus);

	}

	private List<TaxonBase> prepareTaxaAndDistribution(List<TaxonBase> taxonList, Map<UUID, FaunaEuropaeaDistributionTaxon> fauEuTaxonMap,  FaunaEuropaeaImportState state) throws Exception{

		Distribution newDistribution = null;
		NamedArea namedArea;
		PresenceAbsenceTerm presenceAbsenceStatus;
		FaunaEuropaeaDistributionTaxon fauEuHelperTaxon;
		UUID taxonUuid;
		TaxonDescription taxonDescription;
		Taxon taxon;
		for (TaxonBase<?> taxonBase : taxonList) {

			if (taxonBase != null) {

				if (taxonBase instanceof Taxon) {
					taxon = CdmBase.deproxy(taxonBase, Taxon.class);
				} else {
					logger.warn("TaxonBase (" + taxonBase.getId() + " is not of type Taxon but: "
							+ taxonBase.getClass().getSimpleName());
					continue;
				}

				Set<TaxonDescription> descriptionSet = taxon.getDescriptions();
				if (descriptionSet.size() > 0) {
					taxonDescription = descriptionSet.iterator().next();
				} else {
					taxonDescription = TaxonDescription.NewInstance();
					taxon.addDescription(taxonDescription);
					taxonDescription.setDefault(true);
					//addOriginalSource(taxonDescription, null, "Default Import", state.getConfig().getSourceReference());
				}

				taxonUuid = taxonBase.getUuid();
				fauEuHelperTaxon= fauEuTaxonMap.get(taxonUuid);

				for (FaunaEuropaeaDistribution fauEuHelperDistribution : fauEuHelperTaxon.getDistributions()) {
					namedArea = null;
					newDistribution = null;
					presenceAbsenceStatus = null;

					presenceAbsenceStatus = FaunaEuropaeaTransformer.occStatus2PresenceAbsence(fauEuHelperDistribution.getOccurrenceStatusId());


					namedArea = state.areaId2NamedArea(fauEuHelperDistribution, state);
					if (namedArea == null){
                        logger.warn("Area " + fauEuHelperDistribution.getAreaCode() + "not found in FE transformer");
                    }
					if (namedArea == null){
						//UUID areaUuid= FaunaEuropaeaTransformer.getUUIDByAreaAbbr(fauEuHelperDistribution.getAreaCode());
						//if (areaUuid == null){
							//logger.warn("Area " + fauEuHelperDistribution.getAreaCode() + "not found in FE transformer");
					//	}
//						namedArea = getNamedArea(state, areaUuid, fauEuHelperDistribution.getAreaName(), fauEuHelperDistribution.getAreaName(), fauEuHelperDistribution.getAreaCode(), null, null, state.getAreaVoc(), TermMatchMode.UUID_ABBREVLABEL);
//						namedArea.setIdInVocabulary(fauEuHelperDistribution.getAreaCode());
//					    state.putNamedArea(namedArea);
					}

					newDistribution = Distribution.NewInstance(namedArea, presenceAbsenceStatus);
					newDistribution.setCreated(null);

					taxonDescription.addElement(newDistribution);

				}
			}
		}

		taxonDescription= null;
		newDistribution = null;
		namedArea= null;
		return taxonList;
	}

    @Override
    protected boolean isIgnore(FaunaEuropaeaImportState state){
		return !state.getConfig().isDoOccurrence();
	}
}
