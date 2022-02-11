/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraFactCategoryImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -7920836240918111566L;

    private static final Logger logger = Logger.getLogger(MexicoEfloraFactCategoryImport.class);

	protected static final String NAMESPACE = "FactCategory";

	private static final String pluralString = "fact categories";
	private static final String dbTableName = "Eflora_CatalogoNombre4CDM";

	public MexicoEfloraFactCategoryImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
	    //TODO
		String sql = " SELECT IdCatNombre "
		        + " FROM " + dbTableName + " c "
		        + " ORDER BY c.Nivel1, c.Nivel2, c.Nivel3, c.Nivel4, c.Nivel5, c.Nivel6, c.Nivel7, c.IdCatNombre ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName + " c";
		String sqlWhere = " WHERE ( IdCatNombre IN (" + ID_LIST_TOKEN + ") )";
		String sqlOrderBy = "ORDER BY c.Nivel1, c.Nivel2, c.Nivel3, c.Nivel4, c.Nivel5, c.Nivel6, c.Nivel7, c.IdCatNombre";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere + sqlOrderBy ;
		return strRecordQuery;
	}

	private class TreeNode{
	    TreeNode parent;
	    String key;
	    int idCatNombre;
	    String description;
	    List<TreeNode> children = new ArrayList<>();

	    private void addChild(TreeNode child) {
	        child.parent = this;
	        children.add(child);
	    }

        @Override public String toString() {return key + "("+ idCatNombre + "): " + description;}
	}

	private TreeNode root;
	private Map<Integer,TreeNode> idNodeMap = new HashMap<>();
	private Map<String,TreeNode> keyNodeMap = new HashMap<>();


	@Override
    protected void doInvoke(MexicoEfloraImportState state) {
	    root = new TreeNode();
        super.doInvoke(state);
        saveTree(state);
    }

    private void saveTree(MexicoEfloraImportState state) {
        TransactionStatus tx = this.startTransaction();

        @SuppressWarnings("unchecked")
        TermVocabulary<Feature> featureVoc = TermVocabulary.NewInstance(TermType.Feature);
//        featureVoc.setLabel("Catalogo", Language.SPANISH_CASTILIAN());
        featureVoc.setTitleCache("Catalogo", true);

        TermTree<Feature> featureTree = TermTree.NewFeatureInstance(state.getConfig().getFeatureTreeUuid());
        featureTree.setTitleCache(state.getConfig().getFeatureTreeTitle(), true);
        featureTree.setUuid(state.getConfig().getFeatureTreeUuid());
        featureTree.getRoot().addChild(Feature.DISTRIBUTION());
        getTermTreeService().save(featureTree);

        TermTree<Feature> flatFeatureTree = TermTree.NewFeatureInstance(state.getConfig().getFeatureTreeUuid());
        flatFeatureTree.setTitleCache(state.getConfig().getFlatFeatureTreeTitle(), true);
        flatFeatureTree.setUuid(state.getConfig().getFlatFeatureTreeUuid());
        flatFeatureTree.getRoot().addChild(Feature.DISTRIBUTION());
        getTermTreeService().save(flatFeatureTree);


        getVocabularyService().save(featureVoc);
        for (TreeNode child : root.children) {
            saveNodeRecursive(state, child, featureVoc, null, featureTree.getRoot(), flatFeatureTree.getRoot());
        }

        featureTree.getRoot().addChild(Feature.COMMON_NAME());
        flatFeatureTree.getRoot().addChild(Feature.COMMON_NAME());

        this.commitTransaction(tx);
    }

    private void saveNodeRecursive(MexicoEfloraImportState state, TreeNode node, TermVocabulary<Feature> featureVoc, Feature parentFeature,
            TermNode<Feature> parentFeatureTreeNode, TermNode<Feature> flatFeatureTreereTreeNode) {
        if (!node.children.isEmpty()) {
            //is feature
            String sep = UTF8.EN_DASH_SPATIUM.toString();
            String label = (parentFeature == null? "" : parentFeature.getTitleCache() ) + sep + node.description;
            label = label.startsWith(sep)? label.substring(sep.length()):label;
            Feature feature = Feature.NewInstance(label, label, String.valueOf(node.idCatNombre), Language.SPANISH_CASTILIAN());
            feature.setIdInVocabulary(node.key);
            feature.setSupportsCategoricalData(true);
            feature.setSupportsTextData(false);
            featureVoc.addTerm(feature);
            TermNode<Feature> featureTreeNode = parentFeatureTreeNode.addChild(feature);
            flatFeatureTreereTreeNode.addChild(feature);
            getTermService().save(feature);
            //parent-child
            if (parentFeature != null) {
//                parentFeature.addGeneralizationOf(feature);
                parentFeature.addIncludes(feature);
            }
            for (TreeNode child : node.children) {
                saveNodeRecursive(state, child, featureVoc, feature, featureTreeNode, flatFeatureTreereTreeNode);
            }
        }else {
            TermVocabulary<State> supportedStates = parentFeature.getSupportedCategoricalEnumerations().stream().findAny().orElse(null);
            if (supportedStates == null) {
                supportedStates = TermVocabulary.NewInstance(TermType.State, State.class);
                supportedStates.setTitleCache(parentFeature.getTitleCache(), true);
                supportedStates.setLabel("States for " + parentFeature.getTitleCache() , Language.SPANISH_CASTILIAN());
                getVocabularyService().save(supportedStates);
                parentFeature.addSupportedCategoricalEnumeration(supportedStates);
            }
            State categoricalState = State.NewInstance(node.description, node.description, String.valueOf(node.idCatNombre), Language.SPANISH_CASTILIAN());
            categoricalState.setIdInVocabulary(node.key);
            state.getStateMap().put(node.idCatNombre, categoricalState);
            state.getFeatureMap().put(node.idCatNombre, parentFeature);
            getTermService().save(categoricalState);
            supportedStates.addTerm(categoricalState);
        }
    }

    @Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

