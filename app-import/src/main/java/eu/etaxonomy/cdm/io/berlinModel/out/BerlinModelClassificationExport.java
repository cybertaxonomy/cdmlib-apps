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
import java.sql.Types;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.DbExportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.out.CdmDbExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CreatedAndNotesMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbConstantMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.RelationshipBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelClassificationExport extends BerlinModelExportBase<RelationshipBase> {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 1000;
	private static final String dbTableName = "RelPTaxon";
	private static final String pluralString = "TaxonNodes";
	private static final Class<? extends CdmBase> standardMethodParameter = TaxonNode.class;


	public BerlinModelClassificationExport(){
		super();
	}

	@Override
	protected boolean doCheck(BerlinModelExportState state){
		boolean result = true;
		logger.warn("Checking for " + pluralString + " not yet implemented");
		//result &= checkArticlesWithoutJournal(bmiConfig);
		//result &= checkPartOfJournal(bmiConfig);

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

		mapping.addMapper(DbConstantMapper.NewInstance("RelQualifierFk", Types.INTEGER, 1));

		mapping.addMapper(DbObjectMapper.NewInstance("referenceForParentChildRelation", "RelRefFk"));
//		mapping.addMapper(RefDetailMapper.NewInstance("citationMicroReference","citation", "FactRefDetailFk"));
		mapping.addMapper(CreatedAndNotesMapper.NewInstance());

		return mapping;
	}

	@Override
    protected void doInvoke(BerlinModelExportState state){
		if (state.getConfig().isUseClassification() == false){
			return;
		}

		try{
			logger.info("start make " + pluralString + " ...");
			boolean success = true ;
			doDelete(state);

			TransactionStatus txStatus = startTransaction(true);

			List<Classification> list = getClassificationService().list(null,10000000,0,null,null);

			CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = getMapping();
			mapping.initialize(state);

			int count = 0;
			for (Classification tree : list){
				for (TaxonNode node : tree.getAllNodes()){
					if (node.isTopmostNode()){
						continue;
					}else{
						doCount(count++, modCount, pluralString);
						success &= mapping.invoke(node);
					}
				}
			}
			commitTransaction(txStatus);
			logger.info("end make " + pluralString + " ..." + getSuccessString(success));
			if (!success){
                String message = "An undefined error occurred during Classification export";
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

		//already deleted in BerlinModelTaxonRelationExport
//		String sql;
//		Source destination =  bmeConfig.getDestination();
//		//RelPTaxon
//		sql = "DELETE FROM RelPTaxon ";
//		destination.setQuery(sql);
//		destination.update(sql);

		return true;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
    protected boolean isIgnore(BerlinModelExportState state){
		return ! state.getConfig().isDoRelTaxa();
	}

	//called by MethodMapper

	@SuppressWarnings("unused")
	private static Integer getPTNameFk1(TaxonNode node, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(node, state, true, true);
	}

	@SuppressWarnings("unused")
	private static Integer getPTRefFk1(TaxonNode node, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(node, state, false, true);
	}

	@SuppressWarnings("unused")
	private static Integer getPTNameFk2(TaxonNode node, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(node, state, true, false);
	}

	@SuppressWarnings("unused")
	private static Integer getPTRefFk2(TaxonNode node, DbExportStateBase<?, IExportTransformer> state){
		return getObjectFk(node, state, false, false);
	}

	private static Integer getObjectFk(TaxonNode node, DbExportStateBase<?, IExportTransformer> state, boolean isName, boolean isFrom){
		TaxonNode treeNode = (isFrom) ? node :  node.getParent();
		if (treeNode != null){
			Taxon taxon = treeNode.getTaxon();
			CdmBase cdmBase = (isName) ? taxon.getName(): taxon.getSec();
			return state.getDbId(cdmBase);
		}
		logger.warn("No taxon or parent taxon found for taxon node: " + node.toString());
		return null;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.out.BerlinModelExportBase#getStandardMethodParameter()
	 */
	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}
}
