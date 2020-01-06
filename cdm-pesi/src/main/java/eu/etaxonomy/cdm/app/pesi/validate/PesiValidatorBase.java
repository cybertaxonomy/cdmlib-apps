/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.validate;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.CdmUtils;

/**
 * @author a.mueller
 * @since 06.01.2020
 */
public class PesiValidatorBase {

    private static final Logger logger = Logger.getLogger(PesiValidatorBase.class);

    protected boolean testTreeIndex(ResultSet destRS, String childIndexAttr, String parentIndexAttr, String id) throws SQLException {
        boolean result;
        int taxonStatusFk = destRS.getInt("TaxonStatusFk");
        String parentTaxonId = destRS.getString("parentTaxonFk");
        int rankFk = destRS.getInt("RankFk");
        if (taxonStatusFk == 2 || taxonStatusFk == 3|| taxonStatusFk == 4 || taxonStatusFk == 8 || rankFk <= 10){  //synonym; partial syn; pro parte syn; valueless; kingdom and higher
            result = isNull(childIndexAttr, destRS, id);
        }else{
            String childIndex = destRS.getString(childIndexAttr);
            String parentIndex = destRS.getString(parentIndexAttr);
            parentIndex = parentIndex == null? "#": parentIndex;
            result = equals("Tree index", childIndex, parentIndex + parentTaxonId + "#", id);
        }
        return result;
    }

    protected boolean isNull(String attrName, ResultSet destRS, String id) throws SQLException {
        Object value = destRS.getObject(attrName);
        if (value != null){
            String message = attrName + " was expected to be null but was: " + value.toString() + "; id = " + id;
            logger.warn(message);
            return false;
        }else{
            logger.info(attrName + " was null as expected; id = " + id);
            return true;
        }
    }

    protected boolean equals(String messageStart, String strSrc, String strDest, String id) {
        if (StringUtils.isBlank(strSrc)){
            strSrc = null;
        }else{
            strSrc = strSrc.trim();
        }
        //we do not trim strDest here because this should be done during import already. If not it should be shown here
        if (!CdmUtils.nullSafeEqual(strSrc, strDest)){
            int index = CdmUtils.diffIndex(strSrc, strDest);
            String message = id+ ": " + messageStart + " must be equal, but was not at "+index+".\n  Source:      "+  strSrc + "\n  Destination: " + strDest;
            logger.warn(message);
            return false;
        }else{
            logger.info(id+ ": " + messageStart + " were equal: " + strSrc);
            return true;
        }
    }

    protected Integer nullSafeInt(ResultSet rs, String columnName) throws SQLException {
        Object intObject = rs.getObject(columnName);
        if (intObject == null){
            return null;
        }else{
            return Integer.valueOf(intObject.toString());
        }
    }

    protected boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

}
