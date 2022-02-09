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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraDistributionRefImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -3358763003286536675L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraDistributionRefImport.class);

	protected static final String NAMESPACE = "DistributionRef";

	private static final String pluralString = "distribution sources";
	private static final String dbTableName = "Eflora_RelRegionBibliografia";

	public MexicoEfloraDistributionRefImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdDist "
		        + " FROM " + dbTableName
		        + " ORDER BY IdDist ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName;
		String sqlWhere = " WHERE ( IdDist IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, Distribution> distributionMap = partitioner.getObjectMap(MexicoEfloraDistributionImport.NAMESPACE);

	    @SuppressWarnings("unchecked")
	    //TODO
        Map<String, Reference> referenceMap = partitioner.getObjectMap(MexicoEfloraRefArticlesImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String idCombi = rs.getString("IdDist");
				String idBibliografia = rs.getString("IdBibliografia");
			    String observaciones = rs.getString("Observaciones");

			    try {
    				Distribution distribution = distributionMap.get(idCombi);

    				Reference ref = referenceMap.get(idBibliografia);
    				//TODO
    				String detail = null;
    				DescriptionElementSource source = distribution.addPrimaryTaxonomicSource(ref, detail);
    				//TODO
    				TaxonName nameUsedInSource = getNameUsedInSource(state, observaciones);
    				source.setNameUsedInSource(nameUsedInSource);
    				//TODO other observaciones

					partitioner.startDoSave();
//					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCombi + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

//		getTaxonService().save(taxaToSave);
		return success;
	}

    /**
     * @param state
     * @param observaciones
     * @return
     */
    private TaxonName getNameUsedInSource(MexicoEfloraImportState state, String observaciones) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
//				handleForeignKey(rs, nameIdSet, "PTNameFk");
//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
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