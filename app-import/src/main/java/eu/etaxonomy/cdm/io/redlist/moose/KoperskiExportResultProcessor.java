/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import eu.etaxonomy.cdm.io.common.ExportType;
import eu.etaxonomy.cdm.model.common.ICdmBase;

/**
 * @author a.mueller
 * @since 2023-08-05
 */
public class KoperskiExportResultProcessor {

    private static final String HEADER = "HEADER_207dd23a-f877-4c27-b93a-8dbea3234281";

    private Map<KoperskiExportTable, Map<String,String[]>> result = new HashMap<>();
    private KoperskiExportState state;

    public KoperskiExportResultProcessor(KoperskiExportState state) {
        super();
        this.state = state;
        Map<String,String[]> resultMap;
        for (KoperskiExportTable table: KoperskiExportTable.values()){
            resultMap = new HashMap<>();
            if (state.getConfig().isIncludeHeaderLines()){
                resultMap.put(HEADER, table.getColumnNames());
            }
            result.put(table, resultMap);
        }
    }

    public void put(KoperskiExportTable table, String id, String[] csvLine) {
        Map<String,String[]> resultMap = result.get(table);
        if (resultMap == null ){
            resultMap = new HashMap<>();
            if (state.getConfig().isIncludeHeaderLines()){
                resultMap.put(HEADER, table.getColumnNames());
            }
            result.put(table, resultMap);
        }
        String[] record = resultMap.get(id);
        if (record == null){
            record = csvLine;

            String[] oldRecord = resultMap.put(id, record);

            String[] newRecord = resultMap.get(id);

            if (oldRecord != null){
                String message = "Output processor already has a record for id " + id + ". This should not happen.";
                state.getResult().addWarning(message);
            }
        }
    }

    public boolean hasRecord(KoperskiExportTable table, String id){
        Map<String, String[]> resultMap = result.get(table);
        if (resultMap == null){
            return false;
        }else{
            return resultMap.get(id) != null;
        }
    }

    public  String[] getRecord(KoperskiExportTable table, String id){
        return result.get(table).get(id);
    }

    public void put(KoperskiExportTable table, ICdmBase cdmBase, String[] csvLine) {
       this.put(table, cdmBase.getUuid().toString(), csvLine);
    }

    public void createFinalResult(KoperskiExportState state) {

        if (!result.isEmpty() ){
            state.setAuthorStore(new HashMap<>());
            state.setHomotypicalGroupStore(new ArrayList<>());
            state.setReferenceStore(new ArrayList<>());
            state.setSpecimenStore(new ArrayList<>());
            state.setNodeChildrenMap(new HashMap<>());
            //Replace quotes by double quotes
            for (KoperskiExportTable table: result.keySet()){
                //write each table in an explicite stream ...
                Map<String, String[]> tableData = result.get(table);
                KoperskiExportConfigurator config = state.getConfig();
                ByteArrayOutputStream exportStream = new ByteArrayOutputStream();

                try{
                    List<String> data = new ArrayList<>();
                    String[] csvHeaderLine = tableData.get(HEADER);
                    String lineString = createCsvLine(config, csvHeaderLine);
                    lineString = lineString+ "";
                    data.add(lineString);
                    for (String key: tableData.keySet()){
                        if (!key.equals(HEADER)){
                            String[] csvLine = tableData.get(key);

                            lineString = createCsvLine(config, csvLine);
                            data.add(lineString);
                        }
                    }
                    IOUtils.writeLines(data,
                            null,exportStream,
                            Charset.forName("UTF-8"));
                } catch(Exception e){
                    e.printStackTrace();
                    state.getResult().addException(e, e.getMessage());
                }

                state.getResult().putExportData(table.getTableName(), exportStream.toByteArray());
                state.getResult().setExportType(ExportType.CDM_LIGHT);

            }
        }
        result.clear();
    }

    private String createCsvLine(KoperskiExportConfigurator config, String[] csvLine) {
        String lineString = "";
        boolean first = true;
        for (String columnEntry: csvLine){
            if (columnEntry == null){
                columnEntry = "";
            }
            columnEntry = columnEntry.replace("\"", "\"\"");
            columnEntry = columnEntry.replace(config.getLinesTerminatedBy(), "\\r");
            //replace all line brakes according to best practices: http://code.google.com/p/gbif-ecat/wiki/BestPractices
            columnEntry = columnEntry.replace("\r\n", "\\r");
            columnEntry = columnEntry.replace("\r", "\\r");
            columnEntry = columnEntry.replace("\n", "\\r");
            if (first){
                lineString += config.getFieldsEnclosedBy() + columnEntry + config.getFieldsEnclosedBy() ;
                first = false;
            }else{
                lineString += config.getFieldsTerminatedBy() + config.getFieldsEnclosedBy() + columnEntry + config.getFieldsEnclosedBy() ;
            }
        }

        return lineString;
    }
}