/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.palmae;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmApplication;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.IReference;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 * @since 01.10.2009
 */
public class PalmaePostImportUpdater {

    private static final Logger logger = LogManager.getLogger();

	static final ICdmDataSource cdmDestination = CdmDestinations.localH2Palmae();

	private String relationships = "relationships";
	private String taxonomicAccounts = "taxonomic accounts";
	private String fossilRecord = "fossil record";

	public boolean updateMissingFeatures(ICdmDataSource dataSource) {
		try{
			int count = 0;
			UUID featureTreeUuid = PalmaeActivator.featureTreeUuid;
			ICdmApplication cdmApp = CdmApplicationController.NewInstance(dataSource, DbSchemaValidation.VALIDATE);

			TransactionStatus tx = cdmApp.startTransaction();

			@SuppressWarnings("unchecked")
            TermTree<Feature> tree = cdmApp.getTermTreeService().find(featureTreeUuid);
			TermNode<Feature> root = tree.getRoot();

			List<Feature> featureList = cdmApp.getTermService().list(Feature.class, null, null, null, null);
			for (Feature feature : featureList){
				String label = feature.getLabel();
				if (relationships.equals(label)){
					root.addChild(feature);
					count++;
				}else if(taxonomicAccounts.equals(label)){
					root.addChild(feature);
					count++;
				}else if(fossilRecord.equals(label)){
					root.addChild(feature);
					count++;
				}
			}
			cdmApp.commitTransaction(tx);
			if (count != 3){
				logger.warn("Did not find 3 additional features but " + count);
				return false;
			}
			logger.info("Feature tree updated!");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR in feature tree update");
			return false;
		}

	}

	public boolean updateNameUsage(ICdmDataSource dataSource) {
		try{
			boolean result = true;
			ICdmApplication cdmApp = CdmApplicationController.NewInstance(dataSource, DbSchemaValidation.VALIDATE);

			TransactionStatus tx = cdmApp.startTransaction();

			int page = 0;
			int count = cdmApp.getTaxonService().count(Taxon.class);
			@SuppressWarnings("rawtypes")
            List<TaxonBase> taxonList = cdmApp.getTaxonService().list(TaxonBase.class, 100000, page, null, null);
			int i = 0;

			IReference treatmentReference = cdmApp.getCommonService().getSourcedObjectByIdInSource(Reference.class, "palm_pub_ed_999999", "PublicationCitation");
			if (treatmentReference == null){
				logger.error("Treatment reference could not be found");
				result = false;
			}else{
				for (TaxonBase<?> nameUsage : taxonList){
					if ((i++ % 100) == 0){System.out.println(i);}

					try {
						//if not in treatment
						if (! isInTreatment(nameUsage, treatmentReference, false)){
							//if connected treatment taxon can be found
							Taxon acceptedTaxon = getAcceptedTreatmentTaxon(nameUsage, treatmentReference);
							if (acceptedTaxon != null){
								//add as citation and delete
								addNameUsage(acceptedTaxon, nameUsage);
								cdmApp.getTaxonService().delete(nameUsage);
							}else{
								logger.warn("Non treatment taxon has no accepted taxon in treatment: " +  nameUsage + " (" + nameUsage.getId() +")" );
							}
						}
					} catch (Exception e) {
						result = false;
						e.printStackTrace();
					}
				}
			}
			//add citation feature to feature tree
			UUID featureTreeUuid = PalmaeActivator.featureTreeUuid;
			@SuppressWarnings("unchecked")
            TermTree<Feature> tree = cdmApp.getTermTreeService().find(featureTreeUuid);
			TermNode<Feature> root = tree.getRoot();
			List<Feature> featureList = cdmApp.getTermService().list(Feature.class, null, null, null, null);
			count = 0;
			for (Feature feature : featureList){
				if (feature.equals(Feature.CITATION())){
					root.addChild(feature);
					count++;
				}
			}
			if (count != 1){
				logger.warn("Did not add exactly 1 features to the feature tree but " + count);
				result = false;
			}
			//commit
			cdmApp.commitTransaction(tx);
			logger.info("NameUsage updated!");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR in name usage update");
			return false;
		}

	}

