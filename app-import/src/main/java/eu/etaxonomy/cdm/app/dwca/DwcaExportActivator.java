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
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.IExportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.dwca.out.DwcaTaxExportConfigurator;


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

// ****************** ALL *****************************************
	

// ************************ NONE **************************************** //
	
	
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
