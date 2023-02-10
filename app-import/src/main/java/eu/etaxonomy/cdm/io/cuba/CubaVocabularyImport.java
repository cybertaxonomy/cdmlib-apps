/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.io.cuba;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.geo.GeoServiceArea;
import eu.etaxonomy.cdm.api.service.geo.GeoServiceAreaAnnotatedMapping;
import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 05.01.2016
 */
@Component
public class CubaVocabularyImport extends CdmImportBase<CubaImportConfigurator, CubaImportState> {
    private static final long serialVersionUID = -747486709409732371L;

    private static final Logger logger = LogManager.getLogger();

    @Override
    protected void doInvoke(CubaImportState state) {
        try {
            makeAreas(state);
            makePresenceAbsenceTerms(state);
            makeAlternativeFloras(state);
        } catch (UndefinedTransformerMethodException e) {
           e.printStackTrace();
        }
    }

    private void makeAlternativeFloras(CubaImportState state) {

        //FRC
        Reference refFRC = ReferenceFactory.newBook();
        refFRC.setUuid(CubaTransformer.uuidRefFRC);
        refFRC.setTitle("Flora de la República de Cuba");
        getReferenceService().save(refFRC);

        //A&S
        Reference refAS = ReferenceFactory.newArticle();
        refAS.setUuid(CubaTransformer.uuidRefAS);
        refAS.setTitle("Catalogue of seed plants of the West Indies");
        Person acevedo = Person.NewInstance();
        acevedo.setGivenName("Pedro");
        acevedo.setFamilyName("Acevedo-Rodríguez");
        Person strong = Person.NewInstance();
        strong.setGivenName("Mark T.");
        strong.setFamilyName("Strong");
        Team asTeam = Team.NewInstance();
        asTeam.addTeamMember(acevedo);
        asTeam.addTeamMember(strong);
        refAS.setAuthorship(asTeam);
        refAS.setDatePublished(TimePeriodParser.parseStringVerbatim("2012-01-01"));
        refAS.setVolume("98");
        refAS.setPages("i-xxv, 1-1192");
        refAS.setIssn("0081-024X");
        refAS.setDoi(DOI.fromString("10.5479/si.0081024X.98.1"));
        refAS.setUri(URI.create("http://hdl.handle.net/10088/17551"));
        String abstracct = "The catalogue enumerates all taxa of Gymnosperms, Dicotyledons, and Monocotyledons occurring in the West Indies archipelago excluding the islands off the coast of Venezuela (Netherlands Antilles, Venezuelan Antilles, Tobago, and Trinidad). For each accepted taxon, nomenclature (including synonyms described from the West Indies and their references to publication), distribution in the West Indies (including endemic, native, or exotic status), common names, and a numerical listing of literature records are given. Type specimen citations are provided for accepted names and synonyms of Cyperaceae, Sapindaceae, and some selected genera in several families including the Apocynaceae (Plumeria), Aquifoliaceae (Ilex), and Santalaceae (Dendrophthora). More than 30,000 names were treated comprising 208 families, 2,033 genera, and 12,279 taxa, which includes exotic and commonly cultivated plants. The total number of indigenous taxa was approximately 10,470 of which 71% (7,446 taxa) are endemic to the archipelago or part of it. Fifteen new names, 37 combinations, and 7 lectotypifications are validated. A searchable website of this catalogue, maintained and continuously updated at the Smithsonian Institution, is available at http://botany.si.edu/antilles/WestIndies/.";
        refAS.setReferenceAbstract(abstracct);
        Reference refASIn = ReferenceFactory.newJournal();
        refASIn.setTitle("Smithsonian Contr. Bot.");
        refAS.setInReference(refASIn);
        getReferenceService().save(refAS);

        //FC
        Reference refFC = ReferenceFactory.newBook();
        refFC.setUuid(CubaTransformer.uuidRefFC);
        refFC.setTitle("Flora de Cuba");
        Person leon = Person.NewTitledInstance("León");
        Person alain = Person.NewTitledInstance("Alain");
        Team fcTeam = Team.NewInstance();
        fcTeam.addTeamMember(leon);
        fcTeam.addTeamMember(alain);
        refAS.setAuthorship(fcTeam);
        getReferenceService().save(refFC);

    }

