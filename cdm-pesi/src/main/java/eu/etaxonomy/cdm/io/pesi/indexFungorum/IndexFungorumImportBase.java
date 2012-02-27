/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @created 27.02.2012
 */
public abstract class IndexFungorumImportBase<CDM_BASE extends CdmBase> extends CdmImportBase<IndexFungorumImportConfigurator, IndexFungorumImportState> implements ICdmIO<IndexFungorumImportState>, IPartitionedIO<IndexFungorumImportState> {
	private static final Logger logger = Logger.getLogger(IndexFungorumImportBase.class);
	
	//NAMESPACES
	
	protected static final String REFERENCE_NAMESPACE = "Source";
	protected static final String TAXON_NAMESPACE = "Taxon";

	
	

	private String pluralString;
	private String dbTableName;
	//TODO needed?
	private Class cdmTargetClass;

	
	
	/**
	 * @param dbTableName
	 * @param dbTableName2 
	 */
	public IndexFungorumImportBase(String pluralString, String dbTableName, Class cdmTargetClass) {
		this.pluralString = pluralString;
		this.dbTableName = dbTableName;
		this.cdmTargetClass = cdmTargetClass;
	}

	protected void doInvoke(IndexFungorumImportState state){
		logger.info("start make " + getPluralString() + " ...");
		IndexFungorumImportConfigurator config = state.getConfig();
		Source source = config.getSource();
			
		String strIdQuery = getIdQuery();
		String strRecordQuery = getRecordQuery(config);

		int recordsPerTransaction = config.getRecordsPerTransaction();
		try{
			ResultSetPartitioner partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
			return;
		}
		
		logger.info("end make " + getPluralString() + " ... " + getSuccessString(true));
		return;
	}
	
	public boolean doPartition(ResultSetPartitioner partitioner, IndexFungorumImportState state) {
		boolean success = true ;
		Set objectsToSave = new HashSet<CdmBase>();
		
// 		DbImportMapping<?, ?> mapping = getMapping();
//		mapping.initialize(state, cdmTargetClass);
		
		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
//				success &= mapping.invoke(rs,objectsToSave);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	
		partitioner.startDoSave();
		getCommonService().save(objectsToSave);
		return success;
	}


	/**
	 * @return
	 */
	protected abstract String getRecordQuery(IndexFungorumImportConfigurator config);

	/**
	 * @return
	 */
	protected String getIdQuery(){
		String result = " SELECT id FROM " + getTableName();
		return result;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getPluralString()
	 */
	public String getPluralString(){
		return pluralString;
	}

	/**
	 * @return
	 */
	protected String getTableName(){
		return this.dbTableName;
	}

	
	protected boolean resultSetHasColumn(ResultSet rs, String columnName){
		try {
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 0; i < metaData.getColumnCount(); i++){
				if (metaData.getColumnName(i + 1).equalsIgnoreCase(columnName)){
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
            logger.warn("Exception in resultSetHasColumn");
            return false;
		}
	}
	
	protected boolean checkSqlServerColumnExists(Source source, String tableName, String columnName){
		String strQuery = "SELECT  Count(t.id) as n " +
				" FROM sysobjects AS t " +
				" INNER JOIN syscolumns AS c ON t.id = c.id " +
				" WHERE (t.xtype = 'U') AND " + 
				" (t.name = '" + tableName + "') AND " + 
				" (c.name = '" + columnName + "')";
		ResultSet rs = source.getResultSet(strQuery) ;		
		int n;
		try {
			rs.next();
			n = rs.getInt("n");
			return n>0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Returns a map that holds all values of a ResultSet. This is needed if a value needs to
	 * be accessed twice
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected Map<String, Object> getValueMap(ResultSet rs) throws SQLException{
		try{
			Map<String, Object> valueMap = new HashMap<String, Object>();
			int colCount = rs.getMetaData().getColumnCount();
			for (int c = 0; c < colCount ; c++){
				Object value = rs.getObject(c+1);
				String label = rs.getMetaData().getColumnLabel(c+1).toLowerCase();
				if (value != null && ! CdmUtils.Nz(value.toString()).trim().equals("")){
					valueMap.put(label, value);
				}
			}
			return valueMap;
		}catch(SQLException e){
			throw e;
		}
	}


	/**
	 * Reads a foreign key field from the result set and adds its value to the idSet.
	 * @param rs
	 * @param teamIdSet
	 * @throws SQLException
	 */
	protected void handleForeignKey(ResultSet rs, Set<String> idSet, String attributeName)
			throws SQLException {
		Object idObj = rs.getObject(attributeName);
		if (idObj != null){
			String id  = String.valueOf(idObj);
			idSet.add(id);
		}
	}
	
	/**
	 * Returns true if i is a multiple of recordsPerTransaction
	 * @param i
	 * @param recordsPerTransaction
	 * @return
	 */
	protected boolean loopNeedsHandling(int i, int recordsPerLoop) {
		startTransaction();
		return (i % recordsPerLoop) == 0;
	}
	
	protected void doLogPerLoop(int count, int recordsPerLog, String pluralString){
		if ((count % recordsPerLog ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
	}
	


	
}
