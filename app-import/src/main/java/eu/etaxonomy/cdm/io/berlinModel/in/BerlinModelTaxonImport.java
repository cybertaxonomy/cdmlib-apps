/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_ACCEPTED;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_PARTIAL_SYN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_PRO_PARTE_SYN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_SYNONYM;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.T_STATUS_UNRESOLVED;

import java.lang.reflect.Method;
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

import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.03.2008
 * @version 1.0
 */
@Component
public class BerlinModelTaxonImport  extends BerlinModelImportBase {
	private static final Logger logger = Logger.getLogger(BerlinModelTaxonImport.class);
	
	public static final String NAMESPACE = "Taxon";

	private static final String pluralString = "Taxa";
	private static final String dbTableName = "PTaxon";
	
	/**
	 * How should the publish flag in table PTaxon be interpreted
	 * NO_MARKER: No marker is set
	 * ONLY_FALSE: 
	 */
	public enum PublishMarkerChooser{
		NO_MARKER,
		ONLY_FALSE,
		ONLY_TRUE,
		ALL;
		
		boolean doMark(boolean value){
			if (value == true){
				return this == ALL || this == ONLY_TRUE;
			}else{
				return this == ALL || this == ONLY_FALSE;
			}
		}
	}
	
	public BerlinModelTaxonImport(){
		super(dbTableName, pluralString);
	}
	
	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String sqlSelect = " SELECT RIdentifier";
		String taxonTable = state.getConfig().getTaxonTable();
		String sqlFrom = String.format(" FROM %s ", taxonTable);
		String sqlWhere = "";
		
