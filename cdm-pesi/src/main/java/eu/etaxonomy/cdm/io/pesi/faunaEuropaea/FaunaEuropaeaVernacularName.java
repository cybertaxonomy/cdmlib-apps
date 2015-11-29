package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.util.UUID;

public class FaunaEuropaeaVernacularName {
	
	String vernacularName;
	int languageFk;
	String languageCache;
	UUID taxonUuid;
	String guid;
	String source;
	String area = "";
	
	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getVernacularName() {
		return vernacularName;
	}

	public void setVernacularName(String vernacularName) {
		this.vernacularName = vernacularName;
	}

	public int getLanguageFk() {
		return languageFk;
	}

	public void setLanguageFk(int languageFk) {
		this.languageFk = languageFk;
	}

	public String getLanguageCache() {
		return languageCache;
	}

	public void setLanguageCache(String languageCache) {
		this.languageCache = languageCache;
	}

	public UUID getTaxonUuid() {
		return taxonUuid;
	}

	public void setTaxonUuid(UUID taxonUuid) {
		this.taxonUuid = taxonUuid;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public FaunaEuropaeaVernacularName(String vernacularName, int LanguageFK, String languageCache, UUID taxonUuid, String guid, String source){
		this.guid=guid;
		this.languageCache = languageCache;
		this.languageFk = languageFk;
		this.taxonUuid = taxonUuid;
		this.vernacularName = vernacularName;
		this.source = source;
	
	}

	public FaunaEuropaeaVernacularName() {
		
	}
	
}
