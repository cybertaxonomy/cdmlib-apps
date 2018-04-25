/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Simple Excel import class that works without default state class
 * {@link SimpleExcelSpecimenImportState}
 * @author a.mueller
 * @since 16.06.2016
 */
public abstract class SimpleExcelSpecimenImport<CONFIG extends ExcelImportConfiguratorBase>
        extends ExcelImportBase<SimpleExcelSpecimenImportState<CONFIG>, CONFIG, ExcelRowBase>{

    private static final long serialVersionUID = -4345647703312616421L;

    private static final Logger logger = Logger.getLogger(SimpleExcelSpecimenImport.class);

    protected static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();


    @Override
    protected void analyzeRecord(HashMap<String, String> record, SimpleExcelSpecimenImportState<CONFIG> state) {
        //override only if needed
    }

    @Override
    protected void secondPass(SimpleExcelSpecimenImportState<CONFIG> state) {
        //override only if needed
    }

    @Override
    protected boolean isIgnore(SimpleExcelSpecimenImportState<CONFIG> state) {
        return false;
    }

//***************************** METHODS *********************************/


    /**
     * @param state
     * @return
     */
    protected IdentifiableSource makeOriginalSource(SimpleExcelSpecimenImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, state.getConfig().getSourceReference());
    }


    /**
     * @param line
     * @param keys
     * @param expectedKeys
     */
    protected void checkAllKeysExist(String line, Set<String> keys, List<String> expectedKeys) {
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }
    }


}
