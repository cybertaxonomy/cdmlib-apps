/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.out.mapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.out.BerlinModelExportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.out.BerlinModelExportState;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbSingleAttributeExportMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.out.IDbExportMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.IndexCounter;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 12.05.2009
 */
public class RefDetailMapper extends DbSingleAttributeExportMapperBase<BerlinModelExportState>
        implements IDbExportMapper<BerlinModelExportState, IExportTransformer>{

    private static final Logger logger = LogManager.getLogger();

	private final String cdmRefAttributeString;
	private PreparedStatement preparedStatement;

	public static RefDetailMapper NewInstance(String cdmAttributeString, String cdmRefAttributeString, String dbAttributeString){
		return new RefDetailMapper(cdmAttributeString, cdmRefAttributeString, dbAttributeString);
	}

//	public static RefDetailMapper NewInstance(String cdmAttributeString, String dbAttributeString){
//		return new RefDetailMapper();
//	}

	private RefDetailMapper(String cdmAttributeString, String cdmRefAttributeString, String dbAttributeString) {
		super(cdmAttributeString, dbAttributeString, null);
		this.cdmRefAttributeString = cdmRefAttributeString;
	}

	@Override
	public void initialize(PreparedStatement stmt, IndexCounter index,BerlinModelExportState state, String tableName) {
		super.initialize(stmt, index, state, tableName);
		String inRefSql = "INSERT INTO RefDetail (RefDetailId, RefFk , " +
	 		" FullRefCache, FullNomRefCache, PreliminaryFlag , Details , " +
	 		" SecondarySources, " +
	 		" Created_When , Created_Who , Updated_When, Updated_Who, Notes ,IdInSource)"+
	 		" VALUES (?,?, ?,?,?,?, ?, ?,?,?,?,?,?)";
		Connection con = getState().getConfig().getDestination().getConnection();
		try {
			preparedStatement = con.prepareStatement(inRefSql);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected Object getValue(CdmBase cdmBase) {
		String value = (String)super.getValue(cdmBase);
		boolean isBoolean = false;
		Reference ref = (Reference)ImportHelper.getValue(cdmBase, this.cdmRefAttributeString, isBoolean, true);
		Object result = makeRefDetail(value, ref);
//		getState().getConfig().getCdmAppController().commitTransaction(tx);
		return result;
	}


	protected Integer makeRefDetail(String microRef, Reference ref){
		if (ref == null){
			if (microRef == null || microRef.trim().equals("")){
				return null;
			}else{
				//TODO microRef with no reference
				logger.warn("ref == null not yet implemented");
				return null;
			}
		}
		Integer refDetailId = getState().getNextRefDetailId();
		Integer refId = getState().getDbId(ref);
//		String fullRefCache = null;
//		String fullNomRefCache = null;
		Boolean preliminaryFlag = false;
//		String secondarySources = null;
		java.sql.Date created_When = new java.sql.Date(new Date().getTime());
//		java.sql.Date updated_When = null;
		String created_who = "autom.";
//		String update_who = null;
//		String notes = null;

		try {
			preparedStatement.setInt(1, refDetailId);
			preparedStatement.setInt(2, refId);
			preparedStatement.setNull(3, Types.VARCHAR) ;//.setString(3, fullRefCache);
			preparedStatement.setNull(4, Types.VARCHAR) ;//.setString(4, fullNomRefCache);
			preparedStatement.setBoolean(5, preliminaryFlag);
			if (microRef != null){
				preparedStatement.setString(6, microRef);
			}else{
				preparedStatement.setNull(6, Types.VARCHAR);
			}
			preparedStatement.setNull(7, Types.VARCHAR) ;//.setString(7, secondarySources);
			preparedStatement.setDate(8, created_When);
			preparedStatement.setString(9, created_who);
			preparedStatement.setNull(10, Types.DATE) ;//.setDate(10, updated_When);
			preparedStatement.setNull(11, Types.VARCHAR) ;//.setString(11, update_who);
			preparedStatement.setNull(12, Types.VARCHAR) ;//.setString(12, notes);
			preparedStatement.setNull(13, Types.VARCHAR) ;//.setString(13, secondarySources);


			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return refDetailId;
	}

	protected Integer getId(CdmBase cdmBase){
		BerlinModelExportConfigurator config = getState().getConfig();
		if (false && config.getIdType() == BerlinModelExportConfigurator.IdType.CDM_ID){
			return cdmBase.getId();
		}else{
			Integer id = getState().getDbId(cdmBase);
			return id;
		}
	}

	@Override
	protected int getSqlType() {
		return Types.INTEGER;
	}

	@Override
	public Class<?> getTypeClass() {
		return String.class;
	}

}
