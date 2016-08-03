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
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.*;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component("iAPTExcelImport")
public class IAPTExcelImport<CONFIG extends IAPTImportConfigurator> extends SimpleExcelTaxonImport<CONFIG> {
    private static final long serialVersionUID = -747486709409732371L;
    private static final Logger logger = Logger.getLogger(IAPTExcelImport.class);
    public static final String ANNOTATION_MARKER_STRING = "[*]";


    private static UUID ROOT_UUID = UUID.fromString("4137fd2a-20f6-4e70-80b9-f296daf51d82");

    private static NonViralNameParserImpl nameParser = NonViralNameParserImpl.NewInstance();

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

    private static final Pattern nomRefTokenizeP = Pattern.compile("^(.*):\\s([^\\.:]+)\\.(.*)$");
    private static final Pattern nomRefPubYearExtractP = Pattern.compile("(.*?)(1[7,8,9][0-9]{2}).*$|^.*?[0-9]{1,2}([\\./])[0-1]?[0-9]\\3([0-9]{2})\\.$"); // 1700 - 1999

    private MarkerType markerTypeFossil = null;
    private Rank rankUnrankedPseudoClass = null;

    private Taxon makeTaxon(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state,
                            TaxonNode higherTaxonNode, boolean isSynonym, boolean isFossil) {

        String line = state.getCurrentLine() + ": ";

        String titleCacheStr = getValue(record, FULLNAME, true);
        String nameStr = getValue(record, NAMESTRING, true);
        String authorStr = getValue(record, AUTHORSTRING, true);
        String nomRefStr = getValue(record, LITSTRING, true);
        String authorsSpelling = getValue(record, AUTHORSSPELLING, true);
        String notesTxt = getValue(record, NOTESTXT, true);

        String nomRefTitle = null;
        String nomRefDetail = null;
        String nomRefPupDate = null;
        String nomRefPupYear = null;

        // preprocess nomRef: separate citation, reference detail, publishing date
        if(!StringUtils.isEmpty(nomRefStr)){
            nomRefStr = nomRefStr.trim();
            Matcher m = nomRefTokenizeP.matcher(nomRefStr);
            if(m.matches()){
                nomRefTitle = m.group(1);
                nomRefDetail = m.group(2);
                nomRefPupDate = m.group(3);

                // nomRefDetail.replaceAll("[\\:\\.\\s]", ""); // TODO integrate into nomRefTokenizeP
                Matcher m2 = nomRefPubYearExtractP.matcher(nomRefPupDate);
                if(m2.matches()){
                    nomRefPupYear = m2.group(2);
                    if(nomRefPupYear == null){
                        nomRefPupYear = m2.group(4);
                    }
                    if(nomRefPupYear == null){
                        logger.error("nomRefPupYear in " + nomRefStr + " is  NULL" );
                    }
                    if(nomRefPupYear.length() == 2 ){
                        // it is an abbreviated year from the 19** years
                        nomRefPupYear = "19" + nomRefPupYear;
                    }
                    nomRefTitle = nomRefTitle + ": " + nomRefDetail + ". " + nomRefPupYear + ".";
                } else {
                    logger.warn("Pub year not found in " + nomRefStr );
                    // FIXME in in J. Eur. Orchideen 30: 128. 30.09.97 (Vorabdr.).

                }

            } else {
                nomRefTitle = nomRefStr;
            }
        }

        BotanicalName taxonName;
        // cache field for the taxonName.titleCache
        String taxonNameTitleCache = null;
        Map<String, AnnotationType> nameAnnotations = new HashMap<>();

        // TitleCache preprocessing
        if(titleCacheStr.endsWith(ANNOTATION_MARKER_STRING) || (authorStr != null && authorStr.endsWith(ANNOTATION_MARKER_STRING))){
            nameAnnotations.put("Author abbreviation not checked.", AnnotationType.EDITORIAL());
            titleCacheStr = titleCacheStr.replace(ANNOTATION_MARKER_STRING, "").trim();
            authorStr = authorStr.replace(ANNOTATION_MARKER_STRING, "").trim();
        }

        // parse the full taxon name
        if(!StringUtils.isEmpty(nomRefTitle)){
            String referenceSeparator = nomRefTitle.startsWith("in ") ? " " : ", ";
            String taxonFullNameStr = titleCacheStr + referenceSeparator + nomRefTitle;
            logger.debug(":::::" + taxonFullNameStr);
            taxonName = (BotanicalName) nameParser.parseReferencedName(taxonFullNameStr, NomenclaturalCode.ICNAFP, null);
        } else {
            taxonName = (BotanicalName) nameParser.parseFullName(titleCacheStr, NomenclaturalCode.ICNAFP, null);
        }

        taxonNameTitleCache = taxonName.getTitleCache().trim();
        if (taxonName.isProtectedTitleCache()) {
            logger.warn(line + "Name could not be parsed: " + titleCacheStr);
        } else {

            boolean doRestoreTitleCacheStr = false;

            // Check if titleCache and nameCache are plausible
            String titleCacheCompareStr = titleCacheStr;
            String nameCache = taxonName.getNameCache();
            String nameCompareStr = nameStr;
            if(taxonName.isBinomHybrid()){
                titleCacheCompareStr = titleCacheCompareStr.replace(" x ", " ×");
                nameCompareStr = nameCompareStr.replace(" x ", " ×");
            }
            if(taxonName.isBinomHybrid()){
                titleCacheCompareStr = titleCacheCompareStr.replaceAll("^X ", "× ");
                nameCompareStr = nameCompareStr.replace("^X ", "× ");
            }
            if (!taxonNameTitleCache.equals(titleCacheCompareStr)) {
                logger.warn(line + "The generated titleCache differs from the imported string : " + taxonNameTitleCache + " <> " + titleCacheStr + " will restore original titleCacheStr");
                doRestoreTitleCacheStr = true;
            }
            if (!nameCache.trim().equals(nameCompareStr)) {
                logger.warn(line + "The parsed nameCache differs from " + NAMESTRING + " : " + nameCache + " <> " + nameCompareStr);
            }

            //  Author
            //nameParser.handleAuthors(taxonName, titleCacheStr, authorStr);
            //if (!titleCacheStr.equals(taxonName.getTitleCache())) {
            //    logger.warn(line + "titleCache has changed after setting authors, will restore original titleCacheStr");
            //    doRestoreTitleCacheStr = true;
            //}

            if(doRestoreTitleCacheStr){
                taxonName.setTitleCache(titleCacheStr, true);
            }

            // deduplicate
            replaceAuthorNamesAndNomRef(state, taxonName);
        }

        // Annotations
        if(!nameAnnotations.isEmpty()){
            for(String text : nameAnnotations.keySet()){
                taxonName.addAnnotation(Annotation.NewInstance(text, nameAnnotations.get(text), Language.DEFAULT()));
            }
            getNameService().save(taxonName);
        }
        if(!StringUtils.isEmpty(notesTxt)){
            notesTxt = notesTxt.replace("Notes: ", "").trim();
            taxonName.addAnnotation(Annotation.NewInstance(notesTxt, AnnotationType.EDITORIAL(), Language.DEFAULT()));
        }

        // Namerelations
        if(!StringUtils.isEmpty(authorsSpelling)){
            authorsSpelling = authorsSpelling.replaceFirst("Author's spelling:", "").replaceAll("\"", "").trim();

            String[] authorSpellingTokens = StringUtils.split(authorsSpelling, " ");
            String[] nameStrTokens = StringUtils.split(nameStr, " ");

            ArrayUtils.reverse(authorSpellingTokens);
            ArrayUtils.reverse(nameStrTokens);

            for (int i = 0; i < nameStrTokens.length; i++){
                if(i < authorSpellingTokens.length){
                    nameStrTokens[i] = authorSpellingTokens[i];
                }
            }
            ArrayUtils.reverse(nameStrTokens);

            String misspelledNameStr = StringUtils.join (nameStrTokens, ' ');
            // build the fullnameString of the misspelled name
            misspelledNameStr = taxonNameTitleCache.replace(nameStr, misspelledNameStr);

            TaxonNameBase misspelledName = (BotanicalName) nameParser.parseReferencedName(misspelledNameStr, NomenclaturalCode.ICNAFP, null);
            misspelledName.addRelationshipToName(taxonName, NameRelationshipType.MISSPELLING(), null);
            getNameService().save(misspelledName);
        }

        Reference sec = state.getConfig().getSecReference();
        Taxon taxon = Taxon.NewInstance(taxonName, sec);

        // Markers
        if(isFossil){
            taxon.addMarker(Marker.NewInstance(markerTypeFossil(), true));
        }

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
    private TaxonNode getClassificationRootNode(IAPTImportState state) {

     //   Classification classification = state.getClassification();
     //   if (classification == null){
     //       IAPTImportConfigurator config = state.getConfig();
     //       classification = Classification.NewInstance(state.getConfig().getClassificationName());
     //       classification.setUuid(config.getClassificationUuid());
     //       classification.setReference(config.getSecReference());
     //       classification = getClassificationService().find(state.getConfig().getClassificationUuid());
     //   }
        TaxonNode rootNode = state.getRootNode();
        if (rootNode == null){
            rootNode = getTaxonNodeService().find(ROOT_UUID);
        }
        if (rootNode == null){
            Classification classification = state.getClassification();
            if (classification == null){
                Reference sec = state.getSecReference();
                String classificationName = state.getConfig().getClassificationName();
                Language language = Language.DEFAULT();
                classification = Classification.NewInstance(classificationName, sec, language);
                state.setClassification(classification);
                classification.setUuid(state.getConfig().getClassificationUuid());
                classification.getRootNode().setUuid(ROOT_UUID);
                getClassificationService().save(classification);
            }
            rootNode = classification.getRootNode();
            state.setRootNode(rootNode);
        }
        return rootNode;
    }


    /**
     * @param record
     * @param originalKey
     * @param doUnescapeHtmlEntities
     * @return
     */
    private String getValue(HashMap<String, String> record, String originalKey, boolean doUnescapeHtmlEntities) {
        String value = record.get(originalKey);
        if (! StringUtils.isBlank(value)) {
        	if (logger.isDebugEnabled()) {
        	    logger.debug(originalKey + ": " + value);
        	}
        	value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
            if(doUnescapeHtmlEntities){
                value = StringEscapeUtils.unescapeHtml(value);
            }
        	return value.trim();
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

        String lineNumber = state.getCurrentLine() + ": ";
        logger.setLevel(Level.DEBUG);
        HashMap<String, String> record = state.getOriginalRecord();
        logger.debug(lineNumber + record.toString());

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(lineNumber + "Unexpected Key: " + key);
            }
        }

        String reg_id = record.get(REGISTRATIONNO_PK);

        //higherTaxon
        String higherTaxaString = record.get(HIGHERTAXON);
        boolean isFossil = false;
        if(higherTaxaString.startsWith("FOSSIL ")){
            higherTaxaString = higherTaxaString.replace("FOSSIL ", "");
            isFossil = true;
        }
        TaxonNode higherTaxon = getHigherTaxon(higherTaxaString, (IAPTImportState)state);

       //Taxon
        Taxon taxon = makeTaxon(record, state, higherTaxon, isSynonymOnly, isFossil);
        if (taxon == null && ! isSynonymOnly){
            logger.warn(lineNumber + "taxon could not be created and is null");
            return;
        }
        ((IAPTImportState)state).setCurrentTaxon(taxon);

        //Syn.
        //makeSynonyms(record, state, !isSynonymOnly);


		return;
    }

