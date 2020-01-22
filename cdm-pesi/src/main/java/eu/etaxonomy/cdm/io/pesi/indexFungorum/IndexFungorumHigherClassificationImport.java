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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
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
public class IndexFungorumHigherClassificationImport  extends IndexFungorumImportBase {

    private static final long serialVersionUID = -6841466146506309309L;
    private static final Logger logger = Logger.getLogger(IndexFungorumHigherClassificationImport.class);

	private static final String pluralString = "higher classifications";
	private static final String dbTableName = "tblPESIfungi-Classification";

	public IndexFungorumHigherClassificationImport(){
		super(pluralString, dbTableName);
	}

	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery =
			" SELECT DISTINCT [Kingdom name], [Phylum name], [Subphylum name], [Class name], [Subclass name], [Order name], [Family name], g.[NAME OF FUNGUS] as GenusName, c.PreferredName as SpeciesName " +
			" FROM [tblPESIfungi-Classification] c  LEFT OUTER JOIN " +
                      " tblGenera g ON c.PreferredNameFDCnumber = g.[RECORD NUMBER]" +
			" ORDER BY [Kingdom name], [Phylum name], [Subphylum name], [Class name], [Subclass name], [Order name],  [Family name], GenusName, SpeciesName ";
		return strRecordQuery;
	}

	@Override
	protected void doInvoke(IndexFungorumImportState state) {

	    logger.info("Start higher classification ...");

		String sql = getRecordQuery(state.getConfig());
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);

		//only 1 partition here

		String lastKingdom = "";
		String lastPhylum = "";
		String lastSubphylum = "";
		String lastClassname = "";
		String lastSubclass = "";
		String lastOrder = "";
		String lastFamily = "";

		Taxon taxonKingdom = null;
		Taxon taxonPhylum = null;
		Taxon taxonSubphylum = null;
		Taxon taxonClass = null;
		Taxon taxonSubclass = null;
		Taxon taxonOrder = null;
		Taxon taxonFamily = null;

		Taxon higherTaxon = null;

		TransactionStatus tx = startTransaction();
		ResultSet rsRelatedObjects = state.getConfig().getSource().getResultSet(sql);
		state.setRelatedObjects(getRelatedObjectsForPartition(rsRelatedObjects, state));

		Classification classification = getClassification(state);

		try {
			while (rs.next()){
				String kingdom = rs.getString("Kingdom name");
				String phylum = rs.getString("Phylum name");
				String subphylum = rs.getString("Subphylum name");
				String classname = rs.getString("Class name");
				String subclass = rs.getString("Subclass name");
				String order = rs.getString("Order name");
				String family = rs.getString("Family name");

				if (isNewTaxon(family, lastFamily)){
					if (isNewTaxon(order,lastOrder)){
						if (isNewTaxon(subclass,lastSubclass)){
							if (isNewTaxon(classname,lastClassname)){
								if (isNewTaxon(subphylum, lastSubphylum)){
									if (isNewTaxon(phylum,lastPhylum)){
										if (isNewTaxon(kingdom,lastKingdom)){
											taxonKingdom = makeTaxon(state, kingdom, Rank.KINGDOM());
											lastKingdom = kingdom;
											logger.info("Import kingdom " +  kingdom);
											getTaxonService().saveOrUpdate(taxonKingdom);
										}else{
											higherTaxon = taxonKingdom;
										}
										higherTaxon = isIncertisSedis(kingdom) ? higherTaxon : taxonKingdom;
										Rank newRank = (lastKingdom.equals("Fungi") ? null : Rank.PHYLUM());
										taxonPhylum = makeTaxon(state, phylum, newRank);
										if (taxonPhylum != null){
											classification.addParentChild(higherTaxon, taxonPhylum, null, null);
										}
										higherTaxon = isIncertisSedis(phylum) ? higherTaxon : taxonPhylum;
										lastPhylum = phylum;
										logger.info("Import Phylum " +  phylum);
									}else{
										higherTaxon = taxonPhylum;
									}
									Rank newRank = (lastKingdom.equals("Fungi") ? null : Rank.SUBPHYLUM());
									taxonSubphylum = makeTaxon(state, subphylum, newRank);
									if (taxonSubphylum != null){
										getClassification(state).addParentChild(higherTaxon,taxonSubphylum, null, null);
									}
									higherTaxon = isIncertisSedis(subphylum) ? higherTaxon : taxonSubphylum;
									lastSubphylum = subphylum;
								}else{
									higherTaxon = taxonSubphylum;
								}
								taxonClass = makeTaxon(state, classname, Rank.CLASS());
								if (taxonClass != null){
									getClassification(state).addParentChild(higherTaxon, taxonClass, null, null);
								}
								higherTaxon = isIncertisSedis(classname) ? higherTaxon : taxonClass;
								lastClassname = classname;
							}else{
								higherTaxon = taxonClass;
							}
							taxonSubclass = makeTaxon(state, subclass, Rank.SUBCLASS());
							if (taxonSubclass != null){
								getClassification(state).addParentChild(higherTaxon, taxonSubclass,null, null);
							}
							higherTaxon = isIncertisSedis(subclass) ? higherTaxon : taxonSubclass;
							lastSubclass = subclass;
						}else{
							higherTaxon = taxonSubclass;
						}
						taxonOrder = makeTaxon(state, order, Rank.ORDER());
						if (taxonOrder != null){
							getClassification(state).addParentChild(higherTaxon, taxonOrder, null, null);
						}
						higherTaxon = isIncertisSedis(order) ? higherTaxon : taxonOrder;
						lastOrder = order;
					}else{
						higherTaxon = taxonOrder;
					}
					taxonFamily = makeTaxon(state, family, Rank.FAMILY());
					if (taxonFamily != null){
					    try{
							//if this shows a warning see single issue in #2826 about Glomerellaceae (which has 2 different parents)
						    getClassification(state).addParentChild(higherTaxon, taxonFamily, null, null);
						}catch(IllegalStateException e){
							if (e.getMessage().startsWith("The child taxon is already part of the tree")){
								logger.warn(e.getMessage() + taxonFamily.getTitleCache() + " " + higherTaxon.getTitleCache());
							}
						}
					}
					higherTaxon = isIncertisSedis(family) ? higherTaxon : taxonFamily;
					lastFamily = family;
					getTaxonService().saveOrUpdate(higherTaxon);
				}
				getTaxonService().saveOrUpdate(higherTaxon);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			tx.setRollbackOnly();
			state.setSuccess(false);
		}
		commitTransaction(tx);
		logger.info("End higher classification ...");

		return;
	}

	private boolean isIncertisSedis(String uninomial) {
		return  uninomial.equalsIgnoreCase(INCERTAE_SEDIS) || uninomial.equalsIgnoreCase(FOSSIL_FUNGI);
	}

	private boolean isNewTaxon(String uninomial, String lastUninomial) {
		boolean result =  !uninomial.equalsIgnoreCase(lastUninomial);
		result |= lastUninomial.equalsIgnoreCase(INCERTAE_SEDIS);
		result |= lastUninomial.equalsIgnoreCase(FOSSIL_FUNGI);
		return result;
	}

	private Taxon makeTaxon(IndexFungorumImportState state, String uninomial, Rank newRank) {
		if (uninomial.equalsIgnoreCase(INCERTAE_SEDIS) || uninomial.equalsIgnoreCase(FOSSIL_FUNGI)){
			return null;
		}
		Taxon taxon = state.getRelatedObject(IndexFungorumImportBase.NAMESPACE_SUPRAGENERIC_NAMES, uninomial, Taxon.class);
		if (taxon == null){
			if (! newRank.equals(Rank.KINGDOM())){
				logger.warn("Taxon not found for uninomial " + uninomial);
			}
			TaxonName name = TaxonNameFactory.NewBotanicalInstance(newRank);
			name.setGenusOrUninomial(uninomial);
			Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
			taxon = Taxon.NewInstance(name, sourceReference);
			taxon.addMarker(Marker.NewInstance(getMissingGUIDMarkerType(state), true));
		}else if (newRank != null){
			taxon.getName().setRank(newRank);
		}
		return taxon;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, IndexFungorumImportState state) {
		String nameSpace;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			//taxon map
			nameSpace = IndexFungorumImportBase.NAMESPACE_SUPRAGENERIC_NAMES ;
			Map<String, TaxonBase<?>> taxonMap = new HashMap<>();
			@SuppressWarnings("unchecked")
            List<Taxon> list = getCommonService().getHqlResult("SELECT t FROM Taxon t JOIN t.sources s WHERE s.citation.uuid = ?0", new Object[]{ PesiTransformer.uuidSourceRefIndexFungorum});
			for (Taxon taxon : list){
			    String uninomial = CdmBase.deproxy(taxon.getName()).getGenusOrUninomial();
				TaxonBase<?> existing = taxonMap.put(uninomial, taxon);
				if (existing != null){
				    logger.warn("There seem to be duplicate taxa for uninomial: " + uninomial);
				}
			}
			result.put(nameSpace, taxonMap);

			//source reference
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
		return ! state.getConfig().isDoRelTaxa();
	}
}
