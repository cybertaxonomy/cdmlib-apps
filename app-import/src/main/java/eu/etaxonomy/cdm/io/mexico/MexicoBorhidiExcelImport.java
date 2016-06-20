// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.model.common.DefinedTerm;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Identifier;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
@Component
public class MexicoBorhidiExcelImport<CONFIG extends MexicoBorhidiImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG>{
    private static final Logger logger = Logger.getLogger(MexicoBorhidiExcelImport.class);
    private static final long serialVersionUID = -3607776356577606657L;

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "FullnameNoAuthors","OutputNameID","OutputFullNameWithAuthors"
            ,"OutputAbbreviatedTitle","OutputCollation","OutputVolume",
            "OutputIssue","OutputPage","OutputTitlePageYear","OutputYearPublished",
            "OutputBHLLink"});


    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        checkAllKeysExist(line, keys, expectedKeys);

        if (record.get("FullnameNoAuthors") == null ){
            logger.warn("No FullnameNoAuthors given: " + line);
            return;
        }

        //Name
        BotanicalName speciesName = makeName(record, state);

        //Taxon
        Reference sec = state.getConfig().getSourceReference();
        Taxon taxon = Taxon.NewInstance(speciesName, sec);
        TaxonNode rubiaceae = getHighestNode(state);
