/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.images;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author n.hoffmann
 * @since 11.11.2008
 */
public class ImageImportConfigurator
        extends ImportConfiguratorBase<ImageImportState, URI> {

    private static final long serialVersionUID = -3177654877920655720L;
    private static final Logger logger = Logger.getLogger(ImageImportConfigurator.class);

	public static ImageImportConfigurator NewInstance(URI source, ICdmDataSource destination, String mediaUrlString, Class<? extends AbstractImageImporter> importerClass){
		return new ImageImportConfigurator(source, destination, mediaUrlString, importerClass);
	}

	//TODO
	private static IInputTransformer defaultTransformer = null;

	public static ImageImportConfigurator NewInstance(URI source, ICdmDataSource destination, Class<? extends AbstractImageImporter> importerClass){
		return new ImageImportConfigurator(source, destination, null, importerClass);
	}

	private ImageImportConfigurator(URI source, ICdmDataSource destination, String mediaUrlString, Class<? extends AbstractImageImporter> importerClass){
		super(defaultTransformer);
		FileNotFoundException e;
		setSource(source);
		setDestination(destination);
		setMediaUrlString(mediaUrlString);
		ioClassList = new Class[]{importerClass};
	}

	private String mediaUrlString = null;

	@Override
    public Reference getSourceReference() {
	//TODO
		if (this.sourceReference == null){
			logger.warn("getSource Reference not yet fully implemented");
			sourceReference = ReferenceFactory.newDatabase();
			sourceReference.setTitleCache("XXX", true);
		}
		return sourceReference;
	}

	@SuppressWarnings("unchecked")
    @Override
	//NOT used, component class is injected via constructor
	protected void makeIoClassList() {
		ioClassList = new Class[] {
				AbstractImageImporter.class
		};
	}

	public String getMediaUrlString() {
		if(mediaUrlString == null){
			throw new NullPointerException("mediaUrlString has not been set");
		}
		return mediaUrlString;
	}

	public void setMediaUrlString(String mediaUrlString) {
		this.mediaUrlString = mediaUrlString;
	}

	@Override
    public String getSourceNameString() {
		return "Image file " + getSource();
	}

	@SuppressWarnings("unchecked")
    @Override
    public ImageImportState getNewState() {
		return new ImageImportState(this);
	}
}
