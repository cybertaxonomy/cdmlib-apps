/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.cuba;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;

/**
 * @author a.mueller
 * @created 05.01.2016
 */

@Component
public class CubaVocabularyImport extends CdmImportBase<CubaImportConfigurator, CubaImportState> {
    private static final long serialVersionUID = -747486709409732371L;

    private static final Logger logger = Logger.getLogger(CubaVocabularyImport.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInvoke(CubaImportState state) {
        try {
            makeAreas(state);
            makePresenceAbsenceTerms(state);
        } catch (UndefinedTransformerMethodException e) {
           e.printStackTrace();
        }
    }

    /**
     * @param state
     */
    private void makePresenceAbsenceTerms(CubaImportState state) {
        TransactionStatus tx = startTransaction();
        commitTransaction(tx);
    }

    private boolean makeAreas(CubaImportState state) throws UndefinedTransformerMethodException{
        TransactionStatus tx = startTransaction();

        IInputTransformer transformer = state.getTransformer();

        //vocabulary
        UUID cubaAreasVocabularyUuid = UUID.fromString("c81e3c7b-3c01-47d1-87cf-388de4b1908c");
        String label = "Cuba Areas";
        String abbrev = null;
        boolean isOrdered = true;
        NamedArea anyArea = NamedArea.ARCTICOCEAN();  //just any
        TermVocabulary<NamedArea> cubaAreasVocabualary = getVocabulary(TermType.NamedArea, cubaAreasVocabularyUuid, label, label, abbrev, null, isOrdered, anyArea);

        TermMatchMode matchMode = null;

        NamedAreaType areaType = null;  //TODO
        NamedAreaLevel level = null;  //TODO

        //Cuba
        level = NamedAreaLevel.COUNTRY();
        label = "Cuba";
        abbrev = "C";
        UUID cubaUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea cuba = getNamedArea(state, cubaUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Regions
        level = null;

        //Western Cuba
        label = "Western Cuba";
        abbrev = "CuW";
        UUID cubaWestUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea westernCuba = getNamedArea(state, cubaWestUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(westernCuba);

        //Central Cuba
        label = "Central Cuba";
        abbrev = "CuC";
        UUID cubaCentralUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea centralCuba = getNamedArea(state, cubaCentralUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(centralCuba);

        //East Cuba
        label = "East Cuba";
        abbrev = "CuE";
        UUID cubaEastUuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea eastCuba = getNamedArea(state, cubaEastUuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        cuba.addIncludes(eastCuba);

        //Provinces - West
        level = NamedAreaLevel.PROVINCE();

        //Pinar del Río PR
        label = "Pinar del Río";
        abbrev = "PR";
        UUID uuid = transformer.getNamedAreaUuid(abbrev);
        NamedArea area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);

        //Habana Hab
        label = "Habana"; //including Ciudad de la Habana, Mayabeque, Artemisa
        abbrev = "Hab";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);

        //Matanzas Mat
        label = "Matanzas";
        abbrev = "Mat";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);

        //Isla de la Juventud IJ
        label = "Isla de la Juventud";
        abbrev = "IJ";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        westernCuba.addIncludes(area);

        //Provinces - Central
        //Villa Clara VC
        label = "Villa Clara";
        abbrev = "VC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Cienfuegos Ci VC
        label = "Cienfuegos";
        abbrev = "Ci";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Sancti Spiritus SS
        label = "Sancti Spiritus";
        abbrev = "SS";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Ciego de Ávila CA
        label = "Ciego de Ávila";
        abbrev = "CA";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Camagüey Cam
        label = "Camagüey";
        abbrev = "Cam";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Las Tunas LT
        label = "Las Tunas";
        abbrev = "LT";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        centralCuba.addIncludes(area);

        //Provinces - East
        //Granma Gr
        label = "Granma";
        abbrev = "Gr";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);

        //Holguín Ho
        label = "Holguín";
        abbrev = "Ho";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);

        //Santiago de Cuba SC
        label = "Santiago de Cuba";
        abbrev = "SC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);

        //Guantánamo Gu
        label = "Guantánamo";
        abbrev = "Gu";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);
        eastCuba.addIncludes(area);

        //Surrounding
        //Española Esp
        label = "Española";
        abbrev = "Esp";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Jamaica Ja
        label = "Jamaica";
        abbrev = "Ja";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Puerto Rico PR
        label = "Puerto Rico";  //Greater Antilles
        abbrev = "PR";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Smaller Antilles Men
        label = "Smaller Antilles";
        abbrev = "Men";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Bahamas
        label = "Bahamas";
        abbrev = "Bah";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Cayman Islands
        label = "Cayman Islands"; //[Trinidad, Tobago, Curaçao, Margarita, ABC Isl. = S. America];
        abbrev = "Cay";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //World
        //N America
        label = "N America"; //(incl. Mexico)
        abbrev = "AmN";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Central America
        label = "Central America";
        abbrev = "AmC";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //S America
        label = "S America";
        abbrev = "AmS";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        //Old World
        label = "Old World ";
        abbrev = "VM";
        uuid = transformer.getNamedAreaUuid(abbrev);
        area = getNamedArea(state, uuid, label, label, abbrev, areaType, level, cubaAreasVocabualary, matchMode);

        commitTransaction(tx);
        return true;
    }


    @Override
    protected boolean isIgnore(CubaImportState state) {
        return ! state.getConfig().isDoTaxa();
    }


    @Override
    protected boolean doCheck(CubaImportState state) {
        logger.warn("DoCheck not yet implemented for CubaVocabularyImport");
        return true;
    }



}
