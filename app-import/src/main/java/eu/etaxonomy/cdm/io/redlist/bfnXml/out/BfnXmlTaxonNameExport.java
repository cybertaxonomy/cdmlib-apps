/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.model.name.TaxonNameBase;


/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
@Component
public class BfnXmlTaxonNameExport extends BfnXmlExportBase<TaxonNameBase> {

    private static final long serialVersionUID = -931703660108981011L;

    private static final Logger logger = Logger.getLogger(BfnXmlTaxonNameExport.class);

	public BfnXmlTaxonNameExport(){
		super();
	}

	@Override
    protected void doInvoke(BfnXmlExportState state){
	}

    @Override
    protected boolean doCheck(BfnXmlExportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(BfnXmlExportState state) {
        return false;
    }

}
