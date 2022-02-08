/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public abstract class MexicoEfloraImportBase
            extends DbImportBase<MexicoEfloraImportState, MexicoEfloraImportConfigurator>
            implements ICdmIO<MexicoEfloraImportState>, IPartitionedIO<MexicoEfloraImportState> {

    private static final long serialVersionUID = -5229728676004248450L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraImportBase.class);

	public MexicoEfloraImportBase(String tableName, String pluralString ) {
		super(tableName, pluralString);
	}

	@Override
    protected String getIdQuery(MexicoEfloraImportState state){
		String result = " SELECT " + getTableName() + "id FROM " + getTableName();
		return result;
	}

    //can be overriden
    protected String getIdInSource(MexicoEfloraImportState state, ResultSet rs) throws SQLException {
        return null;
    }


	protected Taxon getTaxon(BerlinModelImportState state, int taxonId, Map<String, TaxonBase> taxonMap, int factId) {
		TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(taxonId));

		//TODO for testing
//		if (taxonBase == null && ! state.getConfig().isDoTaxa()){
//			taxonBase = Taxon.NewInstance(TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES()), null);
//		}

		Taxon taxon;
		if ( taxonBase instanceof Taxon ) {
			taxon = (Taxon) taxonBase;
		} else if (taxonBase != null) {
			logger.warn("TaxonBase (" + taxonId + ") for Fact(Specimen) with factId " + factId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
			return null;
		} else {
			logger.warn("TaxonBase (" + taxonId + ") for Fact(Specimen) with factId " + factId + " is null.");
			return null;
		}
		return taxon;
	}


	/**
	 * 	Searches first in the detail maps then in the ref maps for a reference.
	 *  Returns the reference as soon as it finds it in one of the map, according
	 *  to the order of the map.
	 *  If nomRefDetailFk is <code>null</code> no search on detail maps is performed.
	 *  If one of the maps is <code>null</code> no search on the according map is
	 *  performed. <BR>
	 *  You may define the order of search by the order you pass the maps but
	 *  make sure to always pass the detail maps first.
	 * @param firstDetailMap
	 * @param secondDetailMap
	 * @param firstRefMap
	 * @param secondRefMap
	 * @param nomRefDetailFk
	 * @param nomRefFk
	 * @return
	 */
	protected Reference getReferenceFromMaps(
			Map<String, Reference> detailMap,
			Map<String, Reference> refMap,
			String nomRefDetailFk,
			String nomRefFk) {
		Reference ref = null;
		if (detailMap != null){
			ref = detailMap.get(nomRefDetailFk);
		}
		if (ref == null){
			ref = refMap.get(nomRefFk);
		}
		return ref;
	}

    protected Reference getSourceReference(Reference sourceReference) {
        Reference persistentSourceReference = getReferenceService().find(sourceReference.getUuid());  //just to be sure
        if (persistentSourceReference != null){
            sourceReference = persistentSourceReference;
        }
        return sourceReference;
    }

    protected static <T extends IdentifiableSource> boolean importSourceExists(ISourceable<T> sourceable, String idInSource,
            String namespace, Reference ref) {
        for (T source : sourceable.getSources()){
            if (CdmUtils.nullSafeEqual(namespace, source.getIdNamespace()) &&
                CdmUtils.nullSafeEqual(idInSource, source.getIdInSource()) &&
                CdmUtils.nullSafeEqual(ref, source.getCitation())){
                    return true;
            }
        }
        return false;
    }
}
