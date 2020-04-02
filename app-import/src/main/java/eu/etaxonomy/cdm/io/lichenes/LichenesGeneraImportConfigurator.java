/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.lichenes;

import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.greece.FloraHellenicaTermImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * Configurator for lichenes genera import.
 *
 * @author a.mueller
 * @since 10.03.2020
 */
public class LichenesGeneraImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = 8656746111285366704L;

    private static IInputTransformer defaultTransformer = null;
    private Reference secReference;

    public static LichenesGeneraImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new LichenesGeneraImportConfigurator(source, destination);
    }

    private LichenesGeneraImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @Override
    public SimpleExcelTaxonImportState getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
	@Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                LichenesGeneraTaxonImport.class,
        };
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }
}
