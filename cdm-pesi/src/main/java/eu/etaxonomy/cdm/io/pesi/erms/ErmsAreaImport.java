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
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsAreaImportValidator;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsAreaImport
        extends ErmsImportBase<NamedArea>
        implements IMappingImport<NamedArea, ErmsImportState>{

    private static final long serialVersionUID = 7151312300027994346L;

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "areas";
	private static final String dbTableName = "gu";
	private static final Class<?> cdmTargetClass = NamedArea.class;

	public ErmsAreaImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM gu " +
			" WHERE ( gu.id IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
    protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "id", AREA_NAMESPACE)); //id
			mapping.addMapper(DbImportStringMapper.NewInstance("gu_name", "titleCache"));
			ExtensionType extensionType = getExtensionType( ErmsTransformer.uuidExtGazetteer, "Gazetteer ID", "Gazetteer ID", "G-ID");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("gazetteer_id",extensionType));
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("note", AnnotationType.EDITORIAL()));

		}
		return mapping;
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;  //not needed
	}

	@Override
	public NamedArea createObject(ResultSet rs, ErmsImportState state) throws SQLException {
	    TermVocabulary<NamedArea> voc = getVocabulary(state, TermType.NamedArea, ErmsTransformer.uuidVocErmsAreas, "User defined vocabulary for named areas",
	            "User Defined Named Areas", null, null, true, NamedArea.NewInstance());

	    int id = rs.getInt("id");
		String strGuName = rs.getString("gu_name");
		UUID uuid = ErmsTransformer.uuidFromGuName(strGuName);
		String label = strGuName;
		String text = strGuName;
		String labelAbbrev = null;
		NamedAreaType areaType = null;
		NamedAreaLevel level = null;

		NamedArea area = getNamedArea(state, uuid, label, text, labelAbbrev, areaType, level, voc, null);
		area.setIdInVocabulary(String.valueOf(id));
		return area;
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsAreaImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return !state.getConfig().isDoDistributions();
	}
}
