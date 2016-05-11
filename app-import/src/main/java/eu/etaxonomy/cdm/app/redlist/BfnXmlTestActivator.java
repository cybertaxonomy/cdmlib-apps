/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.redlist;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlImportConfigurator;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.oppermann
 * @created 16.07.2013
 */
public class BfnXmlTestActivator {

	private static final Logger logger = Logger.getLogger(BfnXmlTestActivator.class);

	//database validation status (create, update, validate ...)
	static DbSchemaValidation schemaValidation = DbSchemaValidation.CREATE;
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_plant_localhost();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_redlist_animalia_production();


//	private static final String strSource = "/eu/etaxonomy/cdm/io/bfnXml/";
	private static final String sourceUriBase = "file:///home/pplitzner/Rote%20Listen%202020/doctronic/";

	//nom Code
	private static final NomenclaturalCode nomenclaturalCode = NomenclaturalCode.ICZN;

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;


	//authors
	static final boolean doMetaData = true;
	//names
	static final boolean doTaxonNames = true;
	//feature
	static final boolean doFeature = true;
	//feature
    static final boolean doAdditionalTerms = true;

    private final String filename;

	public BfnXmlTestActivator(String fileName){
		filename = fileName;
	}

	private void doImport(){
		System.out.println("Start import from " + filename + " to "+ cdmDestination.getDatabase() + " ...");

		//make Source
		URI source;
		try {
		    source = URI.create(sourceUriBase + filename);
//			source = this.getClass().getResource(strSource + filename).toURI();
			ICdmDataSource destination = cdmDestination;

			BfnXmlImportConfigurator bfnImportConfigurator = BfnXmlImportConfigurator.NewInstance(source,  destination);

			//if xmllist has two lists
			bfnImportConfigurator.setHasSecondList(true);
			bfnImportConfigurator.setNomenclaturalCode(nomenclaturalCode);
			bfnImportConfigurator.setDoMetaData(doMetaData);
			bfnImportConfigurator.setDoTaxonNames(doTaxonNames);

			bfnImportConfigurator.setCheck(check);
			bfnImportConfigurator.setDbSchemaValidation(schemaValidation);

			//TODO only quickfix see also MetaData import
			String classificationName = filename.replace("rldb_print_v4_0_1_0_", "").split("_")[0];
			bfnImportConfigurator.setClassificationName(classificationName);

			// invoke import
			CdmDefaultImport<BfnXmlImportConfigurator> bfnImport = new CdmDefaultImport<BfnXmlImportConfigurator>();
			bfnImport.invoke(bfnImportConfigurator);

			logger.warn("End");
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
//				"rldb_print_v4_0_1_0_Flechten_korr_verantw_syn.xml"
//				"rldb_print_v4_0_1_0_Lichenicole_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Makroalgen_150121_syn.xml",
//				"rldb_print_v4_0_1_0_Myxo_110708_korr_syn_neu.xml",
//				"rldb_print_v4_0_1_0_Saprophyten_verantw.xml"
//
//				Animals
//				"rldb_print_v4_0_1_0_Ameisen_110609_rev120113_syn.xml"
//
//				"rldb_print_v4_0_1_0_artenarmeWeichtiergruppen_121127_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Asilidae_GMH_Wolff_110314_HGxls_120413_DF_korrV_Verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Asseln_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Asselspinnen_120907_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Bienen_PWKorr_HG_120413_DF_120612_syn.xml",
//				"rldb_print_v4_0_1_0_Binnenmollusken_0alle_120413_DF_syn.xml",
////				"rldb_print_v4_0_1_0_Blattoptera_140413_DF_syn.xml",
//				"rldb_print_v4_0_1_0_Empidoidea_120413_DF.xml",
//				"rldb_print_v4_0_1_0_Eulen_Korruebern_23-05-2012_KorrV_syn.xml",
//
////			,	"rldb_print_v4_0_1_0_Eulenspinner_Spanner_13-06-2012_KorrV_syn.xml",
//
//				"rldb_print_v4_0_1_0_Flohkrebse_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Heuschrecken_syn.xml",
//				"rldb_print_v4_0_1_0_Igelwuermer_120907_verantw.xml",
//				"rldb_print_v4_0_1_0_Kumazeen_120709_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Meeresfische_syn.xml",
//				"rldb_print_v4_0_1_0_Moostierchen_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Muscheln_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Nesseltiere_130104_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Ohrwuermer_DF_syn.xml",
//				"rldb_print_v4_0_1_0_Pflanzenwespen_280711_Autor_110815_HG2_120413_DF_syn.xml",
//				"rldb_print_v4_0_1_0_Pyraloidea_Februar_2012_Korruebern_MB_24-04-2012_syn.xml",
//				"rldb_print_v4_0_1_0_Schaedellose_120907_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Schnecken_130206_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Schwaemme_121127_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Schwebfliegen_111103_KorrAS_120413_DF_syn.xml",
//				"rldb_print_v4_0_1_0_Seepocken_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Seescheiden_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Spinner_Oktober2011_eingearbKorr_120124_Korruebern_MB_02-05-2012_KorrV_syn.xml",
//				"rldb_print_v4_0_1_0_Stachelhaeuter_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Tagfalter_06-06-2012_KorrV_syn.xml",
//				"rldb_print_v4_0_1_0_Thysanoptera_120413_DF_korrV_Verantw.xml",
//				"rldb_print_v4_0_1_0_Vielborster_130206_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Wenigborster_121128_verantw_syn.xml",
//				"rldb_print_v4_0_1_0_Zehnfusskrebse_130104_verantw_syn.xml"
//

				//old list
//				"rldb_print_v4_0_1_0_Amphibien.xml",
//				"rldb_print_v4_0_1_0_Brutvoegel.xml",
//				"rldb_print_v4_0_1_0_Fische.xml",
//				"rldb_print_v4_0_1_0_Reptilien_1.xml"

				//two lists in one
				"RoteListe_v4_0_6_0_BFN_Saeuger_korr.xml"
				);
		for(String fileName : fileNames){
			BfnXmlTestActivator bfnXmlTestActivator = new BfnXmlTestActivator(fileName);
			bfnXmlTestActivator.doImport();
//			pauseProg();
		}

			//first run
			//create DB,Metadata
//			String fileName = "rldb_print_v4_0_1_0_Ameisen_110609_rev120113_syn.xml";
//			BfnXmlTestActivator bfnXmlTestActivator = new BfnXmlTestActivator(fileName);
//			bfnXmlTestActivator.doImport();
	}

	@SuppressWarnings("resource")
    public static void pauseProg(){
		System.out.println("Press enter to continue...");
		Scanner keyboard = new Scanner(System.in);
		keyboard.nextLine();
	}
}

