/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.TdwgAreaProvider;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 12.09.2012
 */
public abstract class AlgaTerraSpecimenImportBase extends BerlinModelImportBase{
    private static final long serialVersionUID = -1741703900571072861L;

    private static final Logger logger = LogManager.getLogger();

	public static final String ECO_FACT_FIELD_OBSERVATION_NAMESPACE = "EcoFact_FieldObservation";
	public static final String ECO_FACT_DERIVED_UNIT_NAMESPACE = "EcoFact_DerivedUnit";
	public static final String TYPE_SPECIMEN_FIELD_OBSERVATION_NAMESPACE = "TypeSpecimen_FieldObservation";
	public static final String TYPE_SPECIMEN_DERIVED_UNIT_NAMESPACE = "TypeSpecimen_DerivedUnit";
	public static final String FACT_ECOLOGY_NAMESPACE = "Fact (Ecology)";


	public static final String TERMS_NAMESPACE = "ALGA_TERRA_TERMS";

	//TODO move to transformrer
	final static UUID uuidMarkerAlkalinity = UUID.fromString("e52d0ea2-0c1f-4d95-ae6d-e21ab317c594");
	final static UUID uuidRefSystemGps = UUID.fromString("c23e4928-c137-4e4a-b6ab-b430da3d0b94");
	public final static UUID uuidFeatureSpecimenCommunity = UUID.fromString("3ff5b1ab-3999-4b5a-b8f7-01fd2f6c12c7");
	public final static UUID uuidFeatureAdditionalData = UUID.fromString("0ac82ab8-2c2b-4953-98eb-a9f718eb9c57");
	public final static UUID uuidFeatureHabitatExplanation = UUID.fromString("6fe32295-61a3-44fc-9fcf-a85790ea888f");

	final static UUID uuidVocAlgaTerraClimate = UUID.fromString("b0a677c6-8bb6-43f4-b1b8-fc377a10feb5");
	final static UUID uuidVocAlgaTerraHabitat = UUID.fromString("06f30114-e19c-4e7d-a8e5-5488c41fcbc5");
	final static UUID uuidVocAlgaTerraLifeForm = UUID.fromString("3c0b194e-809c-4b42-9498-6ff034066ed7");

	public final static UUID uuidFeatureAlgaTerraClimate = UUID.fromString("8754674c-9ab9-4f28-95f1-91eeee2314ee");
	public final static UUID uuidFeatureAlgaTerraHabitat = UUID.fromString("7def3fc2-cdc5-4739-8e13-62edbd053415");
	public final static UUID uuidFeatureAlgaTerraLifeForm = UUID.fromString("9b657901-1b0d-4a2a-8d21-dd8c1413e2e6");

	final static UUID uuidVocParameter = UUID.fromString("45888b40-5bbb-4293-aa1e-02479796cd7c");
	final static UUID uuidStatMeasureSingleValue = UUID.fromString("eb4c3d98-4d4b-4c37-8eb4-17315ce79920");
	final static UUID uuidMeasurementValueModifier = UUID.fromString("0218a7a3-f6c0-4d06-a4f8-6b50b73aef5e");

	final static UUID uuidModifierLowerThan = UUID.fromString("2b500085-6bef-4003-b6ea-e0ad0237d79d");
	final static UUID uuidModifierGreaterThan = UUID.fromString("828df49d-c745-48f7-b083-0ada43356c34");

	public AlgaTerraSpecimenImportBase(String tableName, String pluralString) {
		super(tableName, pluralString);
	}

