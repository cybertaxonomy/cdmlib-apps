// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.wp6;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;

/**
 * @author a.mueller
 * @created 01.03.2010
 * @version 1.0
 */
public final class CommonNamesTransformer extends InputTransformerBase {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(CommonNamesTransformer.class);
	

	//named area
	public static final UUID uuidCentralAfrica =  UUID.fromString("45f4e8a4-5145-4d87-af39-122495c08fe3");
	public static final UUID uuidCentralAndEastAfrica =  UUID.fromString("3ea55ef5-7428-4c3f-a1a8-4b6c306add0f");
	public static final UUID uuidCentralAsiaAndMiddleEast =  UUID.fromString("99c2c1e8-dfda-47a4-9835-312336e8ef0e");
	public static final UUID uuidCentralEastAndSouthernAfrica =  UUID.fromString("16972365-a1f7-49ac-89f5-700f3f186263");
	public static final UUID uuidEastAfrica =  UUID.fromString("3b548c0f-8d5d-4f03-b1f2-0c1cd1aa28d2");
	public static final UUID uuidEastAndSouthernAfrica =  UUID.fromString("4b785977-0878-4919-8b80-3b57e64eaa22");
	public static final UUID uuidMascareneIslands =  UUID.fromString("317ad421-5d3e-4e80-b048-15f703de7cee");
	public static final UUID uuidMiddleEast =  UUID.fromString("6575628a-95fa-46ba-aeab-14dbc1300e35");
	public static final UUID uuidNorthEastAfrica =  UUID.fromString("d27cd317-2bd5-4129-8762-40d313d21bed");
	public static final UUID uuidSeychellesMadagascar =  UUID.fromString("36874d33-033e-4b91-9200-96c00e6ef981");
	public static final UUID uuidSeychellesMadagascarMauritius = UUID.fromString("c0d14467-1c8a-4c12-bb1f-8745daa14ab2");
	public static final UUID uuidSomaliaEthiopia =  UUID.fromString("3b4ac59c-b9d6-4cf3-97b0-dff4df7ab839");
	public static final UUID uuidSouthAfrica =  UUID.fromString("12288119-7cea-4cb2-a460-92d9eb8500fb");
	public static final UUID uuidSouthAsia =  UUID.fromString("7127dfb4-1e4b-48b0-9876-204c54a74814");
	public static final UUID uuidSouthEastAsia =  UUID.fromString("ba137511-7137-4692-816d-bf2823c52219");
	public static final UUID uuidWestAfrica =  UUID.fromString("49add437-63c8-4d12-ac32-00988ccde0e7");
	public static final UUID uuidWestAndCentralAfrica =  UUID.fromString("29027ab6-6d21-413b-8d3d-8548d40d5801");
	public static final UUID uuidWestCentralEastAfrica =  UUID.fromString("a94d2b9d-c58e-41df-9587-e3e01714b000");
	public static final UUID uuidWesternAndEasternAfrica =  UUID.fromString("19ffdae5-622c-459d-af29-c19914e0e3da");
	public static final UUID uuidB =  UUID.fromString("d23b31f7-21da-4ff7-bc6b-1ad619f5cf14");
	public static final UUID uuidC =  UUID.fromString("8a1e2e2b-345e-4991-beb3-d94dfdf6a91b");
	public static final UUID uuidD =  UUID.fromString("0938b8b1-6948-45e9-874a-7cd2dfb1ea37");
	
//	
//	
//	
//	
//	
//	7915d555-72b3-4862-b8de-d6037dc581f0
//	0a5f8631-e461-4b1a-bca4-52ef45b2ed10
//	5e4ff341-a5fd-4ae7-9228-c60cb5c668fa
//	f9deb1c6-da95-46a3-a9eb-046fae544850
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getMarkerTypeByKey(java.lang.String)
	 */
	@Override
	public NamedArea getNamedAreaByKey(String key) throws UndefinedTransformerMethodException {
		if (CdmUtils.isEmpty(key)){return null;
//		}else if (key.equalsIgnoreCase("Australia")){return WaterbodyOrCountry.AUSTRALIACOMMONWEALTHOF();
		}else if (key.equalsIgnoreCase("Azores")){return TdwgArea.getAreaByTdwgAbbreviation("AZO-OO");
		}else if (key.equalsIgnoreCase("Canary Islands")){return TdwgArea.getAreaByTdwgAbbreviation("CNY-OO");
		}else if (key.equalsIgnoreCase("North America")){return TdwgArea.getAreaByTdwgAbbreviation("7");
		}else if (key.equalsIgnoreCase("Tansania")){return TdwgArea.getAreaByTdwgAbbreviation("TAN-OO");
		
		}else{
			return null;
		}
	}

