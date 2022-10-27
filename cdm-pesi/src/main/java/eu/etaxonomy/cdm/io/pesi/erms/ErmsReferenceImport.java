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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportDoiMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTimePeriodMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTruncatedStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsReferenceImportValidator;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsReferenceImport
            extends ErmsImportBase<Reference>
            implements IMappingImport<Reference, ErmsImportState>{

    private static final long serialVersionUID = -2345972558542643378L;
    private static Logger logger = LogManager.getLogger();

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "sources";
	private static final String dbTableName = "sources";
	private static final Class<?> cdmTargetClass = Reference.class;

	public ErmsReferenceImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM sources " +
			" WHERE ( sources.id IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
    protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "id", REFERENCE_NAMESPACE)); //id
			ExtensionType imisExtType = getExtensionType( ErmsTransformer.uuidExtImis, "imis", "imis", "imis");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("imis_id", imisExtType));

			ExtensionType truncatedExtType = getExtensionType( ExtensionType.uuidExtNonTruncatedCache, "non-truncated cache", "non-truncated cache", "non-truncated cache");
            mapping.addMapper(DbImportTruncatedStringMapper.NewInstance("source_name", "titleCache", truncatedExtType, 800, true));
            mapping.addMapper(DbImportStringMapper.NewInstance("source_abstract", "referenceAbstract"));
            mapping.addMapper(DbImportStringMapper.NewInstance("source_title", "title"));
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("source_note", AnnotationType.EDITORIAL(), Language.DEFAULT()));
			mapping.addMapper(DbImportTimePeriodMapper.NewVerbatimInstance("source_year", "datePublished"));
			mapping.addMapper(DbImportDoiMapper.NewInstance("source_doi", "doi"));

			//TODO handle as External Link once they are available for Reference
			logger.warn("Handle source_link as ExternalLink once available for class Reference");
			mapping.addMapper(DbImportExtensionMapper.NewInstance("source_link", ExtensionType.URL()));
			//TODO parse  authors
			ExtensionType extTypeAuthor = getExtensionType(ErmsTransformer.uuidExtAuthor, "Reference author", "Reference author", null);
			mapping.addMapper(DbImportExtensionMapper.NewInstance("source_author", extTypeAuthor));

			//not yet implemented

			mapping.addMapper(DbIgnoreMapper.NewInstance("source_type", "Handled by ObjectCreateMapper"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("source_orig_fn", "Currently not needed. Holds information about pdf files."));
			mapping.addMapper(DbIgnoreMapper.NewInstance("source_openaccess", "Currently not needed. Holds information about open access of the source."));

		}
		return mapping;
	}

	@Override
    public Reference createObject(ResultSet rs, ErmsImportState state) throws SQLException {
		String type = rs.getString("source_type");
		Reference ref;
		if (type.equalsIgnoreCase("p")){
			ref = ReferenceFactory.newGeneric();
			MarkerType markerType = getMarkerType(state, ErmsTransformer.uuidMarkerRefPublication, "Publication", "Publication", "p");
			ref.addMarker(markerType, true);
		}else if (type.equalsIgnoreCase("d")){
			ref = ReferenceFactory.newDatabase();
		}else if (type.equalsIgnoreCase("e")){
			ref = ReferenceFactory.newGeneric();
	        MarkerType markerType = getMarkerType(state, ErmsTransformer.uuidMarkerRefInformal, "Informal", "Informal", "e");
	        ref.addMarker(markerType, true);
		}else{
			ref = ReferenceFactory.newGeneric();
			logger.warn("Unknown reference type: " + type + ". Created generic instead.");
		}

		return ref;
	}

	@Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;  //not needed
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsReferenceImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(ErmsImportState state){
		return state.getConfig().getDoReferences() != IImportConfigurator.DO_REFERENCES.ALL;
	}
}
