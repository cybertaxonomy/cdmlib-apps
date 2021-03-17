/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Identifier;
import eu.etaxonomy.cdm.model.media.ExternalLinkType;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
@Component
public class MexicoBorhidiExcelImport<CONFIG extends MexicoBorhidiImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG>{
    private static final Logger logger = Logger.getLogger(MexicoBorhidiExcelImport.class);
    private static final long serialVersionUID = -3607776356577606657L;

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "FullnameNoAuthors","OutputNameID","OutputFullNameWithAuthors","RefType"
            ,"OutputAbbreviatedTitle","OutputCollation","OutputVolume",
            "OutputIssue","OutputPage","OutputTitlePageYear","OutputYearPublished",
            "OutputBHLLink"});

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        checkAllKeysExist(line, keys, expectedKeys);

        if (record.get("FullnameNoAuthors") == null ){
            logger.warn("No FullnameNoAuthors given: " + line);
            return;
        }

        //Name
        IBotanicalName speciesName = makeName(record, state);

        //Taxon
        Reference sec = state.getConfig().getSecReference();
        Taxon taxon = Taxon.NewInstance(speciesName, sec);
        TaxonNode rubiaceae = getHighestNode(state);

        taxon.addSource(makeOriginalSource(state));

        //make genus
        makeGenus(state, speciesName, sec, taxon, rubiaceae);
    }


    private Classification classification;
    private TaxonNode rubiaceaeNode;

    private TaxonNode getHighestNode(SimpleExcelTaxonImportState<CONFIG> state) {
        if (rubiaceaeNode == null){
            MexicoBorhidiImportConfigurator config = state.getConfig();
            classification = Classification.NewInstance(state.getConfig().getClassificationName());
            classification.setUuid(config.getClassificationUuid());
            classification.setReference(config.getSecReference());
            IBotanicalName nameRubiaceae = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
            nameRubiaceae.setGenusOrUninomial("Rubiaceae");
            Taxon rubiaceaeTaxon = Taxon.NewInstance(nameRubiaceae, classification.getReference());
            rubiaceaeNode = classification.addChildTaxon(rubiaceaeTaxon, null, null);
            getClassificationService().save(classification);
        }
        return rubiaceaeNode;
    }

    private IBotanicalName makeName(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";

        String fullNameStr = getValue(record, "OutputFullNameWithAuthors");
//        String volume = getValue(record, "OutputVolume");
//        String issue = getValue(record, "OutputIssue");
//        String page = getValue(record, "OutputPage");
        String titleYear = getValue(record, "OutputTitlePageYear");
        String publishedYear = getValue(record, "OutputYearPublished");
        String refAbbrevTitle = getValue(record, "OutputAbbreviatedTitle");
        String outputCollation = getValue(record, "OutputCollation");
        String refType = getValue(record, "RefType");


        TaxonName name = (TaxonName)nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        if (name.isProtectedTitleCache()){
            //for the 2 ined. names
            name = (TaxonName)nameParser.parseReferencedName(fullNameStr, NomenclaturalCode.ICNAFP, Rank.SPECIES());
        }
        if (name.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + fullNameStr );
        }else{
            replaceAuthorNamesAndNomRef(state, name);
        }

        if (refAbbrevTitle != null){
            String[] volumeDetail = makeVolumeDetail(outputCollation);
            String detail;
            String volume = null;
            if (volumeDetail.length > 1){
                volume = volumeDetail[0].trim();
                detail = volumeDetail[1].trim();
            }else{
                detail = volumeDetail[0].trim();
            }

            refAbbrevTitle = refAbbrevTitle.trim();
            boolean isArticle = "A".equalsIgnoreCase(refType);

            if (isArticle){
                if (! "A".equalsIgnoreCase(refType)){
                    logger.warn(line + "RefType problem with article " + refType);
                }

                Reference journal = state.getReference(refAbbrevTitle);
                if (journal == null){
                    journal = ReferenceFactory.newJournal();
                    journal.setAbbrevTitle(refAbbrevTitle);
                    state.putReference(refAbbrevTitle, journal);
                    journal.addSource(makeOriginalSource(state));

                }
                Reference article = ReferenceFactory.newArticle();


                //            String detail = page;
                name.setNomenclaturalMicroReference(detail);

                article.setVolume(CdmUtils.Ne(volume));
                article.setInReference(journal);

                titleYear = (isBlank(publishedYear)? titleYear : UTF8.QUOT_DBL_LOW9 + titleYear + UTF8.QUOT_DBL_HIGH_REV9 + "[" + publishedYear + "]");
                article.setDatePublished(TimePeriodParser.parseStringVerbatim(titleYear));

                article.setAuthorship(name.getCombinationAuthorship());

                Reference existingArticle = state.getReference(article.getTitleCache());
                if (existingArticle != null){
                    name.setNomenclaturalReference(existingArticle);
                }else{
                    name.setNomenclaturalReference(article);
                    state.putReference(article.getTitleCache(), article);
                    article.addSource(makeOriginalSource(state));
                }
            }else{
                if (! "B".equalsIgnoreCase(refType)){
                    logger.warn(line + "RefType problem with book" + refType);
                }

                Reference book = ReferenceFactory.newBook();
                book.setAbbrevTitle(refAbbrevTitle);

                //year
                titleYear = (isBlank(publishedYear)? titleYear : UTF8.QUOT_DBL_LOW9 + titleYear + UTF8.QUOT_DBL_HIGH_REV9 + "[" + publishedYear + "]");
                book.setDatePublished(TimePeriodParser.parseStringVerbatim(titleYear));

                book.setAuthorship(name.getCombinationAuthorship());

                //deduplicate
                Reference existingBook = state.getReference(book.getTitleCache());
                if (existingBook != null){
                    name.setNomenclaturalReference(existingBook);
                }else{
                    name.setNomenclaturalReference(book);
                    state.putReference(book.getTitleCache(), book);
                }

                book.setVolume(volume);

                //String detail = page;
                name.setNomenclaturalMicroReference(detail);
            }
        }

        addNomRefExtension(state, name);

        //add protologue
        String bhlLink = record.get("OutputBHLLink");
        if (isNotBlank(bhlLink)){
            try {
                URI uri = new URI(bhlLink);
                name.addProtologue(uri, null, ExternalLinkType.WebSite);
            } catch (URISyntaxException e) {
                logger.warn(line + "URI could not be parsed: " + e.getMessage());
            }
        }

        //add tropicos identifier
        String tropicosId = record.get("OutputNameID");
        if (isNotBlank(tropicosId)){
            String tropicosIdTypeLabel = "Tropicos Name Identifier";
            UUID uuid = DefinedTerm.uuidTropicosNameIdentifier;
            TermVocabulary<DefinedTerm> voc = null;  //for now it goes to user defined voc
            DefinedTerm identifierType = this.getIdentiferType(state, uuid, tropicosIdTypeLabel, tropicosIdTypeLabel, null, voc);
            Identifier<Taxon> identifier = Identifier.NewInstance(tropicosId, identifierType);
            name.addIdentifier(identifier);
        }

        name.addSource(makeOriginalSource(state));


        return name;
    }

    private String[] makeVolumeDetail(String outputCollation) {
        if (outputCollation == null){
            return new String[0];
        }else{
            String[] split = outputCollation.split(":");
            return split;
        }
    }

    private void addNomRefExtension(SimpleExcelTaxonImportState<CONFIG> state, IBotanicalName name) {
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
        Extension.NewInstance((TaxonName)name, newExtensionStr, extensionType);
    }
}
