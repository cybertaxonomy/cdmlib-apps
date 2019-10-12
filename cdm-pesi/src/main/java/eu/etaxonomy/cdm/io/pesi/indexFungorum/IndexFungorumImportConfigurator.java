/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class IndexFungorumImportConfigurator
        extends DbImportConfiguratorBase<IndexFungorumImportState> {

    private static final long serialVersionUID = -6354012088315750673L;
    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(IndexFungorumImportConfigurator.class);

	public static IndexFungorumImportConfigurator NewInstance(Source source, ICdmDataSource destination){
			return new IndexFungorumImportConfigurator(source, destination);
	}

	/* Max number of records to be saved with one service call */
	private int recordsPerTransaction = 1000;  //defaultValue

	private boolean doOccurrence = true;
	private boolean doTaxa = true;
	private boolean doRelTaxa = true;


	private static IInputTransformer defaultTransformer = new IndexFungorumTransformer();

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				IndexFungorumSupraGeneraImport.class
				, IndexFungorumHigherClassificationImport.class
				, IndexFungorumGeneraImport.class
				, IndexFungorumSpeciesImport.class
				, IndexFungorumSubSpeciesImport.class
				, IndexFungorumDistributionImport.class
		};
	}

	@SuppressWarnings("unchecked")
    @Override
    public IndexFungorumImportState getNewState() {
		return new IndexFungorumImportState(this);
	}

	private IndexFungorumImportConfigurator(Source source, ICdmDataSource destination) {
	   super(source, destination, NomenclaturalCode.ICNAFP, defaultTransformer);//default for IF
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
    public int getRecordsPerTransaction() {
		return recordsPerTransaction;
	}
	@Override
    public void setRecordsPerTransaction(int recordsPerTransaction) {
		this.recordsPerTransaction = recordsPerTransaction;
	}

	public boolean isDoTaxa() {
		return doTaxa;
	}
	public void setDoTaxa(boolean doTaxa) {
		this.doTaxa = doTaxa;
	}

	public boolean isDoOccurrence() {
		return doOccurrence;
	}
	public void setDoOccurrence(boolean doOccurrence) {
		this.doOccurrence = doOccurrence;
	}

	public boolean isDoRelTaxa() {
		return doRelTaxa;
	}
	public void setDoRelTaxa(boolean doRelTaxa) {
		this.doRelTaxa = doRelTaxa;
	}
}
