/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.validation.CentralAfricaFernsTaxonImportValidator;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class CentralAfricaFernsTaxonImport  extends CentralAfricaFernsImportBase<TaxonBase> implements IMappingImport<TaxonBase, CentralAfricaFernsImportState>{
	private static final Logger logger = Logger.getLogger(CentralAfricaFernsTaxonImport.class);
	
	public static final UUID TNS_EXT_UUID = UUID.fromString("41cb0450-ac84-4d73-905e-9c7773c23b05");
	
	
	private DbImportMapping mapping;
	
	//second path is not used anymore, there is now an ErmsTaxonRelationImport class instead
	private boolean isSecondPath = false;
	
	private static final String pluralString = "taxa";
	private static final String dbTableName = "[African pteridophytes]";
	private static final Class cdmTargetClass = TaxonBase.class;

	public CentralAfricaFernsTaxonImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}
	
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT [Taxon number] FROM " + dbTableName ;
		return strQuery;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getMapping()
	 */
	protected DbImportMapping getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping();
			
			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "Taxon number", TAXON_NAMESPACE)); //id + tu_status

//			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeTypes", ResultSet.class, TaxonBase.class, CentralAfricaFernsImportState.class));
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("Notes", AnnotationType.EDITORIAL()));

			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeCommonName", ResultSet.class, CentralAfricaFernsImportState.class));
			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeReferences", ResultSet.class, CentralAfricaFernsImportState.class));
			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeEcology", ResultSet.class, CentralAfricaFernsImportState.class));
			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeDistribution", ResultSet.class, CentralAfricaFernsImportState.class ));
			
//			mapping.addMapper(DbImportTextDataCreationMapper.NewInstance(dbIdAttribute, objectToCreateNamespace, dbTaxonFkAttribute, taxonNamespace, dbTextAttribute, Language.ENGLISH(), Feature.ECOLOGY(), null));

			//not yet implemented or ignore
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Types XXX", "Method Mapper does not work yet. Needs implementation for all 5 types. FIXMEs in implementation"));

			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Basionym of", "Needs better understanding"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Synonym of", "Needs better understanding. Strange values like "));
			
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Chromosome number" , "Wrong data. Seems to be 'reference full'"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Paper title" , "Needs implementation. Inreferences?"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book Publisher & Place" , "How to access the reference via String mapper?"));
				
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Nom remarks" , "Needs parsing for status, homonyms etc., the rest goes to a name annotation"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Reprint no" , "What's this?"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Date verified" , "Needed?"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Illustrations - non-original" , "What's this?"));
			
//			
//			UUID credibilityUuid = ErmsTransformer.uuidCredibility;
//			mapping.addMapper(DbImportExtensionMapper.NewInstance("tu_credibility", credibilityUuid, "credibility", "credibility", "credibility")); //Werte: null, unknown, marked for deletion
//			
			//ignore
//			mapping.addMapper(DbIgnoreMapper.NewInstance("cache_citation", "citation cache not needed in PESI"));
			
			//not yet implemented or ignore
