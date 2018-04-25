/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImplRegExBase;

/**
 * @author a.mueller
 * @since 18.12.2015
 *
 */
@Component
public class EdaphobaseTaxonImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = -9138378836474086070L;
    private static final Logger logger = Logger.getLogger(EdaphobaseTaxonImport.class);

    private static final String tableName = "tax_taxon";

    private static final String pluralString = "taxa";

    private static final Object AUTHOR_NAMESPACE = "tax_author_name";

    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseTaxonImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return "SELECT DISTINCT taxon_id FROM tax_taxon t "
                + " ORDER BY taxon_id";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = " SELECT DISTINCT t.*, r.value_summary as rankStr, pr.value_summary as parentRankStr, ppr.value_summary as grandParentRankStr, pppr.value_summary as grandGrandParentRankStr, "
                    + " pt.name as parentName, ppt.name as grandParentName, pppt.name as grandGrandParentName "
                + " FROM tax_taxon t "
                    + " LEFT JOIN tax_taxon pt ON t.parent_taxon_fk = pt.taxon_id "
                    + " LEFT JOIN tax_taxon ppt ON pt.parent_taxon_fk = ppt.taxon_id "
                    + " LEFT JOIN tax_taxon pppt ON ppt.parent_taxon_fk = pppt.taxon_id "
                    + " LEFT OUTER JOIN selective_list.element r ON r.element_id = t.tax_rank_fk "
                    + " LEFT OUTER JOIN selective_list.element pr ON pr.element_id = pt.tax_rank_fk "
                    + " LEFT OUTER JOIN selective_list.element ppr ON ppr.element_id = ppt.tax_rank_fk "
                    + " LEFT OUTER JOIN selective_list.element pppr ON pppr.element_id = pppt.tax_rank_fk "
                + " WHERE t.taxon_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(EdaphobaseImportState state) {
        makeIncludedInList(state);
        super.doInvoke(state);
    }

    private Set<Integer> includedInTaxa = new HashSet<>();

    /**
     * @param state
     */
    private void makeIncludedInList(EdaphobaseImportState state) {
        String sql = "SELECT sr.a_taxon_fk_taxon_id "
                + " FROM tax_synonym sr "
                + " WHERE sr.synonym_role <> 11614 ";
        ResultSet rs = state.getConfig().getSource().getResultSet(sql);
        try {
            while (rs.next()){
                Integer synId = rs.getInt("a_taxon_fk_taxon_id");
                includedInTaxa.add(synId);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
        try {
            while (rs.next()){
                makeSingleTaxon(state, rs, taxaToSave);
            }
        } catch (SQLException | UndefinedTransformerMethodException e) {
             e.printStackTrace();
        }

        getTaxonService().saveOrUpdate(taxaToSave);
        return true;
    }

    /**
     * @param state
     * @param rs
     * @param taxaToSave
     * @throws SQLException
     * @throws UndefinedTransformerMethodException
     */
    private void makeSingleTaxon(EdaphobaseImportState state, ResultSet rs, Set<TaxonBase> taxaToSave)
            throws SQLException, UndefinedTransformerMethodException {
        Integer id = nullSafeInt(rs, "taxon_id");
        Integer year = nullSafeInt(rs, "tax_year");
        boolean isBrackets = rs.getBoolean("tax_brackets");
        String remark = rs.getString("remark");
        String nameStr = rs.getString("name");
        String authorName = rs.getString("tax_author_name");
        //parentTaxonFk
        //rankFk
        Integer nomRefId = nullSafeInt(rs, "tax_document");
        boolean isValid = rs.getBoolean("valid");
        boolean isDeleted = rs.getBoolean("deleted");
        String displayString = rs.getString("display_string");
        Integer version = nullSafeInt(rs, "versionfield");
        String pages = rs.getString("pages");
        String treeIndex = rs.getString("path_to_root");
//      Integer rankFk = nullSafeInt(rs, "tax_rank_fk");
        String nameAddition = rs.getString("name_addition");
        String officialRemark = rs.getString("official_remark");
        boolean isGroup = rs.getBoolean("taxonomic_group");
        String rankStr = rs.getString("rankStr");
        String parentRankStr = rs.getString("parentRankStr");
        String grandParentRankStr = rs.getString("grandParentRankStr");
        String grandGrandParentRankStr = rs.getString("grandGrandParentRankStr");
        String parentNameStr = rs.getString("parentName");
        String grandParentNameStr = rs.getString("grandParentName");
        String grandGrandParentNameStr = rs.getString("grandGrandParentName");
        String editUuid = rs.getString("edit_uuid");


        if (isDeleted){
            logger.warn("Deleted not handled according to mail Stephan 2018-03-07. ID: " + id );
            return;
        }
        boolean nameAdditionUsed =  isBlank(nameAddition);
        if (!nameAdditionUsed){
            nameAddition = nameAddition.trim();
        }

        isValid = checkValid(state, id, isValid);

        //for debug only
        if (id.equals(979370000) ){
            logger.debug("now");
        }

        TaxonBase<?> taxonBase;

        rankStr= extractEnglish(rankStr);
        parentRankStr= extractEnglish(parentRankStr);
        grandParentRankStr= extractEnglish(grandParentRankStr);
        grandGrandParentRankStr= extractEnglish(grandGrandParentRankStr);

        //Name etc.
        Rank rank = makeRank(state, rankStr);
        checkRankMarker(state, rank);
        IZoologicalName name = TaxonNameFactory.NewZoologicalInstance(rank);
        if (rank == null){
            name.setNameCache(nameStr, true);
        }else{
            setNamePart(nameStr, rank, name);
            Rank parentRank = makeRank(state, parentRankStr);
            setNamePart(parentNameStr, parentRank, name);
            Rank parentParentRank = makeRank(state, grandParentRankStr);
            setNamePart(grandParentNameStr, parentParentRank, name);
            Rank grandParentParentRank = makeRank(state, grandGrandParentRankStr);
            setNamePart(grandGrandParentNameStr, grandParentParentRank, name);
            if (grandParentParentRank != null && grandParentParentRank.isLower(Rank.GENUS()) || isBlank(name.getGenusOrUninomial()) && !name.isProtectedNameCache()){
                logger.warn("Grand-Grandparent rank is lower than genus for " +
                        name.getTitleCache() + " (edapho-id: " + id + "; cdm-id: " + name.getId() + ")");
            }
        }

        //Authors
        if (isNotBlank(authorName)){
            authorName = authorName.replace(" et ", " & ");
            TeamOrPersonBase<?> author = state.getRelatedObject(AUTHOR_NAMESPACE, authorName, TeamOrPersonBase.class);
            if (author == null){
                logger.warn("Author not found in state: "  + authorName);
            }else{
                if (isBrackets){
                    name.setBasionymAuthorship(author);
                    name.setOriginalPublicationYear(year);
                }else{
                    name.setCombinationAuthorship(author);
                    name.setPublicationYear(year);
                }
            }
        }

        String capitalWord = NonViralNameParserImplRegExBase.capitalWord;
        String autNam = "(" + capitalWord + "( in "+capitalWord+")?|Schuurmans Stekhoven|Winiszewska-Ślipińska|Fürst von Lieven|de Coninck|de Man|de Ley|de Grisse|"
                + "van der Linde|Pschorn-Walcher|van der Berg|J. Goddey)";
        if (isNotBlank(nameAddition) && nameAddition.matches("(\\[|\\()?nomen.*")){
            if ("(nomen oblitum)".equals(nameAddition) ){
                name.addStatus(NomenclaturalStatusType.ZOO_OBLITUM(), null, null);
            }else if ("nomen dubium".equals(nameAddition) || "[nomen dubium]".equals(nameAddition)){
                name.addStatus(NomenclaturalStatusType.DOUBTFUL(), null, null);
            }else if ("nomen nudum".equals(nameAddition)){
                name.addStatus(NomenclaturalStatusType.NUDUM(), null, null);
            }else if (nameAddition.matches("nomen nudum \\["+autNam+"\\, 19\\d{2}]")){
                name.addStatus(NomenclaturalStatusType.NUDUM(), null, null);
                Person nomNudAuthor = parseNomenNudumAuthor(state, name, nameAddition);
                if (name.getCombinationAuthorship()!= null || name.getBasionymAuthorship() != null){
                    logger.warn("Author already exists for nomen nudum name with author. ID: " + id);
                }
                name.setCombinationAuthorship(nomNudAuthor);
            }else{
                logger.warn("'nomen xxx' name addition not recognized: " + nameAddition + ". ID: " + id);
            }
            nameAdditionUsed = true;
        }
        if (isNotBlank(nameAddition) && nameAddition.matches(autNam + "((, "+autNam+")? & " + autNam + ")?" +    ", \\d{4}")){
            nameAddition = nameAddition.replace(" et ", " & ");
            int pos = nameAddition.length()-6;
            String authorStr = nameAddition.substring(0, pos);
            Integer naYear = Integer.valueOf(nameAddition.substring(pos +  2));
            if (name.getPublicationYear() != null){
                logger.warn("Publication year already exists. ID=" +  id);
            }
            name.setPublicationYear(naYear);
            TeamOrPersonBase<?> author = getNameAdditionAuthor(authorStr);
            if (name.getCombinationAuthorship() != null){
                logger.warn("Combination author already exists. ID=" +  id);
            }
            name.setCombinationAuthorship(author);
            nameAdditionUsed = true;
        }
        if (isNotBlank(nameAddition) && nameAddition.matches("(nec|non) " + capitalWord +  ", \\d{4}")){
            String str = nameAddition.substring(4);
            String[] split = str.split(",");
            IZoologicalName homonym = (IZoologicalName)name.clone();
            homonym.setCombinationAuthorship(null);
            homonym.setBasionymAuthorship(null);
            homonym.setPublicationYear(null);
            homonym.setOriginalPublicationYear(null);
            TeamOrPersonBase<?> author = getNameAdditionAuthor(split[0]);
            homonym.setCombinationAuthorship(author);
            homonym.setPublicationYear(Integer.valueOf(split[1].trim()));
            nameAdditionUsed = true;
        }

        //nomRef
        if (nomRefId != null){
            Reference nomRef = state.getRelatedObject(REFERENCE_NAMESPACE, String.valueOf(nomRefId), Reference.class);
            if (nomRef == null){
                logger.warn("Reference " + nomRefId + " could not be found");
            }
            name.setNomenclaturalReference(nomRef);
        }
        name.setNomenclaturalMicroReference(isBlank(pages)? null : pages);

        //taxon
        Reference secRef = state.getRelatedObject(REFERENCE_NAMESPACE, state.getConfig().getSecUuid().toString(), Reference.class);
        if (secRef == null){
            secRef = makeSecRef(state);
        }
        if (isValid){
            taxonBase = Taxon.NewInstance(name, secRef);
        }else{
            taxonBase = Synonym.NewInstance(name, secRef);
        }
        handleTaxonomicGroupMarker(state, taxonBase, isGroup);
        taxaToSave.add(taxonBase);

        //sensu, auct.
        if (isNotBlank(nameAddition) && (nameAddition.startsWith("sensu ") || "auct.".equals(nameAddition))){
            nameAddition = nameAddition.replace(" et ", " & ");
            taxonBase.setSec(null);
            taxonBase.setAppendedPhrase(nameAddition);
            //TODO
            nameAdditionUsed = true;
        }

        //remarks
        doNotes(taxonBase, remark, AnnotationType.TECHNICAL());
        doNotes(taxonBase, officialRemark, AnnotationType.EDITORIAL());

        //id
        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
        taxonBase.setUuid(UUID.fromString(editUuid));
        handleExampleIdentifiers(taxonBase, id);

        if (!nameAdditionUsed){
            logger.warn("name_addition not recognized: " +  nameAddition + ". ID="+id);
            name.setAppendedPhrase(nameAddition);
        }

        if (titleCacheDiffers(state, displayString, name, taxonBase)){
            String titleCache = taxonBase.getAppendedPhrase() != null ? taxonBase.getTitleCache() : name.getTitleCache();
            logger.warn("Displaystring differs from titleCache. ID=" + id + ".\n   " + displayString + "\n   " + titleCache);
        }
    }


    /**
     * @param state
     * @param displayString
     * @param name
     * @param taxonBase
     * @return
     */
    private boolean titleCacheDiffers(EdaphobaseImportState state, String displayString, IZoologicalName name, TaxonBase<?> taxonBase) {
        String orig = displayString.replace("nomen nudum [Hirschmann, 1951]", "Hirschmann, 1951")
                .replace("  ", " ");
        String nameTitleCache = name.getTitleCache().replace("species group", "group");
        String taxonTitleCache = taxonBase.getTitleCache().replace("species group", "group");

//        if (state.getConfig().isIgnore4nomial() && orig.matches(".* subsp"))
        boolean result =
                !orig.equals(nameTitleCache)
                && !orig.equals(name.getFullTitleCache())
                && !orig.equals(taxonTitleCache);
        return result;
    }

    /**
     * @param authorStr
     * @return
     */
    private TeamOrPersonBase<?> getNameAdditionAuthor(String authorStr) {
        TeamOrPersonBase<?> result;
        String[] splits = authorStr.split("(, | & )");
        if (splits.length == 1){
            Person person = Person.NewInstance();
            person.setNomenclaturalTitle(splits[0]);
            result = person;
        }else{
            Team team = Team.NewInstance();
            for (String split: splits){
                Person person = Person.NewInstance();
                person.setNomenclaturalTitle(split);
                team.addTeamMember(person);
            }
            result = team;
        }
        //TODO deduplicate
        return result;
    }

    /**
     * @param state
     * @param nameAddition
     * @return
     */
    private Person parseNomenNudumAuthor(EdaphobaseImportState state, IZoologicalName name, String nameAddition) {
        nameAddition = nameAddition.replace("nomen nudum [", "").replace("tz, 195]", "tz, 1952]")
                .replace("]", "");
        String[] split = nameAddition.split(", ");
        Integer year = Integer.valueOf(split[1]);
        name.setPublicationYear(year);
        //TODO deduplicate
        Person author = Person.NewInstance();
        author.setNomenclaturalTitle(split[0].trim());
        return author;
    }

    /**
     * @param state
     * @param id
     * @param isValid
     * @return
     */
    private boolean checkValid(EdaphobaseImportState state, Integer id, boolean isValid) {
        if (isValid){
            return isValid;
        }else if (includedInTaxa.contains(id)){
            return true;
        }else{
            return isValid;
        }
    }

    /**
     * @param rankStr
     * @return
     */
    private String extractEnglish(String rankStr) {
        if (rankStr == null){
            return null;
        }
        String[] splits = rankStr.split(", ");
        if (splits.length != 3){
            String message = "Wrong rank format: "+  rankStr;
            logger.error(message);
            return null;
        }
        return splits[1].trim();
    }


    static Map<Integer,UUID> idMap = new HashMap<>();
    static{
        idMap.put(86594, UUID.fromString("715c2370-45a4-450c-99f7-e196758979ca"));  //Aporrectodea caliginosa
        idMap.put(86593, UUID.fromString("230f1a69-5dcd-4829-a01c-17490a2fdf34"));  //Aporrectodea
        idMap.put(86684, UUID.fromString("0982dc0e-1a79-45a0-8abc-8166625b94b8"));  //Achaeta
        idMap.put(104328, UUID.fromString("15f0b5f8-44e4-4ae1-8b40-f36f0a049b27")); //Chamaedrilus
        idMap.put(97537, UUID.fromString("899c62e3-a116-4c5b-b22a-c76e761cc32e"));  //Araeolaimoides caecus
    }

    /**
     * @param taxonBase
     * @param id
     */
    private void handleExampleIdentifiers(TaxonBase<?> taxonBase, Integer id) {
        if (idMap.get(id) != null){
            taxonBase.setUuid(idMap.get(id));
            logger.warn("Override UUID for specific taxa. ID="+ id +  "; uuid="+idMap.get(id) + "; name="+ taxonBase.getName().getTitleCache());
        }
    }

    /**
     * @param state
     * @param rank
     * @throws UndefinedTransformerMethodException
     */
    private void checkRankMarker(EdaphobaseImportState state, Rank rank) throws UndefinedTransformerMethodException {

        if (rank != null){
            Set<Marker> markers = rank.getMarkers();
            if ( markers.size() == 0){  //we assume that no markers exist, at least not for markers of unused ranks
                UUID edaphoRankMarkerTypeUuid = state.getTransformer().getMarkerTypeUuid("EdaphoRankMarker");
                MarkerType marker = getMarkerType(state, edaphoRankMarkerTypeUuid, "Edaphobase rank", "Rank used in Edaphobase", "EdaRk" );
                Representation rep = Representation.NewInstance("Rang, verwendet in Edaphobase", "Edaphobase Rang", "EdaRg", Language.GERMAN());
                marker.addRepresentation(rep);
                rank.addMarker(Marker.NewInstance(marker, true));
                getTermService().saveOrUpdate(rank);
            }
        }else{
            logger.info("Rank is null and marker can not be set");
        }
    }

    /**
     * @param state
     * @param isGroup
     * @param taxonBase
     */
    private void handleTaxonomicGroupMarker(EdaphobaseImportState state, TaxonBase<?> taxonBase, boolean isGroup) {
        if (! isGroup){
            return;
        }else{
            try {
                MarkerType markerType = getMarkerType(state, state.getTransformer().getMarkerTypeUuid("TaxGrossgruppe"), "Tax. Gruppe", "Taxonomische Grossgruppe", "TGG", null, Language.GERMAN());
                if (taxonBase.isInstanceOf(Synonym.class)){
                    logger.warn("Syonym is marked as 'taxonomische Grossgruppe'");
                }
                taxonBase.addMarker(Marker.NewInstance(markerType, true));
            } catch (UndefinedTransformerMethodException e) {
            }
        }
    }

    /**
     * @param state
     * @return
     */
    private Reference makeSecRef(EdaphobaseImportState state) {
        Reference ref = ReferenceFactory.newDatabase();
        ref.setTitle(state.getConfig().getEdaphobaseSecundumTitle());
        ref.setUuid(state.getConfig().getSecUuid());
        state.addRelatedObject(REFERENCE_NAMESPACE, ref.getUuid().toString(), ref);
        getReferenceService().save(ref);
        return ref;
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        Map<String, TeamOrPersonBase<?>> authorMap = new HashMap<>();
        Set<String> authorSet = new HashSet<>();
        Set<String> referenceIdSet = new HashSet<>();

        try {
            while (rs.next()){
                String authorStr = rs.getString("tax_author_name");
                if (authorStr != null){
                    authorStr = authorStr.replace(" et ", " & ");
                    authorSet.add(authorStr);
                }
                handleForeignKey(rs, referenceIdSet, "tax_document");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Authors
        Set<UUID> uuidSet = new HashSet<>();
        for (String authorStr : authorSet){
            UUID uuid = state.getAuthorUuid(authorStr);
            uuidSet.add(uuid);
        }
        List<TeamOrPersonBase<?>> authors = (List)getAgentService().find(uuidSet);
        Map<UUID, TeamOrPersonBase<?>> authorUuidMap = new HashMap<>();
        for (TeamOrPersonBase<?> author : authors){
            authorUuidMap.put(author.getUuid(), author);
        }

        for (String authorStr : authorSet){
            UUID uuid = state.getAuthorUuid(authorStr);
            TeamOrPersonBase<?> author = authorUuidMap.get(uuid);
            authorMap.put(authorStr, author);
        }
        result.put(AUTHOR_NAMESPACE, authorMap);

        //reference map
        String nameSpace = REFERENCE_NAMESPACE;
        Class<?> cdmClass = Reference.class;
        Set<String> idSet = referenceIdSet;
        Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
        result.put(nameSpace, referenceMap);

        //secundum
        UUID secUuid = state.getConfig().getSecUuid();
        Reference secRef = getReferenceService().find(secUuid);
        referenceMap.put(secUuid.toString(), secRef);

        return result;
    }

    private void setNamePart(String nameStr, Rank rank, IZoologicalName name) {
        if (rank != null){
            if (rank.isSupraGeneric() || rank.isGenus()){
                if (isBlank(name.getGenusOrUninomial())){
                    name.setGenusOrUninomial(nameStr);
                }
            }else if (rank.isInfraGenericButNotSpeciesGroup()){
                if (isBlank(name.getInfraGenericEpithet())){
                    name.setInfraGenericEpithet(nameStr);
                }
            }else if (rank.isSpeciesAggregate() || rank.isSpecies()){
                if (isBlank(name.getSpecificEpithet())){
                    name.setSpecificEpithet(nameStr);
                }
            }else if (rank.isInfraSpecific()){
                if (isBlank(name.getInfraSpecificEpithet())){
                    name.setInfraSpecificEpithet(nameStr);
                }
            }
        }
    }

    private Rank makeRank(EdaphobaseImportState state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
            if (rank == null && rankStr != null){
                if (rankStr.equals("Cohort")){
                    //position not really clear #7285
                    Rank lowerRank = Rank.SUPERORDER();
                    rank = this.getRank(state, Rank.uuidCohort, "Cohort", "Cohort", null,
                            (OrderedTermVocabulary<Rank>)Rank.GENUS().getVocabulary(),
                            lowerRank, RankClass.Suprageneric);
                }else if (rankStr.equals("Hyporder")){
                    rank = this.getRank(state, Rank.uuidHyporder, "Hyporder", "Hyporder", null,
                            (OrderedTermVocabulary<Rank>)Rank.GENUS().getVocabulary(),
                            Rank.SUBORDER(), RankClass.Suprageneric);
                }
            }
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
        }
        return rank;
    }

    protected void doNotes(AnnotatableEntity annotatableEntity, String notes, AnnotationType type) {
        if (StringUtils.isNotBlank(notes) && annotatableEntity != null ){
            String notesString = String.valueOf(notes);
            if (notesString.length() > 65530 ){
                notesString = notesString.substring(0, 65530) + "...";
                logger.warn("Notes string is longer than 65530 and was truncated: " + annotatableEntity);
            }
            Annotation notesAnnotation = Annotation.NewInstance(notesString, Language.UNDETERMINED());
            //notesAnnotation.setAnnotationType(AnnotationType.EDITORIAL());
            //notes.setCommentator(bmiConfig.getCommentator());
            annotatableEntity.addAnnotation(notesAnnotation);
        }
    }

    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoTaxa();
    }

}
