/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.net.URI;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraSpecimenImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.MeasurementUnit;
import eu.etaxonomy.cdm.model.description.Modifier;
import eu.etaxonomy.cdm.model.description.QuantitativeData;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StatisticalMeasure;
import eu.etaxonomy.cdm.model.description.StatisticalMeasurementValue;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldObservation;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.03.2008
 * @version 1.0
 */
@Component
public class AlgaTerraSpecimenImport  extends BerlinModelImportBase {
	private static final Logger logger = Logger.getLogger(AlgaTerraSpecimenImport.class);

	public static final String ECO_FACT_NAMESPACE = "EcoFact";
	public static final String TERMS_NAMESPACE = "ALGA_TERRA_TERMS";
	
	//TODO move to transformrer
	final static UUID uuidMarkerAlkalinity = UUID.fromString("e52d0ea2-0c1f-4d95-ae6d-e21ab317c594");  
	final static UUID uuidRefSystemGps = UUID.fromString("c23e4928-c137-4e4a-b6ab-b430da3d0b94");  
	final static UUID uuidFeatureSpecimenCommunity = UUID.fromString("3ff5b1ab-3999-4b5a-b8f7-01fd2f6c12c7");
	final static UUID uuidFeatureAdditionalData = UUID.fromString("0ac82ab8-2c2b-4953-98eb-a9f718eb9c57");
	final static UUID uuidFeatureHabitatExplanation = UUID.fromString("6fe32295-61a3-44fc-9fcf-a85790ea888f");
	
	final static UUID uuidVocAlgaTerraClimate = UUID.fromString("b0a677c6-8bb6-43f4-b1b8-fc377a10feb5");
	final static UUID uuidVocAlgaTerraHabitat = UUID.fromString("06f30114-e19c-4e7d-a8e5-5488c41fcbc5");
	final static UUID uuidVocAlgaTerraLifeForm = UUID.fromString("3c0b194e-809c-4b42-9498-6ff034066ed7");
	
	final static UUID uuidFeatureAlgaTerraClimate = UUID.fromString("8754674c-9ab9-4f28-95f1-91eeee2314ee");
	final static UUID uuidFeatureAlgaTerraHabitat = UUID.fromString("7def3fc2-cdc5-4739-8e13-62edbd053415");
	final static UUID uuidFeatureAlgaTerraLifeForm = UUID.fromString("9b657901-1b0d-4a2a-8d21-dd8c1413e2e6");
	
	final static UUID uuidVocParameter = UUID.fromString("45888b40-5bbb-4293-aa1e-02479796cd7c");
	final static UUID uuidStatMeasureSingleValue = UUID.fromString("eb4c3d98-4d4b-4c37-8eb4-17315ce79920");
	final static UUID uuidMeasurementValueModifier = UUID.fromString("0218a7a3-f6c0-4d06-a4f8-6b50b73aef5e");
	
	final static UUID uuidModifierLowerThan = UUID.fromString("2b500085-6bef-4003-b6ea-e0ad0237d79d");
	final static UUID uuidModifierGreaterThan = UUID.fromString("828df49d-c745-48f7-b083-0ada43356c34");
	
	private static int modCount = 5000;
	private static final String pluralString = "specimen and observation";
	private static final String dbTableName = "Fact";  //??  


