/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.MultipleAttributeMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.out.DbAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbExportNotYetImplementedMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbSingleAttributeExportMapperBase;
import eu.etaxonomy.cdm.io.common.mapping.out.IDbExportMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.IndexCounter;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @since 12.05.2009
 */
public class ExpertsAndLastActionMapper
        extends MultipleAttributeMapperBase<DbSingleAttributeExportMapperBase<DbExportStateBase<?, IExportTransformer>>>
        implements IDbExportMapper<DbExportStateBase<?, IExportTransformer>, IExportTransformer>{

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private static final boolean IS_ACTION_TYPE = true;

	public static ExpertsAndLastActionMapper NewInstance(){
		return new ExpertsAndLastActionMapper();
	}

	private ExpertsAndLastActionMapper() {
		singleMappers.add(DbLastActionMapper.NewInstance("LastActionDate", ! IS_ACTION_TYPE));
		singleMappers.add(DbLastActionMapper.NewInstance("LastAction", IS_ACTION_TYPE));
		singleMappers.add(DbAnnotationMapper.NewIncludedInstance(getSpeciesExpertNameType(), "SpeciesExpertName"));
		singleMappers.add(DbExportNotYetImplementedMapper.NewInstance("SpeciesExpertName", "Only implemented for ERMS until now. For other need to better understand what the species expert name is"));
		singleMappers.add(DbExportNotYetImplementedMapper.NewInstance("SpeciesExpertGUID", "SpeciesExpertGUID derives from an external mapeing list: name to GUID from expertsDB"));
	}

    private Collection<UUID> getSpeciesExpertNameType() {
        Set<UUID> result = new HashSet<>();
        result.add(ErmsTransformer.uuidAnnSpeciesExpertName);
        return result;
    }

    @Override
    public void initialize(PreparedStatement stmt, IndexCounter index, DbExportStateBase<?, IExportTransformer> state, String tableName) {
		for (DbSingleAttributeExportMapperBase<DbExportStateBase<?, IExportTransformer>> mapper : singleMappers){
			mapper.initialize(stmt, index, state, tableName);
		}
	}

	@Override
    public boolean invoke(CdmBase cdmBase) throws SQLException {
		boolean result = true;
		for (DbSingleAttributeExportMapperBase<DbExportStateBase<?, IExportTransformer>> mapper : singleMappers){
			result &= mapper.invoke(cdmBase);
		}
		return result;
	}
}
