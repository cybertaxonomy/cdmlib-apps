/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.phycobank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 2018-08-09
 */

@Component
public class PhycobankHigherClassificationExcelImport<CONFIG extends PhycobankHigherClassificationImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -77504409820284052L;
    private static final Logger logger = Logger.getLogger(PhycobankHigherClassificationExcelImport.class);

    private static final String GENUS = "genus";
    private static final String SUBFAMILIA = "subfam";
    private static final String FAMILIA = "familia";
    private static final String SUBORDO = "subordo";
    private static final String ORDO = "ordo";
    private static final String SUBCLASSIS = "subclassis";
    private static final String CLASSIS = "classis";
    private static final String PHYLUM = "phylum";

    private static final List<String> propertyPaths = null;
    private static TaxonRelationshipType relType;

    private Reference secReference;
    private Reference phycobankReference;


    @Override
    protected String getWorksheetName(CONFIG config) {
        return config.getWorksheetName();
    }

    private class RankedUninomial{
        public RankedUninomial(String uninomial, Rank rank) {
            this.uninomial = uninomial;
            this.rank = rank;
        }
        String uninomial;
        Rank rank;
        @Override
        public String toString() {
            return "RankedUninomial [uninomial=" + uninomial + ", rank=" + rank + "]";
        }
    }

    /**
	 *  Creates higher taxonomy
	 */
	@Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

	    @SuppressWarnings("deprecation")
        TaxonRelationshipType type = TaxonRelationshipType.TAXONOMICALLY_INCLUDED_IN();
	    relType = type;

	    String line = "line " + state.getCurrentLine();
	    System.out.println(line);

        Map<String, String> record = state.getOriginalRecord();

        List<RankedUninomial> rankedUninomials = makeRankedUninomials(state, record);

        createOrVerifyRankedUninomials(state, rankedUninomials, line);

		return;
    }


    /**
     * For the given list of {@link RankedUninomial}s names and taxa are created
     * if they do not yet exist. Parent child relationships as well as concept
     * relationships are created if they do not yet exists.
     * If the data is inconsistent to existing data inconsistency is logged and
     * no higher classification is created.
     * @param state
     * @param rankedUninomials
     * @param line
     */
    private TaxonNode createOrVerifyRankedUninomials(SimpleExcelTaxonImportState<CONFIG> state,
            List<RankedUninomial> rankedUninomials, String line) {

        if (rankedUninomials.isEmpty()){
            return getClassification(state, line).getRootNode();
        }else{
            RankedUninomial rankedUninomial = rankedUninomials.get(0);
            rankedUninomials.remove(0);
            Taxon taxon = getOrMakeTaxon(state, rankedUninomial, line);
            TaxonNode existingNode = taxon.getTaxonNode(getClassification(state, line));
            TaxonNode existingHigherNode = existingNode == null? null : existingNode.getParent();
            //recursive call
            TaxonNode createdHigher = createOrVerifyRankedUninomials(state, rankedUninomials, line);
//            boolean exists = verifyNextHigher(state, rankedUninomials, existingHigher);

            if (existingNode != null){
                if (existingHigherNode == null){
                    logger.warn(line + ": Higher node does not exist. This should not happen. Please check classification.");
                }else if (existingHigherNode.equals(createdHigher)){
                    //nothing to do;
                }else{
                    logger.warn(line + ": Inconsistency in data. Higher taxon for rank " + rankedUninomial.rank + "/" + rankedUninomial.uninomial +
                            " differs from existing higher taxon. Higher taxonomy not created. Please check classification.");
                }
            }else { //existingNode == null
                if (createdHigher == null){
                    logger.warn(line + ": Created higher node is null. This should not happen. Please check classification and concept relationships.");
                    return null;
                }else{
                    existingNode = createdHigher.addChildTaxon(taxon, getSecReference(state, line), line);
                    getTaxonNodeService().saveOrUpdate(existingNode);
                }
            }
            makeConceptRelation(state, existingNode, line, line);
            return existingNode;
        }
    }
