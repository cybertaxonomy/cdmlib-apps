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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.DOI;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 18.12.2015
 *
 */
@Component
public class EdaphobaseReferenceImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = 6895687693249076160L;

    private static final Logger logger = Logger.getLogger(EdaphobaseReferenceImport.class);

    private static final String tableName = "lit_document";

    private static final String pluralString = "documents";


    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseReferenceImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
        return    " SELECT DISTINCT document_id "
                + " FROM lit_document ld INNER JOIN tax_taxon t ON t.tax_document = ld.document_id "
                + " UNION "
                + " SELECT DISTINCT pd.document_id "
                + " FROM lit_document ld INNER JOIN tax_taxon t ON t.tax_document = ld.document_id "
                + " INNER JOIN lit_document pd ON pd.document_id = ld.parent_document_fk_document_id "
                + " ORDER BY document_id ";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = " SELECT * "
                + " FROM lit_document ld "
                + " WHERE ld.document_id IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Set<Reference> referencesToSave = new HashSet<>();
        try {
            while (rs.next()){
                handleSingleReference(state, rs, referencesToSave);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getReferenceService().saveOrUpdate(referencesToSave);

        return true;
    }

    /**
     * @param state
     * @param rs
     * @param referencesToSave
     * @throws SQLException
     */
    private void handleSingleReference(EdaphobaseImportState state, ResultSet rs, Set<Reference> referencesToSave) throws SQLException {
        Integer id = nullSafeInt(rs, "document_id");
        String dtype = rs.getString("dtype");
        String issue = rs.getString("issue");
        String orderer = rs.getString("orderer");
        String place = rs.getString("place");
        Integer pageFrom = nullSafeInt(rs, "page_from");
        Integer pageTo = nullSafeInt(rs, "page_to");
        String subtitle = rs.getString("subtitle");
        Integer year = nullSafeInt(rs, "year");
        String isbn = rs.getString("isbn");
        //refers_to_literature
        //refers_to_collection
        //refers_to_observation
        String remark = rs.getString("remark");
        String volume = rs.getString("volume");
        //abbreviation (no record)
        String title = rs.getString("title");
        String issn = rs.getString("issn");
        //circulation //2 records
        String keywords = rs.getString("keywords");
        String abstractt = rs.getString("abstract");
        String parallel_title = rs.getString("parallel_title");
        //language_fk_language_id
        //document_type_fk_document_type_id
        //editor_fk_person_id
        Integer editorFk = nullSafeInt(rs, "editor_fk_person_id");

//        Integer parentFk = nullSafeInt(rs, "parent_document_fk_document_id");
        //publisher_fk_publisher_id
        //deleted
        //chapter_no
        //versionfield
        String doi = rs.getString("doi");
        String displayString = rs.getString("display_string");
        //aquisistion_date, aquisition_type, adoption_date, ex_colletion, barcode_prefix, barcode_org_prefix
        //barcode_type, collection_status, barcode, typus_form,

        //taxon_for_scope, taxon_is_scope
        //language_fk, document_type_backup

        Integer documentType = nullSafeInt(rs, "document_type");
        //normalized_title, normalized_abk_official_remark

        Reference ref = makeReferenceType(documentType, dtype);
        ref.setTitle(title);
        ref.setPlacePublished(place);
        ref.setIssn(issn);
        ref.setIsbn(isbn);
        if (pageFrom != null || pageTo != null){
            String pageStr = pageFrom == null ? "" : String.valueOf(pageFrom);
            pageStr = pageTo == null ? pageStr : "-" + pageTo;
            ref.setPages(pageStr);
        }
        if (year != null){
            ref.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(year));
        }
        ref.setVolume(volume);
        ref.setReferenceAbstract(abstractt);
        if (StringUtils.isNotBlank(doi)){
            try {
                String doiStr = doi;
                if (doiStr.startsWith("dx.doi.org/")){
                    doiStr = doiStr.substring(11);
                }
                ref.setDoi(DOI.fromString(doiStr));
            } catch (IllegalArgumentException e) {
                logger.warn("DOI could not be parsed: " + doi);
            }
        }
        ref.setEdition(issue);

        //id
        ImportHelper.setOriginalSource(ref, state.getTransactionalSourceReference(), id, REFERENCE_NAMESPACE);

        referencesToSave.add(ref);
    }


    /**
     * @param documentType
     * @return
     */
    private Reference makeReferenceType(Integer documentType, String dtype) {
        if (documentType == 11914){
            return ReferenceFactory.newArticle();
        } else if (documentType == 11916){
            return ReferenceFactory.newBook();
        } else if (documentType == 11915){
            return ReferenceFactory.newPrintSeries();
        } else if (documentType == 11913){
            return ReferenceFactory.newJournal();
        } else if (documentType == 11917){
            return ReferenceFactory.newBookSection();
        } else if (documentType == 11912 || documentType == 11919 || documentType == 11924 ){
            Reference ref = ReferenceFactory.newGeneric();
            return ref;
        } else {
            throw new RuntimeException("DocumentType not yet supported: " + documentType + ", " + dtype);
        }
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
//        Map<String, TeamOrPersonBase<?>> authorMap = new HashMap<>();
//        Set<String> authorSet = new HashSet<>();
//        try {
//            while (rs.next()){
//                String authorStr = rs.getString("tax_author_name");
//                authorSet.add(authorStr);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        //Authors
//        Set<UUID> uuidSet = new HashSet<>();
//        for (String authorStr : authorSet){
//            UUID uuid = state.getAuthorUuid(authorStr);
//            uuidSet.add(uuid);
//        }
//        List<TeamOrPersonBase<?>> authors = (List)getAgentService().find(uuidSet);
//        Map<UUID, TeamOrPersonBase<?>> authorUuidMap = new HashMap<>();
//        for (TeamOrPersonBase<?> author : authors){
//            authorUuidMap.put(author.getUuid(), author);
//        }
//
//        for (String authorStr : authorSet){
//            UUID uuid = state.getAuthorUuid(authorStr);
//            TeamOrPersonBase<?> author = authorUuidMap.get(uuid);
//            authorMap.put(authorStr, author);
//        }
//        result.put(AUTHOR_NAMESPACE, authorMap);

        return result;
    }



    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoReferences();
    }

}
