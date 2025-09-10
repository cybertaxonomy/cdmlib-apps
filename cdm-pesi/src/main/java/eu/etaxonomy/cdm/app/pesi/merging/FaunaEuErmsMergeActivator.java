package eu.etaxonomy.cdm.app.pesi.merging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.exception.ReferencedObjectUndeletableException;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.Credit;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.NameRelationship;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

public class FaunaEuErmsMergeActivator extends PesiMergeBase{

    private static Logger logger = LogManager.getLogger();

	static final ICdmDataSource faunaEuropaeaSource = CdmDestinations.localH2();

	static final int faunaEuUuid = 0;
	static final int ermsUuid = 9;
	static final int rankFaunaEu = 4;
	static final int rankErms = 13;
	private Classification faunaEuClassification;
	private Classification ermsClassification;

	private CdmApplicationController appCtrInit;

	//csv files starting with...
	static String sFileName = "c:\\test";

	private void initDb(ICdmDataSource db) {
		// Init source DB
		appCtrInit = CdmIoApplicationController.NewInstance(db, DbSchemaValidation.VALIDATE, false);
	}

	public static void main(String[] args) {

		FaunaEuErmsMergeActivator sc = new FaunaEuErmsMergeActivator();

		sc.initDb(faunaEuropaeaSource);
		//we also need to merge names completely identical!
		sc.mergeAuthors();

		//set the ranks of Agnatha and Gnathostomata to 50 instead of 45
		List<TaxonBase> taxaToChangeRank = new ArrayList<>();

		Pager<TaxonBase> agnatha = sc.appCtrInit.getTaxonService().findTaxaByName(
		        TaxonBase.class, "Agnatha", null, null, null, "*", Rank.INFRAPHYLUM(), null, 10, 0, null);
		List<TaxonBase> agnathaList = agnatha.getRecords();
		taxaToChangeRank.addAll(agnathaList);

		Pager<TaxonBase> gnathostomata = sc.appCtrInit.getTaxonService().findTaxaByName(
		        TaxonBase.class, "Gnathostomata", null, null, null, "*", Rank.INFRAPHYLUM(), null, 10, 0, null);
		List<TaxonBase> gnathostomataList = gnathostomata.getRecords();
		taxaToChangeRank.addAll(gnathostomataList);

		sc.setSpecificRank(taxaToChangeRank, Rank.SUPERCLASS());

		//ermsTaxon is accepted, faunaEu taxon is synonym
		//ermsTaxon is synonym, faunaEu is accepted

		sc.mergeDiffStatus();

		//erms is synonym, faunaEu as well

		// erms is accepted, faunaEu as well

	}

	private void mergeAuthors(){
		List<List<String>> authors = readCsvFile(sFileName + "_authors.csv");
		//authors: get firstAuthor if isFauEu = 1 otherwise get secondAuthor

		Iterator<List<String>> authorIterator = authors.iterator();
		List<TaxonBase<?>> taxaToSave = new ArrayList<>();  //TODO: needed?
		while (authorIterator.hasNext()){
		    List<String> row = authorIterator.next();
			UUID uuidFaunaEu = UUID.fromString(row.get(faunaEuUuid));
			UUID uuidErms = UUID.fromString(row.get(ermsUuid));
			TaxonBase<?> taxonFaunaEu = appCtrInit.getTaxonService().find(uuidFaunaEu);
			TaxonBase<?> taxonErms = appCtrInit.getTaxonService().find(uuidErms);
// which information should be used can be found in last row -> needs to be done manually
			if (Integer.parseInt(row.get(18)) == 1){
				//isFaunaEu = 1 -> copy the author of Fauna Europaea to Erms
				if (taxonFaunaEu.getName().getBasionymAuthorship()!= null){
					taxonErms.getName().setBasionymAuthorship(taxonFaunaEu.getName().getBasionymAuthorship());
				}
				if (taxonFaunaEu.getName().getCombinationAuthorship()!= null){
					taxonErms.getName().setCombinationAuthorship(taxonFaunaEu.getName().getCombinationAuthorship());
				}
				taxonErms.getName().generateAuthorship();
				taxaToSave.add(taxonErms);
			}else{
				if (taxonErms.getName().getBasionymAuthorship()!= null){
					taxonFaunaEu.getName().setBasionymAuthorship(taxonErms.getName().getBasionymAuthorship());
				}
				if (taxonErms.getName().getCombinationAuthorship()!= null){
					taxonFaunaEu.getName().setCombinationAuthorship(taxonErms.getName().getCombinationAuthorship());
				}
				taxonFaunaEu.getName().generateAuthorship();
				taxaToSave.add(taxonFaunaEu);
			}
		}
	}

