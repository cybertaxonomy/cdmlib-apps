/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.fauEu2Cdm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmImportState
        extends ImportStateBase<FauEu2CdmImportConfigurator,CdmImportBase>{

    private ICdmRepository sourceRepository;
    private UUID rootUuid;

    protected FauEu2CdmImportState(FauEu2CdmImportConfigurator config) {
        super(config);
    }


    public ICdmRepository getSourceRepository() {
        return sourceRepository;
    }
    public void setSourceRepository(ICdmRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public UUID getRootId() {
        return rootUuid;
    }
    public void setRootId(UUID rootId) {
        this.rootUuid = rootId;
    }


    private Map<UUID, Integer> targetMap = new HashMap<>();
    /**
     * @param uuid
     * @param class1
     */
    public <T extends CdmBase> Integer getTarget(UUID uuid, Class<T> clazz) {
        return targetMap.get(uuid);

    }

}
