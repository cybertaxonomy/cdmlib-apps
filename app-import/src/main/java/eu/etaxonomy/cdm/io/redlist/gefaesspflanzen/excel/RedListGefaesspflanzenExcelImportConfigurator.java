// $Id$
/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen.excel;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * Import for German red list checklist for plantae.
 *
 * @author a.mueller
 * @since 13.06.2019
 */
public class RedListGefaesspflanzenExcelImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 1747735027309419198L;
    private Reference secReference;

    /**
     * @param source
     * @param cdmDestination
     * @return
     */
    public static RedListGefaesspflanzenExcelImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new RedListGefaesspflanzenExcelImportConfigurator(source, destination);
    }

    /**
     * @param uri
     * @param destination
     */
    private RedListGefaesspflanzenExcelImportConfigurator(URI uri, ICdmDataSource destination) {
        super(uri, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                RedListGefaesspflanzenTaxonExcelImport.class,
        };
    }

    /**
     * @return the secReference
     */
    public Reference getSecReference() {
        return secReference;
    }


    /**
     * @param secReference
     */
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

}
