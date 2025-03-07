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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.globis.validation.GlobisCurrentSpeciesImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class GlobisCurrentSpeciesImport  extends GlobisImportBase<Taxon> {

    private static final long serialVersionUID = -4392659482520384118L;
    private static final Logger logger = LogManager.getLogger();

	private int modCount = 10000;
	private static final String pluralString = "current taxa";
	private static final String dbTableName = "current_species";
	private static final Class<?> cdmTargetClass = Taxon.class;  //not needed

	public GlobisCurrentSpeciesImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strRecordQuery =
			" SELECT IDcurrentspec " +
			" FROM " + dbTableName;
		return strRecordQuery;
	}

	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery =
			" SELECT cs.*, cs.dtSpcEingabedatum as Created_When, cs.dtSpcErfasser as Created_Who," +
				"  cs.dtSpcBearbeiter as Updated_who, cs.dtSpcAendrgdatum as Updated_When, cs.dtSpcBemerkung as Notes " +
			" FROM " + getTableName() + " cs " +
			" WHERE ( cs.IDcurrentspec IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;

		@SuppressWarnings("rawtypes")
        Set<TaxonBase> objectsToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, Taxon> taxonMap = partitioner.getObjectMap(TAXON_NAMESPACE);
		ResultSet rs = partitioner.getResultSet();

		Classification classification = getClassification(state);

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

        		Integer taxonId = rs.getInt("IDcurrentspec");

        		//String dtSpcJahr -> ignore !
        		//empty: fiSpcLiteratur

        		//TODO
        		//fiSpcspcgrptax

				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();
					Taxon nextHigherTaxon = null;

					boolean hasNewParent = false; //true if any parent is new

					//species
					Taxon species = createObject(rs, state, taxonId);


					String familyStr = rs.getString("dtSpcFamakt");
					String subFamilyStr = rs.getString("dtSpcSubfamakt");
					String tribeStr = rs.getString("dtSpcTribakt");

					//family
					Taxon family = getTaxon(state, rs, familyStr, null, Rank.FAMILY(), null, taxonMap, taxonId);

					//subfamily
					Taxon subFamily = getTaxon(state, rs, subFamilyStr, null, Rank.SUBFAMILY(), null, taxonMap, taxonId);
					Taxon subFamilyParent = getParent(subFamily, classification);
					if (subFamilyParent != null){
						if (! compareTaxa(family, subFamilyParent)){
							logger.warn("Current family and parent of subfamily are not equal: " + taxonId);
						}
					}else{
						classification.addParentChild(family, subFamily, sourceRef, null);
					}
					nextHigherTaxon = subFamily;

					//tribe
					Taxon tribe = getTaxon(state, rs, tribeStr, null, Rank.TRIBE(), null, taxonMap, taxonId);
					if (tribe != null){
						Taxon tribeParent = getParent(tribe, classification);
						if (tribeParent != null){
							if (! compareTaxa(subFamily, tribeParent)){
								logger.warn("Current subFamily and parent of tribe are not equal: " + taxonId);
							}
						}else{
							classification.addParentChild(subFamily, tribe, sourceRef, null);
						}
						nextHigherTaxon = tribe;
					}


					//genus
					String genusStr = rs.getString("dtSpcGenusakt");
					String genusAuthorStr = rs.getString("dtSpcGenusaktauthor");
					Taxon genus = getTaxon(state, rs, genusStr, null, Rank.GENUS(), genusAuthorStr, taxonMap, taxonId);
					Taxon genusParent = getParent(genus, classification);

					if (genusParent != null){
						if (! compareTaxa(genusParent, nextHigherTaxon)){
							logger.warn("Current tribe/subfamily and parent of genus are not equal: " + taxonId);
						}
					}else{
						classification.addParentChild(nextHigherTaxon, genus, sourceRef, null);
					}
					nextHigherTaxon = genus;

					//subgenus
					String subGenusStr = CdmBase.deproxy(species.getName(), TaxonName.class).getInfraGenericEpithet();
					String subGenusAuthorStr = rs.getString("dtSpcSubgenaktauthor");
					boolean hasSubgenus = StringUtils.isNotBlank(subGenusStr) || StringUtils.isNotBlank(subGenusAuthorStr);
					if (hasSubgenus){
						Taxon subGenus = getTaxon(state, rs, genusStr, subGenusStr, Rank.SUBGENUS(), subGenusAuthorStr, taxonMap, taxonId);
						classification.addParentChild(nextHigherTaxon, subGenus, sourceRef, null);
						nextHigherTaxon = subGenus;
					}

					classification.addParentChild(nextHigherTaxon, species, sourceRef, null);

					handleCountries(state, rs, species, taxonId);

					//common names -> not used anymore
					handleCommonNames(state, rs, species);

					this.doIdCreatedUpdatedNotes(state, species, rs, taxonId, TAXON_NAMESPACE);

					objectsToSave.add(species);


				} catch (Exception e) {
					logger.warn("Exception in current_species: IDcurrentspec " + taxonId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void handleCountries(GlobisImportState state, ResultSet rs, Taxon species, Integer taxonId) throws SQLException {
		String countriesStr = rs.getString("dtSpcCountries");
		if (isBlank(countriesStr)){
			return;
		}
		String[] countriesSplit = countriesStr.split(";");
		for (String countryStr : countriesSplit){
			if (isBlank(countryStr)){
				continue;
			}
			countryStr = countryStr.trim();

			//TODO use isComplete
			boolean isComplete = countryStr.endsWith(".");
			if (isComplete){
				countryStr = countryStr.substring(0,countryStr.length() - 1).trim();
			}
			boolean isDoubtful = countryStr.endsWith("[?]");
			if (isDoubtful){
				countryStr = countryStr.substring(0,countryStr.length() - 3).trim();
			}
			if (countryStr.startsWith("?")){
				isDoubtful = true;
				countryStr = countryStr.substring(1).trim();
			}

			countryStr = normalizeCountry(countryStr);

			NamedArea country = getCountry(state, countryStr);

			PresenceAbsenceTerm status;
			if (isDoubtful){
				status = PresenceAbsenceTerm.PRESENT_DOUBTFULLY();
			}else{
				status = PresenceAbsenceTerm.PRESENT();
			}

			if (country != null){
				TaxonDescription desc = getTaxonDescription(species, state.getTransactionalSourceReference(), false, true);
				Distribution distribution = Distribution.NewInstance(country, status);
				desc.addElement(distribution);
			}else{
				if (countryStr.length() > 0){
					logger.warn("Country string not recognized : " + countryStr + " for IDcurrentspec " + taxonId);
				}
			}
		}
	}

	private String normalizeCountry(String countryStr) {
		String result = countryStr.trim();
		if (result.endsWith(".")){
			result = result.substring(0,result.length() - 1);
		}
		while (result.startsWith(UTF8.NO_BREAK_SPACE.toString())){
			result = result.substring(1);  //
		}
		if (result.matches("\\s+")){
			result = "";
		}
		return result.trim();
	}

	private void handleCommonNames(GlobisImportState state, ResultSet rs, Taxon species) throws SQLException {
		//DON't use, use seperate common name tables instead

//		String commonNamesStr = rs.getString("vernacularnames");
//		if (isBlank(commonNamesStr)){
//			return;
//		}
//		String[] commonNamesSplit = commonNamesStr.split(";");
//		for (String commonNameStr : commonNamesSplit){
//			if (isBlank(commonNameStr)){
//				continue;
//			}
//			Language language = null; //TODO
//			CommonTaxonName commonName = CommonTaxonName.NewInstance(commonNameStr, language);
//			TaxonDescription desc = getTaxonDescription(species, state.getTransactionalSourceReference(), false, true);
//			desc.addElement(commonName);
//		}
	}

	/**
	 * Compares 2 taxa, returns true of both taxa look similar
	 * @param genus
	 * @param nextHigherTaxon
	 * @return
	 */
	private boolean compareTaxa(Taxon taxon1, Taxon taxon2) {
		IZoologicalName name1 = taxon1.getName();
		IZoologicalName name2 = taxon2.getName();
		if (!name1.getRank().equals(name2.getRank())){
			return false;
		}
		if (! name1.getTitleCache().equals(name2.getTitleCache())){
			return false;
		}
		return true;
	}

	private Taxon getParent(Taxon child, Classification classification) {
		if (child == null){
			logger.warn("Child is null");
			return null;
		}
		for (TaxonNode node :  child.getTaxonNodes()){
			if (node.getClassification().equals(classification)){
				if (node.getParent() != null){
					return node.getParent().getTaxon();
				}else{
					return null;
				}
			}
		}
		return null;
	}

	private Taxon getTaxon(GlobisImportState state, ResultSet rs, String uninomial, String infraGenericEpi, Rank rank, String author, Map<String, Taxon> taxonMap, Integer taxonId) {
		if (isBlank(uninomial)){
			return null;
		}

		String keyEpithet = StringUtils.isNotBlank(infraGenericEpi)? infraGenericEpi : uninomial ;

		String key = keyEpithet + "@" + CdmUtils.Nz(author) + "@" + rank.getTitleCache();
		Taxon taxon = taxonMap.get(key);
		if (taxon == null){
			IZoologicalName name = TaxonNameFactory.NewZoologicalInstance(rank);
			name.setGenusOrUninomial(uninomial);
			if (isNotBlank(infraGenericEpi)){
				name.setInfraGenericEpithet(infraGenericEpi);
			}
			taxon = Taxon.NewInstance(name, state.getTransactionalSourceReference());

			taxonMap.put(key, taxon);
			handleAuthorAndYear(author, name, taxonId, state);
			getTaxonService().save(taxon);
		}

		return taxon;
	}


	//fast and dirty is enough here
	private Classification classification;

	private Classification getClassification(GlobisImportState state) {
		if (this.classification == null){
			String name = state.getConfig().getClassificationName();
			Reference reference = state.getTransactionalSourceReference();
			this.classification = Classification.NewInstance(name, reference, Language.DEFAULT());
			classification.setUuid(state.getConfig().getClassificationUuid());
			getClassificationService().save(classification);
		}
		return this.classification;

	}

	public Taxon createObject(ResultSet rs, GlobisImportState state, Integer taxonId)
			throws SQLException {
		String speciesEpi = rs.getString("dtSpcSpcakt");
		String subGenusEpi = rs.getString("dtSpcSubgenakt");
		String genusEpi = rs.getString("dtSpcGenusakt");
		String author = rs.getString("dtSpcAutor");


		IZoologicalName zooName = TaxonNameFactory.NewZoologicalInstance(Rank.SPECIES());
		zooName.setSpecificEpithet(speciesEpi);
		if (StringUtils.isNotBlank(subGenusEpi)){
			zooName.setInfraGenericEpithet(subGenusEpi);
		}
		zooName.setGenusOrUninomial(genusEpi);
		handleAuthorAndYear(author, zooName, taxonId, state);

		Taxon taxon = Taxon.NewInstance(zooName, state.getTransactionalSourceReference());

		return taxon;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, GlobisImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		try{
			Set<String> taxonIdSet = new HashSet<>();

			while (rs.next()){
//				handleForeignKey(rs, taxonIdSet, "taxonId");
			}

			//taxon map
			nameSpace = TAXON_NAMESPACE;
			idSet = taxonIdSet;
			Map<String, Taxon> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(Taxon.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(GlobisImportState state){
		IOValidator<GlobisImportState> validator = new GlobisCurrentSpeciesImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoCurrentTaxa();
	}





}
