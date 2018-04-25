/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 *
 * @author pplitzner
 * @since Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportRank extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportRank.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "ranks";


    public RedListGefaesspflanzenImportRank() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(RedListGefaesspflanzenImportState state) {
        return null;
    }

    @Override
    protected String getRecordQuery(RedListGefaesspflanzenImportConfigurator config) {
        return null;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        Rank rank = (Rank) getTermService().load(Rank.uuidInfraspecificTaxon);
        rank.getRepresentation(Language.DEFAULT()).setAbbreviatedLabel("[unranked]");
        getTermService().saveOrUpdate(rank);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        return true;
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        return result;
    }

    @Override
    protected boolean doCheck(RedListGefaesspflanzenImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(RedListGefaesspflanzenImportState state) {
        return false;
    }

}