	/**
	 * Creates the vocabularies and the features for Climate, Habitat and Lifeform
	 * @param state
	 * @throws SQLException
	 */
	protected void makeVocabulariesAndFeatures(AlgaTerraImportState state) throws SQLException {
		String abbrevLabel = null;
		URI uri = null;

		if (! state.isSpecimenVocabulariesCreated()){

			TransactionStatus txStatus = this.startTransaction();

			boolean isOrdered = true;
			State tmp = State.NewInstance();
			OrderedTermVocabulary<State> climateVoc = (OrderedTermVocabulary<State>)getVocabulary(state, TermType.State, uuidVocAlgaTerraClimate, "Climate", "Climate", abbrevLabel, uri, isOrdered, tmp);
			OrderedTermVocabulary<State> habitatVoc = (OrderedTermVocabulary<State>)getVocabulary(state, TermType.State, uuidVocAlgaTerraHabitat, "Habitat", "Habitat", abbrevLabel, uri, isOrdered, tmp);
			OrderedTermVocabulary<State> lifeformVoc = (OrderedTermVocabulary<State>)getVocabulary(state, TermType.State, uuidVocAlgaTerraLifeForm, "Lifeform", "Lifeform", abbrevLabel, uri, isOrdered, tmp);

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

			//material category
			TermVocabulary<DefinedTerm> materialCategoryVoc = getVocabulary(state, TermType.KindOfUnit, AlgaTerraImportTransformer.uuidKindOfUnitVoc, "Alga Terra Material Category", "Alga Terra Material Category", abbrevLabel, uri, false, DefinedTerm.NewKindOfUnitInstance(null, null, null));
			getVocabularyService().save(materialCategoryVoc);

			String materialSql = "SELECT * FROM MaterialCategory WHERE MaterialCategoryId <> 16 ";
			rs = source.getResultSet(materialSql);
			while (rs.next()){
				Integer id = rs.getInt("MaterialCategoryId");
				String category = rs.getString("MaterialCategory");
				String description = rs.getString("Description");
				UUID uuid = UUID.randomUUID();

				DefinedTerm kindOfUnit = DefinedTerm.NewKindOfUnitInstance(description, category, null);
				kindOfUnit.setUuid(uuid);
				addOriginalSource(kindOfUnit, id.toString(), "MaterialCategory", state.getTransactionalSourceReference());
				materialCategoryVoc.addTerm(kindOfUnit);
				getTermService().saveOrUpdate(kindOfUnit);
				materialCategoryMapping.put(id, uuid);
			}

			//areas
			OrderedTermVocabulary<NamedArea> informalAreasVoc = (OrderedTermVocabulary<NamedArea>)getVocabulary(state, TermType.NamedArea, AlgaTerraImportTransformer.uuidNamedAreaVocAlgaTerraInformalAreas, "AlgaTerra Specific Areas", "AlgaTerra Specific Areas", abbrevLabel, uri, true, NamedArea.NewInstance());
			getVocabularyService().save(informalAreasVoc);

			String areaSql = "SELECT * FROM TDWGGazetteer WHERE subL4 = 1 ";
			rs = source.getResultSet(areaSql);
			while (rs.next()){
				String l1Code = rs.getString("L1Code");
				String l2Code = rs.getString("L2Code");
				String l3Code = rs.getString("L3Code");
				String l4Code = rs.getString("L4Code");
				String gazetteer = rs.getString("Gazetteer");
				Integer id = rs.getInt("ID");
				String notes = rs.getString("Notes");
				//TODO stable uuids
//				UUID uuid = UUID.fromString(rs.getString("UUID"));
				UUID uuid = UUID.randomUUID();
				subL4Mapping.put(id, uuid);

				String tdwgCode =  (l4Code != null) ? l4Code : (l3Code != null) ? l3Code : (l2Code != null) ? l2Code : l1Code;

				NamedArea tdwgArea = TdwgAreaProvider.getAreaByTdwgAbbreviation(tdwgCode);
				NamedArea newArea  = getNamedArea(state, uuid ,gazetteer, gazetteer, null, null, null, informalAreasVoc, TermMatchMode.UUID_ONLY, null);
				if (isNotBlank(notes)){
					newArea.addAnnotation(Annotation.NewInstance(notes, AnnotationType.EDITORIAL(), Language.DEFAULT()));
				}

				addOriginalSource(newArea, id.toString(), "TDWGGazetteer", state.getTransactionalSourceReference());
				getTermService().saveOrUpdate(newArea);
				newArea.setPartOf(tdwgArea);
				informalAreasVoc.addTerm(newArea);
			}

			this.commitTransaction(txStatus);

			state.setSpecimenVocabulariesCreated(true);
		}
	}

	//tmp
	static Map<Integer, UUID> subL4Mapping = new HashMap<>();
	static Map<Integer, UUID> materialCategoryMapping = new HashMap<>();

	protected String getLocalityString(){
		return "Locality";
	}