//			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("tu_hidden", "Needs DbImportMarkerMapper implemented"));
			
		}
		return mapping;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(CentralAfricaFernsImportConfigurator config) {
		String strSelect = " SELECT * ";
		String strFrom = " FROM [African pteridophytes] as ap";
		String strWhere = " WHERE ( ap.[taxon number] IN (" + ID_LIST_TOKEN + ") )";
		String strOrderBy = " ORDER BY [Taxon number]";
		String strRecordQuery = strSelect + strFrom + strWhere + strOrderBy;
		return strRecordQuery;
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
				Set<String> nameIdSet = new HashSet<String>();
				Set<String> referenceIdSet = new HashSet<String>();
				while (rs.next()){
	//				handleForeignKey(rs, nameIdSet, "PTNameFk");
	//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
				}

			//reference map
//			nameSpace = "Reference";
//			cdmClass = Reference.class;
//			Map<String, Person> referenceMap = (Map<String, Person>)getCommonService().getSourcedObjectsByIdInSource(Person.class, teamIdSet, nameSpace);
//			result.put(Reference.class, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	private TaxonBase makeTypes(ResultSet rs, TaxonBase taxonBase, CentralAfricaFernsImportState state) throws SQLException{
		TaxonNameBase name = taxonBase.getName();
		String typeString = rs.getString("Type");
		String typeCollectorString = rs.getString("Type collector and number");
		String typeLocationString = rs.getString("Type location");
		makeSingleType(name, typeString, typeCollectorString, typeLocationString);
		return taxonBase;
	}
	
	
	private void makeSingleType(TaxonNameBase name, String typeString, String typeCollectorString, String typeLocationString) {
		DerivedUnitFacade type = DerivedUnitFacade.NewInstance(DerivedUnitType.Specimen);
		makeTypeCollectorInfo(type, typeCollectorString);
		type.setLocality(typeString);
		//TODO
//		type.addDuplicate(duplicateSpecimen);
		//FIXME handle also NameTypeDesignations
		SpecimenTypeDesignation designation = SpecimenTypeDesignation.NewInstance();
		designation.setTypeSpecimen(type.innerDerivedUnit());
		name.addTypeDesignation(designation, false);
	}



	private void makeTypeCollectorInfo(DerivedUnitFacade type, String collectorAndNumberString) {
		String reNumber = "(s\\.n\\.|\\d.*)";
		Pattern reNumberPattern = Pattern.compile(reNumber);
		Matcher matcher = reNumberPattern.matcher(collectorAndNumberString);
		
		if ( matcher.find()){
			int numberStart = matcher.start();
			String number = collectorAndNumberString.substring(numberStart).trim();
			String collectorString = collectorAndNumberString.substring(0, numberStart -1).trim();
			type.setCollectorsNumber(number);
			Team team = Team.NewTitledInstance(collectorString, collectorString);
			type.setCollector(team);
			
		}else{
			logger.warn("collector string did not match number pattern: " + collectorAndNumberString);
			
		}
	}

	/**
	 * for internal use only, used by MethodMapper
	 */
	private TaxonBase makeDistribution(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		try {
			String taxonNumber = state.getTaxonNumber();
//			logger.info(taxonNumber);
			TaxonBase<?> taxonBase = state.getRelatedObject(state.CURRENT_OBJECT_NAMESPACE, state.CURRENT_OBJECT_ID, TaxonBase.class);
			String countriesString = rs.getString("Distribution - Country");
			String province = rs.getString("Distribution - Province");
			String distributionDetailed = rs.getString("Distribution - detailed");
			TaxonNameBase nameUsedInSource = taxonBase.getName();
			Taxon taxon;
			if (taxonBase.isInstanceOf(Taxon.class)){
				taxon = CdmBase.deproxy(taxonBase, Taxon.class);
			}else{
				logger.warn("Distributions for synonyms not yet supported");
				taxon = Taxon.NewInstance(null, null); 
			}

			if (StringUtils.isNotBlank(countriesString) ){
				makeCountries(state, taxonNumber, taxon, nameUsedInSource, countriesString, province, distributionDetailed);
			}
			makeProvince(taxon, province);
			makeDistributionDetailed(taxon, distributionDetailed);

			return taxonBase;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	/**
	 * @param state
	 * @param taxonNumber
	 * @param taxonBase
	 * @param countriesString
	 */
	private void makeCountries(CentralAfricaFernsImportState state, String taxonNumber, Taxon taxon, TaxonNameBase nameUsedInSource, String countriesString, String province, String distributionDetailed) {
		countriesString = countriesString.replaceAll("\\*", "");  
		countriesString = countriesString.replace("  ", " ");
		countriesString = countriesString.replace(", endemic", " - endemic");
		countriesString = countriesString.replace("(endemic)", " - endemic");
		countriesString = countriesString.replace("(introduced)", " - introduced");
		countriesString = countriesString.replace("(naturalised)", " - naturalised");
		countriesString = countriesString.replace("Madagascar-", "Madagascar -");
		countriesString = countriesString.replace("Mahé", "Mahe");
		 
		String[] split = countriesString.split("[,;]");
		String remainingString = null;
		for (String countryString : split){
			countryString = CdmUtils.concat(", ", remainingString , countryString);
			if (countryString.matches(".*\\(.*") && ! countryString.matches(".*\\).*")){
				remainingString = countryString;
				continue;
			}
			remainingString = null;
			try {
				makeSingleCountry(state, taxonNumber, taxon, nameUsedInSource, countryString.trim());
			} catch (UndefinedTransformerMethodException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void makeDistributionDetailed(Taxon taxon, String distributionDetailed) {
		if (StringUtils.isNotBlank(distributionDetailed)){
			TaxonDescription description = getTaxonDescription(taxon, false, true);
			TextData distribution = TextData.NewInstance(Feature.DISTRIBUTION());
			description.addElement(distribution);
			distribution.putText(distributionDetailed, Language.ENGLISH());
		}
	}
	
	private void makeProvince(Taxon taxon, String province) {
		if (StringUtils.isNotBlank(province)){
			TaxonDescription description = getTaxonDescription(taxon, false, true);
			TextData distribution = TextData.NewInstance(Feature.DISTRIBUTION());
			description.addElement(distribution);
			distribution.putText(province, Language.ENGLISH());
		}
	}



	private void makeSingleCountry(CentralAfricaFernsImportState state, String taxonNumber, Taxon taxon, TaxonNameBase nameUsedInSource, String country) throws UndefinedTransformerMethodException {
		boolean areaDoubtful = false;
		Distribution distribution = Distribution.NewInstance(null, PresenceTerm.PRESENT());
		distribution.addSource(taxonNumber, "Distribution_Country", state.getConfig().getSourceReference(), null, nameUsedInSource, null);
		NamedArea area = null;
		//empty
		if (StringUtils.isBlank(country)){
			return;
		}
		country = country.trim();
		//doubtful
		if (country.startsWith("?")){
			areaDoubtful = true;
			country = country.substring(1).trim();
		}
		//status
		country = makeCountryStatus(state, country, distribution);
		
		//brackets
		country = makeCountryBrackets(state, taxonNumber, taxon, nameUsedInSource, country);
		String countryWithoutIslands = null;
		String countryWithoutDot = null;
		if (country.endsWith(" Isl.") || country.endsWith(" isl.") ){
			countryWithoutIslands = country.substring(0, country.length()-5);
		}
		if (country.endsWith(".")){
			countryWithoutDot = country.substring(0, country.length()-1);
		}
		if (country.endsWith("*")){
			country = country.substring(0, country.length()-1);
		}
		if (country.endsWith("Islands")){
			country = country.replace("Islands", "Is.");
		}
		
		
		//areas
		if (TdwgArea.isTdwgAreaLabel(country)){
			//tdwg
			area = TdwgArea.getAreaByTdwgLabel(country);
		}else if (TdwgArea.isTdwgAreaLabel(countryWithoutIslands)){
			//tdwg
			area = TdwgArea.getAreaByTdwgLabel(countryWithoutIslands);
		}else if (TdwgArea.isTdwgAreaLabel(countryWithoutDot)){
			//tdwg
			area = TdwgArea.getAreaByTdwgLabel(countryWithoutDot);
		}else if ( (area = state.getTransformer().getNamedAreaByKey(country)) != null) {
			//area already set
		}else if (WaterbodyOrCountry.isWaterbodyOrCountryLabel(country)){
			//iso
			area = WaterbodyOrCountry.getWaterbodyOrCountryByLabel(country);
		}else{
			//others
			NamedAreaLevel level = null;
			NamedAreaType areaType = null;
			
			UUID uuid = state.getTransformer().getNamedAreaUuid(country);
			if (uuid == null){
				logger.error(taxonNumber + " - Unknown country: " + country);
			}
			area = getNamedArea(state, uuid, country, country, country, areaType, level);
		}
		
		distribution.setArea(area);
		if (areaDoubtful == true){
			if (distribution.getStatus().equals(PresenceTerm.PRESENT())){
				distribution.setStatus(PresenceTerm.PRESENT_DOUBTFULLY());
			}
		}
		TaxonDescription description = getTaxonDescription(taxon, false, true);
		description.addElement(distribution);
	}



	private String makeCountryBrackets(CentralAfricaFernsImportState state, String taxonNumber, Taxon taxon, TaxonNameBase nameUsedInSource, String country) {
		String[] split = (country + " ").split("\\(.*\\)");
		if (split.length == 2){
			String bracket = country.substring(split[0].length()+1, country.indexOf(")"));
			country = split[0].trim();
			makeCountries(state, taxonNumber, taxon, nameUsedInSource, bracket, null, null);
		}else if (split.length ==1){
			//do nothing
		}else{
			logger.warn("Illegal length");
		}
		return country;
	}



	private String makeCountryStatus(CentralAfricaFernsImportState state, String country, Distribution distribution) throws UndefinedTransformerMethodException {
		PresenceTerm status = null;
		String[] split = country.split(" - ");
		
		if (split.length == 2){
			country = split[0].trim();
			String statusString = split[1];
			statusString = statusString.replace(".", "");
			status = state.getTransformer().getPresenceTermByKey(statusString);
			if (status == null){
				logger.warn("No status found: "+  statusString);
			}
//			UUID uuid = null;
//			status = getPresenceTerm(state, uuid, statusString, statusString, null);
		}else if (split.length == 1){
			//nothing to do
		}else{
			logger.warn("Invalid length: " + split.length);
		}
		if (status != null){
			distribution.setStatus(status);
		}
		return country;
	}



	/**
	 * for internal use only, used by MethodMapper
	 */
	private TaxonBase makeReferences(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		String taxonNumber = state.getTaxonNumber();
		String referenceFullString = rs.getString("Reference full");
		String referenceAbbreviatedString = rs.getString("Reference - abbreviated");
		String volume = rs.getString("Book / Journal volume");
		String pages = rs.getString("Book / Journal pages");
		String illustrations = rs.getString("Illustration/s");
		
		String fascicle = rs.getString("Book / Journal fascicle");
		String part = rs.getString("Book / Journal part");
		String paperTitle = rs.getString("Book / Paper title");
		
		
		
		String datePublishedString = rs.getString("Date published");
		String referenceString = referenceFullString;
		if (StringUtils.isBlank(referenceString)){
			referenceString = referenceAbbreviatedString;
		}
		
		TaxonBase<?> taxonBase = state.getRelatedObject(state.CURRENT_OBJECT_NAMESPACE, state.CURRENT_OBJECT_ID, TaxonBase.class);
		if (StringUtils.isNotBlank(referenceString) || StringUtils.isNotBlank(volume) || 
					StringUtils.isNotBlank(pages) || StringUtils.isNotBlank(illustrations) || 
					StringUtils.isNotBlank(datePublishedString) || StringUtils.isNotBlank(paperTitle)){
			NonViralName name = CdmBase.deproxy(taxonBase.getName(), NonViralName.class);
			Reference reference = ReferenceFactory.newGeneric();
			reference.setAuthorTeam((TeamOrPersonBase)name.getCombinationAuthorTeam());
			reference.setTitle(referenceString);
			reference.setVolume(volume);
			reference.setEdition(part);
			Reference inrefernce = null;
			//TODO parser
			TimePeriod datePublished = TimePeriod.parseString(datePublishedString);
			reference.setDatePublished(datePublished);
			if (StringUtils.isNotBlank(paperTitle)){
				Reference innerReference = ReferenceFactory.newGeneric();
				innerReference.setDatePublished(datePublished);
				name.setNomenclaturalReference(innerReference);
				innerReference.setInReference(reference);
				reference = innerReference;
			}else{
				name.setNomenclaturalReference(reference);
			}
			
			//details
			String details = CdmUtils.concat(", ", pages, illustrations);
			details = StringUtils.isBlank(details) ? null : details.trim();
			name.setNomenclaturalMicroReference(details);
			try {
				UUID uuidFascicle = state.getTransformer().getExtensionTypeUuid("fascicle");
				ExtensionType extensionType = getExtensionType(state, uuidFascicle, "Fascicle", "Fascicle", null);
				reference.addExtension(fascicle, extensionType);
			} catch (UndefinedTransformerMethodException e) {
				e.printStackTrace();
			}
			
		}else{
			logger.warn(taxonNumber + " - Taxon has no reference");
		}
		return taxonBase;
	}

	/**
	 * for internal use only, used by MethodMapper
	 */
	private TaxonBase makeCommonName(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		String taxonNumber = state.getTaxonNumber();
		String commonNames = rs.getString("Common names");
		TaxonBase<?> taxonBase = state.getRelatedObject(state.CURRENT_OBJECT_NAMESPACE, state.CURRENT_OBJECT_ID, TaxonBase.class);
		if (StringUtils.isNotBlank(commonNames)){
			if (taxonBase.isInstanceOf(Taxon.class)){
				Taxon taxon = (Taxon)taxonBase;
				TaxonDescription description = getTaxonDescription(taxon, false, true);
				String[] split = commonNames.split(",");
				for (String commonNameString: split){
					CommonTaxonName commonName = CommonTaxonName.NewInstance(commonNameString.trim(), Language.ENGLISH());
					description.addElement(commonName);				
				}
			}else{
				logger.warn(taxonNumber + " - Taxon with common name is of type synonym but must be accepted taxon: " + taxonBase.getName().getTitleCache());
			}
		}
		return taxonBase;
	}
	
	/**
	 * for internal use only, used by MethodMapper
	 * @param commonNames 
	 */
	private TaxonBase makeEcology(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		String taxonNumber = state.getTaxonNumber();
		String ecologyString = rs.getString("Ecology");
		TaxonBase<?> taxonBase = state.getRelatedObject(state.CURRENT_OBJECT_NAMESPACE, state.CURRENT_OBJECT_ID, TaxonBase.class);
		if (StringUtils.isNotBlank(ecologyString)){
			if (taxonBase.isInstanceOf(Taxon.class)){
				Taxon taxon = (Taxon)taxonBase;
				TaxonDescription description = getTaxonDescription(taxon, false, true);
				TextData ecology = TextData.NewInstance(Feature.ECOLOGY());
				ecology.putText(ecologyString.trim(), Language.ENGLISH());
				description.addElement(ecology);				
			}else{
				logger.warn(taxonNumber + " - Taxon with ecology is of type synonym but must be accepted taxon: " + taxonBase.getName().getTitleCache());
			}
		}
		return taxonBase;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet)
	 */
	public TaxonBase createObject(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException {
		BotanicalName taxonName = BotanicalName.NewInstance(null);
		Reference sec = state.getConfig().getSourceReference();
		
		String taxonNumber = rs.getString("Taxon number");
		state.setTaxonNumber(taxonNumber);
		
		String orderName = rs.getString("Order name");
		String subOrderName = rs.getString("Suborder name");
		String familyName = rs.getString("Family name");
		String subFamilyName = rs.getString("Subfamily name");
		String tribusName = rs.getString("Tribus name");
		String subTribusName = rs.getString("Subtribus name");
		String sectionName = rs.getString("Section name");
		String genusName = rs.getString("Genus name");
		String subGenusName = rs.getString("Subgenus name");
		String seriesName = rs.getString("Series name");
		String specificEpihet = rs.getString("Specific epihet");
		String subspeciesName = rs.getString("Subspecies name");
		String varietyName = rs.getString("Variety name");
		String subFormaName = rs.getString("Subforma");
		String subVariety = rs.getString("Subvariery");
		String formaName = rs.getString("Forma name");
		String subsectionName = rs.getString("Subsection name");
		
		String status = rs.getString("Current/Synonym");
		
		TaxonBase taxon = makeTaxon(taxonName, sec, taxonNumber, status);
		
//			Integer parent3Rank = rs.getInt("parent3rank");
		
		//rank and epithets
		Rank lowestRank = setLowestUninomial(taxonName, orderName,  subOrderName, familyName, subFamilyName, tribusName, subTribusName,genusName);
		lowestRank = setLowestInfraGeneric(taxonName, lowestRank, subGenusName, sectionName, subsectionName, seriesName);
		if (StringUtils.isNotBlank(specificEpihet)){
			taxonName.setSpecificEpithet(specificEpihet);
			lowestRank = Rank.SPECIES();
		}
		lowestRank = setLowestInfraSpecific(taxonName, lowestRank, subspeciesName,  varietyName, subVariety, formaName,subFormaName);
		
		taxonName.setRank(lowestRank);
		setAuthor(taxonName, rs, taxonNumber, false);
		
		//add original source for taxon name (taxon original source is added in mapper
		Reference citation = state.getConfig().getSourceReference();
//		addOriginalSource(taxonName, taxonNumber, TAXON_NAMESPACE, citation);
		return taxon;
		
	}



	/**
	 * Creates the taxon object depending on name, sec and status
	 * @param taxonName
	 * @param sec
	 * @param taxonNumber
	 * @param status
	 * @return
	 */
	private TaxonBase makeTaxon(BotanicalName taxonName, Reference sec, String taxonNumber, String status) {
		TaxonBase taxon;
		if ("c".equalsIgnoreCase(status)|| "incertus".equalsIgnoreCase(status) ){
			taxon = Taxon.NewInstance(taxonName, sec);
			if ("incertus".equalsIgnoreCase(status)){
				taxon.setDoubtful(true);
			}
		}else if ("s".equalsIgnoreCase(status)){
			taxon = Synonym.NewInstance(taxonName, sec);
		}else{
			logger.warn(taxonNumber + ": Status not given for taxon " );
			taxon = Taxon.NewUnknownStatusInstance(taxonName, sec);
		}
		return taxon;
	}


	private Rank setLowestInfraSpecific(BotanicalName taxonName, Rank lowestRank, String subspeciesName, String varietyName,
			String subVariety, String formaName, String subFormaName) {
		if (StringUtils.isNotBlank(subFormaName)){
			taxonName.setInfraSpecificEpithet(subFormaName);
			return Rank.SUBFORM();
		}else if (StringUtils.isNotBlank(formaName)){
			taxonName.setInfraSpecificEpithet(formaName);
			return Rank.FORM();
		}else if (StringUtils.isNotBlank(subVariety)){
			taxonName.setInfraSpecificEpithet(subVariety);
			return Rank.SUBVARIETY();
		}else if (StringUtils.isNotBlank(varietyName)){
			taxonName.setInfraSpecificEpithet(varietyName);
			return Rank.VARIETY();
		}else if (StringUtils.isNotBlank(subspeciesName)){
			taxonName.setInfraSpecificEpithet(subspeciesName);
			return Rank.SUBSPECIES();
		}else{
			return lowestRank;
		}
	}



	private Rank setLowestInfraGeneric(BotanicalName taxonName, Rank lowestRank, String subGenusName, String sectionName, String subSectionName, String seriesName) {
		if (StringUtils.isNotBlank(seriesName)){
			taxonName.setInfraGenericEpithet(seriesName);
			return Rank.SERIES();
		}else if (StringUtils.isNotBlank(subSectionName)){
			taxonName.setInfraGenericEpithet(subSectionName);
			return Rank.SUBSECTION_BOTANY();
		}else if (StringUtils.isNotBlank(sectionName)){
			taxonName.setInfraGenericEpithet(sectionName);
			return Rank.SECTION_BOTANY();
		}else if (StringUtils.isNotBlank(subGenusName)){
			taxonName.setInfraGenericEpithet(subGenusName);
			return Rank.SUBGENUS();
		}else{
			return lowestRank;
		}
	}



	private Rank setLowestUninomial(BotanicalName taxonName, String orderName, String subOrderName, String familyName, String subFamilyName,
			String tribusName, String subTribusName, String genusName) {
		
		if (StringUtils.isNotBlank(genusName)){
			taxonName.setGenusOrUninomial(genusName);
			return Rank.GENUS();
		}else if (StringUtils.isNotBlank(subTribusName)){
			taxonName.setGenusOrUninomial(subTribusName);
			return Rank.SUBTRIBE();
		}else if (StringUtils.isNotBlank(tribusName)){
			taxonName.setGenusOrUninomial(tribusName);
			return Rank.TRIBE();
		}else if (StringUtils.isNotBlank(subFamilyName)){
			taxonName.setGenusOrUninomial(subFamilyName);
			return Rank.SUBFAMILY();
		}else if (StringUtils.isNotBlank(familyName)){
			taxonName.setGenusOrUninomial(familyName);
			return Rank.FAMILY();
		}else if (StringUtils.isNotBlank(subOrderName)){
			taxonName.setGenusOrUninomial(subOrderName);
			return Rank.SUBORDER();
		}else if (StringUtils.isNotBlank(orderName)){
			taxonName.setGenusOrUninomial(orderName);
			return Rank.ORDER();
		}else{
			return null;
		}
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(CentralAfricaFernsImportState state){
		IOValidator<CentralAfricaFernsImportState> validator = new CentralAfricaFernsTaxonImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean isIgnore(CentralAfricaFernsImportState state){
		return ! state.getConfig().isDoTaxa();
	}



}
