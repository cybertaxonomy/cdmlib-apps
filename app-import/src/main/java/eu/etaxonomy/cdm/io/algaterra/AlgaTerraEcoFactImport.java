/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.algaterra;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraSpecimenImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.MeasurementUnit;
import eu.etaxonomy.cdm.model.description.QuantitativeData;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StatisticalMeasure;
import eu.etaxonomy.cdm.model.description.StatisticalMeasurementValue;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;


/**
 * @author a.mueller
 * @since 01.09.2012
 */
@Component
public class AlgaTerraEcoFactImport  extends AlgaTerraSpecimenImportBase {

    private static final long serialVersionUID = 2918870166537160882L;
    private static final Logger logger = LogManager.getLogger();


	private static int modCount = 5000;
	private static final String pluralString = "eco facts";
	private static final String dbTableName = "EcoFact";  //??


	public AlgaTerraEcoFactImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT EcoFactId " +
				" FROM EcoFact  " +
				" ORDER BY EcoFact.DuplicateFk, EcoFact.EcoFactId ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
            " SELECT EcoFact.*, EcoFact.EcoFactId as unitId, " +
               " tg.ID AS GazetteerId, tg.L1Code, tg.L2Code, tg.L3Code, tg.L4Code, tg.Country, tg.ISOCountry, tg.subL4, " +
               " ec.UUID as climateUuid, eh.UUID as habitatUuid, elf.UUID as lifeFormUuid " +
            " FROM EcoFact " +
                 " LEFT OUTER JOIN TDWGGazetteer tg ON EcoFact.TDWGGazetteerFk = tg.ID " +
                 " LEFT OUTER JOIN EcoClimate  ec  ON EcoFact.ClimateFk  = ec.ClimateId " +
                 " LEFT OUTER JOIN EcoHabitat  eh  ON EcoFact.HabitatFk  = eh.HabitatId " +
                 " LEFT OUTER JOIN EcoLifeForm elf ON EcoFact.LifeFormFk = elf.LifeFormId " +
              " WHERE (EcoFact.EcoFactId IN (" + ID_LIST_TOKEN + ")  )"
            + " ORDER BY EcoFact.DuplicateFk, EcoFact.EcoFactId "
            ;
		return strQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		try {
			makeVocabulariesAndFeatures(state);
		} catch (SQLException e1) {
			logger.warn("Exception occurred when trying to create Ecofact vocabularies: " + e1.getMessage());
			e1.printStackTrace();
		}
		Set<SpecimenOrObservationBase> objectsToSave = new HashSet<SpecimenOrObservationBase>();

