/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.eflora;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Element;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

@Component
public class EfloraImportConfigurator extends ImportConfiguratorBase<EfloraImportState, URI> {

    private static final long serialVersionUID = -2922682361995025203L;
    private static Logger logger = LogManager.getLogger();

	public static EfloraImportConfigurator NewInstance(URI uri, ICdmDataSource destination){
		return new EfloraImportConfigurator(uri, destination);
	}

	private boolean doTaxa = true;

	//TODO
	private static IInputTransformer defaultTransformer = null;
	private String classificationTitle = "E-Flora Import";
	private String sourceReferenceTitle = "E-Flora";
	private UUID defaultLanguageUuid;

	//TODO move to state, but a state gets lost after each import.invoke, so I can't move this information
	//from the one import to another import in case I run 2 imports in line
	private UUID lastTaxonUuid;

	//if true, the keys will be printed after they have been created
	private boolean doPrintKeys = false;

	@SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList(){
		ioClassList = new Class[]{
			EfloraTaxonImport.class
		};
	}

	protected EfloraImportConfigurator() {
		super(defaultTransformer);
	}

	protected EfloraImportConfigurator(IInputTransformer transformer) {
		super(transformer);
	}

	protected EfloraImportConfigurator(URI uri, ICdmDataSource destination) {
		super(defaultTransformer);
		setSource(uri);
		setDestination(destination);
	}

	protected EfloraImportConfigurator(URI uri, ICdmDataSource destination, IInputTransformer transformer) {
		super(transformer);
		setSource(uri);
		setDestination(destination);
	}

	@Override
    public EfloraImportState getNewState() {
		return new EfloraImportState(this);
	}

	public Element getSourceRoot(){
		try {
			URL url = getSource().toURL();
			Object o = url.getContent();
			InputStream is = (InputStream)o;
			Element root = XmlHelp.getRoot(is);
//			makeNamespaces(root);
			return root;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Reference getSourceReference() {
		//TODO
		if (this.sourceReference == null){
			logger.warn("getSource Reference not yet fully implemented");
			sourceReference = ReferenceFactory.newGeneric();
			sourceReference.setTitleCache(sourceReferenceTitle, true);
		}
		return sourceReference;
	}

	@Override
    public String getSourceNameString() {
		if (this.getSource() == null){
			return null;
		}else{
			return this.getSource().toString();
		}
	}

	public void setClassificationTitle(String classificationTitle) {
		this.classificationTitle = classificationTitle;
	}
	public String getClassificationTitle() {
		return classificationTitle;
	}

	public UUID getLastTaxonUuid() {
		return lastTaxonUuid;
	}
	public void setLastTaxonUuid(UUID lastTaxonUuid) {
		this.lastTaxonUuid = lastTaxonUuid;
	}

	public void setDoPrintKeys(boolean doPrintKeys) {
		this.doPrintKeys = doPrintKeys;
	}
	public boolean isDoPrintKeys() {
		return doPrintKeys;
	}

	public UUID getDefaultLanguageUuid() {
		return this.defaultLanguageUuid;
	}
	public void setDefaultLanguageUuid(UUID defaultLanguageUuid) {
		this.defaultLanguageUuid = defaultLanguageUuid;
	}

	public boolean isDoTaxa() {
		return doTaxa;
	}
	public void setDoTaxa(boolean doTaxa) {
		this.doTaxa = doTaxa;
	}
}