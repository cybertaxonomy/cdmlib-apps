/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.bogota.SimpleExcelSpecimenImport;
import eu.etaxonomy.cdm.io.bogota.SimpleExcelSpecimenImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.DescriptionType;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.DeterminationEvent;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.CollectorParser;
import eu.etaxonomy.cdm.strategy.parser.ParserResult;
import eu.etaxonomy.cdm.strategy.parser.SpecimenTypeParser;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Import for the Centaurea specimen import format (Excel).
 *
 * @author a.mueller
 * @since 21.10.2025
 */
@Component
public class CentaureaSpecimenImport<CONFIG extends CentaureaSpecimenImportConfigurator>
        extends SimpleExcelSpecimenImport<CONFIG> {

    private static final long serialVersionUID = -1770218830403823493L;

    private static final Logger logger = LogManager.getLogger();

    private static final String COL_TAXON_UUID = "UUID";
    private static final String COL_BASIS_OF_RECORD = "BasisOfRecord";
    private static final String COL_ISO_COUNTRY = "ISOCountry";
    private static final String COL_COUNTRY = "Country";
    private static final String COL_COLLECTION_DATE = "CollectionDate";
    private static final String COL_COLLECTION_DATE_END = "CollectionDateEnd";
    private static final String COL_COLLECTOR = "Collector";
    private static final String COL_COLLECTORS_NUMBER = "CollectorsNumber";
    private static final String COL_LOCALITY_TOTAL = "Locality Total (Major Location + Locality)";
    private static final String COL_LATITUDE = "Latitude";

    private static final String COL_LONGITUDE = "Longitude";
    private static final String COL_ERROR_RADIUS = "ErrorRadius";
//    private static final String COL_REFERENCE_SYSTEM = "ReferenceSystem";
    private static final String COL_ABSOLUTE_ELEVATION = "AbsoluteElevation";
    private static final String COL_ECOLOGY = "Ecology";
    private static final String COL_PLANT_DESCRIPTION = "PlantDescription";

    private static final String COL_COLLECTION = "Collection";
    private static final String COL_COLLECTION_CODE = "CollectionCode";
    private static final String COL_ACCESSION_NUMBER = "AccessionNumber";
    private static final String COL_BARCODE = "Barcode";

//    private static final String COL_FULLNAME = "FullName";
    private static final String COL_FAMILY = "Family";
    private static final String COL_GENUS = "Genus";
    private static final String COL_SPECIFIC_EPI = "SpecificEpithet";
    private static final String COL_INFRA_SPECIFIC_EPI = "InfraspecificEpithet";
    private static final String COL_FULL_AUTHOR = "Author";

    private static final String COL_DETERMINATION_BY = "DeterminationBy";
    private static final String COL_DETERMINATION_DATE = "DeterminationDate";
    private static final String COL_DETERMINATION_MODIFIER = "DeterminationModifier";
    private static final String COL_TYPUS = "TYPUS";

    private static final UUID uuidDetermQualifierDet = UUID.fromString("3a7b0df6-f89d-46df-b13e-6dad83ce0e75");
    private static final UUID uuidDetermQualifierConfirm = UUID.fromString("448eae75-3c2a-49c7-a704-087b80ef7760");
    private static final UUID uuidDetermQualifierRev = UUID.fromString("cfcbfbe0-35bc-47d5-a164-1b6a24416347");

    private final Map<String, TaxonNode> taxonNodeMap = new HashMap<>();
    private final Map<String, FieldUnit> fieldUnitMap = new HashMap<>();

    @SuppressWarnings("unused")
    private Reference secRef;

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "Specimens";
    }

    @Override
    protected void firstPass(SimpleExcelSpecimenImportState<CONFIG> state) {

        Map<String, String> record = state.getOriginalRecord();

//        String voucherId = state.getCurrentLine().toString(); //getValue(record, COL_VOUCHER_ID);
        if (!isInInterval(state)){
            return;
        }
        String line = state.getCurrentLine() + ""; // + " (id:"+ voucherId+"): ";
        if (state.getCurrentLine() % 100 == 0){System.out.println(line);}
        try {

            //species
            TaxonBase<?> taxonBase = getTaxon(state, record, line);
            if (taxonBase != null){
                handleRecordForTaxon(state, line, taxonBase);
            }else {
                logger.warn(line + "Taxon could not be found.");
//                taxonBase = getOrCreateNewTaxon(state, record, line);
//                handleRecordForTaxon(state, voucherId, line, taxonBase);
            }

        } catch (Exception e) {
            state.getResult().addError("An unexpected exception appeared in record", e, null, line);
            e.printStackTrace();
        }
    }

    private Taxon getTaxon(SimpleExcelSpecimenImportState<CONFIG> state,
            Map<String, String> record, String line) {

        //not needed as long as all taxa are from the same family
//        String familyStr = record.get(COL_FAMILY);
        String genusStr = record.get(COL_GENUS);
        String specEpiStr = record.get(COL_SPECIFIC_EPI);
        String infraSpecEpiStr = record.get(COL_INFRA_SPECIFIC_EPI);
        String pureNameStr = CdmUtils.concat(" ", genusStr, specEpiStr, infraSpecEpiStr);
        if (pureNameStr == null) {
            return null;
        }
        TaxonName name = TaxonNameFactory.PARSED_BOTANICAL(pureNameStr);
        name.setAuthorshipCache(record.get(COL_FULL_AUTHOR));
        String fullName = name.getTitleCache();
        name = TaxonNameFactory.PARSED_BOTANICAL(fullName);

        initTaxonMap(state);

        if (name.isProtectedTitleCache()){
            state.getResult().addWarning("Name not parsable: " +  fullName);
        }
        if (taxonNodeMap.get(fullName) == null){
            state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
            name.addSource(makeOriginalSource(state));
            state.getResult().addError("Non existing taxa not yet implemented: " + name.getTitleCache() , null, "getTaxon()", line);
            //TODO 8 new names/taxa not yet implemented, but currently not needed
            return null;
        }else {
            TaxonNode taxonNode = taxonNodeMap.get(fullName);
            return taxonNode.getTaxon();
        }
      }

    private void initTaxonMap(SimpleExcelSpecimenImportState<CONFIG> state) {
        if (taxonNodeMap.isEmpty()){
            List<String> propertyPaths = Arrays.asList(new String[]{"taxon.name"});
            TaxonNodeFilter filter = TaxonNodeFilter.NewClassificationInstance(state.getConfig().getClassificationUuid());
            List<Integer> idList = getTaxonNodeService().idList(filter);
            List<TaxonNode> list = getTaxonNodeService().loadByIds(idList, propertyPaths);
            for (TaxonNode node : list){
                if (node.getTaxon()!= null){
                    String strName = node.getTaxon().getName().getTitleCache();
                    TaxonNode existingNode = taxonNodeMap.get(strName);
                    if (existingNode != null){
                        state.getResult().addWarning("Taxon name exists more than once while initializing taxon map: " + strName, "initTaxonMap");
                    }else{
                        taxonNodeMap.put(strName, node);
                    }
                }
            }
        }
    }

    protected void handleRecordForTaxon(SimpleExcelSpecimenImportState<CONFIG> state,
            String line, TaxonBase<?> taxonBase) {

        Map<String, String> record = state.getOriginalRecord();
        Taxon taxon = getTaxon(taxonBase);

        TaxonDescription taxonDescription = getTaxonDescription(state, line, taxon);
        taxonDescription.addType(DescriptionType.INDIVIDUALS_ASSOCIATION);

        DerivedUnit specimen = makeSpecimen(state, line, record, taxonBase);

        IndividualsAssociation indAssoc = IndividualsAssociation.NewSpecimenInstance(specimen);
        indAssoc.addImportSource(line, "Row", getSourceCitation(state), null);
        taxonDescription.addElement(indAssoc);
    }

    private boolean isInInterval(SimpleExcelSpecimenImportState<CONFIG> state) {
        Integer min = state.getConfig().getMinLineNumber();
        Integer max = state.getConfig().getMaxLineNumber();
        Integer current = state.getCurrentLine();
        if (current < min || current > max){
            return false;
        }else{
            return true;
        }
    }

    private TaxonDescription getTaxonDescription(SimpleExcelSpecimenImportState<CONFIG> state,
            @SuppressWarnings("unused") String line, Taxon taxon) {

        Reference ref = getSourceCitation(state);
        TaxonDescription desc = this.getTaxonDescription(taxon, ref, ! IMAGE_GALLERY, ! CREATE);
        if (desc == null){
            desc = this.getTaxonDescription(taxon, ref, ! IMAGE_GALLERY, CREATE);
            desc.setTitleCache("Specimen Excel import for " + taxon.getName().getTitleCache(), true);
            desc.addSource(makeOriginalSource(state));
            getDescriptionService().save(desc);
        }
        return desc;
    }

    private DerivedUnit makeSpecimen(SimpleExcelSpecimenImportState<CONFIG> state, String line,
            Map<String, String> record, TaxonBase<?> taxonBase) {

        String basisOfRecord = getValue(record, COL_BASIS_OF_RECORD);
        if (basisOfRecord != null && !basisOfRecord.trim().equals("PreservedSpecimen")) {
            String message = "Basis of record " + basisOfRecord + "; not yet handled. Record could not be imported.";
            state.addError(message);
            return null;
        }
        DerivedUnitFacade facade = DerivedUnitFacade.NewPreservedSpecimenInstance();
        String fieldUnitKey = makeFieldUnitKey(record);
        FieldUnit fu = fieldUnitMap.get(fieldUnitKey);
        if (fu != null) {
            facade.setFieldUnit(fu);
        }else {
            //gathering
            makeLocationFields(facade, state, line, record);
            //fieldUnit
            makeCollectorFields(facade, state, line, record);
            makeEcologyPlantDescription(facade, state, line, record);
            fieldUnitMap.put(fieldUnitKey, facade.innerFieldUnit());
        }

        //derivedUnit
        facade.setAccessionNumber(record.get(COL_ACCESSION_NUMBER));
        facade.setBarcode(record.get(COL_BARCODE));
        makeCollection(facade, state, line, record);
        if(state.getConfig().isDoDetermination()) {
            makeDetermination(facade.innerDerivedUnit(), state, line, record, taxonBase.getName());
        }
        makeTypus(facade.innerDerivedUnit(), state, line, record, taxonBase.getName());

        DerivedUnit specimen = facade.innerDerivedUnit();
        specimen.addSource(makeOriginalSource(state));
        FieldUnit fieldUnit = facade.innerFieldUnit();
        fieldUnit.addSource(makeOriginalSource(state));
        getOccurrenceService().save(specimen);
        return specimen;
    }

    /**
     * Creates a key which represents a unique field unit.
     * Needed for deduplication of field units.
     */
    private String makeFieldUnitKey(Map<String, String> record) {
        String result = CdmUtils.concat("-",
                getValue(record, COL_COLLECTOR),
                getValue(record, COL_COLLECTORS_NUMBER),
                getValue(record, COL_COLLECTION_DATE),
                getValue(record, COL_COLLECTION_DATE_END),
                getValue(record, COL_COUNTRY),
                getValue(record, COL_ISO_COUNTRY),
                getValue(record, COL_LOCALITY_TOTAL),
                getValue(record, COL_LATITUDE),
                getValue(record, COL_LONGITUDE),
                getValue(record, COL_ERROR_RADIUS),
//                getValue(record, COL_REFERENCE_SYSTEM),
                getValue(record, COL_ABSOLUTE_ELEVATION),
                getValue(record, COL_ECOLOGY),
                getValue(record, COL_PLANT_DESCRIPTION));
        return result;
    }

    private void makeTypus(DerivedUnit specimen, SimpleExcelSpecimenImportState<CONFIG> state,
            String line, Map<String, String> record, TaxonName name) {

        String typus = record.get(COL_TYPUS);
        if (StringUtils.isNotBlank(typus)){
            try {
                SpecimenTypeDesignationStatus status = SpecimenTypeParser.parseSpecimenTypeStatus(typus);
                SpecimenTypeDesignation designation = name.addSpecimenTypeDesignation(specimen, status, null, null, null, false, false);
                getNameService().saveTypeDesignation(designation);
            } catch (UnknownCdmTypeException e) {
                state.getResult().addWarning("Type designation could not be parsed", null, line);
            }
        }
    }

    private void makeCollection(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state,
            @SuppressWarnings("unused") String line, Map<String, String> record) {

        Collection collection = Collection.NewInstance();
        String strCollection = record.get(COL_COLLECTION);
        String strCollectionCode = record.get(COL_COLLECTION_CODE);
        collection.setName(strCollection);
        collection.setCode(strCollectionCode);
        collection = state.getDeduplicationHelper().getExistingCollection(collection);
        if (!collection.isPersisted()) {
            getCollectionService().save(collection);
        }
        facade.setCollection(collection);
    }

    private void makeEcologyPlantDescription(DerivedUnitFacade facade, @SuppressWarnings("unused") SimpleExcelSpecimenImportState<CONFIG> state, @SuppressWarnings("unused") String line,
            Map<String, String> record) {

        //plant description
        String habitus = record.get(COL_PLANT_DESCRIPTION);
        if (habitus != null){
            facade.setPlantDescription(habitus);
        }

        //ecology
        String ecology = record.get(COL_ECOLOGY);
        if (ecology != null) {
            facade.setEcology(ecology);
        }
    }

    private void makeLocationFields(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state,
            String line, Map<String, String> record) {

        makeCountry(facade, state, line, record);
        //TODO 9 maybe further parse location?
        facade.setLocality(record.get(COL_LOCALITY_TOTAL));

        String elevation = record.get(COL_ABSOLUTE_ELEVATION);
        if (StringUtils.isNotBlank(elevation)) {
            String[] elevations = elevation.split("-");
            String strAltitudeFrom = elevations[0];
            String strAltitudeTo = elevations.length > 1 ? elevations[1] : null;
            Integer intAltitudeFrom = intFromString(state, strAltitudeFrom, line, COL_ABSOLUTE_ELEVATION);
            Integer intAltitudeTo = intFromString(state, strAltitudeTo, line, COL_ABSOLUTE_ELEVATION);

            //altitude
            if (intAltitudeFrom != null){
                facade.setAbsoluteElevation(intAltitudeFrom);
                if (!intAltitudeFrom.equals(intAltitudeTo)){
                    facade.setAbsoluteElevationMax(intAltitudeTo);
                }
            }
        }

        //Lat + Long
        String strLatitude = record.get(COL_LATITUDE);
        String strLongitude = record.get(COL_LONGITUDE);
        String strErrorRadius = record.get(COL_ERROR_RADIUS);
        try {
            Double dblLatitude = Point.parseLatitude(strLatitude);
            Double dblLongitude = Point.parseLongitude(strLongitude);
            Integer intError = intFromString(state, strErrorRadius, line, COL_ERROR_RADIUS);
            ReferenceSystem referenceSystem = makeReferenceSystem(state, record, line);

            if (dblLatitude != null || dblLongitude != null || intError != null){ //should always exist
                Point exactLocation = Point.NewInstance(dblLongitude, dblLatitude, referenceSystem, intError);
                facade.setExactLocation(exactLocation);
            }
        } catch (ParseException e) {
            state.getResult().addWarning("Latitude or longiture could not be parsed", "makeLocationFields", line);
        }
    }

    private void makeCountry(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state,
            String line, Map<String, String> record) {

        //Country
        String strCountry = record.get(COL_COUNTRY);
        String strISOCountry = record.get(COL_ISO_COUNTRY);
        if (strCountry != null){
            if (strISOCountry.equals("GR")){
                verify(state, line, record, COL_COUNTRY, "Greece");
                facade.setCountry(Country.GREECEHELLENICREPUBLIC());
            } else if (strISOCountry.equals("DK")){
                verify(state, line, record, COL_COUNTRY, "Denmark");
                facade.setCountry(Country.DENMARKKINGDOMOF());
            } else if (strISOCountry.equals("TR")){
                verify(state, line, record, COL_COUNTRY, "Turkey");
                facade.setCountry(Country.TUERKIYEREPUBLICOF());
            }else{
                state.getResult().addWarning("Country not recognized: " +  strCountry,
                        "makeLocationFields", line);
            }
        }
    }

    private void verify(SimpleExcelSpecimenImportState<CONFIG> state, String line, Map<String, String> record, String attr, String compare) {
        if (!compare.equals(record.get(attr))) {
            String message = attr + " should be '" + compare + "' but was '" + record.get(attr) +"'";
            state.getResult().addWarning(message, null, line);
        }

    }

    private void checkNoToIfNoFrom(String strFrom, String strTo,
            SimpleExcelSpecimenImportState<CONFIG> state,
            String line, String toAttributeName) {
        if (isNotBlank(strTo) && isBlank(strFrom)){
            String message = "A min-max attribute has a max value (%s) but no min value. This is invalid."
                    + " The max value attribute name is %s.";
            message = String.format(message, strTo, toAttributeName);
            state.getResult().addWarning(message, null, line);
        }
    }

    private ReferenceSystem makeReferenceSystem(@SuppressWarnings("unused") SimpleExcelSpecimenImportState<CONFIG> state,
            @SuppressWarnings("unused") Map<String, String> record, @SuppressWarnings("unused") String line) {

        //Until now no reference system data available
        return null;

    }

    private void makeCollectorFields(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state,
            String line, Map<String, String> record) {

        //collector number
        facade.setFieldNumber(record.get(COL_COLLECTORS_NUMBER));

        //gathering date
        String dateFrom = unknownToNull((record.get(COL_COLLECTION_DATE)));
        String dateTo = unknownToNull(record.get(COL_COLLECTION_DATE_END));
        checkNoToIfNoFrom(dateFrom, dateTo, state, line, COL_COLLECTION_DATE_END);
        if (dateFrom != null && dateFrom.equals(dateTo)){
            dateTo = null;
        }
        if (isNotBlank(dateFrom)) {
            try {
                TimePeriod gatheringPeriod = TimePeriodParser.parseString(dateFrom);
                if (isNotBlank(dateTo)) {
                    TimePeriod toDate = TimePeriodParser.parseString(dateTo);
                    if (toDate.getStart() != null) {
                        gatheringPeriod.setEnd(toDate.getStart());
                    }else {
                        gatheringPeriod.setFreeText(dateFrom + " - " + dateTo );
                    }
                }
                facade.setGatheringPeriod(gatheringPeriod);
            } catch (Exception e) {
                state.getResult().addError("Error creating gathering date", e, null, line);
            }
        }

        //collector
        String collectorStr = record.get(COL_COLLECTOR);
        //TODO 6 allow pure family name collectors?
        CollectorParser collectorParser = CollectorParser.Instance();
        ParserResult<TeamOrPersonBase<?>> parserResult = collectorParser.parse(collectorStr);
        TeamOrPersonBase<?> collector = parserResult.getEntity();
        state.getResult().getParserResultMessages(parserResult);
        facade.setCollector(collector);

        collector = state.getDeduplicationHelper().getExistingAuthor(collector, false);
        saveUnpersisted(collector);
        facade.setCollector(collector);
    }

    private String unknownToNull(String string) {
        if (string == null || string.equalsIgnoreCase("unknown")){
            return null;
        }else{
            return string;
        }
    }

    private void makeDetermination(DerivedUnit specimen, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            Map<String, String> record, TaxonName taxonName) {

        DeterminationEvent determination = DeterminationEvent.NewInstance(taxonName, specimen);
        determination.setPreferredFlag(true);

        //determiner/identifier
        TeamOrPersonBase<?> determiner = makeDeterminationBy(state, record, line);
        determination.setDeterminer(determiner);

        //date
        TimePeriod date = makeIdentificationDate(state, record, line);
        determination.setTimeperiod(date);

        //qualifier
        DefinedTerm qualifier = makeDeterminationQualifier(state, record, line);
        determination.setModifier(qualifier);
    }

    private TeamOrPersonBase<?> makeDeterminationBy(SimpleExcelSpecimenImportState<CONFIG> state,
            Map<String, String> record, @SuppressWarnings("unused") String line) {

        String identifydBy = record.get(COL_DETERMINATION_BY);
        if (StringUtils.isBlank(identifydBy)){
            return null;
        }else{
            ParserResult<TeamOrPersonBase<?>> parseResult = CollectorParser.Instance().parse(identifydBy);
            TeamOrPersonBase<?> identifiedBy = parseResult.getEntity();
            state.getResult().getParserResultMessages(parseResult);

//            Person person = Person.NewInstance();
//            person.setTitleCache(identifier, true);
//
//            String[] splits = identifier.split("\\.");
//            int length = splits.length;
//            if (splits[length - 1].equals("")){
//                splits[length - 2]= splits[length - 2]+".";
//                length--;
//            }
//            if (splits[length - 1].startsWith("-")){
//                splits[length - 2]= splits[length - 2]+"." + splits[length - 1];
//                length--;
//            }
//            String familyName = splits[length - 1];
//            String initials = null;
//            for (int i= 0; i < length-1;i++){
//                initials = CdmUtils.concat("", initials, splits[i]+".");
//            }
//            person.setFamilyName(familyName);
//            person.setInitials(initials);

            //FIXME does this test on collectorTitle?
            TeamOrPersonBase<?> result = state.getDeduplicationHelper().getExistingAuthor(identifiedBy, false);
            saveUnpersisted(result);
            return result;
        }
    }

    private void saveUnpersisted(TeamOrPersonBase<?> author) {
        if (author != null && !author.isPersisted()) {
            getAgentService().save(author);
            if (author.isInstanceOf(Team.class)) {
                List<Person> members = CdmBase.deproxy(author, Team.class).getTeamMembers();
                members.forEach(m->saveUnpersisted(m));
            }
        }
    }

    private TimePeriod makeIdentificationDate(
            @SuppressWarnings("unused") SimpleExcelSpecimenImportState<CONFIG> state,
            Map<String, String> record, @SuppressWarnings("unused") String line) {

        String strDate = record.get(COL_DETERMINATION_DATE);
        if (StringUtils.isBlank(strDate) || strDate.equals("s.n.")){
            return null;
        }
        TimePeriod result = TimePeriodParser.parseString(strDate);
        return result;
    }

    private DefinedTerm makeDeterminationQualifier(SimpleExcelSpecimenImportState<CONFIG> state,
            Map<String, String> record, String line) {
        String qualifier = record.get(COL_DETERMINATION_MODIFIER);
        if (qualifier != null){
//            try {
//                return DeterminationModifierParser.parseDeterminationQualifier(qualifier);
//            } catch (UnknownCdmTypeException e) {
                //TODO 9 add determination type terms => #10839

            if (qualifier.equals("det.")){
                return this.getDeterminationModifier(state, uuidDetermQualifierDet, "determined", "determined", qualifier, null, Language.ENGLISH());
            } else if (qualifier.equals("confirm.")) {
                return this.getDeterminationModifier(state, uuidDetermQualifierConfirm, "confirmed", "confirmed", qualifier, null, Language.ENGLISH());
            } else if (qualifier.equals("rev.")) {
                return this.getDeterminationModifier(state, uuidDetermQualifierRev, "revised", "revised", qualifier, null, Language.ENGLISH());
            }
            state.getResult().addError("Determination qualifier could not be recognized: " + qualifier, null, line);
            return null;
        }else{
            return null;
        }
    }

    private Taxon getTaxon(TaxonBase<?> taxonBase) {
        if (taxonBase.isInstanceOf(Synonym.class)){
            return CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
        }else{
            return CdmBase.deproxy(taxonBase, Taxon.class);
        }
    }

    @SuppressWarnings("unused")
    private TaxonBase<?> getTaxonByCdmId(SimpleExcelSpecimenImportState<CONFIG> state, String line,
            Map<String, String> record, String noStr) {

        String strUuidTaxon = record.get(COL_TAXON_UUID);
        if (strUuidTaxon != null && ! state.getConfig().isOnlyNonCdmTaxa()){
            UUID uuidTaxon;
            try {
                uuidTaxon = UUID.fromString(strUuidTaxon);
            } catch (Exception e) {
                state.getResult().addError("Taxon uuid has incorrect format. Taxon could not be loaded. Data not imported.", null, line);
                return null;
            }
            TaxonBase<?> result = getTaxonService().find(uuidTaxon);
            if (result == null){
                state.getResult().addError("Taxon for uuid  "+strUuidTaxon+" could not be found in database. "
                        + "Taxon could not be loaded. Data not imported.", null, line);

            }
            return result;
        }else{
            return null;
        }
    }

    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelSpecimenImportState<CONFIG> state) {
        String line = state.getCurrentLine().toString();
        return IdentifiableSource.NewDataImportInstance(line, "Row", getSourceCitation(state));
    }

    protected Reference getSourceCitation(SimpleExcelSpecimenImportState<CONFIG> state) {
        Reference source = state.getConfig().getSourceReference();
        if (source.getId() == 0){
            Reference persisted = getReferenceService().find(source.getUuid());
            if (persisted == null){
                getReferenceService().saveOrUpdate(source);
            }else{
                state.getConfig().setSourceReference(persisted);
                source = persisted;
            }
        }
        return source;
    }
}