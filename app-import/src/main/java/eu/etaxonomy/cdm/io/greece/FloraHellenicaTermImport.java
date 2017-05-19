/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.ext.geo.GeoServiceArea;
import eu.etaxonomy.cdm.ext.geo.GeoServiceAreaAnnotatedMapping;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.OrderedTermBase;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaType;

/**
 * @author a.mueller
 * @date 13.05.2017
 *
 */
@Component
public class FloraHellenicaTermImport <CONFIG extends FloraHellenicaImportConfigurator>
            extends CdmImportBase<CONFIG, SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator>>{

    private static final long serialVersionUID = 1347759514044184010L;

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FloraHellenicaTaxonImport.class);

    private NamedArea greece;



    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInvoke(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {

        TransactionStatus tx = this.startTransaction();
        initAreaVocabulary(state);
        initLifeformVocabulary(state);
        initHabitatVocabulary(state);
        initChorologicalVocabulary(state);
        initStatusVocabulary(state);
        makeFeatureTree(state);
        this.commitTransaction(tx);
    }


    @SuppressWarnings("unchecked")
    private void initAreaVocabulary(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        OrderedTermVocabulary<NamedArea> areasVoc = (OrderedTermVocabulary<NamedArea>)this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaAreasVoc);
        if (areasVoc == null){
            createAreasVoc(state);
        }
    }

    /**
     * @param state
     * @return
     */
    @SuppressWarnings("unchecked")
    private void createAreasVoc(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Areas";
        String description = "Areas as used in the Checklist of Greece";
        OrderedTermVocabulary<NamedArea> areasVoc = OrderedTermVocabulary.NewInstance(TermType.NamedArea,
                description, label, null, termSourceUri);
        areasVoc.setUuid(FloraHellenicaTransformer.uuidFloraHellenicaAreasVoc);
//        Representation rep = Representation.NewInstance("Estados Méxicanos", "Estados Méxicanos", null, Language.SPANISH_CASTILIAN());
//        areasVoc.addRepresentation(rep);

        //greece country
        String countryLabel = "Greece";
        greece = NamedArea.NewInstance(countryLabel, countryLabel, "GR");
        greece.setUuid(FloraHellenicaTransformer.uuidAreaGreece);
        greece.setIdInVocabulary("GR");
        greece.setSymbol("GR");
        areasVoc.addTerm(greece);
        //FIXME
//        addMapping(greece, xx "mex_adm0", "iso", "MEX");

        addArea(state, areasVoc, "IoI", "Ionian Islands", 4, FloraHellenicaTransformer.uuidAreaIoI);
        addArea(state, areasVoc, "NPi", "North Pindos", 13, FloraHellenicaTransformer.uuidAreaNPi);
        addArea(state, areasVoc, "SPi", "South Pindos", 11, FloraHellenicaTransformer.uuidAreaSPi);
        addArea(state, areasVoc, "Pe", "Peloponnisos", 1, FloraHellenicaTransformer.uuidAreaPe);
        addArea(state, areasVoc, "StE", "Sterea Ellas", 12, FloraHellenicaTransformer.uuidAreaStE);
        addArea(state, areasVoc, "EC", "East Central Greece", 5, FloraHellenicaTransformer.uuidAreaEC);
        addArea(state, areasVoc, "NC", "North Central Greece", 2, FloraHellenicaTransformer.uuidAreaNC);
        addArea(state, areasVoc, "NE", "North-East Greece", 10, FloraHellenicaTransformer.uuidAreaNE);
        addArea(state, areasVoc, "NAe", "North Aegean islands", 7, FloraHellenicaTransformer.uuidAreaNAe);
        addArea(state, areasVoc, "WAe", "West Aegean islands", 9, FloraHellenicaTransformer.uuidAreaWAe);
        addArea(state, areasVoc, "Kik", "Kiklades", 8, FloraHellenicaTransformer.uuidAreaKik);
        addArea(state, areasVoc, "KK", "Kriti and Karpathos", 6, FloraHellenicaTransformer.uuidAreaKK);
        addArea(state, areasVoc, "EAe", "East Aegean islands", 3, FloraHellenicaTransformer.uuidAreaEAe);

        this.getVocabularyService().save(areasVoc);
        return;
    }


    private void addArea(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<NamedArea> areasVoc,
            String abbrevLabel, String areaLabel,
            Integer id, UUID uuid) {
        addArea(state, areasVoc, abbrevLabel, areaLabel, uuid, String.valueOf(id));  //short cut if label and mapping label are equal
    }

    private void addArea(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<NamedArea> areasVoc,
            String abbrevLabel, String areaLabel,
            UUID uuid, String mappingLabel) {
        addArea(state, areasVoc, abbrevLabel, areaLabel, uuid, mappingLabel, null);  //short cut if label and mapping label are equal
    }

    /**
     * @param state
     * @param string
     * @param uuidaguascalientes
     */
    private void addArea(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<NamedArea> areasVoc,
            String abbrevLabel, String areaLabel,
            UUID uuid, String mappingLabel, Integer id1) {
        NamedArea newArea = NamedArea.NewInstance(
                areaLabel, areaLabel, abbrevLabel);
        newArea.setIdInVocabulary(abbrevLabel);
        newArea.setUuid(uuid);
        newArea.setPartOf(greece);
        newArea.setLevel(null);
        newArea.setType(NamedAreaType.NATURAL_AREA());
        areasVoc.addTerm(newArea);
        if (mappingLabel != null){
            addMapping(newArea, "phytogeographical_regions_of_greece", "id", mappingLabel);
        }
    }


    private void addMapping(NamedArea area, String mapping_layer, String mapping_field, String abbrev) {
        GeoServiceAreaAnnotatedMapping mapping = (GeoServiceAreaAnnotatedMapping)this.getBean("geoServiceAreaAnnotatedMapping");
        GeoServiceArea geoServiceArea = new GeoServiceArea();
        geoServiceArea.add(mapping_layer, mapping_field, abbrev);
        mapping.set(area, geoServiceArea);
    }


    /**
     * @param state
     */
    private void initChorologicalVocabulary(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaChorologicalVoc;
        OrderedTermVocabulary<State> chorologicalVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
        if (chorologicalVoc == null){
            createChorologicalVoc(state, uuid);
        }
    }

    /**
     * @param state
     * @param uuid
     */
    private void createChorologicalVoc(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Chorological Categories";
        String description = "Chorological Categories as used in the Checklist of Greece";
        OrderedTermVocabulary<State> chorologicalVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        chorologicalVoc.setUuid(vocUuid);

        addChorological(state, chorologicalVoc, "*", "Greek endemic", "Greek endemics (incl. single-island and single-mountain endemics)", FloraHellenicaTransformer.uuidChorologicalStar);
        addChorological(state, chorologicalVoc, "Bk", "Balkan", "Taxa restricted to Balkan countries, occasionally extending to adjacent parts of SE Europe", FloraHellenicaTransformer.uuidChorologicalBk);
        addChorological(state, chorologicalVoc, "BI", "Balkan-Italy", "Taxa restricted to Balkan countries and Italy (amphi-Adreatic)", FloraHellenicaTransformer.uuidChorologicalBI);
        addChorological(state, chorologicalVoc, "BA", "Balkan-Anatolia", "Taxa restricted to Balkan countries and to Asia minor (Anatolia), occasionally extending to S Ukraine (Crimea), adjacent Caucasian countries (Georgia, Armenia) or N Iraq", FloraHellenicaTransformer.uuidChorologicalBA);
        addChorological(state, chorologicalVoc, "BC", "Balkan-Central Europe", "Taxa distributed in the Balkans, Carpathians, Alps and adjacent areas (mainly in the mountains)", FloraHellenicaTransformer.uuidChorologicalBC);
        addChorological(state, chorologicalVoc, "EM", "East Mediterranean", "Taxa restricted to the E Mediterranean, occasionally extending to S Italy or adjacent Caucasian countries", FloraHellenicaTransformer.uuidChorologicalEM);
        addChorological(state, chorologicalVoc, "Me", "Mediterranean", "Taxa with a circum-Mediterranean distribution including Portugal, occasionally extending to the Caucasus area and N Iran", FloraHellenicaTransformer.uuidChorologicalMe);
        addChorological(state, chorologicalVoc, "MA", "Mediterranean-Atlantic", "Taxa restricted to maritime W Europe and the Mediterranean", FloraHellenicaTransformer.uuidChorologicalMA);
        addChorological(state, chorologicalVoc, "ME", "Mediterranean-European", "Taxa restricted to the Mediterranean and temperate Europe, occasionally extending to NW Africa and the Caucasus area", FloraHellenicaTransformer.uuidChorologicalME);
        addChorological(state, chorologicalVoc, "MS", "Mediterranean-SW Asian", "Taxa distributed in one or more Mediterranean countries and extending to SW and C Asia", FloraHellenicaTransformer.uuidChorologicalMS);
        addChorological(state, chorologicalVoc, "EA", "European-SW Asian", "Eruopean taxa (occasionally reachin N Africa) with a distribution extending to SW Asia, occasionally reaching C Asia", FloraHellenicaTransformer.uuidChorologicalEA);
        addChorological(state, chorologicalVoc, "ES", "Euro-Siberian", "Taxa with main distribution in temperate Eurasia (occasionally reaching the Caucasus area)", FloraHellenicaTransformer.uuidChorologicalES);
        addChorological(state, chorologicalVoc, "Eu", "European", "Taxa with a distribution all over Europe. In S European countries this category in fact represents the C European element", FloraHellenicaTransformer.uuidChorologicalEu);
        addChorological(state, chorologicalVoc, "Pt", "Paleotemperate", "Taxa of extratropical Eurasia including the Himalaya and E Asia, not (or at most marginally) extending to North America", FloraHellenicaTransformer.uuidChorologicalPt);
        addChorological(state, chorologicalVoc, "Ct", "Circumtemperate", "Taxa of both extratropical Eurasia and North America", FloraHellenicaTransformer.uuidChorologicalCt);
        addChorological(state, chorologicalVoc, "IT", "Irano-Turanian", "Taxa with main distribution in arid SW and C Asia, extrazonally extending to the Mediterranean", FloraHellenicaTransformer.uuidChorologicalIT);
        addChorological(state, chorologicalVoc, "SS", "Saharo-Sindian", "Taxa with main distribution in arid N Africa and SQ Asia, extrazonally extending to the Mediterranean", FloraHellenicaTransformer.uuidChorologicalSS);
        addChorological(state, chorologicalVoc, "ST", "Subtropical-tropical", "Taxa widespread in the warmer regions of both hemispheres", FloraHellenicaTransformer.uuidChorologicalST);
        addChorological(state, chorologicalVoc, "Bo", "(Circum-)Boreal", "Taxa with main distribution in N and high-montane Eurasia (occasionally extending to North America)", FloraHellenicaTransformer.uuidChorologicalBo);
        addChorological(state, chorologicalVoc, "AA", "Arctic-Alpine", "Taxa with main distribution beyound the N and aove the high-montane timerlines o fEurasia (occasionally extending to North America)", FloraHellenicaTransformer.uuidChorologicalAA);
        addChorological(state, chorologicalVoc, "Co", "Cosmopolitan", "Taxa distributed in all continents, i.e. beyond the N hemisphere. This category may be given in brackets after the known or supposed native distribution in cases of taxa that have been spread worldwide by humans", FloraHellenicaTransformer.uuidChorologicalCo);

        addChorological(state, chorologicalVoc, "[SS]", "[Saharo-Sindian]", "Taxa with main distribution in arid N Africa and SQ Asia, extrazonally extending to the Mediterranean, not native in Greece", FloraHellenicaTransformer.uuidChorological_SS_);
        addChorological(state, chorologicalVoc, "[?MS]", "[Mediterranean-SW Asian]", "Taxa distributed in one or more Mediterranean countries and extending to SW and C Asia, not native in Greece", FloraHellenicaTransformer.uuidChorological_dMS_);
        addChorological(state, chorologicalVoc, "?EM", "? East Mediterranean", "Taxa restricted to the E Mediterranean, occasionally extending to S Italy or adjacent Caucasian countries", FloraHellenicaTransformer.uuidChorological_dEM);


        addChorological(state, chorologicalVoc, "[trop.]", "[tropical]", "", FloraHellenicaTransformer.uuidChorologicaltrop);
        addChorological(state, chorologicalVoc, "[subtrop.]", "[subtropical]", "", FloraHellenicaTransformer.uuidChorologicalsubtrop);
        addChorological(state, chorologicalVoc, "[paleotrop.]", "[paleotropical]", "", FloraHellenicaTransformer.uuidChorologicalpaleotrop);
        addChorological(state, chorologicalVoc, "[neotrop.]", "[neotropical]", "", FloraHellenicaTransformer.uuidChorologicalneotrop);
        addChorological(state, chorologicalVoc, "[pantrop.]", "[pantropical]", "", FloraHellenicaTransformer.uuidChorologicalpantrop);
        addChorological(state, chorologicalVoc, "[N-Am.]", "[North American]", "", FloraHellenicaTransformer.uuidChorologicalN_Am);
        addChorological(state, chorologicalVoc, "[S-Am.]", "[South American]", "", FloraHellenicaTransformer.uuidChorologicalS_Am);
        addChorological(state, chorologicalVoc, "[E-As.]", "[East Asian]", "", FloraHellenicaTransformer.uuidChorologicalE_As);
        addChorological(state, chorologicalVoc, "[SE-As.", "[South East Asian]", "", FloraHellenicaTransformer.uuidChorologicalSE_As);
        addChorological(state, chorologicalVoc, "[S-Afr.]", "[South African]", "", FloraHellenicaTransformer.uuidChorologicalS_Afr);
        addChorological(state, chorologicalVoc, "[Arab.]", "[Arabian]", "", FloraHellenicaTransformer.uuidChorologicalArab);
        addChorological(state, chorologicalVoc, "[Arab. NE-Afr.]", "[Arabian and North East African]", "", FloraHellenicaTransformer.uuidChorologicalArab_NE_Afr);
        addChorological(state, chorologicalVoc, "[Caucas.]", "[Caucasian]", "", FloraHellenicaTransformer.uuidChorologicalCaucas);
        addChorological(state, chorologicalVoc, "[Pontic]", "[Pontic]", "", FloraHellenicaTransformer.uuidChorologicalPontic);
        addChorological(state, chorologicalVoc, "[Europ.]", "[European]", "", FloraHellenicaTransformer.uuidChorologicalEurop);
        addChorological(state, chorologicalVoc, "[Austr.]", "[Australian]", "", FloraHellenicaTransformer.uuidChorologicalAustral);

        addChorological(state, chorologicalVoc, "[W-Med.]", "[West Mediterranean]", "", FloraHellenicaTransformer.uuidChorologicalW_Med);
        addChorological(state, chorologicalVoc, "[C-Med.]", "[Central Mediterranean]", "", FloraHellenicaTransformer.uuidChorologicalC_Med);
        addChorological(state, chorologicalVoc, "[W-Eur.]", "[West European]", "", FloraHellenicaTransformer.uuidChorologicalW_Eur);
        addChorological(state, chorologicalVoc, "[S-Eur.]", "[South European]", "", FloraHellenicaTransformer.uuidChorologicalS_Eur);
        addChorological(state, chorologicalVoc, "[C-Am.]", "[Central American]", "", FloraHellenicaTransformer.uuidChorologicalC_Am);
        addChorological(state, chorologicalVoc, "[C-As.]", "[Central Asian]", "", FloraHellenicaTransformer.uuidChorologicalC_As);
        addChorological(state, chorologicalVoc, "[SW-As.]", "[South West Asian]", "", FloraHellenicaTransformer.uuidChorologicalSW_As);
        addChorological(state, chorologicalVoc, "[unknown]", "[unknown]", "", FloraHellenicaTransformer.uuidChorologicalUnknown);
        addChorological(state, chorologicalVoc, "[N-Afr.]", "[North African]", "", FloraHellenicaTransformer.uuidChorologicalN_Afr);
        addChorological(state, chorologicalVoc, "[Am.]", "[American]", "", FloraHellenicaTransformer.uuidChorologicalAm);
        addChorological(state, chorologicalVoc, "[paleosubtrop.]", "[paleosubtropical]", "", FloraHellenicaTransformer.uuidChorologicalPaleosubtrop);
        addChorological(state, chorologicalVoc, "[SW-Eur.]", "[South West European]", "", FloraHellenicaTransformer.uuidChorologicalSW_Eur);

        addChorological(state, chorologicalVoc, "[S-As.]", "[South Asian]", "", FloraHellenicaTransformer.uuidChorologicalS_As);
        addChorological(state, chorologicalVoc, "[NE-Afr.]", "[North East African]", "", FloraHellenicaTransformer.uuidChorologicalNE_Afr);
        addChorological(state, chorologicalVoc, "[NW-Afr.]", "[North West African]", "", FloraHellenicaTransformer.uuidChorologicalNW_Afr);
        addChorological(state, chorologicalVoc, "[trop. Afr.]", "[tropical African]", "", FloraHellenicaTransformer.uuidChorologicalTrop_Afr);
        addChorological(state, chorologicalVoc, "[Afr.]", "[Arican]", "", FloraHellenicaTransformer.uuidChorologicalAfr);
        addChorological(state, chorologicalVoc, "[As.]", "[Asian]", "", FloraHellenicaTransformer.uuidChorologicalAs);
        addChorological(state, chorologicalVoc, "[W-As.]", "[West Asian]", "", FloraHellenicaTransformer.uuidChorologicalW_As);
        addChorological(state, chorologicalVoc, "[C-Eur.]", "[Central European]", "", FloraHellenicaTransformer.uuidChorologicalC_Eur);
        addChorological(state, chorologicalVoc, "[E-Afr.]", "[East African]", "", FloraHellenicaTransformer.uuidChorologicalE_Afr);
        addChorological(state, chorologicalVoc, "[W-Austr.]", "[West Australian]", "", FloraHellenicaTransformer.uuidChorologicalW_Austr);
        addChorological(state, chorologicalVoc, "[trop. As.]", "[tropical Asian]", "", FloraHellenicaTransformer.uuidChorologicaltrop_As);

        addChorological(state, chorologicalVoc, "[Co]", "[Cosmopolitan]", "Taxa distributed in all continents, i.e. beyond the N hemisphere. This category may be given in brackets after the known or supposed native distribution in cases of taxa that have been spread worldwide by humans", FloraHellenicaTransformer.uuidChorological__Co_);

        this.getVocabularyService().save(chorologicalVoc);
        return;

    }

    /**
     * @param state
     * @param chorologicalVoc
     * @param string
     * @param string2
     * @param string3
     * @param uuidchorologicalstar
     */
    private void addChorological(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<State> chorologicalVoc, String abbrevLabel, String label,
            String desc, UUID uuidChorological) {
        desc = isBlank(desc)? label : desc;
        State chorologyTerm = addState(state, abbrevLabel, label, desc, uuidChorological, chorologicalVoc);
    }

 // ***************************** LIFEFORM ********************************/


    @SuppressWarnings("unchecked")
    private void initLifeformVocabulary(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaLifeformVoc;
        OrderedTermVocabulary<State> lifeformVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
        if (lifeformVoc == null){
            createLifeformVoc(state, uuid);
        }
    }

    /**
     * @param state
     * @param vocUuid
     */
    private void createLifeformVoc(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Lifeforms";
        String description = "Lifeforms as used in the Checklist of Greece";
        OrderedTermVocabulary<State> lifeformVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        lifeformVoc.setUuid(vocUuid);

        addLifeform(state, lifeformVoc, "A", "Aquatic", FloraHellenicaTransformer.uuidLifeformA);
        addLifeform(state, lifeformVoc, "C", "Chamaephyte", FloraHellenicaTransformer.uuidLifeformC);
        addLifeform(state, lifeformVoc, "G", "Geophyte (Cryptophyte)", FloraHellenicaTransformer.uuidLifeformG);
        addLifeform(state, lifeformVoc, "H", "Hemicryptophyte", FloraHellenicaTransformer.uuidLifeformH);
        addLifeform(state, lifeformVoc, "P", "Phanerophyte", FloraHellenicaTransformer.uuidLifeformP);
        addLifeform(state, lifeformVoc, "T", "Therophyte", FloraHellenicaTransformer.uuidLifeformT);
        this.getVocabularyService().save(lifeformVoc);
        return;
    }

    /**
     * @param state
     * @param lifeformVoc
     * @param string
     * @param uuidlifeformt
     */
    private void addLifeform(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<State> lifeformVoc, String abbrevLabel, String label, UUID uuidlifeform) {
        State lifeForm = addState(state, abbrevLabel, label, label, uuidlifeform, lifeformVoc);
    }


