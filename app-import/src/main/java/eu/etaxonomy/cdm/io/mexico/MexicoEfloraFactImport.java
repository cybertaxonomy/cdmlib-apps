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
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraFactImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = 8097679811768529307L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraFactImport.class);

	protected static final String NAMESPACE = "Facts";

	private static final String pluralString = "facts";
	private static final String dbTableName = "Eflora_RelBiblioNombreCatalogoNombre";

	public MexicoEfloraFactImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
	    //TODO
		String sql = " SELECT id  "
		        + " FROM " + dbTableName
		        + " ORDER BY IdCAT, IdCatNombre, IdBibliografia ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT f.*, t.uuid taxonUuid ";
		String sqlFrom = " FROM " + dbTableName + " f LEFT JOIN " + MexicoEfloraTaxonImport.dbTableName + " t ON f.IdCAT = t.IdCAT ";
		String sqlWhere = " WHERE ( Id IN (" + ID_LIST_TOKEN + ") )";
		String sqlOrderBy = " ORDER BY IdCAT, IdCatNombre, IdBibliografia";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere + sqlOrderBy;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, TaxonBase<?>> taxonMap = partitioner.getObjectMap(MexicoEfloraTaxonImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
			    int id = rs.getInt("id");
				int idCatNombre = rs.getInt("IdCatNombre");
				String idCAT = rs.getString("IdCAT");
				String uuidTaxonStr = rs.getString("taxonUuid");
//				UUID uuidTaxon = UUID.fromString(uuidTaxonStr);

				//TODO
                int idBibliografia = rs.getInt("IdBibliografia");
                String observaciones = rs.getString("Observaciones");

			    try {
    				TaxonBase<?> taxonBase = taxonMap.get(uuidTaxonStr);
    				Taxon taxon;
    				if (taxonBase.isInstanceOf(Taxon.class)) {
    				    taxon = CdmBase.deproxy(taxonBase, Taxon.class);
    				}else {
    				    logger.warn(idCatNombre + ": Taxon is not accepted: " + idCAT);
    				    continue;
    				}

    				//TODO
    				Reference ref = null;
                    TaxonDescription description = this.getTaxonDescription(taxon, ref,
                            false, true);
    				makeCategoricalData(state, idCatNombre, description);

					partitioner.startDoSave();
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create fact for id " + id + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	    logger.warn("Next partition");

		return success;
	}

    private void makeCategoricalData(MexicoEfloraImportState importState,
            int idCatNombre, TaxonDescription description) {
        Feature feature = getFeature(importState, idCatNombre);
        State state = getState(importState, idCatNombre);
        //TODO merge data
        CategoricalData categoricalData = CategoricalData.NewInstance(state, feature);

        description.addElement(categoricalData);
        return;
    }

    private State getState(MexicoEfloraImportState importState, int idCatNombre) {
        return importState.getStateMap().get(idCatNombre);
    }

    private Feature getFeature(MexicoEfloraImportState importState, int idCatNombre) {
        return importState.getFeatureMap().get(idCatNombre);
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<UUID> taxonIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignUuidKey(rs, taxonIdSet, "taxonUuid");
//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

			//taxon map
			nameSpace = MexicoEfloraTaxonImport.NAMESPACE;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = new HashMap<>();
			@SuppressWarnings("rawtypes")
            List<TaxonBase> taxa = getTaxonService().find(taxonIdSet);
			taxa.stream().forEach(t->taxonMap.put(t.getUuid().toString(), t));
			result.put(nameSpace, taxonMap);

			//reference map
			nameSpace = MexicoEfloraRefArticlesImport.NAMESPACE;
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