	private void setSpecificRank(List<TaxonBase> taxa, Rank rank){
		for (TaxonBase<?> taxon: taxa){
			taxon.getName().setRank(rank);
		}
	}

	private void mergeDiffStatus(){
		List<List<String>> diffStatus = readCsvFile(sFileName + "_status.csv");

		//find all taxa accepted in erms, but synonyms in FauEu  and the same rank
		List<List<String>> accErmsSynFaunaEu = new ArrayList<>();
		for (List<String> rowList: diffStatus){
			if ((rowList.get(5).equals("synonym")) && (rowList.get(rankFaunaEu).equals(rowList.get(rankErms)))){
				//both conditions are true
				accErmsSynFaunaEu.add(rowList);
			}
		}
		mergeErmsAccFaunaEuSyn(accErmsSynFaunaEu);

		//find all taxa accepted in faunaEu, but synonyms in Erms and the same rank
		List<List<String>> synErmsAccFaunaEu = new ArrayList<>();
		for (List<String> rowList: diffStatus){
			if ((rowList.get(5).equals("accepted")) && (rowList.get(rankFaunaEu).equals(rowList.get(rankErms)))){
				//both conditions are true
				synErmsAccFaunaEu.add(rowList);
			}
		}
		mergeErmsSynFaunaEuAcc(synErmsAccFaunaEu);
	}

	private void mergeSameStatus(){
		List<List<String>> sameStatus = readCsvFile(sFileName + "_names.csv");

		TaxonBase<?> taxonFaunaEu;
		TaxonBase<?> taxonErms;
		List<String> propertyPaths = new ArrayList<>();
		propertyPaths.add("taxonBases.nodes.*");
		for (List<String> row: sameStatus){
			taxonFaunaEu = appCtrInit.getTaxonService().load(UUID.fromString(row.get(faunaEuUuid)), propertyPaths);
			taxonErms = appCtrInit.getTaxonService().load(UUID.fromString(row.get(ermsUuid)), propertyPaths);
			moveAllInformationsFromFaunaEuToErms(taxonFaunaEu, taxonErms);
			if (taxonErms instanceof Taxon){
				moveFaunaEuSynonymsToErmsTaxon((Taxon)taxonFaunaEu, (Taxon)taxonErms);
			}
		}
	}

	private void mergeErmsAccFaunaEuSyn(List<List<String>> ermsAccFaEuSyn){

		// update nameRelationships -> if the nameRelationship does not exist, then create a new one with ermsAcc as relatedTo TaxonName
		updateNameRelationships(ermsAccFaEuSyn);

		//delete all synonyms of FaunaEu Syn TODO: move sources and additional informations to erms taxon
		for (List<String> rowList: ermsAccFaEuSyn){
			UUID faunaUUID = UUID.fromString(rowList.get(faunaEuUuid));
			//UUID ermsUUID = UUID.fromString(rowList.get(ermsUuid));
			Synonym syn = (Synonym)appCtrInit.getTaxonService().find(faunaUUID);
			// remove synonym from taxon then delete
			appCtrInit.getTaxonService().deleteSynonym(syn, null);
		}

		//merge the infos of
	}

