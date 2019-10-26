/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
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
import eu.etaxonomy.cdm.model.reference.IBook;
import eu.etaxonomy.cdm.model.reference.IBookSection;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
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

    private static final Logger logger = Logger.getLogger(IpniImport.class);

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

    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>> deduplicationHelper;
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
        TaxonNode parent = getParent(state, line, genusNode, taxonName, rank);
        Reference sec = parent.getTaxon().getSec();
        Taxon taxon = Taxon.NewInstance(taxonName, sec);
        TaxonNode childNode = parent.addChildTaxon(taxon, null, null); //E+M taxon nodes usually do not have a citation
        getTaxonNodeService().saveOrUpdate(childNode);

        makeDistribution(state, line, taxon);
        addImportSource(state, taxon);
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
            logger.warn(line+"No distribution data exists.");
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
            if (childRank.isHigher(Rank.SPECIES())){
                result = getSpecies(state, line, taxonName, child);
            }else if (childRank.isHigher(Rank.SPECIES())){
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
        TeamOrPersonBase<?> authors = getAuthors(state, line);
        //all authors are combination authors, no basionym authors exist, according to ERS 2019-10-24
        name.setCombinationAuthorship(authors);
        Reference ref = getReference(state, line, authors);
        name.setNomenclaturalReference(ref);
        String[] collSplit = getCollationSplit(state, line);
        name.setNomenclaturalMicroReference(collSplit[1]);
        makeNameRemarks(state, line, name);

        addImportSource(state, name);
        return name;
    }

    @SuppressWarnings("deprecation")
    private void makeNameRemarks(SimpleExcelTaxonImportState<CONFIG> state, String line, TaxonName name) {
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
                name.addOriginalSpelling(origName, getSourceReference(state), null);
            }
        }
    }

    private Reference getReference(SimpleExcelTaxonImportState<CONFIG> state, String line,
            TeamOrPersonBase<?> authors) {
        Map<String, String> record = state.getOriginalRecord();
        String pTypeStr = getValue(record, PTYPE);
        Reference result;
        if("AR".equals(pTypeStr)){
            result = ReferenceFactory.newArticle();
            IArticle article = result;
            Reference journal = getJournal(state, line);
            article.setInJournal(journal);
            String[] collSplit = getCollationSplit(state, line);
            article.setVolume(collSplit[0]);
            article.setDatePublished(getYear(state, line));
            makeReferenceRemarks(state, line, article);
        }else if ("BS".equals(pTypeStr)){
            result = ReferenceFactory.newBookSection();
            IBookSection section = result;
            Reference book = getBook(state, line);
            section.setInBook(book);
            String[] collSplit = getCollationSplit(state, line);
            book.setVolume(collSplit[0]);
            book.setDatePublished(getYear(state, line));
            //TODO in-authors (woher nehmen?)
        }else if ("BO".equals(pTypeStr)){
            result = getBook(state, line);
            IBook book = result;
            String[] collSplit = getCollationSplit(state, line);
            book.setVolume(collSplit[0]);
            book.setDatePublished(getYear(state, line));
        }else{
            logger.warn(line + "Reference type not recognized: " +  pTypeStr);
            return null;
        }
        result.setAuthorship(authors);
        //TODO deduplicate references
        //TODO add source to references
//        addImportSource(state, result);
        return result;
    }

    private void makeReferenceRemarks(SimpleExcelTaxonImportState<CONFIG> state, String line, IArticle article) {
        Map<String, String> record = state.getOriginalRecord();
        String remarksStr = getValue(record, REFERENCE_REMARKS);
        if (isBlank(remarksStr)){
            return;
        }
        if (remarksStr.contains("epublished")){
            MarkerType epublished = getMarkerType(state, MarkerType.uuidEpublished, "epublished", "epublished", null);
            article.addMarker(Marker.NewInstance(epublished, true));
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

    private Reference getBook(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String publicationStr = getValue(record, PUBLICATION);
        Reference result = ReferenceFactory.newBook();
        result.setAbbrevTitle(publicationStr);
        return result;
    }

    private Reference getJournal(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String publicationStr = getValue(record, PUBLICATION);
        Reference result = ReferenceFactory.newJournal();
        result.setAbbrevTitle(publicationStr);
        return result;
    }

    private VerbatimTimePeriod getYear(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String yearStr = getValue(record, YEAR);
        VerbatimTimePeriod result = TimePeriodParser.parseStringVerbatim(yearStr);
        return result;
    }

    private TeamOrPersonBase<?> getAuthors(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Map<String, String> record = state.getOriginalRecord();
        String authorsStr = getValue(record, AUTHORS);
        TeamOrPersonBase<?> newAuthor = parser.author(authorsStr);
        TeamOrPersonBase<?> author = newAuthor; //deduplicationHelper().getExistingAuthor(state, newAuthor);
        //TODO check parsing + deduplication of authors
        return author;
    }

    @SuppressWarnings("unchecked")
    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>> deduplicationHelper() {
        if (deduplicationHelper == null){
            deduplicationHelper = (ImportDeduplicationHelper<SimpleExcelTaxonImportState<?>>)ImportDeduplicationHelper.NewInstance(this);
        }
        return deduplicationHelper;
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
