package eu.etaxonomy.cdm.app.pesi.merging;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.pesi.merging.PesiMergeObject;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class FaunaEuErmsFindIdenticalNamesActivator {

    private static final Logger logger = Logger.getLogger(FaunaEuErmsFindIdenticalNamesActivator.class);

    //static final ICdmDataSource faunaEuropaeaSource = CdmDestinations.localH2();
	static final ICdmDataSource pesiSource = CdmDestinations.cdm_test_local_faunaEu_mysql();

	static final String path = "C:\\Users\\k.luther\\test";

	private static UUID faunaEuSecUuid = UUID.fromString("6786d863-75d4-4796-b916-c1c3dff4cb70");
	private static UUID ermsSecUuid = UUID.fromString("7744bc26-f914-42c4-b54a-dd2a030a8bb7");
	private static UUID ifSecUuid;
	private static UUID emSecUuid;

	private void invoke(ICdmDataSource source){

        CdmApplicationController app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);

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
        System.out.println("Start getIdenticalNames...");

        Reference faunaEuSec = app.getReferenceService().load(faunaEuSecUuid);
        Reference ermsSec = app.getReferenceService().load(ermsSecUuid);
        Map<String, List<TaxonName>> namesOfIdenticalTaxa = app.getTaxonService().findIdenticalTaxonNameIds(ermsSec, faunaEuSec, propertyPaths);

        System.out.println("Start creating merging objects");
        List<PesiMergeObject> mergingObjects= createMergeObjects(namesOfIdenticalTaxa, app);
        boolean resultOK = true;
        System.out.println("Start creating csv files");
        resultOK &= writeSameNamesDifferentAuthorToCsv(mergingObjects, path + "_authors.csv");
        resultOK &= writeSameNamesDifferentStatusToCsv(mergingObjects, path + "_status.csv");
        resultOK &= writeSameNamesToCsvFile(mergingObjects, path + "_names.csv");
        resultOK &= writeSameNamesDifferentPhylumToCsv(mergingObjects, path + "_phylum.csv");
        resultOK &= writeSameNamesDifferentParentToCsv(mergingObjects, path + "parent.csv");

        System.out.println("End merging Fauna Europaea and Erms: " + resultOK);
	}

	private boolean writeSameNamesToCsvFile(
			List<PesiMergeObject> mergingObjects, String string) {

<<<<<<< HEAD
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
			
		List<FaunaEuErmsMerging> mergingObjects = new ArrayList<>();
		FaunaEuErmsMerging mergeObject;
		TaxonName faunaEuTaxName;
		TaxonName ermsTaxName;
		System.err.println("Start creating merging objects");
		mergingObjects= sc.createMergeObjects(namesOfIdenticalTaxa, appCtrFaunaEu);
		boolean resultOK = true;
		System.err.println("Start creating csv files");
		resultOK = resultOK && sc.writeSameNamesdifferentAuthorToCsv(mergingObjects, sFileName + "_authors.csv");
		resultOK = resultOK &&sc.writeSameNamesdifferentStatusToCsv(mergingObjects, sFileName + "_status.csv");
		resultOK = resultOK &&sc.writeSameNamesToCsVFile(mergingObjects, sFileName + "_names.csv");
		//do not create the phylum file, explanation inside the method writeSameNamesdifferentPhylumToCsv
		//resultOK = resultOK &&sc.writeSameNamesdifferentPhylumToCsv(mergingObjects, sFileName + "_phylum.csv");
		resultOK = resultOK &&sc.writeSameNamesDifferentParentToCsv(mergingObjects, sFileName + "parent.csv");

		System.err.println("End merging Fauna Europaea and Erms" + resultOK);
		System.exit(0);

	}

	private boolean writeSameNamesToCsVFile(
			List<FaunaEuErmsMerging> mergingObjects, String string) {
=======
>>>>>>> 34743ac779cc3f8570bd9eae6109207adda534ea
	    try{
    		FileWriter writer = new FileWriter(string);

    	    //create Header
    	    String firstLine = "same names";
    	    createHeader(writer, firstLine);
    		for (PesiMergeObject merging : mergingObjects){
    	    	writeCsvLine(writer, merging) ;
    		}
    		writer.flush();
    		writer.close();
    		return true;
    	}catch(IOException e){
    	    logger.error(e.getMessage());
    	    return false;
    	}
	}

