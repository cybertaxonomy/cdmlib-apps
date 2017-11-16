/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.salvador;

import java.io.IOException;
import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.csv.in.CsvImportConfiguratorBase;
import eu.etaxonomy.cdm.io.csv.in.CsvImportState;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
public class SalvadorSpecimenImportConfigurator extends CsvImportConfiguratorBase{
    private static final long serialVersionUID = -4793138681632122831L;

    private static IInputTransformer defaultTransformer = new SalvadorImportTransformer();

    /**
     * @param source
     * @param cdmDestination
     * @return
     * @throws IOException
     */
    public static SalvadorSpecimenImportConfigurator NewInstance(URI source, ICdmDataSource destination) throws IOException {
        return new SalvadorSpecimenImportConfigurator(source, destination);
    }


    private SalvadorSpecimenImportConfigurator(URI source, ICdmDataSource destination) throws IOException {
        super(source, destination, defaultTransformer);
     }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                SalvadorSpecimenImport.class
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CsvImportState getNewState() {
        return new SalvadorSpecimenImportState(this);
    }

}

