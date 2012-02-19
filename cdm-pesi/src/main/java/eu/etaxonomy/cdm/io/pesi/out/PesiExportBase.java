// $Id$
/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.DbExportBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.query.OrderHint;

/**
 * @author e.-m.lee
 * @date 12.02.2010
 *
 */
public abstract class PesiExportBase extends DbExportBase<PesiExportConfigurator, PesiExportState> {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(PesiExportBase.class);
	
	public PesiExportBase() {
		super();
	}
	

	protected <CLASS extends TaxonBase<?>> List<CLASS> getNextTaxonPartition(Class<CLASS> clazz, int limit, int partitionCount, List<String> propertyPath) {
		List<OrderHint> orderHints = new ArrayList<OrderHint>();
		orderHints.add(new OrderHint("id", OrderHint.SortOrder.ASCENDING ));
		List<CLASS> list = (List<CLASS>)getTaxonService().list(clazz, limit, partitionCount * limit, orderHints, propertyPath);
		
		Iterator<CLASS> it = list.iterator();
		while (it.hasNext()){
			TaxonBase<?> taxonBase = it.next();
			if (! isPesiTaxon(taxonBase)){
				it.remove();
			}
		}
		return list;
	}
	

	/**
	 * Returns the next list of pure names. If finished result will be null. If list is empty there may be result in further partitions.
	 * @param clazz
	 * @param limit
	 * @param partitionCount
	 * @return
	 */
	protected List<NonViralName<?>> getNextPureNamePartition(Class<? extends NonViralName> clazz,int limit, int partitionCount) {
		List<OrderHint> orderHints = new ArrayList<OrderHint>();
		orderHints.add(new OrderHint("id", OrderHint.SortOrder.ASCENDING ));
		List<String> propPath = Arrays.asList(new String[]{"taxonBases"});
		
		List<NonViralName<?>> list = (List)getNameService().list(clazz, limit, partitionCount * limit, orderHints, null);
		if (list.isEmpty()){
			return null;
		}
		Iterator<NonViralName<?>> it = list.iterator();
		while (it.hasNext()){
			NonViralName<?> taxonName = it.next();
			if (! isPurePesiName(taxonName)){
				it.remove();
			}
		}
		return list;
	}

	
	/**
	 * Decides if a name is not used as the name part of a PESI taxon (and therefore is
	 * exported to PESI as taxon already) but is related to a name used as a PESI taxon
	 * (e.g. as basionym, orthographic variant, etc.) and therefore should be exported
	 * to PESI as part of the name relationship.
	 * @param taxonName
	 * @return
	 */
	protected boolean isPurePesiName(TaxonNameBase<?,?> taxonName){
		if (hasPesiTaxon(taxonName)){
			return false;
		}
		
		for (NameRelationship rel :taxonName.getNameRelations()){
			TaxonNameBase<?,?> relatedName = (rel.getFromName().equals(taxonName)? rel.getToName(): rel.getFromName());
			if (hasPesiTaxon(relatedName)){
				return true;
			}
		}
		
		return false;
	}
	

