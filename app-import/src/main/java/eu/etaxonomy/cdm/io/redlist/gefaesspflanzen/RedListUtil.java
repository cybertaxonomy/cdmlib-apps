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

import org.apache.log4j.Logger;

/**
 * @author pplitzner
 * @date Mar 7, 2016
 *
 */
public class RedListUtil {

    public static final String NAME_NAMESPACE = "name";
    public static final String TAXON_NAMESPACE = "taxon";
    public static final String AUTHOR_NAMESPACE = "author";

    public static final String AUCT = "auct.";
    public static final String EX = " ex ";
    public static final String GUELT_BASIONYM = "b";
    public static final String GUELT_SYNONYM = "x";
    public static final String GUELT_ACCEPTED_TAXON = "1";


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

    public static void logMessage(long id, String message, Logger logger){
        logger.error(NAMNR+": "+id+" "+message);
    }

}
