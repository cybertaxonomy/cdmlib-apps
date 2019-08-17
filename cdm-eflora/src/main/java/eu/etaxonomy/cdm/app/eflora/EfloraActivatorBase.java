/**
 *
 */
package eu.etaxonomy.cdm.app.eflora;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.events.IIoObserver;
import eu.etaxonomy.cdm.io.common.events.LoggingIoObserver;
import eu.etaxonomy.cdm.io.markup.FeatureSorter;
import eu.etaxonomy.cdm.io.markup.FeatureSorterInfo;
import eu.etaxonomy.cdm.io.markup.MarkupImportConfigurator;
import eu.etaxonomy.cdm.io.markup.MarkupImportState;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.term.TermNode;
import eu.etaxonomy.cdm.model.term.TermTree;

/**
 * @author a.mueller
 *
 */
public class EfloraActivatorBase {
	private static final Logger logger = Logger.getLogger(EfloraActivatorBase.class);

	protected MarkupImportConfigurator config;
	protected CdmDefaultImport<MarkupImportConfigurator> myImport;
	protected IIoObserver observer = new LoggingIoObserver();
	protected Set<IIoObserver> observerList = new HashSet<IIoObserver>();

	protected MarkupImportConfigurator doImport(URI source, ICdmDataSource cdmDestination, CHECK check, boolean h2ForCheck){
		observerList.add(observer);
		if (h2ForCheck && cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
			check = CHECK.CHECK_ONLY;
		}
		config = MarkupImportConfigurator.NewInstance(source, cdmDestination);
		config.setObservers(observerList);
		config.setCheck(check);

		myImport = new CdmDefaultImport<MarkupImportConfigurator>();

		return config;
	}

	protected TermTree<Feature> makeAutomatedFeatureTree(ICdmRepository app,
			MarkupImportState state, UUID featureTreeUuid, String featureTreeTitle){
		System.out.println("Start creating automated Feature Tree");
		TermTree<Feature> tree = TermTree.NewFeatureInstance(featureTreeUuid);
		tree.setTitleCache(featureTreeTitle, true);
		TermNode<Feature> root = tree.getRoot();

		ITermService termService = app.getTermService();
		FeatureSorter sorter = new FeatureSorter();
		TermNode<Feature> descriptionNode = null;

		//general features
		Map<String, List<FeatureSorterInfo>> generalList = state.getGeneralFeatureSorterListMap();
		List<UUID> uuidList = sorter.getSortOrder(generalList);
		Map<UUID, Feature> map = makeUuidMap(uuidList, termService);
		for (UUID key : uuidList){
			Feature feature = map.get(key);
			TermNode<Feature> node = root.addChild(feature);
			if (feature.equals(Feature.DESCRIPTION())){
				descriptionNode = node;
			}
		}
		TermNode<Feature> newNode = root.addChild(Feature.CITATION());


		//description features
		if (descriptionNode != null){
			Map<String, List<FeatureSorterInfo>> charList = state.getCharFeatureSorterListMap();
			uuidList = sorter.getSortOrder(charList);
			map = makeUuidMap(uuidList, termService);
			for (UUID key : uuidList){
				Feature feature = map.get(key);
				descriptionNode.addChild(feature);
			}
		}else{
			logger.warn("No description node found. Could not create feature nodes for description features.");
		}

		//save tree
		app.getFeatureTreeService().saveOrUpdate(tree);

		System.out.println("End creating automated Feature Tree");

		return tree;
	}

	private Map<UUID,Feature> makeUuidMap(Collection<UUID> uuids, ITermService termService){
		HashSet<UUID> uuidSet = new HashSet<UUID>();
		uuidSet.addAll(uuids);
		List<Feature> featureSet = (List)termService.find(uuidSet);

		Map<UUID,Feature> result = new HashMap<UUID, Feature>();
		for (Feature feature : featureSet){
			result.put(feature.getUuid(), feature);
		}
		return result;
	}


	/**
	 * @param markupConfig
	 * @param myImport
	 */
	protected void executeVolume(URI source, boolean include) {
		if (include){
			System.out.println("\nStart import from ("+ source.toString() + ") ...");
			config.setSource(source);
			myImport.invoke(config);
			System.out.println("End import from ("+ source.toString() + ")...");
		}
	}
}