	private  void mergeErmsSynFaunaEuAcc (List<List<String>> ermsAccFaEuSyn){
		//occurence: connect instead of Fauna Europaea taxon the accepted taxon of the synonym with the occurrence (CDM -> distribution)
		//search distribution (via taxon of the taxon description), of which the taxon is the according Fauna Eu taxon and connect it with the accepted taxon of the ERMS syn
		for (List<String> row: ermsAccFaEuSyn){
		    Taxon taxonFaunaEu = (Taxon)appCtrInit.getTaxonService().find(UUID.fromString(row.get(faunaEuUuid)));
			Synonym synErms = (Synonym)appCtrInit.getTaxonService().find(UUID.fromString(row.get(ermsUuid)));
			synErms = HibernateProxyHelper.deproxy(synErms, Synonym.class);
			Taxon taxonErms = synErms.getAcceptedTaxon();

			if (taxonErms == null){
				logger.debug("There is no accepted taxon for the synonym" + synErms.getTitleCache());
			}

			Set<Feature> features = new HashSet<>();
			features.add(Feature.DISTRIBUTION());
			List<String> propertyPaths = new ArrayList<>();
			propertyPaths.add("inDescription.Taxon.*");
			List<Distribution> distributions = appCtrInit.getDescriptionService()
			        .listDescriptionElementsForTaxon(taxonFaunaEu, features, Distribution.class, false, 10, 0, null);


			for(Distribution distribution: distributions){
				TaxonDescription description = (TaxonDescription)distribution.getInDescription();
				TaxonDescription newDescription = TaxonDescription.NewInstance(taxonErms);
				newDescription.addElement(distribution);
				try{
					appCtrInit.getDescriptionService().delete(description);
				}catch (Exception e){
					logger.debug("The description of" + description.getTaxon().getTitleCache() + description.getTitleCache() + "can't be deleted because it is referenced.");
				}
			}

			//Child-Parent Relationship aktualisieren -> dem Child des Fauna Europaea Taxons als parent das akzeptierte Taxon von synErms
			Set<TaxonNode> nodesErms = taxonErms.getTaxonNodes();
			Set<TaxonNode> nodesFaunaEu =taxonFaunaEu.getTaxonNodes();
			if (nodesFaunaEu.size()>1 || nodesFaunaEu.isEmpty()){

			}else{
				Iterator<TaxonNode> iteratorNodesErms = nodesErms.iterator();

				Iterator<TaxonNode> iteratorNodesFaunaEu = nodesFaunaEu.iterator();
				TaxonNode node = iteratorNodesFaunaEu.next();
				List<TaxonNode> children = node.getChildNodes();
				Iterator<TaxonNode> childrenIterator = children.iterator();
				TaxonNode childNode;
				if (iteratorNodesErms.hasNext()){
					TaxonNode ermsNode = iteratorNodesErms.next();
					while (childrenIterator.hasNext()){
						childNode = childrenIterator.next();
						ermsNode.addChildNode(childNode, childNode.getReference(), childNode.getMicroReference());
					}
				}

			}
			//the fauna eu taxon should now only contain synonyms not existing in erms
			moveFaunaEuSynonymsToErmsTaxon(taxonFaunaEu, taxonErms);
			moveAllInformationsFromFaunaEuToErms(taxonFaunaEu, taxonErms);
			moveOriginalDbToErmsTaxon(taxonFaunaEu, taxonErms);
			//neue sec Referenz an das ErmsTaxon oder an das Synonym und Taxon oder nur Synonym??
			try{
				deleteFaunaEuTaxon(taxonFaunaEu);
			}catch(ReferencedObjectUndeletableException e){
				logger.debug("The taxon " + taxonFaunaEu.getTitleCache() + " can't be deleted because it is referenced.");
			}
		}

	}

	private void updateNameRelationships(List<List<String>> ermsAccFaEuSyn){
		//search all NameRelationships of FaunaEu and Erms, where (faunaEu)relatedFrom.name.titleCache = (erms)relatedFrom.name.titleCache and replace in faunaEu relationship the relatedTo.name by the relatedTo.name of the erms-relationship
		//if this relationship does not yet exist and the type is the same!!
		//if the relatedTo name belongs to an Erms taxon and to an FaunaEu synonym

		Synonym synFaunaEu;
		Taxon taxonErms;
		for (List<String> row: ermsAccFaEuSyn){
			synFaunaEu = (Synonym)appCtrInit.getTaxonService().find(UUID.fromString(row.get(faunaEuUuid)));
			taxonErms = (Taxon)appCtrInit.getTaxonService().find(UUID.fromString(row.get(ermsUuid)));
			logger.warn("Still check correct direction after change to undeprecated method");
			List<NameRelationship> relSynFaunaEu = appCtrInit.getNameService()
			        .listNameRelationships(synFaunaEu.getName(), NameRelationship.Direction.relatedTo, null, 100, 0, null, null);
			List<NameRelationship> relTaxonErms = appCtrInit.getNameService()
			        .listNameRelationships(taxonErms.getName(), NameRelationship.Direction.relatedTo, null, 100, 0, null, null);

			List<NameRelationship> deleteRel = new ArrayList<>();
			for (NameRelationship relFauEu: relSynFaunaEu){
				boolean createNewRelationship = true;
				for (NameRelationship relErms: relTaxonErms){
					if ((relErms.getFromName().getTitleCache().equals(relFauEu.getFromName().getTitleCache())) && (relErms.getToName().getTitleCache().equals(relFauEu.getFromName().getTitleCache()))){
						//delete the faunaEu relationship because there exist an analogous relationship in erms
						deleteRel.add(relFauEu);
						createNewRelationship = false;
						break;
					}
				}
				if (createNewRelationship){
					//if relationship does not exist, create a new one with erms synonym
					taxonErms.getName().addRelationshipFromName(relFauEu.getFromName(), relFauEu.getType(), relFauEu.getRuleConsidered(), null);
				}
			}
		}
	}

