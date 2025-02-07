/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.pesi.archive.app.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 * @since 03.07.2008
 */
public class TreeCreator {

    @SuppressWarnings("unused")
    private static Logger logger = LogManager.getLogger();

	public static TermTree<Feature> flatTree(UUID featureTreeUuid, Map<Integer, Feature> featureMap, Object[] featureKeyList){
	    TermTree<Feature> result = TermTree.NewFeatureInstance(featureTreeUuid);
		TermNode<Feature> root = result.getRoot();

		for (Object featureKey : featureKeyList){
			Feature feature = featureMap.get(featureKey);
			if (feature != null){
				root.addChild(feature);
			}
		}
		return result;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<Integer, Feature>  map = new HashMap<>(null);
		map.put(1, Feature.DISTRIBUTION());
		map.put(2, Feature.ECOLOGY());

		Object[] strFeatureList = new Integer[]{1,2};

		TermTree<Feature> tree = TreeCreator.flatTree(UUID.randomUUID(), map, strFeatureList);
		System.out.println(tree.getRootChildren());
	}
}
