/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.algaterra;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class AlgaTerraImportState extends BerlinModelImportState{

	@SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

	private boolean specimenVocabulariesCreated = false;

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


}