	private void updateSynonymRelationships(List<List<String>> ermsSynFaEuAcc){
//		-- Update queries for RelTaxon (synonym relationships - move relationships to ERMS accepted taxon if not already existent or delete if already existent)
//		UPDATE RelTaxon_1 SET RelTaxon_1.TaxonFk2 = RT.TaxonFk2
//		FROM         Taxon AS ERMSSyn INNER JOIN
//		                      Taxon AS FaEuAcc ON ERMSSyn.RankFk = FaEuAcc.RankFk AND ERMSSyn.FullName = FaEuAcc.FullName AND
//		                      ERMSSyn.TaxonStatusFk <> ISNULL(FaEuAcc.TaxonStatusFk, 0) INNER JOIN
//		                      RelTaxon AS RT ON ERMSSyn.TaxonId = RT.TaxonFk1 INNER JOIN
//		                      RelTaxon AS RelTaxon_1 ON FaEuAcc.TaxonId = RelTaxon_1.TaxonFk2 INNER JOIN
//		                      Taxon AS FaEuSyn ON RelTaxon_1.TaxonFk1 = FaEuSyn.TaxonId LEFT OUTER JOIN
//		                      Taxon AS ERMSAllSyn ON RT.TaxonFk1 = ERMSAllSyn.TaxonId AND FaEuSyn.FullName <> ERMSAllSyn.FullName --(!!)
//		WHERE     (ERMSSyn.OriginalDB = N'ERMS') AND (RT.RelTaxonQualifierFk > 100) AND (ERMSSyn.TaxonStatusFk <> 1) AND (ERMSSyn.KingdomFk = 2) AND
//		                      (FaEuAcc.OriginalDB = N'FaEu') AND (RelTaxon_1.RelTaxonQualifierFk > 100)
		Taxon taxonFaunaEu;
		Synonym synErms;
		Taxon taxonErms;
		Set<Taxon> acceptedTaxa = new HashSet<>();
		for (List<String> row: ermsSynFaEuAcc){
			taxonFaunaEu = (Taxon)appCtrInit.getTaxonService().find(UUID.fromString(row.get(faunaEuUuid)));
			synErms = (Synonym)appCtrInit.getTaxonService().find(UUID.fromString(row.get(ermsUuid)));
			acceptedTaxa.clear();
			acceptedTaxa.add( synErms.getAcceptedTaxon());
			if (!acceptedTaxa.isEmpty()){
				taxonErms = acceptedTaxa.iterator().next();
				if (acceptedTaxa.size() > 1){
					logger.debug("There are more than one accepted taxon for synonym " + synErms.getTitleCache());
				}
			}else{
				taxonErms = null;
				logger.debug("There is no accepted taxon for synonym "  + synErms.getTitleCache());
			}

			if (taxonErms != null){
				Pager<Synonym> synsTaxonFaunaEuPager = appCtrInit.getTaxonService().getSynonyms(taxonFaunaEu, null, null, null, null, null);
				List<Synonym> synsTaxonFaunaEu = synsTaxonFaunaEuPager.getRecords();

				Pager<Synonym> synsTaxonErmsPager = appCtrInit.getTaxonService().getSynonyms(taxonErms, null, null, null, null, null);
				List<Synonym> synsTaxonErms = synsTaxonErmsPager.getRecords();

				List<Synonym> deleteRel = new ArrayList<>();
				for (Synonym synFauEu: synsTaxonFaunaEu){
					//TODO: wenn es noch keine SynonymRelationship gibt zu einem Synonym mit gleichem Namen,
				    //dann erzeuge die SynonymRelationship vom FaunaEuSyn (des FaunaEu Taxons, das
				    //identischen Namen hat) zum akzeptierten Taxon des Erms Syn
					boolean createNewRelationship = true;
					for (Synonym relErms: synsTaxonErms){
						if (relErms.getTitleCache().equals(synFauEu.getTitleCache())){
							//es gibt schon eine Relationship zu einem Synonym mit dem gleichen Namen wie das FaunaEu Synonym, also Relationship l�schen.
							createNewRelationship = false;
							break;
						}
					}
					if (createNewRelationship){
						taxonErms.addSynonym(synFauEu, synFauEu.getType());
					}
					deleteRel.add(synFauEu);
				}
			}
		}
	}

