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

import org.apache.commons.lang3.StringUtils;
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

/**
 * Finds taxa with identical {@link TaxonName#getNameCache() name cache} but from different
 * sources (import source) and writes them into multiple csv file.
 * All cases are stored in file xxx_namesAll, some prefiltered files are created for e.g.
 * those having different parents or different authors.
 * Taxa are pairwise compared. If a name appears in 3 sources for each of the 3 pairs 1 record
 * is created below each other. Also if a name appears multiple times (e.g. homonyms) in 1
 * DB and 1 time in another. Each of the multiple names is compared to the other databases
 * record.
 * <BR><BR>
 *
 * TODO is is necessary to create these extra files? Filters can also be appied in Excel.
 *
 * @author a.mueller
 * @since 22.01.2020
 */
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
        sourceRefUuids.addAll(Arrays.asList(new UUID[]{
//                emSourceUuid,
                ermsSourceUuid
                ,faunaEuSourceUuid
//              ,  ifSourceUuid
        }));
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
                        int combinations = mergeList1.size() * mergeList2.size();
                        if (differenceExists){
                            for (PesiMergeObject merge1 : mergeList1){
                                for (PesiMergeObject merge2 : mergeList2){
                                    writeCsvLine(writer, merge1, merge2, method, isNextNameCache, combinations);
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
                return !merge1.getIdTaxon().equals(merge2.getIdTaxon());
            }
            Object value1 = method.invoke(merge1);
            Object value2 = method.invoke(merge2);
            return !CdmUtils.nullSafeEqual(value1, value2);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return true;
        }
    }

	private void createHeader(Writer writer, String firstLine){
	 	try {
            writer.append(firstLine);
            writer.append('\n');
            writeHeaderPair(writer, "tid");
            writer.append("use;");
            writer.append("nameUse;");
            writer.append("combinations;");
            writer.append("diff;");
            writeHeaderPair(writer, "src");
//            writeHeaderPair(writer, "nuuid");
//            writeHeaderPair(writer, "idInSource");
            writer.append("nameCache;");
            writeHeaderPair(writer, "author");
            writeHeaderPair(writer, "nom.ref.");
            writeHeaderPair(writer, "rank");
            writeHeaderPair(writer, "classification");
            writeHeaderPair(writer, "kingdom");
            writeHeaderPair(writer, "phylum");
            writeHeaderPair(writer, "class");
            writeHeaderPair(writer, "order");
            writeHeaderPair(writer, "family");
            writeHeaderPair(writer, "parentString");
            writeHeaderPair(writer, "parentRankString");
            writeHeaderPair(writer, "status");
            writeHeaderPair(writer, "tuuid");
            writer.append("firstAuthor;");
            writer.append("firstRank;");
            writer.append("firstClassification;");
            writer.append("firstKingdom;");
            writer.append("firstPhylum;");
            writer.append("firstClass;");
            writer.append("firstOrder;");
            writer.append("firstFamily;");
            writer.append("firstStatus;");

            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    private void writeHeaderPair(Writer writer, String header) throws IOException {
        writer.append(header+"1").append(';');
        writer.append(header+"2").append(';');
    }

    //needs to be synchronized with writeHeader()
    private void writeCsvLine(Writer writer,
           PesiMergeObject merge1, PesiMergeObject merge2,
           Method method, boolean isNextNameCache, int combinations){  //isNextNameCache probably not needed anymore

        writePair(writer, merge1, merge2, "IdTaxon", Compare.NO);
        writeSingleValue(writer, "");
        writeSingleValue(writer, "");
//        writeSingleValue(writer, isNextNameCache?"1":"0");
        writeSingleValue(writer, String.valueOf(combinations));
        boolean different = isDifferent(merge1,  merge2, method);
        writeSingleValue(writer, different?"1":"0");
        writeSingleValue(writer, sourcesLabels.get(UUID.fromString(merge1.getUuidSource())));
        writeSingleValue(writer, sourcesLabels.get(UUID.fromString(merge2.getUuidSource())));
//        writePair(writer, merge1, merge2, "UuidName");
//        writePair(writer, merge1, merge2, "IdInSource");
        writeSingleValue(writer, merge1.getNameCache());
//        writePair(writer, merge1, merge2, "NameCache");
        writePair(writer, merge1, merge2, "Author", Compare.YES);
        writePair(writer, merge1, merge2, "NomenclaturalReference", Compare.YES);
        writePair(writer, merge1, merge2, "Rank", Compare.YES);
        writePair(writer, merge1, merge2, "ClassificationCache", Compare.YES);
        writePair(writer, merge1, merge2, "KingdomCache", Compare.YES);
        writePair(writer, merge1, merge2, "PhylumCache", Compare.YES);
        writePair(writer, merge1, merge2, "ClassCache", Compare.YES);
        writePair(writer, merge1, merge2, "OrderCache", Compare.YES);
        writePair(writer, merge1, merge2, "FamilyCache", Compare.YES);
        writePair(writer, merge1, merge2, "ParentString", Compare.YES);
        writePair(writer, merge1, merge2, "ParentRankString", Compare.YES);
        writePair(writer, merge1, merge2, "StatusStr", Compare.YES);
        writePair(writer, merge1, merge2, "UuidTaxon", Compare.YES);
        writeSingleValue(writer, merge1.getAuthor());
        writeSingleValue(writer, merge1.getRank());
        writeSingleValue(writer, merge1.getClassificationCache());
        writeSingleValue(writer, merge1.getKingdomCache());
        writeSingleValue(writer, merge1.getPhylumCache());
        writeSingleValue(writer, merge1.getClassCache());
        writeSingleValue(writer, merge1.getOrderCache());
        writeSingleValue(writer, merge1.getFamilyCache());
        writeSingleValue(writer, merge1.getStatusStr());


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

    private enum Compare{
        NO,
        YES,
        KEEP_FIRST;

        boolean isAnyCompare(){
            return this != NO;
        }
    }

    private void writePair(Writer writer, PesiMergeObject merge1, PesiMergeObject merge2, String methodName, Compare compare) {
        try {
            Method method = PesiMergeObject.class.getDeclaredMethod("get"+methodName);
            String value1 = (String) method.invoke(merge1);
            String value2 = (String) method.invoke(merge2);
            if (compare.isAnyCompare() && CdmUtils.nullSafeEqual(value1, value2)){
                value2 = StringUtils.isBlank(value2)? "":"-";
                if (compare == Compare.YES){
                    value1 = value2;
                }
            }
            writer.append(normalize(value1)).append(";");
            writer.append(normalize(value2)).append(";");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalize(String val) {
        return Nz(val).replace(";", "@");
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

                    //nom.ref.
                    mergeObject.setNomenclaturalReference(name.getNomenclaturalReference()== null?null: name.getNomenclaturalReference().getAbbrevTitleCache());

                    //rank
                    mergeObject.setRank(name.getRank().getLabel());

                    //higherTaxa
                    List<TaxonNodeDto> higherTaxa = getHigherTaxa(appCtr, name);
                    mergeObject.setHigherClassification(higherTaxa);

                    //Kingdom
                    TaxonNodeDto kingdom = getHigherTaxon(appCtr, name, Rank.KINGDOM());
                    mergeObject.setKingdom(kingdom);

                    //Phylum/Division
                    TaxonNodeDto phylum = getHigherTaxon(appCtr, name, Rank.PHYLUM());
                    if(phylum == null){
                        phylum = getHigherTaxon(appCtr, name, Rank.DIVISION());
                    }
                    mergeObject.setPhylum(phylum);

                    //Class
                    TaxonNodeDto tclass = getHigherTaxon(appCtr, name, Rank.CLASS());
                    mergeObject.setTClass(tclass);

                    //Class
                    TaxonNodeDto order = getHigherTaxon(appCtr, name, Rank.ORDER());
                    mergeObject.setOrder(order);

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

    private List<TaxonNodeDto> getHigherTaxa(CdmApplicationController appCtr, TaxonName name) {
        List<TaxonNodeDto> result = new ArrayList<>();
        Taxon taxon = getAcceptedTaxon(name);
        if (taxon.getTaxonNodes().isEmpty()){
            return null;  //probably MAN
        }
        if (taxon.getTaxonNodes().size()>1){
            logger.warn("More than 1 node not yet handled for getHigherTaxon. Take arbitrary one.");
        }
        TaxonNode node = taxon.getTaxonNodes().iterator().next();
        TaxonNodeDto nodeDto = new TaxonNodeDto(node);
        if (!taxon.getName().equals(name)){
            result.add(nodeDto);  //for synonyms add accepted taxon as first node
        }
        while (nodeDto.getTaxonUuid()!= null){
            nodeDto = appCtr.getTaxonNodeService().parentDto(nodeDto.getUuid());
            if (nodeDto.getTaxonUuid()!= null){
                result.add(0, nodeDto);  //for synonyms add accepted taxon as first node
            }
        }
        return result;
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

    private String Nz(String str) {
        return CdmUtils.Nz(str);
    }

    public static void main(String[] args) {
        PesiFindIdenticalNamesActivator activator = new PesiFindIdenticalNamesActivator();
        activator.invoke(pesiSource);
        System.exit(0);
    }
}
