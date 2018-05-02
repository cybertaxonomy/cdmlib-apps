/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

/**
 * @author a.babadshanjan
 * @since 11.09.2009
 */
public class FaunaEuropaeaDistribution {

	private int distributionId;
	private int occurrenceStatusId;
	private int areaId;
	private String areaName;
	private String areaCode;
	private int extraLimital;

	/**
	 * @return the distributionId
	 */
	public int getDistributionId() {
		return distributionId;
	}
	/**
	 * @param distributionId the distributionId to set
	 */
	public void setDistributionId(int distributionId) {
		this.distributionId = distributionId;
	}
	/**
	 * @return the occurrenceStatusId
	 */
	public int getOccurrenceStatusId() {
		return occurrenceStatusId;
	}
	/**
	 * @param occurrenceStatusId the occurrenceStatusId to set
	 */
	public void setOccurrenceStatusId(int occurrenceStatusId) {
		this.occurrenceStatusId = occurrenceStatusId;
	}
	/**
	 * @return the areaId
	 */
	public int getAreaId() {
		return areaId;
	}
	/**
	 * @param areaId the areaId to set
	 */
	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}
	/**
	 * @return the areaName
	 */
	public String getAreaName() {
		return areaName;
	}
	/**
	 * @param areaName the areaName to set
	 */
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}
	/**
	 * @return the areaCode
	 */
	public String getAreaCode() {
		return areaCode;
	}
	/**
	 * @param areaCode the areaCode to set
	 */
	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}
	/**
	 * @return the extraLimital
	 */
	public int getExtraLimital() {
		return extraLimital;
	}
	/**
	 * @param extraLimital the extraLimital to set
	 */
	public void setExtraLimital(int extraLimital) {
		this.extraLimital = extraLimital;
	}


}
