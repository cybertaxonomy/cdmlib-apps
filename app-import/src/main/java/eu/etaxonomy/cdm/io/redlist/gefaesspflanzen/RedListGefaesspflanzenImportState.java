/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */
public class RedListGefaesspflanzenImportState extends DbImportStateBase<RedListGefaesspflanzenImportConfigurator, RedListGefaesspflanzenImportState>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportState.class);

	private final Map<String, UUID> authorMap = new HashMap<String, UUID>();
	private Map<String, UUID> familyMap = new HashMap<String, UUID>();
	private UUID checklistClassificationUuid;

    protected RedListGefaesspflanzenImportState(RedListGefaesspflanzenImportConfigurator config) {
        super(config);
    }

    public Map<String, UUID> getAuthorMap() {
        return authorMap;
    }

    public void setChecklistClassificationUuid(UUID checklistClassificationUuid) {
        this.checklistClassificationUuid = checklistClassificationUuid;
    }

    public UUID getChecklistClassificationUuid() {
        return checklistClassificationUuid;
    }

    public Map<String, UUID> getFamilyMap() {
        return familyMap;
    }

    public void setFamilyMap(Map<String, UUID> familyMap) {
        this.familyMap = familyMap;
    }

}