    private TaxonNode getHigherTaxon(String higherTaxaString, IAPTImportState state) {

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

        ITaxonTreeNode rootNode = getClassificationRootNode(state);
        for (String htn :  higherTaxaNames) {
            htn = StringUtils.capitalize(htn.trim());
            Taxon higherTaxon = state.getHigherTaxon(htn);
            if (higherTaxon != null){
                higherTaxonNode = higherTaxon.getTaxonNodes().iterator().next();
            }else{
                BotanicalName name = makeHigherTaxonName(state, htn);
                Reference sec = state.getSecReference();
                higherTaxon = Taxon.NewInstance(name, sec);
                getTaxonService().save(higherTaxon);
                higherTaxonNode = rootNode.addChildTaxon(higherTaxon, sec, null);
                state.putHigherTaxon(htn, higherTaxon);
                getClassificationService().saveTreeNode(higherTaxonNode);
            }
            rootNode = higherTaxonNode;
        }
        return higherTaxonNode;
    }

    private BotanicalName makeHigherTaxonName(IAPTImportState state, String name) {

        Rank rank = guessRank(name);

        BotanicalName taxonName = BotanicalName.NewInstance(rank);
        taxonName.addSource(makeOriginalSource(state));
        taxonName.setGenusOrUninomial(StringUtils.capitalize(name));
        return taxonName;
    }

