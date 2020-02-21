/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.caryo;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class CaryoImportConfigurator
        extends DbImportConfiguratorBase<CaryoImportState>{

    private static final long serialVersionUID = 9002177401082394179L;
    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(CaryoImportConfigurator.class);

	private boolean isDoTaxa;

	private static IInputTransformer defaultTransformer = new CaryoTransformer();

	public static CaryoImportConfigurator NewInstance(Source source, ICdmDataSource destination){
		return new CaryoImportConfigurator(source, destination);
	}

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				CaryoTaxonImport.class ,
		};
	}

	@SuppressWarnings("unchecked")
    @Override
    public CaryoImportState getNewState() {
		return new CaryoImportState(this);
	}

	private CaryoImportConfigurator(Source source, ICdmDataSource destination) {
	   super(source, destination, NomenclaturalCode.ICNAFP, defaultTransformer);
	}

	public boolean isDoTaxa() {
		return this.isDoTaxa;
	}

	public void setDoTaxa(boolean isDoTaxa) {
		this.isDoTaxa = isDoTaxa;
	}
}
