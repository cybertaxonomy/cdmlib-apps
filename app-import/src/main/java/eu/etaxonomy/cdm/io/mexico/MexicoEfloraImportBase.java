/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public abstract class MexicoEfloraImportBase
            extends DbImportBase<MexicoEfloraImportState, MexicoEfloraImportConfigurator>
            implements ICdmIO<MexicoEfloraImportState>, IPartitionedIO<MexicoEfloraImportState> {

    private static final long serialVersionUID = -5229728676004248450L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraImportBase.class);

	public MexicoEfloraImportBase(String tableName, String pluralString ) {
		super(tableName, pluralString);
	}

	@Override
    protected String getIdQuery(MexicoEfloraImportState state){
		String result = " SELECT " + getTableName() + "id FROM " + getTableName();
		return result;
	}

	protected Reference sourceReference;
    protected Reference getSourceReference(Reference sourceReference) {
        if (this.sourceReference != null) {
            return this.sourceReference;
        }
        Reference persistentSourceReference = getReferenceService().find(sourceReference.getUuid());  //just to be sure
        if (persistentSourceReference != null){
            sourceReference = persistentSourceReference;
        }
        this.sourceReference = sourceReference;
        return sourceReference;
    }

    protected NamedArea getArea(MexicoEfloraImportState state, Integer idRegion,
            String estadoStr, String paisStr, String abbrev, Integer idTipoRegion) {

        NamedArea areaById = state.getAreaMap().get(idRegion);
        if (areaIsMexicoCountry(idRegion, estadoStr, paisStr, idTipoRegion)) {
            return areaById;
        }
        String label = isNotBlank(estadoStr) ? estadoStr : paisStr;
        String labelKey = label.toLowerCase();
        NamedArea areaByLabel = state.getAreaLabelMap().get(labelKey) ;

        NamedArea result;
        if (areaById != null && areaByLabel != null && !areaById.equals(areaByLabel)){
            logger.warn("Area by label and area by id differs: " + areaById + "<->" + label);
        }
        result = areaById == null? areaByLabel : areaById;

        if (result == null && !"ND".equalsIgnoreCase(label)) {  //ND = no data
            logger.warn("Area not found, create new one: " + idRegion + "; " + label);
            NamedAreaLevel level = idTipoRegion != null && idTipoRegion.equals(1)? NamedAreaLevel.COUNTRY() : null;
            NamedAreaType areaType = NamedAreaType.ADMINISTRATION_AREA();
            //TODO new namedAreas vocabulary
            NamedArea namedArea = this.getNamedArea(state, null, label, label, abbrev, areaType, level, null, null);
            state.getAreaLabelMap().put(labelKey, namedArea);
        }
        return result;
    }

    private boolean areaIsMexicoCountry(Integer idRegion, String estadoStr,
            String paisStr, Integer idTipoRegion) {
        return idRegion == 2 && isBlank(estadoStr) &&
                "MÉXICO".equalsIgnoreCase(paisStr); // && idTipoRegion == 1;
    }
}