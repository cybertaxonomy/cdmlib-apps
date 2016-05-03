/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.io.File;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.XmlExportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;

/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public class BfnXmlExportConfigurator extends XmlExportConfiguratorBase<BfnXmlExportState>{

    private static final long serialVersionUID = -74469857806841119L;

    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(BfnXmlExportConfigurator.class);

	public static BfnXmlExportConfigurator NewInstance(File destination, ICdmDataSource cdmSource, IExportTransformer transformer){
			return new BfnXmlExportConfigurator(destination, cdmSource, transformer);
	}

	private BfnXmlExportConfigurator(File destination, ICdmDataSource cdmSource, IExportTransformer transformer) {
	    super(destination, cdmSource, transformer);
	}

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
				BfnXmlTaxonNameExport.class
		};

	}

	@Override
    public BfnXmlExportState getNewState() {
		return new BfnXmlExportState(this);
	}

}
