/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import java.util.EnumSet;

import eu.etaxonomy.cdm.io.out.ITaxonTreeExportTable;

/**
 * An enumeration with each instance representing a table type in the Output Model.
 *
 * @author a.mueller
 * @since 2023-08-05
 */
public enum KoperskiExportTable implements ITaxonTreeExportTable {

    CHECKLIST("Checklist", taxonColumnsKoperski()),
    CHECKLIST_CORLEY("Checklist_Corley_et_al", taxonColumns()),
    CHECKLIST_EARLY("Checklist_early", taxonColumns()),
    CHECKLIST_FRAHM_FREY("Checklist_Frahm_Frey", taxonColumns()),
    CHECKLIST_LUDWIG("Checklist_Ludwig", taxonColumns()),
    CHECKLIST_PATON("Checklist_Paton", taxonColumns()),
    CHECKLIST_SMITH("Checklist_Smith", taxonColumns()),

//    CONCEPT_RELATION("ConceptRelation", taxonConceptRelationColumns()),
    CONCEPT_RELATION_CORLEY("ConceptRelation_Corley_et_al", taxonConceptRelationColumns()),
    CONCEPT_RELATION_EARLY("ConceptRelation_early", taxonConceptRelationColumns()),
    CONCEPT_RELATION_FRAHM_FREY("ConceptRelation_Frahm_Frey", taxonConceptRelationColumns()),
    CONCEPT_RELATION_LUDWIG("ConceptRelation_Ludwig", taxonConceptRelationColumns()),
    CONCEPT_RELATION_PATON("ConceptRelation_Paton", taxonConceptRelationColumns()),
    CONCEPT_RELATION_SMITH("ConceptRelation_Smith", taxonConceptRelationColumns()),
    ;
	
	public static EnumSet<KoperskiExportTable> conceptChecklists() {
		return EnumSet.of(CHECKLIST_CORLEY, CHECKLIST_EARLY, CHECKLIST_FRAHM_FREY, 
				CHECKLIST_LUDWIG, CHECKLIST_PATON, CHECKLIST_SMITH);
	}
	
	public static EnumSet<KoperskiExportTable> conceptRelations() {
		return EnumSet.of(CONCEPT_RELATION_CORLEY, CONCEPT_RELATION_EARLY, CONCEPT_RELATION_FRAHM_FREY, 
				CONCEPT_RELATION_LUDWIG, CONCEPT_RELATION_PATON, CONCEPT_RELATION_SMITH);
	}

    //Taxon
    static final String VOLLNAME = "VOLLNAME";
    static final String TAXON_ID = "TAXON_ID";
    static final String TAXON_CHILD_OF = "TAXON_CHILD_OF";
    static final String SYN_FLAG = "SYN_FLAG";
    static final String ZITAT = "ZITAT";
    static final String FIDE = "FIDE";
    static final String KOMMENTAR_TAXON = "KOMMENTAR_TAXON";
    static final String SEC = "SEC";
    static final String KOMMENTAR_KONZEPT = "KOMMENTAR_KONZEPT";
    static final String TAX_UUID = "TAX_UUID";
    
    //Taxon ConceptRelation / Species Interaction
    static final String TAXID2 = "TAXID2";
    static final String NAME2 = "NAME2";
    static final String STATUS = "STATUS";
    static final String TAXID1 = "TAXID1";
    static final String NAME1 = "NAME1";

    private final static String[] taxonColumnsKoperski(){
        return new String[]{VOLLNAME, TAXON_ID, TAXON_CHILD_OF, SYN_FLAG, 
        		ZITAT, FIDE, KOMMENTAR_TAXON, SEC, KOMMENTAR_KONZEPT, TAX_UUID};
    }
    private final static String[] taxonColumns(){
        return new String[]{VOLLNAME, TAXON_ID, TAXON_CHILD_OF, SYN_FLAG, 
        		ZITAT, FIDE, KOMMENTAR_TAXON, SEC, KOMMENTAR_KONZEPT};
    }

    private final static String[] taxonConceptRelationColumns() {
        return new String[]{TAXID2, NAME2, STATUS, TAXID1, NAME1};
    }

    private String tableName;
    private String[] columnNames;

// ************** CONSTRUCTOR *******************/

    private KoperskiExportTable(String tableName, String[] columnNames){
        this.tableName = tableName;
        this.columnNames = columnNames;
    }

// ****************** GETTER / SETTER *************/

    @Override
    public String getTableName() {return tableName;}

    public int getSize(){ return columnNames.length;}

    public String[] getColumnNames(){return columnNames;}

    public int getIndex(String columnName) {
        int index= 0;
        for(String column : getColumnNames()){
            if (column.equals(columnName)){
                return index;
            }
            index++;
        }
        return -1;
    }
}