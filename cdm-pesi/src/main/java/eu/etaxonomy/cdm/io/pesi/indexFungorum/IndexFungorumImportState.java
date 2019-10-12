/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class IndexFungorumImportState extends DbImportStateBase<IndexFungorumImportConfigurator, IndexFungorumImportState>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(IndexFungorumImportState.class);

	private Map<String, DefinedTermBase> dbCdmDefTermMap = new HashMap<>();

	private Map<String, User> usernameMap = new HashMap<>();

	private Map<String, TaxonBase> speciesMap;

	private List<UUID> infraspecificTaxaUUIDs = new ArrayList<>();



	public IndexFungorumImportState(IndexFungorumImportConfigurator config) {
		super(config);
	}

	@Override
	public void initialize(IndexFungorumImportConfigurator config) {
//		super(config);
	}

    public List<UUID> getInfraspecificTaxaUUIDs() {
        return infraspecificTaxaUUIDs;
    }
    public void setInfraspecificTaxaUUIDs(List<UUID> infraspecificTaxaUUIDs) {
        this.infraspecificTaxaUUIDs = infraspecificTaxaUUIDs;
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

	@Override
    public User getUser(String username){
		return usernameMap.get(username);
	}
	@Override
    public void putUser(String username, User user){
		usernameMap.put(username, user);
	}

    public Map<String, TaxonBase> getSpeciesMap() {
        return speciesMap;
    }
    public void setSpeciesMap(Map<String, TaxonBase> speciesMap) {
        this.speciesMap = speciesMap;
    }

}
