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
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelOccurrenceSourceImport  extends BerlinModelImportBase {

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
            " SELECT * " +
                " FROM emOccurrenceSource " +
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

		Set<DescriptionElementBase> objectsToSave = new HashSet<DescriptionElementBase>();
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
    					TaxonName taxonName;
						taxonName = TaxonName.castAndDeproxy(getName(state, oldName, oldNameFk, occurrenceSourceId, distribution));
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
		Class<?> cdmClass;
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
			}

			sourceNumberSet.remove("");
			Set<String> referenceIdSet = handleSourceNumber(sourceNumberSet);
            oldNamesSet.remove("");
            Set<String> oldNameIdSet = handleOldNames(oldNamesSet);
            nameIdSet.addAll(oldNameIdSet);

			//occurrence map
			nameSpace = BerlinModelOccurrenceImport.NAMESPACE;
			cdmClass = Distribution.class;
			idSet = occurrenceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Distribution> occurrenceMap = (Map<String, Distribution>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, occurrenceMap);

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			cdmClass = TaxonName.class;
			idSet =nameIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonName> nameMap = (Map<String, TaxonName>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
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

    private Set<String> handleOldNames(Set<String> oldNamesSet) {
        Set<String> oldNameIdSet = new HashSet<>();

        for(String oldName : oldNamesSet){
            if (isNotBlank(oldName)){
                Set<Integer> nameIds = nameCache2NameIdMap.get(oldName);
                for (Integer nameId : nameIds){
                    oldNameIdSet.add(String.valueOf(nameId));
                }
            }
        }
        return oldNameIdSet;
    }



	/**
	 * @param state
	 * @param oldName
	 * @param oldNameFk
	 * @return
	 */
	boolean isFirstTimeNoNameByService = true;
	private TaxonName getName(BerlinModelImportState state, String oldName, Integer oldNameFk, Integer occSourceId, Distribution distribution) {
		TaxonName taxonName = (TaxonName)state.getRelatedObject(BerlinModelTaxonNameImport.NAMESPACE, String.valueOf(oldNameFk));
		if (oldNameFk != null && taxonName == null){
		    logger.warn("OldNameFk "+oldNameFk+" exists but taxonName not found for occSource: " + occSourceId);
		}
		if (isNotBlank(oldName)){
		    if (taxonName == null){
		        if (isFirstTimeNoNameByService){
		            logger.warn("oldName not checked against names in BerlinModel. Just take it as a string");
		            isFirstTimeNoNameByService = false;
		        }
		        Set<TaxonName> names = getOldNames(state, oldName);
		        if (names.isEmpty()){
		            logger.warn("No name found for freetext oldName '"+oldName+"'; occSourceId: " + occSourceId);
		            //taxonName = nameParser.parseSimpleName(oldName);
		            return null;
		        }else {
		            if (names.size()> 1){
		                TaxonName synName = getFirstSynonymName(state, names, distribution, occSourceId);
		                if (synName == null){
		                    logger.warn("There is more than one matching oldName for '"+oldName+"' but none of them is a synonym of the accepted taxon. Take arbitrary one. OccSourceId: " + occSourceId);
		                    return names.iterator().next();
		                }else{
		                    return synName;
		                }
		            }else{
                        return names.iterator().next();
		            }
		        }
		    }else if (!oldName.equals(taxonName.getNameCache())){
		        logger.warn("Old name freetext and linked name nameCache are not equal: " + oldName + "/" + taxonName.getNameCache() + "; occSourceId: " +  occSourceId);
	            return taxonName;
		    }else{
		        return taxonName;
		    }
		}else{
		    return taxonName;
		}
	}

	/**
     * @param state
     * @param names
     * @param taxon
     * @return
     */
    private TaxonName getFirstSynonymName(BerlinModelImportState state, Set<TaxonName> names, Distribution distribution, Integer occSourceId) {
        Taxon taxon = CdmBase.deproxy(distribution.getInDescription(), TaxonDescription.class).getTaxon();
        Set<TaxonName> synonyms = taxon.getSynonymNames();
        TaxonName result = null;
        for (TaxonName name : names){
            if (synonyms.contains(name)){
                if (result != null){
                    logger.warn("There is more than 1 matching synonym for " + name.getNameCache() + "; occSourceId: " + occSourceId);
                }
                result = name;
            }
        }
        return result;
    }

    /**
     * @param state
     * @param oldName
     * @return
     */
    private Set<TaxonName> getOldNames(BerlinModelImportState state, String oldName) {
        Set<Integer> nameIds = nameCache2NameIdMap.get(oldName);
        Set<TaxonName> names = new HashSet<>(nameIds.size());
        for (Integer id : nameIds){
            TaxonName name = (TaxonName)state.getRelatedObject(BerlinModelTaxonNameImport.NAMESPACE, String.valueOf(id));
            names.add(name);
        }
        return names;
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
        return result;
    }



	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelOccurrenceSourceImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		if (! state.getConfig().isDoOccurrence()){
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