	protected void handleFieldObservationSpecimen(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state, ResultSetPartitioner partitioner) throws SQLException {
		//FIXME missing fields #3084, #3085, #3080
		try {

			Integer unitId = nullSafeInt(rs, "unitId");
			String locality = rs.getString(getLocalityString());
			Double latitude = nullSafeDouble(rs, "Latitude");
			Double longitude = nullSafeDouble(rs, "Longitude");
			Integer errorRadius = nullSafeInt(rs,"Prec");
			String geoCodeMethod = rs.getString("GeoCodeMethod");

			Integer altitude = nullSafeInt(rs, "Altitude");
			Integer lowerAltitude = nullSafeInt(rs, "AltitudeLowerValue");
			String altitudeUnit = rs.getString("AltitudeUnit");
			Double depth = nullSafeDouble(rs, "Depth");
			Double depthLow = nullSafeDouble(rs, "DepthLow");

			String collectorsNumber = rs.getString("CollectorsNumber");
			Date collectionDateStart = rs.getDate("CollectionDate");
			Date collectionDateEnd = rs.getDate("CollectionDateEnd");

			//location
			facade.setLocality(locality);

			//exact location
			ReferenceSystem referenceSystem = makeRefrenceSystem(geoCodeMethod, state);
			if (longitude != null || latitude != null || referenceSystem != null || errorRadius != null){
				Point exactLocation = Point.NewInstance(longitude, latitude, referenceSystem, errorRadius);
				facade.setExactLocation(exactLocation);
			}

			//altitude, depth
			if (StringUtils.isNotBlank(altitudeUnit) && ! altitudeUnit.trim().equalsIgnoreCase("m")){
				logger.warn("Altitude unit is not [m] but: " +  altitudeUnit);
			}
			if ( altitude != null){
				if (lowerAltitude == null){
					facade.setAbsoluteElevation(altitude);
				}else{
					facade.setAbsoluteElevationRange(lowerAltitude,altitude);
			   	}
			}
			if ( depth != null){
				if (depthLow == null){
					facade.setDistanceToWaterSurface(depth);
				}else{
					//TODO which direction is correct?
					facade.setDistanceToWaterSurfaceRange(depth, depthLow);
			   	}
			}

			//field
			facade.setFieldNumber(collectorsNumber);
			TimePeriod gatheringPeriod = TimePeriod.NewInstance(collectionDateStart, collectionDateEnd);
			facade.setGatheringPeriod(gatheringPeriod);
			handleCollectorTeam(state, facade, rs);

			//areas
			makeAreas(state, rs, facade);

			//notes
			//=> not required according to Henning

			//id, created, updated, notes
			if (unitId != null){
				this.doIdCreatedUpdatedNotes(state, facade.innerFieldUnit(), rs, unitId, getFieldObservationNameSpace());
			}else{
				logger.warn("FieldObservation has no unitId: " +  facade.innerFieldUnit() + ": " + getFieldObservationNameSpace());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	protected void handleFirstDerivedSpecimen(ResultSet rs, DerivedUnitFacade facade, AlgaTerraImportState state, ResultSetPartitioner partitioner) throws SQLException {
		Integer unitId = nullSafeInt(rs, "unitId");
		Integer collectionFk = nullSafeInt(rs,"CollectionFk");
		String label = rs.getString("Label");

		//collection
		if (collectionFk != null){
			Collection subCollection = state.getRelatedObject(AlgaTerraCollectionImport.NAMESPACE_SUBCOLLECTION, String.valueOf(collectionFk), Collection.class);
			if (subCollection != null){
				facade.setCollection(subCollection);
			}else{
				Collection collection = state.getRelatedObject(AlgaTerraCollectionImport.NAMESPACE_COLLECTION, String.valueOf(collectionFk), Collection.class);
				if (collection == null){
					logger.warn("Collection for collectionFK " + collectionFk + " can not be found.");
				}
				facade.setCollection(collection);
			}
		}

		//Label
		if (isNotBlank(label)){
			//TODO implement label #4218, #3090, #3084
			logger.warn("Label not yet implemented for specimen, #4218, #3090, #3084");
		}

		//TODO id, created for fact +  ecoFact
		//    	this.doIdCreatedUpdatedNotes(state, descriptionElement, rs, id, namespace);
		if (unitId != null){
			this.doIdCreatedUpdatedNotes(state, facade.innerDerivedUnit(), rs, unitId, getDerivedUnitNameSpace());
		}else{
			logger.warn("Specimen has no unitId: " +  facade.innerDerivedUnit() + ": " + getDerivedUnitNameSpace());
		}
	}



	protected abstract String getDerivedUnitNameSpace();

	protected abstract String getFieldObservationNameSpace();


	protected DescriptionBase<?> getFieldObservationDescription(DerivedUnitFacade facade) {
		Set<DescriptionBase<?>> descriptions = (Set)facade.innerFieldUnit().getDescriptions();
		for (DescriptionBase<?> desc : descriptions){
			if (desc.isImageGallery() == false){
				return desc;
			}
		}
		SpecimenDescription specDesc = SpecimenDescription.NewInstance(facade.innerFieldUnit());
		descriptions.add(specDesc);
		return specDesc;
	}


	private void makeAreas(AlgaTerraImportState state, ResultSet rs, DerivedUnitFacade facade) throws SQLException {
	   	Integer gazetteerId = nullSafeInt(rs, "GazetteerId");
	   	if (gazetteerId != null){
	   		//TDWG
	   		NamedArea tdwgArea;
	   		String tdwg4 = rs.getString("L4Code");
	   		if (isNotBlank(tdwg4)){
	   			tdwgArea = TdwgAreaProvider.getAreaByTdwgAbbreviation(tdwg4);
	   		}else{
	   			String tdwg3 = rs.getString("L3Code");
	   			if (isNotBlank(tdwg3)){
	   				tdwgArea = TdwgAreaProvider.getAreaByTdwgAbbreviation(tdwg3);
	   			}else{
	   				Number tdwg2D = nullSafeDouble(rs, "L2Code");
	   				if (tdwg2D != null){
	   					Integer tdwg2 = tdwg2D.intValue();
		   				tdwgArea = TdwgAreaProvider.getAreaByTdwgAbbreviation(String.valueOf(tdwg2));
	   				}else{
	   					Number tdwg1D = nullSafeDouble(rs, "L1Code");
		   				if (tdwg1D != null){
		   					Integer tdwg1 = tdwg1D.intValue();
		   					tdwgArea = TdwgAreaProvider.getAreaByTdwgAbbreviation(String.valueOf(tdwg1));
		   				}else{
		   					tdwgArea = null;
		   				}
	   				}
		   		}
	   		}
	   		if (tdwgArea == null){
	   			logger.warn("TDWG area could not be defined for gazetterId: " + gazetteerId);
	   		}else{
	   			facade.addCollectingArea(tdwgArea);
	   		}

	   		//Countries
	   		Country country = null;
	   		String isoCountry = rs.getString("ISOCountry");
	   		String countryStr = rs.getString("Country");
	   		if (isNotBlank(isoCountry)){
		   		country = Country.getCountryByIso3166A2(isoCountry);
	   		}else if (isNotBlank(countryStr)){
	   			logger.warn("Country exists but no ISO code");
	   		}else{

	   		}

	   		NamedArea subL4Area = null;
	   		Boolean subL4 = nullSafeBoolean(rs, "subL4");
	   		if (subL4 != null && subL4.booleanValue() == true){
	   			subL4Area = makeSubL4Area(state, gazetteerId);
	   			if (subL4Area != null){
	   				facade.addCollectingArea(subL4Area);
	   			}else{
	   				logger.warn("SubL4 area not found for gazetteerId: " + gazetteerId);
	   			}
	   		}

	   		if (country == null ){
	   			if (! gazetteerId.equals(40)){//special handling for Borneo, TDWG area is enough here as it matches exactly
		   			if (subL4Area == null ){
		   				logger.warn("Country does not exist and SubL4 could not be found for GazetteerID " + gazetteerId);
		   			}else {
		   				logger.info("Country could not be defined but subL4 area was added");
		   			}
	   			}
	   		}else{
	   			facade.setCountry(country);
	   		}

	   	}

	   	//Waterbody
	   	NamedArea waterbody = null;
	   	String waterbodyStr = rs.getString("WaterBody");
	   	if (isNotBlank(waterbodyStr)){
	   		if (waterbodyStr.equals("Atlantic Ocean")){
	   			waterbody = NamedArea.ATLANTICOCEAN();
	   		}else if (waterbodyStr.equals("Pacific Ocean")){
	   			waterbody = NamedArea.PACIFICOCEAN();
	   		}else if (waterbodyStr.equals("Indian Ocean")){
	   			waterbody = NamedArea.INDIANOCEAN();
	   		}else if (waterbodyStr.equals("Arctic Ocean")){
	   			waterbody = NamedArea.ARCTICOCEAN();
	   		}else{
	   			logger.warn("Waterbody not recognized: " + waterbody);
	   		}
	   		if (waterbody != null){
	   			facade.addCollectingArea(waterbody);
	   		}
	   	}


	   	//countries sub
	   	//TODO -> SpecimenImport (not existing in TypeSpecimen)
	}


	private NamedArea makeSubL4Area(AlgaTerraImportState state, Integer gazetteerId) {
		UUID uuid = subL4Mapping.get(gazetteerId);
		NamedArea area = (NamedArea)getTermService().find(uuid);
		if (area == null){
			logger.warn("SubL4 area could not be found in repository");
		}
		return area;
	}

	private boolean handleMissingCountry(AlgaTerraImportState state, DerivedUnitFacade facade, Integer gazetteerId) {
		NamedArea area = null;
		if (gazetteerId != null){
			if (gazetteerId.equals(42)){
				area = getNamedArea(state, AlgaTerraImportTransformer.uuidNamedAreaBorneo, null, null, null, null, null);
			}else if (gazetteerId.equals(1684)){
				area = getNamedArea(state, AlgaTerraImportTransformer.uuidNamedAreaPatagonia, null, null, null, null, null);
			}else if (gazetteerId.equals(2167)){
				area = getNamedArea(state, AlgaTerraImportTransformer.uuidNamedAreaTierraDelFuego, null, null, null, null, null);
			}
		}
		if (area != null){
			facade.addCollectingArea(area);
			return true;
		}
		return false;

	}

	protected SpecimenOrObservationType makeDerivedUnitType(String recordBasis) {
		SpecimenOrObservationType result = null;
		if (StringUtils.isBlank(recordBasis)){
			result = SpecimenOrObservationType.DerivedUnit;
		} else if (recordBasis.equalsIgnoreCase("FossileSpecimen")){
			result = SpecimenOrObservationType.Fossil;
		}else if (recordBasis.equalsIgnoreCase("HumanObservation")){
			result = SpecimenOrObservationType.HumanObservation;
		}else if (recordBasis.equalsIgnoreCase("Literature")){
			//FIXME
			logger.warn("Literature record basis not yet supported");
			result = SpecimenOrObservationType.DerivedUnit;
		}else if (recordBasis.equalsIgnoreCase("LivingSpecimen")){
			result = SpecimenOrObservationType.LivingSpecimen;
		}else if (recordBasis.equalsIgnoreCase("MachineObservation")){
			result = SpecimenOrObservationType.MachineObservation;
		}else if (recordBasis.equalsIgnoreCase("Observation")){
			result = SpecimenOrObservationType.Observation;
		}else if (recordBasis.equalsIgnoreCase("LivingCulture")){
			//FIXME
			logger.warn("LivingCulture record basis not yet supported");
			result = SpecimenOrObservationType.DerivedUnit;
		}else if (recordBasis.equalsIgnoreCase("PreservedSpecimen")){
			result = SpecimenOrObservationType.PreservedSpecimen;
		}
		return result;
	}


	protected Feature makeFeature(SpecimenOrObservationType type, AlgaTerraImportState state) {
		if (type.equals(SpecimenOrObservationType.DerivedUnit)){
			return Feature.INDIVIDUALS_ASSOCIATION();
		}else if (type.isFeatureObservation()){
			return Feature.OBSERVATION();
		}else if (type.isPreservedSpecimen()){
			return Feature.SPECIMEN();
		}else if (type.equals(SpecimenOrObservationType.LivingSpecimen)){
			UUID uuid = AlgaTerraImportTransformer.uuidFeatureLivingSpecimen;
			Feature feature = getFeature(state, uuid, "Living Specimen", "Living Specimen", null, Feature.SPECIMEN().getVocabulary());
			if (feature == null){
				logger.warn("Living Specimen Feature could not be created");
			}
			return feature;
		}
		logger.warn("No feature defined for derived unit type: " + type);
		return null;
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
			return ReferenceSystem.MAP();
		}else if(geoCodeMethod.startsWith("WikiProjekt Georeferenzierung") || geoCodeMethod.startsWith("http://toolserver.org/~geohack/geohack.php") ){
			return ReferenceSystem.WGS84();
		}else {
			logger.warn("Reference system " +  geoCodeMethod +  " not yet supported.");
			return null;
		}
	}




	private void handleCollectorTeam(AlgaTerraImportState state, DerivedUnitFacade facade, ResultSet rs) throws SQLException {
		String collector = rs.getString("Collector");
		TeamOrPersonBase<?> author = getAuthor(collector);
		facade.setCollector(author);
	}

	/**
	 * @param facade
	 * @param collector
	 */
	protected TeamOrPersonBase<?> getAuthor(String author) {
		// FIXME TODO parsen und deduplizieren
		Team team = Team.NewTitledInstance(author, author);
		return team;
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
	protected TaxonDescription getTaxonDescription(AlgaTerraImportState state, Taxon taxon, Reference sourceSec){
		TaxonDescription result = null;
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




}
