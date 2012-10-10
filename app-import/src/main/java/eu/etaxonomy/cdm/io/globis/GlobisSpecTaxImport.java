/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.globis.validation.GlobisReferenceImportValidator;
import eu.etaxonomy.cdm.io.globis.validation.GlobisSpecTaxaImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationTest;
import eu.etaxonomy.cdm.model.name.ZoologicalName;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivationEvent;
import eu.etaxonomy.cdm.model.occurrence.DerivationEventType;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnitBase;
import eu.etaxonomy.cdm.model.occurrence.FieldObservation;
import eu.etaxonomy.cdm.model.occurrence.Specimen;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class GlobisSpecTaxImport  extends GlobisImportBase<Reference> implements IMappingImport<Reference, GlobisImportState>{
	private static final Logger logger = Logger.getLogger(GlobisSpecTaxImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "taxa";
	private static final String dbTableName = "specTax";
	private static final Class cdmTargetClass = Reference.class;

	public GlobisSpecTaxImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strRecordQuery = 
			" SELECT specTaxId " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery = 
			" SELECT t.*, t.DateCreated as Created_When, t.CreatedBy as Created_Who," +
			"        t.ModifiedBy as Updated_who, t.DateModified as Updated_When, t.SpecRemarks as Notes " + 
			" FROM " + getTableName() + " t " +
			" WHERE ( t.specTaxId IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#doPartition(eu.etaxonomy.cdm.io.common.ResultSetPartitioner, eu.etaxonomy.cdm.io.globis.GlobisImportState)
	 */
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;
		
		Set<TaxonBase> objectsToSave = new HashSet<TaxonBase>();
		
		Map<String, Taxon> taxonMap = (Map<String, Taxon>) partitioner.getObjectMap(TAXON_NAMESPACE);
		Map<String, Reference> referenceMap = (Map<String, Reference>) partitioner.getObjectMap(REFERENCE_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer specTaxId = rs.getInt("SpecTaxId");
        		Integer acceptedTaxonId = nullSafeInt(rs, "SpecCurrspecID");
        		String specSystaxRank = rs.getString("SpecSystaxRank");
        		
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
				
					Taxon acceptedTaxon =  taxonMap.get(String.valueOf(acceptedTaxonId));
					TaxonBase<?> thisTaxon = null;
					
					if (isBlank(specSystaxRank) ){
						//TODO
					}else if (specSystaxRank.equals("synonym")){
						Synonym synonym = getSynonym(state, rs);
						if (acceptedTaxon == null){
							//TODO
							logger.warn("Accepted taxon (" + acceptedTaxonId + ") not found for synonym "+ specTaxId);
						}else{
							acceptedTaxon.addSynonym(synonym, SynonymRelationshipType.SYNONYM_OF());
							thisTaxon = synonym;
						}
					}else if (specSystaxRank.equals("species")){
						validateAcceptedTaxon(acceptedTaxon, rs, specTaxId, acceptedTaxonId);
						thisTaxon = acceptedTaxon;
					}else{
						logger.warn(String.format("Unhandled specSystaxRank %s in specTaxId %d", specSystaxRank, specTaxId));
					}
					
					if (thisTaxon != null){
						ZoologicalName name = CdmBase.deproxy(thisTaxon.getName(), ZoologicalName.class);
						
						handleNomRef(state, referenceMap, rs, name);
					
						handleTypeInformation(state,rs, name);
					
					
//						this.doIdCreatedUpdatedNotes(state, ref, rs, refId, REFERENCE_NAMESPACE);
					
						objectsToSave.add(acceptedTaxon); 
					}
					

				} catch (Exception e) {
					logger.warn("Exception in specTax: SpecTaxId " + specTaxId + ". " + e.getMessage());
					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);	
			
			return success;
		} catch (Exception e) {
			logger.error("Exception: " +  e);
			return false;
		}
	}


	private Pattern patternAll = Pattern.compile("(.+,\\s.+)(\\(.+\\))");
	

	private void handleTypeInformation(GlobisImportState state, ResultSet rs, ZoologicalName name) throws SQLException {
		
		String specTypeDepositoriesStr = rs.getString("SpecTypeDepository");
		String countryString = rs.getString("SpecTypeCountry");
		
		if (! hasTypeInformation(specTypeDepositoriesStr, countryString)){
			return;
		}
		
		FieldObservation fieldObservation = makeTypeFieldObservation(state, countryString);
		
		String[] specTypeDepositories = specTypeDepositoriesStr.split(";");
		//TODO different issues
		if (specTypeDepositories.length == 0){
			//TODO
		}
		for (String specTypeDepositoryStr : specTypeDepositories){
			specTypeDepositoryStr = specTypeDepositoryStr.trim();
			
			//Specimen
			Specimen specimen = makeSingleTypeSpecimen(fieldObservation);

			if (specTypeDepositoryStr.equals("??")){
				//unknown
				//TODO
				specimen.setTitleCache("??", true);
			}else{
				specTypeDepositoryStr = makeAdditionalSpecimenInformation( 
						specTypeDepositoryStr, specimen);
				
				makeCollection(specTypeDepositoryStr, specimen);
			}
			
			//type Designation
			makeTypeDesignation(name, rs, specimen);
		}
		
	}




	private boolean hasTypeInformation(String specTypeDepositoriesStr, String countryString) {
		boolean result = false;
		result |= isNotBlank(specTypeDepositoriesStr) || isNotBlank(countryString);
		return result;
	}



	/**
	 * @param specTypeDepositoryStr
	 * @param specimen
	 */
	protected void makeCollection(String specTypeDepositoryStr, Specimen specimen) {
		//TODO deduplicate
		Map<String, Collection> collectionMap = new HashMap<String, Collection>();
		
		
		//Collection
		String[] split = specTypeDepositoryStr.split(",");
		if (split.length != 2){
			if (split.length == 1 && split[0].startsWith("coll.")){
				Collection collection = Collection.NewInstance();
				collection.setName(split[0]);
			}else{
				logger.warn("Split size is not 2: " + specTypeDepositoryStr);
			}
			
		}else{
			String collectionStr = split[0];
			String location = split[1];
			
			
			Collection collection = collectionMap.get(collectionStr);
			if (collection == null){
				collection = Collection.NewInstance();
				collection.setCode(collectionStr);
				collection.setTownOrLocation(split[1]);
			}else if (CdmUtils.nullSafeEqual(location, collection.getTownOrLocation())){
				String message = "Location (%s) is not equal to location (%s) of existing collection";
				logger.warn(String.format(message, location, collection.getTownOrLocation(), collection.getCode()));
			}
			
			specimen.setCollection(collection);
			
		}
	}




	/**
	 * @param specTypeDepositoriesStr
	 * @param specTypeDepositoryStr
	 * @param specimen
	 * @return
	 */
	protected String makeAdditionalSpecimenInformation( String specTypeDepositoryStr,
			Specimen specimen) {
		//doubful
		if (specTypeDepositoryStr.endsWith("?")){
			Marker.NewInstance(specimen, true, MarkerType.IS_DOUBTFUL());
			specTypeDepositoryStr = specTypeDepositoryStr.substring(0, specTypeDepositoryStr.length() -1).trim();
		}
		
		//brackets
		Matcher matcher = patternAll.matcher(specTypeDepositoryStr);
		if (matcher.find()){
			//has brackets
			String brackets = matcher.group(2);
			brackets = brackets.substring(1, brackets.length()-1);
			
			brackets = brackets.replace("[mm]", "\u2642\u2642");
			brackets = brackets.replace("[m]", "\u2642");
			brackets = brackets.replace("[ff]", "\u2640\u2640");
			brackets = brackets.replace("[f]", "\u2640");
			
			if (brackets.contains("[") || brackets.contains("]")){
				logger.warn ("There are still '[', ']' in the bracket part: " + brackets);
			}
			
			//TODO replace mm/ff by Unicode male 
			specimen.setTitleCache(brackets, true);
			specTypeDepositoryStr = matcher.group(1).trim();
		}
		return specTypeDepositoryStr;
	}




	/**
	 * @param fieldObservation
	 * @return
	 */
	protected Specimen makeSingleTypeSpecimen(FieldObservation fieldObservation) {
		DerivationEvent derivEvent = DerivationEvent.NewInstance();
//			derivEvent.setType(DerivationEventType.ACCESSIONING());
		fieldObservation.addDerivationEvent(derivEvent);
		Specimen specimen = Specimen.NewInstance();
		specimen.setDerivedFrom(derivEvent);
		return specimen;
	}




	/**
	 * @param state
	 * @return
	 * @throws SQLException
	 */
	protected FieldObservation makeTypeFieldObservation(GlobisImportState state, 
			String countryString) throws SQLException {
		
		DerivedUnitType unitType = DerivedUnitType.Specimen;
		DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(unitType);
		
		WaterbodyOrCountry typeCountry = getCountry(state, countryString);
		facade.setCountry(typeCountry);
		FieldObservation fieldObservation = facade.innerFieldObservation();
		return fieldObservation;
	}




	/**
	 * @param name
	 * @param rs 
	 * @param status
	 * @param specimen
	 * @throws SQLException 
	 */
	protected void makeTypeDesignation(ZoologicalName name, ResultSet rs, Specimen specimen) throws SQLException {
		//type
		String specType = rs.getString("SpecType");
		SpecimenTypeDesignationStatus status = getTypeDesigType(specType);

		SpecimenTypeDesignation typeDesignation = SpecimenTypeDesignation.NewInstance();
		typeDesignation.setTypeStatus(status);
		typeDesignation.setTypeSpecimen(specimen);
		
		name.addTypeDesignation(typeDesignation, true);
	}




	private SpecimenTypeDesignationStatus getTypeDesigType(String specType) {
		if (isBlank(specType) ){
			return null;
		}else if (specType.matches("Holotype(Holotypus)?")){
			return SpecimenTypeDesignationStatus.HOLOTYPE();
		}else if (specType.matches("Neotype")){
			return SpecimenTypeDesignationStatus.NEOTYPE();
		}else if (specType.matches("Syntype(\\(s\\))?")){
			return SpecimenTypeDesignationStatus.SYNTYPE();
		}else if (specType.matches("Lectotype")){
			return SpecimenTypeDesignationStatus.LECTOTYPE();
		}else{
			logger.warn("SpecimenTypeDesignationStatus does not match: " + specType);
			return null;
		}
	}




	/**
	 * @param state
	 * @param referenceMap
	 * @param rs
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	private Reference<?> handleNomRef(GlobisImportState state, Map<String, Reference> referenceMap, ResultSet rs,
			ZoologicalName name) throws SQLException {
		//ref
		Integer refId = nullSafeInt(rs, "fiSpecRefID");
		Reference<?> nomRef = null;
		if (refId != null){
			nomRef = referenceMap.get(String.valueOf(refId));
			if (nomRef == null && state.getConfig().getDoReferences().equals(state.getConfig().getDoReferences().ALL)){
				logger.warn("Reference " + refId + " could not be found.");
			}else if (nomRef != null){
				name.setNomenclaturalReference(nomRef);
			}
		}
		
		//refDetail
		String refDetail = rs.getString("SpecPage");
		if (isNotBlank(refDetail)){
			name.setNomenclaturalMicroReference(refDetail);
		}
		return nomRef;
	}



	
	private void validateAcceptedTaxon(Taxon acceptedTaxon, ResultSet rs, Integer specTaxId, Integer acceptedTaxonId) throws SQLException {
		if (acceptedTaxon == null){
			logger.warn("Accepted taxon is null for taxon taxon to validate: ");
			return;
		}
		
		//TODO 
		ZoologicalName name = CdmBase.deproxy(acceptedTaxon.getName(), ZoologicalName.class);
		
		String specName = rs.getString("SpecName");
		if (! name.getSpecificEpithet().equals(specName)){
			logger.warn(String.format("Species epithet is not equal for accepted taxon: %s - %s", name.getSpecificEpithet(), specName));
		}
		//TODO
	}




	private Synonym getSynonym(GlobisImportState state, ResultSet rs) throws SQLException {
		//rank
		String rankStr = rs.getString("SpecRank");
		Rank rank = null;
		if (isNotBlank(rankStr)){
			try {
				rank = Rank.getRankByNameOrAbbreviation(rankStr, NomenclaturalCode.ICZN, true);
			} catch (UnknownCdmTypeException e) {
				e.printStackTrace();
			}
		}
		
		//name
		ZoologicalName name = ZoologicalName.NewInstance(rank);
		makeNamePartsAndCache(state, rs, rankStr, name);
		

//		name.setGenusOrUninomial(genusOrUninomial);
		String authorStr = rs.getString("SpecAuthor");
		String yearStr = rs.getString("SpecYear");
		String authorAndYearStr = CdmUtils.concat(", ", authorStr, yearStr);
		handleAuthorAndYear(authorAndYearStr, name);
				
		Synonym synonym = Synonym.NewInstance(name, state.getTransactionalSourceReference());
		
		return synonym;
	}




	private void makeNamePartsAndCache(GlobisImportState state, ResultSet rs, String rank, ZoologicalName name) throws SQLException {
		String citedFamily = rs.getString("SpecCitedFamily");
		String citedGenus = rs.getString("SpecCitedGenus");
		String citedSpecies = rs.getString("SpecCitedSpecies");
		String citedSubspecies = rs.getString("SpecCitedSubspecies");
		String lastEpithet = rs.getString("SpecName");
		
		
		String cache = CdmUtils.concat(" ", new String[]{citedFamily, citedGenus, citedSpecies, citedSubspecies, rank, lastEpithet});
		name.setGenusOrUninomial(citedGenus);
		//TODO sperate authors
		if (isBlank(citedSpecies)){
			name.setSpecificEpithet(lastEpithet);
		}else{
			name.setSpecificEpithet(citedSpecies);
			if (isBlank(citedSubspecies)){
				name.setInfraSpecificEpithet(lastEpithet);
			}
		}
		
		//TODO check if cache needs protection
		name.setNameCache(cache, true);
	}




	private boolean isInfraSpecies(GlobisImportState state, ResultSet rs, Rank rank) {
		// TODO Auto-generated method stub
		return false;
	}




	private Reference<?> getJournal(GlobisImportState state, ResultSet rs, String refJournal) throws SQLException {
		
		
		Reference<?> journal = ReferenceFactory.newJournal();
		String issn = rs.getString("RefISSN");
		if (StringUtils.isNotBlank(issn)){
			issn.replaceAll("ISSN", "").trim();
			journal.setIssn(issn);			
		}

		
		
		//TODO deduplicate
		journal.setTitle(refJournal);
		return journal;
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet, eu.etaxonomy.cdm.io.common.ImportStateBase)
	 */
	public Reference<?> createObject(ResultSet rs, GlobisImportState state)
			throws SQLException {
		Reference<?> ref;
		String refType = rs.getString("RefType");
		if (refType == null){
			ref = ReferenceFactory.newGeneric();
		}else if (refType == "book"){
			ref = ReferenceFactory.newBook();
		}else if (refType == "paper in journal"){
			ref = ReferenceFactory.newArticle();
		}else if (refType.startsWith("unpublished") ){
			ref = ReferenceFactory.newGeneric();
		}else if (refType.endsWith("paper in journal")){
			ref = ReferenceFactory.newArticle();
		}else if (refType == "paper in book"){
			ref = ReferenceFactory.newBookSection();
		}else if (refType == "paper in journalwebsite"){
			ref = ReferenceFactory.newArticle();
		}else{
			logger.warn("Unknown reference type: " + refType);
			ref = ReferenceFactory.newGeneric();
		}
		return ref;
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
			Set<String> referenceIdSet = new HashSet<String>();
			
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "SpecCurrspecID");
				handleForeignKey(rs, referenceIdSet, "fiSpecRefID");
			}
			
			//taxon map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = Taxon.class;
			idSet = taxonIdSet;
			Map<String, Taxon> objectMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//reference map
			nameSpace = REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, referenceMap);
			
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(GlobisImportState state){
		IOValidator<GlobisImportState> validator = new GlobisSpecTaxaImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoSpecTaxa();
	}





}