	/**
	 * @param nameUsage
	 * @return
	 */
	private Taxon getAcceptedTreatmentTaxon(TaxonBase<?> nameUsage, IReference treatmentReference) {
		boolean hasSynonymInTreatment = false;
		TaxonName name = nameUsage.getName();
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> candidateList = name.getTaxonBases();
		for (TaxonBase<?> candidate : candidateList){
			if (candidate instanceof Taxon){
				if (isInTreatment(candidate, treatmentReference, false)){
					return (Taxon)candidate;
				}
			}else if (candidate instanceof Synonym){
				Synonym synonym = (Synonym)candidate;
				Taxon accTaxon = synonym.getAcceptedTaxon();
				if (isInTreatment(synonym, treatmentReference, true)){
					hasSynonymInTreatment = true;
				}
				if (accTaxon != null){
					if (isInTreatment(accTaxon, treatmentReference, false)){
						return accTaxon;
					}
				}
			}else{
				throw new IllegalStateException("TaxonBase should be either a Taxon or a Synonym but was " + nameUsage.getClass().getName());
			}
		}
		if (hasSynonymInTreatment){
			logger.warn("Non treatment taxon has synonym in treatment but no accepted taxon: " +  nameUsage + " (" + nameUsage.getId() +")" );
		}
		return null;
	}

	private boolean isInTreatment(TaxonBase<?> taxonBase, IReference treatmentReference, boolean silent) {
		if (taxonBase.getSec().equals(treatmentReference)){
			//treatment taxa
			if (! silent){
				if (taxonBase instanceof Taxon){
					if (((Taxon)taxonBase).getTaxonNodes().size()< 1){
						logger.warn("Taxon has treatment sec but is not in tree: " +  taxonBase + " (" + taxonBase.getId() +")" );
					}
				}else if (taxonBase instanceof Synonym){
					Synonym synonym = (Synonym)taxonBase;
					boolean hasAccTaxonInTreatment = false;
					Taxon accTaxon = synonym.getAcceptedTaxon();
					if (accTaxon != null){
						hasAccTaxonInTreatment |= isInTreatment(accTaxon, treatmentReference, false);
					}
					if (hasAccTaxonInTreatment == false){
						logger.warn("Synonym has treatment reference but has no accepted taxon in tree: " +  taxonBase + " (" + taxonBase.getId() +")" );
					}
				}else{
					throw new IllegalStateException("TaxonBase should be either Taxon or Synonym");
				}
			}
			return true;
		}else{
			//taxon not in treatment
			if (! silent){
				if (taxonBase instanceof Taxon){
					if (((Taxon)taxonBase).getTaxonNodes().size()> 0){
						logger.warn("Taxon has no treatment sec but is in tree: " +  taxonBase + " (" + taxonBase.getId() +")" );
					}
				}else if (taxonBase instanceof Synonym){
					Synonym synonym = (Synonym)taxonBase;
					boolean hasAccTaxonInTreatment = false;
					Taxon accTaxon = synonym.getAcceptedTaxon();
					if (accTaxon != null){
						hasAccTaxonInTreatment |= isInTreatment(accTaxon, treatmentReference, false);
					}
					if (hasAccTaxonInTreatment == true){
						logger.warn("Synonym has no treatment reference but has accepted taxon in treatment: " +  taxonBase + " (" + taxonBase.getId() +")" );
					}
				}else{
					throw new IllegalStateException("TaxonBase should be either Taxon or Synonym but was ");
				}
			}
			return false;
		}
	}

	private boolean addNameUsage(Taxon taxon, TaxonBase<?> nameUsageTaxon) {
		TaxonDescription myDescription = null;
		for (TaxonDescription desc : taxon.getDescriptions()){
			if (! desc.isImageGallery()){
				myDescription = desc;
				break;
			}
		}
		if (myDescription == null){
			return false;
		}
		TextData textData = TextData.NewInstance(Feature.CITATION());
		//creates text (name: reference)
		//textData.putText(nameUsageTaxon.getName().getTitleCache()+": " + nameUsageTaxon.getSec().getTitleCache(), Language.DEFAULT());
		textData.addSource(OriginalSourceType.PrimaryTaxonomicSource, null, null, nameUsageTaxon.getSec(), null, nameUsageTaxon.getName(), nameUsageTaxon.getName().getTitleCache());
		myDescription.addElement(textData);
		return true;
	}

	public static void main(String[] args) {
		PalmaePostImportUpdater updater = new PalmaePostImportUpdater();
		try {
			updater.updateMissingFeatures(cdmDestination);
			updater.updateNameUsage(cdmDestination);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR in feature tree update");
		}
	}
}
