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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.out.DbExportIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbFixedIntegerMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbFixedStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IdIncMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsLinkImport;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.profiler.ProfilerController;
/**
 * The export class for PESI ecology notes (marine, brackish, fresh, terrestrial)
 * coming from ERMS tu table.<p>
 *
 * @author a.mueller
 * @since 28.09.2019
 */
@Component
public class PesiEcologyAndLinkExport extends PesiExportBase {

    private static final long serialVersionUID = -2567615286288369111L;
    private static Logger logger = LogManager.getLogger();

	private static final Class<? extends CdmBase> standardMethodParameter = TaxonBase.class;

	private static int modCount = 1000;
	private static final String dbTableName = "Note";
	private static final String pluralString = "ecology or link notes";
	private static final String parentPluralString = "Taxa";

	public PesiEcologyAndLinkExport() {
		super();
	}

	int countNotes = 0;
	int countUrls = 0;
	String currentValue;

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

			// Get specific mappings: (CDM) TaxonBase.marker -> (PESI) Note (ecology)
			PesiExportMapping mapping = getEcologyMapping();
			mapping.initialize(state);

			PesiExportMapping urlMapping = getUrlMapping();
			urlMapping.initialize(state);

			//All
			success &= doPhase01(state, mapping, urlMapping);

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

	//PHASE 01: All
	private boolean doPhase01(PesiExportState state, PesiExportMapping mapping, PesiExportMapping urlMapping) throws SQLException {

//	    logger.info("PHASE 1 (ecology...");
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		int limit = state.getConfig().getLimitSave();

		List<TaxonBase> taxonList = null;

		TransactionStatus txStatus = startTransaction(true);

		if (logger.isDebugEnabled()){
		    logger.info("Started new transaction. Fetching some " + parentPluralString + " (max: " + limit + ") ...");
		    logger.debug("Start snapshot, before starting loop");
		    ProfilerController.memorySnapshot();
		}

		List<String> propPath = null; //see #10779, setting the property path leads to memory issues due to large hibernate query plans by AdvancedBeanInitializer induced queries
		                              //Arrays.asList(new String[]{"markers.*","extensions.*"});
		int partitionCount = 0;
		while ((taxonList = getNextTaxonPartition(TaxonBase.class, limit, partitionCount++, propPath )) != null   ) {

			if (logger.isDebugEnabled()) {
                logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            }

			for (TaxonBase<?> taxon : taxonList) {
				doCount(count++, modCount, pluralString);
				state.setCurrentTaxon(taxon);
				if (!taxon.getMarkers().isEmpty()){
					success &= handleSingleEcologyTaxon(taxon, mapping);
				}
				if (!taxon.getExtensions().isEmpty()){
                    success &= handleSingleLinkTaxon(taxon, urlMapping);
                }
			}
			taxonList = null;
			state.setCurrentTaxon(null);

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " " + parentPluralString + ". Total taxa: " + count + ". Ecology notes: " + countNotes + ". Link notes: " + countUrls);
			pastCount = count;
			if (logger.isDebugEnabled()) {
                ProfilerController.memorySnapshot();
            }
			// Start transaction
			txStatus = startTransaction(true);
			if(logger.isDebugEnabled()) {
                logger.info("Started new transaction. Fetching some " + pluralString + " (max: " + limit + ") for description import ...");
            }
		}


