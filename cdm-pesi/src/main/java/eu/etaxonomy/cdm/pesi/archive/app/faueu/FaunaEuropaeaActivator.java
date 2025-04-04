/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.pesi.archive.app.faueu;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.PesiSources;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea.FaunaEuropaeaImportConfigurator;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
public class FaunaEuropaeaActivator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	static final Source faunaEuropaeaSource = PesiSources.mfn_faunEu_pesi3();
	static final ICdmDataSource cdmDestination = null;
	//static final ICdmDataSource cdmDestination = CdmDestinations.cdm_pesi_fauna_europaea();


	static final int limitSave = 5000;

//	static final CHECK check = CHECK.CHECK_AND_IMPORT;
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.CREATE;
//	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.UPDATE;
//	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;



	static final NomenclaturalCode nomenclaturalCode  = NomenclaturalCode.ICZN;

// ****************** ALL *****************************************

	// Fauna Europaea to CDM import
	static final boolean doAuthors = true;
	static final boolean doTaxa =true;
	static final boolean doBasionyms = true;
	static final boolean doTaxonomicallyIncluded = true;
	static final boolean doMisappliedNames = true;
	static final boolean doHeterotypicSynonyms = true;
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	static final boolean doDistributions = true;
	static final boolean makeFeatureTree = true;
	static final boolean doVernacularNames = true;
	static final boolean doAssociatedSpecialists = true;
	static final boolean addCommonNameFeature = true;
	static final boolean doInferredSynonyms = true;
    // CDM to CDM import
	static final boolean doHeterotypicSynonymsForBasionyms = true;

// ************************ NONE **************************************** //

	// Fauna Europaea to CDM import
//	static final boolean doAuthors = false;
//	static final boolean doTaxa = false;
//	static final boolean doBasionyms = false;
//	static final boolean doTaxonomicallyIncluded = false;
//	static final boolean doMisappliedNames = false;
//	static final boolean doHeterotypicSynonyms = false;
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//	static final boolean doDistributions = false;
//	static final boolean makeFeatureTree = false;
//    // CDM to CDM import
//	static final boolean doHeterotypicSynonymsForBasionyms = false;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ICdmDataSource destination = cdmDestination;
		System.out.println("Starting import from Fauna Europaea (" + faunaEuropaeaSource.getDatabase() + ") to CDM (" + destination.getDatabase() + ")...");

				// invoke Fauna Europaea to CDM import

		FaunaEuropaeaImportConfigurator fauEuImportConfigurator =
			FaunaEuropaeaImportConfigurator.NewInstance(faunaEuropaeaSource,  destination);

		fauEuImportConfigurator.setDbSchemaValidation(dbSchemaValidation);
		fauEuImportConfigurator.setNomenclaturalCode(nomenclaturalCode);
		fauEuImportConfigurator.setCheck(check);

		fauEuImportConfigurator.setDoAuthors(doAuthors);
		fauEuImportConfigurator.setDoTaxa(doTaxa);
		fauEuImportConfigurator.setDoReferences(doReferences);
		fauEuImportConfigurator.setDoOccurrence(doDistributions);
		fauEuImportConfigurator.setDoTaxonomicallyIncluded(doTaxonomicallyIncluded);
		fauEuImportConfigurator.setDoBasionyms(doBasionyms);
		fauEuImportConfigurator.setDoMisappliedNames(doMisappliedNames);
		fauEuImportConfigurator.setDoHeterotypicSynonyms(doHeterotypicSynonyms);
		fauEuImportConfigurator.setDoHeterotypicSynonymsForBasionyms(doHeterotypicSynonymsForBasionyms);
		fauEuImportConfigurator.setSourceRefUuid(PesiTransformer.uuidSourceRefFaunaEuropaea_fromSql);
		fauEuImportConfigurator.setDoAssociatedSpecialists(doAssociatedSpecialists);
		fauEuImportConfigurator.setDoVernacularNames(doVernacularNames);
		fauEuImportConfigurator.setDoInferredSynonyms(doInferredSynonyms);
		CdmDefaultImport<FaunaEuropaeaImportConfigurator> fauEuImport =
			new CdmDefaultImport<FaunaEuropaeaImportConfigurator>();
		try {
			fauEuImport.invoke(fauEuImportConfigurator);
		} catch (Exception e) {
			System.out.println("ERROR in Fauna Europaea to CDM import");
			e.printStackTrace();
		}

		// invoke CDM to CDM import

//		System.out.println("Starting import from CDM to CDM (" + destination.getDatabase() + ")...");
//
//		CdmImportConfigurator cdmImportConfigurator =
//			CdmImportConfigurator.NewInstance(destination, destination);
//
//		cdmImportConfigurator.setDbSchemaValidation(DbSchemaValidation.VALIDATE);
//		cdmImportConfigurator.setNomenclaturalCode(nomenclaturalCode);
//		cdmImportConfigurator.setCheck(check);
//
//		cdmImportConfigurator.setDoHeterotypicSynonymsForBasionyms(doHeterotypicSynonymsForBasionyms);
//		cdmImportConfigurator.setDoAuthors(false);
//		cdmImportConfigurator.setDoTaxa(false);
//		cdmImportConfigurator.setDoReferences(DO_REFERENCES.NONE);
//		cdmImportConfigurator.setDoOccurrence(false);
//		cdmImportConfigurator.setLimitSave(limitSave);
//
//		CdmDefaultImport<CdmImportConfigurator> cdmImport =
//			new CdmDefaultImport<CdmImportConfigurator>();
//		try {
//			cdmImport.invoke(cdmImportConfigurator);
//		} catch (Exception e) {
//			System.out.println("ERROR in CDM to CDM import");
//			e.printStackTrace();
//		}

		//make feature tree

		if (makeFeatureTree) {
			TermTree<Feature> featureTree = TermTree.NewFeatureInstance(UUID.fromString("ff59b9ad-1fb8-4aa4-a8ba-79d62123d0fb"));
			TermNode<Feature> root = featureTree.getRoot();

			ICdmRepository app = fauEuImport.getCdmAppController();
			Feature citationFeature = (Feature)app.getTermService().find(UUID.fromString("99b2842f-9aa7-42fa-bd5f-7285311e0101"));
			root.addChild(citationFeature);
			Feature distributionFeature = (Feature)app.getTermService().find(UUID.fromString("9fc9d10c-ba50-49ee-b174-ce83fc3f80c6"));
			root.addChild(distributionFeature);
			Feature commonNameFeature = (Feature)app.getTermService().find(UUID.fromString("fc810911-51f0-4a46-ab97-6562fe263ae5"));
			root.addChild(commonNameFeature);
			app.getTermTreeService().saveOrUpdate(featureTree);
		}

		System.out.println("End importing Fauna Europaea data");
	}

}
