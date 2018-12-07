/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary;

import java.io.InputStreamReader;

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

    public static PlantGlossaryCsvImportConfigurator NewInstance(InputStreamReader file,
            ICdmDataSource cdmDestination) {
        return new PlantGlossaryCsvImportConfigurator(file, cdmDestination);
    }

// ****************** CONSTRUCTOR *****************************/

    private PlantGlossaryCsvImportConfigurator(InputStreamReader file,
            ICdmDataSource cdmDestination){
        super(file, cdmDestination, null);
    }

// *************************************


    @Override
    @SuppressWarnings("unchecked")
    protected void makeIoClassList(){
        ioClassList = new Class[]{
            PlantGlossaryCsvImport.class,
        };
    }

    @Override
    public PlantGlossaryCsvImportState getNewState() {
        return new PlantGlossaryCsvImportState(this);
    }

}
