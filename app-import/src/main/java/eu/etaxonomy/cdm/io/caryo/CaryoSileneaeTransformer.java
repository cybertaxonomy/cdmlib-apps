/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
public class CaryoSileneaeTransformer extends InputTransformerBase{

    private static final long serialVersionUID = 4635704148993246218L;

    private static final Logger logger = LogManager.getLogger();

     @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }
        try {
            Rank rank = Rank.getRankByIdInVoc(key, NomenclaturalCode.ICNAFP);
            return rank;
        } catch (UnknownCdmTypeException e) {
            if (key.equals("gen.")) {return Rank.GENUS();
            } else if (key.equals("spec.")) {return Rank.SPECIES();
            } else if (key.equals("unranked")) {return Rank.UNRANKED_INFRAGENERIC();
            } else if (key.equals("[infrasp.unranked]")) {return Rank.UNRANKED_INFRASPECIFIC();
            } else{
                logger.warn("Rank not defined: " + key);
                return null;
            }
        }

    }
}
