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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.IClassificationService;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTaxIncludedInMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.checklist.CentralAfricaChecklistImportState;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.validation.CentralAfricaFernsTaxonImportValidator;
import eu.etaxonomy.cdm.model.agent.INomenclaturalAuthor;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.Classification;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class CentralAfricaFernsTaxonRelationImport  extends CentralAfricaFernsImportBase<TaxonBase> implements IMappingImport<TaxonBase, CentralAfricaFernsImportState>{
	private static final Logger logger = Logger.getLogger(CentralAfricaFernsTaxonRelationImport.class);
	
	private DbImportMapping mapping;
	
	
	private static final String pluralString = "taxon relations";
	private static final String dbTableName = "[African pteridophytes]";
	private static final Class cdmTargetClass = TaxonBase.class;

	private Map<String, UUID> taxonMap3 = new HashMap<String, UUID>();

	
	public CentralAfricaFernsTaxonRelationImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}
	
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT [Taxon number] FROM " + dbTableName;;
		return strQuery;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getMapping()
	 */
	protected DbImportMapping getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping();
			
			mapping.addMapper(DbImportMethodMapper.NewInstance(this, "createObject", ResultSet.class, CentralAfricaFernsImportState.class));
//					NewInstance(this, "Taxon number", TAXON_NAMESPACE)); //id + tu_status

