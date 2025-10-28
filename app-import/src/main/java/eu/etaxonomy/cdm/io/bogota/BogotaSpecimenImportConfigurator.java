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
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 21.04.2017
 */
public class BogotaSpecimenImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 6688815926646112726L;

    private int minLineNumber = 0;
    private int maxLineNumber = 1000000;

    private Reference secReference;
    private boolean onlyNonCdmTaxa;

    public static BogotaSpecimenImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new BogotaSpecimenImportConfigurator(source, destination);
    }

    private BogotaSpecimenImportConfigurator(URI uri, ICdmDataSource destination) {
        super(uri, destination);
    }

    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelSpecimenImportState<>(this);
    }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                BogotaSpecimenImport.class,
        };
    }

    public int getMinLineNumber() {
        return minLineNumber;
    }
    public void setMinLineNumber(int minLineNumber) {
        this.minLineNumber = minLineNumber;
    }

    public int getMaxLineNumber() {
        return maxLineNumber;
    }
    public void setMaxLineNumber(int maxLineNumber) {
        this.maxLineNumber = maxLineNumber;
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

    public boolean isOnlyNonCdmTaxa() {
        return onlyNonCdmTaxa;
    }
    public void setOnlyNonCdmTaxa(boolean onlyNonCdmTaxa) {
        this.onlyNonCdmTaxa = onlyNonCdmTaxa;
    }
}