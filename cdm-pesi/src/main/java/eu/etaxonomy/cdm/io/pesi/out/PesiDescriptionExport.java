/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.common.mapping.out.CollectionExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.DbAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbAreaMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbConstantMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbDescriptionElementTaxonMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbDistributionStatusMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbExportIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLanguageMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbNullMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbOriginalNameMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbSimpleFilterMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbTextDataMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.LanguageString;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TaxonInteraction;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.MediaRepresentationPart;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.profiler.ProfilerController;
/**
 * The export class for {@link eu.etaxonomy.cdm.model.description.DescriptionElementBase DescriptionElements}.<p>
 * Inserts into DataWarehouse database table <code>Note</code>.<p>
 * It is divided into two phases:<ul>
 * <li>Phase 1:	Export of DescriptionElements as Notes.
 * <li>Phase 2:	Export of TaxonName extensions <code>taxComment</code>, <code>fauComment</code> and <code>fauExtraCodes</code> as Notes.</ul>
 * @author e.-m.lee
 * @since 23.02.2010
 * @author a.mueller
 */
@Component
public class PesiDescriptionExport extends PesiExportBase {

    private static final long serialVersionUID = -1486235807814098217L;
    private static Logger logger = LogManager.getLogger();

	private static final Class<? extends CdmBase> standardMethodParameter = DescriptionElementBase.class;

	private static int modCount = 1000;
	private static final String dbNoteTableName = "Note";
	private static final String dbOccurrenceTableName = "Occurrence";
	private static final String dbVernacularTableName = "CommonName";
	private static final String dbImageTableName = "Image";
	private static final String dbAdditionalSourceTableName = "AdditionalTaxonSource";
	private static final String pluralString = "descriptions";
	private static final String parentPluralString = "Taxa";

	//decide where to handle them best (configurator, transformer, single method, ...)
	private static Set<Integer> excludedNoteCategories = new HashSet<>(
	        Arrays.asList(new Integer[]{250,251,252,253,10,11,13}));

	//debugging
	private static int countDescriptions;
	private static int countTaxa;
	private static int countDistributionFiltered;
	private static int countAdditionalSources;
	private static int countImages;
	private static int countNotes;

	private static int countCommonName;
	private static int countOccurrence;
	private static int countOthers;

