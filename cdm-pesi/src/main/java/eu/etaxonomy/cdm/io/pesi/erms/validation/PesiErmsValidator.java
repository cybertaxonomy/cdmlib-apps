/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.erms.validation;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.pesi.PesiDestinations;
import eu.etaxonomy.cdm.app.pesi.PesiSources;
import eu.etaxonomy.cdm.io.common.Source;

/**
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
        this.source = source;
        this.destination = destination;
        testReferences();
        testTaxa();
        testTaxonRelations();
        //TBC
        System.out.println("end validation");
    }

    /**
     *
     */
    private void testTaxonRelations() {
        // TODO Auto-generated method stub
    }

    private void testTaxa() {
        testTaxaCount();
    }

    /**
     *
     */
    private void testTaxaCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM tu ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Taxon ");
        equals("Taxon count ", countSrc, countDest);
    }

    private void testReferences() {
        testReferenceCount();
        testSingleReferences();
        //TOD TBC
    }

    /**
     *
     */
    private void testSingleReferences() {
        srcRS = source.getResultSet("SELECT count(*) FROM sources ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s.OriginalDB = 'erms'");  // +1 for the source reference "erms" but this has no OriginalDB


    }

    private void testReferenceCount() {
        int countSrc = source.getUniqueInteger("SELECT count(*) FROM sources ");
        int countDest = destination.getUniqueInteger("SELECT count(*) FROM Source s WHERE s.OriginalDB = 'erms'");  // +1 for the source reference "erms" but this has no OriginalDB
        equals("Reference count ", countSrc, countDest);
    }

    private void equals(String messageStart, int nSrc, int nDest) {
        if (nSrc != nDest){
            String message = messageStart + " must be equal, but was not. Source: "+  nSrc + "; Destination: " + nDest;
            logger.warn(message);
        }else{
            logger.info(messageStart + " were equal: " + nSrc);
        }
    }

//** ************* MAIN ********************************************/

    public static void main(String[] args){
        PesiErmsValidator validator = new PesiErmsValidator();
        validator.invoke(defaultSource, defaultDestination);
        System.exit(0);
    }
}
