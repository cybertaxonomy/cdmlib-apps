// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @created 01.03.2010
 */
@SuppressWarnings("serial")
public final class RedListGefaesspflanzenTransformer extends InputTransformerBase {

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenTransformer.class);

    @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (key == null){return null;}
        if (key.equals("GAT")){return Rank.GENUS();}
        else if (key.equals("SPE")){return Rank.SPECIES();}
        return null;
    }



}
