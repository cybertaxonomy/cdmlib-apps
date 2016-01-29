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

import eu.etaxonomy.cdm.api.application.ICdmApplicationConfiguration;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.cuba.CubaImportConfigurator;
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
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_cuba_production();

	static boolean invers = false;

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
	private static final String classificationName = "Flora of Cuba";

	static final String sourceReferenceTitle = "Cuba import";

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

	boolean doVocabularies = (hbm2dll == DbSchemaValidation.CREATE);
	static final boolean doTaxa = true;
	static final boolean doDeduplicate = false;


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
            doSource(asteraceae(), config, myImport, doVocabularies);
        }
        if (doConvolvulaceae){
            doSource(convolvulaceae(), config, myImport, doVocabularies);
        }
        if (doCyperaceae){
            doSource(cyperaceae(), config, myImport, doVocabularies);
        }
        if (doDicotA_C){
            doSource(dicotA_C(), config, myImport, doVocabularies);
        }
        if (doDicotD_M){
            doSource(dicotD_M(), config, myImport, doVocabularies);
        }
        if (doDicotN_Z){
            doSource(dicotN_Z(), config, myImport, doVocabularies);
        }
        if (doEuphorbiaceae){
            doSource(euphorbiaceae(), config, myImport, doVocabularies);
        }
        if (doFabaceae){
            doSource(fabaceae(), config, myImport, doVocabularies);
        }
        if (doGymnospermae){
            doSource(gymnospermae(), config, myImport, doVocabularies);
        }
        if (doLamVerbenaceae){
            doSource(lamVerbenaceae(), config, myImport, doVocabularies);
        }
        if (doMalpighiaceae){
            doSource(malpighiaceae(), config, myImport, doVocabularies);
        }
        if (doMelastomataceae){
            doSource(melastomataceae(), config, myImport, doVocabularies);
        }
        if (doMonocots){
            doSource(monocots(), config, myImport, doVocabularies);
        }
        if (doMyrtaceae){
            doSource(myrtaceae(), config, myImport, doVocabularies);
        }
        if (doOrchidaceae){
            doSource(orchidaceae(), config, myImport, doVocabularies);
        }
        if (doRubiaceae){
            doSource(rubiaceae(), config, myImport, doVocabularies);
        }
        if (doUrticaceae){
            doSource(urticaceae(), config, myImport, doVocabularies);
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


		System.exit(0);

	}

    /**
     * @param source
     * @param config
     * @param myImport
     */
    private void doSource(URI source, CubaImportConfigurator config,
            CdmDefaultImport<CubaImportConfigurator> myImport, boolean doVocabularies) {
        config.setSource(source);
        String message = "Start import from ("+ source.toString() + ") ...";
        System.out.println(message);
        logger.warn(message);
        config.setSourceReference(getSourceReference(sourceReferenceTitle));
        config.setDoVocabularies(doVocabularies);
        myImport.invoke(config);

        if (doVocabularies){
            FeatureTree tree = makeFeatureNodes(myImport.getCdmAppController().getTermService());
            myImport.getCdmAppController().getFeatureTreeService().saveOrUpdate(tree);
            this.doVocabularies = false;
        }
        System.out.println("End import from ("+ source.toString() + ")...");
    }

	private Reference<?> getSourceReference(String string) {
		Reference<?> result = ReferenceFactory.newGeneric();
		result.setTitleCache(string, true);
		return result;
	}

	private FeatureTree makeFeatureNodes(ITermService service){
//		CyprusTransformer transformer = new CyprusTransformer();

		FeatureTree result = FeatureTree.NewInstance(featureTreeUuid);
		result.setTitleCache("Cuba Feature Tree", true);
		FeatureNode root = result.getRoot();
		FeatureNode newNode;

//		newNode = FeatureNode.NewInstance(Feature.STATUS());
//		root.addChild(newNode);

		newNode = FeatureNode.NewInstance(Feature.DISTRIBUTION());
		root.addChild(newNode);

//		newNode = FeatureNode.NewInstance(Feature.SYSTEMATICS());
//		root.addChild(newNode);

		//user defined features
//		String [] featureList = new String[]{"Red Book", "Endemism"};
//		addFeataureNodesByStringList(featureList, root, transformer, service);

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