//			    int id = rs.getInt("id");
				int idCatNombre = rs.getInt("IdCatNombre");
				String descripcion = rs.getString("Descripcion");
				try {

				    //create node
                    TreeNode node = new TreeNode();
                    node.description = descripcion;
                    node.idCatNombre = idCatNombre;

                    //register node
                    idNodeMap.put(idCatNombre, node);
                    String key = createKey(rs, false);
                    node.key = key;
                    TreeNode oldKey = keyNodeMap.put(key, node);
                    if (oldKey != null) {
                        logger.warn("Key already exists");
                    }

                    //add to parent
                    String parentKey = createKey(rs, true);
                    TreeNode parentNode = keyNodeMap.get(parentKey);
                    if (parentNode != null) {
                        parentNode.addChild(node);
                    }else {
                        root.addChild(node);
                    }

					partitioner.startDoSave();
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCatNombre + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		return success;
	}

    private String createKey(ResultSet rs, boolean forParent) throws SQLException {
        int nivel1 = rs.getInt("Nivel1");
        int nivel2 = rs.getInt("Nivel2");
        int nivel3 = rs.getInt("Nivel3");
        int nivel4 = rs.getInt("Nivel4");
        int nivel5 = rs.getInt("Nivel5");
        int nivel6 = rs.getInt("Nivel6");
        int nivel7 = rs.getInt("Nivel7");

        Integer[] nivels = new Integer[] {nivel1, nivel2, nivel3, nivel4,
                nivel5, nivel6, nivel7, 0};
        String result = "";
        boolean finished = false;
        for (int i = 0; i< nivels.length -1 ; i++) {
            Integer nivel = nivels[i];
            if (nivels[i+1] > 0 || !forParent && nivels[i] > 0) {
                if (finished) {
                    logger.warn("Was already finished");
                }
                result += "-" + nivel;
            }else {
                finished = true;
            }
        }
        if (result.startsWith("-")) {
            result = result.substring(1);
        }

        return result;

    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
//				handleForeignKey(rs, nameIdSet, "PTNameFk");
//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

    @Override
    protected boolean doCheck(MexicoEfloraImportState state){
        return true;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}