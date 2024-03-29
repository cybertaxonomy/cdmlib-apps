/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.salvador;

import java.util.UUID;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;

/**
 * @author a.mueller
 * @since 08.07.2017
 *
 */
public class SalvadorImportTransformer extends InputTransformerBase {

    private static final long serialVersionUID = 2840448227887090910L;

    public static UUID uuidSalvadorFeatureVoc = UUID.fromString("a159c5e3-12b3-4c12-85e6-b16220621adc");

    public static UUID uuidSalvadorTextSpecimenOldFeature = UUID.fromString("d29de161-9b8e-4e09-90ce-621946053720");

}