// ***************************** HABITAT ********************************/

    private void initHabitatVocabulary(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaHabitatVoc;
        OrderedTermVocabulary<State> habitatVoc = (OrderedTermVocabulary<State>)this.getVocabularyService().find(uuid);
        if (habitatVoc == null){
            createHabitatVoc(state, uuid);
        }
    }

    /**
     * @param state
     */
    private void createHabitatVoc(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Habitats";
        String description = "Habitats as used in the Checklist of Greece";
        OrderedTermVocabulary<State> habitatVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                description, label, null, termSourceUri);
        habitatVoc.setUuid(vocUuid);

        addHabitat(state, habitatVoc, "A", "Freshwater habitats", "Freshwater habitats (Aquatic habitats, springs and fens, reedbeds and damp tall herb vegetation, seasonally flooded depressions, damp and seepage meadows, streambanks, river and lake shores)", FloraHellenicaTransformer.uuidHabitatA);
        addHabitat(state, habitatVoc, "C", "Cliffs, rocks, walls, ravines, boulders", "Cliffs, rocks, walls, ravines, boulders", FloraHellenicaTransformer.uuidHabitatC);
        addHabitat(state, habitatVoc, "G", "Temperate and submediterranean Grasslands", "Temperate and submediterranean Grasslands (lowland to montane dry and mesic meadows and pastures, rock outcrops and stony ground, grassy non-ruderal verges and forest edges)", FloraHellenicaTransformer.uuidHabitatG);
        addHabitat(state, habitatVoc, "H", "High mountain vegetation", "High mountain vegetation (subalpine and alpine grasslands, screes and rocks, scrub above the treeline)", FloraHellenicaTransformer.uuidHabitatH);
        addHabitat(state, habitatVoc, "M", "Coastal habitats", "Coastal habitats (Marine waters and mudflats, salt marshes, sand dunes, littoral rocks, halo-nitrophilous scrub)", FloraHellenicaTransformer.uuidHabitatM);
        addHabitat(state, habitatVoc, "P", "Xeric Mediterranean Phrygana and grasslands", "Xeric Mediterranean Phrygana and grasslands (Mediterranean dwarf shrub formations, annual-rich pastures and lowland screes)", FloraHellenicaTransformer.uuidHabitatP);
        addHabitat(state, habitatVoc, "R", "Agricultural and Ruderal habitats", "Agricultural and Ruderal habitats (fields, gardens and plantations, roadsides and trampled sites, frequently disturbed and pioneer habitats)", FloraHellenicaTransformer.uuidHabitatR);
        addHabitat(state, habitatVoc, "W", "Woodlands and scrub", "Woodlands and scrub (broadleaved and coniferous forest, riparian and mountain forest and scrub, hedges, shady woodland margins)", FloraHellenicaTransformer.uuidHabitatW);

        this.getVocabularyService().save(habitatVoc);
        return;
    }

    /**
     * @param state
     * @param habitatVoc
     * @param string
     * @param uuidlifeformt
     */
    private void addHabitat(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            OrderedTermVocabulary<State> habitatVoc, String abbrev, String label, String desc, UUID uuidHabitat) {
        addState(state, abbrev, label, desc, uuidHabitat, habitatVoc);
    }

