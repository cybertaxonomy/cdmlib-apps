/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.centralAfrica.checklist;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class CentralAfricaChecklistImportConfigurator
        extends DbImportConfiguratorBase<CentralAfricaChecklistImportState> {

    private static final long serialVersionUID = 3575836691256409349L;
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	private UUID uuidGenevaReference = UUID.fromString("cf3fd13d-6cad-430c-ab70-7ea841b7159f");

	private String genevaReferenceTitle = null;

	public static CentralAfricaChecklistImportConfigurator NewInstance(Source ermsSource, ICdmDataSource destination){
			return new CentralAfricaChecklistImportConfigurator(ermsSource, destination);
	}

	private boolean doVernaculars = true;
	private boolean doLinks = true;
	private boolean doNotes = true;
	private boolean doImages = true;
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	private boolean doTaxa = true;


	private static IInputTransformer defaultTransformer = new CentralAfricaChecklistTransformer();

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				//ErmsGeneralImportValidator.class
				 CentralAfricaChecklistReferenceImport.class ,
				 CentralAfricaChecklistTaxonImport.class,
				 CentralAfricaChecklistSynonymImport.class
		};
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.IImportConfigurator#getNewState()
	 */
	@Override
    public CentralAfricaChecklistImportState getNewState() {
		return new CentralAfricaChecklistImportState(this);
	}



	private CentralAfricaChecklistImportConfigurator(Source source, ICdmDataSource destination) {
	   super(source, destination,NomenclaturalCode.ICNAFP, defaultTransformer);  //default for FdAC
	}


	@Override
    public Source getSource() {
		return super.getSource();
	}
	@Override
    public void setSource(Source berlinModelSource) {
		super.setSource(berlinModelSource);
	}

	/**
	 * @param doVernaculars the doVernaculars to set
	 */
	public void setDoVernaculars(boolean doVernaculars) {
		this.doVernaculars = doVernaculars;
	}

	/**
	 * @return the doVernaculars
	 */
	public boolean isDoVernaculars() {
		return doVernaculars;
	}



	/**
	 * @param doLinks the doLinks to set
	 */
	public void setDoLinks(boolean doLinks) {
		this.doLinks = doLinks;
	}



	/**
	 * @return the doLinks
	 */
	public boolean isDoLinks() {
		return doLinks;
	}



	/**
	 * @param doNotes the doNotes to set
	 */
	public void setDoNotes(boolean doNotes) {
		this.doNotes = doNotes;
	}



	/**
	 * @return the doNotes
	 */
	public boolean isDoNotes() {
		return doNotes;
	}



	/**
	 * @param doImages the doImages to set
	 */
	public void setDoImages(boolean doImages) {
		this.doImages = doImages;
	}



	/**
	 * @return the doImages
	 */
	public boolean isDoImages() {
		return doImages;
	}


	public void setUuidGenevaReference(UUID uuidGenevaReference) {
		this.uuidGenevaReference = uuidGenevaReference;
	}


	public UUID getUuidGenevaReference() {
		return uuidGenevaReference;
	}


	public void setGenevaReferenceTitle(String genevaReferenceTitle) {
		this.genevaReferenceTitle = genevaReferenceTitle;
	}


	public String getGenevaReferenceTitle() {
		return genevaReferenceTitle;
	}


	public DO_REFERENCES getDoReferences() {
		return doReferences;
	}
	public void setDoReferences(DO_REFERENCES doReferences) {
		this.doReferences = doReferences;
	}

	public boolean isDoTaxa() {
		return doTaxa;
	}
	public void setDoTaxa(boolean doTaxa) {
		this.doTaxa = doTaxa;
	}



}
