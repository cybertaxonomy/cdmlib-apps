package eu.etaxonomy.cdm.app.pesi.merging;

import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class PesiMergeObject {

    private String uuidSource;

    private String uuidName;

	private String uuidTaxon;

	private String idTaxon;

	private String uuidTaxonNode;

	private String idInSource;

	private String nameCache;

	private boolean status;

	private String author;

	private String rank;

	private TaxonNodeDto phylum;

	private TaxonNodeDto kingdom;

	private TaxonNodeDto family;

	private String parentString;

	private String parentRankString;

//************************ FACTORY *******************/

    public static PesiMergeObject NewInstance(){
        return new PesiMergeObject();
    }

    private PesiMergeObject(){

    }

//************************* GETTER/SETTER **********************/

	public String getUuidName() {
		return uuidName;
	}
	public void setUuidName(String uuidName) {
		this.uuidName = uuidName;
	}

	public String getParentRankString() {
		return parentRankString;
	}
	public void setParentRankString(String parentRankString) {
		this.parentRankString = parentRankString;
	}

	public String getParentString() {
		return parentString;
	}
	public void setParentString(String parentString) {
		this.parentString = parentString;
	}

	public String getRank() {
		return rank;
	}
	public void setRank(String rank) {
		this.rank = rank;
	}

	public TaxonNodeDto getPhylum() {
		return phylum;
	}
	public void setPhylum(TaxonNodeDto phylum) {
		this.phylum = phylum;
	}

	public boolean isStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}

	public String getIdInSource() {
		return idInSource;
	}
	public void setIdInSource(String idInSource) {
		this.idInSource = idInSource;
	}

	public String getNameCache() {
		return nameCache;
	}
	public void setNameCache(String nameCache) {
		this.nameCache = nameCache;
	}

    public TaxonNodeDto getKingdom() {
        return kingdom;
    }
    public void setKingdom(TaxonNodeDto kingdom) {
        this.kingdom = kingdom;
    }

    public TaxonNodeDto getFamily() {
        return family;
    }
    public void setFamily(TaxonNodeDto family) {
        this.family = family;
    }

    public String getUuidTaxon() {
        return uuidTaxon;
    }

    public void setUuidTaxon(String uuidTaxon) {
        this.uuidTaxon = uuidTaxon;
    }

    public String getIdTaxon() {
        return idTaxon;
    }

    public void setIdTaxon(String idTaxon) {
        this.idTaxon = idTaxon;
    }

    public String getUuidSource() {
        return uuidSource;
    }

    public void setUuidSource(String uuidSource) {
        this.uuidSource = uuidSource;
    }
}
