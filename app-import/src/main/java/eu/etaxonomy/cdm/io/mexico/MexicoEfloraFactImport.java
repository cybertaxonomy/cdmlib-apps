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

	private CategoricalData lastFact;
	private String lastIdCat = "-1";
	private int lastIdCatNombre = -1;
	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    Reference sourceReference = this.getSourceReference(state.getConfig().getSourceReference());
	    //hope this is transaction save
	    if (lastFact != null) {
	        lastFact = (CategoricalData)getDescriptionElementService().find(lastFact.getUuid());
	    }
	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, TaxonBase<?>> taxonMap = partitioner.getObjectMap(MexicoEfloraTaxonImport.NAMESPACE);
        @SuppressWarnings("unchecked")
        Map<String, Reference> referenceMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
			    int id = rs.getInt("id");  //only for partitioning and logging
				String idCAT = rs.getString("IdCAT");
				int idCatNombre = rs.getInt("IdCatNombre");
				String uuidTaxonStr = rs.getString("taxonUuid");

                int idBibliografia = rs.getInt("IdBibliografia");
                //TODO observaciones in facts
                String observaciones = rs.getString("Observaciones");

			    try {
			        CategoricalData categoricalData;
			        if (idCAT.equals(lastIdCat) && idCatNombre == lastIdCatNombre) {
			            categoricalData = lastFact;
			        }else {
			            categoricalData = makeCategoricalData(state, idCatNombre);
			            Feature lastFeature = lastFact == null? null : lastFact.getFeature();
                        if (idCAT.equals(lastIdCat) && categoricalData.getFeature().equals(lastFeature)) {
                            //merge
                            //add the single new state to the existing categorical data
                            //TODO not fully correct if bibliography differs for the single states;
                            State newState = categoricalData.getStatesOnly().stream().findFirst().orElse(null);
                            if (newState != null) {
                                lastFact.addStateData(newState);
                            }
                            categoricalData  = lastFact;
//                            lastIdCatNombre = idCatNombre;
                        }else {
                            //new categorical data
                            TaxonBase<?> taxonBase = taxonMap.get(uuidTaxonStr);
                            Taxon taxon;
                            if (taxonBase.isInstanceOf(Taxon.class)) {
                                taxon = CdmBase.deproxy(taxonBase, Taxon.class);
                            }else {
                                logger.warn(idCatNombre + ": Taxon is not accepted: " + idCAT);
                                continue;
                            }

                            //TODO source reference correct?
                            TaxonDescription description = this.getTaxonDescription(taxon, sourceReference,
                                    false, true);
                            description.addElement(categoricalData);
                            lastFact = categoricalData;
                            lastIdCat = idCAT;
                            lastIdCatNombre = idCatNombre;
                        }
			        }
			        handleBibliografia(state, referenceMap, categoricalData, idBibliografia, id);

					partitioner.startDoSave();
				} catch (Exception e) {
				    e.printStackTrace();
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

    private void handleBibliografia(MexicoEfloraImportState state, Map<String, Reference> referenceMap,
            CategoricalData categoricalData, int idBibliografia,
            int id) {
        Reference ref = referenceMap == null ? null : referenceMap.get(String.valueOf(idBibliografia));
//        String detail = state.getRefDetailMap().get(idBibliografia);
        String detail = null;

        if (ref != null) {
            if (categoricalData != null) {
                categoricalData.addPrimaryTaxonomicSource(ref, detail);
            }else {
                logger.warn("Fact does not exist: " + id);
            }
        }else {
            logger.warn("Source not found for " + id + " and bibID: " + idBibliografia);
        }
    }

    private CategoricalData makeCategoricalData(MexicoEfloraImportState importState,
            int idCatNombre) {
        Feature feature = getFeature(importState, idCatNombre);
        State state = getState(importState, idCatNombre);
        CategoricalData categoricalData = CategoricalData.NewInstance(state, feature);
        return categoricalData;
    }

    private State getState(MexicoEfloraImportState importState, int idCatNombre) {
        State result = importState.getStateMap().get(idCatNombre);
        if (result == null) {
            logger.warn("State does not exist: " + idCatNombre);
        }
        return result;
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
				handleForeignKey(rs, referenceIdSet, "IdBibliografia");
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