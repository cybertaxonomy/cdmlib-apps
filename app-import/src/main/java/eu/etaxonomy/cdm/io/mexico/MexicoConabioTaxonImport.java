/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
@Component
public class MexicoConabioTaxonImport<CONFIG extends MexicoConabioImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 3691221053127007258L;
    private static final Logger logger = LogManager.getLogger();

    public static final String TAXON_NAMESPACE = "Taxonomia";

    @Override
    protected String getWorksheetName(CONFIG  config) {
        return "Taxonomia";
    }

    //dirty I know, but who cares, needed by distribution and commmon name import
    protected static final Map<String, TaxonBase<?>> taxonIdMap = new HashMap<>();

    private Classification classification;


    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "IdCAT","IdCATRel","IdCAT_AscendenteInmediato"
            ,"IdCAT_AscendenteObligatorio","CategoriaTaxonomica","Nombre",
            "EstatusNombre","AutorNombre","AutorSinAnio","Anio",
            "ReferenciaNombre",
            "Division","AutorDivision","ReferenciaClasificacionDivision",
            "Clase","AutorClase","ReferenciaClasificacionClase",
            "Subclase","AutorSubclase","ReferenciaClasificacionSubclase",
            "Superorden","AutorSuperorden","ReferenciaClasificacionSuperorden",
            "Orden","AutorOrden","ReferenciaClasificacionOrden",
            "Familia",     "EstatusFamilia","AutorFamilia","ReferenciaClasificacionFamilia",
            "Tribu",  "EstatusTribu","AutorTribu","ReferenciaNombreTribu",
            "Genero","EstatusGenero","AutorGenero","","ReferenciaNombreGenero",
            "Epiteto_especifico","EstatusEspecie","AutorEpiteto_especifico","ReferenciaNombreEspecie",
            "CategoriaInfraespecifica","NombreInfraespecifico","EstatusInfraespecie","AutorInfraespecie","ReferenciaNombreInfraespecifico",
            "CitaNomenclatural","Anotacion al Taxon","Fuente_BDs",
            "FamAceptada","GenAceptado","CategoriaTaxAceptada","NombreAceptado","AutorNombreAceptado","AutorSinAnioAceptado","AnioAceptado",
            "TipoRelacion","ReferenciaSinonimia","ComentariosRevisor",
            "CompareID","IdCAT_OLD","Nombre_OLD","AutorSinAnio_OLD",
            "CitaNomenclatural_OLD","ReferenceType","IsUpdated",

            "Hibrido","ReferenciaNombreHibrido","AutorHibrido","EstatusHibrido",
            "Subgenero","ReferenciaNombreSubgenero","EstatusSubgenero","AutorSubgenero",
            "Subtribu","ReferenciaClasificacionSubtribu","AutorSubtribu","EstatusSubtribu",
            "Subfamilia","ReferenciaClasificacionSubfamilia","AutorSubfamilia","EstatusSubfamilia",
            "ReferenciaClasificacionTribu",
            "Supertribu","ReferenciaClasificacionSupertribu","AutorSupertribu","EstatusSupertribu",

        });


    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();

        checkAllKeysExist(line, keys, expectedKeys);

        if (getValue(record, "Nombre") == null ){
            logger.warn("No FullnameNoAuthors given: " + line);
            return;
        }

        //Name
        IBotanicalName name = makeName(line, record, state);

        //sec
        String referenciaNombre = getValueNd(record, "ReferenciaNombre");

        //status
        String statusStr = getValue(record, "EstatusNombre");
        String originalInfo = null;
        TaxonBase<?> taxonBase;
        if ("aceptado".equals(statusStr)){
            Reference sec = getSecRef(state, referenciaNombre, line);
            taxonBase = Taxon.NewInstance(name, sec);
        }else if (statusStr.startsWith("sin")){
            String secRefStr = getValue(record, "ReferenciaSinonimia");

            Reference sec = getSynSec(state, secRefStr, referenciaNombre, line);
            taxonBase = Synonym.NewInstance(name, sec);
            if (isNotBlank(secRefStr)){
                originalInfo = "referenciaNombre: " + referenciaNombre;
            }
        }else{
            throw new RuntimeException(line + " Status not recognized: " + statusStr);
        }

        //annotation
        String annotation = getValue(record, "Anotacion al Taxon");
        if (annotation != null && (!annotation.equals("nom. illeg.") || !annotation.equals("nom. cons."))){
            taxonBase.addAnnotation(Annotation.NewInstance(annotation, AnnotationType.EDITORIAL(), Language.SPANISH_CASTILIAN()));
        }

        //id
        String idCat = getValue(record, "IdCAT");
        this.addOriginalSource(taxonBase, idCat, TAXON_NAMESPACE, state.getConfig().getSourceReference(), originalInfo);
        name.addIdentifier(idCat, getConabioIdIdentifierType(state));

