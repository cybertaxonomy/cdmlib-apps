/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlTransformer;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.OriginalSourceType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;



/**
 *
 * @author pplitzner
 * @date May 3, 2016
 *
 */
@Component
public class BfnXmlTaxonNameExport extends BfnXmlExportBase<TaxonNameBase> {

    private static final long serialVersionUID = -931703660108981011L;

    private static final Logger logger = Logger.getLogger(BfnXmlTaxonNameExport.class);

	public BfnXmlTaxonNameExport(){
		super();
	}

	@Override
    protected void doInvoke(BfnXmlExportState state){
	    startTransaction(true);

	    Document document = state.getConfig().getDocument();


	    //get all classifications
	    List<Classification> classifications = getClassificationService().list(Classification.class, null, null, null, null);
	    for (Classification classification : classifications) {
            Element roteListeDaten = new Element(BfnXmlConstants.EL_ROTELISTEDATEN);
            roteListeDaten.setAttribute(new Attribute(BfnXmlConstants.ATT_INHALT, classification.getTitleCache()));
            document.getRootElement().addContent(roteListeDaten);

            //export features
            Element eigenschaften = new Element(BfnXmlConstants.EL_EIGENSCHAFTEN);
            roteListeDaten.addContent(eigenschaften);
            TermVocabulary<Feature> redListFeaturesVoc = getVocabularyService().load(BfnXmlConstants.vocRLFeatures);
            Set<Feature> terms = redListFeaturesVoc.getTerms();
            for (Feature feature : terms) {
                //export red list features
                Element eigenschaft = new Element(BfnXmlConstants.EL_EIGENSCHAFT);
                eigenschaft.setAttribute(new Attribute(BfnXmlConstants.ATT_STANDARDNAME, feature.getLabel()));
                eigenschaften.addContent(eigenschaft);
                //export feature states
                Element listenwerte = new Element(BfnXmlConstants.EL_LISTENWERTE);
                eigenschaften.addContent(listenwerte);
                Set<TermVocabulary<State>> supportedCategoricalEnumerations = feature.getSupportedCategoricalEnumerations();
                for (TermVocabulary<State> termVocabulary : supportedCategoricalEnumerations) {
                    Set<State> featureStates = termVocabulary.getTerms();
//                    int reihenfolge = 1;
                    for (State featureState : featureStates) {
                        Element lwert = new Element(BfnXmlConstants.EL_LWERT);
//                        lwert.setAttribute(new Attribute(BfnXmlConstants.ATT_REIHENFOLGE, String.valueOf(reihenfolge)));
                        lwert.addContent(featureState.getLabel());
                        listenwerte.addContent(lwert);

//                        reihenfolge++;
                    }
                }
            }

            //export taxonomy
            Element taxonyme = new Element(BfnXmlConstants.EL_TAXONYME);
            roteListeDaten.addContent(taxonyme);
            List<TaxonNode> childNodes = classification.getChildNodes();
            java.util.Collections.sort(childNodes, new Comparator<TaxonNode>() {

                @Override
                public int compare(TaxonNode o1, TaxonNode o2) {
                    Taxon taxon1 = o1.getTaxon();
                    Taxon taxon2 = o2.getTaxon();
                    int id1 = 0;
                    int id2 = 0;

                    Set<IdentifiableSource> sources1 = taxon1.getSources();
                    for (IdentifiableSource identifiableSource : sources1) {
                        if(identifiableSource.getType().equals(OriginalSourceType.Import)
                                && identifiableSource.getIdNamespace().equals(BfnXmlConstants.EL_TAXONYM+":"
                        +BfnXmlConstants.EL_WISSNAME+":"+BfnXmlConstants.EL_NANTEIL+":"+BfnXmlConstants.BEREICH_EINDEUTIGER_CODE)){
                            id1 = Integer.parseInt(identifiableSource.getIdInSource());
                        }
                    }
                    Set<IdentifiableSource> sources2 = taxon2.getSources();
                    for (IdentifiableSource identifiableSource : sources2) {
                        if(identifiableSource.getType().equals(OriginalSourceType.Import)
                                && identifiableSource.getIdNamespace().equals(BfnXmlConstants.EL_TAXONYM+":"
                                        +BfnXmlConstants.EL_WISSNAME+":"+BfnXmlConstants.EL_NANTEIL+":"+BfnXmlConstants.BEREICH_EINDEUTIGER_CODE)){
                            id2 = Integer.parseInt(identifiableSource.getIdInSource());
                        }
                    }
                    return id1-id2;
                }
            });
            for (TaxonNode taxonNode : childNodes) {
                exportTaxon(taxonNode.getTaxon(), taxonyme);
            }
        }

	    XMLOutputter outputter = new XMLOutputter();
	    outputter.setFormat(Format.getPrettyFormat());
	    try {
            outputter.output(document, new FileWriter(state.getConfig().getDestination()));
        } catch (IOException e) {
            logger.error("Error writing file", e);
        }

	}

