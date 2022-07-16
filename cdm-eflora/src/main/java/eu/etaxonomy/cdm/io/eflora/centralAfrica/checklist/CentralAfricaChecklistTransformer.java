/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.centralAfrica.checklist;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.MarkerType;

/**
 * @author a.mueller
 * @since 01.03.2010
 */
public final class CentralAfricaChecklistTransformer extends InputTransformerBase {

    private static final long serialVersionUID = -8848915422208053082L;
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static final UUID uuidAcceptedKew = UUID.fromString("c980102c-2e57-4ed3-b608-51a5d9091d89");
	public static final UUID uuidAcceptedGeneva = UUID.fromString("8c7a0544-c71b-4809-9a2d-0583ff32f833");
	public static final UUID uuidAcceptedItis = UUID.fromString("0738c566-0219-4e3d-a8fd-8f3d82e2d20f");

	@Override
	public MarkerType getMarkerTypeByKey(String key) throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){return null;
//		}else if (key.equalsIgnoreCase("distribution")){return MarkerType.;
//		}else if (key.equalsIgnoreCase("habitatecology")){return Feature.ECOLOGY();
		}else{
			return null;
		}
	}

	@Override
	public UUID getMarkerTypeUuid(String key) throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){return null;
//		}else if (key.equalsIgnoreCase("IMPERFECTLY KNOWN SPECIES")){return uuidIncompleteTaxon;
		}else{
			return null;
		}
	}
}