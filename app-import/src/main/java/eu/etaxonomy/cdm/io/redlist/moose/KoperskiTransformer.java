/**
* Copyright (C) 2023 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @since 03.08.2023
 */
public class KoperskiTransformer extends InputTransformerBase{

    private static final long serialVersionUID = 4635704148993246218L;

    private static final Logger logger = LogManager.getLogger();

     @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("Genus")) {return Rank.GENUS();
        }else if (key.equals("Species")) {return Rank.SPECIES();
        }else if (key.equals("Variety")) {return Rank.VARIETY();
        }else if (key.equals("Subspecies")) {return Rank.SUBSPECIES();
        }else if (key.equals("Subvariety")){return Rank.SUBVARIETY();
        }else if (key.equals("Form")) {return Rank.FORM();
        }else{
            logger.warn("Rank not defined: " + key);
            return null;
        }
    }
}
