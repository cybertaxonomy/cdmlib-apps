/**
* Copyright (C) 2008 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.babadshanjan
 * @since 08.05.2009
 */
public class FaunaEuropaeaImportConfigurator
            extends ImportConfiguratorBase<FaunaEuropaeaImportState, Source>{

    private static final long serialVersionUID = 3218446329444903409L;
    private static Logger logger = LogManager.getLogger();

	private static IInputTransformer defaultTransformer = null;

	private boolean doBasionyms = true;
	private boolean doTaxonomicallyIncluded = true;
	private boolean doMisappliedNames = true;
	private boolean doHeterotypicSynonyms = true;
	private boolean doHeterotypicSynonymsForBasionyms ;
	private boolean doOccurrence = true;
	private boolean doVernacularNames = true;
	private boolean doAssociatedSpecialists = true;
	private boolean doInferredSynonyms = true;

	public boolean isDoVernacularNames() {
		return doVernacularNames;
	}

	public void setDoVernacularNames(boolean doVernacularNames) {
		this.doVernacularNames = doVernacularNames;
	}

	public boolean isDoTypes() {
		return doTypes;
	}

	/* Max number of taxa to be saved with one service call */
	private int limitSave = 5000;
	private Reference auctReference;

	@Override
    @SuppressWarnings("unchecked")
	protected void makeIoClassList() {
		ioClassList = new Class[] {
				FaunaEuropaeaAuthorImport.class,
				FaunaEuropaeaUsersImport.class,
				FaunaEuropaeaTaxonNameImport.class,
				FaunaEuropaeaRelTaxonIncludeImport.class,
				FaunaEuropaeaDistributionImport.class,
				FaunaEuropaeaHeterotypicSynonymImport.class,
				FaunaEuropaeaRefImport.class,
				FaunaEuropaeaAdditionalTaxonDataImport.class,
				FaunaEuropaeaVernacularNamesImport.class
		};
	}

	public static FaunaEuropaeaImportConfigurator NewInstance(Source source, ICdmDataSource destination){
		return new FaunaEuropaeaImportConfigurator(source, destination);
	}

	private FaunaEuropaeaImportConfigurator(Source source, ICdmDataSource destination) {
		super(defaultTransformer);
		setSource(source);
		setDestination(destination);
		setNomenclaturalCode(NomenclaturalCode.ICZN);
	}

//	public static FaunaEuropaeaImportConfigurator NewInstance(ICdmDataSource source, ICdmDataSource destination){
//		return new FaunaEuropaeaImportConfigurator(source, destination);
//}

//	private FaunaEuropaeaImportConfigurator(ICdmDataSource source, ICdmDataSource destination) {
//		super(defaultTransformer);
//		setSource(source);
//		setDestination(destination);
//		setNomenclaturalCode(NomenclaturalCode.ICBN);
//	}


	@Override
	public Reference getSourceReference() {
		//TODO
		if (this.sourceReference == null){
			logger.warn("getSource Reference not yet fully implemented");

			sourceReference = ReferenceFactory.newDatabase();

			sourceReference.setTitleCache("Fauna Europaea database", true);
			if (this.getSourceRefUuid() != null){
				sourceReference.setUuid(this.getSourceRefUuid());
			}
		}
		return sourceReference;
	}

	public Reference getAuctReference() {
		//TODO
		if (auctReference == null){
			auctReference = ReferenceFactory.newPersonalCommunication();

			auctReference.setTitleCache("auct.", true);
			auctReference.setUuid(PesiTransformer.uuidSourceRefAuct);
		}
		return auctReference;
	}

	@Override
    public String getSourceNameString() {
		if (this.getSource() == null) {
			return null;
		}else{
			return this.getSource().toString();
		}
	}

	@SuppressWarnings("unchecked")
    @Override
    public FaunaEuropaeaImportState getNewState() {
		return new FaunaEuropaeaImportState(this);
	}

	public boolean isDoBasionyms() {
		return doBasionyms;
	}
	public void setDoBasionyms(boolean doBasionyms) {
		this.doBasionyms = doBasionyms;
	}

	public boolean isDoTaxonomicallyIncluded() {
		return doTaxonomicallyIncluded;
	}
	public void setDoTaxonomicallyIncluded(boolean doTaxonomicallyIncluded) {
		this.doTaxonomicallyIncluded = doTaxonomicallyIncluded;
	}

	public boolean isDoMisappliedNames() {
		return doMisappliedNames;
	}
	public void setDoMisappliedNames(boolean doMisappliedNames) {
		this.doMisappliedNames = doMisappliedNames;
	}

	public boolean isDoHeterotypicSynonyms() {
		return doHeterotypicSynonyms;
	}
	public void setDoHeterotypicSynonyms(boolean doHeterotypicSynonyms) {
		this.doHeterotypicSynonyms = doHeterotypicSynonyms;
	}

	public void setAuctReference(Reference auctReference) {
		this.auctReference = auctReference;
	}

	public int getLimitSave() {
		return limitSave;
	}
	public void setLimitSave(int limitSave) {
		this.limitSave = limitSave;
	}

	public void setDoHeterotypicSynonymsForBasionyms(
			boolean doHeterotypicSynonymsForBasionyms) {
		this.doHeterotypicSynonymsForBasionyms = doHeterotypicSynonymsForBasionyms;
	}
	public boolean isDoHeterotypicSynonymsForBasionyms() {
		return doHeterotypicSynonymsForBasionyms;
	}

	public boolean isDoOccurrence() {
		return doOccurrence;
	}
	public void setDoOccurrence(boolean doOccurrence) {
		this.doOccurrence = doOccurrence;
	}


	private boolean doAuthors = true;
	//references
	private DO_REFERENCES doReferences = DO_REFERENCES.ALL;
	//names
	private final boolean doTypes = true;

	//taxa
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;

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

	public boolean isDoAssociatedSpecialists() {

		return this.doAssociatedSpecialists;
	}

	public void setDoAssociatedSpecialists(boolean doAssociatedSpecialists){
		this.doAssociatedSpecialists = doAssociatedSpecialists;
	}

    public boolean isDoInferredSynonyms() {
        return doInferredSynonyms;
    }
    public void setDoInferredSynonyms(boolean doInferredSynonyms){
        this.doInferredSynonyms = doInferredSynonyms;
    }
}
