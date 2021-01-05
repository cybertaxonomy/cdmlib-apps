/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 21.04.2017
 */
public class BogotaChecklistImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 5988430626932820343L;

    private Reference secReference;

    /**
     * @param source
     * @param cdmDestination
     * @return
     */
    public static BogotaChecklistImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new BogotaChecklistImportConfigurator(source, destination);
    }

    private BogotaChecklistImportConfigurator(URI uri, ICdmDataSource destination) {
        super(uri, destination);
    }

    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                BogotaChecklistTaxonImport.class,
        };
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

}
