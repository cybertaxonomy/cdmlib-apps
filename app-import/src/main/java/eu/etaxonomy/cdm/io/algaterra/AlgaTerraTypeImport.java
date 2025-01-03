/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.algaterra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraTypeImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelRefDetailImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.DefinedTerm;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class AlgaTerraTypeImport  extends AlgaTerraSpecimenImportBase {

    private static final long serialVersionUID = 32400936433601479L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "types";
	private static final String dbTableName = "TypeDesignation";  //??

	@Override
    protected String getLocalityString(){
		return "TypeLocality";
	}

	public AlgaTerraTypeImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT TypeDesignationId "
				+ " FROM TypeDesignation "
				+ " ORDER BY NameFk ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =

			" SELECT ts.*, ts.TypeSpecimenId as unitId, td.*, gz.ID as GazetteerId,  gz.L1Code, gz.L2Code, gz.L3Code, gz.L4Code, gz.ISOCountry, gz.Country, gz.subL4, ts.WaterBody, " +
               " ts.RefFk as tsRefFk, ts.RefDetailFk as tsRefDetailFk, ts.MaterialCategoryFK as tsMaterialCategoryFK, td.RefFk as tdRefFk, td.RefDetailFk as tdRefDetailFk, " +
               " RefDet.Details as tdRefDetails, " +
               " td.created_When as tdCreated_When, tsd.created_When as tsdCreated_When, td.updated_when as tdUpdated_when, " +
               " td.created_who as tdCreated_who, tsd.created_who as tsdCreated_who, td.updated_who tdUpdated_who,  " +
               " mc.* " +
            " FROM TypeSpecimenDesignation tsd  "
            	+ " LEFT OUTER JOIN TypeSpecimen AS ts ON tsd.TypeSpecimenFk = ts.TypeSpecimenId "
            	+ " FULL OUTER JOIN TypeDesignation td ON  td.TypeDesignationId = tsd.TypeDesignationFk "
            	+ " LEFT OUTER JOIN TDWGGazetteer gz ON ts.TDWGGazetteerFk = gz.ID "
            	+ " LEFT OUTER JOIN RefDetail refDet ON td.RefDetailFk = refDet.RefDetailId AND td.RefFk = refDet.RefFk "
            	+ " LEFT OUTER JOIN MaterialCategory mc ON mc.MaterialCategoryId = ts.MaterialCategoryFK "
		+ 	" WHERE (td.TypeDesignationId IN (" + ID_LIST_TOKEN + ")  )"
          + " ORDER BY NameFk "
            ;
		return strQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;

		//TODO check that no duplicate vocabularies will be created, also remove redundant code here
//		and in Specimen importer
		try {
			makeVocabulariesAndFeatures(state);
		} catch (SQLException e1) {
			logger.warn("Exception occurred when trying to create Type specimen vocabularies: " + e1.getMessage());
			e1.printStackTrace();
		}



		Set<TaxonName> namesToSave = new HashSet<>();

		@SuppressWarnings("unchecked")
        Map<String, TaxonName> taxonNameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, DerivedUnit> ecoFactMap = partitioner.getObjectMap(AlgaTerraSpecimenImportBase.ECO_FACT_FIELD_OBSERVATION_NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, DerivedUnit> typeSpecimenMap = partitioner.getObjectMap(TYPE_SPECIMEN_FIELD_OBSERVATION_NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refDetailMap = partitioner.getObjectMap(BerlinModelRefDetailImport.REFDETAIL_NAMESPACE);


		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("Type designations handled: " + (i-1));}

				int nameId = rs.getInt("nameFk");
				int typeSpecimenId = rs.getInt("TypeSpecimenId");
				int typeDesignationId = rs.getInt("TypeDesignationId");
				Integer typeStatusFk =  nullSafeInt(rs, "typeStatusFk");
				Integer ecoFactId = nullSafeInt(rs, "ecoFactFk");
				Integer tdRefFk = nullSafeInt(rs, "tdRefFk");
				Integer tdRefDetailFk = nullSafeInt(rs, "tdRefDetailFk");
				String tdRefDetails = rs.getString("tdRefDetails");
				Boolean restrictedFlag = nullSafeBoolean(rs, "RestrictedFlag");

				String typeSpecimenPhrase = rs.getString("TypeSpecimenPhrase");
				Integer tsMaterialCategoryFK = nullSafeInt(rs, "MaterialCategoryFK");

				boolean isIcon = typeSpecimenPhrase != null && typeSpecimenPhrase.toLowerCase().startsWith("\u005bicon");

				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					//facade
					SpecimenOrObservationType type = SpecimenOrObservationType.PreservedSpecimen;
					if (isIcon){
						//TODO handle images correctly for these specimen
						type = SpecimenOrObservationType.StillImage;
					}else if (typeStatusFk != null && typeStatusFk.equals(39)){
						type =  SpecimenOrObservationType.LivingSpecimen;
					}else if (tsMaterialCategoryFK != null && tsMaterialCategoryFK.equals(4)){
						type = SpecimenOrObservationType.Fossil;
					}


					if (tsMaterialCategoryFK != null && typeStatusFk != null &&
							( typeStatusFk.equals(39) && !tsMaterialCategoryFK.equals(14) || ! typeStatusFk.equals(39) && tsMaterialCategoryFK.equals(14) )){
						logger.warn("Living Specimen type status should be 39 and materialCategoryFk 14 but one of them wasn't");
					}


					DerivedUnitFacade facade = getDerivedUnit(state, typeSpecimenId, typeSpecimenMap, type, ecoFactMap, ecoFactId, sourceRef);

					//field observation
					handleFieldObservationSpecimen(rs, facade, state, partitioner);

//					handleTypeSpecimenSpecificFieldObservation(rs,facade, state);

					//TODO divide like in EcoFact (if necessary)
					handleTypeSpecimenSpecificSpecimen(rs,facade, state, refMap, typeSpecimenId);

					handleFirstDerivedSpecimen(rs, facade, state, partitioner);


					//Designation
					TaxonName name = getTaxonName(state, taxonNameMap, nameId);
					SpecimenTypeDesignation designation = SpecimenTypeDesignation.NewInstance();
					SpecimenTypeDesignationStatus status = getSpecimenTypeDesignationStatusByKey(typeStatusFk);
					if (typeStatusFk != null && typeStatusFk.equals(39)){
						designation.addAnnotation(Annotation.NewInstance("Type status: Authentic strain", AnnotationType.EDITORIAL(), Language.DEFAULT()));
					}

					designation.setTypeSpecimen(facade.innerDerivedUnit());
					designation.setTypeStatus(status);
					if (tdRefFk != null){
						Reference typeDesigRef = getReferenceFromMaps(refDetailMap, refMap, String.valueOf(tdRefDetailFk), String.valueOf(tdRefFk));
						if (typeDesigRef == null){
							logger.warn("Type designation reference not found in maps: " + tdRefFk);
						}else{
							designation.setCitation(typeDesigRef);
						}
					}
					if (isNotBlank(tdRefDetails)){
						designation.setCitationMicroReference(tdRefDetails);
					}

					//ID: Type designations do not allow OriginalSources
					designation.addAnnotation(Annotation.NewInstance("Id in BerlinModel-TypeDesignation: " + String.valueOf(typeDesignationId), AnnotationType.INTERNAL(), Language.UNDETERMINED()));

					if (restrictedFlag != null &&restrictedFlag.equals(true)){
						logger.warn("Restricted Flag is expected to be null or 0. TypeDesignationId" + typeDesignationId);
					}

					//Created, Updated
					this.doCreatedUpdated(state, designation, rs);

					if (name != null){
						name.addTypeDesignation(designation, true); //TODO check if true is correct
					}else{
						logger.warn("Name could not be found for type designation " + typeDesignationId);
					}
					namesToSave.add(name);


				} catch (Exception e) {
					logger.warn("Exception in TypeDesignation: TypeDesignationId " + typeDesignationId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn("Names to save: " + namesToSave.size());
			getNameService().save(namesToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	/**
	 * same as {@link BerlinModelImportBase#doCreatedUpdatedNotes}, but handles multiple similar fields
	 * @throws SQLException
	 */
	private void doCreatedUpdated(BerlinModelImportState state, AnnotatableEntity annotatableEntity, ResultSet rs) throws SQLException{
		BerlinModelImportConfigurator config = state.getConfig();
		Object createdWhen = rs.getObject("tsdCreated_When");
		Object tdCreatedWhen = rs.getObject("tdCreated_When");
		if (tdCreatedWhen != null){
			createdWhen = tdCreatedWhen;
		}

		String createdWho = rs.getString("tsdCreated_Who");
		String tdCreatedWho = rs.getString("tdCreated_Who");
		if (tdCreatedWho != null){
			createdWho = tdCreatedWho;
		}

		Object updatedWhen = rs.getObject("tdUpdated_When");
		String updatedWho = rs.getString("tdUpdated_who");

		//Created When, Who, Updated When Who
		if (config.getEditor() == null || config.getEditor().equals(EDITOR.NO_EDITORS)){
			//do nothing
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_ANNOTATION)){
			String createdAnnotationString = "Berlin Model record was created By: " + String.valueOf(createdWho) + " (" + String.valueOf(createdWhen) + ") ";
			if (updatedWhen != null && updatedWho != null){
				createdAnnotationString += " and updated By: " + String.valueOf(updatedWho) + " (" + String.valueOf(updatedWhen) + ")";
			}
			Annotation annotation = Annotation.NewInstance(createdAnnotationString, Language.DEFAULT());
			annotation.setCommentator(config.getCommentator());
			annotation.setAnnotationType(AnnotationType.INTERNAL());
			annotatableEntity.addAnnotation(annotation);
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_EDITOR)){
			User creator = getUser(state, createdWho);
			User updator = getUser(state, updatedWho);
			DateTime created = getDateTime(createdWhen);
			DateTime updated = getDateTime(updatedWhen);
			annotatableEntity.setCreatedBy(creator);
			annotatableEntity.setUpdatedBy(updator);
			annotatableEntity.setCreated(created);
			annotatableEntity.setUpdated(updated);
		}else {
			logger.warn("Editor type not yet implemented: " + config.getEditor());
		}
	}


	private DateTime getDateTime(Object timeString){
		if (timeString == null){
			return null;
		}
		DateTime dateTime = null;
		if (timeString instanceof Timestamp){
			Timestamp timestamp = (Timestamp)timeString;
			dateTime = new DateTime(timestamp);
		}else{
			logger.warn("time ("+timeString+") is not a timestamp. Datetime set to current date. ");
			dateTime = new DateTime();
		}
		return dateTime;
	}


	@Override
    protected String getDerivedUnitNameSpace(){
		return TYPE_SPECIMEN_DERIVED_UNIT_NAMESPACE;
	}

	@Override
    protected String getFieldObservationNameSpace(){
		return TYPE_SPECIMEN_FIELD_OBSERVATION_NAMESPACE;
	}

	private TaxonName getTaxonName(AlgaTerraImportState state, Map<String, TaxonName> taxonNameMap, int nameId) {
		TaxonName result;
		if (state.getConfig().isDoTaxonNames()){
			result = taxonNameMap.get(String.valueOf(nameId));
		}else{
			//for testing
			result = TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES());
		}
		return result;
	}

	private void handleTypeSpecimenSpecificSpecimen(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state, Map<String, Reference> refMap, int typeSpecimenId) throws SQLException {


		//TODO



		DerivedUnit derivedUnit = facade.innerDerivedUnit();

		Integer tsMaterialCategoryFK = nullSafeInt(rs, "tsMaterialCategoryFK");
		String matCat = rs.getString("MaterialCategory");
		if (tsMaterialCategoryFK != null){
			if (tsMaterialCategoryFK.equals(16)){
				tsMaterialCategoryFK = 9;
			}
			UUID uuid = materialCategoryMapping.get(tsMaterialCategoryFK);
			if (uuid == null){
				logger.warn("Uuid was null. This should not happen.");
			}
			DefinedTerm kindOfUnit = getKindOfUnit(state, uuid, matCat, null, null, null);  //all terms should exist already
			facade.setFieldUnitKindOfUnit(kindOfUnit);
		}else{
			logger.warn("Material Category was null. This is not expected");
		}


		//collection
		String barcode = rs.getString("Barcode");
		if (StringUtils.isNotBlank(barcode)){
			facade.setBarcode(barcode);
		}

		//RefFk + RefDetailFk
		Integer  refFk = nullSafeInt(rs, "tsRefFk");
		if (refFk != null){

			Reference ref = refMap.get(String.valueOf(refFk));
			if (ref == null){
				logger.warn("TypeSpecimen reference (" + refFk + ")not found in biblioRef. TypeSpecimenId: " + typeSpecimenId);
			}else{
				IdentifiableSource source = IdentifiableSource.NewPrimarySourceInstance(ref, null);
				derivedUnit.addSource(source);
			}
		}

		Integer refDetailFk = nullSafeInt(rs, "tsRefDetailFk");
		if (refDetailFk != null){
			logger.warn("TypeSpecimen.RefDetailFk should always be NULL but wasn't: " + typeSpecimenId);
		}

	}

	/**
	 * @param state
	 * @param ecoFactId
	 * @param derivedUnitMap
	 * @param type
	 * @param ecoFactId2
	 * @param ecoFactMap
	 * @param sourceRef
	 * @return
	 */
	private DerivedUnitFacade getDerivedUnit(AlgaTerraImportState state, int typeSpecimenId, Map<String, DerivedUnit> typeSpecimenMap, SpecimenOrObservationType type, Map<String, DerivedUnit> ecoFactMap, Integer ecoFactId2, Reference sourceRef) {
		//TODO implement ecoFact map - if not all null anymore
		String typeKey = String.valueOf(typeSpecimenId);
		DerivedUnit derivedUnit = typeSpecimenMap.get(typeKey);
		DerivedUnitFacade facade;
		if (derivedUnit == null){
			facade = DerivedUnitFacade.NewInstance(type);
			typeSpecimenMap.put(typeKey, derivedUnit);
		}else{
			try {
				facade = DerivedUnitFacade.NewInstance(derivedUnit);
				facade.addSource(IdentifiableSource.NewDataImportInstance(typeKey, "TypeSpecimen", sourceRef));
			} catch (DerivedUnitFacadeNotSupportedException e) {
				logger.error(e.getMessage());
				facade = DerivedUnitFacade.NewInstance(type);
			}
		}

		return facade;
	}


	private SpecimenTypeDesignationStatus getSpecimenTypeDesignationStatusByKey(Integer typeStatusFk) {
		if (typeStatusFk == null){ return SpecimenTypeDesignationStatus.TYPE();
		}else if (typeStatusFk == 1) { return SpecimenTypeDesignationStatus.HOLOTYPE();
		}else if (typeStatusFk == 2) { return SpecimenTypeDesignationStatus.LECTOTYPE();
		}else if (typeStatusFk == 3) { return SpecimenTypeDesignationStatus.NEOTYPE();
		}else if (typeStatusFk == 4) { return SpecimenTypeDesignationStatus.EPITYPE();
		}else if (typeStatusFk == 5) { return SpecimenTypeDesignationStatus.ISOLECTOTYPE();
		}else if (typeStatusFk == 6) { return SpecimenTypeDesignationStatus.ISONEOTYPE();
		}else if (typeStatusFk == 7) { return SpecimenTypeDesignationStatus.ISOTYPE();
		}else if (typeStatusFk == 8) { return SpecimenTypeDesignationStatus.PARANEOTYPE();
		}else if (typeStatusFk == 9) { return SpecimenTypeDesignationStatus.PARATYPE();
		}else if (typeStatusFk == 10) { return SpecimenTypeDesignationStatus.SECOND_STEP_LECTOTYPE();
		}else if (typeStatusFk == 11) { return SpecimenTypeDesignationStatus.SECOND_STEP_NEOTYPE();
		}else if (typeStatusFk == 12) { return SpecimenTypeDesignationStatus.SYNTYPE();
		}else if (typeStatusFk == 13) { return SpecimenTypeDesignationStatus.PARALECTOTYPE();
		}else if (typeStatusFk == 14) { return SpecimenTypeDesignationStatus.ISOEPITYPE();
		}else if (typeStatusFk == 21) { return SpecimenTypeDesignationStatus.ICONOTYPE();
		}else if (typeStatusFk == 22) { return SpecimenTypeDesignationStatus.PHOTOTYPE();
		}else if (typeStatusFk == 30) { return SpecimenTypeDesignationStatus.TYPE();
		}else if (typeStatusFk == 38) { return SpecimenTypeDesignationStatus.ISOEPITYPE();
		}else if (typeStatusFk == 39) { return SpecimenTypeDesignationStatus.ORIGINAL_MATERIAL();  //but add annotation
		}else if (typeStatusFk == 40) { return SpecimenTypeDesignationStatus.ORIGINAL_MATERIAL();
		}else{
			logger.warn("typeStatusFk undefined for " +  typeStatusFk);
			return SpecimenTypeDesignationStatus.TYPE();
		}

	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> ecoFieldObservationIdSet = new HashSet<>();
			Set<String> typeSpecimenIdSet = new HashSet<>();
//			Set<String> termsIdSet = new HashSet<>();
			Set<String> collectionIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> refDetailIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "nameFk");
				handleForeignKey(rs, ecoFieldObservationIdSet, "ecoFactFk");
				handleForeignKey(rs, typeSpecimenIdSet, "TypeSpecimenId");
				handleForeignKey(rs, collectionIdSet, "CollectionFk");
				handleForeignKey(rs, referenceIdSet, "tsRefFk");
				handleForeignKey(rs, referenceIdSet, "tdRefFk");
				handleForeignKey(rs, refDetailIdSet, "tdRefDetailFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//eco fact field observation map
			nameSpace = AlgaTerraSpecimenImportBase.ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
			idSet = ecoFieldObservationIdSet;
			Map<String, FieldUnit> fieldObservationMap = getCommonService().getSourcedObjectsByIdInSourceC(FieldUnit.class, idSet, nameSpace);
			result.put(nameSpace, fieldObservationMap);

			//type specimen map
			nameSpace = AlgaTerraSpecimenImportBase.TYPE_SPECIMEN_FIELD_OBSERVATION_NAMESPACE;
			idSet = typeSpecimenIdSet;
			Map<String, FieldUnit> typeSpecimenMap = getCommonService().getSourcedObjectsByIdInSourceC(FieldUnit.class, idSet, nameSpace);
			result.put(nameSpace, typeSpecimenMap);


			//collections
			nameSpace = AlgaTerraCollectionImport.NAMESPACE_COLLECTION;
			idSet = collectionIdSet;
			Map<String, Collection> collectionMap = getCommonService().getSourcedObjectsByIdInSourceC(Collection.class, idSet, nameSpace);
			result.put(nameSpace, collectionMap);

			//sub-collections
			nameSpace = AlgaTerraCollectionImport.NAMESPACE_SUBCOLLECTION;
			idSet = collectionIdSet;
			Map<String, Collection> subCollectionMap = getCommonService().getSourcedObjectsByIdInSourceC(Collection.class, idSet, nameSpace);
			result.put(nameSpace, subCollectionMap);

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
		IOValidator<BerlinModelImportState> validator = new AlgaTerraTypeImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoTypes();
	}
}
