// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;

/**
 * @author a.mueller
 * @created 01.03.2010
 * @version 1.0
 */
public final class GlobisTransformer extends InputTransformerBase {
	private static final Logger logger = Logger.getLogger(GlobisTransformer.class);
	

	//extension types
//	public static final UUID uuidEditor = UUID.fromString("07752659-3018-4880-bf26-41bb396fbf37");
	
	
	//language uuids
	
	
	
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getNameTypeDesignationStatusByKey(java.lang.String)
	 */
	@Override
	public NameTypeDesignationStatus getNameTypeDesignationStatusByKey(String key) throws UndefinedTransformerMethodException {
		if (key == null){
			return null;
		}
		Integer intDesignationId = Integer.valueOf(key);
		switch (intDesignationId){
			case 1: return NameTypeDesignationStatus.ORIGINAL_DESIGNATION();
			case 2: return NameTypeDesignationStatus.SUBSEQUENT_DESIGNATION();
			case 3: return NameTypeDesignationStatus.MONOTYPY();
			default: 
				String warning = "Unknown name type designation status id " + key;
				logger.warn(warning);
				return null;
		}
	}




	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getNameTypeDesignationStatusUuid(java.lang.String)
	 */
	@Override
	public UUID getNameTypeDesignationStatusUuid(String key) throws UndefinedTransformerMethodException {
		//nott needed
		return super.getNameTypeDesignationStatusUuid(key);
	}


