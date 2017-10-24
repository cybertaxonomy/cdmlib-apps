package eu.etaxonomy.cdm.app.caryophyllales;


import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.operation.config.DeleteNonReferencedReferencesConfigurator;


public class DeleteNonReferencedReferences {





			private static final Logger logger = Logger.getLogger(DeleteNonReferencedReferences.class);

			//database validation status (create, update, validate ...)
			static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
			static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();





			private ImportResult doInvoke(ICdmDataSource destination){
				ImportResult result = new ImportResult();

				DeleteNonReferencedReferencesConfigurator config;
				config = DeleteNonReferencedReferencesConfigurator.NewInstance(cdmDestination);

				// invoke import
				CdmDefaultImport<DeleteNonReferencedReferencesConfigurator> myImport = new CdmDefaultImport<DeleteNonReferencedReferencesConfigurator>();
				result = myImport.invoke(config);
				//String successString = success ? "successful" : " with errors ";
				//System.out.println("End updating caches for "+ destination.getDatabase() + "..." +  successString);
				return result;
			}

			/**
			 * @param args
			 */
			public static void main(String[] args) {
				ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

				System.out.println("Start deleting non referenced objects for "+ destination.getDatabase() + "...");
				DeleteNonReferencedReferences me = new DeleteNonReferencedReferences();
				me.doInvoke(destination);

			}

		}




