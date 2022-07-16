/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PolytomousKeyNode;
import eu.etaxonomy.cdm.model.term.TermNode;

/**
 * @author a.mueller
 * @since 11.05.2009
 */
public class EfloraImportState extends ImportStateBase<EfloraImportConfigurator, EfloraImportBase>{

	@SuppressWarnings("unused")
	private static Logger logger = LogManager.getLogger();

	private UnmatchedLeads unmatchedLeads;

	private Set<TermNode<Feature>> featureNodesToSave = new HashSet<>();

	private Set<PolytomousKeyNode> polytomousKeyNodesToSave = new HashSet<>();


	private Language defaultLanguage;

//**************************** CONSTRUCTOR ******************************************/

	public EfloraImportState(EfloraImportConfigurator config) {
		super(config);
		if (getTransformer() == null){
			IInputTransformer newTransformer = config.getTransformer();
			if (newTransformer == null){
				newTransformer = new EfloraTransformer();
			}
			setTransformer(newTransformer);
		}
	}

// ********************************** GETTER / SETTER *************************************/

	public UnmatchedLeads getUnmatchedLeads() {
		return unmatchedLeads;
	}

	public void setUnmatchedLeads(UnmatchedLeads unmatchedKeys) {
		this.unmatchedLeads = unmatchedKeys;
	}

	public void setFeatureNodesToSave(Set<TermNode<Feature>> featureNodesToSave) {
		this.featureNodesToSave = featureNodesToSave;
	}

	public Set<TermNode> getFeatureNodesToSave() {
		return (Set)featureNodesToSave;
	}

	public Set<PolytomousKeyNode> getPolytomousKeyNodesToSave() {
		return polytomousKeyNodesToSave;
	}

	public void setPolytomousKeyNodesToSave(Set<PolytomousKeyNode> polytomousKeyNodesToSave) {
		this.polytomousKeyNodesToSave = polytomousKeyNodesToSave;
	}

	public Language getDefaultLanguage() {
		return this.defaultLanguage;
	}

	public void setDefaultLanguage(Language defaultLanguage){
		this.defaultLanguage = defaultLanguage;
	}
}