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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
public class BerlinModelOccurrenceImportValidator implements IOValidator<BerlinModelImportState> {

    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean validate(BerlinModelImportState state) {
		boolean result = true;
		BerlinModelImportConfigurator config = state.getConfig();
		result &= checkTaxonIsAccepted(config);
		result &= checkSourcesWithWhitespace(config);
		result &= checkMissingExplicitSources(config);
		return result;
	}

    //******************************** CHECK *************************************************

	private static boolean checkTaxonIsAccepted(BerlinModelImportConfigurator config){
		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT emOccurrence.OccurrenceId, PTaxon.StatusFk, Name.FullNameCache, "
			                + " Status.Status, PTaxon.PTRefFk, Reference.RefCache, emArea.EMCode, emOccurrence.* " +
						" FROM emOccurrence INNER JOIN " +
							" PTaxon ON emOccurrence.PTNameFk = PTaxon.PTNameFk AND emOccurrence.PTRefFk = PTaxon.PTRefFk INNER JOIN " +
			                " Name ON PTaxon.PTNameFk = Name.NameId INNER JOIN " +
			                " emArea ON emOccurrence.AreaFk = emArea.AreaId INNER JOIN " +
			                " Status ON PTaxon.StatusFk = Status.StatusId LEFT OUTER JOIN " +
			                " Reference ON PTaxon.PTRefFk = Reference.RefId " +
						" WHERE (PTaxon.StatusFk NOT IN ( 1, 5))  ";

			if (StringUtils.isNotBlank(config.getOccurrenceFilter())){
				strQuery += String.format(" AND (%s) ", config.getOccurrenceFilter()) ;
			}


			ResultSet resultSet = source.getResultSet(strQuery);
			boolean firstRow = true;
			while (resultSet.next()){
				if (firstRow){
					System.out.println("========================================================");
					System.out.println("There are Occurrences for a taxon that is not accepted!");
					System.out.println("--------------------------------------------------------");
					System.out.println(strQuery);
					System.out.println("========================================================");

				}
				int occurrenceId = resultSet.getInt("OccurrenceId");
//			    int statusFk = resulSet.getInt("StatusFk");
				String status = resultSet.getString("Status");
				String fullNameCache = resultSet.getString("FullNameCache");
				String ptRefFk = resultSet.getString("PTRefFk");
				String ptRef = resultSet.getString("RefCache");
				String area = resultSet.getString("EMCode");

				System.out.println("OccurrenceId:" + occurrenceId + "\n  Status: " + status +
						"\n  FullNameCache: " + fullNameCache +  "\n  ptRefFk: " + ptRefFk +
						"\n  sec: " + ptRef + "\n  area: " + area) ;

				result = firstRow = false;
			}

			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

    private static boolean checkSourcesWithWhitespace(BerlinModelImportConfigurator config){
        try {
            boolean result = true;
            Source source = config.getSource();
            String strSelect = "SELECT OccurrenceId, PTNameFk, PTRefFk, AreaFk, Sources, Created_When, Created_Who, Updated_When, Updated_Who, Notes, Occurrence ";
            String strCount = " SELECT count(*) as n";
            String strQueryBase =
                    " FROM emOccurrence " +
                    " WHERE (Sources LIKE '%  %') OR (Sources LIKE '% ') OR (Sources LIKE ' %') ";

            ResultSet rs = source.getResultSet(strCount + strQueryBase);
            rs.next();
            int n = rs.getInt("n");
            if (n > 0){
                System.out.println("=======================================================================");
                System.out.println("There are "+n+" occurrences with source attribute has unexpected whitespace!");
                System.out.println("---------------------------------------------------------------");
                System.out.println(strSelect + strQueryBase);
                System.out.println("=======================================================================");
            }

            rs = source.getResultSet(strSelect + strQueryBase);
            while (rs.next()){
                int occurrenceId = rs.getInt("OccurrenceId");
                String sources = rs.getString("Sources");

                System.out.println("OccurrenceSourceId:" + occurrenceId +
                        "\n  Sources: " + sources)
                        ;
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * @param config
     * @return
     */
    private boolean checkMissingExplicitSources(BerlinModelImportConfigurator config) {
        try {
             boolean result = true;
            Source source = config.getSource();
            String sql = " SELECT occ.OccurrenceId, occ.Sources, ocs.OccurrenceSourceId, ocs.SourceNumber, ar.EMCode, ar.Unit, n.FullNameCache, occ.PTRefFk  " +
                " FROM emOccurrence occ " +
                    " INNER JOIN emArea ar ON occ.AreaFk = ar.AreaId " +
                    " INNER JOIN PTaxon pt ON occ.PTNameFk = pt.PTNameFk AND occ.PTRefFk = pt.PTRefFk " +
                    " INNER JOIN Name n ON occ.PTNameFk = n.NameId  " +
                    " LEFT OUTER JOIN emOccurSumCat sumcat ON occ.SummaryStatus = sumcat.emOccurSumCatId " +
                    " LEFT OUTER JOIN emOccurrenceSource ocs ON occ.OccurrenceId = ocs.OccurrenceFk " +
                " WHERE ( occurrenceId IN ( SELECT occurrenceId FROM v_cdm_exp_occurrenceAll ))" +
                " ORDER BY occ.PTRefFk, n.fullNameCache, occ.occurrenceId";
            ResultSet rs = source.getResultSet(sql);
            int oldOccurrenceId = -1;
            Set<Integer> sources = new HashSet<>();
            int unmatched = 0;
            while (rs.next()){
                int occurrenceId = rs.getInt("OccurrenceId");
                if (occurrenceId != oldOccurrenceId){
                    checkExistingSources(sources, oldOccurrenceId, rs);
                    String sourcesStr = rs.getString("Sources");
                    sources = makeSources(sourcesStr);
                    oldOccurrenceId = occurrenceId;
                }
                String sourceIdStr = rs.getString("SourceNumber");
                Integer sourceId = StringUtils.isBlank(sourceIdStr) ? null: Integer.valueOf(sourceIdStr);
                unmatched = removeSource(sources, sourceId, occurrenceId, unmatched);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param sources
     * @param sourceId
     * @param unmatched
     */
    private int removeSource(Set<Integer> sources, Integer sourceId, int occurrenceId, int unmatched) {
        boolean contained = sources.remove(sourceId);
        if (sourceId != null && !contained){
//            System.out.println("OccurrenceId(" + occurrenceId + "): sourceId " + sourceId + " not found in sources field.");
            unmatched++;
        }
        return unmatched;
    }

    /**
     * @param sources
     * @param occurrenceId
     * @param rs
     * @throws SQLException
     */
    private void checkExistingSources(Set<Integer> sources, int occurrenceId, ResultSet rs) throws SQLException {
        sources.remove(27133);
        sources.remove(0);
        if (!sources.isEmpty()){
            String emCode = rs.getString("EMCode").trim();
            String unit = rs.getString("Unit");
            String name = rs.getString("FullNameCache");
            String ref = rs.getString("PTRefFk");
            System.out.println(name + " ("+ ref + " (occId: " + occurrenceId + ", " + emCode + ", " + unit + "): The following sources are not matched: " + sources);
        }
    }

    private Set<Integer> makeSources(String sourcesStr) {
        Set<Integer> result = new HashSet<>();
        if (sourcesStr != null){
            String[] splits = sourcesStr.split("\\|");
            for (String split : splits){
                split = split.trim();
                if (StringUtils.isNotBlank(split)){
                    Integer number;
                    try {
                        number = Integer.valueOf(split);
                        result.add(number);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return result;
    }
}
