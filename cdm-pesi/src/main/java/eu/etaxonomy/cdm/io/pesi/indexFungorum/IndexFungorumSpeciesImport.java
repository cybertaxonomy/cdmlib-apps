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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 27.02.2012
 */
@Component
public class IndexFungorumSpeciesImport  extends IndexFungorumImportBase {

    private static final long serialVersionUID = -1148034079632876980L;
    private static final Logger logger = Logger.getLogger(IndexFungorumSpeciesImport.class);

	private static final String pluralString = "species";
	private static final String dbTableName = "[tblPESIfungi-IFdata]";

	public IndexFungorumSpeciesImport(){
		super(pluralString, dbTableName);
	}

	@Override
	protected String getIdQuery() {
		String result = " SELECT PreferredNameIFnumber "
		        + " FROM " + getTableName()
				+ " ORDER BY PreferredName ";
		return result;
	}

	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery =
				"   SELECT DISTINCT distribution.PreferredNameFDCnumber, species.* , cl.[Phylum name]"
				+ " FROM tblPESIfungi AS distribution "
				+ "   RIGHT OUTER JOIN  dbo.[tblPESIfungi-IFdata] AS species ON distribution.PreferredNameIFnumber = species.PreferredNameIFnumber " +
					" LEFT OUTER JOIN [tblPESIfungi-Classification] cl ON species.PreferredName = cl.PreferredName "
				+ " WHERE ( species.PreferredNameIFnumber IN (" + ID_LIST_TOKEN + ") )" +
			"";
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
	        IndexFungorumImportState state) {

		boolean success = true;
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		ResultSet rs = partitioner.getResultSet();
		Classification classification = getClassification(state);

		try {
			while (rs.next()){

				//DisplayName, NomRefCache -> don't use, created by Marc

				Integer id = rs.getInt("PreferredNameIFnumber");
				String phylumName = rs.getString("Phylum name");

				String preferredName = rs.getString("PreferredName");
				if (isBlank(preferredName)){
					logger.warn("Preferred name is blank. This case is not yet handled by IF import. RECORD NUMBER" + CdmUtils.Nz(id));
				}

				//Rank rank = Rank.SPECIES();

				NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
				INonViralName name = parser.parseSimpleName(preferredName, NomenclaturalCode.ICNAFP, null);

				Taxon taxon = Taxon.NewInstance(name, sourceReference);
				//if name is infra-specific the parent should be the species not the genus
				Integer genusId = rs.getInt("PreferredNameFDCnumber");
		        if (!name.isInfraSpecific()){
				    Taxon parent = getParentTaxon(state, genusId);
				    if (parent == null){
	                    logger.warn("Parent not found for name:" + preferredName + "; ID(PreferredNameIFnumber): " +id+ "; GenusId(PreferredNameFDCnumber): ");
	                }else{
	                    classification.addParentChild(parent, taxon, null, null);
	                }
				}else {
                    state.getInfraspecificTaxaUUIDs().put(taxon.getUuid(), genusId);
                }

				//author + publication
				makeAuthorAndPublication(state, rs, name);
				//source
				makeSource(state, taxon, id, NAMESPACE_SPECIES );

				//fossil
				if (FOSSIL_FUNGI.equalsIgnoreCase(phylumName)){
					ExtensionType fossilExtType = getExtensionType(state, ErmsTransformer.uuidExtFossilStatus, "fossil status", "fossil status", "fos. stat.");
					Extension.NewInstance(taxon, PesiTransformer.STR_FOSSIL_ONLY, fossilExtType);
				}

				//save
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

    private Taxon getParentTaxon(IndexFungorumImportState state, Integer genusId) {
        return state.getRelatedObject(NAMESPACE_GENERA, String.valueOf(genusId), Taxon.class);
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, IndexFungorumImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> taxonSpeciesNames = new HashSet<>();
 			while (rs.next()){
				handleForeignKey(rs, taxonIdSet,"PreferredNameFDCnumber" );
				handleForeignKey(rs, taxonSpeciesNames, "PreferredName");
			}

			//taxon map
			nameSpace = NAMESPACE_GENERA;
			idSet = taxonIdSet;
			@SuppressWarnings({ "rawtypes" })
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//sourceReference
			Reference sourceReference = getReferenceService().find(PesiTransformer.uuidSourceRefIndexFungorum);
			Map<String, Reference> referenceMap = new HashMap<>();
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
