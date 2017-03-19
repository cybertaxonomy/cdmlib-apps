/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @date 14.12.2016
 *
 */
public class FloraHellenicaImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 3782414424818991629L;
    private static IInputTransformer defaultTransformer = new FloraHellenicaTransformer();
    private Reference secReference;

    /**
     * @param source
     * @param cdmDestination
     * @return
     */
    public static FloraHellenicaImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new FloraHellenicaImportConfigurator(source, destination);
    }


    private FloraHellenicaImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                FloraHellenicaTaxonImport.class,
                FloraHellenicaExcludedTaxonImport.class,
                FloraHellenicaSynonymImport.class,
                FloraHellenicaCommentsImport.class,
        };
    }
}
