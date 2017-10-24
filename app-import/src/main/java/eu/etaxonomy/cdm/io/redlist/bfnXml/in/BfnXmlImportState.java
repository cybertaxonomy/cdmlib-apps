/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.oppermann
 * @created 2013
 */
public class BfnXmlImportState extends ImportStateBase<BfnXmlImportConfigurator, BfnXmlImportBase>{
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BfnXmlImportState.class);

    private Reference firstListSecRef;
	private Reference secondListSecRef;
	private Reference currentSecundumRef;
	private Reference completeSourceRef;
	private String classificationA;
	private String classificationB;

    private boolean fillSecondList = false;

	//TODO make it better
	private Map<String, CommonTaxonName> commonNameMap = null;

	public BfnXmlImportState(BfnXmlImportConfigurator config) {
		super(config);
	}

	public Map<String, CommonTaxonName> getCommonNameMap() {
		return commonNameMap;
	}

	public void setCommonNameMap(Map<String, CommonTaxonName> commonNameMap) {
		this.commonNameMap = commonNameMap;
	}


    public Reference getFirstListSecRef(){
        return firstListSecRef;
    }
    public void setFirstListSecRef(Reference ref) {
		this.firstListSecRef = ref;
	}

    public Reference getSecondListSecRef(){
        return secondListSecRef;
    }
	public void setSecondListSecRef(Reference ref) {
		this.secondListSecRef = ref;
	}

	/**
	 * The import file as reference, not the book
	 * @return
	 */
	public Reference getCompleteSourceRef() {
		return completeSourceRef;
	}
	/**
	 * {@link #getCompleteSourceRef()}
	 */
	public void setCompleteSourceRef(Reference completeSourceRef) {
		this.completeSourceRef = completeSourceRef;
	}

	public Reference getCurrentSecRef(){
		return currentSecundumRef;
	}
    public void setCurrentSecundumRef(Reference currentRef) {
        this.currentSecundumRef = currentRef;
    }

	public void setFirstClassificationName(String classificationA) {
		  this.classificationA = classificationA;
	}

	public void setSecondClassificationName(String classificationB) {
		  this.classificationB = classificationB;
	}

	public String getFirstClassificationName() {
		return  classificationA;
	}

	public String getSecondClassificationName() {
		return  classificationB;
	}


    public boolean isFillSecondList() {
        return fillSecondList;
    }
    public void setFillSecondList(boolean fillSecondList) {
        this.fillSecondList = fillSecondList;
    }

}
