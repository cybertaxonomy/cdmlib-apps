/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
public class SimpleExcelTaxonImportState<CONFIG extends ExcelImportConfiguratorBase>
        extends ExcelImportState<CONFIG, ExcelRowBase>{

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    private final Map<String, Reference> refMap = new HashMap<>();

    private final Map<String, TeamOrPersonBase<?>> agentMap = new HashMap<>();

    private final Map<String, Taxon> higherTaxonTaxonMap = new HashMap<>();

    //using titleCache
    private Map<String, TaxonName> nameMap = new HashMap<>();

    private final Map<String, Taxon> taxonMap = new HashMap<>();

// ************************* CONSTRUCTUR *******************************/

    public SimpleExcelTaxonImportState(CONFIG config) {
        super(config);
    }

 //************************ PUTTER / GETTER *****************************/

    public void putReference(String title, Reference ref){
        refMap.put(title, ref);
    }
    public Reference getReference(String title){
        return refMap.get(title);
    }

    public void putAgentBase(String title, TeamOrPersonBase<?> agent){
        agentMap.put(title, agent);
    }
    public TeamOrPersonBase<?> getAgentBase(String title){
        return agentMap.get(title);
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

    //names
    public void putName(String titleCache, TaxonName name){
        nameMap.put(titleCache, name);
    }
    public INonViralName getName(String titleCache){
        return nameMap.get(titleCache);
    }

    //higher taxon
    public Taxon getTaxon(String key) {
        return taxonMap.get(key);
    }
    public Taxon putTaxon(String key, Taxon taxon) {
        return taxonMap.put(key, taxon);
    }
    public Taxon removeTaxon(String key) {
        return taxonMap.remove(key);
    }
    public boolean containsTaxon(String key) {
        return taxonMap.containsKey(key);
    }
}