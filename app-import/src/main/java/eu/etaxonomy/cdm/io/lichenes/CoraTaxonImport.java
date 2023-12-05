/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.lichenes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.strategy.parser.BibliographicAuthorParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImplRegExBase;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Lichenes genus Cora taxon import.
 *
 * @author a.mueller
 * @since 2023-11-23
 */
@Component
public class CoraTaxonImport<CONFIG extends CoraImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 1370751143085038819L;
    private static final Logger logger = LogManager.getLogger();

    private static final String SECUNDUM_PAGE = "SECUNDUM PAGE";
    private static final String SECUNDUM_DOI_UNIQUE = "SECUNDUM DOI (UNIQUE)";
    private static final String SECUNDUM_VOLUME_PAGE_RANGE = "SECUNDUM VOLUME/PAGE RANGE";
    private static final String SECUNDUM_JOURNAL = "SECUNDUM JOURNAL";
    private static final String SECUNDUM_TITLE = "SECUNDUM TITLE";
    private static final String SECUNDUM_AUTHORS = "SECUNDUM AUTHORS";
    private static final String SECUNDUM_REFERENCE = "SECUNDUM REFERENCE";
    private static final String ACCEPTED_NAME_STRING = "ACCEPTED NAME STRING";
    private static final String STATUS = "STATUS";
    private static final String IF_URL = "IF URL";
    private static final String IF_IDENTIFIER = "IF IDENTIFIER";
    private static final String PAGE = "PAGE";
    private static final String DOI_UNIQUE = "DOI (UNIQUE)";
    private static final String PUBLISHER = "PUBLISHER";
    private static final String BOOK_TITLE = "BOOK TITLE";
    private static final String BOOK_EDITOR_S = "BOOK EDITOR(S)";
    private static final String VOLUME_PAGE_RANGE = "VOLUME/PAGE RANGE";
    private static final String JOURNAL = "JOURNAL";
    private static final String TITLE = "TITLE";
    private static final String AUTHORS = "AUTHORS";
    private static final String REFERENCE_UNIQUE = "REFERENCE (UNIQUE)";
    private static final String SUBCLASS = "SUBCLASS";
    private static final String YEAR = "YEAR";
    private static final String AUTHOR_S = "AUTHOR(S)";
    private static final String SPECIES = "SPECIES";
    private static final String GENUS = "GENUS";
    private static final String NAME_STRING_UNIQUE = "NAME STRING (UNIQUE)";

    private static final String oWs = "\\s+";
    private static final String exString = "(ex\\.?)";


	private static UUID coraNodeUuid = UUID.fromString("9a07b581-b37e-4bc4-bd5e-3181e8307354");

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            NAME_STRING_UNIQUE,GENUS,SPECIES,AUTHOR_S,YEAR,SUBCLASS,REFERENCE_UNIQUE,AUTHORS,
            TITLE,JOURNAL,VOLUME_PAGE_RANGE,BOOK_EDITOR_S,BOOK_TITLE,PUBLISHER,
            DOI_UNIQUE,PAGE,IF_IDENTIFIER,
            IF_URL,STATUS,ACCEPTED_NAME_STRING,
            SECUNDUM_REFERENCE,SECUNDUM_AUTHORS,SECUNDUM_TITLE,SECUNDUM_JOURNAL,SECUNDUM_VOLUME_PAGE_RANGE,
            SECUNDUM_DOI_UNIQUE,SECUNDUM_PAGE
    });

    private Reference sourceReference;
    private Reference secReference;

    private Map<String,Taxon> nameTaxonMap = new HashMap<>();
    private Map<String,Taxon> fullNameTaxonMap = new HashMap<>();
    private Map<String,Reference> referenceMap = new HashMap<>();

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "cora_names_EDIT";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = getLine(state, 100);
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        makeTaxon(state, line, record);
    }

    private void makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {

        String pageStr = getValue(record, PAGE);
        String ifIdentifierStr = getValue(record, IF_IDENTIFIER);
        @SuppressWarnings("unused")  //not needed as it can be computed by the IdentifierType by the url template
        String ifUrlStr = getValue(record, IF_URL);
        String statusStr = getValue(record, STATUS);
        String secPageStr = getValue(record, SECUNDUM_PAGE);

        //taxon name
        TaxonName taxonName = makeTaxonName(state, record, line);
        String taxonNameStr = taxonName.getTitleCache();

        //nom. ref.
        Reference nomRef = makeNomRef(record);
        taxonName.setNomenclaturalReference(nomRef);
        taxonName.setNomenclaturalMicroReference(pageStr);

        //Index Fungorum identifier
        if (isNotBlank(ifIdentifierStr)) {
            IdentifierType indexFungorumIdType = getIdentiferType(state, IdentifierType.uuidIndexFungorumIdentifier, null, null, null, null);
            //TODO implement in term loading
            indexFungorumIdType.setUrlPattern("https://www.indexfungorum.org/names/NamesRecord.asp?RecordID={@ID}");
            taxonName.addIdentifier(ifIdentifierStr, indexFungorumIdType);
        }

        //deduplicate
        replaceNameAuthorsAndReferences(state, taxonName);

        //taxon
        TaxonBase<?> taxonBase;
        Reference secRef = makeSecRef(record, line, taxonNameStr);

        if (isBlank(statusStr) || "accepted".equals(statusStr)) {

            Taxon taxon = Taxon.NewInstance(taxonName, secRef);
            taxonBase = taxon;
            addToParentNode(state, taxon, nameTaxonMap, line);
            nameTaxonMap.put(taxonName.getNameCache(), taxon);
            String fullTitle = taxonName.getTitleCache();
            fullNameTaxonMap.put(fullTitle, taxon);
        }else {
            Synonym syn = Synonym.NewInstance(taxonName, secRef);
            taxonBase = syn;
            addSynonymToAccepted(record, syn, fullNameTaxonMap, line);
        }
        taxonBase.setSecMicroReference(secPageStr);

        //source
        taxonBase.addSource(makeOriginalSource(state));
        taxonName.addSource(makeOriginalSource(state));

        getTaxonService().saveOrUpdate(taxonBase);

        return;
    }

    private TaxonName makeTaxonName(SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record, String line) {

        String genusStr = getValue(record, GENUS);
        String speciesStr = getValue(record, SPECIES);

        //TODO implement in-authors in parser
        String taxonNameStr = CdmUtils.concat(" ", genusStr, speciesStr);
        TaxonName taxonName = (TaxonName)parser.parseSimpleName(taxonNameStr, state.getConfig().getNomenclaturalCode(), Rank.SPECIES());
        checkParsed(TaxonName.castAndDeproxy(taxonName), taxonNameStr, line);

        handleNomAuthors(record, taxonName);
        return taxonName;
    }

    private void handleNomAuthors(Map<String, String> record, TaxonName taxonName) {

        String nomAuthorStr = getValue(record, AUTHOR_S);

        String basStart = "\\(";
        String basEnd = "\\)";

        //copied from NonViralNameParser.parseAuthorsChecked()
        TeamOrPersonBase<?>[] authors = new TeamOrPersonBase[6];
        Integer[] years = new Integer[4];
        int authorShipStart = 0;
        String authorTeam = NonViralNameParserImplRegExBase.authorTeam;
        String authorAndExAndInTeam = "(" + authorTeam + oWs + exString + oWs + ")?" + authorTeam +
                "("+oWs+"in" + oWs + authorTeam + ")?" ;
        String fungiBasionymAuthor = basStart + "(" + authorAndExAndInTeam + ")" + basEnd;
        Pattern basionymPattern = Pattern.compile(fungiBasionymAuthor);
        Matcher basionymMatcher = basionymPattern.matcher(nomAuthorStr);

        if (basionymMatcher.find(0)){

            String basString = basionymMatcher.group();
            basString = basString.replaceFirst(basStart, "");
            basString = basString.replaceAll(basEnd, "").trim();
            authorShipStart = basionymMatcher.end(1)+1;

            TeamOrPersonBase<?>[] basAuthors = new TeamOrPersonBase[3];
            Integer[] basYears = new Integer[2];
            authorsAndExAndIn(basString, basAuthors, basYears);
            authors[3]= basAuthors[0];   //basionym author
            years[2] = basYears[0];
            authors[4]= basAuthors[1];   //basionym ex-author
            years[3] = basYears[1];
            authors[5]= basAuthors[2];   //basionym in-author

        }
        if (nomAuthorStr.length() >= authorShipStart){
            TeamOrPersonBase<?>[] combinationAuthors = new TeamOrPersonBase[3];
            Integer[] combinationYears = new Integer[2];
            authorsAndExAndIn(nomAuthorStr.substring(authorShipStart), combinationAuthors, combinationYears);
            authors[0]= combinationAuthors[0] ;  //combination author
            years[0] = combinationYears[0];
            authors[1]= combinationAuthors[1];   //combination ex-author
            years[1] = combinationYears[1];
            authors[2]= combinationAuthors[2];   //combination in-author
        }

        //fill name with authors
        taxonName.setCombinationAuthorship(authors[0]);
        taxonName.setExCombinationAuthorship(authors[1]);
        taxonName.setInCombinationAuthorship(authors[2]);
        taxonName.setBasionymAuthorship(authors[3]);
        taxonName.setExBasionymAuthorship(authors[4]);
        taxonName.setInBasionymAuthorship(authors[5]);
    }

    /**
     * copied from {@link NonViralNameParserImpl#authorsAndEx()}
     */
    private void authorsAndExAndIn (String authorShipStringOrig, TeamOrPersonBase<?>[] authors, Integer[] years){
        Pattern exAuthorPattern = Pattern.compile(oWs + exString);
        String authorShipString = authorShipStringOrig.trim();
        authorShipString = authorShipString.replaceFirst(oWs + "ex" + oWs, " ex. " );

        //ex-author
        Matcher exAuthorMatcher = exAuthorPattern.matcher(authorShipString);
        if (exAuthorMatcher.find(0)){
            int authorBegin = exAuthorMatcher.end(0);
            int exAuthorEnd = exAuthorMatcher.start(0);
            String exAuthorString = authorShipString.substring(0, exAuthorEnd).trim();
            authors [1] = parser.author(exAuthorString);
            authorShipString = authorShipString.substring(authorBegin).trim();
        }

        //in-author
        Pattern inAuthorPattern = Pattern.compile("(.*)" + oWs + "in" + oWs + "(.*)");
        Matcher inAuthorMatcher = inAuthorPattern.matcher(authorShipString);
        if (inAuthorMatcher.matches()) {
            String inAuthor = inAuthorMatcher.group(2);
            authors[2] = parser.author(inAuthor);
            authorShipString = inAuthorMatcher.group(1);
        }
        authors[0] = parser.author(authorShipString);
    }

    private Reference makeNomRef(Map<String, String> record) {

        String referenceUniqueStr = getValue(record, REFERENCE_UNIQUE);
        String yearStr = getValue(record, YEAR);
        String authorsStr = getValue(record, AUTHORS);
        String titleStr = getValue(record, TITLE);
        String journalStr = getValue(record, JOURNAL);
        String volPageRangeStr = getValue(record, VOLUME_PAGE_RANGE);
        String bookEditorsStr = getValue(record, BOOK_EDITOR_S);
        String bookTitleStr = getValue(record, BOOK_TITLE);
        String publisherStr = getValue(record, PUBLISHER);
        String doiStr = getValue(record, DOI_UNIQUE);

        String vol = volPageRangeStr == null? null : volPageRangeStr.split(":")[0].trim();
        String pageRange = volPageRangeStr == null? null : volPageRangeStr.split(":")[1].trim();

        Reference nomRef = null;
        if (isNotBlank(referenceUniqueStr) || isNotBlank(titleStr) || isNotBlank(bookTitleStr)) {
            if (isNotBlank(referenceUniqueStr)) {
                nomRef = referenceMap.get(referenceUniqueStr);
            }
            if (nomRef == null) {
                //TODO deduplicate
                if (isNotBlank(bookTitleStr)) {
                    nomRef = ReferenceFactory.newBook();
                    nomRef.setTitle(bookTitleStr);
                    String publisher = publisherStr.split(",")[0].trim();
                    String placePublished = publisherStr.split(",")[1].trim();
                    nomRef.setPublisher(publisher);
                    nomRef.setPlacePublished(placePublished);
                    nomRef.setEditor(bookEditorsStr);
                    if (isNotBlank(titleStr)) {
                        Reference bookSection = ReferenceFactory.newBookSection();
                        bookSection.setTitle(titleStr);
                        bookSection.setInReference(nomRef);
                        nomRef = bookSection;
                    }
                }else {
                    nomRef = ReferenceFactory.newArticle();
                    nomRef.setTitle(titleStr);
                    nomRef.setVolume(vol);
                    //inRef
                    Reference inRef = referenceMap.get(journalStr);
                    if (inRef == null) {
                        inRef = ReferenceFactory.newJournal();
                        inRef.setTitle(journalStr);
                        referenceMap.put(journalStr, inRef);
                    }
                    nomRef.setInReference(inRef);
                }

                nomRef.setDatePublished(TimePeriodParser.parseStringVerbatim(yearStr));
                TeamOrPersonBase<?> author = parseBiblioAuthors(authorsStr);
                nomRef.setAuthorship(author);
                nomRef.setPages(pageRange);
                DOI doi = isBlank(doiStr)? null : DOI.fromString(doiStr);
                nomRef.setDoi(doi);
                referenceMap.put(referenceUniqueStr, nomRef);
            }
        }
        return nomRef;
    }

    private Reference makeSecRef(Map<String, String> record, String line, String taxonNameStr) {

        String secRefStr = getValue(record, SECUNDUM_REFERENCE);
        String secAuthorsStr = getValue(record, SECUNDUM_AUTHORS);
        String secTitleStr = getValue(record, SECUNDUM_TITLE);
        String secJournalStr = getValue(record, SECUNDUM_JOURNAL);
        String secVolPageRangeStr = getValue(record, SECUNDUM_VOLUME_PAGE_RANGE);
        String secDoiStr = getValue(record, SECUNDUM_DOI_UNIQUE);

        Reference secRef = null;
        if (! isBlank(secRefStr) || ! isBlank(secTitleStr)) {
            if (!isBlank(secRefStr)) {
                secRef = referenceMap.get(secRefStr);
            }
            if (secRef == null) {
                //TODO books
                //TODO deduplicate
                secRef = ReferenceFactory.newArticle();
                secRef.setTitle(secTitleStr);
                //TODO secRef year
                String secRefYearStr = secRefStr.substring(secRefStr.length()-4);
                secRef.setDatePublished(TimePeriodParser.parseStringVerbatim(secRefYearStr));
                TeamOrPersonBase<?> author = parseBiblioAuthors(secAuthorsStr);
                secRef.setAuthorship(author);
                String secVol = secVolPageRangeStr == null? null : secVolPageRangeStr.split(":")[0].trim();
                String secPageRange = secVolPageRangeStr == null? null : secVolPageRangeStr.split(":")[1].trim();
                secRef.setVolume(secVol);

                secRef.setPages(secPageRange);
                DOI doi = isBlank(secDoiStr)? null : DOI.fromString(secDoiStr);
                secRef.setDoi(doi);

                Reference secInRef = referenceMap.get(secJournalStr);
                if (secInRef == null) {
                    secInRef = ReferenceFactory.newJournal();
                    secInRef.setTitle(secJournalStr);
                    referenceMap.put(secJournalStr, secInRef);
                }
                secRef.setInReference(secInRef);
                referenceMap.put(secRefStr, secRef);
            }
        }else {
            logger.warn(line + "No secRef for " + taxonNameStr);
        }
        return secRef;
    }

    private TeamOrPersonBase<?> parseBiblioAuthors(String authorsStr) {
        BibliographicAuthorParser parser = BibliographicAuthorParser.Instance();
        TeamOrPersonBase<?> result = parser.parse(authorsStr);
        return result;
    }

    private void addSynonymToAccepted(Map<String, String> record,
            Synonym syn, Map<String, Taxon> taxonMap, String line) {

        String acceptedNameStr = getValue(record, ACCEPTED_NAME_STRING);
        String statusStr = getValue(record, STATUS);

        Taxon acceptedTaxon = taxonMap.get(acceptedNameStr);
        if (acceptedTaxon == null && "nomen dubium".equals(statusStr)) {
            acceptedNameStr = "Coccocarpia palmicola (Spreng.) Arv. & D.J. Galloway";
            acceptedTaxon = taxonMap.get(acceptedNameStr);
        }

        if (acceptedTaxon == null) {
            logger.warn(line + "Accepted taxon not found: " +  acceptedNameStr);
            return;
        }else {
            //
            SynonymType synonymType = statusStr == null || !statusStr.equals("homotypic synonym")? SynonymType.HETEROTYPIC_SYNONYM_OF : SynonymType.HOMOTYPIC_SYNONYM_OF;
            acceptedTaxon.addSynonym(syn, synonymType);
            if ("invalid [ICNafp Art. 35.2]".equals(statusStr)){
                NomenclaturalStatus nomStatus = NomenclaturalStatus.NewInstance(NomenclaturalStatusType.INVALID());
                nomStatus.setRuleConsidered("Art. 35.2");
                syn.getName().addStatus(nomStatus);
            }else if ("illegitimate [ICNafp Art. 52.1]".equals(statusStr)){
                NomenclaturalStatus nomStatus = NomenclaturalStatus.NewInstance(NomenclaturalStatusType.ILLEGITIMATE());
                nomStatus.setRuleConsidered("Art. 52.1");
                syn.getName().addStatus(nomStatus);
            }else if ("nomen dubium".equals(statusStr)){
                NomenclaturalStatus nomStatus = NomenclaturalStatus.NewInstance(NomenclaturalStatusType.DOUBTFUL());
                syn.getName().addStatus(nomStatus);
            }
        }

    }

    private void checkParsed(TaxonName name, String nameStr, String line) {
		if (name.isProtectedTitleCache() || name.isProtectedFullTitleCache() || name.isProtectedNameCache()) {
			logger.warn(line + "Name could not be parsed: " + nameStr);
		}
	}

	private void addToParentNode(SimpleExcelTaxonImportState<CONFIG> state, Taxon taxon,
	        Map<String,Taxon> taxonMap, String line) {

	    Classification classification = getCoraNode(state).getClassification();

		TaxonName name = taxon.getName();
		TaxonNode parentNode;
		String parentStr;
		if (name.isSpecies()) {
		    parentStr = name.getGenusOrUninomial();
		    parentNode = getCoraNode(state);
		}else {
		    parentStr = CdmUtils.concat(" ", name.getGenusOrUninomial(), name.getSpecificEpithet());
		    Taxon parentTaxon = taxonMap.get(parentStr);
		    if (parentTaxon == null) {
		        logger.warn(line + "Parent not found: " + parentStr);
		        return;
		    }
		    parentNode = parentTaxon.getTaxonNode(classification);
		}

        if (parentNode == null) {
            logger.warn(line + "Node not found for parent: " + parentStr);
            return;
        }
        parentNode.addChildTaxon(taxon, null);

		getTaxonNodeService().saveOrUpdate(parentNode);

		return;
	}

    private TaxonNode coraNode;

    private TaxonNode getCoraNode(SimpleExcelTaxonImportState<CONFIG> state) {
        if (coraNode == null) {
            coraNode = getTaxonNodeService().find(coraNodeUuid);
        }
        if (coraNode == null){
            Reference sec = getSecReference(state);
            String classificationName = state.getConfig().getClassificationName();
            Language language = Language.DEFAULT();
            Classification classification = Classification.NewInstance(classificationName, sec, language);
            classification.setUuid(state.getConfig().getClassificationUuid());
            classification.getRootNode().setUuid(coraNodeUuid);
            getClassificationService().save(classification);

            coraNode = classification.getRootNode();
        }
        return coraNode;
    }

    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference == null){
            this.secReference = getPersistentReference(state.getConfig().getSecReference());
        }
        return this.secReference;
    }

    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
        }
        return this.sourceReference;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        if (result == null){
            result = reference;
        }
        return result;
    }

    private void replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, INonViralName name) {
        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
    }

    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
    	String noStr = "Row:" + state.getCurrentLine();
        return IdentifiableSource.NewDataImportInstance(noStr, null, state.getConfig().getSourceReference());
    }
}