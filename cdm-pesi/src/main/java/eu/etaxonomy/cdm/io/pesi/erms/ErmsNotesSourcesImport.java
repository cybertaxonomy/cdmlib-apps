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
import eu.etaxonomy.cdm.io.common.mapping.DbImportDescriptionElementSourceCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsNoteSourceImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 12.03.2010
 */
@Component
public class ErmsNotesSourcesImport extends ErmsImportBase<CommonTaxonName> {
    private static final long serialVersionUID = -5197101648269924453L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsNotesSourcesImport.class);


//************************** VARIABLES ********************************************

	private static String pluralString = "note sources";
	private static String dbTableName = "notes_sources";
	private static final Class<?> cdmTargetClass = DescriptionElementSource.class;

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

//******************************************* CONSTRUCTOR *******************************

	public ErmsNotesSourcesImport() {
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strQuery =
			" SELECT * " +
			" FROM notes_sources " +
			" WHERE note_id IN (" + ID_LIST_TOKEN + ") AND " +
					" source_id IN (" + ID_LIST_TOKEN + ")";
		return strQuery;
	}

	@Override
	protected String getIdQuery() {
		String strQuery =
			" SELECT note_id, source_id " +
			" FROM notes_sources "
			;
		return strQuery;
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			String noteNamespace = ErmsImportBase.NOTES_NAMESPACE;
			String referenceNamespace = ErmsImportBase.REFERENCE_NAMESPACE;
			mapping.addMapper(DbImportDescriptionElementSourceCreationMapper.NewInstance("note_id", noteNamespace, "source_id", referenceNamespace ));
		}
		return mapping;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> noteIdSet = new HashSet<>();
			Set<String> sourceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, noteIdSet, "note_id");
				handleForeignKey(rs, sourceIdSet, "source_id");
			}

			//note map
			nameSpace = ErmsImportBase.NOTES_NAMESPACE;
			idSet = noteIdSet;
			Map<String, TextData> noteMap = getCommonService().getSourcedObjectsByIdInSourceC(TextData.class, idSet, nameSpace);
			result.put(nameSpace, noteMap);

			//reference map
			nameSpace = ErmsImportBase.REFERENCE_NAMESPACE;
			idSet = sourceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(ErmsImportState state) {
		IOValidator<ErmsImportState> validator = new ErmsNoteSourceImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state) {
		boolean isDo = state.getConfig().isDoNotes();
		return ! isDo ;
	}
}
