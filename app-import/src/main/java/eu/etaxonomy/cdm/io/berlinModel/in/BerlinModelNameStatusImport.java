/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelNameStatusImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelNameStatusImport extends BerlinModelImportBase {

    private static final long serialVersionUID = 6984893930082868489L;
    private static final Logger logger = LogManager.getLogger();

	private int modCount = 5000;
	private static final String pluralString = "nomenclatural status";
	private static final String dbTableName = "NomStatusRel";


	public BerlinModelNameStatusImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result =  " SELECT RIdentifier FROM " + getTableName();

		if (StringUtils.isNotEmpty(state.getConfig().getNameIdTable())){
			result += " WHERE nameFk IN (SELECT NameId FROM " + state.getConfig().getNameIdTable() + ")";
		}
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strQuery =
			" SELECT NomStatusRel.*, NomStatus.NomStatus, RefDetail.Details " +
			" FROM NomStatusRel INNER JOIN " +
              	" NomStatus ON NomStatusRel.NomStatusFk = NomStatus.NomStatusId " +
              	" LEFT OUTER JOIN RefDetail ON NomStatusRel.NomStatusRefDetailFk = RefDetail.RefDetailId AND " +
              	" NomStatusRel.NomStatusRefFk = RefDetail.RefFk " +
            " WHERE (RIdentifier IN (" + ID_LIST_TOKEN + "))";
		return strQuery;
	}

	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,BerlinModelImportState state) {
		boolean success = true;
		String dbAttrName;
		String cdmAttrName;

		Set<TaxonName> namesToSave = new HashSet<>();
		BerlinModelImportConfigurator config = state.getConfig();
		@SuppressWarnings("unchecked")
        Map<String, TaxonName> nameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try {
			//get data from database

			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("NomStatus handled: " + (i-1));}

				int nomStatusRelId;
				try {
					nomStatusRelId = rs.getInt("RIdentifier");
				} catch (Exception e) {  //RIdentifier does not exist in BM database
					nomStatusRelId = -1;
				}
				int nomStatusFk = rs.getInt("NomStatusFk");
				int nameId = rs.getInt("nameFk");

				boolean doubtful = rs.getBoolean("DoubtfulFlag");
				String nomStatusLabel = rs.getString("NomStatus");

				TaxonName taxonName = nameMap.get(String.valueOf(nameId));
				//TODO doubtful

				if (taxonName != null ){
					try{
					    NomenclaturalStatus nomStatus;
						if (state.getConfig().isMcl()) {
						    nomStatus = BerlinModelTransformer.nomStatusFkToNomStatusMedchecklist(nomStatusFk, nomStatusLabel);
	                    }else {
	                        nomStatus = BerlinModelTransformer.nomStatusFkToNomStatus(nomStatusFk, nomStatusLabel);
	                    }
						if (nomStatus == null){
							String message = "Nomenclatural status could not be defined for %s ; %s";
							message = String.format(message, nomStatusFk, nomStatusLabel);
							logger.warn(message);
							success = false;
							continue;
						}else{
							if (nomStatus.getType() == null){
								String message = "Nomenclatural status type could not be defined for %s ; %s";
								message = String.format(message, nomStatusFk, nomStatusLabel);
								logger.warn(message);
								success = false;
								continue;
							}else if(nomStatus.getType().getId() == 0){
								getTermService().save(nomStatus.getType());
							}
						}

						//reference
						makeReference(config, nomStatus, nameId, rs, partitioner);

						//Details
						dbAttrName = "details";
						cdmAttrName = "citationMicroReference";
						success &= ImportHelper.addStringValue(rs, nomStatus, dbAttrName, cdmAttrName, true);

						//doubtful
						if (doubtful){
							nomStatus.addMarker(Marker.NewInstance(MarkerType.IS_DOUBTFUL(), true));
						}
						taxonName.addStatus(nomStatus);
						namesToSave.add(taxonName);
					}catch (UnknownCdmTypeException e) {
						logger.warn("NomStatusType " + nomStatusFk + " not yet implemented");
						success = false;
					}
					//TODO
					//ID
					//etc.
				}else{
					logger.warn("TaxonName for NomStatusRel (" + nomStatusRelId + ") does not exist in store");
					success = false;
				}
			}
			logger.info("TaxonNames to save: " + namesToSave.size());
			getNameService().save(namesToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> refDetailIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "nameFk");
				handleForeignKey(rs, referenceIdSet, "NomStatusRefFk");
				handleForeignKey(rs, refDetailIdSet, "NomStatusRefDetailFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

			//refDetail map
			nameSpace = BerlinModelRefDetailImport.REFDETAIL_NAMESPACE;
			idSet = refDetailIdSet;
			Map<String, Reference> refDetailMap= getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, refDetailMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private boolean makeReference(BerlinModelImportConfigurator config, NomenclaturalStatus nomStatus,
			int nameId, ResultSet rs, @SuppressWarnings("rawtypes") ResultSetPartitioner partitioner)
			throws SQLException{

		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refDetailMap = partitioner.getObjectMap(BerlinModelRefDetailImport.REFDETAIL_NAMESPACE);

		Object nomRefFkObj = rs.getObject("NomStatusRefFk");
		Object nomRefDetailFkObj = rs.getObject("NomStatusRefDetailFk");
		//TODO
//		boolean refDetailPrelim = rs.getBoolean("RefDetailPrelim");

		boolean success = true;
		//nomenclatural Reference
		if (refMap != null){
			if (nomRefFkObj != null){
				String nomRefFk = String.valueOf(nomRefFkObj);
				String nomRefDetailFk = String.valueOf(nomRefDetailFkObj);
				Reference ref = getReferenceFromMaps(refDetailMap, refMap, nomRefDetailFk, nomRefFk);

				//setRef
				if (ref == null ){
					//TODO
					if (! config.isIgnoreNull()){logger.warn("Reference (refFk = " + nomRefFk + ") for NomStatus of TaxonName (nameId = " + nameId + ")"+
						" was not found in reference store. Nomenclatural status reference was not set!!");}
				}else{
					nomStatus.setCitation(ref);
				}
			}
		}
		return success;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelNameStatusImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoNameStatus();
	}
}
