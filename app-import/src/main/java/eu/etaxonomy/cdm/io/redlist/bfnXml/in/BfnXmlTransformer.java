/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

public final class BfnXmlTransformer {
    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(BfnXmlTransformer.class);


	private static final BiMap<String, Rank> rankMap = HashBiMap.create();
	static {
	    rankMap.put(BfnXmlConstants.RNK_INFRAGEN, Rank.INFRAGENUS());
	    rankMap.put(BfnXmlConstants.RNK_SUBGEN, Rank.SUBGENUS());
	    rankMap.put(BfnXmlConstants.RNK_GEN, Rank.GENUS());
	    //genus subdivision
	    rankMap.put(BfnXmlConstants.RNK_AGGR, Rank.SPECIESAGGREGATE());
	    rankMap.put(BfnXmlConstants.RNK_TAXINFRAGEN, Rank.INFRAGENERICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_SUBSER, Rank.SUBSERIES());
	    rankMap.put(BfnXmlConstants.RNK_SER, Rank.SERIES());
	    rankMap.put(BfnXmlConstants.RNK_SUBSECT, Rank.SUBSECTION_BOTANY());
	    rankMap.put(BfnXmlConstants.RNK_SECT, Rank.SECTION_BOTANY());
	    //species group
	    rankMap.put(BfnXmlConstants.RNK_SUBSP_AGGR, Rank.SUBSPECIFICAGGREGATE());
	    rankMap.put(BfnXmlConstants.RNK_SSP, Rank.SUBSPECIES());
	    rankMap.put(BfnXmlConstants.RNK_SPEZIES, Rank.SPECIES());
	    //below subspecies
	    rankMap.put(BfnXmlConstants.RNK_CAND, Rank.CANDIDATE());
	    rankMap.put(BfnXmlConstants.RNK_TAXINFRASP, Rank.INFRASPECIFICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_FSP, Rank.SPECIALFORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBSUBFM, Rank.SUBSUBFORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBFM, Rank.SUBFORM());
	    rankMap.put(BfnXmlConstants.RNK_FM, Rank.FORM());
	    rankMap.put(BfnXmlConstants.RNK_SUBSUBVAR, Rank.SUBSUBVARIETY());
	    rankMap.put(BfnXmlConstants.RNK_SUBVAR, Rank.SUBVARIETY());
	    rankMap.put(BfnXmlConstants.RNK_VAR, Rank.VARIETY());
	    rankMap.put(BfnXmlConstants.RNK_INFRASP, Rank.INFRASPECIES());
	    //above superfamily
	    rankMap.put(BfnXmlConstants.RNK_INFRAORD, Rank.INFRAORDER());
	    rankMap.put(BfnXmlConstants.RNK_ORD, Rank.ORDER());
	    rankMap.put(BfnXmlConstants.RNK_SUPERORD, Rank.SUPERORDER());
	    rankMap.put(BfnXmlConstants.RNK_INFRACL, Rank.INFRACLASS());
	    rankMap.put(BfnXmlConstants.RNK_SUBCL, Rank.SUBCLASS());
	    rankMap.put(BfnXmlConstants.RNK_CL, Rank.CLASS());
	    rankMap.put(BfnXmlConstants.RNK_SUPERCL, Rank.SUPERCLASS());
	    rankMap.put(BfnXmlConstants.RNK_INFRAPHYL_DIV, Rank.INFRAPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_SUBPHYL_DIV, Rank.SUBPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_PHYL_DIV, Rank.PHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_SUPERPHYL_DIV, Rank.SUPERPHYLUM());
	    rankMap.put(BfnXmlConstants.RNK_INFRAREG, Rank.INFRAKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_SUBREG, Rank.SUBKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_REG, Rank.KINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_SUPERREG, Rank.SUPERKINGDOM());
	    rankMap.put(BfnXmlConstants.RNK_DOM, Rank.DOMAIN());
	    rankMap.put(BfnXmlConstants.RNK_TAXSUPRAGEN, Rank.SUPRAGENERICTAXON());
	    rankMap.put(BfnXmlConstants.RNK_AUSWERTUNGSGRUPPE, Rank.EMPIRE());
	    //family group
	    rankMap.put(BfnXmlConstants.RNK_INFRAFAM, Rank.FAMILY());
	}

