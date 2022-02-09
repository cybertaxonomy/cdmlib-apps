/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 16.06.2016
 */
@Component
public class MexicoConabioCommonNamesImport<CONFIG extends MexicoConabioImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 3579868489510261569L;

    private static final Logger logger = Logger.getLogger(MexicoConabioCommonNamesImport.class);

    private TermVocabulary<Language> languagesVoc;
    private Map<String, Language> languagesMap = new HashMap<>();

    private Map<String, Taxon> taxonIdMap;

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "NombresComunes";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        initLanguageVocabulary(state);
        initTaxa();

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        String idCat = getValue(record, "IdCAT");
        Taxon taxon = taxonIdMap.get(idCat);
        if (taxon == null){
            logger.warn(line + "Taxon could not be found: " + idCat);
        }else{
            TaxonDescription desc = getTaxonDescription(taxon);
            String nomComunStr = getValue(record, "NomComun");
            String langStr = getValueNd(record, "Lengua");
            Language language = languagesMap.get(langStr);
            if (language == null && langStr != null){
                addNewLanguage(state, langStr);
            }
            logger.warn("Common names for the same taxon using the same common name but differing in source are currently not yet deduplicated. Needs to be fixed for next import");

            String refStr = getValue(record, "ReferenciasNombreComun");
            CommonTaxonName commonName = CommonTaxonName.NewInstance(nomComunStr,
                    language, null);
            desc.addElement(commonName);

            Reference ref = getReference(state, refStr);
            if (ref != null){
                commonName.addSource(OriginalSourceType.PrimaryTaxonomicSource,
                        null, null, ref, null);
            }

            getTaxonService().save(taxon);
        }
    }

    private Language addNewLanguage(SimpleExcelTaxonImportState<CONFIG> state, String langStr) {
        UUID uuid = getNewLanguageUuid(langStr);
        if (uuid != null){
            Language newLanguage = addLanguage(state, langStr, uuid);
            getTermService().saveOrUpdate(newLanguage);
            logger.warn("New language added: " + langStr + "("+ uuid + ")");
            return newLanguage;
        }else{
            logger.warn("Language not recognized: " + langStr);
            return null;
        }
    }

    private UUID getNewLanguageUuid(String langStr) {
        if (langStr.equals("Mixteco")){return MexicoConabioTransformer.uuidMixteco;}
        else if (langStr.equals("Náhuatl-Español")){return MexicoConabioTransformer.uuidNahuatlEspanol;}
        else if (langStr.equals("Español-Náhuatl")){return MexicoConabioTransformer.uuidEspanolNahuatl;}
        else if (langStr.equals("Tzotzil")){return MexicoConabioTransformer.uuidTzotzil;}
        else if (langStr.equals("Tetzal")){return MexicoConabioTransformer.uuidTetzal;}
        else if (langStr.equals("Tzeltal")){return MexicoConabioTransformer.uuidTzeltal;}
        else if (langStr.equals("Mazateco")){return MexicoConabioTransformer.uuidMazateco;}
        else if (langStr.equals("Tepehua")){return MexicoConabioTransformer.uuidTepehua;}

        else if (langStr.equals("Cuicateco")){return MexicoConabioTransformer.uuidCuicateco;}
        else if (langStr.equals("Zoque")){return MexicoConabioTransformer.uuidZoque;}
        else if (langStr.equals("Tepehuano")){return MexicoConabioTransformer.uuidTepehuano;}
        else if (langStr.equals("Popoloca")){return MexicoConabioTransformer.uuidPopoloca;}
        else if (langStr.equals("Tojolabal")){return MexicoConabioTransformer.uuidTojolabal;}

        else if (langStr.equals("Mayo")){return MexicoConabioTransformer.uuidMayo;}
        else if (langStr.equals("Huichol")){return MexicoConabioTransformer.uuidHuichol;}
        else if (langStr.equals("Cora")){return MexicoConabioTransformer.uuidCora;}
        else if (langStr.equals("Seri")){return MexicoConabioTransformer.uuidSeri;}

        return null;
    }

    private void initTaxa() {
        if (taxonIdMap == null){
            Set<String> existingKeys = MexicoConabioTaxonImport.taxonIdMap.keySet();
            taxonIdMap = getCommonService().getSourcedObjectsByIdInSourceC(Taxon.class,
                    existingKeys, MexicoConabioTaxonImport.TAXON_NAMESPACE);
        }
    }


    @SuppressWarnings("unchecked")
    private void initLanguageVocabulary(SimpleExcelTaxonImportState<CONFIG> state) {
        if (languagesVoc == null){
            languagesVoc = this.getVocabularyService().find(MexicoConabioTransformer.uuidMexicanLanguagesVoc);
            if (languagesVoc == null){
                createLanguagesVoc(state);
            }else{
                fillLanguageMap();
            }
        }
    }

    private void fillLanguageMap() {
        for (Language language: languagesVoc.getTerms()){
            String label = language.getLabel();
            languagesMap.put(label, language);
        }
    }

    private void createLanguagesVoc(SimpleExcelTaxonImportState<CONFIG> state) {
        URI termSourceUri = null;
        String label = "Mexican States";
        String description = "Mexican languages as used by the CONABIO Rubiaceae database";
        languagesVoc = TermVocabulary.NewInstance(TermType.Language, Language.class,
                description, label, null, termSourceUri);
        languagesVoc.setUuid(MexicoConabioTransformer.uuidMexicanLanguagesVoc);

        addLanguage(state, "Chontal", MexicoConabioTransformer.uuidChontal);
        addLanguage(state, "Chinanteco", MexicoConabioTransformer.uuidChinanteco);
        addLanguage(state, "Chiapaneca", MexicoConabioTransformer.uuidChiapaneca);
        addLanguage(state, "Huasteco", MexicoConabioTransformer.uuidHuasteco);
        addLanguage(state, "Español-Maya", MexicoConabioTransformer.uuidEspanol_Maya);
        addLanguage(state, "Guarijío", MexicoConabioTransformer.uuidGuarijio);
        addLanguage(state, "Huave", MexicoConabioTransformer.uuidHuave);
        addLanguage(state, "Español", MexicoConabioTransformer.uuidEspanol);
        addLanguage(state, "Maya", MexicoConabioTransformer.uuidMaya);
        addLanguage(state, "Lacandón", MexicoConabioTransformer.uuidLacandon);
        addLanguage(state, "Inglés", MexicoConabioTransformer.uuidIngles);
        addLanguage(state, "Itzmal", MexicoConabioTransformer.uuidItzmal);
        addLanguage(state, "Náhuatl", MexicoConabioTransformer.uuidNahuatl);
        addLanguage(state, "Tarahumara", MexicoConabioTransformer.uuidTarahumara);
        addLanguage(state, "Otomí", MexicoConabioTransformer.uuidOtomi);
        addLanguage(state, "Mixe", MexicoConabioTransformer.uuidMixe);
        addLanguage(state, "Tseltal", MexicoConabioTransformer.uuidTseltal);
        addLanguage(state, "Zapoteco", MexicoConabioTransformer.uuidZapoteco);
        addLanguage(state, "Totonaco", MexicoConabioTransformer.uuidTotonaco);
        addLanguage(state, "Tarasco", MexicoConabioTransformer.uuidTarasco);

        this.getVocabularyService().save(languagesVoc);

        return;
    }

    private Language addLanguage(SimpleExcelTaxonImportState<CONFIG> state, String label, UUID uuid) {
        String abbrev = null;
        Language language = Language.NewInstance(
                label, label, abbrev);
        language.setUuid(uuid);
        languagesVoc.addTerm(language);
        languagesMap.put(label, language);
        return language;
    }

    private Reference getReference(SimpleExcelTaxonImportState<CONFIG> state, String refStr) {
        if (StringUtils.isBlank(refStr)){
            return null;
        }
        Reference ref = state.getReference(refStr);
        if (ref == null){
            ref = ReferenceFactory.newBook();
            ref.setTitleCache(refStr, true);
            state.putReference(refStr, ref);
        }
        return ref;
    }

    private TaxonDescription getTaxonDescription(Taxon taxon) {
        if (!taxon.getDescriptions().isEmpty()){
            return taxon.getDescriptions().iterator().next();
        }else{
            TaxonDescription desc = TaxonDescription.NewInstance(taxon);
            return desc;
        }
    }

    private String getValueNd(Map<String, String> record, String string) {
        String value = getValue(record, string);
        if ("ND".equals(value)){
            return null;
        }else{
            return value;
        }
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return ! state.getConfig().isDoCommonNames();
    }

}
