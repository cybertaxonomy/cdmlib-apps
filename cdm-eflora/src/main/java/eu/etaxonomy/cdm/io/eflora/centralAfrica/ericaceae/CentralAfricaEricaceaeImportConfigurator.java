/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora.centralAfrica.ericaceae;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.eflora.EfloraImportConfigurator;

@Component
public class CentralAfricaEricaceaeImportConfigurator extends EfloraImportConfigurator  {

    private static final long serialVersionUID = 2277089945601876389L;
    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static CentralAfricaEricaceaeImportConfigurator NewInstance(URI uri, ICdmDataSource destination){
		return new CentralAfricaEricaceaeImportConfigurator(uri, destination);
	}

	private static IInputTransformer defaultTransformer = new CentralAfricaEricaceaeTransformer();
	private String classificationTitle = "Flore d'Afrique Centrale - Ericaceae";
	private String sourceReferenceTitle = "Flore d'Afrique Centrale - Ericaceae";

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
			CentralAfricaEricaceaeTaxonImport.class
		};
	}

	private CentralAfricaEricaceaeImportConfigurator() {
		super();
	}

	private CentralAfricaEricaceaeImportConfigurator(URI uri, ICdmDataSource destination) {
		super(uri, destination, defaultTransformer);
		this.setClassificationTitle(classificationTitle);
		this.setSourceReferenceTitle(sourceReferenceTitle);
	}

	@Override
    public CentralAfricaEricaceaeImportState getNewState() {
		return new CentralAfricaEricaceaeImportState(this);
	}
}