    public static String getRankCodeForRank(Rank rank) {
        return rankMap.inverse().get(rank);
    }

    /** Creates an cdm-Rank by the tcs rank
     */
    public static Rank getRankForRankCode (String rankCode){
        if (rankCode == null){
            return null;
        }
        //handle ambiguous key
        else if (rankCode.equals(BfnXmlConstants.RNK_AGG)){return Rank.SPECIESAGGREGATE();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBSP)){return Rank.SUBSPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBSP_DOT)){return Rank.SUBSPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_INFRAGEN)){return Rank.INFRAGENUS();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SP)){return Rank.SPECIES();
        }else if (rankCode.equals(BfnXmlConstants.RNK_F)){return Rank.FORM();
        }else if (rankCode.equals(BfnXmlConstants.RNK_VAR_DOT)){return Rank.VARIETY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBFAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_FAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUPERFAM)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_INTRATRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUBTRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_TRIB)){return Rank.FAMILY();
        }else if (rankCode.equals(BfnXmlConstants.RNK_SUPERTRIB)){return Rank.FAMILY();
        }else {
            return rankMap.get(rankCode);
        }
    }

    private static final BiMap<String, TaxonRelationshipType> relationshipTypeMap = HashBiMap.create();
    static {
        relationshipTypeMap.put("!=", TaxonRelationshipType.CONGRUENT_TO());
        relationshipTypeMap.put("!=,>", TaxonRelationshipType.CONGRUENT_OR_INCLUDES());
        relationshipTypeMap.put("!=,<", TaxonRelationshipType.CONGRUENT_OR_INCLUDED_OR_INCLUDES());
        relationshipTypeMap.put(">", TaxonRelationshipType.INCLUDES());
        relationshipTypeMap.put(">,><", TaxonRelationshipType.INCLUDES_OR_OVERLAPS());
        relationshipTypeMap.put("><", TaxonRelationshipType.OVERLAPS());
        relationshipTypeMap.put("~", TaxonRelationshipType.CONGRUENT_OR_INCLUDES_OR_OVERLAPS());//TODO Included in not here
        relationshipTypeMap.put("?", TaxonRelationshipType.ALL_RELATIONSHIPS());
        relationshipTypeMap.put("/=", TaxonRelationshipType.EXCLUDES());
    }

    public static String getConceptCodeForTaxonRelation(TaxonRelationshipType type) {
        return relationshipTypeMap.inverse().get(type);
    }

	public static TaxonRelationshipType getTaxonRelationForConceptCode(String conceptCode){
		if(conceptCode == null) {
			return null;
		}
        //handle ambiguous key
		else if(conceptCode.equalsIgnoreCase("=!")){
			return TaxonRelationshipType.CONGRUENT_TO();
		}else if(conceptCode.equalsIgnoreCase("\\")){
			return TaxonRelationshipType.EXCLUDES();
		}
		else{
			return relationshipTypeMap.get(conceptCode);
		}
	}

	public static String redListString2RedListCode (String redListCode) throws UnknownCdmTypeException{
		if (redListCode == null){ return null;
		}else if (redListCode.equals("#dtpl_RLKat_NULL#")){return "0";
		}else if (redListCode.equals("#dtpl_RLKat_EINS#")){return "1";
		}else if (redListCode.equals("#dtpl_RLKat_ZWEI#")){return "2";
		}else if (redListCode.equals("#dtpl_RLKat_DREI#")){return "3";
		}else if (redListCode.equals("#dtpl_RLKat_VIER#")){return "4";
		}else if (redListCode.equals("#dtpl_RLKat_G#")){return "G";
		}else if (redListCode.equals("#dtpl_RLKat_R#")){return "R";
		}else if (redListCode.equals("#dtpl_RLKat_V#")){return "V";
		}else if (redListCode.equals("#dtpl_RLKat_STERN#")){return "*";
		}else if (redListCode.equals("#dtpl_RLKat_STERN##dtpl_RLKat_STERN#")){return "**";
		}else if (redListCode.equals("#dtpl_RLKat_D#")){return "D";
		}else if (redListCode.equals("#dtpl_RLKat_RAUTE#")){
			char c = 0x2666;
			return String.valueOf(c);
		}else if (redListCode.equals("kN")){return "kN";
		}else if (redListCode.equals("+")){return "+";
		}else if (redListCode.equals("-")){return "-";
		}else if (redListCode.equals("=")){return "=";
		}else if (redListCode.equals("N")){return "N";
		}else if (redListCode.equals("S")){return "S";
		}else if (redListCode.equals("E")){return "E";
		}else if (redListCode.equals("D")){return "D";
		}else if (redListCode.equals("#dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER#")){
			char c = 0x2193;
			return String.valueOf(c)+String.valueOf(c)+String.valueOf(c);
		}else if (redListCode.equals("#dtpl_KurzfBest_RUNTER##dtpl_KurzfBest_RUNTER#")){
			char c = 0x2193;
			return String.valueOf(c)+String.valueOf(c);
		}else if (redListCode.equals("(#dtpl_KurzfBest_RUNTER#)")){
			char c = 0x2193;
			return "("+String.valueOf(c)+")";
		}else if (redListCode.equals("#dtpl_KurzfBest_HOCH#")){
			char c = 0x2191;
			return String.valueOf(c);
		}else if (redListCode.equals("#dtpl_Risiko_MINUS#")){return "-";
		}else if (redListCode.equals("#dtpl_VERANTW_NB#")){return "nb";
		}else if (redListCode.equals("#dtpl_TaxBez_GLEICH#")){return "=";
		}else if (redListCode.equals("#dtpl_TaxBez_KLEINER#")){return "<";
		}else if (redListCode.equals("#dtpl_TaxBez_GROESSER#")){return ">";
		}else if (redListCode.equals("#dtpl_TaxBez_UNGLEICH#")){
			char c = 0x2260;
			return String.valueOf(c);
		}else if (redListCode.equals("#dtpl_AlteRLKat_STERN#")){return "*";
		}else if (redListCode.equals("#dtpl_AlteRLKat_ZWEISTERN#")){return "**";
		}else if (redListCode.equals("#dtpl_AlteRLKat_NB#")){return "nb";
		}else if (redListCode.equals("#dtpl_AlteRLKat_KN#")){return "kN";
		}else if (redListCode.equals("#dtpl_TaxBez_UNGLEICH#")){return "-";
		}else if (StringUtils.isBlank(redListCode)){return "keine Angabe";
		}
		else {
			throw new UnknownCdmTypeException("Unknown Redlist Code " + redListCode);
		}
	}


	public static UUID getRedlistVocabularyUUID(String redListVocabulary) throws UnknownCdmTypeException {

		if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.uuidVocStateRLKat;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.uuidVocStateRlKatDiff;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.uuidVocStateRlAkt;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.uuidVocStateRLLang;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.uuidVocStateRLKurz;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.uuidVocStateRLRisk;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.uuidVocStateRLResp;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.uuidVocStateRLKatOld;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return BfnXmlConstants.uuidVocStateRLNeo;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return BfnXmlConstants.uuidVocStateRLKatId;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.uuidVocStateRLSpecialCases;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VORKOMMENSSTATUS)) {
		    return BfnXmlConstants.uuidVocGermanPresenceTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ETABLIERUNGSSTATUS)) {
		    return BfnXmlConstants.uuidVocGermanEstablishmentTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_BUNDESLAENDER)) {
            return BfnXmlConstants.uuidVocGermanFederalStates;
        } else{
			throw new UnknownCdmTypeException("Unknown Vocabulary feature, could not match: " + redListVocabulary);
		}

	}


	public static UUID getRedlistFeatureUUID(String redListFeature) throws UnknownCdmTypeException {

		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.uuidFeatureRLKat;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.uuidFeatureRlKatDiff;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.uuidFeatureRlAkt;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.uuidFeatureRLLang;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.uuidFeatureRLKurz;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.uuidFeatureRLRisk;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.uuidFeatureRLResp;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.uuidFeatureRLKatOld;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return BfnXmlConstants.uuidFeatureRLNeo;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return BfnXmlConstants.uuidFeatureRLKatId;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_TAXONOMIE)) {
            return BfnXmlConstants.uuidFeatureRLTaxComment;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_GEFAEHRDUNG)) {
            return BfnXmlConstants.uuidFeatureRLHazardComment;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.uuidFeatureRLSpecialCases;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_LETZTER_NACHWEIS)) {
            return BfnXmlConstants.uuidFeatureRLLastOccurrence;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.FEAT_WEITERE_KOMMENTARE)) {
            return BfnXmlConstants.uuidFeatureRLAdditionalComment;
        } else{
			throw new UnknownCdmTypeException("Unknown feature, could not match: " + redListFeature);
		}

	}

	public static UUID getRedlistStateTermUUID(String redListStateTerm, String redListFeature) throws UnknownCdmTypeException {
		//RL Kat
		char a = 0x2666;
		if(redListStateTerm.equalsIgnoreCase("0") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKat0;
        }
		if(redListStateTerm.equalsIgnoreCase("1") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKat1;
        }
		if(redListStateTerm.equalsIgnoreCase("2") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKat2;
        }
		if(redListStateTerm.equalsIgnoreCase("3") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKat3;
        }
		if(redListStateTerm.equalsIgnoreCase("G") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatG;
        }
		if(redListStateTerm.equalsIgnoreCase("R") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatR;
        }
		if(redListStateTerm.equalsIgnoreCase("V") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatV;
        }
		if(redListStateTerm.equalsIgnoreCase("*") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatStar;
        }
		if(redListStateTerm.equalsIgnoreCase("**") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatStar2;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatD;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(a)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatDiamond;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.stateTermRlKatKN;
        }

		//RL Diff
		if(redListStateTerm.equalsIgnoreCase("+") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.stateTermRLKatDiffPlus;
        }
		if(redListStateTerm.equalsIgnoreCase("-") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.stateTermRLKatDiffMinus;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.stateTermRLKatDiffEqual;
        }

		//Rl Akt
		if(redListStateTerm.equalsIgnoreCase("ex") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktEx;
        }
		if(redListStateTerm.equalsIgnoreCase("es") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktEs;
        }
		if(redListStateTerm.equalsIgnoreCase("ss") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktSs;
        }
		if(redListStateTerm.equalsIgnoreCase("s") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktS;
        }
		if(redListStateTerm.equalsIgnoreCase("mh") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktMh;
        }
		if(redListStateTerm.equalsIgnoreCase("h") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktH;
        }
		if(redListStateTerm.equalsIgnoreCase("sh") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktSh;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktQuest;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktNb;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.stateTermRLKatAktKn;
        }

		//RL Lang
		if(redListStateTerm.equalsIgnoreCase("<<<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangLT3;
        }
		if(redListStateTerm.equalsIgnoreCase("<<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangLT2;
        }
		if(redListStateTerm.equalsIgnoreCase("<") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangLT1;
        }
		if(redListStateTerm.equalsIgnoreCase("(<)") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangLT;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangEqual;
        }
		if(redListStateTerm.equalsIgnoreCase(">") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangGT;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatLangQuest;
        }

		//RL Kurz
		char c = 0x2193;
		char b = 0x2191;
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(c)+String.valueOf(c)+String.valueOf(c)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzDown3;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(c)+String.valueOf(c)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzDown2;
        }
		if(redListStateTerm.equalsIgnoreCase("("+String.valueOf(c)+")") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzDown1;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzEqual;
        }
		if(redListStateTerm.equalsIgnoreCase(String.valueOf(b)) && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzUp;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.stateTermRLKatKurzQuest;
        }

		//RL Risk
		if(redListStateTerm.equalsIgnoreCase("-") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.stateTermRLKatRiskMinus;
        }
		if(redListStateTerm.equalsIgnoreCase("=") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.stateTermRLKatRiskEqual;
        }

		//RL Resp
		if(redListStateTerm.equalsIgnoreCase("!!") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.stateTermRLKatRespBang2;
        }
		if(redListStateTerm.equalsIgnoreCase("!") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.stateTermRLKatRespBang1;
        }
		if(redListStateTerm.equalsIgnoreCase("(!)") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.stateTermRLKatRespBang;
        }
		if(redListStateTerm.equalsIgnoreCase("?") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.stateTermRLKatRespQuest;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.stateTermRLKatRespNb;
        }

		//RL Kat Old
		if(redListStateTerm.equalsIgnoreCase("0") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOld0;
        }
		if(redListStateTerm.equalsIgnoreCase("1") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOld1;
        }
		if(redListStateTerm.equalsIgnoreCase("2") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOld2;
        }
		if(redListStateTerm.equalsIgnoreCase("3") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOld3;
        }
		if(redListStateTerm.equalsIgnoreCase("G") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldG;
        }
		if(redListStateTerm.equalsIgnoreCase("R") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldR;
        }
		if(redListStateTerm.equalsIgnoreCase("V") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldV;
        }
		if(redListStateTerm.equalsIgnoreCase("*") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldStar;
        }
		if(redListStateTerm.equalsIgnoreCase("**") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldStar2;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldD;
        }
		if(redListStateTerm.equalsIgnoreCase("nb") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldNb;
        }
		if(redListStateTerm.equalsIgnoreCase("kN") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.stateTermRLKatOldKn;
        }

		//RL Neo
		if(redListStateTerm.equalsIgnoreCase("N") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return BfnXmlConstants.stateTermRLKatNeo;
        }

		//RL Special
		if(redListStateTerm.equalsIgnoreCase("S") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.stateTermRLSpecialS;
        }
		if(redListStateTerm.equalsIgnoreCase("E") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.stateTermRLSpecialE;
        }
		if(redListStateTerm.equalsIgnoreCase("D") && redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.stateTermRLSpecialD;
        }



		//RL Empty
		if(StringUtils.isBlank(redListStateTerm) || redListStateTerm.equalsIgnoreCase("keine Angabe")) {
            return BfnXmlConstants.stateTermEmpty;
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + redListStateTerm +"\n In Category: " + redListFeature);
        }

	}


	/**
	 * retrieve german federal States
	 *
	 * @param strGermanState
	 * @return
	 * @throws UnknownCdmTypeException
	 */
	public static UUID getGermanStateUUID(String strGermanState) throws UnknownCdmTypeException {

		if(strGermanState.equalsIgnoreCase("Deutschland")) {
            return UUID.fromString("a7f3855e-d4fa-4313-8fcf-da792ef848e7");
        }else if(strGermanState.equalsIgnoreCase("Baden-Württemberg")) {
            return UUID.fromString("00e64948-9ce9-4ebf-961b-133c56517b1c");
        }else if(strGermanState.equalsIgnoreCase("BW")) {
		    return UUID.fromString("00e64948-9ce9-4ebf-961b-133c56517b1c");
		}else if(strGermanState.equalsIgnoreCase("Bayern")) {
            return UUID.fromString("ba075265-368f-4ff0-8942-88546239c70a");
        }else if(strGermanState.equalsIgnoreCase("BY")) {
		    return UUID.fromString("ba075265-368f-4ff0-8942-88546239c70a");
		}else if(strGermanState.equalsIgnoreCase("Berlin")) {
            return UUID.fromString("d9339e12-7efa-45df-a008-3c934b9386bc");
        }else if(strGermanState.equalsIgnoreCase("BE")) {
		    return UUID.fromString("d9339e12-7efa-45df-a008-3c934b9386bc");
		}else if(strGermanState.equalsIgnoreCase("Bremen")) {
            return UUID.fromString("a6d2f97d-5dba-4b79-a073-25fb491b6320");
        }else if(strGermanState.equalsIgnoreCase("HB")) {
		    return UUID.fromString("a6d2f97d-5dba-4b79-a073-25fb491b6320");
		}else if(strGermanState.equalsIgnoreCase("Brandenburg")) {
            return UUID.fromString("dda9d8b8-8090-4667-953e-d8b1f7243926");
        }else if(strGermanState.equalsIgnoreCase("BB")) {
		    return UUID.fromString("dda9d8b8-8090-4667-953e-d8b1f7243926");
		}else if(strGermanState.equalsIgnoreCase("Hamburg")) {
            return UUID.fromString("f087a7d7-974f-4627-a414-df27c04f99dd");
        }else if(strGermanState.equalsIgnoreCase("HH")) {
		    return UUID.fromString("f087a7d7-974f-4627-a414-df27c04f99dd");
		}else if(strGermanState.equalsIgnoreCase("Hessen")) {
            return UUID.fromString("59de29e6-bf32-4677-89c7-a6834fcb5085");
        }else if(strGermanState.equalsIgnoreCase("HE")) {
		    return UUID.fromString("59de29e6-bf32-4677-89c7-a6834fcb5085");
		}else if(strGermanState.equalsIgnoreCase("Mecklenburg-Vorpommern")) {
            return UUID.fromString("06dccbd5-8d5a-4e4f-b56e-d1d74ab25c19");
        }else if(strGermanState.equalsIgnoreCase("MV")) {
		    return UUID.fromString("06dccbd5-8d5a-4e4f-b56e-d1d74ab25c19");
		}else if(strGermanState.equalsIgnoreCase("Niedersachsen") || strGermanState.equalsIgnoreCase("NI")) {
            return UUID.fromString("97f77fe8-07ab-4e14-8f8b-40e8caf7e653");
        }else if(strGermanState.equalsIgnoreCase("Nordrhein-Westfalen")||strGermanState.equalsIgnoreCase("NW")) {
            return UUID.fromString("46bf702e-1438-470c-9c77-04202c34ebf2");
        }else if(strGermanState.equalsIgnoreCase("Rheinland-Pfalz")||strGermanState.equalsIgnoreCase("RP")) {
            return UUID.fromString("dd3ddb29-b1ec-4937-99a9-4a94d383becf");
        }else if(strGermanState.equalsIgnoreCase("Saarland")||strGermanState.equalsIgnoreCase("SL")) {
            return UUID.fromString("26d3e85f-ce90-43ae-8ac0-42a60302b7b7");
        }else if(strGermanState.equalsIgnoreCase("Sachsen")||strGermanState.equalsIgnoreCase("SN")) {
            return UUID.fromString("ca3ef152-ee3a-45f2-8343-983cf0fdddbd");
        }else if(strGermanState.equalsIgnoreCase("Sachsen-Anhalt")|| strGermanState.equalsIgnoreCase("ST")) {
            return UUID.fromString("bb95b9a4-87ee-49bd-a542-4c30289e8d1f");
        }else if(strGermanState.equalsIgnoreCase("Schleswig-Holstein")||strGermanState.equalsIgnoreCase("SH")) {
            return UUID.fromString("863323a7-22fb-4070-ad94-ce317098a28a");
        }else if(strGermanState.equalsIgnoreCase("Thüringen")||strGermanState.equalsIgnoreCase("TH")) {
            return UUID.fromString("72e18526-6bf7-4300-8329-53cab5da2b51");
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strGermanState);
        }
	}

	public static UUID getGermanAbsenceTermUUID(String strGermanTerm) throws UnknownCdmTypeException {
	    if(strGermanTerm.equalsIgnoreCase("abwesend")) {return UUID.fromString("517c4c68-952e-4580-8379-66a4aa12c04b");}
	    else if(strGermanTerm.equalsIgnoreCase("abwesend - ausgestorben")) {return UUID.fromString("7a620705-7c0d-4c72-863f-f41d548a2cc5");}
	    else if(strGermanTerm.equalsIgnoreCase("abwesend - frühere Fehleingabe")) {return UUID.fromString("1009264c-197d-43d4-ba16-7a7f0a6fde0c");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend")) {return UUID.fromString("b294e7db-919f-4da0-9ba4-c374e7876aff");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - in Einbürgerung befindlich")) {return UUID.fromString("ec2f4099-82f7-44de-8892-09651c76d255");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - etabliert")) {return UUID.fromString("c1954b3c-58b5-43f3-b122-c872b2708bba");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - kultiviert, domestiziert")) {return UUID.fromString("99ebdb24-fda0-4203-9455-30441cdee17b");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - unbeständig")) {return UUID.fromString("12566e82-cdc2-48e4-951d-2fb88f30c5fd");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - Vorkommen unsicher")) {return UUID.fromString("a84d2ddb-fe7b-483b-96ba-fc0884d77c81");}
	    else if(strGermanTerm.equalsIgnoreCase("vorkommend - unsicher")) {return UUID.fromString("0b144b76-dab6-40da-8511-898f8226a24a");
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strGermanTerm);
        }
	}
    public static UUID getGermanEstablishmentTermUUID(String strGermanTerm) throws UnknownCdmTypeException {
        if(strGermanTerm.equalsIgnoreCase("Archaeophyt")) {return UUID.fromString("2cd2bc48-9fcb-4ccd-b03d-bafc0d3dde8c");}
        else if(strGermanTerm.equalsIgnoreCase("Indigen")) {return UUID.fromString("20a99907-406a-45f1-aa3e-4768697488e4");}
        else if(strGermanTerm.equalsIgnoreCase("Kulturpflanze / domestiziertes Tier")) {return UUID.fromString("94aa6408-f950-4e2e-bded-e01a1be859f6");}
        else if(strGermanTerm.equalsIgnoreCase("Neophyt")) {return UUID.fromString("fdf6f1b7-c6ad-4b49-bc6b-b06398f8b1b5");}
        else if(strGermanTerm.equalsIgnoreCase("Kultuflüchtling")) {return UUID.fromString("411f9190-56b7-41dd-a31a-3f200619c5e0");
        } else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strGermanTerm);
        }
    }


    /**
     * @param strDistributionValue
     * @return
     */
    public static UUID matchDistributionValue(String strDistributionValue) throws UnknownCdmTypeException {

        if(strDistributionValue.equalsIgnoreCase("*")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("0")){return getGermanAbsenceTermUUID("abwesend - ausgestorben");}
        else if(strDistributionValue.equalsIgnoreCase("1")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("2")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("3")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("G")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("D")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("R")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("N")){return getGermanAbsenceTermUUID("vorkommend");}
        else if(strDistributionValue.equalsIgnoreCase("V")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("nb")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("*")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_STERN_DP#")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_STERN#")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_STERN##dtpl_SynopseBL_STERN#")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase(" ")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_NB#")){return getGermanAbsenceTermUUID("vorkommend - etabliert");}
        else if(strDistributionValue.equalsIgnoreCase("-")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_X_KLAMMER#")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_X#")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_STRICH#")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("+")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("°")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("G/D")){return getGermanAbsenceTermUUID("vorkommend");}
        else if(strDistributionValue.equalsIgnoreCase("R/1")){return getGermanAbsenceTermUUID("vorkommend");}
        else if(strDistributionValue.equalsIgnoreCase("?")){return getGermanAbsenceTermUUID("vorkommend - Vorkommen unsicher");}
        else if(strDistributionValue.equalsIgnoreCase("#dtpl_SynopseBL_LEER#")){return getGermanAbsenceTermUUID("abwesend");}
        else {
            throw new UnknownCdmTypeException("Unknown State, could not match: " + strDistributionValue);
        }
    }
}
