/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io;

import java.io.File;
import eu.etaxonomy.cdm.common.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ICommonService;
import eu.etaxonomy.cdm.app.wp6.palmae.config.PalmaeProtologueImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.DefaultImportState;
import eu.etaxonomy.cdm.model.media.ExternalLinkType;
import eu.etaxonomy.cdm.model.name.TaxonName;

/**
 * @author a.mueller
 * @since 29.07.2008
 */
@Component
public class ProtologueImport
        extends CdmImportBase<PalmaeProtologueImportConfigurator, DefaultImportState<PalmaeProtologueImportConfigurator>>{

    private static final long serialVersionUID = 4580327331805229644L;
    private static final Logger logger = Logger.getLogger(ProtologueImport.class);

	private String pluralString = "protologues";
	private static int modCount = 200;

	public ProtologueImport(){
		super();
	}

	@Override
    public void doInvoke(DefaultImportState<PalmaeProtologueImportConfigurator> state){
		logger.info("start make Protologues from files ...");

		Set<TaxonName> nameStore = new HashSet<>();

		PalmaeProtologueImportConfigurator config = state.getConfig();
		File source = config.getSource();
		TaxonName name;
		TransactionStatus txStatus = startTransaction(false);
		int count = 0;
		if (source.isDirectory()){
			for (File file : source.listFiles() ){
				if (file.isFile()){
					doCount(count++, modCount, pluralString);
					name = importFile(file, state);
					storeName(nameStore, name, state);
				}
			}
		}else{
			if (source.isFile()){
				name = importFile(source, state);
				storeName(nameStore, name, state);
			}
		}
		getNameService().save(nameStore);
		commitTransaction(txStatus);
		logger.info("end make Protologues from files ...");
		return;
	}

	private void storeName(Set<TaxonName> nameStore, TaxonName name, DefaultImportState<PalmaeProtologueImportConfigurator> state){
		if (name != null){
			nameStore.add(name);
			return;
		}else{
			state.setUnsuccessfull();
			return;
		}
	}

	private TaxonName importFile(File file, DefaultImportState<PalmaeProtologueImportConfigurator> state){
		String originalSourceId = file.getName();
		originalSourceId =originalSourceId.replace("_P.pdf", "");
		originalSourceId =originalSourceId.replace("_tc_", "_tn_");
		String namespace = state.getConfig().getOriginalSourceTaxonNamespace();


		//for testing only
		TaxonName taxonName = getTaxonName(originalSourceId, namespace);
		if (taxonName == null){
			logger.warn("Name not found for " + originalSourceId);
			return null;
		}

		try{
            String urlStringPdf = state.getConfig().getUrlString() + file.getName();
            URI uri = new URI(urlStringPdf);
            taxonName.addProtologue(uri, null, ExternalLinkType.File);
		}catch(NullPointerException e){
			logger.warn("MediaUrl and/or MediaPath not set. Could not get protologue.");
			return null;
		} catch (URISyntaxException e) {
            logger.warn("URISyntaxException when reading URI. Could not get protologue.");
            return null;
		}
		return null;
	}

	private TaxonName getTaxonName(String originalSourceId, String namespace){
		TaxonName result;
		ICommonService commonService = getCommonService();

		result = commonService.getSourcedObjectByIdInSource(TaxonName.class, originalSourceId , namespace);
		if (result == null){
			logger.warn("Taxon (id: " + originalSourceId + ", namespace: " + namespace + ") could not be found");
		}
		return result;
	}

	@Override
    public boolean doCheck(@SuppressWarnings("rawtypes") DefaultImportState state){
		return true;
	}

	@Override
    protected boolean isIgnore(DefaultImportState state){
		return false; // ! state.getConfig();
	}

	protected void doCount(int count, int modCount, String pluralString){
		if ((count % modCount ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
	}
}