//
//
//    /**
//     *
//     * @param state
//     * @param rankedUninomials
//     * @param nextHigher
//     * @return
//     */
//    private boolean verifyNextHigher(SimpleExcelTaxonImportState<CONFIG> state,
//            List<RankedUninomial> rankedUninomials,
//            TaxonNode nextHigher) {
//        if (nextHigher == null){
//            return false;
//        }else{
//            boolean result = true;
//            RankedUninomial rankedUninomial = rankedUninomials.get(0);
//            TaxonName name = nextHigher.getTaxon().getName();
//            if (!rankedUninomial.rank.equals(name.getRank())){
//                result = false;
//            }
//            if (!rankedUninomial.uninomial.equals(name.getNameCache())){
//                result = false;
//            }
//            return result;
//        }
//    }


    /**
     * Creates a list of {@link RankedUninomial}s for the given record.
     * Empty fields are not listed.
     * @param state
     * @param record
     * @return
     */
    private List<RankedUninomial> makeRankedUninomials(
            SimpleExcelTaxonImportState<CONFIG> state, Map<String, String> record) {

        List<RankedUninomial> result = new ArrayList<>();
        addRankedUninomial(result, record, GENUS, Rank.GENUS());
        addRankedUninomial(result, record, SUBFAMILIA, Rank.SUBFAMILY());
        addRankedUninomial(result, record, FAMILIA, Rank.FAMILY());
        addRankedUninomial(result, record, SUBORDO, Rank.SUBORDER());
        addRankedUninomial(result, record, ORDO, Rank.ORDER());
        addRankedUninomial(result, record, SUBCLASSIS, Rank.SUBCLASS());
        addRankedUninomial(result, record, CLASSIS, Rank.CLASS());
        addRankedUninomial(result, record, PHYLUM, Rank.PHYLUM());
        return result;
    }

    /**
     * Creates a {@link RankedUninomial} for a given record field and
     * adds it to the list. If the field is empty no {@link RankedUninomial} is
     * created.
     * @param list
     * @param record
     * @param fieldName
     * @param rank
     */
    private void addRankedUninomial(List<RankedUninomial> list, Map<String, String> record,
            String fieldName, Rank rank) {
        String uninomial = record.get(fieldName);
        if (isNotBlank(uninomial)){
            list.add(new RankedUninomial(uninomial.trim(), rank));
        }
    }


    /**
     * @param state
     * @param uninomial
     * @param rank
     * @param sec
     * @return
     */
    protected Taxon getOrMakeTaxon(SimpleExcelTaxonImportState<CONFIG> state,
            RankedUninomial rankedUninomial, String line) {

        Reference phycobankRef = getPhycobankReference(state);
        List<TaxonName> nameCandidates = getNameService().findNamesByNameCache(
                rankedUninomial.uninomial, MatchMode.EXACT, propertyPaths);
        List<TaxonName> names = rankedNames(state, nameCandidates, rankedUninomial.rank);

        Taxon taxon;
        if (names.isEmpty()){
            taxon = createTaxonAndName(state, rankedUninomial.uninomial,
                    rankedUninomial.rank, line);
        }else{
            if (names.size() > 1){
                List<Taxon> taxa = new ArrayList<>();
                for (TaxonName name : names){
                    taxa.addAll(getReferencedTaxa(name, state.getConfig().getPhycobankReference()));
                }
                if (taxa.isEmpty()){
                    logger.warn(line + ": (" +rankedUninomial.uninomial + ")More than 1 name matches, but no matching taxon exists. Create new taxon with arbitrary name.");
                    TaxonName name = names.get(0);
                    taxon = getOrCreateTaxon(state, name, phycobankRef, line);
                }else if (taxa.size() == 1){
                    taxon = taxa.get(0);
                }else{
                    logger.warn(line + ": (" +rankedUninomial.uninomial + ") More than 1 taxon matches, take arbitrary one. This is unexpected and could be improved in code by "
                            + "also checking parent relationships.");
                    taxon = taxa.get(0);
                }
            }else{
                TaxonName name = names.get(0);
                taxon = getOrCreateTaxon(state, name, phycobankRef, line);
            }
        }
        return taxon;
    }

    /**
     * @param state
     * @return
     */
    private Classification getClassification(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        Classification result = null;
        List<Classification> classifications = getClassificationService().list(null, null, null, null, null);
        Reference sec = getSecReference(state, line);
        for (Classification classification: classifications){
            if (classification.getReference() != null && classification.getReference().equals(sec)){
                result = classification;
            }
        }
        if (result == null){
            result = Classification.NewInstance(sec.getTitleCache());
            result.setReference(sec);
            getClassificationService().save(result);
        }
        return result;
    }


    /**
     * @param state
     */
    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state, String line) {
        String uuidStr = state.getOriginalRecord().get("reference");
        UUID uuid = uuidStr == null? null:UUID.fromString(uuidStr);
        if (uuid == null){
            logger.warn(line + ": reference uuid missing");
            uuid = state.getConfig().getSecReference().getUuid();
        }
        if (secReference == null || !secReference.getUuid().equals(uuid)){
            secReference = getReferenceService().find(uuid);
            if (secReference == null){
                secReference = ReferenceFactory.newGeneric();
                logger.warn(line + ": reference could not be found in database");
                secReference.setUuid(uuid);
                getReferenceService().save(secReference);
            }
        }
        return secReference;
    }

    private Reference getPhycobankReference(SimpleExcelTaxonImportState<CONFIG> state) {
        UUID uuid = state.getConfig().getPhycobankReference().getUuid();
        if (phycobankReference == null || !phycobankReference.getUuid().equals(uuid)){
            phycobankReference = getReferenceService().find(uuid);
            if (phycobankReference == null){
                phycobankReference = state.getConfig().getPhycobankReference();
                getReferenceService().save(phycobankReference);

            }
        }
        return phycobankReference;
    }


    /**
     * @param state
     * @param uninomial
     * @param rank
     * @param line
     * @return
     */
    private Taxon createTaxonAndName(SimpleExcelTaxonImportState<CONFIG> state, String uninomial,
            Rank rank, String line) {
        TaxonName newName = TaxonNameFactory.NewBotanicalInstance(rank);
        newName.setGenusOrUninomial(uninomial);

        newName.addPrimaryTaxonomicSource(getSourceReference(state));
        newName.addImportSource(null, null, getSourceReference(state), line);

        Taxon taxon = createTaxon(state, newName, line);
        return taxon;
    }

    /**
     * @param state
     * @param name
     * @param sec
     * @param line
     * @return
     */
    private Taxon getOrCreateTaxon(SimpleExcelTaxonImportState<CONFIG> state, TaxonName name,
            Reference phycobankRef, String line) {
        List<Taxon> taxa = getReferencedTaxa(name, phycobankRef);
        Taxon result;
        if (taxa.isEmpty()){
            return createTaxon(state, name, line);
        }else{
            if (taxa.size()> 1){
                logger.warn(line + ": More then 1 taxon matches for given name. Take arbitrary one.");
            }
            result = taxa.get(0);
        }
        return result;
    }


    /**
     * @param name
     * @param sec
     * @return
     */
    protected List<Taxon> getReferencedTaxa(TaxonName name, Reference sec) {
        List<Taxon> taxa = new ArrayList<>();
        for (Taxon taxon : name.getTaxa()){
            if (sec.equals(taxon.getSec())){
                taxa.add(taxon);
            }
        }
        return taxa;
    }

    /**
     * @param state
     * @param name
     * @param line
     * @return
     */
    private Taxon createTaxon(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonName name, String line) {
        Taxon taxon = Taxon.NewInstance(name, getPhycobankReference(state));
        taxon.addImportSource(null, null, getSourceReference(state), line);
        return taxon;
    }


    /**
     * Returns those names that match in rank
     * @return
     */
    private List<TaxonName> rankedNames(SimpleExcelTaxonImportState<CONFIG> state,
            List<TaxonName> nameCandidates,
            Rank rank) {
        List<TaxonName> result = new ArrayList<>();
        for (TaxonName name : nameCandidates) {
            if (rank.equals(name.getRank())){
                result.add(name);
            }
        }
        return result;
    }


    /**
     * @param state
     * @param childNode
     * @param microCitation
     */
    private void makeConceptRelation(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonNode childNode, String microCitation, String line) {

        Taxon child = childNode.getTaxon();
        Taxon parent = childNode.getParent().getTaxon();
        if (parent == null){
            return;
        }

        Reference sec = getSecReference(state, line);
        Set<TaxonRelationship> rels = child.getRelationsFromThisTaxon();
        boolean hasRelation = false;
        for (TaxonRelationship rel : rels){
            if (rel.getType().equals(relType)
                    && rel.getToTaxon().equals(parent)
                    && rel.getCitation().equals(sec)){
                hasRelation = true;
            }
        }
        if (!hasRelation){
            child.addTaxonRelation(parent, relType, sec, microCitation);
        }
    }

}
