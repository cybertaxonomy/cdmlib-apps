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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.out.mapper.RefDetailMapper;
import eu.etaxonomy.cdm.io.berlinModel.out.mapper.TeamOrPersonMapper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.CdmDbExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CollectionExportMapping;
import eu.etaxonomy.cdm.io.common.mapping.out.CreatedAndNotesMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbBooleanMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbExtensionMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbMarkerMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonNameExport extends BerlinModelExportBase<TaxonName> {

    private static final long serialVersionUID = 4478799976310317219L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 2500;
	private static final String dbTableName = "Name";
	private static final String pluralString = "TaxonNames";
	private static final Class<? extends CdmBase> standardMethodParameter = TaxonName.class;

	public BerlinModelTaxonNameExport(){
		super();
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(BerlinModelExportState state){
		boolean result = true;
		logger.warn("Checking for " + pluralString + " not yet fully implemented");
		List<TaxonName> list = getObjectList();
		checkRank(list);

		//result &= checkRank(config);

		return result;
	}

	private boolean checkRank(List<TaxonName> list){
		List<TaxonName> errorNames = new ArrayList<>();
		for (TaxonName name : list){
			if (name.getRank() == null) {
                ;
            }
			errorNames.add(name);
		}
		if (errorNames.size() >0){
			System.out.println("The following names have no Rank:\n=======================");
			for (TaxonName name : errorNames){
				System.out.println("  " + name.toString());
				System.out.println("  " + name.getUuid());
				System.out.println("  " + name.getTitleCache());
			}
			return false;
		}else{
			return true;
		}
	}

	private CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> getMapping(){
		String tableName = dbTableName;
		CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = new CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer>(tableName);
		mapping.addMapper(IdMapper.NewInstance("NameId"));
		mapping.addMapper(MethodMapper.NewInstance("RankFk", this));
		mapping.addMapper(MethodMapper.NewInstance("SupraGenericName", this));
		mapping.addMapper(MethodMapper.NewInstance("Genus", this));
		mapping.addMapper(MethodMapper.NewInstance("NameCache", this));
		mapping.addMapper(MethodMapper.NewInstance("FullNameCache", this));
		mapping.addMapper(MethodMapper.NewInstance("PreliminaryFlag", this));
		mapping.addMapper(DbStringMapper.NewInstance("infraGenericEpithet", "GenusSubDivisionEpi"));
		mapping.addMapper(DbStringMapper.NewInstance("SpecificEpithet", "SpeciesEpi"));
		mapping.addMapper(DbStringMapper.NewInstance("infraSpecificEpithet", "InfraSpeciesEpi"));
		mapping.addMapper(DbStringMapper.NewInstance("appendedPhrase", "UnnamedNamePhrase"));
		mapping.addMapper(DbBooleanMapper.NewInstance("isHybridFormula", "HybridFormulaFlag", false, false));
		mapping.addMapper(DbBooleanMapper.NewInstance("isMonomHybrid", "MonomHybFlag", false, false));
		mapping.addMapper(DbBooleanMapper.NewInstance("isBinomHybrid", "BinomHybFlag", false, false));
		mapping.addMapper(DbBooleanMapper.NewInstance("isTrinomHybrid", "TrinomHybFlag", false, false));
		mapping.addMapper(DbStringMapper.NewFacultativeInstance("cultivarName", "CultivarName"));

		mapping.addMapper(TeamOrPersonMapper.NewInstance("combinationAuthorship", "AuthorTeamFk"));
		mapping.addMapper(TeamOrPersonMapper.NewInstance("exCombinationAuthorship", "ExAuthorTeamFk"));
		mapping.addMapper(TeamOrPersonMapper.NewInstance("basionymAuthorship", "BasAuthorTeamFk"));
		mapping.addMapper(TeamOrPersonMapper.NewInstance("exBasionymAuthorship", "ExBasAuthorTeamFk"));

		mapping.addMapper(DbObjectMapper.NewInstance("nomenclaturalSource.citation", "NomRefFk"));
		mapping.addMapper(RefDetailMapper.NewInstance("nomenclaturalMicroReference","nomenclaturalReference", "NomRefDetailFk"));
		mapping.addMapper(CreatedAndNotesMapper.NewInstance(false));
		ExtensionType sourceAccExtensionType = (ExtensionType)getTermService().find(BerlinModelTransformer.SOURCE_ACC_UUID);
		if (sourceAccExtensionType != null){
			mapping.addMapper(DbExtensionMapper.NewInstance(sourceAccExtensionType, "Source_Acc"));
		}
		mapping.addCollectionMapping(getNomStatusMapping());



		//TODO
		//CultivarGroupName
		//NameSourceRefFk
		//     ,[Source_ACC]

		//publicationYear
		//originalPublicationYear
		//breed
//		INonViralName n = null;
		//n.getNomenclaturalMicroReference()
		return mapping;
	}

	private CollectionExportMapping getNomStatusMapping(){
		String tableName = "NomStatusRel";
		String collectionAttribute = "status";
		IdMapper parentMapper = IdMapper.NewInstance("NameFk");
		CollectionExportMapping<?,?,?> mapping = CollectionExportMapping.NewInstance(tableName, collectionAttribute, parentMapper);
		mapping.addMapper(MethodMapper.NewInstance("NomStatusFk", this.getClass(), "getNomStatusFk", NomenclaturalStatus.class));
		mapping.addMapper(DbObjectMapper.NewInstance("citation", "NomStatusRefFk"));
		mapping.addMapper(RefDetailMapper.NewInstance("citationMicroReference","citation", "NomStatusRefDetailFk"));
		mapping.addMapper(DbMarkerMapper.NewInstance(MarkerType.IS_DOUBTFUL(), "DoubtfulFlag", false));
		mapping.addMapper(CreatedAndNotesMapper.NewInstance());

		return mapping;
	}

	@Override
    protected void doInvoke(BerlinModelExportState state){
		try{
			logger.info("start make "+pluralString+" ...");
			boolean success = true ;
			doDelete(state);

			TransactionStatus txStatus = startTransaction(true);
			logger.info("load "+pluralString+" ...");
			List<TaxonName> names = getObjectList();

			CdmDbExportMapping<BerlinModelExportState, BerlinModelExportConfigurator, IExportTransformer> mapping = getMapping();
			mapping.initialize(state);
			logger.info("save "+pluralString+" ...");
			int count = 0;
			for (TaxonName name : names){
				doCount(count++, modCount, pluralString);
				success &= mapping.invoke(name);
				//TODO rank = null or rank < genus and genusOrUninomial != null
			}
			commitTransaction(txStatus);
			logger.info("end make " + pluralString+ " ..." + getSuccessString(success));

			if (!success){
                String message = "An undefined error occurred during Taxonname export";
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

	protected List<TaxonName> getObjectList(){
		List<TaxonName> list = getNameService().list(null,100000000, null,null,null);
		return list;
	}


	protected boolean doDelete(BerlinModelExportState state){
		BerlinModelExportConfigurator bmeConfig = state.getConfig();

		String sql;
		Source destination =  bmeConfig.getDestination();
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
		return true;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
    protected boolean isIgnore(BerlinModelExportState state){
		return ! state.getConfig().isDoTaxonNames();
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static Integer getRankFk(INonViralName name){
		Integer result = BerlinModelTransformer.rank2RankId(name.getRank());
		if (result == null){
			logger.warn ("Rank = null is not allowed in Berlin Model. Rank was changed to KINGDOM: " + name);
			result = 1;
		}
		return result;
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static Integer getNomStatusFk(NomenclaturalStatus status){
		return BerlinModelTransformer.nomStatus2nomStatusFk(status.getType());
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getSupraGenericName(INonViralName name){
		if (name.isSupraGeneric()){
			return name.getGenusOrUninomial();
		}else{
			return null;
		}
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getGenus(INonViralName name){
		if (! name.isSupraGeneric()){
			return name.getGenusOrUninomial();
		}else{
			return null;
		}
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getNameCache(INonViralName name){
		if (name.isProtectedNameCache()){
			return name.getNameCache();
		}else{
			return null;
		}
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static String getFullNameCache(INonViralName name){
		if (name.isProtectedTitleCache()){
			return name.getTitleCache();
		}else{
			return null;
		}
	}

	//called by MethodMapper
	@SuppressWarnings("unused")
	private static Boolean getPreliminaryFlag(INonViralName name){
		if (name.isProtectedTitleCache() || name.isProtectedNameCache()){
			if (name.isProtectedTitleCache() && name.isProtectedNameCache()){
				logger.warn("protectedTitleCache and protectedNameCache do not have the same value for name " + name.getTitleCache() + ". This can not be mapped appropriately to the Berlin Model ");
			}
			return true;
		}else{
			return false;
		}
	}



	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.out.BerlinModelExportBase#getStandardMethodParameter()
	 */
	@Override
	public Class<? extends CdmBase> getStandardMethodParameter() {
		return standardMethodParameter;
	}

}
