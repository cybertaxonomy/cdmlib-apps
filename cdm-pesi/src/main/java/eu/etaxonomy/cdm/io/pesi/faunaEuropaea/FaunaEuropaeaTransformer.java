/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
public final class FaunaEuropaeaTransformer {
	private static final Logger logger = Logger.getLogger(FaunaEuropaeaTransformer.class);

	public static final UUID uuidFauEuArea = UUID.fromString("16325043-e7da-4742-b012-9ce03362a124");

	// Query
	public static final int Q_NO_RESTRICTION = -1;

	// TaxonStatus
	public static final int T_STATUS_ACCEPTED = 1;
	public static final int T_STATUS_NOT_ACCEPTED = 0;

	// Author
	public static final int A_AUCT = 1;
	public static final String A_AUCTNAME = "auct.";

	// Parenthesis
	public static final int P_PARENTHESIS = 1;

	// User

	public static final int U_ACTIVE = 1;

	//new AbsencePresenceTermUUIDs

	// Rank
	public static final int R_KINGDOM = 1;
	public static final int R_SUBKINGDOM = 2;
	public static final int R_SUPERPHYLUM = 3;
	public static final int R_PHYLUM = 4;
	public static final int R_SUBPHYLUM = 5;
	public static final int R_INFRAPHYLUM = 6;
	public static final int R_CLASS = 7;
	public static final int R_SUBCLASS = 8;
	public static final int R_INFRACLASS = 9;
	public static final int R_SUPERORDER = 10;
	public static final int R_ORDER = 11;
	public static final int R_SUBORDER = 12;
	public static final int R_INFRAORDER = 13;
	public static final int R_SUPERFAMILY = 14;
	public static final int R_FAMILY = 15;
	public static final int R_SUBFAMILY = 16;
	public static final int R_TRIBE = 17;
	public static final int R_SUBTRIBE = 18;
	public static final int R_GENUS = 19;
	public static final int R_SUBGENUS = 20;
	public static final int R_SPECIES = 21;
	public static final int R_SUBSPECIES = 22;

	public static PresenceAbsenceTerm occStatus2PresenceAbsence(int occStatusId){

		if (Integer.valueOf(occStatusId) == null) {
			return PresenceAbsenceTerm.PRESENT();
		}
		switch (occStatusId){
    		case 0: return PresenceAbsenceTerm.PRESENT();
    		case 2: return PresenceAbsenceTerm.ABSENT();
    		case 1: return PresenceAbsenceTerm.PRESENT_DOUBTFULLY();
    		default: {
    		    return null;
    		}
		}
	}


	public static PresenceAbsenceTerm occStatus2PresenceAbsence_ (int occStatusId)  throws UnknownCdmTypeException{
		switch (occStatusId){
			case 0: return null;
			//case 110: return AbsenceTerm.CULTIVATED_REPORTED_IN_ERROR();
			case 120: return PresenceAbsenceTerm.CULTIVATED();
		//	case 210: return AbsenceTerm.INTRODUCED_REPORTED_IN_ERROR();
			case 220: return PresenceAbsenceTerm.INTRODUCED_PRESENCE_QUESTIONABLE();
			case 230: return PresenceAbsenceTerm.INTRODUCED_FORMERLY_INTRODUCED();
			case 240: return PresenceAbsenceTerm.INTRODUCED_DOUBTFULLY_INTRODUCED();
			case 250: return PresenceAbsenceTerm.INTRODUCED();
			case 260: return PresenceAbsenceTerm.INTRODUCED_UNCERTAIN_DEGREE_OF_NATURALISATION();
			case 270: return PresenceAbsenceTerm.CASUAL();
			case 280: return PresenceAbsenceTerm.NATURALISED();
			//case 310: return AbsenceTerm.NATIVE_REPORTED_IN_ERROR();
			case 320: return PresenceAbsenceTerm.NATIVE_PRESENCE_QUESTIONABLE();
			case 330: return PresenceAbsenceTerm.NATIVE_FORMERLY_NATIVE();
			case 340: return PresenceAbsenceTerm.NATIVE_DOUBTFULLY_NATIVE();
			case 350: return PresenceAbsenceTerm.NATIVE();
			case 999: {
					logger.warn("endemic for EM can not be transformed in legal status");
					//TODO preliminary
					return PresenceAbsenceTerm.PRESENT();
				}
			default: {
				throw new UnknownCdmTypeException("Unknown occurrence status  (id=" + Integer.valueOf(occStatusId).toString() + ")");
			}
		}
	}


