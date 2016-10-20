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
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */

@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportFamily extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportFamily.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "families";


    public RedListGefaesspflanzenImportFamily() {
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
        try {
            Collection<TaxonBase> families = importFamilies(state);
            getTaxonService().saveOrUpdate(families);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private Collection<TaxonBase> importFamilies(RedListGefaesspflanzenImportState state) throws SQLException {
        Collection<TaxonBase> families = new HashSet<TaxonBase>();
        Map<String, UUID> familyMapGL = new HashMap<>();
        Map<String, UUID> familyMapCL = new HashMap<>();

        String query = "SELECT DISTINCT f.FAMILIE "
                + " FROM GATTUNG_FAMILIE f";

        ResultSet rs = state.getConfig().getSource().getResultSet(query);
        while(rs.next()){
            String familieStr = rs.getString("FAMILIE");
            BotanicalName name = BotanicalName.NewInstance(Rank.FAMILY());
            name.setGenusOrUninomial(familieStr);
            Taxon familyGL = Taxon.NewInstance(name, null);
            familyMapGL.put(familieStr, familyGL.getUuid());
            //clone for checkliste
            Taxon familyCL = Taxon.NewInstance(name, null);
            familyMapCL.put(familieStr, familyCL.getUuid());

            families.add(familyGL);
            families.add(familyCL);
        }
        state.setFamilyMapGesamtListe(familyMapGL);
        state.setFamilyMapCheckliste(familyMapCL);
        return families;
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
