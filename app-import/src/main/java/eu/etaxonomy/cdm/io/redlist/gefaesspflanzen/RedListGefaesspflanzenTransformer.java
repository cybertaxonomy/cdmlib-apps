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
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
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
        else if (key.equals("VAR")){return Rank.VARIETY();}
        else if (key.equals("SSP")){return Rank.SUBSPECIES();}
        else if (key.equals("FOR")){return Rank.FORM();}
        else if (key.equals("RAC")){return Rank.RACE();}
        else if (key.equals("SEC")){return Rank.SECTION_BOTANY();}
        else if (key.equals("SSE")){return Rank.SUBSECTION_BOTANY();}
        else if (key.equals("SGE")){return Rank.SUBGENUS();}
        else if (key.equals("SVA")){return Rank.SUBVARIETY();}
        else if (key.equals("CV")){return Rank.CULTIVAR();}
        else if (key.equals("PRO")){return Rank.PROLES();}
        else if (key.equals("SER")){return Rank.SERIES();}
        else if (key.equals("GRE")){return Rank.GREX();}
        else if (key.equals("AGG")){return Rank.SPECIESAGGREGATE();}
        else if (key.equals("GRO")){return Rank.SPECIESGROUP();}
        else if (key.equals("ORA")){return null;}
        else if (key.equals("?")){return Rank.UNKNOWN_RANK();}
        return null;
    }



}
