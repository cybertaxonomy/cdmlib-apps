// $Id$
/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.bogota;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.joda.time.Partial;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTerm;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.DeterminationEvent;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.DeterminationModifierParser;
import eu.etaxonomy.cdm.strategy.parser.SpecimenTypeParser;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @date 21.04.2017
 *
 */
@Component
public class BogotaSpecimenImport<CONFIG extends BogotaSpecimenImportConfigurator>
        extends SimpleExcelSpecimenImport<CONFIG> {

    private static final long serialVersionUID = -884838817884874228L;
    private static final Logger logger = Logger.getLogger(BogotaSpecimenImport.class);

    private static final String COL_TAXON_UUID = "Platform Name ID = cdmID";
    private static final String COL_VOUCHER_ID = "Voucher ID";

    private static final String COL_FAMILY = "Family";
    private static final String COL_GENUS = "Genus";
    private static final String COL_SPECIFIC_EPI = "Specific Epithet";
    private static final String COL_BASIONYM_AUTHOR = "Author in parenthesis";
    private static final String COL_AUTHOR = "Author";
    private static final String COL_IDENTIFIER = "Identifier";
    private static final String COL_IDENTIFICATION_DATE = "Identification date";
    private static final String COL_IDENTIFICATION_QUALIFIER = "Qualifier";
    private static final String COL_TYPUS = "Type";
    private static final String COL_IDENTIFICATION_HISTORY = "Identification history";
    private static final String COL_COLLECTOR_VERBATIM = "Verbatim Collectors  (Originalfeld JBB)";
    private static final String COL_COLLECTOR_LASTNAME = "Primary Collector Last Name  (Originalfeld JBB)";
    private static final String COL_COLLECTOR_FIRSTNAME = "Primary Collector First Name Initial (Originalfeld JBB)";
    private static final String COL_COLLECTOR_MIDDLENAME = "Primary Collector Middle Name Initial (Originalfeld JBB)";
    private static final String COL_COLLECTOR_TYPE = "Primary collector type (Originalfeld JBB)";
    private static final String COL_COLLECTOR_NUMBER = "Collector's No";
    private static final String COL_COLLECTORS = "Collectors";
    private static final String COL_COLLECTION_DATE_FROM = "Collection Date from";
    private static final String COL_COLLECTION_DATE_TO = "Collection Date to";
    private static final String COL_ALTITUDE_FROM = "Altitude Value from";
    private static final String COL_ALTITUDE_TO = "Altitude Value to";
    private static final String COL_ALTITUDE_UNIT = "Altitude Unit";
    private static final String COL_LOCALITY = "Locality";
    private static final String COL_LOCALITY_ID = "LocalityID";
    private static final String COL_LATITUDE = "Latitude";
    private static final String COL_LONGITUDE = "Longitude";
    private static final String COL_ERROR_DISTANCE = "Error distance in m";
    private static final String COL_COUNTRY = "Country";
    private static final String COL_STATE_AREA = "State/Province/Greater Area";
    private static final String COL_GEO_METHOD = "Geocode Method";
    private static final String COL_HABITUS = "Habitus";
    private static final String COL_COLLECTION = "[Series] Voucher location";

    private static final UUID uuidAnonymous = UUID.fromString("2303f043-6e92-4afa-9082-7719e78a1c8a");
    private static final UUID uuidBogota = UUID.fromString("95b6cb03-8452-4439-98bd-8c1aa3c1da4e");
    private static final UUID uuidDefaultGeocodMethod = UUID.fromString("0983e680-b0ca-4e46-8df7-0f1d757a2e01");
    private static final UUID uuidExtTypeIdentificationHistory = UUID.fromString("7cee5c29-e16b-4e6f-ad57-bf7044259375");
    private static final UUID uuidDetQualVelAff = UUID.fromString("511a0c23-2646-4035-b570-36bdc2eb5557");

//    @SuppressWarnings("unchecked")
    private ImportDeduplicationHelper<SimpleExcelSpecimenImportState<?>> deduplicationHelper;
//           = (ImportDeduplicationHelper<SimpleExcelSpecimenImportState<?>>)ImportDeduplicationHelper.NewStandaloneInstance();


    @Override
    protected String getWorksheetName() {
        return "To be imported";
    }

//    private boolean isFirst = true;
//    private TransactionStatus tx = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelSpecimenImportState<CONFIG> state) {
//        if (isFirst){
//            tx = this.startTransaction();
//            isFirst = false;
//        }

        HashMap<String, String> record = state.getOriginalRecord();

        String voucherId = getValue(record, COL_VOUCHER_ID);
        if (!isInInterval(state)){
            return;
        }
        String line = state.getCurrentLine() + " (id:"+ voucherId+"): ";
        if (state.getCurrentLine() % 100 == 0){System.out.println(line);}
        try {

            //species
            TaxonBase<?> taxonBase = getOrCreateTaxon(state, line, record, voucherId);

            if (taxonBase != null){
                Taxon taxon = getTaxon(taxonBase);

                TaxonDescription taxonDescription = getTaxonDescription(state, line, taxon);

                DerivedUnit specimen = makeSpecimen(state, line, record, voucherId, taxonBase);

                IndividualsAssociation indAssoc = IndividualsAssociation.NewInstance(specimen);
                indAssoc.addImportSource(voucherId, COL_VOUCHER_ID, getSourceCitation(state), null);
                taxonDescription.addElement(indAssoc);

            }
        } catch (Exception e) {
            state.getResult().addError("An unexpected exception appeared in record", e, null, line);
            e.printStackTrace();
        }

    }


    /**
     * @param state
     * @return
     */
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


    /**
     * @param state
     * @param line
     * @param taxon
     * @return
     */
    private TaxonDescription getTaxonDescription(SimpleExcelSpecimenImportState<CONFIG> state, String line,
            Taxon taxon) {
        Reference ref = getSourceCitation(state);
        TaxonDescription desc = this.getTaxonDescription(taxon, ref, ! IMAGE_GALLERY, ! CREATE);
        if (desc == null){
            //TODO move title creation into base method
            desc = this.getTaxonDescription(taxon, ref, ! IMAGE_GALLERY, CREATE);
            desc.setTitleCache("Specimen Excel import for " +  taxon.getName().getTitleCache(), true);
        }
        return desc;
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param voucherId
     * @return
     */
    private DerivedUnit makeSpecimen(SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record, String voucherId, TaxonBase<?> taxonBase) {

        DerivedUnitFacade facade = DerivedUnitFacade.NewPreservedSpecimenInstance();
        facade.setAccessionNumber(voucherId);
        makeDetermination(facade.innerDerivedUnit(), state, line, record, taxonBase.getName());
        makeTypus(facade.innerDerivedUnit(), state, line, record, taxonBase.getName());
        makeCollectorFields(facade, state, line, record);
        makeLocationFields(facade, state, line, record);
        makeHabitus(facade, state, line, record);
        makeCollection(facade, state, line, record);
        DerivedUnit specimen = facade.innerDerivedUnit();
        specimen.addSource(makeOriginalSource(state));
        FieldUnit fieldUnit = facade.innerFieldUnit();
        fieldUnit.addSource(makeOriginalSource(state));
        return specimen;
    }


    /**
     * @param innerDerivedUnit
     * @param state
     * @param line
     * @param record
     * @param name
     */
    private void makeTypus(DerivedUnit specimen, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record, TaxonName name) {
        String typus = record.get(COL_TYPUS);
        if (typus != null){
            SpecimenTypeDesignationStatus status;
            try {
                status = SpecimenTypeParser.parseSpecimenTypeStatus(typus);
                SpecimenTypeDesignation designation = SpecimenTypeDesignation.NewInstance();
                designation.setTypeStatus(status);
                name.addSpecimenTypeDesignation(specimen, status, null, null, null, false, false);
            } catch (UnknownCdmTypeException e) {
                state.getResult().addWarning("Type designation could not be parsed", null, line);
            }
        }
    }


    /**
     * @param facade
     * @param state
     * @param line
     * @param record
     */
    private void makeCollection(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state,
            String line, HashMap<String, String> record) {
        String strCollection = record.get(COL_COLLECTION);
        String collectionFormat = ".*\\([A-Z]{2,4}\\)";
        if (!strCollection.matches(collectionFormat)){
            String message = "Voucher location format does not match the expected format. Voucher '(" + strCollection + ")' location not added.";
            state.getResult().addError(message, null, line);
            return;
        }
        String[] splits = strCollection.split("\\(");
        String collectionName = splits[0];
        String collectionCode = splits[1].replace(")", "");
        Collection collection = Collection.NewInstance();
        collection.setName(collectionName);
        collection.setCode(collectionCode);
        collection = getDeduplicationHelper(state).getExistingCollection(state, collection);
        facade.setCollection(collection);
    }


    /**
     * @param state
     * @return
     */
    private ImportDeduplicationHelper<SimpleExcelSpecimenImportState<?>> getDeduplicationHelper(SimpleExcelSpecimenImportState<CONFIG> state) {
        if (deduplicationHelper == null){
            deduplicationHelper = ImportDeduplicationHelper.NewInstance(this, state);
        }
        return deduplicationHelper;
    }


    /**
     * @param facade
     * @param state
     * @param line
     * @param record
     */
    private void makeHabitus(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record) {
        String habitus = record.get(COL_HABITUS);
        if (habitus != null){
            facade.setPlantDescription(habitus);
        }
    }


    /**
     * @param facade
     * @param state
     * @param line
     * @param record
     * @param voucherId
     */
    private void makeLocationFields(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record) {
        //Altitude
        String strAltitudeFrom = record.get(COL_ALTITUDE_FROM);
        String strAltitudeTo = record.get(COL_ALTITUDE_TO);
        Integer intAltitudeFrom = intFromString(state, strAltitudeFrom, line, COL_ALTITUDE_FROM);
        Integer intAltitudeTo = intFromString(state, strAltitudeTo, line, COL_ALTITUDE_TO);
        if (intAltitudeFrom != null){
            facade.setAbsoluteElevation(intAltitudeFrom);
            if (!intAltitudeFrom.equals(intAltitudeTo)){
                facade.setAbsoluteElevationMax(intAltitudeTo);
            }
            if (!record.get(COL_ALTITUDE_UNIT).equals("m")){
                state.getResult().addWarning("Altitude unit is not m but " + record.get(COL_ALTITUDE_UNIT), "makeLocationFields", line);
            }
        }
        checkNoToIfNoFrom(strAltitudeFrom, strAltitudeTo, state, line, COL_ALTITUDE_TO);

        //locality
        String locality = record.get(COL_LOCALITY);
        if (locality != null){  //should always exist
            facade.setLocality(locality, Language.SPANISH_CASTILIAN());
        }

        //Lat + Long
        String strLatitude = record.get(COL_LATITUDE);
        String strLongitude = record.get(COL_LONGITUDE);
        String strError = record.get(COL_ERROR_DISTANCE);
        Double dblLatitude = doubleFromString(state, strLatitude, line, COL_LATITUDE);
        Double dblLongitude = doubleFromString(state, strLongitude, line, COL_LONGITUDE);
        Integer intError = intFromString(state, strError, line, COL_ERROR_DISTANCE);
        ReferenceSystem referenceSystem = makeReferenceSystem(state, record, line);

        if (dblLatitude != null || dblLongitude != null || intError != null){ //should always exist
            Point exactLocation = Point.NewInstance(dblLongitude, dblLatitude, referenceSystem, intError);
            facade.setExactLocation(exactLocation);
        }

        //Country
        String strCountry = record.get(COL_COUNTRY);
        if (strCountry != null){
            if (strCountry.equals("Colombia")){
                Country colombia = Country.COLOMBIAREPUBLICOF();
                colombia.setLabel("Colombia");
                getTermService().saveOrUpdate(colombia);
                facade.setCountry(colombia);
            }else{
                state.getResult().addWarning("Country was not Colombia as expected but " +  strCountry,
                        "makeLocationFields", line);
            }
        }

        //State
        String strStateArea = record.get(COL_STATE_AREA);
        if (strStateArea != null){
            if (strStateArea.replaceAll("\\s", "").equalsIgnoreCase("Bogotá,D.C.")){
                NamedArea bogota = makeBogota(state, line);
                facade.addCollectingArea(bogota);
            }else{
                state.getResult().addWarning(COL_STATE_AREA + " was not 'Bogotá,  D.C.' as expected but " +  strCountry,
                        "makeLocationFields", line);
            }
        }
    }


    /**
     * @param strAltitudeFrom
     * @param strAltitudeTo
     * @param state
     * @param line
     * @param colAltitudeTo
     */
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

    private ReferenceSystem defaultGeocodeMethod;

    /**
     * @param state
     * @param record
     * @param line
     * @return
     */
    private ReferenceSystem makeReferenceSystem(SimpleExcelSpecimenImportState<CONFIG> state,
            Map<String, String> record, String line) {
        String defaultStrRefSys = "Wieczorek, J., Guo, Q., & Hijmans, R. (2004). The point-radius method for georeferencing locality descriptions and calculating associated uncertainty. International journal of geographical information science, 18(8), 745-767.; Escobar D, Díaz SR, Jojoa LM, Rudas E, Albarracín RD, Ramírez C, Gómez JY, López CR, Saavedra J (2015). Georreferenciación de localidades: Una guía de referencia para colecciones biológicas. Instituto de Investigación de Recursos Biológicos Alexander von Humboldt – Instituto de Ciencias Naturales, Universidad Nacional de Colombia. Bogotá D.C., Colombia. 95 p.";
        String strRefSys = record.get(COL_GEO_METHOD);
        if (strRefSys == null){
            return null;
        }else if (!strRefSys.equals(defaultStrRefSys)){
            state.getResult().addError("The expected Geocode Method is not the expected default method. Geocode Method was not added.", null, line);
            return null;
        }else if (defaultGeocodeMethod != null){
            return defaultGeocodeMethod;
        }else{
            String label = "Point radius method";
            String description = defaultStrRefSys;
            String labelAbbrev = "PRM";
            defaultGeocodeMethod = getReferenceSystem(state, uuidDefaultGeocodMethod,
                    label, description, labelAbbrev, null);
            return defaultGeocodeMethod;
        }
    }

    private NamedArea bogota;
    /**
     * @param state
     * @param line
     * @return
     */
    private NamedArea makeBogota(SimpleExcelSpecimenImportState<CONFIG> state, String line) {
        if (bogota != null){
            return bogota;
        }else{
            String label = "Bogotá, D.C.";
            NamedAreaType areaType = NamedAreaType.ADMINISTRATION_AREA();
            NamedAreaLevel level = NamedAreaLevel.STATE();
            bogota = getNamedArea(state, uuidBogota, label, label, null, areaType,
                    level, null, null, null);
            return bogota;
        }
    }


    /**
     * @param facade
     * @param state
     * @param line
     * @param record
     */
    private void makeCollectorFields(DerivedUnitFacade facade, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record) {

        //collector number
        facade.setFieldNumber(record.get(COL_COLLECTOR_NUMBER));

        //gathering date
        String dateFrom = unknownToNull((record.get(COL_COLLECTION_DATE_FROM)));
        String dateTo = unknownToNull(record.get(COL_COLLECTION_DATE_TO));
        checkNoToIfNoFrom(dateFrom, dateTo, state, line, COL_COLLECTION_DATE_TO);
        try {
            if (dateFrom != null && dateFrom.equals(dateTo)){
                dateTo = null;
            }
            TimePeriod gatheringPeriod = TimePeriodParser.parseEnglishDate(dateFrom, dateTo);
            facade.setGatheringPeriod(gatheringPeriod);
        } catch (Exception e) {
            state.getResult().addError("Error creating gathering date", e, null, line);
        }

        //collector
        String collectorType = record.get(COL_COLLECTOR_TYPE);
        String collectors = record.get(COL_COLLECTORS);
        AgentBase<?> collector;
        if (collectorType.startsWith("Anonymous")){
            collector = getAnonymous();
        }else if (collectorType.equals("Brother") || collectorType.equals("Person")){
            Person person = Person.NewInstance();
            if (collectorType.equals("Person")){
                person.setLastname(record.get(COL_COLLECTOR_LASTNAME));
                String initials = CdmUtils.concat("", record.get(COL_COLLECTOR_FIRSTNAME), record.get(COL_COLLECTOR_MIDDLENAME));
                initials = (initials == null)? null : initials.replaceAll("\\s", "");
                person.setInitials(initials);
                String full = person.getTitleCache();
                if (!full.equals(collectors)){
                    person.setTitleCache(collectors, true);
                    //TODO use setCollectorTitle in future
                }
            }else{
                person.setTitleCache(collectors, true);
                person.setPrefix("Hno.");
                person.setFirstname(collectors.replace("Hno.", "").trim());
            }
            collector = person;
        }else if (collectorType.equals("Group")){
            collector = Team.NewTitledInstance(collectors, collectors);
        }else if (collectorType.equals("Institution")){
            collector = Institution.NewNamedInstance(collectors);
        }else{
            String message = "Collector type " + collectorType + " not yet supported by import. Collector not added.";
            state.getResult().addError(message, null, line);
            collector = null;
        }
        collector = getDeduplicationHelper(state).getExistingAgent(state, collector);
        facade.setCollector(collector);
    }


    /**
     * @param string
     * @return
     */
    private String unknownToNull(String string) {
        if (string == null || string.equalsIgnoreCase("unknown")){
            return null;
        }else{
            return string;
        }
    }

    private Person anonymous;
    private Person getAnonymous() {
        if (anonymous != null){
            return anonymous;
        }
        anonymous = CdmBase.deproxy(getAgentService().find(uuidAnonymous), Person.class);
        if (anonymous == null){
            anonymous = Person.NewTitledInstance("Anon.");
            anonymous.setUuid(uuidAnonymous);
            getAgentService().save(anonymous);
        }
        return anonymous;
    }


    /**
     * @param facade
     * @param state
     * @param line
     * @param record
     * @param taxonBase
     */
    private void makeDetermination(DerivedUnit specimen, SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record, TaxonName taxonName) {

        DeterminationEvent determination;
        determination = DeterminationEvent.NewInstance(taxonName, specimen);
        determination.setPreferredFlag(true);

        //determiner/identifier
        TeamOrPersonBase<?> determiner = makeDeterminer(state, record, line);
        determination.setDeterminer(determiner);

        //date
        TimePeriod date = makeIdentificationDate(state, record, line);
        determination.setTimeperiod(date);

        //qualifier
        DefinedTerm qualifier = makeDeterminationQualifier(state, record, line);
        determination.setModifier(qualifier);

        //history
        String history = record.get(COL_IDENTIFICATION_HISTORY);
        if (history != null){
            String label = "Identification History";
            String text = label;
            ExtensionType detHistoryType = getExtensionType(state, uuidExtTypeIdentificationHistory, label, text, null);
            specimen.addExtension(history, detHistoryType);
        }
    }


    /**
     * @param state
     * @param record
     * @param line
     * @return
     */
    private TeamOrPersonBase<?> makeDeterminer(SimpleExcelSpecimenImportState<CONFIG> state,
            HashMap<String, String> record, String line) {
        String identifier = record.get(COL_IDENTIFIER);
        if (identifier == null){
            return null;
        }else if (identifier.equals("Anon.")){
            return getAnonymous();
        }else{
            Person person = Person.NewInstance();
            person.setTitleCache(identifier, true);

            String[] splits = identifier.split("\\.");
            int length = splits.length;
            if (splits[length - 1].equals("")){
                splits[length - 2]= splits[length - 2]+".";
                length--;
            }
            if (splits[length - 1].startsWith("-")){
                splits[length - 2]= splits[length - 2]+"." + splits[length - 1];
                length--;
            }
            String lastName = splits[length - 1];
            String initials = null;
            for (int i= 0; i < length-1;i++){
                initials = CdmUtils.concat("", initials, splits[i]+".");
            }
            person.setLastname(lastName);
            person.setInitials(initials);
            TeamOrPersonBase<?> result = getDeduplicationHelper(state).getExistingAuthor(state, person);
            return result;
        }
    }


    /**
     * @param state
     * @param record
     * @param line
     * @return
     */
    private TimePeriod makeIdentificationDate(SimpleExcelSpecimenImportState<CONFIG> state,
            HashMap<String, String> record, String line) {
        String strDate = record.get(COL_IDENTIFICATION_DATE);
        if (strDate == null || strDate.equals("s.n.")){
            return null;
        }
        String[] splits = strDate.split("/");
        String strYear = splits[splits.length-1];
        String strMonth = splits.length < 2? null:splits[splits.length-2];
        String strDay = splits.length < 3? null:splits[splits.length-3];

        Integer year = intFromString(state, strYear, line, COL_IDENTIFICATION_DATE);
        Integer month = intFromString(state, strMonth, line, COL_IDENTIFICATION_DATE);
        Integer day = intFromString(state, strDay, line, COL_IDENTIFICATION_DATE);
        Partial start = TimePeriodParser.makePartialFromDateParts(year, month, day);
        return TimePeriod.NewInstance(start);
    }


    /**
     * @param state
     * @param record
     * @param line
     * @return
     */
    private DefinedTerm makeDeterminationQualifier(SimpleExcelSpecimenImportState<CONFIG> state,
            HashMap<String, String> record, String line) {
        String qualifier = record.get(COL_IDENTIFICATION_QUALIFIER);
        if (qualifier != null){
            try {
                return DeterminationModifierParser.parseDeterminationQualifier(qualifier);
            } catch (UnknownCdmTypeException e) {
                //TODO add to terms
                if (qualifier.equals("vel. aff.")){

                    DefinedTerm velAff = (DefinedTerm)getTermService().find(uuidDetQualVelAff);
                    if (velAff == null){
                        velAff = DefinedTerm.NewModifierInstance(qualifier, qualifier, qualifier);
                        velAff.setUuid(uuidDetQualVelAff);
                        getTermService().save(velAff);
                    }
                    return velAff;
                }
                state.getResult().addError("Determination qualifier could not be recognized: " + qualifier, null, line);
                return null;
            }
        }else{
            return null;
        }
    }


    /**
     * @param taxonBase
     * @return
     */
    private Taxon getTaxon(TaxonBase<?> taxonBase) {
        if (taxonBase.isInstanceOf(Synonym.class)){
            return CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
        }else{
            return CdmBase.deproxy(taxonBase, Taxon.class);
        }
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private TaxonBase<?> getOrCreateTaxon(SimpleExcelSpecimenImportState<CONFIG> state, String line,
            HashMap<String, String> record, String noStr) {

        String strUuidTaxon = record.get(COL_TAXON_UUID);
        if (strUuidTaxon != null){
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
            TaxonName taxonName = null;
            Reference sec = null;
            Taxon result = Taxon.NewInstance(taxonName, sec);
            result.addSource(makeOriginalSource(state));
            //TODO export uuid

//            state.getResult().addInfo("Taxon");
            //TODO
            return null;
        }
    }


    @Override
    protected void secondPass(SimpleExcelSpecimenImportState<CONFIG> state) {
//        if (tx != null){
//            this.commitTransaction(tx);
//            tx = null;
//        }
    }

    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelSpecimenImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance(getValue(state.getOriginalRecord(), COL_VOUCHER_ID), COL_VOUCHER_ID, getSourceCitation(state));
    }

    /**
     * @param state
     * @return
     */
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
