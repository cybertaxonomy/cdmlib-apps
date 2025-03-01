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
import java.sql.Timestamp;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.TdwgAreaProvider;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.SourcedEntityBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.reference.ISourceable;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public abstract class BerlinModelImportBase
            extends DbImportBase<BerlinModelImportState, BerlinModelImportConfigurator>
            implements ICdmIO<BerlinModelImportState>, IPartitionedIO<BerlinModelImportState> {

    private static final long serialVersionUID = -4982506434258587864L;
    private static final Logger logger = LogManager.getLogger();

	public BerlinModelImportBase(String tableName, String pluralString ) {
		super(tableName, pluralString);
	}

	@Override
    protected String getIdQuery(BerlinModelImportState state){
		String result = " SELECT " + getTableName() + "id FROM " + getTableName();
		return result;
	}


	protected boolean doIdCreatedUpdatedNotes(BerlinModelImportState state, DescriptionElementBase descriptionElement, ResultSet rs, String id, String namespace) throws SQLException{
		boolean success = true;
		//id
		success &= doId(state, descriptionElement, id, namespace);
		//createdUpdateNotes
		success &= doCreatedUpdatedNotes(state, descriptionElement, rs);
		return success;
	}

	protected boolean doIdCreatedUpdatedNotes(BerlinModelImportState state, IdentifiableEntity identifiableEntity, ResultSet rs, long id, String namespace, boolean excludeUpdated, boolean excludeNotes)
			throws SQLException{
		boolean success = true;
		//id
		success &= doId(state, identifiableEntity, id, namespace);
		//createdUpdateNotes
		success &= doCreatedUpdatedNotes(state, identifiableEntity, rs, excludeUpdated, excludeNotes);
		return success;
	}


	protected boolean doIdCreatedUpdatedNotes(BerlinModelImportState state, IdentifiableEntity identifiableEntity, ResultSet rs, long id, String namespace)
			throws SQLException{
		boolean excludeUpdated = false;
		return doIdCreatedUpdatedNotes(state, identifiableEntity, rs, id, namespace, excludeUpdated, false);
	}

	protected boolean doCreatedUpdatedNotes(BerlinModelImportState state, AnnotatableEntity annotatableEntity, ResultSet rs)
			throws SQLException{
		boolean excludeUpdated = false;
		return doCreatedUpdatedNotes(state, annotatableEntity, rs, excludeUpdated, false);
	}

	protected boolean doCreatedUpdatedNotes(BerlinModelImportState state, AnnotatableEntity annotatableEntity,
	        ResultSet rs, boolean excludeUpdated, boolean excludeNotes)
			throws SQLException{

		BerlinModelImportConfigurator config = state.getConfig();
		Object createdWhen = rs.getObject("Created_When");
		String createdWho = rs.getString("Created_Who");
		createdWho = normalizeUsername(state, createdWho);
		Object updatedWhen = null;
		String updatedWho = null;
		if (excludeUpdated == false){
			try {
				updatedWhen = rs.getObject("Updated_When");
				updatedWho = rs.getString("Updated_who");
				updatedWho = normalizeUsername(state, updatedWho);
			} catch (SQLException e) {
				//Table "Name" has no updated when/who
			}
		}

		boolean success  = true;

		//Created When, Who, Updated When Who
		if (config.getEditor() == null || config.getEditor().equals(EDITOR.NO_EDITORS)){
			//do nothing
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_ANNOTATION)){
			String createdAnnotationString = "Berlin Model record was created By: " + String.valueOf(createdWho) + " (" + String.valueOf(createdWhen) + ") ";
			if (updatedWhen != null && updatedWho != null){
				createdAnnotationString += " and updated By: " + String.valueOf(updatedWho) + " (" + String.valueOf(updatedWhen) + ")";
			}
			Annotation annotation = Annotation.NewInstance(createdAnnotationString, Language.DEFAULT());
			//TODO make transaction compatible, same as common sec reference
			annotation.setCommentator(config.getCommentator());
			annotation.setAnnotationType(AnnotationType.INTERNAL());
			annotatableEntity.addAnnotation(annotation);
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_EDITOR)){
		    User creator;
		    boolean xmlSourceAdded = addXmlSource(state, rs, annotatableEntity, createdWho, false);
		    if (xmlSourceAdded){
		        creator = getXmlImporter(state);
		    }else{
		        creator = getUser(state, createdWho);
		    }
		    annotatableEntity.setCreatedBy(creator);

		    User updator;
		    xmlSourceAdded = addXmlSource(state, rs, annotatableEntity, updatedWho, xmlSourceAdded);
		    if (xmlSourceAdded){
		        updator = getXmlImporter(state);
		    }else{
		        updator = getUser(state, updatedWho);
		    }
			annotatableEntity.setUpdatedBy(updator);

			DateTime created = getDateTime(createdWhen);
			DateTime updated = getDateTime(updatedWhen);
			annotatableEntity.setCreated(created);
			annotatableEntity.setUpdated(updated);
		}else {
			logger.warn("Editor type not yet implemented: " + config.getEditor());
		}


		//notes
		if (! excludeNotes){
		    String notes = rs.getString("notes");
			doNotes(annotatableEntity, notes);
		}
		return success;
	}

	/**
     * @param state
     * @return
     */
    private User getXmlImporter(BerlinModelImportState state) {
        return getUser(state, "import to BM");
    }

    private boolean addXmlSource(BerlinModelImportState state, ResultSet rs, AnnotatableEntity annotatableEntity, String username, boolean existsAlready) throws SQLException {
        if (!state.getConfig().isEuroMed()){
            return false;
        }
        if (isXmlImport(username) && existsAlready){
            return true;
        }
        String idInSource = getIdInSource(state, rs);

        boolean isXmlUser = isXmlImport(username);
        Reference ref = isXmlUser? getXmlRef(state, username): null;
        if (ref != null || isNotBlank(idInSource)){
            if (annotatableEntity.isInstanceOf(SourcedEntityBase.class)){
                SourcedEntityBase<?> sourcedEntity = CdmBase.deproxy(annotatableEntity, SourcedEntityBase.class);
                sourcedEntity.addImportSource(idInSource, null, ref, null);
            }else if (annotatableEntity.isInstanceOf(DescriptionElementBase.class)){
                DescriptionElementBase descriptionElement = CdmBase.deproxy(annotatableEntity, DescriptionElementBase.class);
                descriptionElement.addImportSource(idInSource, null, ref, null);
            }else {
                return false;
            }
        }
        return isXmlUser;
    }

    //can be overriden
    protected String getIdInSource(BerlinModelImportState state, ResultSet rs) throws SQLException {
        return null;
    }


    /**
     * @param state
     * @param username
     * @return
     */
    private Reference getXmlRef(BerlinModelImportState state, String username) {
        String namespace = "IMPORT USER";
        Reference ref = state.getRelatedObject(namespace, username, Reference.class);
        if (ref == null){
            if (state.getXmlImportRefUuid(username)!= null){
                ref = getReferenceService().find(state.getXmlImportRefUuid(username));
            }
            if (ref == null){
                Pager<Reference> pager = getReferenceService().findByTitle(Reference.class, username, MatchMode.EXACT, null, null, null, null, null);
                if (pager.getCount()>0){
                    ref = pager.getRecords().get(0);
                    if (pager.getCount()>1){
                        logger.warn("More then 1 reference found for " +  username);
                    }
                }
            }
            if (ref == null){
                ref = ReferenceFactory.newDatabase();
                ref.setTitleCache(username, true);
                ref.setTitle(username);
                ref.addImportSource(null, this.getTableName(), state.getTransactionalSourceReference(), null);
                getReferenceService().save(ref);
            }
            state.addRelatedObject(namespace, username, ref);
            state.putXmlImportRefUuid(username, ref.getUuid());

        }
        return ref;
    }


    /**
     * @param username
     * @return
     */
    private boolean isXmlImport(String username) {
        if (username == null){
            return false;
        }
        return username.matches(".*\\.xml")
                ||username.equals("MCL-Import, a.mueller")
                ||username.equals("pandora import (J.Li)")
                ||username.equals("Import from Kew Checklist 2010")
                ||username.equals("Import from ILDIS 2010")
                ||username.equals("s_em_ImportCastroviejoReferences")
                ||username.equals("s_em_ImportUotilaReferences")
                ||username.equals("s_em_ImportValdesReferences")
                ||username.equals("s_em_NewImportNomReferences")
                ||username.equals("Import from Anthos 2010");
    }


    private DateTime getDateTime(Object timeString){
		if (timeString == null){
			return null;
		}
		DateTime dateTime = null;
		if (timeString instanceof Timestamp){
			Timestamp timestamp = (Timestamp)timeString;
			dateTime = new DateTime(timestamp);
		}else{
			logger.warn("time ("+timeString+") is not a timestamp. Datetime set to current date. ");
			dateTime = new DateTime();
		}
		return dateTime;
	}

	/**
	 * @param state
	 * @param newTaxonId
	 * @param taxonMap
	 * @param factId
	 * @return
	 */
	protected Taxon getTaxon(BerlinModelImportState state, int taxonId, Map<String, TaxonBase> taxonMap, int factId) {
		TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(taxonId));

		//TODO for testing
