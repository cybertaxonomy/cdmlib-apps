// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportNameTypeDesignationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportSynonymMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTaxIncludedInMapper;
import eu.etaxonomy.cdm.io.common.mapping.ICheckIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.IDbImportMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * @author a.mueller
 * @created 09.03.2010
 */
@Component
public class ErmsTaxonRelationImport extends ErmsImportBase<TaxonBase<?>> implements ICheckIgnoreMapper{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsTaxonRelationImport.class);
	
	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;
	
	private static final String pluralString = "taxon relations";
	private static final String dbTableName = "tu";

	private static final Class<?> cdmTargetClass = TaxonBase.class;
	
	private ErmsImportState state;  //ERMS import will never run in more then one instance
	
	public ErmsTaxonRelationImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<ErmsImportState, ErmsImportConfigurator>();
			
			mapping.addMapper(DbImportTaxIncludedInMapper.NewInstance("id", TAXON_NAMESPACE, "parentId", TAXON_NAMESPACE, "accParentId", TAXON_NAMESPACE, null));//there is only one tree
			
			TaxonRelationshipType taxonRelationshipType = getTaxonRelationshipType(state, ErmsTransformer.uuidTaxRelTypeIsTaxonSynonymOf, "is taxon synonym of", "is synonym of relation used by synonym that are of class Taxon as they can not be handled differently", null, null);
			mapping.addMapper(DbImportSynonymMapper.NewInstance("id", "tu_acctaxon", TAXON_NAMESPACE, null, taxonRelationshipType)); 			
			mapping.addMapper(DbImportNameTypeDesignationMapper.NewInstance("id", "tu_typetaxon", ErmsTaxonImport.NAME_NAMESPACE, "tu_typedesignationstatus"));
//			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("tu_acctaxon"));
		}
		return mapping;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getRecordQuery(eu.etaxonomy.cdm.io.erms.ErmsImportConfigurator)
	 */
	protected String getRecordQuery(ErmsImportConfigurator config) {
		//TODO get automatic by second path mappers
		String selectAttributes = " myTaxon.id, myTaxon.tu_parent, myTaxon.tu_typetaxon, myTaxon.tu_typedesignation, " +
								" myTaxon.tu_acctaxon, myTaxon.tu_status, parent.tu_status AS parentStatus, parent.id AS parentId, " + 
								" accParent.tu_status AS accParentStatus, accParent.id AS accParentId "; 
		String strRecordQuery = 
			" SELECT  " + selectAttributes + 
			" FROM tu AS myTaxon LEFT OUTER JOIN " +
				" tu AS accTaxon ON myTaxon.tu_acctaxon = accTaxon.id LEFT OUTER JOIN " +
				" tu AS accParent RIGHT OUTER JOIN "  + 
				" tu AS parent ON accParent.id = parent.tu_acctaxon ON myTaxon.tu_parent = parent.id " +
			" WHERE ( myTaxon.id IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}
	

	@Override
	protected void doInvoke(ErmsImportState state) {
		this.state = state;
		super.doInvoke(state);
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> taxonIdSet = new HashSet<String>();
			Set<String> nameIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "parentId");
				handleForeignKey(rs, taxonIdSet, "accParentId");
				handleForeignKey(rs, taxonIdSet, "tu_acctaxon");
				handleForeignKey(rs, taxonIdSet, "id");
				handleForeignKey(rs, nameIdSet, "tu_typetaxon");
				handleForeignKey(rs, nameIdSet, "id");		
			}
			
			//name map
			nameSpace = ErmsTaxonImport.NAME_NAMESPACE;
			cdmClass = TaxonNameBase.class;
			idSet = nameIdSet;
			Map<String, TaxonNameBase<?,?>> nameMap = (Map<String, TaxonNameBase<?,?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, nameMap);
			
			
			//taxon map
			nameSpace = ErmsTaxonImport.TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ICheckIgnoreMapper#checkIgnoreMapper(eu.etaxonomy.cdm.io.common.mapping.IDbImportMapper, java.sql.ResultSet)
	 */
	public boolean checkIgnoreMapper(IDbImportMapper mapper, ResultSet rs) throws SQLException{
		boolean result = false;
		if (mapper instanceof DbImportTaxIncludedInMapper){
			//old
//			int tu_status = rs.getInt("tu_status");
//			if (tu_status != 1){
//				result = true;
//			}
			
			int id = rs.getInt("id");
			if (state.getAcceptedTaxaKeys().contains(id)){
				return false;
			}else{
				return true;
			}
		}else if (mapper instanceof DbImportSynonymMapper){
			//old
//			int tu_status = rs.getInt("tu_status");
//			if (tu_status == 1){
//				result = true;
//			}else{
//				return false;
//			}t.tu_acctaxon <> t.id
			
			int id = rs.getInt("id");
			Object accTaxonId = rs.getObject("tu_acctaxon");
			if (accTaxonId == null){
				return true;
			}else{
				int accId = Integer.valueOf(String.valueOf(accTaxonId));
				return accId == id;
			}
		}else if (mapper instanceof DbImportNameTypeDesignationMapper){
			Object tu_typeTaxon = rs.getObject("tu_typetaxon");
			if (tu_typeTaxon == null){
				return true;
			}
		}
		return result;
	}
	
	
	@Override
	protected boolean doCheck(ErmsImportState state){
//		IOValidator<ErmsImportState> validator = new ErmsTaxonImportValidator();
//		return validator.validate(state);
		return true;
	}
	
	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoRelTaxa();
	}



}