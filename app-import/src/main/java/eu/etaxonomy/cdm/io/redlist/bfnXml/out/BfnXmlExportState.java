/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.XmlExportState;

/**
 * @author pplitzner
 * @since May 3, 2016
 */
public class BfnXmlExportState extends XmlExportState<BfnXmlExportConfigurator>{

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	private Set<UUID> knownConceptRelations = new HashSet<>();

	public BfnXmlExportState(BfnXmlExportConfigurator config) {
		super(config);
	}

    public Set<UUID> getKnownConceptRelations() {
        return knownConceptRelations;
    }

}
