// $Id$
/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.xper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import fr_jussieu_snv_lis.base.BaseObject;
import fr_jussieu_snv_lis.base.BaseObjectResource;
import fr_jussieu_snv_lis.base.Group;
import fr_jussieu_snv_lis.base.IBase;
import fr_jussieu_snv_lis.base.Individual;
import fr_jussieu_snv_lis.base.Variable;
import fr_jussieu_snv_lis.base.XPResource;

/**
 * @author a.mueller
 * @date 12.04.2011
 *
 */
public class BaseCdm implements IBase {
	private static final Logger logger = Logger.getLogger(BaseCdm.class);
	
	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getGroups()
	 */
	@Override
	public List<Group> getGroups() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getGroupAt(int)
	 */
	@Override
	public Group getGroupAt(int i) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setGroups(java.util.List)
	 */
	@Override
	public void setGroups(List<Group> n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addGroup(fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public boolean addGroup(Group obj) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addGroupAt(int, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void addGroupAt(int i, Group group) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#deleteGroup(fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public boolean deleteGroup(Group group) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getVariables()
	 */
	@Override
	public List<Variable> getVariables() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getVariableAt(int)
	 */
	@Override
	public Variable getVariableAt(int i) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setVariables(java.util.List)
	 */
	@Override
	public void setVariables(List<Variable> al) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#deleteVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public boolean deleteVariable(Variable variable) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public boolean addVariable(Variable variable) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addVariableAt(fr_jussieu_snv_lis.base.Variable, int)
	 */
	@Override
	public void addVariableAt(Variable variable, int i) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addIndividual(fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public boolean addIndividual(Individual individual) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addIndividualAt(int, fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public boolean addIndividualAt(int i, Individual ind) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#deleteIndividual(fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public boolean deleteIndividual(Individual obj) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getIndividuals()
	 */
	@Override
	public List<Individual> getIndividuals() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getIndividualAt(int)
	 */
	@Override
	public Individual getIndividualAt(int i) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setIndividualList(java.util.List)
	 */
	@Override
	public void setIndividualList(List<Individual> n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getAuthors()
	 */
	@Override
	public Set<String> getAuthors() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getAuthorAt(int)
	 */
	@Override
	public String getAuthorAt(int i) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setAuthors(java.lang.Object[])
	 */
	@Override
	public void setAuthors(Object[] list) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addAuthor(java.lang.String)
	 */
	@Override
	public boolean addAuthor(String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#deleteAuthor(java.lang.String)
	 */
	@Override
	public boolean deleteAuthor(String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getName()
	 */
	@Override
	public String getName() {
		logger.warn("GetName not yet full implemented for BaseCdm");
		return "A Cdm Database";
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setName(java.lang.String)
	 */
	@Override
	public void setName(String n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getPathName()
	 */
	@Override
	public String getPathName() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setPathName(java.lang.String)
	 */
	@Override
	public void setPathName(String n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getShortname()
	 */
	@Override
	public String getShortname() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setShortname(java.lang.String)
	 */
	@Override
	public void setShortname(String string) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getDescription()
	 */
	@Override
	public String getDescription() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String string) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setLastEdition(java.lang.String)
	 */
	@Override
	public void setLastEdition(String s) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getLastEdition()
	 */
	@Override
	public String getLastEdition() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setFirstEdition(java.lang.String)
	 */
	@Override
	public void setFirstEdition(String s) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getFirstEdition()
	 */
	@Override
	public String getFirstEdition() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#checkIndex(java.util.List)
	 */
	@Override
	public void checkIndex(List<? extends BaseObject> al) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbVariables()
	 */
	@Override
	public int getNbVariables() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setNbVariables(int)
	 */
	@Override
	public void setNbVariables(int n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbIndividuals()
	 */
	@Override
	public int getNbIndividuals() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setNbIndividuals(int)
	 */
	@Override
	public void setNbIndividuals(int n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbModes()
	 */
	@Override
	public int getNbModes() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbGroups()
	 */
	@Override
	public int getNbGroups() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setNbGroups(int)
	 */
	@Override
	public void setNbGroups(int n) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbAuthors()
	 */
	@Override
	public int getNbAuthors() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#hasGroups()
	 */
	@Override
	public boolean hasGroups() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#hasAuthors()
	 */
	@Override
	public boolean hasAuthors() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#hasVariables()
	 */
	@Override
	public boolean hasVariables() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#hasIndividuals()
	 */
	@Override
	public boolean hasIndividuals() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#isIllustrated()
	 */
	@Override
	public boolean isIllustrated() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#isIllustrated(fr_jussieu_snv_lis.base.BaseObject[])
	 */
	@Override
	public boolean isIllustrated(BaseObject[] bo) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getIllustratedBo(fr_jussieu_snv_lis.base.XPResource)
	 */
	@Override
	public List<BaseObject> getIllustratedBo(XPResource xpr) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getAllResources()
	 */
	@Override
	public List<BaseObjectResource> getAllResources() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getAllResources(java.lang.String)
	 */
	@Override
	public HashSet<Object> getAllResources(String str) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#containsBaseObject(fr_jussieu_snv_lis.base.BaseObject)
	 */
	@Override
	public boolean containsBaseObject(BaseObject bo) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getVariablesWithoutGroup()
	 */
	@Override
	public List<Variable> getVariablesWithoutGroup() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object arg0) {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#free()
	 */
	@Override
	public void free() {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#isComplete()
	 */
	@Override
	public boolean isComplete() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getCompletePercentage()
	 */
	@Override
	public float getCompletePercentage() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#toTabFile()
	 */
	@Override
	public String toTabFile() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#toHtml()
	 */
	@Override
	public String toHtml() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getResource()
	 */
	@Override
	public BaseObjectResource getResource() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setResource(fr_jussieu_snv_lis.base.BaseObjectResource)
	 */
	@Override
	public void setResource(BaseObjectResource b) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#removeResource()
	 */
	@Override
	public void removeResource() {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#hasAnIllustration()
	 */
	@Override
	public boolean hasAnIllustration() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getLanguage()
	 */
	@Override
	public String getLanguage() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setLanguage(java.lang.String)
	 */
	@Override
	public void setLanguage(String language) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setLinks(java.lang.Object[])
	 */
	@Override
	public void setLinks(Object[] objects) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getLinks()
	 */
	@Override
	public Set<String> getLinks() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getLinkAt(int)
	 */
	@Override
	public String getLinkAt(int i) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addLinks(java.lang.String)
	 */
	@Override
	public boolean addLinks(String link) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#removeLinks(java.lang.String)
	 */
	@Override
	public boolean removeLinks(String link) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getLicense()
	 */
	@Override
	public String getLicense() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setLicense(java.lang.String)
	 */
	@Override
	public void setLicense(String license) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getNbLinks()
	 */
	@Override
	public int getNbLinks() {
		logger.warn("Not yet implemented");
		return 0;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getUnknownData()
	 */
	@Override
	public Map<Individual, Variable> getUnknownData() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getEmptyData()
	 */
	@Override
	public Map<Individual, Variable> getEmptyData() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#getHelp()
	 */
	@Override
	public String getHelp() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#setHelp(java.lang.String)
	 */
	@Override
	public void setHelp(String help) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#isPresentNumVariable()
	 */
	@Override
	public boolean isPresentNumVariable() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IBase#addResource(fr_jussieu_snv_lis.base.BaseObject, fr_jussieu_snv_lis.base.BaseObjectResource)
	 */
	@Override
	public boolean addResource(BaseObject bo, BaseObjectResource rsc) {
		logger.warn("Not yet implemented");
		return false;
	}

}
