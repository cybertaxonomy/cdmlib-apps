// $Id$
/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @date 18.12.2015
 *
 */
public class EdaphobaseImportTransformer extends InputTransformerBase {
    private static final long serialVersionUID = 1011498282020827250L;
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseImportTransformer.class);

    @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (key == null){return null;}
        if (key.equals("Phylum")){return Rank.PHYLUM();}
        else if (key.equals("Subphylum")){return Rank.SUBPHYLUM();}
        else if (key.equals("Superclass")){return Rank.SUPERCLASS();}
        else if (key.equals("Class")){return Rank.CLASS();}
        else if (key.equals("Subclass")){return Rank.SUBCLASS();}
        else if (key.equals("Superorder")){return Rank.SUPERORDER();}
        else if (key.equals("Order")){return Rank.ORDER();}
        else if (key.equals("Suborder")){return Rank.SUBORDER();}
//        else if (key.equals("Supercohort")){return Rank.SUPERCOHORT();}
//        else if (key.equals("Cohort")){return Rank.COHORT();}
//        else if (key.equals("Subcohort")){return Rank.SUBCOHORT();}
        else if (key.equals("Superfamily")){return Rank.SUPERFAMILY();}
        else if (key.equals("Family")){return Rank.FAMILY();}
        else if (key.equals("Subfamily")){return Rank.SUBFAMILY();}
        else if (key.equals("Tribe")){return Rank.TRIBE();}
        else if (key.equals("Genus")){return Rank.GENUS();}
        else if (key.equals("Subgenus")){return Rank.SUBGENUS();}
        else if (key.equals("Species")){return Rank.SPECIES();}
        else if (key.equals("Subspecies")){return Rank.SUBSPECIES();}
        else if (key.equals("Variety")){return Rank.VARIETY();}
        else if (key.equals("Form")){return Rank.FORM();}
        else if (key.equals("Infraorder")){return Rank.INFRAORDER();}
//        else if (key.equals("Hyporder")){return Rank.HYPORDER();}
//        else if (key.equals("Group")){return Rank.GROUP();}
        return null;
    }

//    @Override
//    public UUID getRankUuid(String key) throws UndefinedTransformerMethodException {
//        // TODO Auto-generated method stub
//        return super.getRankUuid(key);
//    }



}