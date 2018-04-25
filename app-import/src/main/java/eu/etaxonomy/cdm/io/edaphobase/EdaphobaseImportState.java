/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbImportStateBase;

/**
 * @author a.mueller
 * @since 18.12.2015
 *
 */
public class EdaphobaseImportState extends DbImportStateBase<EdaphobaseImportConfigurator, EdaphobaseImportState>{
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseImportState.class);

    private final Map<String, UUID> authorMap = new HashMap<>();
    private final Set<Integer> synonymsWithAcceptedTaxa = new HashSet<>();

    /**
     * @param config
     */
    protected EdaphobaseImportState(EdaphobaseImportConfigurator config) {
        super(config);
    }

    public UUID getAuthorUuid(String key){
        return authorMap.get(key);
    }

    public void setAuthorMap(Map<String, UUID> authorMap){
        this.authorMap.putAll(authorMap);
    }

    public void addSynonymWithAcceptedTaxon(Integer synId){
        synonymsWithAcceptedTaxa.add(synId);
    }

    public boolean hasAcceptedTaxon (Integer synId){
        return synonymsWithAcceptedTaxa.contains(synId);
    }
}
