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
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.validation.CentralAfricaFernsTaxonImportValidator;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.ReferenceBase;
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
	
	private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
	
	private DbImportMapping mapping;
	
	//second path is not used anymore, there is now an ErmsTaxonRelationImport class instead
	private boolean isSecondPath = false;
	
	private int modCount = 10000;
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

			//not yet implemented or ignore
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Basionym of", "Needs better understanding"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Synonym of", "Needs better understanding. Strange values like "));
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Common names", "Very view values. Needs parsing for author"));
			
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
			
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("Ecology" , "Needs implementation"));
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
		String strRecordQuery = strSelect + strFrom + strWhere;
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
//			cdmClass = ReferenceBase.class;
//			Map<String, Person> referenceMap = (Map<String, Person>)getCommonService().getSourcedObjectsByIdInSource(Person.class, teamIdSet, nameSpace);
//			result.put(ReferenceBase.class, referenceMap);

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



	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet)
	 */
	public TaxonBase createObject(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException {
		BotanicalName taxonName = BotanicalName.NewInstance(null);
		ReferenceBase sec = state.getConfig().getSourceReference();
		
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
		
		TaxonBase taxon;
		if ("c".equalsIgnoreCase(status)){
			taxon = Taxon.NewInstance(taxonName, sec);
		}else if ("s".equalsIgnoreCase(status)){
			taxon = Synonym.NewInstance(taxonName, sec);
		}else{
			logger.warn(taxonNumber + ": Status not given for taxon " );
			taxon = Taxon.NewUnknownStatusInstance(taxonName, sec);
		}
		
//			Integer parent3Rank = rs.getInt("parent3rank");
		
		//rank and epithets
		Rank lowestRank = setLowestUninomial(taxonName, orderName,  subOrderName, familyName, subFamilyName, tribusName, subTribusName,sectionName, subsectionName, genusName);
		lowestRank = setLowestInfraGeneric(taxonName, lowestRank, subGenusName,  seriesName);
		if (StringUtils.isNotBlank(specificEpihet)){
			taxonName.setSpecificEpithet(specificEpihet);
			lowestRank = Rank.SPECIES();
		}
		lowestRank = setLowestInfraSpecific(taxonName, lowestRank, subspeciesName,  varietyName, subVariety, formaName,subFormaName);
		
		taxonName.setRank(lowestRank);
		setAuthor(taxonName, rs, taxonNumber);
		
		
		
		//set epithets

		
		//add original source for taxon name (taxon original source is added in mapper
		ReferenceBase citation = state.getConfig().getSourceReference();
//		addOriginalSource(taxonName, taxonNumber, TAXON_NAMESPACE, citation);
		return taxon;
		
	}



	private void setAuthor(BotanicalName taxonName, ResultSet rs, String taxonNumber) throws SQLException {
		
		String orderAuthor = rs.getString("Order name author");
		String subOrderAuthor = rs.getString("Suborder name author");
		String familyAuthor = rs.getString("Family name author");
		String subFamilyAuthor = rs.getString("Subfamily name author");
		String tribusAuthor = rs.getString("Tribus author");
		String subTribusAuthor = rs.getString("Subtribus author");
		String sectionAuthor = rs.getString("Section name author");
		String subsectionAuthor = rs.getString("Subsection author");
		String genusAuthor = rs.getString("Genus name author");
		String subGenusAuthor = rs.getString("Subgenus name author");
		String seriesAuthor = rs.getString("Series name author");
		String specificEpihetAuthor = rs.getString("Specific epithet author");
		String subspeciesAuthor = rs.getString("Subspecies author");
		String varietyAuthor = rs.getString("Variety name author");
		String subVarietyAuthor = rs.getString("Subvariety author");
		String formaAuthor = rs.getString("Forma name author");
		String subFormaAuthor = rs.getString("Subforma author");
		
		String authorsFull = rs.getString("Author/s - full");
		String authorsAbbrev = rs.getString("Author/s - abbreviated");
		

		Rank rank = taxonName.getRank();
		String authorString;
		if (rank != null){
			if (rank.equals(Rank.ORDER())){
				authorString = orderAuthor;
			}else if (rank.equals(Rank.SUBORDER())){
				authorString = subOrderAuthor;
			}else if (rank.equals(Rank.FAMILY())){
				authorString = familyAuthor;
			}else if (rank.equals(Rank.SUBFAMILY())){
				authorString = subFamilyAuthor;
			}else if (rank.equals(Rank.TRIBE())){
				authorString = tribusAuthor;
			}else if (rank.equals(Rank.SUBTRIBE())){
				authorString = subTribusAuthor;
			}else if (rank.equals(Rank.SECTION_BOTANY())){
				authorString = sectionAuthor;
			}else if (rank.equals(Rank.SUBSECTION_BOTANY())){
				authorString = subsectionAuthor;
			}else if (rank.equals(Rank.GENUS())){
				authorString = genusAuthor;
			}else if (rank.equals(Rank.SUBGENUS())){
				authorString = subGenusAuthor;
			}else if (rank.equals(Rank.SERIES())){
				authorString = seriesAuthor;
			}else if (rank.equals(Rank.SPECIES())){
				authorString = specificEpihetAuthor;
			}else if (rank.equals(Rank.SUBSPECIES())){
				authorString = subspeciesAuthor;
			}else if (rank.equals(Rank.VARIETY())){
				authorString = varietyAuthor;
			}else if (rank.equals(Rank.SUBVARIETY())){
				authorString = subVarietyAuthor;
			}else if (rank.equals(Rank.FORM())){
				authorString = formaAuthor;
			}else if (rank.equals(Rank.SUBFORM())){
				authorString = subFormaAuthor;
			}else{
				logger.warn("Author string could not be defined");
				authorString = authorsAbbrev;
				if (StringUtils.isBlank(authorString)){
					logger.warn("Authors abbrev string could not be defined");
					authorString = authorsFull;	
				}
			}
		}else{
			logger.warn(taxonNumber + ": Rank is null");
			authorString = authorsAbbrev;
			if (StringUtils.isBlank(authorString)){
				logger.warn(taxonNumber + ": Authors abbrev string could not be defined");
				authorString = authorsFull;	
			}
		}
		
		if (authorString != null){
			parser.handleAuthors(taxonName, taxonName.getNameCache().trim() + " " + authorString, authorString);
		}
		if (StringUtils.isNotBlank(authorsAbbrev) && ! authorsAbbrev.equalsIgnoreCase(taxonName.getCombinationAuthorTeam()==null ? "" :taxonName.getCombinationAuthorTeam().getNomenclaturalTitle())){
			logger.warn(taxonNumber + ": Rank author and abbrev author are not equal: " + authorString + "\t\t " + authorsAbbrev);
		}
//		if (StringUtils.isNotBlank(authorsFull) && ! authorsFull.equalsIgnoreCase(authorString)){
//			logger.warn("Rank author and full author are not equal Rankauthor: " + authorString + ", full author " + authorsFull);
//		}
	
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



	private Rank setLowestInfraGeneric(BotanicalName taxonName, Rank lowestRank, String subGenusName, String seriesName) {
		if (StringUtils.isNotBlank(seriesName)){
			taxonName.setInfraGenericEpithet(seriesName);
			return Rank.SERIES();
		}else if (StringUtils.isNotBlank(subGenusName)){
			taxonName.setInfraGenericEpithet(subGenusName);
			return Rank.SUBGENUS();
		}else{
			return lowestRank;
		}
	}



	private Rank setLowestUninomial(BotanicalName taxonName, String orderName, String subOrderName, String familyName, String subFamilyName,
			String tribusName, String subTribusName, String sectionName, String subsectionName, String genusName) {
		
		if (StringUtils.isNotBlank(genusName)){
			taxonName.setGenusOrUninomial(genusName);
			return Rank.GENUS();
		}else if (StringUtils.isNotBlank(subsectionName)){
			taxonName.setGenusOrUninomial(subsectionName);
			return Rank.SUBSECTION_BOTANY();
		}else if (StringUtils.isNotBlank(sectionName)){
			taxonName.setGenusOrUninomial(sectionName);
			return Rank.SECTION_BOTANY();
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
