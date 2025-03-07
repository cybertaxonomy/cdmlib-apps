/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.globis;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.common.CdmImportSources;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.globis.GlobisImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.permission.User;

/**
 * @author a.mueller
 * @since 14.04.2010
 */
public class GlobisActivator {

    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source globisSource = CdmImportSources.GLOBIS_MDB_20140113_PESIIMPORT_SQLSERVER();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_test_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_globis();


	static final UUID classificationUuid = UUID.fromString("8bd27d84-fd4f-4bfa-bde0-3e6b7311b334");
	static final UUID featureTreeUuid = UUID.fromString("33cbf7a8-0c47-4d47-bd11-b7d77a38d0f6");
	//static final Object[] featureKeyList = new Integer[]{1,4,5,10,11,12,13,14, 249, 250, 251, 252, 253};

	static final String classificationName = "Globis";
	static final String sourceReferenceTitle = "Globis Informix Database";

	static final EDITOR editor = EDITOR.EDITOR_AS_EDITOR;

	static final String imageBaseUrl = "http://globis-images.insects-online.de/images/";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	static final int partitionSize = 3000;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICZN;

	static final boolean doReadMediaData = false;

// ***************** ALL ************************************************//

	//authors
	static final boolean doAuthors = true;

//	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;

	//taxa
	static final boolean doCurrentTaxa = true;
	static final boolean doSpecTaxa = true;
	static final boolean doImages = true;
	static final boolean doCommonNames = true;

//******************** NONE ***************************************//

//	//authors
//	static final boolean doAuthors = false;
//
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.NONE;
//
//	//taxa
//	static final boolean doCurrentTaxa = false;
//	static final boolean doSpecTaxa = false;
//	static final boolean doImages = false;
//	static final boolean doCommonNames = false;

	public void doImport(Source source, ICdmDataSource destination){
		System.out.println("Start import from ("+ globisSource.getDatabase() + ") ...");

		GlobisImportConfigurator config = GlobisImportConfigurator.NewInstance(source,  destination);

		config.setClassificationUuid(classificationUuid);
		config.setNomenclaturalCode(nomenclaturalCode);

		config.setDoReadMediaData(doReadMediaData);
		config.setDoReferences(doReferences);
		config.setDoAuthors(doAuthors);

		config.setDoCurrentTaxa(doCurrentTaxa);
		config.setDoSpecTaxa(doSpecTaxa);
		config.setDoImages(doImages);
		config.setDoCommonNames(doCommonNames);
		config.setImageBaseUrl(imageBaseUrl);

		config.setDbSchemaValidation(hbm2dll);
		config.setCheck(check);
		config.setRecordsPerTransaction(partitionSize);
		config.setClassificationName(classificationName);
		config.setSourceReferenceTitle(sourceReferenceTitle);
		config.setEditor(editor);

		// invoke import
		CdmDefaultImport<GlobisImportConfigurator> globisImport = new CdmDefaultImport<>();
		globisImport.invoke(config);

		if (config.isDoNewUser()){
			//single user or all

			String user = CdmUtils.readInputLine("Please insert username : ");
			String pwd = CdmUtils.readInputLine("Please insert password for user '" + CdmUtils.Nz(user) + "': ");
			ICdmRepository app = globisImport.getCdmAppController();
			app.getUserService().save(User.NewInstance(user, pwd));
		}

		if (config.getCheck().equals(CHECK.CHECK_AND_IMPORT)  || config.getCheck().equals(CHECK.IMPORT_WITHOUT_CHECK)    ){
			ICdmRepository app = globisImport.getCdmAppController();
			TaxonName obj = app.getCommonService().getSourcedObjectByIdInSource(TaxonName.class, "1000027", null);
			logger.info(obj);

//			//make feature tree
//			FeatureTree tree = TreeCreator.flatTree(featureTreeUuid, ermsImportConfigurator.getFeatureMap(), featureKeyList);
//			app = ermsImport.getCdmAppController();
//			app.getTermTreeService().saveOrUpdate(tree);
		}
		System.out.println("End import from ("+ source.getDatabase() + ")...");
	}

	public static void main(String[] args) {

		//make Globis Source
		Source source = globisSource;
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;
		GlobisActivator me = new GlobisActivator();
		me.doImport(source, destination);
	}
}
