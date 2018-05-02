/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.eflora.centralAfrica.checklist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.eflora.centralAfrica.checklist.validation.CentralAfricaChecklistTaxonImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class CentralAfricaChecklistSynonymImport  extends CentralAfricaChecklistImportBase<TaxonBase> implements IMappingImport<TaxonBase, CentralAfricaChecklistImportState>{
    private static final long serialVersionUID = 954395388404224712L;

    private static final Logger logger = Logger.getLogger(CentralAfricaChecklistSynonymImport.class);

	private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();


	private DbImportMapping<?, ?> mapping;

	//second path is not used anymore, there is now an ErmsTaxonRelationImport class instead
//	private boolean isSecondPath = false;

//	private int modCount = 10000;
	private static final String pluralString = "synonyms";
	private static final String dbTableName = "synonyms";
	private static final Class<?> cdmTargetClass = TaxonBase.class;
	private static final String strOrderBy = "";

	public CentralAfricaChecklistSynonymImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}


	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT syn_id FROM " + dbTableName + strOrderBy;
		return strQuery;
	}

	@Override
    protected DbImportMapping getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping();

			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "syn_id", SYNONYM_NAMESPACE));
			//TODO Synonym mapper gibts es auch

		}
		return mapping;
	}

	@Override
	protected String getRecordQuery(CentralAfricaChecklistImportConfigurator config) {
		String strSelect = " SELECT * ";
		String strFrom = " FROM " + dbTableName;
		String strWhere = " WHERE ( syn_id IN (" + ID_LIST_TOKEN + ") )";
		String strRecordQuery = strSelect + strFrom + strWhere + strOrderBy;
		return strRecordQuery;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, CentralAfricaChecklistImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();

		try{
				Set<String> taxonIdSet = new HashSet<String>();
				while (rs.next()){
					handleForeignKey(rs, taxonIdSet, "acc_id");
				}

			//taxon map
			nameSpace = TAXON_NAMESPACE;
			cdmClass = Taxon.class;
			idSet = taxonIdSet;
			Map<String, Taxon> taxonMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
    public TaxonBase createObject(ResultSet rs, CentralAfricaChecklistImportState state) throws SQLException {
		IBotanicalName speciesName = TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES());


		Integer accId = rs.getInt("acc_id");
		Taxon taxon = CdmBase.deproxy(state.getRelatedObject(TAXON_NAMESPACE, String.valueOf(accId)), Taxon.class);

		Reference sec = taxon.getSec();

		String genusString = rs.getString("synonym genus");
		String speciesString = rs.getString("synonym species");
		String authorityString = rs.getString("synonym authority");

		Synonym synonym = Synonym.NewInstance(speciesName, sec);

		speciesName.setGenusOrUninomial(genusString);
		speciesName.setSpecificEpithet(speciesString);
		parser.handleAuthors(speciesName, CdmUtils.concat(" ", new String[] {"", genusString, speciesString, authorityString}), authorityString);

		if (taxon != null){
			taxon.addSynonym(synonym, SynonymType.SYNONYM_OF());
		}else{
			logger.warn("Taxon (" + accId + ") not available for Synonym " + synonym.getTitleCache());
		}
		return synonym;
	}


	@Override
	protected boolean doCheck(CentralAfricaChecklistImportState state){
		IOValidator<CentralAfricaChecklistImportState> validator = new CentralAfricaChecklistTaxonImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(CentralAfricaChecklistImportState state){
		return ! state.getConfig().isDoTaxa();
	}

}
