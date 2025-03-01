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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
@Component
public class FaunaEuropaeaRefImport extends FaunaEuropaeaImportBase {

    private static final long serialVersionUID = -586555645981648177L;
    private static Logger logger = LogManager.getLogger();

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		boolean result = true;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		logger.warn("Checking for References not yet fully implemented");
//		result &= checkReferenceStatus(fauEuConfig);

		return result;
	}

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {
		/*
		logger.warn("Start RefImport doInvoke");
		ProfilerController.memorySnapshot();
		*/
		if (state.getConfig().getDoReferences().equals(DO_REFERENCES.NONE)){
			return;
		}
		if (state.getConfig().getSourceReference().getId() == 0){
		    Reference sourceRef = getReferenceService().find(state.getConfig().getSourceRefUuid());
		    state.getConfig().setSourceReference(sourceRef);
		}
		Set<UUID> taxonUuids = null;
		Map<Integer, Reference> references = null;
		Map<String,TeamOrPersonBase<?>> authors = null;
		Map<UUID, FaunaEuropaeaReferenceTaxon> fauEuTaxonMap = null;
		Map<Integer, UUID> referenceUuids = new HashMap<Integer, UUID>();
		Set<Integer> referenceIDs = null;
		int limit = state.getConfig().getLimitSave();

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

		String namespace = "Reference";
		int i = 0;

		String selectCountTaxRefs =
			" SELECT count(*) ";

		String selectColumnsTaxRefs =
			" SELECT Reference.*, TaxRefs.*, Taxon.UUID  ";

		String fromClauseTaxRefs =
			" FROM TaxRefs " +
			" INNER JOIN Reference ON Reference.ref_id = TaxRefs.trf_ref_id " +
			" INNER JOIN Taxon ON TaxRefs.trf_tax_id = Taxon.TAX_ID ";

		String orderClauseTaxRefs =
			" ORDER BY TaxRefs.trf_tax_id";

		String selectCountRefs =
			" SELECT count(*) FROM Reference";

		String selectColumnsRefs =
			" SELECT * FROM Reference order by ref_author";


		String countQueryTaxRefs =
			selectCountTaxRefs + fromClauseTaxRefs;

		String selectQueryTaxRefs =
			selectColumnsTaxRefs + fromClauseTaxRefs + orderClauseTaxRefs;

		String countQueryRefs =
			selectCountRefs;

		String selectQueryRefs =
			selectColumnsRefs;


		if(logger.isInfoEnabled()) { logger.info("Start making References..."); }
		//first add all References to CDM
		processReferences(state, references, authors,
				referenceUuids, limit, fauEuConfig, source, namespace, i,
				countQueryRefs, selectQueryRefs);

	    /*
		logger.warn("Start ref taxon relationships");
		ProfilerController.memorySnapshot();
	 	*/
	 //create the relationships between references and taxa

        createTaxonReferenceRel(state, taxonUuids, fauEuTaxonMap,
				referenceUuids, referenceIDs, limit, source,
				countQueryTaxRefs, selectQueryTaxRefs);

        /*
		logger.warn("End RefImport doInvoke");
		ProfilerController.memorySnapshot();
		*/
		if(logger.isInfoEnabled()) { logger.info("End making references ..."); }

		return;
	}

	private void processReferences(FaunaEuropaeaImportState state,
			Map<Integer, Reference> references,
			Map<String, TeamOrPersonBase<?>> authors,
			Map<Integer, UUID> referenceUuids, int limit,
			FaunaEuropaeaImportConfigurator fauEuConfig, Source source,
			String namespace, int i, String countQueryRefs,
			String selectQueryRefs) {
		TransactionStatus txStatus = null;
		int count;
		Map<String, Reference> inReferences = new HashMap<String, Reference>();
		try {
			ResultSet rsRefs = source.getResultSet(countQueryRefs);
			rsRefs.next();
			count = rsRefs.getInt(1);

			rsRefs = source.getResultSet(selectQueryRefs);

	        if (logger.isInfoEnabled()) {
	        	logger.info("Get all References...");
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + countQueryRefs);
				logger.info("Select Query: " + selectQueryRefs);
			}

	        while (rsRefs.next()){
	        	int refId = rsRefs.getInt("ref_id");
	        	String var = "\u00A7";
				String refAuthor = deleteSymbol(var,rsRefs.getString("ref_author"));

				String year = deleteSymbol(var, rsRefs.getString("ref_year"));
				String title = deleteSymbol(var, rsRefs.getString("ref_title"));

				if (year == null){
					try{
						year = String.valueOf((Integer.parseInt(title)));
					}
					catch(Exception ex)
					{
						logger.info("year is empty and " +title + " contains no integer");
				    }
				}
				String refSource = deleteSymbol(var, rsRefs.getString("ref_source"));

				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					references = new HashMap<Integer,Reference>(limit);
					authors = new HashMap<String,TeamOrPersonBase<?>>(limit);
					//inReferences = new HashMap<String, Reference>(limit);
					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Reference import transaction started");
					}
				}

				Reference reference = null;
				TeamOrPersonBase<?> author = null;
				//ReferenceFactory refFactory = ReferenceFactory.newInstance();
				reference = ReferenceFactory.newGeneric();

