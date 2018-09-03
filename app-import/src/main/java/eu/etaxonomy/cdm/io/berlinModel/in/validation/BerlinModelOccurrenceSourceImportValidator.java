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

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * @author a.mueller
 * @since 17.02.2010
 */
public class BerlinModelOccurrenceSourceImportValidator implements IOValidator<BerlinModelImportState> {
	private static final Logger logger = Logger.getLogger(BerlinModelOccurrenceSourceImportValidator.class);

	@Override
    public boolean validate(BerlinModelImportState state) {
		boolean result = true;
		BerlinModelImportConfigurator config = state.getConfig();
        result &= checkSourcesWithWhitespace(config);
		return result;
	}


	//******************************** CHECK *************************************************

    private static boolean checkSourcesWithWhitespace(BerlinModelImportConfigurator config){
        try {
            boolean result = true;
            Source source = config.getSource();
            String strSelect = " SELECT OccurrenceSourceId, OccurrenceFk, Source, SourceNumber, OldName, OldNameFk, PreferredReferenceFlag ";
            String strCount = " count(*) ";
            String strQueryBase =
                    " FROM emOccurrenceSource " +
                    " WHERE SourceNumber LIKE '% %' ";

            ResultSet rs = source.getResultSet(strCount + strQueryBase);
            rs.next();
            int n = rs.getInt("n");
            if (n > 0){
                System.out.println("=======================================================================");
                System.out.println("There are "+n+" occurrence source numbers with whitespace!");
                System.out.println("---------------------------------------------------------------");
                System.out.println(strSelect + strQueryBase);
                System.out.println("=======================================================================");
            }

            rs = source.getResultSet(strSelect + strQueryBase);
            while (rs.next()){
                int occurrenceSourceId = rs.getInt("OccurrenceSourceId");
                String sourceNumber = rs.getString("SourceNumber");
                String Source = rs.getString("Source");

                System.out.println("OccurrenceSourceId:" + occurrenceSourceId +
                        "\n  Source Number: " + sourceNumber +
                        "\n  Source: " + Source)
                        ;
            }

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
