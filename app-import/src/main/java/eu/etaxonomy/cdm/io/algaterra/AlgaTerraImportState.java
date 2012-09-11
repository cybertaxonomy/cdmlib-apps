// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.model.description.StatisticalMeasure;

/**
 * @author a.mueller
 * @created 11.05.2009
 * @version 1.0
 */
public class AlgaTerraImportState extends BerlinModelImportState{

	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(AlgaTerraImportState.class);

	private boolean specimenVocabulariesCreated = false;
	private boolean currentFieldObservationNotNew = false;
	private Map<String, UUID> parameterFeatureUuidMap = new HashMap<String, UUID>(); 
	
	public AlgaTerraImportState(AlgaTerraImportConfigurator config) {
		super(config);
	}
	
	public AlgaTerraImportConfigurator getAlgaTerraConfigurator(){
		return (AlgaTerraImportConfigurator)getConfig();
	}

	public boolean isSpecimenVocabulariesCreated() {
		return specimenVocabulariesCreated;
	}

	public void setSpecimenVocabulariesCreated(boolean specimenVocabulariesCreated) {
		this.specimenVocabulariesCreated = specimenVocabulariesCreated;
	}

	public boolean isCurrentFieldObservationNotNew() {
		return currentFieldObservationNotNew;
	}

	public void setCurrentFieldObservationNotNew(
			boolean currentFieldObservationNotNew) {
		this.currentFieldObservationNotNew = currentFieldObservationNotNew;
	}

	public UUID getParameterFeatureUuid(String key) {
		return parameterFeatureUuidMap.get(key);
	}

	public void putParameterFeatureUuid(String key, UUID parameterFeatureUuid) {
		this.parameterFeatureUuidMap.put(key, parameterFeatureUuid);
	}

}
