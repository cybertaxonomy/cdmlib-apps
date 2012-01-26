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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

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
	

	protected List<TaxonBase> getNextTaxonPartition(Class<? extends TaxonBase> clazz,int limit, int partitionCount) {
		List<TaxonBase> list = getTaxonService().list(clazz, limit, partitionCount * limit, null, null);
		
		Iterator<TaxonBase> it = list.iterator();
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
		List<NonViralName<?>> list = (List)getNameService().list(clazz, limit, partitionCount * limit, null, null);
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
	

	protected boolean hasPesiTaxon(TaxonNameBase<?,?> taxonName) {
		for (TaxonBase<?> taxon : taxonName.getTaxonBases()){
			if (isPesiTaxon(taxon)){
				return true;
			}
		}
		return false;
	}
	
	protected Set<TaxonBase<?>> getPesiTaxa(TaxonNameBase<?,?> name){
		Set<TaxonBase<?>> result = new HashSet<TaxonBase<?>>();
		for (TaxonBase<?> taxonBase : name.getTaxonBases()){
			if (isPesiTaxon(taxonBase)){
				result.add(taxonBase);
			}
		}
		return result;
	}


	protected static boolean isPesiTaxon(TaxonBase taxonBase) {
		for (Marker marker : taxonBase.getMarkers()){
			if (marker.getValue() == false && marker.getMarkerType().equals(MarkerType.PUBLISH())){
				return false;
			}
		}
		return true;
	}
	
	protected Object getDbIdCdmWithExceptions(CdmBase cdmBase, PesiExportState state) {
		if (cdmBase.isInstanceOf(TaxonNameBase.class)){
			return ( cdmBase.getId() + state.getConfig().getNameIdStart() );
		}else{
			return super.getDbIdCdmWithExceptions(cdmBase, state);
		}
	}
}
