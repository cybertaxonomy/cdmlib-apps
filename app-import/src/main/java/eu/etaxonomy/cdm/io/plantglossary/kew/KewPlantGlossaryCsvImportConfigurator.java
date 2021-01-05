/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.plantglossary.kew;

import java.io.IOException;
import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.csv.in.CsvImportConfiguratorBase;

/**
 *
 * @author pplitzner
 * @since Jan 25, 2019
 *
 */
public class KewPlantGlossaryCsvImportConfigurator
        extends CsvImportConfiguratorBase{

    private static final long serialVersionUID = 987286481306951779L;

    public static KewPlantGlossaryCsvImportConfigurator NewInstance(URI source,
            ICdmDataSource cdmDestination) throws IOException {
        return new KewPlantGlossaryCsvImportConfigurator(source, cdmDestination);
    }

// ****************** CONSTRUCTOR *****************************/

    private KewPlantGlossaryCsvImportConfigurator(URI source,
            ICdmDataSource cdmDestination) throws IOException{
        super(source, cdmDestination, null);
    }

// *************************************


    @Override
    @SuppressWarnings("unchecked")
    protected void makeIoClassList() {
        ioClassList = new Class[] {
                KewPlantGlossaryImport.class };
    }

    @Override
    public KewPlantGlossaryCsvImportState getNewState() {
        return new KewPlantGlossaryCsvImportState(this);
    }

}
