/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.DbImportConfiguratorBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 18.12.2015
 *
 */
public class EdaphobaseImportConfigurator
        extends DbImportConfiguratorBase<EdaphobaseImportState>
{
    private static final long serialVersionUID = 5397992611211404553L;
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EdaphobaseImportConfigurator.class);

    private boolean doTaxa = true;
    public boolean isDoTaxa() {return doTaxa;}
    public void setDoTaxa(boolean doTaxa) {this.doTaxa = doTaxa;}

    private boolean doSynonyms = true;
    public boolean isDoSynonyms() {return doSynonyms;}
    public void setDoSynonyms(boolean doSynonyms) {this.doSynonyms = doSynonyms;}

    private boolean doReferences = true;
    public boolean isDoReferences() {return doReferences;}
    public void setDoReferences(boolean doReferences) {this.doReferences = doReferences;}

    private boolean doDescriptions = true;
    public boolean isDoDescriptions() {return doDescriptions;}
    public void setDoDescriptions(boolean doDescriptions) {this.doDescriptions = doDescriptions;}


    private String edaphobaseSecundumTitle;
    public String getEdaphobaseSecundumTitle() {return edaphobaseSecundumTitle;}
    public void setEdaphobaseSecundumTitle(String edaphobaseSecundumTitle) {this.edaphobaseSecundumTitle = edaphobaseSecundumTitle;}

    //logging
    private boolean ignoreSubgenus = true;
    public boolean isIgnoreSubgenus() {return ignoreSubgenus;}
    public void setIgnoreSubgenus(boolean ignoreSubgenus) {this.ignoreSubgenus = ignoreSubgenus;}

    private boolean ignore4nomial = true;
    public boolean isIgnore4nomial() {return ignore4nomial;}
    public void setIgnore4nomial(boolean ignore4nomial) {this.ignore4nomial = ignore4nomial;}


    /**
     * @param source
     * @param cdmDestination
     * @return
     */
    public static EdaphobaseImportConfigurator NewInstance(Source source, ICdmDataSource cdmDestination) {
        return new EdaphobaseImportConfigurator(source, cdmDestination, new EdaphobaseImportTransformer());
    }

    /**
     * @param transformer
     */
    public EdaphobaseImportConfigurator(Source source, ICdmDataSource destination, EdaphobaseImportTransformer transformer) {
        super(source, destination, NomenclaturalCode.ICZN, transformer);
    }

    @Override
    public EdaphobaseImportState getNewState() {
        return new EdaphobaseImportState(this);
    }

    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                EdaphobaseAuthorImport.class,
                EdaphobaseReferenceImport.class,
                EdaphobaseInReferenceImport.class,
                EdaphobaseTaxonImport.class,
                EdaphobaseSynonymyImport.class,
                EdaphobaseSynonymy2Import.class,
                EdaphobaseClassificationImport.class,
//                EdaphobaseDescriptionImport.class,
        };
    }

    @Override
    public Reference getSourceReference() {
        Reference sourceRef = ReferenceFactory.newDatabase();
        sourceRef.setTitle("Edaphobase");
        return sourceRef;
    }


}
