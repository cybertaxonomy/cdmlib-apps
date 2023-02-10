/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.geo.GeoServiceArea;
import eu.etaxonomy.cdm.api.service.geo.GeoServiceAreaAnnotatedMapping;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermType;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraRegionImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -361321245993093847L;
    private static final Logger logger = LogManager.getLogger();

    protected static final String NAMESPACE = "Region";

	private static final String pluralString = "regions";
	private static final String dbTableName = "cv2_Controlled_vocabulary_for_Mexican_States";

	public MexicoEfloraRegionImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdRegion "
		        + " FROM " + dbTableName
		        + " ORDER BY IdRegion ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
        String sqlSelect = " SELECT * ";
        String sqlFrom = " FROM " + dbTableName;
 		String sqlWhere = " WHERE ( IdRegion IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {
	    initAreaVocabulary(state);

//	    String label = "Mexican regions";
//	    OrderedTermVocabulary<NamedArea> voc = OrderedTermVocabulary.NewOrderedInstance(TermType.NamedArea, NamedArea.class, label, label, null, null);
//	    getVocabularyService().save(voc);
//
	    boolean success = true ;

//        Set<NamedArea> areasToSave = new HashSet<>();

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
			    int idRegion = rs.getInt("IdRegion");
			    String nombreRegion = rs.getString("NombreRegion");
			    String clave = rs.getString("ClaveRegion");
			    String abbrev = rs.getString("Abreviado");

			    NamedArea area = state.getAreaMap().get(idRegion);
			    if (area == null) {
			        logger.warn("Area not found: " + idRegion + " " + nombreRegion);
			    }else if (!area.getLabel().toLowerCase().equals(nombreRegion.toLowerCase())) {
			        logger.warn("Area does not match: " + idRegion + " " + nombreRegion + " " + area.getLabel());
	            }else {
			        area.getRepresentations().iterator().next().setAbbreviatedLabel(abbrev);
			    }


			    try {
//			        NamedArea area = NamedArea.NewInstance(nombreRegion, nombreRegion, abbrev, Language.SPANISH_CASTILIAN());
//			        area.setIdInVocabulary(clave);
//			        voc.addTerm(area);
//			        state.getAreaMap().put(idRegion, area);
//			        state.getAreaLabelMap().put(nombreRegion, area);

					partitioner.startDoSave();
//					areasToSave.add(area);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create region for id " + idRegion + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

//		getTermService().save(areasToSave);
		return success;
	}

	private OrderedTermVocabulary<NamedArea> stateAreasVoc;

    @SuppressWarnings("unchecked")
    private void initAreaVocabulary(MexicoEfloraImportState state) {
        if (stateAreasVoc == null){
            stateAreasVoc = (OrderedTermVocabulary<NamedArea>)this.getVocabularyService().find(MexicoConabioTransformer.uuidMexicanStatesVoc);
            if (stateAreasVoc == null){
                createStateAreasVoc(state);
            }
        }
    }

    private NamedArea mexico;
    private int idInSource = 4;
    private void createStateAreasVoc(MexicoEfloraImportState state) {
        //voc
        URI termSourceUri = null;
        String label = "Mexican States";
        String description = "Mexican States as used by the CONABIO database";
        stateAreasVoc = OrderedTermVocabulary.NewOrderedInstance(TermType.NamedArea, NamedArea.class,
                description, label, null, termSourceUri);
        stateAreasVoc.setUuid(MexicoConabioTransformer.uuidMexicanStatesVoc);
        Representation rep = Representation.NewInstance("Estados Méxicanos", "Estados Méxicanos", null, Language.SPANISH_CASTILIAN());
        stateAreasVoc.addRepresentation(rep);

        //mexico country
        String mexicoLabel = "México (Country)";
        mexico = NamedArea.NewInstance(
                mexicoLabel, mexicoLabel, null);
        mexico.setUuid(MexicoConabioTransformer.uuidMexicoCountry);
        stateAreasVoc.addTerm(mexico);
        mexico.setIdInVocabulary("2");
        state.getAreaMap().put(2, mexico);
        addMapping(mexico, "mex_adm0", "iso", "MEX");

         addArea(state, "Aguascalientes", MexicoConabioTransformer.uuidAguascalientes);
         addArea(state, "Baja California", MexicoConabioTransformer.uuidBaja_california);
         addArea(state, "Baja California Sur", MexicoConabioTransformer.uuidBaja_california_sur);
         addArea(state, "Campeche", MexicoConabioTransformer.uuidCampeche);
         addArea(state, "Coahuila de Zaragoza", MexicoConabioTransformer.uuidCoahuila_de_zaragoza, "Coahuila");
         addArea(state, "Colima", MexicoConabioTransformer.uuidColima);
         addArea(state, "Chiapas", MexicoConabioTransformer.uuidChiapas);
         addArea(state, "Chihuahua", MexicoConabioTransformer.uuidChihuahua);
         addArea(state, "Distrito Federal", MexicoConabioTransformer.uuidDistrito_federal);
         addArea(state, "Durango", MexicoConabioTransformer.uuidDurango);
         addArea(state, "Guanajuato", MexicoConabioTransformer.uuidGuanajuato);
         addArea(state, "Guerrero", MexicoConabioTransformer.uuidGuerrero);
         addArea(state, "Hidalgo", MexicoConabioTransformer.uuidHidalgo);
         addArea(state, "Jalisco", MexicoConabioTransformer.uuidJalisco);
        //id_1
        addArea(state, "México", MexicoConabioTransformer.uuidMexico, null, 15);
        //id_1
        addArea(state, "Michoacán de Ocampo", MexicoConabioTransformer.uuidMichoacan_de_ocampo, "Michoacán", 16);
         addArea(state, "Morelos", MexicoConabioTransformer.uuidMorelos);
         addArea(state, "Nayarit", MexicoConabioTransformer.uuidNayarit);
        //gibt beim mapping vielleicht Probleme wg. des Accents
        //id_1
        addArea(state, "Nuevo León", MexicoConabioTransformer.uuidNuevo_leon, "Nuevo León", 19);
         addArea(state, "Oaxaca", MexicoConabioTransformer.uuidOaxaca);
         addArea(state, "Puebla", MexicoConabioTransformer.uuidPuebla);
        //id_1
        addArea(state, "Querétaro de Arteaga", MexicoConabioTransformer.uuidQueretaro_de_arteaga, "Querétaro", 22);
         addArea(state, "Quintana Roo", MexicoConabioTransformer.uuidQuintana_roo);
        //id_1
        addArea(state, "San Luis Potosí", MexicoConabioTransformer.uuidSan_luis_potosi,null ,24);
         addArea(state, "Sinaloa", MexicoConabioTransformer.uuidSinaloa);
         addArea(state, "Sonora", MexicoConabioTransformer.uuidSonora);
         addArea(state, "Tabasco", MexicoConabioTransformer.uuidTabasco);
         addArea(state, "Tamaulipas", MexicoConabioTransformer.uuidTamaulipas);
         addArea(state, "Tlaxcala", MexicoConabioTransformer.uuidTlaxcala);
         addArea(state, "Veracruz de Ignacio de la Llave", MexicoConabioTransformer.uuidVeracruz_de_ignacio_de_la_llave, "Veracruz");
        //??
        addArea(state, "Yucatán", MexicoConabioTransformer.uuidYucatan, null, 31);
         addArea(state, "Zacatecas", MexicoConabioTransformer.uuidZacatecas);

        this.getVocabularyService().save(stateAreasVoc);

        return;
    }


    private void addArea(MexicoEfloraImportState state, String areaLabel, UUID uuid) {
        addArea(state, areaLabel, uuid, areaLabel);  //short cut if label and mapping label are equal
    }

    private void addArea(MexicoEfloraImportState state, String areaLabel, UUID uuid, String mappingLabel) {
        addArea(state, areaLabel, uuid, mappingLabel, null);  //short cut if label and mapping label are equal
    }

    private void addArea(MexicoEfloraImportState state, String areaLabel, UUID uuid,
            String mappingLabel, Integer id1) {

        String abbrev = null;
        NamedArea newArea = NamedArea.NewInstance(
                areaLabel, areaLabel, abbrev, Language.SPANISH_CASTILIAN());
        newArea.setUuid(uuid);
        newArea.setPartOf(mexico);
        newArea.setLevel(NamedAreaLevel.STATE());
        newArea.setType(NamedAreaType.ADMINISTRATION_AREA());
        stateAreasVoc.addTerm(newArea);
        if (id1 != null){
            addMapping(newArea, "mex_adm1", "id_1", String.valueOf(id1));
        }else if (mappingLabel != null){
            addMapping(newArea, "mex_adm1", "name_1", mappingLabel);
        }
        //for Eflora:
        newArea.setIdInVocabulary(String.valueOf(idInSource));
        state.getAreaMap().put(idInSource++, newArea);
        state.getAreaLabelMap().put(areaLabel.toLowerCase(), newArea);
    }

    private void addMapping(NamedArea area, String mapping_layer, String mapping_field, String abbrev) {
        GeoServiceAreaAnnotatedMapping mapping = (GeoServiceAreaAnnotatedMapping)this.getBean("geoServiceAreaAnnotatedMapping");
        GeoServiceArea geoServiceArea = new GeoServiceArea();
        geoServiceArea.add(mapping_layer, mapping_field, abbrev);
        mapping.set(area, geoServiceArea);
    }

	//not needed
    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

    @Override
    protected boolean doCheck(MexicoEfloraImportState state){
        return true;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}