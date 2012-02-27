// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @created 01.03.2010
 * @version 1.0
 */
public final class IndesFungorumTransformer extends InputTransformerBase {
	private static final Logger logger = Logger.getLogger(IndesFungorumTransformer.class);
	


	public static NomenclaturalCode kingdomId2NomCode(Integer kingdomId){
		switch (kingdomId){
			case 1: return null;
			case 2: return NomenclaturalCode.ICZN;  //Animalia
			case 3: return NomenclaturalCode.ICBN;  //Plantae
			case 4: return NomenclaturalCode.ICBN;  //Fungi
			case 5: return NomenclaturalCode.ICZN ;  //Protozoa
			case 6: return NomenclaturalCode.ICNB ;  //Bacteria
			case 7: return NomenclaturalCode.ICBN;  //Chromista
			case 147415: return NomenclaturalCode.ICNB;  //Monera
			default: return null;
	
		}
	
	}



}
