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
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.validation.CentralAfricaFernsTaxonImportValidator;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


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
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Types XXX", "Method Mapper does not work yet. Needs implementation for all 5 types. FIXMEs in implementation"));

//			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeTypes", ResultSet.class, TaxonBase.class, CentralAfricaFernsImportState.class));
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("Notes", AnnotationType.EDITORIAL()));

			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "makeCommonName", ResultSet.class, CentralAfricaFernsImportState.class));
			
			//not yet implemented or ignore
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Basionym of", "Needs better understanding"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Synonym of", "Needs better understanding. Strange values like "));
			
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Author/s - full", "Difference to Author/s abbreviated needs to be clarified. Do authors belong to reference? Sometimes authors are not equal to name authors"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Author/s abbreviated" , "Difference to Author/s - full needs to be clarified. Do authors belong to reference? Sometimes authors are not equal to name authors"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Reference abbreviated" , "Clarify relationship to reference tables, authors and to Reference full"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Chromosome number" , "Wrong data. Seems to be 'reference full'"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Journal volume" , "no comment"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Journal part" , "no comment"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Journal fascicle" , "What is this?"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Journal pages" , "What is this?"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Illustrations/s" , "What is this?"));

			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Date published" , "Needs implementation for parsing"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book / Paper title" , "Needs implementation. Inreferences?"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Book Publisher & Place" , "How to access the reference via String mapper?"));
				
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Nom remarks" , "Needs parsing for status, homonyms etc., the rest goes to a name annotation"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Distribution - Country" , "Needs mapping to TDWG or ISO"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Distribution - Province" , "Very few. By hand. Mapping to TDWG4?"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Distribution - detailed" , "Few. Textdata. Sometimes similar to Distribution - Province entries"));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Reprint no" , "What's this?"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Date verified" , "Needed?"));
			
			DbImportMethodMapper.NewInstance(this, "makeEcology", ResultSet.class, CentralAfricaFernsImportState.class);
//			mapping.addMapper(DbImportTextDataCreationMapper.NewInstance(dbIdAttribute, objectToCreateNamespace, dbTaxonFkAttribute, taxonNamespace, dbTextAttribute, Language.ENGLISH(), Feature.ECOLOGY(), null));
			
			
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
	private TaxonBase makeCommonName(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		String taxonNumber = rs.getString("Taxon number");
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
				logger.warn("Taxon with common name is of type synonym but must be accepted taxon: " + taxonNumber);
			}
		}
		return taxonBase;
	}
	
	/**
	 * for internal use only, used by MethodMapper
	 * @param commonNames 
	 */
	private TaxonBase makeEcology(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException{
		String taxonNumber = rs.getString("Taxon number");
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
				logger.warn("Taxon with ecology is of type synonym but must be accepted taxon: " + taxonNumber);
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
		
		//set epithets
		
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
	private TaxonBase makeTaxon(BotanicalName taxonName, Reference sec,
			String taxonNumber, String status) {
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
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(CentralAfricaFernsImportState state){
		IOValidator<CentralAfricaFernsImportState> validator = new CentralAfricaFernsTaxonImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(CentralAfricaFernsImportState state){
		return ! state.getConfig().isDoTaxa();
	}



}
