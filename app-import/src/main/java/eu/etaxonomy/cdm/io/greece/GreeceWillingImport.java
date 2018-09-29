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
import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 21.08.2018
 *
 */
public class GreeceWillingImport
        extends SimpleExcelTaxonImport<GreeceWillingImportConfigurator>{

    private static final long serialVersionUID = 8258914747643501550L;

    private static final Logger logger = Logger.getLogger(GreeceWillingImport.class);

    private ImportDeduplicationHelper<SimpleExcelTaxonImportState> dedupHelper;


    /**
     * {@inheritDoc}
     */
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

            String rdfId = record.get("rdfID");

            TimePeriod date = TimePeriodParser.parseEnglishDate(collectionDate, null);

            validate(state, "BaseOfRecords", "Specimen");
            validate(state, "InstitutionCode", "BGBM");
            validate(state, "CollectionCode", "B");
            validate(state, "HigherGeography", "Greece");
            validate(state, "Country", "Greece");
            validate(state, "CountryCode", "GR");


//        HerbariumID
//        CollDateISO


            Taxon taxon = getTaxonByName(state, scientificName);
            DerivedUnitFacade facade = DerivedUnitFacade.NewPreservedSpecimenInstance();

            facade.setPreferredStableUri(URI.create(stableIdentifier));
            facade.setFieldNumber(collectorNumber);
            facade.setBarcode(catalogNumber);
            facade.setCountry(Country.GREECEHELLENICREPUBLIC());

            facade.setLocality(locality);
            try {
                facade.setExactLocationByParsing(longitude, latitude, null, null);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            facade.setCollector(getCollector(state, collector));
            facade.getGatheringEvent(true).setTimeperiod(date);

            facade.setCollection(getCollection(state));

            Media media = getMedia(state,title, titleDescription, image);
            facade.addFieldObjectMedia(media);

            Reference sourceReference = state.getSourceReference();
            String sourceId = rdfId;
            String namespace = "rdfID";
            this.addOriginalSource(facade.innerFieldUnit(), sourceId, namespace, sourceReference);
            this.addOriginalSource(facade.innerDerivedUnit(), sourceId, namespace, sourceReference);
            this.addOriginalSource(media, sourceId, namespace, sourceReference);

            TaxonDescription description = getTaxonDescription(taxon, sourceReference, false, CREATE);
            IndividualsAssociation specimen = IndividualsAssociation.NewInstance(facade.innerDerivedUnit());
            description.addElement(specimen);

            //        getDedupHelper(state).replaceAuthorNamesAndNomRef(state, name);

        } catch (MalformedURLException e) {
            logger.warn("An error occurred during import");
        }




    }

    private Collection bgbm;
    /**
     * @param state
     * @return
     */
    private Collection getCollection(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state) {
        if (bgbm == null){
            List<Collection> results = getCollectionService().searchByCode("B");
            if (results.size()> 1){
                throw new RuntimeException("More then 1 collection found for 'B'");
            }else if (results.isEmpty()){
                throw new RuntimeException("No collection found for 'B'");
            }
            bgbm = results.get(0);
        }
        return bgbm;
    }

    /**
     * @param state
     * @param string
     * @param string2
     */
    private void validate(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state, String attr,
            String expectedValue) {
        Map<String, String> record = state.getOriginalRecord();
        String attrValue = record.get(attr);
        if (!expectedValue.equalsIgnoreCase(attrValue)){
            throw new RuntimeException("Attribute " + attr + " has not expected value " + expectedValue + " but "+ attrValue);
        }
    }

    /**
     * @param state
     * @param title
     * @param titleDescription
     * @param image
     * @return
     * @throws MalformedURLException
     */
    private Media getMedia(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state, String title,
            String titleDescription, String image) throws MalformedURLException {

        //TODO
        String thumbnail = image;
        Media media = getImageMedia(image, thumbnail, true);

        if (isNotBlank(title)){
            media.putTitle(Language.ENGLISH(), title);
        }
        if (isNotBlank(titleDescription)){
            media.putDescription(Language.ENGLISH(), titleDescription);
        }


        //TODO thumbnails etc.

//        ImageInfo info = ImageInfo.NewInstanceWithMetaData(URI.create(image), 60);
//        Media media = Media.NewInstance(uri, info.getLength(), info.getMimeType(), info.getSuffix());
        return media;
    }


    Team willingCollector;
    /**
     * @param state
     * @param collector
     * @return
     */
    private Team getCollector(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state,
            String collector) {
        if (!"Willing,R. & Willing,E.".equals(collector)){
            throw new RuntimeException("Unexpected collector: " + collector);
        }
        if (willingCollector == null){
            UUID willingTeamUuid = UUID.fromString("ab3594a5-304f-4f19-bc8b-4a38c8abfad7");
            willingCollector = (Team)getAgentService().find(willingTeamUuid);
        }
        return willingCollector;
    }

    /**
     * @param state
     * @param scientificName
     * @return
     */
    private Taxon getTaxonByName(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state,
            String scientificName) {
        MatchingTaxonConfigurator config = MatchingTaxonConfigurator.NewInstance();
        config.setTaxonNameTitle(scientificName);
        config.setIncludeSynonyms(false);
        Taxon result = getTaxonService().findBestMatchingTaxon(config);
        return result;
    }

    /**
     * @param state
     * @return
     */
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState> getDedupHelper(SimpleExcelTaxonImportState<GreeceWillingImportConfigurator> state) {
        if (this.dedupHelper == null){
            dedupHelper = ImportDeduplicationHelper.NewInstance(this, state);
        }
        return this.dedupHelper;
    }
}
