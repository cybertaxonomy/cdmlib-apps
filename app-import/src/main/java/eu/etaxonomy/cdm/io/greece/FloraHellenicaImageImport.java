/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata.Item;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.media.RightsType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
/**
 * Import for the Flora Hellenica images.
 *
 * @author a.mueller
 * @since 03.04.2017
 */

@Component
public class FloraHellenicaImageImport<CONFIG extends FloraHellenicaImportConfigurator>
        extends CdmImportBase<CONFIG,SimpleExcelTaxonImportState<CONFIG>>{

    private static final long serialVersionUID = 7118028793298922703L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaImageImport.class);

    private static final String BASE_URL = "https://media.e-taxonomy.eu/flora-greece/";
    private static final String IMAGE_FOLDER = "////BGBM-PESIHPC/Greece/thumbs/";

    @SuppressWarnings("unchecked")
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>> deduplicationHelper = (ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>>)ImportDeduplicationHelper.NewInstance(this);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInvoke(SimpleExcelTaxonImportState<CONFIG> state) {
        TransactionStatus tx = this.startTransaction();
        for (int plate = 1; plate < 22 ; plate++){
            try {
                handleSinglePlate(state, plate);
            } catch (Exception e) {
                logger.error("Error when handling plate " + plate);
                e.printStackTrace();
            }
        }
        this.commitTransaction(tx);
    }

    /**
     * @param state
     * @param plate
     */
    private void handleSinglePlate(SimpleExcelTaxonImportState<CONFIG> state, int plate) {
        String fill = plate < 10 ? "0" : "";
        String plateStr = "Plate_" + fill + plate + "/";
        String fullFolderUrl = BASE_URL + plateStr;
        String fullThumbUrl = BASE_URL + "thumbs/" + plateStr;
        String folderStr = IMAGE_FOLDER + plateStr;
        File file = new File(folderStr);
        String[] list = file.list();
        for (String fileStr : list){
            try {
                handleSingleFile(state, fullFolderUrl, fullThumbUrl, fileStr, plate);
            } catch (Exception e) {
                logger.error("Error when handling file: " + fileStr + " in plate " + plate);
                e.printStackTrace();
            }
        }
    }

    /**
     * @param state
     * @param fullFolderUrl
     * @param fullThumbUrl
     * @param fileStr
     * @param plate
     */
    private void handleSingleFile(SimpleExcelTaxonImportState<CONFIG> state,
            String fullFolderUrl, String fullThumbUrl, String fileStr, int plate) {
        String[] taxonNameAndArtist = getTaxonName(fileStr);
        String taxonNameStr = taxonNameAndArtist[0];
        String taxonNameStr2 = null;
        String artistStr = taxonNameAndArtist[1];
        if (fileStr.equals("RamondaSerbica(L)+Nathaliae(R)1.jpg")){
            taxonNameStr = "Ramonda serbica";
            taxonNameStr2 = "Ramonda nathaliae";
        }else if (fileStr.contains("HypericumCerastioides")){
            taxonNameStr = taxonNameStr.replace("HypericumCerastoides", "HypericumCerastioides");
        }else if (fileStr.contains("StachysScardica")){
            taxonNameStr = taxonNameStr.replace("StachysScardica", "BetonicaScardica");
        }else if (fileStr.contains("OleaEuropaeaOleaster ")){
            taxonNameStr = taxonNameStr.replace("OleaEuropaeaOleaster", "OleaEuropaeaEuropaea");
        }

        try {

            Media media = getImageMedia(fullFolderUrl + fileStr, fullThumbUrl + fileStr, true);

            //image metadata
            URI uri = URI.create(fullThumbUrl + fileStr);
            try{
                IImageMetadata metadata = Sanselan.getMetadata(uri.toURL().openStream(), null);
                ArrayList<?> items = metadata.getItems();
                for (Object object : items){
                    Item item = (Item) object;
//                    System.out.println(item.getKeyword() +  ":    " + item.getText());
                    String keyword = item.getKeyword().toLowerCase();
                    String value = removeQuots(item.getText());
                    if("image description".equals(keyword)){
                        media.putDescription(Language.DEFAULT(), value);
                    }else if ("artist".equals(keyword)){
                        if (isNotBlank(artistStr) && ! value.contains(artistStr)){
                            logger.warn("Artist and artistStr are different: " +  artistStr  + "; " + value);
                        }
                        artistStr = value;
                    }else if ("date time original".equalsIgnoreCase(item.getKeyword())){
                        DateTimeFormatter f = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");
                        DateTime created = f.withZone(DateTimeZone.forID("Europe/Athens")).parseDateTime(value);
                        media.setMediaCreated(created);
                    }
                }
            } catch (ImageReadException | IOException e1) {
                e1.printStackTrace();
            }
            if (isNotBlank(artistStr)){
                Person person = Person.NewInstance();
                String[] split = artistStr.split("\\+");
                if (split.length == 1){
                    person.setFamilyName(artistStr);
                }else if (split.length == 2){
                    person.setGivenName(split[0]);
                    person.setFamilyName(split[1]);
                }else{
                    person.setTitleCache("artistStr", true);
                }
                person = (Person)deduplicationHelper.getExistingAuthor(state, person);

                media.setArtist(person);
                //copyright
                Rights right = Rights.NewInstance();
                right.setType(RightsType.COPYRIGHT());
                right.setAgent(person);
                right = deduplicationHelper.getExistingCopyright(state, right);
                media.addRights(right);
            }

            String detail = "p. " + FloraHellenicaImageCaptionImport.startPage + 1 + plate *2;
            media.addPrimaryMediaSource(getSecReference(state), detail);


            Taxon taxon = getAcceptedTaxon(taxonNameStr);
            makeTextData(fileStr, media, taxon);
            if (taxonNameStr2 != null){
                Taxon taxon2 = getAcceptedTaxon(taxonNameStr);
                makeTextData(fileStr, media, taxon2);
            }


            if (taxonNameStr2 == null){
                media.putTitle(Language.LATIN(), taxon == null ? taxonNameStr :
                    taxon.getName().getTitleCache());
            }else{
                media.putTitle(Language.LATIN(), "Ramonda serbica(L) + R. nathaliae(R)");
            }


        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private String removeQuots(String text) {
        if (text.startsWith("'") && text.endsWith("'")){
            return text.substring(1, text.length() -1);
        }else{
            return text;
        }
    }

    private Reference secReference;
    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (secReference != null){
            secReference = getReferenceService().find(state.getConfig().getSecReference().getUuid());
        }
        return secReference;
    }


    /**
     * Gets the image gallery, creates
     */
    private void makeTextData(String fileStr, Media media, Taxon taxon) {
        if (taxon == null){
            logger.warn("Taxon not found for image " + fileStr + "."
                    + "Media could not be attached to taxon.");
            getMediaService().saveOrUpdate(media);
            return;
        }
        TaxonDescription imageGallery = taxon.getImageGallery(true);
        TextData textData;
        if (imageGallery.getElements().isEmpty()){
            textData = TextData.NewInstance();
            textData.setFeature(Feature.IMAGE());
        }else{
            textData = CdmBase.deproxy(imageGallery.getElements().iterator().next(), TextData.class);
        }
        imageGallery.addElement(textData);
        textData.addMedia(media);
    }

    /**
     * @param taxonNameStr
     * @return
     */
    private Taxon getAcceptedTaxon(String taxonNameStr) {

        MatchingTaxonConfigurator config = new MatchingTaxonConfigurator();
        taxonNameStr = adaptName(taxonNameStr);
        config.setTaxonNameTitle(taxonNameStr);
        config.setIncludeSynonyms(true);
        List<TaxonBase> list = getTaxonService().findTaxaByName(config);
        if (list.isEmpty()){
            logger.warn("Taxon not found for media: " + taxonNameStr);
            return null;
        }else{
            if (list.size()>1){
                logger.warn("More than 1 taxon found for media: " + taxonNameStr);
            }
            TaxonBase<?> taxonBase = list.get(0);
            Taxon result;
            if (taxonBase.isInstanceOf(Synonym.class)){
                result = CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
            }else{
                result = CdmBase.deproxy(taxonBase, Taxon.class);
            }
            return result;
        }
    }

    /**
     * @param taxonNameStr
     * @return
     */
    private String adaptName(String taxonNameStr) {
        if (taxonNameStr.equals("Hypericum cerastoides")){
            taxonNameStr = "Hypericum cerastioides";
        }
        return taxonNameStr;
    }

    /**
     * @param fileStr
     * @return
     */
    private String[] getTaxonName(String fileStr) {
        String[] result = new String[2];
        fileStr = fileStr.split("\\.")[0];
        fileStr = fileStr.replaceAll("[0-9]", "");
        String[] x = fileStr.split("_");
        if (x.length == 2){
            result[1] = x[1];
        }

        fileStr = splitCamelCase(x[0]);
        String[] split = fileStr.split(" ");
        String name = split[0] + " " + split[1].toLowerCase() +
                (split.length > 2 ? " subsp. " + split[2].toLowerCase() : "");
        result[0] = name;
        System.out.println(result[0] + (result[1] != null ?  "   Artist: " + result[1]: ""));
        return result;
    }

    //from http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    static String splitCamelCase(String s) {
        return s.replaceAll(
           String.format("%s",
//              "(?<=[A-Z])(?=[A-Z][a-z])",
              "(?<=[^A-Z])(?=[A-Z])"
//              "(?<=[A-Za-z])(?=[^A-Za-z])"
           ),
           " "
        );
     }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doCheck(SimpleExcelTaxonImportState<CONFIG> state) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoImages();
    }



}
