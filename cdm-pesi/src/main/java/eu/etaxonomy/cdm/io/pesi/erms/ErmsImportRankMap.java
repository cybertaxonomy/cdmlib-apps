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
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 01.03.2010
 */
@Component
public class ErmsImportRankMap extends ErmsImportBase<Rank>{

    private static final long serialVersionUID = -3956071827341860580L;
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
	public void invoke (ErmsImportState state){
		rankMap = new HashMap<>();
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
				rankName = rankName.replace("Forma", "Form").replace("Subforma", "Subform");
				Rank rank = null;
				@SuppressWarnings("unchecked")
				OrderedTermVocabulary<Rank> voc = CdmBase.deproxy(Rank.GENUS().getVocabulary(), OrderedTermVocabulary.class);
				if (nc == null && kingdomId == 1){
				    rank = Rank.DOMAIN();
				}else{
    				try {
    					rank = Rank.getRankByEnglishName(rankName, nc, false);
    				} catch (UnknownCdmTypeException e) {
					}
                    if (rank == null){
                        if (kingdomId == 2){
                            if (rankId == 85){
                                rank = getRank(state, ErmsTransformer.uuidRankSubterclass, "Subterclass", "Subterclass", null, voc, Rank.INFRACLASS(), RankClass.Suprageneric);
                            }else if (rankId == 122){
                                rank = getRank(state, ErmsTransformer.uuidRankParvorder, "Parvorder", "Parvorder", null, voc, Rank.INFRACLASS(), RankClass.Suprageneric);
                            }
                        }

                        if (kingdomId == 3){
                            if (rankId == 214){
                                rank = Rank.SPECIESAGGREGATE();
                            }else if (rankId == 216){
                                rank = getRank(state, ErmsTransformer.uuidRankCollectiveSpecies, "Collective Species", "Collective Species", "Coll. sp.", voc, Rank.INFRACLASS(), RankClass.Suprageneric);
                            }
                        }
                    }
					if (rank == null){
	                      logger.warn("Rank could not be defined: " + rankName + "; nomcode = " + nc + ", kingdom_id = " + kingdomId);
					}
					kingdomMap.put(kingdomId, rank);
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		state.setRankMap(rankMap);
		return ; //state.getResult();
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
			result = new HashMap<>();
			rankMap.put(rankId, result);
		}
		return result;
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		return null;   // not needed
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {
		return null;  // not needed
	}

	@Override
	protected DbImportMapping<?, ?> getMapping() {
		return null;  //not needed
	}

    @Override
    protected boolean doCheck(ErmsImportState state) {
        IOValidator<ErmsImportState> rankImport = new ErmsRankImportValidator();
        return rankImport.validate(state);
    }

    @Override
    protected boolean isIgnore(ErmsImportState state) {
        return false;  //should always be called
    }
}
