/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.IArticle;
import eu.etaxonomy.cdm.model.reference.IBookSection;
import eu.etaxonomy.cdm.model.reference.IReference;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.persistence.query.MatchMode;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 23.10.2019
 */
@Component
public class IpniImport<CONFIG extends IpniImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -6691694003401153408L;
    private static final Logger logger = LogManager.getLogger();

    private static final String ID_COL = "EDIT-Genus-Taxon-ID";
    private static final String EDIT_GENUS = "EDIT-Genus";
    private static final String GENUS = "genus";
    private static final String SPECIES = "species";
    private static final String INFRA_SPECIES = "infraspecies";
    private static final String NAMECACHE = "full_name_without_family_and_authors";
    private static final String RANK = "rank";
    private static final String AUTHORS = "EMauthors";
    private static final String PTYPE = "PType";
    private static final String YEAR = "publication_year";
    private static final String PUBLICATION = "publication";
    private static final String EM_COLLATION = "EMCollation";
    private static final String REFERENCE_REMARKS = "reference_remarks";
    private static final String EM_GEO = "EM-geo";

    private Map<String,NamedArea> areaMap;

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();


    @Override
    protected String getWorksheetName(CONFIG config) {
        return "_11_IPNI_name_w_EM_genus_tax_m2";
    }

    private boolean isFirst = true;
    private TransactionStatus tx = null;
    private Map<UUID,TaxonNode> genusNodeMap = new HashMap<>();

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (isFirst){
            tx = this.startTransaction();
            isFirst = false;
        }
        getAreaMap();

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        String genusUuidStr = getValue(record, ID_COL);
        UUID genusUuid = UUID.fromString(genusUuidStr);
        TaxonNode genusNode = genusNodeMap.get(genusUuid);

        if (genusNode == null){
            genusNode = getGenusNode(state, genusUuid);
        }
        if (genusNode == null){
            return;
        }

        Rank rank = getRank(state);
        TaxonName taxonName = makeName(state, line, rank);
        getNameService().saveOrUpdate(taxonName);
        if (1 < getNameService().countByTitle(TaxonName.class, taxonName.getTitleCache(), MatchMode.EXACT, null)){
            Pager<TaxonName> candidates = getNameService().findByTitle(TaxonName.class, taxonName.getTitleCache(), MatchMode.EXACT, null, null, null, null, null);
            boolean fullMatchExists = false;
            for (TaxonName candidate : candidates.getRecords()){
                if (candidate.getId() != taxonName.getId() && candidate.getFullTitleCache().equals(taxonName.getFullTitleCache())){
                    logger.warn(line + "Possbile referenced name duplicate: " + taxonName.getFullTitleCache());
                    fullMatchExists = true;
                }
            }
            if (!fullMatchExists){
                logger.warn(line + "Possbile name duplicate: " + taxonName.getTitleCache());
            }
        }
        TaxonNode parent = getParent(state, line, genusNode, taxonName, rank);
        Reference sec = getSec(parent);
        Taxon taxon = Taxon.NewInstance(taxonName, sec);
        TaxonNode childNode = parent.addChildTaxon(taxon, null, null); //E+M taxon nodes usually do not have a citation
        getTaxonNodeService().saveOrUpdate(childNode);

        makeDistribution(state, line, taxon);
        addImportSource(state, taxon);
    }

    private Reference refEuroMed;
    private Reference getSec(TaxonNode parent) {
        Reference parentSec = parent.getTaxon().getSec();
        UUID ILDIS_UUID = UUID.fromString("23dbdf0b-00c3-4ca9-93f4-cefbae63bea1");
        UUID KEW_WORLD_CHECKLIST_UUID = UUID.fromString("ad2037fa-4a30-423b-a9f5-049e52e0e367");
        UUID EURO_MED_PLANTBASE = UUID.fromString("4b478ccf-12b2-495f-829d-4fb631d1fc5e");
        if(parentSec.getUuid().equals(KEW_WORLD_CHECKLIST_UUID)||
                parentSec.getUuid().equals(ILDIS_UUID)){
            if(refEuroMed == null){
                refEuroMed = getReferenceService().find(EURO_MED_PLANTBASE);
                if(refEuroMed == null){
                    logger.warn("refEuroMed not found!");
                }
            }
            return refEuroMed;
        }
        return parent.getTaxon().getSec();
    }

    private void addImportSource(SimpleExcelTaxonImportState<CONFIG> state, ISourceable<?> sourceable) {
        sourceable.addImportSource("row: "+state.getCurrentLine(), "_11_IPNI_name_w_EM_genus_tax_m2", getSourceReference(state), null);
    }

    private void makeDistribution(SimpleExcelTaxonImportState<CONFIG> state, String line, Taxon taxon) {

        //E+M
        NamedArea euroMedArea = getAreaMap().get("EM");
        if (euroMedArea == null){
            logger.warn("Euro+Med area not found");
        }
        TaxonDescription desc = TaxonDescription.NewInstance(taxon);
        Distribution endemicDistribution = Distribution.NewInstance(euroMedArea, PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA());
        desc.addElement(endemicDistribution);

        //single areas
        Map<String, String> record = state.getOriginalRecord();
        String allAreaStr = getValue(record, EM_GEO);
        if(isBlank(allAreaStr)){
            logger.warn(line+"No distribution data exists: " + taxon.getName().getTitleCache());
        }else{
            String[] areaSplit = allAreaStr.split(",");
            for (String areaStr: areaSplit){
                NamedArea area = getAreaMap().get(areaStr.trim());
                if (area == null){
                    logger.warn(line+"Area could not be recognized: " + areaStr.trim());
                }else{
                    //all distributions are native and are usually endemic, endemism will be checked later (ERS 2019-10-24)
                    Distribution distribution = Distribution.NewInstance(area, PresenceAbsenceTerm.NATIVE());
                    desc.addElement(distribution);
                }
            }
        }
    }

    private Map<String, NamedArea> getAreaMap() {
        if (areaMap == null){
            makeAreaMap();
        }
        return areaMap;
    }

    private void makeAreaMap() {
        areaMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        TermVocabulary<NamedArea> emAreaVoc = getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
        for (NamedArea area: emAreaVoc.getTerms()){
            areaMap.put(area.getIdInVocabulary(), area);
        }
    }

    private TaxonNode getParent(SimpleExcelTaxonImportState<CONFIG> state, String line, TaxonNode genusNode,
            TaxonName taxonName, Rank rank) {
        if (rank.equals(Rank.SPECIES())){
            return genusNode;
        }else{
            TaxonNode speciesNode = getSpecies(state, line, taxonName, genusNode);
            if (speciesNode == null){
                logger.warn(line + "Species for infraspecies not found. Added to genus: " + taxonName.getTitleCache());
                return genusNode;
            }else{
                return speciesNode;
            }
        }
    }

    private TaxonNode getSpecies(SimpleExcelTaxonImportState<CONFIG> state, String line, TaxonName taxonName, TaxonNode genusNode) {
        String specEpi = taxonName.getSpecificEpithet();
        TaxonNode result = null;
        for (TaxonNode child : genusNode.getChildNodes()){
            Rank childRank = child.getTaxon().getName().getRank();
            if (childRank.isSupraSpecific()){
                result = getSpecies(state, line, taxonName, child);
            }else if (childRank.isSupraSpecific()){
                //TODO why do we check for supraSpecific 2x here??
                //do nothing
            }else if (childRank.equals(Rank.SPECIES()) && specEpi.equals(child.getTaxon().getName().getSpecificEpithet())){
                    result = child;
            }
            if (result != null){
                return result;
            }
        }
        logger.debug(line+"No species found for subspecies " + taxonName.getTitleCache());
        return null;
    }

    private TaxonName makeName(SimpleExcelTaxonImportState<CONFIG> state, String line, Rank rank) {
        TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);
        Map<String, String> record = state.getOriginalRecord();
        String genusStr = getValue(record, GENUS);
        String speciesStr = getValue(record, SPECIES);
        String infraSpeciesStr = getValue(record, INFRA_SPECIES);
        String nameCache = getValue(record, NAMECACHE);
        name.setGenusOrUninomial(genusStr);
        name.setSpecificEpithet(speciesStr);
        name.setInfraSpecificEpithet(infraSpeciesStr);
        if (!nameCache.equals(name.getNameCache())){
            logger.warn(line + "Namecache not equal: " + nameCache +" <-> " + name.getNameCache());
        }
        TeamOrPersonBase<?>[] authors = getAuthors(state, line);
        //all authors are combination authors, no basionym authors exist, according to ERS 2019-10-24
        name.setCombinationAuthorship(authors[0]);
        name.setExCombinationAuthorship(authors[1]);

        Reference ref = getReference(state, line, authors[0]);
        name.setNomenclaturalReference(ref);
        String[] collSplit = getCollationSplit(state, line);
        name.setNomenclaturalMicroReference(collSplit[1]);
        makeNameRemarks(state, name);

        addImportSource(state, name);
        return name;
    }

    @SuppressWarnings("deprecation")
    private void makeNameRemarks(SimpleExcelTaxonImportState<CONFIG> state, TaxonName name) {
        Map<String, String> record = state.getOriginalRecord();
        String remarksStr = getValue(record, REFERENCE_REMARKS);
        if (isBlank(remarksStr) || remarksStr.equals("[epublished]")||remarksStr.equals("(epublished)")){
            return;
        }
        remarksStr = remarksStr.replace("[epublished]", "").trim();
        if (remarksStr.startsWith("(as")){
            remarksStr = remarksStr.substring(1, remarksStr.length()-1);
        }
        if (remarksStr.startsWith(";")){
            TaxonNameDescription desc = TaxonNameDescription.NewInstance(name);
            TextData textData = TextData.NewInstance(Feature.ADDITIONAL_PUBLICATION());
            textData.putText(Language.ENGLISH(), remarksStr.substring(1).trim());
            desc.addElement(textData);
        }else{
            if (remarksStr.startsWith(";")){
                remarksStr = remarksStr.substring(1).trim();
            }
            String regExStr = "^,?\\s*as ['\"]([\\-a-z]+)['\"]$";
            Matcher matcher = Pattern.compile(regExStr).matcher(remarksStr);
            if (!matcher.matches()){
                logger.warn("name remark does not match: " +  remarksStr);
            }else{
                String origSpelling = matcher.group(1);
                TaxonName origName = TaxonNameFactory.NewBotanicalInstance(name.getRank());
                getNameService().save(origName);
                origName.setGenusOrUninomial(name.getGenusOrUninomial());
                if (name.isSpecies()){
                    origName.setSpecificEpithet(origSpelling);
                }else{
                    origName.setSpecificEpithet(name.getSpecificEpithet());
                    origName.setInfraSpecificEpithet(origSpelling);
                }
                name.setOriginalSpelling(origName);
            }
        }
    }

    private Reference getReference(SimpleExcelTaxonImportState<CONFIG> state, String line,
            TeamOrPersonBase<?> authors) {
        Map<String, String> record = state.getOriginalRecord();
        String pTypeStr = getValue(record, PTYPE);
        Reference result;
        if("AR".equals(pTypeStr)){
            IArticle example = ReferenceFactory.newArticle();
            example.setAuthorship(authors);
            String[] collSplit = getCollationSplit(state, line);
            example.setVolume(collSplit[0]);
            example.setDatePublished(getYear(state));
            Reference journal = getExistingJournal(state, line);
            example.setInJournal(journal);
            result = getExistingArticle(state, line, example);
            if(result != example){
                logger.debug(line+ "article existed");
            }else{
                makeReferenceRemarks(state, example);
            }
        }else if ("BS".equals(pTypeStr)){
            IBookSection example = ReferenceFactory.newBookSection();
            String publicationStr = getValue(record, PUBLICATION);
            String authorsForFlIber = getValue(record, AUTHORS);
            TeamOrPersonBase<?> bookAuthor = getBookSectionBookAuthors(line, publicationStr, authorsForFlIber);
            if (bookAuthor == null){
                logger.warn(line + "No author found for booksection of " + publicationStr);
            }
            Reference book = getExistingBook(state, line, bookAuthor);
            example.setInBook(book);
            example.setAuthorship(authors);
            result = getExistingBookSection(state, line, example);
            if (result != example){
                logger.debug(line+ "book section existed");
            }else{
                makeReferenceRemarks(state, example);
            }
        }else if ("BO".equals(pTypeStr)){
            result = getExistingBook(state, line, authors);
        }else{
            logger.warn(line + "Reference type not recognized: " +  pTypeStr);
            return null;
        }
        return result;
    }

    private TeamOrPersonBase<?> getBookSectionBookAuthors(String line,
            String publicationStr, String authorsForFlIber) {
        if ("Fl. Gr. Brit. Ireland".equals(publicationStr)){
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("009cda5a-f6a7-41bf-a323-dc72f83e6066")),Team.class);
        }else if ("Div. Veg. Yeseras Ibér.".equals(publicationStr)){
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("94c79bdf-1eb8-4094-94bc-686ce55e00f1")),Team.class);
        }else if ("Durum Wheat Breeding".equals(publicationStr)){
            Team team = Team.NewInstance();
            team.setHasMoreMembers(true);
            Person person1 = Person.NewInstance();
            person1.setFamilyName("Roya");
            Person person2 = Person.NewInstance();
            person2.setFamilyName("Conxita");
            team.addTeamMember(person1);
            team.addTeamMember(person2);
            getAgentService().save(team);
            return team;
        }else if ("Fl. Iber.".equals(publicationStr)){
            if(authorsForFlIber.equals("Pedrol, J. J. Regalado & López Encina")){
                //20
                return CdmBase.deproxy(getAgentService().find(UUID.fromString("fbac0541-0876-4cc1-86c5-6e0c34ad90c1")), Person.class);
            }else if(authorsForFlIber.equals("L. Sáez, Juan, M. B. Crespo, F. B. Navarro, J. Peñas & Roquet")){
                //13
                return CdmBase.deproxy(getAgentService().find(UUID.fromString("85db9f56-cf58-4655-8775-69bf097e560c")), Person.class);
            }else{
                logger.warn(line+"Author for Fl. Iber not found: " + authorsForFlIber);
            }
        }else if ("Fl. Valentina".equals(publicationStr)){
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("64cc172a-912b-49f3-ad52-2ce95083668d")),Team.class);
        }else if ("Fl. Reipubl. Popularis Bulg.".equals(publicationStr)){
//            return (Team)getAgentService().find(UUID.fromString("64cc172a-912b-49f3-ad52-2ce95083668d"));
        }else if ("Ill. Fl. Turkey Vol. 2.".equals(publicationStr)){
            logger.warn("Hanlde volume for Ill. Fl. Turkey Vol. 2. correctly");
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("14a81430-18c6-418c-90c4-a08774f4955a")),Person.class);
        }else if ("Türk. Geofitleri".equals(publicationStr)){
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("fe73ca78-3e75-42aa-93cf-b767ae3600ab")),Person.class);
        }else if ("Türk. Bitkileri List.".equals(publicationStr)){
            return CdmBase.deproxy(getAgentService().find(UUID.fromString("87008d44-7923-41e2-942c-bc1b284a2e3b")),Team.class);


        }
        return null;
    }

    private void makeReferenceRemarks(SimpleExcelTaxonImportState<CONFIG> state, IReference ref) {
        Map<String, String> record = state.getOriginalRecord();
        String remarksStr = getValue(record, REFERENCE_REMARKS);
        if (isBlank(remarksStr)){
            return;
        }
        if (remarksStr.contains("epublished")){
            MarkerType epublished = getMarkerType(state, MarkerType.uuidEpublished, "epublished", "epublished", null);
            ref.addMarker(Marker.NewInstance(epublished, true));
        }
    }

    private String[] getCollationSplit(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String collationStr = getValue(record, EM_COLLATION);
        String[] split = collationStr.split(":");
        if (split.length == 2){
            split[0] = split[0].trim();
            split[1] = split[1].trim();
            return split;
        }else if (split.length == 1){
            String[] result = new String[2];
            result[0] = null;
            result[1] = split[0].trim();
            return result;
        }else{
            logger.warn(line+"Collation string not recognized: " + collationStr);
            return new String[2];
        }
    }

    private Map<String,Reference> bookMap = new HashMap<>();
    private Map<String,Reference> bookSectionMap = new HashMap<>();
    private Map<String,Reference> journalMap = new HashMap<>();
    private Map<String,Reference> articleMap = new HashMap<>();


    private Reference getExistingBookSection(SimpleExcelTaxonImportState<CONFIG> state, String line, IBookSection example) {
        Set<String> includeProperties = new HashSet<>();
        includeProperties.add("abbrevTitleCache");
        includeProperties.add("datePublished");
        includeProperties.add("type");
        return getExistingMainRef(state, line, bookSectionMap, (Reference)example, example.getAbbrevTitleCache(), "book section", includeProperties);
    }

    private Reference getExistingArticle(SimpleExcelTaxonImportState<CONFIG> state, String line, IArticle example) {
        Set<String> includeProperties = new HashSet<>();
        includeProperties.add("abbrevTitleCache");
        includeProperties.add("volume");
        includeProperties.add("datePublished");
        includeProperties.add("type");
        return getExistingMainRef(state, line, articleMap, (Reference)example, example.getAbbrevTitleCache(), "article", includeProperties);
    }

    private Reference getExistingJournal(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Reference example = ReferenceFactory.newJournal();
        Map<String, String> record = state.getOriginalRecord();
        String publicationStr = getValue(record, PUBLICATION);
        example.setAbbrevTitle(publicationStr);
        Set<String> includeProperties = new HashSet<>();
        includeProperties.add("abbrevTitle");
        includeProperties.add("type");
        return getExistingMainRef(state, line, journalMap, example, publicationStr, "journal", includeProperties);
    }

    private Reference getExistingBook(SimpleExcelTaxonImportState<CONFIG> state, String line, TeamOrPersonBase<?> author) {
        Reference example = ReferenceFactory.newBook();
        Map<String, String> record = state.getOriginalRecord();
        String publicationStr = getValue(record, PUBLICATION);
        example.setAbbrevTitle(publicationStr);
        String[] collSplit = getCollationSplit(state, line);
        example.setVolume(collSplit[0]);
        example.setDatePublished(getYear(state));
        example.setAuthorship(author);

        Set<String> includeProperties = new HashSet<>();
        includeProperties.add("abbrevTitleCache");
        includeProperties.add("volume");
        includeProperties.add("datePublished");
        includeProperties.add("authorship");
        includeProperties.add("type");
        Reference result = getExistingMainRef(state, line, bookMap, example, example.getAbbrevTitleCache(), "book", includeProperties);
        if (result != example){
            logger.debug("book existed");
        }else{
            makeReferenceRemarks(state, example);
        }
        return result;
    }

    private Reference getExistingMainRef(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String,Reference> map, Reference example, String publicationStr, String type, Set<String> includeProperties) {
        if (map.get(publicationStr)!= null){
            return map.get(publicationStr);
        }else{
            List<Reference> existingRefs = getReferenceService().list(example, includeProperties, null, null, null, null);
            if (existingRefs.isEmpty()){
                logger.warn(line + "New " + type + ": " + publicationStr);
                map.put(publicationStr, example);
                addImportSource(state, example);
                return example;
            }else{
                existingRefs = findBestMatchingRef(existingRefs, publicationStr);
                if(existingRefs.size()>1){
                    logger.warn(line+"More than 1 reference found for " + publicationStr + ". Use arbitrary one.");
                }
                Reference result = existingRefs.get(0);
                map.put(publicationStr, result);
                return result;
            }
        }
    }

    private List<Reference> findBestMatchingRef(List<Reference> existingRefs, String publicationStr) {
        Set<Reference> noTitleCandidates = new HashSet<>();
        Set<Reference> sameTitleCandidates = new HashSet<>();
        for(Reference ref : existingRefs){
            if (ref.getTitleCache().equals(publicationStr)){
                if(ref.getTitle() == null){
                    noTitleCandidates.add(ref);
                }else{
                    sameTitleCandidates.add(ref);
                }
            }
        }
        if(!noTitleCandidates.isEmpty()){
            return new ArrayList<>(noTitleCandidates);
        }else if(!sameTitleCandidates.isEmpty()){
            return new ArrayList<>(sameTitleCandidates);
        }else{
            return existingRefs;
        }
    }

    private VerbatimTimePeriod getYear(SimpleExcelTaxonImportState<CONFIG> state) {
        Map<String, String> record = state.getOriginalRecord();
        String yearStr = getValue(record, YEAR);
        VerbatimTimePeriod result = TimePeriodParser.parseStringVerbatim(yearStr);
        return result;
    }

    private TeamOrPersonBase<?>[] getAuthors(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String authorsStr = getValue(record, AUTHORS);
        String[] split = authorsStr.split(" ex ");
        TeamOrPersonBase<?>[] result = new TeamOrPersonBase<?>[2];
        if (split.length == 1){
            result[0] = getAuthor(state, line, split[0]);
        }else{
            result[0] = getAuthor(state, line, split[1]);
            result[1] = getAuthor(state, line, split[0]);
        }
        return result;
    }

    private Map<String,TeamOrPersonBase<?>> authorMap = new HashMap<>();
    private TeamOrPersonBase<?> getAuthor(SimpleExcelTaxonImportState<CONFIG> state, String line, String authorsStr) {
        if (authorMap.get(authorsStr)!= null){
            return authorMap.get(authorsStr);
        }else{
            TeamOrPersonBase<?> example = parser.author(authorsStr);
            String testStr = example.getNomenclaturalTitleCache();
            if(!authorsStr.equals(testStr)){
               logger.warn("nom title is not equal");
            }
            Set<String> includeProperties = new HashSet<>();
            includeProperties.add("nomenclaturalTitle");
            List<TeamOrPersonBase<?>> existingAuthors = getAgentService().list(example, includeProperties, null, null, null, null);
            if (existingAuthors.isEmpty()){
                logger.info(line+"New author: " + authorsStr);
                authorMap.put(authorsStr, example);
                if (example instanceof Team){
                    List<Person> members = ((Team)example).getTeamMembers();
                    for (int i = 0; i < members.size();i++){
                        Person newPerson = members.get(i);
                        Person existingPerson = getExistingPerson(state, line, newPerson);
                        if (newPerson != existingPerson){
                            members.set(i, existingPerson);
                        }

                    }
                }
                addImportSource(state, example);
                return example;
            }else{
                if(existingAuthors.size()>1){
                    existingAuthors = findBestMatchingAuthor(existingAuthors, authorsStr);
                    if(existingAuthors.size()>1){
                        logger.warn(line+"More than 1 author with same matching found for '" + authorsStr + "'. Use arbitrary one.");
                    }else{
                        logger.debug(line+"Found exactly 1 author with same matching for " +authorsStr);
                    }
                }else{
                    logger.debug(line+"Found exactly 1 author");
                }
                TeamOrPersonBase<?> result = existingAuthors.get(0);
                authorMap.put(authorsStr, result);
                return result;
            }
        }
    }

    private Person getExistingPerson(SimpleExcelTaxonImportState<CONFIG> state, String line, Person newPerson) {
        String authorsStr = newPerson.getNomenclaturalTitleCache();
        if (authorMap.get(authorsStr)!= null){
            return (Person)authorMap.get(authorsStr);
        }else{
            Set<String> includeProperties = new HashSet<>();
            includeProperties.add("nomenclaturalTitle");
            List<Person> existingPersons = getAgentService().list(newPerson, includeProperties, null, null, null, null);
            if (existingPersons.isEmpty()){
                logger.warn(line+"New person: " + authorsStr);
                authorMap.put(authorsStr, newPerson);
                addImportSource(state, newPerson);
                return newPerson;
            }else{
                if(existingPersons.size()>1){
                    existingPersons = findBestMatchingPerson(existingPersons, authorsStr);
                    if(existingPersons.size()>1){
                        existingPersons = findBestMatchingPerson(existingPersons, authorsStr);
                        logger.warn(line+"More than 1 person with same matching found for '" + authorsStr + "'. Use arbitrary one.");
                    }else{
                        logger.debug(line+"Found exactly 1 person with same matching for " +authorsStr);
                    }
                }else{
                    logger.debug(line+"Found exactly 1 person");
                }
                Person result = existingPersons.get(0);
                authorMap.put(authorsStr, result);
                return result;
            }
        }
    }

    private List<TeamOrPersonBase<?>> findBestMatchingAuthor(List<TeamOrPersonBase<?>> existingAuthors,
            String authorsStr) {
        Set<TeamOrPersonBase<?>> noTitleCandidates = new HashSet<>();
        Set<TeamOrPersonBase<?>> sameTitleCandidates = new HashSet<>();
        for(TeamOrPersonBase<?> author : existingAuthors){
            if (author.getTitleCache().equals(authorsStr)){
                sameTitleCandidates.add(author);
            }
        }
        if(!noTitleCandidates.isEmpty()){
            return new ArrayList<>(noTitleCandidates);
        }else if(!sameTitleCandidates.isEmpty()){
            return new ArrayList<>(sameTitleCandidates);
        }else{
            return existingAuthors;
        }
    }

    private List<Person> findBestMatchingPerson(
            List<Person> existingPersons, String authorsStr) {

        Set<Person> noTitleCandidates = new HashSet<>();
        Set<Person> sameTitleCandidates = new HashSet<>();
        for(Person person : existingPersons){
            if (person.getTitleCache().equals(authorsStr)){
                sameTitleCandidates.add(person);
            }
        }
        if(!noTitleCandidates.isEmpty()){
            return new ArrayList<>(noTitleCandidates);
        }else if(!sameTitleCandidates.isEmpty()){
            return new ArrayList<>(sameTitleCandidates);
        }else{
            return existingPersons;
        }
    }

    private Rank getRank(SimpleExcelTaxonImportState<CONFIG> state) {
        Map<String, String> record = state.getOriginalRecord();
        String rankStr = getValue(record, RANK);
        if ("spec.".equals(rankStr)){
            return Rank.SPECIES();
        }else if ("subsp.".equals(rankStr)){
            return Rank.SUBSPECIES();
        }else{
            logger.warn("Unknown rank: " + rankStr);
            return null;
        }
    }

    private TaxonNode getGenusNode(SimpleExcelTaxonImportState<CONFIG> state, UUID genusUuid) {
        Taxon genusTaxon = (Taxon)getTaxonService().find(genusUuid);
        validateGenus(state, genusTaxon);
        Set<TaxonNode> nodes = genusTaxon.getTaxonNodes();
        if (nodes.size()==0){
            logger.warn("No genus node: " + genusTaxon.getTitleCache());
        }else if (nodes.size()>1){
            logger.warn("More than 1 genus node: " + genusTaxon.getTitleCache());
        }else{
            TaxonNode result = nodes.iterator().next();
            genusNodeMap.put(genusUuid, result);
            return result;
        }
        return null;
    }

    private void validateGenus(SimpleExcelTaxonImportState<CONFIG> state, Taxon genusTaxon) {
        Map<String, String> record = state.getOriginalRecord();
        String editGenus = getValue(record, EDIT_GENUS);
        if (!editGenus.equals(genusTaxon.getName().getTitleCache())){
            logger.warn("Full genus not equal: " + editGenus +" <-> "+genusTaxon.getName().getTitleCache());
        }
        String genus = getValue(record, GENUS);
        if (!genus.equals(genusTaxon.getName().getNameCache())){
            logger.warn("Genus not equal: " + genus +" <-> "+genusTaxon.getName().getNameCache());
        }
    }


    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }
}
