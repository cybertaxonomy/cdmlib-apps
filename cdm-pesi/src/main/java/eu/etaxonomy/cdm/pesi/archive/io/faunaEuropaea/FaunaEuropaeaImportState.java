/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class FaunaEuropaeaImportState extends ImportStateBase<FaunaEuropaeaImportConfigurator, FaunaEuropaeaImportBase>{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public FaunaEuropaeaImportState(FaunaEuropaeaImportConfigurator config) {
		super(config);
	}

	private Map<Integer, FaunaEuropaeaTaxon> fauEuTaxonMap = new HashMap<Integer, FaunaEuropaeaTaxon>();
	private Map<UUID, UUID> childParentMap = new HashMap<UUID, UUID>();
	private Map<Integer, UUID> agentUUIDMap = new HashMap<Integer, UUID>();
	private final HashMap<String,UUID> idToUUID = new HashMap<String, UUID>();
	private TermVocabulary<NamedArea> areaVoc;

	/* Highest taxon index in the FauEu database */
//	private int highestTaxonIndex = 305755;
	/* Max number of taxa to be saved with one service call */
//	private int limit = 20000;


//	/**
//	 * @return the limit
//	 */
//	public int getLimit() {
//		return limit;
//	}
//
//	/**
//	 * @param limit the limit to set
//	 */
//	public void setLimit(int limit) {
//		this.limit = limit;
//	}


	/**
	 * @return the fauEuTaxonMap
	 */
	public Map<Integer, FaunaEuropaeaTaxon> getFauEuTaxonMap() {
		return fauEuTaxonMap;
	}

	/**
	 * @param fauEuTaxonMap the fauEuTaxonMap to set
	 */
	public void setFauEuTaxonMap(Map<Integer, FaunaEuropaeaTaxon> fauEuTaxonMap) {
		this.fauEuTaxonMap = fauEuTaxonMap;
	}

	/**
	 * @return the childParentMap
	 */
	public Map<UUID, UUID> getChildParentMap() {
		return childParentMap;
	}

	/**
	 * @param childParentMap the childParentMap to set
	 */
	public void setChildParentMap(Map<UUID, UUID> childParentMap) {
		this.childParentMap = childParentMap;
	}

    /**
     * @return the agentMap
     */
    public Map<Integer, UUID> getAgentMap() {
        return agentUUIDMap;
    }
    /**
     * @return the agentMap
     */
    public void setAgentMap(Map<Integer, UUID> agentUUIDMap) {
        this.agentUUIDMap = agentUUIDMap;
    }

    public NamedArea areaId2NamedArea(FaunaEuropaeaDistribution dis, FaunaEuropaeaImportState state){
        UUID uuid = idToUUID.get(dis.getAreaCode());
        return state.getNamedArea(uuid);
    }

    public void setIdToUuid(String id, UUID uuid){
        if (idToUUID.get(id) != null && !idToUUID.get(id).equals(uuid)){
            logger.error("There are two different uuids for one id.");
        }
        idToUUID.put(id, uuid);
    }

    @Override
    public void putNamedArea(NamedArea namedArea){
        super.putNamedArea(namedArea);
        setIdToUuid(namedArea.getIdInVocabulary(), namedArea.getUuid());
    }

    /**
     * @return the areaVoc
     */
    public TermVocabulary<NamedArea> getAreaVoc() {
        return areaVoc;
    }

    /**
     * @param areaVoc the areaVoc to set
     */
    public void setAreaVoc(TermVocabulary<NamedArea> areaVoc) {
        this.areaVoc = areaVoc;
    }


}
