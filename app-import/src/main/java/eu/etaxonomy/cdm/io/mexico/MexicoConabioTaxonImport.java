// $Id$
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
@Component
public class MexicoConabioTaxonImport<CONFIG extends MexicoConabioImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 3691221053127007258L;

    private static final Logger logger = Logger.getLogger(MexicoConabioTaxonImport.class);

    private final Map<String, TaxonBase<?>> taxonIdMap = new HashMap<>();

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
            "CitaNomenclatural_OLD","ReferenceType","IsUpdated"
        });


    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();

        checkAllKeysExist(line, keys, expectedKeys);

        if (getValue(record, "Nombre") == null ){
            logger.warn("No FullnameNoAuthors given: " + line);
            return;
        }

        //Name
        BotanicalName speciesName = makeName(line, record, state);
        String statusStr = getValue(record, "EstatusNombre");

        String secRefStr = getValueNd(record, "ReferenciaNombre");
        Reference sec = getSecRef(state, secRefStr, line);

        TaxonBase<?> taxonBase;
        if ("aceptado".equals(statusStr)){
            taxonBase = Taxon.NewInstance(speciesName, sec);
        }else if (statusStr.startsWith("sin")){
            taxonBase = Synonym.NewInstance(speciesName, sec);
        }else{
            throw new RuntimeException(line + " Status not recognized: " + statusStr);
        }

        String annotation = getValue(record, "Anotacion al Taxon");
        if (annotation != null && (!annotation.equals("nom. illeg.") || !annotation.equals("nom. cons."))){
            taxonBase.addAnnotation(Annotation.NewInstance(annotation, AnnotationType.EDITORIAL(), Language.SPANISH_CASTILIAN()));
        }

        String idCat = getValue(record, "IdCAT");
        this.addOriginalSource(taxonBase, idCat, "IdCAT", state.getConfig().getSourceReference());

        getTaxonService().save(taxonBase);
        taxonIdMap.put(idCat, taxonBase);
        if (taxonBase instanceof Taxon){
            state.putHigherTaxon(idCat, (Taxon)taxonBase);
        }

    }



    /**
     * @param state
     * @param secRefStr
     * @return
     */
    private Reference getSecRef(SimpleExcelTaxonImportState<CONFIG> state, String secRefStr, String line) {
        Reference result = state.getReference(secRefStr);
        if (result == null && secRefStr != null){
            result = ReferenceFactory.newBook();
            TimePeriod tp = TimePeriodParser.parseString(secRefStr.substring(secRefStr.length()-4));
            String authorStr = secRefStr.substring(0, secRefStr.length()-6);
            if (! (authorStr + ", " + tp.getYear()).equals(secRefStr)){
                logger.warn(line + "Sec ref could not be parsed: " + secRefStr);
            }else{
                result.setDatePublished(tp);
            }

            if (authorStr.contains(",") || authorStr.contains("&")){
                //TODO split
                Team team = Team.NewTitledInstance(authorStr, null);
                result.setAuthorship(team);
            }else if (authorStr.equals("Tropicos") || authorStr.equals("The plant list")
                    || authorStr.equals("APG IV")){
                result.setTitle(authorStr);

            }else{
                Person person = Person.NewTitledInstance(authorStr);
                result.setAuthorship(person);
            }

        }else if(secRefStr == null){
            logger.warn(line + "Empty secRefStr not yet implemented");
        }

        return result;
    }



    /**
     * @param record
     * @param state
     * @return
     */
    private BotanicalName makeName(String line, HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {

        String authorStr = getValueNd(record, "AutorSinAnio");
        String nameStr = getValue(record, "Nombre");
        String fullNameStr = nameStr + (authorStr != null ? " " + authorStr : "");
        String nomRefStr = getValue(record, "CitaNomenclatural");
        String refType = getValue(record, "ReferenceType");
        String idCat = getValue(record, "IdCAT");
        String rankStr = getValue(record, "CategoriaTaxonomica");
        String annotation = getValue(record, "Anotacion al Taxon");
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
        } catch (UndefinedTransformerMethodException e) {
            logger.warn(line + "Rank not recognized: " + rankStr);
        }

        BotanicalName fullName = (BotanicalName)nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, rank);
        if (fullName.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + fullNameStr );
        }else{
            replaceAuthorNames(state, fullName);
        }
        BotanicalName existingName = getExistingName(state, fullName);

        String refNameStr = fullNameStr;
        if ("A".equals(refType)){  //Article
            refNameStr = fullNameStr + " in " + nomRefStr;
        }else if ("B".equals(refType)){   //Book
            refNameStr = fullNameStr + ", " + nomRefStr;
        }

        BotanicalName referencedName = (BotanicalName)nameParser.parseFullName(refNameStr, NomenclaturalCode.ICNAFP, rank);
        if (referencedName.isProtectedFullTitleCache() || referencedName.isProtectedTitleCache()){
            logger.warn(line + "Referenced name could not be parsed: " + refNameStr );
            BotanicalName tmp = (BotanicalName)nameParser.parseFullName(refNameStr, NomenclaturalCode.ICNAFP, rank);

        }else{
            replaceAuthorNames(state, referencedName);
        }

        BotanicalName result= referencedName;
        if (existingName != null){
            String existingRefTitle = existingName.getFullTitleCache();
            String conabioRefTitle = referencedName.getFullTitleCache();
            addNomRefExtension(state, referencedName);
            if (!existingRefTitle.equals(conabioRefTitle)){
                existingName.setNomenclaturalMicroReference(referencedName.getNomenclaturalMicroReference());
                existingName.setNomenclaturalReference(referencedName.getNomenclaturalReference());
                result = existingName;
            }
        }

        if (annotation != null && (annotation.equals("nom. illeg.") || annotation.equals("nom. cons."))){
            try {
                NomenclaturalStatusType nomStatusType = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(annotation, result);
                result.addStatus(NomenclaturalStatus.NewInstance(nomStatusType));
            } catch (UnknownCdmTypeException e) {
                logger.warn(line + "nomStatusType not recognized: " + annotation);
            }
        }

        this.addOriginalSource(result, idCat, "IdCAT", state.getConfig().getSourceReference());

        return result;
    }

    /**
     * @param state
     * @param referencedName
     */
    private void addNomRefExtension(SimpleExcelTaxonImportState<CONFIG> state, BotanicalName name) {
        String newExtensionStr = name.getFullTitleCache() + " - CONABIO";
        UUID uuidNomRefExtension = MexicoConabioTransformer.uuidNomRefExtension;
        for (Extension extension : name.getExtensions()){
            if (extension.getType().getUuid().equals(uuidNomRefExtension)){
                extension.setValue(extension.getValue() + "\n" + newExtensionStr);
                return;
            }
        }
        String label = "Nomenclatural reference in Sources";
        String abbrev = "Nom. ref. src.";
        ExtensionType extensionType = getExtensionType(state, uuidNomRefExtension, label, label, abbrev);
        Extension.NewInstance(name, newExtensionStr, extensionType);
    }

    boolean nameMapIsInitialized = false;
    /**
     * @param state
     * @param fullName
     * @return
     */
    @SuppressWarnings("rawtypes")
    private BotanicalName getExistingName(SimpleExcelTaxonImportState<CONFIG> state, BotanicalName fullName) {
        if (!nameMapIsInitialized){
            List<String> propertyPaths = Arrays.asList("");
            List<TaxonNameBase> existingNames = this.getNameService().list(null, null, null, null, propertyPaths);
            for (TaxonNameBase tnb : existingNames){
                state.putName(tnb.getTitleCache(), (NonViralName<?>)tnb);
            }
            nameMapIsInitialized = true;
        }
        return (BotanicalName)state.getName(fullName.getTitleCache());
    }

    /**
     * @param record
     * @param string
     * @return
     */
    private String getValueNd(HashMap<String, String> record, String string) {
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
        HashMap<String, String> record = state.getOriginalRecord();
        String line = state.getCurrentLine() + ": ";

        String parentStr = getValue(record, "IdCAT_AscendenteInmediato");
        String relStr = getValue(record, "IdCATRel");

        String statusStr = getValue(record, "EstatusNombre");

        Classification classification = getClassification(state);
        String idCat = getValue(record, "IdCAT");
        TaxonBase<?> taxonBase = taxonIdMap.get(idCat);
        Taxon parent;
        if ("aceptado".equals(statusStr)){
            parent = (Taxon)taxonIdMap.get(parentStr);
            if (parent == null){
                logger.warn(line + "Parent is missing: "+ parentStr);
            }else{
                Reference relRef = null;  //TODO
                classification.addParentChild(parent, (Taxon)taxonBase, relRef, null);
            }
        }else if (statusStr.startsWith("sin")){
            parent = (Taxon)taxonIdMap.get(relStr);
            if (parent == null){
                logger.warn(line + "Parent is missing: "+ relStr);
            }else{
                Reference synRef = null; //null
                parent.addSynonym((Synonym)taxonBase, SynonymRelationshipType.SYNONYM_OF(), synRef, null);
            }
        }
    }

     /**
     * @return
     */
    private Classification getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (classification == null){
            MexicoConabioImportConfigurator config = state.getConfig();
            classification = Classification.NewInstance(config.getClassificationName());
            classification.setUuid(config.getClassificationUuid());
            classification.setReference(config.getSourceReference());
            getClassificationService().save(classification);
        }
        return classification;
    }


    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoTaxa();
    }
}
