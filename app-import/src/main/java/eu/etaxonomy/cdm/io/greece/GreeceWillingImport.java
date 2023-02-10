/**
* Copyright (C) 2018 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.media.RightsType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 21.08.2018
 */
@Component
public class GreeceWillingImport
        extends SimpleExcelTaxonImport<GreeceWillingImportConfigurator>{

    private static final long serialVersionUID = 8258914747643501550L;
    private static final Logger logger = LogManager.getLogger();

    private static final String HERBARIUM_ID_NAMESPACE = "HerbariumID";
    private static final String RDF_ID_NAMESPACE = "rdfID";

    private String lastCollectorNumber;
    private UUID lastDerivedUnitUuid;

    private String lastTaxonTitle;
    private UUID lastTaxonDescription;

    private int count = 1;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state) {
        try {
            Map<String, String> record = state.getOriginalRecord();

            String scientificName = record.get("ScientificName");
            String stableIdentifier = record.get("ObjectURI");
            String title = record.get("Title");
            String titleDescription = record.get("TitleDescription");

            String collector = record.get("Collector");
            String collectorNumber = record.get("CollectorNumber");

            String collectionDate = record.get("CollectionDate");
//            String CollDateISO = record.get("CollDateISO");
            String catalogNumber = record.get("CatalogNumber");
            String locality = record.get("Locality");
            String image = record.get("Image");

            String latitude = record.get("Latitude");
            String longitude = record.get("Longitude");

            String rdfId = record.get(RDF_ID_NAMESPACE);
            String herbariumId = record.get(HERBARIUM_ID_NAMESPACE);

            String baseOfRecords = record.get("BaseOfRecords");
            String collectionCode = record.get("CollectionCode");
            String institutionCode = record.get("InstitutionCode");

            TimePeriod date = TimePeriodParser.parseString(collectionDate);
            if (date.getFreeText() != null){
                System.out.println("Date could not be parsed: " + collectionDate + "; row: " + state.getCurrentLine());
            }

//            validate(state, "BaseOfRecords", "Specimen");
//            validate(state, "InstitutionCode", "BGBM");
//            validate(state, "CollectionCode", "B");
            validate(state, "HigherGeography", "Greece");
            validate(state, "Country", "Greece");
            validate(state, "CountryCode", "GR");

            //not used, but validate just in case
            validate(state, "HUH_PURL", "NULL");
//            validate(state, "DB", "JACQ");
            validate(state, "CollDateISO", collectionDate);

//            validate(state, "HerbariumID", collectionDate);

            //open
//          HerbariumID;
//          HTML_URI


            Reference sourceReference = getSourceReference(state);

            Taxon taxon = getTaxonByName(state, scientificName);
            verifyTaxon(state, taxon, record);
            if (taxon == null){
                System.out.println("Taxon not found for " + scientificName + "; row:  " + state.getCurrentLine());
                if (!state.getConfig().isH2()){
                    return;
                }else{
                    taxon = Taxon.NewInstance(TaxonNameFactory.NewBotanicalInstance(null), getSourceReference(state));
                    taxon.getName().setTitleCache(title, true);
                }
            }
            if (state.getConfig().isCheckNamesOnly()){
                return;
            }

            DerivedUnit lastDerivedUnit = null;
            if (collectorNumber.equals(lastCollectorNumber)){
                lastDerivedUnit = (DerivedUnit)getOccurrenceService().find(lastDerivedUnitUuid);
            }

            DerivedUnitFacade facade;
            String sourceId = rdfId;
            String sourceNamespace = RDF_ID_NAMESPACE;
            if (rdfId.equalsIgnoreCase("NULL")){
                sourceId = herbariumId;
                sourceNamespace = HERBARIUM_ID_NAMESPACE;
            }

            if (lastDerivedUnit == null){
                if (baseOfRecords.equals("Specimen")){
                    facade = DerivedUnitFacade.NewPreservedSpecimenInstance();
                }else if (baseOfRecords.equals("HumanObservation")){
                    facade = DerivedUnitFacade.NewInstance(SpecimenOrObservationType.Observation, null);
                }else {
                    System.out.println("baseOfRecords of records not recognized: " +  baseOfRecords + "; use preserved specimen as default");
                    facade = DerivedUnitFacade.NewPreservedSpecimenInstance();
                }
                facade.setFieldNumber(collectorNumber);
                facade.setCountry(Country.GREECEHELLENICREPUBLIC());
                facade.setLocality(locality);
                try {
                    facade.setExactLocationByParsing(longitude, latitude, null, null);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                facade.setCollector(getCollector(state, collector));
                facade.getGatheringEvent(true).setTimeperiod(date);
                facade.setPreferredStableUri(URI.create(stableIdentifier));
                if (catalogNumber.startsWith("B")){
                    facade.setBarcode(catalogNumber);
                }else{
                    facade.setCatalogNumber(catalogNumber);
                }
                facade.setCollection(getCollection(state, collectionCode, institutionCode));
                this.addOriginalSource(facade.innerFieldUnit(), sourceId, sourceNamespace, sourceReference);
                this.addOriginalSource(facade.innerDerivedUnit(), sourceId, sourceNamespace, sourceReference);

                IndividualsAssociation specimen = IndividualsAssociation.NewInstance(facade.innerDerivedUnit());
                if (baseOfRecords.equals("HumanObservation")){
                    specimen.setFeature(Feature.OBSERVATION());
                }else if (baseOfRecords.equals("Specimen")){
                    specimen.setFeature(Feature.SPECIMEN());
                }else{
                    System.out.println("Base of record not recognized for feature selection: " + baseOfRecords);
                }
                if (taxon != null ){
                    TaxonDescription description = getTaxonDescription(taxon, sourceReference, false, CREATE);
                    description.addElement(specimen);
                }
                lastCollectorNumber = collectorNumber;
                lastTaxonTitle = title;
                lastDerivedUnitUuid = specimen.getUuid();
                count = 1;
            }else{
                try {
                    facade = DerivedUnitFacade.NewInstance(lastDerivedUnit);
                } catch (DerivedUnitFacadeNotSupportedException e) {
                    System.out.println("Error in " + state.getCurrentLine());
                    e.printStackTrace();
                    return;
                }
                count++;
            }

            Media media = getMedia(state,title + " (Willing " + count + ")", titleDescription, image, date);
            facade.addFieldObjectMedia(media);

            TaxonDescription imageGallery = taxon.getOrCreateImageGallery(taxon.getName().getTitleCache());
            TextData imageTextData;
            if (imageGallery.getElements().isEmpty()){
                imageTextData = TextData.NewInstance(Feature.IMAGE());
                imageGallery.addElement(imageTextData);
            }else{
                imageTextData = (CdmBase.deproxy(imageGallery.getElements().iterator().next(), TextData.class));
            }
            imageTextData.addMedia(media);

//            media.addPrimaryMediaSource(citation, microCitation);
            this.addOriginalSource(media, sourceId, sourceNamespace, sourceReference);


            //        getDedupHelper(state).replaceAuthorNamesAndNomRef(state, name);

            getOccurrenceService().saveOrUpdate(facade.baseUnit());
            lastDerivedUnitUuid = facade.baseUnit().getUuid();

        } catch (MalformedURLException e) {
            logger.warn("An error occurred during import");
        }

    }

    private void verifyTaxon(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state,
            Taxon taxon, Map<String, String> record) {

        if (taxon==null){
            return;
        }
        String genus = record.get("Genus");
        String specificEpi = record.get("SpecificEpithet");
        String family = record.get("Family");

        if (!CdmUtils.nullSafeEqual(genus, taxon.getName().getGenusOrUninomial())){
            System.out.println(" Genus and taxonNameGenus not equal: " +
                    genus + " <-> " + taxon.getName().getGenusOrUninomial() + "; row: " + state.getCurrentLine());
        }
        if (!CdmUtils.nullSafeEqual(specificEpi, taxon.getName().getSpecificEpithet())){
            System.out.println(" SpecificEpi and taxonNameSpecificEpi not equal: " +
                    specificEpi + " <-> " + taxon.getName().getSpecificEpithet() + "; row: " + state.getCurrentLine());
        }
        while (taxon.getTaxonNodes().size()== 1 ){
            Taxon parent = taxon.getTaxonNodes().iterator().next().getParent().getTaxon();
            if (parent == null){
                break;
            }else{
                if (parent.getName().getRank().equals(Rank.FAMILY()) && parent.getName().getGenusOrUninomial().equals(family)){
                    return;
                }
                taxon = parent;
            }
        }
        System.out.println(" Family could not be verified: " + family + "; row: " + state.getCurrentLine());
    }

    private Collection bgbm;

    private Collection getCollection(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state, String collectionCode, String institutionCode) {
        if (bgbm == null){
            List<Collection> results = getCollectionService().searchByCode(collectionCode);
            if (results.size()> 1){
                throw new RuntimeException("More then 1 collection found for 'B'");
            }else if (results.isEmpty()){
                Collection collection = Collection.NewInstance();
                collection.setCode(collectionCode);
                getCollectionService().save(collection);
                System.out.println("Collection '"+collectionCode+"' did not exist. Created new one.");
                return collection;
//                throw new RuntimeException("No collection found for 'B'");
            }
            if ("B".equals(collectionCode) && !"".equals(institutionCode)
                    || "HWilling".equals(collectionCode) && !"JACQ".equals(institutionCode)){
                System.out.println("CollectionCode and InstitutionCode do not match expected values: " + collectionCode + "; " + institutionCode);
            }
            bgbm = results.get(0);
        }
        return bgbm;
    }

    private void validate(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state, String attr,
            String expectedValue) {
        Map<String, String> record = state.getOriginalRecord();
        String attrValue = record.get(attr);
        if (!expectedValue.equalsIgnoreCase(attrValue)){
            throw new RuntimeException("Attribute " + attr + " has not expected value " + expectedValue + " but "+ attrValue);
        }
    }

    private Media getMedia(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state, String title,
            String titleDescription, String imageUrl, TimePeriod date) throws MalformedURLException {

        Person artist = getArtist(state);
        String baseUrl = imageUrl.replace("http://ww2.bgbm.org/herbarium/images/Willing/GR/", "http://mediastorage.bgbm.org/fsi/server?type=image&source=Willing_GR/");
        String thumbnail = baseUrl + "&width=240&profile=jpeg&quality=98";

        String medium = baseUrl + "&width=350&profile=jpeg&quality=95";
        Media media = getImageMedia(imageUrl, medium, thumbnail, true);

        media.setMediaCreated(date);
        media.setArtist(artist);

        //copyright
        Rights right = Rights.NewInstance();
        right.setType(RightsType.COPYRIGHT());
        right.setAgent(artist);
        right = state.getDeduplicationHelper().getExistingCopyright(right);
        media.addRights(right);

        if (isNotBlank(title)){
            media.putTitle(Language.ENGLISH(), title);
        }
        if (isNotBlank(titleDescription)){
            media.putDescription(Language.ENGLISH(), titleDescription);
        }

        return media;
    }

    private Team willingCollector;

    private Team getCollector(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state,
            String collector) {

        if (!"Willing,R. & Willing,E.".equals(collector)){
            throw new RuntimeException("Unexpected collector: " + collector);
        }
        if (willingCollector == null){
            UUID willingTeamUuid = UUID.fromString("ab3594a5-304f-4f19-bc8b-4a38c8abfad7");
            willingCollector = (Team)getAgentService().find(willingTeamUuid);
            if (willingCollector == null){
                willingCollector = Team.NewTitledInstance("Willing, R. & Willing, E.", null);
                getAgentService().save(willingCollector);
            }
        }
        return willingCollector;
    }

    private Person willingArtist;
    private Person getArtist(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state) {

        if (willingArtist == null){
            UUID willingArtistUuid = UUID.fromString("83ff66c7-4e51-4f6e-ac37-593a52ce3430");
            willingArtist = (Person)getAgentService().find(willingArtistUuid);
            if (willingArtist == null){
                willingArtist = Person.NewTitledInstance("Willing, E.");
                getAgentService().save(willingArtist);
            }
        }
        return willingArtist;
    }

    private Taxon getTaxonByName(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state,
            String scientificName) {

        MatchingTaxonConfigurator config = MatchingTaxonConfigurator.NewInstance();
        config.setTaxonNameTitle(scientificName);
        config.setIncludeSynonyms(false);
        Taxon result = getTaxonService().findBestMatchingTaxon(config);
        return result;
    }
}