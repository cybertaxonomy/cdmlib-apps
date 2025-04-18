/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.algaterra.validation;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.mueller
 * @since 17.02.2010
 */
public class AlgaTerraSpecimenImportValidator implements IOValidator<BerlinModelImportState> {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean validate(BerlinModelImportState state) {
		boolean result = true;
		BerlinModelImportConfigurator bmiConfig = state.getConfig();
		result &= checkOrphanedEcologyFacts(bmiConfig);
		//result &= checkPartOfJournal(bmiConfig);
		System.out.println("Checking for Specimen not yet fully implemented");
		return result;
	}

	//******************************** CHECK *************************************************

	private static boolean checkOrphanedEcologyFacts(BerlinModelImportConfigurator config){
		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT * FROM Fact " +
					" WHERE FactCategoryFk = 202 AND ExtensionFk NOT IN (SELECT EcoFactId FROM EcoFact) " +
					" ORDER BY ExtensionFk  ";

			ResultSet resulSet = source.getResultSet(strQuery);
			boolean firstRow = true;
			while (resulSet.next()){
				if (firstRow){
					System.out.println("========================================================");
					System.out.println("There EcologyFacts with pointing to non existing EcoFacts!");
					System.out.println("========================================================");
				}
				int factId = resulSet.getInt("FactId");
				int extensionFk = resulSet.getInt("ExtensionFk");

				System.out.println("FactId:" + factId + "\n  ExtensionFk: " + extensionFk);

				result = firstRow = false;
			}

			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

}
