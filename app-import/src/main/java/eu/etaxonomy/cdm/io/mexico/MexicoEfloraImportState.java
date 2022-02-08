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

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
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

	private boolean isReferenceSecondPath = false;

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

	public boolean isReferenceSecondPath() {
		return isReferenceSecondPath;
	}

	public void setReferenceSecondPath(boolean isReferenceSecondPath) {
		this.isReferenceSecondPath = isReferenceSecondPath;
	}
}
