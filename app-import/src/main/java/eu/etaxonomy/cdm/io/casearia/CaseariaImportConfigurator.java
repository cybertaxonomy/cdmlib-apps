/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.casearia;

import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 12.05.2020
 */
public class CaseariaImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 3083719227468065516L;

    private boolean doDeduplicate = true;

    private static IInputTransformer defaultTransformer = new CaseariaTransformer();

    public static CaseariaImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new CaseariaImportConfigurator(source, destination);
    }

    private CaseariaImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
            CaseariaTaxonImport.class,
            CaseariaDistributionImport.class
        };
    }

    public boolean isDoDeduplicate() {
        return this.doDeduplicate;
    }
    public void setDoDeduplicate(boolean doDeduplicate) {
        this.doDeduplicate = doDeduplicate;
    }
}
