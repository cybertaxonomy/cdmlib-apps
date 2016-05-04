/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;



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
	    Document document = state.getConfig().getDocument();

	    startTransaction(true);

	    //get all classifications
	    List<Classification> classifications = getClassificationService().list(Classification.class, null, null, null, null);
	    for (Classification classification : classifications) {
            Element roteListeDaten = new Element(BfnXmlConstants.EL_ROTELISTEDATEN);
            roteListeDaten.setAttribute(new Attribute(BfnXmlConstants.ATT_INHALT, classification.getTitleCache()));
            document.getRootElement().addContent(roteListeDaten);

            //export taxonomy
            Element taxonyme = new Element(BfnXmlConstants.EL_TAXONYME);
            roteListeDaten.addContent(taxonyme);
            List<TaxonNode> childNodes = classification.getChildNodes();
            for (TaxonNode taxonNode : childNodes) {
                exportTaxon(taxonNode, taxonyme);
            }
        }

	    XMLOutputter outputter = new XMLOutputter();
	    outputter.setFormat(Format.getPrettyFormat());
	    try {
            outputter.output(document, new FileWriter(state.getConfig().getDestination()));
        } catch (IOException e) {
            logger.error("Error writing file", e);
        }

	}

    private void exportTaxon(TaxonNode taxonNode, Element parent) {
        Element taxonym = new Element(BfnXmlConstants.EL_TAXONYM);
        parent.addContent(taxonym);

        Taxon taxon = taxonNode.getTaxon();
        Element nanteil = addNanteil(BfnXmlConstants.BEREICH_WISSNAME, taxon.getTitleCache());
        taxonym.addContent(nanteil);

    }

    private Element addNanteil(String bereich, String textContent) {
        Element nanteil = new Element(BfnXmlConstants.EL_NANTEIL);
        nanteil.setAttribute(new Attribute(BfnXmlConstants.ATT_BEREICH, bereich));
        nanteil.addContent(textContent);
        return nanteil;
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
