/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.iapt;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.MexicoBorhidiExcelImport;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

import java.net.URI;

/**
 * @author a.mueller
 * @since 16.06.2016
 *
 */
public class IAPTImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = -4793138681632122831L;

    private static IInputTransformer defaultTransformer = new IAPTTransformer();

    private Reference secReference;

    boolean doAlgeaeOnly;


    public static IAPTImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new IAPTImportConfigurator(source, destination);
    }


    private IAPTImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @Override
    public ImportStateBase getNewState() {
        return new IAPTImportState(this);
    }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                IAPTExcelImport.class
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

    public void setDoAlgeaeOnly(boolean doAlgeaeOnly) {
        this.doAlgeaeOnly = doAlgeaeOnly;
    }

    public boolean isDoAlgeaeOnly() {
        return doAlgeaeOnly;
    }
}

