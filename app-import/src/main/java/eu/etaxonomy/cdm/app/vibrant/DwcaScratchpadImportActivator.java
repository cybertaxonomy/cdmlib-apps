/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.vibrant;

import java.net.URI;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.events.LoggingIoObserver;
import eu.etaxonomy.cdm.io.dwca.in.DwcaImportConfigurator;
import eu.etaxonomy.cdm.io.dwca.in.DwcaImportConfigurator.DatasetUse;
import eu.etaxonomy.cdm.io.dwca.in.IImportMapping.MappingType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 03.04.2012
 * @version 1.0
 */
public class DwcaScratchpadImportActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(DwcaScratchpadImportActivator.class);
	
	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final URI source = dwca_test_scratch_test();
	
	
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_dwca();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();

	
	//default nom code is ICZN as it allows adding publication year 
	static final NomenclaturalCode defaultNomCode = null;

	
	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	
	//config
	static DatasetUse datasetUse = DatasetUse.CLASSIFICATION;
	
	//validate
	static boolean validateRankConsistency = true;
	
	
	//taxa
	static final boolean doTaxa = true;
	
	
	
	static final MappingType mappingType = MappingType.InMemoryMapping;
	
	private void doImport(ICdmDataSource cdmDestination){
		
		//make Source
		DwcaImportConfigurator config= DwcaImportConfigurator.NewInstance(source, cdmDestination);
		config.addObserver(new LoggingIoObserver());
		config.setCheck(check);
		config.setDbSchemaValidation(hbm2dll);
		config.setMappingType(mappingType);
		config.setValidateRankConsistency(validateRankConsistency);
		config.setNomenclaturalCode(defaultNomCode);
		
		CdmDefaultImport myImport = new CdmDefaultImport();

		
		//...
		if (true){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}
		
		
	}

	private Reference<?> getSourceReference(String string) {
		Reference<?> result = ReferenceFactory.newGeneric();
		result.setTitleCache(string);
		return result;
	}

	
	//Scratchpads test
	public static URI dwca_test_scratch_test() {
		URI sourceUrl = URI.create("file:////PESIIMPORT3/vibrant/dwca/dwca_export_scratchpads_test.zip");
		return sourceUrl;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DwcaScratchpadImportActivator me = new DwcaScratchpadImportActivator();
		me.doImport(cdmDestination);
	}
	
}
