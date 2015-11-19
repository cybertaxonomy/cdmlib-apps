package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

public class FaunaEuropaeaVernacularNamesImport extends FaunaEuropaeaImportBase {
	private static final Logger logger = Logger.getLogger(FaunaEuropaeaVernacularNamesImport.class);

	private HashMap<String, Reference> sourceMap = new HashMap<String, Reference>();
	
	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {
		int limit = state.getConfig().getLimitSave();
		
		/* Taxon store for retrieving taxa from and saving taxa to CDM */
		List<TaxonBase> taxonList = null;
		/* UUID store as input for retrieving taxa from CDM */
		Set<UUID> taxonUuids = null;
		/* Store to hold helper objects */
		Map<UUID, FaunaEuropaeaVernacularNamesTaxon> fauEuVernacularNamesMap = null;


		TransactionStatus txStatus = null;
		
		createSourceMap(state);
		
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

        int i = 0;

		String selectCount =
			" SELECT count(*) ";

		String selectColumns =
			" SELECT vernacularName.*, Taxon.UUID ";

		String fromClause =
			" FROM vernacularName INNER JOIN " +
            " Taxon ON vernacularname.taxon_UUID = Taxon.UUID ";
		String orderBy = " ORDER BY vernacularName.taxon_UUID";

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
					taxonUuids = new HashSet<UUID>(limit);
					fauEuVernacularNamesMap = new HashMap<UUID, FaunaEuropaeaVernacularNamesTaxon>(limit);

					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Distribution import transaction started");
					}
				}

				//taxonId = rs.getInt("dis_tax_id");
				String vernameGUID = rs.getString("GUID");
				String vernacularNameString = rs.getString("verName");
				int taxonFk = rs.getInt("taxonFK");
				int languageFK= rs.getInt("languageFK");
				String languageCache = rs.getString("languageCache");
				String taxonUUID= rs.getString("taxonUUID");
				String verSource = rs.getString("VerSource");
				UUID currentTaxonUuid = null;
				

				FaunaEuropaeaVernacularName fauEuVernacularName = new FaunaEuropaeaVernacularName();
				fauEuVernacularName.setGuid(vernameGUID);
				fauEuVernacularName.setLanguageCache(languageCache);
				fauEuVernacularName.setLanguageFk(languageFK);
				fauEuVernacularName.setVernacularName(vernacularNameString);
				fauEuVernacularName.setSource(verSource);

				if (!taxonUuids.contains(currentTaxonUuid)) {
					taxonUuids.add(currentTaxonUuid);
					FaunaEuropaeaVernacularNamesTaxon fauEuVernacularNameTaxon =
						new FaunaEuropaeaVernacularNamesTaxon(currentTaxonUuid);
					fauEuVernacularNamesMap.put(currentTaxonUuid, fauEuVernacularNameTaxon);
					fauEuVernacularNameTaxon = null;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("Taxon (" + currentTaxonUuid + ") already stored.");
						continue;
					}
				}

				fauEuVernacularNamesMap.get(currentTaxonUuid).addVernacularName(fauEuVernacularName);

				if (((i % limit) == 0 && i != 1 ) || i == count ) {

					try {
						commitTaxaAndVernacularNames(state,  taxonUuids, fauEuVernacularNamesMap, txStatus);
						taxonUuids = null;
						taxonList = null;
						fauEuVernacularNamesMap = null;

					} catch (Exception e) {
						logger.error("Commit of taxa and distributions failed" + e.getMessage());
						e.printStackTrace();
					}

					if(logger.isInfoEnabled()) { logger.info("i = " + i + " - Transaction committed");}
				}


			}
			if (taxonUuids != null){
				try {
					commitTaxaAndVernacularNames(state, taxonUuids, fauEuVernacularNamesMap, txStatus);
					taxonUuids = null;
					taxonList = null;
					fauEuVernacularNamesMap = null;
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

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isIgnore(FaunaEuropaeaImportState state) {
		// TODO Auto-generated method stub
		return false;
	}
	private void commitTaxaAndVernacularNames(
			FaunaEuropaeaImportState state, 
			Set<UUID> taxonUuids,
			Map<UUID, FaunaEuropaeaVernacularNamesTaxon> fauEuTaxonMap,
			TransactionStatus txStatus) throws Exception {
		 List<TaxonBase> taxonList = prepareTaxaAndVernacularNames(getTaxonService().find(taxonUuids), fauEuTaxonMap, state);

		getTaxonService().save(taxonList);
		taxonList = null;
		taxonUuids = null;
		fauEuTaxonMap = null;
		commitTransaction(txStatus);

	}
	
	private List<TaxonBase> prepareTaxaAndVernacularNames(List<TaxonBase> taxonList, Map<UUID, FaunaEuropaeaVernacularNamesTaxon> fauEuTaxonMap,  FaunaEuropaeaImportState state) throws Exception{

		CommonTaxonName verName = null;
		
		
		FaunaEuropaeaVernacularNamesTaxon fauEuHelperTaxon;
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
				}

				taxonUuid = taxonBase.getUuid();
				fauEuHelperTaxon= fauEuTaxonMap.get(taxonUuid);

				for (FaunaEuropaeaVernacularName fauEuHelperVernacularName: fauEuHelperTaxon.getVernacularNames()) {
					
					verName = null;
					Language lang = FaunaEuropaeaTransformer.langFK2Language(fauEuHelperVernacularName.getLanguageFk());
					verName = CommonTaxonName.NewInstance(fauEuHelperVernacularName.getVernacularName(), lang);
					verName.setCreated(null);
					addOriginalSource(verName, null, null, sourceMap.get(fauEuHelperVernacularName.getSource()));
					taxonDescription.addElement(verName);
					
				}
			}
		}
		return taxonList;
	}
	
	private void createSourceMap(FaunaEuropaeaImportState state){
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();
		TransactionStatus txStatus = null;
		String sourceSelect = "SELECT DISTINCT PESI_FaEu_vernaculars_export.VerSource FROM PESI_FaEu_vernaculars_export group by PESI_FaEu_vernaculars_export.VerSource";
		try {
			txStatus =startTransaction();
			ResultSet rs = source.getResultSet(sourceSelect);
			Reference sourceRef;
			String title ;
			Set<Reference> referncesToSave = new HashSet<Reference>();
			while (rs.next()) {
				sourceRef = ReferenceFactory.newGeneric();
				title = rs.getString("VerSource");
				sourceRef.setTitle(title);
				referncesToSave.add(sourceRef);
				
			}
			commitReferences(referncesToSave, txStatus);

		}catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}

		
	}	
	private void commitReferences(Collection<Reference> references,  TransactionStatus txStatus) {
		Map <UUID, Reference> referenceMap =getReferenceService().save(references);
		Iterator<Entry<UUID, Reference>> it = referenceMap.entrySet().iterator();
		while (it.hasNext()){
			Reference<?> ref = it.next().getValue();
			String refID = ref.getTitle();
			sourceMap.put(refID, ref);
		}
		commitTransaction(txStatus);
	}
}
