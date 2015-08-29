// $Id$
/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.eflora;

import java.net.URI;

import org.apache.log4j.Logger;

/**
 * @author a.mueller
 * @date 09.06.2010
 *
 */
public class EfloraSources {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(EfloraSources.class);

	//Ericaceae
	public static URI ericacea_local() {
		return URI.create("file:C:/localCopy/Data/eflora/africa/Ericaceae/ericaceae_v2.xml");
	}
	
	public static URI ericacea_specimen_local() {
		return URI.create("file:/C:/localCopy/Data/eflora/africa/Specimen/Ericaceae/Ericaceae_CDM_specimen.xls");
	}
	
	public static URI vittaria_specimen_pesiimport3() {
		return URI.create("file:/F:/data/eflora/fdac/Vittaria_neu4b.xls");
	}
	
//******************* MALESIANA ************************************************************/
	
	//Sapindaceae
	public static URI fm_sapindaceae_local(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/sapindaceae-01v25.xml");
	}
	
	//Sapindaceae2
	public static URI fm_sapindaceae2_local(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/sapindaceae-02final2.xml");
	}
	
	//Flora Malesiana Vol 13-1
	public static URI fm_13_1_local(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/fm13_1_v8_final.xml");
	}

	//Flora Malesiana Vol 13-2
	public static URI fm_13_2_local(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/fm13_2_v8_final.xml");
	}

  //***** FM NEW */	
	
	//Flora Malesiana Vol 08_1
	public static URI fm_08_1(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol8_part1_final.xml");
	}
	
	//Flora Malesiana Vol 08_2
	public static URI fm_08_2(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol8_part2_final.xml");
	}

	//Flora Malesiana Vol 08_3
	public static URI fm_08_3(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol8_part3_final.xml");
	}
	
	//Flora Malesiana Vol 10_1
	public static URI fm_10_1(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol10_part1_final.xml");
	}
	
	//Flora Malesiana Vol 10_2
	public static URI fm_10_2(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol10_part2_final.xml");
	}

	//Flora Malesiana Vol 10_3
	public static URI fm_10_3(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol10_part3_final.xml");
	}

	//Flora Malesiana Vol 10_4
	public static URI fm_10_4(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol10_part4_final.xml");
	}
	
	//Flora Malesiana Vol 11_1
	public static URI fm_11_1(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol11_part1_final.xml");
	}
	
	//Flora Malesiana Vol 11_2
	public static URI fm_11_2(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol11_part2_final.xml");
	}

	//Flora Malesiana Vol 11_3
	public static URI fm_11_3(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol11_part3_final.xml");
	}

	//Flora Malesiana Vol 12_1
	public static URI fm_12_1(){
		return URI.create("file://PESIIMPORT3/malesiana/vol12_1_final.xml");
	}
	
	//Flora Malesiana Vol 12_2
	public static URI fm_12_2(){
		return URI.create("file://PESIIMPORT3/malesiana/vol12_2_final.xml");
	}
	
	public static URI fm_13(){
		return URI.create("file://PESIIMPORT3/malesiana/vol13_final.xml");
	}

//	//Flora Malesiana Vol 13 - large families
//	public static URI fm_13_large_families(){
//		return URI.create("file://PESIIMPORT3/malesiana/vol_13/xmlv9_large_families_vol_13.xml");
//	}

	//Flora Malesiana Vol 14
	public static URI fm_14(){
		return URI.create("file://PESIIMPORT3/malesiana/vol14_final.xml");
	}
	
	//Flora Malesiana Vol 15
	public static URI fm_15(){
		return URI.create("file://PESIIMPORT3/malesiana/vol15_final.xml");
	}

	//Flora Malesiana Vol 16
	public static URI fm_16(){
		return URI.create("file://PESIIMPORT3/malesiana/vol16_final.xml");
	}

	//Flora Malesiana Vol 17, part1
	public static URI fm_17_1(){
		return URI.create("file://PESIIMPORT3/malesiana/vol17_part1_final.xml");
	}
	
