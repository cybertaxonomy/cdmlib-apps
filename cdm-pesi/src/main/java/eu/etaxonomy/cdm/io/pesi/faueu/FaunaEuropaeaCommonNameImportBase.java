/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faueu;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 1.07.2025
 */
public abstract class FaunaEuropaeaCommonNameImportBase
        extends CdmImportBase<FaunaEuropaeaCommonNameImportConfigurator, FaunaEuropaeaCommonNameImportState>
        implements ICdmIO<FaunaEuropaeaCommonNameImportState>, IPartitionedIO<FaunaEuropaeaCommonNameImportState> {

    private static final long serialVersionUID = -729872543287390949L;

	//NAMESPACES
	protected static final String NAMESPACE_REFERENCE = "reference";

	protected static final String SOURCE_REFERENCE = "SOURCE_REFERENCE";

	protected void makeSource(FaunaEuropaeaCommonNameImportState state, IdentifiableEntity<?> entity, Integer id, String namespace) {
		//source reference
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		//source
		String strId = (id == null ? null : String.valueOf(id));
		IdentifiableSource source = IdentifiableSource.NewInstance(OriginalSourceType.Import, strId, namespace, sourceReference, null);
		entity.addSource(source);
	}
}
