/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelWebMarkerImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelWebMarkerImport extends BerlinModelImportBase {

    private static final long serialVersionUID = 6350956896121390550L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 2000;
	private static final String dbTableName = "webMarker";
	private static final String pluralString = "markers";

	public BerlinModelWebMarkerImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT markerId FROM " + getTableName();
		if (state.getConfig().getWebMarkerFilter() != null){
			result += " WHERE " +  state.getConfig().getWebMarkerFilter();
		}
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strQuery =
			" SELECT *  " +
            " FROM webMarker INNER JOIN webTableName ON webMarker.TableNameFk = webTableName.TableNameId " +
            " WHERE (markerId IN ("+ ID_LIST_TOKEN + ") )";
		return strQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;

		@SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToBeSaved = new HashSet<>();

		@SuppressWarnings("rawtypes")
        Map<String, DefinedTermBase> definedTermMap = state.getDbCdmDefinedTermMap();
		ResultSet rs = partitioner.getResultSet();

		int i = 0;
		//for each reference
		try{
			while (rs.next()){
				try{
					if ((i++ % modCount ) == 0 && i!= 1 ){ logger.info(""+pluralString+" handled: " + (i-1));}
					//
					int markerId = rs.getInt("MarkerId");
					int markerCategoryFk = rs.getInt("MarkerCategoryFk");
					int rIdentifierFk = rs.getInt("RIdentifierFk");
					String tableName = rs.getString("TableName");
					Boolean activeFlag = rs.getBoolean("ActiveFlag");

					AnnotatableEntity annotatableEntity;
					if ("PTaxon".equals(tableName)){
						TaxonBase<?> taxon = taxonMap.get(String.valueOf(rIdentifierFk));
						if (taxon != null){
							annotatableEntity = taxon;
							taxaToBeSaved.add(taxon);
							addMarker(annotatableEntity, activeFlag, markerCategoryFk, definedTermMap);
						}else{
							logger.warn("TaxonBase (RIdentifier " + rIdentifierFk + ") could not be found for marker " + markerId);
						}
					}else{
						logger.warn("Marker for table " + tableName + " not yet implemented.");
						success = false;
					}


				}catch(Exception ex){
					logger.error(ex.getMessage());
					ex.printStackTrace();
					success = false;
				}
			} //while rs.hasNext()
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		logger.info("save " + i + " "+pluralString + " ...");
		getTaxonService().saveOrUpdate(taxaToBeSaved);
		return success;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			while (rs.next()){
				int tableNameId = rs.getInt("TableNameFk");
				if (tableNameId != 500){
					//TODO
					logger.warn("A marker is not related to table PTaxon. This case is not handled yet!");
				}else{
					handleForeignKey(rs, taxonIdSet, "RIdentifierFk");
				}
			}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private boolean addMarker(AnnotatableEntity annotatableEntity, boolean activeFlag, int markerCategoryFk,
	        @SuppressWarnings("rawtypes") Map<String, DefinedTermBase> map ){
		MarkerType markerType = (MarkerType)map.get("webMarkerCategory_" + markerCategoryFk);
		if (markerType == null){
			logger.warn("MarkerType not found: " + markerCategoryFk);
		}
		Marker marker = Marker.NewInstance(markerType, activeFlag);
		annotatableEntity.addMarker(marker);
		return true;

	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelWebMarkerImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoMarker();
	}
}
