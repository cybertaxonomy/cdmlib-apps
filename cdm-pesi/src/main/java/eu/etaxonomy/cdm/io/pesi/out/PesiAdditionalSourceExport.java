/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.profiler.ProfilerController;
/**
 * The export class for PESI additional sources stored as taxon sources in the CDM (mostly coming from ERMS)<p>
 * @author a.mueller
 * @since 2019-09-22
 */
@Component
public class PesiAdditionalSourceExport extends PesiExportBase {

    private static final long serialVersionUID = -2567615286288369111L;
    private static Logger logger = LogManager.getLogger();

	private static final Class<? extends CdmBase> standardMethodParameter = IdentifiableSource.class;

	private static int modCount = 1000;
	private static final String dbTableName = "AdditionalTaxonSource";
	private static final String pluralString = "addtional sources";
	private static final String parentPluralString = "Taxa";

	public PesiAdditionalSourceExport() {
		super();
	}

	int countTaxa = 0;
	int countSources = 0;

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}

	@Override
	protected void doInvoke(PesiExportState state) {
		try {
			logger.info("*** Started making " + pluralString + " ...");

			// Stores whether this invoke was successful or not.
			boolean success = true;

			success &= doDelete(state);

			// Get specific mappings: (CDM) Source -> (PESI) Addtional Taxon Source
			PesiExportMapping mapping = getMapping();
			mapping.initialize(state);

			// Taxon Sources
			success &= doPhase01(state, mapping);

			logger.info("*** Finished Making " + pluralString + " ..." + getSuccessString(success));

			if (!success){
				state.getResult().addError("An unknown problem occurred");
			}
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.getResult().addException(e, e.getMessage());
		}
	}

	//PHASE 01: Sources
	private boolean doPhase01(PesiExportState state, PesiExportMapping mapping) {

//	    System.out.println("PHASE 1 of description import");
	    logger.info("PHASE 1...");
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		//int limit = state.getConfig().getLimitSave();
		int limit = 1000;

		@SuppressWarnings("rawtypes")
        List<TaxonBase> taxonList = null;

		TransactionStatus txStatus = startTransaction(true);

		if (logger.isDebugEnabled()){
		    logger.info("Started new transaction. Fetching some " + parentPluralString + " (max: " + limit + ") ...");
		    logger.debug("Start snapshot, before starting loop");
		    ProfilerController.memorySnapshot();
		}

		List<String> propPath = Arrays.asList(new String[]{"sources.*"});
		int partitionCount = 0;
		while ((taxonList = getNextTaxonPartition(TaxonBase.class, limit, partitionCount++, propPath )) != null   ) {

			if (logger.isDebugEnabled()) {
                logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            }

			for (TaxonBase<?> taxon : taxonList) {
				countTaxa++;
				doCount(count++, modCount, parentPluralString);
				state.setCurrentTaxon(taxon);
				if (!taxon.getSources().isEmpty()){
					success &= handleSingleTaxon(taxon, mapping);
				}
			}
			taxonList = null;
			state.setCurrentTaxon(null);

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " " + parentPluralString + ". Total taxa: " + count + ". Total sources " + countSources);
			pastCount = count;
			if (logger.isDebugEnabled()) {
                ProfilerController.memorySnapshot();
            }
			// Start transaction
			txStatus = startTransaction(true);
			if(logger.isDebugEnabled()) {
                logger.info("Started new transaction. Fetching some " + parentPluralString + " (max: " + limit + ") for description import ...");
            }
		}

		// Commit transaction
		commitTransaction(txStatus);
		logger.debug("Committed transaction.");
		return success;
	}

	private boolean handleSingleTaxon(TaxonBase<?> taxon, PesiExportMapping mapping) {

	    boolean success = true;

		Set<IdentifiableSource> sources = new HashSet<>();
		sources.addAll(taxon.getSources());

		for (IdentifiableSource src : sources){
			OriginalSourceType type = src.getType();
			if (type == OriginalSourceType.Other){
			    mapping.invoke(src);
			    countSources++;
			}
		}
		if (logger.isDebugEnabled()) {
            logger.info("number of handled sources " + countSources);
        }
		return success;
	}

	/**
	 * Deletes all entries of database tables related to <code>Note</code>.
	 * @param state The PesiExportState
	 * @return Whether the delete operation was successful or not.
	 */
	protected boolean doDelete(PesiExportState state) {
	    Source destination = state.getConfig().getDestination();

		// Clear NoteSource
		String sql = "DELETE FROM AdditionalTaxonSource";
		destination.update(sql);

		return true;
	}

	/**
	 * Returns the TaxonFk for a given TaxonName or Taxon.
	 * @param state The {@link DbExportStateBase DbExportState}.
	 * @return
	 */
	@SuppressWarnings("unused")  //used by mapper
	private static Integer getTaxonFk(IdentifiableSource source, PesiExportState state) {
		TaxonBase<?> entity = state.getCurrentTaxon();
		return state.getDbId(entity);
	}

	@SuppressWarnings("unused")  //used by mapper
    private static Integer getSourceUseFk(IdentifiableSource source, PesiExportState state) {
        Integer result = state.getTransformer().getSourceUseKeyCacheByCache(source.getOriginalInfo());
        if (result == null){
            logger.error("Source use for " + source.getOriginalInfo() + " does not exist. Please check if all source uses of erms.sourceuses exist exist in PESI.SourceUse" );
        }
        return result;
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getSourceNameCache(IdentifiableSource source) {
        Reference ref = source.getCitation();
        return ref == null? null : ref.getTitleCache();
    }

    @SuppressWarnings("unused")  //used by mapper
    private static Integer getCurrentTaxonFk(PesiExportState state) {
        return state.getDbId(state.getCurrentTaxon());
    }

//******************************* MAPPINGS ********************************************

	/**
	 * Returns the CDM to PESI specific export mappings for PESI additonal sources.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbTableName);

		mapping.addMapper(MethodMapper.NewInstance("TaxonFk", this, IdentifiableSource.class, PesiExportState.class));
        mapping.addMapper(DbObjectMapper.NewInstance("Citation", "SourceFk"));
		mapping.addMapper(MethodMapper.NewInstance("SourceUseFk", this, IdentifiableSource.class, PesiExportState.class));
		mapping.addMapper(DbStringMapper.NewInstance("originalInfo", "SourceUseCache"));
        mapping.addMapper(DbObjectMapper.NewInstance("Citation", "SourceNameCache", IS_CACHE));
		mapping.addMapper(DbStringMapper.NewInstance("citationMicroReference", "SourceDetail"));

		return mapping;
	}

    @Override
    protected boolean doCheck(PesiExportState state) {
        boolean result = true;
        return result;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoAdditionalTaxonSource();
    }
}
