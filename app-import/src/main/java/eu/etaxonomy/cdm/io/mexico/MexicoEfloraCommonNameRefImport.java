/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraCommonNameRefImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -1712022169834400067L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraCommonNameRefImport.class);

	protected static final String NAMESPACE = "CommonNameRef";

	private static final String pluralString = "common name sources";
	private static final String dbTableName = "Eflora_RelBiblioNomComun";

	public MexicoEfloraCommonNameRefImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCombinado "
		        + " FROM " + dbTableName
		        + " ORDER BY IdCombinado ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName;
		String sqlWhere = " WHERE ( IdCombinado IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, CommonTaxonName> commonNameMap = partitioner.getObjectMap(MexicoEfloraCommonNameImport.NAMESPACE);

	    @SuppressWarnings("unchecked")
	    Map<String, Reference> referenceMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String idCombi = rs.getString("IdCombinado");
				int idBibliografia = rs.getInt("IdBibliografia");
				String observaciones = rs.getString("Observaciones");
				//needed?
//				String idNomComun = rs.getString("IdNomComun");

			    try {
			        CommonTaxonName commonName = commonNameMap.get(idCombi);

    				Reference ref = referenceMap.get(String.valueOf(idBibliografia));
    				String detail = state.getRefDetailMap().get(idBibliografia);

    				if (commonName != null) {
    				    DescriptionElementSource source = commonName.addPrimaryTaxonomicSource(ref, detail);
    				    if (source!= null) {
    				        TaxonName nameUsedInSource = getNameUsedInSource(state, observaciones);
    				        source.setNameUsedInSource(nameUsedInSource);
    				        //TODO other observaciones
    				    } else {
    				        logger.warn("Source not found for " + idCombi + " and bibID: " + idBibliografia);
    				    }
    				}else {
    				    logger.warn("CommonName not found for " + idCombi);
    				}

					partitioner.startDoSave();
				} catch (Exception e) {
				    e.printStackTrace();
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCombi + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		logger.warn("Finished partition");

		return success;
	}

    private TaxonName getNameUsedInSource(MexicoEfloraImportState state, String observaciones) {
        // TODO named used in source for common names
        return null;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> commonNameIdSet = new HashSet<>();
            Set<String> referenceIdSet = new HashSet<>();
            while (rs.next()){
                handleForeignKey(rs, referenceIdSet, "IdBibliografia");
                handleForeignKey(rs, commonNameIdSet, "IdCombinado");
            }

			//common name map
			nameSpace = MexicoEfloraCommonNameImport.NAMESPACE;
			Map<UUID,String> uuidMap = new HashMap<>();
			commonNameIdSet.stream().forEach(cnId->uuidMap.put(state.getCommonNameMap().get(cnId),cnId));
			@SuppressWarnings({ "rawtypes", "unchecked" })
            List<CommonTaxonName> commonNames = (List)getDescriptionElementService().find(uuidMap.keySet());
			Map<String, CommonTaxonName> commonNameMap = new HashMap<>();
			commonNames.stream().forEach(cn->commonNameMap.put(uuidMap.get(cn.getUuid()), cn));
			result.put(nameSpace, commonNameMap);

            //reference map
            nameSpace = MexicoEfloraReferenceImportBase.NAMESPACE;
            idSet = referenceIdSet;
            Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
            result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

    @Override
    protected boolean doCheck(MexicoEfloraImportState state){
        return true;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}