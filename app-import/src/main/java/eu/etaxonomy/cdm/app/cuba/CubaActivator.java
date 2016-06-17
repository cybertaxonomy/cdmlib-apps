/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.cuba;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.cuba.CubaImportConfigurator;
import eu.etaxonomy.cdm.io.cuba.CubaTransformer;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @created 04.01.2016
 */
public class CubaActivator {
	private static final Logger logger = Logger.getLogger(CubaActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_cuba_production();

	static boolean invers = true;

    boolean doAsteraceae = include;
    boolean doConvolvulaceae = include;
    boolean doCyperaceae = include;
    boolean doDicotA_C = include;
    boolean doDicotD_M = include;
    boolean doDicotN_Z = include;
    boolean doEuphorbiaceae = include;
    boolean doFabaceae = include;
    boolean doGymnospermae = include;
    boolean doLamVerbenaceae = include;
    boolean doMalpighiaceae = include;
    boolean doMelastomataceae = include;
    boolean doMonocots = include;
    boolean doMyrtaceae = include;
    boolean doOrchidaceae = include;
    boolean doRubiaceae = include;
    boolean doUrticaceae = include;

    static boolean include = !invers;


	//feature tree uuid
	public static final UUID featureTreeUuid = UUID.fromString("dad6b9b5-693f-4367-a7aa-076cc9c99476");

	//classification
	static final UUID classificationUuid = UUID.fromString("5de394de-9c76-4b97-b04d-71be31c7f44b");
	private static final String classificationName = "Cuba Checklist";

	static final String sourceReferenceTitle = "Cuba Checklist Word Documents";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);
	static final boolean doTaxa = false;
	static final boolean doDeduplicate = true;


	private void doImport(ICdmDataSource cdmDestination){

	    URI source = monocots();  //just any

		//make Source
		CubaImportConfigurator config= CubaImportConfigurator.NewInstance(source, cdmDestination);
		config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
//		config.setDoDistribution(doDistribution);
		config.setDoTaxa(doTaxa);
		config.setDbSchemaValidation(hbm2dll);
		config.setSourceReferenceTitle(sourceReferenceTitle);
		config.setDoVocabularies(doVocabularies);

		CdmDefaultImport<CubaImportConfigurator> myImport = new CdmDefaultImport<CubaImportConfigurator>();


		//...
        if (doAsteraceae){
            doSingleSource(asteraceae(), config, myImport, doVocabularies);
        }
        if (doConvolvulaceae){
            doSingleSource(convolvulaceae(), config, myImport, doVocabularies);
        }
        if (doCyperaceae){
            doSingleSource(cyperaceae(), config, myImport, doVocabularies);
        }
        if (doDicotA_C){
            doSingleSource(dicotA_C(), config, myImport, doVocabularies);
        }
        if (doDicotD_M){
            doSingleSource(dicotD_M(), config, myImport, doVocabularies);
        }
        if (doDicotN_Z){
            doSingleSource(dicotN_Z(), config, myImport, doVocabularies);
        }
        if (doEuphorbiaceae){
            doSingleSource(euphorbiaceae(), config, myImport, doVocabularies);
        }
        if (doFabaceae){
            doSingleSource(fabaceae(), config, myImport, doVocabularies);
        }
        if (doGymnospermae){
            doSingleSource(gymnospermae(), config, myImport, doVocabularies);
        }
        if (doLamVerbenaceae){
            doSingleSource(lamVerbenaceae(), config, myImport, doVocabularies);
        }
        if (doMalpighiaceae){
            doSingleSource(malpighiaceae(), config, myImport, doVocabularies);
        }
        if (doMelastomataceae){
            doSingleSource(melastomataceae(), config, myImport, doVocabularies);
        }
        if (doMonocots){
            doSingleSource(monocots(), config, myImport, doVocabularies);
        }
        if (doMyrtaceae){
            doSingleSource(myrtaceae(), config, myImport, doVocabularies);
        }
        if (doOrchidaceae){
            doSingleSource(orchidaceae(), config, myImport, doVocabularies);
        }
        if (doRubiaceae){
            doSingleSource(rubiaceae(), config, myImport, doVocabularies);
        }
        if (doUrticaceae){
            doSingleSource(urticaceae(), config, myImport, doVocabularies);
        }


		//deduplicate
		if (doDeduplicate){
		    logger.warn("Start deduplication ...");

		    ICdmApplicationConfiguration app = myImport.getCdmAppController();
			if (app == null){
                app = CdmApplicationController.NewInstance(cdmDestination, hbm2dll, false);
            }
			int count = app.getAgentService().deduplicate(Person.class, null, null);
			logger.warn("Deduplicated " + count + " persons.");
//			count = app.getAgentService().deduplicate(Team.class, null, null);
//			logger.warn("Deduplicated " + count + " teams.");
//			count = app.getReferenceService().deduplicate(Reference.class, null, null);
//			logger.warn("Deduplicated " + count + " references.");
		}

		System.exit(0);

	}

