// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 *
 * @author pplitzner
 * @date Mar 1, 2016
 *
 */
@SuppressWarnings("serial")
public final class RedListGefaesspflanzenTransformer extends InputTransformerBase {

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenTransformer.class);

    @Override
    public UUID getRankUuid(String key) throws UndefinedTransformerMethodException {
        if (key == null){return null;}
        if (key.equals("SAM")){return RedListUtil.uuidRankCollectionSpecies;}
        return null;
    }

    @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (key == null){return null;}
        if (key.equals("GAT")){return Rank.GENUS();}
        else if (key.equals("SPE")){return Rank.SPECIES();}
        else if (key.equals("VAR")){return Rank.VARIETY();}
        else if (key.equals("SSP")){return Rank.SUBSPECIES();}
        else if (key.equals("FOR")){return Rank.FORM();}
        else if (key.equals("RAC")){return Rank.RACE();}
        else if (key.equals("SEC")){return Rank.SECTION_BOTANY();}
        else if (key.equals("SSE")){return Rank.SUBSECTION_BOTANY();}
        else if (key.equals("SGE")){return Rank.SUBGENUS();}
        else if (key.equals("SVA")){return Rank.SUBVARIETY();}
        else if (key.equals("CV")){return Rank.CULTIVAR();}
        else if (key.equals("PRO")){return Rank.PROLES();}
        else if (key.equals("SER")){return Rank.SERIES();}
        else if (key.equals("GRE")){return Rank.GREX();}
        else if (key.equals("AGG")){return Rank.SPECIESAGGREGATE();}
        else if (key.equals("SFO")){return Rank.SUBFORM();}
        else if (key.equals("FAM")){return Rank.FAMILY();}
        else if (key.equals("SLU")){return Rank.SUBLUSUS();}
        else if (key.equals("SPI")){return Rank.SUBSPECIES();}
        else if (key.equals("GRO") || key.equals("GRU")){return Rank.SPECIESGROUP();}
        else if (key.equals("?")){return Rank.UNKNOWN_RANK();}
        return null;
    }

    @Override
    public NomenclaturalStatusType getNomenclaturalStatusByKey(String key) throws UndefinedTransformerMethodException {
        if (key == null){return null;}
        key = key.trim();
        if (key.equals("nom. cons.")){return NomenclaturalStatusType.CONSERVED();}
        else if (key.equals("nom. illeg.")){return NomenclaturalStatusType.ILLEGITIMATE();}
        else if (key.equals("nom. inval.")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. inval. (sine typus)")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. inval. (sine descr.)")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. inval. (sine descr. lat.)")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. inval. (sine basion.)")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. inval. (nom. non accept.)")){return NomenclaturalStatusType.INVALID();}
        else if (key.equals("nom. ambig.")){return NomenclaturalStatusType.AMBIGUOUS();}
        else if (key.equals("nom. nud.")){return NomenclaturalStatusType.NUDUM();}
        else if (key.equals("nom. dub.")){return NomenclaturalStatusType.DOUBTFUL();}
        else if (key.equals("nom. rejic.")){return NomenclaturalStatusType.REJECTED();}
        else if (key.startsWith("nom. rejic. (")){return NomenclaturalStatusType.REJECTED();}
        else if (key.equals("nom. rejic. prop.")){return NomenclaturalStatusType.REJECTED_PROP();}
        else if (key.equals("nom. utique rejic.")){return NomenclaturalStatusType.UTIQUE_REJECTED();}
        else if (key.equals("nom. utique rejic. pro.")){return NomenclaturalStatusType.UTIQUE_REJECTED_PROP();}
        else if (key.equals("nom. utique rejic. prop.")){return NomenclaturalStatusType.UTIQUE_REJECTED_PROP();}
        else if (key.equals("comb. nov.")){return NomenclaturalStatusType.COMB_NOV();}
        else if (key.equals("nom. illeg. (nom. superfl.)")){return NomenclaturalStatusType.SUPERFLUOUS();}
        else if (key.equals("nom. inval. (nom. nud.)")){return NomenclaturalStatusType.NUDUM();}
        else if (key.equals("nom. nud.")){return NomenclaturalStatusType.NUDUM();}
        else if (key.equals("nom. inval. (op. utique oppr.)")){return NomenclaturalStatusType.OPUS_UTIQUE_OPPR();}
        else if (key.equals("nom. inval. (nom. prov.)")){return NomenclaturalStatusType.PROVISIONAL();}
        return null;
    }

    public TaxonRelationshipType getTaxonRelationshipTypeByKey(String key) {
        if (key == null){return null;}
        else {
            if (key.equals("=")){return TaxonRelationshipType.CONGRUENT_TO();}
            else if (key.equals(">")){return TaxonRelationshipType.INCLUDES();}
            else if (key.equals("<")){return TaxonRelationshipType.INCLUDES();}
            else if (key.equals("!")){return TaxonRelationshipType.OVERLAPS();}
            else if (key.equals("?")){return TaxonRelationshipType.UNCLEAR();}
            else if (key.equals("x")){return TaxonRelationshipType.NOT_YET_WORKED_ON();}
        }
        return null;
    }

}
