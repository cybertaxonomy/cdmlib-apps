/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @since 18.12.2015
 */
public class EdaphobaseImportTransformer extends InputTransformerBase {

    private static final long serialVersionUID = 1011498282020827250L;
    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    private static final UUID uuidTaxGrossgruppeMarker = UUID.fromString("71f22f44-5131-4d54-8362-1c77ac5c567a");
    private static final UUID uuidEdaphoRankMarker = UUID.fromString("6eaeffd2-b89b-436b-b0ee-75af5f0a9b81");

    protected static final UUID uuidVocFeatureQuantitative = UUID.fromString("8d449d74-5997-431f-af2d-409113b5b74e");
    protected static final UUID uuidVocFeatureBiological = UUID.fromString("f366ddf8-51da-436d-b4f4-8e86721adae6");
    protected static final UUID uuidVocFeatureMorpho = UUID.fromString("1ecf1157-6943-4790-b015-fde07c2d00d3");

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
//      else if (key.equals("Hyporder")){return Rank.HYPORDER();}

        //to be discussed, but handles epithet correctly during import
        else if (key.equals("Group")){return Rank.SPECIESGROUP();}
        return null;
    }

    @Override
    public UUID getMarkerTypeUuid(String key) throws UndefinedTransformerMethodException {
        if (key == null){
            return null;
        }else if (key.equals("TaxGrossgruppe")){
            return uuidTaxGrossgruppeMarker;
        }else if (key.equals("EdaphoRankMarker")){
            return uuidEdaphoRankMarker;
        }
        return null;
    }



}
