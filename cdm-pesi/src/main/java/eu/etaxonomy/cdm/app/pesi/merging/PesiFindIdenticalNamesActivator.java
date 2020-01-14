package eu.etaxonomy.cdm.app.pesi.merging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.StringComparator;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.pesi.merging.PesiMergeObject;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.dto.TaxonNodeDto;

public class PesiFindIdenticalNamesActivator {

    private static final Logger logger = Logger.getLogger(PesiFindIdenticalNamesActivator.class);

    //static final ICdmDataSource faunaEuropaeaSource = CdmDestinations.localH2();
//	static final ICdmDataSource pesiSource = CdmDestinations.cdm_test_local_faunaEu_mysql();
	static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final();

	static final String path = System.getProperty("user.home")+File.separator+".cdmLibrary"+File.separator+"pesi"+File.separator+"pesimerge";

	private static UUID faunaEuSourceUuid = PesiTransformer.uuidSourceRefFaunaEuropaea;
	private static UUID ermsSourceUuid = PesiTransformer.uuidSourceRefErms;
	private static UUID ifSourceUuid = PesiTransformer.uuidSourceRefIndexFungorum;
	private static UUID emSourceUuid = PesiTransformer.uuidSourceRefEuroMed;
	private static List<UUID> sourceRefUuids = new ArrayList<>();
	private static Map<UUID,String> sources = new HashMap<>();

    static {
        sourceRefUuids.addAll(Arrays.asList(new UUID[]{emSourceUuid, ermsSourceUuid, faunaEuSourceUuid, ifSourceUuid}));
        sources.put(emSourceUuid, "E+M");
        sources.put(ermsSourceUuid, "ERMS");
        sources.put(faunaEuSourceUuid, "FauEu");
        sources.put(ifSourceUuid, "IF");
    }


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

        Map<String, Map<UUID, Set<TaxonName>>> namesOfIdenticalTaxa;
        TransactionStatus tx = app.startTransaction(true);
        try {
            namesOfIdenticalTaxa = app.getTaxonService().findIdenticalTaxonNames(sourceRefUuids, propertyPaths);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Start creating merging objects");
        List<Map<UUID, PesiMergeObject>> mergingObjects = createMergeObjects(namesOfIdenticalTaxa, app);
        app.commitTransaction(tx);

        boolean resultOK = true;
        System.out.println("Start creating csv files");
        resultOK &= writeSameNamesDifferentAuthorToCsv(mergingObjects, sources, path + "_authors.csv");
        resultOK &= writeSameNamesDifferentStatusToCsv(mergingObjects, sources, path + "_status.csv");
        resultOK &= writeSameNamesToCsvFile(mergingObjects, sources, path + "_names.csv");
        resultOK &= writeSameNamesDifferentPhylumToCsv(mergingObjects, sources, path + "_phylum.csv");
        resultOK &= writeSameNamesDifferentParentToCsv(mergingObjects, sources, path + "_parent.csv");
        resultOK &= writeSameNamesDifferentRankToCsv(mergingObjects, sources, path + "_rank.csv");

        System.out.println("End find identical names for PESI: " + resultOK + ". Results written to " + path);
	}

	private boolean writeSameNamesToCsvFile(
			List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName) {

	    String header = "same names (all)";
        String methodName = null;
        return writeDifference(header, methodName, mergingObjects, sources, sFileName);
	}

	private boolean writeSameNamesDifferentPhylumToCsv(
	        List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName){

	    String header = "same names but different phylum";
	    String methodName = "getPhylum";
	    return writeDifference(header, methodName, mergingObjects, sources, sFileName);
	}

    private boolean writeSameNamesDifferentParentToCsv(
	        List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName){

		    String header = "same names but different parent";
	        String methodName = "getParentString";
	        return writeDifference(header, methodName, mergingObjects, sources, sFileName);
	}

	private boolean writeSameNamesDifferentRankToCsv(
	        List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName){

        String header = "same names but different rank";
        String methodName = "getRank";
        return writeDifference(header, methodName, mergingObjects, sources, sFileName);
	}

