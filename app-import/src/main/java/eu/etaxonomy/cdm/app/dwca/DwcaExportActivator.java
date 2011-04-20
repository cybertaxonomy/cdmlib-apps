/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.dwca;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.DefaultProgressMonitor;
import eu.etaxonomy.cdm.common.IProgressMonitor;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.database.update.CdmUpdater;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.IExportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IExportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.dwca.out.DwcaTaxExportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 *
 * @author a.mueller
 *
 */
public class DwcaExportActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(DwcaExportActivator.class);

	//database validation status (create, update, validate ...)
	private static final String fileDestination = "C:\\tmp\\dwcaTmp";
	private static final ICdmDataSource cdmSource = CdmDestinations.cdm_test_local_mysql();
	
	//check - import
	private static final CHECK check = CHECK.EXPORT_WITHOUT_CHECK;

	//NomeclaturalCode
	private static final NomenclaturalCode nomenclaturalCode  = NomenclaturalCode.ICBN;

// ****************** ALL *****************************************
	
	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doTypes = false;  //Types do not exist in El_Salvador DB
	static final boolean doNameFacts = false;  //Name Facts do not exist in El_Salvador DB
	
	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = true;
	static final boolean doOccurences = false; //occurrences do not exist in Salvador

// ************************ NONE **************************************** //
	
//	//authors
//	static final boolean doAuthors = false;
//	static final boolean doAuthorTeams = false;
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
//	//names
//	static final boolean doTaxonNames = true;
//	static final boolean doTypes = false;
//	static final boolean doNameFacts = false;
//	
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = false;
//	static final boolean doFacts = false;
//	static final boolean doOccurences = false;
//	
	
	public boolean 	doExport(ICdmDataSource source){
		System.out.println("Start export to DWC-A ("+ fileDestination + ") ...");
		
//		CdmUpdater su = CdmUpdater.NewInstance();
//		IProgressMonitor monitor = DefaultProgressMonitor.NewInstance();
//		
//		try {
//			su.updateToCurrentVersion(source, monitor);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		if (true){
//			return true;
//		}
		
		//make file destination
		String destination = fileDestination;
		
		DwcaTaxExportConfigurator config = DwcaTaxExportConfigurator.NewInstance(source, destination);
		
//		config.setDoAuthors.setSecUuid(secUuid);
//		config.setDoAuthors(sourceSecId);
//		config.setDoAuthors.setNomenclaturalCode(nomenclaturalCode);

//		config.setDoAuthors(doAuthors);
//		config.setDoReferences(doReferences);
//		config.setDoTaxonNames(doTaxonNames);
//		config.setDoRelNames(doRelNames);
//		config.setDoNameFacts(doNameFacts);
//		
//		config.setDoTaxa(doTaxa);
//		config.setDoRelTaxa(doRelTaxa);
//		config.setDoFacts(doFacts);
//		config.setDoOccurrence(doOccurences);
		config.setCheck(check);
		
		// invoke import
		CdmDefaultExport<DwcaTaxExportConfigurator> bmExport = new CdmDefaultExport<DwcaTaxExportConfigurator>();
		boolean result = bmExport.invoke(config);
		
		System.out.println("End export to DWC-A ("+ fileDestination + ")..." + (result? "(successful)":"(with errors)"));
		return result;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DwcaExportActivator ex = new DwcaExportActivator();
		ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;

		ex.doExport(source);
	}
	
	
	

}
