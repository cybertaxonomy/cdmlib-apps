/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 14.12.2016
 *
 */

@Component
public class FloraHellenicaImageCaptionImport<CONFIG extends FloraHellenicaImportConfigurator>
            extends FloraHellenicaImportBase<CONFIG>{

    private static final long serialVersionUID = 2629253144140992196L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaImageCaptionImport.class);

    private static final String TEXT = "Text";
    protected static Integer startPage = 316;


    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            TEXT
    });


    @Override
    protected String getWorksheetName() {
        return "Captions";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected key: " + key);
            }
        }

        makeCaption(state, line, record);

    }



    Integer plateNo = 0;
    private Taxon makeCaption(SimpleExcelTaxonImportState<CONFIG> state,
            String line,
            Map<String, String> record) {

        String text = getValue(record, TEXT);
        if (text.matches("Plate \\d\\d?")){
            plateNo = Integer.valueOf(text.substring(6));
            return null;
        }
        Pattern pattern = Pattern.compile(
                "(\\d)\\. ([A-Z][a-z\\-]+ [a-z\\-]+(?: subsp. [a-z\\-]+)?) (\\([A-Z][a-z\\-]+\\))(.*)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.matches()){
            logger.warn(line + "String caption not recognized: " + text);
            return null;
        }else{
            String subNo = matcher.group(1);
            String name = matcher.group(2);
            name = adaptName(name);
            MatchingTaxonConfigurator matchConfig = new MatchingTaxonConfigurator();
            matchConfig.setTaxonNameTitle(name);
            matchConfig.setIncludeSynonyms(false);
            List<TaxonBase> taxa = getTaxonService().findTaxaByName(matchConfig);
            TaxonBase<?> taxonBase;
            if (taxa.isEmpty()){
                logger.warn(line + "Taxon not found for name: " + name);
                return null;
            }else if (taxa.size() > 1){
                logger.warn(line + "Found more then 1 taxon for name: " + name);
            }
            taxonBase = taxa.get(0);
            Taxon taxon;
            if (taxonBase.isInstanceOf(Synonym.class)){
                taxon = CdmBase.deproxy(taxonBase, Synonym.class).getAcceptedTaxon();
                logger.warn(line + "Taxon name is synonym: " + name);
            }else{
                taxon = CdmBase.deproxy(taxonBase, Taxon.class);
            }
            TaxonDescription td = getTaxonDescription(taxon, false, true);

            String laterText = matcher.group(4);
            if (laterText.startsWith(". This")){
                laterText = laterText.substring(6);
            }
            text = matcher.group(2) + laterText;

            Feature feature = getFeature(state, FloraHellenicaTransformer.uuidFloraHellenicaTaxonInfoFeature);
            TextData textData = TextData.NewInstance(feature, text, Language.ENGLISH(), null);
            td.addElement(textData);
            Reference citation = this.getSecReference(state);
            Integer myPage = startPage + plateNo * 2;

            textData.addSource(OriginalSourceType.PrimaryTaxonomicSource, plateNo + "." +subNo
                    , "Plate", citation, "p. " + myPage);

            return taxon;

        }
    }

    /**
     * @param name
     * @return
     */
    private String adaptName(String name) {
        if (name.equals("Lathraea rhodopaea")){
            name = "Lathraea rhodopea";
        }else if (name.equals("Soldanella chrysosticha subsp. chrysosticha")){
            name = "Soldanella chrysosticta subsp. chrysosticta";
        }
        return name;
    }

}
