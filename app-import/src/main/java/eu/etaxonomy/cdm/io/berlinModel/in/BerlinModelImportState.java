/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class BerlinModelImportState extends DbImportStateBase<BerlinModelImportConfigurator, BerlinModelImportState>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(BerlinModelImportState.class);

	private Map<String, DefinedTermBase> dbCdmDefTermMap = new HashMap<>();

	private boolean isReferenceSecondPath = false;

	private Map<String, UUID> xmlImportRefUuids = new HashMap<>();


	@Override
	public void initialize(BerlinModelImportConfigurator config) {
//		super(config);
		String tableName = "WebMarkerCategory_";
		//webMarkerCategory
		dbCdmDefTermMap.put(tableName + 1, MarkerType.COMPLETE());
	}

	public BerlinModelImportState(BerlinModelImportConfigurator config) {
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


	public boolean isReferenceSecondPath() {
		return isReferenceSecondPath;
	}

	public void setReferenceSecondPath(boolean isReferenceSecondPath) {
		this.isReferenceSecondPath = isReferenceSecondPath;
	}


    public UUID getXmlImportRefUuid(String username) {
        return xmlImportRefUuids.get(username);
    }

    /**
     * @param username
     * @param uuid
     */
    public void putXmlImportRefUuid(String username, UUID uuid) {
        xmlImportRefUuids.put(username, uuid);
    }




}
