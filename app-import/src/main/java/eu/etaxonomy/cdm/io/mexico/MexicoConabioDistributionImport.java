/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.geo.GeoServiceArea;
import eu.etaxonomy.cdm.api.service.geo.GeoServiceAreaAnnotatedMapping;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermType;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
@Component
public class MexicoConabioDistributionImport<CONFIG extends MexicoConabioImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -1733208053395372094L;
    private static final Logger logger = LogManager.getLogger();

    private OrderedTermVocabulary<NamedArea> stateAreasVoc;
    private NamedArea mexico;

    private Map<String, Taxon> taxonIdMap;


    @Override
    protected String getWorksheetName(CONFIG config) {
        return "DistribucionEstatal";
    }

    private void initTaxa() {
        if (taxonIdMap == null){
            Set<String> existingKeys = MexicoConabioTaxonImport.taxonIdMap.keySet();
            taxonIdMap = getCommonService().getSourcedObjectsByIdInSourceC(Taxon.class,
                    existingKeys, MexicoConabioTaxonImport.TAXON_NAMESPACE);
        }
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        initAreaVocabulary(state);
        initTaxa();

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        String idCat = getValue(record, "IdCAT");
        Taxon taxon = taxonIdMap.get(idCat);
        if (taxon == null){
            logger.warn(line + "Taxon could not be found: " + idCat);
        }else{
            TaxonDescription desc = getTaxonDescription(taxon);

            String distrStatusStr = getValue(record, "TipoDistribucion");
            try {
                if (distrStatusStr != null){
                    PresenceAbsenceTerm mexicanDistributionStatus = state.getTransformer().getPresenceTermByKey(distrStatusStr);
                    if (mexicanDistributionStatus == null){
                        UUID statusUuid = state.getTransformer().getPresenceTermUuid(distrStatusStr);
                        mexicanDistributionStatus = getPresenceTerm(state, statusUuid,
                                distrStatusStr, distrStatusStr, null, false);
                    }
                    if (mexicanDistributionStatus != null){
                        NamedArea mexicoCountry = getMexico();
                        Distribution mexicanDistribution = Distribution.NewInstance(mexicoCountry, mexicanDistributionStatus);
                        desc.addElement(mexicanDistribution);
                        String refStr = getValue(record, "ReferenciaTipoDistribucion");
                        Reference ref = getReference(state, refStr);
                        if (ref != null){
                            mexicanDistribution.addSource(OriginalSourceType.PrimaryTaxonomicSource,
                                    null, null, ref, null);
                        }
                    }
                }
            } catch (UndefinedTransformerMethodException e) {}

            handleDistribution(state, desc, "AGUASCALIENTES", MexicoConabioTransformer.uuidAguascalientes, line);
            handleDistribution(state, desc, "BAJA CALIFORNIA", MexicoConabioTransformer.uuidBaja_california, line);
            handleDistribution(state, desc, "BAJA CALIFORNIA SUR", MexicoConabioTransformer.uuidBaja_california_sur, line);
            handleDistribution(state, desc, "CAMPECHE", MexicoConabioTransformer.uuidCampeche, line);
            handleDistribution(state, desc, "COAHUILA DE ZARAGOZA", MexicoConabioTransformer.uuidCoahuila_de_zaragoza, line);
            handleDistribution(state, desc, "COLIMA", MexicoConabioTransformer.uuidColima, line);
            handleDistribution(state, desc, "CHIAPAS", MexicoConabioTransformer.uuidChiapas, line);
            handleDistribution(state, desc, "CHIHUAHUA", MexicoConabioTransformer.uuidChihuahua, line);
            handleDistribution(state, desc, "DISTRITO FEDERAL", MexicoConabioTransformer.uuidDistrito_federal, line);
            handleDistribution(state, desc, "DURANGO", MexicoConabioTransformer.uuidDurango, line);
            handleDistribution(state, desc, "GUANAJUATO", MexicoConabioTransformer.uuidGuanajuato, line);
            handleDistribution(state, desc, "GUERRERO", MexicoConabioTransformer.uuidGuerrero, line);
            handleDistribution(state, desc, "HIDALGO", MexicoConabioTransformer.uuidHidalgo, line);
            handleDistribution(state, desc, "JALISCO", MexicoConabioTransformer.uuidJalisco, line);
            handleDistribution(state, desc, "MEXICO", MexicoConabioTransformer.uuidMexico, line);
            handleDistribution(state, desc, "MICHOACAN DE OCAMPO", MexicoConabioTransformer.uuidMichoacan_de_ocampo, line);
            handleDistribution(state, desc, "MORELOS", MexicoConabioTransformer.uuidMorelos, line);
            handleDistribution(state, desc, "NAYARIT", MexicoConabioTransformer.uuidNayarit, line);
            handleDistribution(state, desc, "NUEVO LEON", MexicoConabioTransformer.uuidNuevo_leon, line);
            handleDistribution(state, desc, "OAXACA", MexicoConabioTransformer.uuidOaxaca, line);
            handleDistribution(state, desc, "PUEBLA", MexicoConabioTransformer.uuidPuebla, line);
            handleDistribution(state, desc, "QUERETARO DE ARTEAGA", MexicoConabioTransformer.uuidQueretaro_de_arteaga, line);
            handleDistribution(state, desc, "QUINTANA ROO", MexicoConabioTransformer.uuidQuintana_roo, line);
            handleDistribution(state, desc, "SAN LUIS POTOSI", MexicoConabioTransformer.uuidSan_luis_potosi, line);
            handleDistribution(state, desc, "SINALOA", MexicoConabioTransformer.uuidSinaloa, line);
            handleDistribution(state, desc, "SONORA", MexicoConabioTransformer.uuidSonora, line);
            handleDistribution(state, desc, "TABASCO", MexicoConabioTransformer.uuidTabasco, line);
            handleDistribution(state, desc, "TAMAULIPAS", MexicoConabioTransformer.uuidTamaulipas, line);
            handleDistribution(state, desc, "TLAXCALA", MexicoConabioTransformer.uuidTlaxcala, line);
            handleDistribution(state, desc, "VERACRUZ DE IGNACIO DE LA LLAVE", MexicoConabioTransformer.uuidVeracruz_de_ignacio_de_la_llave, line);
            handleDistribution(state, desc, "YUCATAN", MexicoConabioTransformer.uuidYucatan, line);
            handleDistribution(state, desc, "ZACATECAS", MexicoConabioTransformer.uuidZacatecas, line);

            getTaxonService().save(taxon);
        }
    }

    private NamedArea getMexico() {
        if (mexico == null){
            mexico = CdmBase.deproxy(getTermService().find(MexicoConabioTransformer.uuidMexicoCountry), NamedArea.class);
            if (mexico == null){
                logger.warn("Mexico country not found");
            }
        }
        return mexico;
    }

    private Reference getReference(SimpleExcelTaxonImportState<CONFIG> state, String refStr) {
        if (StringUtils.isBlank(refStr)){
            return null;
        }
        Reference ref = state.getReference(refStr);
        if (ref == null){
            ref = ReferenceFactory.newBook();
            ref.setTitleCache(refStr, true);
            state.putReference(refStr, ref);
        }
        return ref;
    }

    private void handleDistribution(SimpleExcelTaxonImportState<CONFIG> state, TaxonDescription desc, String key,
            UUID uuid, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String value = getValue(record, key);
        if ("1".equals(value)){
            NamedArea area = getNamedArea(state, uuid, null, null, null, null, null);
            Distribution dist = Distribution.NewInstance(area, PresenceAbsenceTerm.PRESENT());
            desc.addElement(dist);
        }else if (value != null){
            logger.warn(line + "Unrecognized distribution status '" + value + "' for " + key);
        }
    }

    private TaxonDescription getTaxonDescription(Taxon taxon) {
        if (!taxon.getDescriptions().isEmpty()){
            return taxon.getDescriptions().iterator().next();
        }else{
            TaxonDescription desc = TaxonDescription.NewInstance(taxon);
            return desc;
        }
    }

    @SuppressWarnings("unchecked")
    private void initAreaVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (stateAreasVoc == null){
            stateAreasVoc = (OrderedTermVocabulary<NamedArea>)this.getVocabularyService().find(MexicoConabioTransformer.uuidMexicanStatesVoc);
            if (stateAreasVoc == null){
                createStateAreasVoc(state);
            }
        }
    }

    private void createStateAreasVoc(SimpleExcelTaxonImportState<CONFIG> state) {
        //voc
        URI termSourceUri = null;
        String label = "Mexican States";
        String description = "Mexican States as used by the CONABIO Rubiaceae database";
        stateAreasVoc = OrderedTermVocabulary.NewOrderedInstance(TermType.NamedArea, NamedArea.class,
                description, label, null, termSourceUri);
        stateAreasVoc.setUuid(MexicoConabioTransformer.uuidMexicanStatesVoc);
        Representation rep = Representation.NewInstance("Estados Méxicanos", "Estados Méxicanos", null, Language.SPANISH_CASTILIAN());
        stateAreasVoc.addRepresentation(rep);

        //mexico country
        String mexicoLabel = "Mexico (Country)";
        mexico = NamedArea.NewInstance(
                mexicoLabel, mexicoLabel, null);
        mexico.setUuid(MexicoConabioTransformer.uuidMexicoCountry);
        stateAreasVoc.addTerm(mexico);
         addMapping(mexico, "mex_adm0", "iso", "MEX");

         //Example with almost all areas is Chiococca alba
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
        addArea(state, "Michoacan de Ocampo", MexicoConabioTransformer.uuidMichoacan_de_ocampo, "Michoacán", 16);
         addArea(state, "Morelos", MexicoConabioTransformer.uuidMorelos);
         addArea(state, "Nayarit", MexicoConabioTransformer.uuidNayarit);
        //gibt beim mapping vielleicht Probleme wg. des Accents
        //id_1
        addArea(state, "Nuevo Leon", MexicoConabioTransformer.uuidNuevo_leon, "Nuevo León", 19);
         addArea(state, "Oaxaca", MexicoConabioTransformer.uuidOaxaca);
         addArea(state, "Puebla", MexicoConabioTransformer.uuidPuebla);
        //id_1
        addArea(state, "Queretaro de Arteaga", MexicoConabioTransformer.uuidQueretaro_de_arteaga, "Querétaro", 22);
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


    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String areaLabel, UUID uuid) {
        addArea(state, areaLabel, uuid, areaLabel);  //short cut if label and mapping label are equal
    }

    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String areaLabel, UUID uuid, String mappingLabel) {
        addArea(state, areaLabel, uuid, mappingLabel, null);  //short cut if label and mapping label are equal
    }

    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String areaLabel, UUID uuid, String mappingLabel, Integer id1) {
        String abbrev = null;
        NamedArea newArea = NamedArea.NewInstance(
                areaLabel, areaLabel, abbrev);
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
    }

    private void addMapping(NamedArea area, String mapping_layer, String mapping_field, String abbrev) {
        GeoServiceAreaAnnotatedMapping mapping = (GeoServiceAreaAnnotatedMapping)this.getBean("geoServiceAreaAnnotatedMapping");
        GeoServiceArea geoServiceArea = new GeoServiceArea();
        geoServiceArea.add(mapping_layer, mapping_field, abbrev);
        mapping.set(area, geoServiceArea);
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoDistributions();
    }

}
