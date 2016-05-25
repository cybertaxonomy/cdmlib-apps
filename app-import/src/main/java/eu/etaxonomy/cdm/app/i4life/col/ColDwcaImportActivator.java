/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.i4life.col;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.events.LoggingIoObserver;
import eu.etaxonomy.cdm.io.dwca.in.DwcaDataImportConfiguratorBase.DatasetUse;
import eu.etaxonomy.cdm.io.dwca.in.DwcaImportConfigurator;
import eu.etaxonomy.cdm.io.dwca.in.IImportMapping.MappingType;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 10.11.2011
 */
public class ColDwcaImportActivator {
	private static final Logger logger = Logger.getLogger(ColDwcaImportActivator.class);

	//database validation status (create, update, validate ...)
//	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

	static final ImportSteps importSteps = ImportSteps.TaxaAndExtensions;
	static final UUID stateUuid = UUID.fromString("7e1da388-0039-4ba3-b0dc-b2bebbf507db");

	static final URI source = dwca_col_All();

	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_col();


	static boolean isNoQuotes = true;


	//classification
	static final UUID classificationUuid = UUID.fromString("29d4011f-a6dd-4081-beb8-559ba6b84a6b");

	//classification name
	static String classificationName = "Catalogue of Life 2015";

	//default nom code is ICZN as it allows adding publication year
	static final NomenclaturalCode defaultNomCode = NomenclaturalCode.ICZN;


	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static int partitionSize = 1000;

	//config
	static DatasetUse datasetUse = DatasetUse.ORIGINAL_SOURCE;
	static boolean scientificNameIdAsOriginalSourceId = true;
	static boolean guessNomRef = true;
	private final boolean handleAllRefsAsCitation = true;

	//validate
	static boolean validateRankConsistency = false;

	//deduplicate
	static final boolean doDeduplicate = false;

	//mapping type
	static final MappingType mappingType = MappingType.DatabaseMapping;
	static final String databaseMappingFile = "C:/Users/a.mueller/.cdmLibrary/log/colMappingAnnual";

	private void doImport(ICdmDataSource cdmDestination){

		//make Source
		DwcaImportConfigurator config= DwcaImportConfigurator.NewInstance(source, cdmDestination);
		config.addObserver(new LoggingIoObserver());
		config.setClassificationUuid(classificationUuid);
		config.setClassificationName(classificationName);
		config.setCheck(check);
		config.setDbSchemaValidation(importSteps.validation());
		config.setDoTaxonRelationships(importSteps.doTaxonRelations());
		config.setDoTaxa(importSteps.doTaxa());
		config.setDoExtensions(importSteps.doExtensions());
		config.setDoHigherRankRelationships(importSteps.doHigherTaxonRelations());
		config.setDoSplitRelationshipImport(importSteps.doSplitRelations());
		config.setDoLowerRankRelationships(importSteps.doLowerTaxonRelations());
		config.setDoSynonymRelationships(importSteps.doSynonymRelations());
		config.setKeepMappingForFurtherImports(importSteps.keepMapping());

		config.setMappingType(mappingType);
		config.setDatabaseMappingFile(databaseMappingFile);
		config.setStateUuid(stateUuid);

		config.setScientificNameIdAsOriginalSourceId(scientificNameIdAsOriginalSourceId);
		config.setValidateRankConsistency(validateRankConsistency);
		config.setDefaultPartitionSize(partitionSize);
		config.setNomenclaturalCode(defaultNomCode);
		config.setDatasetUse(datasetUse);
		config.setGuessNomenclaturalReferences(guessNomRef);
		config.setHandleAllRefsAsCitation(handleAllRefsAsCitation);
		config.setNoQuotes(isNoQuotes);

		CdmDefaultImport<DwcaImportConfigurator> myImport = new CdmDefaultImport<DwcaImportConfigurator>();


		//...
		if (true){
			System.out.println("Start import from ("+ source.toString() + ") ...");
			config.setSourceReference(getSourceReference(config.getSourceReferenceTitle()));
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}



		//deduplicate
		if (doDeduplicate){
			ICdmApplicationConfiguration app = myImport.getCdmAppController();
			int count = app.getAgentService().deduplicate(Person.class, null, null);
			logger.warn("Deduplicated " + count + " persons.");
//			count = app.getAgentService().deduplicate(Team.class, null, null);
//			logger.warn("Deduplicated " + count + " teams.");
			count = app.getReferenceService().deduplicate(Reference.class, null, null);
			logger.warn("Deduplicated " + count + " references.");
		}

	}

	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}


	//CoL
	public static URI dwca_col_All() {
	    //http://www.catalogueoflife.org/DCA_Export/
//	    URI sourceUrl = URI.create("file:////BGBM-PESIHPC/CoL/archive-complete_2015_07_02.zip");
//        sourceUrl = URI.create("file:////BGBM-PESIHPC/CoL/archive-complete_2015_07_02_test.zip");
        URI sourceUrl = URI.create("file:////BGBM-PESIHPC/CoL/2015-03-18-archive-complete.zip");
//	    URI sourceUrl = URI.create("file:////Pesiimport3/col/col_20Nov2012.zip");
        return sourceUrl;
	}

	private enum ImportSteps{
	    ALL,
	    TaxaOnly,
	    ExtensionsOnly,
	    TaxaAndExtensions,
	    TaxonRelationsOnly,
	    SynonymsOnly,
	    HigherTaxaOnly,
	    LowerTaxaOnly
	    ;

	    private DbSchemaValidation validation(){
	        if (this == ALL || this == TaxaOnly || this == TaxaAndExtensions){
	            return DbSchemaValidation.CREATE;
	        }else{
	            return DbSchemaValidation.VALIDATE;
	        }
	    }

        private boolean doTaxa(){
            return (this == ALL || this == TaxaOnly || this == TaxaAndExtensions);
        }

        private boolean doExtensions(){
            return (this == ALL || this == ExtensionsOnly || this == TaxaAndExtensions);
        }

        private boolean doTaxonRelations(){
            return (this == ALL || this == TaxonRelationsOnly || doSplitRelations());
        }

        private boolean doSplitRelations(){
            return (this == SynonymsOnly || this == HigherTaxaOnly || this == LowerTaxaOnly);
        }

        private boolean doSynonymRelations(){
            return (this == SynonymsOnly);
        }

        private boolean doHigherTaxonRelations(){
            return (this == HigherTaxaOnly);
        }

        private boolean doLowerTaxonRelations(){
            return (this == LowerTaxaOnly);
        }

        private boolean keepMapping(){
            return !(this == ALL || this == TaxonRelationsOnly);
        }

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ColDwcaImportActivator me = new ColDwcaImportActivator();
		me.doImport(cdmDestination);
	}

}
