/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.dwca;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.events.LoggingIoObserver;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.cyprus.CyprusImportConfigurator;
import eu.etaxonomy.cdm.io.cyprus.CyprusTransformer;
import eu.etaxonomy.cdm.io.dwca.in.DwcaImportConfigurator;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 16.12.2010
 * @version 1.0
 */
public class DwcaImportActivator {
	private static final Logger logger = Logger.getLogger(DwcaImportActivator.class);
	
	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI source = dwca_test_in();

	
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_dwca();

	
	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("14d1e912-5ec2-4d10-878b-828788b70a87");
	
	//classification
	static final UUID classificationUuid = UUID.fromString("d964c855-3916-4c8d-bdb6-acf9cd736b87");
	
	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	
	//taxa
	static final boolean doTaxa = true;
	static final boolean doDeduplicate = false;
	static final boolean doDistribution = false;

	
	private void doImport(ICdmDataSource cdmDestination){
		
		//make Source
		DwcaImportConfigurator config= DwcaImportConfigurator.NewInstance(source, cdmDestination);
		config.addObserver(new LoggingIoObserver());
		config.setClassificationUuid(classificationUuid);
		config.setCheck(check);
		config.setDbSchemaValidation(hbm2dll);
		
		CdmDefaultImport myImport = new CdmDefaultImport();

		
		//...
		if (true){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}
		
		
		
		//deduplicate
		if (doDeduplicate){
			ICdmApplicationConfiguration app = myImport.getCdmAppController();
			int count = app.getAgentService().deduplicate(Person.class, null, null);
			logger.warn("Deduplicated " + count + " persons.");
//			count = app.getAgentService().deduplicate(Team.class, null, null);
//			logger.warn("Deduplicated " + count + " teams.");
			count = app.getReferenceService().deduplicate(Reference.class, null, null);
			logger.warn("Deduplicated " + count + " references.");
		}
		
	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string);
		return result;
	}

	//Dwca
	public static URI dwca_test_in() {
//		URI sourceUrl = URI.create("http://dev.e-taxonomy.eu/trac/export/14463/trunk/cdmlib/cdmlib-io/src/test/resources/eu/etaxonomy/cdm/io/dwca/in/DwcaZipToStreamConverterTest-input.zip");
		URI sourceUrl = URI.create("file:///C:/Users/pesiimport/Documents/pesi_cdmlib/cdmlib-io/src/test/resources/eu/etaxonomy/cdm/io/dwca/in/DwcaZipToStreamConverterTest-input.zip");
		return sourceUrl;
	}


	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DwcaImportActivator me = new DwcaImportActivator();
		me.doImport(cdmDestination);
	}
	
}