    private Rank guessRank(String name) {

        // normalize
        name = name.replaceAll("\\(.*\\)", "").trim();

        if(name.matches("^Plantae$|^Fungi$")){
           return Rank.KINGDOM();
        } else if(name.matches("Incertae sedis$|^No group assigned$")){
           return Rank.FAMILY();
        } else if(name.matches(".*phyta$|.*mycota$")){
           return Rank.SECTION_BOTANY();
        } else if(name.matches(".*phytina$|.*mycotina$")){
           return Rank.SUBSECTION_BOTANY();
        } else if(name.matches("Gymnospermae$|.*ones$")){ // Monocotyledones, Dicotyledones
            return rankUnrankedPseudoClass();
        } else if(name.matches(".*opsida$|.*phyceae$|.*mycetes$|.*ones$|^Musci$|^Hepaticae$")){
           return Rank.CLASS();
        } else if(name.matches(".*idae$|.*phycidae$|.*mycetidae$")){
           return Rank.SUBCLASS();
        } else if(name.matches(".*ales$")){
           return Rank.ORDER();
        } else if(name.matches(".*ineae$")){
           return Rank.SUBORDER();
        } else if(name.matches(".*aceae$")){
            return Rank.FAMILY();
        } else if(name.matches(".*oideae$")){
           return Rank.SUBFAMILY();
        } else
        //    if(name.matches(".*eae$")){
        //    return Rank.TRIBE();
        // } else
            if(name.matches(".*inae$")){
           return Rank.SUBTRIBE();
        } else if(name.matches(".*ae$")){
           return Rank.FAMILY();
        }
        return Rank.UNKNOWN_RANK();
    }

    private Rank rankUnrankedPseudoClass() {

        if(rankUnrankedPseudoClass == null){
            rankUnrankedPseudoClass = Rank.NewInstance(RankClass.Suprageneric, "Unranked pseudo class", " ", " ");
            getTermService().save(rankUnrankedPseudoClass);
        }
        return rankUnrankedPseudoClass;
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

    private MarkerType markerTypeFossil(){
        if(this.markerTypeFossil == null){
            markerTypeFossil = MarkerType.NewInstance("isFossilTaxon", "isFossil", null);
            getTermService().save(this.markerTypeFossil);
        }
        return markerTypeFossil;
    }



}
