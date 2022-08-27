/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.wp6;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.common.URI;
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
 * @since 20.03.2008
 */
public class CommonNameImportConfigurator extends ExcelImportConfiguratorBase implements IImportConfigurator{

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	private String referenceTitle = "Common Name Excel Import";

	private static IInputTransformer defaultTransformer = new CommonNamesTransformer();

	public static CommonNameImportConfigurator NewInstance(URI source, ICdmDataSource destination){
		return new CommonNameImportConfigurator(source, destination);
	}

	@SuppressWarnings("unchecked")
    @Override
	protected void makeIoClassList(){
		ioClassList = new Class[]{
				CommonNameExcelImport.class ,
		};
	}

	@Override
	public ImportStateBase getNewState() {
		return new CichorieaeCommonNameImportState(this);
	}

	private CommonNameImportConfigurator(URI source, ICdmDataSource destination) {
	   super(source, destination, defaultTransformer);
	   setNomenclaturalCode(NomenclaturalCode.ICNAFP);
	   setSource(source);
	   setDestination(destination);
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.ImportConfiguratorBase#getSource()
	 */
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
				sourceReference.setTitleCache(referenceTitle, true);
			}
		}
		return sourceReference;
	}

	@Override
	public String getSourceNameString() {
		return getSource().toString();
	}
}
