/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
public class MexicoConabioImportConfigurator extends ExcelImportConfiguratorBase{
    private static final long serialVersionUID = -2795059530001736347L;

    private Reference secReference;

    private boolean doTaxa = true;
    private boolean doDistributions = true;
    private boolean doCommonNames = true;

    private static IInputTransformer defaultTransformer = new MexicoConabioTransformer();

    public static MexicoConabioImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new MexicoConabioImportConfigurator(source, destination);
    }

    private MexicoConabioImportConfigurator(URI source, ICdmDataSource destination) {
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
                MexicoConabioTaxonImport.class,
                MexicoConabioDistributionImport.class,
                MexicoConabioCommonNamesImport.class
        };
    }

    public boolean isDoTaxa() {
        return doTaxa;
    }
    public void setDoTaxa(boolean doTaxa) {
        this.doTaxa = doTaxa;
    }

    public boolean isDoDistributions() {
        return doDistributions;
    }
    public void setDoDistributions(boolean doDistributions) {
        this.doDistributions = doDistributions;
    }

    public boolean isDoCommonNames() {
        return doCommonNames;
    }
    public void setDoCommonNames(boolean doCommonNames) {
        this.doCommonNames = doCommonNames;
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }
}