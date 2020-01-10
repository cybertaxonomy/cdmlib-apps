package eu.etaxonomy.cdm.io.pesi.merging;

import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class PesiMergeObject {

	private String uuidName;

	private String idInSource;

	private String nameCache;

	private boolean status;

	private String author;

	private String rank;

	private TaxonNodeDto phylum;

	private String parentString;

	private String parentRankString;


//************************ FACTORY *******************/

    public static PesiMergeObject newInstance(){
        return new PesiMergeObject();

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
}
