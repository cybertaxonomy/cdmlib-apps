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

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraDistributionImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = 1660723334219905683L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraDistributionImport.class);

	protected static final String NAMESPACE = "Distribution";

	private static final String pluralString = "distributions";
	private static final String dbTableName = "Eflora_DistribucionEstatalIndividual";

	public MexicoEfloraDistributionImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdDist "
		        + " FROM " + dbTableName
		        + " ORDER BY IdCat, IdDist ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
        String sqlSelect = " SELECT d.*, t.uuid taxonUuid ";
        String sqlFrom = " FROM " + dbTableName + " d "
                        + " LEFT JOIN " + MexicoEfloraTaxonImport.dbTableName + " t ON d.IdCAT = t.IdCAT ";
		String sqlWhere = " WHERE ( IdDist IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();

	    @SuppressWarnings("unchecked")
        Map<String, TaxonBase<?>> taxonMap = partitioner.getObjectMap(MexicoEfloraTaxonImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String idCombi = rs.getString("IdDist");
				String idCAT = rs.getString("IdCAT");
			    String nombreStr = rs.getString("Nombre");
			    String paisStr = rs.getString("Pais");
			    String estadoStr = rs.getString("Estado");
			    String abreviaturaEstado = rs.getString("AbreviaturaEstado");
			    String tipoDistribucion = rs.getString("TipoDistribucion");

			    int idRegion = rs.getInt("IdRegion");
	            int idTipoDistribucion = rs.getInt("IdTipoDistribucion");
	            int idTipoRegion = rs.getInt("IdTipoRegion");

			    try {
    				TaxonBase<?> taxonBase = taxonMap.get(idCAT);
    				Taxon taxon;
    				if (taxonBase.isInstanceOf(Taxon.class)) {
    				    taxon = CdmBase.deproxy(taxonBase, Taxon.class);
    				}else {
    				    logger.warn(idCombi + ": Taxon is not accepted: " + idCAT);
    				    continue;
    				}

    				NamedArea area = getArea(state, idTipoRegion);
    				PresenceAbsenceTerm status = getStatus(state, tipoDistribucion);

    				Distribution distribution = Distribution.NewInstance(area, status);

    				//TODO
    				Reference ref = null;
    				TaxonDescription description = this.getTaxonDescription(taxon, ref,
    				        false, true);
    				description.addElement(distribution);

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCombi + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		return success;
	}

    private PresenceAbsenceTerm getStatus(MexicoEfloraImportState state, String tipoDistribucion) {
        PresenceAbsenceTerm status;
        try {
            status = state.getTransformer().getPresenceTermByKey(tipoDistribucion);
            if (status == null){
                UUID statusUuid = state.getTransformer().getPresenceTermUuid(tipoDistribucion);
                status = getPresenceTerm(state, statusUuid,
                        tipoDistribucion, tipoDistribucion, null, false);
            }
            return status;
        } catch (UndefinedTransformerMethodException e) {
            e.printStackTrace();
            return null;
        }

    }

    private NamedArea getArea(MexicoEfloraImportState state, Integer idRegion) {
        NamedArea area = state.getAreaMap().get(idRegion);
        if (idRegion != null && area == null) {
            logger.warn("Area not found: " + idRegion);
        }
        return area;
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
//	              handleForeignKey(rs, referenceIdSet, "PTRefFk");
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