//				reference.setTitleCache(title);
				reference.setTitle(title);
				reference.setDatePublished(ImportHelper.getDatePublished(year));
				Reference inReference;
				Reference tempInReference;
				if (!StringUtils.isBlank(refSource)) {
				    tempInReference = NonViralNameParserImpl.NewInstance().parseReferenceTitle(refSource, null, false);
				    if (inReferences.containsKey(tempInReference.getTitleCache())){
				        inReference = inReferences.get(tempInReference.getTitleCache());

				    }else{
				        inReference = tempInReference.clone();
				        inReference.setPages(null);
				        inReference.setEdition(null);
				        inReferences.put(inReference.getTitleCache(), inReference);

				    }
				    reference.setPages(tempInReference.getPages());
                    reference.setEdition(tempInReference.getEdition());
                    tempInReference = null;
                    reference.setInReference(inReference);
				}

				if (!authors.containsKey(refAuthor)) {
					if (refAuthor == null) {
						logger.warn("Reference author is null");
					}
					NonViralNameParserImpl parser = new NonViralNameParserImpl();
					//author = parser.author(refAuthor);
					author = FaunaEuropaeaAuthorImport.parseNomAuthorString(refAuthor);
					authors.put(refAuthor,author);
					if (logger.isTraceEnabled()) {
						logger.trace("Stored author (" + refAuthor + ")");
					}
				//}

				} else {
					author = authors.get(refAuthor);
					if (logger.isDebugEnabled()) {
						logger.debug("Not imported author with duplicated aut_id (" + refId +
							") " + refAuthor);
					}
				}

				reference.setAuthorship(author);

				ImportHelper.setOriginalSource(reference, fauEuConfig.getSourceReference(), refId, namespace);
				ImportHelper.setOriginalSource(author, fauEuConfig.getSourceReference(), refId, namespace);

				// Store reference


				if (!references.containsKey(refId) && !(reference.getId() ==fauEuConfig.getSourceReference().getId())) {

					if (reference == null) {
						logger.warn("Reference is null");
					}
					references.put(refId, reference);
					if (logger.isTraceEnabled()) {
						logger.trace("Stored reference (" + refAuthor + ")");
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Duplicated reference (" + refId + ", " + refAuthor + ")");
					}
					//continue;
				}

				if (((i % limit) == 0 && i > 1 ) || i == count ) {

					commitReferences(references, authors, referenceUuids, i,
							txStatus);
					references= null;
					authors = null;
				}



	        }
	        if (references != null){
	        	commitReferences(references, authors, referenceUuids, i, txStatus);
	        	references= null;
				authors = null;
	        }
		}catch(SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
		inReferences = null;

	}

	private void commitReferences(Map<Integer, Reference> references,
			Map<String, TeamOrPersonBase<?>> authors,
			Map<Integer, UUID> referenceUuids, int i, TransactionStatus txStatus) {

		Map <UUID, Reference> referenceMap =getReferenceService().saveOrUpdate(references.values());
		logger.info("i = " + i + " - references saved");

		Iterator<Entry<UUID, Reference>> it = referenceMap.entrySet().iterator();
		while (it.hasNext()){
			Reference ref = it.next().getValue();
			int refID = Integer.valueOf((ref.getSources().iterator().next()).getIdInSource());
			UUID uuid = ref.getUuid();
			referenceUuids.put(refID, uuid);
		}

		getAgentService().save(authors.values());
		commitTransaction(txStatus);
	}

	private void createTaxonReferenceRel(FaunaEuropaeaImportState state,
			Set<UUID> taxonUuids,
			Map<UUID, FaunaEuropaeaReferenceTaxon> fauEuTaxonMap,
			Map<Integer, UUID> referenceUuids, Set<Integer> referenceIDs,
			int limit, Source source, String countQueryTaxRefs,
			String selectQueryTaxRefs) {

		TransactionStatus txStatus = null;
		int i;
		int count;
		Taxon taxon = null;
		i = 0;
		try{
			ResultSet rsTaxRefs = source.getResultSet(countQueryTaxRefs);
			rsTaxRefs.next();
			count = rsTaxRefs.getInt(1);

			rsTaxRefs = source.getResultSet(selectQueryTaxRefs);

			logger.info("Start taxon reference-relationships");
			FaunaEuropaeaReference fauEuReference;
			FaunaEuropaeaReferenceTaxon fauEuReferenceTaxon;
			while (rsTaxRefs.next()) {


				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					taxonUuids = new HashSet<UUID>(limit);
					referenceIDs = new HashSet<Integer>(limit);
					fauEuTaxonMap = new HashMap<UUID, FaunaEuropaeaReferenceTaxon>(limit);

					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - Reference import transaction started");
					}
				}


				int taxonId = rsTaxRefs.getInt("trf_tax_id");
				int refId = rsTaxRefs.getInt("ref_id");
				String refAuthor = rsTaxRefs.getString("ref_author");
				String year = rsTaxRefs.getString("ref_year");
				String title = rsTaxRefs.getString("ref_title");

				if (year == null){
					try{
						year = String.valueOf((Integer.parseInt(title)));
					}
					catch(Exception ex)
					{
						logger.info("year is empty and " +title + " contains no integer");
				    }
				}
				String refSource = rsTaxRefs.getString("ref_source");
				String page = rsTaxRefs.getString("trf_page");
				UUID currentTaxonUuid = null;
				if (resultSetHasColumn(rsTaxRefs, "UUID")){
					currentTaxonUuid = UUID.fromString(rsTaxRefs.getString("UUID"));
				} else {
					logger.error("Taxon (" + taxonId + ") without UUID ignored");
					continue;
				}

				fauEuReference = new FaunaEuropaeaReference();
				fauEuReference.setTaxonUuid(currentTaxonUuid);
				fauEuReference.setReferenceId(refId);
				fauEuReference.setReferenceAuthor(refAuthor);
				fauEuReference.setReferenceYear(year);
				fauEuReference.setReferenceTitle(title);
				fauEuReference.setReferenceSource(refSource);
				fauEuReference.setPage(page);

				if (!taxonUuids.contains(currentTaxonUuid)) {
					taxonUuids.add(currentTaxonUuid);
					fauEuReferenceTaxon =
						new FaunaEuropaeaReferenceTaxon(currentTaxonUuid);
					fauEuTaxonMap.put(currentTaxonUuid, fauEuReferenceTaxon);
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("Taxon (" + currentTaxonUuid + ") already stored.");
						//continue; ein Taxon kann mehr als eine Referenz haben
					}
				}

				if (!referenceIDs.contains(refId)) {


					referenceIDs.add(refId);
					if (logger.isTraceEnabled()) {
						logger.trace("Stored reference (" + refAuthor + ")");
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Duplicated reference (" + refId + ", " + refAuthor + ")");
					}
					//continue;
				}

				fauEuTaxonMap.get(currentTaxonUuid).addReference(fauEuReference);

				if (((i % limit) == 0 && i > 1 ) || i == count) {

					try {
						commitTaxaReferenceRel(taxonUuids, fauEuTaxonMap,
								referenceUuids, referenceIDs, limit, txStatus, i,
								taxon);

						taxonUuids = null;

						fauEuTaxonMap = null;
						referenceIDs = null;

					} catch (Exception e) {
						logger.warn("An exception occurred when creating reference, reference could not be saved.");
					}
				}
			}
			if (taxonUuids != null){
				commitTaxaReferenceRel(taxonUuids, fauEuTaxonMap,
						referenceUuids, referenceIDs, limit, txStatus, i,
						taxon);
			}
			rsTaxRefs.close();
		} catch (SQLException e) {
				logger.error("SQLException:" +  e);
				state.setUnsuccessfull();
		}
		taxonUuids = null;

		fauEuTaxonMap = null;
		referenceIDs = null;

	}

	private void commitTaxaReferenceRel(Set<UUID> taxonUuids,
			Map<UUID, FaunaEuropaeaReferenceTaxon> fauEuTaxonMap,
			Map<Integer, UUID> referenceUuids, Set<Integer> referenceIDs,
			int limit, TransactionStatus txStatus, int i, Taxon taxon) {
		List<TaxonBase> taxonList;
		List<Reference> referenceList;
		Map<Integer, Reference> references;
		taxonList = getTaxonService().find(taxonUuids);
		//get UUIDs of used references
		Iterator<?> itRefs = referenceIDs.iterator();
		Set<UUID> uuidSet = new HashSet<UUID>(referenceIDs.size());
		UUID uuid;
		while (itRefs.hasNext()){
			uuid = referenceUuids.get(itRefs.next());
			uuidSet.add(uuid);
		}

		referenceList = getReferenceService().find(uuidSet);

		references = new HashMap<Integer, Reference>(limit);
		for (Reference ref : referenceList){
			references.put(Integer.valueOf(ref.getSources().iterator().next().getIdInSource()), ref);
		}
		for (TaxonBase<?> taxonBase : taxonList) {

			// Create descriptions

			if (taxonBase == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("TaxonBase is null ");
				}
				continue;
			}
			boolean isSynonym = taxonBase.isInstanceOf(Synonym.class);
			if (isSynonym) {
				Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
				taxon = syn.getAcceptedTaxon();
			} else {
				taxon = CdmBase.deproxy(taxonBase, Taxon.class);
			}

			if (taxon != null) {
				TaxonDescription taxonDescription;
				Set<TaxonDescription> descriptions = taxon.getDescriptions();
				if (descriptions.size() > 0) {
					taxonDescription = descriptions.iterator().next();
				} else {
					taxonDescription = TaxonDescription.NewInstance();
					taxon.addDescription(taxonDescription);
				}


				UUID taxonUuid = taxonBase.getUuid();
				FaunaEuropaeaReferenceTaxon fauEuHelperTaxon = fauEuTaxonMap.get(taxonUuid);
				Reference citation;
				String microCitation;
				DescriptionElementSource originalSource;
				Synonym syn;
				for (FaunaEuropaeaReference storedReference : fauEuHelperTaxon.getReferences()) {

					TextData textData = TextData.NewInstance(Feature.CITATION());

					citation = references.get(storedReference.getReferenceId());
					microCitation = null;
					originalSource = DescriptionElementSource.NewInstance(OriginalSourceType.PrimaryTaxonomicSource, null, null, citation, microCitation, null, null);
					if (isSynonym){
						syn = CdmBase.deproxy(taxonBase, Synonym.class);
						originalSource.setNameUsedInSource(syn.getName());
					}
					textData.addSource(originalSource);
					taxonDescription.addElement(textData);
				}
			}
		}
		if(logger.isInfoEnabled()) {
			logger.info("i = " + i + " - Transaction committed");
		}

		// save taxa
		getTaxonService().save(taxonList);
		commitTransaction(txStatus);

	}

	@Override
	protected boolean isIgnore(FaunaEuropaeaImportState state){
		return (state.getConfig().getDoReferences() == IImportConfigurator.DO_REFERENCES.NONE);
	}

	private String deleteSymbol(String symbol, String stringVar){
		if (stringVar.startsWith(symbol)){
			if (stringVar.endsWith(symbol)){
				stringVar = stringVar.substring(1,stringVar.length()-1);
			}else{
				stringVar = stringVar.substring(1);
			}
		} else if (stringVar.endsWith(symbol)){
			stringVar = stringVar.substring(0, stringVar.length()-1);
		}
		return stringVar;
	}
}