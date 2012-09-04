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

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.algaterra.AlgaTerraImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;


/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen 
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 * 
 * 
 * @author a.mueller
 *
 */
public class AlgaTerraActivator {
	private static final Logger logger = Logger.getLogger(AlgaTerraActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.AlgaTerra();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_algaterra_preview();
	
	static final UUID treeUuid = UUID.fromString("1f617402-78dc-4bf1-ac77-d260600a8879");
	static final int sourceSecId = 7331;
	static final UUID sourceRefUuid = UUID.fromString("7e1a2500-93a5-40c2-ba34-0213d7822379");
	
	static final UUID featureTreeUuid = UUID.fromString("a970168a-36fd-4c7c-931e-87214a965c14");
	static final Object[] featureKeyList = new Integer[]{7,201,202,203,204,205,206,207}; 
	
	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	private boolean ignoreNull = true;

	//NomeclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICBN;

// ****************** ALL *****************************************
	
	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doNameStatus = true;
	static final boolean doTypes = false;  
	static final boolean doNameFacts = false;    //do not exist in Alga Terra
	
	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = true;
	static final boolean doOccurences = false;
	static final boolean doCommonNames = false; //do not exist in Alga Terra
	
	//alga terra specific
	static final boolean doSpecimen = true;

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
//	static final boolean doTaxa = true;
//	static final boolean doRelTaxa = false;
//	static final boolean doFacts = false;
//	static final boolean doOccurences = false;
//	static final boolean doCommonNames = false;
//	
//  //alga terra specific
//	static final boolean doSpecimen = true;
	
	public void invoke(String[] args){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") ...");
		logger.debug("Start");
		//make BerlinModel Source
		Source source = berlinModelSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		
		AlgaTerraImportConfigurator config = AlgaTerraImportConfigurator.NewInstance(source,  destination);
		
		config.setClassificationUuid(treeUuid);
		config.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);

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
		config.setDoSpecimen(doSpecimen);
		
		config.setSourceRefUuid(sourceRefUuid);
		config.setIgnoreNull(ignoreNull);
		
		config.setDbSchemaValidation(hbm2dll);

		config.setCheck(check);
		
		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<BerlinModelImportConfigurator>();
		bmImport.invoke(config);

		if (doFacts && (config.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || config.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK) )   ){
			ICdmApplicationConfiguration app = bmImport.getCdmAppController();
			
			//make feature tree
			FeatureTree tree = TreeCreator.flatTree(featureTreeUuid, config.getFeatureMap(), featureKeyList);
			FeatureNode imageNode = FeatureNode.NewInstance(Feature.IMAGE());
			tree.getRoot().addChild(imageNode);
			FeatureNode distributionNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
			tree.getRoot().addChild(distributionNode, 2); 
			app.getFeatureTreeService().saveOrUpdate(tree);
		}
		
		
		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AlgaTerraActivator activator = new AlgaTerraActivator();
		activator.invoke(args);
	}

}
