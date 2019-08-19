/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.fauEu2Cdm;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmImportConfigurator
        extends ImportConfiguratorBase<FauEu2CdmImportState, ICdmDataSource>{

    private static final long serialVersionUID = -5452466831212722546L;

    private static IInputTransformer myTransformer = null;

    private TaxonNodeFilter taxonNodeFilter = new TaxonNodeFilter();

    public static FauEu2CdmImportConfigurator NewInstance(ICdmDataSource source, ICdmDataSource destination) {
        return new FauEu2CdmImportConfigurator(source, destination);
    }

    public FauEu2CdmImportConfigurator(ICdmDataSource source, ICdmDataSource destination) {
        super(myTransformer);
        this.setSource(source);
        this.setDestination(destination);
    }

    @Override
    public FauEu2CdmImportState getNewState() {
        return new FauEu2CdmImportState(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                FauEu2CdmImport.class ,
        };
    }

    @Override
    @Deprecated
    public Reference getSourceReference() {
        return null;
    }


    public TaxonNodeFilter getTaxonNodeFilter() {
        return taxonNodeFilter;
    }
    public void setTaxonNodeFilter(TaxonNodeFilter taxonNodeFilter) {
        this.taxonNodeFilter = taxonNodeFilter;
    }


}
