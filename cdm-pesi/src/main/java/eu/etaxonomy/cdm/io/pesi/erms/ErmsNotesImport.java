/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportFeatureCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMultiLanguageTextMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTextDataCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsNoteImportValidator;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsNotesImport  extends ErmsImportBase<Annotation> {

    private static final long serialVersionUID = 3597110192009910328L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsNotesImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "notes";
	private static final String dbTableName = "notes";
	private static final Class<?> cdmTargetClass = TextData.class;

	public ErmsNotesImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT n.*, " +
	              " s.sessiondate lastActionDate, a.action_name lastAction, s.ExpertName " +
			" FROM notes n " +
            "     LEFT OUTER JOIN notes_sessions MN ON MN.note_id = n.id " +
            "     LEFT OUTER JOIN [sessions] s ON s.id = MN.session_id " +
            "     LEFT OUTER JOIN actions a ON a.id = MN.action_id " +
			" WHERE ( n.id IN (" + ID_LIST_TOKEN + ") )" +
			" ORDER BY n.id, s.sessiondate DESC, a.id DESC ";
		return strRecordQuery;
	}

    Integer lastNoteId = null;
    @Override
    protected boolean ignoreRecord(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        boolean result = id.equals(lastNoteId);
        lastNoteId = id;
        return result;
    }

	@Override
    protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			mapping.addMapper(DbImportTextDataCreationMapper.NewInstance("id", NOTES_NAMESPACE, "tu_id", TAXON_NAMESPACE));
			mapping.addMapper(DbImportFeatureCreationMapper.NewInstance("type", FEATURE_NAMESPACE, "type", "type", "type"));
			mapping.addMapper(DbImportMultiLanguageTextMapper.NewInstance("note", "lan_id", LANGUAGE_NAMESPACE, "Text", true));
			Language notesNoteLanguage = null;
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("note", AnnotationType.EDITORIAL(), notesNoteLanguage));
            //last action
			AnnotationType speciesExpertNameAnnType = getAnnotationType(ErmsTransformer.uuidAnnSpeciesExpertName, "species expert name", "species expert name", "species expert name");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("ExpertName", speciesExpertNameAnnType)); //according to sql script ExpertName maps to SpeciesExpertName in ERMS
            AnnotationType lastActionDateType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastActionDate, "Last action date", "Last action date", null);
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastActionDate", lastActionDateType));
            AnnotationType lastActionType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastAction, "Last action", "Last action", null);
            MarkerType hasNoLastActionMarkerType = getMarkerType(DbLastActionMapper.uuidMarkerTypeHasNoLastAction, "has no last action", "No last action information available", "no last action");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastAction", lastActionType, hasNoLastActionMarkerType));
		}
		return mapping;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> languageIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
				handleForeignKey(rs, languageIdSet, "lan_id");
			}

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//language map
			nameSpace = LANGUAGE_NAMESPACE;
			Map<String, Language> languageMap = new HashMap<>();
			ErmsTransformer transformer = new ErmsTransformer();
			for (String lanAbbrev: languageIdSet){
				Language language = transformer.getLanguageByKey(lanAbbrev);
				languageMap.put(lanAbbrev, language);
			}
			result.put(nameSpace, languageMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsNoteImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoNotes();
	}
}
