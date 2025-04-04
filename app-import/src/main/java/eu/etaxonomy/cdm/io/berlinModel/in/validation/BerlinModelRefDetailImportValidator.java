/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in.validation;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.mueller
 * @since 17.02.2010
 */
public class BerlinModelRefDetailImportValidator implements IOValidator<BerlinModelImportState> {

    private static final Logger logger = LogManager.getLogger();

	@Override
    public boolean validate(BerlinModelImportState state) {
		boolean result = true;
		result &= checkRefDetailsWithSecondarySource(state);
		result &= checkRefDetailsWithIdInSource(state);
		result &= checkRefDetailsWithNotes(state);

		return result;
	}

	private boolean checkRefDetailsWithSecondarySource(BerlinModelImportState state) {
		boolean success = true;
		try {

			Source source = state.getConfig().getSource();
			String strQuery =
				"SELECT count(*) AS n FROM RefDetail " +
				" WHERE (SecondarySources IS NOT NULL) AND (RTRIM(LTRIM(SecondarySources)) <> '')";
			ResultSet rs = source.getResultSet(strQuery);
			rs.next();
			int n;
			n = rs.getInt("n");
			if (n > 0){
				System.out.println("========================================================");
				System.out.println("There are " + n + " RefDetails with a secondary source. Secondary sources are not supported yet");
				System.out.println("========================================================");
				success = false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return success;
	}

	private boolean checkRefDetailsWithIdInSource(BerlinModelImportState state) {
		boolean success = true;
		try {
			Source source = state.getConfig().getSource();
			if (source.checkColumnExists("RefDetail", "idInSource")){
				String strQuery =
					"SELECT count(*) AS n FROM RefDetail " +
					" WHERE (IdInSource IS NOT NULL) AND (RTRIM(LTRIM(IdInSource)) <> '')";
				ResultSet rs = source.getResultSet(strQuery);
				rs.next();
				int n;
				n = rs.getInt("n");
				if (n > 0){
					System.out.println("========================================================");
					System.out.println("There are " + n + " RefDetails with an idInSource. IdInSources are not supported yet");
					System.out.println("========================================================");
					success = false;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DatabaseTypeNotSupportedException e) {
			logger.debug("Source does not support checking existance of 'idInSource' column");
		}
		return success;
	}

	private boolean checkRefDetailsWithNotes(BerlinModelImportState state) {
		boolean success = true;
		try {

			Source source = state.getConfig().getSource();
			String strQuery =
				"SELECT count(*) AS n FROM RefDetail " +
				" WHERE (Notes IS NOT NULL) AND (RTRIM(LTRIM(Notes)) <> '')";
			ResultSet rs = source.getResultSet(strQuery);
			rs.next();
			int n;
			n = rs.getInt("n");
			if (n > 0){
				System.out.println("========================================================");
				System.out.println("There are " + n + " RefDetails with a note. Notes for RefDetails are not imported!");
				System.out.println("========================================================");
				success = false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return success;
	}
}