//		if (taxonBase == null && ! state.getConfig().isDoTaxa()){
//			taxonBase = Taxon.NewInstance(TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES()), null);
//		}

		Taxon taxon;
		if ( taxonBase instanceof Taxon ) {
			taxon = (Taxon) taxonBase;
		} else if (taxonBase != null) {
			logger.warn("TaxonBase (" + taxonId + ") for Fact(Specimen) with factId " + factId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
			return null;
		} else {
			logger.warn("TaxonBase (" + taxonId + ") for Fact(Specimen) with factId " + factId + " is null.");
			return null;
		}
		return taxon;
	}


	/**
	 * 	Searches first in the detail maps then in the ref maps for a reference.
	 *  Returns the reference as soon as it finds it in one of the map, according
	 *  to the order of the map.
	 *  If nomRefDetailFk is <code>null</code> no search on detail maps is performed.
	 *  If one of the maps is <code>null</code> no search on the according map is
	 *  performed. <BR>
	 *  You may define the order of search by the order you pass the maps but
	 *  make sure to always pass the detail maps first.
	 * @param firstDetailMap
	 * @param secondDetailMap
	 * @param firstRefMap
	 * @param secondRefMap
	 * @param nomRefDetailFk
	 * @param nomRefFk
	 * @return
	 */
	protected Reference getReferenceFromMaps(
			Map<String, Reference> detailMap,
			Map<String, Reference> refMap,
			String nomRefDetailFk,
			String nomRefFk) {
		Reference ref = null;
		if (detailMap != null){
			ref = detailMap.get(nomRefDetailFk);
		}
		if (ref == null){
			ref = refMap.get(nomRefFk);
		}
		return ref;
	}


	/**
	 * Searches for a reference in the first detail map. If it does not exist it
	 * searches in the second detail map. Returns null if it does not exist in any map.
	 * A map may be <code>null</code> to avoid search on this map.
	 * @param secondDetailMap
	 * @param firstDetailMap
	 * @param nomRefDetailFk
	 * @return
	 */
	private Reference getReferenceDetailFromMaps(Map<String, Reference> firstDetailMap, Map<String, Reference> secondDetailMap, String nomRefDetailFk) {
		Reference result = null;
		if (nomRefDetailFk != null){
			//get ref
			if (firstDetailMap != null){
				result = firstDetailMap.get(nomRefDetailFk);
			}
			if (result == null && secondDetailMap != null){
				result = secondDetailMap.get(nomRefDetailFk);
			}
		}
		return result;
	}

	protected NamedArea getOtherAreas(BerlinModelImportState state, String emCodeString, String tdwgCodeString) {
		String em = CdmUtils.Nz(emCodeString).trim();
		String tdwg = CdmUtils.Nz(tdwgCodeString).trim();
		//Cichorieae + E+M
		if ("EM".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.euroMedUuid, "Euro+Med", "Euro+Med area", "EM", null, null);
		}else if("Rf".equals(em)){
			return Country.RUSSIANFEDERATION();

		}else if("KRY-OO;UKR-UK".equals(tdwg)){
			return Country.UKRAINE();

		}else if("TCS-AZ;TCS-NA".equals(tdwg)){
			return Country.AZERBAIJANREPUBLICOF();
		}else if("TCS-AB;TCS-AD;TCS-GR".equals(tdwg)){
			return Country.GEORGIA();

		}else if("Cc".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidCaucasia, "Caucasia (Ab + Ar + Gg + Rf(CS))", "Euro+Med area 'Caucasia (Ab + Ar + Gg + Rf(CS))'", "Cc", null, null);
		}

		//E+M
		else if("EUR".equals(em)){
			return TdwgAreaProvider.getAreaByTdwgAbbreviation("1");
		}else if("14".equals(em)){
			return TdwgAreaProvider.getAreaByTdwgAbbreviation("14");
		}else if("21".equals(em)){
			return TdwgAreaProvider.getAreaByTdwgAbbreviation("21");  // Macaronesia
		}else if("33".equals(em)){
			return TdwgAreaProvider.getAreaByTdwgAbbreviation("33");

		}else if("SM".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidSerbiaMontenegro, "Serbia & Montenegro", "Euro+Med area 'Serbia & Montenegro'", "SM", NamedAreaType.ADMINISTRATION_AREA(), null);
		}else if("Sr".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidSerbia, "Serbia", "Euro+Med area 'Serbia' (including Kosovo and Vojvodina)", "Sr", NamedAreaType.ADMINISTRATION_AREA(), null);


		//see #2769
		}else if("Rs".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidUssr, "Former USSR", "Euro+Med area 'Former USSR'", "Rs", NamedAreaType.ADMINISTRATION_AREA(), null);
		}else if("Rs(N)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidRussiaNorthern, "Russia Northern", "Euro+Med area 'Russia Northern'", "Rs(N)", null, null);
		}else if("Rs(B)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidRussiaBaltic, "Russia Baltic", "Euro+Med area 'Russia Baltic'", "Rs(B)", null, null);
		}else if("Rs(C)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidRussiaCentral, "Russia Central", "Euro+Med area 'Russia Central'", "Rs(C)", null, null);
		}else if("Rs(W)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidRussiaSouthWest, "Russia Southwest", "Euro+Med area 'Russia Southwest'", "Rs(W)", null, null);
		}else if("Rs(E)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidRussiaSouthEast, "Russia Southeast", "Euro+Med area 'Russia Southeast'", "Rs(E)", null, null);

		//see #2770
		}else if("AE".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidEastAegeanIslands, "East Aegean Islands", "Euro+Med area 'East Aegean Islands'", "AE", null, null);
		}else if("AE(T)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTurkishEastAegeanIslands, "Turkish East Aegean Islands", "Euro+Med area 'Turkish East Aegean Islands'", "AE(T)", null, null);
		}else if("Tu".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTuerkiye, "Türkiye", "Euro+Med area 'Türkiye' (without AE(T))", "Tu", null, null);

		//TODO Azores, Canary Is.
		}else if("Md(D)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidDesertas, "Desertas", "Euro+Med area 'Desertas'", "Md(D)", null, null);
		}else if("Md(M)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidMadeira, "Madeira", "Euro+Med area 'Madeira'", "Md(M)", null, null);
		}else if("Md(P)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidPortoSanto, "Porto Santo", "Euro+Med area 'Porto Santo'", "Md(P)", null, null);
		//Azores
		}else if("Az(L)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidFlores, "Flores", "Euro+Med area 'Flores'", "Az(L)", null, null);
		}else if("Az(C)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidCorvo, "Corvo", "Euro+Med area 'Corvo'", "Az(C)", null, null);
		}else if("Az(F)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidFaial, "Faial", "Euro+Med area 'Faial'", "Az(F)", null, null);
		}else if("Az(G)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidGraciosa, "Graciosa", "Euro+Med area 'Graciosa'", "Az(G)", null, null);
		}else if("Az(J)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidSaoJorge, "S\u00E3o Jorge", "Euro+Med area 'S\u00E3o Jorge'", "Az(J)", null, null);
		}else if("Az(M)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidSaoMiguel, "S\u00E3o Miguel", "Euro+Med area 'S\u00E3o Miguel'", "Az(M)", null, null);
		}else if("Az(P)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidPico, "Pico", "Euro+Med area 'Pico'", "Az(P)", null, null);
		}else if("Az(S)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidSantaMaria, "Santa Maria", "Euro+Med area 'Santa Maria'", "Az(S)", null, null);
		}else if("Az(T)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTerceira, "Terceira", "Euro+Med area 'Terceira'", "Az(T)", null, null);
		//Canary Islands
		}else if("Ca(C)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidGranCanaria, "Gran Canaria", "Euro+Med area 'Gran Canaria'", "Ca(C)", null, null);
		}else if("Ca(F)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidFuerteventura, "Fuerteventura with Lobos", "Euro+Med area 'Fuerteventura with Lobos'", "Ca(F)", null, null);
		}else if("Ca(G)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidGomera, "Gomera", "Euro+Med area 'Gomera'", "Ca(G)", null, null);
		}else if("Ca(H)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidHierro, "Hierro", "Euro+Med area 'Hierro'", "Ca(H)", null, null);
		}else if("Ca(L)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidLanzaroteWithGraciosa, "Lanzarote with Graciosa", "Euro+Med area 'Lanzarote with Graciosa'", "Ca(L)", null, null);
		}else if("Ca(P)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidLaPalma, "La Palma", "Euro+Med area 'La Palma'", "Ca(P)", null, null);
		}else if("Ca(T)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTenerife, "Tenerife", "Euro+Med area 'Tenerife'", "Ca(T)", null, null);
			//Baleares
		}else if("Bl(I)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidIbizaWithFormentera, "Ibiza with Formentera", "Euro+Med area 'Ibiza with Formentera'", "Bl(I)", null, null);
		}else if("Bl(M)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTerceira, "Mallorca", "Euro+Med area 'Mallorca'", "Bl(M)", null, null);
		}else if("Bl(N)".equals(em)){
			return getNamedArea(state, BerlinModelTransformer.uuidTerceira, "Menorca", "Euro+Med area 'Menorca'", "Bl(N)", null, null);
		}

		logger.warn("Area(em: '" + em + "', tdwg: '" + tdwg +"') could not be found");

		return null;
	}

    /**
     * @param state
     * @return
     */
    protected TermVocabulary<MarkerType> getEuroMedMarkerTypeVoc(BerlinModelImportState state) {
        TermVocabulary<MarkerType> markerTypeVoc = getVocabulary(state, TermType.MarkerType, BerlinModelTransformer.uuidVocEMMarkerType,
                "Euro+Med marker type vocabulary", "E+M marker types", null, null, false, MarkerType.COMPLETE());
        return markerTypeVoc;
    }


    /**
     * @param sourceReference
     * @return
     */
    protected Reference getSourceReference(Reference sourceReference) {
        Reference persistentSourceReference = getReferenceService().find(sourceReference.getUuid());  //just to be sure
        if (persistentSourceReference != null){
            sourceReference = persistentSourceReference;
        }
        return sourceReference;
    }

    protected static <T extends IdentifiableSource> boolean importSourceExists(ISourceable<T> sourceable, String idInSource,
            String namespace, Reference ref) {
        for (T source : sourceable.getSources()){
            if (CdmUtils.nullSafeEqual(namespace, source.getIdNamespace()) &&
                CdmUtils.nullSafeEqual(idInSource, source.getIdInSource()) &&
                CdmUtils.nullSafeEqual(ref, source.getCitation())){
                    return true;
            }
        }
        return false;
    }

    /**
     * @param state
     * @param username
     * @return
     */
    protected String normalizeUsername(BerlinModelImportState state, String username) {
        if (username == null){
            return null;
        }else{
            username = username.trim();
            if (state.getConfig().isEuroMed()){
                if (username.matches("[A-Za-z]+[7-9][0-9]")){
                    username = username.substring(0, username.length()-2);
                }else if(username.matches("(mariam[1-4]|palermo|palma|paltar)")){
                    username = "mariam";
                }else if (username.matches("a.mueller.*") || "AM".equals(username)){
                    username = "a.mueller";
                }else if (username.matches("sh,.*")){
                    username = "sh";
                }else if (username.matches("J.Li.*pandora")){
                    username = "pandora import (J.Li)";
                }else if (username.matches("euromed")){
                    username = "em";
                }
                if(username.matches("kapet")){
                    username = "kpet";
                }
            }else if (state.getConfig().isMcl()) {
                username = username.replace(", ", "_");
            }
            return username;
        }
    }

}
