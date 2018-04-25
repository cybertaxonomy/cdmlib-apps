/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.germanSL;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @since 25.11.2016
 *
 */
public class GermanSLTransformer  extends InputTransformerBase{

    private static final long serialVersionUID = -1794363151658943665L;

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GermanSLTransformer.class);

    private static final UUID uuidRankZUS = UUID.fromString("db161a9e-aebc-419f-a724-14feae1b08f7");
    private static final UUID uuidIdentifierTypeLettercode = UUID.fromString("99b907df-c932-4007-96e9-b6a0d5e1f3bf");


    @Override
    public UUID getRankUuid(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("ZUS")) {return uuidRankZUS;
        }else{
            return null;
        }
    }


    @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("ABT")) {return Rank.DIVISION();
        }else if (key.equals("AG2")){return Rank.INFRAGENERICTAXON(); //Aggregate oberhalb der Gattung (Auswertungsgruppe)
        }else if (key.equals("AGG")){return Rank.SPECIESAGGREGATE();
        }else if (key.equals("FAM")){return Rank.FAMILY();
        }else if (key.equals("FOR")){return Rank.FORM();
        }else if (key.equals("GAT")){return Rank.GENUS();
        }else if (key.equals("KLA")){return Rank.CLASS();
        }else if (key.equals("ORD")){return Rank.ORDER();
//        }else if (key.equals("ROOT")){return Rank.ROOT();
        }else if (key.equals("SEC")){return Rank.SECTION_BOTANY();
        }else if (key.equals("SER")){return Rank.SERIES();

        }else if (key.equals("SGE")){return Rank.SUBGENUS();
//        }else if (key.equals("SGR")){return Rank.();
        }else if (key.equals("SPE")){return Rank.SPECIES();
        }else if (key.equals("SSE")){return Rank.SUBSERIES();
        }else if (key.equals("SSP")){return Rank.SUBSPECIES();
        }else if (key.equals("UAB")){return Rank.SUBDIVISION();
        }else if (key.equals("UKL")){return Rank.SUBCLASS();
        }else if (key.equals("VAR")){return Rank.VARIETY();
//        }else if (key.equals("ZUS")){return Rank.();
        }else{
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getIdentifierTypeUuid(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("LETTERCODE")) {return uuidIdentifierTypeLettercode;
        }else{
            return null;
        }
    }



}
