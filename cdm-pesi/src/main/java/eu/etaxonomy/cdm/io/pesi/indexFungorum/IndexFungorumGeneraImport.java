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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 27.02.2012
 */
@Component
public class IndexFungorumGeneraImport  extends IndexFungorumImportBase {

    private static final long serialVersionUID = -265928225339065992L;
    private static Logger logger = LogManager.getLogger();

	private static final String pluralString = "genera";
	private static final String dbTableName = "tblGenera";

	public IndexFungorumGeneraImport(){
		super(pluralString, dbTableName);
	}

	@Override
	protected String getIdQuery() {
		String result = " SELECT [RECORD NUMBER] FROM " + getTableName() +
				" ORDER BY [NAME OF FUNGUS] ";
		return result;
	}

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
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
	        IndexFungorumImportState state) {

	    boolean success =true;
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		ResultSet rs = partitioner.getResultSet();
		Classification classification = getClassification(state);
		try {
			while (rs.next()){

				//Don't use (created bei Marc): DisplayName, NomRefCache

				Integer id = rs.getInt("RECORD NUMBER");

				String preferredName = rs.getString("NAME OF FUNGUS");
				if (StringUtils.isBlank(preferredName)){
					logger.warn("Preferred name is blank. This case is not yet handled by IF import. RECORD NUMBER" + id);
				}

				Rank rank = Rank.GENUS();
				TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);
				name.setGenusOrUninomial(preferredName);

				Taxon taxon = Taxon.NewInstance(name, sourceReference);
				Taxon parent = getParentTaxon(state, rs);
				classification.addParentChild(parent, taxon, null, null);

				//author + publication
				makeAuthorAndPublication(state, rs, name);
				//source
				makeSource(state, taxon, id.intValue(), NAMESPACE_GENERA );
				makeSource(state, name, id.intValue(), NAMESPACE_GENERA );

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
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(
	        ResultSet rs, IndexFungorumImportState state) {

	    Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{

			//taxon map
		    String nameSpace = NAMESPACE_SUPRAGENERIC_NAMES ;
			Map<String, TaxonBase<?>> taxonMap = new HashMap<>();
            List<Taxon> list = getCommonService().getHqlResult(
                      " SELECT t FROM TaxonBase t "
                    + " JOIN t.sources s "
                    + " WHERE s.citation.uuid = ?0 AND t.name.rank.uuid <> ?1",
                    new Object[]{ PesiTransformer.uuidSourceRefIndexFungorum, Rank.uuidGenus},
                    Taxon.class);  //only use index fungorum taxa not being genus (important for partitions not being first partition)
            for (Taxon taxon : list){
                String uninomial = CdmBase.deproxy(taxon.getName()).getGenusOrUninomial();
                TaxonBase<?> existing = taxonMap.put(uninomial, taxon);
                if (existing != null){
                    logger.warn("There seem to be duplicate taxa for uninomial: " + uninomial);
                }
            }
			result.put(nameSpace, taxonMap);

			//sourceReference
			Reference sourceReference = getReferenceService().find(PesiTransformer.uuidSourceRefIndexFungorum);
			Map<String, Reference> referenceMap = new HashMap<>();
			referenceMap.put(SOURCE_REFERENCE, sourceReference);
			result.put(NAMESPACE_REFERENCE, referenceMap);

		} catch (Exception e) {
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