	public AlgaTerraSpecimenImport(){
		super();
	}
	
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT factId " + 
				" FROM Fact " +
					" INNER JOIN EcoFact ON Fact.ExtensionFk = EcoFact.EcoFactId " +
					"INNER JOIN PTaxon ON Fact.PTNameFk = PTaxon.PTNameFk AND Fact.PTRefFk = PTaxon.PTRefFk "
				+ " WHERE FactCategoryFk = 202 "
				+ " ORDER BY EcoFact.EcoFactId, PTaxon.RIdentifier, Fact.FactId ";
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =   //DISTINCT because otherwise emOccurrenceSource creates multiple records for a single distribution 
            " SELECT PTaxon.RIdentifier as taxonId, Fact.FactId, Fact.RecordBasis, EcoFact.*, " + 
               " tg.ID AS GazetteerId, tg.L2Code, tg.L3Code, tg.L4Code, tg.Country, tg.ISOCountry, " +
               " ec.UUID as climateUuid, eh.UUID as habitatUuid, elf.UUID as lifeFormUuid" +
            " FROM Fact " + 
                 " INNER JOIN EcoFact ON Fact.ExtensionFk = EcoFact.EcoFactId " +
                 " INNER JOIN PTaxon ON dbo.Fact.PTNameFk = dbo.PTaxon.PTNameFk AND dbo.Fact.PTRefFk = dbo.PTaxon.PTRefFk " +
                 " LEFT OUTER JOIN TDWGGazetteer tg ON EcoFact.TDWGGazetteerFk = tg.ID " +
                 " LEFT OUTER JOIN EcoClimate  ec  ON EcoFact.ClimateFk  = ec.ClimateId " +
                 " LEFT OUTER JOIN EcoHabitat  eh  ON EcoFact.HabitatFk  = eh.HabitatId " +
                 " LEFT OUTER JOIN EcoLifeForm elf ON EcoFact.LifeFormFk = elf.LifeFormId " +
              " WHERE Fact.FactCategoryFk = 202 AND (Fact.FactId IN (" + ID_LIST_TOKEN + ")  )"  
            + " ORDER BY EcoFact.EcoFactId, PTaxon.RIdentifier, Fact.FactId "
            ;
		return strQuery;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#doPartition(eu.etaxonomy.cdm.io.berlinModel.in.ResultSetPartitioner, eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState)
	 */
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;
		
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		try {
			makeVocabulariesAndFeatures(state);
		} catch (SQLException e1) {
			logger.warn("Exception occurred when trying to create Ecofact vocabularies: " + e1.getMessage());
			e1.printStackTrace();
		}
		Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();
		
		Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>) partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		Map<String, DerivedUnit> ecoFactMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("Specimen facts handled: " + (i-1));}
				
				int newTaxonId = rs.getInt("taxonId");
				int factId = rs.getInt("FactId");
				int ecoFactId = rs.getInt("EcoFactId");
				String recordBasis = rs.getString("RecordBasis");
				
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
				
					//facade
					DerivedUnitType type = makeDerivedUnitType(recordBasis);
					DerivedUnitFacade facade = getDerivedUnit(state, ecoFactId, ecoFactMap, type);
					
					//field observation
					handleSingleSpecimen(rs, facade, state);
					
					state.setCurrentFieldObservationNotNew(false);
					
					//description element
					TaxonDescription taxonDescription = getTaxonDescription(state, newTaxonId, taxonMap, factId, sourceRef);
					IndividualsAssociation indAssociation = IndividualsAssociation.NewInstance();
					Feature feature = makeFeature(type);
					indAssociation.setAssociatedSpecimenOrObservation(facade.innerDerivedUnit());
					indAssociation.setFeature(feature);
					taxonDescription.addElement(indAssociation);
					
					taxaToSave.add(taxonDescription.getTaxon()); 
					

				} catch (Exception e) {
					logger.warn("Exception in ecoFact: FactId " + factId + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn("Taxa to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}




	/**
	 * Creates the vocabularies and the features for Climate, Habitat and Lifeform
	 * @param state
	 * @throws SQLException
	 */
	private void makeVocabulariesAndFeatures(AlgaTerraImportState state) throws SQLException {
		String abbrevLabel = null;
		URI uri = null;
		
		if (! state.isSpecimenVocabulariesCreated()){
			
			TransactionStatus txStatus = this.startTransaction();
		
			boolean isOrdered = true;
			OrderedTermVocabulary<State> climateVoc = (OrderedTermVocabulary)getVocabulary(uuidVocAlgaTerraClimate, "Climate", "Climate", abbrevLabel, uri, isOrdered, null);
			OrderedTermVocabulary<State> habitatVoc = (OrderedTermVocabulary)getVocabulary(uuidVocAlgaTerraHabitat, "Habitat", "Habitat", abbrevLabel, uri, isOrdered, null);
			OrderedTermVocabulary<State> lifeformVoc = (OrderedTermVocabulary)getVocabulary(uuidVocAlgaTerraLifeForm, "Lifeform", "Lifeform", abbrevLabel, uri, isOrdered, null);
			
			
			Feature feature = getFeature(state, uuidFeatureAlgaTerraClimate, "Climate","Climate", null, null);
			feature.setSupportsCategoricalData(true);
			
			feature = getFeature(state, uuidFeatureAlgaTerraLifeForm, "LifeForm","LifeForm", null, null);
			feature.setSupportsCategoricalData(true);
			
			feature = Feature.HABITAT();
			feature.setSupportsCategoricalData(true);
			getTermService().saveOrUpdate(feature);
			
			Source source = state.getAlgaTerraConfigurator().getSource();
			
			String climateSql = "SELECT * FROM EcoClimate";
			ResultSet rs = source.getResultSet(climateSql);
			while (rs.next()){
				String climate = rs.getString("Climate");
				String description = rs.getString("Description");
				Integer id = rs.getInt("ClimateId");
				UUID uuid = UUID.fromString(rs.getString("UUID"));
				State stateTerm = getStateTerm(state, uuid, climate, description, null, climateVoc);
				addOriginalSource(stateTerm, id.toString(), "EcoClimate", state.getTransactionalSourceReference());
				getTermService().saveOrUpdate(stateTerm);
			}
			
			String habitatSql = "SELECT * FROM EcoHabitat";
			rs = source.getResultSet(habitatSql);
			while (rs.next()){
				String habitat = rs.getString("Habitat");
				String description = rs.getString("Description");
				Integer id = rs.getInt("HabitatId");
				UUID uuid = UUID.fromString(rs.getString("UUID"));
				State stateTerm = getStateTerm(state, uuid, habitat, description, null, habitatVoc);
				addOriginalSource(stateTerm, id.toString(), "EcoHabitat", state.getTransactionalSourceReference());
				getTermService().saveOrUpdate(stateTerm);
			}
			
			String lifeformSql = "SELECT * FROM EcoLifeForm";
			rs = source.getResultSet(lifeformSql);
			while (rs.next()){
				String lifeform = rs.getString("LifeForm");
				String description = rs.getString("Description");
				Integer id = rs.getInt("LifeFormId");
				UUID uuid = UUID.fromString(rs.getString("UUID"));
				State stateTerm = getStateTerm(state, uuid, lifeform, description, null, lifeformVoc);
				addOriginalSource(stateTerm, id.toString(), "EcoLifeForm", state.getTransactionalSourceReference());
				getTermService().saveOrUpdate(stateTerm);
			}
			
			this.commitTransaction(txStatus);
			
			state.setSpecimenVocabulariesCreated(true);
		}
		
	}



	private void handleSingleSpecimen(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state) throws SQLException {
		//FIXME missing fields #3084, #3085, #3080
		try {
			Object alkalinityFlag = rs.getBoolean("AlkalinityFlag");
			
			String locality = rs.getString("Locality");
			Double latitude = nullSafeDouble(rs, "Latitude");
			Double longitude = nullSafeDouble(rs, "Longitude");
			Integer errorRadius = nullSafeInt(rs,"Prec");
			String geoCodeMethod = rs.getString("GeoCodeMethod");
			
			Integer altitude = nullSafeInt(rs, "Altitude");
			Integer lowerAltitude = nullSafeInt(rs,"AltitudeLowerValue");
			String altitudeUnit = rs.getString("AltitudeUnit");
			Double depth = nullSafeDouble(rs, "Depth");
			Double depthLow = nullSafeDouble(rs, "DepthLow");
			   	
			String collectorsNumber = rs.getString("CollectorsNumber");
			Date collectionDateStart = rs.getDate("CollectionDate");
			Date collectionDateEnd = rs.getDate("CollectionDateEnd");
			
			String climateUuid = rs.getString("climateUuid");
			String habitatUuid = rs.getString("habitatUuid");
			String lifeFormUuid = rs.getString("lifeFormUuid");
			
			String habitat = rs.getString("HabitatExplanation");
			String community = rs.getString("Comunity");
			String additionalData = rs.getString("AdditionalData");
			
			
			
			FieldObservation fieldObservation = facade.getFieldObservation(true);
			
			//alkalinity marker
			if (alkalinityFlag != null){
				MarkerType alkalinityMarkerType = getMarkerType(state, uuidMarkerAlkalinity, "Alkalinity", "Alkalinity", null);
				boolean alkFlag = Boolean.valueOf(alkalinityFlag.toString());
				Marker alkalinityMarker = Marker.NewInstance(alkalinityMarkerType, alkFlag);
				fieldObservation.addMarker(alkalinityMarker);
			}
			
			//location
			facade.setLocality(locality);
			    	
			//exact location
			ReferenceSystem referenceSystem = makeRefrenceSystem(geoCodeMethod, state);
			Point exactLocation = Point.NewInstance(longitude, latitude, referenceSystem, errorRadius);
			facade.setExactLocation(exactLocation);
			
			//altitude, depth
			if (StringUtils.isNotBlank(altitudeUnit) && ! altitudeUnit.trim().equalsIgnoreCase("m")){
				logger.warn("Altitude unit is not [m] but: " +  altitudeUnit);
			}
			if ( altitude != null){
				if (lowerAltitude == null){
					facade.setAbsoluteElevation(altitude);
				}else{
			   		if (! facade.isEvenDistance(lowerAltitude, altitude)){
			   			//FIXME there is a ticket for this
			   			altitude = altitude + 1;
			   			logger.warn("Current implementation of altitude does not allow uneven distances");
			   		}
					facade.setAbsoluteElevationRange(lowerAltitude,altitude);
			   	}
			}
			if ( depth != null){
				//FIXME needs model change to accept double #3072
				Integer intDepth = depth.intValue();
				if (depthLow == null){
					facade.setDistanceToWaterSurface(intDepth);
				}else{
					//FIXME range not yet in model #3074
			   		facade.setDistanceToWaterSurface(intDepth);
			   	}
			}
			
			//habitat, ecology, community, etc.
			DescriptionBase<?> fieldDescription = getFieldObservationDescription(facade);
			
			addCategoricalValue(state, fieldDescription, climateUuid, uuidFeatureAlgaTerraClimate);
			addCategoricalValue(state, fieldDescription, habitatUuid, Feature.HABITAT().getUuid());
			addCategoricalValue(state, fieldDescription, lifeFormUuid, uuidFeatureAlgaTerraLifeForm);
			
			if (isNotBlank(habitat)){
				Feature habitatExplanation = getFeature(state, uuidFeatureHabitatExplanation, "Habitat Explanation", "HabitatExplanation", null, null);
				TextData textData = TextData.NewInstance(habitatExplanation);
				textData.putText(Language.DEFAULT(), habitat);
				getFieldObservationDescription(facade).addElement(textData);
			}
			if (isNotBlank(community)){
				Feature communityFeature = getFeature(state, uuidFeatureSpecimenCommunity, "Community", "The community of a specimen (e.g. other algae in the same sample)", null, null);
				TextData textData = TextData.NewInstance(communityFeature);
				textData.putText(Language.DEFAULT(), community);
				getFieldObservationDescription(facade).addElement(textData);
			}
			if (isNotBlank(additionalData)){  //or handle it as Annotation ??
				Feature additionalDataFeature = getFeature(state, uuidFeatureAdditionalData, "Additional Data", "Additional Data", null, null);
				TextData textData = TextData.NewInstance(additionalDataFeature);
				textData.putText(Language.DEFAULT(), additionalData);
				getFieldObservationDescription(facade).addElement(textData);
			}
			
			//field
			facade.setFieldNumber(collectorsNumber);
			TimePeriod gatheringPeriod = TimePeriod.NewInstance(collectionDateStart, collectionDateEnd);
			facade.setGatheringPeriod(gatheringPeriod);
			handleCollectorTeam(state, facade, rs);
			
			//areas
			makeAreas(state, rs, facade);
			
			//parameters
			makeParameter(state, rs, getFieldObservationDescription(facade));
			
			//collection
			String voucher = rs.getString("Voucher");
			if (StringUtils.isNotBlank(voucher)){
				facade.setAccessionNumber(voucher);
			}
			
			
			//notes
			//TODO is this an annotation on field observation or on the derived unit?
			
			//TODO id, created for fact +  ecoFact
			//    	this.doIdCreatedUpdatedNotes(state, descriptionElement, rs, id, namespace);
		
		} catch (Exception e) {
			throw new RuntimeException(e);
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
				logger.warn("Methods not yet handled: " + method);
			}
			//parameter
			TermVocabulary<Feature> vocParameter = getVocabulary(uuidVocParameter, "Feature vocabulary for AlgaTerra measurement parameters", "Parameters", null, null, false, Feature.COMMON_NAME());
			if (StringUtils.isNotBlank(parameter)){
				UUID featureUuid = getParameterFeatureUuid(state, parameter);
				Feature feature = getFeature(state, featureUuid, parameter, parameter, null, vocParameter);
				QuantitativeData quantData = QuantitativeData.NewInstance(feature);
				
				//unit
				MeasurementUnit unit = getMeasurementUnit(state, unitStr);
				quantData.setUnit(unit);
				try {
					
					Set<Modifier> valueModifier = new HashSet<Modifier>();
					valueStr = normalizeAndModifyValue(state, valueStr, valueModifier);
					//value
					Float valueFlt = Float.valueOf(valueStr);  //TODO maybe change model to Double ??
					
					StatisticalMeasure measureSingleValue = getStatisticalMeasure(state, uuidStatMeasureSingleValue, "Value", "Single measurement value", null, null);
					StatisticalMeasurementValue value = StatisticalMeasurementValue.NewInstance(measureSingleValue, valueFlt); 
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

	private String normalizeAndModifyValue(AlgaTerraImportState state, String valueStr, Set<Modifier> valueModifier) {
		valueStr = valueStr.replace(",", ".");
		if (valueStr.startsWith("<")){
			TermVocabulary<Modifier> measurementValueModifierVocabulary = getVocabulary(uuidMeasurementValueModifier, "Measurement value modifier", "Measurement value modifier", null, null, false, Modifier.NewInstance());
			Modifier modifier = getModifier(state, uuidModifierLowerThan, "Lower", "Lower than the given measurement value", "<", measurementValueModifierVocabulary);
			valueModifier.add(modifier);
			valueStr = valueStr.replace("<", "");
		}
		if (valueStr.startsWith(">")){
			TermVocabulary<Modifier> measurementValueModifierVocabulary = getVocabulary(uuidMeasurementValueModifier, "Measurement value modifier", "Measurement value modifier", null, null, false, Modifier.NewInstance());
			Modifier modifier = getModifier(state, uuidModifierGreaterThan, "Lower", "Lower than the given measurement value", "<", measurementValueModifierVocabulary);
			valueModifier.add(modifier);
			valueStr = valueStr.replace(">", "");
		}
		return valueStr;
	}



	private UUID getParameterFeatureUuid(AlgaTerraImportState state, String key) {
		//TODO define some UUIDs in Transformer
		UUID uuid = state.getParameterFeatureUuid(key);
		if (uuid == null){
			uuid = UUID.randomUUID();
			state.putParameterFeatureUuid(key, uuid);
		}
		return uuid;
	}



	/**
	 * TODO move to InputTransformerBase
	 * @param state
	 * @param unitStr
	 * @return
	 */
	private MeasurementUnit getMeasurementUnit(AlgaTerraImportState state, String unitStr) {
		MeasurementUnit result = null;
		if (StringUtils.isNotBlank(unitStr)){
			UUID uuidMeasurementUnitMgL = UUID.fromString("7ac302c5-3cbd-4334-964a-bf5d11eb9ead");
			UUID uuidMeasurementUnitMolMol = UUID.fromString("96b78d78-3e49-448f-8100-e7779b71dd53");
			UUID uuidMeasurementUnitMicroMolSiL = UUID.fromString("2cb8bc85-a4af-42f1-b80b-34c36c9f75d4");
			UUID uuidMeasurementUnitMicroMolL = UUID.fromString("a631f62e-377e-405c-bd1a-76885b13a72b");
			UUID uuidMeasurementUnitDegreeC = UUID.fromString("55222aec-d5be-413e-8db7-d9a48c316c6c");
			UUID uuidMeasurementUnitPercent = UUID.fromString("3ea3110e-f048-4bed-8bfe-33c60f63626f");
			UUID uuidMeasurementUnitCm = UUID.fromString("3ea3110e-f048-4bed-8bfe-33c60f63626f");
			UUID uuidMeasurementUnitMicroSiCm = UUID.fromString("3ea3110e-f048-4bed-8bfe-33c60f63626f");
			
			
			if (unitStr.equalsIgnoreCase("mg/L")){
				return  getMeasurementUnit(state, uuidMeasurementUnitMgL, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("mol/mol")){
				return result = getMeasurementUnit(state, uuidMeasurementUnitMolMol, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("\u00B5mol Si/L")){   //µmol Si/L
				return getMeasurementUnit(state, uuidMeasurementUnitMicroMolSiL, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("\u00B5mol/L")){		//µmol/L
				return getMeasurementUnit(state, uuidMeasurementUnitMicroMolL, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("\u00B0C")){               //°C
				return getMeasurementUnit(state, uuidMeasurementUnitDegreeC, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("%")){
				return getMeasurementUnit(state, uuidMeasurementUnitPercent, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("cm")){
				return getMeasurementUnit(state, uuidMeasurementUnitCm, unitStr, unitStr, unitStr, null);
			}else if (unitStr.equalsIgnoreCase("\u00B5S/cm")){   //µS/cm
				return getMeasurementUnit(state, uuidMeasurementUnitMicroSiCm, unitStr, unitStr, unitStr, null);
			}else{
				logger.warn("MeasurementUnit was not recognized");
				return null;
			}
		}else{
			return null;
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


	private void handleCollectorTeam(AlgaTerraImportState state, DerivedUnitFacade facade, ResultSet rs) throws SQLException {
		// FIXME parsen
		String collector = rs.getString("Collector");
		Team team = Team.NewTitledInstance(collector, collector);
		facade.setCollector(team);
		
		
		
	}

	private void makeAreas(AlgaTerraImportState state, ResultSet rs, DerivedUnitFacade facade) throws SQLException {
	   	Object gazetteerId = rs.getObject("GazetteerId");
	   	if (gazetteerId != null){
	   		//TDWG
	   		NamedArea tdwgArea;
	   		String tdwg4 = rs.getString("L4Code");
	   		if (isNotBlank(tdwg4)){
	   			tdwgArea = TdwgArea.getAreaByTdwgAbbreviation(tdwg4);
	   		}else{
	   			String tdwg3 = rs.getString("L3Code");
	   			if (isNotBlank(tdwg3)){
	   				tdwgArea = TdwgArea.getAreaByTdwgAbbreviation(tdwg3);
	   			}else{
	   				Integer tdwg2 = rs.getInt("L2Code");   				
	   				tdwgArea = TdwgArea.getAreaByTdwgAbbreviation(String.valueOf(tdwg2));
		   		}
	   		}
	   		if (tdwgArea == null){
	   			logger.warn("TDWG area could not be defined for gazetterId: " + gazetteerId);
	   		}else{
	   			facade.addCollectingArea(tdwgArea);
	   		}
	   		
	   		//Countries
	   		WaterbodyOrCountry country = null;
	   		String isoCountry = rs.getString("ISOCountry");
	   		String countryStr = rs.getString("Country");
	   		if (isNotBlank(isoCountry)){
		   		country = WaterbodyOrCountry.getWaterbodyOrCountryByIso3166A2(isoCountry);
	   		}else if (isNotBlank(countryStr)){
	   			logger.warn("Country exists but no ISO code");
	   		}
	   		if (country == null){
	   			logger.warn("Country does not exist for GazetteerID " + gazetteerId);
	   		}else{
	   			facade.setCountry(country);
	   		}
	   		
	   	}
	    
	   	//Waterbody
	   	WaterbodyOrCountry waterbody = null;
	   	String waterbodyStr = rs.getString("WaterBody");
	   	if (isNotBlank(waterbodyStr)){
	   		if (waterbodyStr.equals("Atlantic Ocean")){
	   			waterbody = WaterbodyOrCountry.ATLANTICOCEAN();
	   		}else{
	   			logger.warn("Waterbody not recognized: " + waterbody);
	   		}
	   		if (waterbody != null){
	   			facade.addCollectingArea(waterbody);
	   		}
	   	}

		
	   	//countries sub
	   	//TODO
	}

	private DescriptionBase getFieldObservationDescription(DerivedUnitFacade facade) {
		Set<DescriptionBase> descriptions = facade.innerFieldObservation().getDescriptions();
		for (DescriptionBase desc : descriptions){
			if (desc.isImageGallery() == false){
				return desc;
			}
		}
		SpecimenDescription specDesc = SpecimenDescription.NewInstance(facade.innerFieldObservation());
		descriptions.add(specDesc);
		return specDesc;
	}

	private ReferenceSystem makeRefrenceSystem(String geoCodeMethod, AlgaTerraImportState state) {
		if (StringUtils.isBlank(geoCodeMethod)){
			return null;
		}else if(geoCodeMethod.startsWith("GPS")){
			getReferenceSystem(state, uuidRefSystemGps, "GPS", "GPS", "GPS", ReferenceSystem.GOOGLE_EARTH().getVocabulary());
			return ReferenceSystem.WGS84(); 
		}else if(geoCodeMethod.startsWith("Google")){
			return ReferenceSystem.GOOGLE_EARTH();
		}else if(geoCodeMethod.startsWith("Map")){
			logger.warn("Reference system " +  geoCodeMethod +  " not yet supported.");
			return null;
		}else if(geoCodeMethod.startsWith("WikiProjekt Georeferenzierung") || geoCodeMethod.startsWith("http://toolserver.org/~geohack/geohack.php") ){
			return ReferenceSystem.WGS84();
		}else {
			logger.warn("Reference system " +  geoCodeMethod +  " not yet supported.");
			return null;
		}
	}

	/**
	 * @param state
	 * @param ecoFactId
	 * @param derivedUnitMap
	 * @param type 
	 * @return
	 */
	private DerivedUnitFacade getDerivedUnit(AlgaTerraImportState state, int ecoFactId, Map<String, DerivedUnit> derivedUnitMap, DerivedUnitType type) {
		String key = String.valueOf(ecoFactId);
		DerivedUnit derivedUnit = derivedUnitMap.get(key);
		DerivedUnitFacade facade;
		if (derivedUnit == null){
			facade = DerivedUnitFacade.NewInstance(type);
			derivedUnitMap.put(key, derivedUnit);
		}else{
			try {
				facade = DerivedUnitFacade.NewInstance(derivedUnit);
			} catch (DerivedUnitFacadeNotSupportedException e) {
				logger.error(e.getMessage());
				facade = DerivedUnitFacade.NewInstance(type);
			}
		}
		
		return facade;
	}
	
	private Feature makeFeature(DerivedUnitType type) {
		if (type.equals(DerivedUnitType.DerivedUnit)){
			return Feature.INDIVIDUALS_ASSOCIATION();
		}else if (type.equals(DerivedUnitType.FieldObservation) || type.equals(DerivedUnitType.Observation) ){
			return Feature.OBSERVATION();
		}else if (type.equals(DerivedUnitType.Fossil) || type.equals(DerivedUnitType.LivingBeing) || type.equals(DerivedUnitType.Specimen )){
			return Feature.SPECIMEN();
		}
		logger.warn("No feature defined for derived unit type: " + type);
		return null;
	}


	private DerivedUnitType makeDerivedUnitType(String recordBasis) {
		DerivedUnitType result = null;
		if (StringUtils.isBlank(recordBasis)){
			result = DerivedUnitType.DerivedUnit;
		} else if (recordBasis.equalsIgnoreCase("FossileSpecimen")){
			result = DerivedUnitType.Fossil;
		}else if (recordBasis.equalsIgnoreCase("HumanObservation")){
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("Literature")){
			logger.warn("Literature record basis not yet supported");
			result = DerivedUnitType.DerivedUnit;
		}else if (recordBasis.equalsIgnoreCase("LivingSpecimen")){
			result = DerivedUnitType.LivingBeing;
		}else if (recordBasis.equalsIgnoreCase("MachineObservation")){
			logger.warn("MachineObservation record basis not yet supported");
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("PreservedSpecimen")){
			result = DerivedUnitType.Specimen;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		String nameSpace;
		Class cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> taxonIdSet = new HashSet<String>();
			Set<String> fieldObservationIdSet = new HashSet<String>();
			Set<String> termsIdSet = new HashSet<String>();
			
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
				handleForeignKey(rs, fieldObservationIdSet, "ecoFactId");
				handleForeignKey(rs, termsIdSet, "ClimateFk");
				handleForeignKey(rs, termsIdSet, "HabitatFk");
				handleForeignKey(rs, termsIdSet, "LifeFormFk");
			}
			
			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase> objectMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//field observation map map
			nameSpace = AlgaTerraSpecimenImport.ECO_FACT_NAMESPACE;
			cdmClass = FieldObservation.class;
			idSet = taxonIdSet;
			Map<String, FieldObservation> fieldObservationMap = (Map<String, FieldObservation>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, fieldObservationMap);

			//terms
			nameSpace = AlgaTerraSpecimenImport.TERMS_NAMESPACE;
			cdmClass = FieldObservation.class;
			idSet = taxonIdSet;
			Map<String, DefinedTermBase> termMap = (Map<String, DefinedTermBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, termMap);

		
			
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	/**
	 * Use same TaxonDescription if two records belong to the same taxon 
	 * @param state 
	 * @param newTaxonId
	 * @param oldTaxonId
	 * @param oldDescription
	 * @param taxonMap
	 * @return
	 */
	private TaxonDescription getTaxonDescription(AlgaTerraImportState state, int newTaxonId, Map<String, TaxonBase> taxonMap, int factId, Reference<?> sourceSec){
		TaxonDescription result = null;
		TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(newTaxonId));
		
		//TODO for testing
		if (taxonBase == null && ! state.getConfig().isDoTaxa()){
			taxonBase = Taxon.NewInstance(BotanicalName.NewInstance(Rank.SPECIES()), null);
		}
		
		Taxon taxon;
		if ( taxonBase instanceof Taxon ) {
			taxon = (Taxon) taxonBase;
		} else if (taxonBase != null) {
			logger.warn("TaxonBase for Fact(Specimen) with factId" + factId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
			return null;
		} else {
			logger.warn("TaxonBase for Fact(Specimen) " + factId + " is null.");
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
		return result;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraSpecimenImportValidator();
		return validator.validate(state);
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getTableName()
	 */
	@Override
	protected String getTableName() {
		return dbTableName;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getPluralString()
	 */
	@Override
	public String getPluralString() {
		return pluralString;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoSpecimen();
	}
	
}
