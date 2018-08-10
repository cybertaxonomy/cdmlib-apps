/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.phycobank;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 09.08.2018
 *
 */
public class PhycobankHigherClassificationImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = -1029519818822847118L;

    private static IInputTransformer defaultTransformer = new IAPTTransformer();

    private Reference secReference;

     public static PhycobankHigherClassificationImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new PhycobankHigherClassificationImportConfigurator(source, destination);
    }


    private PhycobankHigherClassificationImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @Override
    public ImportStateBase getNewState() {
        return new PhycobankHigherClassificationImportState(this);
    }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                PhycobankHigherClassificationExcelImport.class
        };
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

}