		String sql = sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return sql;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String sqlSelect = " SELECT pt.*  ";
		String sqlFrom = " FROM PTaxon pt "; 
		if (isEuroMed(config) ){
			sqlFrom = " FROM PTaxon AS pt " + 
							" INNER JOIN v_cdm_exp_taxaAll AS em ON pt.RIdentifier = em.RIdentifier " +
							" LEFT OUTER JOIN Reference r ON pt.LastScrutinyFk = r.RefId ";
			sqlSelect += " , em.MA, r.RefCache as LastScrutiny ";
		}
		
		
		String sqlWhere = " WHERE ( pt.RIdentifier IN (" + ID_LIST_TOKEN + ") )";
		
		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
//			" SELECT * " + 
//			" FROM PTaxon " + state.getConfig().getTaxonTable();
//			" WHERE ( RIdentifier IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	
	private boolean isEuroMed(BerlinModelImportConfigurator config) {
		return config.getTaxonTable().trim().equals("v_cdm_exp_taxaAll");
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonImportValidator();
		return validator.validate(state);
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		
		BerlinModelImportConfigurator config = state.getConfig();
		Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();
		Map<String, TaxonNameBase> taxonNameMap = (Map<String, TaxonNameBase>) partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		Map<String, Reference> refMap = (Map<String, Reference>) partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();
		try{
			boolean publishFlagExists = state.getConfig().getSource().checkColumnExists("PTaxon", "PublishFlag");
			boolean isEuroMed = isEuroMed(state.getConfig());
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}
				
				//create TaxonName element
				int taxonId = rs.getInt("RIdentifier");
				int statusFk = rs.getInt("statusFk");
				
				int nameFk = rs.getInt("PTNameFk");
				int refFkInt = rs.getInt("PTRefFk");
				String doubtful = rs.getString("DoubtfulFlag");
				String uuid = null;
				if (resultSetHasColumn(rs,"UUID")){
					uuid = rs.getString("UUID");
				}
				
				TaxonNameBase<?,?> taxonName = null;
				taxonName  = taxonNameMap.get(String.valueOf(nameFk));
				
				Reference<?> reference = null;
				String refFk = String.valueOf(refFkInt);
				reference = refMap.get(refFk); 
					
				if(! config.isIgnoreNull()){
					if (taxonName == null ){
						logger.warn("TaxonName belonging to taxon (RIdentifier = " + taxonId + ") could not be found in store. Taxon will not be imported");
						success = false;
						continue; //next taxon
					}else if (reference == null ){
						logger.warn("Reference belonging to taxon could not be found in store. Taxon will not be imported");
						success = false;
						continue; //next taxon
					}
				}
				TaxonBase<?> taxonBase;
				Synonym synonym;
				Taxon taxon;
				try {
					logger.debug(statusFk);
					if (statusFk == T_STATUS_ACCEPTED || statusFk == T_STATUS_UNRESOLVED ){
						taxon = Taxon.NewInstance(taxonName, reference);
						taxonBase = taxon;
						if (statusFk == T_STATUS_UNRESOLVED){
							taxon.setTaxonStatusUnknown(true);
						}
					}else if (statusFk == T_STATUS_SYNONYM || statusFk == T_STATUS_PRO_PARTE_SYN || statusFk == T_STATUS_PARTIAL_SYN){
						synonym = Synonym.NewInstance(taxonName, reference);
						taxonBase = synonym;
						if (statusFk == T_STATUS_PRO_PARTE_SYN){
							config.addProParteSynonym(synonym);
						}
						if (statusFk == T_STATUS_PARTIAL_SYN){
							config.addPartialSynonym(synonym);
						}
					}else{
						logger.warn("TaxonStatus " + statusFk + " not yet implemented. Taxon (RIdentifier = " + taxonId + ") left out.");
						success = false;
						continue;
					}
					if (uuid != null){
						taxonBase.setUuid(UUID.fromString(uuid));
					}
					
					//doubtful
					if (doubtful.equals("a")){
						taxonBase.setDoubtful(false);
					}else if(doubtful.equals("d")){
						taxonBase.setDoubtful(true);
					}else if(doubtful.equals("i")){
						taxonBase.setDoubtful(false);
						logger.warn("Doubtful = i (inactivated) does not exist in CDM. Doubtful set to false. RIdentifier: " + taxonId);
					}
					
					//detail
					String detail = rs.getString("Detail");
					if (StringUtils.isNotBlank(detail)){
						ExtensionType detailExtensionType = getExtensionType(state, BerlinModelTransformer.DETAIL_EXT_UUID, "micro reference","micro reference","micro ref.");
						Extension.NewInstance(taxonBase, detail, detailExtensionType);
					}
					//idInSource
					String idInSource = rs.getString("IdInSource");
					if (StringUtils.isNotBlank(idInSource)){
						ExtensionType detailExtensionType = getExtensionType(state, BerlinModelTransformer.ID_IN_SOURCE_EXT_UUID, "Berlin Model IdInSource","Berlin Model IdInSource","BM source id");
						Extension.NewInstance(taxonBase, idInSource, detailExtensionType);
					}
					//namePhrase
					String namePhrase = rs.getString("NamePhrase");
					if (StringUtils.isNotBlank(namePhrase)){
						taxonBase.setAppendedPhrase(namePhrase);
					}
					//useNameCache
					Boolean useNameCacheFlag = rs.getBoolean("UseNameCacheFlag");
					if (useNameCacheFlag){
						taxonBase.setUseNameCache(true);
					}
					//publisheFlag
					if (publishFlagExists){
						Boolean publishFlag = rs.getBoolean("PublishFlag");
						Boolean misapplied = false;
						if (isEuroMed){
							misapplied = rs.getBoolean("MA");
						}
						
						if ( ! misapplied){
							taxonBase.setPublish(publishFlag);
							if(taxonBase.isInstanceOf(Taxon.class) ){
								CdmBase.deproxy(taxonBase, Taxon.class).setPublish(publishFlag);
							}
						}
					}
					
					//
					if (resultSetHasColumn(rs,"LastScrutiny")){
						String lastScrutiny = rs.getString("LastScrutiny");
						ExtensionType extensionTypeSpeciesExpert = getExtensionType(state, BerlinModelTransformer.uuidSpeciesExpertName, "Species Expert", "Species Expert", "Species Expert");
						taxonBase.addExtension(lastScrutiny, extensionTypeSpeciesExpert);
						ExtensionType extensionTypeExpert = getExtensionType(state, BerlinModelTransformer.uuidExpertName, "Expert", "Expert for a taxonomic group", "Expert");
						taxonBase.addExtension(lastScrutiny, extensionTypeExpert);
					}
					
					//Notes
					doIdCreatedUpdatedNotes(state, taxonBase, rs, taxonId, NAMESPACE);
					
					//external url
					if (config.getMakeUrlForTaxon() != null){
						Method urlMethod = config.getMakeUrlForTaxon();
						urlMethod.invoke(null, taxonBase, rs);
					}
					
					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when creating taxon with id " + taxonId + ". Taxon could not be saved.");
					success = false;
				}
			}
		} catch (DatabaseTypeNotSupportedException e) {
			logger.error("MethodNotSupportedException:" +  e);
			return false;
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	
			
		//	logger.info( i + " names handled");
		getTaxonService().save(taxaToSave);
		return success;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> nameIdSet = new HashSet<String>();
			Set<String> referenceIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "PTNameFk");
				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}
			
			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			cdmClass = TaxonNameBase.class;
			idSet = nameIdSet;
			Map<String, Person> nameMap = (Map<String, Person>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	@Override
	protected String getTableName() {
		return dbTableName;
	}
	
	@Override
	public String getPluralString() {
		return pluralString;
	}
	
	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoTaxa();
	}



}