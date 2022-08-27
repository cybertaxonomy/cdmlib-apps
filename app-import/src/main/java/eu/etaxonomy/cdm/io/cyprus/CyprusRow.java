/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.cyprus;

import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author a.babadshanjan
 * @since 13.01.2009
 */
public class CyprusRow {

    @SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger();

	private String species;
	private String subspecies;
	private String homotypicSynonyms;
	private String heterotypicSynonyms;
	private String endemism;
	private String status;
	private String redDataBookCategory;
	private String systematics;
	private String genus;
	private String family;
	private String division;

	//Sets
	private TreeMap<Integer, String> distributions = new TreeMap<>();

	public CyprusRow() {
		this.species = "";
		this.subspecies = "";
		this.homotypicSynonyms =  "";
		this.heterotypicSynonyms =  "";
		this.endemism =  "";
		this.status =  "";
		this.setRedDataBookCategory("");
		this.systematics = "";
		this.genus = "";
		this.family = "";
		this.division = "";
	}

//	public CyprusRow(String name, int parentId) {
//		this(name, parentId, null);
//	}

//	public CyprusRow(String scientificName, int parentId, String reference) {
//		this.parentId = parentId;
//		this.scientificName = scientificName;
//		this.reference = reference;
//	}

// **************************** GETTER / SETTER *********************************/



	public void putDistribution(int key, String distribution){
		this.distributions.put(key, distribution);
	}

	public String getSpecies() {
		return species;
	}

	public void setSpecies(String species) {
		this.species = species;
	}

	public String getSubspecies() {
		return subspecies;
	}

	public void setSubspecies(String subspecies) {
		this.subspecies = subspecies;
	}

	public String getHomotypicSynonyms() {
		return homotypicSynonyms;
	}

	public void setHomotypicSynonyms(String homotypicSynonyms) {
		this.homotypicSynonyms = homotypicSynonyms;
	}

	public String getHeterotypicSynonyms() {
		return heterotypicSynonyms;
	}

	public void setHeterotypicSynonyms(String heterotypicSynonyms) {
		this.heterotypicSynonyms = heterotypicSynonyms;
	}

	public String getEndemism() {
		return endemism;
	}

	public void setEndemism(String endemism) {
		this.endemism = endemism;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSystematics() {
		return systematics;
	}

	public void setSystematics(String systematics) {
		this.systematics = systematics;
	}

	public String getGenus() {
		return genus;
	}

	public void setGenus(String genus) {
		this.genus = genus;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		this.division = division;
	}


	public void setRedDataBookCategory(String redDataBookCategory) {
		this.redDataBookCategory = redDataBookCategory;
	}

	public String getRedDataBookCategory() {
		return redDataBookCategory;
	}

}
