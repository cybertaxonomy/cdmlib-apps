/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.TdwgAreaProvider;
import eu.etaxonomy.cdm.model.location.Country;
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

        if (result == null) {
            result = getNewCountry(state, label);
        }

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

    private NamedArea getNewCountry(MexicoEfloraImportState state, String label) {
        if (StringUtils.isBlank(label)) {
            return null;
        }else if (label.equalsIgnoreCase("CANADÁ")) {return Country.CANADA();
        }else if (label.equalsIgnoreCase("ESTADOS UNIDOS DE AMÉRICA")) {return Country.UNITEDSTATESOFAMERICA();
        }else if (label.equalsIgnoreCase("HONDURAS")) {return Country.HONDURASREPUBLICOF();
        }else if (label.equalsIgnoreCase("BELICE")) {return Country.BELIZE();
        }else if (label.equalsIgnoreCase("EL SALVADOR")) {return Country.ELSALVADORREPUBLICOF();
        }else if (label.equalsIgnoreCase("PANAMÁ")) {return Country.PANAMAREPUBLICOF();
        }else if (label.equalsIgnoreCase("GUATEMALA")) {return Country.GUATEMALAREPUBLICOF();
        }else if (label.equalsIgnoreCase("NICARAGUA")) {return Country.NICARAGUAREPUBLICOF();
        }else if (label.equalsIgnoreCase("ECUADOR")) {return Country.ECUADORREPUBLICOF();
        }else if (label.equalsIgnoreCase("PERÚ")) {return Country.PERUREPUBLICOF();
        }else if (label.equalsIgnoreCase("COSTA RICA")) {return Country.COSTARICAREPUBLICOF();
        }else if (label.equalsIgnoreCase("COLOMBIA")) {return Country.COLOMBIAREPUBLICOF();
        }else if (label.equalsIgnoreCase("CUBA")) {return Country.CUBAREPUBLICOF();
        }else if (label.equalsIgnoreCase("CHILE")) {return Country.CHILEREPUBLICOF();
        }else if (label.equalsIgnoreCase("BOLIVIA")) {return Country.BOLIVIAREPUBLICOF();
        }else if (label.equalsIgnoreCase("URUGUAY")) {return Country.URUGUAYEASTERNREPUBLICOF();
        }else if (label.equalsIgnoreCase("PARAGUAY")) {return Country.PARAGUAYREPUBLICOF();
        }else if (label.equalsIgnoreCase("VENEZUELA")) {return Country.VENEZUELABOLIVARIANREPUBLICOF();
        }else if (label.equalsIgnoreCase("BAHAMAS")) {return Country.BAHAMASCOMMONWEALTHOFTHE();
        }else if (label.equalsIgnoreCase("BRASIL")) {return Country.BRAZILFEDERATIVEREPUBLICOF();
        }else if (label.equalsIgnoreCase("GRANADA")) {return Country.GRENADA();
        }else if (label.equalsIgnoreCase("TRINIDAD Y TOBAGO")) {return Country.TRINIDADANDTOBAGOREPUBLICOF();
        }else if (label.equalsIgnoreCase("GUYANA")) {return Country.GUYANAREPUBLICOF();
        }else if (label.equalsIgnoreCase("COSTA DE MARFIL")) {return Country.COTEDIVOIREIVORYCOASTREPUBLICOFTHE();
        }else if (label.equalsIgnoreCase("JAMAICA")) {return Country.JAMAICA();
        }else if (label.equalsIgnoreCase("PUERTO RICO")) {return Country.PUERTORICO();
        }else if (label.equalsIgnoreCase("ITALIA")) {return Country.ITALYITALIANREPUBLIC();
        }else if (label.equalsIgnoreCase("FRANCIA")) {return Country.FRANCE();
        }else if (label.equalsIgnoreCase("ESPAÑA")) {return Country.SPAINSPANISHSTATE();
        }else if (label.equalsIgnoreCase("PORTUGAL")) {return Country.PORTUGALPORTUGUESEREPUBLIC();
        }else if (label.equalsIgnoreCase("FILIPINAS")) {return Country.PHILIPPINESREPUBLICOFTHE();
        }else if (label.equalsIgnoreCase("INDIA")) {return Country.INDIAREPUBLICOF();
        }else if (label.equalsIgnoreCase("ALASKA")) {return TdwgAreaProvider.getAreaByTdwgAbbreviation("ASK");
        }else if (label.equalsIgnoreCase("AUSTRALIA")) {return Country.AUSTRALIACOMMONWEALTHOF();
        }else if (label.equalsIgnoreCase("REPÚBLICA DOMINICANA")) {return Country.DOMINICANREPUBLIC();
        }else if (label.equalsIgnoreCase("HAITÍ")) {return Country.HAITIREPUBLICOF();
        }else if (label.equalsIgnoreCase("CHINA")) {return Country.CHINAPEOPLESREPUBLICOF();
        }else if (label.equalsIgnoreCase("SUDÁFRICA")) {return Country.SOUTHAFRICAREPUBLICOF();
        }else if (label.equalsIgnoreCase("MADAGASCAR")) {return Country.MADAGASCARREPUBLICOF();
        }else if (label.equalsIgnoreCase("NAMIBIA")) {return Country.NAMIBIA();
        }else if (label.equalsIgnoreCase("ETIOPÍA")) {return Country.ETHIOPIA();
        }else if (label.equalsIgnoreCase("ANGOLA")) {return Country.ANGOLAREPUBLICOF();
        }else if (label.equalsIgnoreCase("TAILANDIA")) {return Country.THAILANDKINGDOMOF();
        }else if (label.equalsIgnoreCase("TANZANIA")) {return Country.TANZANIAUNITEDREPUBLICOF();
        }
        return null;
    }

    private boolean areaIsMexicoCountry(Integer idRegion, String estadoStr,
            String paisStr, Integer idTipoRegion) {
        return idRegion == 2 && isBlank(estadoStr) &&
                "MÉXICO".equalsIgnoreCase(paisStr); // && idTipoRegion == 1;
    }
}