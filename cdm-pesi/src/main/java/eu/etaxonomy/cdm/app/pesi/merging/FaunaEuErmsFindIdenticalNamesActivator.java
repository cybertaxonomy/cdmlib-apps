package eu.etaxonomy.cdm.app.pesi.merging;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sun.media.jfxmedia.logging.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.app.util.TestDatabase;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.pesi.merging.FaunaEuErmsMerging;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class FaunaEuErmsFindIdenticalNamesActivator {

	//static final ICdmDataSource faunaEuropaeaSource = CdmDestinations.localH2();
	static final ICdmDataSource faunaEuropaeaSource = CdmDestinations.cdm_test_local_faunaEu_mysql();
	static Reference faunaSec;
	static Reference ermsSec;

	//TODO hole aus beiden DB alle TaxonNameBases

	private CdmApplicationController initDb(ICdmDataSource db) {

		// Init source DB
		//CdmApplicationController appCtrInit = null;
		CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, DbSchemaValidation.VALIDATE, false);

		
		//appCtrInit = TestDatabase.initDb(db, DbSchemaValidation.VALIDATE, false);

		return appCtrInit;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		FaunaEuErmsFindIdenticalNamesActivator sc = new FaunaEuErmsFindIdenticalNamesActivator();

		CdmApplicationController appCtrFaunaEu = sc.initDb(faunaEuropaeaSource);
		String sFileName = "C:\\Users\\k.luther\\test";
		//CdmApplicationController appCtrErms = sc.initDb(ermsSource);
		List<String> propertyPaths = new ArrayList<>();
		propertyPaths.add("sources.*");
		propertyPaths.add("sources.idInSource");
		propertyPaths.add("sources.idNamespace");
		propertyPaths.add("taxonBases.*");
		propertyPaths.add("taxonBases.relationsFromThisTaxon");
		propertyPaths.add("taxonBases.taxonNodes.*");
		propertyPaths.add("taxonBases.taxonNodes.parent.*");
		propertyPaths.add("taxonBases.taxonNodes.childNodes.*");
		propertyPaths.add("taxonBases.taxonNodes.childNodes.classification.rootNode.childNodes.*");
		propertyPaths.add("taxonBases.taxonNodes.parent.taxon.name.*");
		propertyPaths.add("taxonBases.acceptedTaxon.taxonNodes.*");
		propertyPaths.add("taxonBases.acceptedTaxon.taxonNodes.childNodes.*");
		propertyPaths.add("taxonBases.acceptedTaxon.taxonNodes.childNodes.classification.rootNode.childNodes.*");
		System.err.println("Start getIdenticalNames...");
		
		faunaSec = appCtrFaunaEu.getReferenceService().load(UUID.fromString("6786d863-75d4-4796-b916-c1c3dff4cb70"));
		ermsSec = appCtrFaunaEu.getReferenceService().load(UUID.fromString("7744bc26-f914-42c4-b54a-dd2a030a8bb7"));
		Map<String, List<TaxonName>> namesOfIdenticalTaxa = appCtrFaunaEu.getTaxonService().findIdenticalTaxonNameIds(ermsSec, faunaSec, propertyPaths);
		//List<UUID> namesOfIdenticalTaxa = appCtrFaunaEu.getTaxonService().findIdenticalTaxonNameIds(propertyPaths);

		System.err.println("first name: " + namesOfIdenticalTaxa.get(0) + " " + namesOfIdenticalTaxa.size());
		//TaxonName zooName = namesOfIdenticalTaxa.get(0);
		//System.err.println(zooName + " nr of taxa " + namesOfIdenticalTaxa.size());
		//TaxonNameComparator taxComp = new TaxonNameComparator();

		//Collections.sort(namesOfIdenticalTaxa,taxComp);
		System.err.println(namesOfIdenticalTaxa.get(0) + " - " + namesOfIdenticalTaxa.get(1) + " - " + namesOfIdenticalTaxa.get(2));
		List<FaunaEuErmsMerging> mergingObjects = new ArrayList<>();
		FaunaEuErmsMerging mergeObject;
		TaxonName faunaEuTaxName;
		TaxonName ermsTaxName;

		mergingObjects= sc.createMergeObjects(namesOfIdenticalTaxa, appCtrFaunaEu);

		sc.writeSameNamesdifferentAuthorToCsv(mergingObjects, sFileName + "_authors.csv");
		sc.writeSameNamesdifferentStatusToCsv(mergingObjects, sFileName + "_status.csv");
		sc.writeSameNamesToCsVFile(mergingObjects, sFileName + "_names.csv");
		sc.writeSameNamesdifferentPhylumToCsv(mergingObjects, sFileName + "_phylum.csv");


		System.out.println("End merging Fauna Europaea and Erms");

	}

	private boolean writeSameNamesToCsVFile(
			List<FaunaEuErmsMerging> mergingObjects, String string) {
	    try{
    		FileWriter writer = new FileWriter(string);

    	    //create Header
    	    String firstLine = "same names";
    	    createHeader(writer, firstLine);
    		for (FaunaEuErmsMerging merging : mergingObjects){
    	    	writeCsvLine(writer, merging) ;
    		}
    		writer.flush();
    		writer.close();
    	}catch(IOException e){
    	    return false;
    	}
    	return true;
	}

	private boolean writeSameNamesdifferentPhylumToCsv(List<FaunaEuErmsMerging> mergingObjects, String sfileName){
		try
		{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		   String firstLine = "same names but different phylum";
		   createHeader(writer, firstLine);

			//write data
			for (FaunaEuErmsMerging merging : mergingObjects){
		    	//TODO
				if ((merging.getPhylumInErms()== null )^ (merging.getPhylumInFaunaEu()== null)){
					writeCsvLine(writer, merging) ;
				}else if(!((merging.getPhylumInErms()==null) && (merging.getPhylumInFaunaEu()==null))){
					if(!merging.getPhylumInErms().equals(merging.getPhylumInFaunaEu())){
						writeCsvLine(writer, merging) ;
					}
				}
			}
			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
		 return false;
		}
		return true;
	}

	private boolean writeSameNamesdifferentRankToCsv(List<FaunaEuErmsMerging> mergingObjects, String sfileName){
		try
		{
		    FileWriter writer = new FileWriter(sfileName);
		    String firstLine = "same names but different rank";
		    //create Header
		    createHeader(writer, firstLine);

			//write data
			for (FaunaEuErmsMerging merging : mergingObjects){

				if (!merging.getRankInErms().equals(merging.getRankInFaunaEu())){
					writeCsvLine(writer, merging);
				}
			}
			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
		 return false;
		}
		return true;
	}

	private void createHeader(FileWriter writer, String firstLine) throws IOException{
		 	writer.append(firstLine);
		    writer.append('\n');
		    writer.append("uuid in Fauna Europaea");
			writer.append(';');
			writer.append("id in Fauna Europaea");
			writer.append(';');
			writer.append("name");
			writer.append(';');
			writer.append("author");
			writer.append(';');
			writer.append("rank");
			writer.append(';');
			writer.append("state");
			writer.append(';');
			writer.append("phylum");
			writer.append(';');
			writer.append("parent");
			writer.append(';');
			writer.append("parent rank");
			writer.append(';');

			writer.append("uuid in Erms");
			writer.append(';');
			writer.append("id in Erms");
			writer.append(';');
			writer.append("name");
			writer.append(';');
			writer.append("author");
			writer.append(';');
			writer.append("rank");
			writer.append(';');
			writer.append("state");
			writer.append(';');
			writer.append("phylum");
			writer.append(';');
			writer.append("parent");
			writer.append(';');
			writer.append("parent rank");
			writer.append('\n');
	}

	private boolean writeSameNamesdifferentStatusToCsv(List<FaunaEuErmsMerging> mergingObjects, String sfileName){
		try
		{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		    String firstLine = "same names but different status";
		    createHeader(writer, firstLine);

			//write data
			for (FaunaEuErmsMerging merging : mergingObjects){

				if (merging.isStatInErms()^merging.isStatInFaunaEu()){
					 writeCsvLine(writer, merging);
				}
			}

			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
		 return false;
		}
		return true;
	}

	private boolean writeSameNamesdifferentAuthorToCsv(List<FaunaEuErmsMerging> mergingObjects, String sfileName){
		try
		{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		   String firstLine = "same names but different authors";
		   createHeader(writer, firstLine);

			//write data
			for (FaunaEuErmsMerging merging : mergingObjects){

				if (!merging.getAuthorInErms().equals(merging.getAuthorInFaunaEu())){
					 writeCsvLine(writer, merging);
				}
			}


			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
		 return false;
		}
		return true;
	}

	private void writeCsvLine(FileWriter writer, FaunaEuErmsMerging merging) throws IOException{

		writer.append(merging.getUuidFaunaEu());
		writer.append(';');
		writer.append(merging.getIdInFaunaEu());
		writer.append(';');
		writer.append(merging.getNameCacheInFaunaEu());
		writer.append(';');
		writer.append(merging.getAuthorInFaunaEu());
		writer.append(';');
		writer.append(merging.getRankInFaunaEu());
		writer.append(';');
		if (merging.isStatInFaunaEu()){
			writer.append("accepted");
		}else{
			writer.append("synonym");
		}
		writer.append(';');
		writer.append(merging.getPhylumInFaunaEu().getTaxonTitleCache());
		writer.append(';');
		writer.append(merging.getParentStringInFaunaEu());
		writer.append(';');
		writer.append(merging.getParentRankStringInFaunaEu());
		writer.append(';');

		writer.append(merging.getUuidErms());
		writer.append(';');
		writer.append(merging.getIdInErms());
		writer.append(';');
		writer.append(merging.getNameCacheInErms());
		writer.append(';');
		writer.append(merging.getAuthorInErms());
		writer.append(';');
		writer.append(merging.getRankInErms());
		writer.append(';');
		if (merging.isStatInErms()){
			writer.append("accepted");
		}else{
			writer.append("synonym");
		}

		writer.append(';');
		writer.append(merging.getPhylumInErms().getTaxonTitleCache());
		writer.append(';');
		writer.append(merging.getParentStringInErms());
		writer.append(';');
		writer.append(merging.getParentRankStringInErms());
		writer.append('\n');
	}


	private List<FaunaEuErmsMerging> createMergeObjects(Map<String,List<TaxonName>> names, CdmApplicationController appCtr){

		List<FaunaEuErmsMerging> merge = new ArrayList<>();
		TaxonName zooName, zooName2;
		FaunaEuErmsMerging mergeObject;
		String idInSource1;
		List<TaxonName> identicalNames;
		for (String nameCache: names.keySet()){
			identicalNames = names.get(nameCache);
			
			mergeObject = new FaunaEuErmsMerging();
			//TODO:überprüfen, ob die beiden Namen identisch sind und aus unterschiedlichen DB kommen
			Classification faunaEuClassification = appCtr.getClassificationService().load(UUID.fromString("44d8605e-a7ce-41e1-bee9-99edfec01e7c"));
			Classification ermsClassification = appCtr.getClassificationService().load(UUID.fromString("6fa988a9-10b7-48b0-a370-2586fbc066eb"));
			//getPhylum
			TaxonNodeDto phylum1 = null;
			TaxonName faunaEuName = null;
			TaxonName ermsName = null;
			TaxonBase tempName = null;
			if (identicalNames.size() == 2) {
				Set<TaxonBase> taxonBases = identicalNames.get(0).getTaxonBases();
				if (taxonBases.size()==1) {
					Iterator<TaxonBase> it = taxonBases.iterator();
					tempName = it.next();
					if (tempName.getSec().equals(faunaSec)) {
						faunaEuName = identicalNames.get(0);
						ermsName = identicalNames.get(1);
					}else {
						faunaEuName = identicalNames.get(1);
						ermsName = identicalNames.get(0);
					}
				}else {
					//TODO: find the two correct names
				}
			}else {
				System.err.println(nameCache + " has more than two identical namecaches");
				return null;
			}
			phylum1 = null;
			if (faunaEuName != null && !faunaEuName.getRank().isHigher(Rank.PHYLUM())){
					phylum1 =appCtr.getTaxonNodeService().taxonNodeDtoParentRank(faunaEuClassification, Rank.PHYLUM(), faunaEuName);
			}

			TaxonNodeDto phylum2 = null;
			if (ermsName != null && !ermsName.getRank().isHigher(Rank.PHYLUM())){
				phylum2 = appCtr.getTaxonNodeService().taxonNodeDtoParentRank(ermsClassification, Rank.PHYLUM(), ermsName);
			}
			mergeObject.setPhylumInErms(phylum1);
			mergeObject.setPhylumInFaunaEu(phylum2);

			//getUuids
			mergeObject.setUuidErms(ermsName.getUuid().toString());
			mergeObject.setUuidFaunaEu(faunaEuName.getUuid().toString());

			Iterator<IdentifiableSource> sources = ermsName.getSources().iterator();
			if (sources.hasNext()){
				IdentifiableSource source = sources.next();
				idInSource1 = source.getIdInSource();
				mergeObject.setIdInErms(idInSource1);
			}
			sources = faunaEuName.getSources().iterator();
			if (sources.hasNext()){
				IdentifiableSource source = sources.next();
				idInSource1 = source.getIdInSource();
				mergeObject.setIdInFaunaEu(idInSource1);
			}

			mergeObject.setNameCacheInErms(ermsName.getNameCache());
			mergeObject.setNameCacheInFaunaEu(faunaEuName.getNameCache());

			mergeObject.setAuthorInErms(ermsName.getAuthorshipCache());
			mergeObject.setAuthorInFaunaEu(faunaEuName.getAuthorshipCache());
			Set<Taxon> taxa = ermsName.getTaxa();
			if (!taxa.isEmpty()){
				mergeObject.setStatInErms(true);
				Iterator<Taxon> taxaIterator = taxa.iterator();
				Taxon taxon = null;
				while (taxaIterator.hasNext()){
					taxon = taxaIterator.next();
					if (!taxon.isMisapplication()){
						break;
					}
				}
				Set<TaxonNode> nodes = taxon.getTaxonNodes();
				Iterator<TaxonNode> taxonNodeIterator = nodes.iterator();
				TaxonNode node, parentNode = null;
				while (taxonNodeIterator.hasNext()){
					node = taxonNodeIterator.next();
					if (!node.isTopmostNode()){
						parentNode = node.getParent();
					}
				}
				//TODO: ändern mit erweitertem Initializer..
				if (parentNode != null){
				    TaxonName parentName = HibernateProxyHelper.deproxy(parentNode.getTaxon().getName());
					String parentNameCache = parentName.getNameCache();
					mergeObject.setParentStringInErms(parentNameCache);
					mergeObject.setParentRankStringInErms(parentName.getRank().getLabel());
					//System.err.println("parentName: " + parentNameCache);
				}
			}else{
				mergeObject.setStatInErms(false);
			}
			taxa = faunaEuName.getTaxa();
			if (!taxa.isEmpty()){
				mergeObject.setStatInFaunaEu(true);
				Iterator<Taxon> taxaIterator = taxa.iterator();
				Taxon taxon = null;
				while (taxaIterator.hasNext()){
					taxon = taxaIterator.next();
					if (!taxon.isMisapplication()){
						break;
					}
				}
				Set<TaxonNode> nodes = taxon.getTaxonNodes();
				Iterator<TaxonNode> taxonNodeIterator = nodes.iterator();
				TaxonNode node, parentNode = null;
				while (taxonNodeIterator.hasNext()){
					node = taxonNodeIterator.next();
					if (!node.isTopmostNode()){
						parentNode = node.getParent();
					}
				}
				//TODO: ändern mit erweitertem Initializer..
				if (parentNode != null){
					if (parentNode.getTaxon().getName().isZoological()){

    					IZoologicalName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
    					String parentNameCache = parentName.getNameCache();
    					mergeObject.setParentStringInFaunaEu(parentNameCache);
    					mergeObject.setParentRankStringInFaunaEu(parentName.getRank().getLabel());
    					System.err.println("parentName: " + parentNameCache);
					}else{
						System.err.println("no zoologicalName: " + parentNode.getTaxon().getName().getTitleCache() +" . "+parentNode.getTaxon().getName().getUuid());
					}

				}
			}else{
				mergeObject.setStatInErms(false);
			}
			taxa = faunaEuName.getTaxa();
			if (!taxa.isEmpty()){
				mergeObject.setStatInFaunaEu(true);
			}else{
				mergeObject.setStatInFaunaEu(false);

			}

			mergeObject.setRankInErms(ermsName.getRank().getLabel());
			mergeObject.setRankInFaunaEu(faunaEuName.getRank().getLabel());

			//set parent informations


			/*
			Set<HybridRelationship> parentRelations = zooName.getParentRelationships();
			Iterator parentIterator = parentRelations.iterator();
			HybridRelationship parentRel;
			ZoologicalName parentName;
			while (parentIterator.hasNext()){
				parentRel = (HybridRelationship)parentIterator.next();
				parentName = (ZoologicalName)parentRel.getParentName();
				mergeObject.setParentRankStringInErms(parentName.getRank().getLabel());
				mergeObject.setParentStringInErms(parentName.getNameCache());
			}

			parentRelations = zooName2.getParentRelationships();
			parentIterator = parentRelations.iterator();

			while (parentIterator.hasNext()){
				parentRel = (HybridRelationship)parentIterator.next();
				parentName = (ZoologicalName)parentRel.getParentName();
				mergeObject.setParentRankStringInFaunaEu(parentName.getRank().getLabel());
				mergeObject.setParentStringInFaunaEu(parentName.getNameCache());
			}*/
			merge.add(mergeObject);
		}
//		}

		return merge;

	}
}
