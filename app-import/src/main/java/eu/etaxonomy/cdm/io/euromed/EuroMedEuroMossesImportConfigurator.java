/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.UUID;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 08.03.2023
 */
public class EuroMedEuroMossesImportConfigurator extends ExcelImportConfiguratorBase {

    private static final long serialVersionUID = -648787716062337242L;

    public static final UUID uuidEuroMossSerial = UUID.fromString("661e6ea4-8ba4-4c0d-aaa7-e30231239ce5");
    public static final UUID uuidEuroMedMossesAreas = UUID.fromString("ad177547-1f23-48db-8657-ca120668fca6");

    private boolean doGenera = false;
    private boolean isLiverwort = false;


    public static EuroMedEuroMossesImportConfigurator NewInstance(URI uri, ICdmDataSource cdmDestination) {
        return new EuroMedEuroMossesImportConfigurator(uri, cdmDestination);
    }

    private EuroMedEuroMossesImportConfigurator(URI uri, ICdmDataSource cdmDestination) {
        super(uri, cdmDestination, null);
        this.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        if (doGenera) {
            ioClassList = new Class[]{
                    EuroMedEuroMossesGenusImport.class,
            };
        }else {
            ioClassList = new Class[]{
                    EuroMedEuroMossesImport.class,
            };
        }
    }

    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    public boolean isDoGenera() {
        return doGenera;
    }
    public void setDoGenera(boolean doGenera) {
        this.doGenera = doGenera;
    }

    public boolean isLiverwort() {
        return isLiverwort;
    }
    public void setLiverwort(boolean isLiverwort) {
        this.isLiverwort = isLiverwort;
    }
}