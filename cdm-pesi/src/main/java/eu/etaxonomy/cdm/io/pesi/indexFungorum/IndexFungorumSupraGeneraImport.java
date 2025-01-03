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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 27.02.2012
 */
@Component
public class IndexFungorumSupraGeneraImport  extends IndexFungorumImportBase {

    private static final long serialVersionUID = -8504227175493151403L;
    private static Logger logger = LogManager.getLogger();

	private static final String pluralString = "Supragenera";
	private static final String dbTableName = "tblSupragenericNames";

	private static final String SUPRAGENERIC_NAMES = "Suprageneric names";
	private static final String COL_RECORD_NUMBER = "RECORD NUMBER";

	public IndexFungorumSupraGeneraImport(){
		super(pluralString, dbTableName);
	}

	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM [tblSupragenericNames] " +
			"";
		return strRecordQuery;
	}

	@Override
	protected void doInvoke(IndexFungorumImportState state) {

	    logger.info("Start supra genera ...");

	    //handle source reference first
		Reference sourceReference = state.getConfig().getSourceReference();
		getReferenceService().save(sourceReference);

		//query
		String sql = getRecordQuery(state.getConfig());
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);

		//transaction and related objects
		TransactionStatus tx = startTransaction();
		state.setRelatedObjects(getRelatedObjectsForPartition(null, state));
		sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);

		try {
			while (rs.next()){

				//Don't use (created by Marc): DisplayName, NomRefCache

				Integer id = rs.getInt(COL_RECORD_NUMBER);

				String supragenericNames = rs.getString(SUPRAGENERIC_NAMES);
				Integer rankFk = rs.getInt("PESI_RankFk");

				//name
				Rank rank = state.getTransformer().getRankByKey(String.valueOf(rankFk));
				TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);
				name.setGenusOrUninomial(supragenericNames);

				//taxon
				Taxon taxon = Taxon.NewInstance(name, sourceReference);
				//author + nom.ref.
				makeAuthorAndPublication(state, rs, name);
				//source
				makeSource(state, taxon, id, NAMESPACE_SUPRAGENERIC_NAMES );
				makeSource(state, name, id, NAMESPACE_SUPRAGENERIC_NAMES );

				getTaxonService().saveOrUpdate(taxon);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			tx.setRollbackOnly();
			state.setSuccess(false);
		}
		commitTransaction(tx);
	    logger.info("End supra genera ...");
		return;
	}

	private Taxon makeTaxon(IndexFungorumImportState state, String uninomial, Rank rank) {
	    TaxonName name = TaxonNameFactory.NewBotanicalInstance(rank);
		name.setGenusOrUninomial(uninomial);
		return Taxon.NewInstance(name, state.getConfig().getSourceReference());
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, IndexFungorumImportState state) {
		HashMap<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();  //not needed here

		//sourceReference
		Reference sourceReference = getReferenceService().find(PesiTransformer.uuidSourceRefIndexFungorum);
		Map<String, Reference> referenceMap = new HashMap<>();
		referenceMap.put(SOURCE_REFERENCE, sourceReference);
		result.put(NAMESPACE_REFERENCE, referenceMap);

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
