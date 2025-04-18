/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.cyprus;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class CyprusImportConfigurator extends ExcelImportConfiguratorBase {
    private static final long serialVersionUID = 5590553979984931651L;

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	private UUID uuidCyprusReference = UUID.fromString("b5281cd3-9d5d-4ae2-8d55-b62a592ce846");

	private String cyprusReferenceTitle = "Cyprus Distributions Excel Import";

	private boolean doDistribution;
	private boolean isDoTaxa;

	private static IInputTransformer defaultTransformer = new CyprusTransformer();

	public static CyprusImportConfigurator NewInstance(URI source, ICdmDataSource destination){
		return new CyprusImportConfigurator(source, destination);
	}

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				CyprusExcelImport.class ,
				CyprusDistributionImport.class ,

		};
	}

	@Override
    public ImportStateBase getNewState() {
		return new CyprusImportState(this);
	}

	private CyprusImportConfigurator(URI source, ICdmDataSource destination) {
	   super(source, destination, defaultTransformer);
	   setNomenclaturalCode(NomenclaturalCode.ICNAFP);
	   setSource(source);
	   setDestination(destination);
	}

	@Override
    public URI getSource() {
		return super.getSource();
	}
	@Override
    public void setSource(URI source) {
		super.setSource(source);
	}

	@Override
    public Reference getSourceReference() {
		if (sourceReference == null){
			sourceReference =  ReferenceFactory.newDatabase();
			if (getSource() != null){
				sourceReference.setTitleCache(getCyprusReferenceTitle(), true);
			}
		}
		return sourceReference;
	}

	@Override
    public String getSourceNameString() {
		return getSource().toString();
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

	public void setDoDistribution(boolean doDistribution) {
		this.doDistribution = doDistribution;
	}

	public boolean isDoDistribution(){
		return this.doDistribution;
	}

	public void setDoTaxa(boolean isDoTaxa) {
		this.isDoTaxa = isDoTaxa;
	}

	public boolean isDoTaxa() {
		return isDoTaxa;
	}

}
