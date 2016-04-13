// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * @author pplitzner
 * @date Mar 7, 2016
 *
 */
public class RedListUtil {

    public static final UUID checkListClassificationUuid = UUID.fromString("928a4695-c055-465e-99da-07322384b0b7");

    public static final UUID checkListReferenceUuid = UUID.fromString("e3abe234-ff23-4d85-9c1f-6769df965f33");
    public static final UUID gesamtListeReferenceUuid = UUID.fromString("ec0308de-2245-4e71-a362-4ee72290bf23");

//    public static final UUID checkListClassificationUuid = UUID.fromString("87d3d656-57bc-4b54-b338-80f9f2a37435");
//    public static final UUID checkListClassificationUuid = UUID.fromString("1bd21015-f542-4bf9-9e1f-ac0147e3a9f8");
//    public static final UUID checkListClassificationUuid = UUID.fromString("c6120524-f21b-4426-92db-52add2286831");
//    public static final UUID checkListClassificationUuid = UUID.fromString("b48ca7a0-a641-4fd1-ad51-238a9b57a470");
//    public static final UUID checkListClassificationUuid = UUID.fromString("a8743caa-56d5-4636-9ce6-4ef272d769d2");
//    public static final UUID checkListClassificationUuid = UUID.fromString("870d2e18-cd1b-4aa1-a360-ba5e0a5905aa");
//    public static final UUID checkListClassificationUuid = UUID.fromString("e19bb2e6-d898-4793-8cd4-d866eeb1f872");
//    public static final UUID checkListClassificationUuid = UUID.fromString("53e81162-5c2d-425b-bbe6-6e8d12e85790");

    public static final String NAME_NAMESPACE = "name";
    public static final String TAXON_GESAMTLISTE_NAMESPACE = "taxon_gesamt_liste";
    public static final String TAXON_CHECKLISTE_NAMESPACE = "taxon_checkliste";
    public static final String AUTHOR_NAMESPACE = "author";


    //cell content
    public static final String HYBRID_CHAR = "";
    public static final String AUCT = "auct.";
    public static final String EX = " ex ";
    public static final String GUELT_BASIONYM = "b";
    public static final String GUELT_SYNONYM = "x";
    public static final String GUELT_ACCEPTED_TAXON = "1";

    //column names
    public static final String GUELT = "GUELT";
    public static final String LOWER = "LOWER";
    public static final String NAMNR = "NAMNR";
    public static final String AUTOR_BASI = "AUTOR_BASI";
    public static final String AUTOR_KOMB = "AUTOR_KOMB";
    public static final String ZUSATZ = "ZUSATZ";
    public static final String NOM_ZUSATZ = "NOM_ZUSATZ";
    public static final String TAX_ZUSATZ = "TAX_ZUSATZ";
    public static final String EPI3 = "EPI3";
    public static final String EPI2 = "EPI2";
    public static final String EPI1 = "EPI1";
    public static final String RANG = "RANG";
    public static final String TAXNAME = "TAXNAME";
    public static final String AUTOR = "AUTOR";
    public static final String CL_TAXON = "CL_TAXON";

    public static void logMessage(long id, String message, Logger logger){
        logger.error(NAMNR+": "+id+" "+message);
    }

}
