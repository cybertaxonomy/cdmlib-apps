/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.berlinModelImport;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 * @author a.mueller
 */
public class MTMooseActivator {

	private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.MT_MOOSE();

//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_moose_production();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();

	static final UUID classificationUuid = UUID.fromString("601d8a00-cffe-4509-af93-b15b543ccf8d");
	static final UUID sourceRefUuid = UUID.fromString("601d8a00-cffe-4509-af93-b15b543ccf8d");

//	static final UUID featureTreeUuid = UUID.fromString("4c5b5bbe-6fef-4607-96b2-1b0104eac19e");
//	static final Object[] featureKeyList = new Integer[]{7,201,202,203,204,205,206,207};

	static final boolean includeFlatClassifications = true;

	static final String relPTaxonIdQuery = "SELECT * FROM RelPTaxon r " +
			" WHERE NOT (r.PTRefFk1 <> r.PTRefFk2 AND r.RelQualifierFk = 1)";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICNAFP;

	static final EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

// ****************** ALL *****************************************

	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doNameStatus = true;
	static final boolean doTypes = true;
	static final boolean doNameFacts = false;   //no name facts exist

	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = false;    //no facts exist
	static final boolean doOccurences = false;   //no occurrences exist
	static final boolean doCommonNames = false;  //no common names exist

// ************************ NONE **************************************** //

//	//authors
//	static final boolean doAuthors = false;
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//	//names
//	static final boolean doTaxonNames = false;
//	static final boolean doRelNames = false;
//	static final boolean doNameStatus = false;
//	static final boolean doTypes = false;
//	static final boolean doNameFacts = false;
//
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = false;
//	static final boolean doFacts = false;
//	static final boolean doOccurences = false;
//	static final boolean doCommonNames = false;


	public void invoke(String[] args){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") ...");
		logger.debug("Start");
		//make BerlinModel Source
		Source source = berlinModelSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		BerlinModelImportConfigurator config = BerlinModelImportConfigurator.NewInstance(source,  destination);

		config.setClassificationUuid(classificationUuid);
//		bmImportConfigurator.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);
		config.setEditor(editor);
		config.setIncludeFlatClassifications(includeFlatClassifications);

		config.setDoAuthors(doAuthors);
		config.setDoReferences(doReferences);
		config.setDoTaxonNames(doTaxonNames);
		config.setDoRelNames(doRelNames);
		config.setDoNameStatus(doNameStatus);
		config.setDoTypes(doTypes);
		config.setDoNameFacts(doNameFacts);

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoFacts(doFacts);
		config.setDoOccurrence(doOccurences);
		config.setDoCommonNames(doCommonNames);
		config.setSourceRefUuid(sourceRefUuid);
		config.setRelTaxaIdQuery(relPTaxonIdQuery);

		config.setDbSchemaValidation(hbm2dll);

		config.setCheck(check);

		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<BerlinModelImportConfigurator>();
		bmImport.invoke(config);

//		if (doFacts && (config.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || config.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK) )   ){
//			ICdmRepository app = bmImport.getCdmAppController();
//
//			//make feature tree
//			FeatureTree tree = TreeCreator.flatTree(featureTreeUuid, config.getFeatureMap(), featureKeyList);
//			FeatureNode distributionNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
//			tree.getRoot().addChild(distributionNode, 0);
//			app.getTermTreeService().saveOrUpdate(tree);
//		}

		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
	}

	public static void main(String[] args) {
	    logger.warn("test");
		MTMooseActivator activator = new MTMooseActivator();
		try {
            activator.invoke(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
		System.exit(0);
	}
}