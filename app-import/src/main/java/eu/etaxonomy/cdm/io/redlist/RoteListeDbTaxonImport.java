/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.IMappingImport;
import eu.etaxonomy.cdm.io.redlist.validation.RoteListeDbTaxonImportValidator;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * TODO do we still need this class?
 * @author a.mueller
 * @created 27.08.2012
 */
@Component
public class RoteListeDbTaxonImport  extends RoteListeDbImportBase<TaxonBase> implements IMappingImport<TaxonBase, RoteListeDbImportState>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(RoteListeDbTaxonImport.class);
	
	private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
	
	private Map<UUID, Taxon> higherTaxonMap;
	
	private Integer TREE_ID = null;
	
	private DbImportMapping mapping;
	
	private int modCount = 10000;
	private static final String pluralString = "taxa";
	private static final String dbTableName = "checklist";
	private static final Class cdmTargetClass = TaxonBase.class;
	private static final String strOrderBy = " ORDER BY family, genus, species ";

	public RoteListeDbTaxonImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}
	
	
	@Override
	protected String getIdQuery() {
		String strQuery = " SELECT pk FROM " + dbTableName +
						strOrderBy;
		return strQuery;
	}

	@Override
	protected DbImportMapping getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping();
			
// 			mapping.addMapper(DbImportObjectCreationMapper.NewInstance(this, "pk", TAXON_NAMESPACE)); //id + tu_status
//		
//			UUID uuidKew = RoteListeDbTransformer.uuidAcceptedKew;
//			mapping.addMapper(DbImportMarkerMapper.NewInstance("accepted kew", uuidKew, "Accepted Kew", "Accepted Kew", "Kew", null));
			
		}
		
		return mapping;
	}

	@Override
	protected String getRecordQuery(RoteListeDbImportConfigurator config) {
		String strSelect = " SELECT * ";
		String strFrom = " FROM checklist";
		String strWhere = " WHERE ( pk IN (" + ID_LIST_TOKEN + ") )";
		String strRecordQuery = strSelect + strFrom + strWhere + strOrderBy;
		return strRecordQuery;
	}

	
	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, RoteListeDbImportState state) {
		boolean success = super.doPartition(partitioner, state);
//		higherTaxonMap = new HashMap<UUID, Taxon>();
//		state.setGenevaReference(null);
		return success;
	}


	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, RoteListeDbImportState state) {
		String nameSpace;
		Class<?> cdmClass;
		Set<String> idSet;
		Set<String> referenceIdSet = new HashSet<String>();
		
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			while (rs.next()){
				handleForeignKey(rs, referenceIdSet, "source");
			}

//			//reference map
//			nameSpace = REFERENCE_NAMESPACE;
//			cdmClass = Reference.class;
//			idSet = referenceIdSet;
//			Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, referenceIdSet, nameSpace);
//			result.put(REFERENCE_NAMESPACE, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	

	@Override
	public TaxonBase createObject(ResultSet rs, RoteListeDbImportState state) throws SQLException {
		BotanicalName speciesName = BotanicalName.NewInstance(Rank.SPECIES());
		
//		Reference sec = state.getConfig().getSourceReference();
//		getReferenceService().saveOrUpdate(sec);
//		
//		String familyString = rs.getString("family");
//		String genusString = rs.getString("genus");
//		String speciesString = rs.getString("species");
//		String authorityString = rs.getString("authority");
//		
//		if (logger.isDebugEnabled()){
//			System.out.println(familyString + " " + genusString + " " + speciesString);
//		}
//		
//		Taxon speciesTaxon = Taxon.NewInstance(speciesName, sec);;
//		speciesName.setGenusOrUninomial(genusString);
//		speciesName.setSpecificEpithet(speciesString);
//		parser.handleAuthors(speciesName, CdmUtils.concat(" ", new String[] {"", genusString, speciesString, authorityString}), authorityString);
//		
//		//family
//		Taxon familyTaxon = null;
//		if (StringUtils.isNotBlank(familyString)){
//			familyTaxon = getHigherTaxon(state, familyString, null);
//			if (familyTaxon == null){
//				BotanicalName familyName = BotanicalName.NewInstance(Rank.FAMILY());
//				familyName.setGenusOrUninomial(familyString);
//				familyTaxon = Taxon.NewInstance(familyName, sec);
//				saveHigherTaxon(state, familyTaxon, familyString, null);
//			}
//			getTaxonService().saveOrUpdate(familyTaxon);	
//		}
//		
//		
//		//genus
//		Taxon genusTaxon = getHigherTaxon(state, familyString, genusString);
//		if (genusTaxon == null){
//			BotanicalName genusName = BotanicalName.NewInstance(Rank.GENUS());
//			genusName.setGenusOrUninomial(genusString);
//			genusTaxon = Taxon.NewInstance(genusName, sec);
//			saveHigherTaxon(state, genusTaxon, familyString, genusString);
//			if (familyTaxon != null){
//				makeTaxonomicallyIncluded(state, TREE_ID, genusTaxon, familyTaxon, null, null);
//			}
//		}
//		makeTaxonomicallyIncluded(state, TREE_ID, speciesTaxon, genusTaxon, null, null);
//		getTaxonService().saveOrUpdate(genusTaxon);
//
//		String sourceString = rs.getString("source");
//		String sourceId = rs.getString("source_id");
//		
//		Reference sourceRef = state.getRelatedObject(REFERENCE_NAMESPACE, sourceString, Reference.class);
//		speciesTaxon.addSource(sourceId, REFERENCE_NAMESPACE, sourceRef, null);
//		
//		
//		//distribution
//		handleDistribution(rs, speciesTaxon);
//		
//		return speciesTaxon;
		
		return null;
	}


	@Override
	protected boolean doCheck(RoteListeDbImportState state){
		IOValidator<RoteListeDbImportState> validator = new RoteListeDbTaxonImportValidator();
		return validator.validate(state);
	}
	
	
	@Override
	protected boolean isIgnore(RoteListeDbImportState state){
		return ! state.getConfig().isDoTaxa();
	}



}
