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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportNameTypeDesignationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportSynonymMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportTaxIncludedInMapper;
import eu.etaxonomy.cdm.io.common.mapping.ICheckIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.IDbImportMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 09.03.2010
 */
@Component
public class ErmsTaxonRelationImport extends ErmsImportBase<TaxonBase<?>> implements ICheckIgnoreMapper{

    private static final long serialVersionUID = 4092728796922591257L;

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "taxon relations";
	private static final String dbTableName = "tu";

	private static final Class<?> cdmTargetClass = TaxonBase.class;

	@Override
    protected int divideCountBy() { return 5;}  //use only 1000 records

	public ErmsTaxonRelationImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();
			//incldued in
			DbImportTaxIncludedInMapper<?> includedIn
			    = DbImportTaxIncludedInMapper.NewInstance("id", TAXON_NAMESPACE, "accId", TAXON_NAMESPACE,
			            "parentAccId", TAXON_NAMESPACE, null);
			mapping.addMapper(includedIn);//there is only one tree
			//synonym
			mapping.addMapper(DbImportSynonymMapper.NewInstance("id", "tu_accfinal", TAXON_NAMESPACE,
			        "tu_unacceptreason", null, null, true));
			//type designations
			mapping.addMapper(DbImportNameTypeDesignationMapper.NewInstance("id", "tu_typetaxon", ErmsImportBase.NAME_NAMESPACE, "tu_typedesignationstatus"));
		}
		return mapping;
	}

    @Override
    protected String getIdQuery(){
        String result = " SELECT id FROM " + getTableName() +
                " ORDER BY tu_sp";
        return result;
    }

    @Override
    protected String getRecordQuery(ErmsImportConfigurator config) {
		//TODO get automatic by second path mappers
		String selectAttributes =
		    "   myTaxon.id, myTaxon.tu_parent, myTaxon.tu_typetaxon, myTaxon.tu_typedesignation, "
		    + " myTaxon.tu_accfinal, myTaxon.tu_status, myTaxon.tu_unacceptreason, "
			+ " parent.tu_status AS parentStatus, parent.id AS parentId, "
		    + " parentAcc.id AS parentAccId,"
		    + " accTaxon.tu_parent accParentId, "
		    + " CASE WHEN myTaxon.id = parentAcc.id THEN parent.id ELSE ISNULL(parentAcc.id, parent.id) END as accId ";
		String strRecordQuery =
			"   SELECT  " + selectAttributes
			+ " FROM tu AS myTaxon "
			+ "   LEFT JOIN tu AS accTaxon ON myTaxon.tu_accfinal = accTaxon.id "
			+ "   LEFT JOIN tu AS parent ON myTaxon.tu_parent = parent.id "
			+ "   LEFT JOIN tu AS parentAcc ON parentAcc.id = parent.tu_accfinal "
			+ " WHERE ( myTaxon.id IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
	protected void doInvoke(ErmsImportState state) {
		super.doInvoke(state);
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> nameIdSet = new HashSet<>();
			while (rs.next()){
			    handleForeignKey(rs, taxonIdSet, "accId");
				handleForeignKey(rs, taxonIdSet, "tu_accfinal");
				handleForeignKey(rs, taxonIdSet, "id");
				handleForeignKey(rs, nameIdSet, "tu_typetaxon");
				handleForeignKey(rs, nameIdSet, "id");
			}

			//name map
			nameSpace = ErmsImportBase.NAME_NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			return result;

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
    public boolean checkIgnoreMapper(@SuppressWarnings("rawtypes") IDbImportMapper mapper, ResultSet rs) throws SQLException{

	    boolean result = false;
        boolean isAccepted = isAccepted(rs);

        if (mapper instanceof DbImportTaxIncludedInMapper){
		    //here we should add the direct parent or the accepted taxon of the parent
		    return !isAccepted;
		}else if (mapper instanceof DbImportSynonymMapper){
	        //the only exact rule in ERMS is that the accepted taxon (tu_accfinal)
	        // of a synonym (def: id <> tu_accfinal) never again has another
	        // accepted taxon.
	        //So the synonym relation is clearly defined, no matter which status
	        //both related taxa have.
	        //TODO: check if data were only adapted by BGBM this way
			return isAccepted;
		}else if (mapper instanceof DbImportNameTypeDesignationMapper){
			Object tu_typeTaxon = rs.getObject("tu_typetaxon");
			if (tu_typeTaxon == null){
				return true;
			}
		}
		return result;
	}

    private boolean isAccepted(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Object accTaxonId = rs.getObject("tu_accfinal");
        Object accParentId = rs.getObject("accParentId");

        boolean isAccepted = false;
        if(accTaxonId == null){
            isAccepted = true;  //if accTaxonId == null we can only assume this taxon is accepted as we have no other accepted taxon, though in most cases the status is not accepted
        }else if (id == (int)accTaxonId){
            isAccepted = true;
        }else if (accParentId != null && id == (int)accParentId){
            //see also ErmsTaxonImport.getAcceptedTaxaKeys, there with accepted taxon (alternate representation) being there own child. These should be fully accepted as other wise the link to the higher taxon (genus) is not given
            isAccepted = true;
        }
        return isAccepted;
    }

	@Override
	protected boolean doCheck(ErmsImportState state){
//		IOValidator<ErmsImportState> validator = new ErmsTaxonImportValidator();
//		return validator.validate(state);
		return true;
	}

	@Override
	protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoRelTaxa();
	}
}
