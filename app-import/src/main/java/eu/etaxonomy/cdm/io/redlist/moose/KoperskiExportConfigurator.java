/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import java.io.File;
import java.util.Comparator;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CsvIOConfigurator;
import eu.etaxonomy.cdm.io.common.ExportResultType;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.io.out.TaxonTreeExportConfiguratorBase;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

/**
 * @author a.mueller
 * @since 2023-08-05
 */
public class KoperskiExportConfigurator
        extends TaxonTreeExportConfiguratorBase<KoperskiExportState,KoperskiExportConfigurator> {

	private static final long serialVersionUID = -5143305983768159605L;

	private CsvIOConfigurator csvIOConfig = CsvIOConfigurator.NewInstance();
    {
        csvIOConfig.setFieldsTerminatedBy(";");
    }

    private boolean createZipFile = false;

    private Comparator<TaxonNodeDto> taxonNodeComparator;

    //filter
    private boolean isExcludeImportSources = true;

    private static final IExportTransformer transformer = null;

//************************* FACTORY ******************************/

    public static KoperskiExportConfigurator NewInstance(){
        KoperskiExportConfigurator result = new KoperskiExportConfigurator(transformer);
        return result;
    }

    public static KoperskiExportConfigurator NewInstance(ICdmDataSource source, File destination){
        KoperskiExportConfigurator result = new KoperskiExportConfigurator(transformer);
        result.setSource(source);
        result.setDestination(destination);
        return result;
    }

//************************ CONSTRUCTOR *******************************/

    private KoperskiExportConfigurator(IExportTransformer transformer) {
        super(transformer);
        this.resultType = ExportResultType.MAP_BYTE_ARRAY;
        this.setTarget(TARGET.EXPORT_DATA);
        setUserFriendlyIOName("Cdm Light Export");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void makeIoClassList() {
        ioClassList = new Class[] {
                KoperskiClassificationExport.class
        };
    }

    @Override
    public KoperskiExportState getNewState() {
        return new KoperskiExportState(this);
    }

    @Override
    public String getDestinationNameString() {
        return null;
    }

//******************** GETTER / SETTER *******************************/

    public String getEncoding() {
        return csvIOConfig.getEncoding();
    }
    public void setEncoding(String encoding) {
        this.csvIOConfig.setEncoding(encoding);
    }

    public String getLinesTerminatedBy() {
        return csvIOConfig.getLinesTerminatedBy();
    }
    public void setLinesTerminatedBy(String linesTerminatedBy) {
        this.csvIOConfig.setLinesTerminatedBy(linesTerminatedBy);
    }

    public String getFieldsEnclosedBy() {
        return  csvIOConfig.getFieldsEnclosedBy();
    }
    public void setFieldsEnclosedBy(String fieldsEnclosedBy) {
        this.csvIOConfig.setFieldsEnclosedBy(fieldsEnclosedBy);
    }

    public boolean isIncludeHeaderLines() {
        return  csvIOConfig.isIncludeHeaderLines();
    }
    public void setIncludeHeaderLines(boolean hasHeaderLines) {
        this.csvIOConfig.setIncludeHeaderLines(hasHeaderLines);
    }

    public String getFieldsTerminatedBy() {
        return  csvIOConfig.getFieldsTerminatedBy();
    }
    public void setFieldsTerminatedBy(String fieldsTerminatedBy) {
        this.csvIOConfig.setFieldsTerminatedBy(fieldsTerminatedBy);
    }

    public boolean isCreateZipFile() {
        return createZipFile;
    }
    public void setCreateZipFile(boolean createZipFile) {
        this.createZipFile = createZipFile;
    }

    public Comparator<TaxonNodeDto> getTaxonNodeComparator() {
        return taxonNodeComparator;
    }
    public void setTaxonNodeComparator(Comparator<TaxonNodeDto> taxonNodeComparator) {
        this.taxonNodeComparator = taxonNodeComparator;
    }

    public boolean isExcludeImportSources() {
        return isExcludeImportSources;
    }
    public void setExcludeImportSources(boolean isFilterImportSources) {
        this.isExcludeImportSources = isFilterImportSources;
    }
}