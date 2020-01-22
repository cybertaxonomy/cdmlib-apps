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
	static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final();

	static final String path = System.getProperty("user.home")+File.separator+".cdmLibrary"+File.separator+"pesi"+File.separator+"pesimerge";

	private static UUID emSourceUuid = PesiTransformer.uuidSourceRefEuroMed;
	private static UUID ermsSourceUuid = PesiTransformer.uuidSourceRefErms;
	private static UUID faunaEuSourceUuid = PesiTransformer.uuidSourceRefFaunaEuropaea;
	private static UUID ifSourceUuid = PesiTransformer.uuidSourceRefIndexFungorum;
	private static List<UUID> sourceRefUuids = new ArrayList<>();
	private static Map<UUID,String> sourcesLabels = new HashMap<>();

    static {
        sourceRefUuids.addAll(Arrays.asList(new UUID[]{emSourceUuid, ermsSourceUuid, faunaEuSourceUuid, ifSourceUuid}));
        sourcesLabels.put(emSourceUuid, "E+M");
        sourcesLabels.put(ermsSourceUuid, "ERMS");
        sourcesLabels.put(faunaEuSourceUuid, "FauEu");
        sourcesLabels.put(ifSourceUuid, "IF");
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
        List<Map<UUID, List<PesiMergeObject>>> mergingObjects = createMergeObjects(namesOfIdenticalTaxa, app);
        app.commitTransaction(tx);

        boolean resultOK = true;
        System.out.println("Start creating csv files");
        resultOK &= writeSameNamesToCsvFile(mergingObjects, path + "_namesAll.csv");
        resultOK &= writeSameNamesDifferentAuthorToCsv(mergingObjects, path + "_authors.csv");
        resultOK &= writeSameNamesDifferentStatusToCsv(mergingObjects, path + "_status.csv");
        resultOK &= writeSameNamesDifferentPhylumToCsv(mergingObjects, path + "_phylum.csv");
        resultOK &= writeSameNamesDifferentParentToCsv(mergingObjects, path + "_parent.csv");
        resultOK &= writeSameNamesDifferentRankToCsv(mergingObjects, path + "_rank.csv");

        System.out.println("End find identical names for PESI: " + resultOK + ". Results written to " + path);
	}

	private boolean writeSameNamesToCsvFile(
			List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName) {

	    String header = "same names (all)";
        String methodName = null;
        return writeDifference(header, methodName, mergingObjects, sFileName);
	}

	private boolean writeSameNamesDifferentPhylumToCsv(
	        List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName){

	    String header = "same names but different phylum";
	    String methodName = "getPhylum";
	    return writeDifference(header, methodName, mergingObjects, sFileName);
	}

    private boolean writeSameNamesDifferentParentToCsv(
	        List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName){

		    String header = "same names but different parent";
	        String methodName = "getParentString";
	        return writeDifference(header, methodName, mergingObjects, sFileName);
	}

	private boolean writeSameNamesDifferentRankToCsv(
	        List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName){

        String header = "same names but different rank";
        String methodName = "getRank";
        return writeDifference(header, methodName, mergingObjects, sFileName);
	}

    private boolean writeSameNamesDifferentStatusToCsv(
            List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName){

        String header = "same names but different status";
        String methodName = "isStatus";
        return writeDifference(header, methodName, mergingObjects, sFileName);
    }

    private boolean writeSameNamesDifferentAuthorToCsv(
            List<Map<UUID,List<PesiMergeObject>>> mergingObjects, String sFileName){

        String header = "same names but different author";
        String methodName = "getAuthor";
        return writeDifference(header, methodName, mergingObjects, sFileName);
    }

    private boolean writeDifference(String header,
            String methodName,
            List<Map<UUID,List<PesiMergeObject>>> mergingObjects,
            String sFileName) {

        try{
            Method method = methodName == null? null : PesiMergeObject.class.getMethod(methodName);

            Writer writer = new OutputStreamWriter(new FileOutputStream(new File(sFileName)), StandardCharsets.UTF_8);

            //create Header
            createHeader(writer, header);

            //write data
            for (Map<UUID,List<PesiMergeObject>> merging : mergingObjects){
                boolean isNextNameCache = true;
                List<UUID> mySources = new ArrayList<>(merging.keySet());
                for (int i = 0; i<mySources.size()-1; i++){
                    for (int j = i+1; j<mySources.size(); j++){
                        boolean differenceExists = false;
                        List<PesiMergeObject> mergeList1 = merging.get(mySources.get(i));
                        List<PesiMergeObject> mergeList2 = merging.get(mySources.get(j));
                        for (PesiMergeObject merge1 : mergeList1){
                            for (PesiMergeObject merge2 : mergeList2){
                                differenceExists |= isDifferent(merge1, merge2, method);
                            }
                        }
                        if (differenceExists){
                            for (PesiMergeObject merge1 : mergeList1){
                                for (PesiMergeObject merge2 : mergeList2){
                                    writeCsvLine(writer, merge1, merge2, method, isNextNameCache);
                                    isNextNameCache = false;
                                }
                            }
                        }
                    }
                }
            }
            writer.flush();
            writer.close();
            return true;
        }catch(NoSuchMethodException | SecurityException | IOException e){
            logger.error(e.getMessage());
            return false;
        }
    }

    private boolean isDifferent(PesiMergeObject merge1, PesiMergeObject merge2, Method method){

        try {
            if (method == null){
                return true;
            }
            Object value1 = method.invoke(merge1);
            Object value2 = method.invoke(merge2);
            return !CdmUtils.nullSafeEqual(value1, value2);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return true;
        }
    }

    //old method when all sources were in 1 line
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

	private void createHeader(Writer writer, String firstLine){
	 	try {
            writer.append(firstLine);
            writer.append('\n');
            writeHeaderPair(writer, "taxon uuid");
            writeHeaderPair(writer, "taxon id");
            writer.append("next name cache").append(";");
            writer.append("diff").append(";");
            writeHeaderPair(writer, "source");
            writeHeaderPair(writer, "name uuid");
            writeHeaderPair(writer, "idInSource");
            writeHeaderPair(writer, "nameCache");
            writeHeaderPair(writer, "author");
            writeHeaderPair(writer, "rank");
            writeHeaderPair(writer, "kingdom");
            writeHeaderPair(writer, "phylum");
            writeHeaderPair(writer, "family");
            writeHeaderPair(writer, "parentString");
            writeHeaderPair(writer, "parentRankString");
            writeHeaderPair(writer, "status");
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    private void writeHeaderPair(Writer writer, String header) throws IOException {
        writer.append(header+"1").append(';');
        writer.append(header+"2").append(';');
    }

    private void writeCsvLine(Writer writer,
           PesiMergeObject merge1, PesiMergeObject merge2,
           Method method, boolean isNextNameCache){

        writePair(writer, merge1, merge2, "UuidTaxon");
        writePair(writer, merge1, merge2, "IdTaxon");
        writeSingleValue(writer, isNextNameCache?"1":"0");
        boolean different = isDifferent(merge1,  merge2, method);
        writeSingleValue(writer, different?"1":"0");
        writeSingleValue(writer, sourcesLabels.get(UUID.fromString(merge1.getUuidSource())));
        writeSingleValue(writer, sourcesLabels.get(UUID.fromString(merge2.getUuidSource())));
        writePair(writer, merge1, merge2, "UuidName");
        writePair(writer, merge1, merge2, "IdInSource");
        writePair(writer, merge1, merge2, "NameCache");
        writePair(writer, merge1, merge2, "Author");
        writePair(writer, merge1, merge2, "Rank");
        writePairNode(writer, merge1, merge2, "Kingdom");
        writePairNode(writer, merge1, merge2, "Phylum");
        writePairNode(writer, merge1, merge2, "Family");
        writePair(writer, merge1, merge2, "ParentString");
        writePair(writer, merge1, merge2, "ParentRankString");
        writeSingleValue(writer, merge1.isStatus()?"accepted":"synonym");
        writeSingleValue(writer, merge2.isStatus()?"accepted":"synonym");
        try {
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSingleValue(Writer writer, String value) {
        try {
            writer.append(value).append(";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writePairNode(Writer writer, PesiMergeObject merge1, PesiMergeObject merge2, String methodName) {
        try {
            Method method = PesiMergeObject.class.getDeclaredMethod("get"+methodName);
            TaxonNodeDto value = (TaxonNodeDto) method.invoke(merge1);
            writer.append(value==null?"":value.getTitleCache()).append(";");
            value = (TaxonNodeDto) method.invoke(merge2);
            writer.append(value==null?"":value.getTitleCache()).append(";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writePair(Writer writer, PesiMergeObject merge1, PesiMergeObject merge2, String methodName) {
        try {
            Method method = PesiMergeObject.class.getDeclaredMethod("get"+methodName);
            String value1 = (String) method.invoke(merge1);
            writer.append(normalize(value1)).append(";");
            String value2 = (String) method.invoke(merge2);
            writer.append(normalize(value2)).append(";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalize(String val) {
        return CdmUtils.Nz(val).replace(";", "@");
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

    private List<Map<UUID,List<PesiMergeObject>>> createMergeObjects(
            Map<String, Map<UUID, Set<TaxonName>>> identicalNames,
	        CdmApplicationController appCtr){

		List<Map<UUID,List<PesiMergeObject>>> merge = new ArrayList<>();

		List<String> nameCaches = new ArrayList<>(identicalNames.keySet());
		nameCaches.sort(StringComparator.Instance);
		for (String nameCache: nameCaches){
		    createSingleMergeObject(appCtr, merge, identicalNames.get(nameCache));
		}
		return merge;
	}

    private void createSingleMergeObject(CdmApplicationController appCtr,
            List<Map<UUID,List<PesiMergeObject>>> merge,
            Map<UUID, Set<TaxonName>> identicalNames) {

        Map<UUID,List<PesiMergeObject>> mergeMap = new HashMap<>();

        for (UUID sourceUuid : identicalNames.keySet()){
            Set<TaxonName> names = identicalNames.get(sourceUuid);
            List<PesiMergeObject> pmoList = new ArrayList<>();
            mergeMap.put(sourceUuid, pmoList);

            for (TaxonName name : names){
                String nameAndIdStr = name.getTitleCache() +  "; id = " + name.getId();
                @SuppressWarnings("rawtypes")
                Set<TaxonBase> taxonBases = name.getTaxonBases();
                if (taxonBases.isEmpty()){
                    logger.warn("No taxonbase attached to name. This is not yet handled: " + nameAndIdStr);
                    continue;
                }
                for (TaxonBase<?> taxonBase : taxonBases)                {
                    if (!taxonBase.isPublish()){
                        continue;
                    }
                    PesiMergeObject mergeObject = PesiMergeObject.NewInstance();
                    pmoList.add(mergeObject);

                    //uuid
                    mergeObject.setUuidSource(sourceUuid.toString());
                    mergeObject.setUuidName(name.getUuid().toString());
                    mergeObject.setUuidTaxon(taxonBase.getUuid().toString());
                    mergeObject.setIdTaxon(String.valueOf(taxonBase.getId()));

                    //nameCache
                    mergeObject.setNameCache(name.getNameCache());

                    //authorship
                    mergeObject.setAuthor(name.getAuthorshipCache());

                    //rank
                    mergeObject.setRank(name.getRank().getLabel());

                    //Kingdom
                    TaxonNodeDto kingdom = getHigherTaxon(appCtr, name, Rank.KINGDOM());
                    mergeObject.setKingdom(kingdom);

                    //Phylum/Division
                    TaxonNodeDto phylum = getHigherTaxon(appCtr, name, Rank.PHYLUM());
                    if(phylum == null){
                        phylum = getHigherTaxon(appCtr, name, Rank.DIVISION());
                    }
                    mergeObject.setPhylum(phylum);

                    //Family
                    TaxonNodeDto family = getHigherTaxon(appCtr, name, Rank.FAMILY());
                    mergeObject.setFamily(family);

                    //idInSource
                    Iterator<IdentifiableSource> sources = name.getSources().iterator();
                    //TODO idInSource - what if multiple sources exist?
                    if (sources.hasNext()){
                        IdentifiableSource source = sources.next();
                        String idInSource = source.getIdInSource();
                        mergeObject.setIdInSource(idInSource);
                    }

                    //status and parent
                    makeStatusAndParent(name, mergeObject);
                }
            }
        }

        merge.add(mergeMap);


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


    }

    private void makeStatusAndParent(TaxonName name, PesiMergeObject mergeObject) {
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
            if (parentNode != null){
                TaxonName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
                String parentNameCache = parentName.getNameCache();
                mergeObject.setParentString(parentNameCache);
                mergeObject.setParentRankString(parentName.getRank().getLabel());
            }
        }else{
            mergeObject.setStatus(false);
            TaxonNode parentNode = getAcceptedNode(name);
            if (parentNode != null){
                TaxonName parentName = CdmBase.deproxy(parentNode.getTaxon().getName());
                String parentNameCache = parentName.getNameCache();
                mergeObject.setParentString(parentNameCache);
                mergeObject.setParentRankString(parentName.getRank().getLabel());
            }
        }
    }

    private TaxonNodeDto getHigherTaxon(CdmApplicationController appCtr, TaxonName name, Rank rank) {
        if (name.getRank().equals(rank)) {
            Taxon taxon = getAcceptedTaxon(name);
            if (taxon != null) {
                if (taxon.getTaxonNodes().isEmpty()){
                    return null;  //probably MAN
                }
                if (taxon.getTaxonNodes().size()>1){
                    logger.warn("More than 1 node not yet handled for getHigherTaxon. Take arbitrary one.");
                }
                TaxonNode node = taxon.getTaxonNodes().iterator().next();
                return new TaxonNodeDto(node);
            }
        }
        if (name.getRank().isHigher(rank)){
            return null;
        }else{
            Taxon taxon = getAcceptedTaxon(name);
            if (taxon.getTaxonNodes().isEmpty()){
                return null;
            }else{
                if (taxon.getTaxonNodes().size()>1){
                    logger.warn("More than 1 node not yet handled for getHigherTaxon. Take arbitrary one.");
                }
                TaxonNode node = taxon.getTaxonNodes().iterator().next();
                List<TaxonNodeDto> higherDtos = appCtr.getTaxonNodeService().taxonNodeDtoParentRank(node.getClassification(), rank, taxon);
                if (higherDtos.isEmpty()){
                    return null;
                }else {
                    if (higherDtos.size() > 1){
                        logger.warn("More than 1 higher dto. This is not yet implemented: " + taxon.getTitleCache());
                    }
                    return higherDtos.get(0);
                }
            }
        }
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