    private void makePresenceAbsenceTerms(CubaImportState state) throws UndefinedTransformerMethodException {
        TransactionStatus tx = startTransaction();

        IInputTransformer transformer = state.getTransformer();

        //vocabulary
        UUID cubaStatusVocabularyUuid = UUID.fromString("e74bba61-551b-4f59-af83-a1a770e4b0ae");
        String label = "Flora of Cuba Distribution Status";
        String abbrev = null;
        boolean isOrdered = true;
        PresenceAbsenceTerm anyTerm = PresenceAbsenceTerm.PRESENT();  //just any
        TermVocabulary<PresenceAbsenceTerm> cubaStatusVocabualary = getVocabulary(state, TermType.PresenceAbsenceTerm, cubaStatusVocabularyUuid, label, label, abbrev, null, isOrdered, anyTerm);

        final boolean PRESENT = false;

        //doubtfully endemic
        UUID doubtfullyEndemicUuid = transformer.getPresenceTermUuid("?E");
        this.getPresenceTerm(state, doubtfullyEndemicUuid, "endemic, doubtfully present", "endemic, doubtfully present", "?E", false);

        //indigenous
//        UUID indigenousUuid = transformer.getPresenceTermUuid("+");
//        this.getPresenceTerm(state, indigenousUuid, "indigenous", "Indigenous", "+", false);
//        UUID indigenousDoubtfulUuid = transformer.getPresenceTermUuid("?");
//        this.getPresenceTerm(state, indigenousDoubtfulUuid, "indigenous, doubtfully present", "indigenous, doubtfully present", "?", false);

        UUID nonNativeDoubtfulNaturalizedUuid = transformer.getPresenceTermUuid("P");
        this.getPresenceTerm(state, nonNativeDoubtfulNaturalizedUuid, "non-native and doubtfully naturalised", "non-native and doubtfully naturalised", "P", false);
        UUID rareCasualUuid = transformer.getPresenceTermUuid("(A)");
        this.getPresenceTerm(state, rareCasualUuid, "rare casual", "rare casual", "(A)", false);

        //occasionally cultivated
        label = "occasionally cultivated";
        abbrev = "(C)";
        UUID occasionallyCultivatedUuid = transformer.getPresenceTermUuid(abbrev);
        getPresenceTerm(state, occasionallyCultivatedUuid, label, label, abbrev, PRESENT, cubaStatusVocabualary);

        //doubtfully present
        UUID indigenousDoubtfullyPresentUuid = transformer.getPresenceTermUuid("D");
        this.getPresenceTerm(state, indigenousDoubtfullyPresentUuid, "indigenous?", "Indigenous?", "D", false);
        UUID doubtfullyIndigenousDoubtfullyPresentUuid = transformer.getPresenceTermUuid("??");
        this.getPresenceTerm(state, doubtfullyIndigenousDoubtfullyPresentUuid, "?indigenous?", "doubfully indigenous, (and) doubtfully present", "??", false);

        UUID doubtfullyNaturalisedUuid = transformer.getPresenceTermUuid("?N");
        this.getPresenceTerm(state, doubtfullyNaturalisedUuid, "?non-native and doubtfully naturalised", "non-native and doubtfully naturalised, doubtfully present", "?N", false);
        UUID doubtfullyNonNativeUuid = transformer.getPresenceTermUuid("?P");
        this.getPresenceTerm(state, doubtfullyNonNativeUuid, "?adventive (casual) alien ", "adventive (casual) alien, doubtfully present", "?P", false);

        //reported in error
        boolean isAbsent = true;
        UUID endemicErrorUuid = transformer.getPresenceTermUuid("-E");
        this.getPresenceTerm(state, endemicErrorUuid, "endemic, reported in error", "endemic, reported in error", "-E", isAbsent);
        UUID naturalizedErrorUuid = transformer.getPresenceTermUuid("-N");
        this.getPresenceTerm(state, naturalizedErrorUuid, "naturalised, reported in error", "naturalised, reported in error", "-N", isAbsent);
        UUID nonNativeErrorUuid = transformer.getPresenceTermUuid("-P");
        this.getPresenceTerm(state, nonNativeErrorUuid, "non-native and doubtfully naturalised, reported in error", "non-native and doubtfully naturalised, reported in error", "-P", isAbsent);
        UUID casualErrorUuid = transformer.getPresenceTermUuid("-A");
        this.getPresenceTerm(state, casualErrorUuid, "adventive alien , reported in error", "adventive alien , reported in error", "-A", isAbsent);

        commitTransaction(tx);
    }

