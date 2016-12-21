/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.iapt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @created 11.05.2009
 */
public class IAPTImportState extends SimpleExcelTaxonImportState<IAPTImportConfigurator> {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(IAPTImportState.class);

	private final Map<String, Taxon> higherTaxonTaxonMap = new HashMap<String, Taxon>();

	private final Map<String, UUID> higherTaxonUuidMap = new HashMap<String, UUID>();

    private final Map<String, Taxon> genusTaxonMap = new HashMap<String, Taxon>();

	private final Map<String, BotanicalName> familyNameMap = new HashMap<String, BotanicalName>();

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

    //Constructor
    public IAPTImportState(IAPTImportConfigurator config) {
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

    public Map<String, Taxon> getGenusTaxonMap() {
        return genusTaxonMap;
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
