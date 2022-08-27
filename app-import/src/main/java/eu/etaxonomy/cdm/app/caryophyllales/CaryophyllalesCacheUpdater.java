package eu.etaxonomy.cdm.app.caryophyllales;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.config.CacheUpdaterConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.IRemotingProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

public class CaryophyllalesCacheUpdater {

    private static final Logger logger = LogManager.getLogger();
	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_algaterranew();

	static final List<String> classListStrings =  Arrays.asList(new String[]{
			//IdentifiableEntity.class.getName(),
//				IdentifiableEntity.class.getName(),
			//AgentBase.class.getName(),
			//Reference.class.getName(),
			TaxonName.class.getName(),
			TaxonBase.class.getName()


	});
	//new ArrayList<Class<? extends IdentifiableEntity>>();

	// **************** ALL *********************

//		//DescriptionBase
//		static final boolean doTaxonDescription = true;
//		static final boolean doSpecimenDescription = true;
//		static final boolean doNameDescription = true;
	//
//		//AgentBase
//		static final boolean doPerson = true;
//		static final boolean doTeam = true;
//		static final boolean doInstitution = true;
	//
//		//MediaEntities
//		static final boolean doCollection = true;
//		static final boolean doReferenceBase = true;
	//
//		//SpecimenOrObservationBase
//		static final boolean doFieldObservation = true;
//		static final boolean doDeriveUnit = true;
//		static final boolean doLivingBeing = true;
//		static final boolean doObservation = true;
//		static final boolean doSpecimen = true;
	//
//		//Media
//		static final boolean doMedia = true;
//		static final boolean doMediaKey = true;
//		static final boolean doFigure = true;
//		static final boolean doPhylogenticTree = true;
	//
	//
//		//TaxonBase
//		static final boolean doTaxon = true;
//		static final boolean doSynonym = true;
	//
//		static final boolean doSequence = true;
	//
//		//Names
//		static final boolean doTaxonName = true;
	//
//		static final boolean doClassification = true;
	//
//		//TermBase
//		static final boolean doFeatureTree = true;
//		static final boolean doPolytomousKey = true;
	//
//		static final boolean doTermVocabulary = true;
//		static final boolean doDefinedTermBase = true;
	//


		private ImportResult doInvoke(ICdmDataSource destination){
			ImportResult result = new ImportResult();

			CacheUpdaterConfigurator config;
			try {
				config = CacheUpdaterConfigurator.NewInstance(classListStrings, true);
				CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(destination, DbSchemaValidation.VALIDATE, false);
				appCtrInit.authenticate("admin", "kups366+RU");
				UUID monitUuid = appCtrInit.getLongRunningTasksService().monitLongRunningTask(config);
				IRemotingProgressMonitor monitor = appCtrInit.getProgressMonitorService().getRemotingMonitor(monitUuid);
				while(monitor != null && (!monitor.isCanceled() || !monitor.isDone() || !monitor.isFailed())) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                logger.info("Waiting for monitered work to start ..");
	                monitor = appCtrInit.getProgressMonitorService().getRemotingMonitor(monitUuid);
				}

				return result;
			} catch (ClassNotFoundException e) {
				logger.error(e);
				result.addException(e);
				return result;
			}
		}

		public static void main(String[] args) {
			ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

			System.out.println("Start updating caches for "+ destination.getDatabase() + "...");
			CaryophyllalesCacheUpdater me = new CaryophyllalesCacheUpdater();
			me.doInvoke(destination);

		}
	}