/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class ErmsImportConfigurator
        extends DbImportConfiguratorBase<ErmsImportState>{

    private static final long serialVersionUID = 5434106058744720246L;
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static ErmsImportConfigurator NewInstance(Source ermsSource, ICdmDataSource destination){
	    return new ErmsImportConfigurator(ermsSource, destination);
	}

	/* Max number of records to be saved with one service call */
	private int recordsPerTransaction = 1000;  //defaultValue

	//TODO needed ??
	private Method userTransformationMethod;

	private boolean doVernaculars = true;
	private boolean doLinks = true;
	private boolean doNotes = true;
	private boolean doImages = true;
	private boolean doDistributions = true;
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;
	private boolean doSourceUse = true;


	private static IInputTransformer defaultTransformer = new ErmsTransformer();

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
//				ErmsGeneralImportValidator.class,
				ErmsImportRankMap.class
				, ErmsReferenceImport.class
				, ErmsTaxonImport.class
				, ErmsTaxonRelationImport.class
				, ErmsSourceUsesImport.class
				, ErmsVernacularImport.class
				, ErmsVernacularSourcesImport.class
				, ErmsNotesImport.class
				, ErmsNotesSourcesImport.class
				, ErmsAreaImport.class
				, ErmsDistributionImport.class
				, ErmsLinkImport.class  //kann weiter hoch
				, ErmsImageImport.class
		};
	}

	@Override
    public ImportStateBase getNewState() {
		return new ErmsImportState(this);
	}

	private ErmsImportConfigurator(Source ermsSource, ICdmDataSource destination) {
	   super(ermsSource, destination, NomenclaturalCode.ICZN, defaultTransformer);//default for ERMS
	}

	@Override
    public Source getSource() {
		return super.getSource();
	}
	@Override
    public void setSource(Source berlinModelSource) {
		super.setSource(berlinModelSource);
	}

	@Override
    public Reference getSourceReference() {
		if (sourceReference == null){
			sourceReference =  ReferenceFactory.newDatabase();
			if (getSource() != null){
				sourceReference.setTitleCache(getSource().getDatabase(), true);
			}
			if (this.getSourceRefUuid() != null){
				sourceReference.setUuid(this.getSourceRefUuid());
			}
		}
		return sourceReference;
	}

	@Override
    public String getSourceNameString() {
		if (this.getSource() == null){
			return null;
		}else{
			return this.getSource().getDatabase();
		}
	}

	@Override
    public Method getUserTransformationMethod() {
		return userTransformationMethod;
	}
	@Override
    public void setUserTransformationMethod(Method userTransformationMethod) {
		this.userTransformationMethod = userTransformationMethod;
	}

	@Override
    public int getRecordsPerTransaction() {
		return recordsPerTransaction;
	}
	@Override
    public void setRecordsPerTransaction(int recordsPerTransaction) {
		this.recordsPerTransaction = recordsPerTransaction;
	}

	public void setDoVernaculars(boolean doVernaculars) {
		this.doVernaculars = doVernaculars;
	}
	public boolean isDoVernaculars() {
		return doVernaculars;
	}

	public void setDoLinks(boolean doLinks) {
		this.doLinks = doLinks;
	}
	public boolean isDoLinks() {
		return doLinks;
	}

	public void setDoNotes(boolean doNotes) {
		this.doNotes = doNotes;
	}
	public boolean isDoNotes() {
		return doNotes;
	}

	public void setDoImages(boolean doImages) {
		this.doImages = doImages;
	}
	public boolean isDoImages() {
		return doImages;
	}

	public boolean isDoDistributions() {
		return doDistributions;
	}
	public void setDoDistributions(boolean doDistributions) {
		this.doDistributions = doDistributions;
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

	public boolean isDoRelTaxa() {
		return doRelTaxa;
	}
	public void setDoRelTaxa(boolean doRelTaxa) {
		this.doRelTaxa = doRelTaxa;
	}

	public boolean isDoSourceUse() {
        return doSourceUse;
    }
    public void setDoSourceUse(boolean doSourceUse) {
        this.doSourceUse = doSourceUse;
    }
}
