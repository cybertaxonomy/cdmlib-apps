/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.moose;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.format.reference.NomenclaturalSourceFormatter;
import eu.etaxonomy.cdm.format.reference.OriginalSourceFormatter;
import eu.etaxonomy.cdm.io.common.CdmExportBase;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.XmlExportState;
import eu.etaxonomy.cdm.io.common.mapping.out.IExportTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ICdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.name.HomotypicalGroup;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

/**
 * Classification or taxon tree exporter into Koperski et al. in BfN format.
 * 
 * @author a.mueller
 * @since 2023-08-05
 */
@Component
public class KoperskiClassificationExport
        extends CdmExportBase<KoperskiExportConfigurator,KoperskiExportState,IExportTransformer,File>{

    private static final long serialVersionUID = 4288364478648729869L;
    
    private static final Logger logger = LogManager.getLogger();


    public KoperskiClassificationExport() {
        this.ioName = this.getClass().getSimpleName();
    }

    @Override
    public long countSteps(KoperskiExportState state) {
        TaxonNodeFilter filter = state.getConfig().getTaxonNodeFilter();
        return getTaxonNodeService().count(filter);
    }

    @Override
    protected void doInvoke(KoperskiExportState state) {

        try {
            IProgressMonitor monitor = state.getConfig().getProgressMonitor();
            KoperskiExportConfigurator config = state.getConfig();

            //set root node
            if (config.getTaxonNodeFilter().hasClassificationFilter()) {
                Classification classification = getClassificationService()
                        .load(config.getTaxonNodeFilter().getClassificationFilter().get(0).getUuid());
                state.setRootId(classification.getRootNode().getUuid());
            } else if (config.getTaxonNodeFilter().hasSubtreeFilter()) {
                state.setRootId(config.getTaxonNodeFilter().getSubtreeFilter().get(0).getUuid());
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            TaxonNodeOutStreamPartitioner<XmlExportState> partitioner = TaxonNodeOutStreamPartitioner.NewInstance(this,
                    state, state.getConfig().getTaxonNodeFilter(), 100, monitor, null);

//            handleMetaData(state);  //FIXME metadata;
            monitor.subTask("Start partitioning");

            TaxonNode node = partitioner.next();
            while (node != null) {
                handleTaxonNode(state, node);
                node = partitioner.next();
            }

            state.getProcessor().createFinalResult(state);
        } catch (Exception e) {
            state.getResult().addException(e,
                    "An unexpected error occurred in main method doInvoke() " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTaxonNode(KoperskiExportState state, TaxonNode taxonNode) {

        if (taxonNode == null) {
            String message = "TaxonNode for given taxon node UUID not found. ";
            state.getResult().addError(message);
        } else {
            try {
                TaxonNode root = taxonNode;
                List<TaxonNodeDto> childNodes;
                if (root.hasChildNodes()) {
                    childNodes = new ArrayList<>();
                    for (TaxonNode child : root.getChildNodes()) {
                    	if (child != null) {
                    		childNodes.add(new TaxonNodeDto(child));
                    	}
                    }
                    state.getNodeChildrenMap().put(root.getUuid(), childNodes);

                    // add root to node map
                }
                TaxonNodeDto rootDto = new TaxonNodeDto(root);
                UUID parentUuid = root.getParent() != null ? root.getParent().getUuid()
                        : state.getClassificationUUID(root);
                List<TaxonNodeDto> children = state.getNodeChildrenMap().get(parentUuid);
                if (children != null && !children.contains(rootDto)) {
                    state.getNodeChildrenMap().get(parentUuid).add(rootDto);
                } else if (state.getNodeChildrenMap().get(parentUuid) == null) {
                    List<TaxonNodeDto> rootList = new ArrayList<>();
                    rootList.add(rootDto);
                    state.getNodeChildrenMap().put(parentUuid, rootList);
                }
                if (root.hasTaxon()) {
                    handleTaxon(state, root);
                }
            } catch (Exception e) {
                state.getResult().addException(e, "An unexpected error occurred when handling taxonNode "
                        + taxonNode.getUuid() + ": " + e.getMessage() + e.getStackTrace());
            }
        }
    }

    private void handleTaxon(KoperskiExportState state, TaxonNode taxonNode) {

        try {

        	//taxon table
            KoperskiExportTable table = KoperskiExportTable.CHECKLIST;
            String[] csvLine = new String[table.getSize()];
            
            //handle taxon
            Taxon taxon = CdmBase.deproxy(taxonNode.getTaxon());
            TaxonName name = taxon.getName();
 
            String vollName = name.getTitleCache();
            if (name.getRank().isSupraGeneric()) {
            	vollName = CdmUtils.concat(" ", name.getRank().getAbbreviation(), vollName);
            }
            vollName = addAddedInfo(name, vollName);
            csvLine[table.getIndex(KoperskiExportTable.VOLLNAME)] = vollName;

            //taxon id
            csvLine[table.getIndex(KoperskiExportTable.TAXON_ID)] = getKoperskiId(state, taxon);
            
            //child of
            Taxon parent = getParent(taxon);
            if (parent != null) {
            	csvLine[table.getIndex(KoperskiExportTable.TAXON_CHILD_OF)] = getKoperskiId(state, parent);
            }

            //syn flag
            csvLine[table.getIndex(KoperskiExportTable.SYN_FLAG)] = "1";
            
            //zitat
            String zitat;
            if (name.getNomenclaturalReference()!= null) {
            	zitat = NomenclaturalSourceFormatter.INSTANCE().format(name.getNomenclaturalSource());
            	csvLine[table.getIndex(KoperskiExportTable.ZITAT)] = zitat;
            }
            
            //fide
            String fide = name.getExtensionsConcat(KoperskiImport.uuidFideType, "; ");
            if (isNotBlank(fide)) {
            	csvLine[table.getIndex(KoperskiExportTable.FIDE)] = fide;
            }
            
            //comment taxon
            if (!taxon.getAnnotations().isEmpty()) {
            	String comment = taxon.getAnnotations().iterator().next().getText();
            	csvLine[table.getIndex(KoperskiExportTable.KOMMENTAR_TAXON)] = comment;
            };
            
            //tax_uuid
            csvLine[table.getIndex(KoperskiExportTable.TAX_UUID)] = taxon.getUuid().toString();
            
            //homotypic synonyms
            List<Synonym> homotypicSyns = taxon.getHomotypicSynonymsByHomotypicGroup();
            for (Synonym syn : homotypicSyns) {
            	handleSynonym(state, syn);
            }
            
            //heteroytypic synonyms
            List<HomotypicalGroup> heterotypicHomotypicGroups = taxon.getHeterotypicSynonymyGroups();
            for (HomotypicalGroup group: heterotypicHomotypicGroups){
                for (Synonym syn : taxon.getSynonymsInGroup(group)) {
                    handleSynonym(state, syn);
                }
            }

            //conceptRelations
            EnumSet<KoperskiExportTable> otherRelationsTables = KoperskiExportTable.conceptRelations();
            for (TaxonRelationship rel : taxon.getRelationsToThisTaxon()) {
            	TaxonRelationshipType type = rel.getType();
            	if (type.isAnyMisappliedName()) {
            		handleMisapplication(state, rel);
            	} else {
            		handleConceptRelation(state, rel, taxon, rel.getFromTaxon().getSec(), otherRelationsTables);
            	}
            }
//            //... not existing relations
            for (KoperskiExportTable conceptTable : otherRelationsTables) {
            	handleRelationshipTable(state, null, null, null, taxon, conceptTable);
            }

        	state.getProcessor().put(table, taxon, csvLine);

        } catch (Exception e) {
            state.getResult().addException(e, "An unexpected error occurred when handling the taxon node of "
                    + cdmBaseStr(taxonNode.getTaxon()) + ", titleCache:"+ taxonNode.getTaxon().getTitleCache()+": " + e.getMessage());
        }
    }

    private String addAddedInfo(TaxonName name, String vollName) {
		String addedInfo = name.getExtensionsConcat(KoperskiImport.uuidAddedInfo, "@"); //>1 should not happen
		if (addedInfo != null && addedInfo.startsWith(",")) {
			addedInfo = " " + addedInfo;
		}
		return CdmUtils.concat("", vollName, addedInfo);
	}

	private void handleMisapplication(KoperskiExportState state, TaxonRelationship rel) {
		
    	//taxon table
        KoperskiExportTable table = KoperskiExportTable.CHECKLIST;
        String[] csvLine = new String[table.getSize()];
        
        //handle taxon
        Taxon taxon = rel.getFromTaxon();
		TaxonName name = taxon.getName();
		Taxon koperskiTaxon = rel.getToTaxon();

        String vollName = name.getFullTitleCache();
        vollName = "– " + CdmUtils.concat(" ", vollName, taxon.getAppendedPhrase()); 
        csvLine[table.getIndex(KoperskiExportTable.VOLLNAME)] = vollName;

        //taxon id
        csvLine[table.getIndex(KoperskiExportTable.TAXON_ID)] = getKoperskiId(state, koperskiTaxon);

        //syn flag
        csvLine[table.getIndex(KoperskiExportTable.SYN_FLAG)] = "x";
        
        //zitat - should not exist
        String zitat;
        if (name.getNomenclaturalReference()!= null) {
        	zitat = NomenclaturalSourceFormatter.INSTANCE().format(name.getNomenclaturalSource());
        	csvLine[table.getIndex(KoperskiExportTable.ZITAT)] = zitat;
        }
        
        //fide - should not exist
        String fide = name.getExtensionsConcat(KoperskiImport.uuidFideType, "; ");
        if (isNotBlank(fide)) {
        	csvLine[table.getIndex(KoperskiExportTable.FIDE)] = fide;
        }
        
        //comment taxon - should not exist for MAN
        if (!taxon.getAnnotations().isEmpty()) {
        	String comment = taxon.getAnnotations().iterator().next().getText();
        	csvLine[table.getIndex(KoperskiExportTable.KOMMENTAR_TAXON)] = comment;
        };
        
    	state.getProcessor().put(table, taxon, csvLine);
	}
    
    private void handleConceptRelation(KoperskiExportState state, TaxonRelationship rel, Taxon koperskiTaxon, 
    		Reference sec, EnumSet<KoperskiExportTable> otherRelationsTables) {
		
    	//taxon table
        KoperskiExportTable table = KoperskiExportTable.CHECKLIST;
        String[] csvLine = new String[table.getSize()];
        KoperskiExportTable secTable = getChecklistTable(sec);
        String[] secCsvLine = new String[secTable.getSize()];
        
        //handle taxon
        Taxon taxon = rel.getFromTaxon();
        TaxonName name = taxon.getName();

        //VOLLNAME
        String vollName = name.getFullTitleCache();
        vollName = addAddedInfo(name, vollName);
        vollName = CdmUtils.concat(" ", vollName, taxon.getAppendedPhrase());
        secCsvLine[secTable.getIndex(KoperskiExportTable.VOLLNAME)] = vollName;
        //adapt rel type labels
        String relSymbol = getConceptRelationSymbol(rel);
        vollName = CdmUtils.concat(" ", relSymbol, vollName);
        if (taxon.getSec() != null) {
        	String secStr = taxon.getSec().getAbbrevTitleCache();
        	vollName = CdmUtils.concat(" sec. ", vollName, secStr);
        	csvLine[table.getIndex(KoperskiExportTable.SEC)] = secStr;
        }
        csvLine[table.getIndex(KoperskiExportTable.VOLLNAME)] = vollName;

        //taxon id
        csvLine[table.getIndex(KoperskiExportTable.TAXON_ID)] = getKoperskiId(state, koperskiTaxon);
        secCsvLine[secTable.getIndex(KoperskiExportTable.TAXON_ID)] = getId(state, taxon);

        //syn flag
        csvLine[table.getIndex(KoperskiExportTable.SYN_FLAG)] = "k";
        secCsvLine[secTable.getIndex(KoperskiExportTable.SYN_FLAG)] = "1";
        
        //child of - should not exist
        Taxon parent = getParent(taxon);
        if (parent != null) {
        	secCsvLine[table.getIndex(KoperskiExportTable.TAXON_CHILD_OF)] = getKoperskiId(state, parent);
        }

        //zitat - should not exist
        String zitat;
        if (name.getNomenclaturalReference()!= null) {
        	zitat = NomenclaturalSourceFormatter.INSTANCE().format(name.getNomenclaturalSource());
        	csvLine[table.getIndex(KoperskiExportTable.ZITAT)] = zitat;
        }
        
        //fide - should not exist
        String fide = name.getExtensionsConcat(KoperskiImport.uuidFideType, "; ");
        if (isNotBlank(fide)) {
        	csvLine[table.getIndex(KoperskiExportTable.FIDE)] = fide;
        }
        
        //comment taxon
        if (!rel.getAnnotations().isEmpty()) {
        	String comment = rel.getAnnotations().iterator().next().getText();
        	csvLine[table.getIndex(KoperskiExportTable.KOMMENTAR_KONZEPT)] = comment;
        };
        
        handleRelationship(state, rel, relSymbol, taxon, koperskiTaxon, taxon.getSec(), otherRelationsTables);
        
    	state.getProcessor().put(table, rel, csvLine);
    	state.getProcessor().put(secTable, taxon, secCsvLine);
	}

	private String getConceptRelationSymbol(TaxonRelationship rel) {
		TaxonRelationshipType type = rel.getType();
		String result;
		if (type.equals(TaxonRelationshipType.CONGRUENT_TO())) {
			result = "≙";
		} else if (type.equals(TaxonRelationshipType.OVERLAPS())) {
			result = "≶";
		} else if (type.equals(TaxonRelationshipType.EXCLUDES())) {
			result = "";
		} else if (type.equals(TaxonRelationshipType.INCLUDES())) {
			result = ">";
		} else if (type.equals(TaxonRelationshipType.UNCLEAR())) {
			result = "?";
		} else if (type.getUuid().equals(KoperskiImport.uuidProParteInclusion)) {
			result = "<";
		} else {
			logger.warn("Konzeptrelation symbol not yet handled explicitly: " + type.getTitleCache());
			result = type.getPreferredRepresentation(Language.DEFAULT()).getAbbreviatedLabel();
		}
		if (rel.isDoubtful() && !"?".equals(result)) {
			result = result + "?";
		}
		return result;
	}

	private KoperskiExportTable getChecklistTable(Reference sec) {
		String secStr = sec.getTitleCache();
		switch (secStr) {
		case "Corley & al. (1981)":
		case "Corley & al. (1981/1991)":
		case "Grolle & Long (2000)":
				return KoperskiExportTable.CHECKLIST_CORLEY;
		case "Smith (1980)":
		case "Smith (1990)":
				return KoperskiExportTable.CHECKLIST_SMITH;
		case "Frahm & Frey (1992)":
			return KoperskiExportTable.CHECKLIST_FRAHM_FREY;
		case "Ludwig & al. (1996)":
			return KoperskiExportTable.CHECKLIST_LUDWIG;
		case "Mönkemeyer (1927)":
		case "Müller (1954-1957)":
		case "Paul (1931)":
		case "Proskauer (1957)":
				return KoperskiExportTable.CHECKLIST_EARLY;
		case "Paton (1999)":
			return KoperskiExportTable.CHECKLIST_PATON;	
		default:
			logger.error("Sec not supported: " + secStr);
			return null;
		}
	}
	
	private KoperskiExportTable getRelationTable(Reference sec) {
		String secStr = sec.getTitleCache();
		switch (secStr) {
		case "Corley & al. (1981)":
		case "Corley & al. (1981/1991)":
		case "Grolle & Long (2000)":
				return KoperskiExportTable.CONCEPT_RELATION_CORLEY;
		case "Smith (1980)":
		case "Smith (1990)":
				return KoperskiExportTable.CONCEPT_RELATION_SMITH;
		case "Frahm & Frey (1992)":
			return KoperskiExportTable.CONCEPT_RELATION_FRAHM_FREY;
		case "Ludwig & al. (1996)":
			return KoperskiExportTable.CONCEPT_RELATION_LUDWIG;
		case "Mönkemeyer (1927)":
		case "Müller (1954-1957)":
		case "Paul (1931)":
		case "Proskauer (1957)":
				return KoperskiExportTable.CONCEPT_RELATION_EARLY;
		case "Paton (1999)":
			return KoperskiExportTable.CONCEPT_RELATION_PATON;	
		default:
			logger.error("Sec not supported: " + secStr);
			return null;
		}
	}

	private void handleRelationship(KoperskiExportState state, TaxonRelationship rel, String relSymbol, 
			Taxon otherTaxon, Taxon koperskiTaxon, Reference sec, EnumSet<KoperskiExportTable> otherRelationsTables) {
    	
		if (!otherTaxon.getMarkers(KoperskiImport.notInGermany).isEmpty()) {
			koperskiTaxon = null;
		}

		//taxon table
        KoperskiExportTable table = getRelationTable(sec);
        otherRelationsTables.remove(table);
        
        handleRelationshipTable(state, rel, relSymbol, otherTaxon, koperskiTaxon, table);
	}

	private void handleRelationshipTable(KoperskiExportState state, TaxonRelationship rel, String relSymbol, 
			Taxon otherTaxon, Taxon koperskyTaxon, KoperskiExportTable table) {
		
        String[] csvLine = new String[table.getSize()];
		if (otherTaxon != null) {
        	//taxon id1
        	csvLine[table.getIndex(KoperskiExportTable.TAXID1)] = getId(state, otherTaxon);
        	//taxon name 1
        	csvLine[table.getIndex(KoperskiExportTable.NAME1)] = otherTaxon.getName().getTitleCache();       	
        }

        if (koperskyTaxon != null) {
		    //taxon id2
		    csvLine[table.getIndex(KoperskiExportTable.TAXID2)] = getKoperskiId(state, koperskyTaxon);
		    //taxon name 2
		    csvLine[table.getIndex(KoperskiExportTable.NAME2)] = koperskyTaxon.getName().getTitleCache();
        }
        
        //status
    	if (otherTaxon != null && koperskyTaxon != null) {
    		csvLine[table.getIndex(KoperskiExportTable.STATUS)] = relSymbol; 
    	}
    	
    	//TODO avoid deduplication for rel
    	CdmBase id = rel != null ? rel : koperskyTaxon;
    	state.getProcessor().put(table, id, csvLine);
	}

	private Taxon getParent(Taxon taxon) {
		if (taxon == null || taxon.getTaxonNodes().isEmpty()) {
			return null;
		}
		TaxonNode parentNode = taxon.getTaxonNodes().iterator().next().getParent();
		
		return parentNode == null ? null : parentNode.getTaxon();
	}

	private void handleSynonym(KoperskiExportState state, Synonym syn) {
       	
		//taxon table
        KoperskiExportTable table = KoperskiExportTable.CHECKLIST;
        String[] csvLine = new String[table.getSize()];
        
        //name
        TaxonName name = syn.getName();
        
        //basionym
        boolean isBasionym = false;
        TaxonName accBasionym = syn.getAcceptedTaxon().getName().getBasionym();
        if (accBasionym != null && syn.getName().equals(accBasionym)) {
        	isBasionym = true;
        }
        
        //VOLLNAME
        String vollName = name.getTitleCache();
        vollName = addAddedInfo(name, vollName);
        String synonymSymbol = "=";
        if (syn.getType().equals(SynonymType.HOMOTYPIC_SYNONYM_OF)) {
        	synonymSymbol = isBasionym ? "≡B" : "≡";
        }
        vollName = CdmUtils.concat(" ", synonymSymbol, vollName);
        csvLine[table.getIndex(KoperskiExportTable.VOLLNAME)] = vollName;
        
        //id
        csvLine[table.getIndex(KoperskiExportTable.TAXON_ID)] = getKoperskiId(state, syn.getAcceptedTaxon());

        //flag
        String flag = isBasionym? "b" : "x";
        csvLine[table.getIndex(KoperskiExportTable.SYN_FLAG)] = flag;
        
        //zitat
        String zitat;
        if (name.getNomenclaturalReference()!= null) {
        	zitat = NomenclaturalSourceFormatter.INSTANCE().format(name.getNomenclaturalSource());
        	csvLine[table.getIndex(KoperskiExportTable.ZITAT)] = zitat;
        }
        
        //fide
        String fide = name.getExtensionsConcat(KoperskiImport.uuidFideType, "; ");
        if (isNotBlank(fide)) {
        	csvLine[table.getIndex(KoperskiExportTable.FIDE)] = fide;
        }
        
        //comment taxon - should not exist for synonyms
        if (!syn.getAnnotations().isEmpty()) {
        	String comment = syn.getAnnotations().iterator().next().getText();
        	csvLine[table.getIndex(KoperskiExportTable.KOMMENTAR_TAXON)] = comment;
        }
    	state.getProcessor().put(table, syn, csvLine);
	}


    private void handleSource(KoperskiExportState state, DescriptionElementBase element,
            KoperskiExportTable factsTable) {
//        ColDpExportTable table = ColDpExportTable.FACT_SOURCES;
//        try {
//            Set<DescriptionElementSource> sources = element.getSources();
//
//            for (DescriptionElementSource source : sources) {
//                if (!(source.getType().equals(OriginalSourceType.Import)
//                        && state.getConfig().isExcludeImportSources())) {
//                    String[] csvLine = new String[table.getSize()];
//                    Reference ref = source.getCitation();
//                    if ((ref == null) && (source.getNameUsedInSource() == null)) {
//                        continue;
//                    }
//                    if (ref != null) {
//                        if (!state.getReferenceStore().contains(ref.getUuid())) {
//                            handleReference(state, ref);
//
//                        }
//                        csvLine[table.getIndex(ColDpExportTable.REFERENCE_FK)] = getId(state, ref);
//                    }
//                    csvLine[table.getIndex(ColDpExportTable.FACT_FK)] = getId(state, element);
//
//                    csvLine[table.getIndex(ColDpExportTable.NAME_IN_SOURCE_FK)] = getId(state,
//                            source.getNameUsedInSource());
//                    csvLine[table.getIndex(ColDpExportTable.FACT_TYPE)] = factsTable.getTableName();
//                    if (StringUtils.isBlank(csvLine[table.getIndex(ColDpExportTable.REFERENCE_FK)])
//                            && StringUtils.isBlank(csvLine[table.getIndex(ColDpExportTable.NAME_IN_SOURCE_FK)])) {
//                        continue;
//                    }
//                    state.getProcessor().put(table, source, csvLine);
//                }
//            }
//        } catch (Exception e) {
//            state.getResult().addException(e, "An unexpected error occurred when handling single source "
//                    + cdmBaseStr(element) + ": " + e.getMessage());
//        }
    }

    private String getId(KoperskiExportState state, ICdmBase cdmBase) {
        if (cdmBase == null) {
            return "";
        }
        return cdmBase.getUuid().toString();
    }
    
    private String getKoperskiId(KoperskiExportState state, Taxon taxon) {
        if (taxon == null) {
            return "";
        }
        return taxon.getIdentifierString(KoperskiImport.uuidTaxonIdIdentifierType);
    }

//    /**
//     * Handles misapplied names (including pro parte and partial as well as pro
//     * parte and partial synonyms
//     */
//    private void handleProPartePartialMisapplied(ColDpExportState state, Taxon taxon, Taxon accepted, boolean isProParte, boolean isMisapplied, int index) {
//        try {
//            Taxon ppSyonym = taxon;
//            if (isUnpublished(state.getConfig(), ppSyonym)) {
//                return;
//            }
//            TaxonName name = ppSyonym.getName();
//            handleName(state, name, accepted);
//
//            ColDpExportTable table = ColDpExportTable.SYNONYM;
//            String[] csvLine = new String[table.getSize()];
//
//            csvLine[table.getIndex(ColDpExportTable.SYNONYM_ID)] = getId(state, ppSyonym);
//            csvLine[table.getIndex(ColDpExportTable.TAXON_FK)] = getId(state, accepted);
//            csvLine[table.getIndex(ColDpExportTable.NAME_FK)] = getId(state, name);
//
//            Reference secRef = ppSyonym.getSec();
//
//            if (secRef != null && !state.getReferenceStore().contains(secRef.getUuid())) {
//                handleReference(state, secRef);
//            }
//            csvLine[table.getIndex(ColDpExportTable.SEC_REFERENCE_FK)] = getId(state, secRef);
//            csvLine[table.getIndex(ColDpExportTable.SEC_REFERENCE)] = getTitleCache(secRef);
//            Set<TaxonRelationship> rels = accepted.getTaxonRelations(ppSyonym);
//            TaxonRelationship rel = null;
//            boolean isPartial = false;
//            if (rels.size() == 1){
//                rel = rels.iterator().next();
//
//            }else if (rels.size() > 1){
//                Iterator<TaxonRelationship> iterator = rels.iterator();
//                while (iterator.hasNext()){
//                    rel = iterator.next();
//                    if (isProParte && rel.getType().isAnySynonym()){
//                        break;
//                    } else if (isMisapplied && rel.getType().isAnyMisappliedName()){
//                        break;
//                    }else{
//                        rel = null;
//                    }
//                }
//            }
//            if (rel != null){
//                Reference synSecRef = rel.getCitation();
//                if (synSecRef != null && !state.getReferenceStore().contains(synSecRef.getUuid())) {
//                    handleReference(state, synSecRef);
//                }
//                csvLine[table.getIndex(ColDpExportTable.SYN_SEC_REFERENCE_FK)] = getId(state, synSecRef);
//                csvLine[table.getIndex(ColDpExportTable.SYN_SEC_REFERENCE)] = getTitleCache(synSecRef);
//                isProParte = rel.getType().isProParte();
//                isPartial = rel.getType().isPartial();
//
//            }else{
//                state.getResult().addWarning("An unexpected error occurred when handling "
//                        + "pro parte/partial synonym or misapplied name  " + cdmBaseStr(taxon) );
//            }
//
//            // pro parte type
//
//            csvLine[table.getIndex(ColDpExportTable.IS_PRO_PARTE)] = isProParte ? "1" : "0";
//            csvLine[table.getIndex(ColDpExportTable.IS_PARTIAL)] = isPartial ? "1" : "0";
//            csvLine[table.getIndex(ColDpExportTable.IS_MISAPPLIED)] = isMisapplied ? "1" : "0";
//            csvLine[table.getIndex(ColDpExportTable.SORT_INDEX)] = String.valueOf(index);
//            state.getProcessor().put(table, ppSyonym, csvLine);
//        } catch (Exception e) {
//            state.getResult().addException(e, "An unexpected error occurred when handling "
//                    + "pro parte/partial synonym or misapplied name  " + cdmBaseStr(taxon) + ": " + e.getMessage());
//        }
//    }


    private String extractStatusString(KoperskiExportState state, TaxonName name, boolean abbrev) {
        try {
            Set<NomenclaturalStatus> status = name.getStatus();
            if (status.isEmpty()) {
                return "";
            }
            String statusString = "";
            for (NomenclaturalStatus nameStatus : status) {
                if (nameStatus != null) {
                    if (abbrev) {
                        if (nameStatus.getType() != null) {
                            statusString += nameStatus.getType().getIdInVocabulary();
                        }
                    } else {
                        if (nameStatus.getType() != null) {
                            statusString += nameStatus.getType().getTitleCache();
                        }
                    }
                    if (!abbrev) {

                        if (nameStatus.getRuleConsidered() != null
                                && !StringUtils.isBlank(nameStatus.getRuleConsidered())) {
                            statusString += ": " + nameStatus.getRuleConsidered();
                        }
                        if (nameStatus.getCitation() != null) {
                            String shortCitation = OriginalSourceFormatter.INSTANCE.format(nameStatus.getCitation(), null);
                            statusString += " (" + shortCitation + ")";
                        }
//                        if (nameStatus.getCitationMicroReference() != null
//                                && !StringUtils.isBlank(nameStatus.getCitationMicroReference())) {
//                            statusString += " " + nameStatus.getCitationMicroReference();
//                        }
                    }
                    statusString += " ";
                }
            }
            return statusString;
        } catch (Exception e) {
            state.getResult().addException(e, "An unexpected error occurred when extracting status string for "
                    + cdmBaseStr(name) + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Returns a string representation of the {@link CdmBase cdmBase} object for
     * result messages.
     */
    private String cdmBaseStr(CdmBase cdmBase) {
        if (cdmBase == null) {
            return "-no object available-";
        } else {
            return cdmBase.getClass().getSimpleName() + ": " + cdmBase.getUuid();
        }
    }

    @Override
    protected boolean doCheck(KoperskiExportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(KoperskiExportState state) {
        return false;
    }
}