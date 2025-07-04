package eu.etaxonomy.cdm.app.pesi.merging;

import java.util.List;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class PesiMergeObject {

    private String uuidSource;

    private String uuidName;

	private String uuidTaxon;

	private String idTaxon;

	private String idInSource;

	private String nameCache;

	private boolean isStatus;

	private String author;

	private String year;

	private String rank;

	private String nomenclaturalReference;

	private TaxonNodeDto phylum;

	private TaxonNodeDto kingdom;

	private TaxonNodeDto tclass;

	private TaxonNodeDto order;

    private TaxonNodeDto family;

	private List<TaxonNodeDto> higherClassification;

	private String parentString;

	private String parentRankString;

	private boolean isMisapplication;

	private Integer nChildren;

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

	public boolean isStatus() {
		return isStatus;
	}
    public String getStatusStr() {
        return isStatus? "accepted":"synonym";
    }
	public void setStatus(boolean status) {
		this.isStatus = status;
	}

    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
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
    public String getKingdomCache() {
        return kingdom == null? null : kingdom.getNameCache();
    }
    public void setKingdom(TaxonNodeDto kingdom) {
        this.kingdom = kingdom;
    }

    public TaxonNodeDto getPhylum() {
        return phylum;
    }
    public String getPhylumCache() {
        return phylum == null? null : phylum.getNameCache();
    }
    public void setPhylum(TaxonNodeDto phylum) {
        this.phylum = phylum;
    }

    //class and getClass are part of java core therefore use other name
    public TaxonNodeDto getTClass() {
        return tclass;
    }
    public String getClassCache() {
        return tclass == null? null : tclass.getNameCache();
    }
    public void setTClass(TaxonNodeDto tclass) {
        this.tclass = tclass;
    }

    //order
    public TaxonNodeDto getOrder() {
        return order;
    }
    public String getOrderCache() {
        return order == null? null : order.getNameCache();
    }
    public void setOrder(TaxonNodeDto order) {
        this.order = order;
    }

    //family
    public TaxonNodeDto getFamily() {
        return family;
    }
    public String getFamilyCache() {
        return family == null? null : family.getNameCache();
    }
    public void setFamily(TaxonNodeDto family) {
        this.family = family;
    }

    //classification
    public List<TaxonNodeDto> getHigherClassification() {
        return higherClassification;
    }
    public String getClassificationCache() {
        return higherClassification == null? null : classificationCache();
    }
    public void setHigherClassification(List<TaxonNodeDto> higherClassification) {
        this.higherClassification = higherClassification;
    }

    private String classificationCache() {
        String result = "";
        for (TaxonNodeDto dto : this.higherClassification){
            result = CdmUtils.concat("-", result, dto.getNameCache());
        }
        return result;
    }

    //taxon uuid
    public String getUuidTaxon() {
        return uuidTaxon;
    }
    public void setUuidTaxon(String uuidTaxon) {
        this.uuidTaxon = uuidTaxon;
    }

    //taxon id
    public String getIdTaxon() {
        return idTaxon;
    }
    public void setIdTaxon(String idTaxon) {
        this.idTaxon = idTaxon;
    }

    //source uuid
    public String getUuidSource() {
        return uuidSource;
    }
    public void setUuidSource(String uuidSource) {
        this.uuidSource = uuidSource;
    }

    //nom.ref.
    public String getNomenclaturalReference() {
        return nomenclaturalReference;
    }
    public void setNomenclaturalReference(String nomenclaturalReference) {
        this.nomenclaturalReference = nomenclaturalReference;
    }

    public boolean isMisapplication() {
        return isMisapplication;
    }

    public void setMisapplication(boolean isMisapplication) {
        this.isMisapplication = isMisapplication;
    }

    public String getnChildren() {
        return nChildren == null ? "" :String.valueOf(nChildren);
    }

    public void setnChildren(Integer nChildren) {
        this.nChildren = nChildren;
    }

    @Override
    public String toString() {
        return "PesiMergeObject [uuidSource=" + uuidSource + ", uuidName=" + uuidName + ", uuidTaxon=" + uuidTaxon
                + ", nameCache=" + nameCache + ", author=" + author + (isMisapplication? ", MAN": "") + "]";
    }

}