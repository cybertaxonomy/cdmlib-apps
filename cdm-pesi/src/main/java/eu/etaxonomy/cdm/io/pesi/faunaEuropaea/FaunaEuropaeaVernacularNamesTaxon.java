package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.util.Set;
import java.util.UUID;

public class FaunaEuropaeaVernacularNamesTaxon {
	
	private UUID taxonUuid;
	private int taxonId;
	private Set<FaunaEuropaeaVernacularName> vernacularNames;	
	
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
