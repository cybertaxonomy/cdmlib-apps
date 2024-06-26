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

import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.CdmDbExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CollectionExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CreatedAndNotesMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbBooleanMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelAuthorTeamExport extends BerlinModelExportBase<Team> {

    private static final long serialVersionUID = 3089928128608037344L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 1000;
	private static final String dbTableName = "AuthorTeam";
	private static final String pluralString = "AuthorTeams";
	private static final Class<? extends CdmBase> standardMethodParameter = Team.class;

	public BerlinModelAuthorTeamExport(){
		super();
	}

	@Override
	protected boolean doCheck(BerlinModelExportState state){
		boolean result = true;
		logger.warn("Checking for "+pluralString+" not yet implemented");
		//result &= checkArticlesWithoutJournal(bmiConfig);

		return result;
	}

	public CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> getMapping(){
		String tableName = dbTableName;
		CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping
		            = new CdmDbExportMapping<>(tableName);
		mapping.addMapper(IdMapper.NewInstance("AuthorTeamId"));
		mapping.addMapper(MethodMapper.NewInstance("AuthorTeamCache", this));
		mapping.addMapper(MethodMapper.NewInstance("FullAuthorTeamCache", this));
		mapping.addMapper(DbBooleanMapper.NewFalseInstance("isProtectedTitleCache", "PreliminaryFlag"));
		mapping.addMapper(CreatedAndNotesMapper.NewInstance());
		mapping.addCollectionMapping(getTeamMemberMapping());
		return mapping;
	}

	private CollectionExportMapping getTeamMemberMapping(){
		String tableName = "AuthorTeamSequence";
		String collectionAttribute = "teamMembers";
		IdMapper parentMapper = IdMapper.NewInstance("AuthorTeamFk");
		int sequenceStart = 0;
		String sequenceAttribute = "Sequence";
		CollectionExportMapping mapping = CollectionExportMapping.NewInstance(tableName, collectionAttribute, parentMapper, sequenceAttribute, sequenceStart);
		mapping.addMapper(IdMapper.NewInstance("AuthorFk"));
		return mapping;
	}


	@Override
    protected void doInvoke(BerlinModelExportState state){
		try{
			BerlinModelExportConfigurator bmeConfig = state.getConfig();

			logger.info("start make "+pluralString+" ...");
			boolean success = true ;
			doDelete(bmeConfig);

			TransactionStatus txStatus = startTransaction(true);
			Class<Team> clazz = Team.class;
			List<? extends AgentBase> list = getAgentService().list(clazz, 100000000, 0,null,null);

			CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = getMapping();
			mapping.initialize(state);

			logger.info("save "+pluralString+" ...");
			int count = 0;
			for (AgentBase<?> team : list){
				doCount(count++, modCount, pluralString);
				if (team instanceof Team){
					success &= mapping.invoke(team);
				}
			}

			commitTransaction(txStatus);

			logger.info("end make "+pluralString+"  ..." + getSuccessString(success));
			if (!success){
                String message = "An undefined error occurred during AuthorTeam export";
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

	protected boolean doDelete(BerlinModelExportConfigurator config){
		String sql;
		Source destination =  config.getDestination();
		//RelPTaxon
		sql = "DELETE FROM RelPTaxon";
		destination.setQuery(sql);
		destination.update(sql);
		//Fact
		sql = "DELETE FROM Fact";
		destination.setQuery(sql);
		destination.update(sql);
		//PTaxon
		sql = "DELETE FROM PTaxon";
		destination.setQuery(sql);
		destination.update(sql);

		//NameHistory
		sql = "DELETE FROM NameHistory";
		destination.setQuery(sql);
		destination.update(sql);
		//RelName
		sql = "DELETE FROM RelName";
		destination.setQuery(sql);
		destination.update(sql);
		//NomStatusRel
		sql = "DELETE FROM NomStatusRel";
		destination.setQuery(sql);
		destination.update(sql);
		//Name
		sql = "DELETE FROM Name";
		destination.setQuery(sql);
		destination.update(sql);
		//RefDetail
		sql = "DELETE FROM RefDetail";
		destination.setQuery(sql);
		destination.update(sql);
		//Reference
		sql = "DELETE FROM Reference";
		destination.setQuery(sql);
		destination.update(sql);
		//AuthorTeamSequence
		sql = "DELETE FROM AuthorTeamSequence";
		destination.setQuery(sql);
		destination.update(sql);
		//AuthorTeam
		sql = "DELETE FROM AuthorTeam";
		destination.setQuery(sql);
		destination.update(sql);
		return true;
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getAuthorTeamCache(Team team){
		if (team.isProtectedTitleCache()){
			return team.getTitleCache();
		}else{
			return null;
		}
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getFullAuthorTeamCache(Team team){
		if (team.isProtectedTitleCache()){
			return team.getNomenclaturalTitleCache();
		}else{
			return null;
		}
	}

	@Override
    protected boolean isIgnore(BerlinModelExportState state){
		return ! state.getConfig().isDoAuthors();
	}

	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}
}