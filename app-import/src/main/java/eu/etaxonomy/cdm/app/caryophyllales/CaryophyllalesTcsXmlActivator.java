/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.caryophyllales;

import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.tcsxml.in.TcsXmlImportConfigurator;

/**
 * @author k.luther
 * @since 2014
 */
public class CaryophyllalesTcsXmlActivator {

    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
//	static final String tcsSource = TcsSources.tcsXml_cichorium();
	static final String tcsSource = TcsSources.tcsXml_nyctaginaceae();

	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();

	static final UUID treeUuid = UUID.fromString("00708000-0c97-48ac-8d33-6099ed68c625");
	static final String sourceSecId = "TestTCS";

	static final boolean includeNormalExplicit = true;

	//check - import
	static final CHECK check = CHECK.CHECK_AND_IMPORT;

	//authors
	static final boolean doMetaData = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doGetMissingNames = true;
	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;

	private void doImport(){
		System.out.println("Start import from Tcs("+ tcsSource.toString() + ") ...");

		//make Source
		URI source;
		try {
			source = new URI(tcsSource);
			ICdmDataSource destination = cdmDestination;

			TcsXmlImportConfigurator tcsImportConfigurator = TcsXmlImportConfigurator.NewInstance(source,  destination);

			tcsImportConfigurator.setClassificationUuid(treeUuid);
			tcsImportConfigurator.setSourceSecId(sourceSecId);

			tcsImportConfigurator.setDoMetaData(doMetaData);
			tcsImportConfigurator.setDoReferences(doReferences);
			tcsImportConfigurator.setDoTaxonNames(doTaxonNames);
			tcsImportConfigurator.setDoRelNames(doRelNames);

			tcsImportConfigurator.setDoTaxa(doTaxa);
			tcsImportConfigurator.setDoRelTaxa(doRelTaxa);

			tcsImportConfigurator.setCheck(check);
			tcsImportConfigurator.setDbSchemaValidation(hbm2dll);
			tcsImportConfigurator.setDoGetMissingNames(doGetMissingNames);

			// invoke import
			CdmDefaultImport<TcsXmlImportConfigurator> tcsImport = new CdmDefaultImport<>();
			//new Test().invoke(tcsImportConfigurator);
			tcsImport.invoke(tcsImportConfigurator);

//			IReferenceService refService = tcsImport.getCdmAppController().getReferenceService();
//			IBook book = ReferenceFactory.newBook();
//			book.setDatePublished(TimePeriod.NewInstance(1945).setEndDay(12).setEndMonth(4));
//			refService.saveOrUpdate((Reference)book);
//			tcsImport.getCdmAppController().close();

//			NormalExplicitTestActivator normExActivator = new NormalExplicitTestActivator();
//			normExActivator.doImport(destination, DbSchemaValidation.VALIDATE);

			logger.info("End");
			System.out.println("End import from TCS ("+ source.toString() + ")...");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CaryophyllalesTcsXmlActivator me = new CaryophyllalesTcsXmlActivator();
		me.doImport();
	}
}
