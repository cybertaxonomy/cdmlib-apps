/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;

/**
 * @author a.mueller
 * @since 08.12.2017
 *
 */
public class GreeceGenusAuthorImportConfigurator
    extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 2036787279608394173L;

    public static GreeceGenusAuthorImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new GreeceGenusAuthorImportConfigurator(source, destination);
    }

    /**
     * @param uri
     * @param destination
     */
    protected GreeceGenusAuthorImportConfigurator(URI uri, ICdmDataSource destination) {
        super(uri, destination);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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
                GreeceGenusAuthorImport.class,
        };
    }

}
