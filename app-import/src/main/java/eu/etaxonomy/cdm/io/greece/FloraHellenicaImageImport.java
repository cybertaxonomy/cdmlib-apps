/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;

/**
 * Import for the Flora Hellenica images.
 *
 * @author a.mueller
 * @date 03.04.2017
 */

@Component
public class FloraHellenicaImageImport<CONFIG extends FloraHellenicaImportConfigurator>
        extends CdmImportBase<CONFIG,SimpleExcelTaxonImportState<CONFIG>>{

    private static final long serialVersionUID = 7118028793298922703L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaImageImport.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInvoke(SimpleExcelTaxonImportState<CONFIG> state) {
//        String baseURI =
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doCheck(SimpleExcelTaxonImportState<CONFIG> state) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoImages();
    }



}