//        checkSame(record, "EstatusHibrido", statusStr, line);
//        checkSame(record, "AutorHibrido", "AutorNombre", line);
//        checkSame(record, "ReferenciaNombreHibrido", "ReferenciaNombre", line);
//        checkSame(record, "Hibrido", "AutorNombre", line);

        //save
        getTaxonService().save(taxonBase);
        taxonIdMap.put(idCat, taxonBase);
    }

    private DefinedTerm getConabioIdIdentifierType(SimpleExcelTaxonImportState<CONFIG> state) {
        DefinedTerm conabioIdIdentifierType = getIdentiferType(state, MexicoConabioTransformer.uuidConabioTaxonIdIdentifierType, "Conabio name identifier", "Conabio name identifier", "CONABIO ID", null);
        return conabioIdIdentifierType;
    }

    private void checkSame(Map<String, String> record, String key, String compareValue, String line) {
        String value = getValue(record, key);
        if (value != null && !value.equals(compareValue)){
            logger.warn(line+ ": Value differs for "+ key +": " + value + "<->" + compareValue );
        }
    }

    private Reference getSynSec(SimpleExcelTaxonImportState<CONFIG> state, String secRefStr,
            String referenciaNombre, String line) {
        if (isBlank(secRefStr)){
            secRefStr = referenciaNombre;
        }
        if (isNotBlank(secRefStr)){
            Reference result = state.getReference(secRefStr);
            if (result == null){
                result = ReferenceFactory.newBook();
                result.setTitleCache(secRefStr, true);
                state.putReference(secRefStr, result);
            }
            return result;
        }else{
            return null;
        }
    }

    private Reference getSecRef(SimpleExcelTaxonImportState<CONFIG> state, String secRefStr, String line) {
        Reference result = state.getReference(secRefStr);
        if (result == null && secRefStr != null){
            result = ReferenceFactory.newBook();
            VerbatimTimePeriod tp = TimePeriodParser.parseStringVerbatim(secRefStr.substring(secRefStr.length()-4));
            String authorStrPart = secRefStr.substring(0, secRefStr.length()-6);
            if (! (authorStrPart + ", " + tp.getYear()).equals(secRefStr)){
                logger.warn(line + "Sec ref could not be parsed: " + secRefStr);
            }else{
                result.setDatePublished(tp);
            }
            TeamOrPersonBase<?> author = state.getAgentBase(authorStrPart);
            if (author == null){
                if (authorStrPart.contains("&")){
                    Team team = Team.NewInstance();
                    String[] authorSplit = authorStrPart.split("&");
                    String[] firstAuthorSplit = authorSplit[0].trim().split(",");
                    for (String authorStr : firstAuthorSplit){
                        addTeamMember(team, authorStr);
                    }
                    addTeamMember(team, authorSplit[1]);
                    result.setAuthorship(team);
                    state.putAgentBase(team.getTitleCache(), team);
                }else if (authorStrPart.equalsIgnoreCase("Tropicos") || authorStrPart.equalsIgnoreCase("The Plant List")
                        || authorStrPart.equalsIgnoreCase("APG IV")){
                    result.setTitle(authorStrPart);
                }else{
                    Person person = Person.NewInstance();
                    person.setFamilyName(authorStrPart);
                    result.setAuthorship(person);
                    state.putAgentBase(person.getTitleCache(), person);
                }
            }else{
                result.setAuthorship(author);
            }
            state.putReference(secRefStr, result);
        }else if(secRefStr == null){
            return state.getConfig().getSecReference();
        }

        return result;
    }

    private void addTeamMember(Team team, String author) {
        if (StringUtils.isNotBlank(author)){
            Person person = Person.NewInstance();
            person.setFamilyName(author.trim());
            team.addTeamMember(person);
        }
    }

    private IBotanicalName makeName(String line, Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {

        String authorStr = getValueNd(record, "AutorSinAnio");
        String nameStr = getValue(record, "Nombre");
        String nomRefStr = getValue(record, "CitaNomenclatural");
        String refType = getValue(record, "ReferenceType");
        String idCat = getValue(record, "IdCAT");
        String rankStr = getValue(record, "CategoriaTaxonomica");
        String annotation = getValue(record, "Anotacion al Taxon");

        //rank
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
            if (Rank.SUBSPECIES().equals(rank) || Rank.VARIETY().equals(rank) || Rank.FORM().equals(rank) || Rank.RACE().equals(rank)){
                int i = nameStr.lastIndexOf(" ");
                nameStr = nameStr.substring(0, i) + " " + rank.getAbbreviation() + nameStr.substring(i);
            }
        } catch (UndefinedTransformerMethodException e) {
            logger.warn(line + "Rank not recognized: " + rankStr);
        }

        //name + author
        String fullNameStr = nameStr + (authorStr != null ? " " + authorStr : "");

        IBotanicalName fullName = (IBotanicalName)nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, rank);
        if (fullName.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + fullNameStr );
        }else{
            replaceAuthorNamesAndNomRef(state, fullName);
        }
        IBotanicalName existingName = getExistingName(state, fullName);

        //reference
        String refNameStr = getRefNameStr(nomRefStr, refType, fullNameStr);

        IBotanicalName referencedName = (IBotanicalName)nameParser.parseReferencedName(refNameStr, NomenclaturalCode.ICNAFP, rank);
        if (referencedName.isProtectedFullTitleCache() || referencedName.isProtectedTitleCache()){
            logger.warn(line + "Referenced name could not be parsed: " + refNameStr );
        }else{
            addSourcesToReferences(referencedName, state);
            replaceAuthorNamesAndNomRef(state, referencedName);
        }
        adaptRefTypeForGeneric(referencedName, refType);

        //compare nom. ref. with Borhidi
        IBotanicalName result= referencedName;
        Boolean equal = null;
        if (existingName != null){
            String existingRefTitle = existingName.getFullTitleCache();
            String conabioRefTitle = referencedName.getFullTitleCache();
            if (!existingRefTitle.equals(conabioRefTitle)){
                existingName.setNomenclaturalMicroReference(referencedName.getNomenclaturalMicroReference());
                existingName.setNomenclaturalReference(referencedName.getNomenclaturalReference());
                equal = false;
            }else{
                equal = true;
            }
            result = existingName;
        }
        addNomRefExtension(state, result, equal);

        //status
        if (annotation != null && (annotation.equals("nom. illeg.") || annotation.equals("nom. cons."))){
            try {
                NomenclaturalStatusType nomStatusType = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(annotation, result);
                result.addStatus(NomenclaturalStatus.NewInstance(nomStatusType));
            } catch (UnknownCdmTypeException e) {
                logger.warn(line + "nomStatusType not recognized: " + annotation);
            }
        }

        this.addOriginalSource(result, idCat, TAXON_NAMESPACE + "_Name", state.getConfig().getSourceReference());

        if(result.getNomenclaturalReference()!=null && result.getNomenclaturalReference().getTitleCache().equals("null")){
            logger.warn("null");
        }

        return result;
    }

    private void addSourcesToReferences(IBotanicalName name, SimpleExcelTaxonImportState<CONFIG> state) {
        Reference nomRef = name.getNomenclaturalReference();
        if (nomRef != null){
            nomRef.addSource(makeOriginalSource(state));
            if (nomRef.getInReference() != null){
                nomRef.getInReference().addSource(makeOriginalSource(state));
            }
        }
    }

    private void adaptRefTypeForGeneric(IBotanicalName referencedName, String refTypeStr) {
        INomenclaturalReference ref = referencedName.getNomenclaturalReference();
        if (ref == null){
            return;
        }
        ReferenceType refType = refTypeByRefTypeStr(refTypeStr);
        if (ref.getType() != refType && refType == ReferenceType.Book){
            ref.setType(refType);
        }
    }

    private ReferenceType refTypeByRefTypeStr(String refType){
        if ("A".equals(refType)){  //Article
            return ReferenceType.Article;
        }else if ("B".equals(refType)){   //Book
            return ReferenceType.Book;
        }else if (refType == null){   //Book
            return null;
        }else{
            throw new IllegalArgumentException("RefType not supported " + refType);
        }
    }

    private String getRefNameStr(String nomRefStr, String refTypeStr, String fullNameStr) {
        String refNameStr = fullNameStr;
        ReferenceType refType = refTypeByRefTypeStr(refTypeStr);
        if (nomRefStr == null){
            //do nothing
        }else if (refType == ReferenceType.Article){
            refNameStr = fullNameStr + " in " + nomRefStr;
        }else if (refType == ReferenceType.Book){
            refNameStr = fullNameStr + ", " + nomRefStr;
        }else if (refType == null && nomRefStr != null){
            logger.warn("RefType is null but nomRefStr exists");
        }
        return refNameStr;
    }

    private void addNomRefExtension(SimpleExcelTaxonImportState<CONFIG> state, IBotanicalName name, Boolean equal) {
        String equalStr = equal == null ? "" : equal == true ? "EQUAL\n" : "NOT EQUAL\n";
        name.setFullTitleCache(null, false);
        String newExtensionStr = name.getFullTitleCache() + " - CONABIO";
        UUID uuidNomRefExtension = MexicoConabioTransformer.uuidNomRefExtension;
        for (Extension extension : name.getExtensions()){
            if (extension.getType().getUuid().equals(uuidNomRefExtension)){
                extension.setValue(equalStr + extension.getValue() + "\n" + newExtensionStr);
                return;
            }
        }
        String label = "Nomenclatural reference in Sources";
        String abbrev = "Nom. ref. src.";
        ExtensionType extensionType = getExtensionType(state, uuidNomRefExtension, label, label, abbrev);
        Extension.NewInstance((TaxonName)name, newExtensionStr, extensionType);
    }

    boolean nameMapIsInitialized = false;
    private IBotanicalName getExistingName(SimpleExcelTaxonImportState<CONFIG> state, IBotanicalName fullName) {
        initExistinNames(state);
        return (IBotanicalName)state.getName(fullName.getTitleCache());
    }

    private void initExistinNames(SimpleExcelTaxonImportState<CONFIG> state) {
        if (!nameMapIsInitialized){
            List<String> propertyPaths = Arrays.asList("");
            List<TaxonName> existingNames = this.getNameService().list(null, null, null, null, propertyPaths);
            for (TaxonName tnb : existingNames){
                state.putName(tnb.getTitleCache(), tnb);
            }
            nameMapIsInitialized = true;
        }
    }

    /**
     * Same as {@link #getValue(eu.etaxonomy.cdm.io.excel.common.ExcelImportState, String)}
     * but "ND" return null.
     */
    private String getValueNd(Map<String, String> record, String string) {
        String value = getValue(record, string);
        if ("ND".equals(value)){
            return null;
        }else{
            return value;
        }
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
//        IdCAT_AscendenteInmediato, IdCATRel, TipoRelacion
        Map<String, String> record = state.getOriginalRecord();
        String line = state.getCurrentLine() + ": ";

        String parentStr = getValue(record, "IdCAT_AscendenteInmediato");
        String relStr = getValue(record, "IdCATRel");

        String statusStr = getValue(record, "EstatusNombre");

        Classification classification = getClassification(state);
        String idCat = getValue(record, "IdCAT");
        TaxonBase<?> taxonBase = taxonIdMap.get(idCat);
        Taxon parent;
        if(statusStr == null){
            logger.warn("No statusStr in line " +line);
        }else if ("aceptado".equals(statusStr)){
            parent = (Taxon)taxonIdMap.get(parentStr);
            if (parent == null){
                logger.warn(line + "Parent is missing: "+ parentStr);
            }else{
                Taxon taxon = (Taxon)taxonBase;
                Reference relRef = null;
                classification.addParentChild(parent, taxon, relRef, null);
//                makeConceptRelation(line, taxon.getName());
            }
        }else if (statusStr.startsWith("sin")){
            parent = (Taxon)taxonIdMap.get(relStr);
            if (parent == null){
                logger.warn(line + "Accepted taxon is missing: "+ relStr);
            }else{
                Synonym synonym = (Synonym)taxonBase;
                parent.addSynonym(synonym, SynonymType.SYNONYM_OF());
//                makeConceptRelation(line, synonym.getName());
            }
        }else{
            logger.warn("Unhandled statusStr in line " + line);
        }
    }

    private void makeConceptRelation(String line, TaxonName name) {
        if (name.getTaxonBases().size()==2){
            Iterator<TaxonBase> it = name.getTaxonBases().iterator();
            Taxon taxon1 = getAccepted(it.next());
            Taxon taxon2 = getAccepted(it.next());
            Reference citation = null;
            TaxonRelationship rel;
            if (taxon1.getSec().getUuid().equals(MexicoConabioTransformer.uuidReferenceBorhidi)){
                rel = taxon1.addTaxonRelation(taxon2, TaxonRelationshipType.CONGRUENT_TO(),
                        citation, null);
            }else{
                rel = taxon2.addTaxonRelation(taxon1, TaxonRelationshipType.CONGRUENT_TO(),
                        citation, null);
            }
            rel.setDoubtful(true);
        }else if (name.getTaxonBases().size()>2){
            logger.warn(line + "Names with more than 2 taxa not yet handled");
        }
    }

    private Taxon getAccepted(TaxonBase<?> taxonBase) {
        if (taxonBase.isInstanceOf(Taxon.class)){
            return CdmBase.deproxy(taxonBase, Taxon.class);
        }else{
            Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
            return syn.getAcceptedTaxon();
        }
    }

    private Classification getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (classification == null){
            MexicoConabioImportConfigurator config = state.getConfig();
            classification = getClassificationService().find(config.getClassificationUuid());
            if (classification == null){
                classification = Classification.NewInstance(config.getClassificationName());
                classification.setUuid(config.getClassificationUuid());
                classification.setReference(config.getSecReference());
                getClassificationService().save(classification);
            }
        }
        return classification;
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoTaxa();
    }
}