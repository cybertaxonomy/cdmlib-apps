/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.phycobank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.LogUtils;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.LanguageString;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NameTypeDesignation;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.occurrence.GatheringEvent;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.ITaxonTreeNode;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 05.01.2016
 */
@Component("iAPTExcelImport")
public class IAPTExcelImport<CONFIG extends IAPTImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -747486709409732371L;
    private static final Logger logger = LogManager.getLogger();

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
            REGISTRATIONNO_PK, HIGHERTAXON, FULLNAME, AUTHORSSPELLING,
            LITSTRING, REGISTRATION, TYPE, CAVEATS, FULLBASIONYM,
            FULLSYNSUBST, NOTESTXT, REGDATE, NAMESTRING, BASIONYMSTRING,
            SYNSUBSTSTR, AUTHORSTRING});

    private static final Pattern nomRefTokenizeP = Pattern.compile("^(?<title>.*):\\s(?<detail>[^\\.:]+)\\.(?<date>.*?)(?:\\s\\((?<issue>[^\\)]*)\\)\\s*)?\\.?$");
    private static final Pattern[] datePatterns = new Pattern[]{
            // NOTE:
            // The order of the patterns is extremely important!!!
            //
            // all patterns cover the years 1700 - 1999
            Pattern.compile("^(?<year>1[7,8,9][0-9]{2})$"), // only year, like '1969'
            Pattern.compile("^(?<monthName>\\p{L}+\\.?)\\s(?<day>[0-9]{1,2})(?:st|rd|th)?\\.?,?\\s(?<year>(?:1[7,8,9])?[0-9]{2})$"), // full date like April 12, 1969 or april 12th 1999
            Pattern.compile("^(?<monthName>\\p{L}+\\.?),?\\s?(?<year>(?:1[7,8,9])?[0-9]{2})$"), // April 99 or April, 1999 or Apr. 12
            Pattern.compile("^(?<day>[0-9]{1,2})([\\.\\-/])(\\s?)(?<month>[0-1]?[0-9])\\2\\3(?<year>(?:1[7,8,9])?[0-9]{2})$"), // full date like 12.04.1969 or 12. 04. 1969 or 12/04/1969 or 12-04-1969
            Pattern.compile("^(?<day>[0-9]{1,2})([\\.\\-/])(?<monthName>[IVX]{1,2})\\2(?<year>(?:1[7,8,9])?[0-9]{2})$"), // full date like 12-VI-1969
            Pattern.compile("^(?:(?<day>[0-9]{1,2})(?:\\sde)?\\s)?(?<monthName>\\p{L}+)(?:\\sde)?\\s(?<year>(?:1[7,8,9])?[0-9]{2})$"), // full and partial date like 12 de Enero de 1999 or Enero de 1999
            Pattern.compile("^(?<month>[0-1]?[0-9])([\\.\\-/])(?<year>(?:1[7,8,9])?[0-9]{2})$"), // partial date like 04.1969 or 04/1969 or 04-1969
            Pattern.compile("^(?<year>(?:1[7,8,9])?[0-9]{2})([\\.\\-/])(?<month>[0-1]?[0-9])$"),//  partial date like 1999-04
            Pattern.compile("^(?<monthName>[IVX]{1,2})([\\.\\-/])(?<year>(?:1[7,8,9])?[0-9]{2})$"), // partial date like VI-1969
            Pattern.compile("^(?<day>[0-9]{1,2})(?:[\\./]|th|rd|st)?\\s(?<monthName>\\p{L}+\\.?),?\\s?(?<year>(?:1[7,8,9])?[0-9]{2})$"), // full date like 12. April 1969 or april 1999 or 22 Dec.1999
        };
    protected static final Pattern typeSpecimenSplitPattern =  Pattern.compile("^(?:\"*[Tt]ype: (?<fieldUnit>.*?))(?:[Hh]olotype:(?<holotype>.*?)\\.?)?(?:[Ii]sotype[^:]*:(?<isotype>.*)\\.?)?\\.?$");

    private static final Pattern typeNameBasionymPattern =  Pattern.compile("\\([Bb]asionym\\s?\\:\\s?(?<basionymName>[^\\)]*).*$");
    private static final Pattern typeNameNotePattern =  Pattern.compile("\\[([^\\[]*)"); // matches the inner of '[...]'
    private static final Pattern typeNameSpecialSplitPattern =  Pattern.compile("(?<note>.*\\;.*?)\\:(?<agent>)\\;(<name>.*)");

    protected static final Pattern collectorPattern =  Pattern.compile(".*?(?<fullStr1>\\([Ll]eg\\.\\s+(?<data1>[^\\)]*)\\)).*$|.*?(?<fullStr2>\\s[Ll]eg\\.\\:?\\s+(?<data2>.*?)\\.?)$|^(?<fullStr3>[Ll]eg\\.\\:?\\s+(?<data3>.*?)\\.?)");
    private static final Pattern collectionDataPattern =  Pattern.compile("^(?<collector>[^,]*),\\s?(?<detail>.*?)\\.?$");
    private static final Pattern collectorsNumber =  Pattern.compile("^([nN]o\\.\\s.*)$");

    // AccessionNumbers: , #.*, n°:?, 96/3293, No..*, -?\w{1,3}-[0-9\-/]*
    private static final Pattern accessionNumberOnlyPattern = Pattern.compile("^(?<accNumber>(?:n°\\:?\\s?|#|No\\.?\\s?)?[\\d\\w\\-/]*)$");

    private static final Pattern[] specimenTypePatterns = new Pattern[]{
            Pattern.compile("^(?<colCode>[A-Z]+|CPC Micropaleontology Lab\\.?)\\s+(?:\\((?<institute>.*[^\\)])\\))(?<accNumber>.*)?$"), // like: GAUF (Gansu Agricultural University) No. 1207-1222
            Pattern.compile("^(?<colCode>[A-Z]+|CPC Micropaleontology Lab\\.?)\\s+(?:Coll\\.\\s(?<subCollection>[^\\.,;]*)(.))(?<accNumber>.*)?$"), // like KASSEL Coll. Krasske, Praep. DII 78
            Pattern.compile("^(?:in\\s)?(?<institute>[Cc]oll\\.\\s.*?)(?:\\s+(?<accNumber>(Praep\\.|slide|No\\.|Inv\\. Nr\\.|Nr\\.).*))?$"), // like Coll. Lange-Bertalot, Bot. Inst., Univ. Frankfurt/Main, Germany Praep. Neukaledonien OTL 62
            Pattern.compile("^(?<institute>Inst\\.\\s.*?)\\s+(?<accNumber>N\\s.*)?$"), // like Inst. Geological Sciences, Acad. Sci. Belarus, Minsk N 212 A
            Pattern.compile("^(?<colCode>[A-Z]+)(?:\\s+(?<accNumber>.*))?$"), // identifies the Collection code and takes the rest as accessionNumber if any
    };


    private static final Pattern registrationPattern = Pattern.compile("^Registration date\\:\\s(?<regdate>\\d\\d\\.\\d\\d\\.\\d\\d); no\\.\\:\\s(?<regid>\\d+);\\soffice\\:\\s(?<office>.*?)\\.(?:\\s\\[Form no\\.\\:\\s(?<formNo>d+)\\])?$"); // Registration date: 29.06.98; no.: 2922; office: Berlin.

    private static Map<String, Integer> monthFromNameMap = new HashMap<>();

    static {
        String[] ck = new String[]{"leden", "únor", "březen", "duben", "květen", "červen", "červenec ", "srpen", "září", "říjen", "listopad", "prosinec"};
        String[] fr = new String[]{"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};
        String[] de = new String[]{"januar", "februar", "märz", "april", "mai", "juni", "juli", "august", "september", "oktober", "november", "dezember"};
        String[] en = new String[]{"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};
        String[] it = new String[]{"gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"};
        String[] sp = new String[]{"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        String[] de_abbrev = new String[]{"jan.", "feb.", "märz", "apr.", "mai", "jun.", "jul.", "aug.", "sept.", "okt.", "nov.", "dez."};
        String[] en_abbrev = new String[]{"jan.", "feb.", "mar.", "apr.", "may", "jun.", "jul.", "aug.", "sep.", "oct.", "nov.", "dec."};
        String[] port = new String[]{"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        String[] rom_num = new String[]{"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii"};

        String[][] perLang =  new String[][]{ck, de, fr, en, it, sp, port, de_abbrev, en_abbrev, rom_num};

        for (String[] months: perLang) {
            for(int m = 1; m < 13; m++){
                monthFromNameMap.put(months[m - 1].toLowerCase(), m);
            }
        }

        // special cases
        monthFromNameMap.put("mar", 3);
        monthFromNameMap.put("dec", 12);
        monthFromNameMap.put("februari", 2);
        monthFromNameMap.put("març", 3);
    }


    DateTimeFormatter formatterYear = DateTimeFormat.forPattern("yyyy");

    private Map<String, Collection> collectionMap = new HashMap<>();

    private ExtensionType extensionTypeIAPTRegData = null;

    private Set<String> nameSet = new HashSet<>();
    private DefinedTermBase<?> duplicateRegistration = null;

    enum TypesName {
        fieldUnit, holotype, isotype;

        public SpecimenTypeDesignationStatus status(){
            switch (this) {
                case holotype:
                    return SpecimenTypeDesignationStatus.HOLOTYPE();
                case isotype:
                    return SpecimenTypeDesignationStatus.ISOTYPE();
                default:
                    return null;
            }
        }
    }

    private MarkerType markerTypeFossil = null;
    private Rank rankUnrankedSupraGeneric = null;
    private Rank familyIncertisSedis = null;
    private AnnotationType annotationTypeCaveats = null;

    private Reference bookVariedadesTradicionales = null;

    /**
     * HACK for unit simple testing
     */
    boolean _testMode = System.getProperty("TEST_MODE") != null;

    private Taxon makeTaxon(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state,
                            TaxonNode higherTaxonNode, boolean isFossil) {

        String regNumber = getValue(record, REGISTRATIONNO_PK, false);
        String regStr = getValue(record, REGISTRATION, true);
        String titleCacheStr = getValue(record, FULLNAME, true);
        String nameStr = getValue(record, NAMESTRING, true);
        String authorStr = getValue(record, AUTHORSTRING, true);
        String nomRefStr = getValue(record, LITSTRING, true);
        String authorsSpelling = getValue(record, AUTHORSSPELLING, true);
        String notesTxt = getValue(record, NOTESTXT, true);
        String caveats = getValue(record, CAVEATS, true);
        String fullSynSubstStr = getValue(record, FULLSYNSUBST, true);
        String fullBasionymStr = getValue(record, FULLBASIONYM, true);
        String basionymNameStr = getValue(record, FULLBASIONYM, true);
        String synSubstStr = getValue(record, SYNSUBSTSTR, true);
        String typeStr = getValue(record, TYPE, true);


        String nomRefTitle = null;
        String nomRefDetail;
        String nomRefPupDate = null;
        String nomRefIssue = null;
        Partial pupDate = null;

        boolean restoreOriginalReference = false;
        boolean nameIsValid = true;

        // preprocess nomRef: separate citation, reference detail, publishing date
        if(!StringUtils.isEmpty(nomRefStr)){
            nomRefStr = nomRefStr.trim();

            // handle the special case which is hard to parse:
            //
            // Las variedades tradicionales de frutales de la Cuenca del Río Segura. Catálogo Etnobotánico (1): Frutos secos, oleaginosos, frutales de hueso, almendros y frutales de pepita: 154. 1997.
            if(nomRefStr.startsWith("Las variedades tradicionales de frutales ")){

                if(bookVariedadesTradicionales == null){
                    bookVariedadesTradicionales = ReferenceFactory.newBook();
                    bookVariedadesTradicionales.setTitle("Las variedades tradicionales de frutales de la Cuenca del Río Segura. Catálogo Etnobotánico (1): Frutos secos, oleaginosos, frutales de hueso, almendros y frutales de pepita");
                    bookVariedadesTradicionales.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(1997));
                    getReferenceService().save(bookVariedadesTradicionales);
                }
                nomRefStr = nomRefStr.replaceAll("^.*?\\:.*?\\:", "Las variedades tradicionales:");
                restoreOriginalReference = true;
            }

            Matcher m = nomRefTokenizeP.matcher(nomRefStr);
            if(m.matches()){
                nomRefTitle = m.group("title");
                nomRefDetail = m.group("detail");
                nomRefPupDate = m.group("date").trim();
                nomRefIssue = m.group("issue");

                pupDate = parseDate(regNumber, nomRefPupDate);
                if (pupDate != null) {
                    nomRefTitle = nomRefTitle + ": " + nomRefDetail + ". " + pupDate.toString(formatterYear) + ".";
                } else {
                    logger.warn(csvReportLine(regNumber, "Pub date", nomRefPupDate, "in", nomRefStr, "not parsable"));
                }
            } else {
                nomRefTitle = nomRefStr;
            }
        }

        TaxonName taxonName = makeBotanicalName(state, regNumber, titleCacheStr, nameStr, authorStr, nomRefTitle);

        // always add the original strings of parsed data as annotation
        taxonName.addAnnotation(Annotation.NewInstance("imported and parsed data strings:" +
                        "\n -  '" + LITSTRING + "': "+ nomRefStr +
                        "\n -  '" + TYPE + "': " + typeStr +
                        "\n -  '" + REGISTRATION  + "': " + regStr
                , AnnotationType.INTERNAL(), Language.DEFAULT()));

        if(restoreOriginalReference){
            taxonName.setNomenclaturalReference(bookVariedadesTradicionales);
        }

        if(taxonName.getNomenclaturalReference() != null){
            if(pupDate != null) {
                taxonName.getNomenclaturalReference().setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(pupDate));
            }
            if(nomRefIssue != null) {
                taxonName.getNomenclaturalReference().setVolume(nomRefIssue);
            }
        }


        if(!StringUtils.isEmpty(notesTxt)){
            notesTxt = notesTxt.replace("Notes: ", "").trim();
            taxonName.addAnnotation(Annotation.NewInstance(notesTxt, AnnotationType.EDITORIAL(), Language.DEFAULT()));
            nameIsValid = false;

        }
        if(!StringUtils.isEmpty(caveats)){
            caveats = caveats.replace("Caveats: ", "").trim();
            taxonName.addAnnotation(Annotation.NewInstance(caveats, annotationTypeCaveats(), Language.DEFAULT()));
            nameIsValid = false;
        }

        if(nameIsValid){
            // Status is always considered valid if no notes and cavets are set
            taxonName.addStatus(NomenclaturalStatus.NewInstance(NomenclaturalStatusType.VALID()));
        }

        getNameService().save(taxonName);

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
            misspelledNameStr = taxonName.getTitleCache().replace(nameStr, misspelledNameStr);

            TaxonName misspelledName = nameParser.parseReferencedName(misspelledNameStr, NomenclaturalCode.ICNAFP, null);
            misspelledName.addRelationshipToName(taxonName, NameRelationshipType.MISSPELLING(), null, null);
            getNameService().save(misspelledName);
        }

        // Replaced Synonyms
        if(!StringUtils.isEmpty(fullSynSubstStr)){
            fullSynSubstStr = fullSynSubstStr.replace("Syn. subst.: ", "");
            TaxonName replacedSynonymName = makeBotanicalName(state, regNumber, fullSynSubstStr, synSubstStr, null, null);
            replacedSynonymName.addReplacedSynonym(taxonName, null, null, null, null);
            getNameService().save(replacedSynonymName);
        }

        Reference sec = state.getConfig().getSecReference();
        Taxon taxon = Taxon.NewInstance(taxonName, sec);

        // Basionym
        if(fullBasionymStr != null){
            fullBasionymStr = fullBasionymStr.replaceAll("^\\w*:\\s", ""); // Strip off the leading 'Basionym: "
            basionymNameStr = basionymNameStr.replaceAll("^\\w*:\\s", ""); // Strip off the leading 'Basionym: "
            TaxonName basionym = makeBotanicalName(state, regNumber, fullBasionymStr, basionymNameStr, null, null);
            getNameService().save(basionym);
            taxonName.addBasionym(basionym);

            Synonym syn = Synonym.NewInstance(basionym, sec);
            taxon.addSynonym(syn, SynonymType.HOMOTYPIC_SYNONYM_OF);
            getTaxonService().save(syn);
        }

        // Markers
        if(isFossil){
            taxon.addMarker(Marker.NewInstance(markerTypeFossil(), true));
        }
        if(!nameSet.add(titleCacheStr)){
            taxonName.addMarker(Marker.NewInstance(markerDuplicateRegistration(), true));
            logger.warn(csvReportLine(regNumber, "Duplicate registration of", titleCacheStr));
        }


        // Types
        if(!StringUtils.isEmpty(typeStr)){

            if(taxonName.getRank().isSpecies() || taxonName.getRank().isLowerThan(RankClass.Species)) {
                makeSpecimenTypeData(typeStr, taxonName, regNumber, state, false);
            } else {
                makeNameTypeData(typeStr, taxonName, regNumber, state);
            }
        }

        getTaxonService().save(taxon);

        if(taxonName.getRank().equals(Rank.SPECIES()) || taxonName.getRank().isLowerThan(RankClass.Species)){
            // try to find the genus, it should have been imported already, Genera are coming first in the import file
            Taxon genus = ((IAPTImportState)state).getGenusTaxonMap().get(taxonName.getGenusOrUninomial());
            if(genus != null){
                higherTaxonNode = genus.getTaxonNodes().iterator().next();
            } else {
                logger.info(csvReportLine(regNumber, "Parent genus not found for", nameStr));
            }
        }

        if(higherTaxonNode != null){
            higherTaxonNode.addChildTaxon(taxon, null, null);
            getTaxonNodeService().save(higherTaxonNode);
        }

        if(taxonName.getRank().isGenus()){
            ((IAPTImportState)state).getGenusTaxonMap().put(taxonName.getGenusOrUninomial(), taxon);
        }

        return taxon;
    }

    private void makeSpecimenTypeData(String typeStr, TaxonName taxonName, String regNumber, SimpleExcelTaxonImportState<CONFIG> state, boolean isFossil) {

        Matcher m = typeSpecimenSplitPattern.matcher(typeStr);

        if(m.matches()){
            String fieldUnitStr = m.group(TypesName.fieldUnit.name());
            // boolean isFieldUnit = typeStr.matches(".*([°']|\\d+\\s?m\\s|\\d+\\s?km\\s).*"); // check for location or unit m, km // makes no sense!!!!
            FieldUnit fieldUnit = parseFieldUnit(fieldUnitStr, regNumber, state);
            if(fieldUnit == null) {
                // create a field unit with only a titleCache using the fieldUnitStr substring
                logger.warn(csvReportLine(regNumber, "Type: fieldUnitStr can not be parsed", fieldUnitStr));
                fieldUnit = FieldUnit.NewInstance();
                fieldUnit.setTitleCache(fieldUnitStr, true);
                getOccurrenceService().save(fieldUnit);
            }
            getOccurrenceService().save(fieldUnit);

            SpecimenOrObservationType specimenType;
            if(isFossil){
                specimenType = SpecimenOrObservationType.Fossil;
            } else {
                specimenType = SpecimenOrObservationType.PreservedSpecimen;
            }

            // all others ..
            addSpecimenTypes(taxonName, fieldUnit, m.group(TypesName.holotype.name()), TypesName.holotype, false, regNumber, specimenType);
            addSpecimenTypes(taxonName, fieldUnit, m.group(TypesName.isotype.name()), TypesName.isotype, true, regNumber, specimenType);

        } else {
            // create a field unit with only a titleCache using the full typeStr
            FieldUnit fieldUnit = FieldUnit.NewInstance();
            fieldUnit.setTitleCache(typeStr, true);
            getOccurrenceService().save(fieldUnit);
            logger.warn(csvReportLine(regNumber, "Type: field 'Type' can not be parsed", typeStr));
        }
        getNameService().save(taxonName);
    }

    private void makeNameTypeData(String typeStr, IBotanicalName taxonName,
            String regNumber, @SuppressWarnings("unused") SimpleExcelTaxonImportState<CONFIG> state) {

        String nameStr = typeStr.replaceAll("^Type\\s?\\:\\s?", "");
        if(nameStr.isEmpty()) {
            return;
        }

        String basionymNameStr = null;
        String noteStr = null;
        String agentStr = null;

        Matcher m;

        if(typeStr.startsWith("not to be indicated")){
            // Special case:
            // Type: not to be indicated (Art. H.9.1. Tokyo Code); stated parent genera: Hechtia Klotzsch; Deuterocohnia Mez
            // FIXME
            m = typeNameSpecialSplitPattern.matcher(nameStr);
            if(m.matches()){
                nameStr = m.group("name");
                noteStr = m.group("note");
                agentStr = m.group("agent");
                // TODO better import of agent?
                if(agentStr != null){
                    noteStr = noteStr + ": " + agentStr;
                }
            }
        } else {
            // Generic case
            m = typeNameBasionymPattern.matcher(nameStr);
            if (m.find()) {
                basionymNameStr = m.group("basionymName");
                if (basionymNameStr != null) {
                    nameStr = nameStr.replace(m.group(0), "");
                }
            }

            m = typeNameNotePattern.matcher(nameStr);
            if (m.find()) {
                noteStr = m.group(1);
                if (noteStr != null) {
                    nameStr = nameStr.replace(m.group(0), "");
                }
            }
        }

        TaxonName typeName = (TaxonName) nameParser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, null);

        if(typeName.isProtectedTitleCache() || typeName.getNomenclaturalReference() != null && typeName.getNomenclaturalReference().isProtectedTitleCache()) {
            logger.warn(csvReportLine(regNumber, "NameType not parsable", typeStr, nameStr));
        }

        if(basionymNameStr != null){
            TaxonName basionymName = (TaxonName) nameParser.parseFullName(nameStr, NomenclaturalCode.ICNAFP, null);
            getNameService().save(basionymName);
            typeName.addBasionym(basionymName);
        }


        NameTypeDesignation nameTypeDesignation = NameTypeDesignation.NewInstance();
        nameTypeDesignation.setTypeName(typeName);
        getNameService().save(typeName);

        if(noteStr != null){
            nameTypeDesignation.addAnnotation(Annotation.NewInstance(noteStr, AnnotationType.EDITORIAL(), Language.UNKNOWN_LANGUAGE()));
        }
        taxonName.addNameTypeDesignation(typeName, null, null, null, null, false);

    }

    /**
     * Currently only parses the collector, fieldNumber and the collection date.
     *
     * @param fieldUnitStr
     * @param regNumber
     * @param state
     * @return null if the fieldUnitStr could not be parsed
     */
    private FieldUnit parseFieldUnit(String fieldUnitStr, String regNumber, SimpleExcelTaxonImportState<CONFIG> state) {

        FieldUnit fieldUnit = null;

        Matcher m1 = collectorPattern.matcher(fieldUnitStr);
        if(m1.matches()){

            String collectorData = m1.group(2); // like ... (leg. Metzeltin, 30. 9. 1996)
            String removal = m1.group(1);
            if(collectorData == null){
                collectorData = m1.group(4); // like ... leg. Metzeltin, 30. 9. 1996
                removal = m1.group(3);
            }
            if(collectorData == null){
                collectorData = m1.group(6); // like ^leg. J. J. Halda 18.3.1997$
                removal = null;
            }
            if(collectorData == null){
                return null;
            }

            // the fieldUnitStr is parsable
            // remove all collectorData from the fieldUnitStr and use the rest as locality
            String locality = null;
            if(removal != null){
                locality = fieldUnitStr.replace(removal, "");
            }

            String collectorStr = null;
            String detailStr = null;
            Partial date = null;
            String fieldNumber = null;

            Matcher m2 = collectionDataPattern.matcher(collectorData);
            if(m2.matches()){
                collectorStr = m2.group("collector");
                detailStr = m2.group("detail");

                // Try to make sense of the detailStr
                if(detailStr != null){
                    detailStr = detailStr.trim();
                    // 1. try to parse as date
                    date = parseDate(regNumber, detailStr);
                    if(date == null){
                        // 2. try to parse as number
                        if(collectorsNumber.matcher(detailStr).matches()){
                            fieldNumber = detailStr;
                        }
                    }
                }
                if(date == null && fieldNumber == null){
                    // detailed parsing not possible, so need fo fallback
                    collectorStr = collectorData;
                }
            }

            if(collectorStr == null) {
                collectorStr = collectorData;
            }

            fieldUnit = FieldUnit.NewInstance();
            GatheringEvent ge = GatheringEvent.NewInstance();
            if(locality != null){
                ge.setLocality(LanguageString.NewInstance(locality, Language.UNKNOWN_LANGUAGE()));
            }

            TeamOrPersonBase<?> agent =  state.getAgentBase(collectorStr);
            if(agent == null) {
                agent = Person.NewTitledInstance(collectorStr);
                getAgentService().save(agent);
                state.putAgentBase(collectorStr, agent);
            }
            ge.setCollector(agent);

            if(date != null){
                ge.setGatheringDate(date);
            }

            getCommonService().save(ge);
            fieldUnit.setGatheringEvent(ge);

            if(fieldNumber != null) {
                fieldUnit.setFieldNumber(fieldNumber);
            }
            getOccurrenceService().save(fieldUnit);

        }

        return fieldUnit;
    }

    protected Partial parseDate(String regNumber, String dateStr) {

        Partial pupDate = null;
        boolean parseError = false;

        String day = null;
        String month = null;
        String monthName = null;
        String year = null;

        for(Pattern p : datePatterns){
            Matcher m2 = p.matcher(dateStr);
            if(m2.matches()){
                try {
                    year = m2.group("year");
                } catch (IllegalArgumentException e){
                    // named capture group not found
                }
                try {
                    month = m2.group("month");
                } catch (IllegalArgumentException e){
                    // named capture group not found
                }

                try {
                    monthName = m2.group("monthName");
                    month = monthFromName(monthName, regNumber);
                    if(month == null){
                        parseError = true;
                    }
                } catch (IllegalArgumentException e){
                    // named capture group not found
                }
                try {
                    day = m2.group("day");
                } catch (IllegalArgumentException e){
                    // named capture group not found
                }

                if(year != null){
                    if (year.length() == 2) {
                        // it is an abbreviated year from the 19** years
                        year = "19" + year;
                    }
                    break;
                } else {
                    parseError = true;
                }
            }
        }
        if(year == null){
            parseError = true;
        }
        List<DateTimeFieldType> types = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        if(!parseError) {
            types.add(DateTimeFieldType.year());
            values.add(Integer.parseInt(year));
            if (month != null) {
                types.add(DateTimeFieldType.monthOfYear());
                values.add(Integer.parseInt(month));
            }
            if (day != null) {
                types.add(DateTimeFieldType.dayOfMonth());
                values.add(Integer.parseInt(day));
            }
            pupDate = new Partial(types.toArray(new DateTimeFieldType[types.size()]), ArrayUtils.toPrimitive(values.toArray(new Integer[values.size()])));
        }
        return pupDate;
    }

    private String monthFromName(String monthName, String regNumber) {

        Integer month = monthFromNameMap.get(monthName.toLowerCase());
        if(month == null){
            logger.warn(csvReportLine(regNumber, "Unknown month name", monthName));
            return null;
        } else {
            return month.toString();
        }
    }


    private void addSpecimenTypes(IBotanicalName taxonName, FieldUnit fieldUnit, String typeStr, TypesName typeName, boolean multiple, String regNumber, SpecimenOrObservationType specimenType){

        if(StringUtils.isEmpty(typeStr)){
            return;
        }
        typeStr = typeStr.trim().replaceAll("\\.$", "");

        Collection collection = null;
        DerivedUnit specimen = null;

        List<DerivedUnit> specimens = new ArrayList<>();
        if(multiple){
            String[] tokens = typeStr.split("\\s?,\\s?");
            for (String t : tokens) {
                // command to  list all complex parsabel types:
                // csvcut -t -c RegistrationNo_Pk,Type iapt.csv | csvgrep -c Type -m "Holotype" | egrep -o 'Holotype:\s([A-Z]*\s)[^.]*?'
                // csvcut -t -c RegistrationNo_Pk,Type iapt.csv | csvgrep -c Type -m "Holotype" | egrep -o 'Isotype[^:]*:\s([A-Z]*\s)[^.]*?'

                if(!t.isEmpty()){
                    // trying to parse the string
                    specimen = parseSpecimenType(fieldUnit, typeName, collection, t, regNumber);
                    if(specimen != null){
                        specimens.add(specimen);
                    } else {
                        // parsing was not successful make simple specimen
                        specimens.add(makeSpecimenType(fieldUnit, t, specimenType));
                    }
                }
            }
        } else {
            specimen = parseSpecimenType(fieldUnit, typeName, collection, typeStr, regNumber);
            if(specimen != null) {
                specimens.add(specimen);
                // remember current collection
                collection = specimen.getCollection();
            } else {
                // parsing was not successful make simple specimen
                specimens.add(makeSpecimenType(fieldUnit, typeStr, SpecimenOrObservationType.PreservedSpecimen));
            }
        }

        for(DerivedUnit s : specimens){
            taxonName.addSpecimenTypeDesignation(s, typeName.status(), null, null, null, false, true);
       }
    }

    private DerivedUnit makeSpecimenType(FieldUnit fieldUnit, String titleCache, SpecimenOrObservationType specimenType) {
        DerivedUnit specimen;DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(specimenType, fieldUnit);
        facade.setTitleCache(titleCache.trim(), true);
        specimen = facade.innerDerivedUnit();
        return specimen;
    }

    /**
     *
     * @param fieldUnit
     * @param typeName
     * @param collection
     * @param text
     * @param regNumber
     * @return
     */
    protected DerivedUnit parseSpecimenType(FieldUnit fieldUnit, TypesName typeName, Collection collection, String text, String regNumber) {

        DerivedUnit specimen = null;

        String collectionCode = null;
        String subCollectionStr = null;
        String instituteStr = null;
        String accessionNumber = null;

        boolean unusualAccessionNumber = false;

        text = text.trim();

        // 1.  For Isotypes often the accession number is noted alone if the
        //     preceeding entry has a collection code.
        if(typeName .equals(TypesName.isotype) && collection != null){
            Matcher m = accessionNumberOnlyPattern.matcher(text);
            if(m.matches()){
                try {
                    accessionNumber = m.group("accNumber");
                    specimen = makeSpecimenType(fieldUnit, collection, accessionNumber);
                } catch (IllegalArgumentException e){
                    // match group acc_number not found
                }
            }
        }

        //2. try it the 'normal' way
        if(specimen == null) {
            for (Pattern p : specimenTypePatterns) {
                Matcher m = p.matcher(text);
                if (m.matches()) {
                    // collection code or collectionTitle is mandatory
                    try {
                        collectionCode = m.group("colCode");
                    } catch (IllegalArgumentException e){
                        // match group colCode not found
                    }

                    try {
                        instituteStr = m.group("institute");
                    } catch (IllegalArgumentException e){
                        // match group col_name not found
                    }

                    try {
                        subCollectionStr = m.group("subCollection");
                    } catch (IllegalArgumentException e){
                        // match group subCollection not found
                    }
                    try {
                        accessionNumber = m.group("accNumber");

                        // try to improve the accessionNumber
                        if(accessionNumber!= null) {
                            accessionNumber = accessionNumber.trim();
                            Matcher m2 = accessionNumberOnlyPattern.matcher(accessionNumber);
                            String betterAccessionNumber = null;
                            if (m2.matches()) {
                                try {
                                    betterAccessionNumber = m.group("accNumber");
                                } catch (IllegalArgumentException e) {
                                    // match group acc_number not found
                                }
                            }
                            if (betterAccessionNumber != null) {
                                accessionNumber = betterAccessionNumber;
                            } else {
                                unusualAccessionNumber = true;
                            }
                        }

                    } catch (IllegalArgumentException e){
                        // match group acc_number not found
                    }

                    if(collectionCode == null && instituteStr == null){
                        logger.warn(csvReportLine(regNumber, "Type: neither 'collectionCode' nor 'institute' found in ", text));
                        continue;
                    }
                    collection = getCollection(collectionCode, instituteStr, subCollectionStr);
                    specimen = makeSpecimenType(fieldUnit, collection, accessionNumber);
                    break;
                }
            }
        }
        if(specimen == null) {
            logger.warn(csvReportLine(regNumber, "Type: Could not parse specimen", typeName.name().toString(), text));
        }
        if(unusualAccessionNumber){
            logger.warn(csvReportLine(regNumber, "Type: Unusual accession number", typeName.name().toString(), text, accessionNumber));
        }
        return specimen;
    }

    private DerivedUnit makeSpecimenType(FieldUnit fieldUnit, Collection collection, String accessionNumber) {

        DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(SpecimenOrObservationType.PreservedSpecimen, fieldUnit);
        facade.setCollection(collection);
        if(accessionNumber != null){
            facade.setAccessionNumber(accessionNumber);
        }
        return facade.innerDerivedUnit();
    }

    private TaxonName makeBotanicalName(SimpleExcelTaxonImportState<CONFIG> state, String regNumber, String titleCacheStr, String nameStr,
                                            String authorStr, String nomRefTitle) {

        TaxonName taxonName;// cache field for the taxonName.titleCache
        String taxonNameTitleCache = null;
        Map<String, AnnotationType> nameAnnotations = new HashMap<>();

        // TitleCache preprocessing
        if(titleCacheStr.endsWith(ANNOTATION_MARKER_STRING) || (authorStr != null && authorStr.endsWith(ANNOTATION_MARKER_STRING))){
            nameAnnotations.put("Author abbreviation not checked.", AnnotationType.EDITORIAL());
            titleCacheStr = titleCacheStr.replace(ANNOTATION_MARKER_STRING, "").trim();
            if(authorStr != null) {
                authorStr = authorStr.replace(ANNOTATION_MARKER_STRING, "").trim();
            }
        }

        // parse the full taxon name
        if(!StringUtils.isEmpty(nomRefTitle)){
            String referenceSeparator = nomRefTitle.startsWith("in ") ? " " : ", ";
            String taxonFullNameStr = titleCacheStr + referenceSeparator + nomRefTitle;
            logger.debug(":::::" + taxonFullNameStr);
            taxonName = nameParser.parseReferencedName(taxonFullNameStr, NomenclaturalCode.ICNAFP, null);
        } else {
            taxonName = (TaxonName) nameParser.parseFullName(titleCacheStr, NomenclaturalCode.ICNAFP, null);
        }

        taxonNameTitleCache = taxonName.getTitleCache().trim();
        if (taxonName.isProtectedTitleCache()) {
            logger.warn(csvReportLine(regNumber, "Name could not be parsed", titleCacheStr));
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
            if(taxonName.isMonomHybrid()){
                titleCacheCompareStr = titleCacheCompareStr.replaceAll("^X ", "× ");
                nameCompareStr = nameCompareStr.replace("^X ", "× ");
            }
            if(authorStr != null && authorStr.contains(" et ")){
                titleCacheCompareStr = titleCacheCompareStr.replaceAll(" et ", " & ");
            }
            if (!taxonNameTitleCache.equals(titleCacheCompareStr)) {
                logger.warn(csvReportLine(regNumber, "The generated titleCache differs from the imported string", taxonNameTitleCache, " != ", titleCacheStr, " ==> original titleCacheStr has been restored"));
                doRestoreTitleCacheStr = true;
            }
            if (!nameCache.trim().equals(nameCompareStr)) {
                logger.warn(csvReportLine(regNumber, "The parsed nameCache differs from field '" + NAMESTRING + "'", nameCache, " != ", nameCompareStr));
            }

            //  Author
            //nameParser.handleAuthors(taxonName, titleCacheStr, authorStr);
            //if (!titleCacheStr.equals(taxonName.getTitleCache())) {
            //    logger.warn(regNumber + ": titleCache has changed after setting authors, will restore original titleCacheStr");
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
        }

        taxonName.addSource(OriginalSourceType.Import, regNumber, null, state.getConfig().getSourceReference(), null);

        getNameService().save(taxonName);

        return taxonName;
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

    private Collection getCollection(String collectionCode, String instituteStr, String subCollectionStr){

        Collection superCollection = null;
        if(subCollectionStr != null){
            superCollection = getCollection(collectionCode, instituteStr, null);
            collectionCode = subCollectionStr;
            instituteStr = null;
        }

        final String key = collectionCode + "-#i:" + StringUtils.defaultString(instituteStr);

        Collection collection = collectionMap.get(key);

        if(collection == null) {
            collection = Collection.NewInstance();
            collection.setCode(collectionCode);
            if(instituteStr != null){
                collection.setInstitute(Institution.NewNamedInstance(instituteStr));
            }
            if(superCollection != null){
                collection.setSuperCollection(superCollection);
            }
            collectionMap.put(key, collection);
            if(!_testMode) {
                getCollectionService().save(collection);
            }
        }

        return collection;
    }


    /**
     * @param record
     * @param originalKey
     * @param doUnescapeHtmlEntities
     * @return
     */
    private String getValue(Map<String, String> record, String originalKey, boolean doUnescapeHtmlEntities) {
        String value = record.get(originalKey);

        value = fixCharacters(value);

        if (! StringUtils.isBlank(value)) {
        	if (logger.isDebugEnabled()) {
        	    logger.debug(originalKey + ": " + value);
        	}
        	value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
            if(doUnescapeHtmlEntities){
                value = StringEscapeUtils.unescapeHtml4(value);
            }
        	return value.trim();
        }else{
        	return null;
        }
    }

    /**
     * Fixes broken characters.
     * For details see
     * https://dev.e-taxonomy.eu/redmine/issues/6035
     *
     * @param value
     * @return
     */
    private String fixCharacters(String value) {

        value = StringUtils.replace(value, "s$K", "š");
        value = StringUtils.replace(value, "n$K", "ň");
        value = StringUtils.replace(value, "e$K", "ě");
        value = StringUtils.replace(value, "r$K", "ř");
        value = StringUtils.replace(value, "c$K", "č");
        value = StringUtils.replace(value, "z$K", "ž");
        value = StringUtils.replace(value, "S>U$K", "Š");
        value = StringUtils.replace(value, "C>U$K", "Č");
        value = StringUtils.replace(value, "R>U$K", "Ř");
        value = StringUtils.replace(value, "Z>U$K", "Ž");
        value = StringUtils.replace(value, "g$K", "ǧ");
        value = StringUtils.replace(value, "s$A", "ś");
        value = StringUtils.replace(value, "n$A", "ń");
        value = StringUtils.replace(value, "c$A", "ć");
        value = StringUtils.replace(value, "e$E", "ę");
        value = StringUtils.replace(value, "o$H", "õ");
        value = StringUtils.replace(value, "s$C", "ş");
        value = StringUtils.replace(value, "t$C", "ț");
        value = StringUtils.replace(value, "S>U$C", "Ş");
        value = StringUtils.replace(value, "a$O", "å");
        value = StringUtils.replace(value, "A>U$O", "Å");
        value = StringUtils.replace(value, "u$O", "ů");
        value = StringUtils.replace(value, "g$B", "ğ");
        value = StringUtils.replace(value, "g$B", "ĕ");
        value = StringUtils.replace(value, "a$B", "ă");
        value = StringUtils.replace(value, "l$/", "ł");
        value = StringUtils.replace(value, ">i", "ı");
        value = StringUtils.replace(value, "i$U", "ï");
        // Special-cases
        value = StringUtils.replace(value, "&yacute", "ý");
        value = StringUtils.replace(value, ">L", "Ł"); // corrected rule
        value = StringUtils.replace(value, "E>U$D", "З");
        value = StringUtils.replace(value, "S>U$E", "Ş");
        value = StringUtils.replace(value, "s$E", "ş");

        value = StringUtils.replace(value, "c$k", "č");
        value = StringUtils.replace(value, " U$K", " Š");

        value = StringUtils.replace(value, "O>U>!", "Ø");
        value = StringUtils.replace(value, "o>!", "ø");
        value = StringUtils.replace(value, "S$K", "Ŝ");
        value = StringUtils.replace(value, ">l", "ğ");

        value = StringUtils.replace(value, "§B>i", "ł");
        value = StringUtils.replace(value, "¤", "ń");

        return value;
    }


    /**
	 *  Stores taxa records in DB
	 */
	@Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        if(excludeFromImport(state)){
            return;
        }

        String lineNumber = "L#" + state.getCurrentLine() + ": ";
        LogUtils.setLevel(logger, Level.DEBUG);
        Map<String, String> record = state.getOriginalRecord();
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
        Taxon taxon = makeTaxon(record, state, higherTaxon, isFossil);
        if (taxon == null){
            logger.warn(lineNumber + "taxon could not be created and is null");
            return;
        }
        ((IAPTImportState)state).setCurrentTaxon(taxon);

        // Registration
        IAPTRegData regData = makeIAPTRegData(state);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String regdataJson = mapper.writeValueAsString(regData);
            Extension.NewInstance(taxon.getName(), regdataJson, getExtensionTypeIAPTRegData());
            getNameService().save(taxon.getName());
        } catch (JsonProcessingException e) {
            logger.error("Error on converting IAPTRegData", e);
        }

        logger.info("#of imported Genera: " + ((IAPTImportState) state).getGenusTaxonMap().size());
		return;
    }

    private boolean excludeFromImport(SimpleExcelTaxonImportState<CONFIG> state) {
        if(state.getConfig().isDoAlgeaeOnly()){
            boolean include = false;
            String higherTaxon = getValue(state.getOriginalRecord(), HIGHERTAXON, true);
            String fullNameStr = getValue(state.getOriginalRecord(), FULLNAME, true);
            include |= higherTaxon.matches(".*?PHYCEAE(?:$|\\s+)");
            for(String test : new String[]{
                    "Bolidophyceae ",
                    "Phaeothamniophyceae ",
                    "Bolidomonadales ",
                    "Bolidomonadaceae ",
                    "Aureoumbra ",
                    "Bolidomonas ",
                    "Seagriefia ",
                    "Navicula "
                }) {
                include |= fullNameStr.startsWith(test);
            }
            return !include;
        }

        return false;
    }

    private ExtensionType getExtensionTypeIAPTRegData() {
        if(extensionTypeIAPTRegData == null){
            extensionTypeIAPTRegData = ExtensionType.NewInstance("IAPTRegData.json", "IAPTRegData.json", "");
            getTermService().save(extensionTypeIAPTRegData);
        }
        return extensionTypeIAPTRegData;
    }

    private IAPTRegData makeIAPTRegData(SimpleExcelTaxonImportState<CONFIG> state) {

        Map<String, String> record = state.getOriginalRecord();
        String registrationStr = getValue(record, REGISTRATION);
        String regDateStr = getValue(record, REGDATE);
        String regStr = getValue(record, REGISTRATION, true);

        String dateStr = null;
        String office = null;
        Integer regID = null;
        Integer formNo = null;

        Matcher m = registrationPattern.matcher(registrationStr);
        if(m.matches()){
            dateStr = m.group("regdate");
            if(parseDate( regStr, dateStr) == null){
                // check for valid dates
                logger.warn(csvReportLine(regStr, REGISTRATION + ": could not parse date", dateStr, " in ", registrationStr));
            }
            office = m.group("office");
            regID = Integer.valueOf(m.group("regid"));
            try {
                formNo = Integer.valueOf(m.group("formNo"));
            } catch(IllegalArgumentException e){
                // ignore
            }
        } else {
            logger.warn(csvReportLine(regStr, REGISTRATION + ": could not be parsed", registrationStr));
        }
        IAPTRegData regData = new IAPTRegData(dateStr, office, regID, formNo);
        return regData;
    }

    private TaxonNode getHigherTaxon(String higherTaxaString, IAPTImportState state) {
        String[] higherTaxaNames = higherTaxaString.toLowerCase().replaceAll("[\\[\\]]", "").split(":");
        TaxonNode higherTaxonNode = null;

        ITaxonTreeNode rootNode = getClassificationRootNode(state);
        for (String htn :  higherTaxaNames) {
            htn = StringUtils.capitalize(htn.trim());
            Taxon higherTaxon = state.getHigherTaxon(htn);
            if (higherTaxon != null){
                higherTaxonNode = higherTaxon.getTaxonNodes().iterator().next();
            }else{
                IBotanicalName name = makeHigherTaxonName(state, htn);
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

    private IBotanicalName makeHigherTaxonName(IAPTImportState state, String name) {

        Rank rank = guessRank(name);

        IBotanicalName taxonName = TaxonNameFactory.NewBotanicalInstance(rank);
        taxonName.addSource(makeOriginalSource(state));
        taxonName.setGenusOrUninomial(StringUtils.capitalize(name));
        return taxonName;
    }

    private Rank guessRank(String name) {

        // normalize
        name = name.replaceAll("\\(.*\\)", "").trim();

        if(name.matches("^Plantae$|^Fungi$")){
           return Rank.KINGDOM();
        } else if(name.matches("^Incertae sedis$|^No group assigned$")){
           return rankFamilyIncertisSedis();
        } else if(name.matches(".*phyta$|.*mycota$")){
           return Rank.PHYLUM();
        } else if(name.matches(".*phytina$|.*mycotina$")){
           return Rank.SUBPHYLUM();
        } else if(name.matches("Gymnospermae$|.*ones$")){ // Monocotyledones, Dicotyledones
            return rankUnrankedSupraGeneric();
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

    private Rank rankUnrankedSupraGeneric() {

        if(rankUnrankedSupraGeneric == null){
            rankUnrankedSupraGeneric = Rank.NewInstance(RankClass.Suprageneric, "Unranked supra generic", " ", " ");
            getTermService().save(rankUnrankedSupraGeneric);
        }
        return rankUnrankedSupraGeneric;
    }

    private Rank rankFamilyIncertisSedis() {

        if(familyIncertisSedis == null){
            familyIncertisSedis = Rank.NewInstance(RankClass.Suprageneric, "Family incertis sedis", " ", " ");
            getTermService().save(familyIncertisSedis);
        }
        return familyIncertisSedis;
    }

    private AnnotationType annotationTypeCaveats(){
        if(annotationTypeCaveats == null){
            annotationTypeCaveats = AnnotationType.NewInstance("Caveats", "Caveats", "");
            getTermService().save(annotationTypeCaveats);
        }
        return annotationTypeCaveats;
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

    private MarkerType markerDuplicateRegistration(){
        if(this.duplicateRegistration == null){
            duplicateRegistration = MarkerType.NewInstance("duplicateRegistration", "duplicateRegistration", null);
            getTermService().save(this.duplicateRegistration);
        }
        return markerTypeFossil;
    }

    private String csvReportLine(String regId, String message, String ... fields){
        StringBuilder out = new StringBuilder("regID#");
        out.append(regId).append(",\"").append(message).append('"');

        for(String f : fields){
            out.append(",\"").append(f).append('"');
        }
        return out.toString();
    }


}
