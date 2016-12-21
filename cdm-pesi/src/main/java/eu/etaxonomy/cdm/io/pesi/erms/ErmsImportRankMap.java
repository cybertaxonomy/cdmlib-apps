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
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsRankImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @created 01.03.2010
 */
@Component
public class ErmsImportRankMap extends ErmsImportBase<Rank>{
	private static final Logger logger = Logger.getLogger(ErmsImportRankMap.class);

	private Map<Integer, Map<Integer,Rank>> rankMap;

	/**
	 * @param pluralString
	 * @param dbTableName
	 */
	public ErmsImportRankMap() {
		super(null, null, null);
	}


	@Override
	public boolean invoke (ErmsImportState state){
		rankMap = new HashMap<Integer, Map<Integer,Rank>>();
		Source source = state.getConfig().getSource() ;
		String strSQL = " SELECT * FROM ranks ";
		ResultSet rs = source.getResultSet(strSQL);
		try {
			while (rs.next()){
				Integer kingdomId = rs.getInt("kingdom_id");
				Integer rankId = rs.getInt("rank_id");
				String rankName = rs.getString("rank_name");
				NomenclaturalCode nc = ErmsTransformer.kingdomId2NomCode(kingdomId);
				
				Map<Integer, Rank> kingdomMap = makeKingdomMap(rankMap, rankId);			
				try {
					rankName = rankName.replace("Forma", "Form").replace("Subforma", "Subform");
					Rank rank;
					if (nc == null && kingdomId == 1){
						rank = getRank(state, ErmsTransformer.uuidRankSuperdomain, "Superdomain", "Superdomain", "Superdomain", CdmBase.deproxy(Rank.GENUS().getVocabulary(), OrderedTermVocabulary.class), Rank.DOMAIN(), RankClass.Suprageneric);
					}else{
						rank = Rank.getRankByEnglishName(rankName, nc, false);
					}
					if (rank == null){
						logger.warn("Rank is null: " + rankName);
					}
					kingdomMap.put(kingdomId, rank);	
				} catch (UnknownCdmTypeException e) {
					String errorMessage = "Rank '" + rankName + "' is not well mapped for code " + nc + ", kingdom_id = " + kingdomId + ". Rank is ignored!";
					logger.warn(errorMessage);
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		state.setRankMap(rankMap);
		return true;
	}
	
	/**
	 * Retrieves or creates the kingdom map (mapping kingdom to rank for a defined rank_id) and
	 * adds it to the rank map.
	 * @param rankMap
	 * @param rankId
	 * @return
	 */
	private Map<Integer, Rank> makeKingdomMap(Map<Integer, Map<Integer, Rank>> rankMap, Integer rankId) {
		Map<Integer, Rank> result = rankMap.get(rankId);
		if (result == null){
			result = new HashMap<Integer, Rank>();
			rankMap.put(rankId, result);
		}
		return result;
	}


	@Override
	protected boolean doCheck(ErmsImportState state) {
		IOValidator<ErmsImportState> rankImport = new ErmsRankImportValidator();
		return rankImport.validate(state);
	}


	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected boolean isIgnore(ErmsImportState state) {
		return false;  //should always be called
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		return null;  // not needed
	}

	public Rank createObject(ResultSet rs, ErmsImportState state)
			throws SQLException {
		return null;  // not needed
	}


	@Override
	protected DbImportMapping<?, ?> getMapping() {
		return null;  //not needed
	}
	
}
