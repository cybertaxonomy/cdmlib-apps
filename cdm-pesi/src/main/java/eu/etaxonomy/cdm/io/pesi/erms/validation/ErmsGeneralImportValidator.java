/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.erms.validation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsImportBase;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsImportConfigurator;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;


/**
 * For validating general consistencies like existence of tables, etc.
 * @author a.mueller
 * @since 10.06.2009
 */
@Component
public class ErmsGeneralImportValidator extends ErmsImportBase<CdmBase> implements IOValidator<ErmsImportState> {

    private static final long serialVersionUID = 7759961747172738096L;
    private static Logger logger = LogManager.getLogger();

	public ErmsGeneralImportValidator(){
		super(null, null, null);
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		return validate(state);
	}

	@Override
    public boolean validate(ErmsImportState state) {
		boolean result = true;
		ErmsImportConfigurator config = state.getConfig();
//		result &= checkRelAuthorsExist(config);
//		result &= checkRelReferenceExist(config);

		return result;
	}

	@Override
    protected void doInvoke(ErmsImportState state){
		//do nothing
		return;

	}

	private boolean checkRelAuthorsExist(ErmsImportConfigurator config){

		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT Count(*) as n " +
					" FROM RelAuthor "
					;
			ResultSet rs = source.getResultSet(strQuery);
			rs.next();
			int count = rs.getInt("n");
			if (count > 0){
				System.out.println("========================================================");
				logger.warn("There are "+count+" RelAuthors, but RelAuthors are not implemented for CDM yet.");
				System.out.println("========================================================");
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	private boolean checkRelReferenceExist(ErmsImportConfigurator config){

		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT Count(*) as n " +
					" FROM RelReference "
					;
			ResultSet rs = source.getResultSet(strQuery);
			rs.next();
			int count = rs.getInt("n");
			if (count > 0){
				System.out.println("========================================================");
				logger.warn("There are "+count+" RelReferences, but RelReferences are not implemented for CDM yet.");
				System.out.println("========================================================");
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	@Override
	protected String getTableName() {
		return null;  //not needed
	}

	@Override
	public String getPluralString() {
		return null; //not needed
	}

	@Override
    protected boolean isIgnore(ErmsImportState state){
		return false;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		return null;  // not needed
	}

	@Override
    public boolean doPartition(ResultSetPartitioner partitioner, ErmsImportState state) {
		return true;  // not needed
	}

	@Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		return null;  // not needed
	}

	public CdmBase createObject(ResultSet rs, ErmsImportState state) throws SQLException {
		return null;  //not needed
	}

	@Override
	protected DbImportMapping<?, ?> getMapping() {
		return null;
	}
}
