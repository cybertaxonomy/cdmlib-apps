/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
public class SimpleExcelSpecimenImportState<CONFIG extends ExcelImportConfiguratorBase>
        extends ExcelImportState<CONFIG, ExcelRowBase>{

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SimpleExcelSpecimenImportState.class);


// ************************* CONSTRUCTUR *******************************/
    /**
     * @param config
     */
    public SimpleExcelSpecimenImportState(CONFIG config) {
        super(config);
    }

 //************************ PUTTER / GETTER *****************************/


}