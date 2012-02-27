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

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 20.02.2010
 * @version 1.0
 */
@Component
public class IndexFungorumHigherClassificationImport  extends IndexFungorumImportBase<Distribution> {
	private static final Logger logger = Logger.getLogger(IndexFungorumHigherClassificationImport.class);
	
	private static final String pluralString = "higher classifications";
	private static final String dbTableName = "tblPESIfungi-Classification";

	public IndexFungorumHigherClassificationImport(){
		super(pluralString, dbTableName, null);
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		String strRecordQuery = 
			" SELECT * " + 
			" FROM [tblPESIfungi-Classification] c" +
//			" WHERE ( dr.id IN (" + ID_LIST_TOKEN + ") )";
			" ORDER BY KingdomName, PhylumName, SubphylumName, ClassName, SubclassName, OrderName, FamilyName ";
		return strRecordQuery;
	}

	
	
	
	
	@Override
	protected void doInvoke(IndexFungorumImportState state) {
		String sql = getRecordQuery(state.getConfig());
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);
		
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
		
		try {
			while (rs.next()){
				String kingdom = rs.getString("KingdomName");
				String phylum = rs.getString("PhylumName");
				String subphylum = rs.getString("SubphylumName");
				String classname = rs.getString("ClassName");
				String subclass = rs.getString("SubclassName");
				String order = rs.getString("OrderName");
				String family = rs.getString("FamilyName");
				
				if (!family.equalsIgnoreCase(lastFamily)){
					if (!order.equalsIgnoreCase(lastOrder)){
						if (!subclass.equalsIgnoreCase(lastSubclass)){
							if (!classname.equalsIgnoreCase(lastClassname)){
								if (!subphylum.equalsIgnoreCase(lastSubphylum)){
									if (! phylum.equalsIgnoreCase(lastPhylum)){
										if (! kingdom.equalsIgnoreCase(lastKingdom)){
											taxonKingdom = makeTaxon(state, kingdom, Rank.KINGDOM());
										}
										taxonPhylum = makeTaxon(state, phylum, Rank.PHYLUM());
										getClassification(state).addParentChild(taxonKingdom, taxonPhylum, null, null);
									}
									taxonSubphylum = makeTaxon(state, subphylum, Rank.SUBPHYLUM());
									getClassification(state).addParentChild(taxonPhylum,taxonSubphylum, null, null);
								}
								taxonClass = makeTaxon(state, classname, Rank.CLASS());
								getClassification(state).addParentChild(taxonSubphylum, taxonClass, null, null);
							}
							taxonSubclass = makeTaxon(state, subclass, Rank.SUBCLASS());
							getClassification(state).addParentChild(taxonClass, taxonSubclass,null, null);
						}
						taxonOrder = makeTaxon(state, order, Rank.ORDER());
						getClassification(state).addParentChild(taxonSubclass, taxonOrder, null, null);
					}
					taxonFamily = makeTaxon(state, family, Rank.FAMILY());
					getClassification(state).addParentChild(taxonOrder, taxonFamily, null, null);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return;
		
	}


	private Classification getClassification(IndexFungorumImportState state) {
		Classification result;
		UUID classificationUuid = state.getTreeUuid(state.getConfig().getSourceReference());
		if (classificationUuid == null){
			result = makeTreeMemSave(state, state.getConfig().getSourceReference());
		} else {
			result = getClassificationService().find(classificationUuid);
		} 
		return result;
	}


	private Taxon makeTaxon(IndexFungorumImportState state, String uninomial, Rank rank) {
		NonViralName<?> name = BotanicalName.NewInstance(rank);
		name.setGenusOrUninomial(uninomial);
		return Taxon.NewInstance(name, state.getConfig().getSourceReference());
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		String nameSpace;
		Class cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> taxonIdSet = new HashSet<String>();
			Set<String> areaIdSet = new HashSet<String>();
			Set<String> sourceIdSet = new HashSet<String>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet,"tu_acctaxon" );
				handleForeignKey(rs, areaIdSet, "gu_id");
				handleForeignKey(rs, sourceIdSet, "source_id");
			}
			
			//taxon map
			nameSpace = "" ;
			cdmClass = TaxonBase.class;
			idSet = taxonIdSet;
			Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, taxonMap);
			
			//areas
//			nameSpace = ErmsAreaImport.AREA_NAMESPACE;
			cdmClass = NamedArea.class;
			idSet = areaIdSet;
			Map<String, NamedArea> areaMap = (Map<String, NamedArea>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, areaMap);
			
			//reference map
//			nameSpace = ErmsReferenceImport.REFERENCE_NAMESPACE;
			cdmClass = Reference.class;
			idSet = sourceIdSet;
			Map<String, Reference> referenceMap = (Map<String, Reference>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(IndexFungorumImportState state){
		return true;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(IndexFungorumImportState state){
		return ! state.getConfig().isDoOccurrence();
	}





}
