package eu.etaxonomy.cdm.app.caryophyllales;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CacheUpdaterConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

public class CaryophyllalesCacheUpdater {

		private static final Logger logger = Logger.getLogger(CaryophyllalesCacheUpdater.class);

		//database validation status (create, update, validate ...)
		static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
		static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo();

		static final List<String> classListStrings =  Arrays.asList(new String[]{
				//IdentifiableEntity.class.getName(),
//				IdentifiableEntity.class.getName(),
				//AgentBase.class.getName(),
				//Reference.class.getName(),
				//TaxonNameBase.class.getName(),
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
//		static final boolean doTaxonNameBase = true;
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
				config = CacheUpdaterConfigurator.NewInstance(destination, classListStrings, true);

				// invoke import
				CdmDefaultImport<CacheUpdaterConfigurator> myImport = new CdmDefaultImport<>();
				result=myImport.invoke(config);
				//String successString = success ? "successful" : " with errors ";
				//System.out.println("End updating caches for "+ destination.getDatabase() + "..." +  successString);
				return result;
			} catch (ClassNotFoundException e) {
				logger.error(e);
				result.addException(e);
				return result;
			}
		}

		/**
		 * @param args
		 */
		public static void main(String[] args) {
			ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

			System.out.println("Start updating caches for "+ destination.getDatabase() + "...");
			CaryophyllalesCacheUpdater me = new CaryophyllalesCacheUpdater();
			me.doInvoke(destination);

		}

	}


