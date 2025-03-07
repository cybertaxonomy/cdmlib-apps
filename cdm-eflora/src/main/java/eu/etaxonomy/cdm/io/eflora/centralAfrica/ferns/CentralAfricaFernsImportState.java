/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.centralAfrica.ferns;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class CentralAfricaFernsImportState extends DbImportStateBase<CentralAfricaFernsImportConfigurator, CentralAfricaFernsImportState>{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	Map<String, DefinedTermBase> dbCdmDefTermMap = new HashMap<>();

	Map<String, User> usernameMap = new HashMap<>();

	private String taxonNumber;
	private Rank currentRank;

	@Override
	public void initialize(CentralAfricaFernsImportConfigurator config) {
//		super(config);
		String tableName = "WebMarkerCategory_";
		//webMarkerCategory
		dbCdmDefTermMap.put(tableName + 1, MarkerType.COMPLETE());
	}

	public CentralAfricaFernsImportState(CentralAfricaFernsImportConfigurator config) {
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

	@Override
    public User getUser(String username){
		return usernameMap.get(username);
	}

	@Override
    public void putUser(String username, User user){
		usernameMap.put(username, user);
	}

	public void setTaxonNumber(String taxonNumber) {
		this.taxonNumber = taxonNumber;
	}

	public String getTaxonNumber() {
		return taxonNumber;
	}

	public void setCurrentRank(Rank currentRank) {
		this.currentRank = currentRank;
	}

	public Rank getCurrentRank() {
		return currentRank;
	}
}