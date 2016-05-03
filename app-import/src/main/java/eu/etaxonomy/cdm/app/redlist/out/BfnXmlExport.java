/**
 * Copyright (C) 2008 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 */

package eu.etaxonomy.cdm.app.redlist.out;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.sdd.ViolaExportActivator;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.io.redlist.bfnXml.out.BfnXmlExportConfigurator;
import eu.etaxonomy.cdm.io.redlist.bfnXml.out.BfnXmlExportTransformer;

/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public class BfnXmlExport {

	private static final Logger logger = Logger.getLogger(ViolaExportActivator.class);

    protected Document document;

	private void invokeExport() {

		ICdmDataSource sourceDb = CdmDestinations.cdm_test_local_mysql();
		BfnXmlExportTransformer transformer = new BfnXmlExportTransformer();
		File destination = new File("/home/pplitzner/Rote Listen 2020/doctronic/export/export.xml");
        document = createDocument();
		BfnXmlExportConfigurator config = BfnXmlExportConfigurator.NewInstance(destination, sourceDb, transformer, document);

		CdmDefaultExport<BfnXmlExportConfigurator> export = new CdmDefaultExport<BfnXmlExportConfigurator>();

		// invoke export
		logger.debug("Invoking BfnXml export");
		export.invoke(config);

		logger.debug("Writing XML file");
        outputXML(destination);

	}

    private void outputXML(File file) {
        XMLOutputter xml = new XMLOutputter();
        xml.setFormat(Format.getPrettyFormat());
        try {
            xml.output(document, new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Document createDocument(){
        Document document = new Document();// create root element
        Element rootElement = new Element(BfnXmlConstants.EL_DEB_EXPORT);
        document.setRootElement(rootElement);
        return document;
    }

	public static void main(String[] args) {
		BfnXmlExport export = new BfnXmlExport();
		export.invokeExport();
	}

}
