/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.cuba;

import java.net.URI;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
public class CubaImportConfigurator extends ExcelImportConfiguratorBase {

    private static final long serialVersionUID = 5590553979984931651L;

    @SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(CubaImportConfigurator.class);

    private String cubaReferenceTitle;

	private boolean isDoTaxa;

	private boolean doAltFlorasSanchez2017;
    private boolean doAltFlorasFRC;
    private boolean doAltFlorasFC;
    private boolean doAltFlorasAS;


    private boolean doVocabularies;
    public void setDoVocabularies(boolean doVocabularies) {this.doVocabularies = doVocabularies;}
    public boolean isDoVocabularies() {return doVocabularies;}

    private static IInputTransformer defaultTransformer = new CubaTransformer();

	public static CubaImportConfigurator NewInstance(URI source, ICdmDataSource destination){
		return new CubaImportConfigurator(source, destination);
	}

	@Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
		        CubaVocabularyImport.class,
				CubaExcelImport.class
		};
	}

	@Override
    public ImportStateBase getNewState() {
		return new CubaImportState(this);
	}



	private CubaImportConfigurator(URI source, ICdmDataSource destination) {
	   super(source, destination, defaultTransformer);
	   setNomenclaturalCode(NomenclaturalCode.ICNAFP);
	   setSource(source);
	   setDestination(destination);
	}


	@Override
    public URI getSource() {
		return super.getSource();
	}
	@Override
    public void setSource(URI source) {
		super.setSource(source);
	}

	@Override
    public Reference getSourceReference() {
		if (sourceReference == null){
			sourceReference =  ReferenceFactory.newDatabase();
			if (getSource() != null){
				sourceReference.setTitleCache(getCubaReferenceTitle(), true);
			}
		}
		return sourceReference;
	}

	@Override
    public String getSourceNameString() {
		return getSource().toString();
	}


	public void setCubaReferenceTitle(String cyprusReferenceTitle) {
		this.cubaReferenceTitle = cyprusReferenceTitle;
	}


	public String getCubaReferenceTitle() {
		return cubaReferenceTitle;
	}

	public void setDoTaxa(boolean isDoTaxa) {
		this.isDoTaxa = isDoTaxa;
	}

	public boolean isDoTaxa() {
		return isDoTaxa;
	}


	//************* ALT FLORAS ********************/

	public boolean isDoAltFlorasSanchez2017() {
        return doAltFlorasSanchez2017;
    }
    public void setDoAltFlorasSanchez2017(boolean doAltFlorasSanchez2017) {
        this.doAltFlorasSanchez2017 = doAltFlorasSanchez2017;
    }
    public boolean isDoAltFlorasFRC() {
        return doAltFlorasFRC;
    }
    public void setDoAltFlorasFRC(boolean doAltFlorasFRC) {
        this.doAltFlorasFRC = doAltFlorasFRC;
    }
    public boolean isDoAltFlorasFC() {
        return doAltFlorasFC;
    }
    public void setDoAltFlorasFC(boolean doAltFlorasFC) {
        this.doAltFlorasFC = doAltFlorasFC;
    }
    public boolean isDoAltFlorasAS() {
        return doAltFlorasAS;
    }
    public void setDoAltFlorasAS(boolean doAltFlorasAS) {
        this.doAltFlorasAS = doAltFlorasAS;
    }



}
