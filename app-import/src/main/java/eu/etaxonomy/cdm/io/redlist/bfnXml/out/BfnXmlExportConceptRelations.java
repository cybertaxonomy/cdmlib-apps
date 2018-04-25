/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.io.redlist.bfnXml.in.BfnXmlTransformer;
import eu.etaxonomy.cdm.model.common.DefinedTerm;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;



/**
 *
 * @author pplitzner
 * @since May 3, 2016
 *
 */
@Component
public class BfnXmlExportConceptRelations extends BfnXmlExportBase {

    private static final long serialVersionUID = -931703660108981011L;


	public BfnXmlExportConceptRelations(){
		super();
	}

	@Override
    protected void doInvoke(BfnXmlExportState state){
	    startTransaction(true);

	    Document document = state.getConfig().getDocument();
	    Element konzeptbeziehungen = null;
	    List<Classification> list = getClassificationService().list(Classification.class, null, null, null, null);
	    DefinedTerm taxNrIdentifierType = HibernateProxyHelper.deproxy(getTermService().load(BfnXmlTransformer.UUID_TAX_NR_IDENTIFIER_TYPE), DefinedTerm.class);
	    for (Classification classification : list) {
            List<TaxonNode> childNodes = classification.getChildNodes();
            for (TaxonNode taxonNode : childNodes) {
                Set<TaxonRelationship> relationsFromThisTaxon = taxonNode.getTaxon().getRelationsFromThisTaxon();
                for (TaxonRelationship taxonRelationship : relationsFromThisTaxon) {
                    UUID uuid = taxonRelationship.getUuid();
                    //all of the relationships should have been processed
                    //when iterating over the taxa of the first classification
                    //if there are only 2 of them
                    if(!state.getKnownConceptRelations().contains(uuid)) {
                        if(konzeptbeziehungen==null){
                            konzeptbeziehungen = new Element(BfnXmlConstants.EL_KONZEPTBEZIEHUNGEN);
                            document.getRootElement().addContent(konzeptbeziehungen);
                        }
                        Element konzeptbeziehung = new Element(BfnXmlConstants.EL_KONZEPTBEZIEHUNG);
                        konzeptbeziehungen.addContent(konzeptbeziehung);

                        state.getKnownConceptRelations().add(uuid);

                        //fromTaxon = TAXONYM1
                        Taxon fromTaxon = taxonRelationship.getFromTaxon();
                        Element taxonym1 = new Element(BfnXmlConstants.EL_TAXONYM1);
                        konzeptbeziehung.addContent(taxonym1);
                        addConceptTaxonym(taxNrIdentifierType, taxonym1, fromTaxon);

                        //toTaxon = TAXONYM2
                        Taxon toTaxon = taxonRelationship.getToTaxon();
                        Element taxonym2 = new Element(BfnXmlConstants.EL_TAXONYM2);
                        konzeptbeziehung.addContent(taxonym2);
                        addConceptTaxonym(taxNrIdentifierType, taxonym2, toTaxon);

                        //relation type
                        Element konzeptSynonymStatus = new Element(BfnXmlConstants.EL_STATUS);
                        konzeptbeziehung.addContent(konzeptSynonymStatus);
                        konzeptSynonymStatus.setAttribute(BfnXmlConstants.ATT_STANDARDNAME, BfnXmlConstants.ATT_STATUS_KONZEPTSYNONYM_STATUS);
                        konzeptSynonymStatus.addContent(BfnXmlTransformer.getConceptCodeForTaxonRelation(taxonRelationship.getType()));
                    }
                }
            }
        }

	}

    private void addConceptTaxonym(DefinedTerm taxNrIdentifierType, Element taxonym, Taxon taxon) {
        String taxNr = getTaxNr(taxon, taxNrIdentifierType);
        if(taxon.getTaxonNodes().size()>1){
            logger.error(taxon+" has more than one taxon node. First one is used");
        }
        Classification classification = taxon.getTaxonNodes().iterator().next().getClassification();
        taxonym.setAttribute(BfnXmlConstants.ATT_TAXNR, taxNr);
        taxonym.setAttribute(BfnXmlConstants.ATT_QUELLE, classification.getTitleCache());
        addIwert(taxonym, BfnXmlConstants.ATT_NAME, taxon.getName().getTitleCache());
    }

    private String getTaxNr(Taxon taxon, DefinedTerm taxNrIdentifierType) {
        String taxNr = null;
        Set<String> identifiers = taxon.getIdentifiers(taxNrIdentifierType);
        if(identifiers.size()==1){
            taxNr = identifiers.iterator().next();
        }
        else{
            logger.error("Taxon "+taxon.getTitleCache()+" has none or multiple identifiers of type 'taxNr'");
        }
        return taxNr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doCheck(BfnXmlExportState state) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isIgnore(BfnXmlExportState state) {
        return false;
    }

}
