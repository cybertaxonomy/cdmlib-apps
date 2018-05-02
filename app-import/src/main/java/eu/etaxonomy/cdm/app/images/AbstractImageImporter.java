/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.images;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.IAgentService;
import eu.etaxonomy.cdm.api.service.IClassificationService;
import eu.etaxonomy.cdm.api.service.ICommonService;
import eu.etaxonomy.cdm.api.service.IDescriptionService;
import eu.etaxonomy.cdm.api.service.IReferenceService;
import eu.etaxonomy.cdm.api.service.ITaxonService;
import eu.etaxonomy.cdm.io.common.CdmImportBase;

/**
 * @author n.hoffmann
 * @since 11.11.2008
 */
public abstract class AbstractImageImporter
        extends CdmImportBase<ImageImportConfigurator, ImageImportState> {

    private static final long serialVersionUID = 6178173988325980060L;
    private static final Logger logger = Logger.getLogger(AbstractImageImporter.class);

	protected CdmApplicationController appCtr;

	protected CdmApplicationController cdmApp;
	protected ITaxonService taxonService;
	protected IClassificationService classificationService;
	//TODO:
	protected IAgentService agentService;
	protected IDescriptionService descriptionService;
	protected IReferenceService referenceService;
	protected ICommonService commonService;

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doInvoke(eu.etaxonomy.cdm.io.common.IImportConfigurator, java.util.Map)
	 */
	@Override
	public void doInvoke(ImageImportState state) {
		//cdmApp = config.getCdmAppController();
		//if (config instanceof ImageImportConfigurator){

		TransactionStatus status = startTransaction();

		taxonService = getTaxonService();
		agentService = getAgentService();
		referenceService = getReferenceService();
		commonService = getCommonService();
		classificationService = getClassificationService();

		invokeImageImport(state);

		commitTransaction(status);

		return;
	}

	/**
	 * This method defines the image import.
	 * It should take care of where to get the images from and what object they get attached to.
	 *
	 * @param config
	 */
	protected abstract void invokeImageImport(ImageImportState state);


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(ImageImportState state) {
		boolean result = true;
		logger.warn("No check implemented for abstract image import");
		return result;
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean isIgnore(ImageImportState state) {
		return false;
	}



}
