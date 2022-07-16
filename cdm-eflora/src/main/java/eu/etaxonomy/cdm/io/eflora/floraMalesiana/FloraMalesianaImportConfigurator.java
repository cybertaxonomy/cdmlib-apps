/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.floraMalesiana;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.eflora.EfloraImportConfigurator;
import eu.etaxonomy.cdm.io.eflora.EfloraTaxonImport;

@Component
public class FloraMalesianaImportConfigurator extends EfloraImportConfigurator  {

    private static final long serialVersionUID = 6245230085258251671L;
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static FloraMalesianaImportConfigurator NewInstance(URI uri, ICdmDataSource destination){
		return new FloraMalesianaImportConfigurator(uri, destination);
	}

	//TODO
	private static IInputTransformer defaultTransformer = null;
	private final String classificationTitle = "Flora Malesiana";
	private final String sourceReferenceTitle = "Flora Malesiana";

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
			EfloraTaxonImport.class
		};
	}

	private FloraMalesianaImportConfigurator() {
		super();
	}

	private FloraMalesianaImportConfigurator(URI uri, ICdmDataSource destination) {
		super(uri, destination, defaultTransformer);
		this.setClassificationTitle(classificationTitle);
		this.setSourceReferenceTitle(sourceReferenceTitle);
	}

	@Override
	public FloraMalesianaImportState getNewState() {
		return new FloraMalesianaImportState(this);
	}
}