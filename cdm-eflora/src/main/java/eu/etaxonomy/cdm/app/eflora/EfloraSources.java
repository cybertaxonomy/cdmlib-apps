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

import java.io.File;
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
	
	//Flora Malesiana Vol 12
	public static URI fm_12(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_12/xml_1995_vol_12_all_final_5.xml");
	}
	
	//Flora Malesiana Vol 13 - small families
	public static URI fm_13_small_families(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_13/vol_13_small_families.xml");
	}

	//Flora Malesiana Vol 13 - large families
	public static URI fm_13_large_families(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_13/xmlv9_large_families_vol_13.xml");
	}

	//Flora Malesiana Vol 15
	public static URI fm_15(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_15/vol15_final.xml");
	}

	//Flora Malesiana Vol 16
	public static URI fm_16(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_16/vol16_final.xml");
	}

	//Flora Malesiana Vol 17, part1
	public static URI fm_17_1(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_17/fmvol17_part1_final.xml");
	}
	
	//Flora Malesiana Vol 17, part2
	public static URI fm_17_2(){
		return URI.create("file://PESIIMPORT3/malesiana/vol_17/fmvol17_part2_final.xml");
	}
	
	//Flora Malesiana Series 2 - Vol 2
	public static URI fm_ser2_2(){
		return URI.create("file://PESIIMPORT3/malesiana/ser2/vol_02/ser2vol2final1.xml");
	}

	//Flora Malesiana Series 2 - Vol 3
	public static URI fm_ser2_3(){
		return URI.create("file://PESIIMPORT3/malesiana/ser2/vol_03/IIvol3_final.xml");
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
	
//************************* GUIANAS **********************************************/	
	
	//Flora of the Guianas Sample
	public static URI fgu_1(){
		return URI.create("file://PESIIMPORT3/guianas/markupData/79THEOPHRASTACEAE.xml");
	}
	
}
