/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.media.ExternalLink;
import eu.etaxonomy.cdm.model.media.ExternalLinkType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 08.03.2023
 */
@Component
public class EuroMedEuroMossesImport<CONFIG extends EuroMedEuroMossesImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -6691694003401153408L;
    private static final Logger logger = LogManager.getLogger();

    private static final String TROPICOS_FULLNAME = "Tropicos FullName";
    private static final String ABBREV_TITLE = "OutputAbbreviatedTitle";
    private static final String IN_AUTHORS = "In-Authors";
    private static final String NAME_ID = "OutputNameID";
    private static final String REF_TYPE = "RefType (A=article, B=book, S=Booksection)";
    private static final String TITLE_YEAR = "OutputTitlePageYear";
    private static final String PAGE = "OutputPage";
    private static final String VOLUME = "OutputVolume";
    private static final String ISSUE = "OutputIssue";
    private static final String YEAR = "OutputYearPublished";
    private static final String NOM_STATUS = "OutputNomenclatureStatus";
    private static final String BHL_LINK = "OutputBHLLink";
    private static final String EUROP_REDLIST_STATUS = "European Red List status";
    private static final String ENDEMIC_EUR = "Endemic in Europe";

    @Override
    protected String getWorksheetName(CONFIG config) {
        if (config.isLiverwort()) {
            return "Liverwort Checklist";
        }else {
            return "Moss Checklist";
        }
    }

    private boolean isFirst = true;
    private TransactionStatus tx = null;
    private TaxonName lastSpecies = null;
    private Map<String,TaxonNode> genusNodeMap = new HashMap<>();

    private Map<String,NamedArea> areaMap;
    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
    private ImportDeduplicationHelper dedupHelper;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (isFirst){
            tx = this.startTransaction();
            isFirst = false;
        }
        getAreaMap();

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        TaxonName taxonName = makeName(state, line, record);
        Taxon taxon = Taxon.NewInstance(taxonName, getSecRef(state));
        taxon.setPublish(false);

        TaxonNode parent = findParentNode(state, taxonName, lastSpecies, genusNodeMap, line);
        if (taxonName.isSpecies()) {
            lastSpecies = taxonName;
        }

        if (parent == null) {
            return;
        }
        TaxonNode childNode = parent.addChildTaxon(taxon, null, null); //E+M taxon nodes usually do not have a citation
        getTaxonNodeService().saveOrUpdate(childNode);

        makeRedListStatus(state, taxon, record.get(EUROP_REDLIST_STATUS), line);
        makeEndemic(taxon, record.get(ENDEMIC_EUR), line);

        makeDistribution(state, line, taxon);
        addImportSource(state, taxon);
    }

    private void makeRedListStatus(SimpleExcelTaxonImportState<CONFIG> state, Taxon taxon, String statusStr, String line) {

        NamedArea europeArea = getEuropeArea();
        PresenceAbsenceTerm rlState = getRedListState(state, statusStr, europeArea, taxon.getName(), line);
        if (rlState == null) {
            return;
        }
        Distribution iucnDistribution = Distribution.NewInstance(europeArea, rlState);
        Feature iucnFeature = getFeature(state, uuidFeatureIucn);
        iucnDistribution.setFeature(iucnFeature);

        Reference source = getDistributionSource();
        iucnDistribution.addPrimaryTaxonomicSource(source);

        TaxonDescription description = getTaxonDescription(taxon, false, true);
        description.addElement(iucnDistribution);

    }

    private static final UUID uuidIucnLeastConcern = UUID.fromString("658580d8-78be-462b-bd03-cd1fc5625676");
    private static final UUID uuidIucnRegionalExtinct = UUID.fromString("bb52103b-78de-4a55-9c34-6f9e315d3783");
    private static final UUID uuidIucnCriticallyEndangered = UUID.fromString("d37660ac-9848-4008-af30-cf7c71962414");
    private static final UUID uuidIucnEndangered = UUID.fromString("be71cfb4-cbc1-4367-b0f7-91d5a38aa2ce");
    private static final UUID uuidIucnVulnerable = UUID.fromString("d3da6fa3-da31-4640-adb8-733f7016d85d");
    private static final UUID uuidIucnNearThreatened = UUID.fromString("a90e20f2-88db-43c6-a0d8-c4c4c83609d7");
    private static final UUID uuidIucnDataDeficient = UUID.fromString("8245e186-a632-484d-b51c-1a42403b43b1");
    private static final UUID uuidIucnNotEvaluated = UUID.fromString("bcc369dd-9af4-4fff-9fd2-6760a9428a6b");
    private static final UUID uuidIucnNotApplicable = UUID.fromString("34a8db19-1be1-49b7-aa1c-c97583a810a5");

    private static final UUID uuidIucnRare = UUID.fromString("2cdb003a-8907-47c9-b0a5-8544c0254f31");
    private static final UUID uuidIucnIndeterminate = UUID.fromString("03212757-506c-4499-8bf1-b4bf9313ad58");
    private static final UUID uuidIucnRiskAssumedd = UUID.fromString("29db707b-2cd3-4387-b163-d7974879bda3");
    private static final UUID uuidAreaLatvia = UUID.fromString("8e338882-2631-4ad6-bc53-799c698c807d");
    @SuppressWarnings("unused")
    private static final UUID uuidAreaLithuania = UUID.fromString("ecf200b6-b1df-414f-b215-edac503b1a65");
    private static final UUID uuidAreaGermany = UUID.fromString("f617ddea-51c9-4ef1-b7f4-ed22f871631d");
    private static final UUID uuidAreaAustria = UUID.fromString("ae65867c-00f6-406c-a315-b3e4cc9a93d2");
    private static final UUID uuidFeatureIucn = UUID.fromString("332c19f6-286f-4420-b9f2-fbb057908f65");
    private static final UUID uuidFeatureIucnAreas = UUID.fromString("0b17d701-4159-488e-a6e7-0045992e61af");

    private PresenceAbsenceTerm getRedListState(SimpleExcelTaxonImportState<CONFIG> state, String statusStr, NamedArea area, TaxonName name, String line) {

        if (isBlank(statusStr)|| "nm".equals(statusStr)) {
            return null;
        }else if (statusStr.matches("(●|■|□|LC)")) {
            return this.getPresenceTerm(state, uuidIucnLeastConcern);
        }else if (statusStr.matches("(RE|0)")) {
            return this.getPresenceTerm(state, uuidIucnRegionalExtinct);
        }else if (statusStr.matches("(CR|EB)")) {
            return this.getPresenceTerm(state, uuidIucnCriticallyEndangered);
        }else if (statusStr.matches("(EN|BE|Mn)")) {
            return this.getPresenceTerm(state, uuidIucnEndangered);
        }else if (statusStr.matches("(VU|KW)")) {
            return this.getPresenceTerm(state, uuidIucnVulnerable);
        }else if (statusStr.matches("(NT|GE)")) {
            return this.getPresenceTerm(state, uuidIucnNearThreatened);
        }else if (statusStr.matches("(DD|DD\\*)")) {       // TODO DD*
            return this.getPresenceTerm(state, uuidIucnDataDeficient);
        }else if (statusStr.matches("(NE)")) {
            return this.getPresenceTerm(state, uuidIucnNotEvaluated);
        }else if (statusStr.matches("(NA)")) {
            return this.getPresenceTerm(state, uuidIucnNotApplicable);
        }else if (statusStr.matches("(1)")) {
            if (area.getUuid().equals(uuidAreaLatvia)) {
                return this.getPresenceTerm(state, uuidIucnEndangered);
            }else {  //Lithuania, Germany, Austria
                return this.getPresenceTerm(state, uuidIucnCriticallyEndangered);
            }
        }else if (statusStr.matches("(2)")) {
            if (area.getUuid().equals(uuidAreaLatvia)) {
                return this.getPresenceTerm(state, uuidIucnVulnerable);
            }else if (area.getUuid().equals(uuidAreaAustria)) {
                return this.getPresenceTerm(state, uuidIucnCriticallyEndangered);
            }else {   //Lithuania, Germany
                return this.getPresenceTerm(state, uuidIucnEndangered);
            }
        }else if (statusStr.matches("(3)")) {
            if (area.getUuid().equals(uuidAreaLatvia)) {
                return this.getPresenceTerm(state, uuidIucnRare);
            }else if (area.getUuid().equals(uuidAreaAustria)) {
                return this.getPresenceTerm(state, uuidIucnEndangered);
            }else {//Lithuania, Germany
                return this.getPresenceTerm(state, uuidIucnVulnerable);
            }
        }else if (statusStr.matches("(4)")) {
            if (area.getUuid().equals(uuidAreaLatvia)) {
                return this.getPresenceTerm(state, uuidIucnDataDeficient);
            }else if (area.getUuid().equals(uuidAreaAustria)){
                return this.getPresenceTerm(state, uuidIucnRiskAssumedd);
            }else {  //Lithuania
                return this.getPresenceTerm(state, uuidIucnRare);
            }
        }else if (statusStr.matches("(G)")) {
            //Germany
            return this.getPresenceTerm(state, uuidIucnRiskAssumedd);
        }else if (statusStr.matches("(R)")) {
            //Germany, Poland, Ukraine
            return this.getPresenceTerm(state, uuidIucnRare);
        }else if (statusStr.matches("(V)")) {
            if (area.getUuid().equals(uuidAreaGermany)) {
                return this.getPresenceTerm(state, uuidIucnNearThreatened);
            }else {  //Poland
                return this.getPresenceTerm(state, uuidIucnVulnerable);
            }
        }else if (statusStr.matches("(D)")) {
            //Germany
            return this.getPresenceTerm(state, uuidIucnDataDeficient);
        }else if (statusStr.matches("(nb)")) {
            //Germany
            return this.getPresenceTerm(state, uuidIucnNotEvaluated);
        }else if (statusStr.matches("(nm)")) {
            //Germany
            return null;  //not mapped
        }else if (statusStr.matches("(E[xX])")) {
            //Poland
            return this.getPresenceTerm(state, uuidIucnRegionalExtinct);
        }else if (statusStr.matches("(E)")) {
            //Poland
            return this.getPresenceTerm(state, uuidIucnEndangered);
        }else if (statusStr.matches("(I)")) {
            //Poland
            return this.getPresenceTerm(state, uuidIucnIndeterminate);
        }else if (statusStr.matches("(\\?|-)")) {
            return null;   //no IUCN status
        }else {
            logger.warn(line + "IUCN status "+statusStr+" not yet handled for " + area.getTitleCache() + " and taxon " + name.getTitleCache());
            return null;
        }
    }

    private void makeEndemic(Taxon taxon, String endemicStr, String line) {

        PresenceAbsenceTerm status;
        if (isBlank(endemicStr)) {
            status = PresenceAbsenceTerm.NOT_ENDEMIC_FOR_THE_RELEVANT_AREA();
        }else if ("E?".equals(endemicStr)) {
            status = PresenceAbsenceTerm.ENDEMISM_UNKNOWN();
        }else if ("E (subsp.)".equals(endemicStr)) {
            status = PresenceAbsenceTerm.NOT_ENDEMIC_FOR_THE_RELEVANT_AREA();
        }else if ("E".equals(endemicStr)) {
            status = PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();
        }else {
            logger.warn(line + "Not yet handled status: " + endemicStr);
            status = null;
        }

        NamedArea europeArea = getEuropeArea();
        Distribution endemic = Distribution.NewInstance(europeArea, status);
        Reference distributionSource = getDistributionSource();
        endemic.addPrimaryTaxonomicSource(distributionSource);

        TaxonDescription description = getTaxonDescription(taxon, false, true);
        description.addElement(endemic);
    }

    private NamedArea getEuropeArea() {
        return areaMap.get("EUR");
    }

    private Reference refDistribution;
    private Reference getDistributionSource() {
        if (refDistribution == null) {
            UUID refUuid = UUID.fromString("83a84360-a00c-443a-8d5c-36c4cbdb1d54"); //Hodgetts et Lockhart 2020
            refDistribution = getReferenceService().find(refUuid);
            if(refDistribution == null){
                logger.warn("refDistribution not found!");
            }
        }
        return refDistribution;
    }

    private Reference refEuroMed;
    private Reference getSecRef(SimpleExcelTaxonImportState<CONFIG> state) {

        if (refEuroMed == null) {
            UUID secUuid = state.getConfig().getSecUuid();
            refEuroMed = getReferenceService().find(secUuid);
            if(refEuroMed == null){
                logger.warn("refEuroMed not found!");
            }
        }
        return refEuroMed;
    }

    private void addImportSource(SimpleExcelTaxonImportState<CONFIG> state, ISourceable<?> sourceable) {
        sourceable.addImportSource("row: "+state.getCurrentLine(), "Moss Checklist", getSourceReference(state), null);
    }

    private void makeDistribution(SimpleExcelTaxonImportState<CONFIG> state, String line, Taxon taxon) {

        handleArea(state, taxon, "Denmark", "Da", line);
        handleArea(state, taxon, "Faroe Islands", "Fa", line);
        handleArea(state, taxon, "Finland", "Fe", line);
        handleArea(state, taxon, "Iceland", "Is", line);
        handleArea(state, taxon, "Norway", "No", line);
        handleArea(state, taxon, "Svalbard", "Sb", line);
        handleArea(state, taxon, "Sweden", "Su", line);
        handleArea(state, taxon, "Channel Islands", "Ga(C)", line);
        handleArea(state, taxon, "Gibraltar", "Hs(G)", line);
        handleArea(state, taxon, "Great Britain", "Br", line);
        handleArea(state, taxon, "Ireland", "Hb", line);
        handleArea(state, taxon, "Northern Ireland", "Hb(N)", line);
        handleArea(state, taxon, "Andorra", "Hs(A)", line);
        handleArea(state, taxon, "Azores", "Az", line);
        handleArea(state, taxon, "Balearic Islands", "Bl", line);
        handleArea(state, taxon, "Canary Islands", "Ca", line);
        handleArea(state, taxon, "Corsica", "Co", line);
        handleArea(state, taxon, "Cyprus", "Cy", line);
        handleArea(state, taxon, "France", "Ga(F)", line);
        handleArea(state, taxon, "Italy", "It", line);
        handleArea(state, taxon, "Madeira", "Md", line);
        handleArea(state, taxon, "Malta", "Si(M)", line);
        handleArea(state, taxon, "Monaco", "Ga(M)", line);
        handleArea(state, taxon, "Portugal", "Lu", line);
        handleArea(state, taxon, "San Marino", "It(S)", line);
        handleArea(state, taxon, "Sardinia", "Sa", line);
        handleArea(state, taxon, "Sicily", "Si(S)", line);
        handleArea(state, taxon, "Spain", "Hs(S)", line);
        handleArea(state, taxon, "Vatican City", "It(V)", line);
        handleArea(state, taxon, "Austria", "Au(A)", line);
        handleArea(state, taxon, "Belgium", "Be(B)", line);
        handleArea(state, taxon, "Czech Republic", "Cs", line);
        handleArea(state, taxon, "Germany", "Ge", line);
        handleArea(state, taxon, "Liechtenstein", "Au(L)", line);
        handleArea(state, taxon, "Luxembourg", "Be(L)", line);
        handleArea(state, taxon, "Netherlands", "Ho", line);
        handleArea(state, taxon, "Poland", "Po", line);
        handleArea(state, taxon, "Slovakia", "Sk", line);
        handleArea(state, taxon, "Switzerland", "He", line);
        handleArea(state, taxon, "Albania", "Al", line);
        handleArea(state, taxon, "Bosnia-Herzegovina", "BH", line);
        handleArea(state, taxon, "Bulgaria", "Bu", line);
        handleArea(state, taxon, "Crete", "Cr", line);
        handleArea(state, taxon, "Croatia", "Ct", line);
        handleArea(state, taxon, "Greece", "Gr_AE(G)", line);
        handleArea(state, taxon, "Hungary", "Hu", line);
        handleArea(state, taxon, "Kosovo", "Ko", line);
        handleArea(state, taxon, "Montenegro", "Cg", line);
        handleArea(state, taxon, "North Macedonia", "Mk", line);
        handleArea(state, taxon, "Romania", "Rm", line);
        handleArea(state, taxon, "Serbia", "Sr", line);
        handleArea(state, taxon, "Slovenia", "Sl", line);
        handleArea(state, taxon, "Turkey", "Tu(E)", line);
        handleArea(state, taxon, "Belarus", "By", line);
        handleArea(state, taxon, "Caucasus (in Europe)", "Rf(CS)", line);
        handleArea(state, taxon, "Crimea", "Cm", line);
        handleArea(state, taxon, "Estonia", "Es", line);
        handleArea(state, taxon, "Kaliningrad", "Rf(K)", line);
//        handleArea(state, taxon, "Kazakhstan (in Europe)", "Kz", line);
        handleArea(state, taxon, "Latvia", "La", line);
        handleArea(state, taxon, "Lithuania", "Lt", line);
        handleArea(state, taxon, "Moldova", "Mo", line);
        handleArea(state, taxon, "Arctic Russia", "Rf(A2)", line);
        handleArea(state, taxon, "Central Russia", "Rf(C2)", line);
        handleArea(state, taxon, "NE Russia", "Rf(NE)", line);
        handleArea(state, taxon, "NW Russia", "Rf(NW2)", line);
        handleArea(state, taxon, "SE Russia", "Rf(SE)", line);
        handleArea(state, taxon, "South Urals", "Rf(SU)", line);
        handleArea(state, taxon, "Ukraine", "Uk", line);
    }

    private void handleArea(SimpleExcelTaxonImportState<CONFIG> state, Taxon taxon, String areaStr, String emAbbrev,
            String line) {

        Map<String, String> record = state.getOriginalRecord();
        //status
        String statusStr = record.get(areaStr);
        PresenceAbsenceTerm status = getDistributionStatus(statusStr, areaStr, taxon.getName(), line);
        if (status == null) {
            return;
        }
        //area
        NamedArea emArea = getEuroMedArea(emAbbrev, line);
        if (emArea == null) {
            return;
        }
        Distribution distribution = Distribution.NewInstance(emArea, status);
        Reference distributionSource = getDistributionSource();
        distribution.addPrimaryTaxonomicSource(distributionSource);

        TaxonDescription description = getTaxonDescription(taxon, false, true);
        description.addElement(distribution);

        //iucn
        PresenceAbsenceTerm iucnStatus = getRedListState(state, statusStr, emArea, taxon.getName(), line);
        if (iucnStatus != null) {
            Distribution iucnDistribution = Distribution.NewInstance(emArea, iucnStatus);
            Feature iucnAreaFeature = getFeature(state, uuidFeatureIucnAreas);
            iucnDistribution.setFeature(iucnAreaFeature);
            iucnDistribution.addPrimaryTaxonomicSource(distributionSource);
            description.addElement(iucnDistribution);
        }
    }

    private PresenceAbsenceTerm getDistributionStatus(String statusStr, String area, TaxonName name, String line) {

        if (isBlank(statusStr)|| "nm".equals(statusStr)) {
            return null;
        }else if (statusStr.trim().matches("(●|■|CR|EN|VU|NT|DD|DD\\*|NE|EB|BE|KW|GE|Mn|[0-4]|G|R|V|D|nb|E|I|LC)")) {
            return PresenceAbsenceTerm.NATIVE();
        }else if (statusStr.matches("(□|\\?)")) {
            return PresenceAbsenceTerm.NATIVE_PRESENCE_QUESTIONABLE();
        }else if (statusStr.matches("(-)")) {
            return PresenceAbsenceTerm.NATIVE_REPORTED_IN_ERROR();
        }else if (statusStr.matches("(RE|0|Ex)")) {
            return PresenceAbsenceTerm.NATIVE_FORMERLY_NATIVE();
        }else if (statusStr.matches("(NA)")) {
            logger.warn(line + "Distribution status 'nativ' given for status 'NA' needs to be checked for " + area + " and taxon " + name.getTitleCache());
            return PresenceAbsenceTerm.NATIVE();

        }else {
            logger.warn(line + "Distribution "+statusStr+" status not yet handled for " +  area + " and taxon " + name.getTitleCache());
            return null;
        }
    }

    private NamedArea getEuroMedArea(String emAbbrev, String line) {
        NamedArea result = areaMap.get(emAbbrev);
        if (result == null) {
            logger.warn(line+ "Area not found for emAbbrev: " + emAbbrev);
        }
        return result;
    }

    private Map<String, NamedArea> getAreaMap() {
        if (areaMap == null){
            makeAreaMap();
        }
        return areaMap;
    }

    private void makeAreaMap() {
        areaMap = new HashMap<>();
        //E+M areas
        @SuppressWarnings("unchecked")
        TermVocabulary<NamedArea> emAreaVoc = getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
        for (NamedArea area: emAreaVoc.getTerms()){
            NamedArea existingArea = areaMap.put(area.getIdInVocabulary(), area);
            if (existingArea != null) {
                logger.warn("Area abbreviation already existed: " + area.getIdInVocabulary());
            }
        }
        //moss areas
        @SuppressWarnings("unchecked")
        TermVocabulary<NamedArea> emMossesAreaVoc = getVocabularyService().find(EuroMedEuroMossesImportConfigurator.uuidEuroMedMossesAreas);
        for (NamedArea area: emMossesAreaVoc.getTerms()){
            NamedArea existingArea = areaMap.put(area.getIdInVocabulary(), area);
            if (existingArea != null) {
                logger.warn("Area abbreviation already existed: " + area.getIdInVocabulary());
            }
        }
    }

    private TaxonNode findParentNode(SimpleExcelTaxonImportState<CONFIG> state, TaxonName taxonName,
            TaxonName lastSpeciesName, Map<String, TaxonNode> genusNodeMap, String line) {

        if (taxonName.isInfraSpecific()) {
            verifyLastSpecies(line, taxonName, lastSpeciesName);

            Taxon lastTaxon = (Taxon)lastSpeciesName.getTaxonBases().iterator().next();
            if (lastSpeciesName.getTaxonBases().isEmpty()) {
                logger.warn(line + "Last species " + lastSpeciesName.getTitleCache() + " has no taxon node for " + taxonName.getTitleCache());
                return null;
            }
            TaxonNode lastTaxonNode = lastTaxon.getTaxonNodes().iterator().next();
            if (lastTaxonNode == null) {
                logger.warn(line + "Last species taxon not found");
            }
            return lastTaxonNode;
        }else if (taxonName.isSpecies()) {
            TaxonNode genusNode = getGenusNodeForSpecies(state, taxonName, genusNodeMap,line);
            if (genusNode == null) {
//                logger.warn(line + "Genus taxon not found for " + taxonName.getTitleCache());
                return null;
            }else {
                return genusNode;
            }
        }else {
            logger.warn(line + "rank not yet handled: " + taxonName.getTitleCache());
            return null;
        }
    }

    private void verifyLastSpecies(String line, TaxonName taxonName, TaxonName lastSpeciesName) {
        if (! taxonName.getNameCache().startsWith(lastSpeciesName.getNameCache())) {
            logger.warn(line + "Last species name not verified: " + taxonName.getTitleCache() +"<->" + lastSpeciesName.getTitleCache());
        }
    }


    private TaxonName makeName(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {

        String tropicosFullName = getValue(record, TROPICOS_FULLNAME);
        String refType = getValue(record, REF_TYPE);
        String abbrevTitle = getValue(record, ABBREV_TITLE);
        String inAuthors = getValue(record, IN_AUTHORS);
        String volume = getValue(record, VOLUME);
        String issue = getValue(record, ISSUE);
        String page = getValue(record, PAGE);
        String titleYear = getValue(record, TITLE_YEAR);
        String year = getValue(record, YEAR);
        String nomStatus = getValue(record, NOM_STATUS);
        String bhlLink = getValue(record, BHL_LINK);
        String nameId = getValue(record, NAME_ID);

        String fullStr = tropicosFullName;

        //ref Type
        if (isBlank(refType)) {
            verifyNoReference(record, line);
        }else if ("A".equals(refType) || "S".equals(refType)) {
            fullStr += " in ";
        }else if ("B".equals(refType)) {
            fullStr += ", ";
        }else {
            logger.warn(line + "RefType not yet handled: " + refType);
        }

        //inAuthor
        if (isNotBlank(inAuthors)) {
            fullStr += inAuthors +", ";
        }
        if (isNotBlank(abbrevTitle)) {
            fullStr += abbrevTitle;
            if (isNotBlank(volume)) {
                fullStr += " " + volume;
            }
            if (isNotBlank(issue)) {
                if (isBlank(volume)) {
                    logger.warn(line + "issue without volume: " + fullStr);
                    fullStr += " " + issue;
                }else {
                    fullStr += "(" + issue +")";
                }
            }
            if (isNotBlank(page)) {
                fullStr += ": " + page;
            }
            if (isBlank(year)) {
                year = titleYear;
                titleYear = null;
            }
            if (isNotBlank(year)) {
                fullStr += ". " + year;
            }
            if (isNotBlank(titleYear)) {
                fullStr += " [\"" + titleYear + "\"]";
            }
        }

        TaxonName name = parser.parseReferencedName(fullStr, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        if (name == null) {
            logger.warn(line + "Name does not exist after parsing: " + fullStr);
            return null;
        }

        if (name.hasProblem()) {
            logger.warn(line + "Problem with parsing: " + fullStr);
            name = handleUnparsableName(tropicosFullName, refType, inAuthors, abbrevTitle, volume, issue, page, year, titleYear, line);
        }else if (name.getNomenclaturalReference() != null && name.getNomenclaturalReference().hasProblem()) {
            logger.warn(line + "Problem with parsing reference for: " + fullStr);
            name = handleUnparsableName(tropicosFullName, refType, inAuthors, abbrevTitle, volume, issue, page, year, titleYear, line);
            name.setNomenclaturalMicroReference(page);
        }

        //nom status
        handleNomStatus(name, nomStatus, line);

        //bhl link
        if (isNotBlank(bhlLink)) {
            URI bhlUri = URI.create(bhlLink);
            ExternalLink link = ExternalLink.NewInstance(ExternalLinkType.WebSite, bhlUri);
            name.getNomenclaturalSource(true).addLink(link);
        }

        if (isNotBlank(nameId)) {
            name.addIdentifier(nameId, IdentifierType.IDENTIFIER_NAME_TROPICOS());
        }

        //deduplicate authors and references
        getDedupHelper(state).replaceAuthorNamesAndNomRef(name);

        addImportSource(state, name);
        return name;
    }

    private TaxonName handleUnparsableName(String tropicosFullName, String refType, String inAuthors,
            String abbrevTitle, String volume, String issue, String page, String year, String titleYear,
            String line) {
        TaxonName name = (TaxonName)parser.parseFullName(tropicosFullName, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        if (name.hasProblem()) {
            logger.warn(line + "Tropicos fullname could not be parsed: " + tropicosFullName);
        }

        String volIssue = isBlank(volume)? null: volume.trim();
        if (isNotBlank(issue)) {
            if (isBlank(volIssue)) {
                volIssue = issue;
            }else {
                volIssue += "(" + issue +")";
            }
        }

        VerbatimTimePeriod datePublished = TimePeriodParser.parseStringVerbatim(year);
        if (isNotBlank(titleYear)) {
            datePublished.setVerbatimDate(titleYear);
        }

        Reference nomRef;
        Reference inRef;
        if ("A".equals(refType)) {
            nomRef = ReferenceFactory.newArticle();
            inRef = ReferenceFactory.newJournal();
            nomRef.setInJournal(inRef);
            nomRef.setVolume(volIssue);
            inRef.setAbbrevTitle(abbrevTitle);
            nomRef.setDatePublished(datePublished);
        }else if ("B".equals(refType)) {
            nomRef = ReferenceFactory.newBook();
            nomRef.setVolume(volIssue);
            nomRef.setAbbrevTitle(abbrevTitle);
            nomRef.setDatePublished(datePublished);
        }else if ("S".equals(refType)) {
            nomRef = ReferenceFactory.newBookSection();
            inRef = ReferenceFactory.newBook();
            nomRef.setInJournal(inRef);
            inRef.setVolume(volIssue);
            inRef.setAbbrevTitle(abbrevTitle);
            nomRef.setDatePublished(datePublished);
            TeamOrPersonBase<?> inAuthor;
            if (inAuthors.contains(" & ")) {
                String[] splits = inAuthors.split(" & ");
                inAuthor = Team.NewInstance();
                for (String split : splits) {
                    if ("al.".equals(split)) {
                        ((Team)inAuthor).setHasMoreMembers(true);
                    }else {
                        Person person = Person.NewInstance(null, split, null, null);
                        ((Team)inAuthor).addTeamMember(person);
                    }
                }
            }else {
                inAuthor = Person.NewInstance(null, inAuthors, null, null);
            }
            inRef.setAuthorship(inAuthor);
        }else {
            logger.warn(line + "RefType not yet handled: " + refType);
            nomRef = null;
        }

        name.setNomenclaturalReference(nomRef);
        name.setNomenclaturalMicroReference(page);

        return name;
    }

    private void handleNomStatus(TaxonName name, String nomStatus, String line) {
        if ("Legitimate".equals(nomStatus)) {
            //do nothing
        }else if ("nom. cons.".equals(nomStatus)) {
            name.addStatus(NomenclaturalStatusType.CONSERVED(), null, null);
        }else if ("No opinion".equals(nomStatus)) {
            //do nothing (for now)
        }else if ("Invalid".equals(nomStatus)) {
            logger.warn(line + "Status 'invalid' not yet handled for " +  name.getTitleCache());
        }else {
            logger.warn(line + "Status '"+nomStatus+"' not yet handled for " +  name.getTitleCache());
        }
    }

    private void verifyNoReference(Map<String, String> record, String line) {
        if (isNotBlank(record.get(VOLUME))
                || isNotBlank(record.get(ISSUE))
                || isNotBlank(record.get(PAGE))
                || isNotBlank(record.get(YEAR))
                || isNotBlank(record.get(TITLE_YEAR))
                || isNotBlank(record.get(BHL_LINK))
                ) {
            logger.warn(line+ "No reftype given but reference information exists");
        }
    }

    private TaxonNode getGenusNodeForSpecies(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonName speciesName, Map<String, TaxonNode> genusNodeMap, String line) {

        String genusStr = speciesName.getGenusOrUninomial();
        if (genusNodeMap.get(genusStr) != null) {
            return genusNodeMap.get(genusStr);
        }

        MatchingTaxonConfigurator matchConfig = MatchingTaxonConfigurator.NewInstance();
        matchConfig.setTaxonNameTitle(genusStr);
        matchConfig.setSecUuid(state.getConfig().getSecUuid());
        @SuppressWarnings("rawtypes")
        List<TaxonBase> genusCandidates = getTaxonService().findTaxaByName(matchConfig);
        Taxon genusTaxon = null;
        if (genusCandidates.isEmpty()) {
            logger.warn(line + "Genus " + genusStr + " not found for " + speciesName.getTitleCache());
            return null;
        } else if (genusCandidates.size() > 1) {
            logger.warn(line + ">1 genus candidates " + genusStr + " found for " + speciesName.getTitleCache());
            for (TaxonBase<?> taxonBase : genusCandidates) {
                if (taxonBase.isInstanceOf(Taxon.class)) {
                    if (genusTaxon != null) {
                        logger.warn(line + " ... and >1 of them are accepted");
                    }
                    genusTaxon = CdmBase.deproxy(taxonBase, Taxon.class);
                }
            }
            if (genusTaxon == null) {
                logger.warn(line + " ... and none of them is accepted");
                return null;
            }
        } else if (!genusCandidates.iterator().next().isInstanceOf(Taxon.class)) {
            logger.warn(line + "Genus candidate " + genusStr + " is not accepted for " + genusStr);
            return null;
        }else {
            genusTaxon = CdmBase.deproxy(genusCandidates.iterator().next(), Taxon.class);
        }

        verifyGenus(line, speciesName, genusTaxon);

        Set<TaxonNode> nodes = genusTaxon.getTaxonNodes();
        if (nodes.size()==0){
            logger.warn(line + "No genus node: " + genusTaxon.getTitleCache());
            return null;
        }else if (nodes.size()>1){
            logger.warn(line + "More than 1 genus node: " + genusTaxon.getTitleCache());
        }
        TaxonNode result = nodes.iterator().next();
        genusNodeMap.put(genusStr, result);
        if (result == null) {
            logger.warn(line + "Taxon node not found for genus: " + genusTaxon.getTitleCache());
        }
        return result;
    }

    private void verifyGenus(String line, TaxonName taxonName, Taxon genus) {
        TaxonName genusName = genus.getName();
        if (! taxonName.getNameCache().startsWith(genusName.getNameCache())) {
            logger.warn(line + "Genus name not verified: " + taxonName.getTitleCache() +"<->" + genusName.getTitleCache());
        }
    }

    private ImportDeduplicationHelper getDedupHelper(SimpleExcelTaxonImportState<CONFIG> state) {
        if (dedupHelper == null) {
            dedupHelper = ImportDeduplicationHelper.NewInstance(this, state);
        }
        return this.dedupHelper;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }
}