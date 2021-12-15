/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.etaxonomy.cdm.api.service.media.MediaInfoFileReader;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.media.CdmImageInfo;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 13.05.2017
 */
public class ImageImportTest {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FloraHellenicaImageImport.class);

    private static final String BASE_URL = "https://media.e-taxonomy.eu/flora-greece/";
    private static final String IMAGE_FOLDER = "////BGBM-PESIHPC/Greece/thumbs/";
    private ImportDeduplicationHelper deduplicationHelper = null;

    protected void doInvoke() {
        for (int plate = 1; plate < 22 ; plate++){
            System.out.println("Plate: " + plate);
            String fill = plate < 10 ? "0" : "";
            String plateStr = "Plate_" + fill + plate + "/";
            String fullFolderUrl = BASE_URL + plateStr;
            String fullThumbUrl = BASE_URL + "thumbs/" + plateStr;
            String folderStr = IMAGE_FOLDER + plateStr;
            File file = new File(folderStr);
            String[] list = file.list();
            System.out.println(DateTimeZone.getAvailableIDs());
            for (String fileStr : list){
                String[] taxonNameAndArtist = getTaxonName(fileStr);
                String taxonNameStr = taxonNameAndArtist[0];
                String artistStr = taxonNameAndArtist[1];

                if(false){
                    continue;
                }
                Taxon taxon = getAcceptedTaxon(taxonNameStr);
                TaxonDescription imageGallery = taxon.getImageGallery(true);
                TextData textData = TextData.NewInstance();

                URI uri = URI.create(fullFolderUrl + fileStr);

                //image metadata
                File imageFile = new File("");
                ImageMetadata metadata;
                try {
                    metadata = Imaging.getMetadata(uri.toURL().openStream(), null);
                    List<? extends ImageMetadataItem> items = metadata.getItems();
                    for (ImageMetadataItem metadataItem : items){
                        if (metadataItem instanceof GenericImageMetadataItem){
                            GenericImageMetadataItem item = (GenericImageMetadataItem) metadataItem;
    //                      System.out.println(item.getKeyword() +  ":    " + item.getText());
                            String value = removeQuots(item.getText());
                            if("Image Description".equalsIgnoreCase(item.getKeyword())){
    //                          media.putDescription(Language.DEFAULT(), item.getText());
                            }else if ("date time original".equalsIgnoreCase(item.getKeyword())){
                                DateTimeFormatter f = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");
                                DateTime created = f.withZone(DateTimeZone.forID("Europe/Athens")).parseDateTime(value);
                                System.out.println(created);
                            }
                        }else{
                            throw new IllegalStateException("Unsupported ImageMetadataItem type: " + metadataItem.getClass().getName());
                        }
                    }
                } catch (ImageReadException | IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                CdmImageInfo imageMetaData;
                try {
                    //imageMetaData = CdmImageInfo.NewInstance(uri, 0);
                    imageMetaData = MediaInfoFileReader.legacyFactoryMethod(uri)
                            .readBaseInfo()
                            .getCdmImageInfo();

                    String mimeType = imageMetaData.getMimeType();
                    String suffix = null;
                    int height = imageMetaData.getHeight();
                    int width = imageMetaData.getWidth();
                    Integer size = null;
                    TimePeriod mediaCreated = null;
                    AgentBase<?> artist = null;
                    Media media = ImageFile.NewMediaInstance(mediaCreated, artist, uri, mimeType, suffix, size, height, width);

                    textData.addMedia(media);
                    imageGallery.addElement(textData);
                } catch (IOException | HttpException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    private String removeQuots(String text) {
        if (text.startsWith("'") && text.endsWith("'")){
            return text.substring(1, text.length() -1);
        }else{
            return text;
        }
    }

    private Taxon getAcceptedTaxon(String taxonNameStr) {
        return Taxon.NewInstance(null, null);
    }

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

    public static void main(String[] str){
        ImageImportTest test = new ImageImportTest();
        test.doInvoke();
    }
}
