/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 23.10.2019
 */
public class IpniSourcesImportConfigurator extends ExcelImportConfiguratorBase {

    private static final long serialVersionUID = 7883220110057878974L;

    public static IpniSourcesImportConfigurator NewInstance(URI uri, ICdmDataSource cdmDestination) {
        return new IpniSourcesImportConfigurator(uri, cdmDestination);
    }

    private IpniSourcesImportConfigurator(URI uri, ICdmDataSource cdmDestination) {
        super(uri, cdmDestination, null);
        this.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                IpniSourcesImport.class
            };
    }

    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }
//
//    /**
//     * If <code>true</code> the name authors will be added
//     * to the nomenclatural reference (Book or Article) though
//     * it might not be the exact same author.<BR>
//     * Default is <code>true</code>
//     */
//    public boolean isAddAuthorsToReference() {
//        return addAuthorsToReference;
//    }
//    /**
//     * @see #isAddAuthorsToReference()
//     */
//    public void setAddAuthorsToReference(boolean addAuthorsToReference) {
//        this.addAuthorsToReference = addAuthorsToReference;
//    }
}
