/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.mexico;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * State for Mexico Eflora import.
 *
 * @author a.mueller
 * @date 29.01.2022
 *
 */
public class MexicoEfloraImportState
        extends DbImportStateBase<MexicoEfloraImportConfigurator, MexicoEfloraImportState>{

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(MexicoEfloraImportState.class);

	private Map<String, DefinedTermBase> dbCdmDefTermMap = new HashMap<>();

	private Map<Integer,Feature> featureMap = new HashMap<>();
	private Map<Integer,State> stateMap = new HashMap<>();
	private Map<Integer,NamedArea> areaMap = new HashMap<>();
	private Map<String,NamedArea> areaLabelMap = new HashMap<>();

	private Map<Integer,UUID> referenceUuidMap = new HashMap<>();
//	private Map<Integer,String> refDetailMap = new HashMap<>();

	private Map<String,UUID> commonNameMap = new HashMap<>();
    private Map<String,UUID> distributionMap = new HashMap<>();


	public MexicoEfloraImportState(MexicoEfloraImportConfigurator config) {
		super(config);
	}

	public Map<String, DefinedTermBase> getDbCdmDefinedTermMap(){
		return this.dbCdmDefTermMap;
	}

	public void putDefinedTermToMap(String tableName, String id, DefinedTermBase term){
		 this.dbCdmDefTermMap.put(tableName + "_" + id, term);
	}

	public void putDefinedTermToMap(String tableName, int id, DefinedTermBase term){
		putDefinedTermToMap(tableName, String.valueOf(id), term);
	}

    public Map<Integer,Feature> getFeatureMap() {
        return featureMap;
    }

    public Map<Integer,State> getStateMap() {
        return stateMap;
    }

    public Map<Integer,NamedArea> getAreaMap() {
        return areaMap;
    }
    public Map<String,NamedArea> getAreaLabelMap() {
        return areaLabelMap;
    }

    public Map<Integer,UUID> getReferenceUuidMap() {
        return referenceUuidMap;
    }
//    public Map<Integer,String> getRefDetailMap() {
//        return refDetailMap;
//    }

    public Map<String,UUID> getCommonNameMap() {
        return commonNameMap;
    }
    public Map<String,UUID> getDistributionMap() {
        return distributionMap;
    }



}
