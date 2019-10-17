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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelOccurrenceSourceImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.RelationshipBase.Direction;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelOccurrenceSourceImport  extends BerlinModelImportBase {

    private static final String EXACT = "(exact) ";

    private static final long serialVersionUID = 1139543760239436841L;

    private static final Logger logger = Logger.getLogger(BerlinModelOccurrenceSourceImport.class);

	private static int modCount = 5000;
	private static final String pluralString = "occurrence sources";
	private static final String dbTableName = "emOccurrenceSource";  //??


	private Map<String, Integer> sourceNumberRefIdMap;
	private Map<String, Set<Integer>> nameCache2NameIdMap;
	private Set<String> notFoundReferences = new HashSet<>();


	public BerlinModelOccurrenceSourceImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = "SELECT occurrenceSourceId FROM " + getTableName();
		if (state.getConfig().getOccurrenceSourceFilter() != null){
			result += " WHERE " +  state.getConfig().getOccurrenceSourceFilter();
		}
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =   //DISTINCT because otherwise emOccurrenceSource creates multiple records for a single distribution
            " SELECT occ.*, n.nameCache, n.fullNameCache " +
                " FROM emOccurrenceSource occ LEFT OUTER JOIN Name n ON n.nameId = occ.oldNameFk " +
            " WHERE (OccurrenceSourceId IN (" + ID_LIST_TOKEN + ")  )" +
             "";
		return strQuery;
	}



	@Override
	protected void doInvoke(BerlinModelImportState state) {
		notFoundReferences = new HashSet<>();

		try {
			sourceNumberRefIdMap = makeSourceNumberReferenceIdMap(state);
			nameCache2NameIdMap = makeNameCache2NameIdMap(state);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		super.doInvoke(state);
		sourceNumberRefIdMap = null;
		nameCache2NameIdMap = null;
		if (notFoundReferences.size()>0){
			String unfound = "'" + CdmUtils.concat("','", notFoundReferences.toArray(new String[]{})) + "'";
			logger.warn("Not found references: " + unfound);
		}
		return;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true;
		ResultSet rs = partitioner.getResultSet();
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		Set<DescriptionElementBase> objectsToSave = new HashSet<>();
		try {
			int i = 0;
			//for each reference
            while (rs.next()){

                if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("occurrence sources handled: " + (i-1));}

                Integer occurrenceSourceId = rs.getInt("OccurrenceSourceId");
                Integer occurrenceFk =nullSafeInt(rs, "OccurrenceFk");
    			String sourceNumber = rs.getString("SourceNumber");
    			String oldName = rs.getString("OldName");
    			Integer oldNameFk = nullSafeInt(rs, "OldNameFk");
    			String oldNameFkCache = rs.getString("nameCache");
    			String oldNameFkFullCache = rs.getString("fullNameCache");

    			Distribution distribution = (Distribution)state.getRelatedObject(BerlinModelOccurrenceImport.NAMESPACE, String.valueOf(occurrenceFk));

    			if (distribution == null){
    				//distribution = duplicateMap.get(occurrenceFk);
    			}
    			if (distribution != null){
    				Integer refId = sourceNumberRefIdMap.get(sourceNumber);
    				Reference ref = refMap.get(String.valueOf(refId));

    				if (ref != null){
    					DescriptionElementSource originalSource = DescriptionElementSource.NewInstance(OriginalSourceType.PrimaryTaxonomicSource);
    					originalSource.setCitation(ref);
    					TaxonName taxonName = getName(state, oldName, oldNameFk, oldNameFkFullCache, oldNameFkCache, occurrenceSourceId, distribution);
						if (taxonName != null){
						    if(isNotBlank(oldName) && !oldName.equals(taxonName.getNameCache())){
	                            originalSource.setOriginalNameString(oldName);
	                        }
						    originalSource.setNameUsedInSource(taxonName);
    					}else if(isNotBlank(oldName)){
    						originalSource.setOriginalNameString(oldName);
    					}
    					distribution.addSource(originalSource);
    				}else{
    					logger.warn("reference for sourceNumber "+sourceNumber+" could not be found. OccurrenceSourceId: " + occurrenceSourceId );
    					notFoundReferences.add(sourceNumber);
    				}
    			}else{
    				logger.warn("distribution ("+occurrenceFk+") for occurrence source (" + occurrenceSourceId + ") could not be found." );
    			}

            }
			logger.info("Distributions to save: " + objectsToSave.size());
			getDescriptionService().saveDescriptionElement(objectsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> occurrenceIdSet = new HashSet<>();
			Set<String> nameIdSet = new HashSet<>();
			Set<String> sourceNumberSet = new HashSet<>();
			Set<String> oldNamesSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, occurrenceIdSet, "occurrenceFk");
				handleForeignKey(rs, nameIdSet, "oldNameFk");
				sourceNumberSet.add(CdmUtils.NzTrim(rs.getString("SourceNumber")));
				oldNamesSet.add(CdmUtils.NzTrim(rs.getString("oldName")));
				oldNamesSet.add(CdmUtils.NzTrim(rs.getString("nameCache")));
				oldNamesSet.add(CdmUtils.NzTrim(rs.getString("fullNameCache")));
			}

			sourceNumberSet.remove("");
			Set<String> referenceIdSet = handleSourceNumber(sourceNumberSet);
            oldNamesSet.remove("");
            Set<String> oldNameIdSet = handleRelatedOldNames(oldNamesSet);
            nameIdSet.addAll(oldNameIdSet);

			//occurrence map
			nameSpace = BerlinModelOccurrenceImport.NAMESPACE;
			idSet = occurrenceIdSet;
            Map<String, Distribution> occurrenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Distribution.class, idSet, nameSpace);
			result.put(nameSpace, occurrenceMap);

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet =nameIdSet;
            Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
            Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private Set<String> handleSourceNumber(Set<String> sourceNumberSet) {
		Map<String, Integer> sourceNumberReferenceIdMap = this.sourceNumberRefIdMap;
		Set<String> referenceIdSet = new HashSet<>();

		for(String sourceNumber : sourceNumberSet){
			Integer refId = sourceNumberReferenceIdMap.get(sourceNumber);
			referenceIdSet.add(String.valueOf(refId));
		}
		return referenceIdSet;
	}

    private Set<String> handleRelatedOldNames(Set<String> oldNamesSet) {
        Set<String> oldNameIdSet = new HashSet<>();

        try {
            for(String oldName : oldNamesSet){
                if (isNotBlank(oldName)){
                    Set<Integer> nameIds = nameCache2NameIdMap.get(oldName);
                    if (nameIds != null){
                        for (Integer nameId : nameIds){
                            oldNameIdSet.add(String.valueOf(nameId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in handleOldNames" + e.getMessage());
        }
        return oldNameIdSet;
    }

	private TaxonName getName(BerlinModelImportState state, String oldNameStr, Integer oldNameFk,
	        String oldNameFkFullCache, String oldNameFkCache,
	        Integer occSourceId, Distribution distribution) {
		if (oldNameStr == null && oldNameFk == null){
		    return null;
		}
		boolean includeMisapplications = state.getConfig().isIncludeMANsForOldNameCheck();

	    TaxonName taxonName = (TaxonName)state.getRelatedObject(BerlinModelTaxonNameImport.NAMESPACE, String.valueOf(oldNameFk));
		if (oldNameFk != null && taxonName == null){
		    //move down if occ source names are not loaded in name view
		    taxonName = handleOldFreetextNameOnly(state, oldNameFkFullCache, occSourceId, distribution);
		    if (taxonName == null){
		        taxonName = handleOldFreetextNameOnly(state, oldNameFkCache, occSourceId, distribution);
		    }
		    if (taxonName == null ){
		        logger.warn("WARN: OldNameFk "+oldNameFk+" exists but taxonName not found and also search by string not successful for occSource: " + occSourceId +"; Taxon: "+getTaxonStr(distribution));
		        oldNameStr = oldNameFkFullCache;
		    }
		}else if (taxonName != null){
            taxonName = checkSynonymy(state, oldNameFk, occSourceId, distribution, taxonName, includeMisapplications);
		}
		if (isNotBlank(oldNameStr) && oldNameStr != null){
		    if (taxonName == null){
		        return handleOldFreetextNameOnly(state, oldNameStr, occSourceId, distribution);
		    }else if (!oldNameStr.equals(taxonName.getNameCache())){
		        logger.info("INFO: Old name freetext and linked name nameCache are not equal: " + oldNameStr + "/" + taxonName.getNameCache() +"; Taxon: "+getTaxonStr(distribution) +  "; occSourceId: " +  occSourceId);
		        checkSynonymy(state, oldNameFk, occSourceId, distribution, taxonName, includeMisapplications);
	            return taxonName;
		    }else{
		        checkSynonymy(state, oldNameFk, occSourceId, distribution, taxonName, includeMisapplications);
	            return taxonName;
		    }
		}else{ //taxonName != null
		    if (taxonName != null){
		        checkSynonymy(state, oldNameFk, occSourceId, distribution, taxonName, includeMisapplications);
		    }
		    return taxonName;
		}
	}

    /**
     * @param state
     * @param oldName
     * @param occSourceId
     * @param distribution
     * @return
     */
    protected TaxonName handleOldFreetextNameOnly(BerlinModelImportState state, String oldName, Integer occSourceId,
            Distribution distribution) {
        Set<TaxonName> names = getOldNames(state, oldName);
        if (names.isEmpty()){
            if (getNameIds(oldName).isEmpty()){
                if (state.getConfig().isLogNotMatchingOldNames()){
                    logger.warn("No name found for freetext oldName '"+oldName+"'; occSourceId: " + occSourceId);
                }
            }else{
                if (state.getConfig().isLogMatchingNotExportedOldNames()){
                    logger.warn("Matching name exists in BM but not in CDM. OldName: " + oldName + "; Taxon: "+getTaxonStr(distribution)+"; occSourceId: " + occSourceId);
                }
            }
            return null;
        }else {
            TaxonName result = names.iterator().next();
            boolean checkOldNameIsSynonym = state.getConfig().isCheckOldNameIsSynonym();
            if (names.size()> 1){
                TaxonName synName = getFirstSynonymName(state, names, distribution, null, occSourceId, true);
                if (synName == null){
                    //TODO should we really use a name if not available in synonymy?
                    String message = "INFO: There is more than one matching oldName for '"+oldName+"' but none of them is a synonym of the accepted taxon '"+getTaxonStr(distribution)+"'.";
                    message += (checkOldNameIsSynonym ? "":"Take arbitrary one. ") + "OccSourceId: " + occSourceId;
                    logger.info(message);
                    return checkOldNameIsSynonym ? null : result;
                }else{
                    return synName;
                }
            }else{
                //names.size() = 1
                if (checkOldNameIsSynonym){
                    TaxonName synName = getFirstSynonymName(state, names, distribution, null, occSourceId, true);
                    if (synName == null){
                        if (state.getConfig().isCheckOldNameIsSynonym()){
                            logger.warn("There is a matching oldName for '"+oldName+"' but it is not a synonym/misapplication of the accepted taxon '"+getTaxonStr(distribution)+"'. OccSourceId: " + occSourceId);
                            return null;
                        }else{
                            return result;
                        }
                    }else if (!synName.equals(result)){
                        //TODO strange, how can this happen if it is the only matching?
                        logger.warn("There is a matching oldName for '"+oldName+"'("+result.getUuid()+") but another matching name "+synName.getUuid()+"exists in the synonymy of the accepted taxon '"+getTaxonStr(distribution)+"'. OccSourceId: " + occSourceId);
                        return synName;
                    }else{
                        return result;
                    }
                }else{
                    return result;
                }
            }
        }
    }

    /**
     * @param state
     * @param oldNameFk
     * @param occSourceId
     * @param distribution
     * @param taxonName
     */
    protected TaxonName checkSynonymy(BerlinModelImportState state, Integer oldNameFk, Integer occSourceId,
            Distribution distribution, TaxonName taxonName, boolean includeMisapplications) {

        if (!state.getConfig().isCheckOldNameIsSynonym()){
            return taxonName;
        }else{
            Set<TaxonName> names = new HashSet<>();
            names.add(taxonName);
            TaxonName synName = getFirstSynonymName(state, names, distribution, null, occSourceId, includeMisapplications);
            if (synName != null){
                return synName;  //same as taxonName?
            }else{
                boolean hasTaxon = !taxonName.getTaxonBases().isEmpty();
                String orphaned = hasTaxon ? "" : "Orphaned name: ";
                Set<TaxonName> existingNames = getOldNames(state, taxonName.getNameCache());
                existingNames.remove(taxonName);
                if (existingNames.isEmpty()){
                    logger.info("INFO:" + orphaned + "NameInSource (" + oldNameFk + " - " +taxonName.getTitleCache() + ") could not be found in synonymy. Similar name does not exist. Use the not in synonymy name. "+getTaxonStr(distribution)+". OccSourceId: " + occSourceId);
                    return taxonName;
                }else{
                    TaxonName existingSynonym = getFirstSynonymName(state, existingNames, distribution, null, occSourceId, false);
                    if (existingSynonym != null){
                        boolean isExact = CdmUtils.nullSafeEqual(existingSynonym.getTitleCache(),taxonName.getTitleCache());
                        String exact = isExact ? EXACT : "";
                        logger.info("INFO: " + exact + orphaned + "A similar name ("+existingSynonym.getUuid()+") was found in synonymy but is not the nameInSource. Use synonymie name (" + oldNameFk + " - " +taxonName.getTitleCache() + "); Taxon: "+getTaxonStr(distribution)+". OccSourceId: " + occSourceId);
                        return existingSynonym;
                    }else{
                        TaxonName existingMisapplication = getFirstMisapplication(state, existingNames, distribution, occSourceId);
                        if (existingMisapplication != null){
                            boolean isExact = CdmUtils.nullSafeEqual(existingMisapplication.getTitleCache(),taxonName.getTitleCache());
                            String exact = isExact ? EXACT : "";
                            logger.info("INFO: " + exact + orphaned + "A similar misapplied name ("+existingMisapplication.getUuid()+") can be found in misapplications but is not the nameInSource. Use synonymie name (" + oldNameFk + " - " +taxonName.getTitleCache() + "); Taxon: "+getTaxonStr(distribution)+". OccSourceId: " + occSourceId);
                            return existingMisapplication;
                        }else{
                            logger.info("INFO: NameInSource not found in synonymy. Similar names exist but also not in synonymy. Use name in source (" + oldNameFk + " - " +taxonName.getTitleCache() + "); Taxon: "+getTaxonStr(distribution)+". OccSourceId: " + occSourceId);
                            return taxonName;
                        }
                    }
                }
            }
        }
    }

	/**
     * @param state
     * @param names
	 * @param taxon
     * @param taxon
     * @return
     */
    private TaxonName getFirstSynonymName(BerlinModelImportState state, Set<TaxonName> names, Distribution distribution,
            Taxon taxon, Integer occSourceId, boolean includeMisapplications) {
        TaxonName result = null;
        taxon = (taxon == null) ? getTaxon(distribution): taxon;
        Set<Synonym> synonyms = taxon.getSynonyms();
        Set<TaxonName> synonymNames = new HashSet<>();

        //taxon, orthvars, synonyms and their orthvars
        synonymNames.add(taxon.getName());
        synonymNames.addAll(getOrthographicVariants(taxon));

        for (Synonym synonym : synonyms){
            synonymNames.add(synonym.getName());
            synonymNames.addAll(getOrthographicVariants(synonym));
        }
        for (TaxonName name : names){
            if (synonymNames.contains(name)){
                if (result != null){
                    logger.warn("There is more than 1 matching synonym/taxon for " + name.getNameCache() + "; occSourceId: " + occSourceId);
                }
                result = name;
            }
        }

        //parent
        if (result == null){
            if (taxon.getName().isInfraSpecific()){
                if (!taxon.getTaxonNodes().isEmpty()){
                    TaxonNode parent = taxon.getTaxonNodes().iterator().next().getParent();
                    if (parent != null && parent.getTaxon() != null){
                        Set<TaxonName> parentNames = new HashSet<>();
                        TaxonName parentName = parent.getTaxon().getName();
                        parentNames.add(parentName);
                        parentNames.addAll(getOrthographicVariants(parent.getTaxon()));

                        for (TaxonName name : names){
                            if (parentNames.contains(name)){
                                if (result != null){
                                    logger.warn("There is more than 1 matching parent for " + name.getNameCache() + "; occSourceId: " + occSourceId);
                                }
                                result = name;
                            }
                        }
                        if (result == null){
                            TaxonName parentSyn = getFirstSynonymName(state, names, distribution, parent.getTaxon(), occSourceId, includeMisapplications);
                            if (parentSyn != null){
                                result = parentSyn;
                            }
                        }
                    }
                }
            }
        }

        //child
        if (result == null){
            if (taxon.getName().isSpecies() || taxon.getName().isSupraSpecific()){
                if (!taxon.getTaxonNodes().isEmpty()){
                    List<TaxonNode> children = taxon.getTaxonNodes().iterator().next().getChildNodes();
                    Set<TaxonName> childNames = new HashSet<>();
                    for (TaxonNode child : children){
                        childNames.add(child.getTaxon().getName());
                        childNames.addAll(getOrthographicVariants(child.getTaxon()));
                    }
                    for (TaxonName name : names){
                        if (childNames.contains(name)){
                            if (result != null){
                                logger.warn("There is more than 1 matching child for " + name.getNameCache() + "; occSourceId: " + occSourceId);
                            }
                            result = name;
                        }
                    }
                }
            }
        }

        if (result == null && includeMisapplications){
            result = getFirstMisapplication(state, names, distribution, occSourceId);
        }

        return result;
    }

    /**
     * @param state
     * @param names
     * @param taxon
     * @return
     */
    private TaxonName getFirstMisapplication(BerlinModelImportState state, Set<TaxonName> names, Distribution distribution, Integer occSourceId) {
        TaxonName result = null;
        Taxon taxon = getTaxon(distribution);

        //MAN
        Set<Taxon> misappliedTaxa = taxon.getMisappliedNames(true);
        misappliedTaxa.addAll(taxon.getInvalidDesignations());
        Set<TaxonName> misappliedNames = new HashSet<>();
        for (Taxon misTaxon : misappliedTaxa){
            misappliedNames.add(misTaxon.getName());
            misappliedNames.addAll(getOrthographicVariants(misTaxon));
        }

        for (TaxonName name : names){
            if (misappliedNames.contains(name)){
                if (result != null){
                    logger.info("INFO: There is more than 1 matching misapplied name or invalid designation for " + name.getNameCache() + ". Take arbitrary one.; occSourceId: " + occSourceId);
                }
                result = name;
            }
        }
        return result;
    }

    /**
     * @param taxon
     * @return
     */
    protected Set<TaxonName> getOrthographicVariants(TaxonBase<?> taxonBase) {
        Set<TaxonName> result = taxonBase.getName().getRelatedNames(Direction.relatedTo, NameRelationshipType.ORTHOGRAPHIC_VARIANT());
        result.addAll(taxonBase.getName().getRelatedNames(Direction.relatedTo, NameRelationshipType.MISSPELLING()));
        result.addAll(taxonBase.getName().getRelatedNames(Direction.relatedTo, NameRelationshipType.ORIGINAL_SPELLING()));
        return result;
    }

    /**
     * @param distribution
     * @return
     */
    protected String getTaxonStr(Distribution distribution) {
        Taxon taxon = CdmBase.deproxy(distribution.getInDescription(), TaxonDescription.class).getTaxon();
        String areaStr = distribution.getArea().getIdInVocabulary();
        return areaStr + ": " + taxon.getName().getTitleCache();
    }

    protected Taxon getTaxon(Distribution distribution) {
        Taxon taxon = CdmBase.deproxy(distribution.getInDescription(), TaxonDescription.class).getTaxon();
        return taxon;
    }

    /**
     * returns all names in DB matching the given name string.
     * The name needs to be loaded via related objects previously.
     */
    private Set<TaxonName> getOldNames(BerlinModelImportState state, String nameStr) {
        Set<TaxonName> names = new HashSet<>();
        Set<Integer> nameIds = getNameIds(nameStr);
        for (Integer id : nameIds){
            TaxonName name = (TaxonName)state.getRelatedObject(BerlinModelTaxonNameImport.NAMESPACE, String.valueOf(id));
            if (name != null){
                names.add(name);
            }else{
//                logger.warn("Name for existing id "+id+" not found in related objects: " + nameStr);
            }
        }
        return names;
    }

    /**
     * @param oldName
     * @return
     */
    private Set<Integer> getNameIds(String oldName) {
        Set<Integer> result = nameCache2NameIdMap.get(oldName);
        return result == null ? new HashSet<>(): result;
    }

    /**
	 * Creates a map which maps source numbers on references
	 * @param state
	 * @return
     * @throws SQLException
	 */
	private Map<String, Integer> makeSourceNumberReferenceIdMap(BerlinModelImportState state) throws SQLException {
		Map<String, Integer> result = new HashMap<>();

		Source source = state.getConfig().getSource();
		String strQuery = " SELECT RefId, IdInSource " +
						  " FROM Reference " +
						  " WHERE (IdInSource IS NOT NULL) AND (IdInSource NOT LIKE '') ";

		ResultSet rs = source.getResultSet(strQuery) ;
		while (rs.next()){
			int refId = rs.getInt("RefId");
			String idInSource = rs.getString("IdInSource");
			if (idInSource != null){
				String[] singleSources = idInSource.split("\\|");
				for (String singleSource : singleSources){
					singleSource = singleSource.trim();
					result.put(singleSource, refId);
				}
			}
		}
		return result;
	}

	   /**
     * Creates a map which maps nameCaches to nameIDs numbers on references
     * @param state
     * @return
     * @throws SQLException
     */
    private Map<String, Set<Integer>> makeNameCache2NameIdMap(BerlinModelImportState state) throws SQLException {
        Map<String, Set<Integer>> result = new HashMap<>();
        try {

            Source source = state.getConfig().getSource();
            String strQuery = " SELECT NameId, nameCache " +
                              " FROM Name " +
                              " WHERE (nameCache IS NOT NULL) AND (nameCache NOT LIKE '') ";

            ResultSet rs = source.getResultSet(strQuery) ;
            while (rs.next()){
                int nameId = rs.getInt("NameId");
                String nameCache = rs.getString("nameCache");
                if (isNotBlank(nameCache)){
                    nameCache = nameCache.trim();
                    Set<Integer> set = result.get(nameCache);
                    if (set == null){
                        set = new HashSet<>();
                        result.put(nameCache, set);
                    }
                    set.add(nameId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in makeNameCache2NameIdMap" + e.getMessage());
        }
        return result;
    }

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelOccurrenceSourceImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		if (! state.getConfig().isDoOccurrenceSources()){
			return true;
		}else{
			if (!this.checkSqlServerColumnExists(state.getConfig().getSource(), "emOccurrenceSource", "OccurrenceSourceId")){
				logger.error("emOccurrenceSource table or emOccurrenceSourceId does not exist. Must ignore occurrence import");
				return true;
			}else{
				return false;
			}
		}
	}

}
