/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.faueu;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;
import eu.etaxonomy.cdm.model.common.Language;

/**
 * @author a.mueller
 * @since 01.07.2025
 */
public class FaunaEuropaeaCommonNameImportState
            extends DbImportStateBase<FaunaEuropaeaCommonNameImportConfigurator, FaunaEuropaeaCommonNameImportState>{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private Map<String, Language> languageMap = new HashMap<>();


	public FaunaEuropaeaCommonNameImportState(FaunaEuropaeaCommonNameImportConfigurator config) {
		super(config);
	}

	@Override
	public void initialize(FaunaEuropaeaCommonNameImportConfigurator config) {
//		super(config);
	}

	public Map<String, Language> getLanguageMap(){
		return this.languageMap;
	}
	public void putLanguageToMap(String tableName, String id, Language term){
		 this.languageMap.put(tableName + "_" + id, term);
	}
	public void putLanguageToMap(String tableName, int id, Language term){
		putLanguageToMap(tableName, String.valueOf(id), term);
	}
}