//funktioniert nicht wegen doppeltem Abfragen von Attributen
//			mapping.addMapper(DbImportSynonymMapper.NewInstance("Taxon number", "Current", TAXON_NAMESPACE, null)); 			
//			mapping.addMapper(DbImportNameTypeDesignationMapper.NewInstance("id", "tu_typetaxon", NAME_NAMESPACE, "tu_typedesignationstatus"));

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
		String strRecordQuery = strSelect + strFrom + strWhere + strOrderBy ;
		return strRecordQuery;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#doInvoke(eu.etaxonomy.cdm.io.erms.ErmsImportState)
	 */
	@Override
	protected boolean doInvoke(CentralAfricaFernsImportState state) {
		//first path
		fillTaxonMap();
		boolean success = super.doInvoke(state);
		
		return success;

	}



	private void fillTaxonMap() {
		List<String> propPath = Arrays.asList(new String []{"name"});
		
		List<Taxon> taxonList = (List)getTaxonService().list(Taxon.class, null, null, null, propPath );
		for (Taxon taxon : taxonList){
			NonViralName nvn = CdmBase.deproxy(taxon.getName(), NonViralName.class);
			UUID uuid = taxon.getUuid();
			String name = nvn.getNameCache();
			taxonMap.put(name, uuid);
			
		}
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
//				Set<String> referenceIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "Current");
				handleForeignKey(rs, taxonIdSet, "Taxon number");

//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

			//reference map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, taxonIdSet, nameSpace);
			result.put(nameSpace, taxonMap);
				
				
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
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet)
	 */
	public TaxonBase createObject(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException {
		TaxonBase result = null;
		try {
			String status = rs.getString("Current/Synonym");
			String taxonNumber = rs.getString("Taxon number");
			state.setTaxonNumber(taxonNumber);
			if ("s".equalsIgnoreCase(status)){
				//synonym
				result = handleSynonym(rs, state);
			}else{
				//accepted Taxon
				result = handleTaxon(rs, state);
			}
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return result;
		}

	}


	private Synonym handleSynonym(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException {
		String accTaxonId = rs.getString("Current");
		String synonymId = state.getTaxonNumber();
		Synonym synonym = (Synonym)state.getRelatedObject(TAXON_NAMESPACE, synonymId);
		if (synonym == null){
			logger.warn ("Synonym ("+synonymId+")not found.");
			return null;
		}
		TaxonBase taxonBase = CdmBase.deproxy(state.getRelatedObject(TAXON_NAMESPACE, accTaxonId), TaxonBase.class);
			
		if (taxonBase != null){
			if (taxonBase.isInstanceOf(Taxon.class)){
				Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
				taxon.addSynonym(synonym, SynonymRelationshipType.SYNONYM_OF());
			}else{
				logger.warn("Accepted taxon (" + accTaxonId + ") for synonym (" + synonymId +") is not of type 'Current'");
			}		
		}else{
			logger.warn("Taxon (" + accTaxonId + ") not found for synonym (" + synonymId +")");
		}
		
		return synonym;
	}

	private Taxon handleTaxon(ResultSet rs, CentralAfricaFernsImportState state) throws SQLException {
		String taxonNumber = rs.getString("Taxon number");
		Taxon child = (Taxon)state.getRelatedObject(TAXON_NAMESPACE, taxonNumber);
		if (child == null){
			logger.warn("Taxon does not exist: " + taxonNumber);
			return null;
		}
		
		String orderName = rs.getString("Order name");
		String subOrderName = rs.getString("Suborder name");
		String familyName = rs.getString("Family name");
		String subFamilyName = rs.getString("Subfamily name");
		String tribusName = rs.getString("Tribus name");
		String subTribusName = rs.getString("Subtribus name");
		String sectionName = rs.getString("Section name");
		String subsectionName = rs.getString("Subsection name");
		String genusName = rs.getString("Genus name");
		String subGenusName = rs.getString("Subgenus name");
		String seriesName = rs.getString("Series name");
		String specificEpihet = rs.getString("Specific epihet");
		String subspeciesName = rs.getString("Subspecies name");
		String varietyName = rs.getString("Variety name");
		String subVariety = rs.getString("Subvariery");
		String formaName = rs.getString("Forma name");
		String subFormaName = rs.getString("Subforma");
		
		makeNextHigherTaxon(state, rs, child, orderName, subOrderName, familyName, subFamilyName, tribusName, subTribusName, sectionName,
				subsectionName, genusName, subGenusName, seriesName, specificEpihet, subspeciesName, varietyName, subVariety, formaName, subFormaName);
		return child;
	}



	/**
	 * Adds recursively this taxon to the next higher taxon. If the taxon exists already the relationship is not added
	 * again but if the author is missing in the old taxon but not in the new taxon the old taxon will get the new taxons
	 * author. If authors differ a new taxon is created.
	 * If a higher taxon exists the method is called recursively on this taxon.
	 * @throws SQLException 
	 */
	private void makeNextHigherTaxon(CentralAfricaFernsImportState state, ResultSet rs, Taxon child, String orderName, String subOrderName,
			String familyName, String subFamilyName, String tribusName, String subTribusName, String sectionName, String subsectionName,
			String genusName, String subGenusName, String seriesName, String specificEpihet, String subspeciesName, String varietyName,
			String subVariety, String formaName, String subFormaName) throws SQLException {

		Taxon higherTaxon = getNextHigherTaxon(state, rs, child, orderName, subOrderName, familyName, subFamilyName, tribusName, subTribusName, sectionName, subsectionName, genusName, subGenusName, seriesName, specificEpihet, subspeciesName, varietyName, subVariety, formaName, subFormaName);
		
		Reference<?> citation = null;
		String microcitation = null;
		if (higherTaxon != null){
			Taxon existingHigherTaxon = getExistingHigherTaxon(child, higherTaxon);
			if (existingHigherTaxon != null){
				boolean authorsAreSame = handleHigherTaxonAuthors(higherTaxon, existingHigherTaxon);
				if (authorsAreSame){
					higherTaxon = existingHigherTaxon;
				}
			}else if (! includedRelationshipExists(child, higherTaxon)){
				makeTaxonomicallyIncluded(state, null, child, higherTaxon, citation, microcitation);
			}else{
				//TODO it can appear because includeRelationshipExists works on strings not on taxon objects
				logger.warn("State should  not appear: " + state.getTaxonNumber() + "-" + higherTaxon.getName().getTitleCache() + "; " + child.getName().getTitleCache());
			}
			makeNextHigherTaxon(state, rs, higherTaxon, orderName, subOrderName, familyName, subFamilyName, tribusName, subTribusName, sectionName, subsectionName, genusName, subGenusName, seriesName, specificEpihet, subspeciesName, varietyName, subVariety, formaName, subFormaName);
		}else{
			//add taxon to tree if not yet added
			if (child.getTaxonNodes().size() == 0){
				makeTaxonomicallyIncluded(state, null, child, null, citation, microcitation);
			}
		}
	}

	/**
	 * Adds the higherTaxon authors to the existingHigherTaxon authors if the higherTaxon has authors and 
	 * the existingHigherTaxon has no authors.
	 * Returns false if both taxa have authors and the authors differ from each other.
	 * @param higherTaxon
	 * @param existingHigherTaxon
	 */
	private boolean handleHigherTaxonAuthors(Taxon higherTaxon, Taxon existingHigherTaxon) {
		NonViralName existingName = CdmBase.deproxy(higherTaxon.getName(), NonViralName.class);
		NonViralName newName = CdmBase.deproxy(existingHigherTaxon.getName(), NonViralName.class);
		if (existingName == newName){
			return true;
		}
		if (! hasAuthors(newName)){
			return true;
		}
		if (!hasAuthors(existingName)){
			existingName.setCombinationAuthorTeam(newName.getCombinationAuthorTeam());
			existingName.setExCombinationAuthorTeam(newName.getExCombinationAuthorTeam());
			existingName.setBasionymAuthorTeam(newName.getBasionymAuthorTeam());
			existingName.setExBasionymAuthorTeam(newName.getExBasionymAuthorTeam());
			return true;
		}
		boolean authorsAreSame = true;
		authorsAreSame &= getNomTitleNotNull(existingName.getCombinationAuthorTeam()).equals(getNomTitleNotNull(newName.getCombinationAuthorTeam()));
		authorsAreSame &= getNomTitleNotNull(existingName.getExCombinationAuthorTeam()).equals(getNomTitleNotNull(newName.getExCombinationAuthorTeam()));
		authorsAreSame &= getNomTitleNotNull(existingName.getBasionymAuthorTeam()).equals(getNomTitleNotNull(newName.getBasionymAuthorTeam()));
		authorsAreSame &= getNomTitleNotNull(existingName.getExBasionymAuthorTeam()).equals(getNomTitleNotNull(newName.getExBasionymAuthorTeam()));
		return authorsAreSame;
		
		
	}



	private String getNomTitleNotNull(INomenclaturalAuthor author) {
		if (author != null){
			return CdmUtils.Nz(author.getNomenclaturalTitle());
		}else{
			return "";
		}
	}



	private boolean hasAuthors(NonViralName name) {
		return (name.getCombinationAuthorTeam() != null || 
				name.getExCombinationAuthorTeam() != null ||
				name.getBasionymAuthorTeam() != null ||
				name.getExBasionymAuthorTeam() != null);
	}



	private Taxon getExistingHigherTaxon(Taxon child, Taxon higherTaxon) {
		int countNodes = child.getTaxonNodes().size();
		if (countNodes < 1){
			return null;
		}else if (countNodes > 1){
			throw new IllegalStateException("Multiple nodes exist for child taxon. This is an invalid state.");
		}else{
			TaxonNode childNode = child.getTaxonNodes().iterator().next();
			TaxonNode parentNode = childNode.getParent();
			if (parentNode != null){
				String existingParentTitle = parentNode.getTaxon().getName().getTitleCache();
				String newParentTitle = higherTaxon.getName().getTitleCache();
				if (existingParentTitle.equals(newParentTitle)){
					return parentNode.getTaxon();
				}
			}
			return null;
		}
	}



	/**
	 * Tests if this the child taxon already is a child of the higher taxon.
	 * @param child
	 * @param higherTaxon
	 * @return
	 */
	private boolean includedRelationshipExists(Taxon child, Taxon higherTaxon) {
		int countNodes = higherTaxon.getTaxonNodes().size();
		if (countNodes < 1){
			return false;
		}else if (countNodes > 1){
			throw new IllegalStateException("Multiple nodes exist for higher taxon. This is an invalid state.");
		}else{
			TaxonNode higherNode = higherTaxon.getTaxonNodes().iterator().next();
			return childExists(child, higherNode);
		}
	}



	private boolean childExists(Taxon child, TaxonNode higherNode) {
		for (TaxonNode childNode : higherNode.getChildNodes()){
			String existingChildTitle = childNode.getTaxon().getName().getTitleCache();
			String newChildTitle = child.getName().getTitleCache();
			if (existingChildTitle.equals(newChildTitle)){
				return true;
			}
		}
		return false;
	}



//	private boolean makeTaxonomicallyIncluded(CentralAfricaFernsImportState state, Taxon parent, Taxon child, Reference citation, String microCitation){
//		Reference sec = child.getSec();
//		Classification tree = state.getTree(sec);
//		if (tree == null){
//			tree = makeTreeMemSave(state, sec);
//		}
//		TaxonNode childNode;
//		if (parent != null){
//			childNode = tree.addParentChild(parent, child, citation, microCitation);
//		}else{
//			childNode = tree.addChildTaxon(child, citation, microCitation, null);
//		}
//		return (childNode != null);
//	}
	
	//TODO use Mapper
	private boolean makeTaxonomicallyIncluded(CentralAfricaFernsImportState state, Integer treeRefFk, Taxon child, Taxon parent, Reference citation, String microCitation){
		String treeKey;
		UUID treeUuid;
		if (treeRefFk == null){
			treeKey = "1";  // there is only one tree and it gets the map key '1'
			treeUuid = state.getConfig().getClassificationUuid();
		}else{
			treeKey =String.valueOf(treeRefFk);
			treeUuid = state.getTreeUuidByTreeKey(treeKey);
		}
		Classification tree = (Classification)state.getRelatedObject(DbImportTaxIncludedInMapper.TAXONOMIC_TREE_NAMESPACE, treeKey);
		if (tree == null){
			IClassificationService service = state.getCurrentIO().getClassificationService();
			tree = service.getClassificationByUuid(treeUuid);
			if (tree == null){
				String treeName = state.getConfig().getClassificationName();
				tree = Classification.NewInstance(treeName);
				tree.setUuid(treeUuid);
				//FIXME tree reference
				tree.setReference(citation);
				service.save(tree);
			}
			state.addRelatedObject(DbImportTaxIncludedInMapper.TAXONOMIC_TREE_NAMESPACE, treeKey, tree);
		}
		
		TaxonNode childNode;
		if (parent != null){
			childNode = tree.addParentChild(parent, child, citation, microCitation);
		}else{
			childNode = tree.addChildTaxon(child, citation, microCitation, null);
		}
		return (childNode != null);
	}



	/**
	 * Reasons if a higher taxon should exist. If it should exist it tries to match with an existing taxon otherwise
	 * creates a new taxon.
	 * @return
	 * @throws SQLException
	 */
	private Taxon getNextHigherTaxon(CentralAfricaFernsImportState state, ResultSet rs, Taxon childTaxon, String orderName, String subOrderName, String familyName, String subFamilyName,
			String tribusName, String subTribusName, String sectionName, String subsectionName, String genusName, String subGenusName, String seriesName, String speciesName, String subspeciesName, String varietyName, String subVariety, String formaName, String subFormaName) throws SQLException {
		
		Taxon result = null;
		BotanicalName childName = CdmBase.deproxy(childTaxon.getName(), BotanicalName.class);
		Rank childRank = childName.getRank();
		BotanicalName higherName;
		higherName = handleInfraSpecific(childRank, genusName, speciesName, subspeciesName, varietyName, subVariety, formaName, subFormaName);
		if (higherName.getRank() == null){
			handleSpecies(childRank, higherName,  genusName, speciesName);
		}
		if (higherName.getRank() == null){
			handleInfraGeneric(childRank, higherName,  genusName, subGenusName, sectionName, subsectionName, seriesName);
		}
		if (higherName.getRank() == null){
			handleUninomial(childRank, higherName, orderName, subOrderName, familyName, subFamilyName, tribusName, subTribusName, genusName);
		}
		//if higher taxon must exist, create it if it was not yet created
		result = getExistingTaxon(higherName, state);
		if (higherName.getRank() != null && result == null ){
			result = Taxon.NewInstance(higherName, childTaxon.getSec());
			setAuthor(higherName, rs, state.getTaxonNumber(), true);
			UUID uuid = higherName.getUuid();
			String name = higherName.getNameCache();
			taxonMap.put(name, uuid);
			state.addRelatedObject(HIGHER_TAXON_NAMESPACE, higherName.getNameCache(), result);
		}
		return result;
	}



	private Taxon getExistingTaxon(BotanicalName higherName, CentralAfricaFernsImportState state) {
		String nameCache = higherName.getNameCache();
		UUID uuid = taxonMap.get(nameCache);
		
		Taxon persistedTaxon = null;
		Taxon relatedTaxon = null;
		if (uuid != null){
			//persistedTaxon = CdmBase.deproxy(getTaxonService().find(uuid), Taxon.class);
			relatedTaxon = state.getRelatedObject(HIGHER_TAXON_NAMESPACE, nameCache, Taxon.class);
			if (relatedTaxon == null){
				//TODO find for partition
				relatedTaxon = (Taxon)getTaxonService().find(uuid);
				if (relatedTaxon == null){
					logger.info(state.getTaxonNumber() +  " - Could not find existing name ("+nameCache+") in related objects map");
				}else{
					state.addRelatedObject(HIGHER_TAXON_NAMESPACE, nameCache, relatedTaxon);
				}
			}
			if (persistedTaxon != relatedTaxon){
				//logger.warn("Difference in related taxa: " + nameCache );
			}
			
		}
		return relatedTaxon; //persistedTaxon;
	}



	private BotanicalName handleInfraSpecific(Rank lowerTaxonRank, String genusName, String specificEpithet, String subspeciesName, String varietyName, String subVariety, String formaName, String subFormaName) {

		BotanicalName taxonName = BotanicalName.NewInstance(null);
		Rank newRank = null;
		
		if (StringUtils.isNotBlank(subFormaName)   && lowerTaxonRank.isLower(Rank.SUBFORM())){
			taxonName.setInfraSpecificEpithet(subFormaName);
			newRank =  Rank.SUBFORM();
		}else if (StringUtils.isNotBlank(formaName)  && lowerTaxonRank.isLower(Rank.FORM())){
			taxonName.setInfraSpecificEpithet(formaName);
			newRank =  Rank.FORM();
		}else if (StringUtils.isNotBlank(subVariety)  && lowerTaxonRank.isLower(Rank.SUBVARIETY())){
			taxonName.setInfraSpecificEpithet(subVariety);
			newRank =  Rank.SUBVARIETY();
		}else if (StringUtils.isNotBlank(varietyName)  && lowerTaxonRank.isLower(Rank.VARIETY())){
			taxonName.setInfraSpecificEpithet(varietyName);
			newRank =  Rank.VARIETY();
		}else if (StringUtils.isNotBlank(subspeciesName)  && lowerTaxonRank.isLower(Rank.SUBSPECIES())){
			taxonName.setInfraSpecificEpithet(subspeciesName);
			newRank = Rank.SUBSPECIES();
		}
		
		if (newRank != null){
			taxonName.setSpecificEpithet(specificEpithet);
			taxonName.setGenusOrUninomial(genusName);
			taxonName.setRank(newRank);
		}
		
		return taxonName;
	}

	private BotanicalName handleSpecies(Rank lowerTaxonRank, BotanicalName taxonName, String genusName, String speciesEpithet) {
		Rank newRank = null;
		
		if (StringUtils.isNotBlank(speciesEpithet)  && lowerTaxonRank.isLower(Rank.SPECIES())){
			taxonName.setSpecificEpithet(speciesEpithet);
			newRank = Rank.SPECIES();
		}
		if (newRank != null){
			taxonName.setGenusOrUninomial(genusName);
			taxonName.setRank(newRank);
		}
		return taxonName;
	}

	private BotanicalName handleInfraGeneric(Rank lowerTaxonRank, BotanicalName taxonName, String genusName, String subGenusName, String sectionName, String subsectionName, String seriesName) {
		Rank newRank = null;
		
		if (StringUtils.isNotBlank(seriesName)  && lowerTaxonRank.isLower(Rank.SERIES())){
			taxonName.setInfraGenericEpithet(seriesName);
			newRank = Rank.SERIES();
		}else if (StringUtils.isNotBlank(subsectionName)  && lowerTaxonRank.isLower(Rank.SUBSECTION_BOTANY())){
			taxonName.setInfraGenericEpithet(subsectionName);
			newRank =  Rank.SUBSECTION_BOTANY();
		}else if (StringUtils.isNotBlank(sectionName)  && lowerTaxonRank.isLower(Rank.SECTION_BOTANY())){
			taxonName.setInfraGenericEpithet(sectionName);
			newRank =  Rank.SECTION_BOTANY();
		}else if (StringUtils.isNotBlank(subGenusName) && lowerTaxonRank.isLower(Rank.SUBGENUS())){
			taxonName.setInfraGenericEpithet(subGenusName);
			newRank = Rank.SUBGENUS();
		}
		if (newRank != null){
			taxonName.setGenusOrUninomial(genusName);
			taxonName.setRank(newRank);
		}
		return taxonName;
	}



	private BotanicalName handleUninomial(Rank lowerTaxonRank, BotanicalName taxonName,  String orderName, String subOrderName, String familyName, String subFamilyName,
				String tribusName, String subTribusName, String genusName) {
		
		Rank newRank = null;
		if (StringUtils.isNotBlank(genusName) && lowerTaxonRank.isLower(Rank.GENUS())){
			taxonName.setGenusOrUninomial(genusName);
			newRank =  Rank.GENUS();
		}else if (StringUtils.isNotBlank(subTribusName) && lowerTaxonRank.isLower(Rank.SUBTRIBE())){
			taxonName.setGenusOrUninomial(subTribusName);
			newRank =  Rank.SUBTRIBE();
		}else if (StringUtils.isNotBlank(tribusName) && lowerTaxonRank.isLower(Rank.TRIBE())){
			taxonName.setGenusOrUninomial(tribusName);
			newRank =  Rank.TRIBE();
		}else if (StringUtils.isNotBlank(subFamilyName) && lowerTaxonRank.isLower(Rank.SUBFAMILY())){
			taxonName.setGenusOrUninomial(subFamilyName);
			newRank =  Rank.SUBFAMILY();
		}else if (StringUtils.isNotBlank(familyName) && lowerTaxonRank.isLower(Rank.FAMILY())){
			taxonName.setGenusOrUninomial(familyName);
			newRank =  Rank.FAMILY();
		}else if (StringUtils.isNotBlank(subOrderName) && lowerTaxonRank.isLower(Rank.SUBORDER())){
			taxonName.setGenusOrUninomial(subOrderName);
			newRank =  Rank.SUBORDER();
		}else if (StringUtils.isNotBlank(orderName) && lowerTaxonRank.isLower(Rank.ORDER())){
			taxonName.setGenusOrUninomial(orderName);
			newRank =  Rank.ORDER();
		}
		taxonName.setRank(newRank);
		return taxonName;
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