	@Override
	public UUID getNamedAreaUuid(String key) throws UndefinedTransformerMethodException {
		if (CdmUtils.isEmpty(key)){return null;
		}else if (key.equalsIgnoreCase("Central Africa")){return uuidCentralAfrica;
		}else if (key.equalsIgnoreCase("Central and East Africa")){return uuidCentralAndEastAfrica;
		}else if (key.equalsIgnoreCase("Central Asia and Middle East")){return uuidCentralAsiaAndMiddleEast;
		}else if (key.equalsIgnoreCase("Central, East and Southern Africa")){return uuidCentralEastAndSouthernAfrica;
		}else if (key.equalsIgnoreCase("East and Southern Africa")){return uuidEastAndSouthernAfrica;
		}else if (key.equalsIgnoreCase("East Africa")){return uuidEastAfrica;
		}else if (key.equalsIgnoreCase("Mascarene Islands")){return uuidMascareneIslands;
		}else if (key.equalsIgnoreCase("Middle East")){return uuidMiddleEast;
		}else if (key.equalsIgnoreCase("North East Africa")){return uuidNorthEastAfrica;
		}else if (key.equalsIgnoreCase("Seychelles and Madagascar")){return uuidSeychellesMadagascar;
		}else if (key.equalsIgnoreCase("Seychelles, Madagascar and Mauritius")){return uuidSeychellesMadagascarMauritius;
		}else if (key.equalsIgnoreCase("Somalia and Ethiopia")){return uuidSomaliaEthiopia;
		}else if (key.equalsIgnoreCase("South Africa")){return uuidSouthAfrica;
		}else if (key.equalsIgnoreCase("Southeast Asia")){return uuidSouthEastAsia;
		}else if (key.equalsIgnoreCase("West Africa")){return uuidWestAfrica;
		}else if (key.equalsIgnoreCase("West and Central Africa")){return uuidWestAndCentralAfrica;
		}else if (key.equalsIgnoreCase("West, Central and East Africa")){return uuidWestCentralEastAfrica;
		}else if (key.equalsIgnoreCase("South Asia")){return uuidSouthAsia;
		}else if (key.equalsIgnoreCase("Western Africa")){return uuidWestAfrica;
		}else if (key.equalsIgnoreCase("Western and Eastern Africa")){return uuidWesternAndEasternAfrica;
		}else if (key.equalsIgnoreCase("Western Central Africa")){return uuidWestAndCentralAfrica;
		
		
		}else{
			return null;
		}

	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getLanguageByKey(java.lang.String)
	 */
	@Override
	public Language getLanguageByKey(String key) throws UndefinedTransformerMethodException {
		if (CdmUtils.isEmpty(key)){return null;
//		}else if (key.equalsIgnoreCase("Australia")){return WaterbodyOrCountry.AUSTRALIACOMMONWEALTHOF();
		}else{
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getLanguageUuid(java.lang.String)
	 */
	@Override
	public UUID getLanguageUuid(String key) throws UndefinedTransformerMethodException {
		if (CdmUtils.isEmpty(key)){return null;
//		}else if (key.equalsIgnoreCase("Central Africa")){return uuidCentralAfrica;
		
		}else{
			return null;
		}

	}
	
}
