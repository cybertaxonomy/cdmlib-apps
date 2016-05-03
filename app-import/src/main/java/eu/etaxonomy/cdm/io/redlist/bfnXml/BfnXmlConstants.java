// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.redlist.bfnXml.out.DoctronicDocumentBuilder;
import eu.etaxonomy.cdm.model.common.Language;

/**
 * @author pplitzner
 * @date May 3, 2016
 *
 */
public class BfnXmlConstants {
    public static final Logger logger = Logger.getLogger(DoctronicDocumentBuilder.class);

    public static final Language defaultLanguage = Language.DEFAULT();

    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String EL_DEB_EXPORT = "DEBExport";
    public static final String EL_ROTELISTEDATEN = "ROTELISTEDATEN";

    public static final String EL_EIGENSCHAFTEN = "EIGENSCHAFTEN";
    public static final String EL_EIGENSCHAFT = "EIGENSCHAFT";
    public static final String ATT_STANDARDNAME = "standardname";

    public static final String EL_METAINFOS = "METAINFOS";
    public static final String EL_MWERT = "MWERT";
    public static final String ATT_NAME = "name";

    public static final String EL_LISTENWERTE = "LISTENWERTE";
    public static final String EL_LWERT = "LWERT";
    public static final String ATT_REIHENFOLGE = "reihenfolge";

    public static final String EL_RAUMINFOS = "RAUMINFOS";
    public static final String EL_RAUM = "RAUM";
    public static final String EL_RAUM_WERT = "RWERT";

    public static final String EL_TAXONYME = "TAXONYME";
    public static final String EL_TAXONYM = "TAXONYM";
    public static final String ATT_TAXNR = "taxNr";

    public static final String EL_WISSNAME = "WISSNAME";
    public static final String EL_NANTEIL = "NANTEIL";
    public static final String ATT_BEREICH = "bereich";

    public static final String EL_DEUTSCHENAMEN = "DEUTSCHENAMEN";
    public static final String EL_DNAME = "DNAME";
    public static final String ATT_SEQUENZ = "sequenz";
    public static final String EL_TRIVIALNAME = "TRIVIALNAME";
    public static final String EL_GRUPPE = "GRUPPE";
    public static final String EL_SPEZIFISCH = "SPEZIFISCH";

    public static final String EL_INFORMATIONEN = "INFORMATIONEN";
    public static final String EL_BEZUGSRAUM = "BEZUGSRAUM";
    public static final String EL_IWERT = "IWERT";
    public static final String EL_WERT = "WERT";

    public static final String EL_KONZEPTBEZIEHUNGEN = "KONZEPTBEZIEHUNGEN";
    public static final String EL_KONZEPTBEZIEHUNG = "KONZEPTBEZIEHUNG";

    public static final String RNK_SUPERTRIB = "supertrib";

    public static final String RNK_TRIB = "trib";

    public static final String RNK_SUBTRIB = "subtrib";

    public static final String RNK_INTRATRIB = "intratrib";

    public static final String RNK_SUPERFAM = "superfam";

    public static final String RNK_FAM = "fam";

    public static final String RNK_SUBCL = "subcl";

    public static final String RNK_SPEZIES = "spezies";

    public static final String RNK_SSP = "ssp";

    public static final String RNK_SUBFAM = "subfam";

    public static final String RNK_INFRAFAM = "infrafam";

    public static final String RNK_AUSWERTUNGSGRUPPE = "Auswertungsgruppe";

    public static final String RNK_TAXSUPRAGEN = "taxsupragen";

    public static final String RNK_DOM = "dom";

    public static final String RNK_SUPERREG = "superreg";

    public static final String RNK_REG = "reg";

    public static final String RNK_SUBREG = "subreg";

    public static final String RNK_INFRAREG = "infrareg";

    public static final String RNK_SUPERPHYL_DIV = "superphyl_div";

    public static final String RNK_PHYL_DIV = "phyl_div";

    public static final String RNK_SUBPHYL_DIV = "subphyl_div";

    public static final String RNK_INFRAPHYL_DIV = "infraphyl_div";

    public static final String RNK_SUPERCL = "supercl";

    public static final String RNK_CL = "cl";

    public static final String RNK_INFRACL = "infracl";

    public static final String RNK_SUPERORD = "superord";

    public static final String RNK_ORD = "ord";

    public static final String RNK_INFRAORD = "infraord";

    public static final String RNK_INFRASP = "infrasp";

    public static final String RNK_VAR_DOT = "var.";

    public static final String RNK_VAR = "var";

    public static final String RNK_SUBVAR = "subvar";

    public static final String RNK_SUBSUBVAR = "subsubvar";

    public static final String RNK_F = "f.";

    public static final String RNK_FM = "fm";

    public static final String RNK_SUBFM = "subfm";

    public static final String RNK_SUBSUBFM = "subsubfm";

    public static final String RNK_FSP = "fsp";

    public static final String RNK_TAXINFRASP = "taxinfrasp";

    public static final String RNK_CAND = "cand";

    public static final String RNK_SP = "sp";

    public static final String RNK_SUBSP = "subsp";

    public static final String RNK_SUBSP_DOT = "subsp.";

    public static final String RNK_SUBSP_AGGR = "subsp_aggr";

    public static final String RNK_SECT = "sect";

    public static final String RNK_SUBSECT = "subsect";

    public static final String RNK_SER = "ser";

    public static final String RNK_SUBSER = "subser";

    public static final String RNK_TAXINFRAGEN = "taxinfragen";

    public static final String RNK_AGG = "agg.";

    public static final String RNK_AGGR = "aggr";

    public static final String RNK_GEN = "gen";

    public static final String RNK_SUBGEN = "subgen";

    public static final String RNK_INFRAGEN = "infragen";

    public static final String VOC_BUNDESLAENDER = "Bundesländer";

    public static final String VOC_ETABLIERUNGSSTATUS = "Etablierungsstatus";

    public static final String VOC_VORKOMMENSSTATUS = "Vorkommensstatus";

    public static final String VOC_SONDERFAELLE = "Sonderfälle";

    public static final String VOC_EINDEUTIGER_CODE = "Eindeutiger Code";

    public static final String VOC_NEOBIOTA = "Neobiota";

    public static final String VOC_ALTE_RL_KAT = "alte RL- Kat.";

    public static final String VOC_VERANTWORTLICHKEIT = "Verantwortlichkeit";

    public static final String VOC_RISIKOFAKTOREN = "Risikofaktoren";

    public static final String VOC_KURZFRISTIGER_BESTANDSTREND = "kurzfristiger Bestandstrend";

    public static final String VOC_LANGFRISTIGER_BESTANDSTREND = "langfristiger Bestandstrend";

    public static final String VOC_AKTUELLE_BESTANDSSTITUATION = "aktuelle Bestandsstituation";

    public static final String VOC_KAT = "Kat. +/-";

    public static final String VOC_RL_KAT = "RL Kat.";
}
