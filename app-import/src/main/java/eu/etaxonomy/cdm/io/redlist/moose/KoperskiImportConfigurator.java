/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 03.08.2023
 */
public class KoperskiImportConfigurator extends ExcelImportConfiguratorBase{

	private static final long serialVersionUID = 1569387505909340499L;

	private boolean doDeduplicate = true;

    private static IInputTransformer defaultTransformer = new KoperskiTransformer();

    public static KoperskiImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new KoperskiImportConfigurator(source, destination);
    }

    private KoperskiImportConfigurator(URI source, ICdmDataSource destination) {
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
            KoperskiImport.class,
        };
    }

    public boolean isDoDeduplicate() {
        return this.doDeduplicate;
    }
    public void setDoDeduplicate(boolean doDeduplicate) {
        this.doDeduplicate = doDeduplicate;
    }
}