/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 27.02.2012
 */
@Component
public class IndexFungorumGeneraImport  extends IndexFungorumImportBase {
	private static final Logger logger = Logger.getLogger(IndexFungorumGeneraImport.class);

	private static final String pluralString = "genera";
	private static final String dbTableName = "tblGenera";

	public IndexFungorumGeneraImport(){
		super(pluralString, dbTableName, null);
	}



	@Override
	protected String getIdQuery() {
		String result = " SELECT [RECORD NUMBER] FROM " + getTableName() +
				" ORDER BY [NAME OF FUNGUS] ";
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery =
				" SELECT DISTINCT c.[Family name], c.[Order name], c.[Subclass name], c.[Class name], c.[Subphylum name], c.[Phylum name], c.[Kingdom name], g.* " +
                " FROM tblGenera AS g LEFT OUTER JOIN  dbo.[tblPESIfungi-Classification] AS c ON g.[RECORD NUMBER] = c.PreferredNameFDCnumber " +
			" WHERE ( g.[RECORD NUMBER] IN (" + ID_LIST_TOKEN + ") )" +
			"";
		return strRecordQuery;
	}




	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, IndexFungorumImportState state) {
		boolean success =true;
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		ResultSet rs = partitioner.getResultSet();
		Classification classification = getClassification(state);
		try {
			while (rs.next()){

				//TODO
				//DisplayName, NomRefCache

				Double id = (Double)rs.getObject("RECORD NUMBER");


				String preferredName = rs.getString("NAME OF FUNGUS");
				if (StringUtils.isBlank(preferredName)){
					logger.warn("Preferred name is blank. This case is not yet handled by IF import. RECORD NUMBER" + id);
				}

				Rank rank = Rank.GENUS();
				NonViralName<?> name = BotanicalName.NewInstance(rank);
				name.setGenusOrUninomial(preferredName);

				Taxon taxon = Taxon.NewInstance(name, sourceReference);
				Taxon parent = getParentTaxon(state, rs);
				classification.addParentChild(parent, taxon, null, null);

				//author + publication
				makeAuthorAndPublication(state, rs, name);
				//source
				//makeSource(state, taxon, id, NAMESPACE_GENERA );
				if (id != null){
					makeSource(state, taxon, id.intValue(), NAMESPACE_GENERA );
				} else{
					makeSource(state, taxon, null,NAMESPACE_GENERA);
				}
				getTaxonService().saveOrUpdate(taxon);
			}


		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.setSuccess(false);
			success = false;
		}
		return success;

	}

	private Taxon getParentTaxon(IndexFungorumImportState state, ResultSet rs) throws SQLException {
		String parentName = getParentNameString(rs);
		if (parentName == null){
			logger.warn("Parent name not found for: " + rs.getString("NAME OF FUNGUS"));
		}
		Taxon taxon = state.getRelatedObject(NAMESPACE_SUPRAGENERIC_NAMES, parentName, Taxon.class);
		if (taxon == null){
			logger.warn("Taxon not found for " + parentName + " name of fungus: " +rs.getString("NAME OF FUNGUS") );
		}
		return taxon;
	}


	private String getParentNameString(ResultSet rs) throws SQLException {
		String parentName = rs.getString("Family name");
		if (parentName == null){
			logger.warn(rs.getObject("NAME OF FUNGUS") + " has no family name. ");
			return null;
		}
		if (parentName.equalsIgnoreCase(INCERTAE_SEDIS)){
			parentName = rs.getString("Order name");
			if (parentName.equalsIgnoreCase(INCERTAE_SEDIS)){
				parentName = rs.getString("Subclass name");
				if (parentName.equalsIgnoreCase(INCERTAE_SEDIS)){
					parentName = rs.getString("Class name");
					if (parentName.equalsIgnoreCase(INCERTAE_SEDIS)){
						parentName = rs.getString("Subphylum name");
						if (parentName.equalsIgnoreCase(INCERTAE_SEDIS)){
							parentName = rs.getString("Phylum name");
							if (parentName.equalsIgnoreCase(INCERTAE_SEDIS) || parentName.equalsIgnoreCase(FOSSIL_FUNGI) ){
								parentName = rs.getString("Kingdom name");
							}
						}
					}
				}
			}
		}
		return parentName;
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, IndexFungorumImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();

		try{
			Set<String> taxonNameSet = new HashSet<String>();
			while (rs.next()){
//				handleForeignKey(rs, taxonIdSet,"tu_acctaxon" );
			}

			//taxon map
			nameSpace = NAMESPACE_SUPRAGENERIC_NAMES ;
			cdmClass = TaxonBase.class;
//			idSet = taxonNameSet;
			Map<String, TaxonBase<?>> taxonMap = new HashMap<String, TaxonBase<?>>();
			List<TaxonBase> list = getTaxonService().listTaxaByName(Taxon.class, "*", null, null, null, null, 1000000, null);
			for (TaxonBase<?> taxon : list){
				taxonMap.put(CdmBase.deproxy(taxon.getName(), NonViralName.class).getGenusOrUninomial(), taxon);
			}
			result.put(nameSpace, taxonMap);

			//sourceReference
			Reference sourceReference = getReferenceService().find(PesiTransformer.uuidSourceRefIndexFungorum);
			Map<String, Reference> referenceMap = new HashMap<String, Reference>();
			referenceMap.put(SOURCE_REFERENCE, sourceReference);
			result.put(NAMESPACE_REFERENCE, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	@Override
	protected boolean doCheck(IndexFungorumImportState state){
		return true;
	}

	@Override
	protected boolean isIgnore(IndexFungorumImportState state){
		return ! state.getConfig().isDoTaxa();
	}





}