	private void deleteFaunaEuTaxon(Taxon taxonFaunaEu) throws ReferencedObjectUndeletableException {
		appCtrInit.getTaxonService().delete(taxonFaunaEu);
	}

	//wenn Name und Rang identisch sind und auch der Status gleich, dann alle Informationen vom Fauna Europaea Taxon/Synonym zum Erms Taxon/Synonym

	private void moveAllInformationsFromFaunaEuToErms(TaxonBase<?> faunaEu, TaxonBase<?> erms){
		Set<Annotation> annotations = faunaEu.getAnnotations();
		Set<Extension> extensions = faunaEu.getExtensions();
		Set<Marker> markers = faunaEu.getMarkers();
		List<Credit> credits = faunaEu.getCredits();
		if (faunaEu instanceof Taxon){
			Set<TaxonDescription> descriptions = ((Taxon)faunaEu).getDescriptions();
			Set<Taxon> misappliedNames = ((Taxon)faunaEu).getMisappliedNames(true);

			if (erms instanceof Taxon){
				Iterator<TaxonDescription> descriptionsIterator = descriptions.iterator();
				TaxonDescription description;
				while (descriptionsIterator.hasNext()){
					description = descriptionsIterator.next();
					((Taxon) erms).addDescription(description);
				}

				Iterator<Taxon> misappliedNamesIterator = misappliedNames.iterator();
				Taxon misappliedName;
				while (misappliedNamesIterator.hasNext()){
					misappliedName = misappliedNamesIterator.next();
					((Taxon) erms).addMisappliedName(misappliedName, null, null);
				}
			}
		}

		//move all these informations to the erms taxon
		Iterator<Annotation> annotationsIterator = annotations.iterator();
		Annotation annotation;
		while (annotationsIterator.hasNext()){
			annotation = annotationsIterator.next();
			erms.addAnnotation(annotation);
		}

		Iterator<Extension> extensionIterator = extensions.iterator();
		Extension extension;
		while (extensionIterator.hasNext()){
			extension = extensionIterator.next();
			erms.addExtension(extension);
		}

		Iterator<Marker> markerIterator = markers.iterator();
		Marker marker;
		while (markerIterator.hasNext()){
			marker = markerIterator.next();
			erms.addMarker(marker);
		}

		for (Credit credit: credits){
			erms.addCredit(credit);
		}
		//move children to erms taxon
		if (faunaEu instanceof Taxon && ((Taxon)faunaEu).getTaxonNode(faunaEuClassification).hasChildNodes()) {
			Taxon acceptedErmsTaxon;
			if (erms instanceof Synonym) {
				acceptedErmsTaxon = ((Synonym)erms).getAcceptedTaxon();
			}else {
				acceptedErmsTaxon = (Taxon)erms;
			}
			TaxonNode node = acceptedErmsTaxon.getTaxonNode(ermsClassification);
			for (TaxonNode child:((Taxon)faunaEu).getTaxonNode(faunaEuClassification).getChildNodes()) {
				//add pesi reference as reference??
				node.addChildNode(child, null, null);
			}
		}
	}

	//if name, rank, and status (accepted) are the same, then move the synonyms of faunaEu taxon to the erms taxon

	private void moveFaunaEuSynonymsToErmsTaxon(Taxon faunaEu, Taxon erms){
		Set<Synonym> syns =faunaEu.getSynonyms();
		Iterator<Synonym> synRelIterator = syns.iterator();
		while (synRelIterator.hasNext()){
			Synonym syn = synRelIterator.next();
			faunaEu.removeSynonym(syn);
			erms.addSynonym(syn, syn.getType());
		}
	}

	//after merging faunaEu taxon and erms taxon, the originalSource of the faunaEu taxon has to be moved to the erms taxon
	private void moveOriginalDbToErmsTaxon(TaxonBase<?> faunaEuTaxon, TaxonBase<?> ermsTaxon){
		Set<IdentifiableSource> sourcesFaunaEu = faunaEuTaxon.getSources();
		IdentifiableSource sourceFaunaEu = sourcesFaunaEu.iterator().next();
		ermsTaxon.addSource(sourceFaunaEu);
	}

	//merged taxon should have a new sec reference
	private void addNewSecForMergedTaxon(Taxon taxon, Reference sec){
		taxon.setSec(sec);
		//this does not work!!
		//taxon.setUuid(UUID.randomUUID());
	}

	// ----------- methods for merging Erms synonyms and Fauna Europaea Taxon

}
