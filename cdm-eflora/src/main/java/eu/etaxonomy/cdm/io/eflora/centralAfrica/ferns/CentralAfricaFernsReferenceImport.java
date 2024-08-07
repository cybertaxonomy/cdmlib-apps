/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns;

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
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns.validation.CentralAfricaFernsReferenceImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class CentralAfricaFernsReferenceImport  extends CentralAfricaFernsImportBase<Reference> implements IMappingImport<Reference, CentralAfricaFernsImportState>{

    private static final long serialVersionUID = 6680459184882127822L;
    private static Logger logger = LogManager.getLogger();

    private DbImportMapping<?,?> mapping;


//	private int modCount = 10000;
	private static final String pluralString = "references";
	private static final String dbTableName = "literature";
	private static final Class<?> cdmTargetClass = Reference.class;

	public CentralAfricaFernsReferenceImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strRecordQuery =
			" SELECT refID " +
			" FROM " + dbTableName;
		return strRecordQuery;
	}

	@Override
	protected String getRecordQuery(CentralAfricaFernsImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM literature " +
			" WHERE ( literature.refId IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
    protected DbImportMapping getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

//			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "refID", REFERENCE_NAMESPACE)); //id
			mapping.addMapper(DbIgnoreMapper.NewInstance("CountryDummy"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("CreatedBy"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("DateCreated"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("DateModified"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("ModifiedBy"));
			mapping.addMapper(DbImportStringMapper.NewInstance("RefBookTitle", "title", false));
			//mapping.addMapper(DbImportTimePeriodMapper.NewInstance("RefDatePublished", "datePublished", false));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("RefDatePublished"));
//			mapping.addMapper(DbImportExtensionTypeCreationMapper.NewInstance(dbIdAttribute, extensionTypeNamespace, dbTermAttribute, dbLabelAttribute, dbLabelAbbrevAttribute)
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("RefIll only"));
			mapping.addMapper(DbImportStringMapper.NewInstance("ISSN", "issn", false));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("RefMarker"));
			mapping.addMapper(DbImportStringMapper.NewInstance("RefPages", "pages"));
			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("RefPages only"));

			Reference ref = null;
//			ref.setP

////			mapping.addMapper(DbImportExtensionMapper.NewInstance("imis_id", GlobisTransformer.IMIS_UUID, "imis", "imis", "imis"));
//
//			mapping.addMapper(DbImportTruncatedStringMapper.NewInstance("source_name", "titleCache", "title"));
//			mapping.addMapper(DbImportStringMapper.NewInstance("source_abstract", "referenceAbstract"));
//			mapping.addMapper(DbImportAnnotationMapper.NewInstance("source_note", AnnotationType.EDITORIAL(), Language.DEFAULT()));
//
//			//or as Extension?
//			mapping.addMapper(DbImportExtensionMapper.NewInstance("source_link", ExtensionType.URL()));
//
//			//not yet implemented
//			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("source_type"));
//			mapping.addMapper(DbNotYetImplementedMapper.NewInstance("source_orig_fn"));

		}
		return mapping;
	}

	@Override
    public Reference createObject(ResultSet rs, CentralAfricaFernsImportState state)
			throws SQLException {
		Reference ref;
		String refType = rs.getString("RefType");
		if (refType == null){
			ref = ReferenceFactory.newGeneric();
		}else if (refType == "book"){
			ref = ReferenceFactory.newBook();
		}else if (refType == "paper in journal"){
			ref = ReferenceFactory.newArticle();
		}else if (refType.startsWith("unpublished") ){
			ref = ReferenceFactory.newGeneric();
		}else if (refType.endsWith("paper in journal")){
			ref = ReferenceFactory.newArticle();
		}else if (refType == "paper in book"){
			ref = ReferenceFactory.newBookSection();
		}else if (refType == "paper in journalwebsite"){
			ref = ReferenceFactory.newArticle();
		}else{
			logger.warn("Unknown reference type: " + refType);
			ref = ReferenceFactory.newGeneric();
		}
		return ref;
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, CentralAfricaFernsImportState state) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		return result;  //not needed
	}

	@Override
	protected boolean doCheck(CentralAfricaFernsImportState state){
		IOValidator<CentralAfricaFernsImportState> validator = new CentralAfricaFernsReferenceImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(CentralAfricaFernsImportState state){
		//TODO
		return state.getConfig().getDoReferences() != IImportConfigurator.DO_REFERENCES.ALL;
	}
}