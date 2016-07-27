/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.iapt;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.*;
import eu.etaxonomy.cdm.model.name.*;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.*;
import eu.etaxonomy.cdm.strategy.exceptions.StringNotParsableException;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component
public class IAPTExcelImport<CONFIG extends IAPTImportConfigurator> extends SimpleExcelTaxonImport<CONFIG> {
    private static final long serialVersionUID = -747486709409732371L;
    private static final Logger logger = Logger.getLogger(IAPTExcelImport.class);


    private static UUID ROOT_UUID = UUID.fromString("4137fd2a-20f6-4e70-80b9-f296daf51d82");

    private static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();

    private final static String REGISTRATIONNO_PK= "RegistrationNo_Pk";
    private final static String HIGHERTAXON= "HigherTaxon";
    private final static String FULLNAME= "FullName";
    private final static String AUTHORSSPELLING= "AuthorsSpelling";
    private final static String LITSTRING= "LitString";
    private final static String REGISTRATION= "Registration";
    private final static String TYPE= "Type";
    private final static String CAVEATS= "Caveats";
    private final static String FULLBASIONYM= "FullBasionym";
    private final static String FULLSYNSUBST= "FullSynSubst";
    private final static String NOTESTXT= "NotesTxt";
    private final static String REGDATE= "RegDate";
    private final static String NAMESTRING= "NameString";
    private final static String BASIONYMSTRING= "BasionymString";
    private final static String SYNSUBSTSTR= "SynSubstStr";
    private final static String AUTHORSTRING= "AuthorString";

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            REGISTRATIONNO_PK, HIGHERTAXON, FULLNAME, AUTHORSSPELLING, LITSTRING, REGISTRATION, TYPE, CAVEATS, FULLBASIONYM, FULLSYNSUBST, NOTESTXT, REGDATE, NAMESTRING, BASIONYMSTRING, SYNSUBSTSTR, AUTHORSTRING});


    private Taxon makeTaxon(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state,
                            TaxonNode higherTaxonNode, boolean isSynonym) {

        String line = state.getCurrentLine() + ": ";

        String fullNameStr = getValue(record, FULLNAME);
        String nameStr = getValue(record, NAMESTRING);
        String authorStr = getValue(record, AUTHORSTRING);

        String sourceReference = getValue(record, LITSTRING);

        BotanicalName taxonName = (BotanicalName) nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, null);
        if (taxonName.isProtectedTitleCache()) {
            logger.warn(line + "Name could not be parsed: " + fullNameStr);
        } else {
            // Check Name
            if (!taxonName.getNameCache().equals(nameStr)) {
                logger.warn(line + "parsed nameCache differs from " + NAMESTRING + " : " + taxonName.getNameCache() + " <> " + nameStr);
            }
            // Check Author
            INomenclaturalReference nomRef = taxonName.getNomenclaturalReference();
            if (!nomRef.getAuthorship().getTitleCache().equals(authorStr)) {
                logger.warn(line + "parsed nomRef.authorship differs from " + AUTHORSTRING + " : " + nomRef.getAuthorship().getTitleCache() + " <> " + authorStr);
                // preserve current titleCache
                taxonName.setProtectedTitleCache(true);
                try {
                    nameParser.parseAuthors(taxonName, authorStr);
                } catch (StringNotParsableException e) {
                    logger.error("    " + authorStr + " can not be parsed");
                }
            }

            // deduplicate
            replaceAuthorNamesAndNomRef(state, taxonName);
        }

        Reference sec = state.getConfig().getSecReference();
        Taxon taxon = Taxon.NewInstance(taxonName, sec);
        getTaxonService().save(taxon);
        if(higherTaxonNode != null){
            higherTaxonNode.addChildTaxon(taxon, null, null);
            getTaxonNodeService().save(higherTaxonNode);
        }

        return taxon;

    }

    /**
     * @param state
     * @return
     */
    private TaxonNode getClassification(IAPTImportState state) {

        Classification classification = state.getClassification();
        if (classification == null){
            IAPTImportConfigurator config = state.getConfig();
            classification = Classification.NewInstance(state.getConfig().getClassificationName());
            classification.setUuid(config.getClassificationUuid());
            classification.setReference(config.getSecReference());
            classification = getClassificationService().find(state.getConfig().getClassificationUuid());
        }
        TaxonNode rootNode = state.getRootNode();
        if (rootNode == null){
            rootNode = getTaxonNodeService().find(ROOT_UUID);
        }
        if (rootNode == null){
            Reference sec = state.getSecReference();
            if (classification == null){
                String classificationName = state.getConfig().getClassificationName();
                //TODO
                Language language = Language.DEFAULT();
                classification = Classification.NewInstance(classificationName, sec, language);
                state.setClassification(classification);
                classification.setUuid(state.getConfig().getClassificationUuid());
                classification.getRootNode().setUuid(ROOT_UUID);
            }

            getClassificationService().save(classification);
            rootNode = classification.getRootNode();
        }
        return rootNode;
    }


    /**
     * @param record
     * @param originalKey
     * @return
     */
    private String getValue(HashMap<String, String> record, String originalKey) {
        String value = record.get(originalKey);
        if (! StringUtils.isBlank(value)) {
        	if (logger.isDebugEnabled()) { logger.debug(originalKey + ": " + value); }
        	value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
        	return value;
        }else{
        	return null;
        }
    }



	/**
	 *  Stores taxa records in DB
	 */
	@Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

	    boolean isSynonymOnly = false;

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        //higherTaxon
        TaxonNode higherTaxon = getHigherTaxon(record, (IAPTImportState)state);

       //Taxon
        Taxon taxon = makeTaxon(record, state, higherTaxon, isSynonymOnly);
        if (taxon == null && ! isSynonymOnly){
            logger.warn(line + "taxon could not be created and is null");
            return;
        }
        ((IAPTImportState)state).setCurrentTaxon(taxon);

        //(Notas)
        //makeNotes(record, state);

        //Syn.
        //makeSynonyms(record, state, !isSynonymOnly);


		return;
    }

    private TaxonNode getHigherTaxon(HashMap<String, String> record, IAPTImportState state) {
        String higherTaxaString = record.get(HIGHERTAXON);
        // higherTaxaString is like
        // - DICOTYLEDONES: LEGUMINOSAE: MIMOSOIDEAE
        // - FOSSIL DICOTYLEDONES: PROTEACEAE
        // - [fungi]
        // - [no group assigned]
        if(higherTaxaString.equals("[no group assigned]")){
            return null;
        }
        String[] higherTaxaNames = higherTaxaString.toLowerCase().replaceAll("[\\[\\]]", "").split(":");
        TaxonNode higherTaxonNode = null;

        ITaxonTreeNode rootNode = getClassification(state);
        for (String htn :  higherTaxaNames) {
            htn = htn.trim();
            Taxon higherTaxon = state.getHigherTaxon(htn);
            if (higherTaxon != null){
                higherTaxonNode = higherTaxon.getTaxonNodes().iterator().next();
            }else{
                BotanicalName name = makeHigherTaxonName(state, htn);
                Reference sec = state.getSecReference();
                higherTaxon = Taxon.NewInstance(name, sec);
                higherTaxonNode = rootNode.addChildTaxon(higherTaxon, sec, null);
                state.putHigherTaxon(htn, higherTaxon);
                rootNode = higherTaxonNode;
            }
        }
        return higherTaxonNode;
    }

    private BotanicalName makeHigherTaxonName(IAPTImportState state, String name) {
        // Abteilung: -phyta (bei Pflanzen), -mycota (bei Pilzen)
        // Unterabteilung: -phytina (bei Pflanzen), -mycotina (bei Pilzen)
        // Klasse: -opsida (bei Pflanzen), -phyceae (bei Algen), -mycetes (bei Pilzen)
        // Unterklasse: -idae (bei Pflanzen), -phycidae (bei Algen), -mycetidae (bei Pilzen)
        // Ordnung: -ales
        // Unterordnung: -ineae
        // Familie: -aceae
        // Unterfamilie: -oideae
        // Tribus: -eae
        // Subtribus: -inae
        Rank rank = Rank.UNKNOWN_RANK();
        if(name.matches("phyta$|mycota$")){
            rank = Rank.SECTION_BOTANY();
        } else if(name.matches("phytina$|mycotina$")){
            rank = Rank.SUBSECTION_BOTANY();
        } else if(name.matches("opsida$|phyceae$|mycetes$")){
            rank = Rank.CLASS();
        } else if(name.matches("idae$|phycidae$|mycetidae$")){
            rank = Rank.SUBCLASS();
        } else if(name.matches("ales$")){
            rank = Rank.ORDER();
        } else if(name.matches("ineae$")){
            rank = Rank.SUBORDER();
        } else if(name.matches("aceae$")){
            rank = Rank.FAMILY();
        } else if(name.matches("oideae$")){
            rank = Rank.SUBFAMILY();
        } else if(name.matches("eae$")){
            rank = Rank.TRIBE();
        } else if(name.matches("inae$")){
            rank = Rank.SUBTRIBE();
        }

        BotanicalName taxonName = BotanicalName.NewInstance(rank);
        taxonName.addSource(makeOriginalSource(state));
        taxonName.setGenusOrUninomial(StringUtils.capitalize(name));
        return taxonName;
    }


    /**
     * @param state
     * @return
     */
    private IdentifiableSource makeOriginalSource(IAPTImportState state) {
        return IdentifiableSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, state.getConfig().getSourceReference());
    }


    private Reference makeReference(IAPTImportState state, UUID uuidRef) {
        Reference ref = state.getReference(uuidRef);
        if (ref == null){
            ref = getReferenceService().find(uuidRef);
            state.putReference(uuidRef, ref);
        }
        return ref;
    }



}
