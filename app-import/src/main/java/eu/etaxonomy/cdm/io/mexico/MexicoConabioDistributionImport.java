// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
@Component
public class MexicoConabioDistributionImport<CONFIG extends MexicoConabioImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -1733208053395372094L;

    private static final Logger logger = Logger.getLogger(MexicoConabioDistributionImport.class);

    private OrderedTermVocabulary<NamedArea> stateAreasVoc;
    private NamedArea mexico;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        initAreaVocabulary(state);

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        String idCat = getValue(record, "IdCAT");
        Taxon taxon = state.getHigherTaxon(idCat);
        if (taxon == null){
            logger.warn(line + "Taxon could not be found: " + idCat);
        }else{
            TaxonDescription desc = getTaxonDescription(taxon);

            String distrStatusStr = getValue(record, "TipoDistribucion");
            try {
                PresenceAbsenceTerm mexicanDistributionStatus = state.getTransformer().getPresenceTermByKey(distrStatusStr);
                if (mexicanDistributionStatus != null){
                    Distribution mexicanDistribution = Distribution.NewInstance(mexico, mexicanDistributionStatus);
                    desc.addElement(mexicanDistribution);
                    String refStr = getValue(record, "ReferenciaTipoDistribucion");
                    Reference ref = getReference(state, refStr);
                    if (ref != null){
                        mexicanDistribution.addSource(OriginalSourceType.PrimaryTaxonomicSource,
                                null, null, ref, null);
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

    /**
     * @param state
     * @param refStr
     * @return
     */
    private Reference getReference(SimpleExcelTaxonImportState<CONFIG> state, String refStr) {
        if (StringUtils.isNoneBlank(refStr)){
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

    /**
     * @param desc
     * @param string
     * @param uuidUserDefinedAnnotationTypeVocabulary
     */
    private void handleDistribution(SimpleExcelTaxonImportState<CONFIG> state, TaxonDescription desc, String key,
            UUID uuid, String line) {
        HashMap<String, String> record = state.getOriginalRecord();
        String value = getValue(record, key);
        if ("1".equals(value)){
            NamedArea area = getNamedArea(state, uuid, null, null, null, null, null);
            Distribution dist = Distribution.NewInstance(area, PresenceAbsenceTerm.PRESENT());
            desc.addElement(dist);
        }else if (value != null){
            logger.warn(line + "Unrecognized distribution status '"+value+"' for " + key);
        }
    }

    /**
     * @param taxon
     * @return
     */
    private TaxonDescription getTaxonDescription(Taxon taxon) {
        if (!taxon.getDescriptions().isEmpty()){
            return taxon.getDescriptions().iterator().next();
        }else{
            TaxonDescription desc = TaxonDescription.NewInstance(taxon);
            return desc;
        }
    }

    /**
     * @param state
     */
    @SuppressWarnings("unchecked")
    private void initAreaVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (stateAreasVoc == null){
            stateAreasVoc = (OrderedTermVocabulary<NamedArea>)this.getVocabularyService().find(MexicoConabioTransformer.uuidMexicanStatesVoc);
            if (stateAreasVoc == null){
                createStateAreasVoc(state);
            }
        }
    }

    /**
     * @param state
     * @return
     */
    private void createStateAreasVoc(SimpleExcelTaxonImportState<CONFIG> state) {
        URI termSourceUri = null;
        String label = "Mexican States";
        String description = "Mexican States as used by the CONABIO Rubiaceae database";
        stateAreasVoc = OrderedTermVocabulary.NewInstance(TermType.NamedArea,
                description, label, null, termSourceUri);
        stateAreasVoc.setUuid(MexicoConabioTransformer.uuidMexicanStatesVoc);

        String mexicoLabel = "Mexico (Country)";
        NamedArea newArea = NamedArea.NewInstance(
                mexicoLabel, mexicoLabel, null);
        newArea.setUuid(MexicoConabioTransformer.uuidMexicoCountry);
        stateAreasVoc.addTerm(newArea);

        addArea(state, "Aguascalientes", MexicoConabioTransformer.uuidAguascalientes);
        addArea(state, "Baja California", MexicoConabioTransformer.uuidBaja_california);
        addArea(state, "Baja California Sur", MexicoConabioTransformer.uuidBaja_california_sur);
        addArea(state, "Campeche", MexicoConabioTransformer.uuidCampeche);
        addArea(state, "Coahuila de Zaragoza", MexicoConabioTransformer.uuidCoahuila_de_zaragoza);
        addArea(state, "Colima", MexicoConabioTransformer.uuidColima);
        addArea(state, "Chiapas", MexicoConabioTransformer.uuidChiapas);
        addArea(state, "Chihuahua", MexicoConabioTransformer.uuidChihuahua);
        addArea(state, "Distrito ederal", MexicoConabioTransformer.uuidDistrito_federal);
        addArea(state, "Durango", MexicoConabioTransformer.uuidDurango);
        addArea(state, "Guanajuato", MexicoConabioTransformer.uuidGuanajuato);
        addArea(state, "Guerrero", MexicoConabioTransformer.uuidGuerrero);
        addArea(state, "Hidalgo", MexicoConabioTransformer.uuidHidalgo);
        addArea(state, "Jalisco", MexicoConabioTransformer.uuidJalisco);
        addArea(state, "Mexico", MexicoConabioTransformer.uuidMexico);
        addArea(state, "Michoacan de Ocampo", MexicoConabioTransformer.uuidMichoacan_de_ocampo);
        addArea(state, "Morelos", MexicoConabioTransformer.uuidMorelos);
        addArea(state, "Nayarit", MexicoConabioTransformer.uuidNayarit);
        addArea(state, "Nuevo Leon", MexicoConabioTransformer.uuidNuevo_leon);
        addArea(state, "Oaxaca", MexicoConabioTransformer.uuidOaxaca);
        addArea(state, "Puebla", MexicoConabioTransformer.uuidPuebla);
        addArea(state, "Queretaro de Arteaga", MexicoConabioTransformer.uuidQueretaro_de_arteaga);
        addArea(state, "Quintana Roo", MexicoConabioTransformer.uuidQuintana_roo);
        addArea(state, "San luis Potosi", MexicoConabioTransformer.uuidSan_luis_potosi);
        addArea(state, "Sinaloa", MexicoConabioTransformer.uuidSinaloa);
        addArea(state, "Sonora", MexicoConabioTransformer.uuidSonora);
        addArea(state, "Tabasco", MexicoConabioTransformer.uuidTabasco);
        addArea(state, "Tamaulipas", MexicoConabioTransformer.uuidTamaulipas);
        addArea(state, "Tlaxcala", MexicoConabioTransformer.uuidTlaxcala);
        addArea(state, "Veracruz de Ignacio de la Llave", MexicoConabioTransformer.uuidVeracruz_de_ignacio_de_la_llave);
        addArea(state, "Yucatan", MexicoConabioTransformer.uuidYucatan);
        addArea(state, "Zacatecas", MexicoConabioTransformer.uuidZacatecas);

        this.getVocabularyService().save(stateAreasVoc);

        return;
    }

    /**
     * @param state
     * @param string
     * @param uuidaguascalientes
     */
    private void addArea(SimpleExcelTaxonImportState<CONFIG> state, String areaLabel, UUID uuid) {
        String abbrev = null;
        NamedArea newArea = NamedArea.NewInstance(
                areaLabel, areaLabel, abbrev);
        newArea.setUuid(uuid);
        newArea.setPartOf(mexico);
        stateAreasVoc.addTerm(newArea);
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoDistributions();
    }

}