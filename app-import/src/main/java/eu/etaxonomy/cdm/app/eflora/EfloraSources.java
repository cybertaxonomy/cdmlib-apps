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


}
