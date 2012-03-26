/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.vibrant;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.BerlinModelSources;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.Source;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen 
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 * 
 * 
 * @author a.mueller
 *
 */
public class VibrantActivator {
	private static final Logger logger = Logger.getLogger(VibrantActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source iopiSource = BerlinModelSources.iopi();
	static final Source mclSource = BerlinModelSources.mcl();
	static final Source emSource = BerlinModelSources.PESI3_euroMed();
	
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
	static final ICdmDataSource cdmDestination = cdm_test_local_vibrant();

	static final boolean doMcl = false;
	static final boolean doEuroMed = true;
	static final boolean doIopi = false;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ICdmDataSource cdmRepository = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		
		if (doMcl){
			MclActivator mclActivator = new MclActivator();
			mclActivator.importMcl(mclSource, cdmRepository, hbm2dll);
			hbm2dll = DbSchemaValidation.NONE;
		}
		
		if (doEuroMed){
			EuroMedActivator emActivator = new EuroMedActivator();
			emActivator.importEm2CDM(emSource, cdmRepository, hbm2dll);
			hbm2dll = DbSchemaValidation.NONE;
		}
		
		if (doIopi){
			IopiActivator iopiActivator = new IopiActivator();
			iopiActivator.importIopi(iopiSource, cdmRepository, hbm2dll);
			hbm2dll = DbSchemaValidation.NONE;
		}
		
		if (doEuroMed){
			logger.warn("DON'T FORGET to reset E+M filter");
			System.out.println("DON'T FORGET to reset E+M filter");
		}

	}
	
	public static ICdmDataSource cdm_test_local_vibrant(){
		DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
		String cdmServer = "127.0.0.1";
		String cdmDB = "vibrant"; 
		String cdmUserName = "root";
		return CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
	}

}
