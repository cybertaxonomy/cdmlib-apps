/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.erms.validation;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.pesi.PesiDestinations;
import eu.etaxonomy.cdm.app.pesi.PesiSources;
import eu.etaxonomy.cdm.io.common.Source;

/**
 * Tests the ERMS -> PESI pipeline by comparing the source DB with destination PESI DB.
 *
 * @author a.mueller
 * @since 01.09.2019
 */
public class PesiErmsValidator {

    private static final Logger logger = Logger.getLogger(PesiErmsValidator.class);

    private static final Source defaultSource = PesiSources.PESI2019_ERMS();
    private static final Source defaultDestination = PesiDestinations.pesi_test_local_CDM_ERMS2PESI();

    private Source source = defaultSource;
    private Source destination = defaultDestination;

    public void invoke(Source source, Source destination){
        boolean success = true;
        this.source = source;
        this.destination = destination;
        success &= testReferences();
        success &= testTaxa();
        success &= testTaxonRelations();
        //TBC
        System.out.println("end validation " + (success? "":"NOT ") + "successful.");
    }

    private boolean testTaxonRelations() {
        boolean success = true;
        return success;
    }

    private boolean testTaxa() {
        boolean success = testTaxaCount();
        return success;
    }

    private boolean testTaxaCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM tu ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon ");
        return equals("Taxon count ", countSrc, countDest);
    }

    private boolean testReferences() {
        boolean success = testReferenceCount();
        if (success){
            try {
                success &= testSingleReferences();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    private boolean testSingleReferences() throws SQLException {
        boolean success = true;
        ResultSet srcRS = source.getResultSet("SELECT s.* FROM sources s ORDER BY s.id ");
        ResultSet destRS = destination.getResultSet("SELECT s.* FROM Source s "
                + " WHERE s.OriginalDB = 'erms' ORDER BY s.RefIdInSource");  // +1 for the source reference "erms" but this has no OriginalDB
        if (srcRS.next() && destRS.next()){
            success &= testSingleReference(srcRS, destRS);
        }
        return success;
    }

    private boolean testSingleReference(ResultSet srcRS, ResultSet destRS) throws SQLException {
        //id, RefIdInSource
        boolean success = equals("Reference ID ", srcRS.getInt("id"), destRS.getInt("RefIdInSource"));

        //TODO TBC
        return success;
    }

    private boolean testReferenceCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM sources ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s.OriginalDB = 'erms'");  // +1 for the source reference "erms" but this has no OriginalDB
        boolean success = equals("Reference count ", countSrc, countDest);
        return success;
    }

    private boolean equals(String messageStart, int nSrc, int nDest) {
        if (nSrc != nDest){
            String message = messageStart + " must be equal, but was not. Source: "+  nSrc + "; Destination: " + nDest;
            logger.warn(message);
            return false;
        }else{
            logger.info(messageStart + " were equal: " + nSrc);
            return true;
        }
    }

//** ************* MAIN ********************************************/

    public static void main(String[] args){
        PesiErmsValidator validator = new PesiErmsValidator();
        validator.invoke(defaultSource, defaultDestination);
        System.exit(0);
    }
}
