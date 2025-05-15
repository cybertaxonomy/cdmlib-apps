/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.merging;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;
import eu.etaxonomy.cdm.common.CdmUtils;

/**
 * Base class for PESI merge classes.
 *
 * @author a.mueller
 * @since 20.01.2020
 */
public abstract class PesiMergeBase {

    protected static List<List<String>> readCsvFile(String fileName){
        List<List<String>> result = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName), ';');
            String[] row;
            while ((row = reader.readNext()) != null){
                result.add(Arrays.asList(row));
            }
            reader.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
        return result;
    }

    protected List<List<String>> getFileData() {
        List<List<String>> result = null;
        while(result == null){
            String input = CdmUtils.readInputLine("Path and filename: ");
            result = readCsvFile(input);
        }
        return result;
    }

    protected boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }
}
