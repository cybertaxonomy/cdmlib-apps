/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * The export state class.
 * Holds data needed while the export classes are running.
 *
 * @author e.-m.lee
 * @since 12.02.2010
 */
public class PesiExportState extends DbExportStateBase<PesiExportConfigurator, PesiTransformer>{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private static List<Integer> processedSourceList = new ArrayList<>();

	private IdentifiableEntity<?> currentToObject;
	private IdentifiableEntity<?> currentFromObject;
	private TaxonBase<?> currentTaxon;
	private boolean sourceForAdditionalSourceCreated = false;

	private final Map<UUID, MarkerType> markerTypeMap = new HashMap<>();
	private final Map<String,Integer> treeIndexKingdomMap = new HashMap<>();


    public Map<String,Integer> getTreeIndexKingdomMap() {
        return treeIndexKingdomMap;
    }

	public PesiExportState(PesiExportConfigurator config) {
		super(config);
	}

	/**
	 * Stores the Datawarehouse.id to a specific CDM object originally.
	 * Does nothing now since we do not want to store Cdm.id/Datawarehouse.id pairs. This saves precious memory.
	 * @param cdmBase
	 * @param dbId
	 */
	@Override
	public void putDbId(CdmBase cdmBase, int dbId) {
		// Do nothing
	}

	/**
	 * TODO -> move to PesiExportBase
	 * Gets the Datawarehouse.id to a specific CDM object originally.
	 * Here it just returns the CDM object's id.
	 */
	@Override
	public Integer getDbId(CdmBase cdmBase) {
		return (Integer)getCurrentIO().getDbId(cdmBase, this);
	}

	/**
	 * Returns whether the given Source object was processed before or not.
	 */
	public boolean alreadyProcessedSource(Integer sourceId) {
		if (processedSourceList.contains(sourceId)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Adds given Source to the list of processed Sources.
	 */
	public boolean addToProcessedSources(Integer sourceId) {
		if (! processedSourceList.contains(sourceId)) {
			processedSourceList.add(sourceId);
		}
		return true;
	}

	/**
	 * Clears the list of already processed Sources.
	 */
	public void clearAlreadyProcessedSources() {
		processedSourceList.clear();
	}

	public IdentifiableEntity<?> getCurrentToObject() {
		return currentToObject;
	}
	public void setCurrentToObject(IdentifiableEntity<?> currentToObject) {
		this.currentToObject = currentToObject;
	}

	public IdentifiableEntity<?> getCurrentFromObject() {
		return currentFromObject;
	}
	public void setCurrentFromObject(IdentifiableEntity<?> currentFromObject) {
		this.currentFromObject = currentFromObject;
	}


	public TaxonBase<?> getCurrentTaxon() {
		return currentTaxon;
	}
	public void setCurrentTaxon(TaxonBase<?> currentTaxon) {
		this.currentTaxon = currentTaxon;
	}

	public MarkerType getMarkerType(UUID uuid){
	    return markerTypeMap.get(uuid);
	}
	public void putMarkerType(MarkerType markerType) {
		markerTypeMap.put(markerType.getUuid(), markerType);
	}

	public boolean isSourceForAdditionalSourceCreated() {
		return sourceForAdditionalSourceCreated;
	}
	public void setSourceForAdditionalSourceCreated(
			boolean sourceForAdditionalSourceCreated) {
		this.sourceForAdditionalSourceCreated = sourceForAdditionalSourceCreated;
	}
}
