// $Id$
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
 * @author a.mueller
 * @created 11.05.2009
 * @version 1.0
 */
public class BfnXmlImportState extends ImportStateBase<BfnXmlImportConfigurator, BfnXmlImportBase>{
	private Reference refA;
	private Reference refB;
	private Reference currentMicroRef;
	private Reference completeSourceRef;
	private String classificationA;
	private String classificationB;
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(BfnXmlImportState.class);

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

	public void setFirstListSecRef(Reference ref) {
		this.refA = ref;
	}

	public void setSecondListSecRef(Reference ref) {
		this.refB = ref;
	}

	public Reference getFirstListSecRef(){
		return refA;
	}

	public Reference getSecondListSecRef(){
		return refB;
	}

	public void setCurrentMicroRef(Reference currentRef) {
		this.currentMicroRef = currentRef;
	}
	public Reference getCompleteSourceRef() {
		return completeSourceRef;
	}

	public void setCompleteSourceRef(Reference completeSourceRef) {
		this.completeSourceRef = completeSourceRef;
	}

	public Reference getCurrentMicroRef(){
		return currentMicroRef;
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

//	/* (non-Javadoc)
//	 * @see eu.etaxonomy.cdm.io.common.IoStateBase#initialize(eu.etaxonomy.cdm.io.common.IoConfiguratorBase)
//	 */
//	@Override
//	public void initialize(TcsXmlImportConfigurator config) {
//
//	}

}