    private boolean makeAreas(CubaImportState state) throws UndefinedTransformerMethodException{
        TransactionStatus tx = startTransaction();

        IInputTransformer transformer = state.getTransformer();

        //vocabulary
        UUID cubaAreasVocabularyUuid = UUID.fromString("c81e3c7b-3c01-47d1-87cf-388de4b1908c");
        String label = "Cuba Areas";
        String abbrev = null;
        boolean isOrdered = true;
        NamedArea anyArea = NamedArea.ARCTICOCEAN();  //just any
        TermVocabulary<NamedArea> cubaAreasVocabualary = getVocabulary(state, TermType.NamedArea, cubaAreasVocabularyUuid, label, label, abbrev, null, isOrdered, anyArea);

        TermMatchMode matchMode = null;

        NamedAreaType areaType = null;  //TODO
        NamedAreaLevel level = null;  //TODO


//        String mappingFormat = "<?xml version=\"1.0\" ?><mapService xmlns=\"http://www.etaxonomy.eu/cdm\" "
//                + "type=\"editMapService\"><area><layer>%s</layer><field>%s</field><value>%s</value></area></mapService>";
//        String mappingAnnotation = String.format(mappingFormat, layer, field, value);

        String mapping_layer_world = "flora_cuba_2016_world";
        String mapping_field_world = "cuba_0";
        String mapping_layer_regions = "flora_cuba_2016_regions";
        String mapping_field_regions = "cuba_1";
        String mapping_layer_provinces = "flora_cuba_2016_provinces";
        String mapping_field_provinces = "cuba_2";

        //Cuba
        level = NamedAreaLevel.COUNTRY();
        label = "Cuba";
        abbrev = "Cu";
        UUID cubaUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea cuba = getNamedArea(state, cubaUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.setIdInVocabulary(abbrev);
        addMapping(cuba, mapping_layer_world, mapping_field_world, abbrev);

        //Regions
        level = null;

        //Western Cuba
        label = "Western Cuba";
        abbrev = "CuW";
        UUID cubaWestUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea westernCuba = getNamedArea(state, cubaWestUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(westernCuba);
        westernCuba.setIdInVocabulary(abbrev);
        addMapping(westernCuba, mapping_layer_regions, mapping_field_regions, abbrev);

        //Central Cuba
        label = "Central Cuba";
        abbrev = "CuC";
        UUID cubaCentralUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea centralCuba = getNamedArea(state, cubaCentralUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(centralCuba);
        centralCuba.setIdInVocabulary(abbrev);
        addMapping(centralCuba, mapping_layer_regions, mapping_field_regions, abbrev);

        //East Cuba
        label = "East Cuba";
        abbrev = "CuE";
        UUID cubaEastUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea eastCuba = getNamedArea(state, cubaEastUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(eastCuba);
        eastCuba.setIdInVocabulary(abbrev);
        addMapping(eastCuba, mapping_layer_regions, mapping_field_regions, abbrev);

        //Provinces - West
        level = NamedAreaLevel.PROVINCE();

        //Pinar del Río PR
        label = "Pinar del Río";
        abbrev = "PR*";
        UUID uuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

//        //Habana Hab
//        label = "Habana"; //including Ciudad de la Habana, Mayabeque, Artemisa
//        abbrev = "HAB";
//        uuid = transformer.getNamedAreaUuid(abbrev);
//        NamedArea habana = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
//        westernCuba.addIncludes(habana);
//        area.setIdInVocabulary(abbrev);

        //Artemisa
        label = "Artemisa";
        abbrev = "Art";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Ciudad de la Habana
        label = "Ciudad de la Habana";
        abbrev = "Hab*";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Ciudad de la Habana
        label = "Mayabeque";
        abbrev = "May";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);


        //Matanzas Mat
        label = "Matanzas";
        abbrev = "Mat";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Isla de la Juventud IJ
        label = "Isla de la Juventud";
        abbrev = "IJ";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Provinces - Central
        //Villa Clara VC
        label = "Villa Clara";
        abbrev = "VC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Cienfuegos Ci VC
        label = "Cienfuegos";
        abbrev = "Ci";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Sancti Spiritus SS
        label = "Sancti Spíritus";
        abbrev = "SS";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Ciego de Ávila CA
        label = "Ciego de Ávila";
        abbrev = "CA";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Camagüey Cam
        label = "Camagüey";
        abbrev = "Cam";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Las Tunas LT
        label = "Las Tunas";
        abbrev = "LT";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Provinces - East
        //Granma Gr
        label = "Granma";
        abbrev = "Gr";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Holguín Ho
        label = "Holguín";
        abbrev = "Ho";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Santiago de Cuba SC
        label = "Santiago de Cuba";
        abbrev = "SC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //Guantánamo Gu
        label = "Guantánamo";
        abbrev = "Gu";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_provinces, mapping_field_provinces, abbrev);

        //other Greater Antilles (Cuba, Española, Jamaica, Puerto Rico)
        level = null;
        //Española Esp (=Haiti + Dominican Republic)
        label = "Española";
        abbrev = "Esp";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Jamaica Ja
        level = NamedAreaLevel.COUNTRY();
        label = "Jamaica";
        abbrev = "Ja";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Puerto Rico PR
        level = NamedAreaLevel.COUNTRY();
        label = "Puerto Rico";
        abbrev = "PRc";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Lesser Antilles Men
        level = null;
        label = "Lesser Antilles";
        abbrev = "Men";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Bahamas
        label = "Bahamas";
        abbrev = "Bah";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Cayman Islands
        label = "Cayman Islands"; //[Trinidad, Tobago, Curaçao, Margarita, ABC Isl. => S. America];
        abbrev = "Cay";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //World
        //N America
        label = "N America"; //(incl. Mexico)
        abbrev = "AmN";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Central America
        label = "Central America";
        abbrev = "AmC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //S America
        label = "S America";
        abbrev = "AmS";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        //Old World
        label = "Old World ";
        abbrev = "VM";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        area.setIdInVocabulary(abbrev);
        addMapping(area, mapping_layer_world, mapping_field_world, abbrev);

        commitTransaction(tx);
        return true;
    }

    private void addMapping(NamedArea area, String mapping_layer, String mapping_field, String abbrev) {
        GeoServiceAreaAnnotatedMapping mapping = (GeoServiceAreaAnnotatedMapping)this.getBean("geoServiceAreaAnnotatedMapping");
        GeoServiceArea geoServiceArea = new GeoServiceArea();
        geoServiceArea.add(mapping_layer, mapping_field, abbrev);
        mapping.set(area, geoServiceArea);
    }

    @Override
    protected boolean isIgnore(CubaImportState state) {
        return ! state.getConfig().isDoVocabularies();
    }


    @Override
    protected boolean doCheck(CubaImportState state) {
        logger.warn("DoCheck not yet implemented for CubaVocabularyImport");
        return true;
    }



}
