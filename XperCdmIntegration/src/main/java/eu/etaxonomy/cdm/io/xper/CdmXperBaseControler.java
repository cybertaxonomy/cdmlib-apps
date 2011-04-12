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

import java.util.List;

import org.apache.log4j.Logger;

import fr_jussieu_snv_lis.base.Base;
import fr_jussieu_snv_lis.base.Group;
import fr_jussieu_snv_lis.base.IBase;
import fr_jussieu_snv_lis.base.IControlerBase;
import fr_jussieu_snv_lis.base.Individual;
import fr_jussieu_snv_lis.base.Mode;
import fr_jussieu_snv_lis.base.Variable;

/**
 * @author a.mueller
 * @date 12.04.2011
 *
 */
public class CdmXperBaseControler implements IControlerBase {
	private static final Logger logger = Logger.getLogger(CdmXperBaseControler.class);
	
	private IBase base;

	public CdmXperBaseControler(IBase base) {
		this.base = base;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createBase(java.lang.String, java.lang.String)
	 */
	@Override
	public void createBase(String path, String name) throws Exception {
		//TODO
		throw new RuntimeException("A new base can not be created in an external base controler");
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createBase(java.lang.String)
	 */
	@Override
	public void createBase(String name) throws Exception {
		//TODO
		throw new RuntimeException("A new base can not be opened in an external base controler");
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#setBase(fr_jussieu_snv_lis.base.Base)
	 */
	@Override
	public void setBase(Base b) {
		logger.warn("Base is newly defined. This may lead to errors as controler base is a CdmXperBaseControler");
		this.base = b;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getBase()
	 */
	@Override
	public IBase getBase() {
		return base;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getBaseName()
	 */
	@Override
	public String getBaseName() {
		return base.getName();
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#closeBase()
	 */
	@Override
	public void closeBase() {
		logger.warn("Not yet implemented");
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createNewGroup(java.lang.String)
	 */
	@Override
	public Group createNewGroup(String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroup(fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public boolean addGroup(Group group) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupToBase(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public boolean addGroupToBase(Group group, IBase b) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupAndVariablesToBase(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public boolean addGroupAndVariablesToBase(Group group, IBase b) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupAt(int, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void addGroupAt(int i, Group g) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupToBaseAt(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.IBase, int)
	 */
	@Override
	public void addGroupToBaseAt(Group g, IBase b, int i) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteGroupFromBase(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public void deleteGroupFromBase(Group g, IBase b) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteGroups(java.lang.Object[])
	 */
	@Override
	public void deleteGroups(Object[] tab) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteGroup(fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void deleteGroup(Group g) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteGroupFromVariable(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void deleteGroupFromVariable(Variable v, Group g) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteGroupFromVariables(java.lang.Object[], fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void deleteGroupFromVariables(Object[] tab, Group g) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteAllGroups(fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public void deleteAllGroups(IBase b) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getGroupsNoBelongingToVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public List<Group> getGroupsNoBelongingToVariable(Variable v) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getGroupsNoBelongingToVariables(java.lang.Object[])
	 */
	@Override
	public List<Group> getGroupsNoBelongingToVariables(Object[] tab) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getGroupsBelongingToVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public List<Group> getGroupsBelongingToVariable(Variable v) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getGroupsBelongingToVariables(java.lang.Object[])
	 */
	@Override
	public List<Group> getGroupsBelongingToVariables(Object[] tab) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getAllGroups()
	 */
	@Override
	public List<Group> getAllGroups() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#findGroupByName(java.lang.String)
	 */
	@Override
	public Group findGroupByName(String n) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceGroup()
	 */
	@Override
	public boolean checkExistanceGroup() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupToVariableAt(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.Variable, int)
	 */
	@Override
	public void addGroupToVariableAt(Group g, Variable v, int i) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupToVariable(fr_jussieu_snv_lis.base.Group, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void addGroupToVariable(Group g, Variable v) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addGroupToVariables(fr_jussieu_snv_lis.base.Group, java.lang.Object[])
	 */
	@Override
	public void addGroupToVariables(Group g, Object[] al) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addVariableToGroups(fr_jussieu_snv_lis.base.Variable, java.util.List)
	 */
	@Override
	public void addVariableToGroups(Variable v, List<Group> al) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceGroup(java.lang.String)
	 */
	@Override
	public boolean checkExistanceGroup(String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceGroup(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public boolean checkExistanceGroup(Variable var, Group g) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#findModeByName(java.lang.String, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public Mode findModeByName(String str, Variable var) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addModeToVariable(fr_jussieu_snv_lis.base.Mode, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void addModeToVariable(Mode m, Variable v) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addModeToVariableAt(fr_jussieu_snv_lis.base.Mode, fr_jussieu_snv_lis.base.Variable, int)
	 */
	@Override
	public void addModeToVariableAt(Mode m, Variable v, int i) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveUpModeFromVariable(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void moveUpModeFromVariable(Variable v, Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveDownModeFromVariable(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void moveDownModeFromVariable(Variable v, Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteModeFromVariable(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void deleteModeFromVariable(Variable v, Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#fusionModeFromvariable(fr_jussieu_snv_lis.base.Variable, java.lang.Object[])
	 */
	@Override
	public Mode fusionModeFromvariable(Variable var, Object[] tab) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceMode(fr_jussieu_snv_lis.base.Variable, java.lang.String)
	 */
	@Override
	public boolean checkExistanceMode(Variable v, String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceMode(fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.Variable, java.lang.String)
	 */
	@Override
	public boolean checkExistanceMode(Individual ind, Variable var, String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#matchModes(fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.Variable, java.util.List, int)
	 */
	@Override
	public boolean matchModes(Individual ind, Variable var, List<Mode> modes,
			int operator) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#matchNumValue(fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.Variable, java.lang.Double)
	 */
	@Override
	public boolean matchNumValue(Individual ind, Variable var, Double value) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createNewMode(fr_jussieu_snv_lis.base.Variable, java.lang.String)
	 */
	@Override
	public Mode createNewMode(Variable va, String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#controlModeExVariable(boolean, fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void controlModeExVariable(boolean selected, Variable v,
			Mode modeException) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteModeExFromVariable(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void deleteModeExFromVariable(Variable v, Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistIndividual()
	 */
	@Override
	public boolean checkExistIndividual() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteIndividualFromBase(fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public void deleteIndividualFromBase(Individual ind, IBase b) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteIndividual(fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public void deleteIndividual(Individual ind) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addIndividual(fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public void addIndividual(Individual ind) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addIndividualToBaseAt(fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.IBase, int)
	 */
	@Override
	public void addIndividualToBaseAt(Individual ind, IBase b, int i) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#findIndividualByName(java.lang.String)
	 */
	@Override
	public Individual findIndividualByName(String ind) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceOneIndividual(java.lang.String)
	 */
	@Override
	public boolean checkExistanceOneIndividual(String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createNewIndividual(java.lang.String)
	 */
	@Override
	public Individual createNewIndividual(String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#copyIndividual(fr_jussieu_snv_lis.base.Individual, java.lang.String)
	 */
	@Override
	public Individual copyIndividual(Individual indtocopy, String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#copyVariable(fr_jussieu_snv_lis.base.Variable, java.lang.String)
	 */
	@Override
	public Variable copyVariable(Variable vartocopy, String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#mergeVariables(java.lang.Object[], java.lang.String)
	 */
	@Override
	public Variable mergeVariables(Object[] tab, String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#mergeIndividuals(java.lang.Object[], java.lang.String)
	 */
	@Override
	public Individual mergeIndividuals(Object[] tab, String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#controlModeIndVar(boolean, fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void controlModeIndVar(boolean selected, Variable v, Individual i,
			Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#controlModeIndVarRec(boolean, fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual, fr_jussieu_snv_lis.base.Mode)
	 */
	@Override
	public void controlModeIndVarRec(boolean selected, Variable v,
			Individual i, Mode m) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkBadDescription(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public void checkBadDescription(Variable var, Individual ind) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkingUnknown(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public boolean checkingUnknown(Variable var, Individual ind) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkUnknown(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual, boolean, boolean)
	 */
	@Override
	public void checkUnknown(Variable var, Individual ind, boolean b,
			boolean withDaughters) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkUnknown(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Individual[], boolean, boolean)
	 */
	@Override
	public void checkUnknown(Variable var, Individual[] tab, boolean b,
			boolean withDaughters) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#buildIndividualsBitSetMatrix()
	 */
	@Override
	public void buildIndividualsBitSetMatrix() {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#buildIndividualsIntegerBitMatrix()
	 */
	@Override
	public void buildIndividualsIntegerBitMatrix() {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public boolean addVariable(Variable variable) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addVariableToBase(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.IBase)
	 */
	@Override
	public void addVariableToBase(Variable variable, IBase b) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addMother(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public boolean addMother(Variable current, Variable mother) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#removeMother(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void removeMother(Variable current) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#changeMother(fr_jussieu_snv_lis.base.Variable, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void changeMother(Variable current, Variable mother) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistVariable()
	 */
	@Override
	public boolean checkExistVariable() {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public boolean checkExistVariable(Variable v) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#findVariableByName(java.lang.String)
	 */
	@Override
	public Variable findVariableByName(String str) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkExistanceOneVariable(java.lang.String)
	 */
	@Override
	public boolean checkExistanceOneVariable(String s) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkCommunModeName(fr_jussieu_snv_lis.base.Variable[])
	 */
	@Override
	public boolean checkCommunModeName(Variable[] tab) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkNumericalType(fr_jussieu_snv_lis.base.Variable[])
	 */
	@Override
	public boolean checkNumericalType(Variable[] tab) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#checkStateNumber(fr_jussieu_snv_lis.base.Variable[])
	 */
	@Override
	public boolean checkStateNumber(Variable[] tab) {
		logger.warn("Not yet implemented");
		return false;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createNewVariable(java.lang.String)
	 */
	@Override
	public Variable createNewVariable(String name) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#createNewVariableNumOrEnum(java.lang.String, java.lang.String)
	 */
	@Override
	public Variable createNewVariableNumOrEnum(String name, String type) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addNewVariableAndItsModesToMatrix(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void addNewVariableAndItsModesToMatrix(Variable newVariable) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#addNewVariableNumAndItsModesToMatrix(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void addNewVariableNumAndItsModesToMatrix(Variable newVariable) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariableFromMatrix(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void deleteVariableFromMatrix(Variable variable) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariable(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void deleteVariable(Variable va) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariableAndAllDaughters(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void deleteVariableAndAllDaughters(Variable v) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariablesAndAllDaughters(fr_jussieu_snv_lis.base.Variable[])
	 */
	@Override
	public void deleteVariablesAndAllDaughters(Variable[] tab) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariableWithoutDaughters(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void deleteVariableWithoutDaughters(Variable v) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#deleteVariablesWithoutDaughters(fr_jussieu_snv_lis.base.Variable[])
	 */
	@Override
	public void deleteVariablesWithoutDaughters(Variable[] tab) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariableDescendant(java.util.List, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public List<Variable> getVariableDescendant(List<Variable> listDescendant,
			Variable v) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariableDaughters(java.util.List, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public List<Variable> getVariableDaughters(List<Variable> listDaugther,
			Variable v) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariableDaughters(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public List<Variable> getVariableDaughters(Variable v) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariablesWithoutMother()
	 */
	@Override
	public List<Variable> getVariablesWithoutMother() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariablesWithoutMother(java.util.List)
	 */
	@Override
	public List<Variable> getVariablesWithoutMother(List<Variable> variables) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariablesWithoutDaughter(java.util.List)
	 */
	@Override
	public List<Variable> getVariablesWithoutDaughter(List<Variable> variables) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getListVariable()
	 */
	@Override
	public List<Variable> getListVariable() {
		logger.warn("getListVariable() Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariablesByGroup(java.util.List, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public List<Variable> getVariablesByGroup(List<Variable> variables,	Group group) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getVariablesWithoutGroup(java.util.List)
	 */
	@Override
	public List<Variable> getVariablesWithoutGroup(List<Variable> variables) {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#setCurrentInd(fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public void setCurrentInd(Individual ind) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#setCurrentIndArray(fr_jussieu_snv_lis.base.Individual[])
	 */
	@Override
	public void setCurrentIndArray(Individual[] tab) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#setCurrentIndArray(java.lang.Object[])
	 */
	@Override
	public void setCurrentIndArray(Object[] tab) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getCurrentVar()
	 */
	@Override
	public Variable getCurrentVar() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#setCurrentVar(fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void setCurrentVar(Variable var) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getCurrentInd()
	 */
	@Override
	public Individual getCurrentInd() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getCurrentIndArray()
	 */
	@Override
	public Individual[] getCurrentIndArray() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getSortedIndividuals()
	 */
	@Override
	public List<Individual> getSortedIndividuals() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#getSortedGroups()
	 */
	@Override
	public List<Group> getSortedGroups() {
		logger.warn("Not yet implemented");
		return null;
	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveIndividualTo(int, fr_jussieu_snv_lis.base.Individual)
	 */
	@Override
	public void moveIndividualTo(int i, Individual ind) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveVariableTo(int, fr_jussieu_snv_lis.base.Variable)
	 */
	@Override
	public void moveVariableTo(int i, Variable v) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveVariablesTo(int, java.util.List)
	 */
	@Override
	public void moveVariablesTo(int i, List<Variable> a) {
		logger.warn("Not yet implemented");

	}

	/* (non-Javadoc)
	 * @see fr_jussieu_snv_lis.base.IControlerBase#moveGroupTo(int, fr_jussieu_snv_lis.base.Group)
	 */
	@Override
	public void moveGroupTo(int i, Group g) {
		logger.warn("Not yet implemented");

	}

}