	public PesiDescriptionExport() {
		super();
	}

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}

	@Override
	protected void doInvoke(PesiExportState state) {
		try {
			logger.info("*** Started Making " + pluralString + " ...");

			// Stores whether this invoke was successful or not.
			boolean success = true;

			if (state.getConfig().getStartDescriptionPartition() == 0) {
			    success &= doDelete(state);
			}

			// Get specific mappings: (CDM) DescriptionElement -> (PESI) Note
			PesiExportMapping notesMapping = getNotesMapping();
			notesMapping.initialize(state);

			// Get specific mappings: (CDM) DescriptionElement -> (PESI) Occurrence
			PesiExportMapping occurrenceMapping = getOccurrenceMapping();
			occurrenceMapping.initialize(state);

			// Get specific mappings: (CDM) DescriptionElement -> (PESI) Additional taxon source
			PesiExportMapping addSourceSourceMapping = getAddTaxonSourceSourceMapping();
			addSourceSourceMapping.initialize(state);
			PesiExportMapping additionalSourceMapping = getAdditionalTaxonSourceMapping();
			additionalSourceMapping.initialize(state);

			// Get specific mappings: (CDM) DescriptionElement -> (PESI) Common name

			PesiExportMapping vernacularMapping = getCommonNamesMapping();
			vernacularMapping.initialize(state);

			// Get specific mappings: (CDM) DescriptionElement -> (PESI) Image
			PesiExportMapping imageMapping = getImageMapping();
			imageMapping.initialize(state);

			// Taxon Descriptions
			success &= doPhase01(state, notesMapping, occurrenceMapping, addSourceSourceMapping, additionalSourceMapping, vernacularMapping, imageMapping);

			// Name Descriptions
			success &= doPhase01b(state, notesMapping, occurrenceMapping, addSourceSourceMapping, additionalSourceMapping, vernacularMapping, imageMapping);

			logger.info("PHASE 2...");
			success &= doPhase02(state);

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

	//PHASE 01: Description Elements
	private boolean doPhase01(PesiExportState state, PesiExportMapping notesMapping, PesiExportMapping occurrenceMapping, PesiExportMapping addSourceSourceMapping,
			PesiExportMapping additionalSourceMapping, PesiExportMapping vernacularMapping, PesiExportMapping imageMapping) throws SQLException {

//	    System.out.println("PHASE 1 of description import");
	    logger.info("PHASE 1...");
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		int limit = state.getConfig().getLimitSaveDescription();

		List<Taxon> taxonList = null;

		TransactionStatus txStatus = startTransaction(true);

		if (logger.isDebugEnabled()){
		    logger.info("Started new transaction. Fetching some " + parentPluralString + " (max: " + limit + ") ...");
		    logger.debug("Start snapshot, before starting loop");
		    ProfilerController.memorySnapshot();
		}

		List<String> propPath = null; //see #10779, setting the property path leads to memory issues due to large hibernate query plans by AdvancedBeanInitializer induced queries
		                              // Arrays.asList(new String[]{"descriptions.elements.*"});
		int startPartition = state.getConfig().getStartDescriptionPartition();
		int maxPartitions = state.getConfig().getMaxDescriptionPartitions();
		int partitionCount = 0;
		while ((taxonList = getNextTaxonPartition(Taxon.class, limit, partitionCount++ + startPartition, propPath )) != null
		        && partitionCount < maxPartitions) {

			if (logger.isDebugEnabled()) {
                logger.info("Fetched " + taxonList.size() + " " + parentPluralString + ". Exporting...");
            }

			for (Taxon taxon : taxonList) {
				countTaxa++;
				doCount(count++, modCount, parentPluralString);
				state.setCurrentTaxon(taxon);
				if (!taxon.getDescriptions().isEmpty()){
					success &= handleSingleTaxon(taxon, state, notesMapping, occurrenceMapping, addSourceSourceMapping,
						additionalSourceMapping, vernacularMapping, imageMapping);
				}
			}
			taxonList = null;
			state.setCurrentTaxon(null);

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " " + parentPluralString + ". Total: " + count + " (Phase 01)");
			pastCount = count;
			ProfilerController.memorySnapshot();
			// Start transaction
			txStatus = startTransaction(true);
			if(logger.isDebugEnabled()) {
                logger.info("Started new transaction. Fetching some " + parentPluralString + " (max: " + limit + ") for description import ...");
            }
		}

		logger.info("No " + parentPluralString + " left to fetch.");
		logger.info("Partition: " + partitionCount);
		logger.info("Taxa: " + countTaxa);
		logger.info("Desc: " + countDescriptions);
		logger.info("Distr: " + countOccurrence);
		logger.info("Distr(Pesi): " + countDistributionFiltered);
		logger.info("Commons: " + countCommonName);
		logger.info("AddSrc: " + countAdditionalSources);
		logger.info("Images: " + countImages);
		logger.info("Notes: " + countNotes);
		logger.info("Others: " + countOthers);

		// Commit transaction
		commitTransaction(txStatus);
		logger.debug("Committed transaction.");
		return success;
	}

	//PHASE 01b: Name Descriptions
	private boolean doPhase01b(PesiExportState state, PesiExportMapping notesMapping, PesiExportMapping occurrenceMapping, PesiExportMapping addSourceSourceMapping,
			PesiExportMapping additionalSourceMapping, PesiExportMapping vernacularMapping, PesiExportMapping imageMapping) throws SQLException {

	    if (state.getConfig().getStartDescriptionPartition() > 0) {
	        logger.info("Skip PHASE 1b. Description partition is not 0 (first)");
	        return true;
	    }
	    logger.info("PHASE 1b...");
		int count = 0;
		int pastCount = 0;
		boolean success = true;
		//int limit = state.getConfig().getLimitSave();
		int limit = 2000;
		List<TaxonNameDescription> nameDescList = null;

		TransactionStatus txStatus = startTransaction(true);
		logger.info("Started new transaction. Fetching some name descriptions (max: " + limit + ") ...");
		List<String> propPath = Arrays.asList(new String[]{"descriptions.elements.*"});

		//name descriptions
		int partitionCount = 0;
		while ((nameDescList = getNextNameDescriptionPartition( limit, partitionCount++, propPath )) != null   ) {

			logger.info("Fetched " + nameDescList.size() + " name descriptions. Exporting...");

			for (TaxonNameDescription desc : nameDescList) {
				countTaxa++;
				doCount(count++, modCount, "name descriptions");
				boolean isImageGallery = desc.isImageGallery();

				TaxonName name = desc.getTaxonName();

				for (DescriptionElementBase element : desc.getElements()){
					if (isPurePesiName(name)){
						success &= handleDescriptionElement(state, notesMapping, occurrenceMapping, vernacularMapping, imageMapping,
								addSourceSourceMapping, additionalSourceMapping, isImageGallery, element);
					}else{
						for (TaxonBase<?> taxonBase : name.getTaxonBases()){
							if (isPesiTaxon(taxonBase)){
								state.setCurrentTaxon(taxonBase);
								success &= handleDescriptionElement(state, notesMapping, occurrenceMapping, vernacularMapping, imageMapping,
										addSourceSourceMapping, additionalSourceMapping, isImageGallery, element);
								state.setSourceForAdditionalSourceCreated(true);
							}
						}
						state.setSourceForAdditionalSourceCreated(false);
					}
				}
			}
			nameDescList = null;
			state.setCurrentTaxon(null);

			// Commit transaction
			commitTransaction(txStatus);
			logger.info("Exported " + (count - pastCount) + " name descriptions. Total: " + count);
			pastCount = count;

			// Start transaction
			txStatus = startTransaction(true);
			logger.info("Started new transaction. Fetching some name descriptions (max: " + limit + ") for description import ...");
		}

		logger.info("No " + parentPluralString + " left to fetch.");
		logger.info("Partition: " + partitionCount);
		logger.info("Taxa: " + countTaxa);
		logger.info("Desc: " + countDescriptions);
		logger.info("Occur: " + countOccurrence);
		logger.info("Distr(Pesi): " + countDistributionFiltered);
		logger.info("Commons: " + countCommonName);
		logger.info("AddSrc: " + countAdditionalSources);
		logger.info("Images: " + countImages);
		logger.info("Notes: " + countNotes);
        logger.info("Others: " + countOthers);

		// Commit transaction
		commitTransaction(txStatus);
		logger.debug("Committed transaction.");
		return success;
	}

	private boolean handleSingleTaxon(Taxon taxon, PesiExportState state, PesiExportMapping notesMapping, PesiExportMapping occurrenceMapping,
			PesiExportMapping addSourceSourceMapping, PesiExportMapping additionalSourceMapping,
			PesiExportMapping vernacularMapping, PesiExportMapping imageMapping) throws SQLException {

	    boolean success = true;

		Set<DescriptionBase<?>> descriptions = new HashSet<>();
		descriptions.addAll(taxon.getDescriptions());

		for (DescriptionBase<?> desc : descriptions){
			boolean isImageGallery = desc.isImageGallery();
			for (DescriptionElementBase element : desc.getElements()){
				success &= handleDescriptionElement(state, notesMapping, occurrenceMapping, vernacularMapping, imageMapping,
						addSourceSourceMapping, additionalSourceMapping, isImageGallery, element);
				countDescriptions++;
			}
		}
		if (logger.isDebugEnabled()) {
            logger.info("number of handled decriptionelements " + countDescriptions);
        }
		descriptions = null;
		return success;
	}

	private boolean handleDescriptionElement(PesiExportState state, PesiExportMapping notesMapping,
			PesiExportMapping occurrenceMapping, PesiExportMapping vernacularMapping, PesiExportMapping imageMapping,
			PesiExportMapping addSourceSourceMapping, PesiExportMapping additionalSourceMapping,
			boolean isImageGallery, DescriptionElementBase element) {

	    try {
			boolean success = true;
			if (isImageGallery){
				for (Media media : element.getMedia()){
				    countImages++;
				    success &= imageMapping.invoke(media);
				}
			}else if (isCommonName(element)){
				countCommonName++;
				if (element.isInstanceOf(TextData.class)){
					//we do not import text data common names
				}else{
					success &= vernacularMapping.invoke(element);
				}
			}else if (isOccurrence(element)){
				countOccurrence++;
				Distribution distribution = CdmBase.deproxy(element, Distribution.class);
//				MarkerType markerType = getUuidMarkerType(PesiTransformer.uuidMarkerTypeHasNoLastAction, state);
//				distribution.addMarker(Marker.NewInstance(markerType, true));
				if (!isPesiDistribution(state, distribution)){
				    logger.debug("Distribution is not PESI distribution: " + distribution.toString());
				}else{
					countDistributionFiltered++;
					try{
					    success &=occurrenceMapping.invoke(distribution);
					}catch(Exception e){
					    System.err.println(distribution.getInDescription().getTitleCache());
					    e.printStackTrace();
					}
				}
			}else if (isAdditionalTaxonSource(element)){
				countAdditionalSources++;
				if (! state.isSourceForAdditionalSourceCreated()){
					success &= addSourceSourceMapping.invoke(element);
				}
				success &= additionalSourceMapping.invoke(element);
			}else if (isExcludedNote(element)){
				//do nothing
			}else if (isPesiNote(element)){
				countNotes++;
				success &= notesMapping.invoke(element);
            }else{
				countOthers++;
				String featureTitle = element.getFeature() == null ? "no feature" :element.getFeature().getTitleCache();
				logger.warn("Description element type not yet handled by PESI export: " + element.getUuid() + ", " +  element.getClass() + ", " +  featureTitle);
			}
			return success;
		} catch (Exception e) {
			logger.warn("Exception appeared in description element handling: " + e);
			e.printStackTrace();
			return false;
		}
	}

    private boolean isExcludedNote(DescriptionElementBase element) {
		Integer categoryFk = PesiTransformer.feature2NoteCategoryFk(element.getFeature());
		//TODO decide where to handle them best (configurator, transformer, single method, ...)
		return (excludedNoteCategories.contains(categoryFk));
	}

    boolean hasFirstUndefinedStatusWarnung = false;
    boolean hasFirstIucnMissingWarning = false;
    private boolean isPesiDistribution(PesiExportState state, Distribution distribution) {
		//currently we use the E+M summary status to decide if a distribution should be exported
		PresenceAbsenceTerm status = distribution.getStatus();
	    if (status == null){
			return false;
		}else if (status.getUuid().equals(BerlinModelTransformer.uuidStatusUndefined)){
		    if (hasFirstUndefinedStatusWarnung){
                logger.warn("Status 'undefined' is not mapped to any status for now. Needs further checking. (E+M specific)");
                hasFirstUndefinedStatusWarnung = true;
            }
            return false;
		}else if (distribution.getFeature().getUuid().equals(Feature.uuidIucnStatus)){
            if (!hasFirstIucnMissingWarning){
                logger.warn("Status 'IUCN' is not mapped yet as it should go to the notes mapping, not the occurrences. (Bryophytes specific)");
                hasFirstIucnMissingWarning = true;
            }
            return false;
		}

		//...this may change in future so we keep the following code
		//area filter
		NamedArea area = distribution.getArea();
		if (area == null){
			logger.warn("Area is null for distribution " +  distribution.getUuid() +"/" + CdmBase.deproxy(distribution.getInDescription(), TaxonDescription.class).getTaxon().getTitleCache());
			return false;
		}else if (area.getUuid().equals(BerlinModelTransformer.euroMedUuid) ||
		        area.getUuid().equals(BerlinModelTransformer.uuidEUR)){
			//E+M area and mosses EUR area only hold endemic status information and therefore is not exported to PESI
			return false;
//		}else if (area.equals(TdwgAreaProvider.getAreaByTdwgAbbreviation("1"))){
//			//Europe area never holds status information (may probably be deleted in E+M)
//			return false;
//		}else if (area.equals(TdwgArea.getAreaByTdwgAbbreviation("21"))){
//			//Macaronesia records should not be exported to PESI
//			return false;
//		//TODO exclude Russion areas Rs*, and maybe others

		} else {
            try {
				if (state.getTransformer().getKeyByNamedArea(area) == null){
					String warning = "Area (%s,%s) not available in PESI transformer for taxon %s: ";
					TaxonBase<?> taxon =  state.getCurrentTaxon();
					warning = String.format(warning, area.getTitleCache(), area.getRepresentation(Language.ENGLISH()).getAbbreviatedLabel(),taxon ==null? "-" : taxon.getTitleCache());
					logger.warn(warning);
					return false;
				}
			} catch (UndefinedTransformerMethodException e1) {
				logger.warn("Area not available in PESI transformer " +  area.getTitleCache());
				return false;
			}
        }
		return true;
	}

	private boolean isPesiNote(DescriptionElementBase element) {
		return (getNoteCategoryFk(element) != null);
	}

	private boolean isAdditionalTaxonSource(DescriptionElementBase element) {
		Feature feature = element.getFeature();
		if (feature == null){
			return false;
		}
		return (feature.equals(Feature.CITATION()) || feature.equals(Feature.ADDITIONAL_PUBLICATION()));
	}

	private boolean isOccurrence(DescriptionElementBase fact) {
		Feature feature = fact.getFeature();
		if (fact.isInstanceOf(Distribution.class)){

		    if (feature == null) {
		        logger.warn("No feature defined for distribution. No Import.");
		        return false;
		    } else if (!Feature.uuidDistribution.equals(feature.getUuid())){
		        if (Feature.uuidIucnStatus.equals(feature.getUuid())) {
		            //we handle IUCN status later
		            return true;
		        } else {
		            logger.warn("Description element has class 'Distribution' but has no feature 'Distribution'. Not imported. FactID: "
		                    + fact.getUuid() + "; other feature: " + feature.getTitleCache());
		            return false;
		        }
		    }
		    return true;
		}else if (Feature.DISTRIBUTION().equals(feature)){
		    logger.debug("Description element has feature Distribtuion but is not of class 'Distribution'");
            return false;
		}else{
			return false;
		}
	}

	private boolean isCommonName(DescriptionElementBase element) {
		Feature feature = element.getFeature();
		if (feature == null){
			return false;
		}
		return (feature.equals(Feature.COMMON_NAME()));
	}

	//PHASE 02: Name extensions
	private boolean doPhase02(PesiExportState state) {
		TransactionStatus txStatus;
		boolean success =  true;

		if (state.getConfig().getStartDescriptionPartition() > 0) {
            logger.warn("Skip PHASE 2. Description partition is not 0 (first)");
            return true;
        }
	    logger.info("PHASE 2 ...");

		// Get the limit for objects to save within a single transaction.
		//int limit = state.getConfig().getLimitSave();
		int limit = 5000;
		txStatus = startTransaction(true);
		ExtensionType taxCommentExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtTaxComment);
		ExtensionType fauCommentExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtFauComment);
		ExtensionType fauExtraCodesExtensionType = (ExtensionType)getTermService().find(PesiTransformer.uuidExtFauExtraCodes);
		List<TaxonName> taxonNameList;

		int count = 0;
		int pastCount = 0;
		Connection connection = state.getConfig().getDestination().getConnection();
		if (logger.isDebugEnabled()) {
            logger.info("Started new transaction. Fetching some " + parentPluralString + " first (max: " + limit + ") ...");
        }
		logger.warn("TODO handle extensions on taxon level, not name level (");
		while ((taxonNameList = getNameService().list(null, limit, count, null, null)).size() > 0) {

			try {
                if(logger.isDebugEnabled()) {
                    logger.info("Fetched " + taxonNameList.size() + " names. Exporting...");
                }
                for (TaxonName taxonName : taxonNameList) {
                	Set<Extension> extensions = taxonName.getExtensions();
                	for (Extension extension : extensions) {
                		if (extension.getType() == null) {
                		    logger.warn("Extension has no type. Not imported: " + extension.getUuid() + " for name " + taxonName.getTitleCache());
                		    continue;
                		}
                	    if (extension.getType().equals(taxCommentExtensionType)) {
                			String taxComment = extension.getValue();
                			invokeNotes(taxComment,
                					PesiTransformer.getNoteCategoryFk(PesiTransformer.uuidExtTaxComment),
                					PesiTransformer.getNoteCategoryCache(PesiTransformer.uuidExtTaxComment),
                					null, null, getTaxonKey(taxonName, state),connection);
                		} else if (extension.getType().equals(fauCommentExtensionType)) {
                			String fauComment = extension.getValue();
                			invokeNotes(fauComment,
                					PesiTransformer.getNoteCategoryFk(PesiTransformer.uuidExtFauComment),
                					PesiTransformer.getNoteCategoryCache(PesiTransformer.uuidExtFauComment),
                					null, null, getTaxonKey(taxonName, state),connection);
                		} else if (extension.getType().equals(fauExtraCodesExtensionType)) {
                			String fauExtraCodes = extension.getValue();
                			invokeNotes(fauExtraCodes,
                					PesiTransformer.getNoteCategoryFk(PesiTransformer.uuidExtFauExtraCodes),
                					PesiTransformer.getNoteCategoryCache(PesiTransformer.uuidExtFauExtraCodes),
                					null, null, getTaxonKey(taxonName, state),connection);
                		}
                	}

                	doCount(count++, modCount, parentPluralString);
                }

                // Commit transaction
                commitTransaction(txStatus);
                logger.debug("Committed transaction.");
                logger.info("Exported " + (count - pastCount) + " names. Total: " + count + " (Phase 02)");
                pastCount = count;

                // Start transaction
                txStatus = startTransaction(true);
                if (logger.isDebugEnabled()) {
                    logger.info("Started new transaction. Fetching some names first (max: " + limit + ") ...");
                }
            } catch (Exception e) {
                logger.error("Unexpected exception occurred during description export.");
                e.printStackTrace();
            }
		}
		// Commit transaction
		commitTransaction(txStatus);
		logger.debug("Committed transaction.");
		return success;
	}

	private void invokeNotes(String note, Integer noteCategoryFk,
			String noteCategoryCache, Integer languageFk, String languageCache,
			Integer taxonFk, Connection connection) {

	    String notesSql = "UPDATE Note SET Note_1 = ?, NoteCategoryFk = ?, NoteCategoryCache = ?, LanguageFk = ?, LanguageCache = ? WHERE TaxonFk = ?";
		try {
			PreparedStatement notesStmt = connection.prepareStatement(notesSql);

			if (note != null) {
				notesStmt.setString(1, note);
			} else {
				notesStmt.setObject(1, null);
			}

			if (noteCategoryFk != null) {
				notesStmt.setInt(2, noteCategoryFk);
			} else {
				notesStmt.setObject(2, null);
			}

			if (noteCategoryCache != null) {
				notesStmt.setString(3, noteCategoryCache);
			} else {
				notesStmt.setObject(3, null);
			}

			if (languageFk != null) {
				notesStmt.setInt(4, languageFk);
			} else {
				notesStmt.setObject(4, null);
			}

			if (languageCache != null) {
				notesStmt.setString(5, languageCache);
			} else {
				notesStmt.setObject(5, null);
			}

			if (taxonFk != null) {
				notesStmt.setInt(6, taxonFk);
			} else {
				notesStmt.setObject(6, null);
			}

			notesStmt.executeUpdate();
		} catch (SQLException e) {
			logger.error("Note could not be created: " + note);
			e.printStackTrace();
		}
	}

	/**
	 * Deletes all entries of database tables related to <code>Note</code>.
	 * @param state The PesiExportState
	 * @return Whether the delete operation was successful or not.
	 */
	protected boolean doDelete(PesiExportState state) {
	    Source destination = state.getConfig().getDestination();

		// Clear NoteSource
		String sql = "DELETE FROM NoteSource";
		destination.update(sql);
		// Clear Note
		sql = "DELETE FROM Note "; // + dbNoteTableName;
		destination.update(sql);

	    // Clear OccurrenceSource
        sql = "DELETE FROM OccurrenceSource ";
        destination.update(sql);
        // Clear Occurrence
        sql = "DELETE FROM Occurrence ";
        destination.update(sql);

        // Clear Image
        sql = "DELETE FROM Image ";
        destination.update(sql);

        // Clear CommonName
        sql = "DELETE FROM CommonNameSource ";
        destination.update(sql);
        sql = "DELETE FROM CommonName ";
        destination.update(sql);

        // Clear AdditionalTaxonSource
        sql = "DELETE FROM AdditionalTaxonSource WHERE SourceFk >= 2000000 ";
        destination.update(sql);

        // Clear Sources for AdditionalTaxonSource
        sql = "DELETE FROM Source WHERE SourceId >= 2000000 ";
        destination.update(sql);

		return true;
	}

	/**
	 * Returns the <code>Note_2</code> attribute.
	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
	 * @return The <code>Note_2</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused") //used for mapper
	private static String getNote_2(DescriptionElementBase element) {
		//E+M map links -> medium
		if (element.getFeature() != null && element.getFeature().getUuid().equals(BerlinModelTransformer.uuidFeatureMaps)){
			String text = CdmBase.deproxy(element, TextData.class).getText(Language.ENGLISH());
			if (text.contains("medium")){
				return "medium";
			}
		}
		return null;
	}

	/**
	 * Returns the <code>NoteCategoryFk</code> attribute.
	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
	 * @return The <code>NoteCategoryFk</code> attribute.
	 * @see MethodMapper
	 */
	private static Integer getNoteCategoryFk(DescriptionElementBase descriptionElement) {
		Integer result = null;
		result = PesiTransformer.feature2NoteCategoryFk(descriptionElement.getFeature());
		//TODO decide where to handle them best (configurator, transformer, single method, ...)
		if (excludedNoteCategories.contains(result)){
			result = null;
		}
		return result;
	}

	/**
	 * Returns the <code>NoteCategoryCache</code> attribute.
	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
	 * @return The <code>NoteCategoryCache</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getNoteCategoryCache(DescriptionElementBase descriptionElement, PesiExportState state) {
		return state.getTransformer().getCacheByFeature(descriptionElement.getFeature());
	}

	/**
	 * Returns the <code>LanguageFk</code> attribute.
	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
	 * @return The <code>LanguageFk</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static Integer getLanguageFk(DescriptionElementBase descriptionElement) {
		Language language = getLanguage(descriptionElement);

		return PesiTransformer.language2LanguageId(language);
	}

	/**
	 * Returns the <code>LanguageCache</code> attribute.
	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
	 * @return The <code>LanguageCache</code> attribute.
	 * @throws UndefinedTransformerMethodException
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getLanguageCache(DescriptionElementBase descriptionElement, PesiExportState state) throws UndefinedTransformerMethodException {
		Language language = getLanguage(descriptionElement);
		return state.getTransformer().getCacheByLanguage(language);
	}

	private static Language getLanguage(DescriptionElementBase descriptionElement) {
		Language language = null;

		Map<Language, LanguageString> multilanguageText = null;
		if (descriptionElement.isInstanceOf(CommonTaxonName.class)) {
			CommonTaxonName commonTaxonName = CdmBase.deproxy(descriptionElement, CommonTaxonName.class);
			language = commonTaxonName.getLanguage();
		} else if (descriptionElement.isInstanceOf(TextData.class)) {
			TextData textData = CdmBase.deproxy(descriptionElement, TextData.class);
			multilanguageText = textData.getMultilanguageText();
		} else if (descriptionElement.isInstanceOf(IndividualsAssociation.class)) {
			IndividualsAssociation individualsAssociation = CdmBase.deproxy(descriptionElement, IndividualsAssociation.class);
			multilanguageText = individualsAssociation.getDescription();
		} else if (descriptionElement.isInstanceOf(TaxonInteraction.class)) {
			TaxonInteraction taxonInteraction = CdmBase.deproxy(descriptionElement, TaxonInteraction.class);
			multilanguageText = taxonInteraction.getDescription();
		} else {
			logger.debug("Given descriptionElement does not support languages. Hence LanguageCache could not be determined: " + descriptionElement.getUuid());
		}

		if (multilanguageText != null) {
			Set<Language> languages = multilanguageText.keySet();

			// TODO: Think of something more sophisticated than this
			if (languages.size() > 0) {
				language = languages.iterator().next();
			}
			if (languages.size() > 1){
				logger.warn("There is more than 1 language for a given description (" + descriptionElement.getClass().getSimpleName() + "):" + descriptionElement.getUuid());
			}
		}
		return language;
	}

//	/**
//	 * Returns the <code>Region</code> attribute.
//	 * @param descriptionElement The {@link DescriptionElementBase DescriptionElement}.
//	 * @return The <code>Region</code> attribute.
//	 * @see MethodMapper
//	 */
//	@SuppressWarnings("unused")
//	private static String getRegion(DescriptionElementBase descriptionElement) {
//		String result = null;
//		DescriptionBase<?> inDescription = descriptionElement.getInDescription();
//
//		// Area information are associated to TaxonDescriptions and Distributions.
//		if (descriptionElement.isInstanceOf(Distribution.class)) {
//			Distribution distribution = CdmBase.deproxy(descriptionElement, Distribution.class);
//			result = PesiTransformer.area2AreaCache(distribution.getArea());
//		} else if (inDescription != null && inDescription.isInstanceOf(TaxonDescription.class)) {
//			TaxonDescription taxonDescription = CdmBase.deproxy(inDescription, TaxonDescription.class);
//			Set<NamedArea> namedAreas = taxonDescription.getGeoScopes();
//			if (namedAreas.size() == 1) {
//				result = PesiTransformer.area2AreaCache(namedAreas.iterator().next());
//			} else if (namedAreas.size() > 1) {
//				logger.warn("This TaxonDescription contains more than one NamedArea: " + taxonDescription.getTitleCache());
//			}
//		}
//		return result;
//	}


	/**
	 * @param state The {@link DbExportStateBase DbExportState}.
	 * @return
	 */
	@SuppressWarnings("unused")  //used by mapper
	private static Integer getTaxonFk(DescriptionElementBase deb, PesiExportState state) {
		TaxonBase<?> entity = state.getCurrentTaxon();
		return state.getDbId(entity);
	}

	/**
	 * Returns the TaxonFk for a given TaxonName.
	 * @param taxonName The {@link TaxonNameBase TaxonName}.
	 * @param state The {@link DbExportStateBase DbExportState}.
	 * @return
	 */
	private static Integer getTaxonKey(TaxonName taxonName, DbExportStateBase<?, PesiTransformer> state) {
		return state.getDbId(taxonName);
	}

	/**
	 * Returns the <code>FullName</code> attribute.
	 * @param taxonName The {@link NonViralName NonViralName}.
	 * @return The <code>FullName</code> attribute.
	 * @see MethodMapper
	 */
	@SuppressWarnings("unused")
	private static String getTaxonFullNameCache(DescriptionElementBase deb, PesiExportState state) {

		TaxonBase<?> taxon =  state.getCurrentTaxon();
		TaxonName taxonName = taxon.getName();
		TaxonName nvn = CdmBase.deproxy(taxonName);
		String result = getCacheStrategy(nvn).getTitleCache(nvn);
		return result;
	}

    @SuppressWarnings("unused")  //used by mapper
    private static Integer getCurrentTaxonFk(Media media, PesiExportState state) {
        return state.getDbId(state.getCurrentTaxon());
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getMediaThumb(Media media) {
        String startsWith = "http://images.vliz.be/thumbs/";
        String result = null;
        for (MediaRepresentation rep : media.getRepresentations()){
            for (MediaRepresentationPart part : rep.getParts()){
                String strUrl = part.getUri().toString();
                if (strUrl.startsWith(startsWith)){
                    result = part.getUri().toString();
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unused")  //used by mapper
    private static String getMediaUrl(Media media) {
        String startsWith = "http://www.marbef.org/data/aphia.php?p=image&pic=";
        String result = null;
        for (MediaRepresentation rep : media.getRepresentations()){
            for (MediaRepresentationPart part : rep.getParts()){
                String strUrl = part.getUri().toString();
                if (strUrl.startsWith(startsWith)){
                    result = part.getUri().toString();
                }
            }
        }
        return result;
    }

//******************************* MAPPINGS ********************************************

	/**
	 * Returns the CDM to PESI specific export mappings for PESI notes.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getNotesMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbNoteTableName);

		mapping.addMapper(IdMapper.NewInstance("NoteId"));
		mapping.addMapper(DbTextDataMapper.NewDefaultInstance("Note_1"));
		//TODO
		mapping.addMapper(MethodMapper.NewInstance("Note_2", this, standardMethodParameter));
		mapping.addMapper(MethodMapper.NewInstance("NoteCategoryFk", this, standardMethodParameter ));
		mapping.addMapper(MethodMapper.NewInstance("NoteCategoryCache", this, standardMethodParameter, PesiExportState.class ));

		mapping.addMapper(MethodMapper.NewInstance("LanguageFk", this));
		mapping.addMapper(MethodMapper.NewInstance("LanguageCache", this, standardMethodParameter, PesiExportState.class));

//		mapping.addMapper(MethodMapper.NewInstance("Region", this));
		mapping.addMapper(DbDescriptionElementTaxonMapper.NewInstance("taxonFk"));

		mapping.addMapper(ExpertsAndLastActionMapper.NewInstance());

		mapping.addCollectionMapping(getNoteSourceMapping());
		return mapping;
	}

	private CollectionExportMapping<PesiExportState, PesiExportConfigurator,PesiTransformer> getNoteSourceMapping() {
		String tableName = "NoteSource";
		String collectionAttribute = "sources";
		IdMapper parentMapper = IdMapper.NewInstance("NoteFk");
		@SuppressWarnings("unchecked")
        CollectionExportMapping<PesiExportState, PesiExportConfigurator, PesiTransformer> mapping
                = CollectionExportMapping.NewInstance(tableName, collectionAttribute, parentMapper);
		mapping.addMapper(DbSimpleFilterMapper.NewSingleNullAttributeInstance("idInSource", "Sources with idInSource currently handle data lineage"));
		mapping.addMapper(DbObjectMapper.NewNotNullInstance("Citation", "SourceFk"));
		mapping.addMapper(DbObjectMapper.NewInstance("Citation", "SourceNameCache", IS_CACHE));
		mapping.addMapper(DbStringMapper.NewInstance("CitationMicroReference", "SourceDetail"));
		return mapping;
	}

	/**
	 * Returns the CDM to PESI specific export mappings for occurrences.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getOccurrenceMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbOccurrenceTableName);

		mapping.addMapper(IdMapper.NewInstance("OccurrenceId"));
		mapping.addMapper(DbDescriptionElementTaxonMapper.NewInstance("taxonFk"));
		mapping.addMapper(DbDescriptionElementTaxonMapper.NewInstance("TaxonFullNameCache", true, true, null));

		mapping.addMapper(DbAreaMapper.NewInstance(Distribution.class, "Area", "AreaFk", ! IS_CACHE));
		mapping.addMapper(DbAreaMapper.NewInstance(Distribution.class, "Area", "AreaNameCache", IS_CACHE));
		mapping.addMapper(DbDistributionStatusMapper.NewInstance("OccurrenceStatusFk", ! IS_CACHE));
		mapping.addMapper(DbDistributionStatusMapper.NewInstance("OccurrenceStatusCache", IS_CACHE));

//		Use OccurrenceSource table instead
		mapping.addMapper(DbNullMapper.NewIntegerInstance("SourceFk"));
		mapping.addMapper(DbNullMapper.NewStringInstance("SourceCache"));

		mapping.addMapper(DbAnnotationMapper.NewExludedInstance(getLastActionAnnotationTypes(), "Notes"));
		mapping.addMapper(ExpertsAndLastActionMapper.NewInstance());

		mapping.addCollectionMapping(getOccurrenceSourceMapping());

		return mapping;
	}

	private CollectionExportMapping<PesiExportState, PesiExportConfigurator, PesiTransformer> getOccurrenceSourceMapping() {
		String tableName = "OccurrenceSource";
		String collectionAttribute = "sources";
		IdMapper parentMapper = IdMapper.NewInstance("OccurrenceFk");
		@SuppressWarnings("unchecked")
        CollectionExportMapping<PesiExportState, PesiExportConfigurator, PesiTransformer> mapping
		        = CollectionExportMapping.NewInstance(tableName, collectionAttribute, parentMapper);
		mapping.addMapper(DbSimpleFilterMapper.NewSingleNullAttributeInstance("idInSource",
		        "Sources with idInSource currently handle data lineage"));
        mapping.addMapper(DbSimpleFilterMapper.NewAllowedValueInstance("idInSource",
              EnumSet.of(OriginalSourceType.PrimaryTaxonomicSource, OriginalSourceType.PrimaryMediaSource, OriginalSourceType.Aggregation),
              null, "Only primary taxonomic sources should be exported"));
		mapping.addMapper(DbObjectMapper.NewNotNullInstance("Citation", "SourceFk"));
		mapping.addMapper(DbObjectMapper.NewInstance("Citation", "SourceNameCache", IS_CACHE));
		mapping.addMapper(DbStringMapper.NewInstance("CitationMicroReference", "SourceDetail"));
        mapping.addMapper(DbOriginalNameMapper.NewInstance("OldTaxonName", IS_CACHE, null));

		return mapping;
	}

	/**
	 * Returns the CDM to PESI specific export mappings for additional taxon sources to create a new
	 * source for the additional taxon source
	 * @see #{@link PesiDescriptionExport#getAdditionalTaxonSourceMapping()}
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getAddTaxonSourceSourceMapping() {
		PesiExportMapping sourceMapping = new PesiExportMapping(PesiSourceExport.dbTableName);

		sourceMapping.addMapper(IdMapper.NewInstance("SourceId"));
		sourceMapping.addMapper(DbConstantMapper.NewInstance("SourceCategoryFk", Types.INTEGER, PesiTransformer.REF_UNRESOLVED));
		sourceMapping.addMapper(DbConstantMapper.NewInstance("SourceCategoryCache", Types.VARCHAR, PesiTransformer.REF_STR_UNRESOLVED));

//		sourceMapping.addMapper(MethodMapper.NewInstance("NomRefCache", PesiSourceExport.class, "getNomRefCache", Reference.class));

		sourceMapping.addMapper(DbTextDataMapper.NewDefaultInstance("NomRefCache"));

		return sourceMapping;
	}

	/**
	 * Returns the CDM to PESI specific export mappings for additional taxon sources.
	 * @see #{@link PesiDescriptionExport#getAddTaxonSourceSourceMapping()}
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getAdditionalTaxonSourceMapping() {

		PesiExportMapping mapping = new PesiExportMapping(dbAdditionalSourceTableName);

		mapping.addMapper(MethodMapper.NewInstance("TaxonFk", this, DescriptionElementBase.class, PesiExportState.class));

		mapping.addMapper(IdMapper.NewInstance("SourceFk"));
		mapping.addMapper(DbTextDataMapper.NewDefaultInstance("SourceNameCache"));

		mapping.addMapper(DbConstantMapper.NewInstance("SourceUseFk", Types.INTEGER, PesiTransformer.NOMENCLATURAL_REFERENCE));
		mapping.addMapper(DbConstantMapper.NewInstance("SourceUseCache", Types.VARCHAR, PesiTransformer.STR_NOMENCLATURAL_REFERENCE));

		mapping.addMapper(DbExportIgnoreMapper.NewInstance("SourceDetail", "SourceDetails not available for additional sources"));

		return mapping;
	}

	/**
	 * Returns the CDM to PESI specific export mappings for common names.
	 * @return The {@link PesiExportMapping PesiExportMapping}.
	 */
	private PesiExportMapping getCommonNamesMapping() {
		PesiExportMapping mapping = new PesiExportMapping(dbVernacularTableName);

		mapping.addMapper(IdMapper.NewInstance("CommonNameId"));
		mapping.addMapper(DbDescriptionElementTaxonMapper.NewInstance("taxonFk"));

		mapping.addMapper(DbStringMapper.NewInstance("Name", "CommonName"));
		mapping.addMapper(DbAreaMapper.NewInstance(CommonTaxonName.class, "Area", "Region", IS_CACHE));

		mapping.addMapper(DbLanguageMapper.NewInstance(CommonTaxonName.class, "Language", "LanguageFk", ! IS_CACHE));
		mapping.addMapper(DbLanguageMapper.NewInstance(CommonTaxonName.class, "Language", "LanguageCache", IS_CACHE));

//      Use CommonNameSource table instead
        mapping.addMapper(DbNullMapper.NewIntegerInstance("SourceFk"));
        mapping.addMapper(DbNullMapper.NewStringInstance("SourceNameCache"));
        //OLD
//		mapping.addMapper(DbSingleSourceMapper.NewInstance("SourceFk", of ( DbSingleSourceMapper.EXCLUDE.WITH_ID) , ! IS_CACHE));
//		mapping.addMapper(DbSingleSourceMapper.NewInstance("SourceNameCache", of ( DbSingleSourceMapper.EXCLUDE.WITH_ID) , IS_CACHE));

		//no SpeciesExpertGUID and SpeciesExpertName for E+M according to SQL
        mapping.addMapper(ExpertsAndLastActionMapper.NewInstance());

	    mapping.addCollectionMapping(getCommonNameSourceMapping());
		return mapping;
	}

    private CollectionExportMapping<PesiExportState, PesiExportConfigurator, PesiTransformer> getCommonNameSourceMapping() {
        String tableName = "CommonNameSource";
        String collectionAttribute = "sources";
        IdMapper parentMapper = IdMapper.NewInstance("CommonNameFk");
        @SuppressWarnings("unchecked")
        CollectionExportMapping<PesiExportState, PesiExportConfigurator, PesiTransformer> mapping
                = CollectionExportMapping.NewInstance(tableName, collectionAttribute, parentMapper);
        mapping.addMapper(DbSimpleFilterMapper.NewSingleNullAttributeInstance("idInSource",
                "Sources with idInSource currently handle data lineage"));
        mapping.addMapper(DbObjectMapper.NewNotNullInstance("Citation", "SourceFk"));
        mapping.addMapper(DbObjectMapper.NewInstance("Citation", "SourceNameCache", IS_CACHE));
        mapping.addMapper(DbStringMapper.NewInstance("CitationMicroReference", "SourceDetail"));
        mapping.addMapper(DbOriginalNameMapper.NewInstance("OldTaxonName", IS_CACHE, null));

        return mapping;
    }

	private PesiExportMapping getImageMapping() {
	    PesiExportMapping mapping = new PesiExportMapping(dbImageTableName);
	    mapping.addMapper(MethodMapper.NewInstance("taxonFk", this.getClass(), "getCurrentTaxonFk", Media.class, PesiExportState.class));
		mapping.addMapper(MethodMapper.NewInstance("img_thumb", this.getClass(), "getMediaThumb", Media.class));
		mapping.addMapper(MethodMapper.NewInstance("img_url", this.getClass(), "getMediaUrl", Media.class));
		return mapping;
	}

    @Override
    protected boolean doCheck(PesiExportState state) {
        boolean result = true;
        return result;
    }

    @Override
    protected boolean isIgnore(PesiExportState state) {
        return ! state.getConfig().isDoDescription();
    }
}
