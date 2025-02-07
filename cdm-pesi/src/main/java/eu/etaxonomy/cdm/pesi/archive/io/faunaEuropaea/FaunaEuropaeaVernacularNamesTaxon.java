/**
* Copyright (C) 2007 EDIT
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

public class FaunaEuropaeaVernacularNamesTaxon {

	private UUID taxonUuid;
	private int taxonId;
	private Set<FaunaEuropaeaVernacularName> vernacularNames = new HashSet<>();

	public FaunaEuropaeaVernacularNamesTaxon(UUID currentTaxonUuid) {
		this.taxonUuid = currentTaxonUuid;
	}

	public UUID getTaxonUuid() {
		return taxonUuid;
	}

	public void setTaxonUuid(UUID taxonUuid) {
		this.taxonUuid = taxonUuid;
	}

	public int getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(int taxonId) {
		this.taxonId = taxonId;
	}

	public Set<FaunaEuropaeaVernacularName> getVernacularNames() {
		return vernacularNames;
	}

	public void setVernacularNames(Set<FaunaEuropaeaVernacularName> vernacularNames) {
		this.vernacularNames = vernacularNames;
	}

	public void addVernacularName(FaunaEuropaeaVernacularName fauEuVernacularname) {
		vernacularNames.add(fauEuVernacularname);
	}
}