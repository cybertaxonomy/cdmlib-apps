/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.cuba;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @created 11.05.2009
 */
public class CubaImportState extends ExcelImportState<CubaImportConfigurator, ExcelRowBase>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(CubaImportState.class);

	private final Map<String, Taxon> higherTaxonTaxonMap = new HashMap<>();

	private final Map<String, UUID> higherTaxonUuidMap = new HashMap<>();

	private final Map<String, IBotanicalName> familyNameMap = new HashMap<>();

	//classification
	private Classification classification;
    public Classification getClassification() {return classification;}
    public void setClassification(Classification classification) {this.classification = classification;}

    //current taxon
    private Taxon currentTaxon;
    public Taxon getCurrentTaxon() {return currentTaxon;}
    public void setCurrentTaxon(Taxon currentTaxon) {this.currentTaxon = currentTaxon;}

    //rootNode
    private TaxonNode rootNode;
    public void setRootNode(TaxonNode rootNode) {this.rootNode = rootNode;}
    public TaxonNode getRootNode() { return rootNode;}

    private Reference secReference;
    public Reference getSecReference() {return secReference;}
    public void setSecReference(Reference secReference) {this.secReference = secReference;}

    private PresenceAbsenceTerm highestStatusForTaxon;
    public PresenceAbsenceTerm getHighestStatusForTaxon(){return highestStatusForTaxon;}
    public void setHighestStatusForTaxon(PresenceAbsenceTerm highestStatusForTaxon){this.highestStatusForTaxon = highestStatusForTaxon;}

    private boolean isCubanProvince;
    public void setCubanProvince(boolean cubanProvince) {this.isCubanProvince = cubanProvince;}
    public boolean isCubanProvince(){return isCubanProvince;}

    private boolean taxonIsAbsent = false;
    public boolean isTaxonIsAbsent(){return this.taxonIsAbsent;}
    public void setTaxonIsAbsent(boolean taxonIsAbsent) {this.taxonIsAbsent = taxonIsAbsent;}

    private boolean isEndemic = false;
    public boolean isEndemic(){return this.isEndemic;}
    public void setEndemic(boolean isEndemic) {this.isEndemic = isEndemic;}

    //Constructor
    public CubaImportState(CubaImportConfigurator config) {
		super(config);
	}


    //higher taxon
    public Taxon getHigherTaxon(String higherName) {
        return higherTaxonTaxonMap.get(higherName);
    }
	public Taxon putHigherTaxon(String higherName, Taxon taxon) {
		return higherTaxonTaxonMap.put(higherName, taxon);
	}
	public Taxon removeHigherTaxon(String higherName) {
		return higherTaxonTaxonMap.remove(higherName);
	}
    public boolean containsHigherTaxon(String higherName) {
        return higherTaxonTaxonMap.containsKey(higherName);
    }

    //higher taxon uuid
    public UUID getHigherTaxonUuid(String higherName) {
        return higherTaxonUuidMap.get(higherName);
    }
	public UUID putHigherTaxon(String higherName, UUID uuid) {
		return higherTaxonUuidMap.put(higherName, uuid);
	}
	public UUID removeHigherTaxonUuid(String higherName) {
		return higherTaxonUuidMap.remove(higherName);
	}
    public boolean containsHigherTaxonUuid(String higherName) {
        return higherTaxonUuidMap.containsKey(higherName);
    }

    //family names
    public IBotanicalName getFamilyName(String familyStr) {
        return familyNameMap.get(familyStr);
    }
    public void putFamilyName(String familyStr, IBotanicalName name) {
        familyNameMap.put(familyStr, name);
    }


    Map<UUID, Reference> refMap = new HashMap<UUID, Reference>();
    //reference
    public Reference getReference(UUID uuidRef) {
        return refMap.get(uuidRef);
    }
    public void putReference(UUID uuidRef, Reference ref) {
        refMap.put(uuidRef, ref);
    }




}
