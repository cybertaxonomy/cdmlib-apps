/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author a.babadshanjan
 * @since 13.09.2009
 */
public class FaunaEuropaeaReferenceTaxon {

	private UUID taxonUuid;

	private Set<FaunaEuropaeaReference> references;

	/**
	 * @param references
	 */
	public FaunaEuropaeaReferenceTaxon() {
		this.references = new HashSet<>();
	}

	public FaunaEuropaeaReferenceTaxon(UUID taxonUuid) {
	    this();
		this.taxonUuid = taxonUuid;
	}


	public UUID getTaxonUuid() {
		return taxonUuid;
	}
	public void setTaxonUuid(UUID taxonUuid) {
		this.taxonUuid = taxonUuid;
	}

	public Set<FaunaEuropaeaReference> getReferences() {
		return references;
	}
	public void setReferences(Set<FaunaEuropaeaReference> references) {
		this.references = references;
	}

	public void addReference(FaunaEuropaeaReference fauEuReference) {
		references.add(fauEuReference);
	}
}