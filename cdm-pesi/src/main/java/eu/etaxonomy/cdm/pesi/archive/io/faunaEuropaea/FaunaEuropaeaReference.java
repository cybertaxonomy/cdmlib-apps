/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import java.util.UUID;

/**
 * @author a.babadshanjan
 * @since 13.09.2009
 */
public class FaunaEuropaeaReference {

	private UUID taxonUuid;
	//private Reference cdmReference;
//	private int taxonId;
	private int referenceId;
	private String referenceAuthor;
	private String referenceYear;
	private String referenceTitle;
	private String referenceSource;
	private String page;

	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}

	public UUID getTaxonUuid() {
		return taxonUuid;
	}
	public void setTaxonUuid(UUID taxonUuid) {
		this.taxonUuid = taxonUuid;
	}

	public int getReferenceId() {
		return referenceId;
	}
	public void setReferenceId(int referenceId) {
		this.referenceId = referenceId;
	}

	public String getReferenceYear() {
		return referenceYear;
	}
	public void setReferenceYear(String referenceYear) {
		this.referenceYear = referenceYear;
	}

	public String getReferenceTitle() {
		return referenceTitle;
	}
	public void setReferenceTitle(String referenceTitle) {
		this.referenceTitle = referenceTitle;
	}

	public String getReferenceSource() {
		return referenceSource;
	}
	public void setReferenceSource(String referenceSource) {
		this.referenceSource = referenceSource;
	}

	public String getReferenceAuthor() {
		return referenceAuthor;
	}
	public void setReferenceAuthor(String referenceAuthor) {
		this.referenceAuthor = referenceAuthor;
	}

	/*public Reference getCdmReference() {
		return cdmReference;
	}*/
	/**
	 * @param cdmReference the cdmReference to set
	 */
	/*public void setCdmReference(Reference cdmReference) {
		this.cdmReference = cdmReference;
	}*/
}