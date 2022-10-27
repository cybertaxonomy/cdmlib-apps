package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

@Component
public class FaunaEuropaeaVernacularNamesImport extends FaunaEuropaeaImportBase {

    private static final long serialVersionUID = 168771351441040059L;
    private static Logger logger = LogManager.getLogger();

	private HashMap<String, Reference> sourceMap = new HashMap<>();
	private Reference pesiProject = ReferenceFactory.newDatabase();

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {
		int limit = state.getConfig().getLimitSave();
		pesiProject.setTitle("PESI");

		if (state.getConfig().isDoVernacularNames()){
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
				" SELECT vernacularNames.*, Taxon.UUID ";

			String fromClause =
				" FROM vernacularNames INNER JOIN " +
	            " Taxon ON vernacularnames.UUID = Taxon.UUID ";
			String orderBy = " ORDER BY vernacularNames.UUID";

			String countQuery =
				selectCount + fromClause;

			String selectQuery =
				selectColumns + fromClause + orderBy;



			if(logger.isInfoEnabled()) { logger.info("Start making vernacular names..."); }

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
							logger.info("i = " + i + " - vernacular names import transaction started");
						}
					}

					//taxonId = rs.getInt("dis_tax_id");
					String vernameGUID = rs.getString("GUID");
					String vernacularNameString = rs.getString("verName");
					//int taxonFk = rs.getInt("taxonFK");
					int languageFK= rs.getInt("languageFK");
					String languageCache = rs.getString("languageCache");
					String taxonUUID= rs.getString("UUID");
					String verSource = rs.getString("VerSource");
					UUID currentTaxonUuid = null;


					FaunaEuropaeaVernacularName fauEuVernacularName = new FaunaEuropaeaVernacularName();
					fauEuVernacularName.setGuid(vernameGUID);
					fauEuVernacularName.setLanguageCache(languageCache);
					fauEuVernacularName.setLanguageFk(languageFK);
					fauEuVernacularName.setVernacularName(vernacularNameString);
					fauEuVernacularName.setSource(verSource);
					fauEuVernacularName.setTaxonUuid(UUID.fromString(taxonUUID));
					currentTaxonUuid =UUID.fromString(taxonUUID);
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
						logger.error("Commit of taxa and vernacular names failed");
						logger.error(e.getMessage());
						e.printStackTrace();
					}
				}
				rs = null;
			} catch (SQLException e) {
				logger.error("SQLException:" +  e);
				state.setUnsuccessfull();
			}

			if(logger.isInfoEnabled()) { logger.info("End making vernacular names..."); }

			return;
		}

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

		getTaxonService().saveOrUpdate(taxonList);
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
					Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
					taxon = syn.getAcceptedTaxon();
					logger.warn("TaxonBase (" + taxonBase.getId() + " is not of type Taxon but: "
							+ taxonBase.getClass().getSimpleName() + " using accepted Taxon for vernacular name");
					continue;
				}
				taxonDescription = TaxonDescription.NewInstance();
				addOriginalSource(taxonDescription, null, "CommonNameDefaultImport", pesiProject);

				taxon.addDescription(taxonDescription);

				taxonUuid = taxonBase.getUuid();
				fauEuHelperTaxon= fauEuTaxonMap.get(taxonUuid);

				for (FaunaEuropaeaVernacularName fauEuHelperVernacularName: fauEuHelperTaxon.getVernacularNames()) {

					verName = null;

					/*
					 * UUID uuid = state.getTransformer().getLanguageUuid(languageKey);
					result = (Language)getTermService().find(uuid);
				}
				if (result == null){
					result = state.getTransformer().getLanguageByKey(languageKey);
					if (result == null){
						UUID uuidLanguage;
						uuidLanguage = state.getTransformer().getLanguageUuid(languageKey);
						if (uuidLanguage == null){
							logger.warn("Language not defined: " + languageKey)  ;
						}
						result = getLanguage(state, uuidLanguage, languageKey, languageKey, null);
						if (result == null){
							logger.warn("Language not defined: " + languageKey)  ;
						}
					}else if (result.getId() == 0){
//						UUID uuidLanguageVoc = UUID.fromString("45ac7043-7f5e-4f37-92f2-3874aaaef2de");
						UUID uuidLanguageVoc = UUID.fromString("434cea89-9052-4567-b2db-ff77f42e9084");
						TermVocabulary<Language> voc = getVocabulary(TermType.Language, uuidLanguageVoc, "User Defined Languages", "User Defined Languages", null, null, false, result);
//						TermVocabulary<Language> voc = getVocabularyService().find(uuidLanguageVoc);
						voc.addTerm(result);
						getTermService().saveOrUpdate(result);
						state.putLanguage(result);
					}
					 *
					 */
					Language lang = FaunaEuropaeaTransformer.langFK2Language(fauEuHelperVernacularName.getLanguageFk());
					if (lang == null && fauEuHelperVernacularName.getLanguageFk() == 83){
						lang = Language.ENGLISH();
						fauEuHelperVernacularName.setArea("USA");
					}

					if (lang == null){
						UUID uuidLanguageVoc = UUID.fromString("434cea89-9052-4567-b2db-ff77f42e9084");
						logger.info("languageFk = " + fauEuHelperVernacularName.getLanguageFk());
						TermVocabulary<Language> voc = getVocabulary(state, TermType.Language, uuidLanguageVoc, "User Defined Languages", "User Defined Languages", null, null, false, lang);
						lang = Language.NewInstance("Dummy", "", "");
						voc.addTerm(lang);
						lang =getTermService().save(lang);
						FaunaEuropaeaTransformer.languageFK2Language.put(fauEuHelperVernacularName.getLanguageFk(), lang);
					}
//					if (lang.getId() == 0){
//						UUID uuidLanguageVoc = UUID.fromString("434cea89-9052-4567-b2db-ff77f42e9084");
//						TermVocabulary<Language> voc = getVocabulary(TermType.Language, uuidLanguageVoc, "User Defined Languages", "User Defined Languages", null, null, false, lang);
//						voc.addTerm(lang);
//						lang =(Language)getTermService().save(lang);
//						//FaunaEuropaeaTransformer.languageFK2Language.put(fauEuHelperVernacularName.getLanguageFk(), lang);
//					}

					verName = CommonTaxonName.NewInstance(fauEuHelperVernacularName.getVernacularName(), lang);
					if (fauEuHelperVernacularName.getArea()!= null && fauEuHelperVernacularName.getArea().equals("USA")){
						verName.setArea(NamedArea.NORTH_AMERICA());
					}
					verName.setCreated(null);
					if (fauEuHelperVernacularName.getSource() != null){
					    addOriginalSource(verName, null, null, sourceMap.get(fauEuHelperVernacularName.getSource()));
					}
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
		String sourceSelect = "SELECT DISTINCT vernacularNames.VerSource FROM vernacularNames group by vernacularNames.VerSource";
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
			Reference ref = it.next().getValue();
			String refID = ref.getTitle();
			sourceMap.put(refID, ref);
		}

		commitTransaction(txStatus);
	}
}
