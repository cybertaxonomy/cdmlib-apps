// $Id$
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
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @created 11.05.2009
 */
public class CubaImportState extends ExcelImportState<CubaImportConfigurator, ExcelRowBase>{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(CubaImportState.class);

	private final Map<String, Taxon> higherTaxonTaxonMap = new HashMap<String, Taxon>();
	private final Map<String, UUID> higherTaxonUuidMap = new HashMap<String, UUID>();

//	private CyprusRow cyprusRow;
//	private CyprusDistributionRow cyprusDistributionRow;

	private Classification classification;


	public CubaImportState(CubaImportConfigurator config) {
		super(config);
	}


	public boolean containsHigherTaxon(String higherName) {
		return higherTaxonTaxonMap.containsKey(higherName);
	}

	public Taxon putHigherTaxon(String higherName, Taxon taxon) {
		return higherTaxonTaxonMap.put(higherName, taxon);
	}

	public Taxon removeHigherTaxon(String higherName) {
		return higherTaxonTaxonMap.remove(higherName);
	}

	public Taxon getHigherTaxon(String higherName) {
		return higherTaxonTaxonMap.get(higherName);
	}


	public boolean containsHigherTaxonUuid(String higherName) {
		return higherTaxonUuidMap.containsKey(higherName);
	}

	public UUID putHigherTaxon(String higherName, UUID uuid) {
		return higherTaxonUuidMap.put(higherName, uuid);
	}

	public UUID removeHigherTaxonUuid(String higherName) {
		return higherTaxonUuidMap.remove(higherName);
	}

	public UUID getHigherTaxonUuid(String higherName) {
		return higherTaxonUuidMap.get(higherName);
	}


    /**
     * @return
     */
    public Classification getClassification() {
        return classification;
    }


    /**
     * @param classification the classification to set
     */
    public void setClassification(Classification classification) {
        this.classification = classification;
    }





//	/**
//	 * @return the cyprusRow
//	 */
//	public CyprusRow getCyprusRow() {
//		return cyprusRow;
//	}
//
//	/**
//	 * @param cyprusRow the normalExplicitRow to set
//	 */
//	public void setCyprusRow(CyprusRow cyprusRow) {
//		this.cyprusRow = cyprusRow;
//	}


//	/**
//	 * @return the cyprusRow
//	 */
//	public CyprusDistributionRow getCyprusDistributionRow() {
//		return cyprusDistributionRow;
//	}
//
//	/**
//	 * @param cyprusRow the normalExplicitRow to set
//	 */
//	public void setCyprusDistributionRow(CyprusDistributionRow cyprusRow) {
//		this.cyprusDistributionRow = cyprusRow;
//	}




}
