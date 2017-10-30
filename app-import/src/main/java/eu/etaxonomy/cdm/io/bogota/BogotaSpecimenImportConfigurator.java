// $Id$
/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;

/**
 * @author a.mueller
 * @date 21.04.2017
 *
 */
public class BogotaSpecimenImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 6688815926646112726L;

    private int minLineNumber = 0;
    private int maxLineNumber = 1000000;

    /**
     * @param source
     * @param cdmDestination
     * @return
     */
    public static BogotaSpecimenImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new BogotaSpecimenImportConfigurator(source, destination);
    }

    /**
     * @param uri
     * @param destination
     */
    private BogotaSpecimenImportConfigurator(URI uri, ICdmDataSource destination) {
        super(uri, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelSpecimenImportState<>(this);
    }

    /**
     * {@inheritDoc}
     */
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


}
