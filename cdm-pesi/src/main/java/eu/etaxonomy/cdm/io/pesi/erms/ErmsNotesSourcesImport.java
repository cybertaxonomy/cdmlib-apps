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

	/**
	 * @param dbTableName
	 * @param pluralString
	 * @param dbTableName
	 */
	public ErmsNotesSourcesImport() {
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strQuery =
			" SELECT * " +
			" FROM vernaculars_sources " +
			" WHERE vernacular_id IN (" + ID_LIST_TOKEN + ") AND " +
					" source_id IN (" + ID_LIST_TOKEN + ")";
		return strQuery;
	}

	@Override
	protected String getIdQuery() {
		String strQuery =
			" SELECT vernacular_id, source_id " +
			" FROM vernaculars_sources "
			;
		return strQuery;
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			String vernacularNamespace = ErmsImportBase.VERNACULAR_NAMESPACE;
			String referenceNamespace = ErmsImportBase.REFERENCE_NAMESPACE;
			mapping.addMapper(DbImportDescriptionElementSourceCreationMapper.NewInstance("vernacular_id", vernacularNamespace, "source_id", referenceNamespace ));
		}
		return mapping;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> vernacularIdSet = new HashSet<String>();
			Set<String> sourceIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, vernacularIdSet, "vernacular_id");
				handleForeignKey(rs, sourceIdSet, "source_id");
			}

			//vernacular map
			nameSpace = ErmsImportBase.VERNACULAR_NAMESPACE;
			cdmClass = CommonTaxonName.class;
			idSet = vernacularIdSet;
			Map<String, CommonTaxonName> vernacularMap = (Map<String, CommonTaxonName>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, vernacularMap);


			//reference map
			nameSpace = ErmsImportBase.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = sourceIdSet;
			@SuppressWarnings("unchecked")
            Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
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
		boolean isDo = state.getConfig().isDoVernaculars() && state.getConfig().isDoVernaculars();
		return ! isDo ;
	}
}
