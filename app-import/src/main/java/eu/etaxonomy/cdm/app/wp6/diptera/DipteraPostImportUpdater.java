/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.wp6.diptera;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.INameService;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.taxon.Taxon;

/**
 * @author a.mueller
 * @since 01.10.2009
 */
public class DipteraPostImportUpdater {
	private static final Logger logger = Logger.getLogger(DipteraPostImportUpdater.class);

	static final ICdmDataSource cdmDestination = CdmDestinations.localH2Palmae();

	/**
	 * This method updateds the citation text by deleting <code>OriginalName</code> tags and
	 * adding the original name to the source either as a link to an existing taxon name
	 * or as a string. The later becomes true if there is not exactly one matching name
	 * @param dataSource
	 * @return
	 */
	public ImportResult updateCitations(ICdmDataSource dataSource) {

			logger.warn("start updating citations");
			ImportResult result = new ImportResult();
			try{
			CdmApplicationController cdmApp = CdmApplicationController.NewInstance(dataSource, DbSchemaValidation.VALIDATE);
			Set<DescriptionElementBase> citationsToSave = new HashSet<>();
			TransactionStatus tx = cdmApp.startTransaction();

			logger.warn("start updating citations ... application context started");
			int modCount = 100;
			int page = 0;
			List<Taxon> taxonList = cdmApp.getTaxonService().list(Taxon.class, 100000, page, null, null);
			List<TaxonName> nameList = cdmApp.getNameService().list(null, 100000, page, null, null);
			Map<String, TaxonName> nameMap = new HashMap<>();
			Map<String, TaxonName> nameDuplicateMap = new HashMap<>();
			fillNameMaps(nameList, nameMap, nameDuplicateMap);

			int i = 0;

			for (Taxon taxon : taxonList){
				if ((i++ % modCount) == 0){ logger.warn("taxa handled: " + (i-1));}

				Set<TextData> citations = getCitations(taxon);
				for (TextData citation : citations){
					Language language = Language.DEFAULT();
					String text = citation.getText(language);
					String originalNameString = parseOriginalNameString(text);
					String newText = parseNewText(text);
					citation.removeText(language);
					citation.putText(language, newText);
					TaxonName scientificName = getScientificName(originalNameString, nameMap, nameDuplicateMap);

					Set<DescriptionElementSource> sources = citation.getSources();
					if (sources.size() > 1){
						logger.warn("There are more then 1 sources for a description");
					}else if (sources.size() == 0){
						DescriptionElementSource source = DescriptionElementSource.NewInstance(OriginalSourceType.PrimaryTaxonomicSource);
						citation.addSource(source);
						sources = citation.getSources();
					}
					for (DescriptionElementSource source : sources){
						if (scientificName != null){
							source.setNameUsedInSource(scientificName);
						}else{
							source.setOriginalNameString(originalNameString);
						}
					}

					citationsToSave.add(citation);
				}
			}

			cdmApp.getDescriptionService().saveDescriptionElement(citationsToSave);
			//commit
			cdmApp.commitTransaction(tx);
			logger.warn("Citations updated!");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.addError("ERROR in citation update");
			result.addException(e);
			return result;
		}

	}

	public ImportResult updateCollections(ICdmDataSource dataSource){
		DipteraCollectionImport collectionImport = new DipteraCollectionImport();
		return collectionImport.invoke(dataSource);
	}


	private void fillNameMaps(List<TaxonName> nameList, Map<String, TaxonName> nameMap, Map<String, TaxonName> duplicateMap) {
		for (TaxonName name : nameList){
			String nameCache = name.getNameCache();
			if (nameMap.containsKey(nameCache)){
				duplicateMap.put(nameCache, name);
			}else{
				nameMap.put(nameCache, name);
			}
		}
	}


	private TaxonName getScientificName(String originalNameString, Map<String, TaxonName> nameMap, Map<String, TaxonName> nameDuplicateMap) {
		originalNameString = originalNameString.trim();
		TaxonName result = nameMap.get(originalNameString);
		if (nameDuplicateMap.containsKey(originalNameString)){
			result = null;
		}
		return result;
	}

	private TaxonName getScientificName(String originalNameString, INameService nameService) {
		Pager<TaxonName> names = nameService.findByName(null, originalNameString, null, null, null, null, null, null);
		if (names.getCount() != 1){
			return null;
		}else{
			return names.getRecords().get(0);
		}
	}

	private String parseOriginalNameString(String text) {
		String originalName = "<OriginalName>";
		int start = text.indexOf(originalName);
		int end = text.indexOf("</OriginalName>");
		if (start >-1 ){
			text = text.substring(start + originalName.length(), end);
		}
		text = text.trim();
		return text;
	}

	private String parseNewText(String text) {
		int start = text.indexOf("</OriginalName>");
		text = text.substring(start + "</OriginalName>".length());
		text = text.trim();
		if (text.startsWith(":")){
			text = text.substring(1);
		}
		text = text.trim();
		return text;
	}

	private Set<TextData> getCitations(Taxon taxon) {
		Set<TextData> result = new HashSet<TextData>();
		Set<TaxonDescription> descriptions = taxon.getDescriptions();
		for (DescriptionBase description : descriptions){
			Set<DescriptionElementBase> elements = description.getElements();
			for (DescriptionElementBase element : elements){
				Feature feature = element.getFeature();
				if (feature.equals(Feature.CITATION())){
					if (! element.isInstanceOf(TextData.class)){
						logger.warn("Citation is not of class TextData but " + element.getClass().getSimpleName());
					}else{
						TextData textData = element.deproxy(element, TextData.class);
						result.add(textData);
					}
				}
			}
		}
		return result;
	}




	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DipteraPostImportUpdater updater = new DipteraPostImportUpdater();
		try {
			updater.updateCitations(cdmDestination);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("ERROR in feature tree update");
		}
	}

}
