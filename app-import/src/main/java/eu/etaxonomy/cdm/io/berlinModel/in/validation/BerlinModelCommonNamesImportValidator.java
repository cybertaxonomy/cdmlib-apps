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
public class BerlinModelCommonNamesImportValidator implements IOValidator<BerlinModelImportState> {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	@Override
	public boolean validate(BerlinModelImportState state) {
		boolean result = true;
		result &= checkNameUsedInSourceMinusOne(state.getConfig());
        result &= checkUnreferredNameUsedInSource(state.getConfig());
		result &= checkUnreferredLanguageRefFk(state.getConfig());

		result &= checkTaxonIsAccepted(state.getConfig());

		return result;
	}

    private boolean checkNameUsedInSourceMinusOne(BerlinModelImportConfigurator config){
        try {
            boolean result = true;
            Source source = config.getSource();
            String strQuery = "SELECT Count(*) as n " +
                    " FROM emCommonName " +
                    " WHERE (emCommonName.NameInSourceFk = - 1) ";

            if (StringUtils.isNotBlank(config.getCommonNameFilter())){
                strQuery += String.format(" AND (%s) ", config.getCommonNameFilter()) ;
            }

            ResultSet rs = source.getResultSet(strQuery);
            rs.next();
            int count = rs.getInt("n");
            if (count > 0){
                System.out.println("========================================================");
                System.out.println("There are " + count + " common names that have a nameInSourceFk = -1.");
                System.out.println("========================================================");
            }
            String sql =
                " SELECT DISTINCT emCommonName.CommonNameId, emCommonName.NameInSourceFk, emCommonName.CommonName, PTaxon.PTNameFk, PTaxon.PTRefFk," +
                    " Name.FullNameCache, PTaxon.RIdentifier " +
                " FROM emCommonName INNER JOIN " +
                    " PTaxon ON emCommonName.PTNameFk = PTaxon.PTNameFk AND emCommonName.PTRefFk = PTaxon.PTRefFk INNER JOIN " +
                    " Name ON PTaxon.PTNameFk = Name.NameId " +
                " WHERE (emCommonName.NameInSourceFk = - 1) ";
            if (StringUtils.isNotBlank(config.getCommonNameFilter())){
                sql += String.format(" AND (%s) ", config.getCommonNameFilter()) ;
            }

            rs = source.getResultSet(sql);
            int i = 0;
            while (rs.next()){
                i++;
                int commonNameId = rs.getInt("CommonNameId");
                String fullNameCache = rs.getString("FullNameCache");
                String commonName = rs.getString("CommonName");
                int rIdentifier = rs.getInt("RIdentifier");
                int nameFk = rs.getInt("PTNameFk");
                int refFk = rs.getInt("PTRefFk");
                int nameInSourceFk = rs.getInt("NameInSourceFk");

                System.out.println("CommonName: " + commonName + "\n  CommonNameId: " + commonNameId + "\n Taxon Name:" + fullNameCache + "\n  TaxonNameFk: " + nameFk +
                        "\n  TaxonRefFk: " + refFk + "\n  TaxonId" + rIdentifier + "\n NameInSourceFk: " + nameInSourceFk + "\n");
            }
            if (i > 0){
                System.out.println(" ");
            }


            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

	private boolean checkUnreferredNameUsedInSource(BerlinModelImportConfigurator config){
		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT Count(*) as n " +
					" FROM emCommonName " +
					" WHERE (emCommonName.NameInSourceFk NOT IN " +
							"(SELECT NameId FROM Name AS Name_1)) AND " +
						"(emCommonName.NameInSourceFk <> - 1) ";

			if (StringUtils.isNotBlank(config.getCommonNameFilter())){
				strQuery += String.format(" AND (%s) ", config.getCommonNameFilter()) ;
			}

			ResultSet rs = source.getResultSet(strQuery);
			rs.next();
			int count = rs.getInt("n");
			if (count > 0){
				System.out.println("========================================================");
				System.out.println("There are " + count + " common names that have a name used in source which can not be found in the database.");

				System.out.println("========================================================");
			}
			String sql =
				" SELECT DISTINCT emCommonName.CommonNameId, emCommonName.NameInSourceFk, emCommonName.CommonName, PTaxon.PTNameFk, PTaxon.PTRefFk," +
					" Name.FullNameCache, PTaxon.RIdentifier " +
				" FROM emCommonName INNER JOIN " +
					" PTaxon ON emCommonName.PTNameFk = PTaxon.PTNameFk AND emCommonName.PTRefFk = PTaxon.PTRefFk INNER JOIN " +
					" Name ON PTaxon.PTNameFk = Name.NameId " +
				" WHERE (emCommonName.NameInSourceFk NOT IN " +
						"(SELECT NameId FROM Name AS Name_1)) AND " +
					"(emCommonName.NameInSourceFk <> - 1)";

			rs = source.getResultSet(sql);
			int i = 0;
			while (rs.next()){
				i++;
				int commonNameId = rs.getInt("CommonNameId");
				String fullNameCache = rs.getString("FullNameCache");
				String commonName = rs.getString("CommonName");
				int rIdentifier = rs.getInt("RIdentifier");
				int nameFk = rs.getInt("PTNameFk");
				int refFk = rs.getInt("PTRefFk");
				int nameInSourceFk = rs.getInt("NameInSourceFk");

				System.out.println("CommonName: " + commonName + "\n  CommonNameId: " + commonNameId + "\n Taxon Name:" + fullNameCache + "\n  TaxonNameFk: " + nameFk +
						"\n  TaxonRefFk: " + refFk + "\n  TaxonId" + rIdentifier + "\n NameInSourceFk: " + nameInSourceFk + "\n");
			}
			if (i > 0){
				System.out.println(" ");
			}


			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	private boolean checkUnreferredLanguageRefFk(BerlinModelImportConfigurator config){
		try {
			boolean result = true;
			Source source = config.getSource();
			String strQueryArticlesWithoutJournal = "SELECT Count(*) as n " +
					" FROM emCommonName cn INNER JOIN PTaxon pt ON pt.PTNameFk = cn.PTNameFk AND pt.PTRefFk = cn.PTRefFk " +
					" WHERE (cn.LanguageRefFk NOT IN " +
							"(SELECT ReferenceId FROM emLanguageReference)) AND " +
						"(cn.LanguageRefFk is NOT NULL) AND "
						+ " cn.LanguageRefFk <> cn.RefFk "
						+ " AND pt.statusFk NOT IN (6) ";
			ResultSet rs = source.getResultSet(strQueryArticlesWithoutJournal);
			rs.next();
			int count = rs.getInt("n");
			if (count > 0){
				System.out.println("============================================================================");
				System.out.println("There are " + count + " common names that have a languageRefFk which can not be found in the emLanguageReference table AND are not equal to RefFk.");
				System.out.println("============================================================================");
			}
			if (count > 0){
				System.out.println(" ");
			}


			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	private static boolean checkTaxonIsAccepted(BerlinModelImportConfigurator config){
		try {
			boolean result = true;
			Source source = config.getSource();
			String strQuery = "SELECT cn.CommonNameId, cn.CommonName, pt.StatusFk, n.FullNameCache, s.Status, pt.PTRefFk, pt.PTNameFk, r.RefCache " +
						" FROM emCommonName cn " +
							" INNER JOIN PTaxon pt ON cn.PTNameFk = pt.PTNameFk AND cn.PTRefFk = pt.PTRefFk " +
			                " INNER JOIN Name n ON pt.PTNameFk = n.NameId " +
			                " INNER JOIN Status s ON pt.StatusFk = s.StatusId " +
			                " LEFT OUTER JOIN Reference r ON pt.PTRefFk = r.RefId " +
						" WHERE (pt.StatusFk NOT IN ( 1, 5))  ";

			if (StringUtils.isNotBlank(config.getOccurrenceFilter())){
				strQuery += String.format(" AND (%s) ", config.getCommonNameFilter()) ;
			}


			ResultSet resulSet = source.getResultSet(strQuery);
			boolean firstRow = true;
			while (resulSet.next()){
				if (firstRow){
					System.out.println("========================================================");
					System.out.println("There are Common Names for a taxon that is not accepted!");
					System.out.println("========================================================");
				}
				int commonNameId = resulSet.getInt("CommonNameId");
				String commonName = resulSet.getString("CommonName");
				String status = resulSet.getString("Status");
				String fullNameCache = resulSet.getString("FullNameCache");
				String ptRefFk = resulSet.getString("PTRefFk");
				String ptNameFk = resulSet.getString("PTNameFk");
                String ptRef = resulSet.getString("RefCache");

				System.out.println("CommonNameId: " + commonNameId + "\n CommonName: " + commonName +
						"\n  Status: " + status +
						"\n  FullNameCache: " + fullNameCache +
						"\n  ptRefFk: " + ptRefFk +
						"\n  ptNameFk: " + ptNameFk +
                        "\n  sec: " + ptRef );

				result = firstRow = false;
			}

			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


}
