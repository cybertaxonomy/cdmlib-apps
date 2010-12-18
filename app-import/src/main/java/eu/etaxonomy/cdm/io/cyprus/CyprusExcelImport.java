/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy 
 * http://www.e-taxonomy.eu
 * 
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.cyprus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.excel.common.ExcelImporterBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.babadshanjan
 * @created 08.01.2009
 * @version 1.0
 */

@Component
public class CyprusExcelImport extends ExcelImporterBase<CyprusImportState> {
	private static final Logger logger = Logger.getLogger(CyprusExcelImport.class);
	
	public static Set<String> validMarkers = new HashSet<String>(Arrays.asList(new String[]{"", "valid", "accepted", "a", "v", "t"}));
	public static Set<String> synonymMarkers = new HashSet<String>(Arrays.asList(new String[]{"", "invalid", "synonym", "s", "i"}));
	
	
	@Override
	protected boolean isIgnore(CyprusImportState state) {
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(CyprusImportState state) {
		logger.warn("DoCheck not yet implemented for CyprusExcelImport");
		return true;
	}

//	protected static final String ID_COLUMN = "Id";
	protected static final String SPECIES_COLUMN = "species";
	protected static final String SUBSPECIES_COLUMN = "subspecies";
	protected static final String GENUS_COLUMN = "genus";
	protected static final String FAMILY_COLUMN = "family";
	protected static final String DIVISION_COLUMN = "division";
	protected static final String HOMOTYPIC_SYNONYM_COLUMN = "homotypic synonyms";
	protected static final String HETEROTYPIC_SYNONYMS_COLUMN = "heterotypic synonyms";
	protected static final String ENDEMISM_COLUMN = "endemism";

	protected static final String STATUS_COLUMN = "status";
	protected static final String RED_DATA_BOOK_CATEGORY_COLUMN = "red data book category";
	protected static final String SYSTEMATICS_COLUMN = "systematics";
	
	
	
	// TODO: This enum is for future use (perhaps).
	protected enum Columns { 
//		Id("Id"), 
		Species("species"), 
		Subspecies("subspecies"),
		Genus("genus"),
		Family("family"),
		Division("division"),
		HomotypicSynonyms("homotypic synonyms"),
		HeterotypicSynonyms("heterotypic synonyms"),
		Status("status"),
		Endemism("endemism");
		
		private String head;
		private String value;
	
		private Columns(String head) {
			this.head = head;
		}
		
		public String head() {
			return this.head;
		}
	
		public String value() {
			return this.value;
		}
	}
	
	
	@Override
    protected boolean analyzeRecord(HashMap<String, String> record, CyprusImportState state) {
		
		boolean success = true;
    	Set<String> keys = record.keySet();
    	
    	CyprusRow cyprusRow = new CyprusRow();
    	state.setCyprusRow(cyprusRow);
    	
    	for (String originalKey: keys) {
    		Integer index = 0;
    		String indexedKey = CdmUtils.removeDuplicateWhitespace(originalKey.trim()).toString();
    		String[] split = indexedKey.split("_");
    		String key = split[0];
    		if (split.length > 1){
    			String indexString = split[1];
    			try {
					index = Integer.valueOf(indexString);
				} catch (NumberFormatException e) {
					String message = "Index must be integer";
					logger.error(message);
					continue;
				}
    		}
    		
    		String value = (String) record.get(indexedKey);
    		if (! StringUtils.isBlank(value)) {
    			if (logger.isDebugEnabled()) { logger.debug(key + ": " + value); }
        		value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
    		}else{
    			continue;
    		}
    		
    		
    		if (key.equalsIgnoreCase(SPECIES_COLUMN)) {
//    			int ivalue = floatString2IntValue(value);
    			cyprusRow.setSpecies(value);
    			
			} else if(key.equalsIgnoreCase(SUBSPECIES_COLUMN)) {
				cyprusRow.setSubspecies(value);
				
			} else if(key.equalsIgnoreCase(HOMOTYPIC_SYNONYM_COLUMN)) {
				cyprusRow.setHomotypicSynonyms(value);
    			
			} else if(key.equalsIgnoreCase(HETEROTYPIC_SYNONYMS_COLUMN)) {
				cyprusRow.setHeterotypicSynonyms(value);
    			
			} else if(key.equalsIgnoreCase(ENDEMISM_COLUMN)) {
				cyprusRow.setEndemism(value);
   			
			} else if(key.equalsIgnoreCase(STATUS_COLUMN)) {
				cyprusRow.setStatus(value);
    			
			} else if(key.equalsIgnoreCase(RED_DATA_BOOK_CATEGORY_COLUMN)) {
				cyprusRow.setRedDataBookCategory(value);
    			
			} else if(key.equalsIgnoreCase(SYSTEMATICS_COLUMN)) {
				cyprusRow.setSystematics(value);
			
			} else if(key.equalsIgnoreCase(GENUS_COLUMN)) {
				cyprusRow.setGenus(value);
			
			} else if(key.equalsIgnoreCase(FAMILY_COLUMN)) {
				cyprusRow.setFamily(value);
    			
			} else if(key.equalsIgnoreCase(DIVISION_COLUMN)) {
				cyprusRow.setDivision(value);
    			
			} else {
				success = false;
				logger.error("Unexpected column header " + key);
			}
    	}
    	return success;
    }
	
	private static INonViralNameParser nameParser = NonViralNameParserImpl.NewInstance();
	private static NomenclaturalCode nc = NomenclaturalCode.ICBN;
	
	
	/** 
	 *  Stores taxa records in DB
	 */
	@Override
    protected boolean firstPass(CyprusImportState state) {
		boolean success = true;
		Rank rank = null;
		CyprusRow taxonLight = state.getCyprusRow();
		Reference citation = null;
		String microCitation = null;
		
		//species name
		String speciesStr = taxonLight.getSpecies();
		String subSpeciesStr = taxonLight.getSubspecies();
		String homotypicSynonymsString = taxonLight.getHomotypicSynonyms();
		List<String> homotypicSynonymList = Arrays.asList(homotypicSynonymsString.split(";"));
		String heterotypicSynonymsString = taxonLight.getHeterotypicSynonyms();
		List<String> heterotypicSynonymList = Arrays.asList(heterotypicSynonymsString.split(";"));
		
		String systematicsString = taxonLight.getSystematics();
		String endemismString = taxonLight.getEndemism();
		String statusString = taxonLight.getStatus();
		String redBookCategory = taxonLight.getRedDataBookCategory();
		
		//		BotanicalName subSpeciesName = (BotanicalName)nameParser.parseSimpleName(subSpeciesStr, nc, Rank.SUBSPECIES());
//		BotanicalName speciesName = (BotanicalName)nameParser.parseSimpleName(speciesStr, nc, Rank.SPECIES());
		
//		Classification classification = null;
			
		if (StringUtils.isNotBlank(speciesStr)) {
			Taxon mainTaxon = null;
			Taxon speciesTaxon = (Taxon)createTaxon(state, Rank.SPECIES(), speciesStr, Taxon.class, nc);
			mainTaxon = speciesTaxon;
			if (StringUtils.isNotBlank(subSpeciesStr)){
				Taxon subSpeciesTaxon = (Taxon)createTaxon(state, Rank.SUBSPECIES(), subSpeciesStr, Taxon.class, nc);
				
				if (subSpeciesTaxon != null){
					makeParent(state, speciesTaxon, subSpeciesTaxon, citation, microCitation);
				}
				mainTaxon = subSpeciesTaxon;
			}
			
			makeHigherTaxa(state, taxonLight, speciesTaxon, citation, microCitation);
			makeHomotypicSynonyms(state, citation, microCitation, homotypicSynonymList, mainTaxon);			
			makeHeterotypicSynonyms(state, citation, microCitation, heterotypicSynonymList, mainTaxon);			
			makeSystematics(systematicsString, mainTaxon);
			makeEndemism(endemismString, mainTaxon);
			makeStatus(statusString, mainTaxon);
			makeRedBookCategory(redBookCategory, mainTaxon);
			
//			state.putHigherTaxon(higherName, uuid);//(speciesStr, mainTaxon);
			getTaxonService().save(mainTaxon);
		}
		return success;
    }


	private void makeHigherTaxa(CyprusImportState state, CyprusRow taxonLight, Taxon speciesTaxon, Reference citation, String microCitation) {
		String divisionStr = taxonLight.getDivision();
		String genusStr = taxonLight.getGenus();
		String familyStr = taxonLight.getFamily();
		
		Taxon division = getTaxon(state, divisionStr, Rank.DIVISION(), null, citation, microCitation);
		Taxon family = getTaxon(state, familyStr, Rank.FAMILY(), division, citation, microCitation);
		Taxon genus = getTaxon(state, genusStr, Rank.GENUS(), family, citation, microCitation);
		makeParent(state, genus, speciesTaxon, citation, microCitation)	;
	}


	private Taxon getTaxon(CyprusImportState state, String taxonNameStr, Rank rank, Taxon parent, Reference citation, String microCitation) {
		Taxon result;
		if (state.containsHigherTaxon(taxonNameStr)){
			result = state.getHigherTaxon(taxonNameStr);
		}else{
			result = (Taxon)createTaxon(state, rank, taxonNameStr, Taxon.class, nc);
			state.putHigherTaxon(taxonNameStr, result);
			if (parent == null){
				makeParent(state, null,result, citation, microCitation);
			}else{
				makeParent(state, parent, result, citation, microCitation);
			}
			
		}
		return result;
	}


	private void makeHomotypicSynonyms(CyprusImportState state,
			Reference citation, String microCitation, List<String> homotypicSynonymList, Taxon mainTaxon) {
		for (String homotypicSynonym: homotypicSynonymList){
			if (StringUtils.isNotBlank(homotypicSynonym)){
				Synonym synonym = (Synonym)createTaxon(state, null, homotypicSynonym, Synonym.class, nc);
				mainTaxon.addHomotypicSynonym(synonym, citation, microCitation);
			}
		}
	}


	private void makeHeterotypicSynonyms(CyprusImportState state, Reference citation, String microCitation, List<String> heterotypicSynonymList, Taxon mainTaxon) {
		for (String heterotypicSynonym: heterotypicSynonymList){
			if (StringUtils.isNotBlank(heterotypicSynonym)){
				Synonym synonym = (Synonym)createTaxon(state, null, heterotypicSynonym, Synonym.class, nc);
				mainTaxon.addSynonym(synonym, SynonymRelationshipType.HETEROTYPIC_SYNONYM_OF(), citation, microCitation);
			}
		}
	}


	private void makeSystematics(String systematicsString, Taxon mainTaxon) {
		//Systematics
		if (StringUtils.isNotBlank(systematicsString)){
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			//FIXME feature type
			TextData textData = TextData.NewInstance(Feature.ANATOMY());
			textData.putText(systematicsString, Language.UNDETERMINED());
			td.addElement(textData);
		}
	}


	private void makeEndemism(String endemismString, Taxon mainTaxon) {
		//endemism
		if (StringUtils.isNotBlank(endemismString)){
			boolean flag;
			if (endemismString.trim().equalsIgnoreCase("not endemic")){
				flag = false;
			}else if (endemismString.trim().equalsIgnoreCase("endemic")){
				flag = true;
			}else{
				throw new RuntimeException(endemismString + " is not a valid value for endemism");
			}
			//FIXME marker type
			Marker marker = Marker.NewInstance(MarkerType.IS_DOUBTFUL(), flag);
			mainTaxon.addMarker(marker);
		}
	}


	private void makeStatus(String statusString, Taxon mainTaxon) {
		//status
		if (StringUtils.isNotBlank(statusString)){
			PresenceTerm status = null;
			if (statusString.contains("Indigenous")){
				//FIXME 
				status = PresenceTerm.INTRODUCED();
			}else if (statusString.contains("Casual") || statusString.contains("Causal")){
				//FIXME
				status = PresenceTerm.CULTIVATED();
			}else if (statusString.contains("Cultivated")){
				status = PresenceTerm.CULTIVATED();
			}else if (statusString.contains("non-invasive")){
				//FIXME
				status = PresenceTerm.NATURALISED();
			}else if (statusString.contains("invasive")){
				//FIXME
				status = PresenceTerm.NATURALISED();
			}else if (statusString.contains("Questionable")){
				//FIXME
				status = PresenceTerm.NATIVE_PRESENCE_QUESTIONABLE();
			}else if (statusString.startsWith("F")){
				//FIXME
				status = PresenceTerm.NATIVE_PRESENCE_QUESTIONABLE();
			}else if (statusString.equals("##")){
				//FIXME
				status = PresenceTerm.NATIVE_PRESENCE_QUESTIONABLE();
			}else{
				logger.warn("Unknown status: " + statusString);
				status = PresenceTerm.PRESENT();
			}
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			NamedArea area = TdwgArea.getAreaByTdwgAbbreviation("CYP");
			Distribution distribution = Distribution.NewInstance(area, status);
			td.addElement(distribution);
			
			//text data
			//FIXME feature 
			TextData textData = TextData.NewInstance(Feature.DISTRIBUTION());
			textData.putText(statusString, Language.ENGLISH());
			td.addElement(textData);
		}
	}


	private void makeRedBookCategory(String redBookCategory, Taxon mainTaxon) {
		//red data book category
		if (StringUtils.isNotBlank(redBookCategory)){
			TaxonDescription td = this.getTaxonDescription(mainTaxon, false, true);
			//FIXME feature type
			TextData textData = TextData.NewInstance(Feature.DESCRIPTION());
			textData.putText(redBookCategory, Language.ENGLISH());
			td.addElement(textData);
		}
	}




	/** 
	 *  Stores parent-child, synonym and common name relationships
	 */
	@Override
    protected boolean secondPass(CyprusImportState state) {
		boolean success = true;
		CyprusRow cyprusRow = state.getCyprusRow();
//		try {
//			String taxonNameStr = state.getTaxonLight().getScientificName();
//			String nameStatus = state.getTaxonLight().getNameStatus();
//			String commonNameStr = state.getTaxonLight().getCommonName();
//			Integer parentId = state.getTaxonLight().getParentId();
//			Integer childId = state.getTaxonLight().getId();
//			
//			Taxon parentTaxon = (Taxon)state.getTaxonBase(parentId);
//			if (CdmUtils.isNotEmpty(taxonNameStr)) {
//				nameStatus = CdmUtils.Nz(nameStatus).trim().toLowerCase();
//				if (validMarkers.contains(nameStatus)){
//					Taxon taxon = (Taxon)state.getTaxonBase(childId);
//					// Add the parent relationship
//					if (state.getTaxonLight().getParentId() != 0) {
//						if (parentTaxon != null) {
//							//Taxon taxon = (Taxon)state.getTaxonBase(childId);
//							
//							Reference citation = state.getConfig().getSourceReference();
//							String microCitation = null;
//							Taxon childTaxon = taxon;
//							success &= makeParent(state, parentTaxon, childTaxon, citation, microCitation);
//							getTaxonService().saveOrUpdate(parentTaxon);
//						} else {
//							logger.warn("Taxonomic parent not found for " + taxonNameStr);
//							success = false;
//						}
//					}else{
//						//do nothing (parent == 0) no parent exists
//					}
//				}else if (synonymMarkers.contains(nameStatus)){
//					//add synonym relationship
//					try {
//						TaxonBase taxonBase = state.getTaxonBase(childId);
//						Synonym synonym = CdmBase.deproxy(taxonBase,Synonym.class);
//						parentTaxon.addSynonym(synonym, SynonymRelationshipType.SYNONYM_OF());
//						getTaxonService().saveOrUpdate(parentTaxon);
//					} catch (Exception e) {
//						logger.warn("Child id = " + childId);
//						e.printStackTrace();
//					}
//				}
//			} 
//			if (CdmUtils.isNotEmpty(commonNameStr)){			// add common name to taxon
//				handleCommonName(state, taxonNameStr, commonNameStr, parentId);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return success;
	}



	/**
	 * @param state
	 * @param rank
	 * @param taxonNameStr
	 * @param authorStr
	 * @param nameStatus
	 * @param nc
	 * @return
	 */
	private TaxonBase createTaxon(CyprusImportState state, Rank rank, String taxonNameStr, 
			Class statusClass, NomenclaturalCode nc) {
		TaxonBase taxonBase;
		NonViralName taxonNameBase = null;
		if (nc == NomenclaturalCode.ICVCN){
			logger.warn("ICVCN not yet supported");
			
		}else{
			taxonNameBase =(NonViralName) nc.getNewTaxonNameInstance(rank);
			//NonViralName nonViralName = (NonViralName)taxonNameBase;
			INonViralNameParser parser = nameParser;//NonViralNameParserImpl.NewInstance();
			taxonNameBase = (NonViralName<BotanicalName>)parser.parseFullName(taxonNameStr, nc, rank);
			
			taxonNameBase.setNameCache(taxonNameStr);
			
		}

		//Create the taxon
		Reference sec = state.getConfig().getSourceReference();
		// Create the status
		if (statusClass.equals(Taxon.class)){
			taxonBase = Taxon.NewInstance(taxonNameBase, sec);
		}else if (statusClass.equals(Synonym.class)){
			taxonBase = Synonym.NewInstance(taxonNameBase, sec);
		}else {
			Taxon taxon = Taxon.NewInstance(taxonNameBase, sec);
			taxon.setTaxonStatusUnknown(true);
			taxonBase = taxon;
		}
		return taxonBase;
	}

	private boolean makeParent(CyprusImportState state, Taxon parentTaxon, Taxon childTaxon, Reference citation, String microCitation){
		boolean success = true;
		Reference sec = state.getConfig().getSourceReference();
		
//		Reference sec = parentTaxon.getSec();
		Classification tree = state.getTree(sec);
		if (tree == null){
			tree = makeTree(state, sec);
		}
		if (sec.equals(childTaxon.getSec())){
			success &=  (null !=  tree.addParentChild(parentTaxon, childTaxon, citation, microCitation));
		}else{
			logger.warn("No relationship added for child " + childTaxon.getTitleCache());
		}
		return success;
	}
	

	
}
