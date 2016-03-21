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

    public static void logMessage(long id, String message, Logger logger){
        logger.error("NAMNR: "+id+" "+message);
    }
}
