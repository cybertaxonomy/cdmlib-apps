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

	private static final BiMap<Rank, String> rankMap = HashBiMap.create();
	static {
	    rankMap.put(Rank.INFRAGENUS(), BfnXmlConstants.RNK_INFRAGEN);
	    rankMap.put(Rank.SUBGENUS(), BfnXmlConstants.RNK_SUBGEN);
	    rankMap.put(Rank.GENUS(), BfnXmlConstants.RNK_GEN);
	    //genus subdivision
	    rankMap.put(Rank.SPECIESAGGREGATE(), BfnXmlConstants.RNK_AGGR);
	    rankMap.put(Rank.INFRAGENERICTAXON(), BfnXmlConstants.RNK_TAXINFRAGEN);
	    rankMap.put(Rank.SUBSERIES(), BfnXmlConstants.RNK_SUBSER);
	    rankMap.put(Rank.SERIES(), BfnXmlConstants.RNK_SER);
	    rankMap.put(Rank.SUBSECTION_BOTANY(), BfnXmlConstants.RNK_SUBSECT);
	    rankMap.put(Rank.SECTION_BOTANY(), BfnXmlConstants.RNK_SECT);
	    //species group
	    rankMap.put(Rank.SUBSPECIFICAGGREGATE(), BfnXmlConstants.RNK_SUBSP_AGGR);
	    rankMap.put(Rank.SUBSPECIES(), BfnXmlConstants.RNK_SSP);
	    rankMap.put(Rank.SUBSPECIES(), BfnXmlConstants.RNK_SUBSP);
	    rankMap.put(Rank.SUBSPECIES(), BfnXmlConstants.RNK_SUBSP_DOT);
	    rankMap.put(Rank.SPECIES(), BfnXmlConstants.RNK_SP);
	    rankMap.put(Rank.SPECIES(), BfnXmlConstants.RNK_SPEZIES);
	    //below subspecies
	    rankMap.put(Rank.CANDIDATE(), BfnXmlConstants.RNK_CAND);
	    rankMap.put(Rank.INFRASPECIFICTAXON(), BfnXmlConstants.RNK_TAXINFRASP);
	    rankMap.put(Rank.SPECIALFORM(), BfnXmlConstants.RNK_FSP);
	    rankMap.put(Rank.SUBSUBFORM(), BfnXmlConstants.RNK_SUBSUBFM);
	    rankMap.put(Rank.SUBFORM(), BfnXmlConstants.RNK_SUBFM);
	    rankMap.put(Rank.FORM(), BfnXmlConstants.RNK_FM);
	    rankMap.put(Rank.SUBSUBVARIETY(), BfnXmlConstants.RNK_SUBSUBVAR);
	    rankMap.put(Rank.SUBVARIETY(), BfnXmlConstants.RNK_SUBVAR);
	    rankMap.put(Rank.VARIETY(), BfnXmlConstants.RNK_VAR);
	    rankMap.put(Rank.VARIETY(), BfnXmlConstants.RNK_VAR_DOT);
	    rankMap.put(Rank.INFRASPECIES(), BfnXmlConstants.RNK_INFRASP);
	    //above superfamily
	    rankMap.put(Rank.INFRAORDER(), BfnXmlConstants.RNK_INFRAORD);
	    rankMap.put(Rank.ORDER(), BfnXmlConstants.RNK_ORD);
	    rankMap.put(Rank.SUPERORDER(), BfnXmlConstants.RNK_SUPERORD);
	    rankMap.put(Rank.INFRACLASS(), BfnXmlConstants.RNK_INFRACL);
	    rankMap.put(Rank.SUBCLASS(), BfnXmlConstants.RNK_SUBCL);
	    rankMap.put(Rank.CLASS(), BfnXmlConstants.RNK_CL);
	    rankMap.put(Rank.SUPERCLASS(), BfnXmlConstants.RNK_SUPERCL);
	    rankMap.put(Rank.INFRAPHYLUM(), BfnXmlConstants.RNK_INFRAPHYL_DIV);
	    rankMap.put(Rank.SUBPHYLUM(), BfnXmlConstants.RNK_SUBPHYL_DIV);
	    rankMap.put(Rank.PHYLUM(), BfnXmlConstants.RNK_PHYL_DIV);
	    rankMap.put(Rank.SUPERPHYLUM(), BfnXmlConstants.RNK_SUPERPHYL_DIV);
	    rankMap.put(Rank.INFRAKINGDOM(), BfnXmlConstants.RNK_INFRAREG);
	    rankMap.put(Rank.SUBKINGDOM(), BfnXmlConstants.RNK_SUBREG);
	    rankMap.put(Rank.KINGDOM(), BfnXmlConstants.RNK_REG);
	    rankMap.put(Rank.SUPERKINGDOM(), BfnXmlConstants.RNK_SUPERREG);
	    rankMap.put(Rank.DOMAIN(), BfnXmlConstants.RNK_DOM);
	    rankMap.put(Rank.SUPRAGENERICTAXON(), BfnXmlConstants.RNK_TAXSUPRAGEN);
	    rankMap.put(Rank.EMPIRE(), BfnXmlConstants.RNK_AUSWERTUNGSGRUPPE);
	    //family group
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_INFRAFAM);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_SUBFAM);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_FAM);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_SUPERFAM);
	    //family subdivision
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_INTRATRIB);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_SUBTRIB);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_TRIB);
	    rankMap.put(Rank.FAMILY(), BfnXmlConstants.RNK_SUPERTRIB);
	}

    public static BiMap<Rank, String> getRankmap() {
        return rankMap;
    }


	public static TaxonRelationshipType concept2TaxonRelation(String conceptStatus) throws UnknownCdmTypeException{
		if(conceptStatus == null) {
			return null;
		}else if(conceptStatus.equalsIgnoreCase("!=")){
			return TaxonRelationshipType.CONGRUENT_TO();
		}else if(conceptStatus.equalsIgnoreCase("=!")){
			return TaxonRelationshipType.CONGRUENT_TO();
		}else if(conceptStatus.equalsIgnoreCase("!=,>")){
			return TaxonRelationshipType.CONGRUENT_OR_INCLUDES();
		}else if(conceptStatus.equalsIgnoreCase("!=,<")){
			return TaxonRelationshipType.CONGRUENT_OR_INCLUDED_OR_INCLUDES();
		}else if(conceptStatus.equalsIgnoreCase(">")){
			return TaxonRelationshipType.INCLUDES();
		}else if(conceptStatus.equalsIgnoreCase(">,><")){
			return TaxonRelationshipType.INCLUDES_OR_OVERLAPS();
//		}else if(conceptStatus.equalsIgnoreCase("<")){//TODO: should be just Included In
//			return TaxonRelationshipType.INCLUDED_OR_INCLUDES();
		}else if(conceptStatus.equalsIgnoreCase(">,><")){//TODO: should be Included In Or Overlaps
			return TaxonRelationshipType.INCLUDED_OR_INCLUDES_OR_OVERLAPS();
		}else if(conceptStatus.equalsIgnoreCase("><")){
			return TaxonRelationshipType.OVERLAPS();
		}else if(conceptStatus.equalsIgnoreCase("~")){//TODO Included in not here
			return TaxonRelationshipType.CONGRUENT_OR_INCLUDES_OR_OVERLAPS();
		}else if(conceptStatus.equalsIgnoreCase("?")){
			return TaxonRelationshipType.ALL_RELATIONSHIPS();
		}else if(conceptStatus.equalsIgnoreCase("/=")){
			return TaxonRelationshipType.EXCLUDES();
		}else if(conceptStatus.equalsIgnoreCase("\\")){
			return TaxonRelationshipType.EXCLUDES();
		}
		else{
			throw new UnknownCdmTypeException("Unknown concept relation status " + conceptStatus);
		}
	}


	/** Creates an cdm-Rank by the tcs rank
	 */
	public static Rank rankCode2Rank (String strRank) throws UnknownCdmTypeException{
		if (strRank == null){return null;
		//genus group
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRAGEN)){return Rank.INFRAGENUS();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBGEN)){return Rank.SUBGENUS();
		}else if (strRank.equals(BfnXmlConstants.RNK_GEN)){return Rank.GENUS();
		//genus subdivision
		//TODO
		}else if (strRank.equals(BfnXmlConstants.RNK_AGGR)){return Rank.SPECIESAGGREGATE();
		}else if (strRank.equals(BfnXmlConstants.RNK_AGG)){return Rank.SPECIESAGGREGATE();
		}else if (strRank.equals(BfnXmlConstants.RNK_TAXINFRAGEN)){return Rank.INFRAGENERICTAXON();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSER)){return Rank.SUBSERIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SER)){return Rank.SERIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSECT)){return Rank.SUBSECTION_BOTANY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SECT)){return Rank.SECTION_BOTANY();
		//species group
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSP_AGGR)){return Rank.SUBSPECIFICAGGREGATE();
		}else if (strRank.equals(BfnXmlConstants.RNK_SSP)){return Rank.SUBSPECIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSP)){return Rank.SUBSPECIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSP_DOT)){return Rank.SUBSPECIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SP)){return Rank.SPECIES();
		}else if (strRank.equals(BfnXmlConstants.RNK_SPEZIES)){return Rank.SPECIES();
		//below subspecies
		}else if (strRank.equals(BfnXmlConstants.RNK_CAND)){return Rank.CANDIDATE();
		}else if (strRank.equals(BfnXmlConstants.RNK_TAXINFRASP)){return Rank.INFRASPECIFICTAXON();
		}else if (strRank.equals(BfnXmlConstants.RNK_FSP)){return Rank.SPECIALFORM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSUBFM)){return Rank.SUBSUBFORM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBFM)){return Rank.SUBFORM();
		}else if (strRank.equals(BfnXmlConstants.RNK_FM)){return Rank.FORM();
		}else if (strRank.equals(BfnXmlConstants.RNK_F)){return Rank.FORM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBSUBVAR)){return Rank.SUBSUBVARIETY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBVAR)){return Rank.SUBVARIETY();
		}else if (strRank.equals(BfnXmlConstants.RNK_VAR)){return Rank.VARIETY();
		}else if (strRank.equals(BfnXmlConstants.RNK_VAR_DOT)){return Rank.VARIETY();
		//TODO -> see documentation, Bacteria status
//		}else if (strRank.equals("pv")){return Rank;
//		}else if (strRank.equals("bv")){return Rank.;
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRASP)){return Rank.INFRASPECIES();
		//above superfamily
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRAORD)){return Rank.INFRAORDER();
		}else if (strRank.equals(BfnXmlConstants.RNK_ORD)){return Rank.ORDER();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERORD)){return Rank.SUPERORDER();
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRACL)){return Rank.INFRACLASS();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBCL)){return Rank.SUBCLASS();
		}else if (strRank.equals(BfnXmlConstants.RNK_CL)){return Rank.CLASS();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERCL)){return Rank.SUPERCLASS();
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRAPHYL_DIV)){return Rank.INFRAPHYLUM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBPHYL_DIV)){return Rank.SUBPHYLUM();
		}else if (strRank.equals(BfnXmlConstants.RNK_PHYL_DIV)){return Rank.PHYLUM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERPHYL_DIV)){return Rank.SUPERPHYLUM();
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRAREG)){return Rank.INFRAKINGDOM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBREG)){return Rank.SUBKINGDOM();
		}else if (strRank.equals(BfnXmlConstants.RNK_REG)){return Rank.KINGDOM();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERREG)){return Rank.SUPERKINGDOM();
		}else if (strRank.equals(BfnXmlConstants.RNK_DOM)){return Rank.DOMAIN();
		}else if (strRank.equals(BfnXmlConstants.RNK_TAXSUPRAGEN)){return Rank.SUPRAGENERICTAXON();
		}else if (strRank.equals(BfnXmlConstants.RNK_AUSWERTUNGSGRUPPE)){return Rank.EMPIRE();
		//family group
		}else if (strRank.equals(BfnXmlConstants.RNK_INFRAFAM)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBFAM)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_FAM)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERFAM)){return Rank.FAMILY();
		//family subdivision
		}else if (strRank.equals(BfnXmlConstants.RNK_INTRATRIB)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUBTRIB)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_TRIB)){return Rank.FAMILY();
		}else if (strRank.equals(BfnXmlConstants.RNK_SUPERTRIB)){return Rank.FAMILY();
		}
		else {
			throw new UnknownCdmTypeException("Unknown Rank " + strRank);
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
            return BfnXmlConstants.vocStateRLKat;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.vocStateRlKatDiff;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.vocStateRlAkt;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.vocStateRLLang;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.vocStateRLKurz;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.vocStateRLRisk;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.vocStateRLResp;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.vocStateRLKatOld;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return BfnXmlConstants.vocStateRLNeo;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return BfnXmlConstants.vocStateRLKatId;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.vocStateRLSpecialCases;
        }else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_VORKOMMENSSTATUS)) {
		    return BfnXmlConstants.vocGermanPresenceTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_ETABLIERUNGSSTATUS)) {
		    return BfnXmlConstants.vocGermanEstablishmentTerms;
		}else if(redListVocabulary.equalsIgnoreCase(BfnXmlConstants.VOC_BUNDESLAENDER)) {
            return BfnXmlConstants.vocGermanFederalStates;
        } else{
			throw new UnknownCdmTypeException("Unknown Vocabulary feature, could not match: " + redListVocabulary);
		}

	}


	public static UUID getRedlistFeatureUUID(String redListFeature) throws UnknownCdmTypeException {

		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)) {
            return BfnXmlConstants.featureRLKat;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KAT)) {
            return BfnXmlConstants.featureRlKatDiff;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)) {
            return BfnXmlConstants.featureRlAkt;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.featureRLLang;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)) {
            return BfnXmlConstants.featureRLKurz;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)) {
            return BfnXmlConstants.featureRLRisk;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)) {
            return BfnXmlConstants.featureRLResp;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)) {
            return BfnXmlConstants.featureRLKatOld;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)) {
            return BfnXmlConstants.featureRLNeo;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)) {
            return BfnXmlConstants.featureRLKatId;
        }
		if(redListFeature.equalsIgnoreCase("Kommentar zur Taxonomie")) {
            return BfnXmlConstants.featureRLTaxComment;
        }
		if(redListFeature.equalsIgnoreCase("Kommentar zur Gefährdung")) {
            return BfnXmlConstants.featureRLHazardComment;
        }
		if(redListFeature.equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)) {
            return BfnXmlConstants.featureRLSpecialCases;
        }
		if(redListFeature.equalsIgnoreCase("Letzter Nachweis")) {
            return BfnXmlConstants.featureRLLastOccurrence;
        }
		if(redListFeature.equalsIgnoreCase("Weitere Kommentare")) {
            return BfnXmlConstants.featureRLAdditionalComment;
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
