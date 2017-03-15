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
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
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
 * @created 27.02.2012
 */
@Component
public class IndexFungorumSpeciesImport  extends IndexFungorumImportBase {
	private static final Logger logger = Logger.getLogger(IndexFungorumSpeciesImport.class);

	private static final String pluralString = "species";
	private static final String dbTableName = "[tblPESIfungi-IFdata]";


	public IndexFungorumSpeciesImport(){
		super(pluralString, dbTableName, null);

	}




	@Override
	protected String getIdQuery() {
		String result = " SELECT PreferredNameIFnumber FROM " + getTableName() +
				" ORDER BY PreferredName ";
		return result;
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery =
				" SELECT DISTINCT distribution.PreferredNameFDCnumber, species.* , cl.[Phylum name]" +
				" FROM tblPESIfungi AS distribution RIGHT OUTER JOIN  dbo.[tblPESIfungi-IFdata] AS species ON distribution.PreferredNameIFnumber = species.PreferredNameIFnumber " +
					" LEFT OUTER JOIN [tblPESIfungi-Classification] cl ON species.PreferredName   = cl.PreferredName " +
				" WHERE ( species.PreferredNameIFnumber IN (" + ID_LIST_TOKEN + ") )" +
			"";
		return strRecordQuery;
	}
	@Override
    protected void doInvoke(IndexFungorumImportState state){
        System.out.println("start make " + getPluralString() + " ...");
        super.doInvoke(state);




	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, IndexFungorumImportState state) {
		boolean success = true;
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		ResultSet rs = partitioner.getResultSet();
		Classification classification = getClassification(state);

		try {
			while (rs.next()){

				//DisplayName, NomRefCache -> don't use, created by Marc

				Integer id = (Integer)rs.getObject("PreferredNameIFnumber");
				String phylumName = rs.getString("Phylum name");

				String preferredName = rs.getString("PreferredName");
				if (StringUtils.isBlank(preferredName)){
					logger.warn("Preferred name is blank. This case is not yet handled by IF import. RECORD UMBER" + CdmUtils.Nz(id));
				}

				//Rank rank = Rank.SPECIES();

				NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
				INonViralName name = parser.parseSimpleName(preferredName, NomenclaturalCode.ICNAFP, null);

				Taxon taxon = Taxon.NewInstance(name, sourceReference);
				//if name is infraspecific the parent should be the species not the genus
				Taxon parent;
				if (!name.isInfraSpecific()){
				    parent = getParentTaxon(state, rs);
				    if (parent == null){
	                    logger.warn("parent not found for name:" +preferredName);
	                }
				    classification.addParentChild(parent, taxon, null, null);
				}

				//author + publication
				makeAuthorAndPublication(state, rs, name);
				//source
				makeSource(state, taxon, id, NAMESPACE_SPECIES );

				//fossil
				if (FOSSIL_FUNGI.equalsIgnoreCase(phylumName)){
					ExtensionType fossilExtType = getExtensionType(state, ErmsTransformer.uuidFossilStatus, "fossil status", "fossil status", "fos. stat.");
					Extension.NewInstance(taxon, PesiTransformer.STR_FOSSIL_ONLY, fossilExtType);
				}
				//save

				UUID uuidTaxon = getTaxonService().saveOrUpdate(taxon);
				//getNameService().saveOrUpdate(name);
				if (name.isInfraSpecific()){
                    state.getInfraspecificTaxaUUIDs().add(uuidTaxon);
                }

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
		Integer genusId = rs.getInt("PreferredNameFDCnumber");

		Taxon taxon = state.getRelatedObject(NAMESPACE_GENERA, String.valueOf(genusId), Taxon.class);
		if (taxon == null){
			logger.warn("Taxon not found for " + genusId);
		}
		return taxon;
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, IndexFungorumImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();

		try{
			Set<String> taxonIdSet = new HashSet<String>();
			Set<String> taxonSpeciesNames = new HashSet<String>();
 			while (rs.next()){
				handleForeignKey(rs, taxonIdSet,"PreferredNameFDCnumber" );
				handleForeignKey(rs, taxonSpeciesNames, "PreferredName");
			}

			//taxon map
			nameSpace = NAMESPACE_GENERA;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
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
