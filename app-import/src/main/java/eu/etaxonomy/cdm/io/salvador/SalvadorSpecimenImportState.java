/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.salvador;

import eu.etaxonomy.cdm.io.csv.in.CsvImportState;

/**
 * @author a.mueller
 * @date 08.07.2017
 *
 */
public class SalvadorSpecimenImportState
        extends CsvImportState<SalvadorSpecimenImportConfigurator>{

    /**
     * @param config
     */
    protected SalvadorSpecimenImportState(SalvadorSpecimenImportConfigurator config) {
        super(config);
    }

}
