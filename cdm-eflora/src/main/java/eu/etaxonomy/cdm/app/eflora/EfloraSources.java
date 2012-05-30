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
	
	//Flora Malesiana Vol 13 - small families
	public static URI fm_13_small_families(){
		URI uri = URI.create("file:/C:/localCopy/Data/eflora/floraMalesiana/vol_13/vol_13_small_families.xml");
		File file = new File(uri);
		return uri;
	}

	//Flora Malesiana Vol 13 - large families
	public static URI fm_13_large_families(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/vol_13/xmlv9_large_families_vol_13.xml");
	}


	//Flora Malesiana Vol 12
	public static URI fm_12(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/vol_12/xml_1995_vol_12_all_final_5.xml");
	}

	//Flora Malesiana Series 2 - Vol 2
	public static URI fm_2_2(){
		return URI.create("file:C:/localCopy/Data/eflora/floraMalesiana/ser2_vol2/ser2vol2final1.xml");
	}
	
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
	
	//Flora of the Guianas Sample
	public static URI fgu_1(){
		return URI.create("file://PESIIMPORT3/guianas/markupData/79THEOPHRASTACEAE.xml");
	}
	
}
