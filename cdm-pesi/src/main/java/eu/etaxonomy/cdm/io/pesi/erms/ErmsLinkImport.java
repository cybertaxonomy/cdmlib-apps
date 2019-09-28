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
import eu.etaxonomy.cdm.io.common.mapping.DbIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportExtensionCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsLinkImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsLinkImport  extends ErmsImportBase<TaxonBase> {

    private static final long serialVersionUID = 1270264097223862441L;
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ErmsLinkImport.class);

    public static final String TOKEN_URL = "@URL: ";
    public static final String TOKEN_LINKTEXT = " ,@Text: ";

	private DbImportMapping<ErmsImportState,ErmsImportConfigurator> mapping;

	private static final String pluralString = "links";
	private static final String dbTableName = "links";
	private static final Class<?> cdmTargetClass = Extension.class;

	public ErmsLinkImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT l.* " +
			        ",'%s' + link_url + '%s' + ISNULL(link_text, ' ')   valueAll" + //+ ' ,@Note: ' + ISNULL(CAST(note as nvarchar(max)), ' ')
			" FROM links l " +
			" WHERE ( l.id IN (" + ID_LIST_TOKEN + ") )";
		strRecordQuery = String.format(strRecordQuery, TOKEN_URL, TOKEN_LINKTEXT);
		return strRecordQuery;
	}

	@Override
    protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			ExtensionType extensionType = getExtensionType(ErmsTransformer.uuidExtErmsLink, "ERMS link", "ERMS link", null);
			mapping.addMapper(DbImportExtensionCreationMapper.NewInstance("tu_id", ErmsImportBase.TAXON_NAMESPACE, "valueAll", "id", extensionType));

			//handled in creation mapper
			mapping.addMapper(DbIgnoreMapper.NewInstance("link_text", "handled in creation mapper"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("note", "A note field does not yet exist in PESI.Note"));//not used in SQL script but why not put to PESI.Note.note field?

			//Ignore
			mapping.addMapper(DbIgnoreMapper.NewInstance("link_fn", "Seems to be an internal VLIZ file name. Not used in SQL script "));
			mapping.addMapper(DbIgnoreMapper.NewInstance("link_thumbnail", "Some data (>1000) but not used in SQL script."));
			mapping.addMapper(DbIgnoreMapper.NewInstance("link_qualitystatus_id", "Not used in SQL script."));
			mapping.addMapper(DbIgnoreMapper.NewInstance("link_order", "Not used in SQL script. Until 2019 only 'null' and '0' existed."));
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
			Set<String> taxonIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "tu_id");
			}

			//taxon map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsLinkImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoLinks();
	}
}
