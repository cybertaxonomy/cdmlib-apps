/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_ACCEPTED;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_PARTIAL_SYN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_PRO_PARTE_SYN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_SYNONYM;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_UNRESOLVED;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Identifier;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.IdentifierType;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonImport  extends BerlinModelImportBase {

    private static final long serialVersionUID = -1186364983750790695L;
    private static final Logger logger = LogManager.getLogger();

	public static final String NAMESPACE = "Taxon";

	private static final String pluralString = "Taxa";
	private static final String dbTableName = "PTaxon";

	private static final String LAST_SCRUTINY_FK = "lastScrutinyFk";

	/**
	 * How should the publish flag in table PTaxon be interpreted
	 * NO_MARKER: No marker is set
	 * ONLY_FALSE:
	 */
	public enum PublishMarkerChooser{
		NO_MARKER,
		ONLY_FALSE,
		ONLY_TRUE,
		ALL;

		boolean doMark(boolean value){
			if (value == true){
				return this == ALL || this == ONLY_TRUE;
			}else{
				return this == ALL || this == ONLY_FALSE;
			}
		}
	}

	public BerlinModelTaxonImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String sqlSelect = " SELECT RIdentifier";
		String taxonTable = state.getConfig().getTaxonTable();
		String sqlFrom = String.format(" FROM %s ", taxonTable);
		String sqlWhere = "";

		String sql = sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return sql;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String sqlSelect = " SELECT pt.* ";
		String sqlFrom = " FROM PTaxon pt ";
		if (config.isEuroMed()){
			sqlFrom = " FROM PTaxon AS pt "
			                + " INNER JOIN v_cdm_exp_taxaAll AS em ON pt.RIdentifier = em.RIdentifier "
							+ " LEFT OUTER JOIN Name n ON pt.PTNameFk = n.nameId ";
			if (!config.isUseLastScrutinyAsSec()){
			    sqlFrom += " LEFT OUTER JOIN Reference r ON pt.LastScrutinyFk = r.RefId ";
			}
			sqlSelect += ", n.notes nameNotes , em.MA ";
			if (!config.isUseLastScrutinyAsSec()){
			    sqlSelect += ", r.RefCache as LastScrutiny ";
            }
		}

		String sqlWhere = " WHERE ( pt.RIdentifier IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
//			" SELECT * " +
//			" FROM PTaxon " + state.getConfig().getTaxonTable();
//			" WHERE ( RIdentifier IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonImportValidator();
		return validator.validate(state);
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {

	    boolean success = true ;
		BerlinModelImportConfigurator config = state.getConfig();
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, TaxonName> taxonNameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			boolean publishFlagExists = state.getConfig().getSource().checkColumnExists("PTaxon", "PublishFlag");
			boolean isEuroMed = config.isEuroMed();
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				int taxonId = rs.getInt("RIdentifier");
				int statusFk = rs.getInt("statusFk");

				int nameFk = rs.getInt("PTNameFk");
				int refFkInt = rs.getInt("PTRefFk");
				String doubtful = rs.getString("DoubtfulFlag");
				String uuid = null;
				if (resultSetHasColumn(rs,"UUID")){
					uuid = rs.getString("UUID");
				}

				TaxonName taxonName = null;
				taxonName  = taxonNameMap.get(String.valueOf(nameFk));

				Reference reference = null;
				String refFkStr = String.valueOf(refFkInt);
				reference = refMap.get(refFkStr);

				Reference lastScrutinyRef = null;
                if (state.getConfig().isUseLastScrutinyAsSec() && resultSetHasColumn(rs,LAST_SCRUTINY_FK)){
                    Integer lastScrutinyFk = nullSafeInt(rs,LAST_SCRUTINY_FK);
                    if (lastScrutinyFk != null){
                        String lastScrutinyFkStr = String.valueOf(lastScrutinyFk);
                        if (lastScrutinyFkStr != null){
                            lastScrutinyRef = refMap.get(lastScrutinyFkStr);
                            if (lastScrutinyRef == null){
                                logger.warn("Last scrutiny reference "+lastScrutinyFkStr+" could not be found "
                                        + "for taxon " + taxonId);
                            }
                            //MANs do have last scrutiny => the following is not correct
//                            if(!StringUtils.right(refFkStr, 5).equals("00000")){
//                                logger.warn("Unexpected secFk " + refFkStr + " for taxon with last scrutiny. Taxon id " + taxonId);
//                            }
                        }
                    }
                }

				if(! config.isIgnoreNull()){
					if (taxonName == null ){
						logger.warn("TaxonName belonging to taxon (RIdentifier = " + taxonId + ") could not be found in store. Taxon will not be imported");
						success = false;
						continue; //next taxon
					}else if (reference == null ){
						logger.warn("Sec Reference belonging to taxon could not be found in store. Taxon will not be imported");
						success = false;
						continue; //next taxon
					}
				}
				TaxonBase<?> taxonBase;
				Synonym synonym;
				Taxon taxon;
				Reference sec = (lastScrutinyRef != null && isRightAccessSec(refFkInt)) ? lastScrutinyRef: reference;
				try {
					logger.debug(statusFk);
					if (statusFk == T_STATUS_ACCEPTED || statusFk == T_STATUS_UNRESOLVED
					        || statusFk == T_STATUS_PRO_PARTE_SYN || statusFk == T_STATUS_PARTIAL_SYN ){
						taxon = Taxon.NewInstance(taxonName, sec);
						taxonBase = taxon;
						if (statusFk == T_STATUS_UNRESOLVED){
							taxon.setTaxonStatusUnknown(true);
						}
						//TODO marker for pp and partial?
					}else if (statusFk == T_STATUS_SYNONYM ){
						synonym = Synonym.NewInstance(taxonName, sec);
						taxonBase = synonym;
//						if (statusFk == T_STATUS_PRO_PARTE_SYN){
//						    synonym.setProParte(true);
//						}
//						if (statusFk == T_STATUS_PARTIAL_SYN){
//							synonym.setPartial(true);
//						}
					}else{
						logger.warn("TaxonStatus " + statusFk + " not yet implemented. Taxon (RIdentifier = " + taxonId + ") left out.");
						success = false;
						continue;
					}
					if (uuid != null){
						taxonBase.setUuid(UUID.fromString(uuid));
					}

					//doubtful
					if (doubtful.equals("a")){
						taxonBase.setDoubtful(false);
					}else if(doubtful.equals("d")){
						taxonBase.setDoubtful(true);
					}else if(doubtful.equals("i")){
						taxonBase.setDoubtful(false);
						logger.warn("Doubtful = i (inactivated) does not exist in CDM. Doubtful set to false. RIdentifier: " + taxonId);
					}

					//detail
					String detail = rs.getString("Detail");
					if (isNotBlank(detail)){
//						ExtensionType detailExtensionType = getExtensionType(state, BerlinModelTransformer.DETAIL_EXT_UUID, "micro reference","micro reference","micro ref.");
//						Extension.NewInstance(taxonBase, detail, detailExtensionType);
						taxonBase.setSecMicroReference(detail.trim());
					}
					//idInSource
					String idInSource = rs.getString("IdInSource");
					if (isNotBlank(idInSource)){
						if(!state.getConfig().isEuroMed() && !state.getConfig().isMcl()){
						    ExtensionType detailExtensionType = getExtensionType(state, BerlinModelTransformer.ID_IN_SOURCE_EXT_UUID, "Berlin Model IdInSource","Berlin Model IdInSource","BM source id");
						    Extension.NewInstance(taxonBase, idInSource.trim(), detailExtensionType);
						}else if(isMclIdentifier(state,rs, idInSource) || state.getConfig().isMcl()){
						    IdentifierType identifierType = getIdentiferType(state, BerlinModelTransformer.uuidEM_MCLIdentifierType, "MCL identifier", "Med-Checklist identifier", "MCL ID", null);
						    Identifier.NewInstance(taxonBase, idInSource.trim(), identifierType);
						}
						//maybe we want to handle it as fact in future for MCL
//						if (state.getConfig().isMcl()) {
//						}
					}
					//namePhrase
					String namePhrase = rs.getString("NamePhrase");
					if (StringUtils.isNotBlank(namePhrase)){
						taxonBase.setAppendedPhrase(namePhrase);
					}
					//useNameCache
					Boolean useNameCacheFlag = rs.getBoolean("UseNameCacheFlag");
					if (useNameCacheFlag){
						taxonBase.setUseNameCache(true);
					}
					//publisheFlag
					if (publishFlagExists){
						Boolean publishFlag = rs.getBoolean("PublishFlag");
						Boolean misapplied = false;
						if (isEuroMed){
							misapplied = rs.getBoolean("MA");
						}

						if ( ! misapplied){
							taxonBase.setPublish(publishFlag);
						}
					}

					//  does not exist anymore as we use last scrutiny now as sec ref
					if (!state.getConfig().isUseLastScrutinyAsSec() && resultSetHasColumn(rs, "LastScrutiny")){
						String lastScrutiny = rs.getString("LastScrutiny");
						//TODO strange, why not Extension last scrutiny? To match PESI? Is there a difference
						//to LastScrutinyFK and SpeciesExpertFK?
						if (isNotBlank(lastScrutiny)){
						    ExtensionType extensionTypeSpeciesExpert = getExtensionType(state, BerlinModelTransformer.uuidSpeciesExpertName, "Species Expert", "Species Expert", "Species Expert");
						    taxonBase.addExtension(lastScrutiny, extensionTypeSpeciesExpert);
						    ExtensionType extensionTypeExpert = getExtensionType(state, BerlinModelTransformer.uuidExpertName, "Expert", "Expert for a taxonomic group", "Expert");
						    taxonBase.addExtension(lastScrutiny, extensionTypeExpert);
						}
					}
					//
					if (resultSetHasColumn(rs, "IsExcludedMarker")){
					    boolean isExcluded = rs.getBoolean("IsExcludedMarker");
					    if (isExcluded){
					        String extension = rs.getString("IsExcludedExtension");
					        String valueless = "not accepted: taxonomically valueless local or singular biotype";
					        String provisional = "provisional: probably a taxonomically valueless local or singular biotype";

					        MarkerType markerType = null;
					        if (valueless.equals(extension)){
					            markerType = getMarkerType(state, BerlinModelTransformer.uuidTaxonomicallyValueless, "taxonomically valueless", valueless, "valueless", getEuroMedMarkerTypeVoc(state));
					        }else if (provisional.equals(extension)){
                                markerType = getMarkerType(state, BerlinModelTransformer.uuidProbablyTaxonomicallyValueless, "probably taxonomically valueless", provisional, "provisional", getEuroMedMarkerTypeVoc(state));
                            }
					        if (markerType != null){
					            taxonBase.addMarker(Marker.NewInstance(markerType, true));
					        }else{
					            logger.warn("IsExcludedExtension not regonized for taxon " + taxonId + "; " + extension);
					        }
					    }
					}

					//Notes
					boolean excludeNotes = state.getConfig().isTaxonNoteAsFeature() && taxonBase.isInstanceOf(Taxon.class);
					String notes = rs.getString("Notes");
					if (state.getConfig().isEuroMed()){
					    if (isNotBlank(notes) && notes.startsWith("non ")){
					        taxonBase.setAppendedPhrase(CdmUtils.concat("; ", taxonBase.getAppendedPhrase(), notes));
					        notes = null;
					    }
					    String nameNotes = rs.getString("nameNotes");
					    nameNotes = BerlinModelTaxonNameImport.filterNotes(nameNotes, 900000000 + taxonId);
					    if (BerlinModelTaxonNameImport.isPostulatedParentalSpeciesNote(nameNotes)){
					        nameNotes = nameNotes.replace("{", "").replace("}", "");
					        String text = "For intermediate, so-called \"collective\" species in the genus Pilosella, a combination of the postulated parental basic species is given.";
					        UUID parSpecUuid = BerlinModelTransformer.PARENTAL_SPECIES_EXT_UUID;
					        ExtensionType parentalSpeciesExtType = getExtensionType(state, parSpecUuid, " Postulated parental species", text, "par. spec.");
					        Extension.NewInstance(taxonBase, nameNotes, parentalSpeciesExtType);
					    }
					}

					doIdCreatedUpdatedNotes(state, taxonBase, rs, taxonId, NAMESPACE, false, excludeNotes || notes == null);
					if (excludeNotes && notes != null){
					    makeTaxonomicNote(state, CdmBase.deproxy(taxonBase, Taxon.class), rs.getString("Notes"));
					}

					//external url
					if (config.getMakeUrlForTaxon() != null){
						Method urlMethod = config.getMakeUrlForTaxon();
						urlMethod.invoke(null, taxonBase, rs);
					}

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when creating taxon with id " + taxonId + ". Taxon could not be saved.");
					success = false;
				}
			}
		} catch (DatabaseTypeNotSupportedException e) {
			logger.error("MethodNotSupportedException:" +  e);
			return false;
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		return success;
	}

    private boolean isMclIdentifier(BerlinModelImportState state, ResultSet rs, String idInSource) throws SQLException {
        if (idInSource.contains("-")){
            return true;
        }else if (idInSource.matches("(293|303)")){
            String created = rs.getString("Created_Who");
            if (created.endsWith(".xml")){
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getIdInSource(BerlinModelImportState state, ResultSet rs) throws SQLException {
        String id = rs.getString("idInSource");
        return id;
    }


    /**
     * @param refFkInt
     * @return
     */
    private boolean isRightAccessSec(Integer refFkInt) {
        List<Integer> rightAccessSecs = Arrays.asList(new Integer[]{7000000, 7100000, 7200000, 7300000,
                7400000, 7500000, 7600000, 7700000, 8000000, 8500000, 9000000});
        return rightAccessSecs.contains(refFkInt);
    }

    /**
     * @param state
     * @param taxonBase
	 * @param notes
     */
    private void makeTaxonomicNote(BerlinModelImportState state, Taxon taxon, String notes) {
        if (isNotBlank(notes)){
            TaxonDescription desc = getTaxonDescription(taxon, false, true);
            desc.setDefault(true);  //hard coded for Salvador, not used elsewhere as far as I can see
            TextData textData = TextData.NewInstance(Feature.NOTES() , notes, Language.SPANISH_CASTILIAN(), null);
            desc.addElement(textData);
        }
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "PTNameFk");
				handleForeignKey(rs, referenceIdSet, "PTRefFk");
				if (state.getConfig().isUseLastScrutinyAsSec() && resultSetHasColumn(rs, LAST_SCRUTINY_FK)){
				    handleForeignKey(rs, referenceIdSet, LAST_SCRUTINY_FK);
				}
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoTaxa();
	}

}
