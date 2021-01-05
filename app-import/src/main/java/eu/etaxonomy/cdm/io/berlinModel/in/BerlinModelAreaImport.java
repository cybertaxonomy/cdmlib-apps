/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.berlinModel.in;

import eu.etaxonomy.cdm.common.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

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
    private static final Logger logger = Logger.getLogger(BerlinModelAreaImport.class);

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


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
        // not relevant
        return true;
    }

    @Override
    public void doInvoke(BerlinModelImportState state)  {
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
        return;
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

	/**
     * @param areaLevelEm2
	 * @param euroMedAreas2
     */
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

            MarkerType hiddenAreaMarkerType = getMarkerType(state, BerlinModelTransformer.uuidHiddenArea,
                    "Hidden Area","Used to hide distributions for the named areas in publications", null, getEuroMedMarkerTypeVoc(state));

            //Add hidden area marker to Rs(C) and Rs(N)
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_B);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_C);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_E);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_N);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_K);
            hideArea(euroMedAreasVoc, hiddenAreaMarkerType, BerlinModelTransformer.uuidRs_W);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in markAreasAsHidden: " + e.getMessage());
        }

    }

    private void hideArea(TermVocabulary<NamedArea> euroMedAreasVoc, MarkerType hiddenAreaMarkerType, UUID areaUuid) {
        for (NamedArea namedArea : euroMedAreasVoc.getTerms()){
            if (namedArea.getUuid().equals(areaUuid)){
                namedArea.addMarker(Marker.NewInstance(hiddenAreaMarkerType, true));
                return;
            }
        }
    }

    /**
     * @param oldArea
     * @param namedArea
     * @param areaLevelEm2
     */
    private void makeSubterm(NamedArea oldArea, NamedArea namedArea, NamedAreaLevel areaLevelEm2) {
        namedArea.setLevel(areaLevelEm2);
        namedArea.setPartOf(oldArea);
    }


	/**
	 * @param eurMarkerType
	 * @param euroMedAreaMarkerType
	 * @param isoCodeExtType
	 * @param tdwgCodeExtType
	 * @param mclCodeExtType
	 * @param rs
	 * @param areaLevelEm2
	 * @param areaLevelEm1
	 * @param areaLevelTop
	 * @throws SQLException
	 */
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

	/**
	 *
	 */
	private OrderedTermVocabulary<NamedArea> makeEmptyEuroMedVocabulary() {
		TermType type = TermType.NamedArea;
		String description = "Euro+Med area vocabulary";
		String label = "E+M Areas";
		String abbrev = null;
		URI termSourceUri = null;
		OrderedTermVocabulary<NamedArea> result = OrderedTermVocabulary.NewInstance(type, description, label, abbrev, termSourceUri);

		result.setUuid(BerlinModelTransformer.uuidVocEuroMedAreas);
		getVocabularyService().save(result);
		return result;
	}


    /**
     * @param state
     */
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

	/**
     * @param state
	 * @param voc
     * @param uuidCauc1
     * @param string
     * @param string2
     * @param string3
     * @param string4
     * @return
     */
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

    /**
     * @param state
     */
    private OrderedTermVocabulary<NamedArea> makeEmptyCaucasusVocabulary(BerlinModelImportState state) {
        TermType type = TermType.NamedArea;
        String description = "E+M Caucasus area vocabulary";
        String label = "E+M Caucasus Areas";
        String abbrev = null;
        URI termSourceUri = null;
        OrderedTermVocabulary<NamedArea> result = OrderedTermVocabulary.NewInstance(type, description, label, abbrev, termSourceUri);
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
