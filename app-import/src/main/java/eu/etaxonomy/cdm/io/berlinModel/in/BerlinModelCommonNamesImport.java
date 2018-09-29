/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelCommonNamesImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 *
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelCommonNamesImport  extends BerlinModelImportBase {
    private static final long serialVersionUID = -8921948187177864321L;

    private static final Logger logger = Logger.getLogger(BerlinModelCommonNamesImport.class);

	public static final UUID REFERENCE_LANGUAGE_ISO639_2_UUID = UUID.fromString("40c4f8dd-3d9c-44a4-b77a-76e137a89a5f");
	public static final UUID REFERENCE_LANGUAGE_STRING_UUID = UUID.fromString("2a1b678f-c27d-48c1-b43e-98fd0d426305");
	public static final UUID COMMONNAME_STATUS_RECOMMENDED_UUID = UUID.fromString("e3f7b80a-1286-458d-812c-5e818f731968");
	public static final UUID COMMONNAME_STATUS_SYNONYM_UUID = UUID.fromString("169b2d97-a706-49de-b28b-c67f0ee6764b");

	public static final String NAMESPACE = "common name";


	private static final String pluralString = "common names";
	private static final String dbTableName = "emCommonName";


	//map that stores the regions (named areas) and makes them accessible via the regionFk
	private Map<String, NamedArea> regionFkToAreaMap = new HashMap<>();

	public BerlinModelCommonNamesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT CommonNameId FROM emCommonName WHERE (1=1) ";
		if (isNotBlank(state.getConfig().getCommonNameFilter())){
			result += " AND " + state.getConfig().getCommonNameFilter();
		}
		result += " ORDER BY CommonNameId ";

		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String recordQuery = "";
		recordQuery =
				" SELECT rel.RelPTaxonId, rel.RelQualifierFk, acc.RIdentifier accTaxonId, factTaxon.RIdentifier factTaxonId, accName.NameId, f.FactId, " +
				           "       cn.CommonNameId, cn.CommonName, tax.RIdentifier AS taxonId, cn.PTNameFk, cn.RefFk AS refId, cn.Status, cn.RegionFks, cn.MisNameRefFk, " +
					       "       cn.NameInSourceFk, cn.Created_When, cn.Updated_When, cn.Created_Who, cn.Updated_Who, cn.Note AS Notes, languageCommonName.Language, " +
					       "       languageCommonName.LanguageOriginal, languageCommonName.ISO639_1, languageCommonName.ISO639_2,   " +
					       "       emLanguageReference.RefFk AS languageRefRefFk, emLanguageReference.ReferenceShort, emLanguageReference.ReferenceLong,  " +
					       "       emLanguageReference.LanguageFk, languageReferenceLanguage.Language AS refLanguage, languageReferenceLanguage.ISO639_2 AS refLanguageIso639_2,  "+
					       "       misappliedTaxon.RIdentifier AS misappliedTaxonId " +
				  " FROM  PTaxon AS misappliedTaxon RIGHT OUTER JOIN " +
					       "      emLanguage AS languageReferenceLanguage RIGHT OUTER JOIN " +
			               "      emLanguageReference ON languageReferenceLanguage.LanguageId = emLanguageReference.LanguageFk RIGHT OUTER JOIN " +
			               "      emCommonName AS cn INNER JOIN " +
			               "      PTaxon AS tax ON cn.PTNameFk = tax.PTNameFk AND cn.PTRefFk = tax.PTRefFk ON  " +
			               "      emLanguageReference.ReferenceId = cn.LanguageRefFk LEFT OUTER JOIN " +
			               "      emLanguage AS languageCommonName ON cn.LanguageFk = languageCommonName.LanguageId ON misappliedTaxon.PTNameFk = cn.NameInSourceFk AND  " +
			               "      misappliedTaxon.PTRefFk = cn.MisNameRefFk " +

	                     "     LEFT OUTER JOIN Fact f ON cn.CommonNameId = f.ExtensionFk " +
	                     "     LEFT OUTER JOIN PTaxon factTaxon ON factTaxon.PTNameFk = f.PTNameFk AND factTaxon.PTRefFk = f.PTRefFk " +
	                     "     LEFT OUTER JOIN RelPTaxon rel ON rel.PTNameFk1 = tax.PTNameFk AND rel.PTRefFk1 = tax.PTRefFk AND rel.RelQualifierFk IN (2,6,7) " +
                         "     LEFT OUTER JOIN PTaxon acc ON rel.PTNameFk2 = acc.PTNameFk AND rel.PTRefFk2 = acc.PTRefFk " +
                         "     LEFT OUTER JOIN Name accName ON accName.NameId = acc.PTNameFk " +
			        " WHERE cn.CommonNameId IN (" + ID_LIST_TOKEN + ") " +
			        " ORDER BY cn.CommonNameId ";

		return recordQuery;
	}

	@Override
	protected void doInvoke(BerlinModelImportState state) {
		try {
			makeRegions(state);
		} catch (Exception e) {
			logger.error("Error when creating common name regions:" + e.getMessage());
			e.printStackTrace();
			state.setUnsuccessfull();
		}
		super.doInvoke(state);
		return;
	}

	/**
	 * @param state
	 *
	 */
	private void makeRegions(BerlinModelImportState state) {
		try {
			TransactionStatus tx = startTransaction();
		    SortedSet<Integer> regionFks = new TreeSet<>();
			Source source = state.getConfig().getSource();

			//fill set with all regionFk from emCommonName.regionFks
			fillRegionFks(state, regionFks, source);
			//concat filter string
			String sqlWhere = getSqlWhere(regionFks);

			//get E+M - TDWG Mapping
//			Map<String, String> emTdwgMap = getEmTdwgMap(source);
			Map<String, NamedArea> emCodeToAreaMap = getEmCodeToAreaMap(source);
			//fill regionMap
			fillRegionMap(state, sqlWhere, emCodeToAreaMap);

			commitTransaction(tx);

			return;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			state.setUnsuccessfull();
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			state.setUnsuccessfull();
			return;
		}
	}


    @Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state)  {
		boolean success = true ;

		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, Taxon> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, TaxonName> taxonNameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		Map<String, Language> iso6392Map = new HashMap<>();

	//	logger.warn("MisappliedNameRefFk  not yet implemented for Common Names");

		ResultSet rs = partitioner.getResultSet();
		Integer lastCommonNameId = null;
		try{
			while (rs.next()){

				//create TaxonName element
				Integer commonNameId = rs.getInt("CommonNameId");
				int taxonId = rs.getInt("taxonId");
				Integer factTaxonId = nullSafeInt(rs, "factTaxonId");
				Integer accTaxonId = nullSafeInt(rs, "accTaxonId");  //if common name is related to synonym this is the accepted taxon id

				Integer refId = nullSafeInt(rs, "refId");
//				Integer ptNameFk = nullSafeInt(rs,"PTNameFk");
				String commonNameString = rs.getString("CommonName");
				String iso639_2 = rs.getString("ISO639_2");
				String iso639_1 = rs.getString("ISO639_1");
				String languageString = rs.getString("Language");
				String originalLanguageString = rs.getString("LanguageOriginal");
				Integer misNameRefFk = nullSafeInt(rs, "MisNameRefFk");
				Integer languageRefRefFk = nullSafeInt(rs, "languageRefRefFk");
				String refLanguage = rs.getString("refLanguage");
				String refLanguageIso639_2 = rs.getString("refLanguageIso639_2");
				String status = rs.getString("Status");
				Integer nameInSourceFk = nullSafeInt( rs, "NameInSourceFk");
				Integer misappliedTaxonId = nullSafeInt( rs, "misappliedTaxonId");

				if (commonNameId == lastCommonNameId){
				    logger.warn("CommonNameId >1 times in query. This may happen due to LEFT JOINS to fact and/or accepted taxon and e.g. multiple taxon relationships. 2018-04-01 no such double relation existed in E+M. ");
				}else{
				    lastCommonNameId = commonNameId;
				}

				final String NO_REGION = "";
				//regions
				String regionFks  = rs.getString("RegionFks");
				String[] regionFkSplit = (regionFks==null)? new String[]{NO_REGION} : regionFks.trim().split(",");
				if (regionFkSplit.length == 0){
				    String message = "regionFkSplit should never be empty but was for common name id " + commonNameId;
                    logger.warn(message);
				}

				//commonNameString
				if (isBlank(commonNameString)){
					String message = "CommonName is empty or null. Do not import record for taxon " + taxonId;
					logger.warn(message);
					continue;
				}

				//taxon
				Taxon taxon = null;
				TaxonBase<?> taxonBase  = taxonMap.get(String.valueOf(taxonId));
				if (taxonBase == null){
					logger.warn("Taxon (" + taxonId + ") could not be found. Common name " + commonNameString + "(" + commonNameId + ") not imported");
					continue;
				}else if (taxonBase.isInstanceOf(Taxon.class)){
				    taxon = CdmBase.deproxy(taxonBase, Taxon.class);
				    if (factTaxonId != null && !factTaxonId.equals(taxonId)){
				        logger.warn("Fact taxon ("+factTaxonId+") for common name "+commonNameId+" differs from common name taxon " + taxonId);
				    }
				}else{
				    Taxon factTaxon = null;
				    if (factTaxonId != null && factTaxonId.equals(accTaxonId)){
				        factTaxon = taxonMap.get(String.valueOf(factTaxonId));
				    }
				    if (factTaxon != null){
				        taxon = factTaxon;
				    }else{
				        logger.warn("taxon (" + taxonId + ") is not accepted. Can't import common name " +  commonNameId + ". FactTaxonId= " +  factTaxonId + "; accTaxonId = " + accTaxonId);
				        continue;
				    }
				}

				//Language
				Language language = getAndHandleLanguage(iso6392Map, iso639_2, iso639_1, languageString, originalLanguageString, state);

				//CommonTaxonName
				List<CommonTaxonName> commonTaxonNames = new ArrayList<>();
				for (String regionFk : regionFkSplit){ //
					CommonTaxonName commonTaxonName;
					if (commonTaxonNames.size() == 0){
						commonTaxonName = CommonTaxonName.NewInstance(commonNameString, language);
					}else{
						commonTaxonName = (CommonTaxonName)commonTaxonNames.get(0).clone();
					}
					commonTaxonNames.add(commonTaxonName);
					regionFk = regionFk.trim();
					NamedArea area = regionFkToAreaMap.get(regionFk);
					if (area == null){
						if (isNotBlank(regionFk) && regionFk != NO_REGION){
							logger.warn("Area for " + regionFk + " not defined in regionMap.");
						}else{
							//no region is defined
						}
					}else{
						commonTaxonName.setArea(area);
					}
					TaxonDescription description = getDescription(taxon);
					description.addElement(commonTaxonName);
				}

				//Reference/Source
				if (! CdmUtils.nullSafeEqual(refId, languageRefRefFk)){
					//use strRefId if languageRefFk is null
					if (languageRefRefFk == null){
						languageRefRefFk = refId;
					}else{
						logger.warn("CommonName.RefFk (" + CdmUtils.Nz(refId) + ") and LanguageReference.RefFk " + languageRefRefFk  + " are not equal. I will import only languageReference.RefFk");
					}
				}

				Reference reference = refMap.get(String.valueOf(languageRefRefFk));
				if (reference == null && languageRefRefFk != null){
					logger.warn("CommonName reference was null but reference exists. languageRefRefFk = " + languageRefRefFk + "; commonNameId = " + commonNameId);
				}
				String microCitation = null;
				String originalNameString = null;

				TaxonName nameUsedInSource = taxonNameMap.get(String.valueOf(nameInSourceFk));
				if (nameInSourceFk != null && nameUsedInSource == null){
					if (nameInSourceFk != -1 || !state.getConfig().isEuroMed()){
					    logger.warn("Name used in source (" + nameInSourceFk + ") was not found for common name " + commonNameId);
					}
				}
				for (CommonTaxonName commonTaxonName : commonTaxonNames){
				    DescriptionElementSource source = DescriptionElementSource.NewPrimarySourceInstance(reference, microCitation, nameUsedInSource, originalNameString);
	                commonTaxonName.addSource(source);
				}

				//MisNameRef
				if (misNameRefFk != null){
					//Taxon misappliedName = getMisappliedName(biblioRefMap, nomRefMap, misNameRefFk, taxon);
					Taxon misappliedNameTaxon = null;
					if (misappliedTaxonId != null){
						TaxonBase<?> misTaxonBase =  taxonMap.get(String.valueOf(misappliedTaxonId));
						if (misTaxonBase == null){
							logger.warn("MisappliedName not found for misappliedTaxonId " + misappliedTaxonId + "; commonNameId: " + commonNameId);
						}else if (misTaxonBase.isInstanceOf(Taxon.class)){
							misappliedNameTaxon = CdmBase.deproxy(misTaxonBase, Taxon.class);
						}else{
							logger.warn("Misapplied name taxon is not of type Taxon but " + misTaxonBase.getClass().getSimpleName());
						}
					}else{
						Reference sec = refMap.get(String.valueOf(misNameRefFk));
						if (nameUsedInSource == null || sec == null){
							logger.warn("Taxon name or misapplied name reference is null for common name " + commonNameId);
						}else{
							misappliedNameTaxon = Taxon.NewInstance(nameUsedInSource, sec);
							MarkerType misCommonNameMarker = getMarkerType(state, BerlinModelTransformer.uuidMisappliedCommonName,"Misapplied Common Name in Berlin Model",
							        "Misapplied taxon was automatically created by Berlin Model import for a common name with a misapplied name reference", "MCN", getEuroMedMarkerTypeVoc());
							Marker marker = Marker.NewInstance(misCommonNameMarker, true);
							misappliedNameTaxon.addMarker(marker);
							taxaToSave.add(misappliedNameTaxon);
							logger.warn("Misapplied name taxon could not be found in database but misapplied name reference exists for common name. " +
									"New misapplied name for misapplied reference common name was added. CommonNameId: " + commonNameId);
						}
					}
					if (misappliedNameTaxon != null){

						if (! taxon.getMisappliedNames(false).contains(misappliedNameTaxon)){
							taxon.addMisappliedName(misappliedNameTaxon, state.getTransactionalSourceReference(), null);
							logger.warn("Misapplied name for common name was not found related to the accepted taxon. Created new relationship. CommonNameId: " + commonNameId);
						}

						//add common name also to missaplied taxon
						//TODO is this really wanted
						TaxonDescription misappliedNameDescription = getDescription(misappliedNameTaxon);
						for (CommonTaxonName commonTaxonName : commonTaxonNames){
							CommonTaxonName commonNameClone = (CommonTaxonName)commonTaxonName.clone();
							misappliedNameDescription.addElement(commonNameClone);
							doIdCreatedUpdatedNotes(state, commonNameClone, rs, String.valueOf(commonNameId), NAMESPACE);
						}
					}else{
						//wird schon oben gelogged
					    //logger.warn("Misapplied name is null for common name " + commonNameId);
					}

				}

				//reference extensions
				if (reference != null){
					if (isNotBlank(refLanguage) && !reference.hasExtension(REFERENCE_LANGUAGE_STRING_UUID, refLanguage)){
						ExtensionType refLanguageExtensionType = getExtensionType( state, REFERENCE_LANGUAGE_STRING_UUID, "reference language","The language of the reference","ref. lang.");
						Extension.NewInstance(reference, refLanguage, refLanguageExtensionType);
					}

					if (isNotBlank(refLanguageIso639_2) && !reference.hasExtension(REFERENCE_LANGUAGE_ISO639_2_UUID, refLanguage)){
						ExtensionType refLanguageIsoExtensionType = getExtensionType( state, REFERENCE_LANGUAGE_ISO639_2_UUID, "reference language iso 639-2","The iso 639-2 code of the references language","ref. lang. 639-2");
						Extension.NewInstance(reference, refLanguageIso639_2, refLanguageIsoExtensionType);
					}
				}else if (isNotBlank(refLanguage) || isNotBlank(refLanguageIso639_2)){
					logger.warn("Reference is null (" + languageRefRefFk + ") but refLanguage (" + CdmUtils.Nz(refLanguage) + ") or iso639_2 (" + CdmUtils.Nz(refLanguageIso639_2) + ") was not null for common name ("+ commonNameId +")");
				}

				//status
				if (isNotBlank(status)){
				    TermVocabulary<MarkerType> markerTypeVoc = getEuroMedMarkerTypeVoc();
				    MarkerType recommendedMarkerType = getMarkerType( state, COMMONNAME_STATUS_RECOMMENDED_UUID, "recommended","If the common name has the status recommended (see also status 'synonym', if none of them is true the default status is 'unassessed')",
				            "recommended", markerTypeVoc);
					MarkerType synonymMarkerType = getMarkerType( state, COMMONNAME_STATUS_SYNONYM_UUID, "synonym","If the common name has the status synonym (see also status 'recommended', if none of them is true the default status is 'unassessed')",
					        "synonym", markerTypeVoc);
                    for (CommonTaxonName commonTaxonName : commonTaxonNames){
                        Marker marker = null;
                        if (status.equals("recommended")){
						    marker = Marker.NewInstance(recommendedMarkerType, true);
                        }else if (status.equals("synonym")){
                            marker = Marker.NewInstance(synonymMarkerType, true);
                        }else if (status.equals("unassessed")){
                            //do nothing
                        }else{
						    logger.warn("Unknown common name status: " + status);
						}
                        if (marker != null){
                            commonTaxonName.addMarker(marker);
                        }
					}
				}

				//Notes
				for (CommonTaxonName commonTaxonName : commonTaxonNames){
					doIdCreatedUpdatedNotes(state, commonTaxonName, rs, String.valueOf(commonNameId), NAMESPACE);
				}
				partitioner.startDoSave();
				taxaToSave.add(taxon);

			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		} catch (ClassCastException e) {
			e.printStackTrace();
		} catch (Exception e) {
            throw e;
        }

		//	logger.info( i + " names handled");
		getTaxonService().save(taxaToSave);
		return success;

	}


    /**
	 * @param iso6392Map
	 * @param iso639_2
	 * @param languageString
	 * @param originalLanguageString
	 * @param state
	 * @return
	 */
	private Language getAndHandleLanguage(Map<String, Language> iso639Map,	String iso639_2, String iso639_1, String languageString, String originalLanguageString, BerlinModelImportState state) {
		Language language;
		if (isNotBlank(iso639_2)|| isNotBlank(iso639_1)  ){
			//TODO test performance, implement in state
			language = getLanguageFromIsoMap(iso639Map, iso639_2, iso639_1);

			if (language == null){
				language = getTermService().getLanguageByIso(iso639_2);
				iso639Map.put(iso639_2, language);
				if (language == null){
					try {
						language = getTermService().getLanguageByIso(iso639_1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						// TODO remove if problem with duplicate DescElement_Annot id is solved
						e.printStackTrace();
					}
					iso639Map.put(iso639_1, language);
				}
				if (language == null){
					logger.warn("Language for code ISO693-2 '" + iso639_2 + "' and ISO693-1 '" + iso639_1 + "' was not found");
				}
			}
		} else if ("unknown".equals(languageString)){
			language = Language.UNKNOWN_LANGUAGE();
		} else if ("Majorcan".equalsIgnoreCase(languageString)){
			language = getLanguage(state, BerlinModelTransformer.uuidLangMajorcan, "Majorcan", "Majorcan (original 'mallorqu\u00EDn')", null);
		}else{
			logger.warn("language ISO 639_1 and ISO 639_2 were empty for " + languageString);
			language = null;
		}
		addOriginalLanguage(language, originalLanguageString);
		return language;
	}


	/**
	 * @param iso639Map
	 * @param iso639_2
	 * @param iso639_1
	 * @return
	 */
	private Language getLanguageFromIsoMap(Map<String, Language> iso639Map,	String iso639_2, String iso639_1) {
		Language language;
		language = iso639Map.get(iso639_2);
		if (language == null){
			language = iso639Map.get(iso639_1);
		}
		return language;
	}

	/**
	 * @param language
	 * @param originalLanguageString
	 */
	private void addOriginalLanguage(Language language,	String originalLanguageString) {
		if (isBlank(originalLanguageString)){
			return;
		}else if (language == null){
			logger.warn("Language could not be defined, but originalLanguageString exists: " + originalLanguageString);
		}else {
			Representation representation = language.getRepresentation(language);
			if (representation == null){
				language.addRepresentation(Representation.NewInstance(originalLanguageString, originalLanguageString, originalLanguageString, language));
				getTermService().saveOrUpdate(language);
			}
		}

	}



	/**
	 * Fills the regionFks with all regionFks from emCommonName. Comma separated regionFks will be split.
	 * @param state
	 * @param regionFks
	 * @param source
	 * @return
	 * @throws SQLException
	 *
	 */
	private void fillRegionFks(BerlinModelImportState state, SortedSet<Integer> regionFks,
	        Source source) throws SQLException {
		String sql =
		          " SELECT DISTINCT RegionFks "
		        + " FROM emCommonName";
		if (state.getConfig().getCommonNameFilter() != null){
			sql += " WHERE " + state.getConfig().getCommonNameFilter();
		}

		ResultSet rs = source.getResultSet(sql);
		while (rs.next()){
			String strRegionFks = rs.getString("RegionFks");
			if (isBlank(strRegionFks)){
				continue;
			}

			String[] regionFkArray = strRegionFks.split(",");
			for (String regionFk: regionFkArray){
				regionFk = regionFk.trim();
				if (! StringUtils.isNumeric(regionFk) || "".equals(regionFk)  ){
					state.setUnsuccessfull();
					logger.warn("RegionFk is not numeric: " + regionFk +  " ( part of " + strRegionFks + ")");
				}else{
					regionFks.add(Integer.valueOf(regionFk));
				}
			}
		}
		return;
	}



	/**
	 * Fills the {@link #regionMap} by all emLanguageRegion regions defined in the sql filter.
	 * {@link #regionMap} maps emLanguageRegion.RegionId to named areas.
	 * @param state
	 * @param sqlWhere
	 * @param emTdwgMap
	 * @throws SQLException
	 */
	private void fillRegionMap(BerlinModelImportState state, String sqlWhere,
			Map<String, NamedArea> emCodeToAreaMap) throws SQLException {

	    Source source = state.getConfig().getSource();
		String sql =
		      " SELECT RegionId, Region "
		    + " FROM  emLanguageRegion "
		    + " WHERE RegionId IN ("+ sqlWhere+ ") ";
		ResultSet rs = source.getResultSet(sql);
		while (rs.next()){
			Object regionId = rs.getObject("RegionId");
			String region = rs.getString("Region");
			String[] splitRegion = region.split("-");
			if (splitRegion.length <= 1){
				NamedArea newArea = getNamedArea(state, null, region, "Language region '" + region + "'", null, null, null);
//				getTermService().save(newArea);
				regionFkToAreaMap.put(String.valueOf(regionId), newArea);
				logger.warn("Found new area: " +  region);
			}else if (splitRegion.length == 2){
				String emCode = splitRegion[1].trim().replace(" ", "");

				NamedArea area = emCodeToAreaMap.get(emCode);
				if (area == null){
				    String[] splits = emCode.split("/");
				    if (splits.length == 2){
				        area = emCodeToAreaMap.get(splits[0]);
		            }
				    if (area != null){
				        logger.warn("emCode ambigous. Use larger area: " +  CdmUtils.Nz(emCode) + "->" + regionId);
				    }else{
				        logger.warn("emCode not recognized. Region not defined: " +  CdmUtils.Nz(emCode) + "->" + regionId);
				    }
				}
				if (area != null){
				    regionFkToAreaMap.put(String.valueOf(regionId), area);
				}
			}
		}
	}

	/**
	 * @param regionFks
	 * @return
	 */
	private String getSqlWhere(SortedSet<Integer> regionFks) {
		String sqlWhere = "";
		for (Integer regionFk : regionFks){
			sqlWhere += regionFk + ",";
		}
		sqlWhere = sqlWhere.substring(0, sqlWhere.length()-1);
		return sqlWhere;
	}

//	/**
//	 * Returns a map which is filled by the emCode->TdwgCode mapping defined in emArea.
//	 * Some exceptions are defined for emCode 'Ab','Rf','Uk' and some additional mapping is added
//	 * for 'Ab / Ab(A)', 'Ga / Ga(F)', 'It / It(I)', 'Ar / Ar(A)','Hs / Hs(S)'
//	 * @param source
//	 * @throws SQLException
//	 */
//	private Map<String, String> getEmTdwgMap(Source source) throws SQLException {
//
//		Map<String, String> emTdwgMap = new HashMap<>();
//		String sql = " SELECT EmCode, TDWGCode "
//		    + " FROM emArea ";
//		ResultSet rs = source.getResultSet(sql);
//		while (rs.next()){
//			String emCode = rs.getString("EMCode");
//			String TDWGCode = rs.getString("TDWGCode");
//			if (isNotBlank(emCode) ){
//				emCode = emCode.trim();
//				if (emCode.equalsIgnoreCase("Ab") || emCode.equalsIgnoreCase("Rf")||
//						emCode.equalsIgnoreCase("Uk") || emCode.equalsIgnoreCase("Gg")
//						|| emCode.equalsIgnoreCase("SM") || emCode.equalsIgnoreCase("Tu")){
//					emTdwgMap.put(emCode, emCode);
//				}else if (isNotBlank(TDWGCode)){
//					emTdwgMap.put(emCode, TDWGCode.trim());
//				}
//			}
//		}
//		emTdwgMap.put("Ab / Ab(A)", "Ab");
//		emTdwgMap.put("Ga / Ga(F)", "FRA-FR");
//		emTdwgMap.put("It / It(I)", "ITA");
//		emTdwgMap.put("Uk / Uk(U)", "Uk");
//		emTdwgMap.put("Ar / Ar(A)", "TCS-AR");
//		emTdwgMap.put("Hs / Hs(S)", "SPA-SP");
//		emTdwgMap.put("Hb / Hb(E)", "IRE-IR");
//
//		return emTdwgMap;
//	}



    /**
     * @param source
     * @return
     * @throws SQLException
     */
    private Map<String, NamedArea> getEmCodeToAreaMap(Source source) throws SQLException {
        Map<String, NamedArea> emCodeToAreaMap = new HashMap<>();
        String sql =
              " SELECT EmCode, AreaId "
            + " FROM emArea ";
        ResultSet rs = source.getResultSet(sql);
        while (rs.next()){

            String emCode = rs.getString("EMCode");
            if (isNotBlank(emCode)){
                Integer areaId = rs.getInt("AreaId");
                NamedArea area = getAreaByAreaId(areaId);
                if (area != null){
                    emCodeToAreaMap.put(emCode.trim(), area);
                }else{
                    logger.warn("Area not found for areaId " + areaId);
                }
            }

        }

//        emTdwgMap.put("Ab / Ab(A)", "Ab");

        return emCodeToAreaMap;
    }

    /**
     * @param emCode
     * @return
     */
    private NamedArea getAreaByAreaId(int areaId) {
        NamedArea result = null;
        String areaIdStr = String.valueOf(areaId);
        OrderedTermVocabulary<NamedArea> voc = getAreaVoc();
        getVocabularyService().update(voc);
        for (NamedArea area : voc.getTerms()){
            for (IdentifiableSource source : area.getSources()){
                if (areaIdStr.equals(source.getIdInSource()) && BerlinModelAreaImport.NAMESPACE.equals(source.getIdNamespace())){
                    if (result != null){
                        logger.warn("Result for areaId already exists. areaId: " + areaId);
                    }
                    result = area;
                }
            }
        }
        return result;
    }

    private OrderedTermVocabulary<NamedArea> areaVoc;
    @SuppressWarnings("unchecked")
    private OrderedTermVocabulary<NamedArea> getAreaVoc(){
        if (areaVoc == null){
            areaVoc = (OrderedTermVocabulary<NamedArea>)getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
        }
        return areaVoc;
    }


	/**
	 * Returns the first non-image gallery description. Creates a new one if no description exists.
	 * @param taxon
	 * @return
	 */
	private TaxonDescription getDescription(Taxon taxon) {
		TaxonDescription result = null;
		for (TaxonDescription taxonDescription : taxon.getDescriptions()){
			if (! taxonDescription.isImageGallery()){
				result = taxonDescription;
			}
		}
		if (result == null){
			result = TaxonDescription.NewInstance(taxon);
		}
		return result;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		String pos = "0";
		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
				handleForeignKey(rs, taxonIdSet, "factTaxonId");
                handleForeignKey(rs, taxonIdSet, "misappliedTaxonId");
				handleForeignKey(rs, referenceIdSet, "refId");
				handleForeignKey(rs, referenceIdSet, "languageRefRefFk");
				handleForeignKey(rs, nameIdSet, "NameInSourceFk");
				handleForeignKey(rs, nameIdSet, "PTNameFk");
				handleForeignKey(rs, referenceIdSet, "MisNameRefFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			cdmClass = TaxonName.class;
			idSet = nameIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonName> nameMap = (Map<String, TaxonName>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, referenceMap);
			// TODO remove if problem with duplicate DescElement_Annot id is solved
		} catch (SQLException e) {
			throw new RuntimeException("pos: " + pos, e);
		} catch (NullPointerException nep){
			logger.error("NullPointerException in getRelatedObjectsForPartition()");
		}
		return result;
	}


	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelCommonNamesImportValidator();
		return validator.validate(state);
	}


	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoCommonNames();
	}

}