	//Flora Malesiana Vol 17, part2
	public static URI fm_17_2(){
		return URI.create("file://PESIIMPORT3/malesiana/vol17_part2_final.xml");
	}
	
	//Flora Malesiana Vol 18
	public static URI fm_18(){
		return URI.create("file://PESIIMPORT3/malesiana/vol18_final.xml");
	}
	//Flora Malesiana Vol 19
	public static URI fm_19(){
		return URI.create("file://PESIIMPORT3/malesiana/vol19_final.xml");
	}
	//Flora Malesiana Vol 20
	public static URI fm_20(){
		return URI.create("file://PESIIMPORT3/malesiana/vol20_final.xml");
	}
	
	//Flora Malesiana Vol 21
	public static URI fm_21(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/vol21_final.xml");
	}
	
	//Flora Malesiana Series 2 - Vol 2
	public static URI fm_ser2_2(){
		return URI.create("file://PESIIMPORT3/malesiana/IIvol2_final.xml");
	}

	//Flora Malesiana Series 2 - Vol 3
	public static URI fm_ser2_3(){
		return URI.create("file://PESIIMPORT3/malesiana/IIvol3_final.xml");
	}
	
	//Flora Malesiana Series 2 - Vol 4
	public static URI fm_ser2_4(){
		return URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/ser2vol4_final.xml");
	}

//************************* GABON ************************************************/
	
	//Flore du Gabon sample 
	public static URI fdg_sample(){
		return URI.create("file:/E:/opt/data/floreGabon/sample.xml");
	}

	//Flore du Gabon vol 1
	public static URI fdg_1(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol1_9.xml");
	}

	//Flore du Gabon vol 2
	public static URI fdg_2(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol2_9.xml");
	}
	
	//Flore du Gabon vol 3
	public static URI fdg_3(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol3_9.xml");
	}
	
	//Flore du Gabon vol 4
	public static URI fdg_4(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol4_9.xml");
	}
	
	//Flore du Gabon vol 5
	public static URI fdg_5(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol5_final.xml");
	}

	
	//Flore du Gabon vol 5
	public static URI fdg_5bis(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/5bis_final.xml");
	}
	
	//Flore du Gabon vol 6
	public static URI fdg_6(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol6_final.xml");
	}
	
	//Flore du Gabon vol 7
	public static URI fdg_7(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol7_final.xml");
	}
	
	//Flore du Gabon vol 8
	public static URI fdg_8(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol8_final.xml");
	}
	
	//Flore du Gabon vol 9
	public static URI fdg_9(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol9_final.xml");
	}
	
	//Flore du Gabon vol 10
	public static URI fdg_10(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol10_final.xml");
	}
	
	//Flore du Gabon vol 11
	public static URI fdg_11(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol11_final.xml");
	}

	//Flore du Gabon vol 12 and 17  (same family)
	public static URI fdg_12_17(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol12and17_final.xml");
	}


	//Flore du Gabon vol 13
	public static URI fdg_13(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol13_final.xml");
	}

	//Flore du Gabon vol 14
	public static URI fdg_14(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol14_final.xml");
	}

	//Flore du Gabon vol 15
	public static URI fdg_15(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol15_final.xml");
	}

	//Flore du Gabon vol 16
	public static URI fdg_16(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol16_final.xml");
	}

	//Flore du Gabon vol 17
	public static URI fdg_17(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol17_final.xml");
	}

	//Flore du Gabon vol 18
	public static URI fdg_18(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol18_final.xml");
	}

	//Flore du Gabon vol 19
	public static URI fdg_19(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol19_final.xml");
	}

	//Flore du Gabon vol 20
	public static URI fdg_20(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol20_final.xml");
	}
	
	//Flore du Gabon vol 21
	public static URI fdg_21(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol21_final.xml");
	}

	//Flore du Gabon vol 22
	public static URI fdg_22(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol22_final.xml");
	}

	//Flore du Gabon vol 23
	public static URI fdg_23(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol23_final.xml");
	}

