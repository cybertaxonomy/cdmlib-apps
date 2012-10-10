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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.globis.validation.GlobisCurrentSpeciesImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.ZoologicalName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class GlobisImageImport  extends GlobisImportBase<Taxon> {
	private static final Logger logger = Logger.getLogger(GlobisImageImport.class);
	
	private int modCount = 10000;
	private static final String pluralString = "images";
	private static final String dbTableName = "Einzelbilder";
	private static final Class cdmTargetClass = Media
	.class;  //not needed
	
	private static final String IMAGE_NAMESPACE = "Einzelbilder";
	
	public GlobisImageImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.globis.GlobisImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery() {
		String strRecordQuery = 
			" SELECT BildId " + 
			" FROM " + dbTableName; 
		return strRecordQuery;	
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery = 
			" SELECT i.*, NULL as Created_When, NULL as Created_Who," +
				"  NULL as Updated_who, NULL as Updated_When, NULL as Notes " + 
			" FROM " + getTableName() + " i " +
			" WHERE ( i.BildId IN (" + ID_LIST_TOKEN + ") )";
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
//		Map<String, DerivedUnit> ecoFactDerivedUnitMap = (Map<String, DerivedUnit>) partitioner.getObjectMap(ECO_FACT_DERIVED_UNIT_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		Classification classification = getClassification(state);
		
		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
        		Integer taxonId = rs.getInt("IDcurrentspec");
        		
        		
        		//TODO
        		//String dtSpcJahr,
        		//dtSpcFamakt,dtSpcSubfamakt,dtSpcTribakt,
        		//fiSpcLiteratur, fiSpcspcgrptax, dtSpcCountries,vernacularnames
        		
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
					Taxon nextHigherTaxon = null;
					
					Taxon species = createObject(rs, state);
					
					//subgenus
					String subGenusStr = rs.getString("dtSpcSubgenakt");
					String subGenusAuthorStr = rs.getString("dtSpcSubgenaktauthor");
					boolean hasSubgenus = StringUtils.isNotBlank(subGenusStr) || StringUtils.isNotBlank(subGenusAuthorStr);
					if (hasSubgenus){
						Taxon subGenus = getTaxon(state, rs, subGenusStr, Rank.SUBGENUS(), subGenusAuthorStr, taxonMap);
						classification.addParentChild(subGenus, species, sourceRef, null);
						nextHigherTaxon = getParent(subGenus, classification);
					}
					
					//genus
					String genusStr = rs.getString("dtSpcGenusakt");
					String genusAuthorStr = rs.getString("dtSpcGenusaktauthor");
					Taxon genus = getTaxon(state, rs, genusStr, Rank.GENUS(), genusAuthorStr, taxonMap);
					if (nextHigherTaxon != null){
						if (! compareTaxa(genus, nextHigherTaxon)){
							logger.warn("Current genus and parent of subgenus are not equal: " + taxonId);
						}
					}else{
						classification.addParentChild(genus, species, sourceRef, null);
						nextHigherTaxon = getParent(genus, classification);
					}
					
					this.doIdCreatedUpdatedNotes(state, species, rs, taxonId, REFERENCE_NAMESPACE);
					
					objectsToSave.add(species); 
					

				} catch (Exception e) {
					logger.warn("Exception in current_species: IDcurrentspec " + taxonId + ". " + e.getMessage());
//					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	/**
	 * Compares 2 taxa, returns true of both taxa look similar
	 * @param genus
	 * @param nextHigherTaxon
	 * @return
	 */
	private boolean compareTaxa(Taxon taxon1, Taxon taxon2) {
		ZoologicalName name1 = CdmBase.deproxy(taxon1.getName(), ZoologicalName.class);
		ZoologicalName name2 = CdmBase.deproxy(taxon2.getName(), ZoologicalName.class);
		if (!name1.getRank().equals(name2.getRank())){
			return false;
		}
		if (! name1.getTitleCache().equals(name2.getTitleCache())){
			return false;
		}
		return true;
	}




	private Taxon getParent(Taxon subgenus, Classification classification) {
		for (TaxonNode node :  subgenus.getTaxonNodes()){
			if (node.getClassification().equals(classification)){
				return node.getParent().getTaxon();
			}
		}
		return null;
	}




	private Taxon getTaxon(GlobisImportState state, ResultSet rs, String subGenus, Rank rank, String author, Map<String, Taxon> taxonMap) {
		String key = subGenus + "@" + "subGenusAuthor" + "@" + rank.getTitleCache();
		Taxon taxon = taxonMap.get(key);
		if (taxon == null){
			ZoologicalName name = ZoologicalName.NewInstance(rank);
			taxon = Taxon.NewInstance(name, state.getTransactionalSourceReference());
			handleAuthorAndYear(author, name);
			getTaxonService().save(taxon);
		}
		
		return taxon;
	}


	//fast and dirty is enough here
	private Classification classification;
	
	private Classification getClassification(GlobisImportState state) {
		if (this.classification == null){
			String name = state.getConfig().getClassificationName();
			Reference<?> reference = state.getTransactionalSourceReference();
			this.classification = Classification.NewInstance(name, reference, Language.DEFAULT());
			classification.setUuid(state.getConfig().getClassificationUuid());
			getClassificationService().save(classification);
		}
		return this.classification;
		
	}

	private INonViralNameParser parser = NonViralNameParserImpl.NewInstance();
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.IMappingImport#createObject(java.sql.ResultSet, eu.etaxonomy.cdm.io.common.ImportStateBase)
	 */
	public Taxon createObject(ResultSet rs, GlobisImportState state)
			throws SQLException {
		String speciesEpi = rs.getString("dtSpcSpcakt");
		String subGenusEpi = rs.getString("dtSpcSubgenakt");
		String genusEpi = rs.getString("dtSpcGenusakt");
		String author = rs.getString("dtSpcAutor");
		
		
		ZoologicalName zooName = ZoologicalName.NewInstance(Rank.SPECIES());
		zooName.setSpecificEpithet(speciesEpi);
		if (StringUtils.isNotBlank(subGenusEpi)){
			zooName.setInfraSpecificEpithet(subGenusEpi);
		}
		zooName.setGenusOrUninomial(genusEpi);
		handleAuthorAndYear(author, zooName);
		
		Taxon taxon = Taxon.NewInstance(zooName, state.getTransactionalSourceReference());
		
		return taxon;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		return result;  //not needed
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(GlobisImportState state){
		IOValidator<GlobisImportState> validator = new GlobisCurrentSpeciesImportValidator();
		return validator.validate(state);
	}
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoImages();
	}





}
