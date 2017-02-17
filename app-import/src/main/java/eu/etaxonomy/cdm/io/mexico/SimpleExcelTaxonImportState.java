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

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportState;
import eu.etaxonomy.cdm.io.excel.common.ExcelRowBase;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
public class SimpleExcelTaxonImportState<CONFIG extends ExcelImportConfiguratorBase>
        extends ExcelImportState<CONFIG, ExcelRowBase>{

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SimpleExcelTaxonImportState.class);

    private Map<String, Reference> refMap = new HashMap<>();

    private Map<String, TeamOrPersonBase<?>> agentMap = new HashMap<>();

    private final Map<String, Taxon> higherTaxonTaxonMap = new HashMap<>();

    //using titleCache
    private Map<String, INonViralName> nameMap = new HashMap<>();


// ************************* CONSTRUCTUR *******************************/
    /**
     * @param config
     */
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
    public void putName(String titleCache, INonViralName name){
        nameMap.put(titleCache, name);
    }
    public INonViralName getName(String titleCache){
        return nameMap.get(titleCache);
    }

}
