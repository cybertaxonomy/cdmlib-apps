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
import org.jdom2.Attribute;
import org.jdom2.Element;

import eu.etaxonomy.cdm.io.common.CdmExportBase;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;

/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public abstract class BfnXmlExportBase extends CdmExportBase<BfnXmlExportConfigurator, BfnXmlExportState, IExportTransformer> {

    private static final long serialVersionUID = 1115122553345412881L;

    protected static final Logger logger = Logger.getLogger(BfnXmlExportBase.class);

	public BfnXmlExportBase() {
		super();
	}

    protected void addIwert(Element parent, String standardNameValue, String wertString) {
        Element iwert = new Element(BfnXmlConstants.EL_IWERT);
        iwert.setAttribute(new Attribute(BfnXmlConstants.ATT_STANDARDNAME, standardNameValue));
        parent.addContent(iwert);
    
        Element wert = new Element(BfnXmlConstants.EL_WERT);
        wert.addContent(wertString);
        iwert.addContent(wert);
    }
}
