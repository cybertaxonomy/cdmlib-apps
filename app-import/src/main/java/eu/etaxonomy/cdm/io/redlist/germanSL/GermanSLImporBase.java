/**
* Copyright (C) 2018 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.germanSL;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.model.name.IBotanicalName;

/**
 * @author a.mueller
 * @since 15.05.2018
 *
 */
public abstract class GermanSLImporBase
        extends SimpleExcelTaxonImport<GermanSLImportConfigurator> {

    private static final long serialVersionUID = 236093186271666895L;

    private static final Logger logger = Logger.getLogger(GermanSLImporBase.class);


    protected class NameResult{
        IBotanicalName name;
        boolean proParte = false;
        String sensu = null;
        String auct = null;
    }

    protected boolean isAccepted(String statusStr, NameResult nameResult){
        if (nameResult.proParte){
            return true; //pro parte synonyms and misapplied names are always handled as concept relationships
            //and therefore need to be accepted
        }else if ("FALSE()".equals(statusStr) || "0".equals(statusStr) || "false".equalsIgnoreCase(statusStr)){
            return true;
        } else if ("TRUE()".equals(statusStr) || "1".equals(statusStr)|| "true".equalsIgnoreCase(statusStr)){
            return false;
        }else{
            logger.warn("Unhandled taxon status: " + statusStr);
            return false;
        }
    }
}