    private boolean writeSameNamesDifferentStatusToCsv(
            List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName){

        String header = "same names but different status";
        String methodName = "isStatus";
        return writeDifference(header, methodName, mergingObjects, sources, sFileName);
    }

    private boolean writeSameNamesDifferentAuthorToCsv(
            List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName){

        String header = "same names but different author";
        String methodName = "getAuthor";
        return writeDifference(header, methodName, mergingObjects, sources, sFileName);
    }

    private boolean writeDifference(String header, String methodName,
            List<Map<UUID, PesiMergeObject>> mergingObjects, Map<UUID,String> sources, String sFileName) {

        try{
            Method method = methodName == null? null : PesiMergeObject.class.getMethod(methodName);

//            FileWriter writer = new FileWriter(sFileName);
            Writer writer = new OutputStreamWriter(new FileOutputStream(new File(sFileName)), StandardCharsets.UTF_8);

            //create Header
            createHeader(writer, header);

            //write data
            for (Map<UUID, PesiMergeObject> merging : mergingObjects){
                if (isDifferent(merging, method)){
                    writeCsvLine(writer, merging, sources) ;
                }
            }
            writer.flush();
            writer.close();
            return true;
        }catch(IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e){
            logger.error(e.getMessage());
            return false;
        }
    }

