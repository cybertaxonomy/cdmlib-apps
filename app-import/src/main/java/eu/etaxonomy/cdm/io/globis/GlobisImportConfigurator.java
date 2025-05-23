/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class GlobisImportConfigurator extends DbImportConfiguratorBase<GlobisImportState> implements IImportConfigurator{

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	public static GlobisImportConfigurator NewInstance(Source ermsSource, ICdmDataSource destination){
			return new GlobisImportConfigurator(ermsSource, destination);
	}

	/* Max number of records to be saved with one service call */
	private int recordsPerTransaction = 2000;  //defaultValue

	//TODO needed ??
	private Method userTransformationMethod;


	private boolean doAuthors = true;
	private boolean doImages = true;
	private boolean doCurrentTaxa = true;
	private boolean doSpecTaxa = true;
	private boolean doCommonNames = true;

	private boolean doReadMediaData = true;

	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;

	private String imageBaseUrl = "http://globis-images.insects-online.de/images/";

	private static IInputTransformer defaultTransformer = new GlobisTransformer();
	public boolean isDoNewUser() {
		return doNewUser;
	}


	public void setDoNewUser(boolean doNewUser) {
		this.doNewUser = doNewUser;
	}

	private boolean doNewUser = true;

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				 GlobisAuthorImport.class,
				 GlobisReferenceImport.class
	//			, ErmsReferenceImport.class
				, GlobisCurrentSpeciesImport.class
				, GlobisSpecTaxImport.class
				, GlobisCommonNameImport.class
				, GlobisImageImport.class
		};
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.IImportConfigurator#getNewState()
	 */
	@Override
    public GlobisImportState getNewState() {
		return new GlobisImportState(this);
	}



	private GlobisImportConfigurator(Source source, ICdmDataSource destination) {
	   super(source, destination, NomenclaturalCode.ICZN, defaultTransformer);//default for Globis
	}

	/**
	 * @return the limitSave
	 */
	@Override
    public int getRecordsPerTransaction() {
		return recordsPerTransaction;
	}
	/**
	 * @param limitSave the limitSave to set
	 */
	@Override
    public void setRecordsPerTransaction(int recordsPerTransaction) {
		this.recordsPerTransaction = recordsPerTransaction;
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

	public DO_REFERENCES getDoReferences() {
		return doReferences;
	}
	public void setDoReferences(DO_REFERENCES doReferences) {
		this.doReferences = doReferences;
	}


	public void setDoCurrentTaxa(boolean doCurrentTaxa) {
		this.doCurrentTaxa = doCurrentTaxa;
	}


	public boolean isDoCurrentTaxa() {
		return doCurrentTaxa;
	}


	public void setDoSpecTaxa(boolean doSpecTaxa) {
		this.doSpecTaxa = doSpecTaxa;
	}


	public boolean isDoSpecTaxa() {
		return doSpecTaxa;
	}


	public void setDoCommonNames(boolean doCommonNames) {
		this.doCommonNames = doCommonNames;
	}


	public boolean isDoCommonNames() {
		return doCommonNames;
	}


	public void setImageBaseUrl(String imageBaseUrl) {
		this.imageBaseUrl = imageBaseUrl;
	}


	public String getImageBaseUrl() {
		return imageBaseUrl;
	}


	public void setDoReadMediaData(boolean doReadMediaData) {
		this.doReadMediaData = doReadMediaData;
	}


	public boolean isDoReadMediaData() {
		return doReadMediaData;
	}


	public boolean isDoAuthors() {
		return doAuthors;
	}


	public void setDoAuthors(boolean doAuthors) {
		this.doAuthors = doAuthors;
	}

}
