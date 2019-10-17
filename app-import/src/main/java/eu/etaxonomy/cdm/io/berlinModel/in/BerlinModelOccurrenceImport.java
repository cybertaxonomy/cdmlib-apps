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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelOccurrenceImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.TdwgAreaProvider;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelOccurrenceImport  extends BerlinModelImportBase {

    private static final long serialVersionUID = -7918122767284077183L;

    private static final Logger logger = Logger.getLogger(BerlinModelOccurrenceImport.class);

	public static final String NAMESPACE = "Occurrence";

	private static int modCount = 5000;
	private static final String pluralString = "occurrences";
	private static final String dbTableName = "emOccurrence";  //??

	public BerlinModelOccurrenceImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT occurrenceId FROM " + getTableName();
		if (StringUtils.isNotBlank(state.getConfig().getOccurrenceFilter())){
			result += " WHERE " +  state.getConfig().getOccurrenceFilter();
		}
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String emCode = config.isIncludesAreaEmCode()? ", ar.EMCode" : "";
			String strQuery =   //DISTINCT because otherwise emOccurrenceSource creates multiple records for a single distribution
                " SELECT DISTINCT pt.RIdentifier AS taxonId, occ.OccurrenceId, occ.Native, occ.Introduced, " +
            		" occ.Cultivated, occ.StatusUnknown, occ.WorldDistCompl, occ.Notes occNotes, " +
            		" sumcat.emOccurSumCatId, sumcat.Short, sumcat.Description, " +
                	" sumcat.OutputCode, ar.AreaId, ar.TDWGCode, "
                	+ " occ.Created_When , occ.Updated_When, occ.Created_Who , occ.Updated_Who, occ.notes " + emCode +
                " FROM emOccurrence occ " +
                	" INNER JOIN emArea ar ON occ.AreaFk = ar.AreaId " +
                	" INNER JOIN PTaxon pt ON occ.PTNameFk = pt.PTNameFk AND occ.PTRefFk = pt.PTRefFk " +
                	" LEFT OUTER JOIN emOccurSumCat sumcat ON occ.SummaryStatus = sumcat.emOccurSumCatId " +
                	" LEFT OUTER JOIN emOccurrenceSource ocs ON occ.OccurrenceId = ocs.OccurrenceFk " +
                " WHERE (occ.OccurrenceId IN (" + ID_LIST_TOKEN + ")  )" +
                " ORDER BY pt.RIdentifier";
		return strQuery;
	}


	@Override
	public void doInvoke(BerlinModelImportState state) {
		super.doInvoke(state);
	}

    private NamedArea getAreaByAreaId(int areaId) {
        NamedArea result = null;
        String areaIdStr = String.valueOf(areaId);
        OrderedTermVocabulary<NamedArea> voc = getAreaVoc();
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


	private String nullSafeTrim(String string) {
		if (string == null){
			return null;
		}else{
			return string.trim();
		}
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true;
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();

		@SuppressWarnings("unchecked")
        Map<String, TaxonBase<?>> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {
			//map to store the mapping of duplicate berlin model occurrences to their real distributions
			//duplicated may occur due to area mappings from BM areas to TDWG areas
			Map<Integer, String> duplicateMap = new HashMap<>();
			int oldTaxonId = -1;
			TaxonDescription oldDescription = null;
			int i = 0;
			int countDescriptions = 0;
			int countDistributions = 0;
			int countDuplicates = 0;
			//for each reference
            while (rs.next()){

            	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("Facts handled: " + (i-1));}

                int occurrenceId = rs.getInt("OccurrenceId");
                int newTaxonId = rs.getInt("taxonId");
                String notes = nullSafeTrim(rs.getString("occNotes"));

                Integer emStatusId = nullSafeInt(rs, "emOccurSumCatId");

                try {
                    //area(s)
                    List<NamedArea> areas = makeAreaList(state, partitioner, rs, occurrenceId);
                    if (areas.size() != 1){
                        logger.warn("Exactly 1 area expected but was " + areas.size() + ". OccId: " + occurrenceId);
                        if (areas.isEmpty()){
                            continue;
                        }
                    }

                    //status
                	PresenceAbsenceTerm status = null;
                	String alternativeStatusString = null;
					if (emStatusId != null){
						status = BerlinModelTransformer.occStatus2PresenceAbsence(emStatusId);
						if (state.getConfig().isEuroMed() && emStatusId == 250){
						    String introduced = nullSafeTrim(rs.getString("Introduced"));
						    if (emStatusId == 250 && "I(P)".equals(introduced)){
						        status = PresenceAbsenceTerm.INTRODUCED_UNCERTAIN_DEGREE_OF_NATURALISATION();
						    }
						}
					}else{
						//EM
					    if (state.getConfig().isEuroMed() && areas.get(0).getUuid().equals(BerlinModelTransformer.uuidEM)){
						    String complete = rs.getString("WorldDistCompl");
						    if (complete == null){
						        //FIXME
                                status = PresenceAbsenceTerm.ENDEMISM_UNKNOWN();
                                alternativeStatusString = getStatusAnnotation(rs);
						    }else if (complete.equals("C")){
                                status = PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();
                                logger.warn("EmStatusId undefined though WorldDistCompl is 'C'. This is an unexpected state. OccID: " + occurrenceId);
						    }else if (complete.equals("I")){
						        status = PresenceAbsenceTerm.NOT_ENDEMIC_FOR_THE_RELEVANT_AREA();
                            }else{
                                status = PresenceAbsenceTerm.ENDEMISM_UNKNOWN();
                                alternativeStatusString = getStatusAnnotation(rs);
                            }
						}else{ //other areas
						    alternativeStatusString = getStatusAnnotation(rs);
						    status = getPresenceTerm(state, BerlinModelTransformer.uuidStatusUndefined, "Undefined", "Undefined status as status was not computed in Berlin Model", "none", false, null);
						}
					}

					Reference sourceRef = state.getTransactionalSourceReference();


                    //create description(elements)
                    TaxonDescription taxonDescription = getTaxonDescription(newTaxonId, oldTaxonId, oldDescription, taxonMap, occurrenceId, sourceRef);
                    for (NamedArea area : areas){
                    	Distribution distribution = Distribution.NewInstance(area, status);
                    	boolean excludeNotes = true;
                    	doCreatedUpdatedNotes(state, distribution, rs, false, excludeNotes);
                        if (StringUtils.isNotBlank(alternativeStatusString)){
                            AnnotationType type = getAnnotationType(state, BerlinModelTransformer.uuidAnnoTypeDistributionStatus, "Original distribution status", "Original distribution status", null, null);
                            Annotation annotation = Annotation.NewInstance(alternativeStatusString, type, null);
                            distribution.addAnnotation(annotation);
                        }

//                      distribution.setCitation(sourceRef);
                        if (taxonDescription != null) {
                        	Distribution duplicate = checkIsNoDuplicate(taxonDescription, distribution, duplicateMap , occurrenceId);
                            if (duplicate == null){
                            	taxonDescription.addElement(distribution);
	                            distribution.addImportSource(String.valueOf(occurrenceId), NAMESPACE, state.getTransactionalSourceReference(), null);
	                        	countDistributions++;
	                            if (taxonDescription != oldDescription){
	                            	taxaToSave.add(taxonDescription.getTaxon());
	                                oldDescription = taxonDescription;
	                                countDescriptions++;
	                            }
                            }else{
                            	countDuplicates++;
                            	distribution = duplicate;
                            	duplicate.addImportSource(String.valueOf(occurrenceId), NAMESPACE, state.getTransactionalSourceReference(), null);
                            	logger.info("Distribution is duplicate");	                           }
                        } else {
                        	logger.warn("Distribution " + area.getLabel() + " ignored. OccurrenceId = " + occurrenceId);
	                       	success = false;
	                    }
                        //notes
                        if (isNotBlank(notes)){
                        	Annotation annotation = Annotation.NewInstance(notes, Language.DEFAULT());
                        	distribution.addAnnotation(annotation);
                        }
                    }
                } catch (UnknownCdmTypeException e) {
                     logger.error("Unknown presenceAbsence status id: " + emStatusId);
                	e.printStackTrace();
                     success = false;
                }
            }

            logger.info("Distributions: " + countDistributions + ", Descriptions: " + countDescriptions );
			logger.info("Duplicate occurrences: "  + (countDuplicates));

			logger.info("Taxa to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

    /**
     * @param rs
     * @return
     * @throws SQLException
     */
    protected String getStatusAnnotation(ResultSet rs) throws SQLException {
        String alternativeStatusString;
        String[] stringArray = new String[]{"Native: " + rs.getString("Native"), "Introduced: "+ rs.getString("Introduced"),
                "Cultivated: " + rs.getString("Cultivated"), "StatusUnknown: " + rs.getString("StatusUnknown"),
                "WorldDistCompl: " + rs.getString("WorldDistCompl")};
        alternativeStatusString = CdmUtils.concat("; ", stringArray);
        return alternativeStatusString;
    }

	//Create area list
	private List<NamedArea> makeAreaList(BerlinModelImportState state,
	        @SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
	        ResultSet rs, int occurrenceId) throws SQLException {

	    List<NamedArea> areas = new ArrayList<>();

		if (state.getConfig().isUseEmAreaVocabulary()){
		    Integer areaId = rs.getInt("AreaId");
			NamedArea area = getAreaByAreaId(areaId);
			if (area == null){
			    logger.warn("Area for areaId " + areaId + " not found.");
			}
			areas.add(area);
		}else{
	        String tdwgCodeString = rs.getString("TDWGCode");
	        String emCodeString = state.getConfig().isIncludesAreaEmCode() ? rs.getString("EMCode") : null;

			if (tdwgCodeString != null){

				String[] tdwgCodes = new String[]{tdwgCodeString};
				if (state.getConfig().isSplitTdwgCodes()){
					tdwgCodes = tdwgCodeString.split(";");
				}

				for (String tdwgCode : tdwgCodes){
					NamedArea area = TdwgAreaProvider.getAreaByTdwgAbbreviation(tdwgCode.trim());
			    	if (area == null){
			    		area = getOtherAreas(state, emCodeString, tdwgCodeString);
			    	}
			    	if (area != null){
			    		areas.add(area);
			    	}
				}
			 }

			 if (areas.size()== 0){
				 NamedArea area = getOtherAreas(state, emCodeString, tdwgCodeString);
				 if (area != null){
			         areas.add(area);
			   }
			 }
			 if (areas.size() == 0){
				 String areaId = rs.getString("AreaId");
				 logger.warn("No areas defined for occurrence " + occurrenceId + ". EMCode: " + CdmUtils.Nz(emCodeString).trim() + ". AreaId: " + areaId );
			 }
		}
		return areas;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

		try{
		    Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
			Set<String> taxonIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
			}

			//taxon map
			String nameSpace = BerlinModelTaxonImport.NAMESPACE;
			Set<String> idSet = taxonIdSet;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}


	/**
     * Tests if a distribution with the same tdwgArea and the same status already exists in the description.
     * If so the old distribution is returned
     * @return false, if duplicate exists. True otherwise.
     */
    private Distribution checkIsNoDuplicate(TaxonDescription description, Distribution distribution, Map<Integer, String> duplicateMap, Integer bmDistributionId){
    	for (DescriptionElementBase descElBase : description.getElements()){
    		if (descElBase.isInstanceOf(Distribution.class)){
    			Distribution oldDistr = HibernateProxyHelper.deproxy(descElBase, Distribution.class);
    			NamedArea oldArea = oldDistr.getArea();
    			if (oldArea != null && oldArea.equals(distribution.getArea())){
    				PresenceAbsenceTerm oldStatus = oldDistr.getStatus();
    				if (oldStatus != null && oldStatus.equals(distribution.getStatus())){
    					duplicateMap.put(bmDistributionId, oldDistr.getSources().iterator().next().getIdInSource());
    					return oldDistr;
    				}
    			}
    		}
    	}
    	return null;
    }

	/**
	 * Use same TaxonDescription if two records belong to the same taxon
	 * @param newTaxonId
	 * @param oldTaxonId
	 * @param oldDescription
	 * @param taxonMap
	 * @return
	 */
	private TaxonDescription getTaxonDescription(int newTaxonId, int oldTaxonId, TaxonDescription oldDescription, Map<String, TaxonBase<?>> taxonMap, int occurrenceId, Reference sourceSec){
		TaxonDescription result = null;
		if (oldDescription == null || newTaxonId != oldTaxonId){
			TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(newTaxonId));
			//TODO for testing
			//TaxonBase taxonBase = Taxon.NewInstance(TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES()), null);
			Taxon taxon;
			if ( taxonBase instanceof Taxon ) {
				taxon = (Taxon) taxonBase;
			} else if (taxonBase != null) {
				logger.warn("TaxonBase for Occurrence " + occurrenceId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
				return null;
			} else {
				logger.warn("TaxonBase for Occurrence " + occurrenceId + " is null.");
				return null;
			}
			Set<TaxonDescription> descriptionSet= taxon.getDescriptions();
			if (descriptionSet.size() > 0) {
				result = descriptionSet.iterator().next();
			}else{
				result = TaxonDescription.NewInstance();
				result.setTitleCache(sourceSec.getTitleCache(), true);
				taxon.addDescription(result);
			}
		}else{
			result = oldDescription;
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelOccurrenceImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		if (! state.getConfig().isDoOccurrence()){
			return true;
		}else{
			if (!this.checkSqlServerColumnExists(state.getConfig().getSource(), "emOccurrence", "OccurrenceId")){
				logger.error("emOccurrence table or emOccurrenceId does not exist. Must ignore occurrence import");
				return true;
			}else{
				return false;
			}
		}
	}
}
