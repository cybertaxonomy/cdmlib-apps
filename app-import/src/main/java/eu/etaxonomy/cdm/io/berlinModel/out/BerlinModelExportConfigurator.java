/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.out;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbExportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.IExportConfigurator;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class BerlinModelExportConfigurator
            extends DbExportConfiguratorBase<BerlinModelExportState, IExportTransformer, Source>
            implements IExportConfigurator<BerlinModelExportState, IExportTransformer>{

    private static final long serialVersionUID = -2346209092417359348L;

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

//	TODO
	private static IExportTransformer defaultTransformer = null;

	//im-/export uses Classification for is_taxonomically_included_in relationships
    private boolean useClassification = true;

	//ID for the isHomotypic relationship in RelName, if not exist null
	private Integer isHomotypicId = null;

	private boolean doAuthors = true;
	//references
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	//names
	private boolean doTaxonNames = true;
	private boolean doRelNames = true;
	private boolean doNameFacts = true;

	//taxa
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;
	private boolean doFacts = true;
	private boolean doOccurrence = true;


	public static BerlinModelExportConfigurator NewInstance(Source berlinModelDestination, ICdmDataSource source){
			return new BerlinModelExportConfigurator(berlinModelDestination, source);
	}

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				BerlinModelAuthorExport.class
				, BerlinModelAuthorTeamExport.class
				, BerlinModelReferenceExport.class
				, BerlinModelTaxonNameExport.class
				, BerlinModelNameRelationExport.class
//				, BerlinModelNameFactsImport.class
//				, BerlinModelTypesImport.class
				, BerlinModelTaxonExport.class
				, BerlinModelTaxonRelationExport.class
				, BerlinModelClassificationExport.class
				, BerlinModelFactExport.class
//				, BerlinModelOccurrenceImport.class
		};
	}

	private BerlinModelExportConfigurator(Source destination, ICdmDataSource cdmSource) {
	   super(defaultTransformer);
	   setSource(cdmSource);
	   setDestination(destination);
//	   setState((BerlinModelExportState)getNewState());
	}

	/**
	 * @return the isHomotypicId
	 */
	public Integer getIsHomotypicId() {
		return isHomotypicId;
	}

	/**
	 * @param isHomotypicId the isHomotypicId to set
	 */
	public void setIsHomotypicId(Integer isHomotypicId) {
		this.isHomotypicId = isHomotypicId;
	}

	@Override
    public BerlinModelExportState getNewState() {
		return new BerlinModelExportState(this);
	}

	/**
	 * Import name relationships yes/no?.
	 * @return
	 */
	public boolean isDoRelNames() {
		return doRelNames;
	}
	public void setDoRelNames(boolean doRelNames) {
		this.doRelNames = doRelNames;
	}
	public boolean isDoOccurrence() {
		return doOccurrence;
	}
	public void setDoOccurrence(boolean doOccurrence) {
		this.doOccurrence = doOccurrence;
	}

	public boolean isDoFacts() {
		return doFacts;
	}
	public void setDoFacts(boolean doFacts) {
		this.doFacts = doFacts;
	}

	public boolean isDoNameFacts() {
		return doNameFacts;
	}
	public void setDoNameFacts(boolean doNameFacts) {
		this.doNameFacts = doNameFacts;
	}


	public boolean isDoAuthors() {
		return doAuthors;
	}
	public void setDoAuthors(boolean doAuthors) {
		this.doAuthors = doAuthors;
	}

	public DO_REFERENCES getDoReferences() {
		return doReferences;
	}
	public void setDoReferences(DO_REFERENCES doReferences) {
		this.doReferences = doReferences;
	}

	public boolean isDoTaxonNames() {
		return doTaxonNames;
	}
	public void setDoTaxonNames(boolean doTaxonNames) {
		this.doTaxonNames = doTaxonNames;
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

    public boolean isUseClassification() {
        return useClassification;
    }
    public void setUseClassification(boolean useClassification) {
        this.useClassification = useClassification;
    }
}