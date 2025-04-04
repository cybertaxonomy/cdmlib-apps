/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author a.mueller
 * @since 28.09.2011
 */
public class AppImportApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger logger = LogManager.getLogger();

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (logger.isDebugEnabled()) {
            logger.debug(event);
        }
	}
}