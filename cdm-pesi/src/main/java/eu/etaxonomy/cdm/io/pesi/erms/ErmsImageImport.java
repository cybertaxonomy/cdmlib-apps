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
import eu.etaxonomy.cdm.io.common.mapping.DbImportImageCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMediaMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsImageImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsImageImport  extends ErmsImportBase<TextData> {

    private static final long serialVersionUID = 3482371545516808276L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsImageImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "images";
	private static final String dbTableName = "images";
	private static final Class<?> cdmTargetClass = Media.class;
	private static final int devideCountBy = 10;

	public ErmsImageImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getIdQuery() {
		String strIdQuery =
			" SELECT tu_id, img_thumb " +   //tu_id is not a key
			" FROM images " +
			" ORDER BY tu_id, img_thumb, img_url ";
		return strIdQuery;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM images " +
			" WHERE ( images.tu_id IN (" + ID_LIST_TOKEN + ") AND " +
				"  images.img_thumb IN (" + ID_LIST_TOKEN + ")  )";
		return strRecordQuery;
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			//TODO do we need to add to TaxonNameBase too?
			//Often images are attached to synonyms
			String idAttribute = null;
			boolean isOneTextData = true;
			mapping.addMapper(DbImportImageCreationMapper.NewInstance(idAttribute, IMAGE_NAMESPACE, "tu_id", ErmsImportBase.TAXON_NAMESPACE, isOneTextData));
			mapping.addMapper(DbImportMediaMapper.NewInstance("img_url", "img_thumb"));
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
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
			}

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			idSet = taxonIdSet;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected int divideCountBy() { return devideCountBy;}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsImageImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoImages();
	}
}
