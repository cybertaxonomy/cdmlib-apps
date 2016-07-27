// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.iapt;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;

/**
 * @author a.mueller
 * @created 01.03.2010
 */
public final class IAPTTransformer extends InputTransformerBase {
    private static final long serialVersionUID = 1070018208741186271L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(IAPTTransformer.class);

    //references
    public static final UUID uuidRefFRC = UUID.fromString("c1caf6a2-5083-4f44-8f97-9abe23a84cd8");
    public static final UUID uuidRefAS = UUID.fromString("1f15291a-b4c5-4e15-960f-d0145a250539");
    public static final UUID uuidRefFC = UUID.fromString("c5a0bfb8-85b2-422d-babe-423aa2e24c35");



}