	/**
	 * Decides if a given name has "PESI taxa" attached.
	 * 
	 * @see #getPesiTaxa(TaxonNameBase)
	 * @see #isPesiTaxon(TaxonBase)
	 * @param taxonName
	 * @return
	 */
	protected boolean hasPesiTaxon(TaxonNameBase<?,?> taxonName) {
		for (TaxonBase<?> taxon : taxonName.getTaxonBases()){
			if (isPesiTaxon(taxon)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns those concepts (taxon bases) for the given name that
	 * are pesi taxa.
	 * 
	 *  @see #isPesiTaxon(TaxonBase)
	 * @param name
	 * @return
	 */
	protected Set<TaxonBase<?>> getPesiTaxa(TaxonNameBase<?,?> name){
		Set<TaxonBase<?>> result = new HashSet<TaxonBase<?>>();
		for (TaxonBase<?> taxonBase : name.getTaxonBases()){
			if (isPesiTaxon(taxonBase)){
				result.add(taxonBase);
			}
		}
		return result;
	}


	/**
	 * Checks if this taxon base is a taxon that is to be exported to PESI. This is generally the case
	 * but not for taxa that are marked as "unpublish". Synonyms and misapplied names are exported if they are
	 * related at least to one accepted taxon that is also exported, except for those misapplied names 
	 * marked as misapplied names created by Euro+Med common names ({@linkplain http://dev.e-taxonomy.eu/trac/ticket/2786} ).
	 * The list of conditions may change in future.
	 * @param taxonBase
	 * @return
	 */
	protected static boolean isPesiTaxon(TaxonBase taxonBase) {
		//handle accepted taxa
		if (taxonBase.isInstanceOf(Taxon.class)){
			Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
			for (Marker marker : taxon.getMarkers()){
				if (marker.getValue() == false && marker.getMarkerType().equals(MarkerType.PUBLISH())){
					return false;
				}else if (marker.getValue() == true && marker.getMarkerType().getUuid().equals(BerlinModelTransformer.uuidMisappliedCommonName)){
					return false;
				}
				
			}
			//handle PESI accepted taxa
			if (! taxon.isMisapplication()){
				for (Marker marker : taxon.getMarkers()){
					if (marker.getValue() == false && marker.getMarkerType().equals(MarkerType.PUBLISH())){
						return false;
					}
				}
				return true;
			//handle misapplied names
			}else{
				for (Marker marker : taxon.getMarkers()){
					if (marker.getValue() == true && marker.getMarkerType().getUuid().equals(BerlinModelTransformer.uuidMisappliedCommonName)){
						return false;
					}
				}
				for (TaxonRelationship taxRel : taxon.getRelationsFromThisTaxon()){
					if (taxRel.getType().equals(TaxonRelationshipType.MISAPPLIED_NAME_FOR())){
						if (isPesiTaxon(taxRel.getToTaxon())){
							return true;
						}
					}
				}
				logger.info("Misapplied name has no accepted PESI taxon: " +  taxon.getUuid() + ", (" +  taxon.getTitleCache() + ")");
				return false;
			}
		//handle synonyms
		}else if (taxonBase.isInstanceOf(Synonym.class)){
			Synonym synonym = CdmBase.deproxy(taxonBase, Synonym.class);
			boolean hasAcceptedPesiTaxon = false;
			for (Taxon accTaxon : synonym.getAcceptedTaxa()){
				if (isPesiTaxon(accTaxon)){
					hasAcceptedPesiTaxon = true;
				}
			}
			if (!hasAcceptedPesiTaxon) {if (logger.isDebugEnabled()){logger.debug("Synonym has no accepted PESI taxon: " +  synonym.getUuid() + ", (" +  synonym.getTitleCache() + ")");}}
			return hasAcceptedPesiTaxon;
		}else {
			throw new RuntimeException("Unknown taxon base type: " + taxonBase.getClass());
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.DbExportBase#getDbIdCdmWithExceptions(eu.etaxonomy.cdm.model.common.CdmBase, eu.etaxonomy.cdm.io.common.ExportStateBase)
	 */
	protected Object getDbIdCdmWithExceptions(CdmBase cdmBase, PesiExportState state) {
		if (cdmBase.isInstanceOf(TaxonNameBase.class)){
			return ( cdmBase.getId() + state.getConfig().getNameIdStart() );
		}else{
			return super.getDbIdCdmWithExceptions(cdmBase, state);
		}
	}
	

//	protected List<TaxonBase> getNextDescriptionPartition(Class<? extends DescriptionElementBase> clazz,int limit, int partitionCount) {
//		List<DescriptionElementBase> list = getDescriptionService().listDescriptionElements(null, null, pageSize, pageNumber, propPath);
//		
//		Iterator<TaxonBase> it = list.iterator();
//		while (it.hasNext()){
//			TaxonBase<?> taxonBase = it.next();
//			if (! isPesiTaxon(taxonBase)){
//				it.remove();
//			}
//		}
//		return list;
//	}

}
