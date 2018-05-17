/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.salvador;

import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.csv.in.CsvImportBase;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 08.07.2017
 *
 */
@Component
public class SalvadorSpecimenImport
            extends CsvImportBase<SalvadorSpecimenImportConfigurator,SalvadorSpecimenImportState, FieldUnit> {

    private static final long serialVersionUID = -2165916187195347780L;

    private ImportDeduplicationHelper<?> dedupHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleSingleLine(SalvadorSpecimenImportState state) {

        initDedupHelper();
        try {
            UUID factUuid = UUID.fromString(state.getCurrentRecord().get("specimenFactUuid"));

            if (existingFieldUnits.get(factUuid)== null){

                FieldUnit fieldUnit = makeFieldUnit(state);
                DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(
                        SpecimenOrObservationType.PreservedSpecimen, fieldUnit);
                makeFieldUnitData(state, facade);
                makeSpecimen(state, facade);
            }else{
                FieldUnit fieldUnit = fieldUnitMap.get(factUuid);
                if (fieldUnit == null){
                    fieldUnit = CdmBase.deproxy(getOccurrenceService().find(existingFieldUnits.get(factUuid)), FieldUnit.class);
                    fieldUnitMap.put(factUuid, fieldUnit);
                }
                DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(
                        SpecimenOrObservationType.PreservedSpecimen, fieldUnit);
                makeSpecimenDuplicate(state, facade);
            }

            return;
        } catch (Exception e) {
            String message = "Unexpected error in handleSingleLine: " + e.getMessage();
            state.getResult().addException(e, message, null, String.valueOf(state.getRow()));
            e.printStackTrace();
        }
    }


    /**
     *
     */
    private void initDedupHelper() {
        if (dedupHelper == null){
            dedupHelper = ImportDeduplicationHelper.NewStandaloneInstance();
        }
    }


    /**
     * @param config
     * @param facade
     * @param importResult
     */
    private void makeSpecimenDuplicate(SalvadorSpecimenImportState state,
            DerivedUnitFacade facade) {

        Map<String, String> record = state.getCurrentRecord();

        TaxonDescription desc = getTaxonDescription(state, record);

        int row = state.getRow();
        String herbariaStr = record.get("Herbaria");
        String[] splits = herbariaStr.split(";");
        for (String split : splits){
            if ("B".equals(split)){
                Collection collection = getCollection(split, row);
                facade.setCollection(collection);
                if (record.get("B-Barcode") != null){
                    facade.setBarcode(record.get("B-Barcode"));
                }
                String uriStr = record.get("B_UUID");
                if (uriStr != null){
                    URI uri = URI.create(uriStr);
                    facade.setPreferredStableUri(uri);
                }
                IndividualsAssociation assoc = IndividualsAssociation.NewInstance(facade.innerDerivedUnit());
                assoc.setFeature(Feature.SPECIMEN());
                desc.addElement(assoc);
            }

        }
    }


    private Map<UUID, FieldUnit> fieldUnitMap = new HashMap<>();
    private Map<UUID, UUID> existingFieldUnits = new HashMap<>();
    private FieldUnit makeFieldUnit(SalvadorSpecimenImportState state) {

        Map<String, String> record = state.getCurrentRecord();
        UUID factUuid = UUID.fromString(record.get("specimenFactUuid"));

        TextData textSpecimen = (TextData)getDescriptionService().getDescriptionElementByUuid(factUuid);
        textSpecimen.setFeature(getTexSpecimenFeature());

        FieldUnit fieldUnit = FieldUnit.NewInstance();

        fieldUnitMap.put(factUuid, fieldUnit);
        existingFieldUnits.put(factUuid, fieldUnit.getUuid());

        return fieldUnit;
    }

    //taxonUuid, TaxonDescription map
    private Map<UUID, TaxonDescription> taxonDescMap = new HashMap<>();
    //taxonUuid, TaxonDescription.uuid map
    private Map<UUID, UUID> existingTaxonDescs = new HashMap<>();

    private TaxonDescription getTaxonDescription(SalvadorSpecimenImportState state,
            Map<String, String> record) {

        int row = state.getRow();
        UUID taxonUuid = UUID.fromString(record.get("taxonUuid"));
        TaxonDescription taxonDesc = taxonDescMap.get(taxonUuid);
        if (taxonDesc == null && existingTaxonDescs.get(taxonUuid) != null){
            taxonDesc = (TaxonDescription)getDescriptionService().find(existingTaxonDescs.get(taxonUuid));
            taxonDescMap.put(taxonUuid, taxonDesc);
        }
        if (taxonDesc == null){

            Taxon taxon = (Taxon)getTaxonService().find(taxonUuid);

            taxonDesc = TaxonDescription.NewInstance(taxon);
            taxonDesc.setTitleCache("JACQ import for " + taxon.getName().getTitleCache(), true);
            taxonDesc.addImportSource(null, null, state.getConfig().getSourceReference(), String.valueOf(row));
            taxonDescMap.put(taxonUuid, taxonDesc);
            existingTaxonDescs.put(taxonUuid, taxonDesc.getUuid());
        }else{
            System.out.println("Reuse desc: " + row);
        }

        return taxonDesc;
    }

    /**
     * @param config
     * @param facade
     * @param importResult
     */
    private void makeSpecimen(SalvadorSpecimenImportState state, DerivedUnitFacade facade) {

        Map<String, String> record = state.getCurrentRecord();

        TaxonDescription desc = getTaxonDescription(state, record);

        int row = state.getRow();
        String herbariaStr = record.get("Herbaria");
        String laguUuidStr = record.get("LAGU_UUID");
        if (laguUuidStr != null && !herbariaStr.contains("LAGU")){
            herbariaStr += ";LAGU";
        }
        String[] splits = herbariaStr.split(";");
        boolean isFirst = true;
        for (String split : splits){
            Collection collection = getCollection(split, row);
            DerivedUnit unit;
            if (isFirst){
                facade.setCollection(collection);
                unit = facade.innerDerivedUnit();
            }else{
                unit = facade.addDuplicate(collection, null, null, null, null);
            }
            isFirst = false;
            if ("B".equalsIgnoreCase(split)){
                unit.setBarcode(record.get("B-Barcode"));
                String uriStr = record.get("B_UUID");
                if (uriStr != null){
                    URI uri = URI.create(uriStr);
                    unit.setPreferredStableUri(uri);
                }
            }else if ("LAGU".equalsIgnoreCase(split)){
                String uriStr = record.get("LAGU_UUID");
                if (uriStr != null){
                    URI uri = URI.create(uriStr);
                    unit.setPreferredStableUri(uri);
                }
            }

            IndividualsAssociation assoc = IndividualsAssociation.NewInstance(unit);
            assoc.setFeature(Feature.SPECIMEN());
            desc.addElement(assoc);
        }
    }

    private Map<String, Collection> collectionMap = new HashMap<>();

    private Collection getCollection(String code, int row) {

        if (StringUtils.isBlank(code)){
            return null;
        }
        if (collectionMap.isEmpty()){
            List<Collection> collections = getCollectionService().list(null, null, null, null, null);
            for (Collection collection :collections){
                collectionMap.put(collection.getCode(), collection);
            }
        }
        if (collectionMap.get(code) == null){
            Collection collection = Collection.NewInstance();
            collection.setCode(code);
            collectionMap.put(code, collection);
            getCollectionService().save(collection);
        }
        return collectionMap.get(code);
    }

    /**
     * @param config
     * @param facade
     * @param importResult
     */
    private void makeFieldUnitData(SalvadorSpecimenImportState state, DerivedUnitFacade facade) {

        Map<String, String> record = state.getCurrentRecord();
        int row = state.getRow();

        Language spanish = Language.SPANISH_CASTILIAN();

        //idInSource
        String idInSource = record.get("IdInSource");
        String nameSpace = "http://resolv.jacq.org/";
        facade.innerFieldUnit().addImportSource(idInSource, nameSpace,
                state.getConfig().getSourceReference(), String.valueOf(row));

        //collector
        TeamOrPersonBase<?> collector = makeCollectorTeam(state, record, row);
        if (collector != null){
            collector = dedupHelper.getExistingAuthor(null, collector);
            facade.setCollector(collector);
        }

        //collectorsNumber
        facade.setFieldNumber(record.get("CollectorsNumber"));

        //CollectionDate
        String collectionDate = record.get("CollectionDate");
        if (collectionDate != null){
            TimePeriod tp;
            if (collectionDate.equals("1987")){
                tp = TimePeriod.NewInstance(1987);
                state.getResult().addWarning("Only year is not correct: " + collectionDate, state.getRow());
            }else{
                DateTimeFormatter f = DateTimeFormat.forPattern("yyyy-MM-dd");
                collectionDate = collectionDate.replace(" 00:00:00", "");
                DateTime dateTime = f.parseDateTime(collectionDate);
                tp = TimePeriod.NewInstance(dateTime, null);
            }
            facade.getGatheringEvent(true).setTimeperiod(tp);
        }
        //Country
        Country country = makeCountry(state, record, row);
        facade.setCountry(country);

        //Area_Major
        NamedArea area = makeMajorArea(state);
        if(area != null){
            facade.addCollectingArea(area);
        }

        //Locality
        String locality = record.get("Locality");
        if (locality != null){
            facade.setLocality(locality, spanish);
        }

        //Geo
        String latitude = record.get("LatitudeDecimal");
        String longitude = record.get("LongitudeDecimal");
        longitude = normalizeLongitude(longitude);
        if (latitude != null || longitude != null){
            if (latitude == null || longitude == null){
                state.getResult().addError("Only Lat or Long is null", row);
            }
            if (!"WGS84".equals(record.get("ReferenceSystem"))){
                state.getResult().addWarning("Reference system is not WGS84", row);
            }
            String errorRadiusStr =record.get("ErrorRadius");
            Integer errorRadius = null;
            if (errorRadiusStr != null){
                errorRadius = Integer.valueOf(errorRadiusStr);
            }
            try {
                facade.setExactLocationByParsing(longitude, latitude, ReferenceSystem.WGS84(), errorRadius);
            } catch (ParseException e) {
                state.getResult().addError("Error when parsing exact location" + e.getMessage(), row);
            }
        }

        //Altitude
        String altStr = record.get("Altitude");
        if (altStr != null){
            facade.setAbsoluteElevation(Integer.valueOf(altStr));
        }
        String altStrMax = record.get("AltitudeMax");
        if (altStrMax != null){
            facade.setAbsoluteElevationMax(Integer.valueOf(altStrMax));
        }

        //habitat
        String habitatStr = record.get("habitat");
        if (habitatStr != null){
            //TODO habitat, not ecology
            facade.setEcology(habitatStr, spanish);
//            //habitat
//            TextData habitat = TextData.NewInstance(Feature.HABITAT(), habitatStr, spanish, null);
//            facade.innerFieldUnit().getDescriptions().iterator().next()
//                .addElement(habitat);
//            facade.removeEcology(spanish);
        }

        //plant description
        String plantDescription = record.get("PlantDescription");
        if (plantDescription != null){
            facade.setPlantDescription(plantDescription, spanish);
        }

        //note
        //TODO is this field unit??
        String note = record.get("note");
        if (note != null){
            facade.innerFieldUnit().addAnnotation(Annotation.NewInstance(note, spanish));
        }

        //IdentificationHistory
        String identificationHistory = record.get("IdentificationHistory");
        if (identificationHistory != null){
            ExtensionType type = getExtensionType();
            facade.innerFieldUnit().addExtension(identificationHistory, type);
        }

        //LocalCommonName
        String localCommonName = record.get("LocalCommonName");
        if (localCommonName != null){
            CommonTaxonName commonName = CommonTaxonName.NewInstance(localCommonName, spanish);
            Set<SpecimenDescription> descs = (Set)facade.innerFieldUnit().getDescriptions();
            if (descs.isEmpty()){
                SpecimenDescription desc = SpecimenDescription.NewInstance(facade.innerFieldUnit());
                descs.add(desc);
            }
            descs.iterator().next().addElement(commonName);
        }

    }


    /**
     * @param longitude
     * @return
     */
    private String normalizeLongitude(String longitude) {
        if (longitude == null || longitude.startsWith("-")){
            return longitude;
        }else{
            return "-" + longitude;
        }
    }


    private ExtensionType identificationHistoryType;

    /**
     * @return
     */
    private ExtensionType getExtensionType() {
        if (identificationHistoryType == null){
            identificationHistoryType = ExtensionType.NewInstance("Identification History", "Identification History", null);
            UUID vocUuid = uuidUserDefinedExtensionTypeVocabulary;
            TermVocabulary<ExtensionType> voc = getVocabularyService().find(vocUuid);
            if (voc == null){
                voc = TermVocabulary.NewInstance(TermType.ExtensionType,
                        "User defined extension types", "User defined extension types", null, null);
                getVocabularyService().save(voc);
            }
            voc.addTerm(identificationHistoryType);
            getTermService().saveOrUpdate(identificationHistoryType);
        }
        return identificationHistoryType;
    }



    private Feature textSpecimenFeature;


    private Feature getTexSpecimenFeature() {
        if (textSpecimenFeature == null){
            UUID uuidSpecimenTextOld = SalvadorImportTransformer.uuidSalvadorTextSpecimenOldFeature;
            textSpecimenFeature = (Feature)getTermService().find(uuidSpecimenTextOld);
        }
        if (textSpecimenFeature == null){
            String label = "Text Specimen";
            textSpecimenFeature = Feature.NewInstance(label, label, null);
            UUID vocUuid = SalvadorImportTransformer.uuidSalvadorFeatureVoc;
            TermVocabulary<Feature> voc = getVocabularyService().find(vocUuid);
            if (voc == null){
                voc = TermVocabulary.NewInstance(TermType.Feature,
                        "User defined features", "User defined features", null, null);
                getVocabularyService().save(voc);
            }
            textSpecimenFeature.setUuid(SalvadorImportTransformer.uuidSalvadorTextSpecimenOldFeature);
            voc.addTerm(textSpecimenFeature);
            getTermService().saveOrUpdate(textSpecimenFeature);
        }
        return textSpecimenFeature;
    }


    private Map<String, NamedArea> majorAreaMap = null;

    /**
     * @param state
     * @param record
     * @param row
     * @return
     */
    private NamedArea makeMajorArea(SalvadorSpecimenImportState state) {

        if (majorAreaMap == null){
            majorAreaMap = new HashMap<>();
            TermVocabulary<NamedArea> voc = getVocabularyService().find(UUID.fromString("8ef90ca3-77d7-4adc-8bbc-1eb354e61b65"));
            for (NamedArea area : voc.getTerms()){
                majorAreaMap.put(area.getTitleCache(), area);
            }
        }

        String areaStr = state.getCurrentRecord().get("Area_Major");
        NamedArea area = majorAreaMap.get(areaStr);
        if (area == null && areaStr != null){
            state.getResult().addError("Major area not found: " + areaStr, state.getRow());
        }
        return area;
    }

    /**
     * @param state
     * @param record
     * @param i
     * @return
     */
    private Country makeCountry(SalvadorSpecimenImportState state, Map<String, String> record, int row) {
        String iso = record.get("IsoCountry");
        String countryStr = record.get("COUNTRY");
        if (iso == null && countryStr == null){
            return null;
        }else if ("SLV".equals(iso) && "El Salvador".equals(countryStr)){
            return Country.ELSALVADORREPUBLICOF();
        }else if ("HND".equals(iso) && "Honduras".equals(countryStr)){
            return Country.HONDURASREPUBLICOF();
        }else if ("GTM".equals(iso) && "Guatemala".equals(countryStr)){
            return Country.GUATEMALAREPUBLICOF();
        }else{
            String message = "Iso-country combination not recognized: " + iso + " - " + countryStr;
            state.getResult().addWarning(message, row);
            return null;
        }
    }

    /**
     * @param state
     * @param record
     * @param row
     * @param importResult
     * @return
     */
    private TeamOrPersonBase<?> makeCollectorTeam(SalvadorSpecimenImportState state, Map<String, String> record, int row) {

        Team team = Team.NewInstance();
        String first = record.get("COLLECTOR_0");
        if(first != null && first.startsWith("Grupo Ecológico")){
            team.setTitleCache(first, true);
            return team;
        }else{
            makeCollector(state, 0, team, record, row);
            makeCollector(state, 1, team, record, row);
            makeCollector(state, 2, team, record, row);
            makeCollector(state, 3, team, record, row);
            makeCollector(state, 4, team, record, row);
            if (team.getTeamMembers().size() == 0){
                return null;
            }else if (team.getTeamMembers().size() == 1){
                return team.getTeamMembers().get(0);
            }else{
                return team;
            }
        }
    }

    private void makeCollector(SalvadorSpecimenImportState state,
            int collectorNo, Team team, Map<String, String> record, int row) {

        String str = record.get("COLLECTOR_" + collectorNo);
        if (str == null){
            return;
        }else{
            parsePerson(state, str, team, row);
        }
        return ;
    }

    /**
     * @param str
     * @param team
     * @param row
     * @param importResult
     */
    private void parsePerson(SalvadorSpecimenImportState state, String str, Team team, int row) {
        Person result = Person.NewInstance();
        String regEx = "(.*),(([A-Z]\\.)+(\\sde)?)";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);

        String noInitials = "(Campo|Chinchilla|Campos|Claus|Desconocido|Fomtg|Huezo|Martínez|"
                + "Quezada|Romero|Ruíz|Sandoval|Serrano|Vásquez|Cabrera|Calderón)";

        if (matcher.matches()){
            String familyname = matcher.group(1);
            result.setFamilyName(familyname);
            String initials = matcher.group(2);
            result.setInitials(initials);
        }else if (str.matches(noInitials)){
            result.setFamilyName(str);
        }else if (str.matches("Martínez, F. de M.")){
            result.setFamilyName("Martínez");
            result.setInitials("F. de M.");
        }else if (str.equals("et al.")){
            team.setHasMoreMembers(true);
            return;
        }else if (str.startsWith("Grupo Ecológico")){
            result.setFamilyName(str);
        }else{
            String message = "Collector did not match pattern: " + str;
            state.getResult().addWarning(message, row);
            result.setTitleCache(str, true);
        }
        result = (Person)dedupHelper.getExistingAuthor(null, result);

        team.addTeamMember(result);
        return ;
    }


    @Override
    protected void refreshTransactionStatus(SalvadorSpecimenImportState state) {
        super.refreshTransactionStatus(state);
        collectionMap = new HashMap<>();
        fieldUnitMap = new HashMap<>();
        taxonDescMap = new HashMap<>();
        dedupHelper.restartSession(this, state.getResult());
    }



}
