/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.mexico;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelAuthorImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelUserImport;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelGeneralImportValidator;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * Configurator for Mexico Eflora import.
 *
 * @author a.mueller
 * @date 29.01.2022
 */
public class MexicoEfloraImportConfigurator
        extends DbImportConfiguratorBase<MexicoEfloraImportState>{

    private static final long serialVersionUID = 70300913255425256L;

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(MexicoEfloraImportConfigurator.class);

	public static MexicoEfloraImportConfigurator NewInstance(Source berlinModelSource, ICdmDataSource destination){
			return new MexicoEfloraImportConfigurator(berlinModelSource, destination);
	}

	//TODO
	private static IInputTransformer defaultTransformer = null;

	private boolean doNameStatus = true;
	private boolean doCommonNames = true;
	private boolean doOccurrence = true;
	private boolean doOccurrenceSources = true;
	private boolean doMarker = true;
	private boolean doUser = true;
	private boolean doFacts = true;
	private boolean doNameFacts = true;
	private boolean doAuthors = true;
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	private boolean doTaxonNames = true;
	private boolean doTypes = true;
	private boolean doNamedAreas = true;

	//taxa
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;


	private UUID featureTreeUuid;
	private String featureTreeTitle;

    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
			BerlinModelGeneralImportValidator.class
			, BerlinModelUserImport.class
			, BerlinModelAuthorImport.class
		};
	}

	@Override
    public MexicoEfloraImportState getNewState() {
		return new MexicoEfloraImportState(this);
	}

	protected MexicoEfloraImportConfigurator(Source berlinModelSource, ICdmDataSource destination) {
	   super(berlinModelSource, destination, NomenclaturalCode.ICNAFP, defaultTransformer); //default for Berlin Model
	}


	public boolean isDoNameStatus() {
		return doNameStatus;
	}
	public void setDoNameStatus(boolean doNameStatus) {
		this.doNameStatus = doNameStatus;
	}


	public boolean isDoCommonNames() {
		return doCommonNames;
	}


	/**
	 * @param doCommonNames
	 */
	public void setDoCommonNames(boolean doCommonNames) {
		this.doCommonNames = doCommonNames;

	}

	public boolean isDoFacts() {
		return doFacts;
	}
	public void setDoFacts(boolean doFacts) {
		this.doFacts = doFacts;
	}


	public boolean isDoOccurrence() {
		return doOccurrence;
	}
	public void setDoOccurrence(boolean doOccurrence) {
		this.doOccurrence = doOccurrence;
	}

    public boolean isDoOccurrenceSources() {
        return doOccurrenceSources;
    }
    public void setDoOccurrenceSources(boolean doOccurrenceSources) {
        this.doOccurrenceSources = doOccurrenceSources;
	}


	public boolean isDoMarker() {
		return doMarker;
	}

	public void setDoMarker(boolean doMarker) {
		this.doMarker = doMarker;
	}

	public boolean isDoUser() {
		return doUser;
	}
	public void setDoUser(boolean doUser) {
		this.doUser = doUser;
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

	public boolean isDoTypes() {
		return doTypes;
	}
	public void setDoTypes(boolean doTypes) {
		this.doTypes = doTypes;
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

   public UUID getFeatureTreeUuid() {
        return featureTreeUuid;
    }
    public void setFeatureTreeUuid(UUID featureTreeUuid) {
        this.featureTreeUuid = featureTreeUuid;
    }

    @Override
    public String getFeatureTreeTitle() {
        return featureTreeTitle;
    }
    @Override
    public void setFeatureTreeTitle(String featureTreeTitle) {
        this.featureTreeTitle = featureTreeTitle;
    }

    public boolean isDoNamedAreas() {
        return doNamedAreas;
    }
    public void setDoNamedAreas(boolean doNamedAreas) {
        this.doNamedAreas = doNamedAreas;
    }

}