	public static Rank rankId2Rank (ResultSet rs, boolean useUnknown) throws UnknownCdmTypeException {
		Rank result;
		try {
			int rankId = rs.getInt("rnk_id");
			int parentRankId = rs.getInt("rnk_rnk_id");
			String rankName = rs.getString("rnk_name");
			String rankLatinName = rs.getString("rnk_latinname");
			int rankCategory = rs.getInt("rnk_category");

			if (logger.isDebugEnabled()) {
				logger.debug(rankId + ", " + parentRankId + ", " + rankName + ", " + rankCategory);
			}

			try {
				result = Rank.getRankByNameOrIdInVoc(rankName);
			} catch (UnknownCdmTypeException e1) {

				switch (rankId) {
				case 0: return null;
				case R_KINGDOM: return Rank.KINGDOM();
				case R_SUBKINGDOM: return Rank.SUBKINGDOM();
				case R_SUPERPHYLUM: return Rank.SUPERPHYLUM();
				case R_PHYLUM: return Rank.PHYLUM();
				case R_SUBPHYLUM: return Rank.SUBPHYLUM();
				case R_INFRAPHYLUM: return Rank.INFRAPHYLUM();
				case R_CLASS: return Rank.CLASS();
				case R_SUBCLASS: return Rank.SUBCLASS();
				case R_INFRACLASS: return Rank.INFRACLASS();
				case R_SUPERORDER: return Rank.SUPERORDER();
				case R_ORDER: return Rank.ORDER();
				case R_SUBORDER: return Rank.SUBORDER();
				case R_INFRAORDER: return Rank.INFRAORDER();
				case R_SUPERFAMILY: return Rank.SUPERFAMILY();
				case R_FAMILY: return Rank.FAMILY();
				case R_SUBFAMILY: return Rank.SUBFAMILY();
				case R_TRIBE: return Rank.TRIBE();
				case R_SUBTRIBE: return Rank.SUBTRIBE();
				case R_GENUS: return Rank.GENUS();
				case R_SUBGENUS: return Rank.SUBGENUS();
				case R_SPECIES: return Rank.SPECIES();
				case R_SUBSPECIES: return Rank.SUBSPECIES();

				default: {
					if (useUnknown){
						logger.error("Rank unknown. Created UNKNOWN_RANK");
						return Rank.UNKNOWN_RANK();
					}
					throw new UnknownCdmTypeException("Unknown Rank id" + Integer.valueOf(rankId).toString());
				}
				}
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warn("Exception occurred. Created UNKNOWN_RANK instead");
			return Rank.UNKNOWN_RANK();
		}
	}


	 	public final static HashMap<Integer, Language> languageFK2Language  = new HashMap<>();

        static
	 	{
	 		languageFK2Language.put(1, Language.ALBANIAN());
	 		languageFK2Language.put(4, Language.AZERBAIJANI());
	 		languageFK2Language.put(6, Language.BULGARIAN());
	 		languageFK2Language.put(10, Language.DANISH());
	 		languageFK2Language.put(11, Language.DUTCH_MIDDLE());
	 		languageFK2Language.put(12, Language.ENGLISH());
	 		languageFK2Language.put(16, Language.FRENCH());

	 		languageFK2Language.put(18, Language.GERMAN());
	 		languageFK2Language.put(19, Language.GREEK_MODERN());
	 		languageFK2Language.put(23, Language.HEBREW());
	 		languageFK2Language.put(24, Language.ITALIAN());
	 		languageFK2Language.put(26, Language.LITHUANIAN());
	 		languageFK2Language.put(30, Language.NORWEGIAN());
	 		languageFK2Language.put(31, Language.POLISH());
	 		languageFK2Language.put(34, Language.RUSSIAN());
	 		languageFK2Language.put(54, Language.SLOVAK());
	 		languageFK2Language.put(55, Language.SLOVENIAN());
	 		languageFK2Language.put(57, Language.SWEDISH());
	 		languageFK2Language.put(58, Language.TURKISH());

	 		languageFK2Language.put(59, Language.UKRAINIAN());
	 		languageFK2Language.put(60, Language.WELSH());
	 		languageFK2Language.put(62, Language.GALICIAN());
	 		//languageFK2Language.put(83, getEnglishUS());
	 		languageFK2Language.put(97, Language.IRISH());


	 		languageFK2Language.put(100, Language.NORWEGIAN_BOKMOL());
	 		languageFK2Language.put(101, Language.NORWEGIAN_NYNORSK());

	 		languageFK2Language.put(102, Language.ARABIC());
	 		languageFK2Language.put(103, Language.ARMENIAN());

	 		languageFK2Language.put(104, Language.CATALAN_VALENCIAN());
	 		languageFK2Language.put(105, Language.CHINESE());
	 		languageFK2Language.put(106, Language.ESTONIAN());
	 		languageFK2Language.put(107, Language.FINNISH());

	 		languageFK2Language.put(108, Language.GAELIC_SCOTTISH_GAELIC());
	 		languageFK2Language.put(109, Language.JAPANESE());
	 		languageFK2Language.put(110, Language.KOREAN());
	 		languageFK2Language.put(111, Language.LATIN());
	 		languageFK2Language.put(112, Language.LATVIAN());
	 		languageFK2Language.put(113, Language.PERSIAN());
	 		languageFK2Language.put(114, Language.PORTUGUESE());
	 		languageFK2Language.put(115, Language.ROMANIAN());
	 		languageFK2Language.put(116, Language.GAELIC_SCOTTISH_GAELIC());
	 		languageFK2Language.put(117, Language.SWAHILI());
	 		languageFK2Language.put(118, Language.SPANISH_CASTILIAN());
	 	}

	static NomenclaturalStatusType nomStatusTempNamed;

    private static DefinedTerm taxonomicSpecialistType;

    private static UUID uuidTaxonomicSpecialistType = UUID.fromString("006879e4-cf99-405a-a720-2e81d9cbc34c");

    private static DefinedTerm groupCoordinatorType;

    private static UUID uuidGroupCoordinatorType = UUID.fromString("3a827ebe-4410-40e5-a241-941b17028e11");

	private static DefinedTerm associateSpecialistType;

	private static UUID uuidAssociateSpecialistType = UUID.fromString("8258f73c-e0ad-4f87-a88c-53c58c08bba9");

	private static Language langEnglishUS;

	private static UUID uuidEnglishUS;

	public static NomenclaturalStatusType getNomStatusTempNamed(ITermService termService){
		if (nomStatusTempNamed == null){
			nomStatusTempNamed = (NomenclaturalStatusType)termService.find(PesiTransformer.uuidNomStatusTemporaryName);
			if (nomStatusTempNamed == null){
				nomStatusTempNamed = NomenclaturalStatusType.NewInstance("temporary named", "temporary named", "temp. named", Language.ENGLISH());
				Representation repLatin = Representation.NewInstance("", "", "", Language.LATIN());
				nomStatusTempNamed.addRepresentation(repLatin);
				nomStatusTempNamed.setUuid(PesiTransformer.uuidNomStatusTemporaryName);
				NomenclaturalStatusType.ALTERNATIVE().getVocabulary().addTerm(nomStatusTempNamed);
				termService.save(nomStatusTempNamed);
			}
		}
		return nomStatusTempNamed;
	}

	public static Language getEnglishUS(ITermService termService){
		if (langEnglishUS == null){
			langEnglishUS = (Language)termService.find(uuidEnglishUS);
            if (langEnglishUS == null){
            	logger.info("create language english-us");
            	langEnglishUS = Language.NewInstance("english-United States", "english-US", "eng-US");
    			langEnglishUS.setUuid(uuidEnglishUS);

                langEnglishUS = (Language)termService.save(langEnglishUS);
                languageFK2Language.put(83, langEnglishUS);
            }
        }
		return langEnglishUS;
	}

	public static DefinedTerm getTaxonomicSpecialistType(ITermService termService) {
        if (taxonomicSpecialistType == null){
            taxonomicSpecialistType = (DefinedTerm)termService.find(uuidTaxonomicSpecialistType);
            if (taxonomicSpecialistType == null){
            	logger.info("create taxonomic specialist type");
                taxonomicSpecialistType = DefinedTerm.NewInstance(TermType.TaxonNodeAgentRelationType, "taxonomic specialist", "taxonomic specialist", "TS");

                taxonomicSpecialistType.setUuid(uuidTaxonomicSpecialistType);

                termService.save(taxonomicSpecialistType);
            }
        }
        return taxonomicSpecialistType;
    }

	public static DefinedTerm getGroupCoordinatorType(ITermService termService) {
        if (groupCoordinatorType == null){
            groupCoordinatorType = (DefinedTerm)termService.find(uuidGroupCoordinatorType);
            if (groupCoordinatorType == null){
                groupCoordinatorType = DefinedTerm.NewInstance(TermType.TaxonNodeAgentRelationType, "group coordinator", "group coordinator", "GC");

                groupCoordinatorType.setUuid(uuidGroupCoordinatorType);

                termService.save(groupCoordinatorType);
            }
        }
        return groupCoordinatorType;
    }

    public static DefinedTerm getAssociateSpecialistType(ITermService termService) {
        if (associateSpecialistType == null){
        	associateSpecialistType = (DefinedTerm)termService.find(uuidAssociateSpecialistType);
            if (associateSpecialistType == null){
            	associateSpecialistType = DefinedTerm.NewInstance(TermType.TaxonNodeAgentRelationType, "associate specialist", "associate specialist", "AS");

            	associateSpecialistType.setUuid(uuidAssociateSpecialistType);

                termService.save(associateSpecialistType);
            }
        }
        return associateSpecialistType;
    }

	public static Language langFK2Language(Integer languageFk) {
	    Language result = languageFK2Language.get(languageFk);
		return result;
	}
}