		//TODO do we still need this map? EcoFacts are not handled separate from Facts.
		//However, they have duplicates on derived unit level. Also check duplicateFk.
		Map<String, FieldUnit> ecoFactFieldObservationMap = partitioner.getObjectMap(ECO_FACT_FIELD_OBSERVATION_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				int ecoFactId = rs.getInt("EcoFactId");
				Integer duplicateFk = nullSafeInt(rs, "DuplicateFk");

				//FIXME RecordBasis is in Fact table, which is not part of the query anymore.
				//Some EcoFacts have multiple RecordBasis types in Fact. Henning will check this.
//				String recordBasis = rs.getString("RecordBasis");
				String recordBasis = "PreservedSpecimen";

				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					//facade
					SpecimenOrObservationType type = makeDerivedUnitType(recordBasis);

					DerivedUnitFacade facade;
					//field observation
					if (duplicateFk == null){
						facade = DerivedUnitFacade.NewInstance(type);
						handleFieldObservationSpecimen(rs, facade, state, partitioner);
						handleEcoFactSpecificFieldObservation(rs,facade, state);
						FieldUnit fieldObservation = facade.getFieldUnit(true);
						ecoFactFieldObservationMap.put(String.valueOf(ecoFactId), fieldObservation);
					}else{
						FieldUnit fieldObservation = ecoFactFieldObservationMap.get(String.valueOf(duplicateFk));
						facade = DerivedUnitFacade.NewInstance(type, fieldObservation);
					}

					handleFirstDerivedSpecimen(rs, facade, state, partitioner);
					handleEcoFactSpecificDerivedUnit(rs,facade, state);


					DerivedUnit objectToSave = facade.innerDerivedUnit();
					objectsToSave.add(objectToSave);


				} catch (Exception e) {
					logger.warn("Exception in ecoFact: ecoFactId " + ecoFactId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

			logger.warn("Specimen to save: " + objectsToSave.size());
			getOccurrenceService().save(objectsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	@Override
    protected String getDerivedUnitNameSpace(){
		return ECO_FACT_DERIVED_UNIT_NAMESPACE;
	}

	@Override
    protected String getFieldObservationNameSpace(){
		return ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
	}



	private void handleEcoFactSpecificFieldObservation(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state) throws SQLException {

		Object alkalinityFlag = rs.getBoolean("AlkalinityFlag");

		//alkalinity marker
		if (alkalinityFlag != null){
			MarkerType alkalinityMarkerType = getMarkerType(state, uuidMarkerAlkalinity, "Alkalinity", "Alkalinity", null);
			boolean alkFlag = Boolean.valueOf(alkalinityFlag.toString());
			Marker alkalinityMarker = Marker.NewInstance(alkalinityMarkerType, alkFlag);
			facade.getFieldUnit(true).addMarker(alkalinityMarker);
		}


		DescriptionBase<?> fieldDescription = getFieldObservationDescription(facade);

		//habitat, ecology, community, etc.
		String habitat = rs.getString("HabitatExplanation");

		if (isNotBlank(habitat)){
			Feature habitatExplanation = getFeature(state, uuidFeatureHabitatExplanation, "Habitat Explanation", "HabitatExplanation", null, null);
			TextData textData = TextData.NewInstance(habitatExplanation);
			textData.putText(Language.DEFAULT(), habitat);
			fieldDescription.addElement(textData);
		}

		String community = rs.getString("Comunity");
		if (isNotBlank(community)){
			Feature communityFeature = getFeature(state, uuidFeatureSpecimenCommunity, "Community", "The community of a specimen (e.g. other algae in the same sample)", null, null);
			TextData textData = TextData.NewInstance(communityFeature);
			textData.putText(Language.DEFAULT(), community);
			fieldDescription.addElement(textData);
		}

		String additionalData = rs.getString("AdditionalData");
		if (isNotBlank(additionalData)){  //or handle it as Annotation ??
			Feature additionalDataFeature = getFeature(state, uuidFeatureAdditionalData, "Additional Data", "Additional Data", null, null);
			TextData textData = TextData.NewInstance(additionalDataFeature);
			textData.putText(Language.DEFAULT(), additionalData);
			fieldDescription.addElement(textData);
		}

		String climateUuid = rs.getString("climateUuid");
		String habitatUuid = rs.getString("habitatUuid");
		String lifeFormUuid = rs.getString("lifeFormUuid");

		addCategoricalValue(state, fieldDescription, climateUuid, uuidFeatureAlgaTerraClimate);
		addCategoricalValue(state, fieldDescription, habitatUuid, Feature.HABITAT().getUuid());
		addCategoricalValue(state, fieldDescription, lifeFormUuid, uuidFeatureAlgaTerraLifeForm);


		//parameters
		makeParameter(state, rs, getFieldObservationDescription(facade));

	}

	private void handleEcoFactSpecificDerivedUnit(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state) throws SQLException {
		//collection
		String voucher = rs.getString("Voucher");
		if (StringUtils.isNotBlank(voucher)){
			facade.setAccessionNumber(voucher);
		}
	}


	private void addCategoricalValue(AlgaTerraImportState importState, DescriptionBase description, String uuidTerm, UUID featureUuid) {
		if (uuidTerm != null){
			State state = this.getStateTerm(importState, UUID.fromString(uuidTerm));
			Feature feature = getFeature(importState, featureUuid);
			CategoricalData categoricalData = CategoricalData.NewInstance(state, feature);
			description.addElement(categoricalData);
		}
	}

	private void makeParameter(AlgaTerraImportState state, ResultSet rs, DescriptionBase<?> descriptionBase) throws SQLException {
		for (int i = 1; i <= 10; i++){
			String valueStr = rs.getString(String.format("P%dValue", i));
			String unitStr = rs.getString(String.format("P%dUnit", i));
			String parameter = rs.getString(String.format("P%dParameter", i));
			String method = rs.getString(String.format("P%dMethod", i));

			//method
			if (StringUtils.isNotBlank(method)){
				//TODO
				//see https://dev.e-taxonomy.eu/redmine/issues/4205
				logger.warn("Methods not yet handled: " + method + ", #4205");
			}
			//parameter
			TermVocabulary<Feature> vocParameter = getVocabulary(state, TermType.Feature, uuidVocParameter, "Feature vocabulary for AlgaTerra measurement parameters", "Parameters", null, null, false, Feature.COMMON_NAME());
			if (StringUtils.isNotBlank(parameter)){
				UUID featureUuid = getParameterFeatureUuid(state, parameter);
				Feature feature = getFeature(state, featureUuid, parameter, parameter, null, vocParameter);
				QuantitativeData quantData = QuantitativeData.NewInstance(feature);

				//unit
				MeasurementUnit unit = getMeasurementUnit(state, unitStr);
				quantData.setUnit(unit);
				try {

					Set<DefinedTerm> valueModifier = new HashSet<>();
					valueStr = normalizeAndModifyValue(state, valueStr, valueModifier);
					//value
					BigDecimal valueDec = new BigDecimal(valueStr);

					StatisticalMeasure measureSingleValue = getStatisticalMeasure(state, uuidStatMeasureSingleValue, "Value", "Single measurement value", null, null);
					StatisticalMeasurementValue value = StatisticalMeasurementValue.NewInstance(measureSingleValue, valueDec);
					quantData.addStatisticalValue(value);
					descriptionBase.addElement(quantData);

				} catch (NumberFormatException e) {
					logger.warn(String.format("Value '%s' can't be converted to double. Parameter %s not imported.", valueStr, parameter));
				}
			}else if (isNotBlank(valueStr) || isNotBlank(unitStr) ){
				logger.warn("There is value or unit without parameter: " + i);
			}
		}
	}

	private String normalizeAndModifyValue(AlgaTerraImportState state, String valueStr, Set<DefinedTerm> valueModifier) {
		valueStr = valueStr.replace(",", ".");
		if (valueStr.startsWith("<")){
			TermVocabulary<DefinedTerm> measurementValueModifierVocabulary = getVocabulary(state, TermType.Modifier, uuidMeasurementValueModifier, "Measurement value modifier", "Measurement value modifier", null, null, false, DefinedTerm.NewModifierInstance(null, null, null));
			DefinedTerm modifier = getModifier(state, uuidModifierLowerThan, "Lower", "Lower than the given measurement value", "<", measurementValueModifierVocabulary);
			valueModifier.add(modifier);
			valueStr = valueStr.replace("<", "");
		}
		if (valueStr.startsWith(">")){
			TermVocabulary<DefinedTerm> measurementValueModifierVocabulary = getVocabulary(state, TermType.Modifier, uuidMeasurementValueModifier, "Measurement value modifier", "Measurement value modifier", null, null, false, DefinedTerm.NewModifierInstance(null, null, null));
			DefinedTerm modifier = getModifier(state, uuidModifierGreaterThan, "Lower", "Lower than the given measurement value", "<", measurementValueModifierVocabulary);
			valueModifier.add(modifier);
			valueStr = valueStr.replace(">", "");
		}
		return valueStr;
	}



	private UUID getParameterFeatureUuid(AlgaTerraImportState state, String key) {
		return AlgaTerraImportTransformer.getFeatureUuid(key);
	}



	/**
	 * TODO move to InputTransformerBase
	 * @param state
	 * @param unitStr
	 * @return
	 */
	private MeasurementUnit getMeasurementUnit(AlgaTerraImportState state, String unitStr) {
		if (StringUtils.isNotBlank(unitStr)){
			UUID uuid = AlgaTerraImportTransformer.getMeasurementUnitUuid(unitStr);
			if (uuid != null){
				return getMeasurementUnit(state, uuid, unitStr, unitStr, unitStr, null);
			}else{
				logger.warn("MeasurementUnit was not recognized");
				return null;
			}
		}else{
			return null;
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> fieldObservationIdSet = new HashSet<>();
			Set<String> termsIdSet = new HashSet<>();
			Set<String> collectionIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, fieldObservationIdSet, "DuplicateFk");
				handleForeignKey(rs, termsIdSet, "ClimateFk");
				handleForeignKey(rs, termsIdSet, "HabitatFk");
				handleForeignKey(rs, termsIdSet, "LifeFormFk");
				handleForeignKey(rs, collectionIdSet, "CollectionFk");
			}

			//field observation map for duplicates
			nameSpace = AlgaTerraSpecimenImportBase.ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
			idSet = fieldObservationIdSet;
			Map<String, FieldUnit> fieldObservationMap = getCommonService().getSourcedObjectsByIdInSourceC(FieldUnit.class, idSet, nameSpace);
			result.put(nameSpace, fieldObservationMap);

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

			//terms
			nameSpace = AlgaTerraSpecimenImportBase.TERMS_NAMESPACE;
			Class<FieldUnit> cdmClass = FieldUnit.class;  //????????
			idSet = termsIdSet;
			//FIXME something not correct here with the classes
			Map<String, DefinedTermBase> termMap = (Map)getCommonService().getSourcedObjectsByIdInSourceC(cdmClass, idSet, nameSpace);
			result.put(nameSpace, termMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraSpecimenImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoEcoFacts();
	}

}
