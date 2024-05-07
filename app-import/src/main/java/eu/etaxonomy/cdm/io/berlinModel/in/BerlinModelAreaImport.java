/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelAreaImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelAreaImport  extends BerlinModelImportBase {

    private static final long serialVersionUID = -2810052908318645219L;
    private static final Logger logger = LogManager.getLogger();

	public static final String NAMESPACE = "emArea";

	private static final String pluralString = "areas";
	private static final String dbTableName = "emArea";  //??

	public BerlinModelAreaImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
//		String result =
//		          " SELECT AreaId "
//		        + " FROM " + getTableName();
//		if (state.getConfig().isEuroMed()){
//		    result += " WHERE AreaID NOT IN (1, 21, 650, 653, 1718, 654, 646, 647) ";  //#3986
//		}
//		return result;
	    return null; //not relevant as we have no partitioning here
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
//		String strQuery =
//		          " SELECT * "
//		        + " FROM emArea a "
//                + " WHERE (a.AreaId IN (" + ID_LIST_TOKEN + ")  )"
//		        + " ORDER BY a.AreaId "
//                ;
//		return strQuery;
	    return null;  //not relevant as we have no partitioning here
	}

	private Map<Integer, NamedArea> euroMedAreas = new HashMap<>();

    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
        // not relevant
        return true;
    }

    @Override
    public void doInvoke(BerlinModelImportState state)  {
        if (state.getConfig().isEuroMed()) {
            TermVocabulary<?> voc = getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
            if (voc == null){
                try {
                    createEuroMedAreas(state);
                    createCaucasusAreas(state);
                } catch (SQLException e) {
                    logger.warn("Exception when creating areas: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }else if (state.getConfig().isMcl()) {
            TermVocabulary<?> voc = getVocabularyService().find(BerlinModelTransformer.uuidVocMclAreas);
            if (voc == null){
                createMclAreas(state);
            }
        }
        return;
    }

    private TermVocabulary<NamedArea> createMclAreas(BerlinModelImportState state) {
        logger.warn("Start creating MCL areas");

        Reference sourceReference = state.getConfig().getSourceReference();

        TransactionStatus txStatus = this.startTransaction();
        sourceReference = getSourceReference(sourceReference);

        OrderedTermVocabulary<NamedAreaLevel> mclAreaLevelVoc = makeEmptyMclAreaLevelVocabulary();
        NamedAreaLevel areaLevelTop = getNamedAreaLevel(state, BerlinModelTransformer.uuidMclAreaLevelTop, "MCL top area level",
                "MCL top area level. This level is only to be used for the area representing the complete MCL area", "mcl top", mclAreaLevelVoc);
        NamedAreaLevel areaLevelMclMain = getNamedAreaLevel(state, BerlinModelTransformer.uuidMclAreaLevelFirst,
                "MCL main area level", "MCL main area level", "mcl main", mclAreaLevelVoc);

        OrderedTermVocabulary<NamedArea> mclAreasVoc = makeEmptyMclVocabulary();
        NamedArea topArea = createMclArea(state, mclAreasVoc, null, areaLevelTop, "MCL", "MCL Area", "f0500f01-0a59-4a6b-83cf-4070182f7266");

        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "AE", "East Aegean Islands", "e2367915-828b-4151-a3af-1a278abf1cd5");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Ag", "Algeria", "9dea2928-65fc-4999-8a5d-f63552553f9f");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Al", "Albania", "53e87d91-f5a8-434b-86c9-268750c3473b");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "An", "Asiatic Turkey", "96394d80-85b7-4b5d-940e-28772cb8fe46");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Bl", "Balearic Islands", "b9259337-c216-44b2-be26-337e1beebf5f");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Bu", "Bulgaria", "bb85aa3f-18cb-4961-866e-8bdedaf0c41b");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Co", "Corsica", "bcc5a02c-b37f-4639-9f50-e5623da46c95");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Cr", "Crete and Karpathos", "0773529f-e230-4397-8ba5-cd12d5af4172");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Cy", "Cyprus", "3b502256-4db5-47be-96da-c20341f7984e");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Eg", "Egypt", "9564126a-e24c-4ca1-a229-dd5c084dd543");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Ga", "France", "1a0b6e9d-9568-434f-8346-f33cd31b0c5f");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Gr", "Greece", "4a9a0f92-eb51-428a-b82f-96ee7ed77c60");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Hs", "Spain", "29f6ac94-2573-4122-87c7-165710037ff6");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "IJ", "Israel and Jordan", "abaa10ea-7da4-4940-81e3-6a6023b0d6b5");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "It", "Italy", "7003c0d4-ffab-4ee6-888d-47fc483fa8bd");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Ju", "Jugoslavia", "6409b4b8-2b3d-440c-b22c-ef98ed3257f3");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Li", "Libya", "a5418d46-5f3c-4964-8b12-19509bb313e1");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "LS", "Lebanon and Syria", "023f8dba-40f5-4d4e-b2ca-4e3004a25d1c");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Lu", "Portugal", "6b5018ed-d637-4dd2-a08e-e0d2f7633688");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Ma", "Morocco", "b9b85f84-6e3e-4f6c-b3bc-2f42d17bf077");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Me", "Malta", "6394ee61-999c-473e-88ed-aad9259ffa81");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "RK", "Crimea", "60ea0344-0639-451d-81fd-33263014f30f");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Sa", "Sardinia", "c3a7d579-998d-472b-a238-26d893ee03cb");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Si", "Sicily", "5187232e-38d3-4bec-9094-2f90abde78b8");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Sn", "Sinai", "54a53738-2e04-433e-9657-dd1fac45094e");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Tn", "Tunisia", "46d2df14-0b45-4413-bbf0-022d769cb479");
        createMclArea(state, mclAreasVoc, topArea, areaLevelMclMain, "Tu", "Turkey-in-Europe", "aa93af77-033f-4096-a4ca-468ba07c64d9");

        getVocabularyService().saveOrUpdate(mclAreasVoc);
        try {
            commitTransaction(txStatus);
        } catch (Exception e) {
             e.printStackTrace();
             logger.error("An exception occurred when trying to commit MCL Areas");
        }
        logger.warn("Created MCL areas");

        return mclAreasVoc;
    }

    private NamedArea createMclArea(BerlinModelImportState state, OrderedTermVocabulary<NamedArea> mclAreasVoc, NamedArea topArea,
            NamedAreaLevel areaLevel, String abbrev, String label, String uuidStr) {
        NamedArea namedArea = makeSingleMclArea(state, mclAreasVoc, topArea, areaLevel, UUID.fromString(uuidStr), label, abbrev);
        state.putNamedArea(namedArea);
        return namedArea;
    }

    private NamedArea makeSingleMclArea(@SuppressWarnings("unused") BerlinModelImportState state, OrderedTermVocabulary<NamedArea> voc,
            NamedArea top, NamedAreaLevel areaLevel, UUID uuid, String label, String labelAbbrev) {

        NamedArea namedArea = NamedArea.NewInstance(label, label, labelAbbrev);
        namedArea.setUuid(uuid);

        namedArea.setPartOf(top);
        namedArea.setLevel(areaLevel);
        namedArea.setIdInVocabulary(labelAbbrev);
        namedArea.setSymbol(labelAbbrev);
        namedArea.setSymbol2(labelAbbrev);

        String geoAbbrev = labelAbbrev;
        if ("An".equalsIgnoreCase(geoAbbrev)) {
            geoAbbrev = "Tu(A)";
        } else if ("Me".equalsIgnoreCase(geoAbbrev)) {
            geoAbbrev = "Si(M)";
        } else if ("RK".equalsIgnoreCase(geoAbbrev)) {
            geoAbbrev = "Cm";
        } else if ("Si".equalsIgnoreCase(geoAbbrev)) {
            geoAbbrev = "Si(S)";
        } else if ("Tu".equalsIgnoreCase(geoAbbrev)) {
            geoAbbrev = "Tu(E)";
        }

        String geoMapping = "<?xml version=\"1.0\" ?><mapService xmlns=\"http://www.etaxonomy.eu/cdm\" type=\"editMapService\">"
                + "<area><layer>euromed_2013</layer><field>EMAREA</field>"
                + "<value>" + geoAbbrev + "</value></area></mapService>";
        Annotation geoServiceMapping = Annotation.NewInstance(geoMapping, AnnotationType.TECHNICAL(), Language.UNKNOWN_LANGUAGE());
        namedArea.addAnnotation(geoServiceMapping);

        voc.addTerm(namedArea);
        return namedArea;
    }

    private TermVocabulary<NamedArea> createEuroMedAreas(BerlinModelImportState state) throws SQLException {

	    logger.warn("Start creating E+M areas");
		Source source = state.getConfig().getSource();
		Reference sourceReference = state.getConfig().getSourceReference();

		TransactionStatus txStatus = this.startTransaction();

		sourceReference = getSourceReference(sourceReference);

		TermVocabulary<NamedArea> euroMedAreasVoc = makeEmptyEuroMedVocabulary();

		MarkerType eurMarkerType = getMarkerType(state, BerlinModelTransformer.uuidEurArea, "eur", "eur Area", "eur", getEuroMedMarkerTypeVoc(state));
		MarkerType euroMedAreaMarkerType = getMarkerType(state, BerlinModelTransformer.uuidEurMedArea, "EuroMedArea", "EuroMedArea", "EuroMedArea", getEuroMedMarkerTypeVoc(state));
		ExtensionType isoCodeExtType = getExtensionType(state, BerlinModelTransformer.uuidIsoCode, "IsoCode", "IsoCode", "iso");
		ExtensionType tdwgCodeExtType = getExtensionType(state, BerlinModelTransformer.uuidTdwgAreaCode, "TDWG code", "TDWG Area code", "tdwg");
		ExtensionType mclCodeExtType = getExtensionType(state, BerlinModelTransformer.uuidMclCode, "MCL code", "MedCheckList code", "mcl");
		NamedAreaLevel areaLevelTop = getNamedAreaLevel(state, BerlinModelTransformer.uuidEuroMedAreaLevelTop, "Euro+Med top area level", "Euro+Med top area level. This level is only to be used for the area representing the complete Euro+Med area", "e+m top", null);
		NamedAreaLevel areaLevelEm1 = getNamedAreaLevel(state, BerlinModelTransformer.uuidEuroMedAreaLevelFirst, "Euro+Med 1. area level", "Euro+Med 1. area level", "e+m 1.", null);
		NamedAreaLevel areaLevelEm2 = getNamedAreaLevel(state, BerlinModelTransformer.uuidEuroMedAreaLevelSecond, "Euro+Med 2. area level", "Euro+Med 2. area level", "Euro+Med 1. area level", null);

		String sql = "SELECT * , CASE WHEN EMCode = 'EM' THEN 'a' ELSE 'b' END as isEM " +
				" FROM emArea " +
				" WHERE areaId not IN (1, 14, 20, 21, 33, 646, 647, 653, 654, 1718) " +
				" ORDER BY isEM, EMCode";
		ResultSet rs = source.getResultSet(sql);

		NamedArea euroMedArea = null;
		NamedArea lastLevel1Area = null;

		//euroMedArea (EMCode = 'EM')
		rs.next();
		euroMedArea = makeSingleEuroMedArea(rs, eurMarkerType, euroMedAreaMarkerType, isoCodeExtType, tdwgCodeExtType, mclCodeExtType,
				areaLevelTop, areaLevelEm1 , areaLevelEm2, sourceReference, euroMedArea, lastLevel1Area);
		euroMedAreasVoc.addTerm(euroMedArea);

		//all other areas
		while (rs.next()){
			NamedArea newArea = makeSingleEuroMedArea(rs, eurMarkerType, euroMedAreaMarkerType,
					isoCodeExtType, tdwgCodeExtType, mclCodeExtType,
					areaLevelTop, areaLevelEm1 , areaLevelEm2, sourceReference, euroMedArea, lastLevel1Area);
			if (newArea != null){
    			euroMedAreasVoc.addTerm(newArea);
    			if (newArea.getPartOf().equals(euroMedArea)){
    				lastLevel1Area = newArea;
    			}
			}
		}
		emAreaFinetuning(euroMedAreasVoc, areaLevelEm2);

		markAreasAsHidden(state, euroMedAreasVoc);

	    getVocabularyService().saveOrUpdate(euroMedAreasVoc);

		try {
            commitTransaction(txStatus);
        } catch (Exception e) {
             e.printStackTrace();
             logger.error("An exception occurred when trying to commit E+M Areas");
        }
		logger.warn("Created E+M areas");

		return euroMedAreasVoc;
	}

    private void emAreaFinetuning(TermVocabulary<NamedArea> euroMedAreas, NamedAreaLevel areaLevelEm2) {
        //CZ
        NamedArea oldArea = euroMedAreas.getTermByIdInvocabulary("Cz");
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Cs"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Sk"), areaLevelEm2);

        //Ju
        oldArea = euroMedAreas.getTermByIdInvocabulary("Ju");
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("BH"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Cg"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Ct"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Mk"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Sl"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Sr"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Yu(K)"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("SM"), areaLevelEm2);

        //IJ
        oldArea = euroMedAreas.getTermByIdInvocabulary("IJ");
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Ir"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Jo"), areaLevelEm2);

        //LS
        oldArea = euroMedAreas.getTermByIdInvocabulary("LS");
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Le"), areaLevelEm2);
        makeSubterm(oldArea, euroMedAreas.getTermByIdInvocabulary("Sy"), areaLevelEm2);

    }

    //5.Mark areas to be hidden #3979 .5
    private void markAreasAsHidden(BerlinModelImportState state, TermVocabulary<NamedArea> euroMedAreasVoc) {

        try {

            MarkerType fallbackAreaMarkerType = getMarkerType(state, BerlinModelTransformer.uuidHiddenArea,
                    "Hidden Area","Used to hide distributions for the named areas in publications", null, getEuroMedMarkerTypeVoc(state));

            //Add hidden area marker to Rs(C) and Rs(N)
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_B);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_C);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_E);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_N);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_K);
            hideArea(euroMedAreasVoc, fallbackAreaMarkerType, BerlinModelTransformer.uuidRs_W);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in markAreasAsHidden: " + e.getMessage());
        }

    }

    private void hideArea(TermVocabulary<NamedArea> euroMedAreasVoc, MarkerType fallbackAreaMarkerType, UUID areaUuid) {
        for (NamedArea namedArea : euroMedAreasVoc.getTerms()){
            if (namedArea.getUuid().equals(areaUuid)){
                namedArea.addMarker(Marker.NewInstance(fallbackAreaMarkerType, true));
                return;
            }
        }
    }

    private void makeSubterm(NamedArea oldArea, NamedArea namedArea, NamedAreaLevel areaLevelEm2) {
        namedArea.setLevel(areaLevelEm2);
        namedArea.setPartOf(oldArea);
    }

	private NamedArea makeSingleEuroMedArea(ResultSet rs, MarkerType eurMarkerType,
			MarkerType euroMedAreaMarkerType, ExtensionType isoCodeExtType,
			ExtensionType tdwgCodeExtType, ExtensionType mclCodeExtType,
			NamedAreaLevel areaLevelTop, NamedAreaLevel areaLevelEm1, NamedAreaLevel areaLevelEm2,
			Reference sourceReference, NamedArea euroMedArea, NamedArea level1Area) throws SQLException {

	    Integer areaId = rs.getInt("AreaId");
		String emCode = nullSafeTrim(rs.getString("EMCode"));
		String isoCode = nullSafeTrim(rs.getString("ISOCode"));
		String tdwgCode = nullSafeTrim(rs.getString("TDWGCode"));
		String unit = nullSafeTrim(rs.getString("Unit"));
//				      ,[Status]
//				      ,[OutputOrder]
		boolean eurMarker = rs.getBoolean("eur");
		boolean euroMedAreaMarker = rs.getBoolean("EuroMedArea");
		String notes = nullSafeTrim(rs.getString("Notes"));
		String mclCode = nullSafeTrim(rs.getString("MCLCode"));
		String geoSearch = nullSafeTrim(rs.getString("NameForGeoSearch"));

		if (isBlank(emCode)){
			emCode = unit;
		}

		//uuid
		UUID uuid = BerlinModelTransformer.getEMAreaUuid(emCode);
		NamedArea area = (NamedArea)getTermService().find(uuid);
		if (area == null){
			//label
			area = NamedArea.NewInstance(geoSearch, unit, emCode);
			if (uuid != null){
				area.setUuid(uuid);
			}else{
			    if (areaId == 211 || areaId == 213){  //Additional Azores and Canary Is. area are merged into primary area, see also area.addSource part below
			        return null;
			    }
				logger.warn("Uuid for emCode could not be defined: " + emCode);
			}
		}

		//code
		area.setIdInVocabulary(emCode);
		//notes
		if (StringUtils.isNotEmpty(notes)){
			area.addAnnotation(Annotation.NewInstance(notes, AnnotationType.EDITORIAL(), Language.DEFAULT()));
		}
		//markers
		area.addMarker(Marker.NewInstance(eurMarkerType, eurMarker));
		area.addMarker(Marker.NewInstance(euroMedAreaMarkerType, euroMedAreaMarker));

		//extensions
		if (isNotBlank(isoCode)){
			area.addExtension(isoCode, isoCodeExtType);
		}
		if (isNotBlank(tdwgCode)){
			area.addExtension(tdwgCode, tdwgCodeExtType);
		}
		if (isNotBlank(mclCode)){
			area.addExtension(mclCode, mclCodeExtType);
		}

		//type
		area.setType(NamedAreaType.ADMINISTRATION_AREA());

		//source
		area.addSource(OriginalSourceType.Import, String.valueOf(areaId), NAMESPACE, sourceReference, null);
		//add duplicate area ids for canary
		if (areaId == 624){ //Canary Is.
		    area.addSource(OriginalSourceType.Import, String.valueOf(213), NAMESPACE, sourceReference, null);
		}
		if (areaId == 210){//Azores
            area.addSource(OriginalSourceType.Import, String.valueOf(211), NAMESPACE, sourceReference, null);
        }

		//parent
		if (euroMedArea != null){
			if (emCode.contains("(") && !emCode.startsWith("Yu(K)")){
				area.setPartOf(level1Area);
				area.setLevel(areaLevelEm2);
			}else{
				area.setPartOf(euroMedArea);
				area.setLevel(areaLevelEm1);
			}
		}else{
			area.setLevel(areaLevelTop);
		}
		this.euroMedAreas.put(areaId, area);

		//save
		getTermService().saveOrUpdate(area);

		return area;
	}

	private String nullSafeTrim(String string) {
		if (string == null){
			return null;
		}else{
			return string.trim();
		}
	}

	private OrderedTermVocabulary<NamedArea> makeEmptyEuroMedVocabulary() {
		TermType type = TermType.NamedArea;
		String description = "Euro+Med area vocabulary";
		String label = "E+M Areas";
		String abbrev = null;
		URI termSourceUri = null;
		OrderedTermVocabulary<NamedArea> result = OrderedTermVocabulary.NewOrderedInstance(type, null, description, label, abbrev, termSourceUri);

		result.setUuid(BerlinModelTransformer.uuidVocEuroMedAreas);
		getVocabularyService().save(result);
		return result;
	}

    private OrderedTermVocabulary<NamedArea> makeEmptyMclVocabulary() {
        TermType type = TermType.NamedArea;
        String description = "MCL area vocabulary";
        String label = "MCL Areas";
        String abbrev = null;
        URI termSourceUri = null;
        OrderedTermVocabulary<NamedArea> result = OrderedTermVocabulary.NewOrderedInstance(type, null, description, label, abbrev, termSourceUri);

        result.setUuid(BerlinModelTransformer.uuidVocMclAreas);
        getVocabularyService().save(result);
        return result;
    }

    private OrderedTermVocabulary<NamedAreaLevel> makeEmptyMclAreaLevelVocabulary() {
        TermType type = TermType.NamedAreaLevel;
        String description = "MCL area level vocabulary";
        String label = "MCL Area Levels";
        String abbrev = null;
        URI termSourceUri = null;
        OrderedTermVocabulary<NamedAreaLevel> result = OrderedTermVocabulary.NewOrderedInstance(type, null, description, label, abbrev, termSourceUri);

        result.setUuid(UUID.randomUUID());
        getVocabularyService().save(result);
        return result;
    }

    private void createCaucasusAreas(BerlinModelImportState state) {
        OrderedTermVocabulary<NamedArea> voc = makeEmptyCaucasusVocabulary(state);
        NamedArea last = null;
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc1, "Western Ciscaucasia","WCC","1","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc1a, "Azov-Kuban","Az.-Kub.","1","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc1b, "Western Stavropol","W. Stavr.","1","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc2, "Eastern Ciscaucasia","ECC","2","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc2a, "Eastern Stavropol","E. Stavr.","2","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc2b, "Terek-Kuma","Ter.-Kuma ","2","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc2c, "Terek-Sulak","Ter.-Sul.","2","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc3, "Western Caucasus","WC","3","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc3a, "Adagum-Pshish","Adag.-Pshish","3","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc3b, "Belaja-Laba","Bel.-Laba","3","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc3c, "Urup-Teberda","Urup-Teb.","3","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc3d, "Upper Kuban","U. Kub.","3","d");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc4, "Central Caucasus","CC","4","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc4a, "Upper Kuma","U. Kuma","4","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc4b, "Malka","Malka","4","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc4c, "Upper Terek","U. Ter.","4","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc5, "Eastern Caucasus","EC","5","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc5a, "Assa-Argun","Assa-Arg.","5","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc5b, "Upper Sulak","U. Sulak","5","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc5c, "Manas-Samur","Man.-Samur","5","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc5d, "Kubinsky","Kubin.","5","d");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc6, "North-Western Transcaucasia","NWTC","6","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc6a, "Anapa-Gelendzhik","Anapa-Gel.","6","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc6b, "Pshada-Dzubga","Pshada-Dzhubga ","6","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7, "Western Transcaucasia","WTC","7","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7a, "Tuapse-Adler","Tuap.-Adl.","7","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7b, "Abkhasia","Abkh.","7","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7c, "Inguri-Rioni","Ing.-Rioni","7","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7d, "Rioni-Kvirili","Rioni-Kvir.","7","d");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc7e, "Adzharia","Adzh.","7","e");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc8, "Central Transcaucasia","CTC","8","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc8a, "Karthalinia-South Ossetia","Kart.-S. Oss.","8","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc8b, "Trialetia-Lower Karthalinia","Trial.-L. Kart.","8","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc8c, "Lori","Lori","8","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9, "Eastern Transcaucasia","ETC","9","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9a, "Alazan-Agrichay","Alaz.-Agrich.","9","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9b, "Shirvan","Shirv.","9","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9c, "Iori-Sheki","Iori-Sheki","9","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9d, "Murghuz-Murovdagh","Murgh.-Murovd.","9","d");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9e, "Lower Kura","L. Kura","9","e");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc9f, "Karabagh","Karab.","9","f");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc10, "South-Western Transcaucasia","SWTC","10","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc10a, "Meskhetia","Meskh.","10","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc10b, "Dzhavachetia-Upper Akhurjan","Dzhav.-U. Akh.","10","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc10c, "Aragatz","Arag.","10","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11, "Southern Transcaucasia","STC","11","");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11a, "Erevan","Erev.","11","a");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11b, "Sevan","Sevan","11","b");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11c, "Daraleghiz","Dar.","11","c");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11d, "Nakhitshevan","Nakh.","11","d");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11e, "Zangezur","Zang.","11","e");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11f, "Megri-Zangelan","Megri-Zan.","11","f");
        last = makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc11g, "Southern Karabagh","S. Karab.","11","g");
        makeSingleArea(state, voc, last, BerlinModelTransformer.uuidCauc12, "Talysch","T","12","");
    }

    private NamedArea makeSingleArea(BerlinModelImportState state, OrderedTermVocabulary<NamedArea> voc, NamedArea last,
            UUID uuid, String label, String labelAbbrev,
            String mainLevel, String subLevel) {

        NamedArea namedArea = NamedArea.NewInstance(label, label, labelAbbrev);
        namedArea.setUuid(uuid);
        if (isBlank(subLevel)){
            namedArea.setLevel(getNamedAreaLevel(state, BerlinModelTransformer.uuidCaucasusAreaLevelFirst, "Caucasus main areas", "Caucasus main areas", "Cauc. I", null));
        }else{
            namedArea.setLevel(getNamedAreaLevel(state, BerlinModelTransformer.uuidCaucasusAreaLevelSecond, "Caucasus sub areas", "Caucasus sub areas", "Cauc. II", null));
            if(last.getPartOf() != null){
                namedArea.setPartOf(last.getPartOf());
            }else{
                namedArea.setPartOf(last);
            }
        }
        String idInVoc = mainLevel + subLevel;
        namedArea.setIdInVocabulary(idInVoc);
        voc.addTerm(namedArea);
        return namedArea;
    }

    private OrderedTermVocabulary<NamedArea> makeEmptyCaucasusVocabulary(BerlinModelImportState state) {
        TermType type = TermType.NamedArea;
        String description = "E+M Caucasus area vocabulary";
        String label = "E+M Caucasus Areas";
        String abbrev = null;
        URI termSourceUri = null;
        OrderedTermVocabulary<NamedArea> result = OrderedTermVocabulary.NewOrderedInstance(type, null, description, label, abbrev, termSourceUri);
        result.setUuid(BerlinModelTransformer.uuidVocCaucasusAreas);
        getVocabularyService().save(result);
        return result;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelAreaImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		if (state.getConfig().isDoNamedAreas()){
		    return false;
		}else if (! (state.getConfig().isDoOccurrence() || state.getConfig().isDoCommonNames())){
			return true;
		}else{
			if (!this.checkSqlServerColumnExists(state.getConfig().getSource(), "emArea", "AreaId")){
				logger.error("emArea table or AreaId column do not exist. Must ignore area import");
				return true;
			}else{
			    TermVocabulary<?> voc = getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
		        return voc != null;
			}
		}
	}
}