    private void exportTaxon(Taxon taxon, Element parent) {
        Element taxonym = new Element(BfnXmlConstants.EL_TAXONYM);
        parent.addContent(taxonym);

        NonViralName name = HibernateProxyHelper.deproxy(taxon.getName(), NonViralName.class);
        Rank rank = name.getRank();
        //wissName
        addNanteil(taxonym, BfnXmlConstants.BEREICH_WISSNAME, taxon.getTitleCache());
        //epitithon 1,2,3
        addNanteil(taxonym, BfnXmlConstants.BEREICH_EPITHETON1, name.getGenusOrUninomial());
        if(rank.isLower(Rank.GENUS())){
            String epitheton2 = name.getInfraGenericEpithet();
            if(epitheton2==null){
                epitheton2 = name.getSpecificEpithet();
            }
            addNanteil(taxonym, BfnXmlConstants.BEREICH_EPITHETON2, epitheton2);
        }
        if(rank.isLower(Rank.SPECIES())){
            String epitheton3 = name.getInfraSpecificEpithet();
            if(epitheton3==null){
                epitheton3 = name.getSpecificEpithet();
            }
            addNanteil(taxonym, BfnXmlConstants.BEREICH_EPITHETON3, epitheton3);
        }
        Set<IdentifiableSource> sources = taxon.getSources();
        for (IdentifiableSource identifiableSource : sources) {
            if(identifiableSource.getType().equals(OriginalSourceType.Import)
                    && identifiableSource.getIdNamespace().equals(BfnXmlConstants.EL_TAXONYM+":"
            +BfnXmlConstants.EL_WISSNAME+":"+BfnXmlConstants.EL_NANTEIL+":"+BfnXmlConstants.BEREICH_EINDEUTIGER_CODE)){
                addNanteil(taxonym, BfnXmlConstants.BEREICH_EINDEUTIGER_CODE, identifiableSource.getIdInSource());
            }
        }
        //rank
        addNanteil(taxonym, BfnXmlConstants.BEREICH_RANG, BfnXmlTransformer.getRankmap().get(rank));

        //common name
        Element deutscheNamen = new Element(BfnXmlConstants.EL_DEUTSCHENAMEN);
        taxonym.addContent(deutscheNamen);

        int sequenz = 1;
        Set<TaxonDescription> descriptions = taxon.getDescriptions();
        for (TaxonDescription taxonDescription : descriptions) {
            Set<DescriptionElementBase> elements = taxonDescription.getElements();
            for (DescriptionElementBase descriptionElementBase : elements) {
                if(descriptionElementBase.isInstanceOf(CommonTaxonName.class)){
                    Element dName = new Element(BfnXmlConstants.EL_DNAME);
                    Element trivialName = new Element(BfnXmlConstants.EL_TRIVIALNAME);
                    deutscheNamen.addContent(dName);
                    dName.addContent(trivialName);

                    dName.setAttribute(new Attribute(BfnXmlConstants.ATT_SEQUENZ, String.valueOf(sequenz)));
                    trivialName.addContent(((CommonTaxonName)descriptionElementBase).getName());
                }
            }
            sequenz++;
        }

    }

    private void addNanteil(Element element, String bereich, String textContent) {
        Element nanteil = new Element(BfnXmlConstants.EL_NANTEIL);
        nanteil.setAttribute(new Attribute(BfnXmlConstants.ATT_BEREICH, bereich));
        nanteil.addContent(textContent);
        element.addContent(nanteil);
    }

    @Override
    protected boolean doCheck(BfnXmlExportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(BfnXmlExportState state) {
        return false;
    }

}
