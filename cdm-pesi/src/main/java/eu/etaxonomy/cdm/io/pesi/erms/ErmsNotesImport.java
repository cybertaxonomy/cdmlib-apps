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
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsNoteImportValidator;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.02.2010
 */
@Component
public class ErmsNotesImport  extends ErmsImportBase<Annotation> {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsNotesImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;
	
	private static final String pluralString = "notes";
	private static final String dbTableName = "notes";
	private static final Class<?> cdmTargetClass = TextData.class;

	public ErmsNotesImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery = 
			" SELECT * " + 
			" FROM notes " +
			" WHERE ( notes.id IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.erms.ErmsImportBase#getMapping()
	 */
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<ErmsImportState, ErmsImportConfigurator>();
			mapping.addMapper(DbImportTextDataCreationMapper.NewInstance("id", NOTES_NAMESPACE, "tu_id", TAXON_NAMESPACE));
			mapping.addMapper(DbImportMultiLanguageTextMapper.NewInstance("note", "lan_id", LANGUAGE_NAMESPACE, "Text"));
			Language notesNoteLanguage = null;
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("note", AnnotationType.EDITORIAL(), notesNoteLanguage));
			mapping.addMapper(DbImportFeatureCreationMapper.NewInstance("type", FEATURE_NAMESPACE, "type", "type", "type"));			
		}
		return mapping;
	}
	

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> taxonIdSet = new HashSet<String>();
			Set<String> languageIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
				handleForeignKey(rs, languageIdSet, "lan_id");
			}
			
			//taxon map
			nameSpace = ErmsTaxonImport.TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase<?>> taxonMap = (Map<String, TaxonBase<?>>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);
			
			//language map
			nameSpace = LANGUAGE_NAMESPACE;
			Map<String, Language> languageMap = new HashMap<String, Language>();
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