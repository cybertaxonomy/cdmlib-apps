/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.faueu;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.Language;

/**
 * @author a.mueller
 * @since 01.07.2025
 */
public final class FaunaEuropaeaCommonNameTransformer extends InputTransformerBase {

    private static final long serialVersionUID = 8114104457988475108L;
    private static Logger logger = LogManager.getLogger();

	@Override
	public Language getLanguageByKey(String key) throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){
			return null;
		}else if (key.equalsIgnoreCase("Dutch")){return Language.DUTCH_FLEMISH();
		}else if (key.equalsIgnoreCase("Greek")){return Language.GREEK_MODERN();
		}else if (key.equalsIgnoreCase("Norwegian Nynorsk")){return Language.NORWEGIAN_NYNORSK();
		//TODO
		}else if (key.equalsIgnoreCase("English-United States")){return Language.ENGLISH();
        }else if (key.equalsIgnoreCase("Norwegian Bokmål")){return Language.NORWEGIAN_BOKMOL();
        }else if (key.equalsIgnoreCase("Israel (Hebrew)")){return Language.HEBREW();
        }else if (key.equalsIgnoreCase("Bokmål  (Norwegian)")){return Language.NORWEGIAN_BOKMOL();
        }else if (key.equalsIgnoreCase("Spanish, Castillian")){return Language.SPANISH_CASTILIAN();
        }else if (key.equalsIgnoreCase("Nynorsk (Norwegian)")){return Language.NORWEGIAN_NYNORSK();
        }else if (key.equalsIgnoreCase("Gaelic")){return Language.GAELIC_SCOTTISH_GAELIC();
        }else if (key.equalsIgnoreCase("Scottish")){return Language.SCOTS();
        }else if (key.equalsIgnoreCase("Catalan")){return Language.CATALAN_VALENCIAN();
        }else if (key.equalsIgnoreCase("Azerbaijan")){return Language.AZERBAIJANI();



		} else {
			logger.warn("Language not yet mapped: " +  key);
			return null;
		}
	}
}
