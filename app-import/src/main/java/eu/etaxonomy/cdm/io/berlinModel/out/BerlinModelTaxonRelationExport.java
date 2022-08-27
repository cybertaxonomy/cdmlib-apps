/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.out;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.CdmDbExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CreatedAndNotesMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonRelationExport extends BerlinModelExportBase<RelationshipBase> {
    private static final long serialVersionUID = -7568153921512019118L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 1000;
	private static final String dbTableName = "RelPTaxon";
	private static final String pluralString = "TaxonRelationships";
	private static final Class<? extends CdmBase> standardMethodParameter = RelationshipBase.class;


	public BerlinModelTaxonRelationExport(){
		super();
	}

	@Override
	protected boolean doCheck(BerlinModelExportState state){
		boolean result = true;
		logger.warn("Exporting synonym relationships not implemented anymore. See https://dev.e-taxonomy.eu/redmine/issues/5974 for reasons.");
		logger.warn("Checking for " + pluralString + " not yet implemented");

		return result;
	}

	private CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> getMapping(){
		String tableName = dbTableName;
		CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = new CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer>(tableName);
//		mapping.addMapper(IdMapper.NewInstance("RelPTaxonId"));  //is Identity column

		mapping.addMapper(MethodMapper.NewInstance("PTNameFk1", this.getClass(), "getPTNameFk1", standardMethodParameter, DbExportStateBase.class));
		mapping.addMapper(MethodMapper.NewInstance("PTRefFk1", this.getClass(), "getPTRefFk1", standardMethodParameter, DbExportStateBase.class));

		mapping.addMapper(MethodMapper.NewInstance("PTNameFk2", this.getClass(), "getPTNameFk2", standardMethodParameter, DbExportStateBase.class));
		mapping.addMapper(MethodMapper.NewInstance("PTRefFk2", this.getClass(), "getPTRefFk2", standardMethodParameter, DbExportStateBase.class));

		mapping.addMapper(MethodMapper.NewInstance("RelQualifierFk", this));

		mapping.addMapper(DbObjectMapper.NewInstance("citation", "RelRefFk"));
//		mapping.addMapper(RefDetailMapper.NewInstance("citationMicroReference","citation", "FactRefDetailFk"));
		mapping.addMapper(CreatedAndNotesMapper.NewInstance());

		return mapping;
	}

	@Override
    protected void doInvoke(BerlinModelExportState state){
		try{
			logger.info("start make " + pluralString + " ...");
			boolean success = true ;
			doDelete(state);

			TransactionStatus txStatus = startTransaction(true);

			List<TaxonRelationship> list = getTaxonService().listTaxonRelationships(null,  null,  null,  null,  null);

			CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = getMapping();
			mapping.initialize(state);

			int count = 0;
			for (TaxonRelationship rel : list){
				doCount(count++, modCount, pluralString);
				success &= mapping.invoke(rel);

			}
			commitTransaction(txStatus);
			logger.info("end make " + pluralString + " ..." + getSuccessString(success));
			if (!success){
                String message = "An undefined error occurred during taxon relation export";
                state.getResult().addError(message);
			}
			return;
		}catch(SQLException e){
			e.printStackTrace();
			logger.error(e.getMessage());
			state.getResult().addException(e);
			return;
		}
	}


	protected boolean doDelete(BerlinModelExportState state){
		BerlinModelExportConfigurator bmeConfig = state.getConfig();

		String sql;
		Source destination =  bmeConfig.getDestination();
		//RelPTaxon
		sql = "DELETE FROM RelPTaxon";
		destination.setQuery(sql);
		destination.update(sql);

		return true;
	}

	@Override
    protected boolean isIgnore(BerlinModelExportState state){
		return ! state.getConfig().isDoRelTaxa();
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static Integer getRelQualifierFk(RelationshipBase<?, ?, ?> rel){
		return BerlinModelTransformer.taxRelation2relPtQualifierFk(rel);
	}

	@SuppressWarnings("unused")
	private static Integer getPTNameFk1(RelationshipBase<?, ?, ?> rel, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(rel, state, true, true);
	}

	@SuppressWarnings("unused")
	private static Integer getPTRefFk1(RelationshipBase<?, ?, ?> rel, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(rel, state, false, true);
	}

	@SuppressWarnings("unused")
	private static Integer getPTNameFk2(RelationshipBase<?, ?, ?> rel, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(rel, state, true, false);
	}

	@SuppressWarnings("unused")
	private static Integer getPTRefFk2(RelationshipBase<?, ?, ?> rel, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(rel, state, false, false);
	}

	private static Integer getObjectFk(RelationshipBase<?, ?, ?> rel, DbExportStateBase<?, IExportTransformer> state, boolean isName, boolean isFrom){
		TaxonBase<?> taxon = null;
		if (rel.isInstanceOf(TaxonRelationship.class)){
			TaxonRelationship tr = (TaxonRelationship)rel;
			taxon = (isFrom) ? tr.getFromTaxon():  tr.getToTaxon();
		//#5974 we need a new mapper for synonym relationships
//		}else if (rel.isInstanceOf(SynonymRelationship.class)){
//			SynonymRelationship sr = (SynonymRelationship)rel;
//			taxon = (isFrom) ? sr.getSynonym() : sr.getAcceptedTaxon();
		}
		if (taxon != null){
			CdmBase cdmBase = (isName) ? taxon.getName(): taxon.getSec();
			return state.getDbId(cdmBase);
		}
		logger.warn("No taxon found for relationship: " + rel.toString());
		return null;
	}

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}
}