//        rubiaceae.addChildTaxon(taxon, null, null);

        //make genus
        makeGenus(state, speciesName, sec, taxon, rubiaceae);

        //add tropicos identifier
        String tropicosId = record.get("OutputNameID");
        if (isNotBlank(tropicosId)){
            String tropicosIdTypeLabel = "Tropicos Name Identifier";
            UUID uuid = MexicoConabioTransformer.uuidTropicosNameIdentifier;
            TermVocabulary<DefinedTerm> voc = null;  //for now it goes to user defined voc
            DefinedTerm identifierType = this.getIdentiferType(state, uuid, tropicosIdTypeLabel, tropicosIdTypeLabel, null, voc);
            Identifier<Taxon> identifier = Identifier.NewInstance(tropicosId, identifierType);
            taxon.addIdentifier(identifier);
        }

    }


    private Classification classification;
    private TaxonNode rubiaceaeNode;
    /**
     * @return
     */
    private TaxonNode getHighestNode(SimpleExcelTaxonImportState<CONFIG> state) {
        if (rubiaceaeNode == null){
            MexicoBorhidiImportConfigurator config = state.getConfig();
            classification = Classification.NewInstance(state.getConfig().getClassificationName());
            classification.setUuid(config.getClassificationUuid());
            classification.setReference(config.getSourceReference());
            BotanicalName nameRubiaceae = BotanicalName.NewInstance(Rank.FAMILY());
            nameRubiaceae.setGenusOrUninomial("Rubiaceae");
            Taxon rubiaceaeTaxon = Taxon.NewInstance(nameRubiaceae, classification.getReference());
            rubiaceaeNode = classification.addChildTaxon(rubiaceaeTaxon, null, null);
            getClassificationService().save(classification);
        }
        return rubiaceaeNode;
    }

    /**
     * @param record
     * @param state
     * @return
     */
    private BotanicalName makeName(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";

        String fullNameStr = getValue(record, "OutputFullNameWithAuthors");
        String volume = getValue(record, "OutputVolume");
        String issue = getValue(record, "OutputIssue");
        String page = getValue(record, "OutputPage");
        String titleYear = getValue(record, "OutputTitlePageYear");
        String publishedYear = getValue(record, "OutputYearPublished");
        String refAbbrevTitle = getValue(record, "OutputAbbreviatedTitle");
        String outputCollation = getValue(record, "OutputCollation");

        BotanicalName name = (BotanicalName)nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + fullNameStr );
        }else{
            replaceAuthorNames(state, name);
        }

        String[] volumeDetail = makeVolumeDetail(outputCollation);

        if (refAbbrevTitle != null){
            if (volume != null){
                Reference journal = state.getReference(refAbbrevTitle);
                if (journal == null){
                    journal = ReferenceFactory.newJournal();
                    journal.setAbbrevTitle(refAbbrevTitle);
                    state.putReference(refAbbrevTitle, journal);
                }
                Reference article = ReferenceFactory.newArticle();

                //volume + issue
    //            if (isNotBlank(issue)){
    //                volume = volume + "(" + issue + ")";
    //            }
                volume = volumeDetail[0];
                String detail = volumeDetail.length > 1 ? volumeDetail[1].trim() : null;

                //            String detail = page;
                name.setNomenclaturalMicroReference(detail);

                article.setVolume(CdmUtils.Ne(volume));
                article.setInReference(journal);

                titleYear = (isBlank(publishedYear)? titleYear : "\"" + titleYear + "\"[" + publishedYear + "]");
                article.setDatePublished(TimePeriodParser.parseString(titleYear));

                Reference existingArticle = state.getReference(article.getTitleCache());
                if (existingArticle != null){
                    name.setNomenclaturalReference(existingArticle);
                }else{
                    name.setNomenclaturalReference(article);
                    state.putReference(article.getTitleCache(), article);
                }
            }else{

                Reference book = ReferenceFactory.newBook();
                book.setAbbrevTitle(refAbbrevTitle);

                if (volumeDetail.length > 1){
                    logger.warn(line + "Book outputCollation has volume part");
                }

                //year
                titleYear = (isBlank(publishedYear)? titleYear : "\"" + titleYear + "\"[" + publishedYear + "]");
                book.setDatePublished(TimePeriodParser.parseString(titleYear));

                //deduplicate
                Reference existingBook = state.getReference(book.getTitleCache());
                if (existingBook != null){
                    name.setNomenclaturalReference(existingBook);
                }else{
                    name.setNomenclaturalReference(book);
                    state.putReference(book.getTitleCache(), book);
                }

                //micro ref
                String detail = outputCollation;
                //String detail = page;
                name.setNomenclaturalMicroReference(detail);
            }
        }

        addNomRefExtension(state, name);


        //add protologue
        String bhlLink = record.get("OutputBHLLink");
        if (isNotBlank(bhlLink)){
            URI uri;
            try {
                uri = new URI(bhlLink);
                Media media = Media.NewInstance(uri, null, null, null);
                TaxonNameDescription desc = TaxonNameDescription.NewInstance(name);
                desc.setTitleCache("Protologue for " + name.getNameCache(), true);
                DescriptionElementBase elem = TextData.NewInstance(Feature.PROTOLOGUE());
                elem.addMedia(media);
                desc.addElement(elem);
            } catch (URISyntaxException e) {
                logger.warn(line + "URI could not be parsed: " + e.getMessage());
            }
        }

        return name;
    }

    /**
     * @param outputCollation
     * @return
     */
    private String[] makeVolumeDetail(String outputCollation) {
        if (outputCollation == null){
            return new String[0];
        }else{
            String[] split = outputCollation.split(":");
            return split;
        }
    }

    /**
     * @param state
     * @param referencedName
     */
    private void addNomRefExtension(SimpleExcelTaxonImportState<CONFIG> state, BotanicalName name) {
        String newExtensionStr = name.getFullTitleCache() + " - BORHIDI";
        UUID uuidNomRefExtension = MexicoConabioTransformer.uuidNomRefExtension;
        for (Extension extension : name.getExtensions()){
            if (extension.getType().getUuid().equals(uuidNomRefExtension)){
                extension.setValue(extension.getValue() + "\n" + newExtensionStr);
                return;
            }
        }
        String label = "Nomenclatural reference in Sources";
        String abbrev = "Nom. ref. src.";
        ExtensionType extensionType = getExtensionType(state, uuidNomRefExtension, label, label, abbrev);
        Extension.NewInstance(name, newExtensionStr, extensionType);
    }


}
