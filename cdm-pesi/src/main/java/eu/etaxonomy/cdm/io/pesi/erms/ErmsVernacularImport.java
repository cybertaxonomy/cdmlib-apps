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
			" SELECT v.*, tu.tu_acctaxon, tu.id, l.*, gr.date lastActionDate, a.action_name lastAction  " +
			" FROM vernaculars v INNER JOIN tu ON v.tu_id = tu.id "
			+ "   LEFT OUTER JOIN languages l ON l.LanID = v.lan_id "
			+ " LEFT JOIN ( " +
			        " SELECT maxDate.vernacular_id, maxDate.date, max(action_id) action_id " +
			        " FROM (SELECT vernacular_id, max(s.sessiondate) date FROM  vernaculars_sessions MN INNER JOIN sessions s ON s.id = MN.session_id GROUP BY vernacular_id) maxDate " +
			            " INNER JOIN (SELECT MN2.vernacular_id, MN2.action_id, s2.sessiondate FROM vernaculars_sessions MN2  INNER JOIN sessions s2 ON s2.id = MN2.session_id) as a ON a.vernacular_id = maxDate.vernacular_id AND a.sessiondate = maxDate.date " +
			            " GROUP BY maxDate.vernacular_id,  maxDate.date) as gr ON v.id = gr.vernacular_id " +
			            " LEFT JOIN actions a ON a.id = gr.action_id " +
			" WHERE ( v.id IN (" + ID_LIST_TOKEN + ") )";
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
			//last action
			AnnotationType lastActionDateType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastActionDate, "Last action date", "Last action date", null);
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastActionDate", lastActionDateType));
            AnnotationType lastActionType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastAction, "Last action", "Last action", null);
            MarkerType hasNoLastActionMarkerType = getMarkerType(DbLastActionMapper.uuidMarkerTypeHasNoLastAction, "has no last action", "No last action information available", "no last action");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastAction", lastActionType, hasNoLastActionMarkerType));
		}
		return mapping;
	}

	@Override
	protected void doInvoke(ErmsImportState state) {
		super.doInvoke(state);
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		Map<String, Language> languageMap = new HashMap<>();
		try{
			Set<String> taxonIdSet = new HashSet<>();
//			Set<String> languageIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
				addLanguage(rs, languageMap, state);
			}

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			@SuppressWarnings("unchecked")
            Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
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
        String id639_3 = rs.getString("639_3");
        String lanId = rs.getString("LanID");

        if (id639_1 != null && Language.getLanguageByIsoCode(id639_1)!= null){
            languageMap.put(lanId, Language.getLanguageByIsoCode(id639_1));
        }else if (id639_2 != null && Language.getLanguageByIsoCode(id639_2)!= null){
            languageMap.put(lanId, Language.getLanguageByIsoCode(id639_2));
        }else{
            Language language = null;
            try {
                language = transformer.getLanguageByKey(lanId);
                persistLanguage(language);
                if (language == null || language.equals(Language.UNDETERMINED())){
                    UUID uuidLang = transformer.getLanguageUuid(lanId);
                    if (uuidLang != null){
                        language = getLanguage(state, uuidLang, rs.getString("LanName"), "LanName", "639_3");
                    }
                    if (language == null || language.equals(Language.UNDETERMINED() )){
                        logger.warn("Language undefined: " + lanId);
                    }
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