// ***************************** STATUS ********************************/

    private void initStatusVocabulary(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaStatusVoc;
        OrderedTermVocabulary<?> statusVoc = (OrderedTermVocabulary<?>)this.getVocabularyService().find(uuid);
        if (statusVoc == null){
            createStatusVoc(state, uuid);
        }
    }

    /**
     * @param state
     */
    private void createStatusVoc(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state, UUID vocUuid) {
        //voc
        URI termSourceUri = null;
        String label = "Checklist of Greece Status";
        String description = "Status as used in the Checklist of Greece";

        OrderedTermVocabulary<?> statusVoc;
        if (state.getConfig().isStatusAsDistribution()){
            statusVoc = OrderedTermVocabulary.NewInstance(TermType.PresenceAbsenceTerm,
                    description, label, null, termSourceUri);
        }else{
            statusVoc = OrderedTermVocabulary.NewInstance(TermType.State,
                    description, label, null, termSourceUri);
        }

        statusVoc.setUuid(vocUuid);

        if (!state.getConfig().isStatusAsDistribution()){
            addStatus(state, "N", "Native", "", FloraHellenicaTransformer.uuidStatusNative, statusVoc);
            addStatus(state, "X", "Xenophyte", "", FloraHellenicaTransformer.uuidStatusXenophyte, statusVoc);
            addStatus(state, "?X", "?Xenophyte", "", FloraHellenicaTransformer.uuidStatusXenophyteDoubtfully, statusVoc);
        }
        addStatus(state, "RR", "Range-restricted", "", FloraHellenicaTransformer.uuidStatusRangeRestricted, statusVoc);
        addStatus(state, "?RR", "?Range-restricted", "", FloraHellenicaTransformer.uuidStatusRangeRestrictedDoubtfully, statusVoc);



        this.getVocabularyService().save(statusVoc);
        return;
    }