    private boolean isDifferent(Map<UUID, PesiMergeObject> merging, Method method)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (method == null){
            return true;
        }
        Object value = null;
        boolean isFirst = true;
        for (UUID sourceUuid: merging.keySet()){
            if (isFirst){
                value = method.invoke(merging.get(sourceUuid));
                isFirst = false;
            }else{
                Object newValue = method.invoke(merging.get(sourceUuid));
                if (!CdmUtils.nullSafeEqual(newValue, value)){
                    return true;
                }
            }
        }
        return false;
    }

	private void createHeader(Writer writer, String firstLine) throws IOException{
		 	writer.append(firstLine);
		    writer.append('\n');

		    for (int i=1; i<=2; i++){
		        writer.append("source"+i);
                writer.append(';');
                writer.append("name uuid"+i);
		        writer.append(';');
		        writer.append("name id"+i);
		        writer.append(';');
		        writer.append("name"+i);
		        writer.append(';');
		        writer.append("author"+i);
		        writer.append(';');
		        writer.append("rank"+i);
		        writer.append(';');
		        writer.append("status"+i);
		        writer.append(';');
		        writer.append("phylum"+i);
		        writer.append(';');
		        writer.append("parent"+i);
		        writer.append(';');
		        writer.append("parent rank"+i);
		        writer.append(';');
		    }
			writer.append('\n');
	}

	private void writeCsvLine(Writer writer, Map<UUID,PesiMergeObject> mergeObjects, Map<UUID,String> sources) throws IOException{

        for (UUID uuid : sourceRefUuids){
	        PesiMergeObject merging = mergeObjects.get(uuid);
	        if(merging == null){
	            continue;
	        }
	        writer.append(Nz(sources.get(uuid))).append(";");
            writer.append(Nz(merging.getUuidName())).append(";");
	        writer.append(Nz(merging.getIdInSource())).append(";");
	        writer.append(Nz(merging.getNameCache())).append(";");
	        writer.append(Nz(merging.getAuthor())).append(";");
	        writer.append(Nz(merging.getRank())).append(";");
	        if (merging.isStatus()){
	            writer.append("accepted").append(";");
	        }else{
	            writer.append("synonym").append(";");
	        }
	        writer.append(Nz(merging.getPhylum() != null? merging.getPhylum().getTitleCache(): "")).append(";");
	        writer.append(Nz(merging.getParentString())).append(";");
	        writer.append(Nz(merging.getParentRankString())).append(";");
	    }
        writer.append('\n');
	}

    private List<Map<UUID,PesiMergeObject>> createMergeObjects(Map<String, Map<UUID, Set<TaxonName>>> names,
	        CdmApplicationController appCtr){

		List<Map<UUID,PesiMergeObject>> merge = new ArrayList<>();

		List<String> nameCaches = new ArrayList<>(names.keySet());
		nameCaches.sort(StringComparator.Instance);
		for (String nameCache: nameCaches){
		    createSingleMergeObject(appCtr, merge, names.get(nameCache));
		}

		return merge;
	}


    private void createSingleMergeObject(CdmApplicationController appCtr, List<Map<UUID,PesiMergeObject>> merge,
            Map<UUID, Set<TaxonName>> identicalNames) {

        Map<UUID,PesiMergeObject> mergeMap = new HashMap<>();

        for (UUID sourceUuid : identicalNames.keySet()){
            Set<TaxonName> names = identicalNames.get(sourceUuid);
            if (names.isEmpty()){
                continue;
            }
            TaxonName name = names.iterator().next();
            String nameAndIdStr = name.getTitleCache() +  "; id = " + name.getId();
            if (names.size()>1){
                logger.warn("Multiple names per source not yet handled. Take arbitrary one: " + nameAndIdStr);
            }

            PesiMergeObject mergeObject = new PesiMergeObject();
            mergeMap.put(sourceUuid, mergeObject);

            Set<TaxonBase> taxonBases = name.getTaxonBases();
            if (taxonBases.isEmpty()){
                logger.warn("No taxonbase attached to name. This is not yet handled: " + nameAndIdStr);
                continue;
            }
            if (taxonBases.size() > 1) {
                //TODO: find the two correct names
                logger.warn("Name has not exact 1 but " + taxonBases.size() + " taxon base attached. This is not yet handled. Take arbitrary one.");
            }

            //uuid
            mergeObject.setUuidName(name.getUuid().toString());

            //nameCache
            mergeObject.setNameCache(name.getNameCache());

            //authorship
            mergeObject.setAuthor(name.getAuthorshipCache());

            //rank
            mergeObject.setRank(name.getRank().getLabel());

            //Phylum
            TaxonNodeDto phylum = getPhylum(appCtr, name);
            mergeObject.setPhylum(phylum);

            //idInSource
            Iterator<IdentifiableSource> sources = name.getSources().iterator();
            //TODO idInSource - what if multiple sources exist?
            if (sources.hasNext()){
                IdentifiableSource source = sources.next();
                String idInSource = source.getIdInSource();
                mergeObject.setIdInSource(idInSource);
            }

            //status and parent
            Set<Taxon> taxa = name.getTaxa();
            taxa = getReallyAcceptedTaxa(taxa);
            if (!taxa.isEmpty()){
                mergeObject.setStatus(true);
                Iterator<Taxon> taxaIterator = taxa.iterator();
                Taxon taxon = null;
                while (taxaIterator.hasNext()){
                    taxon = taxaIterator.next();
                    if (!taxon.isMisapplication()){
                        break;
                    }
                }
                @SuppressWarnings("null")
                Set<TaxonNode> nodes = taxon.getTaxonNodes();
                Iterator<TaxonNode> taxonNodeIterator = nodes.iterator();
                TaxonNode parentNode = null;
                while (taxonNodeIterator.hasNext()){
                    TaxonNode node = taxonNodeIterator.next();
                    if (!node.isTopmostNode()){
                        parentNode = node.getParent();
                    }
                }
                //TODO: ändern mit erweitertem Initializer..
                if (parentNode != null){
                    TaxonName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
                    String parentNameCache = parentName.getNameCache();
                    mergeObject.setParentString(parentNameCache);
                    mergeObject.setParentRankString(parentName.getRank().getLabel());
                }
            }else{
                mergeObject.setStatus(false);
                TaxonNode parentNode = getAcceptedNode(name);
                //TODO: ändern mit erweitertem Initializer..
                if (parentNode != null){
                    TaxonName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
                    String parentNameCache = parentName.getNameCache();
                    mergeObject.setParentString(parentNameCache);
                    mergeObject.setParentRankString(parentName.getRank().getLabel());
                }
            }
        }


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

        merge.add(mergeMap);
    }

    private TaxonNodeDto getPhylum(CdmApplicationController appCtr, TaxonName name) {
        TaxonNodeDto phylum = null;
        if (name.getRank().equals(Rank.PHYLUM())) {
            Taxon taxon = getAcceptedTaxon(name);
            if (taxon != null) {
                if (taxon.getTaxonNodes().size()>1){
                    logger.warn("More than 1 node not yet handled for getPhylum. Take arbitrary one.");
                }
                TaxonNode node = taxon.getTaxonNodes().iterator().next();
                phylum = new TaxonNodeDto(node);
            }

        }
        if (phylum == null && !name.getRank().isHigher(Rank.PHYLUM())){
            Taxon taxon = getAcceptedTaxon(name);
            if (!taxon.getTaxonNodes().isEmpty()){
                if (taxon.getTaxonNodes().size()>1){
                    logger.warn("More than 1 node not yet handled for getPhylum. Take arbitrary one.");
                }
                TaxonNode node = taxon.getTaxonNodes().iterator().next();
                phylum = appCtr.getTaxonNodeService().taxonNodeDtoParentRank(node.getClassification(), Rank.PHYLUM(), name);
            }
        }
        return phylum;
    }

	private TaxonNode getAcceptedNode(TaxonName ermsName) {
	    TaxonNode parentNode = null;
		Set<TaxonBase> taxonBases = ermsName.getTaxonBases();
		if (!taxonBases.isEmpty()) {
		    Taxon taxon = null;
			TaxonBase<?> taxonBase = taxonBases.iterator().next();
			if (taxonBase instanceof Synonym) {
				taxon = ((Synonym)taxonBase).getAcceptedTaxon();
			}else{
			    taxon = getAccTaxonForTaxonSynonym((Taxon)taxonBase);
			}
			Set<TaxonNode> nodes = taxon.getTaxonNodes();
			if (!nodes.isEmpty()) {
			    parentNode = nodes.iterator().next();
			}
		}

		return parentNode;
	}

	private Taxon getAcceptedTaxon(TaxonName name) {
		Taxon taxon = null;
		//prefer accepted taxon
		if (name.getTaxa() != null && !name.getTaxa().isEmpty()){
			taxon = name.getTaxa().iterator().next();
			taxon = getAccTaxonForTaxonSynonym(taxon);
		//else take synonym
		}else if (name.getTaxonBases() != null && !name.getTaxonBases().isEmpty()){
			TaxonBase<?> taxonBase = name.getTaxonBases().iterator().next();
			if (taxonBase instanceof Synonym) {
				Synonym syn = (Synonym)taxonBase;
				taxon = syn.getAcceptedTaxon();
			}
		}
		return taxon;
	}

    private Taxon getAccTaxonForTaxonSynonym(Taxon taxon) {
        if (!taxon.getRelationsFromThisTaxon().isEmpty()){
            for (TaxonRelationship rel: taxon.getRelationsFromThisTaxon()){
                UUID uuidType = rel.getType().getUuid();
                if (uuidType.equals(TaxonRelationshipType.uuidSynonymOfTaxonRelationship)
                        || uuidType.equals(TaxonRelationshipType.uuidHeterotypicSynonymTaxonRelationship)
                        || uuidType.equals(TaxonRelationshipType.uuidHomotypicSynonymTaxonRelationship)){
                    taxon = rel.getToTaxon();
                }
            }
        }
        return taxon;
    }

    /**
     * Filters out the ERMS taxon synonyms
     */
    private Set<Taxon> getReallyAcceptedTaxa(Set<Taxon> taxa) {
        Set<Taxon> result = new HashSet<>();
        for (Taxon taxon : taxa){
            Taxon accTaxon = getAccTaxonForTaxonSynonym(taxon);
            if(taxon.equals(accTaxon)) {
                result.add(taxon);
            }
        }
        return result;
    }

    private CharSequence Nz(String str) {
        return CdmUtils.Nz(str);
    }

    public static void main(String[] args) {
        PesiFindIdenticalNamesActivator activator = new PesiFindIdenticalNamesActivator();
        activator.invoke(pesiSource);
        System.exit(0);
    }
}
