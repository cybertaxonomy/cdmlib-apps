/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

/**
 * @author a.oppermann
 * @date 03.07.2013
 *
 */
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

@Component
public class BfnXmlImportConfigurator extends ImportConfiguratorBase<BfnXmlImportState, URI>  {
    private static final long serialVersionUID = -1647291548711127385L;

    private static final Logger logger = Logger.getLogger(BfnXmlImportConfigurator.class);

	//TODO
	private static IInputTransformer defaultTransformer = null;


	private boolean doMetaData = true;
	private boolean doTaxonNames = true;
    private boolean doFeature = true;
    private boolean doAdditionalTerms = true;

    private boolean doInformationImport = true;
    private boolean hasSecondList = false;


	//	rdfNamespace
	Namespace bfnXmlNamespace;

	private NomenclaturalCode nomenclaturalCode = null;

	@SuppressWarnings("unchecked")
	@Override
	protected void makeIoClassList(){
		ioClassList = new Class[]{
		        BfnXmlImportReferences.class,
				BfnXmlImportAddtionalTerms.class,
				BfnXmlImportMetaData.class,
				BfnXmlImportFeature.class,
				BfnXmlImportTaxonName.class
		};
	}

	public static BfnXmlImportConfigurator NewInstance(URI uri,
			ICdmDataSource destination){
		return new BfnXmlImportConfigurator(uri, destination);
	}

	/**
	 * @param berlinModelSource
	 * @param sourceReference
	 * @param destination
	 */
	private BfnXmlImportConfigurator() {
		super(defaultTransformer);
	}

	/**
	 * @param berlinModelSource
	 * @param sourceReference
	 * @param destination
	 */
	private BfnXmlImportConfigurator(URI uri, ICdmDataSource destination) {
		super(defaultTransformer);
		setSource(uri);
		setDestination(destination);
	}


	@Override
	public BfnXmlImportState getNewState() {
		return new BfnXmlImportState(this);
	}

	/**
	 * @return
	 */
	public Element getSourceRoot(){
		URI source = getSource();
		try {
			URL url;
			url = source.toURL();
			Object o = url.getContent();
			InputStream is = (InputStream)o;
			Element root = XmlHelp.getRoot(is);
			makeNamespaces(root);
			return root;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private boolean makeNamespaces(Element root){
		bfnXmlNamespace = root.getNamespace();
		if (bfnXmlNamespace == null
				/**|| tcNamespace == null
				 * || tnNamespace == null
				 * || commonNamespace == null
				 * ||	geoNamespace == null
				 * || publicationNamespace == null*/){
			logger.warn("At least one Namespace is NULL");
		}
		return true;
	}

	@Override
	public Reference getSourceReference() {
		//TODO
		if (this.sourceReference == null){
			logger.warn("getSource Reference not yet fully implemented");
			sourceReference = ReferenceFactory.newDatabase();
			sourceReference.setTitleCache("", true);
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

	public Namespace getBfnXmlNamespace() {
		return bfnXmlNamespace;
	}
	public void setBfnXmlNamespace(Namespace bfnXmlNamespace) {
		this.bfnXmlNamespace = bfnXmlNamespace;
	}

	public boolean isDoTaxonNames() {
		return doTaxonNames;
	}
	public void setDoTaxonNames(boolean doTaxonNames) {
		this.doTaxonNames = doTaxonNames;
	}

	/**
	 * @return the doMetaData
	 */
	public boolean isDoMetaData() {
		return doMetaData;
	}
	/**
	 * @param doMetaData the doMetaData to set
	 */
	public void setDoMetaData(boolean doMetaData) {
		this.doMetaData = doMetaData;
	}


	public boolean isDoInformationImport() {
		return doInformationImport;
	}
	public void setDoInformationImport(boolean doInformationImport) {
		this.doInformationImport = doInformationImport;
	}

	public boolean isHasSecondList() {
		return hasSecondList;
	}
	public void setHasSecondList(boolean hasSecondList) {
		this.hasSecondList = hasSecondList;
	}

	@Override
    public void setNomenclaturalCode(NomenclaturalCode nomenclaturalCode) {
		this.nomenclaturalCode = nomenclaturalCode;
	}
	@Override
    public NomenclaturalCode getNomenclaturalCode(){
		return nomenclaturalCode;
	}

    /**
     * @return the doFeature
     */
    public boolean isDoFeature() {
        return doFeature;
    }
    /**
     * @param doFeature the doFeature to set
     */
    public void setDoFeature(boolean doFeature) {
        this.doFeature = doFeature;
    }

    /**
     * @return the doAdditionalTerms
     */
    public boolean isDoAdditionalTerms() {
        return doAdditionalTerms;
    }
    /**
     * @param doAdditionalTerms the doAdditionalTerms to set
     */
    public void setDoAdditionalTerms(boolean doAdditionalTerms) {
        this.doAdditionalTerms = doAdditionalTerms;
    }
}
