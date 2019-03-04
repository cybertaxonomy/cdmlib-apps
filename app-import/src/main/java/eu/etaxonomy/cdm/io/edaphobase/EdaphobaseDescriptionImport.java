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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 18.12.2015
 *
 */
@Component
public class EdaphobaseDescriptionImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = -9138378836474086070L;
    private static final Logger logger = Logger.getLogger(EdaphobaseDescriptionImport.class);

    private static final String tableName = "description_detail";

    private static final String pluralString = "descriptions";


    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseDescriptionImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {

        return " SELECT dd.description_detail_id " +
            " FROM observation.observation o " +
            " JOIN observation.observation2description o2d ON o.observation_id = o2d.observation_fk " +
            " JOIN observation.description_detail dd ON o2d.description_fk = dd.description_fk " +
            " JOIN observation.column c ON dd.column_fk = c.column_id " +
            " LEFT JOIN observation.unit u ON dd.unit_fk = u.unit_id " +
            " WHERE o.first_description = true and o.taxon_fk is not null " +
            " ORDER BY dd.description_detail_id, o.taxon_fk";
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
        String result = " SELECT DISTINCT dd.description_detail_id, o.taxon_fk, c.name_de, dd.min_value, "
                + " dd.max_value, dd.value, u.name, dd.text, dd.list_element_fk " +
                " FROM observation.observation o " +
                    " JOIN observation.observation2description o2d ON o.observation_id = o2d.observation_fk " +
                    " JOIN observation.description_detail dd ON o2d.description_fk = dd.description_fk " +
                    " JOIN observation.column c ON dd.column_fk = c.column_id " +
                    " LEFT JOIN observation.unit u ON dd.unit_fk = u.unit_id " +
                " WHERE o.first_description = true and o.taxon_fk is not null " +
                  " AND dd.description_detail_id IN (@IDSET)";

        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(EdaphobaseImportState state) {
        doFeatures(state);
        super.doInvoke(state);
    }


    /**
     *
     */
    private void doFeatures(EdaphobaseImportState state) {
        String sql = " SELECT  c.*, l.list_id, l.is_hierarchical listIsHierarchical, l.name listName, l.attribute_order " +
                " FROM observation.column c LEFT OUTER JOIN selective_list.list l ON c.list_fk = l.list_id " +
                " WHERE c.column_id IN " +
                   " (SELECT dd.column_fk " +
                       " FROM observation.observation o " +
                       " JOIN observation.observation2description o2d ON o.observation_id = o2d.observation_fk " +
                       " JOIN observation.description_detail dd ON o2d.description_fk = dd.description_fk " +
                       " WHERE o.first_description = true and o.taxon_fk is not null"
                   + ")";
        ResultSet rs = state.getConfig().getSource().getResultSet(sql);
        try {
            @SuppressWarnings("unchecked")
            TermVocabulary<Feature> vocQuant = TermVocabulary.NewInstance(TermType.Feature, "Edaphobase quantitative features", "Quantitative features", "quant.", null);
            vocQuant.setUuid(EdaphobaseImportTransformer.uuidVocFeatureQuantitative);
            @SuppressWarnings("unchecked")
            TermVocabulary<Feature> vocBiology = TermVocabulary.NewInstance(TermType.Feature, "Edaphobase biological features", "Biological features", "biol.", null);
            vocBiology.setUuid(EdaphobaseImportTransformer.uuidVocFeatureBiological);
            @SuppressWarnings("unchecked")
            TermVocabulary<Feature> vocMorphology = TermVocabulary.NewInstance(TermType.Feature, "Edaphobase morphological features", "Morphological features", "morph.", null);
            vocBiology.setUuid(EdaphobaseImportTransformer.uuidVocFeatureMorpho);

            while (rs.next()){
               handleSingleFeature(state, rs, vocQuant, vocBiology, vocMorphology);
            }
            getVocabularyService().save(vocQuant);
            getVocabularyService().save(vocBiology);
            getVocabularyService().save(vocMorphology);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param state
     * @param rs
     * @param vocMorphology
     * @param vocBiology
     * @param vocQuant
     * @throws SQLException
     */
    private void handleSingleFeature(EdaphobaseImportState state, ResultSet rs,
            TermVocabulary<Feature> vocQuant, TermVocabulary<Feature> vocBiology,
            TermVocabulary<Feature> vocMorphology) throws SQLException {
        int id = rs.getInt("column_id");
        int minVal = rs.getInt("min_value");
        int maxVal = rs.getInt("max_value");  //null
        String nameDe = rs.getString("name_de");
        String nameEn = rs.getString("name_en");
        //required
        Integer unit_fk = nullSafeInt(rs, "unit_fk"); //18=mm; 47=mg
        int dataType = rs.getInt("data_type_fk");  //11475=Integer, 11476=Floating point, 11477=String, 11478=Categorical data
        Integer list = nullSafeInt(rs, "list_fk"); //allows null,  distinct values except for null
        int columnGroup = rs.getInt("column_group"); //11661=Quantität, 11663=Biology, 11664=Morphology
        String description = rs.getString("description");
        String description_en = rs.getString("description_en");
        //versionfield
        checkNullStr(state, rs, id, "regEx", "tableName", "scheme_name","query","pg_preview_query");
        //anonym (f, but once t)
        //only once (f, sometimes t)
        //inner_agg
        //inner_summary
        //hierarchical_level_fk //empty int
        //usings (json), some values

        String listName = rs.getString("listName");

        Feature feature = Feature.NewInstance(description_en, nameEn, null);
        feature.addRepresentation(Representation.NewInstance(description, nameDe, null, Language.GERMAN()));

        //columnGroup => vocabulary
        if (columnGroup == 11661){
           vocQuant.addTerm(feature);
        }else if (columnGroup == 11663){
            vocBiology.addTerm(feature);
         }else if (columnGroup == 11664){
             vocMorphology.addTerm(feature);
         }else {
             logger.error("Unhandled column group "+ columnGroup);
         }

        //dataType
        feature.setSupportsTextData(false);
        if (dataType == 11475 || dataType == 11476){
            feature.setSupportsQuantitativeData(true);
        }else if (dataType == 11477){
            feature.setSupportsTextData(true);
        }else if (dataType == 11478){
            feature.setSupportsCategoricalData(true);
        }else{
            logger.error("Unhandled dataType " +  dataType);
        }

        //list
        if (list != null){
            //TODO term type
            TermVocabulary<State> categoryVoc = makeCategoricalVocabulary(state, list, listName);
            feature.addSupportedCategoricalEnumeration(categoryVoc);
        }

        //unit_fk
        //TODO

    }


    /**
     * @param state
     * @param list
     * @param listName
     * @return
     * @throws SQLException
     */
    private TermVocabulary<State> makeCategoricalVocabulary(EdaphobaseImportState state, Integer list, String listName) throws SQLException {
        TermVocabulary<State> result = TermVocabulary.NewInstance(TermType.State, listName, listName, null, null );
        String sql = " SELECT * "
                + " FROM selective_list.element "
                + " WHERE list_fk = " + list
                + " ORDER BY path ";
        ResultSet rs = state.getConfig().getSource().getResultSet(sql);
        Map<Integer, State> map = new HashMap<>();

        while (rs.next()){
            Integer id = rs.getInt("element_id");
            Integer parentFk = nullSafeInt(rs, "element_id");
            String value = rs.getString("value_summary");

            State term = State.NewInstance();
            handleValueSummary(state, term, value, list, id);
            if (parentFk != null && map.get(parentFk) != null){
                State parent = map.get(parentFk);
                term.setKindOf(term);
            }
            result.addTerm(term);
        }
        return result;
    }

    /**
     * @param state
     * @param term
     * @param value
     * @param list
     */
    private void handleValueSummary(EdaphobaseImportState state, State term, String valueOrig, Integer list, Integer id) {
        String sep = ", ";
        String value = valueOrig;
        String[] splits = value.split(sep);
        String german;
        String english;
        String idInVoc = null;
        //idInVoc
        if (splits[0].length() <=2){
            idInVoc = splits[0];
            String[] newSplit = new String[splits.length - 1];
            for (int i = 0; i < newSplit.length; i++){
                newSplit[i] = splits[i+1];
            }
            splits = newSplit;
            value = value.substring(idInVoc.length() + 1);
        }else if (list == 27){
            Matcher matcher = Pattern.compile("(.+)(\\(d[a-h]?\\))").matcher(splits[1]);
            if (matcher.matches()){
                splits[1] = matcher.group(1).trim();
                idInVoc = matcher.group(2).replace("(", "").replace(")", "").trim();
            }
        }
        if(value.contains(", indicator ")){
            splits = value.split(", indicator ");
            splits[1] = "indicator " + splits[1];
        }
        //
        if (splits.length == 2){
            german = splits[0];
            english = splits[1];
        }else if (splits.length == 4){
            german = CdmUtils.concat(sep, splits[0], splits[1]);
            english = CdmUtils.concat(sep, splits[2], splits[3]);
        }else if (splits.length == 6){
            german = CdmUtils.concat(sep, splits[0], splits[1], splits[2]);
            english = CdmUtils.concat(sep, splits[3], splits[4], splits[5]);
        }else{
            logger.warn("Pattern for id = " + id + " could not be recognized: " + valueOrig);
        }
        //TODO 84, 95  (mit Einheit)

        term.addRepresentation(Representation.NewInstance(german, german, null, Language.GERMAN()));
        term.addRepresentation(Representation.NewInstance(english, english, null, Language.ENGLISH()));
        term.setIdInVocabulary(idInVoc);
    }

    /**
     * Checks if the value for these attributes is blank
     * @param state
     * @param rs
     * @param strings
     * @throws SQLException
     */
    private void checkNullStr(EdaphobaseImportState state, ResultSet rs, int id, String ... cols) throws SQLException {
        for (String col:cols){
            String val = rs.getString(col);
            if (StringUtils.isNotBlank(val)){
                logger.warn("Column " + col + " is not empty as expected for observation.column.column_id = " + id);
            }
        }
    }

    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
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
        Integer id = nullSafeInt(rs, "description_detail_id");
        Integer taxonFk = nullSafeInt(rs, "taxon_fk");
        TaxonBase<?> taxonBase = state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(taxonFk), TaxonBase.class);


//        //id
//        ImportHelper.setOriginalSource(taxonBase, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
//        ImportHelper.setOriginalSource(name, state.getTransactionalSourceReference(), id, TAXON_NAMESPACE);
//        handleExampleIdentifiers(taxonBase, id);
    }


    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {

        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        Set<String> taxonIdSet = new HashSet<>();

        try {
            while (rs.next()){
                handleForeignKey(rs, taxonIdSet, "taxon_fk");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //reference map
        String nameSpace = TAXON_NAMESPACE;
        Class<?> cdmClass = TaxonBase.class;
        Set<String> idSet = taxonIdSet;
        Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
        result.put(nameSpace, taxonMap);

        return result;
    }


    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoDescriptions();
    }

}
