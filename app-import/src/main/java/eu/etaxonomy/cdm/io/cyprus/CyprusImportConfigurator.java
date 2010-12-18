/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.cyprus;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;


/**
 * @author a.mueller
 * @created 20.03.2008
 * @version 1.0
 */
public class CyprusImportConfigurator extends ExcelImportConfiguratorBase implements IImportConfigurator{
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(CyprusImportConfigurator.class);

	private UUID uuidCyprusReference = UUID.fromString("b5281cd3-9d5d-4ae2-8d55-b62a592ce846");
	
	private String cyprusReferenceTitle = null;
	
	public static CyprusImportConfigurator NewInstance(URI source, ICdmDataSource destination){
			return new CyprusImportConfigurator(source, destination);
	}

	/* Max number of records to be saved with one service call */
	private int recordsPerTransaction = 1000;  //defaultValue

	//TODO needed ??
	private Method userTransformationMethod;
	
	private boolean doVernaculars = true;
	private boolean doLinks = true;
	private boolean doNotes = true;
	private boolean doImages = true;
	
	private static IInputTransformer defaultTransformer = new CyprusTransformer();
	
	protected void makeIoClassList(){
		ioClassList = new Class[]{
				CyprusExcelImport.class ,
		};	
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.IImportConfigurator#getNewState()
	 */
	public ImportStateBase getNewState() {
		return new CyprusImportState(this);
	}



	private CyprusImportConfigurator(URI source, ICdmDataSource destination) {
	   super(source, destination);
	   setNomenclaturalCode(NomenclaturalCode.ICZN); //default for ERMS
	   setSource(source);
	   setDestination(destination);
	}
	
	
	public URI getSource() {
		return (URI)super.getSource();
	}
	public void setSource(URI source) {
		super.setSource(source);
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.tcsrdf.IImportConfigurator#getSourceReference()
	 */
	public Reference getSourceReference() {
		if (sourceReference == null){
			sourceReference =  ReferenceFactory.newDatabase();
			if (getSource() != null){
				sourceReference.setTitleCache(getSource().getFragment(), true);
			}
		}
		return sourceReference;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.IImportConfigurator#getSourceNameString()
	 */
	public String getSourceNameString() {
		return getSource().toString();
	}

	/**
	 * @return the userTransformationMethod
	 */
	public Method getUserTransformationMethod() {
		return userTransformationMethod;
	}

	/**
	 * @param userTransformationMethod the userTransformationMethod to set
	 */
	public void setUserTransformationMethod(Method userTransformationMethod) {
		this.userTransformationMethod = userTransformationMethod;
	}

	
	/**
	 * @return the limitSave
	 */
	public int getRecordsPerTransaction() {
		return recordsPerTransaction;
	}

	/**
	 * @param limitSave the limitSave to set
	 */
	public void setRecordsPerTransaction(int recordsPerTransaction) {
		this.recordsPerTransaction = recordsPerTransaction;
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


	public void setUuidCyprusReference(UUID uuidCyprusReference) {
		this.uuidCyprusReference = uuidCyprusReference;
	}


	public UUID getUuidCyprusReference() {
		return uuidCyprusReference;
	}


	public void setCyprusReferenceTitle(String cyprusReferenceTitle) {
		this.cyprusReferenceTitle = cyprusReferenceTitle;
	}


	public String getCyprusReferenceTitle() {
		return cyprusReferenceTitle;
	}
	
	

}