// ************************** FEATURE TREE  ************************************/

    boolean hasFeatureTree = false;

    /**
     * @param state
     */
    private void makeFeatureTree(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        if (hasFeatureTree  ){
            return;
        }
        if (getFeatureTreeService().find(state.getConfig().getUuidFeatureTree()) != null){
            hasFeatureTree = true;
            return;
        }
        FeatureTree result = FeatureTree.NewInstance(state.getConfig().getUuidFeatureTree());
        result.setTitleCache(state.getConfig().getFeatureTreeTitle(), true);
        FeatureNode root = result.getRoot();
        FeatureNode newNode;
        ITermService service = getTermService();

        Feature newFeature = (Feature)service.find(Feature.DESCRIPTION().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.DISTRIBUTION().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.STATUS().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);
        newFeature.setSupportsCategoricalData(true);
        TermVocabulary<State> voc = this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaStatusVoc);
        newFeature.addSupportedCategoricalEnumeration(voc);


        UUID uuid = FloraHellenicaTransformer.uuidFloraHellenicaChorologyFeature;
        newFeature = getFeature(state, uuid, "Chorology", "Chorology", null, null);
        newFeature.setSupportsCategoricalData(true);
        voc = this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaChorologicalVoc);
        newFeature.addSupportedCategoricalEnumeration(voc);
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        newFeature = (Feature)service.find(Feature.LIFEFORM().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);
        newFeature.setSupportsCategoricalData(true);
        voc = this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaLifeformVoc);
        newFeature.addSupportedCategoricalEnumeration(voc);

        newFeature = (Feature)service.find(Feature.HABITAT().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);
        newFeature.setSupportsCategoricalData(true);
        voc = this.getVocabularyService().find(FloraHellenicaTransformer.uuidFloraHellenicaHabitatVoc);
        newFeature.addSupportedCategoricalEnumeration(voc);


        newFeature = (Feature)service.find(Feature.NOTES().getUuid());
        newNode = FeatureNode.NewInstance(newFeature);
        root.addChild(newNode);

        getFeatureTreeService().saveOrUpdate(result);
        hasFeatureTree = true;

    }


    private State addState(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            String abbrev, String stateLabel, String description, UUID uuid, OrderedTermVocabulary<State> voc) {
        State newState = State.NewInstance(
                description, stateLabel, abbrev);
        newState.setUuid(uuid);
        newState.setIdInVocabulary(abbrev);
        voc.addTerm(newState);
        return newState;
    }

    private OrderedTermBase addStatus(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state,
            String abbrev, String stateLabel, String description, UUID uuid,
            OrderedTermVocabulary voc) {
        FloraHellenicaImportConfigurator config = state.getConfig();
        OrderedTermBase<?> newStatus;
        if (config.isStatusAsDistribution()){
            newStatus = PresenceAbsenceTerm.NewPresenceInstance( description, stateLabel, abbrev);
        }else{
            newStatus = State.NewInstance(description, stateLabel, abbrev);
        }

        newStatus.setUuid(uuid);
        newStatus.setIdInVocabulary(abbrev);
        newStatus.setSymbol(abbrev);
        voc.addTerm(newStatus);
        return newStatus;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doCheck(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<FloraHellenicaImportConfigurator> state) {
        return false;
    }
}
