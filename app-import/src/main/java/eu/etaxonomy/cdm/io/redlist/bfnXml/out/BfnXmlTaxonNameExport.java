/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlTransformer;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StateData;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author pplitzner
 * @since May 3, 2016
 */
@Component
public class BfnXmlTaxonNameExport extends BfnXmlExportBase {

    private static final long serialVersionUID = -931703660108981011L;
    private static final Logger logger = LogManager.getLogger();

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

            exportFeatures(roteListeDaten);

            exportTaxonomy(classification, roteListeDaten, state);

        }
	}

    private void exportTaxonomy(Classification classification, Element roteListeDaten, BfnXmlExportState state) {
        Element taxonyme = new Element(BfnXmlConstants.EL_TAXONYME);
        roteListeDaten.addContent(taxonyme);
        List<TaxonNode> childNodes = classification.getChildNodes();
        java.util.Collections.sort(childNodes, new TaxonComparator());
        for (TaxonNode taxonNode : childNodes) {
            exportTaxon(taxonNode.getTaxon(), taxonyme, state);
        }
    }

    private void exportFactualData(Taxon taxon, Element parent) {
        Element informationen = new Element(BfnXmlConstants.EL_INFORMATIONEN);
        parent.addContent(informationen);
        Element bezugsraum = new Element(BfnXmlConstants.EL_BEZUGSRAUM);
        bezugsraum.setAttribute(new Attribute(BfnXmlConstants.ATT_NAME, BfnXmlConstants.BEZUGRAUM_BUND));
        informationen.addContent(bezugsraum);

        Set<TaxonDescription> descriptions = taxon.getDescriptions();
        for (TaxonDescription taxonDescription : descriptions) {
            //TODO: export only red list features ??
            Set<DescriptionElementBase> descriptionElements = taxonDescription.getElements();
            exportCategoricalData(BfnXmlConstants.VOC_RL_KAT, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_KAT, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_NEOBIOTA, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_RISIKOFAKTOREN, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_SONDERFAELLE, descriptionElements, taxon, parent);
            exportTextData(BfnXmlConstants.FEAT_LETZTER_NACHWEIS, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_VERANTWORTLICHKEIT, descriptionElements, taxon, parent);
            exportTextData(BfnXmlConstants.FEAT_KOMMENTAR_TAXONOMIE, descriptionElements, taxon, parent);
            exportTextData(BfnXmlConstants.FEAT_KOMMENTAR_GEFAEHRDUNG, descriptionElements, taxon, parent);
            exportTextData(BfnXmlConstants.FEAT_WEITERE_KOMMENTARE, descriptionElements, taxon, parent);
            exportCategoricalData(BfnXmlConstants.VOC_ALTE_RL_KAT, descriptionElements, taxon, parent);


//            for (DescriptionElementBase descriptionElementBase : descriptionElements) {
//                if(descriptionElementBase.isInstanceOf(CategoricalData.class)){
//                    CategoricalData categoricalData = HibernateProxyHelper.deproxy(descriptionElementBase, CategoricalData.class);
//                    Feature feature = categoricalData.getFeature();
//                    List<StateData> stateData = categoricalData.getStateData();
//                    if(stateData.size()!=1){
//                        logger.error("StateData does not have a size of 1 for feature "+feature.getLabel()+" in taxon "+taxon.getTitleCache());
//                        continue;
//                    }
//                    addIwert(bezugsraum, feature.getLabel(), stateData.iterator().next().getState().getLabel());
//                }
//                else if(descriptionElementBase.isInstanceOf(TextData.class)){
//                    TextData textData = HibernateProxyHelper.deproxy(descriptionElementBase, TextData.class);
//                    addIwert(bezugsraum, textData.getFeature().getLabel(), textData.getLanguageText(Language.GERMAN()).getText());
//                }
//            }
        }
    }

    private void exportTextData(String featureLabel, Set<DescriptionElementBase> descriptionElements, Taxon taxon, Element parent){
        for (DescriptionElementBase descriptionElementBase : descriptionElements) {
            if(descriptionElementBase.getFeature().getLabel().equals(featureLabel)){
              TextData textData = HibernateProxyHelper.deproxy(descriptionElementBase, TextData.class);
              addIwert(parent, textData.getFeature().getLabel(), textData.getLanguageText(Language.GERMAN()).getText());
            }
        }
    }

    private void exportCategoricalData(String featureLabel, Set<DescriptionElementBase> descriptionElements, Taxon taxon, Element parent){
        for (DescriptionElementBase descriptionElementBase : descriptionElements) {
            if(descriptionElementBase.getFeature().getLabel().equals(featureLabel)){
                CategoricalData categoricalData = HibernateProxyHelper.deproxy(descriptionElementBase, CategoricalData.class);
                Feature feature = categoricalData.getFeature();
                List<StateData> stateData = categoricalData.getStateData();
                if(stateData.size()!=1){
                    logger.error("StateData does not have a size of 1 for feature "+feature.getLabel()+" in taxon "+taxon.getTitleCache());
                    continue;
                }
                addIwert(parent, feature.getLabel(), stateData.iterator().next().getState().getLabel());
            }
        }
    }

    private void exportFeatures(Element roteListeDaten) {
        Element eigenschaften = new Element(BfnXmlConstants.EL_EIGENSCHAFTEN);
        roteListeDaten.addContent(eigenschaften);
        TermVocabulary<Feature> redListFeaturesVoc = getVocabularyService().load(BfnXmlTransformer.vocRLFeatures);
        Set<Feature> terms = redListFeaturesVoc.getTerms();
        for (Feature feature : terms) {
            //export red list features
            Element eigenschaft = new Element(BfnXmlConstants.EL_EIGENSCHAFT);
            eigenschaft.setAttribute(new Attribute(BfnXmlConstants.ATT_STANDARDNAME, feature.getLabel()));
            eigenschaften.addContent(eigenschaft);
            if(feature.isSupportsCategoricalData()){
                //export feature states
                Element listenwerte = new Element(BfnXmlConstants.EL_LISTENWERTE);
                eigenschaft.addContent(listenwerte);
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
        }
    }

    private void exportTaxon(Taxon taxon, Element parent, BfnXmlExportState state) {
        Element taxonym = new Element(BfnXmlConstants.EL_TAXONYM);
        parent.addContent(taxonym);

        //reihenfolge attribute
        taxonym.setAttribute(BfnXmlConstants.ATT_REIHENFOLGE, getExtension(taxon, ExtensionType.ORDER()));
                //getIdentifier(taxon, BfnXmlConstants.UUID_REIHENFOLGE_IDENTIFIER_TYPE));

        //taxNr attribute
        taxonym.setAttribute(BfnXmlConstants.ATT_TAXNR, getIdentifier(taxon, BfnXmlTransformer.UUID_TAX_NR_IDENTIFIER_TYPE));


        exportWissName(taxon, taxonym);

        //synonyms
        Set<Synonym> synonyms = taxon.getSynonyms();
        if(synonyms.size()>0){
            Element synonymeElement = new Element(BfnXmlConstants.EL_SYNONYME);
            taxonym.addContent(synonymeElement);
            for (Synonym synonym : synonyms) {
                Element synonymElement = new Element(BfnXmlConstants.EL_SYNONYM);
                synonymeElement.addContent(synonymElement);
                exportWissName(synonym, synonymElement);
            }
        }

        //common name
        exportCommonName(taxon, taxonym);

        //factual data
        exportFactualData(taxon, taxonym);


    }

    /**
     * @param taxon
     * @param order
     * @return
     */
    private String getExtension(Taxon taxon, ExtensionType order) {
        Set<String> set = taxon.getExtensions(order);
        if (set.size() != 1){
            logger.warn("Exactly 1 order extension should exist, but has " +  set.size());
            if (set.size() > 1){
                return set.iterator().next();
            }
            return null;
        }else{
            return set.iterator().next();
        }
    }

    private String getIdentifier(Taxon taxon, UUID identifierUuid) {
        DefinedTerm identifierType = HibernateProxyHelper.deproxy(getTermService().load(identifierUuid), DefinedTerm.class);
        Set<String> identfiers = taxon.getIdentifierStrings(identifierType);
        if(identfiers.size()==1){
            return identfiers.iterator().next();
        }
        else{
            logger.error("Taxon "+taxon.getTitleCache()+" has none or multiple identifiers of type '"+identifierType.getLabel()+"'");
            return null;
        }
    }

    private void exportWissName(TaxonBase<?> taxon, Element parent) {
        Element wissName = new Element(BfnXmlConstants.EL_WISSNAME);
        parent.addContent(wissName);

        INonViralName name = taxon.getName();
        Rank rank = name.getRank();
        //epithet 1,2,3
        exportEpithet(taxon, wissName, name, rank);

        //rank
        addNanteil(wissName, BfnXmlConstants.BEREICH_RANG, BfnXmlTransformer.getRankCodeForRank(rank));


        addNanteil(wissName, BfnXmlConstants.BEREICH_ORDNUNGSZAHL, null);//TODO
        addNanteil(wissName, BfnXmlConstants.BEREICH_AUTONYM, null);//TODO
        addNanteil(wissName, BfnXmlConstants.BEREICH_REICH, null);//TODO
        addNanteil(wissName, BfnXmlConstants.BEREICH_BASTARD, null);//TODO

        //authors
        addNanteil(wissName, BfnXmlConstants.BEREICH_AUTOREN, name.getAuthorshipCache());

        addNanteil(wissName, BfnXmlConstants.BEREICH_ZUSAETZE, null);//TODO
        addNanteil(wissName, "SortWissName", null);//TODO
        addNanteil(wissName, "SortArtEpi", null);//TODO
        addNanteil(wissName, "SortDeutName", null);//TODO

        //wissName
        addNanteil(wissName, BfnXmlConstants.BEREICH_WISSNAME, name.getTitleCache());
    }

    private void exportEpithet(TaxonBase<?> taxon, Element wissName, INonViralName name, Rank rank) {
        //eindeutiger Code
        Set<IdentifiableSource> sources = taxon.getSources();
        for (IdentifiableSource identifiableSource : sources) {
            if(identifiableSource.getType().equals(OriginalSourceType.Import)
                    && identifiableSource.getIdNamespace().equals(BfnXmlConstants.EL_TAXONYM+":"
                            +BfnXmlConstants.EL_WISSNAME+":"+BfnXmlConstants.EL_NANTEIL+":"+BfnXmlConstants.BEREICH_EINDEUTIGER_CODE)){
                addNanteil(wissName, BfnXmlConstants.BEREICH_EINDEUTIGER_CODE, identifiableSource.getIdInSource());
            }
        }

        //epitheton1-2
        addNanteil(wissName, BfnXmlConstants.BEREICH_EPITHETON1, name.getGenusOrUninomial());
        if(rank.isLower(Rank.GENUS())){
            String epitheton2 = name.getInfraGenericEpithet();
            if(epitheton2==null){
                epitheton2 = name.getSpecificEpithet();
            }
            addNanteil(wissName, BfnXmlConstants.BEREICH_EPITHETON2, epitheton2);
        }
        //epitheton3
        String epitheton3 = null;
        if(rank.isLower(Rank.SPECIES())){
            epitheton3 = name.getInfraSpecificEpithet();
        }
        if(epitheton3==null){
            epitheton3 = name.getSpecificEpithet();
        }
        addNanteil(wissName, BfnXmlConstants.BEREICH_EPITHETON3, epitheton3);

        //epitheton4-5
        addNanteil(wissName, BfnXmlConstants.BEREICH_EPITHETON4, null);
        addNanteil(wissName, BfnXmlConstants.BEREICH_EPITHETON5, null);
    }

    private void exportCommonName(Taxon taxon, Element taxonym) {
        Element deutscheNamen = new Element(BfnXmlConstants.EL_DEUTSCHENAMEN);
        taxonym.addContent(deutscheNamen);

        int sequenz = 1;
        Set<TaxonDescription> descriptions = taxon.getDescriptions();
        for (TaxonDescription taxonDescription : descriptions) {
            Set<DescriptionElementBase> elements = taxonDescription.getElements();
            for (DescriptionElementBase descriptionElementBase : elements) {
                if(descriptionElementBase.isInstanceOf(CommonTaxonName.class)){
                    CommonTaxonName commonName = HibernateProxyHelper.deproxy(descriptionElementBase, CommonTaxonName.class);
                    if(commonName.getLanguage().equals(Language.GERMAN())){
                        Element dName = new Element(BfnXmlConstants.EL_DNAME);
                        Element trivialName = new Element(BfnXmlConstants.EL_TRIVIALNAME);
                        deutscheNamen.addContent(dName);
                        dName.addContent(trivialName);

                        dName.setAttribute(new Attribute(BfnXmlConstants.ATT_SEQUENZ, String.valueOf(sequenz)));
                        trivialName.addContent(commonName.getName());
                    }
                }
            }
            sequenz++;
        }
    }

    private void addNanteil(Element element, String bereich, String textContent) {
        Element nanteil = new Element(BfnXmlConstants.EL_NANTEIL);
        nanteil.setAttribute(new Attribute(BfnXmlConstants.ATT_BEREICH, bereich));
        if(textContent!=null){
            nanteil.addContent(textContent);
        }
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

    private final class TaxonComparator implements Comparator<TaxonNode> {
        @Override
        public int compare(TaxonNode o1, TaxonNode o2) {
            Taxon taxon1 = o1.getTaxon();
            Taxon taxon2 = o2.getTaxon();

            int reihenfolge1 = Integer.parseInt(getExtension(taxon1, ExtensionType.ORDER()));
            int reihenfolge2 = Integer.parseInt(getExtension(taxon2, ExtensionType.ORDER()));

            return reihenfolge1-reihenfolge2;
        }
    }

}
