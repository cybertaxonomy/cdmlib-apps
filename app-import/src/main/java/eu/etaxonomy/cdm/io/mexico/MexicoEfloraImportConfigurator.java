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
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

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
	private static IInputTransformer defaultTransformer = new MexicoConabioTransformer();

	private boolean doCommonNames = true;
	private boolean doOccurrence = true;
	private boolean doOccurrenceSources = true;
	private boolean doFacts = true;
	private boolean doReferences = true;
	private boolean doNamedAreas = true;

	//taxa
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;

	private UUID featureTreeUuid;
	private UUID flatFeatureTreeUuid;

	private String featureTreeTitle;
	private String flatFeatureTreeTitle;


    private Reference secReference;

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
		        MexicoEfloraRefArticlesImport.class,
		        MexicoEfloraRefSerialsImport.class,
		        MexicoEfloraRefOtherBooksImport.class,
		        MexicoEfloraRefWebSitesImport.class,
		        MexicoEfloraRegionImport.class,
		        MexicoEfloraTaxonImport.class,
		        MexicoEfloraCommonNameImport.class,
		        MexicoEfloraCommonNameRefImport.class,
		        MexicoEfloraDistributionImport.class,
		        MexicoEfloraDistributionRefImport.class,
		        MexicoEfloraFactCategoryImport.class,
		        MexicoEfloraFactImport.class,
		        MexicoEfloraTaxonRelationImport.class,
		};
	}

	@SuppressWarnings("unchecked")
    @Override
    public MexicoEfloraImportState getNewState() {
		return new MexicoEfloraImportState(this);
	}

	protected MexicoEfloraImportConfigurator(Source berlinModelSource, ICdmDataSource destination) {
	   super(berlinModelSource, destination, NomenclaturalCode.ICNAFP, defaultTransformer); //default for Berlin Model
	}


	public boolean isDoCommonNames() {
		return doCommonNames;
	}
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

	public boolean isDoReferences() {
		return doReferences;
	}
	public void setDoReferences(boolean doReferences) {
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

   public UUID getFeatureTreeUuid() {
        return featureTreeUuid;
    }
    public void setFeatureTreeUuid(UUID featureTreeUuid) {
        this.featureTreeUuid = featureTreeUuid;
    }

    public UUID getFlatFeatureTreeUuid() {
        return flatFeatureTreeUuid;
    }
    public void setFlatFeatureTreeUuid(UUID flatFeatureTreeUuid) {
        this.flatFeatureTreeUuid = flatFeatureTreeUuid;
    }

    @Override
    public String getFeatureTreeTitle() {
        return featureTreeTitle;
    }
    @Override
    public void setFeatureTreeTitle(String featureTreeTitle) {
        this.featureTreeTitle = featureTreeTitle;
    }

    public String getFlatFeatureTreeTitle() {
        return flatFeatureTreeTitle;
    }
    public void setFlatFeatureTreeTitle(String flatFeatureTreeTitle) {
        this.flatFeatureTreeTitle = flatFeatureTreeTitle;
    }

    public boolean isDoNamedAreas() {
        return doNamedAreas;
    }
    public void setDoNamedAreas(boolean doNamedAreas) {
        this.doNamedAreas = doNamedAreas;
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

}
