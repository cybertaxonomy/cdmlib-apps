/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_HAS_SAME_TYPE_AS;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_ALTERNATIVE_NAME_FOR;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_BASIONYM_FOR;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_CONSERVED_TYPE_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_FEMALE_PARENT_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_FIRST_PARENT_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_LATER_HOMONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_LATER_VALIDATION_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_LECTOTYPE_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_MALE_PARENT_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_ORTHOGRAPHIC_VARIANT_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_REJECTED_TYPE_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_REPLACED_SYNONYM_FOR;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_SECOND_PARENT_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_TREATED_AS_LATER_HOMONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_TYPE_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_IS_VALIDATION_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.NAME_REL_TYPE_NOT_DESIGNATED;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonNameRelationImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.HybridRelationshipType;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonNameRelationImport extends BerlinModelImportBase {

    private static final long serialVersionUID = 1197601822023101796L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "name relations";
	private static final String dbTableName = "RelName";


	public BerlinModelTaxonNameRelationImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String nameIdTable = state.getConfig().getNameIdTable();
		String result = super.getIdQuery(state);
	    if (isNotBlank(nameIdTable)){
			if (state.getConfig().isEuroMed()){
			    result += " WHERE nameFk1 IN (SELECT NameId FROM %s) AND RelNameQualifierFk IN (2, 4, 5, 13, 14, 15, 17, 18, 37, 62) OR ";
			    result += "       nameFk2 IN (SELECT NameId FROM %s)  ";
			    //the first part is only to check if there are relations that we have maybe missed.
			    //2 is unclear, 17 should be in both, 62 links names to itself
			    result = String.format(result, nameIdTable, nameIdTable);
			}else{
			    result += " WHERE nameFk1 IN (SELECT NameId FROM %s) OR ";
			    result += "       nameFk2 IN (SELECT NameId FROM %s) ";
			    result = String.format(result, nameIdTable, nameIdTable );
			}
		}else{
			//
		}
	    return result;
	}


	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
					" SELECT RelName.*, FromName.nameId as name1Id, ToName.nameId as name2Id, RefDetail.Details " +
					" FROM Name as FromName INNER JOIN " +
                      	" RelName ON FromName.NameId = RelName.NameFk1 INNER JOIN " +
                      	" Name AS ToName ON RelName.NameFk2 = ToName.NameId LEFT OUTER JOIN "+
                      	" RefDetail ON RelName.RefDetailFK = RefDetail.RefDetailId " +
            " WHERE (RelNameId IN ("+ID_LIST_TOKEN +"))";
		return strQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		BerlinModelImportConfigurator config = state.getConfig();
		Set<TaxonName> nameToSave = new HashSet<>();
		Map<String, TaxonName> nameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try {

			int i = 0;
			//for each name relation
			while (rs.next()){

				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("RelName handled: " + (i-1));}

				int relNameId = rs.getInt("RelNameId");
				int name1Id = rs.getInt("name1Id");
				int name2Id = rs.getInt("name2Id");
				Object relRefFkObj = rs.getObject("refFk");
				String details = rs.getString("details");
				int relQualifierFk = rs.getInt("relNameQualifierFk");
				String notes = rs.getString("notes");

				TaxonName nameFrom = nameMap.get(String.valueOf(name1Id));
				TaxonName nameTo = nameMap.get(String.valueOf(name2Id));


				Reference citation = null;
				if (relRefFkObj != null){
					String relRefFk = String.valueOf(relRefFkObj);
					//get nomRef
					citation = refMap.get(relRefFk);
				}

				//TODO (preliminaryFlag = true testen
				String microcitation = details;
				String rule = null;

				if (nameFrom != null && nameTo != null){
					success = handleNameRelationship(success, config, name1Id, name2Id,	relQualifierFk,
							notes, nameFrom, nameTo, citation, microcitation, rule, relNameId);

					if (! nameFrom.isProtectedTitleCache()){
						nameFrom.setTitleCache(null);
						nameFrom.getTitleCache();
					}
					if (! nameTo.isProtectedTitleCache()){
						nameTo.setTitleCache(null);
						nameTo.getTitleCache();
					}
					nameToSave.add(nameFrom);

					//TODO
					//ID
					//etc.
				}else{
					//TODO
					if (nameFrom == null){
						if ( ! (config.isUseEmAreaVocabulary() && relNameId == 28159 )) {
							logger.warn("from TaxonName " + name1Id + "  for RelName (" + relNameId + " , type: " + relQualifierFk  + " , toName: " + name2Id+ ") does not exist in store. ToName is: " + (nameTo == null ? "" : nameTo.getTitleCache()));
						}
					}
					if (nameTo == null){
						logger.warn("to TaxonName " + name2Id + " for RelName (" + relNameId + " , type: " + relQualifierFk  + " , fromName: " + name1Id + ") does not exist in store. FromName is: "  + (nameFrom == null ? "" : nameFrom.getTitleCache()));
					}
					success = false;
				}
			}


			partitioner.startDoSave();
			getNameService().save(nameToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	/**
	 * @param success
	 * @param config
	 * @param name1Id
	 * @param name2Id
	 * @param relQualifierFk
	 * @param notes
	 * @param nameFrom
	 * @param nameTo
	 * @param citation
	 * @param microcitation
	 * @param rule
	 * @param relNameId
	 * @return
	 */
	private boolean handleNameRelationship(boolean success,
				BerlinModelImportConfigurator config, int name1Id, int name2Id,
				int relQualifierFk, String notes, TaxonName nameFrom,
				TaxonName nameTo, Reference citation,
				String microcitation, String rule, int relNameId) {
		AnnotatableEntity nameRelationship = null;
		if (relQualifierFk == NAME_REL_IS_BASIONYM_FOR){
			nameRelationship = nameTo.addBasionym(nameFrom, citation, microcitation, rule, null);
		}else if (relQualifierFk == NAME_REL_IS_LATER_HOMONYM_OF){
			nameRelationship = nameFrom.addRelationshipToName(nameTo, NameRelationshipType.LATER_HOMONYM(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_TREATED_AS_LATER_HOMONYM_OF){
			nameRelationship = nameFrom.addRelationshipToName(nameTo, NameRelationshipType.LATER_HOMONYM(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_REPLACED_SYNONYM_FOR){
			nameRelationship = nameFrom.addRelationshipToName(nameTo, NameRelationshipType.REPLACED_SYNONYM(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_HAS_SAME_TYPE_AS){
			nameTo.getHomotypicalGroup().merge(nameFrom.getHomotypicalGroup());
		}else if (relQualifierFk == NAME_REL_IS_VALIDATION_OF){
			nameRelationship = nameTo.addRelationshipToName(nameFrom, NameRelationshipType.VALIDATED_BY_NAME(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_LATER_VALIDATION_OF){
			nameRelationship = nameTo.addRelationshipToName(nameFrom, NameRelationshipType.LATER_VALIDATED_BY_NAME(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_TYPE_OF || relQualifierFk == NAME_REL_IS_REJECTED_TYPE_OF ||  relQualifierFk == NAME_REL_IS_CONSERVED_TYPE_OF || relQualifierFk == NAME_REL_IS_LECTOTYPE_OF || relQualifierFk == NAME_REL_TYPE_NOT_DESIGNATED ){
			boolean isRejectedType = (relQualifierFk == NAME_REL_IS_REJECTED_TYPE_OF);
			boolean isConservedType = (relQualifierFk == NAME_REL_IS_CONSERVED_TYPE_OF);
			boolean isLectoType = (relQualifierFk == NAME_REL_IS_LECTOTYPE_OF);
			boolean isNotDesignated = (relQualifierFk == NAME_REL_TYPE_NOT_DESIGNATED);

			NameTypeDesignationStatus status = null;
			String originalNameString = null;
			//TODO addToAllNames true or false?
			boolean addToAllNames = false;
			if (config.getNameTypeDesignationStatusMethod() != null){
				Method method = config.getNameTypeDesignationStatusMethod();
				method.setAccessible(true);
				try {
					status = (NameTypeDesignationStatus)method.invoke(null, notes);
					nameRelationship = nameTo.addNameTypeDesignation(nameFrom, citation, microcitation, originalNameString, status, addToAllNames);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}else{
				if (isLectoType){
					status = NameTypeDesignationStatus.LECTOTYPE();
				}
				if (isNotDesignated && nameTo == nameFrom){
				    nameFrom = null;  //E+M case
				}

				nameRelationship = nameTo.addNameTypeDesignation(nameFrom, citation, microcitation, originalNameString, status, isRejectedType, isConservedType, isNotDesignated, addToAllNames);
			}

		}else if (relQualifierFk == NAME_REL_IS_ORTHOGRAPHIC_VARIANT_OF){
			nameRelationship = nameFrom.addRelationshipToName(nameTo, NameRelationshipType.ORTHOGRAPHIC_VARIANT(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_ALTERNATIVE_NAME_FOR){
			nameRelationship = nameFrom.addRelationshipToName(nameTo, NameRelationshipType.ALTERNATIVE_NAME(), citation, microcitation, rule, null) ;
		}else if (relQualifierFk == NAME_REL_IS_FIRST_PARENT_OF || relQualifierFk == NAME_REL_IS_SECOND_PARENT_OF || relQualifierFk == NAME_REL_IS_FEMALE_PARENT_OF || relQualifierFk == NAME_REL_IS_MALE_PARENT_OF){
			//HybridRelationships
			try {
				HybridRelationshipType hybridRelType = BerlinModelTransformer.relNameId2HybridRel(relQualifierFk);
				IBotanicalName parent = nameFrom;
				IBotanicalName child = nameTo;

				nameRelationship = parent.addHybridChild(child, hybridRelType, rule);

			} catch (UnknownCdmTypeException e) {
				logger.warn(e);
				success = false;
			}
		}else {
			//TODO
			Method method = config.getNamerelationshipTypeMethod();
			if (method != null){
				try {
					method.invoke(null, relQualifierFk, nameTo, nameFrom);
				} catch (Exception e) {
					logger.error(e.getMessage());
					logger.warn("NameRelationship could not be imported");
					success = false;
				}
			}else{
				logger.warn("NameRelationShipType " + relQualifierFk + " not yet implemented");
				success = false;
			}
		}
		Annotation annotation = doNotes(nameRelationship, notes);
		if (config.isEuroMed() && annotation != null){
		    if (relQualifierFk == NAME_REL_IS_BASIONYM_FOR){
		        annotation.setAnnotationType(AnnotationType.TECHNICAL());
		    }else if ((relQualifierFk == NAME_REL_IS_LECTOTYPE_OF) && !notes.contains("designated")){
		        annotation.setAnnotationType(AnnotationType.TECHNICAL());
		    }else{
		        logger.warn("Annotation type not defined for name relationship " + relNameId);
		    }
		}else if (config.isMoose() && annotation != null){
		    //do nothing (there are only 2 relations with notes (12858,13006) and very unclear what they mean
		}else if (annotation != null){
            logger.warn("Annotation type not defined for name relationship " + relNameId);
		}
		return success;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> refDetailIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "name1Id");
				handleForeignKey(rs, nameIdSet, "name2Id");
				handleForeignKey(rs, referenceIdSet, "RefFk");
				handleForeignKey(rs, refDetailIdSet, "RefDetailFk");
	}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

			//refDetail map
			nameSpace = BerlinModelRefDetailImport.REFDETAIL_NAMESPACE;
			idSet = refDetailIdSet;
			Map<String, Reference> refDetailMap= getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, refDetailMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
				}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonNameRelationImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoRelNames();
		}
}
