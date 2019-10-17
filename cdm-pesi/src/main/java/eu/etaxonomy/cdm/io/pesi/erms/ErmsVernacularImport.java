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
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportCommonNameCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsVernacularImportValidator;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsVernacularImport  extends ErmsImportBase<CommonTaxonName> {

    private static final long serialVersionUID = -5928250782824234181L;

	private static final Logger logger = Logger.getLogger(ErmsVernacularImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;
//	private ErmsImportState state;   //import instance is never used more than once for Erms ; dirty

	private static final String pluralString = "vernaculars";
	private static final String dbTableName = "vernaculars";
	private static final Class<?> cdmTargetClass = CommonTaxonName.class;

	public ErmsVernacularImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT v.*, tu.tu_acctaxon, tu.id, l.*, " +
			       " s.sessiondate lastActionDate, a.action_name lastAction, s.ExpertName " +
			" FROM vernaculars v INNER JOIN tu ON v.tu_id = tu.id " +
			"     LEFT OUTER JOIN languages l ON l.LanID = v.lan_id " +
            "     LEFT OUTER JOIN vernaculars_sessions MN ON MN.vernacular_id = v.id " +
            "     LEFT OUTER JOIN [sessions] s ON s.id = MN.session_id " +
            "     LEFT OUTER JOIN actions a ON a.id = MN.action_id " +
			" WHERE ( v.id IN (" + ID_LIST_TOKEN + ") )" +
			" ORDER BY v.id, s.sessiondate DESC, a.id DESC ";
		return strRecordQuery;
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

			mapping.addMapper(DbImportCommonNameCreationMapper.NewInstance("id", VERNACULAR_NAMESPACE, "tu_id", ErmsImportBase.TAXON_NAMESPACE));

			mapping.addMapper(DbImportObjectMapper.NewInstance("lan_id", "language", LANGUAGE_NAMESPACE));
			mapping.addMapper(DbImportStringMapper.NewInstance("vername", "name"));
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("note", AnnotationType.EDITORIAL(), Language.DEFAULT()));
			//last action+expert
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

    Integer lastVernacularId = null;
    @Override
    protected boolean ignoreRecord(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        boolean result = id.equals(lastVernacularId);
        lastVernacularId = id;
        return result;
    }

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		Map<String, Language> languageMap = new HashMap<>();
		try{
			Set<String> taxonIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
				addLanguage(rs, languageMap, state);
			}

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//language map
			nameSpace = LANGUAGE_NAMESPACE;
			result.put(nameSpace, languageMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

    private void addLanguage(ResultSet rs, Map<String, Language> languageMap, ErmsImportState state) throws SQLException {
        IInputTransformer transformer = state.getTransformer();
        String id639_1 = rs.getString("639_1");
        String id639_2 = rs.getString("639_2");
        String lanId = rs.getString("LanID");

        if (isNotBlank(id639_1) && Language.getLanguageByIsoCode(id639_1)!= null){
            languageMap.put(lanId, Language.getLanguageByIsoCode(id639_1));
        }else if (isNotBlank(id639_2) && Language.getLanguageByIsoCode(id639_2)!= null){
            languageMap.put(lanId, Language.getLanguageByIsoCode(id639_2));
        }else{
            try {
                Language language = transformer.getLanguageByKey(lanId);
                persistLanguage(language);
                if (language == null || language.equals(Language.UNDETERMINED())){
                    UUID uuidLang = transformer.getLanguageUuid(lanId);
                    if (uuidLang != null){
                        language = getLanguage(state, uuidLang, rs.getString("LanName"), "LanName", "639_3");
                    }
                    if (language == null || language.equals(Language.UNDETERMINED() )){
                        logger.warn("Language undefined: " + lanId);
                    }
                }if (language != null){
                    languageMap.put(lanId, language);
                }
            } catch (IllegalArgumentException | UndefinedTransformerMethodException e) {
                e.printStackTrace();
                logger.error("Error when retrieving language", e);
            }
        }
    }

    private void persistLanguage(Language language) {
        if(!language.isPersited()){
            getTermService().saveOrUpdate(language);
        }
        if (!language.getVocabulary().isPersited()){
            getVocabularyService().saveOrUpdate(language.getVocabulary());
        }
    }

    @Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsVernacularImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoVernaculars();
	}
}
