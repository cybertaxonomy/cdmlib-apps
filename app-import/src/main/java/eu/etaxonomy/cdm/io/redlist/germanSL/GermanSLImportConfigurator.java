/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.germanSL;

import eu.etaxonomy.cdm.common.URI;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 25.11.2016
 *
 */
public class GermanSLImportConfigurator extends ExcelImportConfiguratorBase{
    private static final long serialVersionUID = -210709271544496787L;

    private Reference secReference;
    public Reference getSecReference() { return secReference;}
    public void setSecReference(Reference secReference) {this.secReference = secReference;}

    private boolean doTaxa = true;
    public boolean isDoTaxa() {return doTaxa;}
    public void setDoTaxa(boolean doTaxa) {this.doTaxa = doTaxa;}

    private boolean doCommonNames = true;
    public boolean isDoCommonNames() {return doCommonNames;}
    public void setDoCommonNames(boolean doCommonNames) {this.doCommonNames = doCommonNames;}

    private String versionString = "version_1_3_4";
    public String getVersionString(){return versionString;}
    public void setVersionString(String versionString){this.versionString = versionString;}

    private static IInputTransformer defaultTransformer = new GermanSLTransformer();

    public static GermanSLImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new GermanSLImportConfigurator(source, destination);
    }

    private GermanSLImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                GermanSLTaxonImport.class,
                GermanSLTaxonRelationImport.class,
        };
    }

}
