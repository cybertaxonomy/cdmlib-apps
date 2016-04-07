// $Id$
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
	private final Map<Long, UUID> nameMap = new HashMap<Long, UUID>();
	private final Map<Long, UUID> taxonMap = new HashMap<Long, UUID>();
	private UUID checklistClassificationUuid;

    protected RedListGefaesspflanzenImportState(RedListGefaesspflanzenImportConfigurator config) {
        super(config);
    }

    public Map<String, UUID> getAuthorMap() {
        return authorMap;
    }

    public Map<Long, UUID> getNameMap() {
        return nameMap;
    }

    public Map<Long, UUID> getTaxonMap() {
        return taxonMap;
    }

    public void setChecklistClassificationUuid(UUID checklistClassificationUuid) {
        this.checklistClassificationUuid = checklistClassificationUuid;
    }

    public UUID getChecklistClassificationUuid() {
        return checklistClassificationUuid;
    }

}
