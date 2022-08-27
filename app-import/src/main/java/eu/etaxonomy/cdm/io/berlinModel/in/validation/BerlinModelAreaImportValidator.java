/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;

/**
 * @author a.mueller
 * @since 17.02.2010
 */
public class BerlinModelAreaImportValidator implements IOValidator<BerlinModelImportState> {

    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean validate(BerlinModelImportState state) {
		boolean result = true;
//		BerlinModelImportConfigurator config = state.getConfig();
//		result &= checkSourcesWithWhitespace(config);
		return result;
	}


	//******************************** CHECK *************************************************


//    private static boolean checkSourcesWithWhitespace(BerlinModelImportConfigurator config){
//        try {
//            boolean result = true;
//            Source source = config.getSource();
//            String strSelect = "SELECT OccurrenceId, PTNameFk, PTRefFk, AreaFk, Sources, Created_When, Created_Who, Updated_When, Updated_Who, Notes, Occurrence ";
//            String strCount = " SELECT count(*) as n";
//            String strQueryBase =
//                    " FROM emOccurrence " +
//                    " WHERE (Sources LIKE '%  %') OR (Sources LIKE '% ') OR (Sources LIKE ' %') ";
//
//            ResultSet rs = source.getResultSet(strCount + strQueryBase);
//            rs.next();
//            int n = rs.getInt("n");
//            if (n > 0){
//                System.out.println("=======================================================================");
//                System.out.println("There are "+n+" occurrences with source attribute has unexpected whitespace!");
//                System.out.println("---------------------------------------------------------------");
//                System.out.println(strSelect + strQueryBase);
//                System.out.println("=======================================================================");
//            }
//
//            rs = source.getResultSet(strSelect + strQueryBase);
//            while (rs.next()){
//                int occurrenceId = rs.getInt("OccurrenceId");
//                String sources = rs.getString("Sources");
//
//                System.out.println("OccurrenceSourceId:" + occurrenceId +
//                        "\n  Sources: " + sources)
//                        ;
//            }
//            return result;
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}