		// Commit transaction
		commitTransaction(txStatus);
		logger.debug("Committed transaction.");
		return success;
	}

	private boolean handleSingleEcologyTaxon(TaxonBase<?> taxon, PesiExportMapping mapping) {

	    boolean success = true;
	    String ecologyStr = getEcologyString(taxon);
	    if (isNotBlank(ecologyStr)){
	        success &= mapping.invoke(taxon);
	        countNotes++;
	    }
		return success;
	}

    private static String getEcologyString(TaxonBase<?> taxon) {
        String ecologyStr = null;
	    ecologyStr = CdmUtils.concat(", ", ecologyStr, createEcologyStr(taxon, "marine", ErmsTransformer.uuidMarkerMarine));
	    ecologyStr = CdmUtils.concat(", ", ecologyStr, createEcologyStr(taxon, "brackish", ErmsTransformer.uuidMarkerBrackish));
	    ecologyStr = CdmUtils.concat(", ", ecologyStr, createEcologyStr(taxon, "fresh", ErmsTransformer.uuidMarkerFreshwater));
	    ecologyStr = CdmUtils.concat(", ", ecologyStr, createEcologyStr(taxon, "terrestrial", ErmsTransformer.uuidMarkerTerrestrial));
        return ecologyStr;
    }

    private static String createEcologyStr(TaxonBase<?> taxon, String strEcology, UUID markerUuid) {
        Boolean value = taxon.markerValue(markerUuid);
        if (value == null){
            return null;
        }else if (value == true){
            return strEcology;
        }else{
            return "not " + strEcology;
        }
    }

    private boolean handleSingleLinkTaxon(TaxonBase<?> taxon, PesiExportMapping mapping) {

        boolean success = true;
        Set<Extension> urlExtensions = taxon.getFilteredExtensions(ErmsTransformer.uuidExtErmsLink);
        for (Extension extension : urlExtensions){
            mapping.invoke(extension);
            countUrls++;
        }
        return success;
    }

	protected boolean doDelete(PesiExportState state) {
	    //Note table is already filled by Description import
	    //=> we do not empty any table here
		return true;
	}

    @SuppressWarnings("unused")  //used by mapper
    private static Integer getTaxonFk(TaxonBase<?> taxonBase, PesiExportState state) {
        return state.getDbId(taxonBase);
    }

    @SuppressWarnings("unused")  //used by mapper
    private static Integer getCurrentTaxonFk(Extension extension, PesiExportState state) {
        return state.getDbId(state.getCurrentTaxon());
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getNote_1(TaxonBase<?> taxon) {
        return getEcologyString(taxon);
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getUrlNote_1(Extension extension) {
        String value = extension.getValue();
        if (value == null){
            return null;
        }else{
            //TODO use regex grouping
            String result = value.split(ErmsLinkImport.TOKEN_LINKTEXT)[0];
            result = result.replace(ErmsLinkImport.TOKEN_URL, "").trim();
            return result;
        }
    }

//  @SuppressWarnings("unused")  //used by mapper
    private static String getUrlNote_2(Extension extension) {
        String value = extension.getValue();
        if (value == null){
            return null;
        }else{
            //TODO use regex grouping
            String[] split = value.split(ErmsLinkImport.TOKEN_LINKTEXT);
            if (split.length > 1){
                String result = split[1];
                result = result.trim();
                return CdmUtils.Ne(result);
            }else{
                return null;
            }
        }
    }

//  @SuppressWarnings("unused")  //used by mapper
    private static Integer getNoteCategoryFk(Extension extension) {
        String linktext = getUrlNote_2(extension);
        int catFk = categoryByLinkText(linktext);
        return catFk;
    }

    private static int categoryByLinkText(String linktext) {
        if (linktext == null){
            return PesiTransformer.NoteCategory_undefined_link;
        }else if (linktext.matches("(?i)(to fishbase|marine life inf).*")){
            return PesiTransformer.NoteCategory_Link_to_general_information;
        }else if (linktext.matches("(?i).*(clemam|nemys|algaebase|fishbase).*")){
            return PesiTransformer.NoteCategory_Link_to_taxonomy;
        }else{
            return PesiTransformer.NoteCategory_undefined_link;
        }
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getNoteCategoryCache(Extension extension) {
        int catFk = getNoteCategoryFk(extension);
        String result = categoryByLinkText(catFk);
        return result;
    }

    private static String categoryByLinkText(int catFk) {
        if(catFk == 22){
            return "";
        }else if (catFk == 23){
            return "";
        }else if (catFk == 24){
            return "";
        }else{
            logger.warn("Link category fk not yet supported: " + catFk);
            return null;
        }
    }

//******************************* MAPPINGS ********************************************

    private IdIncMapper idIncMapperEcology = IdIncMapper.NewComputedInstance("NoteId");
    private PesiExportMapping getEcologyMapping() {
        PesiExportMapping mapping = new PesiExportMapping(dbTableName);

        mapping.addMapper(idIncMapperEcology);
        mapping.addMapper(MethodMapper.NewInstance("Note_1", this, standardMethodParameter));

        mapping.addMapper(DbFixedIntegerMapper.NewInstance(PesiTransformer.NoteCategory_ecology, "NoteCategoryFk"));
        mapping.addMapper(DbFixedStringMapper.NewInstance("ecology", "NoteCategoryCache"));
        mapping.addMapper(DbFixedIntegerMapper.NewInstance(12, "LanguageFk"));
        mapping.addMapper(DbFixedStringMapper.NewInstance("English", "LanguageCache"));

        mapping.addMapper(MethodMapper.NewInstance("TaxonFk", this, standardMethodParameter, PesiExportState.class));

        mapping.addMapper(DbExportIgnoreMapper.NewInstance("Note_2", "not used for ecology fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("Region", "not used for ecology fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("SpeciesExpertGUID", "not used for ecology fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("SpeciesExpertName", "not used for ecology fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LastAction", "not used for ecology fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LastActionDate", "not used for ecology fact"));

        return mapping;
    }
    private PesiExportMapping getUrlMapping() {
        PesiExportMapping mapping = new PesiExportMapping(dbTableName);
        IdIncMapper idIncMapper = IdIncMapper.NewDependendInstance("NoteId", idIncMapperEcology);
        mapping.addMapper(idIncMapper);
        mapping.addMapper(MethodMapper.NewInstance("Note_1", this.getClass(), "getUrlNote_1", Extension.class));
        mapping.addMapper(MethodMapper.NewInstance("Note_2", this.getClass(), "getUrlNote_2", Extension.class));
        mapping.addMapper(MethodMapper.NewInstance("NoteCategoryFk", this.getClass(), Extension.class));
        mapping.addMapper(MethodMapper.NewInstance("NoteCategoryCache", this.getClass(), Extension.class));
        mapping.addMapper(MethodMapper.NewInstance("TaxonFk", this.getClass(), "getCurrentTaxonFk", Extension.class, PesiExportState.class));

        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LanguageFk", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LanguageCache", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("Region", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("SpeciesExpertGUID", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("SpeciesExpertName", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LastAction", "not used for link fact"));
        mapping.addMapper(DbExportIgnoreMapper.NewInstance("LastActionDate", "not used for link fact"));

        return mapping;
    }

    @Override
    protected boolean doCheck(PesiExportState state) {
        boolean result = true;
        return result;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoEcologyAndLink();
    }
}