<<<<<<< HEAD
	private boolean writeSameNamesdifferentPhylumToCsv(List<FaunaEuErmsMerging> mergingObjects, String sfileName){
		try
		{
			//do we really need this?? it is a taxon needed merged like all others? for erms only one taxon has different phylum. (Valencia, but these are not the same taxa -> fish and ribbon worms)
=======
	private boolean writeSameNamesDifferentPhylumToCsv(
	        List<PesiMergeObject> mergingObjects, String sfileName){

	    try{
>>>>>>> 34743ac779cc3f8570bd9eae6109207adda534ea
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		   String firstLine = "same names but different phylum";
		   createHeader(writer, firstLine);

			//write data
<<<<<<< HEAD
			for (FaunaEuErmsMerging merging : mergingObjects){
		    	//TODO the phyllum is always different doing it this way, maybe we need to merge the phylum taxa first and then 
=======
			for (PesiMergeObject merging : mergingObjects){
		    	//TODO
>>>>>>> 34743ac779cc3f8570bd9eae6109207adda534ea
				if ((merging.getPhylumInErms()== null )^ (merging.getPhylumInFaunaEu()== null)){
					writeCsvLine(writer, merging) ;
				}else if(!((merging.getPhylumInErms()==null) && (merging.getPhylumInFaunaEu()==null))){
					if(!merging.getPhylumInErms().getNameTitleCache().equals(merging.getPhylumInFaunaEu().getNameTitleCache())){
						writeCsvLine(writer, merging) ;
					}
				}
			}
			writer.flush();
			writer.close();
			return true;
		}catch(IOException e){
		    logger.error(e.getMessage());
            return false;
		}
	}

	private boolean writeSameNamesDifferentParentToCsv(
	        List<PesiMergeObject> mergingObjects, String sfileName){

	    try{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		   String firstLine = "same names but different parent";
		   createHeader(writer, firstLine);

			//write data
			for (PesiMergeObject merging : mergingObjects){
		    	//TODO
				if ((merging.getParentStringInErms()== null )^ (merging.getParentStringInFaunaEu()== null)){
					writeCsvLine(writer, merging) ;
				}else if(!((merging.getParentStringInErms()==null) && (merging.getParentStringInFaunaEu()==null))){
					if(!merging.getParentStringInErms().equals(merging.getParentStringInFaunaEu())){
						writeCsvLine(writer, merging) ;
					}
				}
			}
			writer.flush();
			writer.close();
			return true;
		}catch(IOException e){
		    return false;
		}
	}

	private boolean writeSameNamesdifferentRankToCsv(
	        List<PesiMergeObject> mergingObjects, String sfileName){

	    try{
		    FileWriter writer = new FileWriter(sfileName);
		    String firstLine = "same names but different rank";
		    //create Header
		    createHeader(writer, firstLine);

			//write data
			for (PesiMergeObject merging : mergingObjects){

				if (!merging.getRankInErms().equals(merging.getRankInFaunaEu())){
					writeCsvLine(writer, merging);
				}
			}
			writer.flush();
			writer.close();
			return true;
		}catch(IOException e){
		    return false;
		}
	}

	private void createHeader(FileWriter writer, String firstLine) throws IOException{
		 	writer.append(firstLine);
		    writer.append('\n');
		    writer.append("uuid in Fauna Europaea");
			writer.append(';');
			writer.append("id in Fauna Europaea");
			writer.append(';');
			writer.append("name in FE");
			writer.append(';');
			writer.append("author in FE");
			writer.append(';');
			writer.append("rank in FE");
			writer.append(';');
			writer.append("state in FE");
			writer.append(';');
			writer.append("phylum in FE");
			writer.append(';');
			writer.append("parent in FE");
			writer.append(';');
			writer.append("parent rank in FE");
			writer.append(';');

			writer.append("uuid in Erms");
			writer.append(';');
			writer.append("id in Erms");
			writer.append(';');
			writer.append("name in Erms");
			writer.append(';');
			writer.append("author in Erms");
			writer.append(';');
			writer.append("rank in Erms");
			writer.append(';');
			writer.append("state in Erms");
			writer.append(';');
			writer.append("phylum in Erms");
			writer.append(';');
			writer.append("parent in Erms");
			writer.append(';');
			writer.append("parent rank in Erms");
			writer.append('\n');
	}

	private boolean writeSameNamesDifferentStatusToCsv(
	        List<PesiMergeObject> mergingObjects, String sfileName){

	    try{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		    String firstLine = "same names but different status";
		    createHeader(writer, firstLine);

			//write data
			for (PesiMergeObject merging : mergingObjects){
				if (merging.isStatInErms()^merging.isStatInFaunaEu()){
					 writeCsvLine(writer, merging);
				}
			}

			writer.flush();
			writer.close();
			return true;
		}catch(IOException e){
		    return false;
		}
	}

	private boolean writeSameNamesDifferentAuthorToCsv(
	        List<PesiMergeObject> mergingObjects, String sfileName){

	    try{
		    FileWriter writer = new FileWriter(sfileName);

		    //create Header
		    String firstLine = "same names but different authors";
		    createHeader(writer, firstLine);

			//write data
			for (PesiMergeObject merging : mergingObjects){

				if (merging.getAuthorInErms() != null && merging.getAuthorInFaunaEu() != null && !merging.getAuthorInErms().equals(merging.getAuthorInFaunaEu())){
					 writeCsvLine(writer, merging);
				}else if ((merging.getAuthorInErms() == null && merging.getAuthorInFaunaEu() != null) || (merging.getAuthorInErms() != null && merging.getAuthorInFaunaEu() == null)) {
					writeCsvLine(writer, merging);
				}
			}

			writer.flush();
			writer.close();
			return true;
		}catch(IOException e){
		    return false;
		}
	}

	private void writeCsvLine(FileWriter writer, PesiMergeObject merging) throws IOException{

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
		writer.append(merging.getPhylumInFaunaEu() != null? merging.getPhylumInFaunaEu().getTaxonTitleCache(): "");
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
		writer.append(merging.getPhylumInErms() != null? merging.getPhylumInErms().getTitleCache():"");
		writer.append(';');
		writer.append(merging.getParentStringInErms());
		writer.append(';');
		writer.append(merging.getParentRankStringInErms());
		writer.append('\n');
	}

	private List<PesiMergeObject> createMergeObjects(Map<String,List<TaxonName>> names,
	        CdmApplicationController appCtr){

	    UUID uuidClassification1 = UUID.fromString("44d8605e-a7ce-41e1-bee9-99edfec01e7c");
	    UUID uuidClassification2 = UUID.fromString("6fa988a9-10b7-48b0-a370-2586fbc066eb");
	    Classification classification1 = appCtr.getClassificationService().load(uuidClassification1);
		Classification classification2 = appCtr.getClassificationService().load(uuidClassification2);

<<<<<<< HEAD
				}
			}else{
				mergeObject.setStatInFaunaEu(false);
				TaxonNode parentNode = getAcceptedNode(faunaEuName);
				//TODO: ändern mit erweitertem Initializer..
				if (parentNode != null){
				    TaxonName parentName = HibernateProxyHelper.deproxy(parentNode.getTaxon().getName());
					String parentNameCache = parentName.getNameCache();
					mergeObject.setParentStringInFaunaEu(parentNameCache);
					mergeObject.setParentRankStringInFaunaEu(parentName.getRank().getLabel());
					
				}
			}
			

			mergeObject.setRankInErms(ermsName.getRank().getLabel());
			mergeObject.setRankInFaunaEu(faunaEuName.getRank().getLabel());

		
			merge.add(mergeObject);
		}

=======
		List<PesiMergeObject> merge = new ArrayList<>();

		for (String nameCache: names.keySet()){
		    createSingleMergeObject(appCtr, merge, nameCache, names.get(nameCache),classification1, classification2);
		}
>>>>>>> 34743ac779cc3f8570bd9eae6109207adda534ea

		return merge;
	}


    private void createSingleMergeObject(CdmApplicationController appCtr, List<PesiMergeObject> merge, String nameCache,
            List<TaxonName> identicalNames,
            Classification classification1, Classification classification2) {

        PesiMergeObject mergeObject = new PesiMergeObject();

        if(identicalNames.size()!= 2) {
            logger.warn(nameCache + " has more than 2 names with identical name cache from different sources. This is not yet handled.");
            return;
        }
        //getPhylum
        TaxonNodeDto phylum1 = null;
        TaxonName faunaEuName = null;
        TaxonName ermsName = null;

        Set<TaxonBase> taxonBases = identicalNames.get(0).getTaxonBases();
        if (taxonBases.size()==1) {
            Iterator<TaxonBase> it = taxonBases.iterator();
            TaxonBase<?> tempName = it.next();
            if (tempName.getSec().getUuid().equals(faunaEuSecUuid)) {
                faunaEuName = identicalNames.get(0);
                ermsName = identicalNames.get(1);
            }else {
                faunaEuName = identicalNames.get(1);
                ermsName = identicalNames.get(0);
            }
        }else {
            //TODO: find the two correct names
            logger.warn("Name has not exact 1 but " + taxonBases.size() + " taxon base attached. This is not yet handled");
            return;
        }
        if (faunaEuName.getRank().equals(Rank.PHYLUM())) {
            Taxon taxon = null;
            taxon = getAcceptedTaxon(faunaEuName);
            if (taxon != null) {
                phylum1 = new TaxonNodeDto(taxon.getTaxonNode(classification1));
            }

        }
        if (phylum1 == null && !faunaEuName.getRank().isHigher(Rank.PHYLUM())){
                phylum1 = appCtr.getTaxonNodeService().taxonNodeDtoParentRank(classification1, Rank.PHYLUM(), faunaEuName);
        }

        TaxonNodeDto phylum2 = null;
        if (ermsName.getRank().equals(Rank.PHYLUM())) {
            Taxon taxon = null;
            taxon = getAcceptedTaxon(ermsName);
            if (taxon != null) {
                phylum2 = new TaxonNodeDto(taxon.getTaxonNode(classification2));
            }

        }
        if (phylum2 == null && !ermsName.getRank().isHigher(Rank.PHYLUM())){
            phylum2 = appCtr.getTaxonNodeService().taxonNodeDtoParentRank(classification2, Rank.PHYLUM(), ermsName);
        }
        mergeObject.setPhylumInErms(phylum1);
        mergeObject.setPhylumInFaunaEu(phylum2);

        //getUuids
        mergeObject.setUuidErms(ermsName.getUuid().toString());
        mergeObject.setUuidFaunaEu(faunaEuName.getUuid().toString());

        Iterator<IdentifiableSource> sources = ermsName.getSources().iterator();
        if (sources.hasNext()){
            IdentifiableSource source = sources.next();
            String idInSource1 = source.getIdInSource();
            mergeObject.setIdInErms(idInSource1);
        }
        sources = faunaEuName.getSources().iterator();
        if (sources.hasNext()){
            IdentifiableSource source = sources.next();
            String idInSource1 = source.getIdInSource();
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
                TaxonName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
                String parentNameCache = parentName.getNameCache();
                mergeObject.setParentStringInErms(parentNameCache);
                mergeObject.setParentRankStringInErms(parentName.getRank().getLabel());
            }
        }else{
            mergeObject.setStatInErms(false);
            TaxonNode parentNode = getAcceptedNode(ermsName);
            //TODO: ändern mit erweitertem Initializer..
            if (parentNode != null){
                TaxonName parentName = HibernateProxyHelper.deproxy(parentNode.getTaxon().getName());
                String parentNameCache = parentName.getNameCache();
                mergeObject.setParentStringInErms(parentNameCache);
                mergeObject.setParentRankStringInErms(parentName.getRank().getLabel());
            }
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

                }else{
                    logger.debug("no zoologicalName: " + parentNode.getTaxon().getName().getTitleCache() +" . "+parentNode.getTaxon().getName().getUuid());
                }
            }
        }else{
            mergeObject.setStatInFaunaEu(false);
            TaxonNode parentNode = getAcceptedNode(faunaEuName);
            //TODO: ändern mit erweitertem Initializer..
            if (parentNode != null){
                TaxonName parentName = HibernateProxyHelper.deproxy(parentNode.getTaxon().getName());
                String parentNameCache = parentName.getNameCache();
                mergeObject.setParentStringInFaunaEu(parentNameCache);
                mergeObject.setParentRankStringInFaunaEu(parentName.getRank().getLabel());

            }
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

	private TaxonNode getAcceptedNode(TaxonName ermsName) {
		Set<TaxonBase> taxonBases = ermsName.getTaxonBases();
		Taxon taxon = null;
		if (taxonBases != null && !taxonBases.isEmpty()) {
			TaxonBase<?> taxonBase = taxonBases.iterator().next();
			if (taxonBase instanceof Synonym) {
				taxon = ((Synonym)taxonBase).getAcceptedTaxon();
			}
		}

		Set<TaxonNode> nodes = taxon.getTaxonNodes();

		TaxonNode parentNode = null;
		if (nodes != null && !nodes.isEmpty()) {
			parentNode = nodes.iterator().next();
		}
		return parentNode;
	}

	private Taxon getAcceptedTaxon(TaxonName ermsName) {
		Taxon taxon = null;
		if (ermsName.getTaxa() != null && !ermsName.getTaxa().isEmpty()){
			taxon = ermsName.getTaxa().iterator().next();

		}else if (ermsName.getTaxonBases() != null && !ermsName.getTaxonBases().isEmpty()){
			TaxonBase<?> taxonBase = ermsName.getTaxonBases().iterator().next();
			if (taxonBase instanceof Synonym) {
				Synonym syn = (Synonym)taxonBase;
				taxon = syn.getAcceptedTaxon();
			}
		}
		return taxon;
	}

    public static void main(String[] args) {
        FaunaEuErmsFindIdenticalNamesActivator activator = new FaunaEuErmsFindIdenticalNamesActivator();
        activator.invoke(pesiSource);
        System.exit(0);
    }
}