	public NamedArea getNamedAreaByKey(String area)  {
		Set<String> unhandledCountries = new HashSet<String>();
		
		if (StringUtils.isBlank(area)){return null;
		}else if (area.equals("Argentina")){return WaterbodyOrCountry.ARGENTINAARGENTINEREPUBLIC();
		}else if (area.equals("Bolivia")){return WaterbodyOrCountry.BOLIVIAREPUBLICOF();
		}else if (area.equals("Ghana")){return WaterbodyOrCountry.GHANAREPUBLICOF();
		}else if (area.equals("Angola")){return WaterbodyOrCountry.ANGOLAREPUBLICOF();
		}else if (area.equals("Tanzania")){return WaterbodyOrCountry.TANZANIAUNITEDREPUBLICOF();
		}else if (area.equals("China")){return WaterbodyOrCountry.CHINAPEOPLESREPUBLICOF();
		}else if (area.equals("Brunei")){return WaterbodyOrCountry.BRUNEIDARUSSALAM();
		}else if (area.equals("Australia")){return WaterbodyOrCountry.AUSTRALIACOMMONWEALTHOF();
		}else if (area.equals("Indonesia")){return WaterbodyOrCountry.INDONESIAREPUBLICOF();
		}else if (area.equals("Philippines")){return WaterbodyOrCountry.PHILIPPINESREPUBLICOFTHE();
		}else if (area.equals("Mongolia")){return WaterbodyOrCountry.MONGOLIAMONGOLIANPEOPLESREPUBLIC();
		}else if (area.equals("Russia")){return WaterbodyOrCountry.RUSSIANFEDERATION();
		}else if (area.equals("France")){return WaterbodyOrCountry.FRANCEFRENCHREPUBLIC();
		}else if (area.equals("Poland")){return WaterbodyOrCountry.POLANDPOLISHPEOPLESREPUBLIC();
		}else if (area.equals("Brazil")){return WaterbodyOrCountry.BRAZILFEDERATIVEREPUBLICOF();
		
		}else if (area.equals("Cuba")){return WaterbodyOrCountry.BRAZILFEDERATIVEREPUBLICOF();
		}else if (area.equals("Guatemala")){return WaterbodyOrCountry.GUATEMALAREPUBLICOF();
		}else if (area.equals("Colombia")){return WaterbodyOrCountry.COLOMBIAREPUBLICOF();
		}else if (area.equals("India")){return WaterbodyOrCountry.INDIAREPUBLICOF();
		
		}else if (area.equals("Mexico")){return WaterbodyOrCountry.MEXICOUNITEDMEXICANSTATES();
		}else if (area.equals("Peru")){return WaterbodyOrCountry.PERUREPUBLICOF();
		}else if (area.equals("Ecuador")){return WaterbodyOrCountry.ECUADORREPUBLICOF();
		}else if (area.equals("Venezuela")){return WaterbodyOrCountry.VENEZUELABOLIVARIANREPUBLICOF();
		}else if (area.equals("Guyana")){return WaterbodyOrCountry.GUYANAREPUBLICOF();
		}else if (area.equals("Panama")){return WaterbodyOrCountry.PANAMAREPUBLICOF();

		}else if (area.equals("Paraguay")){return WaterbodyOrCountry.PARAGUAYREPUBLICOF();
		}else if (area.equals("Suriname")){return WaterbodyOrCountry.SURINAMEREPUBLICOF();
		}else if (area.equals("Costa Rica")){return WaterbodyOrCountry.COSTARICAREPUBLICOF();
		}else if (area.equals("Ivory Coast")){return WaterbodyOrCountry.COTEDIVOIREIVORYCOASTREPUBLICOFTHE();

		}else if (area.equals("Benin")){return WaterbodyOrCountry.BENINPEOPLESREPUBLICOF();
		}else if (area.equals("Kenya")){return WaterbodyOrCountry.KENYAREPUBLICOF();
		}else if (area.equals("Uganda")){return WaterbodyOrCountry.UGANDAREPUBLICOF();
		}else if (area.equals("Zambia")){return WaterbodyOrCountry.ZAMBIAREPUBLICOF();
		}else if (area.equals("Rwanda")){return WaterbodyOrCountry.RWANDARWANDESEREPUBLIC();
		}else if (area.equals("South Africa")){return WaterbodyOrCountry.SOUTHAFRICAREPUBLICOF();
		}else if (area.equals("Botswana")){return WaterbodyOrCountry.BOTSWANAREPUBLICOF();
		}else if (area.equals("Burundi")){return WaterbodyOrCountry.BURUNDIREPUBLICOF();
		}else if (area.equals("Cameroon")){return WaterbodyOrCountry.CAMEROONUNITEDREPUBLICOF();
		
//		}else if (area.equals("Congo")){return WaterbodyOrCountry.Congo();
		}else if (area.equals("Equatorial Guinea")){return WaterbodyOrCountry.EQUATORIALGUINEAREPUBLICOF();
		}else if (area.equals("Gabon")){return WaterbodyOrCountry.GABONGABONESEREPUBLIC();
		}else if (area.equals("Liberia")){return WaterbodyOrCountry.LIBERIAREPUBLICOF();
		
		}else if (area.equals("Togo")){return WaterbodyOrCountry.TOGOTOGOLESEREPUBLIC();
//		}else if (area.equals("Guinea")){return WaterbodyOrCountry.Guinea();
		}else if (area.equals("Malawi")){return WaterbodyOrCountry.MALAWIREPUBLICOF();
		}else if (area.equals("Mozambique")){return WaterbodyOrCountry.MOZAMBIQUEPEOPLESREPUBLICOF();
		}else if (area.equals("Nigeria")){return WaterbodyOrCountry.NIGERIAFEDERALREPUBLICOF();
		}else if (area.equals("Senegal")){return WaterbodyOrCountry.SENEGALREPUBLICOF();
		}else if (area.equals("Sierra Leone")){return WaterbodyOrCountry.SIERRALEONEREPUBLICOF();
		}else if (area.equals("Sudan")){return WaterbodyOrCountry.SUDANDEMOCRATICREPUBLICOFTHE();
		}else if (area.equals("Madagascar")){return WaterbodyOrCountry.MADAGASCARREPUBLICOF();
		}else if (area.equals("Comoros")){return WaterbodyOrCountry.COMOROSUNIONOFTHE();
		
		}else if (area.equals("Vietnam")){return WaterbodyOrCountry.VIETNAMSOCIALISTREPUBLICOF();
		}else if (area.equals("Thailand")){return WaterbodyOrCountry.THAILANDKINGDOMOF();
		}else if (area.equals("Bhutan")){return WaterbodyOrCountry.BHUTANKINGDOMOF();
		}else if (area.equals("Laos")){return WaterbodyOrCountry.LAOPEOPLESDEMOCRATICREPUBLIC();
		}else if (area.equals("Myanmar (Burma)")){return WaterbodyOrCountry.MYANMAR();
		}else if (area.equals("Nepal")){return WaterbodyOrCountry.NEPALKINGDOMOF();
		}else if (area.equals("Pakistan")){return WaterbodyOrCountry.PAKISTANISLAMICREPUBLICOF();
		}else if (area.equals("Singapore")){return WaterbodyOrCountry.SINGAPOREREPUBLICOF();
			
		
		}else{	
			if (unhandledCountries.contains(area)){
//				logger.warn("Unhandled country '" + area + "' replaced by null" );
				return null;
			}
//			String warning = "New language abbreviation " + area;
//			logger.warn(warning);
			return null;
//			throw new IllegalArgumentException(warning);
		}
		
		
		
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getLanguageUuid(java.lang.String)
	 */
	@Override
	public UUID getLanguageUuid(String key)
			throws UndefinedTransformerMethodException {
		return super.getLanguageUuid(key);
	}
	
	
	
	
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getExtensionTypeByKey(java.lang.String)
	 */
	@Override
	public ExtensionType getExtensionTypeByKey(String key) throws UndefinedTransformerMethodException {
		if (key == null){return null;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getExtensionTypeUuid(java.lang.String)
	 */
	@Override
	public UUID getExtensionTypeUuid(String key)
			throws UndefinedTransformerMethodException {
		if (key == null){return null;
//		}else if (key.equalsIgnoreCase("recent only")){return uuidRecentOnly;
//		}else if (key.equalsIgnoreCase("recent + fossil")){return uuidRecentAndFossil;
//		}else if (key.equalsIgnoreCase("fossil only")){return uuidFossilOnly;
		}
		return null;
	}

	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase#getFeatureByKey(java.lang.String)
	 */
	@Override
	public Feature getFeatureByKey(String key) throws UndefinedTransformerMethodException {
		if (StringUtils.isBlank(key)){return null;
		}else if (key.equalsIgnoreCase("Distribution")){return Feature.DISTRIBUTION();
		}else if (key.equalsIgnoreCase("Ecology")){return Feature.ECOLOGY();
		}else if (key.equalsIgnoreCase("Diagnosis")){return Feature.DIAGNOSIS();
		}else if (key.equalsIgnoreCase("Biology")){return Feature.BIOLOGY_ECOLOGY();
		}else if (key.equalsIgnoreCase("Host")){return Feature.HOSTPLANT();
		}else{
			return null;
		}
	}

	
	
	
}
