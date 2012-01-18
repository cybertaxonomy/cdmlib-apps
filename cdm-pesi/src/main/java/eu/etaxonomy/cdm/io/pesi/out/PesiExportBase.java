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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.DbExportBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
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
	

	protected boolean isPesiTaxon(TaxonBase taxonBase) {
		for (Marker marker : taxonBase.getMarkers()){
			if (marker.getValue() == false && marker.getMarkerType().equals(MarkerType.PUBLISH())){
				return false;
			}
		}
		return true;
	}
}
