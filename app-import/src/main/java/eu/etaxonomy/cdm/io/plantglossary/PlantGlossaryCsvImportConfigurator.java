/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import java.io.IOException;
import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.csv.in.CsvImportConfiguratorBase;

/**
 *
 * @author pplitzner
 * @since Dec 7, 2018
 *
 */
public class PlantGlossaryCsvImportConfigurator
        extends CsvImportConfiguratorBase{

    private static final long serialVersionUID = 987286481306951779L;

    public static PlantGlossaryCsvImportConfigurator NewInstance(URI source,
            ICdmDataSource cdmDestination) throws IOException {
        return new PlantGlossaryCsvImportConfigurator(source, cdmDestination);
    }

// ****************** CONSTRUCTOR *****************************/

    private PlantGlossaryCsvImportConfigurator(URI source,
            ICdmDataSource cdmDestination) throws IOException{
        super(source, cdmDestination, null);
    }

// *************************************


    @Override
    @SuppressWarnings("unchecked")
    protected void makeIoClassList() {
        ioClassList = new Class[] {
                PlantGlossaryCategoryImport.class,
                PlantGlossaryCategoryAsPropertiesImport.class,
                PlantGlossaryStateImport.class };
    }

    @Override
    public PlantGlossaryCsvImportState getNewState() {
        return new PlantGlossaryCsvImportState(this);
    }

}
