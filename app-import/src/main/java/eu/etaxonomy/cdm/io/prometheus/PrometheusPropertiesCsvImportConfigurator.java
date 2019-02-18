/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.prometheus;

import java.io.IOException;
import java.net.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.csv.in.CsvImportConfiguratorBase;

/**
*
* @author pplitzner
* @date 18.02.2019
*
*/
public class PrometheusPropertiesCsvImportConfigurator
        extends CsvImportConfiguratorBase{

    private static final long serialVersionUID = 987286481306951779L;

    public static PrometheusPropertiesCsvImportConfigurator NewInstance(URI source,
            ICdmDataSource cdmDestination) throws IOException {
        return new PrometheusPropertiesCsvImportConfigurator(source, cdmDestination);
    }

// ****************** CONSTRUCTOR *****************************/

    private PrometheusPropertiesCsvImportConfigurator(URI source,
            ICdmDataSource cdmDestination) throws IOException{
        super(source, cdmDestination, null);
    }

// *************************************


    @Override
    @SuppressWarnings("unchecked")
    protected void makeIoClassList() {
        ioClassList = new Class[] {
                PrometheusPropertiesImport.class };
    }

    @Override
    public PrometheusPropertiesCsvImportState getNewState() {
        return new PrometheusPropertiesCsvImportState(this);
    }

}
