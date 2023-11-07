/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.palmae;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.ProtologueImport;
import eu.etaxonomy.cdm.io.common.DefaultImportState;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 23.06.2009
 */
public class PalmaeProtologueImportConfigurator extends	ImportConfiguratorBase<DefaultImportState, File> {

    private static final long serialVersionUID = 803256639557697105L;

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	private String urlString = null;

	//TODO
	private static IInputTransformer defaultTransformer = null;

	public static PalmaeProtologueImportConfigurator NewInstance(File source, ICdmDataSource datasource, String urlString){
		PalmaeProtologueImportConfigurator result = new PalmaeProtologueImportConfigurator();
		result.setSource(source);
		result.setDestination(datasource);
		result.setUrlString(urlString);
		return result;
	}


	private String originalSourceTaxonNamespace = "TaxonName";


	public PalmaeProtologueImportConfigurator() {
		super(defaultTransformer);
	}

	@SuppressWarnings({ "unchecked" })
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				ProtologueImport.class
		};
	}

	@Override
    public DefaultImportState getNewState() {
		return new DefaultImportState(this);
	}

	@Override
	public Reference getSourceReference() {
		//TODO
		//logger.warn("getSource Reference not yet implemented");
		Reference result = ReferenceFactory.newDatabase();
		result.setTitleCache("XXX", true);
		return result;
	}

	@Override
    public String getSourceNameString() {
		if (this.getSource() == null){
			return null;
		}else{
			return this.getSource().getName();
		}
	}

	public String getOriginalSourceTaxonNamespace() {
		return originalSourceTaxonNamespace;
	}

	public void setOriginalSourceTaxonNamespace(String originalSourceTaxonNamespace) {
		this.originalSourceTaxonNamespace = originalSourceTaxonNamespace;
	}

	public String getUrlString() {
		return urlString;
	}

	public void setUrlString(String urlString) {
		this.urlString = urlString;
	}
}