    /**
     * @param source
     * @param config
     * @param myImport
     */
    private void doSingleSource(URI source, CubaImportConfigurator config,
            CdmDefaultImport<CubaImportConfigurator> myImport, boolean doVocabularies) {
        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);
        config.setSourceReference(getSourceReference(fileName));
        config.setDoVocabularies(doVocabularies);
        myImport.invoke(config);

        if (doVocabularies){
            FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
            myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
            this.doVocabularies = false;
        }
        System.out.println("End import from ("+ source.toString() + ")...");
    }

    private final Reference inRef = ReferenceFactory.newGeneric();
	private Reference getSourceReference(String string) {
		Reference result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		result.setInReference(inRef);
		inRef.setTitleCache(sourceReferenceTitle, true);
		return result;
	}

	private FeatureTree makeFeatureNodes(ITermService service){
//		CyprusTransformer transformer = new CyprusTransformer();

		FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
		result.setTitleCache("Cuba Feature Tree", true);
		FeatureNode root = result.getRoot();
		FeatureNode newNode;

		newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
		root.addChild(newNode);

//		Feature featurAltFam = (Feature)service.find(CubaTransformer.uuidAlternativeFamily);
//		newNode = FeatureNode.NewInstance(featurAltFam);
//		root.addChild(newNode);

	    Feature featurAltFam2 = (Feature)service.find(CubaTransformer.uuidAlternativeFamily2);
	    newNode = FeatureNode.NewInstance(featurAltFam2);
	    root.addChild(newNode);

		return result;
	}


	//Monocots
	public static URI monocots() {
	    return URI.create("file:////BGBM-PESIHPC/Cuba/Monocot.xlsx");
	}
	//Cyperaceae
	public static URI cyperaceae() {
	    return URI.create("file:////BGBM-PESIHPC/Cuba/Cyper_Poaceae.xlsx");
	}
    //Fabaceae
    public static URI fabaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Fabaceae.xlsx");
    }
    //Urticaceae
    public static URI urticaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Urticaceae.xlsx");
    }
    //Asteraceae
    public static URI asteraceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Asteraceae.xlsx");
    }
    //Convolvulaceae
    public static URI convolvulaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Convolvulaceae.xlsx");
    }
    //dicot A-C
    public static URI dicotA_C() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/dicotA_C.xlsx");
    }
    //dicot D-M
    public static URI dicotD_M() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/dicotD_M.xlsx");
    }
    //dicot N-Z
    public static URI dicotN_Z() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/dicotN_Z.xlsx");
    }
    //Euphorbiaceae
    public static URI euphorbiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Euphorbiaceae.xlsx");
    }
    //Gymnospermae
    public static URI gymnospermae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/gymnospermae.xlsx");
    }
    //Lam.Verbenaceae
    public static URI lamVerbenaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Lam_Verbenaceae.xlsx");
    }
    //Malpighiaceae
    public static URI malpighiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Malpighiaceae.xlsx");
    }
    //Melastomataceae
    public static URI melastomataceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Melastomataceae.xlsx");
    }
    //Myrtaceae
    public static URI myrtaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Myrtaceae.xlsx");
    }
    //Orchidaceae
    public static URI orchidaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Orchidaceae.xlsx");
    }
    //Rubiaceae
    public static URI rubiaceae() {
        return URI.create("file:////BGBM-PESIHPC/Cuba/Rubiaceae.xlsx");
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    CubaActivator me = new CubaActivator();
		me.doImport(cdmDestination);
	}

}