	//Flore du Gabon vol 24
	public static URI fdg_24(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol24_final.xml");
	}

	//Flore du Gabon vol 25
	public static URI fdg_25(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol25_final.xml");
	}

	//Flore du Gabon vol 26
	public static URI fdg_26(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol26_final.xml");
	}
	
	//Flore du Gabon vol 27
	public static URI fdg_27(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol27_final.xml");
	}

	//Flore du Gabon vol 28
	public static URI fdg_28(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol28_final.xml");
	}

	//Flore du Gabon vol 29
	public static URI fdg_29(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol29_final.xml");
	}

	//Flore du Gabon vol 30
	public static URI fdg_30(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol30_final.xml");
	}
	
	//Flore du Gabon vol 31
	public static URI fdg_31(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol31_final.xml");
	}
	//Flore du Gabon vol 32
	public static URI fdg_32(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol32_final.xml");
	}
	//Flore du Gabon vol 33
	public static URI fdg_33(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol33_final.xml");
	}
	//Flore du Gabon vol 34
	public static URI fdg_34(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol34_final.xml");
	}
	
	//Flore du Gabon vol 35
	public static URI fdg_35(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol35_final.xml");
	}

	//Flore du Gabon vol 36
	public static URI fdg_36(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol36_final.xml");
	}
	
	//Flore du Gabon vol 37
	public static URI fdg_37(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol37_final.xml");
	}

	//Flore du Gabon vol 36 and 37
	public static URI fdg_36_37(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol36and37_final.xml");
	}
	
	
	//Flore du Gabon vol 38
	public static URI fdg_38(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol38_final.xml");
	}
	
	//Flore du Gabon vol 39
	public static URI fdg_39(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol39_final.xml");
	}
	
	//Flore du Gabon vol 40
	public static URI fdg_40(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol40_final.xml");
	}
	
	//Flore du Gabon vol 41
	public static URI fdg_41(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol41_final.xml");
	}
	
	//Flore du Gabon vol 42
	public static URI fdg_42(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol42_final.xml");
	}
	
	//Flore du Gabon vol 43
	public static URI fdg_43(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol43_final.xml");
	}
	
	//Flore du Gabon vol 44
	public static URI fdg_44(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol44_final.xml");
	}
	
	//Flore du Gabon vol 45
	public static URI fdg_45(){
		return URI.create("file://PESIIMPORT3/gabon/markupData/fdgvol45_final.xml");
	}
	
//************************* GUIANAS **********************************************/	
	
	//Flora of the Guianas Sample
	public static URI fgu_1(){
//		return URI.create("file:///PESIIMPORT3/guianas/markupData/79THEOPHRASTACEAE.xml");
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/79THEOPHRASTACEAE.xml");
	}

	
	public static URI fotg_03(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A03_final.xml");
	}

	
	public static URI fotg_11(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A11_final.xml");
	}

	
	public static URI fotg_14(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A14_final.xml");
	}

	
	public static URI fotg_15(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A15_final.xml");
	}

	
	public static URI fotg_16(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A16_final.xml");
	}
	
	public static URI fotg_20(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A20_final.xml");
	}

	public static URI fotg_22(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A22_final.xml");
	}

	public static URI fotg_23(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A23_final.xml");
	}
	
	public static URI fotg_24(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A24_final4.xml");
	}

	public static URI fotg_24plus(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A24_finalplus4.xml");
	}
	
	public static URI fotg_25(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A25_final2.xml");
	}

	public static URI fotg_25plus(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A25_finalplus2.xml");
	}
	
	public static URI fotg_26(){
		return URI.create("file://BGBM-PESIHPC/FloraGuianasXml/A26_final.xml");
	}

	public static URI fotg_27(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A27_final.xml");
	}

	public static URI fotg_29(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A29_final.xml");
	}
	public static URI fotg_30(){
		return URI.create("file:////BGBM-PESIHPC/FloraGuianasXml/A30_final.xml");
	}
	
}
