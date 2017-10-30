/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.redlist;

import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand1_kriechtiere;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand1_lurche;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand1_saeugetiere;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand1_suessfische;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand2_bodenlebendenWirbellosenMeerestiere;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand2_meeresfischeUndNeunaugen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_ameisen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_bienen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_binnenmollusken;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_eulenfalter;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_fransenfluegler;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_heuschrecken;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_ohrwuermer;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_pflanzenwespen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_raubfliegen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_schaben;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_schwebfliegen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_spanner;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_spinner;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_tagfalter;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_tanzfliegen;
import static eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences.uuidBand3_zuenslerfalter;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportConfigurator;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportReferences;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlTransformer;
import eu.etaxonomy.cdm.model.metadata.CdmPreference;
import eu.etaxonomy.cdm.model.metadata.PreferencePredicate;
import eu.etaxonomy.cdm.model.metadata.PreferenceSubject;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.oppermann
 * @created 16.07.2013
 */
public class BfnXmlImportActivator {

	private static final Logger logger = Logger.getLogger(BfnXmlImportActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation schemaValidation = DbSchemaValidation.CREATE;
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_plant_localhost();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_animalia_production_final();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_mammalia_test();

	private static final String sourceUriBase = "file:////BGBM-PESIHPC/RoteListen/RoteListenXml/";

	//nom Code
	private static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICZN;

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;


	static final boolean doMetaData = true;
	static final boolean doTaxonNames = true;
	static final boolean doFeature = true;
    static final boolean doAdditionalTerms = true;

    private String filename;

	public BfnXmlImportActivator(String fileName){
		filename = fileName;
	}

	private void doImport(BfnXmlImportConfigurator config, CdmDefaultImport<BfnXmlImportConfigurator> bfnImport){
		System.out.println("Start import from " + filename + " to "+ cdmDestination.getDatabase() + " ...");

		//make Source
		URI source;
		try {
		    source = URI.create(sourceUriBase + filename);
		    config.setSource(source);

			//if xmllist has two lists
			config.setHasSecondList(filename.contains("BFN_Saeuger"));
			config.setNomenclaturalCode(nomenclaturalCode);
			config.setDoMetaData(doMetaData);
			config.setDoTaxonNames(doTaxonNames);
			config.setDoFeature(doFeature);
			config.setDoAdditionalTerms(doAdditionalTerms);

			config.setCheck(check);
			config.setDbSchemaValidation(schemaValidation);

			//TODO only quickfix see also MetaData import
			String classificationName;
			if (filename.startsWith("rldb")){
			    classificationName = filename.replace("rldb_print_v4_0_1_0_", "")
			            .split("_")[0].replace(".xml", "").replace("artenarmeWeichtiergruppen", "Artenarme Weichtiergruppen");
			}else{
			    classificationName = filename.replace("RoteListe_v4_0_6_0_BFN_Saeuger_korr.xml", "Säuger");
			}
			config.setClassificationName(classificationName);

			// invoke import
			bfnImport.invoke(config);

			setVaadinPreferences(bfnImport, nomenclaturalCode);
			logger.info("End");
			System.out.println("End import from BfnXML ("+ source.toString() + ")...");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


    /**
	 * @param args
	 */
	public static void main(String[] args) {

		List<String> fileNames = Arrays.asList(
//				Plants
//		        //band2
//		        "rldb_print_v4_0_1_0_Makroalgen_150121_syn.xml",uuidBand2_marineMakroalgen.toString(),

		        //fungi
		        //band6
//				"rldb_print_v4_0_1_0_Flechten_korr_verantw_syn.xml",uuidBand6_flechtenUndPilze.toString(),
//				//korrekt??
//				"rldb_print_v4_0_1_0_Saprophyten_verantw.xml",uuidBand6_flechtenUndPilze.toString(),
//				"rldb_print_v4_0_1_0_Lichenicole_verantw_syn.xml",uuidBand6_flechtenUndPilze.toString(),
//				"rldb_print_v4_0_1_0_Myxo_110708_korr_syn_neu.xml",uuidBand6_myxomyzeten.toString(),
//
//				Animals
		        //band1
				"rldb_print_v4_0_1_0_Brutvoegel.xml",BfnXmlImportReferences.uuidBand1_brutvoegel.toString(), //Brutvögel
				"rldb_print_v4_0_1_0_Reptilien_1.xml",uuidBand1_kriechtiere.toString(),
                "rldb_print_v4_0_1_0_Amphibien.xml",uuidBand1_lurche.toString(),  //Kriechtiere
                "RoteListe_v4_0_6_0_BFN_Saeuger_korr.xml",uuidBand1_saeugetiere.toString(), //Säugetiere
                "rldb_print_v4_0_1_0_Fische.xml",uuidBand1_suessfische.toString(),
                //
		        //band2
		        "rldb_print_v4_0_1_0_artenarmeWeichtiergruppen_121127_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Asselspinnen_120907_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Flohkrebse_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Igelwuermer_120907_verantw.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Kumazeen_120709_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Asseln_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Moostierchen_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Muscheln_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Schnecken_130206_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Meeresfische_syn.xml",uuidBand2_meeresfischeUndNeunaugen.toString(),
		        "rldb_print_v4_0_1_0_Nesseltiere_130104_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Schaedellose_120907_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(), //Schädellose
		        "rldb_print_v4_0_1_0_Schwaemme_121127_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(), //Schwämme
		        "rldb_print_v4_0_1_0_Seepocken_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Seescheiden_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Stachelhaeuter_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(), //Stachelhäuter
		        "rldb_print_v4_0_1_0_Vielborster_130206_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Wenigborster_121128_verantw_syn.xml",uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        "rldb_print_v4_0_1_0_Zehnfusskrebse_130104_verantw_syn.xml", uuidBand2_bodenlebendenWirbellosenMeerestiere.toString(),
		        //
		        //band3
			    "rldb_print_v4_0_1_0_Ameisen_110609_rev120113_syn.xml", uuidBand3_ameisen.toString(), //Hymenoptera: Formicidae
			    "rldb_print_v4_0_1_0_Bienen_PWKorr_HG_120413_DF_120612_syn.xml",uuidBand3_bienen.toString(),  //Hymnenoptera: Apidae
			    "rldb_print_v4_0_1_0_Binnenmollusken_0alle_120413_DF_syn.xml",uuidBand3_binnenmollusken.toString(),
			    //eulenfalter - korrekt?
			    "rldb_print_v4_0_1_0_Eulen_Korruebern_23-05-2012_KorrV_syn.xml",uuidBand3_eulenfalter.toString(), //Eulen & Korrübern
			    "rldb_print_v4_0_1_0_Thysanoptera_120413_DF_korrV_Verantw.xml",uuidBand3_fransenfluegler.toString(),
			    "rldb_print_v4_0_1_0_Heuschrecken_syn.xml",uuidBand3_heuschrecken.toString(),  //Saltatoria
			    "rldb_print_v4_0_1_0_Ohrwuermer_DF_syn.xml",uuidBand3_ohrwuermer.toString(),   //Dermaptera
			    "rldb_print_v4_0_1_0_Pflanzenwespen_280711_Autor_110815_HG2_120413_DF_syn.xml",uuidBand3_pflanzenwespen.toString(), //Hymenoptera: Symphata
			    "rldb_print_v4_0_1_0_Asilidae_GMH_Wolff_110314_HGxls_120413_DF_korrV_Verantw_syn.xml",uuidBand3_raubfliegen.toString(),
			    "rldb_print_v4_0_1_0_Blattoptera_140413_DF_syn.xml",uuidBand3_schaben.toString(),
			    "rldb_print_v4_0_1_0_Schwebfliegen_111103_KorrAS_120413_DF_syn.xml",uuidBand3_schwebfliegen.toString(), //Diptera: Syrphidae
			    //spanner - korrekt?
				"rldb_print_v4_0_1_0_Eulenspinner_Spanner_13-06-2012_KorrV_syn.xml",uuidBand3_spanner.toString(), //Eulenspinner & Spanner
			    "rldb_print_v4_0_1_0_Spinner_Oktober2011_eingearbKorr_120124_Korruebern_MB_02-05-2012_KorrV_syn.xml",uuidBand3_spinner.toString(),
			    "rldb_print_v4_0_1_0_Tagfalter_06-06-2012_KorrV_syn.xml",uuidBand3_tagfalter.toString(),
			    "rldb_print_v4_0_1_0_Empidoidea_120413_DF.xml",uuidBand3_tanzfliegen.toString(),  //Empidoidea
			    //wespen - fehlen ????? => siehe auch titel des Referenz Word Files
			    "rldb_print_v4_0_1_0_Pyraloidea_Februar_2012_Korruebern_MB_24-04-2012_syn.xml",uuidBand3_zuenslerfalter.toString()


//		        //the 4 first lists, THESE ARE DUPLICATES
//		        "RoteListe_v4_0_6_0_BFN_Saeuger_korr.xml",
//                "rldb_print_v4_0_1_0_Amphibien.xml",
//                "rldb_print_v4_0_1_0_Reptilien_1.xml",
//                "rldb_print_v4_0_1_0_Heuschrecken_syn.xml"

				);

        CdmDefaultImport<BfnXmlImportConfigurator> bfnImport = new CdmDefaultImport<>();

        ICdmDataSource destination = cdmDestination;
        URI source = null;
		BfnXmlImportConfigurator config = BfnXmlImportConfigurator.NewInstance(source,  destination);

		Iterator<String> it = fileNames.iterator();
		while(it.hasNext()){
		    String fileName = it.next();
			BfnXmlImportActivator bfnXmlTestActivator = new BfnXmlImportActivator(fileName);
			String uuid = it.next();
			config.setSourceRefUuid(UUID.fromString(uuid));
			bfnXmlTestActivator.doImport(config, bfnImport);
//			pauseProg();
			schemaValidation = DbSchemaValidation.VALIDATE;
		}


			//first run
			//create DB,Metadata
//			String fileName = "rldb_print_v4_0_1_0_Ameisen_110609_rev120113_syn.xml";
//			BfnXmlTestActivator bfnXmlTestActivator = new BfnXmlTestActivator(fileName);
//			bfnXmlTestActivator.doImport();

		System.exit(0);
	}

	@SuppressWarnings("resource")
    public static void pauseProg(){
		System.out.println("Press enter to continue...");
		Scanner keyboard = new Scanner(System.in);
		keyboard.nextLine();
	}

    /**
     * @param bfnImport
     * @param nomCode
     */
    private void setVaadinPreferences(CdmDefaultImport<BfnXmlImportConfigurator> bfnImport, NomenclaturalCode nomCode) {
        ICdmRepository app = bfnImport.getCdmAppController();
        CdmPreference statusPref = CdmPreference.NewInstance(
                PreferenceSubject.NewVaadinInstance(),
                PreferencePredicate.AvailableDistributionStatus,
                BfnXmlTransformer.uuidStatusVorkommend,
                BfnXmlTransformer.uuidStatusUnsicher,
                BfnXmlTransformer.uuidStatusAbgelehnt,
                BfnXmlTransformer.uuidStatusKeinNachweis
                );
        app.getPreferenceService().set(statusPref);
        CdmPreference areaVocPref = CdmPreference.NewInstance(
                PreferenceSubject.NewVaadinInstance(),
                PreferencePredicate.AvailableDistributionAreaVocabularies,
                BfnXmlTransformer.uuidVocGermanFederalStates);
        app.getPreferenceService().set(areaVocPref);
        CdmPreference nomCodePref = CdmPreference.NewDatabaseInstance(PreferencePredicate.NomenclaturalCode, "eu.etaxonomy.cdm.model.name.NomenclaturalCode." + nomCode.getUuid());
        app.getPreferenceService().set(nomCodePref);

    }
}

